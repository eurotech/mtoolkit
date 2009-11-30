package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.statushandlers.StatusManager;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.Util;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;

public abstract class RemoteBundleOperation extends Job {

	private Bundle bundle;

	public RemoteBundleOperation(String taskName, Bundle bundle) {
		super(taskName);
		setUser(true);
		this.bundle = bundle;
	}

	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask(getName(), 1);
		IStatus operationResult = Status.OK_STATUS;
		try {
			monitor.beginTask(getName(), 1);
			operationResult = doOperation(monitor);
		} catch (IAgentException e) {
			// refresh the bundle state
			if (getBundle() != null)
				getBundle().refreshStateFromRemote();
			operationResult = handleException(e);
		} finally {
			if (bundle != null)
				bundle.updateElement();
			monitor.done();
		}
		if (!operationResult.isOK())
			StatusManager.getManager().handle(Util.newStatus(getMessage(operationResult), operationResult),
				StatusManager.SHOW | StatusManager.LOG);
		return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
	}

	protected Bundle getBundle() {
		return bundle;
	}

	protected abstract IStatus doOperation(IProgressMonitor monitor) throws IAgentException;

	protected IStatus handleException(IAgentException e) {
		return Util.handleIAgentException(e);
	}

	protected abstract String getMessage(IStatus operationStatus);
}
