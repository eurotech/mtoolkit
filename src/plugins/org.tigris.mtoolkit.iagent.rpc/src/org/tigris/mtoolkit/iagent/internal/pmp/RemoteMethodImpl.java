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

import org.tigris.mtoolkit.iagent.pmp.PMPException;
import org.tigris.mtoolkit.iagent.pmp.RemoteMethod;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;

/**
 * The client side representation of a method of an object located on the
 * Framework. Instances of this class can be received from the methods
 * {@link RemoteObject#getMethod RemoteObject.getMethod} and
 * {@link RemoteObject#getMethods RemoteObject.getMethods}.
 */

class RemoteMethodImpl implements RemoteMethod {

	/**
	 * The method's name
	 */
	private String name;
	/**
	 * The method's return type
	 */
	private String returnType;
	/**
	 * The method's arguments' types
	 */
	private String[] argTypes;

	private Connection c;
	private int methodID;
	private RemoteObjectImpl ro;
	private boolean changed = false;
	private ClassLoader loader;

	protected RemoteMethodImpl(String name, String returnType, String[] argTypes, Connection c, int methodID,
			RemoteObjectImpl ro) {
		this.c = c;
		this.methodID = methodID;
		this.ro = ro;
		this.name = name;
		this.returnType = returnType;
		this.argTypes = argTypes;
	}

	/**
	 * Executes the method on the server.
	 * 
	 * @param args
	 *            parameters' values
	 * @param serflag
	 *            specifies how the method result should be returned. If
	 *            <code>true</code>, return as Serializable. Else return as
	 *            {@link RemoteObject RemoteObject}. If the return type is void
	 *            this parameter is ignored.
	 * @exception PMPException
	 *                If an IOException occUred or if the mEthod thrown an
	 *                exception on the Framework.
	 */
	public Object invoke(Object[] args, boolean serflag, Class clazz) throws PMPException {
		if (!c.connected)
			throw new PMPException("PMP Service stoped");
		if (clazz != null) {
			loader = clazz.getClassLoader();
		}
		return c.invoke(args, argTypes, serflag, ro.IOR, methodID, returnType, loader, changed, c);
	}

	public Object invoke(Object[] args, boolean serflag) throws PMPException {
		if (!c.connected)
			throw new PMPException("PMP Service stoped");
		return c.invoke(args, argTypes, serflag, ro.IOR, methodID, returnType, loader, changed, c);
	}

	/**
	 * Changes the return type of the method. This can be used to read the
	 * method's result in a different object than the original return type.
	 * 
	 * @param newReturnType
	 *            The new return type class name.
	 */
	public void changeReturnType(String newReturnType) {

		this.returnType = newReturnType;
		this.loader = null;
		changed = true;
	}

	/**
	 * Changes the return type of the method. Use this method if the new return
	 * type can't be found in the system class path.
	 * 
	 * @param newReturnType
	 *            The new return type class name.
	 */
	public void changeReturnType(Class newReturnType) {
		this.returnType = newReturnType.getName();
		this.loader = newReturnType.getClassLoader();
		changed = true;
	}

	public String getName() {
		return name;
	}

	public String[] getArgTypes() {
		return argTypes;
	}

	public String toString() {
		String s = "REMOTE METHOD: " + name + " " + returnType; //$NON-NLS-1$ //$NON-NLS-2$
		for (int i = 0; i < argTypes.length; i++) {
			s += " " + argTypes[i]; //$NON-NLS-1$
		}
		return s;
	}
}
