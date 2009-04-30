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

import java.util.Dictionary;

/**
 * Represents remote service, available on the remote OSGi runtime. It enables
 * to retrieve the information about the registered service.<br>
 * 
 * The clients of this interface cannot manipulate the registered service.<br>
 * 
 * If the VM is restarted, all {@link RemoteService} objects retrieved becomes
 * invalid.
 * 
 * @author Danail Nachev
 * 
 */
public interface RemoteService {

	/**
	 * Returns an array containing the interfaces under which the service is
	 * registered.
	 * 
	 * @return an array containing one or more interface names
	 * @throws IAgentException
	 */
	public String[] getObjectClass() throws IAgentException;

	/**
	 * Returns an unique identifier of the service.
	 * 
	 * @return the id of the service as found in the service.id property
	 * @throws IAgentException
	 */
	public long getServiceId() throws IAgentException;

	/**
	 * Returns the properties of the registered service. Because they are
	 * transfered via the wire they are converted, so the client of this API
	 * should depend on the class of the returned value.<br>
	 * 
	 * The following conversion rules are used:<br>
	 * <ol>
	 * <li>If the value is one of the types: all primitive types and their
	 * wrappers, java.lang.String, then the value is returned unchanged.</li>
	 * <li>If the value is array of one of the types listed in the first step,
	 * then the value is returned unchanged.</li>
	 * <li>If the value is a Collection (implements java.util.Collection
	 * interface) and all of its elements are instances of one the classes
	 * listed in step 1, then the collection is returned unchanged.</li>
	 * <li>If the value is a array or Collection and at least one of its
	 * elements isn't instance of one of the classes listed in the first step,
	 * then the value returned is array of java.lang.String with the same size
	 * and its elements are the corresponding results from toString() method
	 * call on each of the original collection elemenents.</li>
	 * <li>The resulting String object from toString() method call on the value
	 * is returned.</li>
	 * </ol>
	 * 
	 * @return key-value pairs containing the properties of the service
	 * @throws IAgentException
	 */
	public Dictionary getProperties() throws IAgentException;

	/**
	 * Returns an array containing the bundles which are using the service. If
	 * there aren't any users of the service or the service has been
	 * unregistered, empty array will be returned.
	 * 
	 * @return an array containing the users of the service or empty array if
	 *         there aren't any users or the service is unregistered
	 * @throws IAgentException
	 */
	public RemoteBundle[] getUsingBundles() throws IAgentException;

	/**
	 * Returns the bundle registered the service or null if the service has been
	 * unregistered.
	 * 
	 * @return the bundle registered the service or null if the service has been
	 *         unregistered
	 * @throws IAgentException
	 */
	public RemoteBundle getBundle() throws IAgentException;

	/**
	 * Returns wether the service has been unregistered
	 * 
	 * @return true if the service has been unregistered, false if the service
	 *         is still available
	 * @throws IAgentException
	 */
	public boolean isStale() throws IAgentException;
}
