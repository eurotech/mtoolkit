/*******************************************************************************
 * Copyright (c) 2005, 2013 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.Util;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;

public abstract class RemoteBundleOperation extends Job {
  private final Bundle bundle;

  public RemoteBundleOperation(String taskName, Bundle bundle) {
    super(taskName);
    this.bundle = bundle;
    setUser(true);
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
      // refresh the bundle state
      if (getBundle() != null) {
        getBundle().refreshStateFromRemote();
      }
      operationResult = handleException(e);
    } finally {
      if (bundle != null) {
        bundle.updateElement();
      }
      monitor.done();
    }
    if (monitor.isCanceled()) {
      return Status.CANCEL_STATUS;
    }
    return Util.newStatus(getMessage(operationResult), operationResult);
  }

  protected Bundle getBundle() {
    return bundle;
  }

  protected IStatus handleException(IAgentException e) {
    return Util.handleIAgentException(e);
  }

  protected abstract IStatus doOperation(IProgressMonitor monitor) throws IAgentException;

  protected abstract String getMessage(IStatus operationStatus);
}
