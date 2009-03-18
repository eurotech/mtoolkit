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

import org.osgi.framework.BundleEvent;
import org.tigris.mtoolkit.iagent.RemoteBundle;


/**
 * Event object containing details about remote bundle event.
 * 
 * @author Danail Nachev
 * 
 */
public class RemoteBundleEvent extends RemoteEvent {

  /**
   * Constant indicating that a bundle has been installed.
   */
  public static final int INSTALLED = BundleEvent.INSTALLED;

  /**
   * Constant indicating that a bundle has been started.
   */
  public static final int STARTED = BundleEvent.STARTED;

  /**
   * Constant indicating that a bundle has been stopped.
   */
  public static final int STOPPED = BundleEvent.STOPPED;

  /**
   * Constant indicating that a bundle has been updated.
   */
  public static final int UPDATED = BundleEvent.UPDATED;

  /**
   * Constant indicating that a bundle has been uninstalled.
   */
  public static final int UNINSTALLED = BundleEvent.UNINSTALLED;

  /**
   * Constant indicating that a bundle has been resolved.
   */
  public static final int RESOLVED = BundleEvent.RESOLVED;

  /**
   * Constant indicating that a bundle has been unresolved.
   */
  public static final int UNRESOLVED = BundleEvent.UNRESOLVED;

  /**
   * Constant indicating that a bundle has been lazy started.
   */
  public static final int LAZY_STARTED = BundleEvent.LAZY_ACTIVATION;


  /**
   * Constant indicating that a bundle has entered in starting phase. This state
   * usually is very short and it is discouraged to use it for something
   * different purpose than bundle state tracking.
   * 
   */
  public static final int STARTING = BundleEvent.STARTING;

  /**
   * Constant indicating that a bundle has entered in stopping phase. This state
   * usually is very short and it is discouraged to use it for something
   * different purpose than bundle state tracking.
   * 
   */
  public static final int STOPPING = BundleEvent.STOPPING;

  private RemoteBundle bundle;

  public RemoteBundleEvent(RemoteBundle bundle, int type) {
    super(type);
    this.bundle = bundle;
  }

  /**
   * Returns a reference to the bundle which has been changed.
   * 
   * @return a {@link RemoteBundle} object associated with this event
   */
  public RemoteBundle getBundle() {
    return bundle;
  }

  public String toString() {
    return "RemoteBundleEvent[bundle=" + bundle + ";type=" + convertType(getType()) + "]";
  }

  private String convertType(int type) {
    switch (type) {
    case INSTALLED:
      return "INSTALLED";
    case RESOLVED:
      return "RESOLVED";
    case STARTED:
      return "STARTED";
    case STOPPED:
      return "STOPPED";
    case UNINSTALLED:
      return "UNINSTALLED";
    case UNRESOLVED:
      return "UNRESOLVED";
    case UPDATED:
      return "UPDATED";
    case STARTING:
      return "STARTING";
    case STOPPING:
      return "STOPPPING";
    case LAZY_STARTED:
      return "LAZY_STARTED";
    default:
      return "UNKNOWN(" + type + ")";
    }
  }

}
