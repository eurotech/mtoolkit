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
package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.internal.DeviceConnectorImpl;
import org.tigris.mtoolkit.iagent.spi.ConnectionEvent;
import org.tigris.mtoolkit.iagent.spi.ConnectionListener;
import org.tigris.mtoolkit.iagent.spi.ConnectionManager;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;
import org.tigris.mtoolkit.osgimanagement.internal.console.ConsoleManager;

public class PMPConnectionListener implements ConnectionListener {

	private FrameWork fw;
	private String frameworkName;
	private DeviceConnector connector;
	private boolean autoConnected;
	boolean shouldInstallIAgent = false;

	public PMPConnectionListener(FrameWork fw, String frameworkName, DeviceConnector connector, boolean autoConnected) {
		this.fw = fw;
		this.frameworkName = frameworkName;
		this.connector = connector;
		this.autoConnected = autoConnected;
		((DeviceConnectorImpl) connector).getConnectionManager().addConnectionListener(this);
	}

	public void connectionChanged(ConnectionEvent e) {
		if (e.getConnection().getType() != ConnectionManager.PMP_CONNECTION)
			return;
		BrowserErrorHandler.debug("PMP Connection Changed " + (e.getType() == ConnectionEvent.CONNECTED	? "CONNECTED" : "DISCONNECTED") + " " + connector.getProperties().get("framework-name") + " " + connector); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		if (e.getType() == ConnectionEvent.DISCONNECTED) {
			disconnected();
		} else if (e.getType() == ConnectionEvent.CONNECTED) {
			connected();
		}
	}

	public void disconnected() {
		if (!autoConnected) {
			FrameworkConnectorFactory.disconnectConsole(fw);
		}

		// if disconnect event received while connect thread is running
		// block disconnect until connect thread is finished/breaked
		synchronized (FrameworkConnectorFactory.getLockObject(connector)) {
		}

		fw.disconnect();
	}

	public void connected() {
		if (fw.isConnected())
			return;
		new Thread() {
			public void run() {
				try {
					// force creation of pmp connection
					if (!connector.getVMManager().isVMActive()) {
						return;
					}
				} catch (IAgentException e) {
					e.printStackTrace();
					BrowserErrorHandler.processError(e, fw);
					return;
				}

				if (fw != null) {
					fw.connected(connector);
				}
				if (!autoConnected && fw.isConnected()) {
					ConsoleManager.connectConsole(fw);
				}
			}
		}.start();
	}

}
