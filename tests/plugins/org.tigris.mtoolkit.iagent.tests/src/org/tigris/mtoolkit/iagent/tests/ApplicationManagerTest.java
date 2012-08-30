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

import org.tigris.mtoolkit.iagent.RemoteApplication;
import org.tigris.mtoolkit.iagent.RemoteBundle;

public class ApplicationManagerTest extends ApplicationTestCase {

	public void testListApplications() throws Exception {
		RemoteApplication[] apps = appManager.listApplications();
		assertNotNull("listApplications() must not return null result", apps);
		int initialLength = apps.length;
		
		RemoteBundle oneAppBundle = installBundle(BUNDLE_ONE_APP_NAME);
		
		apps = appManager.listApplications();
		assertNotNull("listApplications() must not return null result", apps);
		int newLength = apps.length;
		assertEquals("Installed one application, it must show in the applications list", initialLength + 1, newLength);
		
		boolean found = false;
		for (int i = 0; i < apps.length; i++) {
			assertNotNull("All applications must have ID", apps[i].getApplicationId());
			if (apps[i].getApplicationId().equals("FirstApplication")) {
				assertFalse("Our application must be listed only once", found);
				found = true;
			}
		}
		assertTrue("Our application must have been found", found);
		
		oneAppBundle.uninstall(null);
		apps = appManager.listApplications();
		assertNotNull("listApplications() must not return null result", apps);
		int lengthAfterUninstall = apps.length;
		assertEquals("Application was uninstalled, available applications number must be restored", initialLength, lengthAfterUninstall);
		
		found = false;
		for (int i = 0; i < apps.length; i++) {
			assertNotNull("All applications must have ID", apps[i].getApplicationId());
			if (apps[i].getApplicationId().equals("FirstApplication")) {
				found = true;
			}
		}
		assertFalse("Uninstalled application must not be available", found);
	}
	
}
