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
package org.tigris.mtoolkit.osgimanagement.application.model;

import org.osgi.service.application.ApplicationDescriptor;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteApplication;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public final class Application extends Model {
  private final String applicationID;
  private final RemoteApplication remoteApplication;
  private String state = RemoteApplication.STATE_INSTALLED;

  public Application(String id, RemoteApplication remoteApplication) {
    super(id);
    applicationID = id;
    this.remoteApplication = remoteApplication;
    try {
      String localizedName = (String) remoteApplication.getProperties().get(ApplicationDescriptor.APPLICATION_NAME);
      if (localizedName != null) {
        setName(localizedName);
      }
      state = remoteApplication.getState();
    } catch (IAgentException e) {
      FrameworkPlugin.error(e);
    }
  }

  public String getApplicationID() {
    return applicationID;
  }

  public RemoteApplication getRemoteApplication() {
    return remoteApplication;
  }

  public synchronized String getState() {
    return state;
  }

  public synchronized void setState(String aState) {
    state = aState;
  }
}
