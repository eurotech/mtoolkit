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
package org.tigris.mtoolkit.iagent.transport.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import org.tigris.mtoolkit.iagent.transport.TransportConnection;

/**
 * @since 3.0
 */
public class SocketTransportConnection implements TransportConnection {
	private String host;
	private int port;
	private Socket socket;
	private volatile boolean closed;
	private static boolean preJava14EE = false;

	public SocketTransportConnection(String host, int port, int timeout) throws IOException {
		this.host = host;
		this.port = port;
		socket = new Socket(host, port);
		try {
			setKeepAlive(socket, true);
		} catch (SocketException e) {
			// problem setting keepalive shouldn't affect the connection
		}
		if (timeout > 0)
			socket.setSoTimeout(timeout);
	}
	
	private void setKeepAlive(Socket socket, boolean keepAlive) throws SocketException {
		if (preJava14EE)
			return;
		try {
			socket.setKeepAlive(keepAlive);
		} catch (NoSuchMethodError e) {
			preJava14EE = false;
		}
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
		if (closed)
			return true;
		return isSocketClosed(socket);
	}
	
	private static boolean isSocketClosed(Socket socket) {
		if (preJava14EE)
			return false;
		try {
			return socket.isClosed();
		} catch (NoSuchMethodError e) {
			preJava14EE = true;
			return false;
		}
	}

	public String toString() {
		return "SocketTransportConnection: " + host + ":" + port;
	}
}
