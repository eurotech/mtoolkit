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

import java.util.Map;

/**
 * Represents remote application, installed in the device OSGi runtime. It
 * enables client to check it state and perform the standard operations - start
 * and stop.
 */
public interface RemoteApplication {

	public final static String STATE_RUNNING = "RUNNING";

	public final static String STATE_STOPPING = "STOPPING";
	
	public final static String STATE_INSTALLED = "INSTALLED";

	public final static String STATE_UNINSTALLED = "UNINSTALLED";

	public final static String STATE_MIXED = "MIXED";
	
	public final static String STATE_STARTING = "org.eclipse.equinox.app.starting";

	/**
	 * Starts the application.
	 * 
	 * @param properties
	 *            - The properties parameter specifies the startup parameters
	 *            for the application instance to be launched, it may be null.
	 */
	public void start(Map properties) throws IAgentException;

	/**
	 * Stop the application. After call this method the application state should
	 * become STOPPING.
	 */
	public void stop() throws IAgentException;

	/**
	 * Returns the identifier of the represented application.
	 * 
	 * @return application identifier.
	 */
	public String getApplicationId() throws IAgentException;

	/**
	 * Returns the current state of the application.
	 * 
	 * @return a String representation of application state.
	 */
	public String getState() throws IAgentException;

	/**
	 * Returns the properties of the ApplicationDescriptor.
	 * 
	 * @return
	 * @throws IAgentException
	 */
	public Map getProperties() throws IAgentException;

}
