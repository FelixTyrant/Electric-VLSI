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
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.geom.Point2D;

import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.prototype.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.database.network.*;
import com.sun.electric.technology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;

import com.sun.electric.tool.generator.layout.gates.*;

/*
 * Regression test for gate generators
 */
public class GateRegression extends Job {
	// specify which gates shouldn't be surrounded by DRC rings
	private static final DrcRings.Filter FILTER = new DrcRings.Filter() {
		public boolean skip(NodeInst ni) {
			// well tie cells don't pass DRC with DrcRings
	        return ni.getProto().getProtoName().indexOf("mosWellTie_") != -1;
		}
	};

	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}

	private static boolean osIsWindows() {
		Properties props = System.getProperties();
		String osName = ((String) props.get("os.name")).toLowerCase();
		return osName.indexOf("windows") != -1;
	}
	private static int gateNb;
	
	private static void tracePass(double x) {
		System.out.println("\nbegin pass x="+x);
		gateNb=0;
	}
	
	private static void traceGate() {
		System.out.print(" "+gateNb++);
		System.out.flush();
	}

	private static void aPass(double x, StdCellParams stdCell) {
		tracePass(x);
		MullerC_sy.makePart(x, stdCell); traceGate();
		Nms1.makePart(x, stdCell); traceGate();
		Nms2.makePart(x, stdCell); traceGate();
		Nms2_sy.makePart(x, stdCell); traceGate();
		Nms3_sy3.makePart(x, stdCell); traceGate();
		Pms1.makePart(x, stdCell); traceGate();
		Pms2.makePart(x, stdCell); traceGate();
		Pms2_sy.makePart(x, stdCell); traceGate();
		Inv_passgate.makePart(x, stdCell); traceGate();
		Inv.makePart(x, stdCell); traceGate();
		InvCTLn.makePart(x, stdCell); traceGate();
		InvLT.makePart(x, stdCell); traceGate();
		InvHT.makePart(x, stdCell); traceGate();
		Inv2iKp.makePart(x, stdCell); traceGate();

		Inv2iKn.makePart(x, stdCell); traceGate();
		Inv2i.makePart(x, stdCell); traceGate();
		Nor2.makePart(x, stdCell); traceGate();
		Nor2LT.makePart(x, stdCell); traceGate();
		Nor2kresetV.makePart(x, stdCell); traceGate();
		Nand2.makePart(x, stdCell); traceGate();

		Nand2en.makePart(x, stdCell); traceGate();
		Nand2PH.makePart(x, stdCell); traceGate();
		Nand2HLT.makePart(x, stdCell); traceGate();
		Nand2PHfk.makePart(x, stdCell); traceGate();
		Nand2LT.makePart(x, stdCell); traceGate();
		Nand2_sy.makePart(x, stdCell); traceGate();
		Nand2HLT_sy.makePart(x, stdCell); traceGate();
		Nand2LT_sy.makePart(x, stdCell); traceGate();
		Nand2en_sy.makePart(x, stdCell); traceGate();
		Nand3.makePart(x, stdCell); traceGate();
		Nand3LT.makePart(x, stdCell); traceGate();
		Nand3MLT.makePart(x, stdCell); traceGate();
		Nand3_sy3.makePart(x, stdCell); traceGate();
		Nand3LT_sy3.makePart(x, stdCell); traceGate();
		Nand3en_sy.makePart(x, stdCell); traceGate();
		Nand3LTen_sy.makePart(x, stdCell); traceGate();
		Nand3en.makePart(x, stdCell); traceGate();
		Nand3LTen.makePart(x, stdCell); traceGate();

		//if (x>=1.7) Nand3en_sy3.makePart(x, stdCell);
		//if (x>=2.5) Nand3LTen_sy3.makePart(x, stdCell);

		// Test gates that can double strap MOS gates
		stdCell.setDoubleStrapGate(true);
		Inv.makePart(x, stdCell); traceGate();
		InvLT.makePart(x, stdCell); traceGate();
		InvHT.makePart(x, stdCell); traceGate();
		Nms1.makePart(x, stdCell); traceGate();
		Pms1.makePart(x, stdCell); traceGate();
		stdCell.setDoubleStrapGate(false);
	}

	private static void allSizes(StdCellParams stdCell) {
		double minSz = 0.1;
		double maxSz = 1000;
		for (double d=minSz; d<maxSz; d*=10) {
			for (double x=d; x<Math.min(d*10, maxSz); x*=1.01) {
				aPass(x, stdCell);
			} 
		}
	}

	public void doIt() {
		System.out.println("begin execution of Gates");
		boolean nfsWedged = true;
		String homeDir;
		if (!osIsWindows()) {
			homeDir = "/home/rkao/";
		} else if (nfsWedged) {
			homeDir = "c:/a1/kao/Sun/";
		} else {
			homeDir = "x:/";
		}

		Library scratchLib = 
		  LayoutLib.openLibForModify("scratch", homeDir+"scratch");

		StdCellParams stdCell = new StdCellParams(scratchLib);
//		stdCell.enableNCC(
//			homeDir + "work/async/electric-jkl-28august/purpleFour.elib");
		stdCell.setSizeQuantizationError(0.05);
		stdCell.setMaxMosWidth(1000);

		// a normal run
		allSizes(stdCell);
		//aPass(500, stdCell);

		// test the ability to move ground bus
		stdCell.setGndY(stdCell.getGndY() - 7);
		//allSizes(stdCell);
		aPass(20, stdCell);
		stdCell.setGndY(stdCell.getGndY() + 7);

		// test different PMOS to NMOS heights
		stdCell.setNmosWellHeight(50);
		stdCell.setPmosWellHeight(100);
		//allSizes(stdCell);
		aPass(20, stdCell);

		stdCell.setNmosWellHeight(100);
		stdCell.setPmosWellHeight(50);
		//allSizes(stdCell);
		aPass(20, stdCell);
		stdCell.setNmosWellHeight(70);
		stdCell.setPmosWellHeight(70);

		Cell gallery = Gallery.makeGallery(scratchLib);
		DrcRings.addDrcRings(gallery, FILTER);
		
		LayoutLib.writeLibrary(scratchLib);

		System.out.println("done.");
	}
	public GateRegression() {
		super("Run Gate regression", User.tool, Job.Type.CHANGE, 
			  null, null, Job.Priority.ANALYSIS);
		startJob();
	}
}
