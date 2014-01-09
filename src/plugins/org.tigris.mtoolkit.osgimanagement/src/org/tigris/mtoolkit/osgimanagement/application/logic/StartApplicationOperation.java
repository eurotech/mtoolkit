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

import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.application.model.Application;

public final class StartApplicationOperation extends RemoteApplicationOperation {
  public StartApplicationOperation(Application application) {
    super("Start Application", application);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.model.AbstractRemoteModelOperation#doOperation(org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  protected IStatus doOperation(IProgressMonitor monitor) throws IAgentException {
    monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
    getApplication().getRemoteApplication().start(Collections.EMPTY_MAP);
    return Status.OK_STATUS;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.model.AbstractRemoteModelOperation#getMessage(org.eclipse.core.runtime.IStatus)
   */
  @Override
  protected String getMessage(IStatus operationStatus) {
    return NLS.bind("Failed to start {0} application on remote framework", getApplication().toString());
  }
}
