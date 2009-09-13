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

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.osgi.framework.AllServiceListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.tigris.mtoolkit.iagent.event.EventData;
import org.tigris.mtoolkit.iagent.event.EventSynchronizer;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.rpc.Remote;
import org.tigris.mtoolkit.iagent.rpc.RemoteServiceAdmin;

public class RemoteServiceAdminImpl implements RemoteServiceAdmin, Remote, AllServiceListener {

	private static final String EVENT_TYPE_KEY = "type";
	private static final int SERVICE_REGISTERED = 1 << 0;
	private static final int SERVICE_MODIFIED = 1 << 1;
	private static final int SERVICE_UNREGISTERED = 1 << 2;

	private static final boolean TRACK_SERVICES_DEBUG = Boolean.getBoolean("iagent.debug.services");

	private Bundle systemBundle;

	private Class[] filterSupportedClasses = new Class[] { int.class,
		long.class,
		float.class,
		double.class,
		byte.class,
		short.class,
		char.class,
		boolean.class,
		Integer.class,
		Long.class,
		Float.class,
		Double.class,
		Byte.class,
		Short.class,
		Character.class,
		Boolean.class,
		String.class };

	private BundleContext bc;
	private ServiceRegistration registration;

	private Map services = new Hashtable();

	public void register(BundleContext context) {
		log("[register] Registering remote service admin...");
		this.bc = context;

		registration = context.registerService(RemoteServiceAdmin.class.getName(), this, null);

		synchronized (services) {
			context.addServiceListener(this);
			try {
				ServiceReference[] refs = context.getAllServiceReferences(null, null);
				fillServicesMap(refs);
			} catch (InvalidSyntaxException e) {
				// ignore
			}
		}
		
		Activator.getSynchronizer().addEventSource(CUSTOM_SERVICE_EVENT);
		
		log("[register] Remote Service Admin Registered.");
	}

	private void fillServicesMap(ServiceReference[] references) {
		if (references == null || references.length == 0)
			return;
		for (int i = 0; i < references.length; i++) {
			ServiceReference ref = references[i];
			Long sid = (Long) ref.getProperty(Constants.SERVICE_ID);
			services.put(sid, ref);
		}
	}

	public void unregister(BundleContext context) {
		log("[unregister] Unregistering...");
		context.removeServiceListener(this);
		services = null;
		if (registration != null) {
			registration.unregister();
			registration = null;
		}
		
		Activator.getSynchronizer().removeEventSource(CUSTOM_SERVICE_EVENT);
		
		this.bc = null;

		log("[unregister] Unregistered.");
	}

	public String checkFilter(String filter) {
		log("[checkFilter] >>> filter: " + filter);
		try {
			bc.createFilter(filter);
			log("[checkFilter] Filter check is successful");
			return null;
		} catch (InvalidSyntaxException e) {
			log("[checkFilter] Unable to create filter", e);
			return e.toString();
		}
	}

	public Dictionary[] getAllRemoteServices(String clazz, String filter) {
		if (DebugUtils.DEBUG)
			log("[getAllRemoteServices] >>> clazz: " + clazz + "; filter: " + filter);
		ServiceReference[] refs;
		try {
			refs = bc.getAllServiceReferences(clazz, filter);
		} catch (InvalidSyntaxException e) {
			return null;
		}
		if (DebugUtils.DEBUG)
			log("[getAllRemoteServices] " + (refs != null ? refs.length : 0) + " services found.");
		return convertReferences(refs);
	}

	private ServiceReference getServiceReference(long id) {
		ServiceReference ref = (ServiceReference) services.get(new Long(id));
		return ref;
	}

	public Class[] remoteInterfaces() {
		return new Class[] { RemoteServiceAdmin.class };
	}

	public void addService(ServiceReference ref) {
		Long serviceId = (Long) ref.getProperty(Constants.SERVICE_ID);
		if (TRACK_SERVICES_DEBUG)
			log("[addService] Track service: " + ref + "; id: " + serviceId);
		services.put(serviceId, ref);
	}

	public void removeService(ServiceReference ref) {
		Long serviceId = (Long) ref.getProperty(Constants.SERVICE_ID);
		if (TRACK_SERVICES_DEBUG)
			log("[addService] Stop tracking service: " + ref + "; id: " + serviceId);
		// XXX: Unnecessary synchronization?
		synchronized (services) {
			services.remove(serviceId);
		}
	}

	public void serviceChanged(ServiceEvent event) {
		switch (event.getType()) {
		case ServiceEvent.REGISTERED: {
			addService(event.getServiceReference());
			break;
		}
		case ServiceEvent.UNREGISTERING: {
			removeService(event.getServiceReference());
			break;
		}
		}
		postRemoteEvent(event);
	}

	private void postRemoteEvent(ServiceEvent event) {
		if (systemBundle == null)
			systemBundle = bc.getBundle(0);
		if (systemBundle.getState() == Bundle.STOPPING)
			return;
		if (bc.getBundle().getState() == Bundle.STOPPING)
			return;
		EventSynchronizer synchronizer = Activator.getSynchronizer();
		if (synchronizer != null) {
			Dictionary convertedServiceEvent = convertServiceEvent(event);
			log("[postRemoteEvent] Posting remote event: "
							+ DebugUtils.convertForDebug(convertedServiceEvent)
							+ "; type: "
							+ RemoteServiceAdmin.CUSTOM_SERVICE_EVENT);
			synchronizer.enqueue(new EventData(convertedServiceEvent, RemoteServiceAdmin.CUSTOM_SERVICE_EVENT));
		} else {
			log("[postRemoteEvent] Event synchronizer was disabled");
		}
	}

	private Dictionary convertServiceEvent(ServiceEvent event) {
		Dictionary props = new Hashtable();
		switch (event.getType()) {
		case ServiceEvent.REGISTERED:
			props.put(EVENT_TYPE_KEY, new Integer(SERVICE_REGISTERED));
			break;
		case ServiceEvent.MODIFIED:
			props.put(EVENT_TYPE_KEY, new Integer(SERVICE_MODIFIED));
			break;
		case ServiceEvent.UNREGISTERING:
			props.put(EVENT_TYPE_KEY, new Integer(SERVICE_UNREGISTERED));
			break;
		}

		props.put(Constants.SERVICE_ID, event.getServiceReference().getProperty(Constants.SERVICE_ID));
		props.put(Constants.OBJECTCLASS, event.getServiceReference().getProperty(Constants.OBJECTCLASS));
		return props;
	}


	public long getBundle(long id) {
		if (DebugUtils.DEBUG)
			log("[getBundle] >>> id: " + id);
		ServiceReference ref = getServiceReference(id);
		if (ref == null) {
			if (DebugUtils.DEBUG)
				log("[getBundle] No such service");
			return -1;
		}
		long bundleID = ref.getBundle().getBundleId();
		if (DebugUtils.DEBUG)
			log("[getBundle] bundle id: " + bundleID);
		return bundleID;
	}

	public Dictionary getProperties(long id) {
		if (DebugUtils.DEBUG)
			log("[getProperties] >>> id: " + id);

		ServiceReference ref = getServiceReference(id);
		if (ref == null) {
			log("[getProperties] No such service");
			return null;
		}

		Dictionary props = new Hashtable();
		String[] keys = ref.getPropertyKeys();
		for (int i = 0; i < keys.length; i++) {
			Object prop = ref.getProperty(keys[i]);
			props.put(keys[i], convertProperty(prop));
		}

		if (DebugUtils.DEBUG)
			log("[getProperties] service properties: " + DebugUtils.convertForDebug(props));

		return props;
	}

	private Object convertProperty(Object value) {
		if (value == null) {
			return null;
		} else if (isFilterSupportedClass(value.getClass())) {
			return value;
		} else if (value.getClass().isArray()) {
			Class componentType = value.getClass().getComponentType();
			if (isFilterSupportedClass(componentType)) {
				return value;
			} else {
				return convertCollection(value);
			}
		} else if (value instanceof Collection) {
			boolean supported = true;
			for (Iterator it = ((Collection) value).iterator(); it.hasNext();) {
				Object next = it.next();
				if (!isFilterSupportedClass(next.getClass())) {
					supported = false;
					break;
				}
			}
			if (supported) {
				return value;
			} else {
				return convertCollection(value);
			}
		} else {
			return value.toString();
		}
	}

	private String[] convertCollection(Object convertible) {
		String[] result;
		if (convertible.getClass().isArray()) {
			Object[] array = (Object[]) convertible;
			result = new String[array.length];
			for (int i = 0; i < array.length; i++)
				result[i] = (array[i] != null ? array[i].toString() : null);
		} else if (convertible instanceof Collection) {
			Collection c = (Collection) convertible;
			result = new String[c.size()];
			int i = 0;
			for (Iterator it = c.iterator(); it.hasNext() && i < result.length; i++) {
				Object next = (Object) it.next();
				result[i] = (next != null ? next.toString() : null);
			}
		} else {
			throw new IllegalArgumentException("The passsed convertible is neither array or Collection");
		}
		return result;
	}

	private boolean isFilterSupportedClass(Class clazz) {
		for (int i = 0; i < filterSupportedClasses.length; i++) {
			if (filterSupportedClasses[i].equals(clazz)) {
				return true;
			}
		}
		return false;
	}

	public long[] getUsingBundles(long id) {
		if (DebugUtils.DEBUG)
			log("[getUsingBundles] >>> id: " + id);
		ServiceReference ref = getServiceReference(id);
		if (ref == null) {
			if (DebugUtils.DEBUG)
				log("[getUsingBundles] No such service");
			return null;
		}
		Bundle[] bundles = ref.getUsingBundles();
		long[] bids = RemoteBundleAdminImpl.convertBundlesToIds(bundles);
		if (DebugUtils.DEBUG)
			log("[getUsingBundles] bundles: " + DebugUtils.convertForDebug(bids));
		return bids;
	}

	public boolean isServiceStale(long id) {
		boolean stale = services.get(new Long(id)) == null;
		if (DebugUtils.DEBUG)
			log("[isServiceStale] id: " + id + "; stale: " + stale);
		return stale;
	}

	static Dictionary[] convertReferences(ServiceReference[] refs) {
		if (refs == null)
			return new Dictionary[0];
		Dictionary[] refsProps = new Dictionary[refs.length];
		for (int i = 0; i < refs.length; i++) {
			refsProps[i] = new Hashtable();
			refsProps[i].put(Constants.SERVICE_ID, refs[i].getProperty(Constants.SERVICE_ID));
			refsProps[i].put(Constants.OBJECTCLASS, refs[i].getProperty(Constants.OBJECTCLASS));
		}
		return refsProps;
	}

	private static final void log(String message) {
		log(message, null);
	}

	private static final void log(String message, Throwable e) {
		DebugUtils.log(RemoteServiceAdminImpl.class, message, e);
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
}
