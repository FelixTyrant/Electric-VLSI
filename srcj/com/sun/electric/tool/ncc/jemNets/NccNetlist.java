/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NccNetlist.java
 *
 * Copyright (c) 2003 Sun Microsystems and Free Software
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
package com.sun.electric.tool.ncc.jemNets;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Collection;
import java.util.Set;

import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.database.text.Name;

import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.basicA.TransitiveRelation;
import com.sun.electric.tool.ncc.jemNets.Wire;

/**
 * NCC's representation of a netlist.
 */
public class NccNetlist {
	private NccGlobals globals;
	private ArrayList wires, parts, ports;
	private String rootCellName;
	
	// ---------------------------- public methods ---------------------------
	public NccNetlist(Cell root, VarContext context, Netlist netlist, 
					  NccGlobals globals) {
		this.globals = globals;

		Visitor v = new Visitor(globals);
		HierarchyEnumerator.enumerateCell(root, context, netlist, v);
		wires = v.getWireList();
		parts = v.getPartList();
		ports = v.getPortList();
		String libNm = root.getLibrary().getName();
		String cellNm = root.getName();
		String viewNm = root.getView().getAbbreviation();
		rootCellName = libNm+":"+cellNm+"{"+viewNm+"}";
	}
	public ArrayList getWireArray() {return wires;}
	public ArrayList getPartArray() {return parts;}
	public ArrayList getPortArray() {return ports;}
	public String getRootCellName() {return rootCellName;}
}

/** map from netID to NCC Wire */
class Wires {
	private ArrayList wires = new ArrayList();
	private void growIfNeeded(int ndx) {
		while(ndx>wires.size()-1) wires.add(null);
	}
	/** Construct a Wire table. The mergedNetIDs argument specifies
	 * sets of net IDs that should point to the same Wire.
	 * @param mergedNetIDs contains sets of net IDs that must be merged
	 * into the same Wire. */
	public Wires(TransitiveRelation mergedNetIDs, 
	             HierarchyEnumerator.CellInfo info) {
		for (Iterator it=mergedNetIDs.getSetsOfRelatives(); it.hasNext();) {
			Set relatives = (Set) it.next();
			Iterator ni = relatives.iterator();
			if (!ni.hasNext()) continue;
			int netId = ((Integer)ni.next()).intValue();
			Wire w = get(netId, info);
			while (ni.hasNext()) {
				netId = ((Integer)ni.next()).intValue();
				growIfNeeded(netId);
				wires.set(netId, w);
			}
		}
	}
	public Wire get(int netID, HierarchyEnumerator.CellInfo info) {
		growIfNeeded(netID);
		Wire wire = (Wire) wires.get(netID);
		if (wire==null) {
			String wireNm = info.getUniqueNetName(netID, "/");
			wire = new Wire(wireNm, info.isGlobalNet(netID));
			wires.set(netID, wire);
		}
		return wire;
	}
	// return non-null entries of wires array
	// Eliminate duplicated Wires.
	public ArrayList getWireArray() {
		Set wireSet = new HashSet();
		wireSet.addAll(wires);
		wireSet.remove(null);
		ArrayList nonNullWires = new ArrayList();
		nonNullWires.addAll(wireSet);
		return nonNullWires;
	}
}

class NccCellInfo extends HierarchyEnumerator.CellInfo {
	private HashSet nodablesToDiscard = new HashSet();
	private HashMap nodableSizeMultipliers = new HashMap();
	private NccGlobals globals;

	// Compute a Nodable's hash code based upon its connectivity and Cell type
	private int computeNodableHashCode(Nodable no, Netlist netlist) {
		Cell c = (Cell) no.getProto();
		int hash = c.hashCode();
		for (Iterator it=c.getPorts(); it.hasNext();) {
			Export e = (Export) it.next();
			int[] netIDs = getPortNetIDs(no, e);
			for (int i=0; i<netIDs.length; i++) {
				int netID = netIDs[i];
				hash = (hash<<1) ^ netID;
			}
		}
		return hash;
	}
	
	private HashMap hashNodablesBasedOnConnectivity(Netlist netlist) {
		HashMap codeToNodable = new HashMap();
		for (Iterator it=netlist.getNodables(); it.hasNext();) {
			Nodable no = (Nodable) it.next();
			NodeProto np = no.getProto();
			if (np instanceof Cell) {
				Integer c = new Integer(computeNodableHashCode(no, netlist));
				LinkedList ll = (LinkedList) codeToNodable.get(c);
				if (ll==null) {
					ll = new LinkedList();
					codeToNodable.put(c, ll);
				}
				ll.add(no);
			}
		}
		return codeToNodable;
	}
	
	private boolean areParalleled(Nodable n1, Nodable n2, Netlist netlist) {
		Cell c = (Cell) n1.getProto();
		Cell c2 = (Cell) n2.getProto();
		if (c2!=c) return false;
		for (Iterator it=c.getPorts(); it.hasNext();) {
			Export e = (Export) it.next();
			int[] netIDs1 = getPortNetIDs(n1, e);
			int[] netIDs2 = getPortNetIDs(n2, e);
			for (int i=0; i<netIDs1.length; i++) {
				if (netIDs1[i]!=netIDs2[i]) return false;
			}
		}
		return true;
	}
	// Find all parallel Nodables. For each group of paralleled Nodables 
	// discard all Nodables but the first. Record the number of Nodables in 
	// parallel.
	private void findParallelNodables(LinkedList ll, Netlist netlist) {
		for (Iterator it=ll.iterator(); it.hasNext();) {
			Nodable first = (Nodable) it.next();
			it.remove();
			int count = 1;
			for (; it.hasNext();) {
				Nodable no = (Nodable) it.next();
				if (areParalleled(first, no, netlist)) {
					it.remove();
					nodablesToDiscard.add(no);
					count++;
				}
			}
			if (count>=2) nodableSizeMultipliers.put(first, new Integer(count));
		}
	}
	// Get size multiplier for nodable contained by current Cell	
	private int getSizeMultiplier(Nodable no) {
		Integer i = (Integer) nodableSizeMultipliers.get(no);
		return (i==null) ? 1 : i.intValue();
	}
	
	// ----------------------------- public methods ---------------------------
	public NccCellInfo(NccGlobals globals) {this.globals=globals;}

	// Record the information we need to merge parallel instances of the same 
	// Cell
	public void recordMergeParallelInfo() {
		Netlist netlist = getNetlist();
		HashMap codeToNodables = hashNodablesBasedOnConnectivity(netlist);
		for (Iterator it=codeToNodables.keySet().iterator(); it.hasNext();) {
			LinkedList ll = (LinkedList) codeToNodables.get(it.next());
			findParallelNodables(ll, netlist);
		}
	}
	public boolean isDiscardable(Nodable no) {
		return nodablesToDiscard.contains(no);
	}
	/** Get size multipler for everything instantiated by the current Cell */
	public int getSizeMultiplier() {
		if (isRootCell()) return 1;
		NccCellInfo parentInfo = (NccCellInfo) getParentInfo();
		int parentMult = parentInfo.getSizeMultiplier();
		int nodableMult = parentInfo.getSizeMultiplier(getParentInst());
		return parentMult * nodableMult;
	}
}

class Visitor extends HierarchyEnumerator.Visitor {
	// --------------------------- private data -------------------------------
	private static final boolean debug = false;
	private NccGlobals globals;
	private int depth = 0;

	/** map from netID to Wire */	 private Wires wires;
	/** all Parts in the net list */ private ArrayList parts = new ArrayList();
	/** all ports in the net list */ private ArrayList ports = new ArrayList();
	
	// --------------------------- private methods ----------------------------
	private void error(boolean pred, String msg) {globals.error(pred, msg);}
	private void spaces() {
		for (int i=0; i<depth; i++)	 globals.print(" ");
	}
	private void addMatchingNetIDs(List netIDs, 
	                               String pattern, 
								   HierarchyEnumerator.CellInfo rootInfo) {
		Cell rootCell = rootInfo.getCell();  								 
		for (Iterator it=rootCell.getPorts(); it.hasNext();) {
			Export e = (Export) it.next();
			int[] expNetIDs = rootInfo.getExportNetIDs(e);
			for (int i=0; i<expNetIDs.length; i++) {
				String expName = e.getNameKey().subname(i).toString();
				if (expName.equals(pattern)) netIDs.add(new Integer(expNetIDs[i]));
			}
		}
	}
	private void processExportConnectedAnnotation(
							TransitiveRelation mergedNetIDs, String note, 
	            			HierarchyEnumerator.CellInfo rootInfo) {
		StringTokenizer st = new StringTokenizer(note);
		st.nextToken();  // skip the keyword
		List netIDs = new ArrayList();
		while (st.hasMoreTokens()) {
			addMatchingNetIDs(netIDs, st.nextToken(), rootInfo);
		}
		for (int i=1; i<netIDs.size(); i++) {
			mergedNetIDs.theseAreRelated(netIDs.get(0), netIDs.get(i));
		}
	}
	private void doAnnotation(TransitiveRelation mergedNetIDs, String note,
							  HierarchyEnumerator.CellInfo rootInfo) {
		if (note.startsWith("exportsConnectedByParent")) {
			processExportConnectedAnnotation(mergedNetIDs, note, rootInfo);
		} else {
			System.out.println("Unrecognized NCC annotation: "+note);
		}
	}
	private void readCellAnnotations(TransitiveRelation mergedNetIDs,
	                                 HierarchyEnumerator.CellInfo rootInfo) {
		Cell rootCell = rootInfo.getCell();
		Variable nccVar = rootCell.getVar("ATTR_NCC");
		if (nccVar==null) return;
		Object annotation = nccVar.getObject();
		
		if (annotation instanceof String) {
			doAnnotation(mergedNetIDs, (String) annotation, rootInfo);
		} else if (annotation instanceof String[]) {
			String[] ss = (String[]) annotation;
			for (int i=0; i<ss.length; i++) {
				doAnnotation(mergedNetIDs, ss[i], rootInfo);
			}
		} else {
			System.out.println("ignoring bad NCC annotations: "+annotation+
			                   " on Cell: "+rootCell.getName());
		}
	}
	
	private void initWires(HierarchyEnumerator.CellInfo rootInfo) {
		TransitiveRelation mergedNetIDs = new TransitiveRelation();
		readCellAnnotations(mergedNetIDs, rootInfo);
		wires = new Wires(mergedNetIDs, rootInfo);
	}
	
	private void createPortsFromExports(HierarchyEnumerator.CellInfo rootInfo){
		HashSet portSet = new HashSet();
		Cell rootCell = rootInfo.getCell();
		for (Iterator it=rootCell.getPorts(); it.hasNext();) {
			Export e = (Export) it.next();
			int[] expNetIDs = rootInfo.getExportNetIDs(e);
			for (int i=0; i<expNetIDs.length; i++) {
				Wire wire = wires.get(expNetIDs[i], rootInfo);
				String expName = e.getNameKey().subname(i).toString();
				portSet.add(wire.addExportName(expName));
			}
		}
		// create exports for global schematic signals
		Netlist rootNetlist = rootInfo.getNetlist();
		Global.Set globals = rootNetlist.getGlobals();
		for (int i=0; i<globals.size(); i++) {
			Global global = globals.get(i);
			String globName = global.getName();
			int netIndex = rootNetlist.getNetIndex(global);
			Wire wire = wires.get(netIndex, rootInfo);
			portSet.add(wire.addExportName(globName));
		}
		
		for (Iterator it=portSet.iterator(); it.hasNext();) 
			ports.add(it.next());
	}
	
	
	/** 
	 * Get the Wire that's attached to port pi. The Nodable must be an instance
	 * of a PrimitiveNode. 
	 * @return the attached Wire.
	 */
	private Wire getWireForPortInst(PortInst pi, 
					     		    HierarchyEnumerator.CellInfo info) {
		NodeInst ni = pi.getNodeInst();
		NodeProto np = ni.getProto();
		error(!(np instanceof PrimitiveNode), "not PrimitiveNode");
		PortProto pp = pi.getPortProto();
		int[] netIDs = info.getPortNetIDs(ni, pp);
		error(netIDs.length!=1, "Primitive Port connected to bus?");
		return wires.get(netIDs[0], info);
	}

	private void buildMOS(NodeInst ni, Transistor.Type type, NccCellInfo info){
		String name = ni.getName();
		int mul = info.getSizeMultiplier();
//		if (mul!=1) {
//			globals.println("mul="+mul+" for "+
//						   info.getContext().getInstPath("/")+"/"+name);
//		}
		double width=0, length=0;
		if (globals.getOptions().checkSizes) {
			Dimension dim = ni.getTransistorSize(info.getContext());
			width = mul * dim.getWidth();
			length = dim.getHeight();
		}
		Wire s = getWireForPortInst(ni.getTransistorSourcePort(), info);
		Wire g = getWireForPortInst(ni.getTransistorGatePort(), info);
		Wire d = getWireForPortInst(ni.getTransistorDrainPort(), info);
		Part t = new Transistor(type, name, width, length, s, g, d);
		parts.add(t);								 
	}
	
	private void doPrimitiveNode(NodeInst ni, NodeProto np, NccCellInfo info) {
		NodeProto.Function func = ni.getFunction();
		if (func==NodeProto.Function.TRA4NMOS) {
			// schematic NMOS
			buildMOS(ni, Transistor.NTYPE, info);
		} else if (func==NodeProto.Function.TRA4PMOS) {
			// schematic PMOS
			buildMOS(ni, Transistor.PTYPE, info);
		} else if (func==NodeProto.Function.TRANMOS) {
			// layout NMOS
			buildMOS(ni, Transistor.NTYPE, info);
		} else if (func==NodeProto.Function.TRAPMOS) {
			// layout PMOS
			buildMOS(ni, Transistor.PTYPE, info);
		} else if (func==NodeProto.Function.RESIST) {
//		    error(true, "can't handle Resistors yet");
		} else {	
//		    globals.println("NccNetlist not handled: func="+
//		    			   func+" proto="+ni.getProto().toString());
//		    error(true, "unrecognized PrimitiveNode");
		}
	}
	
	
	// --------------------------- public methods -----------------------------
	public HierarchyEnumerator.CellInfo newCellInfo() {
		return new NccCellInfo(globals);
	}
	
	public boolean enterCell(HierarchyEnumerator.CellInfo ci) {
		NccCellInfo info = (NccCellInfo) ci;
		if (debug) {
			spaces();
			globals.println("Enter cell: " + info.getCell().getName());
			depth++;
		}
		if (info.isRootCell()) {
			initWires(info);
			createPortsFromExports(info);
		} 
		if (globals.getOptions().mergeParallelCells)  
			info.recordMergeParallelInfo();
		return true;
	}

	public void exitCell(HierarchyEnumerator.CellInfo info) {
		if (debug) {
			depth--;
			spaces();
			globals.println("Exit cell: " + info.getCell().getName());
		}
	}
	
	public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo ci) {
		NccCellInfo info = (NccCellInfo) ci;
		NodeProto np = no.getProto();
		if (np instanceof PrimitiveNode) {
			doPrimitiveNode((NodeInst)no, np, info);
			return false; 
		} else {
			error(!(np instanceof Cell), "expecting Cell");
			boolean expand = !info.isDiscardable(no);
//			if (!expand) {
//				globals.println("don't expand: "+
//					info.getContext().getInstPath("/")+"/"+no.getName());
//			}
			return expand;
		}
	}
	public ArrayList getWireList() {return wires.getWireArray();}
	public ArrayList getPartList() {return parts;}
	public ArrayList getPortList() {return ports;}
	
	public Visitor(NccGlobals globals) {this.globals=globals;}
}
