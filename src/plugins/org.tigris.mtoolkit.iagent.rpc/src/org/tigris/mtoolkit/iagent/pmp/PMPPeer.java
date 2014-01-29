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
package org.tigris.mtoolkit.iagent.pmp;

public interface PMPPeer {
  /**
   * @since 3.2
   */
  public static final int    DEFAULT_PMP_PORT = 1450;

  /**
   * @since 3.2
   */
  public static final String PORT             = "port"; //$NON-NLS-1$

  /**
   * Registers listener to receive events whenever one host is connected or
   * disconected to the pmp service
   *
   * @param listener
   *          the PMPEventListener to register
   * @see PMPConnectionListener
   */

  public void addConnectionListener(PMPConnectionListener listener);

  /**
   * Removes registred PMPEventlistener
   *
   * @param listener
   *          the PMPEventListener to remove
   * @see PMPConnectionListener
   */

  public void removeConnectionListener(PMPConnectionListener listener);
}
