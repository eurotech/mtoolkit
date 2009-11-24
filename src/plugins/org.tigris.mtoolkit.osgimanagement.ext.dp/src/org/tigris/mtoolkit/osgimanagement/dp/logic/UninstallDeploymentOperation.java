/*******************************************************************************
 * Copyright (c) 2005, 2009 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.osgimanagement.dp.logic;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteDP;
import org.tigris.mtoolkit.osgimanagement.dp.model.DeploymentPackage;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;

public class UninstallDeploymentOperation extends RemoteDeploymentOperation {

	public UninstallDeploymentOperation(DeploymentPackage pack) {
		super("Uninstalling deployment package...", pack);
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

	private boolean askUserToForceUninstallation(final IStatus status) {
		Display display = Display.getDefault();
		final int[] result = new int[1];
		display.syncExec(new Runnable() {
			public void run() {
				dialog = new MessageDialog(FrameWorkView.getShell(),
						"Force Deployment Package Uninstallation",
						null,
						NLS.bind("Deployment package {0} uninstallation failed: {1}",
							getDeploymentPackage().toString(),
							status.getMessage()),
						MessageDialog.QUESTION,
						new String[] { "Force Uninstallation", "Cancel" },
						0);
				result[0] = dialog.open();
			}
		});
		return result[0] == 0;
	}

	protected String getMessage(IStatus operationStatus) {
		return NLS.bind("Deployment package {0} uninstallation failed", getDeploymentPackage().toString());
	}

}
