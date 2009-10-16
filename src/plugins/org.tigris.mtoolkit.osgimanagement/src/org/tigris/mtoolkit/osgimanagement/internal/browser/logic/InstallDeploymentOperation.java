package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteDP;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.DeploymentPackage;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;

public class InstallDeploymentOperation extends RemoteDeploymentOperation {

	private File sourceFile;
	private FrameWork framework;

	public InstallDeploymentOperation(File dpFile, FrameWork framework) {
		super(Messages.install_dp, null);
		this.sourceFile = dpFile;
		this.framework = framework;
	}

	protected IStatus doOperation(IProgressMonitor monitor) throws IAgentException {
		try {
			JarFile jar = new JarFile(sourceFile);
			Manifest manifest = jar.getManifest();
			if (manifest == null)
				return FrameworkPlugin.newStatus(IStatus.ERROR, NLS.bind("Source file {0} doesn't have valid manifest",
					sourceFile), null);
			String symbolicName = manifest.getMainAttributes().getValue("DeploymentPackage-SymbolicName");
			if (symbolicName == null)
				return FrameworkPlugin.newStatus(IStatus.ERROR, NLS.bind("Source file {0} doesn't have valid manifest",
					sourceFile), null);
			String version = manifest.getMainAttributes().getValue("DeploymentPackage-Version");
			if (version == null)
				return FrameworkPlugin.newStatus(IStatus.ERROR, NLS.bind("Source file {0} doesn't have valid manifest",
					sourceFile), null);
			DeviceConnector connector = framework.getConnector();
			if (connector == null) return FrameworkPlugin.newStatus(IStatus.ERROR, "Connection lost", null);
			RemoteDP remoteDP = connector.getDeploymentManager().getDeploymentPackage(symbolicName);
			if (remoteDP != null) {
				// deployment package already exists, if it has the same version
				// we need to remove and install again after user confirmation
				String remoteVersion = remoteDP.getVersion();
				if (remoteVersion.equals(version)) {
					if (askUserToUninstallRemotePackage(symbolicName)) {
						DeploymentPackage packageNode = framework.findDP(symbolicName);
						if (packageNode == null)
							return FrameworkPlugin.newStatus(IStatus.ERROR,
								"Local representation of the remote OSGi framework is stale. Refresh and try again.",
								null);
						Job uninstallJob = new UninstallDeploymentOperation(packageNode);
						uninstallJob.schedule();
						try {
							uninstallJob.join();
						} catch (InterruptedException e) {
							return FrameworkPlugin.newStatus(IStatus.WARNING,
								"Unable to finish deployment package installation",
								e);
						}
					}
				}
			}
			framework.getConnector().getDeploymentManager().installDeploymentPackage(new FileInputStream(sourceFile));
		} catch (IOException e) {
		}
		return Status.OK_STATUS;
	}

	private boolean askUserToUninstallRemotePackage(final String symbolicName) {
		Display display = Display.getDefault();
		final int[] result = new int[1];
		display.syncExec(new Runnable() {
			public void run() {
				dialog = new MessageDialog(FrameWorkView.getShell(),
						"Uninstall Existing Deployment Package",
						null,
						NLS.bind("The deployment package {0} exists on the remote framework with the same version. If you want to update it, the remote version of the deployment package needs to be uninstalled first",
							symbolicName),
						MessageDialog.QUESTION,
						new String[] { "Uninstall Remote Version", "Cancel" },
						0);
				result[0] = dialog.open();
			}
		});
		return result[0] == 0;
	}

	protected String getMessage(IStatus operationStatus) {
		return "Deployment package installation failed";
	}

}
