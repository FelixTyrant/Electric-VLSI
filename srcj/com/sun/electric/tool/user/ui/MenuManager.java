/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Menu.java
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
package com.sun.electric.tool.user.ui;

/*
 * Created on Sep 30, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import com.sun.electric.tool.user.KeyBindingManager;


import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.plaf.basic.BasicMenuItemUI;

/**
 * @author gainsley
 * Most of the meat in this class resides in the static MenuManager methods,
 * and the inner class Menu, which extends JMenu.  This is mostly a result of
 * conforming to the way the JMenuBar is populated in MenuCommands.
 * <p>
 * A usage of this custom class is in SDI mode where there are 
 * multiple menubars and menus (for each window) that must maintain 
 * consistency across the multiple instances.  This is ensured by using a 
 * HashMap to store ArrayLists of associated menuitems, and adding this class 
 * as an ActionListener. This class then takes care of updating associated 
 * menus when one menu changes to state, to ensure consistency across all
 * the menubars.
 */

public class MenuManager
{
    /** Preferences for User Bindings */                            private static Preferences prefs = Preferences.userNodeForPackage(MenuManager.class);
    /** All menu items created, stores as ArrayLists in HashMap */  private static HashMap menuItems = new HashMap(40);
    /** global object to be used as listener */                     public static MenuUpdater updater = new MenuUpdater();
    /** Key Binding Manager for menu items */                       public static KeyBindingManager keyBindingManager = new KeyBindingManager("MenuKeyBinding-", prefs);

    /**
     * Common Interface for all MenuItem types:
     * Menu, MenuItem, CheckBoxMenuItem, RadioButtonMenuItem
     */
    public interface MenuItemInterface
    {
        public void setParentMenu(JMenu menu);
        public JMenu getParentMenu();
        public void addDefaultKeyBinding(KeyStroke stroke, KeyStroke prefixStroke);
        public KeyBindings getKeyBindings();
    }

    /**
     * Custom MenuItem extends JMenuItem.  Also conforms to
     * common Interface MenuItemInterface so that all custom
     * classes can be treated the same.
     */
    public static class MenuItem extends JMenuItem implements MenuItemInterface
    {
        /** parent menu */                      private JMenu parentMenu = null;

        public MenuItem(String s) { super(s); }
        public MenuItem(String text, int mnemonic) { super( text, mnemonic); }

        public void setParentMenu(JMenu menu) { parentMenu = menu; }
        public JMenu getParentMenu() { return parentMenu; }
        public void addDefaultKeyBinding(KeyStroke stroke, KeyStroke prefixStroke) {
            MenuManager.addDefaultKeyBinding(this, stroke, prefixStroke);
        }
        public KeyBindings getKeyBindings() { return MenuManager.getKeyBindings(this); }
    }

    public static class RadioButtonMenuItem extends JRadioButtonMenuItem implements MenuItemInterface
    {
        /** parent menu */                      private JMenu parentMenu = null;

        public RadioButtonMenuItem(String text, boolean selected) { super(text, selected); }
        
        public void setParentMenu(JMenu menu) { parentMenu = menu; }
        public JMenu getParentMenu() { return parentMenu; }
        public void addDefaultKeyBinding(KeyStroke stroke, KeyStroke prefixStroke) {
            MenuManager.addDefaultKeyBinding(this, stroke, prefixStroke);
        }
        public KeyBindings getKeyBindings() { return MenuManager.getKeyBindings(this); }
    }
    

    public static class CheckBoxMenuItem extends JCheckBoxMenuItem implements MenuItemInterface
    {
        /** parent menu */                      private JMenu parentMenu = null;

        public CheckBoxMenuItem(String text, boolean state) { super(text, state); }
        
        public void setParentMenu(JMenu menu) { parentMenu = menu; }
        public JMenu getParentMenu() { return parentMenu; }
        public void addDefaultKeyBinding(KeyStroke stroke, KeyStroke prefixStroke) {
            MenuManager.addDefaultKeyBinding(this, stroke, prefixStroke);
        }
        public KeyBindings getKeyBindings() { return MenuManager.getKeyBindings(this); }
    }

    /**
     * The is the class with most of the meat, because of the way MenuCommands was
     * originally set up.  I believe this is Ok tho, as we can put all the Menu
     * specific code in this file.
     */
    public static class Menu extends JMenu implements MenuItemInterface
    {
        /** parent menu */                      private JMenu parentMenu = null;

        public Menu(String s) { super(s); }
        public Menu(String text, char mnemonic) { super(text); setMnemonic(mnemonic); }

        public void setParentMenu(JMenu menu) { parentMenu = menu; }
        public JMenu getParentMenu() { return parentMenu; }
        public void addDefaultKeyBinding(KeyStroke stroke, KeyStroke prefixStroke) {
            MenuManager.addDefaultKeyBinding(this, stroke, prefixStroke);
        }
        public KeyBindings getKeyBindings() { return MenuManager.getKeyBindings(this); }

        /**
         * Override the default method to add a JMenuItem to this Menu.
         * Exact same as super.add(JMenuItem), but also sets parent of
         * added menuItem to this.
         * @param menuItem the menuItem to be added to menu
         * @return the menuItem added
         */
        public MenuItem add(MenuItem menuItem) {
            super.add(menuItem);
            if (menuItem instanceof MenuItemInterface) {
                ((MenuItemInterface)menuItem).setParentMenu(this);
            }
            return menuItem;
        }

        /**
         * Add a JMenuItem to the JMenu.
         * @param s the menu item's displayed text.
         * @param accelerator the shortcut key, or null if none specified.
         * @param action the action to be taken when menu is activated.
         */
        public MenuItem addMenuItem(String s, KeyStroke accelerator, ActionListener action)
        {
            MenuItem item = new MenuItem(s);
            addItem(item, accelerator, action);
            return item;
        }

        /**
         * Add a JCheckBoxMenuItem to the JMenu.
         * @param s the menu item's displayed text.
         * @param state the initial state of the check box
         * @param accelerator the shortcut key, or null if none specified.
         * @param action the action to be taken when menu is activated.
         */
        public JMenuItem addCheckBox(String s, boolean state, KeyStroke accelerator, ActionListener action)
        {
            JMenuItem item = new CheckBoxMenuItem(s, state);
            addItem(item, accelerator, action);
            return item;
        }

        /**
         * Add a JRadioButtonMenuItem to the JMenu.
         * @param s the menu item's displayed text.
         * @param selected the initial selected state of the check box
         * @param group the button group to belong to; only one RadioButton per group can be selected.
         * @param accelerator the shortcut key, or null if none specified.
         * @param action the action to be taken when menu is activated.
         */
        public JMenuItem addRadioButton(String s, boolean selected, ButtonGroup group, KeyStroke accelerator, ActionListener action)
        {
            JMenuItem item = new RadioButtonMenuItem(s, selected);
            if (group != null) group.add((JRadioButtonMenuItem)item);
            addItem(item, accelerator, action);
            return item;
        }

        /**
         * Common method to add an item to this Menu.  This method is private;
         * external code should call the add<code>ItemType<code> methods of Menu instead.
         * @param item the item to add
         * @param accelerator the accelerator (null if none)
         * @param action the action to perform
         */
        private void addItem(JMenuItem item, KeyStroke accelerator, ActionListener action)
        {
            this.add(item);
            ((MenuItemInterface)item).setParentMenu(this);
            // add to hash map
            synchronized(menuItems) {
                String key = item.getText();
                ArrayList list = (ArrayList)menuItems.get(key);
                if (list == null) {
                    // this is the first instance of this menu item
                    list = new ArrayList();
                    menuItems.put(key, list);
                    // add default binding
                    ((MenuItemInterface)item).addDefaultKeyBinding(accelerator, null);
                    // add listener that will perform same thing as if user clicked on menu
                    // this is needed to change the state of button and fire off to listeners
                    keyBindingManager.addActionListener(item.getText(), new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            JMenuItem m = (JMenuItem)e.getSource(); m.doClick(); }
                    });
                    // add action listeners to keyBindingManager so it will activate them
                    // on a shortcut key or key-combo activation.
                    //keyBindingManager.addActionListener(item.getText(), action);
                    //keyBindingManager.addActionListener(item.getText(), MenuManager.updater);
                    //keyBindingManager.addActionListener(item.getText(), ToolBarButton.updater);
                    keyBindingManager.setEventSource(item.getText(), item);
                }
                list.add(item);
            }
            // set accelerator so user sees shortcut key on menu
            item.setAccelerator(accelerator);
            // add action listeners so when user selects menu, actions will occur
            item.addActionListener(action);
            item.addActionListener(MenuManager.updater);
            item.addActionListener(ToolBarButton.updater);
        }
    }

    /**
     * This class is used to update associated MenuItems in other JMenuBars
     * when one changes state.
     */
    public static class MenuUpdater implements ActionListener
    {
        /** Update associated MenuItems in any other JMenuBars on a state change */
        public void actionPerformed(ActionEvent e)
        {
            AbstractButton source = (AbstractButton)e.getSource();
            String name;
            if (source instanceof ToolBarButton) name = ((ToolBarButton)source).getName(); else
                name = source.getText();
            //System.out.println("ActionPerformed on Menu "+name+", state is "+source.isSelected());
            synchronized(menuItems) {
                ArrayList list = (ArrayList)menuItems.get(name);
                if (list == null) return;
                for (Iterator it = list.iterator(); it.hasNext(); ) {
                    AbstractButton b = (AbstractButton)it.next();
                    if (b == source) continue;
                    String name2;
                    if (source instanceof ToolBarButton) name2 = ((ToolBarButton)source).getName(); else
                        name2 = source.getText();
                    //System.out.println("   - SubactionPerformed on Menu "+name2+", state set to "+source.isSelected());
                    // update state on other menus to match state on activated menu
                    b.setSelected(source.isSelected());
                }
            }
        }
    }

    /**
     * Override JMenuBar to change the way JMenuBar handles KeyEvents.
     */
    public static class MenuBar extends JMenuBar
    {
        /** hidden menus */                 ArrayList hiddenMenus = new ArrayList();
        /** whether to ignore all shortcuts keys */ boolean ignoreKeyBindings;
        /** whether to ignore text editing keys */  boolean ignoreTextEditKeys;

        /**
         * Overrides JMenuBar's processKeyBinding, which distributes event
         * to it's menu items.  Instead, we pass the event to the keyBindingManager
         * to process the event.
         * @param ks the KeyStroke of the event
         * @param e the KeyEvent
         * @param condition condition (focused/not etc)
         * @param pressed
         * @return
         */
        protected boolean processKeyBinding(KeyStroke ks, KeyEvent e,
                                            int condition, boolean pressed) {
            // if ignoreTextEditKeys, ignore anything that does not have CTRL
            if (ignoreTextEditKeys) {
                if (!e.isControlDown() && !e.isMetaDown() && !e.isAltDown())
                    return false;                   // ignore
            }
            // see if we have a local binding (InputMap on JComponent)
            //boolean retValue = processKeyBinding(ks, e, condition, pressed);
            boolean retValue = false;

            // otherwise, pass to our keyBindingManager
            if (!retValue)
                retValue = keyBindingManager.processKeyEvent(e);
            // *do not* pass it to menus

            return retValue;
        }

        public JMenu addHidden(JMenu c) {
            hiddenMenus.add(c);
            return c;
        }

        public void setIgnoreKeyBindings(boolean b) { ignoreKeyBindings = b; }

        public boolean getIgnoreKeyBindings() { return ignoreKeyBindings; }

        public void setIgnoreTextEditKeys(boolean b) { ignoreTextEditKeys = b; }

        public boolean getIgnoreTextEditKeys() { return ignoreTextEditKeys; }
    }
    
    //------------------------------ Manager Methods -------------------------------

    public static KeyBindings getKeyBindings(JMenuItem item) {
        return keyBindingManager.getKeyBindings(item.getText());
    }

    /** Get a string description of the menu item of the form <p>
     * Menu | SubMenu | SubMenu | item
     * <p>
     * @param item The item to get a description for
     * @return a string of the description.
     */
    public static String getDescription(JMenuItem item) {
        Stack parents = new Stack();
        JMenu parent = ((MenuItemInterface)item).getParentMenu();
        JMenu previousParent = null;
        while (parent != null) {
            parents.push(parent);
            previousParent = parent;            // store for checking later
            parent = ((MenuItemInterface)parent).getParentMenu();
        }
        StringBuffer buf = new StringBuffer();
        while(!(parents.isEmpty())) {
            buf.append(((JMenuItem)parents.pop()).getText() + " | ");
        }
        buf.append(item.getText());
        // if final menu's parent is not a JMenuBar, then this menu is
        // not in it's final container, and subsequent calls to this method
        // may return a different String.  Because this string is used as a
        // key into hash tables, that would be very bad.  So issue an error.
        if ((previousParent == null) || !(previousParent.getParent() instanceof JMenuBar)) {
            System.out.println("  ERROR: Menu "+buf.toString()+" not in JMenuBar.");
            System.out.println("  Menus must be built top-down or inconsistencies in key bindings will result");
            Throwable t = new Throwable();
            t.printStackTrace(System.out);
        }
        return buf.toString();
    }

    /**
     * Adds a default key binding to the MenuItem.  Default key bindings are overridden
     * by any user stored key bindings, but may be restored via the Edit Key Bindings dialog.
     * @param m the MenuItem to add a default key binding
     * @param stroke the key stroke
     * @param prefixStroke an optional prefix stroke (may be null)
     */
    public static void addDefaultKeyBinding(JMenuItem m, KeyStroke stroke, KeyStroke prefixStroke) {
        // avoid adding the same binding multiple times when multiple menus are generated
        synchronized (menuItems) {
            List list = (List)menuItems.get(m.getText());
            if (list != null) {
                if (list.size() > 1) return;                    // more than one item created already
            }
        }
        // add default key binding
        keyBindingManager.addDefaultKeyBinding(m.getText(), KeyStrokePair.getKeyStrokePair(prefixStroke, stroke));
        // update accelerator
        updateAccelerator(m.getText());
    }

    /**
     * Add a user defined Key binding. This gets stored to preferences.
     * @param m the menu item
     * @param stroke the key stroke bound to menu item
     * @param prefixStroke an option prefix stroke (may be null)
     */
    public static void addUserKeyBinding(JMenuItem m, KeyStroke stroke, KeyStroke prefixStroke) {
        // add user key binding (gets stored to prefs, overrides default bindings)
        keyBindingManager.addUserKeyBinding(m.getText(), KeyStrokePair.getKeyStrokePair(prefixStroke, stroke));
        // update accelerator
        updateAccelerator(m.getText());
    }

    /**
     * Sets <ocde>item<code> back to default Key Bindings
     * @param item the item to reset to default bindings
     */
    public static void resetKeyBindings(JMenuItem item) {
        // tell KeyBindingManager to reset to default KeyBindings
        keyBindingManager.resetKeyBindings(item.getText());
        // update accelerator
        updateAccelerator(item.getText());
    }

    /**
     * Sets *All* menu items back to their default key bindings
     */
    public static void resetAllKeyBindings() {
        synchronized(menuItems) {
            Collection c = menuItems.values();
            for (Iterator it = c.iterator(); it.hasNext(); ) {
                List list = (List)it.next();
                JMenuItem m = (JMenuItem)list.get(0);
                // call set to default only on one, others will get reset by that method
                resetKeyBindings(m);
            }
        }
    }

    /**
     * Removes a key binding.
     * @param actionDesc the item to remove the binding from
     * @param pair the key stroke pair to remove
     */
    public static void removeKeyBinding(String actionDesc, KeyStrokePair pair) {
        // remove binding from binding manager
        keyBindingManager.removeKeyBinding(actionDesc, pair);
        // update accelerator
        updateAccelerator(actionDesc);
    }

    /**
     * Updates a menu item's accelerator.  Menu item is specified by
     * actionDesc (MenuItem.getText()).  This is usually called after a menu item's bindings
     * have changed (binding removed, added, or reset to default). This
     * updates the accelerator to a valid binding, which is displayed when
     * the user opens the menu.
     * @param actionDesc key for menu item
     */
    public static void updateAccelerator(String actionDesc) {
        // get valid key binding, update menus with it
        KeyBindings bindings = keyBindingManager.getKeyBindings(actionDesc);
        KeyStroke accelerator = null;
        if (bindings != null) {
            Iterator it;
            if (bindings.getUsingDefaultKeys()) it = bindings.getDefaultKeyStrokePairs(); else
                it = bindings.getKeyStrokePairs();
            while (it.hasNext()) {
                KeyStrokePair pair = (KeyStrokePair)it.next();
                if (pair.getPrefixStroke() != null) continue;         // menus can't display two-stroke key bindings
                accelerator = pair.getStroke();
                break;
            }
        }
        // update menu items
        synchronized(menuItems) {
            ArrayList list = (ArrayList)menuItems.get(actionDesc);
            for (Iterator it = list.iterator(); it.hasNext(); ) {
                JMenuItem m = (JMenuItem)it.next();
                m.setAccelerator(accelerator);
            }
        }
    }

    // ------------------------------ Clean Up Methods ----------------------------

    /**
     * Called when a TopLevel (in SDI mode) is disposed. This gets rid
     * of references to freed menu items, so that memory allocated to them
     * can be reclaimed.
     * @param menuBar the JMenuBar being disposed of.
     */
    public static void disposeOf(JMenuBar menuBar) 
    {
        // all menus
        for (int i=0; i<menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if (menu == null) continue;
            disposeofMenu(menu);
        }
    }

    private static void disposeofMenu(JMenu menu)
    {
        for (int j=0; j<menu.getItemCount(); j++) {
            JMenuItem item = menu.getItem(j);
            if (item == null) continue;
            if (item instanceof JMenu) {
                disposeofMenu((JMenu)item);
            } else {
                disposeofMenuItem(item);
            }
        }
    }

    private static void disposeofMenuItem(JMenuItem item)
    {
        // remove all listeners (which contain references to item)
        ActionListener [] listeners = item.getActionListeners();
        for (int k = 0; k < listeners.length; k++) {
            ActionListener listener = listeners[k];
            item.removeActionListener(listener);
        }
        synchronized(menuItems) {
            ArrayList list = (ArrayList)menuItems.get(item.getText());
            if (list == null) return;
            // remove reference to item
            list.remove(item);
        }
        //System.out.println("  removing menu item "+item);
    }

    // ---------------------------------------------------------------------------
    // This is really a pain but it's really the only way to do it ---
    // In order to display two-stroke accelerators on the painted menu item,
    // we need to extend BasicMenuItemUI.  Unfortunately, determining the
    // accelerator text is not a single method, but is duplicated in two big methods.
    // So I have copy those methods here and just change a few lines of code in each.
    // --- More importantly, this allows me to remove the true accelerator
    // (thus unbinding the menu) and allow the KeyBindingManager to solely handle
    // key events (KeyEvents hit the menu items before they are postprocessed by the
    // the KeyBindingManager.



}
