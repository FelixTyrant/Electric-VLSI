/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LETool.java
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
 *
 * LETool.java
 *
 * Created on November 17, 2003, 10:16 AM
 */

package com.sun.electric.tool.logicaleffort;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.EvalJavaBsh;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.EditWindow;

import java.io.OutputStream;
import java.lang.InterruptedException;
import java.util.Iterator;
import java.util.Stack;

/**
 * This is the Logical Effort Tool.  It doesn't actually do
 * any work itself, but acts as a public API for all of the
 * logical effort tool functionality.
 *
 * @author  gainsley
 */
public class LETool extends Tool {
    
    /** The Logical Effort tool */              public static LETool tool = new LETool();

    private static final boolean DEBUG = false;

    /** Creates a new instance of LETool */
    private LETool() {
        super("logeffort");
    }

    /** get LETool */
    public static LETool getLETool() {
        return tool;
    }
    
    /** Initialize tool - add calls to Bean Shell Evaluator */
    public void init() {
		EvalJavaBsh.evalJavaBsh.setVariable("LE", tool);
   }


    // =========================== Java Parameter Evaluation ======================

    /**
     * Grabs a logical effort calculated size from the instance
     * @return
     */
    public Object getdrive() throws EvalJavaBsh.IgnorableException {

        // info should be the node on which there is the variable with the getDrive() call
        Object info = EvalJavaBsh.evalJavaBsh.getCurrentInfo();
        if (!(info instanceof Nodable))
            return "Not enough hierarchy";
        VarContext context = EvalJavaBsh.evalJavaBsh.getCurrentContext();
        if (context == null) return "null VarContext";
        Nodable ni = (Nodable)info;

        // Try to find drive strength
        Variable var = getLEDRIVE(ni, context.push(ni));
        if (var == null) {
            // none found, try to find drive strength using old format from C-Electric
            var = getLEDRIVE_old(ni, context);
        }
        //if (var == null) return "No variable "+ledrive;
        if (var == null) throw new EvalJavaBsh.IgnorableException("getdrive() var not found");
        Object val = var.getObject();
        if (val == null) throw new EvalJavaBsh.IgnorableException("getdrive() value null");
        return val;
    }

    /**
     * Grab a paramter 'parName' from a nodeInst 'nodeName' in a sub cell.
     * @param nodeName name of the nodeInst
     * @param parName name of parameter to evaluate
     * @return
     */
    public Object subdrive(String nodeName, String parName) {

        // info should be the node on which there is the variable with the subDrive() call
        Object info = EvalJavaBsh.evalJavaBsh.getCurrentInfo();
        if (!(info instanceof Nodable)) return "subdrive(): Not enough hierarchy information";
        Nodable no = (Nodable)info;                                 // this inst has LE.subdrive(...) on it
        if (no == null) return "subdrive(): Not enough hierarchy information";

        if (no instanceof NodeInst) {
            // networks have not been evaluated, calling no.getProto()
            // is going to give us icon cell, not equivalent schematic cell
            // We need to re-evaluate networks to get equivalent schematic cell
            NodeInst ni = (NodeInst)no;
            Cell parent = no.getParent();                               // Cell in which inst which has LE.subdrive is
            if (parent == null) return "subdrive(): null parent";
			int arrayIndex = 0;                                         // just use first index
            no = Netlist.getNodableFor(ni, arrayIndex);
            if (no == null) return "subdrive(): can't get equivalent schematic";
        }

        VarContext context = EvalJavaBsh.evalJavaBsh.getCurrentContext();  // get current context
        if (context == null) return "subdrive(): null context";

        NodeProto np = no.getProto();                               // get contents of instance
        if (np == null) return "subdrive(): null nodeProto";
        if (!(np instanceof Cell)) return "subdrive(): NodeProto not a Cell";
        Cell cell = (Cell)np;

        NodeInst ni = cell.findNode(nodeName);                      // find nodeinst that has variable on it
        if (ni == null) return "subdrive(): no nodeInst of name "+nodeName;

        Variable var = ni.getVar(parName);                          // find variable on nodeinst
        if (var == null) var = ni.getVar("ATTR_"+parName);          // maybe it's an attribute
        //if (var == null) return "subdrive(): no variable of name "+parName.replaceFirst("ATTR_", "");
        if (var == null) return "?";
        return context.push(no).evalVar(var, ni);                       // evaluate variable and return it
    }

    /**
     * Attempt to get old style LEDRIVE off of <CODE>no</CODE> based
     * on the VarContext <CODE>context</CODE>.
     * Attemps to compensate for the situation when the user
     * had added extra hierarchy to the top of the hierarchy.
     * It cannot compensate for the user has less hierarchy than
     * is required to create the correct Variable name.
     * @param no nodable on which LEDRIVE_ var exists
     * @param context context of <CODE>no</CODE>
     * @return a variable if found, null otherwise
     */
    private Variable getLEDRIVE_old(Nodable no, VarContext context) {
        String drive = makeDriveStrOLDRecurse(context);
        Variable var = null;
        while (!drive.equals("")) {
            if (DEBUG) System.out.println("  Looking for: LEDRIVE_"+drive+";0;S");
            var = no.getVar("LEDRIVE_"+drive+";0;S");
            if (var != null) return var;            // look for var
            int i = drive.indexOf(';');
            if (i == -1) break;
            drive = drive.substring(i+1);             // remove top level of hierarchy
        }
        // look for it at current level
        if (DEBUG) System.out.println("  Looking for: LEDRIVE_0;S");
        var = no.getVar("LEDRIVE_0;S");
        if (var != null) return var;            // look for var
        return null;
    }

    /**
     * Attempt to get LEDRIVE off of <CODE>no</CODE> based
     * on the VarContext <CODE>context</CODE>.
     * Attemps to compensate for the situation when the user
     * had added extra hierarchy to the top of the hierarchy.
     * It cannot compensate for the user has less hierarchy than
     * is required to create the correct Variable name.
     * @param no nodable on which LEDRIVE_ var exists
     * @param context context of <CODE>no</CODE>
     * @return a variable if found, null otherwise
     */
    private Variable getLEDRIVE(Nodable no, VarContext context) {
        String drive = context.getInstPath(".");
        Variable var = null;
        while (!drive.equals("")) {
            if (DEBUG) System.out.println("  Looking for: LEDRIVE_"+drive);
            var = no.getVar("LEDRIVE_"+drive);
            if (var != null) return var;            // look for var
            int i = drive.indexOf('.');
            if (i == -1) return null;
            drive = drive.substring(i+1);             // remove top level of hierarchy
        }
        return null;
    }

    /**
     * Makes a string denoting hierarchy path.
     * @param context var context of node
     * @return  a string denoting hierarchical path of node
     */
    private static String makeDriveStr(VarContext context) {
        return "LEDRIVE_" + context.getInstPath(".");
    }

    /**
     * Makes a string denoting hierarchy path.
     * This is the old version compatible with Java Electric.
     * @param context var context of node
     * @return  a string denoting hierarchical path of node
     */
    private static String makeDriveStrOLD(VarContext context) {
        String s = "LEDRIVE_"+makeDriveStrOLDRecurse(context)+";0;S";
        //System.out.println("name is "+s);
        return s;
    }

    private static String makeDriveStrOLDRecurse(VarContext context) {
        if (context == VarContext.globalContext) return "";

        String prefix = context.pop() == VarContext.globalContext ? "" : makeDriveStrOLDRecurse(context.pop());
        Nodable no = context.getNodable();
        if (no == null) {
            System.out.println("VarContext.getInstPath: context with null NodeInst?");
        }
        String me;
        // two cases if arrayed node: one if just a NodeInst, another if Node Proxy
        if (no instanceof NodeInst) {
            // no array info, assume zeroth index
            me = no.getName() + ",0";
        } else {
            // TODO: not sure what to do here
            me = no.getName() + ",0";
        }

        if (prefix.equals("")) return me;
        return prefix + ";" + me;
    }


    // ============================== Menu Commands ===================================


    /**
     * Optimizes a Cell containing logical effort gates for equal gate delays.
     * @param cell the cell to be sized
     * @param context varcontext of the cell
     * @param wnd the edit window holding the cell
     */
    public void optimizeEqualGateDelays(Cell cell, VarContext context, EditWindow wnd) {
        AnalyzeCell acjob = new AnalyzeCell(LESizer.Alg.EQUALGATEDELAYS, cell, context, wnd);
    }
    
    /**
     * Performs a cell analysis. The algorithm argument tells the LESizer how to size
     * the netlist generated by LENetlist.
     */
    protected class AnalyzeCell extends Job
    {
        /** progress */                         private String progress;
        /** cell to analyze */                  private Cell cell;
        /** var context */                      private VarContext context;
        /** EditWindow */                       private EditWindow wnd;
        /** algorithm type */                   private LESizer.Alg algorithm;
        /** netlist */                          private LENetlister netlister;

        protected AnalyzeCell(LESizer.Alg algorithm, Cell cell, VarContext context, EditWindow wnd) {
            super("Analyze Cell "+cell.describe(), tool, Job.Type.EXAMINE, null, cell, Job.Priority.USER);
            progress = null;
            this.algorithm = algorithm;
            this.cell = cell;
            this.context = context;
            this.wnd = wnd;
			this.startJob(true, false);
        }
        
        public boolean doIt() {
            setProgress("building equations");
            System.out.print("Building equations...");

            // sleep for testing purposes only, remove later
//            try {
//                boolean donesleeping = false;
//                while(!donesleeping) {
//                    Thread.sleep(1);
//                    donesleeping = true;
//                }
//            } catch (InterruptedException e) {}

            // get sizer and netlister
            netlister = new LENetlister(algorithm, this);
            netlister.netlist(cell, context);

            // calculate statistics
            long equationsDone = System.currentTimeMillis();
            String elapsed = TextUtils.getElapsedTime(equationsDone-startTime);
            System.out.println("done ("+elapsed+")");

            // if user aborted, return, and do not run sizer
            if (checkAbort(null)) return false;

            System.out.println("Starting iterations: ");
            setProgress("iterating");
            boolean success = netlister.size();

            // if user aborted, return, and do not update sizes
            if (checkAbort(null)) return false;

            if (success) {
                UpdateSizes job = new UpdateSizes(netlister, cell, wnd);
                netlister.printStatistics();
            } else {
                System.out.println("Sizing failed, sizes unchanged");
            }
			return true;
       }

        /**
         * Check if we are scheduled to abort. If so, print msg if non null
         * and return true.
         * @param msg message to print if we are aborted
         * @return true on abort, false otherwise
         */
        protected boolean checkAbort(String msg) {
            if (getScheduledToAbort()) {
                if (msg != null) System.out.println("LETool aborted: "+msg);
                else System.out.println("LETool aborted: no changes made");
                setAborted();                   // Job has been aborted
                return true;
            }
            return false;
        }

        // add more info to default getInfo
        public String getInfo() {

            StringBuffer buf = new StringBuffer();
            buf.append(super.getInfo());
            if (getScheduledToAbort())
                buf.append("  Job aborted, no changes made\n");
            else {
                buf.append("  Gates sized: "+netlister.getNumGates()+"\n");
                buf.append("  Total Drive Strength: "+netlister.getTotalSize()+"\n");
            }
            return buf.toString();
        }

        protected LENetlister getNetlister() { return netlister; }
    }

    private static class UpdateSizes extends Job {

        private LENetlister netlister;
        private EditWindow wnd;

        private UpdateSizes(LENetlister netlister, Cell cell, EditWindow wnd) {
            super("Update LE Sizes", tool, Job.Type.CHANGE, null, cell, Job.Priority.USER);
            this.netlister = netlister;
            this.wnd = wnd;
            startJob();
        }

        public boolean doIt() {
            netlister.updateSizes();
            wnd.repaintContents(null);
            return true;
        }
    }

    /**
     * Prints results of a sizing job for a Nodable.
     * @param no the Nodable to print info for.
     */
    public static void printResults(Nodable no) {
        // iterate through LE jobs from most recent until we
        // find info for 'no'
        Iterator it = Job.getAllJobs();
        Stack stack = new Stack();
        while (it.hasNext()) {
            Job job = (Job)it.next();
            if (job instanceof AnalyzeCell) stack.push(job);
        }
        while (!stack.isEmpty()) {
            AnalyzeCell job = (AnalyzeCell)stack.pop();
            if (job.getAborted()) continue;             // ignore aborted jobs
            LENetlister netlister = job.getNetlister();
            if (netlister.printResults(no)) return;
        }
        // no info found
        System.out.println("No existing completed sizing jobs contain info about "+no.getName());
    }

    /**
     * Clears stored "LEDRIVE_" sizes on a Nodable.
     */
    public void clearStoredSizes(Nodable no) {
        // delete all vars that start with "LEDRIVE_"
        for (Iterator it = no.getVariables(); it.hasNext(); ) {
            Variable var = (Variable)it.next();
            String name = var.getKey().getName();
            if (name.startsWith("LEDRIVE_")) {
                no.delVar(var.getKey());
            }
        }
    }

    /**
     * Clears stored "LEDRIVE_" sizes on all nodes in a Cell.
     */
    public void clearStoredSizes(Cell cell) {
        for (Iterator it = cell.getNodes(); it.hasNext(); ) {
            clearStoredSizes((Nodable)it.next());
        }
    }

    /**
     * Clears stored "LEDRIVE_" sizes for all cells in a Library.
     */
    public void clearStoredSizes(Library lib) {
        for (Iterator it = lib.getCells(); it.hasNext(); ) {
            clearStoredSizes((Cell)it.next());
        }
    }


	/****************************** OPTIONS ******************************/

	// preferences; default values are for the TSMC 180nm technology
    private static double DEFAULT_GLOBALFANOUT = 4.7;
    private static double DEFAULT_EPSILON      = 0.001;
    private static int    DEFAULT_MAXITER      = 30;
    private static double DEFAULT_GATECAP      = 0.4;
    private static double DEFAULT_WIRERATIO    = 0.16;
    private static double DEFAULT_DIFFALPHA    = 0.7;
    private static double DEFAULT_KEEPERRATIO  = 0.1;

	private static Pref cacheUseLocalSettings = Pref.makeBooleanPref("UseLocalSettings", LETool.tool.prefs, true);
	/**
	 * Method to tell whether to use local settings for Logical Effort.
	 * The default is true.
	 * @return true to use local settings for Logical Effort
	 */
	public static boolean isUseLocalSettings() { return cacheUseLocalSettings.getBoolean(); }
	/**
	 * Method to set whether to use local settings for Logical Effort
	 * @param on whether to use local settings for Logical Effort
	 */
	public static void setUseLocalSettings(boolean on) { cacheUseLocalSettings.setBoolean(on); }

	private static Pref cacheHighlightComponents = Pref.makeBooleanPref("HighlightComponents", LETool.tool.prefs, false);
	/**
	 * Method to tell whether to highlight components in Logical Effort.
	 * The default is false.
	 * @return true to highlight components in Logical Effort
	 */
	public static boolean isHighlightComponents() { return cacheHighlightComponents.getBoolean(); }
	/**
	 * Method to set whether to highlight components in Logical Effort
	 * @param on whether to highlight components in Logical Effort
	 */
	public static void setHighlightComponents(boolean on) { cacheHighlightComponents.setBoolean(on); }

	private static Pref cacheShowIntermediateCapacitances = Pref.makeBooleanPref("ShowIntermediateCapacitances", LETool.tool.prefs, false);
	/**
	 * Method to tell whether to highlight intermediate capacitances in Logical Effort.
	 * The default is false.
	 * @return true to highlight intermediate capacitances in Logical Effort
	 */
	public static boolean isShowIntermediateCapacitances() { return cacheShowIntermediateCapacitances.getBoolean(); }
	/**
	 * Method to set whether to highlight intermediate capacitances in Logical Effort
	 * @param on whether to highlight intermediate capacitances in Logical Effort
	 */
	public static void setShowIntermediateCapacitances(boolean on) { cacheShowIntermediateCapacitances.setBoolean(on); }

	private static Pref cacheGlobalFanout = Pref.makeDoublePref("GlobalFanout", LETool.tool.prefs, DEFAULT_GLOBALFANOUT);
	/**
	 * Method to get the Global Fanout for Logical Effort.
	 * The default is DEFAULT_GLOBALFANOUT.
	 * @return the Global Fanout for Logical Effort.
	 */
	public static double getGlobalFanout() { return cacheGlobalFanout.getDouble(); }
	/**
	 * Method to set the Global Fanout for Logical Effort.
	 * @param fo the Global Fanout for Logical Effort.
	 */
	public static void setGlobalFanout(double fo) { cacheGlobalFanout.setDouble(fo); }

	private static Pref cacheConvergenceEpsilon = Pref.makeDoublePref("Epsilon", LETool.tool.prefs, DEFAULT_EPSILON);
	/**
	 * Method to get the Convergence Epsilon value for Logical Effort.
	 * The default is DEFAULT_EPSILON.
	 * @return the Convergence Epsilon value for Logical Effort.
	 */
	public static double getConvergenceEpsilon() { return cacheConvergenceEpsilon.getDouble(); }
	/**
	 * Method to set the Convergence Epsilon value for Logical Effort.
	 * @param ep the Convergence Epsilon value for Logical Effort.
	 */
	public static void setConvergenceEpsilon(double ep) { cacheConvergenceEpsilon.setDouble(ep); }

	private static Pref cacheMaxIterations = Pref.makeIntPref("MaxIterations", LETool.tool.prefs, DEFAULT_MAXITER);
	/**
	 * Method to get the maximum number of iterations for Logical Effort.
	 * The default is DEFAULT_MAXITER.
	 * @return the maximum number of iterations for Logical Effort.
	 */
	public static int getMaxIterations() { return cacheMaxIterations.getInt(); }
	/**
	 * Method to set the maximum number of iterations for Logical Effort.
	 * @param it the maximum number of iterations for Logical Effort.
	 */
	public static void setMaxIterations(int it) { cacheMaxIterations.setInt(it); }

	private static Pref cacheGateCapacitance = Pref.makeDoublePref("GateCapfFPerLambda", LETool.tool.prefs, DEFAULT_GATECAP);
	/**
	 * Method to get the Gate Capacitance for Logical Effort.
	 * The default is DEFAULT_GATECAP.
	 * @return the Gate Capacitance for Logical Effort.
	 */
	public static double getGateCapacitance() { return cacheGateCapacitance.getDouble(); }
	/**
	 * Method to set the Gate Capacitance for Logical Effort.
	 * @param gc the Gate Capacitance for Logical Effort.
	 */
	public static void setGateCapacitance(double gc) { cacheGateCapacitance.setDouble(gc); }

	private static Pref cacheWireRatio = Pref.makeDoublePref("WireRatio", LETool.tool.prefs, DEFAULT_WIRERATIO);
	/**
	 * Method to get the wire capacitance ratio for Logical Effort.
	 * The default is DEFAULT_WIRERATIO.
	 * @return the wire capacitance ratio for Logical Effort.
	 */
	public static double getWireRatio() { return cacheWireRatio.getDouble(); }
	/**
	 * Method to set the wire capacitance ratio for Logical Effort.
	 * @param wr the wire capacitance ratio for Logical Effort.
	 */
	public static void setWireRatio(double wr) { cacheWireRatio.setDouble(wr); }

	private static Pref cacheDiffAlpha = Pref.makeDoublePref("DiffusionAlpha", LETool.tool.prefs, DEFAULT_DIFFALPHA);
	/**
	 * Method to get the diffusion to gate capacitance ratio for Logical Effort.
	 * The default is DEFAULT_DIFFALPHA.
	 * @return the diffusion to gate capacitance ratio for Logical Effort.
	 */
	public static double getDiffAlpha() { return cacheDiffAlpha.getDouble(); }
	/**
	 * Method to set the diffusion to gate capacitance ratio for Logical Effort.
	 * @param da the diffusion to gate capacitance ratio for Logical Effort.
	 */
	public static void setDiffAlpha(double da) { cacheDiffAlpha.setDouble(da); }

	private static Pref cacheKeeperRatio = Pref.makeDoublePref("KeeperRatio", LETool.tool.prefs, DEFAULT_KEEPERRATIO);
	/**
	 * Method to get the keeper size ratio for Logical Effort.
	 * The default is DEFAULT_KEEPERRATIO.
	 * @return the keeper size ratio for Logical Effort.
	 */
	public static double getKeeperRatio() { return cacheKeeperRatio.getDouble(); }
	/**
	 * Method to set the keeper size ratio for Logical Effort.
	 * @param kr the keeper size ratio for Logical Effort.
	 */
	public static void setKeeperRatio(double kr) { cacheKeeperRatio.setDouble(kr); }

}
