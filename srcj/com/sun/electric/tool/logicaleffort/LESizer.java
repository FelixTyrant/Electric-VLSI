/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LESizer.java
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
 * LESizer.java
 *
 * Created on November 11, 2003, 4:42 PM
 */

package com.sun.electric.tool.logicaleffort;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.logicaleffort.*;

import java.util.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.OutputStream;

/**
 * LESizer sizes an LENetlist. The LENetlist is generated by LENetlister from
 * the Electric database, or perhaps read in from a Spice file(?)
 *
 * NOTE: the only 'Electric' objects used are in LENetlister,
 * any objects referenced in this file are from the logicaleffort
 * package, although their names may imply otherwise.  Their names
 * are as such because their names match PNP's naming scheme.
 *
 * @author  gainsley
 */
public class LESizer {
    
    /** which algorithm to use */                   private Alg optimizationAlg;
    /** Where to direct output */                   private PrintStream out;
    /** What job we are part of */                  private Job job;
    /** Netlist */                                  private LENetlister netlist;

    /** Alg is a typesafe enum class that describes the algorithm to be used */
    protected static class Alg {
        private final String name;
        private Alg(String name) { this.name = name; }
        public String toString() { return name; }
        
        /** Sizes all gates for user specified equal gate delay */
        protected static final Alg EQUALGATEDELAYS = new Alg("Equal Gate Delays");
        /** Sizes for optimal path delay */
        protected static final Alg PATHDELAY = new Alg("Path Delay");
    }
        
    /** Creates a new instance of LESizer */
    protected LESizer(Alg alg, LENetlister netlist, Job job) {
        optimizationAlg = alg;
        this.netlist = netlist;
        this.job = job;

        out = new PrintStream(System.out);
    }


    // ============================ Sizing For Equal Gate Delays ==========================

	/**
	 * Optimize using loop algorithm;
	 * @param maxDeltaX maximum tolerance allowed in X
	 * @param N maximum number of loops
	 * @param verbose print out size information for each optimization loop
	 * @return true if succeeded, false otherwise
     *
	 * Optimization will stop when the difference in sizes (X) is 
	 * less than maxDeltaX, or when N iterations have occurred.
	 */
	protected boolean optimizeLoops(float maxDeltaX, int N, boolean verbose,
        float alpha, float keeperRatio)
	{
		// iterate through all the instances, updating sizes

		float currentLoopDeltaX = maxDeltaX + 1;	// force at least one iteration
        float lastLoopDeltaX = currentLoopDeltaX;
        int divergingIters = 0;                     // count number if iterations sizing is diverging
        long startTime;
        
		int loopcount = 0;
		
		while ((currentLoopDeltaX > maxDeltaX) && (loopcount < N)) {

            // check for aborted state of job
            if (((LETool.AnalyzeCell)job).checkAbort(null)) return false;

			currentLoopDeltaX = 0;
            startTime = System.currentTimeMillis();
            System.out.print("  Iteration "+loopcount);
            if (verbose) System.out.println(":");
            
			// iterate through each instance
			Iterator instancesIter = netlist.getAllInstances().values().iterator();

			while (instancesIter.hasNext()) {
				Instance instance = (Instance)instancesIter.next();
				String instanceName = instance.getName();
				
				// make sure it is a sizeable gate
				if (instance.isLeGate()) {
					
					// get output pin (do not iterate over all output pins, does not make sense)
					ArrayList outputPins = instance.getOutputPins();
                    if (outputPins.size() != 1) {
                        // error
                        continue;
                    }
                    Pin outputPin = (Pin)outputPins.get(0);                    
                    Net net = outputPin.getNet();
                    
                    // now find all pins connected to this net
                    ArrayList netpins = net.getAllPins();

                    // find all drivers in same group, of same type (LEGATe or LEKEEPER)
                    List drivers = new ArrayList();
                    for (Iterator it = netpins.iterator(); it.hasNext(); ) {
                        Pin pin = (Pin)it.next();
                        // only interested in drivers
                        if (pin.getDir() != Pin.Dir.OUTPUT) continue;
                        Instance inst = pin.getInstance();
                        if (inst.getType() == instance.getType()) {
                            if (inst.getParallelGroup() == instance.getParallelGroup()) {
                                // add the instance. Note this adds the current instance at some point as well
                                drivers.add(inst);
                            }
                        }
                    }

                    // this will be the new size.
                    float newX = 0;

                    // if this is an LEKEEPER, we need to find smallest gate (or group)
                    // that also drives this net, it is assumed that will have to overpower this keeper
                    if (instance.getType() == Instance.Type.LEKEEPER) {
                        ArrayList drivingGroups = new ArrayList();

                        float smallestX = 0;

                        // iterate over all drivers on net
                        for (Iterator it = netpins.iterator(); it.hasNext(); ) {
                            Pin pin = (Pin)it.next();
                            // only interested in drivers
                            if (pin.getDir() != Pin.Dir.OUTPUT) continue;
                            Instance inst = pin.getInstance();
                            if ((inst.getType() == Instance.Type.LEGATE) ||
                                (inst.getType() == Instance.Type.STATICGATE)) {
                                // organize by groups
                                int i = inst.getParallelGroup();
                                if (i <= 0) {
                                    // this gate drives independently, check size
                                    if (smallestX == 0) smallestX = inst.getLeX();
                                    if (inst.getLeX() < smallestX) smallestX = inst.getLeX();
                                }
                                // otherwise, add to group to sum up drive strength later
                                ArrayList groupList = (ArrayList)drivingGroups.get(i);
                                if (groupList == null) {
                                    groupList = new ArrayList();
                                    drivingGroups.add(i, groupList);
                                }
                                groupList.add(inst);
                            }
                        }

                        // find smallest total size of groups
                        for (Iterator it = drivingGroups.iterator(); it.hasNext(); ) {
                            ArrayList groupList = (ArrayList)it.next();
                            if (groupList == null) continue;            // skip empty groups
                            // get size
                            float sizeX = 0;
                            for (Iterator it2 = groupList.iterator(); it2.hasNext(); ) {
                                Instance inst = (Instance)it2.next();
                                sizeX += inst.getLeX();
                            }
                            // check size of group
                            if (smallestX == 0) smallestX = sizeX;
                            if (sizeX < smallestX) smallestX = sizeX;
                        }

                        // if size is 0, no drivers found, issue warning
                        if (smallestX == 0)
                            System.out.println("Error: LEKEEPER \""+instance.getName()+"\" does not fight against any drivers");

                        // For now, split effort equally amongst all drivers
                        newX = smallestX * netlist.getKeeperRatio() / drivers.size();
                    }

                    // If this is an LEGATE, simply sum all capacitances on the Net
                    if (instance.getType() == Instance.Type.LEGATE) {

                        // compute total le*X (totalcap)
                        float totalcap = 0;
                        Iterator netpinsIter = netpins.iterator();
                        while (netpinsIter.hasNext()) {
                            Pin netpin = (Pin)netpinsIter.next();
                            Instance netpinInstance = netpin.getInstance();
                            float load = netpinInstance.getLeX() * netpin.getLE() * (float)netpinInstance.getMfactor();
                            if (netpin.getDir() == Pin.Dir.OUTPUT) load *= alpha;
                            totalcap += load;
                        }

                        // For now, split effort equally amongst all drivers
                        newX = totalcap / instance.getLeSU() / drivers.size();
                        // also take into account mfactor of driver
                        newX = newX / (float)instance.getMfactor();
                    }

                    // determine change in size
                    float currentX = instance.getLeX();
                    if (currentX == 0) currentX = 0.001f;
                    float deltaX = Math.abs( (newX-currentX)/currentX);
                    currentLoopDeltaX = (deltaX > currentLoopDeltaX) ? deltaX : currentLoopDeltaX;
                    if (verbose) {
                        out.println("Optimized "+instanceName+": size:  "+
                            Variable.format(new Float(instance.getLeX()), 3)+
                            "x ==> "+Variable.format(new Float(newX), 3)+"x");
                    }
                    instance.setLeX(newX);

                } // if (leGate)

			} // while (instancesIter)

            // All done, print some statistics about this iteration
            String elapsed = TextUtils.getElapsedTime(System.currentTimeMillis()-startTime);
            System.out.println("  ...done ("+elapsed+"), delta: "+currentLoopDeltaX);            
            if (verbose) System.out.println("-----------------------------------");
			loopcount++;

            // check to see if we're diverging or not converging
            if (currentLoopDeltaX >= lastLoopDeltaX) {
                if (divergingIters > 2) {
                    System.out.println("  Sizing diverging, aborting");
                    return false;
                }
                divergingIters++;
            }
            lastLoopDeltaX = currentLoopDeltaX;

		} // while (currentLoopDeltaX ... )
        return true;
	}



    // ========================== Sizing for Path Optimization =====================

    protected List getEndNets() {

        List endNets = new ArrayList();

        Iterator netIter = netlist.getAllNets().values().iterator();
        while (netIter.hasNext()) {
            Net net = (Net)netIter.next();

        }
        return null;
    }



    // =============================== Statistics ==================================


    // ============================== Design Printing ===============================

    /**
     * Dump the design information for debugging purposes
     */
    protected void printDesign() 
	{
		out.println("Instances in design are:");
		        
		Iterator instancesIter = netlist.getAllInstances().values().iterator();
		while (instancesIter.hasNext()) {
			Instance instance = (Instance)instancesIter.next();
			String instanceName = instance.getName();
            StringBuffer buf = new StringBuffer();
			out.println("\t"+instanceName+" ==> "+Variable.format(new Float(instance.getLeX()), 3)+"x");
			ArrayList pins = instance.getAllPins();
			
			// now print out pinname ==> netname
			Iterator pinsIter = pins.iterator();
			while (pinsIter.hasNext()) {
				Pin pin = (Pin)pinsIter.next();
				out.println("\t\t"+pin.getName()+" ==> "+pin.getNetName());
			}
		}
	}

    /**
     * Generate simple size file (for regression purposes)
     * @param filename output filename
     */
    protected int printDesignSizes(String filename)
	{
		// open output file
		try {
			FileWriter fileWriter = new FileWriter(filename); // throws IOException
			
			// iterate through all instances
			Iterator instancesIter = netlist.getAllInstances().values().iterator();
			while (instancesIter.hasNext()) {
				Instance instance = (Instance)instancesIter.next();
				String instanceName = instance.getName();
				float leX = instance.getLeX();
				fileWriter.write(instanceName+" "+leX+"\n"); // throws IOException
				fileWriter.flush(); // throws IOException
			}
			fileWriter.close(); // throws IOException

		} catch (IOException e) {
			out.println("Writing to file "+filename+": "+e.getMessage());
			return 1;
		}
		return 0;
	}

    /**
     * Generate SKILL backannotation file
     * @param filename output filename
     * @param libname  The Opus library name to be annotated
     * @param cellname  The Opus cell to be annotated
     */
    protected int printDesignSkill(String filename, String libname, String cellname)
	{
		// nothing here
		return 0;
	}

    /**
     * Dummy method to improve test coverage
     */
    protected void testcoverage()
	{
		// nothing here
	}
   
    //---------------------------------------TEST---------------------------------------
    //---------------------------------------TEST---------------------------------------
    
    /** run a contrived test */
    public static void test1()
    {
        System.out.println("Running GASP test circuit");
        System.out.println("=========================");
        
        float su = (float)4.0;
        LENetlister netlist = new LENetlister(Alg.EQUALGATEDELAYS, null);
                
        {
        // inv1
        Pin pin_a = new Pin("A", Pin.Dir.INPUT, (float)1.0, "nand1_out");
        Pin pin_y = new Pin("Y", Pin.Dir.OUTPUT, (float)1.0, "inv1_out");
        ArrayList pins = new ArrayList();
        pins.add(pin_a);
        pins.add(pin_y);
        netlist.addInstance("inv1", Instance.Type.LEGATE, su, (float)1.0, pins, null);
        }

        {
        // inv2
        Pin pin_a = new Pin("A", Pin.Dir.INPUT, (float)1.0, "pu_out");
        Pin pin_y = new Pin("Y", Pin.Dir.OUTPUT, (float)1.0, "inv2_out");
        ArrayList pins = new ArrayList();
        pins.add(pin_a);
        pins.add(pin_y);
        netlist.addInstance("inv2", Instance.Type.LEGATE, su, (float)1.0, pins, null);
        }

        {
        // inv3
        Pin pin_a = new Pin("A", Pin.Dir.INPUT, (float)1.0, "nand1_out");
        Pin pin_y = new Pin("Y", Pin.Dir.OUTPUT, (float)1.0, "inv3_out");
        ArrayList pins = new ArrayList();
        pins.add(pin_a);
        pins.add(pin_y);
        netlist.addInstance("inv3", Instance.Type.LEGATE, su, (float)1.0, pins, null);
        }
        
        {
        // nand1
        Pin pin_a = new Pin("A", Pin.Dir.INPUT, (float)1.333, "inv2_out");
        Pin pin_b = new Pin("B", Pin.Dir.INPUT, (float)1.333, "pd_out");
        Pin pin_y = new Pin("Y", Pin.Dir.OUTPUT, (float)2.0, "nand1_out");
        ArrayList pins = new ArrayList();
        pins.add(pin_a);
        pins.add(pin_b);
        pins.add(pin_y);
        netlist.addInstance("nand1", Instance.Type.LEGATE, su, (float)1.0, pins, null);
        }

        {
        // pu
        Pin pin_g = new Pin("G", Pin.Dir.INPUT, (float)0.667, "nand1_out");
        Pin pin_d = new Pin("D", Pin.Dir.OUTPUT, (float)0.667, "pu_out");
        ArrayList pins = new ArrayList();
        pins.add(pin_g);
        pins.add(pin_d);
        netlist.addInstance("pu", Instance.Type.LEGATE, su, (float)1.0, pins, null);
        }

        {
        // pd
        Pin pin_g = new Pin("G", Pin.Dir.INPUT, (float)0.333, "inv3_out");
        Pin pin_d = new Pin("D", Pin.Dir.OUTPUT, (float)0.333, "pd_out");
        ArrayList pins = new ArrayList();
        pins.add(pin_g);
        pins.add(pin_d);
        netlist.addInstance("pd", Instance.Type.LEGATE, su, (float)1.0, pins, null);
        }

        {
        // cap1
        Pin pin_c = new Pin("C", Pin.Dir.INPUT, (float)1.0, "pd_out");
        ArrayList pins = new ArrayList();
        pins.add(pin_c);
        netlist.addInstance("cap1", Instance.Type.LOAD, su, (float)0.0, pins, null);
        }

        {
        // cap2
        Pin pin_c = new Pin("C", Pin.Dir.INPUT, (float)1.0, "pu_out");
        ArrayList pins = new ArrayList();
        pins.add(pin_c);
        netlist.addInstance("cap2", Instance.Type.LOAD, su, (float)0.0, pins, null);
        }

        {
        // cap3
        Pin pin_c = new Pin("C", Pin.Dir.INPUT, (float)1.0, "inv1_out");
        ArrayList pins = new ArrayList();
        pins.add(pin_c);
        netlist.addInstance("cap3", Instance.Type.LOAD, su, (float)100.0, pins, null);
        }

        netlist.getSizer().printDesign();
        netlist.getSizer().optimizeLoops((float)0.01, 30, true, (float)0.7, (float)0.1);
        System.out.println("After optimization:");
        netlist.getSizer().printDesign();
    }
}
