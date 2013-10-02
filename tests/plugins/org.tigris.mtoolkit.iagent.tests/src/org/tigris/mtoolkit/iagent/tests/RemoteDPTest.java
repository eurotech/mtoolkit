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

import java.util.Dictionary;

import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteDP;

public class RemoteDPTest extends DeploymentTestCase {

	private RemoteDP dp;

	protected void setUp() throws Exception {
		super.setUp();
		dp = installDeploymentPackage("test.depl.p1_1.0.0.dp");
	}

	protected void tearDown() throws Exception {
		if (dp != null && !dp.isStale()) {
			try {
				dp.uninstall(true);
			} catch (Exception e) {
				// ignore
			}
		}
		super.tearDown();
	}

	public void testGetNameVersion() throws IAgentException {
    assertEquals("The bundle name should be test.depl.p1", "test.depl.p1", dp.getName());
    assertEquals("The bundle version should be 1.0.0", "1.0.0", dp.getVersion());
	}

	public void testGetHeader() throws IAgentException {
    assertEquals("Get OtherHeader should be 'some string'", "some string", dp.getHeader("OtherHeader"));
    assertEquals("Get DeploymentPackage-Vendor should be 'prosyst'", "prosyst",
        dp.getHeader("DeploymentPackage-Vendor"));
    assertNull("Get Non-Existent-Header header should be null", dp.getHeader("Non-Existent-Header"));

		try {
			dp.getHeader(null);
			fail("IllegalArgumentException must be thrown in case null header name is passed");
		} catch (IllegalArgumentException e) {
			// expected result
		}
	}

	public void testGetBundles() throws IAgentException {
		Dictionary bundles = dp.getBundles();
		assertNotNull("The bundles returned should be non-null", bundles);
    assertEquals("The bundles size should be 1", 1, bundles.size());

		String bundleSN = (String) bundles.keys().nextElement();
    assertEquals("The bundle symbolic name shoud be 'test.depl.b1'", "test.depl.b1", bundleSN);
		String bundleVer = (String) bundles.get(bundleSN);
    assertEquals("The bundle version should be 1.0.0", "1.0.0", bundleVer);

	}

	public void testUninstall() throws IAgentException {
    assertTrue("The result calling uninstall(false) should be successful", dp.uninstall(false));
    assertTrue("The result calling isStale() should be true, because deployment package is uninstalled", dp.isStale());
	}

	public void testUninstallForced() throws IAgentException {
    assertTrue("The result calling uninstall(true) should be successful", dp.uninstall(true));
    assertTrue("The result calling isStale() should be true, because deployment package is uninstalled", dp.isStale());
	}

	public void testStaleExceptions() throws IAgentException {
		dp.uninstall(true);
    assertTrue("The result calling isStale() should be true, because deployment package is uninstalled", dp.isStale());

		try {
			dp.getBundle("test.depl.b1");
			fail("Should throw IllegalStateException");
		} catch (IAgentException e) {
		}

		try {
			dp.getBundles();
			fail("Should throw IllegalStateException");
		} catch (IAgentException e) {
		}

		try {
			dp.getHeader("OtherHeader");
			fail("Should throw IllegalStateException");
		} catch (IAgentException e) {
		}

		try {
			dp.uninstall(false);
			fail("Should throw IllegalStateException");
		} catch (IAgentException e) {
		}

		try {
			dp.getName();
			dp.getVersion();
			dp.isStale();
		} catch (IAgentException e) {
			fail("Must not throw IllegalStateException for name, version and stale status");
		}
	}
}
