/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SpiceTab.java
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
package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.io.output.Verilog;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.dialogs.OpenFile;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Class to handle the "Spice" tab of the Preferences dialog.
 */
public class SpiceTab extends PreferencePanel
{
	/** Creates new form SpiceTab */
	public SpiceTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return spice; }

	/** return the name of this preferences tab. */
	public String getName() { return "Spice"; }

	private JList spiceCellList;
	private DefaultListModel spiceCellListModel;
	private HashMap spiceCellModelOptions;
	private HashMap initialSpiceModelFiles;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Spice tab.
	 */
	public void init()
	{
		// the top section: general controls
		spiceEnginePopup.addItem("Spice 2");
		spiceEnginePopup.addItem("Spice 3");
		spiceEnginePopup.addItem("HSpice");
		spiceEnginePopup.addItem("PSpice");
		spiceEnginePopup.addItem("Gnucap");
		spiceEnginePopup.addItem("SmartSpice");
		spiceEnginePopup.setSelectedIndex(Simulation.getSpiceEngine());

		spiceLevelPopup.addItem("1");
		spiceLevelPopup.addItem("2");
		spiceLevelPopup.addItem("3");
		spiceLevelPopup.setSelectedItem(Simulation.getSpiceLevel());

		spiceOutputFormatPopup.addItem("Standard");
		spiceOutputFormatPopup.addItem("Raw");
		spiceOutputFormatPopup.addItem("Raw/Smart");
        spiceOutputFormatPopup.addItem("Epic");
		spiceOutputFormatPopup.setSelectedItem(Simulation.getSpiceOutputFormat());

		spiceUseParasitics.setSelected(Simulation.isSpiceUseParasitics());
		spiceUseNodeNames.setSelected(Simulation.isSpiceUseNodeNames());
		spiceForceGlobalPwrGnd.setSelected(Simulation.isSpiceForceGlobalPwrGnd());
		spiceUseCellParameters.setSelected(Simulation.isSpiceUseCellParameters());
		spiceWriteTransSizesInLambda.setSelected(Simulation.isSpiceWriteTransSizeInLambda());

        // spice Run options
        useDir.setText(Simulation.getSpiceRunDir());
        useDirCheckBox.setSelected(Simulation.getSpiceUseRunDir());
        overwriteOutputFile.setSelected(Simulation.getSpiceOutputOverwrite());
        spiceRunProbe.setSelected(Simulation.getSpiceRunProbe());
        spiceRunProgram.setText(Simulation.getSpiceRunProgram());
        spiceRunProgramArgs.setText(Simulation.getSpiceRunProgramArgs());

		String [] libFiles = LibFile.getSpicePartsLibraries();
		for(int i=0; i<libFiles.length; i++)
			spicePrimitivesetPopup.addItem(libFiles[i]);
		spicePrimitivesetPopup.setSelectedItem(Simulation.getSpicePartsLibrary());

        String [] runChoices = Simulation.getSpiceRunChoiceValues();
        for (int i=0; i<runChoices.length; i++) {
            spiceRunPopup.addItem(runChoices[i]);
        }
        spiceRunPopup.setSelectedItem(Simulation.getSpiceRunChoice());
        if (spiceRunPopup.getSelectedIndex() == 0) setSpiceRunOptionsEnabled(false);
        else setSpiceRunOptionsEnabled(true);

		// the next section: header and trailer cards
		String spiceHeaderCardInitial = Simulation.getSpiceHeaderCardInfo();
		if (spiceHeaderCardInitial.length() == 0) spiceNoHeaderCards.setSelected(true); else
		{
			if (spiceHeaderCardInitial.startsWith(Spice.SPICE_EXTENSION_PREFIX))
			{
				spiceHeaderCardsWithExtension.setSelected(true);
				spiceHeaderCardExtension.setText(spiceHeaderCardInitial.substring(Spice.SPICE_EXTENSION_PREFIX.length()));
			} else
			{
				spiceHeaderCardsFromFile.setSelected(true);
				spiceHeaderCardFile.setText(spiceHeaderCardInitial);
			}
		}
		String spiceTrailerCardInitial = Simulation.getSpiceTrailerCardInfo();
		if (spiceTrailerCardInitial.length() == 0) spiceNoTrailerCards.setSelected(true); else
		{
			if (spiceTrailerCardInitial.startsWith(Spice.SPICE_EXTENSION_PREFIX))
			{
				spiceTrailerCardsWithExtension.setSelected(true);
				spiceTrailerCardExtension.setText(spiceTrailerCardInitial.substring(Spice.SPICE_EXTENSION_PREFIX.length()));
			} else
			{
				spiceTrailerCardsFromFile.setSelected(true);
				spiceTrailerCardFile.setText(spiceTrailerCardInitial);
			}
		}
		spiceBrowseHeaderFile.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { spiceBrowseHeaderFileActionPerformed(); }
		});
		spiceBrowseTrailerFile.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { spiceBrowseTrailerFileActionPerformed(); }
		});

		// the last section has cell overrides
		// gather all existing behave file information
		initialSpiceModelFiles = new HashMap();
		for(Iterator lIt = Library.getLibraries(); lIt.hasNext(); )
		{
			Library lib = (Library)lIt.next();
			if (lib.isHidden()) continue;
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				String behaveFile = "";
				Variable var = cell.getVar(Spice.SPICE_MODEL_FILE_KEY);
				if (var != null) behaveFile = var.getObject().toString();
				initialSpiceModelFiles.put(cell, Pref.makeStringPref(null, null, behaveFile));
			}
		}

		// make list of libraries
		spiceCellModelOptions = new HashMap();
		for(Iterator it = Library.getVisibleLibraries().iterator(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			spiceModelLibrary.addItem(lib.getName());

			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				String modelFile = "";
				Variable var = cell.getVar(Spice.SPICE_MODEL_FILE_KEY, String.class);
				if (var != null) modelFile = (String)var.getObject();
				spiceCellModelOptions.put(cell, Pref.makeStringPref(null, null, modelFile));
			}
		}
		spiceModelLibrary.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { spiceLoadCellList(); }
		});

		spiceCellListModel = new DefaultListModel();
		spiceCellList = new JList(spiceCellListModel);
		spiceCellList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		spiceCell.setViewportView(spiceCellList);
		spiceCellList.setSelectedIndex(0);
		spiceCellList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { spiceCellListClick(); }
		});
		spiceModelFileBrowse.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { spiceModelFileBrowseActionPerformed(); }
		});
		spiceDeriveModelFromCircuit.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { spiceCellModelButtonClick(); }
		});
		spiceUseModelFromFile.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { spiceCellModelButtonClick(); }
		});
		spiceModelCell.getDocument().addDocumentListener(new SpiceModelDocumentListener(this));
		spiceLoadCellList();
	}

	/**
	 * Method called when the library selection changes in the bottom (for cell models).
	 */
	private void spiceLoadCellList()
	{
		String libName = (String)spiceModelLibrary.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		if (lib == null) return;
		spiceCellListModel.clear();
		boolean notEmpty = false;
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			spiceCellListModel.addElement(cell.noLibDescribe());
			notEmpty = true;
		}
		if (notEmpty)
		{
			spiceCellList.setSelectedIndex(0);
			spiceCellListClick();
		}
	}

	private boolean spiceModelFileChanging = false;

	/**
	 * Method called when the user clicks on a model file radio button at in the bottom of the Spice Options dialog.
	 */
	private void spiceCellModelButtonClick()
	{
		spiceModelCell.setEditable(spiceUseModelFromFile.isSelected());
		spiceModelFileChanged();
//		if (spiceDeriveModelFromCircuit.isSelected()) spiceModelCell.setText("");
	}

	/**
	 * Method called when the user clicks on a cell name at in the bottom of the Spice Options dialog.
	 */
	private void spiceCellListClick()
	{
		if (spiceCellListModel.size() == 0) return;

		String libName = (String)spiceModelLibrary.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		if (lib == null) return;

		String cellName = (String)spiceCellList.getSelectedValue();
		Cell cell = lib.findNodeProto(cellName);
		if (cell == null) return;

		Pref pref = (Pref)spiceCellModelOptions.get(cell);
		String modelFile = pref.getString();
		spiceModelFileChanging = true;
		boolean hasModelFile = false;
		if (modelFile.length() > 0 && !modelFile.startsWith("-----"))
		{
			hasModelFile = true;
			spiceModelCell.setText(modelFile);
		}
		if (modelFile.startsWith("-----"))
		{
			spiceModelCell.setText(modelFile.substring(5));
		}
		if (hasModelFile) spiceUseModelFromFile.setSelected(true); else
			spiceDeriveModelFromCircuit.setSelected(true);
		spiceModelCell.setEditable(hasModelFile);
		spiceModelFileChanging = false;
	}

	/**
	 * Method called when the user clicks on the "Browse" button in the bottom of the Spice Options dialog.
	 */
	private void spiceModelFileBrowseActionPerformed()
	{
		String fileName = OpenFile.chooseInputFile(FileType.ANY, null);
		if (fileName == null) return;
		spiceUseModelFromFile.setSelected(true);
		spiceModelCell.setText(fileName);
		spiceModelCell.setEditable(true);
		spiceModelFileChanged();
	}

	/**
	 * Method called when the user changes the model file name at the bottom of the Spice Options dialog.
	 */
	private void spiceModelFileChanged()
	{
		if (spiceModelFileChanging) return;

		String libName = (String)spiceModelLibrary.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		if (lib == null) return;

		String cellName = (String)spiceCellList.getSelectedValue();
		Cell cell = lib.findNodeProto(cellName);
		if (cell == null) return;

		Pref pref = (Pref)spiceCellModelOptions.get(cell);
		String typedString = spiceModelCell.getText();
		if (spiceDeriveModelFromCircuit.isSelected()) typedString = "-----" + typedString;
		pref.setString(typedString);
	}

	/**
	 * Class to handle changes to per-cell model file names.
	 */
	private static class SpiceModelDocumentListener implements DocumentListener
	{
		SpiceTab dialog;

		SpiceModelDocumentListener(SpiceTab dialog)
		{
			this.dialog = dialog;
		}

		public void changedUpdate(DocumentEvent e) { dialog.spiceModelFileChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.spiceModelFileChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.spiceModelFileChanged(); }
	}

	private void spiceBrowseTrailerFileActionPerformed()
	{
		String fileName = OpenFile.chooseInputFile(FileType.ANY, null);
		if (fileName == null) return;
		spiceTrailerCardFile.setText(fileName);
		spiceTrailerCardsFromFile.setSelected(true);
	}

	private void spiceBrowseHeaderFileActionPerformed()
	{
		String fileName = OpenFile.chooseInputFile(FileType.ANY, null);
		if (fileName == null) return;
		spiceHeaderCardFile.setText(fileName);
		spiceHeaderCardsFromFile.setSelected(true);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Spice tab.
	 */
	public void term()
	{
		// the top section: general controls
		int intNow = spiceEnginePopup.getSelectedIndex();
		if (Simulation.getSpiceEngine() != intNow) Simulation.setSpiceEngine(intNow);

		String stringNow = (String)spiceLevelPopup.getSelectedItem();
		if (!Simulation.getSpiceLevel().equals(stringNow)) Simulation.setSpiceLevel(stringNow);

		stringNow = (String)spiceOutputFormatPopup.getSelectedItem();
		if (!Simulation.getSpiceOutputFormat().equals(stringNow)) Simulation.setSpiceOutputFormat(stringNow);

		stringNow = (String)spicePrimitivesetPopup.getSelectedItem();
		if (!Simulation.getSpicePartsLibrary().equals(stringNow)) Simulation.setSpicePartsLibrary(stringNow);

		boolean booleanNow = spiceUseNodeNames.isSelected();
		if (Simulation.isSpiceUseNodeNames() != booleanNow) Simulation.setSpiceUseNodeNames(booleanNow);

		booleanNow = spiceForceGlobalPwrGnd.isSelected();
		if (Simulation.isSpiceForceGlobalPwrGnd() != booleanNow) Simulation.setSpiceForceGlobalPwrGnd(booleanNow);

		booleanNow = spiceUseCellParameters.isSelected();
		if (Simulation.isSpiceUseCellParameters() != booleanNow) Simulation.setSpiceUseCellParameters(booleanNow);

		booleanNow = spiceWriteTransSizesInLambda.isSelected();
		if (Simulation.isSpiceWriteTransSizeInLambda() != booleanNow) Simulation.setSpiceWriteTransSizeInLambda(booleanNow);

		booleanNow = spiceUseParasitics.isSelected();
		if (Simulation.isSpiceUseParasitics() != booleanNow) Simulation.setSpiceUseParasitics(booleanNow);

        // spice run options
        stringNow = (String)spiceRunPopup.getSelectedItem();
        if (!Simulation.getSpiceRunChoice().equals(stringNow)) Simulation.setSpiceRunChoice(stringNow);

        stringNow = useDir.getText();
        if (!Simulation.getSpiceRunDir().equals(stringNow)) Simulation.setSpiceRunDir(stringNow);

        booleanNow = useDirCheckBox.isSelected();
        if (Simulation.getSpiceUseRunDir() != booleanNow) Simulation.setSpiceUseRunDir(booleanNow);

        booleanNow = overwriteOutputFile.isSelected();
        if (Simulation.getSpiceOutputOverwrite() != booleanNow) Simulation.setSpiceOutputOverwrite(booleanNow);

        booleanNow = spiceRunProbe.isSelected();
        if (Simulation.getSpiceRunProbe() != booleanNow) Simulation.setSpiceRunProbe(booleanNow);

        stringNow = spiceRunProgram.getText();
        if (!Simulation.getSpiceRunProgram().equals(stringNow)) Simulation.setSpiceRunProgram(stringNow);

        stringNow = spiceRunProgramArgs.getText();
        if (!Simulation.getSpiceRunProgramArgs().equals(stringNow)) Simulation.setSpiceRunProgramArgs(stringNow);

		// the next section: header and trailer cards
		String header = "";
		if (spiceHeaderCardsWithExtension.isSelected())
		{
			header = Spice.SPICE_EXTENSION_PREFIX + spiceHeaderCardExtension.getText();
		} else if (spiceHeaderCardsFromFile.isSelected())
		{
			header = spiceHeaderCardFile.getText();
		}
		if (!Simulation.getSpiceTrailerCardInfo().equals(header)) Simulation.setSpiceHeaderCardInfo(header);

		String trailer = "";
		if (spiceTrailerCardsWithExtension.isSelected())
		{
			trailer = Spice.SPICE_EXTENSION_PREFIX + spiceTrailerCardExtension.getText();
		} else if (spiceTrailerCardsFromFile.isSelected())
		{
			trailer = spiceTrailerCardFile.getText();
		}
		if (!Simulation.getSpiceHeaderCardInfo().equals(trailer)) Simulation.setSpiceTrailerCardInfo(trailer);

		// bottom section: model file overrides for cells
		for(Iterator lIt = Library.getVisibleLibraries().iterator(); lIt.hasNext(); )
		{
			Library lib = (Library)lIt.next();
			for(Iterator it = lib.getCells(); it.hasNext(); )
			{
				Cell cell = (Cell)it.next();
				Pref pref = (Pref)spiceCellModelOptions.get(cell);
				if (pref == null) continue;
				if (!pref.getStringFactoryValue().equals(pref.getString()))
				{
					String fileName = pref.getString().trim();
					if (fileName.length() == 0) cell.delVar(Spice.SPICE_MODEL_FILE_KEY); else
						cell.newVar(Spice.SPICE_MODEL_FILE_KEY, fileName);
				}
			}
		}
	}

    // enable or disable the spice run options
    private void setSpiceRunOptionsEnabled(boolean enabled) {
        useDirCheckBox.setEnabled(enabled);
        overwriteOutputFile.setEnabled(enabled);
        spiceRunProgram.setEnabled(enabled);
        spiceRunProgramArgs.setEnabled(enabled);
        spiceRunHelp.setEnabled(enabled);
        spiceRunProbe.setEnabled(enabled);
        if (enabled) {
            spiceRunProgram.setBackground(Color.white);
            if (!useDirCheckBox.isSelected()) {
                // if no use dir specified, disable text box
                useDir.setEnabled(false);
                useDir.setBackground(spice2.getBackground());
            } else {
                useDir.setEnabled(true);
                useDir.setBackground(Color.white);
            }
        } else {
            spiceRunProgram.setBackground(spice2.getBackground());
            useDir.setBackground(spice2.getBackground());
            useDir.setEnabled(false);
        }
    }

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        spiceHeader = new javax.swing.ButtonGroup();
        spiceTrailer = new javax.swing.ButtonGroup();
        spiceModel = new javax.swing.ButtonGroup();
        spice = new javax.swing.JPanel();
        spice1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        spiceRunPopup = new javax.swing.JComboBox();
        spiceEnginePopup = new javax.swing.JComboBox();
        spiceLevelPopup = new javax.swing.JComboBox();
        spiceOutputFormatPopup = new javax.swing.JComboBox();
        spiceUseParasitics = new javax.swing.JCheckBox();
        spiceUseNodeNames = new javax.swing.JCheckBox();
        spiceForceGlobalPwrGnd = new javax.swing.JCheckBox();
        spiceUseCellParameters = new javax.swing.JCheckBox();
        spiceWriteTransSizesInLambda = new javax.swing.JCheckBox();
        spice2 = new javax.swing.JPanel();
        spiceRunProgram = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        useDirCheckBox = new javax.swing.JCheckBox();
        useDir = new javax.swing.JTextField();
        overwriteOutputFile = new javax.swing.JCheckBox();
        spiceRunHelp = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        spiceRunProgramArgs = new javax.swing.JTextField();
        spiceRunProbe = new javax.swing.JCheckBox();
        spice3 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        spicePrimitivesetPopup = new javax.swing.JComboBox();
        jSeparator1 = new javax.swing.JSeparator();
        spice5 = new javax.swing.JPanel();
        spiceHeaderCardExtension = new javax.swing.JTextField();
        spiceNoHeaderCards = new javax.swing.JRadioButton();
        spiceHeaderCardsWithExtension = new javax.swing.JRadioButton();
        spiceHeaderCardsFromFile = new javax.swing.JRadioButton();
        spiceNoTrailerCards = new javax.swing.JRadioButton();
        spiceTrailerCardsWithExtension = new javax.swing.JRadioButton();
        spiceTrailerCardsFromFile = new javax.swing.JRadioButton();
        spiceHeaderCardFile = new javax.swing.JTextField();
        spiceBrowseHeaderFile = new javax.swing.JButton();
        spiceTrailerCardExtension = new javax.swing.JTextField();
        spiceTrailerCardFile = new javax.swing.JTextField();
        spiceBrowseTrailerFile = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JSeparator();
        jSeparator3 = new javax.swing.JSeparator();
        spice6 = new javax.swing.JPanel();
        spiceCell = new javax.swing.JScrollPane();
        jLabel8 = new javax.swing.JLabel();
        spiceDeriveModelFromCircuit = new javax.swing.JRadioButton();
        spiceUseModelFromFile = new javax.swing.JRadioButton();
        spiceModelFileBrowse = new javax.swing.JButton();
        spiceModelCell = new javax.swing.JTextField();
        spiceModelLibrary = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Spice Preferences");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        spice.setLayout(new java.awt.GridBagLayout());

        spice.setToolTipText("Options for Spice deck generation");
        spice1.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("Spice Engine:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 0.5;
        spice1.add(jLabel1, gridBagConstraints);

        jLabel9.setText("Spice Level:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(jLabel9, gridBagConstraints);

        jLabel10.setText("Output format:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(jLabel10, gridBagConstraints);

        spiceRunPopup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spiceRunPopupActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        spice1.add(spiceRunPopup, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        spice1.add(spiceEnginePopup, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        spice1.add(spiceLevelPopup, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        spice1.add(spiceOutputFormatPopup, gridBagConstraints);

        spiceUseParasitics.setText("Use Parasitics");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(spiceUseParasitics, gridBagConstraints);

        spiceUseNodeNames.setText("Use Node Names");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(spiceUseNodeNames, gridBagConstraints);

        spiceForceGlobalPwrGnd.setText("Force Global VDD/GND");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(spiceForceGlobalPwrGnd, gridBagConstraints);

        spiceUseCellParameters.setText("Use Cell Parameters");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(spiceUseCellParameters, gridBagConstraints);

        spiceWriteTransSizesInLambda.setText("Write Trans Sizes in Units");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(spiceWriteTransSizesInLambda, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        spice.add(spice1, gridBagConstraints);

        spice2.setLayout(new java.awt.GridBagLayout());

        spice2.setBorder(new javax.swing.border.EtchedBorder());
        spiceRunProgram.setColumns(8);
        spiceRunProgram.setMinimumSize(new java.awt.Dimension(100, 20));
        spiceRunProgram.setPreferredSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.2;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        spice2.add(spiceRunProgram, gridBagConstraints);

        jLabel17.setText("Run Program:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        spice2.add(jLabel17, gridBagConstraints);

        useDirCheckBox.setText("Use Dir:");
        useDirCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                useDirCheckBoxActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        spice2.add(useDirCheckBox, gridBagConstraints);

        useDir.setColumns(8);
        useDir.setText("jTextField1");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        spice2.add(useDir, gridBagConstraints);

        overwriteOutputFile.setText("Overwrite existing output file (no prompts)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        spice2.add(overwriteOutputFile, gridBagConstraints);

        spiceRunHelp.setText("help");
        spiceRunHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spiceRunHelpActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        spice2.add(spiceRunHelp, gridBagConstraints);

        jLabel3.setText("with args:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        spice2.add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        spice2.add(spiceRunProgramArgs, gridBagConstraints);

        spiceRunProbe.setText("Run probe");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        spice2.add(spiceRunProbe, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        spice.add(spice2, gridBagConstraints);

        spice3.setLayout(new java.awt.GridBagLayout());

        jLabel13.setText("Spice primitive set:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 0.5;
        spice3.add(jLabel13, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        spice3.add(spicePrimitivesetPopup, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        spice.add(spice3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        spice.add(jSeparator1, gridBagConstraints);

        spice5.setLayout(new java.awt.GridBagLayout());

        spiceHeaderCardExtension.setColumns(5);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        spice5.add(spiceHeaderCardExtension, gridBagConstraints);

        spiceHeader.add(spiceNoHeaderCards);
        spiceNoHeaderCards.setText("No Header Cards");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(spiceNoHeaderCards, gridBagConstraints);

        spiceHeader.add(spiceHeaderCardsWithExtension);
        spiceHeaderCardsWithExtension.setText("Use Header Cards with extension:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(spiceHeaderCardsWithExtension, gridBagConstraints);

        spiceHeader.add(spiceHeaderCardsFromFile);
        spiceHeaderCardsFromFile.setText("Use Header Cards from File:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(spiceHeaderCardsFromFile, gridBagConstraints);

        spiceTrailer.add(spiceNoTrailerCards);
        spiceNoTrailerCards.setText("No Trailer Cards");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(spiceNoTrailerCards, gridBagConstraints);

        spiceTrailer.add(spiceTrailerCardsWithExtension);
        spiceTrailerCardsWithExtension.setText("Use Trailer Cards with extension:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(spiceTrailerCardsWithExtension, gridBagConstraints);

        spiceTrailer.add(spiceTrailerCardsFromFile);
        spiceTrailerCardsFromFile.setText("Use Trailer Cards from File:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(spiceTrailerCardsFromFile, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        spice5.add(spiceHeaderCardFile, gridBagConstraints);

        spiceBrowseHeaderFile.setText("Browse");
        spiceBrowseHeaderFile.setMinimumSize(new java.awt.Dimension(78, 20));
        spiceBrowseHeaderFile.setPreferredSize(new java.awt.Dimension(78, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        spice5.add(spiceBrowseHeaderFile, gridBagConstraints);

        spiceTrailerCardExtension.setColumns(5);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        spice5.add(spiceTrailerCardExtension, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        spice5.add(spiceTrailerCardFile, gridBagConstraints);

        spiceBrowseTrailerFile.setText("Browse");
        spiceBrowseTrailerFile.setMinimumSize(new java.awt.Dimension(78, 20));
        spiceBrowseTrailerFile.setPreferredSize(new java.awt.Dimension(78, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        spice5.add(spiceBrowseTrailerFile, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        spice5.add(jSeparator4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        spice.add(spice5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        spice.add(jSeparator3, gridBagConstraints);

        spice6.setLayout(new java.awt.GridBagLayout());

        spiceCell.setMinimumSize(new java.awt.Dimension(150, 100));
        spiceCell.setPreferredSize(new java.awt.Dimension(150, 100));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        spice6.add(spiceCell, gridBagConstraints);

        jLabel8.setText("Cell:");
        jLabel8.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        spice6.add(jLabel8, gridBagConstraints);

        spiceModel.add(spiceDeriveModelFromCircuit);
        spiceDeriveModelFromCircuit.setText("Derive Cell Model from Circuitry");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice6.add(spiceDeriveModelFromCircuit, gridBagConstraints);

        spiceModel.add(spiceUseModelFromFile);
        spiceUseModelFromFile.setText("Use Cell Model from File:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice6.add(spiceUseModelFromFile, gridBagConstraints);

        spiceModelFileBrowse.setText("Browse");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        spice6.add(spiceModelFileBrowse, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        spice6.add(spiceModelCell, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        spice6.add(spiceModelLibrary, gridBagConstraints);

        jLabel2.setText("Library:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        spice6.add(jLabel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        spice.add(spice6, gridBagConstraints);

        getContentPane().add(spice, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents

    private void spiceRunPopupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spiceRunPopupActionPerformed
        if (spiceRunPopup.getSelectedIndex() == 0) {
            setSpiceRunOptionsEnabled(false);
        } else
            setSpiceRunOptionsEnabled(true);
    }//GEN-LAST:event_spiceRunPopupActionPerformed

    private void spiceRunHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spiceRunHelpActionPerformed
        String [] message = { "IMPORTANT: This executes a single program with the given args.  It does NOT run a command-line command.",
                              "For example, 'echo blah > file' will NOT work. Encapsulate it in a script if you want to do such things.",
                              "-----------------",
                              "The following variables are available to use in the program name and arguments:",
                              "   ${WORKING_DIR}:  The current working directory",
                              "   ${USE_DIR}:  The Use Dir field, if specified (otherwise defaults to WORKING_DIR)",
                              "   ${FILENAME}:  The output file name (with extension)",
                              "   ${FILENAME_NO_EXT}:  The output file name (without extension)",
                              "Example: Program: \"hspice\".  Args: \"${FILENAME}\"" };
        JOptionPane.showMessageDialog(this, message, "Spice Run Help", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_spiceRunHelpActionPerformed

    private void useDirCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_useDirCheckBoxActionPerformed
        // enable use dir field
        boolean b = useDirCheckBox.isSelected();
        useDir.setEnabled(b);
        if (b) {
            useDir.setBackground(Color.white);
        } else {
            useDir.setBackground(spice2.getBackground());
        }
    }//GEN-LAST:event_useDirCheckBoxActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JCheckBox overwriteOutputFile;
    private javax.swing.JPanel spice;
    private javax.swing.JPanel spice1;
    private javax.swing.JPanel spice2;
    private javax.swing.JPanel spice3;
    private javax.swing.JPanel spice5;
    private javax.swing.JPanel spice6;
    private javax.swing.JButton spiceBrowseHeaderFile;
    private javax.swing.JButton spiceBrowseTrailerFile;
    private javax.swing.JScrollPane spiceCell;
    private javax.swing.JRadioButton spiceDeriveModelFromCircuit;
    private javax.swing.JComboBox spiceEnginePopup;
    private javax.swing.JCheckBox spiceForceGlobalPwrGnd;
    private javax.swing.ButtonGroup spiceHeader;
    private javax.swing.JTextField spiceHeaderCardExtension;
    private javax.swing.JTextField spiceHeaderCardFile;
    private javax.swing.JRadioButton spiceHeaderCardsFromFile;
    private javax.swing.JRadioButton spiceHeaderCardsWithExtension;
    private javax.swing.JComboBox spiceLevelPopup;
    private javax.swing.ButtonGroup spiceModel;
    private javax.swing.JTextField spiceModelCell;
    private javax.swing.JButton spiceModelFileBrowse;
    private javax.swing.JComboBox spiceModelLibrary;
    private javax.swing.JRadioButton spiceNoHeaderCards;
    private javax.swing.JRadioButton spiceNoTrailerCards;
    private javax.swing.JComboBox spiceOutputFormatPopup;
    private javax.swing.JComboBox spicePrimitivesetPopup;
    private javax.swing.JButton spiceRunHelp;
    private javax.swing.JComboBox spiceRunPopup;
    private javax.swing.JCheckBox spiceRunProbe;
    private javax.swing.JTextField spiceRunProgram;
    private javax.swing.JTextField spiceRunProgramArgs;
    private javax.swing.ButtonGroup spiceTrailer;
    private javax.swing.JTextField spiceTrailerCardExtension;
    private javax.swing.JTextField spiceTrailerCardFile;
    private javax.swing.JRadioButton spiceTrailerCardsFromFile;
    private javax.swing.JRadioButton spiceTrailerCardsWithExtension;
    private javax.swing.JCheckBox spiceUseCellParameters;
    private javax.swing.JRadioButton spiceUseModelFromFile;
    private javax.swing.JCheckBox spiceUseNodeNames;
    private javax.swing.JCheckBox spiceUseParasitics;
    private javax.swing.JCheckBox spiceWriteTransSizesInLambda;
    private javax.swing.JTextField useDir;
    private javax.swing.JCheckBox useDirCheckBox;
    // End of variables declaration//GEN-END:variables

}
