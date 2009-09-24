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

import java.util.List;

import org.tigris.mtoolkit.iagent.transport.Transport;
import org.tigris.mtoolkit.iagent.transport.TransportType;

public class SocketTransportType implements TransportType {

	private static final String TYPE_ID = "socket";

	public List listAvailable() {
		return null;
	}

	public Transport openTransport(String id) {
		return new SocketTransport(this, id);
	}

	public String getTypeId() {
		return TYPE_ID;
	}
}
