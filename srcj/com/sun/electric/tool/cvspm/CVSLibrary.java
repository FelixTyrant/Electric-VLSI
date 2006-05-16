/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CVSLibrary.java
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

package com.sun.electric.tool.cvspm;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.output.DELIB;
import com.sun.electric.tool.user.dialogs.OpenFile;

import java.util.*;
import java.io.File;
import java.awt.Color;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Mar 16, 2006
 * Time: 9:52:50 AM
 * Track the state of a library that has been checked into CVS.
 */
public class CVSLibrary {

    private Library lib;
    private FileType type;
    private State libState;                 // only used for non-DELIB file types
    private Map<Cell,State> cellStates;
    private Map<Cell,Cell> editing;         // list of cells I am editing
    private boolean libEditing;                 // true if library is being edited (only for JELIB, DELIB)

    private static Map<Library,CVSLibrary> CVSLibraries = new HashMap<Library,CVSLibrary>();

    private CVSLibrary(Library lib) {
        this.lib = lib;
        String libFile = lib.getLibFile().getPath();
        type = OpenFile.getOpenFileType(libFile, FileType.JELIB);
        libState = State.NONE;
        libEditing = false;

        cellStates = new HashMap<Cell,State>();
        for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
            Cell cell = it.next();
            cellStates.put(cell, State.NONE);
            if (CVS.isDELIB(lib)) {
                if (!CVS.isFileInCVS(CVS.getCellFile(cell))) cellStates.put(cell, State.UNKNOWN);
            }

        }
        editing = new HashMap<Cell,Cell>();
    }
    /**
     * Add a library to the list of CVS libraries, that will maintain
     * the known state of the library with respect to CVS.  The known
     * state specifies the color mapping of libraries and cells in
     * the explorer tree.
     * @param lib
     */
    public static void addLibrary(Library lib) {
        addLibrary(lib, false);
    }
    private static void addLibrary(Library lib, boolean added) {
        if (lib.isHidden()) return;
        if (!lib.isFromDisk()) return;
        String libFile = lib.getLibFile().getPath();
        if (!added && !CVS.isFileInCVS(new File(libFile))) {
            return;
        }
        CVSLibrary cvslib = new CVSLibrary(lib);
        CVSLibraries.put(lib,cvslib);
    }

    /**
     * Remove a library from the list of CVS libraries.
     * @param lib
     */
    public static void removeLibrary(Library lib) {
        CVSLibraries.remove(lib);
    }

    /**
     * See if library is part of CVS repository
     * @param lib
     * @return true if it is part of a CVS repository,
     * false otherwise
     */
    public static boolean isInCVS(Library lib) {
        CVSLibrary cvslib = CVSLibraries.get(lib);
        if (cvslib == null) return false;
        return true;
    }

    /**
     * See if cell is part of CVS repository (DELIB).
     * @param cell
     * @return
     */
    public static boolean isInCVS(Cell cell) {
        CVSLibrary cvslib = CVSLibraries.get(cell.getLibrary());
        if (cvslib == null) return false;
        if (cvslib.type != FileType.DELIB) return false;
        State state = cvslib.cellStates.get(cell);
        if (state == null) return false;
        if (state == State.UNKNOWN) return false;
        return true;
    }

    /**
     * Only for DELIBs, check if there are any cells in
     * library that have state "UNKNOWN", and need to be added to CVS.
     * @param lib
     * @return
     */
    public static boolean hasUnknownCells(Library lib) {
        CVSLibrary cvslib = CVSLibraries.get(lib);
        if (cvslib == null) return false;
        if (cvslib.type != FileType.DELIB) return false;
        for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
            Cell cell = it.next();
            State state = cvslib.cellStates.get(cell);
            if (state == null) return true;
            if (state == State.UNKNOWN) return true;
        }
        return false;
    }

    // --------------------- State recording ---------------------

    /**
     * Set state of a Cell
     * @param cell
     * @param state
     */
    public static void setState(Cell cell, State state) {
        CVSLibrary cvslib = CVSLibraries.get(cell.getLibrary());
        if (cvslib == null && state == State.ADDED) {
            // if state is added, CVSLibrary should be created
            addLibrary(cell.getLibrary(), true);
            cvslib = CVSLibraries.get(cell.getLibrary());
        }
        if (cvslib == null) return;
        if (state == null)
            return;
        cvslib.cellStates.put(cell, state);
    }

    /**
     * Set state of all Cells in a library
     * @param lib
     * @param state
     */
    public static void setState(Library lib, State state) {
        CVSLibrary cvslib = CVSLibraries.get(lib);
        if (cvslib == null && state == State.ADDED) {
            // if state is added, CVSLibrary should be created
            addLibrary(lib, true);
            cvslib = CVSLibraries.get(lib);
        }
        if (cvslib == null) return;
        if (state == null)
            return;
        if (cvslib.type != FileType.DELIB) {
            cvslib.libState = state;
            return;
        }
        for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
            Cell cell = it.next();
            State currentState = cvslib.cellStates.get(cell);
            // When cell is not in CVS and doing commit of library,
            // do not set state of unknown cells to NONE
            if (currentState == State.UNKNOWN) continue;
            cvslib.cellStates.put(cell, state);
        }
    }

    // ------------------ Color Mapping for Explorer Tree -----------------

    public static Color getColor(Library lib) {
        return getColor(getState(lib));
    }

    public static Color getColor(Cell cell) {
        return getColor(getState(cell));
    }

    static State getState(Library lib) {
        CVSLibrary cvslib = CVSLibraries.get(lib);
        if (cvslib == null) return State.UNKNOWN;
        if (cvslib.type != FileType.DELIB) {
            return cvslib.libState;
        }
        Set<State> states = new TreeSet<State>();
        for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
            Cell cell = it.next();
            State state = cvslib.cellStates.get(cell);
            if (state == null) continue;
            states.add(state);
        }
        Iterator<State> it = states.iterator();
        if (it.hasNext()) return it.next();
        return State.UNKNOWN;
    }

    static State getState(Cell cell) {
        CVSLibrary cvslib = CVSLibraries.get(cell.getLibrary());
        if (cvslib == null) return State.UNKNOWN;
        if (!CVS.isDELIB(cell.getLibrary())) {
            // return state for library
            return cvslib.libState;
        }
        State state = cvslib.cellStates.get(cell);
        if (state == null) return State.UNKNOWN;
        return state;
    }

    public static Color getColor(Cell.CellGroup cg) {
        Set<State> states = new TreeSet<State>();
        for (Iterator<Cell> it = cg.getCells(); it.hasNext(); ) {
            Cell cell = it.next();
            State state = getState(cell);
            states.add(state);
        }
        return getColor(states);
    }

    public static Color getColor(State state) {
        if (state == State.UPDATE) return Color.black;
        if (state == State.MODIFIED) return Color.blue;
        if (state == State.CONFLICT) return Color.red;
        if (state == State.ADDED) return Color.green;
        if (state == State.REMOVED) return Color.green;
        if (state == State.PATCHED) return Color.black;
        if (state == State.UNKNOWN) return Color.orange;
        return Color.black;
    }

    public static Color getColor(Set<State> states) {
        Iterator<State> it = states.iterator();
        if (it.hasNext()) return getColor(it.next());
        return Color.black;
    }

    // -------------------- Editing tracking ---------------------

    public static void setEditing(Cell cell, boolean editing) {
        CVSLibrary cvslib = CVSLibraries.get(cell.getLibrary());
        if (cvslib == null) return;
        if (editing) {
            cvslib.editing.put(cell, cell);
        } else {
            cvslib.editing.remove(cell);
        }
    }

    public static boolean isEditing(Cell cell) {
        CVSLibrary cvslib = CVSLibraries.get(cell.getLibrary());
        if (cvslib == null) return false;
        return cvslib.editing.containsKey(cell);
    }

    public static void setEditing(Library lib, boolean editing) {
        CVSLibrary cvslib = CVSLibraries.get(lib);
        if (cvslib == null) return;
        if (!CVS.isDELIB(lib))
            cvslib.libEditing = editing;
        else {
            if (editing = false)
                cvslib.editing.clear();
        }
    }

    public static boolean isEditing(Library lib) {
        CVSLibrary cvslib = CVSLibraries.get(lib);
        if (cvslib == null) return false;
        if (!CVS.isDELIB(lib))
            return cvslib.libEditing;
        else {
            return !cvslib.editing.isEmpty();
        }
    }

    /**
     * Method called when saving a library, BEFORE the library
     * file(s) are written.
     * @param lib
     */
    public static void savingLibrary(Library lib) {
        // When doing "save as", library type may change. Update library type here
        URL libFile = lib.getLibFile();
        if (libFile != null) {
            FileType type = OpenFile.getOpenFileType(libFile.getFile(), FileType.JELIB);
            CVSLibrary cvslib = CVSLibraries.get(lib);
            if (cvslib != null) {
                cvslib.type = type;
            }
        }

        if (!CVS.isDELIB(lib)) {
            File file = TextUtils.getFile(lib.getLibFile());
            if (!CVS.isFileInCVS(new File(lib.getLibFile().getPath()))) return;
            if (getState(lib) == State.ADDED) return;       // not actually in cvs yet
            if (!isEditing(lib)) {
                Edit.edit(file.getName(), file.getParentFile().getPath());
                setEditing(lib, true);
            }
            if (lib.isChanged()) {
                setState(lib, State.MODIFIED);
            }
            return;
        }
        // all modifies cells must have edit turned on
        List<Cell> modifiedCells = new ArrayList<Cell>();
        for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
            Cell cell = it.next();
            if (getState(cell) == State.ADDED) continue;        // not actually in cvs yet
            if (cell.isModified(true)) modifiedCells.add(cell);
        }
        // turn on edit for cells that are not being edited
        StringBuffer buf = new StringBuffer();
        for (Cell cell : modifiedCells) {
            if (!CVS.isFileInCVS(CVS.getCellFile(cell))) continue;
            // make sure state of cell is 'modified'
            setState(cell, State.MODIFIED);
            if (!isEditing(cell)) {
                buf.append(DELIB.getCellFile(cell.backup())+" ");
                setEditing(cell, true);
            }
        }
        if (buf.length() == 0) return;      // nothing to 'edit'
        // turn on edit for files to be modified
        // note that header is never to have edit on or off
        Edit.edit(buf.toString(), TextUtils.getFile(lib.getLibFile()).getPath());
    }

    /**
     * Method called when closing library.  Should be called
     * after library is closed.
     * @param lib
     */
    public static void closeLibrary(Library lib) {
        removeLibrary(lib);
    }

}
