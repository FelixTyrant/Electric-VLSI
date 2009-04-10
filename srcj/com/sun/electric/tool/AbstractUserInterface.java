/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UserInterfaceExtended.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool;

import com.sun.electric.database.Snapshot;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.LibId;
import com.sun.electric.database.id.TechId;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

import java.util.List;

/**
 *
 */
public abstract class AbstractUserInterface extends Client implements UserInterface {
    private TechId curTechId;
    private LibId curLibId;

    protected AbstractUserInterface() {
        super(-1);
    }

    public EDatabase getDatabase() {
        return EDatabase.clientDatabase();
    }
	public Technology getCurrentTechnology() {
        EDatabase database = getDatabase();
        Technology tech = null;
        if (curTechId != null)
            tech = database.getTech(curTechId);
        if (tech == null)
            tech = database.getTechPool().findTechnology(User.getDefaultTechnology());
        if (tech == null)
            tech = database.getTechPool().findTechnology("mocmos");
        return tech;
    }
    public TechId getCurrentTechId() { return curTechId; }
    public void setCurrentTechnology(Technology tech) {
        if (tech != null)
            curTechId = tech.getId();
    }
	/**
	 * Method to return the current Library.
	 * @return the current Library.
	 */
	public Library getCurrentLibrary() {
        return curLibId != null ? getDatabase().getLib(curLibId) : null;
    }
    public LibId getCurrentLibraryId() { return curLibId; }
    public void setCurrentLibrary(Library lib) {
        curLibId = lib != null ? lib.getId() : null;
    }
    // For Mac OS X version
    protected void initializeInitJob(Job job, Object mode) {}


    protected void addEvent(Client.ServerEvent serverEvent) {}

    public void finishInitialization() {}

    protected void updateNetworkErrors(Cell cell, List<ErrorLogger.MessageLog> errors) {
        if (!errors.isEmpty()) System.out.println(errors.size() + " network errors in " + cell);
    }

    protected void updateIncrementalDRCErrors(Cell cell, List<ErrorLogger.MessageLog> newErrors,
                                              List<ErrorLogger.MessageLog> delErrors) {
        if (!newErrors.isEmpty()) System.out.println(newErrors.size() + " drc errors in " + cell);
    }

    /**
     * Save current state of highlights and return its ID.
     */
    public int saveHighlights() { return 0; }

    /**
     * Restore state of highlights by its ID.
     * @param highlightsId id of saved highlights.
     */
    public void restoreHighlights(int highlightsId) {}

    /**
     * Show status of undo/redo buttons
     * @param newUndoEnabled new status of undo button.
     * @param newRedoEnabled new status of redo button.
     */
    protected void showUndoRedoStatus(boolean newUndoEnabled, boolean newRedoEnabled) {}

    /**
     * Show new database snapshot.
     * @param newSnapshot new snapshot.
     */
    protected void showSnapshot(Snapshot newSnapshot, boolean undoRedo) {}


    public void beep() {}
}
