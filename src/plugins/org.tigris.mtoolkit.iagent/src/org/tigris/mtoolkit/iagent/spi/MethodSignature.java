/**
 * 
 */
package org.tigris.mtoolkit.iagent.spi;

import java.util.Arrays;

import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.pmp.RemoteMethod;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;

public class MethodSignature {

	String name;
	String[] parameterTypes;
	boolean shouldSerialize;
	RemoteMethod cachedMethod;
	RemoteObject cachedObject;

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
		this(name, getClassNames(new Class[] { parameterType }));
	}
	
	private static String[] getClassNames(Class[] classes) {
		if (classes == null)
			return null;
		if (classes.length == 0)
			return new String[0];
		String[] classNames = new String[classes.length];
		for (int i = 0; i < classes.length; i++) {
			classNames[i] = classes[i].getName();
		}
		return classNames;
	}

	public MethodSignature(String name, String[] parameterNames, boolean shouldSerialize) {
		if (name == null)
			throw new IllegalArgumentException("name cannot be null");
		if (parameterNames != null)
			for (int i = 0; i < parameterNames.length; i++) {
				if (parameterNames[i] == null)
					throw new IllegalArgumentException("parameterNames contain null element at index " + i);
			}
		this.name = name;
		this.parameterTypes = parameterNames;
		if (this.parameterTypes == null)
			this.parameterTypes = new String[0];
		this.shouldSerialize = shouldSerialize;
	}

	public MethodSignature(RemoteMethod rMethod) {
		this(rMethod.getName(), rMethod.getArgTypes());
	}

	public String toString() {
		return name + "[" + DebugUtils.flattenStringArray(parameterTypes) + "]";
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + MethodSignature.hashCode(parameterTypes);
		return result;
	}

	private static int hashCode(Object[] array) {
		int prime = 31;
		if (array == null)
			return 0;
		int result = 1;
		for (int index = 0; index < array.length; index++) {
			result = prime * result + (array[index] == null ? 0 : array[index].hashCode());
		}
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MethodSignature other = (MethodSignature) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (!Arrays.equals(parameterTypes, other.parameterTypes))
			return false;
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
		return call(obj, new Object[] { parameter });
	}

	public Object call(RemoteObject obj, Object[] parameters) throws IAgentException {
		if (parameters == null)
			throw new IllegalArgumentException(
					"parameters array cannot be null, it must be empty if no args are passed");
		if (parameterTypes.length != parameters.length)
			throw new IllegalArgumentException("method signature expects " + parameterTypes.length + " arguments, but "
					+ parameters.length + " arguments are provided.");
		return Utils.callRemoteMethod(obj, this, parameters);
	}
	
	public boolean isDefined(RemoteObject obj) throws IAgentException {
		if (obj == null)
			throw new IllegalArgumentException("Remote object cannot be null");
		return Utils.isRemoteMethodDefined(obj, this);
	}

}