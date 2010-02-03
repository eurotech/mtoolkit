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
package org.tigris.mtoolkit.iagent.spi;

import java.util.Dictionary;

import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.transport.Transport;

/**
 * Extenders which provide additional connections shall implement this interface
 */
public interface ExtConnectionFactory {

	/**
	 * Returns true if connections, created by this factory can be used to
	 * connect to controller.
	 * 
	 * @return
	 */
	public boolean isControllerType();

	/**
	 * Returns the type of connections this factory creates.
	 * 
	 * @return
	 */
	public int getConnectionType();

	/**
	 * Creates connection using the passed transport.
	 * 
	 * @param transport
	 * @param connProperties
	 * @param connManager
	 * @throws IAgentException
	 *             if connection cannot be established
	 */
	public AbstractConnection createConnection(Transport transport, Dictionary connProperties,
			ConnectionManager connManager) throws IAgentException;

}
