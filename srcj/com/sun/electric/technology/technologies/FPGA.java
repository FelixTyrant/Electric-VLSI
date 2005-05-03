/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FPGA.java
 * FPGA technology
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.technology.technologies;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Technology.TechPoint;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is the FPGA Technology.
 */
public class FPGA extends Technology
{
	/** the FPGA Technology object. */	public static final FPGA tech = new FPGA();
	private Layer fpga_w_lay, fpga_c_lay, fpga_p_lay;
	private PrimitiveArc wire_arc;
	private PrimitiveNode wirePinNode, pipNode, repeaterNode;

	// -------------------- private and protected methods ------------------------
	private FPGA()
	{
		super("fpga");
		setTechShortName("FPGA");
		setTechDesc("FPGA Building-Blocks");
		setFactoryScale(2000, true);   // in nanometers: really 2 microns
		setStaticTechnology();
		setNonStandard();
		setNoPrimitiveNodes();

		//**************************************** LAYERS ****************************************

		/** Wire layer */
		fpga_w_lay = Layer.newInstance(this, "Wire",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 255,0,0,1,true,
			new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));

		/** Component layer */
		fpga_c_lay = Layer.newInstance(this, "Component",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 0,0,0,1,true,
			new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));

		/** Pip layer */
		fpga_p_lay = Layer.newInstance(this, "Pip",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 0,255,0,1,true,
			new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));

		/** Repeater layer */
		Layer fpga_r_lay = Layer.newInstance(this, "Repeater",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 0,0,255,1,true,
			new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));

		// The layer functions
		fpga_w_lay.setFunction(Layer.Function.METAL1);		// wire
		fpga_c_lay.setFunction(Layer.Function.ART);			// component
		fpga_p_lay.setFunction(Layer.Function.ART);			// pip
		fpga_r_lay.setFunction(Layer.Function.ART);			// repeater

		//**************************************** ARCS ****************************************

		/** wire arc */
		wire_arc = PrimitiveArc.newInstance(this, "wire", 0.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(fpga_w_lay, 0, Poly.Type.FILLED)
		});
		wire_arc.setFunction(PrimitiveArc.Function.METAL1);
		wire_arc.setFactoryFixedAngle(true);
		wire_arc.setFactorySlidable(false);
		wire_arc.setFactoryAngleIncrement(45);

		//**************************************** NODES ****************************************

		/** wire pin */
		wirePinNode = PrimitiveNode.newInstance("Wire_Pin", this, 1, 1, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(fpga_w_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())})
			});
		wirePinNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, wirePinNode, new ArcProto[] {wire_arc}, "wire", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		wirePinNode.setFunction(PrimitiveNode.Function.PIN);
		wirePinNode.setSquare();
		wirePinNode.setWipeOn1or2();

		/** pip */
		pipNode = PrimitiveNode.newInstance("Pip", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(fpga_p_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge())})
			});
		pipNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pipNode, new ArcProto[] {wire_arc}, "pip", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		pipNode.setFunction(PrimitiveNode.Function.CONNECT);
		pipNode.setSquare();

		/** repeater */
		repeaterNode = PrimitiveNode.newInstance("Repeater", this, 10, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(fpga_r_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge())})
			});
		repeaterNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, repeaterNode, new ArcProto[] {wire_arc}, "a", 180,45, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeCenter(), EdgeH.makeLeftEdge(), EdgeV.makeCenter()),
				PrimitivePort.newInstance(this, repeaterNode, new ArcProto[] {wire_arc}, "b", 0,45, 1, PortCharacteristic.UNKNOWN,
					EdgeH.makeRightEdge(), EdgeV.makeCenter(), EdgeH.makeRightEdge(), EdgeV.makeCenter())
			});
		repeaterNode.setFunction(PrimitiveNode.Function.CONNECT);
	}

	/******************** TREE STRUCTURE FOR ARCHITECTURE FILE ********************/

	private static final int MAXDEPTH	= 50;		/* max depth of FPGA nesting */
	
	private static final int PARAMBRANCH =  1;		/* parameter is a subtree */
	private static final int PARAMATOM   =  2;		/* parameter is atomic */
	
	private static class LispTree
	{
		String   keyword;
		int  lineno;
		List paramtype;
		List paramvalue;

		LispTree()
		{
			paramtype = new ArrayList();
			paramvalue = new ArrayList();
		}
	};
	
	static LispTree [] fpga_treestack = new LispTree[MAXDEPTH];
	static int         fpga_treedepth;
	static LispTree    fpga_treepos;
	
	/******************** ADDITIONAL INFORMATION ABOUT PRIMITIVES ********************/
	
	private static final int ACTIVEPART   = 1;			/* set if segment or pip is active */
	private static final int ACTIVESAVE   = 2;			/* saved area for segment/pip activity */
	
	private static class FPGAPort
	{
		double        posx, posy;
		int           con;
		PrimitivePort pp;
	};
	
	private static class FPGANet
	{
		String     netname;
		int        segactive;
		int        segcount;
		Point2D [] segf;
		Point2D [] segt;
	};
	
	private static class FPGAPip
	{
		String  pipname;
		int     pipactive;
		int     con1, con2;
		double  posx, posy;
	};

	private static class FPGANode extends PrimitiveNode
	{
		int         portcount;
		FPGAPort [] portlist;
		int         netcount;
		FPGANet []  netlist;
		int         pipcount;
		FPGAPip []  piplist;

		protected FPGANode(String protoName, Technology tech, double defWidth, double defHeight,
			SizeOffset offset, Technology.NodeLayer [] layers)
		{
			super(protoName, tech, defWidth, defHeight, offset, layers);
		}
	};
	
	/* the display level */
	private static final int DISPLAYLEVEL       =  07;		/* level of display */
	private static final int NOPRIMDISPLAY      =   0;		/*   display no internals */
	private static final int FULLPRIMDISPLAY    =  01;		/*   display all internals */
	private static final int ACTIVEPRIMDISPLAY  =  02;		/*   display only active internals */
	private static final int TEXTDISPLAY        = 010;		/* set to display text */

	static int         fpga_internaldisplay = ACTIVEPRIMDISPLAY | TEXTDISPLAY;
//	static INTBIG         fpga_curpip;
//	static INTBIG         fpga_curnet, fpga_cursegment;
	/** key of Variable holding active pips. */				public static final Variable.Key ACTIVEPIPS_KEY = ElectricObject.newKey("FPGA_activepips");
	/** key of Variable holding active repeaters. */		public static final Variable.Key ACTIVEREPEATERS_KEY = ElectricObject.newKey("FPGA_activerepeaters");
	/** key of Variable holding cache of pips on node. */	public static final Variable.Key NODEPIPCACHE_KEY = ElectricObject.newKey("FPGA_nodepipcache");
	/** key of Variable holding cache of active arcs. */	public static final Variable.Key ARCACTIVECACHE_KEY = ElectricObject.newKey("FPGA_arcactivecache");
	static String         fpga_repeatername;		/* name of current repeater for activity examining */
	static boolean        fpga_repeaterisactive;	/* nonzero if current repeater is found to be active */
//	
//	/* working memory for "fpga_arcactive()" */
//	static INTBIG         fpga_arcbufsize = 0;
//	static UCHAR1        *fpga_arcbuf;
//	
//	/* working memory for "fpga_reevaluatepips()" */
//	static INTBIG         fpga_pipbufsize = 0;
//	static UCHAR1        *fpga_pipbuf;

	static int     fpga_nodecount;
	static FPGANode [] fpga_nodes;
	
//	void fpga_setmode(INTBIG count, CHAR *par[])
//	{
//		REGISTER CHAR *pp;
//		REGISTER NODEPROTO *topcell;
//		REGISTER NODEINST *ni;
//		CHAR *filename, *subpar[1];
//		FILE *f;
//		REGISTER INTBIG l, total;
//		REGISTER LISPTREE *lt;
//		static INTBIG filetypefpga = -1;
//		REGISTER void *infstr, *dia;
//	
//		l = estrlen(pp = par[0]);
//		if (namesamen(pp, x_("display-level"), l) == 0)
//		{
//			l = estrlen(pp = par[1]);
//			if (namesamen(pp, x_("text"), l) == 0)
//			{
//				if (count == 2)
//				{
//					if ((fpga_internaldisplay & TEXTDISPLAY) == 0)
//						ttyputmsg(M_("Text not displayed")); else
//							ttyputmsg(M_("Text is displayed"));
//					return;
//				}
//				l = estrlen(pp = par[2]);
//				if (namesamen(pp, x_("on"), l) == 0 && l >= 2)
//				{
//					fpga_internaldisplay |= TEXTDISPLAY;
//					ttyputverbose(M_("Text is displayed"));
//					return;
//				}
//				if (namesamen(pp, x_("off"), l) == 0 && l >= 2)
//				{
//					fpga_internaldisplay &= ~TEXTDISPLAY;
//					ttyputverbose(M_("Text not displayed"));
//					return;
//				}
//				ttyputbadusage(x_("technology tell fpga display-level text"));
//				return;
//			}
//			ttyputbadusage(x_("technology tell fpga display-level"));
//			return;
//		}
//	
//		if (namesamen(pp, x_("wipe-cache"), l) == 0)
//		{
//			fpga_clearcache(NONODEINST);
//			return;
//		}
//		if (namesamen(pp, x_("clear-node-cache"), l) == 0)
//		{
//			ni = (NODEINST *)us_getobject(VNODEINST, FALSE);
//			if (ni == NONODEINST) return;
//			fpga_clearcache(ni);
//			return;
//		}
//	
//		ttyputbadusage(x_("technology tell fpga"));
//	}


	private static Technology.NodeLayer[] NULLNODELAYER = new Technology.NodeLayer [] {};

	/**
	 * Method to return a list of Polys that describe a given NodeInst.
	 * This method overrides the general one in the Technology object
	 * because of the unusual primitives in this Technology.
	 * @param ni the NodeInst to describe.
	 * @param wnd the window in which this node will be drawn.
	 * @param electrical true to get the "electrical" layers.
	 * This makes no sense for Schematics primitives.
	 * @param reasonable true to get only a minimal set of contact cuts in large contacts.
	 * This makes no sense for Schematics primitives.
	 * @param primLayers an array of NodeLayer objects to convert to Poly objects.
	 * @param layerOverride the layer to use for all generated polygons (if not null).
	 * @return an array of Poly objects.
	 */
	public Poly [] getShapeOfNode(NodeInst ni, EditWindow wnd, boolean electrical, boolean reasonable, Technology.NodeLayer [] primLayers, Layer layerOverride)
	{
		NodeProto prototype = ni.getProto();
		if (!(prototype instanceof PrimitiveNode)) return null;

		PrimitiveNode np = (PrimitiveNode)prototype;
		if (np == tech.wirePinNode)
		{
			if (ni.pinUseCount()) primLayers = NULLNODELAYER;
		} else if (np == tech.repeaterNode)
		{
			if ((fpga_internaldisplay&DISPLAYLEVEL) == ACTIVEPRIMDISPLAY)
			{
				if (!fpga_repeateractive(ni)) primLayers = NULLNODELAYER;
			}
		} else if (np instanceof FPGANode)
		{
			// dynamic primitive
			FPGANode fn = (FPGANode)np;

			// hard reset of all segment and pip activity
			int numPips = 0, numSegs = 0;
			for(int i=0; i<fn.netcount; i++) fn.netlist[i].segactive = 0;
			for(int i=0; i<fn.pipcount; i++) fn.piplist[i].pipactive = 0;

			switch (fpga_internaldisplay & DISPLAYLEVEL)
			{
				case NOPRIMDISPLAY:
					break;
				case ACTIVEPRIMDISPLAY:
					// count number of active nets and pips
	
					// determine the active segments and pips
					VarContext context = null;
					if (wnd != null) context = wnd.getVarContext();
					fpga_reevaluatepips(ni, fn, context);
	
					// save the activity bits
					for(int i=0; i<fn.netcount; i++)
						if ((fn.netlist[i].segactive&ACTIVEPART) != 0)
							fn.netlist[i].segactive |= ACTIVESAVE;
					for(int i=0; i<fn.pipcount; i++)
						if ((fn.piplist[i].pipactive&ACTIVEPART) != 0)
							fn.piplist[i].pipactive |= ACTIVESAVE;

					// propagate inactive segments to others that may be active
//					gettraversalpath(ni.parent, NOWINDOWPART, &nilist, &indexlist, &depth, 1);
//					if (depth > 0)
//					{
//						oni = nilist[depth-1];
//						uphierarchy();
//						for(i=0; i<fn.netcount; i++)
//						{
//							if ((fn.netlist[i].segactive&ACTIVESAVE) != 0) continue;
//							for(j=0; j<fn.portcount; j++)
//							{
//								if (fn.portlist[j].con != i) continue;
//								for(pi = ni.firstportarcinst; pi != NOPORTARCINST; pi = pi.nextportarcinst)
//								{
//									if (pi.proto != fn.portlist[j].pp) continue;
//									ai = pi.conarcinst;
//									if (ai.end[0].nodeinst == ni) otherend = 1; else otherend = 0;
//									if (fpga_arcendactive(ai, otherend)) break;
//								}
//								if (pi != NOPORTARCINST) break;
//							}
//							if (j < fn.portcount) fn.netlist[i].segactive |= ACTIVESAVE;
//						}
//						downhierarchy(oni, oni.proto, 0);
//					}
	
					// add up the active segments
					for(int i=0; i<fn.pipcount; i++)
						if ((fn.piplist[i].pipactive&ACTIVESAVE) != 0) numPips++;
					for(int i=0; i<fn.netcount; i++)
						if ((fn.netlist[i].segactive&ACTIVESAVE) != 0)
							numSegs += fn.netlist[i].segcount;
					break;
				case FULLPRIMDISPLAY:
					for(int i=0; i<fn.netcount; i++)
					{
						fn.netlist[i].segactive |= ACTIVESAVE;
						numSegs += fn.netlist[i].segcount;
					}
					break;
			}
			int total = 1 + numPips + numSegs;
//			if ((fpga_internaldisplay&TEXTDISPLAY) != 0) total++;

			// construct the polygon array
			if (wnd != null) total += ni.numDisplayableVariables(true);
			Poly [] polys = new Poly[total];

			// add the basic box layer
 			double xCenter = ni.getTrueCenterX();
 			double yCenter = ni.getTrueCenterY();
			double xSize = ni.getXSize();
			double ySize = ni.getYSize();
			Point2D [] pointList = Poly.makePoints(xCenter - xSize/2, xCenter + xSize/2, yCenter - ySize/2, yCenter + ySize/2);
			polys[0] = new Poly(pointList);
			polys[0].setStyle(fn.getLayers()[0].getStyle());
			polys[0].setLayer(tech.fpga_c_lay);

			// add in the pips
			int fillPos = 1;
			for(int i=0; i<fn.pipcount; i++)
			{
				if ((fn.piplist[i].pipactive&ACTIVESAVE) == 0) continue;
				double x = xCenter - xSize/2 + fn.piplist[i].posx;
				double y = yCenter - ySize/2 + fn.piplist[i].posy;
				polys[fillPos] = new Poly(Poly.makePoints(x-1, x+1, y-1, y+1));
				polys[fillPos].setStyle(Poly.Type.FILLED);
				polys[fillPos].setLayer(tech.fpga_p_lay);
				fillPos++;
			}

			// add in the network segments
			for(int i=0; i<fn.netcount; i++)
			{
				if ((fn.netlist[i].segactive&ACTIVESAVE) == 0) continue;
				for(int j=0; j<fn.netlist[i].segcount; j++)
				{
					double fX = xCenter + fn.netlist[i].segf[j].getX();
					double fY = yCenter + fn.netlist[i].segf[j].getY();
					double tX = xCenter + fn.netlist[i].segt[j].getX();
					double tY = yCenter + fn.netlist[i].segt[j].getY();
					Point2D [] line = new Point2D[2];
					line[0] = new Point2D.Double(fX, fY);
					line[1] = new Point2D.Double(tX, tY);
					polys[fillPos] = new Poly(line);
					polys[fillPos].setStyle(Poly.Type.OPENED);
					polys[fillPos].setLayer(tech.fpga_w_lay);
					fillPos++;
				}
			}

			// add in displayable variables
			if (wnd != null)
			{
				Rectangle2D rect = ni.getUntransformedBounds();
				ni.addDisplayableVariables(rect, polys, fillPos, wnd, true);
			}
			return polys;
		}

		return super.getShapeOfNode(ni, wnd, electrical, reasonable, primLayers, layerOverride);
	}

	/**
	 * Method to return a list of Polys that describe a given ArcInst.
	 * This method overrides the general one in the Technology object
	 * because of the unusual primitives in this Technology.
	 * @param ai the ArcInst to describe.
	 * @param wnd the window in which this arc will be drawn.
	 * @param onlyTheseLayers to filter the only required layers
	 * @return an array of Poly objects.
	 */
	public Poly [] getShapeOfArc(ArcInst ai, EditWindow wnd, Layer layerOverride, List onlyTheseLayers)
	{
		boolean active = true;
		if ((fpga_internaldisplay&DISPLAYLEVEL) == NOPRIMDISPLAY ||
			(fpga_internaldisplay&DISPLAYLEVEL) == ACTIVEPRIMDISPLAY)
		{
//			if (!fpga_arcactive(ai)) active = false;
		}

		int total = active ? 1 : 0;
		int numDisplayable = 0;
		if (wnd != null) numDisplayable = ai.numDisplayableVariables(true);
		Poly [] polys = new Poly[total+numDisplayable];

		int polyNum = 0;
		if (active)
		{
			polys[polyNum] = ai.makePoly(ai.getLength(), ai.getWidth(), Poly.Type.FILLED);
			if (polys[polyNum] == null) return null;
			polys[polyNum].setLayer(tech.fpga_w_lay);
			polyNum++;
		}

		// add in the displayable variables
		if (numDisplayable > 0)
		{
			Rectangle2D rect = ai.getBounds();
			ai.addDisplayableVariables(rect, polys, polyNum, wnd, true);
		}
		return polys;
	}

	/******************** TECHNOLOGY INTERFACE SUPPORT ********************/
	
	private boolean fpga_arcendactive(ArcInst ai, int j)
	{
//		REGISTER PORTARCINST *pi;
//		REGISTER PORTEXPINST *pe;
//		REGISTER ARCINST *oai;
//		REGISTER NODEINST *ni, *oni, *subni;
//		REGISTER PORTPROTO *pp, *opp;
//		REGISTER INTBIG pindex, i, newend;
//		REGISTER FPGANODE *fn;
//		NODEINST **nilist;
//		INTBIG depth, *indexlist;
	
//		// examine end
//		ni = ai.end[j].nodeinst;
//		pi = ai.end[j].portarcinst;
//		if (pi == NOPORTARCINST) return false;
//		pp = pi.proto;
//		pindex = ni.proto.primindex;
//		if (pindex == 0)
//		{
//			// follow down into cell
//			downhierarchy(ni, ni.proto, 0);
//			subni = pp.subnodeinst;
//			for(pi = subni.firstportarcinst; pi != NOPORTARCINST; pi = pi.nextportarcinst)
//			{
//				oai = pi.conarcinst;
//				if (oai.end[0].nodeinst == subni) newend = 1; else newend = 0;
//				if (fpga_arcendactive(oai, newend)) break;
//			}
//			uphierarchy();
//			if (pi != NOPORTARCINST) return(TRUE);
//			return false;
//		} else
//		{
//			// primitive: see if it is one of ours
//			if (ni.proto.tech == fpga_tech && pindex > NODEPROTOCOUNT)
//			{
//				fn = fpga_nodes[pindex - NODEPROTOCOUNT - 1];
//				downhierarchy(ni, ni.proto, 0);
//				fpga_reevaluatepips(ni, fn);
//				uphierarchy();
//				if (fn.netcount != 0)
//				{
//					for(i = 0; i < fn.portcount; i++)
//					{
//						if (fn.portlist[i].pp != pp) continue;
//						if ((fn.netlist[fn.portlist[i].con].segactive&ACTIVEPART) != 0) return(TRUE);
//						break;
//					}
//				}
//			}
//		}
//	
//		// propagate
//		if (pp.network != NONETWORK)
//		{
//			for(pi = ni.firstportarcinst; pi != NOPORTARCINST; pi = pi.nextportarcinst)
//			{
//				oai = pi.conarcinst;
//				if (oai == ai) continue;
//				if (pi.proto.network != pp.network) continue;
//				if (oai.end[0].nodeinst == ni) newend = 1; else newend = 0;
//				if (fpga_arcendactive(oai, newend)) return(TRUE);
//			}
//	
//			gettraversalpath(ni.parent, NOWINDOWPART, &nilist, &indexlist, &depth, 1);
//			if (depth > 0)
//			{
//				oni = nilist[depth-1];
//				for(pe = ni.firstportexpinst; pe != NOPORTEXPINST; pe = pe.nextportexpinst)
//				{
//					opp = pe.exportproto;
//					uphierarchy();
//	
//					for(pi = oni.firstportarcinst; pi != NOPORTARCINST; pi = pi.nextportarcinst)
//					{
//						oai = pi.conarcinst;
//						if (pi.proto != opp) continue;
//						if (oai.end[0].nodeinst == oni) newend = 1; else newend = 0;
//						if (fpga_arcendactive(oai, newend)) break;
//					}
//	
//					downhierarchy(oni, oni.proto, 0);
//					if (pi != NOPORTARCINST) return(TRUE);
//				}
//			}
//		}
		return false;
	}
	
	private boolean fpga_arcactive(ArcInst ai)
	{
//		REGISTER INTBIG i, size, cachedepth;
//		REGISTER BOOLEAN value;
//		INTBIG depth, *indexlist;
//		REGISTER NODEINST *oni;
//		REGISTER VARIABLE *var;
//		NODEINST **nilist;
//		UCHAR1 *ptr;
	
//		if (ai.end[0].portarcinst == NOPORTARCINST) return false;
	
		// see if there is a cache on the arc
//		gettraversalpath(ai.parent, NOWINDOWPART, &nilist, &indexlist, &depth, 0);
//		var = getvalkey((INTBIG)ai, VARCINST, VCHAR|VISARRAY, ARCACTIVECACHE_KEY);
//		if (var != NOVARIABLE)
//		{
//			ptr = (UCHAR1 *)var.addr;
//			cachedepth = ((INTBIG *)ptr)[0];   ptr += SIZEOFINTBIG;
//			if (cachedepth == depth)
//			{
//				for(i=0; i<cachedepth; i++)
//				{
//					oni = ((NODEINST **)ptr)[0];   ptr += (sizeof (NODEINST *));
//					if (oni != nilist[i]) break;
//				}
//				if (i >= cachedepth)
//				{
//					// cache applies to this arc: get active factor
//					if (((INTSML *)ptr)[0] == 0) return false;
//					return true;
//				}
//			}
//		}
	
		// compute arc activity
		boolean value = false;
		if (fpga_arcendactive(ai, 0)) value = true; else
			if (fpga_arcendactive(ai, 1)) value = true;
	
		// store the cache
//		size = depth * (sizeof (NODEINST *)) + SIZEOFINTBIG + SIZEOFINTSML;
//		if (size > fpga_arcbufsize)
//		{
//			if (fpga_arcbufsize > 0) efree((CHAR *)fpga_arcbuf);
//			fpga_arcbufsize = 0;
//			fpga_arcbuf = (UCHAR1 *)emalloc(size, fpga_tech.cluster);
//			if (fpga_arcbuf == 0) return(value);
//			fpga_arcbufsize = size;
//		}
//		ptr = fpga_arcbuf;
//		((INTBIG *)ptr)[0] = depth;   ptr += SIZEOFINTBIG;
//		for(i=0; i<depth; i++)
//		{
//			((NODEINST **)ptr)[0] = nilist[i];   ptr += (sizeof (NODEINST *));
//		}
//		((INTSML *)ptr)[0] = value ? 1 : 0;
//		nextchangequiet();
//		setvalkey((INTBIG)ai, VARCINST, ARCACTIVECACHE_KEY, (INTBIG)fpga_arcbuf,
//			VCHAR|VISARRAY|(size<<VLENGTHSH)|VDONTSAVE);
		return value;
	}
	
	/**
	 * Method to reevaluate primitive node "ni" (which is associated with internal
	 * structure "fn").  Finds programming of pips and sets pip and net activity.
	 */
	private void fpga_reevaluatepips(NodeInst ni, FPGANode fn, VarContext context)
	{
//		REGISTER INTBIG i, value, size, cachedepth;
//		INTBIG depth, *indexlist;
//		REGISTER FPGAPIP *fpip;
//		REGISTER NODEINST *oni;
//		REGISTER VARIABLE *var;
//		NODEINST **nilist;
//		UCHAR1 *ptr;
	
		// primitives with no pips or nets need no evaluation
		if (fn.netcount == 0 && fn.pipcount == 0) return;
	
		// see if there is a cache on the node
//		gettraversalpath(ni.parent, NOWINDOWPART, &nilist, &indexlist, &depth, 0);
//		var = getvalkey((INTBIG)ni, VNODEINST, VCHAR|VISARRAY, NODEPIPCACHE_KEY);
//		if (var != NOVARIABLE)
//		{
//			ptr = (UCHAR1 *)var.addr;
//			cachedepth = ((INTBIG *)ptr)[0];   ptr += SIZEOFINTBIG;
//			if (cachedepth == depth)
//			{
//				for(i=0; i<cachedepth; i++)
//				{
//					oni = ((NODEINST **)ptr)[0];   ptr += (sizeof (NODEINST *));
//					if (oni != nilist[i]) break;
//				}
//				if (i >= cachedepth)
//				{
//					// cache applies to this node: get values
//					for(i=0; i<fn.netcount; i++)
//					{
//						value = ((INTSML *)ptr)[0];   ptr += SIZEOFINTSML;
//						if (value != 0) fn.netlist[i].segactive |= ACTIVEPART; else
//							fn.netlist[i].segactive &= ~ACTIVEPART;
//					}
//					for(i=0; i<fn.pipcount; i++)
//					{
//						value = ((INTSML *)ptr)[0];   ptr += SIZEOFINTSML;
//						if (value != 0) fn.piplist[i].pipactive |= ACTIVEPART; else
//							fn.piplist[i].pipactive &= ~ACTIVEPART;
//					}
//					return;
//				}
//			}
//		}
	
		// reevaluate: presume all nets and pips are inactive
		for(int i=0; i<fn.netcount; i++) fn.netlist[i].segactive &= ~ACTIVEPART;
		for(int i=0; i<fn.pipcount; i++) fn.piplist[i].pipactive &= ~ACTIVEPART;
	
		// look for pip programming
		fpga_findvariableobjects(fn, ni, ACTIVEPIPS_KEY, true, context);
	
		// set nets active where they touch active pips
		for(int i=0; i<fn.pipcount; i++)
		{
			FPGAPip fpip = fn.piplist[i];
			if ((fpip.pipactive&ACTIVEPART) == 0) continue;
			if (fpip.con1 > 0) fn.netlist[fpip.con1].segactive |= ACTIVEPART;
			if (fpip.con2 > 0) fn.netlist[fpip.con2].segactive |= ACTIVEPART;
		}
	
//		// store the cache
//		size = depth * (sizeof (NODEINST *)) + SIZEOFINTBIG + fn.netcount * SIZEOFINTSML +
//			fn.pipcount * SIZEOFINTSML;
//		if (size > fpga_pipbufsize)
//		{
//			if (fpga_pipbufsize > 0) efree((CHAR *)fpga_pipbuf);
//			fpga_pipbufsize = 0;
//			fpga_pipbuf = (UCHAR1 *)emalloc(size, fpga_tech.cluster);
//			if (fpga_pipbuf == 0) return;
//			fpga_pipbufsize = size;
//		}
//		ptr = fpga_pipbuf;
//		((INTBIG *)ptr)[0] = depth;   ptr += SIZEOFINTBIG;
//		for(i=0; i<depth; i++)
//		{
//			((NODEINST **)ptr)[0] = nilist[i];   ptr += (sizeof (NODEINST *));
//		}
//		for(i=0; i<fn.netcount; i++)
//		{
//			if ((fn.netlist[i].segactive&ACTIVEPART) != 0) ((INTSML *)ptr)[0] = 1; else
//				((INTSML *)ptr)[0] = 0;
//			ptr += SIZEOFINTSML;
//		}
//		for(i=0; i<fn.pipcount; i++)
//		{
//			if ((fn.piplist[i].pipactive&ACTIVEPART) != 0) ((INTSML *)ptr)[0] = 1; else
//				((INTSML *)ptr)[0] = 0;
//			ptr += SIZEOFINTSML;
//		}
//		nextchangequiet();
//		setvalkey((INTBIG)ni, VNODEINST, NODEPIPCACHE_KEY, (INTBIG)fpga_pipbuf,
//			VCHAR|VISARRAY|(size<<VLENGTHSH)|VDONTSAVE);
	}
	
	/**
	 * Method to examine primitive node "ni" and return true if the repeater is active.
	 */
	private boolean fpga_repeateractive(NodeInst ni)
	{
		fpga_repeatername = ni.getName();
		fpga_repeaterisactive = false;
		fpga_findvariableobjects(null, ni, ACTIVEREPEATERS_KEY, false, null);
		return fpga_repeaterisactive;
	}

//	/*
//	 * Routine to clear the cache of arc activity in the current cell.  If "ni" is NONODEINST,
//	 * clear all node caches as well, otherwise only clear the node cache on "ni".
//	 */
//	void fpga_clearcache(NODEINST *ni)
//	{
//		REGISTER VARIABLE *var;
//		REGISTER ARCINST *ai;
//		REGISTER NODEPROTO *np;
//	
//		np = getcurcell();
//		if (np == NONODEPROTO)
//		{
//			ttyputerr(_("Must edit a cell to clear its cache"));
//			return;
//		}
//		for(ai = np.firstarcinst; ai != NOARCINST; ai = ai.nextarcinst)
//		{
//			var = getvalkey((INTBIG)ai, VARCINST, VCHAR|VISARRAY, ARCACTIVECACHE_KEY);
//			if (var != NOVARIABLE)
//				(void)delvalkey((INTBIG)ai, VARCINST, ARCACTIVECACHE_KEY);
//		}
//		if (ni != NONODEINST)
//		{
//			var = getvalkey((INTBIG)ni, VNODEINST, VCHAR|VISARRAY, NODEPIPCACHE_KEY);
//			if (var != NOVARIABLE)
//				(void)delvalkey((INTBIG)ni, VNODEINST, NODEPIPCACHE_KEY);
//		}
//	}

	Nodable [] path = new Nodable[100];

	private void fpga_findvariableobjects(FPGANode fn, NodeInst ni, Variable.Key varkey, boolean setPips, VarContext context)
	{
		// search hierarchical path
		int depth = 0;
		path[depth++] = ni;
		while(context != null)
		{
			Nodable niClimb = context.getNodable();
            if (niClimb == null) break;
			path[depth++] = niClimb;
			context = context.pop();
		}

		// look for programming variables on the nodes
		for(int c=0; c<depth; c++)
		{
			Nodable niClimb = path[c];
			Variable var = niClimb.getVar(varkey);
			if (var == null) continue;

			// found pip settings: evaluate them
			String pt = (String)var.getObject();
			String [] pipNames = pt.split(" ");
			for(int i=0; i<pipNames.length; i++)
			{
				String start = pipNames[i];
				if (start.length() == 0) continue;

				// find pip name in "start"
				String [] pipParts = start.split("\\.");
				if (pipParts.length == 0 || pipParts.length > depth) continue;
				boolean pathgood = true;
				VarContext climb = context;
				for(int j=0; j<pipParts.length-1; j++)
				{
					if (!pipParts[j].equalsIgnoreCase(path[depth-2-j].getName()))
					{
						pathgood = false;
						break;
					}
				}
				if (pathgood)
				{
					String lastPart = pipParts[pipParts.length-1];
					if (setPips)
					{
						for(int k=0; k<fn.pipcount; k++)
							if (fn.piplist[k].pipname.equalsIgnoreCase(lastPart))
						{
							fn.piplist[i].pipactive |= ACTIVEPART;
						}
					} else
					{
						if (fpga_repeatername.equalsIgnoreCase(lastPart)) fpga_repeaterisactive = true;
					}
				}
			}
			break;
		}
	}
	
//	/*
//	 * Routine to fill polygon "poly" with a description of pip "pipindex" on node "ni"
//	 * which is a FPGA NODE "fn".
//	 */
//	void fpga_describepip(NODEINST *ni, FPGANODE *fn, INTBIG pipindex, POLYGON *poly)
//	{
//		REGISTER INTBIG xc, yc, lambda;
//	
//		lambda = lambdaofnode(ni);
//		if (poly.limit < 2) (void)extendpolygon(poly, 2);
//		xc = (ni.lowx+ni.highx)/2;
//		yc = (ni.lowy+ni.highy)/2;
//		poly.xv[0] = fn.piplist[pipindex].posx + xc - lambda;
//		poly.yv[0] = fn.piplist[pipindex].posy + yc - lambda;
//		poly.xv[1] = fn.piplist[pipindex].posx + xc + lambda;
//		poly.yv[1] = fn.piplist[pipindex].posy + yc + lambda;
//		poly.count = 2;
//		poly.style = FILLEDRECT;
//		poly.layer = LPIP;
//		poly.desc = fpga_layers[poly.layer];
//	}
//	
//	/*
//	 * Routine to fill polygon "poly" with a description of network segment "whichseg"
//	 * on node "ni".  The network is in "fnet".
//	 */
//	void fpga_describenetseg(NODEINST *ni, FPGANET *fnet, INTBIG whichseg, POLYGON *poly)
//	{
//		REGISTER INTBIG xc, yc;
//	
//		if (poly.limit < 2) (void)extendpolygon(poly, 2);
//		xc = (ni.lowx+ni.highx)/2;
//		yc = (ni.lowy+ni.highy)/2;
//		poly.xv[0] = fnet.segfx[whichseg] + xc;
//		poly.yv[0] = fnet.segfy[whichseg] + yc;
//		poly.xv[1] = fnet.segtx[whichseg] + xc;
//		poly.yv[1] = fnet.segty[whichseg] + yc;
//		poly.count = 2;
//		poly.style = OPENED;
//		poly.layer = LWIRE;
//	}
	
	/******************** ARCHITECTURE FILE READING ********************/

	public static void readArchitectureFile(boolean placeAndWire)
	{
		if (fpga_nodecount != 0)
		{
			System.out.println("This technology already has primitives defined");
			return;
		}

		// get architecture file
		String fileName = OpenFile.chooseInputFile(FileType.FPGA, null);
		if (fileName == null) return;

		// read the file
		LispTree lt = fpga_readfile(fileName);
		if (lt == null)
		{
			System.out.println("Error reading file");
			return;
		}
		System.out.println("FPGA file read");

		// turn the tree into primitives
		new BuildTechnology(lt, placeAndWire);
	}

	public static void setWireDisplay(int level)
	{
		switch (level)
		{
			case 0:		// no wires
				fpga_internaldisplay = (fpga_internaldisplay & ~DISPLAYLEVEL) | NOPRIMDISPLAY;
				break;
			case 1:		// active wires
				fpga_internaldisplay = (fpga_internaldisplay & ~DISPLAYLEVEL) | ACTIVEPRIMDISPLAY;
				break;
			case 2:		// all wires
				fpga_internaldisplay = (fpga_internaldisplay & ~DISPLAYLEVEL) | FULLPRIMDISPLAY;
				break;
		}
	}

	/**
	 * This class implement the command to build an FPGA technology.
	 */
	private static class BuildTechnology extends Job
	{
		private LispTree lt;
		private boolean placeAndWire;

		protected BuildTechnology(LispTree lt, boolean placeAndWire)
		{
			super("Build FPGA Technology", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.lt = lt;
			this.placeAndWire = placeAndWire;
			startJob();
		}

		public boolean doIt()
		{
			int total = fpga_makeprimitives(lt);
			System.out.println("Created " + total + " primitives");
//			net_redoprim();

			// place and wire the primitives
			if (placeAndWire)
			{
				Cell topcell = fpga_placeprimitives(lt);
				if (topcell != null)
				{
					// recompute bounds
//					(*el_curconstraint.solve)(NONODEPROTO);

					// recompute networks
//					(void)asktool(net_tool, x_("total-re-number"));

					// display top cell
//					subpar[0] = describenodeproto(topcell);
//					us_editcell(1, subpar);
				}
			}
			return true;
		}
	}

	/**
	 * Method to read the FPGA file in "f" and create a LISPTREE structure which is returned.
	 * Returns zero on error.
	 */
	private static LispTree fpga_readfile(String fileName)
	{	
		// make the tree top
		LispTree treetop = new LispTree();
		treetop.keyword = "TOP";
	
		// initialize current position and stack
		fpga_treepos = treetop;
		fpga_treedepth = 0;

		URL url = TextUtils.makeURLToFile(fileName);
		try
		{
			URLConnection urlCon = url.openConnection();
			InputStreamReader is = new InputStreamReader(urlCon.getInputStream());
			LineNumberReader lnr = new LineNumberReader(is);

			// read the file
			for(;;)
			{
				// get the next line of text
				String line = lnr.readLine();
				if (line == null) break;
		
				// stop now if it is a comment
				line = line.trim();
				if (line.length() == 0) continue;
				if (line.charAt(0) == '#') continue;

				// keep parsing it
				int pt = 0;
				for(;;)
				{
					// skip spaces
					while (pt < line.length() && Character.isWhitespace(line.charAt(pt))) pt++;
					if (pt >= line.length()) break;
		
					// check for special characters
					char chr = line.charAt(pt);
					if (chr == ')')
					{
						if (fpga_pushkeyword(line.substring(pt, pt+1), lnr)) return null;
						pt++;
						continue;
					}
		
					// gather a keyword
					int ptEnd = pt;
					for(;;)
					{
						if (ptEnd >= line.length()) break;
						char chEnd = line.charAt(ptEnd);
						if (chEnd == ')' || Character.isWhitespace(chEnd)) break;
						if (chEnd == '"')
						{
							ptEnd++;
							for(;;)
							{
								if (ptEnd >= line.length() || line.charAt(ptEnd) == '"') break;
								ptEnd++;
							}
							if (ptEnd < line.length()) ptEnd++;
							break;
						}
						ptEnd++;
					}
					if (fpga_pushkeyword(line.substring(pt, ptEnd), lnr)) return null;
					pt = ptEnd;
				}
			}
			lnr.close();
			System.out.println(fileName + " read");
		} catch (IOException e)
		{
			System.out.println("Error reading " + fileName);
			return null;
		}
	
		if (fpga_treedepth != 0)
		{
			System.out.println("Not enough close parenthesis in file");
			return null;
		}
		return treetop;
	}
	
	/**
	 * Method to add the next keyword "keyword" to the lisp tree in the globals.
	 * Returns true on error.
	 */
	private static boolean fpga_pushkeyword(String keyword, LineNumberReader lnr)
	{
		if (keyword.startsWith("("))
		{
			if (fpga_treedepth >= MAXDEPTH)
			{
				System.out.println("Nesting too deep (more than " + MAXDEPTH + ")");
				return true;
			}
	
			// create a new tree branch
			LispTree newtree = new LispTree();
			newtree.lineno = lnr.getLineNumber();
	
			// add branch to previous branch
			fpga_addparameter(fpga_treepos, PARAMBRANCH, newtree);
	
			// add keyword
			int pt = 1;
			while (pt < keyword.length() && Character.isWhitespace(keyword.charAt(pt))) pt++;
			newtree.keyword = keyword.substring(pt);
	
			// push tree onto stack
			fpga_treestack[fpga_treedepth] = fpga_treepos;
			fpga_treedepth++;
			fpga_treepos = newtree;
			return false;
		}
	
		if (keyword.equals(")"))
		{
			// pop tree stack
			if (fpga_treedepth <= 0)
			{
				System.out.println("Too many close parenthesis");
				return true;
			}
			fpga_treedepth--;
			fpga_treepos = fpga_treestack[fpga_treedepth];
			return false;
		}
	
		// just add the atomic keyword
		if (keyword.startsWith("\"") && keyword.endsWith("\""))
			keyword = keyword.substring(1, keyword.length()-1);
		fpga_addparameter(fpga_treepos, PARAMATOM, keyword);
		return false;
	}
	
	/**
	 * Method to add a parameter of type "type" and value "value" to the tree element "tree".
	 * Returns true on memory error.
	 */
	private static void fpga_addparameter(LispTree tree, int type, Object value)
	{
		tree.paramtype.add(new Integer(type));
		tree.paramvalue.add(value);
	}
	
	/******************** ARCHITECTURE PARSING: PRIMITIVES ********************/
	
	/**
	 * Method to parse the entire tree and create primitives.
	 * Returns the number of primitives made.
	 */
	private static int fpga_makeprimitives(LispTree lt)
	{
		// look through top level for the "primdef"s
		int total = 0;
		for(int i=0; i<lt.paramtype.size(); i++)
		{
			Integer type = (Integer)lt.paramtype.get(i);
			if (type.intValue() != PARAMBRANCH) continue;
			LispTree sublt = (LispTree)lt.paramvalue.get(i);
			if (!sublt.keyword.equalsIgnoreCase("primdef")) continue;
	
			// create the primitive
			if (fpga_makeprimitive(sublt)) return(0);
			total++;
		}
		return total;
	}
	
	/**
	 * Method to create a primitive from a subtree "lt".
	 * Tree has "(primdef...)" structure.
	 */
	private static boolean fpga_makeprimitive(LispTree lt)
	{
		// find all of the pieces of this primitive
		LispTree ltattribute = null, ltnets = null, ltports = null, ltcomponents = null;
		for(int i=0; i<lt.paramtype.size(); i++)
		{
			Integer type = (Integer)lt.paramtype.get(i);
			if (type.intValue() != PARAMBRANCH) continue;
			LispTree scanlt = (LispTree)lt.paramvalue.get(i);
			if (scanlt.keyword.equalsIgnoreCase("attributes"))
			{
				if (ltattribute != null)
				{
					System.out.println("Multiple 'attributes' sections for a primitive (line " + scanlt.lineno + ")");
					return true;
				}
				ltattribute = scanlt;
				continue;
			}
			if (scanlt.keyword.equalsIgnoreCase("nets"))
			{
				if (ltnets != null)
				{
					System.out.println("Multiple 'nets' sections for a primitive (line " + scanlt.lineno + ")");
					return true;
				}
				ltnets = scanlt;
				continue;
			}
			if (scanlt.keyword.equalsIgnoreCase("ports"))
			{
				if (ltports != null)
				{
					System.out.println("Multiple 'ports' sections for a primitive (line " + scanlt.lineno + ")");
					return true;
				}
				ltports = scanlt;
				continue;
			}
			if (scanlt.keyword.equalsIgnoreCase("components"))
			{
				if (ltcomponents != null)
				{
					System.out.println("Multiple 'components' sections for a primitive (line " + scanlt.lineno + ")");
					return true;
				}
				ltcomponents = scanlt;
				continue;
			}
		}
	
		// scan the attributes section
		if (ltattribute == null)
		{
			System.out.println("Missing 'attributes' sections on a primitive (line " + lt.lineno + ")");
			return true;
		}
		String primname = null;
		String primsizex = null;
		String primsizey = null;
		for(int j=0; j<ltattribute.paramtype.size(); j++)
		{
			Integer type = (Integer)ltattribute.paramtype.get(j);
			if (type.intValue() != PARAMBRANCH) continue;
			LispTree scanlt = (LispTree)ltattribute.paramvalue.get(j);
			if (scanlt.keyword.equalsIgnoreCase("name"))
			{
				if (scanlt.paramtype.size() != 1 || ((Integer)scanlt.paramtype.get(0)).intValue() != PARAMATOM)
				{
					System.out.println("Primitive 'name' attribute should take a single atomic parameter (line " + scanlt.lineno + ")");
					return true;
				}
				primname = (String)scanlt.paramvalue.get(0);
				continue;
			}
			if (scanlt.keyword.equalsIgnoreCase("size"))
			{
				if (scanlt.paramtype.size() != 2 || ((Integer)scanlt.paramtype.get(0)).intValue() != PARAMATOM ||
						((Integer)scanlt.paramtype.get(1)).intValue() != PARAMATOM)
				{
					System.out.println("Primitive 'size' attribute should take two atomic parameters (line " + scanlt.lineno + ")");
					return true;
				}
				primsizex = (String)scanlt.paramvalue.get(0);
				primsizey = (String)scanlt.paramvalue.get(1);
				continue;
			}
		}
	
		// make sure a name and size were given
		if (primname == null)
		{
			System.out.println("Missing 'name' attribute in primitive definition (line " + ltattribute.lineno + ")");
			return true;
		}
		if (primsizex == null || primsizey == null)
		{
			System.out.println("Missing 'size' attribute in primitive definition (line " + ltattribute.lineno + ")");
			return true;
		}
	
		// make the primitive
		double sizex = TextUtils.atof(primsizex);
		double sizey = TextUtils.atof(primsizey);
		FPGANode primnp = new FPGANode(primname, tech, sizex, sizey, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(tech.fpga_c_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, new Technology.TechPoint[] {
					new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge()),
				})
			});
		primnp.setLockedPrim();
		fpga_nodecount++;

		// add any unrecognized attributes
		for(int j=0; j<ltattribute.paramtype.size(); j++)
		{
			Integer type = (Integer)ltattribute.paramtype.get(j);
			if (type.intValue() != PARAMBRANCH) continue;
			LispTree scanlt = (LispTree)ltattribute.paramvalue.get(j);
			if (scanlt.keyword.equalsIgnoreCase("name")) continue;
			if (scanlt.keyword.equalsIgnoreCase("size")) continue;
	
			if (scanlt.paramtype.size() != 1 || ((Integer)scanlt.paramtype.get(0)).intValue() != PARAMATOM)
			{
				System.out.println("Attribute '" + scanlt.keyword +
					"' attribute should take a single atomic parameter (line " + scanlt.lineno + ")");
				return true;
			}
//			primnp.newVar(scanlt.keyword, (String)scanlt.paramvalue.get(0));
		}

		// get ports
		primnp.portcount = 0;
		if (ltports != null)
		{
			// count ports
			for(int j=0; j<ltports.paramtype.size(); j++)
			{
				if (((Integer)ltports.paramtype.get(j)).intValue() != PARAMBRANCH) continue;
				LispTree scanlt = (LispTree)ltports.paramvalue.get(j);
				if (scanlt.keyword.equalsIgnoreCase("port")) primnp.portcount++;
			}
	
			// create local port structures
			primnp.portlist = new FPGAPort[primnp.portcount];
			PrimitivePort [] ports = new PrimitivePort[primnp.portcount];
			for(int i=0; i<primnp.portcount; i++)
			{
				primnp.portlist[i] = new FPGAPort();
				primnp.portlist[i].pp = null;
			}
	
			// create the ports
			int portNumber = 0;
			for(int j=0; j<ltports.paramtype.size(); j++)
			{
				Integer type = (Integer)ltports.paramtype.get(j);
				if (type.intValue() != PARAMBRANCH) continue;
				LispTree scanlt = (LispTree)ltports.paramvalue.get(j);
				if (scanlt.keyword.equalsIgnoreCase("port"))
				{
					if (fpga_makeprimport(primnp, scanlt, primnp.portlist[portNumber], portNumber)) return true;
					ports[portNumber] = primnp.portlist[portNumber].pp;
					for(int k=0; k<portNumber; k++)
					{
						if (ports[k].getName().equalsIgnoreCase(ports[portNumber].getName()))
						{
							System.out.println("Duplicate port name: " + ports[portNumber].getName() + " (line " + scanlt.lineno + ")");
							return true;
						}
					}
					portNumber++;
				}
			}

			// add ports to the primitive
			primnp.addPrimitivePorts(ports);
		}
	
		// get nets
		primnp.netcount = 0;
		if (ltnets != null)
		{
			// count the nets
			for(int j=0; j<ltnets.paramtype.size(); j++)
			{
				Integer type = (Integer)ltnets.paramtype.get(j);
				if (type.intValue() != PARAMBRANCH) continue;
				LispTree scanlt = (LispTree)ltnets.paramvalue.get(j);
				if (scanlt.keyword.equalsIgnoreCase("net")) primnp.netcount++;
			}
	
			// create local net structures
			primnp.netlist = new FPGANet[primnp.netcount];
			for(int i=0; i<primnp.netcount; i++)
			{
				primnp.netlist[i] = new FPGANet();
				primnp.netlist[i].netname = null;
				primnp.netlist[i].segcount = 0;
			}
	
			// create the nets
			int i = 0;
			for(int j=0; j<ltnets.paramtype.size(); j++)
			{
				Integer type = (Integer)ltnets.paramtype.get(j);
				if (type.intValue() != PARAMBRANCH) continue;
				LispTree scanlt = (LispTree)ltnets.paramvalue.get(j);
				if (scanlt.keyword.equalsIgnoreCase("net"))
				{
					if (fpga_makeprimnet(primnp, scanlt, primnp, primnp.netlist[i])) return true;
					i++;
				}
			}
		}
	
		// associate nets and ports
		for(int k=0; k<primnp.portcount; k++)
		{
			FPGAPort fp = primnp.portlist[k];
			for(int i=0; i<primnp.netcount; i++)
			{
				boolean found = false;
				for(int j=0; j<primnp.netlist[i].segcount; j++)
				{
					if ((primnp.netlist[i].segf[j].getX() == fp.posx && primnp.netlist[i].segf[j].getY() == fp.posy) ||
						(primnp.netlist[i].segt[j].getX() == fp.posx && primnp.netlist[i].segt[j].getY() == fp.posy))
					{
						fp.con = i;
						found = true;
						break;
					}
				}
				if (found) break;
			}
		}
	
		// set electrical connectivity
		for(int k=0; k<primnp.portcount; k++)
		{
			if (primnp.portlist[k].con < 0)
			{
//				primnp.portlist[k].pp.userbits |= PORTNET;
			} else
			{
//				if (primnp.portlist[k].con >= (PORTNET >> PORTNETSH))
//				{
//					System.out.println("Too many networks in FPGA primitive");
//				}
//				primnp.portlist[k].pp.userbits |= (primnp.portlist[k].con >> PORTNETSH);
			}
		}
	
		// get pips
		primnp.pipcount = 0;
		if (ltcomponents != null)
		{
			// count the pips
			for(int j=0; j<ltcomponents.paramtype.size(); j++)
			{
				Integer type = (Integer)ltcomponents.paramtype.get(j);
				if (type.intValue() != PARAMBRANCH) continue;
				LispTree scanlt = (LispTree)ltcomponents.paramvalue.get(j);
				if (scanlt.keyword.equalsIgnoreCase("pip")) primnp.pipcount++;
			}
	
			// create local pips structures
			primnp.piplist = new FPGAPip[primnp.pipcount];
			for(int i=0; i<primnp.pipcount; i++)
			{
				primnp.piplist[i] = new FPGAPip();
			}
	
			// create the pips
			int i = 0;
			for(int j=0; j<ltcomponents.paramtype.size(); j++)
			{
				Integer type = (Integer)ltcomponents.paramtype.get(j);
				if (type.intValue() != PARAMBRANCH) continue;
				LispTree scanlt = (LispTree)ltcomponents.paramvalue.get(j);
				if (scanlt.keyword.equalsIgnoreCase("pip"))
				{
					if (fpga_makeprimpip(primnp, scanlt, primnp, primnp.piplist[i])) return true;
					i++;
				}
			}
		}
		return false;
	}
	
	/**
	 * Method to add a port to primitive "np" from the tree in "lt" and
	 * store information about it in the local structure "fp".
	 * Tree has "(port...)" structure.  Returns true on error.
	 */
	private static boolean fpga_makeprimport(PrimitiveNode np, LispTree lt, FPGAPort fp, int net)
	{
		// look for keywords
		LispTree ltname = null, ltposition = null, ltdirection = null;
		for(int j=0; j<lt.paramtype.size(); j++)
		{
			Integer type = (Integer)lt.paramtype.get(j);
			if (type.intValue() != PARAMBRANCH) continue;
			LispTree scanlt = (LispTree)lt.paramvalue.get(j);
			if (scanlt.keyword.equalsIgnoreCase("name"))
			{
				ltname = scanlt;
				continue;
			}
			if (scanlt.keyword.equalsIgnoreCase("position"))
			{
				ltposition = scanlt;
				continue;
			}
			if (scanlt.keyword.equalsIgnoreCase("direction"))
			{
				ltdirection = scanlt;
				continue;
			}
		}
	
		// validate
		if (ltname == null)
		{
			System.out.println("Port has no name (line " + lt.lineno + ")");
			return true;
		}
		if (ltname.paramtype.size() != 1 || ((Integer)ltname.paramtype.get(0)).intValue() != PARAMATOM)
		{
			System.out.println("Port name must be a single atom (line " + ltname.lineno + ")");
			return true;
		}
		if (ltposition == null)
		{
			System.out.println("Port has no position (line " + lt.lineno + ")");
			return true;
		}
		if (ltposition.paramtype.size() != 2 || ((Integer)ltposition.paramtype.get(0)).intValue() != PARAMATOM ||
			((Integer)ltposition.paramtype.get(1)).intValue() != PARAMATOM)
		{
			System.out.println("Port position must be two atoms (line " + ltposition.lineno + ")");
			return true;
		}
	
		// determine directionality
		PortCharacteristic characteristic = PortCharacteristic.UNKNOWN;
		if (ltdirection != null)
		{
			if (ltdirection.paramtype.size() != 1 || ((Integer)ltdirection.paramtype.get(0)).intValue() != PARAMATOM)
			{
				System.out.println("Port direction must be a single atom (line " + ltdirection.lineno + ")");
				return true;
			}
			String dir = (String)ltdirection.paramvalue.get(0);
			if (dir.equalsIgnoreCase("input")) characteristic = PortCharacteristic.IN; else
				if (dir.equalsIgnoreCase("output")) characteristic = PortCharacteristic.OUT; else
					if (dir.equalsIgnoreCase("bidir")) characteristic = PortCharacteristic.BIDIR; else
			{
				System.out.println("Unknown port direction (line " + ltdirection.lineno + ")");
				return true;
			}
		}

		// create the portproto
		double x = TextUtils.atof((String)ltposition.paramvalue.get(0)) - np.getDefWidth()/2;
		double y = TextUtils.atof((String)ltposition.paramvalue.get(1)) - np.getDefHeight()/2;
		PrimitivePort newpp = PrimitivePort.newInstance(tech, np, new ArcProto [] {tech.wire_arc},
			(String)ltname.paramvalue.get(0), 0,180, net, characteristic,
			EdgeH.fromCenter(x), EdgeV.fromCenter(y), EdgeH.fromCenter(x), EdgeV.fromCenter(y));

		// add it to the local port structure
		fp.posx = x; // + np.lowx;
		fp.posy = y; // + np.lowy;
		fp.pp = newpp;
		fp.con = -1;
		return false;
	}
	
	/**
	 * Method to add a net to primitive "np" from the tree in "lt" and store information
	 * about it in the local object "fnet".
	 * Tree has "(net...)" structure.  Returns true on error.
	 */
	private static boolean fpga_makeprimnet(PrimitiveNode np, LispTree lt, FPGANode fn, FPGANet fnet)
	{
		// scan for information in the tree
		fnet.netname = null;
		fnet.segcount = 0;
		Point2D [] seg = new Point2D[2];
		for(int j=0; j<lt.paramtype.size(); j++)
		{
			if (((Integer)lt.paramtype.get(j)).intValue() != PARAMBRANCH) continue;
			LispTree scanlt = (LispTree)lt.paramvalue.get(j);
			if (scanlt.keyword.equalsIgnoreCase("name") && scanlt.paramtype.size() == 1 &&
				((Integer)scanlt.paramtype.get(0)).intValue() == PARAMATOM)
			{
				if (fnet.netname != null)
				{
					System.out.println("Multiple names for network (line " + lt.lineno + ")");
					return true;
				}
				fnet.netname = (String)scanlt.paramvalue.get(0);
				continue;
			}
			if (scanlt.keyword.equalsIgnoreCase("segment"))
			{
				int pos = 0;
				for(int i=0; i<2; i++)
				{
					// get end of net segment
					if (scanlt.paramtype.size() < pos+1)
					{
						System.out.println("Incomplete block net segment (line " + scanlt.lineno + ")");
						return true;
					}
					if (((Integer)scanlt.paramtype.get(pos)).intValue() != PARAMATOM)
					{
						System.out.println("Must have atoms in block net segment (line " + scanlt.lineno + ")");
						return true;
					}
					if (((String)scanlt.paramvalue.get(pos)).equalsIgnoreCase("coord"))
					{
						if (scanlt.paramtype.size() < pos+3)
						{
							System.out.println("Incomplete block net segment (line " + scanlt.lineno + ")");
							return true;
						}
						if (((Integer)scanlt.paramtype.get(pos+1)).intValue() != PARAMATOM ||
							((Integer)scanlt.paramtype.get(pos+2)).intValue() != PARAMATOM)
						{
							System.out.println("Must have atoms in block net segment (line " + scanlt.lineno + ")");
							return true;
						}
						double x = TextUtils.atof((String)scanlt.paramvalue.get(pos+1)) - np.getDefWidth()/2;
						double y = TextUtils.atof((String)scanlt.paramvalue.get(pos+2)) - np.getDefHeight()/2;
						seg[i] = new Point2D.Double(x, y);
						pos += 3;
					} else if (((String)scanlt.paramvalue.get(pos)).equalsIgnoreCase("port"))
					{
						if (scanlt.paramtype.size() < pos+2)
						{
							System.out.println("Incomplete block net segment (line " + scanlt.lineno + ")");
							return true;
						}
						if (((Integer)scanlt.paramtype.get(pos+1)).intValue() != PARAMATOM)
						{
							System.out.println("Must have atoms in block net segment (line " + scanlt.lineno + ")");
							return true;
						}
	
						// find port
						int found = -1;
						for(int k=0; k<fn.portcount; k++)
						{
							if (fn.portlist[k].pp.getName().equalsIgnoreCase((String)scanlt.paramvalue.get(pos+1)))
							{
								found = k;
								break;
							}
						}
						if (found < 0)
						{
							System.out.println("Unknown port on primitive net segment (line " + scanlt.lineno + ")");
							return true;
						}
						double x = fn.portlist[found].posx;
						double y = fn.portlist[found].posy;
						seg[i] = new Point2D.Double(x, y);
						pos += 2;
					} else
					{
						System.out.println("Unknown keyword '" + (String)scanlt.paramvalue.get(pos) +
							"' in block net segment (line " + scanlt.lineno + ")");
						return true;
					}
				}

				Point2D [] newFrom = new Point2D[fnet.segcount+1];
				Point2D [] newTo = new Point2D[fnet.segcount+1];
				for(int i=0; i<fnet.segcount; i++)
				{
					newFrom[i] = fnet.segf[i];
					newTo[i] = fnet.segt[i];
				}
				newFrom[fnet.segcount] = seg[0];
				newTo[fnet.segcount] = seg[1];
				fnet.segf = newFrom;
				fnet.segt = newTo;
				fnet.segcount++;
				continue;
			}
		}
		return false;
	}
	
	/**
	 * Method to add a pip to primitive "np" from the tree in "lt" and save
	 * information about it in the local object "fpip".
	 * Tree has "(pip...)" structure.  Returns true on error.
	 */
	private static boolean fpga_makeprimpip(PrimitiveNode np, LispTree lt, FPGANode fn, FPGAPip fpip)
	{
		// scan for information in this FPGAPIP object
		fpip.pipname = null;
		fpip.con1 = fpip.con2 = -1;
		for(int j=0; j<lt.paramtype.size(); j++)
		{
			if (((Integer)lt.paramtype.get(j)).intValue() != PARAMBRANCH) continue;
			LispTree scanlt = (LispTree)lt.paramvalue.get(j);
			if (scanlt.keyword.equalsIgnoreCase("name") && scanlt.paramtype.size() == 1 &&
				((Integer)scanlt.paramtype.get(0)).intValue() == PARAMATOM)
			{
				if (fpip.pipname != null)
				{
					System.out.println("Multiple names for pip (line " + lt.lineno + ")");
					return true;
				}
				fpip.pipname = (String)scanlt.paramvalue.get(0);
				continue;
			}
			if (scanlt.keyword.equalsIgnoreCase("position") && scanlt.paramtype.size() == 2 &&
				((Integer)scanlt.paramtype.get(0)).intValue() == PARAMATOM &&
				((Integer)scanlt.paramtype.get(1)).intValue() == PARAMATOM)
			{
				fpip.posx = TextUtils.atof((String)scanlt.paramvalue.get(0)); // + np.lowx;
				fpip.posy = TextUtils.atof((String)scanlt.paramvalue.get(1)); // + np.lowy;
				continue;
			}
			if (scanlt.keyword.equalsIgnoreCase("connectivity") && scanlt.paramtype.size() == 2 &&
				((Integer)scanlt.paramtype.get(0)).intValue() == PARAMATOM &&
				((Integer)scanlt.paramtype.get(1)).intValue() == PARAMATOM)
			{
				for(int i=0; i<fn.netcount; i++)
				{
					if (fn.netlist[i].netname.equalsIgnoreCase((String)scanlt.paramvalue.get(0)))
						fpip.con1 = i;
					if (fn.netlist[i].netname.equalsIgnoreCase((String)scanlt.paramvalue.get(1)))
						fpip.con2 = i;
				}
				continue;
			}
		}
		return false;
	}
	
	/******************** ARCHITECTURE PARSING: LAYOUT ********************/
	
	/**
	 * Method to scan the entire tree for block definitions and create them.
	 */
	private static Cell fpga_placeprimitives(LispTree lt)
	{
		// look through top level for the "blockdef"s
		Cell toplevel = null;
		for(int i=0; i<lt.paramtype.size(); i++)
		{
			if (((Integer)lt.paramtype.get(i)).intValue() != PARAMBRANCH) continue;
			LispTree sublt = (LispTree)lt.paramvalue.get(i);
			if (!sublt.keyword.equalsIgnoreCase("blockdef") &&
				!sublt.keyword.equalsIgnoreCase("architecture")) continue;
	
			// create the primitive
			Cell np = fpga_makecell(sublt);
			if (np == null) return null;
			if (sublt.keyword.equalsIgnoreCase("architecture")) toplevel = np;
		}
		return toplevel;
	}
	
	/**
	 * Method to create a cell from a subtree "lt".
	 * Tree has "(blockdef...)" or "(architecture...)" structure.
	 * Returns nonzero on error.
	 */
	private static Cell fpga_makecell(LispTree lt)
	{
		// find all of the pieces of this block
		LispTree ltattribute = null, ltnets = null, ltports = null, ltcomponents = null;
		for(int i=0; i<lt.paramtype.size(); i++)
		{
			if (((Integer)lt.paramtype.get(i)).intValue() != PARAMBRANCH) continue;
			LispTree scanlt = (LispTree)lt.paramvalue.get(i);
			if (scanlt.keyword.equalsIgnoreCase("attributes"))
			{
				if (ltattribute != null)
				{
					System.out.println("Multiple 'attributes' sections for a block (line " + lt.lineno + ")");
					return null;
				}
				ltattribute = scanlt;
				continue;
			}
			if (scanlt.keyword.equalsIgnoreCase("nets"))
			{
				if (ltnets != null)
				{
					System.out.println("Multiple 'nets' sections for a block (line " + lt.lineno + ")");
					return null;
				}
				ltnets = scanlt;
				continue;
			}
			if (scanlt.keyword.equalsIgnoreCase("ports"))
			{
				if (ltports != null)
				{
					System.out.println("Multiple 'ports' sections for a block (line " + lt.lineno + ")");
					return null;
				}
				ltports = scanlt;
				continue;
			}
			if (scanlt.keyword.equalsIgnoreCase("components"))
			{
				if (ltcomponents != null)
				{
					System.out.println("Multiple 'components' sections for a block (line " + lt.lineno + ")");
					return null;
				}
				ltcomponents = scanlt;
				continue;
			}
		}
	
		// scan the attributes section
		if (ltattribute == null)
		{
			System.out.println("Missing 'attributes' sections on a block (line " + lt.lineno + ")");
			return null;
		}
		String blockname = null;
		boolean gotsize = false;
		double sizex = 0, sizey = 0;
		for(int j=0; j<ltattribute.paramtype.size(); j++)
		{
			if (((Integer)ltattribute.paramtype.get(j)).intValue() != PARAMBRANCH) continue;
			LispTree scanlt = (LispTree)ltattribute.paramvalue.get(j);
			if (scanlt.keyword.equalsIgnoreCase("name"))
			{
				if (scanlt.paramtype.size() != 1 || ((Integer)scanlt.paramtype.get(0)).intValue() != PARAMATOM)
				{
					System.out.println("Block 'name' attribute should take a single atomic parameter (line " + scanlt.lineno + ")");
					return null;
				}
				blockname = (String)scanlt.paramvalue.get(0);
				continue;
			}
			if (scanlt.keyword.equalsIgnoreCase("size") && scanlt.paramtype.size() == 2 &&
				((Integer)scanlt.paramtype.get(0)).intValue() == PARAMATOM &&
				((Integer)scanlt.paramtype.get(1)).intValue() == PARAMATOM)
			{
				gotsize = true;
				sizex = TextUtils.atof((String)scanlt.paramvalue.get(0));
				sizey = TextUtils.atof((String)scanlt.paramvalue.get(1));
				continue;
			}
		}
	
		// validate
		if (blockname == null)
		{
			System.out.println("Missing 'name' attribute in block definition (line " + ltattribute.lineno + ")");
			return null;
		}
	
		// make the cell
		Cell cell = Cell.newInstance(Library.getCurrent(), blockname);
		if (cell == null) return null;
		System.out.println("Creating cell '" + blockname + "'");
	
		// force size by placing pins in the corners
		if (gotsize)
		{
			NodeInst.makeInstance(tech.wirePinNode, new Point2D.Double(0.5, 0.5), 1, 1, cell);
			NodeInst.makeInstance(tech.wirePinNode, new Point2D.Double(sizex-0.5, 0.5), 1, 1, cell);
			NodeInst.makeInstance(tech.wirePinNode, new Point2D.Double(0.5, sizey-0.5), 1, 1, cell);
			NodeInst.makeInstance(tech.wirePinNode, new Point2D.Double(sizex-0.5, sizey-0.5), 1, 1, cell);
		}
	
		// add any unrecognized attributes
		for(int j=0; j<ltattribute.paramtype.size(); j++)
		{
			if (((Integer)ltattribute.paramtype.get(j)).intValue() != PARAMBRANCH) continue;
			LispTree scanlt = (LispTree)ltattribute.paramvalue.get(j);
			if (scanlt.keyword.equalsIgnoreCase("name")) continue;
			if (scanlt.keyword.equalsIgnoreCase("size")) continue;
	
			if (scanlt.paramtype.size() != 1 || ((Integer)scanlt.paramtype.get(0)).intValue() != PARAMATOM)
			{
				System.out.println("Attribute '" + scanlt.keyword + "' attribute should take a single atomic parameter (line " +
					scanlt.lineno + ")");
				return null;
			}
			cell.newVar(scanlt.keyword, (String)scanlt.paramvalue.get(0));
		}
	
		// place block components
		if (ltcomponents != null)
		{
			for(int j=0; j<ltcomponents.paramtype.size(); j++)
			{
				if (((Integer)ltcomponents.paramtype.get(j)).intValue() != PARAMBRANCH) continue;
				LispTree scanlt = (LispTree)ltcomponents.paramvalue.get(j);
				if (scanlt.keyword.equalsIgnoreCase("repeater"))
				{
					if (fpga_makeblockrepeater(cell, scanlt)) return null;
					continue;
				}
				if (scanlt.keyword.equalsIgnoreCase("instance"))
				{
					if (fpga_makeblockinstance(cell, scanlt)) return null;
					continue;
				}
			}
		}
	
		// place block ports
		if (ltports != null)
		{
			for(int j=0; j<ltports.paramtype.size(); j++)
			{
				if (((Integer)ltports.paramtype.get(j)).intValue() != PARAMBRANCH) continue;
				LispTree scanlt = (LispTree)ltports.paramvalue.get(j);
				if (scanlt.keyword.equalsIgnoreCase("port"))
				{
					if (fpga_makeblockport(cell, scanlt)) return null;
				}
			}
		}
	
		// place block nets
		if (ltnets != null)
		{
			// read the block nets
			for(int j=0; j<ltnets.paramtype.size(); j++)
			{
				if (((Integer)ltnets.paramtype.get(j)).intValue() != PARAMBRANCH) continue;
				LispTree scanlt = (LispTree)ltnets.paramvalue.get(j);
				if (scanlt.keyword.equalsIgnoreCase("net"))
				{
					if (fpga_makeblocknet(cell, scanlt)) return null;
				}
			}
		}
		return cell;
	}
	
	/**
	 * Method to place an instance in cell "cell" from the LISPTREE in "lt".
	 * Tree has "(instance...)" structure.  Returns true on error.
	 */
	private static boolean fpga_makeblockinstance(Cell cell, LispTree lt)
	{
		// scan for information in this block instance object
		LispTree lttype = null, ltname = null, ltposition = null, ltrotation = null, ltattribute = null;
		for(int i=0; i<lt.paramtype.size(); i++)
		{
			if (((Integer)lt.paramtype.get(i)).intValue() != PARAMBRANCH) continue;
			LispTree scanlt = (LispTree)lt.paramvalue.get(i);
			if (scanlt.keyword.equalsIgnoreCase("type"))
			{
				if (lttype != null)
				{
					System.out.println("Multiple 'type' sections for a block (line " + lt.lineno + ")");
					return true;
				}
				lttype = scanlt;
				continue;
			}
			if (scanlt.keyword.equalsIgnoreCase("name"))
			{
				if (ltname != null)
				{
					System.out.println("Multiple 'name' sections for a block (line " + lt.lineno + ")");
					return true;
				}
				ltname = scanlt;
				continue;
			}
			if (scanlt.keyword.equalsIgnoreCase("position"))
			{
				if (ltposition != null)
				{
					System.out.println("Multiple 'position' sections for a block (line " + lt.lineno + ")");
					return true;
				}
				ltposition = scanlt;
				continue;
			}
			if (scanlt.keyword.equalsIgnoreCase("rotation"))
			{
				if (ltrotation != null)
				{
					System.out.println("Multiple 'rotation' sections for a block (line " + lt.lineno + ")");
					return true;
				}
				ltrotation = scanlt;
				continue;
			}
			if (scanlt.keyword.equalsIgnoreCase("attributes"))
			{
				if (ltattribute != null)
				{
					System.out.println("Multiple 'attributes' sections for a block (line " + lt.lineno + ")");
					return true;
				}
				ltattribute = scanlt;
				continue;
			}
		}
	
		// validate
		if (lttype == null)
		{
			System.out.println("No 'type' specified for block instance (line " + lt.lineno + ")");
			return true;
		}
		if (lttype.paramtype.size() != 1 || ((Integer)lttype.paramtype.get(0)).intValue() != PARAMATOM)
		{
			System.out.println("Need one atom in 'type' of block instance (line " + lttype.lineno + ")");
			return true;
		}
		NodeProto np = tech.findNodeProto((String)lttype.paramvalue.get(0));
		if (np == null) np = cell.getLibrary().findNodeProto((String)lttype.paramvalue.get(0));
		if (np == null)
		{
			System.out.println("Cannot find block type '" + (String)lttype.paramvalue.get(0) + "' (line " + lttype.lineno + ")");
			return true;
		}
		if (ltposition == null)
		{
			System.out.println("No 'position' specified for block instance (line " + lt.lineno + ")");
			return true;
		}
		if (ltposition.paramtype.size() != 2 || ((Integer)ltposition.paramtype.get(0)).intValue() != PARAMATOM ||
			((Integer)ltposition.paramtype.get(1)).intValue() != PARAMATOM)
		{
			System.out.println("Need two atoms in 'position' of block instance (line " + ltposition.lineno + ")");
			return true;
		}
		int rotation = 0;
		if (ltrotation != null)
		{
			if (ltrotation.paramtype.size() != 1 || ((Integer)ltrotation.paramtype.get(0)).intValue() != PARAMATOM)
			{
				System.out.println("Need one atom in 'rotation' of block instance (line " + ltrotation.lineno + ")");
				return true;
			}
			rotation = TextUtils.atoi((String)ltrotation.paramvalue.get(0)) * 10;
		}
		
		// name the instance if one is given
		String nodeName = null;
		if (ltname != null)
		{
			if (ltname.paramtype.size() != 1 || ((Integer)ltname.paramtype.get(0)).intValue() != PARAMATOM)
			{
				System.out.println("Need one atom in 'name' of block instance (line " + ltname.lineno + ")");
				return true;
			}
			nodeName = (String)ltname.paramvalue.get(0);
		}

		// place the instance
		double posx = TextUtils.atof((String)ltposition.paramvalue.get(0));
		double posy = TextUtils.atof((String)ltposition.paramvalue.get(1));
		double wid = np.getDefWidth();
		double hei = np.getDefHeight();
		Point2D ctr = new Point2D.Double(posx + wid/2, posy + hei/2);
		NodeInst ni = NodeInst.makeInstance(np, ctr, wid, hei, cell, rotation, nodeName, 0);
		if (ni == null) return true;
	
		// add any attributes
		if (ltattribute != null)
		{
			for(int i=0; i<ltattribute.paramtype.size(); i++)
			{
				if (((Integer)ltattribute.paramtype.get(i)).intValue() != PARAMBRANCH) continue;
				LispTree scanlt = (LispTree)ltattribute.paramvalue.get(i);
				if (scanlt.paramtype.size() != 1 || ((Integer)scanlt.paramtype.get(0)).intValue() != PARAMATOM)
				{
					System.out.println("Attribute '" + scanlt.keyword+ "' attribute should take a single atomic parameter (line " + lt.lineno + ")");
					return true;
				}
				ni.newVar(scanlt.keyword, (String)scanlt.paramvalue.get(0));
			}
		}
		return false;
	}
	
	/**
	 * Method to add a port to block "cell" from the tree in "lt".
	 * Tree has "(port...)" structure.  Returns true on error.
	 */
	private static boolean fpga_makeblockport(Cell cell, LispTree lt)
	{
		LispTree ltname = null, ltposition = null;
		for(int j=0; j<lt.paramtype.size(); j++)
		{
			if (((Integer)lt.paramtype.get(j)).intValue() != PARAMBRANCH) continue;
			LispTree scanlt = (LispTree)lt.paramvalue.get(j);
			if (scanlt.keyword.equalsIgnoreCase("name"))
			{
				ltname = scanlt;
				continue;
			}
			if (scanlt.keyword.equalsIgnoreCase("position"))
			{
				ltposition = scanlt;
				continue;
			}
			if (scanlt.keyword.equalsIgnoreCase("direction"))
			{
				// ltdirection = scanlt;
				continue;
			}
		}
	
		// make the port
		if (ltname == null)
		{
			System.out.println("Port has no name (line " + lt.lineno + ")");
			return true;
		}
		if (ltname.paramtype.size() != 1 || ((Integer)ltname.paramtype.get(0)).intValue() != PARAMATOM)
		{
			System.out.println("Port name must be a single atom (line " + ltname.lineno + ")");
		}
		if (ltposition == null)
		{
			System.out.println("Port has no position (line " + lt.lineno + ")");
			return true;
		}
		if (ltposition.paramtype.size() != 2 || ((Integer)ltposition.paramtype.get(0)).intValue() != PARAMATOM ||
				((Integer)ltposition.paramtype.get(1)).intValue() != PARAMATOM)
		{
			System.out.println("Port position must be two atoms (line " + ltposition.lineno + ")");
		}
	
		// create the structure
		double posx = TextUtils.atof((String)ltposition.paramvalue.get(0));
		double posy = TextUtils.atof((String)ltposition.paramvalue.get(1));
		NodeInst ni = NodeInst.makeInstance(tech.wirePinNode, new Point2D.Double(posx, posy), 0, 0, cell);
		if (ni == null)
		{
			System.out.println("Error creating pin for port '" + (String)ltname.paramvalue.get(0) + "' (line " + lt.lineno + ")");
			return true;
		}
		PortInst pi = ni.getOnlyPortInst();
		Export expp = Export.newInstance(cell, pi, (String)ltname.paramvalue.get(0));
		if (expp == null)
		{
			System.out.println("Error creating port '" + (String)ltname.paramvalue.get(0) + "' (line " + lt.lineno + ")");
			return true;
		}
		return false;
	}
	
	/**
	 * Method to place a repeater in cell "cell" from the LISPTREE in "lt".
	 * Tree has "(repeater...)" structure.  Returns true on error.
	 */
	private static boolean fpga_makeblockrepeater(Cell cell, LispTree lt)
	{
		LispTree ltname = null, ltporta = null, ltportb = null;
		for(int j=0; j<lt.paramtype.size(); j++)
		{
			if (((Integer)lt.paramtype.get(j)).intValue() != PARAMBRANCH) continue;
			LispTree scanlt = (LispTree)lt.paramvalue.get(j);
			if (scanlt.keyword.equalsIgnoreCase("name"))
			{
				ltname = scanlt;
				continue;
			}
			if (scanlt.keyword.equalsIgnoreCase("porta"))
			{
				ltporta = scanlt;
				continue;
			}
			if (scanlt.keyword.equalsIgnoreCase("portb"))
			{
				ltportb = scanlt;
				continue;
			}
			if (scanlt.keyword.equalsIgnoreCase("direction"))
			{
				// ltdirection = scanlt;
				continue;
			}
		}
	
		// make the repeater
		if (ltporta == null)
		{
			System.out.println("Repeater has no 'porta' (line " + lt.lineno + ")");
			return true;
		}
		if (ltporta.paramtype.size() != 2 || ((Integer)ltporta.paramtype.get(0)).intValue() != PARAMATOM ||
				((Integer)ltporta.paramtype.get(1)).intValue() != PARAMATOM)
		{
			System.out.println("Repeater 'porta' position must be two atoms (line " + ltporta.lineno + ")");
		}
		if (ltportb == null)
		{
			System.out.println("Repeater has no 'portb' (line " + lt.lineno + ")");
			return true;
		}
		if (ltportb.paramtype.size() != 2 || ((Integer)ltportb.paramtype.get(0)).intValue() != PARAMATOM ||
				((Integer)ltportb.paramtype.get(1)).intValue() != PARAMATOM)
		{
			System.out.println("Repeater 'portb' position must be two atoms (line " + ltportb.lineno + ")");
		}
		
		// name the repeater if one is given
		String repeaterName = null;
		if (ltname != null)
		{
			if (ltname.paramtype.size() != 1 || ((Integer)ltname.paramtype.get(0)).intValue() != PARAMATOM)
			{
				System.out.println("Need one atom in 'name' of block repeater (line " + ltname.lineno + ")");
				return true;
			}
			repeaterName = (String)ltname.paramvalue.get(0);
		}

		// create the repeater
		double portax = TextUtils.atof((String)ltporta.paramvalue.get(0));
		double portay = TextUtils.atof((String)ltporta.paramvalue.get(1));
		double portbx = TextUtils.atof((String)ltportb.paramvalue.get(0));
		double portby = TextUtils.atof((String)ltportb.paramvalue.get(1));
		int angle = GenMath.figureAngle(new Point2D.Double(portax, portay), new Point2D.Double(portbx, portby));
		Point2D ctr = new Point2D.Double((portax + portbx) / 2, (portay + portby) / 2);
		NodeInst ni = NodeInst.makeInstance(tech.repeaterNode, ctr, 10,3, cell, angle, repeaterName, 0);
		if (ni == null)
		{
			System.out.println("Error creating repeater (line " + lt.lineno + ")");
			return true;
		}
		return false;
	}
	
	/**
	 * Method to extract block net information from the LISPTREE in "lt".
	 * Tree has "(net...)" structure.  Returns true on error.
	 */
	private static boolean fpga_makeblocknet(Cell cell, LispTree lt)
	{
		// find the net name
		for(int j=0; j<lt.paramtype.size(); j++)
		{
			if (((Integer)lt.paramtype.get(j)).intValue() != PARAMBRANCH) continue;
			LispTree scanlt = (LispTree)lt.paramvalue.get(j);
			if (scanlt.keyword.equalsIgnoreCase("name"))
			{
				if (scanlt.paramtype.size() != 1 || ((Integer)scanlt.paramtype.get(0)).intValue() != PARAMATOM)
				{
					System.out.println("Net name must be a single atom (line " + scanlt.lineno + ")");
					return true;
				}
				// ltname = scanlt;
				continue;
			}
		}
	
		// scan for segment objects
		for(int j=0; j<lt.paramtype.size(); j++)
		{
			if (((Integer)lt.paramtype.get(j)).intValue() != PARAMBRANCH) continue;
			LispTree scanlt = (LispTree)lt.paramvalue.get(j);
			if (scanlt.keyword.equalsIgnoreCase("segment"))
			{
				int pos = 0;
				NodeInst [] nis = new NodeInst[2];
				PortProto [] pps = new PortProto[2];
				for(int i=0; i<2; i++)
				{
					// get end of arc
					if (scanlt.paramtype.size() < pos+1)
					{
						System.out.println("Incomplete block net segment (line " + scanlt.lineno + ")");
						return true;
					}
					if (((Integer)scanlt.paramtype.get(pos)).intValue() != PARAMATOM)
					{
						System.out.println("Must have atoms in block net segment (line " + scanlt.lineno + ")");
						return true;
					}
					if (((String)scanlt.paramvalue.get(pos)).equalsIgnoreCase("component"))
					{
						if (scanlt.paramtype.size() < pos+3)
						{
							System.out.println("Incomplete block net segment (line " + scanlt.lineno + ")");
							return true;
						}
						if (((Integer)scanlt.paramtype.get(pos+1)).intValue() != PARAMATOM ||
								((Integer)scanlt.paramtype.get(pos+2)).intValue() != PARAMATOM)
						{
							System.out.println("Must have atoms in block net segment (line " + scanlt.lineno + ")");
							return true;
						}
	
						// find component and port
						NodeInst niFound = null;
						String name = (String)scanlt.paramvalue.get(pos+1);
						for(Iterator it = cell.getNodes(); it.hasNext(); )
						{
							NodeInst ni = (NodeInst)it.next();
							if (ni.getName().equalsIgnoreCase(name))
							{
								niFound = ni;
								break;
							}
						}
						if (niFound == null)
						{
							System.out.println("Cannot find component '" + (String)scanlt.paramvalue.get(pos+1) +
								"' in block net segment (line " + scanlt.lineno + ")");
							return true;
						}
						nis[i] = niFound;
						pps[i] = niFound.getProto().findPortProto((String)scanlt.paramvalue.get(pos+2));
						if (pps[i] == null)
						{
							System.out.println("Cannot find port '" + (String)scanlt.paramvalue.get(pos+2) +
								"' on component '" + (String)scanlt.paramvalue.get(pos+1) +
								"' in block net segment (line " + scanlt.lineno + ")");
							return true;
						}
						pos += 3;
					} else if (((String)scanlt.paramvalue.get(pos)).equalsIgnoreCase("coord"))
					{
						if (scanlt.paramtype.size() < pos+3)
						{
							System.out.println("Incomplete block net segment (line " + scanlt.lineno + ")");
							return true;
						}
						if (((Integer)scanlt.paramtype.get(pos+1)).intValue() != PARAMATOM ||
								((Integer)scanlt.paramtype.get(pos+2)).intValue() != PARAMATOM)
						{
							System.out.println("Must have atoms in block net segment (line " + scanlt.lineno + ")");
							return true;
						}
						double x = TextUtils.atof((String)scanlt.paramvalue.get(pos+1));
						double y = TextUtils.atof((String)scanlt.paramvalue.get(pos+2));
						Rectangle2D search = new Rectangle2D.Double(x, y, 0, 0);
	
						// find pin at this point
						NodeInst niFound = null;
						for(Iterator it = cell.searchIterator(search); it.hasNext(); )
						{
							Geometric geom = (Geometric)it.next();
							if (!(geom instanceof NodeInst)) continue;
							NodeInst ni = (NodeInst)geom;
							if (ni.getProto() != tech.wirePinNode) continue;
							if (ni.getTrueCenterX() == x && ni.getTrueCenterY() == y)
							{
								niFound = ni;
								break;
							}
						}
						if (niFound == null)
						{
							niFound = NodeInst.makeInstance(tech.wirePinNode, new Point2D.Double(x, y), 0, 0, cell);
							if (niFound == null)
							{
								System.out.println("Cannot create pin for block net segment (line " + scanlt.lineno + ")");
								return true;
							}
						}
						nis[i] = niFound;
						pps[i] = niFound.getProto().getPort(0);
						pos += 3;
					} else if (((String)scanlt.paramvalue.get(pos)).equalsIgnoreCase("port"))
					{
						if (scanlt.paramtype.size() < pos+2)
						{
							System.out.println("Incomplete block net segment (line " + scanlt.lineno + ")");
							return true;
						}
						if (((Integer)scanlt.paramtype.get(pos+1)).intValue() != PARAMATOM)
						{
							System.out.println("Must have atoms in block net segment (line " + scanlt.lineno + ")");
							return true;
						}
	
						// find port
						Export pp = cell.findExport((String)scanlt.paramvalue.get(pos+1));
						if (pp == null)
						{
							System.out.println("Cannot find port '" + (String)scanlt.paramvalue.get(pos+1) +
									"' in block net segment (line " + scanlt.lineno + ")");
							return true;
						}
						pps[i] = pp.getOriginalPort().getPortProto();
						nis[i] = pp.getOriginalPort().getNodeInst();
						pos += 2;
					} else
					{
						System.out.println("Unknown keyword '" + (String)scanlt.paramvalue.get(pos) +
								"' in block net segment (line " + scanlt.lineno + ")");
						return true;
					}
				}
	
				// now create the arc
				PortInst pi0 = nis[0].findPortInstFromProto(pps[0]);
				PortInst pi1 = nis[1].findPortInstFromProto(pps[1]);
				ArcInst ai = ArcInst.makeInstance(tech.wire_arc, 0, pi0, pi1);
				if (ai == null)
				{
					System.out.println("Cannot run segment (line " + scanlt.lineno + ")");
					return true;
				}
			}
		}
		return false;
	}

}
