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

import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;

import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteService;
import org.tigris.mtoolkit.iagent.ServiceManager;
import org.tigris.mtoolkit.iagent.event.RemoteServiceEvent;
import org.tigris.mtoolkit.iagent.event.RemoteServiceListener;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.pmp.EventListener;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;
import org.tigris.mtoolkit.iagent.spi.ConnectionEvent;
import org.tigris.mtoolkit.iagent.spi.ConnectionListener;
import org.tigris.mtoolkit.iagent.spi.ConnectionManager;
import org.tigris.mtoolkit.iagent.spi.MethodSignature;
import org.tigris.mtoolkit.iagent.spi.PMPConnection;

public class ServiceManagerImpl implements ServiceManager, EventListener, ConnectionListener {

	private static MethodSignature GET_ALL_REMOTE_SERVICES_METHOD = new MethodSignature("getAllRemoteServices", new String[] { MethodSignature.STRING_TYPE, MethodSignature.STRING_TYPE }, true);
	private static MethodSignature CHECK_FILTER_METHOD = new MethodSignature("checkFilter", new String[] { MethodSignature.STRING_TYPE }, true);

	private DeviceConnectorImpl connector;

	private static final String CUSTOM_SERVICE_EVENT = "iagent_service_event";
	private static final String EVENT_TYPE_KEY = "type";

	private List serviceListeners = new LinkedList();

	private boolean addedConnectionListener;

	public ServiceManagerImpl(DeviceConnectorImpl connector) {
		if (connector == null)
			throw new IllegalArgumentException();
		this.connector = connector;
	}

	RemoteObject getServiceAdmin(PMPConnection pmpConnection) throws IAgentException {
		return pmpConnection.getRemoteServiceAdmin();
	}

	public RemoteService[] getAllRemoteServices(String clazz, String filter) throws IAgentException {
		debug("[getAllRemoteServices] >>> clazz: " + clazz + "; filter: " + filter);
		if (filter != null) {
			String filterCheck = (String) CHECK_FILTER_METHOD.call(getServiceAdmin(getConnection()),
				new Object[] { filter });
			if (filterCheck != null) { // invalid filter syntax
				info("[getAllRemoteServices] Filter check failed: " + filterCheck);
				throw new IllegalArgumentException("Invalid Filter Syntax: " + filterCheck);
			}
		}
		Dictionary[] servicesProps = (Dictionary[]) GET_ALL_REMOTE_SERVICES_METHOD.call(getServiceAdmin(getConnection()),
			new Object[] { clazz, filter });
		if (servicesProps == null) {
			info("[getAllRemoteServices] Internal error: it seems that filter check either returned invalid result or we interpreted it wrong");
			throw new IAgentException("Internal error: invalid filter syntax exception",
				IAgentErrors.ERROR_INTERNAL_ERROR);
		}
		RemoteService[] services = new RemoteService[servicesProps.length];
		for (int i = 0; i < servicesProps.length; i++) {
			services[i] = new RemoteServiceImpl(this, servicesProps[i]);
		}
		debug("[getAllRemoteServices] result: " + DebugUtils.convertForDebug(services));
		return services;
	}

	public void addRemoteServiceListener(RemoteServiceListener listener) throws IAgentException {
		debug("[addRemoteServiceListener] >>> listener: " + listener);
		synchronized (this) {
			if (!addedConnectionListener) {
				connector.getConnectionManager().addConnectionListener(this);
				addedConnectionListener = true;
			}
		}
		synchronized (serviceListeners) {
			if (!serviceListeners.contains(listener)) {
				serviceListeners.add(listener);
				PMPConnection connection = getConnection(false);
				if (connection != null) {
					debug("[addRemoteServiceListener] PMP connection available, adding event listener");
					connection.addEventListener(this, new String[] { CUSTOM_SERVICE_EVENT });
				}
			} else {
				debug("[addRemoteServiceListener] listener already contained in the list");
			}
		}
	}

	public void removeRemoteServiceListener(RemoteServiceListener listener) throws IAgentException {
		debug("[removeRemoteServiceListener] >>> listener: " + listener);
		synchronized (serviceListeners) {
			if (serviceListeners.contains(listener)) {
				serviceListeners.remove(listener);
				if (serviceListeners.size() == 0) {
					PMPConnection connection = getConnection(false);
					if (connection != null) {
						debug("[removeRemoteServiceListener] PMP connection is available, removing event listener");
						connection.removeEventListener(this, new String[] { CUSTOM_SERVICE_EVENT });
					}
				}
			} else {
				debug("[removeRemoteServiceListener] listener not found");
			}
		}
	}

	PMPConnection getConnection() throws IAgentException {
		return getConnection(true);
	}

	PMPConnection getConnection(boolean create) throws IAgentException {
		debug("[getConnection] >>> create: " + create);
		ConnectionManager connectionManager = connector.getConnectionManager();
		PMPConnection pmpConnection = (PMPConnection) connectionManager.getActiveConnection(ConnectionManager.PMP_CONNECTION);
		if (pmpConnection == null && create) {
			debug("[getConnection] No active connection found. Create new PMP connection...");
			if (!connector.isActive()) {
				info("[getConnection] Request for new connection arrived, but DeviceConnector is disconnected.");
				throw new IAgentException("Associated DeviceConnector object is closed",
					IAgentErrors.ERROR_DISCONNECTED);
			}
			pmpConnection = (PMPConnection) connectionManager.createConnection(ConnectionManager.PMP_CONNECTION);
			debug("[getConnection] PMP connection opened successfully: " + pmpConnection);
		} else {
			debug("[getConnection] Active connection found: " + pmpConnection);
		}
		return pmpConnection;
	}

	public void event(Object event, String eventType) {
		debug("[event] >>> event: " + event + "; eventType: " + eventType);
		if (eventType.equals(CUSTOM_SERVICE_EVENT)) {
			try {
				Dictionary props = (Dictionary) event;
				int type = ((Integer) props.remove(EVENT_TYPE_KEY)).intValue();
				fireServiceEvent(props, type);
			} catch (Exception e) {
				IAgentLog.error("[ServiceManagerImpl][event] Failed to process PMP event: "
								+ event
								+ "; type: "
								+ eventType, e);
			}
		}
	}

	private void fireServiceEvent(Dictionary serviceProps, int type) {
		debug("[fireServiceEvent] >>> serviceProps: " + DebugUtils.convertForDebug(serviceProps) + "; type=" + type);
		RemoteServiceListener[] listeners;
		synchronized (serviceListeners) {
			if (serviceListeners.size() != 0) {
				listeners = (RemoteServiceListener[]) serviceListeners.toArray(new RemoteServiceListener[serviceListeners.size()]);
			} else {
				return;
			}
		}
		RemoteService service = new RemoteServiceImpl(this, serviceProps);
		RemoteServiceEvent event = new RemoteServiceEvent(service, type);
		debug("[fireServiceEvent] listener count: " + listeners.length + "; event=" + event);
		for (int i = 0; i < listeners.length; i++) {
			RemoteServiceListener listener = listeners[i];
			try {
				debug("[fireServiceEvent] deliver event to listener: " + listener);
				listener.serviceChanged(event);
			} catch (Throwable e) {
				error("[fireServiceEvent] Failed to deliver event to " + listener, e);
			}
		}
	}

	DeviceConnectorImpl getDeviceConnector() {
		return connector;
	}

	public void connectionChanged(ConnectionEvent event) {
		if (event.getType() == ConnectionEvent.CONNECTED
						&& event.getConnection().getType() == ConnectionManager.PMP_CONNECTION) {
			synchronized (serviceListeners) {
				if (serviceListeners != null && serviceListeners.size() > 0) {
					try {
						debug("[connectionChanged] PMP connection created, add event listener");
						((PMPConnection) event.getConnection()).addEventListener(this,
							new String[] { CUSTOM_SERVICE_EVENT });
					} catch (IAgentException e) {
						error("[connectionChanged] Failed to add event listener to PMP connection", e);
					}
				} else {
					debug("[connectionChanged] PMP connection created, but no need to add event listener");
				}
			}
		}
	}

	public void removeListeners() throws IAgentException {
		debug("[removeListeners] >>>");
		synchronized (serviceListeners) {
			serviceListeners.clear();
			PMPConnection connection = getConnection(false);
			if (connection != null) {
				debug("[removeListeners] PMP connection available, remove event listener");
				connection.removeEventListener(this, new String[] { CUSTOM_SERVICE_EVENT });
			}
		}
		debug("[removeListeners] Listener successfully removed");
	}

	private final void debug(String message) {
		DebugUtils.debug(this, message);
	}

	private final void info(String message) {
		DebugUtils.info(this, message);
	}

	private final void error(String message, Throwable t) {
		DebugUtils.error(this, message, t);
	}
}
