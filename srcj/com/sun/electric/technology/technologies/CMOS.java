/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CMOS.java
 * CMOS technology description
 * Generated automatically from a library
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
package com.sun.electric.technology.technologies;

import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.NodeProto;

import java.awt.Color;

/**
 * This is the Complementary MOS (old, N-Well, from Griswold) Technology.
 */
public class CMOS extends Technology
{
	/** the Complementary MOS (old, N-Well, from Griswold) Technology object. */	public static final CMOS tech = new CMOS();

	// -------------------- private and protected methods ------------------------
	private CMOS()
	{
		setTechName("cmos");
		setTechShortName("Generic CMOS");
		setTechDesc("CMOS (N-Well, Griswold rules)");
		setFactoryScale(2000, true);   // in nanometers: really 2 microns
		setNoNegatedArcs();
		setStaticTechnology();
		setNumTransparentLayers(5);
		setColorMap(new Color []
		{
			new Color(200,200,200), //  0:      +           +         +  +      
			new Color(  0,  0,255), //  1: Metal+           +         +  +      
			new Color(223,  0,  0), //  2:      +Polysilicon+         +  +      
			new Color(150, 20,150), //  3: Metal+Polysilicon+         +  +      
			new Color(  0,255,  0), //  4:      +           +Diffusion+  +      
			new Color(  0,160,160), //  5: Metal+           +Diffusion+  +      
			new Color(180,130,  0), //  6:      +Polysilicon+Diffusion+  +      
			new Color( 55, 70,140), //  7: Metal+Polysilicon+Diffusion+  +      
			new Color(255,190,  6), //  8:      +           +         +P++      
			new Color(100,100,200), //  9: Metal+           +         +P++      
			new Color(255,114,  1), // 10:      +Polysilicon+         +P++      
			new Color( 70, 50,150), // 11: Metal+Polysilicon+         +P++      
			new Color(180,255,  0), // 12:      +           +Diffusion+P++      
			new Color( 40,160,160), // 13: Metal+           +Diffusion+P++      
			new Color(200,180, 70), // 14:      +Polysilicon+Diffusion+P++      
			new Color( 60, 60,130), // 15: Metal+Polysilicon+Diffusion+P++      
			new Color(170,140, 30), // 16:      +           +         +  +P-Well
			new Color(  0,  0,180), // 17: Metal+           +         +  +P-Well
			new Color(200,130, 10), // 18:      +Polysilicon+         +  +P-Well
			new Color( 60,  0,140), // 19: Metal+Polysilicon+         +  +P-Well
			new Color(156,220,  3), // 20:      +           +Diffusion+  +P-Well
			new Color(  0,120,120), // 21: Metal+           +Diffusion+  +P-Well
			new Color(170,170,  0), // 22:      +Polysilicon+Diffusion+  +P-Well
			new Color( 35, 50,120), // 23: Metal+Polysilicon+Diffusion+  +P-Well
			new Color(200,160, 20), // 24:      +           +         +P++P-Well
			new Color( 65, 85,140), // 25: Metal+           +         +P++P-Well
			new Color(170, 60, 80), // 26:      +Polysilicon+         +P++P-Well
			new Color( 50, 30,130), // 27: Metal+Polysilicon+         +P++P-Well
			new Color( 60,190,  0), // 28:      +           +Diffusion+P++P-Well
			new Color( 30,110,100), // 29: Metal+           +Diffusion+P++P-Well
			new Color(150, 90,  0), // 30:      +Polysilicon+Diffusion+P++P-Well
			new Color( 40, 40,110), // 31: Metal+Polysilicon+Diffusion+P++P-Well
		});

		//**************************************** LAYERS ****************************************

		/** M layer */
		Layer M_lay = Layer.newInstance(this, "Metal",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, EGraphics.TRANSPARENT_1, 0,255,0,0.8,1,
			new int[] { 0x2222,   //   X   X   X   X 
						0x0000,   //                 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x0000,   //                 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x0000,   //                 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x0000,   //                 
						0x8888,   // X   X   X   X   
						0x0000}));//                 

		/** P layer */
		Layer P_lay = Layer.newInstance(this, "Polysilicon",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, EGraphics.TRANSPARENT_2, 255,190,6,0.8,1,
			new int[] { 0x0808,   //     X       X   
						0x0404,   //      X       X  
						0x0202,   //       X       X 
						0x0101,   //        X       X
						0x8080,   // X       X       
						0x4040,   //  X       X      
						0x2020,   //   X       X     
						0x1010,   //    X       X    
						0x0808,   //     X       X   
						0x0404,   //      X       X  
						0x0202,   //       X       X 
						0x0101,   //        X       X
						0x8080,   // X       X       
						0x4040,   //  X       X      
						0x2020,   //   X       X     
						0x1010}));//    X       X    

		/** D layer */
		Layer D_lay = Layer.newInstance(this, "Diffusion",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, EGraphics.TRANSPARENT_3, 170,140,30,0.8,1,
			new int[] { 0x0000,   //                 
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X   
						0x0303,   //       XX      XX
						0x0000,   //                 
						0x3030,   //   XX      XX    
						0x8484,   // X    X  X    X  
						0x3030,   //   XX      XX    
						0x0000,   //                 
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X   
						0x0303,   //       XX      XX
						0x0000,   //                 
						0x3030,   //   XX      XX    
						0x8484,   // X    X  X    X  
						0x3030}));//   XX      XX    

		/** P0 layer */
		Layer P0_lay = Layer.newInstance(this, "P+",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, EGraphics.TRANSPARENT_4, 0,0,0,0.8,1,
			new int[] { 0x1000,   //    X            
						0x0020,   //           X     
						0x0000,   //                 
						0x0000,   //                 
						0x0001,   //                X
						0x0200,   //       X         
						0x0000,   //                 
						0x0000,   //                 
						0x1000,   //    X            
						0x0020,   //           X     
						0x0000,   //                 
						0x0000,   //                 
						0x0001,   //                X
						0x0200,   //       X         
						0x0000,   //                 
						0x0000}));//                 

		/** CC layer */
		Layer CC_lay = Layer.newInstance(this, "Contact-Cut",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 180,130,0,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** OC layer */
		Layer OC_lay = Layer.newInstance(this, "Ohmic-Cut",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 180,130,0,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** PW layer */
		Layer PW_lay = Layer.newInstance(this, "P-Well",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, EGraphics.TRANSPARENT_5, 0,0,0,0.8,1,
			new int[] { 0x0000,   //                 
						0x00c0,   //         XX      
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x00c0,   //         XX      
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x00c0,   //         XX      
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x00c0,   //         XX      
						0x0000,   //                 
						0x0000}));//                 

		/** O layer */
		Layer O_lay = Layer.newInstance(this, "Overglass",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 0,0,0,0.8,1,
			new int[] { 0x1c1c,   //    XXX     XXX  
						0x3e3e,   //   XXXXX   XXXXX 
						0x3636,   //   XX XX   XX XX 
						0x3e3e,   //   XXXXX   XXXXX 
						0x1c1c,   //    XXX     XXX  
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x1c1c,   //    XXX     XXX  
						0x3e3e,   //   XXXXX   XXXXX 
						0x3636,   //   XX XX   XX XX 
						0x3e3e,   //   XXXXX   XXXXX 
						0x1c1c,   //    XXX     XXX  
						0x0000,   //                 
						0x0000,   //                 
						0x0000}));//                 

		/** T layer */
		Layer T_lay = Layer.newInstance(this, "Transistor",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 200,200,200,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** PM layer */
		Layer PM_lay = Layer.newInstance(this, "Pseudo-Metal",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, EGraphics.TRANSPARENT_1, 0,255,0,0.8,1,
			new int[] { 0x2222,   //   X   X   X   X 
						0x0000,   //                 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x0000,   //                 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x0000,   //                 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x0000,   //                 
						0x8888,   // X   X   X   X   
						0x0000}));//                 

		/** PP layer */
		Layer PP_lay = Layer.newInstance(this, "Pseudo-Polysilicon",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, EGraphics.TRANSPARENT_2, 255,190,6,0.8,1,
			new int[] { 0x0808,   //     X       X   
						0x0404,   //      X       X  
						0x0202,   //       X       X 
						0x0101,   //        X       X
						0x8080,   // X       X       
						0x4040,   //  X       X      
						0x2020,   //   X       X     
						0x1010,   //    X       X    
						0x0808,   //     X       X   
						0x0404,   //      X       X  
						0x0202,   //       X       X 
						0x0101,   //        X       X
						0x8080,   // X       X       
						0x4040,   //  X       X      
						0x2020,   //   X       X     
						0x1010}));//    X       X    

		/** PD layer */
		Layer PD_lay = Layer.newInstance(this, "Pseudo-Diffusion",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, EGraphics.TRANSPARENT_3, 170,140,30,0.8,1,
			new int[] { 0x0000,   //                 
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X   
						0x0303,   //       XX      XX
						0x0000,   //                 
						0x3030,   //   XX      XX    
						0x8484,   // X    X  X    X  
						0x3030,   //   XX      XX    
						0x0000,   //                 
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X   
						0x0303,   //       XX      XX
						0x0000,   //                 
						0x3030,   //   XX      XX    
						0x8484,   // X    X  X    X  
						0x3030}));//   XX      XX    

		/** PP0 layer */
		Layer PP0_lay = Layer.newInstance(this, "Pseudo-P+",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, EGraphics.TRANSPARENT_4, 0,0,0,0.8,1,
			new int[] { 0x1000,   //    X            
						0x0020,   //           X     
						0x0000,   //                 
						0x0000,   //                 
						0x0001,   //                X
						0x0200,   //       X         
						0x0000,   //                 
						0x0000,   //                 
						0x1000,   //    X            
						0x0020,   //           X     
						0x0000,   //                 
						0x0000,   //                 
						0x0001,   //                X
						0x0200,   //       X         
						0x0000,   //                 
						0x0000}));//                 

		/** PPW layer */
		Layer PPW_lay = Layer.newInstance(this, "Pseudo-P-Well",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, EGraphics.TRANSPARENT_5, 0,0,0,0.8,1,
			new int[] { 0x0000,   //                 
						0x00c0,   //         XX      
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x00c0,   //         XX      
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x00c0,   //         XX      
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x00c0,   //         XX      
						0x0000,   //                 
						0x0000}));//                 

		// The layer functions
		M_lay.setFunction(Layer.Function.METAL1);		// Metal
		P_lay.setFunction(Layer.Function.POLY1);		// Polysilicon
		D_lay.setFunction(Layer.Function.DIFF);		// Diffusion
		P0_lay.setFunction(Layer.Function.IMPLANTP);		// P+
		CC_lay.setFunction(Layer.Function.CONTACT1);		// Contact-Cut
		OC_lay.setFunction(Layer.Function.CONTACT2);		// Ohmic-Cut
		PW_lay.setFunction(Layer.Function.WELLP);		// P-Well
		O_lay.setFunction(Layer.Function.OVERGLASS);		// Overglass
		T_lay.setFunction(Layer.Function.TRANSISTOR, Layer.Function.PSEUDO);		// Transistor
		PM_lay.setFunction(Layer.Function.METAL1, Layer.Function.PSEUDO);		// Pseudo-Metal
		PP_lay.setFunction(Layer.Function.POLY1, Layer.Function.PSEUDO);		// Pseudo-Polysilicon
		PD_lay.setFunction(Layer.Function.DIFF, Layer.Function.PSEUDO);		// Pseudo-Diffusion
		PP0_lay.setFunction(Layer.Function.IMPLANTP, Layer.Function.PSEUDO);		// Pseudo-P+
		PPW_lay.setFunction(Layer.Function.WELLP, Layer.Function.PSEUDO);		// Pseudo-P-Well

		// The CIF names
		M_lay.setFactoryCIFLayer("CM");		// Metal
		P_lay.setFactoryCIFLayer("CP");		// Polysilicon
		D_lay.setFactoryCIFLayer("CD");		// Diffusion
		P0_lay.setFactoryCIFLayer("CS");	// P+
		CC_lay.setFactoryCIFLayer("CC");	// Contact-Cut
		OC_lay.setFactoryCIFLayer("CC");	// Ohmic-Cut
		PW_lay.setFactoryCIFLayer("CW");	// P-Well
		O_lay.setFactoryCIFLayer("CG");		// Overglass
		T_lay.setFactoryCIFLayer("");		// Transistor
		PM_lay.setFactoryCIFLayer("");		// Pseudo-Metal
		PP_lay.setFactoryCIFLayer("");		// Pseudo-Polysilicon
		PD_lay.setFactoryCIFLayer("");		// Pseudo-Diffusion
		PP0_lay.setFactoryCIFLayer("");		// Pseudo-P+
		PPW_lay.setFactoryCIFLayer("");		// Pseudo-P-Well

		// The DXF names
		M_lay.setFactoryDXFLayer("");		// Metal
		P_lay.setFactoryDXFLayer("");		// Polysilicon
		D_lay.setFactoryDXFLayer("");		// Diffusion
		P0_lay.setFactoryDXFLayer("");		// P+
		CC_lay.setFactoryDXFLayer("");		// Contact-Cut
		OC_lay.setFactoryDXFLayer("");		// Ohmic-Cut
		PW_lay.setFactoryDXFLayer("");		// P-Well
		O_lay.setFactoryDXFLayer("");		// Overglass
		T_lay.setFactoryDXFLayer("");		// Transistor
		PM_lay.setFactoryDXFLayer("");		// Pseudo-Metal
		PP_lay.setFactoryDXFLayer("");		// Pseudo-Polysilicon
		PD_lay.setFactoryDXFLayer("");		// Pseudo-Diffusion
		PP0_lay.setFactoryDXFLayer("");		// Pseudo-P+
		PPW_lay.setFactoryDXFLayer("");		// Pseudo-P-Well

		// The GDS names
		M_lay.setFactoryGDSLayer("");		// Metal
		P_lay.setFactoryGDSLayer("");		// Polysilicon
		D_lay.setFactoryGDSLayer("");		// Diffusion
		P0_lay.setFactoryGDSLayer("");		// P+
		CC_lay.setFactoryGDSLayer("");		// Contact-Cut
		OC_lay.setFactoryGDSLayer("");		// Ohmic-Cut
		PW_lay.setFactoryGDSLayer("");		// P-Well
		O_lay.setFactoryGDSLayer("");		// Overglass
		T_lay.setFactoryGDSLayer("");		// Transistor
		PM_lay.setFactoryGDSLayer("");		// Pseudo-Metal
		PP_lay.setFactoryGDSLayer("");		// Pseudo-Polysilicon
		PD_lay.setFactoryGDSLayer("");		// Pseudo-Diffusion
		PP0_lay.setFactoryGDSLayer("");		// Pseudo-P+
		PPW_lay.setFactoryGDSLayer("");		// Pseudo-P-Well

		//******************** ARCS ********************

		/** Metal arc */
		PrimitiveArc Metal_arc = PrimitiveArc.newInstance(this, "Metal", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(M_lay, 0, Poly.Type.FILLED)
		});
		Metal_arc.setFunction(PrimitiveArc.Function.METAL1);
		Metal_arc.setFactoryFixedAngle(true);
		Metal_arc.setWipable();
		Metal_arc.setFactoryAngleIncrement(90);

		/** Polysilicon arc */
		PrimitiveArc Polysilicon_arc = PrimitiveArc.newInstance(this, "Polysilicon", 2, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P_lay, 0, Poly.Type.FILLED)
		});
		Polysilicon_arc.setFunction(PrimitiveArc.Function.POLY1);
		Polysilicon_arc.setFactoryFixedAngle(true);
		Polysilicon_arc.setWipable();
		Polysilicon_arc.setFactoryAngleIncrement(90);

		/** Diffusion-p arc */
		PrimitiveArc Diffusion_p_arc = PrimitiveArc.newInstance(this, "Diffusion-p", 6, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(D_lay, 4, Poly.Type.FILLED),
			new Technology.ArcLayer(P0_lay, 0, Poly.Type.FILLED)
		});
		Diffusion_p_arc.setFunction(PrimitiveArc.Function.DIFFP);
		Diffusion_p_arc.setFactoryFixedAngle(true);
		Diffusion_p_arc.setWipable();
		Diffusion_p_arc.setFactoryAngleIncrement(90);
		Diffusion_p_arc.setWidthOffset(0);

		/** Diffusion-well arc */
		PrimitiveArc Diffusion_well_arc = PrimitiveArc.newInstance(this, "Diffusion-well", 8, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(D_lay, 6, Poly.Type.FILLED),
			new Technology.ArcLayer(PW_lay, 0, Poly.Type.FILLED)
		});
		Diffusion_well_arc.setFunction(PrimitiveArc.Function.DIFFN);
		Diffusion_well_arc.setFactoryFixedAngle(true);
		Diffusion_well_arc.setWipable();
		Diffusion_well_arc.setFactoryAngleIncrement(90);
		Diffusion_well_arc.setWidthOffset(0);

		//******************** RECTANGLE DESCRIPTIONS ********************

		Technology.TechPoint [] box_1 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromCenter(1), EdgeV.fromBottom(3)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(3)),
		};
		Technology.TechPoint [] box_2 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromBottom(2)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromTop(2)),
		};
		Technology.TechPoint [] box_3 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.fromCenter(1), EdgeV.makeTopEdge()),
		};
		Technology.TechPoint [] box_4 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(3)),
			new Technology.TechPoint(EdgeH.fromCenter(1), EdgeV.fromTop(3)),
		};
		Technology.TechPoint [] box_5 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5)),
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.fromTop(1.5)),
		};
		Technology.TechPoint [] box_6 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(4)),
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.fromTop(4)),
		};
		Technology.TechPoint [] box_7 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.fromBottom(4)),
			new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(4)),
		};
		Technology.TechPoint [] box_8 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromRight(3), EdgeV.makeCenter()),
		};
		Technology.TechPoint [] box_9 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.makeCenter()),
			new Technology.TechPoint(EdgeH.fromRight(3), EdgeV.fromTop(1)),
		};
		Technology.TechPoint [] box_10 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromRight(3), EdgeV.fromTop(1)),
		};
		Technology.TechPoint [] box_11 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(3)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(3)),
		};
		Technology.TechPoint [] box_12 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.makeCenter()),
		};
		Technology.TechPoint [] box_13 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.makeCenter()),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.makeTopEdge()),
		};
		Technology.TechPoint [] box_14 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.makeTopEdge()),
		};
		Technology.TechPoint [] box_15 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromBottom(2)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromTop(2)),
		};
		Technology.TechPoint [] box_16 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(3)),
		};
		Technology.TechPoint [] box_17 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(3)),
			new Technology.TechPoint(EdgeH.fromRight(3), EdgeV.fromTop(3)),
		};
		Technology.TechPoint [] box_18 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromBottom(2)),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.fromTop(2)),
		};
		Technology.TechPoint [] box_19 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge()),
		};

		//******************** NODES ********************

		/** Metal-Pin */
		PrimitiveNode mp_node = PrimitiveNode.newInstance("Metal-Pin", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PM_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_19)
			});
		mp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mp_node, new ArcProto [] {Metal_arc}, "metal", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mp_node.setFunction(NodeProto.Function.PIN);
		mp_node.setArcsWipe();
		mp_node.setArcsShrink();

		/** Polysilicon-Pin */
		PrimitiveNode pp_node = PrimitiveNode.newInstance("Polysilicon-Pin", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PP_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_19)
			});
		pp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pp_node, new ArcProto [] {Polysilicon_arc}, "polysilicon", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		pp_node.setFunction(NodeProto.Function.PIN);
		pp_node.setArcsWipe();
		pp_node.setArcsShrink();

		/** Diffusion-P-Pin */
		PrimitiveNode dpp_node = PrimitiveNode.newInstance("Diffusion-P-Pin", this, 6, 6, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PP0_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_19),
				new Technology.NodeLayer(PD_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_18)
			});
		dpp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, dpp_node, new ArcProto [] {Diffusion_p_arc}, "diff-p", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.fromRight(3), EdgeV.fromTop(3))
			});
		dpp_node.setFunction(NodeProto.Function.PIN);
		dpp_node.setArcsWipe();
		dpp_node.setArcsShrink();

		/** Diffusion-Well-Pin */
		PrimitiveNode dwp_node = PrimitiveNode.newInstance("Diffusion-Well-Pin", this, 8, 8, new SizeOffset(3, 3, 3, 3),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PPW_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_19),
				new Technology.NodeLayer(PD_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_17)
			});
		dwp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, dwp_node, new ArcProto [] {Diffusion_well_arc}, "diff-w", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(4), EdgeH.fromRight(4), EdgeV.fromTop(4))
			});
		dwp_node.setFunction(NodeProto.Function.PIN);
		dwp_node.setArcsWipe();
		dwp_node.setArcsShrink();

		/** Metal-Polysilicon-Con */
		PrimitiveNode mpc_node = PrimitiveNode.newInstance("Metal-Polysilicon-Con", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_19),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_19),
				new Technology.NodeLayer(CC_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_16)
			});
		mpc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mpc_node, new ArcProto [] {Polysilicon_arc, Metal_arc}, "metal-poly", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		mpc_node.setFunction(NodeProto.Function.CONTACT);
		mpc_node.setSpecialType(PrimitiveNode.MULTICUT);
		mpc_node.setSpecialValues(new double [] {2, 2, 1, 2});

		/** Metal-Diff-P-Con */
		PrimitiveNode mdpc_node = PrimitiveNode.newInstance("Metal-Diff-P-Con", this, 8, 8, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_19),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_18),
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_18),
				new Technology.NodeLayer(CC_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_16)
			});
		mdpc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mdpc_node, new ArcProto [] {Diffusion_p_arc, Metal_arc}, "metal-diff-p", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.fromRight(3), EdgeV.fromTop(3))
			});
		mdpc_node.setFunction(NodeProto.Function.CONTACT);
		mdpc_node.setSpecialType(PrimitiveNode.MULTICUT);
		mdpc_node.setSpecialValues(new double [] {2, 2, 1, 2});

		/** Metal-Diff-Well-Con */
		PrimitiveNode mdwc_node = PrimitiveNode.newInstance("Metal-Diff-Well-Con", this, 10, 10, new SizeOffset(3, 3, 3, 3),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_17),
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_17),
				new Technology.NodeLayer(PW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_19),
				new Technology.NodeLayer(CC_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_16)
			});
		mdwc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mdwc_node, new ArcProto [] {Diffusion_well_arc, Metal_arc}, "metal-diff-w", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(4), EdgeH.fromRight(4), EdgeV.fromTop(4))
			});
		mdwc_node.setFunction(NodeProto.Function.CONTACT);
		mdwc_node.setSpecialType(PrimitiveNode.MULTICUT);
		mdwc_node.setSpecialValues(new double [] {2, 2, 1, 2});

		/** Transistor */
		PrimitiveNode t_node = PrimitiveNode.newInstance("Transistor", this, 6, 6, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_15, 1, 1, 2, 2),
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_14, 3, 3, 0, 0),
				new Technology.NodeLayer(P0_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_19, 3, 3, 2, 2)
			});
		t_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, t_node, new ArcProto [] {Polysilicon_arc}, "trans-poly-left", 180,85, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(3), EdgeH.fromLeft(1), EdgeV.fromTop(3)),
				PrimitivePort.newInstance(this, t_node, new ArcProto [] {Diffusion_p_arc}, "trans-diff-top", 90,85, 2, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromTop(1), EdgeH.fromRight(3), EdgeV.fromTop(1)),
				PrimitivePort.newInstance(this, t_node, new ArcProto [] {Polysilicon_arc}, "trans-poly-right", 0,85, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromRight(1), EdgeV.fromBottom(3), EdgeH.fromRight(1), EdgeV.fromTop(3)),
				PrimitivePort.newInstance(this, t_node, new ArcProto [] {Diffusion_p_arc}, "trans-diff-bottom", 270,85, 3, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(1), EdgeH.fromRight(3), EdgeV.fromBottom(1))
			});
		t_node.setFunction(NodeProto.Function.TRANMOS);
		t_node.setHoldsOutline();
		t_node.setCanShrink();
		t_node.setSpecialType(PrimitiveNode.SERPTRANS);
		t_node.setSpecialValues(new double [] {0.0333333, 1, 1, 2, 1, 1});

		/** Transistor-Well */
		PrimitiveNode tw_node = PrimitiveNode.newInstance("Transistor-Well", this, 8, 8, new SizeOffset(3, 3, 3, 3),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_11, 1, 1, 2, 2),
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_10, 3, 3, 0, 0),
				new Technology.NodeLayer(PW_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_19, 4, 4, 3, 3)
			});
		tw_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, tw_node, new ArcProto [] {Polysilicon_arc}, "transw-poly-left", 180,85, 2, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(4), EdgeH.fromLeft(2), EdgeV.fromTop(4)),
				PrimitivePort.newInstance(this, tw_node, new ArcProto [] {Diffusion_well_arc}, "transw-diff-top", 90,85, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromTop(2), EdgeH.fromRight(4), EdgeV.fromTop(2)),
				PrimitivePort.newInstance(this, tw_node, new ArcProto [] {Polysilicon_arc}, "transw-poly-right", 0,85, 2, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromRight(2), EdgeV.fromBottom(4), EdgeH.fromRight(2), EdgeV.fromTop(4)),
				PrimitivePort.newInstance(this, tw_node, new ArcProto [] {Diffusion_well_arc}, "transw-diff-bottom", 270,85, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(2), EdgeH.fromRight(4), EdgeV.fromBottom(2))
			});
		tw_node.setFunction(NodeProto.Function.TRAPMOS);
		tw_node.setHoldsOutline();
		tw_node.setCanShrink();
		tw_node.setSpecialType(PrimitiveNode.SERPTRANS);
		tw_node.setSpecialValues(new double [] {0.0333333, 1, 1, 2, 1, 1});

		/** Metal-Diff-Split-Cut */
		PrimitiveNode mdsc_node = PrimitiveNode.newInstance("Metal-Diff-Split-Cut", this, 14, 10, new SizeOffset(3, 3, 3, 3),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_17),
				new Technology.NodeLayer(P0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_5),
				new Technology.NodeLayer(PW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_19),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_17),
				new Technology.NodeLayer(CC_lay, 1, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_7),
				new Technology.NodeLayer(OC_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_6)
			});
		mdsc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mdsc_node, new ArcProto [] {Metal_arc}, "metal-diff-splw-l", 180,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(4), EdgeH.fromCenter(-1), EdgeV.fromTop(4)),
				PrimitivePort.newInstance(this, mdsc_node, new ArcProto [] {Diffusion_well_arc, Metal_arc}, "metal-diff-splw-r", 0,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromCenter(1), EdgeV.fromBottom(4), EdgeH.fromRight(4), EdgeV.fromTop(4))
			});
		mdsc_node.setFunction(NodeProto.Function.WELL);

		/** Metal-Diff-SplitN-Cut */
		PrimitiveNode mdsc0_node = PrimitiveNode.newInstance("Metal-Diff-SplitN-Cut", this, 10, 8, new SizeOffset(2, 0, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_3),
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2),
				new Technology.NodeLayer(CC_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_4),
				new Technology.NodeLayer(OC_lay, 1, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_1)
			});
		mdsc0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mdsc0_node, new ArcProto [] {Diffusion_p_arc, Metal_arc}, "metal-diff-splp-l", 0,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.makeCenter(), EdgeV.fromTop(3)),
				PrimitivePort.newInstance(this, mdsc0_node, new ArcProto [] {Metal_arc}, "metal-diff-splp-r", 180,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromCenter(2), EdgeV.fromBottom(3), EdgeH.fromRight(1), EdgeV.fromTop(3))
			});
		mdsc0_node.setFunction(NodeProto.Function.SUBSTRATE);

		/** Metal-Node */
		PrimitiveNode mn_node = PrimitiveNode.newInstance("Metal-Node", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_19)
			});
		mn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mn_node, new ArcProto [] {Metal_arc}, "metal", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mn_node.setFunction(NodeProto.Function.NODE);
		mn_node.setHoldsOutline();
		mn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Polysilicon-Node */
		PrimitiveNode pn_node = PrimitiveNode.newInstance("Polysilicon-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_19)
			});
		pn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn_node, new ArcProto [] {Polysilicon_arc}, "polysilicon", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		pn_node.setFunction(NodeProto.Function.NODE);
		pn_node.setHoldsOutline();
		pn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Diffusion-Node */
		PrimitiveNode dn_node = PrimitiveNode.newInstance("Diffusion-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_19)
			});
		dn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, dn_node, new ArcProto [] {}, "diffusion", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		dn_node.setFunction(NodeProto.Function.NODE);
		dn_node.setHoldsOutline();
		dn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** P-Node */
		PrimitiveNode pn0_node = PrimitiveNode.newInstance("P-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_19)
			});
		pn0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn0_node, new ArcProto [] {}, "p+", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		pn0_node.setFunction(NodeProto.Function.NODE);
		pn0_node.setHoldsOutline();
		pn0_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Cut-Node */
		PrimitiveNode cn_node = PrimitiveNode.newInstance("Cut-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(CC_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_19)
			});
		cn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, cn_node, new ArcProto [] {}, "cut", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		cn_node.setFunction(NodeProto.Function.NODE);
		cn_node.setHoldsOutline();
		cn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Ohmic-Cut-Node */
		PrimitiveNode ocn_node = PrimitiveNode.newInstance("Ohmic-Cut-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(OC_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_19)
			});
		ocn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, ocn_node, new ArcProto [] {}, "ohmic-cut", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		ocn_node.setFunction(NodeProto.Function.NODE);
		ocn_node.setHoldsOutline();
		ocn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Well-Node */
		PrimitiveNode wn_node = PrimitiveNode.newInstance("Well-Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_19)
			});
		wn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, wn_node, new ArcProto [] {}, "well", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		wn_node.setFunction(NodeProto.Function.NODE);
		wn_node.setHoldsOutline();
		wn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Overglass-Node */
		PrimitiveNode on_node = PrimitiveNode.newInstance("Overglass-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(O_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_19)
			});
		on_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, on_node, new ArcProto [] {}, "overglass", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		on_node.setFunction(NodeProto.Function.NODE);
		on_node.setHoldsOutline();
		on_node.setSpecialType(PrimitiveNode.POLYGONAL);

		// The pure layer nodes
		M_lay.setPureLayerNode(mn_node);		// Metal
		P_lay.setPureLayerNode(pn_node);		// Polysilicon
		D_lay.setPureLayerNode(dn_node);		// Diffusion
		P0_lay.setPureLayerNode(pn0_node);		// P+
		CC_lay.setPureLayerNode(cn_node);		// Contact-Cut
		OC_lay.setPureLayerNode(ocn_node);		// Ohmic-Cut
		PW_lay.setPureLayerNode(wn_node);		// P-Well
		O_lay.setPureLayerNode(on_node);		// Overglass
	};
}
