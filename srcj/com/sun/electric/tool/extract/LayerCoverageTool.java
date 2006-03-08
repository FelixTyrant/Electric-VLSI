/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LayerCoverageTool.java
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
package com.sun.electric.tool.extract;

import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.MessagesStream;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.geometry.*;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.Job.Priority;
import com.sun.electric.tool.Tool;

import javax.swing.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Area;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Class to describe coverage percentage for a layer.
 */
public class LayerCoverageTool extends Tool
{
    /** the LayerCoverageTool tool. */		protected static LayerCoverageTool tool = new LayerCoverageTool();
    /**
	 * The constructor sets up the DRC tool.
	 */
	private LayerCoverageTool()
	{
		super("coverage");
	}

    /**
     * Method to retrieve the singleton associated with the LayerCoverageTool tool.
     * @return the LayerCoverageTool tool.
     */
    public static LayerCoverageTool getLayerCoverageTool() { return tool; }

    /****************************** OPTIONS ******************************/

    // Default value is in um to be technology independent
    private static final double defaultSize = 50000;
    private static Pref cacheDeltaX = Pref.makeDoublePref("DeltaX", tool.prefs, defaultSize);
//    static { cacheDeltaX.attachToObject(tool, "Tools/Coverage tab", "Delta along X to sweep bounding box"); }
	/**
	 * Method to get user preference for deltaX.
	 * The default is 50 mm.
	 * @return double representing deltaX
	 */
	public static double getDeltaX(Technology tech)
    {
        return cacheDeltaX.getDouble()/tech.getScale();
    }
	/**
	 * Method to set user preference for deltaX.
	 * @param delta double representing new deltaX
	 */
	public static void setDeltaX(double delta, Technology tech)
    {
        cacheDeltaX.setDouble(delta*tech.getScale());
    }

    private static Pref cacheDeltaY = Pref.makeDoublePref("DeltaY", tool.prefs, defaultSize);
//    static { cacheDeltaY.attachToObject(tool, "Tools/Coverage tab", "Delta along Y to sweep bounding box"); }
	/**
	 * Method to get user preference for deltaY.
	 * The default is 50 mm.
	 * @return double representing deltaY
	 */
	public static double getDeltaY(Technology tech)
    {
        return cacheDeltaY.getDouble()/tech.getScale();
    }
	/**
	 * Method to set user preference for deltaY.
	 * @param delta double representing new deltaY
	 */
	public static void setDeltaY(double delta, Technology tech)
    {
        cacheDeltaY.setDouble(delta*tech.getScale());
    }

    private static Pref cacheWidth = Pref.makeDoublePref("Width", tool.prefs, defaultSize);
//    static { cacheWidth.attachToObject(tool, "Tools/Coverage tab", "Bounding box width"); }
	/**
	 * Method to get user preference for deltaY.
	 * The default is 50 mm.
	 * @return double representing deltaY
	 */
	public static double getWidth(Technology tech)
    {
        return cacheWidth.getDouble()/tech.getScale();
    }
	/**
	 * Method to set user preference for width of the bounding box.
	 * @param w double representing new width
	 */
	public static void setWidth(double w, Technology tech)
    {
        cacheWidth.setDouble(w*tech.getScale());
    }

    private static Pref cacheHeight = Pref.makeDoublePref("Height", tool.prefs, defaultSize);
//    static { cacheHeight.attachToObject(tool, "Tools/Coverage tab", "Bounding box height"); }
	/**
	 * Method to get user preference for deltaY.
	 * The default is 50 mm.
	 * @return double representing deltaY
	 */
	public static double getHeight(Technology tech)
    {
        return cacheHeight.getDouble()/tech.getScale();
    }
	/**
	 * Method to set user preference for height of the bounding box.
	 * @param h double representing new width
	 */
	public static void setHeight(double h, Technology tech)
    {
        cacheHeight.setDouble(h*tech.getScale());
    }

    /**
     * Method to handle the "List Layer Coverage", "Coverage Implant Generator",  polygons merge
     * except "List Geometry on Network" commands.
     */
    public static void layerCoverageCommand(Job.Type jobType, LCMode func, GeometryHandler.GHMode mode,
                                            Cell curCell, Highlighter highlighter)
    {
        LayerCoverageData data = new LayerCoverageData(null, curCell, func, mode, highlighter, null, null);
        Job job = new LayerCoverageJob(data, jobType);
        job.startJob();
    }
    
    /**
     * Method to kick area coverage per layer in a cell. It has to be public due to regressions.
     * @param cell
     * @param mode
     * @param startJob to determine if job has to run in a separate thread
     * @return true if job runs without errors. Only valid if startJob is false (regression purpose)
     */
    public static Map<Layer,Double> layerCoverageCommand(Cell cell, GeometryHandler.GHMode mode, boolean startJob)
    {
        if (cell == null) return null;

        double width = getWidth(cell.getTechnology());
        double height = getHeight(cell.getTechnology());
        double deltaX = getDeltaX(cell.getTechnology());
        double deltaY = getDeltaY(cell.getTechnology());

        // Reset values to cell bounding box if area is bigger than the actual cell
        Rectangle2D bbox = cell.getBounds();
        if (width > bbox.getWidth()) width = bbox.getWidth();
        if (height > bbox.getHeight()) height = bbox.getHeight();
        Map<Layer,Double> map = null;
        AreaCoverageJob job = new AreaCoverageJob(cell, null, mode, width, height,
                deltaX, deltaY);

        // No regression
        if (startJob)
            job.startJob();
        else
        {
        	try
        	{
        		job.doIt();
                map = job.getDataInfo();
        	} catch (JobException e)
        	{
        	}
        }
        return (map);
    }

    /**
     * Method to extract bounding box for a particular Network/Layer
     * @param exportCell
     * @return
     */
    public static Rectangle2D getGeometryOnNetwork(Cell exportCell, PortInst pi, Layer layer)
    {
        Netlist netlist = exportCell.getNetlist(false);
        Network net = netlist.getNetwork(pi);
        HashSet<Network> nets = new HashSet<Network>();
        nets.add(net);
        GeometryOnNetwork geoms = new GeometryOnNetwork(exportCell, nets, 1.0, false);
        LayerCoverageData data = new LayerCoverageData(null, exportCell, LCMode.NETWORK,
                GeometryHandler.GHMode.ALGO_SWEEP, null, geoms, null);
		LayerCoverageJob job = new LayerCoverageJob(data, Job.Type.EXAMINE);
        // Must run it now
        try
        {
            job.doIt();  // Former listGeometryOnNetworksNoJob
        } catch (JobException e)
        {
        }
        Collection<PolyBase> list = data.tree.getObjects(layer, false, true);
        // Don't know what to do if there is more than one
//        if (list.size() != 1)
//        assert(list.size() == 1);
        PolyBase poly = (PolyBase)list.toArray()[0];
        return poly.getBounds2D();
    }

    /**
     * Method to calculate area, half-perimeter and ratio of each layer by merging geometries
     * @param cell cell to analyze
     * @param nets networks to analyze
     * @param startJob if job has to run on thread
     * @param mode geometric algorithm to use: GeometryHandler.ALGO_QTREE, GeometryHandler.SWEEP or GeometryHandler.ALGO_MERGE
     */
    public static GeometryOnNetwork listGeometryOnNetworks(Cell cell, HashSet<Network> nets, boolean startJob,
                                                           GeometryHandler.GHMode mode)
    {
	    if (cell == null || nets == null || nets.isEmpty()) return null;
	    double lambda = 1; // lambdaofcell(np);
        // startJob is identical to printable
	    GeometryOnNetwork geoms = new GeometryOnNetwork(cell, nets, lambda, startJob);
        LayerCoverageData data = new LayerCoverageData(null, cell, LCMode.NETWORK, mode, null, geoms, null);
		Job job = new LayerCoverageJob(data, Job.Type.EXAMINE);

        if (startJob)
            job.startJob();
        else
        {
        	try
        	{
        		job.doIt();  // Former listGeometryOnNetworksNoJob
        	} catch (JobException e)
        	{
        	}
        }
	    return geoms;
	}

    /************************************************************************
     * LayerCoverageData Class
     ************************************************************************/
    private static class LayerCoverageData
    {
        private Cell curCell;
        private Job parentJob; // to stop if parent job is killed
        private GeometryHandler tree;
        private LCMode function;
        private List<NodeInst> deleteList; // Only used for coverage Implants. New coverage implants are pure primitive nodes
        private HashMap<Layer,Set<PolyBase>> originalPolygons = new HashMap<Layer,Set<PolyBase>>(); // Storing initial nodes
        private Highlighter highlighter; // To highlight new implants
        private GeometryOnNetwork geoms;  // Valid only for network job
        private Rectangle2D bBox; // To crop geometry under analysis by given bounding box

        LayerCoverageData(Job parentJob, Cell cell, LCMode func, GeometryHandler.GHMode mode, Highlighter highlighter,
                          LayerCoverageTool.GeometryOnNetwork geoms, Rectangle2D bBox)
        {
            this.parentJob = parentJob;
            this.curCell = cell;
            this.tree = GeometryHandler.createGeometryHandler(mode, curCell.getTechnology().getNumLayers());
            this.function = func;
            this.deleteList = new ArrayList<NodeInst>(); // should only be used by IMPLANT
            this.highlighter = highlighter;
            this.geoms = geoms; // Valid only for network
            this.bBox = bBox;

            if (func == LCMode.AREA && this.geoms == null)
                this.geoms = new GeometryOnNetwork(curCell, null, 1, true);
        }

        boolean doIt()
        {
            // enumerate the hierarchy below here
            LayerVisitor visitor = new LayerVisitor(parentJob, tree, deleteList, function,
                    originalPolygons, (geoms != null) ? (geoms.nets) : null, bBox);
            HierarchyEnumerator.enumerateCell(curCell, VarContext.globalContext, visitor);
            tree.postProcess(true);

            switch (function)
            {
                case MERGE:
                case IMPLANT:
                    {
                        // With polygons collected, new geometries are calculated
                        if (highlighter != null) highlighter.clear();
                        boolean noNewNodes = true;
                        boolean isMerge = (function == LCMode.MERGE);
                        Rectangle2D rect;
                        PolyBase polyB = null;
                        Point2D [] points;

                        // Need to detect if geometry was really modified
                        for(Iterator<Layer> it = tree.getKeyIterator(); it.hasNext(); )
                        {
                            Layer layer = it.next();
                            Collection<PolyBase> set = tree.getObjects(layer, !isMerge, true);
                            Set polySet = (function == LCMode.IMPLANT) ? originalPolygons.get(layer) : null;

                            // Ready to create new implants.
                            for (Iterator<PolyBase> i = set.iterator(); i.hasNext(); )
                            {
                                polyB = i.next();
                                points = polyB.getPoints();
                                rect = polyB.getBounds2D();

                                // One of the original elements
                                if (polySet != null)
                                {
                                    Object[] array = polySet.toArray();
                                    boolean foundOrigPoly = false;
                                    for (int j = 0; j < array.length; j++)
                                    {
                                        foundOrigPoly = polyB.polySame((PolyBase)array[j]);
                                        if (foundOrigPoly)
                                            break;
                                    }
                                    if (foundOrigPoly)
                                        continue;
                                }

                                Point2D center = new Point2D.Double(rect.getCenterX(), rect.getCenterY());
                                PrimitiveNode priNode = layer.getPureLayerNode();
                                // Adding the new implant. New implant not assigned to any local variable                                .
                                NodeInst node = NodeInst.makeInstance(priNode, center, rect.getWidth(), rect.getHeight(), curCell);
                                if (highlighter != null)
                                    highlighter.addElectricObject(node, curCell);

                                if (isMerge)
                                {
                                    EPoint [] ePoints = new EPoint[points.length];
                                    for(int j=0; j<points.length; j++)
                                        ePoints[j] = new EPoint(points[j].getX(), points[j].getY());
                                    node.newVar(NodeInst.TRACE, ePoints);
                                }
                                else
                                {
                                    // New implant can't be selected again
                                    node.setHardSelect();
                                }
                                noNewNodes = false;
                            }
                        }
                        if (highlighter != null) highlighter.finished();
                        for (NodeInst node : deleteList)
                        {
                            node.kill();
                        }
                        if (noNewNodes)
                            System.out.println("No new areas added");
                    }
                    break;
                case AREA:
                case NETWORK:
                    {
                        double lambdaSqr = 1; // lambdaofcell(np);
                        Rectangle2D bbox = curCell.getBounds();
                        double totalArea =  (bbox.getHeight()*bbox.getWidth())/lambdaSqr;
                        // Traversing tree with merged geometry and sorting layers per name first
                        List<Layer> list = new ArrayList<Layer>(tree.getKeySet());
                        Collections.sort(list, Layer.layerSort);

                        for (Layer layer : list)
                        {
                            Collection<PolyBase> set = tree.getObjects(layer, false, true);
                            double layerArea = 0;
                            double perimeter = 0;

                            // Get all objects and sum the area
                            for (PolyBase poly : set)
                            {
                                layerArea += poly.getArea();
                                perimeter += poly.getPerimeter();
                            }
                            layerArea /= lambdaSqr;
                            perimeter /= 2;

                            if (geoms != null)
                                geoms.addLayer(layer, layerArea, perimeter);
                            else
                                System.out.println("Layer " + layer.getName() + " covers " + TextUtils.formatDouble(layerArea)
                                        + " square lambda (" + TextUtils.formatDouble((layerArea/totalArea)*100, 2) + "%)");
                        }
                        geoms.setTotalArea(totalArea);
                        if (geoms != null)
                            geoms.print();
                        else
                            System.out.println("Cell is " + TextUtils.formatDouble(totalArea, 2) + " square lambda");
                    }
                    break;
                default:
                    System.out.println("Error in LayerCoverage: function not implemented");
            }
            return true;
        }
    }

    /************************************************************************
     * LayerCoverageJob Class
     ************************************************************************/
    private static class LayerCoverageJob extends Job
    {
        private LayerCoverageData coverageData;
        public LayerCoverageJob(LayerCoverageData data, Type jobType)
        {
            super("Layer Coverage on " + data.curCell, User.getUserTool(), jobType, null, null, Priority.USER);
            this.coverageData = data;

            setReportExecutionFlag(true);
        }

        public boolean doIt() throws JobException
        {
            return coverageData.doIt();
        }
    }

    /************************************************************************
     * AreaCoverageJob Class
     ************************************************************************/
    private static class AreaCoverageJob extends Job
    {
        private Cell curCell;
        private double deltaX, deltaY;
        private double width, height;
        private Highlighter highlighter;
        private GeometryHandler.GHMode mode;
        private boolean foundError = false;
        private Map<Layer,Double> internalMap;

        public AreaCoverageJob(Cell cell, Highlighter highlighter, GeometryHandler.GHMode mode,
                               double width, double height, double deltaX, double deltaY)
        {
            super("Layer Coverage", User.getUserTool(), Type.EXAMINE, null, null, Priority.USER);
            this.curCell = cell;
            this.highlighter = highlighter;
            this.mode = mode;
            this.width = width;
            this.height = height;
            this.deltaX = deltaX;
            this.deltaY = deltaY;
            setReportExecutionFlag(true); // Want to report statistics
        }

        public boolean doIt() throws JobException
        {
            ErrorLogger errorLogger = ErrorLogger.newInstance("Area Coverage");
            Rectangle2D bBoxOrig = curCell.getBounds();
            double maxY = bBoxOrig.getMaxY();
            double maxX = bBoxOrig.getMaxX();

            // if negative or zero values -> only once
            if (deltaX <= 0) deltaX = bBoxOrig.getWidth();
            if (deltaY <= 0) deltaY = bBoxOrig.getHeight();
            if (width <= 0) width = bBoxOrig.getWidth();
            if (height <= 0) height = bBoxOrig.getHeight();

            internalMap = new HashMap<Layer,Double>();
//            fieldVariableChanged("internalMap");

            for (double posY = bBoxOrig.getMinY(); posY < maxY; posY += deltaY)
            {
                for (double posX = bBoxOrig.getMinX(); posX < maxX; posX += deltaX)
                {
                    Rectangle2D box = new Rectangle2D.Double(posX, posY, width, height);
                    GeometryOnNetwork geoms = new GeometryOnNetwork(curCell, null, 1, true);
                    System.out.println("Calculating Coverage on cell '" + curCell.getName() + "' for area (" +
                            DBMath.round(posX) + "," + DBMath.round(posY) + ") (" +
                            DBMath.round(box.getMaxX()) + "," + DBMath.round(box.getMaxY()) + ")");
                    LayerCoverageData data = new LayerCoverageData(this, curCell, LCMode.AREA, mode, highlighter,
                            geoms, box);
                    if (!data.doIt())  // aborted by user
                    {
                        foundError = true;
                        return false; // didn't finish
                    }
                    if (geoms.analyzeCoverage(box, errorLogger))
                        foundError = true;

                    for (int i = 0; i < geoms.layers.size(); i++)
                    {
                        Layer layer = geoms.layers.get(i);
                        Double area = geoms.areas.get(i);

                        Double oldV = internalMap.get(layer);
                        double newV = area;
                        if (oldV != null)
                            newV += oldV;
                        internalMap.put(layer, new Double(newV));
                    }
                }
            }
            errorLogger.termLogging(true);
            return true;
        }

        public Map<Layer,Double> getDataInfo() { return internalMap; }
    }

    public enum LCMode // LC = LayerCoverageTool mode
    {
	    AREA,   // function Layer Coverage
	    MERGE,  // Generic merge polygons function
	    IMPLANT, // Coverage implants
	    NETWORK; // List Geometry on Network function
    }


    /************************************************************************
     * LayerVisitor Class
     ************************************************************************/
    public static class LayerVisitor extends HierarchyEnumerator.Visitor
	{
        private Job parentJob;
		private GeometryHandler tree;
		private List<NodeInst> deleteList; // Only used for coverage Implants. New coverage implants are pure primitive nodes
		private final LCMode function;
		private HashMap<Layer,Set<PolyBase>> originalPolygons;
		private Set netSet; // For network type, rest is null
        private Rectangle2D origBBox;
        private Area origBBoxArea;   // Area is always in coordinates of top cell

		/**
		 * Determines if function of given layer is applicable for the corresponding operation
		 */
		private static boolean isValidFunction(Layer.Function func, LCMode function)
		{
			switch (function)
			{
                case MERGE:
				case NETWORK:
					return (true);
				case IMPLANT:
					return (func.isSubstrate());
				case AREA:
                    if (Job.LOCALDEBUGFLAG) return (true);
					return (func.isPoly() || func.isMetal());
				default:
					return (false);
			}
		}

		public LayerVisitor(Job job, GeometryHandler t, List<NodeInst> delList, LCMode func, HashMap<Layer,Set<PolyBase>> original, Set netSet, Rectangle2D bBox)
		{
            this.parentJob = job;
			this.tree = t;
			this.deleteList = delList;
			this.function = func;
			this.originalPolygons = original;
			this.netSet = netSet;
            this.origBBox = bBox;
            origBBoxArea = (bBox != null) ? new Area(origBBox) : null;
		}

        /**
         * In case of non null bounding box, it will undo the
         * transformation
         * @param info
         */
		public void exitCell(HierarchyEnumerator.CellInfo info)
        {
        }

        private boolean doesIntersectBoundingBox(Rectangle2D rect, HierarchyEnumerator.CellInfo info)
        {
            // Default case when no bouding box is used to crop the geometry
            if (origBBox == null) return true;

            // only because I need to transform the points.
            PolyBase polyRect = new PolyBase(rect);
            // To avoid transformation while traversing the hierarchy
            polyRect.transform(info.getTransformToRoot());
            rect = polyRect.getBounds2D();
            return rect.intersects(origBBox);
        }

		public boolean enterCell(HierarchyEnumerator.CellInfo info)
		{
            // Checking if job is scheduled for abort or already aborted
	        if (parentJob != null && parentJob.checkAbort()) return (false);

			Cell curCell = info.getCell();
			Netlist netlist = info.getNetlist();

            // Nothing to visit  CAREFUL WITH TRANSFORMATION IN SUBCELL!!
            if (!doesIntersectBoundingBox(curCell.getBounds(), info))
                return false;

			// Checking if any network is found
            boolean found = (netSet == null);
            for (Iterator<Network> it = netlist.getNetworks(); !found && it.hasNext(); )
            {
                Network aNet = it.next();
                Network parentNet = aNet;
                HierarchyEnumerator.CellInfo cinfo = info;
                boolean netFound = false;
                while ((netFound = netSet.contains(parentNet)) == false && cinfo.getParentInst() != null) {
                    parentNet = cinfo.getNetworkInParent(parentNet);
                    cinfo = cinfo.getParentInfo();
                }
                found = netFound;
            }
            if (!found) return (false);

			// Traversing arcs

			for (Iterator<ArcInst> it = curCell.getArcs(); it.hasNext(); )
			{
				ArcInst arc = it.next();
				int width = netlist.getBusWidth(arc);
                found = (netSet == null);

                for (int i=0; !found && i<width; i++)
                {
                    Network parentNet = netlist.getNetwork(arc, i);
                    HierarchyEnumerator.CellInfo cinfo = info;
                    boolean netFound = false;
                    while ((netFound = netSet.contains(parentNet)) == false && cinfo.getParentInst() != null) {
                        parentNet = cinfo.getNetworkInParent(parentNet);
                        cinfo = cinfo.getParentInfo();
                    }
                found = netFound;
                }
                if (!found) continue; // skipping this arc
				ArcProto arcType = arc.getProto();
				Technology tech = arcType.getTechnology();
				Poly[] polyList = tech.getShapeOfArc(arc);

				// Treating the arcs associated to each node
				// Arcs don't need to be rotated
				for (int i = 0; i < polyList.length; i++)
				{
					Poly poly = polyList[i];
					Layer layer = poly.getLayer();
					Layer.Function func = layer.getFunction();

					boolean value = isValidFunction(func, function);
					if (!value) continue;

					poly.transform(info.getTransformToRoot());

                    // If points are not rounded, in IMPLANT map.containsValue() might not work
                    poly.roundPoints();

                    storeOriginalPolygons(layer, poly);

                    Shape pnode = cropGeometry(poly, origBBoxArea);
                    // empty intersection
                    if (pnode == null) continue;

					tree.add(layer, pnode);  // tmp fix
				}
			}
			return (true);
		}

        /**
         * To store original polygons checked by coverage implant run and then
         * able to determine if new implants should be created.
         * @param layer
         * @param poly
         */
        private void storeOriginalPolygons(Layer layer, PolyBase poly)
        {
            if (function != LCMode.IMPLANT) return;
            // For coverage implants
            Set<PolyBase> polySet = originalPolygons.get(layer);
            if (polySet == null)
            {
                polySet = new HashSet<PolyBase>();
                originalPolygons.put(layer, polySet);
            }
            //map.put(pnode, pnode.clone());
            polySet.add(poly);
        }

        /**
         *
         * @param no
         * @param info
         * @return
         */
		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
		{
			//if (checkAbort()) return false;
			NodeInst node = no.getNodeInst();
			boolean found = (netSet == null);

			// Its like pins, facet-center
			if (NodeInst.isSpecialNode(node)) return (false);

            boolean inside = doesIntersectBoundingBox(node.getBounds(), info);

			// Its a cell
            if (node.isCellInstance()) return (inside);

            // Geometry outside contour
            if (!inside) return false;

			for(Iterator<PortInst> pIt = node.getPortInsts(); !found && pIt.hasNext(); )
			{
				PortInst pi = pIt.next();
				PortProto subPP = pi.getPortProto();
				Network oNet = info.getNetlist().getNetwork(node, subPP, 0);
				Network parentNet = oNet;
				HierarchyEnumerator.CellInfo cinfo = info;
				boolean netFound = false;
				while ((netFound = netSet.contains(parentNet)) == false && cinfo.getParentInst() != null) {
					parentNet = cinfo.getNetworkInParent(parentNet);
					cinfo = cinfo.getParentInfo();
				}
				found = netFound;
			}
			if (!found) return (false); // skipping this node

			// Coverage implants are pure primitive nodes
			// and they are ignored.
			if (node.isPrimtiveSubstrateNode()) //node.getFunction() == PrimitiveNode.Function.NODE)
			{
				deleteList.add(node);
				return (false);
			}

			Technology tech = node.getProto().getTechnology();
			Poly[] polyList = tech.getShapeOfNode(node);
			AffineTransform transform = node.rotateOut();

			for (int i = 0; i < polyList.length; i++)
			{
				Poly poly = polyList[i];
				Layer layer = poly.getLayer();
				Layer.Function func = layer.getFunction();

				// Only checking poly or metal for AREA case
				boolean value = isValidFunction(func, function);
				if (!value) continue;

				if (poly.getPoints().length < 3)
				{
					// When is this happening?
					continue;
				}

				poly.transform(transform);
				// Not sure if I need this for general merge polygons function
				poly.transform(info.getTransformToRoot());

                // If points are not rounded, in IMPLANT map.containsValue() might not work
                poly.roundPoints();

                storeOriginalPolygons(layer, poly);

                Shape pnode = cropGeometry(poly, origBBoxArea);
                // empty intersection
                if (pnode == null)
                    continue;

				tree.add(layer, pnode);
			}
			return (true);
		}

        /**
         * Method to crop original polygon by given bounding box. If they
         * don't intersect, returns original shape
         * @param origGeom polygon to crop
         * @param bBoxArea area that defines bounding box
         * @return cropped shape
         */
        private static Shape cropGeometry(Shape origGeom, Area bBoxArea)
        {
            Shape pnode = origGeom;

            // exclude area outside bounding box
            if (bBoxArea != null)
            {
                Area tmpA = new Area(pnode);
                tmpA.intersect(bBoxArea);
                // Empty intersection
                if (tmpA.isEmpty()) return null;
                pnode = tmpA;
            }
            return pnode;
        }
	}

    /**
	 * Class to represent all geometry on a network during layer coverage analysis.
	 */
	public static class GeometryOnNetwork {
	    public final Cell cell;
	    protected Set<Network> nets;
	    private double lambda;
		private boolean printable;

	    // these lists tie together a layer, its area, and its half-perimeter
	    private ArrayList<Layer> layers;
	    private ArrayList<Double> areas;
	    private ArrayList<Double> halfPerimeters;
	    private double totalWire;
        private double totalArea;

	    public GeometryOnNetwork(Cell cell, Set<Network> nets, double lambda, boolean printable) {
	        this.cell = cell;
	        this.nets = nets;
	        this.lambda = lambda;
	        layers = new ArrayList<Layer>();
	        areas = new ArrayList<Double>();
	        halfPerimeters = new ArrayList<Double>();
		    this.printable = printable;
	        totalWire = 0;
            totalArea = 0;
	    }
	    public double getTotalWireLength() { return totalWire; }
        protected void setTotalArea(double area) {totalArea = area; }
	    private void addLayer(Layer layer, double area, double halfperimeter) {
	        layers.add(layer);
	        areas.add(new Double(area));
	        halfPerimeters.add(new Double(halfperimeter));

	        Layer.Function func = layer.getFunction();
	        /* accumulate total wire length on all metal/poly layers */
	        if (func.isPoly() && !func.isGatePoly() || func.isMetal()) {
	            totalWire += halfperimeter;
	        }
	    }

        /**
         * Method to analyze amount of area covered by layer and if meets the minimum
         * specified
         * @param bbox
         * @param errorLogger
         * @return true if no errors are found
         */
        public boolean analyzeCoverage(Rectangle2D bbox, ErrorLogger errorLogger)
        {
            totalArea = (bbox.getHeight()*bbox.getWidth())/(lambda*lambda);
            boolean foundError = false;

            for (int i = 0; i < layers.size(); i++)
            {
                Layer layer = layers.get(i);
                Double area = areas.get(i);
                double percentage = area.doubleValue()/totalArea * 100;
                double minV = layer.getAreaCoverage();
                if (percentage < minV)
                {
                    String msg = "Error area coverage " + layer.getName() + " min value = " + minV + " actual value = " + percentage;
                    PolyBase poly = new PolyBase(bbox);
                    errorLogger.logError(msg, poly, cell, layer.getIndex());
                    foundError = true;
                }
            }
            return foundError;
        }

	    public void print() {
		    // Doesn't print information
		    if (!printable) return;

            // nets is null for mode=AREA
            if (nets != null)
            {
                for(Network net : nets)
                {
                    System.out.println("For " + net + " in " + cell + ":");
                }
            }

	        for (int i=0; i<layers.size(); i++) {
	            Layer layer = layers.get(i);
	            Double area = areas.get(i);
	            Double halfperim = halfPerimeters.get(i);

                double layerArea = area.doubleValue();
	            System.out.println("\tLayer " + layer.getName()
	                    + ":\t area " + TextUtils.formatDouble(layerArea) + "(" + TextUtils.formatDouble((layerArea/totalArea)*100, 2) + "%)"
	                    + "\t half-perimeter " + TextUtils.formatDouble(halfperim.doubleValue())
	                    + "\t ratio " + TextUtils.formatDouble(area.doubleValue()/halfperim.doubleValue()));
	        }
	        if (totalWire > 0)
	            System.out.println("Total wire length = " + TextUtils.formatDouble(totalWire/lambda));
            if (totalArea > 0)
                System.out.println("Total cell area = " + TextUtils.formatDouble(totalArea, 2));
	    }
	}

    /***********************************
     * JUnit interface
     ***********************************/
    public static boolean testAll()
    {
        return (basicAreaCoverageTest("area.log"));
    }

    private static class FakeCoverageCircuitry extends Job
    {
        private String theTechnology;
        private Cell myCell;

        protected FakeCoverageCircuitry(String tech)
        {
            super("Make fake circuitry for coverage tests", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            theTechnology = tech;
            startJob();
        }

        public boolean doIt() throws JobException
        {
            myCell = doItInternal(theTechnology);
			fieldVariableChanged("myCell");
            return true;
        }

        public void terminateOK()
        {
			WindowFrame wf = WindowFrame.getCurrentWindowFrame(false);
			if (wf != null) wf.loadComponentMenuForTechnology();
			// display a cell
            if (!BATCHMODE)
			    WindowFrame.createEditWindow(myCell);
        }

        private static Cell doItInternal(String technology)
		{
			// get information about the nodes
			Technology tech = Technology.findTechnology(technology);
			if (tech == null)
			{
				System.out.println("Technology not found in createCoverageTestCells");
				return null;
			}
			tech.setCurrent();

			NodeProto m1NodeProto = Cell.findNodeProto(technology+":Metal-1-Node");
            NodeProto m2NodeProto = Cell.findNodeProto(technology+":Metal-2-Node");
            NodeProto m3NodeProto = Cell.findNodeProto(technology+":Metal-3-Node");
            NodeProto m4NodeProto = Cell.findNodeProto(technology+":Metal-4-Node");

            NodeProto invisiblePinProto = Cell.findNodeProto("generic:Invisible-Pin");

			// get information about the arcs
			ArcProto m1ArcProto = ArcProto.findArcProto(technology+":Metal-1");

			// get the current library
			Library mainLib = Library.getCurrent();

			// create a layout cell in the library
			Cell m1Cell = Cell.makeInstance(mainLib, technology+"Metal1Test{lay}");
            NodeInst metal1Node = NodeInst.newInstance(m1NodeProto, new Point2D.Double(0, 0), m1NodeProto.getDefWidth(), m1NodeProto.getDefHeight(), m1Cell);

            // Two metals
            Cell myCell = Cell.makeInstance(mainLib, technology+"M1M2Test{lay}");
            NodeInst node = NodeInst.newInstance(m1NodeProto, new Point2D.Double(-m1NodeProto.getDefWidth()/2, -m1NodeProto.getDefHeight()/2),
                    m1NodeProto.getDefWidth(), m1NodeProto.getDefHeight(), myCell);
            node = NodeInst.newInstance(m2NodeProto, new Point2D.Double(-m2NodeProto.getDefWidth()/2, m2NodeProto.getDefHeight()/2),
                    m2NodeProto.getDefWidth(), m2NodeProto.getDefHeight(), myCell);
            node = NodeInst.newInstance(m3NodeProto, new Point2D.Double(m3NodeProto.getDefWidth()/2, -m3NodeProto.getDefHeight()/2),
                    m3NodeProto.getDefWidth(), m3NodeProto.getDefHeight(), myCell);
            node = NodeInst.newInstance(m4NodeProto, new Point2D.Double(m4NodeProto.getDefWidth()/2, m4NodeProto.getDefHeight()/2),
                    m4NodeProto.getDefWidth(), m4NodeProto.getDefHeight(), myCell);

			// now up the hierarchy
			Cell higherCell = Cell.makeInstance(mainLib, "higher{lay}");
			Rectangle2D bounds = myCell.getBounds();
			double myWidth = myCell.getDefWidth();
			double myHeight = myCell.getDefHeight();
            for (int iX = 0; iX < 2; iX++) {
                boolean flipX = iX != 0;
                for (int i = 0; i < 4; i++) {
                    Orientation orient = Orientation.fromJava(i*900, flipX, false);
                    NodeInst instanceNode = NodeInst.newInstance(myCell, new Point2D.Double(i*myWidth, iX*myHeight), myWidth, myHeight, higherCell, orient, null, 0);
                    instanceNode.setExpanded();
                }
            }
			System.out.println("Created " + higherCell);

            return myCell;
		}
    }

    /**
     *
     * @param tech
     * @param asJob
     */
    public static void makeFakeCircuitryForCoverageCommand(String tech, boolean asJob)
	{
		// test code to make and show something
        if (asJob)
        {
            new FakeCoverageCircuitry(tech);
        }
        else
        {
            final Cell myCell = FakeCoverageCircuitry.doItInternal(tech);
            if (!Job.BATCHMODE)
            {
                SwingUtilities.invokeLater(new Runnable() {
	            public void run() { WindowFrame.createEditWindow(myCell); }});
            }
        }
	}

    /**
     * Basic test of area coverage. Function must be public due to regressions.
     * @param logname
     * @return
     */
    public static boolean basicAreaCoverageTest(String logname)
    {
        boolean[] errorCounts = new boolean[2];
        double delta = DBMath.getEpsilon()* DBMath.getEpsilon();
        double wireLength = GenMath.toNearest(163.30159818105273, delta);

        try {
          MessagesStream.getMessagesStream().save(logname);
          String techName = "tsmc90";
          makeFakeCircuitryForCoverageCommand(techName, false);
          Library rootLib = Library.findLibrary("noname");
          Cell cell = rootLib.findNodeProto("higher{lay}");
          Technology curTech = cell.getTechnology();

          // Setting 10% as default value
          for(Iterator<Layer> it = curTech.getLayers(); it.hasNext(); )
          {
            Layer layer = it.next();
            layer.setFactoryAreaCoverageInfo(10);
          }

          System.out.println("------RUNNING MERGE MODE -------------");
          Map<Layer,Double> map = layerCoverageCommand(cell, GeometryHandler.GHMode.ALGO_MERGE, false);
            double area = 0;
            for(Layer layer : map.keySet())
            {
                Double val = map.get(layer);
                if (val != null)
                    area += val;
            }
          errorCounts[0] = map == null;

          System.out.println("------RUNNING SWEEP MODE -------------");
          map = layerCoverageCommand(cell, GeometryHandler.GHMode.ALGO_SWEEP, false);
          errorCounts[1] = map == null;
        } catch (Exception e) {
          System.out.println("exception: "+e);
          e.printStackTrace();
          return false;
        }

        System.out.println("Error results : MERGE=" + errorCounts[0] + " SWEEP=" + errorCounts[1]);
        return(!errorCounts[0] && !errorCounts[1]);
    }
}
