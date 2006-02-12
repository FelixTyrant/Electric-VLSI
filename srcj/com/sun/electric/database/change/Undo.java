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

import com.sun.electric.database.CellUsage;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableCell;
import com.sun.electric.database.ImmutableElectricObject;
import com.sun.electric.database.ImmutableExport;
import com.sun.electric.database.ImmutableLibrary;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.constraint.Constraints;
import com.sun.electric.database.constraint.Layout;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.ServerJobManager;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.UserInterfaceMain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
/**
 * This interface defines changes that are made to the database.
 */
public class Undo
{

	/**
	 * Type is a typesafe enum class that describes the nature of a change.
	 */
    public enum Type
    {
		/** Describes a newly-created NodeInst. */							NODEINSTNEW,
		/** Describes a deleted NodeInst. */								NODEINSTKILL,
		/** Describes a changed NodeInst. */								NODEINSTMOD,
		/** Describes a newly-created ArcInst. */							ARCINSTNEW,
		/** Describes a deleted ArcInst. */									ARCINSTKILL,
		/** Describes a changed ArcInst. */									ARCINSTMOD,
		/** Describes a newly-created Export. */							EXPORTNEW,
		/** Describes a deleted Export. */									EXPORTKILL,
		/** Describes a changed Export. */									EXPORTMOD,
		/** Describes a newly-created Cell. */								CELLNEW,
		/** Describes a deleted Cell. */									CELLKILL,
		/** Describes the renaming of an arbitrary object. */				OBJECTRENAME,
		/** Describes the redrawing of an arbitrary object. */				OBJECTREDRAW,
		/** Describes the change of Variables on an object. */              VARIABLESMOD,
		/** Describes a new library change */								LIBRARYNEW,
		/** Describes a delete library change */							LIBRARYKILL,
		/** Describes a Cell-group change */								CELLGROUPMOD,
		/** Describes an other (non-undoable) change */						OTHERCHANGE;
    }

	/**
	 * The Change class describes a single change to the Electric database.
	 * These objects are used to undo and redo changes.
	 */
	public static class Change
	{
		private ElectricObject obj;
		private Type type;
		private Object o1;

		Change(ElectricObject obj, Type type)
		{
			this.obj = obj;
			this.type = type;
		}

		/**
		 * Method to get the ElectricObject associated with this Change.
		 * @return the ElectricObject associated with this Change.
		 */
		public ElectricObject getObject() { return obj; }
		/**
		 * Method to get the type of this Change.
		 * @return the type of this Change.
		 */
		public Type getType() { return type; }
		/**
		 * Method to get the first Object associated with this Change.
		 * @return the first Object associated with this Change.
		 */
		public Object getO1() { return o1; }

		/**
		 * Method to broadcast a change to all tools that are on.
		 * @param undoRedo true if this is an undo/redo batch.
		 */
		private void broadcast(boolean undoRedo)
		{
			// start the batch if this is the first change
			broadcasting = type;
			if (type == Type.NODEINSTNEW || type == Type.ARCINSTNEW || type == Type.EXPORTNEW ||
				type == Type.CELLNEW/* || type == Type.LIBRARYNEW*/)
			{
				for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.newObject(obj);
				}
			} else if (type == Type.NODEINSTKILL || type == Type.ARCINSTKILL || type == Type.EXPORTKILL ||
				type == Type.CELLKILL/* || type == Type.LIBRARYKILL*/)
			{
				for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.killObject(obj);
				}
			} else if (type == Type.OBJECTRENAME) {
				for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.renameObject(obj, o1);
				}
			} else if (type == Type.OBJECTREDRAW)
			{
				for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.redrawObject(obj);
				}
			} else if (type == Type.NODEINSTMOD)
			{
				for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.modifyNodeInst((NodeInst)obj, (ImmutableNodeInst)o1);
				}
			} else if (type == Type.ARCINSTMOD)
			{
				for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.modifyArcInst((ArcInst)obj, (ImmutableArcInst)o1);
				}
			} else if (type == Type.EXPORTMOD)
			{
				for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.modifyExport((Export)obj, (ImmutableExport)o1);
				}
			} else if (type == Type.CELLGROUPMOD)
			{
				for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.modifyCellGroup((Cell)obj, (Cell.CellGroup)o1);
				}
			} else if (type == Type.VARIABLESMOD)
			{
				for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.modifyVariables(obj, (ImmutableElectricObject)o1);
				}
			} else if (type == Type.OTHERCHANGE)
            {
            }
			broadcasting = null;
		}

		/**
		 * Method to undo the effects of this change.
		 */
		private void reverse(boolean backwards)
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
                ImmutableNodeInst oldD = ni.getD();

				// change the node information
				ni.lowLevelModify((ImmutableNodeInst)o1);

				// update the change to its reversed state
				o1 = oldD;
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
				if (Job.getDebug()) ai.getParent().checkInvariants();
				return;
			}
			if (type == Type.ARCINSTMOD)
			{
				// get information about the arc as it is now
				ArcInst ai = (ArcInst)obj;
                ImmutableArcInst oldD = ai.getD();

				// change the arc information
				ai.lowLevelModify((ImmutableArcInst)o1);

				// update the change to its reversed state
                o1 = oldD;
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
				if (Job.getDebug()) ((Cell)pp.getParent()).checkInvariants();
				return;
			}
			if (type == Type.EXPORTMOD)
			{
				Export pp = (Export)obj;
                ImmutableExport oldD = pp.getD();
				pp.lowLevelModify((ImmutableExport)o1);
				o1 = oldD;
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
			if (type == Type.CELLGROUPMOD)
			{
				Cell cell = (Cell)obj;
				Cell.CellGroup oldGroup = (Cell.CellGroup)o1;
				o1 = cell.getCellGroup();
				cell.lowLevelSetCellGroup(oldGroup);
				return;
			}
			if (type == Type.OBJECTRENAME)
			{
                 if (obj instanceof Cell)
				{
					Cell cell = (Cell)obj;
					CellName oldName = (CellName)o1;
					o1 = cell.getCellName();
					cell.lowLevelRename(oldName);
				} else if (obj instanceof Library)
				{
					Library lib = (Library)obj;
					String oldName = (String)o1;
					o1 = lib.getName();
					lib.lowLevelRename(oldName);
				}
				return;
			}
			if (type == Type.VARIABLESMOD)
			{
				ImmutableElectricObject oldImmutable = obj.getImmutable();
                if (obj instanceof Cell)
                    ((Cell)obj).lowLevelModifyVariables((ImmutableCell)o1);
                else if (obj instanceof Library)
                    ((Library)obj).lowLevelModifyVariables((ImmutableLibrary)o1);
                o1 = oldImmutable;
				return;
			}
            if (type == Type.OTHERCHANGE)
            {
                return;
            }
		}

		/**
		 * Method to examine a change and mark the appropriate libraries and cells as "dirty".
		 * @param type the type of change being made.
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
                if (type == Type.EXPORTMOD && pp.getNameKey() != ((ImmutableExport)o1).name)
                    modifyUsages(cell);
				major = true;
			} else if (type == Type.CELLNEW || type == Type.CELLKILL)
			{
				cell = (Cell)obj;
				lib = cell.getLibrary();
				major = true;
			} else if (type == Type.CELLGROUPMOD)
			{
				cell = (Cell)obj;
				lib = cell.getLibrary();
			} else if (type == Type.OBJECTREDRAW)
            {
                return;
            } else if (type == Type.OBJECTRENAME)
			{
				cell = obj.whichCell();
				if (cell != null)
				{
					lib = cell.getLibrary();
                    modifyUsages(cell);
				}
				major = true;   // this is major change for the library (E.g.: export names)
			} else if (type == Type.VARIABLESMOD)
			{
				cell = obj.whichCell();
				if (cell != null) lib = cell.getLibrary();
//				Variable var = (Variable)o1;
//				major = isMajorVariable(obj, var.getKey());
			} else if (type == Type.OTHERCHANGE)
            {
                cell = obj.whichCell();
                if (cell != null) lib = cell.getLibrary();
            }

			// set "changed" and "dirty" bits
			if (cell != null)
			{
				if (major)
                    cell.madeRevision();
                cell.setModified(major); // this will avoid marking DRC variables.

				changedCells.add(cell);
			}
			if (lib != null)
			{
				if (major) lib.setChangedMajor(); else
					lib.setChangedMinor();
			}
		}

        /**
         * Mark libraries that reference given Cell as dirty.
         * @param cell given cell.
         */
        private static void modifyUsages(Cell cell) {
            for (Iterator<CellUsage> it = cell.getUsagesOf(); it.hasNext(); ) {
                CellUsage u = (CellUsage)it.next();
                Cell parent = u.getParent();
                parent.getLibrary().setChanged();
                parent.setModified(true);
            }
        }
        
		private static boolean isMajorVariable(ElectricObject obj, Variable.Key key)
		{
// 			if ((obj instanceof Cell) && key == el_cell_message_key) return true;
// 			if ((obj instanceof NodeInst) && key == sim_weaknodekey) return true;
			return false;
		}

		/**
		 * Returns a printable version of this Change.
		 * @return a printable version of this Change.
		 */
		public String toString() { return describe(); }

		/**
		 * Method to describe this change as a string.
		 */
		private String describe()
		{
			if (type == Type.NODEINSTNEW)
			{
				NodeInst ni = (NodeInst)obj;
				return ni + " created in " + ni.getParent();
			}
			if (type == Type.NODEINSTKILL)
			{
				NodeInst ni = (NodeInst)obj;
				return ni + " deleted from " + ni.getParent();
			}
			if (type == Type.NODEINSTMOD)
			{
				NodeInst ni = (NodeInst)obj;
                ImmutableNodeInst d = (ImmutableNodeInst)getO1();
				return ni + " modified in " + ni.getParent() +
					"[was " + d.width + "x" + d.height + " at (" + d.anchor.getX() + "," + d.anchor.getY() + ") rotated " + d.orient + ", is " +
					ni.getD().width + "x" + ni.getD().height + " at (" + ni.getAnchorCenterX() + "," +
					ni.getAnchorCenterY() + ") rotated " + ni.getOrient() + "]";
			}
			if (type == Type.ARCINSTNEW)
			{
				ArcInst ai = (ArcInst)obj;
				return ai + " created in " + ai.getParent();
			}
			if (type == Type.ARCINSTKILL)
			{
				ArcInst ai = (ArcInst)obj;
				return ai + " deleted from " + ai.getParent();
			}
			if (type == Type.ARCINSTMOD)
			{
				ArcInst ai = (ArcInst)obj;
                ImmutableArcInst d = (ImmutableArcInst)getO1();
				return ai + " modified in " + ai.getParent() +
					"[was " + d.width + " wide from (" + d.headLocation.getX() + "," + d.headLocation.getY() + ") to (" + d.tailLocation.getX() + "," + d.tailLocation.getY() + ")]" +
                        ", is " + ai.getWidth() + " wide from (" + ai.getHeadLocation().getX() + "," + ai.getHeadLocation().getY() + ") to (" + ai.getTailLocation().getX() + "," + ai.getTailLocation().getY() + ")";
			}
			if (type == Type.EXPORTNEW)
			{
				Export pp = (Export)obj;
				return "Export " + pp.getName() + " created in " + pp.getParent();
			}
			if (type == Type.EXPORTKILL)
			{
				Export pp = (Export)obj;
				return "Export " + pp.getName() + " deleted from " + pp.getParent();
			}
			if (type == Type.EXPORTMOD)
			{
				Export pp = (Export)obj;
				ImmutableExport d = (ImmutableExport)o1;
                PortInst pi = ((Cell)pp.getParent()).getPortInst(d.originalNodeId, d.originalPortId);
				return "Export " + pp.getName() + " moved in " + pp.getParent() +
					"[was on " + pi.getNodeInst() + " port " + pi.getPortProto().getName() + "]";
			}
			if (type == Type.CELLNEW)
			{
				Cell cell = (Cell)obj;
				return cell + " created";
			}
			if (type == Type.CELLKILL)
			{
				Cell cell = (Cell)obj;
				return cell + " deleted";
			}
			if (type == Type.CELLGROUPMOD)
			{
				Cell cell = (Cell)obj;
//				Cell.CellGroup group = (Cell.CellGroup)o1;
				return cell + " moved to group";
			}
			if (type == Type.OBJECTRENAME)
			{
				return "Renamed object " + obj + " (was " + o1 + ")";
			}
			if (type == Type.OBJECTREDRAW)
			{
				return "Redraw object " + obj;
			}
			if (type == Type.VARIABLESMOD)
			{
				return "Changed variables on "+obj;
			}
			if (type == Type.LIBRARYNEW)
			{
				return "Created "+obj;
			}
			if (type == Type.LIBRARYKILL)
			{
				return "Deleted "+obj;
			}
            if (type == Type.OTHERCHANGE)
            {
                return "Other (non-undoable) change in " + obj;
            }
			return "UNKNOWN CHANGE, type=" + type;
		}
	}

	/**
	 * Class to describe a batch of changes to the Electric database.
	 */
	public static class ChangeBatch
	{
		private ArrayList<Change> changes;
		private int batchNumber;
//		private boolean done;
		private Tool tool;
		private String activity;
//		private Cell upCell;
		private int startingHighlights = -1;				// highlights before changes made
		private int preUndoHighlights = -1;				// highlights before undo of changes done

		private ChangeBatch() {}
		
		private void add(Change change) { changes.add(change); }

		/**
		 * Method to return an iterator over all changes in this ChangeBatch.
		 * @return an iterator over all changes in this ChangeBatch.
		 */
		public Iterator<Change> getChanges() { return changes.iterator(); }

		/**
		 * Method to return the number of this ChangeBatch.
		 * Each batch has a unique number, and their order increases with time.
		 * @return the unique number of this ChangeBatch.
		 */
		public int getBatchNumber() { return batchNumber; }

        public int getStartingHighlights() { return startingHighlights; }
        public int getPreUndoHighlights() { return preUndoHighlights; }
        
		/**
		 * Method to return the Tool associated with this ChangeBatch.
		 * @return the Tool associated with this ChangeBatch.
		 */
		public Tool getTool() { return tool; }

		/**
		 * Method to return the number of changes in this ChangeBatch.
		 * @return the number of changes in this ChangeBatch.
		 */
		public int getNumChanges() { return changes.size(); }

        public void reverse(boolean backwards) {
            if (changes.isEmpty()) return;
            broadcastStart(true);
            
            Collections.reverse(changes);
            for(Change ch: changes) {
                // reverse the change
                ch.reverse(backwards);
                // now broadcast this change
                ch.broadcast(true);
            }
            
        }
        
		private void describe(String title)
		{
			// display the change batches
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
				} else if (ch.getType() == Type.CELLNEW || ch.getType() == Type.CELLKILL || ch.getType() == Type.CELLGROUPMOD)
				{
					cell++;
				} else if (ch.getType() == Type.OBJECTREDRAW)
				{
					object++;
				} else if (ch.getType() == Type.VARIABLESMOD)
				{
					variable++;
				} else if (ch.getType() == Type.OTHERCHANGE)
                {
                        object++;
                }
			}

			String message = "*** Batch '" + title + "', " + batchNumber + " (" + activity + " from " + tool.getName() + " tool) has " + batchSize + " changes and affects";
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

	private static Type broadcasting = null;
	private static boolean doChangesQuietly = false;
	private static ChangeBatch currentBatch = null;
	private static int maximumBatches = User.getMaxUndoHistory();
	private static int overallBatchNumber = 0;
	private static List<ChangeBatch> doneList = new ArrayList<ChangeBatch>();
	private static List<ChangeBatch> undoneList = new ArrayList<ChangeBatch>();
	private static HashSet<Cell> changedCells = new HashSet<Cell>();

    /** Listeners. */
    private static EventListenerList listenerList = new EventListenerList();

	/**
	 * Method to start a new batch of changes.
	 * @param tool the tool that is producing the activity.
	 * @param activity a String describing the activity.
	 * @param cell root of up-tree or null for whole database lock
	 */
	public static void startChanges(Tool tool, String activity, int startingHighlights)
	{
		// close any open batch of changes
		clearChanges();

		// kill off any undone batches
		noRedoAllowed();

		// erase the list of changed cells
		changedCells.clear();

		// allocate a new change batch
		currentBatch = new ChangeBatch();
		currentBatch.changes = new ArrayList<Change>();
		currentBatch.batchNumber = ++overallBatchNumber;
//		currentBatch.done = true;
		currentBatch.tool = tool;
		currentBatch.activity = activity;
//		currentBatch.upCell = cell;
		currentBatch.startingHighlights = startingHighlights;

		// put at head of list
		doneList.add(currentBatch);

		// kill last batch if list is full
		if (doneList.size() > maximumBatches)
		{
			doneList.remove(0);
		}
        updateUndoRedo();

		// start the batch of changes
		Constraints.getCurrent().startBatch(tool, false);

		for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
		{
			Listener listener = (Listener)it.next();
			listener.startBatch(tool, false);
		}
	}

    private static void clearChanges() {
        endChanges();
        if (currentBatch == null) return;
        System.out.println("Ignored a batch " + currentBatch.activity + " of " + currentBatch.getNumChanges() + " changes");
        currentBatch = null;
    }
    
    /**
	 * Method to terminate the current batch of changes.
	 */
	public static void endChanges()
	{
		// if no changes were recorded, stop
		if (currentBatch == null) return;

		// if no changes were recorded, stop
		if (currentBatch.changes.size() == 0)
		{
			// remove from doneList
			doneList.remove(currentBatch);
            updateUndoRedo();
		}

		// changes made: apply final constraints to this batch of changes
		fireEndChangeBatch(currentBatch, false);

		currentBatch = null;
	}

	/**
	 * Method to return an iterator on cells that have changed in the current batch.
	 * @return an Interator over the changed cells.
	 */
	public static Iterator<Cell> getChangedCells()
	{
		return changedCells.iterator();
	}

	private static synchronized void fireEndChangeBatch(ChangeBatch batch, boolean undo)
	{
        if (undo) {
            refreshCellBounds();
        } else {
            Constraints.getCurrent().endBatch();
        }
		for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
		{
			Listener listener = (Listener)it.next();
			listener.endBatch();
		}
        
		if (Job.BATCHMODE) return;

		SwingUtilities.invokeLater(new DatabaseChangeRun(batch));
	}
    
    private static synchronized void updateUndoRedo() {
        ServerJobManager.setUndoRedoStatus(!doneList.isEmpty(), !undoneList.isEmpty());
    }

	private static class DatabaseChangeRun implements Runnable
	{
		private DatabaseChangeEvent event;
		private DatabaseChangeRun(ChangeBatch batch) { event = new DatabaseChangeEvent(batch); }
        public void run() {
            UserInterfaceMain.fireDatabaseChangeEvent(event);
        }
	}

    private static void broadcastStart(boolean undoRedo) {
        // broadcast a start-batch on the first change
        for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); ) {
            Listener listener = (Listener)it.next();
            listener.startBatch(listener, undoRedo);
        }
    }
    
	/**
	 * Method to record and broadcast a change.
	 * <P>
	 * These types of changes exist:
	 * <UL>
	 * <LI>NODEINSTNEW takes nothing.
	 * <LI>NODEINSTKILL takes nothing.
	 * <LI>NODEINSTMOD takes o1=oldD.
	 * <LI>ARCINSTNEW takes nothing.
	 * <LI>ARCINSTKILL takes nothing.
	 * <LI>ARCINSTMOD takes o1=oldD.
	 * <LI>EXPORTNEW takes nothing.
	 * <LI>EXPORTKILL takes nothing.
	 * <LI>EXPORTMOD takes o1=oldD.
	 * <LI>CELLNEW takes nothing.
	 * <LI>CELLKILL takes nothing.
	 * <LI>OBJECTRENAME takes o1=oldName.
	 * <LI>OBJECTREDRAW takes nothing.
	 * <LI>VARIABLESMOD takes o1=oldImmutable.
	 * <LI>CELLGROUPMOD takes o1=oldCellGroup
     * <LI>OTHERCHANGE takes nothing
	 * </UL>
	 * @param obj the object to which the change applies.
	 * @param change the change being recorded.
	 * @return the change object (null on error).
	 */
	private static Change newChange(ElectricObject obj, Type change, Object o1)
	{
        Job.checkChanging();
		if (currentBatch == null)
		{
			System.out.println("Received " + change + " change when no batch started");
			return null;
		}
		if (broadcasting != null)
		{
			System.out.println("Received " + change + " change during broadcast of " + broadcasting);
			return null;
		}

		// determine what needs to be marked as changed
        Change.setDirty(change, obj, o1);

		// see if this is the first change
//		boolean firstChange = false;
//		if (currentBatch.getNumChanges() == 0) firstChange = true;

		// get change module
		Change ch = new Change(obj, change);
		ch.o1 = o1;

		// insert new change module into linked list
		currentBatch.add(ch);

		// broadcast the change
		//ch.broadcast(firstChange, false);
		return ch;
	}

	/**
	 * Method to store a change to a NodeInst in the change-control system.
	 * @param ni the NodeInst that changed.
	 * @param oD the old contents of the NodeInst.
	 */
	public static void modifyNodeInst(NodeInst ni, ImmutableNodeInst oD)
	{
		if (!recordChange()) return;
		Change ch = newChange(ni, Type.NODEINSTMOD, oD);
		if (ch == null) return;

        if (currentBatch.getNumChanges() <= 1)
            broadcastStart(false);
		ch.broadcast(false);
        Constraints.getCurrent().modifyNodeInst(ni, oD);
	}

	/**
	 * Method to store a change to an ArcInst in the change-control system.
	 * @param ai the ArcInst that changed.
     * @param oD the old contents of the ArcInst.
	 */
	public static void modifyArcInst(ArcInst ai, ImmutableArcInst oD)
	{
		if (!recordChange()) return;
		Change ch = newChange(ai, Type.ARCINSTMOD, oD);
		if (ch == null) return;

        if (currentBatch.getNumChanges() <= 1)
            broadcastStart(false);
		ch.broadcast(false);
        Constraints.getCurrent().modifyArcInst(ai, oD);
	}

	/**
	 * Method to store a change to an Export in the change-control system.
	 * Export changes involve moving them from one PortInst to another in the Cell.
	 * @param pp the Export that was moved.
	 * @param oD the old contents of the Export.
	 */
	public static void modifyExport(Export pp, ImmutableExport oD)
	{
		if (!recordChange()) return;
		Change ch = newChange(pp, Type.EXPORTMOD, oD);
		if (ch == null) return;

        if (currentBatch.getNumChanges() <= 1)
            broadcastStart(false);
		ch.broadcast(false);
		Constraints.getCurrent().modifyExport(pp, oD);
	}

	/**
	 * Method to store a change to a Cell's CellGroup in the change-control system.
	 * @param cell the Cell whose CellGroup is changing
	 * @param oldGroup the old CellGroup
	 */
	public static void modifyCellGroup(Cell cell, Cell.CellGroup oldGroup)
	{
		if (!recordChange()) return;
		Change ch = newChange(cell, Type.CELLGROUPMOD, oldGroup);
		if (ch == null) return;

        if (currentBatch.getNumChanges() <= 1)
            broadcastStart(false);
		ch.broadcast(false);
	}

	/**
	 * Method to store the creation of a new ElectricObject in the change-control system.
	 * @param obj the ElectricObject that was created.
	 */
	public static void newObject(ElectricObject obj)
	{
//		// always broadcast library changes
//		if (!recordChange() && !(obj instanceof Library)) return;
		if (!recordChange()) return;
		Cell cell = obj.whichCell();
		Type type = null;
		if (obj instanceof Cell) type = Type.CELLNEW;
		else if (obj instanceof NodeInst) type = Type.NODEINSTNEW;
		else if (obj instanceof ArcInst) type = Type.ARCINSTNEW;
		else if (obj instanceof Export) type = Type.EXPORTNEW;
		else if (obj instanceof Library) type = Type.LIBRARYNEW;
		Change ch = newChange(obj, type, null);
		if (ch == null) return;

        if (currentBatch.getNumChanges() <= 1)
            broadcastStart(false);
		ch.broadcast(false);
		Constraints.getCurrent().newObject(obj);
	}

	/**
	 * Method to store the deletion of an ElectricObject in the change-control system.
	 * @param obj the ElectricObject that was deleted.
	 * Note: because a reference to the object is kept in the change-control system,
	 * the object is not really deallocated until the change-control system no longer
	 * remembers the change, and it can no longer be undone.
	 */
	public static void killObject(ElectricObject obj)
	{
//		// always broadcast library changes
//		if (!recordChange() && !(obj instanceof Library)) return;
		if (!recordChange()) return;
		Type type = null;
		if (obj instanceof Cell) type = Type.CELLKILL;
		else if (obj instanceof NodeInst) type = Type.NODEINSTKILL;
		else if (obj instanceof ArcInst) type = Type.ARCINSTKILL;
        else if (obj instanceof Export) type = Type.EXPORTKILL;
		else if (obj instanceof Library) type = Type.LIBRARYKILL;
		Change ch = newChange(obj, type, null);
		if (ch == null) return;

        if (currentBatch.getNumChanges() <= 1)
            broadcastStart(false);
		ch.broadcast(false);
		Constraints.getCurrent().killObject(obj);
	}

	/**
	 * Method to store the renaming of an ElectricObject in the change-control system.
	 * @param obj the ElectricObject that was renamed.
	 * @param oldName the former name of the ElectricObject.
	 */
	public static void renameObject(ElectricObject obj, Object oldName)
	{
		if (!recordChange()) return;
		Type type = Type.OBJECTRENAME;
		Change ch = newChange(obj, type, oldName);
		if (ch == null) return;

        if (currentBatch.getNumChanges() <= 1)
            broadcastStart(false);
		ch.broadcast(false);
		Constraints.getCurrent().renameObject(obj, oldName);
	}

	/**
	 * Method to store the redrawing of an ElectricObject in the change-control system.
	 * This does not relate to database changes, but may be necessary if
	 * another change requires redisplay of the object.
	 * @param obj the ElectricObject to redisplay.
	 */
	public static void redrawObject(ElectricObject obj)
	{
		if (!recordChange()) return;
		Type type = Type.OBJECTREDRAW;
		Change ch = newChange(obj, type, null);
		if (ch == null) return;

        if (currentBatch.getNumChanges() <= 1)
            broadcastStart(false);
		ch.broadcast(false);
	}

    /*
	 * Method to store the change of object Variables.
	 * @param obj the ElectricObject on which Variables changed.
	 * @param oldImmutable the old Variables.
	 */
	public static void modifyVariables(ElectricObject obj, ImmutableElectricObject oldImmutable)
	{
		if (!recordChange()) return;
		Type type = Type.VARIABLESMOD;
		Change ch = newChange(obj, type, oldImmutable);
		if (ch == null) return;

        if (currentBatch.getNumChanges() <= 1)
            broadcastStart(false);
		ch.broadcast(false);
		Constraints.getCurrent().modifyVariables(obj, oldImmutable);
	}

	/**
	 * Method to schedule an 'other' change on an ElectricObject.
	 * @param obj the ElectricObject on which the miscellaneous change is being made.
	 */
    public static void otherChange(ElectricObject obj)
    {
        if (!recordChange()) return;
        Change ch = Undo.newChange(obj, Type.OTHERCHANGE, null);
        if (ch == null) return;
        
//        ch.broadcast(currentBatch.getNumChanges() <= 1, false);
    }
    
    /**
	 * Method to return the current change batch.
	 * @return the current change batch (null if no changes are being done).
	 */
	public static ChangeBatch getCurrentBatch() { return currentBatch; }

	/**
	 * Method to tell whether changes are being made quietly.
	 * Quiet changes are not passed to constraint satisfaction, not recorded for Undo and are not broadcast.
	 * @return true if changes are being made quietly.
	 */
	public static boolean isChangeQuiet() { return doChangesQuietly; }

	/**
	 * Method to set the subsequent changes to be "quiet".
	 * Quiet changes are not passed to constraint satisfaction, not recorded for Undo and are not broadcast.
	 * @return the previous value of the "quiet" state.
	 */
	public static boolean changesQuiet(boolean quiet) {
		Library.checkInvariants();
        Layout.changesQuiet(quiet);
		NetworkTool.changesQuiet(quiet);
		boolean formerQuiet = doChangesQuietly;
        doChangesQuietly = quiet;
		return formerQuiet;
    }

	/**
	 * Method to tell whether changes are currently "quiet".
	 * Quiet changes are not passed to constraint satisfaction, not recorded for Undo and are not broadcast.
	 * By calling this method, the "next change quiet" state is reset.
	 * @return true if changes are "quiet".
	 */
	public static boolean recordChange()
	{
		if (currentBatch == null) return false;
		return !doChangesQuietly;
	}

	/**
	 * Method to undo the last batch of changes.
	 * @return the batch number that was undone.
	 * Returns null if nothing was undone.
	 */
	public static ChangeBatch undoABatch(int savedHighlights)
	{
		// save highlights for redo
//		List<Object> savedHighlights = null;
//		Point2D offset = new Point2D.Double(0, 0);
//		// for now, just save from the current window
//		UserInterface ui = Job.getUserInterface();
//		EditWindow_ wnd = ui.getCurrentEditWindow_();
//		if (wnd != null)
//		{
//			savedHighlights = wnd.saveHighlightList();
//			offset = wnd.getHighlightOffset();
//            wnd.clearHighlighting();
//            wnd.finishedHighlighting();
////			highlighter.clear();
////			highlighter.finished();
//		}

		// close out the current batch
		clearChanges();

		// get the most recent batch of changes
		int listSize = doneList.size();
		if (listSize == 0) return null;
		ChangeBatch batch = (ChangeBatch)doneList.get(listSize-1);
		doneList.remove(listSize-1);
		undoneList.add(batch);
        updateUndoRedo();

		// save pre undo highlights
		batch.preUndoHighlights = savedHighlights;

		// look through the changes in this batch
        batch.reverse(true);

		// Put message in Message Window
		System.out.println("Undoing: " + batch.activity);
//        batch.describe("Undo");

		// broadcast the end-batch
		fireEndChangeBatch(batch, true);

//		// restore highlights (must be done after all other tools have
//		// responded to changes)
//		if (wnd != null)
//		{
//			wnd.restoreHighlightList(batch.startingHighlights);
//			if (batch.startHighlightsOffset != null)
//				wnd.setHighlightOffset((int)batch.startHighlightsOffset.getX(),
//											   (int)batch.startHighlightsOffset.getY());
//			wnd.finishedHighlighting();
//		}

		// mark that this batch is undone
//		batch.done = false;
		return batch;
	}

	/**
	 * Method to redo the last batch of changes.
	 * @return true if a batch was redone.
	 */
	public static ChangeBatch redoABatch()
	{
//		UserInterface ui = Job.getUserInterface();
//		EditWindow_ wnd = ui.getCurrentEditWindow_();
//		if (wnd != null)
//		{
//			wnd.clearHighlighting();
//			wnd.finishedHighlighting();
//		}

		// close out the current batch
		clearChanges();

		// get the most recent batch of changes
		if (undoneList == null) return null;
		int listSize = undoneList.size();
		if (listSize == 0) return null;
		ChangeBatch batch = (ChangeBatch)undoneList.get(listSize-1);
		undoneList.remove(listSize-1);
		doneList.add(batch);
        updateUndoRedo();

        batch.reverse(false);
        
        // Put message in Message Window
		System.out.println("Redoing: " + batch.activity);
//        batch.describe("Redo");

		// broadcast the end-batch
		fireEndChangeBatch(batch, true);

//		// set highlights to what they were before undo
//		if (batch.preUndoHighlights != null)
//		{
//			wnd.restoreHighlightList(batch.preUndoHighlights);
//			wnd.setHighlightOffset((int)batch.preUndoHighlightsOffset.getX(),
//				(int)batch.preUndoHighlightsOffset.getY());
//			wnd.finishedHighlighting();
//		}

		// mark that this batch is redone
//		batch.done = true;
		return batch;
	}

    /** 
     * Refresh Cell bounds. This method is necessary for Undo/Redu batches,
     * because constraint system is disabled.
     */
    private static void refreshCellBounds() {
        for (Iterator<Library> it = Library.getLibraries(); it.hasNext(); ) {
            Library lib = (Library)it.next();
            for (Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); ) {
                Cell cell = (Cell)cIt.next();
                cell.getBounds();
            }
        }
    }
    
    /**
	 * Returns root of up-tree of undo or redo batch.
	 * @param redo true if redo batch, false if undo batch
	 * @return root of up-tree of a batch.
	 */
	public static Cell upCell(boolean redo)
	{
        return null;
//		ChangeBatch batch = null;
//		if (redo)
//		{
//			int listSize = undoneList.size();
//			if (listSize > 0)
//				batch = (ChangeBatch)undoneList.get(listSize-1);
//		} else
//		{
//			int listSize = doneList.size();
//			if (listSize != 0)
//				batch = (ChangeBatch)doneList.get(listSize-1);
//		}
//		return batch != null ? batch.upCell : null;
	}

	/**
	 * Method to prevent undo by deleting all change batches.
	 */
	public static void noUndoAllowed()
	{
 		// properly terminate the current batch
		clearChanges();

 		// kill them all
 		doneList.clear();
 		undoneList.clear();
        updateUndoRedo();
	}

	/**
	 * Method to prevent redo by deleting all undone change batches.
	 */
	public static void noRedoAllowed()
	{
		// properly terminate the current batch
		clearChanges();

		undoneList.clear();
        updateUndoRedo();
	}

	/**
	 * Method to set the size of the history list and return the former size.
	 * @param newSize the new size of the history list (number of batches of changes).
	 * If not positive, the list size is not changed.
	 * @return the former size of the history list.
	 */
	public static int setHistoryListSize(int newSize)
	{
		if (newSize <= 0) return maximumBatches;

		int oldSize = maximumBatches;
		maximumBatches = newSize;
		if (doneList.size() > maximumBatches)
		{
			doneList.remove(0);
		}
        updateUndoRedo();
		return oldSize;
	}

	/**
	 * Method to display all changes.
	 */
	public static void showHistoryList()
	{
		System.out.println("----------  Done batches (" + doneList.size() + ") ----------:");
		for(int i=0; i<doneList.size(); i++)
		{
			ChangeBatch batch = (ChangeBatch)doneList.get(i);
			batch.describe("Done");
		}
		System.out.println("----------  Undone batches (" + undoneList.size() + ") ----------:");
		for(int i=0; i<undoneList.size(); i++)
		{
			ChangeBatch batch = (ChangeBatch)undoneList.get(i);
			batch.describe("Undone");
		}
	}
}
