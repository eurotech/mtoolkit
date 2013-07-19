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
package org.tigris.mtoolkit.osgimanagement.application.logic;

import org.tigris.mtoolkit.osgimanagement.application.model.Application;
import org.tigris.mtoolkit.osgimanagement.model.AbstractRemoteModelOperation;

public abstract class RemoteApplicationOperation extends AbstractRemoteModelOperation<Application> {
  public RemoteApplicationOperation(String taskName, Application application) {
    super(taskName, application);
  }

  protected Application getApplication() {
    return getModel();
  }
}
