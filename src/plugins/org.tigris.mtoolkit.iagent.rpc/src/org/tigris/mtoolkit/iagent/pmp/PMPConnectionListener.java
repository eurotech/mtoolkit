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
 * Listener used to receicve events whenever a remote Framework is connected or
 * disconnected to this PMPService. Implementations of this class must be
 * registered {@link PMPService#addPMPConnListener
 * PMPService.addPMPConnListener} and unregistered
 * {@link PMPService#removePMPConnListener PMPService.removePMPConnListener}
 */

public interface PMPConnectionListener {

	/**
	 * Method fired from PMPService when a remote client is connected to the
	 * PMPService.
	 * 
	 * @param connection
	 *            Connection to the remote Framework
	 */

	public void clientConnected(PMPConnection connection);

	/**
	 * Method fired from PMPService when a remote clientis disconnected from the
	 * PMPService.
	 * 
	 * @param connection
	 *            the clients connection
	 */

	public void clientDisconnected(PMPConnection connection);

}
