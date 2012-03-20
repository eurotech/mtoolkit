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

}
