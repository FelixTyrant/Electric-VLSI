/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DRC.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.drc;

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.constraint.Layout;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.DRCRules;
import com.sun.electric.technology.DRCTemplate;
import com.sun.electric.technology.Foundry;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Area;
import java.util.*;
import java.util.prefs.Preferences;

/**
 * This is the Design Rule Checker tool.
 */
public class DRC extends Listener
{
	/** the DRC tool. */								protected static DRC tool = new DRC();
	/** overrides of rules for each technology. */		private static HashMap<Technology,Pref> prefDRCOverride = new HashMap<Technology,Pref>();
	/** map of cells and their objects to DRC */		private static HashMap<Cell,HashSet<Geometric>> cellsToCheck = new HashMap<Cell,HashSet<Geometric>>();
    /** to temporary store DRC dates for spacing checking */                 private static HashMap<Cell,StoreDRCInfo> storedSpacingDRCDate = new HashMap<Cell,StoreDRCInfo>();
    /** to temporary store DRC dates for area checking */                 private static HashMap<Cell,StoreDRCInfo> storedAreaDRCDate = new HashMap<Cell,StoreDRCInfo>();
    /** for logging incremental errors */               private static ErrorLogger errorLoggerIncremental = ErrorLogger.newInstance("DRC (incremental)", true);

    public static Layer.Function.Set getMultiLayersSet(Layer layer)
    {
        Layer.Function.Set thisLayerFunction = (layer.getFunction().isPoly()) ?
        new Layer.Function.Set(Layer.Function.POLY1, Layer.Function.GATE) :
        new Layer.Function.Set(layer.getFunction(), layer.getFunctionExtras());
        return thisLayerFunction;
    }

    /*********************************** QUICK DRC ERROR REPORTING ***********************************/
    public static enum DRCErrorType
    {
	    // the different types of errors
        SPACINGERROR, MINWIDTHERROR, NOTCHERROR, MINSIZEERROR, BADLAYERERROR, LAYERSURROUNDERROR,
        MINAREAERROR, ENCLOSEDAREAERROR, SURROUNDERROR, FORBIDDEN, RESOLUTION, CUTERROR, SLOTSIZEERROR,
	    // Different types of warnings
        ZEROLENGTHARCWARN, TECHMIXWARN
    }

    public static void createDRCErrorLogger(ErrorLogger errorLogger, Map<Cell, Area> exclusionMap,
                                            DRCCheckMode errorTypeSearch, boolean interactiveLogger,
                                            DRCErrorType errorType, String msg,
                                            Cell cell, double limit, double actual, String rule,
                                            PolyBase poly1, Geometric geom1, Layer layer1,
                                            PolyBase poly2, Geometric geom2, Layer layer2)
    {
		if (errorLogger == null) return;

		// if this error is in an ignored area, don't record it
		StringBuffer DRCexclusionMsg = new StringBuffer();
        if (exclusionMap != null && exclusionMap.get(cell) != null)
		{
			// determine the bounding box of the error
			List<PolyBase> polyList = new ArrayList<PolyBase>(2);
			List<Geometric> geomList = new ArrayList<Geometric>(2);
			polyList.add(poly1); geomList.add(geom1);
			if (poly2 != null)
			{
				polyList.add(poly2);
				geomList.add(geom2);
			}
            boolean found = checkExclusionMap(exclusionMap, cell, polyList, geomList, DRCexclusionMsg);

            // At least one DRC exclusion that contains both
            if (found) return;
		}

		// describe the error
		Cell np1 = (geom1 != null) ? geom1.getParent() : null;
		Cell np2 = (geom2 != null) ? geom2.getParent() : null;

		// Message already logged
        boolean onlyWarning = (errorType == DRCErrorType.ZEROLENGTHARCWARN || errorType == DRCErrorType.TECHMIXWARN);
        // Until a decent algorithm is in place for detecting repeated errors, ERROR_CHECK_EXHAUSTIVE might report duplicate errros
		if ( geom2 != null && errorTypeSearch != DRCCheckMode.ERROR_CHECK_EXHAUSTIVE && errorLogger.findMessage(cell, geom1, geom2.getParent(), geom2, !onlyWarning))
            return;

		StringBuffer errorMessage = new StringBuffer();
        DRCCheckLogging loggingType = getErrorLoggingType();

        int sortKey = cell.hashCode(); // 0;
		if (errorType == DRCErrorType.SPACINGERROR || errorType == DRCErrorType.NOTCHERROR || errorType == DRCErrorType.SURROUNDERROR)
		{
			// describe spacing width error
			if (errorType == DRCErrorType.SPACINGERROR)
				errorMessage.append("Spacing");
			else if (errorType == DRCErrorType.SURROUNDERROR)
				errorMessage.append("Surround");
			else
				errorMessage.append("Notch");
			if (layer1 == layer2)
				errorMessage.append(" (layer '" + layer1.getName() + "')");
			errorMessage.append(": ");

			if (np1 != np2)
			{
				errorMessage.append(np1 + ", ");
			} else if (np1 != cell && np1 != null)
			{
				errorMessage.append("[in " + np1 + "] ");
			}

            if (geom1 != null)
                errorMessage.append(geom1);
			if (layer1 != layer2)
				errorMessage.append(", layer '" + layer1.getName() + "'");

			if (actual < 0) errorMessage.append(" OVERLAPS (BY " + TextUtils.formatDouble(limit-actual) + ") ");
			else if (actual == 0) errorMessage.append(" TOUCHES ");
			else errorMessage.append(" LESS (BY " + TextUtils.formatDouble(limit-actual) + ") THAN " + TextUtils.formatDouble(limit) +
                    ((geom2!=null)?" TO ":""));

			if (np1 != np2 && np2 != null)
				errorMessage.append(np2 + ", ");

            if (geom2 != null)
                errorMessage.append(geom2);
			if (layer1 != layer2)
				errorMessage.append(", layer '" + layer2.getName() + "'");
			if (msg != null)
				errorMessage.append("; " + msg);
		} else
		{
			// describe minimum width/size or layer error
			StringBuffer errorMessagePart2 = null;
			switch (errorType)
			{
                case RESOLUTION:
                    errorMessage.append("Resolution error:");
					errorMessagePart2 = new StringBuffer(msg);
                    break;
                case FORBIDDEN:
                    errorMessage.append("Forbidden error:");
					errorMessagePart2 = new StringBuffer(msg);
                    break;
                case SLOTSIZEERROR:
                    errorMessage.append("Slot size error:");
					errorMessagePart2 = new StringBuffer(", layer '" + layer1.getName() + "'");
					errorMessagePart2.append(" BIGGER THAN " + TextUtils.formatDouble(limit) + " IN LENGTH (IS " + TextUtils.formatDouble(actual) + ")");
                    break;
				case MINAREAERROR:
					errorMessage.append("Minimum area error:");
					errorMessagePart2 = new StringBuffer(", layer '" + layer1.getName() + "'");
					errorMessagePart2.append(" LESS THAN " + TextUtils.formatDouble(limit) + " IN AREA (IS " + TextUtils.formatDouble(actual) + ")");
					break;
				case ENCLOSEDAREAERROR:
					errorMessage.append("Enclosed area error:");
					errorMessagePart2 = new StringBuffer(", layer '" + layer1.getName() + "'");
					errorMessagePart2.append(" LESS THAN " + TextUtils.formatDouble(limit) + " IN AREA (IS " + TextUtils.formatDouble(actual) + ")");
					break;
				case TECHMIXWARN:
					errorMessage.append("Technology mixture warning:");
					errorMessagePart2 = new StringBuffer(msg);
					break;
				case ZEROLENGTHARCWARN:
					errorMessage.append("Zero width warning:");
					errorMessagePart2 = new StringBuffer(msg); break;
				case CUTERROR:
                    errorMessage.append("Maximum cut error" + ((msg != null) ? ("(" + msg + "):") : ""));
                    errorMessagePart2 = new StringBuffer(", layer '" + layer1.getName() + "'");
                    errorMessagePart2.append(" BIGGER THAN " + TextUtils.formatDouble(limit) + " WIDE (IS " + TextUtils.formatDouble(actual) + ")");
					break;
				case MINWIDTHERROR:
                    errorMessage.append("Minimum width/height error" + ((msg != null) ? ("(" + msg + "):") : ""));
					errorMessagePart2 = new StringBuffer(", layer '" + layer1.getName() + "'");
					errorMessagePart2.append(" LESS THAN " + TextUtils.formatDouble(limit) + " WIDE (IS " + TextUtils.formatDouble(actual) + ")");
                    break;
				case MINSIZEERROR:
					errorMessage.append("Minimum size error on " + msg + ":");
					errorMessagePart2 = new StringBuffer(" LESS THAN " + TextUtils.formatDouble(limit) + " IN SIZE (IS " + TextUtils.formatDouble(actual) + ")");
					break;
				case BADLAYERERROR:
					errorMessage.append("Invalid layer ('" + layer1.getName() + "'):");
					break;
				case LAYERSURROUNDERROR:
					errorMessage.append("Layer surround error: " + msg);
					errorMessagePart2 = new StringBuffer(", layer '" + layer1.getName() + "'");
                    String layerName = (layer2 != null) ? layer2.getName() : "Select";
					errorMessagePart2.append(" NEEDS SURROUND OF LAYER '" + layerName + "' BY " + limit);
                    break;
			}

			errorMessage.append(" " + cell + " ");
			if (geom1 != null)
			{
				errorMessage.append(geom1);
			}
            // only when is flat -> use layer index for sorting
            if (layer1 != null && loggingType == DRCCheckLogging.DRC_LOG_FLAT) sortKey = layer1.getIndex();
			errorMessage.append(errorMessagePart2);
		}
		if (rule != null && rule.length() > 0) errorMessage.append(" [rule '" + rule + "']");
		errorMessage.append(DRCexclusionMsg);

		List<Geometric> geomList = new ArrayList<Geometric>();
		List<PolyBase> polyList = new ArrayList<PolyBase>();
		if (poly1 != null) polyList.add(poly1); else
			if (geom1 != null) geomList.add(geom1);
		if (poly2 != null) polyList.add(poly2); else
			if (geom2 != null) geomList.add(geom2);

        switch (loggingType)
        {
            case DRC_LOG_PER_CELL:
                errorLogger.setGroupName(sortKey, cell.getName());
                break;
            case DRC_LOG_PER_RULE:
                String theRuleName = rule;
                if (theRuleName == null)
                    theRuleName = errorType.name();
                sortKey = theRuleName.hashCode();
                if (errorLogger.getGroupName(sortKey) == null) // only if nothing was found
                    errorLogger.setGroupName(sortKey, theRuleName);
                break;
        }

        if (onlyWarning)
            errorLogger.logWarning(errorMessage.toString(), geomList, null, null, null, polyList, cell, sortKey);
        else
		    errorLogger.logError(errorMessage.toString(), geomList, null, null, null, polyList, cell, sortKey);
        // Temporary display of errors.
        if (interactiveLogger)
            Job.getUserInterface().termLogging(errorLogger, false, false);
	}

    private static boolean checkExclusionMap(Map<Cell, Area> exclusionMap, Cell cell, List<PolyBase> polyList,
                                             List<Geometric> geomList, StringBuffer DRCexclusionMsg) {
        Area area = exclusionMap.get(cell);
        if (area == null) return false;

        int count = 0, i = -1;

        for (PolyBase thisPoly : polyList) {
            i++;
            if (thisPoly == null)
                continue; // MinNode case
            boolean found = area.contains(thisPoly.getBounds2D());

            if (found) count++;
            else {
                Rectangle2D rect = (geomList.get(i) != null) ? geomList.get(i).getBounds() : thisPoly.getBounds2D();
                DRCexclusionMsg.append("\n\t(DRC Exclusion in '" + cell.getName() + "' does not completely contain element (" +
                        rect.getMinX() + "," + rect.getMinY() + ") (" + rect.getMaxX() + "," + rect.getMaxY() + "))");
            }
        }
// At least one DRC exclusion that contains both
//        if (count == polyList.size())
        if (count >= 1) // at one element is inside the DRC exclusion
            return true;
        return false;
    }

    /*********************************** END DRC ERROR REPORTING ***********************************/

    private static class StoreDRCInfo
    {
        long date;
        int bits;
        StoreDRCInfo(long d, int b)
        {
            date = d;
            bits = b;
        }
    }

    private static boolean incrementalRunning = false;
    /** key of Variable for last valid DRC date on a Cell. Only area rules */
//    private static final int DRC_BIT_AREA = 01; /* Min area condition */
    private static final int DRC_BIT_EXTENSION = 02;   /* Coverage DRC condition */
    private static final int DRC_BIT_ST_FOUNDRY = 04; /* For ST foundry selection */
    private static final int DRC_BIT_TSMC_FOUNDRY = 010; /* For TSMC foundry selection */
    private static final int DRC_BIT_MOSIS_FOUNDRY = 020; /* For Mosis foundry selection */

    public enum DRCCheckMinArea
    {
        AREA_BASIC("Simple") /*brute force algorithm*/, AREA_LOCAL("Local");
        private final String name;
        DRCCheckMinArea(String s)
        {
            name = s;
        }
        public String toString() {return name;}
    }

    public enum DRCCheckLogging
    {
        DRC_LOG_FLAT("Flat")/*original strategy*/, DRC_LOG_PER_CELL("By Cell"), DRC_LOG_PER_RULE("By Rule");
        private final String name;
        DRCCheckLogging(String s)
        {
            name = s;
        }
        public String toString() {return name;}
    }

    /** Control different level of error checking */
    public enum DRCCheckMode
    {
	    ERROR_CHECK_DEFAULT (0),    /** DRC stops after first error between 2 nodes is found (default) */
        ERROR_CHECK_CELL (1),       /** DRC stops after first error per cell is found */
        ERROR_CHECK_EXHAUSTIVE (2);  /** DRC checks all combinations */
        private final int mode;   // mode
        DRCCheckMode(int m) {this.mode = m;}
        public int mode() { return this.mode; }
        public String toString() {return name();}
    }

    /****************************** TOOL CONTROL ******************************/

	/**
	 * The constructor sets up the DRC tool.
	 */
	private DRC()
	{
		super("drc");
	}

    /**
	 * Method to initialize the DRC tool.
	 */
	public void init()
	{
		setOn();
	}

    /**
     * Method to retrieve the singleton associated with the DRC tool.
     * @return the DRC tool.
     */
    public static DRC getDRCTool() { return tool; }

	private static void includeGeometric(Geometric geom)
	{
		if (!isIncrementalDRCOn()) return;
        Cell cell = geom.getParent();

        synchronized (cellsToCheck)
		{
			HashSet<Geometric> cellSet = cellsToCheck.get(cell);
			if (cellSet == null)
			{
				cellSet = new HashSet<Geometric>();
				cellsToCheck.put(cell, cellSet);
			}
			cellSet.add(geom);
		}
    }

//	private static void removeGeometric(Geometric geom)
//	{
//		if (!isIncrementalDRCOn()) return;
//		Cell cell = geom.getParent();
//		synchronized (cellsToCheck)
//		{
//			HashSet<Geometric> cellSet = cellsToCheck.get(cell);
//			if (cellSet != null) cellSet.remove(geom);
//		}
//	}

	private static void doIncrementalDRCTask()
	{
		if (!isIncrementalDRCOn()) return;
		if (incrementalRunning) return;

		Library curLib = Library.getCurrent();
		if (curLib == null) return;
		Cell cellToCheck = Job.getUserInterface().getCurrentCell(curLib);
		HashSet<Geometric> cellSet = null;

		// get a cell to check
		synchronized (cellsToCheck)
		{
			if (cellToCheck != null)
				cellSet = cellsToCheck.get(cellToCheck);
			if (cellSet == null && cellsToCheck.size() > 0)
			{
				cellToCheck = cellsToCheck.keySet().iterator().next();
				cellSet = cellsToCheck.get(cellToCheck);
			}
			if (cellSet != null)
				cellsToCheck.remove(cellToCheck);
		}

		if (cellToCheck == null) return; // nothing to do

		// don't check if cell not in database anymore
		if (!cellToCheck.isLinked()) return;
		// Handling clipboard case (one type of hidden libraries)
		if (cellToCheck.getLibrary().isHidden()) return;

		// if there is a cell to check, do it
		if (cellSet != null)
		{
			Geometric [] objectsToCheck = new Geometric[cellSet.size()];
			int i = 0;
            for(Geometric geom : cellSet)
				objectsToCheck[i++] = geom;

            // cleaning previous errors on the cells to check now.
            for (Geometric geo : objectsToCheck)
            {
                Cell c = geo.getParent();
                ArrayList<ErrorLogger.MessageLog> getAllLogs = errorLoggerIncremental.getAllLogs(c);
                Job.updateIncrementalDRCErrors(c, null, getAllLogs);
            }
            new CheckDRCIncrementally(cellToCheck, objectsToCheck, cellToCheck.getTechnology().isScaleRelevant());
		}
	}

   /**
     * Handles database changes of a Job.
     * @param oldSnapshot database snapshot before Job.
     * @param newSnapshot database snapshot after Job and constraint propagation.
     * @param undoRedo true if Job was Undo/Redo job.
     */
    public void endBatch(Snapshot oldSnapshot, Snapshot newSnapshot, boolean undoRedo)
	{
        for (CellId cellId: newSnapshot.getChangedCells(oldSnapshot)) {
            Cell cell = Cell.inCurrentThread(cellId);
            if (cell == null) continue;
            CellBackup oldBackup = oldSnapshot.getCell(cellId);
            for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); ) {
                NodeInst ni = it.next();
                ImmutableNodeInst d = ni.getD();
                if (oldBackup == null || oldBackup.cellRevision.getNode(d.nodeId) != d)
                    includeGeometric(ni);
            }
            for (Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); ) {
                ArcInst ai = it.next();
                ImmutableArcInst d = ai.getD();
                if (oldBackup == null || oldBackup.cellRevision.getArc(d.arcId) != d)
                    includeGeometric(ai);
            }
        }
		doIncrementalDRCTask();
	}

//	/**
//	 * Method to announce a change to a NodeInst.
//	 * @param ni the NodeInst that was changed.
//	 * @param oD the old contents of the NodeInst.
//	 */
//	public void modifyNodeInst(NodeInst ni, ImmutableNodeInst oD)
//	{
//		includeGeometric(ni);
//	}
//
//	/**
//	 * Method to announce a change to an ArcInst.
//	 * @param ai the ArcInst that changed.
//     * @param oD the old contents of the ArcInst.
//	 */
//	public void modifyArcInst(ArcInst ai, ImmutableArcInst oD)
//	{
//		includeGeometric(ai);
//	}
//
//	/**
//	 * Method to announce the creation of a new ElectricObject.
//	 * @param obj the ElectricObject that was just created.
//	 */
//	public void newObject(ElectricObject obj)
//	{
//		if (obj instanceof Geometric)
//		{
//			includeGeometric((Geometric)obj);
//		}
//	}
//
//	/**
//	 * Method to announce the deletion of an ElectricObject.
//	 * @param obj the ElectricObject that was just deleted.
//	 */
//	public void killObject(ElectricObject obj)
//	{
//		if (obj instanceof Geometric)
//		{
//			removeGeometric((Geometric)obj);
//		}
//	}

	/****************************** DRC INTERFACE ******************************/
    public static ErrorLogger getDRCErrorLogger(boolean layout, boolean incremental, String extraMsg)
    {
        ErrorLogger errorLogger = null;
        String title = (layout) ? "Layout " : "Schematic ";

        if (incremental)
        {
//            if (errorLoggerIncremental == null) errorLoggerIncremental = ErrorLogger.newInstance("DRC (incremental)"/*, true*/);
            errorLogger = errorLoggerIncremental;
        }
        else
        {
            errorLogger = ErrorLogger.newInstance(title + "DRC (full)" + ((extraMsg != null) ? extraMsg:""));
        }
        return errorLogger;
    }

    public static ErrorLogger getDRCIncrementalLogger() {return errorLoggerIncremental;}

    /**
     * This method generates a DRC job from the GUI or for a bash script.
     */
    public static void checkDRCHierarchically(Cell cell, Rectangle2D bounds, GeometryHandler.GHMode mode, boolean onlyArea)
    {
        if (cell == null) return;
        boolean isLayout = true; // hierarchical check of layout by default
		if (cell.isSchematic() || cell.getTechnology() == Schematics.tech() ||
			cell.isIcon() || cell.getTechnology() == Artwork.tech())
			// hierarchical check of schematics
			isLayout = false;

        if (mode == null) mode = GeometryHandler.GHMode.ALGO_SWEEP;
        new CheckDRCHierarchically(cell, isLayout, bounds, mode, onlyArea);
    }

	/**
	 * Base class for checking design rules.
	 *
	 */
	public static class CheckDRCJob extends Job
	{
		Cell cell;
        boolean isLayout; // to check layout

        private static String getJobName(Cell cell) { return "Design-Rule Check " + cell; }
		protected CheckDRCJob(Cell cell, Listener tool, Priority priority, boolean layout)
		{
			super(getJobName(cell), tool, Job.Type.EXAMINE, null, null, priority);
			this.cell = cell;
            this.isLayout = layout;

		}
		// never used
		public boolean doIt() { return (false);}
	}

    /**
     * Class for hierarchical DRC for layout and schematics
     */
	private static class CheckDRCHierarchically extends CheckDRCJob
	{
		Rectangle2D bounds;
        private GeometryHandler.GHMode mergeMode; // to select the merge algorithm
        private boolean onlyArea;

        /**
         * Check bounds within cell. If bounds is null, check entire cell.
         * @param cell
         * @param layout
         * @param bounds
         */
		protected CheckDRCHierarchically(Cell cell, boolean layout, Rectangle2D bounds, GeometryHandler.GHMode mode,
                                         boolean onlyA)
		{
			super(cell, tool, Job.Priority.USER, layout);
			this.bounds = bounds;
            this.mergeMode = mode;
            this.onlyArea = onlyA;
            startJob();
		}

		public boolean doIt()
		{
			long startTime = System.currentTimeMillis();
            ErrorLogger errorLog = getDRCErrorLogger(isLayout, false, null);
            checkNetworks(errorLog, cell, isLayout);
            if (isLayout)
                Quick.checkDesignRules(errorLog, cell, null, null, bounds, this, mergeMode, onlyArea);
            else
                Schematic.doCheck(errorLog, cell, null);
            long endTime = System.currentTimeMillis();
            int errorCount = errorLog.getNumErrors();
            int warnCount = errorLog.getNumWarnings();
            System.out.println(errorCount + " errors and " + warnCount + " warnings found (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
            if (onlyArea)
                Job.getUserInterface().termLogging(errorLog, false, false); // otherwise the errors don't appear
            return true;
		}
	}

	private static class CheckDRCIncrementally extends CheckDRCJob
	{
		Geometric [] objectsToCheck;

		protected CheckDRCIncrementally(Cell cell, Geometric[] objectsToCheck, boolean layout)
		{
			super(cell, tool, Job.Priority.ANALYSIS, layout);
			this.objectsToCheck = objectsToCheck;
			startJob();
		}

		public boolean doIt()
		{
			incrementalRunning = true;
            ErrorLogger errorLog = getDRCErrorLogger(isLayout, true, null);
            if (isLayout)
                errorLog = Quick.checkDesignRules(errorLog, cell, objectsToCheck, null, null);
            else
                errorLog = Schematic.doCheck(errorLog, cell, objectsToCheck);
            int errorsFound = errorLog.getNumErrors();
			if (errorsFound > 0)
				System.out.println("Incremental DRC found " + errorsFound + " errors/warnings in "+ cell);
			incrementalRunning = false;
			doIncrementalDRCTask();
			return true;
		}
	}

	/****************************** DESIGN RULE CONTROL ******************************/

	/** The Technology whose rules are cached. */		private static Technology currentTechnology = null;

	/**
	 * Method to build a Rules object that contains the current design rules for a Technology.
	 * The DRC dialogs use this to hold the values while editing them.
	 * It also provides a cache for the design rule checker.
	 * @param tech the Technology to examine.
	 * @return a new Rules object with the design rules for the given Technology.
	 */
	public static DRCRules getRules(Technology tech)
	{
        DRCRules currentRules = tech.getCachedRules();
		if (currentRules != null && tech == currentTechnology) return currentRules;

		// constructing design rules: start with factory rules
		currentRules = tech.getFactoryDesignRules();
		if (currentRules != null)
		{
			// add overrides
			StringBuffer override = getDRCOverrides(tech);
			currentRules.applyDRCOverrides(override.toString(), tech);
		}

		// remember technology whose rules are cached
		currentTechnology = tech;
        tech.setCachedRules(currentRules);
		return currentRules;
	}

	/**
	 * Method to load a full set of design rules for a Technology.
	 * @param tech the Technology to load.
	 * @param newRules a complete design rules object.
	 */
	public static void setRules(Technology tech, DRCRules newRules)
	{
		// get factory design rules
		DRCRules factoryRules = tech.getFactoryDesignRules();

		// determine override differences from the factory rules
		StringBuffer changes = Technology.getRuleDifferences(factoryRules, newRules);

        if (Job.LOCALDEBUGFLAG)
            System.out.println("This function needs attention");

		// get current overrides of factory rules
		StringBuffer override = getDRCOverrides(tech);

		// if the differences are the same as before, stop
		if (changes.toString().equals(override.toString())) return;

		// update the preference for the rule overrides
		setDRCOverrides(changes, tech);

		// update variables on the technology
		tech.setRuleVariables(newRules);

		// flush the cache of rules
		if (currentTechnology == tech) currentTechnology = null;
	}

	/****************************** INDIVIDUAL DESIGN RULES ******************************/

	/**
	 * Method to find the worst spacing distance in the design rules.
	 * Finds the largest spacing rule in the Technology.
	 * @param tech the Technology to examine.
     * @param lastMetal
     * @return the largest spacing distance in the Technology. Zero if nothing found
	 */
	public static double getWorstSpacingDistance(Technology tech, int lastMetal)
	{
		DRCRules rules = getRules(tech);
		if (rules == null)
            return 0;
		return (rules.getWorstSpacingDistance(lastMetal));
	}

    /**
	 * Method to find the maximum design-rule distance around a layer.
	 * @param layer the Layer to examine.
	 * @return the maximum design-rule distance around the layer. -1 if nothing found.
	 */
	public static double getMaxSurround(Layer layer, double maxSize)
	{
		Technology tech = layer.getTechnology();
        if (tech == null) return -1; // case when layer is a Graphics
		DRCRules rules = getRules(tech);
		if (rules == null) return -1;

        return (rules.getMaxSurround(layer, maxSize));
	}

	/**
	 * Method to find the edge spacing rule between two layer.
	 * @param layer1 the first layer.
	 * @param layer2 the second layer.
	 * @return the edge rule distance between the layers.
	 * Returns null if there is no edge spacing rule.
	 */
	public static DRCTemplate getEdgeRule(Layer layer1, Layer layer2)
	{
		Technology tech = layer1.getTechnology();
		DRCRules rules = getRules(tech);
		if (rules == null) return null;

		return (rules.getEdgeRule(layer1, layer2));
	}

	/**
	 * Method to find the spacing rule between two layer.
	 * @param layer1 the first layer.
     * @param geo1
	 * @param layer2 the second layer.
     * @param geo2
	 * @param connected true to find the distance when the layers are connected.
	 * @param multiCut true to find the distance when this is part of a multicut contact.
     * @param wideS widest polygon
     * @param length length of the intersection
	 * @return the spacing rule between the layers.
	 * Returns null if there is no spacing rule.
	 */
	public static DRCTemplate getSpacingRule(Layer layer1, Geometric geo1, Layer layer2, Geometric geo2,
                                             boolean connected, int multiCut, double wideS, double length)
	{
		Technology tech = layer1.getTechnology();
		DRCRules rules = getRules(tech);
		if (rules == null) return null;
        return (rules.getSpacingRule(layer1, geo1, layer2, geo2, connected, multiCut, wideS, length));
	}

    /**
     * Method to find all possible rules of DRCRuleType type associated a layer.
     * @param layer1 the layer whose rules are desired.
     * @return a list of DRCTemplate objects associated with the layer.
     */
    public static List<DRCTemplate> getRules(Layer layer1, DRCTemplate.DRCRuleType type)
    {
        Technology tech = layer1.getTechnology();
        DRCRules rules = getRules(tech);
		if (rules == null)
            return null;
		return (rules.getRules(layer1, type));
    }

    /**
	 * Method to find the extension rule between two layer.
	 * @param layer1 the first layer.
	 * @param layer2 the second layer.
     * @param isGateExtension to decide between the rule EXTENSIONGATE or EXTENSION
	 * @return the extension rule between the layers.
	 * Returns null if there is no extension rule.
	 */
	public static DRCTemplate getExtensionRule(Layer layer1, Layer layer2, boolean isGateExtension)
	{
		Technology tech = layer1.getTechnology();
		DRCRules rules = getRules(tech);
		if (rules == null) return null;
        return (rules.getExtensionRule(layer1, layer2, isGateExtension));
	}

	/**
	 * Method to tell whether there are any design rules between two layers.
	 * @param layer1 the first Layer to check.
	 * @param layer2 the second Layer to check.
	 * @return true if there are design rules between the layers.
	 */
	public static boolean isAnySpacingRule(Layer layer1, Layer layer2)
	{
		Technology tech = layer1.getTechnology();
		DRCRules rules = getRules(tech);
		if (rules == null) return false;
        return (rules.isAnySpacingRule(layer1, layer2));
	}

	/**
	 * Method to get the minimum <type> rule for a Layer
	 * where <type> is the rule type. E.g. MinWidth or Area
	 * @param layer the Layer to examine.
	 * @param type rule type
	 * @return the minimum width rule for the layer.
	 * Returns null if there is no minimum width rule.
	 */
	public static DRCTemplate getMinValue(Layer layer, DRCTemplate.DRCRuleType type)
	{
		Technology tech = layer.getTechnology();
		if (tech == null) return null;
		DRCRules rules = getRules(tech);
		if (rules == null) return null;
        return (rules.getMinValue(layer, type));
	}

    /**
     * Determine if node represented by index in DRC mapping table is forbidden under
     * this foundry.
     */
    public static boolean isForbiddenNode(int index1, int index2, DRCTemplate.DRCRuleType type, Technology tech)
    {
        DRCRules rules = getRules(tech);
        if (rules == null) return false;
        return isForbiddenNode(rules, index1, index2, type);
    }

    public static boolean isForbiddenNode(DRCRules rules, int index1, int index2, DRCTemplate.DRCRuleType type)
    {
        int index = index1; // In case of primitive nodes
        if (index2 != -1 )
            index = rules.getRuleIndex(index1, index2);
        else
            index += rules.getTechnology().getNumLayers(); // Node forbidden
        return (rules.isForbiddenNode(index, type));
    }

    /**
	 * Method to get the minimum size rule for a NodeProto.
	 * @param np the NodeProto to examine.
	 * @return the minimum size rule for the NodeProto.
	 * Returns null if there is no minimum size rule.
	 */
	public static PrimitiveNode.NodeSizeRule getMinSize(NodeProto np)
	{
		if (np instanceof Cell) return null;
		PrimitiveNode pnp = (PrimitiveNode)np;
        return pnp.getMinSizeRule();
	}

	/****************************** SUPPORT FOR DESIGN RULES ******************************/

	/**
	 * Method to get the DRC overrides from the preferences for a given technology.
	 * @param tech the Technology on which to get overrides.
	 * @return a Pref describing DRC overrides for the Technology.
	 */
	private static StringBuffer getDRCOverrides(Technology tech)
	{
		Pref pref = prefDRCOverride.get(tech);
		if (pref == null)
		{
			pref = Pref.makeStringPref("DRCOverridesFor" + tech.getTechName(), tool.prefs, "");
			prefDRCOverride.put(tech, pref);
		}
		StringBuffer sb = new StringBuffer();
		sb.append(pref.getString());
		return sb;
	}

	/**
	 * Method to set the DRC overrides for a given technology.
	 * @param sb the overrides (a StringBuffer).
	 * @param tech the Technology on which to get overrides.
	 */
	private static void setDRCOverrides(StringBuffer sb, Technology tech)
	{
		if (sb.length() >= Preferences.MAX_VALUE_LENGTH)
		{
			System.out.println("Warning: Design rule overrides are too complex to be saved (are " +
				sb.length() + " long which is more than the limit of " + Preferences.MAX_VALUE_LENGTH + ")");
		}
		Pref pref = prefDRCOverride.get(tech);
		if (pref == null)
		{
			pref = Pref.makeStringPref("DRCOverridesFor" + tech.getTechName(), tool.prefs, "");
			prefDRCOverride.put(tech, pref);
		}
		pref.setString(sb.toString());
	}

    /**
     * Method to clean those cells that were marked with a valid date due to
     * changes in the DRC rules.
     * @param f
     */
    public static void cleanCellsDueToFoundryChanges(Technology tech, Foundry f)
    {
        // Need to clean cells using this foundry because the rules might have changed.
        System.out.println("Cleaning good DRC dates in cells using '" + f.getType().name() +
                "' in '" + tech.getTechName() + "'");
        HashSet<Cell> cleanSpacingDRCDate = new HashSet<Cell>();
        HashSet<Cell> cleanAreaDRCDate = new HashSet<Cell>();

        int bit = 0;
        switch(f.getType())
        {
            case MOSIS:
                bit = DRC_BIT_MOSIS_FOUNDRY;
                break;
            case TSMC:
                bit = DRC_BIT_TSMC_FOUNDRY;
                break;
            case ST:
                bit = DRC_BIT_ST_FOUNDRY;
                break;
        }

        boolean inMemory = isDatesStoredInMemory();

        for (Iterator<Library> it = Library.getLibraries(); it.hasNext();)
        {
            Library lib = it.next();
            for (Iterator<Cell> itC = lib.getCells(); itC.hasNext();)
            {
                Cell cell = itC.next();
                if (cell.getTechnology() != tech) continue;

                StoreDRCInfo data = getCellGoodDRCDateAndBits(cell, true, !inMemory);

                if (data != null) // there is data
                {
                    // It was marked as valid with previous set of rules
                    if ((data.bits & bit) != 0)
                        cleanSpacingDRCDate.add(cell);
                }

                // Checking area bit
                data = getCellGoodDRCDateAndBits(cell, false, !inMemory);

                if (data != null) // there is data
                {
                    // It was marked as valid with previous set of rules
                    if ((data.bits & bit) != 0)
                        cleanAreaDRCDate.add(cell);
                }
            }
        }
        addDRCUpdate(0, null, cleanSpacingDRCDate, null, cleanAreaDRCDate, null);
    }

    /**
     * Method to retrieve Date in miliseconds if a valid date for the given Key is found
     * @param cell
     * @param key
     * @return true if a valid date is found
     */
    private static boolean getDateStored(Cell cell, Variable.Key key, GenMath.MutableLong date)
    {
        long lastDRCDateInMilliseconds;
        // disk version
        Long lastDRCDateAsLong = cell.getVarValue(key, Long.class); // new strategy
        if (lastDRCDateAsLong != null) {
            lastDRCDateInMilliseconds = lastDRCDateAsLong.longValue();
        } else {
            Integer[] lastDRCDateAsInts = cell.getVarValue(key, Integer[].class);
            if (lastDRCDateAsInts == null) return false;
            long lastDRCDateInSecondsHigh = lastDRCDateAsInts[0].intValue();
            long lastDRCDateInSecondsLow = lastDRCDateAsInts[1].intValue();
            lastDRCDateInMilliseconds = (lastDRCDateInSecondsHigh << 32) | (lastDRCDateInSecondsLow & 0xFFFFFFFFL);
        }
//        Variable varSpacingDate = cell.getVar(key, Long.class); // new strategy
//        if (varSpacingDate == null)
//            varSpacingDate = cell.getVar(key, Integer[].class);
//        if (varSpacingDate == null) return false;
//        Object lastDRCDateObject = varSpacingDate.getObject();
//        if (lastDRCDateObject instanceof Integer[]) {
//            Integer[] lastDRCDateAsInts = (Integer [])lastDRCDateObject;
//            long lastDRCDateInSecondsHigh = lastDRCDateAsInts[0].intValue();
//            long lastDRCDateInSecondsLow = lastDRCDateAsInts[1].intValue();
//            lastDRCDateInMilliseconds = (lastDRCDateInSecondsHigh << 32) | (lastDRCDateInSecondsLow & 0xFFFFFFFFL);
//        } else {
//            lastDRCDateInMilliseconds = ((Long)lastDRCDateObject).longValue();
//        }
        date.setValue(lastDRCDateInMilliseconds);
        return true;
    }

    /**
     * Method to extract the corresponding DRC bits stored in disk(database) or
     * in memory for a given cell for further analysis
     * @param cell
     * @param fromDisk
     * @return temporary class containing date and bits if available. Null if nothing is found
     */
    private static StoreDRCInfo getCellGoodDRCDateAndBits(Cell cell, boolean spacingCheck, boolean fromDisk)
    {
        HashMap<Cell,StoreDRCInfo> storedDRCDate = storedSpacingDRCDate;
        Variable.Key dateKey = Layout.DRC_LAST_GOOD_DATE_SPACING;
        Variable.Key bitKey = Layout.DRC_LAST_GOOD_BIT_SPACING;

        if (!spacingCheck)
        {
            storedDRCDate = storedAreaDRCDate;
            dateKey = Layout.DRC_LAST_GOOD_DATE_AREA;
            bitKey = null;
        }

        StoreDRCInfo data = storedDRCDate.get(cell);
        boolean firstTime = false;

        if (data == null)
        {
            boolean validVersion = true;
            Version version = cell.getLibrary().getVersion();
            if (version != null) validVersion = version.compareTo(Version.getVersion()) >=0;
            data = new StoreDRCInfo(-1, Layout.DRC_LAST_GOOD_BIT_DEFAULT);
            storedDRCDate.put(cell, data);
            firstTime = true; // to load Variable date from disk in case of inMemory case.
            if (!validVersion)
                return null; // only the first the data is access the version is considered
        }
        if (fromDisk || (!fromDisk && firstTime))
        {
            GenMath.MutableLong lastDRCDateInMilliseconds = new GenMath.MutableLong(0);
            // Nothing found
            if (!getDateStored(cell, dateKey, lastDRCDateInMilliseconds))
                return null;

            int thisByte = Layout.DRC_LAST_GOOD_BIT_DEFAULT;
            if (bitKey != null)
            {
                Integer varBitsAsInt = cell.getVarValue(bitKey, Integer.class);
                if (varBitsAsInt != null) {
                    thisByte = varBitsAsInt.intValue();
                } else {
                    Byte varBitsAsByte = cell.getVarValue(bitKey, Byte.class);
                    if (varBitsAsByte != null)
                        thisByte = varBitsAsByte.byteValue();
                    else
                        System.out.println("No valid bit associated to DRC data was found as cell variable");
                }
//                Variable varBits = cell.getVar(bitKey, Integer.class);
//                if (varBits == null) // old Byte class
//                {
//                    varBits = cell.getVar(bitKey, Byte.class);
//                    if (varBits != null)
//                        thisByte =((Byte)varBits.getObject()).byteValue();
//                    else
//                        System.out.println("No valid bit associated to DRC data was found as cell variable");
//                }
//                else
//                    thisByte = ((Integer)varBits.getObject()).intValue();
            }
            data.bits = thisByte;
            data.date = lastDRCDateInMilliseconds.longValue();
        }
        else
        {
            data = storedDRCDate.get(cell);
        }
        return data;
    }

    /**
     * Method to check if current date is later than cell revision
     * @param cell
     * @param date
     * @return true if DRC date in cell is valid
     */
    public static boolean isCellDRCDateGood(Cell cell, Date date)
    {
        if (date != null)
        {
            Date lastChangeDate = cell.getRevisionDate();
            if (date.after(lastChangeDate)) return true;
        }
        return false;
    }

    /**
     * Method to tell the date of the last successful DRC of a given Cell.
     * @param cell the cell to query.
     * @param fromDisk
     * @return the date of the last successful DRC of that Cell.
     */
    public static Date getLastDRCDateBasedOnBits(Cell cell, boolean spacingCheck,
                                                 int activeBits, boolean fromDisk)
    {
        StoreDRCInfo data = getCellGoodDRCDateAndBits(cell, spacingCheck, fromDisk);

        // if data is null -> nothing found
        if (data == null)
            return null;

        int thisByte = data.bits;
        if (fromDisk && spacingCheck)
            assert(thisByte!=0);
        if (activeBits != Layout.DRC_LAST_GOOD_BIT_DEFAULT)
        {
//            boolean area = (thisByte & DRC_BIT_AREA) == (activeBits & DRC_BIT_AREA);
            boolean extension = (thisByte & DRC_BIT_EXTENSION) == (activeBits & DRC_BIT_EXTENSION);
            // DRC date is invalid if conditions were checked for another foundry
            boolean sameManufacturer = (thisByte & DRC_BIT_TSMC_FOUNDRY) == (activeBits & DRC_BIT_TSMC_FOUNDRY) &&
                    (thisByte & DRC_BIT_ST_FOUNDRY) == (activeBits & DRC_BIT_ST_FOUNDRY) &&
                    (thisByte & DRC_BIT_MOSIS_FOUNDRY) == (activeBits & DRC_BIT_MOSIS_FOUNDRY);
            assert(activeBits != 0);
            if (activeBits != 0 && (/*!area* || */ !extension || !sameManufacturer))
                return null;
        }

        // If in memory, date doesn't matter
        Date revisionDate = cell.getRevisionDate();
        Date lastDRCDate = new Date(data.date);
        return (lastDRCDate.after(revisionDate)) ? lastDRCDate : null;
    }

    /**
     * Method to clean any DRC date stored previously
     * @param cell the cell to clean
     */
    private static void cleanDRCDateAndBits(Cell cell, Variable.Key key)
    {
        if (key == Layout.DRC_LAST_GOOD_DATE_SPACING)
        {
            cell.delVar(Layout.DRC_LAST_GOOD_DATE_SPACING);
            cell.delVar(Layout.DRC_LAST_GOOD_BIT_SPACING);
        }
        else
            cell.delVar(Layout.DRC_LAST_GOOD_DATE_AREA);
    }

    public static String explainBits(int bits)
    {
        boolean on = !isIgnoreAreaChecking(); // (bits & DRC_BIT_AREA) != 0;
        String msg = "area bit ";
        msg += on ? "on" : "off";

        on = (bits & DRC_BIT_EXTENSION) != 0;
        msg += ", extension bit ";
        msg += on ? "on" : "off";

        if ((bits & DRC_BIT_TSMC_FOUNDRY) != 0)
            msg += ", TSMC bit";
        else if ((bits & DRC_BIT_ST_FOUNDRY) != 0)
            msg += ", ST bit";
        else if ((bits & DRC_BIT_MOSIS_FOUNDRY) != 0)
            msg += ", Mosis bit";
        return msg;
    }

    public static int getActiveBits(Technology tech)
    {
        int bits = 0;
//        if (!isIgnoreAreaChecking()) bits |= DRC_BIT_AREA;
        if (!isIgnoreExtensionRuleChecking()) bits |= DRC_BIT_EXTENSION;
        // Adding foundry to bits set
        Foundry foundry = tech.getSelectedFoundry();
        if (foundry != null)
        {
	        switch(foundry.getType())
	        {
	            case MOSIS:
	                bits |= DRC_BIT_MOSIS_FOUNDRY;
	                break;
	            case TSMC:
	                bits |= DRC_BIT_TSMC_FOUNDRY;
	                break;
	            case ST:
	                bits |= DRC_BIT_ST_FOUNDRY;
	                break;
	        }
        }
        return bits;
    }

    /**
     * Check networks rules of this Cell.
     * @param errorLog error logger
     * @param cell cell to check
     * @param true if this is layout cell.
     */
    private static void checkNetworks(ErrorLogger errorLog, Cell cell, boolean isLayout) {
        final int errorSortNetworks = 0;
        final int errorSortNodes = 1;
        HashMap<NodeProto,ArrayList<NodeInst>> strangeNodes = null;
        HashMap<NodeProto,ArrayList<NodeInst>> unconnectedPins = null;
        for (int i = 0, numNodes = cell.getNumNodes(); i < numNodes; i++) {
            NodeInst ni = cell.getNode(i);
            NodeProto np = ni.getProto();
            if (!cell.isIcon()) {
                if (ni.isIconOfParent() ||
                        np.getFunction() == PrimitiveNode.Function.ART && np != Generic.tech().simProbeNode ||
//                        np == Artwork.tech.pinNode ||
                        np == Generic.tech().invisiblePinNode) {
                    if (ni.hasConnections()) {
                        String msg = "Network: " + cell + " has connections on " + ni;
                        System.out.println(msg);
                        errorLog.logError(msg, ni, cell, null, errorSortNodes);
                    }
                } else if (np.getFunction() == PrimitiveNode.Function.PIN &&
                        cell.getTechnology().isLayout() && !ni.hasConnections()) {
                    if (unconnectedPins == null)
                        unconnectedPins = new HashMap<NodeProto,ArrayList<NodeInst>>();
                    ArrayList<NodeInst> pinsOfType = unconnectedPins.get(np);
                    if (pinsOfType == null) {
                        pinsOfType = new ArrayList<NodeInst>();
                        unconnectedPins.put(np, pinsOfType);
                    }
                    pinsOfType.add(ni);
                }
            }
            if (isLayout) {
                if (ni.getNameKey().isBus()) {
                    String msg = "Network: Layout " + cell + " has arrayed " + ni;
                    System.out.println(msg);
                    errorLog.logError(msg, ni, cell, null, errorSortNetworks);
                }
                boolean isSchematicNode;
                if (ni.isCellInstance()) {
                    Cell subCell = (Cell)np;
                    isSchematicNode = subCell.isIcon() || subCell.isSchematic();
                } else {
                    isSchematicNode = np == Generic.tech().universalPinNode ||np.getTechnology() == Schematics.tech();
                }
                if (isSchematicNode) {
                    if (strangeNodes == null)
                        strangeNodes = new HashMap<NodeProto,ArrayList<NodeInst>>();
                    ArrayList<NodeInst> nodesOfType = strangeNodes.get(np);
                    if (nodesOfType == null) {
                        nodesOfType = new ArrayList<NodeInst>();
                        strangeNodes.put(np, nodesOfType);
                    }
                    nodesOfType.add(ni);
                }
            }
        }
        if (unconnectedPins != null) {
            for (NodeProto np : unconnectedPins.keySet()) {
                ArrayList<NodeInst> pinsOfType = unconnectedPins.get(np);
                String msg = "Network: " + cell + " has " + pinsOfType.size() + " unconnected pins " + np;
                System.out.println(msg);
                errorLog.logWarning(msg, Collections.<Geometric>unmodifiableList(pinsOfType),
                        null, null, null, null, cell, errorSortNodes);
            }
        }
        if (strangeNodes != null) {
            for (NodeProto np : strangeNodes.keySet()) {
                ArrayList<NodeInst> nodesOfType = strangeNodes.get(np);
                String msg = "Network: Layout " + cell + " has " + nodesOfType.size() +
                        " " + np.describe(true) + " nodes";
                System.out.println(msg);
                errorLog.logError(msg, Collections.<Geometric>unmodifiableList(nodesOfType), null, cell, errorSortNetworks);
            }
        }
    }

    /****************************** OPTIONS ******************************/

	private static Pref cacheIncrementalDRCOn = Pref.makeBooleanPref("IncrementalDRCOn", tool.prefs, false);
	/**
	 * Method to tell whether DRC should be done incrementally.
	 * The default is "false".
	 * @return true if DRC should be done incrementally.
	 */
	public static boolean isIncrementalDRCOn() { return cacheIncrementalDRCOn.getBoolean(); }
	/**
	 * Method to set whether DRC should be done incrementally.
	 * @param on true if DRC should be done incrementally.
	 */
	public static void setIncrementalDRCOn(boolean on) { cacheIncrementalDRCOn.setBoolean(on); }

	private static Pref cacheInteractiveDRCDragOn = Pref.makeBooleanPref("InteractiveDRCDrag", tool.prefs, true);
	/**
	 * Method to tell whether DRC violations should be shown while nodes and arcs are dragged.
	 * The default is "true".
	 * @return true if DRC violations should be shown while nodes and arcs are dragged.
	 */
	public static boolean isInteractiveDRCDragOn() { return cacheInteractiveDRCDragOn.getBoolean(); }
	/**
	 * Method to set whether DRC violations should be shown while nodes and arcs are dragged.
	 * @param on true if DRC violations should be shown while nodes and arcs are dragged.
	 */
	public static void setInteractiveDRCDragOn(boolean on) { cacheInteractiveDRCDragOn.setBoolean(on); }

    /** Logging Type **/
    private static Pref cacheErrorLoggingType = Pref.makeStringPref("ErrorLoggingType", tool.prefs,
            DRCCheckLogging.DRC_LOG_PER_CELL.name());
	/**
	 * Method to retrieve logging type in DRC
	 * The default is "DRC_LOG_PER_CELL".
	 * @return integer representing error type
	 */
	public static DRCCheckLogging getErrorLoggingType() {return DRCCheckLogging.valueOf(cacheErrorLoggingType.getString());}

	/**
	 * Method to set DRC logging type.
	 * @param type representing error logging mode
	 */
	public static void setErrorLoggingType(DRCCheckLogging type) { cacheErrorLoggingType.setString(type.name()); }

    /** ErrorLevel **/
    private static Pref cacheErrorCheckLevel = Pref.makeIntPref("ErrorCheckLevel", tool.prefs,
            DRCCheckMode.ERROR_CHECK_DEFAULT.mode());
	/**
	 * Method to retrieve checking level in DRC
	 * The default is "ERROR_CHECK_DEFAULT".
	 * @return integer representing error type
	 */
	public static DRCCheckMode getErrorType()
    {
        int val = cacheErrorCheckLevel.getInt();
        for (DRCCheckMode p : DRCCheckMode.values())
        {
            if (p.mode() == val) return p;
        }
        return null;
    }

	/**
	 * Method to set DRC error type.
	 * @param type representing error level
	 */
	public static void setErrorType(DRCCheckMode type) { cacheErrorCheckLevel.setInt(type.mode()); }

	private static Pref cacheIgnoreCenterCuts = Pref.makeBooleanPref("IgnoreCenterCuts", tool.prefs, false);
//    static { cacheIgnoreCenterCuts.attachToObject(tool, "Tools/DRC tab", "DRC ignores center cuts in large contacts"); }
	/**
	 * Method to tell whether DRC should ignore center cuts in large contacts.
	 * Only the perimeter of cuts will be checked.
	 * The default is "false".
	 * @return true if DRC should ignore center cuts in large contacts.
	 */
	public static boolean isIgnoreCenterCuts() { return cacheIgnoreCenterCuts.getBoolean(); }
	/**
	 * Method to set whether DRC should ignore center cuts in large contacts.
	 * Only the perimeter of cuts will be checked.
	 * @param on true if DRC should ignore center cuts in large contacts.
	 */
	public static void setIgnoreCenterCuts(boolean on) { cacheIgnoreCenterCuts.setBoolean(on); }

    private static Pref cacheIgnoreAreaChecking = Pref.makeBooleanPref("IgnoreAreaCheck", tool.prefs, false);
//    static { cacheIgnoreAreaChecking.attachToObject(tool, "Tools/DRC tab", "DRC ignores area checking"); }
	/**
	 * Method to tell whether DRC should ignore minimum/enclosed area checking.
	 * The default is "false".
	 * @return true if DRC should ignore minimum/enclosed area checking.
	 */
	public static boolean isIgnoreAreaChecking() { return cacheIgnoreAreaChecking.getBoolean(); }
	/**
	 * Method to set whether DRC should ignore minimum/enclosed area checking.
	 * @param on true if DRC should ignore minimum/enclosed area checking.
	 */
	public static void setIgnoreAreaChecking(boolean on) { cacheIgnoreAreaChecking.setBoolean(on); }

    private static Pref cacheIgnoreExtensionRuleChecking = Pref.makeBooleanPref("IgnoreExtensionRuleCheck", tool.prefs, false);
//    static { cacheIgnoreExtensionRuleChecking.attachToObject(tool, "Tools/DRC tab", "DRC extension rule checking"); }
	/**
	 * Method to tell whether DRC should check extension rules.
	 * The default is "false".
	 * @return true if DRC should check extension rules.
	 */
	public static boolean isIgnoreExtensionRuleChecking() { return cacheIgnoreExtensionRuleChecking.getBoolean(); }
	/**
	 * Method to set whether DRC should check extension rules.
	 * @param on true if DRC should check extension rules.
	 */
	public static void setIgnoreExtensionRuleChecking(boolean on) { cacheIgnoreExtensionRuleChecking.setBoolean(on); }

    private static Pref cacheStoreDatesInMemory = Pref.makeBooleanPref("StoreDatesInMemory", tool.prefs, false);
    /**
     * Method to tell whether DRC dates should be stored in memory or not.
     * The default is "false".
     * @return true if DRC dates should be stored in memory or not.
     */
    public static boolean isDatesStoredInMemory() { return cacheStoreDatesInMemory.getBoolean(); }
    /**
     * Method to set whether DRC dates should be stored in memory or not.
     * @param on true if DRC dates should be stored in memory or not.
     */
    public static void setDatesStoredInMemory(boolean on) { cacheStoreDatesInMemory.setBoolean(on); }

    private static Pref cacheInteractiveLog = Pref.makeBooleanPref("InteractiveLog", tool.prefs, false);
    /**
     * Method to tell whether DRC loggers should be displayed in Explorer immediately
     * The default is "false".
     * @return true if DRC loggers should be displayed in Explorer immediately or not.
     */
    public static boolean isInteractiveLoggingOn() { return cacheInteractiveLog.getBoolean(); }
    /**
     * Method to set whether DRC loggers should be displayed in Explorer immediately or not
     * @param on true if DRC loggers should be displayed in Explorer immediately.
     */
    public static void setInteractiveLogging(boolean on) { cacheInteractiveLog.setBoolean(on); }

    private static Pref cacheMinAreaAlgo = Pref.makeStringPref("MinAreaAlgorithm", tool.prefs, DRCCheckMinArea.AREA_LOCAL.name());
    /**
     * Method to tell which min area algorithm to use.
     * The default is AREA_BASIC which is the brute force version
     * @return true if DRC loggers should be displayed in Explorer immediately or not.
     */
    public static DRCCheckMinArea getMinAreaAlgoOption() { return DRCCheckMinArea.valueOf(cacheMinAreaAlgo.getString()); }
    /**
     * Method to set which min area algorithm to use.
     * @param mode DRCCheckMinArea type to set
     */
    public static void setMinAreaAlgoOption(DRCCheckMinArea mode) { cacheMinAreaAlgo.setString(mode.name()); }

    private static Pref cacheMultiThread = Pref.makeBooleanPref("MinMultiThread", tool.prefs, false);
    /**
     * Method to tell whether DRC should run in a single thread or multi-threaded.
     * The default is single-threaded.
     * @return true if DRC run in a multi-threaded fashion.
     */
    public static boolean isMultiThreaded() { return cacheMultiThread.getBoolean(); }
    /**
     * Method to set whether DRC should run in a single thread or multi-threaded.
     * @param mode True if it will run a multi-threaded version.
     */
    public static void setMultiThreaded(boolean mode) { cacheMultiThread.setBoolean(mode); }

    /****************************** END OF OPTIONS ******************************/

    /***********************************
     * Update Functions
     ***********************************/

    static void addDRCUpdate(int spacingBits,
                             Set<Cell> goodSpacingDRCDate, Set<Cell> cleanSpacingDRCDate,
                             Set<Cell> goodAreaDRCDate, Set<Cell> cleanAreaDRCDate,
                             HashMap<Geometric, List<Variable>> newVariables)
    {
        boolean goodSpace = (goodSpacingDRCDate != null && goodSpacingDRCDate.size() > 0);
        boolean cleanSpace = (cleanSpacingDRCDate != null && cleanSpacingDRCDate.size() > 0);
        boolean goodArea = (goodAreaDRCDate != null && goodAreaDRCDate.size() > 0);
        boolean cleanArea = (cleanAreaDRCDate != null && cleanAreaDRCDate.size() > 0);
        boolean vars = (newVariables != null && newVariables.size() > 0);
        if (!goodSpace && !cleanSpace && !vars && !goodArea && !cleanArea) return; // nothing to do
        new DRCUpdate(spacingBits, goodSpacingDRCDate, cleanSpacingDRCDate,
            goodAreaDRCDate, cleanAreaDRCDate, newVariables);
    }

	/**
	 * Method to delete all cached date information on all cells.
     * @param startJob
     */
	public static void resetDRCDates(boolean startJob)
	{
        new DRCReset(startJob);
	}

    private static class DRCReset extends Job
    {
        DRCReset(boolean startJob)
        {
            super("Resetting DRC Dates", User.getUserTool(), Job.Type.CHANGE, null, null, Priority.USER);
            if (startJob)
                startJob();
            else
                doIt();
        }

        public boolean doIt()
        {
            storedSpacingDRCDate.clear();
            storedAreaDRCDate.clear();
            // Always clean the dates as variables.
//            if (!isDatesStoredInMemory())
            {
                for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
                {
                    Library lib = it.next();
                    for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
                    {
                        Cell cell = cIt.next();
                        cleanDRCDateAndBits(cell, Layout.DRC_LAST_GOOD_DATE_SPACING);
                        cleanDRCDateAndBits(cell, Layout.DRC_LAST_GOOD_DATE_AREA);
                    }
                }
            }
            return true;
        }
    }

    /**
	 * Class to save good Layout DRC dates in a new thread or add new variables in Schematic DRC
	 */
	private static class DRCUpdate extends Job
	{
		Set<Cell> goodSpacingDRCDate;
		Set<Cell> cleanSpacingDRCDate;
        Set<Cell> goodAreaDRCDate;
		Set<Cell> cleanAreaDRCDate;
        HashMap<Geometric,List<Variable>> newVariables;
        int activeBits = Layout.DRC_LAST_GOOD_BIT_DEFAULT;

		public DRCUpdate(int bits,
                         Set<Cell> goodSpacingDRCD, Set<Cell> cleanSpacingDRCD,
                         Set<Cell> goodAreaDRCD, Set<Cell> cleanAreaDRCD,
                         HashMap<Geometric, List<Variable>> newVars)
		{
			super("Update DRC data", tool, Type.CHANGE, null, null, Priority.USER);
            this.goodSpacingDRCDate = goodSpacingDRCD;
			this.cleanSpacingDRCDate = cleanSpacingDRCD;
            this.goodAreaDRCDate = goodAreaDRCD;
            this.cleanAreaDRCDate = cleanAreaDRCD;
            this.newVariables = newVars;
            this.activeBits = bits;
            // Only works for layout with in memory dates -> no need of adding the job into the queue
            if (isDatesStoredInMemory() && (newVars == null || newVars.isEmpty()))
            {
                try {doIt();} catch (Exception e) {e.printStackTrace();}
            }
            else // put it into the queue
			    startJob();
		}

        /**
         * Template method to set DAte and bits information for a given map.
         * @param inMemory
         */
        private static void setInformation(HashMap<Cell,StoreDRCInfo> storedDRCDate,
                                           Set<Cell> goodDRCDate, Set<Cell> cleanDRCDate,
                                           Variable.Key key, int bits, boolean inMemory)
        {
            HashSet<Cell> goodDRCCells = new HashSet<Cell>();
            Long time = System.currentTimeMillis();

            if (goodDRCDate != null)
            {
                for (Cell cell : goodDRCDate)
                {
                    if (!cell.isLinked())
                        new JobException("Cell '" + cell + "' is invalid to clean DRC date");
                    else
                    {
                        if (inMemory)
                            storedDRCDate.put(cell, new StoreDRCInfo(time, bits));
                        else
                            goodDRCCells.add(cell);
                    }
                }
            }
            if (!goodDRCCells.isEmpty())
                Layout.setGoodDRCCells(goodDRCCells, key, bits, inMemory);

            if (cleanDRCDate != null)
            {
                for (Cell cell : cleanDRCDate)
                {
                    if (!cell.isLinked())
                        new JobException("Cell '" + cell + "' is invalid to clean DRC date");
                    else
                    {
                        StoreDRCInfo data = storedDRCDate.get(cell);
                        assert(data != null);
                        data.date = -1;
                        data.bits = Layout.DRC_LAST_GOOD_BIT_DEFAULT; // I can't put null because of the version
                        if (!inMemory)
                            cleanDRCDateAndBits(cell, key);
                    }
                }
            }

        }

        public boolean doIt() throws JobException
		{
            boolean inMemory = isDatesStoredInMemory();

            setInformation(storedSpacingDRCDate, goodSpacingDRCDate, cleanSpacingDRCDate,
                Layout.DRC_LAST_GOOD_DATE_SPACING, activeBits, inMemory);

            setInformation(storedAreaDRCDate, goodAreaDRCDate, cleanAreaDRCDate,
                Layout.DRC_LAST_GOOD_DATE_AREA, Layout.DRC_LAST_GOOD_BIT_DEFAULT, inMemory);

            // Update variables in Schematics DRC
            if (newVariables != null)
            {
                assert(!inMemory);
                for (Map.Entry<Geometric,List<Variable>> e : newVariables.entrySet())
                {
                    Geometric ni = e.getKey();
                    for (Variable var : e.getValue()) {
                        if (ni.isParam(var.getKey()))
                            ((NodeInst)ni).addParameter(var);
                        else
                            ni.addVar(var);
                    }
                }
            }
			return true;
		}
	}

    /***********************************
     * JUnit interface
     ***********************************/
    public static boolean testAll()
    {
        return true;
    }
}
