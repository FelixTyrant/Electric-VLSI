/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NccResult.java
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

import java.util.LinkedList;
import java.util.List;

import com.sun.electric.database.hierarchy.HierarchyEnumerator.NetNameProxy;
import com.sun.electric.tool.user.ncc.NccComparisonMismatches;

/** The result of running a netlist comparison */
public class NccResult {
	private boolean exportMatch;
	private boolean topologyMatch;
	private boolean sizeMatch;
	private NccGlobals globalData;
    private List<NccComparisonMismatches> comparisonMismatches;

	public NccResult(boolean exportNameMatch, boolean topologyMatch, 
			         boolean sizeMatch, NccGlobals globalData) {
		this.exportMatch = exportNameMatch;
		this.topologyMatch = topologyMatch;
		this.sizeMatch = sizeMatch;
		this.globalData = globalData;
        comparisonMismatches = new LinkedList<NccComparisonMismatches>();
	}
	/** Use this method to avoid holding the global data for two comparisons
	 * at the same time. */
	public void abandonNccGlobals() {globalData=null;}
	/** aggregate the result of multiple comparisons */
	public void andEquals(NccResult result) {
		exportMatch &= result.exportMatch;
		topologyMatch &= result.topologyMatch;
		sizeMatch &= result.sizeMatch;
		globalData = result.globalData;
		// Tricky: globalData is null if the Cell has a blackBox annotation
		// because that annotation means "don't perform a comparison".
        if ((globalData!=null && globalData.cantBuildNetlist()) ||
        	!result.exportMatch || !result.topologyMatch || !result.sizeMatch){
            NccComparisonMismatches cr = globalData.getComparisonResult();
            cr.setGlobalData(globalData);
            cr.setMatchFlags(result.exportMatch, result.topologyMatch, result.sizeMatch);
            comparisonMismatches.add(cr);
        }
	}
	/** No problem was found with Exports */ 
	public boolean exportMatch() {return exportMatch;}
	
	/** No problem was found with the network topology */
	public boolean topologyMatch() {return topologyMatch;}

	/** No problem was found with transistor sizes */
	public boolean sizeMatch() {return sizeMatch;}
	
	/** No problem was found */
	public boolean match() {return exportMatch && topologyMatch && sizeMatch;}
	
	public NetEquivalence getNetEquivalence() {
		NetNameProxy[][] equivNets;
		if (globalData==null) {
			// For severe netlist errors, NCC doesn't even construct globalData.
			// Create a NetEquivalence that has no matching nets.
			equivNets = new NetNameProxy[2][];
			equivNets[0] = new NetNameProxy[0];
			equivNets[1] = new NetNameProxy[0];
		} else {
			equivNets = globalData.getEquivalentNets();
		}
		return new NetEquivalence(equivNets);
	}
	
	public String summary(boolean checkSizes) {
		String s;
		if (exportMatch) {
			s = "exports match, ";
		} else {
			s = "exports mismatch, ";
		}
		if (topologyMatch) {
			s += "topologies match, ";
		} else {
			s += "topologies mismatch, ";
		}
		if (!checkSizes) {
			s += "sizes not checked";
		} else if (sizeMatch) {
			s += "sizes match";
		} else {
			s += "sizes mismatch";
		}
		return s;
	}

    public NccGlobals getGlobalData() {
        return globalData;
    }
    
    public void addComparisonMismatches(NccComparisonMismatches cr) {
        comparisonMismatches.add(cr);
    }

    public List<NccComparisonMismatches> getAllComparisonMismatches() {
        return comparisonMismatches;
    }
}
