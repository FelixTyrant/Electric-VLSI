/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TopLevel.java
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
package com.sun.electric.tool.user.ui;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.tool.user.UserMenuCommands;
import com.sun.electric.tool.user.ui.PaletteFrame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Cursor;

import javax.swing.JFrame;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.ImageIcon;


/**
 * @author wc147374
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TopLevel extends JFrame
{
	/**
	 * OS is a typesafe enum class that describes the current operating system.
	 */
	public static class OS
	{
		private final String name;

		private OS(String name) { this.name = name; }

		/**
		 * Returns a printable version of this OS.
		 * @return a printable version of this OS.
		 */
		public String toString() { return name; }

		/** Describes Windows. */							public static final OS WINDOWS   = new OS("unknown");
		/** Describes UNIX/Linux. */						public static final OS UNIX      = new OS("metal-1");
		/** Describes Macintosh. */							public static final OS MACINTOSH = new OS("metal-2");
	}

	/** True if in MDI mode, otherwise SDI. */				private static boolean mdi;
	/** The desktop pane (if MDI). */						private static JDesktopPane desktop;
	/** The main frame (if MDI). */							private static TopLevel topLevel;
	/** The current window frame (only 1 if MDI). */		private static TopLevel current;
	/** A list of all frames (if SDI). */					private static List windowList = new ArrayList();
	/** The EditWindow associated with this (if SDI). */	private EditWindow wnd;
	/** The size of the screen. */							private static Dimension scrnSize;
	/** The current operating system. */					private static OS os;
	/** The palette object system. */						private static PaletteFrame palette;
	/** The cursor being displayed. */						private static Cursor cursor;

	/**
	 * Constructor to build a window.
	 * @param name the title of the window.
	 */
	public TopLevel(String name, Dimension screenSize)
	{
		super(name);
		addWindowListener(new WindowsEvents(this));
		addWindowFocusListener(new WindowsEvents(this));
		setSize(screenSize);
		getContentPane().setLayout(new BorderLayout());
		setVisible(true);

		// set an icon on the window
		setIconImage(new ImageIcon(getClass().getResource("IconElectric.gif")).getImage());

		// create the menu bar
		JMenuBar menuBar = UserMenuCommands.createMenuBar();
		setJMenuBar(menuBar);

		// create the tool bar
		ToolBar toolBar = ToolBar.createToolBar();
		getContentPane().add(toolBar, BorderLayout.NORTH);

		if (!isMDIMode())
			setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		windowList.add(this);
		current = this;
		cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
	}

	/**
	 * Routine to initialize the window system.
	 */
	public static void Initialize()
	{
		// initialize the messages window
		MessagesWindow cl = new MessagesWindow(scrnSize);
		palette = PaletteFrame.newInstance();
	}

	/**
	 * Routine to initialize the window system.
	 */
	public static void OSInitialize()
	{
		// setup the size of the screen
		scrnSize = (Toolkit.getDefaultToolkit()).getScreenSize();

		// setup specific look-and-feel
		mdi = false;
		try{
			String osName = System.getProperty("os.name").toLowerCase();
			if (osName.startsWith("windows"))
			{
				os = OS.WINDOWS;
				mdi = true;
				scrnSize.height -= 30;
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			} else if (osName.startsWith("linux") || osName.startsWith("solaris") || osName.startsWith("sunos"))
			{
				os = OS.UNIX;
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.MotifLookAndFeel");
			} else if (osName.startsWith("mac"))
			{
				os = OS.MACINTOSH;
				System.setProperty("com.apple.macos.useScreenMenuBar", "true");
				System.setProperty("apple.laf.useScreenMenuBar", "true");
				System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Electric");
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.MacLookAndFeel");
			}
		} catch(Exception e) {}

		// in MDI, create the top frame now
		if (isMDIMode())
		{
			topLevel = new TopLevel("Electric", scrnSize);	

			// make the desktop
			desktop = new JDesktopPane();
			topLevel.getContentPane().add(desktop, BorderLayout.CENTER);
		}
	}

	/**
	 * Routine to tell which operating system Electric is running on.
	 * @return the operating system Electric is running on.
	 */
	public static OS getOperatingSystem() { return os; }

	/**
	 * Routine to tell whether Electric is running in SDI or MDI mode.
	 * SDI is Single Document Interface, where each document appears in its own window.
	 * This is used on UNIX/Linux and on Macintosh.
	 * MDI is Multiple Document Interface, where the main window has all documents in it as subwindows.
	 * This is used on Windows.
	 * @return true if Electric is in MDI mode.
	 */
	public static boolean isMDIMode()
	{
		return mdi;
	}

	/**
	 * Routine to return an iterator over all top-level windows.
	 * This only makes sense in SDI mode, where there are multiple top-level windows.
	 * @return an iterator over all top-level windows.
	 */
	public static Iterator getWindows() { return windowList.iterator(); }

	/**
	 * Routine to return component palette window.
	 * The component palette is the vertical toolbar on the left side.
	 * @return the component palette window.
	 */
	public static PaletteFrame getPaletteFrame() { return palette; }

	/**
	 * Routine to return the size of the screen that Electric is on.
	 * @return the size of the screen that Electric is on.
	 */
	public static Dimension getScreenSize() { return new Dimension(scrnSize); }

	/**
	 * Routine to remove a window from the list of top-level windows.
	 * This only makes sense in SDI mode, where there are multiple top-level windows.
	 * @param tl the top-level window to remove.
	 */
	public static void removeWindow(TopLevel tl) { windowList.remove(tl); }

	/**
	 * Routine to set the current top-level window.
	 * This only makes sense in SDI mode, where there are multiple top-level windows.
	 * @param tl the top-level window to make current.
	 */
	public static void setCurrentWindow(TopLevel tl) { current = tl; }

	/**
	 * Routine to add an internal frame to the desktop.
	 * This only makes sense in MDI mode, where the desktop has multiple subframes.
	 * @param jif the internal frame to add.
	 */
	public static void addToDesktop(JInternalFrame jif) { desktop.add(jif); }

	/**
	 * Routine to return the current EditWindow.
	 * @return the current EditWindow.
	 */
	public static EditWindow getCurrentEditWindow()
	{
		if (isMDIMode())
        {
        	JInternalFrame frame = desktop.getSelectedFrame();
			for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = (WindowFrame)it.next();
				if (wf.getInternalFrame() == frame) return wf.getEditWindow();
			}
			return null;
        } else
        {
        	return current.getEditWindow();
        }
	}

	public static Cursor getCurrentCursor() { return cursor; }

	public static void setCurrentCursor(Cursor cursor)
	{
		TopLevel.cursor = cursor;
		JFrame jf = TopLevel.getCurrentJFrame();
		jf.setCursor(cursor);
		palette.getPanel().setCursor(cursor);
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			EditWindow wnd = wf.getEditWindow();
			wnd.setCursor(cursor);
		}
	}

	/**
	 * Routine to return the current EditWindow.
	 * @return the current EditWindow.
	 */
	public static JFrame getCurrentJFrame()
	{
		if (isMDIMode())
        {
			return topLevel;
 		} else
        {
        	return current;
        }
	}

	/**
	 * Routine to return the EditWindow associated with this top-level window.
	 * @return the EditWindow associated with this top-level window.
	 */
	public EditWindow getEditWindow() { return wnd; }

	/**
	 * Routine to set the edit window associated with this top-level window.
	 * @param wnd the EditWindow to associatd with this.
	 */
	public void setEditWindow(EditWindow wnd) { this.wnd = wnd; }
}


class WindowsEvents extends WindowAdapter
{
	TopLevel window;
	
	WindowsEvents(TopLevel tl)
	{
		super();
		this.window = tl;	
	}

	public void windowGainedFocus(WindowEvent e)
	{
		if (TopLevel.isMDIMode()) return;
		TopLevel.setCurrentWindow(window);
	}
	
	public void windowClosing(WindowEvent e)
	{
		if (TopLevel.isMDIMode())
		{
			UserMenuCommands.quitCommand();
		}
		for(Iterator it = TopLevel.getWindows(); it.hasNext(); )
		{
			TopLevel tl = (TopLevel)it.next();
			if (tl != window)
			{
				TopLevel.removeWindow(window);
				window.dispose();
				TopLevel.setCurrentWindow(tl);
				return;
			}
		}
		System.out.println("Cannot delete the last window");
	}		
}