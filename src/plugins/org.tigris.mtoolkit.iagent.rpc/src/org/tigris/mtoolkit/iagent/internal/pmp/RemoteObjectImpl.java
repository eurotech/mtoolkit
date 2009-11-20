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

import org.tigris.mtoolkit.iagent.pmp.PMPConnection;
import org.tigris.mtoolkit.iagent.pmp.PMPException;
import org.tigris.mtoolkit.iagent.pmp.RemoteMethod;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;

/**
 * The client side representation of an object located on the Framework.
 * Instances of this class can be received from the methods
 * {@link PMPConnection#getReference PMPConnection.getReference} and
 * {@link RemoteMethod#invoke RemoteMethod.invoke}
 */
class RemoteObjectImpl implements RemoteObject {

	protected int IOR;
	protected Connection c;

	protected RemoteObjectImpl(int objID, Connection c) {
		this.c = c;
		IOR = objID;
	}

	/**
	 * Dynamically gets references to all the methods of the object associated
	 * with this RemoteObject.
	 * 
	 * @return The references to the object's methods.
	 * @exception PMPException
	 *                If an IOException or protocol error occurred.
	 */
	public RemoteMethod[] getMethods() throws PMPException {
		if (!c.connected)
			throw new PMPException("PMPConnection closed");
		return c.getMethods(this);
	}

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
	 *                if an IOException, if a protocol error occurred, or if
	 *                there is no such method.
	 */
	public RemoteMethod getMethod(String name, String[] args) throws PMPException {
		if (name == null || name.length() == 0)
			throw new PMPException("Incorrect method name");
		if (!c.connected)
			throw new PMPException("PMPConnection closed");
		return c.getMethod(this, name, args);
	}

	/**
	 * Disposes the resources allocated for the remote object so that it would
	 * be no longer usable.
	 * 
	 * @exception PMPException
	 *                if an IOException or protocol error occurred.
	 */
	public void dispose() throws PMPException {
		if (!c.connected)
			// if we are not connected, we don't have to dispose anything
			return;
		c.dispose(IOR);
	}

	protected void finalize() {
		if (c == null)
			return;
		try {
			c.dump("FINILIZE CALLED" + IOR);
			dispose();
		} catch (Exception exc) {
		}
	}
}
