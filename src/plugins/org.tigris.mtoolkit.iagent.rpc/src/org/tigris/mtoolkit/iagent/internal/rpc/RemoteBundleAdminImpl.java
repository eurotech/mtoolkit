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
package org.tigris.mtoolkit.iagent.internal.rpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.zip.DeflaterOutputStream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;
import org.tigris.mtoolkit.iagent.Error;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.event.EventData;
import org.tigris.mtoolkit.iagent.event.EventSynchronizer;
import org.tigris.mtoolkit.iagent.rpc.AbstractRemoteAdmin;
import org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin;
import org.tigris.mtoolkit.iagent.rpc.spi.BundleManagerDelegate;
import org.tigris.mtoolkit.iagent.util.DebugUtils;

public final class RemoteBundleAdminImpl extends AbstractRemoteAdmin implements RemoteBundleAdmin,
    SynchronousBundleListener {
  public static final String    SYNCH_BUNDLE_EVENTS = "synch_bundle_event";
  public static final String    EVENT_TYPE_KEY      = "type";
  public static final String    EVENT_BUNDLE_ID_KEY = "bundle.id";

  private static final Class[]  CLASSES             = new Class[] {
                                                      RemoteBundleAdmin.class
                                                    };

  private BundleContext         bc;
  private ServiceRegistration   registration;
  private ServiceTracker        packageAdminTrack;
  private ServiceTracker        startLevelTrack;
  private ServiceTracker        delegatesTrack;

  private BundleManagerDelegate defaultDelegate;

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.Remote#remoteInterfaces()
   */
  public Class[] remoteInterfaces() {
    return CLASSES;
  }

  public void register(BundleContext bc) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[register] Registering remote Bundle Admin...");
    }
    this.bc = bc;

    this.defaultDelegate = new DefaultBundleManagerDelegate(bc);

    packageAdminTrack = new ServiceTracker(bc, PackageAdmin.class.getName(), null);
    packageAdminTrack.open();

    startLevelTrack = new ServiceTracker(bc, StartLevel.class.getName(), null);
    startLevelTrack.open();

    delegatesTrack = new ServiceTracker(bc, BundleManagerDelegate.class.getName(), null);
    delegatesTrack.open();

    registration = bc.registerService(RemoteBundleAdmin.class.getName(), this, null);

    bc.addBundleListener(this);

    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[register] Remote Bundle Admin Registered.");
    }
  }

  public void unregister(BundleContext bc) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[unregister] Unregistering remoteBundleAdmin...");
    }
    if (registration != null) {
      registration.unregister();
      registration = null;
    }

    if (packageAdminTrack != null) {
      packageAdminTrack.close();
      packageAdminTrack = null;
    }

    if (startLevelTrack != null) {
      startLevelTrack.close();
      startLevelTrack = null;
    }

    bc.removeBundleListener(this);

    this.bc = null;
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[unregister] Remote Bundle Admin unregistered.");
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#getBundleState(long)
   */
  public int getBundleState(long id) {
    Bundle bundle = bc.getBundle(id);
    int bundleState = bundle != null ? bundle.getState() : Bundle.UNINSTALLED;
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getBundleState] id " + id + "; state: " + bundleState);
    }
    return bundleState;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#getBundleLocation(long)
   */
  public String getBundleLocation(long id) {
    Bundle bundle = bc.getBundle(id);
    if (bundle != null) {
      String bundleLocation = bundle.getLocation();
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getBundleLocation] id: " + id + "; location: " + bundleLocation);
      }
      return bundleLocation;
    } else {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getBundleLocation] id: " + id + " -> No such bundle");
      }
      return null;
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#getBundleHeaders(long, java.lang.String)
   */
  public Dictionary getBundleHeaders(long id, String locale) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getBundleHeaders] >>> id: " + id + "; locale: " + locale);
    }
    Bundle bundle = bc.getBundle(id);
    if (bundle == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getBundleHeaders] No such bundle");
      }
      return null;
    }
    Dictionary headers = bundle.getHeaders(locale);
    Dictionary converted = new Hashtable(headers.size(), 1f);
    for (Enumeration e = headers.keys(); e.hasMoreElements();) {
      Object key = e.nextElement();
      converted.put(key.toString(), headers.get(key).toString());
    }
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getBundleHeaders] headers: " + DebugUtils.convertForDebug(converted));
    }
    return converted;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#getBundleHeader(long, java.lang.String, java.lang.String)
   */
  public Object getBundleHeader(long id, String headerName, String locale) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getBundleHeader] >>> id: " + id + "; headerName" + headerName + "; locale" + locale);
    }
    Bundle bundle = bc.getBundle(id);
    if (bundle == null) {
      Error error = new Error(Error.BUNDLE_UNINSTALLED_CODE, "Bundle " + id + " has been uninstalled");
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getBundleHeader] No such bundle: " + error);
      }
      return error;
    }
    Dictionary headers = bundle.getHeaders(locale);
    Object value = headers.get(headerName);
    Object bundleHeader = value != null ? value.toString() : null;
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getBundleHeader] header value: " + bundleHeader);
    }
    return bundleHeader;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#isBundleSigned(long)
   */
  public boolean isBundleSigned(long id) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[isBundleSigned] >>> id: " + id);
    }
    Bundle bundle = bc.getBundle(id);
    if (bundle == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getBundleHeaders] No such bundle");
      }
      return false;
    }
    Map ss = bundle.getSignerCertificates(Bundle.SIGNERS_ALL);
    boolean signed = !ss.isEmpty();
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[isBundleSigned] result: " + signed);
    }
    return signed;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#getBundleLastModified(long)
   */
  public long getBundleLastModified(long id) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getBundleLastModified] >>> id: " + id);
    }
    Bundle bundle = bc.getBundle(id);
    if (bundle == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getBundleLastModified] No such bundle");
      }
      return -2; // -1 value is often used to indicate unknown value, so
      // we return -2
      // it is hard for impl. to return -2 meaning that the bundle was
      // last modified 2 ms before 1 Jan, 1970:)
    }
    long bundleLastModified = bundle.getLastModified();
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getBundleLastModified] last modified: " + bundleLastModified);
    }
    return bundleLastModified;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#getBundleSymbolicName(long)
   */
  public String getBundleSymbolicName(long id) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getBundleSymbolicName] >>> id: " + id);
    }
    Bundle bundle = bc.getBundle(id);
    if (bundle == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getBundleSymbolicName] No such bundle");
      }
      return null;
    }
    String symbolicName = bundle.getSymbolicName();
    symbolicName = symbolicName != null ? symbolicName : "";
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getBundleSymbolicName] symbolic name: " + symbolicName);
    }
    return symbolicName;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#startBundle(long, int)
   */
  public Object startBundle(long id, int flags) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[startBundle] >>> id: " + id + "; flags" + flags);
    }
    Bundle bundle = bc.getBundle(id);
    if (bundle == null) {
      Error error = new Error(Error.BUNDLE_UNINSTALLED_CODE, "Bundle " + id + " has been uninstalled");
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[startBundle] No such bundle");
      }
      return error;
    }
    try {
      try {
        bundle.start(flags);
      } catch (NoSuchMethodError e) { // no OSGi R4.1 support
        bundle.start();
      }
    } catch (BundleException e) {
      Error error = new Error(getBundleErrorCode(e), "Failed to start bundle: " + DebugUtils.toString(e),
          DebugUtils.getStackTrace(e));
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[startBundle] Bundle cannot be started: " + error, e);
      }
      return error;
    } catch (IllegalStateException e) {
      Error error = new Error(Error.BUNDLE_UNINSTALLED_CODE, "Bundle " + id + " has been uninstalled");
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[startBundle] No such bundle: " + error);
      }
      return error;
    } catch (Throwable t) {
      Error error = new Error(IAgentErrors.ERROR_BUNDLE_UNKNOWN, "Failed to start bundle: " + DebugUtils.toString(t),
          DebugUtils.getStackTrace(t));
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[startBundle] Bundle cannot be started: " + error, t);
      }
      return error;
    }
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[startBundle] Bundle started successfully");
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#stopBundle(long, int)
   */
  public Object stopBundle(long id, int flags) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[stopBundle] id: " + id + "; flags" + flags);
    }
    Bundle bundle = bc.getBundle(id);
    if (bundle == null) {
      Error error = new Error(Error.BUNDLE_UNINSTALLED_CODE, "Bundle " + id + " has been uninstalled");
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[stopBundle] No such bundle: " + error);
      }
      return error;
    }
    try {
      try {
        bundle.stop(flags);
      } catch (NoSuchMethodError e) { // no OSGi R4.1 support
        bundle.stop();
      }
    } catch (BundleException e) {
      Error error = new Error(getBundleErrorCode(e), "Failed to stop bundle: " + DebugUtils.toString(e),
          DebugUtils.getStackTrace(e));
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[stopBundle] Unable to stop bundle: " + error, e);
      }
      return error;
    } catch (IllegalStateException e) {
      Error error = new Error(Error.BUNDLE_UNINSTALLED_CODE, null);
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[stopBundle] No such bundle: " + error);
      }
      return error;
    } catch (Throwable t) {
      Error error = new Error(IAgentErrors.ERROR_BUNDLE_UNKNOWN, "Failed to stop bundle: " + DebugUtils.toString(t),
          DebugUtils.getStackTrace(t));
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[stopBundle] Unable to stop bundle: " + error, t);
      }
      return error;
    }
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[stopBundle] Successfully stopped");
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#resolveBundles(long[])
   */
  public boolean resolveBundles(long[] ids) {
    if (ids == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[resolveBundles] Passed bundle ids must not be null");
      }
      throw new IllegalArgumentException("Passed bundle ids must be not null");
    }
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[resolveBundles] bundles: " + DebugUtils.convertForDebug(ids));
    }
    Vector v = new Vector();
    for (int i = 0; i < ids.length; i++) {
      Bundle b = bc.getBundle(ids[i]);
      if (b != null) {
        v.addElement(b);
      }
    }
    Bundle[] bs = null;
    if (v.size() > 0) {
      bs = new Bundle[v.size()];
      v.copyInto(bs);
    }
    PackageAdmin packageAdmin = (PackageAdmin) packageAdminTrack.getService();
    if (packageAdmin == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[resolveBundles] PackageAdmin service is not available!");
      }
      throw new IllegalStateException("PackageAdmin is not available at the moment");
    }
    boolean areBundlesResolved = packageAdmin.resolveBundles(bs);
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[resolveBundles] Bundles resolved successfully: " + areBundlesResolved);
    }
    return areBundlesResolved;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#listBundles()
   */
  public long[] listBundles() {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[listBundles] >>>");
    }
    Bundle[] bundles = bc.getBundles();
    long[] bids = convertBundlesToIds(bundles);
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[listBundles] bundles: " + DebugUtils.convertForDebug(bids));
    }
    return bids;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#getBundlesSnapshot(int, java.util.Dictionary)
   */
  public Object getBundlesSnapshot(int includeOptions, Dictionary properties) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getBundlesSnapshot] >>>");
    }
    long[] ids = listBundles();
    List snapshots = new ArrayList();
    for (int i = 0; i < ids.length; i++) {
      Dictionary bundleInfo = new Hashtable(5, 1f);
      bundleInfo.put(KEY_BUNDLE_ID, new Long(ids[i]));
      if ((includeOptions & INCLUDE_BUNDLE_HEADERS) != 0) {
        Dictionary headers = getBundleHeaders(ids[i], null);
        if (headers == null) {
          continue; // bundle is uninstalled
        }
        bundleInfo.put(KEY_BUNDLE_HEADERS, headers);
      }
      if ((includeOptions & INCLUDE_BUNDLE_STATES) != 0) {
        int state = getBundleState(ids[i]);
        if (state == Bundle.UNINSTALLED) {
          continue; // bundle is uninstalled
        }
        bundleInfo.put(KEY_BUNDLE_STATE, new Integer(state));
      }
      if ((includeOptions & INCLUDE_REGISTERED_SERVICES) != 0) {
        Dictionary[] registeredServices = getRegisteredServices(ids[i]);
        if (registeredServices == null) {
          continue; // bundle is uninstalled
        }
        bundleInfo.put(KEY_REGISTERED_SERVICES, registeredServices);
      }
      if ((includeOptions & INCLUDE_USED_SERVICES) != 0) {
        Dictionary[] usedServices = getUsingServices(ids[i]);
        if (usedServices == null) {
          continue; // bundle is uninstalled
        }
        bundleInfo.put(KEY_USED_SERVICES, usedServices);
      }
      snapshots.add(bundleInfo);
    }
    Dictionary[] result = (Dictionary[]) snapshots.toArray(new Dictionary[snapshots.size()]);

    String transportType = System.getProperty("iagent.snapshot.transport.type");
    if ("compressed".equals(transportType)) {
      try {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(new DeflaterOutputStream(bos));
        oos.writeObject(result);
        oos.close();
        return bos.toByteArray();
      } catch (IOException e) {
        // failed to compress result, return uncompressed data
      }
    }
    return result;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#installBundle(java.lang.String, java.io.InputStream)
   */
  public Object installBundle(String location, InputStream is) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[installBundle] location: " + location + "; inputStream: " + is);
    }
    Object result = getDelegate().installBundle(location, is);
    if (result instanceof Error) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[installBundle] Unable to install bundle: " + result);
      }
      return result;
    }

    Bundle bundle = (Bundle) result;
    Long bundleId = new Long(bundle.getBundleId());
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[installBundle] Bundle installed successfully. Id: " + bundleId);
    }
    return bundleId;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#uninstallBundle(long)
   */
  public Object uninstallBundle(long id) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[uninstallBundle] id: " + id);
    }
    Bundle bundle = bc.getBundle(id);
    if (bundle != null) {
      Object result = getDelegate().uninstallBundle(bundle);
      if (result instanceof Error) {
        if (DebugUtils.DEBUG_ENABLED) {
          DebugUtils.debug(this, "[uninstallBundle] Unable to uninstall bundle: " + result);
        }
        return result;
      }
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[uninstallBundle] Bundle uninstalled");
      }
      return result;
    } else {
      Error error = new Error(Error.BUNDLE_UNINSTALLED_CODE, "Bundle " + id + " has been uninstalled");
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[uninstallBundle] Unable to uninstall bundle: " + error);
      }
      return error;
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#getBundles(java.lang.String, java.lang.String)
   */
  public long[] getBundles(String symbolicName, String version) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getBundles] symbolicName: " + symbolicName + "; version: " + version);
    }
    PackageAdmin admin = (PackageAdmin) packageAdminTrack.getService();
    if (admin == null) {
      throw new IllegalStateException("No PackageAdmin available");
    }
    Bundle[] bundles = admin.getBundles(symbolicName, version);
    long[] bids = convertBundlesToIds(bundles);
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getBundles] Bundles successfully gotten: " + DebugUtils.convertForDebug(bids));
    }
    return bids;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#updateBundle(long, java.io.InputStream)
   */
  public Object updateBundle(long id, InputStream is) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[updateBundle] installBundle; id: " + id + "; inputStream: " + is);
    }
    Bundle bundle = bc.getBundle(id);
    if (bundle == null) {
      Error error = new Error(Error.BUNDLE_UNINSTALLED_CODE, "Bundle " + id + " has been uninstalled");
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[updateBundle] No such bundle: " + error);
      }
      return error;
    } else {
      Object result = getDelegate().updateBundle(bundle, is);
      if (result instanceof Error) {
        if (DebugUtils.DEBUG_ENABLED) {
          DebugUtils.debug(this, "[updateBundle] Unable to update bundle: " + result);
        } else if (DebugUtils.DEBUG_ENABLED) {
          DebugUtils.debug(this, "[updateBundle] Bundle updated successfully");
        }
      }
      return result;
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#getRegisteredServices(long)
   */
  public Dictionary[] getRegisteredServices(long id) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getRegisteredServices] id: " + id);
    }
    Bundle bundle = bc.getBundle(id);
    if (bundle == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getRegisteredServices] No such bundle");
      }
      return null;
    }
    try {
      ServiceReference[] refs = bundle.getRegisteredServices();
      return RemoteServiceAdminImpl.convertReferences(refs);
    } catch (IllegalStateException e) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getRegisteredServices] No such bundle");
      }
      return null;
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#getUsingServices(long)
   */
  public Dictionary[] getUsingServices(long id) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getUsingServices] id: " + id);
    }
    Bundle bundle = bc.getBundle(id);
    if (bundle == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getUsingServices] No such bundle");
      }
      return null;
    }
    try {
      ServiceReference[] refs = bundle.getServicesInUse();
      Dictionary[] convertedReferences = RemoteServiceAdminImpl.convertReferences(refs);
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getUsingServices] Used services: " + DebugUtils.convertForDebug(convertedReferences));
      }
      return convertedReferences;
    } catch (IllegalStateException e) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getUsingServices] No such bundle");
      }
      return null;
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#getFragmentBundles(long)
   */
  public long[] getFragmentBundles(long id) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getFragmentBundles] id: " + id);
    }
    Bundle bundle = bc.getBundle(id);
    if (bundle == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getFragmentBundles] No such bundle");
      }
      return null;
    }
    PackageAdmin admin = (PackageAdmin) packageAdminTrack.getService();
    if (admin == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getFragmentBundles] No packageAdmin");
      }
      return new long[0];
    }
    Bundle[] fragmentBundles = admin.getFragments(bundle);
    long[] bids = convertBundlesToIds(fragmentBundles);
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this,
          "[getFragmentBundles] Fragment bundles successfully gotten: " + DebugUtils.convertForDebug(bids));
    }
    return bids;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#getHostBundles(long)
   */
  public long[] getHostBundles(long id) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getHostBundles] id: " + id);
    }
    Bundle bundle = bc.getBundle(id);
    if (bundle == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getHostBundles] No such bundle");
      }
      return null;
    }
    PackageAdmin admin = (PackageAdmin) packageAdminTrack.getService();
    if (admin == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getHostBundles] No packageAdmin");
      }
      return new long[0];
    }
    Bundle[] hostBundles = admin.getHosts(bundle);
    long[] bids = convertBundlesToIds(hostBundles);
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getHostBundles] Host bundles successfully gotten: " + DebugUtils.convertForDebug(bids));
    }
    return bids;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#getBundleType(long)
   */
  public int getBundleType(long id) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getBundleType] id: " + id);
    }
    Bundle bundle = bc.getBundle(id);
    if (bundle == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getBundleType] No such bundle");
      }
      return -1;
    }
    PackageAdmin admin = (PackageAdmin) packageAdminTrack.getService();
    if (admin == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getBundleType] No packageAdmin");
      }
      return -2;
    }
    int bundleType = admin.getBundleType(bundle);
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getBundleType] Bundle Type successfully gotten: " + bundleType);
    }
    return bundleType;
  }

  /* (non-Javadoc)
   * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
   */
  public void bundleChanged(BundleEvent event) {
    final Bundle systemBundle = bc.getBundle(0);
    if (systemBundle.getState() == Bundle.STOPPING) {
      return; // don't send events when the framework shutting down
    }
    if (bc.getBundle().getState() == Bundle.STOPPING) {
      return; // stop sending events when the iagent bundle is stopping,
    }
    // they're inspectation will lead to errors
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[bundleChanged] Event type is BundleEvent." + event.getType());
    }

    EventSynchronizer synchronizer = Activator.getSynchronizer();
    if (synchronizer != null) {
      Dictionary convEvent = convertBundleEvent(event);
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this,
            "[bundleChanged] Sending event through existing pmpConnection. eventType: " + event.getType());
      }
      synchronizer.enqueue(new EventData(convEvent, SYNCH_BUNDLE_EVENTS));
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[bundleChanged] Bundle successfully changed");
      }
    } else {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[bundleChanged] Event synchronizer was disabled.");
      }
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#getBundleByLocation(java.lang.String)
   */
  public long getBundleByLocation(String location) {
    if (location == null) {
      throw new IllegalArgumentException("getBundleByLocation requires non-null string passed as arg");
    }
    Bundle[] bundles = bc.getBundles();
    for (int i = 0; i < bundles.length; i++) {
      Bundle bundle = bundles[i];
      if (location.equals(bundle.getLocation())) {
        return bundle.getBundleId();
      }
    }
    // no bundle found
    return -1;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#getAgentData()
   */
  public String[] getAgentData() {
    Bundle agentBundle = bc.getBundle();
    String agentVersion = (String) agentBundle.getHeaders().get(Constants.BUNDLE_VERSION);
    String[] agentData = new String[] {
        Long.toString(agentBundle.getBundleId()), agentVersion
    };
    return agentData;
  }

  public int getBundleStartLevel(long id) {
    Bundle bundle = bc.getBundle(id);
    StartLevel slService = (StartLevel) startLevelTrack.getService();
    if (slService != null) {
      return slService.getBundleStartLevel(bundle);
    } else {
      return -1;
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#getFrameworkStartLevel()
   */
  public int getFrameworkStartLevel() {
    StartLevel slService = (StartLevel) startLevelTrack.getService();
    if (slService != null) {
      return slService.getStartLevel();
    } else {
      return -1;
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#getSystemProperty(java.lang.String)
   */
  public String getSystemProperty(String name) {
    return System.getProperty(name);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#setSystemProperty(java.lang.String, java.lang.String)
   */
  public Object setSystemProperty(String name, String value) {
    System.setProperty(name, value);
    return null;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#getBundleResource(long, java.lang.String, java.util.Dictionary)
   */
  public Object getBundleResource(long id, String name, Dictionary properties) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getBundleResource] id: " + id);
    }
    if (name == null) {
      throw new IllegalArgumentException("getBundleResource requires non-null name");
    }
    Bundle bundle = bc.getBundle(id);
    if (bundle == null) {
      return new Error(Error.BUNDLE_UNINSTALLED_CODE, "Bundle " + id + " has been uninstalled");
    }
    URL resource = bundle.getResource(name);
    if (resource == null) {
      return null;
    }
    try {
      return resource.openStream();
    } catch (IOException e) {
      return new Error(IAgentErrors.ERROR_BUNDLE_UNKNOWN, "Failed to get resource: " + e.getMessage());
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#refreshPackages()
   */
  public Object refreshPackages() {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[refreshPackages]");
    }
    PackageAdmin admin = (PackageAdmin) packageAdminTrack.getService();
    if (admin == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[refreshPackages] No packageAdmin");
      }
    } else {
      admin.refreshPackages(null);
    }
    return null;
  }

  /* (non-Javadoc)
  * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#getSignerCertificates(long)
  */
  public Dictionary getSignerCertificates(long id) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getSignerCertificates] >>> id: " + id);
    }
    Bundle bundle = bc.getBundle(id);
    if (bundle == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getSignerCertificates] No such bundle");
      }
      return null;
    }
    final Map signers = bundle.getSignerCertificates(Bundle.SIGNERS_ALL);
    final Dictionary extMap = new Hashtable(signers.size(), 1f);
    final Iterator entries = signers.entrySet().iterator();
    while (entries.hasNext()) {
      final Entry entry = (Entry) entries.next();
      final X509Certificate cert = (X509Certificate) entry.getKey();
      final List chain = (List) entry.getValue();
      List chain_mod = new ArrayList(chain.size());
      chain_mod.addAll(chain);
      extMap.put(cert, chain_mod);
    }
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getSignerCertificates] result: " + extMap.toString());
    }
    return extMap;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin#isSignerTrusted(long)
   */
  public boolean isSignerTrusted(long id) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[isSignerTrusted] >>> id: " + id);
    }
    Bundle bundle = bc.getBundle(id);
    if (bundle == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[isSignerTrusted] No such bundle");
      }
      return false;
    }
    Map ss = bundle.getSignerCertificates(Bundle.SIGNERS_TRUSTED);
    boolean signed = !ss.isEmpty();
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[isSignerTrusted] result: " + signed);
    }
    return signed;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.AbstractRemoteAdmin#getServiceRegistration()
   */
  protected ServiceRegistration getServiceRegistration() {
    return registration;
  }

  static long[] convertBundlesToIds(Bundle[] bundles) {
    if (bundles == null) {
      return new long[0];
    }
    long[] bids = new long[bundles.length];
    for (int i = 0; i < bundles.length; i++) {
      bids[i] = bundles[i].getBundleId();
    }
    return bids;
  }

  public static int getBundleErrorCode(BundleException e) {
    int code;
    try {
      Method getType = BundleException.class.getMethod("getType", null);
      code = ((Integer) getType.invoke(e, null)).intValue();
    } catch (Exception e1) {
      return IAgentErrors.ERROR_BUNDLE_UNKNOWN;
    }

    switch (code) {
    case BundleException.ACTIVATOR_ERROR:
      return IAgentErrors.ERROR_BUNDLE_ACTIVATOR;
    case BundleException.DUPLICATE_BUNDLE_ERROR:
      return IAgentErrors.ERROR_BUNDLE_DUPLICATE;
    case BundleException.INVALID_OPERATION:
      return IAgentErrors.ERROR_BUNDLE_INVALID_OPERATION;
    case BundleException.MANIFEST_ERROR:
      return IAgentErrors.ERROR_BUNDLE_MANIFEST;
    case BundleException.NATIVECODE_ERROR:
      return IAgentErrors.ERROR_BUNDLE_NATIVECODE;
    case BundleException.RESOLVE_ERROR:
      return IAgentErrors.ERROR_BUNDLE_RESOLVE;
    case BundleException.SECURITY_ERROR:
      return IAgentErrors.ERROR_BUNDLE_SECURITY;
    case BundleException.START_TRANSIENT_ERROR:
      return IAgentErrors.ERROR_BUNDLE_START_TRANSIENT;
    case BundleException.STATECHANGE_ERROR:
      return IAgentErrors.ERROR_BUNDLE_STATECHANGE;
    case BundleException.UNSUPPORTED_OPERATION:
      return IAgentErrors.ERROR_BUNDLE_UNSUPPORTED_OPERATION;
    default:
      return IAgentErrors.ERROR_BUNDLE_UNKNOWN;
    }
  }

  private Dictionary convertBundleEvent(BundleEvent bEvent) {
    Dictionary event = new Hashtable(2, 1f);
    event.put(EVENT_TYPE_KEY, new Integer(bEvent.getType()));
    event.put(EVENT_BUNDLE_ID_KEY, new Long(bEvent.getBundle().getBundleId()));
    return event;
  }

  private BundleManagerDelegate getDelegate() {
    BundleManagerDelegate delegate = (BundleManagerDelegate) delegatesTrack.getService();
    if (delegate != null) {
      return delegate;
    }
    return defaultDelegate;
  }

}
