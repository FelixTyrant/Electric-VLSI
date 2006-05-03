/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ERectangle.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.text.ImmutableArrayList;
import java.awt.geom.Rectangle2D;

/**
 * The <code>ERectangle</code> immutable class defines a point representing
 * defined by a location (x,&nbsp;y) and dimension (w&nbsp;x&nbsp;h).
 * <p>
 * This class is used in Electric database.
 * NOTE: This class can't be serializable because of Rectangle2D.Double
 */
public class ERectangle extends Rectangle2D.Double {
    public static final ERectangle[] NULL_ARRAY = {};
    public static final ImmutableArrayList<ERectangle> EMPTY_LIST = new ImmutableArrayList<ERectangle>(NULL_ARRAY);
    
    /**
     * Constructs and initializes a <code>ERectangle</code>
     * from the specified double coordinates.
     * @param x,&nbsp;y the coordinates of the upper left corner
     * of the newly constructed <code>Rectangle2D</code>
     * @param w the width of the
     * newly constructed <code>Rectangle2D</code>
     * @param h the height of the
     * newly constructed <code>Rectangle2D</code>
     */
    public ERectangle(double x, double y, double w, double h) {
	    super.setRect(x, y, w, h);
	}

	public void setRect(double x, double y, double w, double h) {
        throw new UnsupportedOperationException();
	}

	public void setRect(Rectangle2D r) {
        throw new UnsupportedOperationException();
	}
}
