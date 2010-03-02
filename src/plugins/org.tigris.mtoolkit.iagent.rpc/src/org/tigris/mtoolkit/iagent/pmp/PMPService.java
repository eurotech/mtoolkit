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

import java.util.Dictionary;
import java.util.Map;

import org.tigris.mtoolkit.iagent.transport.Transport;

/**
 * Interface of the PMP Service
 */

public interface PMPService extends PMPPeer {

	/**
	 * Tries to establish a connection to remote PMP Service.
	 * 
	 * @param uri
	 *            of the remote PMPService host uri is in the following format
	 *            transportprotocol://host:port
	 * @param login
	 *            the login certificates
	 * @return PMPConnection
	 * @exception PMPException
	 *                If an IOException, protocol or authentication error
	 *                occured.
	 */

	public PMPConnection connect(Transport transport, Dictionary conProperties) throws PMPException;

}
