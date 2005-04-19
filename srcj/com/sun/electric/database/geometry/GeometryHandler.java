/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GeometryHandler.java
 * Written by Gilda Garreton, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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
package com.sun.electric.database.geometry;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Area;
import java.awt.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Comparator;

/**
 * To handle merge operation. Two different classes have been proposed
 * and this interface would handle the implementation
 * @author  Gilda Garreton
 */
public abstract class GeometryHandler {
    HashMap layers;
    public static final int ALGO_MERGE = 0;
    public static final int ALGO_QTREE = 1;
    public static final int ALGO_SWEEP = 2;
    public static final ShapeSort shapeSort = new ShapeSort();
    public static final AreaSort areaSort = new AreaSort();

    /**
     * Method to create appropiate GeometryHandler depending on the mode
     * @param mode
     * @param root
     * @return
     */
    public static GeometryHandler createGeometryHandler(int mode, int initialSize, Rectangle2D root)
    {
        switch (mode)
        {
            case GeometryHandler.ALGO_MERGE:
                return new PolyMerge();
            case GeometryHandler.ALGO_QTREE:
                return new PolyQTree(root);
            case GeometryHandler.ALGO_SWEEP:
                return new PolySweepMerge(initialSize);
        }
        return null;
    }

    public GeometryHandler()
    {
        layers = new HashMap();
    }

    /**
     * Special constructor in case of using huge amount of memory. E.g. ERC
     * @param initialSize
     */
    public GeometryHandler(int initialSize)
    {
        layers = new HashMap(initialSize);
    }

    // To insert new element into handler
	public void add(Object key, Object value, boolean fasterAlgorithm) {;}

	// To add an entire GeometryHandler like collections
	public void addAll(GeometryHandler subMerge, AffineTransform tTrans) {;}

    /**
	 * Method to subtract a geometrical object from the merged collection.
	 * @param key the key that this Object sits on.
	 * @param element the Object to merge.
	 */
    public void subtract(Object key, Object element)
    {
        System.out.println("Error: subtract not implemented for GeometryHandler subclass " + this.getClass().getName());
    }

    /**
     * Method to subtract all geometries stored in hash map from corresponding layers
     * @param map
     */
    public void subtractAll(HashMap map)
    {
        System.out.println("Error: subtractAll not implemented for GeometryHandler subclass " + this.getClass().getName());
    }

	/**
	 * Access to keySet to create a collection for example.
	 */
	public Collection getKeySet()
	{
		return (layers.keySet());
	}

	/**
	 * Access to keySet with iterator
	 * @return iterator for keys in hashmap
	 */
	public Iterator getKeyIterator()
	{
		return (getKeySet().iterator());
	}

	/**
	 * Iterator among all layers inserted.
	 * @return an iterator over all layers inserted.
	 */
	public Iterator getIterator()
	{
		return (layers.values().iterator());
	}

	/**
	 * To retrieve leave elements from internal structure
	 * @param layer current layer under analysis
	 * @param modified to avoid retrieving original polygons
	 * @param simple to obtain simple polygons
	 */
	public Collection getObjects(Object layer, boolean modified, boolean simple)
    {
        System.out.println("Error: getObjects not implemented for GeometryHandler subclass " + this.getClass().getName());
        return null;
    }

    /**
     * Method to perform operations after no more elemenets will
     * be added. Valid for PolySweepMerge
     * @param merge true if polygons must be merged otherwise non-overlapping polygons will be generated.
     */
    public void postProcess(boolean merge)
    {
        if (!merge) System.out.println("Error: postProcess not implemented for GeometryHandler subclass " + this.getClass().getName());
    }

    /**
     * Auxiliar class to sort shapes in array
     */
    public static class ShapeSort implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            double bb1 = ((Shape)o1).getBounds2D().getX();
            double bb2 = ((Shape)o2).getBounds2D().getX();
            // Sorting along X
            if (bb1 < bb2) return -1;
            else if (bb1 > bb2) return 1;
            return (0); // identical
        }
    }

    /**
     * Auxiliar class to sort areas in array
     */
    public static class AreaSort implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            double bb1 = ((Area)o1).getBounds2D().getX();
            double bb2 = ((Area)o2).getBounds2D().getX();
            // Sorting along X
            if (bb1 < bb2) return -1;
            else if (bb1 > bb2) return 1;
            return (0); // identical
        }
    }
}
