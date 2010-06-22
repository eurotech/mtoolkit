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
package org.tigris.mtoolkit.iagent.internal;

import java.util.Dictionary;

import org.tigris.mtoolkit.iagent.Error;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.iagent.RemoteDP;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;
import org.tigris.mtoolkit.iagent.spi.MethodSignature;

public class RemoteDPImpl implements RemoteDP {

	private static MethodSignature GET_DP_HEADER_METHOD = new MethodSignature("getDeploymentPackageHeader", new String[] { MethodSignature.STRING_TYPE, MethodSignature.STRING_TYPE, MethodSignature.STRING_TYPE },
			true);
	private static MethodSignature GET_DP_BUNDLES_METHOD = new MethodSignature("getDeploymentPackageBundles", new String[] { MethodSignature.STRING_TYPE, MethodSignature.STRING_TYPE }, true);
	private static MethodSignature GET_DP_BUNDLE_METHOD = new MethodSignature("getDeploymentPackageBundle", new String[] { MethodSignature.STRING_TYPE, MethodSignature.STRING_TYPE, MethodSignature.STRING_TYPE },
			true);
	private static MethodSignature UNINSTALL_DP_METHOD = new MethodSignature("uninstallDeploymentPackage", new String[] { MethodSignature.STRING_TYPE, MethodSignature.STRING_TYPE, "boolean" },
			true);
	private static MethodSignature IS_DP_STALE_METHOD = new MethodSignature("isDeploymentPackageStale", new String[] { MethodSignature.STRING_TYPE, MethodSignature.STRING_TYPE }, true);

	private String symbolicName;
	private String version;

	private boolean stale;

	private DeploymentManagerImpl commands;

	public RemoteDPImpl(DeploymentManagerImpl commands, String symbolicName, String version) {
		debug("[Constructor] >>> Create new deployment package: manager: "
						+ commands
						+ "; symbolicName: "
						+ symbolicName
						+ "; version: "
						+ version);
		if (symbolicName == null || version == null)
			throw new IllegalArgumentException("both symbolicName and version must be specified");
		this.symbolicName = symbolicName;
		this.version = version;
		this.commands = commands;
	}

	public RemoteBundle getBundle(String bsn) throws IAgentException {
		debug("[getBundle] >>> symbolicName: " + bsn);
		checkState();
		Long bid = (Long) GET_DP_BUNDLE_METHOD.call(getDeploymentAdmin(), new Object[] { this.symbolicName, version, bsn });
		// TODO: Rework this to work with Error objects instead of longs
		if (bid.longValue() == -2) {
			debug("[getBundle] remote call result is: " + bid + " -> deployment package is stale");
			stale = true;
			checkState();
			return null;
		} else if (bid.longValue() == -1) {
			debug("[getBundle] remote call result is: " + bid + " -> bundle wasn't found");
			return null;
		} else {
			debug("[getBundle] remote call result is: " + bid + " -> bundle found");
			return new RemoteBundleImpl(commands, bid);
		}
	}

	private void checkState() throws IAgentException {
		if (stale) {
			debug("[checkState] Remote deployment package has been uninstalled or updated with newer version");
			throw new IAgentException("Remote deployment package has been uninstalled or updated with newer version",
				IAgentErrors.ERROR_DEPLOYMENT_STALE);
		}
	}

	public Dictionary getBundles() throws IAgentException {
		debug("[getBundles] >>>");
		checkState();
		Dictionary bundles = (Dictionary) GET_DP_BUNDLES_METHOD.call(getDeploymentAdmin(), new Object[] { symbolicName, version });
		if (bundles == null) {
			debug("[getBundle] remote call result is: " + bundles + " -> deployment package is stale");
			stale = true;
			checkState();
		}
		debug("[getBundle] deployment package bundles: " + DebugUtils.convertForDebug(bundles));
		return bundles;
	}

	public String getHeader(String header) throws IAgentException {
		debug("[getHeader] >>> header: " + header);
		if (header == null)
			throw new IllegalArgumentException();
		checkState();
		Object result = GET_DP_HEADER_METHOD.call(getDeploymentAdmin(), new Object[] { symbolicName, version, header });
		if (result == null) {
			debug("[getHeader] remote call result is: " + result + " -> header not found");
			return null;
		} else if (result instanceof String) {
			debug("[getHeader] remote call result is: " + result + " -> header found");
			return (String) result;
		} else if (result instanceof Error) {
			Error err = (Error) result;
			if (err.getCode() == Error.DEPLOYMENT_UNINSTALLED_CODE) {
				debug("[getHeader] remote call result is: " + result + " -> DP uninstalled");
				stale = true;
				checkState();
				return null;
			} else {
				info("[getHeader] remote call result is: " + result + " -> general error");
				throw new IAgentException(err);
			}
		} else {
			info("[getHeader] Unknown response from remote method invocation: " + result);
			throw new IAgentException("Unknown response from remote method invocation: " + result,
				IAgentErrors.ERROR_INTERNAL_ERROR);
		}
	}

	public String getName() throws IAgentException {
		return symbolicName;
	}

	public String getVersion() throws IAgentException {
		return version;
	}

	public boolean isStale() throws IAgentException {
		if (!stale) {
			debug("[isStale] Quering remote site...");
			Boolean result = (Boolean) IS_DP_STALE_METHOD.call(getDeploymentAdmin(), new Object[] { symbolicName, version });
			stale = result.booleanValue();
		}
		debug("[isStale] deployment package is " + (stale ? "stale" : "not stale"));
		return stale;
	}

	public boolean uninstall(boolean force) throws IAgentException {
		debug("[uninstall] >>> force: " + force);
		checkState();
		Object result = UNINSTALL_DP_METHOD.call(getDeploymentAdmin(), new Object[] { symbolicName, version, new Boolean(force) });
		if (result instanceof Boolean) { // OK, no exception was thrown
			debug("[uninstall] remote call result is: " + result + " -> no exceptions");
			return ((Boolean) result).booleanValue();
		} else if (result instanceof Error) {
			Error err = (Error) result;
			debug("[uninstall] DP uninstallation failed: " + result);
			if (err.getCode() == Error.DEPLOYMENT_UNINSTALLED_CODE) {
				debug("[uninstall] -> deployment package is stale");
				stale = true;
				checkState();
				return false;
			} else {
				throw new IAgentException(err);
			}
		} else {
			info("[uninstall] Unknown response from remote method invocation: " + result);
			throw new IAgentException("Unknown response from remote method invocation: " + result,
				IAgentErrors.ERROR_INTERNAL_ERROR);
		}
	}

	private RemoteObject getDeploymentAdmin() throws IAgentException {
		return commands.getDeploymentAdmin();
	}

	private final void debug(String message) {
		DebugUtils.debug(this, message);
	}

	private final void info(String message) {
		DebugUtils.info(this, message);
	}

	public String toString() {
		return "RemoteDP@"
						+ Integer.toHexString(System.identityHashCode(this))
						+ "["
						+ symbolicName
						+ "]["
						+ version
						+ "]";
	}

}
