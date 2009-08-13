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
import org.tigris.mtoolkit.iagent.spi.PMPConnection;
import org.tigris.mtoolkit.iagent.spi.Utils;

public class RemoteDPImpl implements RemoteDP {

	private String symbolicName;
	private String version;

	private boolean stale;

	private DeploymentManagerImpl commands;

	public RemoteDPImpl(DeploymentManagerImpl commands, String symbolicName, String version) {
		log("[Constructor] >>> Create new deployment package: manager: "
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
		log("[getBundle] >>> symbolicName: " + bsn);
		checkState();
		Long bid = (Long) Utils.callRemoteMethod(getDeploymentAdmin(),
			Utils.GET_DP_BUNDLE_METHOD,
			new Object[] { this.symbolicName, version, bsn });
		// TODO: Rework this to work with Error objects instead of longs
		if (bid.longValue() == -2) {
			log("[getBundle] remote call result is: " + bid + " -> deployment package is stale");
			stale = true;
			checkState();
			return null;
		} else if (bid.longValue() == -1) {
			log("[getBundle] remote call result is: " + bid + " -> bundle wasn't found");
			return null;
		} else {
			log("[getBundle] remote call result is: " + bid + " -> bundle found");
			return new RemoteBundleImpl(commands, bid);
		}
	}

	private void checkState() throws IAgentException {
		if (stale) {
			log("[checkState] Remote deployment package has been uninstalled or updated with newer version");
			throw new IAgentException("Remote deployment package has been uninstalled or updated with newer version",
				IAgentErrors.ERROR_DEPLOYMENT_STALE);
		}
	}

	public Dictionary getBundles() throws IAgentException {
		log("[getBundles] >>>");
		checkState();
		Dictionary bundles = (Dictionary) Utils.callRemoteMethod(getDeploymentAdmin(),
			Utils.GET_DP_BUNDLES_METHOD,
			new Object[] { symbolicName, version });
		if (bundles == null) {
			log("[getBundle] remote call result is: " + bundles + " -> deployment package is stale");
			stale = true;
			checkState();
		}
		log("[getBundle] deployment package bundles: " + DebugUtils.convertForDebug(bundles));
		return bundles;
	}

	public String getHeader(String header) throws IAgentException {
		log("[getHeader] >>> header: " + header);
		if (header == null)
			throw new IllegalArgumentException();
		checkState();
		Object result = Utils.callRemoteMethod(getDeploymentAdmin(),
			Utils.GET_DP_HEADER_METHOD,
			new Object[] { symbolicName, version, header });
		if (result == null) {
			log("[getHeader] remote call result is: " + result + " -> header not found");
			return null;
		} else if (result instanceof String) {
			log("[getHeader] remote call result is: " + result + " -> header found");
			return (String) result;
		} else if (result instanceof Error) {
			Error err = (Error) result;
			if (err.getCode() == Error.DEPLOYMENT_UNINSTALLED_CODE) {
				log("[getHeader] remote call result is: " + result + " -> DP uninstalled");
				stale = true;
				checkState();
				return null;
			} else {
				log("[getHeader] remote call result is: " + result + " -> general error");
				throw new IAgentException(err);
			}
		} else {
			log("[getHeader] Unknown response from remote method invocation: " + result);
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
			log("[isStale] Quering remote site...");
			Boolean result = (Boolean) Utils.callRemoteMethod(getDeploymentAdmin(),
				Utils.IS_DP_STALE_METHOD,
				new Object[] { symbolicName, version });
			stale = result.booleanValue();
		}
		log("[isStale] deployment package is " + (stale ? "stale" : "not stale"));
		return stale;
	}

	public boolean uninstall(boolean force) throws IAgentException {
		log("[uninstall] >>> force: " + force);
		checkState();
		Object result = Utils.callRemoteMethod(getDeploymentAdmin(),
			Utils.UNINSTALL_DP_METHOD,
			new Object[] { symbolicName, version, new Boolean(force) });
		if (result instanceof Boolean) { // OK, no exception was thrown
			log("[uninstall] remote call result is: " + result + " -> no exceptions");
			return ((Boolean) result).booleanValue();
		} else if (result instanceof Error) {
			Error err = (Error) result;
			log("[uninstall] DP uninstallation failed: " + result);
			if (err.getCode() == Error.DEPLOYMENT_UNINSTALLED_CODE) {
				log("[uninstall] -> deployment package is stale");
				stale = true;
				checkState();
				return false;
			} else {
				throw new IAgentException(err);
			}
		} else {
			log("[uninstall] Unknown response from remote method invocation: " + result);
			throw new IAgentException("Unknown response from remote method invocation: " + result,
				IAgentErrors.ERROR_INTERNAL_ERROR);
		}
	}

	private RemoteObject getDeploymentAdmin() throws IAgentException {
		return commands.getDeploymentAdmin();
	}

	private final void log(String message) {
		log(message, null);
	}

	private final void log(String message, Throwable e) {
		DebugUtils.log(this.toString(), message, e);
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
