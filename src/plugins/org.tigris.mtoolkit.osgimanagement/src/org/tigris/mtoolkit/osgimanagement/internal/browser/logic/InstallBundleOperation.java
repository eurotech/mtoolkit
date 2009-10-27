package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.statushandlers.StatusManager;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
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
		RemoteBundle rBundle[] = null;
		try {
			int work = (int) bundle.length();
			monitor.beginTask(getName(), work);
			input = new ProgressInputStream(new FileInputStream(bundle), monitor);
			DeviceConnector connector = framework.getConnector();
			if (connector == null)
				return FrameworkPlugin.newStatus(IStatus.ERROR, "Connection lost", null);
			
			ZipFile zip = new ZipFile(bundle);
			ZipEntry entry = zip.getEntry("META-INF/MANIFEST.MF");
			Manifest mf = new Manifest(zip.getInputStream(entry));
			final String symbName = (String) mf.getMainAttributes().getValue("Bundle-SymbolicName");
			
			// check if already installd
			final boolean update[] = new boolean []{false};
			if (symbName != null) {
				rBundle = connector.getDeploymentManager().getBundles(symbName, null);
				if (rBundle != null) {
					update[0] = true;
				}
			}
			
			// install if missing
			if (!update[0]) {
				Set bundleIds = new HashSet();
				bundleIds.addAll(framework.getBundlesKeys());
				rBundle = new RemoteBundle[1];
				rBundle[0] = connector.getDeploymentManager().installBundle("remote:" + bundle.getName(), input);
				// check again if already installed
				if (bundleIds.contains(new Long(rBundle[0].getBundleId()))) {
					update[0] = true;
				}
			}

			// bundle already exists, in which case, we need to update it
			if (update[0]) {
				try {
					// close the old input stream and try again
					input.close();
				} catch (IOException e) {
				}

				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						update[0] = MessageDialog.openConfirm(FrameWorkView.getShell(), Messages.update_dialog_title,
								"Bundle \"" + symbName + "\" is already installed.\nDo you want to update bundle?");
					}
				});
				if (update[0]) {
					monitor.beginTask(Messages.update_bundle, work);
					input = new ProgressInputStream(new FileInputStream(bundle), monitor);
					rBundle[0].update(input);
				}
			}
		} catch (IOException e) {
			return FrameworkPlugin.newStatus(IStatus.ERROR, NLS.bind(Messages.update_file_not_found, bundle.getName()),
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
				rBundle[0].start(0);
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