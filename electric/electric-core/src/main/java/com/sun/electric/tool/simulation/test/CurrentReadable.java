/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CurrentReadable.java
 * Written by Tom O'Neill, Sun Microsystems.
 *
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.tool.simulation.test;

/**
 * Device-independent interface to something (e.g., power supply or digitial
 * multimeter) that can read back current
 */
public interface CurrentReadable {

    /**
     * Measures current using default range and resolution.
     * 
     * @return current in Amps
     */
    public float readCurrent();

    /**
     * Measures current using range appropriate for <code>ampsExpected</code>,
     * and resolution of <code>ampsResolution</code>.
     * 
     * @param ampsExpected
     *            expected value of current in amps, for range setting
     * @param ampsResolution
     *            desired resolution for measurement, in amps
     * @return current in Amps
     */
    public float readCurrent(float ampsExpected, float ampsResolution);

}
