/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PreferencesFrame.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.dialogs;

import com.sun.electric.database.text.Pref;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.io.output.CellModelPrefs;
import com.sun.electric.tool.ncc.Pie;
import com.sun.electric.tool.routing.Routing;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.options.*;
import com.sun.electric.tool.user.help.ManualViewer;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.lang.reflect.Constructor;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * Class to handle the "PreferencesFrame" dialog.
 */
public class PreferencesFrame extends EDialog
{
	private JSplitPane splitPane;
	private JTree optionTree;
	JButton cancel;
	JButton ok;

	List<PreferencePanel> optionPanes = new ArrayList<PreferencePanel>();

	/** The name of the current tab in this dialog. */		private static String currentTabName = "General";
	/** The name of the current section in this dialog. */	private static String currentSectionName = "General ";
	private DefaultMutableTreeNode currentDMTN;

	/**
	 * This method implements the command to show the PreferencesFrame dialog.
	 */
	public static void preferencesCommand()
	{
		PreferencesFrame dialog = new PreferencesFrame(TopLevel.getCurrentJFrame());
		dialog.setVisible(true);
	}

	/** Creates new form PreferencesFrame */
	public PreferencesFrame(Frame parent)
	{
		super(parent, true);
		getContentPane().setLayout(new GridBagLayout());
		setTitle("Preferences");
		setName("");
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent evt)
			{
				closeDialog(evt);
			}
		});

		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Preferences");
		DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
		optionTree = new JTree(treeModel);
		TreeHandler handler = new TreeHandler(this);
		optionTree.addMouseListener(handler);
		optionTree.addTreeExpansionListener(handler);

		// the "General" section of the Preferences
		DefaultMutableTreeNode generalSet = new DefaultMutableTreeNode("General ");
		rootNode.add(generalSet);
		addTreeNode(new GeneralTab(parent, true), generalSet);
		addTreeNode(new SelectionTab(parent, true), generalSet);
        TopLevel top = TopLevel.getCurrentJFrame();
        if (top != null && top.getEMenuBar() != null)
    		addTreeNode(new EditKeyBindings(top.getEMenuBar(), parent, true), generalSet);
		addTreeNode(new NewNodesTab(parent, true), generalSet);
		addTreeNode(new NewArcsTab(parent, true), generalSet);
		addTreeNode(new ProjectManagementTab(parent, true), generalSet);
		addTreeNode(new CVSTab(parent, true), generalSet);
		addTreeNode(new PrintingTab(parent, true), generalSet);
        if (Job.getDebug())
        {
            // Open test tab only if plugin is available and in debug mode
            try
            {
                Class<?> testTab = Class.forName("com.sun.electric.plugins.tests.TestTab");
                Constructor tab = testTab.getDeclaredConstructor(new Class[]{Frame.class, Boolean.class});
        		addTreeNode((PreferencePanel)tab.newInstance(new Object[] {parent, new Boolean(true)}), generalSet);
            }
            catch (Exception ex) { /* do nothing */ };
        }

		// the "Display" section of the Preferences
		DefaultMutableTreeNode displaySet = new DefaultMutableTreeNode("Display ");
		rootNode.add(displaySet);
		addTreeNode(new DisplayControlTab(parent, true), displaySet);
		addTreeNode(new ComponentMenuTab(parent, true), displaySet);
		addTreeNode(new LayersTab(parent, true), displaySet);
		addTreeNode(new ToolbarTab(parent, true, this), displaySet);
		addTreeNode(new TextTab(parent, true), displaySet);
		addTreeNode(new SmartTextTab(parent, true), displaySet);
		addTreeNode(new GridAndAlignmentTab(parent, true), displaySet);
		addTreeNode(new PortsAndExportsTab(parent, true), displaySet);
		addTreeNode(new FrameTab(parent, true), displaySet);
		addTreeNode(ThreeDTab.create3DTab(parent, true), displaySet);

		// the "I/O" section of the Preferences
		DefaultMutableTreeNode ioSet = new DefaultMutableTreeNode("I/O ");
		rootNode.add(ioSet);
		addTreeNode(new CIFTab(parent, true), ioSet);
		addTreeNode(new GDSTab(parent, true), ioSet);
		addTreeNode(new EDIFTab(parent, true), ioSet);
		addTreeNode(new DEFTab(parent, true), ioSet);
		addTreeNode(new CDLTab(parent, true), ioSet);
		addTreeNode(new DXFTab(parent, true), ioSet);
		addTreeNode(new SUETab(parent, true), ioSet);
		if (IOTool.hasDais())
			addTreeNode(new DaisTab(parent, true), ioSet);
		if (IOTool.hasSkill())
			addTreeNode(new SkillTab(parent, true), ioSet);
		addTreeNode(new LibraryTab(parent, true), ioSet);

		// the "Tools" section of the Preferences
		DefaultMutableTreeNode toolSet = new DefaultMutableTreeNode("Tools ");
		rootNode.add(toolSet);
		addTreeNode(new AntennaRulesTab(parent, true), toolSet);
		addTreeNode(new CompactionTab(parent, true), toolSet);
		addTreeNode(new CoverageTab(parent, true), toolSet);
		addTreeNode(new DRCTab(parent, true), toolSet);
		addTreeNode(new FastHenryTab(parent, true), toolSet);
		addTreeNode(new NCCTab(parent, true), toolSet);
		if (Pie.hasPie())
		{
	        try
	        {
	            Class pTab = Class.forName("com.sun.electric.plugins.pie.ui.PIETab");
	            Constructor tab = pTab.getDeclaredConstructor(new Class[]{Frame.class, boolean.class});
	    		addTreeNode((PreferencePanel)tab.newInstance(new Object[] {parent, new Boolean(true)}), toolSet);
	        }
	        catch (Exception ex) { /* do nothing */ };
		}
		addTreeNode(new NetworkTab(parent, true), toolSet);
		addTreeNode(new ParasiticTab(parent, true), toolSet);
		addTreeNode(new RoutingTab(parent, true), toolSet);
		addTreeNode(new SiliconCompilerTab(parent, true), toolSet);
		addTreeNode(new SimulatorsTab(parent, true), toolSet);
		addTreeNode(new SpiceTab(parent, true), toolSet);
		addTreeNode(new CellModelTab(parent, true, CellModelPrefs.spiceModelPrefs), toolSet);
		if (Routing.hasSunRouter())
			addTreeNode(new SunRouterTab(parent, true), toolSet);
        addTreeNode(new VerilogTab(parent, true), toolSet);
		addTreeNode(new CellModelTab(parent, true, CellModelPrefs.verilogModelPrefs), toolSet);
		addTreeNode(new WellCheckTab(parent, true), toolSet);

		// the "Technology" section of the Preferences
		DefaultMutableTreeNode techSet = new DefaultMutableTreeNode("Technology ");
		rootNode.add(techSet);
		addTreeNode(new TechnologyTab(parent, true), techSet);
		addTreeNode(new DesignRulesTab(parent, true), techSet);
		addTreeNode(new UnitsTab(parent, true), techSet);
		addTreeNode(new IconTab(parent, true), techSet);

		// pre-expand the tree
		TreePath topPath = optionTree.getPathForRow(0);
		optionTree.expandPath(topPath);
		topPath = optionTree.getPathForRow(1);
		optionTree.expandPath(topPath);
		topPath = optionTree.getNextMatch(currentSectionName, 0, null);
		optionTree.expandPath(topPath);

        // searching for selected node
        openSelectedPath(rootNode);

		// the left side of the preferences dialog: a tree
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new GridBagLayout());

		JScrollPane scrolledTree = new JScrollPane(optionTree);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;   gbc.weighty = 1.0;
		leftPanel.add(scrolledTree, gbc);

		JButton save = new JButton("Export");
		save.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { exportActionPerformed(); }
		});
		gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 1;
		gbc.insets = new Insets(4, 4, 4, 4);
		leftPanel.add(save, gbc);

		JButton restore = new JButton("Import");
		restore.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { importActionPerformed(); }
		});
		gbc = new GridBagConstraints();
		gbc.gridx = 1;   gbc.gridy = 1;
		gbc.insets = new Insets(4, 4, 4, 4);
		leftPanel.add(restore, gbc);

		JButton help = new JButton("Help");
		help.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { helpActionPerformed(); }
		});
		gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 2;
		gbc.gridwidth = 2;
		gbc.insets = new Insets(4, 4, 4, 4);
		leftPanel.add(help, gbc);

		cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { cancelActionPerformed(); }
		});
		gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 3;
		gbc.insets = new Insets(4, 4, 4, 4);
		leftPanel.add(cancel, gbc);

		ok = new JButton("OK");
		ok.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { okActionPerformed(); }
		});
		gbc = new GridBagConstraints();
		gbc.gridx = 1;   gbc.gridy = 3;
		gbc.insets = new Insets(4, 4, 4, 4);
		leftPanel.add(ok, gbc);
		getRootPane().setDefaultButton(ok);

		getRootPane().setDefaultButton(ok);

		// build preferences framework
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

		loadOptionPanel();
		splitPane.setLeftComponent(leftPanel);
		recursivelyHighlight(optionTree, rootNode, currentDMTN, optionTree.getPathForRow(0));

		gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;   gbc.weighty = 1.0;
		getContentPane().add(splitPane, gbc);

		pack();
		finishInitialization();
	}

	private void addTreeNode(PreferencePanel panel, DefaultMutableTreeNode theSet)
	{
		optionPanes.add(panel);
		String sectionName = (String)theSet.getUserObject();
		String name = panel.getName();
		DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(name);
		theSet.add(dmtn);
		if (sectionName.equals(currentSectionName) && name.equals(currentTabName))
			currentDMTN = dmtn;
	}

    private boolean openSelectedPath(DefaultMutableTreeNode rootNode)
    {
        for (int i = 0; i < rootNode.getChildCount(); i++)
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)rootNode.getChildAt(i);
            Object o = node.getUserObject();
            if (o.toString().equals(currentTabName))//indexOf(currentTabName) != -1)
            {
                optionTree.scrollPathToVisible(new TreePath(node.getPath()));
                return true;
            }
            if (openSelectedPath(node)) return true;
        }
        return false;
    }

	private void cancelActionPerformed()
	{
		closeDialog(null);
	}

	private void okActionPerformed()
	{
		new OKUpdate(this);
	}

	private void helpActionPerformed()
	{
		ManualViewer.showPreferenceHelp(currentSectionName.trim() + "/" + currentTabName);
		closeDialog(null);
	}

	private void exportActionPerformed()
	{
		Job.getUserInterface().exportPrefs();
	}

	private void importActionPerformed()
	{
		Job.getUserInterface().importPrefs();
        TopLevel top = TopLevel.getCurrentJFrame();
        top.getEMenuBar().restoreSavedBindings(false); // trying to cache again

		// recache all layers and their graphics
        Technology.cacheTransparentLayerColors();

		// close dialog now because all values are cached badly
		closeDialog(null);

		// redraw everything
		EditWindow.repaintAllContents();
        for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
        {
        	WindowFrame wf = it.next();
        	wf.loadComponentMenuForTechnology();
        }
	}

	private void loadOptionPanel()
	{
		for(PreferencePanel ti : optionPanes)
		{
			if (ti.getName().equals(currentTabName))
			{
				if (!ti.isInited())
				{
					ti.init();
					ti.setInited();
				}
				splitPane.setRightComponent(ti.getPanel());
				return;
			}
		}
	}

	protected void escapePressed() { cancelActionPerformed(); }

	/**
	 * Class to update primitive node information.
	 */
	private static class OKUpdate extends Job
	{
		private transient PreferencesFrame dialog;
		private Pref.PrefChangeBatch changeBatch;

		private OKUpdate(PreferencesFrame dialog)
		{
			super("Update Preferences", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.dialog = dialog;

			// gather preference changes on the client
			Pref.gatherPrefChanges();
			for(PreferencePanel ti : dialog.optionPanes)
			{
				if (ti.isInited())
					ti.term();
			}
			changeBatch = Pref.getPrefChanges();
			startJob();
		}

		public boolean doIt() throws JobException
		{
			Pref.implementPrefChanges(changeBatch);
			return true;
		}

		public void terminateOK()
		{
			dialog.closeDialog(null);
		}
	}

	private static class TreeHandler implements MouseListener, TreeExpansionListener
	{
		private PreferencesFrame dialog;

		TreeHandler(PreferencesFrame dialog) { this.dialog = dialog; }

		public void mouseClicked(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}

		public void mousePressed(MouseEvent e)
		{
			TreePath currentPath = dialog.optionTree.getPathForLocation(e.getX(), e.getY());
			if (currentPath == null) return;
			dialog.optionTree.setSelectionPath(currentPath);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)currentPath.getLastPathComponent();
			currentTabName = (String)node.getUserObject();
			dialog.optionTree.expandPath(currentPath);
			if (currentTabName.endsWith(" "))
			{
				currentSectionName = currentTabName;
			} else
			{
				dialog.loadOptionPanel();
			}
			dialog.pack();
		}

		public void treeCollapsed(TreeExpansionEvent e)
		{
			dialog.pack();
		}
		public void treeExpanded(TreeExpansionEvent e)
		{
			TreePath tp = e.getPath();
			if (tp.getPathCount() == 2)
			{
				// opened a path down to the bottom: close all others
				TreePath topPath = dialog.optionTree.getPathForRow(0);
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)topPath.getLastPathComponent();
				int numChildren = node.getChildCount();
				for(int i=0; i<numChildren; i++)
				{
					DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
					TreePath descentPath = topPath.pathByAddingChild(child);
					if (descentPath.getLastPathComponent().equals(tp.getLastPathComponent()))
					{
						DefaultMutableTreeNode subNode = (DefaultMutableTreeNode)descentPath.getLastPathComponent();
						currentSectionName = (String)subNode.getUserObject();
					} else
					{
						dialog.optionTree.collapsePath(descentPath);
					}
				}
			}
			dialog.pack();
		}
	}

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)
	{
		setVisible(false);
		dispose();
	}
}
