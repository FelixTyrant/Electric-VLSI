/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FileType.java
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
package com.sun.electric.tool.io;

import javax.swing.filechooser.FileFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.FilenameFilter;
import java.io.File;

/**
 * A typesafe enum class that describes the types of files that can be read or written.
 */
public class FileType {
	/** all types */                        private static final ArrayList allTypes = new ArrayList();

	/** Describes any file.*/				public static final FileType ANY          = new FileType("All", new String[] {}, "All Files");
	/** Describes CDL decks.*/				public static final FileType CDL          = new FileType("CDL", new String[] {"cdl"}, "CDL Deck (cdl)");
	/** Describes CIF files. */				public static final FileType CIF          = new FileType("CIF", new String[] {"cif"}, "CIF File (cif)");
	/** Describes COSMOS output. */			public static final FileType COSMOS       = new FileType("COSMOS", new String[] {"sim"}, "COSMOS File (sim)");
	/** Describes DEF output. */			public static final FileType DEF          = new FileType("DEF", new String[] {"def"}, "DEF File (def)");
	/** Describes DXF output. */			public static final FileType DXF          = new FileType("DXF", new String[] {"dxf"}, "DXF File (dxf)");
	/** Describes Eagle files.*/			public static final FileType EAGLE        = new FileType("Eagle", new String[] {"txt"}, "Eagle File (txt)");
	/** Describes ECAD files.*/				public static final FileType ECAD         = new FileType("ECAD", new String[] {"enl"}, "ECAD File (enl)");
	/** Describes EDIF files.*/				public static final FileType EDIF         = new FileType("EDIF", new String[] {"edif"}, "EDIF File (edif)");
	/** Describes ELIB files.*/				public static final FileType ELIB         = new FileType("ELIB", new String[] {"elib"}, "Library File (elib)");
	/** Describes Encapsulated PS files. */	public static final FileType EPS          = new FileType("Encapsulated PostScript", new String[] {"eps"}, "Encapsulated PostScript (eps)");
	/** Describes ESIM/RNL output. */		public static final FileType ESIM         = new FileType("ESIM", new String[] {"sim"}, "ESIM File (sim)");
	/** Describes FastHenry files.*/		public static final FileType FASTHENRY    = new FileType("FastHenry", new String[] {"inp"}, "FastHenry File (inp)");
	/** Describes GDS files. */				public static final FileType GDS          = new FileType("GDS", new String[] {"gds"}, "GDS File (gds)");
	/** Describes HSpice output. */			public static final FileType HSPICEOUT    = new FileType("HSpiceOutput", new String[] {"tr0", "pa0"}, "HSpice Output File (tr0/pa0)");
	/** Describes HTML files. */			public static final FileType HTML         = new FileType("HTML", new String[] {"html"}, "HTML File (html)");
	/** Describes IRSIM decks. */			public static final FileType IRSIM        = new FileType("IRSIM", new String[] {"sim"}, "IRSIM Deck (sim)");
	/** Describes IRSIM parameter decks. */	public static final FileType IRSIMPARAM   = new FileType("IRSIM Parameters", new String[] {"prm"}, "IRSIM Parameter Deck (prm)");
	/** Describes IRSIM vector decks. */	public static final FileType IRSIMVECTOR  = new FileType("IRSIM Vectors", new String[] {"cmd"}, "IRSIM Vector Deck (cmd)");
	/** Describes Java source. */			public static final FileType JAVA         = new FileType("Java", new String[] {"java", "bsh"}, "Java Script File (java, bsh)");
	/** Describes JELIB files.*/			public static final FileType JELIB        = new FileType("JELIB", new String[] {"jelib"}, "Library File (jelib)");
    /** Describes J3D files.*/				public static final FileType J3D          = new FileType("J3D", new String[] {"j3d"}, "Java3D Demo File (j3d}");
    /** Describes L files.*/				public static final FileType L            = new FileType("L", new String[] {"L"}, "L File (L)");
	/** Describes LEF files.*/				public static final FileType LEF          = new FileType("LEF", new String[] {"lef"}, "LEF File (lef)");
	/** Describes Maxwell decks. */			public static final FileType MAXWELL      = new FileType("Maxwell", new String[] {"mac"}, "Maxwell Deck (mac)");
	/** Describes MOSSIM decks. */			public static final FileType MOSSIM       = new FileType("MOSSIM", new String[] {"ntk"}, "MOSSIM Deck (ntk)");
    /** Describes Movie files. */			public static final FileType MOV          = new FileType("Movie", new String[] {"mov"}, "Movie File (mov)");
    /** Describes Pad Frame Array spec. */	public static final FileType PADARR       = new FileType("PadArray", new String[] {"arr"}, "Pad Generator Array File (arr)");
	/** Describes Pads files. */			public static final FileType PADS         = new FileType("Pads", new String[] {"asc"}, "Pads File (asc)");
	/** Describes PAL files. */				public static final FileType PAL          = new FileType("PAL", new String[] {"pal"}, "PAL File (pal)");
	/** Describes PostScript files. */		public static final FileType POSTSCRIPT   = new FileType("PostScript", new String[] {"ps"}, "PostScript (ps)");
	/** Describes PostScript files. */		public static final FileType PNG          = new FileType("PNG", new String[] {"png"}, "PNG (png)");
	/** Describes Preferences files. */		public static final FileType PREFS        = new FileType("Preferences", new String[] {"xml"}, "Preferences (xml)");
	/** Describes PSpice standard output.*/	public static final FileType PSPICEOUT    = new FileType("PSpiceOutput", new String[] {"spo"}, "PSpice/Spice3 Output File (spo)");
	/** Describes Raw Spice output. */		public static final FileType RAWSPICEOUT  = new FileType("RawSpiceOutput", new String[] {"raw"}, "Spice Raw Output File (raw)");
	/** Describes Raw SmartSpice output. */	public static final FileType RAWSSPICEOUT = new FileType("RawSmartSpiceOutput", new String[] {"raw"}, "SmartSPICE Raw Output File (raw)");
	/** Describes Readable Dump files. */	public static final FileType READABLEDUMP = new FileType("ReadableDump", new String[] {"txt"}, "Readable Dump Library File (txt)");
	/** Describes RSIM output. */			public static final FileType RSIM         = new FileType("RSIM", new String[] {"sim"}, "RSIM File (sim)");
	/** Describes Silos decks.*/			public static final FileType SILOS        = new FileType("Silos", new String[] {"sil"}, "Silos Deck (sil)");
	/** Describes Skill decks.*/			public static final FileType SKILL        = new FileType("Skill", new String[] {"il"}, "Skill Deck (il)");
    /** Describes Skill decks.*/			public static final FileType SKILLEXPORTSONLY        = new FileType("SkillExportsOnly", new String[] {"il"}, "Skill Deck (il)");
	/** Describes Spice decks.*/			public static final FileType SPICE        = new FileType("Spice", new String[] {"spi", "sp"}, "Spice Deck (spi, sp)");
	/** Describes Spice standard output.*/	public static final FileType SPICEOUT     = new FileType("SpiceOutput", new String[] {"spo"}, "Spice/GNUCap Output File (spo)");
	/** Describes Sue files.*/				public static final FileType SUE          = new FileType("Sue", new String[] {"sue"}, "Sue File (sue)");
	/** Describes Tegas files. */			public static final FileType TEGAS        = new FileType("Tegas", new String[] {"tdl"}, "Tegas File (tdl)");
	/** Describes text files. */			public static final FileType TEXT         = new FileType("Text", new String[] {"txt"}, "Text File (txt)");
	/** Describes Verilog decks. */			public static final FileType VERILOG      = new FileType("Verilog", new String[] {"v"}, "Verilog Deck (v)");
	/** Describes Verilog output. */		public static final FileType VERILOGOUT   = new FileType("VerilogOutput", new String[] {"dump"}, "Verilog VCD Dump (vcd)");
	/** Describes Xml files. */				public static final FileType XML          = new FileType("XML", new String[] {"xml"}, "XML File (xml)");

	/** Describes default file format.*/	public static final FileType DEFAULTLIB   = JELIB;

	/** Valid Library formats */            public static final FileType libraryTypes[] = {JELIB, ELIB};
	private static String [] libraryTypesExt;
	private static String libraryTypesExtReadable;
	static {
		ArrayList exts = new ArrayList();
		for (int i=0; i<libraryTypes.length; i++) {
			FileType type = libraryTypes[i];
			String [] typeExts = type.getExtensions();
			for (int j=0; j<typeExts.length; j++) exts.add(typeExts[j]);
		}
		libraryTypesExt = new String[exts.size()];
		StringBuffer buf = new StringBuffer("(");
		for (int i=0; i<exts.size(); i++) {
			libraryTypesExt[i] = (String)exts.get(i);
			buf.append((String)exts.get(i));
			buf.append(", ");
		}
		if (buf.length() > 2) buf.replace(buf.length()-2, buf.length(), ")");
		libraryTypesExtReadable = buf.toString();
	}

	/** Valid library formats as a Type */  public static final FileType LIBRARYFORMATS = new FileType("LibraryFormtas", libraryTypesExt, "Library Formats "+libraryTypesExtReadable);

	private final String name;
	private final String [] extensions;
	private final String desc;
	private FileFilterSwing ffs;
	private FileFilterAWT ffa;

	private FileType(String name, String [] extensions, String desc)
	{
		this.name = name;
		this.extensions = extensions;
		this.desc = desc;
		this.ffs = null;
		this.ffa = null;
		allTypes.add(this);
	}

	public String getName() { return name; }
	public String getDescription() { return desc; }
	public String [] getExtensions() { return extensions; }

	public FileFilterSwing getFileFilterSwing()
	{
		if (ffs == null) ffs = new FileFilterSwing(extensions, desc);
		return ffs;
	}

	public FileFilterAWT getFileFilterAWT()
	{
		if (ffa == null) ffa = new FileFilterAWT(extensions, desc);
		return ffa;
	}

	/**
	 * Returns a printable version of this Type.
	 * @return a printable version of this Type.
	 */
	public String toString() { return name; }

	/**
	 * Get the Type for the specified filter
	 */
	public static FileType getType(FileFilter filter) {
		for (Iterator it = allTypes.iterator(); it.hasNext(); ) {
			FileType type = (FileType)it.next();
			if (type.ffs == filter) return type;
		}
		return null;
	}

	/**
	 * Get the Type for the specified filter
	 */
	public static FileType getType(FilenameFilter filter) {
		for (Iterator it = allTypes.iterator(); it.hasNext(); ) {
			FileType type = (FileType)it.next();
			if (type.ffa == filter) return type;
		}
		return null;
	}

	private static class FileFilterSwing extends FileFilter
	{
		/** list of valid extensions */		private String[] extensions;
		/** description of filter */		private String desc;

		/** Creates a new instance of FileFilterSwing */
		public FileFilterSwing(String[] extensions, String desc)
		{
			this.extensions = extensions;
			this.desc = desc;
		}

		/** Returns true if file has extension matching any of
		 * extensions is @param extensions.  Also returns true
		 * if file is a directory.  Returns false otherwise
		 */
		public boolean accept(File f)
		{
			if (f == null) return false;
			if (f.isDirectory()) return true;
			if (extensions.length == 0) return true;	// special case for ANY
			String filename = f.getName();
			int i = filename.lastIndexOf('.');
			if (i < 0) return false;
			String thisExtension = filename.substring(i+1);
			if (thisExtension == null) return false;
			for (int j=0; j<extensions.length; j++)
			{
				String extension = extensions[j];
				if (extension.equalsIgnoreCase(thisExtension)) return true;
			}
			return false;
		}

		public String getDescription() { return desc; }
	}


	private static class FileFilterAWT implements FilenameFilter
	{
		/** list of valid extensions */		private String[] extensions;
		/** description of filter */		private String desc;

		/** Creates a new instance of FileFilterAWT */
		public FileFilterAWT(String[] extensions, String desc)
		{
			this.extensions = extensions;
			this.desc = desc;
		}

		/** Returns true if file has extension matching any of
		 * extensions is @param extensions.  Also returns true
		 * if file is a directory.  Returns false otherwise
		 */
		public boolean accept(File f, String filename)
		{
			if (extensions.length == 0) return true;	// special case for ANY
			int i = filename.lastIndexOf('.');
			if (i < 0) return false;
			String thisExtension = filename.substring(i+1);
			if (thisExtension == null) return false;
			for (int j=0; j<extensions.length; j++)
			{
				String extension = extensions[j];
				if (extension.equalsIgnoreCase(thisExtension)) return true;
			}
			return false;
		}

		public String getDescription() { return desc; }
	}
}
