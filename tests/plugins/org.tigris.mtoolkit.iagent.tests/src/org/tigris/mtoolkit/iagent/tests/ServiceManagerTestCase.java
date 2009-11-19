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
package org.tigris.mtoolkit.iagent.tests;

import java.util.ArrayList;
import java.util.List;

import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteService;
import org.tigris.mtoolkit.iagent.ServiceManager;
import org.tigris.mtoolkit.iagent.event.RemoteServiceListener;

public class ServiceManagerTestCase extends DeploymentTestCase {

	private ServiceManager serviceManager = null;
	private List services = null;

	public static final String TEST_SERVICE_CLASS = "com.prosyst.test.servicemanager.packages.register.TestService";

	protected void setUp() throws Exception {
		super.setUp();
		services = new ArrayList();
		serviceManager = connector.getServiceManager();
	}

	protected void tearDown() throws Exception {
		for (int j = 0; j < services.size(); j++) {
			try {
				serviceManager.removeRemoteServiceListener((RemoteServiceListener) services.get(j));
			} catch (IAgentException e) {
				e.printStackTrace();
			}
		}
		services.clear();
		services = null;
		super.tearDown();
	}

	public void addRemoteServiceListener(RemoteServiceListener listener) throws IAgentException {
		if (listener != null) {
			serviceManager.addRemoteServiceListener(listener);
			services.add(listener);
		}
	}

	public void removeRemoteServiceListener(RemoteServiceListener listener) throws IAgentException {
		if (listener != null) {
			serviceManager.removeRemoteServiceListener(listener);
			services.remove(listener);
		}
	}

	public RemoteService[] getAllRemoteServices(String clazz, String filter) throws IAgentException {
		return serviceManager.getAllRemoteServices(clazz, filter);
	}

}
