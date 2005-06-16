/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ScanChainXML.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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

package com.sun.electric.tool.io.output;

import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;

import java.util.*;
import java.io.*;

/**
 * Class to define XML for scan chains.
 */
public class ScanChainXML {

    private static final boolean DEBUG = false;
    private static final boolean FLAT = false;
    private static final boolean REDUCE = true;

    // --------------------------- Scan Chain Primitives -----------------------------

    /** Defines a Scan Chain Element, which contains one bit of scan storage */
    private static class ScanChainElement {
        public final String name;
        public final String access;
        public final String clears;
        public final String inport;
        public final String outport;

        private ScanChainElement(String name, String access, String clears, String inport, String outport) {
            this.name = name;
            this.access = access;
            this.clears = clears;
            this.inport = inport;
            this.outport = outport;
        }
    }

    /** Defines the Jtag controller from which the scan chains start and end */
    private static class JtagController {
        public final String name;
        public final int lengthIR;
        private List ports;

        protected static class Port {
            public final int opcode;
            public final String soutPort;
            public final String chainName;
            public Port(int opcode, String soutPort, String chainName) {
                this.opcode = opcode;
                this.soutPort = soutPort;
                this.chainName = chainName;
            }
        }

        private JtagController(String name, int lengthIR) {
            this.name = name;
            this.lengthIR = lengthIR;
            ports = new ArrayList();
        }

        /**
         * Add an opcode for a chain, and the associated scan out port,
         * for example addPort(1, "leaf1[0]")
         * @param opcode the opcode of the chain
         * @param soutPort the associated scan out port
         */
        private void addPort(int opcode, String soutPort, String chainName) {
            Port p = new Port(opcode, soutPort, chainName);
            ports.add(p);
        }
        /** get iterator over JtagContoller.Port list */
        private Iterator getPorts() { return ports.iterator(); }
    }

    /**
     * Define a pass through cell that passes scan data through it.  Examples
     * are inverters or buffers.
     */
    private static class PassThroughCell {
        public final String cellName;
        public final String inport;
        public final String outport;

        public PassThroughCell(String cellName, String inport, String outport) {
            this.cellName = cellName;
            this.inport = inport;
            this.outport = outport;
        }
    }

    // --------------------------------------------------------------------

    private String outputFile;
    private PrintWriter out;
    // objects used to parse cell schematics
    private JtagController jtagController;
    private HashMap scanChainElements;
    private HashMap passThroughCells;
    private HashMap cellsToFlatten;
    private String chipName;
    private Cell jtagCell;
    // list of parsed objects to write to XML
    private Map entities;                          // list of !ENTITY definitions
    private List chains;                            // list of top level chains
    private SubChain endChain;

    // ------------------------ Constructors ------------------------------

    /**
     * Create a new ScanChainXML object that will parse the schematics and
     * write out an XML description
     */
    public ScanChainXML() {
        this.jtagController = null;
        this.scanChainElements = new HashMap();
        this.passThroughCells = new HashMap();
        this.cellsToFlatten = new HashMap();
        this.chipName = "?";
        this.jtagCell = null;
        outputFile = null;
        out = new PrintWriter(System.out);
        entities = new HashMap();
        chains = new ArrayList();
    }

    // --------------------------- Settings --------------------------------

    /**
     * Specify a scan chain element.  When an instance of this is found, it is will
     * be parsed as one bit in the scan chain.
     * @param name name of the cell to be defined as a scan chain element.
     * @param access the access type: for example, "RW".
     * @param clears the clears type: for example, "L".
     * @param inport the name of input data port, typically "sin".
     * May contain index info, such as "s[1]"
     * @param outport the name of the output data port, typically "sout".
     * May contain index info, such as "ss[1]"
     */
    public void addScanChainElement(String name, String access, String clears, String inport, String outport) {
        ScanChainElement e = new ScanChainElement(name, access, clears, inport, outport);
        scanChainElements.put(name+"_"+inport, e);
    }

    /**
     * Specify a pass through element.  Pass through elements are found in series in
     * the scan chain, but are not scan chain elements themselves.  Examples of this are
     * inverters and buffers that buffer the scan chain data.
     * @param cellName name of the cell to be defined as a pass through element
     * @param inport the name of the input port that passes data through
     * May contain index info, such as "s[1]"
     * @param outport the name of the output port that passes data through
     * May contain index info, such as "ss[1]"
     */
    public void addPassThroughCell(String cellName, String inport, String outport) {
        PassThroughCell p = new PassThroughCell(cellName, inport, outport);
        passThroughCells.put(cellName+"_"+inport, p);
    }

    /**
     * Specify a cell to flatten.  The XML is hierarchical, but sometimes you don't need
     * or want all that hierarchy.  This specifies a cell that will be flattened
     * @param libName the library that contains the cell
     * @param cellName the name of the cell
     */
    public void addCellToFlatten(String libName, String cellName) {
        Library lib = Library.findLibrary(libName);
        if (lib == null) {
            System.out.println("Did not find library "+libName+" for flattening cell "+cellName);
            return;
        }
        Cell cell = lib.findNodeProto(cellName);
        if (cell == null) {
            System.out.println("Did not find cell "+cellName+" to flatten, in library "+libName);
            return;
        }
        cellsToFlatten.put(cell, cell);
    }

    /**
     * Specify the JTAG Controller.  All scan chains are assumed to start, and end, at the
     * JTAG Controller.  This specifies the jtag controller.
     * @param jtagLib the name of the library that holds the jtag controller cell
     * @param jtagCellName the name of the cell that is the jtag controller
     * @param lengthIR the number of instruction register bits in the jtag controller.
     */
    public void setJtagController(String jtagLib, String jtagCellName, int lengthIR) {
        Library lib = Library.findLibrary(jtagLib);
        if (lib == null) {
            System.out.println("Did not find jtag library "+jtagLib);
            return;
        }
        Cell cell = lib.findNodeProto(jtagCellName);
        if (cell == null) {
            System.out.println("Did not find jtag cell "+jtagCellName+" in library "+jtagLib);
            return;
        }
        jtagCell = cell;
        jtagController = new JtagController(jtagCellName, lengthIR);
        //PassThroughCell endCell = new PassThroughCell(jtagCellName, null, null);
        endChain = new SubChain("end:jtagController", -1, null, null);
    }

    /**
     * Add a port to the JTAG Controller that serves as a starting point for a scan chain.
     * A JTAG Controller may have several ports that each have a scan chain attached.
     * The JTAG Controller must have already been specified using setJtagController.
     * @param opcode the opcode for this scan chain
     * @param soutPortName the port name that outputs data for the scan chain.
     * May contain index info, such as "leaf1[1]"
     * @param chainName the name given to this scan chain
     */
    public void addJtagPort(int opcode, String soutPortName, String chainName) {
        if (jtagController == null) {
            System.out.println("Can't add port "+soutPortName+" because the jtag controller has not been defined yet");
            return;
        }
        jtagController.addPort(opcode, soutPortName, chainName);
    }

    /**
     * Specify the name of the chip. Only used when writing the chip name to the file.
     * @param name the chip name
     */
    public void setChipName(String name) { chipName = name; }

    /**
     * Set the output file.  This may be an absolute or relative path.  If this
     * option is not specified, the output goes to the Electric console.
     * @param file the name of the file.
     */
    public void setOutput(String file) {
        // try to open outputFile
        outputFile = file;
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
        } catch (IOException e) {
            System.out.println(e.getMessage() + "\nWriting XML to console");
        }
    }

    /**
     * Start tracing all the scan chains from the any instances of specified
     * jtag controller
     */
    public void start(String libName, String cellName) {
        Library lib = Library.findLibrary(libName);
        if (lib == null) {
            System.out.println("Did not find library "+libName+" for starting chain analysis in cell "+cellName);
            return;
        }
        Cell cell = lib.findNodeProto(cellName);
        if (cell == null) {
            System.out.println("Did not find cell "+cellName+" for starting chain analysis, in library "+libName);
            return;
        }
        int usages = 0;
        Netlist netlist = cell.getNetlist(true);
        for (Iterator it = netlist.getNodables(); it.hasNext(); ) {
            Nodable no = (Nodable)it.next();
            if (no.getProto().getName().equals(jtagCell.getName())) {
                System.out.println("*** Generating chains starting from jtag controller "+no.getName()+" in cell "+no.getParent().describe());
                start(no);
                usages++;
            }
        }
        if (usages == 0) System.out.println("Did not find any usages of the jtag controller: "+jtagCell.getName());
    }

    /**
     * Start tracing the scan chains. The nodeinst must be the jtag controller
     * @param no an instance of the jtag controller
     */
    private void start(Nodable no) {
        if (no == null) return;

        if (!no.getProto().getName().equals(jtagCell.getName())) {
            System.out.println("Node instance of cell "+no.getProto().getName() +" does not match jtag controller, "+jtagCell.getName());
            return;
        }
        // generate a chain for each port
        for (Iterator it = jtagController.getPorts(); it.hasNext(); ) {
            JtagController.Port jtagPort = (JtagController.Port)it.next();
            if (jtagPort.soutPort == null) continue;
            if (DEBUG) System.out.println("Starting chain "+jtagPort.opcode+" from port "+jtagPort.soutPort);
            Port startPort = getPort(no, jtagPort.soutPort);
            if (startPort == null) {
                System.out.println("Can't find specified start port "+jtagPort.soutPort+" on jtag controller "+jtagController.name);
                continue;
            }
            String chainName = jtagPort.chainName;
            if (chainName == null)
                chainName = "chain_"+jtagPort.soutPort;
            Chain chain = new Chain(chainName, jtagPort.opcode, -1, null, null);
            appendChain(chain, getOtherPorts(startPort));
            // report number of elements found
            int found = chain.numScanElements();
            System.out.println("Info: chain "+chainName+" had "+found+" scan chain elements");
            // make sure chain ended properly
            SubChain last = chain.getLastSubChain();
            if (last != endChain) {
                System.out.println("Error! Chain "+chainName+" did not end at the jtag controller. Possible error in parsing or schematics.");
                if (last != null) System.out.println("     Last sub chain is "+last.name+", length="+last.length);
            }
            chains.add(chain);
        }
        // post process
        postProcessEntitiesRemovePassThroughs();
        if (REDUCE)
            postProcessEntitiesPhase1();

        // write header
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("\n<!--");
        out.println("    Document      : "+outputFile);
        out.println("    Author        : automatically generated by Electric");
        out.println("    Description   : none");
        out.println("-->\n");
        out.println();
        out.println("<!DOCTYPE ChainG SYSTEM \"file:./ChainG.dtd\" [");
        // print all entities
        if (!FLAT) {
            for (Iterator it = entities.values().iterator(); it.hasNext(); ) {
                Entity ent = (Entity)it.next();
                ent.writeDefinition(out, new StringBuffer(), cellsToFlatten);
            }
            out.println("]>");
            out.println();
        }

        // print all chains
        StringBuffer indent = new StringBuffer();
        out.println("<ChainG>");
        indent.append("\t");
        out.println(indent+"<system>");
        indent.append("\t");
        out.println(indent+"<chip name=\""+chipName+"\" lengthIR=\""+jtagController.lengthIR+"\">");
        indent.append("\t");
        for (Iterator it = chains.iterator(); it.hasNext(); ) {
            Chain chain = (Chain)it.next();
            if (chain.numScanElements() == 0) continue;
            chain.write(out, indent, null, cellsToFlatten);
            if (DEBUG) System.out.println("Double Check: chain "+chain.name+" had "+chain.numScanElements()+" scan chain elements");
        }
        indent.setLength(indent.length() - 1);
        out.println(indent+"</chip>");
        indent.setLength(indent.length() - 1);
        out.println(indent+"</system>");
        indent.setLength(indent.length() - 1);
        out.println("</ChainG>");
        out.flush();
    }

    // ------------------------- Elements ----------------------------------------

    private static class SubChainInst {
        private Port inport;
        private Port outport;
        private Nodable no;
        private SubChain content;

        private SubChainInst(Port inport, Port outport, Nodable no, SubChain content) {
            this.inport = inport;
            this.outport = outport;
            this.no = no;
            this.content = content;
        }
        protected void setInport(Port port) { this.inport = port; }
        protected Port getInport() { return inport; }
        protected void setOutport(Port port) { this.outport = port; }
        protected Port getOutport() { return outport; }

        public String toString() {
            return "SubChainInst "+no.getName()+"; in: "+inport+", out: "+outport;
        }
        protected SubChain getSubChain() { return content; }
        protected String getName() { return no.getName(); }
    }

    /**
     * A chain is a description of an entire scan chain
     */
    private static class Chain {
        protected String name;
        private int opcode;                           // the opcode to control this chain
        protected int length;                         // not output if 0 or less
        protected String access;                      // not output if null
        protected String clears;                      // not output if null
        private List subchains;                       // list of ScanChainInstances

        private Chain(String name, int opcode, int length, String access, String clears) {
            this.name = name;
            this.opcode = opcode;
            this.length = length;
            this.access = access;
            this.clears = clears;
            subchains = new ArrayList();
        }

        protected void addSubChainInst(SubChainInst subChain) {
            subchains.add(subChain);
        }
        protected int getSubChainSize() { return subchains.size(); }
        protected SubChainInst getSubChainInst(int i) { return (SubChainInst)subchains.get(i); }
        protected SubChain getSubChain(int i) {
            SubChainInst inst = (SubChainInst)subchains.get(i);
            if (inst == null) return null;
            return inst.getSubChain();
        }
        protected Iterator getSubChainInsts() { return subchains.iterator(); }
        protected Iterator getSubChains() {
            List subs = new ArrayList();
            for (Iterator it = subchains.iterator(); it.hasNext(); ) {
                SubChainInst inst = (SubChainInst)it.next();
                subs.add(inst.getSubChain());
            }
            return subs.iterator();
        }
        protected int getLength() { return length; }
        protected String getAccess() { return access; }
        protected String getClears() { return clears; }

        protected void write(PrintWriter out, StringBuffer indent, String instName, Map cellsToFlatten) {
            if (numScanElements() == 0) return;   // nothing to print

            String n = (instName == null) ? name : instName;
            out.print(indent+"<"+getTag()+" name=\""+n+"\"");
            if (opcode > -1) out.print(" opcode=\""+Integer.toBinaryString(opcode)+"\"");
            if (length > 0) out.print(" length=\""+length+"\"");
            if (access != null) out.print(" access=\""+access+"\"");
            if (clears != null) out.print(" clears=\""+clears+"\"");

            // short hand if no elements
            if (subchains.size() == 0) {
                out.println(" />");
                return;
            }

            out.println(">");
            indent.append("\t");
            for (Iterator it = getSubChainInsts(); it.hasNext(); ) {
                SubChainInst inst = (SubChainInst)it.next();
                SubChain subChain = inst.getSubChain();
                subChain.write(out, indent, inst.getName(), cellsToFlatten);
            }
            indent.setLength(indent.length() - 1);
            out.println(indent+"</"+getTag()+">");
        }

        protected String getTag() { return "chain"; }

        protected int numScanElements() {
            int num = 0;
            if (length > 0) num += length;
            for (Iterator it = subchains.iterator(); it.hasNext(); ) {
                SubChainInst inst = (SubChainInst)it.next();
                SubChain sub = inst.getSubChain();
                num += sub.numScanElements();
            }
            //System.out.println("Num scan chain elements in "+name+": "+num+" (length="+length+")");
            return num;
        }

        /**
         * Get the last subchain in this chain
         * @return
         */
        protected SubChain getLastSubChain() {
            if (subchains.size() == 0) return null;
            SubChainInst last = (SubChainInst)subchains.get(subchains.size() - 1);
            return last.getSubChain().getLastSubChain();
        }

        protected void copyTo(Chain dest) {
            dest.name = this.name;
            dest.length = this.length;
            dest.access = this.access;
            dest.clears = this.clears;
            dest.subchains.clear();
            dest.subchains.addAll(this.subchains);
        }

        protected void removePassThroughs() {
            List toRemove = new ArrayList();
            for (Iterator it2 = getSubChainInsts(); it2.hasNext(); ) {
                SubChainInst inst = (SubChainInst)it2.next();
                SubChain sub = inst.getSubChain();
                if (sub.isPassThrough()) {
                    //System.out.println("Removed pass through cell "+sub.name);
                    toRemove.add(inst);
                }
            }
            for (Iterator it2 = toRemove.iterator(); it2.hasNext(); ) {
                subchains.remove(it2.next());
            }
        }
        protected void replaceSubChainInsts(List newSubChainInsts) {
            subchains.clear();
            subchains.addAll(newSubChainInsts);
        }
        protected void remove(SubChainInst inst) {
            subchains.remove(inst);
        }
        protected List getSubChainsInsts() {
            ArrayList copy = new ArrayList();
            for (Iterator it = getSubChainInsts(); it.hasNext(); ) copy.add(it.next());
            return copy;
        }
        protected void addAllSubChainInsts(int i, List list) {
            subchains.addAll(i, list);
        }
    }

    /**
     * A subchain is exactly like a chain, except that it must be part of
     * a chain, and cannot stand alone.
     */
    private static class SubChain extends Chain {
        private boolean passThrough;

        private SubChain(String name, int length, String access, String clears) {
            super(name, -1, length, access, clears);
            passThrough = false;
        }
        protected String getTag() { return "subchain"; }
        private void setPassThrough(boolean b) { passThrough = b; }
        private boolean isPassThrough() { return passThrough; }

        /**
         * Get the last subchain in this chain
         * @return
         */
        protected SubChain getLastSubChain() {
            if (getSubChainSize() == 0) {
                return this;
            }
            return super.getLastSubChain();
        }

        public Object clone() {
            SubChain sub = new SubChain(name, length, access, clears);
            this.copyTo(sub);
            sub.passThrough = passThrough;
            return sub;
        }
    }

    /**
     * An entity is a SubChain that associate with a cell, and contains
     * the scan chain elements for that cell.  This is unlike a subchain,
     * which may simply be a single scan element, or a group of scan elements.
     * Entities also serve as caching mechanisms, and define the chain for a cell.
     */
    private static class Entity extends SubChain {
        private static final String deftag = "!ENTITY";
        private Cell cell;
        private ExPort inExport;
        private ExPort outExport;

        private Entity(Cell cell) {
            super(cell.getName(), -1, null, null);
            this.cell = cell;
        }

        private void setInExPort(ExPort port) { this.inExport = port; }
        private ExPort getInExPort() { return inExport; }
        private void setOutExPort(ExPort port) { this.outExport = port; }
        private ExPort getOutExPort() { return outExport; }

        protected void writeDefinition(PrintWriter out, StringBuffer indent, Map cellsToFlatten) {
            if (FLAT) return;
            // don't write a definition
            if (numScanElements() < 3 || getSubChainSize() == 0) return;

            out.println(indent+"<"+deftag+" "+getKey()+" '");
            indent.append("\t");
            for (Iterator it = getSubChainInsts(); it.hasNext(); ) {
                SubChainInst inst = (SubChainInst)it.next();
                SubChain subChain = inst.getSubChain();
                subChain.write(out, indent, inst.getName(), cellsToFlatten);
            }
            indent.setLength(indent.length() - 1);
            out.println(indent+"'>");
        }
        protected void write(PrintWriter out, StringBuffer indent, String instName, Map cellsToFlatten) {
            if (FLAT) {
                super.write(out, indent, instName, cellsToFlatten);
                return;
            }

            if (numScanElements() < 3 || getSubChainSize() == 0) {
                super.write(out, indent, instName, cellsToFlatten);
            } else {
                //super.write(out, indent);
                out.println(indent+"<subchain name=\""+instName+"\"> &"+getKey()+"; </subchain>");
            }
        }

        private Object getKey() {
            String key = name+"_"+inExport.name.toString();
            key = key.replaceAll("[\\[\\]]", "_");
            return key;
        }

        public Object clone() {
            Entity ent = new Entity(cell);
            this.copyTo(ent);
            ent.inExport = this.inExport;
            ent.outExport = this.outExport;
            return ent;
        }
    }

    /**
     * A Port holds information about a single port, that may be part of a bussed portinst
     */
    private static class Port {
        private PortProto pp;
        private int index;
        private Name name;
        private Nodable no;

        private Port(Name name, Nodable no, PortProto pp, int index) {
            this.name = name;
            this.no = no;
            this.pp = pp;
            this.index = index;
        }
        public String toString() { if (name == null) return null; return no.getName() +":"+name.toString(); }
        public void print() {
            System.out.println("  Name: "+name);
            System.out.println("  No: "+no);
            System.out.println("  int: "+index);
            System.out.println("  pp: "+pp);
        }
    }

    private static class ExPort {
        private Export ex;
        private int index;
        private Name name;

        private ExPort(Name name, Export ex, int index) {
            this.name = name;
            this.ex = ex;
            this.index = index;
        }
        public String toString() { if (name == null) return null; return name.toString(); }
        public void print() {
            System.out.println("  Name: "+name);
            System.out.println("  Ex: "+ex);
            System.out.println("  int: "+index);
        }
    }

    // -------------------------------------------------------------------------

    private ScanChainElement getScanChainElement(String name, String sin) {
        ScanChainElement e = (ScanChainElement)scanChainElements.get(name+"_"+sin);
        return e;
    }

    private PassThroughCell getPassThroughCell(String name, String sin) {
        PassThroughCell p = (PassThroughCell)passThroughCells.get(name+"_"+sin);
        return p;
    }

    // -------------------------------------------------------------------------

    private SubChainInst getSubChain(Port inport) {
        Nodable no = inport.no;
        if (DEBUG) System.out.println("getSubChain for NodeInst: "+no.getName()+", sin: "+inport);
        NodeProto np = no.getProto();
        SubChainInst inst;

        // check if this is a pass through element
        PassThroughCell p = getPassThroughCell(np.getName(), inport.name.toString());
        if (p != null) {
            // find output port
            Port outport = getPort(no, p.outport);
            // make dummy subchain with no length
            SubChain sub = new SubChain(p.cellName, -1, null, null);
            sub.setPassThrough(true);
            inst = new SubChainInst(inport, outport, no, sub);
            if (DEBUG) System.out.println("  ...matched pass through cell "+p.cellName);
            return inst;
        }

        // check if this is a scan chain element
        if (np instanceof Cell) {
            ScanChainElement e = getScanChainElement(np.getName(), inport.name.toString());
            if (e != null) {
                SubChain sub = new SubChain(no.getName(), 1, e.access, e.clears);
                // find output port
                Port outport = getPort(no, e.outport);
                inst = new SubChainInst(inport, outport, no, sub);
                if (DEBUG) System.out.println("  ...matched scan chain element "+e.name);
                return inst;
            }

            // check if this is the jtag controller, which signals the end of the chain
            if (np.getName().equals(jtagCell.getName())) {
                if (DEBUG) System.out.println("  ...matched end of chain, port "+inport);
                inst = new SubChainInst(inport, null, no, endChain);
                return inst;
            }

            // otherwise, descend into cell and get subchain (entity) for it
            Cell sch = ((Cell)np).contentsView();
            if (sch == null) sch = (Cell)np;
            if (DEBUG) System.out.println("  ...descending into cell "+sch.describe());
            inst = getSubChain(sch, inport);
            return inst;
        }

        //System.out.println("Error! Scan chain terminated on node "+ni.getName()+" ("+ni.getParent().getName()+")");
        return null;
    }

    /**
     * Create the subchain for the cell
     * @param cell the cell
     * @param inport the scan data in port on the nodeinst for this cell
     * @return a new Entity that contains the scan chain for this cell
     */
    private SubChainInst getSubChain(Cell cell, Port inport) {
        // get the export corresponding to the nodeInst input port
        Nodable no = inport.no;
        Export inputEx = cell.findExport(inport.pp.getNameKey());
        ExPort schInPort = new ExPort(inport.name, inputEx, inport.index);

        // look up entity
        String key = cell.describe() + schInPort.name.toString();
        Entity ent = (Entity)entities.get(key);
        Port outport = null;

        // if not found, create it now
        if (ent == null) {
            // verify that sinport is part of cell
            if (inputEx == null) {
                System.out.println("Error! In cell "+cell.describe()+", scan data input Export "+inport.name+" not found.");
                return null;
            }
            ent = new Entity(cell);
            ent.setInExPort(schInPort);
            entities.put(key, ent);

            List nextPorts = getOtherPorts(schInPort);
            SubChainInst lastInst = appendChain(ent, nextPorts);
            if (lastInst != null) {
                // last inst is instance within cell, find export in cell on same network
                Port lastOutport = lastInst.getOutport();
                if (lastOutport != null) {
                    ExPort outEx = getExportedPort(lastOutport);
                    if (outEx == null) {
                        System.out.println("Error! In cell "+cell.describe()+", last element \""+lastOutport.no.getName()+"\", output port \""+lastOutport+"\""+
                                " does not connect to another scan element, is not exported from cell, and does not terminate at the JTAG Controller");
                    } else {
                        ent.setOutExPort(outEx);
                        // get output port on nodeinst corresponding to export
                        outport = getPort(no, outEx.name.toString());
                    }
                }
            }
            if (DEBUG) System.out.println("Completed Entity "+ent.name+".  inport: "+schInPort+", outport: "+ent.getOutExPort());
        } else {
            // definition found, just grab output port instance info
            ExPort outEx = ent.getOutExPort();
            if (outEx != null) {
                outport = getPort(no, outEx.name.toString());
            }
        }
        // wrap with instance info
        return new SubChainInst(inport, outport, no, ent);
    }


    /**
     * Append any scan chain elements to the chain.  Returns the number
     * of elements appended
     * @param chain
     * @param ports
     * @return the last instance added to the chain. May be null if nothing added
     */
    private SubChainInst appendChain(Chain chain, List ports) {
        ArrayList possibleChains = new ArrayList();
        ArrayList chainLastInstances = new ArrayList();

        // each port may or may not lead to a chain of scan chain elements
        for (Iterator it = ports.iterator(); it.hasNext(); ) {
            Port p = (Port)it.next();
            SubChainInst inst = getSubChain(p);
            if (inst == null) continue;         // possible dead end

            Chain tempChain = new Chain("temp", -1, -1, null, null);
            SubChain sub = inst.content;
            if (sub == endChain) {
                tempChain.addSubChainInst(inst);
                // store if found good chain
                if (tempChain.getSubChainSize() > 0) {
                    possibleChains.add(tempChain);
                    chainLastInstances.add(inst);
                }
                continue;          // end of chain
            }
            if (sub != null) {
                if (sub.getLength() > 0 || sub.getSubChainSize() > 0 || sub.isPassThrough()) {
                    // add to chain if has any content, or if it's a pass through cell
                    tempChain.addSubChainInst(inst);
                }
            }
            // Note: Here we don't care if sub is null. Only the instance info is
            // important from this point on to continue following the chain
            // continue along chain
            Port outport = inst.getOutport();
            if (outport == null) continue;
            SubChainInst last = inst;
            if (DEBUG) System.out.println("Setting last to "+last);
            List nextPorts = getOtherPorts(outport);
            SubChainInst appendLast = appendChain(tempChain, nextPorts);
            if (appendLast != null) {
                if (DEBUG) System.out.println("Replacing last with "+appendLast);
                last = appendLast;
            }
            // store if found good chain
            if (tempChain.getSubChainSize() > 0) {
                possibleChains.add(tempChain);
                chainLastInstances.add(last);
            }
        }
        // check if any chains found, or if too many found
        if (possibleChains.size() == 0) return null;
        if (possibleChains.size() > 1) {
            System.out.print("Error! Found more than one chain branching from port set: ");
            for (Iterator it = ports.iterator(); it.hasNext(); ) {
                Port p = (Port)it.next();
                System.out.print(p.name+", ");
            }
            System.out.println();
        }
        // append only chain, return last instance in chain
        Chain temp = (Chain)possibleChains.get(0);
        for (Iterator it = temp.getSubChainInsts(); it.hasNext(); ) {
            SubChainInst inst = (SubChainInst)it.next();
            chain.addSubChainInst(inst);
        }
        return (SubChainInst)chainLastInstances.get(0);
    }

    private void postProcessEntitiesRemovePassThroughs() {
        for (Iterator it = entities.values().iterator(); it.hasNext(); ) {
            Entity ent = (Entity)it.next();
            ent.removePassThroughs();
        }
    }

    private void postProcessEntitiesPhase1() {

        int reduced = 1;
        while (reduced > 0) {
            reduced = 0;
            for (Iterator it = entities.values().iterator(); it.hasNext(); ) {
                Entity ent = (Entity)it.next();
                // if lots of subchains with the same clears and access, consolidate them
                if (ent.getSubChainSize() > 1) {
                    SubChainInst consolidated = null;
                    List newList = new ArrayList();
                    // merge subchains with the same access and clears
                    for (Iterator it2 = ent.getSubChainInsts(); it2.hasNext(); ) {
                        SubChainInst inst = (SubChainInst)it2.next();
                        SubChain sub = inst.getSubChain();
                        if (sub.access == null || sub.clears == null ||
                            sub.length <= 0 || sub.getSubChainSize() > 0) {
                            // can't consolidate
                            consolidated = null;
                            newList.add(inst);
                        } else {
                            // consolidatable
                            if (consolidated != null &&
                                    consolidated.getSubChain().access.equals(sub.access) &&
                                    consolidated.getSubChain().clears.equals(sub.clears) && isMergable(inst)) {
                                // consolidate if they match
                                consolidated.getSubChain().length += sub.length;
                                consolidated.setOutport(inst.getOutport());
                                reduced++;
                            } else {
                                consolidated = new SubChainInst(inst.getInport(), inst.getOutport(), inst.no, (SubChain)inst.getSubChain().clone());
                                newList.add(consolidated);
                            }
                        }
                    }
                    ent.replaceSubChainInsts(newList);
                }
            }
            for (Iterator it = entities.values().iterator(); it.hasNext(); ) {
                Entity ent = (Entity)it.next();

                // if only one sub chain that contains no other sub chains, fold into this
                if (ent.length <= 0 && ent.getSubChainSize() == 1) {
                    SubChainInst inst = (SubChainInst)ent.getSubChainInsts().next();
                    SubChain sub = inst.getSubChain();
                    if (sub.length > 0 && sub.getSubChainSize() == 0 && isFlattenable(inst)) {
                        // fold into parent
                        ent.length = sub.length;
                        ent.access = sub.access;
                        ent.clears = sub.clears;
                        ent.remove(inst);
                        reduced++;
                    }
                }
                // flatten any specified sub chain cells
                if (ent.length <= 0) {
                    // see if any sub cells should be flattened
                    for (int i=0; i<ent.getSubChainSize(); i++) {
                        SubChain sub = ent.getSubChain(i);
                        SubChainInst inst = ent.getSubChainInst(i);
                        if (sub instanceof Entity) {
                            Entity subEnt = (Entity)sub;
                            if ((cellsToFlatten.get(subEnt.cell) != null) &&
                                    isFlattenable(inst) && sub.length < 0 && sub.getSubChainSize() > 0) {
                                // merge subchain's subchains into this entity
                                ent.remove(ent.getSubChainInst(i));
                                ent.addAllSubChainInsts(i, sub.getSubChainsInsts());
                                reduced++;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isMergable(SubChainInst inst) {
        if (!inst.getName().matches(".*?@.*")) return false;
        return true;
    }
    private boolean isFlattenable(SubChainInst inst) {
        if (!inst.getName().matches(".*?@.*")) return false;
        return true;
    }

    // -------------------------------------- Port Manipulation ---------------------------------

    private ExPort getExportedPort(Port port) {
        if (port == null) return null;

        Cell cell = port.no.getParent();

        // list of all portinsts on net
        Netlist netlist = cell.getNetlist(true);
        Network net = netlist.getNetwork(port.no, port.pp, port.index);

        for (Iterator it = cell.getPorts(); it.hasNext(); ) {
            Export ex = (Export)it.next();
            Name name = ex.getNameKey();
            for (int i=0; i<name.busWidth(); i++) {
                if (netlist.getNetwork(ex, i) == net)
                    return new ExPort(name.subname(i), ex, i);
            }
        }
        return null;
    }

    /**
     * Get a list of other Ports on the same network as the ExPort.
     * The returned list does not include the Port corresponding to the ExPort
     * @param inport the ExPort
     * @return a list of Port objects
     */
    private ArrayList getOtherPorts(ExPort inport) {
        // convert ExPort to Port
        PortInst pi = inport.ex.getOriginalPort();
        NodeInst ni = pi.getNodeInst();
        Netlist netlist = ni.getParent().getNetlist(true);
        Network net = netlist.getNetwork(inport.ex, inport.index);
        Port port = null;

        for (Iterator it = netlist.getNodables(); it.hasNext(); ) {
            Nodable no = (Nodable)it.next();
            if (no.getNodeInst() == ni) {
                if (net == netlist.getNetwork(no, pi.getPortProto(), inport.index)) {
                    port = new Port(inport.name, no, pi.getPortProto(), inport.index);
                    break;
                }
            }
        }
        if (port == null) return null;
        return getOtherPorts(port);
    }

    /**
     * Get a list of other Ports on the same network as inport.  List
     * does not include inport.
     * @param inport
     * @return a list of Port objects
     */
    private ArrayList getOtherPorts(Port inport) {
        if (inport == null) return null;
        ArrayList ports = new ArrayList();

        Cell cell = (Cell)inport.no.getParent();
        Netlist netlist = cell.getNetlist(true);
        Network net = netlist.getNetwork(inport.no, inport.pp, inport.index);

        for (Iterator it = netlist.getNodables(); it.hasNext(); ) {
            Nodable no = (Nodable)it.next();
            for (Iterator it2 = no.getProto().getPorts(); it2.hasNext(); ) {
                PortProto pp = (PortProto)it2.next();

                Name name = pp.getNameKey();
                for (int i=0; i<name.busWidth(); i++) {
                    if ((no == inport.no) && (pp == inport.pp) && (i == inport.index))
                        continue;
                    if (netlist.getNetwork(no, pp, i) == net) {
                        // found matching port
                        Name subname = name;
                        if (name.busWidth() > 1) subname = name.subname(i);
                        Port p = new Port(subname, no, pp, i);
                        ports.add(p);
                    }
                }
            }
        }

        if (ports.size() == 0) {
            System.out.println("Error, no other ports connected to "+inport.name);
        }
        return ports;
    }

    /**
     * Get a port from a port name on a Nodable
     * @param no the nodable
     * @param portName the port name including bus index, such as foo[1][2]
     * @return a Port, or null if none found
     */
    private Port getPort(Nodable no, String portName) {
        for (Iterator it = no.getProto().getPorts(); it.hasNext(); ) {
            PortProto pp = (PortProto)it.next();
            Name name = pp.getNameKey();
            for (int i=0; i<name.busWidth(); i++) {
                Name subname = name.subname(i);
                if (subname.toString().equals(portName)) {
                    return new Port(subname, no, pp, i);
                }
            }
        }
        System.out.println("Could not find "+portName+" on "+no.getName());
        return null;
    }

}
