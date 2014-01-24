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

import java.util.Dictionary;

import org.tigris.mtoolkit.iagent.transport.Transport;

/**
 * Interface of the PMP Service
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface PMPService extends PMPPeer {
  /**
   * @since 3.0
   */
  public static final String PROP_PMP_PORT = "pmp-port"; //$NON-NLS-1$

  /**
   * Tries to establish a connection to remote PMP Service.
   *
   * @param uri
   *          of the remote PMPService host uri is in the following format
   *          protocol://host:port
   * @param login
   *          the login certificates
   * @return PMPConnection
   * @exception PMPException
   *              If an IOException, protocol or authentication error occured.
   * @since 3.0
   */

  public PMPConnection connect(Transport transport, Dictionary props) throws PMPException;
}
