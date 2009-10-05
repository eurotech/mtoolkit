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
package org.tigris.mtoolkit.iagent.internal;

import java.util.Map;

import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteApplication;

public class RemoteApplicationImpl implements RemoteApplication {

	private ApplicationManagerImpl appManager = null;
	private String applicationID = null;

	public RemoteApplicationImpl(ApplicationManagerImpl applicationManager, String applicationID) {
		this.applicationID = applicationID;
		this.appManager = applicationManager;
	}

	public void start(Map properties) throws IAgentException {
		appManager.startApplication(applicationID, properties);
	}

	public void stop() throws IAgentException {
		appManager.stopApplication(applicationID);
	}

	public String getApplicationId() throws IAgentException {
		return applicationID;
	}

	public String getState() throws IAgentException {
		return appManager.getApplicationState(applicationID);
	}
	
	public Map getProperties() throws IAgentException {
		return appManager.getApplicationProperties(applicationID);
	}

}
