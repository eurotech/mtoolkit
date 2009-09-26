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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.application.ApplicationDescriptor;
import org.osgi.service.application.ApplicationException;
import org.osgi.service.application.ApplicationHandle;
import org.osgi.util.tracker.ServiceTracker;
import org.tigris.mtoolkit.iagent.event.EventData;
import org.tigris.mtoolkit.iagent.event.EventSynchronizer;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.rpc.Remote;
import org.tigris.mtoolkit.iagent.rpc.RemoteApplicationAdmin;

public class RemoteApplicationAdminImpl implements Remote, RemoteApplicationAdmin, ServiceListener {

	private static final int REGISTER_EVENT_TOPIC = 1;
	private static final int UNREGISTER_EVENT_TOPIC = 4;
	public static final String SYNCH_APPLICATION_EVENT = "synch_application_event";
	private static final String EVENT_TYPE_KEY = "type";
	private static final String EVENT_APPLICATION_ID_KEY = "application_id";

	private static final int APP_INSTALLED = 1 << 0;
	private static final int APP_UNINSTALLED = 1 << 4;
	private static final int APP_STARTED = 1 << 2;
	private static final int APP_STOPPED = 1 << 3;

	private ServiceRegistration registration;
	private BundleContext bc;
	private ServiceTracker eventAdminTrack;
	private Map applicationMap;

	public Class[] remoteInterfaces() {
		return new Class[] { RemoteApplicationAdmin.class };
	}

	public void register(BundleContext bundleContext) {
		this.bc = bundleContext;

		Activator.getSynchronizer().addEventSource(SYNCH_APPLICATION_EVENT);

		registration = bc.registerService(RemoteApplicationAdmin.class.getName(), this, null);

		applicationMap = new HashMap();
		bc.addServiceListener(this);
	}

	public void unregister(BundleContext bc) {
		if (registration != null) {
			registration.unregister();
			registration = null;
		}
		if (eventAdminTrack != null) {
			eventAdminTrack.close();
			eventAdminTrack = null;
		}

		Activator.getSynchronizer().removeEventSource(SYNCH_APPLICATION_EVENT);

		this.bc.removeServiceListener(this);
		this.bc = null;
	}

	public void serviceChanged(ServiceEvent event) {
		EventSynchronizer synchronizer = Activator.getSynchronizer();
		synchronized (synchronizer) {
			Dictionary myEvent = null;
			int eventType = event.getType();
			ServiceReference ref = event.getServiceReference();
			Object service = bc.getService(ref);
			if (service instanceof ApplicationHandle) {
				ApplicationHandle handle = (ApplicationHandle) service;
				if (eventType == REGISTER_EVENT_TOPIC) {
					myEvent = convertApplicationEvent(handle.getInstanceId(), APP_STARTED);
				}
				if (eventType == UNREGISTER_EVENT_TOPIC) {
					myEvent = convertApplicationEvent(handle.getInstanceId(), APP_STOPPED);
				}
			}
			if (service instanceof ApplicationDescriptor) {
				ApplicationDescriptor desc = (ApplicationDescriptor) service;
				if (eventType == REGISTER_EVENT_TOPIC) {
					myEvent = convertApplicationEvent(desc.getApplicationId(), APP_INSTALLED);
				} else if (eventType == UNREGISTER_EVENT_TOPIC) {
					myEvent = convertApplicationEvent(desc.getApplicationId(), APP_UNINSTALLED);
				}
			}
			if (myEvent != null) {
				synchronizer.enqueue(new EventData(myEvent, SYNCH_APPLICATION_EVENT));
			}
		}
	}

	public String[] getApplications() {
		ServiceReference[] serviceRefs = null;
		List result = new ArrayList();
		try {
			serviceRefs = bc.getServiceReferences(ApplicationDescriptor.class.getName(), null);
		} catch (InvalidSyntaxException e) {
			// cannot happen
			log("[getApplications] ", e);
		}
		if (serviceRefs != null) {
			for (int i = 0; i < serviceRefs.length; i++) {
				ApplicationDescriptor desc = (ApplicationDescriptor) bc.getService(serviceRefs[i]);
				if (desc != null) {
					result.add(desc.getApplicationId());
				}
			}
		}
		log("[getApplications] result: " + result);
		return (String[]) result.toArray(new String[result.size()]);
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

	private static final void log(String message) {
		log(message, null);
	}

	private static final void log(String message, Throwable e) {
		DebugUtils.log(RemoteApplicationAdmin.class, message, e);
	}

	public void start(String applicationID, Map properties) throws ApplicationException {
		ApplicationDescriptor appDescriptor = findApplicationDescriptor(applicationID);
		if (appDescriptor == null) {
			throw new ApplicationException(ApplicationException.APPLICATION_INTERNAL_ERROR,
					"Unrecognized application ID: " + applicationID);
		}
		ApplicationHandle applicationHandle = appDescriptor.launch(properties);
		applicationMap.put(applicationID, applicationHandle);
	}

	public void stop(String applicationID) {
		ApplicationHandle handle = (ApplicationHandle) applicationMap.get(applicationID);
		if (handle == null) {
			return;
		}
		handle.destroy();
		applicationMap.remove(applicationID);
	}

	private ApplicationDescriptor findApplicationDescriptor(String applicationID) {
		ServiceReference[] serviceRefs = null;
		try {
			serviceRefs = bc.getServiceReferences(ApplicationDescriptor.class.getName(), null);
		} catch (InvalidSyntaxException e) {
			// cannot happen
			log("[findApplicationDescriptor] ", e);
		}
		if (serviceRefs != null) {
			for (int i = 0; i < serviceRefs.length; i++) {
				ApplicationDescriptor desc = (ApplicationDescriptor) bc.getService(serviceRefs[i]);
				if (desc != null && desc.getApplicationId().equals(applicationID)) {
					return desc;
				}
			}
		}

		return null;
	}

	public String getState(String applicationId) {
		ApplicationHandle handle = (ApplicationHandle) applicationMap.get(applicationId);
		if (handle != null)
			return handle.getState();
		return "UNINSTALLED";
	}

	private Dictionary convertApplicationEvent(String applicationId, int type) {
		Dictionary event = new Hashtable();
		event.put(EVENT_TYPE_KEY, new Integer(type));
		event.put(EVENT_APPLICATION_ID_KEY, applicationId);
		return event;
	}
}
