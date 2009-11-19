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
		assertNotNull(props);
		assertEquals("FirstApplication", props.get(Constants.SERVICE_PID));
		assertEquals(new Long(oneAppBundle.getBundleId()), props.get("bundle.id"));
	}
	
	public void testState() throws Exception {
		RemoteBundle oneAppBundle = installBundle(ApplicationTestCase.BUNDLE_ONE_APP_NAME);
		
		RemoteApplication app = getRemoteApplication("FirstApplication");
		
		assertEquals("INSTALLED", app.getState());
		app.start(null);
		assertEquals("RUNNING", app.getState());
		app.stop();
		assertEquals("INSTALLED", app.getState());
		oneAppBundle.uninstall();
		assertEquals("UNINSTALLED", app.getState());
	}
}
