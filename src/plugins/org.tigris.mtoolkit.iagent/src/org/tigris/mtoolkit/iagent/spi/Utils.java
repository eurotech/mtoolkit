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
package org.tigris.mtoolkit.iagent.spi;

import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.internal.tcp.PMPRemoteObjectAdapter;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.pmp.PMPException;
import org.tigris.mtoolkit.iagent.pmp.RemoteMethod;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;

public class Utils {

	private static final String STRING_TYPE = "java.lang.String";
	private static final String INPUT_STREAM_TYPE = "java.io.InputStream";

	private static final String[] NO_ARGS = new String[0];
	private static final String[] BID_ARGS = new String[] { "long" };
	private static final String[] SERVICEID_ARGS = new String[] { "long" };

	// TODO: Change the way remote method calls are made, use
	// MethodSignature.call() instead of this class

	private static MethodSignature[] METHOD_SIGNATURES = {
			new MethodSignature("installBundle", new String[] { STRING_TYPE, INPUT_STREAM_TYPE }, true),
			new MethodSignature("getBundleState", BID_ARGS, true),
			new MethodSignature("getBundleLastModified", BID_ARGS, true),
			new MethodSignature("getBundleHeaders", new String[] { "long", STRING_TYPE }, true),
			new MethodSignature("getBundleLocation", BID_ARGS, true),
			new MethodSignature("startBundle", new String[] { "long", "int" }, true),
			new MethodSignature("stopBundle", new String[] { "long", "int" }, true),
			new MethodSignature("updateBundle", new String[] { "long", INPUT_STREAM_TYPE }, true),
			new MethodSignature("uninstallBundle", BID_ARGS, true),
			new MethodSignature("getBundleSymbolicName", BID_ARGS, true),
			new MethodSignature("getBundleVersion", BID_ARGS, true),
			new MethodSignature("resolveBundles", new String[] { long[].class.getName() }, true),
			new MethodSignature("listBundles", NO_ARGS, true),
			new MethodSignature("getBundles", new String[] { STRING_TYPE, STRING_TYPE }, true),

			new MethodSignature("listDeploymentPackages", NO_ARGS, true),
			new MethodSignature("getDeploymentPackageHeader", new String[] { STRING_TYPE, STRING_TYPE, STRING_TYPE },
					true),
			new MethodSignature("getDeploymentPackageBundles", new String[] { STRING_TYPE, STRING_TYPE }, true),
			new MethodSignature("getDeploymentPackageBundle", new String[] { STRING_TYPE, STRING_TYPE, STRING_TYPE },
					true),
			new MethodSignature("uninstallDeploymentPackage", new String[] { STRING_TYPE, STRING_TYPE, "boolean" },
					true),
			new MethodSignature("isDeploymentPackageStale", new String[] { STRING_TYPE, STRING_TYPE }, true),
			new MethodSignature("getDeploymentPackageVersion", new String[] { STRING_TYPE }, true),
			new MethodSignature("installDeploymentPackage", new String[] { INPUT_STREAM_TYPE }, true),

			new MethodSignature("registerOutput", new String[] { RemoteObject.class.getName() }, true),
			new MethodSignature("executeCommand", new String[] { STRING_TYPE }, true),
			new MethodSignature("releaseConsole", NO_ARGS, true),

			new MethodSignature("getAllRemoteServices", new String[] { STRING_TYPE, STRING_TYPE }, true),
			new MethodSignature("getObjectClass", SERVICEID_ARGS, true),
			new MethodSignature("getProperties", SERVICEID_ARGS, true),
			new MethodSignature("getUsingBundles", SERVICEID_ARGS, true),
			new MethodSignature("getBundle", SERVICEID_ARGS, true),
			new MethodSignature("isServiceStale", SERVICEID_ARGS, true),
			new MethodSignature("checkFilter", new String[] { STRING_TYPE }, true),

			// additional methods to Bundle Admin
			new MethodSignature("getRegisteredServices", BID_ARGS, true),
			new MethodSignature("getUsingServices", BID_ARGS, true),
			new MethodSignature("getFragmentBundles", BID_ARGS, true),
			new MethodSignature("getHostBundles", BID_ARGS, true),
			new MethodSignature("getBundleType", BID_ARGS, true),

			// additional method for getting single header
			new MethodSignature("getBundleHeader", new String[] { "long", STRING_TYPE, STRING_TYPE }, true),

			// method for identifying the remote admins
			new MethodSignature("getRemoteServiceID", NO_ARGS, true),

			// method for getting a bundle by location
			new MethodSignature("getBundleByLocation", new String[] { STRING_TYPE }, true),

			// method from RemoteBundleAdmin for retreiving agentData
			new MethodSignature("getAgentData", NO_ARGS, true),
			new MethodSignature("getSystemBundlesIDs", NO_ARGS, true),

			// methods for getting the bundle and framework start levels
			new MethodSignature("getBundleStartLevel", new String[] { "long" }, true),
			new MethodSignature("getFrameworkStartLevel", NO_ARGS, true),
			new MethodSignature("getSystemProperty", new String[] { STRING_TYPE }, true), 
	};

	public static final int INSTALL_BUNDLE_METHOD = 0;
	public static final int GET_BUNDLE_STATE_METHOD = 1;
	public static final int GET_BUNDLE_LAST_MODIFIED_METHOD = 2;
	public static final int GET_BUNDLE_HEADERS_METHOD = 3;
	public static final int GET_BUNDLE_LOCATION_METHOD = 4;
	public static final int START_BUNDLE_METHOD = 5;
	public static final int STOP_BUNDLE_METHOD = 6;
	public static final int UPDATE_BUNDLE_METHOD = 7;
	public static final int UNINSTALL_BUNDLE_METHOD = 8;
	public static final int GET_BUNDLE_NAME_METHOD = 9;
	public static final int GET_BUNDLE_VERSION_METHOD = 10;
	public static final int RESOLVE_BUNDLES_METHOD = 11;
	public static final int LIST_BUNDLES_METHOD = 12;
	public static final int GET_BUNDLES_METHOD = 13;

	public static final int LIST_DPS_METHOD = 14;
	public static final int GET_DP_HEADER_METHOD = 15;
	public static final int GET_DP_BUNDLES_METHOD = 16;
	public static final int GET_DP_BUNDLE_METHOD = 17;
	public static final int UNINSTALL_DP_METHOD = 18;
	public static final int IS_DP_STALE_METHOD = 19;
	public static final int GET_DP_VERSION_METHOD = 20;
	public static final int INSTALL_DP_METHOD = 21;

	public static final int REGISTER_METHOD = 22;
	public static final int EXECUTE_METHOD = 23;
	public static final int RELEASE_METHOD = 24;

	public static final int GET_ALL_REMOTE_SERVICES_METHOD = 25;
	public static final int GET_OBJECT_CLASS_METHOD = 26;
	public static final int GET_PROPERTIES_METHOD = 27;
	public static final int GET_USING_BUNDLES_METHOD = 28;
	public static final int GET_BUNDLE_METHOD = 29;
	public static final int IS_SERVICE_STALE_METHOD = 30;
	public static final int CHECK_FILTER_METHOD = 31;

	public static final int GET_REGISTERED_SERVICES_METHOD = 32;
	public static final int GET_USING_SERVICES_METHOD = 33;
	public static final int GET_FRAGMENT_BUNDLES_METHOD = 34;
	public static final int GET_HOST_BUNDLES_METHOD = 35;
	public static final int GET_BUNDLE_TYPE_METHOD = 36;

	public static final int GET_BUNDLE_HEADER_METHOD = 37;

	public static final int GET_REMOTE_SERVICE_ID_METHOD = 38;

	public static final int GET_BUNDLE_BY_LOCATION_METHOD = 39;

	public static final int GET_AGENT_DATA_METHOD = 40;
	public static final int GET_SYSTEM_BUNDLES_IDS_METHOD = 41;

	public static final int GET_BUNDLE_START_LEVEL_METHOD = 42;
	public static final int GET_FW_START_LEVEL = 43;

	public static final int GET_SYSTEM_PROPERTY = 44;

	public static final int LAST = 44;

	static {
		if (METHOD_SIGNATURES.length != LAST + 1) {
			error("ERROR: There is a mismatch between the method constants and the array holding the method descriptions: [expected: "
							+ METHOD_SIGNATURES.length + ", actual: " + (LAST + 1) + "]");
		}
	}

	public static int[] addMethodSignatures(MethodSignature[] signatures) {
		if (signatures == null)
			return null;
		int[] result = new int[signatures.length];
		MethodSignature[] newSignatures = new MethodSignature[METHOD_SIGNATURES.length + signatures.length];
		int i = 0;
		for (; i < METHOD_SIGNATURES.length; i++) {
			newSignatures[i] = METHOD_SIGNATURES[i];
		}
		for (int j = 0; j < signatures.length; j++) {
			newSignatures[i + j] = signatures[j];
			result[j] = i + j;
		}
		METHOD_SIGNATURES = newSignatures;
		return result;
	}

	/**
	 * 
	 * @param remote
	 * @param method
	 * @param parameters
	 * @return
	 * @throws IAgentException
	 * @deprecated Using this method result in too verbose code. Consider using
	 *             {@link MethodSignature#call(RemoteObject)} or one of its
	 *             siblings instead.
	 */
	public static Object callRemoteMethod(RemoteObject remote, int method, Object[] parameters) throws IAgentException {
		MethodSignature methodSignature = METHOD_SIGNATURES[method];
		debug("[callRemoteMethod] >>> " + formatRemoteMethodInformation(remote, method, methodSignature));
		return callRemoteMethod(remote, methodSignature, parameters);
	}

	/**
	 * 
	 * @param remote
	 * @param methodSignature
	 * @param parameters
	 * @return
	 * @throws IAgentException
	 * @deprecated Using this method result in too verbose code. Consider using
	 *             {@link MethodSignature#call(RemoteObject)} or one of its
	 *             siblings instead.
	 */
	public static Object callRemoteMethod(RemoteObject remote, MethodSignature methodSignature, Object[] parameters)
			throws IAgentException {
		try {
			return callRemoteMethod0(remote, parameters, methodSignature);
		} catch (PMPException e) {
			info("[callRemoteMethod] Method invocation failed", e);
			if (remote instanceof PMPRemoteObjectAdapter) {
				int verificationResult = ((PMPRemoteObjectAdapter) remote).verifyRemoteReference();
				if (verificationResult == PMPRemoteObjectAdapter.REPEAT) {
					debug("[callRemoteMethod] Remote reference verification says REPEAT");
					clearCache(remote);
					try {
						return callRemoteMethod0(remote, parameters, methodSignature);
					} catch (PMPException e1) {
						info("[callRemoteMethod] Method invocation failed", e);
						throw new IAgentException("Unable to call method: " + methodSignature.name,
								IAgentErrors.ERROR_INTERNAL_ERROR, e1);
					}
				}
			}
			info("[callRemoteMethod] Method invocation failed", e);
			throw new IAgentException("Unable to call method: " + methodSignature.name,
					IAgentErrors.ERROR_INTERNAL_ERROR, e);
		}
	}

	public static boolean isRemoteMethodDefined(RemoteObject remote, int method) throws IAgentException {
		MethodSignature methodSignature = METHOD_SIGNATURES[method];
		debug("[isRemoteDefined] >>> " + formatRemoteMethodInformation(remote, method, methodSignature));
		return isRemoteMethodDefined(remote, methodSignature);
	}

	public static boolean isRemoteMethodDefined(RemoteObject remote, MethodSignature methodSignature)
			throws IAgentException {
		try {
			return getRemoteMethod(remote, methodSignature) != null;
		} catch (PMPException e) {
			if (remote instanceof PMPRemoteObjectAdapter) {
				int verificationResult = ((PMPRemoteObjectAdapter) remote).verifyRemoteReference();
				if (verificationResult == PMPRemoteObjectAdapter.REPEAT) {
					debug("[isRemoteMethodDefined] Remote reference verification says REPEAT");
					clearCache(remote);
					try {
						return getRemoteMethod(remote, methodSignature) != null;
					} catch (PMPException e1) {
						info("[isRemoteMethodDefined] Failed to get method again", e1);
						return false;
					}
				}
			}
		}
		return false;
	}

	private final static String formatRemoteMethodInformation(RemoteObject remote, int method,
			MethodSignature methodSignature) {
		return "RemoteObject: " + remote + "; methodNumber: " + method + "; methodName: " + methodSignature.name;
	}

	private static Object callRemoteMethod0(RemoteObject remote, Object[] parameters, MethodSignature methodSignature)
			throws PMPException, IAgentException {
		RemoteMethod method = getRemoteMethod(remote, methodSignature);
		if (method == null)
			throw new IAgentException("Method " + methodSignature + " is not defined.",
					IAgentErrors.ERROR_INTERNAL_ERROR);
		Object result = method.invoke(parameters, methodSignature.shouldSerialize);
		debug("[invokeCachedMethod] remote method invocation result: " + result);
		return result;
	}

	private static RemoteMethod getRemoteMethod(RemoteObject remote, MethodSignature methodSignature)
			throws PMPException {
		synchronized (methodSignature) {
			if (methodSignature.cachedMethod == null || methodSignature.cachedObject != remote) {
				debug("[invokeCachedMethod] Method wasn't found in the cache. Quering remote site...");
				// we don't want to left behind incosistence cache, if the
				// getMethod fails
				methodSignature.cachedMethod = null;
				methodSignature.cachedObject = remote;
				methodSignature.cachedMethod = remote.getMethod(methodSignature.name, methodSignature.parameterTypes);
			}
			return methodSignature.cachedMethod;
		}
	}

	public static void clearCache() {
		debug("[clearCache] >>>");
		for (int i = 0; i < METHOD_SIGNATURES.length; i++) {
			METHOD_SIGNATURES[i].cachedMethod = null;
		}
	}

	public static void clearCache(RemoteObject object) {
		debug("[clearCache] >>> RemoteObject:" + object);
		for (int i = 0; i < METHOD_SIGNATURES.length; i++) {
			if (METHOD_SIGNATURES[i].cachedObject == object) {
				METHOD_SIGNATURES[i].cachedMethod = null;
				METHOD_SIGNATURES[i].cachedObject = null;
			}
		}
	}

	private static final void debug(String message) {
		DebugUtils.debug(Utils.class, message);
	}

	private static final void info(String message, Throwable t) {
		DebugUtils.info(Utils.class, message, t);
	}

	private static final void error(String message) {
		DebugUtils.error(Utils.class, message);
	}
}
