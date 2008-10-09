/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Layer.java
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
package com.sun.electric.technology;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.Setting;
import com.sun.electric.technology.xml.XmlParam;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.projectSettings.ProjSettingsNode;

import java.awt.Color;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The Layer class defines a single layer of material, out of which NodeInst and ArcInst objects are created.
 * The Layers are defined by the PrimitiveNode and ArcProto classes, and are used in the generation of geometry.
 * In addition, layers have extra information that is used for output and behavior.
 */
public class Layer 
{
    public static final double DEFAULT_THICKNESS = 0; // 3D default thickness
    public static final double DEFAULT_DISTANCE = 0; // 3D default distance
    public static final String DEFAULT_MODE = "NONE"; // 3D default transparency mode DEFAULT_FACTOR
    public static final double DEFAULT_FACTOR = 0.0; // 3D default transparency factor
    private static final double DEFAULT_AREA_COVERAGE = 10; // 10%

    /** Describes a P-type layer. */												private static final int PTYPE =          0100;
    /** Describes a N-type layer. */												private static final int NTYPE =          0200;
    /** Describes a depletion layer. */												private static final int DEPLETION =      0400;
    /** Describes a enhancement layer. */											private static final int ENHANCEMENT =   01000;
    /** Describes a light doped layer. */											private static final int LIGHT =         02000;
    /** Describes a heavy doped layer. */											private static final int HEAVY =         04000;
//    /** Describes a pseudo layer. */												private static final int PSEUDO =       010000;
    /** Describes a nonelectrical layer (does not carry signals). */				private static final int NONELEC =      020000;
    /** Describes a layer that contacts metal (used to identify contacts/vias). */	private static final int CONMETAL =     040000;
    /** Describes a layer that contacts polysilicon (used to identify contacts). */	private static final int CONPOLY =     0100000;
    /** Describes a layer that contacts diffusion (used to identify contacts). */	private static final int CONDIFF =     0200000;
    /** Describes a layer that is native. */	                                    private static final int NATIVE =      0400000;
    /** Describes a layer that is VTH or VTL */								        private static final int HLVT =      010000000;
    /** Describes a layer that is inside transistor. */								private static final int INTRANS =   020000000;
    /** Describes a thick layer. */								                    private static final int THICK =     040000000;

    private static final ArrayList<Function> metalLayers = new ArrayList<Function>();
    private static final ArrayList<Function> contactLayers = new ArrayList<Function>();
    private static final ArrayList<Function> polyLayers = new ArrayList<Function>();
    private static List<Function> allFunctions;

	/**
	 * Function is a typesafe enum class that describes the function of a layer.
	 * Functions are technology-independent and describe the nature of the layer (Metal, Polysilicon, etc.)
	 */
	public static enum Function
	{
		/** Describes an unknown layer. */						UNKNOWN   ("unknown",     0, 0, 0, 35, 0),
		/** Describes a metal layer 1. */						METAL1    ("metal-1",     1, 0, 0, 17, 0),
		/** Describes a metal layer 2. */						METAL2    ("metal-2",     2, 0, 0, 19, 0),
		/** Describes a metal layer 3. */						METAL3    ("metal-3",     3, 0, 0, 21, 0),
		/** Describes a metal layer 4. */						METAL4    ("metal-4",     4, 0, 0, 23, 0),
		/** Describes a metal layer 5. */						METAL5    ("metal-5",     5, 0, 0, 25, 0),
		/** Describes a metal layer 6. */						METAL6    ("metal-6",     6, 0, 0, 27, 0),
		/** Describes a metal layer 7. */						METAL7    ("metal-7",     7, 0, 0, 29, 0),
		/** Describes a metal layer 8. */						METAL8    ("metal-8",     8, 0, 0, 31, 0),
		/** Describes a metal layer 9. */						METAL9    ("metal-9",     9, 0, 0, 33, 0),
		/** Describes a metal layer 10. */						METAL10   ("metal-10",   10, 0, 0, 35, 0),
		/** Describes a metal layer 11. */						METAL11   ("metal-11",   11, 0, 0, 37, 0),
		/** Describes a metal layer 12. */						METAL12   ("metal-12",   12, 0, 0, 39, 0),
		/** Describes a polysilicon layer 1. */					POLY1     ("poly-1",      0, 0, 1, 12, 0),
		/** Describes a polysilicon layer 2. */					POLY2     ("poly-2",      0, 0, 2, 13, 0),
		/** Describes a polysilicon layer 3. */					POLY3     ("poly-3",      0, 0, 3, 14, 0),
		/** Describes a polysilicon gate layer. */				GATE      ("gate",        0, 0, 0, 15, Layer.INTRANS),
		/** Describes a diffusion layer. */						DIFF      ("diffusion",   0, 0, 0, 11, 0),
		/** Describes a P-diffusion layer. */					DIFFP     ("p-diffusion", 0, 0, 0, 11, Layer.PTYPE),
		/** Describes a N-diffusion layer. */					DIFFN     ("n-diffusion", 0, 0, 0, 11, Layer.NTYPE),
		/** Describes an implant layer. */						IMPLANT   ("implant",     0, 0, 0, 2, 0),
		/** Describes a P-implant layer. */						IMPLANTP  ("p-implant",   0, 0, 0, 2, Layer.PTYPE),
		/** Describes an N-implant layer. */					IMPLANTN  ("n-implant",   0, 0, 0, 2, Layer.NTYPE),
		/** Describes a contact layer 1. */						CONTACT1  ("contact-1",   0, 1, 0, 16, 0),
		/** Describes a contact layer 2. */						CONTACT2  ("contact-2",   0, 2, 0, 18, 0),
		/** Describes a contact layer 3. */						CONTACT3  ("contact-3",   0, 3, 0, 20, 0),
		/** Describes a contact layer 4. */						CONTACT4  ("contact-4",   0, 4, 0, 22, 0),
		/** Describes a contact layer 5. */						CONTACT5  ("contact-5",   0, 5, 0, 24, 0),
		/** Describes a contact layer 6. */						CONTACT6  ("contact-6",   0, 6, 0, 26, 0),
		/** Describes a contact layer 7. */						CONTACT7  ("contact-7",   0, 7, 0, 28, 0),
		/** Describes a contact layer 8. */						CONTACT8  ("contact-8",   0, 8, 0, 30, 0),
		/** Describes a contact layer 9. */						CONTACT9  ("contact-9",   0, 9, 0, 32, 0),
		/** Describes a contact layer 10. */					CONTACT10 ("contact-10",  0,10, 0, 34, 0),
		/** Describes a contact layer 11. */					CONTACT11 ("contact-11",  0,11, 0, 36, 0),
		/** Describes a contact layer 12. */					CONTACT12 ("contact-12",  0,12, 0, 38, 0),
		/** Describes a sinker (diffusion-to-buried plug). */	PLUG      ("plug",        0, 0, 0, 40, 0),
		/** Describes an overglass layer (passivation). */		OVERGLASS ("overglass",   0, 0, 0, 41, 0),
		/** Describes a resistor layer. */						RESISTOR  ("resistor",    0, 0, 0, 4, 0),
		/** Describes a capacitor layer. */						CAP       ("capacitor",   0, 0, 0, 5, 0),
		/** Describes a transistor layer. */					TRANSISTOR("transistor",  0, 0, 0, 3, 0),
		/** Describes an emitter of bipolar transistor. */		EMITTER   ("emitter",     0, 0, 0, 6, 0),
		/** Describes a base of bipolar transistor. */			BASE      ("base",        0, 0, 0, 7, 0),
		/** Describes a collector of bipolar transistor. */		COLLECTOR ("collector",   0, 0, 0, 8, 0),
		/** Describes a substrate layer. */						SUBSTRATE ("substrate",   0, 0, 0, 1, 0),
		/** Describes a well layer. */							WELL      ("well",        0, 0, 0, 0, 0),
		/** Describes a P-well layer. */						WELLP     ("p-well",      0, 0, 0, 0, Layer.PTYPE),
		/** Describes a N-well layer. */						WELLN     ("n-well",      0, 0, 0, 0, Layer.NTYPE),
		/** Describes a guard layer. */							GUARD     ("guard",       0, 0, 0, 9, 0),
		/** Describes an isolation layer (bipolar). */			ISOLATION ("isolation",   0, 0, 0, 10, 0),
		/** Describes a bus layer. */							BUS       ("bus",         0, 0, 0, 42, 0),
		/** Describes an artwork layer. */						ART       ("art",         0, 0, 0, 43, 0),
		/** Describes a control layer. */						CONTROL   ("control",     0, 0, 0, 44, 0),
        /** Describes a tileNot layer. */						TILENOT   ("tileNot",     0, 0, 0, 45, 0);

//        /** Describes a P-type layer. */												public static final int PTYPE = Layer.PTYPE;
//        /** Describes a N-type layer. */												public static final int NTYPE = Layer.NTYPE;
        /** Describes a depletion layer. */												public static final int DEPLETION = Layer.DEPLETION;
        /** Describes a enhancement layer. */											public static final int ENHANCEMENT = Layer.ENHANCEMENT;
        /** Describes a light doped layer. */											public static final int LIGHT = Layer.LIGHT;
        /** Describes a heavy doped layer. */											public static final int HEAVY = Layer.HEAVY;
//        /** Describes a pseudo layer. */												public static final int PSEUDO = Layer.PSEUDO;
        /** Describes a nonelectrical layer (does not carry signals). */				public static final int NONELEC = Layer.NONELEC;
        /** Describes a layer that contacts metal (used to identify contacts/vias). */	public static final int CONMETAL = Layer.CONMETAL;
        /** Describes a layer that contacts polysilicon (used to identify contacts). */	public static final int CONPOLY = Layer.CONPOLY;
        /** Describes a layer that contacts diffusion (used to identify contacts). */	public static final int CONDIFF = Layer.CONDIFF;
        /** Describes a layer that is VTH or VTL */								        public static final int HLVT = Layer.HLVT;
//        /** Describes a layer that is inside transistor. */								public static final int INTRANS = Layer.INTRANS;
        /** Describes a thick layer. */								                    public static final int THICK = Layer.THICK;
        /** Describes a native layer. */								                public static final int NATIVE = Layer.NATIVE;

        private final String name;
        private final boolean isMetal;
        private final boolean isContact;
        private final boolean isPoly;
		private int level;
		private final int height;
		private final int extraBits;
		private static final int [] extras = {PTYPE, NTYPE, DEPLETION, ENHANCEMENT, LIGHT, HEAVY, /*PSEUDO,*/ NONELEC, CONMETAL, CONPOLY, CONDIFF, HLVT, INTRANS, THICK};

        static {
            allFunctions = Arrays.asList(Function.class.getEnumConstants());
        }

		private Function(String name, int metalLevel, int contactLevel, int polyLevel, int height, int extraBits)
		{
			this.name = name;
			this.height = height;
			this.extraBits = extraBits;
            isMetal = metalLevel != 0;
            isContact = contactLevel != 0;
            isPoly = polyLevel != 0;
			if (isMetal) addToLayers(metalLayers, metalLevel);
			if (contactLevel != 0) addToLayers(contactLayers, contactLevel);
			if (polyLevel != 0) addToLayers(polyLayers, polyLevel);
		}

        private void addToLayers(ArrayList<Function> layers, int level) {
            this.level = level;
            while (layers.size() <= level) layers.add(null);
            Function oldFunction = layers.set(level, this);
            assert oldFunction == null;
        }

		/**
		 * Returns a printable version of this Function.
		 * @return a printable version of this Function.
		 */
		public String toString()
		{
			String toStr = name;
			for(int i=0; i<extras.length; i++)
			{
				if ((extraBits & extras[i]) == 0) continue;
				toStr += "," + getExtraName(extras[i]);
			}
			return toStr;
		}

		/**
		 * Returns the name for this Function.
		 * @return the name for this Function.
		 */
		public String getName() { return name; }

		/**
		 * Returns the constant name for this Function.
		 * Constant names are used when writing Java code, so they must be the same as the actual symbol name.
		 * @return the constant name for this Function.
		 */
		public String getConstantName() { return name(); }

		/**
		 * Method to return a list of all Layer Functions.
		 * @return a list of all Layer Functions.
		 */
		public static List<Function> getFunctions() { return allFunctions; }

		/**
		 * Method to return an array of the Layer Function "extra bits".
		 * @return an array of the Layer Function "extra bits".
		 * Each entry in the array is a single "extra bit", but they can be ORed together to combine them.
		 */
		public static int [] getFunctionExtras() { return extras; }

		/**
		 * Method to convert an "extra bits" value to a name.
		 * @param extra the extra bits value (must be a single bit, not an ORed combination).
		 * @return the name of that extra bit.
		 */
		public static String getExtraName(int extra)
		{
			if (extra == PTYPE) return "p-type";
			if (extra == NTYPE) return "n-type";
			if (extra == DEPLETION) return "depletion";
			if (extra == ENHANCEMENT) return "enhancement";
			if (extra == LIGHT) return "light";
			if (extra == HEAVY) return "heavy";
//			if (extra == PSEUDO) return "pseudo";
			if (extra == NONELEC) return "nonelectrical";
			if (extra == CONMETAL) return "connects-metal";
			if (extra == CONPOLY) return "connects-poly";
			if (extra == CONDIFF) return "connects-diff";
            if (extra == HLVT) return "vt";
			if (extra == INTRANS) return "inside-transistor";
			if (extra == THICK) return "thick";
            if (extra == NATIVE) return "native";
            return "";
		}

		/**
		 * Method to convert an "extra bits" value to a constant name.
		 * Constant names are used when writing Java code, so they must be the same as the actual symbol name.
		 * @param extra the extra bits value (must be a single bit, not an ORed combination).
		 * @return the name of that extra bit's constant.
		 */
		public static String getExtraConstantName(int extra)
		{
			if (extra == PTYPE) return "PTYPE";
			if (extra == NTYPE) return "NTYPE";
			if (extra == DEPLETION) return "DEPLETION";
			if (extra == ENHANCEMENT) return "ENHANCEMENT";
			if (extra == LIGHT) return "LIGHT";
			if (extra == HEAVY) return "HEAVY";
//			if (extra == PSEUDO) return "PSEUDO";
			if (extra == NONELEC) return "NONELEC";
			if (extra == CONMETAL) return "CONMETAL";
			if (extra == CONPOLY) return "CONPOLY";
			if (extra == CONDIFF) return "CONDIFF";
            if (extra == HLVT) return "HLVT";
			if (extra == INTRANS) return "INTRANS";
			if (extra == THICK) return "THICK";
            if (extra == NATIVE) return "NATIVE";
            return "";
		}

		/**
		 * Method to convert an "extra bits" name to its numeric value.
		 * @param name the name of the bit.
		 * @return the numeric equivalent of that bit.
		 */
		public static int parseExtraName(String name)
		{
			if (name.equalsIgnoreCase("p-type")) return PTYPE;
			if (name.equalsIgnoreCase("n-type")) return NTYPE;
			if (name.equalsIgnoreCase("depletion")) return DEPLETION;
			if (name.equalsIgnoreCase("enhancement")) return ENHANCEMENT;
			if (name.equalsIgnoreCase("light")) return LIGHT;
			if (name.equalsIgnoreCase("heavy")) return HEAVY;
//			if (name.equalsIgnoreCase("pseudo")) return PSEUDO;
			if (name.equalsIgnoreCase("nonelectrical")) return NONELEC;
			if (name.equalsIgnoreCase("connects-metal")) return CONMETAL;
			if (name.equalsIgnoreCase("connects-poly")) return CONPOLY;
			if (name.equalsIgnoreCase("connects-diff")) return CONDIFF;
			if (name.equalsIgnoreCase("inside-transistor")) return INTRANS;
			if (name.equalsIgnoreCase("thick")) return THICK;
            if (name.equalsIgnoreCase("vt")) return HLVT;
            if (name.equalsIgnoreCase("native")) return NATIVE;
            return 0;
		}

		/**
		 * Method to get the level of this Layer.
		 * The level applies to metal and polysilicon functions, and gives the layer number
		 * (i.e. Metal-2 is level 2).
		 * @return the level of this Layer.
		 */
		public int getLevel() { return level; }

		/**
		 * Method to find the Function that corresponds to Metal on a given layer.
		 * @param level the layer (starting at 1 for Metal-1).
		 * @return the Function that represents that Metal layer. Null if the given layer level is invalid.
		 */
		public static Function getMetal(int level)
		{
            if (level > EGraphics.TRANSPARENT_12)
            {
                System.out.println("Invalid metal layer level:" + level);
                return null;
            }
            Function func = metalLayers.get(level);
			return func;
		}

		/**
		 * Method to find the Function that corresponds to a contact on a given layer.
		 * @param level the layer (starting at 1 for Contact-1).
		 * @return the Function that represents that Contact layer. Null if the given layer level is invalid.
		 */
		public static Function getContact(int level)
		{
            if (level > EGraphics.TRANSPARENT_12)
            {
                System.out.println("Invalid via layer level:" + level);
                return null;
            }
			Function func = contactLayers.get(level);
			return func;
		}

		/**
		 * Method to find the Function that corresponds to Polysilicon on a given layer.
		 * @param level the layer (starting at 1 for Polysilicon-1).
		 * @return the Function that represents that Polysilicon layer.
		 */
		public static Function getPoly(int level)
		{
			Function func = polyLayers.get(level);
			return func;
		}

		/**
		 * Method to tell whether this layer function is metal.
		 * @return true if this layer function is metal.
		 */
		public boolean isMetal() { return isMetal; }

		/**
		 * Method to tell whether this layer function is diffusion (active).
		 * @return true if this layer function is diffusion (active).
		 */
		public boolean isDiff()
		{
			if (this == DIFF || this == DIFFP || this == DIFFN) return true;
			return false;
		}

		/**
		 * Method to tell whether this layer function is polysilicon.
		 * @return true if this layer function is polysilicon.
		 */
		public boolean isPoly() { return isPoly || this == GATE; };

		/**
		 * Method to tell whether this layer function is polysilicon in the gate of a transistor.
		 * @return true if this layer function is the gate of a transistor.
		 */
		public boolean isGatePoly()
		{
			if (isPoly() && (extraBits&INTRANS) != 0) return true;
			return false;
		}

		/**
		 * Method to tell whether this layer function is a contact.
		 * @return true if this layer function is contact.
		 */
		public boolean isContact() { return isContact; }

        /**
		 * Method to tell whether this layer function is a well.
		 * @return true if this layer function is a well.
		 */
		public boolean isWell()
		{
			if (this == WELL || this == WELLP || this == WELLN)  return true;
			return false;
		}

		/**
		 * Method to tell whether this layer function is substrate.
		 * @return true if this layer function is substrate.
		 */
		public boolean isSubstrate()
		{
			if (this == SUBSTRATE ||
				this == WELL || this == WELLP || this == WELLN ||
				this == IMPLANT || this == IMPLANTN || this == IMPLANTP)  return true;
			return false;
		}

		/**
		 * Method to tell whether this layer function is implant.
		 * @return true if this layer function is implant.
		 */
		public boolean isImplant()
		{
			return (this == IMPLANT || this == IMPLANTN || this == IMPLANTP);
		}

        /**
         * Method to tell whether this layer function is in subset
         * of layer functions restricted by specified number
         * of metals and polysilicons.
         * @param numMetals number of metals in subset.
         * @param numPolys number of polysilicons in subset
         * @return true if this layer function is in subset.
         */
        public boolean isUsed(int numMetals, int numPolys) {
            if (isMetal || isContact)
                return level <= numMetals;
            else if (isPoly)
                return level <= numPolys;
            else
                return true;
        }

		/**
		 * Method to tell the distance of this layer function.
		 * @return the distance of this layer function.
		 */
		public int getHeight() { return height; }

        /**
         * A set of Layer.Functions
         */
        public static class Set {
            final BitSet bits = new BitSet();
            final int extraBits; // -1 means no check extraBits
            /** Set if all Layer.Functions */
            public static final Set ALL = new Set(Function.class.getEnumConstants());

            /**
             * Constructs Function.Set from a Function plus extra bits
             * @param f Function
             * @param extraB extra bits to check
             */
            public Set(Function f, int extraB)
            {
                bits.set(f.ordinal());
                extraBits = extraB;
            }

            /**
             * Constructs Function.Set from varargs Functions.
             * @param funs variable list of Functions.
             */
            public Set(Function ... funs) {
                for (Function f: funs)
                    bits.set(f.ordinal());
                this.extraBits = noFunctionExtras; // same value as Layer.extraFunctions
            }

            /**
             * Constructs Function.Set from a collection of Functions.
             * @param funs a Collection of Functions.
             */
            public Set(Collection<Function> funs) {
                for (Function f: funs)
                    bits.set(f.ordinal());
                this.extraBits = noFunctionExtras; // same value as Layer.extraFunctions;
            }

            /**
             * Returns true if specified Functions is in this Set.
             * @param f Function to test.
             * @param extraFunction
             * @return true if specified Functions is in this Set.
             */
            public boolean contains(Function f, int extraFunction)
            {
                // Check first if there is a match in the extra bits
                boolean extraBitsM = extraBits == noFunctionExtras || (extraBits == extraFunction);
                return extraBitsM && bits.get(f.ordinal());
            }
        }
	}

    /***************************************************************************************************
     * Layer Comparators
     ***************************************************************************************************/
    /**
	 * A comparator object for sorting Layers by their name.
	 * Created once because it is used often.
	 */
    public static final LayerSortByName layerSortByName = new LayerSortByName();

	/**
	 * Comparator class for sorting Layers by their name.
	 */
	private static class LayerSortByName implements Comparator<Layer>
	{
		/**
		 * Method to compare two layers by their name.
		 * @param l1 one layer.
		 * @param l2 another layer.
		 * @return an integer indicating their sorting order.
		 */
		public int compare(Layer l1, Layer l2)
        {
			String s1 = l1.getName();
			String s2 = l2.getName();;
			return s1.compareToIgnoreCase(s2);
        }
	}

    /***************************************************************************************************
     * End of Layer Comparators
     ***************************************************************************************************/

	private final String name;
	private int index = -1; // contains index in technology or -1 for standalone layers
	private final Technology tech;
	private EGraphics graphics;
	private Function function;
    private static final int noFunctionExtras = 0;
    private int functionExtras;
    private boolean pseudo;
	private Setting cifLayerSetting;
	private Setting dxfLayerSetting;
//	private String gdsLayer;
	private Setting skillLayerSetting;
    private Setting resistanceSetting;
    private Setting capacitanceSetting;
    private Setting edgeCapacitanceSetting;
	/** the pseudo layer (if exists) */                                     private Layer pseudoLayer;
	/** the "real" layer (if this one is pseudo) */							private Layer nonPseudoLayer;
	/** true if this layer is visible */									private boolean visible;
	/** true if this layer's visibity has been initialized */				private boolean visibilityInitialized;
	/** true if dimmed (drawn darker) undimmed layers are highlighted */	private boolean dimmed;
	/** the pure-layer node that contains just this layer */				private PrimitiveNode pureLayerNode;
    /** the Xml expression for size pf pure-layer node */                   private Technology.Distance pureLayerNodeXmlSize;

//	private static Map<String,Pref> gdsLayerPrefs = new HashMap<String,Pref>();
    private static final Map<Layer,Pref> layerVisibilityPrefs = new HashMap<Layer,Pref>();

    // 3D options
	private static final Map<Layer,Pref> layer3DThicknessPrefs = new HashMap<Layer,Pref>();
	private static final Map<Layer,Pref> layer3DDistancePrefs = new HashMap<Layer,Pref>();
    private static final Map<Layer,Pref> layer3DTransModePrefs = new HashMap<Layer,Pref>(); // NONE is the default
    private static final Map<Layer,Pref> layer3DTransFactorPrefs = new HashMap<Layer,Pref>(); // 0 is the default

    private static final Map<Layer,Pref> areaCoveragePrefs = new HashMap<Layer,Pref>();  // Used by area coverage tool

	private Layer(String name, Technology tech, EGraphics graphics)
	{
		this.name = name;
		this.tech = tech;
		this.graphics = graphics;
		this.nonPseudoLayer = this;
		this.visible = true;
		visibilityInitialized = false;
		this.dimmed = false;
		this.function = Function.UNKNOWN;
	}

	/**
	 * Method to create a new layer with the given name and graphics.
	 * @param tech the Technology that this layer belongs to.
	 * @param name the name of the layer.
	 * @param graphics the appearance of the layer.
	 * @return the Layer object.
	 */
	public static Layer newInstance(Technology tech, String name, EGraphics graphics)
	{
        if (tech == null) throw new NullPointerException();
        int transparent = graphics.getFactoryTransparentLayer();
        if (transparent != 0) {
            Color colorFromMap = tech.getFactoryColorMap()[1 << (transparent - 1)];
            if ((colorFromMap.getRGB() & 0xFFFFFF) != graphics.getRGB())
                throw new IllegalArgumentException();
        }
		Layer layer = new Layer(name, tech, graphics);
		tech.addLayer(layer);          
        if (graphics.getLayer() == null)
            graphics.setLayer(layer);
		return layer;
	}

	/**
	 * Method to create a new layer with the given name and graphics.
     * Layer is not attached to any technology but still has a technology pointer.
	 * @param name the name of the layer.
	 * @param graphics the appearance of the layer.
	 * @return the Layer object.
	 */
	public static Layer newInstanceFree(Technology tech, String name, EGraphics graphics)
	{
		Layer layer = new Layer(name, tech, graphics);
		graphics.setLayer(layer);
		return layer;
	}

	/**
	 * Method to create a pseudo-layer for this Layer with a standard name "Pseudo-XXX".
	 * @return the pseudo-layer.
	 */
    public Layer makePseudo() {
            assert pseudoLayer == null;
        String pseudoLayerName = "Pseudo-" + name;
        pseudoLayer = new Layer(pseudoLayerName, tech, graphics);
        pseudoLayer.setFunction(function, functionExtras, true);
        pseudoLayer.nonPseudoLayer = this;
        return pseudoLayer;
    }

	/**
	 * Method to return the name of this Layer.
	 * @return the name of this Layer.
	 */
	public String getName() { return name; }

	/**
	 * Method to return the index of this Layer.
	 * The index is 0-based.
	 * @return the index of this Layer.
	 */
	public int getIndex() { return index; }

	/**
	 * Method to set the index of this Layer.
	 * The index is 0-based.
	 * @param index the index of this Layer.
	 */
	public void setIndex(int index) { this.index = index; }

	/**
	 * Method to return the Technology of this Layer.
	 * @return the Technology of this Layer.
	 */
	public Technology getTechnology() { return tech; }

	/**
	 * Method to return the graphics description of this Layer.
	 * @return the graphics description of this Layer.
	 */
	public EGraphics getGraphics() { return graphics; }

	/**
	 * Method to set the Function of this Layer.
	 * @param function the Function of this Layer.
	 */
	public void setFunction(Function function)
	{
		this.function = function;
		this.functionExtras = noFunctionExtras;
	}

	/**
	 * Method to set the Function of this Layer when the function is complex.
	 * Some layer functions have extra bits of information to describe them.
	 * For example, P-Type Diffusion has the Function DIFF but the extra bits PTYPE.
	 * @param function the Function of this Layer.
	 * @param functionExtras extra bits to describe the Function of this Layer.
	 */
	public void setFunction(Function function, int functionExtras)
    {
        setFunction(function, functionExtras, false);
    }

	/**
	 * Method to set the Function of this Layer when the function is complex.
	 * Some layer functions have extra bits of information to describe them.
	 * For example, P-Type Diffusion has the Function DIFF but the extra bits PTYPE.
	 * @param function the Function of this Layer.
	 * @param functionExtras extra bits to describe the Function of this Layer.
     * @param pseudo true if the Layer is pseudo-layer
	 */
	public void setFunction(Function function, int functionExtras, boolean pseudo)
	{
		this.function = function;
        int numBits = 0;
        for (int i = 0; i < 32; i++) {
            if ((functionExtras & (1 << i)) != 0)
                numBits++;
        }
        if (numBits >= 2 &&
                functionExtras != (DEPLETION|HEAVY) && functionExtras != (DEPLETION|LIGHT) &&
                functionExtras != (ENHANCEMENT|HEAVY) && functionExtras != (ENHANCEMENT|LIGHT))
            throw new IllegalArgumentException("functionExtras=" + Integer.toHexString(functionExtras));
        this.functionExtras = functionExtras;
        this.pseudo = pseudo;
	}

	/**
	 * Method to return the Function of this Layer.
	 * @return the Function of this Layer.
	 */
	public Function getFunction() { return function; }

	/**
	 * Method to return the Function "extras" of this Layer.
	 * The "extras" are a set of modifier bits, such as "p-type".
	 * @return the Function extras of this Layer.
	 */
	public int getFunctionExtras() { return functionExtras; }

	/**
	 * Method to set the Pure Layer Node associated with this Layer.
	 * @param pln the Pure Layer PrimitiveNode to use for this Layer.
	 */
	public void setPureLayerNode(PrimitiveNode pln) { pureLayerNode = pln; }

	/**
	 * Method to make the Pure Layer Node associated with this Layer.
	 * @param nodeName the name of the PrimitiveNode.
	 * Primitive names may not contain unprintable characters, spaces, tabs, a colon (:), semicolon (;) or curly braces ({}).
	 * @param size the width and the height of the PrimitiveNode.
     * @param style the Poly.Type this PrimitiveNode will generate (polygon, cross, etc.).
	 * @return the Pure Layer PrimitiveNode to use for this Layer.
	 */
	public PrimitiveNode makePureLayerNode(String nodeName, double size, Poly.Type style, String portName, ArcProto ... connections) {
        Technology.Distance d = new Technology.Distance();
        d.addLambda(size);
        return makePureLayerNode(nodeName, size, null, style, portName, connections);
    }

	/**
	 * Method to make the Pure Layer Node associated with this Layer.
	 * @param nodeName the name of the PrimitiveNode.
	 * Primitive names may not contain unprintable characters, spaces, tabs, a colon (:), semicolon (;) or curly braces ({}).
	 * @param size the width and the height of the PrimitiveNode.
     * @param xmlSize expression for default size of this pure layer node depending on tech parameters
     * @param style the Poly.Type this PrimitiveNode will generate (polygon, cross, etc.).
	 * @return the Pure Layer PrimitiveNode to use for this Layer.
	 */
	public PrimitiveNode makePureLayerNode(String nodeName, double size, Technology.Distance xmlSize, Poly.Type style, String portName, ArcProto ... connections) {
		PrimitiveNode pln = PrimitiveNode.newInstance0(nodeName, tech, size, size,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(this, 0, style, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		pln.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(tech, pln, connections, portName, 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pln.setFunction(PrimitiveNode.Function.NODE);
		pln.setHoldsOutline();
		pln.setSpecialType(PrimitiveNode.POLYGONAL);
        pureLayerNode = pln;
        pureLayerNodeXmlSize = xmlSize;
        return pln;
    }

    void resizePureLayerNode(Technology.DistanceContext context) {
        if (pureLayerNodeXmlSize == null) return;
        double lambdaSize = pureLayerNodeXmlSize.getLambda(context);
        pureLayerNode.setDefSize(lambdaSize, lambdaSize);
    }

	/**
	 * Method to return the Pure Layer Node associated with this Layer.
	 * @return the Pure Layer Node associated with this Layer.
	 */
	public PrimitiveNode getPureLayerNode() { return pureLayerNode; }

	/**
	 * Method to tell whether this layer function is non-electrical.
	 * Non-electrical layers do not carry any signal (for example, artwork, text).
	 * @return true if this layer function is non-electrical.
	 */
	public boolean isNonElectrical()
	{
		return (functionExtras&Function.NONELEC) != 0;
	}

    /**
     * Method to determine if the layer function corresponds to a diffusion layer.
     * Used in parasitic calculation
     * @return true if this Layer is diffusion.
     */
    public boolean isDiffusionLayer()
    {
		return !isPseudoLayer() && getFunction().isDiff();
    }

    /**
     * Method to determine if the layer corresponds to a VT layer. Used in DRC
     * @return true if this layer is a VT layer.
     */
    public boolean isVTImplantLayer()
    {
        return (function.isImplant() && (functionExtras&Layer.Function.HLVT) != 0);
    }

    /**
     * Method to determine if the layer corresponds to a poly cut layer. Used in 3D View
     * @return true if this layer is a poly cut layer.
     */
    public boolean isPolyCutLayer()
    {
        return (function.isContact() && (functionExtras&Layer.Function.CONPOLY) != 0);
    }

    /**
	 * Method to return true if this is pseudo-Layer.
	 * Pseudo layers are those used in pins, and have no real geometry.
	 * @return true if this is pseudo-layer.
	 */
	public boolean isPseudoLayer() { return pseudo; }
	/**
	 * Method to return the pseudo layer associated with this real-Layer.
	 * Pseudo layers are those used in pins, and have no real geometry.
	 * @return the pseudo layer associated with this read-Layer.
	 * If this layer is hass not pseudo, the null is returned.
	 */
	public Layer getPseudoLayer() { return pseudoLayer; }
	/**
	 * Method to return the non-pseudo layer associated with this pseudo-Layer.
	 * Pseudo layers are those used in pins, and have no real geometry.
	 * @return the non-pseudo layer associated with this pseudo-Layer.
	 * If this layer is already not pseudo, this layer is returned.
	 */
	public Layer getNonPseudoLayer() { return nonPseudoLayer; }

//	/**
//	 * Method to set the non-pseudo layer associated with this pseudo-Layer.
//	 * Pseudo layers are those used in pins, and have no real geometry.
//	 * @param nonPseudoLayer the non-pseudo layer associated with this pseudo-Layer.
//	 */
//	private void setNonPseudoLayer(Layer nonPseudoLayer) { this.nonPseudoLayer = nonPseudoLayer; }

	/**
	 * Method to reset the graphics on this Layer.
	 */
	public void factoryResetGraphics()
	{
    	if (tech == null) return;
		if (!visibilityInitialized)
		{
			Pref vp = getBooleanPref("Visibility", layerVisibilityPrefs, visible);
			visible = vp.getBooleanFactoryValue();
			if (vp.getBoolean() != visible) vp.setBoolean(visible);
			visibilityInitialized = true;
		}
		if (graphics.getFactoryColor() != graphics.getRGB())
			graphics.setColor(new Color(graphics.getFactoryColor()));
		if (graphics.getFactoryTransparentLayer() != graphics.getTransparentLayer())
			graphics.setTransparentLayer(graphics.getFactoryTransparentLayer());
		if (graphics.getFactoryOpacity() != graphics.getOpacity())
			graphics.setOpacity(graphics.getFactoryOpacity());
		if (graphics.getForeground() != graphics.getForeground())
			graphics.setForeground(graphics.getForeground());
		if (!graphics.getFactoryPattern().equals(graphics.getPattern()))
			graphics.setPattern(graphics.getFactoryPattern());
		if (!graphics.getFactoryOutlined().equals(graphics.getOutlined()))
			graphics.setOutlined(graphics.getFactoryOutlined());
		if (graphics.isFactoryPatternedOnDisplay() != graphics.isPatternedOnDisplay())
			graphics.setPatternedOnDisplay(graphics.isFactoryPatternedOnDisplay());
		if (graphics.isFactoryPatternedOnPrinter() != graphics.isPatternedOnPrinter())
			graphics.setPatternedOnPrinter(graphics.isFactoryPatternedOnPrinter());
	}

	/**
	 * Method to tell whether this Layer is visible.
	 * @return true if this Layer is visible.
	 */
    public boolean isVisible()
    {
    	if (tech == null) return true;
		if (!visibilityInitialized)
		{
			visible = getBooleanPref("Visibility", layerVisibilityPrefs, visible).getBoolean();
			visibilityInitialized = true;
		}
		return visible;
    }

	/**
	 * Method to set whether this Layer is visible.
	 * For efficiency, this method does not update preferences, but only changes
	 * the field variable.
	 * Changes to visibility are saved to Preferences at exit (with "preserveVisibility()").
	 * @param newVis true if this Layer is to be visible.
	 */
    public void setVisible(boolean newVis)
	{
		if (!visibilityInitialized)
		{
			visible = getBooleanPref("Visibility", layerVisibilityPrefs, visible).getBoolean();
			visibilityInitialized = true;
		}
		visible = newVis;
		PrimitiveNode.resetAllVisibility();
	}

	/**
	 * Method called when the program exits to preserve any changes to the layer visibility.
	 */
	public static void preserveVisibility()
	{
		Pref.delayPrefFlushing();
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = lIt.next();
				Pref visPref = layer.getBooleanPref("Visibility", layerVisibilityPrefs, layer.visible);
				boolean savedVis = visPref.getBoolean();
				if (savedVis != layer.visible)
				{
					visPref.setBoolean(layer.visible);
			        if (Job.getDebug()) System.err.println("Save visibility of " + layer.getName());
				}
			}
		}
		Pref.resumePrefFlushing();
	}

	/**
	 * Method to tell whether this Layer is dimmed.
	 * Dimmed layers are drawn darker so that undimmed layers can be highlighted.
	 * @return true if this Layer is dimmed.
	 */
	public boolean isDimmed() { return dimmed; }

	/**
	 * Method to set whether this Layer is dimmed.
	 * Dimmed layers are drawn darker so that undimmed layers can be highlighted.
	 * @param dimmed true if this Layer is to be dimmed.
	 */
	public void setDimmed(boolean dimmed) { this.dimmed = dimmed; }

    private Setting makeLayerSetting(String what, String factory) {
        String techName = tech.getTechName();
        return Setting.makeStringSetting(what + "LayerFor" + name + "IN" + techName, Technology.getTechnologyPreferences(),
                getSubNode(what), name,
                what + " tab", what + " for layer " + name + " in technology " + techName, factory);
    }

    private Setting makeParasiticSetting(String what, double factory)
    {
        return Setting.makeDoubleSetting(what + "ParasiticFor" + name + "IN" + tech.getTechName(),
                Technology.getTechnologyPreferences(),
                getSubNode(what), name,
                "Parasitic tab", "Technology " + tech.getTechName() + ", " + what + " for layer " + name, factory);
    }

    private ProjSettingsNode getSubNode(String type) {
        ProjSettingsNode node = tech.getProjectSettings();
        ProjSettingsNode typenode = node.getNode(type);
//        if (typenode == null) {
//            typenode = new ProjSettingsNode();
//            node.putNode(type, typenode);
//        }
        return typenode;
    }

    /**
	 * Method to get a string preference for this Layer and a specific purpose.
	 * @param what the purpose of the preference.
	 * @param map a Map of preferences for the purpose.
	 * @param factory the factory default value for this Layer/purpose.
	 * @return the string Pref object for this Layer/purpose.
	 */
    public Pref getStringPref(String what, Map<Layer,Pref> map, String factory)
	{
		Pref pref = map.get(this);
		if (pref == null)
		{
			pref = Pref.makeStringPref(what + "Of" + name + "IN" + tech.getTechName(), Technology.getTechnologyPreferences(), factory);
			map.put(this, pref);
		}
		return pref;
	}

    /**
	 * Method to get a boolean preference for this Layer and a specific purpose.
	 * @param what the purpose of the preference.
	 * @param map a Map of preferences for the purpose.
	 * @param factory the factory default value for this Layer/purpose.
	 * @return the boolean Pref object for this Layer/purpose.
	 */
    public Pref getBooleanPref(String what, Map<Layer,Pref> map, boolean factory)
	{
		Pref pref = map.get(this);
		if (pref == null)
		{
			pref = Pref.makeBooleanPref(what + "Of" + name + "IN" + tech.getTechName(), Technology.getTechnologyPreferences(), factory);
			map.put(this, pref);
		}
		return pref;
	}

	/**
	 * Method to get a double-precision preference for this Layer and a specific purpose.
	 * @param what the purpose of the preference.
	 * @param map a Map of preferences for the purpose.
	 * @param factory the factory default value for this Layer/purpose.
	 * @return the double-precision Pref object for this Layer/purpose.
	 */
	public Pref getDoublePref(String what, Map<Layer,Pref> map, double factory)
	{
		Pref pref = map.get(this);
		if (pref == null)
		{
			pref = Pref.makeDoublePref(what + "Of" + name + "IN" + tech.getTechName(), Technology.getTechnologyPreferences(), factory);
			map.put(this, pref);
		}
		return pref;
	}

	/**
	 * Method to get an integer preference for this Layer and a specific purpose.
	 * @param what the purpose of the preference.
	 * @param map a Map of preferences for the purpose.
	 * @param factory the factory default value for this Layer/purpose.
	 * @return the integer Pref object for this Layer/purpose.
	 */
	public Pref getIntegerPref(String what, Map<Layer,Pref> map, int factory)
	{
		Pref pref = map.get(this);
		if (pref == null)
		{
			pref = Pref.makeIntPref(what + "Of" + name + "IN" + tech.getTechName(), Technology.getTechnologyPreferences(), factory);
			map.put(this, pref);
		}
		return pref;
	}

	/**
	 * Method to set the 3D distance and thickness of this Layer.
	 * @param thickness the thickness of this layer.
     * @param distance the distance of this layer above the ground plane (silicon).
     * Negative values represent layes in silicon like p++, p well, etc.
     * @param mode
     * @param factor
     */
	public void setFactory3DInfo(double thickness, double distance, String mode, double factor)
	{
        assert !isPseudoLayer();

        thickness = DBMath.round(thickness);
        distance = DBMath.round(distance);
        // We don't call setDistance and setThickness directly here due to reflection code.
        getDoublePref("Distance", layer3DDistancePrefs, distance).setFactoryDouble(distance);
		getDoublePref("Thickness", layer3DThicknessPrefs, thickness).setFactoryDouble(thickness);
        if (mode != null)
            setTransparencyMode(mode);
        setTransparencyFactor(factor);
    }

    /**
	 * Method to return the transparency mode of this layer as a string.
     * Possible values "NONE, "FASTEST", "NICEST", "BLENDED", "SCREEN_DOOR".
	 * @return the transparency mode of this layer for the 3D view.
	 */
	public String getTransparencyMode() {
        if (isPseudoLayer())
            return getNonPseudoLayer().getTransparencyMode();
        return getStringPref("3DTransparencyMode", layer3DTransModePrefs, DEFAULT_MODE).getString();
    }

    /**
	 * Method to set the transparency mode of this layer.
	 * Possible values "NONE, "FASTEST", "NICEST", "BLENDED", "SCREEN_DOOR".
	 * @param mode the transparency mode of this layer.
	 */
	public void setTransparencyMode(String mode)
    {
        assert !isPseudoLayer();
        getStringPref("3DTransparencyMode", layer3DTransModePrefs, mode).setString(mode);
    }

    /**
	 * Method to return the transparency mode of this layer as a string, by default.
     * Possible values "NONE, "FASTEST", "NICEST", "BLENDED", "SCREEN_DOOR".
	 * @return the transparency mode of this layer for the 3D view, by default.
	 */
	public String getFactoryTransparencyMode() {
        if (isPseudoLayer())
            return getNonPseudoLayer().getFactoryTransparencyMode();
        return getStringPref("3DTransparencyMode", layer3DTransModePrefs, DEFAULT_MODE).getStringFactoryValue();
    }

    /**
	 * Method to return the transparency factor of this layer as a string.
     * Possible values from 0 (opaque) -> 1 (transparent)
	 * @return the transparency factor of this layer for the 3D view.
	 */
	public double getTransparencyFactor() {
        if (isPseudoLayer())
            return getNonPseudoLayer().getTransparencyFactor();
        return getDoublePref("3DTransparencyFactor", layer3DTransFactorPrefs, DEFAULT_FACTOR).getDouble();
    }

    /**
	 * Method to set the transparency factor of this layer.
	 * Layers can have a transparency from 0 (opaque) to 1(transparent).
	 * @param factor the transparency factor of this layer.
	 */
	public void setTransparencyFactor(double factor)
    {
        assert !isPseudoLayer();
        getDoublePref("3DTransparencyFactor", layer3DTransFactorPrefs, factor).setDouble(factor);
    }

    /**
	 * Method to return the transparency factor of this layer as a string, by default.
     * Possible values from 0 (opaque) -> 1 (transparent)
	 * @return the transparency factor of this layer for the 3D view, by default.
	 */
	public double getFactoryTransparencyFactor() {
        if (isPseudoLayer())
            return getNonPseudoLayer().getFactoryTransparencyFactor();
        return getDoublePref("3DTransparencyFactor", layer3DTransFactorPrefs, DEFAULT_FACTOR).getDoubleFactoryValue();
    }

    /**
	 * Method to return the distance of this layer.
	 * The higher the distance value, the farther from the wafer.
	 * @return the distance of this layer above the ground plane.
	 */
	public double Factory() {
        if (isPseudoLayer())
            return getNonPseudoLayer().getDistance();
        return getDoublePref("Distance", layer3DDistancePrefs, DEFAULT_DISTANCE).getDouble();
    }

    /**
	 * Method to return the distance of this layer, by default.
	 * The higher the distance value, the farther from the wafer.
	 * @return the distance of this layer above the ground plane, by default.
	 */
	public double getDistance() {
        if (isPseudoLayer())
            return getNonPseudoLayer().getDistance();
        return getDoublePref("Distance", layer3DDistancePrefs, DEFAULT_DISTANCE).getDouble();
    }

    /**
	 * Method to return the distance of this layer, by default.
	 * The higher the distance value, the farther from the wafer.
	 * @return the distance of this layer above the ground plane, by default.
	 */
	public double getFactoryDistance() {
        if (isPseudoLayer())
            return getNonPseudoLayer().getFactoryDistance();
        return getDoublePref("Distance", layer3DDistancePrefs, DEFAULT_DISTANCE).getDoubleFactoryValue();
    }

	/**
	 * Method to set the distance of this layer.
	 * The higher the distance value, the farther from the wafer.
	 * @param distance the distance of this layer above the ground plane.
	 */
	public void setDistance(double distance)
    {
        assert !isPseudoLayer();
        // Not done with observer/observable to avoid long list of elements attached to this class
        // so reflection will be used.
        try
        {
            Class<?> viewClass = Resources.get3DClass("View3DWindow");
            Method setMethod = viewClass.getDeclaredMethod("setZValues", new Class[] {Layer.class, Double.class, Double.class, Double.class, Double.class});
            setMethod.invoke(viewClass,  new Object[] {this, new Double(getDistance()), new Double(getThickness()), new Double(distance), new Double(getThickness())});
        } catch (Exception e) {
            String extra = (e.getMessage() != null) ? " due to: " + e.getMessage() : ".";
            System.out.print("Cannot call 3D plugin method setZValues" + extra);
//            e.printStackTrace();
        }
        getDoublePref("Distance", layer3DDistancePrefs, distance).setDouble(distance);
    }

	/**
	 * Method to calculate Z value of the upper part of the layer.
	 * Note: not called getHeight to avoid confusion
	 * with getDistance())
     * Don't call distance+thickness because those are factory values.
	 * @return Height of the layer
	 */
	public double getDepth() { return (getDistance()+getThickness()); }

	/**
	 * Method to return the thickness of this layer.
	 * Layers can have a thickness of 0, which causes them to be rendered flat.
	 * @return the thickness of this layer.
	 */
	public double getThickness() {
        if (isPseudoLayer())
            return 0;
        return getDoublePref("Thickness", layer3DThicknessPrefs, DEFAULT_THICKNESS).getDouble();
    }
	/**
	 * Method to return the thickness of this layer, by default.
	 * Layers can have a thickness of 0, which causes them to be rendered flat.
	 * @return the thickness of this layer, by default.
	 */
	public double getFactoryThickness() {
        if (isPseudoLayer())
            return 0;
        return getDoublePref("Thickness", layer3DThicknessPrefs, DEFAULT_THICKNESS).getDoubleFactoryValue();
    }

	/**
	 * Method to set the thickness of this layer.
	 * Layers can have a thickness of 0, which causes them to be rendered flat.
	 * @param thickness the thickness of this layer.
	 */
	public void setThickness(double thickness)
    {
        assert !isPseudoLayer();
        // Not done with observer/observable to avoid long list of elements attached to this class
        // so reflection will be used.
        try
        {
            Class<?> viewClass = Resources.get3DClass("View3DWindow");
            Method setMethod = viewClass.getDeclaredMethod("setZValues", new Class[] {Layer.class, Double.class, Double.class, Double.class, Double.class});
            setMethod.invoke(viewClass,  new Object[] {this, new Double(getDistance()), new Double(getThickness()), new Double(getDistance()), new Double(thickness)});
        } catch (Exception e) {
            System.out.println("Cannot call 3D plugin method setZValues: " + e.getMessage());
            e.printStackTrace();
        }
        getDoublePref("Thickness", layer3DThicknessPrefs, thickness).setDouble(thickness);
    }

	/**
	 * Method to set the factory-default CIF name of this Layer.
	 * @param cifLayer the factory-default CIF name of this Layer.
	 */
	public void setFactoryCIFLayer(String cifLayer) {
        assert !isPseudoLayer();
        cifLayerSetting = makeLayerSetting("CIF", cifLayer);
    }
	/**
	 * Method to return the CIF name of this layer.
	 * @return the CIF name of this layer.
	 */
	public String getCIFLayer() { return cifLayerSetting.getString(); }
	/**
	 * Returns project Setting to tell the CIF name of this Layer.
	 * @return project Setting to tell the CIF name of this Layer.
	 */
	public Setting getCIFLayerSetting() { return cifLayerSetting; }


    /**
     * Generate key name for GDS value depending on the foundry
     * @return
     */
//    private String getGDSPrefName(String foundry)
//    {
//        return ("GDS("+foundry+")");
//    }

	/**
	 * Method to set the factory-default GDS name of this Layer.
	 * @param factoryDefault the factory-default GDS name of this Layer.
     * @param foundry
     */
//	public void setFactoryGDSLayer(String factoryDefault, String foundry)
//    {
//        // Getting rid of spaces
//        String value = factoryDefault.replaceAll(", ", ",");
//        getLayerSetting(getGDSPrefName(foundry), gdsLayerPrefs, value);
//    }

	/**
	 * Method to set the GDS name of this Layer.
	 * @param gdsLayer the GDS name of this Layer.
	 */
//	public void setGDSLayer(String gdsLayer)
//    {
//        assert(this.gdsLayer == null);// probing gdsLayer is never used.
//		getLayerSetting(getGDSPrefName(tech.getPrefFoundry()), gdsLayerPrefs, this.gdsLayer).setString(gdsLayer);
//    }

	/**
	 * Method to return the GDS name of this layer.
	 * @return the GDS name of this layer.
	 */
//	public String getGDSLayer()
//    {
//        assert(gdsLayer == null);// probing gdsLayer is never used.
//        return getLayerSetting(getGDSPrefName(tech.getPrefFoundry()), gdsLayerPrefs, gdsLayer).getString();
//    }

	/**
	 * Method to set the factory-default DXF name of this Layer.
	 * @param dxfLayer the factory-default DXF name of this Layer.
	 */
	public void setFactoryDXFLayer(String dxfLayer) {
        assert !isPseudoLayer();
        dxfLayerSetting = makeLayerSetting("DXF", dxfLayer);
    }

	/**
	 * Method to return the DXF name of this layer.
	 * @return the DXF name of this layer.
	 */
	public String getDXFLayer()
	{
		if (dxfLayerSetting == null) return "";
		return dxfLayerSetting.getString();
	}
	/**
	 * Returns project Setting to tell the DXF name of this Layer.
	 * @return project Setting to tell the DXF name of this Layer.
	 */
    public Setting getDXFLayerSetting() { return dxfLayerSetting; }

	/**
	 * Method to set the factory-default Skill name of this Layer.
	 * @param skillLayer the factory-default Skill name of this Layer.
	 */
	public void setFactorySkillLayer(String skillLayer) {
        assert !isPseudoLayer();
        skillLayerSetting = makeLayerSetting("Skill", skillLayer);
    }
	/**
	 * Method to return the Skill name of this layer.
	 * @return the Skill name of this layer.
	 */
	public String getSkillLayer() { return skillLayerSetting.getString(); }
	/**
	 * Returns project Setting to tell the Skill name of this Layer.
	 * @return project Setting to tell the Skill name of this Layer.
	 */
	public Setting getSkillLayerSetting() { return skillLayerSetting; }

	/**
	 * Method to set the Spice parasitics for this Layer.
	 * This is typically called only during initialization.
	 * It does not set the "option" storage, as "setResistance()",
	 * "setCapacitance()", and ""setEdgeCapacitance()" do.
	 * @param resistance the resistance of this Layer.
	 * @param capacitance the capacitance of this Layer.
	 * @param edgeCapacitance the edge capacitance of this Layer.
	 */
	public void setFactoryParasitics(double resistance, double capacitance, double edgeCapacitance)
	{
        assert !isPseudoLayer();
		resistanceSetting = makeParasiticSetting("Resistance", resistance);
		capacitanceSetting = makeParasiticSetting("Capacitance", capacitance);
		edgeCapacitanceSetting = makeParasiticSetting("EdgeCapacitance", edgeCapacitance);
	}

//    /**
//     * Reset this layer's Parasitics to their factory default values
//     */
//    public void resetToFactoryParasitics()
//    {
//        double res = resistanceSetting.getDoubleFactoryValue();
//        double cap = capacitanceSetting.getDoubleFactoryValue();
//        double edgecap = edgeCapacitanceSetting.getDoubleFactoryValue();
//        setResistance(res);
//        setCapacitance(cap);
//        setEdgeCapacitance(edgecap);
//    }

	/**
	 * Method to return the resistance for this layer.
	 * @return the resistance for this layer.
	 */
	public double getResistance() { return resistanceSetting.getDouble(); }
	/**
	 * Returns project Setting to tell the resistance for this Layer.
	 * @return project Setting to tell the resistance for this Layer.
	 */
	public Setting getResistanceSetting() { return resistanceSetting; }

	/**
	 * Method to return the capacitance for this layer.
	 * @return the capacitance for this layer.
	 */
	public double getCapacitance() { return capacitanceSetting.getDouble(); }
	/**
	 * Returns project Setting to tell the capacitance for this Layer.
	 * Returns project Setting to tell the capacitance for this Layer.
	 */
	public Setting getCapacitanceSetting() { return capacitanceSetting; }

	/**
	 * Method to return the edge capacitance for this layer.
	 * @return the edge capacitance for this layer.
	 */
	public double getEdgeCapacitance() { return edgeCapacitanceSetting.getDouble(); }
    /**
     * Returns project Setting to tell the edge capacitance for this Layer.
     * Returns project Setting to tell the edge capacitance for this Layer.
     */
    public Setting getEdgeCapacitanceSetting() { return edgeCapacitanceSetting; }

    /**
	 * Method to set the minimum area to cover with this Layer in a particular cell.
	 * @param area the minimum area coverage of this layer.
	 */
//	public void setFactoryAreaCoverage(double area)
//	{
//		getDoublePref("AreaCoverageJob", areaCoveragePrefs, area).setDouble(area);
//	}

    /**
	 * Method to return the minimum area coverage that the layer must reach in the technology.
	 * @return the minimum area coverage (in percentage).
	 */
	public double getAreaCoverage() { return getDoublePref("AreaCoverageJob", areaCoveragePrefs, DEFAULT_AREA_COVERAGE).getDouble(); }

	/**
	 * Method to return the minimum area coverage that the layer must reach in the technology, by default.
	 * @return the minimum area coverage (in percentage), by default.
	 */
	public double getFactoryAreaCoverage() { return getDoublePref("AreaCoverageJob", areaCoveragePrefs, DEFAULT_AREA_COVERAGE).getDoubleFactoryValue(); }

    /**
     * Methot to set minimum area coverage that the layer must reach in the technology.
     * @param area the minimum area coverage (in percentage).
     */
	public void setAreaCoverageInfo(double area) { getDoublePref("AreaCoverageJob", areaCoveragePrefs, area).setDouble(area); }

     /**
     * Method to finish initialization of this Technology.
     */
    void finish() {
		if (resistanceSetting == null || capacitanceSetting == null || edgeCapacitanceSetting == null) {
            setFactoryParasitics(0, 0, 0);
        }
        if (cifLayerSetting == null) {
            setFactoryCIFLayer("");
        }
        if (dxfLayerSetting == null) {
            setFactoryDXFLayer("");
        }
        if (skillLayerSetting == null) {
            setFactorySkillLayer("");
        }
    }

	/**
	 * Returns a printable version of this Layer.
	 * @return a printable version of this Layer.
	 */
	public String toString()
	{
		return "Layer " + name;
	}

    void dump(PrintWriter out) {
        final String[] layerBits = {
            null, null, null,
            null, null, null,
            "PTYPE", "NTYPE", "DEPLETION",
            "ENHANCEMENT",  "LIGHT", "HEAVY",
            null, "NONELEC", "CONMETAL",
            "CONPOLY", "CONDIFF", null,
            null, null, null,
            "HLVT", "INTRANS", "THICK"
        };
        out.print("Layer " + getName() + " " + getFunction().name());
        Technology.printlnBits(out, layerBits, getFunctionExtras());
        out.print("\t"); Technology.printlnSetting(out, getCIFLayerSetting());
        out.print("\t"); Technology.printlnSetting(out, getDXFLayerSetting());
        out.print("\t"); Technology.printlnSetting(out, getSkillLayerSetting());
        out.print("\t"); Technology.printlnSetting(out, getResistanceSetting());
        out.print("\t"); Technology.printlnSetting(out, getCapacitanceSetting());
        out.print("\t"); Technology.printlnSetting(out, getEdgeCapacitanceSetting());
        // GDS
        EGraphics desc = getGraphics();
        out.println("\tpatternedOnDisplay=" + desc.isPatternedOnDisplay() + "(" + desc.isFactoryPatternedOnDisplay() + ")");
        out.println("\tpatternedOnPrinter=" + desc.isPatternedOnPrinter() + "(" + desc.isFactoryPatternedOnPrinter() + ")");
        out.println("\toutlined=" + desc.getOutlined() + "(" + desc.getFactoryOutlined() + ")");
        out.println("\ttransparent=" + desc.getTransparentLayer() + "(" + desc.getFactoryTransparentLayer() + ")");
        out.println("\tcolor=" + Integer.toHexString(desc.getColor().getRGB()) + "(" + Integer.toHexString(desc.getFactoryColor()) + ")");
        out.println("\topacity=" + desc.getOpacity() + "(" + desc.getFactoryOpacity() + ")");
        out.println("\tforeground=" + desc.getForeground());
        int pattern[] = desc.getFactoryPattern();
        out.print("\tpattern");
        for (int p: pattern)
            out.print(" " + Integer.toHexString(p));
        out.println();
        out.println("\tdistance3D=" + getDistance());
        out.println("\tthickness3D=" + getThickness());
        out.println("\tmode3D=" + getTransparencyMode());
        out.println("\tfactor3D=" + getTransparencyFactor());

        if (getPseudoLayer() != null)
            out.println("\tpseudoLayer=" + getPseudoLayer().getName());
    }

    /**
     * Method to create XML version of a Layer.
     * @return
     */
    Xml.Layer makeXml() {
        Xml.Layer l = new Xml.Layer();
        l.name = getName();
        l.function = getFunction();
        l.extraFunction = getFunctionExtras();
        l.desc = getGraphics();
        if (getThickness() != DEFAULT_THICKNESS || getDistance() != DEFAULT_DISTANCE ||
                !getTransparencyMode().equals(DEFAULT_MODE) || getTransparencyFactor() != DEFAULT_FACTOR) {
            l.thick3D = getThickness();
            l.height3D = getDistance();
            l.mode3D = getTransparencyMode();
            l.factor3D = getTransparencyFactor();
        }
        l.cif = (String)getCIFLayerSetting().getFactoryValue();
        l.skill = (String)getSkillLayerSetting().getFactoryValue();
        l.resistance = getResistanceSetting().getDoubleFactoryValue();
        l.capacitance = getCapacitanceSetting().getDoubleFactoryValue();
        l.edgeCapacitance = getEdgeCapacitanceSetting().getDoubleFactoryValue();
//            if (layer.getPseudoLayer() != null)
//                l.pseudoLayer = layer.getPseudoLayer().getName();
        if (pureLayerNode != null) {
            l.pureLayerNode = new Xml.PureLayerNode();
            l.pureLayerNode.name = pureLayerNode.getName();
            for (Map.Entry<String,PrimitiveNode> e: tech.getOldNodeNames().entrySet()) {
                if (e.getValue() != pureLayerNode) continue;
                assert l.pureLayerNode.oldName == null;
                l.pureLayerNode.oldName = e.getKey();
            }
            l.pureLayerNode.style = pureLayerNode.getLayers()[0].getStyle();
            l.pureLayerNode.port = pureLayerNode.getPort(0).getName();
//            if (pureLayerNodeXmlSize != null)
//                l.pureLayerNode.size.assign(pureLayerNodeXmlSize);
//            else
                l.pureLayerNode.size.addLambda(pureLayerNode.getDefWidth());
            for (ArcProto ap: pureLayerNode.getPort(0).getConnections()) {
                if (ap.getTechnology() != tech) continue;
                l.pureLayerNode.portArcs.add(ap.getName());
            }
        }
        return l;
    }

    /**
     * Method to create parameterized XML.
     * @return
     */
    XmlParam.Layer makeXmlParam(XmlParam.Technology t, XmlParam.DisplayStyle displayStyle) {
        XmlParam.Layer l = t.newLayer(getName());
        l.function = getFunction();
        l.extraFunction = getFunctionExtras();
        l.cif = (String)getCIFLayerSetting().getFactoryValue();
        l.skill = (String)getSkillLayerSetting().getFactoryValue();
        l.resistance = getResistanceSetting().getDoubleFactoryValue();
        l.capacitance = getCapacitanceSetting().getDoubleFactoryValue();
        l.edgeCapacitance = getEdgeCapacitanceSetting().getDoubleFactoryValue();
//            if (layer.getPseudoLayer() != null)
//                l.pseudoLayer = layer.getPseudoLayer().getName();
        if (pureLayerNode != null) {
            l.pureLayerNode = new XmlParam.PureLayerNode();
            l.pureLayerNode.name = pureLayerNode.getName();
            for (Map.Entry<String,PrimitiveNode> e: tech.getOldNodeNames().entrySet()) {
                if (e.getValue() != pureLayerNode) continue;
                assert l.pureLayerNode.oldName == null;
                l.pureLayerNode.oldName = e.getKey();
            }
            l.pureLayerNode.style = pureLayerNode.getLayers()[0].getStyle();
            l.pureLayerNode.port = pureLayerNode.getPort(0).getName();
            for (ArcProto ap: pureLayerNode.getPort(0).getConnections()) {
                if (ap.getTechnology() != tech) continue;
                l.pureLayerNode.portArcs.add(ap.getName());
            }
        }

        XmlParam.LayerDisplayStyle lds = displayStyle.newLayer(l);
        lds.desc = getGraphics();
        if (!getTransparencyMode().equals(DEFAULT_MODE) || getTransparencyFactor() != DEFAULT_FACTOR) {
            lds.mode3D = getTransparencyMode();
            lds.factor3D = getTransparencyFactor();
        }

        return l;
    }

    void makeXmlParam(XmlParam.Technology t,
            Map<XmlParam.Layer,XmlParam.Distance> thick3D, Map<XmlParam.Layer,XmlParam.Distance> height3D) {
        XmlParam.Layer l = t.findLayer(getName());
        XmlParam.Distance dist;
        dist = new XmlParam.Distance(); dist.addLambda(getThickness()); thick3D.put(l, dist);
        dist = new XmlParam.Distance(); dist.addLambda(getDistance()); height3D.put(l, dist);
    }
}
