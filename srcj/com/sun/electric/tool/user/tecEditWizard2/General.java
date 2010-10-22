/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: General.java
 *
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.tool.user.tecEditWizard2;

import com.sun.electric.util.TextUtils;

import java.awt.*;

/**
 * Class to handle the "General" tab of the Numeric Technology Editor dialog.
 */
public class General extends TechEditWizardPanel
{
	/** Creates new form General */
	public General(TechEditWizard parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this Numeric Technology Editor tab. */
	public Component getComponent() { return general; }

	/** return the name of this Numeric Technology Editor tab. */
	public String getName() { return "General"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the General tab.
	 */
	public void init()
	{
		TechEditWizardData data = wizard.getTechEditData();
		stepSize.setText(Integer.toString(data.getStepSize()));
        resolution.setText(Double.toString(data.getResolution()));
        techName.setText(data.getTechName());
		description.setText(data.getTechDescription());

        psubstrateButton.setSelected(data.getPSubstratelProcess());
        horizontalButton.setSelected(data.getHorizontalTransistors());
    }

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the General tab.
	 */
	public void term()
	{
		TechEditWizardData data = wizard.getTechEditData();
		data.setStepSize(TextUtils.atoi(stepSize.getText()));
        data.setResolution(TextUtils.atof(resolution.getText()));
        data.setTechName(techName.getText());
		data.setTechDescription(description.getText());
        data.setPSubstratelProcess(psubstrateButton.isSelected());
        data.setHorizontalTransistors(horizontalButton.isSelected());
    }

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        general = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        stepSize = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        techName = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        description = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        psubstrateButton = new javax.swing.JCheckBox();
        horizontalButton = new javax.swing.JCheckBox();
        jLabel13 = new javax.swing.JLabel();
        resolution = new javax.swing.JTextField();

        setTitle("General");
        setName(""); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        general.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("Unit size (nm):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 0);
        general.add(jLabel1, gridBagConstraints);

        stepSize.setColumns(15);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        general.add(stepSize, gridBagConstraints);

        jLabel2.setText("Technology name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 0);
        general.add(jLabel2, gridBagConstraints);

        techName.setColumns(15);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 1, 2);
        general.add(techName, gridBagConstraints);

        jLabel3.setText("Description:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 0);
        general.add(jLabel3, gridBagConstraints);

        description.setColumns(15);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        general.add(description, gridBagConstraints);

        jLabel4.setText("This wizard creates a new technology from");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 0);
        general.add(jLabel4, gridBagConstraints);

        jLabel5.setText("numeric parameters provided by the foundry.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 0);
        general.add(jLabel5, gridBagConstraints);

        jLabel6.setText("Click on all of the panels listed on the left and");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 0);
        general.add(jLabel6, gridBagConstraints);

        jLabel7.setText("fill in the values.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 0);
        general.add(jLabel7, gridBagConstraints);

        jLabel8.setText("When done, click \"Write XML\" to write");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 0);
        general.add(jLabel8, gridBagConstraints);

        jLabel9.setText("an XML file that can be added to Electric with");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 0);
        general.add(jLabel9, gridBagConstraints);

        jLabel10.setText("the \"Added Technologies\" project settings.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 0);
        general.add(jLabel10, gridBagConstraints);

        jLabel11.setText("Use \"Save Parameters\" to save these values.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 0);
        general.add(jLabel11, gridBagConstraints);

        jLabel12.setText("Use \"Load Paramerers\" to restore saved values.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 0);
        general.add(jLabel12, gridBagConstraints);

        jLabel14.setText("be further edited with the Technology Editor.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 0);
        general.add(jLabel14, gridBagConstraints);

        jLabel15.setText("Once installed in Electric, the technology can");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 0);
        general.add(jLabel15, gridBagConstraints);

        psubstrateButton.setText("Psubstrate process");
        psubstrateButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        psubstrateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                psubstrateButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 0);
        general.add(psubstrateButton, gridBagConstraints);

        horizontalButton.setText("Horizontal transistors");
        horizontalButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 0);
        general.add(horizontalButton, gridBagConstraints);

        jLabel13.setText("Resolution (nm):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 0);
        general.add(jLabel13, gridBagConstraints);

        resolution.setColumns(15);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        general.add(resolution, gridBagConstraints);

        getContentPane().add(general, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    private void psubstrateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_psubstrateButtonActionPerformed
        // TODO add your handling code here:
}//GEN-LAST:event_psubstrateButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField description;
    private javax.swing.JPanel general;
    private javax.swing.JCheckBox horizontalButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JCheckBox psubstrateButton;
    private javax.swing.JTextField resolution;
    private javax.swing.JTextField stepSize;
    private javax.swing.JTextField techName;
    // End of variables declaration//GEN-END:variables

}
