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
package org.tigris.mtoolkit.iagent.internal.pmp;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Vector;

/** Records the info for a single remote object */
public final class ObjectInfo {

	/** all the Method-s of the remote object represented by this ObjectInfo */
	protected Vector methods;
	/** the remote object */
	protected Object obj;
	/** the remote interfaces */
	protected Class[] interfaces;

	// TODO: Update comments
	/**
	 * The ServiceReference associated with this remote object. If the remote
	 * object has been obtained with the getReference request, this is the
	 * ServiceReference from which the object has been obtained. Otherwise this
	 * is the ServiceReference of the remote object from which this remote
	 * object is derived. This is field is used when a service registered as a
	 * remote object is unregistered from the Framework - to unregister all
	 * remote objects derived from it.
	 */

	protected Object context;

	/**
	 * creates a new ObjectInfo instance. its private so the only way to get an
	 * ObjectInfo instance is the newInfo method
	 */
	protected ObjectInfo(Object obj, Class[] interfaces, Object context) {
		this.obj = obj;
		this.interfaces = interfaces;
		this.context = context;
	}

	/**
	 * creates a new ObjectInfo instance. its private so the only way to get an
	 * ObjectInfo instance is the newInfo method
	 */
	protected ObjectInfo(Object obj, Class[] interfaces) {
		this.obj = obj;
		this.interfaces = interfaces;
	}

	/** Returns the methodID of the specified Method */
	public int getMethod(String name, String[] argTypes, Method[] mArr) throws Exception {
		Class[] argtypes = new Class[argTypes.length];
		for (int i = 0; i < argTypes.length; i++) {
			argtypes[i] = getClass(argTypes[i]);
		}
		Method m = null;
		if (interfaces == null) {
			throw new NoSuchMethodException(name);
		}
		for (int i = 0; i < interfaces.length; i++) {
			try {
				m = interfaces[i].getMethod(name, argtypes);
			} catch (Exception ex) {
			}
		}
		if (m == null) {
			throw new NoSuchMethodException(name);
		}
		mArr[0] = m;
		if (methods == null)
			methods = new Vector(2, 5);
		for (int i = 0; i < methods.size(); i++) {
			if (m.equals(methods.elementAt(i)))
				return i + 1;
		}
		methods.addElement(m);
		return methods.size();
	}

	/** returns all the Method-s of the remote object */
	public Vector getMethods() {
		if (methods == null)
			methods = new Vector();
		if (interfaces != null) {
			for (int i = 0; i < interfaces.length; i++) {
				Method[] allMethods = interfaces[i].getMethods();
				for (int j = 0; j < allMethods.length; j++) {
					if (!methods.contains(allMethods[j])) {
						methods.addElement(allMethods[j]);
					}
				}
			}
		}
		return methods;
	}

	/** maps a class name to a java.lang.Class */
	// XXX: Doesn't support multidimensional arrays
	private Class getClass(String name) throws ClassNotFoundException {
		if (name.charAt(0) == '[') {
			if (name.length() == 2) // primitive array -> can be loaded from
				// anywhere
				return Class.forName(name);
			// array of reference -> load the array component type, create array
			// and get the class
			String componentType = name.substring(2);
			Class componentClass = getClass(componentType);
			return Array.newInstance(componentClass, 0).getClass();
		}
		// not an array
		return name.equals(PMPData.TYPES1[0])	? int.class
												: name.equals(PMPData.TYPES1[4]) ? long.class
																				: name.equals(PMPData.TYPES1[3]) ? short.class
																												: name.equals(PMPData.TYPES1[1]) ? byte.class
																																				: name.equals(PMPData.TYPES1[2]) ? char.class
																																												: name.equals(PMPData.TYPES1[5]) ? float.class
																																																				: name.equals(PMPData.TYPES1[6]) ? double.class
																																																												: name.equals(PMPData.TYPES1[8]) ? void.class
																																																																				: name.equals(PMPData.TYPES1[7]) ? boolean.class
																																																																												: obj.getClass().getClassLoader() == null	? Class.forName(name)
																																																																																							: obj.getClass().getClassLoader().loadClass(name);
	}
	/** puts an ObjectInfo instance in the free objects' queue */
	public void freeInfo() {
		methods = null;
		obj = null;
		context = null;
		interfaces = null;
	}
}
