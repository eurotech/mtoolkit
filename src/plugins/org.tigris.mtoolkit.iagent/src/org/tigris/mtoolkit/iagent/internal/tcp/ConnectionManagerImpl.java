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

import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;

import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.spi.AbstractConnection;
import org.tigris.mtoolkit.iagent.spi.ConnectionEvent;
import org.tigris.mtoolkit.iagent.spi.ConnectionListener;
import org.tigris.mtoolkit.iagent.spi.ConnectionManager;
import org.tigris.mtoolkit.iagent.util.LightServiceRegistry;

public class ConnectionManagerImpl implements ConnectionManager {
	protected Dictionary conProperties;
	protected MBSAConnectionImpl mbsaConnection;
	protected PMPConnectionImpl pmpConnection;
	private List listeners = new LinkedList();

	public ConnectionManagerImpl(Dictionary aConProperties) {
		this.conProperties = aConProperties;
	}

	public AbstractConnection createConnection(int type) throws IAgentException {
		log("[createConnection] >>> type: " + type);
		AbstractConnection connection = null;
		AbstractConnection staleConnection = null;
		boolean fireEvent = false;

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
			switch (type) {
			case MBSA_CONNECTION:
		        if (mbsaConnection != null && !mbsaConnection.isConnected()) {
		          staleConnection = mbsaConnection;
		          mbsaConnection = null;
		        }
		        break;
			case PMP_CONNECTION:
				if (pmpConnection != null && !pmpConnection.isConnected()) {
					staleConnection = pmpConnection;
					pmpConnection = null;
				}
				break;
			default:
				log("[createConnection] Unknown connection type passed: " + type);
				throw new IllegalArgumentException("Unknown connection type passed: " + type);
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
			switch (type) {
			case MBSA_CONNECTION:
		        if (mbsaConnection == null) {
		          mbsaConnection = createMBSAConnection();
		          fireEvent = true;
		        }
		        connection = mbsaConnection;
		        break;
			case PMP_CONNECTION:
				if (pmpConnection == null) {
					pmpConnection = createPMPConnection();
					fireEvent = true;
				}
				connection = pmpConnection;
				break;
			}
		}
		// If new connection was created, fire an event
		if (fireEvent) {
			fireConnectionEvent(ConnectionEvent.CONNECTED, connection);
			log("[createConnection] Finished sending events");
		}
		log("[createConnection] connection: " + connection);
		return connection;
	}
	
	private MBSAConnectionImpl createMBSAConnection() throws IAgentException {
	    MBSAConnectionImpl connection = new MBSAConnectionImpl(conProperties, this);
	    log("[createMBSAConnection] Created connection: " + connection);
	    return connection;
	  }

	private PMPConnectionImpl createPMPConnection() throws IAgentException {
		final PMPConnectionImpl connection = new PMPConnectionImpl(conProperties, this);
		log("[createPMPConnection] Created connection: " + connection);
		return connection;
	}

	public synchronized AbstractConnection getActiveConnection(int type) {
		log("[getActiveConnection] >>> type: " + type);
		AbstractConnection connection = null;
		switch (type) {
		case MBSA_CONNECTION:
		    if (mbsaConnection != null && mbsaConnection.isConnected())
		        connection = mbsaConnection;
		    break;
		case PMP_CONNECTION:
			if (pmpConnection != null && pmpConnection.isConnected())
				connection = pmpConnection;
			break;
		}
		log("[getActiveConnection] connection: " + connection);
		return connection;
	}

	public void closeConnections() throws IAgentException {
		log("[closeConnections] >>>");
		// only call closeConnection() because it will result in
		// connectionClosed()
		AbstractConnection mbsaConnection = this.mbsaConnection; 
	    if ( mbsaConnection != null ){
	      mbsaConnection.closeConnection();
	    }
		AbstractConnection pmpConnection = this.pmpConnection;
		if (pmpConnection != null) {
			pmpConnection.closeConnection();
		}
	}

	protected void finalize() throws Throwable {
		closeConnections();
	}

	public void addConnectionListener(ConnectionListener listener) {
		log("[addConnectionListener]  >>> listener: " + listener);
		synchronized (listeners) {
			if (!listeners.contains(listener)) {
				listeners.add(listener);
			} else {
				log("[addConnectionListener] listener already have been added");
			}
		}
	}

	public void removeConnectionListener(ConnectionListener listener) {
		log("[removeConnectionListener] >>> listener: " + listener);
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	private void fireConnectionEvent(int type, AbstractConnection connection) {
		log("[fireConnectionEvent] >>> type=" + type + ";connection=" + connection);
		ConnectionListener[] clonedListeners;
		synchronized (listeners) {
			if (listeners.size() == 0) {
				log("[fireConnectionEvent] There were no listeners");
				return;
			}
			clonedListeners = (ConnectionListener[]) listeners.toArray(new ConnectionListener[listeners.size()]);
		}

		ConnectionEvent event = new ConnectionEvent(type, connection);
		log("[fireConnectionEvent] Sending event: " + event + " to " + clonedListeners.length + " connection listeners");
		for (int i = 0; i < clonedListeners.length; i++) {
			ConnectionListener listener = clonedListeners[i];
			try {
				log("[fireConnectionEvent] Sending event to " + listener);
				listener.connectionChanged(event);
			} catch (Throwable e) {
				log("[fireConnectionEvent] Failed to deliver event to " + listener, e);
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
	void connectionClosed(AbstractConnection connection) {
		log("[connectionClosed] >>> connection: " + connection);
		boolean sendEvent = false;
		synchronized (this) {
			if (connection == null)
				return;
			if (mbsaConnection == connection) {
		        log("[connectionClosed] Active MBSA connection match");
		        mbsaConnection = null;
		        sendEvent = true;
			} else if (pmpConnection == connection) {
				log("[connectionClosed] Active PMP connection match");
				pmpConnection = null;
				sendEvent = true;
			}
		}
		if (sendEvent)
			fireConnectionEvent(ConnectionEvent.DISCONNECTED, connection);
	}

	public void removeListeners() throws IAgentException {
		log("[removeListeners] >>>");
		synchronized (listeners) {
			listeners.clear();
		}
	}

	private final void log(String message) {
		log(message, null);
	}

	private final void log(String message, Throwable e) {
		DebugUtils.log(this, message, e);
	}
}
