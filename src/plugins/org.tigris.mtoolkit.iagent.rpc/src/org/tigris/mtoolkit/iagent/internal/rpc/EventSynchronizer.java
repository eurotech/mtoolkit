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

import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.pmp.PMPServer;
import org.tigris.mtoolkit.iagent.rpc.RemoteServiceAdmin;

public class EventSynchronizer extends Thread {

	private List eventQueue = new LinkedList();
	private volatile boolean running = true;
	private PMPServer server;

	EventSynchronizer(BundleContext bc) {
		super("IAgent RPC Event Thread");
		setDaemon(true);
	}
	
	void setPMPServer(PMPServer server) {
		this.server = server;
		server.addEventSource(RemoteBundleAdminImpl.SYNCH_BUNDLE_EVENTS);
		server.addEventSource(RemoteBundleAdminImpl.SYSTEM_BUNDLE_EVENT);
		server.addEventSource(RemoteServiceAdmin.CUSTOM_SERVICE_EVENT);
		server.addEventSource(RemoteDeploymentAdminImpl.DEPLOYMENT_EVENT);
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
			server.removeEventSource(RemoteBundleAdminImpl.SYNCH_BUNDLE_EVENTS);
			server.removeEventSource(RemoteBundleAdminImpl.SYSTEM_BUNDLE_EVENT);
			server.removeEventSource(RemoteServiceAdmin.CUSTOM_SERVICE_EVENT);
			server.removeEventSource(RemoteDeploymentAdminImpl.DEPLOYMENT_EVENT);
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

	private static final void log(String message) {
		log(message, null);
	}

	private static final void log(String message, Throwable e) {
		DebugUtils.log(EventSynchronizer.class, message, e);
	}
}
