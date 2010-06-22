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

	public static boolean isRemoteMethodDefined(RemoteObject remote, MethodSignature methodSignature)
			throws IAgentException {
		try {
			return getRemoteMethod(remote, methodSignature) != null;
		} catch (PMPException e) {
			if (remote instanceof PMPRemoteObjectAdapter) {
				int verificationResult = ((PMPRemoteObjectAdapter) remote).verifyRemoteReference();
				if (verificationResult == PMPRemoteObjectAdapter.REPEAT) {
					debug("[isRemoteMethodDefined] Remote reference verification says REPEAT");
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

	private final static String formatRemoteMethodInformation(RemoteObject remote, MethodSignature methodSignature) {
		if (DebugUtils.DEBUG_ENABLED)
			return "RemoteObject: " + remote + "; methodName: " + methodSignature.name;
		else
			return "Debug not enabled";
	}

	private static Object callRemoteMethod0(RemoteObject remote, Object[] parameters, MethodSignature methodSignature)
			throws PMPException, IAgentException {
		RemoteMethod method = getRemoteMethod(remote, methodSignature);
		if (method == null)
			throw new IAgentException("Method " + methodSignature + " is not defined.",
					IAgentErrors.ERROR_INTERNAL_ERROR);
		Object result = method.invoke(parameters, methodSignature.shouldSerialize);
		if (DebugUtils.DEBUG_ENABLED)
			debug("[invokeCachedMethod] remote method invocation result: " + result);
		return result;
	}

	private static RemoteMethod getRemoteMethod(RemoteObject remote, MethodSignature methodSignature)
			throws PMPException {
		synchronized (methodSignature) {
			if (!validateMethodCache(remote, methodSignature)) {
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
	
	private static boolean validateMethodCache(RemoteObject object, MethodSignature signature) {
		if (signature.cachedMethod == null)
			return false;
		if (signature.cachedObject != object)
			return false;
		if (!(object instanceof PMPRemoteObjectAdapter))
			return true;
		return ((PMPRemoteObjectAdapter)object).validateCache(signature.cachedMethod);
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
