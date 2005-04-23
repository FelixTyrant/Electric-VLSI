/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NewExport.java
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
package com.sun.electric.plugins.j3d.ui;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.plugins.j3d.View3DWindow;
import com.sun.electric.plugins.j3d.utils.J3DClientApp;
import com.sun.electric.plugins.j3d.utils.J3DUtils;
import com.sun.electric.plugins.j3d.utils.J3DAlpha;
import com.sun.electric.plugins.j3d.utils.J3DUtils;
import com.sun.electric.plugins.j3d.utils.J3DClientApp;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Class to handle the "3D View Demo Dialog" dialog.
 * @author  Gilda Garreton
 * @version 0.1
 */
public class J3DViewDialog extends EDialog
{
    private View3DWindow view3D = null;
    private J3DClientApp socketJob = null;
    private String hostname;
    private List knots = new ArrayList();
    private Map interMap;

    public static void create3DViewDialog(java.awt.Frame parent, String hostname)
    {
        View3DWindow view3D = null;
        WindowContent content = WindowFrame.getCurrentWindowFrame().getContent();
        if (content instanceof View3DWindow)
            view3D = (View3DWindow)content;
        else
        {
            System.out.println("Current Window Frame is not a 3D View");
            return;
        }
        J3DViewDialog dialog = new J3DViewDialog(parent, view3D, false, hostname);
		dialog.setVisible(true);
    }

	/** Creates new form ThreeView */
	public J3DViewDialog(java.awt.Frame parent, View3DWindow view3d, boolean modal, String hostname)
	{
		super(parent, modal);
		initComponents();
        this.view3D = view3d;
        this.hostname = hostname;
        getRootPane().setDefaultButton(connect);
//        spline.addItem("KB Spline");
//        spline.addItem("TCB Spline");
        if (J3DUtils.jAlpha != null)
        {
            slider.addChangeListener(J3DUtils.jAlpha);
            auto.setSelected(J3DUtils.jAlpha.getAutoMode());
        }

        // setting initial other values
        setOtherValues("?", "?");

        // to calculate window position
		finishInitialization();
	}

    public void socketAction(String inData)
    {
        String[] stringValues = J3DClientApp.parseValues(inData, 0);

        xField.setText(stringValues[0]);
        yField.setText(stringValues[1]);
        zField.setText(stringValues[2]);
        xRotField.setText(stringValues[3]);
        yRotField.setText(stringValues[4]);
        zRotField.setText(stringValues[5]);
        xRotPosField.setText(stringValues[6]);
        yRotPosField.setText(stringValues[7]);
        zRotPosField.setText(stringValues[8]);
        setOtherValues(stringValues[9], stringValues[10]);
        double[] values = J3DClientApp.convertValues(stringValues);
        knots.add(view3D.moveAndRotate(values));
    }

    private void setOtherValues(String capacitance, String radius)
    {
        capacitanceLabel.setText("Capacitance: " + capacitance + " [fF]");
        radiusLabel.setText("Radius: " + radius + " [mm]");
    }

	protected void escapePressed() { closeActionPerformed(null); }

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        rotationPanel = new javax.swing.JPanel();
        zRotPosLabelUnit = new javax.swing.JLabel();
        yRotLabelUnit = new javax.swing.JLabel();
        yRotPosLabelUnit = new javax.swing.JLabel();
        xRotPosLabelUnit = new javax.swing.JLabel();
        zRotLabelUnit = new javax.swing.JLabel();
        xRotLabelUnit = new javax.swing.JLabel();
        zRotPosField = new javax.swing.JTextField();
        zRotPosLabel = new javax.swing.JLabel();
        yRotPosField = new javax.swing.JTextField();
        yRotPosLabel = new javax.swing.JLabel();
        xRotPosField = new javax.swing.JTextField();
        xRotPosLabel = new javax.swing.JLabel();
        zRotField = new javax.swing.JTextField();
        zRotLabel = new javax.swing.JLabel();
        yRotField = new javax.swing.JTextField();
        yRotLabel = new javax.swing.JLabel();
        xRotField = new javax.swing.JTextField();
        xRotLabel = new javax.swing.JLabel();
        xRotBox = new javax.swing.JCheckBox();
        yRotBox = new javax.swing.JCheckBox();
        zRotBox = new javax.swing.JCheckBox();
        positionPanel = new javax.swing.JPanel();
        zLabelUnit = new javax.swing.JLabel();
        yLabelUnit = new javax.swing.JLabel();
        xLabelUnit = new javax.swing.JLabel();
        zField = new javax.swing.JTextField();
        zLabel = new javax.swing.JLabel();
        yField = new javax.swing.JTextField();
        yLabel = new javax.swing.JLabel();
        xField = new javax.swing.JTextField();
        xLabel = new javax.swing.JLabel();
        xBox = new javax.swing.JCheckBox();
        yBox = new javax.swing.JCheckBox();
        zBox = new javax.swing.JCheckBox();
        otherPanel = new javax.swing.JPanel();
        capacitanceLabel = new javax.swing.JLabel();
        radiusLabel = new javax.swing.JLabel();
        separator = new javax.swing.JSeparator();
        slider = new javax.swing.JSlider();
        auto = new javax.swing.JCheckBox();
        demo = new javax.swing.JButton();
        separator1 = new javax.swing.JSeparator();
        close = new javax.swing.JButton();
        connect = new javax.swing.JButton();
        enter = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("3D Demo Control Dialog");
        setBackground(java.awt.Color.white);
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        rotationPanel.setLayout(new java.awt.GridBagLayout());

        rotationPanel.setBorder(new javax.swing.border.TitledBorder("Rotation Values"));
        zRotPosLabelUnit.setText("[um]");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        rotationPanel.add(zRotPosLabelUnit, gridBagConstraints);

        yRotLabelUnit.setText("[degrees]");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        rotationPanel.add(yRotLabelUnit, gridBagConstraints);

        yRotPosLabelUnit.setText("[um]");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        rotationPanel.add(yRotPosLabelUnit, gridBagConstraints);

        xRotPosLabelUnit.setText("[um]");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        rotationPanel.add(xRotPosLabelUnit, gridBagConstraints);

        zRotLabelUnit.setText("[degrees]");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        rotationPanel.add(zRotLabelUnit, gridBagConstraints);

        xRotLabelUnit.setText("[degrees]");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        rotationPanel.add(xRotLabelUnit, gridBagConstraints);

        zRotPosField.setPreferredSize(new java.awt.Dimension(60, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        rotationPanel.add(zRotPosField, gridBagConstraints);

        zRotPosLabel.setText("Z:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        rotationPanel.add(zRotPosLabel, gridBagConstraints);

        yRotPosField.setPreferredSize(new java.awt.Dimension(60, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        rotationPanel.add(yRotPosField, gridBagConstraints);

        yRotPosLabel.setText("Y:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        rotationPanel.add(yRotPosLabel, gridBagConstraints);

        xRotPosField.setPreferredSize(new java.awt.Dimension(60, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        rotationPanel.add(xRotPosField, gridBagConstraints);

        xRotPosLabel.setText("X:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        rotationPanel.add(xRotPosLabel, gridBagConstraints);

        zRotField.setPreferredSize(new java.awt.Dimension(60, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        rotationPanel.add(zRotField, gridBagConstraints);

        zRotLabel.setText("Angle Z:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        rotationPanel.add(zRotLabel, gridBagConstraints);

        yRotField.setPreferredSize(new java.awt.Dimension(60, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        rotationPanel.add(yRotField, gridBagConstraints);

        yRotLabel.setText("Angle Y:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        rotationPanel.add(yRotLabel, gridBagConstraints);

        xRotField.setPreferredSize(new java.awt.Dimension(60, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        rotationPanel.add(xRotField, gridBagConstraints);

        xRotLabel.setText("Angle X:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        rotationPanel.add(xRotLabel, gridBagConstraints);

        xRotBox.setSelected(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        rotationPanel.add(xRotBox, gridBagConstraints);

        yRotBox.setSelected(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        rotationPanel.add(yRotBox, gridBagConstraints);

        zRotBox.setSelected(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        rotationPanel.add(zRotBox, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.gridheight = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(rotationPanel, gridBagConstraints);

        positionPanel.setLayout(new java.awt.GridBagLayout());

        positionPanel.setBorder(new javax.swing.border.TitledBorder("Position Values"));
        zLabelUnit.setText("[um]");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        positionPanel.add(zLabelUnit, gridBagConstraints);

        yLabelUnit.setText("[um]");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        positionPanel.add(yLabelUnit, gridBagConstraints);

        xLabelUnit.setText("[um]");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        positionPanel.add(xLabelUnit, gridBagConstraints);

        zField.setPreferredSize(new java.awt.Dimension(60, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        positionPanel.add(zField, gridBagConstraints);

        zLabel.setText("Z:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        positionPanel.add(zLabel, gridBagConstraints);

        yField.setPreferredSize(new java.awt.Dimension(60, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        positionPanel.add(yField, gridBagConstraints);

        yLabel.setText("Y:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        positionPanel.add(yLabel, gridBagConstraints);

        xField.setMinimumSize(new java.awt.Dimension(20, 21));
        xField.setPreferredSize(new java.awt.Dimension(60, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        positionPanel.add(xField, gridBagConstraints);

        xLabel.setText("X:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        positionPanel.add(xLabel, gridBagConstraints);

        xBox.setSelected(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        positionPanel.add(xBox, gridBagConstraints);

        yBox.setSelected(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        positionPanel.add(yBox, gridBagConstraints);

        zBox.setSelected(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        positionPanel.add(zBox, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(positionPanel, gridBagConstraints);

        otherPanel.setLayout(new java.awt.GridBagLayout());

        otherPanel.setBorder(new javax.swing.border.TitledBorder(""));
        capacitanceLabel.setText("Capacitance: ? [fF]");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        otherPanel.add(capacitanceLabel, gridBagConstraints);

        radiusLabel.setText("Radius: ? [mm]");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        otherPanel.add(radiusLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(otherPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(separator, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(slider, gridBagConstraints);

        auto.setText("Auto");
        auto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(auto, gridBagConstraints);

        demo.setText("Start Demo");
        demo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                demoActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(demo, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.RELATIVE;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(separator1, gridBagConstraints);

        close.setText("Close");
        close.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(close, gridBagConstraints);

        connect.setText("Connect");
        connect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(connect, gridBagConstraints);

        enter.setText("Enter");
        enter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enterActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(enter, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

    private void autoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoActionPerformed
        J3DUtils.jAlpha.setAutoMode(auto.isSelected());
    }//GEN-LAST:event_autoActionPerformed

    private void enterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enterActionPerformed
        double[] values = new double[9];

        values[0] = TextUtils.atof(xField.getText());
        values[1] = TextUtils.atof(yField.getText());
        values[2] = TextUtils.atof(zField.getText());
        values[3] = J3DUtils.convertToRadiant(TextUtils.atof(xRotField.getText()));
        values[4] = J3DUtils.convertToRadiant(TextUtils.atof(yRotField.getText()));
        values[5] = J3DUtils.convertToRadiant(TextUtils.atof(zRotField.getText()));
        values[6] = TextUtils.atof(xRotPosField.getText());
        values[7] = TextUtils.atof(yRotPosField.getText());
        values[8] = TextUtils.atof(zRotPosField.getText());
        knots.add(view3D.moveAndRotate(values));
    }//GEN-LAST:event_enterActionPerformed

    private void demoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_demoActionPerformed
        if (demo.getText().equals("Start Demo"))
        {
            interMap = view3D.addInterpolator(knots);
            if (interMap != null) // no error
                demo.setText("Stop Demo");
        }
        else
        {
            demo.setText("Start Demo");
            view3D.removeInterpolator(interMap);
        }
        //view3D.set3DCamera(spline.getSelectedIndex());
    }//GEN-LAST:event_demoActionPerformed

    private void connectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectActionPerformed
        if (connect.getText().equals("Connect"))
        {
            connect.setText("Disconnect");
            socketJob = new J3DClientApp(this, hostname);
            socketJob.startJob();
            enter.setEnabled(false);// don't want to add data if stream is connected
        }
        else
        {
            connect.setText("Connect");
            enter.setEnabled(true);
            if (socketJob != null)
                socketJob.killJob();
        }
    }//GEN-LAST:event_connectActionPerformed

    private void closeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeActionPerformed
        setVisible(false);
        dispose();
        if (socketJob != null)
        {
            socketJob.abort();
            socketJob.checkAbort();
            socketJob.remove();
        }
    }//GEN-LAST:event_closeActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    public String getToggleInfo()
    {
        int xBoxValue = (xBox.isSelected()) ? 1 : 0;
        int yBoxValue = (yBox.isSelected()) ? 1 : 0;
        int zBoxValue = (zBox.isSelected()) ? 1 : 0;
        int xRotBoxValue = (xRotBox.isSelected()) ? 1 : 0;
        int yRotBoxValue = (yRotBox.isSelected()) ? 1 : 0;
        int zRotBoxValue = (zRotBox.isSelected()) ? 1 : 0;
        return (String.valueOf(xBoxValue) + " " +
                String.valueOf(yBoxValue) + " " +
                String.valueOf(zBoxValue) + " " +
                String.valueOf(xRotBoxValue) + " " +
                String.valueOf(yRotBoxValue) + " " +
                String.valueOf(zRotBoxValue));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox auto;
    private javax.swing.JLabel capacitanceLabel;
    private javax.swing.JButton close;
    private javax.swing.JButton connect;
    private javax.swing.JButton demo;
    private javax.swing.JButton enter;
    private javax.swing.JPanel otherPanel;
    private javax.swing.JPanel positionPanel;
    private javax.swing.JLabel radiusLabel;
    private javax.swing.JPanel rotationPanel;
    private javax.swing.JSeparator separator;
    private javax.swing.JSeparator separator1;
    private javax.swing.JSlider slider;
    private javax.swing.JCheckBox xBox;
    private javax.swing.JTextField xField;
    private javax.swing.JLabel xLabel;
    private javax.swing.JLabel xLabelUnit;
    private javax.swing.JCheckBox xRotBox;
    private javax.swing.JTextField xRotField;
    private javax.swing.JLabel xRotLabel;
    private javax.swing.JLabel xRotLabelUnit;
    private javax.swing.JTextField xRotPosField;
    private javax.swing.JLabel xRotPosLabel;
    private javax.swing.JLabel xRotPosLabelUnit;
    private javax.swing.JCheckBox yBox;
    private javax.swing.JTextField yField;
    private javax.swing.JLabel yLabel;
    private javax.swing.JLabel yLabelUnit;
    private javax.swing.JCheckBox yRotBox;
    private javax.swing.JTextField yRotField;
    private javax.swing.JLabel yRotLabel;
    private javax.swing.JLabel yRotLabelUnit;
    private javax.swing.JTextField yRotPosField;
    private javax.swing.JLabel yRotPosLabel;
    private javax.swing.JLabel yRotPosLabelUnit;
    private javax.swing.JCheckBox zBox;
    private javax.swing.JTextField zField;
    private javax.swing.JLabel zLabel;
    private javax.swing.JLabel zLabelUnit;
    private javax.swing.JCheckBox zRotBox;
    private javax.swing.JTextField zRotField;
    private javax.swing.JLabel zRotLabel;
    private javax.swing.JLabel zRotLabelUnit;
    private javax.swing.JTextField zRotPosField;
    private javax.swing.JLabel zRotPosLabel;
    private javax.swing.JLabel zRotPosLabelUnit;
    // End of variables declaration//GEN-END:variables

}
