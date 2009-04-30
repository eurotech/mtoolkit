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
package org.tigris.mtoolkit.iagent;

import org.tigris.mtoolkit.iagent.event.RemoteServiceListener;

/**
 * Enables quering the service registry of the remote OSGi runtime. It can
 * provide information about the registered services and notify interested
 * parties in remote service registry events.
 * 
 * @author Danail Nachev
 * 
 */
public interface ServiceManager {

	/**
	 * Adds listener which will be notified for service registry events. Adding
	 * the same listener when it has been already registered does nothing.
	 * 
	 * @param listener
	 *            the listener to be added
	 * @throws IAgentException
	 */
	public void addRemoteServiceListener(RemoteServiceListener listener) throws IAgentException;

	/**
	 * Removes listener to prevent it from receiving service registry events.
	 * Removing listeners which wasn't been registered does nothing.
	 * 
	 * @param listener
	 *            the listener to be removed
	 * @throws IAgentException
	 *             if the device is already disconnected
	 */
	public void removeRemoteServiceListener(RemoteServiceListener listener) throws IAgentException;

	/**
	 * Returns service registered in the remote OSGi runtime filtered by class
	 * and/or LDAP filter.<br>
	 * If the class filter passed is null, then no filtering based on interface
	 * name will be done.<br>
	 * If the LDAP filter is null, no additional filtering will be done.<br>
	 * If both are null, then all service available in the remote OSGi runtime
	 * will be returned.<br>
	 * If the LDAP filter doesn't have correct syntax, {@link IAgentException}
	 * will be thrown.<br>
	 * If class and LDAP filter are both non-null and LDAP filter is correct,
	 * then the result will be filtered by both criteria.
	 * 
	 * @param clazz
	 *            the interface name under which the services are registered. A
	 *            service, registered under multiple names, will be returned if
	 *            one of its interfaces are the same as the filter
	 * @param filter
	 *            LDAP filter as specified by OSGi specification
	 * @return an array containing references to remote services or empty array
	 *         in case there aren't any services which satisfies the criteria.
	 * @throws IAgentException
	 */
	public RemoteService[] getAllRemoteServices(String clazz, String filter) throws IAgentException;

}
