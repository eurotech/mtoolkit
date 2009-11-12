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

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.part.IPageBookViewPage;
import org.tigris.mtoolkit.iagent.DeviceConnectionListener;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.browser.model.Framework;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ContentChangeEvent;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ContentChangeListener;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.TreeRoot;
import org.tigris.mtoolkit.osgimanagement.internal.images.ImageHolder;

public class RemoteConsole extends IOConsole {

	private DeviceConnector connector;
	private Framework fw;
	private ConsoleReader reader;
	private Listener listener = new Listener();
	private Date timestamp;
	private IOConsoleOutputStream output;
	
	public static final String P_DISCONNECTED = "org.tigris.mtoolkit.osgimanagement.console.disconnected";
	
	public RemoteConsole(Framework fw) {
		super("", "osgiManagementConsole", ImageHolder.getImageDescriptor("frameworks.gif"), true);
		this.fw = fw;
		this.connector = fw.getConnector();
		timestamp = new Date();
		DeviceConnector.addDeviceConnectionListener(listener);
		if (fw.getParent() != null)
			((TreeRoot)fw.getParent()).addListener(listener);
	}
	
	protected void init() {
		super.init();
	}
	
	public IPageBookViewPage createPage(IConsoleView view) {
		IPageBookViewPage createPage = super.createPage(view);
		if (connector != null && connector.isActive()) {
			reader = redirectInput();
			output = newOutputStream();
			redirectOutput(output);
		}
		setName(computeName());
		return createPage;
	}
	
	
	
	private String computeName() {
		boolean connected = connector != null && connector.isActive();
		String fwName = fw.getName();
		String timeStamp = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(timestamp);
		return (connected ? "" : "<disconnected> ") + fwName + " [Remote Framework] (" + timeStamp + ")";
	}

	private ConsoleReader redirectInput() {
		if (connector == null)
			return null;
		try {
			return new ConsoleReader(this, connector.getVMManager());
		} catch (IAgentException e) {
			FrameworkPlugin.error("Exception while redirecting console input", e);
		}
		return null;
	}
	
	private void redirectOutput(IOConsoleOutputStream output) {
		try {
			if (connector != null) {
				connector.getVMManager().redirectFrameworkOutput(output);
			}
		} catch (IAgentException e) {
			try {
				IStatus status = FrameworkPlugin.handleIAgentException(e);
				output.write(NLS.bind("Failed to redirect framework output: {0}", status.getMessage()));
			} catch (IOException e1) {
				FrameworkPlugin.error("Exception while writing to console", e1);
			}
			FrameworkPlugin.log(FrameworkPlugin.handleIAgentException(e));
		}
	}
	
	public void disconnect() {
		DeviceConnector.removeDeviceConnectionListener(listener);
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				setName(computeName());
			}
		});
		if (reader != null)
			reader.dispose();
		if (output != null) {
			if (connector != null && connector.isActive())
				try {
					connector.getVMManager().redirectFrameworkOutput(null);
				} catch (IAgentException e) {
					FrameworkPlugin.error("Failed to reset framework output", e);
				}
			try {
				output.close();
			} catch (IOException e) {
			}
		}
		firePropertyChange(this, P_DISCONNECTED, Boolean.FALSE, Boolean.TRUE);
		if (fw.getParent() != null)
			((TreeRoot)fw.getParent()).removeListener(listener);
	}
	
	public boolean isDisconnected() {
		return connector == null || !connector.isActive();
	}
	
	protected void dispose() {
		disconnect();
		super.dispose();
	}



	private class Listener implements DeviceConnectionListener, ContentChangeListener {
		public void connected(DeviceConnector connector) {
		}

		public void disconnected(DeviceConnector connector) {
			if (connector == RemoteConsole.this.connector) {
				disconnect();
			}
		}

		public void elementAdded(ContentChangeEvent event) {
		}

		public void elementChanged(ContentChangeEvent event) {
			if (event.getTarget() == fw) {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						setName(computeName());
					}
				});
			}
		}

		public void elementRemoved(ContentChangeEvent event) {
		}
	}

	
}
