/**
 * 
 */
package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;

public class UpdateBundleOperation extends RemoteBundleOperation {
	private final File bundleFile;

	public UpdateBundleOperation(Bundle bundle, File bundleFile) {
		super(Messages.update_bundle, bundle);
		this.bundleFile = bundleFile;
	}

	protected IStatus doOperation(IProgressMonitor monitor) throws IAgentException {
		InputStream pis = null;
		try {
			RemoteBundle rBundle = getBundle().getRemoteBundle();
			monitor.beginTask(Messages.update_bundle, (int) bundleFile.length());
			pis = new ProgressInputStream(new FileInputStream(bundleFile), monitor);
			rBundle.update(pis);
		} catch (IOException e) {
			return FrameworkPlugin.newStatus(IStatus.ERROR, NLS.bind(Messages.update_file_not_found,
				bundleFile.toString()), e);
		} finally {
			if (pis != null)
				try {
					pis.close();
				} catch (IOException e) {
				}
		}
		return Status.OK_STATUS;
	}

	protected String getMessage(IStatus operationStatus) {
		return NLS.bind(Messages.bundle_update_failure, operationStatus);
	}
}