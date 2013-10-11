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
package org.tigris.mtoolkit.iagent.event;

import org.tigris.mtoolkit.iagent.RemoteService;

/**
 * Event object containing details about remote service event.
 *
 * @author Danail Nachev
 * @see RemoteServiceListener
 */
public final class RemoteServiceEvent extends RemoteEvent {
  /**
   * Constant indicating that a service has been registered.
   */
  public static final int REGISTERED   = 1 << 0;

  /**
   * Constant indicating that a service has been modified.
   */
  public static final int MODIFIED     = 1 << 1;

  /**
   * Constant indicating that a service has been unregistered.
   */
  public static final int UNREGISTERED = 1 << 2;

  private final RemoteService service;

  public RemoteServiceEvent(RemoteService service, int type) {
    super(type, service);
    this.service = service;
  }

  /**
   * Returns {@link RemoteService} object associated with this event
   *
   * @return
   */
  public RemoteService getService() {
    return service;
  }

  /* (non-Javadoc)
   * @see java.util.EventObject#toString()
   */
  public String toString() {
    return "RemoteServiceEvent[service=" + service + ";type=" + convertType(getType()) + "]";
  }

  private String convertType(int type) {
    switch (type) {
    case REGISTERED:
      return "REGISTERED";
    case MODIFIED:
      return "MODIFIED";
    case UNREGISTERED:
      return "UNREGISTERED";
    default:
      return "UNKNOWN(" + type + ")";
    }
  }

}
