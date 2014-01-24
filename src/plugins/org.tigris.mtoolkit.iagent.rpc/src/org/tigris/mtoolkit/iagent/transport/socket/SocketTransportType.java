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

import java.util.List;

import org.tigris.mtoolkit.iagent.transport.Transport;
import org.tigris.mtoolkit.iagent.transport.TransportType;

/**
 * @since 3.0
 */
public final class SocketTransportType implements TransportType {
	private static final String TYPE_ID = "socket"; //$NON-NLS-1$

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.transport.TransportType#listAvailable()
   */
	public List listAvailable() {
		return null;
	}

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.transport.TransportType#openTransport(java.lang.String)
   */
	public Transport openTransport(String id) {
		return new SocketTransport(this, id);
	}

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.transport.TransportType#getTypeId()
   */
	public String getTypeId() {
		return TYPE_ID;
	}
}
