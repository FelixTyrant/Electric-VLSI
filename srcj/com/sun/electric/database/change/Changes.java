/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Changes.java
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.tool.Tool;

import java.util.Collection;

/**
 * This interface defines changes that are made to the database.
 */
public interface Changes
{
	void init();
	void request(String cmd);
	void examineCell(Cell cell);
	void slice();

	void startBatch(Tool tool, boolean undoRedo);
	void endBatch();

	void modifyNodeInst(NodeInst ni, double oCX, double oCY, double oSX, double oSY, int oRot);
	void modifyNodeInsts(NodeInst [] nis, double [] oCX, double [] oCY, double [] oSX, double [] oSY, int [] oRot);
	void modifyArcInst(ArcInst ai, double oHX, double oHY, double oTX, double oTY, double oWid);
	void modifyExport(Export pp, PortInst oldPi);
	void modifyCell(Cell cell, double oLX, double oHX, double oLY, double oHY);
	void modifyTextDescript(ElectricObject obj, TextDescriptor descript, int oldDescript0, int oldDescript1);

	void newObject(ElectricObject obj);
	void killObject(ElectricObject obj);
	void killExport(Export pp, Collection oldPortInsts);
	void renameObject(ElectricObject obj, Name oldName);
	void redrawObject(ElectricObject obj);
	void newVariable(ElectricObject obj, Variable var);
	void killVariable(ElectricObject obj, Variable var);
	void modifyVariableFlags(ElectricObject obj, Variable var, int oldFlags);
	void modifyVariable(ElectricObject obj, Variable var, int index, Object oldValue);
	void insertVariable(ElectricObject obj, Variable var, int index);
	void deleteVariable(ElectricObject obj, Variable var, int index, Object oldValue);

	void readLibrary(Library lib);
	void eraseLibrary(Library lib);
	void writeLibrary(Library lib, boolean pass2);
}
