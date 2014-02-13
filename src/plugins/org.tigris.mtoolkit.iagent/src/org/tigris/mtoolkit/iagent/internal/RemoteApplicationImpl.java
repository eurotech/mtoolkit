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
package org.tigris.mtoolkit.iagent.internal;

import java.util.Map;

import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteApplication;

public final class RemoteApplicationImpl implements RemoteApplication {
  private final String                 applicationID;
  private final ApplicationManagerImpl appManager;

  public RemoteApplicationImpl(ApplicationManagerImpl applicationManager, String applicationID) {
    this.applicationID = applicationID;
    this.appManager = applicationManager;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.RemoteApplication#start(java.util.Map)
   */
  public void start(Map properties) throws IAgentException {
    appManager.startApplication(applicationID, properties);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.RemoteApplication#stop()
   */
  public void stop() throws IAgentException {
    appManager.stopApplication(applicationID);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.RemoteApplication#getApplicationId()
   */
  public String getApplicationId() throws IAgentException {
    return applicationID;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.RemoteApplication#getState()
   */
  public String getState() throws IAgentException {
    return appManager.getApplicationState(applicationID);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.RemoteApplication#getProperties()
   */
  public Map getProperties() throws IAgentException {
    return appManager.getApplicationProperties(applicationID);
  }
}
