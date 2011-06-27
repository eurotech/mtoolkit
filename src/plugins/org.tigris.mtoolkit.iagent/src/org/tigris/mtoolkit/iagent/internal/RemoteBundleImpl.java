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

import java.io.InputStream;
import java.util.Dictionary;

import org.tigris.mtoolkit.iagent.Error;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.iagent.RemoteService;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;
import org.tigris.mtoolkit.iagent.spi.MethodSignature;

public class RemoteBundleImpl implements RemoteBundle {

	private static MethodSignature GET_BUNDLE_STATE_METHOD = new MethodSignature("getBundleState", MethodSignature.BID_ARGS, true);
	private static MethodSignature GET_BUNDLE_LAST_MODIFIED_METHOD = new MethodSignature("getBundleLastModified", MethodSignature.BID_ARGS, true);
	private static MethodSignature GET_BUNDLE_HEADERS_METHOD = new MethodSignature("getBundleHeaders", new String[] { "long", MethodSignature.STRING_TYPE }, true);
	private static MethodSignature GET_BUNDLE_LOCATION_METHOD = new MethodSignature("getBundleLocation", MethodSignature.BID_ARGS, true);
	private static MethodSignature START_BUNDLE_METHOD = new MethodSignature("startBundle", new String[] { "long", "int" }, true);
	private static MethodSignature STOP_BUNDLE_METHOD = new MethodSignature("stopBundle", new String[] { "long", "int" }, true);
	private static MethodSignature UPDATE_BUNDLE_METHOD = new MethodSignature("updateBundle", new String[] { "long", MethodSignature.INPUT_STREAM_TYPE }, true);
	private static MethodSignature UNINSTALL_BUNDLE_METHOD = new MethodSignature("uninstallBundle", MethodSignature.BID_ARGS, true);
	private static MethodSignature GET_BUNDLE_NAME_METHOD = new MethodSignature("getBundleSymbolicName", MethodSignature.BID_ARGS, true);
	private static MethodSignature RESOLVE_BUNDLES_METHOD = new MethodSignature("resolveBundles", new String[] { long[].class.getName() }, true);
	private static MethodSignature GET_REGISTERED_SERVICES_METHOD = new MethodSignature("getRegisteredServices", MethodSignature.BID_ARGS, true);
	private static MethodSignature GET_USING_SERVICES_METHOD = new MethodSignature("getUsingServices", MethodSignature.BID_ARGS, true);
	private static MethodSignature GET_FRAGMENT_BUNDLES_METHOD = new MethodSignature("getFragmentBundles", MethodSignature.BID_ARGS, true);
	private static MethodSignature GET_HOST_BUNDLES_METHOD = new MethodSignature("getHostBundles", MethodSignature.BID_ARGS, true);
	private static MethodSignature GET_BUNDLE_TYPE_METHOD = new MethodSignature("getBundleType", MethodSignature.BID_ARGS, true);
	private static MethodSignature GET_BUNDLE_HEADER_METHOD = new MethodSignature("getBundleHeader", new String[] { "long", MethodSignature.STRING_TYPE, MethodSignature.STRING_TYPE }, true);
	private static MethodSignature GET_BUNDLE_START_LEVEL_METHOD = new MethodSignature("getBundleStartLevel", new String[] { "long" }, true);
	// should not serialize because resource could be big and sending will block the communication
	private static MethodSignature GET_BUNDLE_RESOURCE_METHOD = new MethodSignature("getBundleResource", new String[] { "long", MethodSignature.STRING_TYPE, Dictionary.class.getName() }, false);

	private Long id;
	private String location;
	public boolean uninstalled = false;
	private boolean isSystemBundle = false;

	private DeploymentManagerImpl commands;
	private boolean cachedSystemBundle = false;

	public RemoteBundleImpl(DeploymentManagerImpl deploymentCommands, Long id) {
		this(deploymentCommands, id, null);
	}

	public RemoteBundleImpl(DeploymentManagerImpl deploymentCommands, Long id, String location) {
		debug("[Constructor] >>> Creating new RemoteBundle: manager: "
						+ deploymentCommands
						+ "; id "
						+ id
						+ "; location "
						+ location);
		this.commands = deploymentCommands;
		this.id = id;
		this.location = location;
	}

	public long getBundleId() {
		return id.longValue();
	}

	public Dictionary getHeaders(String locale) throws IAgentException {
		debug("[getHeaders] >>> locale: " + locale);
		checkBundleState();
		Dictionary headers = (Dictionary) GET_BUNDLE_HEADERS_METHOD.call(getBundleAdmin(), new Object[] { id, locale });
		if (headers == null) {
			debug("[getHeaders] Bundle cannot be found on the remote site. Assuming it is uninstalled");
			uninstalled = true;
			checkBundleState(); // throw illegal state exception
		}
		debug("[getHeaders] result: " + DebugUtils.convertForDebug(headers));
		return headers;
	}

	public String getHeader(String headerName, String locale) throws IAgentException {
		debug("[getHeader] >>> headerName: " + headerName + "; locale: " + locale);
		checkBundleState();
		Object result = GET_BUNDLE_HEADER_METHOD.call(getBundleAdmin(), new Object[] { id,
			headerName,
			locale });
		if (result == null) {
			debug("[getHeader] No header with given method found");
			return null;
		} else if (result instanceof Error) {
			Error error = (Error) result;
			info("[getHeader] Failed to get header: " + error);
			checkBundleErrorResult(error);
			return null;
		} else {
			debug("[getHeader] Header value: " + result);
			return (String) result;
		}
	}

	private void checkBundleState() throws IAgentException {
		if (uninstalled) {
			debug("[checkBundleState] Remote bundle has been uninstalled");
			throw new IAgentException("Remote bundle has been uninstalled", IAgentErrors.ERROR_BUNDLE_UNINSTALLED);
		}
	}

	public String getLocation() throws IAgentException {
		debug("[getLocation] >>>");
		checkBundleState();
		if (location == null) {
			location = (String) GET_BUNDLE_LOCATION_METHOD.call(getBundleAdmin(), new Object[] { id });
			if (location == null) {
				debug("[getLocation] Bundle cannot be found on the remote site. Assuming it is uninstalled");
				uninstalled = true;
				checkBundleState();
				return null;
			}
		}
		debug("[getLocation] Bundle location: " + location);
		return location;
	}

	private RemoteObject getBundleAdmin() throws IAgentException {
		return commands.getBundleAdmin();
	}

	public int getState() throws IAgentException {
		if (uninstalled) {
			debug("[getState] bundle state: " + UNINSTALLED);
			return UNINSTALLED;
		}
		Integer state = (Integer) GET_BUNDLE_STATE_METHOD.call(getBundleAdmin(), new Object[] { id });
		if (state.intValue() == UNINSTALLED)
			uninstalled = true;
		debug("[getState] bundle state: " + state);
		return state.intValue();
	}

	public String getSymbolicName() throws IAgentException {
		debug("[getSymbolicName] >>>");
		checkBundleState();
		String symbolicName = (String) GET_BUNDLE_NAME_METHOD.call(getBundleAdmin(), new Object[] { id });
		if (symbolicName == null) { // the bundle is uninstalled
			debug("[getSymbolicName] Bundle cannot be found on the remote site. Assuming it was uninstalled");
			uninstalled = true;
			checkBundleState();
			return null;
		} else if (symbolicName.length() == 0) { // the bundle is R3
			info("[getSymbolicName] symbolic name: null");
			return null;
		} else { // everything is normal
			debug("[getSymbolicName] symbolic name: " + symbolicName);
			return symbolicName;
		}
	}

	public String getVersion() throws IAgentException {
		debug("[getVersion] >>>");
		checkBundleState();
		String headerValue = getHeader("Bundle-Version", "");
		debug("[getVersion] bundle version: " + headerValue);
		return headerValue != null ? headerValue.trim() : null;
	}

	public boolean resolve() throws IAgentException {
		debug("[resolve] >>> Trying to resolve bundle...");
		if (!uninstalled && getState() == UNINSTALLED)
			uninstalled = true; // check for uninstall before call
		checkBundleState();
		boolean resolvingResult = ((Boolean) RESOLVE_BUNDLES_METHOD.call(getBundleAdmin(),
			new Object[] { new long[] { id.longValue() } })).booleanValue();
		debug("[resolve] resolve status: " + resolvingResult);
		return resolvingResult;
	}

	public void start(int flags) throws IAgentException {
		debug("[start] >>> flags: " + flags);
		checkBundleState();
		Error error = (Error) START_BUNDLE_METHOD.call(getBundleAdmin(), new Object[] { id, new Integer(flags) });
		debug("[start] Bundle start result: " + error);
		checkBundleErrorResult(error);
	}

	public void stop(int flags) throws IAgentException {
		debug("[stop] flags: " + flags);
		checkBundleState();
		Error error = (Error) STOP_BUNDLE_METHOD.call(getBundleAdmin(), new Object[] { id, new Integer(flags) });
		debug("[stop] Bundle stop result: " + error);
		checkBundleErrorResult(error);
	}

	public void uninstall() throws IAgentException {
		debug("[uninstall] >>>");
		checkBundleState();
		Error err = (Error) UNINSTALL_BUNDLE_METHOD.call(getBundleAdmin(), new Object[] { id });
		debug("[uninstall] Bundle uninstallation result: " + err);
		if (err == null) {
			uninstalled = true;
			return;
		}
		checkBundleErrorResult(err);
	}

	public void update(InputStream in) throws IAgentException {
		debug("[update] >>> in: " + in);
		if (in == null) {
			throw new IllegalArgumentException();
		}
		checkBundleState();
		Error err = (Error) UPDATE_BUNDLE_METHOD.call(getBundleAdmin(), new Object[] { id, in });
		debug("[update] Bundle update result: " + err);
		checkBundleErrorResult(err);
	}

	private void checkBundleErrorResult(Error err) throws IAgentException {
		if (err == null)
			return;
		else if (err.getCode() == Error.BUNDLE_UNINSTALLED_CODE) {
			uninstalled = true;
			checkBundleState();
		} else {
			throw new IAgentException(err);
		}
	}

	public long getLastModified() throws IAgentException {
		debug("[getLastModified] >>>");
		checkBundleState();
		Long lastModified = (Long) GET_BUNDLE_LAST_MODIFIED_METHOD.call(getBundleAdmin(), new Object[] { id });
		if (lastModified.longValue() == -2) {
			debug("[getLastModified] remote call result: " + lastModified + " -> bundle is uninstalled");
			uninstalled = true;
			checkBundleState();
			return 0; // unreachable
		}
		debug("[getLastModified] bundle last modified: " + lastModified);
		return lastModified.longValue();
	}

	public RemoteService[] getRegisteredServices() throws IAgentException {
		debug("[getRegisteredServices] >>>");
		checkBundleState();
		Dictionary[] servicesProps = (Dictionary[]) GET_REGISTERED_SERVICES_METHOD.call(getBundleAdmin(),
			new Object[] { id });
		if (servicesProps == null) {
			debug("[getRegisteredServices] remote call result is: " + servicesProps + " -> bundle is uninstalled");
			uninstalled = true;
			checkBundleState();
		}
		RemoteService[] services = new RemoteService[servicesProps.length];
		for (int i = 0; i < servicesProps.length; i++) {
			services[i] = new RemoteServiceImpl((ServiceManagerImpl) commands.getDeviceConnector().getServiceManager(),
				servicesProps[i]);
		}
		debug("[getRegisteredServices] Registered services: " + DebugUtils.convertForDebug(services));
		return services;
	}

	public RemoteService[] getServicesInUse() throws IAgentException {
		debug("[getServicesInUse] >>>");
		checkBundleState();
		Dictionary[] servicesProps = (Dictionary[]) GET_USING_SERVICES_METHOD.call(getBundleAdmin(),
			new Object[] { id });
		if (servicesProps == null) {
			debug("[getServicesInUse] remote call result is: " + servicesProps + " -> bundle is uninstalled");
			uninstalled = true;
			checkBundleState();
		}
		RemoteService[] services = new RemoteService[servicesProps.length];
		for (int i = 0; i < servicesProps.length; i++) {
			services[i] = new RemoteServiceImpl((ServiceManagerImpl) commands.getDeviceConnector().getServiceManager(),
				servicesProps[i]);
		}
		debug("[getServicesInUse] In use services: " + DebugUtils.convertForDebug(services));
		return services;
	}

	public RemoteBundle[] getFragments() throws IAgentException {
		debug("[getFragments] >>>");
		checkBundleState();
		long[] fragmentBundleIDs = (long[]) GET_FRAGMENT_BUNDLES_METHOD.call(getBundleAdmin(), new Object[] { id });
		if (fragmentBundleIDs == null) {
			debug("[getFragments] remote call result is: " + fragmentBundleIDs + " -> bundle is uninstalled");
			uninstalled = true;
			checkBundleState();
		}
		if (fragmentBundleIDs.length == 0) {
			debug("[getFragments] No fragment bundles");
			return null;
		}
		RemoteBundle[] fragmentRemoteBundles = new RemoteBundle[fragmentBundleIDs.length];
		for (int i = 0; i < fragmentRemoteBundles.length; i++) {
			fragmentRemoteBundles[i] = commands.getBundle(fragmentBundleIDs[i]);
		}
		debug("[getFragments] Attached fragments: " + DebugUtils.convertForDebug(fragmentRemoteBundles));
		return fragmentRemoteBundles;
	}

	public RemoteBundle[] getHosts() throws IAgentException {
		debug("[getHosts] >>>");
		checkBundleState();
		long[] hostBundleIDs = (long[]) GET_HOST_BUNDLES_METHOD.call(getBundleAdmin(), new Object[] { id });
		if (hostBundleIDs == null) {
			debug("[getHosts] remote call result is: " + hostBundleIDs + " -> bundle is uninstalled");
			uninstalled = true;
			checkBundleState();
		}
		if (hostBundleIDs.length == 0) {
			debug("[getHosts] No host bundles");
			return null;
		}
		RemoteBundle[] hostRemoteBundles = new RemoteBundle[hostBundleIDs.length];
		for (int i = 0; i < hostRemoteBundles.length; i++) {
			hostRemoteBundles[i] = commands.getBundle(hostBundleIDs[i]);
		}
		debug("[getHosts] Hosts attached to: " + DebugUtils.convertForDebug(hostRemoteBundles));
		return hostRemoteBundles;
	}

	public int getType() throws IAgentException {
		debug("[getType] >>>");
		checkBundleState();
		Integer bundleType = (Integer) GET_BUNDLE_TYPE_METHOD.call(getBundleAdmin(), new Object[] { id });
		if (bundleType.intValue() == -1) {
			debug("[getType] remote call result is: " + bundleType + " -> bundle is uninstalled");
			uninstalled = true;
			checkBundleState();
		} else if (bundleType.intValue() == -2) {
			info("[getType] PackageAdmin is not available on the remote site");
			throw new IAgentException("PackageAdmin is not available on the remote site", 0);
		}
		debug("[getType] bundle type: " + bundleType);
		return bundleType.intValue();
	}

	private final void debug(String message) {
		DebugUtils.debug(this, message);
	}

	private final void info(String message) {
		DebugUtils.info(this, message);
	}

	public String toString() {
		return "RemoteBundle@" + Integer.toHexString(System.identityHashCode(this)) + "[" + id + "][" + location + "]";
	}

	public boolean isSystemBundle() throws IAgentException {
		if (!cachedSystemBundle) {
			isSystemBundle = commands.scanSystemBundlesList(this);
			cachedSystemBundle = true;
		}
		return isSystemBundle;
	}

	public int getBundleStartLevel() throws IAgentException {
		debug("[getBundleStartLevel] >>>");
		checkBundleState();
		Integer bundleStartLevel = (Integer) GET_BUNDLE_START_LEVEL_METHOD.call(getBundleAdmin(),
			new Object[] { id });
		return bundleStartLevel.intValue();
	}

	public InputStream getResource(String name) throws IAgentException {
		if (name == null) {
			throw new IllegalArgumentException();
		}
		if (!GET_BUNDLE_RESOURCE_METHOD.isDefined(getBundleAdmin())) {
			return null;
		}
		Object res = GET_BUNDLE_RESOURCE_METHOD.call(getBundleAdmin(), new Object[] { id, name, null });
		if (res instanceof Error) {
			checkBundleErrorResult((Error) res);
		} else if (res instanceof InputStream) {
			return (InputStream) res;
		} else if (res instanceof RemoteObject) {
			return new RemoteReader((RemoteObject) res);
		}
		return null;
	}

}
