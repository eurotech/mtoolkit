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

import org.tigris.mtoolkit.iagent.ApplicationManager;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteApplication;
import org.tigris.mtoolkit.iagent.RemoteBundle;

public class ApplicationTestCase extends DeploymentTestCase {

	private static final String BUNDLE_APPS_LISTENER_NAME = "test.bundle.listener.jar";
	protected static final String BUNDLE_ONE_APP_NAME = "test.bundle.one.app.register.jar";
	protected ApplicationManager appManager;
	private RemoteBundle bundleAppsListener;

	public ApplicationTestCase() {
		super();
	}

	protected void setUp() throws Exception {
		super.setUp();
		appManager = (ApplicationManager) connector.getManager(ApplicationManager.class.getName());
    assertNotNull("The result from calling getManager() should be non-null", appManager);
		bundleAppsListener = installBundle(BUNDLE_APPS_LISTENER_NAME);
		bundleAppsListener.start(0);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	protected RemoteApplication getRemoteApplication(String appId) throws IAgentException {
		RemoteApplication[] apps = appManager.listApplications();
		RemoteApplication app = null;
		for (int i = 0; i < apps.length; i++) {
			if (apps[i].getApplicationId().equals(appId)) {
				app = apps[i];
				break;
			}
		}
		return app;
	}

}