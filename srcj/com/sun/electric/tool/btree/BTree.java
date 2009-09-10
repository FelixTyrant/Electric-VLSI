/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BTree.java
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
package com.sun.electric.tool.btree;

import java.io.*;
import java.util.*;
import com.sun.electric.tool.btree.unboxed.*;

/**
 *  A B+Tree implemented using PageStorage.
 *
 *  http://www.youtube.com/watch?v=coRJrcIYbF4
 *
 *  This is a B-Plus-Tree; values are stored only in leaf nodes.
 *
 *  A value drawn from a monoid is associated with each node of the
 *  tree.  An interior node's value is the monoid-product of its
 *  childrens' values.  A query method is provided to return the
 *  monoid product of arbitrary contiguous ranges within the tree.
 *  This can be used to efficiently implement "min/max value over this
 *  range" queries.
 *
 *  We proactively split nodes as soon as they become full rather than
 *  waiting for them to become overfull.  This has a space overhead of
 *  1/NUM_KEYS_PER_PAGE, but puts an O(1) bound on the number of pages
 *  written per operation (number of pages read is still O(log n)).
 *  It also makes the walk routine tail-recursive.
 *
 *  Each node of the BTree uses one page of the PageStorage; we don't
 *  yet support situations where a single page is too small for one
 *  key and one value.
 *
 *  The coding style in this file is pretty unusual; it looks a lot
 *  like "Java as a better C".  This is mainly because Hotspot is
 *  remarkably good at inlining methods, but remarkably bad (still,
 *  even in Java 1.7) at figuring out when it's safe to unbox values.
 *  So I expend a lot of effort trying not to create boxed values, but
 *  don't worry at all about the overhead of method calls,
 *  particularly when made via "final" references (nearly always
 *  inlined).  I don't code this way very often -- I reserve this for
 *  the 2% of my code that bears 98% of the performance burden.
 *
 *  Possible feature: sibling-stealing
 *      http://en.wikipedia.org/wiki/B_sharp_tree
 *
 *  Possible feature: COWTree like Oracle's btrfs:
 *      http://dx.doi.org/10.1145/1326542.1326544
 *
 *  @author Adam Megacz <adam.megacz@sun.com>
 */
public class BTree
    <K extends Serializable & Comparable,
     V extends Serializable,
     S extends Serializable> {

    final PageStorage          ps;
    final UnboxedComparable<K> uk;
    final UnboxedMonoid<S>     monoid;    // XXX: would be nice if the monoid had acesss to the K-value too...
    final Unboxed<V>           uv;
    final UnboxedInt           ui = UnboxedInt.instance;

    private LeafNodeCursor<K,V,S>       leafNodeCursor;
    private InteriorNodeCursor<K,V,S>   interiorNodeCursor1;
    private InteriorNodeCursor<K,V,S>   interiorNodeCursor2;

    int                  rootpage;
    private       byte[]               buf1;
    private       byte[]               buf2;
    private final byte[] keybuf;

    public BTree(PageStorage ps, UnboxedComparable<K> uk, UnboxedMonoid<S> monoid, Unboxed<V> uv) {
        this.ps = ps;
        this.uk = uk;
        this.monoid = monoid;
        this.uv = uv;
        this.leafNodeCursor = new LeafNodeCursor<K,V,S>(this);
        this.interiorNodeCursor1 = new InteriorNodeCursor<K,V,S>(this);
        this.interiorNodeCursor2 = new InteriorNodeCursor<K,V,S>(this);
        this.rootpage = ps.createPage();
        this.buf1 = new byte[ps.getPageSize()];
        this.buf2 = new byte[ps.getPageSize()];
        this.keybuf = new byte[uk.getSize()];
        leafNodeCursor.initBuf(buf1);
        leafNodeCursor.setParentPageId(rootpage);
        ps.writePage(rootpage, buf1, 0);
    }

    public V get(K key) {
        uk.serialize(key, keybuf, 0);
        return walk(keybuf, 0, null, false);
    }

    public void put(K key, V val) {
        uk.serialize(key, keybuf, 0);
        walk(keybuf, 0, val, true);
    }
    

    /**
     *  B+Tree walking routine.
     *
     *  This is the hairiest part, so I arranged things to share a single
     *  codepath across all four operations (insert/replace/delete/find).
     *
     *  The routine is implemented using a loop rather than recursive
     *  calls because the JVM does not support tail recursion (and
     *  probably never will, because its lame security model is based
     *  on stack inspection).
     *
     *  On writes/deletes, this returns the previous value.
     */
    private V walk(byte[] key, int key_ofs, V val, boolean isPut) {
        int pageid = rootpage;
        byte[] buf  = this.buf1;
        byte[] buf2 = this.buf2;
        int idx = -1;

        LeafNodeCursor<K,V,S>       leafNodeCursor = this.leafNodeCursor;
        InteriorNodeCursor<K,V,S>   interiorNodeCursor = this.interiorNodeCursor1;
        InteriorNodeCursor<K,V,S>   parentNodeCursor = this.interiorNodeCursor2;
        NodeCursor cur = null;

        while(true) {
            ps.readPage(pageid, buf, 0);
            cur = LeafNodeCursor.isLeafNode(buf) ? leafNodeCursor : interiorNodeCursor;
            cur.setBuf(pageid, buf);

            if (isPut && cur.isFull()) {
                assert cur!=parentNodeCursor;
                assert cur.getBuf()!=parentNodeCursor.getBuf();
                int right = ps.createPage();
                if (pageid == rootpage) {
                    assert buf2!=cur.getBuf();
                    parentNodeCursor.initRoot(buf2);
                    parentNodeCursor.setChildPageId(0, pageid);
                    cur.setParentPageId(rootpage);
                    idx = 0;
                }
                int ofs = parentNodeCursor.insertNewChildAt(idx+1);
                parentNodeCursor.setChildPageId(idx+1, right);
                cur.split(right, parentNodeCursor.getBuf(), ofs);
                cur.writeBack();
                parentNodeCursor.writeBack();
                pageid = rootpage;
                continue;
            }

            idx = cur.search(key, key_ofs);
            int comp = cur.compare(idx, key, key_ofs);
            if (cur.isLeafNode()) {
                if (!isPut) return comp==0 ? leafNodeCursor.getVal(idx) : null;
                if (comp==0) {
                    if (val==null) throw new RuntimeException("deletion is not yet implemented");
                    return leafNodeCursor.setVal(idx, val);
                }
                leafNodeCursor.insertVal(idx+1, key, key_ofs, val);
                return null;
            } else {
                if (isPut && val==null)
                    throw new RuntimeException("need to adjust 'least value under X' on the way down for deletions");
                pageid = interiorNodeCursor.getChildPageId(idx);
                InteriorNodeCursor<K,V,S> ic = interiorNodeCursor; interiorNodeCursor = parentNodeCursor; parentNodeCursor = ic;
                byte[] b = buf; buf = buf2; buf2 = b;
                assert interiorNodeCursor!=parentNodeCursor;
                assert interiorNodeCursor.getBuf()!=parentNodeCursor.getBuf();
                continue;
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////////

    /** returns the first key, or null if tree is empty */                 public K    first() { throw new RuntimeException("not implemented"); }
    /** returns the last key, or null if tree is empty */                  public K    last() { throw new RuntimeException("not implemented"); }
    /** returns the least key which is greater than the one given */       public K    next(K key) { throw new RuntimeException("not implemented"); }
    /** returns the greatest key which is less than the one given */       public K    prev(K key) { throw new RuntimeException("not implemented"); }

    /** no error if key doesn't exist */                                   public void remove(K key) { throw new RuntimeException("not implemented"); }
    /** remove all entries */                                              public void clear() { throw new RuntimeException("not implemented"); }

    /** returns the number of keys in the BTree */                         public int  size() { throw new RuntimeException("not implemented"); }
    /** returns the number of keys strictly after the one given */         public int  sizeAfter(K key) { throw new RuntimeException("not implemented"); }
    /** returns the number of keys strictly after the one given */         public int  sizeBefore(K key) { throw new RuntimeException("not implemented"); }

    /** returns the key with the given ordinal index */                    public K    seek(int ordinal) { throw new RuntimeException("not implemented"); }
    /** returns the ordinal index of the given key */                      public int  ordinal(K key) { throw new RuntimeException("not implemented"); }

    //////////////////////////////////////////////////////////////////////////////

    /**
     *  A simple regression test
     *
     *  Feature: test the case where we delete all the entries; there
     *  are a lot of corner cases there.
     */
    public static void main(String[] s) throws Exception {
        if (s.length != 4) {
            System.err.println("");
            System.err.println("usage: java " + BTree.class.getName() + " <maxsize> <numops> <cachesize> <seed>");
            System.err.println("");
            System.err.println("  Creates a BTree and runs random operations on both it and an in-memory TreeMap.");
            System.err.println("  Reports any disagreements.");
            System.err.println("");
            System.err.println("    <maxsize>  maximum number of entries in the tree, or 0 for no limit");
            System.err.println("    <numops>   number of operations to perform, or 0 for no limit");
            System.err.println("    <seed>     seed for random number generator, in hex");
            System.err.println("");
            System.exit(-1);
        }
        Random rand = new Random(Integer.parseInt(s[3], 16));
        int cachesize = Integer.parseInt(s[2]);
        int numops = Integer.parseInt(s[1]);
        int maxsize = Integer.parseInt(s[0]);
        int size = 0;
        PageStorage ps = cachesize==0
            ? FilePageStorage.create()
            : new CachingPageStorage(FilePageStorage.create(), cachesize);
        BTree<Integer,Integer,Integer> btree =
            new BTree<Integer,Integer,Integer>(ps, UnboxedInt.instance, null, UnboxedInt.instance);
        TreeMap<Integer,Integer> tm = 
            new TreeMap<Integer,Integer>();

        int puts=0, gets=0, deletes=0, misses=0, inserts=0;
        long lastprint=0;

        // you can switch one of these off to gather crude performance measurements and compare them to TreeMap
        boolean do_tm = true;
        boolean do_bt = true;

        for(int i=0; numops==0 || i<numops; i++) {
            if (System.currentTimeMillis()-lastprint > 200) {
                lastprint = System.currentTimeMillis();
                System.out.print("\r puts="+puts+" gets="+(gets-misses)+"/"+gets+" deletes="+deletes);
            }
            int key = rand.nextInt() % 1000000;
            switch(rand.nextInt() % 2) {
                case 0: { // get
                    Integer tget = do_tm ? tm.get(key) : null;
                    Integer bget = do_bt ? btree.get(key) : null;
                    gets++;
                    if (do_tm && do_bt) {
                        if (tget==null && bget==null) { misses++; break; }
                        if (tget!=null && bget!=null && tget.equals(bget)) break;
                        System.out.print("\r puts="+puts+" gets="+(gets-misses)+"/"+gets+" deletes="+deletes);
                        System.out.println();
                        System.out.println();
                        throw new RuntimeException("  disagreement on key " + key + ": btree="+bget+", treemap="+tget);
                    }
                }

                case 1: { // put
                    int val = rand.nextInt();
                    if (do_tm) tm.put(key, val);
                    if (do_bt) btree.put(key, val);
                    puts++;
                    break;
                }

                case 2: // delete
                    deletes++;
                    break;
            }
        }
    }

}