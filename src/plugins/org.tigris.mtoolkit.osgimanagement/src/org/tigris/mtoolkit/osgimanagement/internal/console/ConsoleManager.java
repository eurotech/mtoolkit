/*******************************************************************************
 * Copyright (c) 2009 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.osgimanagement.internal.console;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.model.Framework;

public class ConsoleManager {

	private static Map consoles = new HashMap();
	private static Object CREATING_CONSOLE = new Object();
	private static Object SHOW_CONSOLE = new Object();
	private static Object DISCONNECT_CONSOLE = new Object();

	public static void connectConsole(Framework fw) {
		synchronized (consoles) {
			if (consoles.get(fw) != null)
				return;
			if (!fw.isConnected()) {
				FrameworkPlugin.error("Remote console cannot be connected to disconnected framework", new Throwable("<from here>"));
				return;
			}
			consoles.put(fw, CREATING_CONSOLE);
		}
		RemoteConsole con = null;
		boolean showConsole = false;
		try {
			con = createConsole(fw);
		} finally {
			synchronized (consoles) {
				if (con == null) {
					consoles.remove(fw);
					return;	// failed to initialize
				}
				Object action = consoles.put(fw, con);
				if (action == DISCONNECT_CONSOLE) {
					disconnectConsole0(con);
					consoles.remove(fw);
					return;
				}
				if (action == SHOW_CONSOLE) {
					showConsole = true;
				} // else action == CREATING_CONSOLE
				
			}
		}
		IConsoleManager conMng = ConsolePlugin.getDefault().getConsoleManager();
		conMng.addConsoles(new IConsole[] { con });
		if (showConsole)
			conMng.showConsoleView(con);
	}
	
	public static void showConsole(Framework fw) {
		Object obj;
		synchronized (consoles) {
			obj = (IOConsole) consoles.get(fw);
			if (obj == CREATING_CONSOLE) {
				consoles.put(fw, SHOW_CONSOLE);
				return;
			} else if (obj == SHOW_CONSOLE) {
				return;
			}
		}
		if (obj == null)
			connectConsole(fw);
		
		showConsoleIfCreated(fw);
	}
	
	private static void showConsoleIfCreated(Framework fw) {
		RemoteConsole con; 
		synchronized (consoles) {
			Object obj = consoles.get(fw);
			if (obj == null || obj == DISCONNECT_CONSOLE)
				return;
			if (obj == CREATING_CONSOLE) {
				consoles.put(fw, SHOW_CONSOLE);
				return;
			}
			con = (RemoteConsole) obj;
		}
		ConsolePlugin.getDefault().getConsoleManager().showConsoleView(con);
	}
	
	public static void disconnectConsole(Framework fw) {
		RemoteConsole console;
		synchronized (consoles) {
			Object obj = consoles.get(fw);
			if (obj == null || obj == DISCONNECT_CONSOLE)
				return;
			if (obj == CREATING_CONSOLE || obj == SHOW_CONSOLE) {
				consoles.put(fw, DISCONNECT_CONSOLE);
				return;
			}
			console = (RemoteConsole) consoles.remove(fw);
		}
		disconnectConsole0(console);
	}
	
	private static void disconnectConsole0(RemoteConsole con) {
	}
	
	private static RemoteConsole createConsole(Framework fw) {
		RemoteConsole console = new RemoteConsole(fw);
		return console;
	}
	
}
