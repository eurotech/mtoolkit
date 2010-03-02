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
package org.tigris.mtoolkit.iagent.internal.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.spi.AbstractConnection;
import org.tigris.mtoolkit.iagent.spi.ConnectionEvent;
import org.tigris.mtoolkit.iagent.spi.ConnectionListener;
import org.tigris.mtoolkit.iagent.spi.ConnectionManager;
import org.tigris.mtoolkit.iagent.spi.ExtConnectionFactory;
import org.tigris.mtoolkit.iagent.transport.Transport;
import org.tigris.mtoolkit.iagent.util.LightServiceRegistry;

public class ConnectionManagerImpl implements ConnectionManager {
	protected Dictionary conProperties;
	protected Transport transport;
	private List listeners = new LinkedList();
	private List extFactories = new LinkedList();
	private Map connections = new Hashtable();
	private int connectedType;

	public ConnectionManagerImpl(Transport transport, Dictionary aConProperties) {
		this.transport = transport;
		this.conProperties = aConProperties;
		LightServiceRegistry registry = new LightServiceRegistry(ConnectionManagerImpl.class.getClassLoader());
		Object[] extenders = registry.getAll(ExtConnectionFactory.class.getName());
		for (int i = 0; i < extenders.length; i++) {
			if (extenders[i] instanceof ExtConnectionFactory) {
				extFactories.add(extenders[i]);
			}
		}
	}

	public AbstractConnection createConnection(int type) throws IAgentException {
		debug("[createConnection] >>> type: " + type);
		AbstractConnection connection = null;
		AbstractConnection staleConnection = null;
		boolean fireEvent = false;
		Integer key = new Integer(type);

		// Connection creation is done in two steps. This have the side effect
		// that DISCONNECT event
		// can be delivered after CONNECT event.

		// Step 1: check whether there is a stale connection is closed/closing
		// If there is a closed/closing connection, save the reference and clear
		// the active connection reference
		// This gives us two things: 1. event won't be fired from the
		// connectionClosed() method
		// 2. saved reference will be used when firing the event
		synchronized (this) {
			connection = (AbstractConnection) connections.get(key);
			if (connection != null && !connection.isConnected()) {
				staleConnection = connection;
				connections.remove(key);
			}
		}
		// If stale connection is detected, try to close it (just in case) and
		// fire an event for the disconnection
		if (staleConnection != null) {
			staleConnection.closeConnection();
			fireConnectionEvent(ConnectionEvent.DISCONNECTED, staleConnection);
		}
		// Step 2: check whether there is an active connection. Create if
		// necessary.
		// It is very unlikely to get stale connection here, because we have
		// already checked it in Step 1
		synchronized (this) {
			connection = (AbstractConnection) connections.get(key);
			if (connection == null) {
				fireEvent = true;
				switch (type) {
				case MBSA_CONNECTION:
					connection = createMBSAConnection(transport);
					break;
				case PMP_CONNECTION:
					AbstractConnection activeConnection = getActiveConnection(connectedType);
					Integer pmpPort = (Integer) activeConnection.getProperty("pmp.port");
					conProperties.put("pmp-port", pmpPort);
					connection = createPMPConnection(transport);
					break;
				default:
					ExtConnectionFactory factory = findFactoryForType(type);
					if (factory == null) {
						info("[createConnection] Unknown connection type passed: " + type);
						throw new IllegalArgumentException("Unknown connection type passed: " + type);
					}
					connection = factory.createConnection(transport, conProperties, this);
					connectedType = connection.getType();
				}
				connections.put(key, connection);
			}
		}
		// If new connection was created, fire an event
		if (fireEvent) {
			fireConnectionEvent(ConnectionEvent.CONNECTED, connection);
			debug("[createConnection] Finished sending events");
		}
		debug("[createConnection] connection: " + connection);
		return connection;
	}

	private MBSAConnectionImpl createMBSAConnection(Transport transport) throws IAgentException {
		MBSAConnectionImpl connection = new MBSAConnectionImpl(transport, conProperties, this);
		debug("[createMBSAConnection] Created connection: " + connection);
		return connection;
	}

	private PMPConnectionImpl createPMPConnection(Transport transport) throws IAgentException {
		final PMPConnectionImpl connection = new PMPConnectionImpl(transport, conProperties, this);
		debug("[createPMPConnection] Created connection: " + connection);
		return connection;
	}

	public synchronized AbstractConnection getActiveConnection(int type) {
		debug("[getActiveConnection] >>> type: " + type);
		AbstractConnection connection = (AbstractConnection) connections.get(new Integer(type));
		if (connection != null && !connection.isConnected()) {
			connection = null;
		}
		debug("[getActiveConnection] connection: " + connection);
		return connection;
	}

	public void closeConnections() throws IAgentException {
		debug("[closeConnections] >>>");
		// only call closeConnection() because it will result in
		// connectionClosed()

		ArrayList tmpConnections = new ArrayList();
		synchronized (this) {
			tmpConnections.addAll(connections.values());
		}
		Iterator it = tmpConnections.iterator();
		while (it.hasNext()) {
			AbstractConnection connection = (AbstractConnection) it.next();
			connection.closeConnection();
		}
	}

	protected void finalize() throws Throwable {
		closeConnections();
	}

	public void addConnectionListener(ConnectionListener listener) {
		debug("[addConnectionListener]  >>> listener: " + listener);
		synchronized (listeners) {
			if (!listeners.contains(listener)) {
				listeners.add(listener);
			} else {
				debug("[addConnectionListener] listener already have been added");
			}
		}
	}

	public void removeConnectionListener(ConnectionListener listener) {
		debug("[removeConnectionListener] >>> listener: " + listener);
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	private void fireConnectionEvent(int type, AbstractConnection connection) {
		debug("[fireConnectionEvent] >>> type=" + type + ";connection=" + connection);
		ConnectionListener[] clonedListeners;
		synchronized (listeners) {
			if (listeners.size() == 0) {
				debug("[fireConnectionEvent] There were no listeners");
				return;
			}
			clonedListeners = (ConnectionListener[]) listeners.toArray(new ConnectionListener[listeners.size()]);
		}

		ConnectionEvent event = new ConnectionEvent(type, connection);
		debug("[fireConnectionEvent] Sending event: " + event + " to " + clonedListeners.length
				+ " connection listeners");
		for (int i = 0; i < clonedListeners.length; i++) {
			ConnectionListener listener = clonedListeners[i];
			try {
				debug("[fireConnectionEvent] Sending event to " + listener);
				listener.connectionChanged(event);
			} catch (Throwable e) {
				error("[fireConnectionEvent] Failed to deliver event to " + listener, e);
			}
		}
	}

	/**
	 * This method should be called by
	 * {@link AbstractConnection#closeConnection()} method every time it is
	 * called, passing itself as argument for the method.
	 * 
	 * @param connection
	 */
	public void connectionClosed(AbstractConnection connection, boolean notify) {
		debug("[connectionClosed] >>> connection: " + connection);
		boolean sendEvent = false;
		synchronized (this) {
			if (connection == null)
				return;
			Integer key = new Integer(connection.getType());
			AbstractConnection currentConnection = (AbstractConnection) connections.get(key);
			if (currentConnection == connection) {
				debug("[connectionClosed] Active connection match, connection type: " + connection.getType());
				connections.remove(key);
				sendEvent = true;
			}
		}
		if (sendEvent && notify)
			fireConnectionEvent(ConnectionEvent.DISCONNECTED, connection);
	}
	
	public void connectionClosed(AbstractConnection connection) {
		connectionClosed(connection, true);
	}

	public void removeListeners() throws IAgentException {
		debug("[removeListeners] >>>");
		synchronized (listeners) {
			listeners.clear();
		}
	}

	private final void debug(String message) {
		DebugUtils.debug(this, message);
	}

	private final void info(String message) {
		DebugUtils.info(this, message);
	}

	private final void error(String message, Throwable e) {
		DebugUtils.error(this, message, e);
	}

	/**
	 * Returns types of ext controller connections.
	 * 
	 * @return array of types or empty array
	 */
	public int[] getExtControllerConnectionTypes() {
		List types = new ArrayList();
		for (Iterator it = extFactories.iterator(); it.hasNext();) {
			ExtConnectionFactory factory = (ExtConnectionFactory) it.next();
			if (factory.isControllerType()) {
				types.add(new Integer(factory.getConnectionType()));
			}
		}
		int[] result = new int[types.size()];
		int i = 0;
		for (Iterator it = types.iterator(); it.hasNext();) {
			result[i++] = ((Integer) it.next()).intValue();
		}
		return result;
	}

	private ExtConnectionFactory findFactoryForType(int type) {
		for (Iterator it = extFactories.iterator(); it.hasNext();) {
			ExtConnectionFactory factory = (ExtConnectionFactory) it.next();
			if (factory.getConnectionType() == type) {
				return factory;
			}
		}
		return null;
	}
	
	  
	public Object getProperty(Object propertyName) {
		return conProperties.get(propertyName);
	}
}
