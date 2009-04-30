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
package org.tigris.mtoolkit.iagent.internal.connection;

import org.tigris.mtoolkit.iagent.IAgentException;

/**
 * Centralized body for managing connections. It provides methods for getting
 * active connections, creating new connections and adding/removing listeners. <br>
 * 
 * Registered listeners will be notified for changes in the state of the managed
 * connections.
 * 
 * @author Danail Nachev
 * 
 */
public interface ConnectionManager {

	public static final int PMP_CONNECTION = 0x1;

	public AbstractConnection getActiveConnection(int type) throws IAgentException;

	/**
	 * Create new connection of given type. If the connection is unable to be
	 * created, exception will be thrown. The method don't return null in any
	 * case. In case of unknown type, {@link IllegalArgumentException} will be
	 * thrown.
	 * 
	 * @param type
	 *            the type of the connection.
	 * @return AbstractConnection object. The client should upcast it to the
	 *         proper type
	 * @throws IAgentException
	 * @throws {@link IllegalArgumentException} if the type passed in is unknown
	 */
	public AbstractConnection createConnection(int type) throws IAgentException;

	/**
	 * Close all active connections
	 * 
	 * @throws IAgentException
	 */
	public void closeConnections() throws IAgentException;

	/**
	 * Add ConnectionListener, to be notified for connection events. If the
	 * passed listener is already present, nothing will be done
	 * 
	 * @param listener
	 * @see ConnectionListener
	 */
	public void addConnectionListener(ConnectionListener listener);

	/**
	 * Remove ConnectionListener. If the passed listener is not present, nothing
	 * will be done.
	 * 
	 * @param listener
	 */
	public void removeConnectionListener(ConnectionListener listener);

}
