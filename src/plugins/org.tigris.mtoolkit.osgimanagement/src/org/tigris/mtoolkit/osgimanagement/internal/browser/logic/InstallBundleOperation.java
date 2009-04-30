package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;

public class InstallBundleOperation extends RemoteBundleOperation {
	private final File bundle;
	private final FrameWork framework;

	InstallBundleOperation(File bundle, FrameWork framework) {
		super(Messages.install_bundle, null);
		this.bundle = bundle;
		this.framework = framework;
	}

	protected IStatus doOperation(IProgressMonitor monitor) throws IAgentException {
		InputStream input = null;
		RemoteBundle rBundle = null;
		try {
			int work = (int) bundle.length();
			monitor.beginTask(getName(), work);
			input = new ProgressInputStream(new FileInputStream(bundle), monitor);
			rBundle = framework.getConnector().getDeploymentManager().installBundle("remote:" + bundle.getName(), input);

			Set bundleIds = framework.getBundlesKeys();

			// bundle already exists, in which case, we need to update it
			if (bundleIds.contains(new Long(rBundle.getBundleId()))) {
				monitor.beginTask(Messages.update_bundle, work);
				try {
					// close the old input stream and try again
					input.close();
				} catch (IOException e) {
				}
				input = new ProgressInputStream(new FileInputStream(bundle), monitor);
				rBundle.update(input);
			}
		} catch (IOException e) {
			return FrameworkPlugin.newStatus(IStatus.ERROR,
				NLS.bind(Messages.update_file_not_found, bundle.getName()),
				e);
		} finally {
			if (input != null)
				try {
					input.close();
				} catch (IOException e) {
				}
		}
		if (FrameworkConnectorFactory.isAutoStartBundlesEnabled && rBundle != null) {
			try {
				rBundle.start(0);
			} catch (IAgentException e) {
				// only log this exception, because the user requested install
				// bundle, which succeeded
				StatusManager.getManager().handle(FrameworkPlugin.handleIAgentException(e), StatusManager.LOG);
			}
		}
		return Status.OK_STATUS;
	}

	protected String getMessage(IStatus operationStatus) {
		return "Failed to install bundle on remote framework";
	}
}