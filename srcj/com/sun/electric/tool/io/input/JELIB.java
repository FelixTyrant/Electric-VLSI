/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JELIB.java
 * Input/output tool: JELIB Library input
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.ArcProtoId;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.id.LibId;
import com.sun.electric.database.id.NodeProtoId;
import com.sun.electric.database.id.PrimitiveNodeId;
import com.sun.electric.database.id.TechId;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.ncc.basic.TransitiveRelation;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class reads files in new library file (.jelib) format.
 */
public class JELIB extends LibraryFiles
{
    private final FileType fileType;
    private final JelibParser parser;

	JELIB(LibId libId, URL fileURL, FileType fileType) throws IOException
	{
        this.fileType = fileType;
        parser = JelibParser.parse(libId, fileURL, fileType, false, Input.errorLogger);
	}
    
    public static Map<Setting,Object> readProjectSettings(URL fileURL, FileType fileType, TechPool techPool, ErrorLogger errorLogger) {
        JelibParser parser;
        try {
            parser = parse(techPool.idManager, fileURL, fileType, true, errorLogger);
        } catch (IOException e) {
            errorLogger.logError("Error readeing " + fileURL + ": " + e.getMessage(), -1);
            return null;
        }
        HashMap<Setting,Object> projectSettings = new HashMap<Setting,Object>();
        for (Map.Entry<TechId,Variable[]> e: parser.techIds.entrySet()) {
            TechId techId = e.getKey();
            Variable[] vars = e.getValue();
            Technology tech = techPool.getTech(techId);
            if (tech == null && techId.techName.equals("tsmc90"))
                tech = techPool.getTech(techPool.idManager.newTechId("cmos90"));
            if (tech == null) {
                Input.errorLogger.logError(fileURL +
                    ", Cannot identify technology " + techId.techName, -1);
                continue;
            }
            realizeMeaningPrefs(projectSettings, tech, vars);
        }
        for (Map.Entry<Tool,Variable[]> e: parser.tools.entrySet()) {
            Tool tool = e.getKey();
            Variable[] vars = e.getValue();
            realizeMeaningPrefs(projectSettings, tool, vars);
        }
        return projectSettings;
    }
    
    public static JelibParser parse(IdManager idManager, URL fileURL, FileType fileType, boolean onlyProjectSettings, ErrorLogger errorLogger) throws IOException {
        LibId libId = idManager.newLibId(TextUtils.getFileNameWithoutExtension(fileURL));
        return JelibParser.parse(libId, fileURL, fileType, onlyProjectSettings, errorLogger);
    }
    
	/**
	 * Method to read a Library in new library file (.jelib) format.
	 * @return true on error.
	 */
    @Override
	protected boolean readLib()
	{
        lib.erase();
        realizeVariables(lib, parser.libVars);
        lib.setVersion(parser.version);
        
        // Project settings
        for (Map.Entry<TechId,Variable[]> e: parser.techIds.entrySet()) {
            TechId techId = e.getKey();
            Variable[] vars = e.getValue();
            Technology tech = findTechnology(techId);
            if (tech == null) {
                Input.errorLogger.logError(filePath +
                    ", Cannot identify technology " + techId.techName, -1);
                continue;
            }
            realizeMeaningPrefs(tech, vars);
        }
        for (Map.Entry<Tool,Variable[]> e: parser.tools.entrySet()) {
            Tool tool = e.getKey();
            Variable[] vars = e.getValue();
            realizeMeaningPrefs(tool, vars);
        }
        for (Map.Entry<LibId,String> e: parser.externalLibIds.entrySet()) {
            LibId libId = e.getKey();
            String libFileName = e.getValue();
            if (Library.findLibrary(libId.libName) == null) 
                readExternalLibraryFromFilename(libFileName, fileType);
        }
        
        nodeProtoCount = parser.allCells.size();
        nodeProtoList = new Cell[nodeProtoCount];
        cellLambda = new double[nodeProtoCount];
        int cellNum = 0;
        for (JelibParser.CellContents cc: parser.allCells.values()) {
            CellId cellId = cc.cellId;
            Cell cell = Cell.newInstance(lib, cellId.cellName.toString());
             Technology tech = findTechnology(cc.techId);
            cell.setTechnology(tech);
            cell.lowLevelSetCreationDate(new Date(cc.creationDate));
            cell.lowLevelSetRevisionDate(new Date(cc.revisionDate));
            if (cc.expanded) cell.setWantExpanded();
            if (cc.allLocked) cell.setAllLocked();
            if (cc.instLocked) cell.setInstancesLocked();
            if (cc.cellLib) cell.setInCellLibrary();
            if (cc.techLib) cell.setInTechnologyLibrary();
            realizeVariables(cell, cc.vars);
            nodeProtoList[cellNum++] = cell;
       }

        // collect the cells by common protoName and by "groupLines" relation
        TransitiveRelation<Object> transitive = new TransitiveRelation<Object>();
        HashMap<String,String> protoNames = new HashMap<String,String>();
        for (Iterator<Cell> cit = lib.getCells(); cit.hasNext();)
        {
            Cell cell = cit.next();
            String protoName = protoNames.get(cell.getName());
            if (protoName == null)
            {
                protoName = cell.getName();
                protoNames.put(protoName, protoName);
            }
            transitive.theseAreRelated(cell, protoName);

            // consider groupName fields
            String groupName = parser.allCells.get(cell.getId()).groupName;
            if (groupName == null) continue;
            protoName = protoNames.get(groupName);
            if (protoName == null)
            {
                protoName = groupName;
                protoNames.put(protoName, protoName);
            }
            transitive.theseAreRelated(cell, protoName);
        }
        for (CellId[] groupLine : parser.groupLines)
        {
            Cell firstCell = null;
            for (int i = 0; i < groupLine.length; i++)
            {
                if (groupLine[i] == null) continue;
                CellId cellId = groupLine[i];
                Cell cell = lib.findNodeProto(cellId.cellName.toString());
                if (cell == null)
                {
                    Input.errorLogger.logError(filePath + ", line " + lineReader.getLineNumber() +
                        ", Cannot find cell " + cellId, -1);
                    break;
                }
                if (firstCell == null)
                    firstCell = cell;
                else
                    transitive.theseAreRelated(firstCell, cell);
            }
        }

        // create the cell groups
        for (Iterator<Set<Object>> git = transitive.getSetsOfRelatives(); git.hasNext();)
        {
            Set<Object> group = git.next();
            Cell firstCell = null;
            for (Object o : group)
            {
                if (!(o instanceof Cell)) continue;
                Cell cell = (Cell)o;
                if (firstCell == null)
                    firstCell = cell;
                else
                    cell.joinGroup(firstCell);
            }
        }

//        // set main schematic cells
//        for (Cell[] groupLine : groupLines)
//        {
//            Cell firstCell = groupLine[0];
//            if (firstCell == null) continue;
//            if (firstCell.isSchematic() && firstCell.getNewestVersion() == firstCell)
//                firstCell.getCellGroup().setMainSchematics(firstCell);
//        }

        // sensibility check: shouldn't all cells with the same root name be in the same group?
        HashMap<String,Cell.CellGroup> cellGroups = new HashMap<String,Cell.CellGroup>();
        for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
        {
            Cell cell = it.next();
            String canonicName = TextUtils.canonicString(cell.getName());
            Cell.CellGroup group = cell.getCellGroup();
            Cell.CellGroup groupOfName = cellGroups.get(canonicName);
            if (groupOfName == null)
            {
                cellGroups.put(canonicName, group);
            } else
            {
                if (groupOfName != group)
                {
                    Input.errorLogger.logError(filePath + ", Library has multiple cells named '" +
                        canonicName + "' that are not in the same group", -1);
                }
            }
        }

        lib.setFromDisk();
        lib.setDelibCellFiles(parser.delibCellFiles);
        return false;
	}

	/**
	 * Method to recursively create the contents of each cell in the library.
	 */
    @Override
	protected void realizeCellsRecursively(Cell cell, HashSet<Cell> recursiveSetupFlag, String scaledCellName, double scale)
	{
		if (scaledCellName != null) return;
		JelibParser.CellContents cc = parser.allCells.get(cell.getId());
		if (cc == null || cc.filledIn) return;
		instantiateCellContent(cell, cc, recursiveSetupFlag);
		cellsConstructed++;
        setProgressValue(cellsConstructed * 100 / totalCells);
		recursiveSetupFlag.add(cell);
        cell.loadExpandStatus();
	}

	/**
	 * Method called after all libraries have been read to instantiate a single Cell.
	 * @param cell the Cell to instantiate.
	 * @param cc the contents of that cell (the strings from the file).
	 */
	private void instantiateCellContent(Cell cell, JelibParser.CellContents cc, HashSet<Cell> recursiveSetupFlag)
	{
        HashMap<Technology,Technology.SizeCorrector> sizeCorrectors = new HashMap<Technology,Technology.SizeCorrector>();
        
		// place all nodes
        for (JelibParser.NodeContents n: cc.nodes) {
            int line = n.line;

			String prefixName = lib.getName();
			NodeProto np = null;
            NodeProtoId protoId = n.protoId;
            if (protoId instanceof CellId)
                np = database.getCell((CellId)protoId);
            else {
                PrimitiveNodeId pnId = (PrimitiveNodeId)protoId;
                np = findPrimitiveNode(pnId);
                if (np == null) {
					Input.errorLogger.logError(cc.fileName + ", line " + line +
						", Cannot find primitive node " + pnId, cell, -1);
                    CellName cellName = CellName.parseName(pnId.name);
                    if (cellName.getVersion() <= 0)
                        cellName = CellName.newName(cellName.getName(), cellName.getView(), 1);
                    protoId = lib.getId().newCellId(cellName);
                }
            }

			// make sure the subcell has been instantiated
			if (np != null && np instanceof Cell)
			{
				Cell subCell = (Cell)np;
				// subcell: make sure that cell is setup
				if (!recursiveSetupFlag.contains(subCell))
				{
					LibraryFiles reader = this;
					if (subCell.getLibrary() != cell.getLibrary())
						reader = getReaderForLib(subCell.getLibrary());

					// subcell: make sure that cell is setup
					if (reader != null)
						reader.realizeCellsRecursively(subCell, recursiveSetupFlag, null, 0);
				}
			}

            EPoint size = n.size;
            if (np instanceof PrimitiveNode) {
                PrimitiveNode pn = (PrimitiveNode)np;
                Technology tech = pn.getTechnology();
                Technology.SizeCorrector sizeCorrector = sizeCorrectors.get(tech);
                if (sizeCorrector == null) {
                    sizeCorrector = tech.getSizeCorrector(cc.version, projectSettings, true, false);
                    sizeCorrectors.put(tech, sizeCorrector);
                }
                size = sizeCorrector.getSizeFromDisk(pn, size.getLambdaX(), size.getLambdaY());
            }

			if (np == null)
			{
                CellId dummyCellId = (CellId)protoId;
                String protoName = dummyCellId.cellName.toString();
                Library cellLib = database.getLib(dummyCellId.libId);
				if (cellLib == null)
				{
					Input.errorLogger.logError(cc.fileName + ", line " + line +
						", Creating dummy library " + prefixName, cell, -1);
					cellLib = Library.newInstance(prefixName, null);
				}
				Cell dummyCell = Cell.makeInstance(cellLib, protoName);
				if (dummyCell == null)
				{
					Input.errorLogger.logError(cc.fileName + ", line " + line +
						", Unable to create dummy cell " + protoName + " in " + cellLib, cell, -1);
					continue;
				}
				Input.errorLogger.logError(cc.fileName + ", line " + line +
					", Creating dummy cell " + protoName + " in " + cellLib, cell, -1);
				Rectangle2D bounds = parser.externalCells.get(dummyCellId.toString());
				if (bounds == null)
				{
					Input.errorLogger.logError(cc.fileName + ", line " + line +
						", Warning: cannot find information about external cell " + dummyCellId, cell, -1);
					NodeInst.newInstance(Generic.tech().invisiblePinNode, new Point2D.Double(0,0), 0, 0, dummyCell);
				} else
				{
					NodeInst.newInstance(Generic.tech().invisiblePinNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
						bounds.getWidth(), bounds.getHeight(), dummyCell);
				}

				// mark this as a dummy cell
				dummyCell.newVar(IO_TRUE_LIBRARY, prefixName);
				dummyCell.newVar(IO_DUMMY_OBJECT, protoName);
				np = dummyCell;
			}

			// create the node
			NodeInst ni = NodeInst.newInstance(cell, np, n.nodeName, n.nameTextDescriptor,
                    n.anchor, size, n.orient, n.flags, n.techBits, n.protoTextDescriptor, Input.errorLogger);
			if (ni == null)
			{
				Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
					" (" + cell + ") cannot create node " + n.protoId, cell, -1);
				continue;
			}

			// add variables
            realizeVariables(ni, n.vars);
            
			// insert into map of disk names
            n.ni = ni;

		}

		// place all exports
        for (JelibParser.ExportContents e: cc.exports) {
            int line = e.line;

			PortInst pi = figureOutPortInst(cell, e.originalPort.externalId, e.originalNode, e.pos, cc.fileName, line);
			if (pi == null) continue;
            
			// create the export
			Export pp = Export.newInstance(cell, e.exportId, e.exportUserName, e.nameTextDescriptor, pi,
                    e.alwaysDrawn, e.bodyOnly, e.ch, errorLogger);
			if (pp == null)
			{
				Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
					" (" + cell + ") cannot create export " + e.exportUserName, pi.getNodeInst(), cell, null, -1);
				continue;
			}

            // add variables in tail fields
            realizeVariables(pp, e.vars);
		}

		// next place all arcs
		for(JelibParser.ArcContents a: cc.arcs) {
            int line = a.line;
            
            ArcProto ap = findArcProto(a.arcProtoId);
			if (ap == null) {
				Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
					" (" + cell + ") cannot find arc " + a.arcProtoId, cell, -1);
				continue;
			}
            Technology tech = ap.getTechnology();
            Technology.SizeCorrector sizeCorrector = sizeCorrectors.get(tech);
            if (sizeCorrector == null) {
                 sizeCorrector = tech.getSizeCorrector(cc.version, projectSettings, true, false);
                sizeCorrectors.put(tech, sizeCorrector);
            }
			long gridExtendOverMin = sizeCorrector.getExtendFromDisk(ap, a.diskWidth);

			PortInst headPI = figureOutPortInst(cell, a.headPort.externalId, a.headNode, a.headPoint, cc.fileName, line);
			if (headPI == null) continue;

			PortInst tailPI = figureOutPortInst(cell, a.tailPort.externalId, a.tailNode, a.tailPoint, cc.fileName, line);
			if (tailPI == null) continue;

            ArcInst ai = ArcInst.newInstance(cell, ap, a.arcName, a.nameTextDescriptor,
                    headPI, tailPI, a.headPoint, a.tailPoint, gridExtendOverMin, a.angle, a.flags);
			if (ai == null)
			{
				List<Geometric> geomList = new ArrayList<Geometric>();
				geomList.add(headPI.getNodeInst());
				geomList.add(tailPI.getNodeInst());
				Input.errorLogger.logError(cc.fileName + ", line " + line +
					" (" + cell + ") cannot create arc " + a.arcProtoId, geomList, null, cell, 2);
				continue;
			}
            realizeVariables(ai, a.vars);
		}
		cc.filledIn = true;
	}

	/**
	 * Method to find the proper PortInst for a specified port on a node, at a given position.
	 * @param cell the cell in which this all resides.
	 * @param portName the name of the port (may be an empty string if there is only 1 port).
	 * @param n the node.
	 * @param pos the position of the port on the node.
	 * @param lineNumber the line number in the file being read (for error reporting).
	 * @return the PortInst specified (null if none can be found).
	 */
	private PortInst figureOutPortInst(Cell cell, String portName, JelibParser.NodeContents n, Point2D pos, String fileName, int lineNumber)
	{
		NodeInst ni = n != null ? n.ni : null;
		if (ni == null)
		{
			Input.errorLogger.logError(fileName + ", line " + lineNumber +
				" (" + cell + ") cannot find node " + n.nodeName, cell, -1);
			return null;
		}

		PortInst pi = null;
        PortProto pp = findPortProto(ni.getProto(), portName);
        if (pp != null)
            pi = ni.findPortInstFromProto(pp);

		// primitives use the name match
//		if (!ni.isCellInstance()) return pi;

//		// make sure the port can handle the position
//		if (pi != null && pos != null)
//		{
//			Poly poly = pi.getPoly();
//			if (!(poly.isInside(pos) || poly.polyDistance(pos.getX(), pos.getY()) < TINYDISTANCE))
//			{
//				NodeProto np = ni.getProto();
//				Input.errorLogger.logError(fileName + ", line " + lineNumber +
//					" (" + cell + ") point (" + pos.getX() + "," + pos.getY() + ") does not fit in " +
//					pi + " which is centered at (" + poly.getCenterX() + "," + poly.getCenterY() + ")", new EPoint(pos.getX(), pos.getY()), cell, -1);
//				if (np instanceof Cell)
//					pi = null;
//			}
//		}
		if (pi != null) return pi;

		// see if this is a dummy cell
        Variable var = null;
        Cell subCell = null;
        if (ni.isCellInstance()) {
            subCell = (Cell)ni.getProto();
            var = subCell.getVar(IO_TRUE_LIBRARY);
            if (pos == null)
                pos = parser.externalExports.get(subCell.getId().newPortId(portName));
		}
        if (pos == null)
            pos = ni.getAnchorCenter().lambdaMutable();
		if (var == null)
		{
			// not a dummy cell: create a pin at the top level
			NodeInst portNI = NodeInst.newInstance(Generic.tech().universalPinNode, pos, 0, 0, cell);
			if (portNI == null)
			{
				Input.errorLogger.logError(fileName + ", line " + lineNumber +
					", Unable to create dummy node in " + cell + " (cannot create source node)", cell, -1);
				return null;
			}
			Input.errorLogger.logError(fileName + ", line " + lineNumber +
				", Port "+portName+" on "+ni.getProto() + " renamed or deleted, still used on node "+n.nodeName+" in " + cell, portNI, cell, null, -1);
			return portNI.getOnlyPortInst();
		}

		// a dummy cell: create a dummy export on it to fit this
		String name = portName;
		if (name.length() == 0) name = "X";
        ni.transformIn().transform(pos, pos);
		NodeInst portNI = NodeInst.newInstance(Generic.tech().universalPinNode, pos, 0, 0, subCell);
		if (portNI == null)
		{
			Input.errorLogger.logError(fileName + ", line " + lineNumber +
				", Unable to create export " + name + " on dummy " + subCell + " (cannot create source node)", cell, -1);
			return null;
		}
		PortInst portPI = portNI.getOnlyPortInst();
		Export export = Export.newInstance(subCell, portPI, name, false);
		if (export == null)
		{
			Input.errorLogger.logError(fileName + ", line " + lineNumber +
				", Unable to create export " + name + " on dummy " + subCell, cell, -1);
			return null;
		}
		pi = ni.findPortInstFromProto(export);
		Input.errorLogger.logError(fileName + ", line " + lineNumber +
			", Creating export " + name + " on dummy " + subCell, cell, -1);

		return pi;
	}

    Technology findTechnology(TechId techId) {
        TechPool techPool = database.getTechPool();
        Technology tech = techPool.getTech(techId);
        if (tech == null && techId.techName.equals("tsmc90"))
            tech = techPool.getTech(idManager.newTechId("cmos90"));
        return tech;
    }
    
	PrimitiveNode findPrimitiveNode(PrimitiveNodeId primitiveNodeId) {
        TechPool techPool = database.getTechPool();
        PrimitiveNode pn = techPool.getPrimitiveNode(primitiveNodeId);
        if (pn == null && primitiveNodeId.techId.techName.equals("tsmc90"))
            pn = findPrimitiveNode(idManager.newTechId("cmos90").newPrimitiveNodeId(primitiveNodeId.name));
		if (pn == null) {
            Technology tech = findTechnology(primitiveNodeId.techId);
            if (tech != null)
                pn = tech.convertOldNodeName(primitiveNodeId.name);
        }
        return pn;
	}
    
    ArcProto findArcProto(ArcProtoId arcProtoId) {
        TechPool techPool = database.getTechPool();
        ArcProto ap = techPool.getArcProto(arcProtoId);
        if (ap == null && arcProtoId.techId.techName.equals("tsmc90"))
            ap = findArcProto(idManager.newTechId("cmos90").newArcProtoId(arcProtoId.name));
        return ap;
    }
}
