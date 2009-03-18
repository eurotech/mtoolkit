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

import org.tigris.mtoolkit.iagent.ServiceManager;


/**
 * Clients which are interested in remote service events must implement this
 * interface and add itself as interested parties in listeners list via
 * {@link ServiceManager#addRemoteServiceListener(RemoteServiceListener)}
 * 
 * @author Danail Nachev
 * 
 */
public interface RemoteServiceListener {

  /**
   * Sent when a remote service is changed.
   * 
   * @param event
   *            an object describing details about the event
   */
  void serviceChanged(RemoteServiceEvent event);
  
}
