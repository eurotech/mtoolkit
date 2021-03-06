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

import java.util.Map;

/**
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface PMPServer extends PMPPeer {

	public void close();

	public boolean isActive();

	/**
	 * Sends an event to the EventCollector
	 * 
	 * @param ev
	 *            The event
	 * @param eventType
	 *            The event's type
	 * @param bid
	 *            The id of the bundle that the event source belongs to
	 */
	public void event(Object ev, String eventType);

	/**
	 * Returns a Map that contains server properties.
	 * 
	 * @return
	 * @since 3.0
	 */
	public Map getProperties();

}
