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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.ELIBConstants;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This class reads files in new library file (.jelib) format.
 */
public class JELIB1 extends LibraryFiles
{
	private static final String[] revisions =
	{
		// Revision 1
		"8.01az"
	};

	private static final String[] NULL_STRINGS_ARRAY = {};
	private static final LibraryContents.NodeContents[] NULL_NODES_ARRAY = {};
	private static final LibraryContents.ArcContents[] NULL_ARCS_ARRAY = {};
	private static final LibraryContents.ExportContents[] NULL_EXPORTS_ARRAY = {};

	private LibraryContents libraryContents;
	private HashMap/*<Cell,CellContents>*/ cellToContents = new HashMap();

	private Version version;
	private int revision;
	private LibraryContents.CellContents currentCellContents;

	private int lineNumber;
	private List stringPieces = new ArrayList();
	private int curPiece;
	/** The number of lines that have been "processed" so far. */	private static int numProcessed;
	/** The number of lines that must be "processed". */			private static int numToProcess;

	JELIB1()
	{
	}

	/**
	 * Method to read a Library from disk.
	 * This method is for reading full Electric libraries in ELIB, JELIB, and Readable Dump format.
	 * @param fileURL the URL to the disk file.
	 * @param type the type of library file (ELIB, JELIB, etc.)
	 * @return the read Library, or null if an error occurred.
	 */
	public static synchronized void convertLibrary(URL fileURL, String outFilePath)
	{
		errorLogger = ErrorLogger.newInstance("Library Convert");
		try {
			JELIB1 in = new JELIB1();
			if (in.openTextInput(fileURL)) return;
			// read the library
			boolean error = in.readTheLibrary();
			in.closeInput();
			errorLogger.termLogging(true);
			if (error)
			{
				System.out.println("Error reading library " + fileURL);
				if (in.topLevelLibrary) mainLibDirectory = null;
				return;
			}
			PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(outFilePath)));
			in.libraryContents.printJelib(printWriter);
			printWriter.close();
			System.out.println("Written " + outFilePath);
        } catch (IOException e)
		{
            System.out.println("Error opening " + outFilePath);
        }
    }

	/**
	 * Method to read a Library in new library file (.jelib) format.
	 * @return true on error.
	 */
	protected boolean readLib()
	{
		try
		{
			if (readTheLibrary()) return true;
			List cellContentses = libraryContents.checkTheLibrary(lib, topLevelLibrary);
			if (cellContentses == null) return true;
			nodeProtoCount = cellContentses.size();
			nodeProtoList = new Cell[nodeProtoCount];
			cellLambda = new double[nodeProtoCount];
			for (int i = 0; i < nodeProtoCount; i++)
			{
				LibraryContents.CellContents cc = (LibraryContents.CellContents)cellContentses.get(i);
				if (cc != null)
				{
					Cell cell = cc.getCell();
					nodeProtoList[i] = cell;
					cellToContents.put(cell, cc);
				}
			}
			return false;
		} catch (IOException e)
		{
			Input.errorLogger.logError("End of file reached while reading " + filePath, null, -1);
			return true;
		}
	}

	private LibraryContents.TechnologyRef getTechnologyRef(String techName)
	{
		return libraryContents.getTechnologyRef(techName);
	}

	private LibraryContents.PrimitiveNodeRef getPrimitiveNodeRef(LibraryContents.TechnologyRef techRef, String name)
	{
		return libraryContents.getPrimitiveNodeRef(techRef, name);
	}

	private LibraryContents.ArcProtoRef getArcProtoRef(LibraryContents.TechnologyRef techRef, String name)
	{
		return libraryContents.getArcProtoRef(techRef, name);
	}

	private LibraryContents.LibraryRef getLibraryRef(String libName)
	{
		return libraryContents.getLibraryRef(libName);
	}

	private LibraryContents.CellRef getCellRef(String name)
	{
		return libraryContents.getCellRef(name);
	}

// 	VariableKeyRef getVariableKeyRef(String varName)
// 	{
// 		VariableKeyRef variableKeyRef = (VariableKeyRef)variableKeyRefs.get(varName);
// 		if (variableKeyRef == null)
// 		{
// 			variableKeyRef = new VariableKeyRef(varName);
// 			variableKeyRefs.put(varName, variableKeyRef);
// 			variableKeyRefs.setUsed();
// 		}
// 		return variableKeyRef;
// 	}

	/**
	 * Method to read the .elib file.
	 * Returns true on error.
	 */
	private boolean readTheLibrary()
		throws IOException
	{
		revision = revisions.length; // Latest revesion
		LibraryContents.LibraryRef curExternalLib = null;
		LibraryContents.TechnologyRef curTech = null;
		LibraryContents.PrimitiveNodeRef curPrim = null;
		for(;;)
		{
			// get keyword from file
			String line = lineReader.readLine();
			if (line == null) break;

			// ignore blanks and comments
			if (line.length() == 0) continue;
			char first = line.charAt(0);
			if (first == '#') continue;

			if (libraryContents == null && first != 'H')
			{
				logError("Header line is omitted. Stop reading");
				return true;
			}
			if (curExternalLib != null && first != 'R')
				curExternalLib = null;
			if (curTech != null && first != 'D' && first != 'P' && first != 'W')
				curTech = null;
			if (curPrim != null && first != 'D')
				curPrim = null;

			if (first == 'C')
			{
				// grab a cell description
				if (parseLine(line, "QssQsss")) continue;
				String baseName = nextPiece();
				String abbrev = nextPiece();
				String version = nextPiece();
				String name = baseName + ";" + version + "{" + abbrev + "}";
				LibraryContents.TechnologyRef techRef = getTechnologyRef(nextPiece());
				long creationDate = Long.parseLong(nextPiece());
				long revisionDate = Long.parseLong(nextPiece());
				LibraryContents.CellContents cc = libraryContents.newCellContents(name, creationDate, revisionDate);
				cc.setTechRef(techRef);
				cc.setLineNumber(lineReader.getLineNumber() + 1);

				// parse state information in field 6
				String stateInfo = nextPiece();
				boolean expanded = false, allLocked = false, instLocked = false,
					cellLib = false, techLib = false;
				for(int i=0; i<stateInfo.length(); i++)
				{
					char chr = stateInfo.charAt(i);
					if (chr == 'E') cc.setWantExpanded(true); else
					if (chr == 'L') cc.setAllLocked(true); else
					if (chr == 'I') cc.setInstancesLocked(true); else
					if (chr == 'C') cc.setInCellLibrary(true); else
					if (chr == 'T') cc.setInTechnologyLibrary(true);
				}

				// gather the contents of the cell into a list of Strings
				cc.setVars(varPieces());
				readCellContents(cc);
				continue;
			}

			if (first == 'L')
			{
				// cross-library reference
			    if (parseLine(line, "QQ")) continue;
				String libName = nextPiece();
				String fileName = nextPiece();
				if (libName.equals(""))
				{
					logError("No library name in " + line);
					continue;
				}
				if (libName.indexOf(':') >= 0)
				{
					logError("Library name " + libName + " contains semicolon");
					continue;
				}
				if (fileName.equals("")) fileName = libName;
				curExternalLib = libraryContents.newLibraryRef(libName, fileName);
				continue;
			}

			if (first == 'R')
			{
				// cross-library cell information
				if (parseLine(line, "Sssss")) continue;
				String cellName = convertCellName(nextPiece());
				double lowX = TextUtils.atof(nextPiece());
				double highX = TextUtils.atof(nextPiece());
				double lowY = TextUtils.atof(nextPiece());
				double highY = TextUtils.atof(nextPiece());
				if (curExternalLib == null)
				{
					logError("External cell " + cellName + " has no library before it");
					continue;
				}
				curExternalLib.newExternalCellRef(cellName, lowX, lowY, highX, highY);
				continue;
			}

			if (first == 'H')
			{
				// parse header
				if (parseLine(line, "Qs")) continue;
				if (libraryContents != null)
				{
					logError("Second header line ignored " + line);
					continue;
				}
				String curLibName = nextPiece();
				String versionString = nextPiece();
				if (curLibName.equals(""))
				{
					logError("No library name in Header line " + line);
					return true;
				}
				if (curLibName.indexOf(':') >= 0)
				{
					logError("Library name " + curLibName + " contains semicolon");
					return true;
				}
				version = Version.parseVersion(versionString);
				if (version != null)
				{
					if (version.compareTo(Version.getVersion()) > 0)
					{
						logWarning("Library " + curLibName + " comes from a NEWER version of Electric (" + version + ")");
					}
					// Determine format revision
					for (revision = 0; revision < revisions.length; revision++)
					{
						if (version.compareTo(Version.parseVersion(revisions[revision])) < 0) break;
					}
				} else
				{
					logError("Badly formed version: " + versionString);
				}
				
				libraryContents = new LibraryContents(curLibName, this);
				libraryContents.setVars(varPieces());
				libraryContents.setVersion(version);
				continue;
			}

			if (first == 'O')
			{
				// parse Tool information
				if (parseLine(line, "Q")) continue;
				String toolName = nextPiece();
				LibraryContents.ToolRef toolRef = libraryContents.newToolRef(toolName);
				toolRef.setVars(varPieces());
				continue;
			}

			if (first == 'V')
			{
				// parse View information
				if (parseLine(line, "QQ")) continue;
				String viewFullName = nextPiece();
				String viewAbbr = nextPiece();
				libraryContents.newViewRef(viewAbbr, viewFullName);
//				// get additional variables starting at position 2
//				addVariables(view, varPieces());
				continue;
			}

			if (first == 'T')
			{
				// parse Technology information
				if (parseLine(line, "Q")) continue;
				String techName = nextPiece();
				curTech = libraryContents.newTechnologyRef(techName);
				curTech.setVars(varPieces());
				continue;
			}

			if (first == 'D' && revision < 1)
			{
				// parse PrimitiveNode information
				if (parseLine(line, "Q")) continue;
				String primName = nextPiece();
				if (curTech == null)
				{
					logError("Primitive node " + primName + " has no technology before it");
					continue;
				}
				curPrim = curTech.newPrimitiveNodeRef(primName);
				curPrim.setVars(varPieces());
				continue;
			}

			if (first == 'P' && revision < 1)
			{
				// parse PrimitivePort information
				if (parseLine(line, "Q")) continue;
				String primPortName = nextPiece();
				if (curPrim == null)
				{
					logError("Primitive port " + primPortName + " has no primitive node before it");
					continue;
				}
				curPrim.newPrimitivePortRef(primPortName);
				curPrim.setVars(varPieces());
				continue;
			}

			if (first == 'W' && revision < 1)
			{
				// parse ArcProto information
				if (parseLine(line, "Q")) continue;
				String arcName = nextPiece();
				if (curTech == null)
				{
					logError("Primitive arc " + arcName + " has no technology before it");
					continue;
				}
				LibraryContents.ArcProtoRef apRef = curTech.newArcProtoRef(arcName);
				apRef.setVars(varPieces());
				continue;
			}

			if (first == 'G')
			{
				// group information
				if (parseLine(line, "")) continue;
				String[] pieces = stringPieces();
				LibraryContents.CellRef[] cellRefs = new LibraryContents.CellRef[pieces.length];
				for (int i = 0; i < pieces.length; i++)
				{
					if (pieces[i].equals("")) continue;
					cellRefs[i] = libraryContents.getCellRef(pieces[i]);
//					if (cellRefs[i].libraryRef);  // check from this library
				}
				libraryContents.addCellGroup(cellRefs);
				continue;

// 				// If first cell is nonempty, relate cells linearily
// 				// If first cell is empty, relate cells circularily
// 				LibraryContents.CellRef firstC = null;
// 				LibraryContents.CellRef prevC = null;
// 				for (int i = 0; i < pieces.length; i++)
// 				{
// 					LibraryContents.CellRef nextC = null;
// 					for (; nextC == null && i < pieces.length; i++)
// 					{
// 						String cellName = pieces[i];
// 						if (cellName.length() == 0) continue;
// 						int colonPos = cellName.indexOf(':');
// 						if (colonPos >= 0) cellName = cellName.substring(colonPos+1);
// 						nextC = libraryContents.getMyLibraryRef().getCellRef(cellName);
// 					}
// 					if (nextC == null) break;
// 					if (firstC == null)	firstC = nextC;
// 					if (i == 0) continue;
// 					if (prevC != null) prevC.setNextInGroup(nextC);
// 					prevC = nextC;
// 				}
// 				if (prevC != null) prevC.setNextInGroup(firstC);
// 				continue;
			}

			logError("Unrecognized line: " + line);
		}
		return errorCount != 0;
	}

	/**
	 * Method called after all libraries have been read to instantiate a single Cell.
	 * @param cell the Cell to instantiate.
	 * @param cc the contents of that cell (the strings from the file).
	 */
	private void readCellContents(LibraryContents.CellContents cc)
		throws IOException
	{
		currentCellContents = cc;

		// map disk node names (duplicate node names written "sig"1 and "sig"2)
		HashMap diskName = new HashMap();

		String line = lineReader.readLine();

		// place all nodes
		List nodes = new ArrayList();
		for(;line != null; line = lineReader.readLine())
		{
			// ignore blanks and comments
			if (line.length() == 0) continue;
			char first = line.charAt(0);
			if (first == '#') continue;
			if (first != 'N' && first != 'I') break;

			// parse the node line
			String format;
			if (revision >= 1)
			{
				if (first == 'N')
				{
					format = "QQsssssss";
				} else 
				{
					format =  "QQssssss";
				}
			} else
			{
				format = "QQssssssss";
			}
			if (parseLine(line, format)) continue;

			// figure out the name for this node.  Handle the form: "Sig"12
			String protoName = nextPiece();
			LibraryContents.NodeProtoRef protoRef;
			if (first == 'I')
			{
				protoRef = getCellRef(protoName);
			} else if (revision >= 1)
			{
				protoRef = getPrimitiveNodeRef(cc.techRef, protoName);
			} else
			{
				if (protoName.indexOf(':') >= 0 && Cell.findNodeProto(protoName) == null)
					protoRef = getCellRef(protoName);
				else
					protoRef = getPrimitiveNodeRef(null, protoName);
			}

			String diskNodeName = nextPiece();
			String nodeName = diskNodeName;
			if (nodeName.charAt(0) == '"')
			{
				int lastQuote = nodeName.lastIndexOf('"');
				if (lastQuote > 1)
				{
					nodeName = nodeName.substring(1, lastQuote);
				}
			}

			String textDescriptorInfo = nextPiece();
			double x = TextUtils.atof(nextPiece());
			double y = TextUtils.atof(nextPiece());

			LibraryContents.NodeContents nc = cc.newNodeContents(protoRef, nodeName, textDescriptorInfo, x, y);
			nodes.add(nc);
			nc.setJelibName(diskNodeName);
			nc.setLineNumber(lineReader.getLineNumber(), first);

			if (first == 'N' || revision < 1)
			{
				double wid = TextUtils.atof(nextPiece());
				if (wid < 0 && revision >= 1)
					logError("Negative width " + wid + " of cell instance");
				double hei = TextUtils.atof(nextPiece());
				if (hei < 0 && revision >= 1)
					logError("Negative height " + hei + " of cell instance");
				nc.setSize(wid, hei);
			}

			String orientString = nextPiece();
			boolean mirX = false, mirY = false;
			int angle = 0;
			for (int i = 0; i < orientString.length(); i++)
			{
				char ch = orientString.charAt(i);
				if (ch == 'X') mirX = !mirX;
				else if (ch == 'Y')	mirY = !mirY;
				else if (ch == 'R') angle += 900;
				else
				{
					angle += TextUtils.atoi(orientString.substring(i));
					break;
				}
			}
			nc.setOrientation(mirX, mirY, angle);

			String stateInfo = nextPiece();
			// parse state information in stateInfo field 
			for(int i=0; i<stateInfo.length(); i++)
			{
				char chr = stateInfo.charAt(i);
				if (chr == 'E') nc.setExpanded(true); else
				if (chr == 'L') nc.setLocked(true); else
				if (chr == 'S') nc.setShortened(true); else
				if (chr == 'V') nc.setVisInside(true); else
				if (chr == 'W') nc.setWiped(true); else
				if (chr == 'A') nc.setHardSelect(true); else
				if (TextUtils.isDigit(chr))
				{
					nc.setTechSpecific(TextUtils.atoi(stateInfo.substring(i)));
					break;
				}
			}

			if (first == 'I' || revision < 1)
				nc.setProtoTextDescriptor(nextPiece());

			nc.setVars(varPieces());

			// insert into map of disk names
			diskName.put(diskNodeName, nc);
		}
		cc.nodes = (LibraryContents.NodeContents[])nodes.toArray(NULL_NODES_ARRAY);

		List arcs = new ArrayList();
	ArcLoop:
		for(;line != null; line = lineReader.readLine())
		{
			// ignore blanks and comments
			if (line.length() == 0) continue;
			char first = line.charAt(0);
			if (first == '#') continue;
			if (first != 'A') break;

			// parse the arc line
			if (parseLine(line, "QQsssQQssQQss")) continue;
			String protoName = nextPiece();
			LibraryContents.ArcProtoRef apRef = getArcProtoRef(cc.techRef, protoName);
			String arcName = nextPiece();
			String textDescriptorInfo = nextPiece();
			double wid = TextUtils.atof(nextPiece());

			LibraryContents.ArcContents ac = cc.newArcContents(apRef, arcName, textDescriptorInfo, wid);
			ac.setLineNumber(lineReader.getLineNumber());

			// parse state information
			String stateInfo = nextPiece();
			ac.setFixedAngle(true);
			ac.setExtended(true);
			for(int i=0; i<stateInfo.length(); i++)
			{
				char chr = stateInfo.charAt(i);
				if (chr == 'R') ac.setRigid(true); else
				if (chr == 'F') ac.setFixedAngle(false); else
				if (chr == 'S') ac.setSlidable(true); else
				if (chr == 'E') ac.setExtended(false); else
				if (chr == 'D') ac.setDirectional(true); else
				if (chr == 'V') ac.setReverseEnds(true); else
				if (chr == 'A') ac.setHardSelect(true); else
				if (chr == 'H') ac.setSkipHead(true); else
				if (chr == 'T') ac.setSkipTail(true); else
				if (chr == 'N') ac.setTailNegated(true); else
				if (chr == 'G') ac.setHeadNegated (true); else
				if (TextUtils.isDigit(chr))
				{
					ac.setAngle(TextUtils.atoi(stateInfo.substring(i)));
					break;
				}
			}

			// parse head and tail information
			for (int i = 0; i < 2; i++)
			{
				String nodeName = nextPiece();
				LibraryContents.NodeContents node = (LibraryContents.NodeContents)diskName.get(nodeName);
				if (node == null)
				{
					logError("cannot find node " + nodeName);
					continue ArcLoop;
				}
				String portName = nextPiece();
				LibraryContents.PortProtoRef portProtoRef = node.getNodeProtoRef().getPortProtoRef(portName);
				double x = TextUtils.atof(nextPiece());
				double y = TextUtils.atof(nextPiece());
				ac.setEnd(i != 0, node, portProtoRef, x, y);
			}

			ac.setVars(varPieces());

			arcs.add(ac);
		}
		cc.arcs = (LibraryContents.ArcContents[])arcs.toArray(NULL_ARCS_ARRAY);

		List exports = new ArrayList();
		for(;line != null; line = lineReader.readLine())
		{
			// ignore blanks and comments
			if (line.length() == 0) continue;
			char first = line.charAt(0);
			if (first == '#') continue;
			if (first != 'E') break;

			// parse the export line
			if (parseLine(line, "QsQQsss")) continue;
			String exportName = nextPiece();
			String textDescriptorInfo = nextPiece();

			String nodeName = nextPiece();
			LibraryContents.NodeContents node = (LibraryContents.NodeContents)diskName.get(nodeName);
			if (node == null)
			{
				logError("cannot find node " + nodeName);
				continue;
			}

			String portName = nextPiece();
			LibraryContents.PortProtoRef portProtoRef = node.getNodeProtoRef().getPortProtoRef(portName);
			double x = TextUtils.atof(nextPiece());
			double y = TextUtils.atof(nextPiece());

			LibraryContents.ExportContents ec = cc.newExportContents(exportName, textDescriptorInfo, node, portProtoRef, x, y);
			ec.setLineNumber(lineReader.getLineNumber());

			// parse state information
			String stateInfo = nextPiece();
			int slashPos = stateInfo.indexOf('/');
			if (slashPos >= 0)
			{
				String extras = stateInfo.substring(slashPos);
				stateInfo = stateInfo.substring(0, slashPos);
				while (extras.length() > 0)
				{
					if (extras.charAt(1) == 'A') ec.setAlwaysDrawn(true); else
					if (extras.charAt(1) == 'B') ec.setBodyOnly(true);
					extras = extras.substring(2);
				}
			}
			ec.setCharacteristic(PortCharacteristic.findCharacteristicShort(stateInfo));

			ec.setVars(varPieces());

			exports.add(ec);
		}
		cc.exports = (LibraryContents.ExportContents[])exports.toArray(NULL_EXPORTS_ARRAY);

		// Check 'X'
		if (line != null && line.charAt(0) != 'X')
		{
			logError("unrecognized line type " + line.charAt(0));
		}
		currentCellContents = null;
	}

	/**
	 * Method called after all libraries have been read.
	 * Instantiates all of the Cell contents that were saved in "allCells".
	 */
// 	private void instantiateCellContents()
// 	{
// 		System.out.println("Creating the circuitry...");
// 		progress.setNote("Creating the circuitry");

// 		libraryContents.instantiateCellContents();
// 	}

	/**
	 * Method to recursively create the contents of each cell in the library.
	 */
	protected void realizeCellsRecursively(Cell cell, FlagSet recursiveSetupFlag, String scaledCellName, double scaleX, double scaleY)
	{
		// do not realize cross-library references
		if (cell.getLibrary() != lib) return;

		LibraryContents.CellContents cc = (LibraryContents.CellContents)cellToContents.get(cell);
		if (cc == null || cc.isFilledIn()) return;

		// cannot do scaling yet
		if (scaledCellName != null) return;

		NodeProto[] npList = new NodeProto[cc.nodes.length];
		for (int i = 0; i < cc.nodes.length; i++)
		{
			LibraryContents.NodeContents nc = cc.nodes[i];
			if (nc == null) continue;
			npList[i] = nc.getNodeProtoRef().getNodeProto();
		}
		scanNodesForRecursion(cell, recursiveSetupFlag, npList, 0, npList.length);

		cellsConstructed++;
		progress.setProgress(cellsConstructed * 100 / totalCells);
		cc.instantiate();
	}

	/**
	 * Method to parse a line from the file, breaking it into a List of Strings.
	 * Each field in the file is separated by "|".
	 * Quoted strings are handled properly, as are the escape character, "^".
	 * @param line the text from the file.
	 * @param format descriptions of fields
	 * @return true if there were an error parsing line
	 */
	boolean parseLine(String line, String format)
	{
		stringPieces.clear();
		curPiece = 0;

		int len = line.length();
		int pos = 1;
		StringBuffer sb = new StringBuffer();
		boolean inQuote = false;
		while (pos < len)
		{
			char chr = line.charAt(pos++);
			if (chr == '^')
			{
				sb.append(chr);
				sb.append(line.charAt(pos++));
				continue;
			}
			if (chr == '"') inQuote = !inQuote;
			if (chr == '|' && !inQuote)
			{
				String piece = sb.toString();
				if (stringPieces.size() < format.length() && format.charAt(stringPieces.size()) == 'Q' || line.charAt(0) == 'G')
					piece = unQuote(piece);
				stringPieces.add(piece);
				sb = new StringBuffer();
			} else
			{
				sb.append(chr);
			}
		}
		String piece = sb.toString();
		if (stringPieces.size() < format.length() && format.charAt(stringPieces.size()) == 'Q' || line.charAt(0) == 'G')
			piece = unQuote(piece);
		stringPieces.add(piece);
		if (stringPieces.size() < format.length())
		{
			logError("Declaration '" + line.charAt(0) + "' needs " + format.length() + " fields: " + line);
			return true;
		}
		return false;
	}

	private String nextPiece()
	{
		if (curPiece >= stringPieces.size()) return null;
		return (String)stringPieces.get(curPiece++);
	}

	/**
	 * Returns al the unread pieces.
	 * @return larray of unread pieces
	 */
	private LibraryContents.VariableContents[] varPieces()
	{
		if (curPiece >= stringPieces.size()) return LibraryContents.NULL_VARS_ARRAY;
		LibraryContents.VariableContents[] vars = new LibraryContents.VariableContents[stringPieces.size() - curPiece];
		for (int i = 0; i < vars.length; i++)
		{
			String piece = (String)stringPieces.get(curPiece + i);
			int openPos = 0;
			for(; openPos < piece.length(); openPos++)
			{
				char chr = piece.charAt(openPos);
				if (chr == '^') { openPos++;   continue; }
				if (chr == '(') break;
			}
			if (openPos >= piece.length())
			{
				logError("Badly formed variable (no open parenthesis): " + piece);
				continue;
			}
			String varName = piece.substring(0, openPos);
			int closePos = piece.indexOf(')', openPos);
			if (closePos < 0)
			{
				logError("Badly formed variable (no close parenthesis): " + piece);
				continue;
			}
			String varBits = piece.substring(openPos+1, closePos);
			int objectPos = closePos + 1;
			if (objectPos >= piece.length())
			{
				logError("Variable type missing: " + piece);
				continue;
			}
			char varType = piece.charAt(objectPos++);
			switch (varType)
			{
				case 'B':
				case 'C':
				case 'D':
				case 'E':
				case 'F':
				case 'G':
				case 'H':
				case 'I':
				case 'L':
				case 'O':
				case 'P':
				case 'R':
				case 'S':
				case 'T':
				case 'V':
				case 'Y':
					break; // break from switch
				default:
					logError("Variable type invalid: " + piece);
					continue; // continue loop
			}
			if (objectPos >= piece.length())
			{
				logError("Variable value missing: " + piece);
				continue;
			}
			Object obj;
			if (piece.charAt(objectPos) == '[')
			{
				List objList = new ArrayList();
				objectPos++;
				while (objectPos < piece.length())
				{
					int start = objectPos;
					boolean inQuote = false;
					while (objectPos < piece.length())
					{
						if (inQuote)
						{
							if (piece.charAt(objectPos) == '^')
							{
								objectPos++;
							} else if (piece.charAt(objectPos) == '"')
							{
								inQuote = false;
							}
							objectPos++;
							continue;
						}
						if (piece.charAt(objectPos) == ',' || piece.charAt(objectPos) == ']') break;
						if (piece.charAt(objectPos) == '"')
						{
							inQuote = true;
						}
						objectPos++;
					}
					Object oneObj = getVariableValue(piece.substring(start, objectPos), 0, varType);
					objList.add(oneObj);
					if (piece.charAt(objectPos) == ']') break;
					objectPos++;
				}
				if (objectPos >= piece.length())
				{
					logError("Badly formed array (no closed bracket): " + piece);
					continue;
				}
				else if (objectPos < piece.length() - 1)
				{
					logError("Badly formed array (extra characters after closed bracket): " + piece);
					continue;
				}
				int limit = objList.size();
				Object[] objArray = new Object[limit];
				for(int j=0; j<limit; j++)
					objArray[j] = objList.get(j);
				obj = objArray;
			} else
			{
				// a scalar Variable
				obj = getVariableValue(piece, objectPos, varType);
			}
			vars[i] = libraryContents.newVariableContents(libraryContents.getVariableKeyRef(varName), varBits, varType, obj);
		}
		curPiece = stringPieces.size();
		return vars;
	}

	/**
	 * Method to convert a String to an Object so that it can be stored in a Variable.
	 * @param piece the String to be converted.
	 * @param objectPos the character number in the string to consider.
	 * Note that the string may be larger than the object description, both by having characters
	 * before it, and also by having characters after it.
	 * Therefore, do not assume that the end of the string is the proper termination of the object specification.
	 * @param varType the type of the object to convert (a letter from the file).
	 * @return the Object representation of the given String.
	 */
	private Object getVariableValue(String piece, int objectPos, char varType)
	{
		int colonPos;
		String libName;
		LibraryContents.LibraryRef libRef;
		int secondColonPos;
		String cellName;
		LibraryContents.CellRef cellRef;
		int commaPos;

		switch (varType)
		{
// 			case 'A':		// ArcInst (should delay analysis until database is built!!!)
// 				int colonPos = piece.indexOf(':', objectPos);
// 				if (colonPos < 0)
// 				{
// 					logError("Badly formed Export (missing library colon): " + piece);
// 					break;
// 				}
// 				String libName = piece.substring(objectPos, colonPos);
// 				Library lib = Library.findLibrary(libName);
// 				if (lib == null)
// 				{
// 					logError("Unknown library: " + libName);
// 					break;
// 				}
// 				int secondColonPos = piece.indexOf(':', colonPos+1);
// 				if (secondColonPos < 0)
// 				{
// 					logError("Badly formed Export (missing cell colon): " + piece);
// 					break;
// 				}
// 				String cellName = piece.substring(colonPos+1, secondColonPos);
// 				Cell cell = lib.findNodeProto(cellName);
// 				if (cell == null)
// 				{
// 					logError("Unknown Cell: " + piece);
// 					break;
// 				}
// 				String arcName = piece.substring(secondColonPos+1);
// 				int commaPos = arcName.indexOf(',');
// 				if (commaPos >= 0) arcName = arcName.substring(0, commaPos);
// 				ArcInst ai = cell.findArc(arcName);
// 				if (ai == null)
// 					logError("Unknown ArcInst: " + piece);
// 				return ai;
			case 'B':		// Boolean
				return new Boolean(piece.charAt(objectPos)=='T' ? true : false);
			case 'C':		// Cell (should delay analysis until database is built!!!)
				colonPos = piece.indexOf(':', objectPos);
				if (colonPos < 0)
				{
					logError("Badly formed ArcProto (missing colon): " + piece);
					break;
				}
				libName = piece.substring(objectPos, colonPos);
				libRef = libraryContents.getLibraryRef(libName);
				if (libRef == null)
				{
					logError("Unknown library: " + libName);
					break;
				}
				cellName = piece.substring(colonPos+1);
				commaPos = cellName.indexOf(',');
				if (commaPos >= 0) cellName = cellName.substring(0, commaPos);
				cellRef = libRef.getCellRef(cellName);
				if (cellRef == null)
					logError("Unknown Cell: " + piece);
				return cellRef;
			case 'D':		// Double
				return new Double(TextUtils.atof(piece.substring(objectPos)));
			case 'E':		// Export (should delay analysis until database is built!!!)
				colonPos = piece.indexOf(':', objectPos);
				if (colonPos < 0)
				{
					logError("Badly formed Export (missing library colon): " + piece);
					break;
				}
				libName = piece.substring(objectPos, colonPos);
				libRef = libraryContents.getLibraryRef(libName);
				if (libRef == null)
				{
					logError("Unknown library: " + libName);
					break;
				}
				secondColonPos = piece.indexOf(':', colonPos+1);
				if (secondColonPos < 0)
				{
					logError("Badly formed Export (missing cell colon): " + piece);
					break;
				}
				cellName = piece.substring(colonPos+1, secondColonPos);
				cellRef = libRef.getCellRef(cellName);
				if (cellRef == null)
				{
					logError("Unknown Cell: " + piece);
					break;
				}
				String exportName = piece.substring(secondColonPos+1);
				commaPos = exportName.indexOf(',');
				if (commaPos >= 0) exportName = exportName.substring(0, commaPos);
				return cellRef.getPortProtoRef(exportName);
			case 'F':		// Float
				return new Float((float)TextUtils.atof(piece.substring(objectPos)));
			case 'G':		// Long
				return new Long(TextUtils.atoi(piece.substring(objectPos)));
			case 'H':		// Short
				return new Short((short)TextUtils.atoi(piece.substring(objectPos)));
			case 'I':		// Integer
				return new Integer(TextUtils.atoi(piece.substring(objectPos)));
			case 'L':		// Library (should delay analysis until database is built!!!)
				libName = piece.substring(objectPos);
				commaPos = libName.indexOf(',');
				if (commaPos >= 0) libName = libName.substring(0, commaPos);
				libRef = getLibraryRef(libName);
				return libRef;
// 			case 'N':		// NodeInst (should delay analysis until database is built!!!)
// 				colonPos = piece.indexOf(':', objectPos);
// 				if (colonPos < 0)
// 				{
// 					logError("Badly formed Export (missing library colon): " + piece);
// 					break;
// 				}
// 				libName = piece.substring(objectPos, colonPos);
// 				lib = Library.findLibrary(libName);
// 				if (lib == null)
// 				{
// 					logError("Unknown library: " + libName);
// 					break;
// 				}
// 				secondColonPos = piece.indexOf(':', colonPos+1);
// 				if (secondColonPos < 0)
// 				{
// 					logError("Badly formed Export (missing cell colon): " + piece);
// 					break;
// 				}
// 				cellName = piece.substring(colonPos+1, secondColonPos);
// 				cell = lib.findNodeProto(cellName);
// 				if (cell == null)
// 				{
// 					logError("Unknown Cell: " + piece);
// 					break;
// 				}
// 				String nodeName = piece.substring(secondColonPos+1);
// 				commaPos = nodeName.indexOf(',');
// 				if (commaPos >= 0) nodeName = nodeName.substring(0, commaPos);
// 				NodeInst ni = cell.findNode(nodeName);
// 				if (ni == null)
// 					logError("Unknown NodeInst: " + piece);
// 				return ni;
			case 'O':		// Tool
				String toolName = piece.substring(objectPos);
				commaPos = toolName.indexOf(',');
				if (commaPos >= 0) toolName = toolName.substring(0, commaPos);
				return libraryContents.getToolRef(toolName);
			case 'P':		// PrimitiveNode
				String pnName = piece.substring(objectPos);
				commaPos = pnName.indexOf(',');
				if (commaPos >= 0) pnName = pnName.substring(0, commaPos);
				return getPrimitiveNodeRef(null, pnName);
			case 'R':		// ArcProto
				String apName = piece.substring(objectPos);
				commaPos = apName.indexOf(',');
				if (commaPos >= 0) apName = apName.substring(0, commaPos);
				return getArcProtoRef(null, apName);
			case 'S':		// String
				if (piece.charAt(objectPos) != '"')
				{
					logError("Badly formed string variable (missing open quote): " + piece);
					break;
				}
				StringBuffer sb = new StringBuffer();
				int len = piece.length();
				while (objectPos < len)
				{
					objectPos++;
					if (piece.charAt(objectPos) == '"') break;
					if (piece.charAt(objectPos) == '^')
					{
						objectPos++;
						if (objectPos <= len - 2 && piece.charAt(objectPos) == '\\' && piece.charAt(objectPos+1) == 'n')
						{
							sb.append('\n');
							objectPos++;
							continue;
						}
					}
					sb.append(piece.charAt(objectPos));
				}
				return sb.toString();
			case 'T':		// Technology
				String techName = piece.substring(objectPos);
				commaPos = techName.indexOf(',');
				if (commaPos >= 0) techName = techName.substring(0, commaPos);
				return getTechnologyRef(techName);
			case 'V':		// Point2D
				double x = TextUtils.atof(piece.substring(objectPos));
				int slashPos = piece.indexOf('/', objectPos);
				if (slashPos < 0)
				{
					logError("Badly formed Point2D variable (missing slash): " + piece);
					break;
				}
				double y = TextUtils.atof(piece.substring(slashPos+1));
				return new Point2D.Double(x, y);
			case 'Y':		// Byte
				return new Byte((byte)TextUtils.atoi(piece.substring(objectPos)));
		}
		return null;
	}

	/**
	 * Returns al the unread pieces.
	 * @return larray of unread pieces
	 */
	private String[] stringPieces()
	{
		if (curPiece >= stringPieces.size()) return NULL_STRINGS_ARRAY;
		String[] pieces = new String[stringPieces.size() - curPiece];
		for (int i = 0; i < pieces.length; i++)
			pieces[i] = (String)stringPieces.get(curPiece + i);
		curPiece = stringPieces.size();
		return pieces;
	}

	void setLineNumber(int lineNumber)
	{
		this.lineNumber = lineNumber;
	}

	ErrorLogger.MessageLog logError(String message)
	{
		if (lineReader != null)
			lineNumber = lineReader.getLineNumber();
		Cell cell = null;
		String prefix;
		if (currentCellContents != null)
		{
			cell = currentCellContents.getCell();
			prefix = filePath + ", line " + lineNumber + " (cell " + currentCellContents.getName() + ") ";
		} else
		{
			prefix = filePath + ", line " + lineNumber + ", ";
		}
		return errorLogger.logError(prefix + message, cell, -1);
	}

	ErrorLogger.MessageLog logWarning(String message)
	{
		if (lineReader != null)
			lineNumber = lineReader.getLineNumber();
		Cell cell = null;
		String prefix;
		if (currentCellContents != null)
		{
			cell = currentCellContents.getCell();
			prefix = filePath + ", line " + lineNumber + " (cell " + currentCellContents.getName() + ") ";
		} else
		{
			prefix = filePath + ", line " + lineNumber + ", ";
		}
		return errorLogger.logWarning(prefix + message, cell, -1);
	}

	void setupProgress(int numToProcess)
	{
		JELIB1.numToProcess = numToProcess;
		numProcessed = 0;
	}

	void showProgress(int lineNumber)
	{
		numProcessed++;
		if ((numProcessed%100) == 0) progress.setProgress(numProcessed * 100 / numToProcess);
		setLineNumber(lineNumber);
	}
		
	private String unQuote(String line)
	{
		if (line.indexOf('^') < 0) return line;
		StringBuffer sb = new StringBuffer();
		int len = line.length();
		for(int i=0; i<len; i++)
		{
			char chr = line.charAt(i);
			if (chr == '^')
			{
				i++;
				if (i >= len) break;
				chr = line.charAt(i);
			}
			sb.append(chr);
		}
		return sb.toString();
	}
}
