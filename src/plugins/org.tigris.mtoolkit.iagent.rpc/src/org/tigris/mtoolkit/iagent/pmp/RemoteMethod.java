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
package org.tigris.mtoolkit.iagent.pmp;

/**
 * The client side representation of a method of an object located on the
 * Framework. Instances of this class can be received from the methods
 * {@link RemoteObject#getMethod RemoteObject.getMethod} and
 * {@link RemoteObject#getMethods RemoteObject.getMethods}.
 */

public interface RemoteMethod {

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
	 * @param loader
	 *            the class to load return type. If this parameter is null then
	 *            the default class loading is used.
	 * @exception PMPException
	 *                If an IOException occred or if the method throwed an
	 *                exception on the Framework.
	 */
	public Object invoke(Object[] args, boolean serflag, Class loader) throws PMPException;

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
	 *                If an IOException occred or if the method throwed an
	 *                exception on the Framework.
	 */
	public Object invoke(Object[] args, boolean serflag) throws PMPException;

	/**
	 * Changes the return type of the method. This can be used to read the
	 * method's result in a different object than the original return type.
	 * 
	 * @param newReturnType
	 *            The new return type class name.
	 */
	public void changeReturnType(String newReturnType);

	/**
	 * Changes the return type of the method. Use this method if the new return
	 * type can't be found in the system class path.
	 * 
	 * @param newReturnType
	 *            The new return type class name.
	 */
	public void changeReturnType(Class newReturnType);

	/**
	 * returns the name of the method.
	 */
	public String getName();

	/**
	 * returns String array with classnames of all argumens of this method.
	 */
	public String[] getArgTypes();

	/**
	 * Overwrites java.lang.Object toString() method.
	 */

	public String toString();

	public RemoteObject getRemoteObject();
}
