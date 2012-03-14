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
package org.tigris.mtoolkit.iagent.rpc;

import java.util.Map;

import org.osgi.framework.BundleContext;

public interface RemoteApplicationAdmin extends RemoteServiceIDProvider {

	/**
	 * Returns all currently installed application from OSGI environment.
	 * 
	 * @return a string array with installed application identifiers.
	 */
	public String[] getApplications();

	/**
	 * Start the application that identifier match the applicationIP parameter
	 * with the specified from properties as startup parameters.
	 * 
	 * @param applicationID
	 *            - the ID of the application to be started
	 * @param properties
	 *            - the start up properties for launching this application.
	 * 
	 * @throws Exception
	 */
	public Object start(String applicationID, Map properties) throws Exception;

	/**
	 * Stop the application which identifier match the parameter -
	 * applicationID.
	 * 
	 * @param applicationID
	 *            - the ID of the application that is going to be stopped
	 */
	public Object stop(String applicationID);

	/**
	 * Returns the state of the application which ID match the parameter.
	 * 
	 * @param ApplicationId
	 *            - the ID of application from which state we are interested in
	 * @return - the state as string.
	 */
	public String getState(String applicationId);

	public Object getProperties(String applicationId);

	void unregister(BundleContext bc);
}
