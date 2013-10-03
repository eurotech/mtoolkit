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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.tigris.mtoolkit.iagent.DeploymentManager;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAProgressMonitor;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.ServiceManager;
import org.tigris.mtoolkit.iagent.VMManager;
import org.tigris.mtoolkit.iagent.event.RemoteDevicePropertyEvent;
import org.tigris.mtoolkit.iagent.event.RemoteDevicePropertyListener;
import org.tigris.mtoolkit.iagent.internal.tcp.ConnectionManagerImpl;
import org.tigris.mtoolkit.iagent.pmp.EventListener;
import org.tigris.mtoolkit.iagent.pmp.PMPService;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;
import org.tigris.mtoolkit.iagent.rpc.Capabilities;
import org.tigris.mtoolkit.iagent.rpc.RemoteCapabilitiesProvider;
import org.tigris.mtoolkit.iagent.spi.AbstractConnection;
import org.tigris.mtoolkit.iagent.spi.ConnectionEvent;
import org.tigris.mtoolkit.iagent.spi.ConnectionListener;
import org.tigris.mtoolkit.iagent.spi.ConnectionManager;
import org.tigris.mtoolkit.iagent.spi.DeviceConnectorSpi;
import org.tigris.mtoolkit.iagent.spi.ExtConnectionFactory;
import org.tigris.mtoolkit.iagent.spi.IAgentManager;
import org.tigris.mtoolkit.iagent.spi.MethodSignature;
import org.tigris.mtoolkit.iagent.spi.PMPConnection;
import org.tigris.mtoolkit.iagent.transport.Transport;
import org.tigris.mtoolkit.iagent.util.DebugUtils;
import org.tigris.mtoolkit.iagent.util.LightServiceRegistry;

/**
 *
 * DeviceConnector implementation
 *
 */
public final class DeviceConnectorImpl extends DeviceConnector implements EventListener, ConnectionListener,
    DeviceConnectorSpi {
  private static final String         EVENT_CAPABILITY_NAME   = "capability.name";
  private static final String         EVENT_CAPABILITY_VALUE  = "capability.value";

  private static final String         DEVICE_PROPERTY_EVENT   = "iagent_property_event";

  private final Object                lock                    = new Object();

  private LightServiceRegistry        serviceRegistry;
  private VMManagerImpl               runtimeCommands;
  private DeploymentManagerImpl       deploymentCommands;
  private ServiceManagerImpl          serviceManager;

  private final ConnectionManagerImpl connectionManager;
  private final Dictionary            connectionProperties;

  private volatile boolean            isActive                = true;

  private final List                  devicePropertyListeners = new LinkedList();
  private final HashMap               managers                = new HashMap(4);
  private final HashSet               currentConnectionTypes  = new HashSet();

  private final MethodSignature       methodGetCapabilities   = new MethodSignature("getCapabilities"); //$NON-NLS-1$

  /**
   * Creates new DeviceConnector with specified transport object
   *
   * @param transport
   * @param aConManager
   * @param monitor
   *          progress monitor. Can be null.
   * @throws IAgentException
   */
  public DeviceConnectorImpl(Transport transport, Dictionary props, IAProgressMonitor monitor) throws IAgentException {
    DebugUtils.debug(this, "[Constructor] >>> connection properties: " + DebugUtils.convertForDebug(props));
    if (props == null) {
      throw new IllegalArgumentException("Connection properties hashtable could not be null!");
    }
    this.connectionProperties = props;
    setTransportProps(transport);

    connectionManager = new ConnectionManagerImpl(transport, props);
    connectionManager.addConnectionListener(this);
    connect(props, monitor);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.ConnectionListener#connectionChanged(org.tigris.mtoolkit.iagent.spi.ConnectionEvent)
   */
  public void connectionChanged(ConnectionEvent event) {
    int connectionType = event.getConnection().getType();
    switch (event.getType()) {
    case ConnectionEvent.CONNECTED:
      currentConnectionTypes.add(new Integer(connectionType));
      break;
    case ConnectionEvent.DISCONNECTED:
      currentConnectionTypes.remove(new Integer(connectionType));
      break;
    }
    if (currentConnectionTypes.isEmpty()) {
      DebugUtils.debug(this, "No active connections. Closing DeviceConnector...");
      try {
        if (isActive) {
          closeConnection();
        }
      } catch (IAgentException e) {
        DebugUtils.error(this, "Failed to cleanup after disconnection", e);
      }
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeviceConnector#addRemoteDevicePropertyListener(org.tigris.mtoolkit.iagent.event.RemoteDevicePropertyListener)
   */
  public void addRemoteDevicePropertyListener(RemoteDevicePropertyListener listener) throws IAgentException {
    DebugUtils.debug(this, "[addRemoteDevicePropertyListener] >>> listener: " + listener);
    synchronized (devicePropertyListeners) {
      if (!devicePropertyListeners.contains(listener)) {
        PMPConnection connection = (PMPConnection) getConnection(ConnectionManager.PMP_CONNECTION, false);
        if (connection != null) {
          DebugUtils.debug(this, "[addRemoteDevicePropertyListener] PMP connection is available, add event listener");
          connection.addEventListener(this, new String[] {
            DEVICE_PROPERTY_EVENT
          });
        }
        devicePropertyListeners.add(listener);
      } else {
        DebugUtils.debug(this, "[addRemoteDevicePropertyListener] Listener already present");
      }
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeviceConnector#removeRemoteDevicePropertyListener(org.tigris.mtoolkit.iagent.event.RemoteDevicePropertyListener)
   */
  public void removeRemoteDevicePropertyListener(RemoteDevicePropertyListener listener) throws IAgentException {
    DebugUtils.debug(this, "[removeRemoteDevicePropertyListener] >>> listener: " + listener);
    synchronized (devicePropertyListeners) {
      if (devicePropertyListeners.contains(listener)) {
        devicePropertyListeners.remove(listener);
        if (devicePropertyListeners.size() == 0) {
          DebugUtils.debug(this,
              "[removeRemoteDevicePropertyListener] No more listeners in the list, try to remove PMP event listener");
          PMPConnection connection = (PMPConnection) getConnection(ConnectionManager.PMP_CONNECTION, false);
          if (connection != null) {
            DebugUtils.debug(this,
                "[removeRemoteDevicePropertyListener] PMP connection is available, remove event listener");
            connection.removeEventListener(this, new String[] {
              DEVICE_PROPERTY_EVENT
            });
          }
        }
      } else {
        DebugUtils.debug(this, "[removeRemoteDevicePropertyListener] Listener not found in the list");
      }
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.DeviceConnectorSpi#getDeviceConnector()
   */
  public DeviceConnector getDeviceConnector() {
    return this;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeviceConnector#closeConnection()
   */
  public void closeConnection() throws IAgentException {
    DebugUtils.debug(this, "[closeConnection] >>> Closing DeviceConnector...");
    synchronized (lock) {
      if (!isActive) {
        DebugUtils.debug(this, "[closeConnection] Already closed.");
        return;
      }
      isActive = false;
    }
    try {
      Iterator managersIterator = managers.entrySet().iterator();
      while (managersIterator.hasNext()) {
        Entry next = (Entry) managersIterator.next();
        ((IAgentManager) next.getValue()).dispose();
      }
      managers.clear();
      if (deploymentCommands != null) {
        deploymentCommands.removeListeners();
      }
      if (serviceManager != null) {
        serviceManager.removeListeners();
      }
      if (connectionManager != null) {
        connectionManager.removeListeners();
        DebugUtils.debug(this, "[closeConnection] Closing underlying connections...");
        connectionManager.closeConnections();
      }
      DebugUtils.debug(this, "[closeConnection] DeviceConnector closed successfully");
    } catch (Throwable t) {
      DebugUtils.error(this, "Failed to close underlying connections", t);
    }
    fireConnectionEvent(DISCONNECTED, this);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeviceConnector#getVMManager()
   */
  public VMManager getVMManager() throws IAgentException {
    synchronized (lock) {
      if (!isActive) {
        DebugUtils.info(this, "[getVMManager] Request for VMManager received, but DeviceConnector is closed");
        throw new IAgentException("The connection is closed", IAgentErrors.ERROR_DISCONNECTED);
      }
      if (runtimeCommands == null) {
        runtimeCommands = new VMManagerImpl(this);
      }
    }
    return runtimeCommands;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeviceConnector#getDeploymentManager()
   */
  public DeploymentManager getDeploymentManager() throws IAgentException {
    synchronized (lock) {
      if (!isActive) {
        DebugUtils.info(this,
            "[getDeploymentManager] Request for DeploymentManager received, but DeviceConnector is closed");
        throw new IAgentException("The connection is closed", IAgentErrors.ERROR_DISCONNECTED);
      }
      if (deploymentCommands == null) {
        deploymentCommands = new DeploymentManagerImpl(this);
      }
    }
    return deploymentCommands;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeviceConnector#isActive()
   */
  public boolean isActive() {
    return isActive;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.DeviceConnectorSpi#getConnectionManager()
   */
  public ConnectionManager getConnectionManager() {
    return connectionManager;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeviceConnector#getServiceManager()
   */
  public ServiceManager getServiceManager() throws IAgentException {
    synchronized (lock) {
      if (!isActive) {
        DebugUtils.info(this, "[getServiceManager] Request for ServiceManager received, but DeviceConnector is closed");
        throw new IAgentException("Connection to target device has been closed.", IAgentErrors.ERROR_DISCONNECTED);
      }
      if (serviceManager == null) {
        serviceManager = new ServiceManagerImpl(this);
      }
    }
    return serviceManager;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeviceConnector#getManager(java.lang.String)
   */
  public Object getManager(String className) throws IAgentException {
    synchronized (lock) {
      if (!isActive) {
        DebugUtils.info(this, "[getManager] Request for getting Manager [" + className
            + "] received, but DeviceConnector is closed");
        throw new IAgentException("The connection is closed", IAgentErrors.ERROR_DISCONNECTED);
      }
      IAgentManager manager = (IAgentManager) managers.get(className);
      if (manager == null) {
        LightServiceRegistry registry = getServiceRegistry();
        manager = (IAgentManager) registry.get(className);
        if (manager == null) {
          return null;
        }
        manager.init(this);
        managers.put(className, manager);
      }
      return manager;
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeviceConnector#getProperties()
   */
  public Dictionary getProperties() {
    Dictionary props = cloneDictionary(connectionProperties);
    return props;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeviceConnector#getRemoteProperties()
   */
  public Dictionary getRemoteProperties() throws IAgentException {
    Dictionary props = new Hashtable();
    if (isActive) {
      try {
        // We shall not create PMP connection here, only check for
        // active PMP connection.
        // The connector can be active with only controller connection,
        // which can be used
        // later to create PMP connection to different port.
        PMPConnection connection = (PMPConnection) getConnection(ConnectionManager.PMP_CONNECTION, false);
        if (connection != null) {
          RemoteObject service = connection.getRemoteAdmin(RemoteCapabilitiesProvider.class.getName());
          if (service != null) {
            Map devCapabilities = (Map) methodGetCapabilities.call(service);
            Iterator iterator = devCapabilities.keySet().iterator();
            while (iterator.hasNext()) {
              String property = (String) iterator.next();
              props.put(property, devCapabilities.get(property));
            }
          }
          props.put(Capabilities.CAPABILITIES_SUPPORT, Boolean.TRUE);
        }
      } catch (Exception e) {
        DebugUtils.error(this, "Failed to get Remote Capabilities", e);
        props.put(Capabilities.CAPABILITIES_SUPPORT, Boolean.FALSE);
      }
    }
    return props;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.pmp.EventListener#event(java.lang.Object, java.lang.String)
   */
  public void event(Object event, String evType) {
    try {
      DebugUtils.debug(this, "[event] >>> event: " + event + "; type: " + evType);
      if (DEVICE_PROPERTY_EVENT.equals(evType)) {
        Dictionary eventProps = (Dictionary) event;
        String capabilityName = (String) eventProps.get(EVENT_CAPABILITY_NAME);
        Object capabilityValue = eventProps.get(EVENT_CAPABILITY_VALUE);
        fireDevicePropertyEvent(capabilityName, capabilityValue);
      }
    } catch (Throwable e) {
      DebugUtils.error(this, "Failed to process PMP event: " + event + "; type: " + evType);
    }
  }

  public AbstractConnection getConnection(int type, boolean create) throws IAgentException {
    DebugUtils.debug(this, "[getConnection] >>> create: " + create);
    ConnectionManager connectionManager = getConnectionManager();
    AbstractConnection connection = connectionManager.getActiveConnection(type);
    if (connection == null && create) {
      DebugUtils
          .debug(this, "[getConnection] No active connection found. Create new connection (type=" + type + ")...");
      if (!isActive()) {
        DebugUtils.info(this,
            "[getConnection] Request for new connection arrived, but DeviceConnector is disconnected.");
        throw new IAgentException("Associated DeviceConnector object is closed", IAgentErrors.ERROR_DISCONNECTED);
      }
      connection = connectionManager.createConnection(type);
      DebugUtils.debug(this, "[getConnection] Connection opened successfully: " + connection);
    } else {
      DebugUtils.debug(this, "[getConnection] Active connection found: " + connection);
    }
    return connection;
  }

  public AbstractConnection getConnection(int type) throws IAgentException {
    return getConnection(type, true);
  }

  private void connect(Dictionary props, IAProgressMonitor monitor) throws IAgentException {
    Boolean connectImmeadiate = (Boolean) props.get("framework-connection-immediate");
    if (connectImmeadiate == null) {
      connectImmeadiate = Boolean.TRUE;
    }
    //In case we already have PMP port connect to PMP directly
    final Integer pmpPort = (Integer) props.get(PMPService.PROP_PMP_PORT);
    if (pmpPort != null || !connectImmeadiate.booleanValue()) {
      DebugUtils.debug(this, "[connect] Connect directly to PMP");
      connect0(ConnectionManager.PMP_CONNECTION, monitor);
    }
    if (connectImmeadiate.booleanValue()) {
      // Trying controller connections
      ExtConnectionFactory[] factories = connectionManager.getExtConnectionFactories();
      for (int i = 0; i < factories.length; i++) {
        checkCancel(monitor);
        if (factories[i].isSupported() && (pmpPort == null || factories[i].isControllerType(this))) {
          try {
            DebugUtils.debug(this,
                "[connect] Trying to connect to controller of type: " + factories[i].getConnectionType());
            connect0(factories[i].getConnectionType(), monitor);
          } catch (IAgentException e) {
            DebugUtils.debug(this, "[connect] Failed: " + e);
            throw new IAgentException("Unable to create controller connection", IAgentErrors.ERROR_CANNOT_CONNECT, e);
          }
        }
      }
    }
  }

  private void connect0(int connectionType, IAProgressMonitor monitor) throws IAgentException {
    DebugUtils.debug(this, "[connect] >>> connectionType: " + connectionType);
    AbstractConnection connection = connectionManager.getActiveConnection(connectionType);
    if (connection == null) {
      DebugUtils.debug(this, "[connect] No active connection with type: " + connectionType + ". Create new...");
      connection = connectionManager.createConnection(connectionType, monitor);
      if (connection == null) {
        DebugUtils.info(this, "[connect] Failed to create connection of type: " + connectionType);
        throw new IAgentException("Unable to create connection", IAgentErrors.ERROR_CANNOT_CONNECT);
      }
    }
    DebugUtils.debug(this, "[connect] connection: " + connection);
  }

  private void checkCancel(IAProgressMonitor monitor) throws IAgentException {
    if (monitor != null && monitor.isCanceled()) {
      throw new IAgentException("Operation canceled", IAgentErrors.OPERATION_CANCELED);
    }
  }

  private Dictionary cloneDictionary(Dictionary source) {
    Dictionary dest = new Hashtable();
    for (Enumeration en = source.keys(); en.hasMoreElements();) {
      Object k = en.nextElement();
      dest.put(k, source.get(k));
    }
    return dest;
  }

  private void fireDevicePropertyEvent(String property, Object value) {
    DebugUtils.debug(this, "[fireDevicePropertyEvent] >>> property: " + property);
    RemoteDevicePropertyListener[] listeners;
    synchronized (devicePropertyListeners) {
      if (devicePropertyListeners.size() != 0) {
        listeners = (RemoteDevicePropertyListener[]) devicePropertyListeners
            .toArray(new RemoteDevicePropertyListener[devicePropertyListeners.size()]);
      } else {
        return;
      }
    }
    RemoteDevicePropertyEvent event = new RemoteDevicePropertyEvent(property, value);
    DebugUtils.debug(this, "[fireRemoteDevicePropertyEvent] " + listeners.length + " listeners found.");
    for (int i = 0; i < listeners.length; i++) {
      RemoteDevicePropertyListener listener = listeners[i];
      try {
        DebugUtils.debug(this, "[fireRemoteDevicePropertyEvent] deliver event: " + event + " to listener: " + listener);
        listener.devicePropertiesChanged(event);
      } catch (Throwable e) {
        DebugUtils.error(this, "[fireRemoteDevicePropertyEvent] Failed to deliver event to " + listener, e);
      }
    }
  }

  private LightServiceRegistry getServiceRegistry() {
    if (serviceRegistry == null) {
      serviceRegistry = new LightServiceRegistry(DeviceConnectorImpl.class.getClassLoader());
    }
    return serviceRegistry;
  }

  private void setTransportProps(Transport transport) {
    connectionProperties.put(TRANSPORT_TYPE, transport.getType().getTypeId());
    connectionProperties.put(TRANSPORT_ID, transport.getId());
  }
}
