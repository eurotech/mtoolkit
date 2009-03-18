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
package org.tigris.mtoolkit.iagent;

import java.util.Dictionary;


/**
 * Provides factory mechanism for {@link DeviceConnector}s. Each factory provides
 * {@link DeviceConnector}s for specified connection type.
 */
public interface DeviceConnectorFactory {
  
  /**
   * Returns the type of the connection over which {@link DeviceConnector}s provided from this
   * factory are working ({@link DeviceConnector#TYPE_TCP})
   * 
   * @return the type of the connection this factory is providing
   */
  public int getConnectionType();
  
  /**
   * Creates {@link DeviceConnector} working over specified client connection to specified remote OSGi framework
   * 
   * @param aConProps the connection properties (host, port, etc.)
   * @return {@link DeviceConnector} connected to specified remote OSGi framework
   * @throws IAgentException thrown if connection could not be established
   * @see DeviceConnector#openClientConnection(int, Dictionary)
   */
  public DeviceConnector createClientConnection(Dictionary aConProps) throws IAgentException;
  
  /**
   * Opens specified server connection and blocks until connection from remote OSGi framework is accepted or specified timeout
   * is passed.
   * 
   * @param aConProps the connection properties (port, connection timeout, etc.)
   * @return {@link DeviceConnector} connected to specified remote OSGi framework
   * @throws IAgentException thrown if connection could not be established
   */
  public DeviceConnector createServerConnection(Dictionary aConProps) throws IAgentException;
  
}
