/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CircuitChanges.java
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
package com.sun.electric.tool.user;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.constraint.Layout;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.tool.user.dialogs.ChangeCurrentLib;
import com.sun.electric.tool.user.menus.MenuCommands;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.StatusBar;
import com.sun.electric.tool.user.ui.ToolBar;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WaveformWindow;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

/**
 * Class for user-level changes to the circuit.
 */
public class CircuitChanges
{
	// constructor, never used
	CircuitChanges() {}

	/****************************** NODE TRANSFORMATION ******************************/

	private static double lastRotationAmount = 90;

    /**
	 * Method to handle the command to rotate the selected objects by an amount.
	 * @param amount the amount to rotate.  If the amount is zero, prompt for an amount.
	 */
	public static void rotateObjects(int amount)
	{
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;

		// disallow rotating if lock is on
		if (cantEdit(cell, null, true) != 0) return;

		// if zero rotation, prompt for amount
		if (amount == 0)
		{
			String val = JOptionPane.showInputDialog("Amount to rotate", new Double(lastRotationAmount));
			if (val == null) return;
			double fAmount = TextUtils.atof(val);
			if (fAmount == 0)
			{
				System.out.println("Null rotation amount");
				return;
			}
			lastRotationAmount = fAmount;
			amount = (int)(fAmount * 10);
		}

		RotateSelected job = new RotateSelected(cell, MenuCommands.getSelectedObjects(true, true), amount, false, false);
	}

	/**
	 * Method to handle the command to mirror the selected objects.
	 * @param horizontally true to mirror horizontally (about the horizontal, flipping the Y value).
	 * False to mirror vertically (about the vertical, flipping the X value).
	 */
	public static void mirrorObjects(boolean horizontally)
	{
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;
        Cell cell = wf.getContent().getCell();

		if (cell == null) return;

		// disallow rotating if lock is on
		if (cantEdit(cell, null, true) != 0) return;

		RotateSelected job = new RotateSelected(cell, MenuCommands.getSelectedObjects(true, true), 0, true, horizontally);
	}
	
	private static class RotateSelected extends Job
	{
		private Cell cell;
		private int amount;
		private boolean mirror;
        private boolean mirrorH;
        private List highs;

        /**
         * @param cell
         * @param highs the highlighted objects (list of highlights)
         * @param amount angle in tenth degrees to rotate
         * @param mirror whether or not to mirror. if true, amount is ignored, and mirrorH is used.
         * @param mirrorH if true, mirror horizontally (flip over X-axis), otherwise mirror
         * vertically (flip over Y-axis). Ignored if mirror is false.
         */
		protected RotateSelected(Cell cell, List highs, int amount, boolean mirror, boolean mirrorH)
		{
			super("Rotate selected objects", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.amount = amount;
			this.mirror = mirror;
			this.mirrorH = mirrorH;
            this.highs = highs;
			startJob();
		}

		public boolean doIt()
		{
			// figure out which nodes get rotated/mirrored
			HashSet markObj = new HashSet();
			int nicount = 0;
			NodeInst theNi = null;
			Rectangle2D selectedBounds = new Rectangle2D.Double();
			for(Iterator it = highs.iterator(); it.hasNext(); )
			{
				Geometric geom = (Geometric)it.next();
				if (!(geom instanceof NodeInst)) continue;
				NodeInst ni = (NodeInst)geom;
				if (cantEdit(cell, ni, true) != 0)
				{
					return false;
				}
				markObj.add(ni);
				if (nicount == 0)
				{
					selectedBounds.setRect(ni.getBounds());
				} else
				{
					Rectangle2D.union(selectedBounds, ni.getBounds(), selectedBounds);
				}
				theNi = ni;
				nicount++;
			}

			// must be at least 1 node
			if (nicount <= 0)
			{
				System.out.println("Must select at least 1 node for rotation");
				return false;
			}

			// if multiple nodes, find the center one
			if (nicount > 1)
			{
				Point2D center = new Point2D.Double(selectedBounds.getCenterX(), selectedBounds.getCenterY());
				theNi = null;
				double bestdist = Integer.MAX_VALUE;
				for(Iterator it = cell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = (NodeInst)it.next();
					if (!markObj.contains(ni)) continue;
					double dist = center.distance(ni.getTrueCenter());

					// LINTED "bestdist" used in proper order
					if (theNi == null || dist < bestdist)
					{
						theNi = ni;
						bestdist = dist;
					}
				}
			}

			// see which nodes already connect to the main rotation/mirror node (theNi)
			markObj.clear();
			markObj.add(theNi);
			for(Iterator it = highs.iterator(); it.hasNext(); )
			{
				Geometric geom = (Geometric)it.next();
				if (!(geom instanceof ArcInst)) continue;
				ArcInst ai = (ArcInst)geom;
				markObj.add(ai);
			}
			spreadRotateConnection(theNi, markObj);

			// now make sure that it is all connected
			List niList = new ArrayList();
			List aiList = new ArrayList();
			for(Iterator it = highs.iterator(); it.hasNext(); )
			{
				Geometric geom = (Geometric)it.next();
				if (!(geom instanceof NodeInst)) continue;
				NodeInst ni = (NodeInst)geom;
				if (ni == theNi) continue;
				if (markObj.contains(ni)) continue;

				if (theNi.getNumPortInsts() == 0)
				{
					// no port on the cell: create one
					Cell subCell = (Cell)theNi.getProto();
					NodeInst subni = NodeInst.makeInstance(Generic.tech.universalPinNode, new Point2D.Double(0,0), 0, 0, subCell);
					if (subni == null) break;
					Export thepp = Export.newInstance(subCell, subni.getOnlyPortInst(), "temp");
					if (thepp == null) break;

					// add to the list of temporary nodes
					niList.add(subni);
				}
				PortInst thepi = theNi.getPortInst(0);
				if (ni.getNumPortInsts() != 0)
				{
					ArcInst ai = ArcInst.makeInstance(Generic.tech.invisible_arc, 0, ni.getPortInst(0), thepi);
					if (ai == null) break;
					ai.setRigid(true);
					aiList.add(ai);
					spreadRotateConnection(ni, markObj);
				}
			}

			// make all selected arcs temporarily rigid
			for(Iterator it = highs.iterator(); it.hasNext(); )
			{
				Geometric geom = (Geometric)it.next();
				if (!(geom instanceof ArcInst)) continue;
				Layout.setTempRigid((ArcInst)geom, true);
			}

			// do the rotation/mirror
			if (mirror)
			{
				// do mirroring
				if (mirrorH)
				{
					// mirror horizontally (flip Y)
					double sY = theNi.getYSizeWithMirror();
					theNi.modifyInstance(0, 0, 0, -sY - sY, 0);
				} else
				{
					// mirror vertically (flip X)
					double sX = theNi.getXSizeWithMirror();
					theNi.modifyInstance(0, 0, -sX - sX, 0, 0);
				}
			} else
			{
				// do rotation
				theNi.modifyInstance(0, 0, 0, 0, amount);
			}

			// delete intermediate arcs used to constrain
			for(Iterator it = aiList.iterator(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				ai.kill();
			}

			// delete intermediate nodes used to constrain
			for(Iterator it = niList.iterator(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
//				(void)killportproto(niList[i]->parent, niList[i]->firstportexpinst->exportproto);
				ni.kill();
			}
			return true;
		}
	}

	/**
	 * Helper method for rotation to mark selected nodes that need not be
	 * connected with an invisible arc.
	 */
	private static void spreadRotateConnection(NodeInst theNi, HashSet markObj)
	{
		for(Iterator it = theNi.getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			ArcInst ai = con.getArc();
			if (!markObj.contains(ai)) continue;
			Connection other = ai.getTail();
			if (other == con) other = ai.getHead();
			NodeInst ni = other.getPortInst().getNodeInst();
			if (markObj.contains(ni)) continue;
			markObj.add(ni);
			spreadRotateConnection(ni, markObj);
		}
	}

	/****************************** NODE ALIGNMENT ******************************/

	/**
	 * Method to align the selected objects to the grid.
	 */
	public static void alignToGrid()
	{
		AlignObjects job = new AlignObjects(MenuCommands.getSelectedObjects(true, true));
	}

	/**
	 * This class implement the command to delete unused old versions of cells.
	 */
	private static class AlignObjects extends Job
	{
        private List list;          // list of highlighted objects to align

		protected AlignObjects(List highs)
		{
			super("Align Objects", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.list = highs;
			startJob();
		}

		public boolean doIt()
		{
			if (list.size() == 0)
			{
				System.out.println("Must select something before aligning it to the grid");
				return false;
			}
			double alignment_ratio = User.getAlignmentToGrid();
			if (alignment_ratio <= 0)
			{
				System.out.println("No alignment given: set Alignment Options first");
				return false;
			}

			// first adjust the nodes
			int adjustedNodes = 0;
			for(Iterator it = list.iterator(); it.hasNext(); )
			{
				Geometric geom = (Geometric)it.next();
				if (!(geom instanceof NodeInst)) continue;
				NodeInst ni = (NodeInst)geom;
	
				// ignore pins
				if (ni.getFunction() == PrimitiveNode.Function.PIN) continue;
				Point2D center = new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY());
				EditWindow.gridAlign(center);
				double bodyXOffset = center.getX() - ni.getAnchorCenterX();
				double bodyYOffset = center.getY() - ni.getAnchorCenterY();
	
				double portXOffset = bodyXOffset;
				double portYOffset = bodyYOffset;
				boolean mixedportpos = false;
				boolean firstPort = true;
				for(Iterator pIt = ni.getPortInsts(); pIt.hasNext(); )
				{
					PortInst pi = (PortInst)pIt.next();
					Poly poly = pi.getPoly();
					Point2D portCenter = new Point2D.Double(poly.getCenterX(), poly.getCenterY());
					EditWindow.gridAlign(portCenter);
					double pXO = portCenter.getX() - poly.getCenterX();
					double pYO = portCenter.getY() - poly.getCenterY();
					if (firstPort)
					{
						firstPort = false;
						portXOffset = pXO;   portYOffset = pYO;
					} else
					{
						if (portXOffset != pXO || portYOffset != pYO) mixedportpos = true;
					}
				}
				if (!mixedportpos)
				{
					bodyXOffset = portXOffset;   bodyYOffset = portYOffset;
				}
	
				// if a primitive has an offset, see if the node edges are aligned
				if (bodyXOffset != 0 || bodyYOffset != 0)
				{
					if (ni.getProto() instanceof PrimitiveNode)
					{
						AffineTransform transr = ni.rotateOut();
						Technology tech = ni.getProto().getTechnology();
						Poly [] polyList = tech.getShapeOfNode(ni);
						for(int j=0; j<polyList.length; j++)
						{
							Poly poly = polyList[j];
							poly.transform(transr);
							Rectangle2D bounds = poly.getBox();
							if (bounds == null) continue;
							Point2D polyPoint1 = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
							Point2D polyPoint2 = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
							EditWindow.gridAlign(polyPoint1);
							EditWindow.gridAlign(polyPoint2);
							if (polyPoint1.getX() == bounds.getMinX() &&
								polyPoint2.getX() == bounds.getMaxX()) bodyXOffset = 0;
							if (polyPoint1.getY() == bounds.getMinY() &&
								polyPoint2.getY() == bounds.getMaxY()) bodyYOffset = 0;
							if (bodyXOffset == 0 && bodyYOffset == 0) break;
						}
					}
				}
	
				// move the node
				if (bodyXOffset != 0 || bodyYOffset != 0)
				{
					// turn off all constraints on arcs
					HashMap constraints = new HashMap();
					for(Iterator aIt = ni.getConnections(); aIt.hasNext(); )
					{
						Connection con = (Connection)aIt.next();
						ArcInst ai = con.getArc();
						int constr = 0;
						if (ai.isRigid()) constr |= 1;
						if (ai.isFixedAngle()) constr |= 2;
						constraints.put(ai, new Integer(constr));
						ai.setRigid(false);
						ai.setFixedAngle(false);
					}
					ni.modifyInstance(bodyXOffset, bodyYOffset, 0, 0, 0);
					adjustedNodes++;
	
					// restore arc constraints
					for(Iterator aIt = ni.getConnections(); aIt.hasNext(); )
					{
						Connection con = (Connection)aIt.next();
						ArcInst ai = con.getArc();
						Integer constr = (Integer)constraints.get(ai);
						if (constr == null) continue;
						if ((constr.intValue() & 1) != 0) ai.setRigid(true);
						if ((constr.intValue() & 2) != 0) ai.setFixedAngle(true);
					}
				}
			}
	
			// now adjust the arcs
			int adjustedArcs = 0;
			for(Iterator it = list.iterator(); it.hasNext(); )
			{
				Geometric geom = (Geometric)it.next();
				if (!(geom instanceof ArcInst)) continue;
				ArcInst ai = (ArcInst)geom;
	
				Point2D origHead = ai.getHead().getLocation();
				Point2D origTail = ai.getTail().getLocation();
				Point2D arcHead = new Point2D.Double(origHead.getX(), origHead.getY());
				Point2D arcTail = new Point2D.Double(origTail.getX(), origTail.getY());
				EditWindow.gridAlign(arcHead);
				EditWindow.gridAlign(arcTail);
	
				double headXOff = arcHead.getX() - origHead.getX();
				double headYOff = arcHead.getY() - origHead.getY();
				double tailXOff = arcTail.getX() - origTail.getX();
				double tailYOff = arcTail.getY() - origTail.getY();
				if (headXOff == 0 && tailXOff == 0 && headYOff == 0 && tailYOff == 0) continue;

				if (!ai.stillInPort(ai.getHead(), arcHead, false))
				{
					if (!ai.stillInPort(ai.getHead(), origHead, false)) continue;
					headXOff = headYOff = 0;
				}
				if (!ai.stillInPort(ai.getTail(), arcTail, false))
				{
					if (!ai.stillInPort(ai.getTail(), origTail, false)) continue;
					tailXOff = tailYOff = 0;
				}
	
				// make sure an arc does not change angle
				int ang = ai.getAngle();
				if (ang == 0 || ang == 1800)
				{
					// horizontal arc: both DY values must be the same
					if (headYOff != tailYOff) headYOff = tailYOff = 0;
				} else if (ang == 900 || ang == 2700)
				{
					// vertical arc: both DX values must be the same
					if (headXOff != tailXOff) headXOff = tailXOff = 0;
				}
				if (headXOff != 0 || tailXOff != 0 || headYOff != 0 || tailYOff != 0)
				{
					int constr = 0;
					if (ai.isRigid()) constr |= 1;
					if (ai.isFixedAngle()) constr |= 2;
                    ai.setRigid(false);
                    ai.setFixedAngle(false);

					ai.modify(0, headXOff, headYOff, tailXOff, tailYOff);
					adjustedArcs++;
					if ((constr & 1) != 0) ai.setRigid(true);
					if ((constr & 2) != 0) ai.setFixedAngle(true);
				}
			}
	
			// show results
			if (adjustedNodes == 0 && adjustedArcs == 0) System.out.println("No adjustments necessary"); else
				System.out.println("Adjusted " + adjustedNodes + " nodes and " + adjustedArcs + " arcs");
			return true;
		}
	}

	/**
	 * Method to align the selected nodes.
	 * @param horizontal true to align them horizontally; false for vertically.
	 * @param direction if horizontal is true, meaning is 0 for left, 1 for right, 2 for center.
	 * If horizontal is false, meaning is 0 for top, 1 for bottom, 2 for center.
	 */
	public static void alignNodes(boolean horizontal, int direction)
	{
		// make sure there is a current cell
		Cell np = WindowFrame.needCurCell();
		if (np == null) return;

		// get the objects to be moved (mark nodes with nonzero "temp1")
		List list = MenuCommands.getSelectedObjects(true, true);
		if (list.size() == 0)
		{
			System.out.println("First select objects to move");
			return;
		}

		// make sure they are all in the same cell
		for(Iterator it = list.iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (geom.getParent() != np)
			{
				System.out.println("All moved objects must be in the same cell");
				return;
			}
		}

		// count the number of nodes
		List nodes = new ArrayList();
		for(Iterator it = list.iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (geom instanceof NodeInst)
			{
				if (cantEdit(np, (NodeInst)geom, true) != 0) return;
				nodes.add(geom);
			}
		}
		int total = nodes.size();
		if (total == 0) return;

		NodeInst [] nis = new NodeInst[total];
		double [] dCX = new double[total];
		double [] dCY = new double[total];
		double [] dSX = new double[total];
		double [] dSY = new double[total];
		int [] dRot = new int[total];
		for(int i=0; i<total; i++)
		{
			nis[i] = (NodeInst)nodes.get(i);
			dSX[i] = dSY[i] = 0;
			dRot[i] = 0;
		}

		// get bounds
		double lX = 0, hX = 0, lY = 0, hY = 0;
		for(int i=0; i<total; i++)
		{
			NodeInst ni = nis[i];
			Rectangle2D bounds = ni.getBounds();
			if (i == 0)
			{
				lX = bounds.getMinX();
				hX = bounds.getMaxX();
				lY = bounds.getMinY();
				hY = bounds.getMaxY();
			} else
			{
				if (bounds.getMinX() < lX) lX = bounds.getMinX();
				if (bounds.getMaxX() > hX) hX = bounds.getMaxX();
				if (bounds.getMinY() < lY) lY = bounds.getMinY();
				if (bounds.getMaxY() > hY) hY = bounds.getMaxY();
			}
		}

		// determine motion
		for(int i=0; i<total; i++)
		{
			NodeInst ni = nis[i];
			Rectangle2D bounds = ni.getBounds();
			dCX[i] = dCY[i] = 0;
			if (horizontal)
			{
				// horizontal alignment
				switch (direction)
				{
					case 0:		// align to left
						dCX[i] = lX - bounds.getMinX();
						break;
					case 1:		// align to right
						dCX[i] = hX - bounds.getMaxX();
						break;
					case 2:		// align to center
						dCX[i] = (lX + hX) / 2 - bounds.getCenterX();
						break;
				}
			} else
			{
				// vertical alignment
				switch (direction)
				{
					case 0:		// align to top
						dCY[i] = hY - bounds.getMaxY();
						break;
					case 1:		// align to bottom
						dCY[i] = lY - bounds.getMinY();
						break;
					case 2:		// align to center
						dCY[i] = (lY + hY) / 2 - bounds.getCenterY();
						break;
				}
			}
		}
		AlignNodes job = new AlignNodes(nis, dCX, dCY, dSX, dSY, dRot);
	}

	private static class AlignNodes extends Job
	{
		NodeInst [] nis;
		double [] dCX;
		double [] dCY;
		double [] dSX;
		double [] dSY;
		int [] dRot;

		protected AlignNodes(NodeInst [] nis, double [] dCX, double [] dCY, double [] dSX, double [] dSY, int [] dRot)
		{
			super("Align objects", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.nis = nis;
			this.dCX = dCX;
			this.dCY = dCY;
			this.dSX = dSX;
			this.dSY = dSY;
			this.dRot = dRot;
			startJob();
		}

		public boolean doIt()
		{
			NodeInst.modifyInstances(nis, dCX, dCY, dSX, dSY, dRot);
			return true;
		}
	}

	/****************************** ARC MODIFICATION ******************************/

	/**
	 * This method sets the highlighted arcs to Rigid
	 */
	public static void arcRigidCommand()
	{
		ChangeArcProperties job = new ChangeArcProperties(1, MenuCommands.getHighlighted());
	}

	/**
	 * This method sets the highlighted arcs to Non-Rigid
	 */
	public static void arcNotRigidCommand()
	{
		ChangeArcProperties job = new ChangeArcProperties(2, MenuCommands.getHighlighted());
	}

	/**
	 * This method sets the highlighted arcs to Fixed-Angle
	 */
	public static void arcFixedAngleCommand()
	{
		ChangeArcProperties job = new ChangeArcProperties(3, MenuCommands.getHighlighted());
	}

	/**
	 * This method sets the highlighted arcs to Not-Fixed-Angle
	 */
	public static void arcNotFixedAngleCommand()
	{
		ChangeArcProperties job = new ChangeArcProperties(4, MenuCommands.getHighlighted());
	}

	/**
	 * This method toggles the directionality of highlighted arcs.
	 */
	public static void arcDirectionalCommand()
	{
		ChangeArcProperties job = new ChangeArcProperties(5, MenuCommands.getHighlighted());
	}

	/**
	 * This method sets the highlighted arcs to be End-Extended.
	 */
	public static void arcEndsExtendCommand()
	{
		ChangeArcProperties job = new ChangeArcProperties(6, MenuCommands.getHighlighted());
	}

	/**
	 * This method sets the highlighted arcs to be Reversed.
	 */
	public static void arcReverseCommand()
	{
		ChangeArcProperties job = new ChangeArcProperties(7, MenuCommands.getHighlighted());
	}

	/**
	 * This method sets the highlighted arcs to have their head skipped.
	 */
	public static void arcSkipHeadCommand()
	{
		ChangeArcProperties job = new ChangeArcProperties(8, MenuCommands.getHighlighted());
	}

	/**
	 * This method sets the highlighted arcs to have their tail skipped.
	 */
	public static void arcSkipTailCommand()
	{
		ChangeArcProperties job = new ChangeArcProperties(9, MenuCommands.getHighlighted());
	}

	private static class ChangeArcProperties extends Job
	{
		private int how;
        private List highlighted;

		protected ChangeArcProperties(int how, List highlighted)
		{
			super("Align objects", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.how = how;
            this.highlighted = highlighted;
			startJob();
		}

		public boolean doIt()
		{
			// make sure changing arcs is allowed
			Cell cell = WindowFrame.needCurCell();
			if (cell == null) return false;
			if (CircuitChanges.cantEdit(cell, null, true) != 0) return false;

			int numSet = 0, numUnset = 0;
			for(Iterator it = highlighted.iterator(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				if (h.getType() != Highlight.Type.EOBJ) continue;
				ElectricObject eobj = h.getElectricObject();
				if (eobj instanceof ArcInst)
				{
					ArcInst ai = (ArcInst)eobj;
					switch (how)
					{
						case 1:
							if (!ai.isRigid())
							{
								ai.setRigid(true);
								numSet++;
							}
							break;
						case 2:
							if (ai.isRigid())
							{
								ai.setRigid(false);
								numSet++;
							}
							break;
						case 3:
							if (!ai.isFixedAngle())
							{
								ai.setFixedAngle(true);
								numSet++;
							}
							break;
						case 4:
							if (ai.isFixedAngle())
							{
								ai.setFixedAngle(false);
								numSet++;
							}
							break;
						case 5:		// toggle directionality
							if (ai.isDirectional())
							{
								ai.setDirectional(false);
								numUnset++;
							} else
							{
								ai.setDirectional(true);
								numSet++;
							}
							break;
						case 6:		// end-extended
							if (ai.isExtended())
							{
								ai.setExtended(false);
								numUnset++;
							} else
							{
								ai.setExtended(true);
								numSet++;
							}
							break;
						case 7:		// reverse end
							if (ai.isReverseEnds())
							{
								ai.setReverseEnds(false);
								numUnset++;
							} else
							{
								ai.setReverseEnds(true);
								numSet++;
							}
							break;
						case 8:		// skip head
							if (ai.isSkipHead())
							{
								ai.setSkipHead(false);
								numUnset++;
							} else
							{
								ai.setSkipHead(true);
								numSet++;
							}
							break;
						case 9:		// skip tail
							if (ai.isSkipTail())
							{
								ai.setSkipTail(false);
								numUnset++;
							} else
							{
								ai.setSkipTail(true);
								numSet++;
							}
							break;
					}
				}
			}

			if (numSet == 0 && numUnset == 0) System.out.println("No changes were made"); else
			{
				String action = "";
				boolean repaintContents = false;
				switch (how)
				{
					case 1: action = "Rigid";   break;
					case 2: action = "Non-Rigid";   break;
					case 3: action = "Fixed-Angle";   break;
					case 4: action = "Not-Fixed-Angle";   break;
					case 5: action = "Directional";   repaintContents = true;   break;
					case 6: action = "have ends extended";   repaintContents = true;   break;
					case 7: action = "reversed";   repaintContents = true;   break;
					case 8: action = "skip head";   repaintContents = true;   break;
					case 9: action = "skip tail";   repaintContents = true;   break;
				}
				if (numUnset == 0) System.out.println("Made " + numSet + " arcs " + action); else
					if (numSet == 0) System.out.println("Made " + numUnset + " arcs not " + action); else
						System.out.println("Made " + numSet + " arcs " + action + "; and " + numUnset + " arcs not " + action);
				if (repaintContents) EditWindow.repaintAllContents(); else
					EditWindow.repaintAll();
			}
			return true;
		}
	}

	/**
	 * This method sets the highlighted ports to be negated.
	 */
	public static void toggleNegatedCommand()
	{
		ToggleNegationJob job = new ToggleNegationJob(MenuCommands.getHighlighted());
	}

	private static class ToggleNegationJob extends Job
	{
        private List highlighted;

		protected ToggleNegationJob(List highlighted)
		{
			super("Toggle negation", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.highlighted = highlighted;
			startJob();
		}

		public boolean doIt()
		{
			// make sure negation is allowed
			Cell cell = WindowFrame.needCurCell();
			if (cell == null) return false;
			if (CircuitChanges.cantEdit(cell, null, true) != 0) return false;

			int numSet = 0;
			for(Iterator it = highlighted.iterator(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				if (h.getType() != Highlight.Type.EOBJ) continue;
				ElectricObject eobj = h.getElectricObject();
				if (eobj instanceof PortInst)
				{
					PortInst pi = (PortInst)eobj;
					NodeInst ni = pi.getNodeInst();
					for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
					{
						Connection con = (Connection)cIt.next();
						if (con.getPortInst() != pi) continue;
						if (pi.getNodeInst().getProto() instanceof PrimitiveNode)
						{
							PrimitivePort pp = (PrimitivePort)pi.getPortProto();
							if (pp.isNegatable())
							{
								boolean newNegated = !con.isNegated();
								con.setNegated(newNegated);
								numSet++;
							}
						}
					}
				}
				if (eobj instanceof ArcInst)
				{
					ArcInst ai = (ArcInst)eobj;
					for(int i=0; i<2; i++)
					{
						Connection con = ai.getConnection(i);
						PortInst pi = con.getPortInst();
						if (pi.getNodeInst().getProto() instanceof PrimitiveNode)
						{
							PrimitivePort pp = (PrimitivePort)pi.getPortProto();
							if (pp.isNegatable())
							{
								boolean newNegated = !con.isNegated();
								con.setNegated(newNegated);
								numSet++;
							}
						}
					}
				}
			}
			if (numSet == 0) System.out.println("No ports negated"); else
			{
				System.out.println("Negated " + numSet + " ports");
				EditWindow.repaintAllContents();
			}
			return true;
		}
	}

	/**
	 * Method to rip the currently selected bus arc out into individual wires.
	 */
	public static void ripBus()
	{
		List list = MenuCommands.getSelectedObjects(false, true);
		if (list.size() == 0)
		{
			System.out.println("Must select bus arcs to rip into individual signals");
			return;
		}
		RipTheBus job = new RipTheBus(list);
	}

	private static class RipTheBus extends Job
	{
		List list;

		protected RipTheBus(List list)
		{
			super("Rip Bus", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.list = list;
			startJob();
		}

		public boolean doIt()
		{
			// make sure ripping arcs is allowed
			Cell cell = WindowFrame.needCurCell();
			if (cell == null) return false;
			if (CircuitChanges.cantEdit(cell, null, true) != 0) return false;

			for(Iterator it = list.iterator(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				if (ai.getProto() != Schematics.tech.bus_arc) continue;
				Netlist netList = ai.getParent().getUserNetlist();
				int busWidth = netList.getBusWidth(ai);
				String netName = netList.getNetworkName(ai);
				if (netName.length() == 0)
				{
					System.out.println("Bus " + ai.describe() + " has no name");
					continue;
				}

				// determine length of stub wires
				double stublen = (int)(ai.getLength() / 3 + 0.5);
				double lowXBus = 0, lowYBus = 0;
				int lowEnd = 1;
				double sepX = 0, sepY = 0;
				double lowX = 0, lowY = 0;

				// determine location of individual signals
				if (ai.getHead().getLocation().getX() == ai.getTail().getLocation().getX())
				{
					lowX = ai.getHead().getLocation().getX();
					if (lowX < ai.getParent().getBounds().getCenterX()) lowX += stublen; else
						lowX -= stublen;

					if (ai.getConnection(0).getLocation().getY() < ai.getConnection(1).getLocation().getY()) lowEnd = 0;
					lowY = (int)(ai.getConnection(lowEnd).getLocation().getY());
					double highy = (int)(ai.getConnection(1-lowEnd).getLocation().getY());
					if (highy-lowY >= busWidth-1)
					{
						// signals fit on grid
						sepY = (int)((highy-lowY) / (busWidth-1));
						lowY = (int)(((highy - lowY) - (sepY * (busWidth-1))) / 2 + lowY);
					} else
					{
						// signals don't fit: just make them even
						lowY = ai.getConnection(lowEnd).getLocation().getY();
						highy = ai.getConnection(1-lowEnd).getLocation().getY();
						sepY = (highy-lowY) / (busWidth-1);
					}
					lowXBus = ai.getTail().getLocation().getX();   lowYBus = lowY;
				} else if (ai.getTail().getLocation().getY() == ai.getHead().getLocation().getY())
				{
					lowY = ai.getTail().getLocation().getY();
					if (lowY < ai.getParent().getBounds().getCenterY()) lowY += stublen; else
						lowY -= stublen;

					if (ai.getConnection(0).getLocation().getX() < ai.getConnection(1).getLocation().getX()) lowEnd = 0;
					lowX = (int)(ai.getConnection(lowEnd).getLocation().getX());
					double highx = (int)(ai.getConnection(1-lowEnd).getLocation().getX());
					if (highx-lowX >= busWidth-1)
					{
						// signals fit on grid
						sepX = (int)((highx-lowX) / (busWidth-1));
						lowX = (int)(((highx - lowX) - (sepX * (busWidth-1))) / 2 + lowX);
					} else
					{
						// signals don't fit: just make them even
						lowX = ai.getConnection(lowEnd).getLocation().getX();
						highx = ai.getConnection(1-lowEnd).getLocation().getX();
						sepX = (highx-lowX) / (busWidth-1);
					}
					lowXBus = lowX;   lowYBus = ai.getTail().getLocation().getY();
				} else
				{
					System.out.println("Bus " + ai.describe() + " must be horizontal or vertical to be ripped out");
					continue;
				}

				// copy names to a local array
				String [] localStrings = new String[busWidth];
				for(int i=0; i<busWidth; i++)
				{
					Network subNet = netList.getNetwork(ai, i);
					if (subNet.hasNames()) localStrings[i] = (String)subNet.getNames().next(); else
						localStrings[i] = subNet.describe();
				}

				// turn off highlighting
				//Highlighter.global.clear();
				//Highlighter.global.finished();

				double sxw = Schematics.tech.wirePinNode.getDefWidth();
				double syw = Schematics.tech.wirePinNode.getDefHeight();
				double sxb = Schematics.tech.busPinNode.getDefWidth();
				double syb = Schematics.tech.busPinNode.getDefHeight();
				ArcProto apW = Schematics.tech.wire_arc;
				ArcProto apB = Schematics.tech.bus_arc;
				NodeInst niBLast = null;
				for(int i=0; i<busWidth; i++)
				{
					// make the wire pin
					NodeInst niw = NodeInst.makeInstance(Schematics.tech.wirePinNode, new Point2D.Double(lowX, lowY), sxw, syw, ai.getParent());
					if (niw == null) break;

					// make the bus pin
					NodeInst nib = NodeInst.makeInstance(Schematics.tech.busPinNode, new Point2D.Double(lowXBus, lowYBus), sxb, syb, ai.getParent());
					if (nib == null) break;

					// wire them
					PortInst head = niw.getOnlyPortInst();
					PortInst tail = nib.getOnlyPortInst();
					ArcInst aiw = ArcInst.makeInstance(apW, apW.getDefaultWidth(), head, tail);
					if (aiw == null) break;
					aiw.setName(localStrings[i]);

					// wire to the bus pin
					if (i == 0)
					{
						PortInst first = ai.getConnection(lowEnd).getPortInst();
						aiw = ArcInst.makeInstance(apB, apB.getDefaultWidth(), first, tail);
					} else
					{
						PortInst first = niBLast.getOnlyPortInst();
						aiw = ArcInst.makeInstance(apB, apB.getDefaultWidth(), first, tail);
					}
					if (aiw == null) break;

					// advance to the next segment
					niBLast = nib;
					lowX += sepX;      lowY += sepY;
					lowXBus += sepX;   lowYBus += sepY;
				}

				// wire up the last segment
				PortInst head = niBLast.getOnlyPortInst();
				PortInst tail = ai.getConnection(1-lowEnd).getPortInst();
				ArcInst aiw = ArcInst.makeInstance(apB, apB.getDefaultWidth(), head, tail);
				if (aiw == null) return false;
				aiw.setName(netName);

				// remove original arc
				ai.kill();
			}
			return true;
		}
	}

	/****************************** DELETE SELECTED OBJECTS ******************************/

	/**
	 * Method to delete all selected objects.
	 */
	public static void deleteSelected()
	{
		// see what type of window is selected
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf == null) return;
        Highlighter highlighter = wf.getContent().getHighlighter();
        if (highlighter == null) return;

		// for waveform windows, delete selected signals
		if (wf.getContent() instanceof WaveformWindow)
		{
			WaveformWindow ww = (WaveformWindow)wf.getContent();
			ww.deleteSelectedSignals();
			return;
		}

		if (ToolBar.getSelectMode() == ToolBar.SelectMode.AREA)
		{
            EditWindow wnd = EditWindow.getCurrent();
            Rectangle2D bounds = highlighter.getHighlightedArea(wnd);
			DeleteSelectedGeometry job = new DeleteSelectedGeometry(wnd.getCell(), bounds);
		} else
		{
            List highlightedText = highlighter.getHighlightedText(true);
            List highlighted = highlighter.getHighlights();
            if (highlighted.size() == 0) return;
	        DeleteSelected job = new DeleteSelected(highlightedText, highlighted);
		}
	}

	private static class DeleteSelected extends Job
	{
        private List highlightedText;
        private List highlighted;

		protected DeleteSelected(List highlightedText, List highlighted)
		{
			super("Delete selected objects", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.highlightedText = highlightedText;
            this.highlighted = highlighted;
			startJob();
		}

		public boolean doIt()
		{
			// make sure deletion is allowed
			Cell cell = WindowFrame.needCurCell();
			if (cell != null)
			{
				if (cantEdit(cell, null, true) != 0) return false;
			}

			List deleteList = new ArrayList();
			Geometric oneGeom = null;
			for(Iterator it = highlighted.iterator(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				Geometric geom = h.getGeometric();
				if (geom == null) continue;
				if (h.getType() == Highlight.Type.TEXT)
				{
					ElectricObject eobj = h.getElectricObject();
					if (eobj instanceof Export) continue;
				}

				if (cell != h.getCell())
				{
					JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
						"All objects to be deleted must be in the same cell",
							"Delete failed", JOptionPane.ERROR_MESSAGE);
					return false;
				}
				if (geom instanceof NodeInst)
				{
					int errCode = cantEdit(cell, (NodeInst)geom, true);
					if (errCode < 0) return false;
					if (errCode > 0) continue;
				}
				deleteList.add(geom);
			}

			// clear the highlighting
			//Highlighter.global.clear();
			//Highlighter.global.finished();

/*			// if just one node is selected, see if it can be deleted with "pass through"
			if (deleteList.size() == 1 && oneGeom instanceof NodeInst)
			{
				Reconnect re = Reconnect.erasePassThru((NodeInst)oneGeom, false);
				if (re != null)
				{
					List arcs = re.reconnectArcs();
                    for (Iterator it = arcs.iterator(); it.hasNext(); ) {
                        ArcInst ai = (ArcInst)it.next();
					    Highlight.addElectricObject(ai, cell);
					    Highlight.finished();
                    }
					//return true;
				}
			}*/

			// delete the text
			for(Iterator it = highlightedText.iterator(); it.hasNext(); )
			{
				Highlight high = (Highlight)it.next();

//				// do not deal with text on an object if the object is already in the list
//				if (high.fromgeom != NOGEOM)
//				{
//					for(j=0; list[j] != NOGEOM; j++)
//						if (list[j] == high.fromgeom) break;
//					if (list[j] != NOGEOM) continue;
//				}

				// deleting variable on object
				Variable var = high.getVar();
				ElectricObject eobj = high.getElectricObject();
				if (var != null)
				{
					eobj.delVar(var.getKey());
				} else
				{
					if (high.getName() != null)
					{
						if (eobj instanceof Geometric)
						{
							Geometric geom = (Geometric)eobj;
							if (geom instanceof NodeInst)
							{
								NodeInst ni = (NodeInst)geom;
								ni.setName(null);
								ni.modifyInstance(0, 0, 0, 0, 0);
							} else
							{
								ArcInst ai = (ArcInst)geom;
								ai.setName(null);
								ai.modify(0, 0, 0, 0, 0);
							}
						}
					} else
					{
						if (eobj instanceof Export)
						{
							Export pp = (Export)eobj;
							pp.kill();
						}
					}
				}
			}
			if (cell != null)
				eraseObjectsInList(cell, deleteList);
			return true;
		}
	}

	private static class DeleteSelectedGeometry extends Job
	{
        private Cell cell;
        private Rectangle2D bounds;

		protected DeleteSelectedGeometry(Cell cell, Rectangle2D bounds)
		{
			super("Delete selected geometry", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.cell = cell;
            this.bounds = bounds;
			startJob();
		}

		public boolean doIt()
		{
			if (cell == null)
			{
				System.out.println("No current cell");
				return false;
			}
            if (bounds == null)
            {
                System.out.println("Nothing selected");
                return false;
            }

			// disallow erasing if lock is on
			if (cantEdit(cell, null, true) != 0) return false;

			if (bounds == null)
			{
				System.out.println("Outline an area first");
				return false;
			}

			// grid the area
			double lX = Math.floor(bounds.getMinX());
			double hX = Math.ceil(bounds.getMaxX());
			double lY = Math.floor(bounds.getMinY());
			double hY = Math.ceil(bounds.getMaxY());

			// crop arcs that cross the area boundary
			List arcsInCell = new ArrayList();
			for(Iterator aIt = cell.getArcs(); aIt.hasNext(); )
				arcsInCell.add(aIt.next());
			for(Iterator aIt = arcsInCell.iterator(); aIt.hasNext(); )
			{
				ArcInst ai = (ArcInst)aIt.next();

				// if an end is inside, ignore
				Point2D headPt = ai.getHead().getLocation();
				Point2D tailPt = ai.getTail().getLocation();

				// if length is zero, ignore
				if (tailPt.getX() == headPt.getX() &&
					tailPt.getY() == headPt.getY()) continue;

				// if the arc doesn't intersect the area, ignore
				double halfWidth = (ai.getWidth() - ai.getProto().getWidthOffset()) / 2;
				double lXExt = lX - halfWidth;
				double hXExt = hX + halfWidth;
				double lYExt = lY - halfWidth;
				double hYExt = hY + halfWidth;
				Point2D tailPtAdj = new Point2D.Double(tailPt.getX(), tailPt.getY());
				Point2D headPtAdj = new Point2D.Double(headPt.getX(), headPt.getY());
				if (DBMath.clipLine(tailPtAdj, headPtAdj, lXExt, hXExt, lYExt, hYExt)) continue;
				if (tailPtAdj.distance(headPt) + headPtAdj.distance(tailPt) <
					headPtAdj.distance(headPt) + tailPtAdj.distance(tailPt))
				{
					Point2D swap = headPtAdj;
					headPtAdj = tailPtAdj;
					tailPtAdj = swap;
				}
				Name name = ai.getNameKey();
				String newName = null;
				if (!name.isTempname()) newName = name.toString();
				if (!tailPt.equals(tailPtAdj))
				{
					// create a pin at this point
					PrimitiveNode pin = ((PrimitiveArc)ai.getProto()).findPinProto();
					NodeInst ni = NodeInst.makeInstance(pin, tailPtAdj, pin.getDefWidth(), pin.getDefHeight(), cell);
					if (ni == null)
					{
						System.out.println("Error creating pin for shortening of arc "+ai.describe());
						continue;
					}

					ArcInst ai1 = ArcInst.makeInstance(ai.getProto(), ai.getWidth(),
						ai.getTail().getPortInst(), ni.getOnlyPortInst(), ai.getTail().getLocation(),
					        tailPtAdj, newName);
					if (ai1 == null)
					{
						System.out.println("Error shortening arc "+ai.describe());
						continue;
					}
					newName = null;
					ai1.copyPropertiesFrom(ai);
				}
				if (!headPt.equals(headPtAdj))
				{
					// create a pin at this point
					PrimitiveNode pin = ((PrimitiveArc)ai.getProto()).findPinProto();
					NodeInst ni = NodeInst.makeInstance(pin, headPtAdj, pin.getDefWidth(), pin.getDefHeight(), cell);
					if (ni == null)
					{
						System.out.println("Error creating pin for shortening of arc "+ai.describe());
						continue;
					}

					ArcInst ai1 = ArcInst.makeInstance(ai.getProto(), ai.getWidth(), ni.getOnlyPortInst(), ai.getHead().getPortInst(), headPtAdj,
					        ai.getHead().getLocation(), newName);
					if (ai1 == null)
					{
						System.out.println("Error shortening arc "+ai.describe());
						continue;
					}
					ai1.copyPropertiesFrom(ai);
				}
				ai.kill();
			}

			// now remove nodes in the area
			List nodesToDelete = new ArrayList();
			for(Iterator nIt = cell.getNodes(); nIt.hasNext(); )
			{
				NodeInst ni = (NodeInst)nIt.next();
		
				// if the node is outside of the area, ignore it
				double cX = ni.getTrueCenterX();
				double cY = ni.getTrueCenterY();
				if (cX >= hX || cX <= lX || cY >= hY || cY <= lY) continue;

				// if it cannot be modified, stop
				int errorCode = cantEdit(cell, ni, true);
				if (errorCode < 0) return false;
				if (errorCode > 0) continue;
				nodesToDelete.add(ni);
			}

			// delete the nodes
			for(Iterator nIt = nodesToDelete.iterator(); nIt.hasNext(); )
			{
				NodeInst ni = (NodeInst)nIt.next();
				eraseNodeInst(ni);		
			}
			return true;
		}
	}

	/****************************** DELETE OBJECTS IN A LIST ******************************/

	/**
	 * Method to delete all of the Geometrics in a list.
	 * @param cell the cell with the objects to be deleted.
	 * @param list a List of Geometric or Highlight objects to be deleted.
	 */
	public static void eraseObjectsInList(Cell cell, List list)
	{
		HashMap deleteFlag = new HashMap();

		for(Iterator it = list.iterator(); it.hasNext(); )
		{
			Object obj = it.next();
			if (obj instanceof Highlight) obj = ((Highlight)obj).getGeometric();
			if (!(obj instanceof Geometric)) continue;
			Geometric geom = (Geometric)obj;

			if (geom instanceof ArcInst)
			{
				ArcInst ai = (ArcInst)geom;
				deleteFlag.put(ai.getHead().getPortInst().getNodeInst(), new Integer(1));
				deleteFlag.put(ai.getTail().getPortInst().getNodeInst(), new Integer(1));
			}
		}

		// also mark all nodes on arcs that will be erased
		for(Iterator it = list.iterator(); it.hasNext(); )
		{
			Object obj = it.next();
			if (obj instanceof Highlight) obj = ((Highlight)obj).getGeometric();
			if (!(obj instanceof NodeInst)) continue;
			NodeInst ni = (NodeInst)obj;

			Integer flag = (Integer)deleteFlag.get(ni);
			if (flag != null && flag.intValue() != 2)
				deleteFlag.put(ni, new Integer(2));
		}

		// also mark all nodes on the other end of arcs connected to erased nodes
		for(Iterator it = list.iterator(); it.hasNext(); )
		{
			Object obj = it.next();
			if (obj instanceof Highlight) obj = ((Highlight)obj).getGeometric();
			if (!(obj instanceof NodeInst)) continue;
			NodeInst ni = (NodeInst)obj;

			for(Iterator sit = ni.getConnections(); sit.hasNext(); )
			{
				Connection con = (Connection)sit.next();
				ArcInst ai = con.getArc();
				Connection otherEnd = ai.getHead();
				if (ai.getHead() == con) otherEnd = ai.getTail();

				NodeInst oNi = otherEnd.getPortInst().getNodeInst();
				if (deleteFlag.get(oNi) == null) deleteFlag.put(oNi, new Integer(1));
			}
		}

		// now kill all of the arcs
		for(Iterator it = list.iterator(); it.hasNext(); )
		{
			Object obj = it.next();
			if (obj instanceof Highlight) obj = ((Highlight)obj).getGeometric();
			if (!(obj instanceof ArcInst)) continue;
			ArcInst ai = (ArcInst)obj;

			ai.kill();
		}

		// next kill all of the nodes
		for(Iterator it = list.iterator(); it.hasNext(); )
		{
			Object obj = it.next();
			if (obj instanceof Highlight) obj = ((Highlight)obj).getGeometric();
			if (!(obj instanceof NodeInst)) continue;
			NodeInst ni = (NodeInst)obj;

            // see if any arcs can be reconnected as a result of this kill
            Reconnect re = Reconnect.erasePassThru(ni, false);
            if (re != null) re.reconnectArcs();

            eraseNodeInst(ni);
		}

		// kill all pin nodes that touched an arc and no longer do
		List nodesToDelete = new ArrayList();
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (deleteFlag.get(ni) == null) continue;
			if (ni.getProto() instanceof PrimitiveNode)
			{
				if (ni.getProto().getFunction() != PrimitiveNode.Function.PIN) continue;
				if (ni.getNumConnections() != 0 || ni.getNumExports() != 0) continue;
				nodesToDelete.add(ni);
			}
		}
		for(Iterator it = nodesToDelete.iterator(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			eraseNodeInst(ni);
		}

		// kill all unexported pin or bus nodes left in the middle of arcs
		List nodesToPassThru = new ArrayList();
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (deleteFlag.get(ni) == null) continue;
			if (ni.getProto() instanceof PrimitiveNode)
			{
				if (ni.getProto().getFunction() != PrimitiveNode.Function.PIN) continue;
				if (ni.getNumExports() != 0) continue;
                if (!ni.isInlinePin()) continue;
				nodesToPassThru.add(ni);
			}
		}
		for(Iterator it = nodesToPassThru.iterator(); it.hasNext(); )
		{
            NodeInst ni = (NodeInst)it.next();
            Reconnect re = Reconnect.erasePassThru(ni, false);
			if (re != null)
			{
                re.reconnectArcs();
			    eraseNodeInst(ni);
            }
		}

		// kill variables on cells
		for(Iterator it = list.iterator(); it.hasNext(); )
		{
			Object obj = it.next();
			if (!(obj instanceof Highlight)) continue;
			Highlight h = (Highlight)obj;
			if (h.getType() != Highlight.Type.TEXT) continue;
			Variable var = h.getVar();
			if (var == null) continue;
			ElectricObject owner = h.getElectricObject();
			if (!(owner instanceof Cell)) continue;

			owner.delVar(var.getKey());
		}
	}

	/**
	 * Method to erase node "ni" and all associated arcs, exports, etc.
	 */
	private static void eraseNodeInst(NodeInst ni)
	{
		// erase all connecting ArcInsts on this NodeInst
		int numConnectedArcs = ni.getNumConnections();
		if (numConnectedArcs > 0)
		{
			ArcInst [] arcsToDelete = new ArcInst[numConnectedArcs];
			int i = 0;
			for(Iterator it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = (Connection)it.next();
				arcsToDelete[i++] = con.getArc();
			}
			for(int j=0; j<numConnectedArcs; j++)
			{
				ArcInst ai = arcsToDelete[j];

				// delete the ArcInst
				ai.kill();
			}
		}

		// if this NodeInst has Exports, delete them
		undoExport(ni, null);

		// now erase the NodeInst
		ni.kill();
	}

	/**
	 * Method to recursively delete ports at nodeinst "ni" and all arcs connected
	 * to them anywhere.  If "spt" is not null, delete only that portproto
	 * on this nodeinst (and its hierarchically related ports).  Otherwise delete
	 * all portprotos on this nodeinst.
	 */
	private static void undoExport(NodeInst ni, Export spt)
	{
		int numExports = ni.getNumExports();
		if (numExports == 0) return;
		Export exportsToDelete [] = new Export[numExports];
		int i = 0;		
		for(Iterator it = ni.getExports(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			exportsToDelete[i++] = pp;
		}
		for(int j=0; j<numExports; j++)
		{
			Export pp = exportsToDelete[j];
			if (spt != null && spt != pp) continue;
			pp.kill();
		}
	}

	/****************************** DELETE A CELL ******************************/

	/**
	 * Method to delete a cell.
	 * @param cell the cell to delete.
	 * @param confirm true to prompt the user to confirm the deletion.
	 * @return true if the cell will be deleted (in a separate Job).
	 */
	public static boolean deleteCell(Cell cell, boolean confirm)
	{
		// see if this cell is in use anywhere
		if (cell.isInUse("delete")) return false;

		// make sure the user really wants to delete the cell
		if (confirm)
		{
			int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(),
				"Are you sure you want to delete cell " + cell.describe() + "?");
			if (response != JOptionPane.YES_OPTION) return false;
		}

		// delete the cell
		DeleteCell job = new DeleteCell(cell);
		return true;
	}

	/**
	 * Class to delete a cell in a new thread.
	 */
	private static class DeleteCell extends Job
	{
		Cell cell;

		protected DeleteCell(Cell cell)
		{
			super("Delete Cell" + cell.describe(), User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			startJob();
		}

		public boolean doIt()
		{
			// check cell usage once more
			if (cell.isInUse("delete")) return false;
			doKillCell(cell);
			return true;
		}
	}

	/**
	 * Method to delete cell "cell".  Validity checks are assumed to be made (i.e. the
	 * cell is not used and is not locked).
	 */
	private static void doKillCell(Cell cell)
	{
		// delete random references to this cell
		Library lib = cell.getLibrary();
		if (cell == lib.getCurCell()) lib.setCurCell(null);

		// close windows that reference this cell
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			WindowContent content = wf.getContent();
			if (content == null) continue;
			if (content.getCell() == cell)
			{
				if (!(content instanceof EditWindow))
				{
					wf.setCellWindow(null);
				} else
				{
					content.setCell(null, null);
					content.fullRepaint();
				}
			}
		}

//		prevversion = cell->prevversion;
//		toolturnoff(net_tool, FALSE);
		cell.kill();
//		toolturnon(net_tool);

//		// see if this was the latest version of a cell
//		if (prevversion != NONODEPROTO)
//		{
//			// newest version was deleted: rename next older version
//			for(ni = prevversion->firstinst; ni != NONODEINST; ni = ni->nextinst)
//			{
//				if ((ni->userbits&NEXPAND) != 0) continue;
//				startobjectchange((INTBIG)ni, VNODEINST);
//				endobjectchange((INTBIG)ni, VNODEINST);
//			}
//		}
//
//		// update status display if necessary
//		if (us_curnodeproto != NONODEPROTO && us_curnodeproto->primindex == 0)
//		{
//			if (cell == us_curnodeproto)
//			{
//				if ((us_state&NONPERSISTENTCURNODE) != 0) us_setnodeproto(NONODEPROTO); else
//					us_setnodeproto(el_curtech->firstnodeproto);
//			}
//		}
	}

	/****************************** RENAME CELLS ******************************/

	public static void renameCellInJob(Cell cell, String newName)
	{
		// see if the rename should also regroup
		Cell.CellGroup newGroup = null;
		for(Iterator it = cell.getLibrary().getCells(); it.hasNext(); )
		{
			Cell oCell = (Cell)it.next();
			if (oCell.getName().equalsIgnoreCase(newName) && oCell.getCellGroup() != cell.getCellGroup())
			{
				int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(),
					"Also place the cell into the " + oCell.getCellGroup().getName() + " group?");
				if (response == JOptionPane.YES_OPTION) newGroup = oCell.getCellGroup();
				break;
			}
		}
		RenameCell job = new RenameCell(cell, newName, newGroup);
	}

	/**
	 * Class to rename a cell in a new thread.
	 */
	private static class RenameCell extends Job
	{
		private Cell cell;
		private String newName;
		private Cell.CellGroup newGroup;

		protected RenameCell(Cell cell, String newName, Cell.CellGroup newGroup)
		{
			super("Rename Cell " + cell.describe(), User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.newName = newName;
			this.newGroup = newGroup;
			startJob();
		}

		public boolean doIt()
		{
			cell.rename(newName);
			if (newGroup != null)
			{
				cell.setCellGroup(newGroup);
			}
			return true;
		}
	}

	public static void renameCellGroupInJob(Cell.CellGroup cellGroup, String newName)
	{
		RenameCellGroup job = new RenameCellGroup(cellGroup, newName);
	}

	/**
	 * Class to rename a cell in a new thread.
	 */
	private static class RenameCellGroup extends Job
	{
		Cell.CellGroup cellGroup;
		String newName;

		protected RenameCellGroup(Cell.CellGroup cellGroup, String newName)
		{
			super("Rename Cell Group", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cellGroup = cellGroup;
			this.newName = newName;
			startJob();
		}

		public boolean doIt()
		{
			for(Iterator it = cellGroup.getCells(); it.hasNext(); )
			{
				Cell cell = (Cell)it.next();
				cell.rename(newName);
			}
			return true;
		}
	}

	/****************************** DELETE UNUSED OLD VERSIONS OF CELLS ******************************/

	public static void deleteUnusedOldVersions()
	{
		DeleteUnusedOldCells job = new DeleteUnusedOldCells();
	}

	/**
	 * This class implement the command to delete unused old versions of cells.
	 */
	private static class DeleteUnusedOldCells extends Job
	{
		protected DeleteUnusedOldCells()
		{
			super("Delete Unused Old Cells", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			startJob();
		}

		public boolean doIt()
		{
			boolean found = true;
			int totalDeleted = 0;
			while (found)
			{
				found = false;
				for(Iterator it = Library.getCurrent().getCells(); it.hasNext(); )
				{
					Cell cell = (Cell)it.next();
					if (cell.getNewestVersion() == cell) continue;
					if (cell.getInstancesOf().hasNext()) continue;
					System.out.println("Deleting cell " + cell.describe());
					doKillCell(cell);
					found = true;
					totalDeleted++;
					break;
				}
			}
			if (totalDeleted == 0) System.out.println("No unused old cell versions to delete"); else
			{
				System.out.println("Deleted " + totalDeleted + " cells");
				EditWindow.repaintAll();
			}
			return true;
		}
	}

	/****************************** SHOW CELLS GRAPHICALLY ******************************/

	/**
	 * Method to graph the cells, starting from the current cell.
	 */
	public static void graphCellsFromCell()
	{
		Cell top = WindowFrame.needCurCell();
		if (top == null) return;
		GraphCells job = new GraphCells(top);
	}

	/**
	 * Method to graph all cells in the current Library.
	 */
	public static void graphCellsInLibrary()
	{
		GraphCells job = new GraphCells(null);
	}

	/**
	 * This class implement the command to make a graph of the cells.
	 */
	private static class GraphCells extends Job
	{
		private Cell top;

		private static class CellGraphNode
		{
			int            depth;
			int            clock;
			double         x;
			double         y;
			double         yoff;
			NodeInst       pin;
			CellGraphNode  main;
		}

		protected GraphCells(Cell top)
		{
			super("Graph Cells", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			startJob();
		}

		public boolean doIt()
		{
			// create the graph cell
			Cell graphCell = Cell.newInstance(Library.getCurrent(), "CellStructure");
			if (graphCell == null) return false;
			if (graphCell.getNumVersions() > 1)
				System.out.println("Creating new version of cell: CellStructure"); else
					System.out.println("Creating cell: CellStructure");

			// create CellGraphNodes for every cell and initialize the depth to -1
			HashMap cellGraphNodes = new HashMap();
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				if (lib.isHidden()) continue;
				for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = (Cell)cIt.next();
					CellGraphNode cgn = new CellGraphNode();
					cgn.depth = -1;
					cellGraphNodes.put(cell, cgn);
				}
			}

			// find all top-level cells
			if (top != null)
			{
				CellGraphNode cgn = (CellGraphNode)cellGraphNodes.get(top);
				cgn.depth = 0;
			} else
			{
				for(Iterator cIt = Library.getCurrent().getCells(); cIt.hasNext(); )
				{
					Cell cell = (Cell)cIt.next();
					if (cell.getNumUsagesIn() == 0)
					{
						CellGraphNode cgn = (CellGraphNode)cellGraphNodes.get(cell);
						cgn.depth = 0;
					}
				}
			}

			// now place all cells at their proper depth
			int maxDepth = 0;
			boolean more = true;
			while (more)
			{
				more = false;
				for(Iterator it = Library.getLibraries(); it.hasNext(); )
				{
					Library lib = (Library)it.next();
					if (lib.isHidden()) continue;
					for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
					{
						Cell cell = (Cell)cIt.next();
						CellGraphNode cgn = (CellGraphNode)cellGraphNodes.get(cell);
						if (cgn.depth == -1) continue;
						for(Iterator nIt = cell.getNodes(); nIt.hasNext(); )
						{
							NodeInst ni = (NodeInst)nIt.next();
							if (!(ni.getProto() instanceof Cell)) continue;
							Cell sub = (Cell)ni.getProto();
	
							// ignore recursive references (showing icon in contents)
							if (ni.isIconOfParent()) continue;

							CellGraphNode subCgn = (CellGraphNode)cellGraphNodes.get(sub);
							if (subCgn.depth <= cgn.depth)
							{
								subCgn.depth = cgn.depth + 1;
								if (subCgn.depth > maxDepth) maxDepth = subCgn.depth;
								more = true;
							}
							Cell trueCell = sub.contentsView();
							if (trueCell == null) continue;
							CellGraphNode trueCgn = (CellGraphNode)cellGraphNodes.get(trueCell);
							if (trueCgn.depth <= cgn.depth)
							{
								trueCgn.depth = cgn.depth + 1;
								if (trueCgn.depth > maxDepth) maxDepth = trueCgn.depth;
								more = true;
							}
						}
					}
				}

				// add in any cells referenced from other libraries
				if (!more && top == null)
				{
					for(Iterator cIt = Library.getCurrent().getCells(); cIt.hasNext(); )
					{
						Cell cell = (Cell)cIt.next();
						CellGraphNode cgn = (CellGraphNode)cellGraphNodes.get(cell);
						if (cgn.depth >= 0) continue;
						cgn.depth = 0;
						more = true;
					}
				}
			}

			// now assign X coordinates to each cell
			maxDepth++;
			double maxWidth = 0;
			double [] xval = new double[maxDepth];
			double [] yoff = new double[maxDepth];
			for(int i=0; i<maxDepth; i++) xval[i] = yoff[i] = 0;
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				if (lib.isHidden()) continue;
				for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = (Cell)cIt.next();
					CellGraphNode cgn = (CellGraphNode)cellGraphNodes.get(cell);

					// ignore icon cells from the graph (merge with contents)
					if (cgn.depth == -1) continue;

					// ignore associated cells for now
					Cell trueCell = graphMainView(cell);
					if (trueCell != null &&
						(cell.getNumUsagesIn() == 0 || cell.isIcon() ||
							cell.getView() == View.LAYOUTSKEL))
					{
						cgn.depth = -1;
						continue;
					}

					cgn.x = xval[cgn.depth];
					xval[cgn.depth] += cell.describe().length();
					if (xval[cgn.depth] > maxWidth) maxWidth = xval[cgn.depth];
					cgn.y = cgn.depth;
					cgn.yoff = 0;
				}
			}

			// now center each row
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				if (lib.isHidden()) continue;
				for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = (Cell)cIt.next();
					CellGraphNode cgn = (CellGraphNode)cellGraphNodes.get(cell);
					if (cgn.depth == -1) continue;
					if (xval[(int)cgn.y] < maxWidth)
					{
						double spread = maxWidth / xval[(int)cgn.y];
						cgn.x = cgn.x * spread;
					}
				}
			}

			// generate accurate X/Y coordinates
			double xScale = 2.0 / 3.0;
			double yScale = 20;
			double yOffset = 0.5;
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				if (lib.isHidden()) continue;
				for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = (Cell)cIt.next();
					CellGraphNode cgn = (CellGraphNode)cellGraphNodes.get(cell);
					if (cgn.depth == -1) continue;
					double x = cgn.x;   double y = cgn.y;
					x = x * xScale;
					y = -y * yScale + ((yoff[(int)cgn.y]++)%2) * yOffset;
					cgn.x = x;   cgn.y = y;
				}
			}

			// make unattached cells sit with their contents view
			if (top == null)
			{
				for(Iterator it = Library.getLibraries(); it.hasNext(); )
				{
					Library lib = (Library)it.next();
					if (lib.isHidden()) continue;
					for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
					{
						Cell cell = (Cell)cIt.next();
						CellGraphNode cgn = (CellGraphNode)cellGraphNodes.get(cell);
						if (cgn.depth != -1) continue;

						if (cell.getNumUsagesIn() != 0 && !cell.isIcon() &&
							cell.getView() != View.LAYOUTSKEL) continue;
						Cell trueCell = graphMainView(cell);
						if (trueCell == null) continue;
						CellGraphNode trueCgn = (CellGraphNode)cellGraphNodes.get(trueCell);
						if (trueCgn.depth == -1) continue;
		
						cgn.pin = null;
						cgn.main = trueCgn;
						cgn.yoff += yOffset*2;
						cgn.x = trueCgn.x;
						cgn.y = trueCgn.y + trueCgn.yoff;
					}
				}
			}

			// write the header message
			double xsc = maxWidth * xScale / 2;
			NodeInst titleNi = NodeInst.newInstance(Generic.tech.invisiblePinNode, new Point2D.Double(xsc, yScale), 0, 0, graphCell);
			if (titleNi == null) return false;
			StringBuffer infstr = new StringBuffer();
			if (top != null)
			{
				infstr.append("Structure below cell " + top.describe());
			} else
			{
				infstr.append("Structure of library " + Library.getCurrent().getName());
			}
			Variable var = titleNi.newVar(Artwork.ART_MESSAGE, infstr.toString());
			if (var != null)
			{
				var.setDisplay(true);
				var.getTextDescriptor().setRelSize(6);
			}

			// place the components
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				if (lib.isHidden()) continue;
				for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = (Cell)cIt.next();
					if (cell == graphCell) continue;
					CellGraphNode cgn = (CellGraphNode)cellGraphNodes.get(cell);
					if (cgn.depth == -1) continue;

					double x = cgn.x;   double y = cgn.y;
					NodeInst ni = NodeInst.newInstance(Generic.tech.invisiblePinNode, new Point2D.Double(x, y), 0, 0, graphCell);
					if (ni == null) return false;
					cgn.pin = ni;

					// write the cell name in the node
					var = ni.newVar(Artwork.ART_MESSAGE, cell.describe());
					if (var != null)
					{
						var.setDisplay(true);
						var.getTextDescriptor().setRelSize(1);
					}
				}
			}

			// attach related components with rigid arcs
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				if (lib.isHidden()) continue;
				for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = (Cell)cIt.next();
					if (cell == graphCell) continue;
					CellGraphNode cgn = (CellGraphNode)cellGraphNodes.get(cell);
					if (cgn.depth == -1) continue;
					if (cgn.main == null) continue;

					PortInst firstPi = cgn.pin.getOnlyPortInst();
					PortInst secondPi = cgn.main.pin.getOnlyPortInst();
					ArcInst ai = ArcInst.makeInstance(Artwork.tech.solidArc, 0, firstPi, firstPi);
					if (ai == null) return false;
					ai.setRigid(true);

					// set an invisible color on the arc
					ai.newVar(Artwork.ART_COLOR, new Integer(0));
				}
			}

			// build wires between the hierarchical levels
			int clock = 0;
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				if (lib.isHidden()) continue;
				for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = (Cell)cIt.next();
					if (cell == graphCell) continue;

					// always use the contents cell, not the icon
					Cell trueCell = cell.contentsView();
					if (trueCell == null) trueCell = cell;
					CellGraphNode trueCgn = (CellGraphNode)cellGraphNodes.get(trueCell);
					if (trueCgn.depth == -1) continue;

					clock++;
					for(Iterator nIt = trueCell.getNodes(); nIt.hasNext(); )
					{
						NodeInst ni = (NodeInst)nIt.next();
						if (!(ni.getProto() instanceof Cell)) continue;

						// ignore recursive references (showing icon in contents)
						if (ni.isIconOfParent()) continue;
						Cell sub = (Cell)ni.getProto();

						Cell truesubnp = sub.contentsView();
						if (truesubnp == null) truesubnp = sub;

						CellGraphNode trueSubCgn = (CellGraphNode)cellGraphNodes.get(truesubnp);
						if (trueSubCgn.clock == clock) continue;
						trueSubCgn.clock = clock;

						// draw a line from cell "trueCell" to cell "truesubnp"
						if (trueSubCgn.depth == -1) continue;
						PortInst toppinPi = trueCgn.pin.getOnlyPortInst();
						PortInst niBotPi = trueSubCgn.pin.getOnlyPortInst();
						ArcInst ai = ArcInst.makeInstance(Artwork.tech.solidArc, Artwork.tech.solidArc.getDefaultWidth(), toppinPi, niBotPi);
						if (ai == null) return false;
                        ai.setRigid(false);
                        ai.setFixedAngle(false);

						// set an appropriate color on the arc (red for jumps of more than 1 level of depth)
						int color = EGraphics.BLUE;
						if (trueCgn.y - trueSubCgn.y > yScale+yOffset+yOffset) color = EGraphics.RED;
						ai.newVar(Artwork.ART_COLOR, new Integer(color));
					}
				}
			}
			WindowFrame.createEditWindow(graphCell);
			return true;
		}
	}

	/**
	 * Method to find the main cell that "np" is associated with in the graph.  This code is
	 * essentially the same as "contentscell()" except that any original type is allowed.
	 * Returns NONODEPROTO if the cell is not associated.
	 */
	private static Cell graphMainView(Cell cell)
	{
		// first check to see if there is a schematics link
		Cell mainSchem = cell.getCellGroup().getMainSchematics();
		if (mainSchem != null) return mainSchem;
// 		for(Iterator it = cell.getCellGroup().getCells(); it.hasNext(); )
// 		{
// 			Cell cellInGroup = (Cell)it.next();
// 			if (cellInGroup.getView() == View.SCHEMATIC) return cellInGroup;
// 			if (cellInGroup.getView().isMultiPageView()) return cellInGroup;
// 		}

		// now check to see if there is any layout link
		for(Iterator it = cell.getCellGroup().getCells(); it.hasNext(); )
		{
			Cell cellInGroup = (Cell)it.next();
			if (cellInGroup.getView() == View.LAYOUT) return cellInGroup;
		}

		// finally check to see if there is any "unknown" link
		for(Iterator it = cell.getCellGroup().getCells(); it.hasNext(); )
		{
			Cell cellInGroup = (Cell)it.next();
			if (cellInGroup.getView() == View.UNKNOWN) return cellInGroup;
		}

		// no contents found
		return null;
	}

	/****************************** EXTRACT CELL INSTANCES ******************************/

	/**
	 * Method to package the selected objects into a new cell.
	 */
	public static void packageIntoCell()
	{
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;
        Highlighter highlighter = wf.getContent().getHighlighter();
        if (highlighter == null) return;

		// get the specified area
		EditWindow wnd = EditWindow.needCurrent();
		if (wnd == null) return;
		Cell curCell = wnd.getCell();
		if (curCell == null)
		{
			System.out.println("No cell in this window");
			return;
		}
		Rectangle2D bounds = highlighter.getHighlightedArea(wnd);
		if (bounds == null)
		{
			System.out.println("Must first select circuitry to package");
			return;
		}

		String newCellName = JOptionPane.showInputDialog("New cell name:", curCell.getName());
		if (newCellName == null) return;
		newCellName += "{" + curCell.getView().getAbbreviation() + "}";

		PackageCell job = new PackageCell(curCell, bounds, newCellName);
	}

	/**
	 * This class implement the command to delete unused old versions of cells.
	 */
	private static class PackageCell extends Job
	{
		Cell curCell;
		Rectangle2D bounds;
		String newCellName;

		protected PackageCell(Cell curCell, Rectangle2D bounds, String newCellName)
		{
			super("Package Cell", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.curCell = curCell;
			this.bounds = bounds;
			this.newCellName = newCellName;
			startJob();
		}

		public boolean doIt()
		{
			// create the new cell
			Cell cell = Cell.makeInstance(Library.getCurrent(), newCellName);
			if (cell == null) return false;

			// copy the nodes into the new cell
			HashMap newNodes = new HashMap();
			for(Iterator sIt = curCell.searchIterator(bounds); sIt.hasNext(); )
			{
				Geometric look = (Geometric)sIt.next();
				if (!(look instanceof NodeInst)) continue;
				NodeInst ni = (NodeInst)look;

				String name = null;
				Name oldName = ni.getNameKey();
				if (!oldName.isTempname()) name = oldName.toString();
				NodeInst newNi = NodeInst.makeInstance(ni.getProto(), new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY()),
					ni.getXSize(), ni.getYSize(), cell, ni.getAngle(), name, 0);
				if (newNi == null) return false;
				newNodes.put(ni, newNi);
				newNi.lowLevelSetUserbits(ni.lowLevelGetUserbits());
				newNi.copyVarsFrom(ni);
				newNi.setNameTextDescriptor(ni.getNameTextDescriptor());
	
				// make ports where this nodeinst has them
				for(Iterator it = ni.getExports(); it.hasNext(); )
				{
					Export pp = (Export)it.next();
					PortInst pi = newNi.findPortInstFromProto(pp.getOriginalPort().getPortProto());
					Export newPp = Export.newInstance(cell, pi, pp.getName());
					if (newPp != null)
					{
						newPp.setCharacteristic(pp.getCharacteristic());
						newPp.setTextDescriptor(pp.getTextDescriptor());
						newPp.copyVarsFrom(pp);
					}
				}
			}
	
			// copy the arcs into the new cell
			for(Iterator sIt = curCell.searchIterator(bounds); sIt.hasNext(); )
			{
				Geometric look = (Geometric)sIt.next();
				if (!(look instanceof ArcInst)) continue;
				ArcInst ai = (ArcInst)look;
				NodeInst niTail = (NodeInst)newNodes.get(ai.getTail().getPortInst().getNodeInst());
				NodeInst niHead = (NodeInst)newNodes.get(ai.getHead().getPortInst().getNodeInst());
				if (niTail == null || niHead == null) continue;
				PortInst piTail = niTail.findPortInstFromProto(ai.getTail().getPortInst().getPortProto());
				PortInst piHead = niHead.findPortInstFromProto(ai.getHead().getPortInst().getPortProto());

				String name = null;
				Name oldName = ai.getNameKey();
				if (!oldName.isTempname()) name = oldName.toString();
				ArcInst newAi = ArcInst.makeInstance(ai.getProto(), ai.getWidth(), piHead, piTail, ai.getHead().getLocation(),
				        ai.getTail().getLocation(), name);
				if (newAi == null) return false;
				newAi.copyPropertiesFrom(ai);
			}
			System.out.println("Cell " + cell.describe() + " created");
			return true;
		}
	}
	
	/**
	 * Method to yank the contents of complex node instance "topno" into its
	 * parent cell.
	 */
	public static void extractCells()
	{
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;

		ExtractCellInstances job = new ExtractCellInstances(MenuCommands.getSelectedObjects(true, false));
	}

	/**
	 * This class implement the command to delete unused old versions of cells.
	 */
	private static class ExtractCellInstances extends Job
	{
        private List nodes;

		protected ExtractCellInstances(List highlighted)
		{
			super("Extract Cell Instances", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.nodes = highlighted;
			startJob();
		}

		public boolean doIt()
		{
			boolean foundInstance = false;
			for(Iterator it = nodes.iterator(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				NodeProto np = ni.getProto();
				if (!(np instanceof Cell)) continue;
				foundInstance = true;
				extractOneNode(ni);
			}
			if (!foundInstance)
			{
				System.out.println("Must select cell instances to extract");
				return false;
			}
			return true;
		}
	}

	private static void extractOneNode(NodeInst topno)
	{
		// make transformation matrix for this cell
		Cell cell = topno.getParent();
		Cell subCell = (Cell)topno.getProto();
		AffineTransform localTrans = topno.translateOut();
		AffineTransform localRot = topno.rotateOut();
		localTrans.preConcatenate(localRot);

		// build a list of nodes to copy
		List nodes = new ArrayList();
		for(Iterator it = subCell.getNodes(); it.hasNext(); )
			nodes.add(it.next());

		// sort the nodes by name
		Collections.sort(nodes, new TextUtils.NodesByName());

		// copy the nodes
		HashMap newNodes = new HashMap();
		for(Iterator it = nodes.iterator(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();

			// do not extract "cell center" or "essential bounds" primitives
			NodeProto np = ni.getProto();
			if (np == Generic.tech.cellCenterNode || np == Generic.tech.essentialBoundsNode) continue;

			Point2D pt = new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY());
			localTrans.transform(pt, pt);
			double xSize = ni.getXSizeWithMirror();
			double ySize = ni.getYSizeWithMirror();
			int newAngle = topno.getAngle();
			boolean revAngle = false;
			if (topno.isXMirrored() != topno.isYMirrored()) revAngle = !revAngle;
			if (ni.isXMirrored() != ni.isYMirrored()) revAngle = !revAngle;
			if (revAngle) newAngle = newAngle + 3600 - ni.getAngle(); else
				newAngle += ni.getAngle();
			if (topno.isXMirrored()) xSize = -xSize;
			if (topno.isYMirrored()) ySize = -ySize;

			newAngle = newAngle % 3600;   if (newAngle < 0) newAngle += 3600;
			String name = null;
			if (ni.isUsernamed())
				name = ElectricObject.uniqueObjectName(ni.getName(), cell, NodeInst.class);
			NodeInst newNi = NodeInst.makeInstance(np, pt, xSize, ySize, cell, newAngle, name, 0);
			if (newNi == null) return;
			newNodes.put(ni, newNi);
			newNi.setNameTextDescriptor(ni.getNameTextDescriptor());
			newNi.lowLevelSetUserbits(ni.lowLevelGetUserbits());
			newNi.copyVarsFrom(ni);
		}

		// make a list of arcs to extract
		List arcs = new ArrayList();
		for(Iterator it = subCell.getArcs(); it.hasNext(); )
			arcs.add(it.next());

		// sort the arcs by name
		Collections.sort(arcs, new TextUtils.ArcsByName());

		// extract the arcs
		for(Iterator it = arcs.iterator(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();

			// ignore arcs connected to nodes that didn't get yanked
			NodeInst niTail = (NodeInst)newNodes.get(ai.getTail().getPortInst().getNodeInst());
			NodeInst niHead = (NodeInst)newNodes.get(ai.getHead().getPortInst().getNodeInst());
			if (niTail == null || niHead == null) continue;
			PortInst piTail = niTail.findPortInstFromProto(ai.getTail().getPortInst().getPortProto());
			PortInst piHead = niHead.findPortInstFromProto(ai.getHead().getPortInst().getPortProto());

			Point2D ptTail = new Point2D.Double();
			localTrans.transform(ai.getTail().getLocation(), ptTail);
			Point2D ptHead = new Point2D.Double();
			localTrans.transform(ai.getHead().getLocation(), ptHead);

			// make sure the head end fits in the port
			Poly polyHead = piHead.getPoly();
			if (!polyHead.isInside(ptHead))
			{
				ptHead.setLocation(polyHead.getCenterX(), polyHead.getCenterY());
			}

			// make sure the tail end fits in the port
			Poly polyTail = piTail.getPoly();
			if (!polyTail.isInside(ptTail))
			{
				ptTail.setLocation(polyTail.getCenterX(), polyTail.getCenterY());
			}

			String name = null;
			if (ai.isUsernamed())
				name = ElectricObject.uniqueObjectName(ai.getName(), cell, ArcInst.class);
			ArcInst newAi = ArcInst.makeInstance(ai.getProto(), ai.getWidth(), piHead, piTail, ptHead, ptTail, name);
			if (newAi == null) return;
			newAi.copyPropertiesFrom(ai);
		}

		// replace arcs to the cell
		List replaceTheseArcs = new ArrayList();
		for(Iterator it = topno.getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			replaceTheseArcs.add(con.getArc());
		}
		for(Iterator it = replaceTheseArcs.iterator(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
//			if ((ai->userbits&DEADA) != 0) continue;
			ArcProto ap = ai.getProto();
			double wid = ai.getWidth();
			String name = null;
			if (ai.isUsernamed())
				name = ElectricObject.uniqueObjectName(ai.getName(), cell, ArcInst.class);
			PortInst [] pis = new PortInst[2];
			Point2D [] pts = new Point2D[2];
			for(int i=0; i<2; i++)
			{
				pis[i] = ai.getConnection(i).getPortInst();
				pts[i] = ai.getConnection(i).getLocation();
				if (pis[i].getNodeInst() != topno) continue;
				Export pp = (Export)pis[i].getPortProto();
				NodeInst subNi = pp.getOriginalPort().getNodeInst();
				NodeInst newNi = (NodeInst)newNodes.get(subNi);
				if (newNi == null) continue;
				pis[i] = newNi.findPortInstFromProto(pp.getOriginalPort().getPortProto());
			}
			if (pis[0] == null || pis[1] == null) continue;

			ai.kill();
			ArcInst newAi = ArcInst.makeInstance(ap, wid, pis[0], pis[1], pts[0], pts[1], name);
			if (newAi == null) return;
            newAi.copyPropertiesFrom(ai);
		}

		// replace the exports
		List existingExports = new ArrayList();
		for(Iterator it = topno.getExports(); it.hasNext(); )
			existingExports.add(it.next());
		for(Iterator it = existingExports.iterator(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			Export subPp = (Export)pp.getOriginalPort().getPortProto();
			NodeInst subNi = subPp.getOriginalPort().getNodeInst();
			NodeInst newNi = (NodeInst)newNodes.get(subNi);
			if (newNi == null) continue;
			PortInst pi = newNi.findPortInstFromProto(subPp.getOriginalPort().getPortProto());
			pp.move(pi);
		}

		// copy the exports if requested
		if (User.isExtractCopiesExports())
		{
			// initialize for queueing creation of new exports
			List queuedExports = new ArrayList();
			for(Iterator it = subCell.getPorts(); it.hasNext(); )
				queuedExports.add(it.next());

			// sort the exports by name
			Collections.sort(queuedExports, new TextUtils.ExportsByName());

			for(Iterator it = queuedExports.iterator(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				NodeInst subNi = pp.getOriginalPort().getNodeInst();
				NodeInst newNi = (NodeInst)newNodes.get(subNi);
				if (newNi == null) continue;
				PortInst pi = newNi.findPortInstFromProto(pp.getOriginalPort().getPortProto());

				// don't copy if the port is already exported
				boolean alreadyDone = false;
				for(Iterator eIt = newNi.getExports(); eIt.hasNext(); )
				{
					Export oPp = (Export)eIt.next();
					if (oPp.getOriginalPort() == pi)
					{
						alreadyDone = true;
						break;
					}
				}
				if (alreadyDone) continue;

				// copy the port
				String portName = ElectricObject.uniqueObjectName(pp.getName(), cell, PortProto.class);
				Export.newInstance(cell, pi, portName);
			}
		}

		// delete the cell instance
		eraseNodeInst(topno);
	}

	/****************************** CLEAN-UP ******************************/

	public static void cleanupPinsCommand(boolean everywhere)
	{
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;
        Highlighter highlighter = wf.getContent().getHighlighter();
        if (highlighter == null) return;

		if (everywhere)
		{
			boolean cleaned = false;
			for(Iterator it = Library.getCurrent().getCells(); it.hasNext(); )
			{
				Cell cell = (Cell)it.next();
				if (cleanupCell(cell, false, highlighter)) cleaned = true;
			}
			if (!cleaned) System.out.println("Nothing to clean");
		} else
		{
			// just cleanup the current cell
            Cell cell = WindowFrame.needCurCell();
			if (cell == null) return;
			cleanupCell(cell, true, highlighter);
		}
	}

	/**
	 * Method to clean-up cell "np" as follows:
	 *   remove stranded pins
	 *   collapse redundant (inline) arcs
	 *   highlight zero-size nodes
	 *   removes duplicate arcs
	 *   highlight oversize pins that allow arcs to connect without touching
	 *   move unattached and invisible pins with text in a different location
	 *   resize oversized pins that don't have oversized arcs on them
	 * Returns true if changes are made.
	 */
	private static boolean cleanupCell(Cell cell, boolean justThis, Highlighter highlighter)
	{
		// look for unused pins that can be deleted
		List pinsToRemove = new ArrayList();
		List pinsToPassThrough = getPinsToPassThrough(cell);
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getFunction() != PrimitiveNode.Function.PIN) continue;

			// if the pin is an export, save it
			if (ni.getNumExports() > 0) continue;

			// if the pin is not connected or displayed, delete it
			if (ni.getNumConnections() == 0)
			{
				// see if the pin has displayable variables on it
				boolean hasDisplayable = false;
				for(Iterator vIt = ni.getVariables(); vIt.hasNext(); )
				{
					Variable var = (Variable)vIt.next();
					if (var.isDisplay()) { hasDisplayable = true;   break; }
				}
				if (hasDisplayable) continue;

				// disallow erasing if lock is on
				int errorCode = cantEdit(cell, ni, true);
				if (errorCode < 0) return false;
				if (errorCode > 0) continue;

				// no displayable variables: delete it
				pinsToRemove.add(ni);
				continue;
			}
		}

		// look for oversized pins that can be reduced in size
		HashMap pinsToScale = new HashMap();
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getFunction() != PrimitiveNode.Function.PIN) continue;

			// if the pin is standard size, leave it alone
			double overSizeX = ni.getXSize() - ni.getProto().getDefWidth();
			if (overSizeX < 0) overSizeX = 0;
			double overSizeY = ni.getYSize() - ni.getProto().getDefHeight();
			if (overSizeY < 0) overSizeY = 0;
			if (overSizeX == 0 && overSizeY == 0) continue;

			// all arcs must connect in the pin center
			boolean arcsInCenter = true;
			for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
			{
				Connection con = (Connection)cIt.next();
				ArcInst ai = con.getArc();
				if (ai.getHead().getPortInst().getNodeInst() == ni)
				{
					if (ai.getHead().getLocation().getX() != ni.getAnchorCenterX()) { arcsInCenter = false;   break; }
					if (ai.getHead().getLocation().getY() != ni.getAnchorCenterY()) { arcsInCenter = false;   break; }
				}
				if (ai.getTail().getPortInst().getNodeInst() == ni)
				{
					if (ai.getTail().getLocation().getX() != ni.getAnchorCenterX()) { arcsInCenter = false;   break; }
					if (ai.getTail().getLocation().getY() != ni.getAnchorCenterY()) { arcsInCenter = false;   break; }
				}
			}
			if (!arcsInCenter) continue;

			// look for arcs that are oversized
			double overSizeArc = 0;
			for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
			{
				Connection con = (Connection)cIt.next();
				ArcInst ai = con.getArc();
				double overSize = ai.getWidth() - ai.getProto().getDefaultWidth();
				if (overSize < 0) overSize = 0;
				if (overSize > overSizeArc) overSizeArc = overSize;
			}

			// if an arc covers the pin, leave the pin
			if (overSizeArc >= overSizeX && overSizeArc >= overSizeY) continue;

			double dSX = 0, dSY = 0;
			if (overSizeArc < overSizeX) dSX = overSizeX - overSizeArc;
			if (overSizeArc < overSizeY) dSY = overSizeY - overSizeArc;
			pinsToScale.put(ni, new Point2D.Double(-dSX, -dSY));
		}

		// look for pins that are invisible and have text in different location
		List textToMove = new ArrayList();
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			Point2D pt = ni.invisiblePinWithOffsetText(false);
			if (pt != null)
				textToMove.add(ni);
		}

		// highlight oversize pins that allow arcs to connect without touching
		int overSizePins = 0;
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getFunction() != PrimitiveNode.Function.PIN) continue;

			// make sure all arcs touch each other
			boolean nodeIsBad = false;
			for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
			{
				Connection con = (Connection)cIt.next();
				ArcInst ai = con.getArc();
				double i = ai.getWidth() - ai.getProto().getWidthOffset();
				Poly poly = ai.makePoly(ai.getLength(), i, Poly.Type.FILLED);
				for(Iterator oCIt = ni.getConnections(); oCIt.hasNext(); )
				{
					Connection oCon = (Connection)oCIt.next();
					ArcInst oAi = oCon.getArc();
					if (ai.getArcIndex() <= oAi.getArcIndex()) continue;
					double oI = oAi.getWidth() - oAi.getProto().getWidthOffset();
					Poly oPoly = oAi.makePoly(oAi.getLength(), oI, Poly.Type.FILLED);
					double dist = poly.separation(oPoly);
					if (dist <= 0) continue;
					nodeIsBad = true;
					break;
				}
				if (nodeIsBad) break;
			}
			if (nodeIsBad)
			{
				if (justThis)
				{
					highlighter.addElectricObject(ni, cell);
				}
				overSizePins++;
			}
		}

		// look for duplicate arcs
		HashMap arcsToKill = new HashMap();
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
			{
				Connection con = (Connection)cIt.next();
				ArcInst ai = con.getArc();
				int otherEnd = 0;
				if (ai.getConnection(0) == con) otherEnd = 1;
				boolean foundAnother = false;
				for(Iterator oCIt = ni.getConnections(); oCIt.hasNext(); )
				{
					Connection oCon = (Connection)oCIt.next();
					ArcInst oAi = oCon.getArc();
					if (ai.getArcIndex() <= oAi.getArcIndex()) continue;
					if (con.getPortInst().getPortProto() != oCon.getPortInst().getPortProto()) continue;
					if (ai.getProto() != oAi.getProto()) continue;
					int oOtherEnd = 0;
					if (oAi.getConnection(0) == oCon) oOtherEnd = 1;
					if (ai.getConnection(otherEnd).getPortInst().getNodeInst() !=
						oAi.getConnection(oOtherEnd).getPortInst().getNodeInst()) continue;
					if (ai.getConnection(otherEnd).getPortInst().getPortProto() !=
						oAi.getConnection(oOtherEnd).getPortInst().getPortProto()) continue;

					// this arc is a duplicate
					arcsToKill.put(oAi, oAi);
					foundAnother = true;
					break;
				}
				if (foundAnother) break;
			}
		}

		// now highlight negative or zero-size nodes
		int zeroSize = 0, negSize = 0;
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getProto() == Generic.tech.cellCenterNode ||
				ni.getProto() == Generic.tech.invisiblePinNode ||
				ni.getProto() == Generic.tech.essentialBoundsNode) continue;
			SizeOffset so = ni.getSizeOffset();
			double sX = ni.getXSize() - so.getLowXOffset() - so.getHighXOffset();
			double sY = ni.getYSize() - so.getLowYOffset() - so.getHighYOffset();
			if (sX > 0 && sY > 0) continue;
			if (justThis)
			{
				highlighter.addElectricObject(ni, cell);
			}
			if (sX < 0 || sY < 0) negSize++; else
				zeroSize++;
		}

		if (pinsToRemove.size() == 0 &&
			pinsToPassThrough.size() == 0 &&
			pinsToScale.size() == 0 &&
			zeroSize == 0 &&
			negSize == 0 &&
			textToMove.size() == 0 &&
			overSizePins == 0 &&
			arcsToKill.size() == 0)
		{
			if (justThis) System.out.println("Nothing to clean");
			return false;
		}

		CleanupChanges job = new CleanupChanges(cell, justThis, pinsToRemove, pinsToPassThrough, pinsToScale, textToMove, arcsToKill,
			zeroSize, negSize, overSizePins);
        job.startJob();
		return true;
	}

	private static List getPinsToPassThrough(Cell cell)
	{
		List pinsToPassThrough = new ArrayList();
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getFunction() != PrimitiveNode.Function.PIN) continue;
	
			// if the pin is an export, save it
			if (ni.getNumExports() > 0) continue;
	
			// if the pin is connected to two arcs along the same slope, delete it
			if (ni.isInlinePin())
			{
				Reconnect re = Reconnect.erasePassThru(ni, false);
				if (re != null)
				{
					pinsToPassThrough.add(re);
				}
			}
		}
		return pinsToPassThrough;
	}

	/**
	 * This class implements the changes needed to cleanup pins in a Cell.
	 */
	public static class CleanupChanges extends Job
	{
		private Cell cell;
		private boolean justThis;
		private List pinsToRemove;
		private List pinsToPassThrough;
		private HashMap pinsToScale;
		private List textToMove;
		private HashMap arcsToKill;
		private int zeroSize, negSize, overSizePins;

		public CleanupChanges(Cell cell, boolean justThis, List pinsToRemove, List pinsToPassThrough, HashMap pinsToScale, List textToMove, HashMap arcsToKill,
			int zeroSize, int negSize, int overSizePins)
		{
			super("Cleanup cell " + cell.describe(), User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.justThis = justThis;
			this.pinsToRemove = pinsToRemove;
			this.pinsToPassThrough = pinsToPassThrough;
			this.pinsToScale = pinsToScale;
			this.textToMove = textToMove;
			this.arcsToKill = arcsToKill;
			this.zeroSize = zeroSize;
			this.negSize = negSize;
			this.overSizePins = overSizePins;
		}

		public boolean doIt()
		{
			// make sure moving the node is allowed
			if (CircuitChanges.cantEdit(cell, null, true) != 0) return false;

			// do the queued operations
			for(Iterator it = pinsToRemove.iterator(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				ni.kill();
			}
            int pinsPassedThrough = 0;
            for(;;)
            {
            	boolean found = false;
				for(Iterator it = pinsToPassThrough.iterator(); it.hasNext(); )
				{
					Reconnect re = (Reconnect)it.next();
					if (!re.ni.isLinked()) continue;
					List created = re.reconnectArcs();
					if (created.size() > 0)
					{
						re.ni.kill();
	                    pinsPassedThrough++;
	                    found = true;
	                }
				}
				if (!found) break;
				pinsToPassThrough = getPinsToPassThrough(cell);
            }
			for(Iterator it = pinsToScale.keySet().iterator(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				Point2D scale = (Point2D)pinsToScale.get(ni);
				ni.modifyInstance(0, 0, scale.getX(), scale.getY(), 0);
			}
			for(Iterator it = textToMove.iterator(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				ni.invisiblePinWithOffsetText(true);
			}
			for(Iterator it = arcsToKill.keySet().iterator(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				if (!ai.isLinked()) continue;
				ai.kill();
			}

			// report what was cleaned
			StringBuffer infstr = new StringBuffer();
			if (!justThis) infstr.append("Cell " + cell.describe() + ":");
			boolean spoke = false;
			if ((pinsToRemove.size()+pinsPassedThrough) != 0)
			{
                int removed = pinsToRemove.size() + pinsPassedThrough;
				infstr.append("Removed " + removed + " pins");
				spoke = true;
			}
			if (arcsToKill.size() != 0)
			{
				if (spoke) infstr.append("; ");
				infstr.append("Removed " + arcsToKill.size() + " duplicate arcs");
				spoke = true;
			}
			if (pinsToScale.size() != 0)
			{
				if (spoke) infstr.append("; ");
				infstr.append("Shrunk " + pinsToScale.size() + " pins");
				spoke = true;
			}
			if (zeroSize != 0)
			{
				if (spoke) infstr.append("; ");
				if (justThis)
				{
					infstr.append("Highlighted " + zeroSize + " zero-size pins");
				} else
				{
					infstr.append("Found " + zeroSize + " zero-size pins");
				}
				spoke = true;
			}
			if (negSize != 0)
			{
				if (spoke) infstr.append("; ");
				if (justThis)
				{
					infstr.append("Highlighted " + negSize + " negative-size pins");
				} else
				{
					infstr.append("Found " + negSize + " negative-size pins");
				}
				spoke = true;
			}
			if (overSizePins != 0)
			{
				if (spoke) infstr.append("; ");
				if (justThis)
				{
					infstr.append("Highlighted " + overSizePins + " oversize pins with arcs that don't touch");
				} else
				{
					infstr.append("Found " + overSizePins + " oversize pins with arcs that don't touch");
				}
				spoke = true;
			}
			if (textToMove.size() != 0)
			{
				if (spoke) infstr.append("; ");
				infstr.append("Moved text on " + textToMove.size() + " pins with offset text");
			}
			System.out.println(infstr.toString());
			return true;
		}
	}

	/**
	 * Method to analyze the current cell and show all nonmanhattan geometry.
	 */
	public static void showNonmanhattanCommand()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;

        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;
        Highlighter highlighter = wf.getContent().getHighlighter();
        if (highlighter == null) return;

		// see which cells (in any library) have nonmanhattan stuff
        HashSet cellsSeen = new HashSet();
		for(Iterator lIt = Library.getLibraries(); lIt.hasNext(); )
		{
			Library lib = (Library)lIt.next();
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				for(Iterator aIt = cell.getArcs(); aIt.hasNext(); )
				{
					ArcInst ai = (ArcInst)aIt.next();
					ArcProto ap = ai.getProto();
					if (ap.getTechnology() == Generic.tech || ap.getTechnology() == Artwork.tech ||
						ap.getTechnology() == Schematics.tech) continue;
					Variable var = ai.getVar(ArcInst.ARC_RADIUS);
					if (var != null) cellsSeen.add(cell);
					if (ai.getHead().getLocation().getX() != ai.getTail().getLocation().getX() &&
						ai.getHead().getLocation().getY() != ai.getTail().getLocation().getY())
							cellsSeen.add(cell);
				}
				for(Iterator nIt = cell.getNodes(); nIt.hasNext(); )
				{
					NodeInst ni = (NodeInst)nIt.next();
					if ((ni.getAngle() % 900) != 0) cellsSeen.add(cell);
				}
			}
		}

		// show the nonmanhattan things in the current cell
		int i = 0;
		for(Iterator aIt = curCell.getArcs(); aIt.hasNext(); )
		{
			ArcInst ai = (ArcInst)aIt.next();
			ArcProto ap = ai.getProto();
			if (ap.getTechnology() == Generic.tech || ap.getTechnology() == Artwork.tech ||
				ap.getTechnology() == Schematics.tech) continue;
			boolean nonMan = false;
			Variable var = ai.getVar(ArcInst.ARC_RADIUS);
			if (var != null) nonMan = true;
			if (ai.getHead().getLocation().getX() != ai.getTail().getLocation().getX() &&
				ai.getHead().getLocation().getY() != ai.getTail().getLocation().getY())
					nonMan = true;
			if (nonMan)
			{
				if (i == 0) highlighter.clear();
				highlighter.addElectricObject(ai, curCell);
				i++;
			}
		}
		for(Iterator nIt = curCell.getNodes(); nIt.hasNext(); )
		{
			NodeInst ni = (NodeInst)nIt.next();
			if ((ni.getAngle() % 900) == 0) continue;
			if (i == 0) highlighter.clear();
			highlighter.addElectricObject(ni, curCell);
			i++;
		}
		if (i == 0) System.out.println("No nonmanhattan objects in this cell"); else
		{
			highlighter.finished();
			System.out.println(i + " objects are not manhattan in this cell");
		}

		// tell about other non-manhatten-ness elsewhere
		for(Iterator lIt = Library.getLibraries(); lIt.hasNext(); )
		{
			Library lib = (Library)lIt.next();
			if (lib.isHidden()) continue;
			int numBad = 0;
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				if (cellsSeen.contains(cell) && cell != curCell) numBad++;
			}
			if (numBad == 0) continue;
			if (lib == Library.getCurrent())
			{
				int cellsFound = 0;
				String infstr = "";
				for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = (Cell)cIt.next();
					if (cell == curCell || !cellsSeen.contains(cell)) continue;
					if (cellsFound > 0) infstr += " ";;
					infstr += cell.describe();
					cellsFound++;
				}
				if (cellsFound == 1)
				{
					System.out.println("Found nonmanhattan geometry in cell " + infstr);
				} else
				{
					System.out.println("Found nonmanhattan geometry in these cells: " + infstr);
				}
			} else
			{
				System.out.println("Found nonmanhattan geometry in library " + lib.getName());
			}
		}
	}

	/**
	 * Method to shorten all selected arcs.
	 * Since arcs may connect anywhere inside of the ports on nodes, a port with nonzero area will allow an arc
	 * to move freely.
	 * This command shortens selected arcs so that their endpoints arrive at the part of the node that allows the shortest arc.
	 */
	public static void shortenArcsCommand()
	{
		ShortenArcs job = new ShortenArcs(MenuCommands.getSelectedObjects(false, true));
	}
	
	/**
	 * This class implements the changes needed to shorten selected arcs.
	 */
	private static class ShortenArcs extends Job
	{
        private List selected;

		private ShortenArcs(List selected)
		{
			super("Shorten selected arcs", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.selected = selected;
            startJob();
		}

		public boolean doIt()
		{
			// make sure shortening is allowed
			Cell cell = WindowFrame.needCurCell();
			if (cell == null) return false;
			if (CircuitChanges.cantEdit(cell, null, true) != 0) return false;

			int l = 0;
			double [] dX = new double[2];
			double [] dY = new double[2];
			for(Iterator it = selected.iterator(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				for(int j=0; j<2; j++)
				{
					Poly portPoly = ai.getConnection(j).getPortInst().getPoly();
					double wid = ai.getWidth() - ai.getProto().getWidthOffset();
					portPoly.reducePortPoly(ai.getConnection(j).getPortInst(), wid, ai.getAngle());
					Point2D closest = portPoly.closestPoint(ai.getConnection(1-j).getLocation());
					dX[j] = closest.getX() - ai.getConnection(j).getLocation().getX();
					dY[j] = closest.getY() - ai.getConnection(j).getLocation().getY();
				}
				if (dX[0] != 0 || dY[0] != 0 || dX[1] != 0 || dY[1] != 0)
				{
					ai.modify(0, dX[0], dY[0], dX[1], dY[1]);
					l++;
				}
			}
			System.out.println("Shortened " + l + " arcs");
			return true;
		}
	}

	/****************************** MAKE A NEW VERSION OF A CELL ******************************/

	public static void newVersionOfCell(Cell cell)
	{
		NewCellVersion job = new NewCellVersion(cell);
	}

	/**
	 * This class implement the command to make a new version of a cell.
	 */
	private static class NewCellVersion extends Job
	{
		Cell cell;

		protected NewCellVersion(Cell cell)
		{
			super("Create new Version of cell " + cell.describe(), User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			startJob();
		}

		public boolean doIt()
		{
			Cell newVersion = cell.makeNewVersion();
			if (newVersion == null) return false;

			// change the display of old versions to the new one
			for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = (WindowFrame)it.next();
				WindowContent content = wf.getContent();
				if (content == null) continue;
				if (content.getCell() == cell)
					content.setCell(newVersion, VarContext.globalContext);
			}
			EditWindow.repaintAll();
            System.out.println("Created new version: \""+newVersion.describe()+"\", old version renamed to \""+cell.describe()+"\"");
			return true;
		}
	}

	/****************************** MAKE A COPY OF A CELL ******************************/

	public static void duplicateCell(Cell cell, String newName)
	{
		DuplicateCell job = new DuplicateCell(cell, newName);
	}

	/**
	 * This class implement the command to duplicate a cell.
	 */
	private static class DuplicateCell extends Job
	{
		Cell cell;
		String newName;

		protected DuplicateCell(Cell cell, String newName)
		{
			super("Duplicate cell " + cell.describe(), User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.newName = newName;
			startJob();
		}

		public boolean doIt()
		{
			String newCellName = newName + "{" + cell.getView().getAbbreviation() + "}";
			if (cell.getLibrary().findNodeProto(newCellName) != null)
			{
				int response = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
						"Cell " + newCellName + " already exists.  Make this a new version?", "Confirm duplication",
					JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, new String[] {"Yes", "Cancel"}, "Yes");
				if (response != 0) return false;
			}
			Cell dupCell = Cell.copyNodeProto(cell, cell.getLibrary(), newCellName, false);
			if (dupCell == null) {
                System.out.println("Could not duplicate cell "+cell.describe());
                return false;
            }

            System.out.println("Duplicated cell "+cell+". New cell is "+dupCell+".");

            // if icon of cell is present, duplicate that as well, and replace old icon with new icon in new cell
            for (Iterator it = cell.getNodes(); it.hasNext(); ) {
                NodeInst ni = (NodeInst)it.next();
                if (ni.getProtoEquivalent() == cell) {
                    // this is the icon, duplicate it as well
                    Cell icon = (Cell)ni.getProto();
                    Cell dupIcon = Cell.copyNodeProto(icon, icon.getLibrary(), newName + "{" + icon.getView().getAbbreviation() + "}", false);
                    if (dupIcon == null) {
                        System.out.println("Could not duplicate icon cell "+icon.describe());
                        break;
                    }
                    System.out.println("  Also duplicated icon view, cell "+icon+". New cell is "+dupIcon+".");

                    // replace old icon(s) in duplicated cell
                    for (Iterator it2 = dupCell.getNodes(); it2.hasNext(); ) {
                        NodeInst ni2 = (NodeInst)it2.next();
                        if (ni2.getProto() == icon) {
                            NodeInst newNi2 = ni2.replace(dupIcon, true, true);
                            // replace name on old self-icon
                            newNi2.setName(null);
                        }
                    }

                    break;
                }
            }

			// change the display of old cell to the new one
            WindowFrame curWf = WindowFrame.getCurrentWindowFrame();
            if (curWf != null)
            {
				WindowContent content = curWf.getContent();
				if (content != null && content.getCell() == cell)
				{
					content.setCell(dupCell, VarContext.globalContext);
					content.repaint();
					return true;
				}
            }

            // current cell was not duplicated: see if any displayed cell is
			for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = (WindowFrame)it.next();
				WindowContent content = wf.getContent();
				if (content == null) continue;
				if (content.getCell() == cell)
				{
					content.setCell(dupCell, VarContext.globalContext);
					content.repaint();
					return true;
				}
			}
			return true;
		}
	}

	/****************************** MOVE SELECTED OBJECTS ******************************/

	/**
	 * Method to move the selected geometry by (dX, dY).
	 */
	public static void manyMove(double dX, double dY)
	{
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;
        Highlighter highlighter = wf.getContent().getHighlighter();
        if (highlighter == null) return;

        List highlighted = highlighter.getHighlights();

        // prevent mixing cell-center and non-cell-center
        int nonCellCenterCount = 0;
        Highlight cellCenterHighlight = null;
        for(Iterator it = highlighted.iterator(); it.hasNext(); )
        {
        	Highlight h = (Highlight)it.next();
        	if (h.getType() != Highlight.Type.EOBJ) continue;
        	ElectricObject eObj = h.getElectricObject();
        	if (eObj instanceof NodeInst)
        	{
        		NodeInst ni = (NodeInst)eObj;
        		if (ni.getProto() == Generic.tech.cellCenterNode) cellCenterHighlight = h; else
        			nonCellCenterCount++;
        	} else nonCellCenterCount++;
        }
        if (cellCenterHighlight != null && nonCellCenterCount != 0)
        {
        	System.out.println("Cannot move the Cell-center along with other objects.  Cell-center will not be moved.");
        	highlighted.remove(cellCenterHighlight);
        }
        List highlightedText = highlighter.getHighlightedText(true);
        ManyMove job = new ManyMove(highlighted, highlightedText, dX, dY);
	}

	private static class ManyMove extends Job
	{
		double dX, dY;
        static final boolean verbose = false;
        private List highlighted;
        private List highlightedText;

		protected ManyMove(List highlighted, List highlightedText, double dX, double dY)
		{
			super("Move", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.dX = dX;   this.dY = dY;
            this.highlighted = highlighted;
            this.highlightedText = highlightedText;
			startJob();
		}

		public boolean doIt()
		{
			// get information about what is highlighted
			int total = highlighted.size();
			if (total <= 0) return false;
			Iterator oit = highlighted.iterator();
			Highlight firstH = (Highlight)oit.next();
			ElectricObject firstEObj = firstH.getElectricObject();
			Cell cell = firstH.getCell();

			// make sure moving is allowed
			if (CircuitChanges.cantEdit(cell, null, true) != 0) return false;

			// special case if moving only one node
			if (total == 1 && firstH.getType() == Highlight.Type.EOBJ &&
				((firstEObj instanceof NodeInst) || firstEObj instanceof PortInst))
			{
                NodeInst ni;
                if (firstEObj instanceof PortInst) {
                    ni = ((PortInst)firstEObj).getNodeInst();
                } else {
				    ni = (NodeInst)firstEObj;
                }

				// make sure moving the node is allowed
				if (CircuitChanges.cantEdit(cell, ni, true) != 0) return false;

				ni.modifyInstance(dX, dY, 0, 0, 0);
                if (verbose) System.out.println("Moved "+ni.describe()+": delta(X,Y) = ("+dX+","+dY+")");
                StatusBar.updateStatusBar();
				return true;
			}

			// special case if moving diagonal fixed-angle arcs connected to single manhattan arcs
			boolean found = false;
			for(Iterator it = highlighted.iterator(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				if (h.getType() != Highlight.Type.EOBJ) continue;
				ElectricObject eobj = h.getElectricObject();
				if (eobj instanceof ArcInst)
				{
					ArcInst ai = (ArcInst)eobj;
					if (ai.getHead().getLocation().getX() != ai.getTail().getLocation().getX() &&
						ai.getHead().getLocation().getY() != ai.getTail().getLocation().getY())
					{
						if (ai.isFixedAngle() && !ai.isRigid())
						{
							int j;
							for(j=0; j<2; j++)
							{
								NodeInst ni = ai.getConnection(j).getPortInst().getNodeInst();
								ArcInst oai = null;
								for(Iterator pIt = ni.getConnections(); pIt.hasNext(); )
								{
									Connection con = (Connection)pIt.next();
									if (con.getArc() == ai) continue;
									if (oai == null) oai = con.getArc(); else
									{
										oai = null;
										break;
									}
								}
								if (oai == null) break;
								if (oai.getHead().getLocation().getX() != oai.getTail().getLocation().getX() &&
									oai.getHead().getLocation().getY() != oai.getTail().getLocation().getY()) break;
							}
							if (j >= 2) { found = true;   break; }
						}
					}
				}
			}
			if (found)
			{
				// meets the test: make the special move to slide other orthogonal arcs
				for(Iterator it = highlighted.iterator(); it.hasNext(); )
				{
					Highlight h = (Highlight)it.next();
					if (h.getType() != Highlight.Type.EOBJ) continue;
					ElectricObject eobj = h.getElectricObject();
					if (!(eobj instanceof ArcInst)) continue;
					ArcInst ai = (ArcInst)eobj;

					double [] deltaXs = new double[2];
					double [] deltaYs = new double[2];
					double [] deltaNulls = new double[2];
					int [] deltaRots = new int[2];
					NodeInst [] niList = new NodeInst[2];
					deltaNulls[0] = deltaNulls[1] = 0;
					deltaXs[0] = deltaYs[0] = deltaXs[1] = deltaYs[1] = 0;
					int arcangle = ai.getAngle();
					int j;
					for(j=0; j<2; j++)
					{
						NodeInst ni = ai.getConnection(j).getPortInst().getNodeInst();
						niList[j] = ni;
						ArcInst oai = null;
						for(Iterator pIt = ni.getConnections(); pIt.hasNext(); )
						{
							Connection con = (Connection)pIt.next();
							if (con.getArc() != ai) { oai = con.getArc();   break; }
						}
						if (oai == null) break;
						if (DBMath.doublesEqual(oai.getHead().getLocation().getX(), oai.getTail().getLocation().getX()))
						{
							Point2D iPt = DBMath.intersect(oai.getHead().getLocation(), 900,
								new Point2D.Double(ai.getHead().getLocation().getX()+dX, ai.getHead().getLocation().getY()+dY), arcangle);
							deltaXs[j] = iPt.getX() - ai.getConnection(j).getLocation().getX();
							deltaYs[j] = iPt.getY() - ai.getConnection(j).getLocation().getY();
						} else if (DBMath.doublesEqual(oai.getHead().getLocation().getY(), oai.getTail().getLocation().getY()))
						{
							Point2D iPt = DBMath.intersect(oai.getHead().getLocation(), 0,
								new Point2D.Double(ai.getHead().getLocation().getX()+dX, ai.getHead().getLocation().getY()+dY), arcangle);
							deltaXs[j] = iPt.getX() - ai.getConnection(j).getLocation().getX();
							deltaYs[j] = iPt.getY() - ai.getConnection(j).getLocation().getY();
						}
					}
					if (j < 2) continue;
					deltaRots[0] = deltaRots[1] = 0;
					NodeInst.modifyInstances(niList, deltaXs, deltaYs, deltaNulls, deltaNulls, deltaRots);
				}
                if (verbose) System.out.println("Moved many objects: delta(X,Y) = ("+dX+","+dY+")");
                StatusBar.updateStatusBar();
				return true;
			}

			// special case if moving only arcs and they slide
			boolean onlySlidable = true, foundArc = false;
			for(Iterator it = highlighted.iterator(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				if (h.getType() != Highlight.Type.EOBJ) continue;
				ElectricObject eobj = h.getElectricObject();
				if (eobj instanceof ArcInst)
				{
					ArcInst ai = (ArcInst)eobj;
					foundArc = true;
					// see if the arc moves in its ports
					if (ai.isSlidable())
					{
						Connection head = ai.getHead();
						Connection tail = ai.getTail();
						Point2D newHead = new Point2D.Double(head.getLocation().getX()+dX, head.getLocation().getY()+dY);
						Point2D newTail = new Point2D.Double(tail.getLocation().getX()+dX, tail.getLocation().getY()+dY);
						if (ai.stillInPort(head, newHead, true) && ai.stillInPort(tail, newTail, true)) continue;
					}
				}
				onlySlidable = false;
			}
			if (foundArc && onlySlidable)
			{
				for(Iterator it = highlighted.iterator(); it.hasNext(); )
				{
					Highlight h = (Highlight)it.next();
					if (h.getType() != Highlight.Type.EOBJ) continue;
					ElectricObject eobj = h.getElectricObject();
					if (eobj instanceof ArcInst)
					{
						ArcInst ai = (ArcInst)eobj;
						ai.modify(0, dX, dY, dX, dY);
                        if (verbose) System.out.println("Moved "+ai.describe()+": delta(X,Y) = ("+dX+","+dY+")");
					}
				}
                StatusBar.updateStatusBar();
				return true;
			}

			// make flag to track the nodes that move
			HashSet flag = new HashSet();

			// remember the location of every node and arc
			HashMap nodeLocation = new HashMap();
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				nodeLocation.put(ni, new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY()));
			}
			HashMap arcLocation = new HashMap();
			for(Iterator it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				arcLocation.put(ai, new Point2D.Double(ai.getTrueCenterX(), ai.getTrueCenterY()));
			}

			// mark all nodes that want to move
			for(Iterator it = highlighted.iterator(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				if (h.getType() != Highlight.Type.EOBJ) continue;
				ElectricObject eobj = h.getElectricObject();
				if (eobj instanceof PortInst) eobj = ((PortInst)eobj).getNodeInst();
				if (eobj instanceof NodeInst)
				{
					NodeInst ni = (NodeInst)eobj;
					flag.add(ni);
				} else if (eobj instanceof ArcInst)
				{
					ArcInst ai = (ArcInst)eobj;
					NodeInst ni1 = ai.getHead().getPortInst().getNodeInst();
					NodeInst ni2 = ai.getTail().getPortInst().getNodeInst();
					flag.add(ni1);
					flag.add(ni2);
					Layout.setTempRigid(ai, true);
				}
			}

			// count the number of nodes that will move
			int numNodes = 0;
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (!flag.contains(ni)) continue;

				// make sure moving the node is allowed
				int errorCode = cantEdit(cell, ni, true);
				if (errorCode < 0) return false;
				if (errorCode > 0)
				{
					flag.remove(ni);
					continue;
				}
				numNodes++;
			}

			// look at all nodes and move them appropriately
			if (numNodes > 0)
			{
				NodeInst [] nis = new NodeInst[numNodes];
				double [] dXs = new double[numNodes];
				double [] dYs = new double[numNodes];
				double [] dSize = new double[numNodes];
				int [] dRot = new int[numNodes];
				boolean [] dTrn = new boolean[numNodes];
				numNodes = 0;
				for(Iterator it = cell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = (NodeInst)it.next();
					if (!flag.contains(ni)) continue;
					nis[numNodes] = ni;
					dXs[numNodes] = dX;
					dYs[numNodes] = dY;
					dSize[numNodes] = 0;
					dRot[numNodes] = 0;
					dTrn[numNodes] = false;
					numNodes++;
				}
				NodeInst.modifyInstances(nis, dXs, dYs, dSize, dSize, dRot);
			}
			flag = null;

			// look at all arcs and move them appropriately
			for(Iterator it = highlighted.iterator(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				if (h.getType() != Highlight.Type.EOBJ) continue;
				ElectricObject eobj = h.getElectricObject();
				if (!(eobj instanceof ArcInst)) continue;
				ArcInst ai = (ArcInst)eobj;
				Point2D pt = (Point2D)arcLocation.get(ai);
				if (pt.getX() != ai.getTrueCenterX() ||
					pt.getY() != ai.getTrueCenterY()) continue;

				// see if the arc moves in its ports
				boolean headInPort = false, tailInPort = false;
				if (!ai.isRigid() && ai.isSlidable())
				{
					headInPort = ai.stillInPort(ai.getHead(),
						new Point2D.Double(ai.getHead().getLocation().getX()+dX, ai.getHead().getLocation().getY()+dY), true);
					tailInPort = ai.stillInPort(ai.getTail(),
						new Point2D.Double(ai.getTail().getLocation().getX()+dX, ai.getTail().getLocation().getY()+dY), true);
				}

				// if both ends slide in their port, move the arc
				if (headInPort && tailInPort)
				{
					ai.modify(0, dX, dY, dX, dY);
					continue;
				}

				// if neither end can slide in its port, move the nodes
				if (!headInPort && !tailInPort)
				{
					for(int k=0; k<2; k++)
					{
						NodeInst ni;
						if (k == 0) ni = ai.getHead().getPortInst().getNodeInst(); else
							ni = ai.getTail().getPortInst().getNodeInst();
						Point2D nPt = (Point2D)nodeLocation.get(ni);
						if (ni.getAnchorCenterX() != nPt.getX() || ni.getAnchorCenterY() != nPt.getY()) continue;

						// fix all arcs that aren't sliding
						for(Iterator oIt = highlighted.iterator(); oIt.hasNext(); )
						{
							Highlight oH = (Highlight)oIt.next();
							if (oH.getType() != Highlight.Type.EOBJ) continue;
							ElectricObject oEObj = oH.getElectricObject();
							if (oEObj instanceof ArcInst)
							{
								ArcInst oai = (ArcInst)oEObj;
								Point2D aPt = (Point2D)arcLocation.get(oai);
								if (aPt.getX() != oai.getTrueCenterX() ||
									aPt.getY() != oai.getTrueCenterY()) continue;
								if (oai.stillInPort(oai.getHead(),
										new Point2D.Double(ai.getHead().getLocation().getX()+dX, ai.getHead().getLocation().getY()+dY), true) ||
									oai.stillInPort(oai.getTail(),
										new Point2D.Double(ai.getTail().getLocation().getX()+dX, ai.getTail().getLocation().getY()+dY), true))
											continue;
								Layout.setTempRigid(oai, true);
							}
						}
						ni.modifyInstance(dX - (ni.getAnchorCenterX() - nPt.getX()),
							dY - (ni.getAnchorCenterY() - nPt.getY()), 0, 0, 0);
					}
					continue;
				}

//				// only one end is slidable: move other node and the arc
//				for(int k=0; k<2; k++)
//				{
//					if (e[k] != 0) continue;
//					ni = ai->end[k].nodeinst;
//					if (ni->lowx == ni->temp1 && ni->lowy == ni->temp2)
//					{
//						// node "ni" hasn't moved yet but must because arc motion forces it
//						for(j=0; list[j] != NOGEOM; j++)
//						{
//							if (list[j]->entryisnode) continue;
//							oai = list[j]->entryaddr.ai;
//							if (oai->temp1 != (oai->end[0].xpos + oai->end[1].xpos) / 2 ||
//								oai->temp2 != (oai->end[0].ypos + oai->end[1].ypos) / 2) continue;
//							if (oai->end[0].nodeinst == ni) otherend = 1; else otherend = 0;
//							if (db_stillinport(oai, otherend, ai->end[otherend].xpos+dX,
//								ai->end[otherend].ypos+dY)) continue;
//							(void)(*el_curconstraint->setobject)((INTBIG)oai,
//								VARCINST, CHANGETYPETEMPRIGID, 0);
//						}
//						startobjectchange((INTBIG)ni, VNODEINST);
//						modifynodeinst(ni, dX-(ni->lowx-ni->temp1), dY-(ni->lowy-ni->temp2),
//							dX-(ni->lowx-ni->temp1), dY-(ni->lowy-ni->temp2), 0, 0);
//						endobjectchange((INTBIG)ni, VNODEINST);
//
//						if (ai->temp1 != (ai->end[0].xpos + ai->end[1].xpos) / 2 ||
//							ai->temp2 != (ai->end[0].ypos + ai->end[1].ypos) / 2) continue;
//						startobjectchange((INTBIG)ai, VARCINST);
//						(void)modifyarcinst(ai, 0, dX, dY, dX, dY);
//						endobjectchange((INTBIG)ai, VARCINST);
//					}
//				}
			}

			// also move selected text
			moveSelectedText(highlightedText);
            if (verbose) System.out.println("Moved many objects: delta(X,Y) = ("+dX+","+dY+")");
            StatusBar.updateStatusBar();
			return true;
		}

		/*
		 * Method to move the "numtexts" text objects described (as highlight strings)
		 * in the array "textlist", by "odx" and "ody".  Geometry objects in "list" (NOGEOM-terminated)
		 * and the "total" nodes in "nodelist" have already been moved, so don't move any text that
		 * is on these objects.
		 */
		private void moveSelectedText(List highlightedText)
		{
			for(Iterator it = highlightedText.iterator(); it.hasNext(); )
			{
				Highlight high = (Highlight)it.next();

				// disallow moving if lock is on
				Cell np = high.getCell();
				if (np != null)
				{
					int errorCode = cantEdit(np, null, true);
					if (errorCode < 0) return;
					if (errorCode > 0) continue;
				}

				// handle nodes that move with text
				ElectricObject eobj = high.getElectricObject();
				if (high.nodeMovesWithText())
				{
					NodeInst ni = null;
					if (eobj instanceof NodeInst) ni = (NodeInst)eobj;
					if (eobj instanceof Export) ni = ((Export)eobj).getOriginalPort().getNodeInst();
					if (ni != null)
					{
						ni.modifyInstance(dX, dY, 0, 0, 0);
						continue;
					}
				}

				// moving variable on object
				Variable var = high.getVar();
				if (var != null)
				{
					TextDescriptor td = var.getTextDescriptor();
					if (eobj instanceof NodeInst || eobj instanceof PortInst || eobj instanceof Export)
					{
						NodeInst ni = null;
						if (eobj instanceof NodeInst) ni = (NodeInst)eobj; else
							if (eobj instanceof PortInst) ni = ((PortInst)eobj).getNodeInst(); else
								if (eobj instanceof Export) ni = ((Export)eobj).getOriginalPort().getNodeInst();
						if (ni != null)
						{
							adjustTextDescriptor(td, ni);
						}
					} else
					{
						td.setOff(td.getXOff()+dX, td.getYOff()+dY);
					}
				} else
				{
					if (high.getName() != null)
					{
						TextDescriptor td = ((Geometric)eobj).getNameTextDescriptor();
						if (eobj instanceof NodeInst)
						{
							NodeInst ni = (NodeInst)eobj;
							adjustTextDescriptor(td, ni);
						} else
							td.setOff(td.getXOff()+dX, td.getYOff()+dY);
					} else
					{
						if (eobj instanceof Export)
						{
							Export pp = (Export)eobj;
							TextDescriptor td = pp.getTextDescriptor();
//							td.setOff(td.getXOff()+dX, td.getYOff()+dY);
							adjustTextDescriptor(td, pp.getOriginalPort().getNodeInst());
						}
					}
				}
			}
		}

		private void adjustTextDescriptor(TextDescriptor td, NodeInst ni)
		{
			Point2D curLoc = new Point2D.Double(ni.getAnchorCenterX()+td.getXOff(), ni.getAnchorCenterY()+td.getYOff());
			AffineTransform rotateOut = ni.rotateOut();
			rotateOut.transform(curLoc, curLoc);
			curLoc.setLocation(curLoc.getX()+dX, curLoc.getY()+dY);
			AffineTransform rotateIn = ni.rotateIn();
			rotateIn.transform(curLoc, curLoc);
			td.setOff(curLoc.getX()-ni.getAnchorCenterX(), curLoc.getY()-ni.getAnchorCenterY());
		}
	}


    /**
     * Store information of new arc to be created that reconnects
     * two arcs that will be deleted
     */
    private static class ReconnectedArc
    {
        /** port at other end of arc */						private PortInst [] reconPi;
        /** coordinate at other end of arc */				private Point2D [] recon;
        /** old arc insts that will be deleted */           private ArcInst [] reconAr;
        /** prototype of new arc */							private ArcProto ap;
        /** width of new arc */								private double wid;
        /** true to make new arc directional */				private boolean directional;
        /** true to ignore the head of the new arc */		private boolean ignoreHead;
        /** true to ignore the tail of the new arc */		private boolean ignoreTail;
        /** true to negate the head of the new arc */		private boolean negateHead;
        /** true to negate the tail of the new arc */		private boolean negateTail;
        /** true to reverse the head/tail on new arc */		private boolean reverseEnd;
        /** the name to use on the reconnected arc */		private String arcName;
        /** TextDescriptor for the reconnected arc name */	private TextDescriptor arcNameTD;
    }

	/**
	 * This class handles deleting pins that are between two arcs,
	 * and reconnecting the arcs without the pin.
	 */
	public static class Reconnect
	{
		/** node in the center of the reconnect */			private NodeInst ni;
        /** list of reconnected arcs */                     private ArrayList reconnectedArcs;

		/**
		 * Method to find a possible Reconnect through a given NodeInst.
		 * @param ni the NodeInst to examine.
		 * @param allowdiffs true to allow differences in the two arcs.
		 * If this is false, then different width arcs, or arcs that are not lined up
		 * precisely, will not be considered for reconnection.
		 * @return a Reconnect object that describes the reconnection to be done.
		 * Returns null if no reconnection can be found.
		 */
		public static Reconnect erasePassThru(NodeInst ni, boolean allowdiffs)
		{
			// disallow erasing if lock is on
			Cell cell = ni.getParent();
			if (cantEdit(cell, ni, true) != 0) return null;
			// Netlist netlist = cell.getUserNetlist(); Commented 07.01.04 by DN to avoid Netlist recalculation

            Reconnect recon = new Reconnect();
            recon.ni = ni;
            recon.reconnectedArcs = new ArrayList();

            // get all arcs connected to each portinst on node
            for (Iterator it = ni.getPortInsts(); it.hasNext(); ) {
                PortInst pi = (PortInst)it.next();

                ArrayList arcs = new ArrayList();
                for (Iterator it2 = pi.getConnections(); it2.hasNext(); ) {
                    Connection conn = (Connection)it2.next();
                    ArcInst ai = conn.getArc();
                    // ignore arcs that connect from the node to itself
                    if (ai.getHead().getPortInst().getNodeInst() == ai.getTail().getPortInst().getNodeInst())
                        continue;
                    arcs.add(ai);
                }

                // go through all arcs on this portinst and see if any can be reconnected
                while (arcs.size() > 1) {
                    ArcInst ai1 = (ArcInst)arcs.remove(0);
                    for (Iterator it2 = arcs.iterator(); it2.hasNext(); ) {
                        ArcInst ai2 = (ArcInst)it2.next();

                        ReconnectedArc ra = reconnectArcs(pi, ai1, ai2, allowdiffs);
                        // if reconnection to be made, add to list
                        if (ra != null) recon.reconnectedArcs.add(ra);
                    }
                }
            }
            if (recon.reconnectedArcs.size() == 0) return null;

            return recon;
		}

        /** Returns null if couldn't reconnect arcs together */
        private static ReconnectedArc reconnectArcs(PortInst pi, ArcInst ai1, ArcInst ai2, boolean allowdiffs) {

            // verify that the two arcs to merge have the same type
            if (ai1.getProto() != ai2.getProto()) return null;

            ReconnectedArc ra = new ReconnectedArc();
            ra.ap = ai1.getProto();
            ra.reconPi = new PortInst[2];
            ra.recon = new Point2D[2];
            ra.reconAr = new ArcInst[2];
            ra.reconAr[0] = ai1;
            ra.reconAr[1] = ai2;
            Point2D [] orig = new Point2D[2];               // end points on port that will be deleted
            Point2D [] delta = new Point2D[2];              // deltaX,Y of arc

            // get end points of arcs
            for (int i=0; i<2; i++) {
                if (ai1.getConnection(i).getPortInst() != pi) {
                    ra.reconPi[0] = ai1.getConnection(i).getPortInst();
                    ra.recon[0] = ai1.getConnection(i).getLocation();
                } else {
                    orig[0] = ai1.getConnection(i).getLocation();
                }
                if (ai2.getConnection(i).getPortInst() != pi) {
                    ra.reconPi[1] = ai2.getConnection(i).getPortInst();
                    ra.recon[1] = ai2.getConnection(i).getLocation();
                } else {
                    orig[1] = ai2.getConnection(i).getLocation();
                }
            }
            delta[0] = new Point2D.Double(ra.recon[0].getX() - orig[0].getX(),
                                          ra.recon[0].getY() - orig[0].getY());
            delta[1] = new Point2D.Double(ra.recon[1].getX() - orig[1].getX(),
                                          ra.recon[1].getY() - orig[1].getY());

            if (!allowdiffs)
            {
                // verify that the two arcs to merge have the same width
                if (ai1.getWidth() != ai2.getWidth()) return null;

                // verify that the two arcs have the same slope
                if ((delta[1].getX()*delta[0].getY()) != (delta[0].getX()*delta[1].getY())) return null;
                if (orig[0].getX() != orig[1].getX() || orig[0].getY() != orig[1].getY())
                {
                    // did not connect at the same location: be sure that angle is consistent
                    if (delta[0].getX() != 0 || delta[0].getY() != 0)
                    {
                        if (((orig[0].getX()-orig[1].getX())*delta[0].getY()) !=
                            (delta[0].getX()*(orig[0].getY()-orig[1].getY()))) return null;
                    } else if (delta[1].getX() != 0 || delta[1].getY() != 0)
                    {
                        if (((orig[0].getX()-orig[1].getX())*delta[1].getY()) !=
                            (delta[1].getX()*(orig[0].getY()-orig[1].getY()))) return null;
                    } else return null;
                }
            }

            // ok to connect arcs
            ra.wid = ai1.getWidth();

            ra.directional = ai1.isDirectional() || ai2.isDirectional();
            ra.ignoreHead = ai1.isSkipHead() || ai2.isSkipHead();
            ra.ignoreTail = ai1.isSkipTail() || ai2.isSkipTail();
            ra.negateHead = ai1.getHead().isNegated() || ai2.getHead().isNegated();
            ra.negateTail = ai1.getTail().isNegated() || ai2.getTail().isNegated();
            ra.reverseEnd = ai1.isReverseEnds() || ai2.isReverseEnds();
            ra.arcName = null;
            if (ai1.getName() != null && !ai1.getNameKey().isTempname())
            {
                ra.arcName = ai1.getName();
                ra.arcNameTD = ai1.getNameTextDescriptor();
            }
            if (ai2.getName() != null && !ai2.getNameKey().isTempname())
            {
                ra.arcName = ai2.getName();
                ra.arcNameTD = ai2.getNameTextDescriptor();
            }

            // special code to handle directionality
            if (ra.directional || ra.negateHead || ra.negateTail || ra.ignoreHead || ra.ignoreTail || ra.reverseEnd)
            {
                // reverse ends if the arcs point the wrong way
                for(int i=0; i<2; i++)
                {
                    if (ra.reconAr[i].getConnection(i).getPortInst() == pi)
                    {
                        if (ra.reconAr[i].isReverseEnds()) ra.reverseEnd = false; else
                            ra.reverseEnd = true;
                    }
                }
            }

            return ra;
        }


		/**
		 * Method to implement the reconnection in this Reconnect.
		 * @return list of newly created ArcInst that reconnects.
		 */
		public List reconnectArcs()
		{
			// kill the intermediate pin
			//eraseNodeInst(ni);

            List newArcs = new ArrayList();

			// reconnect the arcs
            for (Iterator it = reconnectedArcs.iterator(); it.hasNext(); )
            {
                ReconnectedArc ra = (ReconnectedArc)it.next();
                if (!ra.reconPi[0].getNodeInst().isLinked() || !ra.reconPi[1].getNodeInst().isLinked()) continue;
                ArcInst newAi = ArcInst.makeInstance(ra.ap, ra.wid, ra.reconPi[0], ra.reconPi[1], ra.recon[0], ra.recon[1], null);
                if (newAi == null) continue;
                newAi.setDirectional(ra.directional);
                newAi.setSkipHead(ra.ignoreHead);
                newAi.setSkipTail(ra.ignoreTail);
                newAi.getHead().setNegated(ra.negateHead);
                newAi.getTail().setNegated(ra.negateTail);
                if (ra.reverseEnd) newAi.setReverseEnds(true);
                if (ra.arcName != null)
                {
                    newAi.setName(ra.arcName);
                    newAi.setNameTextDescriptor(ra.arcNameTD);
                }
                newAi.copyVarsFrom(ra.reconAr[0]);
                newAi.copyVarsFrom(ra.reconAr[1]);
                newArcs.add(newAi);
            }
			return newArcs;
		}
	};

	/****************************** COPY CELLS ******************************/

	/**
	 * recursive helper method for "us_copycell" which copies cell "fromCell"
	 * to a new cell called "toName" in library "toLib" with the new view type
	 * "toView".  All needed subcells are copied (unless "noSubCells" is true).
	 * All shared view cells referenced by variables are copied too
	 * (unless "noRelatedViews" is true).  If "useExisting" is TRUE, any subcells
	 * that already exist in the destination library will be used there instead of
	 * creating a cross-library reference.
	 * If "move" is nonzero, delete the original after copying, and update all
	 * references to point to the new cell.  If "subDescript" is empty, the operation
	 * is a top-level request.  Otherwise, this is for a subcell, so only create a
	 * new cell if one with the same name and date doesn't already exists.
	 */
	private static HashSet cellsCopied = null;
	public static Cell copyRecursively(Cell fromCell, String toName, Library toLib,
		View toView, boolean verbose, boolean move, String subDescript, boolean noRelatedViews,
		boolean noSubCells, boolean useExisting)
	{
		Date fromCellCreationDate = fromCell.getCreationDate();
		Date fromCellRevisionDate = fromCell.getRevisionDate();

		boolean topLevel = subDescript.length() == 0;
		if (topLevel) cellsCopied = new HashSet();

		// see if the cell is already there
		for(Iterator it = cellsCopied.iterator(); it.hasNext(); )
		{
			Cell copiedCell = (Cell)it.next();
			if (copiedCell.getName().equalsIgnoreCase(toName) && copiedCell.getView() == toView)
				return copiedCell;
		}

		// copy subcells
		if (!noSubCells)
		{
			boolean found = true;
			while (found)
			{
				found = false;
				for(Iterator it = fromCell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = (NodeInst)it.next();
					NodeProto np = ni.getProto();
					if (!(np instanceof Cell)) continue;
					Cell cell = (Cell)np;

					// allow cross-library references to stay
//					if (cell.getLibrary() != fromCell.getLibrary()) continue;
					if (cell.getLibrary() == toLib) continue;

					// see if the cell is already there
					if (inDestLib(cell)) continue;

					// copy subcell if not already there
					Cell oNp = copyRecursively(cell, cell.getName(), toLib, cell.getView(),
						verbose, move, "subcell ", noRelatedViews, noSubCells, useExisting);
					if (oNp == null)
					{
						if (move) System.out.println("Move of subcell " + cell.describe() + " failed"); else
							System.out.println("Copy of subcell " + cell.describe() + " failed");
						return null;
					}
					found = true;
					break;
				}
			}
		}

		// also copy equivalent views
		if (!noRelatedViews)
		{
			// first copy the icons
			boolean found = true;
			Cell fromCellWalk = fromCell;
			while (found)
			{
				found = false;
				for(Iterator it = fromCellWalk.getCellGroup().getCells(); it.hasNext(); )
				{
					Cell np = (Cell)it.next();
					if (!np.isIcon()) continue;

					// see if the cell is already there
					if (inDestLib(np)) continue;

					// copy equivalent view if not already there
					Cell oNp = copyRecursively(np, np.getName(), toLib, np.getView(),
						verbose, move, "alternate view ", true, noSubCells, useExisting);
					if (oNp == null)
					{
						if (move) System.out.println("Move of alternate view " + np.describe() + " failed"); else
							System.out.println("Copy of alternate view " + np.describe() + " failed");
						return null;
					}
					found = true;
					break;
				}
			}

			// now copy the rest
			found = true;
			while (found)
			{
				found = false;
				for(Iterator it = fromCellWalk.getCellGroup().getCells(); it.hasNext(); )
				{
					Cell np = (Cell)it.next();
					if (np.isIcon()) continue;

					// see if the cell is already there
					if (inDestLib(np)) continue;

					// copy equivalent view if not already there
					Cell oNp = copyRecursively(np, np.getName(), toLib, np.getView(),
						verbose, move, "alternate view ", true, noSubCells, useExisting);
					if (oNp == null)
					{
						if (move) System.out.println("Move of alternate view " + np.describe() + " failed"); else
							System.out.println("Copy of alternate view " + np.describe() + " failed");
						return null;
					}
					found = true;
					break;
				}
			}
		}

		// see if the cell is NOW there
		for(Iterator it = cellsCopied.iterator(); it.hasNext(); )
		{
			Cell copiedCell = (Cell)it.next();
			if (copiedCell.getName().equalsIgnoreCase(toName) && copiedCell.getView() == toView)
				return copiedCell;
		}

		// copy the cell
		String newName = toName;
		if (toView.getAbbreviation().length() > 0)
		{
			newName = toName + "{" + toView.getAbbreviation() + "}";
		}
		Cell newFromCell = Cell.copyNodeProto(fromCell, toLib, newName, useExisting);
		if (newFromCell == null)
		{
			if (move)
			{
				System.out.println("Move of " + subDescript + fromCell.describe() + " failed");
			} else
			{
				System.out.println("Copy of " + subDescript + fromCell.describe() + " failed");
			}
			return null;
		}

		// remember that this cell was copied
		cellsCopied.add(newFromCell);

        // Message before the delete!!
		if (verbose)
		{
			if (fromCell.getLibrary() != toLib)
			{
				String msg = "";
				if (move) msg += "Moved "; else
					 msg += "Copied ";
				msg += subDescript + fromCell.libDescribe() + " to library " + toLib.getName();
				System.out.println(msg);
			} else
			{
				System.out.println("Copied " + subDescript + newFromCell.describe());
			}
		}

		// if moving, adjust pointers and kill original cell
		if (move)
		{
			// clear highlighting if the current node is being replaced
//			list = us_gethighlighted(WANTNODEINST, 0, 0);
//			for(i=0; list[i] != NOGEOM; i++)
//			{
//				if (!list[i]->entryisnode) continue;
//				ni = list[i]->entryaddr.ni;
//				if (ni->proto == fromCell) break;
//			}
//			if (list[i] != NOGEOM) us_clearhighlightcount();

			// now replace old instances with the moved one
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell np = (Cell)cIt.next();
					boolean found = true;
					while (found)
					{
						found = false;
						for(Iterator nIt = np.getNodes(); nIt.hasNext(); )
						{
							NodeInst ni = (NodeInst)nIt.next();
							if (ni.getProto() == fromCell)
							{
								NodeInst replacedNi = ni.replace(newFromCell, false, false);
								if (replacedNi == null)
									System.out.println("Error moving node " + ni.describe() + " in cell " + np.describe());
								found = true;
								break;
							}
						}
					}
				}
			}
			doKillCell(fromCell);
			fromCell = null;
		}
		return newFromCell;
	}

	/**
	 * Method to return true if a cell like "cell" exists in library "lib".
	 */
	private static boolean inDestLib(Cell cell)
	{
		for(Iterator it = cellsCopied.iterator(); it.hasNext(); )
		{
			Cell copiedCell = (Cell)it.next();
			if (copiedCell.getName().equalsIgnoreCase(cell.getName()) && copiedCell.getView() == cell.getView())
				return true;
		}
		return false;
	}

	/****************************** CHANGE CELL EXPANSION ******************************/

	private static HashSet expandFlagBit;

	/**
	 * This method implements the command to expand the selected cells by 1 level down.
	 */
	public static void expandOneLevelDownCommand()
	{
		DoExpandCommands(false, 1);
	}

	/**
	 * This method implements the command to expand the selected cells all the way to the bottom of the hierarchy.
	 */
	public static void expandFullCommand()
	{
		DoExpandCommands(false, Integer.MAX_VALUE);
	}

	/**
	 * This method implements the command to expand the selected cells by a given number of levels from the top.
	 */
	public static void expandSpecificCommand()
	{
		Object obj = JOptionPane.showInputDialog("Number of levels to expand", "1");
		int levels = TextUtils.atoi((String)obj);

		DoExpandCommands(false, levels);
	}

	/**
	 * This method implements the command to unexpand the selected cells by 1 level up.
	 */
	public static void unexpandOneLevelUpCommand()
	{
		DoExpandCommands(true, 1);
	}

	/**
	 * This method implements the command to unexpand the selected cells all the way from the bottom of the hierarchy.
	 */
	public static void unexpandFullCommand()
	{
		DoExpandCommands(true, Integer.MAX_VALUE);
	}

	/**
	 * This method implements the command to unexpand the selected cells by a given number of levels from the bottom.
	 */
	public static void unexpandSpecificCommand()
	{
		Object obj = JOptionPane.showInputDialog("Number of levels to unexpand", "1");
		int levels = TextUtils.atoi((String)obj);

		DoExpandCommands(true, levels);
	}

	public static void peekCommand()
	{
		EditWindow wnd = EditWindow.needCurrent();
		if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();
        if (highlighter == null) return;

		Rectangle2D bounds = highlighter.getHighlightedArea(wnd);
		if (bounds == null)
		{
			System.out.println("Must define an area in which to display");
			return;
		}
		wnd.repaintContents(bounds);
	}

	private static void DoExpandCommands(boolean unExpand, int amount)
	{
		List list = MenuCommands.getSelectedObjects(true, false);
		CircuitChanges.ExpandUnExpand job = new CircuitChanges.ExpandUnExpand(list, unExpand, amount);
	}

	/**
	 * Class to read a library in a new thread.
	 */
	private static class ExpandUnExpand extends Job
	{
		List list;
		boolean unExpand;
		int amount;
		
		protected ExpandUnExpand(List list, boolean unExpand, int amount)
		{
			super("Change Cell Expansion", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.list = list;
			this.unExpand = unExpand;
			this.amount = amount;
			startJob();
		}

		public boolean doIt()
		{
			expandFlagBit = new HashSet();
			if (unExpand)
			{
				for(Iterator it = list.iterator(); it.hasNext(); )
				{
					NodeInst ni = (NodeInst)it.next();
					NodeProto np = ni.getProto();
					if (!(np instanceof Cell)) continue;
					{
						if (ni.isExpanded())
							setUnExpand(ni, amount);
					}
				}
			}
			for(Iterator it = list.iterator(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (unExpand) doUnExpand(ni); else
					doExpand(ni, amount, 0);
				Undo.redrawObject(ni);
			}
			expandFlagBit = null;
			EditWindow.repaintAllContents();
			return true;
		}
	}

	/**
	 * Method to recursively expand the cell "ni" by "amount" levels.
	 * "sofar" is the number of levels that this has already been expanded.
	 */
	private static void doExpand(NodeInst ni, int amount, int sofar)
	{
		if (!ni.isExpanded())
		{
			// expanded the cell
			ni.setExpanded();

			// if depth limit reached, quit
			if (++sofar >= amount) return;
		}

		// explore insides of this one
		NodeProto np = ni.getProto();
		if (!(np instanceof Cell)) return;
		Cell cell = (Cell)np;
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst subNi = (NodeInst)it.next();
			NodeProto subNp = subNi.getProto();
			if (!(subNp instanceof Cell)) continue;
			Cell subCell = (Cell)subNp;

			// ignore recursive references (showing icon in contents)
			if (subNi.isIconOfParent()) continue;
			doExpand(subNi, amount, sofar);
		}
	}

	private static void doUnExpand(NodeInst ni)
	{
		if (!ni.isExpanded()) return;

		NodeProto np = ni.getProto();
		if (!(np instanceof Cell)) return;
		Cell cell = (Cell)np;
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst subNi = (NodeInst)it.next();
			NodeProto subNp = subNi.getProto();
			if (!(subNp instanceof Cell)) continue;

			// ignore recursive references (showing icon in contents)
			if (subNi.isIconOfParent()) continue;
			if (subNi.isExpanded()) doUnExpand(subNi);
		}

		// expanded the cell
		if (expandFlagBit.contains(ni))
		{
			ni.clearExpanded();
		}
	}

	private static int setUnExpand(NodeInst ni, int amount)
	{
		expandFlagBit.remove(ni);
		if (!ni.isExpanded()) return(0);
		NodeProto np = ni.getProto();
		int depth = 0;
		if (np instanceof Cell)
		{
			Cell cell = (Cell)np;
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst subNi = (NodeInst)it.next();
				NodeProto subNp = subNi.getProto();
				if (!(subNp instanceof Cell)) continue;

				// ignore recursive references (showing icon in contents)
				if (subNi.isIconOfParent()) continue;
				if (subNi.isExpanded())
					depth = Math.max(depth, setUnExpand(subNi, amount));
			}
			if (depth < amount) expandFlagBit.add(ni);
		}
		return depth+1;
	}

	/****************************** NODE AND ARC REPLACEMENT ******************************/

	private static class PossibleVariables
	{
        private static HashMap posVarsMap = new HashMap();

        private static void add(String varName, PrimitiveNode pn) {
            List varKeys = (List)posVarsMap.get(pn);
            if (varKeys == null) {
                varKeys = new ArrayList();
                posVarsMap.put(pn, varKeys);
            }
            Variable.Key key = ElectricObject.newKey(varName);
            if (!varKeys.contains(key)) varKeys.add(key);
        }

		static
		{
			add("ATTR_length",       Schematics.tech.transistorNode);
			add("ATTR_length",       Schematics.tech.transistor4Node);
			add("ATTR_width",        Schematics.tech.transistorNode);
			add("ATTR_width",        Schematics.tech.transistor4Node);
			add("ATTR_area",         Schematics.tech.transistorNode);
			add("ATTR_area",         Schematics.tech.transistor4Node);
			add("SIM_spice_model",   Schematics.tech.sourceNode);
			add("SIM_spice_model",   Schematics.tech.transistorNode);
			add("SIM_spice_model",   Schematics.tech.transistor4Node);
			add("SCHEM_meter_type",  Schematics.tech.meterNode);
			add("SCHEM_diode",       Schematics.tech.diodeNode);
			add("SCHEM_capacitance", Schematics.tech.capacitorNode);
			add("SCHEM_resistance",  Schematics.tech.resistorNode);
			add("SCHEM_inductance",  Schematics.tech.inductorNode);
			add("SCHEM_function",    Schematics.tech.bboxNode);
		}

        /**
         * Get an iterator over valid Variable Keys for the primitive node
         * @param pn
         * @return
         */
        public Iterator getPossibleVarKeys(PrimitiveNode pn) {
            List varKeys = (List)posVarsMap.get(pn);
            if (varKeys == null)
                varKeys = new ArrayList();
            return varKeys.iterator();
        }

        /**
         * Returns True if a valid key for the primitive node
         * @param pn
         * @return
         */
        public static boolean validKey(Variable.Key key, PrimitiveNode pn) {
            List varKeys = (List)posVarsMap.get(pn);
            if (varKeys == null) return false;
            return varKeys.contains(key);
        }
	}

	/**
	 * Method to replace node "oldNi" with a new one of type "newNp"
	 * and return the new node.  Also removes any node-specific variables.
	 */
	public static NodeInst replaceNodeInst(NodeInst oldNi, NodeProto newNp, boolean ignorePortNames,
		boolean allowMissingPorts)
	{
		// replace the node
		NodeInst newNi = oldNi.replace(newNp, ignorePortNames, allowMissingPorts);
		if (newNi != null)
		{
			if (newNp instanceof PrimitiveNode)
			{
				// remove variables that make no sense
                for (Iterator it = newNi.getVariables(); it.hasNext(); ) {
                    Variable var = (Variable)it.next();
                    if (!PossibleVariables.validKey(var.getKey(), (PrimitiveNode)newNp)) {
                        newNi.delVar(var.getKey());
                    }
                }
			} else
			{
				// remove parameters that don't exist on the new object d
/*				Cell newCell = (Cell)newNp;
				List varList = new ArrayList();
				for(Iterator it = newNi.getVariables(); it.hasNext(); )
					varList.add(it.next());
				for(Iterator it = varList.iterator(); it.hasNext(); )
				{
					Variable var = (Variable)it.next();
					if (!var.getTextDescriptor().isParam()) continue;

					// see if this parameter exists on the new prototype
					Cell cNp = newCell.contentsView();
					if (cNp == null) cNp = newCell;
					for(Iterator cIt = cNp.getVariables(); it.hasNext(); )
					{
						Variable cVar = (Variable)cIt.next();
						if (!(var.getKey().equals(cVar.getKey()))) continue;
						if (cVar.getTextDescriptor().isParam())
						{
							newNi.delVar(var.getKey());
							break;
						}
					}
				}*/
			}

			// now inherit parameters that now do exist
			inheritAttributes(newNi, true);

			// remove node name if it is not visible
			//Variable var = newNi.getVar(NodeInst.NODE_NAME, String.class);
			//if (var != null && !var.isDisplay())
			//	newNi.delVar(NodeInst.NODE_NAME);
		}
		return newNi;
	}

	/****************************** INHERIT ATTRIBUTES ******************************/

	/**
	 * Method to inherit all prototype attributes down to instance "ni".
	 */
	public static void inheritAttributes(NodeInst ni, boolean cleanUp)
	{
		// ignore primitives
		NodeProto np = ni.getProto();
		if (np instanceof PrimitiveNode) return;
		Cell cell = (Cell)np;

		// first inherit directly from this node's prototype
		for(Iterator it = cell.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (!var.getTextDescriptor().isInherit()) continue;
			inheritCellAttribute(var, ni, cell, null);
		}

		// inherit directly from each port's prototype
		for(Iterator it = cell.getPorts(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			inheritExportAttributes(pp, ni, cell);
		}

		// if this node is an icon, also inherit from the contents prototype
		Cell cNp = cell.contentsView();
		if (cNp != null)
		{
			// look for an example of the icon in the contents
			NodeInst icon = null;
			for(Iterator it = cNp.getNodes(); it.hasNext(); )
			{
				icon = (NodeInst)it.next();
				if (icon.getProto() == cell) break;
				icon = null;
			}
            // if icon is this, ignore it: can't inherit from ourselves!
            if (icon == ni) icon = null;

			for(Iterator it = cNp.getVariables(); it.hasNext(); )
			{
				Variable var = (Variable)it.next();
				if (!var.getTextDescriptor().isInherit()) continue;
				inheritCellAttribute(var, ni, cNp, icon);
			}
			for(Iterator it = cNp.getPorts(); it.hasNext(); )
			{
				Export cpp = (Export)it.next();
				inheritExportAttributes(cpp, ni, cNp);
			}
		}

		// now delete parameters that are not in the prototype
		if (cleanUp)
		{
			if (cNp == null) cNp = cell;
			boolean found = true;
			while (found)
			{
				found = false;
                // look through all parameters on instance
				for(Iterator it = ni.getVariables(); it.hasNext(); )
				{
					Variable var = (Variable)it.next();
					if (!var.getTextDescriptor().isParam()) continue;
					Variable oVar = null;
                    // try to find equivalent in all parameters on prototype
                    Iterator oIt = cNp.getVariables();
                    boolean delete = true;
					while (oIt.hasNext())
					{
						oVar = (Variable)oIt.next();
						if (!oVar.getTextDescriptor().isParam()) continue;
						if (oVar.getKey().equals(var.getKey())) { delete = false; break; }
					}
					if (delete)
					{
                        // no matching parameter on prototype found, so delete
						ni.delVar(var.getKey());
						found = true;
						break;
					}
				}
			}
		}
	}

	/**
	 * Method to add all inheritable export variables from export "pp" on cell "np"
	 * to instance "ni".
	 */
	private static void inheritExportAttributes(Export pp, NodeInst ni, Cell np)
	{
		for(Iterator it = pp.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (!var.getTextDescriptor().isInherit()) continue;

			Variable.Key attrKey = var.getKey();

			// see if the attribute is already there
			PortInst pi = ni.findPortInstFromProto(pp);
			Variable newVar = pi.getVar(attrKey);
			if (newVar != null) continue;

			// set the attribute
			newVar = pi.newVar(attrKey, inheritAddress(pp, var));
			if (newVar != null)
			{
				double lambda = 1;
				TextDescriptor descript = new TextDescriptor(null);
				var.setTextDescriptor(descript);
				double dX = descript.getXOff();
				double dY = descript.getYOff();

//				saverot = pp->subnodeinst->rotation;
//				savetrn = pp->subnodeinst->transpose;
//				pp->subnodeinst->rotation = pp->subnodeinst->transpose = 0;
//				portposition(pp->subnodeinst, pp->subportproto, &x, &y);
//				pp->subnodeinst->rotation = saverot;
//				pp->subnodeinst->transpose = savetrn;
//				x += dX;   y += dY;
//				makerot(pp->subnodeinst, trans);
//				xform(x, y, &x, &y, trans);
//				maketrans(ni, trans);
//				xform(x, y, &x, &y, trans);
//				makerot(ni, trans);
//				xform(x, y, &x, &y, trans);
//				x = x - (ni->lowx + ni->highx) / 2;
//				y = y - (ni->lowy + ni->highy) / 2;
//				switch (TDGETPOS(descript))
//				{
//					case VTPOSCENT:      style = TEXTCENT;      break;
//					case VTPOSBOXED:     style = TEXTBOX;       break;
//					case VTPOSUP:        style = TEXTBOT;       break;
//					case VTPOSDOWN:      style = TEXTTOP;       break;
//					case VTPOSLEFT:      style = TEXTRIGHT;     break;
//					case VTPOSRIGHT:     style = TEXTLEFT;      break;
//					case VTPOSUPLEFT:    style = TEXTBOTRIGHT;  break;
//					case VTPOSUPRIGHT:   style = TEXTBOTLEFT;   break;
//					case VTPOSDOWNLEFT:  style = TEXTTOPRIGHT;  break;
//					case VTPOSDOWNRIGHT: style = TEXTTOPLEFT;   break;
//				}
//				makerot(pp->subnodeinst, trans);
//				style = rotatelabel(style, TDGETROTATION(descript), trans);
//				switch (style)
//				{
//					case TEXTCENT:     TDSETPOS(descript, VTPOSCENT);      break;
//					case TEXTBOX:      TDSETPOS(descript, VTPOSBOXED);     break;
//					case TEXTBOT:      TDSETPOS(descript, VTPOSUP);        break;
//					case TEXTTOP:      TDSETPOS(descript, VTPOSDOWN);      break;
//					case TEXTRIGHT:    TDSETPOS(descript, VTPOSLEFT);      break;
//					case TEXTLEFT:     TDSETPOS(descript, VTPOSRIGHT);     break;
//					case TEXTBOTRIGHT: TDSETPOS(descript, VTPOSUPLEFT);    break;
//					case TEXTBOTLEFT:  TDSETPOS(descript, VTPOSUPRIGHT);   break;
//					case TEXTTOPRIGHT: TDSETPOS(descript, VTPOSDOWNLEFT);  break;
//					case TEXTTOPLEFT:  TDSETPOS(descript, VTPOSDOWNRIGHT); break;
//				}
//				x = x * 4 / lambda;
//				y = y * 4 / lambda;
//				TDSETOFF(descript, x, y);
//				TDSETINHERIT(descript, 0);
//				TDCOPY(newVar->textdescript, descript);
			}
		}
	}

	/*
	 * Method to add inheritable variable "var" from cell "np" to instance "ni".
	 * If "icon" is not NONODEINST, use the position of the variable from it.
	 */
	private static void inheritCellAttribute(Variable var, NodeInst ni, Cell np, NodeInst icon)
	{
		// see if the attribute is already there
		Variable.Key key = var.getKey();
		Variable newVar = ni.getVar(key.getName());
		if (newVar != null)
		{
			// make sure visibility is OK
			if (!var.getTextDescriptor().isInterior())
			{
				// parameter should be visible: make it so
				if (!newVar.isDisplay())
				{
					newVar.setDisplay(true);
				}
			} else
			{
				// parameter not normally visible: make it invisible if it has the default value
				if (newVar.isDisplay())
				{
					if (var.describe(-1).equals(newVar.describe(-1)))
					{
						newVar.setDisplay(false);
					}
				}
			}
		} else {
            newVar = ni.updateVar(var.getKey(), inheritAddress(np, var));
            updateInheritedVar(newVar, ni, np, icon);
        }
    }


    public static void updateInheritedVar(Variable nivar, NodeInst ni, Cell np, NodeInst icon) {

        if (nivar == null) return;

        // determine offset of the attribute on the instance
        Variable posVar = np.getVar(nivar.getKey().getName());
        Variable var = posVar;
        if (icon != null)
        {
            for(Iterator it = icon.getVariables(); it.hasNext(); )
            {
                Variable ivar = (Variable)it.next();
                if (ivar.getKey().equals(nivar.getKey()))
                {
                    posVar = ivar;
                    break;
                }
            }
        }

		double xc = posVar.getTextDescriptor().getXOff();
		if (posVar == var) xc -= np.getBounds().getCenterX();
		double yc = posVar.getTextDescriptor().getYOff();
		if (posVar == var) yc -= np.getBounds().getCenterY();

        TextDescriptor oldDescript = posVar.getTextDescriptor();
        TextDescriptor newDescript = nivar.getTextDescriptor();
//        if (oldDescript.isInterior())
//        {
//            nivar.clearDisplay();
//        } else
//        {
//            nivar.setDisplay();
//        }
        nivar.setDisplay(posVar.isDisplay());
        newDescript.setInherit(false);
        newDescript.setOff(xc, yc);
        newDescript.setParam(oldDescript.isParam());
        if (oldDescript.isParam())
        {
            newDescript.setInterior(false);
            newDescript.setDispPart(oldDescript.getDispPart());
            newDescript.setPos(oldDescript.getPos());
            newDescript.setRotation(oldDescript.getRotation());
            TextDescriptor.Size s = oldDescript.getSize();
            newDescript.setRelSize(s.getSize());
            newDescript.setBold(oldDescript.isBold());
            newDescript.setItalic(oldDescript.isItalic());
            newDescript.setUnderline(oldDescript.isUnderline());
            newDescript.setFace(oldDescript.getFace());

            TextDescriptor.DispPos i = newDescript.getDispPart();
            newDescript.setDispPart(i);
            //if (i == TextDescriptor.DispPos.NAMEVALINH || i == TextDescriptor.DispPos.NAMEVALINHALL)
            //    newDescript.setDispPart(TextDescriptor.DispPos.NAMEVALUE);
        }
        nivar.setCode(posVar.getCode());
	}

	/**
	 * Helper method to determine the proper value of an inherited Variable.
	 * Normally, it is simply "var.getObject()", but if it is a string with the "++" or "--"
	 * sequence in it, then it indicates an auto-increments/decrements of that numeric value.
	 * The returned object has the "++"/"--" removed, and the original variable is modified.
	 * @param addr the ElectricObject on which this Variable resides.
	 * @param var the Variable being examined.
	 * @return the Object in the Variable.
	 */
	private static Object inheritAddress(ElectricObject addr, Variable var)
	{
		// if it isn't a string, just return its address
		Object obj = var.getObject();
		if (obj instanceof Object[]) return obj;
		if (!var.isCode() && !(obj instanceof String)) return obj;

		String str = (String)obj;
		int plusPlusPos = str.indexOf("++");
		int minusMinusPos = str.indexOf("--");
		if (plusPlusPos < 0 && minusMinusPos < 0) return obj;

		// construct the proper inherited string and increment the variable
		int incrPoint = Math.max(plusPlusPos, minusMinusPos);
		String retVal = str.substring(0, incrPoint) + str.substring(incrPoint+2);

		// increment the variable
		int i;
		for(i = incrPoint-1; i>0; i--)
			if (!TextUtils.isDigit(str.charAt(i))) break;
		i++;
		int curVal = TextUtils.atoi(str.substring(i));
		if (str.charAt(incrPoint) == '+') curVal++; else curVal--;
		String newIncrString = str.substring(0, i) + curVal + str.substring(incrPoint+2);
		addr.newVar(var.getKey(), newIncrString);

		return retVal;
	}

	/****************************** LIBRARY CHANGES ******************************/

	/**
	 * Method to implement the "Change Current Library" command.
	 * Prompts the user for a new "current library".
	 */
	public static void changeCurrentLibraryCommand()
	{
		ChangeCurrentLib.showDialog();
	}

	/**
	 * Method to implement the "List Libraries" command.
	 */
	public static void listLibrariesCommand()
	{
		System.out.println("----- Libraries: -----");
		int k = 0;
		for(Iterator it = Library.getVisibleLibrariesSortedByName().iterator(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			if (lib.isHidden()) continue;
			StringBuffer infstr = new StringBuffer();
			infstr.append(lib.getName());
			if (lib.isChangedMajor() || lib.isChangedMinor())
			{
				infstr.append("*");
				k++;
			}
			if (lib.getLibFile() != null)
				infstr.append(" (disk file: " + lib.getLibFile() + ")");
			System.out.println(infstr.toString());

			// see if there are dependencies
			Set dummyLibs = new HashSet();
			HashSet markedLibs = new HashSet();
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				for(Iterator nIt = cell.getNodes(); nIt.hasNext(); )
				{
					NodeInst ni = (NodeInst)nIt.next();
					if (!(ni.getProto() instanceof Cell)) continue;
					Cell subCell = (Cell)ni.getProto();
					Variable var = subCell.getVar(Input.IO_TRUE_LIBRARY, String.class);
					if (var != null)
					{
						String pt = (String)var.getObject();
						dummyLibs.add(pt);
					}
					markedLibs.add(subCell.getLibrary());
				}
			}
			for(Iterator lIt = Library.getLibraries(); lIt.hasNext(); )
			{
				Library oLib = (Library)lIt.next();
				if (oLib == lib) continue;
				if (!markedLibs.contains(oLib)) continue;
				System.out.println("   Makes use of cells in library " + oLib.getName());
				infstr = new StringBuffer();
				infstr.append("      These cells make reference to that library:");
				for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = (Cell)cIt.next();
					boolean found = false;
					for(Iterator nIt = cell.getNodes(); nIt.hasNext(); )
					{
						NodeInst ni = (NodeInst)nIt.next();
						if (!(ni.getProto() instanceof Cell)) continue;
						Cell subCell = (Cell)ni.getProto();
						if (subCell.getLibrary() == oLib) { found = true;   break; }
					}
					if (found) infstr.append(" " + cell.noLibDescribe());
				}
				System.out.println(infstr.toString());
			}
			for(Iterator dIt = dummyLibs.iterator(); dIt.hasNext(); )
			{
				String dummyLibName = (String)dIt.next();
				System.out.println("   Has dummy cells that should be in library " + dummyLibName);
				infstr = new StringBuffer();
				infstr.append("      Instances of these dummy cells are in:");
				for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = (Cell)cIt.next();
					boolean found = false;
					for(Iterator nIt = cell.getNodes(); nIt.hasNext(); )
					{
						NodeInst ni = (NodeInst)nIt.next();
						if (!(ni.getProto() instanceof Cell)) continue;
						Cell subCell = (Cell)ni.getProto();
						Variable var = subCell.getVar(Input.IO_TRUE_LIBRARY, String.class);
						if (var == null) continue;
						if (((String)var.getObject()).equals(dummyLibName)) { found = true;   break; }
					}
					if (found) infstr.append(" " + cell.noLibDescribe());
				}
				System.out.println(infstr.toString());
			}
		}
		if (k != 0) System.out.println("   (* means library has changed)");
	}

	/**
	 * Method to implement the "Rename Library" command.
	 */
	public static void renameLibrary(Library lib)
	{
		String val = JOptionPane.showInputDialog("New Name of Library:", lib.getName());
		if (val == null) return;
		RenameLibrary job = new RenameLibrary(lib, val);
	}

	/**
	 * This class implement the command to make a new version of a cell.
	 */
	private static class RenameLibrary extends Job
	{
		Library lib;
		String newName;

		protected RenameLibrary(Library lib, String newName)
		{
			super("Renaming library " + lib.getName(), User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.lib = lib;
			this.newName = newName;
			startJob();
		}

		public boolean doIt()
		{
			String oldName = lib.getName();
			if (lib.setName(newName)) return false;
			System.out.println("Library " + oldName + " renamed to " + newName);

            // mark this library for saving
            lib.setChangedMajor();

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
			return true;
		}
	}

	/**
	 * Method to implement the "Mark All Libraries for Saving" command.
	 */
	public static void markAllLibrariesForSavingCommand()
	{
		// mark all libraries as "changed"
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			if (lib.isHidden()) continue;

			// make sure all old format library extensions are converted
			String ext = TextUtils.getExtension(lib.getLibFile());
            // I don't think this should change the file format: only "save as" should
            // JKG 29 Oct 2004
            /*
			if (OpenFile.Type.DEFAULTLIB == OpenFile.Type.JELIB)
			{
				if (ext.equals("elib"))
				{
					String fullName = lib.getLibFile().getFile();
					int len = fullName.length();
					fullName = fullName.substring(0, len-4) + "jelib";
					lib.setLibFile(TextUtils.makeURLToFile(fullName));
				}
			}*/

			// do not mark readable dump files for saving
			if (ext.equals("txt")) continue;

			lib.setChangedMajor();
			lib.setChangedMinor();
		}
		System.out.println("All libraries now need to be saved");
	}

	/**
	 * Method to implement the "Repair Librariesy" command.
	 */
	public static void checkAndRepairCommand(boolean repair)
	{
		new CheckAndRepairJob(repair);
	}

	/**
	 * This class implement the command to repair libraries.
	 */
	private static class CheckAndRepairJob extends Job
	{
		boolean repair;

		protected CheckAndRepairJob(boolean repair)
		{
			super((repair ? "Repair Libraries" : "Check Libraries"), User.tool, (repair ? Job.Type.CHANGE : Job.Type.EXAMINE), null, null, Job.Priority.USER);
			this.repair = repair;
			startJob();
		}

		public boolean doIt()
		{
			ErrorLogger errorLogger = ErrorLogger.newInstance(repair ? "Repair Libraries" : "Check Libraries");
			int errorCount = 0;
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				errorCount += lib.checkAndRepair(repair, errorLogger);
			}
			if (errorCount > 0) System.out.println("Found " + errorCount + " errors"); else
				System.out.println("No errors found");
			errorLogger.termLogging(true);
			return true;
		}
	}

	/****************************** DETERMINE ABILITY TO MAKE CHANGES ******************************/

	/**
	 * Method to tell whether a NodeInst can be modified in a cell.
	 * @param cell the Cell in which the NodeInst resides.
	 * @param item the NodeInst (may be null).
	 * @param giveError true to print an error message if the modification is disallowed.
	 * @return positive if the edit CANNOT be done.
	 * Return negative if the edit CANNOT be done and the overall operation should be cancelled.
	 * Return zero if the operation CAN be done.
	 */
	public static int cantEdit(Cell cell, NodeInst item, boolean giveError)
	{
		String [] options = {"Yes", "No", "Always", "Cancel"};
		// if an instance is specified, check it
		if (item != null)
		{
			if (item.isLocked())
			{
				if (!giveError) return 1;
				int ret = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
					"Changes to locked node " + item.describe() + " are disallowed.  Change anyway?",
					"Allow changes", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
					null, options, options[1]);
				if (ret == 1) return 1;
				if (ret == 2) item.clearLocked();
				if (ret == 3) return -1;
			}
			if (item.getProto() instanceof PrimitiveNode)
			{
				// see if a primitive is locked
				if (((PrimitiveNode)item.getProto()).isLockedPrim() &&
					User.isDisallowModificationLockedPrims())
				{
					if (!giveError) return 1;
					int ret = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
						"Changes to locked primitives (such as " + item.describe() + ") are disallowed.  Change anyway?",
						"Allow changes", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
						null, options, options[1]);
					if (ret == 1) return 1;
					if (ret == 2) User.setDisallowModificationLockedPrims(false);
					if (ret == 3) return -1;
				}
			} else
			{
				// see if this type of cell is locked
				if (cell.isInstancesLocked())
				{
					if (!giveError) return 1;
					int ret = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
						"Modification of instances in cell " + cell.describe() + " is disallowed.  You cannot move " + item.describe() +
						".  Change anyway?",
						"Allow changes", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
						null, options, options[1]);
					if (ret == 1) return 1;
					if (ret == 2) cell.clearInstancesLocked();
					if (ret == 3) return -1;
				}
			}
		}

		// check for general changes to the cell
		if (cell.isAllLocked())
		{
			if (!giveError) return 1;
			int ret = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
				"Modification of cell " + cell.describe() + " is disallowed.  Change "+((item == null)? "" : item.describe())+" anyway?",
				"Allow changes", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
				null, options, options[1]);
			if (ret == 1) return 1;
			if (ret == 2) cell.clearAllLocked();
			if (ret == 3) return -1;
		}
		return 0;
	}

}
