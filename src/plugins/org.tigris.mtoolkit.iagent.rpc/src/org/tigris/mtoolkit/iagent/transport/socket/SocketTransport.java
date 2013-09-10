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

import org.tigris.mtoolkit.iagent.transport.Transport;
import org.tigris.mtoolkit.iagent.transport.TransportConnection;
import org.tigris.mtoolkit.iagent.transport.TransportType;

/**
 * @since 3.0
 */
public final class SocketTransport implements Transport {
  private final SocketTransportType type;
  private final String              host;

  public SocketTransport(SocketTransportType type, String host) {
    this.host = host;
    this.type = type;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.transport.Transport#createConnection(int)
   */
  public TransportConnection createConnection(int port) throws IOException {
    return new SocketTransportConnection(host, port, 0);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.transport.Transport#getId()
   */
  public String getId() {
    return host;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.transport.Transport#getType()
   */
  public TransportType getType() {
    return type;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "Socket Transport: " + host;
  }

}
