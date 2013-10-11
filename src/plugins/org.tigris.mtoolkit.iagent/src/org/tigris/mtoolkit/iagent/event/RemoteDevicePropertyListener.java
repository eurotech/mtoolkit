/*******************************************************************************
 * Copyright (c) 2005, 2012 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 ****************************************************************************/
package org.tigris.mtoolkit.iagent.event;

import java.util.EventListener;

import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;

/**
 * Clients interested in device properties change events must implement this
 * interface. The listeners must be added to the listeners list via
 * {@link DeviceConnector#addRemoteDevicePropertyListener(RemoteDevicePropertyListener)
 * method.
 */
public interface RemoteDevicePropertyListener extends EventListener {

	/**
	 * Sent when remote device properties are changed in some way (no more
	 * console is available, or eventAdmin service is registered/unregistered,
	 * etc.).
	 *
	 * @param event
	 *            an event object containing details
	 * @throws IAgentException
	 */
	public void devicePropertiesChanged(RemoteDevicePropertyEvent e) throws IAgentException;
}
