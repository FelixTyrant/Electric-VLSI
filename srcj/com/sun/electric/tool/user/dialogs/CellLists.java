/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellLists.java
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
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
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


/**
 * Class to handle the "New Cell" dialog.
 */
public class CellLists extends javax.swing.JDialog
{
	/** Creates new form New Cell */
	public CellLists(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		setLocation(100, 50);
		initComponents();

		// make a popup of views
		for(Iterator it = View.getViews(); it.hasNext(); )
		{
			View v = (View)it.next();
			views.addItem(v.getFullName());
		}

		orderByName.setSelected(true);
		displayInMessages.setSelected(true);
		allCells.setSelected(true);
		views.setEnabled(false);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        whichCells = new javax.swing.ButtonGroup();
        ordering = new javax.swing.ButtonGroup();
        destination = new javax.swing.ButtonGroup();
        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        allCells = new javax.swing.JRadioButton();
        onlyCellsUsedElsewhere = new javax.swing.JRadioButton();
        onlyCellsNotUsedElsewhere = new javax.swing.JRadioButton();
        onlyCellsUnderCurrent = new javax.swing.JRadioButton();
        onlyPlaceholderCells = new javax.swing.JRadioButton();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel2 = new javax.swing.JLabel();
        onlyThisView = new javax.swing.JCheckBox();
        views = new javax.swing.JComboBox();
        alsoIconViews = new javax.swing.JCheckBox();
        jSeparator2 = new javax.swing.JSeparator();
        jLabel3 = new javax.swing.JLabel();
        excludeOlderVersions = new javax.swing.JCheckBox();
        excludeNewestVersions = new javax.swing.JCheckBox();
        jSeparator3 = new javax.swing.JSeparator();
        jLabel4 = new javax.swing.JLabel();
        orderByName = new javax.swing.JRadioButton();
        orderByDate = new javax.swing.JRadioButton();
        orderByStructure = new javax.swing.JRadioButton();
        jSeparator4 = new javax.swing.JSeparator();
        jLabel5 = new javax.swing.JLabel();
        displayInMessages = new javax.swing.JRadioButton();
        saveToDisk = new javax.swing.JRadioButton();

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
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 0.5;
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
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 0.5;
        getContentPane().add(ok, gridBagConstraints);

        jLabel1.setText("Which cells:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jLabel1, gridBagConstraints);

        allCells.setText("All cells");
        whichCells.add(allCells);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(allCells, gridBagConstraints);

        onlyCellsUsedElsewhere.setText("Only those used elsewhere");
        whichCells.add(onlyCellsUsedElsewhere);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(onlyCellsUsedElsewhere, gridBagConstraints);

        onlyCellsNotUsedElsewhere.setText("Only those not used elsewhere");
        whichCells.add(onlyCellsNotUsedElsewhere);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(onlyCellsNotUsedElsewhere, gridBagConstraints);

        onlyCellsUnderCurrent.setText("Only those under current cell");
        whichCells.add(onlyCellsUnderCurrent);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(onlyCellsUnderCurrent, gridBagConstraints);

        onlyPlaceholderCells.setText("Only placeholder cells");
        whichCells.add(onlyPlaceholderCells);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(onlyPlaceholderCells, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jSeparator1, gridBagConstraints);

        jLabel2.setText("View filter:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jLabel2, gridBagConstraints);

        onlyThisView.setText("Show only this view:");
        onlyThisView.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                onlyThisViewActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(onlyThisView, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 40, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(views, gridBagConstraints);

        alsoIconViews.setText("Also include icon views");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(alsoIconViews, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jSeparator2, gridBagConstraints);

        jLabel3.setText("Version filter:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jLabel3, gridBagConstraints);

        excludeOlderVersions.setText("Exclude older versions");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(excludeOlderVersions, gridBagConstraints);

        excludeNewestVersions.setText("Exclude newest versions");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(excludeNewestVersions, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jSeparator3, gridBagConstraints);

        jLabel4.setText("Display ordering:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jLabel4, gridBagConstraints);

        orderByName.setText("Order by name");
        ordering.add(orderByName);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(orderByName, gridBagConstraints);

        orderByDate.setText("Order by modification date");
        ordering.add(orderByDate);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(orderByDate, gridBagConstraints);

        orderByStructure.setText("Order by skeletal structure");
        ordering.add(orderByStructure);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 19;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(orderByStructure, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jSeparator4, gridBagConstraints);

        jLabel5.setText("Destination:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 21;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jLabel5, gridBagConstraints);

        displayInMessages.setText("Display in messages window");
        destination.add(displayInMessages);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(displayInMessages, gridBagConstraints);

        saveToDisk.setText("Save to disk");
        destination.add(saveToDisk);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 23;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(saveToDisk, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void onlyThisViewActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_onlyThisViewActionPerformed
	{//GEN-HEADEREND:event_onlyThisViewActionPerformed
		boolean selected = onlyThisView.isSelected();
		views.setEnabled(selected);
	}//GEN-LAST:event_onlyThisViewActionPerformed

	private void cancel(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancel
	{//GEN-HEADEREND:event_cancel
		closeDialog(null);
	}//GEN-LAST:event_cancel

	private void ok(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ok
	{//GEN-HEADEREND:event_ok
		// mark cells to be shown
		FlagSet flagBit = NodeProto.getFlagSet(1);
		if (allCells.isSelected())
		{
			// mark all cells for display
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = (Cell)cIt.next();
					cell.setBit(flagBit);
				}
			}
		} else
		{
			// mark no cells for display, filter according to request
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = (Cell)cIt.next();
					cell.clearBit(flagBit);
				}
			}
			if (onlyCellsUnderCurrent.isSelected())
			{
				// mark those that are under this
//				if (curf != NONODEPROTO) us_recursivemark(curf);
			} else if (onlyCellsUsedElsewhere.isSelected())
			{
				// mark those that are in use
				for(Iterator it = Library.getLibraries(); it.hasNext(); )
				{
					Library lib = (Library)it.next();
					for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
					{
						Cell cell = (Cell)cIt.next();
						Cell iconCell = cell.iconView();
						if (iconCell == null) iconCell = cell;
						if (cell.getInstancesOf().hasNext() || iconCell.getInstancesOf().hasNext())
							cell.setBit(flagBit);
					}
				}
			} else if (onlyCellsNotUsedElsewhere.isSelected())
			{
				// mark those that are not in use
//				for(lib = el_curlib; lib != NOLIBRARY; lib = lib->nextlibrary)
//				{
//					for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//					{
//						inp = iconview(np);
//						if (inp != NONODEPROTO)
//						{
//							// has icon: acceptable if the only instances are examples
//							if (np->firstinst != NONODEINST) continue;
//							for(ni = inp->firstinst; ni != NONODEINST; ni = ni->nextinst)
//								if (!isiconof(inp, ni->parent)) break;
//							if (ni != NONODEINST) continue;
//						} else
//						{
//							// no icon: reject if this has instances
//							if (np->cellview == el_iconview)
//							{
//								// this is an icon: reject if instances are not examples
//								for(ni = np->firstinst; ni != NONODEINST; ni = ni->nextinst)
//									if (!isiconof(np, ni->parent)) break;
//								if (ni != NONODEINST) continue;
//							} else
//							{
//								if (np->firstinst != NONODEINST) continue;
//							}
//						}
//						np->temp1 = 1;
//					}
//				}
			} else
			{
				// mark placeholder cells
				for(Iterator it = Library.getLibraries(); it.hasNext(); )
				{
					Library lib = (Library)it.next();
					for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
					{
						Cell cell = (Cell)cIt.next();
						Variable var = cell.getVar("IO_true_library");
						if (var != null) cell.setBit(flagBit);
					}
				}
			}
		}

		// filter views
		if (onlyThisView.isSelected())
		{
			String viewName = (String)views.getSelectedItem();
			View v = View.findView(viewName);
			if (v != null)
			{
//				for(lib = el_curlib; lib != NOLIBRARY; lib = lib->nextlibrary)
//				{
//					for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//					{
//						if (np->cellview != v)
//						{
//							if (np->cellview == el_iconview)
//							{
//								if (alsoIconViews.isSelected()) continue;
//							}
//							np->temp1 = 0;
//						}
//					}
//				}
			}
		}

		// filter versions
		if (excludeOlderVersions.isSelected())
		{
//			for(lib = el_curlib; lib != NOLIBRARY; lib = lib->nextlibrary)
//			{
//				for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//				{
//					if (np->newestversion != np) np->temp1 = 0;
//				}
//			}
		}
		if (excludeNewestVersions.isSelected())
		{
//			for(lib = el_curlib; lib != NOLIBRARY; lib = lib->nextlibrary)
//			{
//				for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//				{
//					if (np->newestversion == np) np->temp1 = 0;
//				}
//			}
		}

		// now make a list and sort it
		List cellList = new ArrayList();
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			if (lib.isHidden()) continue;
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				if (cell.isBit(flagBit)) cellList.add(cell);
			}
		}
		if (cellList.size() == 0) System.out.println("No cells match this request"); else
		{
			if (orderByName.isSelected())
			{
//				esort(nplist, total, sizeof (NODEPROTO *), us_sortbycellname);
			} else if (orderByDate.isSelected())
			{
//				esort(nplist, total, sizeof (NODEPROTO *), us_sortbycelldate);
			} else if (orderByStructure.isSelected())
			{
//				esort(nplist, total, sizeof (NODEPROTO *), us_sortbyskeletonstructure);
			}

			// finally show the results
			if (saveToDisk.isSelected())
			{
//				dumpfile = xcreate(x_("celllist.txt"), el_filetypetext, _("Cell Listing File:"), &truename);
//				if (dumpfile == 0) ttyputerr(_("Cannot write cell listing")); else
//				{
//					efprintf(dumpfile, _("List of cells created on %s\n"), timetostring(getcurrenttime()));
//					efprintf(dumpfile, _("Cell\tVersion\tCreation date\tRevision Date\tSize\tUsage\tLock\tInst-lock\tCell-lib\tDRC\tNCC\n"));
//					for(i=0; i<total; i++)
//						efprintf(dumpfile, x_("%s\n"), us_makecellline(nplist[i], -1));
//					xclose(dumpfile);
//					ttyputmsg(_("Wrote %s"), truename);
//				}
			} else
			{
//				maxlen = 0;
//				for(i=0; i<total; i++)
//					maxlen = maxi(maxlen, estrlen(nldescribenodeproto(nplist[i])));
//				maxlen = maxi(maxlen+2, 7);
//				infstr = initinfstr();
//				addstringtoinfstr(infstr, _("Cell"));
//				for(i=4; i<maxlen; i++) addtoinfstr(infstr, '-');
//				addstringtoinfstr(infstr, _("Version-----Creation date"));
//				addstringtoinfstr(infstr, _("---------Revision Date------------Size-------Usage-L-I-C-D-N"));
//				ttyputmsg(x_("%s"), returninfstr(infstr));
//				lib = NOLIBRARY;
//				for(i=0; i<total; i++)
//				{
//					if (nplist[i]->lib != lib)
//					{
//						lib = nplist[i]->lib;
//						ttyputmsg(_("======== LIBRARY %s: ========"), lib->libname);
//					}
//					savelib = el_curlib;
//					el_curlib = lib;
//					ttyputmsg(x_("%s"), us_makecellline(nplist[i], maxlen));
//					el_curlib = lib;
//				}
			}
		}
		closeDialog(null);
	}//GEN-LAST:event_ok

//	/*
//	 * Routine to recursively walk the hierarchy from "np", marking all cells below it.
//	 */
//	void us_recursivemark(NODEPROTO *np)
//	{
//		REGISTER NODEINST *ni;
//		REGISTER NODEPROTO *cnp;
//
//		if (np->temp1 != 0) return;
//		np->temp1 = 1;
//		for(ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//		{
//			if (ni->proto->primindex != 0) continue;
//			us_recursivemark(ni->proto);
//			cnp = contentsview(ni->proto);
//			if (cnp != NONODEPROTO) us_recursivemark(cnp);
//		}
//	}

//	/*
//	 * Helper routine for "esort" that makes cells go by skeleton structure
//	 */
//	int us_sortbyskeletonstructure(const void *e1, const void *e2)
//	{
//		REGISTER NODEPROTO *f1, *f2;
//		REGISTER INTBIG xs1, xs2, ys1, ys2, pc1, pc2;
//		INTBIG x1, y1, x2, y2;
//		REGISTER PORTPROTO *pp1, *pp2;
//		NODEINST dummyni;
//		REGISTER NODEINST *ni;
//
//		f1 = *((NODEPROTO **)e1);
//		f2 = *((NODEPROTO **)e2);
//
//		// first sort by cell size
//		xs1 = f1->highx - f1->lowx;   xs2 = f2->highx - f2->lowx;
//		if (xs1 != xs2) return(xs1-xs2);
//		ys1 = f1->highy - f1->lowy;   ys2 = f2->highy - f2->lowy;
//		if (ys1 != ys2) return(ys1-ys2);
//
//		// now sort by number of exports
//		pc1 = 0;
//		for(pp1 = f1->firstportproto; pp1 != NOPORTPROTO; pp1 = pp1->nextportproto) pc1++;
//		pc2 = 0;
//		for(pp2 = f2->firstportproto; pp2 != NOPORTPROTO; pp2 = pp2->nextportproto) pc2++;
//		if (pc1 != pc2) return(pc1-pc2);
//
//		// now match the exports
//		for(pp1 = f1->firstportproto; pp1 != NOPORTPROTO; pp1 = pp1->nextportproto) pp1->temp1 = 0;
//		for(pp2 = f2->firstportproto; pp2 != NOPORTPROTO; pp2 = pp2->nextportproto)
//		{
//			// locate center of this export
//			ni = &dummyni;
//			initdummynode(ni);
//			ni->proto = f2;
//			ni->lowx = -xs1/2;   ni->highx = ni->lowx + xs1;
//			ni->lowy = -ys1/2;   ni->highy = ni->lowy + ys1;
//			portposition(ni, pp2, &x2, &y2);
//
//			ni->proto = f1;
//			for(pp1 = f1->firstportproto; pp1 != NOPORTPROTO; pp1 = pp1->nextportproto)
//			{
//				portposition(ni, pp1, &x1, &y1);
//				if (x1 == x2 && y1 == y2) break;
//			}
//			if (pp1 == NOPORTPROTO) return(f1-f2);
//			pp1->temp1 = 1;
//		}
//		for(pp1 = f1->firstportproto; pp1 != NOPORTPROTO; pp1 = pp1->nextportproto)
//			if (pp1->temp1 == 0) return(f1-f2);
//		return(0);
//	}
//
//	/*
//	 * Helper routine for "esort" that makes cells go by date
//	 */
//	int us_sortbycelldate(const void *e1, const void *e2)
//	{
//		REGISTER NODEPROTO *f1, *f2;
//		REGISTER UINTBIG r1, r2;
//
//		f1 = *((NODEPROTO **)e1);
//		f2 = *((NODEPROTO **)e2);
//		r1 = f1->revisiondate;
//		r2 = f2->revisiondate;
//		if (r1 == r2) return(0);
//		if (r1 < r2) return(-(int)(r2-r1));
//		return(r1-r2);
//	}
//
//	/*
//	 * Helper routine for "esort" that makes cell names be ascending
//	 */
//	int us_sortbycellname(const void *e1, const void *e2)
//	{
//		REGISTER NODEPROTO *f1, *f2;
//		REGISTER void *infstr;
//		REGISTER CHAR *s1, *s2;
//
//		f1 = *((NODEPROTO **)e1);
//		f2 = *((NODEPROTO **)e2);
//		infstr = initinfstr();
//		formatinfstr(infstr, x_("%s:%s"), f1->lib->libname, nldescribenodeproto(f1));
//		s1 = returninfstr(infstr);
//		infstr = initinfstr();
//		formatinfstr(infstr, x_("%s:%s"), f2->lib->libname, nldescribenodeproto(f2));
//		s2 = returninfstr(infstr);
//		return(namesame(s1, s2));
//	}

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JRadioButton allCells;
    private javax.swing.JCheckBox alsoIconViews;
    private javax.swing.JButton cancel;
    private javax.swing.ButtonGroup destination;
    private javax.swing.JRadioButton displayInMessages;
    private javax.swing.JCheckBox excludeNewestVersions;
    private javax.swing.JCheckBox excludeOlderVersions;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JButton ok;
    private javax.swing.JRadioButton onlyCellsNotUsedElsewhere;
    private javax.swing.JRadioButton onlyCellsUnderCurrent;
    private javax.swing.JRadioButton onlyCellsUsedElsewhere;
    private javax.swing.JRadioButton onlyPlaceholderCells;
    private javax.swing.JCheckBox onlyThisView;
    private javax.swing.JRadioButton orderByDate;
    private javax.swing.JRadioButton orderByName;
    private javax.swing.JRadioButton orderByStructure;
    private javax.swing.ButtonGroup ordering;
    private javax.swing.JRadioButton saveToDisk;
    private javax.swing.JComboBox views;
    private javax.swing.ButtonGroup whichCells;
    // End of variables declaration//GEN-END:variables
	
}
