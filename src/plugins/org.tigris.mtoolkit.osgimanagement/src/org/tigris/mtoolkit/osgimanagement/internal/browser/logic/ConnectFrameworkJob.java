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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.Util;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.FrameworkPanel;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.FrameworkPanel.DeviceTypeProviderElement;
import org.tigris.mtoolkit.osgimanagement.model.Framework;

public class ConnectFrameworkJob extends Job {
	private static List connectingFrameworks = new ArrayList();

	private Framework fw;

	public ConnectFrameworkJob(Framework framework) {
		super(NLS.bind(Messages.connect_framework, framework.getName()));
		this.fw = framework;
	}

	public IStatus run(final IProgressMonitor monitor) {
		monitor.beginTask(NLS.bind(Messages.connect_framework, fw.getName()), 1);

		synchronized (connectingFrameworks) {
			// there is already job for this fw, so wait that job
			// otherwise, start connecting
			if (connectingFrameworks.contains(fw)) {
				do {
					try {
						connectingFrameworks.wait();
					} catch (InterruptedException e) {
					}
					if (monitor.isCanceled()) {
						monitor.done();
						return Status.CANCEL_STATUS;
					}
				} while (connectingFrameworks.contains(fw));
				monitor.done();
				if (fw.isConnected()) {
					return Status.OK_STATUS;
				} else {
					return Util.newStatus(IStatus.ERROR,
						"Could not connect to framework " + fw.getName(),
						null);
				}
			}
			connectingFrameworks.add(fw);
		}

		DeviceConnector connector = fw.getConnector();
		final boolean canceled[] = new boolean[] {false};
		try {
			if (connector != null && connector.isActive()) {
				FrameworkConnectorFactory.createPMPConnection(connector, (FrameworkImpl)fw, fw.getName(), ((FrameworkImpl)fw).autoConnected);
			} else {
				IMemento config = ((FrameworkImpl) fw).getConfig();
				String id = null;
				String transportType = null;
				Dictionary aConnProps = null;
				
				String providerID = config.getString(ConstantsDistributor.TRANSPORT_PROVIDER_ID);
				List providers = FrameworkPanel.obtainDeviceTypeProviders(null);
				for (int i=0; i<providers.size(); i++) {
					DeviceTypeProviderElement provider = (DeviceTypeProviderElement) providers.get(i);
					if (providerID.equals(provider.getTypeId())) {
						try {
							transportType = provider.getProvider().getTransportType();
							aConnProps = provider.getProvider().load(config);
							id = (String) aConnProps.get(Framework.FRAMEWORK_ID);
						} catch (CoreException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
					}
				}

				if (transportType != null && id != null) {
					if (aConnProps == null) {
						aConnProps = new Hashtable();
					}
					final DeviceConnector conn[] = new DeviceConnector[1];
					final String rTransportType = transportType;
					final String rID = id;
					final Dictionary rConnProps = aConnProps;
					final IStatus rStatus[] = new IStatus[1];
					new Thread() {
						public void run() {
							try {
								conn[0] = DeviceConnector.connect(rTransportType, rID, rConnProps, null);
								FrameworkConnectorFactory.connectFramework(conn[0], fw.getName());
							} catch (IAgentException e) {
								if (monitor.isCanceled() || canceled[0]) {
									return;
								}
								if (e.getErrorCode() == IAgentErrors.ERROR_CANNOT_CONNECT) {
									handleConnectionFailure(e);
									monitor.setCanceled(true);
								} else {
									rStatus[0] = Util.handleIAgentException(e);
								}
							} catch (IllegalStateException e) {
								rStatus[0] = Util.handleIAgentException(new IAgentException(e.getMessage(), IAgentErrors.ERROR_CANNOT_CONNECT, e));
							}
						}
					}.start();
					while (conn[0] == null && rStatus[0] == null) {
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						if (monitor.isCanceled()) {
							monitor.done();
							canceled[0] = true;
							return Status.CANCEL_STATUS;
						}
					}
					if (rStatus[0] != null) {
						monitor.done();
						return rStatus[0];
					}
				} else {
					errorProviderNotFound();
				}
			}
		} finally {
			// remove the framework in any case
			synchronized (connectingFrameworks) {
				connectingFrameworks.remove(fw);
				connectingFrameworks.notifyAll();
			}
		}
		monitor.done();
		return Status.OK_STATUS;
	}

	private static void errorProviderNotFound() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				MessageDialog.openError(shell, "Error", "Could not connect to framework. The selected " +
						"connection type provider is no more available. Please select another connection type.");
			}
		});
	}

	public static boolean isConnecting(FrameworkImpl fw) {
		synchronized (connectingFrameworks) {
			return connectingFrameworks.contains(fw);
		}
	}

	protected void handleConnectionFailure(final IAgentException e) {
		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.asyncExec(new Runnable() {
			public void run() {
				String[] buttons = { Messages.close_button_label, Messages.get_iagent_button_label };
				String message = Messages.connection_failed;
				if (e != null) {//add cause for connection failed
					message += "\nCause: " + e.getMessage();
					Throwable cause = e.getCauseException();
					if (cause != null) {
						message +=  " (" + cause.getLocalizedMessage() + ")";
					}
				}
				message += "\n\n" + Messages.rcp_bundle_missing_message;
				MessageDialog dialog = new MessageDialog(FrameWorkView.getShell(),
					Messages.rcp_bundle_missing_title,
					null,
					message,
					MessageDialog.INFORMATION,
					buttons,
					0);
				dialog.setBlockOnOpen(true);
				dialog.open();
				if (dialog.getReturnCode() == 1) {
					// get IAgent button has been selected
					InputStream iagentInput = FrameworkPlugin.getIAgentBundleAsStream();
					OutputStream output = null;
					try {
						if (iagentInput == null)
							// TODO: Add dialog here
							return;

						FileDialog saveDialog = new FileDialog(display.getActiveShell(), SWT.SAVE);
						saveDialog.setText(Messages.save_as_dialog_title);
						String[] filterExt = { "*.jar" }; //$NON-NLS-1$
						saveDialog.setFilterExtensions(filterExt);
						// TODO: initial filename setting doesn't work on Mac OS
						// X
						saveDialog.setFileName("iagent.rpc.jar");
						String path = saveDialog.open();
						if (path == null) return;
						output = new FileOutputStream(path);

						int bytesRead = 0;
						byte[] buffer = new byte[1024];

						while ((bytesRead = iagentInput.read(buffer)) != -1) {
							output.write(buffer, 0, bytesRead);
						}

					} catch (IOException e1) {
						StatusManager.getManager().handle(Util.newStatus(IStatus.ERROR,
							"An error occurred while saving IAgent bundle",
							e1));
					} finally {
						if (output != null)
							try {
								output.close();
							} catch (IOException e) {
							}
						if (iagentInput != null)
							try {
								iagentInput.close();
							} catch (IOException e) {
							}
					}
				}
			}
		});

	}
}
