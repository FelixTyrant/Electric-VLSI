/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WaveformWindow.java
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
package com.sun.electric.tool.user.ui;

import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.input.Simulate;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.User;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * This class defines the a screenful of Panels that make up a waveform display.
 */
public class WaveformWindow implements WindowContent, HighlightListener
{
	private static int panelSizeDigital = 30;
	private static int panelSizeAnalog = 150;
	private static Color [] colorArray = new Color [] {
		Color.RED, Color.GREEN, Color.BLUE, Color.PINK, Color.CYAN, Color.ORANGE, Color.MAGENTA, Color.YELLOW};

	/** the window that this lives in */					private WindowFrame wf;
	/** the cell being simulated */							private Simulation.SimData sd;
	/** the top-level panel of the waveform window. */		private JPanel overall;
	/** let panel: the signal names */						private JPanel left;
	/** right panel: the signal traces */					private JPanel right;
	/** the "lock time" button. */							private JButton timeLock;
	/** the "refresh" button. */							private JButton refresh;
	/** the "grow panel" button for widening. */			private JButton growPanel;
	/** the "shrink panel" button for narrowing. */			private JButton shrinkPanel;
	/** the list of panels. */								private JComboBox signalNameList;
	/** true if rebuilding the list of panels */			private boolean rebuildingSignalNameList = false;
	/** the main scroll of all panels. */					private JScrollPane scrollAll;
	/** the split between signal names and traces. */		private JSplitPane split;
	/** labels for the text at the top */					private JLabel mainPos, extPos, delta;
	/** a list of panels in this window */					private List wavePanels;
	/** the time panel at the top of the wave window. */	private TimeTickPanel mainTimePanel;
	/** true to repaint the main time panel. */				private boolean mainTimePanelNeedsRepaint;
	/** the VCR timer, when running */						private Timer vcrTimer;
	/** true to run VCR backwards */						private boolean vcrPlayingBackwards = false;
	/** time the VCR last advanced */						private long vcrLastAdvance;
	/** speed of the VCR (in screen pixels) */				private int vcrAdvanceSpeed = 3;
	/** current "main" time cursor */						private double mainTime;
	/** current "extension" time cursor */					private double extTime;
	/** default range along horozintal axis */				private double minTime, maxTime;
	/** true if the time axis is the same in each panel */	private boolean timeLocked;
	/** the actual screen coordinates of the waveform */	private int screenLowX, screenHighX;
	/** true if click in waveform changes highlights */		private boolean highlightChangedByWaveform = false;
	/** Varible key for true library of fake cell. */		public static final Variable.Key WINDOW_SIGNAL_ORDER = ElectricObject.newKey("SIM_window_signalorder");

	private static WaveFormDropTarget waveformDropTarget = new WaveFormDropTarget();

	private static final ImageIcon iconAddPanel = Resources.getResource(WaveformWindow.class, "ButtonSimAddPanel.gif");
	private static final ImageIcon iconLockTime = Resources.getResource(WaveformWindow.class, "ButtonSimLockTime.gif");
	private static final ImageIcon iconUnLockTime = Resources.getResource(WaveformWindow.class, "ButtonSimUnLockTime.gif");
	private static final ImageIcon iconRefresh = Resources.getResource(WaveformWindow.class, "ButtonSimRefresh.gif");
	private static final ImageIcon iconGrowPanel = Resources.getResource(WaveformWindow.class, "ButtonSimGrow.gif");
	private static final ImageIcon iconShrinkPanel = Resources.getResource(WaveformWindow.class, "ButtonSimShrink.gif");
	private static final ImageIcon iconVCRRewind = Resources.getResource(WaveformWindow.class, "ButtonVCRRewind.gif");
	private static final ImageIcon iconVCRPlayBackward = Resources.getResource(WaveformWindow.class, "ButtonVCRPlayBackward.gif");
	private static final ImageIcon iconVCRStop = Resources.getResource(WaveformWindow.class, "ButtonVCRStop.gif");
	private static final ImageIcon iconVCRPlay = Resources.getResource(WaveformWindow.class, "ButtonVCRPlay.gif");
	private static final ImageIcon iconVCRToEnd = Resources.getResource(WaveformWindow.class, "ButtonVCRToEnd.gif");
	private static final ImageIcon iconVCRFaster = Resources.getResource(WaveformWindow.class, "ButtonVCRFaster.gif");
	private static final ImageIcon iconVCRSlower = Resources.getResource(WaveformWindow.class, "ButtonVCRSlower.gif");

	/**
	 * Test method to build a waveform with fake data.
	 */
	public static void makeFakeWaveformCommand()
	{
		// make the waveform data
		Simulation.SimData sd = new Simulation.SimData();
		double timeStep = 0.0000000001;
		sd.buildCommonTime(100);
		for(int i=0; i<100; i++)
			sd.setCommonTime(i, i * timeStep);
		for(int i=0; i<18; i++)
		{
			Simulation.SimAnalogSignal as = new Simulation.SimAnalogSignal(sd);
			as.setSignalName("Signal"+(i+1));
			as.setSignalColor(colorArray[i % colorArray.length]);
			as.setCommonTimeUse(true);
			as.buildValues(100);
			for(int k=0; k<100; k++)
			{
				as.setValue(k, Math.sin((k+i*10) / (2.0+i*2)) * 4);
			}
		}
		sd.setCell(null);

		// make the waveform window
		WindowFrame wf = WindowFrame.createWaveformWindow(sd);
		WaveformWindow ww = (WaveformWindow)wf.getContent();
		ww.setMainTimeCursor(timeStep*22);
		ww.setExtensionTimeCursor(timeStep*77);
		ww.setDefaultTimeRange(0, timeStep*100);

		// make some waveform panels and put signals in them
		for(int i=0; i<6; i++)
		{
			Panel wp = new Panel(ww, true);
			wp.setValueRange(-5, 5);
			for(int j=0; j<(i+1)*3; j++)
			{
				Simulation.SimAnalogSignal as = (Simulation.SimAnalogSignal)sd.getSignals().get(j);
				Signal wsig = new Signal(wp, as);
			}
		}
	}

	/**
	 * This class defines a single panel of Signals with an associated list of signal names.
	 */
	public static class Panel extends JPanel
		implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
	{
		/** the main waveform window this is part of */			private WaveformWindow waveWindow;
		/** maps signal buttons to the actual Signal */			private HashMap waveSignals;
		/** the list of signal name buttons on the left */		private JPanel signalButtons;
		/** the JScrollPane with of signal name buttons */		private JScrollPane signalButtonsPane;
		/** the left side: with signal names etc. */			private JPanel leftHalf;
		/** the right side: with signal traces */				private JPanel rightHalf;
		/** the button to close this panel. */					private JButton close;
		/** the button to hide this panel. */					private JButton hide;
		/** the button to delete selected signal (analog). */	private JButton deleteSignal;
		/** the button to delete all signals (analog). */		private JButton deleteAllSignals;
		/** the button to toggle bus display (digital). */		private JButton toggleBusSignals;
		/** the signal name button (digital). */				private JButton digitalSignalButton;
		/** displayed range along horozintal axis */			private double minTime, maxTime;
		/** low vertical axis for this trace (analog) */		private double analogLowValue;
		/** high vertical axis for this trace (analog) */		private double analogHighValue;
		/** vertical range for this trace (analog) */			private double analogRange;
		/** the size of the window (in pixels) */				private Dimension sz;
		/** true if a time cursor is being dragged */			private boolean draggingMain, draggingExt;
		/** true if an area is being dragged */					private boolean draggingArea;
		/** true if this waveform panel is selected */			private boolean selected;
		/** true if this waveform panel is hidden */			private boolean hidden;
		/** true if this waveform panel is analog */			private boolean isAnalog;
		/** the time panel at the top of this panel. */			private TimeTickPanel timePanel;
		/** the number of this panel. */						private int panelNumber;

		private int dragStartX, dragStartY;
		private int dragEndX, dragEndY;

		private static final int VERTLABELWIDTH = 60;
		private static Color background = null;
		private static int nextPanelNumber = 1;

		private static final ImageIcon iconHidePanel = Resources.getResource(WaveformWindow.class, "ButtonSimHide.gif");
		private static final ImageIcon iconClosePanel = Resources.getResource(WaveformWindow.class, "ButtonSimClose.gif");
		private static final ImageIcon iconDeleteSignal = Resources.getResource(WaveformWindow.class, "ButtonSimDelete.gif");
		private static final ImageIcon iconDeleteAllSignals = Resources.getResource(WaveformWindow.class, "ButtonSimDeleteAll.gif");
		private static final ImageIcon iconToggleBus = Resources.getResource(WaveformWindow.class, "ButtonSimToggleBus.gif");

	    // constructor
		public Panel(WaveformWindow waveWindow, boolean isAnalog)
		{
			// remember state
			this.waveWindow = waveWindow;
			this.isAnalog = isAnalog;
			this.selected = false;
			this.panelNumber = nextPanelNumber++;

			// setup this panel window
			int height = panelSizeDigital;
			if (isAnalog) height = panelSizeAnalog;
			sz = new Dimension(500, height);
			setSize(sz.width, sz.height);
			setPreferredSize(sz);
			setLayout(new FlowLayout());
			// add listeners --> BE SURE to remove listeners in finished()
			addKeyListener(this);
			addMouseListener(this);
			addMouseMotionListener(this);
			addMouseWheelListener(this);
			waveSignals = new HashMap();

			setTimeRange(waveWindow.minTime, waveWindow.maxTime);

			// the left side with signal names
			leftHalf = new OnePanel(this, waveWindow);
			leftHalf.setLayout(new GridBagLayout());

			// a drop target for the signal panel
			DropTarget dropTargetLeft = new DropTarget(leftHalf, DnDConstants.ACTION_LINK, waveformDropTarget, true);

			// a separator at the top
			JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;       gbc.gridy = 0;
			gbc.gridwidth = 5;   gbc.gridheight = 1;
			gbc.weightx = 1;     gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets(4, 0, 4, 0);
			leftHalf.add(sep, gbc);

			// the name of this panel
			if (isAnalog)
			{
				JLabel label = new JLabel(Integer.toString(panelNumber));
				label.setToolTipText("Identification number of this waveform panel");
				gbc.gridx = 0;       gbc.gridy = 1;
				gbc.gridwidth = 1;   gbc.gridheight = 1;
				gbc.weightx = 0.2;  gbc.weighty = 0;
				gbc.anchor = GridBagConstraints.NORTHWEST;
				gbc.fill = GridBagConstraints.NONE;
				gbc.insets = new Insets(4, 4, 4, 4);
				leftHalf.add(label, gbc);
			} else
			{
				digitalSignalButton = new JButton(Integer.toString(panelNumber));
				digitalSignalButton.setBorderPainted(false);
				digitalSignalButton.setToolTipText("Name of this waveform panel");
				gbc.gridx = 0;       gbc.gridy = 1;
				gbc.gridwidth = 1;   gbc.gridheight = 1;
				gbc.weightx = 1;     gbc.weighty = 1;
				gbc.anchor = GridBagConstraints.CENTER;
				gbc.fill = GridBagConstraints.NONE;
				gbc.insets = new Insets(0, 4, 0, 4);
				leftHalf.add(digitalSignalButton, gbc);
				digitalSignalButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent evt) { digitalSignalNameClicked(evt); }
				});
			}

			// the close button for this panel
			close = new JButton(iconClosePanel);
			close.setBorderPainted(false);
			close.setDefaultCapable(false);
			close.setToolTipText("Close this waveform panel");
			Dimension minWid = new Dimension(iconClosePanel.getIconWidth()+4, iconClosePanel.getIconHeight()+4);
			close.setMinimumSize(minWid);
			close.setPreferredSize(minWid);
			gbc.gridx = 1;       gbc.gridy = 1;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0.2;  gbc.weighty = 0;
			if (isAnalog) gbc.anchor = GridBagConstraints.NORTH; else
				gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.NONE;
			leftHalf.add(close, gbc);
			close.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { closePanel(); }
			});

			// the hide button for this panel
			hide = new JButton(iconHidePanel);
			hide.setBorderPainted(false);
			hide.setDefaultCapable(false);
			hide.setToolTipText("Hide this waveform panel");
			minWid = new Dimension(iconHidePanel.getIconWidth()+4, iconHidePanel.getIconHeight()+4);
			hide.setMinimumSize(minWid);
			hide.setPreferredSize(minWid);
			gbc.gridx = 2;       gbc.gridy = 1;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0.2;  gbc.weighty = 0;
			if (isAnalog) gbc.anchor = GridBagConstraints.NORTH; else
				gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.NONE;
			leftHalf.add(hide, gbc);
			hide.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { hidePanel(); }
			});
	
			if (isAnalog)
			{
				// the "delete signal" button for this panel
				deleteSignal = new JButton(iconDeleteSignal);
				deleteSignal.setBorderPainted(false);
				deleteSignal.setDefaultCapable(false);
				deleteSignal.setToolTipText("Remove selected signals from waveform panel");
				minWid = new Dimension(iconDeleteSignal.getIconWidth()+4, iconDeleteSignal.getIconHeight()+4);
				deleteSignal.setMinimumSize(minWid);
				deleteSignal.setPreferredSize(minWid);
				gbc.gridx = 3;       gbc.gridy = 1;
				gbc.gridwidth = 1;   gbc.gridheight = 1;
				gbc.weightx = 0.2;  gbc.weighty = 0;
				gbc.anchor = GridBagConstraints.NORTH;
				gbc.fill = GridBagConstraints.NONE;
				leftHalf.add(deleteSignal, gbc);
				deleteSignal.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent evt) { deleteSignalFromPanel(); }
				});

				// the "delete all signal" button for this panel
				deleteAllSignals = new JButton(iconDeleteAllSignals);
				deleteAllSignals.setBorderPainted(false);
				deleteAllSignals.setDefaultCapable(false);
				deleteAllSignals.setToolTipText("Remove all signals from waveform panel");
				minWid = new Dimension(iconDeleteAllSignals.getIconWidth()+4, iconDeleteAllSignals.getIconHeight()+4);
				deleteAllSignals.setMinimumSize(minWid);
				deleteAllSignals.setPreferredSize(minWid);
				gbc.gridx = 4;       gbc.gridy = 1;
				gbc.gridwidth = 1;   gbc.gridheight = 1;
				gbc.weightx = 0.2;  gbc.weighty = 0;
				gbc.anchor = GridBagConstraints.NORTH;
				gbc.fill = GridBagConstraints.NONE;
				leftHalf.add(deleteAllSignals, gbc);
				deleteAllSignals.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent evt) { deleteAllSignalsFromPanel(); }
				});
			} else
			{
				// the "toggle bus" button for this panel
				toggleBusSignals = new JButton(iconToggleBus);
				toggleBusSignals.setBorderPainted(false);
				toggleBusSignals.setDefaultCapable(false);
				toggleBusSignals.setToolTipText("View or hide the individual signals on this bus");
				minWid = new Dimension(iconToggleBus.getIconWidth()+4, iconToggleBus.getIconHeight()+4);
				toggleBusSignals.setMinimumSize(minWid);
				toggleBusSignals.setPreferredSize(minWid);
				gbc.gridx = 3;       gbc.gridy = 1;
				gbc.gridwidth = 1;   gbc.gridheight = 1;
				gbc.weightx = 0.2;  gbc.weighty = 0;
				gbc.anchor = GridBagConstraints.CENTER;
				gbc.fill = GridBagConstraints.NONE;
				leftHalf.add(toggleBusSignals, gbc);
				toggleBusSignals.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent evt) { toggleBusContents(); }
				});
			}

			// the list of signals in this panel (analog only)
			if (isAnalog)
			{
				signalButtons = new JPanel();
				signalButtons.setLayout(new BoxLayout(signalButtons, BoxLayout.Y_AXIS));
				signalButtonsPane = new JScrollPane(signalButtons);
				signalButtonsPane.setPreferredSize(new Dimension(100, height));
				gbc.gridx = 0;       gbc.gridy = 2;
				gbc.gridwidth = 5;   gbc.gridheight = 1;
				gbc.weightx = 1;     gbc.weighty = 1;
				gbc.anchor = GridBagConstraints.CENTER;
				gbc.fill = GridBagConstraints.BOTH;
				gbc.insets = new Insets(0, 0, 0, 0);
				leftHalf.add(signalButtonsPane, gbc);
			}

			// the right side with signal traces
			rightHalf = new OnePanel(this, waveWindow);
			rightHalf.setLayout(new GridBagLayout());

			// a drop target for the signal panel
			DropTarget dropTargetRight = new DropTarget(rightHalf, DnDConstants.ACTION_LINK, waveformDropTarget, true);

			// a separator at the top
			sep = new JSeparator(SwingConstants.HORIZONTAL);
			gbc.gridx = 0;       gbc.gridy = 0;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 1;     gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new java.awt.Insets(4, 0, 4, 0);
			rightHalf.add(sep, gbc);

			// the time tick panel (if separate time in each panel)
			if (!waveWindow.timeLocked)
				addTimePanel();

			// the waveform display for this panel
			gbc.gridx = 0;       gbc.gridy = 2;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 1;     gbc.weighty = 1;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.insets = new Insets(0, 0, 0, 0);
			rightHalf.add(this, gbc);

			// put the left and right sides into the window
			waveWindow.left.add(leftHalf);
			waveWindow.right.add(rightHalf);

			// add to list of wave panels
			waveWindow.wavePanels.add(this);
			if (waveWindow.wavePanels.size() == 1)
			{
				// on the first real addition, redraw any main time panel
				if (waveWindow.mainTimePanel != null)
				{
					waveWindow.mainTimePanel.repaint();
					waveWindow.mainTimePanelNeedsRepaint = true;
				}
			}

			// rebuild list of panels
			waveWindow.rebuildPanelList();
			waveWindow.redrawAllPanels();
		}

		private void digitalSignalNameClicked(ActionEvent evt)
		{
			Set set = waveSignals.keySet();
			if (set.size() == 0) return;
			JButton but = (JButton)set.iterator().next();
			Signal ws = (Signal)waveSignals.get(but);

			if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) == 0)
			{
				// standard click: add this as the only trace
				for(Iterator it = waveWindow.wavePanels.iterator(); it.hasNext(); )
				{
					Panel wp = (Panel)it.next();
					wp.clearHighlightedSignals();
				}
				addHighlightedSignal(ws);
				makeSelectedPanel();
			} else
			{
				// shift click: add or remove to list of highlighted traces
				if (ws.highlighted) removeHighlightedSignal(ws); else
					addHighlightedSignal(ws);
			}

			// show it in the schematic
			waveWindow.showSelectedNetworksInSchematic();
		}

		private void addTimePanel()
		{
			timePanel = new TimeTickPanel(this, waveWindow);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;       gbc.gridy = 1;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 1;     gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets(0, 0, 0, 0);
			rightHalf.add(timePanel, gbc);
		}

		private void removeTimePanel()
		{
			rightHalf.remove(timePanel);
			timePanel = null;
		}

		public void hidePanel()
		{
			waveWindow.hidePanel(this);
		}

		public void closePanel()
		{
			waveWindow.closePanel(this);
		}

		private void addSignalToPanel(Simulation.SimSignal sSig)
		{
			// see if the signal is already there
			for(Iterator it = waveSignals.keySet().iterator(); it.hasNext(); )
			{
				JButton but = (JButton)it.next();
				Signal ws = (Signal)waveSignals.get(but);
				if (ws.sSig == sSig)
				{
					// found it already: just change the color
					Color color = ws.sSig.getSignalColor();
					int index = 0;
					for( ; index<colorArray.length; index++)
					{
						if (color.equals(colorArray[index])) { index++;   break; }
					}
					if (index >= colorArray.length) index = 0;
					ws.sSig.setSignalColor(colorArray[index]);
					but.setForeground(colorArray[index]);
					signalButtons.repaint();
					repaint();
					return;
				}
			}

			// not found: add it
			int sigNo = waveSignals.size();
			sSig.setSignalColor(colorArray[sigNo % colorArray.length]);
			Signal wsig = new Signal(this, sSig);
			signalButtons.validate();
			signalButtons.repaint();
			if (signalButtonsPane != null) signalButtonsPane.validate();
			repaint();
			waveWindow.saveSignalOrder();
		}

		private void deleteSignalFromPanel()
		{
			waveWindow.deleteSignalFromPanel(this);
		}

		private void deleteAllSignalsFromPanel()
		{
			waveWindow.deleteAllSignalsFromPanel(this);
		}

		private void toggleBusContents()
		{
			// this panel must have one signal
			java.util.Collection theSignals = waveSignals.values();
			if (theSignals.size() != 1) return;

			// the only signal must be digital
			Signal ws = (Signal)theSignals.iterator().next();
			if (!(ws.sSig instanceof Simulation.SimDigitalSignal)) return;

			// the digital signal must be a bus
			Simulation.SimDigitalSignal sDSig = (Simulation.SimDigitalSignal)ws.sSig;
			List bussedSignals = sDSig.getBussedSignals();
			if (bussedSignals == null) return;

			// see if any of the bussed signals are displayed
			boolean opened = false;
			for(Iterator bIt = bussedSignals.iterator(); bIt.hasNext(); )
			{
				Simulation.SimDigitalSignal subDS = (Simulation.SimDigitalSignal)bIt.next();
				Signal subWs = waveWindow.findDisplayedSignal(subDS);
				if (subWs != null)
				{
					opened = true;
					break;
				}
			}

			// now open or close the bus
			if (opened)
			{
				// opened: remove all entries on the bus
				List allPanels = new ArrayList();
				for(Iterator it = waveWindow.wavePanels.iterator(); it.hasNext(); )
					allPanels.add(it.next());

				for(Iterator bIt = bussedSignals.iterator(); bIt.hasNext(); )
				{
					Simulation.SimDigitalSignal subDS = (Simulation.SimDigitalSignal)bIt.next();
					Signal subWs = waveWindow.findDisplayedSignal(subDS);
					if (subWs != null)
					{
						Panel wp = subWs.wavePanel;
						waveWindow.closePanel(wp);
						allPanels.remove(wp);
					}
				}
			} else
			{
				// closed: add all entries on the bus
				for(Iterator bIt = bussedSignals.iterator(); bIt.hasNext(); )
				{
					Simulation.SimDigitalSignal subDS = (Simulation.SimDigitalSignal)bIt.next();
					Panel wp = new Panel(waveWindow, false);
					Signal wsig = new Signal(wp, subDS);
				}
			}
			waveWindow.overall.validate();
			waveWindow.saveSignalOrder();
		}

		/**
		 * Method to set the time range in this panel.
		 * @param minTime the low time value.
		 * @param maxTime the high time value.
		 */
		public void setTimeRange(double minTime, double maxTime)
		{
			this.minTime = minTime;
			this.maxTime = maxTime;
		}

		/**
		 * Method to set the value range in this panel.
		 * @param low the low value.
		 * @param high the high value.
		 */
		public void setValueRange(double low, double high)
		{
			analogLowValue = low;
			analogHighValue = high;
			analogRange = analogHighValue - analogLowValue;
		}

		/**
		 * Method to get rid of this WaveformWindow.  Called by WindowFrame when
		 * that windowFrame gets closed.
		 */
		public void finished()
		{
			// remove myself from listener list
			removeKeyListener(this);
			removeMouseListener(this);
			removeMouseMotionListener(this);
			removeMouseWheelListener(this);
		}

		/**
		 * Method to repaint this window and its associated time-tick panel.
		 */
		public void repaintWithTime()
		{
			if (timePanel != null) timePanel.repaint(); else
			{
				waveWindow.mainTimePanel.repaint();								
			}
			repaint();
		}

		private Font waveWindowFont;
		private FontRenderContext waveWindowFRC = new FontRenderContext(null, false, false);

		/** for drawing solid lines */		private static final BasicStroke solidLine = new BasicStroke(0);
		/** for drawing dashed lines */		private static final BasicStroke dashedLine = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[] {10}, 0);

		/**
		 * Method to repaint this Panel.
		 */
		public void paint(Graphics g)
		{
			// to enable keys to be received
			requestFocus();

			sz = getSize();
			int wid = sz.width;
			int hei = sz.height;

			Point screenLoc = getLocationOnScreen();
			if (waveWindow.screenLowX != screenLoc.x ||
				waveWindow.screenHighX - waveWindow.screenLowX != wid)
					waveWindow.mainTimePanelNeedsRepaint = true;
			waveWindow.screenLowX = screenLoc.x;
			waveWindow.screenHighX = waveWindow.screenLowX + wid;

			// show the image
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, wid, hei);
			waveWindowFont = new Font(User.getDefaultFont(), Font.PLAIN, 12);

			// look at all traces in this panel
			for(Iterator it = waveSignals.values().iterator(); it.hasNext(); )
			{
				Signal ws = (Signal)it.next();
				g.setColor(ws.sSig.getSignalColor());
				if (ws.sSig instanceof Simulation.SimAnalogSignal)
				{
					// draw analog trace
					Simulation.SimAnalogSignal as = (Simulation.SimAnalogSignal)ws.sSig;
					int lx = 0, ly = 0;
					int numEvents = as.getNumEvents();
					for(int i=0; i<numEvents; i++)
					{
						double time = ws.sSig.getTime(i);
						int x = scaleTimeToX(time);
						int y = scaleValueToY(as.getValue(i));
						if (i != 0)
						{
							drawALine(g, lx, ly, x, y, ws.highlighted);
						}
						lx = x;   ly = y;
					}
					continue;
				}
				if (ws.sSig instanceof Simulation.SimDigitalSignal)
				{
					// draw digital traces
					Simulation.SimDigitalSignal ds = (Simulation.SimDigitalSignal)ws.sSig;
					List bussedSignals = ds.getBussedSignals();
					if (bussedSignals != null)
					{
						// a digital bus trace
						int busWidth = bussedSignals.size();
						long curValue = 0;
						double curTime = 0;
						int lastX = VERTLABELWIDTH;
						for(;;)
						{
							double nextTime = Double.MAX_VALUE;
							int bit = 0;
							boolean curDefined = true;
							for(Iterator bIt = bussedSignals.iterator(); bIt.hasNext(); )
							{
								Simulation.SimDigitalSignal subDS = (Simulation.SimDigitalSignal)bIt.next();
								int numEvents = subDS.getNumEvents();
								boolean undefined = false;
								for(int i=0; i<numEvents; i++)
								{
									double time = subDS.getTime(i);
									if (time <= curTime)
									{
										switch (subDS.getState(i) & Simulation.SimData.LOGIC)
										{
											case Simulation.SimData.LOGIC_LOW:  curValue &= ~(1<<bit);   undefined = false;   break;
											case Simulation.SimData.LOGIC_HIGH: curValue |= (1<<bit);    undefined = false;   break;
											case Simulation.SimData.LOGIC_X:
											case Simulation.SimData.LOGIC_Z: undefined = true;    break;
										}
									} else
									{
										if (time < nextTime) nextTime = time;
										break;
									}
								}
								if (undefined) { curDefined = false;   break; }
								bit++;
							}
							int x = scaleTimeToX(curTime);
							if (x >= VERTLABELWIDTH)
							{
								if (x < VERTLABELWIDTH+5)
								{
									// on the left edge: just draw the "<"
									drawALine(g, x, hei/2, x+5, hei-5, ws.highlighted);
									drawALine(g, x, hei/2, x+5, 5, ws.highlighted);
								} else
								{
									// bus change point: draw the "X"
									drawALine(g, x-5, 5, x+5, hei-5, ws.highlighted);
									drawALine(g, x+5, 5, x-5, hei-5, ws.highlighted);
								}
								if (lastX+5 < x-5)
								{
									// previous bus change point: draw horizontal bars to connect
									drawALine(g, lastX+5, 5, x-5, 5, ws.highlighted);
									drawALine(g, lastX+5, hei-5, x-5, hei-5, ws.highlighted);
								}
								String valString = "XX";
								if (curDefined) valString = Long.toString(curValue);
								g.setFont(waveWindowFont);
								GlyphVector gv = waveWindowFont.createGlyphVector(waveWindowFRC, valString);
								Rectangle2D glyphBounds = gv.getVisualBounds();
								int textHei = (int)glyphBounds.getHeight();
								g.drawString(valString, x+2, hei/2+textHei/2);
							}
							curTime = nextTime;
							lastX = x;
							if (nextTime == Double.MAX_VALUE) break;
						}
						if (lastX+5 < wid)
						{
							// run horizontal bars to the end
							drawALine(g, lastX+5, 5, wid, 5, ws.highlighted);
							drawALine(g, lastX+5, hei-5, wid, hei-5, ws.highlighted);
						}
						continue;
					}

					// a simple digital signal
					int lastx = VERTLABELWIDTH;
					int lastState = 0;
					if (ds.getStateVector() == null) continue;
					int numEvents = ds.getNumEvents();
					for(int i=0; i<numEvents; i++)
					{
						double time = ds.getTime(i);
						int x = scaleTimeToX(time);
						int lowy = 0, highy = 0;
						int state = ds.getState(i) & Simulation.SimData.LOGIC;
						switch (state)
						{
							case Simulation.SimData.LOGIC_LOW:  lowy = highy = 5;            break;
							case Simulation.SimData.LOGIC_HIGH: lowy = highy = hei-5;        break;
							case Simulation.SimData.LOGIC_X:    lowy = 5;   highy = hei-5;   break;
							case Simulation.SimData.LOGIC_Z:    lowy = 5;   highy = hei-5;   break;
						}
						if (i != 0)
						{
							if (state != lastState)
							{
								drawALine(g, x, 5, x, hei-5, ws.highlighted);
							}
						}
						if (lowy == highy)
						{
							drawALine(g, lastx, lowy, x, lowy, ws.highlighted);
						} else
						{
							g.fillRect(lastx, lowy, x-lastx, highy-lowy);
						}
						lastx = x;
						lastState = state;
					}
				}
			}

			// draw the vertical label
			g.setColor(Color.WHITE);
			g.drawLine(VERTLABELWIDTH, 0, VERTLABELWIDTH, hei);
			if (selected)
			{
				g.drawLine(VERTLABELWIDTH-1, 0, VERTLABELWIDTH-1, hei);
				g.drawLine(VERTLABELWIDTH-2, 0, VERTLABELWIDTH-2, hei-1);
				g.drawLine(VERTLABELWIDTH-3, 0, VERTLABELWIDTH-3, hei-2);
			}
			if (isAnalog)
			{
				double displayedLow = scaleYToValue(hei);
				double displayedHigh = scaleYToValue(0);
				StepSize ss = getSensibleValues(displayedHigh, displayedLow, 5);
				if (ss.separation != 0.0)
				{
					double value = ss.low;
					g.setFont(waveWindowFont);
					for(;;)
					{
						if (value >= displayedLow)
						{
							if (value > displayedHigh) break;
							int y = scaleValueToY(value);
							g.drawLine(VERTLABELWIDTH-10, y, VERTLABELWIDTH, y);
							String yValue = prettyPrint(value, ss.rangeScale, ss.stepScale);
							GlyphVector gv = waveWindowFont.createGlyphVector(waveWindowFRC, yValue);
							Rectangle2D glyphBounds = gv.getVisualBounds();
							int height = (int)glyphBounds.getHeight();
							int yPos = y + height / 2;
							if (yPos-height <= 0) yPos = height+1;
							if (yPos >= hei) yPos = hei;
							g.drawString(yValue, VERTLABELWIDTH-10-(int)glyphBounds.getWidth()-2, yPos);
						}
						value += ss.separation;
					}
				}
			}

			// draw the time cursors
			Graphics2D g2 = (Graphics2D)g;
			g2.setStroke(dashedLine);
			int x = scaleTimeToX(waveWindow.mainTime);
			if (x >= VERTLABELWIDTH)
				g.drawLine(x, 0, x, hei);
			g.setColor(Color.YELLOW);
			x = scaleTimeToX(waveWindow.extTime);
			if (x >= VERTLABELWIDTH)
				g.drawLine(x, 0, x, hei);
			g2.setStroke(solidLine);
			
			// show dragged area if there
			if (draggingArea)
			{
				g.setColor(Color.WHITE);
				int lowX = Math.min(dragStartX, dragEndX);
				int highX = Math.max(dragStartX, dragEndX);
				int lowY = Math.min(dragStartY, dragEndY);
				int highY = Math.max(dragStartY, dragEndY);
				g.drawLine(lowX, lowY, lowX, highY);
				g.drawLine(lowX, highY, highX, highY);
				g.drawLine(highX, highY, highX, lowY);
				g.drawLine(highX, lowY, lowX, lowY);
				if (ToolBar.getCursorMode() == ToolBar.CursorMode.MEASURE)
				{
					// show dimensions while dragging
					double lowTime = scaleXToTime(lowX);
					double highTime = scaleXToTime(highX);
					double lowValue = scaleYToValue(highY);
					double highValue = scaleYToValue(lowY);
					g.setFont(waveWindowFont);

					// show the low time value
					String lowTimeString = convertToEngineeringNotation(lowTime, "s", 9999);
					GlyphVector gv = waveWindowFont.createGlyphVector(waveWindowFRC, lowTimeString);
					Rectangle2D glyphBounds = gv.getVisualBounds();
					int textWid = (int)glyphBounds.getWidth();
					int textHei = (int)glyphBounds.getHeight();
					g.drawString(lowTimeString, lowX-textWid-2, (lowY+highY)/2+textHei/2);

					// show the high time value
					String highTimeString = convertToEngineeringNotation(highTime, "s", 9999);
					gv = waveWindowFont.createGlyphVector(waveWindowFRC, highTimeString);
					glyphBounds = gv.getVisualBounds();
					textWid = (int)glyphBounds.getWidth();
					textHei = (int)glyphBounds.getHeight();
					g.drawString(highTimeString, highX+2, (lowY+highY)/2+textHei/2);

					// show the difference time value
					String timeDiffString = convertToEngineeringNotation(highTime-lowTime, "s", 9999);
					gv = waveWindowFont.createGlyphVector(waveWindowFRC, timeDiffString);
					glyphBounds = gv.getVisualBounds();
					textWid = (int)glyphBounds.getWidth();
					textHei = (int)glyphBounds.getHeight();
					g.drawString(timeDiffString, lowX+(highX-lowX)/4 - textWid/2, highY-2);
					if (isAnalog)
					{
						// show the low value
						String lowValueString = TextUtils.formatDouble(lowValue);
						gv = waveWindowFont.createGlyphVector(waveWindowFRC, lowValueString);
						glyphBounds = gv.getVisualBounds();
						textWid = (int)glyphBounds.getWidth();
						textHei = (int)glyphBounds.getHeight();
						g.drawString(lowValueString, (lowX+highX)/2 - textWid/2, highY + textHei + 3);
	
						// show the high value
						String highValueString = TextUtils.formatDouble(highValue);
						gv = waveWindowFont.createGlyphVector(waveWindowFRC, highValueString);
						glyphBounds = gv.getVisualBounds();
						textWid = (int)glyphBounds.getWidth();
						textHei = (int)glyphBounds.getHeight();
						g.drawString(highValueString, (lowX+highX)/2 - textWid/2, lowY - 2);
	
						// show the value difference
						String valueDiffString = TextUtils.formatDouble(highValue - lowValue);
						gv = waveWindowFont.createGlyphVector(waveWindowFRC, valueDiffString);
						glyphBounds = gv.getVisualBounds();
						textWid = (int)glyphBounds.getWidth();
						textHei = (int)glyphBounds.getHeight();
						g.drawString(valueDiffString, lowX + 2, lowY+(highY-lowY)/4+textHei/2);
					}
				}
			}
		}

		private void drawALine(Graphics g, int fX, int fY, int tX, int tY, boolean highlighted)
		{
			// clip to left edge
			if (fX < VERTLABELWIDTH || tX < VERTLABELWIDTH)
			{
				Point2D from = new Point2D.Double(fX, fY);
				Point2D to = new Point2D.Double(tX, tY);
				sz = getSize();
				if (GenMath.clipLine(from, to, VERTLABELWIDTH, sz.width, 0, sz.height)) return;
				fX = (int)from.getX();
				fY = (int)from.getY();
				tX = (int)to.getX();
				tY = (int)to.getY();
			}

			// draw the line
			g.drawLine(fX, fY, tX, tY);

			// highlight the line if requested
			if (highlighted)
			{
				if (fX == tX)
				{
					// vertical line
					g.drawLine(fX-1, fY, tX-1, tY);
					g.drawLine(fX+1, fY, tX+1, tY);
				} else if (fY == tY)
				{
					// horizontal line
					g.drawLine(fX, fY+1, tX, tY+1);
					g.drawLine(fX, fY-1, tX, tY-1);
				} else
				{
					int xDelta = 0, yDelta = 1;
					if (Math.abs(fX-tX) < Math.abs(fY-tY))
					{
						xDelta = 1;   yDelta = 0;
					}
					g.drawLine(tX+xDelta, tY+yDelta, fX+xDelta, fY+yDelta);
					g.drawLine(tX-xDelta, tY-yDelta, fX-xDelta, fY-yDelta);
				}
			}
		}

		/**
		 * Method to scale a time value to the X coordinate in this window.
		 * @param time the time value.
		 * @return the X coordinate of that time value.
		 */
		private int scaleTimeToX(double time)
		{
			double x = (time - minTime) / (maxTime - minTime) * (sz.width - VERTLABELWIDTH) + VERTLABELWIDTH;
			return (int)x;
		}

		/**
		 * Method to scale an X coordinate to a time value in this window.
		 * @param x the X coordinate.
		 * @return the time value corresponding to that coordinate.
		 */
		private double scaleXToTime(int x)
		{
			double time = ((double)(x - VERTLABELWIDTH)) / (sz.width - VERTLABELWIDTH) * (maxTime - minTime) + minTime;
			return time;
		}

		/**
		 * Method to scale a delta-X to a delta-time in this window.
		 * @param dx the delta-X.
		 * @return the delta-time value corresponding to that coordinate.
		 */
		private double scaleDeltaXToTime(int dx)
		{
			double dTime = ((double)dx) / (sz.width - VERTLABELWIDTH) * (maxTime - minTime);
			return dTime;
		}

		/**
		 * Method to scale a value to the Y coordinate in this window.
		 * @param value the value in Y.
		 * @return the Y coordinate of that value.
		 */
		private int scaleValueToY(double value)
		{
			double y = sz.height - 1 - (value - analogLowValue) / analogRange * (sz.height-1);
			return (int)y;
		}

		/**
		 * Method to scale a Y coordinate in this window to a value.
		 * @param y the Y coordinate.
		 * @return the value corresponding to that coordinate.
		 */
		private double scaleYToValue(int y)
		{
			double value = analogLowValue - ((double)(y - sz.height + 1)) / (sz.height-1) * analogRange;
			return value;
		}

		/**
		 * Method to scale a delta-yY in this window to a delta-value.
		 * @param dy the delta-Y.
		 * @return the delta-value corresponding to that Y change.
		 */
		private double scaleDeltaYToValue(int dy)
		{
			double dValue = - ((double)dy) / (sz.height-1) * analogRange;
			return dValue;
		}

		/**
		 * Method to find the Signals in an area.
		 * @param lX the low X coordinate of the area.
		 * @param hX the high X coordinate of the area.
		 * @param lY the low Y coordinate of the area.
		 * @param hY the high Y coordinate of the area.
		 * @return a List of signals in that area.
		 */
		private List findSignalsInArea(int lX, int hX, int lY, int hY)
		{
			double lXd = Math.min(lX, hX)-2;
			double hXd = Math.max(lX, hX)+2;
			double hYd = Math.min(lY, hY)-2;
			double lYd = Math.max(lY, hY)+2;
			if (lXd > hXd) { double swap = lXd;   lXd = hXd;   hXd = swap; }
			if (lYd > hYd) { double swap = lYd;   lYd = hYd;   hYd = swap; }
			List foundList = new ArrayList();
			for(Iterator it = waveSignals.values().iterator(); it.hasNext(); )
			{
				Signal ws = (Signal)it.next();
				if (ws.sSig instanceof Simulation.SimAnalogSignal)
				{
					// search analog trace
					Simulation.SimAnalogSignal as = (Simulation.SimAnalogSignal)ws.sSig;
					double lastXd = 0, lastYd = 0;
					int numEvents = as.getNumEvents();
					for(int i=0; i<numEvents; i++)
					{
						double time = ws.sSig.getTime(i);
						double x = scaleTimeToX(time);
						double y = scaleValueToY(as.getValue(i));
						if (i != 0)
						{
							// should see if the line is in the area
							Point2D from = new Point2D.Double(lastXd, lastYd);
							Point2D to = new Point2D.Double(x, y);
							if (!GenMath.clipLine(from, to, lXd, hXd, lYd, hYd))
							{
								foundList.add(ws);
								break;
							}
						}
						lastXd = x;   lastYd = y;
					}
				}
			}
			return foundList;
		}
		
		private void clearHighlightedSignals()
		{
			for(Iterator it = waveSignals.values().iterator(); it.hasNext(); )
			{
				Signal ws = (Signal)it.next();
				if (!ws.highlighted) continue;
				ws.highlighted = false;
				if (ws.sigButton != null)
					ws.sigButton.setBackground(background);
			}
			this.repaint();
		}

		private void addHighlightedSignal(Signal ws)
		{
			if (ws.sigButton != null)
			{
				if (background == null) background = ws.sigButton.getBackground();
				ws.sigButton.setBackground(Color.BLACK);
			}
			ws.highlighted = true;
			this.repaint();
		}

		private void removeHighlightedSignal(Signal ws)
		{
			ws.highlighted = false;
			if (ws.sigButton != null)
				ws.sigButton.setBackground(background);
			this.repaint();
		}

		/**
		 * Method to make this the highlighted Panel.
		 */
		public void makeSelectedPanel()
		{
			for(Iterator it = waveWindow.wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				if (wp.selected && wp != this)
				{
					wp.selected = false;
					wp.repaint();
				}
			}
			if (!selected)
			{
				selected = true;
				this.repaint();
			}
		}

		// the MouseListener events
		public void mousePressed(MouseEvent evt)
		{
			waveWindow.vcrClickStop();

			// set this to be the selected panel
			makeSelectedPanel();

			ToolBar.CursorMode mode = ToolBar.getCursorMode();
			if (mode == ToolBar.CursorMode.ZOOM) mousePressedZoom(evt); else
				if (mode == ToolBar.CursorMode.PAN) mousePressedPan(evt); else
					mousePressedSelect(evt);
		}

		public void mouseReleased(MouseEvent evt)
		{
			ToolBar.CursorMode mode = ToolBar.getCursorMode();
			if (mode == ToolBar.CursorMode.ZOOM) mouseReleasedZoom(evt); else
				if (mode == ToolBar.CursorMode.PAN) mouseReleasedPan(evt); else
					mouseReleasedSelect(evt);
		}

		public void mouseClicked(MouseEvent evt) {}
		public void mouseEntered(MouseEvent evt) {}
		public void mouseExited(MouseEvent evt) {}

		// the MouseMotionListener events
		public void mouseMoved(MouseEvent evt)
		{
			ToolBar.CursorMode mode = ToolBar.getCursorMode();
			if (mode == ToolBar.CursorMode.ZOOM) mouseMovedZoom(evt); else
				if (mode == ToolBar.CursorMode.PAN) mouseMovedPan(evt); else
					mouseMovedSelect(evt);
		}
		public void mouseDragged(MouseEvent evt)
		{
			ToolBar.CursorMode mode = ToolBar.getCursorMode();
			if (mode == ToolBar.CursorMode.ZOOM) mouseDraggedZoom(evt); else
				if (mode == ToolBar.CursorMode.PAN) mouseDraggedPan(evt); else
					mouseDraggedSelect(evt);
		}

		// the MouseWheelListener events
		public void mouseWheelMoved(MouseWheelEvent evt) {}

		// the KeyListener events
		public void keyPressed(KeyEvent evt)
		{
			waveWindow.vcrClickStop();
		}
		public void keyReleased(KeyEvent evt) {}
		public void keyTyped(KeyEvent evt) {}

		// ****************************** SELECTION IN WAVEFORM WINDOW ******************************

		/**
		 * Method to implement the Mouse Pressed event for selection.
		 */ 
		public void mousePressedSelect(MouseEvent evt)
		{
			// see if the time cursors are selected
			draggingMain = draggingExt = draggingArea = false;
			int mainX = scaleTimeToX(waveWindow.mainTime);
			if (Math.abs(mainX - evt.getX()) < 5)
			{
				draggingMain = true;
				return;
			}
			int extX = scaleTimeToX(waveWindow.extTime);
			if (Math.abs(extX - evt.getX()) < 5)
			{
				draggingExt = true;
				return;
			}

			// drag area
			draggingArea = true;
			dragStartX = dragEndX = evt.getX();
			dragStartY = dragEndY = evt.getY();
		}

		/**
		 * Method to implement the Mouse Released event for selection.
		 */ 
		public void mouseReleasedSelect(MouseEvent evt)
		{
			if (draggingArea)
			{
				Panel wp = (Panel)evt.getSource();
				if (ToolBar.getCursorMode() != ToolBar.CursorMode.MEASURE &&
					ToolBar.getSelectMode() == ToolBar.SelectMode.OBJECTS)
				{
					draggingArea = false;
					List foundList = wp.findSignalsInArea(dragStartX, dragEndX, dragStartY, dragEndY);
					if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) == 0)
					{
						// standard click: add this as the only trace
						if (wp.isAnalog) clearHighlightedSignals(); else
						{
							for(Iterator it = waveWindow.wavePanels.iterator(); it.hasNext(); )
							{
								Panel oWp = (Panel)it.next();
								oWp.clearHighlightedSignals();
							}
						}
						for(Iterator it = foundList.iterator(); it.hasNext(); )
						{
							Signal ws = (Signal)it.next();
							wp.addHighlightedSignal(ws);
						}
					} else
					{
						// shift click: add or remove to list of highlighted traces
						for(Iterator it = foundList.iterator(); it.hasNext(); )
						{
							Signal ws = (Signal)it.next();
							if (ws.highlighted) removeHighlightedSignal(ws); else
								wp.addHighlightedSignal(ws);
						}
					}

					// show it in the schematic
					wp.waveWindow.showSelectedNetworksInSchematic();
				} else
				{
					// just leave this highlight and show dimensions
				}
			}
			this.repaint();
		}

		/**
		 * Method to implement the Mouse Dragged event for selection.
		 */ 
		public void mouseDraggedSelect(MouseEvent evt)
		{
			if (draggingMain)
			{
				double time = scaleXToTime(evt.getX());
				waveWindow.setMainTimeCursor(time);
				waveWindow.redrawAllPanels();
			} else if (draggingExt)
			{
				double time = scaleXToTime(evt.getX());
				waveWindow.setExtensionTimeCursor(time);
				waveWindow.redrawAllPanels();
			} else if (draggingArea)
			{
				dragEndX = evt.getX();
				dragEndY = evt.getY();
				this.repaint();
			}
		}

		public void mouseMovedSelect(MouseEvent evt) {}

		// ****************************** ZOOMING IN WAVEFORM WINDOW ******************************
	
		/**
		 * Method to implement the Mouse Pressed event for zooming.
		 */ 
		public void mousePressedZoom(MouseEvent evt)
		{
			dragStartX = evt.getX();
			dragStartY = evt.getY();
			ZoomAndPanListener.setProperCursor(evt);
			draggingArea = true;
		}
		
		/**
		 * Method to implement the Mouse Released event for zooming.
		 */ 
		public void mouseReleasedZoom(MouseEvent evt)
		{
			ZoomAndPanListener.setProperCursor(evt);
			draggingArea = false;
			double lowTime = this.scaleXToTime(Math.min(dragEndX, dragStartX));
			double highTime = this.scaleXToTime(Math.max(dragEndX, dragStartX));
			double timeRange = highTime - lowTime;
			lowTime -= timeRange / 8;
			highTime += timeRange / 8;
			double lowValue = this.scaleYToValue(Math.max(dragEndY, dragStartY));
			double highValue = this.scaleYToValue(Math.min(dragEndY, dragStartY));
			double valueRange = highValue - lowValue;
			lowValue -= valueRange / 8;
			highValue += valueRange / 8;
			for(Iterator it = waveWindow.wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				if (!waveWindow.timeLocked && wp != this) continue;
				if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) == 0)
				{
					// standard click: zoom in
					wp.minTime = lowTime;
					wp.maxTime = highTime;
					if (wp == this)
					{
						wp.setValueRange(lowValue, highValue);
					}
				} else
				{
					// shift-click: zoom out
					double oldRange = wp.maxTime - wp.minTime;
					wp.minTime = (lowTime + highTime) / 2 - oldRange;
					wp.maxTime = (lowTime + highTime) / 2 + oldRange;
					if (wp == this)
					{
						wp.setValueRange((lowValue + highValue) / 2 - wp.analogRange,
							(lowValue + highValue) / 2 + wp.analogRange);
					}
				}
				wp.repaintWithTime();
			}
		}
		
		/**
		 * Method to implement the Mouse Dragged event for zooming.
		 */ 
		public void mouseDraggedZoom(MouseEvent evt)
		{
			ZoomAndPanListener.setProperCursor(evt);
			if (draggingArea)
			{
				dragEndX = evt.getX();
				dragEndY = evt.getY();
				this.repaint();
			}
		}

		public void mouseMovedZoom(MouseEvent evt)
		{
			ZoomAndPanListener.setProperCursor(evt);
		}

		// ****************************** PANNING IN WAVEFORM WINDOW ******************************
	
		/**
		 * Method to implement the Mouse Pressed event for panning.
		 */ 
		public void mousePressedPan(MouseEvent evt)
		{
			dragStartX = evt.getX();
			dragStartY = evt.getY();
		}
		
		/**
		 * Method to implement the Mouse Released event for panning.
		 */ 
		public void mouseReleasedPan(MouseEvent evt)
		{
		}
		
		/**
		 * Method to implement the Mouse Dragged event for panning.
		 */ 
		public void mouseDraggedPan(MouseEvent evt)
		{
			dragEndX = evt.getX();
			dragEndY = evt.getY();
			double dTime = scaleDeltaXToTime(dragEndX - dragStartX);
			double dValue = scaleDeltaYToValue(dragEndY - dragStartY);

			for(Iterator it = waveWindow.wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				if (!waveWindow.timeLocked && wp != this) continue;
				wp.minTime -= dTime;
				wp.maxTime -= dTime;
				if (wp == this)
				{
					setValueRange(analogLowValue - dValue, analogHighValue - dValue);
				}
				wp.repaintWithTime();
			}
			dragStartX = dragEndX;
			dragStartY = dragEndY;
		}

		public void mouseMovedPan(MouseEvent evt) {}
	}

	// ****************************** DRAG AND DROP ******************************

	/**
	 * This class extends JPanel so that wavepanels can be identified by the Drag and Drop system.
	 */
	public static class OnePanel extends JPanel
	{
		Panel panel;
		WaveformWindow ww;

		public OnePanel(Panel panel, WaveformWindow ww)
		{
			super();
			this.panel = panel;
			this.ww = ww;
		}

		public Panel getPanel() { return panel; }

		public WaveformWindow getWaveformWindow() { return ww; }
	}

	private static class WaveFormDropTarget implements DropTargetListener
	{
		public void dragEnter(DropTargetDragEvent e)
		{
			e.acceptDrag(DnDConstants.ACTION_LINK);
		}
	
		public void dragOver(DropTargetDragEvent e)
		{
			e.acceptDrag(DnDConstants.ACTION_LINK);
		}
	
		public void dropActionChanged(DropTargetDragEvent e)
		{
			e.acceptDrag(DnDConstants.ACTION_LINK);
		}

		public void dragExit(DropTargetEvent e) {}
	
		public void drop(DropTargetDropEvent dtde)
		{
			Object data = null;
			try
			{
				dtde.acceptDrop(DnDConstants.ACTION_LINK);
				data = dtde.getTransferable().getTransferData(DataFlavor.stringFlavor);
				if (data == null)
					throw new NullPointerException();
			} catch (Throwable t)
			{
				t.printStackTrace();
				dtde.dropComplete(false);
				return;
			}
			if (!(data instanceof String))
			{
				dtde.dropComplete(false);
				return;
			}
			String sigName = (String)data;
			DropTarget dt = (DropTarget)dtde.getSource();
			if (!(dt.getComponent() instanceof OnePanel))
			{
				dtde.dropComplete(false);
				return;
			}
			OnePanel op = (OnePanel)dt.getComponent();
			WaveformWindow ww = op.getWaveformWindow();
			Panel panel = op.getPanel();

			// dropped onto an existing panel
			Simulation.SimSignal sSig = ww.findSignal(sigName);
			if (sSig == null)
			{
				dtde.dropComplete(false);
				return;
			}

			// digital signals are always added in new panels
			if (sSig instanceof Simulation.SimDigitalSignal) panel = null;
			if (panel != null)
			{
				// overlay this signal onto an existing panel
				panel.addSignalToPanel(sSig);
				panel.makeSelectedPanel();
				dtde.dropComplete(true);
				return;
			}

			// add this signal in a new panel
			boolean isAnalog = false;
			if (sSig instanceof Simulation.SimAnalogSignal) isAnalog = true;
			panel = new Panel(ww, isAnalog);
			if (isAnalog)
			{
				Simulation.SimAnalogSignal as = (Simulation.SimAnalogSignal)sSig;
				double lowValue = 0, highValue = 0;
				for(int i=0; i<as.getNumEvents(); i++)
				{
					double val = as.getValue(i);
					if (i == 0) lowValue = highValue = val; else
					{
						if (val < lowValue) lowValue = val;
						if (val > highValue) highValue = val;
					}
				}
				double range = highValue - lowValue;
				if (range == 0) range = 2;
				double rangeExtra = range / 10;
				panel.setValueRange(lowValue - rangeExtra, highValue + rangeExtra);
			}
			Signal wsig = new Signal(panel, sSig);
			ww.overall.validate();
			panel.repaint();
			dtde.dropComplete(true);
		}
	}

	// ************************************* TIME GRID ALONG THE TOP OF EACH PANEL *************************************

	/**
	 * This class defines the horizontal time tick display at the top of each Panel.
	 */
	private static class TimeTickPanel extends JPanel
	{
		Panel wavePanel;
		WaveformWindow waveWindow;

		// constructor
		TimeTickPanel(Panel wavePanel, WaveformWindow waveWindow)
		{
			// remember state
			this.wavePanel = wavePanel;
			this.waveWindow = waveWindow;

			// setup this panel window
			Dimension sz = new Dimension(16, 20);
			this.setMinimumSize(sz);
			setPreferredSize(sz);
		}

		/**
		 * Method to repaint this TimeTickPanel.
		 */
		public void paint(Graphics g)
		{
			Dimension sz = getSize();
			int wid = sz.width;
			int hei = sz.height;
			int offX = 0;
			Panel drawHere = wavePanel;
			if (drawHere == null)
			{
				// this is the main time panel for all panels
				Point screenLoc = getLocationOnScreen();
				offX = waveWindow.screenLowX - screenLoc.x;
				int newWid = waveWindow.screenHighX - waveWindow.screenLowX;

				// because the main time panel needs a Panel (won't work if there aren't any)
				// have to do complex things to request a repaint after adding the first Panel
				if (newWid == 0 || waveWindow.wavePanels.size() == 0)
				{
					if (waveWindow.mainTimePanelNeedsRepaint)
						repaint();
					return;
				}

				if (offX + newWid > wid) newWid = wid - offX;
				wid = newWid;

				drawHere = (Panel)waveWindow.wavePanels.get(0);
				waveWindow.mainTimePanelNeedsRepaint = false;
				g.setClip(offX, 0, wid, hei);
			}

			// draw the black background
			g.setColor(Color.BLACK);
			g.fillRect(offX, 0, wid, hei);

			// draw the time ticks
			g.setColor(Color.WHITE);
			if (wavePanel != null)
				g.drawLine(WaveformWindow.Panel.VERTLABELWIDTH + offX, hei-1, wid+offX, hei-1);
			double displayedLow = drawHere.scaleXToTime(WaveformWindow.Panel.VERTLABELWIDTH);
			double displayedHigh = drawHere.scaleXToTime(wid);
			StepSize ss = getSensibleValues(displayedHigh, displayedLow, 10);
			if (ss.separation == 0.0) return;
			double time = ss.low;
			for(;;)
			{
				if (time >= displayedLow)
				{
					if (time > ss.high) break;
					int x = drawHere.scaleTimeToX(time) + offX;
					g.drawLine(x, 0, x, hei);
					String timeVal = convertToEngineeringNotation(time, "s", ss.stepScale);
					g.drawString(timeVal, x+2, hei-2);
				}
				time += ss.separation;
			}
		}
	}

	// ************************************* INDIVIDUAL TRACES *************************************

	/**
	 * This class defines a single trace in a Panel.
	 */
	public static class Signal
	{
		/** the panel that holds this signal */			private Panel wavePanel;
		/** the data for this signal */					private Simulation.SimSignal sSig;
		/** true if this signal is highlighted */		private boolean highlighted;
		/** the button on the left with this signal */	private JButton sigButton;

		private static class SignalButton extends MouseAdapter
		{
			private static final int BUTTON_SIZE = 15;

			private Signal signal;

			SignalButton(Signal signal) { this.signal = signal; }

			public void mouseClicked(MouseEvent e)
			{
				if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0)
				{
					if (!signal.highlighted)
					{
						signal.wavePanel.clearHighlightedSignals();
						signal.wavePanel.addHighlightedSignal(signal);
						signal.wavePanel.makeSelectedPanel();
					}
					JPopupMenu menu = new JPopupMenu("Color");
					for(int i=0; i < colorArray.length; i++)
						addColoredButton(menu, colorArray[i]);

					menu.show(signal.sigButton, e.getX(), e.getY());
				}
			}
			private void addColoredButton(JPopupMenu menu, Color color)
			{
				BufferedImage bi = new BufferedImage(BUTTON_SIZE, BUTTON_SIZE, BufferedImage.TYPE_INT_RGB);
				for(int y=0; y<BUTTON_SIZE; y++)
				{
					for(int x=0; x<BUTTON_SIZE; x++)
					{
						bi.setRGB(x, y, color.getRGB());
					}
				}
				ImageIcon redIcon = new ImageIcon(bi);
				JMenuItem menuItem = new JMenuItem(redIcon);
				menu.add(menuItem);
				menuItem.addActionListener(new ChangeSignalColorListener(signal, color));
			}
		}

		static class ChangeSignalColorListener implements ActionListener
		{
			Signal signal;
			Color col;

			ChangeSignalColorListener(Signal signal, Color col) { super();  this.signal = signal;   this.col = col; }

			public void actionPerformed(ActionEvent evt)
			{
				signal.sSig.setSignalColor(col);
				signal.sigButton.setForeground(col);
				signal.wavePanel.repaint();
			}
		};

		public Signal(Panel wavePanel, Simulation.SimSignal sSig)
		{
			this.wavePanel = wavePanel;
			this.sSig = sSig;
			this.highlighted = false;
			String sigName = sSig.getSignalName();
			if (sSig.getSignalContext() != null) sigName = sSig.getSignalContext() + "." + sigName;
			if (wavePanel.isAnalog)
			{
				sigButton = new JButton(sigName);
				sigButton.setBorderPainted(false);
				sigButton.setDefaultCapable(false);
				sigButton.setForeground(sSig.getSignalColor());
				wavePanel.signalButtons.add(sigButton);
				wavePanel.waveSignals.put(sigButton, this);
				sigButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent evt) { signalNameClicked(evt); }
				});
				sigButton.addMouseListener(new SignalButton(this));
			} else
			{
				wavePanel.digitalSignalButton.setText(sigName);
				wavePanel.waveSignals.put(wavePanel.digitalSignalButton, this);
			}
		}

		private void signalNameClicked(ActionEvent evt)
		{
			JButton signal = (JButton)evt.getSource();
			Signal ws = (Signal)wavePanel.waveSignals.get(signal);
			if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) == 0)
			{
				// standard click: add this as the only trace
				ws.wavePanel.clearHighlightedSignals();
				ws.wavePanel.addHighlightedSignal(ws);
				ws.wavePanel.makeSelectedPanel();
			} else
			{
				// shift click: add or remove to list of highlighted traces
				if (ws.highlighted) ws.wavePanel.removeHighlightedSignal(ws); else
					ws.wavePanel.addHighlightedSignal(ws);
			}

			// show it in the schematic
			ws.wavePanel.waveWindow.showSelectedNetworksInSchematic();
		}
	}

    // ************************************* CONTROL *************************************

	private static class WaveComponentListener implements ComponentListener
	{
		private JPanel panel;

		public WaveComponentListener(JPanel panel) { this.panel = panel; }

		public void componentHidden(ComponentEvent e) {}
		public void componentMoved(ComponentEvent e) {}
		public void componentResized(ComponentEvent e)
		{
			panel.repaint();
		}
		public void componentShown(ComponentEvent e) {}
	}

	public WaveformWindow(Simulation.SimData sd, WindowFrame wf)
	{
		// initialize the structure
		this.wf = wf;
		this.sd = sd;
		wavePanels = new ArrayList();
		this.timeLocked = true;

		Highlight.addHighlightListener(this);

		// the total panel in the waveform window
		overall = new OnePanel(null, this);
		overall.setLayout(new GridBagLayout());

		WaveComponentListener wcl = new WaveComponentListener(overall);
		overall.addComponentListener(wcl);

		// the main part of the waveform window: a split-pane between names and traces, put into a scrollpane
		left = new JPanel();
		left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
		right = new JPanel();
		right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
		split.setResizeWeight(0.1);
		scrollAll = new JScrollPane(split);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;       gbc.gridy = 2;
		gbc.gridwidth = 13;  gbc.gridheight = 1;
		gbc.weightx = 1;     gbc.weighty = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.BOTH;
		overall.add(scrollAll, gbc);

		if (sd.isAnalog())
		{
			// the top part of the waveform window: status information
			JButton addPanel = new JButton(iconAddPanel);
			addPanel.setBorderPainted(false);
			addPanel.setDefaultCapable(false);
			addPanel.setToolTipText("Create new waveform panel");
			Dimension minWid = new Dimension(iconAddPanel.getIconWidth()+4, iconAddPanel.getIconHeight()+4);
			addPanel.setMinimumSize(minWid);
			addPanel.setPreferredSize(minWid);
			gbc.gridx = 0;       gbc.gridy = 0;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0;     gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = java.awt.GridBagConstraints.NONE;
			overall.add(addPanel, gbc);
			addPanel.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { addNewPanel(); }
			});
		}

		refresh = new JButton(iconRefresh);
		refresh.setBorderPainted(false);
		refresh.setDefaultCapable(false);
		refresh.setToolTipText("Reread stimuli data file and update waveforms");
		Dimension minWid = new Dimension(iconRefresh.getIconWidth()+4, iconRefresh.getIconHeight()+4);
		refresh.setMinimumSize(minWid);
		refresh.setPreferredSize(minWid);
		gbc.gridx = 1;       gbc.gridy = 1;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		overall.add(refresh, gbc);
		refresh.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { refreshData(); }
		});

		timeLock = new JButton(iconLockTime);
		timeLock.setBorderPainted(false);
		timeLock.setDefaultCapable(false);
		timeLock.setToolTipText("Lock all panels in time");
		minWid = new Dimension(iconLockTime.getIconWidth()+4, iconLockTime.getIconHeight()+4);
		timeLock.setMinimumSize(minWid);
		timeLock.setPreferredSize(minWid);
		gbc.gridx = 2;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		overall.add(timeLock, gbc);
		timeLock.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { togglePanelTimeLock(); }
		});

		signalNameList = new JComboBox();
		signalNameList.setToolTipText("Show or hide waveform panels");
		signalNameList.setLightWeightPopupEnabled(false);
		gbc.gridx = 3;       gbc.gridy = 0;
		gbc.gridwidth = 5;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		overall.add(signalNameList, gbc);
		signalNameList.addItem("Panel 1");
		signalNameList.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { togglePanelName(); }
		});

		growPanel = new JButton(iconGrowPanel);
		growPanel.setBorderPainted(false);
		growPanel.setDefaultCapable(false);
		growPanel.setToolTipText("Increase minimum panel height");
		minWid = new Dimension(iconGrowPanel.getIconWidth()+4, iconGrowPanel.getIconHeight()+4);
		growPanel.setMinimumSize(minWid);
		growPanel.setPreferredSize(minWid);
		gbc.gridx = 8;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		overall.add(growPanel, gbc);
		growPanel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { growPanels(1.25); }
		});

		shrinkPanel = new JButton(iconShrinkPanel);
		shrinkPanel.setBorderPainted(false);
		shrinkPanel.setDefaultCapable(false);
		shrinkPanel.setToolTipText("Decrease minimum panel height");
		minWid = new Dimension(iconShrinkPanel.getIconWidth()+4, iconShrinkPanel.getIconHeight()+4);
		shrinkPanel.setMinimumSize(minWid);
		shrinkPanel.setPreferredSize(minWid);
		gbc.gridx = 9;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		overall.add(shrinkPanel, gbc);
		shrinkPanel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { growPanels(0.8); }
		});

		mainPos = new JLabel("Main:");
		mainPos.setToolTipText("The main (white) time cursor");
		gbc.gridx = 10;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.3;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		overall.add(mainPos, gbc);
		extPos = new JLabel("Ext:");
		extPos.setToolTipText("The extension (yellow) time cursor");
		gbc.gridx = 11;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.3;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		overall.add(extPos, gbc);
		delta = new JLabel("Delta:");
		delta.setToolTipText("Time distance between cursors");
		gbc.gridx = 12;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.3;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		overall.add(delta, gbc);

		// add VCR controls
		JButton vcrButtonRewind = new JButton(iconVCRRewind);
		vcrButtonRewind.setBorderPainted(false);
		vcrButtonRewind.setDefaultCapable(false);
		vcrButtonRewind.setToolTipText("Rewind main time cursor to start");
		minWid = new Dimension(iconVCRRewind.getIconWidth()+4, iconVCRRewind.getIconHeight()+4);
		vcrButtonRewind.setMinimumSize(minWid);
		vcrButtonRewind.setPreferredSize(minWid);
		gbc.gridx = 3;       gbc.gridy = 1;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		overall.add(vcrButtonRewind, gbc);
		vcrButtonRewind.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { vcrClickRewind(); }
		});

		JButton vcrButtonPlayBackwards = new JButton(iconVCRPlayBackward);
		vcrButtonPlayBackwards.setBorderPainted(false);
		vcrButtonPlayBackwards.setDefaultCapable(false);
		vcrButtonPlayBackwards.setToolTipText("Play main time cursor backwards");
		minWid = new Dimension(iconVCRPlayBackward.getIconWidth()+4, iconVCRPlayBackward.getIconHeight()+4);
		vcrButtonPlayBackwards.setMinimumSize(minWid);
		vcrButtonPlayBackwards.setPreferredSize(minWid);
		gbc.gridx = 4;       gbc.gridy = 1;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		overall.add(vcrButtonPlayBackwards, gbc);
		vcrButtonPlayBackwards.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { vcrClickPlayBackwards(); }
		});

		JButton vcrButtonStop = new JButton(iconVCRStop);
		vcrButtonStop.setBorderPainted(false);
		vcrButtonStop.setDefaultCapable(false);
		vcrButtonStop.setToolTipText("Stop moving main time cursor");
		minWid = new Dimension(iconVCRStop.getIconWidth()+4, iconVCRStop.getIconHeight()+4);
		vcrButtonStop.setMinimumSize(minWid);
		vcrButtonStop.setPreferredSize(minWid);
		gbc.gridx = 5;       gbc.gridy = 1;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		overall.add(vcrButtonStop, gbc);
		vcrButtonStop.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { vcrClickStop(); }
		});

		JButton vcrButtonPlay = new JButton(iconVCRPlay);
		vcrButtonPlay.setBorderPainted(false);
		vcrButtonPlay.setDefaultCapable(false);
		vcrButtonPlay.setToolTipText("Play main time cursor");
		minWid = new Dimension(iconVCRPlay.getIconWidth()+4, iconVCRPlay.getIconHeight()+4);
		vcrButtonPlay.setMinimumSize(minWid);
		vcrButtonPlay.setPreferredSize(minWid);
		gbc.gridx = 6;       gbc.gridy = 1;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		overall.add(vcrButtonPlay, gbc);
		vcrButtonPlay.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { vcrClickPlay(); }
		});

		JButton vcrButtonToEnd = new JButton(iconVCRToEnd);
		vcrButtonToEnd.setBorderPainted(false);
		vcrButtonToEnd.setDefaultCapable(false);
		vcrButtonToEnd.setToolTipText("Move main time cursor to end");
		minWid = new Dimension(iconVCRToEnd.getIconWidth()+4, iconVCRToEnd.getIconHeight()+4);
		vcrButtonToEnd.setMinimumSize(minWid);
		vcrButtonToEnd.setPreferredSize(minWid);
		gbc.gridx = 7;       gbc.gridy = 1;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		overall.add(vcrButtonToEnd, gbc);
		vcrButtonToEnd.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { vcrClickToEnd(); }
		});

		JButton vcrButtonFaster = new JButton(iconVCRFaster);
		vcrButtonFaster.setBorderPainted(false);
		vcrButtonFaster.setDefaultCapable(false);
		vcrButtonFaster.setToolTipText("Move main time cursor faster");
		minWid = new Dimension(iconVCRFaster.getIconWidth()+4, iconVCRFaster.getIconHeight()+4);
		vcrButtonFaster.setMinimumSize(minWid);
		vcrButtonFaster.setPreferredSize(minWid);
		gbc.gridx = 8;       gbc.gridy = 1;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		overall.add(vcrButtonFaster, gbc);
		vcrButtonFaster.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { vcrClickFaster(); }
		});

		JButton vcrButtonSlower = new JButton(iconVCRSlower);
		vcrButtonSlower.setBorderPainted(false);
		vcrButtonSlower.setDefaultCapable(false);
		vcrButtonSlower.setToolTipText("Move main time cursor slower");
		minWid = new Dimension(iconVCRSlower.getIconWidth()+4, iconVCRSlower.getIconHeight()+4);
		vcrButtonSlower.setMinimumSize(minWid);
		vcrButtonSlower.setPreferredSize(minWid);
		gbc.gridx = 9;       gbc.gridy = 1;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		overall.add(vcrButtonSlower, gbc);
		vcrButtonSlower.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { vcrClickSlower(); }
		});


		// the single time panel (when time is locked)
		if (timeLocked)
		{
			addMainTimePanel();
		}

		// a drop target for the overall waveform window
		DropTarget dropTarget = new DropTarget(overall, DnDConstants.ACTION_LINK, waveformDropTarget, true);
	}

	/**
	 * Method to return the associated schematics or layout window for this WaveformWindow.
	 * @return the other window that is cross-linked to this.
	 * Returns null if none can be found.
	 */
	private WindowFrame findSchematicsWindow()
	{
		Cell cell = getCell();
		if (cell == null) return null;

		// look for the original cell to highlight it
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			if (wf.getContent().getCell() != cell) continue;
			if (wf.getContent() instanceof EditWindow) return wf;
		}
		return null;
	}

	/**
	 * Method to return the associated schematics or layout window for this WaveformWindow.
	 * @return the other window that is cross-linked to this.
	 * Returns null if none can be found.
	 */
	public static WaveformWindow findWaveformWindow(Cell cell)
	{
		// look for the original cell to highlight it
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			if (wf.getContent().getCell() != cell) continue;
			if (wf.getContent() instanceof WaveformWindow)
				return (WaveformWindow)wf.getContent();
		}
		return null;
	}

	/**
	 * Method called when signal waveforms change, and equivalent should be shown in the edit window.
	 */
	public void showSelectedNetworksInSchematic()
	{
		WindowFrame wf = findSchematicsWindow();
		if (wf == null) return;

		Cell cell = wf.getContent().getCell();
		Netlist netlist = cell.getUserNetlist();

		highlightChangedByWaveform = true;
		Highlight.clear();
		for(Iterator it = this.wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			for(Iterator pIt = wp.waveSignals.values().iterator(); pIt.hasNext(); )
			{
				Signal ws = (Signal)pIt.next();
				if (!ws.highlighted) continue;
				String want = ws.sSig.getSignalName();
		
				JNetwork net = findNetwork(netlist, want);
				if (net != null)
				{
					Highlight.addNetwork(net, cell);
				}
			}
		}
		Highlight.finished();
		highlightChangedByWaveform = false;
	}

	private void addMainTimePanel()
	{
		mainTimePanel = new TimeTickPanel(null, this);
		mainTimePanel.setToolTipText("One time scale applies to all signals when time is locked");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 10;       gbc.gridy = 1;
		gbc.gridwidth = 3;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		overall.add(mainTimePanel, gbc);
	}

	private void removeMainTimePanel()
	{
		overall.remove(mainTimePanel);
		mainTimePanel = null;
	}

	private void togglePanelName()
	{
		if (rebuildingSignalNameList) return;
		String panelName = (String)signalNameList.getSelectedItem();
		int spacePos = panelName.indexOf(' ');
		if (spacePos >= 0) panelName = panelName.substring(spacePos+1);
		int index = TextUtils.atoi(panelName);

		// toggle its state
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (wp.panelNumber == index)
			{
				if (wp.hidden)
				{
					showPanel(wp);
				} else
				{
					hidePanel(wp);
				}
				break;
			}
		}
	}

	private void tick()
	{
		/* see if it is time to advance the VCR */
		long curtime = System.currentTimeMillis();
		if (curtime - vcrLastAdvance < 100) return;
		vcrLastAdvance = curtime;

		if (this.wavePanels.size() == 0) return;
		Panel wp = (Panel)wavePanels.iterator().next();
		double dTime = wp.scaleDeltaXToTime(vcrAdvanceSpeed);
		double newTime = mainTime;
		Rectangle2D bounds = sd.getBounds();
		if (vcrPlayingBackwards)
		{
			newTime -= dTime;
			double lowTime = bounds.getMinX();
			if (newTime <= lowTime)
			{
				newTime = lowTime;
				vcrClickStop();
			}		
		} else
		{
			newTime += dTime;
			double highTime = bounds.getMaxX();
			if (newTime >= highTime)
			{
				newTime = highTime;
				vcrClickStop();
			}		
		}
		setMainTimeCursor(newTime);
		redrawAllPanels();
	}

	private void vcrClickRewind()
	{
		vcrClickStop();
		Rectangle2D bounds = sd.getBounds();
		double lowTime = bounds.getMinX();
		setMainTimeCursor(lowTime);
		redrawAllPanels();
	}

	private void vcrClickPlayBackwards()
	{
		if (vcrTimer == null)
		{
			ActionListener taskPerformer = new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { tick(); }
			};
			vcrTimer = new Timer(100, taskPerformer);
			vcrLastAdvance = System.currentTimeMillis();
			vcrTimer.start();
		}
		vcrPlayingBackwards = true;
	}

	private void vcrClickStop()
	{
		if (vcrTimer == null) return;
		vcrTimer.stop();
		vcrTimer = null;
	}

	private void vcrClickPlay()
	{
		if (vcrTimer == null)
		{
			ActionListener taskPerformer = new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { tick(); }
			};
			vcrTimer = new Timer(100, taskPerformer);
			vcrLastAdvance = System.currentTimeMillis();
			vcrTimer.start();
		}
		vcrPlayingBackwards = false;
	}

	private void vcrClickToEnd()
	{
		vcrClickStop();
		Rectangle2D bounds = sd.getBounds();
		double highTime = bounds.getMaxX();
		setMainTimeCursor(highTime);
		redrawAllPanels();
	}

	private void vcrClickFaster()
	{
		int j = vcrAdvanceSpeed / 4;
		if (j <= 0) j = 1;
		vcrAdvanceSpeed += j;
	}

	private void vcrClickSlower()
	{
		int j = vcrAdvanceSpeed / 4;
		if (j <= 0) j = 1;
		vcrAdvanceSpeed -= j;
		if (vcrAdvanceSpeed <= 0) vcrAdvanceSpeed = 1;
	}

	/**
	 * Method to update the Simulation data for this waveform window.
	 * When new data is read from disk, this is used.
	 * @param sd new simulation data for this window.
	 */
	public void setSimData(Simulation.SimData sd)
	{
		this.sd = sd;
		List panelList = new ArrayList();
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
			panelList.add(it.next());
		for(Iterator it = panelList.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			boolean redoPanel = false;
			for(Iterator pIt = wp.waveSignals.values().iterator(); pIt.hasNext(); )
			{
				Signal ws = (Signal)pIt.next();
				Simulation.SimSignal ss = ws.sSig;
				String oldSigName = "";
				if (ss.getSignalContext() != null) oldSigName = ss.getSignalContext();
				oldSigName += ss.getSignalName();
				ws.sSig = null;
				for(Iterator sIt = sd.getSignals().iterator(); sIt.hasNext(); )
				{
					Simulation.SimSignal newSs = (Simulation.SimSignal)sIt.next();
					String newSigName = "";
					if (newSs.getSignalContext() != null) newSigName = newSs.getSignalContext();
					newSigName += newSs.getSignalName();
					if (!newSigName.equals(oldSigName)) continue;
					newSs.setSignalColor(ss.getSignalColor());
					ws.sSig = newSs;
					break;
				}
				if (ws.sSig == null)
				{
					System.out.println("Could not find signal " + oldSigName + " in the new data");
					redoPanel = true;
				}
			}
			while (redoPanel)
			{
				redoPanel = false;
				for(Iterator pIt = wp.waveSignals.values().iterator(); pIt.hasNext(); )
				{
					Signal ws = (Signal)pIt.next();
					if (ws.sSig == null)
					{
						redoPanel = true;
						wp.signalButtons.remove(ws.sigButton);
						wp.waveSignals.remove(ws.sigButton);
						break;
					}
				}		
			}
			if (wp.waveSignals.size() == 0)
			{
				// removed all signals: delete the panel
				wp.waveWindow.closePanel(wp);
			} else
			{
				if (wp.signalButtons != null)
				{
					wp.signalButtons.validate();
					wp.signalButtons.repaint();
				}
				wp.repaint();
			}
		}
		wf.wantToRedoSignalTree();
		System.out.println("Simulation data refreshed from disk");
	}

	/**
	 * Method to return the top-level JPanel for this WaveformWindow.
	 * The actual WaveformWindow object is below the top level, surrounded by scroll bars and other display artifacts.
	 * @return the top-level JPanel for this WaveformWindow.
	 */
	public JPanel getPanel() { return overall; }

	public void setCell(Cell cell, VarContext context)
	{
		sd.setCell(cell);
		setWindowTitle();
	}

	/**
	 * Method to return the cell that is shown in this window.
	 * @return the cell that is shown in this window.
	 */
	public Cell getCell() { return sd.getCell(); }

	private void rebuildPanelList()
	{
		rebuildingSignalNameList = true;
		signalNameList.removeAllItems();
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next(); 
			signalNameList.addItem("Panel " + Integer.toString(wp.panelNumber) + (wp.hidden ? " (HIDDEN)" : ""));
		}
		signalNameList.setSelectedIndex(0);
		rebuildingSignalNameList = false;
	}

	public void loadExplorerTree(DefaultMutableTreeNode rootNode)
	{
		wf.libraryExplorerNode = null;
		wf.jobExplorerNode = Job.getExplorerTree();
		wf.errorExplorerNode = ErrorLogger.getExplorerTree();
		wf.signalExplorerNode = getSignalsForExplorer();
		rootNode.add(wf.signalExplorerNode);
		rootNode.add(wf.jobExplorerNode);
		rootNode.add(wf.errorExplorerNode);
	}

	public void bottomScrollChanged(int e) {}

	public void rightScrollChanged(int e) {}

	public boolean cellHistoryCanGoBack() { return false; }

	public boolean cellHistoryCanGoForward() { return false; }

	public void cellHistoryGoBack() {}

	public void cellHistoryGoForward() {}

	private DefaultMutableTreeNode getSignalsForExplorer()
	{
		DefaultMutableTreeNode signalsExplorerTree = new DefaultMutableTreeNode("SIGNALS");
		HashMap contextMap = new HashMap();
		contextMap.put("", signalsExplorerTree);
		for(Iterator it = sd.getSignals().iterator(); it.hasNext(); )
		{
			Simulation.SimSignal sSig = (Simulation.SimSignal)it.next();
			DefaultMutableTreeNode thisTree = signalsExplorerTree;
			if (sSig.getSignalContext() != null)
			{
				thisTree = (DefaultMutableTreeNode)contextMap.get(sSig.getSignalContext());
				if (thisTree == null)
				{
					String branchName = sSig.getSignalContext();
					String parent = "";
					int dotPos = branchName.lastIndexOf('.');
					if (dotPos >= 0)
					{
						parent = branchName.substring(0, dotPos);
						branchName = branchName.substring(dotPos+1);
					}
					thisTree = new DefaultMutableTreeNode(branchName);
					contextMap.put(sSig.getSignalContext(), thisTree);
					DefaultMutableTreeNode parentTree = (DefaultMutableTreeNode)contextMap.get(parent);
					if (parentTree != null)
						parentTree.add(thisTree);
				}
			}
			thisTree.add(new DefaultMutableTreeNode(sSig));
		}
		return signalsExplorerTree;
	}

	private Simulation.SimSignal findSignal(String name)
	{
		for(Iterator it = sd.getSignals().iterator(); it.hasNext(); )
		{
			Simulation.SimSignal sSig = (Simulation.SimSignal)it.next();
			String sigName = sSig.getSignalContext();
			if (sigName == null) sigName = sSig.getSignalName(); else
				sigName += sSig.getSignalName();
			if (sigName.equals(name)) return sSig;
		}
		return null;
	}

	private static JNetwork findNetwork(Netlist netlist, String name)
	{
		/*
		 * Should really use extended code, found in "simspicerun.cpp:sim_spice_signalname()"
		 */
		for(Iterator nIt = netlist.getNetworks(); nIt.hasNext(); )
		{
			JNetwork net = (JNetwork)nIt.next();
			if (net.describe().equals(name)) return net;
		}
		return null;
	}

	/**
	 * Method to add a set of JNetworks to the waveform display.
	 * @param nets the Set of JNetworks to add.
	 * @param newPanel true to create new panels for each signal.
	 */
	public void showSignals(Set nets, boolean newPanel)
	{
		// determine the current panel
		Panel wp = null;
		for(Iterator pIt = wavePanels.iterator(); pIt.hasNext(); )
		{
			Panel oWp = (Panel)pIt.next();
			if (oWp.selected)
			{
				wp = oWp;
				break;
			}
		}
		if (!sd.isAnalog()) newPanel = true;
		if (!newPanel && wp == null)
		{
			System.out.println("No current waveform panel to add signals");
			return;
		}

		boolean added = false;
		for(Iterator it = nets.iterator(); it.hasNext(); )
		{
			JNetwork net = (JNetwork)it.next();
			Simulation.SimSignal sSig = sd.findSignalForNetwork(net);
			if (sSig == null) continue;
			Signal subWs = findDisplayedSignal(sSig);
			if (subWs == null)
			{
				// add the signal
				if (newPanel)
				{
					boolean isAnalog = false;
					if (sSig instanceof Simulation.SimAnalogSignal) isAnalog = true;
					wp = new Panel(this, isAnalog);
					if (isAnalog)
					{
						Simulation.SimAnalogSignal as = (Simulation.SimAnalogSignal)sSig;
						double lowValue = 0, highValue = 0;
						for(int i=0; i<as.getNumEvents(); i++)
						{
							double val = as.getValue(i);
							if (i == 0) lowValue = highValue = val; else
							{
								if (val < lowValue) lowValue = val;
								if (val > highValue) highValue = val;
							}
						}
						double range = highValue - lowValue;
						if (range == 0) range = 2;
						double rangeExtra = range / 10;
						wp.setValueRange(lowValue - rangeExtra, highValue + rangeExtra);
					}
				}
				Signal wsig = new Signal(wp, sSig);
				added = true;
				wp.repaint();
			}
		}
		if (added) overall.validate();
	}

	/**
	 * Method to locate a simulation signal in the waveform.
	 * @param sSig the Simulation.SimSignal to locate.
	 * @return the displayed Signal where it is in the waveform window.
	 * Returns null if the signal is not being displayed.
	 */
	public Signal findDisplayedSignal(Simulation.SimSignal sSig)
	{
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			for(Iterator sIt = wp.waveSignals.values().iterator(); sIt.hasNext(); )
			{
				Signal ws = (Signal)sIt.next();
				if (ws.sSig == sSig) return ws;
			}
		}
		return null;
	}

	/**
	 * Method to highlight waveform signals corresponding to circuit networks that are highlighted.
	 */
	public void highlightChanged()
	{
		if (highlightChangedByWaveform) return;

		// start by removing all highlighting in the waveform
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			wp.clearHighlightedSignals();
		}

		Set highSet = Highlight.getHighlightedNetworks();
		if (highSet.size() == 1)
		{
			JNetwork net = (JNetwork)highSet.iterator().next();
			String netName = net.describe();
			Simulation.SimSignal sSig = sd.findSignalForNetwork(net);
			if (sSig == null) return;

			Signal ws = findDisplayedSignal(sSig);
			if (ws != null)
			{
				ws.wavePanel.addHighlightedSignal(ws);
				repaint();
				return;
			}
		}
	}

	/**
	 * Method to get a Set of currently highlighted networks in this WaveformWindow.
	 */
	public Set getHighlightedNetworks()
	{
		// make empty set
		Set nets = new HashSet();

		// if no cell in the window, stop now
		Cell cell = sd.getCell();
		if (cell == null) return nets;
		Netlist netlist = cell.getUserNetlist();

		// look at all signal names in the cell
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();

			// look at all traces in this panel
			for(Iterator sIt = wp.waveSignals.values().iterator(); sIt.hasNext(); )
			{
				Signal ws = (Signal)sIt.next();
				JNetwork net = findNetwork(netlist, ws.sSig.getSignalName());
				if (net != null) nets.add(net);
			}
		}
		return nets;
	}

	// ************************************ TIME ************************************

	public void setMainTimeCursor(double time)
	{
		mainTime = time;
		String amount = convertToEngineeringNotation(mainTime, "s", 9999);
		mainPos.setText("Main: " + amount);
		String diff = convertToEngineeringNotation(Math.abs(mainTime - extTime), "s", 9999);
		delta.setText("Delta: " + diff);
		updateAssociatedLayoutWindow();
	}

	public void setExtensionTimeCursor(double time)
	{
		extTime = time;
		String amount = convertToEngineeringNotation(extTime, "s", 9999);
		extPos.setText("Ext: " + amount);
		String diff = convertToEngineeringNotation(Math.abs(mainTime - extTime), "s", 9999);
		delta.setText("Delta: " + diff);
	}

	/**
	 * Method to set the time range in all panels.
	 * @param minTime the low time value.
	 * @param maxTime the high time value.
	 */
	public void setDefaultTimeRange(double minTime, double maxTime)
	{
		this.minTime = minTime;
		this.maxTime = maxTime;
	}

	private void redrawAllPanels()
	{
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			wp.repaint();
		}
	}

	/**
	 * Method to set the window title.
	 */
	public void setWindowTitle()
	{
		if (wf == null) return;
		/*
		if (sd.getCell() == null)
		{
			wf.setTitle("***WAVEFORM WITH NO CELL***");
			return;
		}

		String title = "Waveform for " + sd.getCell().describe();
		if (sd.getCell().getLibrary() != Library.getCurrent())
			title += " - Current library: " + Library.getCurrent().getName();
			*/
		wf.setTitle(wf.composeTitle(sd.getCell(), "Waveform for "));
	}

	private static class StepSize
	{
		double separation;
		double low, high;
		int rangeScale;
		int stepScale;
	}

	/**
	 * Method to analyze a range of values and determine sensible displayable values.
	 * @param h the high value in the range.
	 * @param l the low value in the range.
	 * @param n the number of steps in the range.
	 * @return a structure that contains the adjusted values of "l" and "h"
	 * as well as the integers rangeScale and stepScale, which are the
	 * powers of 10 that belong to the largest value in the interval and the step size.
	 */
	private static StepSize getSensibleValues(double h, double l, int n)
	{
		StepSize ss = new StepSize();
		ss.low = l;   ss.high = h;
		ss.rangeScale = ss.stepScale = 0;

		double range = Math.max(Math.abs(l), Math.abs(h));
		if (range == 0.0)
		{
			ss.separation = 0;
			return ss;
		}

		// determine powers of ten in the range
		while ( range >= 10.0 ) { range /= 10.0;   ss.rangeScale++; }
		while ( range <= 1.0  ) { range *= 10.0;   ss.rangeScale--; }

		// determine powers of ten in the step size
		double d = Math.abs(h - l)/(double)n;
		if (Math.abs(d/(h+l)) < 0.0000001) d = 0.1f;
		int mp = 0;
		while ( d >= 10.0 ) { d /= 10.0;   mp++;   ss.stepScale++; }
		while ( d <= 1.0  ) { d *= 10.0;   mp--;   ss.stepScale--; }
		double m = Math.pow(10, mp);

		int di = (int)d;
		if (di > 2 && di <= 5) di = 5; else 
			if (di > 5) di = 10;
		int li = (int)(l / m);
		int hi = (int)(h / m);
		li = (li/di) * di;
		hi = (hi/di) * di;
		if (li < 0) li -= di;
		if (hi > 0) hi += di;
		ss.low = (double)li * m;
		ss.high = (double)hi * m;
		ss.separation = di * m;
		return ss;
	}

	private static String prettyPrint(double v, int i1, int i2)
	{
		double d = 1.0;
		if (i2 > 0)
			for(int i = 0; i < i2; i++) d *= 10.0;
		if (i2 < 0)
			for(int i = 0; i > i2; i--) d /= 10.0;

		if (Math.abs(v)*100.0 < d) return "0";

		if (i1 <= 4 && i1 >= 0 && i2 >= 0)
		{
			String s = TextUtils.formatDouble(v, 1);
			return s;
		}
		if (i1 <= 4 && i1 >= -2 && i2 < 0)
		{
			String s = TextUtils.formatDouble(v, -i2);
			return s;
		}

		int p = i1 - 12 - 1;
		if (p < 0) p = 0;
		String s = TextUtils.formatDouble(v/d, p);
		return s + "e" + i2;
	}

	/*
	 * Method to converts a floating point number into engineering units such as pico, micro, milli, etc.
	 * @param time floating point value to be converted to engineering notation.
	 * @param precpower decimal power of necessary time precision.
	 * Use a very large number to ignore this factor (9999).
	 */
	private static String convertToEngineeringNotation(double time, String unit, int precpower)
	{
		String negative = "";
		if (time < 0.0)
		{
			negative = "-";
			time = -time;
		}
		if (GenMath.doublesEqual(time, 0.0)) return "0" + unit;
		if (time < 1.0E-15 || time >= 1000.0) return negative + TextUtils.formatDouble(time) + unit;

		// get proper time unit to use
		double scaled = time * 1.0E17;
		long intTime = Math.round(scaled);
		String secType = null;
		int scalePower = 0;
		if (scaled < 200000.0 && intTime < 100000)
		{
			secType = "f" + unit;
			scalePower = -15;
		} else
		{
			scaled = time * 1.0E14;   intTime = Math.round(scaled);
			if (scaled < 200000.0 && intTime < 100000)
			{
				secType = "p" + unit;
				scalePower = -12;
			} else
			{
				scaled = time * 1.0E11;   intTime = Math.round(scaled);
				if (scaled < 200000.0 && intTime < 100000)
				{
					secType = "n" + unit;
					scalePower = -9;
				} else
				{
					scaled = time * 1.0E8;   intTime = Math.round(scaled);
					if (scaled < 200000.0 && intTime < 100000)
					{
						secType = "u" + unit;
						scalePower = -6;
					} else
					{
						scaled = time * 1.0E5;   intTime = Math.round(scaled);
						if (scaled < 200000.0 && intTime < 100000)
						{
							secType = "m" + unit;
							scalePower = -3;
						} else
						{
							scaled = time * 1.0E2;  intTime = Math.round(scaled);
							secType = unit;
							scalePower = 0;
						}
					}
				}
			}
		}
		if (precpower >= scalePower)
		{
			long timeleft = intTime / 100;
			long timeright = intTime % 100;
			if (timeright == 0)
			{
				return negative + timeleft + secType;
			} else
			{
				if ((timeright%10) == 0)
				{
					return negative + timeleft + "." + timeright/10 + secType;
				} else
				{
					return negative + timeleft + "." + timeright + secType;
				}
			}
		}
		scaled /= 1.0E2;
		String numPart = TextUtils.formatDouble(scaled, scalePower - precpower);
		while (numPart.endsWith("0")) numPart = numPart.substring(0, numPart.length()-1);
		if (numPart.endsWith(".")) numPart = numPart.substring(0, numPart.length()-1);
		return negative + numPart + secType;
	}

	// ************************************ SHOWING CROSS-PROBED LEVELS IN EDITWINDOW ************************************

	private HashMap netValues;

	/**
	 * Method to update associated layout windows when the main cursor changes.
	 */
	private void updateAssociatedLayoutWindow()
	{
		// this only works for digital simulation
		if (sd.isAnalog()) return;

		// make sure there is a layout/schematic window being simulated
		WindowFrame oWf = findSchematicsWindow();
		if (oWf == null) return;
		EditWindow schemWnd = (EditWindow)oWf.getContent();

		boolean crossProbeChanged = schemWnd.hasCrossProbeData();
		schemWnd.clearCrossProbeLevels();

		Cell cell = getCell();
		Netlist netlist = cell.getUserNetlist();

		// reset all values on networks
		netValues = new HashMap();

		// assign values from simulation window traces to networks
		for(Iterator it = this.wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (wp.hidden) continue;
			for(Iterator sIt = wp.waveSignals.values().iterator(); sIt.hasNext(); )
			{
				Signal ws = (Signal)sIt.next();
				Simulation.SimDigitalSignal ds = (Simulation.SimDigitalSignal)ws.sSig;
				List bussedSignals = ds.getBussedSignals();
				if (bussedSignals != null)
				{
					// a digital bus trace
					int busWidth = bussedSignals.size();
					for(Iterator bIt = bussedSignals.iterator(); bIt.hasNext(); )
					{
						Simulation.SimDigitalSignal subDS = (Simulation.SimDigitalSignal)bIt.next();
						putValueOnTrace(subDS, cell, netValues, netlist);
					}
				} else
				{
					// single signal
					putValueOnTrace(ds, cell, netValues, netlist);
				}
			}
		}

		// light up any simulation-probe objects
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getProto() != Generic.tech.simProbeNode) continue;
			JNetwork net = null;
			for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
			{
				Connection con = (Connection)cIt.next();
				net = netlist.getNetwork(con.getArc(), 0);
				break;
			}

			if (net == null) continue;
			Integer state = (Integer)netValues.get(net);
			if (state == null) continue;
			Color col = getHighlightColor(state.intValue());
			schemWnd.addCrossProbeBox(ni.getBounds(), col);
			crossProbeChanged = true;
			netValues.remove(net);
		}

		// redraw all arcs in the layout/schematic window
		for(Iterator it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			int width = netlist.getBusWidth(ai);
			for(int i=0; i<width; i++)
			{
				JNetwork net = netlist.getNetwork(ai, i);
				Integer state = (Integer)netValues.get(net);
				if (state == null) continue;
				Color col = getHighlightColor(state.intValue());
				schemWnd.addCrossProbeLine(ai.getHead().getLocation(), ai.getTail().getLocation(), col);
				crossProbeChanged = true;
			}
		}

		// if anything changed, queue the window for redisplay
		if (crossProbeChanged)
			schemWnd.repaint();
	}

	/**
	 * Method to convert a digital state to a color.
	 * The color is used when showing cross-probed levels in the EditWindow.
	 * The colors used to be user-selectable, but are not yet.
	 * @param state the digital state from the Waveform Window.
	 * @return the color to display in the EditWindow.
	 */
	private Color getHighlightColor(int state)
	{
//		if ((sim_window_state&FULLSTATE) != 0)
//		{
//			/* 12-state display: determine trace texture */
//			strength = state & 0377;
//			if (strength == 0) *texture = -1; else
//				if (strength <= NODE_STRENGTH) *texture = 1; else
//					if (strength <= GATE_STRENGTH) *texture = 0; else
//						*texture = 2;
//
//			/* determine trace color */
//			switch (state >> 8)
//			{
//				case LOGIC_LOW:  *color = sim_colorlevellow;     break;
//				case LOGIC_X:    *color = sim_colorlevelundef;   break;
//				case LOGIC_HIGH: *color = sim_colorlevelhigh;    break;
//				case LOGIC_Z:    *color = sim_colorlevelzdef;    break;
//			}
//		} else
		{
			/* 2-state display */
			if ((state & Simulation.SimData.LOGIC) == Simulation.SimData.LOGIC_HIGH) return Color.RED;
			return Color.BLACK;
		}
	}

	private void putValueOnTrace(Simulation.SimDigitalSignal ds, Cell cell, HashMap netValues, Netlist netlist)
	{
		// set simulation value on the network in the associated layout/schematic window
		JNetwork net = findNetwork(netlist, ds.getSignalName());
		if (net == null) return;

		// find the proper data for the time of the main cursor
		int numEvents = ds.getNumEvents();
		int state = 0;
		for(int i=0; i<numEvents; i++)
		{
			double time = ds.getTime(i);
			if (mainTime < time)
			{
				state = ds.getState(i) & Simulation.SimData.LOGIC;
				break;
			}
		}
		netValues.put(net, new Integer(state));	
	}

	// ************************************ PANEL CONTROL ************************************

	/**
	 * Method called when a Panel is to be closed.
	 * @param wp the Panel to close.
	 */
	public void closePanel(Panel wp)
	{
		left.remove(wp.leftHalf);
		right.remove(wp.rightHalf);
		wavePanels.remove(wp);
		rebuildPanelList();
		overall.validate();
		redrawAllPanels();
	}

	/**
	 * Method called when a Panel is to be hidden.
	 * @param wp the Panel to hide.
	 */
	public void hidePanel(Panel wp)
	{
		if (wp.hidden) return;
		wp.hidden = true;
		left.remove(wp.leftHalf);
		right.remove(wp.rightHalf);
		rebuildPanelList();
		overall.validate();
		redrawAllPanels();
	}

	/**
	 * Method called when a Panel is to be shown.
	 * @param wp the Panel to show.
	 */
	public void showPanel(Panel wp)
	{
		if (!wp.hidden) return;
		wp.hidden = false;
		left.add(wp.leftHalf);
		right.add(wp.rightHalf);
		rebuildPanelList();
		overall.validate();
		redrawAllPanels();
	}
	
	/**
	 * Method called to grow or shrink the panels vertically.
	 */
	public void growPanels(double scale)
	{
		panelSizeDigital = (int)(panelSizeDigital * scale);
		panelSizeAnalog = (int)(panelSizeAnalog * scale);
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			Dimension sz = wp.getSize();
			sz.height = (int)(sz.height * scale);
			wp.setSize(sz.width, sz.height);
			wp.setMinimumSize(sz);
			wp.setPreferredSize(sz);

			if (wp.signalButtonsPane != null)
			{
				sz = wp.signalButtonsPane.getSize();
				sz.height = (int)(sz.height * scale);
				wp.signalButtonsPane.setPreferredSize(sz);
				wp.signalButtonsPane.setSize(sz.width, sz.height);
			} else
			{
				sz = wp.leftHalf.getSize();
				sz.height = (int)(sz.height * scale);
				wp.leftHalf.setPreferredSize(sz);
				wp.leftHalf.setMinimumSize(sz);
				wp.leftHalf.setSize(sz.width, sz.height);
			}
		}
		overall.validate();
		redrawAllPanels();
	}

	/**
	 * Method called to toggle the panel time lock.
	 */
	public void togglePanelTimeLock()
	{
		timeLocked = ! timeLocked;
		if (timeLocked)
		{
			// time now locked: add main time, remove individual time
			timeLock.setIcon(iconLockTime);
			addMainTimePanel();
			double minTime = 0, maxTime = 0;
			boolean first = true;
			for(Iterator it = wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				wp.removeTimePanel();
				if (first)
				{
					first = false;
					minTime = wp.minTime;
					maxTime = wp.maxTime;
				} else
				{
					if (wp.minTime < minTime)
					{
						minTime = wp.minTime;
						maxTime = wp.maxTime;
					}
				}
			}

			// force all panels to be at the same time
			for(Iterator it = wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				if (wp.minTime != minTime || wp.maxTime != maxTime)
				{
					wp.minTime = minTime;
					wp.maxTime = maxTime;
//					wp.repaintWithTime();
				}
			}
		} else
		{
			// time is unlocked: put a time bar in each panel, remove main panel
			timeLock.setIcon(iconUnLockTime);
			for(Iterator it = wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				wp.addTimePanel();
			}
			removeMainTimePanel();
		}
		overall.validate();
		overall.repaint();
	}

	/**
	 * Method to refresh the simulation data from disk.
	 */
	public void refreshData()
	{
		if (sd.getDataType() == null)
		{
			System.out.println("This simulation data did not come from disk...cannot refresh");
			return;
		}
		Simulate.plotSimulationResults(sd.getDataType(), sd.getCell(), sd.getFileURL(), this);
	}

	/**
	 * Method to save the signal ordering on the cell.
	 */
	private void saveSignalOrder()
	{
		Cell cell = getCell();
		if (cell == null) return;
		new SaveSignalOrder(cell, this);
	}

	/**
	 * This class saves the signal order on the cell.
	 */
	private static class SaveSignalOrder extends Job
	{
		private Cell cell;
		private WaveformWindow ww;

		private SaveSignalOrder(Cell cell, WaveformWindow ww)
		{
			super("Save Signal Order", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.ww = ww;
			startJob();
		}

		public boolean doIt()
		{
			List signalList = new ArrayList();
			for(Iterator it = ww.wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				StringBuffer sb = new StringBuffer();
				boolean first = true;
				for(Iterator sIt = wp.waveSignals.values().iterator(); sIt.hasNext(); )
				{
					Signal ws = (Signal)sIt.next();
					String sigName = ws.sSig.getSignalName();
					if (ws.sSig.getSignalContext() != null) sigName = ws.sSig.getSignalContext() + "." + sigName;
					if (first) first = false; else
						sb.append("\t");
					sb.append(sigName);
				}
				if (!first)
					signalList.add(sb.toString());
			}
			if (signalList.size() == 0)
			{
				if (cell.getVar(WINDOW_SIGNAL_ORDER) != null)
					cell.delVar(WINDOW_SIGNAL_ORDER);
			} else
			{
				String [] strings = new String[signalList.size()];
				int i = 0;
				for(Iterator it = signalList.iterator(); it.hasNext(); )
					strings[i++] = (String)it.next();
				cell.newVar(WINDOW_SIGNAL_ORDER, strings);
			}
			return true;
		}
	}

	/**
	 * Method called when a new Panel is to be created.
	 */
	private void addNewPanel()
	{
		boolean isAnalog = sd.isAnalog();
		if (isAnalog)
		{
			WaveformWindow.Panel wp = new WaveformWindow.Panel(this, isAnalog);
			Rectangle2D bounds = sd.getBounds();
			double lowValue = bounds.getMinY();
			double highValue = bounds.getMaxY();
			wp.setValueRange(lowValue, highValue);
			wp.makeSelectedPanel();
		}
		getPanel().validate();
	}

	public void addSignal(Simulation.SimSignal sig)
	{
		if (sig instanceof Simulation.SimAnalogSignal)
		{
			// add analog signal on top of current panel
			for(Iterator it = wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				if (wp.selected)
				{
					wp.addSignalToPanel(sig);
					return;
				}
			}
		} else
		{
			// add digital signal in new panel
			Panel wp = new Panel(this, false);
			Signal wsig = new Signal(wp, sig);
			overall.validate();
			wp.repaint();
			saveSignalOrder();
		}
	}

	/**
	 * Method to delete the selected signals.
	 */
	public void deleteSelectedSignals()
	{
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (!wp.selected) continue;
			if (wp.isAnalog) deleteSignalFromPanel(wp); else
				wp.closePanel();
			break;
		}
		saveSignalOrder();
	}

	/**
	 * Method called to delete the highlighted signal from its Panel.
	 * @param wp the Panel with the signal to be deleted.
	 */
	public void deleteSignalFromPanel(Panel wp)
	{
		boolean found = true;
		while (found)
		{
			found = false;
			for(Iterator it = wp.waveSignals.values().iterator(); it.hasNext(); )
			{
				Signal ws = (Signal)it.next();
				if (!ws.highlighted) continue;
				wp.removeHighlightedSignal(ws);
				wp.signalButtons.remove(ws.sigButton);
				wp.waveSignals.remove(ws.sigButton);
				found = true;
				break;
			}
		}
		wp.signalButtons.validate();
		wp.signalButtons.repaint();
		wp.repaint();
		saveSignalOrder();
	}

	/**
	 * Method called to delete all signals from a Panel.
	 * @param wp the Panel to clear.
	 */
	public void deleteAllSignalsFromPanel(Panel wp)
	{
		wp.clearHighlightedSignals();
		wp.signalButtons.removeAll();
		wp.signalButtons.validate();
		wp.signalButtons.repaint();
		wp.waveSignals.clear();
		wp.repaint();
		saveSignalOrder();
	}

	public void fillScreen()
	{
		Rectangle2D bounds = sd.getBounds();
		double lowTime = bounds.getMinX();
		double highTime = bounds.getMaxX();
		double lowValue = bounds.getMinY();
		double highValue = bounds.getMaxY();
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (!timeLocked && !wp.selected) continue;

			boolean repaint = false;
			if (wp.minTime != lowTime || wp.maxTime != highTime)
			{
				wp.minTime = lowTime;
				wp.maxTime = highTime;
				repaint = true;
			}
			if (wp.isAnalog)
			{
				if (wp.minTime != lowValue || wp.maxTime != highValue)
				{
					wp.setValueRange(lowValue, highValue);
					repaint = true;
				}
			}
			if (repaint)
			{
				wp.repaintWithTime();
			}
		}
	}

	public void zoomOutContents()
	{
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (!timeLocked && !wp.selected) continue;

			boolean repaint = false;
			double range = wp.maxTime - wp.minTime;
			wp.minTime -= range/2;
			wp.maxTime += range/2;
			wp.repaintWithTime();
		}
	}

	public void zoomInContents()
	{
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (!timeLocked && !wp.selected) continue;

			boolean repaint = false;
			double range = wp.maxTime - wp.minTime;
			wp.minTime += range/4;
			wp.maxTime -= range/4;
			wp.repaintWithTime();
		}
	}

	public void focusOnHighlighted()
	{
		if (mainTime == extTime) return;
		double maxTime, minTime;
		if (mainTime > extTime)
		{
			double size = (mainTime-extTime) / 20.0;
			maxTime = mainTime + size;
			minTime = extTime - size;
		} else
		{
			double size = (extTime-mainTime) / 20.0;
			maxTime = extTime + size;
			minTime = mainTime - size;
		}
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (!timeLocked && !wp.selected) continue;
			if (wp.minTime != minTime || wp.maxTime != maxTime)
			{
				wp.minTime = minTime;
				wp.maxTime = maxTime;
				wp.repaintWithTime();
			}
		}
	}

	public void finished()
	{
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			wp.finished();
		}
	}

	public void fullRepaint() { repaint(); }

	public void repaint()
	{
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			wp.repaint();
		}
		if (mainTimePanel != null)
			mainTimePanel.repaint();
	}

	public void fireCellHistoryStatus()
	{
	}

	/**
	 * Method to initialize for a new text search.
	 * @param search the string to locate.
	 * @param caseSensitive true to match only where the case is the same.
	 */
	public void initTextSearch(String search, boolean caseSensitive) {}

	/**
	 * Method to find the next occurrence of a string.
	 * @param reverse true to find in the reverse direction.
	 * @return true if something was found.
	 */
	public boolean findNextText(boolean reverse) { return false; }

	/**
	 * Method to replace the text that was just selected with findNextText().
	 * @param replace the new text to replace.
	 */
	public void replaceText(String replace) {}

	/**
	 * Method to replace all selected text.
	 * @param replace the new text to replace everywhere.
	 */
	public void replaceAllText(String replace) {}

}
