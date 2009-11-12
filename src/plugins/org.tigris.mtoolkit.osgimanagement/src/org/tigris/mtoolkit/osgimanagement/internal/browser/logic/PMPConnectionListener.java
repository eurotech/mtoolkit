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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.internal.DeviceConnectorImpl;
import org.tigris.mtoolkit.iagent.spi.ConnectionEvent;
import org.tigris.mtoolkit.iagent.spi.ConnectionListener;
import org.tigris.mtoolkit.iagent.spi.ConnectionManager;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.ActionsManager;
import org.tigris.mtoolkit.osgimanagement.internal.console.ConsoleManager;

public class PMPConnectionListener implements ConnectionListener {

	private FrameworkImpl fw;
	private String frameworkName;
	private DeviceConnector connector;
	private boolean autoConnected;
	boolean shouldInstallIAgent = false;

	public PMPConnectionListener(FrameworkImpl fw, String frameworkName, DeviceConnector connector, boolean autoConnected) {
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
			ActionsManager.disconnectConsole(fw);
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
		Job connectJob = new Job(frameworkName) {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Connecting "+frameworkName, FrameworkConnectorFactory.CONNECT_PROGRESS);
					monitor.subTask("Connecting "+frameworkName);
					try {
						// force creation of pmp connection
						if (!connector.getVMManager().isVMActive()) {
							return FrameworkPlugin.newStatus(IStatus.ERROR, "Connection failed", null);
						}
					} catch (IAgentException e) {
						return FrameworkPlugin.newStatus(IStatus.ERROR, "Connection failed", e);
					}
					
					
					try {
						if (!connector.getVMManager().isVMInstrumented(false)) {
							if (shouldInstallIAgent()) {
								try {
									connector.getVMManager().instrumentVM();
								} catch (IAgentException iae) {
									return new Status(IStatus.ERROR, FrameworkPlugin.PLUGIN_ID, "Unable to instrument VM.", iae);
								}
							} else {
								return Status.OK_STATUS;
							}
						}
					} catch (IAgentException e) {
						return FrameworkPlugin.newStatus(IStatus.ERROR, "Connection failed", e);
					}
					
					monitor.worked(FrameworkConnectorFactory.CONNECT_PROGRESS_CONNECTING);

					
					
					if (fw != null) {
						fw.connect(connector, monitor);
					}
					if (!autoConnected && fw.isConnected()) {
						ConsoleManager.connectConsole(fw);
					}
					return Status.OK_STATUS;
				} finally {
					monitor.done();
				}
			}
		};
		connectJob.schedule();
		
	}

	private boolean shouldInstallIAgent() {
		final Display display = PlatformUI.getWorkbench().getDisplay();
		final Boolean result[] = new Boolean[1];
		display.syncExec(new Runnable() {
			public void run() {
				Shell shell = display.getActiveShell();
				boolean install = MessageDialog.openQuestion(shell, Messages.framework_not_instrumented,
						Messages.framework_not_instrumented_msg);
				result[0] = new Boolean(install);
			}
		});
		return result[0] != null && result[0].booleanValue();
	}

}
