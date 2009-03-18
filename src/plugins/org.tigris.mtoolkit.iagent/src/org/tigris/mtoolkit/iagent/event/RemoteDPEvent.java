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

import org.tigris.mtoolkit.iagent.RemoteDP;


/**
 * Event object containing details about remote deployment package event.
 * 
 * @author Danail Nachev
 * 
 */
public class RemoteDPEvent extends RemoteEvent {

  /**
   * Constant indicating that a deployment package has been installed or
   * updated.
   */
  public static final int INSTALLED = 1 << 0;
  
  /**
   * Constant indicating that a deployment package has been uninstalled.
   */
  public static final int UNINSTALLED = 1 << 1;

  private RemoteDP dp;

  public RemoteDPEvent(RemoteDP dp, int type) {
    super(type);
    this.dp = dp;
  }

  /**
   * Returns the deployment package which has been changed in some way.
   * 
   * @return a {@link RemoteDP} object associated with this event
   */
  public RemoteDP getDeploymentPackage() {
    return dp;
  }

  public String toString() {
    return "RemoteDPEvent[dp=" + dp + ";type=" + convertType(getType()) + "]";
  }

  private String convertType(int type) {
    switch (type) {
    case INSTALLED:
      return "INSTALLED";
    case UNINSTALLED:
      return "UNINSTALLED";
    default:
      return "UNKNOWN(" + type + ")";
    }
  }

}
