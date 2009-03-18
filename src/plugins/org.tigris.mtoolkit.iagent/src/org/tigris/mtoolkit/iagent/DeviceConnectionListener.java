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


/**
 * Provides disconnection listener interface for the {@link DeviceConnector}'s associated connection
 * 
 * @version 1.0
 */
public interface DeviceConnectionListener {
  
  /**
   * Called when the {@link DeviceConnector}'s associated connection is disconnected.
   */
  public void disconnected(DeviceConnector connector);

  /**
   * Called when new {@link DeviceConnector} is connected.
   * @param connector
   */
  public void connected(DeviceConnector connector);
  
}
