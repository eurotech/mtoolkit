package org.tigris.mtoolkit.iagent.internal.rpc;

import java.io.InputStream;

import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.tigris.mtoolkit.iagent.rpc.spi.DeploymentManagerDelegate;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.Error;

public class DefaultDeploymentManagerDelegate implements DeploymentManagerDelegate {

	DeploymentAdmin dpAdmin;
	
	public DefaultDeploymentManagerDelegate(DeploymentAdmin dpAdmin) {
		this.dpAdmin = dpAdmin;
	}
	
	public Object installDeploymentPackage(InputStream in) {
		try {
			DeploymentPackage dp = dpAdmin.installDeploymentPackage(in);
			return dp;
		} catch (DeploymentException e) {
	        return new Error(org.tigris.mtoolkit.iagent.internal.utils.ExceptionCodeHelper.fromDeploymentExceptionCode(e.getCode()), "Failed to install deployment package: " + DebugUtils.toString(e));
		}
	}
	
	public Object uninstallDeploymentPackage(DeploymentPackage dp, boolean force) {
		if (!force) {
			// normal
			try {
				dp.uninstall();
				return Boolean.TRUE;
			} catch (DeploymentException e) {
				return new Error(org.tigris.mtoolkit.iagent.internal.utils.ExceptionCodeHelper.fromDeploymentExceptionCode(e.getCode()), "Failed to uninstall deployment package: " + DebugUtils.toString(e));
			}
		} else {
			// forced
			try {
				boolean result = dp.uninstallForced();
				return result ? Boolean.TRUE : Boolean.FALSE;
			} catch (DeploymentException e) {
				return new Error(org.tigris.mtoolkit.iagent.internal.utils.ExceptionCodeHelper.fromDeploymentExceptionCode(e.getCode()), "Failed to uninstall deployment package: " + DebugUtils.toString(e));
			}
		}
	}

	public boolean isSupported() {
		return true;
	}
}
