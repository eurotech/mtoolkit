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
	 * Registeres an event type at the event collector. After an event type has
	 * been registered listeners can register for recieving events of this type.
	 * 
	 * @param eventType
	 *            The event type to register
	 */
	public void addEventSource(String eventType);

	/**
	 * Unregisteres an event type from the event collector. All listeners
	 * registered for receiving events of this type will recieve a notification
	 * for this unregistration.
	 * 
	 * @param eventType
	 *            The event type to unregister.
	 */
	public void removeEventSource(String eventType);

}
