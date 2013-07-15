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
package org.tigris.mtoolkit.console;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.debug.core.model.IProcess;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.tigris.mtoolkit.console.internal.RemoteConsole;
import org.tigris.mtoolkit.iagent.DeviceConnector;

public final class ConsoleManager {
  public static final String CONSOLE_TYPE       = "osgiManagementConsole";

  private static Object      CREATING_CONSOLE   = new Object();
  private static Object      SHOW_CONSOLE       = new Object();
  private static Object      DISCONNECT_CONSOLE = new Object();
  private static Map         consoles           = new HashMap();

  public static boolean isOSGiManagementConsole(IConsole console) {
    return CONSOLE_TYPE.equals(console.getType());
  }

  public static IOConsole connectConsole(DeviceConnector dc, String consoleName) {
    return connectConsole(dc, consoleName, null);
  }

  public static IOConsole connectConsole(DeviceConnector dc, String consoleName, Object fwId) {
    return connectConsole(dc, consoleName, null, fwId);
  }

  public static IOConsole connectConsole(DeviceConnector dc, String consoleName, IProcess iProcess, Object fwId) {
    synchronized (consoles) {
      Object console = consoles.get(dc);
      if (consoles.get(dc) != null) {
        if (console instanceof IOConsole) {
          return (IOConsole) console;
        }
        return null;
      }
      if (!dc.isActive()) {
        return null;
      }
      consoles.put(dc, CREATING_CONSOLE);
    }
    RemoteConsole con = null;
    boolean showConsole = false;
    try {
      con = createConsole(dc, consoleName, iProcess, fwId);
    } finally {
      synchronized (consoles) {
        if (con == null) {
          consoles.remove(dc);
          return null; // failed to initialize
        }
        Object action = consoles.put(dc, con);
        if (action == DISCONNECT_CONSOLE) {
          disconnectConsole0(con);
          consoles.remove(dc);
          return con;
        }
        if (action == SHOW_CONSOLE) {
          showConsole = true;
        } // else action == CREATING_CONSOLE

      }
    }
    IConsoleManager conMng = ConsolePlugin.getDefault().getConsoleManager();
    conMng.addConsoles(new IConsole[] {
      con
    });
    if (showConsole) {
      conMng.showConsoleView(con);
    }
    return con;
  }

  public static void showConsole(DeviceConnector dc, String consoleName) {
    Object obj;
    synchronized (consoles) {
      obj = consoles.get(dc);
      if (obj == CREATING_CONSOLE) {
        consoles.put(dc, SHOW_CONSOLE);
        return;
      } else if (obj == SHOW_CONSOLE) {
        return;
      }
    }
    if (obj == null) {
      connectConsole(dc, consoleName);
    }

    showConsoleIfCreated(dc);
  }

  private static IOConsole showConsoleIfCreated(DeviceConnector dc) {
    RemoteConsole con;
    synchronized (consoles) {
      Object obj = consoles.get(dc);
      if (obj == null || obj == DISCONNECT_CONSOLE) {
        return null;
      }
      if (obj == CREATING_CONSOLE) {
        consoles.put(dc, SHOW_CONSOLE);
        return null;
      }
      con = (RemoteConsole) obj;
    }
    ConsolePlugin.getDefault().getConsoleManager().showConsoleView(con);
    return con;
  }

  public static void disconnectConsole(DeviceConnector dc) {
    RemoteConsole console;
    synchronized (consoles) {
      Object obj = consoles.get(dc);
      if (obj == null || obj == DISCONNECT_CONSOLE) {
        return;
      }
      if (obj == CREATING_CONSOLE || obj == SHOW_CONSOLE) {
        consoles.put(dc, DISCONNECT_CONSOLE);
        return;
      }
      console = (RemoteConsole) consoles.remove(dc);
    }
    disconnectConsole0(console);
  }

  private static void disconnectConsole0(RemoteConsole con) {
    con.disconnect();
  }

  private static RemoteConsole createConsole(DeviceConnector dc, String name, IProcess iProcess, Object fwId) {
    RemoteConsole console = new RemoteConsole(dc, name, iProcess, fwId);
    return console;
  }

  public static void setName(DeviceConnector connector, String name) {
    synchronized (consoles) {
      Object console = consoles.get(connector);
      if (console instanceof RemoteConsole) {
        ((RemoteConsole) console).setConsoleName(name);
      }
    }
  }

  public static boolean equalConsoleName(DeviceConnector connector, String name) {
    synchronized (consoles) {
      Object console = consoles.get(connector);
      if (console instanceof RemoteConsole) {
        RemoteConsole remCon = (RemoteConsole) console;
        return remCon.equalsName(name);
      }
    }
    return false;
  }
}
