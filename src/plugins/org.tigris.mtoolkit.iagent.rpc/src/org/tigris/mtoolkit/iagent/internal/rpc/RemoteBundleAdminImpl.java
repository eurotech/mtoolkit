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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.rpc.Error;
import org.tigris.mtoolkit.iagent.rpc.Remote;
import org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin;


public class RemoteBundleAdminImpl implements Remote, RemoteBundleAdmin, SynchronousBundleListener {

	  public static final String SYNCH_BUNDLE_EVENTS = "synch_bundle_event";
	  public static final String SYSTEM_BUNDLE_EVENT = "system_bundle_event";
	  public static final String EVENT_TYPE_KEY = "type";
	  public static final String EVENT_BUNDLE_ID_KEY = "bundle.id";
	  
  private static final String PROP_SYSTEM_BUNDLES_LIST = "iagent.system.bundles.list";
  private static final String SYSTEM_BUNDLES_FILE_NAME = "system_bundles.txt";
  private static final String SYSTEM_BUNDLES_RESOURCE_NAME = "/" + SYSTEM_BUNDLES_FILE_NAME;

  private ServiceTracker packageAdminTrack;
  private ServiceRegistration registration;
  private BundleContext bc;

  private Set loadedSymbolicNames;
  private Bundle systemBundle;

  public Class[] remoteInterfaces() {
    return new Class[] {RemoteBundleAdmin.class};
  }

  public void register(BundleContext bc) {
    log("[register] Registering remote Bundle Admin...");
    this.bc = bc;

    packageAdminTrack = new ServiceTracker(bc, PackageAdmin.class.getName(), null);
    packageAdminTrack.open();

    registration = bc.registerService(RemoteBundleAdmin.class.getName(), this, null);

    bc.addBundleListener(this);

    log("[register] Remote Bundle Admin Registered.");
  }

  public void unregister(BundleContext bc) {
    log("[unregister] Unregistering remoteBundleAdmin...");
    if (registration != null) {
      registration.unregister();
      registration = null;
    }

    if (packageAdminTrack != null) {
      packageAdminTrack.close();
      packageAdminTrack = null;
    }

    bc.removeBundleListener(this);

    this.bc = null;
    log("[unregister] Remote Bundle Admin unregistered.");
  }

  public int getBundleState(long id) {
    Bundle bundle = bc.getBundle(id);
    int bundleState = bundle != null ? bundle.getState() : Bundle.UNINSTALLED;
    log("[getBundleState] id " + id + "; state: " + bundleState);
    return bundleState;
  }

  public String getBundleLocation(long id) {
    Bundle bundle = bc.getBundle(id);
    if (bundle != null) {
      String bundleLocation = bundle.getLocation();
      log("[getBundleLocation] id: " + id + "; location: " + bundleLocation);
      return bundleLocation;
    } else {
      log("[getBundleLocation] id: " + id + " -> No such bundle");
      return null;
    }
  }

  public Dictionary getBundleHeaders(long id, String locale) {
    log("[getBundleHeaders] >>> id: " + id + "; locale: " + locale);
    Bundle bundle = bc.getBundle(id);
    if (bundle == null){
      log("[getBundleHeaders] No such bundle");
      return null;
    }
    Dictionary headers = bundle.getHeaders(locale);
    Dictionary converted = new Hashtable();
    for (Enumeration e = headers.keys(); e.hasMoreElements();) {
      Object key = e.nextElement();
      converted.put(key.toString(), headers.get(key).toString());
    }
    log("[getBundleHeaders] headers: " + DebugUtils.convertForDebug(converted));
    return converted;
  }

  public Object getBundleHeader(long id, String headerName, String locale) {
    log("[getBundleHeader] >>> id: " + id + "; headerName" + headerName + "; locale" + locale);
    Bundle bundle = bc.getBundle(id);
    if (bundle == null){
      Error error = new Error(Error.BUNDLE_UNINSTALLED_CODE, null);
      log("[getBundleHeader] No such bundle", error);
      return error;
    }
    Dictionary headers = bundle.getHeaders(locale);
    Object value = headers.get(headerName);
    Object bundleHeader = value != null ? value.toString() : null;
    log("[getBundleHeader] header value: " + bundleHeader);
    return bundleHeader;
  }

  public long getBundleLastModified(long id) {
    log("[getBundleLastModified] >>> id: " + id);
    Bundle bundle = bc.getBundle(id);
    if (bundle == null) {
      log("[getBundleLastModified] No such bundle");
      return -2;    // -1 value is often used to indicate unknown value, so we return -2
      // it is hard for impl. to return -2 meaning that the bundle was
      // last modified 2 ms before 1 Jan, 1970:)
    }
    long bundleLastModified = bundle.getLastModified();
    log("[getBundleLastModified] last modified: " + bundleLastModified);
    return bundleLastModified;
  }

  public String getBundleSymbolicName(long id) {
    log("[getBundleSymbolicName] >>> id: " + id);
    Bundle bundle = bc.getBundle(id);
    if (bundle == null) {
      log("[getBundleSymbolicName] No such bundle");
      return null;
    }
    String symbolicName = bundle.getSymbolicName();
    symbolicName = symbolicName != null ? symbolicName : "";
    log("[getBundleSymbolicName] symbolic name: " + symbolicName);
    return symbolicName;
  }

  public Object startBundle(long id, int flags) {
    log("[startBundle] >>> id: " + id + "; flags" + flags);
    Bundle bundle = bc.getBundle(id);
    if (bundle == null){
      Error error = new Error(Error.BUNDLE_UNINSTALLED_CODE, null);
      log("[startBundle] No such bundle", error);
      return error;
    }
    try {
      try {
        bundle.start(flags);
      } catch (NoSuchMethodError e) {   // no OSGi R4.1 support
        bundle.start();
      }
    } catch (BundleException e) {
      Error error = new Error(IAgentErrors.ERROR_BUNDLE_UNKNOWN, "Failed to start bundle: " + e.getMessage());
      log("[startBundle] Bundle cannot be started: " + error, e);
      return error;
    } catch (IllegalStateException e) {
      Error error = new Error(Error.BUNDLE_UNINSTALLED_CODE, null);
      log("[startBundle] No such bundle", error);
      return error;
    }
    log("[startBundle] Bundle started successfully");
    return null;
  }

  public Object stopBundle(long id, int flags) {
    log("[stopBundle] id: " + id + "; flags" + flags);
    Bundle bundle = bc.getBundle(id);
    if (bundle == null) {
      Error error = new Error(Error.BUNDLE_UNINSTALLED_CODE, null);
      log("[stopBundle] No such bundle",error);
      return error;
    }
    try {
      try {
        bundle.stop(flags);
      } catch (NoSuchMethodError e) { // no OSGi R4.1 support
        bundle.stop();
      }
    } catch (BundleException e) {
      Error error = new Error(IAgentErrors.ERROR_BUNDLE_UNKNOWN, "Failed to stop bundle: " + e.getMessage());
      log("[stopBundle] Unable to stop bundle: " + error, e);
      return error;
    } catch (IllegalStateException e) {
      Error error = new Error(Error.BUNDLE_UNINSTALLED_CODE, null);
      log("[stopBundle] No such bundle",error);
      return error;
    }
    log("[stopBundle] Successfully stopped");
    return null;
  }

  public boolean resolveBundles(long[] ids) {
    if (ids == null){
      log("[resolveBundles] Passed bundle ids must not be null");
      throw new IllegalArgumentException("Passed bundle ids must be not null");
    }
    if (DebugUtils.DEBUG)
      log("[resolveBundles] bundles: " + DebugUtils.convertForDebug(ids));
    Vector v = new Vector();
    for (int i = 0; i < ids.length; i++) {
      Bundle b = bc.getBundle(ids[i]);
      if (b != null)
        v.addElement(b);
    }
    Bundle[] bs = null;
    if (v.size() > 0) {
      bs = new Bundle[v.size()];
      v.copyInto(bs);
    }

    PackageAdmin packageAdmin = (PackageAdmin) packageAdminTrack.getService();
    if (packageAdmin == null) {
      log("[resolveBundles] PackageAdmin service is not available!");
      throw new IllegalStateException("PackageAdmin is not available at the moment");
    }
    boolean areBundlesResolved = packageAdmin.resolveBundles(bs);
    log("[resolveBundles] Bundles resolved successfully: " + areBundlesResolved);
    return areBundlesResolved;
  }

  public long[] listBundles() {
    log("[listBundles] >>>");
    Bundle[] bundles = bc.getBundles();
    long[] bids = convertBundlesToIds(bundles);
    if (DebugUtils.DEBUG)
      log("[listBundles] bundles: " + DebugUtils.convertForDebug(bids));
    return bids;
  }

  public Object installBundle(String location, InputStream is) {
    if (DebugUtils.DEBUG)
      log("[installBundle] location: " + location + "; inputStream: " + is);
    Bundle bundle;
    try {
      bundle = bc.installBundle(location, is);
    } catch (BundleException e) {
      Error error = new Error(IAgentErrors.ERROR_BUNDLE_UNKNOWN, "Failed to install bundle: " + e.getMessage());
      log("[installBundle] Unable to install bundle", error);
      return error;
    }

    try {
      is.close();
    } catch (Exception exc) {
      // ignore
    }
    Long bundleId = new Long(bundle.getBundleId());
    if (DebugUtils.DEBUG)
      log("[installBundle] Bundle installed successfully. Id: " + bundleId);
    return bundleId;
  }

  public Object uninstallBundle(long id) {
    if (DebugUtils.DEBUG)
      log("[uninstallBundle] id: " + id);
    Bundle bundle = bc.getBundle(id);
    if (bundle != null) {
      try {
        bundle.uninstall();
        log("[uninstallBundle] Bundle uninstalled");
        return null;
      } catch (BundleException e) {
        Error error = new Error(IAgentErrors.ERROR_BUNDLE_UNKNOWN, "Failed to uninstall bundle: " + e.getMessage());
        log("[uninstallBundle] Unable to uninstall bundle",error);
        return error;
      }
    } else {
      Error error = new Error(Error.BUNDLE_UNINSTALLED_CODE, null);
      log("[uninstallBundle] Unable to uninstall bundle",error);
      return error;
    }
  }

  public long[] getBundles(String symbolicName, String version) {
    log("[getBundles] symbolicName: " + symbolicName + "; version: " + version);
    PackageAdmin admin = (PackageAdmin) packageAdminTrack.getService();
    if (admin == null){
      throw new IllegalStateException("No PackageAdmin available");
    }
    Bundle[] bundles = admin.getBundles(symbolicName, version);
    long[] bids = convertBundlesToIds(bundles);
    if (DebugUtils.DEBUG)
      log("[getBundles] Bundles successfully gotten: " + DebugUtils.convertForDebug(bids));
    return bids;
  }

  public Object updateBundle(long id, InputStream is) {
    if (DebugUtils.DEBUG)
      log("[updateBundle] installBundle; id: " + id + "; inputStream: " + is);
    Bundle bundle = bc.getBundle(id);
    if (bundle == null) {
      Error error = new Error(Error.BUNDLE_UNINSTALLED_CODE, null);
      log("[updateBundle] No such bundle", error);
      return error;
    } else
      try {
        bundle.update(is);
        log("[updateBundle] Bundle updated successfully");
        return null;
      } catch (BundleException e) {
        Error error = new Error(IAgentErrors.ERROR_BUNDLE_UNKNOWN, "Failed to update bundle: " + e.getMessage());
        log("[updateBundle] Unable to update bundle", error);
        return error;
      } catch (IllegalStateException e) {
        Error error = new Error(Error.BUNDLE_UNINSTALLED_CODE, null);
        log("[updateBundle] No such bundle", error);
        return error;
      }
  }

  public Dictionary[] getRegisteredServices(long id) {
    if (DebugUtils.DEBUG)
      log("[getRegisteredServices] id: " + id);
    Bundle bundle = bc.getBundle(id);
    if (bundle == null) {
      log("[getRegisteredServices] No such bundle");
      return null;
    }
    try {
      ServiceReference[] refs = bundle.getRegisteredServices();
      return RemoteServiceAdminImpl.convertReferences(refs);
    } catch (IllegalStateException e) {
      log("[getRegisteredServices] No such bundle");
      return null;
    }
  }

  public Dictionary[] getUsingServices(long id) {
    log("[getUsingServices] id: " + id);
    Bundle bundle = bc.getBundle(id);
    if (bundle == null) {
      log("[getUsingServices] No such bundle");
      return null;
    }
    try {
      ServiceReference[] refs = bundle.getServicesInUse();
      Dictionary[] convertedReferences = RemoteServiceAdminImpl.convertReferences(refs);
      if (DebugUtils.DEBUG)
        log("[getUsingServices] Used services: " + DebugUtils.convertForDebug(convertedReferences));
      return convertedReferences;
    } catch (IllegalStateException e) {
      log("[getUsingServices] No such bundle");
      return null;
    }
  }

  public long[] getFragmentBundles(long id) {
    log("[getFragmentBundles] id: " + id);
    Bundle bundle = bc.getBundle(id);
    if (bundle == null) {
      log("[getFragmentBundles] No such bundle");
      return null;
    }
    PackageAdmin admin = (PackageAdmin) packageAdminTrack.getService();
    if (admin == null) {
      log("[getFragmentBundles] No packageAdmin");
      return new long[0];
    }
    Bundle[] fragmentBundles = admin.getFragments(bundle);
    long[] bids = convertBundlesToIds(fragmentBundles);
    log("[getFragmentBundles] Fragment bundles successfully gotten: " + DebugUtils.convertForDebug(bids));
    return bids;
  }

  public long[] getHostBundles(long id) {
    log("[getHostBundles] id: " + id);
    Bundle bundle = bc.getBundle(id);
    if (bundle == null){
      log("[getHostBundles] No such bundle");
      return null;}
    PackageAdmin admin = (PackageAdmin) packageAdminTrack.getService();
    if (admin == null){
      log("[getHostBundles] No packageAdmin");
      return new long[0];
    }
    Bundle[] hostBundles = admin.getHosts(bundle);
    long[] bids = convertBundlesToIds(hostBundles);
    log("[getHostBundles] Host bundles successfully gotten: " + DebugUtils.convertForDebug(bids));
    return bids;
  }

  public int getBundleType(long id) {
    log("[getBundleType] id: " + id);
    Bundle bundle = bc.getBundle(id);
    if (bundle == null){
      log("[getBundleType] No such bundle");
      return -1;
    }
    PackageAdmin admin = (PackageAdmin) packageAdminTrack.getService();
    if (admin == null){
      log("[getBundleType] No packageAdmin");
      return -2;
    }
    int bundleType = admin.getBundleType(bundle);
    log("[getBundleType] Bundle Type successfully gotten: " + bundleType);
    return bundleType;
  }

  static long[] convertBundlesToIds(Bundle[] bundles) {
    if (bundles == null)
      return new long[0];
    long[] bids = new long[bundles.length];
    for (int i = 0; i < bundles.length; i++)
      bids[i] = bundles[i].getBundleId();
    return bids;
  }

  public void bundleChanged(BundleEvent event) {
    if (systemBundle == null)
      systemBundle = bc.getBundle(0);
    if (systemBundle.getState() == Bundle.STOPPING)
      return;   // don't send events when the framework shutting down
    if (bc.getBundle().getState() == Bundle.STOPPING)
      return;   // stop sending events when the iagent bundle is stopping, they're inspectation will lead to errors
    log("[bundleChanged] Event type is BundleEvent." + event.getType());
    EventSynchronizer synchronizer = Activator.getSynchronizer();
    if (synchronizer != null) {
      Dictionary convEvent = convertBundleEvent(event);
      log("[bundleChanged] Sending event through existing pmpConnection. eventType: " + event.getType());
      String symbolicName = event.getBundle().getSymbolicName();
      if(event.getType() == BundleEvent.INSTALLED && symbolicName != null && isBundleSystem(symbolicName)) {
        // post event if new bundle is installed whose symbolic name is in the system bundles symbolic names
        synchronizer.enqueue(new EventData(new Long(event.getBundle().getBundleId()), SYSTEM_BUNDLE_EVENT));
      }
      synchronizer.enqueue(new EventData(convEvent, SYNCH_BUNDLE_EVENTS));
      log("[bundleChanged] Bundle successfully changed");
    } else {
      log("[bundleChanged] Event synchronizer was disabled.");
    }
  }

  private boolean isBundleSystem(String symbolicName) {
    if (loadedSymbolicNames != null)
      return loadedSymbolicNames.contains(symbolicName);
    else
      return false;
  }

  private Dictionary convertBundleEvent(BundleEvent bEvent) {
    Dictionary event = new Hashtable();
    event.put(EVENT_TYPE_KEY, new Integer(bEvent.getType()));
    event.put(EVENT_BUNDLE_ID_KEY, new Long(bEvent.getBundle().getBundleId()));
    return event;
  }

  private static final void log(String message) {
    log(message, (Throwable) null);
  }

  private static final void log(String message, Error error){
    if (DebugUtils.DEBUG)
      DebugUtils.log(RemoteBundleAdminImpl.class, message + (error != null ? " [" + error + "]": ""));
  }

  private static final void log(String message, Throwable e) {
    if (DebugUtils.DEBUG)
      DebugUtils.log(RemoteBundleAdminImpl.class, message, e);
  }

  // XXX: Extract this method in common base class
  public long getRemoteServiceID() {
    try {
      ServiceRegistration localRegistration = registration;
      if (localRegistration == null)
        return -1;
      ServiceReference localRef = localRegistration.getReference();
      if (localRef == null)
        return -1;
      return ((Long) localRef.getProperty(Constants.SERVICE_ID)).longValue();
    } catch (IllegalStateException e) {
      // catch it in case the service is unregistered mean while
      return -1;
    }
  }

  public long getBundleByLocation(String location) {
    if (location == null)
      throw new IllegalArgumentException("getBundleByLocation requires non-null string passed as arg");
    Bundle[] bundles = bc.getBundles();
    for (int i = 0; i < bundles.length; i++) {
      Bundle bundle = bundles[i];
      if (location.equals(bundle.getLocation()))
        return bundle.getBundleId();
    }
    // no bundle found
    return -1;
  }

  public String[] getAgentData() {
    Bundle agentBundle = bc.getBundle();
    String agentVersion = (String) agentBundle.getHeaders().get(Constants.BUNDLE_VERSION);
    String[] agentData = new String[] {
        Long.toString(agentBundle.getBundleId()), agentVersion
    };
    return agentData;

  }

  public String[] getSystemBundlesNames() {
	  Set symbolicNames = getSystemSymbolicNames();
	  List systemBundlesNames = new ArrayList(symbolicNames.size());
	  Bundle[] allBundles = bc.getBundles();
	    for (int i = 0; i < allBundles.length; i++) {
			for (Iterator iter = symbolicNames.iterator(); iter.hasNext();) {
				String symbolicName = (String) iter.next();
				if (symbolicName.equals(allBundles[i].getSymbolicName())) {
					systemBundlesNames.add(symbolicName);
				}
			}
		}
      return (String[]) systemBundlesNames.toArray(new String[systemBundlesNames.size()]);
  }
  
  public long[] getSystemBundlesIDs() {
	Set symbolicNames = getSystemSymbolicNames();
    List systemBundles = new LinkedList();
    Bundle[] allBundles = bc.getBundles();
    for (int i = 0; i < allBundles.length; i++) {
      for (Iterator iter = symbolicNames.iterator(); iter.hasNext();) {
        String symbolicName = (String) iter.next();
        if (symbolicName.equals(allBundles[i].getSymbolicName())) {
          systemBundles.add(allBundles[i]);
        }
      }
    }
    return convertBundlesToIds((Bundle[]) systemBundles.toArray(new Bundle[systemBundles.size()]));
  }
  
  private Set getSystemSymbolicNames() {
    if(loadedSymbolicNames == null) {
        BufferedReader reader = null;
        try {
          File systemBundleFile = new File(System.getProperty(PROP_SYSTEM_BUNDLES_LIST, SYSTEM_BUNDLES_FILE_NAME));
          if (systemBundleFile.exists()) {
            try {
              reader = new BufferedReader(new FileReader(systemBundleFile));
            } catch (FileNotFoundException e) {
              // ignore and fall down
            }
          }
          if (reader == null) { // file not found, load default
            URL systemBundleURL = bc.getBundle().getResource(SYSTEM_BUNDLES_RESOURCE_NAME);
            if (systemBundleURL == null) {
              log("No system bundles list found.");
              loadedSymbolicNames = new HashSet(0);
            } else {
              reader = new BufferedReader(new InputStreamReader(systemBundleURL.openStream()));
            }
          }
          Set symbolicNames = new HashSet();
          String line;
          while ((line = reader.readLine()) != null) {
            symbolicNames.add(line);
          }
          this.loadedSymbolicNames = symbolicNames;
        } catch (IOException e) {
          log("Failed to load system buindles list from the bundle resources", e);
          loadedSymbolicNames = new HashSet(0);
        } finally {
          if (reader != null) {
            try {
              reader.close();
            } catch (IOException e) {
              // ignore
            }
          }
        }
      }
    return loadedSymbolicNames;
  }
}
