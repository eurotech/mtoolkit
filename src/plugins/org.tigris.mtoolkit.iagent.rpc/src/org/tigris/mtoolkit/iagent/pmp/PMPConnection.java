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
 * Contains the methods for getting references to the services registered in the
 * remote Framework.
 */
public interface PMPConnection {

	public static final String FRAMEWORK_DISCONNECTED = "framework_disconnected_event";

	/**
	 * Gets reference to a service registered in the Framework.
	 * 
	 * @param clazz
	 *            Specifies the interface under which the service was regisered.
	 * @param filter
	 *            Specifies the search filter (exactly as
	 *            <code>BundleContext.getService</code> filter)
	 * @return a reference to the service.
	 * @exception PMPException
	 *                If an IOException or protocol error occured, if the user
	 *                does not have access rigths for this service, or if there
	 *                was no such service registered in the Framework.
	 * @see RemoteObject
	 */

	public RemoteObject getReference(String clazz, String filter) throws PMPException;

	/**
	 * Gets reference to a service registered in the Framework.
	 * 
	 * @param clazz
	 *            Specifies the interface under which the service was regisered.
	 * @param filter
	 *            Specifies the search filter (exactly as
	 *            <code>BundleContext getService</code> filter)
	 * @param bid
	 *            The id of the bundle that registered the service.
	 * @return a reference to the service.
	 * @exception PMPException
	 *                If an IOException or protocol error occured, if the user
	 *                does not have access rigths for this service, or if there
	 *                was no such service registered in the Framework.
	 * @see RemoteObject
	 */
	public RemoteObject getReference(String clazz, String filter, long bid) throws PMPException;

	/**
	 * Disconnects this PMPConnection from the remote Framework
	 * 
	 * @param message
	 *            The dissconnection message
	 */

	public void disconnect(String message);

	/**
	 * Checks whenever this this connection is bind
	 * 
	 * @return remote pmpservice uri
	 */

	public boolean isConnected();

	/**
	 * Gets the identification of this session
	 * 
	 * @return SessionID associated with this session
	 */

	public String getSessionID();

	/**
	 * Registers an EventListener
	 * 
	 * @param el
	 *            the EventListener
	 * @param eventTypes
	 *            Array with elements all types of the events which this
	 *            listener wants to receive. Custom event types are in the
	 *            fallowing format EventListener.CUSTOM_EVENT=custom_event_type;
	 * 
	 * @exception IllegalArgumentException
	 */

	public void addEventListener(EventListener el, String[] eventTypes) throws IllegalArgumentException;
	
	/**
	 * Unregisters an EventListener
	 * 
	 * @param el
	 *            the EventListener
	 * @param eventTypes
	 *            Array with elements all types of the events which this
	 *            listener wants to receive. Custom event types are in the
	 *            fallowing format EventListener.CUSTOM_EVENT=custom_event_type;
	 * 
	 * @exception IllegalArgumentException
	 */
	public void removeEventListener(EventListener el, String[] eventTypes) throws IllegalArgumentException;

}
