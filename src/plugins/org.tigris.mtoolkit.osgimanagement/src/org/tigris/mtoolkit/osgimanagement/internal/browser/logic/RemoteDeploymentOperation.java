package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.statushandlers.StatusManager;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.DeploymentPackage;

public abstract class RemoteDeploymentOperation extends Job {

	private DeploymentPackage pack;

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
			StatusManager.getManager().handle(FrameworkPlugin.newStatus(getMessage(operationResult), operationResult),
				StatusManager.SHOW | StatusManager.LOG);
		return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
	}

	protected abstract IStatus doOperation(IProgressMonitor monitor) throws IAgentException;

	protected IStatus handleException(IAgentException e) {
		return FrameworkPlugin.handleIAgentException(e);
	}

	protected abstract String getMessage(IStatus operationStatus);
}
