/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Undo.java
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
package com.sun.electric.database.change;

import com.sun.electric.database.constraint.Constraint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.tool.Tool;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * This interface defines changes that are made to the database.
 */
public class Undo
{
	/**
	 * Type is a typesafe enum class that describes the nature of a change.
	 */
	public static class Type
	{
		private final String name;

		private Type(String name) { this.name = name; }

		/**
		 * Returns a printable version of this Type.
		 * @return a printable version of this Type.
		 */
		public String toString() { return name; }

		/** Describes a newly-created NodeInst. */							public static final Type NODEINSTNEW = new Type("NodeInstNew");
		/** Describes a deleted NodeInst. */								public static final Type NODEINSTKILL = new Type("NodeInstKill");
		/** Describes a changed NodeInst. */								public static final Type NODEINSTMOD = new Type("NodeInstMod");
		/** Describes a newly-created ArcInst. */							public static final Type ARCINSTNEW = new Type("ArcInstNew");
		/** Describes a deleted ArcInst. */									public static final Type ARCINSTKILL = new Type("ArcInstKill");
		/** Describes a changed ArcInst. */									public static final Type ARCINSTMOD = new Type("ArcInstMod");
		/** Describes a newly-created Export. */							public static final Type EXPORTNEW = new Type("ExportNew");
		/** Describes a deleted Export. */									public static final Type EXPORTKILL = new Type("ExportKill");
		/** Describes a changed Export. */									public static final Type EXPORTMOD = new Type("ExportMod");
		/** Describes a newly-created Cell. */								public static final Type CELLNEW = new Type("CellNew");
		/** Describes a deleted Cell. */									public static final Type CELLKILL = new Type("CellKill");
		/** Describes a changed Cell. */									public static final Type CELLMOD = new Type("CellMod");
		/** Describes the creation of an arbitrary object. */				public static final Type OBJECTNEW = new Type("ObjectNew");
		/** Describes the deletion of an arbitrary object. */				public static final Type OBJECTKILL = new Type("ObjectKill");
		/** Describes the redrawing of an arbitrary object. */				public static final Type OBJECTREDRAW = new Type("ObjectRedraw");
		/** Describes the creation of a Variable on an object. */			public static final Type VARIABLENEW = new Type("VariableNew");
		/** Describes the deletion of a Variable on an object. */			public static final Type VARIABLEKILL = new Type("VariableKill");
		/** Describes the modification of a Variable on an object. */		public static final Type VARIABLEMODFLAGS = new Type("VariableModFlags");
		/** Describes the modification of a Variable on an object. */		public static final Type VARIABLEMOD = new Type("VariableMod");
		/** Describes the insertion of an entry in an arrayed Variable. */	public static final Type VARIABLEINSERT = new Type("VariableInsert");
		/** Describes the deletion of an entry in an arrayed Variable. */	public static final Type VARIABLEDELETE = new Type("VariableDelete");
		/** Describes the change to a TextDescriptor. */					public static final Type DESCRIPTORMOD = new Type("DescriptMod");
	}

	/**
	 * Change describes a single change.
	 */
	public static class Change
	{
		private ElectricObject obj;
		private Type type;
		private double a1, a2, a3, a4, a5;
		private int i1, i2;
		private Object o1, o2;

		Change(ElectricObject obj, Type type)
		{
			this.obj = obj;
			this.type = type;
		}
		private void setDoubles(double a1, double a2, double a3, double a4)
		{
			this.a1 = a1;
			this.a2 = a2;
			this.a3 = a3;
			this.a4 = a4;
		}
		
		public ElectricObject getObject() { return obj; }
		public Type getType() { return type; }
		private void setType(Type change) { this.type = type; }
		public double getA1() { return a1; }
		public double getA2() { return a2; }
		public double getA3() { return a3; }
		public double getA4() { return a4; }
		public double getA5() { return a5; }
		public int getI1() { return i1; }
		public int getI2() { return i2; }
		public Object getO1() { return o1; }
		public Object getO2() { return o2; }

		/**
		 * Routine to broadcast a change to all tools that are on.
		 * @param firstchange true if this is the first change of a batch, so that a "startbatch" change must also be broadcast.
		 * @param undoredo true if this is an undo/redo batch.
		 */
		void broadcast(boolean firstchange, boolean undoRedo)
		{
			// start the batch if this is the first change
			broadcasting = type;
			if (firstchange)
			{
				// broadcast a start-batch on the first change
				for(Iterator it = Tool.getTools(); it.hasNext(); )
				{
					Tool tool = (Tool)it.next();
					if (tool.isOn()) tool.startBatch(tool, undoRedo);
				}
			}
			if (type == Type.NODEINSTNEW || type == Type.ARCINSTNEW || type == Type.EXPORTNEW ||
				type == Type.CELLNEW || type == Type.OBJECTNEW)
			{
				for(Iterator it = Tool.getTools(); it.hasNext(); )
				{
					Tool tool = (Tool)it.next();
					if (tool.isOn()) tool.newObject(obj);
				}
			} else if (type == Type.NODEINSTKILL || type == Type.ARCINSTKILL || type == Type.EXPORTKILL ||
				type == Type.CELLKILL || type == Type.OBJECTKILL)
			{
				for(Iterator it = Tool.getTools(); it.hasNext(); )
				{
					Tool tool = (Tool)it.next();
					if (tool.isOn()) tool.killObject(obj);
				}
			} else if (type == Type.OBJECTREDRAW)
			{
				for(Iterator it = Tool.getTools(); it.hasNext(); )
				{
					Tool tool = (Tool)it.next();
					if (tool.isOn()) tool.redrawObject(obj);
				}
			} else if (type == Type.NODEINSTMOD)
			{
				for(Iterator it = Tool.getTools(); it.hasNext(); )
				{
					Tool tool = (Tool)it.next();
					if (tool.isOn()) tool.modifyNodeInst((NodeInst)obj, a1, a2, a3, a4, i1);
				}
			} else if (type == Type.ARCINSTMOD)
			{
				for(Iterator it = Tool.getTools(); it.hasNext(); )
				{
					Tool tool = (Tool)it.next();
					if (tool.isOn()) tool.modifyArcInst((ArcInst)obj, a1, a2, a3, a4, a5);
				}
			} else if (type == Type.EXPORTMOD)
			{
				for(Iterator it = Tool.getTools(); it.hasNext(); )
				{
					Tool tool = (Tool)it.next();
					if (tool.isOn()) tool.modifyExport((Export)obj, (PortInst)o1);
				}
			} else if (type == Type.CELLMOD)
			{
				for(Iterator it = Tool.getTools(); it.hasNext(); )
				{
					Tool tool = (Tool)it.next();
					if (tool.isOn()) tool.modifyCell((Cell)obj, a1, a2, a3, a4);
				}
			} else if (type == Type.VARIABLENEW)
			{
				for(Iterator it = Tool.getTools(); it.hasNext(); )
				{
					Tool tool = (Tool)it.next();
					if (tool.isOn()) tool.newVariable(obj, (Variable)o1);
				}
			} else if (type == Type.VARIABLEKILL)
			{
				for(Iterator it = Tool.getTools(); it.hasNext(); )
				{
					Tool tool = (Tool)it.next();
					if (tool.isOn()) tool.killVariable(obj, (Variable)o1);
				}
			} else if (type == Type.VARIABLEMODFLAGS)
			{
				for(Iterator it = Tool.getTools(); it.hasNext(); )
				{
					Tool tool = (Tool)it.next();
					if (tool.isOn()) tool.modifyVariableFlags(obj, (Variable)o1, i1);
				}
			} else if (type == Type.VARIABLEMOD)
			{
				for(Iterator it = Tool.getTools(); it.hasNext(); )
				{
					Tool tool = (Tool)it.next();
					if (tool.isOn()) tool.modifyVariable(obj, (Variable)o1, i1, o2);
				}
			} else if (type == Type.VARIABLEINSERT)
			{
				for(Iterator it = Tool.getTools(); it.hasNext(); )
				{
					Tool tool = (Tool)it.next();
					if (tool.isOn()) tool.insertVariable(obj, (Variable)o1, i1);
				}
			} else if (type == Type.VARIABLEDELETE)
			{
				for(Iterator it = Tool.getTools(); it.hasNext(); )
				{
					Tool tool = (Tool)it.next();
					if (tool.isOn()) tool.deleteVariable(obj, (Variable)o1, i1, o2);
				}
			} else if (type == Type.DESCRIPTORMOD)
			{
				for(Iterator it = Tool.getTools(); it.hasNext(); )
				{
					Tool tool = (Tool)it.next();
					if (tool.isOn()) tool.modifyTextDescript(obj, (TextDescriptor)o1, i1, i2);
				}
			}
			broadcasting = null;
		}

		/**
		 * Routine to undo the effects of this change.
		 */
		void reverse()
		{
			// determine what needs to be marked as changed
			setDirty(type, obj, o1);

			if (type == Type.NODEINSTNEW)
			{
				NodeInst ni = (NodeInst)obj;
				ni.lowLevelUnlink();
				type = Type.NODEINSTKILL;
				return;
			}
			if (type == Type.NODEINSTKILL)
			{
				NodeInst ni = (NodeInst)obj;
				ni.lowLevelLink();
				type = Type.NODEINSTNEW;
				return;
			}
			if (type == Type.NODEINSTMOD)
			{
				// get information about the node as it is now
				NodeInst ni = (NodeInst)obj;
				double oldCX = ni.getGrabCenterX();
				double oldCY = ni.getGrabCenterY();
				double oldSX = ni.getXSize();
				double oldSY = ni.getYSize();
				int oldRot = ni.getAngle();

				// change the node information
				ni.lowLevelModify(a1 - oldCX, a2 - oldCY, a3 - oldSX, a4 - oldSY, i1 - oldRot);

				// update the change to its reversed state
				a1 = oldCX;   a2 = oldCY;
				a3 = oldSX;   a4 = oldSY;
				i1 = oldRot;
				return;
			}
			if (type == Type.ARCINSTNEW)
			{
				ArcInst ai = (ArcInst)obj;
				ai.lowLevelUnlink();
				type = Type.ARCINSTKILL;
				return;
			}
			if (type == Type.ARCINSTKILL)
			{
				ArcInst ai = (ArcInst)obj;
				ai.lowLevelLink();
				type = Type.ARCINSTNEW;
				return;
			}
			if (type == Type.ARCINSTMOD)
			{
				// get information about the arc as it is now
				ArcInst ai = (ArcInst)obj;
				Point2D oldHeadPt = new Point2D.Double();
				oldHeadPt.setLocation(ai.getHead().getLocation());
				Point2D oldTailPt = new Point2D.Double();
				oldTailPt.setLocation(ai.getTail().getLocation());
				double oldWid = ai.getWidth();

				// change the arc information
				ai.lowLevelModify(a5 - oldWid, a1-oldHeadPt.getX(), a2-oldHeadPt.getY(), a3-oldTailPt.getX(), a4-oldTailPt.getY());

				// update the change to its reversed state
				a1 = oldHeadPt.getX();   a2 = oldHeadPt.getY();
				a3 = oldTailPt.getX();   a4 = oldTailPt.getY();
				a5 = oldWid;
				return;
			}
			if (type == Type.EXPORTNEW)
			{
				Export pp = (Export)obj;
				pp.lowLevelUnlink();
				type = Type.EXPORTKILL;
				return;
			}
			if (type == Type.EXPORTKILL)
			{
				Export pp = (Export)obj;
				pp.lowLevelLink();
				type = Type.EXPORTNEW;
				return;
			}
			if (type == Type.EXPORTMOD)
			{
				Export pp = (Export)obj;
				PortInst oldPi = (PortInst)o1;
				PortInst currentPi = pp.getOriginalPort();
				pp.lowLevelModify(oldPi);
				o1 = currentPi;
				return;
			}
			if (type == Type.CELLNEW)
			{
				Cell cell = (Cell)obj;
				cell.lowLevelUnlink();
				type = Type.CELLKILL;
				return;
			}
			if (type == Type.CELLKILL)
			{
				Cell cell = (Cell)obj;
				cell.lowLevelLink();
				type = Type.CELLNEW;
				return;
			}
			if (type == Type.CELLMOD)
			{
				Cell cell = (Cell)obj;
				Rectangle2D bounds = cell.getBounds();
				a1 = bounds.getMinX();   a2 = bounds.getMaxX();
				a3 = bounds.getMinY();   a4 = bounds.getMaxY();
				return;
			}
			if (type == Type.OBJECTNEW)
			{
				// args: addr, type
//				switch (c->p1&VTYPE)
//				{
//					case VVIEW:
//						// find the view
//						view = (VIEW *)c->entryaddr;
//						lastv = NOVIEW;
//						for(v = el_views; v != NOVIEW; v = v->nextview)
//						{
//							if (v == view) break;
//							lastv = v;
//						}
//						if (v != NOVIEW)
//						{
//							// delete the view
//							if (lastv == NOVIEW) el_views = v->nextview; else
//								lastv->nextview = v->nextview;
//						}
//						break;
//				}
				type = Type.OBJECTKILL;
				return;
			}
			if (type == Type.OBJECTKILL)
			{
				// args: addr, type
//				switch (c->p1&VTYPE)
//				{
//					case VVIEW:
//						view = (VIEW *)c->entryaddr;
//						view->nextview = el_views;
//						el_views = view;
//						break;
//				}
				type = Type.OBJECTNEW;
				return;
			}
			if (type == Type.VARIABLENEW)
			{
				Variable var = (Variable)o1;
				obj.lowLevelUnlinkVar(var);
				type = Type.VARIABLEKILL;
				return;
			}
			if (type == Type.VARIABLEKILL)
			{
				Variable var = (Variable)o1;
				obj.lowLevelLinkVar(var);
				type = Type.VARIABLENEW;
				return;
			}
			if (type == Type.VARIABLEMODFLAGS)
			{
				Variable var = (Variable)o1;
				int oldFlags = var.lowLevelGetFlags();
				var.lowLevelSetFlags(i1);
				i1 = oldFlags;
				return;
			}
			if (type == Type.VARIABLEMOD)
			{
				Variable var = (Variable)o1;
				Object[] arr = (Object[])var.getObject();
				Object oldVal = arr[i1];
				arr[i1] = o2;
				o2 = oldVal;
				return;
			}
			if (type == Type.VARIABLEINSERT)
			{
				Variable var = (Variable)o1;
				Object[] oldArr = (Object[])var.getObject();
				o2 = oldArr[i1];
				var.lowLevelDelete(i1);
				type = Type.VARIABLEDELETE;
				return;
			}
			if (type == Type.VARIABLEDELETE)
			{
				Variable var = (Variable)o1;
				var.lowLevelInsert(i1, o2);
				type = Type.VARIABLEINSERT;
				return;
			}
			if (type == Type.DESCRIPTORMOD)
			{
				TextDescriptor descript = (TextDescriptor)o1;
				int oldDescript0 = descript.lowLevelGet0();
				int oldDescript1 = descript.lowLevelGet1();
				descript.lowLevelSet(i1, i2);
				i1 = oldDescript0;
				i2 = oldDescript1;
				return;
			}
		}

		/**
		 * Routine to examine a change and mark the appropriate libraries and cells as "dirty".
		 * @param change the type of change being made.
		 * @param obj the object to which the change is applied.
		 */
		private static void setDirty(Type type, ElectricObject obj, Object o1)
		{
			Cell cell = null;
			Library lib = null;
			boolean major = false;
			if (type == Type.NODEINSTNEW || type == Type.NODEINSTKILL || type == Type.NODEINSTMOD)
			{
				NodeInst ni = (NodeInst)obj;
				cell = ni.getParent();
				lib = cell.getLibrary();
				major = true;
			} else if (type == Type.ARCINSTNEW || type == Type.ARCINSTKILL || type == Type.ARCINSTMOD)
			{
				ArcInst ai = (ArcInst)obj;
				cell = ai.getParent();
				lib = cell.getLibrary();
				major = true;
			} else if (type == Type.EXPORTNEW || type == Type.EXPORTKILL || type == Type.EXPORTMOD)
			{
				Export pp = (Export)obj;
				cell = (Cell)pp.getParent();
				lib = cell.getLibrary();
				major = true;
			} else if (type == Type.CELLNEW || type == Type.CELLKILL)
			{
				cell = (Cell)obj;
				lib = cell.getLibrary();
				major = true;
			} else if (type == Type.CELLMOD)
			{
				cell = (Cell)obj;
				lib = cell.getLibrary();
			} else if (type == Type.OBJECTNEW || type == Type.OBJECTKILL || type == Type.OBJECTREDRAW)
			{
				cell = obj.whichCell();
				if (cell != null) lib = cell.getLibrary();
			} else if (type == Type.VARIABLENEW || type == Type.VARIABLEKILL || type == Type.VARIABLEMOD ||
				type == Type.VARIABLEINSERT || type == Type.VARIABLEDELETE)
			{
				cell = obj.whichCell();
				if (cell != null) lib = cell.getLibrary();
				Variable var = (Variable)o1;
				major = isMajorVariable(obj, var.getKey());
			} else if (type == Type.VARIABLEMODFLAGS || type == Type.DESCRIPTORMOD)
			{
				cell = obj.whichCell();
				if (cell != null) lib = cell.getLibrary();
			}

			// set "changed" and "dirty" bits
			if (cell != null)
			{
				if (major) cell.madeRevision();
				if (!ChangeCell.contains(cell))
				{
					ChangeCell.add(cell);
				}
			}
			if (lib != null)
			{
				if (major) lib.setChangedMajor(); else
					lib.setChangedMinor();
			}
		}

		private static boolean isMajorVariable(ElectricObject obj, Variable.Key key)
		{
// 			if ((obj instanceof Cell) && key == el_cell_message_key) return true;
// 			if ((obj instanceof NodeInst) && key == sim_weaknodekey) return true;
			return false;
		}

		/**
		 * Routine to describe this change as a string.
		 */
		String describe()
		{
			if (type == Type.NODEINSTNEW)
			{
				NodeInst ni = (NodeInst)obj;
				return "Node " + ni.describe() + " created in cell " + ni.getParent().describe();
			}
			if (type == Type.NODEINSTKILL)
			{
				NodeInst ni = (NodeInst)obj;
				return "Node " + ni.describe() + " deleted from cell " + ni.getParent().describe();
			}
			if (type == Type.NODEINSTMOD)
			{
				NodeInst ni = (NodeInst)obj;
				return "Node " + ni.describe() + " modified in cell " + ni.getParent().describe() +
					"[was " + getA3() + "x" + getA4() + " at (" + getA1() + "," + getA2() + ") rotated " + getI1()/10.0 + "]";
			}
			if (type == Type.ARCINSTNEW)
			{
				ArcInst ai = (ArcInst)obj;
				return "Arc " + ai.describe() + " created in cell " + ai.getParent().describe();
			}
			if (type == Type.ARCINSTKILL)
			{
				ArcInst ai = (ArcInst)obj;
				return "Arc " + ai.describe() + " deleted from cell " + ai.getParent().describe();
			}
			if (type == Type.ARCINSTMOD)
			{
				ArcInst ai = (ArcInst)obj;
				return "Arc " + ai.describe() + " modified in cell " + ai.getParent().describe() +
					"[was " + getA5() + " wide from (" + getA1() + "," + getA2() + ") to (" + getA3() + "," + getA4() + ")]";
			}
			if (type == Type.EXPORTNEW)
			{
				Export pp = (Export)obj;
				return "Export " + pp.getProtoName() + " created in cell " + pp.getParent().describe();
			}
			if (type == Type.EXPORTKILL)
			{
				Export pp = (Export)obj;
				return "Export " + pp.getProtoName() + " deleted from cell " + pp.getParent().describe();
			}
			if (type == Type.EXPORTMOD)
			{
				Export pp = (Export)obj;
				PortInst pi = (PortInst)o1;
				return "Export " + pp.getProtoName() + " moved in cell " + pp.getParent().describe() +
					"[was on node " + pi.getNodeInst().describe() + " port " + pi.getPortProto().getProtoName() + "]";
			}
			if (type == Type.CELLNEW)
			{
				Cell cell = (Cell)obj;
				return "Cell " + cell.describe() + " created";
			}
			if (type == Type.CELLKILL)
			{
				Cell cell = (Cell)obj;
				return "Cell " + cell.describe() + " deleted";
			}
			if (type == Type.CELLMOD)
			{
				Cell cell = (Cell)obj;
				return "Cell " + cell.describe() + " modified [was from " + a1 + "<=X<=" + a2 + " " + a3 + "<=Y<=" + a4 + "]";
			}
			if (type == Type.OBJECTNEW)
			{
				return "Created new object " + obj;
			}
			if (type == Type.OBJECTKILL)
			{
				return "Deleted object " + obj;
			}
			if (type == Type.OBJECTREDRAW)
			{
				return "Redraw object " + obj;
			}
			if (type == Type.VARIABLENEW)
			{
				return "Created variable "+o1+" on "+obj;
			}
			if (type == Type.VARIABLEKILL)
			{
				return "Deleted variable "+o1+" on "+obj+" [was "+((Variable)o1).getObject()+"]";
			}
			if (type == Type.VARIABLEMODFLAGS)
			{
				return "Modified variable flags "+obj+" "+o1+" [was 0"+Integer.toOctalString(i1)+"]";
			}
			if (type == Type.VARIABLEMOD)
			{
				return "Modified variable "+o1+"["+i1+"] on "+obj;
			}
			if (type == Type.VARIABLEINSERT)
			{
				return "Inserted variable "+o1+" on "+obj;
			}
			if (type == Type.VARIABLEDELETE)
			{
				return "Deleted variable "+o1+" on "+obj;
			}
			if (type == Type.DESCRIPTORMOD)
			{
				return "Modified Text Descriptor in "+obj+" [was 0"+Integer.toOctalString(i1)+"/0"+Integer.toOctalString(i2)+"]";
			}
			return "?";
		}
	}

	/**
	 * ChangeBatch describes a batch of changes.
	 */
	public static class ChangeBatch
	{
		private List changes;
		private int batchNumber;
		private boolean done;
		private Tool tool;
		private String activity;
		private Cell upCell;

		ChangeBatch() {}
		
		void add(Change change) { changes.add(change); }
		public Iterator getChanges() { return changes.iterator(); }
		public int getNumChanges() { return changes.size(); }

		void describe()
		{
			/* display the change batches */
			int nodeInst = 0, arcInst = 0, export = 0, cell = 0, object = 0, variable = 0;
			int batchSize = changes.size();
			for(int j = 0; j<batchSize; j++)
			{
				Change ch = (Change)changes.get(j);
				if (ch.getType() == Type.NODEINSTNEW || ch.getType() == Type.NODEINSTKILL || ch.getType() == Type.NODEINSTMOD)
				{
					nodeInst++;
				} else if (ch.getType() == Type.ARCINSTNEW || ch.getType() == Type.ARCINSTKILL || ch.getType() == Type.ARCINSTMOD)
				{
					arcInst++;
				} else if (ch.getType() == Type.EXPORTNEW || ch.getType() == Type.EXPORTKILL || ch.getType() == Type.EXPORTMOD)
				{
					export++;
				} else if (ch.getType() == Type.CELLNEW || ch.getType() == Type.CELLKILL || ch.getType() == Type.CELLMOD)
				{
					cell++;
				} else if (ch.getType() == Type.OBJECTNEW || ch.getType() == Type.OBJECTKILL || ch.getType() == Type.OBJECTREDRAW)
				{
					object++;
				} else if (ch.getType() == Type.VARIABLENEW || ch.getType() == Type.VARIABLEKILL || ch.getType() == Type.VARIABLEMODFLAGS ||
					ch.getType() == Type.VARIABLEMOD || ch.getType() == Type.VARIABLEINSERT ||
					ch.getType() == Type.VARIABLEDELETE)
				{
					variable++;
				} else if (ch.getType() == Type.DESCRIPTORMOD)
				{
					TextDescriptor td = (TextDescriptor)ch.o1;
					if (ch.obj instanceof NodeInst)
					{
						NodeInst ni = (NodeInst)ch.obj;
						if (ni.getNameTextDescriptor() == td || ni.getProtoTextDescriptor() == td)
							nodeInst++;
						else
							variable++;
					} else if (ch.obj instanceof ArcInst)
					{
						ArcInst ai = (ArcInst)ch.obj;
						if (ai.getNameTextDescriptor() == td)
							arcInst++;
						else
							variable++;
					} else if (ch.obj instanceof Export)
					{
						Export e = (Export)ch.obj;
						if (e.getTextDescriptor() == td)
							export++;
						else
							variable++;
					} else
					{
						variable++;
					}
				}
			}

			String message = "*** Batch " + batchNumber + " (" + activity + ") has " + batchSize + " changes and affects";
			if (nodeInst != 0) message += " " + nodeInst + " nodes";
			if (arcInst != 0) message += " " + arcInst + " arcs";
			if (export != 0) message += " " + export + " exports";
			if (cell != 0) message += " " + cell + " cells";
			if (object != 0) message += " " + object + " objects";
			if (variable != 0) message += " " + variable + " variables";
			System.out.println(message + ":");
			for(int j = 0; j<batchSize; j++)
			{
				Change ch = (Change)changes.get(j);
				System.out.println("   " + ch.describe());
			}
		}
	}

	/**
	 * ChangeBatch describes a batch of changes.
	 * There are no public methods or field variables.
	 */
	public static class ChangeCell
	{
		private Cell cell;
		private boolean forcedLook;
		private static List changeCells = new ArrayList();

		private ChangeCell(Cell cell)
		{
			this.cell = cell;
			this.forcedLook = false;
		}

		public Cell getCell() { return cell;}
		public boolean getForcedLook() { return forcedLook;}

		public static void clear()
		{
			changeCells.clear();
		}

		public static ChangeCell add(Cell cell)
		{
			ChangeCell cc = new ChangeCell(cell);
			changeCells.add(cc);
			return cc;
		}

		public static boolean contains(Cell cell)
		{
			for(Iterator it = changeCells.iterator(); it.hasNext(); )
			{
				ChangeCell cc = (ChangeCell)it.next();
				if (cc.cell == cell) return true;
			}
			return false;
		}

		/**
		 * Routine to return a list of cells that have changed in this batch.
		 * @return an Interator over the list of changed cells.
		 */
		public static Iterator getIterator()
		{
			return changeCells.iterator();
		}

		/*
		 * Routine to ensure that cell "np" is given a hierarchical analysis by the
		 * constraint system.
		 */
		public static void forceHierarchicalAnalysis(Cell cell)
		{
			if (currentBatch == null) return;
			for(Iterator it = changeCells.iterator(); it.hasNext(); )
			{
				ChangeCell cc = (ChangeCell)it.next();
				if (cc.cell != cell) continue;
				cc.forcedLook = true;
				return;
			}

			/* if not in the list, create the entry and try again */
			ChangeCell cc = ChangeCell.add(cell);
			cc.forcedLook = true;
		}
	}

	private static Type broadcasting = null;
	private static boolean doNextChangeQuietly = false;
	private static boolean doChangesQuietly = false;
	private static ChangeBatch currentBatch = null;
	private static int maximumBatches = 20;
	private static int overallBatchNumber = 0;
	private static List doneList = new ArrayList();
	private static List undoneList = new ArrayList();

	/**
	 * Routine to start a new batch of changes.
	 * @param tool the tool that is producing the activity.
	 * @param activity a String describing the activity.
	 * @param cell root of up-tree or null for whole database lock
	 */
	public static void startChanges(Tool tool, String activity, Cell cell)
	{
		// close any open batch of changes
		endChanges();

		// kill off any undone batches
		noRedoAllowed();

		// erase the list of changed cells
		ChangeCell.clear();

		// allocate a new change batch
		currentBatch = new ChangeBatch();
		currentBatch.changes = new ArrayList();
		currentBatch.batchNumber = ++overallBatchNumber;
		currentBatch.done = true;
		currentBatch.tool = tool;
		currentBatch.activity = activity;
		currentBatch.upCell = cell;

		// put at head of list
		doneList.add(currentBatch);

		// kill last batch if list is full
		if (doneList.size() > maximumBatches)
		{
			doneList.remove(0);
		}
		// start the batch of changes
		Constraint.getCurrent().startBatch(tool, false);
	}

	/**
	 * Routine to terminate the current batch of changes.
	 */
	public static void endChanges()
	{
		// if no changes were recorded, stop
		if (currentBatch == null) return;

		// changes made: apply final constraints to this batch of changes
		Constraint.getCurrent().endBatch();

		for(Iterator it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = (Tool)it.next();
			if (tool.isOn()) tool.endBatch();
		}

		currentBatch = null;
	}

	/**
	 * Routine to record and broadcast a change.
	 * <P>
	 * These types of changes exist:
	 * <UL>
	 * <LI>NODEINSTNEW takes nothing.
	 * <LI>NODEINSTKILL takes nothing.
	 * <LI>NODEINSTMOD takes a1=oldLX a2=oldHX a3=oldLY a4=oldHY i1=oldRotation.
	 * <LI>ARCINSTNEW takes nothing.
	 * <LI>ARCINSTKILL takes nothing.
	 * <LI>ARCINSTMOD takes a1=oldHeadX a2=oldHeadY a3=oldTailX a4=oldTailY a5=oldWidth.
	 * <LI>EXPORTNEW takes nothing.
	 * <LI>EXPORTKILL takes nothing.
	 * <LI>EXPORTMOD takes o1=oldPortInst.
	 * <LI>CELLNEW takes nothing.
	 * <LI>CELLKILL takes nothing.
	 * <LI>CELLMOD takes a1=oldLowX a2=oldHighX a3=oldLowY a4=oldHighY.
	 * <LI>OBJECTNEW takes nothing.
	 * <LI>OBJECTKILL takes nothing.
	 * <LI>OBJECTREDRAW takes nothing.
	 * <LI>VARIABLENEW takes o1=var.
	 * <LI>VARIABLEKILL takes o1=var.
	 * <LI>VARIABLEMOD takes o1=var i1=index o2=oldValue.
	 * <LI>VARIABLEMODFLAGS takes o1=var i1=flags.
	 * <LI>VARIABLEINSERT takes o1=var i1=index.
	 * <LI>VARIABLEDELETE takes a1=var i1=index o2=oldValue.
	 * <LI>DESCRIPTORMOD takes o1=descript i1=oldDescript0 i2=oldDescript1.
	 * </UL>
	 * @param obj the object to which the change applies.
	 * @param change the change being recorded.
	 * @return the change object (null on error).
	 */
	private static Change newChange(ElectricObject obj, Type change, Object o1)
	{
		if (currentBatch == null)
		{
			System.out.println("Recieved " + change + " change when no batch started");
			return null;
		}
		if (broadcasting != null)
		{
			System.out.println("Recieved " + change + " change during broadcast of " + broadcasting);
			return null;
		}

		// determine what needs to be marked as changed
		Change.setDirty(change, obj, o1);

		// see if this is the first change
		boolean firstChange = false;
		if (currentBatch.getNumChanges() == 0) firstChange = true;

		// get change module
		Change ch = new Change(obj, change);
		ch.o1 = o1;

		// insert new change module into linked list
		currentBatch.add(ch);

		// broadcast the change
		//ch.broadcast(firstChange, false);
		return ch;
	}

	public static void modifyNodeInst(NodeInst ni, double oCX, double oCY, double oSX, double oSY, int oRot)
	{
		if (!recordChange()) return;
		Change ch = newChange(ni, Type.NODEINSTMOD, null);
		ch.setDoubles(oCX, oCY, oSX, oSY);
		ch.i1 = oRot;
		ni.setChange(ch);

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
		//Constraint.getCurrent().modifyArcInst(ni, oCX, oCY, oSX, oSY, oRot);
	}

	public static void modifyArcInst(ArcInst ai, double oHX, double oHY, double oTX, double oTY, double oWid)
	{
		if (!recordChange()) return;
		Change ch = newChange(ai, Type.ARCINSTMOD, null);
		ch.setDoubles(oHX, oHY, oTX, oTY);
		ch.a5 = oWid;
		ai.setChange(ch);

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
		//Constraint.getCurrent().modifyArcInst(ai, oHX, oHY, oTX, oTY, oWid);
	}

	public static void modifyExport(Export pp, PortInst oldPi)
	{
		if (!recordChange()) return;
		Change ch = newChange(pp, Type.EXPORTMOD, oldPi);
		pp.setChange(ch);

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
		Constraint.getCurrent().modifyExport(pp, oldPi);
	}

	public static void modifyCell(Cell cell, double oLX, double oHX, double oLY, double oHY)
	{
		if (!recordChange()) return;
		Change ch = newChange(cell, Type.CELLMOD, null);
		ch.setDoubles(oLX, oHX, oLY, oHY);
		cell.setChange(ch);

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
		//Constraint.getCurrent().modifyCell(cell, oLX, oHX, oLY, oHY);
	}

	public static void  modifyTextDescript(ElectricObject obj, TextDescriptor descript, int oldDescript0, int oldDescript1)
	{
		if (!recordChange()) return;
		Change ch = newChange(obj, Type.DESCRIPTORMOD, descript);
		ch.i1 = oldDescript0;
		ch.i2 = oldDescript1;

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
		// tell constraint system about this TextDescriptor
		Constraint.getCurrent().modifyTextDescript(obj, descript, oldDescript0, oldDescript1);
	}

	public static void newObject(ElectricObject obj)
	{
		if (!recordChange()) return;
		Type type = Type.OBJECTNEW;
		if (obj instanceof Cell) type = Type.CELLNEW;
		else if (obj instanceof NodeInst) type = type.NODEINSTNEW;
		else if (obj instanceof ArcInst) type = type.ARCINSTNEW;
		else if (obj instanceof Export) type = type.EXPORTNEW;
		Change ch = newChange(obj, type, null);

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
		Constraint.getCurrent().newObject(obj);
	}

	public static void killObject(ElectricObject obj)
	{
		if (!recordChange()) return;
		Type type = Type.OBJECTKILL;
		if (obj instanceof Cell) type = Type.CELLKILL;
		else if (obj instanceof NodeInst) type = Type.NODEINSTKILL;
		else if (obj instanceof ArcInst) type = Type.ARCINSTKILL;
		else if (obj instanceof Export) type = Type.EXPORTKILL;
		Change ch = newChange(obj, type, null);

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
		Constraint.getCurrent().killObject(obj);
	}

	public static void redrawObject(ElectricObject obj)
	{
		if (!recordChange()) return;
		Type type = Type.OBJECTREDRAW;
		Change ch = newChange(obj, type, null);

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
	}

	public static void newVariable(ElectricObject obj, Variable var)
	{
		if (!recordChange()) return;
		Type type = Type.VARIABLENEW;
		Change ch = newChange(obj, type, var);

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
		Constraint.getCurrent().newVariable(obj, var);
	}

	public static void killVariable(ElectricObject obj, Variable var)
	{
		if (!recordChange()) return;
		Type type = Type.VARIABLEKILL;
		Change ch = newChange(obj, type, var);

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
		Constraint.getCurrent().killVariable(obj, var);
	}

	public static void modifyVariableFlags(ElectricObject obj, Variable var, int oldFlags)
	{
		if (!recordChange()) return;
		Change ch = Undo.newChange(obj, Type.VARIABLEMODFLAGS, var);
		ch.i1 = oldFlags;

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
		Constraint.getCurrent().modifyVariableFlags(obj, var, oldFlags);
	}

	public static void modifyVariable(ElectricObject obj, Variable var, int index, Object oldValue)
	{
		if (!recordChange()) return;
		Change ch = Undo.newChange(obj, Type.VARIABLEMOD, var);
		ch.i1 = index;
		ch.o2 = oldValue;

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
		Constraint.getCurrent().modifyVariable(obj, var, index, oldValue);
	}

	public static void insertVariable(ElectricObject obj, Variable var, int index)
	{
		if (!recordChange()) return;
		Change ch = Undo.newChange(obj, Type.VARIABLEINSERT, var);
		ch.i1 = index;

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
		Constraint.getCurrent().insertVariable(obj, var, index);
	}

	public static void deleteVariable(ElectricObject obj, Variable var, int index, Object oldValue)
	{
		if (!recordChange()) return;
		Change ch = Undo.newChange(obj, Type.VARIABLEDELETE, var);
		ch.i1 = index;
		ch.o2 = oldValue;

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
		Constraint.getCurrent().deleteVariable(obj, var, index, oldValue);
	}

	/**
	 * Routine to return the current change batch.
	 * @return the current change batch (null if no changes are being done).
	 */
	public static ChangeBatch getCurrentBatch() { return currentBatch; }

	/**
	 * Routine to request that the next change be made "quietly".
	 * Quiet changes are not passed to constraint satisfaction, not recorded for Undo and are not broadcast.
	 */
	public static void setNextChangeQuiet() { doNextChangeQuietly = true; }

	/**
	 * Routine to request that the next change not be made "quietly".
	 * Quiet changes are not passed to constraint satisfaction, not recorded for Undo and are not broadcast.
	 */
	public static void clearNextChangeQuiet() { doNextChangeQuietly = false; }

	/**
	 * Routine to set the subsequent changes to be "quiet".
	 * Quiet changes are not passed to constraint satisfaction, not recorded for Undo and are not broadcast.
	 */
	public static void changesQuiet(boolean quiet) { doChangesQuietly = quiet; }

	/**
	 * Routine to tell whether changes are currently "quiet".
	 * Quiet changes are not passed to constraint satisfaction, not recorded for Undo and are not broadcast.
	 * By calling this routine, the "next change quiet" state is reset.
	 * @return true if changes are "quiet".
	 */
	public static boolean recordChange()
	{
		boolean returnValue = !doNextChangeQuietly && !doChangesQuietly;
		clearNextChangeQuiet();
		return returnValue;
	}

	/**
	 * Routine to undo the last batch of changes.
	 * @return true if a batch was undone.
	 */
	public static boolean undoABatch()
	{
		// close out the current batch
		endChanges();

		// get the most recent batch of changes
		int listSize = doneList.size();
		if (listSize == 0) return false;
		ChangeBatch batch = (ChangeBatch)doneList.get(listSize-1);
		doneList.remove(listSize-1);
		undoneList.add(batch);

		// look through the changes in this batch
		boolean firstChange = true;
		int batchSize = batch.changes.size();
		for(int i = batchSize-1; i >= 0; i--)
		{
			Change ch = (Change)batch.changes.get(i);

			// reverse the change
			ch.reverse();

			// now broadcast this change
			ch.broadcast(firstChange, true);
			firstChange = false;
		}

		// broadcast the end-batch
		for(Iterator it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = (Tool)it.next();
			if (tool.isOn()) tool.endBatch();
		}

		// mark that this batch is undone
		batch.done = false;
		return true;
	}

	/**
	 * Routine to redo the last batch of changes.
	 * @return true if a batch was redone.
	 */
	public static boolean redoABatch()
	{
		// close out the current batch
		endChanges();

		// get the most recent batch of changes
		int listSize = undoneList.size();
		if (listSize == 0) return false;
		ChangeBatch batch = (ChangeBatch)undoneList.get(listSize-1);
		undoneList.remove(listSize-1);
		doneList.add(batch);

		// look through the changes in this batch
		boolean firstChange = true;
		int batchSize = batch.changes.size();
		for(int i = 0; i<batchSize; i++)
		{
			Change ch = (Change)batch.changes.get(i);

			// reverse the change
			ch.reverse();

			// now broadcast this change
			ch.broadcast(firstChange, true);
			firstChange = false;
		}

		// broadcast the end-batch
		for(Iterator it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = (Tool)it.next();
			if (tool.isOn()) tool.endBatch();
		}

		// mark that this batch is redone
		batch.done = true;
		return true;
	}

	
	/**
	 * Returns root of up-tree of undo or redo batch.
	 * @param redo true if redo batch, false if undo batch
	 * @return root of up-tree of a batch.
	 */
	public static Cell upCell(boolean redo)
	{
		ChangeBatch batch = null;
		if (redo)
		{
			int listSize = undoneList.size();
			if (listSize > 0)
				batch = (ChangeBatch)undoneList.get(listSize-1);
		} else
		{
			int listSize = doneList.size();
			if (listSize != 0)
				batch = (ChangeBatch)doneList.get(listSize-1);
		}
		return batch != null ? batch.upCell : null;

	}

	/**
	 * Routine to prevent undo by deleting all change batches.
	 */
	public static void noUndoAllowed()
	{
 		// properly terminate the current batch
		endChanges();

 		// kill them all
 		doneList.clear();
 		undoneList.clear();
	}

	/**
	 * Routine to prevent redo by deleting all undone change batches.
	 */
	public static void noRedoAllowed()
	{
		// properly terminate the current batch
		endChanges();

		undoneList.clear();
	}

	/**
	 * Routine to set the size of the history list and return the former size.
	 * @param newSize the new size of the history list (number of batches of changes).
	 * If not positive, the list size is not changed.
	 * @return the former size of the history list.
	 */
	public static int historyListSize(int newSize)
	{
		if (newSize <= 0) return maximumBatches;

		int oldSize = maximumBatches;
		maximumBatches = newSize;
		if (doneList.size() > maximumBatches)
		{
			doneList.remove(0);
		}
		return oldSize;
	}

	/**
	 * Routine to display all changes.
	 */
	public static void showHistoryList()
	{
		System.out.println("----------  Done batches (" + doneList.size() + ") ----------:");
		for(int i=0; i<doneList.size(); i++)
		{
			ChangeBatch batch = (ChangeBatch)doneList.get(i);
			batch.describe();
		}
		System.out.println("----------  Undone batches (" + undoneList.size() + ") ----------:");
		for(int i=0; i<undoneList.size(); i++)
		{
			ChangeBatch batch = (ChangeBatch)undoneList.get(i);
			batch.describe();
		}
	}
}
