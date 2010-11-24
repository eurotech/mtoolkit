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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
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
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.rpc.Capabilities;
import org.tigris.mtoolkit.iagent.rpc.Remote;
import org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin;
import org.tigris.mtoolkit.iagent.rpc.RemoteCapabilitiesManager;
import org.tigris.mtoolkit.iagent.rpc.spi.BundleManagerDelegate;

public class RemoteBundleAdminImpl implements Remote, RemoteBundleAdmin, SynchronousBundleListener {

	public static final String SYNCH_BUNDLE_EVENTS = "synch_bundle_event";
	public static final String SYSTEM_BUNDLE_EVENT = "system_bundle_event";
	public static final String EVENT_TYPE_KEY = "type";
	public static final String EVENT_BUNDLE_ID_KEY = "bundle.id";

	private static final String PROP_SYSTEM_BUNDLES_LIST = "iagent.system.bundles.list";
	private static final String PROP_SYSTEM_BUNDLES = "iagent.system.bundles";
	private static final String SYSTEM_BUNDLES_FILE_NAME = "system_bundles.txt";
	private static final String SYSTEM_BUNDLES_RESOURCE_NAME = "/" + SYSTEM_BUNDLES_FILE_NAME;

	private ServiceTracker packageAdminTrack;
	private ServiceTracker startLevelTrack;
	private ServiceTracker delegatesTrack;
	private ServiceRegistration registration;
	private BundleContext bc;
	
	private Set loadedSymbolicNames;
	private Bundle systemBundle;
	
	private BundleManagerDelegate defaultDelegate;

	public Class[] remoteInterfaces() {
		return new Class[] { RemoteBundleAdmin.class };
	}

	public void register(BundleContext bc) {
		debug("[register] Registering remote Bundle Admin...");
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

		RemoteCapabilitiesManager capMan = Activator.getCapabilitiesManager();
		if (capMan != null) {
			capMan.setCapability(Capabilities.BUNDLE_SUPPORT, Boolean.TRUE);
		}

		debug("[register] Remote Bundle Admin Registered.");
	}

	public void unregister(BundleContext bc) {
		debug("[unregister] Unregistering remoteBundleAdmin...");
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

		RemoteCapabilitiesManager capMan = Activator.getCapabilitiesManager();
		if (capMan != null) {
			capMan.setCapability(Capabilities.BUNDLE_SUPPORT, new Boolean(false));
		}

		this.bc = null;
		debug("[unregister] Remote Bundle Admin unregistered.");
	}

	public int getBundleState(long id) {
		Bundle bundle = bc.getBundle(id);
		int bundleState = bundle != null ? bundle.getState() : Bundle.UNINSTALLED;
		debug("[getBundleState] id " + id + "; state: " + bundleState);
		return bundleState;
	}

	public String getBundleLocation(long id) {
		Bundle bundle = bc.getBundle(id);
		if (bundle != null) {
			String bundleLocation = bundle.getLocation();
			debug("[getBundleLocation] id: " + id + "; location: " + bundleLocation);
			return bundleLocation;
		} else {
			info("[getBundleLocation] id: " + id + " -> No such bundle");
			return null;
		}
	}

	public Dictionary getBundleHeaders(long id, String locale) {
		debug("[getBundleHeaders] >>> id: " + id + "; locale: " + locale);
		Bundle bundle = bc.getBundle(id);
		if (bundle == null) {
			info("[getBundleHeaders] No such bundle");
			return null;
		}
		Dictionary headers = bundle.getHeaders(locale);
		Dictionary converted = new Hashtable();
		for (Enumeration e = headers.keys(); e.hasMoreElements();) {
			Object key = e.nextElement();
			converted.put(key.toString(), headers.get(key).toString());
		}
		debug("[getBundleHeaders] headers: " + DebugUtils.convertForDebug(converted));
		return converted;
	}

	public Object getBundleHeader(long id, String headerName, String locale) {
		debug("[getBundleHeader] >>> id: " + id + "; headerName" + headerName + "; locale" + locale);
		Bundle bundle = bc.getBundle(id);
		if (bundle == null) {
			Error error = new Error(Error.BUNDLE_UNINSTALLED_CODE, "Bundle " + id + " has been uninstalled");
			info("[getBundleHeader] No such bundle: " + error);
			return error;
		}
		Dictionary headers = bundle.getHeaders(locale);
		Object value = headers.get(headerName);
		Object bundleHeader = value != null ? value.toString() : null;
		debug("[getBundleHeader] header value: " + bundleHeader);
		return bundleHeader;
	}

	public long getBundleLastModified(long id) {
		debug("[getBundleLastModified] >>> id: " + id);
		Bundle bundle = bc.getBundle(id);
		if (bundle == null) {
			info("[getBundleLastModified] No such bundle");
			return -2; // -1 value is often used to indicate unknown value, so
			// we return -2
			// it is hard for impl. to return -2 meaning that the bundle was
			// last modified 2 ms before 1 Jan, 1970:)
		}
		long bundleLastModified = bundle.getLastModified();
		debug("[getBundleLastModified] last modified: " + bundleLastModified);
		return bundleLastModified;
	}

	public String getBundleSymbolicName(long id) {
		debug("[getBundleSymbolicName] >>> id: " + id);
		Bundle bundle = bc.getBundle(id);
		if (bundle == null) {
			info("[getBundleSymbolicName] No such bundle");
			return null;
		}
		String symbolicName = bundle.getSymbolicName();
		symbolicName = symbolicName != null ? symbolicName : "";
		debug("[getBundleSymbolicName] symbolic name: " + symbolicName);
		return symbolicName;
	}

	public Object startBundle(long id, int flags) {
		debug("[startBundle] >>> id: " + id + "; flags" + flags);
		Bundle bundle = bc.getBundle(id);
		if (bundle == null) {
			Error error = new Error(Error.BUNDLE_UNINSTALLED_CODE, "Bundle " + id + " has been uninstalled");
			info("[startBundle] No such bundle");
			return error;
		}
		try {
			try {
				bundle.start(flags);
			} catch (NoSuchMethodError e) { // no OSGi R4.1 support
				bundle.start();
			}
		} catch (BundleException e) {
			Error error = new Error(IAgentErrors.ERROR_BUNDLE_UNKNOWN, "Failed to start bundle: " + e.getMessage());
			info("[startBundle] Bundle cannot be started: " + error, e);
			return error;
		} catch (IllegalStateException e) {
			Error error = new Error(Error.BUNDLE_UNINSTALLED_CODE, "Bundle " + id + " has been uninstalled");
			info("[startBundle] No such bundle: " + error);
			return error;
		} catch (Throwable t) {
			Error error = new Error(IAgentErrors.ERROR_BUNDLE_UNKNOWN, "Failed to start bundle: " + (t.getMessage() != null ? t.getMessage() : t.toString()));
			info("[startBundle] Bundle cannot be started: " + error, t);
			return error;
		}
		debug("[startBundle] Bundle started successfully");
		return null;
	}

	public Object stopBundle(long id, int flags) {
		debug("[stopBundle] id: " + id + "; flags" + flags);
		Bundle bundle = bc.getBundle(id);
		if (bundle == null) {
			Error error = new Error(Error.BUNDLE_UNINSTALLED_CODE, "Bundle " + id + " has been uninstalled");
			debug("[stopBundle] No such bundle: " + error);
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
			info("[stopBundle] Unable to stop bundle: " + error, e);
			return error;
		} catch (IllegalStateException e) {
			Error error = new Error(Error.BUNDLE_UNINSTALLED_CODE, null);
			info("[stopBundle] No such bundle: " + error);
			return error;
		} catch (Throwable t) {
			Error error = new Error(IAgentErrors.ERROR_BUNDLE_UNKNOWN, "Failed to stop bundle: " + (t.getMessage() != null ? t.getMessage() : t.toString()));
			info("[stopBundle] Unable to stop bundle: " + error, t);
			return error;
		}
		debug("[stopBundle] Successfully stopped");
		return null;
	}

	public boolean resolveBundles(long[] ids) {
		if (ids == null) {
			info("[resolveBundles] Passed bundle ids must not be null");
			throw new IllegalArgumentException("Passed bundle ids must be not null");
		}
		debug("[resolveBundles] bundles: " + DebugUtils.convertForDebug(ids));
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
			info("[resolveBundles] PackageAdmin service is not available!");
			throw new IllegalStateException("PackageAdmin is not available at the moment");
		}
		boolean areBundlesResolved = packageAdmin.resolveBundles(bs);
		debug("[resolveBundles] Bundles resolved successfully: " + areBundlesResolved);
		return areBundlesResolved;
	}

	public long[] listBundles() {
		debug("[listBundles] >>>");
		Bundle[] bundles = bc.getBundles();
		long[] bids = convertBundlesToIds(bundles);
		debug("[listBundles] bundles: " + DebugUtils.convertForDebug(bids));
		return bids;
	}

	public Object getBundlesSnapshot(int includeOptions, Dictionary properties) {
		debug("[getBundlesSnapshot] >>>");
		long[] ids = listBundles();
		List snapshots = new ArrayList();
		for (int i = 0; i < ids.length; i++) {
			Dictionary bundleInfo = new Hashtable();
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

	public Object installBundle(String location, InputStream is) {
		debug("[installBundle] location: " + location + "; inputStream: " + is);
		Object result = getDelegate().installBundle(location, is);
		if (result instanceof Error) {
			info("[installBundle] Unable to install bundle: " + result);
			return result;
		}
		
		Bundle bundle = (Bundle) result;
		Long bundleId = new Long(bundle.getBundleId());
		debug("[installBundle] Bundle installed successfully. Id: " + bundleId);
		return bundleId;
	}
	
	private BundleManagerDelegate getDelegate() {
		BundleManagerDelegate delegate = (BundleManagerDelegate) delegatesTrack.getService();
		if (delegate != null)
			return delegate;
		return defaultDelegate;
	}

	public Object uninstallBundle(long id) {
		debug("[uninstallBundle] id: " + id);
		Bundle bundle = bc.getBundle(id);
		if (bundle != null) {
			Object result = getDelegate().uninstallBundle(bundle);
			if (result instanceof Error) {
				info("[uninstallBundle] Unable to uninstall bundle: " + result);
				return result;
			}
			debug("[uninstallBundle] Bundle uninstalled");
			return result;
		} else {
			Error error = new Error(Error.BUNDLE_UNINSTALLED_CODE, "Bundle " + id + " has been uninstalled");
			info("[uninstallBundle] Unable to uninstall bundle: " + error);
			return error;
		}
	}

	public long[] getBundles(String symbolicName, String version) {
		debug("[getBundles] symbolicName: " + symbolicName + "; version: " + version);
		PackageAdmin admin = (PackageAdmin) packageAdminTrack.getService();
		if (admin == null) {
			throw new IllegalStateException("No PackageAdmin available");
		}
		Bundle[] bundles = admin.getBundles(symbolicName, version);
		long[] bids = convertBundlesToIds(bundles);
		debug("[getBundles] Bundles successfully gotten: " + DebugUtils.convertForDebug(bids));
		return bids;
	}

	public Object updateBundle(long id, InputStream is) {
		debug("[updateBundle] installBundle; id: " + id + "; inputStream: " + is);
		Bundle bundle = bc.getBundle(id);
		if (bundle == null) {
			Error error = new Error(Error.BUNDLE_UNINSTALLED_CODE, "Bundle " + id + " has been uninstalled");
			info("[updateBundle] No such bundle: " + error);
			return error;
		} else {
			Object result = getDelegate().updateBundle(bundle, is);
			if (result instanceof Error)
				info("[updateBundle] Unable to update bundle: " + result);
			else
				debug("[updateBundle] Bundle updated successfully");
			return result;
		}
	}

	public Dictionary[] getRegisteredServices(long id) {
		debug("[getRegisteredServices] id: " + id);
		Bundle bundle = bc.getBundle(id);
		if (bundle == null) {
			info("[getRegisteredServices] No such bundle");
			return null;
		}
		try {
			ServiceReference[] refs = bundle.getRegisteredServices();
			return RemoteServiceAdminImpl.convertReferences(refs);
		} catch (IllegalStateException e) {
			info("[getRegisteredServices] No such bundle");
			return null;
		}
	}

	public Dictionary[] getUsingServices(long id) {
		debug("[getUsingServices] id: " + id);
		Bundle bundle = bc.getBundle(id);
		if (bundle == null) {
			info("[getUsingServices] No such bundle");
			return null;
		}
		try {
			ServiceReference[] refs = bundle.getServicesInUse();
			Dictionary[] convertedReferences = RemoteServiceAdminImpl.convertReferences(refs);
			debug("[getUsingServices] Used services: " + DebugUtils.convertForDebug(convertedReferences));
			return convertedReferences;
		} catch (IllegalStateException e) {
			info("[getUsingServices] No such bundle");
			return null;
		}
	}

	public long[] getFragmentBundles(long id) {
		debug("[getFragmentBundles] id: " + id);
		Bundle bundle = bc.getBundle(id);
		if (bundle == null) {
			info("[getFragmentBundles] No such bundle");
			return null;
		}
		PackageAdmin admin = (PackageAdmin) packageAdminTrack.getService();
		if (admin == null) {
			info("[getFragmentBundles] No packageAdmin");
			return new long[0];
		}
		Bundle[] fragmentBundles = admin.getFragments(bundle);
		long[] bids = convertBundlesToIds(fragmentBundles);
		debug("[getFragmentBundles] Fragment bundles successfully gotten: " + DebugUtils.convertForDebug(bids));
		return bids;
	}

	public long[] getHostBundles(long id) {
		debug("[getHostBundles] id: " + id);
		Bundle bundle = bc.getBundle(id);
		if (bundle == null) {
			info("[getHostBundles] No such bundle");
			return null;
		}
		PackageAdmin admin = (PackageAdmin) packageAdminTrack.getService();
		if (admin == null) {
			info("[getHostBundles] No packageAdmin");
			return new long[0];
		}
		Bundle[] hostBundles = admin.getHosts(bundle);
		long[] bids = convertBundlesToIds(hostBundles);
		debug("[getHostBundles] Host bundles successfully gotten: " + DebugUtils.convertForDebug(bids));
		return bids;
	}

	public int getBundleType(long id) {
		debug("[getBundleType] id: " + id);
		Bundle bundle = bc.getBundle(id);
		if (bundle == null) {
			info("[getBundleType] No such bundle");
			return -1;
		}
		PackageAdmin admin = (PackageAdmin) packageAdminTrack.getService();
		if (admin == null) {
			info("[getBundleType] No packageAdmin");
			return -2;
		}
		int bundleType = admin.getBundleType(bundle);
		debug("[getBundleType] Bundle Type successfully gotten: " + bundleType);
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
			return; // don't send events when the framework shutting down
		if (bc.getBundle().getState() == Bundle.STOPPING)
			return; // stop sending events when the iagent bundle is stopping,
		// they're inspectation will lead to errors
		debug("[bundleChanged] Event type is BundleEvent." + event.getType());
			
		EventSynchronizer synchronizer = Activator.getSynchronizer();
		if (synchronizer != null) {
			Dictionary convEvent = convertBundleEvent(event);
			debug("[bundleChanged] Sending event through existing pmpConnection. eventType: " + event.getType());
			String symbolicName = event.getBundle().getSymbolicName();
			if (event.getType() == BundleEvent.INSTALLED && symbolicName != null && isBundleSystem(symbolicName)) {
				// post event if new bundle is installed whose symbolic name is
				// in the system bundles symbolic names
				synchronizer.enqueue(new EventData(new Long(event.getBundle().getBundleId()), SYSTEM_BUNDLE_EVENT));
			}
			synchronizer.enqueue(new EventData(convEvent, SYNCH_BUNDLE_EVENTS));
			debug("[bundleChanged] Bundle successfully changed");
		} else {
			info("[bundleChanged] Event synchronizer was disabled.");
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

	private final void debug(String message) {
		DebugUtils.debug(this, message);
	}

	private final void info(String message) {
		DebugUtils.info(this, message);
	}

	private final void info(String message, Throwable t) {
		DebugUtils.info(this, message, t);
	}

	private final void error(String message, Throwable e) {
		DebugUtils.error(this, message, e);
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
		String[] agentData = new String[] { Long.toString(agentBundle.getBundleId()), agentVersion };
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
		if (loadedSymbolicNames == null) {
			BufferedReader reader = null;
			try {
				String systemBundlesName = System.getProperty(PROP_SYSTEM_BUNDLES_LIST, SYSTEM_BUNDLES_FILE_NAME);
				File systemBundlesFile = new File(systemBundlesName);
				if (!systemBundlesFile.exists()) {
					File cwd = new File("").getAbsoluteFile();
					systemBundlesFile = new File(cwd.getParentFile(), systemBundlesName);
				}
				if (systemBundlesFile.exists()) {
					try {
						reader = new BufferedReader(new FileReader(systemBundlesFile));
					} catch (FileNotFoundException e) {
						// ignore and fall down
					}
				}
				if (reader == null) { // file not found, load default
					URL systemBundleURL = bc.getBundle().getResource(SYSTEM_BUNDLES_RESOURCE_NAME);
					if (systemBundleURL != null) {
						reader = new BufferedReader(new InputStreamReader(systemBundleURL.openStream()));
					}
				}
				if (reader == null) {
					if (System.getProperty(PROP_SYSTEM_BUNDLES) != null) {
						String sysBundles = System.getProperty(PROP_SYSTEM_BUNDLES);
						StringTokenizer token = new StringTokenizer(sysBundles, ",");
						Set bundleNames = new HashSet();
						while (token.hasMoreElements()) {
							String sysBundle = (String) token.nextElement();
							bundleNames.add(sysBundle);
						}
						this.loadedSymbolicNames = bundleNames;
						return loadedSymbolicNames;
					}
					this.loadedSymbolicNames = Collections.EMPTY_SET;
					return loadedSymbolicNames;
				}
				Set symbolicNames = new HashSet();
				String line;
				while ((line = reader.readLine()) != null) {
					symbolicNames.add(line);
				}
				this.loadedSymbolicNames = symbolicNames;
			} catch (IOException e) {
				error("Failed to load system buindles list from the bundle resources", e);
				loadedSymbolicNames = Collections.EMPTY_SET;
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

	public int getBundleStartLevel(long id) {
		Bundle bundle = bc.getBundle(id);
		StartLevel slService = (StartLevel) startLevelTrack.getService();
		if (slService != null)
			return slService.getBundleStartLevel(bundle);
		else
			return -1;
	}

	public int getFrameworkStartLevel() {
		StartLevel slService = (StartLevel) startLevelTrack.getService();
		if (slService != null)
			return slService.getStartLevel();
		else
			return -1;
	}
	
	public String getSystemProperty(String name) {
		return System.getProperty(name);
	}
}
