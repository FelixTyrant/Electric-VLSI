/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool.simulation;

import java.io.*;
import java.util.*;
import com.sun.electric.database.geometry.btree.*;
import com.sun.electric.database.geometry.btree.unboxed.*;
import com.sun.electric.tool.simulation.*;

abstract class BTreeSignal<S extends Sample> extends MutableSignal<S> {

    private Signal.View<S> exactView = null;
    private final BTree<Double,S,Pair<S,S>> tree;
    private Signal.View<S> pa = null;
    private double tmin;
    private double tmax;
    private int    emax;

    public static int misses = 0;
    public static int steps = 0;
    public static int numLookups = 0;

    public BTreeSignal(Analysis analysis, String signalName, String signalContext,
                       BTree<Double,S,Pair<S,S>> tree
                       ) {
        super(analysis, signalName, signalContext);
        if (tree==null) throw new RuntimeException();
        this.tree = tree;
        this.exactView = new Signal.View<S>() {
            public int getNumEvents() { return BTreeSignal.this.tree.size(); }
            public double             getTime(int index) {
                Double d = BTreeSignal.this.tree.getKeyFromOrd(index);
                if (d==null) throw new RuntimeException("index out of bounds");
                return d.doubleValue();
            }
            public S       getSample(int index) {
                S ret = BTreeSignal.this.tree.getValFromOrd(index);
                if (ret==null) throw new RuntimeException("index out of bounds");
                return ret;
            }
        };
    }

    public S    getSample(double time) { return tree.getValFromKey(time); }
    public void addSample(double time, S sample) { tree.insert(time, sample); }
    public void replaceSample(double time, S sample) { tree.replace(time, sample); }

    public Signal.View<S> getExactView() { return exactView; }

    public Signal.View<RangeSample<S>> getRasterView(double t0, double t1, int numPixels, boolean extrapolate) {
        return new BTreeRasterView(t0, t1, numPixels, extrapolate);
    }

    public boolean isEmpty() { return tree.size()==0; }
	public double getMinTime()  { return tree.size()==0 ? 0 : getExactView().getTime(0); }
	public double getMaxTime()  { return tree.size()==0 ? 0 : getExactView().getTime(getExactView().getNumEvents()-1); }
	public S getMinValue() {
        Pair<S,S> pk = getSummaryFromKeys(null, null);
        if (pk==null) return null;
        return pk.getKey();
    }
	public S getMaxValue() {
        Pair<S,S> pk = getSummaryFromKeys(null, null);
        if (pk==null) return null;
        return pk.getValue();
    }

    protected Pair<S,S> getSummaryFromKeys(Double t1, Double t2) {
        return tree.getSummaryFromKeys(t1, t2);
    }

    /**
     *  When a raster view is requested, there are two possibilities:
     *  the signal has at least numRegions samples between t0 and t1,
     *  or it has less than numRegions samples.  In the latter case we
     *  use "exact" mode and snap the raster samples to the actual
     *  samples.  In the former case we use range queries to summarize
     *  multiple actual samples in each raster sample.
     */
    private class BTreeRasterView implements Signal.View<RangeSample<S>> {
        private final double t0, t1;
        private final int numRegions;
        private final boolean exact;
        private int t0_ord, t1_ord;
        public BTreeRasterView(double t0, double t1, int numRegions, boolean extrapolate) {
            double t0_ = Math.min(t0, t1);
            double t1_ = Math.max(t0, t1);
            t0_ord = tree.getOrdFromKeyFloor(t0_);
            t1_ord = tree.getOrdFromKeyFloor(t1_);

            if (extrapolate) {
                // "snap" t0 and t1 to the nearest actual sample strictly outside the viewfinder
                t0_ = tree.getKeyFromOrd(t0_ord);
                t1_ord = Math.min(tree.size()-1, t1_ord+1);
                t1_ = tree.getKeyFromOrd(t1_ord);
            }

            this.t0 = t0_;
            this.t1 = t1_;
            this.exact = numRegions > (t1_ord - t0_ord);
            this.numRegions = exact ? numRegions : (t1_ord - t0_ord);
        }
        public int getNumEvents() { return numRegions; }
        public double getTime(int index) {
            if (exact) return tree.getKeyFromOrd(t0_ord + index);
            return t0+(((t1-t0)*index)/numRegions);
        }
        public RangeSample<S> getSample(int index) {
            if (exact) {
                S sample = tree.getValFromOrd(t0_ord+index);
                return sample==null ? null : new RangeSample<S>(sample, sample);
            } else {
                double tfirst = getTime(index);
                double tsecond = getTime(index+1);
                if (tfirst==tsecond) {
                    // this case can occur if the signal's samples
                    // aren't evenly spaced; in effect we end up
                    // acting sort of like exact mode at times.
                    S sample = tree.getValFromKey(tfirst);
                    return sample==null ? null : new RangeSample<S>(sample, sample);
                } else {
                    Pair<S,S> highlow = tree.getSummaryFromKeys(tfirst, tsecond);
                    return highlow==null
                        ? null
                        : new RangeSample<S>(highlow.getKey(),
                                             highlow.getValue());
                }
            }
        }
    }


    // Page Storage //////////////////////////////////////////////////////////////////////////////

    private static CachingPageStorage ps = null;
    static <SS extends Sample>
        BTree<Double,SS,Pair<SS,SS>>
        getTree(Unboxed<SS> unboxer, LatticeOperation<SS> latticeOp) {
        if (ps==null)
            try {
                long highWaterMarkInBytes = 50 * 1024 * 1024;
                PageStorage fps = FilePageStorage.create();
                PageStorage ops = new OverflowPageStorage(new MemoryPageStorage(fps.getPageSize()), fps, highWaterMarkInBytes);
                ps = new CachingPageStorageWrapper(ops, 16 * 1024, false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        return new BTree<Double,SS,Pair<SS,SS>>
            (ps, UnboxedHalfDouble.instance, unboxer,
             new Summary<SS>(UnboxedHalfDouble.instance, unboxer, latticeOp));
    }

    private static class Summary<SS extends Sample>
        extends UnboxedPair<SS,SS>
        implements BTree.Summary<Double,SS,Pair<SS,SS>>,
        AssociativeCommutativeOperation<Pair<SS,SS>> {
        private final LatticeOperation<SS> latticeOp;
        private final Unboxed<Double> uk;
        private final Unboxed<SS> uv;

        public Summary(Unboxed<Double> uk, Unboxed<SS> uv, LatticeOperation<SS> latticeOp) {
            super(uv,uv);
            this.uk = uk;
            this.uv = uv;
            this.latticeOp = latticeOp;
        }
        public void call(byte[] buf_arg, int ofs_arg,
                         byte[] buf_result, int ofs_result) {
            System.arraycopy(buf_arg, ofs_arg+uk.getSize(),
                             buf_result, ofs_result,
                             uv.getSize());
            System.arraycopy(buf_arg, ofs_arg+uk.getSize(),
                             buf_result, ofs_result+uv.getSize(),
                             uv.getSize());
        }
        
        public void multiply(byte[] buf1, int ofs1,
                             byte[] buf2, int ofs2,
                             byte[] buf_dest, int ofs_dest) {
            latticeOp.glb(buf1, ofs1,
                          buf2, ofs2,
                          buf_dest, ofs_dest);
            latticeOp.lub(buf1, ofs1+uv.getSize(),
                          buf2, ofs2+uv.getSize(),
                          buf_dest, ofs_dest+uv.getSize());
        }
    };

}

