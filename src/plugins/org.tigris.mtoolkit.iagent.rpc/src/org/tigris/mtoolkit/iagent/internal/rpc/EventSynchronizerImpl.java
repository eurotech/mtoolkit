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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.tigris.mtoolkit.iagent.event.EventData;
import org.tigris.mtoolkit.iagent.event.EventSynchronizer;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.pmp.PMPServer;
import org.tigris.mtoolkit.iagent.rpc.Remote;
import org.tigris.mtoolkit.iagent.rpc.RemoteServiceAdmin;

public class EventSynchronizerImpl extends Thread implements EventSynchronizer, Remote {

	private List eventQueue = new LinkedList();
	private List eventTypes;
	private volatile boolean running = true;
	private PMPServer server;
	private ServiceRegistration registration;
	private BundleContext bc;
	
	public Class[] remoteInterfaces() {
        return new Class[] { EventSynchronizer.class };
    }

	EventSynchronizerImpl(BundleContext bc) {
		super("IAgent RPC Event Thread");
		this.bc = bc;
		setDaemon(true);
		
		//didi export service
		registration = bc.registerService(EventSynchronizer.class.getName(), this, null);
		
	}

	void setPMPServer(PMPServer server) {
		this.server = server;
		addEventSource(RemoteBundleAdminImpl.SYNCH_BUNDLE_EVENTS);
		addEventSource(RemoteBundleAdminImpl.SYSTEM_BUNDLE_EVENT);
		addEventSource(RemoteServiceAdmin.CUSTOM_SERVICE_EVENT);
		addEventSource(Activator.CUSTOM_PROPERTY_EVENT);
		addEventSource(RemoteDeploymentAdminImpl.DEPLOYMENT_EVENT);
	}
	
	public void addEventSource(String eventType) {
		log("[addEventSource] >>> eventType: " + eventType);
		if (eventTypes == null) {
			eventTypes = new ArrayList();
		}
		if (!eventTypes.contains(eventType))
			eventTypes.add(eventType);
		server.addEventSource(eventType);
	}
	
	public void removeEventSource(String eventType) {
		log("[removeEventSource] >>> eventType: " + eventType);
		server.removeEventSource(eventType);
		if (eventTypes != null)
			eventTypes.remove(eventType);
	}

	public void run() {
		try {
			while (running) {
				EventData eventData = null;
				synchronized (this) {
					try {
						while (eventQueue.isEmpty() && running) {
							log("[run] event queue is empty >> thread will wait");
							wait();
						}
					} catch (InterruptedException e) {
						running = false;
						return;
					}
					if (!running)
						return;
					eventData = (EventData) eventQueue.remove(0);
				}
				Object convEvent = eventData.getConvertedEvent();
				String eventType = eventData.getEventType();
				log("[run] sending event: " + eventData);
				server.event(convEvent, eventType);
			}
		} finally {
			unregister(bc);
		}
	}

	public void enqueue(EventData eventData) {
		log("[enqueue] >>> eventData: " + eventData);
		if (!running) {
			log("[enqueue] Not running anymore. Skipping...");
			return;
		}
		synchronized (this) {
			eventQueue.add(eventData);
			notify();
		}
	}

	public synchronized void stopDispatching() {
		running = false;
		notifyAll();
	}
	
	public void unregister(BundleContext bc) {
		log("[unregister] Unregistering EventSynchronizer...");
		
		if (eventTypes != null) {
			for (int i = 0; i < eventTypes.size(); i++) {
				server.removeEventSource((String) eventTypes.get(i));
			}
			eventTypes = null;
		}
		
		if (registration != null) {
			registration.unregister();
			registration = null;
		}
		this.bc = null;
		log("[unregister] EventSynchronizer unregistered.");
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
		DebugUtils.log(EventSynchronizerImpl.class, message, e);
	}
}
