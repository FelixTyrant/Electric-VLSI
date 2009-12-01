/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableNetSchem.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.network;

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellRevision;
import com.sun.electric.database.CellTree;
import com.sun.electric.database.EquivPorts;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableExport;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.ExportId;
import com.sun.electric.database.id.NodeProtoId;
import com.sun.electric.database.id.PortProtoId;
import com.sun.electric.database.id.PrimitiveNodeId;
import com.sun.electric.database.id.PrimitivePortId;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.technologies.Schematics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author dn146861
 */
class ImmutableNetSchem {
    private static final boolean DEBUG = false;

    private final Snapshot snapshot;
    private final TechPool techPool;
    private final Schematics schem;
    private final PrimitivePortId busPinPortId;
    private final ArcProto busArc;
    private final CellTree cellTree;
    private final CellBackup cellBackup;
    private final CellBackup.Memoization m;
    private final CellRevision cellRevision;
    private final CellId cellId;
    private final ImmutableArrayList<ImmutableExport> exports;
    private final ImmutableArrayList<ImmutableNodeInst> nodes;
    private final ImmutableArrayList<ImmutableArcInst> arcs;
    private final int numExports;
    private final int numNodes;
    private final int numArcs;

    private final int[] ni_pi;
    private final int arcsOffset;
    private final int[] tailConn;
    private final int[] headConn;
    private final int[] drawns;

    private final ArrayList<PortInst> stack = new ArrayList<PortInst>();
    private int numDrawns;
    private int numExportedDrawns;
    private int numConnectedDrawns;

    /** A map from canonic String to NetName. */
    private final HashMap<Name, GenMath.MutableInteger> netNames = new HashMap<Name, GenMath.MutableInteger>();
    /** Counter for enumerating NetNames. */
    private int netNameCount;
    /** Counter for enumerating NetNames. */
    private int exportedNetNameCount;

    /** */
    private final int[] portOffsets;
    /** Node offsets. */
    private final int[] drawnOffsets;
    /** */
    private final Name[] drawnNames;
    /** */
    private final int[] drawnWidths;
    /** Node offsets. */
    int[] nodeOffsets;

    /** */
    Global.Set globals;
    private int[] equivPortsN;
    private int[] equivPortsP;
    private int[] equivPortsA;

    /* Implementation of this NetSchem. */ ImmutableNetSchem implementation;
    /* Mapping from ports of this to ports of implementation. */ int[] portImplementation;

    /** Node offsets. */
    private Proxy[] nodeProxies;
    /** Proxies with global rebindes. */
    private Map<Proxy, Set<Global>> proxyExcludeGlobals;
    /** Map from names to proxies. Contains non-temporary names. */
    private final Map<Name, Proxy> name2proxy = new HashMap<Name, Proxy>();
    /** */
    int netNamesOffset;

    ImmutableNetSchem(Snapshot snapshot, CellId cellId, Map<CellId,ImmutableNetSchem> netSchems) {
        this.snapshot = snapshot;
        this.cellId = cellId;
        techPool = snapshot.techPool;
        schem = techPool.getSchematics();
        busPinPortId = schem != null ? schem.busPinNode.getPort(0).getId() : null;
        busArc = schem != null ? schem.bus_arc : null;
        cellTree = snapshot.getCellTree(cellId);
        cellBackup = cellTree.top;
        m = cellBackup.getMemoization();
        cellRevision = cellBackup.cellRevision;
        exports = cellRevision.exports;
        nodes = cellRevision.nodes;
        arcs = cellRevision.arcs;
        numExports = cellRevision.exports.size();
        numNodes = cellRevision.nodes.size();
        numArcs = cellRevision.arcs.size();

        // init connections
        ni_pi = new int[numNodes];
        int offset = numExports;
        for (int i = 0; i < numNodes; i++) {
            ImmutableNodeInst n = nodes.get(i);
            ni_pi[i] = offset;
            offset += getNumPorts(n.protoId);
        }
        arcsOffset = offset;
        offset += numArcs;

        headConn = new int[offset];
        tailConn = new int[offset];
        drawns = new int[offset];
        for (int i = numExports; i < arcsOffset; i++) {
            headConn[i] = i;
            tailConn[i] = i;
        }
        for (int i = 0; i < numExports; i++) {
            int portOffset = i;
            ImmutableExport export = exports.get(i);
            int orig = getPortInstOffset(export.originalNodeId, export.originalPortId);
            headConn[portOffset] = headConn[orig];
            headConn[orig] = portOffset;
            tailConn[portOffset] = -1;
        }
        for (int arcIndex = 0; arcIndex < numArcs; arcIndex++) {
            ImmutableArcInst a = arcs.get(arcIndex);
            int arcOffset = arcsOffset + arcIndex;
            int head = getPortInstOffset(a.headNodeId, a.headPortId);
            headConn[arcOffset] = headConn[head];
            headConn[head] = arcOffset;
            int tail = getPortInstOffset(a.tailNodeId, a.tailPortId);
            tailConn[arcOffset] = tailConn[tail];
            tailConn[tail] = arcOffset;
        }

        makeDrawns();

        initNetnames();

        portOffsets = new int[numExports + 1];
        drawnNames = new Name[numDrawns];
        drawnWidths = new int[numDrawns];
        drawnOffsets = new int[numDrawns];

        calcDrawnWidths();

        nodeOffsets = new int[numNodes];

        initNodables(netSchems);
        
        int mapSize = netNamesOffset + netNames.size();
        int[] netMapN = Netlist.initMap(mapSize);
        localConnections(netMapN, netSchems);
        
        int[] netMapP = netMapN.clone();
        int[] netMapA = netMapN.clone();
        internalConnections(netMapN, netMapP, netMapA, netSchems);

        Netlist.closureMap(netMapN);
        Netlist.closureMap(netMapP);
        Netlist.closureMap(netMapA);

        updatePortImplementation();

        updateInterface(netMapN, netMapP, netMapA);
    }

    private ImmutableNetSchem getSchem() {
        return implementation;
    }

    private void addToDrawn1(PortInst pi) {
        int piOffset = pi.getPortInstOffset();
        if (drawns[piOffset] >= 0) {
            return;
        }
        if (pi.portId instanceof PrimitivePortId && techPool.getPrimitivePort((PrimitivePortId)pi.portId).isIsolated()) {
            return;
        }
        drawns[piOffset] = numDrawns;
        if (DEBUG) {
            System.out.println(numDrawns + ": " + pi);
        }

        for (int k = piOffset; headConn[k] != piOffset;) {
            k = headConn[k];
            if (drawns[k] >= 0) {
                continue;
            }
            if (k < arcsOffset) {
                // This is port
                drawns[k] = numDrawns;
                if (DEBUG) {
                    System.out.println(numDrawns + ": " + exports.get(k));
                }
                continue;
            }
            ImmutableArcInst a = arcs.get(k - arcsOffset);
            ArcProto ap = techPool.getArcProto(a.protoId);
            if (ap.getFunction() == ArcProto.Function.NONELEC) {
                continue;
            }
            if (pi.portId == busPinPortId && ap != busArc) {
                continue;
            }
            drawns[k] = numDrawns;
            if (DEBUG) {
                System.out.println(numDrawns + ": " + a.name);
            }
            PortInst tpi = new PortInst(a, ImmutableArcInst.TAILEND);
            if (tpi.portId == busPinPortId && ap != busArc) {
                continue;
            }
            stack.add(tpi);
        }
        for (int k = piOffset; tailConn[k] != piOffset;) {
            k = tailConn[k];
            if (drawns[k] >= 0) {
                continue;
            }
            ImmutableArcInst a = arcs.get(k - arcsOffset);
            ArcProto ap = techPool.getArcProto(a.protoId);
            if (ap.getFunction() == ArcProto.Function.NONELEC) {
                continue;
            }
            if (pi.portId == busPinPortId && ap != busArc) {
                continue;
            }
            drawns[k] = numDrawns;
            if (DEBUG) {
                System.out.println(numDrawns + ": " + a.name);
            }
            PortInst hpi = new PortInst(a, ImmutableArcInst.HEADEND);
            if (hpi.portId == busPinPortId && ap != busArc) {
                continue;
            }
            stack.add(hpi);
        }
    }

    private void addToDrawn(PortInst pi) {
        assert stack.isEmpty();
        stack.add(pi);
        while (!stack.isEmpty()) {
            pi = stack.remove(stack.size() - 1);
            PortProtoId ppId = pi.portId;
            int numPorts = getNumPorts(ppId.getParentId());
            if (numPorts == 1 || ppId instanceof ExportId) {
                addToDrawn1(pi);
                continue;
            }
            PrimitivePort pp = techPool.getPrimitivePort((PrimitivePortId) ppId);
            PrimitiveNode pn = pp.getParent();
            int topology = pp.getTopology();
            for (int i = 0; i < numPorts; i++) {
                PrimitivePort pp2 = pn.getPort(i);
                if (pp2.getTopology() != topology) {
                    continue;
                }
                addToDrawn1(new PortInst(pi.n.nodeId, pp2.getId()));
            }
        }
    }

    void makeDrawns() {
        Arrays.fill(drawns, -1);
        numDrawns = 0;
        for (int i = 0; i < numExports; i++) {
            if (drawns[i] >= 0) {
                continue;
            }
            drawns[i] = numDrawns;
            ImmutableExport export = exports.get(i);
            addToDrawn(new PortInst(export));
            numDrawns++;
        }
        numExportedDrawns = numDrawns;
        for (int i = 0; i < numArcs; i++) {
            if (drawns[arcsOffset + i] >= 0) {
                continue;
            }
            ImmutableArcInst a = arcs.get(i);
            ArcProto ap = techPool.getArcProto(a.protoId);
            if (ap.getFunction() == ArcProto.Function.NONELEC) {
                continue;
            }
            drawns[arcsOffset + i] = numDrawns;
            if (DEBUG) {
                System.out.println(numDrawns + ": " + a.name);
            }
            PortInst hpi = new PortInst(a, ImmutableArcInst.HEADEND);
            if (hpi.portId != busPinPortId || ap == busArc) {
                addToDrawn(hpi);
            }
            PortInst tpi = new PortInst(a, ImmutableArcInst.TAILEND);
            if (tpi.portId != busPinPortId || ap == busArc) {
                addToDrawn(tpi);
            }
            numDrawns++;
        }
        numConnectedDrawns = numDrawns;
        for (int i = 0; i < numNodes; i++) {
//            ImmutableNodeInst n = nodes.get(i);
//            if (n.protoId instanceof CellId) {
//                if (ni.isIconOfParent())
//                    continue;
//            } else {
//                PrimitiveNode pn = techPool.getPrimitiveNode((PrimitiveNodeId) n.protoId);
//                if (pn.getFunction() == PrimitiveNode.Function.ART && pn != Generic.tech().simProbeNode
//                        || pn == Artwork.tech().pinNode
//                        || pn == Generic.tech().invisiblePinNode) {
//                    continue;
//                }
//            }
//            NodeProto np = ni.getProto();
//            int numPortInsts = np.getNumPorts();
//            for (int j = 0; j < numPortInsts; j++) {
//                PortInst pi = ni.getPortInst(j);
//                int piOffset = getPortInstOffset(pi);
//                if (drawns[piOffset] >= 0) {
//                    continue;
//                }
//                if (pi.getPortProto() instanceof PrimitivePort && ((PrimitivePort) pi.getPortProto()).isIsolated()) {
//                    continue;
//                }
//                addToDrawn(pi);
//                numDrawns++;
//            }
        }
    }

    private void initNetnames() {
        netNameCount = 0;
        for (ImmutableExport e: exports) {
            addNetNames(e.name);
        }
        exportedNetNameCount = netNameCount;
        for (ImmutableArcInst a: arcs) {
            ArcProto ap = techPool.getArcProto(a.protoId);
            if (ap.getFunction() == ArcProto.Function.NONELEC) {
                continue;
            }
            assert !a.name.isBus() || ap == busArc;
            if (a.isUsernamed()) {
                addNetNames(a.name);
            }
        }
        if (DEBUG) {
            for (Iterator<Map.Entry<Name, GenMath.MutableInteger>> it = netNames.entrySet().iterator(); it.hasNext();) {
                Map.Entry<Name, GenMath.MutableInteger> e = it.next();
                Name name = e.getKey();
                int index = e.getValue().intValue();
                System.out.println("NetName " + name + " " + index);
            }
        }
        assert netNameCount == netNames.size();
    }

    private void addNetNames(Name name) {
        for (int i = 0; i < name.busWidth(); i++) {
            addNetName(name.subname(i));
        }
    }

    private void addNetName(Name name) {
        GenMath.MutableInteger nn = netNames.get(name);
        if (nn == null) {
            nn = new GenMath.MutableInteger(-1);
            netNames.put(name, nn);
        }
        if (nn.intValue() < 0) {
            nn.setValue(netNameCount++);
        }
    }

    private void calcDrawnWidths() {
        Arrays.fill(drawnNames, null);
        Arrays.fill(drawnWidths, -1);

        for (int i = 0; i < numExports; i++) {
            int drawn = drawns[i];
            Name name = exports.get(i).name;
            int newWidth = name.busWidth();
            int oldWidth = drawnWidths[drawn];
            if (oldWidth < 0) {
                drawnNames[drawn] = name;
                drawnWidths[drawn] = newWidth;
                continue;
            }
            if (oldWidth != newWidth) {
                reportDrawnWidthError(/*cell.getPort(i), null,*/ drawnNames[drawn].toString(), name.toString());
                if (oldWidth < newWidth) {
                    drawnNames[drawn] = name;
                    drawnWidths[drawn] = newWidth;
                }
            }
        }
        for (int arcIndex = 0; arcIndex < numArcs; arcIndex++) {
            ImmutableArcInst a = arcs.get(arcIndex);
            int drawn = drawns[arcsOffset + arcIndex];
            if (drawn < 0) {
                continue;
            }
            Name name = a.name;
            if (name.isTempname()) {
                continue;
            }
            int newWidth = name.busWidth();
            int oldWidth = drawnWidths[drawn];
            if (oldWidth < 0) {
                drawnNames[drawn] = name;
                drawnWidths[drawn] = newWidth;
                continue;
            }
            if (oldWidth != newWidth) {
                reportDrawnWidthError(/*null, ai,*/ drawnNames[drawn].toString(), name.toString());
                if (oldWidth < newWidth) {
                    drawnNames[drawn] = name;
                    drawnWidths[drawn] = newWidth;
                }
            }
        }
        for (int arcIndex = 0; arcIndex < numArcs; arcIndex++) {
            int drawn = drawns[arcsOffset + arcIndex];
            if (drawn < 0) {
                continue;
            }
            ImmutableArcInst a = arcs.get(arcIndex);
            Name name = a.name;
            if (!name.isTempname()) {
                continue;
            }
            int oldWidth = drawnWidths[drawn];
            if (oldWidth < 0) {
                drawnNames[drawn] = name;
                if (a.protoId != busArc.getId()) {
                    drawnWidths[drawn] = 1;
                }
            }
        }
        for (int i = 0; i < numNodes; i++) {
            ImmutableNodeInst n = nodes.get(i);
//            NodeProto np = ni.getProto();
            if (n.protoId instanceof PrimitiveNodeId) {
                PrimitiveNode pn = techPool.getPrimitiveNode((PrimitiveNodeId)n.protoId);
                if (pn.getFunction().isPin()) {
                    continue;
                }
                if (pn == Schematics.tech().offpageNode) {
                    continue;
                }
            }
            int numPortInsts = getNumPorts(n.protoId);
            for (int j = 0; j < numPortInsts; j++) {
                PortInst pi = new PortInst(n.nodeId, getPortIdByIndex(n.protoId, j));
                int drawn = drawns[pi.getPortInstOffset()];
                if (drawn < 0) {
                    continue;
                }
                int oldWidth = drawnWidths[drawn];
                int newWidth = 1;
                if (n.protoId instanceof CellId) {
                    CellBackup subCell = snapshot.getCell((CellId)n.protoId);
                    CellId subCellId = subCell.cellRevision.d.cellId;
                    if (subCellId.isIcon() || subCellId.isSchematic()) {
                        int arraySize = subCellId.isIcon() ? n.name.busWidth() : 1;
                        int portWidth = subCell.cellRevision.exports.get(j).name.busWidth();
                        if (oldWidth == portWidth) {
                            continue;
                        }
                        newWidth = arraySize * portWidth;
                    }
                }
                if (oldWidth < 0) {
                    drawnWidths[drawn] = newWidth;
                    continue;
                }
                if (oldWidth != newWidth) {
                    String msg = "Network: Schematic " + cellId + " has net <"
                            + drawnNames[drawn] + "> with width conflict in connection " + pi.n.name + " " + pi.portId;
                    System.out.println(msg);
//                    networkManager.pushHighlight(pi);
//                    networkManager.logError(msg, NetworkTool.errorSortNetworks);
                }
            }
        }
        for (int i = 0; i < drawnWidths.length; i++) {
            if (drawnWidths[i] < 1) {
                drawnWidths[i] = 1;
            }
            if (DEBUG) {
                System.out.println("Drawn " + i + " " + (drawnNames[i] != null ? drawnNames[i].toString() : "") + " has width " + drawnWidths[i]);
            }
        }
    }

    // this method will not be called often because user will fix error, so it's not
    // very efficient.
    private void reportDrawnWidthError(/*Export pp, ArcInst ai,*/ String firstname, String badname) {
        String msg = "Network: Schematic " + cellId + " has net with conflict width of names <"
                + firstname + "> and <" + badname + ">";
        System.out.println(msg);
//
//        boolean originalFound = false;
//        for (int i = 0; i < numPorts; i++) {
//            String name = cell.getPort(i).getName();
//            if (name.equals(firstname)) {
//                networkManager.pushHighlight(cell.getPort(i));
//                originalFound = true;
//                break;
//            }
//        }
//        if (!originalFound) {
//            for (Iterator<ArcInst> it = cell.getArcs(); it.hasNext();) {
//                ArcInst oai = it.next();
//                String name = oai.getName();
//                if (name.equals(firstname)) {
//                    networkManager.pushHighlight(oai);
//                    break;
//                }
//            }
//        }
//        if (ai != null) {
//            networkManager.pushHighlight(ai);
//        }
//        if (pp != null) {
//            networkManager.pushHighlight(pp);
//        }
//        networkManager.logError(msg, NetworkTool.errorSortNetworks);
    }

    private void initNodables(Map<CellId,ImmutableNetSchem> netSchems) {
        Global.Buf globalBuf = new Global.Buf();
        int nodeProxiesOffset = 0;
        Map<ImmutableNodeInst, Set<Global>> nodeInstExcludeGlobal = null;
        for (int i = 0; i < numNodes; i++) {
            ImmutableNodeInst n = nodes.get(i);
//            NodeProto np = ni.getProto();
//            NetCell netCell = null;
//            if (ni.isCellInstance()) {
//                netCell = networkManager.getNetCell((Cell) np);
//            }
            if (n.protoId instanceof CellId && (((CellId)n.protoId).isIcon() || ((CellId)n.protoId).isSchematic())) {
                if (n.name.hasDuplicates()) {
                    String msg = cellId + ": Node name <" + n.name + "> has duplicate subnames";
                    System.out.println(msg);
//                    networkManager.pushHighlight(ni);
//                    networkManager.logError(msg, NetworkTool.errorSortNodes);
                }
                nodeOffsets[i] = ~nodeProxiesOffset;
                nodeProxiesOffset += n.name.busWidth();
            } else {
                if (n.name.isBus()) {
                    String msg = cellId + ": Array name <" + n.name + "> can be assigned only to icon nodes";
                    System.out.println(msg);
//                    networkManager.pushHighlight(ni);
//                    networkManager.logError(msg, NetworkTool.errorSortNodes);
                }
                nodeOffsets[i] = 0;
            }
            if (n.protoId instanceof CellId) {
                ImmutableNetSchem netCell = getNetCell((CellId)n.protoId, netSchems);
                ImmutableNetSchem sch = netCell.getSchem();
                if (sch != null && sch != this) {
                    Global.Set gs = sch.globals;

                    // Check for rebinding globals
                    int numPortInsts = sch.numExports;
                    Set<Global> gb = null;
                    for (int j = 0; j < numPortInsts; j++) {
                        PortInst pi = new PortInst(n.nodeId, getPortIdByIndex(n.protoId, j));
                        int piOffset = pi.getPortInstOffset();
                        int drawn = drawns[piOffset];
                        if (drawn < 0 || drawn >= numConnectedDrawns) {
                            continue;
                        }
                        int portIndex = netCell.portImplementation[j];
                        if (portIndex < 0) {
                            continue;
                        }
                        ImmutableExport e = sch.exports.get(portIndex);
                        if (!isGlobalPartition(e)) {
                            continue;
                        }
                        if (gb == null) {
                            gb = new HashSet<Global>();
                        }
                        for (int k = 0, busWidth = e.name.busWidth(); k < busWidth; k++) {
                            int q = sch.equivPortsN[sch.portOffsets[portIndex] + k];
                            for (int l = 0; l < sch.globals.size(); l++) {
                                if (sch.equivPortsN[l] == q) {
                                    Global g = sch.globals.get(l);
                                    gb.add(g);
                                }
                            }
                        }
                    }
                    if (gb != null) {
                        // remember excluded globals for this NodeInst
                        if (nodeInstExcludeGlobal == null) {
                            nodeInstExcludeGlobal = new HashMap<ImmutableNodeInst, Set<Global>>();
                        }
                        nodeInstExcludeGlobal.put(n, gb);
                        // fix Set of globals
                        gs = gs.remove(gb.iterator());
                    }

                    String errorMsg = globalBuf.addToBuf(gs);
                    if (errorMsg != null) {
                        String msg = "Network: " + cellId + " has globals with conflicting characteristic " + errorMsg;
                        System.out.println(msg);
//                        networkManager.logError(msg, NetworkTool.errorSortNetworks);
                        // TODO: what to highlight?
                        // log.addGeom(shared[i].nodeInst, true, 0, null);
                    }
                }
            } else {
                Global g = globalInst(n);
                if (g != null) {
                    PortCharacteristic characteristic;
                    if (g == Global.ground) {
                        characteristic = PortCharacteristic.GND;
                    } else if (g == Global.power) {
                        characteristic = PortCharacteristic.PWR;
                    } else {
                        characteristic = PortCharacteristic.findCharacteristic(n.techBits);
                        if (characteristic == null) {
                            String msg = "Network: " + cellId + " has global " + g.getName()
                                    + " with unknown characteristic bits";
                            System.out.println(msg);
//                            networkManager.pushHighlight(ni);
//                            networkManager.logError(msg, NetworkTool.errorSortNetworks);
                            characteristic = PortCharacteristic.UNKNOWN;
                        }
                    }
                    String errorMsg = globalBuf.addToBuf(g, characteristic);
                    if (errorMsg != null) {
                        String msg = "Network: " + cellId + " has global with conflicting characteristic " + errorMsg;
                        System.out.println(msg);
//                        networkManager.logError(msg, NetworkTool.errorSortNetworks);
                        //log.addGeom(shared[i].nodeInst, true, 0, null);
                    }
                }
            }
        }
        globals = globalBuf.getBuf();
//        boolean changed = false;
//        if (globals != newGlobals) {
//            changed = true;
//            globals = newGlobals;
//            if (NetworkTool.debug) {
//                System.out.println(cell + " has " + globals);
//            }
//        }
        int mapOffset = portOffsets[0] = globals.size();
        for (int i = 1; i <= numExports; i++) {
            ImmutableExport export = exports.get(i - 1);
            if (DEBUG) {
                System.out.println(export + " " + portOffsets[i - 1]);
            }
            mapOffset += export.name.busWidth();
//            if (portOffsets[i] != mapOffset) {
//                changed = true;
                portOffsets[i] = mapOffset;
//            }
        }
        equivPortsN = new int[mapOffset];
        equivPortsP = new int[mapOffset];
        equivPortsA = new int[mapOffset];

        for (int i = 0; i < numDrawns; i++) {
            drawnOffsets[i] = mapOffset;
            mapOffset += drawnWidths[i];
            if (NetworkTool.debug) {
                System.out.println("Drawn " + i + " has offset " + drawnOffsets[i]);
            }
        }
        if (nodeProxies == null || nodeProxies.length != nodeProxiesOffset) {
            nodeProxies = new Proxy[nodeProxiesOffset];
        }
        Arrays.fill(nodeProxies, null);
        name2proxy.clear();
        proxyExcludeGlobals = null;
        for (int nodeIndex = 0; nodeIndex < numNodes; nodeIndex++) {
            ImmutableNodeInst n = nodes.get(nodeIndex);
            int proxyOffset = nodeOffsets[nodeIndex];
            if (DEBUG) {
                System.out.println(n.name + " " + proxyOffset);
            }
            if (proxyOffset >= 0) {
                continue;
            }
            CellBackup iconCell = snapshot.getCell((CellId)n.protoId);
            ImmutableNetSchem netSchem = getNetCell((CellId)n.protoId, netSchems).getSchem();
            if (netSchem == null || isIconOfParent(n)) {
                continue;
            }
            Set<Global> gs = nodeInstExcludeGlobal != null ? nodeInstExcludeGlobal.get(n) : null; // exclude set of globals
            for (int i = 0; i < n.name.busWidth(); i++) {
                Proxy proxy = new Proxy(n, i);
                Name name = n.name.subname(i);
                if (!name.isTempname()) {
                    Proxy namedProxy = name2proxy.get(name);
                    if (namedProxy != null) {
//                        Cell namedIconCell = (Cell)namedProxy.nodeInst.getProto();
                        String msg = "Network: " + cellId + " has instances " + n.name + " and "
                                + namedProxy.n.name + " with same name <" + name + ">";
                        System.out.println(msg);
//                        networkManager.pushHighlight(ni);
//                        networkManager.pushHighlight(namedProxy.nodeInst);
//                        networkManager.logError(msg, NetworkTool.errorSortNodes);
                    }
                    name2proxy.put(name, proxy);
                }
                if (NetworkTool.debug) {
                    System.out.println(proxy + " " + mapOffset + " " + netSchem.equivPortsN.length);
                }
                proxy.nodeOffset = mapOffset;
                mapOffset += netSchem.equivPortsN.length;
                if (gs != null) {
                    if (proxyExcludeGlobals == null) {
                        proxyExcludeGlobals = new HashMap<Proxy, Set<Global>>();
                    }
                    Set<Global> gs0 = proxyExcludeGlobals.get(proxy);
                    if (gs0 != null) {
                        gs = new HashSet<Global>(gs);
                        gs.addAll(gs0);
                    }
                    proxyExcludeGlobals.put(proxy, gs);
                }
                nodeProxies[~proxyOffset + i] = proxy;
            }
        }
        netNamesOffset = mapOffset;
        if (DEBUG) {
            System.out.println("netNamesOffset=" + netNamesOffset);
        }
    }

    private Global globalInst(ImmutableNodeInst n) {
        if (!(n.protoId instanceof PrimitiveNodeId)) return null;
        PrimitiveNode pn = techPool.getPrimitiveNode((PrimitiveNodeId)n.protoId);
        if (pn == Schematics.tech().groundNode) {
            return Global.ground;
        }
        if (pn == Schematics.tech().powerNode) {
            return Global.power;
        }
        if (pn == Schematics.tech().globalNode) {
            String globalName = n.getVarValue(Schematics.SCHEM_GLOBAL_NAME, String.class);
            if (globalName != null) {
                return Global.newGlobal(globalName);
            }
        }
        return null;
    }

    private void localConnections(int netMap[], Map<CellId,ImmutableNetSchem> netSchems) {

        // Exports
        for (int k = 0; k < numExports; k++) {
            ImmutableExport e = exports.get(k);
            int portOffset = portOffsets[k];
            Name expNm = e.name;
            int busWidth = expNm.busWidth();
            int drawn = drawns[k];
            int drawnOffset = drawnOffsets[drawn];
            for (int i = 0; i < busWidth; i++) {
                Netlist.connectMap(netMap, portOffset + i, drawnOffset + (busWidth == drawnWidths[drawn] ? i : i % drawnWidths[drawn]));
                GenMath.MutableInteger nn = netNames.get(expNm.subname(i));
                Netlist.connectMap(netMap, portOffset + i, netNamesOffset + nn.intValue());
            }
        }

        // PortInsts
        for (int k = 0; k < numNodes; k++) {
            ImmutableNodeInst n = nodes.get(k);
            if (isIconOfParent(n)) {
                continue;
            }
//            NodeProto np = ni.getProto();
            if (n.protoId instanceof PrimitiveNodeId) {
                // Connect global primitives
                Global g = globalInst(n);
                if (g != null) {
                    int drawn = drawns[ni_pi[k]];
                    Netlist.connectMap(netMap, globals.indexOf(g), drawnOffsets[drawn]);
                }
                if (n.protoId == schem.wireConNode.getId()) {
                    connectWireCon(netMap, n);
                }
                continue;
            }
            if (nodeOffsets[k] >= 0) {
                continue;
            }

            CellId subCellId = (CellId)n.protoId;
            if (!(subCellId.isIcon() || subCellId.isSchematic())) {
                continue;
            }
            ImmutableNetSchem icon = getNetCell((CellId) n.protoId, netSchems);
            ImmutableNetSchem schem = icon.getSchem();
            if (schem == null) {
                continue;
            }
            Name nodeName = n.name;
            int arraySize = nodeName.busWidth();
            int proxyOffset = nodeOffsets[k];
            int numPorts = getNumPorts(n.protoId);
            CellBackup subCell = snapshot.getCell(subCellId);
            for (int m = 0; m < numPorts; m++) {
                ImmutableExport e = subCell.cellRevision.exports.get(m);
                int portIndex = m;
                portIndex = icon.portImplementation[portIndex];
                if (portIndex < 0) {
                    continue;
                }
                int portOffset = schem.portOffsets[portIndex];
                int busWidth = e.name.busWidth();
                int drawn = drawns[ni_pi[k] + m];
                if (drawn < 0) {
                    continue;
                }
                int width = drawnWidths[drawn];
                if (width != busWidth && width != busWidth * arraySize) {
                    continue;
                }
                for (int i = 0; i < arraySize; i++) {
                    Proxy proxy = nodeProxies[~proxyOffset + i];
                    if (proxy == null) {
                        continue;
                    }
                    int nodeOffset = proxy.nodeOffset + portOffset;
                    int busOffset = drawnOffsets[drawn];
                    if (width != busWidth) {
                        busOffset += busWidth * i;
                    }
                    for (int j = 0; j < busWidth; j++) {
                        Netlist.connectMap(netMap, busOffset + j, nodeOffset + j);
                    }
                }
            }
        }

        // Arcs
        for (int arcIndex = 0; arcIndex < numArcs; arcIndex++) {
            ImmutableArcInst a = arcs.get(arcIndex);
            int drawn = drawns[arcsOffset + arcIndex];
            if (drawn < 0) {
                continue;
            }
            if (!a.isUsernamed()) {
                continue;
            }
            int busWidth = drawnWidths[drawn];
            Name arcNm = a.name;
            if (arcNm.busWidth() != busWidth) {
                continue;
            }
            int drawnOffset = drawnOffsets[drawn];
            for (int i = 0; i < busWidth; i++) {
                GenMath.MutableInteger nn = netNames.get(arcNm.subname(i));
                Netlist.connectMap(netMap, drawnOffset + i, netNamesOffset + nn.intValue());
            }
        }

        // Globals of proxies
        for (int k = 0; k < nodeProxies.length; k++) {
            Proxy proxy = nodeProxies[k];
            if (proxy == null) {
                continue;
            }
            CellBackup subCell = proxy.getProto(netSchems);
            ImmutableNetSchem schem = getNetCell(subCell.cellRevision.d.cellId, netSchems);
            int numGlobals = schem.portOffsets[0];
            if (numGlobals == 0) {
                continue;
            }
            Set/*<Global>*/ excludeGlobals = null;
            if (proxyExcludeGlobals != null) {
                excludeGlobals = proxyExcludeGlobals.get(proxy);
            }
            for (int i = 0; i < numGlobals; i++) {
                Global g = schem.globals.get(i);
                if (excludeGlobals != null && excludeGlobals.contains(g)) {
                    continue;
                }
                Netlist.connectMap(netMap, this.globals.indexOf(g), proxy.nodeOffset + i);
            }
        }

        Netlist.closureMap(netMap);
//        HashMap<String, Name> canonicToName = new HashMap<String, Name>();
//        for (Map.Entry<Name, GenMath.MutableInteger> e : netNames.entrySet()) {
//            Name name = e.getKey();
//            int index = e.getValue().intValue();
//            assert index >= 0;
//            String canonicString = name.canonicString();
//            Name canonicName = canonicToName.get(canonicString);
//            if (canonicName == null) {
//                canonicName = name;
//                canonicToName.put(canonicString, canonicName);
//                continue;
//            }
//            int mapIndex0 = netNamesOffset + index;
//            int mapIndex1 = netNamesOffset + netNames.get(canonicName).intValue();
//            if (netMap[mapIndex0] != netMap[mapIndex1]) {
//                String msg = "Network: Schematic " + cell + " doesn't connect nets with names '" + name + "' and '" + canonicName + "'";
//                System.out.println(msg);
//                pushName(name);
//                pushName(canonicName);
//                networkManager.logWarning(msg, NetworkTool.errorSortNetworks);
////                Netlist.connectMap(netMap, mapIndex0, mapIndex1);
//            }
//        }
    }

    private void connectWireCon(int[] netMap, ImmutableNodeInst n) {
        ImmutableArcInst a1 = null;
        ImmutableArcInst a2 = null;
        for (ImmutableArcInst a: m.getConnections(null, n, null)) {
            if (a1 == null) {
                a1 = a;
            } else if (a2 == null) {
                a2 = a;
            } else {
                String msg = "Network: Schematic " + cellId + " has connector " + n.name
                        + " which merges more than two arcs";
                System.out.println(msg);
//                networkManager.pushHighlight(ni);
//                networkManager.logError(msg, NetworkTool.errorSortNetworks);
                return;
            }
        }
        if (a2 == null || a1 == a2) {
            return;
        }
        int large = getArcDrawn(a1);
        int small = getArcDrawn(a2);
        if (large < 0 || small < 0) {
            return;
        }
        if (drawnWidths[small] > drawnWidths[large]) {
            int temp = small;
            small = large;
            large = temp;
        }
        for (int i = 0; i < drawnWidths[large]; i++) {
            Netlist.connectMap(netMap, drawnOffsets[large] + i, drawnOffsets[small] + (i % drawnWidths[small]));
        }
    }

    private void internalConnections(int[] netMapF, int[] netMapP, int[] netMapA, Map<CellId,ImmutableNetSchem> netSchems) {
        for (int k = 0; k < numNodes; k++) {
            ImmutableNodeInst n = nodes.get(k);
            int nodeOffset = ni_pi[k];
//            NodeProto np = ni.getProto();
            if (n.protoId instanceof PrimitiveNodeId) {
                PrimitiveNode.Function fun = getFunction(n);
                if (fun == PrimitiveNode.Function.RESIST) {
                    Netlist.connectMap(netMapP, drawnOffsets[drawns[nodeOffset]], drawnOffsets[drawns[nodeOffset + 1]]);
                    Netlist.connectMap(netMapA, drawnOffsets[drawns[nodeOffset]], drawnOffsets[drawns[nodeOffset + 1]]);
                } else if (fun.isPolyOrWellResistor()) {
                    Netlist.connectMap(netMapA, drawnOffsets[drawns[nodeOffset]], drawnOffsets[drawns[nodeOffset + 1]]);
                }
                continue;
            }
//            NetCell netCell = networkManager.getNetCell((Cell) np);
            if (nodeOffsets[k] < 0) {
                continue;
            }
            CellId subCellId = (CellId)n.protoId;
            assert !subCellId.isIcon() && !subCellId.isSchematic();
            EquivPorts ep = snapshot.getCellTree(subCellId).getEquivPorts();
            int[] epN = ep.getEquivPortsN();
            int[] epP = ep.getEquivPortsP();
            int[] epA = ep.getEquivPortsA();
            for (int i = 0, numPorts = epN.length; i < numPorts; i++) {
                int di = drawns[nodeOffset + i];
                if (di < 0) {
                    continue;
                }
                int jN = epN[i];
                if (i != jN) {
                    int dj = drawns[nodeOffset + jN];
                    if (dj >= 0) {
                        Netlist.connectMap(netMapF, drawnOffsets[di], drawnOffsets[dj]);
                    }
                }
                int jP = epP[i];
                if (i != jP) {
                    int dj = drawns[nodeOffset + jP];
                    if (dj >= 0) {
                        Netlist.connectMap(netMapP, drawnOffsets[di], drawnOffsets[dj]);
                    }
                }
                int jA = epA[i];
                if (i != jA) {
                    int dj = drawns[nodeOffset + jA];
                    if (dj >= 0) {
                        Netlist.connectMap(netMapA, drawnOffsets[di], drawnOffsets[dj]);
                    }
                }
            }
        }
        for (int k = 0; k < nodeProxies.length; k++) {
            Proxy proxy = nodeProxies[k];
            if (proxy == null) {
                continue;
            }
            CellBackup subCell = proxy.getProto(netSchems);
            ImmutableNetSchem schem = getNetCell(subCell.cellRevision.d.cellId, netSchems);
            int[] eqF = schem.equivPortsN;
            int[] eqP = schem.equivPortsP;
            int[] eqA = schem.equivPortsA;
            for (int i = 0; i < eqF.length; i++) {
                int io = proxy.nodeOffset + i;

                int jF = eqF[i];
                if (i != jF) {
                    Netlist.connectMap(netMapF, io, proxy.nodeOffset + jF);
                }

                int jP = eqP[i];
                if (i != jP) {
                    Netlist.connectMap(netMapP, io, proxy.nodeOffset + jP);
                }

                int jA = eqA[i];
                if (i != jA) {
                    Netlist.connectMap(netMapA, io, proxy.nodeOffset + jA);
                }
            }
        }
        if (cellId.libId.libName.equals("spiceparts") && cellId.cellName.getName().equals("Ammeter") && cellId.isIcon()) {
            int mapOffset0 = drawnOffsets[drawns[0]];
            int mapOffset1 = drawnOffsets[drawns[1]];
            Netlist.connectMap(netMapP, mapOffset0, mapOffset1);
            Netlist.connectMap(netMapA, mapOffset0, mapOffset1);
        }
    }

    private void updatePortImplementation() {
        portImplementation = new int[numExports];
        CellRevision c = implementation.cellRevision;
        for (int i = 0; i < numExports; i++) {
            ImmutableExport e = exports.get(i);
            int equivIndex = -1;
            if (c != null) {
                ImmutableExport equiv = getEquivalentPort(c, e);
                if (equiv != null) {
                    equivIndex = c.getExportIndexByExportId(equiv.exportId);
                }
            }
            portImplementation[i] = equivIndex;
            if (equivIndex < 0) {
                String msg = cellId + ": Icon port <" + e.name + "> has no equivalent port";
                System.out.println(msg);
//                networkManager.pushHighlight(e);
//                networkManager.logError(msg, NetworkTool.errorSortPorts);
            }
        }
        if (c != null && numExports != c.exports.size()) {
            for (int i = 0; i < c.exports.size(); i++) {
                ImmutableExport e = c.exports.get(i);
                if (getEquivalentPort(cellRevision, e) == null) {
                    String msg = c + ": Schematic port <" + e.name + "> has no equivalent port in " + cellId;
                    System.out.println(msg);
//                    networkManager.pushHighlight(e);
//                    networkManager.logError(msg, NetworkTool.errorSortPorts);
                }
            }
        }
//        return changed;
    }

    /**
     * Update map of equivalent ports newEquivPort.
     */
    private void updateInterface(int[] netMapN, int[] netMapP, int[] netMapA) {
        for (int i = 0; i < equivPortsN.length; i++) {
            equivPortsN[i] = netMapN[i];
            equivPortsP[i] = netMapP[i];
            equivPortsA[i] = netMapA[i];
        }
    }

    /**
     * Returns true if this export has its original port on Global-Partition schematics
     * primitive.
     * @return true if this export is Global-Partition export.
     */
    private boolean isGlobalPartition(ImmutableExport e) {
        return e.originalPortId.parentId == schem.globalPartitionNode.getId();
    }

    /**
     * Method to tell whether this NodeInst is an icon of its parent.
     * Electric does not allow recursive circuit hierarchies (instances of Cells inside of themselves).
     * However, it does allow one exception: a schematic may contain its own icon for documentation purposes.
     * This method determines whether this NodeInst is such an icon.
     * @return true if this NodeInst is an icon of its parent.
     */
    private boolean isIconOfParent(ImmutableNodeInst n) {
        if (!(n.protoId instanceof CellId)) {
            return false;
        }
        CellBackup subCell = snapshot.getCell((CellId)n.protoId);
        return subCell.cellRevision.d.cellId.isIcon() && cellId.isSchematic() && subCell.cellRevision.d.groupName.equals(cellRevision.d.groupName);
    }

    /**
     * Method to find the Export on another Cell that is equivalent to this Export.
     * @param otherCell the other cell to equate.
     * @return the Export on that other Cell which matches this Export.
     * Returns null if none can be found.
     */
    private ImmutableExport getEquivalentPort(CellRevision otherCell, ImmutableExport e) {
        /* don't waste time searching if the two views are the same */
        if (cellRevision == otherCell) {
            return e;
        }

        // this is the non-cached way to do it
        return findExport(otherCell, e.name);
    }

    /**
     * Method to find the PortProto that has a particular Name.
     * @return the PortProto, or null if there is no PortProto with that name.
     */
    public ImmutableExport findExport(CellRevision cell, Name name) {
        int portIndex = searchExport(cell, name.toString());
        if (portIndex >= 0) {
            return cell.exports.get(portIndex);
        }
        return null;
    }

    /**
     * Searches the exports for the specified name using the binary
     * search algorithm.
     * @param name the name to be searched.
     * @return index of the search name, if it is contained in the exports;
     *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
     *	       <i>insertion point</i> is defined as the point at which the
     *	       Export would be inserted into the list: the index of the first
     *	       element greater than the name, or <tt>exports.length()</tt>, if all
     *	       elements in the list are less than the specified name.  Note
     *	       that this guarantees that the return value will be &gt;= 0 if
     *	       and only if the Export is found.
     */
    private int searchExport(CellRevision cellRevision, String name) {
        ImmutableArrayList<ImmutableExport> exports = cellRevision.exports;
        int low = 0;
        int high = exports.size() - 1;

        while (low <= high) {
            int mid = (low + high) >> 1;
            ImmutableExport e = exports.get(mid);
            int cmp = TextUtils.STRING_NUMBER_ORDER.compare(e.name.toString(), name);

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid; // Export found
            }
        }
        return -(low + 1);  // Export not found.
    }

    /**
     * Method to return the function of this NodeProto.
     * The Function is a technology-independent description of the behavior of this NodeProto.
     * @return function the function of this NodeProto.
     */
    public PrimitiveNode.Function getFunction(ImmutableNodeInst n) {
        if (n.protoId instanceof CellId) {
            return PrimitiveNode.Function.UNKNOWN;
        }

        PrimitiveNode np = techPool.getPrimitiveNode((PrimitiveNodeId)n.protoId);
        return np.getTechnology().getPrimitiveFunction(np, n.techBits);
    }

    private ImmutableNetSchem getNetCell(CellId cellId, Map<CellId,ImmutableNetSchem> netSchems) {
        return null;
    }

    private int getArcDrawn(ImmutableArcInst a) {
        int arcIndex = m.getArcIndex(a);
        return drawns[arcsOffset + arcIndex];
    }

    private int getPortInstOffset(int nodeId, PortProtoId portId) {
        return ni_pi[m.getNodeIndexByNodeId(nodeId)] + getPortIndex(portId);
    }
    
    private class PortInst {
        private final ImmutableNodeInst n;
        private final int nodeIndex;
        private final PortProtoId portId;
        private final int portIndex;
        
        private PortInst(int nodeId, PortProtoId portId) {
            nodeIndex = m.getNodeIndexByNodeId(nodeId);
            n = nodes.get(nodeIndex);
            this.portId = portId;
            portIndex = getPortIndex(portId);
        }
        
        private PortInst(ImmutableExport e) {
            this(e.originalNodeId, e.originalPortId);
        }
        
        private PortInst(ImmutableArcInst a, int end) {
            this(end != 0 ? a.headNodeId : a.tailNodeId, end != 0 ? a.headPortId : a.tailPortId);
        }
        
        private int getPortInstOffset() {
            return ni_pi[nodeIndex] + portIndex;
        }
    }

    private int getNumPorts(NodeProtoId nodeProtoId) {
        if (nodeProtoId instanceof CellId) {
            return snapshot.getCell((CellId)nodeProtoId).cellRevision.exports.size();
        } else {
            return techPool.getPrimitiveNode((PrimitiveNodeId)nodeProtoId).getNumPorts();
        }
    }

    private PortProtoId getPortIdByIndex(NodeProtoId nodeProtoId, int portIndex) {
        if (nodeProtoId instanceof CellId) {
            return snapshot.getCell((CellId)nodeProtoId).cellRevision.exports.get(portIndex).exportId;
        } else {
            return techPool.getPrimitiveNode((PrimitiveNodeId)nodeProtoId).getPort(portIndex).getId();
        }
    }

    private int getPortIndex(PortProtoId portId) {
        if (portId instanceof ExportId) {
            ExportId exportId = (ExportId)portId;
            return snapshot.getCell(exportId.getParentId()).cellRevision.getExportIndexByExportId(exportId);
        } else {
            return techPool.getPrimitivePort((PrimitivePortId)portId).getPortIndex();
        }
    }

    private class Proxy {

        private ImmutableNodeInst n;
        private int arrayIndex;
        int nodeOffset;

        private Proxy(ImmutableNodeInst n, int arrayIndex) {
            this.n = n;
            this.arrayIndex = arrayIndex;
        }

        /**
         * Method to return the prototype of this Nodable.
         * @return the prototype of this Nodable.
         */
        public CellBackup getProto(Map<CellId,ImmutableNetSchem> netSchems) {
            ImmutableNetSchem schem = getNetCell(((CellId)n.protoId), netSchems).getSchem();
            return schem.cellBackup;
        }

    }
}