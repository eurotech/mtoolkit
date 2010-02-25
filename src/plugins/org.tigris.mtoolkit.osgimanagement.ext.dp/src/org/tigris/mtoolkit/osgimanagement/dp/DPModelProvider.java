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
package org.tigris.mtoolkit.osgimanagement.dp;

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ui.statushandlers.StatusManager;
import org.tigris.mtoolkit.iagent.DeploymentManager;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteDP;
import org.tigris.mtoolkit.iagent.event.RemoteDPEvent;
import org.tigris.mtoolkit.iagent.event.RemoteDPListener;
import org.tigris.mtoolkit.iagent.event.RemoteDevicePropertyEvent;
import org.tigris.mtoolkit.iagent.event.RemoteDevicePropertyListener;
import org.tigris.mtoolkit.iagent.rpc.Capabilities;
import org.tigris.mtoolkit.osgimanagement.ContentTypeModelProvider;
import org.tigris.mtoolkit.osgimanagement.dp.model.DeploymentPackage;
import org.tigris.mtoolkit.osgimanagement.model.Framework;
import org.tigris.mtoolkit.osgimanagement.model.Model;
import org.tigris.mtoolkit.osgimanagement.model.SimpleNode;

public class DPModelProvider implements ContentTypeModelProvider, RemoteDPListener, RemoteDevicePropertyListener {

	private static SimpleNode dpNode;
	private DeviceConnector connector;
	private Model parent;
	private DeploymentManager manager;
	private boolean supportDP;
	public static final Dictionary<DeviceConnector, Boolean> supportDPDictionary = new Hashtable<DeviceConnector, Boolean>();
	
	public Model connect(Model parent, DeviceConnector connector, IProgressMonitor monitor) {
		this.connector = connector;
		this.parent = parent;

		supportDP = isDpSupported(connector);
		supportDPDictionary.put(connector, supportDP);

		try {
			connector.addRemoteDevicePropertyListener(this);
		} catch (IAgentException e1) {
			e1.printStackTrace();
		}
		
		if (supportDP) {
			initModel(monitor);
		}
		return dpNode;
	}

	private void initModel(IProgressMonitor monitor) {
		dpNode = new SimpleNode("Deployment Packages");
		if (parent.findFramework().getViewType() == Framework.BUNDLES_VIEW) { 
			parent.addElement(dpNode);
		}
		try {
			manager = connector.getDeploymentManager();
			addDPs(monitor);
			try {
				manager.addRemoteDPListener(this);
			} catch (IAgentException e) {
				e.printStackTrace();
			}
		} catch (IAgentException e) {
			e.printStackTrace();
		}
	}


	public void disconnect() {
		if (manager != null) {
			try {
				manager.removeRemoteDPListener(this);
			} catch (IAgentException e) {
				e.printStackTrace();
			}
		}
		if (parent != null) {
			if (dpNode != null) {
				parent.removeElement(dpNode);
			}
			parent = null;
		}
		connector = null;
		dpNode = null;
		supportDP = false;
	}

	public Model switchView(int viewType) {
		Model node = null;
		if (supportDP) {
			if (viewType == Framework.BUNDLES_VIEW) {
				parent.addElement(dpNode);
				node = dpNode;
			} else if (viewType == Framework.SERVICES_VIEW) {
				parent.removeElement(dpNode);
			}
		}
		return node;
	}

	private void addDPs(IProgressMonitor monitor) throws IAgentException {
		if (!supportDP) {
			return;
		}
		Model deplPackagesNode = dpNode;
		RemoteDP dps[] = null;
		dps = connector.getDeploymentManager().listDeploymentPackages();

		if (dps != null && dps.length > 0) {
			SubMonitor sMonitor = SubMonitor.convert(monitor, dps.length);
			sMonitor.setTaskName("Retrieving deployment packages information...");

			for (int i = 0; i < dps.length; i++) {
				DeploymentPackage dpNode = new DeploymentPackage(dps[i], (Framework) parent);
				deplPackagesNode.addElement(dpNode);
				monitor.worked(1);
				if (monitor.isCanceled()) {
					return;
				}
			}
		}
	}

	public void deploymentPackageChanged(final RemoteDPEvent e) {
		try {
			StatusManager.getManager().handle(new Status(IStatus.INFO, Activator.PLUGIN_ID, 
				"Deployment package changed " + e.getDeploymentPackage().getName() + " " + (e.getType() == RemoteDPEvent.INSTALLED ? "INSTALLED" : "UNINSTALLED")));
		} catch (IAgentException e2) {
		}

		synchronized (Framework.getLockObject(connector)) {
			try {
				RemoteDP remoteDP = e.getDeploymentPackage();
				if (e.getType() == RemoteDPEvent.INSTALLED) {
					Model dpNodeRoot = dpNode;
					try {
						// check if this install actually is update
						DeploymentPackage dp = findDP(remoteDP.getName());
						if (dp != null) {
							dpNode.removeElement(dp);
						}

						DeploymentPackage dpNode = new DeploymentPackage(remoteDP, (Framework) parent);
						dpNodeRoot.addElement(dpNode);
					} catch (IAgentException e1) {
						if (e1.getErrorCode() != IAgentErrors.ERROR_DEPLOYMENT_STALE
								&& e1.getErrorCode() != IAgentErrors.ERROR_REMOTE_ADMIN_NOT_AVAILABLE) {
							StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e1.getMessage(), e1));
						}
					}
				} else if (e.getType() == RemoteDPEvent.UNINSTALLED) {
					if (remoteDP != null) {
						// there are cases where the dp failed to be added,
						// because it was too quickly uninstalled/updated
						DeploymentPackage dp = findDP(remoteDP.getName());
						if (dp != null) {
							dpNode.removeElement(dp);
						}
					}
				}

				dpNode.updateElement();
			} catch (IllegalStateException ex) {
				// ignore state exceptions, which usually indicates that something
				// is was fast enough to disappear
			} catch (Throwable t) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, t.getMessage(), t));
			}
		}
	}

	public static DeploymentPackage findDP(String name) {
		Model[] dps = dpNode.getChildren();
		for (int i=0; i<dps.length; i++) {
			if (dps[i].getName().equals(name)) {
				return (DeploymentPackage) dps[i];
			}
		}
		return null;
	}

	public void devicePropertiesChanged(RemoteDevicePropertyEvent e) throws IAgentException {
		if (e.getType() == RemoteDevicePropertyEvent.PROPERTY_CHANGED_TYPE) {
			boolean enabled = ((Boolean) e.getValue()).booleanValue();
			Object property = e.getProperty();
			if (Capabilities.DEPLOYMENT_SUPPORT.equals(property)) {
				supportDPDictionary.put(connector, enabled);
				if (enabled) {
					supportDP = true;
					initModel(new NullProgressMonitor());
				} else {
					supportDP = false;
					try {
						manager.removeRemoteDPListener(this);
					} catch (IAgentException ex) {
						ex.printStackTrace();
					}
					if (dpNode != null) {
						dpNode.removeChildren();
						parent.removeElement(dpNode);
						dpNode = null;
					}
				}
			}
		}
	}

	public Model getResource(String id, String version, Framework fw) throws IAgentException {
		return null;
	}

	public String[] getSupportedMimeTypes() {
		return null;
	}

	public static boolean isDpSupported(DeviceConnector connector) {
		if (connector == null) {
			return false;
		}
		Dictionary connectorProperties = connector.getProperties();
		Object support = connectorProperties.get(Capabilities.CAPABILITIES_SUPPORT);
		if (support == null || !Boolean.valueOf(support.toString()).booleanValue()) {
			return true;
		} else {
			support = connectorProperties.get(Capabilities.DEPLOYMENT_SUPPORT);
			if (support != null && Boolean.valueOf(support.toString()).booleanValue()) {
				return true;
			}
		}
		return false;
	}
}
