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

	private DeploymentManagerImpl dManager = null;
	private String applicationID = null;

	public RemoteApplicationImpl(DeploymentManagerImpl deploymentManager, String applicationID) {
		this.applicationID = applicationID;
		this.dManager = deploymentManager;
	}

	public void start(Map properties) throws IAgentException {
		dManager.startApplication(applicationID, properties);
	}

	public void stop() throws IAgentException {
		dManager.stopApplication(applicationID);
	}

	public String getApplicationId() throws IAgentException {
		return applicationID;
	}

	public String getState() throws IAgentException {
		return dManager.getApplicationState(applicationID);
	}

}
