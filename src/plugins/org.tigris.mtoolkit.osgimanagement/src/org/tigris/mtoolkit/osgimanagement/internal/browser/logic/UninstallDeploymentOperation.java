package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteDP;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.UIHelper;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.DeploymentPackage;

public class UninstallDeploymentOperation extends RemoteDeploymentOperation {

	public UninstallDeploymentOperation(DeploymentPackage pack) {
		super(Messages.uninstall_dp, pack);
	}

	protected IStatus doOperation(IProgressMonitor monitor) throws IAgentException {
		try {
			return uninstallDeploymentPackage(false);
		} catch (IAgentException e) {
			if (e.getErrorCode() == IAgentErrors.ERROR_DEPLOYMENT_STALE)
				// in case the deployment package is stale, rethrow
				throw e;
			if (IAgentErrors.toDeploymentExceptionCode(e.getErrorCode()) > 0) {
				// remote deployment admin threw an exception, ask the user for
				// forced uninstallation
				if (askUserToForceUninstallation(FrameworkPlugin.handleIAgentException(e))) {
					return uninstallDeploymentPackage(true);
				} else {
					// the user has already been notified, skip the error dialog
					return Status.OK_STATUS;
				}
			}
		}
		return Status.OK_STATUS;
	}

	private IStatus uninstallDeploymentPackage(boolean forced) throws IAgentException {
		RemoteDP rPackage = getDeploymentPackage().getRemoteDP();
		rPackage.uninstall(forced);
		return Status.OK_STATUS;
	}

	private boolean askUserToForceUninstallation(IStatus status) {
		MessageDialog dialog = new MessageDialog(FrameWorkView.getShell(),
			"Force Deployment Package Uninstallation",
			null,
			NLS.bind("Deployment package {0} uninstallation failed: {1}",
				getDeploymentPackage().toString(),
				status.getMessage()),
			MessageDialog.QUESTION,
			new String[] { "Force Uninstallation", "Cancel" },
			0);
		return UIHelper.openWindow(dialog) == 0;
	}

	protected String getMessage(IStatus operationStatus) {
		return NLS.bind("Deployment package {0} uninstallation failed", getDeploymentPackage().toString());
	}

}
