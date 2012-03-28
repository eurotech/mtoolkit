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

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

  public static Test suite() {
    TestSuite suite = new TestSuite("Tests for IAgent API");
    // $JUnit-BEGIN$
    suite.addTestSuite(ApplicationManagerTest.class);
    suite.addTestSuite(RemoteApplicationTest.class);
    suite.addTestSuite(DeploymentManagerTest.class);
    suite.addTestSuite(RemoteBundleTest.class);
    suite.addTestSuite(RemoteBundleListenerTest.class);
    suite.addTestSuite(RemoteCapabilitiesTest.class);
    suite.addTestSuite(RemoteDPTest.class);
    suite.addTestSuite(RemoteDPListenerTest.class);
    suite.addTestSuite(ServiceManagerTest.class);
    suite.addTestSuite(RemoteServiceTest.class);
    suite.addTestSuite(RemoteServiceListenerTest.class);
    suite.addTestSuite(MBSASessionTest.class);
    suite.addTestSuite(ThreadPoolTest.class);
    suite.addTestSuite(VMManagerTest.class);
    // $JUnit-END$
    return suite;
  }

}
