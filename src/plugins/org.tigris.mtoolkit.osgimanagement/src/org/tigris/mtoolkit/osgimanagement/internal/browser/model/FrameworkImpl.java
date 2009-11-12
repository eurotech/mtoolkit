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
package org.tigris.mtoolkit.osgimanagement.internal.browser.model;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.tigris.mtoolkit.iagent.DeploymentManager;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.iagent.RemoteDP;
import org.tigris.mtoolkit.iagent.RemoteService;
import org.tigris.mtoolkit.iagent.event.RemoteBundleEvent;
import org.tigris.mtoolkit.iagent.event.RemoteBundleListener;
import org.tigris.mtoolkit.iagent.event.RemoteDPEvent;
import org.tigris.mtoolkit.iagent.event.RemoteDPListener;
import org.tigris.mtoolkit.iagent.event.RemoteServiceEvent;
import org.tigris.mtoolkit.iagent.event.RemoteServiceListener;
import org.tigris.mtoolkit.osgimanagement.ContentTypeModelProvider;
import org.tigris.mtoolkit.osgimanagement.browser.model.Framework;
import org.tigris.mtoolkit.osgimanagement.browser.model.Model;
import org.tigris.mtoolkit.osgimanagement.browser.model.SimpleNode;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.PMPConnectionListener;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.ActionsManager;

public class FrameworkImpl extends Framework implements RemoteBundleListener, RemoteDPListener, RemoteServiceListener {

	private boolean showServicePropertiesInTree = false;

	public Hashtable bundleHash;
	public Hashtable categoryHash;
	public Hashtable dpHash;

	// stores services info during connect action
	public Vector servicesVector;
	// stores services view nodes
	public Vector servicesViewVector;

	private Model bundles;
	private Model deplPackages;

	public boolean autoConnected;
	private Display display;

	// public IProgressMonitor monitor;
	private boolean refreshing = false;
	private boolean connecting = false;

	private PMPConnectionListener connectionListener;

	// flag indicates user has forced disconnect action
	public boolean userDisconnect = false;

	public FrameworkImpl(String name, boolean autoConnected) {
		super(name);
		this.autoConnected = autoConnected;
		configs = XMLMemento.createWriteRoot(MEMENTO_TYPE);
		configs.putString(FRAMEWORK_NAME, name);
	}

	public Model getBundlesNode() {
		if (bundles == null) {
			bundles = new SimpleNode(Messages.bundles_node_label);
			if (getViewType() == BUNDLES_VIEW)
				addElement(bundles);
		}
		return bundles;
	}

	public Model getDPNode() {
		if (deplPackages == null) {
			deplPackages = new SimpleNode(Messages.dpackages_node_label);
			if (getViewType() == BUNDLES_VIEW)
				addElement(deplPackages);
		}
		return deplPackages;
	}

	public void setConfig(IMemento configs) {
		this.configs = configs;
	}

	public void dispose() {
		if (this.isConnected()) {
			this.disconnect();
		}
		FrameWorkView.treeRoot.removeElement(this);
		bundleHash = null;
		categoryHash = null;
		dpHash = null;
		servicesVector = null;
		servicesViewVector = null;
	}

	public boolean isConnecting() {
		return connecting;
	}

	// private void refreshViewers() {
	// display = Display.getDefault();
	// if (display == null)
	// display = Display.getCurrent();
	// display.asyncExec(new Runnable() {
	// public void run() {
	// TreeViewer[] all = FrameWorkView.getTreeViewers();
	// if (all != null) {
	// for (int i = 0; i < all.length; i++) {
	// if (!all[i].getControl().isDisposed()) {
	// all[i].refresh();
	// }
	// }
	// }
	// }
	// });
	// }

	public void setConnector(DeviceConnector connector) {
		this.connector = connector;
		if (connector == null) {
			connectedFlag = false;
		}
	}

	public void connect(final DeviceConnector connector, IProgressMonitor monitor) {
		this.connector = connector;
		this.connectedFlag = true;
		boolean success = initModel(monitor);
		if (!success) {
			ActionsManager.disconnectFrameworkAction(FrameworkImpl.this);
		}
	}

	private void addRemoteListeners() {
		try {
			DeploymentManager deploymentManager = connector.getDeploymentManager();
			if (deploymentManager != null) {
				deploymentManager.addRemoteBundleListener(this);
				deploymentManager.addRemoteDPListener(this);
			}
			connector.getServiceManager().addRemoteServiceListener(this);
		} catch (IAgentException e) {
			BrowserErrorHandler.processError(e, connector, userDisconnect);
		}
	}

	private void removeRemoteListeners() {
		try {
			DeploymentManager deploymentManager = connector.getDeploymentManager();
			if (deploymentManager != null) {
				deploymentManager.removeRemoteBundleListener(this);
				deploymentManager.removeRemoteDPListener(this);
			}
			connector.getServiceManager().removeRemoteServiceListener(this);
		} catch (IAgentException e) {
			BrowserErrorHandler.processError(e, connector, userDisconnect);
		}
	}

	private boolean buildModel(IProgressMonitor monitor) {
		try {
			addBundles(monitor);
			if (monitor.isCanceled())
				return false;

			obtainModelProviders();

			int modelTotal = modelProviders.size() + 1;
			modelTotal = (int) (FrameworkConnectorFactory.CONNECT_PROGRESS_ADDITIONAL / modelTotal);
			try {
				addDPs(monitor, modelTotal);
				if (monitor.isCanceled())
					return false;
			} catch (IAgentException e) {
				// if deployment admin was not found log only
				// warning
				if (e.getErrorCode() == IAgentErrors.ERROR_REMOTE_ADMIN_NOT_AVAILABLE) {
					FrameworkPlugin.log(FrameworkPlugin.newStatus(IStatus.WARNING, NLS.bind(
							"Remote framework {0} doesn't support deployment packages", this.getName()), e));
				} else {
					BrowserErrorHandler.processError(e, connector, userDisconnect);
				}
			}

			monitor.subTask("Deploy services info");
			addServices(monitor);
			if (monitor.isCanceled())
				return false;

			monitor.subTask("Retrieve additional providers data");
			for (int i = 0; i < modelProviders.size(); i++) {
				ContentTypeModelProvider manager = ((ModelProviderElement) modelProviders.get(i)).getProvider();
				Model node = manager.connect(this, connector);
				if (monitor.isCanceled())
					return false;
				monitor.worked(modelTotal);
			}

		} catch (IAgentException e) {
			BrowserErrorHandler.processError(e, connector, userDisconnect);
			return false;
		} catch (IllegalStateException ise) {
			// connection was closed
			return false;
		}

		return true;
	}

	private boolean initModel(IProgressMonitor monitor) {
		synchronized (FrameworkConnectorFactory.getLockObject(connector)) {
			try {
				connecting = true;
				userDisconnect = false;
				if (connector != null) {
					bundleHash = new Hashtable();
					categoryHash = new Hashtable();
					servicesVector = new Vector();
					servicesViewVector = new Vector();
					dpHash = new Hashtable();

					if (monitor != null && monitor.isCanceled())
						return false;
					addRemoteListeners();
					updateElement();
					buildModel(monitor);
				}
				updateContextMenuStates();
			} catch (Throwable t) {
				FrameworkPlugin.error("Unexpected exception occurred while connecting to remote framework", t);
			} finally {
				connecting = false;
			}
			if (!refreshing) {
				BrowserErrorHandler.processInfo(name + " successfully " + "connected", false); //$NON-NLS-1$
			}
		}
		return true;
	}

	public void disconnect() {
		synchronized (FrameworkConnectorFactory.getLockObject(connector)) {
			if (connector != null) {
				removeRemoteListeners();
			}

			for (int i = 0; i < modelProviders.size(); i++) {
				((ModelProviderElement) modelProviders.get(i)).getProvider().disconnect();
			}

			clearModel();
			connector = null;
			connectedFlag = false;
			updateElement();
			updateContextMenuStates();
			BrowserErrorHandler.processInfo(name + " successfully " + "disconnected", false); //$NON-NLS-1$
		}
	}

	private void clearModel() {
		modelProviders.clear();
		removeChildren();
		deplPackages = null;
		bundles = null;
		bundleHash.clear();
		categoryHash.clear();
		servicesVector.removeAllElements();
		servicesViewVector.removeAllElements();
		dpHash.clear();
		ServiceObject.usedInHashFWs.remove(this);
	}

	public Bundle findBundle(long id) {
		if (bundleHash == null)
			return null;
		return (Bundle) bundleHash.get(new Long(id));
	}

	public Bundle findBundleForService(long id) throws IAgentException {
		if (bundleHash == null)
			return null;
		Object bundles[] = bundleHash.values().toArray();
		for (int i = 0; i < bundles.length; i++) {
			Model serviceCategories[] = ((Bundle) bundles[i]).getChildren();
			if (serviceCategories != null && serviceCategories.length > 0) {
				Object services[] = serviceCategories[0].getChildren();
				if (services != null) {
					for (int j = 0; j < services.length; j++) {
						if (((ObjectClass) services[j]).getService().getServiceId() == id) {
							return (Bundle) bundles[i];
						}
					}
				}
			}
		}
		return null;
	}

	public Bundle findBundleInDP(long id) {
		Enumeration deps = dpHash.elements();
		while (deps.hasMoreElements()) {
			DeploymentPackage dp = (DeploymentPackage) deps.nextElement();
			Model bundles[] = dp.getChildren();
			for (int i = 0; i < bundles.length; i++) {
				if (((Bundle) bundles[i]).getID() == id) {
					return ((Bundle) bundles[i]);
				}
			}
		}
		return null;
	}

	public Bundle findBundle(Object id) {
		if (bundleHash == null)
			return null;
		return (Bundle) bundleHash.get(id);
	}

	public DeploymentPackage findDP(String name) {
		if (dpHash == null)
			return null;
		return (DeploymentPackage) dpHash.get(name);
	}

	public Set getBundlesKeys() {
		if ((bundleHash == null) || (bundleHash.size() == 0)) {
			return Collections.EMPTY_SET;
		}
		return Collections.unmodifiableSet(bundleHash.keySet());
	}

	// Overrides method in Model class
	public boolean testAttribute(Object target, String name, String value) {
		if (!(target instanceof org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl)) {
			return false;
		}

		if (name.equalsIgnoreCase(FRAMEWORK_STATUS_NAME)) {
			if (value.equalsIgnoreCase(FRAMEWORK_CONNECT_VALUE)) {
				return this.isConnected();
			}
		}
		return false;
	}

	public void setViewType(int viewType) {
		if (this.viewType == viewType)
			return;
		this.viewType = viewType;

		Model[] children = getChildren();
		for (int i = 0; i < children.length; i++) {
			removeElement(children[i]);
		}

		if (viewType == FrameworkImpl.SERVICES_VIEW) {
			for (int i = 0; i < servicesViewVector.size(); i++) {
				addElement((Model) servicesViewVector.elementAt(i));
			}
		} else {
			Model bundlesNode = getBundlesNode();
			Model dpNode = getDPNode();
			addElement(bundlesNode);
			addElement(dpNode);
		}
		updateElement();

		for (int i = 0; i < modelProviders.size(); i++) {
			((ModelProviderElement) modelProviders.get(i)).getProvider().switchView(viewType);
		}
		updateElement();
	}

	protected boolean isShowBundlesID() {
		TreeRoot root = (TreeRoot) getParent();
		if (root != null)
			return root.isShowBundlesID();
		return false;
	}

	protected boolean isShowBundlesVersion() {
		TreeRoot root = (TreeRoot) getParent();
		if (root != null)
			return root.isShowBundlesVersion();
		return false;
	}

	public void setShowServicePropertiesInTree(boolean state) {
		showServicePropertiesInTree = state;
	}

	public boolean isShownServicePropertiss() {
		return showServicePropertiesInTree;
	}

	// find and update nodes for bundle in "Bundles" and "Deployment packages"
	private void updateBundleNodes(RemoteBundle rBundle) throws IAgentException {
		long id = rBundle.getBundleId();
		Bundle node = findBundle(id);
		if (node != null) {
			node.update();
			node = findBundleInDP(id);
			if (node != null) {
				node.update();
			}
		}

		for (int i = 0; servicesViewVector != null && i < servicesViewVector.size(); i++) {
			Model service = (Model) servicesViewVector.elementAt(i);
			Model children[] = service.getChildren();
			for (int j = 0; j < children.length; j++) {
				Model bundles[] = children[j].getChildren();
				for (int k = 0; k < bundles.length; k++) {
					Bundle bundle = (Bundle) bundles[k];
					if (bundle.getID() == id) {
						bundle.update();
					}
				}
			}
		}
	}

	private void udpateBundleCategory(RemoteBundle rBundle) throws IAgentException {
		long id = rBundle.getBundleId();

		// remove bundle and empty category
		Bundle bundle = findBundle(id);
		bundleHash.remove(new Long(id));

		if (bundle != null && FrameworkConnectorFactory.isBundlesCategoriesShown) {
			Category category = (Category) bundle.getParent();
			category.removeElement(bundle);
			if (category.getSize() == 0) {
				categoryHash.remove(category.getName());
				category.getParent().removeElement(category);
			}
		}
		removeBundleInServicesView(id);
		addBundle(rBundle);
	}

	private void removeBundle(long id) throws IAgentException {
		// remove bundle and empty category
		Bundle bundle = findBundle(id);
		FrameworkImpl fw = (FrameworkImpl) bundle.findFramework();
		bundleHash.remove(new Long(id));
		if (bundle != null) {
			if (FrameworkConnectorFactory.isBundlesCategoriesShown) {
				Category category = (Category) bundle.getParent();
				category.removeElement(bundle);
				if (category.getSize() == 0) {
					categoryHash.remove(category.getName());
					category.getParent().removeElement(category);
				}
			} else {
				fw.getBundlesNode().removeElement(bundle);
			}
			// remove bundle from DP
			bundle = findBundleInDP(bundle.getID());
			if (bundle != null) {
				bundle.getParent().removeElement(bundle);
			}
		}

		removeBundleInServicesView(id);
	}

	private void removeBundleInServicesView(long id) {
		// remove bundles from services view nodes
		for (int i = servicesViewVector.size() - 1; i >= 0; i--) {
			ObjectClass objClass = (ObjectClass) servicesViewVector.elementAt(i);
			Model children[] = objClass.getChildren();
			for (int j = 0; j < children.length; j++) {
				Model bundles[] = children[j].getChildren();
				for (int k = 0; k < bundles.length; k++) {
					if (((Bundle) bundles[k]).getID() == id) {
						children[j].removeElement(bundles[k]);
						if (j == 0)
							servicesViewVector.removeElementAt(i);
						break;
					}
				}
			}
		}
	}

	private String getDebugBundleChangedMsg(RemoteBundleEvent e) {
		int type = e.getType();
		String debug = "Bundle state changed " + type; //$NON-NLS-1$

		try {
			if (type == RemoteBundleEvent.UNINSTALLED) {
				debug = "Bundle " + e.getBundle().getBundleId() + " uninstalled"; //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				debug = "Bundle " + getBundleName(e.getBundle(), null) + " "; //$NON-NLS-1$ //$NON-NLS-2$
				switch (type) {
				case (RemoteBundleEvent.INSTALLED): {
					debug += "INSTALLED"; //$NON-NLS-1$
					break;
				}
				case (RemoteBundleEvent.LAZY_STARTED): {
					debug += "LAZY_STARTED"; //$NON-NLS-1$
					break;
				}
				case (RemoteBundleEvent.RESOLVED): {
					debug += "RESOLVED"; //$NON-NLS-1$
					break;
				}
				case (RemoteBundleEvent.STARTED): {
					debug += "STARTED"; //$NON-NLS-1$
					break;
				}
				case (RemoteBundleEvent.STOPPED): {
					debug += "STOPPED"; //$NON-NLS-1$
					break;
				}
				case (RemoteBundleEvent.UNRESOLVED): {
					debug += "UNRESOLVED"; //$NON-NLS-1$
					break;
				}
				case (RemoteBundleEvent.UPDATED): {
					debug += "UPDATED"; //$NON-NLS-1$
					break;
				}
				case (RemoteBundleEvent.STARTING): {
					debug += "STARTING"; //$NON-NLS-1$
					break;
				}
				case (RemoteBundleEvent.STOPPING): {
					debug += "STOPPING"; //$NON-NLS-1$
					break;
				}
				}
			}
		} catch (IAgentException e1) {
		}
		return debug;
	}

	public void bundleChanged(RemoteBundleEvent e) {
		BrowserErrorHandler.debug(getDebugBundleChangedMsg(e));

		if (!isConnected())
			return;

		final int type = e.getType();

		if (isConnecting()) {
			synchronized ((FrameworkConnectorFactory.getLockObject(connector))) {
			}
		}

		final RemoteBundle rBundle = e.getBundle();
		long id = rBundle.getBundleId();
		try {
			if (!bundleHash.containsKey(new Long(id)) && type != RemoteBundleEvent.UNINSTALLED) {
				// add this bundle - apparently, we have missed the INSTALLED
				// event
				addBundle(rBundle);
			} else if (type == RemoteBundleEvent.UPDATED) {
				Bundle bundle = findBundle(id);
				String category = rBundle.getHeader("Bundle-Category", ""); //$NON-NLS-1$ //$NON-NLS-2$
				if (!bundle.getParent().getName().equals(category)
						&& FrameworkConnectorFactory.isBundlesCategoriesShown) {
					udpateBundleCategory(rBundle);
				}
				bundle = findBundle(id);
				String newName = getBundleName(rBundle, null);
				bundle.setName(newName);
				bundle = findBundleInDP(id);
				if (bundle != null) {
					bundle.setName(newName);
				}
				updateBundleNodes(rBundle);
			} else if (type == RemoteBundleEvent.UNINSTALLED) {
				removeBundle(rBundle.getBundleId());
			} else if (type == RemoteBundleEvent.STOPPED) {
				Bundle bundle = findBundle(rBundle.getBundleId());
				bundle.removeChildren();
				bundle = findBundleInDP(bundle.getID());
				if (bundle != null) {
					bundle.removeChildren();
				}
				removeBundleInServicesView(rBundle.getBundleId());
				updateBundleNodes(rBundle);
			} else if (type == RemoteBundleEvent.STARTED) {
				Bundle bundle = findBundle(rBundle.getBundleId());
				Bundle dpBundle = findBundleInDP(rBundle.getBundleId());

				bundle.setState(org.osgi.framework.Bundle.ACTIVE);
				if (dpBundle != null)
					dpBundle.setState(org.osgi.framework.Bundle.ACTIVE);

				RemoteService usedServ[] = bundle.getRemoteBundle().getServicesInUse();
				if (usedServ.length > 0) {
					Model bundleUsedInCategory = getServiceCategoryNode(bundle, ServicesCategory.USED_SERVICES, true);
					Model dpBundleCategory = dpBundle == null ? null : getServiceCategoryNode(dpBundle,
							ServicesCategory.USED_SERVICES, true);

					for (int i = 0; i < usedServ.length; i++) {
						createObjectClassNodes(bundleUsedInCategory, usedServ[i].getObjectClass(), new Long(usedServ[i]
								.getServiceId()), usedServ[i]);
						if (dpBundleCategory != null)
							createObjectClassNodes(dpBundleCategory, usedServ[i].getObjectClass(), new Long(usedServ[i]
									.getServiceId()), usedServ[i]);

						for (int j = 0; j < servicesViewVector.size(); j++) {
							ObjectClass oc = (ObjectClass) servicesViewVector.elementAt(j);
							if (oc.getNameID().longValue() == usedServ[i].getServiceId()) {
								BundlesCategory bCategory = (BundlesCategory) oc.getChildren()[1];
								Model bundles[] = bCategory.getChildren();
								boolean added = false;
								for (int k = 0; k < bundles.length; k++) {
									if (((Bundle) bundles[k]).getID() == bundle.getID()) {
										added = true;
										break;
									}
								}
								if (!added) {
									Bundle usedInBundle = new Bundle(bundle.getName(), bundle.getRemoteBundle(), bundle
											.getState(), bundle.getType(), bundle.getCategory());
									if (FrameworkConnectorFactory.isBundlesCategoriesShown)
										bCategory.addElement(usedInBundle);
									else
										((FrameworkImpl) bCategory.findFramework()).getBundlesNode().addElement(
												usedInBundle);
								}
							}
						}

					}
				}
				updateBundleNodes(rBundle);
			} else {
				updateBundleNodes(rBundle);
			}
		} catch (IAgentException ex) {
			// ignore bundle installed exceptions, we will receive uninstalled
			// event shortly
			if (ex.getErrorCode() != IAgentErrors.ERROR_BUNDLE_UNINSTALLED) {
				BrowserErrorHandler.processError(ex, connector, userDisconnect);
			}
		}
		updateContextMenuStates();
	}

	public void deploymentPackageChanged(final RemoteDPEvent e) {
		try {
			BrowserErrorHandler
					.debug("Deployment package changed " + e.getDeploymentPackage().getName() + " " + (e.getType() == RemoteDPEvent.INSTALLED ? "INSTALLED" : "UNINSTALLED"));} catch (IAgentException e2) {} //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		if (!isConnected())
			return;

		if (isConnecting()) {
			synchronized (FrameworkConnectorFactory.getLockObject(connector)) {
			}
		}

		try {
			RemoteDP remoteDP = e.getDeploymentPackage();
			if (e.getType() == RemoteDPEvent.INSTALLED) {
				Model dpNodeRoot = getDPNode();
				try {
					// check if this install actually is update
					DeploymentPackage dp = findDP(remoteDP.getName());
					if (dp != null) {
						getDPNode().removeElement(dp);
					}

					DeploymentPackage dpNode = new DeploymentPackage(remoteDP, FrameworkImpl.this);
					dpNodeRoot.addElement(dpNode);
					dpHash.put(remoteDP.getName(), dpNode);
				} catch (IAgentException e1) {
					if (e1.getErrorCode() != IAgentErrors.ERROR_DEPLOYMENT_STALE
							&& e1.getErrorCode() != IAgentErrors.ERROR_REMOTE_ADMIN_NOT_AVAILABLE) {
						BrowserErrorHandler.processError(e1, connector, userDisconnect);
					}
				}
			} else if (e.getType() == RemoteDPEvent.UNINSTALLED) {
				try {
					if (remoteDP != null) {
						DeploymentPackage dpNode = (DeploymentPackage) dpHash.remove(remoteDP.getName());
						// there are cases where the dp failed to be added,
						// because it was too quickly uninstalled/updated
						if (dpNode != null) {

							getDPNode().removeElement(dpNode);
						}
					}
				} catch (IAgentException e1) {
					e1.printStackTrace();
					BrowserErrorHandler.processError(e1, connector, userDisconnect);
				}
			}

			updateElement();
		} catch (IllegalStateException ex) {
			// ignore state exceptions, which usually indicates that something
			// is was fast enough to disappear
			BrowserErrorHandler.debug(ex);
		} catch (Throwable t) {
			t.printStackTrace();
			BrowserErrorHandler.processError(t, connector, userDisconnect);
		}
		updateContextMenuStates();
	}

	public void removeService(long id) throws IAgentException {
		try {
			Enumeration bundlesKeys = bundleHash.keys();
			while (bundlesKeys.hasMoreElements()) {
				Object key = bundlesKeys.nextElement();
				Bundle bundle = (Bundle) bundleHash.get(key);
				Model children[] = bundle.getChildren();
				for (int i = 0; i < children.length; i++) {
					Model services[] = children[i].getChildren();
					for (int j = 0; j < services.length; j++) {
						if (((ObjectClass) services[j]).getNameID().longValue() == id) {
							children[i].removeElement(services[j]);
						}
					}
				}
			}

			Enumeration dpKeys = dpHash.keys();
			while (dpKeys.hasMoreElements()) {
				Object key = dpKeys.nextElement();
				DeploymentPackage dp = (DeploymentPackage) dpHash.get(key);
				Model bundles[] = dp.getChildren();
				for (int i = 0; i < bundles.length; i++) {
					Bundle bundle = (Bundle) bundles[i];

					Model children[] = bundle.getChildren();
					for (int j = 0; j < children.length; j++) {
						Model services[] = children[j].getChildren();
						for (int k = 0; k < services.length; k++) {
							if (((ObjectClass) services[k]).getNameID().longValue() == id) {
								children[j].removeElement(services[k]);
							}
						}
					}
				}
			}

			for (int i = servicesViewVector.size() - 1; i >= 0; i--) {
				ObjectClass objClass = (ObjectClass) servicesViewVector.elementAt(i);
				if (objClass.getService().getServiceId() == id) {
					if (viewType == SERVICES_VIEW) {
						removeElement(objClass);
					}
					servicesViewVector.removeElementAt(i);
				}
			}
		} catch (Throwable t) {
			BrowserErrorHandler.processError(t, true);
		}
	}

	private String getDebugServiceChangedMsg(RemoteServiceEvent e) {
		String type = ""; //$NON-NLS-1$
		try {
			switch (e.getType()) {
			case RemoteServiceEvent.UNREGISTERED: {
				type = "UNREGISTERED"; //$NON-NLS-1$
				break;
			}
			case RemoteServiceEvent.MODIFIED: {
				type = "MODIFIED"; //$NON-NLS-1$
				break;
			}
			case RemoteServiceEvent.REGISTERED: {
				type = "REGISTERED"; //$NON-NLS-1$
				break;
			}

			}
			return "Service id = " + e.getService().getServiceId() + " " + type; //$NON-NLS-1$ //$NON-NLS-2$
		} catch (IAgentException e2) {
		}
		return "Service " + type + " " + e; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void serviceChanged(final RemoteServiceEvent e) {
		BrowserErrorHandler.debug(getDebugServiceChangedMsg(e));
		if (!isConnected())
			return;

		if (isConnecting()) {
			synchronized (FrameworkConnectorFactory.getLockObject(connector)) {
			}
		}

		try {
			RemoteService rService = e.getService();
			if (e.getType() == RemoteServiceEvent.UNREGISTERED) {
				removeService(rService.getServiceId());
			} else {
				RemoteBundle rBundle = rService.getBundle();
				if (rBundle != null) {
					Bundle bundle = findBundle(rBundle.getBundleId());
					if (bundle == null) {
						addBundle(rBundle);
					}
					if (e.getType() == RemoteServiceEvent.REGISTERED) {
						ServiceObject servObj = new ServiceObject(rService, rBundle);
						RemoteBundle usedInBundles[] = rService.getUsingBundles();
						for (int i = 0; i < usedInBundles.length; i++) {
							ServiceObject.addUsedInBundle(rService, usedInBundles[i], this);
						}
						addServiceNodes(servObj);
					}
				}
			}
			updateElement();
		} catch (IAgentException e1) {
			BrowserErrorHandler.processError(e1, connector, userDisconnect);
		} catch (IllegalStateException ex) {
			// ignore illegal states, they are usually due to working with stale
			// data
			BrowserErrorHandler.debug(ex);
		} catch (Throwable t) {
			BrowserErrorHandler.processError(t, connector, userDisconnect);
		}
	}

	private void updateContextMenuStates() {
		display = Display.getCurrent();
		if (display == null)
			display = Display.getDefault();
		display.asyncExec(new Runnable() {
			public void run() {
				FrameWorkView.updateContextMenuStates();
			}
		});
	}

	public void refreshAction() {
		Job job = new Job(Messages.refresh_framework_info) {
			protected IStatus run(final IProgressMonitor monitor) {
				synchronized (FrameworkConnectorFactory.getLockObject(connector)) {
					monitor.beginTask("Refreshing " + FrameworkImpl.this.getName(),
							FrameworkConnectorFactory.CONNECT_PROGRESS);
					refreshing = true;
					clearModel();
					monitor.worked(FrameworkConnectorFactory.CONNECT_PROGRESS_CONNECTING);
					buildModel(monitor);
					updateContextMenuStates();
					refreshing = false;
				}
				return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	public void setPMPConnectionListener(PMPConnectionListener connectionListener) {
		this.connectionListener = connectionListener;
	}

	public PMPConnectionListener getPMPConnectionListener() {
		return connectionListener;
	}

	// TODO
	public void refreshBundleAction(final Bundle sourceBundle) {
		Job job = new Job(Messages.refresh_bundles_info) {

			protected IStatus run(IProgressMonitor monitor) {
				try {
					long id = sourceBundle.getID();
					RemoteService regServ[] = sourceBundle.getRemoteBundle().getRegisteredServices();
					RemoteService usedServ[] = sourceBundle.getRemoteBundle().getServicesInUse();

					Bundle bundle = findBundleInDP(id);
					if (bundle != null) {
						updateBundleServices(bundle, regServ, usedServ);
					}

					bundle = findBundle(id);
					updateBundleServices(bundle, regServ, usedServ);

					// remove bundle in services view
					for (int i = servicesViewVector.size() - 1; i >= 0; i--) {
						Model service = (Model) servicesViewVector.elementAt(i);
						Model children[] = service.getChildren();
						for (int j = 0; j < children.length; j++) {
							Model bundles[] = children[j].getChildren();
							for (int k = 0; k < bundles.length; k++) {
								bundle = (Bundle) bundles[k];
								if (bundle.getID() == id) {
									children[j].removeElement(bundle);
									// remove bundles registered services
									if (j == 0) {
										servicesViewVector.removeElementAt(i);
										removeElement(service);
									}
								}
							}
						}
					}

					for (int i = 0; i < regServ.length; i++) {
						String objClass[] = regServ[i].getObjectClass();
						for (int j = 0; j < objClass.length; j++) {
							ObjectClass oc = new ObjectClass(objClass[j] + " [Service " + regServ[i].getServiceId()
									+ "]", new Long(regServ[i].getServiceId()), regServ[i]);
							BundlesCategory regCategory = new BundlesCategory(BundlesCategory.REGISTERED);
							BundlesCategory usedCategory = new BundlesCategory(BundlesCategory.IN_USE);
							oc.addElement(regCategory);
							oc.addElement(usedCategory);

							Bundle newBundle = new Bundle(sourceBundle.getName(), sourceBundle.getRemoteBundle(),
									sourceBundle.getState(), sourceBundle.getType(), sourceBundle.getCategory());
							regCategory.addElement(newBundle);
							servicesViewVector.addElement(oc);
							if (viewType == SERVICES_VIEW) {
								addElement(oc);
								oc.updateElement();
							}
						}
					}

					for (int i = 0; i < usedServ.length; i++) {
						long usedInId = usedServ[i].getServiceId();
						for (int j = 0; j < servicesViewVector.size(); j++) {
							ObjectClass oc = (ObjectClass) servicesViewVector.elementAt(j);
							if (oc.getService().getServiceId() == usedInId) {
								oc.getChildren()[1].addElement(new Bundle(sourceBundle.getName(), sourceBundle
										.getRemoteBundle(), sourceBundle.getState(), sourceBundle.getType(),
										sourceBundle.getCategory()));
								break;
							}
						}
					}

				} catch (IAgentException e) {
					BrowserErrorHandler.processError(e, true);
				} catch (IllegalStateException e) {
					BrowserErrorHandler.processError(e, true);
				}
				return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
			}

		};
		job.schedule();

	}

	private void updateBundleServices(Bundle bundle, RemoteService regServ[], RemoteService usedServ[])
			throws IAgentException {
		bundle.removeChildren();

		if (regServ.length > 0) {
			Model regServCategory = getServiceCategoryNode(bundle, ServicesCategory.REGISTERED_SERVICES, true);
			for (int i = 0; i < regServ.length; i++) {
				String objClass[] = regServ[i].getObjectClass();
				for (int j = 0; j < objClass.length; j++) {
					ObjectClass oc = new ObjectClass(objClass[j] + " [Service " + regServ[i].getServiceId() + "]",
							new Long(regServ[i].getServiceId()), regServ[i]);
					regServCategory.addElement(oc);
					if (isShownServicePropertiss()) {
						try {
							addServicePropertiesNodes(oc);
						} catch (IAgentException e) {
							e.printStackTrace();
						}
					}

				}
			}
		}
		if (usedServ.length > 0) {
			Model usedServCategory = getServiceCategoryNode(bundle, ServicesCategory.USED_SERVICES, true);
			for (int i = 0; i < usedServ.length; i++) {
				String objClass[] = usedServ[i].getObjectClass();
				for (int j = 0; j < objClass.length; j++) {
					ObjectClass oc = new ObjectClass(objClass[j] + " [Service " + usedServ[i].getServiceId() + "]",
							new Long(usedServ[i].getServiceId()), usedServ[i]);
					usedServCategory.addElement(oc);
					if (isShownServicePropertiss()) {
						try {
							addServicePropertiesNodes(oc);
						} catch (IAgentException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	public int getFrameWorkStartLevel() throws IAgentException {
		return connector.getVMManager().getFrameworkStartLevel();
	}

	public boolean isRefreshing() {
		return refreshing;
	}

	/**
	 * Called when connecting to framework
	 * 
	 * @param monitor
	 * @throws IAgentException
	 */
	private void addBundles(IProgressMonitor monitor) throws IAgentException {
		RemoteBundle rBundlesArray[] = null;
		DeviceConnector connector = getConnector();
		if (connector == null)
			return;
		rBundlesArray = connector.getDeploymentManager().listBundles();

		if (rBundlesArray != null) {
			monitor.subTask(Messages.retrieve_bundles_info);
			int work = (int) (FrameworkConnectorFactory.CONNECT_PROGRESS_BUNDLES / rBundlesArray.length);

			for (int i = 0; i < rBundlesArray.length; i++) {
				try {
					addBundle(rBundlesArray[i]);
				} catch (IAgentException e) {
					if (!userDisconnect && e.getErrorCode() != IAgentErrors.ERROR_BUNDLE_UNINSTALLED) {
						BrowserErrorHandler.processError(e, getConnector(), userDisconnect);
					}
				}
				if (monitor.isCanceled())
					return;
				monitor.worked(work);
			}
		}

		if (rBundlesArray != null) {
			monitor.subTask(Messages.retrieve_services_info);

			int work = (int) (FrameworkConnectorFactory.CONNECT_PROGRESS_SERVICES / rBundlesArray.length);

			for (int i = 0; i < rBundlesArray.length; i++) {
				Bundle bundle = findBundle(rBundlesArray[i].getBundleId());
				if (bundle != null
						&& (bundle.getState() != org.osgi.framework.Bundle.ACTIVE || bundle.getState() != org.osgi.framework.Bundle.STARTING)) {
					try {
						RemoteService rServices[] = rBundlesArray[i].getRegisteredServices();
						for (int j = 0; j < rServices.length; j++) {
							servicesVector.addElement(new ServiceObject(rServices[j], rBundlesArray[i]));
						}
						rServices = rBundlesArray[i].getServicesInUse();
						if (rServices != null) {
							for (int j = 0; j < rServices.length; j++) {
								ServiceObject.addUsedInBundle(rServices[j], rBundlesArray[i], this);
							}
						}
					} catch (IAgentException e) {
						if (e.getErrorCode() != IAgentErrors.ERROR_BUNDLE_UNINSTALLED) {
							throw e;
						}
					}
				}
				if (monitor.isCanceled())
					return;
				monitor.worked(work);
			}
		}
	}

	private void addDPs(IProgressMonitor monitor, int totalWork) throws IAgentException {
		Model deplPackagesNode = getDPNode();
		RemoteDP dps[] = null;
		DeviceConnector connector = getConnector();
		if (connector == null)
			return;
		dps = connector.getDeploymentManager().listDeploymentPackages();

		if (dps != null && dps.length > 0) {
			monitor.subTask(Messages.retrieve_dps_info);

			int work = totalWork / dps.length;
			for (int i = 0; i < dps.length; i++) {
				DeploymentPackage dpNode = new DeploymentPackage(dps[i], this);
				dpHash.put(dps[i].getName(), dpNode);
				deplPackagesNode.addElement(dpNode);
				monitor.worked(work);
				if (monitor.isCanceled()) {
					return;
				}
			}
		} else {
			monitor.worked(totalWork);
		}
	}

	public Model getServiceCategoryNode(Bundle bundle, int type, boolean add) {
		Model[] categories = bundle.getChildren();
		Model category = null;
		if (categories != null) {
			for (int i = 0; i < categories.length; i++) {
				if (((ServicesCategory) categories[i]).getType() == type) {
					category = categories[i];
					break;
				}
			}
		}
		if (category == null && add) {
			category = new ServicesCategory(type);
			bundle.addElement(category);
		}
		return category;
	}

	private void addBundle(RemoteBundle rBundle) throws IAgentException {
		try {
			if (bundleHash.containsKey(new Long(rBundle.getBundleId())))
				return;

			Dictionary headers = rBundle.getHeaders(null);
			Model bundleParentModel;
			String categoryName = (String) headers.get("Bundle-Category");
			if (FrameworkConnectorFactory.isBundlesCategoriesShown) {
				if (categoryName == null)
					categoryName = Messages.unknown_category_label;
				Category category = null;
				if (categoryHash.containsKey(categoryName)) {
					category = (Category) categoryHash.get(categoryName);
				} else {
					category = new Category(categoryName);
					categoryHash.put(categoryName, category);
					getBundlesNode().addElement(category);
				}
				bundleParentModel = category;
			} else {
				bundleParentModel = getBundlesNode();
			}

			String bundleName = getBundleName(rBundle, headers);
			Bundle bundle = new Bundle(bundleName, rBundle, rBundle.getState(), getRemoteBundleType(rBundle, headers),
					categoryName);
			bundleParentModel.addElement(bundle);
			bundleHash.put(new Long(bundle.getID()), bundle);

		} catch (IllegalArgumentException e) {
			// bundle was uninstalled
		}
	}

	private String getBundleName(RemoteBundle bundle, Dictionary headers) throws IAgentException {
		String bundleName = ""; //$NON-NLS-1$
		if (headers == null)
			headers = bundle.getHeaders(null);
		bundleName = (String) headers.get("Bundle-SymbolicName"); //$NON-NLS-1$
		if (bundleName == null || bundleName.equals("")) { //$NON-NLS-1$
			bundleName = (String) headers.get("Bundle-Name"); //$NON-NLS-1$
		}
		if (bundleName == null || bundleName.equals("")) { //$NON-NLS-1$
			bundleName = bundle.getLocation();
			if (bundleName.indexOf('/') != -1)
				bundleName = bundleName.substring(bundleName.lastIndexOf('/'));
			if (bundleName.indexOf('\\') != -1)
				bundleName = bundleName.substring(bundleName.lastIndexOf('\\'));
		}
		int delimIndex = bundleName.indexOf(';');
		if (delimIndex != -1)
			bundleName = bundleName.substring(0, delimIndex);
		return bundleName;
	}

	protected int getRemoteBundleType(RemoteBundle rBundle, Dictionary headers) throws IAgentException {
		String fragment = (String) headers.get("Fragment-Host"); //$NON-NLS-1$
		int type = 0;
		if (fragment != null && !fragment.equals("")) { //$NON-NLS-1$
			type = Bundle.BUNDLE_TYPE_FRAGMENT;
			RemoteBundle hosts[] = rBundle.getHosts();
			if (hosts != null && hosts.length == 1 && hosts[0].getBundleId() == 0) {
				type = Bundle.BUNDLE_TYPE_EXTENSION;
			}
		}
		return type;
	}

	private void addServices(IProgressMonitor monitor) throws IAgentException {
		for (int i = 0; i < servicesVector.size(); i++) {
			ServiceObject servObj = (ServiceObject) servicesVector.elementAt(i);
			addServiceNodes(servObj);
			if (monitor.isCanceled())
				return;
		}
	}

	private void addServiceNodes(ServiceObject servObj) throws IAgentException {
		Bundle bundle = findBundle(servObj.getRegisteredIn().getBundleId());
		addServiceNodes(servObj, bundle, true);
		bundle = findBundleInDP(bundle.getID());
		if (bundle != null) {
			addServiceNodes(servObj, bundle, false);
		}
	}

	private void addServiceNodes(ServiceObject servObj, Bundle bundle, boolean first) throws IAgentException {
		if (bundle.getState() == org.osgi.framework.Bundle.ACTIVE
				|| bundle.getState() == org.osgi.framework.Bundle.STARTING
				|| bundle.getRemoteBundle().getState() == org.osgi.framework.Bundle.ACTIVE
				|| bundle.getRemoteBundle().getState() == org.osgi.framework.Bundle.STARTING) {
			try {
				Model registeredCategory = getServiceCategoryNode(bundle, ServicesCategory.REGISTERED_SERVICES, true);
				createObjectClassNodes(registeredCategory, servObj.getObjectClass(), new Long(servObj
						.getRemoteService().getServiceId()), servObj.getRemoteService());

				for (int i = 0; i < servObj.getObjectClass().length; i++) {
					ObjectClass hashService = new ObjectClass(servObj.getObjectClass()[i] + " [Service "
							+ servObj.getRemoteService().getServiceId() + "]", new Long(servObj.getRemoteService()
							.getServiceId()), servObj.getRemoteService());
					BundlesCategory hashRegisteredCategory = new BundlesCategory(BundlesCategory.REGISTERED);
					BundlesCategory hashUsedCategory = new BundlesCategory(BundlesCategory.IN_USE);
					hashService.addElement(hashRegisteredCategory);
					hashService.addElement(hashUsedCategory);
					hashRegisteredCategory.addElement(new Bundle(bundle.getName(), bundle.getRemoteBundle(), bundle
							.getState(), bundle.getType(), bundle.getCategory()));

					RemoteBundle usedInBundles[] = servObj.getUsedIn(this);
					if (usedInBundles != null) {
						for (int k = 0; k < usedInBundles.length; k++) {
							Bundle usedInBundleNode = findBundle(servObj.getUsedIn(this)[k].getBundleId());
							if (usedInBundleNode == null) {
								throw new IllegalStateException(
										"Bundle " + servObj.getUsedIn(this)[k].getBundleId() + " is missing"); //$NON-NLS-1$ //$NON-NLS-2$
							}
							hashUsedCategory.addElement(new Bundle(usedInBundleNode.getName(), usedInBundleNode
									.getRemoteBundle(), usedInBundleNode.getState(), usedInBundleNode.getType(),
									usedInBundleNode.getCategory()));
						}
					}

					for (int j = servicesViewVector.size() - 1; j >= 0; j--) {
						Model model = (Model) servicesViewVector.elementAt(j);
						if (model.getName().equals(hashService.getName())) {
							servicesViewVector.removeElementAt(j);
						}
					}
					servicesViewVector.addElement(hashService);

					if (getViewType() == FrameworkImpl.SERVICES_VIEW) {
						addElement(hashService);
					}
				}

			} catch (IllegalArgumentException e) {
				// bundle was uninstalled
			}
		}

		RemoteBundle usedInBundles[] = servObj.getUsedIn(this);
		if (usedInBundles != null) {
			for (int j = 0; j < usedInBundles.length; j++) {
				Bundle usedInBundle = first ? findBundle(usedInBundles[j].getBundleId())
						: findBundleInDP(usedInBundles[j].getBundleId());
				if (usedInBundle == null) {
					continue;
				}
				Model usedCategory = getServiceCategoryNode(usedInBundle, ServicesCategory.USED_SERVICES, true);
				createObjectClassNodes(usedCategory, servObj.getObjectClass(), new Long(servObj.getRemoteService()
						.getServiceId()), servObj.getRemoteService());
			}
		}
	}

	private void createObjectClassNodes(Model parent, String objClasses[], Long nameID, RemoteService service)
			throws IAgentException {
		for (int i = 0; i < objClasses.length; i++) {
			ObjectClass objClass = new ObjectClass(objClasses[i] + " [Service " + service.getServiceId() + "]", nameID,
					service);
			parent.addElement(objClass);
			FrameworkImpl fw = (FrameworkImpl) objClass.findFramework();
			if (fw != null && fw.isShownServicePropertiss())
				addServicePropertiesNodes(objClass);
		}
	}

	protected void addServicePropertiesNodes(ObjectClass objClass) throws IAgentException {
		RemoteService rService = objClass.getService();
		Dictionary servProperties = rService.getProperties();
		Enumeration keys = servProperties.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			Object value = servProperties.get(key);
			if (value instanceof String[]) {
				String[] values = (String[]) value;
				if (values.length == 1) {
					ServiceProperty node = new ServiceProperty(key + ": " + values[0]);
					objClass.addElement(node);
				} else {
					for (int j = 0; j < values.length; j++) {
						StringBuffer buff = new StringBuffer();
						buff.append(key).append("[").append(String.valueOf(j + 1)).append("]");
						String key2 = buff.toString();
						ServiceProperty node = new ServiceProperty(key2 + ": " + values[j]);
						objClass.addElement(node);
					}
				}
			} else {
				ServiceProperty node = new ServiceProperty(key + ": " + value.toString());
				objClass.addElement(node);
			}
		}
	}

}
