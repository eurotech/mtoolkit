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

import org.tigris.mtoolkit.iagent.DeploymentManager;


/**
 * Clients interested in remote deployment package events must implement this
 * interface. The listeners must be added to the listeners list via
 * {@link DeploymentManager#addRemoteDPListener(RemoteDPListener)} method.
 * 
 * @author Danail Nachev
 * 
 */
public interface RemoteDPListener {

  /**
   * Sent when remote deployment package has been installed/uninstalled or
   * changed in some other way.
   * 
   * @param event
   *            an object containing details about the event
   */
  void deploymentPackageChanged(RemoteDPEvent event);

}
