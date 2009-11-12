/**
 * 
 */
package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;

public class StopBundleOperation extends RemoteBundleOperation {
	public StopBundleOperation(Bundle bundle) {
		super(Messages.stop_bundle, bundle);
	}

	protected IStatus doOperation(IProgressMonitor monitor) throws IAgentException {
		getBundle().getRemoteBundle().stop(0);
		return Status.OK_STATUS;
	}

	protected String getMessage(IStatus operationStatus) {
		return NLS.bind(Messages.bundle_stop_failure, getBundle().toString());
	}
}