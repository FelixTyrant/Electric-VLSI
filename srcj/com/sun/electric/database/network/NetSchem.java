/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NetSchem.java
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
package com.sun.electric.database.network;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.NodeUsage;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.technologies.Schematics;

// import java.util.Arrays;
import java.util.ArrayList;
// import java.util.HashMap;
import java.util.Iterator;
// import java.util.Map;

/**
 * This is the mirror of group of Icon and Schematic cells in Network tool.
 */
class NetSchem extends NetCell {
	class Icon {
		Cell iconCell;
		Export[] dbIconExports;
		int[] iconToSchem;

		Icon(Cell iconCell) {
			this.iconCell = iconCell;
		}

		void invalidateUsagesOf(boolean strong) { NetCell.invalidateUsagesOf(iconCell, strong); }

		private void updateInterface() {
			boolean changed = false;
			if (dbIconExports == null || dbIconExports.length != iconCell.getNumPorts()) {
				changed = true;
				dbIconExports = new Export[iconCell.getNumPorts()];
				iconToSchem = new int[iconCell.getNumPorts()];
			}
			for (Iterator it = iconCell.getPorts(); it.hasNext();) {
				Export e = (Export)it.next();
				if (dbIconExports[e.getIndex()] != e) {
					changed = true;
					dbIconExports[e.getIndex()] = e;
				}
				int portInd = -1;
				if (cell != null) {
					Export equiv = e.getEquivalentPort(cell);
					if (equiv != null) portInd = equiv.getIndex();
				}
				if (iconToSchem[e.getIndex()] != portInd) {
					changed = true;
					iconToSchem[e.getIndex()] = portInd;
				}
			}
			if (changed)
				invalidateUsagesOf(true);
		}
	}

	private class Proxy {
		NodeInst nodeInst;
		int arrayIndex;
		int nodeOffset;

		Proxy(NodeInst nodeInst, int arrayIndex) {
			this.nodeInst = nodeInst;
			this.arrayIndex = arrayIndex;
		}
	
		/**
		 * Routine to return the prototype of this Nodable.
		 * @return the prototype of this Nodable.
		 */
		public NodeProto getProto() {
			NodeProto np = nodeInst.getProto();
			if (np instanceof Cell) {
				np = Network.getNetCell((Cell)np).cell;
			}
			return np;
		}

		/**
		 * Routine to return the Cell that contains this Nodable.
		 * @return the Cell that contains this Nodable.
		 */
		public Cell getParent() { return cell; }

		/**
		 * Routine to return the name of this Nodable.
		 * @return the name of this Nodable.
		 */
		public String getName() { return getNameKey().toString(); }

		/**
		 * Routine to return the name key of this Nodable.
		 * @return the name key of this Nodable.
		 */
		public Name getNameKey() { return nodeInst.getNameKey().subname(arrayIndex); }

		/**
		 * Routine to return the Variable on this Nodable with a given name.
		 * @param name the name of the Variable.
		 * @return the Variable with that name, or null if there is no such Variable.
		 */
		public Variable getVar(String name) { return nodeInst.getVar(name); }

		/**
		 * Routine to get network by PortProto and bus index.
		 * @param portProto PortProto in protoType.
		 * @param busIndex index in bus.
		 */
		public JNetwork getNetwork(PortProto portProto, int busIndex) {
			return Network.getNetwork(nodeInst, arrayIndex, portProto, busIndex);
		}

		/**
		 * Returns a printable version of this Nodable.
		 * @return a printable version of this Nodable.
		 */
		public String toString() { return "NetSchem.Proxy " + getName(); }

	}

	/** */															private Icon[] icons;
	/** Node offsets. */											Proxy[] nodeProxies;
	/** */															Global.Set globals = Global.Set.empty;
	/** */															int netNamesOffset;

	NetSchem(Cell cell) {
		super(cell.isIcon() ? cell.getCellGroup().getMainSchematics() : cell);
		if (cell.isIcon() || cell.getCellGroup().getMainSchematics() == cell)
			initIcons(cell.getCellGroup());
		else
			icons = new Icon[0];
	}

	NetSchem(Cell.CellGroup cellGroup) {
		super(cellGroup.getMainSchematics());
		initIcons(cellGroup);
	}

	private void initIcons(Cell.CellGroup cellGroup) {
		int iconCount = 0;
		for (Iterator it = cellGroup.getCells(); it.hasNext();) {
			Cell c = (Cell)it.next();
			if (c.isIcon())
				iconCount++;
		}
		icons = new Icon[iconCount];
		iconCount = 0;
		for (Iterator it = cellGroup.getCells(); it.hasNext();) {
			Cell c = (Cell)it.next();
			if (c.isIcon()) {
				icons[iconCount] = new Icon(c);
				Network.setCell(c, this, ~iconCount);
				iconCount++;
			}
		}
	}


// 		int ind = cellsStart + c.getIndex();
// 		int numPorts = c.getNumPorts();
// 		int[] beg = new int[numPorts + 1];
// 		for (int i = 0; i < beg.length; i++) beg[i] = 0;
// 		for (Iterator pit = c.getPorts(); pit.hasNext(); )
// 		{
// 			Export pp = (Export)pit.next();
// 			beg[pp.getIndex()] = pp.getProtoNameLow().busWidth();
// 		}
// 		int b = 0;
// 		for (int i = 0; i < numPorts; i++)
// 		{
// 			int w = beg[i];
// 			beg[i] = b;
// 			b = b + w;
// 		}
// 		beg[numPorts] = b;
//		protoPortBeg[ind] = beg;

	int getPortOffset(int iconIndex, int portIndex, int busIndex) {
		int portInd = icons[~iconIndex].iconToSchem[portIndex];
		if (portInd < 0) return -1;
		return portInd + busIndex;
	}

	/**
	 * Get an iterator over all of the Nodables of this Cell.
	 */
	Iterator getNodables()
	{
		if ((flags & VALID) == 0) redoNetworks();
		ArrayList nodables = new ArrayList();
		for (Iterator it = cell.getNodes(); it.hasNext();) {
			NodeInst ni = (NodeInst)it.next();
			if (nodeOffsets[ni.getIndex()] < 0) continue;
			nodables.add(ni);
		}
		for (int i = 0; i < nodeProxies.length; i++) {
			if (nodeProxies[i] != null)
				nodables.add(nodeProxies[i]);
		}
		return nodables.iterator();
	}

	/*
	 * Get network by index in networks maps.
	 */
	JNetwork getNetwork(Nodable no, int arrayIndex, PortProto portProto, int busIndex) {
		if ((flags & VALID) == 0) redoNetworks();
		int portOffset = Network.getPortOffset(portProto, busIndex);
		if (portOffset < 0) return null;
		int nodeOffset;
		if (no instanceof Proxy) {
			Proxy proxy = (Proxy)no;
			nodeOffset = proxy.nodeOffset;
		} else {
			NodeInst ni = (NodeInst)no;
			nodeOffset = nodeOffsets[ni.getIndex()];
			if (nodeOffset < 0) {
				if (arrayIndex < 0 || arrayIndex >= ni.getNameKey().busWidth())
					return null;
				Proxy proxy = nodeProxies[~nodeOffset + arrayIndex];
				if (proxy == null) return null;
				nodeOffset = proxy.nodeOffset;
			}
		}
		return networks[netMap[nodeOffset + portOffset]];
	}

	void invalidateUsagesOf(boolean strong)
	{
		if (cell != null)
			super.invalidateUsagesOf(strong);	
		for (int i = 0; i < icons.length; i++)
			icons[i].invalidateUsagesOf(strong);
	}

	private boolean initNodables() {
		if (nodeOffsets == null || nodeOffsets.length != cell.getNumNodes())
			nodeOffsets = new int[cell.getNumNodes()];
		int nodeOffset = cell.getNumPorts();
		Global.clearBuf();
		int nodeProxiesOffset = 0;
		for (Iterator it = cell.getNodes(); it.hasNext();) {
			NodeInst ni = (NodeInst)it.next();
			int nodeIndex = ni.getIndex();
			NodeProto np = ni.getProto();
			if (np.isIcon()) {
				nodeOffsets[nodeIndex] = ~nodeProxiesOffset;
				nodeProxiesOffset += ni.getNameKey().busWidth();
			} else {
				if (Network.debug) System.out.println(ni+" "+nodeOffset);
				nodeOffsets[nodeIndex] = nodeOffset;
				nodeOffset += np.getNumPorts();
			}
			if (isSchem(np)) {
				NetSchem sch = (NetSchem)Network.getNetCell((Cell)np);
				Global.addToBuf(sch.globals);
			} else {
				Global g = getGlobal(ni);
				if (g != null) Global.addToBuf(g);
			}
		}
		Global.Set newGlobals = Global.getBuf();
		boolean changed = false;
		if (globals != newGlobals) {
			changed = true;
			globals = newGlobals;
			if (Network.debug) System.out.println(cell+" has "+globals);
		}
		if (nodeProxies == null || nodeProxies.length != nodeProxiesOffset)
			nodeProxies = new Proxy[nodeProxiesOffset];
		for (Iterator it = cell.getNodes(); it.hasNext();) {
			NodeInst ni = (NodeInst)it.next();
			int proxyOffset = nodeOffsets[ni.getIndex()];
			if (proxyOffset >= 0) continue;
			NetCell netCell = Network.getNetCell((Cell)ni.getProto());
			for (int i = 0; i < ni.getNameKey().busWidth(); i++) {
				Proxy proxy = null;
				if (netCell != this && netCell.cell != null) {
					proxy = new Proxy(ni, i);
					if (Network.debug) System.out.println(proxy+" "+nodeOffset);
					proxy.nodeOffset = nodeOffset;
					nodeOffset += proxy.getProto().getNumPorts();
				}
				nodeProxies[~proxyOffset + i] = proxy;
			}
		}
		netNamesOffset = nodeOffset;
		return changed;
	}

	private static Global getGlobal(NodeInst ni) {
		NodeProto np = ni.getProto();
		if (np == Schematics.tech.groundNode) return Global.ground;
		if (np == Schematics.tech.powerNode) return Global.power;
		if (np == Schematics.tech.globalNode) {
			Variable var = ni.getVar(Schematics.SCHEM_GLOBAL_NAME, String.class);
			if (var != null) return Global.newGlobal((String)var.getObject());
		}
		return null;
	}

	void addNetNames(Name name) {
		addNetName(name);
// 		for (int i = 0; i < name.busWidth(); i++)
// 			addNetName(name.subname(i));
	}

	void mergeInternallyConnected()
	{
		for (Iterator it = cell.getNodes(); it.hasNext();) {
			NodeInst ni = (NodeInst)it.next();
			int nodeOffset = nodeOffsets[ni.getIndex()];
			if (nodeOffset < 0) continue;
			mergeInternallyConnected(nodeOffset, ni);
		}
		for (int i = 0; i < nodeProxies.length; i++) {
			Proxy proxy = nodeProxies[i];
			if (proxy == null) continue;
			mergeInternallyConnected(proxy.nodeOffset, proxy.nodeInst);
		}
	}

	private void mergeInternallyConnected(int nodeOffset, NodeInst ni) {
		int[] eq = Network.getEquivPorts(ni.getProto());
		for (int i = 0; i < eq.length; i++) {
			if (eq[i] == i) continue;
			connectMap(nodeOffset + i, nodeOffset + eq[i]);
		}
	}

	private void mergeNetsConnectedByArcs()
	{
		for (Iterator it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst) it.next();
			if (ai.getProto().getFunction() == ArcProto.Function.NONELEC) continue;

			int ind = -1;
			if (ai.isUsernamed()) {
				Name arcNm = ai.getNameKey();
				NetName nn = (NetName)netNames.get(arcNm);
				ind = netNamesOffset + nn.index;
			}
			ind = connectPortInst(ind, ai.getHead().getPortInst());
			ind = connectPortInst(ind, ai.getTail().getPortInst());
		}
	}

	private void addExportNamesToNets()
	{
		for (Iterator it = cell.getPorts(); it.hasNext();)
		{
			Export e = (Export) it.next();
			int ind = e.getIndex();
			Name expNm = e.getProtoNameKey();
			NetName nn = (NetName)netNames.get(expNm);
			connectMap(ind, netNamesOffset + nn.index);
			connectPortInst(ind, e.getOriginalPort());
		}
	}

	private int connectPortInst(int oldInd, PortInst pi) {
		int portOffset = Network.getPortOffset(pi.getPortProto(), 0);
		if (portOffset < 0) return oldInd;
		NodeInst ni = pi.getNodeInst();
		int nodeOffset = nodeOffsets[ni.getIndex()];
		if (nodeOffset >= 0) {
			int ind = nodeOffset + portOffset;
			if (oldInd < 0)
				oldInd = ind;
			else
				connectMap(oldInd, ind);
		} else {
			for (int i = 0; i < ni.getNameKey().busWidth(); i++) {
				Proxy proxy = nodeProxies[~nodeOffset + i];
				if (proxy == null) continue;
				nodeOffset = proxy.nodeOffset;
				int ind = nodeOffset + portOffset;
				if (oldInd < 0)
					oldInd = ind;
				else
					connectMap(oldInd, ind);
			}
		}
		return oldInd;
	}

	void buildNetworkList()
	{
		if (networks == null || networks.length != netMap.length)
			networks = new JNetwork[netMap.length];
		for (int i = 0; i < netMap.length; i++)
		{
			networks[i] = (netMap[i] == i ? new JNetwork(cell) : null);
		}
		for (Iterator it = netNames.values().iterator(); it.hasNext(); )
		{
			NetName nn = (NetName)it.next();
			if (nn.index < 0) continue;
			networks[netMap[netNamesOffset + nn.index]].addName(nn.name.toString());
		}
		/*
		// debug info
		System.out.println("BuildNetworkList "+this);
		int i = 0;
		for (Iterator nit = getNetworks(); nit.hasNext(); )
		{
			JNetwork network = (JNetwork)nit.next();
			String s = "";
			for (Iterator sit = network.getNames(); sit.hasNext(); )
			{
				String n = (String)sit.next();
				s += "/"+ n;
			}
			for (Iterator pit = network.getPorts(); pit.hasNext(); )
			{
				PortInst pi = (PortInst)pit.next();
				s += "|"+pi.getNodeInst().getProto()+"&"+pi.getPortProto().getProtoName();
			}
			System.out.println("    "+i+"    "+s);
			i++;
		}
		*/
	}

	void updateInterface(boolean changed) {
		if (cell != null)
			super.updateInterface(changed);
		for (int i = 0; i < icons.length; i++) {
			icons[i].updateInterface();
		}
	}

	void redoNetworks()
	{
		if ((flags & VALID) != 0) return;

		if (cell == null) {
			updateInterface(false);
			flags |= (LOCALVALID|VALID);
			return;
		}

		// redo descendents
		for (Iterator it = cell.getUsagesIn(); it.hasNext();)
		{
			NodeUsage nu = (NodeUsage) it.next();
			if (nu.isIconOfParent()) continue;

			NodeProto np = nu.getProto();
			int npInd = np.getIndex();
			if (npInd < 0) continue;
			NetCell netCell = Network.getNetCell((Cell)np);
			if ((netCell.flags & VALID) == 0)
				netCell.redoNetworks();
		}

		if ((flags & LOCALVALID) != 0)
		{
			flags |= VALID;
			return;
		}

		/* Set index of NodeInsts */
		boolean changed = initNodables();

		// Gather port and arc names
		int mapSize = netNamesOffset + initNetnames();

		if (netMap == null || netMap.length != mapSize)
			netMap = new int[mapSize];
		for (int i = 0; i < netMap.length; i++) netMap[i] = i;

		if (Network.debug) System.out.println("mergeInternallyConnected");
		mergeInternallyConnected();
		if (Network.debug) System.out.println("mergeNetsConnectedByArcs");
		mergeNetsConnectedByArcs();
		if (Network.debug) System.out.println("addExportNamesToNets");
		addExportNamesToNets();
		closureMap();
		buildNetworkList();
		updateInterface(changed);
		flags |= (LOCALVALID|VALID);
	}
}

