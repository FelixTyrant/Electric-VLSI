/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: OutputVerilog.java
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
package com.sun.electric.tool.io;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.io.OutputTopology;
import com.sun.electric.tool.io.Output;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.User;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Date;

/**
 * This is the Simulation Interface tool.
 */
public class OutputVerilog extends OutputTopology
{
	/* maximum size of output line */						private static final int MAXDECLARATIONWIDTH = 80;
	/* name of inverters generated from negated wires */	private static final String IMPLICITINVERTERNODENAME = "Imp";
	/* name of signals generated from negated wires */		private static final String IMPLICITINVERTERSIGNAME = "ImpInv";

	/** key of Variable holding verilog code. */			public static final Variable.Key VERILOG_CODE_KEY = ElectricObject.newKey("VERILOG_code");
	/** key of Variable holding verilog declarations. */	public static final Variable.Key VERILOG_DECLARATION_KEY = ElectricObject.newKey("VERILOG_declaration");
	/** key of Variable holding verilog wire time. */		public static final Variable.Key WIRE_TYPE_KEY = ElectricObject.newKey("SIM_verilog_wire_type");
	/** key of Variable holding verilog templates. */		public static final Variable.Key VERILOG_TEMPLATE_KEY = ElectricObject.newKey("ATTR_verilog_template");

	private boolean mustBackAnnotate;
	private int unconnectedNet;

	public static boolean writeVerilogFile(Cell cell, String filePath)
	{
		boolean error = false;
		OutputVerilog out = new OutputVerilog();
		out.mustBackAnnotate = false;
		if (out.openTextOutputStream(filePath)) error = true;
		if (out.writeCell(cell)) error = true;
		if (out.closeTextOutputStream()) error = true;
		if (!error) System.out.println(filePath + " written");
		if (out.mustBackAnnotate)
			System.out.println("Back-annotation information has been added (library must be saved)");
		return error;
	}

	/**
	 * Creates a new instance of Verilog
	 */
	OutputVerilog()
	{
	}

	protected void start()
	{
		// write header information
		printWriter.print("/* Verilog for cell " + topCell.describe() + " from Library " + topCell.getLibrary().getLibName() + " */\n");
		if (User.isIncludeDateAndVersionInOutput())
		{
			SimpleDateFormat sdf = new SimpleDateFormat("EEE MMMM dd, yyyy HH:mm:ss");
			printWriter.print("/* Created on " + sdf.format(topCell.getCreationDate()) + " */\n");
			printWriter.print("/* Last revised on " + sdf.format(topCell.getRevisionDate()) + " */\n");
			printWriter.print("/* Written on " + sdf.format(new Date()) + " by Electric VLSI Design System, version " + Version.CURRENT + " */\n");
		} else
		{
			printWriter.print("/* Written by Electric VLSI Design System */\n");
		}
		emitCopyright("/* ", " */");

		// gather all global signal names
		Netlist netList = getNetlistForCell(topCell);
		Global.Set globals = netList.getGlobals();
		int globalSize = globals.size();
		if (globalSize > 0)
		{
			printWriter.print("\nmodule glbl();\n");
			for(int i=0; i<globalSize; i++)
			{
				Global global = (Global)globals.get(i);
//				if (global == PWRPORT)
//				{
//					printWriter.print("    supply1 " + global.getName() + ";\n");
//				} else if (global == GNDPORT)
//				{
//					printWriter.print("    supply0 " + global.getName() + ";\n");
//				} else
					if (Simulation.getVerilogUseTrireg())
				{
					printWriter.print("    trireg " + global.getName() + ";\n");
				} else
				{
					printWriter.print("    wire " + global.getName() + ";\n");
				}
			}
			printWriter.print("endmodule\n");
		}
	}

	protected void done()
	{
	}

	/**
	 * Method to write cellGeom
	 */
	protected void writeCellTopology(Cell cell, OutputTopology.CellNets cn)
	{
		// use attached file if specified
		Variable behaveFileVar = cell.getVar("SIM_verilog_behave_file");
		if (behaveFileVar != null)
		{
			printWriter.print("`include \"" + behaveFileVar.getObject() + "\"\n");
			return;
		}

		// use library behavior if it is available
		Cell verViewCell = cell.otherView(View.VERILOG);
		if (verViewCell != null)
		{
			Variable var = verViewCell.getVar(Cell.CELL_TEXT_KEY);
			if (var != null)
			{
				String [] stringArray = (String [])var.getObject();
				for(int i=0; i<stringArray.length; i++)
					printWriter.print(stringArray[i] + "%s\n");
			}
			return;
		}

//		// make sure that all nodes and networks have names on them
//		addednames = 0;
//		if (asktool(net_tool, x_("name-nodes"), (INTBIG)np) != 0) addednames++;
//		if (asktool(net_tool, x_("name-nets"), (INTBIG)np) != 0) addednames++;
//		if (addednames != 0)
//		{
//			mustBackAnnotate = true;
//			net_endbatch();
//		}
//
//		// prepare arcs to store implicit inverters
//		impinv = 1;
//		for(ai = np->firstarcinst; ai != NOARCINST; ai = ai->nextarcinst)
//			ai->temp1 = 0;
//		for(ai = np->firstarcinst; ai != NOARCINST; ai = ai->nextarcinst)
//		{
//			if ((ai->userbits&ISNEGATED) == 0) continue;
//			if ((ai->userbits&REVERSEEND) == 0)
//			{
//				ni = ai->end[0].nodeinst;
//				pi = ai->end[0].portarcinst;
//			} else
//			{
//				ni = ai->end[1].nodeinst;
//				pi = ai->end[1].portarcinst;
//			}
//			if (ni->proto == sch_bufprim || ni->proto == sch_andprim ||
//				ni->proto == sch_orprim || ni->proto == sch_xorprim)
//			{
//				if (Simulation.getVerilogUseAssign()) continue;
//				if (estrcmp(pi->proto->protoname, x_("y")) == 0) continue;
//			}
//
//			// must create implicit inverter here
//			ai->temp1 = impinv;
//			if (ai->proto != sch_busarc) impinv++; else
//			{
//				net = ai->network;
//				if (net == NONETWORK) impinv++; else
//					impinv += net->buswidth;
//			}
//		}

		// gather networks in the cell
		Netlist netList = getNetlistForCell(cell);

		// write the module header
		printWriter.print("\n");
		StringBuffer sb = new StringBuffer();
		sb.append("module ");
//		if (paramname != 0) sb.append(getSafeNetName(paramname)); else
			sb.append(getUniqueCellName(cell));
		sb.append("(");
		boolean first = true;
		for(Iterator it = cn.portInfo.iterator(); it.hasNext(); )
		{
			OutputTopology.NetInfo nin = (OutputTopology.NetInfo)it.next();
			if (nin.temp1 <= 1 || nin.temp1 >= 6) continue;
			if (!first) sb.append(", ");
			sb.append(nin.name);
			first = false;
		}
		sb.append(");");
		writeLongLine(sb.toString());

		// look for "wire/trireg" overrides
		for(Iterator it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			Variable var = ai.getVar(WIRE_TYPE_KEY);
			if (var == null) continue;
			String wireType = var.getObject().toString();
			int overrideValue = 0;
			if (wireType.equalsIgnoreCase("wire")) overrideValue = 1; else
				if (wireType.equalsIgnoreCase("trireg")) overrideValue = 2;
			int busWidth = netList.getBusWidth(ai);
			for(int i=0; i<busWidth; i++)
			{
				JNetwork net = netList.getNetwork(ai, i);
				OutputTopology.WireList wl = (OutputTopology.WireList)cn.wireLists.get(net);
				if (wl == null) continue;
				wl.flags = overrideValue;
			}
		}

		// write description of formal parameters to module
		first = true;
		for(Iterator it = cn.portInfo.iterator(); it.hasNext(); )
		{
			OutputTopology.NetInfo nin = (OutputTopology.NetInfo)it.next();
			if (nin.temp1 > 1 && nin.temp1 < 6)
			{
				String portType = "input";
				switch (nin.temp1)
				{
					case 4: case 5: portType = "output";  break;
				}
				printWriter.print("  " + portType);
				if (nin.low > nin.high)
				{
					printWriter.print(" " + nin.name + ";");
				} else
				{
					int low = nin.low, high = nin.high;
					if ((nin.temp1&1) != 0)
					{
						low = nin.high;   high = nin.low;
					}
					printWriter.print(" [" + low + ":" + high + "] " + nin.name + ";");
				}
				OutputTopology.WireList wl = nin.wires[0];
				if (wl != null && wl.flags != 0)
				{
					if (wl.flags == 1) printWriter.print("  wire"); else
						printWriter.print("  trireg");
					printWriter.print(" " + nin.name + ";");
				}
				printWriter.print("\n");
				first = false;
			}
		}
		if (!first) printWriter.print("\n");
//		public void setUserBit(boolean bit) { this.bit = bit; }
//		public boolean getUserBit() { return bit; }

		// describe power and ground nets
		if (cn.pwrNet != null) printWriter.print("  supply1 vdd;\n");
		if (cn.gndNet != null) printWriter.print("  supply0 gnd;\n");

		// determine whether to use "wire" or "trireg" for networks
		String wireType = "wire";
		if (Simulation.getVerilogUseTrireg()) wireType = "trireg";

		// write "wire/trireg" declarations for internal single-wide signals
		int localWires = 0;
		for(int wt=0; wt<2; wt++)
		{
			first = true;
			for(Iterator it = cn.portInfo.iterator(); it.hasNext(); )
			{
				OutputTopology.NetInfo nin = (OutputTopology.NetInfo)it.next();
				if (nin.temp1 <= 1 && nin.low > nin.high)
				{
					OutputTopology.WireList wl = nin.wires[0];
					if (wl.isGlobal) continue;
					String impSigName = wireType;
					if (wl != null && wl.flags != 0)
					{
						if (wl.flags == 1) impSigName = "wire"; else
							impSigName = "trireg";
					}
					if ((wt == 0) ^ !wireType.equals(impSigName))
					{
						if (first)
						{
							initDeclaration("  " + impSigName);
						}
						addDeclaration(nin.name);
						localWires++;
						first = false;
					}
				}
			}
			if (!first) termDeclaration();
		}

		// write "wire/trireg" declarations for internal busses
		for(Iterator it = cn.portInfo.iterator(); it.hasNext(); )
		{
			OutputTopology.NetInfo nin = (OutputTopology.NetInfo)it.next();
			if (nin.temp1 > 1) continue;
			if (nin.low > nin.high) continue;

			if ((nin.temp1&1) != 0)
			{
				printWriter.print("  " + wireType + " [" + nin.high + ":" + nin.low + "] " + nin.name + ";\n");
			} else
			{
				printWriter.print("  " + wireType + " [" + nin.low + ":" + nin.high + "] " + nin.name + ";\n");
			}
			localWires++;
		}
		if (localWires != 0) printWriter.print("\n");

//		// add "wire" declarations for implicit inverters
//		if (impinv > 1)
//		{
//			esnprintf(invsigname, 100, x_("  %s"), wireType);
//			initDeclaration(invsigname);
//			for(i=1; i<impinv; i++)
//			{
//				esnprintf(impsigname, 100, x_("%s%ld"), IMPLICITINVERTERSIGNAME, i);
//				addDeclaration(impsigname);
//			}
//			termDeclaration();
//		}

		// add in any user-specified declarations and code
		first = includeTypedCode(cell, VERILOG_DECLARATION_KEY, "declarations");
		first |= includeTypedCode(cell, VERILOG_CODE_KEY, "code");
		if (!first)
			printWriter.print("  /* automatically generated Verilog */\n");

		// look at every node in this cell
		unconnectedNet = 1;
		for(Iterator nIt = netList.getNodables(); nIt.hasNext(); )
		{
			Nodable no = (Nodable)nIt.next();
			NodeProto niProto = no.getProto();

			// not interested in passive nodes (ports electrically connected)
			NodeProto.Function nodeType = NodeProto.Function.UNKNOWN;
			if (niProto instanceof PrimitiveNode)
			{
				NodeInst ni = (NodeInst)no;
				Iterator pIt = ni.getPortInsts();
				if (pIt.hasNext())
				{
					boolean allConnected = true;
					PortInst firstPi = (PortInst)pIt.next();
					JNetwork firstNet = netList.getNetwork(firstPi);
					for( ; pIt.hasNext(); )
					{
						PortInst pi = (PortInst)pIt.next();
						JNetwork thisNet = netList.getNetwork(pi);
						if (thisNet != firstNet) { allConnected = false;   break; }
					}
					if (allConnected) continue;
				}
				nodeType = ni.getFunction();

				// special case: verilog should ignore R L C etc.
				if (nodeType == NodeProto.Function.RESIST || nodeType == NodeProto.Function.CAPAC ||
					nodeType == NodeProto.Function.ECAPAC || nodeType == NodeProto.Function.INDUCT)
						continue;
			}

//			int nodewidth = ni->arraysize;
//			if (nodewidth < 1) nodewidth = 1;
//
//			// use "assign" statement if possible
//			if (Simulation.getVerilogUseAssign())
//			{
//				if (nodeType == NPGATEAND || nodeType == NPGATEOR ||
//					nodeType == NPGATEXOR || nodeType == NPBUFFER)
//				{
//					// assign possible: determine operator
//					switch (nodeType)
//					{
//						case NPGATEAND:  op = x_(" & ");   break;
//						case NPGATEOR:   op = x_(" | ");   break;
//						case NPGATEXOR:  op = x_(" ^ ");   break;
//						case NPBUFFER:   op = x_("");      break;
//					}
//					for(nindex=0; nindex<nodewidth; nindex++)
//					{
//						// write a line describing this signal
//						infstr = initinfstr();
//						wholenegated = 0;
//						first = TRUE;
//						for(i=0; i<2; i++)
//						{
//							for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//							{
//								if (i == 0)
//								{
//									if (estrcmp(pi->proto->protoname, x_("y")) != 0) continue;
//								} else
//								{
//									if (estrcmp(pi->proto->protoname, x_("a")) != 0) continue;
//								}
//
//								// determine the network name at this port
//								net = pi->conarcinst->network;
//								if (nodewidth > 1)
//								{
//									(void)net_evalbusname(
//										(pi->conarcinst->proto->userbits&AFUNCTION)>>AFUNCTIONSH,
//											networkname(net, 0), &strings, pi->conarcinst, np, 1);
//									estrcpy(impsigname, strings[nindex]);
//									signame = impsigname;
//								} else
//								{
//									if (net != NONETWORK && net->namecount > 0)
//										signame = networkname(net, 0); else
//											signame = describenetwork(net);
//									if (net == pwrnet) signame = x_("vdd"); else
//										if (net == gndnet) signame = x_("gnd");
//								}
//
//								// see if this end is negated
//								isnegated = 0;
//								ai = pi->conarcinst;
//								if ((ai->userbits&ISNEGATED) != 0)
//								{
//									if ((ai->end[0].nodeinst == ni && (ai->userbits&REVERSEEND) == 0) ||
//										(ai->end[1].nodeinst == ni && (ai->userbits&REVERSEEND) != 0))
//									{
//										isnegated = 1;
//									}
//								}
//
//								// write the port name
//								if (i == 0)
//								{
//									// got the output port: do the left-side of the "assign"
//									addstringtoinfstr(infstr, x_("assign "));
//									addstringtoinfstr(infstr, signame);
//									addstringtoinfstr(infstr, x_(" = "));
//									if (isnegated != 0)
//									{
//										addstringtoinfstr(infstr, x_("~("));
//										wholenegated = 1;
//									}
//									break;
//								} else
//								{
//									if (!first)
//										addstringtoinfstr(infstr, op);
//									first = FALSE;
//									if (isnegated != 0) addstringtoinfstr(infstr, x_("~"));
//									addstringtoinfstr(infstr, signame);
//								}
//							}
//						}
//						if (wholenegated != 0)
//							addstringtoinfstr(infstr, x_(")"));
//						addstringtoinfstr(infstr, x_(";"));
//						writeLongLine(returninfstr(infstr));
//					}
//					continue;
//				}
//			}

			// get the name of the node
			int implicitPorts = 0;
			boolean dropBias = false;
			String nodeName = null;
			if (niProto instanceof Cell)
			{
				nodeName = getUniqueCellName((Cell)niProto);
//				String pname = parameterizedname(ni, nodeName);
//				if (pname != 0)
//					nodeName = getSafeNetName(pname);
			} else
			{
				nodeName = niProto.getProtoName();

				// convert 4-port transistors to 3-port
				if (nodeType == NodeProto.Function.TRA4NMOS)
				{
					nodeType = NodeProto.Function.TRANMOS;  dropBias = true;
				} else if (nodeType == NodeProto.Function.TRA4PMOS)
				{
					nodeType = NodeProto.Function.TRAPMOS;  dropBias = true;
				}

				if (nodeType == NodeProto.Function.TRANMOS)
				{
					implicitPorts = 2;
					nodeName = "tranif1";
					Variable varWeakNode = ((NodeInst)no).getVar(Simulation.WEAK_NODE_KEY);
					if (varWeakNode != null) nodeName = "rtranif1";
				} else if (nodeType == NodeProto.Function.TRAPMOS)
				{
					implicitPorts = 2;
					nodeName = "tranif0";
					Variable varWeakNode = ((NodeInst)no).getVar(Simulation.WEAK_NODE_KEY);
					if (varWeakNode != null) nodeName = "rtranif0";
				} else if (nodeType == NodeProto.Function.GATEAND)
				{
					implicitPorts = 1;
					nodeName = chooseNodeName((NodeInst)no, "and", "nand");
				} else if (nodeType == NodeProto.Function.GATEOR)
				{
					implicitPorts = 1;
					nodeName = chooseNodeName((NodeInst)no, "or", "nor");
				} else if (nodeType == NodeProto.Function.GATEXOR)
				{
					implicitPorts = 1;
					nodeName = chooseNodeName((NodeInst)no, "xor", "xnor");
				} else if (nodeType == NodeProto.Function.BUFFER)
				{
					implicitPorts = 1;
					nodeName = chooseNodeName((NodeInst)no, "buf", "not");
					nodeName = "buf";
				}
			}

			// look for a Verilog template on the prototype
			Variable varTemplate = null;
			if (niProto instanceof Cell)
			{
				varTemplate = niProto.getVar(VERILOG_TEMPLATE_KEY);
				if (varTemplate == null)
				{
					Cell cNp = ((Cell)niProto).contentsView();
					if (cNp != null)
						varTemplate = cNp.getVar(VERILOG_TEMPLATE_KEY);
				}
			}

			StringBuffer infstr = new StringBuffer();
			if (varTemplate == null)
			{
				// write the type of the node
				infstr.append("  " + nodeName + " " + nameNoIndices(no.getName()) + "(");
			}

			// write the rest of the ports
			first = true;
			switch (implicitPorts)
			{
				case 0:		// explicit ports
					// special case for Verilog templates
					if (varTemplate != null)
					{
						writeTemplate((String)varTemplate.getObject(), no);
						break;
					}

					// generate the line the normal way
					OutputTopology.CellNets subCn = getCellNets((Cell)niProto);
					for(Iterator sIt = subCn.portInfo.iterator(); sIt.hasNext(); )
					{
						NetInfo nin = (NetInfo)sIt.next();

						// ignore networks that aren't exported
						if (nin.temp1 <= 1 || nin.temp1 >= 6) continue;

						if (first) first = false; else
							infstr.append(", ");
						if (nin.low > nin.high)
						{
							// single signal
							infstr.append("." + nin.name + "(");
							PortProto pp = nin.pp;
							if (pp == null)
							{
								System.out.println("How did null port get here?");
								continue;
							}
							JNetwork net = netList.getNetwork(no, pp, 0);
							if (net == null)
							{
								infstr.append("UNCONNECTED" + (unconnectedNet++));
							} else
							{
								if (net == cn.pwrNet) infstr.append("vdd"); else
								{
									if (net == cn.gndNet) infstr.append("gnd"); else
									{
										WireList wl = (WireList)cn.wireLists.get(net);
										if (wl.isGlobal) infstr.append("glbl.");
										infstr.append(wl.name);
									}
								}
							}
							infstr.append(")");
						} else
						{
							int total = nin.high - nin.low+1;
							WireList [] outerNetList = new WireList[total];
							for(int j=nin.low; j<=nin.high; j++)
							{
								JNetwork net = netList.getNetwork(no, nin.pp, j-nin.low);
								WireList wl = (WireList)cn.wireLists.get(net);
								outerNetList[j-nin.low] = wl;
							}

							writeBus(outerNetList, nin.low, nin.high, nin.temp1,
								nin.name, cn.pwrNet, cn.gndNet, infstr);
						}
					}
					infstr.append(");");
					break;

				case 1:		// and/or gate: write ports in the proper order
//					for(i=0; i<2; i++)
//					{
//						for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//						{
//							if (i == 0)
//							{
//								if (estrcmp(pi->proto->protoname, x_("y")) != 0) continue;
//							} else
//							{
//								if (estrcmp(pi->proto->protoname, x_("a")) != 0) continue;
//							}
//							if (first) first = FALSE; else
//								addstringtoinfstr(infstr, x_(", "));
//							net = pi->conarcinst->network;
//							if (nodewidth > 1)
//							{
//								if (net->buswidth == nodewidth) net = net->networklist[nindex];
//							} else
//							{
//								if (net->buswidth > 1)
//								{
//									ttyputerr(_("***ERROR: cell %s, node %s is not arrayed but is connected to a bus"),
//										describenodeproto(np), describenodeinst(ni));
//									net = net->networklist[0];
//								}
//							}
//							signame = &((CHAR *)net->temp2)[1];
//							if (net == pwrnet) signame = x_("vdd"); else
//								if (net == gndnet) signame = x_("gnd");
//							if (i != 0 && pi->conarcinst->temp1 != 0)
//							{
//								// this input is negated: write the implicit inverter
//								esnprintf(invsigname, 100, x_("%s%ld"), IMPLICITINVERTERSIGNAME,
//									pi->conarcinst->temp1+nindex);
//								printWriter.print(sim_verfile, x_("  inv %s%ld (%s, %s);\n"),
//									IMPLICITINVERTERNODENAME, pi->conarcinst->temp1+nindex,
//										invsigname, signame);
//								signame = invsigname;
//							}
//							addstringtoinfstr(infstr, signame);
//						}
//					}
//					addstringtoinfstr(infstr, x_(");"));
					break;

				case 2:		// transistors: write ports in the proper order
					// schem: g/s/d  mos: g/s/g/d
					NodeInst ni = (NodeInst)no;
					JNetwork gateNet = netList.getNetwork(ni.getTransistorGatePort());
					for(int i=0; i<2; i++)
					{
						for(Iterator pIt = ni.getPortInsts(); pIt.hasNext(); )
						{
							PortInst pi = (PortInst)pIt.next();
							JNetwork net = netList.getNetwork(pi);

							// see if it connects to an earlier portinst
							boolean connected = false;
							for(Iterator ePIt = ni.getPortInsts(); ePIt.hasNext(); )
							{
								PortInst ePi = (PortInst)ePIt.next();
								if (ePi == pi) break;
								JNetwork eNet = netList.getNetwork(ePi);
								if (eNet == net) { connected = true;   break; }
							}
							if (connected) continue;
							if (dropBias && pi.getPortProto().getProtoName().equals("b")) continue;
							if (i == 0)
							{
								if (net == gateNet) continue;
							} else
							{
								if (net != gateNet) continue;
							}
							if (first) first = false; else
								infstr.append(", ");

							WireList wl = (WireList)cn.wireLists.get(net);
							String sigName = wl.name;
							if (net == cn.pwrNet) sigName = "vdd"; else
							{
								if (net == cn.gndNet) sigName = "gnd"; else
								{
									if (wl.isGlobal) sigName = "glbl." + sigName;
								}
							}
//							if (i != 0 && pi != null && pi->conarcinst->temp1 != 0)
//							{
//								// this input is negated: write the implicit inverter
//								esnprintf(invsigname, 100, x_("%s%ld"), IMPLICITINVERTERSIGNAME,
//									pi->conarcinst->temp1+nindex);
//								printWriter.print("  inv " + IMPLICITINVERTERNODENAME +
//									(pi->conarcinst->temp1+nindex) + " (" + invsigname + ", " + signame + ");\n");
//								signame = invsigname;
//							}
							infstr.append(sigName);
						}
					}
					infstr.append(");");
					break;
			}
			writeLongLine(infstr.toString());
		}
		String moduleName = getUniqueCellName(cell);
//		if (paramname != 0) moduleName = getSafeNetName(paramname);
		printWriter.print("endmodule   /* " + moduleName + " */\n");
	}

	private String chooseNodeName(NodeInst ni, String positive, String negative)
	{
		for(Iterator aIt = ni.getConnections(); aIt.hasNext(); )
		{
			Connection con = (Connection)aIt.next();
			if (con.getPortInst().getPortProto().getProtoName().equals("y") &&
				con.getArc().isNegated()) return negative;
		}
		return positive;
	}

	private void writeTemplate(String line, Nodable no)
	{
//		// special case for Verilog templates
//		StringBuffer infstr = new StringBuffer();
//		infstr.append("  ");
//		for(int pt = 0; pt < line.length(); pt++)
//		{
//			char chr = line.charAt(pt);
//			if (chr != '$' || line.charAt(pt+1) != '(')
//			{
//				infstr.append(chr);
//				continue;
//			}
//			int startpt = pt + 2;
//			for(pt = startpt; pt < line.length(); pt++)
//				if (*pt == ')') break;
//			save = *pt;
//			*pt = 0;
//			pp = getportproto(ni->proto, startpt);
//			if (pp != NOPORTPROTO)
//			{
//				// port name found: use its verilog node
//				net = getNetOnPort(ni, pp);
//				if (net == NONETWORK)
//				{
//					formatinfstr(infstr, x_("UNCONNECTED%ld"), unconnectedNet++);
//				} else
//				{
//					if (net->buswidth > 1)
//					{
//						sigcount = net->buswidth;
//						if (nodewidth > 1 && pp->network->buswidth * nodewidth == net->buswidth)
//						{
//							// map wide bus to a particular instantiation of an arrayed node
//							if (pp->network->buswidth == 1)
//							{
//								onet = net->networklist[nindex];
//								if (onet == pwrnet) addstringtoinfstr(infstr, x_("vdd")); else
//									if (onet == gndnet) addstringtoinfstr(infstr, x_("gnd")); else
//										addstringtoinfstr(infstr, &((CHAR *)onet->temp2)[1]);
//							} else
//							{
//								outernetlist = (NETWORK **)emalloc(pp->network->buswidth * (sizeof (NETWORK *)),
//									sim_tool->cluster);
//								for(j=0; j<pp->network->buswidth; j++)
//									outernetlist[j] = net->networklist[i + nindex*pp->network->buswidth];
//								for(opt = pp->protoname; *opt != 0; opt++)
//									if (*opt == '[') break;
//								osave = *opt;
//								*opt = 0;
//								writeBus(outernetlist, 0, net->buswidth-1, 0,
//									0, pwrnet, gndnet, infstr);
//								*opt = osave;
//								efree((CHAR *)outernetlist);
//							}
//						} else
//						{
//							if (pp->network->buswidth != net->buswidth)
//							{
//								ttyputerr(_("***ERROR: port %s on node %s in cell %s is %d wide, but is connected/exported with width %d"),
//									pp->protoname, describenodeinst(ni), describenodeproto(np),
//										cpp->network->buswidth, net->buswidth);
//								sigcount = mini(sigcount, cpp->network->buswidth);
//								if (sigcount == 1) sigcount = 0;
//							}
//							outernetlist = (NETWORK **)emalloc(net->buswidth * (sizeof (NETWORK *)),
//								sim_tool->cluster);
//							for(j=0; j<net->buswidth; j++)
//								outernetlist[j] = net->networklist[j];
//							for(opt = pp->protoname; *opt != 0; opt++)
//								if (*opt == '[') break;
//							osave = *opt;
//							*opt = 0;
//							writeBus(outernetlist, 0, net->buswidth-1, 0,
//								0, pwrnet, gndnet, infstr);
//							*opt = osave;
//							efree((CHAR *)outernetlist);
//						}
//					} else
//					{
//						if (net == pwrnet) addstringtoinfstr(infstr, x_("vdd")); else
//							if (net == gndnet) addstringtoinfstr(infstr, x_("gnd")); else
//								addstringtoinfstr(infstr, &((CHAR *)net->temp2)[1]);
//					}
//				}
//			} else if (namesame(startpt, x_("node_name")) == 0)
//			{
//				if (nodewidth > 1) opt = nodenames[nindex]; else
//				{
//					var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, el_node_name_key);
//					if (var == NOVARIABLE) opt = x_(""); else
//						opt = (CHAR *)var->addr;
//				}
//				addstringtoinfstr(infstr, nameNoIndices(opt));
//			} else
//			{
//				// no port name found, look for variable name
//				esnprintf(line, 200, x_("ATTR_%s"), startpt);
//				var = getval((INTBIG)ni, VNODEINST, -1, line);
//				if (var == NOVARIABLE)
//					var = getval((INTBIG)ni, VNODEINST, -1, startpt);
//				if (var == NOVARIABLE)
//				{
//					// value not found: see if this is a parameter and use default
//					nip = ni->proto;
//					nipc = contentsview(nip);
//					if (nipc != NONODEPROTO) nip = nipc;
//					var = getval((INTBIG)nip, VNODEPROTO, -1, line);
//				}
//				if (var == NOVARIABLE)
//					addstringtoinfstr(infstr, x_("??")); else
//				{
//					addstringtoinfstr(infstr, describesimplevariable(var));
//				}
//			}
//			*pt = save;
//			if (save == 0) break;
//		}
	}

	/*
	 * Method to add a bus of signals named "name" to the infinite string "infstr".  If "name" is zero,
	 * do not include the ".NAME()" wrapper.  The signals are in "outernetlist" and range in index from
	 * "lowindex" to "highindex".  They are described by a bus with characteristic "tempval"
	 * (low bit is on if the bus descends).  Any unconnected networks can be numbered starting at
	 * "*unconnectednet".  The power and grounds nets are "pwrnet" and "gndnet".
	 */
	private void writeBus(WireList [] outernetlist, int lowindex, int highindex, int tempval,
		String name, JNetwork pwrNet, JNetwork gndNet, StringBuffer infstr)
	{
		// array signal: see if it gets split out
		boolean breakBus = false;

		// bus cannot have pwr/gnd, must be connected
		int numExported = 0, numInternal = 0;
		for(int j=lowindex; j<=highindex; j++)
		{
			WireList wl = outernetlist[j-lowindex];
			if (wl.temp1 == 6) { breakBus = true;   break; }
			if (wl.temp1 > 1) numExported++; else
				numInternal++;
		}

		// must be all exported or all internal, not a mix
		if (numExported > 0 && numInternal > 0) breakBus = true;

		if (!breakBus)
		{
			// see if all of the nets on this bus are distinct
			int j = lowindex+1;
			for( ; j<=highindex; j++)
			{
				WireList wl = outernetlist[j-lowindex];
				int k = lowindex;
				for( ; k<j; k++)
				{
					WireList oWl = outernetlist[k-lowindex];
					if (wl == oWl) break;
				}
				if (k < j) break;
			}
			if (j <= highindex) breakBus = true; else
			{
				// bus entries must have the same root name and go in order
				String lastnetname = null;
				for(j=lowindex; j<=highindex; j++)
				{
					WireList wl = outernetlist[j-lowindex];
					String thisnetname = wl.name;
					if (wl.descending)
					{
						if ((tempval&1) == 0) break;
					} else
					{
						if ((tempval&1) != 0) break;
					}

					int openSquare = thisnetname.indexOf('[');
					if (openSquare < 0) break;
					if (j > lowindex)
					{
						int li = 0;
						for( ; li < lastnetname.length(); li++)
						{
							if (thisnetname.charAt(li) != lastnetname.charAt(li)) break;
							if (lastnetname.charAt(li) == '[') break;
						}
						if (lastnetname.charAt(li) != '[' || thisnetname.charAt(li) != '[') break;
						if (TextUtils.atoi(thisnetname.substring(li+1)) != TextUtils.atoi(lastnetname.substring(li+1)) + 1) break;
					}
					lastnetname = thisnetname;
				}
				if (j <= highindex) breakBus = true;
			}
		}

		if (name != null) infstr.append("." + name + "(");
		if (breakBus)
		{
			infstr.append("{");
			int start = lowindex, end = highindex;
			int order = 1;
			if ((tempval&1) != 0)
			{
				start = highindex;
				end = lowindex;
				order = -1;
			}
			for(int j=start; ; j += order)
			{
				if (j != start)
					infstr.append(", ");
				WireList wl = outernetlist[j-lowindex];
				JNetwork net = wl.net;
				if (net == pwrNet) infstr.append("vdd"); else
				{
					if (net == gndNet) infstr.append("gnd"); else
					{
						if (wl.isGlobal) infstr.append("glbl.");
						infstr.append(wl.name);
					}
				}
				if (j == end) break;
			}
			infstr.append("}");
		} else
		{
			WireList lastWl = outernetlist[0];
			String lastNetName = lastWl.name;
			int openSquare = lastNetName.indexOf('[');
			WireList wl = outernetlist[highindex-lowindex];
			String netName = wl.name;
			int i = 0;
			for( ; i<netName.length(); i++)
			{
				if (netName.charAt(i) == '[') break;
				infstr.append(netName.charAt(i));
			}
			if ((tempval&1) != 0)
			{
				int first = TextUtils.atoi(netName.substring(i+1));
				int second = TextUtils.atoi(lastNetName.substring(openSquare+1));
				infstr.append("[" + first + ":" + second + "]");
			} else
			{
				int first = TextUtils.atoi(netName.substring(i+1));
				int second = TextUtils.atoi(lastNetName.substring(openSquare+1));
				infstr.append("[" + second + ":" + first + "]");
			}
		}
		if (name != null) infstr.append(")");
	}

	/*
	 * Method to add text from all nodes in cell "np"
	 * (which have "verilogkey" text on them)
	 * to that text to the output file.  Returns true if anything
	 * was found.
	 */
	boolean includeTypedCode(Cell cell, Variable.Key verilogkey, String descript)
	{
		// write out any directly-typed Verilog code
		boolean first = true;
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getProto() != Generic.tech.invisiblePinNode) continue;
			Variable var = ni.getVar(verilogkey);
			if (var == null) continue;
			if (!var.isDisplay()) continue;
			Object obj = var.getObject();
			if (!(obj instanceof String) && !(obj instanceof String [])) continue;
			if (first)
			{
				first = false;
				printWriter.print("  /* user-specified Verilog " + descript + " */\n");
			}
			if (obj instanceof String)
			{
				printWriter.print("  " + (String)obj + "\n");
			} else
			{
				String [] stringArray = (String [])obj;
				int len = stringArray.length;
				for(int i=0; i<len; i++)
					printWriter.print("  " + stringArray[i] + "\n");
			}
		}
		if (!first) printWriter.print("\n");
		return first;
	}

	/*
	 * Method to write a long line to the Verilog file, breaking it where sensible.
	 */
	private void writeLongLine(String s)
	{
		while (s.length() > MAXDECLARATIONWIDTH)
		{
			int lastSpace = s.lastIndexOf(' ', MAXDECLARATIONWIDTH);
			if (lastSpace < 0) lastSpace = MAXDECLARATIONWIDTH;
			printWriter.print(s.substring(0, lastSpace) + "\n      ");
			while (lastSpace+1 < s.length() && s.charAt(lastSpace) == ' ') lastSpace++;
			s = s.substring(lastSpace);
		}
		printWriter.print(s + "\n");
	}

	private StringBuffer sim_verDeclarationLine;
	private int sim_verdeclarationprefix;

	/*
	 * Method to initialize the collection of signal names in a declaration.
	 * The declaration starts with the string "header".
	 */
	private void initDeclaration(String header)
	{
		sim_verDeclarationLine = new StringBuffer();
		sim_verDeclarationLine.append(header);
		sim_verdeclarationprefix = header.length();
	}

	/*
	 * Method to add "signame" to the collection of signal names in a declaration.
	 */
	private void addDeclaration(String signame)
	{
		if (sim_verDeclarationLine.length() + signame.length() + 3 > MAXDECLARATIONWIDTH)
		{
			printWriter.print(sim_verDeclarationLine.toString() + ";\n");
			sim_verDeclarationLine.delete(sim_verdeclarationprefix, sim_verDeclarationLine.length());
		}
		if (sim_verDeclarationLine.length() != sim_verdeclarationprefix)
			sim_verDeclarationLine.append(",");
		sim_verDeclarationLine.append(" " + signame);
	}

	/*
	 * Method to terminate the collection of signal names in a declaration
	 * and write the declaration to the Verilog file.
	 */
	private void termDeclaration()
	{
		printWriter.print(sim_verDeclarationLine.toString() + ";\n");
	}

	/*
	 * Method to adjust name "p" and return the string.
	 * This code removes all index indicators and other special characters, turning
	 * them into "_".
	 */
	private String nameNoIndices(String p)
	{
		StringBuffer sb = new StringBuffer();
		if (Character.isDigit(p.charAt(0))) sb.append('_');
		for(int i=0; i<p.length(); i++)
		{
			char chr = p.charAt(i);
			if (!Character.isLetterOrDigit(chr) && chr != '_' && chr != '$') chr = '_';
			sb.append(chr);
		}
		return sb.toString();
	}

	protected String getSafeCellName(String name)
	{
		return getSafeNetName(name);
	}

	/*
	 * Method to adjust name "p" and return the string.
	 * Verilog does permit a digit in the first location; prepend a "_" if found.
	 * Verilog only permits the "_" and "$" characters: all others are converted to "_".
	 * Verilog does not permit nonnumeric indices, so "P[A]" is converted to "P_A_"
	 * Verilog does not permit multidimensional arrays, so "P[1][2]" is converted to "P_1_[2]"
	 *   and "P[1][T]" is converted to "P_1_T_"
	 */
	protected String getSafeNetName(String name)
	{
		// simple names are trivially accepted as is
		boolean allAlnum = true;
		for(int i=0; i<name.length(); i++)
		{
			if (!Character.isLetterOrDigit(name.charAt(i))) { allAlnum = false;   break; }
		}
		if (allAlnum && Character.isLetter(name.charAt(0))) return name;

		StringBuffer sb = new StringBuffer();
		for(int t=0; t<name.length(); t++)
		{
			char chr = name.charAt(t);
			if (chr == '[' || chr == ']')
			{
				sb.append('_');
				if (t+1 < name.length() && chr == ']' && name.charAt(t+1) == '[') t++;
			} else
			{
				if (Character.isLetterOrDigit(chr) || chr == '$')
					sb.append(chr); else
						sb.append('_');
			}
		}
		return sb.toString();
	}

	protected Netlist getNetlistForCell(Cell cell)
	{
		// get network information about this cell
		boolean shortResistors = false;
		Netlist netList = cell.getNetlist(shortResistors);
		return netList;
	}

}
