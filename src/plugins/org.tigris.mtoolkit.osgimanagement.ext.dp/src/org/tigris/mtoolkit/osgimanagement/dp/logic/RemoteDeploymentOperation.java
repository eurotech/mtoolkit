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
package org.tigris.mtoolkit.osgimanagement.dp.logic;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ui.statushandlers.StatusManager;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.Utils;
import org.tigris.mtoolkit.osgimanagement.dp.model.DeploymentPackage;

public abstract class RemoteDeploymentOperation extends Job {

	private DeploymentPackage pack;
	protected Dialog dialog;

	public RemoteDeploymentOperation(String name, DeploymentPackage pack) {
		super(name);
		this.pack = pack;
	}

	protected DeploymentPackage getDeploymentPackage() {
		return pack;
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
			monitor.done();
		}
		if (!operationResult.isOK())
			StatusManager.getManager().handle(operationResult,
				StatusManager.SHOW | StatusManager.LOG);
		return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
	}

	protected abstract IStatus doOperation(IProgressMonitor monitor) throws IAgentException;

	protected IStatus handleException(IAgentException e) {
		return Utils.handleIAgentException(e);
	}

	protected abstract String getMessage(IStatus operationStatus);
}
