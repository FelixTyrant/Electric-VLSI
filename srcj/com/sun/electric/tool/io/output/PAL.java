/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PAL.java
 * Input/output tool: PAL Netlist output
 * Written by Steven M. Rubin, Sun Microsystems.
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
package com.sun.electric.tool.io.output;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.PrimitiveNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This is the netlister for PAL.
 */
public class PAL extends Output
{
	private Cell topCell;
	private List equations;
	private Set internalSymbols;
	private Set externalSymbols;

	/**
	 * The main entry point for PAL deck writing.
	 * @param cell the top-level cell to write.
	 * @param filePath the disk file to create with PAL.
	 */
	public static void writePALFile(Cell cell, VarContext context, String filePath)
	{
		PAL out = new PAL();
		if (out.openTextOutputStream(filePath)) return;
		out.initialize(cell);
		PALNetlister netlister = new PALNetlister(out);
		Netlist netlist = cell.getNetlist(true);
		HierarchyEnumerator.enumerateCell(cell, context, netlist, netlister);
		out.terminate(cell);
		if (out.closeTextOutputStream()) return;
		System.out.println(filePath + " written");
	}

	/**
	 * Creates a new instance of the PAL netlister.
	 */
	PAL()
	{
	}

	private void initialize(Cell cell)
	{
		topCell = cell;
		equations = new ArrayList();
		internalSymbols = new HashSet();
		externalSymbols = new HashSet();
	}

	private void terminate(Cell cell)
	{
		// initialize the deck
		printWriter.println("module " + cell.getName());
		printWriter.println("title 'generated by Electric'");

		// write the external and internal symbols
		int pinNumber = 1;
		for(Iterator it = externalSymbols.iterator(); it.hasNext(); )
		{
			String symbol = (String)it.next();
			printWriter.println("    " + symbol + " pin " + pinNumber + ";");
			pinNumber++;
		}
		for(Iterator it = internalSymbols.iterator(); it.hasNext(); )
		{
			String symbol = (String)it.next();
			printWriter.println("    " + symbol + " = 0,1;");
		}

		// write the equations
		printWriter.println("");
		printWriter.println("equations");
		for(Iterator it = equations.iterator(); it.hasNext(); )
		{
			String eq = (String)it.next();
			printWriter.println("    " + eq + ";");
		}

		// end of deck
		printWriter.println("");
		printWriter.println("end " + cell.describe());
	}

	/** PAL Netlister */
	private static class PALNetlister extends HierarchyEnumerator.Visitor
	{
		private PAL pal;

		PALNetlister(PAL pal)
		{
			super();
			this.pal = pal;
		}

		public boolean enterCell(HierarchyEnumerator.CellInfo info) { return true; }

		public void exitCell(HierarchyEnumerator.CellInfo info) {}   

		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
		{
			// if this is a cell instance, keep recursing down
			NodeProto np = no.getProto();
			if (!(np instanceof PrimitiveNode)) return true;

			// Nodable is NodeInst because it is primitive node
			NodeInst ni = (NodeInst)no;

			// must be a logic gate
			PrimitiveNode.Function fun = ni.getFunction();
			if (fun != PrimitiveNode.Function.GATEAND && fun != PrimitiveNode.Function.GATEOR &&
				fun != PrimitiveNode.Function.GATEXOR && fun != PrimitiveNode.Function.BUFFER) return false;

			String funName = "";
			if (fun == PrimitiveNode.Function.GATEAND) funName = "&"; else
			if (fun == PrimitiveNode.Function.GATEOR) funName = "#"; else
				if (fun == PrimitiveNode.Function.GATEXOR) funName = "$";

			// find output
			Connection outputCon = null;
			for(Iterator it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = (Connection)it.next();
				PortInst pi = con.getPortInst();
				if (pi.getPortProto().getName().equals("y")) { outputCon = con;   break; }
			}
			if (outputCon == null)
			{
				System.out.println("ERROR: output port is not connected on " + ni.describe() + " in cell " + ni.getParent().describe());
				return false;
			}

			Netlist netlist = info.getNetlist();
			StringBuffer sb = new StringBuffer();
			if (outputCon.isNegated()) sb.append("!");
			Network oNet = netlist.getNetwork(outputCon.getPortInst());
			sb.append(getNetName(oNet, info) + " =");
			int count = 0;
			for(Iterator it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = (Connection)it.next();
				PortInst pi = con.getPortInst();
				if (!pi.getPortProto().getName().equals("a")) continue;
				if (count == 1) sb.append(" " + funName);
				count++;
				sb.append(" ");
				if (con.isNegated()) sb.append("!");
				Network net = netlist.getNetwork(con.getArc(), 0);
				if (net == null) continue;
				sb.append(getNetName(net, info));
			}
			pal.equations.add(sb.toString());
			return false;
		}

		private String getNetName(Network net, HierarchyEnumerator.CellInfo info)
		{
			Network originalNet = net;
			HierarchyEnumerator.CellInfo originalInfo = info;
			for(;;)
			{
				Network higher = info.getNetworkInParent(net);
				if (higher == null) break;
				net = higher;
				info = info.getParentInfo();
			}
			if (net.isExported() && info.getCell() == pal.topCell)
			{
				String exportName = net.describe();
				pal.externalSymbols.add(exportName);
				return exportName;
			}

			String internalName = info.getUniqueNetName(net, ".");
			pal.internalSymbols.add(internalName);
			return internalName;
		}
	}
}
