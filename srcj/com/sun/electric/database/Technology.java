package com.sun.electric.database;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.geom.Point2D;

/**
 * A Technology object contains PrimitiveNodes and ArcProtos.  There may
 * be more than one Technology object.  To get a particular Technology,
 * use <code>Electric.findTechnology</code>
 */
public class Technology extends ElectricObject
{
	public static class ArcLayer
	{
		Layer layer;
		int offset;
		Poly.Type style;

		public ArcLayer(Layer layer, int offset, Poly.Type style)
		{
			this.layer = layer;
			this.offset = offset;
			this.style = style;
		}
		public Layer getLayer() { return layer; }
		public int getOffset() { return offset; }
		public Poly.Type getStyle() { return style; }
	}

	public static class TechPoint
	{
		private EdgeH x;
		private EdgeV y;
		
		public static final TechPoint [] ATCENTER = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.AtCenter, EdgeV.AtCenter),
					new Technology.TechPoint(EdgeH.AtCenter, EdgeV.AtCenter)};
		public static final TechPoint [] FULLBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.LeftEdge, EdgeV.BottomEdge),
					new Technology.TechPoint(EdgeH.RightEdge, EdgeV.TopEdge)};
		public static final TechPoint [] IN0HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(0.5), EdgeV.fromBottom(0.5)),
					new Technology.TechPoint(EdgeH.fromRight(0.5), EdgeV.fromTop(0.5))};
		public static final TechPoint [] IN1BOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
					new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(1))};
		public static final TechPoint [] IN1HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5)),
					new Technology.TechPoint(EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))};
		public static final TechPoint [] IN2BOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromBottom(2)),
					new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.fromTop(2))};
		public static final TechPoint [] IN2HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5)),
					new Technology.TechPoint(EdgeH.fromRight(2.5), EdgeV.fromTop(2.5))};
		public static final TechPoint [] IN3BOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(3)),
					new Technology.TechPoint(EdgeH.fromRight(3), EdgeV.fromTop(3))};
		public static final TechPoint [] IN3HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(3.5), EdgeV.fromBottom(3.5)),
					new Technology.TechPoint(EdgeH.fromRight(3.5), EdgeV.fromTop(3.5))};
		public static final TechPoint [] IN4BOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(4)),
					new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(4))};
		public static final TechPoint [] IN4HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(4.5), EdgeV.fromBottom(4.5)),
					new Technology.TechPoint(EdgeH.fromRight(4.5), EdgeV.fromTop(4.5))};
		public static final TechPoint [] IN5BOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(5), EdgeV.fromBottom(5)),
					new Technology.TechPoint(EdgeH.fromRight(5), EdgeV.fromTop(5))};
		public static final TechPoint [] IN5HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(5.5), EdgeV.fromBottom(5.5)),
					new Technology.TechPoint(EdgeH.fromRight(5.5), EdgeV.fromTop(5.5))};
		public static final TechPoint [] IN6BOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(6)),
					new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(6))};
		public static final TechPoint [] IN6HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6.5), EdgeV.fromBottom(6.5)),
					new Technology.TechPoint(EdgeH.fromRight(6.5), EdgeV.fromTop(6.5))};
		public static final TechPoint [] IN7BOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7), EdgeV.fromBottom(7)),
					new Technology.TechPoint(EdgeH.fromRight(7), EdgeV.fromTop(7))};
		public static final TechPoint [] IN7HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromBottom(7.5)),
					new Technology.TechPoint(EdgeH.fromRight(7.5), EdgeV.fromTop(7.5))};

		public TechPoint(EdgeH x, EdgeV y)
		{
			this.x = x;
			this.y = y;
		}
		public EdgeH getX() { return x; }
		public EdgeV getY() { return y; }
	}

	public static class NodeLayer
	{
		private Layer layer;
		private int portNum;
		private Poly.Type style;
		private int representation;
		private TechPoint [] points;

		/** list of scalable points */		public static final int POINTS=     0;
		/** a rectangle */					public static final int BOX=        1;
		/** list of absolute points */		public static final int ABSPOINTS=  2;
		/** minimum sized rectangle */		public static final int MINBOX=     3;

		public NodeLayer(Layer layer, int portNum, Poly.Type style, int representation,
			TechPoint [] points)
		{
			this.layer = layer;
			this.portNum = portNum;
			this.style = style;
			this.representation = representation;
			this.points = points;
		}
		public Layer getLayer() { return layer; }
		public int getPortNum() { return portNum; }
		public Poly.Type getStyle() { return style; }
		public int getRepresentation() { return representation; }
		public TechPoint [] getPoints() { return points; }
		public EdgeH getLeftEdge() { return points[0].getX(); }
		public EdgeV getBottomEdge() { return points[0].getY(); }
		public EdgeH getRightEdge() { return points[1].getX(); }
		public EdgeV getTopEdge() { return points[1].getY(); }
	}

	/** name of the technology */						private String techName;
	/** full description of the technology */			private String techDesc;
	/** critical dimensions for the technology */		private double scale;
	/** list of primitive nodes in the technology */	private List nodes;
	/** list of arcs in the technology */				private List arcs;

	/* static list of all Technologies in Electric */	private static List technologies = new ArrayList();
	/* the current technology in Electric */			private static Technology curTech = null;

	protected Technology()
	{
		this.nodes = new ArrayList();
		this.arcs = new ArrayList();
		scale = 1.0;

		// add the technology to the global list
		technologies.add(this);
	}

	protected static boolean validTechnology(String techName)
	{
		if (Technology.findTechnology(techName) != null)
		{
			System.out.println("ERROR: Multiple technologies named " + techName);
			return false;
		}
		return true;
	}

	void addNodeProto(PrimitiveNode pn)
	{
		nodes.add(pn);
	}

	void addArcProto(ArcProto ap)
	{
		arcs.add(ap);
	}

	/**
	 * Return the current Technology
	 */
	public static Technology getCurrent() { return curTech; }

	/**
	 * Set the current Technology
	 */
	public static void setCurrent(Technology tech) { curTech = tech; }

	/** 
	 * get the name (short) of this technology
	 */
	public String getTechName() { return techName; }

	/** 
	 * set the name (short) of this technology
	 */
	protected void setTechName(String techName) { this.techName = techName; }

	/**
	 * get the description (long) of this technology
	 */
	public String getTechDesc() { return techDesc; }

	/**
	 * get the description (long) of this technology
	 */
	public void setTechDesc(String techDesc) { this.techDesc = techDesc; }

	/**
	 * get the default scale for this technology.
	 * typically overridden by a library
	 */
	public double getScale() { return scale; }

	/**
	 * set the default scale of this technology.
	 */
	public void setScale(double scale)
	{
		if (scale != 0) this.scale = scale;
	}

	/** Find the Technology with a particular name.
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
	 * Get a list of polygons that describe node "ni"
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

		// get information about the node
		double halfWidth = ni.getXSize() / 2;
		double lowX = ni.getCenterX() - halfWidth;
		double highX = ni.getCenterX() + halfWidth;
		double halfHeight = ni.getYSize() / 2;
		double lowY = ni.getCenterY() - halfHeight;
		double highY = ni.getCenterY() + halfHeight;
		
		// construct the polygons
		Poly [] polys = new Poly[primLayers.length];
		for(int i = 0; i < primLayers.length; i++)
		{
			Technology.NodeLayer primLayer = primLayers[i];
			int representation = primLayer.getRepresentation();
			Poly.Type style = primLayer.getStyle();
			if (representation == Technology.NodeLayer.BOX)
			{
//				if (style == Poly.Type.FILLEDRECT || style == Poly.Type.CLOSEDRECT)
//				{
//				} else
				{
					double portLowX = ni.getCenterX() + primLayer.getLeftEdge().getMultiplier() * ni.getXSize() + primLayer.getLeftEdge().getAdder();
					double portHighX = ni.getCenterX() + primLayer.getRightEdge().getMultiplier() * ni.getXSize() + primLayer.getRightEdge().getAdder();
					double portLowY = ni.getCenterY() + primLayer.getBottomEdge().getMultiplier() * ni.getYSize() + primLayer.getBottomEdge().getAdder();
					double portHighY = ni.getCenterY() + primLayer.getTopEdge().getMultiplier() * ni.getYSize() + primLayer.getTopEdge().getAdder();
					double portX = (portLowX + portHighX) / 2;
					double portY = (portLowY + portHighY) / 2;
					polys[i] = new Poly(portX, portY, portHighX-portLowX, portHighY-portLowY);
				}
			} else if (representation == Technology.NodeLayer.POINTS)
			{
				TechPoint [] points = primLayer.getPoints();
				Point2D.Double [] pointList = new Point2D.Double[points.length];
				for(int j=0; j<points.length; j++)
				{
					double x = ni.getCenterX() + points[j].getX().getMultiplier() * ni.getXSize() + points[j].getX().getAdder();
					double y = ni.getCenterY() + points[j].getY().getMultiplier() * ni.getYSize() + points[j].getY().getAdder();
					pointList[j] = new Point2D.Double(x, y);
				}
				polys[i] = new Poly(pointList);
			}
			polys[i].setStyle(style);
			polys[i].setLayer(primLayer.getLayer());
		}
		return polys;
	}

	/**
	 * Get a list of polygons that describe arc "ai"
	 */
	public Poly [] getShape(ArcInst ai)
	{
		ArcProto ap = ai.getProto();
		Technology.ArcLayer [] primLayers = ap.getLayers();

		// get information about the arc
		double halfWidth = ai.getXSize() / 2;
		double lowX = ai.getCenterX() - halfWidth;
		double highX = ai.getCenterX() + halfWidth;
		double halfHeight = ai.getYSize() / 2;
		double lowY = ai.getCenterY() - halfHeight;
		double highY = ai.getCenterY() + halfHeight;
		
		// construct the polygons
		Poly [] polys = new Poly[primLayers.length];
		for(int i = 0; i < primLayers.length; i++)
		{
			Technology.ArcLayer primLayer = primLayers[i];
			polys[i] = ai.makearcpoly(ai.getXSize(), ai.getWidth() - primLayer.getOffset(), primLayer.getStyle());
			if (polys[i] == null) return null;
			polys[i].setLayer(primLayer.getLayer());
		}
		return polys;
	}

	/**
	 * Get a polygon that describes port "pp" of node "ni"
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

	public String toString()
	{
		return "Technology " + techName;
	}

	// *************************** ArcProtos ***************************

	/**
	 * get the ArcProto with a particular name from this technology
	 */
	public ArcProto findArcProto(String name)
	{
		for (int i = 0; i < arcs.size(); i++)
		{
			ArcProto ap = (ArcProto) arcs.get(i);
			if (ap.getProtoName().equalsIgnoreCase(name))
				return ap;
		}
		return null;
	}

	/**
	 * get an iterator over all of the ArcProtos in this technology
	 */
	public Iterator getArcIterator()
	{
		return arcs.iterator();
	}

	// *************************** NodeProtos ***************************

	/**
	 * get the PrimitiveNode with a particular name from this technology
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
	 * get an iterator over all of the PrimitiveNodes in this technology
	 */
	public Iterator getNodeIterator()
	{
		return nodes.iterator();
	}

}
