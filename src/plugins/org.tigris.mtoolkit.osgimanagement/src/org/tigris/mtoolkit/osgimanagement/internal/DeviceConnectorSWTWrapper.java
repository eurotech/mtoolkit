package org.tigris.mtoolkit.osgimanagement.internal;

import java.io.Serializable;
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
import org.tigris.mtoolkit.iagent.DeviceConnectionListener;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.ServiceManager;
import org.tigris.mtoolkit.iagent.VMManager;
import org.tigris.mtoolkit.iagent.event.RemoteDevicePropertyListener;
import org.tigris.mtoolkit.iagent.spi.AbstractConnection;
import org.tigris.mtoolkit.iagent.spi.ConnectionManager;
import org.tigris.mtoolkit.iagent.spi.DeviceConnectorSpi;

public class DeviceConnectorSWTWrapper extends DeviceConnector implements DeviceConnectorSpi {
	
	private DeviceConnector delegate;
	private Thread displayThread;
	
	public DeviceConnectorSWTWrapper(DeviceConnector delegate, Display display) {
		this.delegate = delegate;
		this.displayThread = display.getThread();
	}

	public void addRemoteDevicePropertyListener(RemoteDevicePropertyListener listener) throws IAgentException {
		delegate.addRemoteDevicePropertyListener(listener);
	}

	public void closeConnection() throws IAgentException {
		checkThread();
		delegate.closeConnection();
	}
	
	private void checkThread() {
		if (Thread.currentThread() == displayThread)
			FrameworkPlugin.warning("Access to IAgent API in UI thread is detected", new Exception("Access to IAgent API in UI thread is detected"));
	}

	public DeploymentManager getDeploymentManager() throws IAgentException {
		checkThread();
		return (DeploymentManager) wrapObject(delegate.getDeploymentManager());
	}

	public Object getManager(String className) throws IAgentException {
		checkThread();
		return wrapObject(delegate.getManager(className));
	}

	public Dictionary getProperties() {
		checkThread();
		return delegate.getProperties();
	}

	public ServiceManager getServiceManager() throws IAgentException {
		checkThread();
		return (ServiceManager) wrapObject(delegate.getServiceManager());
	}

	public VMManager getVMManager() throws IAgentException {
		checkThread();
		return (VMManager) wrapObject(delegate.getVMManager());
	}

	public boolean isActive() {
		return delegate.isActive();
	}

	public void removeRemoteDevicePropertyListener(RemoteDevicePropertyListener listener) throws IAgentException {
		delegate.removeRemoteDevicePropertyListener(listener);
	}

	private Object wrapObject(Object delegate) {
		if (delegate == null)
			return delegate;
		if (isJavaClasslibDescendant(delegate.getClass()))
			return delegate;
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
			if (interfaces.length == 0)
				return delegate;
			return Proxy.newProxyInstance(DeviceConnector.class.getClassLoader(), interfaces, new DelegatingHandler(delegate));
		}
	}
	
	private boolean isJavaClasslibDescendant(Class clazz) {
		while (clazz != null) {
			if (clazz.getName().startsWith("java"))
				return true;
			clazz = clazz.getSuperclass();
		}
		return false;
	}
	
	private Class[] removeDuplicates(Class[] interfaces) {
		Set allInterfaces = new HashSet();
		allInterfaces.addAll(Arrays.asList(interfaces));
		return (Class[]) allInterfaces.toArray(new Class[allInterfaces.size()]);
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof DeviceConnectorSWTWrapper)
			return delegate.equals(((DeviceConnectorSWTWrapper) obj).delegate);
		return delegate.equals(obj);
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	private class DelegatingHandler implements InvocationHandler {

		private Object delegate;
		
		public DelegatingHandler(Object delegate) {
			this.delegate = delegate;
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			checkThread();
			String methodName = method.getName();
			Class[] parameterTypes = method.getParameterTypes();
			Object result = ReflectionUtils.invokeProtectedMethod(delegate, methodName, parameterTypes, args);
			return wrapObject(result);
		}
	}
	
	public ConnectionManager getConnectionManager() {
		return (ConnectionManager) wrapObject(((DeviceConnectorSpi) delegate).getConnectionManager());
	}

	public DeviceConnector getDeviceConnector() {
		return this;
	}
}
