/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Ncc.java
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
package com.sun.electric.tool.ncc;

import java.util.Date;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.basic.NccUtils;

/** Compare potentially multiple cells in a hierarchy. 
 * <p>This is the class that should be used by programs wishing to perform
 * netlist comparison. */
public class NCC {
	private void prln(String s) {System.out.println(s);}

	private NCC() {}
	
    private NccResult compare1(Cell cell1, VarContext ctxt1,
    		                   Cell cell2, VarContext ctxt2,
							   NccOptions options) {
		if (options.operation==NccOptions.LIST_ANNOTATIONS) {
			ListNccAnnotations.doYourJob(cell1, cell2);
			return new NccResult(true, true, true);
		} else {
	    	Date before = new Date();
			NccResult result = new NccResult(false, false, false);
			if (options.operation==NccOptions.FLAT_TOP_CELL) {
				prln("Flat NCC top cell");
				result = NccUtils.compareAndPrintStatus(cell1, ctxt1, cell2, 
														ctxt2, null, options);
			} else if (options.operation==NccOptions.FLAT_EACH_CELL) {
				prln("Flat NCC every cell in the design");
				result = NccBottomUp.compare(cell1, cell2, 
				                             false, options.skipPassed, options);
			} else if (options.operation==NccOptions.HIER_EACH_CELL) {
				prln("Hierarchical NCC every cell in the design");
				result = NccBottomUp.compare(cell1, cell2, 
	                                         true, options.skipPassed, options);
			} else {
				LayoutLib.error(true, "bad operation: "+options.operation);
				return result;
			}
			System.out.println("Summary for all cells: "+result.summary(options.checkSizes));
			Date after = new Date();
			System.out.println("NCC command completed in: "+
			                   NccUtils.hourMinSec(before, after)+".");
			return result;
		}
    }
   
    // ------------------------- public method --------------------------------
    public static NccResult compare(Cell cell1, VarContext ctxt1, 
    		                        Cell cell2, VarContext ctxt2, 
									NccOptions options) {
    	NCC ncc = new NCC();
    	return ncc.compare1(cell1, ctxt1, cell2, ctxt2, options);
    }
}
