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

/**
 * Listeners implementing this interface will be notified for changes in the
 * state of the underlying connections.
 * 
 * @author Danail Nachev
 * 
 */
public interface ConnectionListener {

	/**
	 * Called when a change in the state of a connection occur
	 * 
	 * @param event
	 */
	public void connectionChanged(ConnectionEvent event);

}
