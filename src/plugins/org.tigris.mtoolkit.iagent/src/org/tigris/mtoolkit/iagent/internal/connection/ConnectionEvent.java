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
package org.tigris.mtoolkit.iagent.internal.connection;


/**
 * Event, fired when change in the state of internal connection occur
 * 
 */
public class ConnectionEvent {

  /**
   * Event type, indicating a connection was established.
   */
  public final static int CONNECTED = 1;
  
  /**
   * Event type, indicating a connection was closed. It doesn't specify which
   * end initiated the disconnection.
   */
  public final static int DISCONNECTED = 2;
  
  private int type;
  
  private AbstractConnection connection;
  
  public ConnectionEvent(int type, AbstractConnection connection) {
    this.type = type;
    this.connection = connection;
  }

  /**
   * Returns the type of the event
   * 
   * @return
   */
  public int getType() {
    return type;
  }

  /**
   * Returns the associated connection
   * 
   * @return
   */
  public AbstractConnection getConnection() {
    return connection;
  }

  public String toString() {
    return "ConnectionEvent[type=" + convertType(type) + "; connection=" + connection + "]";
  }
  
  private String convertType(int type) {
    switch (type) {
    case CONNECTED:
      return "CONNECTED";
    case DISCONNECTED:
      return "DISCONNECTED";
    default:
      return "UNKNOWN(" + type + ")";
    }
  }
  
}
