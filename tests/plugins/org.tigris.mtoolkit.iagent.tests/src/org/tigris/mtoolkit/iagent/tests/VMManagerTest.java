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

public class VMManagerTest extends DeploymentTestCase {

	protected void setUp() throws Exception {
		super.setUp();
		installBundle("test.bundle.sysprop_1.0.0.jar");
	}

	public void testGetSystemProperty() throws Exception {
		String javaVersion = connector.getVMManager().getSystemProperty("java.version");
    assertNotNull("The value of system property 'java.version' should be non-null", javaVersion);
	}
}
