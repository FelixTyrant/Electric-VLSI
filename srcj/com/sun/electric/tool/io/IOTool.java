/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IOTool.java
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

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Tool;

import java.util.Iterator;
import java.util.Date;
import javax.print.PrintServiceLookup;

/**
 * This class manages reading files in different formats.
 * The class is subclassed by the different file readers.
 */
public class IOTool extends Tool
{
	/** the IO tool. */										public static IOTool tool = new IOTool();

	/** Varible key for true library of fake cell. */		public static final Variable.Key IO_TRUE_LIBRARY = ElectricObject.newKey("IO_true_library");

	// ---------------------- private and protected methods -----------------

	/**
	 * The constructor sets up the I/O tool.
	 */
	protected IOTool()
	{
		super("io");
	}

	/**
	 * Method to set name of Geometric object.
	 * @param geom the Geometric object.
	 * @param value name of object
	 * @param type type mask.
	 */
	protected Name makeGeomName(Geometric geom, Object value, int type)
	{
		if (value == null || !(value instanceof String)) return null;
		String str = (String)value;
		Name name = Name.findName(str);
		if ((type & ELIBConstants.VDISPLAY) != 0)
		{
			if (name.isTempname())
			{
				String newS = "";
				for (int i = 0; i < str.length(); i++)
				{
					char c = str.charAt(i);
					if (c == '@') c = '_';
					newS += c;
				}
				name = Name.findName(newS);
			}
		} else if (!name.isTempname()) return null;
		return name;
	}

	private static String [] fontNames = null;

	/**
	 * Method to grab font associations that were stored on a Library.
	 * The font associations are used later to convert indices to true font names and numbers.
	 * @param lib the Library to examine.
	 */
	public static void getFontAssociationVariable(Library lib)
	{
		fontNames = null;
		Variable var = lib.getVar(Library.FONT_ASSOCIATIONS, String[].class);
		if (var == null) return;

		String [] associationArray = (String [])var.getObject();
		int maxAssociation = 0;
		for(int i=0; i<associationArray.length; i++)
		{
			int fontNumber = TextUtils.atoi(associationArray[i]);
			if (fontNumber > maxAssociation) maxAssociation = fontNumber;
		}
		if (maxAssociation <= 0) return;

		fontNames = new String[maxAssociation];
		for(int i=0; i<maxAssociation; i++) fontNames[i] = null;
		for(int i=0; i<associationArray.length; i++)
		{
			int fontNumber = TextUtils.atoi(associationArray[i]);
			if (fontNumber <= 0) continue;
			int slashPos = associationArray[i].indexOf('/');
			if (slashPos < 0) continue;
			fontNames[fontNumber-1] = associationArray[i].substring(slashPos+1);
		}

		// data cached: delete the association variable
		lib.delVar(Library.FONT_ASSOCIATIONS);
	}

	public static void fixVariableFont(ElectricObject eobj)
	{
		if (eobj == null) return;
		for(Iterator it = eobj.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			fixTextDescriptorFont(var.getTextDescriptor());
		}
	}

	/**
	 * Method to convert the font number in a TextDescriptor to the proper value as
	 * cached in the Library.  The caching is examined by "getFontAssociationVariable()".
	 * @param td the TextDescriptor to convert.
	 */
	public static void fixTextDescriptorFont(TextDescriptor td)
	{
		int fontNumber = td.getFace();
		if (fontNumber == 0) return;

		if (fontNames == null) fontNumber = 0; else
		{
			if (fontNumber <= fontNames.length)
			{
				String fontName = fontNames[fontNumber-1];
				TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(fontName);
				if (af == null) fontNumber = 0; else
					fontNumber = af.getIndex();
			}
		}
		td.setFace(fontNumber);
	}

	/****************************** GENERAL IO PREFERENCES ******************************/

	private static Pref cacheBackupRedundancy = Pref.makeIntPref("OutputBackupRedundancy", IOTool.tool.prefs, 0);
	/**
	 * Method to tell what kind of redundancy to apply when writing library files.
	 * The value is:
	 * 0 for no backup (just overwrite the old file) [the default];
	 * 1 for 1-level backup (rename the old file to have a "~" at the end);
	 * 2 for full history backup (rename the old file to have date information in it).
	 * @return the level of redundancy to apply when writing library files.
	 */
	public static int getBackupRedundancy() { return cacheBackupRedundancy.getInt(); }
	/**
	 * Method to set the level of redundancy to apply when writing library files.
	 * The value is:
	 * 0 for no backup (just overwrite the old file);
	 * 1 for 1-level backup (rename the old file to have a "~" at the end);
	 * 2 for full history backup (rename the old file to have date information in it).
	 * @param r the level of redundancy to apply when writing library files.
	 */
	public static void setBackupRedundancy(int r) { cacheBackupRedundancy.setInt(r); }

	/****************************** GENERAL OUTPUT PREFERENCES ******************************/

	private static Pref cacheUseCopyrightMessage = Pref.makeBooleanPref("UseCopyrightMessage", IOTool.tool.prefs, false);
    static { cacheUseCopyrightMessage.attachToObject(IOTool.tool, "IO Options, Copyright tab", "Use Copyright Message"); }
	/**
	 * Method to tell whether to add the copyright message to output decks.
	 * The default is "false".
	 * @return true to add the copyright message to output decks.
	 */
	public static boolean isUseCopyrightMessage() { return cacheUseCopyrightMessage.getBoolean(); }
	/**
	 * Method to set whether to add the copyright message to output decks.
	 * @param u true to add the copyright message to output decks.
	 */
	public static void setUseCopyrightMessage(boolean u) { cacheUseCopyrightMessage.setBoolean(u); }

	private static Pref cacheCopyrightMessage = Pref.makeStringPref("CopyrightMessage", IOTool.tool.prefs, "");
    static { cacheUseCopyrightMessage.attachToObject(IOTool.tool, "IO Options, Copyright tab", "Copyright Message"); }
	/**
	 * Method to tell the copyright message that will be added to output decks.
	 * The default is "".
	 * @return the copyright message that will be added to output decks.
	 */
	public static String getCopyrightMessage() { return cacheCopyrightMessage.getString(); }
	/**
	 * Method to set the copyright message that will be added to output decks.
	 * @param m the copyright message that will be added to output decks.
	 */
	public static void setCopyrightMessage(String m) { cacheCopyrightMessage.setString(m); }

	private static Pref cachePlotArea = Pref.makeIntPref("PlotArea", IOTool.tool.prefs, 0);
	/**
	 * Method to tell the area of the screen to plot for printing/PostScript/HPGL.
	 * @return the area of the screen to plot for printing/PostScript/HPGL:
	 * 0=plot the entire cell (the default);
	 * 1=plot only the highlighted area;
	 * 2=plot only the displayed window.
	 */
	public static int getPlotArea() { return cachePlotArea.getInt(); }
	/**
	 * Method to set the area of the screen to plot for printing/PostScript/HPGL.
	 * @param pa the area of the screen to plot for printing/PostScript/HPGL.
	 * 0=plot the entire cell;
	 * 1=plot only the highlighted area;
	 * 2=plot only the displayed window.
	 */
	public static void setPlotArea(int pa) { cachePlotArea.setInt(pa); }

	private static Pref cachePlotDate = Pref.makeBooleanPref("PlotDate", IOTool.tool.prefs, false);
	/**
	 * Method to tell whether to plot the date in PostScript/HPGL output.
	 * The default is "false".
	 * @return whether to plot the date in PostScript/HPGL output.
	 */
	public static boolean isPlotDate() { return cachePlotDate.getBoolean(); }
	/**
	 * Method to set whether to plot the date in PostScript/HPGL output.
	 * @param pd true to plot the date in PostScript/HPGL output.
	 */
	public static void setPlotDate(boolean pd) { cachePlotDate.setBoolean(pd); }

	private static Pref cachePrinterName = null;

	private static Pref getCachePrinterName()
	{
		if (cachePrinterName == null)
		{
			cachePrinterName = Pref.makeStringPref("PrinterName", IOTool.tool.prefs,
				PrintServiceLookup.lookupDefaultPrintService().getName());
		}
		return cachePrinterName;
	}

	/**
	 * Method to tell the default printer name to use.
	 * The default is "".
	 * @return the default printer name to use.
	 */
	public static String getPrinterName() { return getCachePrinterName().getString(); }
	/**
	 * Method to set the default printer name to use.
	 * @param pName the default printer name to use.
	 */
	public static void setPrinterName(String pName) { getCachePrinterName().setString(pName); }

	/****************************** CIF OUTPUT PREFERENCES ******************************/

	private static Pref cacheCIFOutMimicsDisplay = Pref.makeBooleanPref("CIFMimicsDisplay", IOTool.tool.prefs, false);
    static { cacheCIFOutMimicsDisplay.attachToObject(IOTool.tool, "IO Options, CIF tab", "Output Mimics Display"); }

	/**
	 * Method to tell whether CIF Output mimics the display.
	 * To mimic the display, unexpanded cell instances are described as black boxes,
	 * instead of calls to their contents.
	 * The default is "false".
	 * @return true if CIF Output mimics the display.
	 */
	public static boolean isCIFOutMimicsDisplay() { return cacheCIFOutMimicsDisplay.getBoolean(); }
	/**
	 * Method to set whether CIF Output mimics the display.
	 * To mimic the display, unexpanded cell instances are described as black boxes,
	 * instead of calls to their contents.
	 * @param on true if CIF Output mimics the display.
	 */
	public static void setCIFOutMimicsDisplay(boolean on) { cacheCIFOutMimicsDisplay.setBoolean(on); }

	private static Pref cacheCIFOutMergesBoxes = Pref.makeBooleanPref("CIFMergesBoxes", IOTool.tool.prefs, false);
    static { cacheCIFOutMergesBoxes.attachToObject(IOTool.tool, "IO Options, CIF tab", "Output Merges Boxes"); }
	/**
	 * Method to tell whether CIF Output merges boxes into complex polygons.
	 * This takes more time but produces a smaller output file.
	 * The default is "false".
	 * @return true if CIF Output merges boxes into complex polygons.
	 */
	public static boolean isCIFOutMergesBoxes() { return cacheCIFOutMergesBoxes.getBoolean(); }
	/**
	 * Method to set whether CIF Output merges boxes into complex polygons.
	 * This takes more time but produces a smaller output file.
	 * @param on true if CIF Output merges boxes into complex polygons.
	 */
	public static void setCIFOutMergesBoxes(boolean on) { cacheCIFOutMergesBoxes.setBoolean(on); }

	private static Pref cacheCIFOutInstantiatesTopLevel = Pref.makeBooleanPref("CIFInstantiatesTopLevel", IOTool.tool.prefs, true);
    static { cacheCIFOutInstantiatesTopLevel.attachToObject(IOTool.tool, "IO Options, CIF tab", "Output Instantiates Top Level"); }
	/**
	 * Method to tell whether CIF Output instantiates the top-level.
	 * When this happens, a CIF "call" to the top cell is emitted.
	 * The default is "true".
	 * @return true if CIF Output merges boxes into complex polygons.
	 */
	public static boolean isCIFOutInstantiatesTopLevel() { return cacheCIFOutInstantiatesTopLevel.getBoolean(); }
	/**
	 * Method to set whether CIF Output merges boxes into complex polygons.
	 * When this happens, a CIF "call" to the top cell is emitted.
	 * @param on true if CIF Output merges boxes into complex polygons.
	 */
	public static void setCIFOutInstantiatesTopLevel(boolean on) { cacheCIFOutInstantiatesTopLevel.setBoolean(on); }

	private static Pref cacheCIFOutCheckResolution = Pref.makeBooleanPref("CIFCheckResolution", IOTool.tool.prefs, false);
	/**
	 * Method to tell whether to report CIF resolution errors.
	 * The default is "false".
	 * @return true to report CIF resolution errors.
	 */
	public static boolean isCIFOutCheckResolution() { return cacheCIFOutCheckResolution.getBoolean(); }
	/**
	 * Method to set whether to report CIF resolution errors.
	 * @param c whether to report CIF resolution errors.
	 */
	public static void setCIFOutCheckResolution(boolean c) { cacheCIFOutCheckResolution.setBoolean(c); }

	private static Pref cacheCIFOutResolution = Pref.makeDoublePref("CIFResolution", IOTool.tool.prefs, 0);
	/**
	 * Method to tell the minimum CIF Output resolution.
	 * This is the smallest feature size that can be safely generated.
	 * The default is "0".
	 * @return the minimum CIF Output resolution.
	 */
	public static double getCIFOutResolution() { return cacheCIFOutResolution.getDouble(); }
	/**
	 * Method to set the minimum CIF Output resolution.
	 * This is the smallest feature size that can be safely generated.
	 * @param r the minimum CIF Output resolution.
	 */
	public static void setCIFOutResolution(double r) { cacheCIFOutResolution.setDouble(r); }

	/****************************** GDS OUTPUT PREFERENCES ******************************/

	private static Pref cacheGDSOutMergesBoxes = Pref.makeBooleanPref("GDSMergesBoxes", IOTool.tool.prefs, false);
    static { cacheGDSOutMergesBoxes.attachToObject(IOTool.tool, "IO Options, GDS tab", "Output merges Boxes"); }
	/**
	 * Method to tell whether GDS Output merges boxes into complex polygons.
	 * This takes more time but produces a smaller output file.
	 * The default is "false".
	 * @return true if GDS Output merges boxes into complex polygons.
	 */
	public static boolean isGDSOutMergesBoxes() { return cacheGDSOutMergesBoxes.getBoolean(); }
	/**
	 * Method to set whether GDS Output merges boxes into complex polygons.
	 * This takes more time but produces a smaller output file.
	 * @param on true if GDS Output merges boxes into complex polygons.
	 */
	public static void setGDSOutMergesBoxes(boolean on) { cacheGDSOutMergesBoxes.setBoolean(on); }

	private static Pref cacheGDSOutWritesExportPins = Pref.makeBooleanPref("GDSWritesExportPins", IOTool.tool.prefs, false);
    static { cacheGDSOutWritesExportPins.attachToObject(IOTool.tool, "IO Options, GDS tab", "Output writes export Pins"); }
	/**
	 * Method to tell whether GDS Output writes pins at Export locations.
	 * Some systems can use this information to reconstruct export locations.
	 * The default is "false".
	 * @return true if GDS Output writes pins at Export locations.
	 */
	public static boolean isGDSOutWritesExportPins() { return cacheGDSOutWritesExportPins.getBoolean(); }
	/**
	 * Method to set whether GDS Output writes pins at Export locations.
	 * Some systems can use this information to reconstruct export locations.
	 * @param on true if GDS Output writes pins at Export locations.
	 */
	public static void setGDSOutWritesExportPins(boolean on) { cacheGDSOutWritesExportPins.setBoolean(on); }

	private static Pref cacheGDSOutUpperCase = Pref.makeBooleanPref("GDSOutputUpperCase", IOTool.tool.prefs, false);
    static { cacheGDSOutUpperCase.attachToObject(IOTool.tool, "IO Options, GDS tab", "Output all upper-case"); }
	/**
	 * Method to tell whether GDS Output makes all text upper-case.
	 * Some systems insist on this.
	 * The default is "false".
	 * @return true if GDS Output makes all text upper-case.
	 */
	public static boolean isGDSOutUpperCase() { return cacheGDSOutUpperCase.getBoolean(); }
	/**
	 * Method to set whether GDS Output makes all text upper-case.
	 * Some systems insist on this.
	 * @param on true if GDS Output makes all text upper-case.
	 */
	public static void setGDSOutUpperCase(boolean on) { cacheGDSOutUpperCase.setBoolean(on); }

	private static Pref cacheGDSOutDefaultTextLayer = Pref.makeIntPref("GDSDefaultTextLayer", IOTool.tool.prefs, 230);
    static { cacheGDSOutDefaultTextLayer.attachToObject(IOTool.tool, "IO Options, GDS tab", "Output default text layer"); }
	/**
	 * Method to tell the default GDS layer to use for the text of Export pins.
	 * Export pins are annotated with text objects on this layer.
	 * If this is negative, do not write Export pins.
	 * The default is "230".
	 * @return the default GDS layer to use for the text of Export pins.
	 */
	public static int getGDSOutDefaultTextLayer() { return cacheGDSOutDefaultTextLayer.getInt(); }
	/**
	 * Method to set the default GDS layer to use for the text of Export pins.
	 * Export pins are annotated with text objects on this layer.
	 * @param num the default GDS layer to use for the text of Export pins.
	 * If this is negative, do not write Export pins.
	 */
	public static void setGDSOutDefaultTextLayer(int num) { cacheGDSOutDefaultTextLayer.setInt(num); }

	/****************************** POSTSCRIPT OUTPUT PREFERENCES ******************************/

	private static Pref cachePrintEncapsulated = Pref.makeBooleanPref("PostScriptEncapsulated", IOTool.tool.prefs, false);
	/**
	 * Method to tell whether PostScript Output is Encapsulated.
	 * Encapsulated PostScript can be inserted into other documents.
	 * The default is "false".
	 * @return true if PostScript Output is Encapsulated.
	 */
	public static boolean isPrintEncapsulated() { return cachePrintEncapsulated.getBoolean(); }
	/**
	 * Method to set whether PostScript Output is Encapsulated.
	 * Encapsulated PostScript can be inserted into other documents.
	 * @param on true if PostScript Output is Encapsulated.
	 */
	public static void setPrintEncapsulated(boolean on) { cachePrintEncapsulated.setBoolean(on); }

	private static Pref cachePrintForPlotter = Pref.makeBooleanPref("PostScriptForPlotter", IOTool.tool.prefs, false);
	/**
	 * Method to tell whether PostScript Output is for a plotter.
	 * Plotters have width, but no height, since they are continuous feed.
	 * The default is "false".
	 * @return true if PostScript Output is for a plotter.
	 */
	public static boolean isPrintForPlotter() { return cachePrintForPlotter.getBoolean(); }
	/**
	 * Method to set whether PostScript Output is for a plotter.
	 * Plotters have width, but no height, since they are continuous feed.
	 * @param on true if PostScript Output is for a plotter.
	 */
	public static void setPrintForPlotter(boolean on) { cachePrintForPlotter.setBoolean(on); }

	private static Pref cachePrintWidth = Pref.makeDoublePref("PostScriptWidth", IOTool.tool.prefs, 8.5);
	/**
	 * Method to tell the width of PostScript Output.
	 * The width is in inches.
	 * The default is "8.5".
	 * @return the width of PostScript Output.
	 */
	public static double getPrintWidth() { return cachePrintWidth.getDouble(); }
	/**
	 * Method to set the width of PostScript Output.
	 * The width is in inches.
	 * @param wid the width of PostScript Output.
	 */
	public static void setPrintWidth(double wid) { cachePrintWidth.setDouble(wid); }

	private static Pref cachePrintHeight = Pref.makeDoublePref("PostScriptHeight", IOTool.tool.prefs, 11);
	/**
	 * Method to tell the height of PostScript Output.
	 * The height is in inches, and only applies if printing (not plotting).
	 * The default is "11".
	 * @return the height of PostScript Output.
	 */
	public static double getPrintHeight() { return cachePrintHeight.getDouble(); }
	/**
	 * Method to set the height of PostScript Output.
	 * The height is in inches, and only applies if printing (not plotting).
	 * @param hei the height of PostScript Output.
	 */
	public static void setPrintHeight(double hei) { cachePrintHeight.setDouble(hei); }

	private static Pref cachePrintMargin = Pref.makeDoublePref("PostScriptMargin", IOTool.tool.prefs, 0.75);
	/**
	 * Method to tell the margin of PostScript Output.
	 * The margin is in inches and insets from all sides.
	 * The default is "0.75".
	 * @return the margin of PostScript Output.
	 */
	public static double getPrintMargin() { return cachePrintMargin.getDouble(); }
	/**
	 * Method to set the margin of PostScript Output.
	 * The margin is in inches and insets from all sides.
	 * @param mar the margin of PostScript Output.
	 */
	public static void setPrintMargin(double mar) { cachePrintMargin.setDouble(mar); }

	private static Pref cachePrintRotation = Pref.makeIntPref("PostScriptRotation", IOTool.tool.prefs, 0);
	/**
	 * Method to tell the rotation of PostScript Output.
	 * The plot can be normal or rotated 90 degrees to better fit the paper.
	 * @return the rotation of PostScript Output:
	 * 0=no rotation (the default);
	 * 1=rotate 90 degrees;
	 * 2=rotate automatically to fit best.
	 */
	public static int getPrintRotation() { return cachePrintRotation.getInt(); }
	/**
	 * Method to set the rotation of PostScript Output.
	 * The plot can be normal or rotated 90 degrees to better fit the paper.
	 * @param rot the rotation of PostScript Output.
	 * 0=no rotation;
	 * 1=rotate 90 degrees;
	 * 2=rotate automatically to fit best.
	 */
	public static void setPrintRotation(int rot) { cachePrintRotation.setInt(rot); }

	private static Pref cachePrintColorMethod = Pref.makeIntPref("PostScriptColorMethod", IOTool.tool.prefs, 0);
	/**
	 * Method to tell the color method of PostScript Output.
	 * @return the color method of PostScript Output:
	 * 0=Black & White (the default);
	 * 1=Color (solid);
	 * 2=Color (stippled);
	 * 3=Color (merged).
	 */
	public static int getPrintColorMethod() { return cachePrintColorMethod.getInt(); }
	/**
	 * Method to set the color method of PostScript Output.
	 * @param cm the color method of PostScript Output.
	 * 0=Black & White;
	 * 1=Color (solid);
	 * 2=Color (stippled);
	 * 3=Color (merged).
	 */
	public static void setPrintColorMethod(int cm) { cachePrintColorMethod.setInt(cm); }

	public static final Variable.Key POSTSCRIPT_EPS_SCALE = ElectricObject.newKey("IO_postscript_EPS_scale");
	/**
	 * Method to tell the EPS scale of a given Cell.
	 * @param cell the cell to query.
	 * @return the EPS scale of that Cell.
	 */
	public static double getPrintEPSScale(Cell cell)
	{
		Variable var = cell.getVar(POSTSCRIPT_EPS_SCALE);
		if (var != null)
		{
			Object obj = var.getObject();
			String desc = obj.toString();
			double epsScale = TextUtils.atof(desc);
			return epsScale;
		}
		return 1;
	}
	/**
	 * Method to set the EPS scale of a given Cell.
	 * @param cell the cell to modify.
	 * @param scale the EPS scale of that Cell.
	 */
	public static void setPrintEPSScale(Cell cell, double scale)
	{
		tool.setVarInJob(cell, POSTSCRIPT_EPS_SCALE, new Double(scale));
	}

	public static final Variable.Key POSTSCRIPT_FILENAME = ElectricObject.newKey("IO_postscript_filename");
	/**
	 * Method to tell the EPS synchronization file of a given Cell.
	 * During automatic synchronization of PostScript, any cell changed more
	 * recently than the date on this file will cause that file to be generated
	 * from the Cell.
	 * @param cell the cell to query.
	 * @return the EPS synchronization file of that Cell.
	 */
	public static String getPrintEPSSynchronizeFile(Cell cell)
	{
		Variable var = cell.getVar(POSTSCRIPT_FILENAME);
		if (var != null)
		{
			Object obj = var.getObject();
			String desc = obj.toString();
			return desc;
		}
		return "";
	}
	/**
	 * Method to set the EPS synchronization file of a given Cell.
	 * During automatic synchronization of PostScript, any cell changed more
	 * recently than the date on this file will cause that file to be generated
	 * from the Cell.
	 * @param cell the cell to modify.
	 * @param syncFile the EPS synchronization file to associate with that Cell.
	 */
	public static void setPrintEPSSynchronizeFile(Cell cell, String syncFile)
	{
		tool.setVarInJob(cell, POSTSCRIPT_FILENAME, syncFile);
	}

	public static final Variable.Key POSTSCRIPT_FILEDATE = ElectricObject.newKey("IO_postscript_filedate");
	/**
	 * Method to tell the EPS synchronization file of a given Cell.
	 * During automatic synchronization of PostScript, any cell changed more
	 * recently than the date on this file will cause that file to be generated
	 * from the Cell.
	 * @param cell the cell to query.
	 * @return the EPS synchronization file of that Cell.
	 */
	public static Date getPrintEPSSavedDate(Cell cell)
	{
		Variable varDate = cell.getVar(POSTSCRIPT_FILEDATE, Integer[].class);
		if (varDate == null) return null;
		Integer [] lastSavedDateAsInts = (Integer [])varDate.getObject();
		long lastSavedDateInSeconds = ((long)lastSavedDateAsInts[0].intValue() << 32) |
			(lastSavedDateAsInts[1].intValue() & 0xFFFFFFFF);
		Date lastSavedDate = new Date(lastSavedDateInSeconds);
		return lastSavedDate;
	}
	/**
	 * Method to set the EPS synchronization file of a given Cell.
	 * During automatic synchronization of PostScript, any cell changed more
	 * recently than the date on this file will cause that file to be generated
	 * from the Cell.
	 * @param cell the cell to modify.
	 * @param date the EPS synchronization date to associate with that Cell.
	 */
	public static void setPrintEPSSavedDate(Cell cell, Date date)
	{
		long iVal = date.getTime();
		Integer [] dateArray = new Integer[2];
		dateArray[0] = new Integer((int)(iVal >> 32));
		dateArray[1] = new Integer((int)(iVal & 0xFFFFFFFF));
		tool.setVarInJob(cell, POSTSCRIPT_FILEDATE, dateArray);
	}
}
