/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SizeOffset.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.technology;

/**
 * The SizeOffset object describes the difference between the stored bounds of
 * a NodeInst and the displayed/selected bounds.
 * <P>
 * In Electric, extra space may surround a NodeInst, in order to leave room
 * for expansion of the definition.  For example, in the MOCMOS technology,
 * a metal-1/metal-2 contact is 5x5 (in memory) but when displayed, it is only
 * 4x4.  The extra space does not scale, meaning that if you stretch the node
 * so that it appears to be 10x10, then it will be 11x11 in memory.
 * <P>
 * The distance from each edge is stored in a SizeOffset object.  For the
 * Via described above, all four offsets would be 0.5 to indicate a half-unit
 * surround between the stored and displayed/selected bounds.
 */
public class SizeOffset
{
	private double lx, ly, hx, hy;

	/**
	 * Constructor to create a SizeOffset from the specified parameters.
	 * @param lx the low-X offset (distance from left side to actual bounds).
	 * @param ly the low-Y offset (distance from bottom side to actual bounds).
	 * @param hx the high-X offset (distance from left side to actual bounds).
	 * @param hy the high-Y offset (distance from top side to actual bounds).
	 */
	public SizeOffset(double lx, double ly, double hx, double hy)
	{
		this.lx = lx;
		this.ly = ly;
		this.hx = hx;
		this.hy = hy;
	}

	/**
	 * Routine to return the low-X offset of this SizeOffset.
	 * The low-X offset is the distance from the left side to the acutal bounds.
	 * @return the low-X offset of this SizeOffset.
	 */
	public double getLowXOffset() { return lx; }

	/**
	 * Routine to return the high-X offset of this SizeOffset.
	 * The high-X offset is the distance from the right side to the acutal bounds.
	 * @return the high-X offset of this SizeOffset.
	 */
	public double getHighXOffset() { return hx; }

	/**
	 * Routine to return the low-Y offset of this SizeOffset.
	 * The low-Y offset is the distance from the bottom side to the acutal bounds.
	 * @return the low-Y offset of this SizeOffset.
	 */
	public double getLowYOffset() { return ly; }

	/**
	 * Routine to return the high-Y offset of this SizeOffset.
	 * The high-Y offset is the distance from the top side to the acutal bounds.
	 * @return the high-Y offset of this SizeOffset.
	 */
	public double getHighYOffset() { return hy; }

	/**
	 * Returns a printable version of this SizeOffset.
	 * @return a printable version of this SizeOffset.
	 */
	public String toString()
	{
		return "SizeOffset {X:[" + lx + "," + hx + "] Y:[" + ly + "," + hy + "]}";
	}
}
