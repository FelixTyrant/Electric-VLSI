/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ScalarSignal.java
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

/**
 *  A crude implementation of ScalarSignal which implements
 *  getApproximation() for the case where tn=vd=0 by binary search.
 */
public abstract class ScalarSignalSimpleImpl implements ScalarSignal {

    private ScalarSignal.Approximation pa = null;
    private double tmin;
    private double tmax;
    private int    emax;

    // This binary search is done carefully so that it takes the
    // same search path as far as possible even for different
    // t0/t1 inputs.  That is, for example, why we don't use e0 to
    // help start the search for e1.  This ensures that any
    // caching done by the subclass is exploited as fully as
    // possible.
    private int getEventForTime(double t, boolean justLessThan) {
        if (pa==null) {
            this.pa = getPreferredApproximation();
            this.emax = pa.getNumEvents()-1;
            this.tmin = pa.getTime(0);
            this.tmax = pa.getTime(this.emax);
        }
        numLookups++;
        int emin  = 0;
        int emax  = this.emax;
        double tmin = this.tmin;
        double tmax = this.tmax;
        boolean last = true;
        while(true) {
            if (emin==emax) return emin;
            if (emin+1 == emax) return justLessThan ? emin : emax;
            double est = ((t-tmin)*(emax-emin))/(tmax-tmin);
            //int e = (emin+emax)/2;
            int e = emin + (last ? ((int)Math.ceil(est)) : ((int)Math.floor(est)));
            last = !last;
            if (e<=emin) return emin;
            if (e>=emax) return emax;
            double te = pa.getTime(e);
            steps++;
            if      (te < t) { emin = e; tmin = te; }
            else if (te > t) { emax = e; tmax = te; }
            else             return e;
        }
    }
    public static int misses = 0;
    public static int steps = 0;
    public static int numLookups = 0;

    public ScalarSignal.Approximation getApproximation(double t0, double t1, int td,
                                                       double v0, double v1, int vd) {
        if (vd!=0) throw new RuntimeException("not implemented");

        // FIXME: currently ignoring v0/v1
        int e0 = getEventForTime(t0, true);
        int e1 = getEventForTime(t1, false);
        return new ApproximationSimpleImpl(e0, e1, td==0 ? getPreferredApproximation().getTimeDenominator() : td);
    }

    private class ApproximationSimpleImpl implements ScalarSignal.Approximation {
        private final int    minEvent;
        private final int    maxEvent;
        private final int    td;
        private final double t0;
        private final double t1;
        public ApproximationSimpleImpl(int minEvent, int maxEvent, int td) {
            this.minEvent=minEvent;
            this.maxEvent=maxEvent;
            this.t0 = getPreferredApproximation().getTime(minEvent);
            this.t1 = getPreferredApproximation().getTime(maxEvent);
            this.td = td;
        }
        public int    getNumEvents() { return td+1; }
        public double getTime(int event) { return t0 + (event*(t1-t0))/td; }
        public double getValue(int event) {
            int e     = getEventForTime(getTime(event), /* FIXME: should interpolate */ true);
            return getPreferredApproximation().getValue(e);
        }
        public int    getTimeDenominator() { return td; }
        public int    getValueDenominator() { return getPreferredApproximation().getValueDenominator(); }
        public int    getEventWithMinValue() { throw new RuntimeException("not implemented"); }
        public int    getEventWithMaxValue() { throw new RuntimeException("not implemented"); }
        public int    getTimeNumerator(int event) { throw new RuntimeException("not implemented"); }
        public int    getValueNumerator(int event) { throw new RuntimeException("not implemented"); }
    }

}

