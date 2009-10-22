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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.tigris.mtoolkit.iagent.transport.TransportConnection;

public class SocketTransportConnection implements TransportConnection {
	private String host;
	private int port;
	private Socket socket;
	private volatile boolean closed;

	public SocketTransportConnection(String host, int port, int timeout) throws IOException {
		this.host = host;
		this.port = port;
		socket = new Socket(host, port);
		if (timeout > 0)
			socket.setSoTimeout(timeout);
	}

	public void close() {
		closed = true;
		try {
			socket.close();
		} catch (IOException ioe) {
			// nothing to do
		}
	}

	public InputStream getInputStream() throws IOException {
		return socket.getInputStream();
	}

	public OutputStream getOutputStream() throws IOException {
		return socket.getOutputStream();
	}

	public boolean isClosed() {
		// TODO
		//return socket.isClosed();
		if (closed)
			return true;
		return false;
	}

	public String toString() {
		return "SocketTransportConnection: " + host + ":" + port;
	}
}
