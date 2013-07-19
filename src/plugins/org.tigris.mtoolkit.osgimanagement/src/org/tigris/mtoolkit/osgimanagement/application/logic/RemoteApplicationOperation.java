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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.Util;
import org.tigris.mtoolkit.osgimanagement.application.model.Application;

public abstract class RemoteApplicationOperation extends Job {
  private final Application application;

  public RemoteApplicationOperation(String taskName, Application application) {
    super(taskName);
    setUser(true);
    this.application = application;
  }

  /* (non-Javadoc)
   * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  protected IStatus run(IProgressMonitor monitor) {
    monitor.beginTask(getName(), 1);
    IStatus operationResult = Status.OK_STATUS;
    try {
      monitor.beginTask(getName(), 1);
      operationResult = doOperation(monitor);
    } catch (IAgentException e) {
      operationResult = handleException(e);
    } finally {
      if (application != null) {
        application.updateElement();
      }
      monitor.done();
    }
    if (monitor.isCanceled()) {
      return Status.CANCEL_STATUS;
    }
    return Util.newStatus(getMessage(operationResult), operationResult);
  }

  protected Application getApplication() {
    return application;
  }

  protected IStatus handleException(Exception e) {
    return Util.newStatus(IStatus.ERROR, e.getMessage(), e);
  }

  protected abstract IStatus doOperation(IProgressMonitor monitor) throws IAgentException;

  protected abstract String getMessage(IStatus operationStatus);
}
