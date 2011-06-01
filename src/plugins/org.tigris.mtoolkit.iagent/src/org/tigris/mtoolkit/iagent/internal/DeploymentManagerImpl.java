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
package org.tigris.mtoolkit.iagent.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.InflaterInputStream;

import org.tigris.mtoolkit.iagent.BundleSnapshot;
import org.tigris.mtoolkit.iagent.DeploymentManager;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.Error;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.iagent.RemoteDP;
import org.tigris.mtoolkit.iagent.event.RemoteBundleEvent;
import org.tigris.mtoolkit.iagent.event.RemoteBundleListener;
import org.tigris.mtoolkit.iagent.event.RemoteDPEvent;
import org.tigris.mtoolkit.iagent.event.RemoteDPListener;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.pmp.EventListener;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;
import org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin;
import org.tigris.mtoolkit.iagent.spi.ConnectionEvent;
import org.tigris.mtoolkit.iagent.spi.ConnectionListener;
import org.tigris.mtoolkit.iagent.spi.ConnectionManager;
import org.tigris.mtoolkit.iagent.spi.MethodSignature;
import org.tigris.mtoolkit.iagent.spi.PMPConnection;

public class DeploymentManagerImpl implements DeploymentManager, EventListener, ConnectionListener {

	private static final String DEPLOYMENT_EVENT = "d_event";
	private static final String SYNCH_BUNDLE_EVENT = "synch_bundle_event";
	private static final String SYSTEM_BUNDLE_EVENT = "system_bundle_event";
	private static final String EVENT_TYPE_KEY = "type";
	private static final String EVENT_DEPLOYMENT_PACKAGE_KEY = "deployment.package";
	private static final String EVENT_BUNDLE_ID_KEY = "bundle.id";

	private static MethodSignature INSTALL_BUNDLE_METHOD = new MethodSignature("installBundle", new String[] { MethodSignature.STRING_TYPE, MethodSignature.INPUT_STREAM_TYPE }, true);
	private static MethodSignature GET_SYSTEM_BUNDLES_NAMES = new MethodSignature("getSystemBundlesNames");
	private static MethodSignature LIST_BUNDLES_METHOD = new MethodSignature("listBundles", MethodSignature.NO_ARGS, true);
	private static MethodSignature GET_BUNDLES_METHOD = new MethodSignature("getBundles", new String[] { MethodSignature.STRING_TYPE, MethodSignature.STRING_TYPE }, true);
	private static MethodSignature LIST_DPS_METHOD = new MethodSignature("listDeploymentPackages", MethodSignature.NO_ARGS, true);
	private static MethodSignature GET_DP_VERSION_METHOD = new MethodSignature("getDeploymentPackageVersion", new String[] { MethodSignature.STRING_TYPE }, true);
	private static MethodSignature INSTALL_DP_METHOD = new MethodSignature("installDeploymentPackage", new String[] { MethodSignature.INPUT_STREAM_TYPE }, true);
	private static MethodSignature GET_BUNDLE_BY_LOCATION_METHOD = new MethodSignature("getBundleByLocation", new String[] { MethodSignature.STRING_TYPE }, true);
	private static MethodSignature GET_SYSTEM_BUNDLES_IDS_METHOD = new MethodSignature("getSystemBundlesIDs", MethodSignature.NO_ARGS, true);
	private static MethodSignature GET_BUNDLES_SNAPSHOT_METHOD = new MethodSignature("getBundlesSnapshot", new String[] { "int", Dictionary.class.getName() }, true);

	private DeviceConnectorImpl connector;

	private long[] systemBundlesIDs = null;
	private String[] systemBundlesNames = null;

	private List bundleListeners = new LinkedList();
	private List dpListeners = new LinkedList();

	private boolean addedConnectionListener;

	public DeploymentManagerImpl(DeviceConnectorImpl connector) {
		if (connector == null)
			throw new IllegalArgumentException();
		this.connector = connector;
	}

	public RemoteBundle[] getBundles(String symbolicName, String version) throws IAgentException {
		debug("[getBundles] >>> symbolicName: " + symbolicName + "; version: " + version);
		if (symbolicName == null)
			throw new IllegalArgumentException("Symbolic name parameter cannot be null");
		long[] bids = (long[]) GET_BUNDLES_METHOD.call(getBundleAdmin(), new Object[] { symbolicName, "[" + version + "," + version + "]" });
		if (bids == null) {
			final String msg = "PackageAdmin service is unavailable on the remote site";
			info("[getBundles] " + msg);
			throw new IAgentException(msg, IAgentErrors.GENERAL_ERROR);
		} else if (bids.length == 0) {
			debug("[getBundles] There are no bundles with name: " + symbolicName + " version: " + version);
			return null;
		} else {
			debug("[getBundles] Returned bundles list: " + DebugUtils.convertForDebug(bids));
			RemoteBundle[] bundles = new RemoteBundle[bids.length];
			for (int i = 0; i < bids.length; i++) {
				bundles[i] = new RemoteBundleImpl(this, new Long(bids[i]));
			}
			return bundles;
		}
	}

	public RemoteDP getDeploymentPackage(String name) throws IAgentException {
		debug("[getDeploymentPackage] >>> name: " + name);
		if (name == null)
			throw new IllegalArgumentException();
		String version = (String) GET_DP_VERSION_METHOD.call(getDeploymentAdmin(), new Object[] { name });
		if (version == null) {
			debug("[getDeploymentPackage] No deployment package with this name: " + name + " is found.");
			return null;
		} else {
			debug("[getDeploymentPackage] Deployment package for name: " + name + " found with version: " + version);
			return new RemoteDPImpl(this, name, version);
		}
	}

	public RemoteBundle installBundle(String location, InputStream is) throws IAgentException {
		debug("[installBundle] >>> location: " + location + "; inputStream: " + is);
		if (location == null || is == null)
			throw new IllegalArgumentException();
		Long bundleId = (Long) GET_BUNDLE_BY_LOCATION_METHOD.call(getBundleAdmin(), new Object[] { location });
		if (bundleId != null) {
			if (bundleId.longValue() != -1)
				return new RemoteBundleImpl(this, bundleId, location); // directly
			// return
			// reference
			// to
			// the
			// bundle,
			// instead
			// of
			// walking
			// to
			// the
			// device
			// for
			// this
		}
		Object result = INSTALL_BUNDLE_METHOD.call(getBundleAdmin(), new Object[] { location, is });
		if (result instanceof Long) {
			bundleId = (Long) result;
			debug("[installBundle] installed bundle id: " + bundleId);
			return new RemoteBundleImpl(this, bundleId, location);
		} else if (result instanceof Error) {
			Error error = (Error) result;
			info("[installBundle] Failed to install bundle: " + error);
			throw new IAgentException(error);
		} else {
			String msg = "Unknown response received from the remote method invocation: " + result;
			info("[installBundle] " + msg);
			throw new IAgentException(msg, IAgentErrors.GENERAL_ERROR);
		}
	}

	public RemoteObject getBundleAdmin() throws IAgentException {
		PMPConnection connection = (PMPConnection) connector.getConnection(ConnectionManager.PMP_CONNECTION);
		RemoteObject bundleAdmin = connection.getRemoteBundleAdmin();
		return bundleAdmin;
	}

	public RemoteObject getDeploymentAdmin() throws IAgentException {
		PMPConnection connection = (PMPConnection) connector.getConnection(ConnectionManager.PMP_CONNECTION);
		RemoteObject bundleAdmin = connection.getRemoteDeploymentAdmin();
		return bundleAdmin;
	}

	public RemoteDP installDeploymentPackage(InputStream is) throws IAgentException {
		debug("[installDeploymentPackage] >>> is: " + is);
		if (is == null) {
			throw new IllegalArgumentException("Non-null InputStream must be passed");
		}
		Object result = INSTALL_DP_METHOD.call(getDeploymentAdmin(), new Object[] { is });
		if (result instanceof Error) {
			Error error = (Error) result;
			info("[installDeploymentPackage] Installation failed: " + error);
			throw new IAgentException(error);
		} else {
			String[] deploymentPackage = (String[]) result;
			return new RemoteDPImpl(this, deploymentPackage[0], deploymentPackage[1]);
		}
	}

	public RemoteBundle[] listBundles() throws IAgentException {
		debug("[listBundles] >>>");
		long[] bids = (long[]) LIST_BUNDLES_METHOD.call(getBundleAdmin());
		if (bids == null) {
			info("[listBundles] listBundles() must not return null array. There is a problem with the transport.");
			throw new IAgentException(
					"listBundles() must not return null array. There is a problem with the transport.",
					IAgentErrors.GENERAL_ERROR);
		}
		RemoteBundle[] bundles = new RemoteBundle[bids.length];
		debug("[listBundles] Returned bundles list: " + DebugUtils.convertForDebug(bids));
		for (int i = 0; i < bids.length; i++) {
			bundles[i] = new RemoteBundleImpl(this, new Long(bids[i]));
		}
		return bundles;
	}

	public RemoteDP[] listDeploymentPackages() throws IAgentException {
		debug("[listDeploymentPackages] >>>");
		Dictionary dps = (Dictionary) LIST_DPS_METHOD.call(getDeploymentAdmin());
		RemoteDP[] result = new RemoteDP[dps.size()];
		int i = 0;
		for (Enumeration e = dps.keys(); e.hasMoreElements(); i++) {
			String symbolicName = (String) e.nextElement();
			String version = (String) dps.get(symbolicName);
			result[i] = new RemoteDPImpl(this, symbolicName, version);
		}
		debug("[listDeploymentPackages] Returned deployment packages: " + DebugUtils.convertForDebug(result));
		return result;
	}

	public RemoteBundle getBundle(long id) throws IAgentException {
		debug("[getBundle] >>> id: " + id);
		RemoteBundle bundle = new RemoteBundleImpl(this, new Long(id));
		int state = bundle.getState();
		if (state != RemoteBundle.UNINSTALLED) {
			debug("[getBundle] Bundle found with state: " + state);
			return bundle;
		} else {
			debug("[getBundle] No such bundle");
			return null; // no bundle found
		}
	}

	public void addRemoteBundleListener(RemoteBundleListener listener) throws IAgentException {
		debug("[addRemoteBundleListener] >>> listener: " + listener);
		synchronized (this) {
			if (!addedConnectionListener) {
				connector.getConnectionManager().addConnectionListener(this);
				addedConnectionListener = true;
				debug("[addRemoteBundleListener] Connection listener added");
			}
		}
		synchronized (bundleListeners) {
			if (!bundleListeners.contains(listener)) {
				PMPConnection connection = (PMPConnection) connector.getConnection(ConnectionManager.PMP_CONNECTION,
						false);
				if (connection != null) {
					debug("[addRemoteBundleListener] PMP connection is available, add event listener");
					connection.addEventListener(this, new String[] { SYNCH_BUNDLE_EVENT });
				}
				bundleListeners.add(listener);
			}
		}
	}

	public void addRemoteDPListener(RemoteDPListener listener) throws IAgentException {
		debug("[addRemoteDPListener] >>> listener: " + listener);
		synchronized (this) {
			if (!addedConnectionListener) {
				connector.getConnectionManager().addConnectionListener(this);
				addedConnectionListener = true;
				debug("[addRemoteDPListener] Connection listener added");
			}
		}
		synchronized (dpListeners) {
			if (!dpListeners.contains(listener)) {
				PMPConnection connection = (PMPConnection) connector.getConnection(ConnectionManager.PMP_CONNECTION,
						false);
				if (connection != null) {
					debug("[addRemoteDPListener] PMP connection is available, add event listener");
					connection.addEventListener(this, new String[] { DEPLOYMENT_EVENT });
				}
				dpListeners.add(listener);
			} else {
				debug("[addRemoteDPListener] Listener already present");
			}
		}
	}

	public void removeRemoteBundleListener(RemoteBundleListener listener) throws IAgentException {
		debug("[removeRemoteBundleListener] >>> listener: " + listener);
		synchronized (bundleListeners) {
			if (bundleListeners.contains(listener)) {
				bundleListeners.remove(listener);
				if (bundleListeners.size() == 0) {
					debug("[removeRemoteBundleListener] No more listeners in the list, try to remove PMP event listener");
					PMPConnection connection = (PMPConnection) connector.getConnection(
							ConnectionManager.PMP_CONNECTION, false);
					if (connection != null) {
						debug("[removeRemoteBundleListener] PMP connection is available, remove event listener");
						connection.removeEventListener(this, new String[] { SYNCH_BUNDLE_EVENT });
					}
				}
			} else {
				debug("[removeRemoteBundleListener] Listener not found in the list");
			}
		}
	}

	public void removeRemoteDPListener(RemoteDPListener listener) throws IAgentException {
		debug("[removeRemoteDPListener] >>> listener: " + listener);
		synchronized (dpListeners) {
			if (dpListeners.contains(listener)) {
				dpListeners.remove(listener);
				if (dpListeners.size() == 0) {
					debug("[removeRemoteDPListener] No more listeners in the list, try to remove PMP event listener");
					PMPConnection connection = (PMPConnection) connector.getConnection(
							ConnectionManager.PMP_CONNECTION, false);
					if (connection != null) {
						debug("[removeRemoteDPListener] PMP connection is available, remove event listener");
						connection.removeEventListener(this, new String[] { DEPLOYMENT_EVENT });
					}
				}
			} else {
				debug("[removeRemoteDPListener] Listener not found in the list");
			}
		}
	}

	private void fireBundleEvent(long bundleId, int type) {
		debug("[fireBundleEvent] >>> bundleId: " + bundleId + "; type: " + type);
		RemoteBundleListener[] listeners;
		synchronized (bundleListeners) {
			if (bundleListeners.size() != 0) {
				listeners = (RemoteBundleListener[]) bundleListeners.toArray(new RemoteBundleListener[bundleListeners
						.size()]);
			} else {
				return;
			}
		}
		RemoteBundle bundle = new RemoteBundleImpl(this, new Long(bundleId));
		RemoteBundleEvent event = new RemoteBundleEvent(bundle, type);
		debug("[fireBundleEvent] " + listeners.length + " listeners found.");
		for (int i = 0; i < listeners.length; i++) {
			RemoteBundleListener listener = listeners[i];
			try {
				debug("[fireBundleEvent] deliver event: " + event + " to listener: " + listener);
				listener.bundleChanged(event);
			} catch (Throwable e) {
				error("[fireBundleEvent] Failed to deliver event to " + listener, e);
			}
		}
	}

	private void fireDeploymentEvent(String symbolicName, String version, int type) {
		debug("[fireDeploymentEvent] >>> symbolicName: " + symbolicName + "; version: " + version + "; type: " + type);
		RemoteDPListener[] listeners;
		synchronized (dpListeners) {
			if (dpListeners.size() != 0) {
				listeners = (RemoteDPListener[]) dpListeners.toArray(new RemoteDPListener[dpListeners.size()]);
			} else {
				return;
			}
		}
		debug("[fireDeploymentEvent] " + listeners.length + " listeners found.");
		RemoteDP dp = new RemoteDPImpl(this, symbolicName, version);
		RemoteDPEvent event = new RemoteDPEvent(dp, type);
		for (int i = 0; i < listeners.length; i++) {
			RemoteDPListener listener = listeners[i];
			try {
				debug("[fireDeploymentEvent] deliver event: " + event + " to listener: " + listener);
				listener.deploymentPackageChanged(event);
			} catch (Throwable e) {
				IAgentLog.error("[DeploymentManagerImpl][fireDeploymentEvent] Failed to deliver event to " + listener,
						e);
			}
		}
	}

	public void event(Object event, String eventType) {
		try {
			debug("[event] >>> event: " + event + "; type: " + eventType);
			if (DEPLOYMENT_EVENT.equals(eventType)) {
				Dictionary eventProps = (Dictionary) event;
				String[] dp = (String[]) eventProps.get(EVENT_DEPLOYMENT_PACKAGE_KEY);
				int type = ((Integer) eventProps.get(EVENT_TYPE_KEY)).intValue();
				fireDeploymentEvent(dp[0], dp[1], type);
			} else if (SYNCH_BUNDLE_EVENT.equals(eventType)) {
				Dictionary eventProps = (Dictionary) event;
				int type = ((Integer) eventProps.get(EVENT_TYPE_KEY)).intValue();
				Long bid = (Long) eventProps.get(EVENT_BUNDLE_ID_KEY);
				fireBundleEvent(bid.longValue(), type);
			} else if (SYSTEM_BUNDLE_EVENT.equals(eventType)) {
				clearSystemBundlesList();
			}
		} catch (Throwable e) {
			error("[event] Failed to process PMP event: " + event + "; type: " + eventType, e);
		}
	}

	DeviceConnector getDeviceConnector() {
		return connector;
	}

	public void connectionChanged(ConnectionEvent event) {
		debug("[connectionChanged] >>> event: " + event);
		if (event.getType() == ConnectionEvent.CONNECTED
				&& event.getConnection().getType() == ConnectionManager.PMP_CONNECTION) {
			debug("[connectionChanged] New PMP connection created, restore event listeners");
			systemBundlesIDs = null; // reset system bundle ids - we don't know
			// whether it was VM restart or PMP connection closed
			systemBundlesNames = null;
			synchronized (dpListeners) {
				synchronized (bundleListeners) {
						PMPConnection connection = (PMPConnection) event.getConnection();
						if (dpListeners.size() > 0) {
							debug("[connectionChanged] Restoring deployment package listeners...");
							try {
								connection.addEventListener(this, new String[] { DEPLOYMENT_EVENT });
							} catch (IAgentException e) {
								error("[connectionChanged] Failed to add event listener to PMP connection", e);
							}
						}
						if (bundleListeners.size() > 0) {
							debug("[connectionChanged] Restoring bundle listeners...");
							try {
								connection.addEventListener(this, new String[] { SYNCH_BUNDLE_EVENT });
							} catch (IAgentException e) {
								error("[connectionChanged] Failed to add event listener to PMP connection", e);
							}
						}
				}
			}
		}
	}

	private long[] getSystemBundlesIDs() throws IAgentException {
		if (systemBundlesIDs != null) {
			return systemBundlesIDs;
		}
		long[] result = (long[]) GET_SYSTEM_BUNDLES_IDS_METHOD.call(getBundleAdmin());
		if (result == null) {
			info("[getSystemBundlesIDs] getSystemBundlesIDs() must not return null array. There is a problem with the transport.");
			throw new IAgentException(
					"getSystemBundlesIDs() must not return null array. There is a problem with the transport.",
					IAgentErrors.ERROR_INTERNAL_ERROR);
		}
		PMPConnection connection = (PMPConnection) connector.getConnection(ConnectionManager.PMP_CONNECTION, false);
		if (connection != null) {
			debug("[getSystemBundlesIDs] PMP connection is available, add event listener");
			connection.addEventListener(this, new String[] { SYSTEM_BUNDLE_EVENT });
		}
		systemBundlesIDs = result;
		return result;
	}

	public void removeListeners() throws IAgentException {
		debug("[removeListeners] >>> Removing all listeners...");
		synchronized (dpListeners) {
			dpListeners.clear();
			PMPConnection connection = (PMPConnection) connector.getConnection(ConnectionManager.PMP_CONNECTION, false);
			if (connection != null) {
				debug("[removeListeners] PMP connection is available, remove event listener for DP events...");
				connection.removeEventListener(this, new String[] { DEPLOYMENT_EVENT });
			}
			debug("[removeListeners] deployment package listeners removed");
		}
		synchronized (bundleListeners) {
			bundleListeners.clear();
			PMPConnection connection = (PMPConnection) connector.getConnection(ConnectionManager.PMP_CONNECTION, false);
			if (connection != null) {
				debug("[removeListeners] PMP connection is available, remove event listener for synchronous bundle events...");
				connection.removeEventListener(this, new String[] { SYNCH_BUNDLE_EVENT });
			}
			debug("[removeListeners] bundle listeners removed");
		}
		PMPConnection connection = (PMPConnection) connector.getConnection(ConnectionManager.PMP_CONNECTION, false);
		if (connection != null) {
			debug("[removeListeners] PMP connection is available. remove evnet listener for system bundle events...");
			connection.removeEventListener(this, new String[] { SYSTEM_BUNDLE_EVENT });
		}
	}

	public boolean scanSystemBundlesList(RemoteBundle bundle) throws IAgentException {
		long systemIDs[] = getSystemBundlesIDs();
		long tempBundleID = bundle.getBundleId();
		for (int i = 0; i < systemIDs.length; i++) {
			if (tempBundleID == systemIDs[i]) {
				return true;
			}
		}
		return false;
	}

	public void clearSystemBundlesList() {
		systemBundlesIDs = null;
		systemBundlesNames = null;
	}

	private final void debug(String message) {
		DebugUtils.debug(this, message);
	}

	private final void info(String message) {
		DebugUtils.info(this, message);
	}

	private final void error(String message, Throwable e) {
		DebugUtils.error(this, message, e);
	}

	public String[] getSystemBundlesNames() throws IAgentException {
		if (systemBundlesNames != null) {
			return systemBundlesNames;
		}
		String[] result = null;
		if (GET_SYSTEM_BUNDLES_NAMES.isDefined(getBundleAdmin())) {
			result = (String[]) GET_SYSTEM_BUNDLES_NAMES.call(getBundleAdmin());
			if (result == null) {
				info("[getSystemBundlesNames] getSystemBundlesNames() must not return null array. There is a problem with the transport.");
				throw new IAgentException(
						"getSystemBundlesNames() must not return null array. There is a problem with the transport.",
						IAgentErrors.ERROR_INTERNAL_ERROR);
			}
		} else {
			// fallback to old method of retrieving the names
			result = getSystemBundlesNamesFallback();
		}
		PMPConnection connection = (PMPConnection) connector.getConnection(ConnectionManager.PMP_CONNECTION, false);
		if (connection != null) {
			debug("[getSystemBundlesIDs] PMP connection is available, add event listener");
			connection.addEventListener(this, new String[] { SYSTEM_BUNDLE_EVENT });
		}
		systemBundlesNames = result;
		return result;
	}

	private String[] getSystemBundlesNamesFallback() throws IAgentException {
		long[] idArray = getSystemBundlesIDs();

		List names = new ArrayList(idArray.length);
		for (int i = 0; i < idArray.length; i++) {
			RemoteBundle bundle = getBundle(idArray[i]);
			if (bundle != null)
				names.add(bundle.getSymbolicName());
		}
		return (String[]) names.toArray(new String[names.size()]);
	}

	public BundleSnapshot[] getBundlesSnapshot(Dictionary properties) throws IAgentException {
		debug("[getBundlesSnapshot] >>>");
		int options = RemoteBundleAdmin.INCLUDE_BUNDLE_HEADERS | RemoteBundleAdmin.INCLUDE_BUNDLE_STATES
				| RemoteBundleAdmin.INCLUDE_REGISTERED_SERVICES | RemoteBundleAdmin.INCLUDE_USED_SERVICES;
		Object snapshotData = GET_BUNDLES_SNAPSHOT_METHOD.call(getBundleAdmin(), new Object[] {
				new Integer(options), properties });
		if (snapshotData == null) {
			info("[getBundlesSnapshot] getBundlesSnapshot() must not return null array. There is a problem with the transport.");
			throw new IAgentException(
					"getBundlesSnapshot() must not return null array. There is a problem with the transport.",
					IAgentErrors.GENERAL_ERROR);
		}
		Dictionary[] snapshots;
		if (snapshotData instanceof Dictionary[]) {
			snapshots = (Dictionary[]) snapshotData;
		} else {
			try {
				ByteArrayInputStream bis = new ByteArrayInputStream((byte[]) snapshotData);
				ObjectInputStream ois = new ObjectInputStream(new InflaterInputStream(bis));
				snapshots = (Dictionary[]) ois.readObject();
				ois.close();
			} catch (Exception e) {
				throw new IAgentException(
						"getBundlesSnapshot() Cannot decode the result. There is a problem with the transport.",
						IAgentErrors.GENERAL_ERROR);
			}
		}

		BundleSnapshot[] result = new BundleSnapshot[snapshots.length];
		for (int i = 0; i < snapshots.length; i++) {
			result[i] = new BundleSnapshotImpl(this, snapshots[i]);
		}
		return result;
	}

}