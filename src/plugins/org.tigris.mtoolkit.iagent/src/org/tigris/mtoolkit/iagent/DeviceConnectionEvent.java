/*******************************************************************************
 * Copyright (c) 2005, 2013 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.iagent;

import java.util.EventObject;

public final class DeviceConnectionEvent extends EventObject {
  public static final int       CONNECTED    = 1 << 0;
  public static final int       DISCONNECTED = 1 << 1;

  private final int             type;
  private final DeviceConnector connector;

  /**
   * @param source
   */
  public DeviceConnectionEvent(int type, DeviceConnector connector) {
    super(connector);
    this.connector = connector;
    this.type = type;
  }

  public int getType() {
    return type;
  }

  public DeviceConnector getConnector() {
    return connector;
  }
}
