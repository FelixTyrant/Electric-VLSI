/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FileMenu.java
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

package com.sun.electric.tool.user.menus;

import com.sun.electric.Main;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.io.output.PNG;
import com.sun.electric.tool.io.output.PostScript;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.dialogs.PreferencesFrame;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.ElectricPrinter;
import com.sun.electric.tool.user.ui.TextWindow;
import com.sun.electric.tool.user.ui.ToolBar;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WaveformWindow;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

/**
 * Class to handle the commands in the "File" pulldown menu.
 */
public class FileMenu {


    protected static void addFileMenu(MenuBar menuBar) {
        MenuBar.MenuItem m;
		int buckyBit = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		/****************************** THE FILE MENU ******************************/

		MenuBar.Menu fileMenu = new MenuBar.Menu("File", 'F');
        menuBar.add(fileMenu);

		fileMenu.addMenuItem("New Library...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { newLibraryCommand(); } });
		fileMenu.addMenuItem("Open Library...", KeyStroke.getKeyStroke('O', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { openLibraryCommand(); } });

		MenuBar.Menu importSubMenu = new MenuBar.Menu("Import");
		fileMenu.add(importSubMenu);
		importSubMenu.addMenuItem("CIF (Caltech Intermediate Format)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(FileType.CIF); } });
		importSubMenu.addMenuItem("GDS II (Stream)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(FileType.GDS); } });
		importSubMenu.addMenuItem("EDIF (Electronic Design Interchange Format)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(FileType.EDIF); } });
		importSubMenu.addMenuItem("LEF (Library Exchange Format)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(FileType.LEF); } });
		importSubMenu.addMenuItem("DEF (Design Exchange Format)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(FileType.DEF); } });
		importSubMenu.addMenuItem("DXF (AutoCAD)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(FileType.DXF); } });
		importSubMenu.addMenuItem("SUE (Schematic User Environment)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(FileType.SUE); } });
		importSubMenu.addSeparator();
		importSubMenu.addMenuItem("ELIB...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(FileType.ELIB); } });
		importSubMenu.addMenuItem("Readable Dump...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(FileType.READABLEDUMP); } });
		importSubMenu.addMenuItem("Text Cell Contents...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { TextWindow.readTextCell(); }});

		fileMenu.addSeparator();

		fileMenu.addMenuItem("Close Library", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { closeLibraryCommand(Library.getCurrent()); } });
		fileMenu.addMenuItem("Save Library", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveLibraryCommand(Library.getCurrent(), FileType.DEFAULTLIB, false, true); } });
		fileMenu.addMenuItem("Save Library as...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveAsLibraryCommand(Library.getCurrent()); } });
		fileMenu.addMenuItem("Save All Libraries", KeyStroke.getKeyStroke('S', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveAllLibrariesCommand(); } });
        fileMenu.addMenuItem("Save All Libraries in Format...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { saveAllLibrariesInFormatCommand(); } });

		MenuBar.Menu exportSubMenu = new MenuBar.Menu("Export");
		fileMenu.add(exportSubMenu);
		exportSubMenu.addMenuItem("CIF (Caltech Intermediate Format)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.CIF, false); } });
		exportSubMenu.addMenuItem("GDS II (Stream)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.GDS, false); } });
		exportSubMenu.addMenuItem("EDIF (Electronic Design Interchange Format)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.EDIF, false); } });
		exportSubMenu.addMenuItem("LEF (Library Exchange Format)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.LEF, false); } });
		exportSubMenu.addMenuItem("L...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.L, false); } });
		if (IOTool.hasSkill())
			exportSubMenu.addMenuItem("Skill (Cadence Commands)...", null,
				new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.SKILL, false); } });
		exportSubMenu.addSeparator();
		exportSubMenu.addMenuItem("Eagle...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.EAGLE, false); } });
		exportSubMenu.addMenuItem("ECAD...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.ECAD, false); } });
		exportSubMenu.addMenuItem("Pads...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.PADS, false); } });
		exportSubMenu.addSeparator();
		exportSubMenu.addMenuItem("Text Cell Contents...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { TextWindow.writeTextCell(); }});
		exportSubMenu.addMenuItem("PostScript...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.POSTSCRIPT, false); } });
	    exportSubMenu.addMenuItem("PNG (Portable Network Graphics)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.PNG, false); } });
		exportSubMenu.addMenuItem("DXF (AutoCAD)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.DXF, false); } });
		exportSubMenu.addSeparator();
		exportSubMenu.addMenuItem("ELIB (Version 6)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveLibraryCommand(Library.getCurrent(), FileType.ELIB, true, false); } });

		fileMenu.addSeparator();

		fileMenu.addMenuItem("Change Current Library...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.changeCurrentLibraryCommand(); } });
		fileMenu.addMenuItem("List Libraries", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.listLibrariesCommand(); } });
		fileMenu.addMenuItem("Rename Library...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.renameLibrary(Library.getCurrent()); } });
		fileMenu.addMenuItem("Mark All Libraries for Saving", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.markAllLibrariesForSavingCommand(); } });
		MenuBar.Menu checkSubMenu = new MenuBar.Menu("Check Libraries");
		fileMenu.add(checkSubMenu);
		checkSubMenu.addMenuItem("Check", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.checkAndRepairCommand(false); } });
		checkSubMenu.addMenuItem("Repair", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.checkAndRepairCommand(true); } });

        fileMenu.addSeparator();

        fileMenu.addMenuItem("Page Setup...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { pageSetupCommand(); } });
		fileMenu.addMenuItem("Print...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { printCommand(); } });

		fileMenu.addSeparator();
		fileMenu.addMenuItem("Preferences...",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { PreferencesFrame.preferencesCommand(); } });

		if (TopLevel.getOperatingSystem() != TopLevel.OS.MACINTOSH)
		{
			fileMenu.addMenuItem("Quit", KeyStroke.getKeyStroke('Q', buckyBit),
				new ActionListener() { public void actionPerformed(ActionEvent e) { quitCommand(); } });
		}
        fileMenu.addMenuItem("Force Quit (and Save)", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { forceQuit(); } });

    }

    // ------------------------------ File Menu -----------------------------------

    public static void newLibraryCommand()
    {
        String newLibName = JOptionPane.showInputDialog("New Library Name", "");
        if (newLibName == null) return;
        NewLibrary job = new NewLibrary(newLibName);
    }

    public static class NewLibrary extends Job {
        private String newLibName;

        public NewLibrary(String newLibName) {
            super("New Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.newLibName = newLibName;
            startJob();
        }

        public boolean doIt()
        {
            Library lib = Library.newInstance(newLibName, null);
            if (lib == null) return false;
            lib.setCurrent();
            EditWindow.repaintAll();
            TopLevel.getCurrentJFrame().getToolBar().setEnabled(ToolBar.SaveLibraryName, Library.getCurrent() != null);
            System.out.println("New Library '"+lib.getName()+"' created");
            return true;
        }
    }

    /**
     * This method implements the command to read a library.
     * It is interactive, and pops up a dialog box.
     */
    public static void openLibraryCommand()
    {
        String fileName = OpenFile.chooseInputFile(FileType.LIBRARYFORMATS, null);
        //String fileName = OpenFile.chooseInputFile(OpenFile.Type.DEFAULTLIB, null);
        if (fileName != null)
        {
            // start a job to do the input
            URL fileURL = TextUtils.makeURLToFile(fileName);
			String libName = TextUtils.getFileNameWithoutExtension(fileURL);
			Library deleteThis = null;
			Library deleteLib = Library.findLibrary(libName);
			if (deleteLib != null)
			{
				// library already exists, prompt for save
				if (FileMenu.preventLoss(deleteLib, 2)) return;
				WindowFrame.removeLibraryReferences(deleteLib);
			}
			FileType type = getLibraryFormat(fileName, FileType.DEFAULTLIB);
			ReadLibrary job = new ReadLibrary(fileURL, type, deleteLib);
        }
    }

    /** Get the type from the fileName, or if no valid Library type found, return defaultType.
     */
    public static FileType getLibraryFormat(String fileName, FileType defaultType) {
        for (int i=0; i<FileType.libraryTypes.length; i++) {
            FileType type = FileType.libraryTypes[i];
            if (fileName.endsWith("."+type.getExtensions()[0])) return type;
        }
        return defaultType;
    }

	/**
	 * Class to read a text library in a new thread.
	 * For a non-interactive script, use ReadLibrary job = new ReadLibrary(filename, format).
	 */
	public static class ReadLibrary extends Job
	{
		private URL fileURL;
		private FileType type;
		private Library deleteLib;

		public ReadLibrary(URL fileURL, FileType type, Library deleteLib)
		{
			super("Read External Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.fileURL = fileURL;
			this.type = type;
			this.deleteLib = deleteLib;
			startJob();
		}

		public boolean doIt()
		{
			// see if the former library can be deleted
			if (deleteLib != null)
			{
				if (Library.getVisibleLibraries().size() > 1)
				{
					if (!deleteLib.kill()) return false;
					deleteLib = null;
				} else
				{
					// cannot delete last library: must delete it later
					// mangle the name so that the new one can be created
					deleteLib.setName("FORMERVERSIONOF" + deleteLib.getName());
				}
			}
			openALibrary(fileURL, type);
			if (deleteLib != null)
				deleteLib.kill();
			return true;
		}
	}

    public static class ReadInitialELIBs extends Job
    {
        java.util.List fileURLs;

        public ReadInitialELIBs(java.util.List fileURLs) {
            super("Read Initial Libraries", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.fileURLs = fileURLs;
            startJob();
        }

        public boolean doIt() {
            // open no name library first
            Library mainLib = Library.newInstance("noname", null);
            if (mainLib == null) return false;
            mainLib.setCurrent();

            // try to open initial libraries
            boolean success = false;
            for (Iterator it = fileURLs.iterator(); it.hasNext(); ) {
                URL file = (URL)it.next();
                FileType defType = FileType.DEFAULTLIB;
                String fileName = file.getFile();
                defType = getLibraryFormat(fileName, defType);
                if (openALibrary(file, defType)) success = true;
            }
            if (success) {
                // close no name library
                //mainLib.kill();
                // the calls to repaint actually cause the
                // EditWindow to come up BLANK in Linux SDI mode
                //EditWindow.repaintAll();
                //EditWindow.repaintAllContents();
            }
            return true;
        }
    }

    /** Opens a library */
    private static boolean openALibrary(URL fileURL, FileType type)
    {
    	Library lib = null;
    	if (type == FileType.ELIB || type == FileType.JELIB || type == FileType.READABLEDUMP)
        {
    		lib = Input.readLibrary(fileURL, type);
        } else
        {
    		lib = Input.importLibrary(fileURL, type);
        }
        if (lib != null)
        {
            // new library open: check for default "noname" library and close if empty
            Library noname = Library.findLibrary("noname");
            if (noname != null)
            {
                if (!noname.getCells().hasNext())
                {
                    noname.kill();
                }
            }
        }
        Undo.noUndoAllowed();
        if (lib == null) return false;
        lib.setCurrent();
        Cell cell = lib.getCurCell();
        if (cell == null) System.out.println("No current cell in this library");
        else if (!Main.BATCHMODE)
        {
            CreateCellWindow creator = new CreateCellWindow(cell);
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeLater(creator);
            } else {
                creator.run();
            }
        }
        return true;
    }

    public static class CreateCellWindow implements Runnable {
        private Cell cell;
        public CreateCellWindow(Cell cell) { this.cell = cell; }
        public void run() {
            // check if edit window open with null cell, use that one if exists
            for (Iterator it = WindowFrame.getWindows(); it.hasNext(); )
            {
                WindowFrame wf = (WindowFrame)it.next();
                WindowContent content = wf.getContent();
                if (content.getCell() == null)
                {
                    wf.setCellWindow(cell);
                    //wf.requestFocus();
                    TopLevel.getCurrentJFrame().getToolBar().setEnabled(ToolBar.SaveLibraryName, Library.getCurrent() != null);
                    return;
                }
            }
            WindowFrame.createEditWindow(cell);

            // no clean for now.
            TopLevel.getCurrentJFrame().getToolBar().setEnabled(ToolBar.SaveLibraryName, Library.getCurrent() != null);
        }
    }

	/**
	 * This method implements the command to import a library (Readable Dump or JELIB format).
	 * It is interactive, and pops up a dialog box.
	 */
	public static void importLibraryCommand(FileType type)
	{
		String fileName = OpenFile.chooseInputFile(type, null);
		if (fileName != null)
		{
			// start a job to do the input
			URL fileURL = TextUtils.makeURLToFile(fileName);
			String libName = TextUtils.getFileNameWithoutExtension(fileURL);
			Library deleteThis = null;
			Library deleteLib = Library.findLibrary(libName);
			if (deleteLib != null)
			{
				// library already exists, prompt for save
				if (FileMenu.preventLoss(deleteLib, 2)) return;
				WindowFrame.removeLibraryReferences(deleteLib);
			}
			ReadLibrary job = new ReadLibrary(fileURL, type, deleteLib);
		}
	}

    public static void closeLibraryCommand(Library lib)
    {
	    Set found = Library.findReferenceInCell(lib);
	    // You can't close it because there are open cells that refer to library elements
	    if (found.size() != 0)
	    {
		    System.out.println("Cannot close library '" + lib.getName() + "':");
		    System.out.print("\t Cells ");

		    for (Iterator i = found.iterator(); i.hasNext();)
		    {
			   Cell cell = (Cell)i.next();
			   System.out.print("'" + cell.getName() + "'(" + cell.getLibrary().getName() + ") ");
		    }
		    System.out.println("refer to it.");
		    return;
	    }
	    if (preventLoss(lib, 1)) return;

        String libName = lib.getName();
        WindowFrame.removeLibraryReferences(lib);
        CloseLibrary job = new CloseLibrary(lib);
    }

    public static class CloseLibrary extends Job {
        Library lib;

        public CloseLibrary(Library lib) {
            super("Close Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.lib = lib;
            startJob();
        }

        public boolean doIt() {
            if (lib.kill())
            {
                System.out.println("Library " + lib.getName() + " closed");
	            WindowFrame.wantToRedoTitleNames();
	            EditWindow.repaintAll();

	            // Disable save icon if no more libraries are open
	            TopLevel.getCurrentJFrame().getToolBar().setEnabled(ToolBar.SaveLibraryName, Library.getCurrent() != null);
            }
            return true;
        }
    }

    /**
     * This method implements the command to save a library.
     * It is interactive, and pops up a dialog box.
     * @param lib the Library to save.
     * @param type the format of the library (OpenFile.Type.ELIB, OpenFile.Type.READABLEDUMP, or OpenFile.Type.JELIB).
     * @param compatibleWith6 true to write a library that is compatible with version 6 Electric.
     * @return true if library saved, false otherwise.
     */
    public static boolean saveLibraryCommand(Library lib, FileType type, boolean compatibleWith6, boolean forceToType)
    {
        String [] extensions = type.getExtensions();
        String extension = extensions[0];
        String fileName = null;
        if (lib.isFromDisk())
        {
        	if (type == FileType.JELIB ||
        		(type == FileType.ELIB && !compatibleWith6))
	        {
	            fileName = lib.getLibFile().getPath();
	            if (forceToType)
	            {
		            type = OpenFile.getOpenFileType(fileName, FileType.DEFAULTLIB);
		            if (Main.getDebug())
		            {
			            // old code Gilda Nov 9
						FileType oldtype = type;
						if (fileName.endsWith(".elib")) oldtype = FileType.ELIB; else
						if (fileName.endsWith(".jelib")) oldtype = FileType.JELIB; else
						if (fileName.endsWith(".txt")) oldtype = FileType.READABLEDUMP;
			            if (type != oldtype)
				            System.out.println("Wrong type determined by OpenFile.getOpenFileType");
		            }
	            }
	        }
            // check to see that file is writable
            if (fileName != null) {
                File file = new File(fileName);
                if (file.exists() && !file.canWrite()) fileName = null;
/*
                try {
                    if (!file.createNewFile()) fileName = null;
                } catch (java.io.IOException e) {
                    System.out.println(e.getMessage());
                    fileName = null;
                }
*/
            }
        }
        if (fileName == null)
        {
            fileName = OpenFile.chooseOutputFile(FileType.libraryTypes, null, lib.getName() + "." + extension);
            if (fileName == null) return false;
            type = getLibraryFormat(fileName, type);
            // mark for saving, all libraries that depend on this
            for(Iterator it = Library.getLibraries(); it.hasNext(); )
            {
                Library oLib = (Library)it.next();
                if (oLib.isHidden()) continue;
                if (oLib == lib) continue;
                if (oLib.isChangedMajor()) continue;

                // see if any cells in this library reference the renamed one
                if (oLib.referencesLib(lib))
                    oLib.setChangedMajor();
            }
        }
        SaveLibrary job = new SaveLibrary(lib, fileName, type, compatibleWith6);
        return true;
    }

    /**
     * Class to save a library in a new thread.
     * For a non-interactive script, use SaveLibrary job = new SaveLibrary(filename).
     * Saves as an elib.
     */
    public static class SaveLibrary extends Job
    {
        Library lib;
        String newName;
        FileType type;
        boolean compatibleWith6;

        public SaveLibrary(Library lib, String newName, FileType type, boolean compatibleWith6)
        {
            super("Write Library "+lib.getName(), User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.lib = lib;
            this.newName = newName;
            this.type = type;
            this.compatibleWith6 = compatibleWith6;
            startJob();
        }

        public boolean doIt()
        {
            boolean retVal = false;
            try {
                retVal = _doIt();
                if (!retVal) {
                    JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), new String [] {"Error saving files",
                         "Please check your disk libraries"}, "Saving Failed", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), new String [] {"Exception caught when saving files",
                     e.getMessage(),
                     "Please check your disk libraries"}, "Saving Failed", JOptionPane.ERROR_MESSAGE);
                ActivityLogger.logException(e);
            }
            return retVal;
        }

        private boolean _doIt() {
            // rename the library if requested
            if (newName != null)
            {
                URL libURL = TextUtils.makeURLToFile(newName);
                lib.setLibFile(libURL);
                lib.setName(TextUtils.getFileNameWithoutExtension(libURL));
            }

            boolean error = Output.writeLibrary(lib, type, compatibleWith6);
            if (error) return false;
            return true;
        }
    }

    /**
     * This method implements the command to save a library to a different file.
     * It is interactive, and pops up a dialog box.
     */
    public static void saveAsLibraryCommand(Library lib)
    {
        lib.clearFromDisk();
        saveLibraryCommand(lib, FileType.DEFAULTLIB, false, true);
        WindowFrame.wantToRedoTitleNames();
    }

    /**
     * This method implements the command to save all libraries.
     */
    public static void saveAllLibrariesCommand()
    {
        saveAllLibrariesCommand(FileType.DEFAULTLIB, false, true);
    }

    public static void saveAllLibrariesCommand(FileType type, boolean compatibleWith6, boolean forceToType)
    {
        for(Iterator it = Library.getLibraries(); it.hasNext(); )
        {
            Library lib = (Library)it.next();
            if (lib.isHidden()) continue;
            if (!lib.isChangedMajor() && !lib.isChangedMinor()) continue;
            if (lib.getLibFile() != null)
                type = getLibraryFormat(lib.getLibFile().getFile(), type);
            if (!saveLibraryCommand(lib, type, compatibleWith6, forceToType)) break;
        }
    }

    public static void saveAllLibrariesInFormatCommand() {
        Object[] formats = {FileType.ELIB, FileType.JELIB, FileType.READABLEDUMP};
        Object format = JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(),
                "Output file format for all libraries:", "Save All Libraries In Format...",
                JOptionPane.PLAIN_MESSAGE,
                null, formats, FileType.DEFAULTLIB);
        FileType outType = (FileType)format;
        for (Iterator it = Library.getLibraries(); it.hasNext(); ) {
            Library lib = (Library)it.next();
            if (lib.isHidden()) continue;
            if (!lib.isFromDisk()) continue;
            if (lib.getLibFile() != null) {
                // set library file to new format
                String fullName = lib.getLibFile().getFile();
                //if (fullName.endsWith("spiceparts.txt")) continue; // ignore spiceparts library
                // match ".<word><endline>"
                fullName = fullName.replaceAll("\\.\\w*?$", "."+outType.getExtensions()[0]);
                lib.setLibFile(TextUtils.makeURLToFile(fullName));
            }
            lib.setChangedMajor();
            lib.setChangedMinor();
        }
        saveAllLibrariesCommand(outType, false, false);
    }

    /**
     * This method implements the export cell command for different export types.
     * It is interactive, and pops up a dialog box.
     */
    public static void exportCommand(FileType type, boolean isNetlist)
    {
        if (type == FileType.POSTSCRIPT)
        {
            if (PostScript.syncAll()) return;
            if (IOTool.isPrintEncapsulated()) type = FileType.EPS;
        }
	    WindowFrame wf = WindowFrame.getCurrentWindowFrame(false);
	    WindowContent wnd = (wf != null) ? wf.getContent() : null;

	    if (wnd == null)
        {
            System.out.println("No current window");
            return;
        }
        Cell cell = wnd.getCell();
        if (cell == null)
        {
            System.out.println("No cell in this window");
        }
        VarContext context = (wnd instanceof EditWindow) ? ((EditWindow)wnd).getVarContext() : null;

        String [] extensions = type.getExtensions();
        String filePath = ((cell != null) ? cell.getName() : "") + "." + extensions[0];

        // special case for spice
        if (type == FileType.SPICE &&
			!Simulation.getSpiceRunChoice().equals(Simulation.spiceRunChoiceDontRun))
        {
            // check if user specified working dir
            if (Simulation.getSpiceUseRunDir())
                filePath = Simulation.getSpiceRunDir() + File.separator + filePath;
            else
                filePath = User.getWorkingDirectory() + File.separator + filePath;
            // check for automatic overwrite
            if (User.isShowFileSelectionForNetlists() && !Simulation.getSpiceOutputOverwrite()) {
                String saveDir = User.getWorkingDirectory();
                filePath = OpenFile.chooseOutputFile(type, null, filePath);
                User.setWorkingDirectory(saveDir);
                if (filePath == null) return;
            }

            exportCellCommand(cell, context, filePath, type);
            return;
        }

        if (User.isShowFileSelectionForNetlists() || !isNetlist)
        {
            filePath = OpenFile.chooseOutputFile(type, null, filePath);
            if (filePath == null) return;
        } else
        {
            filePath = User.getWorkingDirectory() + File.separator + filePath;
        }

	    // Special case for PNG format
	    if (type == FileType.PNG)
	    {
		    String name = (cell != null) ? cell.describe() : filePath;
		    ExportImage job = new ExportImage(name, wnd, filePath);
			return;
	    }

        exportCellCommand(cell, context, filePath, type);
    }

    /**
     * This is the non-interactive version of exportCellCommand
     */
    public static void exportCellCommand(Cell cell, VarContext context, String filePath, FileType type)
    {
        ExportCell job = new ExportCell(cell, context, filePath, type);
    }

    /**
     * Class to export a cell in a new thread.
     * For a non-interactive script, use
     * ExportCell job = new ExportCell(Cell cell, String filename, Output.ExportType type).
     * Saves as an elib.
     */
    private static class ExportCell extends Job
    {
        Cell cell;
        VarContext context;
        String filePath;
        FileType type;

        public ExportCell(Cell cell, VarContext context, String filePath, FileType type)
        {
            super("Export "+cell.describe()+" ("+type+")", User.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
            this.cell = cell;
            this.context = context;
            this.filePath = filePath;
            this.type = type;
            startJob();
        }

        public boolean doIt()
        {
            Output.writeCell(cell, context, filePath, type);
            return true;
        }

    }

	private static class ExportImage extends Job
	{
		String filePath;
		WindowContent wnd;

		public ExportImage(String description, WindowContent wnd, String filePath)
		{
			super("Export "+description+" ("+FileType.PNG+")", User.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
			this.wnd = wnd;
			this.filePath = filePath;
			startJob();
		}
		public boolean doIt()
        {
			PrinterJob pj = PrinterJob.getPrinterJob();
	        ElectricPrinter ep = getOutputPreferences(wnd, pj);
            BufferedImage img = wnd.getOffScreenImage(ep);
			PNG.writeImage(img, filePath);
            return true;
        }
	}

    private static PageFormat pageFormat = null;

    public static void pageSetupCommand() {
        PrinterJob pj = PrinterJob.getPrinterJob();
        if (pageFormat == null)
            pageFormat = pj.pageDialog(pj.defaultPage());
        else
            pageFormat = pj.pageDialog(pageFormat);
    }

	private static ElectricPrinter getOutputPreferences(WindowContent context, PrinterJob pj)
	{
 		if (pageFormat == null)
		 {
			pageFormat = pj.defaultPage();
			pageFormat.setOrientation(PageFormat.LANDSCAPE);
            pageFormat = pj.validatePage(pageFormat);
		 }

 		ElectricPrinter ep = new ElectricPrinter(context, pageFormat);
		pj.setPrintable(ep, pageFormat);
		return (ep);
	}

    /**
     * This method implements the command to print the current window.
     */
    public static void printCommand()
    {
    	WindowFrame wf = WindowFrame.getCurrentWindowFrame();
    	if (wf == null)
    	{
    		System.out.println("No current window to print");
    		return;
    	}

        PrinterJob pj = PrinterJob.getPrinterJob();
        pj.setJobName(wf.getTitle());
	    ElectricPrinter ep = getOutputPreferences(wf.getContent(), pj);
// 		if (pageFormat == null)
//		 {
//			pageFormat = pj.defaultPage();
//			pageFormat.setOrientation(PageFormat.LANDSCAPE);
//            pageFormat = pj.validatePage(pageFormat);
//		 }
//
// 		ElectricPrinter ep = new ElectricPrinter(wf.getContent());
//        pj.setPrintable(ep, pageFormat);

        // see if a default printer should be mentioned
        String pName = IOTool.getPrinterName();
        PrintService [] printers = PrintServiceLookup.lookupPrintServices(null, null);
        PrintService printerToUse = null;
        for(int i=0; i<printers.length; i++)
        {
            if (pName.equals(printers[i].getName()))
            {
                printerToUse = printers[i];
                break;
            }
        }
        if (printerToUse != null)
        {
            try
            {
                pj.setPrintService(printerToUse);
            } catch (PrinterException e)
            {
                System.out.println("Printing error "+e);
            }
        }

        if (pj.printDialog())
        {
			// disable double-buffering so prints look better
			JPanel overall = wf.getContent().getPanel();
			RepaintManager currentManager = RepaintManager.currentManager(overall);
			currentManager.setDoubleBufferingEnabled(false);

			// resize the window if this is a WaveformWindow
			Dimension oldSize = null;
			if (wf.getContent() instanceof WaveformWindow)
			{
				int iw = (int)pageFormat.getImageableWidth();
				int ih = (int)pageFormat.getImageableHeight();
				oldSize = overall.getSize();
				overall.setSize(iw, ih);
				overall.validate();
				overall.repaint();
			}

            printerToUse = pj.getPrintService();
            if (printerToUse != null)
 				IOTool.setPrinterName(printerToUse.getName());
			SwingUtilities.invokeLater(new PrintJobAWT(wf, pj, oldSize));
        }
    }

	private static class PrintJobAWT implements Runnable
	{
		private WindowFrame wf;
		private PrinterJob pj;
		private Dimension oldSize;

		PrintJobAWT(WindowFrame wf, PrinterJob pj, Dimension oldSize)
		{
			this.wf = wf;
			this.pj = pj;
			this.oldSize = oldSize;
		}

		public void run()
		{
			try {
				pj.print();
			} catch (PrinterException pe)
			{
				System.out.println("Print aborted.");
			}

			JPanel overall = wf.getContent().getPanel();
			RepaintManager currentManager = RepaintManager.currentManager(overall);
			currentManager.setDoubleBufferingEnabled(true);

			if (oldSize != null)
			{
				overall.setSize(oldSize);
				overall.validate();
			}
		}
	}

    /**
     * This method implements the command to quit Electric.
     */
    public static boolean quitCommand()
    {
        if (preventLoss(null, 0)) return false;

	    try {
            QuitJob job = new QuitJob();
	    } catch (java.lang.NoClassDefFoundError e)
	    {
		    // Ignoring this one
		    return true;
	    } catch (Exception e)
	    {
		    // Don't quit in this case.
		    return false;
	    }
        return true;
    }

    /**
     * Class to quit Electric in a new thread.
     */
    public static class QuitJob extends Job
    {
        public QuitJob()
        {
            super("Quitting", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            startJob();
        }

        public boolean doIt()
        {
            ActivityLogger.finished();
            System.exit(0);
            return true;
        }
    }

    /**
     * Method to ensure that one or more libraries are saved.
     * @param desiredLib the library to check for being saved.
     * If desiredLib is null, all libraries are checked.
     * @param action the type of action that will occur:
     * 0: quit;
     * 1: close a library;
     * 2: replace a library.
     * @return true if the operation should be aborted;
     * false to continue with the quit/close/replace.
     */
    public static boolean preventLoss(Library desiredLib, int action)
    {
        boolean saveCancelled = false;
        for(Iterator it = Library.getLibraries(); it.hasNext(); )
        {
            Library lib = (Library)it.next();
            if (desiredLib != null && desiredLib != lib) continue;
            if (lib.isHidden()) continue;
            if (!lib.isChangedMajor() && !lib.isChangedMinor()) continue;

            // warn about this library
            String how = "significantly";
            if (!lib.isChangedMajor()) how = "insignificantly";

            String theAction = "Save before quitting?";
            if (action == 1) theAction = "Save before closing?"; else
                if (action == 2) theAction = "Save before replacing?";
            String [] options = {"Yes", "No", "Cancel", "No to All"};
            int ret = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
                "Library " + lib.getName() + " has changed " + how + ".  " + theAction,
                "Save Library?", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);
            if (ret == 0)
            {
                // save the library
                if (!saveLibraryCommand(lib, FileType.DEFAULTLIB, false, true))
                    saveCancelled = true;
                continue;
            }
            if (ret == 1) continue;
            if (ret == 2) return true;
            if (ret == 3) break;
        }
        if (saveCancelled) return true;
        return false;
    }

    /**
     * Unsafe way to force Electric to quit.  If this method returns,
     * it obviously did not kill electric (because of user input or some other reason).
     */
    public static void forceQuit() {
        // check if libraries need to be saved
        boolean dirty = false;
        for(Iterator it = Library.getLibraries(); it.hasNext(); )
        {
            Library lib = (Library)it.next();
            if (lib.isHidden()) continue;
            if (!lib.isChangedMajor() && !lib.isChangedMinor()) continue;
            dirty = true;
            break;
        }
        if (dirty) {
            String [] options = { "Force Save and Quit", "Cancel", "Quit without Saving"};
            int i = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
                 new String [] {"Warning!  Libraries Changed!  Saving changes now may create bad libraries!"},
                    "Force Quit", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    options, options[1]);
            if (i == 0) {
                // force save
                if (!forceSave(false)) {
                    JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
                            "Error during forced library save, not quiting", "Saving Failed", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                System.exit(1);
            }
            if (i == 1) return;
            if (i == 2) System.exit(1);
        }
        int i = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(), new String [] {"Warning! You are about to kill Electric!",
            "Do you really want to force quit?"}, "Force Quit", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (i == JOptionPane.YES_OPTION) {
            System.exit(1);
        }
    }

    /**
     * Force saving of libraries. This does not run in a Job, and could generate corrupt libraries.
     * This saves all libraries to a new directory called "panic" in the current directory.
     * @param confirm true to pop up confirmation dialog, false to just try to save
     * @return true if libraries saved (if they needed saving), false otherwise
     */
    public static boolean forceSave(boolean confirm) {
        boolean dirty = false;
        for(Iterator it = Library.getLibraries(); it.hasNext(); )
        {
            Library lib = (Library)it.next();
            if (lib.isHidden()) continue;
            if (!lib.isChangedMajor() && !lib.isChangedMinor()) continue;
            dirty = true;
            break;
        }
        if (!dirty) {
            System.out.println("Libraries have not changed, not saving");
            return true;
        }
        if (confirm) {
            String [] options = { "Cancel", "Force Save"};
            int i = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
                 new String [] {"Warning! Saving changes now may create bad libraries!",
                                "Libraries will be saved to \"Panic\" directory in current directory",
                                "Do you really want to force save?"},
                 "Force Save", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                 options, options[0]);
            if (i == 0) return false;
        }
        // try to create the panic directory
        String currentDir = User.getWorkingDirectory();
        File panicDir = new File(currentDir, "panic");
        if (!panicDir.exists() && !panicDir.mkdir()) {
            JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), new String [] {"Could not create panic directory",
                 panicDir.getAbsolutePath()}, "Error creating panic directory", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        // set libraries to save to panic dir
        boolean retValue = true;
        FileType type = FileType.DEFAULTLIB;
        for(Iterator it = Library.getLibraries(); it.hasNext(); )
        {
            Library lib = (Library)it.next();
            if (lib.isHidden()) continue;
            URL libURL = lib.getLibFile();
            File newLibFile = null;
            if (libURL.getPath() == null) {
                newLibFile = new File(panicDir.getAbsolutePath(), lib.getName()+type.getExtensions()[0]);
            } else {
                File libFile = new File(libURL.getPath());
                String fileName = libFile.getName();
                if (fileName == null) fileName = lib.getName() + type.getExtensions()[0];
                newLibFile = new File(panicDir.getAbsolutePath(), fileName);
            }
            URL newLibURL = TextUtils.makeURLToFile(newLibFile.getAbsolutePath());
            lib.setLibFile(newLibURL);
            boolean error = Output.writeLibrary(lib, type, false);
            if (error) {
                System.out.println("Error saving library "+lib.getName());
                retValue = false;
            }
        }
        return retValue;
    }
}
