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
package org.tigris.mtoolkit.iagent.spi;

import java.util.Arrays;

import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.pmp.RemoteMethod;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;

public final class MethodSignature {
  /**
   * @since 4.0
   */
  public static final String   INPUT_STREAM_TYPE = "java.io.InputStream";
  /**
   * @since 4.0
   */
  public static final String   STRING_TYPE       = "java.lang.String";

  /**
   * @since 4.0
   */
  public static final String[] NO_ARGS           = new String[0];
  /**
   * @since 4.0
   */
  public static final String[] BID_ARGS          = new String[] {
                                                   "long"
                                                 };
  /**
   * @since 4.0
   */
  public static final String[] SERVICEID_ARGS    = new String[] {
                                                   "long"
                                                 };

  String                       name;
  String[]                     parameterTypes;
  boolean                      shouldSerialize;
  RemoteMethod                 cachedMethod;
  RemoteObject                 cachedObject;

  public MethodSignature(String name) {
    this(name, new String[0], true);
  }

  public MethodSignature(String name, String[] parameterNames) {
    this(name, parameterNames, true);
  }

  public MethodSignature(String name, Class[] classes) {
    this(name, getClassNames(classes));
  }

  public MethodSignature(String name, Class parameterType) {
    this(name, getClassNames(new Class[] {
      parameterType
    }));
  }

  public MethodSignature(String name, String[] parameterNames, boolean shouldSerialize) {
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }
    if (parameterNames != null) {
      for (int i = 0; i < parameterNames.length; i++) {
        if (parameterNames[i] == null) {
          throw new IllegalArgumentException("parameterNames contain null element at index " + i);
        }
      }
    }
    this.name = name;
    this.parameterTypes = parameterNames;
    if (this.parameterTypes == null) {
      this.parameterTypes = new String[0];
    }
    this.shouldSerialize = shouldSerialize;
  }

  public MethodSignature(RemoteMethod rMethod) {
    this(rMethod.getName(), rMethod.getArgTypes());
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return name + "[" + DebugUtils.flattenStringArray(parameterTypes) + "]";
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + MethodSignature.hashCode(parameterTypes);
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    MethodSignature other = (MethodSignature) obj;
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    if (!Arrays.equals(parameterTypes, other.parameterTypes)) {
      return false;
    }
    return true;
  }

  public String getName() {
    return name;
  }

  public String[] getParameterTypes() {
    return parameterTypes;
  }

  public Object call(RemoteObject obj) throws IAgentException {
    return call(obj, new Object[0]);
  }

  public Object call(RemoteObject obj, Object parameter) throws IAgentException {
    return call(obj, new Object[] {
      parameter
    });
  }

  public Object call(RemoteObject obj, Object[] parameters) throws IAgentException {
    if (parameters == null) {
      throw new IllegalArgumentException("parameters array cannot be null, it must be empty if no args are passed");
    }
    if (parameterTypes.length != parameters.length) {
      throw new IllegalArgumentException("method signature expects " + parameterTypes.length + " arguments, but "
          + parameters.length + " arguments are provided.");
    }
    return Utils.callRemoteMethod(obj, this, parameters);
  }

  public boolean isDefined(RemoteObject obj) throws IAgentException {
    if (obj == null) {
      throw new IllegalArgumentException("Remote object cannot be null");
    }
    return Utils.isRemoteMethodDefined(obj, this);
  }

  private static int hashCode(Object[] array) {
    int prime = 31;
    if (array == null) {
      return 0;
    }
    int result = 1;
    for (int index = 0; index < array.length; index++) {
      result = prime * result + (array[index] == null ? 0 : array[index].hashCode());
    }
    return result;
  }

  private static String[] getClassNames(Class[] classes) {
    if (classes == null) {
      return null;
    }
    if (classes.length == 0) {
      return new String[0];
    }
    String[] classNames = new String[classes.length];
    for (int i = 0; i < classes.length; i++) {
      classNames[i] = classes[i].getName();
    }
    return classNames;
  }
}
