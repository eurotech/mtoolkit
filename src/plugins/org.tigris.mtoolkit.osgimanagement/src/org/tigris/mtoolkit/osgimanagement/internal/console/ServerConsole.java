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
package org.tigris.mtoolkit.osgimanagement.internal.console;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.osgimanagement.internal.ConsoleView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.IHelpContextIds;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;


public class ServerConsole {
  
	private String server;
	private String tabName;
	private CTabFolder tabFolder;
	private CTabItem tabItem;
	private MenuItem menuItem;
	private Console console;
	private ConsoleManager cManager;
  
	private int indexOnTab = 0;
  
	private final static int NOT_CONNECTED = 0;
	private final static int CONNECTED = 1;
	
	private int connectionState = NOT_CONNECTED;
  
	public ServerConsole(String serverName, CTabFolder folder, IActionBars aBars) {
		server = serverName;
		tabName = NLS.bind(Messages.Not_Connected, serverName);
		tabFolder = folder;

		tabItem = new CTabItem(tabFolder, 0);
		tabItem.setText(tabName);
    
		menuItem = new MenuItem(serverName, MenuItem.AS_CHECK_BOX);
		menuItem.setChecked(true);
    
		console = new Console(tabFolder, SWT.CENTER, aBars);
		console.setEditable(false);
		tabItem.setControl(console);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(console, IHelpContextIds.CONSOLE);
	}
  
	public CTabItem getTabItem() {
		return tabItem;
	}
  
	public MenuItem getMenuItem() {
		return menuItem;
	}

	public String getServerName() {
		return server;
	}
  
	public void renameServer(String newName) {
		server = newName;
		switch (connectionState) {
			case NOT_CONNECTED:
				tabName = NLS.bind(Messages.Not_Connected, server);
				break;
			case CONNECTED:
				tabName = NLS.bind(Messages.Connected_To, server);
				break;
		}
    
		if (!tabItem.isDisposed()) {
			tabItem.setText(tabName);
		}
		menuItem.setText(server);
	}
  
	public void connectConsole(DeviceConnector connector) {
		cManager = new RemoteConsoleManager(console, connector, this);
		updateAfterConnect();
		tabName = NLS.bind(Messages.Connected_To, server);
		tabItem.setText(tabName);
		connectionState = CONNECTED;
	}
  
	private void updateAfterConnect() {
		setTabItemVisible(true);
		menuItem.setChecked(true);
		console.setConsoleManager(cManager);
		console.setEditable(true);
	}
  
	public void disconnect() {
		if (connectionState != NOT_CONNECTED) {
			disconnected();
		}
	}
  
	public boolean isConnected() {
		return connectionState != NOT_CONNECTED;
	}
  
	public void setFocusOnConsole() {
		console.setFocus();
	}
  
  
	void disconnected() {
		console.setConsoleManager(null);
		if (cManager != null) cManager.freeResources();
		cManager = null;
		connectionState = NOT_CONNECTED;
		if (!console.isDisposed() && FrameworkPlugin.getDefault() != null) {
			console.getDisplay().asyncExec(new Runnable() {
				public void run() {
					try {
						tabName = NLS.bind(Messages.Not_Connected, server);
						if (!tabItem.isDisposed()) {
							tabItem.setText(tabName);
						}
						console.setEditable(false);
					} catch (org.eclipse.swt.SWTException ex) {}
				}
			});
		}
		ConsoleView.serverConsoleWasDisconnected(server);
	}
  
	public void setTabItemVisible(boolean isVisible) {
		if (tabItem.isDisposed() == !isVisible) {
			//no need to change anything
			return;
		}
		if (isVisible) {
			if (indexOnTab > tabFolder.getItemCount()) {
				indexOnTab = tabFolder.getItemCount();
			}
			tabItem = new CTabItem(tabFolder, 0, indexOnTab);
			tabItem.setText(tabName);
			tabItem.setControl(console);
		} else {
			indexOnTab = tabFolder.indexOf(tabItem);
			if (console != null && console.isVisible()) 
				console.setVisible(false); 
			tabItem.dispose();
		}
	}
  
	public ConsoleListener getConsoleListener() {
		if (cManager instanceof ConsoleListener) {
			return (ConsoleListener)cManager;
		}
		return null;
	}
  
	public String getText() {
		String text = ""; //$NON-NLS-1$
		if (console != null) {
			if (console.isDisposed()) {
				text = console.getStoredText();
				console.clearStoredText();
			} else {
				text = console.getText();
			}
		}

		return text;
	}
	
}