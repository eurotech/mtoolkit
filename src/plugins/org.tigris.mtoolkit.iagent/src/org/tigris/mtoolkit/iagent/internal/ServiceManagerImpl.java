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

import org.tigris.mtoolkit.iagent.Error;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteService;
import org.tigris.mtoolkit.iagent.ServiceManager;
import org.tigris.mtoolkit.iagent.event.RemoteServiceEvent;
import org.tigris.mtoolkit.iagent.event.RemoteServiceListener;
import org.tigris.mtoolkit.iagent.pmp.EventListener;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;
import org.tigris.mtoolkit.iagent.spi.ConnectionEvent;
import org.tigris.mtoolkit.iagent.spi.ConnectionListener;
import org.tigris.mtoolkit.iagent.spi.ConnectionManager;
import org.tigris.mtoolkit.iagent.spi.MethodSignature;
import org.tigris.mtoolkit.iagent.spi.PMPConnection;
import org.tigris.mtoolkit.iagent.util.DebugUtils;

final class ServiceManagerImpl implements ServiceManager, EventListener, ConnectionListener {
  private static final String       CUSTOM_SERVICE_EVENT           = "iagent_service_event";
  private static final String       EVENT_TYPE_KEY                 = "type";

  private static MethodSignature    GET_ALL_REMOTE_SERVICES_METHOD = new MethodSignature("getAllRemoteServices",
                                                                       new String[] {
      MethodSignature.STRING_TYPE, MethodSignature.STRING_TYPE
                                                                       }, true);

  private final DeviceConnectorImpl connector;

  private final List                serviceListeners               = new LinkedList();

  ServiceManagerImpl(DeviceConnectorImpl connector) {
    if (connector == null) {
      throw new IllegalArgumentException();
    }
    this.connector = connector;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.ServiceManager#getAllRemoteServices(java.lang.String, java.lang.String)
   */
  public RemoteService[] getAllRemoteServices(String clazz, String filter) throws IAgentException {
    DebugUtils.debug(this, "[getAllRemoteServices] >>> clazz: " + clazz + "; filter: " + filter);
    Object result = GET_ALL_REMOTE_SERVICES_METHOD.call(getServiceAdmin(getConnection()), new Object[] {
        clazz, filter
    });
    if (result instanceof Error) {
      throw new IllegalArgumentException(((Error) result).getMessage());
    }
    Dictionary[] servicesProps = (Dictionary[]) result;
    RemoteService[] services = new RemoteService[(servicesProps == null) ? 0 : servicesProps.length];
    if (servicesProps != null) {
      for (int i = 0; i < servicesProps.length; i++) {
        services[i] = new RemoteServiceImpl(this, servicesProps[i]);
      }
    }
    DebugUtils.debug(this, "[getAllRemoteServices] result: " + DebugUtils.convertForDebug(services));
    return services;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.ServiceManager#addRemoteServiceListener(org.tigris.mtoolkit.iagent.event.RemoteServiceListener)
   */
  public void addRemoteServiceListener(RemoteServiceListener listener) throws IAgentException {
    DebugUtils.debug(this, "[addRemoteServiceListener] >>> listener: " + listener);
    connector.getConnectionManager().addConnectionListener(this);
    synchronized (serviceListeners) {
      if (!serviceListeners.contains(listener)) {
        serviceListeners.add(listener);
        PMPConnection connection = getConnection(false);
        if (connection != null) {
          DebugUtils.debug(this, "[addRemoteServiceListener] PMP connection available, adding event listener");
          connection.addEventListener(this, new String[] {
            CUSTOM_SERVICE_EVENT
          });
        }
      } else {
        DebugUtils.debug(this, "[addRemoteServiceListener] listener already contained in the list");
      }
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.ServiceManager#removeRemoteServiceListener(org.tigris.mtoolkit.iagent.event.RemoteServiceListener)
   */
  public void removeRemoteServiceListener(RemoteServiceListener listener) throws IAgentException {
    DebugUtils.debug(this, "[removeRemoteServiceListener] >>> listener: " + listener);
    synchronized (serviceListeners) {
      if (serviceListeners.contains(listener)) {
        serviceListeners.remove(listener);
        if (serviceListeners.size() == 0) {
          PMPConnection connection = getConnection(false);
          if (connection != null) {
            DebugUtils
                .debug(this, "[removeRemoteServiceListener] PMP connection is available, removing event listener");
            connection.removeEventListener(this, new String[] {
              CUSTOM_SERVICE_EVENT
            });
          }
        }
      } else {
        DebugUtils.debug(this, "[removeRemoteServiceListener] listener not found");
      }
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.pmp.EventListener#event(java.lang.Object, java.lang.String)
   */
  public void event(Object event, String eventType) {
    DebugUtils.debug(this, "[event] >>> event: " + event + "; eventType: " + eventType);
    if (eventType.equals(CUSTOM_SERVICE_EVENT)) {
      try {
        Dictionary props = (Dictionary) event;
        int type = ((Integer) props.remove(EVENT_TYPE_KEY)).intValue();
        fireServiceEvent(props, type);
      } catch (Exception e) {
        DebugUtils.error(this, "Failed to process PMP event: " + event + "; type: " + eventType, e);
      }
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.ConnectionListener#connectionChanged(org.tigris.mtoolkit.iagent.spi.ConnectionEvent)
   */
  public void connectionChanged(ConnectionEvent event) {
    if (event.getType() == ConnectionEvent.CONNECTED
        && event.getConnection().getType() == ConnectionManager.PMP_CONNECTION) {
      synchronized (serviceListeners) {
        if (serviceListeners.size() > 0) {
          try {
            DebugUtils.debug(this, "[connectionChanged] PMP connection created, add event listener");
            ((PMPConnection) event.getConnection()).addEventListener(this, new String[] {
              CUSTOM_SERVICE_EVENT
            });
          } catch (IAgentException e) {
            DebugUtils.error(this, "[connectionChanged] Failed to add event listener to PMP connection", e);
          }
        } else {
          DebugUtils.debug(this, "[connectionChanged] PMP connection created, but no need to add event listener");
        }
      }
    }
  }

  void removeListeners() throws IAgentException {
    DebugUtils.debug(this, "[removeListeners] >>>");
    synchronized (serviceListeners) {
      serviceListeners.clear();
      PMPConnection connection = getConnection(false);
      if (connection != null) {
        DebugUtils.debug(this, "[removeListeners] PMP connection available, remove event listener");
        connection.removeEventListener(this, new String[] {
          CUSTOM_SERVICE_EVENT
        });
      }
    }
    DebugUtils.debug(this, "[removeListeners] Listener successfully removed");
  }

  DeviceConnectorImpl getDeviceConnector() {
    return connector;
  }

  PMPConnection getConnection() throws IAgentException {
    return getConnection(true);
  }

  RemoteObject getServiceAdmin(PMPConnection pmpConnection) throws IAgentException {
    return pmpConnection.getRemoteServiceAdmin();
  }

  PMPConnection getConnection(boolean create) throws IAgentException {
    DebugUtils.debug(this, "[getConnection] >>> create: " + create);
    ConnectionManager connectionManager = connector.getConnectionManager();
    PMPConnection pmpConnection = (PMPConnection) connectionManager
        .getActiveConnection(ConnectionManager.PMP_CONNECTION);
    if (pmpConnection == null && create) {
      DebugUtils.debug(this, "[getConnection] No active connection found. Create new PMP connection...");
      if (!connector.isActive()) {
        DebugUtils.info(this,
            "[getConnection] Request for new connection arrived, but DeviceConnector is disconnected.");
        throw new IAgentException("Associated DeviceConnector object is closed", IAgentErrors.ERROR_DISCONNECTED);
      }
      pmpConnection = (PMPConnection) connectionManager.createConnection(ConnectionManager.PMP_CONNECTION);
      DebugUtils.debug(this, "[getConnection] PMP connection opened successfully: " + pmpConnection);
    } else {
      DebugUtils.debug(this, "[getConnection] Active connection found: " + pmpConnection);
    }
    return pmpConnection;
  }

  private void fireServiceEvent(Dictionary serviceProps, int type) {
    DebugUtils.debug(this, "[fireServiceEvent] >>> serviceProps: " + DebugUtils.convertForDebug(serviceProps)
        + "; type=" + type);
    RemoteServiceListener[] listeners;
    synchronized (serviceListeners) {
      if (serviceListeners.size() != 0) {
        listeners = (RemoteServiceListener[]) serviceListeners.toArray(new RemoteServiceListener[serviceListeners
            .size()]);
      } else {
        return;
      }
    }
    RemoteService service = new RemoteServiceImpl(this, serviceProps);
    RemoteServiceEvent event = new RemoteServiceEvent(service, type);
    DebugUtils.debug(this, "[fireServiceEvent] listener count: " + listeners.length + "; event=" + event);
    for (int i = 0; i < listeners.length; i++) {
      RemoteServiceListener listener = listeners[i];
      try {
        DebugUtils.debug(this, "[fireServiceEvent] deliver event to listener: " + listener);
        listener.serviceChanged(event);
      } catch (Throwable e) {
        DebugUtils.error(this, "[fireServiceEvent] Failed to deliver event to " + listener, e);
      }
    }
  }

}
