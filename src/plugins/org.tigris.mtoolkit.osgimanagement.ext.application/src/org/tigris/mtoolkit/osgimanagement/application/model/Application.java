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
package org.tigris.mtoolkit.osgimanagement.application.model;

import org.osgi.service.application.ApplicationDescriptor;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteApplication;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public class Application extends Model {
	
	private RemoteApplication remoteApplication;
	
	public Application(String name, RemoteApplication remoteApplication) {
		super(name);
		this.remoteApplication = remoteApplication;
		try {
			String localizedName = (String) remoteApplication.getProperties().get(ApplicationDescriptor.APPLICATION_NAME);
			if (localizedName != null) {
				setName(localizedName);
			}
		} catch (IAgentException e) {
			e.printStackTrace();
		}
	}
	
	public RemoteApplication getRemoteApplication() {
		return remoteApplication;
	}
}
