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
 *  Internal use only; kind of a hack.  This is just a "parser" for
 *  the page format.  Only this class knows the internal structure of
 *  a leaf node page.
 *
 *    int: pageid of parent
 *    int: 0
 *    int: pageid of left neighbor (not used)
 *    int: pageid of right neighbor (not used)
 *    int: number of key-value pairs on this page
 *    repeat
 *       key: key
 *       val: val
 *
 *  Possible feature: store the buckets of an interior node
 *  internally as a simple balanced tree (a splay tree?).  The
 *  System.arraycopy()'s are scaling very poorly as the page size
 *  increases.
 *
 *  Possible feature: try to share more code with InteriorNodeCursor.
 */
class LeafNodeCursor
    <K extends Serializable & Comparable,
     V extends Serializable,
     S extends Serializable>
    extends NodeCursor<K,V,S> {

    private        final int LEAF_HEADER_SIZE;
    private        final int LEAF_ENTRY_SIZE;
    private        final int LEAF_MAX_BUCKETS;
    private int numbuckets = 0;

    public int getMaxBuckets() { return LEAF_MAX_BUCKETS; }

    public LeafNodeCursor(BTree<K,V,S> bt) {
        super(bt);
        this.LEAF_HEADER_SIZE = 5 * SIZEOF_INT;
        this.LEAF_ENTRY_SIZE  = bt.uk.getSize() + bt.uv.getSize();
        this.LEAF_MAX_BUCKETS = (ps.getPageSize() - LEAF_HEADER_SIZE) / LEAF_ENTRY_SIZE;
    }

    public static boolean isLeafNode(byte[] buf) { return UnboxedInt.instance.deserializeInt(buf, 1*SIZEOF_INT)==0; }

    public void setBuf(int pageid, byte[] buf) {
        assert isLeafNode(buf);
        super.setBuf(pageid, buf);
        numbuckets = bt.ui.deserializeInt(buf, 4*SIZEOF_INT);
    }
    public void initBuf(int pageid, byte[] buf) {
        this.buf = buf;
        this.pageid = pageid;
        bt.ui.serializeInt(0, buf, 1*SIZEOF_INT);
        setNumBuckets(0);
    }
    public int  getParentPageId() { return bt.ui.deserializeInt(buf, 0*SIZEOF_INT); }
    public void setParentPageId(int pageid) { bt.ui.serializeInt(pageid, buf, 0*SIZEOF_INT); }
    public int  getLeftNeighborPageId() { return bt.ui.deserializeInt(buf, 2*SIZEOF_INT); }
    public int  getRightNeighborPageId() { return bt.ui.deserializeInt(buf, 3*SIZEOF_INT); }
    public void setNumBuckets(int num) { bt.ui.serializeInt(numbuckets = num, buf, 4*SIZEOF_INT); }
    public int  getNumBuckets() { return numbuckets; }
    public int  compare(byte[] key, int key_ofs, int keynum) {
        if (keynum<0) return 1;
        if (keynum>=getNumBuckets()) return -1;
        return bt.uk.compare(key, key_ofs, buf, LEAF_HEADER_SIZE + keynum*LEAF_ENTRY_SIZE);
    }
    public V getVal(int slot) {
        return bt.uv.deserialize(buf, LEAF_HEADER_SIZE + bt.uk.getSize() + LEAF_ENTRY_SIZE*slot);
    }

    /** returns the value previously in the slot */
    public V setVal(int slot, V val) {
        assert val!=null;
        int pos = LEAF_HEADER_SIZE + bt.uk.getSize() + LEAF_ENTRY_SIZE*slot;
        V ret = bt.uv.deserialize(buf, pos);
        bt.uv.serialize(val, buf, pos);
        writeBack();
        return ret;
    }

    /**
     *  Insert a key/value pair at the designated slot;
     */
    public void insertVal(int slot, byte[] key, int key_ofs, V val) {
        assert val!=null;
        assert getNumBuckets() < getMaxBuckets();
        System.arraycopy(buf,
                         LEAF_HEADER_SIZE + LEAF_ENTRY_SIZE*slot,
                         buf,
                         LEAF_HEADER_SIZE + LEAF_ENTRY_SIZE*(slot+1),
                         (getNumBuckets()-slot)*LEAF_ENTRY_SIZE);
        setNumBuckets(getNumBuckets()+1);
        System.arraycopy(key, key_ofs, buf, LEAF_HEADER_SIZE + LEAF_ENTRY_SIZE*slot, bt.uk.getSize());
        setVal(slot, val);

        writeBack();
    }

    protected void scoot(byte[] oldbuf, int endOfBuf) {
        int len = LEAF_HEADER_SIZE + LEAF_ENTRY_SIZE * (getMaxBuckets()/2);
        System.arraycopy(oldbuf, len,
                         buf, LEAF_HEADER_SIZE,
                         endOfBuf - len);
    }

    public boolean isFull() { return getNumBuckets() >= getMaxBuckets(); }
    public boolean isLeafNode() { return true; }
    protected int endOfBuf() { return LEAF_HEADER_SIZE + LEAF_ENTRY_SIZE * getNumBuckets(); }
    public void getKey(int keynum, byte[] key, int key_ofs) {
        System.arraycopy(buf, LEAF_HEADER_SIZE + LEAF_ENTRY_SIZE*keynum, key, key_ofs, bt.uk.getSize());
    }
}
