/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: HorizRuler.java
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
package com.sun.electric.tool.user.waveform;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.ClickZoomWireListener;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

// ************************************* RULER ALONG THE TOP OF EACH PANEL *************************************

/**
 * This class defines the horizontal ruler display at the top of each Panel.
 */
public class HorizRuler extends JPanel implements MouseListener
{
	Panel wavePanel;
	WaveformWindow waveWindow;

	// constructor
	HorizRuler(Panel wavePanel, WaveformWindow waveWindow)
	{
		// remember state
		this.wavePanel = wavePanel;
		this.waveWindow = waveWindow;

		// setup this panel window
		Dimension sz = new Dimension(16, 20);
		setMinimumSize(sz);
		setPreferredSize(sz);

		addMouseListener(this);

		// a drop target for the ruler panel
		new DropTarget(this, DnDConstants.ACTION_LINK, WaveformWindow.waveformDropTarget, true);
	}

	/**
	 * Method to repaint this HorizRulerPanel.
	 */
	public void paint(Graphics g)
	{
		Dimension sz = getSize();
		int wid = sz.width;
		int hei = sz.height;
		int offX = 0;
		Panel drawHere = wavePanel;
		Signal xAxisSig = waveWindow.getXAxisSignalAll();
		if (drawHere != null)
		{
			xAxisSig = drawHere.getXAxisSignal();
		} else
		{
			// this is the main horizontal ruler panel for all panels
			Point screenLoc = getLocationOnScreen();
			offX = waveWindow.getScreenLowX() - screenLoc.x;
			int newWid = waveWindow.getScreenHighX() - waveWindow.getScreenLowX();

			// because the main horizontal ruler panel needs a Panel (won't work if there aren't any)
			// have to do complex things to request a repaint after adding the first Panel
			if (newWid == 0 || waveWindow.getPanelList().size() == 0)
			{
				if (waveWindow.isMainHorizRulerNeedsRepaint())
					repaint();
				return;
			}

			if (offX + newWid > wid) newWid = wid - offX;
			wid = newWid;

			drawHere = (Panel)waveWindow.getPanelList().get(0);
			waveWindow.setMainHorizRulerNeedsRepaint(false);
			g.setClip(offX, 0, wid, hei);
		}

		// draw the background
		g.setColor(new Color(User.getColorWaveformBackground()));
		g.fillRect(offX, 0, wid, hei);

		// draw the name of the signal on the horizontal ruler axis
		g.setColor(new Color(User.getColorWaveformForeground()));
		g.setFont(waveWindow.getFont());
		String xAxisName = "Time";
		if (xAxisSig != null) xAxisName = xAxisSig.getSignalName();
		g.drawLine(drawHere.getVertAxisPos() + offX, hei-1, wid+offX, hei-1);
		g.drawString(xAxisName, offX+1, hei-6);

		// draw the ruler ticks
		double displayedLow = drawHere.convertXScreenToData(drawHere.getVertAxisPos());
		double displayedHigh = drawHere.convertXScreenToData(wid);
		StepSize ss = StepSize.getSensibleValues(displayedHigh, displayedLow, 10);
		if (ss.separation == 0.0) return;
		double xValue = ss.low;
		int lastX = -1;
		for(;;)
		{
			if (xValue > ss.high) break;
			if (xValue >= displayedLow)
			{
				int x = drawHere.convertXDataToScreen(xValue) + offX;
				g.drawLine(x, 0, x, hei);
				if (lastX >= 0)
				{
					if (x - lastX > 100)
					{
						// add 5 tick marks
						for(int i=1; i<5; i++)
						{
							int intX = (x - lastX) / 5 * i + lastX;
							g.drawLine(intX, hei/2, intX, hei);
						}
					} else if (x - lastX > 25)
					{
						// add 1 tick mark
						int intX = (x - lastX) / 2 + lastX;
						g.drawLine(intX, hei/2, intX, hei);
					}
				}
				String xValueVal = TextUtils.convertToEngineeringNotation(xValue, "s", ss.stepScale);
				g.drawString(xValueVal, x+2, hei-2);
				lastX = x;
			}
			xValue += ss.separation;
		}
	}

	/**
	 * the MouseListener events for the horizontal ruler panel
	 */
	public void mousePressed(MouseEvent evt)
	{
		if (!ClickZoomWireListener.isRightMouse(evt)) return;
		waveWindow.vcrClickStop();

		// right click in horizontal ruler area: show popup of choices
		JPopupMenu menu = new JPopupMenu();
		JMenuItem item = new JMenuItem("Linear");
		item.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { makeLinear(); } });
		menu.add(item);
		item = new JMenuItem("Logarithmic");
		item.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { makeLogarithmic(); } });
		menu.add(item);
		menu.addSeparator();
		item = new JMenuItem("Make the X axis show Time");
		item.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { restoreTime(); } });
		menu.add(item);

		menu.show(this, evt.getX(), evt.getY());
	}

	public void mouseReleased(MouseEvent evt) {}
	public void mouseClicked(MouseEvent evt) {}
	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}

	/**
	 * Make this panel show a linear X axis.
	 */
	private void makeLinear()
	{
		if (waveWindow.isXAxisLocked())
		{
			waveWindow.setWaveWindowLogarithmic(false);
			waveWindow.redrawAllPanels();
		} else
		{
			wavePanel.setPanelLogarithmic(false);
			wavePanel.repaint();
		}
	}

	/**
	 * Make this panel show a logarithmic X axis.
	 */
	private void makeLogarithmic()
	{
		if (waveWindow.isXAxisLocked())
		{
			waveWindow.setWaveWindowLogarithmic(true);
			waveWindow.redrawAllPanels();
		} else
		{
			wavePanel.setPanelLogarithmic(true);
			wavePanel.repaint();
		}
	}

	/**
	 * Make this panel show a time in the X axis.
	 */
	private void restoreTime()
	{
		Rectangle2D dataBounds = waveWindow.getSimData().getBounds();
		double lowXValue = dataBounds.getMinX();
		double highXValue = dataBounds.getMaxX();

		for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (!waveWindow.isXAxisLocked() && wp != wavePanel) continue;
			wp.setXAxisSignal(null);
			wp.setXAxisRange(lowXValue, highXValue);
			if (wp.getHorizRuler() != null) wp.getHorizRuler().repaint();
			wp.repaint();
		}
		if (waveWindow.isXAxisLocked())
		{
			waveWindow.setXAxisSignalAll(null);
			waveWindow.getMainHorizRuler().repaint();
			waveWindow.redrawAllPanels();
		}
	}
}
