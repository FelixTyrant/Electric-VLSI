/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Gates.java
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
package com.sun.electric.tool.generator.layout;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.MoCMOS;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.generator.layout.gates.MoCMOSGenerator;
import com.sun.electric.tool.io.output.CIF;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.User;

import java.lang.reflect.Method;

/*
 * Regression test for gate generators
 */
public class GateRegression extends Job {
    private Technology technology;

    // specify which gates shouldn't be surrounded by DRC rings
	private static final DrcRings.Filter FILTER = new DrcRings.Filter() {
		public boolean skip(NodeInst ni) {
			// well tie cells don't pass DRC with DrcRings
	        return ni.getProto().getName().indexOf("mosWellTie_") != -1;
		}
	};

	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}

    private static void allSizes(StdCellParams stdCell, Technology technology) {
        double minSz = 0.1;
        double maxSz = 200;//500;
        for (double d=minSz; d<maxSz; d*=10) {
            for (double x=d; x<Math.min(d*10, maxSz); x*=1.01) {
                aPass(x, stdCell, technology);
            }
        }
    }

    public static void aPass(double x, StdCellParams stdCell, Technology technology) {
        if (technology == MoCMOS.tech) {
            Tech.setTechnology(Tech.MOCMOS);
            MoCMOSGenerator.generateAllGates(x, stdCell);
        }
        Technology tsmc90 = Technology.getTSMC90Technology();
        if (tsmc90 != null && technology == tsmc90) {
            Tech.setTechnology(Tech.TSMC90);

            // invoke the TSMC90 generator by reflection because it may not exist
    		try
			{
				Class tsmc90GeneratorClass = Class.forName("com.sun.electric.plugins.tsmc90.gates90nm.TSMC90Generator");
				Class [] parameterTypes = new Class[] {Double.class, StdCellParams.class};
				Method generateMethod = tsmc90GeneratorClass.getDeclaredMethod("generateAllGates", parameterTypes);
				generateMethod.invoke(null, new Object[] {new Double(x), stdCell});
	 		} catch (Exception e)
	        {
	 			System.out.println("ERROR invoking the TSMC90 gate generator");
	        }
//            TSMC90Generator.generateAllGates(x, stdCell);
        }
    }

    public boolean doIt() {
        runRegression(technology);
        return true;
    }
    /** Programatic interface to gate regressions.
     * @param technology
     * @return the number of errors detected */
    public static int runRegression(Technology technology) {
		System.out.println("begin Gate Regression");

		Library scratchLib = 
		  LayoutLib.openLibForWrite("scratch", "scratch");

        StdCellParams stdCell;
        Technology tsmc90 = Technology.getTSMC90Technology();
        if (tsmc90 != null && technology == tsmc90) {
            stdCell = new StdCellParams(scratchLib, Tech.TSMC90);
            stdCell.enableNCC("purpleFour");
            stdCell.setSizeQuantizationError(0.05);
            stdCell.setMaxMosWidth(1000);
        } else {
            stdCell = new StdCellParams(scratchLib, Tech.MOCMOS);
            stdCell.enableNCC("purpleFour");
            stdCell.setSizeQuantizationError(0.05);
            stdCell.setMaxMosWidth(1000);
            stdCell.setVddY(21);
            stdCell.setGndY(-21);
            stdCell.setNmosWellHeight(49);
            stdCell.setPmosWellHeight(49);
        }

		// a normal run
        allSizes(stdCell, technology);
        //aPass(200, stdCell, technology);

        // test the ability to move ground bus
        stdCell.setGndY(stdCell.getGndY() - 7);
        stdCell.setNmosWellHeight(stdCell.getNmosWellHeight()+7);
        //allSizes(stdCell, technology);
        aPass(10, stdCell, technology);
        aPass(200, stdCell, technology);
        stdCell.setGndY(stdCell.getGndY() + 7);
        stdCell.setNmosWellHeight(stdCell.getNmosWellHeight()-7);

        // test different PMOS to NMOS heights
        stdCell.setNmosWellHeight(50);
        stdCell.setPmosWellHeight(100);
        //allSizes(stdCell, technology);
        aPass(10, stdCell, technology);
        aPass(200, stdCell, technology);

        stdCell.setNmosWellHeight(100);
        stdCell.setPmosWellHeight(50);
        //allSizes(stdCell, technology);
        aPass(10, stdCell, technology);
        aPass(200, stdCell, technology);
        stdCell.setNmosWellHeight(70);
        stdCell.setPmosWellHeight(70);

        Cell gallery = Gallery.makeGallery(scratchLib);
        DrcRings.addDrcRings(gallery, FILTER, stdCell);

        int numCifErrs = CIF.writeCIFFile(gallery, VarContext.globalContext, 
        		                          "scratch.cif", 0.5);
        LayoutLib.writeLibrary(scratchLib);

        System.out.println("done.");

        return numCifErrs;
	}
	public GateRegression(Technology tech) {
		super("Run Gate regression", User.tool, Job.Type.CHANGE, 
			  null, null, Job.Priority.ANALYSIS);
        this.technology = tech;
		startJob();
	}
}
