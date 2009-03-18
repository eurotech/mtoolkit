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

import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.DeviceConnectorFactory;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.internal.DeviceConnectorImpl;


/**
 * Device connection factory providing DeviceConnectors working over TCP transport 
 *
 */
public class DeviceConnectionFactoryImpl implements DeviceConnectorFactory {

  public DeviceConnector createClientConnection(Dictionary aConProps) throws IAgentException {
    return new DeviceConnectorImpl(aConProps);
  }

  public DeviceConnector createServerConnection(Dictionary aConProps) throws IAgentException {
    throw new UnsupportedOperationException();
  }

  public int getConnectionType() {
    return DeviceConnector.TYPE_TCP;
  }
}
