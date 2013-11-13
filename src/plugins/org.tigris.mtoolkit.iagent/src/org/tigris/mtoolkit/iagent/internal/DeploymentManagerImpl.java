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
import org.tigris.mtoolkit.iagent.pmp.EventListener;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;
import org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin;
import org.tigris.mtoolkit.iagent.spi.ConnectionEvent;
import org.tigris.mtoolkit.iagent.spi.ConnectionListener;
import org.tigris.mtoolkit.iagent.spi.ConnectionManager;
import org.tigris.mtoolkit.iagent.spi.MethodSignature;
import org.tigris.mtoolkit.iagent.spi.PMPConnection;
import org.tigris.mtoolkit.iagent.util.DebugUtils;

public final class DeploymentManagerImpl implements DeploymentManager, EventListener, ConnectionListener {
  private static final String    DEPLOYMENT_EVENT              = "d_event";
  private static final String    SYNCH_BUNDLE_EVENT            = "synch_bundle_event";
  private static final String    EVENT_TYPE_KEY                = "type";
  private static final String    EVENT_DEPLOYMENT_PACKAGE_KEY  = "deployment.package";
  private static final String    EVENT_BUNDLE_ID_KEY           = "bundle.id";

  private static MethodSignature INSTALL_BUNDLE_METHOD         = new MethodSignature("installBundle", new String[] {
      MethodSignature.STRING_TYPE, MethodSignature.INPUT_STREAM_TYPE
                                                               }, true);
  private static MethodSignature LIST_BUNDLES_METHOD           = new MethodSignature("listBundles",
                                                                   MethodSignature.NO_ARGS, true);
  private static MethodSignature GET_BUNDLES_METHOD            = new MethodSignature("getBundles", new String[] {
      MethodSignature.STRING_TYPE, MethodSignature.STRING_TYPE
                                                               }, true);
  private static MethodSignature LIST_DPS_METHOD               = new MethodSignature("listDeploymentPackages",
                                                                   MethodSignature.NO_ARGS, true);
  private static MethodSignature GET_DP_VERSION_METHOD         = new MethodSignature("getDeploymentPackageVersion",
                                                                   new String[] {
                                                                     MethodSignature.STRING_TYPE
                                                                   }, true);
  private static MethodSignature INSTALL_DP_METHOD             = new MethodSignature("installDeploymentPackage",
                                                                   new String[] {
                                                                     MethodSignature.INPUT_STREAM_TYPE
                                                                   }, true);
  private static MethodSignature GET_BUNDLE_BY_LOCATION_METHOD = new MethodSignature("getBundleByLocation",
                                                                   new String[] {
                                                                     MethodSignature.STRING_TYPE
                                                                   }, true);
  private static MethodSignature GET_BUNDLES_SNAPSHOT_METHOD   = new MethodSignature("getBundlesSnapshot",
                                                                   new String[] {
      "int", Dictionary.class.getName()
                                                                   }, true);

  private static MethodSignature REFRESH_PACKAGES              = new MethodSignature("refreshPackages",
                                                                   MethodSignature.NO_ARGS, true);

  private DeviceConnectorImpl    connector;

  private List                   bundleListeners               = new LinkedList();
  private List                   dpListeners                   = new LinkedList();

  private boolean                addedConnectionListener;

  public DeploymentManagerImpl(DeviceConnectorImpl connector) {
    if (connector == null) {
      throw new IllegalArgumentException();
    }
    this.connector = connector;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeploymentManager#getBundles(java.lang.String, java.lang.String)
   */
  public RemoteBundle[] getBundles(String symbolicName, String version) throws IAgentException {
    DebugUtils.debug(this, "[getBundles] >>> symbolicName: " + symbolicName + "; version: " + version);
    if (symbolicName == null) {
      throw new IllegalArgumentException("Symbolic name parameter cannot be null");
    }
    if (version != null) {
      version = "[" + version + "," + version + "]";
    }
    long[] bids = (long[]) GET_BUNDLES_METHOD.call(getBundleAdmin(), new Object[] {
        symbolicName, version
    });
    if (bids == null) {
      final String msg = "PackageAdmin service is unavailable on the remote site";
      DebugUtils.info(this, "[getBundles] " + msg);
      throw new IAgentException(msg, IAgentErrors.GENERAL_ERROR);
    } else if (bids.length == 0) {
      DebugUtils.debug(this, "[getBundles] There are no bundles with name: " + symbolicName + " version: " + version);
      return null;
    } else {
      DebugUtils.debug(this, "[getBundles] Returned bundles list: " + DebugUtils.convertForDebug(bids));
      RemoteBundle[] bundles = new RemoteBundle[bids.length];
      for (int i = 0; i < bids.length; i++) {
        bundles[i] = new RemoteBundleImpl(this, new Long(bids[i]));
      }
      return bundles;
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeploymentManager#getDeploymentPackage(java.lang.String)
   */
  public RemoteDP getDeploymentPackage(String name) throws IAgentException {
    DebugUtils.debug(this, "[getDeploymentPackage] >>> name: " + name);
    if (name == null) {
      throw new IllegalArgumentException();
    }
    String version = (String) GET_DP_VERSION_METHOD.call(getDeploymentAdmin(), new Object[] {
      name
    });
    if (version == null) {
      DebugUtils.debug(this, "[getDeploymentPackage] No deployment package with this name: " + name + " is found.");
      return null;
    } else {
      DebugUtils.debug(this, "[getDeploymentPackage] Deployment package for name: " + name + " found with version: "
          + version);
      return new RemoteDPImpl(this, name, version);
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeploymentManager#installBundle(java.lang.String, java.io.InputStream)
   */
  public RemoteBundle installBundle(String location, InputStream is) throws IAgentException {
    DebugUtils.debug(this, "[installBundle] >>> location: " + location + "; inputStream: " + is);
    if (location == null || is == null) {
      throw new IllegalArgumentException();
    }
    Long bundleId = (Long) GET_BUNDLE_BY_LOCATION_METHOD.call(getBundleAdmin(), new Object[] {
      location
    });
    if (bundleId != null) {
      if (bundleId.longValue() != -1) {
        return new RemoteBundleImpl(this, bundleId, location);
      }
    }
    Object result = INSTALL_BUNDLE_METHOD.call(getBundleAdmin(), new Object[] {
        location, is
    });
    if (result instanceof Long) {
      bundleId = (Long) result;
      DebugUtils.debug(this, "[installBundle] installed bundle id: " + bundleId);
      return new RemoteBundleImpl(this, bundleId, location);
    } else if (result instanceof Error) {
      Error error = (Error) result;
      DebugUtils.info(this, "[installBundle] Failed to install bundle: " + error);
      throw new IAgentException(error);
    } else {
      String msg = "Unknown response received from the remote method invocation: " + result;
      DebugUtils.info(this, "[installBundle] " + msg);
      throw new IAgentException(msg, IAgentErrors.GENERAL_ERROR);
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeploymentManager#installDeploymentPackage(java.io.InputStream)
   */
  public RemoteDP installDeploymentPackage(InputStream is) throws IAgentException {
    DebugUtils.debug(this, "[installDeploymentPackage] >>> is: " + is);
    if (is == null) {
      throw new IllegalArgumentException("Non-null InputStream must be passed");
    }
    Object result = INSTALL_DP_METHOD.call(getDeploymentAdmin(), new Object[] {
      is
    });
    if (result instanceof Error) {
      Error error = (Error) result;
      DebugUtils.info(this, "[installDeploymentPackage] Installation failed: " + error);
      throw new IAgentException(error);
    } else {
      String[] deploymentPackage = (String[]) result;
      return new RemoteDPImpl(this, deploymentPackage[0], deploymentPackage[1]);
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeploymentManager#listBundles()
   */
  public RemoteBundle[] listBundles() throws IAgentException {
    DebugUtils.debug(this, "[listBundles] >>>");
    long[] bids = (long[]) LIST_BUNDLES_METHOD.call(getBundleAdmin());
    if (bids == null) {
      DebugUtils.info(this,
          "[listBundles] listBundles() must not return null array. There is a problem with the transport.");
      throw new IAgentException("listBundles() must not return null array. There is a problem with the transport.",
          IAgentErrors.GENERAL_ERROR);
    }
    RemoteBundle[] bundles = new RemoteBundle[bids.length];
    DebugUtils.debug(this, "[listBundles] Returned bundles list: " + DebugUtils.convertForDebug(bids));
    for (int i = 0; i < bids.length; i++) {
      bundles[i] = new RemoteBundleImpl(this, new Long(bids[i]));
    }
    return bundles;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeploymentManager#listDeploymentPackages()
   */
  public RemoteDP[] listDeploymentPackages() throws IAgentException {
    DebugUtils.debug(this, "[listDeploymentPackages] >>>");
    Dictionary dps = (Dictionary) LIST_DPS_METHOD.call(getDeploymentAdmin());
    RemoteDP[] result = new RemoteDP[dps.size()];
    int i = 0;
    for (Enumeration e = dps.keys(); e.hasMoreElements(); i++) {
      String symbolicName = (String) e.nextElement();
      String version = (String) dps.get(symbolicName);
      result[i] = new RemoteDPImpl(this, symbolicName, version);
    }
    DebugUtils.debug(this,
        "[listDeploymentPackages] Returned deployment packages: " + DebugUtils.convertForDebug(result));
    return result;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeploymentManager#addRemoteBundleListener(org.tigris.mtoolkit.iagent.event.RemoteBundleListener)
   */
  public void addRemoteBundleListener(RemoteBundleListener listener) throws IAgentException {
    DebugUtils.debug(this, "[addRemoteBundleListener] >>> listener: " + listener);
    synchronized (this) {
      if (!addedConnectionListener) {
        connector.getConnectionManager().addConnectionListener(this);
        addedConnectionListener = true;
        DebugUtils.debug(this, "[addRemoteBundleListener] Connection listener added");
      }
    }
    synchronized (bundleListeners) {
      if (!bundleListeners.contains(listener)) {
        PMPConnection connection = (PMPConnection) connector.getConnection(ConnectionManager.PMP_CONNECTION, false);
        if (connection != null) {
          DebugUtils.debug(this, "[addRemoteBundleListener] PMP connection is available, add event listener");
          connection.addEventListener(this, new String[] {
            SYNCH_BUNDLE_EVENT
          });
        }
        bundleListeners.add(listener);
      }
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeploymentManager#addRemoteDPListener(org.tigris.mtoolkit.iagent.event.RemoteDPListener)
   */
  public void addRemoteDPListener(RemoteDPListener listener) throws IAgentException {
    DebugUtils.debug(this, "[addRemoteDPListener] >>> listener: " + listener);
    synchronized (this) {
      if (!addedConnectionListener) {
        connector.getConnectionManager().addConnectionListener(this);
        addedConnectionListener = true;
        DebugUtils.debug(this, "[addRemoteDPListener] Connection listener added");
      }
    }
    synchronized (dpListeners) {
      if (!dpListeners.contains(listener)) {
        PMPConnection connection = (PMPConnection) connector.getConnection(ConnectionManager.PMP_CONNECTION, false);
        if (connection != null) {
          DebugUtils.debug(this, "[addRemoteDPListener] PMP connection is available, add event listener");
          connection.addEventListener(this, new String[] {
            DEPLOYMENT_EVENT
          });
        }
        dpListeners.add(listener);
      } else {
        DebugUtils.debug(this, "[addRemoteDPListener] Listener already present");
      }
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeploymentManager#removeRemoteBundleListener(org.tigris.mtoolkit.iagent.event.RemoteBundleListener)
   */
  public void removeRemoteBundleListener(RemoteBundleListener listener) throws IAgentException {
    DebugUtils.debug(this, "[removeRemoteBundleListener] >>> listener: " + listener);
    synchronized (bundleListeners) {
      if (bundleListeners.contains(listener)) {
        bundleListeners.remove(listener);
        if (bundleListeners.size() == 0) {
          DebugUtils.debug(this,
              "[removeRemoteBundleListener] No more listeners in the list, try to remove PMP event listener");
          PMPConnection connection = (PMPConnection) connector.getConnection(ConnectionManager.PMP_CONNECTION, false);
          if (connection != null) {
            DebugUtils.debug(this, "[removeRemoteBundleListener] PMP connection is available, remove event listener");
            connection.removeEventListener(this, new String[] {
              SYNCH_BUNDLE_EVENT
            });
          }
        }
      } else {
        DebugUtils.debug(this, "[removeRemoteBundleListener] Listener not found in the list");
      }
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeploymentManager#removeRemoteDPListener(org.tigris.mtoolkit.iagent.event.RemoteDPListener)
   */
  public void removeRemoteDPListener(RemoteDPListener listener) throws IAgentException {
    DebugUtils.debug(this, "[removeRemoteDPListener] >>> listener: " + listener);
    synchronized (dpListeners) {
      if (dpListeners.contains(listener)) {
        dpListeners.remove(listener);
        if (dpListeners.size() == 0) {
          DebugUtils.debug(this,
              "[removeRemoteDPListener] No more listeners in the list, try to remove PMP event listener");
          PMPConnection connection = (PMPConnection) connector.getConnection(ConnectionManager.PMP_CONNECTION, false);
          if (connection != null) {
            DebugUtils.debug(this, "[removeRemoteDPListener] PMP connection is available, remove event listener");
            connection.removeEventListener(this, new String[] {
              DEPLOYMENT_EVENT
            });
          }
        }
      } else {
        DebugUtils.debug(this, "[removeRemoteDPListener] Listener not found in the list");
      }
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.pmp.EventListener#event(java.lang.Object, java.lang.String)
   */
  public void event(Object event, String eventType) {
    try {
      DebugUtils.debug(this, "[event] >>> event: " + event + "; type: " + eventType);
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
      }
    } catch (Throwable e) {
      DebugUtils.error(this, "[event] Failed to process PMP event: " + event + "; type: " + eventType, e);
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.ConnectionListener#connectionChanged(org.tigris.mtoolkit.iagent.spi.ConnectionEvent)
   */
  public void connectionChanged(ConnectionEvent event) {
    DebugUtils.debug(this, "[connectionChanged] >>> event: " + event);
    if (event.getType() == ConnectionEvent.CONNECTED
        && event.getConnection().getType() == ConnectionManager.PMP_CONNECTION) {
      DebugUtils.debug(this, "[connectionChanged] New PMP connection created, restore event listeners");
      synchronized (dpListeners) {
        synchronized (bundleListeners) {
          PMPConnection connection = (PMPConnection) event.getConnection();
          if (dpListeners.size() > 0) {
            DebugUtils.debug(this, "[connectionChanged] Restoring deployment package listeners...");
            try {
              connection.addEventListener(this, new String[] {
                DEPLOYMENT_EVENT
              });
            } catch (IAgentException e) {
              DebugUtils.error(this, "[connectionChanged] Failed to add event listener to PMP connection", e);
            }
          }
          if (bundleListeners.size() > 0) {
            DebugUtils.debug(this, "[connectionChanged] Restoring bundle listeners...");
            try {
              connection.addEventListener(this, new String[] {
                SYNCH_BUNDLE_EVENT
              });
            } catch (IAgentException e) {
              DebugUtils.error(this, "[connectionChanged] Failed to add event listener to PMP connection", e);
            }
          }
        }
      }
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeploymentManager#getBundlesSnapshot(java.util.Dictionary)
   */
  public BundleSnapshot[] getBundlesSnapshot(Dictionary properties) throws IAgentException {
    DebugUtils.debug(this, "[getBundlesSnapshot] >>>");
    int options = RemoteBundleAdmin.INCLUDE_BUNDLE_HEADERS | RemoteBundleAdmin.INCLUDE_BUNDLE_STATES
        | RemoteBundleAdmin.INCLUDE_REGISTERED_SERVICES | RemoteBundleAdmin.INCLUDE_USED_SERVICES;
    Object snapshotData = GET_BUNDLES_SNAPSHOT_METHOD.call(getBundleAdmin(), new Object[] {
        new Integer(options), properties
    });
    if (snapshotData == null) {
      DebugUtils
          .info(this,
              "[getBundlesSnapshot] getBundlesSnapshot() must not return null array. There is a problem with the transport.");
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

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeploymentManager#refreshPackages()
   */
  public void refreshPackages() throws IAgentException {
    final RemoteObject bAdmin = getBundleAdmin();
    if (REFRESH_PACKAGES.isDefined(bAdmin)) {
      Object error = REFRESH_PACKAGES.call(bAdmin);
      if (error instanceof Error) {
        throw new IAgentException((Error) error);
      }
    }
  }

  RemoteBundle getBundle(long id) throws IAgentException {
    DebugUtils.debug(this, "[getBundle] >>> id: " + id);
    RemoteBundle bundle = new RemoteBundleImpl(this, new Long(id));
    int state = bundle.getState();
    if (state != RemoteBundle.UNINSTALLED) {
      DebugUtils.debug(this, "[getBundle] Bundle found with state: " + state);
      return bundle;
    } else {
      DebugUtils.debug(this, "[getBundle] No such bundle");
      return null; // no bundle found
    }
  }

  DeviceConnector getDeviceConnector() {
    return connector;
  }

  RemoteObject getBundleAdmin() throws IAgentException {
    PMPConnection connection = (PMPConnection) connector.getConnection(ConnectionManager.PMP_CONNECTION);
    RemoteObject bundleAdmin = connection.getRemoteBundleAdmin();
    return bundleAdmin;
  }

  RemoteObject getDeploymentAdmin() throws IAgentException {
    PMPConnection connection = (PMPConnection) connector.getConnection(ConnectionManager.PMP_CONNECTION);
    RemoteObject bundleAdmin = connection.getRemoteDeploymentAdmin();
    return bundleAdmin;
  }

  void removeListeners() throws IAgentException {
    DebugUtils.debug(this, "[removeListeners] >>> Removing all listeners...");
    synchronized (dpListeners) {
      dpListeners.clear();
      PMPConnection connection = (PMPConnection) connector.getConnection(ConnectionManager.PMP_CONNECTION, false);
      if (connection != null) {
        DebugUtils.debug(this, "[removeListeners] PMP connection is available, remove event listener for DP events...");
        connection.removeEventListener(this, new String[] {
          DEPLOYMENT_EVENT
        });
      }
      DebugUtils.debug(this, "[removeListeners] deployment package listeners removed");
    }
    synchronized (bundleListeners) {
      bundleListeners.clear();
      PMPConnection connection = (PMPConnection) connector.getConnection(ConnectionManager.PMP_CONNECTION, false);
      if (connection != null) {
        DebugUtils.debug(this,
            "[removeListeners] PMP connection is available, remove event listener for synchronous bundle events...");
        connection.removeEventListener(this, new String[] {
          SYNCH_BUNDLE_EVENT
        });
      }
      DebugUtils.debug(this, "[removeListeners] bundle listeners removed");
    }
  }

  private void fireBundleEvent(long bundleId, int type) {
    DebugUtils.debug(this, "[fireBundleEvent] >>> bundleId: " + bundleId + "; type: " + type);
    RemoteBundleListener[] listeners;
    synchronized (bundleListeners) {
      if (bundleListeners.size() != 0) {
        listeners = (RemoteBundleListener[]) bundleListeners.toArray(new RemoteBundleListener[bundleListeners.size()]);
      } else {
        return;
      }
    }
    RemoteBundle bundle = new RemoteBundleImpl(this, new Long(bundleId));
    RemoteBundleEvent event = new RemoteBundleEvent(bundle, type);
    DebugUtils.debug(this, "[fireBundleEvent] " + listeners.length + " listeners found.");
    for (int i = 0; i < listeners.length; i++) {
      RemoteBundleListener listener = listeners[i];
      try {
        DebugUtils.debug(this, "[fireBundleEvent] deliver event: " + event + " to listener: " + listener);
        listener.bundleChanged(event);
      } catch (Throwable e) {
        DebugUtils.error(this, "[fireBundleEvent] Failed to deliver event to " + listener, e);
      }
    }
  }

  private void fireDeploymentEvent(String symbolicName, String version, int type) {
    DebugUtils.debug(this, "[fireDeploymentEvent] >>> symbolicName: " + symbolicName + "; version: " + version
        + "; type: " + type);
    RemoteDPListener[] listeners;
    synchronized (dpListeners) {
      if (dpListeners.size() != 0) {
        listeners = (RemoteDPListener[]) dpListeners.toArray(new RemoteDPListener[dpListeners.size()]);
      } else {
        return;
      }
    }
    DebugUtils.debug(this, "[fireDeploymentEvent] " + listeners.length + " listeners found.");
    RemoteDP dp = new RemoteDPImpl(this, symbolicName, version);
    RemoteDPEvent event = new RemoteDPEvent(dp, type);
    for (int i = 0; i < listeners.length; i++) {
      RemoteDPListener listener = listeners[i];
      try {
        DebugUtils.debug(this, "[fireDeploymentEvent] deliver event: " + event + " to listener: " + listener);
        listener.deploymentPackageChanged(event);
      } catch (Throwable e) {
        DebugUtils.error(this, "Failed to deliver event to " + listener, e);
      }
    }
  }
}
