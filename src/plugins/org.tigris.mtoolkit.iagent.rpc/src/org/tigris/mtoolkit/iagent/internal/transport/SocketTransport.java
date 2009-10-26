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
package org.tigris.mtoolkit.iagent.internal.transport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.transport.Transport;
import org.tigris.mtoolkit.iagent.transport.TransportConnection;
import org.tigris.mtoolkit.iagent.transport.TransportType;

public class SocketTransport implements Transport {

	private static final int PMP_PORT = 1450;

	private String host;
	private SocketTransportType type;
	private List connections = new ArrayList();
	private volatile boolean closed;

	public SocketTransport(SocketTransportType type, String host) {
		this.host = host;
		this.type = type;
	}

	public TransportConnection createConnection(int port) throws IOException {
		if (closed)
			throw new IOException("Transport is closed");
		int timeout = 0; // by default, no timeout
		// if we have PMP connection, we need a timeout
		// because PMP have own ping mechanism
		if (port == PMP_PORT)
			timeout = 1000;
		SocketTransportConnection connection = new SocketTransportConnection(host, port, timeout);
			synchronized (connections) {
				connections.add(connection);
			}
		return connection;
	}
	
	public String toString() {
		return "Socket Transport: " + host;
	}

	public void dispose() {
		closed = true;
		SocketTransportConnection[] establishedConnections;
		synchronized (connections) {
			establishedConnections = (SocketTransportConnection[]) connections.toArray(new SocketTransportConnection[connections.size()]);
		}
		for (int i = 0; i < establishedConnections.length; i++) {
			try {
				establishedConnections[i].close();
			} catch (Throwable e) {
				DebugUtils.log(SocketTransport.class, "Failed to correctly close an connection: " + establishedConnections[i], e);
			}
		}
	}

	public String getId() {
		return host;
	}

	public TransportType getType() {
		return type;
	}

	public boolean isDisposed() {
		return closed;
	}
}
