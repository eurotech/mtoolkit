/*******************************************************************************
 * Copyright (c) 2005, 2009 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.osgimanagement.internal;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.osgimanagement.internal.console.MenuItem;
import org.tigris.mtoolkit.osgimanagement.internal.console.MenuItemListener;
import org.tigris.mtoolkit.osgimanagement.internal.console.RemoteConsoleManager;
import org.tigris.mtoolkit.osgimanagement.internal.console.ServerConsole;
import org.tigris.mtoolkit.osgimanagement.internal.console.StreamBuffer;


public class ConsoleView extends ViewPart implements MenuItemListener {
  
	private static Hashtable activeViews;
	private static Hashtable connsHash = new Hashtable();
	private static Hashtable streamsBuff = null;
  
	private CTabFolder tabPanel;
	private MenuItem defaultMenuItem;
	private IWorkbenchPage activePage;
  
	private Hashtable consoles;
	private boolean isInit = false;
  
	private Vector serverQueue;
  
	public ConsoleView() {
		consoles = new Hashtable();
		serverQueue = new Vector();
		defaultMenuItem = new MenuItem(Messages.ViewM_NoFrameworksMI_N);
		defaultMenuItem.setEnabled(false);
	}

	public void createPartControl(Composite parent) {
		streamsBuff = (Hashtable) FrameworkPlugin.getFromStorage(getClass().getName());
		if (streamsBuff == null) streamsBuff = new Hashtable();
		tabPanel = new CTabFolder(parent, SWT.TOP);
		isInit = true;
    activePage = getSite().getPage(); 
		if (activeViews == null) {
			activeViews = new Hashtable();
		}
    activeViews.put(new Integer(activePage.hashCode()), this);

		if (serverQueue.size() > 0) {
			for (int i=0; i < serverQueue.size(); i++) {
				addServerConsole((String)serverQueue.elementAt(i));
			}
			serverQueue.removeAllElements();
		} else {      
			IMenuManager mmanager = getViewSite().getActionBars().getMenuManager();
			mmanager.add(defaultMenuItem);    
			getViewSite().getActionBars().updateActionBars();
		}
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.CONSOLE);
	}
  
	public void setFocus() {
		IWorkbenchWindow  actWnd = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = getViewSite().getPage();
		IWorkbenchWindow pageWnd = ((page == null) ? null : page.getWorkbenchWindow());
		if (actWnd != null && pageWnd != null && actWnd.equals(pageWnd)) {
			getViewSite().getPage().bringToTop(this);
			if (tabPanel != null) {
  			tabPanel.setFocus();
	  	}
	  }
  }
  
	public static void setActiveServer(String serverName) {
		if (activeViews == null) return;
		Enumeration e = activeViews.elements();
		while (e.hasMoreElements()) {
			((ConsoleView) e.nextElement()).setActiveServer0(serverName);
		}
	}

	private void setActiveServer0(String serverName) {
		if (activePage == getViewSite().getWorkbenchWindow().getActivePage()) {
			activePage.activate(this);
		}
		ServerConsole cons = (ServerConsole)consoles.get(serverName);
		if (cons != null) {
			tabPanel.setSelection(cons.getTabItem());
			cons.setFocusOnConsole();
		}
	}

	public static void addServerConsole(final String serverName) {
		if (activeViews == null) return;
    Display display = Display.getCurrent();
    if (display == null) display = Display.getDefault();
    display.asyncExec(new Runnable() {
      public void run() {
        Enumeration e = activeViews.elements();
        while (e.hasMoreElements()) {
          ((ConsoleView)e.nextElement()).addServerConsole0(serverName);
        }
      }
    });
	}
  
	private void addServerConsole0(String serverName) {
		if (!isInit) {
			if (!serverQueue.contains(serverName)) {
				serverQueue.addElement(serverName);
			}
			return;
		}
		ServerConsole cons = (ServerConsole)consoles.get(serverName);
		if (cons == null) {
			cons = new ServerConsole(serverName, tabPanel, getViewSite().getActionBars());
			tabPanel.setSelection(cons.getTabItem());
			MenuItem consoleItem = cons.getMenuItem();
			consoleItem.addMenuItemListener(this);

			IMenuManager mmanager = getViewSite().getActionBars().getMenuManager();
			mmanager.add(consoleItem);    
      
			consoles.put(serverName, cons);
      
			if (consoles.size() == 1 && mmanager.find(defaultMenuItem.getId()) != null) {
				mmanager.remove(defaultMenuItem.getId());
			}

			getViewSite().getActionBars().updateActionBars();
		}
	}
  
	public static void renameServerConsole(String oldName, String newName) {
		if (activeViews == null) return;
		Enumeration e = activeViews.elements();
		while (e.hasMoreElements()) {
			((ConsoleView)e.nextElement()).renameServerConsole0(oldName, newName);
		}

		Object conn = connsHash.remove(oldName);
		if (conn != null) connsHash.put(newName, conn);
	}
  
	private void renameServerConsole0(String oldName, String newName) {
		ServerConsole cons = (ServerConsole)consoles.get(oldName);
		if (cons != null) {
			consoles.remove(oldName);
			cons.renameServer(newName);
			consoles.put(newName, cons);
		}
	}
  
	public static void removeServerConsole(String serverName) {
		if (activeViews == null) return;
		Enumeration e = activeViews.elements();
		while (e.hasMoreElements()) {
			((ConsoleView)e.nextElement()).removeServerConsole0(serverName);
		}
	}
  
	private void removeServerConsole0(String serverName) {
		ServerConsole cons = (ServerConsole)consoles.get(serverName);
		if (cons != null) {
			cons.disconnect();
			cons.setTabItemVisible(false);
			MenuItem consoleItem = cons.getMenuItem();
			consoleItem.removeMenuItemListener(this);
      
			IMenuManager mmanager = getViewSite().getActionBars().getMenuManager();
			mmanager.remove(consoleItem.getId());    

			consoles.remove(serverName);
      
			if (consoles.size() == 0) {
				mmanager.add(defaultMenuItem);
			}
    
			getViewSite().getActionBars().updateActionBars();
		}
	}
	
	public static void serverConsoleWasDisconnected(String serverName) {
		connsHash.remove(serverName);
	}
  
	public static void connectServer(final String serverName, final DeviceConnector connector) {
		if (activeViews == null) return;
		connsHash.put(serverName, connector);
		Display display = Display.getCurrent();
		if (display == null) display = Display.getDefault();
		display.asyncExec(new Runnable() {
		  public void run() {
		    Enumeration e = activeViews.elements();
		    while (e.hasMoreElements()) {
		      ((ConsoleView)e.nextElement()).connectServer0(serverName, connector);
		    }
		  }
		});
	}

	private void connectServer0(String frameworkName, DeviceConnector connector) {
		ServerConsole cons = (ServerConsole)consoles.get(frameworkName);
		if (!cons.isConnected()) {
			cons.connectConsole(connector);
		} else {
		  ((RemoteConsoleManager)cons.getConsoleListener()).reconnect(connector);
		}
		
		StreamBuffer sb = (StreamBuffer) streamsBuff.get(frameworkName);

		if (sb != null) {
			cons.getConsoleListener().dumpText(sb.get());
			sb.clear();
		}
		
		tabPanel.setSelection(cons.getTabItem());
		setFocus();
	}

	public static void disconnectServer(String serverName) {
	  if (activeViews == null) return;
	  Enumeration e = activeViews.elements();
	  while (e.hasMoreElements()) {
	    ((ConsoleView)e.nextElement()).disconnectServer0(serverName);
	  }
	}

	private void disconnectServer0(String serverName) {
    ServerConsole cons = (ServerConsole)consoles.get(serverName);
    if (cons != null && cons.isConnected()) {
      cons.getConsoleListener().disconnected();
    }
	}

	public void menuSelected(MenuItem mi) {
		ServerConsole cons = (ServerConsole)consoles.get(mi.getText());
		cons.setTabItemVisible(mi.isChecked());
	}
  
	public void dispose() {
		isInit = false;
		super.dispose();
		activeViews.remove(new Integer(activePage.hashCode()));
    
		Enumeration cons = consoles.keys();
		while (cons.hasMoreElements()) {
			String name = (String)cons.nextElement();

			// TODO: remember cmd that user was written before hidding
			// of console in order to give him the possibility to execute it. 
			//getCurrentLineText()
			
			ServerConsole sc = (ServerConsole)consoles.get(name);
			if (activeViews.size() == 0) {
				if (FrameworkPlugin.getDefault() != null) {
					StreamBuffer sb = new StreamBuffer();
					sb.dumpText(sc.getText());				
					streamsBuff.put(name, sb);
				}
			}
		}
    
		if (activeViews.size() == 0) {
			FrameworkPlugin.putInStorage(getClass().getName(), streamsBuff);
			activeViews = null;
		}
	}
  
	public boolean isCreated() {
		return isInit;
	}
	
}