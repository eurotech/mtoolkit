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
import org.eclipse.ui.statushandlers.StatusManager;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.application.Activator;
import org.tigris.mtoolkit.osgimanagement.application.model.Application;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;



public abstract class RemoteApplicationOperation extends Job {

	private Application application;

	public RemoteApplicationOperation(String taskName, Application application) {
		super(taskName);
		setUser(true);
		this.application = application;
	}

	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask(getName(), 1);
		IStatus operationResult = Status.OK_STATUS;
		try {
			monitor.beginTask(getName(), 1);
			operationResult = doOperation(monitor);
		} catch (IAgentException e) {
			operationResult = handleException(e);
		} finally {
			if (application != null)
				application.updateElement();
			monitor.done();
		}
		if (!operationResult.isOK())
			StatusManager.getManager().handle(new Status(operationResult.getSeverity(), Activator.PLUGIN_ID, operationResult.getMessage(), operationResult.getException()),
				StatusManager.SHOW | StatusManager.LOG);
		return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
	}

	protected Application getApplication() {
		return application;
	}

	protected abstract IStatus doOperation(IProgressMonitor monitor) throws IAgentException;

	protected IStatus handleException(Exception e) {
		if (e instanceof IAgentException) {
			IStatus tmpStatus = FrameworkPlugin.handleIAgentException((IAgentException)e);
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, tmpStatus.getMessage(), e);
		} else {
			Status errStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage());
			return errStatus;
		}
	}

	protected abstract String getMessage(IStatus operationStatus);
}
