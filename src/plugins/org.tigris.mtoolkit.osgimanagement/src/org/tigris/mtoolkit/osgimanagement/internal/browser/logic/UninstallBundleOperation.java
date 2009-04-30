/**
 * 
 */
package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;

public class UninstallBundleOperation extends RemoteBundleOperation {
	UninstallBundleOperation(Bundle bundle) {
		super(Messages.uninstall_bundle, bundle);
	}

	protected IStatus doOperation(IProgressMonitor monitor) throws IAgentException {
		getBundle().getRemoteBundle().uninstall();
		return Status.OK_STATUS;
	}

	protected IStatus handleException(IAgentException e) {
		// ignore that a bundle is uninstalled when trying uninstalling it
		if (e.getErrorCode() == IAgentErrors.ERROR_BUNDLE_UNINSTALLED)
			return Status.OK_STATUS;
		return super.handleException(e);
	}

	protected String getMessage(IStatus operationStatus) {
		return NLS.bind("Bundle {0} uninstallation did not finish cleanly", getBundle().toString());
	}
}