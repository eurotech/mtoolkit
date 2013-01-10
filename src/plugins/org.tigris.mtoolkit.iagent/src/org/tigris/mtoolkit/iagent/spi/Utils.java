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

public final class Utils {

  private Utils() {
  }

  public static boolean isRemoteMethodDefined(RemoteObject remote, MethodSignature methodSignature)
      throws IAgentException {
    try {
      return getRemoteMethod(remote, methodSignature) != null;
    } catch (PMPException e) {
      if (remote instanceof PMPRemoteObjectAdapter) {
        int verificationResult = ((PMPRemoteObjectAdapter) remote).verifyRemoteReference();
        if (verificationResult == PMPRemoteObjectAdapter.REPEAT) {
          DebugUtils.debug(Utils.class, "[isRemoteMethodDefined] Remote reference verification says REPEAT");
          try {
            return getRemoteMethod(remote, methodSignature) != null;
          } catch (PMPException e1) {
            DebugUtils.info(Utils.class, "[isRemoteMethodDefined] Failed to get method again", e1);
            return false;
          }
        }
      }
    }
    return false;
  }

  /**
   *
   * @param remote
   * @param methodSignature
   * @param parameters
   * @return
   * @throws IAgentException
   */
  static Object callRemoteMethod(RemoteObject remote, MethodSignature methodSignature, Object[] parameters)
      throws IAgentException {
    try {
      return callRemoteMethod0(remote, parameters, methodSignature);
    } catch (PMPException e) {
      DebugUtils.info(Utils.class, "[callRemoteMethod] Method invocation failed", e);
      if (remote instanceof PMPRemoteObjectAdapter) {
        int verificationResult = ((PMPRemoteObjectAdapter) remote).verifyRemoteReference();
        if (verificationResult == PMPRemoteObjectAdapter.REPEAT) {
          DebugUtils.debug(Utils.class, "[callRemoteMethod] Remote reference verification says REPEAT");
          try {
            return callRemoteMethod0(remote, parameters, methodSignature);
          } catch (PMPException e1) {
            DebugUtils.info(Utils.class, "[callRemoteMethod] Method invocation failed", e);
            throw new IAgentException("Unable to call method: " + methodSignature.name,
                IAgentErrors.ERROR_INTERNAL_ERROR, e1);
          }
        }
      }
      DebugUtils.info(Utils.class, "[callRemoteMethod] Method invocation failed", e);
      throw new IAgentException("Unable to call method: " + methodSignature.name, IAgentErrors.ERROR_INTERNAL_ERROR, e);
    }
  }

  private static Object callRemoteMethod0(RemoteObject remote, Object[] parameters, MethodSignature methodSignature)
      throws PMPException, IAgentException {
    RemoteMethod method = getRemoteMethod(remote, methodSignature);
    if (method == null) {
      throw new IAgentException("Method " + methodSignature + " is not defined.", IAgentErrors.ERROR_INTERNAL_ERROR);
    }
    Object result = method.invoke(parameters, methodSignature.shouldSerialize);
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(Utils.class, "[invokeCachedMethod] remote method invocation result: " + result);
    }
    return result;
  }

  private static RemoteMethod getRemoteMethod(RemoteObject remote, MethodSignature methodSignature) throws PMPException {
    synchronized (methodSignature) {
      if (!validateMethodCache(remote, methodSignature)) {
        DebugUtils.debug(Utils.class, "[invokeCachedMethod] Method wasn't found in the cache. Quering remote site...");
        // we don't want to left behind inconsistency cache, if the getMethod fails
        methodSignature.cachedMethod = null;
        methodSignature.cachedObject = remote;
        methodSignature.cachedMethod = remote.getMethod(methodSignature.name, methodSignature.parameterTypes);
      }
      return methodSignature.cachedMethod;
    }
  }

  private static boolean validateMethodCache(RemoteObject object, MethodSignature signature) {
    if (signature.cachedMethod == null) {
      return false;
    }
    if (signature.cachedObject != object) {
      return false;
    }
    if (!(object instanceof PMPRemoteObjectAdapter)) {
      return true;
    }
    return ((PMPRemoteObjectAdapter) object).validateCache(signature.cachedMethod);
  }
}
