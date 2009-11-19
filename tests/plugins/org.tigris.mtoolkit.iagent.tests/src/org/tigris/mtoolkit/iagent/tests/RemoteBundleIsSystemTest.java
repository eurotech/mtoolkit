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

import java.io.ByteArrayInputStream;

import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.iagent.internal.DeploymentManagerImpl;

public class RemoteBundleIsSystemTest extends DeploymentTestCase {
	RemoteBundle bundle;
	DeploymentManagerImpl commandsImpl = null;

	protected void setUp() throws Exception {
		super.setUp();
		commandsImpl = (DeploymentManagerImpl) commands;
		bundle = installBundle("test.system.bundle.1.0.0.jar");
		bundle.start(0);
		commandsImpl.clearSystemBundlesList();
	}

	public void testIsSystemBundle() throws Exception {
		bundle = commandsImpl.getBundle(bundle.getBundleId());
		boolean systemBundle = bundle.isSystemBundle();
		assertTrue(systemBundle);
	}

	public void testSystemBundleListRefresh() throws Exception {
		assertTrue(bundle.isSystemBundle());
		RemoteBundle testBundle = installBundle("test.bundle.b1_1.0.0.jar");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		assertTrue(testBundle.isSystemBundle());
	}
	
	/**
	 * Tests whether the remote framework protects its system bundles.
	 * 
	 * @throws Exception
	 */
	public void testRemoteSystemBundleProtection() throws Exception {
		RemoteBundle[] bundles = commands.listBundles();
		assertNotNull(bundles);
		assertTrue(bundles.length > 0);
		boolean atLeastOneFound = false;
		for (int i = 0; i < bundles.length; i++) {
			// find the system bundle (bundle 0) and assert that you cannot stop it
			if (bundles[i].isSystemBundle()) {
				atLeastOneFound = true;
				try {
					bundles[i].stop(0);
					fail("Exception must be thrown indicating refused system bundle operation");
				} catch (IAgentException e) {
					assertEquals(IAgentErrors.ERROR_BUNDLE_SYSTEM, e.getErrorCode());
				}
				
				try {
					bundles[i].start(0);
					fail("Exception must be thrown indicating refused system bundle operation");
				} catch (IAgentException e) {
					assertEquals(IAgentErrors.ERROR_BUNDLE_SYSTEM, e.getErrorCode());
				}

				try {
					bundles[i].uninstall();
					fail("Exception must be thrown indicating refused system bundle operation");
				} catch (IAgentException e) {
					assertEquals(IAgentErrors.ERROR_BUNDLE_SYSTEM, e.getErrorCode());
				}

				try {
					bundles[i].update(new ByteArrayInputStream(new byte[0]));
					fail("Exception must be thrown indicating refused system bundle operation");
				} catch (IAgentException e) {
					assertEquals(IAgentErrors.ERROR_BUNDLE_SYSTEM, e.getErrorCode());
				}
			}
		}
		assertTrue(atLeastOneFound);
	}
}
