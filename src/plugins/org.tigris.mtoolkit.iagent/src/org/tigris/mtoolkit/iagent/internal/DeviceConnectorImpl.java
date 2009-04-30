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
import java.util.Map;

import org.osgi.service.event.EventAdmin;
import org.tigris.mtoolkit.iagent.DeploymentManager;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.ServiceManager;
import org.tigris.mtoolkit.iagent.VMManager;
import org.tigris.mtoolkit.iagent.event.RemoteDevicePropertyEvent;
import org.tigris.mtoolkit.iagent.event.RemoteDevicePropertyListener;
import org.tigris.mtoolkit.iagent.internal.connection.AbstractConnection;
import org.tigris.mtoolkit.iagent.internal.connection.ConnectionEvent;
import org.tigris.mtoolkit.iagent.internal.connection.ConnectionListener;
import org.tigris.mtoolkit.iagent.internal.connection.ConnectionManager;
import org.tigris.mtoolkit.iagent.internal.connection.PMPConnection;
import org.tigris.mtoolkit.iagent.internal.tcp.ConnectionManagerImpl;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.pmp.EventListener;
import org.tigris.mtoolkit.iagent.rpc.RemoteApplicationAdmin;
import org.tigris.mtoolkit.iagent.rpc.RemoteConsole;
import org.tigris.mtoolkit.iagent.rpc.RemoteDeploymentAdmin;

/**
 * 
 * DeviceConnector implementation
 * 
 */
public class DeviceConnectorImpl extends DeviceConnector implements EventListener {
	private VMManagerImpl runtimeCommands;
	private DeploymentManagerImpl deploymentCommands;
	private ServiceManagerImpl serviceManager;
	private ConnectionManager connectionManager;
	private Object lock = new Object();
	private volatile boolean isActive = true;
	private Dictionary connectionProperties;

	private List devicePropertyListeners = new LinkedList();
	private String DEVICE_PROPERTY_EVENT = "iagent_property_event";
	private PropertiesRegestry propRegistry;

	private static final String EVENT_OBJECT_CLASS = "objectClass";
	private static final String EVENT_STATE_KEY = "iagent.service.state";

	/**
	 * Creates new DeviceConnector with specified transport object
	 * 
	 * @param aConManager
	 * @throws IAgentException
	 */
	public DeviceConnectorImpl(Dictionary props) throws IAgentException {
		log("[Constructor] >>> connection properties: " + DebugUtils.convertForDebug(props));
		if (props == null)
			throw new IllegalArgumentException("Connection properties hashtable could not be null!");
		this.connectionProperties = props;
		connectionManager = new ConnectionManagerImpl(props);
		log("[Constructor] Connect to device");
		connect(ConnectionManager.PMP_CONNECTION);
	}

	private void monitorConnection(final int connectionType) {
		log("[monitorConnection] >>> connectionType: " + connectionType);
		connectionManager.addConnectionListener(new ConnectionListener() {
			public void connectionChanged(ConnectionEvent event) {
				if (event.getType() == ConnectionEvent.DISCONNECTED
								&& event.getConnection().getType() == connectionType) {
					log("[Constructor] connection of type: "
									+ connectionType
									+ " was disconnected. Close DeviceConnector...");
					try {
						closeConnection();
					} catch (IAgentException e) {
						IAgentLog.error("[DeviceConnectorImpl][Constructor] Failed to cleanup after disconnection", e);
					}
				}
			}
		});
	}

	public void monitorDeviceProperties() throws IAgentException {
		propRegistry = new PropertiesRegestry(serviceManager, this);
		addRemoteDevicePropertyListener(propRegistry);
	}

	public void cancelMonitoringDeviceProperties() throws IAgentException {
		removeRemoteDevicePropertyListener(propRegistry);
	}

	private void connect(int connectionType) throws IAgentException {
		log("[connect] >>> connectionType: " + connectionType);
		// start monitoring the connection before connecting
		monitorConnection(connectionType);
		AbstractConnection connection = connectionManager.getActiveConnection(connectionType);
		if (connection == null) {
			log("[connect] No active connection with type: " + connectionType + ". Create new...");
			connection = connectionManager.createConnection(connectionType);
			if (connection == null) {
				log("[connect] Failed to create connection of type: " + connectionType);
				throw new IAgentException("Unable to create connection", IAgentErrors.ERROR_CANNOT_CONNECT);
			}
		}
		log("[connect] connection: " + connection);
	}

	public void closeConnection() throws IAgentException {
		log("[closeConnection] >>> Closing DeviceConnector...");
		synchronized (lock) {
			if (!isActive) {
				log("[closeConnection] Already closed.");
				return;
			}
			isActive = false;
		}
		try {
			if (deploymentCommands != null)
				deploymentCommands.removeListeners();
			if (serviceManager != null)
				serviceManager.removeListeners();
			if (connectionManager != null)
				((ConnectionManagerImpl) connectionManager).removeListeners();
			log("[closeConnection] Closing underlying connections...");
			connectionManager.closeConnections();
			log("[closeConnection] DeviceConnector closed successfully");
		} catch (Throwable t) {
			IAgentLog.error("[DeviceConnectorImpl][closeConnection] Failed to close underlying connections", t);
		}
		fireConnectionEvent(DISCONNECTED, this);
	}

	public VMManager getVMManager() throws IAgentException {
		synchronized (lock) {
			if (!isActive) {
				log("[getVMManager] Request for VMManager received, but DeviceConnector is closed");
				throw new IAgentException("The connection is closed", IAgentErrors.ERROR_DISCONNECTED);
			}
			if (runtimeCommands == null) {
				runtimeCommands = new VMManagerImpl(this);
			}
		}
		return runtimeCommands;
	}

	public DeploymentManager getDeploymentManager() throws IAgentException {
		synchronized (lock) {
			if (!isActive) {
				log("[getDeploymentManager] Request for DeploymentManager received, but DeviceConnector is closed");
				throw new IAgentException("The connection is closed", IAgentErrors.ERROR_DISCONNECTED);
			}
			if (deploymentCommands == null) {
				deploymentCommands = new DeploymentManagerImpl(this);
			}
		}
		return deploymentCommands;
	}

	public boolean isActive() {
		return isActive;
	}

	public ConnectionManager getConnectionManager() throws IAgentException {
		return connectionManager;
	}

	public ServiceManager getServiceManager() throws IAgentException {
		synchronized (lock) {
			if (!isActive) {
				log("[getServiceManager] Request for ServiceManager received, but DeviceConnector is closed");
				throw new IAgentException("Connection to target device has been closed.",
					IAgentErrors.ERROR_DISCONNECTED);
			}
			if (serviceManager == null) {
				serviceManager = new ServiceManagerImpl(this);
			}
		}
		return serviceManager;
	}

	public Dictionary getProperties() {
		return connectionProperties;
	}

	public Map getDeviceProperties() {
		return propRegistry.getDeviceProperties();
	}

	private final void log(String message) {
		log(message, null);
	}

	private final void log(String message, Throwable e) {
		DebugUtils.log(this, message, e);
	}

	void fireDevicePropertyEvent(int type, boolean state) {
		log("[fireDevicePropertyEvent] >>> type: " + type);
		RemoteDevicePropertyListener[] listeners;
		synchronized (devicePropertyListeners) {
			if (devicePropertyListeners.size() != 0) {
				listeners = (RemoteDevicePropertyListener[]) devicePropertyListeners.toArray(new RemoteDevicePropertyListener[devicePropertyListeners.size()]);
			} else {
				return;
			}
		}
		RemoteDevicePropertyEvent event = new RemoteDevicePropertyEvent(type, state);
		log("[fireRemoteDevicePropertyEvent] " + listeners.length + " listeners found.");
		for (int i = 0; i < listeners.length; i++) {
			RemoteDevicePropertyListener listener = listeners[i];
			try {
				log("[fireRemoteDevicePropertyEvent] deliver event: " + event + " to listener: " + listener);
				listener.devicePropertiesChanged(event);
			} catch (Throwable e) {
				log("[fireRemoteDevicePropertyEvent] Failed to deliver event to " + listener, e);
			}
		}
	}

	public void addRemoteDevicePropertyListener(RemoteDevicePropertyListener listener) throws IAgentException {
		log("[addRemoteDevicePropertyListener] >>> listener: " + listener);
		synchronized (devicePropertyListeners) {
			if (!devicePropertyListeners.contains(listener)) {
				PMPConnection connection = deploymentCommands.getConnection(false);
				if (connection != null) {
					log("[addRemoteDevicePropertyListener] PMP connection is available, add event listener");
					connection.addEventListener(this, new String[] { DEVICE_PROPERTY_EVENT });
				}
				devicePropertyListeners.add(listener);
			} else {
				log("[addRemoteDevicePropertyListener] Listener already present");
			}
		}
	}

	final static String[] propertyMap = { RemoteDeploymentAdmin.class.getName(),
		EventAdmin.class.getName(),
		RemoteApplicationAdmin.class.getName(),
		RemoteConsole.class.getName() };

	public void event(Object event, String evType) {
		try {
			log("[event] >>> event: " + event + "; type: " + evType);
			if (DEVICE_PROPERTY_EVENT.equals(evType)) {
				Dictionary eventProps = (Dictionary) event;
				String[] eventObjectClasses = (String[]) eventProps.get(EVENT_OBJECT_CLASS);
				boolean state = ((Boolean) eventProps.get(EVENT_STATE_KEY)).booleanValue();
				for (int i = 0; i < propertyMap.length; i++) {
					if (propertyMap[i].equals(eventObjectClasses[0])) {
						fireDevicePropertyEvent(i, state);
						break;
					}
				}
			}
		} catch (Throwable e) {
			IAgentLog.error("[DeploymentManagerImpl][event] Failed to process PMP event: "
							+ event
							+ "; type: "
							+ evType);
		}
	}

	public void removeRemoteDevicePropertyListener(RemoteDevicePropertyListener listener) throws IAgentException {
		log("[removeRemoteDevicePropertyListener] >>> listener: " + listener);
		synchronized (devicePropertyListeners) {
			if (devicePropertyListeners.contains(listener)) {
				devicePropertyListeners.remove(listener);
				if (devicePropertyListeners.size() == 0) {
					log("[removeRemoteDevicePropertyListener] No more listeners in the list, try to remove PMP event listener");
					PMPConnection connection = deploymentCommands.getConnection(false);
					if (connection != null) {
						log("[removeRemoteDevicePropertyListener] PMP connection is available, remove event listener");
						connection.removeEventListener(this, new String[] { DEVICE_PROPERTY_EVENT });
					}
				}
			} else {
				log("[removeRemoteDevicePropertyListener] Listener not found in the list");
			}
		}
	}

}
