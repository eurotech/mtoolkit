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

import java.util.Map;

import org.osgi.framework.Constants;
import org.tigris.mtoolkit.iagent.RemoteApplication;
import org.tigris.mtoolkit.iagent.RemoteBundle;

public class RemoteApplicationTest extends ApplicationTestCase {

	public void testProperties() throws Exception {
		RemoteBundle oneAppBundle = installBundle(ApplicationTestCase.BUNDLE_ONE_APP_NAME);

		RemoteApplication app = getRemoteApplication("FirstApplication");

		Map props = app.getProperties();
    assertNotNull("The result from calling properties of the ApplicationDescriptor should be non-null", props);
    assertEquals("Service's persistent identifier should be 'FirstApplication'", "FirstApplication",
        props.get(Constants.SERVICE_PID));
    assertEquals("The bundle.id property should be equals of the bundle id of installed bundle",
        new Long(oneAppBundle.getBundleId()), props.get("bundle.id"));
	}

	public void testState() throws Exception {
		RemoteBundle oneAppBundle = installBundle(ApplicationTestCase.BUNDLE_ONE_APP_NAME);

		RemoteApplication app = getRemoteApplication("FirstApplication");

    assertEquals("The result of calling getState() should be INSTALLED", "INSTALLED", app.getState());
		app.start(null);
    assertEquals("The result of calling getState() should be RUNNING", "RUNNING", app.getState());
		app.stop();
    assertEquals("The result of calling getState() should be INSTALLED", "INSTALLED", app.getState());
		oneAppBundle.uninstall(null);
    assertEquals("The result of calling getState() should be UNINSTALLED", "UNINSTALLED", app.getState());
	}
}
