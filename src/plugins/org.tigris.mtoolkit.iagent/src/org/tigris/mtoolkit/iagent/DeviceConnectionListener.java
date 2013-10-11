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

import java.util.EventListener;

/**
 * Provides disconnection listener interface for the {@link DeviceConnector}'s
 * associated connection
 *
 * @version 1.0
 */
public interface DeviceConnectionListener extends EventListener {
  /**
   * Called when the {@link DeviceConnector}'s associated connection is
   * connected/disconnected.
   */
  public void deviceConnectionEvent(DeviceConnectionEvent event);
}
