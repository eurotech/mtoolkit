/*******************************************************************************
 * Copyright (c) 2005, 2012 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.osgimanagement.internal;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.widgets.Display;
import org.tigris.mtoolkit.common.ReflectionUtils;
import org.tigris.mtoolkit.iagent.DeploymentManager;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.ServiceManager;
import org.tigris.mtoolkit.iagent.VMManager;
import org.tigris.mtoolkit.iagent.event.RemoteDevicePropertyListener;
import org.tigris.mtoolkit.iagent.spi.ConnectionManager;
import org.tigris.mtoolkit.iagent.spi.DeviceConnectorSpi;

public final class DeviceConnectorSWTWrapper extends DeviceConnector implements DeviceConnectorSpi {
  private DeviceConnector delegate;
  private Thread          displayThread;

  public DeviceConnectorSWTWrapper(DeviceConnector delegate, Display display) {
    this.delegate = delegate;
    this.displayThread = display.getThread();
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeviceConnector#addRemoteDevicePropertyListener(org.tigris.mtoolkit.iagent.event.RemoteDevicePropertyListener)
   */
  @Override
  public void addRemoteDevicePropertyListener(RemoteDevicePropertyListener listener) throws IAgentException { // NO_UCD
    delegate.addRemoteDevicePropertyListener(listener);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeviceConnector#removeRemoteDevicePropertyListener(org.tigris.mtoolkit.iagent.event.RemoteDevicePropertyListener)
   */
  @Override
  public void removeRemoteDevicePropertyListener(RemoteDevicePropertyListener listener) throws IAgentException { // NO_UCD
    delegate.removeRemoteDevicePropertyListener(listener);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeviceConnector#closeConnection()
   */
  @Override
  public void closeConnection() throws IAgentException {
    checkThread();
    delegate.closeConnection();
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeviceConnector#getDeploymentManager()
   */
  @Override
  public DeploymentManager getDeploymentManager() throws IAgentException {
    checkThread();
    return (DeploymentManager) wrapObject(delegate.getDeploymentManager());
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeviceConnector#getManager(java.lang.String)
   */
  @Override
  public Object getManager(String className) throws IAgentException {
    checkThread();
    return wrapObject(delegate.getManager(className));
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeviceConnector#getProperties()
   */
  @Override
  public Dictionary getProperties() {
    return delegate.getProperties();
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeviceConnector#getRemoteProperties()
   */
  @Override
  public Dictionary getRemoteProperties() throws IAgentException {
    checkThread();
    return delegate.getRemoteProperties();
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeviceConnector#getServiceManager()
   */
  @Override
  public ServiceManager getServiceManager() throws IAgentException {
    checkThread();
    return (ServiceManager) wrapObject(delegate.getServiceManager());
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeviceConnector#getVMManager()
   */
  @Override
  public VMManager getVMManager() throws IAgentException {
    checkThread();
    return (VMManager) wrapObject(delegate.getVMManager());
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeviceConnector#isActive()
   */
  @Override
  public boolean isActive() {
    return delegate.isActive();
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.DeviceConnectorSpi#getConnectionManager()
   */
  public ConnectionManager getConnectionManager() {
    return (ConnectionManager) wrapObject(((DeviceConnectorSpi) delegate).getConnectionManager());
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.DeviceConnectorSpi#getDeviceConnector()
   */
  public DeviceConnector getDeviceConnector() {
    return this;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof DeviceConnectorSWTWrapper) {
      return delegate.equals(((DeviceConnectorSWTWrapper) obj).delegate);
    }
    return delegate.equals(obj);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  private void checkThread() {
    if (Thread.currentThread() == displayThread) {
      FrameworkPlugin.warning("Access to IAgent API in UI thread is detected", new Exception(
          "Access to IAgent API in UI thread is detected"));
    }
  }

  private Object wrapObject(Object delegate) {
    if (delegate == null) {
      return delegate;
    }
    if (isJavaClasslibDescendant(delegate.getClass())) {
      return delegate;
    }
    if (delegate.getClass().isArray()) {
      int length = Array.getLength(delegate);
      Object result = Array.newInstance(delegate.getClass().getComponentType(), length);
      Object[] arr = (Object[]) delegate;
      for (int i = 0; i < arr.length; i++) {
        Array.set(result, i, wrapObject(arr[i]));
      }
      return result;
    } else {
      Class[] interfaces = delegate.getClass().getInterfaces();
      interfaces = removeDuplicates(interfaces);
      if (interfaces.length == 0) {
        return delegate;
      }
      return Proxy
          .newProxyInstance(DeviceConnector.class.getClassLoader(), interfaces, new DelegatingHandler(delegate));
    }
  }

  private boolean isJavaClasslibDescendant(Class clazz) {
    while (clazz != null) {
      if (clazz.getName().startsWith("java")) {
        return true;
      }
      clazz = clazz.getSuperclass();
    }
    return false;
  }

  private Class[] removeDuplicates(Class[] interfaces) {
    Set allInterfaces = new HashSet();
    allInterfaces.addAll(Arrays.asList(interfaces));
    return (Class[]) allInterfaces.toArray(new Class[allInterfaces.size()]);
  }

  private final class DelegatingHandler implements InvocationHandler {
    private Object delegate;

    public DelegatingHandler(Object delegate) {
      this.delegate = delegate;
    }

    /* (non-Javadoc)
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      checkThread();
      String methodName = method.getName();
      Class[] parameterTypes = method.getParameterTypes();
      Object result = ReflectionUtils.invokeProtectedMethod(delegate, methodName, parameterTypes, args);
      return wrapObject(result);
    }
  }

}
