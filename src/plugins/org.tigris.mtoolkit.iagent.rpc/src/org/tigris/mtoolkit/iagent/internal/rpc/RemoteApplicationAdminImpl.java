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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.application.ApplicationException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.tigris.mtoolkit.iagent.Error;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.event.EventData;
import org.tigris.mtoolkit.iagent.event.EventSynchronizer;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.internal.utils.ExceptionCodeHelper;
import org.tigris.mtoolkit.iagent.rpc.Capabilities;
import org.tigris.mtoolkit.iagent.rpc.Remote;
import org.tigris.mtoolkit.iagent.rpc.RemoteApplicationAdmin;
import org.tigris.mtoolkit.iagent.rpc.RemoteCapabilitiesManager;

public class RemoteApplicationAdminImpl implements Remote, RemoteApplicationAdmin, ServiceTrackerCustomizer {

	private static final String OSGI_APPLICATION_PACKAGE = "org.osgi.service.application.";
	private static final String APPLICATION_DESCRIPTOR = OSGI_APPLICATION_PACKAGE + "ApplicationDescriptor";
	private static final String APPLICATION_HANDLE = OSGI_APPLICATION_PACKAGE + "ApplicationHandle";

	public static final String SYNCH_APPLICATION_EVENT = "synch_application_event";
	private static final String EVENT_TYPE_KEY = "type";
	private static final String EVENT_APPLICATION_ID_KEY = "application_id";

	private static final int APP_INSTALLED = 1 << 0;
	private static final int APP_UNINSTALLED = 1 << 4;
	private static final int APP_STARTED = 1 << 2;
	private static final int APP_STOPPED = 1 << 3;

	private static final String UNINSTALLED_STATE = "UNINSTALLED";
	private static final String INSTALLED_STATE = "INSTALLED";
	private static final String MIXED_STATE = "MIXED";
	private static final String ERROR_STATE = "ERROR";
	private static final String UNKNOWN_STATE = "UNKNOWN";

	private ServiceRegistration registration;
	private BundleContext bc;
	private ServiceTracker applicationTracker;
	private ServiceTracker handlesTracker;
	private boolean suppressEventFiring = false;

	public Class[] remoteInterfaces() {
		return new Class[] { RemoteApplicationAdmin.class };
	}

	public void register(BundleContext bundleContext) {
		this.bc = bundleContext;

		suppressEventFiring = true;
		try {
			applicationTracker = new ServiceTracker(bc, APPLICATION_DESCRIPTOR, this);
			applicationTracker.open(true);

			handlesTracker = new ServiceTracker(bc, APPLICATION_HANDLE, this);
			handlesTracker.open(true);
		} finally {
			suppressEventFiring = false;
		}

		registration = bc.registerService(RemoteApplicationAdmin.class.getName(), this, null);

		RemoteCapabilitiesManager capMan = Activator.getCapabilitiesManager();
		if (capMan != null) {
			capMan.setCapability(Capabilities.APPLICATION_SUPPORT, new Boolean(true));
		}
	}

	public void unregister(BundleContext bc) {
		if (registration != null) {
			registration.unregister();
			registration = null;
		}

		if (applicationTracker != null) {
			applicationTracker.close();
			applicationTracker = null;
		}
		if (handlesTracker != null) {
			handlesTracker.close();
			handlesTracker = null;
		}

		RemoteCapabilitiesManager capMan = Activator.getCapabilitiesManager();
		if (capMan != null) {
			capMan.setCapability(Capabilities.APPLICATION_SUPPORT, new Boolean(false));
		}

		this.bc = null;
	}

	private void fireApplicationEvent(String id, int type) {
		Dictionary event = convertApplicationEvent(id, type);
		EventSynchronizer synchronizer = Activator.getSynchronizer();
		if (synchronizer != null)
			synchronizer.enqueue(new EventData(event, SYNCH_APPLICATION_EVENT));
	}

	public String[] getApplications() {
		ServiceReference[] refs = applicationTracker.getServiceReferences();
		if (refs == null)
			return new String[0];
		debug("[getApplications] " + refs.length + " applications available.");
		String[] ids = new String[refs.length];
		for (int i = 0; i < refs.length; i++) {
			ids[i] = (String) refs[i].getProperty(Constants.SERVICE_PID);
		}
		debug("[getApplications] Application ids: " + DebugUtils.convertForDebug(ids));
		return ids;
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

	private final void debug(String message) {
		DebugUtils.debug(this, message);
	}

	private final void error(String message, Throwable e) {
		DebugUtils.error(this, message, e);
	}

	public Object start(String applicationID, Map properties) {
		Object descriptor = findApplicationDescriptor(applicationID);
		if (descriptor == null)
			return new Error(IAgentErrors.ERROR_APPLICATION_UNINSTALLED, "Application with ID \"" + applicationID + "\" is not installed.");
		Object result = launchFromDescriptor(descriptor, properties);
		return result;
	}

	public Object stop(String applicationID) {
		Object[] handles = handlesTracker.getServices();
		if (handles == null)
			return null;
		for (int i = 0; i < handles.length; i++) {
			String id = getApplicationIdFromHandle(handles[i]);
			if (id != null && id.equals(applicationID)) {
				Object result = destroyHandle(handles[i]);
				if (result != null)
					return result;
			}
		}
		return null;
	}

	private Object launchFromDescriptor(Object descriptor, Map properties) {
		try {
			invokeMethod1(descriptor, "launch", Map.class, properties);
			return null;
		} catch (Exception e) {
			if (e instanceof ApplicationException) {
				return new Error(ExceptionCodeHelper.fromApplicationExceptionCode(((ApplicationException) e)
						.getErrorCode()), "Application start failed: " + DebugUtils.toString(e));
			} else {
				return new Error(IAgentErrors.ERROR_APPLICATION_UNKNOWN, "Application start failed: "
						+ DebugUtils.toString(e));
			}
		}
	}

	private Object destroyHandle(Object handle) {
		try {
			invokeMethod0(handle, "destroy");
			return null;
		} catch (Exception e) {
			return new Error(IAgentErrors.ERROR_APPLICATION_UNKNOWN, "Application stop failed: "
					+ DebugUtils.toString(e));
		}
	}

	private Object findApplicationDescriptor(String applicationID) {
		ServiceReference ref = findDescriptorReference(applicationID);
		if (ref != null)
			return applicationTracker.getService(ref);
		return null;
	}

	private ServiceReference findDescriptorReference(String applicationId) {
		ServiceReference[] refs = applicationTracker.getServiceReferences();
		if (refs == null)
			return null;
		for (int i = 0; i < refs.length; i++) {
			String id = getApplicationIdFromReference(refs[i]);
			if (applicationId.equals(id))
				return refs[i];
		}
		return null;
	}

	public String getState(String applicationId) {
		Object descriptor = findApplicationDescriptor(applicationId);
		if (descriptor == null)
			return UNINSTALLED_STATE;
		Object[] handles = findHandles(applicationId);
		if (handles.length == 0)
			return INSTALLED_STATE;
		if (handles.length == 1)
			return getHandleState(handles[0]);
		if (handles.length > 1)
			return MIXED_STATE;
		return UNKNOWN_STATE;
	}

	private String getHandleState(Object handle) {
		try {
			return (String) invokeMethod0(handle, "getState");
		} catch (Exception e) {
			error("Failed to get application state", e);
			return ERROR_STATE;
		}
	}

	private Object[] findHandles(String applicationId) {
		Object[] handles = handlesTracker.getServices();
		if (handles == null)
			return new Object[0];
		List filtered = new ArrayList(handles.length);
		for (int i = 0; i < handles.length; i++) {
			String id = getApplicationIdFromHandle(handles[i]);
			if (id != null && id.equals(applicationId)) {
				filtered.add(handles[i]);
			}
		}
		return filtered.toArray();
	}

	private Dictionary convertApplicationEvent(String applicationId, int type) {
		Dictionary event = new Hashtable();
		event.put(EVENT_TYPE_KEY, new Integer(type));
		event.put(EVENT_APPLICATION_ID_KEY, applicationId);
		return event;
	}

	private String getApplicationIdFromReference(ServiceReference ref) {
		return (String) ref.getProperty(Constants.SERVICE_PID);
	}

	private String getApplicationIdFromHandle(Object obj) {
		try {
			Object descriptor = invokeMethod0(obj, "getApplicationDescriptor");
			return getApplicationIdFromDescriptor(descriptor);
		} catch (Exception e) {
			error("Failed to get application descriptor from " + obj, e);
			return null;
		}
	}

	private String getApplicationIdFromDescriptor(Object obj) {
		try {
			return (String) invokeMethod0(obj, "getApplicationId");
		} catch (Exception e) {
			error("Failed to get application id from " + obj, e);
			return null;
		}
	}

	private Object invokeMethod0(Object obj, String method) throws Exception {
		return invokeMethodn(obj, method, null, null);
	}

	private Object invokeMethod1(Object obj, String method, Class paramType, Object param) throws Exception {
		return invokeMethodn(obj, method, new Class[] { paramType }, new Object[] { param });
	}

	private Object invokeMethodn(Object obj, String method, Class[] paramTypes, Object[] param) throws Exception {
		Class clazz = obj.getClass();
		try {
			Method m = clazz.getMethod(method, paramTypes);
			return m.invoke(obj, param);
		} catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof Exception)
				throw (Exception) e.getTargetException();
			throw e;
		}
	}

	private boolean testObjectClass(Object value, String className) {
		if (value instanceof String[]) {
			String[] classes = (String[]) value;
			for (int i = 0; i < classes.length; i++) {
				if (classes[i].equals(className))
					return true;
			}
			return false;
		} else {
			return className.equals(value);
		}
	}

	public Object addingService(ServiceReference reference) {
		Object service = bc.getService(reference);
		if (!suppressEventFiring) {
			if (testObjectClass(reference.getProperty(Constants.OBJECTCLASS), APPLICATION_DESCRIPTOR)) {
				String id = getApplicationIdFromDescriptor(service);
				if (id != null)
					fireApplicationEvent(id, APP_INSTALLED);
			} else if (testObjectClass(reference.getProperty(Constants.OBJECTCLASS), APPLICATION_HANDLE)) {
				String id = getApplicationIdFromHandle(service);
				if (id != null)
					fireApplicationEvent(id, APP_STARTED);
			}
		}
		return service;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		// do nothing
	}

	public void removedService(ServiceReference reference, Object service) {
		bc.ungetService(reference);
		if (!suppressEventFiring) {
			if (testObjectClass(reference.getProperty(Constants.OBJECTCLASS), APPLICATION_DESCRIPTOR)) {
				String id = getApplicationIdFromDescriptor(service);
				if (id != null)
					fireApplicationEvent(id, APP_UNINSTALLED);
			} else if (testObjectClass(reference.getProperty(Constants.OBJECTCLASS), APPLICATION_HANDLE)) {
				String id = getApplicationIdFromHandle(service);
				if (id != null)
					fireApplicationEvent(id, APP_STOPPED);
			}
		}
	}

	public Object getProperties(String applicationId) {
		Object descriptor = findApplicationDescriptor(applicationId);
		if (descriptor == null) {
			return new Error(IAgentErrors.ERROR_APPLICATION_UNINSTALLED, "Application has been uninstalled");
		}
		try {
			Object result = invokeMethod1(descriptor, "getProperties", String.class, null);
			if (result instanceof Map) {
				Map map = convertProperties((Map) result);
				ServiceReference sr = findDescriptorReference(applicationId);
				if (sr != null) {
					map.putAll(getReferenceProperties(sr));
				}
				return map;
			}
		} catch (Exception e) {
			return new Error(IAgentErrors.ERROR_APPLICATION_UNKNOWN, "Cannot get properties: "
					+ DebugUtils.toString(e));
		}
		return new Hashtable();
	}

	private Map getReferenceProperties(ServiceReference ref) {
		Map props = new Hashtable();
		String[] keys = ref.getPropertyKeys();
		for (int i = 0; i < keys.length; i++) {
			Object value = ref.getProperty(keys[i]);
			if (value != null)
				props.put(keys[i], convertObject(ref.getProperty(keys[i])));
		}
		return props;
	}

	private Map convertProperties(Map props) {
		Map result = new Hashtable();
		Set keys = props.keySet();
		Iterator iterator = keys.iterator();
		while (iterator.hasNext()) {
			Object key = iterator.next();
			Object value = props.get(key);
			if (value != null) {
				result.put(key, convertObject(value));
			}
		}
		return result;
	}

	private Object convertObject(Object obj) {
		if (obj instanceof URL) {
			/*
			 * XXX: Special cases for URLs, because we cannot guarantee that the
			 * remote side has the same scheme handlers. The other option would
			 * be to limit the transferable types, but the possible types are a
			 * lot, so we do it only for the types we have problems with.
			 */
			return obj.toString();
		} else if (obj instanceof URL[]) {
			URL[] urls = (URL[]) obj;
			String[] result = new String[urls.length];
			for (int i = 0; i < result.length; i++)
				result[i] = urls[i].toString();
			return result;
		} else {
			return obj;
		}
	}

}
