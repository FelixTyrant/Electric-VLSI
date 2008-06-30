/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TechPalette.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.ui;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.AnnularRing;
import com.sun.electric.tool.user.dialogs.CellBrowser;
import com.sun.electric.tool.user.dialogs.LayoutText;
import com.sun.electric.tool.user.menus.CellMenu;
import com.sun.electric.tool.user.redisplay.AbstractDrawing;
import com.sun.electric.tool.user.redisplay.PixelDrawing;
import com.sun.electric.tool.user.redisplay.VectorCache;
import com.sun.electric.tool.user.tecEdit.Info;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.VolatileImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

/**
 * Class to display the nodes and arcs in a technology (in the Component Menu).
 */
public class TechPalette extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener,
        KeyListener, PaletteFrame.PlaceNodeEventListener, ComponentListener, DragGestureListener, DragSourceListener {

    /** the number of palette entries. */				private int menuX = -1, menuY = -1;
    /** the size of a palette entry. */					private int entrySize;
    /** the list of objects in the palette. */			private List<Object> inPalette = new ArrayList<Object>();
    /** the currently selected Node object. */			private Object highlightedNode;
    /** to collect contacts that must be groups */      private HashMap<Object,Object> elementsMap = new HashMap<Object,Object>();
    /** cached palette image */                         private VolatileImage paletteImage;
    /** if the palette image needs to be redrawn */     private boolean paletteImageStale;
    /** menu entry bounds */                            private Rectangle entryRect;
    /** Variables needed for drag-and-drop */			private DragSource dragSource = null;
    /** Offscreen image */                              private PixelDrawing offscreen;

    TechPalette()
    {
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addComponentListener(this);
        paletteImage = null;
        paletteImageStale = true;

        // initialize drag-and-drop from this palette
        dragSource = DragSource.getDefaultDragSource();
		dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, this);
    }

	/**
	 * Loads a new technology into the palette. Returns the
	 * new desired size of the panel
	 * @param tech the technology to load
	 * @param curCell the cell in the window associated with this palette
	 * @return the preferred size of the new panel
	 */
	public Dimension loadForTechnology(Technology tech, Cell curCell)
	{
		inPalette.clear();
		elementsMap.clear();

		Object[][] paletteMatrix = tech.getNodesGrouped(curCell);

		if (paletteMatrix == null)
		{
			System.out.println("Error: no palette information found for " + tech.getTechName());
		} else
		{
			menuX = paletteMatrix[0].length;
			menuY = paletteMatrix.length;
			inPalette.clear();
			for (int i = 0; i < menuX; i++)
			{
				for (int j = 0; j < menuY; j++)
				{
					Object item = (paletteMatrix[j] == null) ? null : paletteMatrix[j][i];
					if (item instanceof NodeInst)
					{
						item = rotateTransistor((NodeInst)item);
					} else if (item instanceof List)
					{
						List nodes = (List)item;
						for(int k=0; k<nodes.size(); k++)
						{
							Object o = nodes.get(k);
							if (o instanceof NodeInst)
							{
								NodeInst ni = (NodeInst)o;
								nodes.set(k, rotateTransistor(ni));
							}
						}
					}
					item = getInUse(item);
					inPalette.add(item);
				}
			}
		}

		Dimension size = TopLevel.getScreenSize();
		entrySize = (int)size.getWidth() / menuX;
		int ysize = (int)(size.getHeight()*0.9) / menuY;
		if (ysize < entrySize) entrySize = ysize;
		size.setSize(entrySize*menuX+1, entrySize*menuY+1);
		User.getUserTool().setCurrentArcProto(tech.getArcs().next());

		paletteImageStale = true;

		return size;
	}

	private NodeInst rotateTransistor(NodeInst ni)
	{
		if (!ni.getProto().getTechnology().isLayout()) return ni;
		int rot = 0;
		if (User.isRotateLayoutTransistors()) rot = 900;
		rot = (rot + ni.getAngle()) % 3600;
		PrimitiveNode.Function fun = ni.getFunction();
		if (fun.isTransistor())
		{
			NodeInst newNi = Technology.makeNodeInst(ni.getProto(), fun, rot, false, null, 0);
			newNi.copyVarsFrom(ni);
			ni = newNi;
		}
		return ni;
	}

    /**
     * Method to determine if item is in use or not. Null if not.
     */
    private static Object getInUse(Object item)
    {
        if (item != null)
        {
            PrimitiveNode p = null;
            // checking if node is in use
            if (item instanceof NodeInst && ((NodeInst)item).getProto() instanceof PrimitiveNode)
            {
                p = (PrimitiveNode)((NodeInst)item).getProto();
            }
            else if (item instanceof PrimitiveNode)
                p = (PrimitiveNode)item;
            if (p != null && p.isNotUsed())
                item = null;
        }
        return item;
    }

    /**
     * Method to compose item name depending on object class
     */
    private static String getItemName(Object item, boolean getVarName)
    {
        if (item instanceof PrimitiveNode)
        {
            PrimitiveNode np = (PrimitiveNode)item;
            return (np.getName());
        }
        if (item instanceof NodeInst)
        {
            NodeInst ni = (NodeInst)item;
            Variable var = ni.getVar(Technology.TECH_TMPVAR);
            if (getVarName && var != null && !var.isDisplay())
            {
                return (var.getObject().toString());
            }
            // At least case for well contacts
            return (ni.getProto().getName());
        }
        if (item instanceof ArcProto)
        {
            ArcProto ap = (ArcProto)item;
            return (ap.getName());
        }
        return ("");
    }

    public void mousePressed(MouseEvent e) {}

    /**
     * Method called when the user clicks over an entry in the component menu.
     */
    public void mouseReleased(MouseEvent e)
    {
        TechPalette panel = (TechPalette)e.getSource();
        panel.requestFocus();
        Object obj = getObjectUnderCursor(e.getX(), e.getY());
        JMenuItem menuItem;

        if (obj == null) return; // nothing selected

        if (obj instanceof NodeProto || obj instanceof NodeInst || obj instanceof ArcProto || obj instanceof List)
        {
            if (obj instanceof List)
            {
                List<?> list = (List)obj;
	            // Getting first element
                obj = list.get(0);
	            if (obj instanceof List) obj = ((List)obj).get(0);
                if (list.size() > 1 && isCursorOnCorner(e))
				{
                    // Careful with this name
					JPopupMenu menu = new JPopupMenu(getItemName(obj, true));

					for (Object item : list)
					{
						if (item instanceof JSeparator)
							menu.add((JSeparator)item);
						else if (item instanceof List)
						{
							List<?> subList = (List)item;
							for (Object subItem : subList)
							{
                                subItem = getInUse(subItem);
                                if (subItem == null) continue;
								menu.add(menuItem = new JMenuItem(getItemName(subItem, true)));
                                menuItem.addActionListener(new TechPalette.PlacePopupListListener(panel, subItem, list, subList));
							}
						}
                        else
						{
                            item = getInUse(item);
                            if (item == null) continue;
							menu.add(menuItem = new JMenuItem(getItemName(item, true)));
                            menuItem.addActionListener(new TechPalette.PlacePopupListListener(panel, item, list, null));
						}
					}
					menu.show(panel, e.getX(), e.getY());
					return;
				}
            }
            if (obj instanceof ArcProto)
				User.getUserTool().setCurrentArcProto((ArcProto)obj);
            else
			{
                PaletteFrame.placeInstance(obj, panel, false);
			}
        } else if (obj instanceof String)
        {
            String msg = (String)obj;
            if (msg.startsWith("LOADCELL "))
            {
            	String cellName = msg.substring(9);
            	Cell cell = (Cell)Cell.findNodeProto(cellName);
            	if (cell == null)
            	{
            		Job.getUserInterface().showErrorMessage("Cannot find cell " + cellName, "Unknown Cell");
            		return;
            	}
                PaletteFrame.placeInstance(cell, panel, false);
            } else if (msg.equals(Technology.SPECIALMENUCELL))
            {
            	// get current cell type
            	View view = null;
            	Cell curCell = WindowFrame.getCurrentCell();
            	if (curCell != null)
            	{
                	if (curCell.getTechnology().isLayout()) view = View.LAYOUT; else
                		if (curCell.getTechnology().isSchematics()) view = View.ICON;
            	}
                JPopupMenu cellMenu = new JPopupMenu("Cells");
                for(Iterator<Cell> it = Library.getCurrent().getCells(); it.hasNext(); )
                {
                    Cell cell = it.next();
                    if (view != null && cell.getView() != view) continue;
                    menuItem = new JMenuItem(cell.describe(false));
                    menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, cell));
                    cellMenu.add(menuItem);
                }
                cellMenu.show(panel, e.getX(), e.getY());
            } else if (msg.equals(Technology.SPECIALMENUMISC))
            {
                JPopupMenu specialMenu = new JPopupMenu("Miscellaneous");
                menuItem = new JMenuItem("Cell Instance...");
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { CellMenu.cellBrowserCommand(CellBrowser.DoAction.newInstance); } });
                specialMenu.add(menuItem);

                specialMenu.addSeparator();

                menuItem = new JMenuItem("Annotation Text");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, "ART_message"));
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("Layout Text...");
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { makeLayoutTextCommand(); } });
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("Annular Ring...");
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { AnnularRing.showAnnularRingDialog(); } });
                specialMenu.add(menuItem);

                specialMenu.addSeparator();

                menuItem = new JMenuItem("Cell Center");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, Generic.tech().cellCenterNode));
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("Essential Bounds");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, Generic.tech().essentialBoundsNode));
                specialMenu.add(menuItem);

                specialMenu.addSeparator();

                menuItem = new JMenuItem("Spice Code");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, "SIM_spice_card"));
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("Spice Declaration");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, "SIM_spice_declaration"));
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("Verilog Code");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, "VERILOG_code"));
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("Verilog Declaration");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, "VERILOG_declaration"));
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("Verilog Parameter");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, "VERILOG_parameter"));
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("Verilog External Code");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, "VERILOG_external_code"));
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("Simulation Probe");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, Generic.tech().simProbeNode));
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("DRC Exclusion");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, Generic.tech().drcNode));
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("AFG Exclusion");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, Generic.tech().afgNode));
                specialMenu.add(menuItem);


                specialMenu.addSeparator();

                menuItem = new JMenuItem("Invisible Pin");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, Generic.tech().invisiblePinNode));
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("Universal Pin");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, Generic.tech().universalPinNode));
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("Unrouted Pin");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, Generic.tech().unroutedPinNode));
                specialMenu.add(menuItem);
                specialMenu.show(panel, e.getX(), e.getY());
            } else if (msg.equals(Technology.SPECIALMENUPURE))
            {
                JPopupMenu pureMenu = new JPopupMenu("Pure");
                for(PrimitiveNode np : Technology.getCurrent().getNodesSortedByName())
                {
                    if (np.isNotUsed()) continue;
                    if (np.getFunction() != PrimitiveNode.Function.NODE) continue;
                    Technology.NodeLayer layer = np.getLayers()[0];
                    if (layer.getLayer().getFunction().isContact()) continue;
                    menuItem = new JMenuItem(np.describe(false));
                    menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, np));
                    pureMenu.add(menuItem);
                }
                pureMenu.show(panel, e.getX(), e.getY());
            } if (msg.equals(Technology.SPECIALMENUSPICE))
            {
                JPopupMenu cellMenu = new JPopupMenu("Spice");

                String currentSpiceLib = Simulation.getSpicePartsLibrary();
                Library spiceLib = Library.findLibrary(currentSpiceLib);
                if (spiceLib == null)
                {
                    // must read the Spice library from disk
                    URL fileURL = LibFile.getLibFile(currentSpiceLib + ".jelib");
                    new TechPalette.ReadSpiceLibrary(fileURL, cellMenu, panel, e.getX(), e.getY());
                } else
                {
                    ReadSpiceLibrary.loadSpiceCells(spiceLib, panel, cellMenu);
                    cellMenu.show(panel, e.getX(), e.getY());
                }
            } if (msg.equals(Technology.SPECIALMENUEXPORT))
            {
                JPopupMenu specialMenu = new JPopupMenu("Export");
                menuItem = new JMenuItem("Wire");
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { makeExport("wire"); } });
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("Bus");
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { makeExport("bus"); } });
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("Universal");
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { makeExport("universal"); } });
                specialMenu.add(menuItem);
                specialMenu.show(panel, e.getX(), e.getY());
            } if (msg.equals(Technology.SPECIALMENUTEXT))
            {
            	// place a piece of text
                PaletteFrame.placeInstance("ART_message", panel, false);
            } if (msg.equals(Technology.SPECIALMENUHIGH))
            {
            	// place a technology-edit highlight box
            	NodeInst ni = NodeInst.makeDummyInstance(Artwork.tech().boxNode);
    			ni.newVar(Info.OPTION_KEY, new Integer(Info.HIGHLIGHTOBJ));
                PaletteFrame.placeInstance(ni, panel, false);
            } if (msg.equals(Technology.SPECIALMENUPORT))
            {
            	// place a technology-edit port
                PaletteFrame.placeInstance(Generic.tech().portNode, panel, false);
            }
        }
        repaint();
    }

    private void makeExport(String type)
    {
        if (type.equals("wire")) PaletteFrame.placeInstance(Schematics.tech().wirePinNode, this, true); else
        if (type.equals("bus")) PaletteFrame.placeInstance(Schematics.tech().busPinNode, this, true); else
        if (type.equals("universal")) PaletteFrame.placeInstance(Generic.tech().invisiblePinNode, this, true);
    }

    public void makeLayoutTextCommand()
    {
        LayoutText dialog = new LayoutText(TopLevel.getCurrentJFrame());
        if (!Job.BATCHMODE) dialog.setVisible(true);
    }

    public void placeNodeStarted(Object nodeToBePlaced) {
        highlightedNode = nodeToBePlaced;
    }

    public void placeNodeFinished(boolean cancelled) {
        highlightedNode = null;
        repaint();
    }

    // ************************************************ DRAG-AND-DROP CODE ************************************************

    public void dragGestureRecognized(DragGestureEvent e)
	{
	    Object obj = getObjectUnderCursor(e.getDragOrigin().x, e.getDragOrigin().y);

		// make a Transferable Object
		EditWindow.NodeProtoTransferable transferable = new EditWindow.NodeProtoTransferable(obj, null);

		// begin the drag
		dragSource.startDrag(e, DragSource.DefaultLinkDrop, transferable, this);
	}

	public void dragEnter(DragSourceDragEvent e) {}

	public void dragOver(DragSourceDragEvent e) {}

	public void dragExit(DragSourceEvent e) {}

	public void dragDropEnd(DragSourceDropEvent e) {}

	public void dropActionChanged (DragSourceDragEvent e) {}

    /**
     * Class to read a Spice library in a new thread.
     */
    private static class ReadSpiceLibrary extends Job
    {
    	private URL fileURL;
        private transient JPopupMenu cellMenu;
        private transient TechPalette panel;
        private transient int x, y;
        private Library lib;

    	protected ReadSpiceLibrary(URL fileURL, JPopupMenu cellMenu, TechPalette panel, int x, int y)
        {
            super("Read Spice Library", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.fileURL = fileURL;
            this.cellMenu = cellMenu;
            this.panel = panel;
            this.x = x;
            this.y = y;
            startJob();
        }

        public boolean doIt() throws JobException
        {
            lib = LibraryFiles.readLibrary(fileURL, null, FileType.JELIB, false);
//            Undo.noUndoAllowed();
            if (lib == null) return false;
			fieldVariableChanged("lib");
            return true;
        }

        public void terminateOK()
        {
            if (lib == null) return;
            loadSpiceCells(lib, panel, cellMenu);
            cellMenu.show(panel, x, y);
        }

        public static void loadSpiceCells(Library lib, TechPalette panel, JPopupMenu cellMenu)
        {
            for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
            {
                Cell cell = it.next();
                // only access to icons of those cells
                if (!cell.isIcon()) continue;
                JMenuItem menuItem = new JMenuItem(cell.getName());
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, cell));
                cellMenu.add(menuItem);
            }
        }
    }

    static class PlacePopupListener implements ActionListener
    {
        TechPalette panel;
        Object obj;

        PlacePopupListener(TechPalette panel, Object obj) { super();  this.panel = panel;   this.obj = obj; }

        public void actionPerformed(ActionEvent evt)
        {
            PaletteFrame.placeInstance(obj, panel, false);
        }
    }

    static class PlacePopupListListener extends PlacePopupListener
        implements ActionListener
    {
        List list;
	    List subList;

        PlacePopupListListener(TechPalette panel, Object obj, List list, List subList)
        {
	        super(panel, obj);
	        this.list = list;
	        this.subList = subList;
        }

        public void actionPerformed(ActionEvent evt)
        {
            if (obj instanceof ArcProto)
				User.getUserTool().setCurrentArcProto((ArcProto)obj);
            else
                PaletteFrame.placeInstance(obj, panel, false);
            // No first element -> make it default
	        if (subList == null)
		        Collections.swap(list, 0, list.indexOf(obj));
	        else
	        {
		        Collections.swap(list, 0, list.indexOf(subList));
		        Collections.swap(subList, 0, subList.indexOf(obj));
	        }
            panel.paletteImageStale = true;
        }
    }

    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void keyPressed(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}
    public void keyTyped(KeyEvent e) {}
    public void mouseDragged(MouseEvent e) {}

    /**
     * Method to figure out which palette entry the cursor is over.
     * @return an Object that is in the selected palette entry.
     */
    private Object getObjectUnderCursor(int xp, int yp)
    {
        int x = xp / (entrySize+1);
        int y = menuY - (yp / (entrySize+1)) - 1;
        if (y < 0) y = 0;
        int index = x * menuY + y;
        if (index < 0 || index >= inPalette.size()) return null;
        Object obj = inPalette.get(index);
        return obj;
    }

	/**
     * Method to figure out which palette entry the cursor is over.
     * @return true if mouse is over bottom right corner
     */
    private boolean isCursorOnCorner(MouseEvent e)
    {
		int entryS = (entrySize+1);
        int x = e.getX() / (entryS);
        int y = menuY - (e.getY() / (entryS)) - 1;
        if (y < 0) y = 0;
		double deltaX = (e.getX() - x*entryS)/(double)entryS;
		double deltaY = (e.getY() - (menuY-y-1)*entryS)/(double)entryS;
		return (deltaX > 0.75 && deltaY > 0.75);
    }

    /**
     * Method called when the mouse hovers over a palette entry.
     * Updates the status area to indicate what the palette will do.
     */
    public void mouseMoved(MouseEvent e)
    {
        Object obj = getObjectUnderCursor(e.getX(), e.getY());
        if (obj instanceof List)
        {
            obj = ((List)obj).get(0);
	        if (obj instanceof List) obj = ((List)obj).get(0);
        }
        if (obj instanceof PrimitiveNode)
        {
           StatusBar.setSelectionOverride("CREATE NODE: " + ((PrimitiveNode)obj).describe(false));
        }
        else if (obj instanceof NodeInst)
        {
           StatusBar.setSelectionOverride("CREATE NODE: " + ((NodeInst)obj).describe(false));
        }
        else if (obj instanceof NodeProto)
        {
            StatusBar.setSelectionOverride("CREATE NODE: " + ((NodeProto)obj).describe(false));
        } else if (obj instanceof ArcProto)
        {
            ArcProto ap = (ArcProto)obj;
            StatusBar.setSelectionOverride("USE ARC: " + ap.describe());
        } else if (obj instanceof String)
        {
        	String str = (String)obj;
        	if (str.startsWith("LOADCELL "))
        		StatusBar.setSelectionOverride("CREATE CELL: " + str.substring(9)); else
        			StatusBar.setSelectionOverride(null);
        }
    }

    public void mouseExited(MouseEvent e)
    {
        StatusBar.setSelectionOverride(null);
    }

    public void mouseWheelMoved(MouseWheelEvent e) {}

    public void componentHidden(ComponentEvent e) {}
    public void componentMoved(ComponentEvent e) {}
    public void componentShown(ComponentEvent e) {}
    public void componentResized(ComponentEvent e) {
        paletteImageStale = true;
    }

    public void paint(Graphics g)
    {
        // stop now if not initialized
        if (menuX < 0 || menuY < 0) return;

        // recompute size of an entry from current window size
        Dimension size = getSize();
        int wid = (int)size.getWidth();
        int hei = (int)size.getHeight();
        entrySize = Math.min(wid / menuX - 1, hei / menuY - 1);
        if (wid <= 0 || hei <= 0) return;

        // show the image
        // copying from the image (here, gScreen is the Graphics object for the onscreen window)
        do {
            int returnCode;
            if (paletteImage == null) {
                paletteImage = createVolatileImage(getWidth(), getHeight());
                returnCode = VolatileImage.IMAGE_RESTORED;
            } else {
                returnCode = paletteImage.validate(getGraphicsConfiguration());
                if (returnCode == VolatileImage.IMAGE_INCOMPATIBLE || paletteImage.getWidth() != wid || paletteImage.getHeight() != hei) {
                    returnCode = VolatileImage.IMAGE_INCOMPATIBLE;
                    // old paletteImage doesn't work with new GraphicsConfig; re-create it
                    paletteImage.flush();
                    paletteImage = createVolatileImage(getWidth(), getHeight());
                }
            }
            if (returnCode != VolatileImage.IMAGE_OK || paletteImageStale)
                renderPaletteImage();
            g.drawImage(paletteImage, 0, 0, this);
        } while (paletteImage.contentsLost());
        paletteImageStale = false;

        // highlight node that user selected
        if (highlightedNode != null) {
            // draw highlights around cell with highlighted node
            int index = inPalette.indexOf(highlightedNode);
            if (index >= 0) {
                // put the Image in the proper place
                int x = index / menuY;
                int y = index % menuY;
                int imgX = x * (entrySize+1)+1;
                int imgY = (menuY-y-1) * (entrySize+1)+1;
                g.setColor(Color.BLUE);
                g.drawRect(imgX+1, imgY+1, entrySize-3, entrySize-3);
                g.drawRect(imgX+2, imgY+2, entrySize-5, entrySize-5);
            }
        }

        // highlight current arc
        Object arcObj = User.getUserTool().getCurrentArcProto();
        int index = -1;
        for (int i = 0; i < inPalette.size(); i++)
        {
            Object obj = inPalette.get(i);
            if (obj == null) continue;
            if (obj instanceof List) obj = ((List)obj).get(0);
            if (obj == arcObj)
            {
                index = i;
                break;
            }
        }

        if (index >= 0) {
            int x = index / menuY;
            int y = index % menuY;
            int imgX = x * (entrySize+1)+1;
            int imgY = (menuY-y-1) * (entrySize+1)+1;
            g.setColor(Color.RED);
            g.drawRect(imgX+1, imgY+1, entrySize-3, entrySize-3);
            g.drawRect(imgX+2, imgY+2, entrySize-5, entrySize-5);
        }
    }

    private final static double menuArcLength = 8;

    private void renderPaletteImage() {
        // draw the menu entries
        if (entrySize < 2) return;
        entryRect = new Rectangle(new Dimension(entrySize-2, entrySize-2));
        offscreen = new PixelDrawing(new Dimension(entrySize, entrySize));

        Graphics2D g = (Graphics2D)paletteImage.getGraphics();
        g.setBackground(new Color(User.getColor(User.ColorPrefType.BACKGROUND)));
        g.clearRect(0, 0, getWidth(), getHeight());

        for(int x=0; x<menuX; x++) {
            for(int y=0; y<menuY; y++) {
                // render the entry into an Image
                int index = x * menuY + y;
                if (index >= inPalette.size()) continue;
                Object toDraw = inPalette.get(index);
                boolean drawArrow = false;
//System.out.print("ENTRY "+x+","+y+" IS "+toDraw);
//if (toDraw != null) System.out.print(" (TYPE="+toDraw.getClass()+")");
//System.out.println();
                if (toDraw instanceof List) {
                    List list = ((List)toDraw);
                    toDraw = list.get(0);
                    if (toDraw instanceof List) toDraw = ((List)toDraw).get(0);
                    drawArrow = list.size() > 1;
                }

                // put the Image in the proper place
                int imgX = x * (entrySize+1)+1;
                int imgY = (menuY-y-1) * (entrySize+1)+1;

                if (toDraw instanceof ArcProto) {
                    // rendering an arc: create the temporary arc
                    ArcProto ap = (ArcProto)toDraw;

                    // determine scale for rendering
                    double largest = 0;
                    for(Iterator<ArcProto> it = ap.getTechnology().getArcs(); it.hasNext(); ) {
                        ArcProto otherAp = it.next();
                        if (otherAp.isSpecialArc()) continue;  // these are not drawn in palette
                        if (otherAp.isSkipSizeInPalette()) continue;
                        double wid = DBMath.gridToLambda(2*(otherAp.getDefaultGridExtendOverMin() + otherAp.getMaxLayerGridExtend()));
                        if (wid+menuArcLength > largest) largest = wid+menuArcLength;
                    }

                    // render the arc
                    double scalex = entrySize/largest * 0.8;
                    double scaley = entrySize/largest * 0.8;
                    double scale = Math.min(scalex, scaley);

            		// draw the arc
                    VectorCache.VectorBase[] shapes = VectorCache.drawPolys(ap.getShapeOfDummyArc(menuArcLength));
                    drawShapes(g, imgX, imgY, scale, shapes);

                    g.setColor(Color.RED);
                    g.drawRect(imgX, imgY, entrySize-1, entrySize-1);
                }
                if (toDraw instanceof NodeProto || toDraw instanceof NodeInst) {
                    NodeInst ni;
                    if (toDraw instanceof NodeInst) {
                        ni = (NodeInst)toDraw;
                        if (ni.getFunction() == PrimitiveNode.Function.TRAPNP || ni.getFunction() == PrimitiveNode.Function.TRA4PNP)
                            drawArrow = true;
                    } else {
                    	// rendering a node: create the temporary node
                        NodeProto np = (NodeProto)toDraw;
                        ni = NodeInst.makeDummyInstance(np);
                        if (np == Schematics.tech().diodeNode || np == Schematics.tech().capacitorNode ||
                            np == Schematics.tech().flipflopNode) drawArrow = true;
                    }

                    // determine scale for rendering
                    if (ni.isCellInstance())
                    {
                    	String str = ni.getProto().getName();
                        int defSize = 12;
                        Font f = new Font(User.getDefaultFont(), Font.BOLD, defSize);
                        FontMetrics fm = g.getFontMetrics(f);
                        float width = fm.stringWidth(str);
                        if (width > entryRect.width)
                        {
                        	defSize = (int)(defSize * entryRect.width / width);
                            f = new Font(User.getDefaultFont(), Font.BOLD, defSize);
                            fm = g.getFontMetrics(f);
                            width = fm.stringWidth(str);
                        }
                        g.setFont(f);
                        g.setColor(new Color(User.getColor(User.ColorPrefType.TEXT)));
                        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                        g.drawString(str, imgX + (entryRect.width - width)/2, imgY + (entryRect.height + (float)fm.getAscent())/2);
                    } else
                    {
	                    PrimitiveNode np = (PrimitiveNode)ni.getProto();
	                    double largest = 0;
	                    PrimitiveNode.Function groupFunction = np.getGroupFunction();
	                    for(Iterator<PrimitiveNode> it = np.getTechnology().getNodes(); it.hasNext(); )
	                    {
	                        PrimitiveNode otherNp = it.next();
	                        if (otherNp.getGroupFunction() != groupFunction) continue;
	                        if (otherNp.isSkipSizeInPalette()) continue;
	                        if (otherNp.getDefHeight() > largest) largest = otherNp.getDefHeight();
	                        if (otherNp.getDefWidth() > largest) largest = otherNp.getDefWidth();
	                    }

	                    // for pins, make them the same scale as the arcs
	                    if (groupFunction == PrimitiveNode.Function.PIN)
	                    {
	                        largest = 0;
	                        for(Iterator<ArcProto> it = np.getTechnology().getArcs(); it.hasNext(); )
	                        {
	                            ArcProto otherAp = it.next();
	                            if (otherAp.isSpecialArc()) continue; // ignore arc for sizing
	                            if (otherAp.isSkipSizeInPalette()) continue;
	                            double wid = DBMath.gridToLambda(2*(otherAp.getDefaultGridExtendOverMin() + otherAp.getMaxLayerGridExtend()));
	                            if (wid+8 > largest) largest = wid+8;
	                        }
	                    }

	                    // render it
	                    double scalex = entrySize/largest * 0.8;
	                    double scaley = entrySize/largest * 0.8;
	                    double scale = Math.min(scalex, scaley);

	                    // make sure the text is at the bottom of the entry
	                    Variable var = ni.getVar(Technology.TECH_TMPVAR);
	                    if (var != null)
	                    	ni.setTextDescriptor(Technology.TECH_TMPVAR, var.getTextDescriptor().withOff(0, -largest/2/0.8));
	                    VectorCache.VectorBase[] shapes = VectorCache.drawNode(ni);
	                    drawShapes(g, imgX, imgY, scale, shapes);
                    }
                    g.setColor(Color.BLUE);
                    g.drawRect(imgX, imgY, entrySize-1, entrySize-1);
                }
                if (toDraw instanceof String) {
                    String str = (String)toDraw;
                    if (str.equals(Technology.SPECIALMENUCELL) || str.equals(Technology.SPECIALMENUSPICE) ||
                    	str.equals(Technology.SPECIALMENUMISC) || str.equals(Technology.SPECIALMENUPURE))
                        	drawArrow = true;
                    if (str.startsWith("LOADCELL "))
                    {
                    	int colonPos = str.indexOf(':');
                    	if (colonPos < 0) str = str.substring(9); else
                    		str = str.substring(colonPos+1);
                        g.setColor(Color.BLUE);
                        g.drawRect(imgX, imgY, entrySize-1, entrySize-1);
                    }

                    int defSize = 18;
                    Font f = new Font(User.getDefaultFont(), Font.BOLD, defSize);
                    FontMetrics fm = g.getFontMetrics(f);
                    float width = fm.stringWidth(str);
                    if (width > entryRect.width)
                    {
                    	defSize = (int)(defSize * entryRect.width / width);
                        f = new Font(User.getDefaultFont(), Font.BOLD, defSize);
                        fm = g.getFontMetrics(f);
                        width = fm.stringWidth(str);
                    }
                    g.setFont(f);
                    g.setColor(new Color(User.getColor(User.ColorPrefType.TEXT)));
                    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g.drawString(str, imgX + (entryRect.width - width)/2, imgY + (entryRect.height + (float)fm.getAscent())/2);
                }
                if (drawArrow) drawArrow(g, x, y);
            }
        }
        offscreen = null;

        // show dividing lines
        g.setColor(new Color(User.getColor(User.ColorPrefType.GRID)));
        for(int i=0; i<=menuX; i++) {
            int xPos = (entrySize+1) * i;
            g.drawLine(xPos, 0, xPos, menuY*(entrySize+1));
        }
        for(int i=0; i<=menuY; i++) {
            int yPos = (entrySize+1) * i;
            g.drawLine(0, yPos, menuX*(entrySize+1), yPos);
        }
        g.dispose();
    }

    private void drawArrow(Graphics g, int x, int y) {
        int imgX = x * (entrySize+1)+1;
        int imgY = (menuY-y-1) * (entrySize+1)+1;
        int [] arrowX = new int[3];
        int [] arrowY = new int[3];
        arrowX[0] = imgX-2 + entrySize*7/8;
        arrowY[0] = imgY-2 + entrySize;
        arrowX[1] = imgX-2 + entrySize;
        arrowY[1] = imgY-2 + entrySize*7/8;
        arrowX[2] = imgX-2 + entrySize*7/8;
        arrowY[2] = imgY-2 + entrySize*3/4;
        g.setColor(new Color(User.getColor(User.ColorPrefType.GRID)));
        g.fillPolygon(arrowX, arrowY, 3);
    }

	/**
	 * Method to draw polygon "poly", transformed through "trans".
	 */
    private void drawShapes(Graphics2D g, int imgX, int imgY, double scale, VectorCache.VectorBase[] shapes) {
        AbstractDrawing.drawShapes(g, imgX, imgY, scale, shapes, offscreen, entryRect);
    }
}
