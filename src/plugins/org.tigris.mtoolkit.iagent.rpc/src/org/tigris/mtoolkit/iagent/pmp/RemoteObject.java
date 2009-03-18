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
 * The client side representation of an object located on the remote Framework.
 * Instances of this class can be received from the methods
 * {@link PMPConnection#getReference PMPConnection.getReference} and
 * {@link RemoteMethod#invoke RemoteMethod.invoke}
 */

public interface RemoteObject {

	/**
	 * Dynamically gets references to all the methods of the object associated
	 * with this RemoteObject.
	 * 
	 * @return The references to the object's methods.
	 * @exception PMPException
	 *                If an IOException or protocol error occured.
	 */

	public RemoteMethod[] getMethods() throws PMPException;

	/**
	 * Gets a reference to a method of the object associated with this
	 * RemoteObject.
	 * 
	 * @param name
	 *            the method's name
	 * @param args
	 *            the method's arguments types
	 * @return a reference to the requested method.
	 * @exception PMPException
	 *                if an IOException, if a protocol error occured, or if
	 *                there is no such method.
	 */
	public RemoteMethod getMethod(String name, String[] args) throws PMPException;

	/**
	 * Disposes the resources allocated for the remote object so that it would
	 * be no longer usable.
	 * 
	 * @exception PMPException
	 *                if an IOException or protocol error occured.
	 */
	public void dispose() throws PMPException;

}
