/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ToolSettings.java
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
package com.sun.electric.tool;

import com.sun.electric.StartupPrefs;
import com.sun.electric.database.text.Setting;

/**
 *
 */
public class ToolSettings {
	/**
	 * Returns project Setting Group of a tool.
     * @param groupName name of a Setting Group
	 * @return project Setting Group of a tool.
	 */
    public static Setting.Group getToolSettings(String groupName) {
        return t.rootSettingGroup.node(groupName);
    }

	/**
	 * Returns project Setting to tell default technique in Tech Palette.
	 * @return project Setting to tell default technique in Tech Palette.
	 */
	public static Setting getDefaultTechnologySetting() { return t.cacheDefaultTechnology; }
	/**
	 * Returns project Setting to tell the layout Technology to use when schematics are found.
	 * This is important in Spice deck generation (for example) because the Spice primitives may
	 * say "2x3" on them, but a real technology (such as "mocmos") must be found to convert these pure
	 * numbers to real spacings for the deck.
	 * @return project Setting to tell the Technology to use when schematics are found.
	 */
	public static Setting getSchematicTechnologySetting() { return t.cacheSchematicTechnology; }
	/**
	 * Returns project Setting to tell whether to include the date and Electric version in output files.
	 * @return project Setting to tell whether to include the date and Electric version in output files.
	 */
	public static Setting getIncludeDateAndVersionInOutputSetting() { return t.cacheIncludeDateAndVersionInOutput; }
	/**
	 * Method to tell whether the process is a PWell process. If true, it will ignore the pwell spacing rule.
	 * The default is "true".
	 * @return true if the process is PWell
	 */
	public static Setting getPWellProcessLayoutTechnologySetting() {return t.cachePWellProcess;}
	/**
	 * Returns project Setting with additional technologies.
	 * @return project Setting with additional technologies.
	 */
	public static Setting getSoftTechnologiesSetting() { return t.cacheSoftTechnologies; }

	/**
	 * Returns project Setting to tell whether resistors are ignored in the circuit.
	 * When ignored, they appear as a "short", connecting the two sides.
	 * When included, they appear as a component with different networks on either side.
	 * Returns project Setting to tell whether resistors are ignored in the circuit.
	 */
	public static Setting getIgnoreResistorsSetting() { return t.cacheIgnoreResistors; }

    /**
	 * Returns project Setting to tell whether to add the copyright message to output decks.
	 * @return project Setting to tell whether to add the copyright message to output decks.
	 */
	public static Setting getUseCopyrightMessageSetting() { return t.cacheUseCopyrightMessage; }
	/**
	 * Returns project Setting to tell the copyright message that will be added to output decks.
	 * @return project Setting to tell the copyright message that will be added to output decks.
	 */
	public static Setting getCopyrightMessageSetting() { return t.cacheCopyrightMessage; }
	/**
	 * Returns Setting to tell whether CIF Output mimics the display.
	 * To mimic the display, unexpanded cell instances are described as black boxes,
	 * instead of calls to their contents.
	 * @return Setting to tell whether CIF Output mimics the display.
	 */
	public static Setting getCIFOutMimicsDisplaySetting() { return t.cacheCIFMimicsDisplay; }
	/**
	 * Returns Setting to tell whether CIF Output merges boxes into complex polygons.
	 * This takes more time but produces a smaller output file.
	 * @return Setting to tell whether CIF Output merges boxes into complex polygons.
	 */
	public static Setting getCIFOutMergesBoxesSetting() { return t.cacheCIFMergesBoxes; }
	/**
	 * Returns Setting to tell whether CIF Output merges boxes into complex polygons.
	 * When this happens, a CIF "call" to the top cell is emitted.
	 * @return Setting to tell whether CIF Output merges boxes into complex polygons.
	 */
	public static Setting getCIFOutInstantiatesTopLevelSetting() { return t.cacheCIFInstantiatesTopLevel; }
	/**
	 * Returns Setting to tell the scale factor to use for CIF Output.
	 * The scale factor is used in cell headers to avoid precision errors.
	 * @return Setting to tell the scale factor to use for CIF Output.
	 */
	public static Setting getCIFOutScaleFactor() { return t.cacheCIFOutScaleFactor; }
	/**
	 * Returns Setting to tell whether GDS Output merges boxes into complex polygons.
	 * This takes more time but produces a smaller output file.
	 * @return Setting to tell if GDS Output merges boxes into complex polygons.
	 */
	public static Setting getGDSOutMergesBoxesSetting() { return t.cacheGDSMergesBoxes; }
	/**
	 * Returns Setting to tell whether GDS Output writes pins at Export locations.
	 * Some systems can use this information to reconstruct export locations.
	 * @return Setting to tell whether GDS Output writes pins at Export locations.
	 */
	public static Setting getGDSOutWritesExportPinsSetting() { return t.cacheGDSWritesExportPins; }
	/**
	 * Returns Setting to tell whether GDS Output makes all text upper-case.
	 * Some systems insist on this.
	 * @return Setting to tell whether GDS Output makes all text upper-case.
	 */
	public static Setting getGDSOutUpperCaseSetting() { return t.cacheGDSOutputUpperCase; }
	/**
	 * Returns Setting to tell the default GDS layer to use for the text of Export pins.
	 * Export pins are annotated with text objects on this layer.
	 * If this is negative, do not write Export pins.
	 * @return Setting to tell to set the default GDS layer to use for the text of Export pins.
	 */
	public static Setting getGDSOutDefaultTextLayerSetting() { return t.cacheGDSDefaultTextLayer; }
	/**
	 * Returns Setting to tell the state of whether the GDS writer converts brackets
	 * to underscores in export names.
	 * @return Setting to tell the state of whether the GDS writer converts brackets
	 */
	public static Setting getGDSOutputConvertsBracketsInExportsSetting() { return t.cacheGDSOutputConvertsBracketsInExports; }
	/**
	 * Returns Setting to tell the maximum length (number of chars) for Cell names in the GDS output file
	 * @return Setting to tell the maximum length (number of chars) for Cell names in the GDS output file
	 */
	public static Setting getGDSCellNameLenMaxSetting() { return t.cacheGDSCellNameLenMax; }
	/**
	 * Method to set the scale to be applied when reading GDS.
	 * @return the scale to be applied when reading GDS.
	 */
	public static Setting getGDSInputScaleSetting() { return t.cacheGDSInputScale; }
	/**
	 * Returns project Settings to tell the DXF scale.
	 * The DXF scale is:
	 * <UL>
	 * <LI>-3: GigaMeters
	 * <LI>-2: MegaMeters
	 * <LI>-1: KiloMeters
	 * <LI>0: Meters
	 * <LI>1: MilliMeters
	 * <LI>2: MicroMeters
	 * <LI>3: NanoMeters
	 * <LI>4: PicoMeters
	 * <LI>5: FemtoMeters
	 * </UL>
	 * @return project Settings to tell the DXF scale.
	 */
	public static Setting getDXFScaleSetting() { return t.cacheDXFScale; }

	/**
	 * Returns project Setting to tell whether to use local settings for Logical Effort
	 * @return project Setting to tell whether to use local settings for Logical Effort
	 */
	public static Setting getUseLocalSettingsSetting() { return t.cacheUseLocalSettings; }
	/**
     * Returns project Setting to tell the Global Fanout for Logical Effort.
     * @return project Setting to tell the Global Fanout for Logical Effort.
	 */
	public static Setting getGlobalFanoutSetting() { return t.cacheGlobalFanout; }
	/**
	 * Returns project Setting to tell the Convergence Epsilon value for Logical Effort.
	 * @return project Setting to tell the Convergence Epsilon value for Logical Effort.
	 */
	public static Setting getConvergenceEpsilonSetting() { return t.cacheConvergenceEpsilon; }
	/**
	 * Returns project Setting to tell the maximum number of iterations for Logical Effort.
	 * @return project Setting to tell the maximum number of iterations for Logical Effort.
	 */
	public static Setting getMaxIterationsSetting() { return t.cacheMaxIterations; }
	/**
	 * Returns project Setting to tell the keeper size ratio for Logical Effort.
	 * @return project Setting to tell the keeper size ratio for Logical Effort.
	 */
	public static Setting getKeeperRatioSetting() { return t.cacheKeeperRatio; }

	/**
	 * Returns setting to tell whether Verilog deck generation should use the Assign statement.
	 * @return setting to tell whether Verilog deck generation should use the Assign statement.
	 */
	public static Setting getVerilogUseAssignSetting() { return t.cacheVerilogUseAssign; }
	/**
	 * Returns setting to tell whether Verilog deck generation should use Trireg by default.
	 * The alternative is to use the "wire" statement.
	 * @return setting to tell whether Verilog deck generation should use Trireg by default.
	 */
	public static Setting getVerilogUseTriregSetting() { return t.cacheVerilogUseTrireg; }

    public static Setting getFoundrySetting() { return t.cachefoundry; }
    public static Setting getEnableNCCSetting() {return t.cacheenableNCC;}
    public static Setting getSizeQuantizationErrorSetting() { return t.cachequantError; }
    public static Setting getMaxMosWidthSetting() {return t.cachemaxmos;}
    public static Setting getVddYSetting() {return t.cachevddy;}
    public static Setting getGndYSetting() {return t.cachegndy;}
    public static Setting getNmosWellHeightSetting() {return t.cachenheight;}
    public static Setting getPmosWellHeightSetting() {return t.cachepheight;}
    public static Setting getSimpleNameSetting() {return t.cachesimpleName;}

    public static Setting getGlobalSDCCommandsSetting() { return t.cacheGlobalSDCCommands; }
    public static Setting getNumWorstPathsSetting() { return t.cacheNumWorstPaths; }

    private static ToolSettings t = new ToolSettings();

    private Setting.Group curXmlGroup;
    private String curPrefGroup;

    private final Setting.RootGroup rootSettingGroup = new Setting.RootGroup();

    { tool("userTool", "/com/sun/electric/tool/user"); }
	private final Setting cacheDefaultTechnology = makeStringSetting("DefaultTechnology", "Technology tab", "Default Technology for editing", "mocmos");
	private final Setting cacheSchematicTechnology = makeStringSetting("SchematicTechnology", "Technology tab", "Schematics use scale values from this technology", "mocmos");
	private final Setting cacheIncludeDateAndVersionInOutput = makeBooleanSetting("IncludeDateAndVersionInOutput", "Netlists tab", "Include date and version in output", true);
	private final Setting cachePWellProcess = makeBooleanSetting("PWellProcess", "Technology tab", "Define Layout Technology as a PWell process", true);
    private final Setting cacheSoftTechnologies = makeStringSetting(StartupPrefs.SoftTechnologiesKey, "Technology tab", "A list of added Xml Technologies", StartupPrefs.SoftTechnologiesDef);

    { tool("networkTool", "/com/sun/electric/database/network"); }
    private final Setting cacheIgnoreResistors = makeBooleanSetting("IgnoreResistors", "Netlists tab", "Networks ignore Resistors", false);

    { tool("compactionTool", "/com/sun/electric/tool/compaction"); }

    { tool("drcTool", "/com/sun/electric/tool/drc"); }

    { tool("ercTool", "/com/sun/electric/tool/erc"); }

    { tool("extractTool", "/com/sun/electric/tool/extract"); }

    { tool("ioTool", "/com/sun/electric/tool/io"); }
	private final Setting cacheUseCopyrightMessage = makeBooleanSetting("UseCopyrightMessage", "Netlists tab", "Use copyright message", false);
	private final Setting cacheCopyrightMessage = makeStringSetting("CopyrightMessage", "Netlists tab", "Copyright message", "");
	private final Setting cacheCIFMimicsDisplay = makeBooleanSetting("CIFMimicsDisplay", "CIF tab", "CIF output mimics display", false);
 	private final Setting cacheCIFMergesBoxes = makeBooleanSetting("CIFMergesBoxes", "CIF tab", "CIF output merges boxes", false);
	private final Setting cacheCIFInstantiatesTopLevel = makeBooleanSetting("CIFInstantiatesTopLevel", "CIF tab", "CIF output instantiates top level", true);
	private final Setting cacheCIFOutScaleFactor = makeIntSetting("CIFOutScaleFactor", "CIF tab", "CIF output scale factor", 1);
	private final Setting cacheGDSMergesBoxes = makeBooleanSetting("GDSMergesBoxes", "GDS tab", "GDS output merges boxes", false);
	private final Setting cacheGDSWritesExportPins = makeBooleanSetting("GDSWritesExportPins", "GDS tab", "GDS output writes export pins", false);
	private final Setting cacheGDSOutputUpperCase = makeBooleanSetting("GDSOutputUpperCase", "GDS tab", "GDS output all upper-case", false);
	private final Setting cacheGDSDefaultTextLayer = makeIntSetting("GDSDefaultTextLayer", "GDS tab", "GDS output default text layer", 230);
	private final Setting cacheGDSOutputConvertsBracketsInExports = makeBooleanSetting("GDSOutputConvertsBracketsInExports", "GDS tab", "GDS output converts brackets in exports", true);
	private final Setting cacheGDSCellNameLenMax = makeIntSetting("GDSCellNameLenMax", "GDS tab", "GDS name length limit", 32);
	private final Setting cacheGDSInputScale = makeDoubleSetting("GDSInputScale", "GDS tab", "GDS input scale", 1.0);
	private final Setting cacheDXFScale = makeIntSetting("DXFScale", "DXF tab", "DXF scale factor", 2);

    { tool("logeffortTool", "/com/sun/electric/tool/logicaleffort"); }
    private static final double DEFAULT_GLOBALFANOUT = 4.7;
    private static final double DEFAULT_EPSILON      = 0.001;
    private static final int    DEFAULT_MAXITER      = 30;
    private static final double DEFAULT_KEEPERRATIO  = 0.1;
	private final Setting cacheUseLocalSettings = makeBooleanSetting("UseLocalSettings", "Logical Effort Tab", "Use Local Settings from Cell", true);
    private final Setting cacheGlobalFanout = makeDoubleSetting("GlobalFanout", "Logical Effort Tab", "Global Fanout", DEFAULT_GLOBALFANOUT);
    private final Setting cacheConvergenceEpsilon = makeDoubleSetting("ConvergenceEpsilon", "Logical Effort Tab", "Convergence Epsilon", DEFAULT_EPSILON);
    private final Setting cacheMaxIterations = makeIntSetting("MaxIterations", "Logical Effort Tab", "Maximum Iterations", DEFAULT_MAXITER);
    private final Setting cacheKeeperRatio = makeDoubleSetting("KeeperRatio", "Logical Effort Tab", "Keeper Ratio", DEFAULT_KEEPERRATIO);

    { tool("parasiticTool", "/com/sun/electric/tool/extract"); }

    { tool("projectTool", "/com/sun/electric/tool/project"); }

    { tool("routingTool", "/com/sun/electric/tool/routing"); }

    { tool("scTool", "/com/sun/electric/tool/sc"); }

    { tool("simulationTool", "/com/sun/electric/tool/simulation"); }
	private final Setting cacheVerilogUseAssign = makeBooleanSetting("VerilogUseAssign", "Verilog tab", "Verilog uses Assign construct", false);
	private final Setting cacheVerilogUseTrireg = makeBooleanSetting("VerilogUseTrireg", "Verilog tab", "Verilog presumes wire is Trireg", false);

    { tool("coverageTool", "/com/sun/electric/tool/extract"); }

    { tool("GateLayoutGenerator", "/com/sun/electric/tool/generator/layout"); }
    private final Setting cachefoundry = makeStringSetting ("foundry",    "Gate Layout Generator Tab", "Foundry", "MOCMOS");
    private final Setting cacheenableNCC = makeStringSetting ("enableNCC",  "Gate Layout Generator Tab", "Enable NCC checking of layout", "purpleFour");
    private final Setting cachequantError = makeIntSetting    ("quantError", "Gate Layout Generator Tab", "Allowable quantization error", 0);
    private final Setting cachemaxmos = makeIntSetting    ("maxmos",     "Gate Layout Generator Tab", "Maximum width of MOS transistors", 1000);
    private final Setting cachevddy = makeIntSetting    ("vddy",       "Gate Layout Generator Tab", "Y coordinate of VDD bus", 21);
    private final Setting cachegndy = makeIntSetting    ("gndy",       "Gate Layout Generator Tab", "Y coordinate of GND bus", -21);
    private final Setting cachenheight = makeIntSetting    ("nheight",    "Gate Layout Generator Tab", "Height of Nwell", 84);
    private final Setting cachepheight = makeIntSetting    ("pheight",    "Gate Layout Generator Tab", "Height of Pwell", 84);
    private final Setting cachesimpleName = makeBooleanSetting("simpleName", "Gate Layout Generator Tab", "Name is gate type plus size", true);

    { tool("Fill GeneratorTool", "/com/sun/electric/tool/generator/layout/fill"); }

    { tool("CVSTool", "/com/sun/electric/tool/cvspm"); }

    { tool("STATool", "/com/sun/electric/plugins/sctiming"); }
    private final Setting cacheGlobalSDCCommands = makeStringSetting("GlobalSDCCommands", "Static Timing Analysis Tab", "Global SDC Constraints", "");
    private final Setting cacheNumWorstPaths = makeIntSetting("NumWorstPaths", "Static Timing Analysis Tab", "Num Worst Paths", 10);

    private ToolSettings() {
        curXmlGroup = null;
        curPrefGroup = null;
        rootSettingGroup.lock();
    }

    private void tool(String xmlPath, String prefPath) {
        curXmlGroup = rootSettingGroup.node(xmlPath);
        curPrefGroup = prefPath;
    }

	/**
	 * Factory methods to create a boolean project setting objects.
     * The created object is stored into a field of this Tool whose name is "cache"+name.
	 * @param name the name of this Pref.
	 * @param location the user-command that can affect this meaning option.
	 * @param description the description of this meaning option.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
    private Setting makeBooleanSetting(String name, String location, String description, boolean factory) {
        return curXmlGroup.makeBooleanSetting(name, curPrefGroup, name, location, description, factory);
    }

	/**
	 * Factory methods to create an integer project setting objects.
     * The created object is stored into a field of this Tool whose name is "cache"+name.
	 * @param name the name of this Pref.
	 * @param location the user-command that can affect this meaning option.
	 * @param description the description of this meaning option.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
    private Setting makeIntSetting(String name, String location, String description, int factory) {
        return curXmlGroup.makeIntSetting(name, curPrefGroup, name, location, description, factory);
    }

	/**
	 * Factory methods to create a long project setting objects.
     * The created object is stored into a field of this Tool whose name is "cache"+name.
	 * @param name the name of this Pref.
	 * @param location the user-command that can affect this meaning option.
	 * @param description the description of this meaning option.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
    private Setting makeLongSetting(String name, String location, String description, long factory) {
        return curXmlGroup.makeLongSetting(name, curPrefGroup, name, location, description, factory);
    }

	/**
	 * Factory methods to create a double project setting objects.
     * The created object is stored into a field of this Tool whose name is "cache"+name.
	 * @param name the name of this Pref.
	 * @param location the user-command that can affect this meaning option.
	 * @param description the description of this meaning option.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
    private Setting makeDoubleSetting(String name, String location, String description, double factory) {
        return curXmlGroup.makeDoubleSetting(name, curPrefGroup, name, location, description, factory);
    }

	/**
	 * Factory methods to create a string project setting objects.
     * The created object is stored into a field of this Tool whose name is "cache"+name.
	 * @param name the name of this Pref.
	 * @param location the user-command that can affect this meaning option.
	 * @param description the description of this meaning option.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
    private Setting makeStringSetting(String name, String location, String description, String factory) {
        return curXmlGroup.makeStringSetting(name, curPrefGroup, name, location, description, factory);
    }
}