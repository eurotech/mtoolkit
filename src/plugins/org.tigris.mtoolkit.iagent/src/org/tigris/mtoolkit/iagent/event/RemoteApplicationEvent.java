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

import org.tigris.mtoolkit.iagent.RemoteApplication;

public final class RemoteApplicationEvent extends RemoteEvent {
  /**
   * Constant indicating that an application has been installed.
   */
  public static final int         INSTALLED   = 1 << 0;

  /**
   * Constant indicating that an application has been started.
   */
  public static final int         STARTED     = 1 << 2;

  /**
   * Constant indicating that an application has been stopped.
   */
  public static final int         STOPPED     = 1 << 3;

  /**
   * Constant indicating that an application has been uninstalled.
   */
  public static final int         UNINSTALLED = 1 << 4;

  private final RemoteApplication remoteApplication;

  /**
   * Constructs a new remote application event object.
   *
   * @param application
   *          - the application that has been changed.
   * @param type
   *          -the type of Event {@link RemoteApplicationEvent#INSTALLED},
   *          {@link RemoteApplicationEvent#UNINSTALLED},
   *          {@link RemoteApplicationEvent#STARTED},
   *          {@link RemoteApplicationEvent#STOPPED}
   */
  public RemoteApplicationEvent(RemoteApplication application, int type) {
    super(type);
    this.remoteApplication = application;
  }

  /**
   * Returns a reference to the application which has been changed.
   *
   * @return a {@link RemoteApplication} object associated with this event
   */
  public RemoteApplication getApplication() {
    return remoteApplication;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "RemoteBundleEvent[bundle=" + remoteApplication + ";type=" + convertType(getType()) + "]";
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return super.hashCode();
  }

  private String convertType(int type) {
    switch (type) {
    case INSTALLED:
      return "INSTALLED";
    case STARTED:
      return "STARTED";
    case STOPPED:
      return "STOPPED";
    case UNINSTALLED:
      return "UNINSTALLED";
    default:
      return "UNKNOWN(" + type + ")";
    }
  }
}
