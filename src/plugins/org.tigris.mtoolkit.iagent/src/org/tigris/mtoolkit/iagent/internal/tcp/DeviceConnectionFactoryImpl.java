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
import org.tigris.mtoolkit.iagent.IAProgressMonitor;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.internal.DeviceConnectorImpl;
import org.tigris.mtoolkit.iagent.transport.Transport;

/**
 * Device connection factory providing DeviceConnectors working over TCP
 * transport
 *
 */
public final class DeviceConnectionFactoryImpl implements DeviceConnectorFactory { // NO_UCD
  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeviceConnectorFactory#createClientConnection(org.tigris.mtoolkit.iagent.transport.Transport, java.util.Dictionary, org.tigris.mtoolkit.iagent.IAProgressMonitor)
   */
  public DeviceConnector createClientConnection(Transport transport, Dictionary aConProps, IAProgressMonitor monitor)
      throws IAgentException {
    return new DeviceConnectorImpl(transport, aConProps, monitor);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeviceConnectorFactory#getConnectionType()
   */
  public int getConnectionType() {
    return DeviceConnector.TYPE_TCP;
  }
}
