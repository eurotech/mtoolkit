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

import java.io.InputStream;

import org.osgi.framework.Bundle;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.iagent.RemoteDP;

public class DeploymentManagerTest extends DeploymentTestCase {

	public void testListDeploymentPackages() throws IAgentException {
		// assume that there are no deployment packages installed
		RemoteDP[] initialDPs = commands.listDeploymentPackages();
		assertNotNull(initialDPs);
		int initialDPCount = initialDPs.length;

		installDeploymentPackage("test.depl.p1_1.0.0.dp", "test.depl.p1");

		RemoteDP[] dps = commands.listDeploymentPackages();
		assertNotNull("The listDeploymentPackages() must not return null", dps);
		assertEquals("There must be one more dp available", initialDPCount + 1, dps.length);

		boolean found = false;
		for (int i = 0; i < dps.length; i++) {
			RemoteDP dp = dps[i];
			assertNotNull("There must be no null element in the returned array", dp);
			assertFalse("There should be no stale DP in the array", dp.isStale());
			if ("test.depl.p1".equals(dp.getName()) && "1.0.0".equals(dp.getVersion())) {
				found = true;
			}
		}

		assertTrue("The installed test package must be in the returned array", found);
	}

	public void testGetDeploymentPackage() throws IAgentException {
		installDeploymentPackage("test.depl.p1_1.0.0.dp", "test.depl.p1");

		RemoteDP dp = commands.getDeploymentPackage("test.depl.p1");
		assertNotNull(dp);
		assertEquals("test.depl.p1", dp.getName());
		assertEquals("1.0.0", dp.getVersion());

		// invalid parameters
		RemoteDP nonExistentDP = commands.getDeploymentPackage("non.existent.package");
		assertNull(nonExistentDP);

		try {
			commands.getDeploymentPackage(null);
			fail("IllegalArgumentException must be thrown when the passed name is null");
		} catch (IllegalArgumentException e) {
			// expected result
		}
	}

	public void testInstallDeploymentPackage() throws Exception {
		RemoteDP dp = installDeploymentPackage("test.depl.p1_1.0.0.dp", "test.depl.p1");
		assertNotNull("The result from calling installDeploymentPackage() should be non-null", dp);
		assertEquals("test.depl.p1", dp.getName());
		assertEquals("1.0.0", dp.getVersion());
		assertFalse("The installed package must not be stale", dp.isStale());

		try {
			commands.installDeploymentPackage(null);
			fail("IllegalArgumentException must be thrown");
		} catch (IllegalArgumentException e) {
			// expected result
		}
	}

	public void testListBundles() throws IAgentException {
		installBundle("test.bundle.b1_1.0.0.jar");

		RemoteBundle[] remoteBundles = commands.listBundles();
		assertNotNull("The listBundles() method must not return null", remoteBundles);

		boolean found = false;
		for (int i = 0; i < remoteBundles.length; i++) {
			RemoteBundle bundle = remoteBundles[i];
			assertNotNull("There must be no null element in the returned array", bundle);
			assertTrue("There must be no UNINSTALLED bundle in the returned array",
				bundle.getState() != Bundle.UNINSTALLED);
			assertTrue("There must be no bundle with ID equals to -1", bundle.getBundleId() != -1);
			if (bundle.getLocation().equals("dc-test:bundle:test.bundle.b1_1.0.0.jar")) {
				found = true;
			}
		}
		assertTrue("The installed bundle must be in the returned array", found);
	}

	public void testGetBundles() throws IAgentException {
		RemoteBundle installedBundle = installBundle("test.bundle.b1_1.0.0.jar");

		RemoteBundle[] bundle = commands.getBundles("test.bundle.b1", "1.0.0");
		assertNotNull("The bundle must be available", bundle);
		assertEquals("The returned array must contain only one element because we have specified version",
			1,
			bundle.length);
		assertEquals(bundle[0].getBundleId(), installedBundle.getBundleId());

		installBundle("test.bundle.b1_1.0.1.jar");

		bundle = commands.getBundles("test.bundle.b1", null);
		assertNotNull(bundle);
		String dpSwAdminEnable = connector.getVMManager().getSystemProperty("iagent.swadmin.deployment.support");
		dpSwAdminEnable = dpSwAdminEnable == null ? null : dpSwAdminEnable.trim();
		if (dpSwAdminEnable != null && dpSwAdminEnable.length() != 0 && dpSwAdminEnable.equals("true")) {
			assertEquals(1, bundle.length);
		} else {
			assertEquals(2, bundle.length);
		}
		bundle = commands.getBundles("unknown.bundle.b1", null);
		assertNull(bundle);

		try {
			commands.getBundles(null, null);
			fail("IllegalArgumentException must be thrown when the passed symbolic name is null");
		} catch (IllegalArgumentException e) {
			// expected result
		}
	}

	public void testInstallBundle() throws IAgentException {
		String location = "dc-test:testInstallBundle:bundle1";
		registerBundle(location);

		InputStream is = getClass().getClassLoader().getResourceAsStream("test.bundle.b1_1.0.0.jar");
		RemoteBundle bundle = commands.installBundle(location, is);

		assertNotNull(bundle);
		assertTrue(bundle.getBundleId() != -1);
		assertTrue(bundle.getState() != Bundle.UNINSTALLED);

		assertEquals("test.bundle.b1", bundle.getSymbolicName());
		assertEquals("1.0.0", bundle.getVersion());

		// get the bundle via list, to check the location
		RemoteBundle[] rBundles = commands.listBundles();
		boolean found = false;
		for (int i = rBundles.length - 1; i >= 0; i--) {
			if (rBundles[i].getBundleId() == bundle.getBundleId()) {
				assertEquals(rBundles[i].getLocation(), bundle.getLocation());
				found = true;
				break;
			}
		}
		assertTrue(found); // assert that the bundle is found

		is = getClass().getClassLoader().getResourceAsStream("test.bundle.b1_1.0.1.jar");
		RemoteBundle bundle101 = commands.installBundle(location, is); // try to
		// update
		assertNotNull(bundle101);
		assertEquals(bundle.getBundleId(), bundle101.getBundleId());
		assertEquals(bundle.getSymbolicName(), bundle101.getSymbolicName());
		assertEquals(bundle.getVersion(), bundle101.getVersion()); // they
		// should
		// have the
		// same
		// version
		// because
		// you
		// cannot
		// update
		// bundle
		// through
		// install

		try {
			commands.installBundle(null, is);
			fail("IllegalArgumentException must be thrown when no location is passed");
		} catch (IllegalArgumentException e) {
			// expected result
		}

		try {
			commands.installBundle("dummy.location", null);
			fail("IllegalArgumentException must be thrown when no InputStream is passed");
		} catch (IllegalArgumentException e) {
			// expected result
		}
	}
}
