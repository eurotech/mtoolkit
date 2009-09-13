/**
 * 
 */
package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;

public class StartBundleOperation extends RemoteBundleOperation {
	StartBundleOperation(Bundle bundle) {
		super(Messages.start_bundle, bundle);
	}

	protected IStatus doOperation(IProgressMonitor monitor) throws IAgentException {
		RemoteBundle rBundle = getBundle().getRemoteBundle();
		rBundle.start(0);
		if (rBundle.getState() == org.osgi.framework.Bundle.RESOLVED) {
			// the bundle failed to start, most probably because its
			// start level is too high
			int bundleStartLevel = rBundle.getBundleStartLevel();
			int fwStartLevel = getBundle().findFramework().getFrameWorkStartLevel();
			if (fwStartLevel < bundleStartLevel)
				return FrameworkPlugin.newStatus(IStatus.WARNING, Messages.bundle_start_failure, null);
		}
		return Status.OK_STATUS;
	}

	protected String getMessage(IStatus operationStatus) {
		return NLS.bind(Messages.bundle_startup_failure, getBundle().toString());
	}
}