/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Technology.java
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

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.CMOS;
import com.sun.electric.technology.technologies.MoCMOS;
import com.sun.electric.technology.technologies.MoCMOSOld;
import com.sun.electric.technology.technologies.MoCMOSSub;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Technology is the base class for all of the specific technologies in Electric.
 *
 * It is organized into two main areas: nodes and arcs.
 * Both nodes and arcs are composed of Layers.
 *<P>
 * Subclasses of Technology usually start by defining the Layers (such as Metal-1, Metal-2, etc.)
 * Then the PrimitiveArc objects are created, built entirely from Layers.
 * Next PrimitiveNode objects are created, and they have Layers as well as connectivity to the PrimitiveArcs.
 * The Technology concludes with miscellaneous data assignments of technology-wide information.
 * <P>
 * Here are the nodes in a sample CMOS technology.
 * Note that there are two types of transistors and diffusion contacts, one for Well and one for Substrate.
 * Each layer that can exist as a wire must have a pin node (in this case, metal, polysilicon, and two flavors of diffusion.
 * Note that there are pure-layer nodes at the bottom which allow arbitrary geometry to be constructed.
 * <CENTER><IMG SRC="doc-files/Technology-1.gif"></CENTER>
 * <P>
 * The Schematic technology has some unusual features.
 * <CENTER><IMG SRC="doc-files/Technology-2.gif"></CENTER>
 * <P>
 * Conceptually, a Technology has 3 types of information:
 * <UL><LI><I>Geometry</I>.  Each node and arc can be described in terms of polygons on differnt Layers.
 * The ArcLayer and NodeLayer subclasses help define those polygons.
 * <LI><I>Connectivity</I>.  The very structure of the nodes and arcs establisheds a set of rules of connectivity.
 * Examples include the list of allowable arc types that may connect to each port, and the use of port "network numbers"
 * to identify those that are connected internally.
 * <LI><I>Behavior</I>.  Behavioral information takes many forms, but they can all find a place here.
 * For example, each layer, node, and arc has a "function" that describes its general behavior.
 * Some information applies to the technology as a whole, for example SPICE model cards.
 * Other examples include Design Rules and technology characteristics.
 * </UL>
 * @author Steven M. Rubin
 */
public class Technology extends ElectricObject
{
	/** technology is not electrical */									private static final int NONELECTRICAL =       01;
	/** has no directional arcs */										private static final int NODIRECTIONALARCS =   02;
	/** has no negated arcs */											private static final int NONEGATEDARCS =       04;
	/** nonstandard technology (cannot be edited) */					private static final int NONSTANDARD =        010;
	/** statically allocated (don't deallocate memory) */				private static final int STATICTECHNOLOGY =   020;
	/** no primitives in this technology (don't auto-switch to it) */	private static final int NOPRIMTECHNOLOGY =   040;

	/**
	 * Defines a single layer of a PrimitiveArc.
	 * A PrimitiveArc has a list of these ArcLayer objects, one for
	 * each layer in a typical ArcInst.
	 * Each PrimitiveArc is composed of a number of ArcLayer descriptors.
	 * A descriptor converts a specific ArcInst into a polygon that describe this particular layer.
	 */
	public static class ArcLayer
	{
		private Layer layer;
		private int offset;
		private Poly.Type style;

		/**
		 * Constructs an <CODE>ArcLayer</CODE> with the specified description.
		 * @param layer the Layer of this ArcLayer.
		 * @param offset the distance from the outside of the ArcInst to this ArcLayer.
		 * @param style the Poly.Style of this ArcLayer.
		 */
		public ArcLayer(Layer layer, int offset, Poly.Type style)
		{
			this.layer = layer;
			this.offset = offset;
			this.style = style;
		}

		/**
		 * Returns the Layer from the Technology to be used for this ArcLayer.
		 * @return the Layer from the Technology to be used for this ArcLayer.
		 */
		public Layer getLayer() { return layer; }

		/**
		 * Returns the distance from the outside of the ArcInst to this ArcLayer.
		 * This is the difference between the width of this layer and the overall width of the arc.
		 * For example, a value of 4 on an arc that is 6 wide indicates that this layer should be only 2 wide.
		 * @return the distance from the outside of the ArcInst to this ArcLayer.
		 */
		public int getOffset() { return offset; }

		/**
		 * Returns the Poly.Style of this ArcLayer.
		 * @return the Poly.Style of this ArcLayer.
		 */
		public Poly.Type getStyle() { return style; }
	}

	/**
	 * Defines a point in space that is relative to a NodeInst's bounds.
	 * The TechPoint has two coordinates: X and Y.
	 * Each of these coordinates is represented by an Edge class (EdgeH for X
	 * and EdgeV for Y).
	 * The Edge classes have two numbers: a multiplier and an adder.
	 * The desired coordinate takes the NodeInst's center, adds in the
	 * product of the Edge multiplier and the NodeInst's size, and then adds
	 * in the Edge adder.
	 * <P>
	 * Arrays of TechPoint objects can be used to describe the bounds of
	 * a particular layer in a NodeInst.  Typically, four TechPoint objects
	 * can describe a rectangle.  Circles only need two (center and edge).
	 * The <CODE>Poly.Style</CODE> class defines the possible types of
	 * geometry.
	 * @see EdgeH
	 * @see EdgeV
	 */
	public static class TechPoint
	{
		private EdgeH x;
		private EdgeV y;

		/** An array of TechPoints that describes a zero-size rectangle in the center of the NodeInst. */
		public static final TechPoint [] ATCENTER = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER)};
		/** An array of TechPoints that describes a rectangle that completely covers the NodeInst. */
		public static final TechPoint [] FULLBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE),
					new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)};
		/** An array of TechPoints that describes a rectangle that is inset by 0.5 units from the edge of the NodeInst. */
		public static final TechPoint [] IN0HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(0.5), EdgeV.fromBottom(0.5)),
					new Technology.TechPoint(EdgeH.fromRight(0.5), EdgeV.fromTop(0.5))};
		/** An array of TechPoints that describes a rectangle that is inset by 1 unit from the edge of the NodeInst. */
		public static final TechPoint [] IN1BOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
					new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(1))};
		/** An array of TechPoints that describes a rectangle that is inset by 1.5 units from the edge of the NodeInst. */
		public static final TechPoint [] IN1HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5)),
					new Technology.TechPoint(EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))};
		/** An array of TechPoints that describes a rectangle that is inset by 2 units from the edge of the NodeInst. */
		public static final TechPoint [] IN2BOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromBottom(2)),
					new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.fromTop(2))};
		/** An array of TechPoints that describes a rectangle that is inset by 2.5 units from the edge of the NodeInst. */
		public static final TechPoint [] IN2HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5)),
					new Technology.TechPoint(EdgeH.fromRight(2.5), EdgeV.fromTop(2.5))};
		/** An array of TechPoints that describes a rectangle that is inset by 3 units from the edge of the NodeInst. */
		public static final TechPoint [] IN3BOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(3)),
					new Technology.TechPoint(EdgeH.fromRight(3), EdgeV.fromTop(3))};
		/** An array of TechPoints that describes a rectangle that is inset by 3.5 units from the edge of the NodeInst. */
		public static final TechPoint [] IN3HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(3.5), EdgeV.fromBottom(3.5)),
					new Technology.TechPoint(EdgeH.fromRight(3.5), EdgeV.fromTop(3.5))};
		/** An array of TechPoints that describes a rectangle that is inset by 4 units from the edge of the NodeInst. */
		public static final TechPoint [] IN4BOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(4)),
					new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(4))};
		/** An array of TechPoints that describes a rectangle that is inset by 4.5 units from the edge of the NodeInst. */
		public static final TechPoint [] IN4HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(4.5), EdgeV.fromBottom(4.5)),
					new Technology.TechPoint(EdgeH.fromRight(4.5), EdgeV.fromTop(4.5))};
		/** An array of TechPoints that describes a rectangle that is inset by 5 units from the edge of the NodeInst. */
		public static final TechPoint [] IN5BOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(5), EdgeV.fromBottom(5)),
					new Technology.TechPoint(EdgeH.fromRight(5), EdgeV.fromTop(5))};
		/** An array of TechPoints that describes a rectangle that is inset by 5.5 units from the edge of the NodeInst. */
		public static final TechPoint [] IN5HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(5.5), EdgeV.fromBottom(5.5)),
					new Technology.TechPoint(EdgeH.fromRight(5.5), EdgeV.fromTop(5.5))};
		/** An array of TechPoints that describes a rectangle that is inset by 6 units from the edge of the NodeInst. */
		public static final TechPoint [] IN6BOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(6)),
					new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(6))};
		/** An array of TechPoints that describes a rectangle that is inset by 6.5 units from the edge of the NodeInst. */
		public static final TechPoint [] IN6HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6.5), EdgeV.fromBottom(6.5)),
					new Technology.TechPoint(EdgeH.fromRight(6.5), EdgeV.fromTop(6.5))};
		/** An array of TechPoints that describes a rectangle that is inset by 7 units from the edge of the NodeInst. */
		public static final TechPoint [] IN7BOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7), EdgeV.fromBottom(7)),
					new Technology.TechPoint(EdgeH.fromRight(7), EdgeV.fromTop(7))};
		/** An array of TechPoints that describes a rectangle that is inset by 7.5 units from the edge of the NodeInst. */
		public static final TechPoint [] IN7HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromBottom(7.5)),
					new Technology.TechPoint(EdgeH.fromRight(7.5), EdgeV.fromTop(7.5))};

		/**
		 * Constructs a <CODE>TechPoint</CODE> with the specified description.
		 * @param x the EdgeH that converts a NodeInst into an X coordinate on that NodeInst.
		 * @param y the EdgeV that converts a NodeInst into a Y coordinate on that NodeInst.
		 */
		public TechPoint(EdgeH x, EdgeV y)
		{
			this.x = x;
			this.y = y;
		}

		/**
		 * Returns the EdgeH that converts a NodeInst into an X coordinate on that NodeInst.
		 * @return the EdgeH that converts a NodeInst into an X coordinate on that NodeInst.
		 */
		public EdgeH getX() { return x; }

		/**
		 * Returns the EdgeV that converts a NodeInst into a Y coordinate on that NodeInst.
		 * @return the EdgeV that converts a NodeInst into a Y coordinate on that NodeInst.
		 */
		public EdgeV getY() { return y; }
	}

	/**
	 * Defines a single layer of a PrimitiveNode.
	 * A PrimitiveNode has a list of these NodeLayer objects, one for
	 * each layer in a typical NodeInst.
	 * Each PrimitiveNode is composed of a number of NodeLayer descriptors.
	 * A descriptor converts a specific NodeInst into a polygon that describe this particular layer.
	 */
	public static class NodeLayer
	{
		private Layer layer;
		private int portNum;
		private Poly.Type style;
		private int representation;
		private TechPoint [] points;
		private String message;
		private double lWidth, rWidth, extentT, extendB;

		// the meaning of "representation"
		/**
		 * Indicates that the "points" list defines scalable points.
		 * Each point here becomes a point on the Poly.
		 */
		public static final int POINTS = 0;

		/**
		 * Indicates that the "points" list defines a rectangle.
		 * It contains two diagonally opposite points.
		 */
		public static final int BOX = 1;

		/**
		 * Indicates that the "points" list defines a minimum sized rectangle.
		 * It contains two diagonally opposite points, like BOX,
		 * and also contains a minimum box size beyond which the polygon will not shrink
		 * (again, two diagonally opposite points).
		 */
		public static final int MINBOX = 2;

		/**
		 * Constructs a <CODE>NodeLayer</CODE> with the specified description.
		 * @param layer the <CODE>Layer</CODE> this is on.
		 * @param portNum a 0-based index of the port (from the actual NodeInst) on this layer.
		 * A negative value indicates that this layer is not connected to an electrical layer.
		 * @param style the Poly.Type this NodeLayer will generate (polygon, circle, text, etc.).
		 * @param representation tells how to interpret "points".  It can be POINTS, BOX, or MINBOX.
		 * @param points the list of coordinates (stored as TechPoints) associated with this NodeLayer.
		 */
		public NodeLayer(Layer layer, int portNum, Poly.Type style, int representation, TechPoint [] points)
		{
			this.layer = layer;
			this.portNum = portNum;
			this.style = style;
			this.representation = representation;
			this.points = points;
			this.lWidth = this.rWidth = this.extentT = this.extendB = 0;
		}

		/**
		 * Constructs a <CODE>NodeLayer</CODE> with the specified description.
		 * @param layer the <CODE>Layer</CODE> this is on.
		 * @param portNum a 0-based index of the port (from the actual NodeInst) on this layer.
		 * A negative value indicates that this layer is not connected to an electrical layer.
		 * @param style the Poly.Type this NodeLayer will generate (polygon, circle, text, etc.).
		 * @param representation tells how to interpret "points".  It can be POINTS, BOX, or MINBOX.
		 * @param points the list of coordinates (stored as TechPoints) associated with this NodeLayer.
		 * @param lWidth the extension to the left of this layer (serpentine transistors only).
		 * @param rWidth the extension to the right of this layer (serpentine transistors only).
		 * @param extentT the extension to the top of this layer (serpentine transistors only).
		 * @param extendB the extension to the bottom of this layer (serpentine transistors only).
		 */
		public NodeLayer(Layer layer, int portNum, Poly.Type style, int representation, TechPoint [] points,
			double lWidth, double rWidth, double extentT, double extendB)
		{
			this.layer = layer;
			this.portNum = portNum;
			this.style = style;
			this.representation = representation;
			this.points = points;
			this.lWidth = lWidth;
			this.rWidth = rWidth;
			this.extentT = extentT;
			this.extendB = extendB;
		}

		/**
		 * Returns the <CODE>Layer</CODE> object associated with this NodeLayer.
		 * @return the <CODE>Layer</CODE> object associated with this NodeLayer.
		 */
		public Layer getLayer() { return layer; }

		/**
		 * Returns the 0-based index of the port associated with this NodeLayer.
		 * @return the 0-based index of the port associated with this NodeLayer.
		 */
		public int getPortNum() { return portNum; }

		/**
		 * Returns the Poly.Type this NodeLayer will generate.
		 * @return the Poly.Type this NodeLayer will generate.
		 * Examples are polygon, lines, splines, circle, text, etc.
		 */
		public Poly.Type getStyle() { return style; }

		/**
		 * Returns the method of interpreting "points".
		 * @return the method of interpreting "points".
		 * It can be POINTS, BOX, ABSPOINTS, or MINBOX.
		 */
		public int getRepresentation() { return representation; }

		/**
		 * Returns the list of coordinates (stored as TechPoints) associated with this NodeLayer.
		 * @return the list of coordinates (stored as TechPoints) associated with this NodeLayer.
		 */
		public TechPoint [] getPoints() { return points; }

		/**
		 * Returns the left edge coordinate (a scalable EdgeH object) associated with this NodeLayer.
		 * @return the left edge coordinate associated with this NodeLayer.
		 * It only makes sense if the representation is BOX or MINBOX.
		 * The returned coordinate is a scalable EdgeH object.
		 */
		public EdgeH getLeftEdge() { return points[0].getX(); }

		/**
		 * Returns the bottom edge coordinate (a scalable EdgeV object) associated with this NodeLayer.
		 * @return the bottom edge coordinate associated with this NodeLayer.
		 * It only makes sense if the representation is BOX or MINBOX.
		 * The returned coordinate is a scalable EdgeV object.
		 */
		public EdgeV getBOTTOMEDGE() { return points[0].getY(); }

		/**
		 * Returns the right edge coordinate (a scalable EdgeH object) associated with this NodeLayer.
		 * @return the right edge coordinate associated with this NodeLayer.
		 * It only makes sense if the representation is BOX or MINBOX.
		 * The returned coordinate is a scalable EdgeH object.
		 */
		public EdgeH getRIGHTEDGE() { return points[1].getX(); }

		/**
		 * Returns the top edge coordinate (a scalable EdgeV object) associated with this NodeLayer.
		 * @return the top edge coordinate associated with this NodeLayer.
		 * It only makes sense if the representation is BOX or MINBOX.
		 * The returned coordinate is a scalable EdgeV object.
		 */
		public EdgeV getTOPEDGE() { return points[1].getY(); }

		/**
		 * Returns the text message associated with this list NodeLayer.
		 * @return the text message associated with this list NodeLayer.
		 * This only makes sense if the style is one of the TEXT types.
		 */
		public String getMessage() { return message; }

		/**
		 * Sets the text to be drawn by this NodeLayer.
		 * @param message the text to be drawn by this NodeLayer.
		 * This only makes sense if the style is one of the TEXT types.
		 */
		public void setMessage(String message) { this.message = message; }

		/**
		 * Returns the left extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @return the left extension of this layer.
		 */
		public double getSerpentineLWidth() { return lWidth; }

		/**
		 * Returns the right extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @return the right extension of this layer.
		 */
		public double getSerpentineRWidth() { return rWidth; }

		/**
		 * Returns the top extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @return the top extension of this layer.
		 */
		public double getSerpentineExtentT() { return extentT; }

		/**
		 * Returns the bottom extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @return the bottom extension of this layer.
		 */
		public double getSerpentineExtentB() { return extendB; }
	}

	/** name of the technology */						private String techName;
	/** full description of the technology */			private String techDesc;
	/** flags for the technology */						private int userBits;
	/** 0-based index of the technology */				private int techIndex;
	/** critical dimensions for the technology */		private double scale;
	/** list of primitive nodes in the technology */	private List nodes;
	/** list of arcs in the technology */				private List arcs;

	/* static list of all Technologies in Electric */	private static List technologies = new ArrayList();
	/* the current technology in Electric */			private static Technology curTech = null;
	/* the current tlayout echnology in Electric */		private static Technology curLayoutTech = null;
	/* counter for enumerating technologies */			private static int techNumber = 0;

	/**
	 * Constructs a <CODE>Technology</CODE>.
	 * This should not be called directly, but instead is invoked through each subclass's factory.
	 */
	protected Technology()
	{
		this.nodes = new ArrayList();
		this.arcs = new ArrayList();
		this.scale = 1.0;
		this.techIndex = techNumber++;
		userBits = 0;

		// add the technology to the global list
		technologies.add(this);
	}

	/**
	 * Routine to determine whether a new technology with the given name would be legal.
	 * All technology names must be unique, so the name cannot already be in use.
	 * @param techName the name of the new technology that will be created.
	 * @return true if the name is valid.
	 */
	protected static boolean validTechnology(String techName)
	{
		if (Technology.findTechnology(techName) != null)
		{
			System.out.println("ERROR: Multiple technologies named " + techName);
			return false;
		}
		return true;
	}

	/**
	 * Routine to add a new PrimitiveNode to this Technology.
	 * This is usually done during initialization.
	 * @param np the PrimitiveNode to be added to this Technology.
	 */
	void addNodeProto(PrimitiveNode np)
	{
		nodes.add(np);
	}

	/**
	 * Routine to add a new PrimitiveArc to this Technology.
	 * This is usually done during initialization.
	 * @param ap the PrimitiveArc to be added to this Technology.
	 */
	public void addArcProto(PrimitiveArc ap)
	{
		arcs.add(ap);
	}

	/**
	 * This is called once, at the start of Electric, to initialize the technologies.
	 * Because of Java's "lazy evaluation", the only way to force the technology constructors to fire
	 * and build a proper list of technologies, each class must somehow be referenced.
	 * So, each technology is listed here.  If a new technology is created, this must be updated.
	 */
	public static void initAllTechnologies()
	{
		// Because of lazy evaluation, technologies aren't initialized unless they're referenced here
		Generic.tech.setCurrent();		// must be called first

		// now all of the rest
		Schematics.tech.setCurrent();
		Artwork.tech.setCurrent();
		CMOS.tech.setCurrent();
		MoCMOSOld.tech.setCurrent();
		MoCMOSSub.tech.setCurrent();

		// the last one is the real current technology
		MoCMOS.tech.setCurrent();

		// setup the generic technology to handle all connections
		Generic.tech.makeUnivList();
	}

	/**
	 * Sets the technology to be "non-electrical".
	 * Users should never call this routine.
	 * It is set once by the technology during initialization.
	 * Examples of non-electrical technologies are "Artwork" and "Gem".
	 */
	protected void setNonElectrical() { userBits |= NONELECTRICAL; }

	/**
	 * Returns true if this technology is "non-electrical".
	 * @return true if this technology is "non-electrical".
	 * Examples of non-electrical technologies are "Artwork" and "Gem".
	 */
	public boolean isNonElectrical() { return (userBits & NONELECTRICAL) != 0; }

	/**
	 * Sets the technology to have no directional arcs.
	 * Users should never call this routine.
	 * It is set once by the technology during initialization.
	 * Directional arcs are those with arrows on them, indicating (only graphically) the direction of flow through the arc.
	 * @see ArcInst#setDirectional
	 * @see ArcProto#setDirectional
	 */
	protected void setNoDirectionalArcs() { userBits |= NODIRECTIONALARCS; }

	/**
	 * Returns true if this technology does not have directional arcs.
	 * @return true if this technology does not have directional arcs.
	 * Directional arcs are those with arrows on them, indicating (only graphically) the direction of flow through the arc.
	 * @see ArcInst#setDirectional
	 * @see ArcProto#setDirectional
	 */
	public boolean isNoDirectionalArcs() { return (userBits & NODIRECTIONALARCS) != 0; }

	/**
	 * Sets the technology to have no negated arcs.
	 * Users should never call this routine.
	 * It is set once by the technology during initialization.
	 * Negated arcs have bubbles on them to graphically indicated negation.
	 * Only Schematics and related technologies allow negated arcs.
	 * @see ArcInst#setNegated
	 * @see ArcProto#setNegated
	 */
	protected void setNoNegatedArcs() { userBits |= NONEGATEDARCS; }

	/**
	 * Returns true if this technology does not have negated arcs.
	 * @return true if this technology does not have negated arcs.
	 * Negated arcs have bubbles on them to graphically indicated negation.
	 * Only Schematics and related technologies allow negated arcs.
	 * @see ArcInst#setNegated
	 * @see ArcProto#setNegated
	 */
	public boolean isNoNegatedArcs() { return (userBits & NONEGATEDARCS) != 0; }

	/**
	 * Sets the technology to be non-standard.
	 * Users should never call this routine.
	 * It is set once by the technology during initialization.
	 * A non-standard technology cannot be edited in the technology editor.
	 * Examples are Schematics and Artwork, which have more complex graphics.
	 */
	protected void setNonStandard() { userBits |= NONSTANDARD; }

	/**
	 * Returns true if this technology is non-standard.
	 * @return true if this technology is non-standard.
	 * A non-standard technology cannot be edited in the technology editor.
	 * Examples are Schematics and Artwork, which have more complex graphics.
	 */
	public boolean isNonStandard() { return (userBits & NONSTANDARD) != 0; }

	/**
	 * Sets the technology to be "static".
	 * Users should never call this routine.
	 * It is set once by the technology during initialization.
	 * Static technologies are the core set of technologies in Electric that are
	 * essential, and cannot be deleted.
	 * The technology-editor can create others later, and they can be deleted.
	 */
	protected void setStaticTechnology() { userBits |= NONSTANDARD; }

	/**
	 * Returns true if this technoology is "static" (cannot be deleted).
	 * @return true if this technoology is "static" (cannot be deleted).
	 * Static technologies are the core set of technologies in Electric that are
	 * essential, and cannot be deleted.
	 * The technology-editor can create others later, and they can be deleted.
	 */
	public boolean isStaticTechnology() { return (userBits & NONSTANDARD) != 0; }

	/**
	 * Sets the technology to have no primitives.
	 * Users should never call this routine.
	 * It is set once by the technology during initialization.
	 * This indicates to the user interface that it should not switch to this technology.
	 * The FPGA technology has this bit set because it initially contains no primitives,
	 * and they are only created dynamically.
	 */
	public void setNoPrimitiveNodes() { userBits |= NOPRIMTECHNOLOGY; }

	/**
	 * Returns true if this technology has no primitives.
	 * @return true if this technology has no primitives.
	 * This indicates to the user interface that it should not switch to this technology.
	 * The FPGA technology has this bit set because it initially contains no primitives,
	 * and they are only created dynamically.
	 */
	public boolean isNoPrimitiveNodes() { return (userBits & NOPRIMTECHNOLOGY) != 0; }

	/**
	 * Returns the current Technology.
	 * @return the current Technology.
	 * The current technology is maintained by the system as a default
	 * in situations where a technology cannot be determined.
	 */
	public static Technology getCurrent() { return curTech; }

	/**
	 * Set this to be the current Technology
	 * The current technology is maintained by the system as a default
	 * in situations where a technology cannot be determined.
	 */
	public void setCurrent()
	{
		curTech = this;
		if (this != Generic.tech && this != Schematics.tech && this != Artwork.tech)
			curLayoutTech = this;
	}

	/**
	 * Returns the name of this technology.
	 * Each technology has a unique name, such as "mocmos" (MOSIS CMOS).
	 * @return the name of this technology.
	 * @see Technology#setTechName
	 */
	public String getTechName() { return techName; }

	/**
	 * Sets the name of this technology.
	 * Technology names must be unique.
	 */
	protected void setTechName(String techName) { this.techName = techName; }

	/**
	 * Returns the full description of this Technology.
	 * Full descriptions go beyond the one-word technology name by including such
	 * information as foundry, nuumber of available layers, and process specifics.
	 * For example, "Complementary MOS (from MOSIS, Submicron, 2-6 metals [4], double poly)".
	 * @return the full description of this Technology.
	 */
	public String getTechDesc() { return techDesc; }

	/**
	 * Sets the full description of this Technology.
	 * Full descriptions go beyond the one-word technology name by including such
	 * information as foundry, nuumber of available layers, and process specifics.
	 * For example, "Complementary MOS (from MOSIS, Submicron, 2-6 metals [4], double poly)".
	 */
	public void setTechDesc(String techDesc) { this.techDesc = techDesc; }

	/**
	 * Returns the default scale for this Technology.
	 * The technology's scale is for manufacturing output, which must convert
	 * the unit-based values in Electric to real-world values.
	 * @return the default scale for this Technology.
	 */
	public double getScale() { return scale; }

	/**
	 * Sets the default scale of this technology.
	 * The technology's scale is for manufacturing output, which must convert
	 * the unit-based values in Electric to real-world values.
	 * @param scale the new scale between this technology and the real units.
	 */
	public void setScale(double scale)
	{
		if (scale != 0) this.scale = scale;
	}

	/**
	 * Returns the 0-based index of this Technology.
	 * Each Technology has a unique index that can be used for array lookup.
	 * @return the index of this Technology.
	 */
	public int getIndex() { return techIndex; }

	/**
	 * Returns the total number of Technologies currently in Electric.
	 * @return the total number of Technologies currently in Electric.
	 */
	public static int getNumTechnologies()
	{
		return technologies.size();
	}

	/**
	 * Find the Technology with a particular name.
	 * @param name the name of the desired Technology
	 * @return the Technology with the same name, or null if no 
	 * Technology matches.
	 */
	public static Technology findTechnology(String name)
	{
		for (int i = 0; i < technologies.size(); i++)
		{
			Technology t = (Technology) technologies.get(i);
			if (t.techName.equalsIgnoreCase(name))
				return t;
		}
		return null;
	}

	/**
	 * Get an iterator over all of the Technologies.
	 * @return an iterator over all of the Technologies.
	 */
	public static Iterator getTechnologies()
	{
		return technologies.iterator();
	}

	/**
	 * Returns the polygons that describe node "ni".
	 * @param ni the NodeInst that is being described.
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @return an array of Poly objects that describes this NodeInst graphically.
	 * This array includes displayable variables on the NodeInst.
	 */
	public Poly [] getShape(NodeInst ni)
	{
		NodeProto prototype = ni.getProto();
		if (!(prototype instanceof PrimitiveNode)) return null;

		// see if the node is "wiped" (not drawn)
		if (ni.isWiped()) return null;
		if (prototype.isWipeOn1or2())
		{
			if (ni.pinUseCount()) return null;
		}
		PrimitiveNode np = (PrimitiveNode)prototype;
		Technology.NodeLayer [] primLayers = np.getLayers();
		return getShape(ni, primLayers);
	}

	/**
	 * Returns the polygons that describe node "ni", given a set of
	 * NodeLayer objects to use.
	 * @param ni the NodeInst that is being described.
	 * @param primLayers an array of NodeLayer objects to convert to Poly objects.
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @return an array of Poly objects that describes this NodeInst graphically.
	 * This array includes displayable variables on the NodeInst.
	 */
	public Poly [] getShape(NodeInst ni, Technology.NodeLayer [] primLayers)
	{
		// get information about the node
		double halfWidth = ni.getXSize() / 2;
		double lowX = ni.getCenterX() - halfWidth;
		double highX = ni.getCenterX() + halfWidth;
		double halfHeight = ni.getYSize() / 2;
		double lowY = ni.getCenterY() - halfHeight;
		double highY = ni.getCenterY() + halfHeight;

		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		int [] specialValues = np.getSpecialValues();
		if (specialValues[0] != PrimitiveNode.SERPTRANS && np.isHoldsOutline())
		{
			Float [] outline = ni.getTrace();
			if (outline != null)
			{
				int numPolys = ni.numDisplayableVariables() + 1;
				Poly [] polys = new Poly[numPolys];
				int numPoints = outline.length / 2;
				Point2D.Double [] pointList = new Point2D.Double[numPoints];
				for(int i=0; i<numPoints; i++)
				{
					pointList[i] = new Point2D.Double(ni.getCenterX() + outline[i*2].floatValue(),
						ni.getCenterY() + outline[i*2+1].floatValue());
				}
				polys[0] = new Poly(pointList);
				Technology.NodeLayer primLayer = primLayers[0];
				polys[0].setStyle(primLayer.getStyle());
				polys[0].setLayer(primLayer.getLayer());
				Rectangle2D rect = ni.getBounds();
				ni.addDisplayableVariables(rect, polys, 1);
				return polys;
			}
		}

		// if a MultiCut contact, determine the number of extra cuts
		int numBasicLayers = primLayers.length;
		int numExtraCuts = 0;
		MultiCutData mcd = null;
		SerpentineTrans std = null;
		if (specialValues[0] == PrimitiveNode.MULTICUT)
		{
			mcd = new MultiCutData(ni, np.getSpecialValues());
			numExtraCuts = mcd.cutsTotal;
			numBasicLayers--;
		} else if (specialValues[0] == PrimitiveNode.SERPTRANS)
		{
			std = new SerpentineTrans(ni, np.getSpecialValues());
			if (std.layersTotal > 0)
			{
				numExtraCuts = std.layersTotal;
				numBasicLayers = 0;
			}
		}

		// construct the polygon array
		int numPolys = numBasicLayers + numExtraCuts + ni.numDisplayableVariables();
		Poly [] polys = new Poly[numPolys];
		
		// add in the basic polygons
		for(int i = 0; i < numBasicLayers; i++)
		{
			Technology.NodeLayer primLayer = primLayers[i];
			int representation = primLayer.getRepresentation();
			Poly.Type style = primLayer.getStyle();
			if (representation == Technology.NodeLayer.BOX)
			{
				double portLowX = ni.getCenterX() + primLayer.getLeftEdge().getMultiplier() * ni.getXSize() + primLayer.getLeftEdge().getAdder();
				double portHighX = ni.getCenterX() + primLayer.getRIGHTEDGE().getMultiplier() * ni.getXSize() + primLayer.getRIGHTEDGE().getAdder();
				double portLowY = ni.getCenterY() + primLayer.getBOTTOMEDGE().getMultiplier() * ni.getYSize() + primLayer.getBOTTOMEDGE().getAdder();
				double portHighY = ni.getCenterY() + primLayer.getTOPEDGE().getMultiplier() * ni.getYSize() + primLayer.getTOPEDGE().getAdder();
				double portX = (portLowX + portHighX) / 2;
				double portY = (portLowY + portHighY) / 2;
				polys[i] = new Poly(portX, portY, portHighX-portLowX, portHighY-portLowY);
			} else if (representation == Technology.NodeLayer.POINTS)
			{
				TechPoint [] points = primLayer.getPoints();
				Point2D.Double [] pointList = new Point2D.Double[points.length];
				for(int j=0; j<points.length; j++)
				{
					EdgeH xFactor = points[j].getX();
					EdgeV yFactor = points[j].getY();
					double x = 0, y = 0;
					if (xFactor != null && yFactor != null)
					{
						x = ni.getCenterX() + xFactor.getMultiplier() * ni.getXSize() + xFactor.getAdder();
						y = ni.getCenterY() + yFactor.getMultiplier() * ni.getYSize() + yFactor.getAdder();
					}
					pointList[j] = new Point2D.Double(x, y);
				}
				polys[i] = new Poly(pointList);
			}
			if (style == Poly.Type.TEXTCENT || style == Poly.Type.TEXTTOP || style == Poly.Type.TEXTBOT ||
				style == Poly.Type.TEXTLEFT || style == Poly.Type.TEXTRIGHT || style == Poly.Type.TEXTTOPLEFT ||
				style == Poly.Type.TEXTBOTLEFT || style == Poly.Type.TEXTTOPRIGHT || style == Poly.Type.TEXTBOTRIGHT ||
				style == Poly.Type.TEXTBOX)
			{
				polys[i].setString(primLayer.getMessage());
				polys[i].setTextDescriptor(null);
			}
			polys[i].setStyle(style);
			polys[i].setLayer(primLayer.getLayer());
		}

		// add in the extra contact cuts
		if (mcd != null)
		{
			Technology.NodeLayer primLayer = primLayers[numBasicLayers];
			Poly.Type style = primLayer.getStyle();
			for(int i = 0; i < numExtraCuts; i++)
			{
				polys[numBasicLayers+i] = mcd.fillCutPoly(ni, i);
				polys[numBasicLayers+i].setStyle(style);
				polys[numBasicLayers+i].setLayer(primLayer.getLayer());
			}
		}

		// add in the extra transistor layers
		if (std != null)
		{
			for(int i = 0; i < numExtraCuts; i++)
			{
				polys[numBasicLayers+i] = std.fillTransPoly(ni, i);
			}
		}

		// add in the displayable variables
		Rectangle2D rect = ni.getBounds();
		ni.addDisplayableVariables(rect, polys, numBasicLayers+numExtraCuts);
		return polys;
	}

	/**
	 * Class MultiCutData here.
	 */
	private static class MultiCutData
	{
		/** the size of each cut */													int cutSizeX, cutSizeY;
		/** the separation between cuts */											int cutSep;
		/** the indent of the edge cuts to the node */								int cutIndent;
		/** the number of cuts in X and Y */										int cutsX, cutsY;
		/** the total number of cuts */												int cutsTotal;
		/** the X coordinate of the leftmost cut's center */						double cutBaseX;
		/** the Y coordinate of the topmost cut's center */							double cutBaseY;
		/** cut position of last top-edge cut (for interior-cut elimination) */		int cutTopEdge;
		/** cut position of last left-edge cut  (for interior-cut elimination) */	int cutLeftEdge;
		/** cut position of last right-edge cut  (for interior-cut elimination) */	int cutRightEdge;

		/**
		 * Constructor throws initialize for multiple cuts.
		 * @param ni the NodeInst with multiple cuts.
		 * @param specialValues the array of special values for the NodeInst.
		 * The values in "specialValues" are:
		 *     cuts sized "cutSizeX" x "cutSizeY" (specialValues[1] x specialValues[2])
		 *     cuts indented at least "cutIndent" from the node edge (specialValues[3])
		 *     cuts separated by "cutSep" (specialValues[4])
		 */
		public MultiCutData(NodeInst ni, int [] specialValues)
		{
			cutSizeX = specialValues[1];
			cutSizeY = specialValues[2];
			cutIndent = specialValues[3];
			cutSep = specialValues[4];

			// determine the actual node size
			PrimitiveNode np = (PrimitiveNode)ni.getProto();
			SizeOffset so = Technology.getSizeOffset(ni);
			double cutLX = so.getLowXOffset();
			double cutHX = so.getHighXOffset();
			double cutLY = so.getLowYOffset();
			double cutHY = so.getHighYOffset();

			Rectangle2D.Double bounds = ni.getBounds();
			double lx = bounds.getMinX() + cutLX;
			double hx = bounds.getMaxX() - cutHX;
			double ly = bounds.getMinY() + cutLY;
			double hy = bounds.getMaxY() - cutHY;

			// number of cuts depends on the size
			cutsX = ((int)(hx-lx)-cutIndent*2+cutSep) / (cutSizeX+cutSep);
			cutsY = ((int)(hy-ly)-cutIndent*2+cutSep) / (cutSizeY+cutSep);
			if (cutsX <= 0) cutsX = 1;
			if (cutsY <= 0) cutsY = 1;
			cutsTotal = cutsX * cutsY;
	//		*reasonable = pl->moscuttotal;
			if (cutsTotal != 1)
			{
				// prepare for the multiple contact cut locations
				cutBaseX = (hx-lx-cutIndent*2 - cutSizeX*cutsX -
					cutSep*(cutsX-1)) / 2 + (cutLX + cutIndent + cutSizeX/2) + bounds.getMinX();
				cutBaseY = (hy-ly-cutIndent*2 - cutSizeY*cutsY -
					cutSep*(cutsY-1)) / 2 + (cutLY + cutIndent + cutSizeY/2) + bounds.getMinY();
				if (cutsX > 2 && cutsY > 2)
				{
					//*reasonable = cutsX * 2 + (cutsY-2) * 2;
					cutTopEdge = cutsX*2;
					cutLeftEdge = cutsX*2 + cutsY-2;
					cutRightEdge = cutsX*2 + (cutsY-2)*2;
				}
			}
		}

		/**
		 * routine to fill in the contact cuts of a MOS contact when there are
		 * multiple cuts.  Node is in "ni" and the contact cut number (0 based) is
		 * in "cut".
		 */
		Poly fillCutPoly(NodeInst ni, int cut)
		{
			if (cutsX > 2 && cutsY > 2)
			{
				// rearrange cuts so that the initial ones go around the outside
				if (cut < cutsX)
				{
					// bottom edge: it's ok as is
				} else if (cut < cutTopEdge)
				{
					// top edge: shift up
					cut += cutsX * (cutsY-2);
				} else if (cut < cutLeftEdge)
				{
					// left edge: rearrange
					cut = (cut - cutTopEdge) * cutsX + cutsX;
				} else if (cut < cutRightEdge)
				{
					// right edge: rearrange
					cut = (cut - cutLeftEdge) * cutsX + cutsX*2-1;
				} else
				{
					// center: rearrange and scale down
					cut = cut - cutRightEdge;
					int cutx = cut % (cutsX-2);
					int cuty = cut / (cutsX-2);
					cut = cuty * cutsX + cutx+cutsX+1;
				}
			}

			// locate the X center of the cut
			double cX;
			if (cutsX == 1)
			{
				cX = ni.getCenterX();
			} else
			{
				cX = cutBaseX + (cut % cutsX) * (cutSizeX + cutSep);
			}

			// locate the Y center of the cut
			double cY;
			if (cutsY == 1)
			{
				cY = ni.getCenterY();
			} else
			{
				cY = cutBaseY + (cut / cutsX) * (cutSizeY + cutSep);
			}
			return new Poly(cX, cY, cutSizeX, cutSizeY);
		}
	}

	/**
	 * Class SerpentineTrans here.
	 */
	private static class SerpentineTrans
	{
		/** the number of polygons that make up this serpentine transistor */	int layersTotal;
		/** the number of segments in this serpentine transistor */				int numSegments;
		/** the extra gate width of this serpentine transistor */				double extraScale;
		/** the node layers that make up this serpentine transistor */			Technology.NodeLayer [] primLayers;
		/** the gate coordinates for this serpentine transistor */				Point2D.Double [] points;

		/**
		 * Constructor throws initialize for a serpentine transistor.
		 * @param ni the NodeInst with a serpentine transistor.
		 * @param specialValues the array of special values for the NodeInst.
		 * The values in "specialValues" are:
		 *     layer count is [1]
		 *     active port inset [2] from end of serpentine path
		 *     active port is [3] from poly edge
		 *     poly width is [4]
		 *     poly port inset [5] from poly edge
		 *     poly port is [6] from active edge
		 */
		public SerpentineTrans(NodeInst ni, int [] specialValues)
		{
			points = null;
			layersTotal = 0;
			Float [] outline = ni.getTrace();
			if (outline != null)
			{
				if (outline.length < 4) outline = null;
			}
			if (outline != null)
			{
				PrimitiveNode np = (PrimitiveNode)ni.getProto();
				primLayers = np.getLayers();
				int count = primLayers.length;
				numSegments = outline.length/2 - 1;
				layersTotal = count * numSegments;
				points = new Point2D.Double[outline.length/2];
				for(int i=0; i<outline.length; i += 2)
					points[i/2] = new Point2D.Double(outline[i].intValue(), outline[i+1].intValue());

				extraScale = 0;
				Variable varw = ni.getVal("transistor_width", Integer.class);
				if (varw != null)
				{
					Object obj = varw.getObject();
					extraScale = ((Integer)obj).intValue() / 120 / 2;
				}
			}
		}

		private static final int LEFTANGLE =  900;
		private static final int RIGHTANGLE =  2700;

		/**
		 * routine to describe a box of a serpentine transistor.
		 * If the variable "trace" exists on the node, get that
		 * x/y/x/y information as the centerline of the serpentine path.  The outline is
		 * placed in the polygon "poly".
		 * NOTE: For each trace segment, the left hand side of the trace
		 * will contain the polygons that appear ABOVE the gate in the node
		 * definition. That is, the "top" port and diffusion will be above a
		 * gate segment that extends from left to right, and on the left of a
		 * segment that goes from bottom to top.
		 */
		Poly fillTransPoly(NodeInst ni, int box)
		{
			// compute the segment (along the serpent) and element (of transistor)
			int segment = box % numSegments;
			int element = box / numSegments;

			// see if nonstandard width is specified
			double lwid = primLayers[element].getSerpentineLWidth();
			double rwid = primLayers[element].getSerpentineRWidth();
			double extendt = primLayers[element].getSerpentineExtentT();
			double extendb = primLayers[element].getSerpentineExtentB();
			lwid += extraScale;
			rwid += extraScale;

			// prepare to fill the serpentine transistor
			double xoff = ni.getCenterX();
			double yoff = ni.getCenterY();
			int thissg = segment;   int next = segment+1;
			Point2D.Double thisPt = points[thissg];
			Point2D.Double nextPt = points[next];
			int angle = EMath.figureAngle(thisPt, nextPt);

			// push the points at the ends of the transistor
			if (thissg == 0)
			{
				// extend "thissg" 180 degrees back
				int ang = angle+1800;
				thisPt = EMath.addPoints(thisPt, EMath.cos(ang) * extendt, EMath.sin(ang) * extendt);
			}
			if (next == numSegments)
			{
				// extend "next" 0 degrees forward
				nextPt = EMath.addPoints(nextPt, EMath.cos(angle) * extendb, EMath.sin(angle) * extendb);
			}

			// compute endpoints of line parallel to and left of center line
			int ang = angle+LEFTANGLE;
			double sin = EMath.sin(ang) * lwid;
			double cos = EMath.cos(ang) * lwid;
			Point2D.Double thisL = EMath.addPoints(thisPt, cos, sin);
			Point2D.Double nextL = EMath.addPoints(nextPt, cos, sin);

			// compute endpoints of line parallel to and right of center line
			ang = angle+RIGHTANGLE;
			sin = EMath.sin(ang) * rwid;
			cos = EMath.cos(ang) * rwid;
			Point2D.Double thisR = EMath.addPoints(thisPt, cos, sin);
			Point2D.Double nextR = EMath.addPoints(nextPt, cos, sin);

			// determine proper intersection of this and the previous segment
			if (thissg != 0)
			{
				Point2D.Double otherPt = points[thissg-1];
				int otherang = EMath.figureAngle(otherPt, thisPt);
				if (otherang != angle)
				{
					ang = otherang + LEFTANGLE;
					thisL = EMath.intersect(EMath.addPoints(thisPt, EMath.cos(ang)*lwid, EMath.sin(ang)*lwid),
						otherang, thisL,angle);
					ang = otherang + RIGHTANGLE;
					thisR = EMath.intersect(EMath.addPoints(thisPt, EMath.cos(ang)*rwid, EMath.sin(ang)*rwid),
						otherang, thisR,angle);
				}
			}

			// determine proper intersection of this and the next segment
			if (next != numSegments)
			{
				Point2D.Double otherPt = points[next+1];
				int otherang = EMath.figureAngle(nextPt, otherPt);
				if (otherang != angle)
				{
					ang = otherang + LEFTANGLE;
					Point2D.Double newPtL = EMath.addPoints(nextPt, EMath.cos(ang)*lwid, EMath.sin(ang)*lwid);
					nextL = EMath.intersect(newPtL, otherang, nextL,angle);
					ang = otherang + RIGHTANGLE;
					Point2D.Double newPtR = EMath.addPoints(nextPt, EMath.cos(ang)*rwid, EMath.sin(ang)*rwid);
					nextR = EMath.intersect(newPtR, otherang, nextR,angle);
				}
			}

			// fill the polygon
			Point2D.Double [] points = new Point2D.Double[4];
			points[0] = EMath.addPoints(thisL, xoff, yoff);
			points[1] = EMath.addPoints(thisR, xoff, yoff);
			points[2] = EMath.addPoints(nextR, xoff, yoff);
			points[3] = EMath.addPoints(nextL, xoff, yoff);
			Poly retPoly = new Poly(points);

			// see if the sides of the polygon intersect
//			ang = figureangle(poly->xv[0], poly->yv[0], poly->xv[1], poly->yv[1]);
//			angle = figureangle(poly->xv[2], poly->yv[2], poly->xv[3], poly->yv[3]);
//			if (intersect(poly->xv[0], poly->yv[0], ang, poly->xv[2], poly->yv[2], angle, &x, &y) >= 0)
//			{
//				// lines intersect, see if the point is on one of the lines
//				if (x >= mini(poly->xv[0], poly->xv[1]) && x <= maxi(poly->xv[0], poly->xv[1]) &&
//					y >= mini(poly->yv[0], poly->yv[1]) && y <= maxi(poly->yv[0], poly->yv[1]))
//				{
//					if (abs(x-poly->xv[0])+abs(y-poly->yv[0]) > abs(x-poly->xv[1])+abs(y-poly->yv[1]))
//					{
//						poly->xv[1] = x;   poly->yv[1] = y;
//						poly->xv[2] = poly->xv[3];   poly->yv[2] = poly->yv[3];
//					} else
//					{
//						poly->xv[0] = x;   poly->yv[0] = y;
//					}
//					poly->count = 3;
//				}
//			}

			Technology.NodeLayer primLayer = primLayers[element];
			retPoly.setStyle(primLayer.getStyle());
			retPoly.setLayer(primLayer.getLayer());
			return retPoly;
		}

		/**
		 * routine to describe a port in a transistor that may be part of a serpentine
		 * path.  If the variable "trace" exists on the node, get that x/y/x/y
		 * information as the centerline of the serpentine path.  The port path
		 * is shrunk by "diffinset" in the length and is pushed "diffextend" from the centerline.
		 * The default width of the transistor is "defwid".  The outline is placed
		 * in the polygon "poly".
		 * The assumptions about directions are:
		 * Segments have port 1 to the left, and port 3 to the right of the gate
		 * trace. Port 0, the "left-hand" end of the gate, appears at the starting
		 * end of the first trace segment; port 2, the "right-hand" end of the gate,
		 * appears at the end of the last trace segment.  Port 3 is drawn as a
		 * reflection of port 1 around the trace.
		 * The values "diffinset", "diffextend", "defwid", "polyinset", and "polyextend"
		 * are used to determine the offsets of the ports:
		 * The poly ports are extended "polyextend" beyond the appropriate end of the trace
		 * and are inset by "polyinset" from the polysilicon edge.
		 * The diffusion ports are extended "diffextend" from the polysilicon edge
		 * and set in "diffinset" from the ends of the trace segment.
		 */
//		Poly fillTransPort(NodeInst ni, PortProto *pp, XARRAY trans,
//			TECH_NODES *nodedata, int diffinset, int diffextend, int defwid,
//			int polyinset, int polyextend)
//		{
//			/* see if the transistor has serpentine information */
//			var = gettrace(ni);
//			if (var != NOVARIABLE)
//			{
//				/* trace data is there: make sure there are enough points */
//				total = getlength(var);
//				if (total <= 2) var = NOVARIABLE;
//			}
//
//			/* nonserpentine transtors fill in the normal way */
//			lambda = lambdaofnode(ni);
//			if (var == NOVARIABLE)
//			{
//				tech_fillportpoly(ni, pp, poly, trans, nodedata, -1, lambda);
//				return;
//			}
//
//			/* prepare to fill the serpentine transistor port */
//			list = (INTBIG *)var->addr;
//			poly->style = OPENED;
//			xoff = (ni->highx+ni->lowx)/2;
//			yoff = (ni->highy+ni->lowy)/2;
//			total /= 2;
//
//			/* see if nonstandard width is specified */
//			defwid = lambda * defwid / WHOLE;
//			diffinset = lambda * diffinset / WHOLE;   diffextend = lambda * diffextend / WHOLE;
//			polyinset = lambda * polyinset / WHOLE;   polyextend = lambda * polyextend / WHOLE;
//			varw = getvalkey((INTBIG)ni, VNODEINST, VFRACT, el_transistor_width_key);
//			if (varw != NOVARIABLE) defwid = lambda * varw->addr / WHOLE;
//
//			/* determine which port is being described */
//			for(lpp = ni->proto->firstportproto, which=0; lpp != NOPORTPROTO;
//				lpp = lpp->nextportproto, which++) if (lpp == pp) break;
//
//			/* ports 0 and 2 are poly (simple) */
//			if (which == 0)
//			{
//				if (poly->limit < 2) (void)extendpolygon(poly, 2);
//				thisx = list[0];   thisy = list[1];
//				nextx = list[2];   nexty = list[3];
//				angle = figureangle(thisx, thisy, nextx, nexty);
//				ang = (angle+1800) % 3600;
//				thisx += mult(cosine(ang), polyextend) + xoff;
//				thisy += mult(sine(ang), polyextend) + yoff;
//				ang = (angle+LEFTANGLE) % 3600;
//				nextx = thisx + mult(cosine(ang), defwid/2-polyinset);
//				nexty = thisy + mult(sine(ang), defwid/2-polyinset);
//				xform(nextx, nexty, &poly->xv[0], &poly->yv[0], trans);
//				ang = (angle+RIGHTANGLE) % 3600;
//				nextx = thisx + mult(cosine(ang), defwid/2-polyinset);
//				nexty = thisy + mult(sine(ang), defwid/2-polyinset);
//				xform(nextx, nexty, &poly->xv[1], &poly->yv[1], trans);
//				poly->count = 2;
//				return;
//			}
//			if (which == 2)
//			{
//				if (poly->limit < 2) (void)extendpolygon(poly, 2);
//				thisx = list[(total-1)*2];   thisy = list[(total-1)*2+1];
//				nextx = list[(total-2)*2];   nexty = list[(total-2)*2+1];
//				angle = figureangle(thisx, thisy, nextx, nexty);
//				ang = (angle+1800) % 3600;
//				thisx += mult(cosine(ang), polyextend) + xoff;
//				thisy += mult(sine(ang), polyextend) + yoff;
//				ang = (angle+LEFTANGLE) % 3600;
//				nextx = thisx + mult(cosine(ang), defwid/2-polyinset);
//				nexty = thisy + mult(sine(ang), defwid/2-polyinset);
//				xform(nextx, nexty, &poly->xv[0], &poly->yv[0], trans);
//				ang = (angle+RIGHTANGLE) % 3600;
//				nextx = thisx + mult(cosine(ang), defwid/2-polyinset);
//				nexty = thisy + mult(sine(ang), defwid/2-polyinset);
//				xform(nextx, nexty, &poly->xv[1], &poly->yv[1], trans);
//				poly->count = 2;
//				return;
//			}
//
//			/* THE ORIGINAL CODE TREATED PORT 1 AS THE NEGATED PORT ... SRP */
//			/* port 3 is the negated path side of port 1 */
//			if (which == 3)
//			{
//				diffextend = -diffextend;
//				defwid = -defwid;
//			}
//
//			/* extra port on some n-transistors */
//			if (which == 4) diffextend = defwid = 0;
//
//			/* polygon will need total points */
//			if (poly->limit < total) (void)extendpolygon(poly, total);
//
//			for(next=1; next<total; next++)
//			{
//				thissg = next-1;
//				thisx = list[thissg*2];   thisy = list[thissg*2+1];
//				nextx = list[next*2];   nexty = list[next*2+1];
//				angle = figureangle(thisx, thisy, nextx, nexty);
//
//				/* determine the points */
//				if (thissg == 0)
//				{
//					/* extend "thissg" 0 degrees forward */
//					thisx += mult(cosine(angle), diffinset);
//					thisy += mult(sine(angle), diffinset);
//				}
//				if (next == total-1)
//				{
//					/* extend "next" 180 degrees back */
//					ang = (angle+1800) % 3600;
//					nextx += mult(cosine(ang), diffinset);
//					nexty += mult(sine(ang), diffinset);
//				}
//
//				/* compute endpoints of line parallel to center line */
//				ang = (angle+LEFTANGLE) % 3600;   sin = sine(ang);   cos = cosine(ang);
//				thisx += mult(cos, defwid/2+diffextend);   thisy += mult(sin, defwid/2+diffextend);
//				nextx += mult(cos, defwid/2+diffextend);   nexty += mult(sin, defwid/2+diffextend);
//
//				if (thissg != 0)
//				{
//					/* compute intersection of this and previous line */
//
//					/* LINTED "pthisx", "pthisy", and "pangle" used in proper order */
//					(void)intersect(pthisx, pthisy, pangle, thisx, thisy, angle, &x, &y);
//					thisx = x;   thisy = y;
//					xform(thisx+xoff, thisy+yoff, &poly->xv[thissg], &poly->yv[thissg], trans);
//				} else
//					xform(thisx+xoff, thisy+yoff, &poly->xv[0], &poly->yv[0], trans);
//				pthisx = thisx;   pthisy = thisy;
//				pangle = angle;
//			}
//
//			xform(nextx+xoff, nexty+yoff, &poly->xv[total-1], &poly->yv[total-1], trans);
//			poly->count = total;
//		}
	}

	/**
	 * Returns the polygons that describe arc "ai".
	 * @param ai the ArcInst that is being described.
	 * @return an array of Poly objects that describes this ArcInst graphically.
	 * This array includes displayable variables on the ArcInst.
	 */
	public Poly [] getShape(ArcInst ai)
	{
		// get information about the arc
		PrimitiveArc ap = (PrimitiveArc)ai.getProto();
		Technology tech = ap.getTechnology();
		Technology.ArcLayer [] primLayers = ap.getLayers();

		// see how many polygons describe this arc
		boolean addArrow = false;
		if (!tech.isNoDirectionalArcs() && ai.isDirectional()) addArrow = true;
		int numDisplayable = ai.numDisplayableVariables();
		int maxPolys = primLayers.length + numDisplayable;
		if (addArrow) maxPolys++;
		Poly [] polys = new Poly[maxPolys];
		int polyNum = 0;

		// construct the polygons that describe the basic arc
		for(int i = 0; i < primLayers.length; i++)
		{
			Technology.ArcLayer primLayer = primLayers[i];
			polys[polyNum] = ai.makePoly(ai.getXSize(), ai.getWidth() - primLayer.getOffset(), primLayer.getStyle());
			if (polys[polyNum] == null) return null;
			polys[polyNum].setLayer(primLayer.getLayer());
			polyNum++;
		}

		// add an arrow to the arc description
		if (addArrow)
		{
			Point2D.Double headLoc = ai.getHead().getLocation();
			Point2D.Double tailLoc = ai.getTail().getLocation();
			double headX = headLoc.getX();   double headY = headLoc.getY();
			double tailX = tailLoc.getX();   double tailY = tailLoc.getY();
			double angle = ai.getAngle();
			if (ai.isReverseEnds())
			{
				double swap = headX;   headX = tailX;   tailX = swap;
				swap = headY;   headY = tailY;   tailY = swap;
				angle += Math.PI;
			}
			int numPoints = 6;
			if (ai.isSkipHead()) numPoints = 2;
			Point2D.Double [] points = new Point2D.Double[numPoints];
			points[0] = new Point2D.Double(headX, headY);
			points[1] = new Point2D.Double(tailX, tailY);
			if (!ai.isSkipHead())
			{
				points[2] = points[0];
				double angleOfArrow = Math.PI/6;		// 30 degrees
				double backAngle1 = angle - angleOfArrow;
				double backAngle2 = angle + angleOfArrow;
				points[3] = new Point2D.Double(headX + Math.cos(backAngle1), headY + Math.sin(backAngle1));
				points[4] = points[0];
				points[5] = new Point2D.Double(headX + Math.cos(backAngle2), headY + Math.sin(backAngle2));
			}
			polys[polyNum] = new Poly(points);
			polys[polyNum].setStyle(Poly.Type.VECTORS);
			polys[polyNum].setLayer(null);
			polyNum++;
		}
		
		// add in the displayable variables
		Rectangle2D rect = ai.getBounds();
		ai.addDisplayableVariables(rect, polys, polyNum);

		return polys;
	}

	/**
	 * Returns a polygon that describes a particular port on a NodeInst.
	 * @param ni the NodeInst that has the port of interest.
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @param pp the PrimitivePort on that NodeInst that is being described.
	 * @return a Poly object that describes this PrimitivePort graphically.
	 */
	public Poly getPoly(NodeInst ni, PrimitivePort pp)
	{
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		int [] specialValues = np.getSpecialValues();
//		if (specialValues[0] == PrimitiveNode.SERPTRANS)
//		{
//			// serpentine transistors use a more complex port determination (tech_filltransport)
//			Poly portpoly = new Poly(0, 0, 0, 0);
//			return portpoly;
//		} else
		{
			// standard port determination, see if there is outline information
//			if (np.isHoldsOutline())
//			{
//				// outline may determinesthe port
//				Poly portpoly = new Poly(1, 2, 3, 4);
//				return portpoly;
//			} else
			{
				// standard port computation
				double halfWidth = ni.getXSize() / 2;
				double lowX = ni.getCenterX() - halfWidth;
				double highX = ni.getCenterX() + halfWidth;
				double halfHeight = ni.getYSize() / 2;
				double lowY = ni.getCenterY() - halfHeight;
				double highY = ni.getCenterY() + halfHeight;
				
				double portLowX = ni.getCenterX() + pp.getLeft().getMultiplier() * ni.getXSize() + pp.getLeft().getAdder();
				double portHighX = ni.getCenterX() + pp.getRight().getMultiplier() * ni.getXSize() + pp.getRight().getAdder();
				double portLowY = ni.getCenterY() + pp.getBottom().getMultiplier() * ni.getYSize() + pp.getBottom().getAdder();
				double portHighY = ni.getCenterY() +pp.getTop().getMultiplier() * ni.getYSize() + pp.getTop().getAdder();
				double portX = (portLowX + portHighX) / 2;
				double portY = (portLowY + portHighY) / 2;
				Poly portpoly = new Poly(portX, portY, portHighX-portLowX, portHighY-portLowY);
				return portpoly;
			}
		}
	}

	public static SizeOffset getSizeOffset(NodeInst ni)
	{
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		return np.getSizeOffset();
	}

	/*
	 * Routine to write a description of this Technology.
	 * Displays the description in the Messages Window.
	 */
	protected void getInfo()
	{
		System.out.println(" Name: " + techName);
		System.out.println(" Description: " + techDesc);
		System.out.println(" Nodes (" + nodes.size() + ")");
		for (int i = 0; i < nodes.size(); i++)
		{
			System.out.println("     " + nodes.get(i));
		}
		System.out.println(" Arcs (" + arcs.size() + ")");
		for (int i = 0; i < arcs.size(); i++)
		{
			System.out.println("     " + arcs.get(i));
		}
		super.getInfo();
	}

	/**
	 * Routine to convert old primitive node names to their proper NodeProtos.
	 * This method is overridden by those technologies that have any special node name conversion issues.
	 * By default, there is nothing to be done, because by the time this
	 * routine is called, normal searches have failed.
	 * @param name the unknown node name, read from an old Library.
	 * @return the proper PrimitiveNode to use for this name.
	 */
	public PrimitiveNode convertOldNodeName(String name) { return null; }

	/**
	 * Routine to convert old primitive arc names to their proper ArcProtos.
	 * This method is overridden by those technologies that have any special arc name conversion issues.
	 * By default, there is nothing to be done, because by the time this
	 * routine is called, normal searches have failed.
	 * @param name the unknown arc name, read from an old Library.
	 * @return the proper PrimitiveArc to use for this name.
	 */
	public PrimitiveArc convertOldArcName(String name) { return null; }

	/**
	 * Routine to convert old primitive port names to their proper PortProtos.
	 * This method is overridden by those technologies that have any special port name conversion issues.
	 * By default, there is nothing to be done, because by the time this
	 * routine is called, normal searches have failed.
	 * @param portName the unknown port name, read from an old Library.
	 * @param np the PrimitiveNode on which this port resides.
	 * @return the proper PrimitivePort to use for this name.
	 */
	public PrimitivePort convertOldPortName(String portName, PrimitiveNode np)
	{
//		if (np == sch_sourceprim || np == sch_meterprim)
//		{
//			if (portname.equals("top")) return np->firstportproto;
//			if (portname.equals("bottom")) return np->firstportproto->nextportproto;
//		}
//		if (np == sch_twoportprim)
//		{
//			if (portname.equals("upperleft")) return(np->firstportproto);
//			if (portname.equals("lowerleft")) return(np->firstportproto->nextportproto);
//			if (portname.equals("upperright")) return(np->firstportproto->nextportproto->nextportproto);
//			if (portname.equals("lowerright")) return(np->firstportproto->nextportproto->nextportproto->nextportproto);
//		}

		// some technologies switched from ports ending in "-bot" to the ending "-bottom"
		int len = portName.length() - 4;
		if (len > 0 && portName.substring(len).equals("-bot"))
		{
			PrimitivePort pp = (PrimitivePort)np.findPortProto(portName + "tom");
			if (pp != null) return pp;
		}
		return null;
	}

	/**
	 * Returns a printable version of this Technology.
	 * @return a printable version of this Technology.
	 */
	public String toString()
	{
		return "Technology " + techName;
	}

	/**
	 * Routine to determine the appropriate Technology to use for a Cell.
	 * @param cell the Cell to examine.
	 * @return the Technology for that cell.
	 */
	public static Technology whatTechnology(NodeProto cell)
	{
		return whatTechnology(cell, null, 0, 0, null, 0, 0);
	}

	/**
	 * Routine to determine the appropriate technology to use for a cell.
	 * The contents of the cell can be defined by the lists of NodeInsts and ArcInsts, or
	 * if they are null, then by the contents of the Cell.
	 * @param cell the Cell to examine.
	 * @param nodeProtoList the list of prototypes of NodeInsts in the Cell.
	 * @param startNodeProto the starting point in the "nodeProtoList" array.
	 * @param endNodeProto the ending point in the "nodeProtoList" array.
	 * @param arcProtoList the list of prototypes of ArcInsts in the Cell.
	 * @param startArcProto the starting point in the "arcProtoList" array.
	 * @param endArcProto the ending point in the "arcProtoList" array.
	 * @return the Technology for that cell.
	 */
	public static Technology whatTechnology(NodeProto cell, NodeProto [] nodeProtoList, int startNodeProto, int endNodeProto,
		ArcProto [] arcProtoList, int startArcProto, int endArcProto)
	{
		// primitives know their technology
		if (cell instanceof PrimitiveNode) return(((PrimitiveNode)cell).getTechnology());

		// count the number of technologies
		int maxTech = 0;
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			if (tech.getIndex() > maxTech) maxTech = tech.getIndex();
		}
		maxTech++;

		// create an array of counts for each technology
		int [] useCount = new int[maxTech];
		for(int i=0; i<maxTech; i++) useCount[i] = 0;

		// count technologies of all primitive nodes in the cell
		if (nodeProtoList != null)
		{
			// iterate over the NodeProtos in the list
			for(int i=startNodeProto; i<endNodeProto; i++)
			{
				NodeProto np = nodeProtoList[i];
				if (np == null) continue;
				Technology nodeTech = np.getTechnology();
				if (nodeTech != null) useCount[nodeTech.getIndex()]++;
			}
		} else
		{
			for(Iterator it = ((Cell)cell).getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				NodeProto np = ni.getProto();
				Technology nodeTech = np.getTechnology();
				if (nodeTech != null) useCount[nodeTech.getIndex()]++;
			}
		}

		// count technologies of all arcs in the cell
		if (arcProtoList != null)
		{
			// iterate over the arcprotos in the list
			for(int i=startArcProto; i<endArcProto; i++)
			{
				ArcProto ap = arcProtoList[i];
				if (ap == null) continue;
				useCount[ap.getTechnology().getIndex()]++;
			}
		} else
		{
			for(Iterator it = ((Cell)cell).getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				ArcProto ap = ai.getProto();
				useCount[ap.getTechnology().getIndex()]++;
			}
		}

		// find a concensus
		int best = 0;         Technology bestTech = null;
		int bestLayout = 0;   Technology bestLayoutTech = null;
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();

			// always ignore the generic technology
			if (tech == Generic.tech) continue;

			// find the most popular of ALL technologies
			if (useCount[tech.getIndex()] > best)
			{
				best = useCount[tech.getIndex()];
				bestTech = tech;
			}

			// find the most popular of the layout technologies
			if (tech == Schematics.tech || tech == Artwork.tech) continue;
			if (useCount[tech.getIndex()] > bestLayout)
			{
				bestLayout = useCount[tech.getIndex()];
				bestLayoutTech = tech;
			}
		}

		Technology retTech = null;
		if (((Cell)cell).getView() == View.ICON)
		{
			// in icons, if there is any artwork, use it
			if (useCount[Artwork.tech.getIndex()] > 0) return(Artwork.tech);

			// in icons, if there is nothing, presume artwork
			if (bestTech == null) return(Artwork.tech);

			// use artwork as a default
			retTech = Artwork.tech;
		} else if (((Cell)cell).getView() == View.SCHEMATIC)
		{
			// in schematic, if there are any schematic components, use it
			if (useCount[Schematics.tech.getIndex()] > 0) return(Schematics.tech);

			// in schematic, if there is nothing, presume schematic
			if (bestTech == null) return(Schematics.tech);

			// use schematic as a default
			retTech = Schematics.tech;
		} else
		{
			// use the current layout technology as the default
			retTech = curLayoutTech;
		}

		// if a layout technology was voted the most, return it
		if (bestLayoutTech != null) retTech = bestLayoutTech; else
		{
			// if any technology was voted the most, return it
			if (bestTech != null) retTech = bestTech; else
			{
//				// if this is an icon, presume the technology of its contents
//				cv = contentsview(cell);
//				if (cv != NONODEPROTO)
//				{
//					if (cv->tech == NOTECHNOLOGY)
//						cv->tech = whattech(cv);
//					retTech = cv->tech;
//				} else
//				{
//					// look at the contents of the sub-cells
//					foundicons = FALSE;
//					for(ni = cell->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//					{
//						np = ni->proto;
//						if (np == NONODEPROTO) continue;
//						if (np->primindex != 0) continue;
//
//						// ignore recursive references (showing icon in contents)
//						if (isiconof(np, cell)) continue;
//
//						// see if the cell has an icon
//						if (np->cellview == el_iconview) foundicons = TRUE;
//
//						// do not follow into another library
//						if (np->lib != cell->lib) continue;
//						onp = contentsview(np);
//						if (onp != NONODEPROTO) np = onp;
//						tech = whattech(np);
//						if (tech == gen_tech) continue;
//						retTech = tech;
//						break;
//					}
//					if (ni == NONODEINST)
//					{
//						// could not find instances that give information: were there icons?
//						if (foundicons) retTech = sch_tech;
//					}
//				}
			}
		}

		// give up and report the generic technology
		return retTech;
	}

	// *************************** ArcProtos ***************************

	/**
	 * Returns the PrimitiveArc in this technology with a particular name.
	 * @param name the name of the PrimitiveArc.
	 * @return the PrimitiveArc in this technology with that name.
	 */
	public PrimitiveArc findArcProto(String name)
	{
		for (int i = 0; i < arcs.size(); i++)
		{
			PrimitiveArc ap = (PrimitiveArc) arcs.get(i);
			if (ap.getProtoName().equalsIgnoreCase(name))
				return ap;
		}
		return null;
	}

	/**
	 * Returns an Iterator on the PrimitiveArc objects in this technology.
	 * @return an Iterator on the PrimitiveArc objects in this technology.
	 */
	public Iterator getArcs()
	{
		return arcs.iterator();
	}

	/**
	 * Returns the number of PrimitiveArc objects in this technology.
	 * @return the number of PrimitiveArc objects in this technology.
	 */
	public int getNumArcs()
	{
		return arcs.size();
	}

	// *************************** NodeProtos ***************************

	/**
	 * Returns the PrimitiveNode in this technology with a particular name.
	 * @param name the name of the PrimitiveNode.
	 * @return the PrimitiveNode in this technology with that name.
	 */
	public PrimitiveNode findNodeProto(String name)
	{
		for (int i = 0; i < nodes.size(); i++)
		{
			PrimitiveNode pn = (PrimitiveNode) nodes.get(i);
			if (pn.getProtoName().equalsIgnoreCase(name))
				return pn;
		}
		return null;
	}

	/**
	 * Returns an Iterator on the PrimitiveNode objects in this technology.
	 * @return an Iterator on the PrimitiveNode objects in this technology.
	 */
	public Iterator getNodes()
	{
		return nodes.iterator();
	}

	/**
	 * Returns the number of PrimitiveNodes objects in this technology.
	 * @return the number of PrimitiveNodes objects in this technology.
	 */
	public int getNumNodes()
	{
		return nodes.size();
	}

}
