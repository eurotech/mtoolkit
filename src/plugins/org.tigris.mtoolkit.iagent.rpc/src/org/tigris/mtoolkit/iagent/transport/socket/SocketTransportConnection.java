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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import org.tigris.mtoolkit.iagent.transport.TransportConnection;

/**
 * @since 3.0
 */
public final class SocketTransportConnection implements TransportConnection {
  private static final String     IAGENT_LOCAL_ADDRESS_PROP = "iagent.pmp.local.address";

  private static volatile boolean preJava14EE               = false;

  private final String            host;
  private final int               port;

  private final Socket            socket;
  private volatile boolean        closed;

  public SocketTransportConnection(String host, int port, int timeout) throws IOException {
    this.host = host;
    this.port = port;
    String iagentLocalAddr = System.getProperty(IAGENT_LOCAL_ADDRESS_PROP);
    if (iagentLocalAddr != null) {
      socket = new Socket(host, port, InetAddress.getByName(iagentLocalAddr), 0);
    } else {
      socket = new Socket(host, port);
    }
    try {
      socket.setKeepAlive(true);
    } catch (SocketException e) {
      // problem setting keepalive shouldn't affect the connection
    }
    if (timeout > 0) {
      socket.setSoTimeout(timeout);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.tigris.mtoolkit.iagent.transport.TransportConnection#getInputStream()
   */
  public InputStream getInputStream() throws IOException {
    return socket.getInputStream();
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.tigris.mtoolkit.iagent.transport.TransportConnection#getOutputStream
   * ()
   */
  public OutputStream getOutputStream() throws IOException {
    return socket.getOutputStream();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.tigris.mtoolkit.iagent.transport.TransportConnection#close()
   */
  public synchronized void close() {
    if (closed) {
      return;
    }
    try {
      socket.close();
    } catch (IOException ioe) {
      // nothing to do
    } finally {
      closed = true;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.tigris.mtoolkit.iagent.transport.TransportConnection#isClosed()
   */
  public synchronized boolean isClosed() {
    if (closed) {
      return true;
    }
    return isSocketClosed(socket);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "SocketTransportConnection: " + host + ":" + port;
  }

  private static boolean isSocketClosed(Socket socket) {
    if (preJava14EE) {
      return false;
    }
    try {
      Method m = socket.getClass().getMethod("isClosed", (Class[]) null);
      return ((Boolean) m.invoke(socket, null)).booleanValue();
    } catch (NoSuchMethodException e) {
      preJava14EE = true;
    } catch (InvocationTargetException e) {
    } catch (IllegalAccessException e) {
    }
    return false;
  }
}
