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
import org.tigris.mtoolkit.iagent.internal.connection.ConnectionEvent;
import org.tigris.mtoolkit.iagent.internal.connection.ConnectionListener;
import org.tigris.mtoolkit.iagent.internal.connection.ConnectionManager;
import org.tigris.mtoolkit.iagent.internal.connection.PMPConnection;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.pmp.EventListener;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;


public class ServiceManagerImpl implements ServiceManager, EventListener, ConnectionListener {

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
    log("[getAllRemoteServices] >>> clazz: " + clazz + "; filter: " + filter);
    if (filter != null) {
      String filterCheck = (String) Utils.callRemoteMethod(getServiceAdmin(getConnection()), Utils.CHECK_FILTER_METHOD, new Object[] { filter });
      if (filterCheck != null) {    // invalid filter syntax
        log("[getAllRemoteServices] Filter check failed: " + filterCheck);
        throw new IllegalArgumentException("Invalid Filter Syntax: " + filterCheck);
      }
    }
    Dictionary[] servicesProps = (Dictionary[]) Utils.callRemoteMethod(getServiceAdmin(getConnection()), Utils.GET_ALL_REMOTE_SERVICES_METHOD, new Object[] { clazz, filter });
    if (servicesProps == null) {
      log("[getAllRemoteServices] Internal error: it seems that filter check either returned invalid result or we interpreted it wrong");
      throw new IAgentException("Internal error: invalid filter syntax exception", IAgentErrors.ERROR_INTERNAL_ERROR);
    }
    RemoteService[] services = new RemoteService[servicesProps.length];
    for (int i = 0; i < servicesProps.length; i++) {
      services[i] = new RemoteServiceImpl(this, servicesProps[i]);
    }
    log("[getAllRemoteServices] result: " + DebugUtils.convertForDebug(services));
    return services;
  }

  public void addRemoteServiceListener(RemoteServiceListener listener) throws IAgentException {
    log("[addRemoteServiceListener] >>> listener: " + listener);
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
          log("[addRemoteServiceListener] PMP connection available, adding event listener");
          connection.addEventListener(this, new String[] { CUSTOM_SERVICE_EVENT });
        }
      } else {
        log("[addRemoteServiceListener] listener already contained in the list");
      }
    }
  }

  public void removeRemoteServiceListener(RemoteServiceListener listener) throws IAgentException {
    log("[removeRemoteServiceListener] >>> listener: " + listener);
    synchronized (serviceListeners) {
      if (serviceListeners.contains(listener)) {
        serviceListeners.remove(listener);
        if (serviceListeners.size() == 0) {
          PMPConnection connection = getConnection(false);
          if (connection != null) {
            log("[removeRemoteServiceListener] PMP connection is available, removing event listener");
            connection.removeEventListener(this, new String[] { CUSTOM_SERVICE_EVENT });
          }
        }
      } else {
        log("[removeRemoteServiceListener] listener not found");
      }
    }
  }

  PMPConnection getConnection() throws IAgentException {
    return getConnection(true);
  }

  PMPConnection getConnection(boolean create) throws IAgentException {
    log("[getConnection] >>> create: " + create);
    ConnectionManager connectionManager = connector.getConnectionManager();
    PMPConnection pmpConnection = (PMPConnection) connectionManager.getActiveConnection(ConnectionManager.PMP_CONNECTION);
    if (pmpConnection == null && create) {
      log("[getConnection] No active connection found. Create new PMP connection...");
      if (!connector.isActive()) {
        log("[getConnection] Request for new connection arrived, but DeviceConnector is disconnected.");
        throw new IAgentException("Associated DeviceConnector object is closed", IAgentErrors.ERROR_DISCONNECTED);
      }
      pmpConnection = (PMPConnection) connectionManager.createConnection(ConnectionManager.PMP_CONNECTION);
      log("[getConnection] PMP connection opened successfully: " + pmpConnection);
    } else {
      log("[getConnection] Active connection found: " + pmpConnection);
    }
    return pmpConnection;
  }

  public void event(Object event, String eventType) {
    log("[event] >>> event: " + event + "; eventType: " + eventType);
    if (eventType.equals(CUSTOM_SERVICE_EVENT)) {
      try {
        Dictionary props = (Dictionary) event;
        int type = ((Integer) props.remove(EVENT_TYPE_KEY)).intValue();
        fireServiceEvent(props, type);
      } catch (Exception e) {
        IAgentLog.error("[ServiceManagerImpl][event] Failed to process PMP event: " + event + "; type: " + eventType, e);
      }
    }
  }

  private void fireServiceEvent(Dictionary serviceProps, int type) {
    log("[fireServiceEvent] >>> serviceProps: " + DebugUtils.convertForDebug(serviceProps) + "; type=" + type);
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
    log("[fireServiceEvent] listener count: " + listeners.length + "; event=" + event);
    for (int i = 0; i < listeners.length; i++) {
      RemoteServiceListener listener = listeners[i];
      try {
        log("[fireServiceEvent] deliver event to listener: " + listener);
        listener.serviceChanged(event);
      } catch (Throwable e) {
        log("[fireServiceEvent] Failed to deliver event to " + listener, e);
      }
    }
  }

  DeviceConnectorImpl getDeviceConnector() {
    return connector;
  }

  public void connectionChanged(ConnectionEvent event) {
    if (event.getType() == ConnectionEvent.CONNECTED && event.getConnection().getType() == ConnectionManager.PMP_CONNECTION) {
      synchronized (serviceListeners) {
        if (serviceListeners != null && serviceListeners.size() > 0) {
          try {
            log("[connectionChanged] PMP connection created, add event listener");
            ((PMPConnection) event.getConnection()).addEventListener(this, new String[] { CUSTOM_SERVICE_EVENT });
          } catch (IAgentException e) {
            log("[connectionChanged] Failed to add event listener to PMP connection", e);
          }
        } else {
          log("[connectionChanged] PMP connection created, but no need to add event listener");
        }
      }
    }
  }
  public void removeListeners() throws IAgentException {
    log("[removeListeners] >>>");
    synchronized (serviceListeners) {
      serviceListeners.clear();
      PMPConnection connection = getConnection(false);
      if (connection != null) {
        log("[removeListeners] PMP connection available, remove event listener");
        connection.removeEventListener(this, new String[] { CUSTOM_SERVICE_EVENT });
      }
    }
    log("[removeListeners] Listener successfully removed");
  }

  private final void log(String message) {
    log(message, null);
  }

  private final void log(String message, Throwable e) {
    DebugUtils.log(this, message, e);
  }

}
