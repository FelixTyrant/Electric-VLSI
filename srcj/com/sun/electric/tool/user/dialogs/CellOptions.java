/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellOptions.java
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
package com.sun.electric.tool.user.dialogs;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.Job;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;


/**
 * Class to handle the "Cell Options" dialog.
 */
public class CellOptions extends javax.swing.JDialog
{
	private JList cellList;
	private DefaultListModel cellListModel;

	/** Creates new form Cell Options */
	public CellOptions(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		setLocation(100, 50);
		initComponents();

		// build the cell list
		cellListModel = new DefaultListModel();
		cellList = new JList(cellListModel);
		cellList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		cellPane.setViewportView(cellList);
		cellList.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseClicked(java.awt.event.MouseEvent evt) { cellListClick(); }
		});

		// make a popup of libraries
		List libList = Library.getVisibleLibrariesSortedByName();
		for(Iterator it = libList.iterator(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			libraryPopup.addItem(lib.getLibName());
		}
		int curIndex = libList.indexOf(Library.getCurrent());
		if (curIndex >= 0) libraryPopup.setSelectedIndex(curIndex);

		loadCellList();
	}

	private void loadCellList()
	{
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		if (lib == null) return;
		boolean any = false;
		cellListModel.clear();
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			cellListModel.addElement(cell.noLibDescribe());
			any = true;
		}
		if (any) cellList.setSelectedIndex(0); else
			cellList.setSelectedValue(null, false);
		cellListClick();
	}

	private void cellListClick()
	{
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		String cellName = (String)cellList.getSelectedValue();
		if (cellName == null) return;
		Cell cell = lib.findNodeProto(cellName);
		if (cell == null) return;

		disallowModAnyInCell.setSelected(cell.isAllLocked());
		disallowModInstInCell.setSelected(cell.isInstancesLocked());
		partOfCellLib.setSelected(cell.isInCellLibrary());
		useTechEditor.setSelected(cell.isInTechnologyLibrary());
		expandNewInstances.setSelected(cell.isWantExpanded());
		unexpandNewInstances.setSelected(!cell.isWantExpanded());
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        expansion = new javax.swing.ButtonGroup();
        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        libraryPopup = new javax.swing.JComboBox();
        cellPane = new javax.swing.JScrollPane();
        disallowModAnyInCell = new javax.swing.JCheckBox();
        setDisallowModAnyInCell = new javax.swing.JButton();
        clearDisallowModAnyInCell = new javax.swing.JButton();
        disallowModInstInCell = new javax.swing.JCheckBox();
        setDisallowModInstInCell = new javax.swing.JButton();
        clearDisallowModInstInCell = new javax.swing.JButton();
        partOfCellLib = new javax.swing.JCheckBox();
        setPartOfCellLib = new javax.swing.JButton();
        clearPartOfCellLib = new javax.swing.JButton();
        useTechEditor = new javax.swing.JCheckBox();
        setUseTechEditor = new javax.swing.JButton();
        clearUseTechEditor = new javax.swing.JButton();
        expandNewInstances = new javax.swing.JRadioButton();
        unexpandNewInstances = new javax.swing.JRadioButton();
        jLabel2 = new javax.swing.JLabel();
        charXSpacing = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        charYSpacing = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        hashScale = new javax.swing.JTextField();
        explorerTextSize = new javax.swing.JTextField();
        checkDatesDuringCreation = new javax.swing.JCheckBox();
        autoSwitchTechnology = new javax.swing.JCheckBox();
        placeCellCenter = new javax.swing.JCheckBox();
        tinyInstancesHashed = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("New Cell");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cancel(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(cancel, gridBagConstraints);

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                ok(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        jLabel1.setText("Library:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel1, gridBagConstraints);

        libraryPopup.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                libraryPopupActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(libraryPopup, gridBagConstraints);

        cellPane.setMinimumSize(new java.awt.Dimension(200, 300));
        cellPane.setPreferredSize(new java.awt.Dimension(200, 300));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(cellPane, gridBagConstraints);

        disallowModAnyInCell.setText("Disallow modification of anything in this cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        getContentPane().add(disallowModAnyInCell, gridBagConstraints);

        setDisallowModAnyInCell.setText("Set");
        setDisallowModAnyInCell.setMinimumSize(new java.awt.Dimension(53, 20));
        setDisallowModAnyInCell.setPreferredSize(new java.awt.Dimension(53, 20));
        setDisallowModAnyInCell.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                setDisallowModAnyInCellActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 2);
        getContentPane().add(setDisallowModAnyInCell, gridBagConstraints);

        clearDisallowModAnyInCell.setText("Clear");
        clearDisallowModAnyInCell.setMinimumSize(new java.awt.Dimension(64, 20));
        clearDisallowModAnyInCell.setPreferredSize(new java.awt.Dimension(64, 20));
        clearDisallowModAnyInCell.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                clearDisallowModAnyInCellActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 2, 2, 4);
        getContentPane().add(clearDisallowModAnyInCell, gridBagConstraints);

        disallowModInstInCell.setText("Disallow modification of instances in this cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        getContentPane().add(disallowModInstInCell, gridBagConstraints);

        setDisallowModInstInCell.setText("Set");
        setDisallowModInstInCell.setMinimumSize(new java.awt.Dimension(53, 20));
        setDisallowModInstInCell.setPreferredSize(new java.awt.Dimension(53, 20));
        setDisallowModInstInCell.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                setDisallowModInstInCellActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 2);
        getContentPane().add(setDisallowModInstInCell, gridBagConstraints);

        clearDisallowModInstInCell.setText("Clear");
        clearDisallowModInstInCell.setMinimumSize(new java.awt.Dimension(64, 20));
        clearDisallowModInstInCell.setPreferredSize(new java.awt.Dimension(64, 20));
        clearDisallowModInstInCell.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                clearDisallowModInstInCellActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 4);
        getContentPane().add(clearDisallowModInstInCell, gridBagConstraints);

        partOfCellLib.setText("Part of a cell-library");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        getContentPane().add(partOfCellLib, gridBagConstraints);

        setPartOfCellLib.setText("Set");
        setPartOfCellLib.setMinimumSize(new java.awt.Dimension(53, 20));
        setPartOfCellLib.setPreferredSize(new java.awt.Dimension(53, 20));
        setPartOfCellLib.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                setPartOfCellLibActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 2);
        getContentPane().add(setPartOfCellLib, gridBagConstraints);

        clearPartOfCellLib.setText("Clear");
        clearPartOfCellLib.setMinimumSize(new java.awt.Dimension(64, 20));
        clearPartOfCellLib.setPreferredSize(new java.awt.Dimension(64, 20));
        clearPartOfCellLib.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                clearPartOfCellLibActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 4);
        getContentPane().add(clearPartOfCellLib, gridBagConstraints);

        useTechEditor.setText("Use technology editor on this cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        getContentPane().add(useTechEditor, gridBagConstraints);

        setUseTechEditor.setText("Set");
        setUseTechEditor.setMinimumSize(new java.awt.Dimension(53, 20));
        setUseTechEditor.setPreferredSize(new java.awt.Dimension(53, 20));
        setUseTechEditor.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                setUseTechEditorActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 2);
        getContentPane().add(setUseTechEditor, gridBagConstraints);

        clearUseTechEditor.setText("Clear");
        clearUseTechEditor.setMinimumSize(new java.awt.Dimension(64, 20));
        clearUseTechEditor.setPreferredSize(new java.awt.Dimension(64, 20));
        clearUseTechEditor.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                clearUseTechEditorActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 4, 4);
        getContentPane().add(clearUseTechEditor, gridBagConstraints);

        expandNewInstances.setText("Expand new instances");
        expansion.add(expandNewInstances);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(expandNewInstances, gridBagConstraints);

        unexpandNewInstances.setText("Unexpand new instances");
        expansion.add(unexpandNewInstances);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(unexpandNewInstances, gridBagConstraints);

        jLabel2.setText("Characteristic X Spacing:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel2, gridBagConstraints);

        charXSpacing.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(charXSpacing, gridBagConstraints);

        jLabel3.setText("Characteristic Y Spacing:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel3, gridBagConstraints);

        charYSpacing.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(charYSpacing, gridBagConstraints);

        jLabel4.setText("Every cell:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        getContentPane().add(jLabel4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(jSeparator1, gridBagConstraints);

        hashScale.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(hashScale, gridBagConstraints);

        explorerTextSize.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(explorerTextSize, gridBagConstraints);

        checkDatesDuringCreation.setText("Check cell dates during creation");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(checkDatesDuringCreation, gridBagConstraints);

        autoSwitchTechnology.setText("Switch technology to match current cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(autoSwitchTechnology, gridBagConstraints);

        placeCellCenter.setText("Place Cell-Center in new cells");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(placeCellCenter, gridBagConstraints);

        tinyInstancesHashed.setText("Tiny cell instances hashed out");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(tinyInstancesHashed, gridBagConstraints);

        jLabel6.setText("Hash cells when scale is more than:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel6, gridBagConstraints);

        jLabel7.setText("units per pixel");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel7, gridBagConstraints);

        jLabel8.setText("Cell explorer text size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel8, gridBagConstraints);

        jLabel5.setText("For all cells:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel5, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void libraryPopupActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_libraryPopupActionPerformed
	{//GEN-HEADEREND:event_libraryPopupActionPerformed
		loadCellList();
	}//GEN-LAST:event_libraryPopupActionPerformed

	private void setUseTechEditorActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_setUseTechEditorActionPerformed
	{//GEN-HEADEREND:event_setUseTechEditorActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_setUseTechEditorActionPerformed

	private void clearPartOfCellLibActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_clearPartOfCellLibActionPerformed
	{//GEN-HEADEREND:event_clearPartOfCellLibActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_clearPartOfCellLibActionPerformed

	private void setPartOfCellLibActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_setPartOfCellLibActionPerformed
	{//GEN-HEADEREND:event_setPartOfCellLibActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_setPartOfCellLibActionPerformed

	private void clearDisallowModInstInCellActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_clearDisallowModInstInCellActionPerformed
	{//GEN-HEADEREND:event_clearDisallowModInstInCellActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_clearDisallowModInstInCellActionPerformed

	private void setDisallowModInstInCellActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_setDisallowModInstInCellActionPerformed
	{//GEN-HEADEREND:event_setDisallowModInstInCellActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_setDisallowModInstInCellActionPerformed

	private void clearDisallowModAnyInCellActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_clearDisallowModAnyInCellActionPerformed
	{//GEN-HEADEREND:event_clearDisallowModAnyInCellActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_clearDisallowModAnyInCellActionPerformed

	private void setDisallowModAnyInCellActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_setDisallowModAnyInCellActionPerformed
	{//GEN-HEADEREND:event_setDisallowModAnyInCellActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_setDisallowModAnyInCellActionPerformed

	private void clearUseTechEditorActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_clearUseTechEditorActionPerformed
	{//GEN-HEADEREND:event_clearUseTechEditorActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_clearUseTechEditorActionPerformed

	private void cancel(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancel
	{//GEN-HEADEREND:event_cancel
		closeDialog(null);
	}//GEN-LAST:event_cancel

	private void ok(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ok
	{//GEN-HEADEREND:event_ok

		closeDialog(null);
	}//GEN-LAST:event_ok

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox autoSwitchTechnology;
    private javax.swing.JButton cancel;
    private javax.swing.JScrollPane cellPane;
    private javax.swing.JTextField charXSpacing;
    private javax.swing.JTextField charYSpacing;
    private javax.swing.JCheckBox checkDatesDuringCreation;
    private javax.swing.JButton clearDisallowModAnyInCell;
    private javax.swing.JButton clearDisallowModInstInCell;
    private javax.swing.JButton clearPartOfCellLib;
    private javax.swing.JButton clearUseTechEditor;
    private javax.swing.JCheckBox disallowModAnyInCell;
    private javax.swing.JCheckBox disallowModInstInCell;
    private javax.swing.JRadioButton expandNewInstances;
    private javax.swing.ButtonGroup expansion;
    private javax.swing.JTextField explorerTextSize;
    private javax.swing.JTextField hashScale;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JComboBox libraryPopup;
    private javax.swing.JButton ok;
    private javax.swing.JCheckBox partOfCellLib;
    private javax.swing.JCheckBox placeCellCenter;
    private javax.swing.JButton setDisallowModAnyInCell;
    private javax.swing.JButton setDisallowModInstInCell;
    private javax.swing.JButton setPartOfCellLib;
    private javax.swing.JButton setUseTechEditor;
    private javax.swing.JCheckBox tinyInstancesHashed;
    private javax.swing.JRadioButton unexpandNewInstances;
    private javax.swing.JCheckBox useTechEditor;
    // End of variables declaration//GEN-END:variables
	
}
