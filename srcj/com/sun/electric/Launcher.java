/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Launcher.java
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
package com.sun.electric;

import com.sun.electric.Main;
import com.sun.electric.tool.user.User;

import java.lang.Runtime;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class initializes the User Interface.
 * It is the main class of Electric.
 */
public final class Launcher
{
	public static void main(String[] args)
	{
		// launching is different on different computers
		try{
			String osName = System.getProperty("os.name").toLowerCase();
			if (osName.startsWith("linux") || osName.startsWith("solaris") ||
				osName.startsWith("sunos") || osName.startsWith("mac"))
			{
				// able to "exec" a new JVM: do it with the proper memory limit
				invokeElectric(args, "java");
			} else if (osName.startsWith("windows"))
			{
				invokeElectric(args, "javaw");
			} else
			{
				// not able to relaunch a JVM: just start Electric
				Main.main(args);
			}
		} catch(Exception e)
		{
			// problem figurint out what computer this is: just start Electric
			Main.main(args);
		}
	}

	private static void invokeElectric(String[] args, String program)
	{
		// see if the required amount of memory is already present
		Runtime runtime = Runtime.getRuntime();
		long maxMem = runtime.maxMemory() / 1000000;
		int maxMemWanted = User.getMemorySize();
		if (maxMemWanted <= maxMem)
		{
			// already have the desired amount of memory: just start Electric
			Main.main(args);
			return;
		}

		String command = program + " -mx" + maxMemWanted + "m -jar electric.jar";
        for (int i=0; i<args.length; i++) command += " " + args[i];
		try
		{
			runtime.exec(command);
		} catch (java.io.IOException e)
		{
			Main.main(args);
		}
	}

}
