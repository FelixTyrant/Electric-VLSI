/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StartupPrefs.java
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
package com.sun.electric;

import java.util.prefs.Preferences;

/**
 * Module to access Prefs which are used at startup.
 * They are accessed without initializing Tool and Technology classes
 */
public class StartupPrefs {
    private static final String USER_NODE = "/com/sun/electric/tool/user";
    private static final Preferences userNode = Preferences.userRoot().node(USER_NODE);

	/** Preferences key to tell the initial display style for Electric. */
    public static final String DisplayStyleKey = "DisplayStyle";
	/** Default initial display style for Electric. */
    public static final int DisplayStyleDef = 0;
	/**
	 * Method to tell the initial display style for Electric.
	 * The values are: 0=OS default, 1=MDI, 2=SDI.
	 * The default is 0.
	 * @return the display style for Electric.
	 */
	public static int getDisplayStyle() { return getUserInt(DisplayStyleKey, DisplayStyleDef); }

	/** Preferences key to tell the maximum memory to use for Electric, in megatybes. */
    public static final String MemorySizeKey = "MemorySize";
	/** Default maximum memory to use for Electric, in megatybes. */
    public static final int MemorySizeDef = 65;
	/**
	 * Method to tell the maximum memory to use for Electric, in megatybes.
	 * The default is 65 megabytes which is not enough for serious work.
	 * @return the maximum memory to use for Electric (in megabytes).
	 */
    public static int getMemorySize() { return getUserInt(MemorySizeKey, MemorySizeDef); }

	/** Preferences key to tell the maximum permanent space of 2dn GC to use for Electric, in megatybes. */
    public static final String PermSizeKey = "PermSize";
	/** Default maximum permanent space of 2dn GC to use for Electric, in megatybes. */
    public static final int PermSizeDef = 0;
	/**
	 * Method to tell the maximum permanent space of 2dn GC to use for Electric, in megatybes.
	 * The default is 0. If zero, value is not considered.
	 * @return the maximum memory to use for Electric (in megabytes).
	 */
    public static int getPermSpace() { return getUserInt(PermSizeKey, PermSizeDef); }

    public static boolean getUserBoolean(String key, boolean def) {
        return userNode.getBoolean(key, def);
    }

    public static int getUserInt(String key, int def) {
        return userNode.getInt(key, def);
    }

    public static long getUserLong(String key, long def) {
        return userNode.getLong(key, def);
    }

    public static double getUserDouble(String key, double def) {
        return userNode.getDouble(key, def);
    }

    public static String getUserString(String key, String def) {
        return userNode.get(key, def);
    }
}
