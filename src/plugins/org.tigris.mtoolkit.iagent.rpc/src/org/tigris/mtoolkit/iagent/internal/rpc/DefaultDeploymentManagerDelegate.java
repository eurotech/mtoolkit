/*******************************************************************************
 * Copyright (c) 2005, 2012 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 ****************************************************************************/
package org.tigris.mtoolkit.iagent.internal.rpc;

import java.io.InputStream;

import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.tigris.mtoolkit.iagent.Error;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.rpc.spi.DeploymentManagerDelegate;
import org.tigris.mtoolkit.iagent.util.DebugUtils;

public final class DefaultDeploymentManagerDelegate implements DeploymentManagerDelegate {
	private DeploymentAdmin dpAdmin;

	public DefaultDeploymentManagerDelegate(DeploymentAdmin dpAdmin) {
		this.dpAdmin = dpAdmin;
	}


  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.spi.DeploymentManagerDelegate#installDeploymentPackage(java.io.InputStream)
   */
	public Object installDeploymentPackage(InputStream in) {
		try {
			DeploymentPackage dp = dpAdmin.installDeploymentPackage(in);
			return dp;
		} catch (DeploymentException e) {
      return new Error(IAgentErrors.fromDeploymentExceptionCode(e.getCode()),
					"Failed to install deployment package: " + DebugUtils.toString(e), DebugUtils.getStackTrace(e));
		}
	}

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.spi.DeploymentManagerDelegate#uninstallDeploymentPackage(org.osgi.service.deploymentadmin.DeploymentPackage, boolean)
   */
	public Object uninstallDeploymentPackage(DeploymentPackage dp, boolean force) {
		if (!force) {
			// normal
			try {
				dp.uninstall();
				return Boolean.TRUE;
			} catch (DeploymentException e) {
        return new Error(IAgentErrors.fromDeploymentExceptionCode(e.getCode()),
						"Failed to uninstall deployment package: " + DebugUtils.toString(e),
						DebugUtils.getStackTrace(e));
			}
		} else {
			// forced
			try {
				boolean result = dp.uninstallForced();
				return result ? Boolean.TRUE : Boolean.FALSE;
			} catch (DeploymentException e) {
        return new Error(IAgentErrors.fromDeploymentExceptionCode(e.getCode()),
						"Failed to uninstall deployment package: " + DebugUtils.toString(e),
						DebugUtils.getStackTrace(e));
			}
		}
	}
}
