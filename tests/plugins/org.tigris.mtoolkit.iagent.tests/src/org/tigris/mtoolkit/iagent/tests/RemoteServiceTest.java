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

import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.iagent.RemoteService;
import org.tigris.mtoolkit.iagent.spi.ConnectionManager;
import org.tigris.mtoolkit.iagent.spi.DeviceConnectorSpi;
import org.tigris.mtoolkit.iagent.spi.PMPConnection;

public final class RemoteServiceTest extends ServiceManagerTestCase {
  protected static final String FILTER = "(TEST_PROPERTY=Test)";
  private RemoteBundle          bundle = null;

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.tests.ServiceManagerTestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    bundle = installBundle("test_register_service.jar");
    bundle.start(0);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.tests.ServiceManagerTestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    bundle.stop(0);
    bundle.uninstall(null);
    bundle = null;
    super.tearDown();
  }

  public void testGetObjectClass() throws IAgentException {
    assertNotNull("Can not find registred service!", findService(bundle.getRegisteredServices()));
    assertNotNull("Can not find registred service!", getAllRemoteServices(TEST_SERVICE_CLASS, FILTER));
  }

  public void testGetServiceId() throws IAgentException {
    RemoteService s1 = findService(bundle.getRegisteredServices());
    RemoteService s2 = findService(getAllRemoteServices(TEST_SERVICE_CLASS, FILTER));
    assertEquals("Different service id's!", s1.getServiceId(), s2.getServiceId());
  }

  public void testGetProperties() throws IAgentException {
    RemoteService service = findService(bundle.getRegisteredServices());
    assertTrue("Incorrect service property!", "Test".equals(service.getProperties().get("TEST_PROPERTY")));
    RemoteBundle b2 = installBundle("test_listener_service.jar");
    b2.start(0);
    assertTrue("Service property not modified!", "Modify".equals(service.getProperties().get("TEST_PROPERTY")));
    b2.uninstall(null);
    b2 = null;
  }

  public void testGetUsingBundles() throws IAgentException {
    RemoteService service = findService(getAllRemoteServices(TEST_SERVICE_CLASS, FILTER));
    RemoteBundle b2 = installBundle("test_listener_service.jar");
    b2.start(0);
    RemoteBundle[] bundles = service.getUsingBundles();
    assertTrue("Service listener bundle not found in UsingBundles!", findBundle(b2, bundles));
    b2.stop(0);
    bundles = service.getUsingBundles();
    assertFalse("Stoped service listener bundle found in UsingBundles!", findBundle(b2, bundles));
    b2.uninstall(null);
    b2 = null;
  }

  public void testGetBundle() throws IAgentException {
    assertTrue("Service.getBundle() return different bundle!",
        isEqualsBundles(bundle, findService(getAllRemoteServices(TEST_SERVICE_CLASS, FILTER)).getBundle()));
  }

  public void testIsStale() throws IAgentException {
    RemoteService service = findService(getAllRemoteServices(TEST_SERVICE_CLASS, FILTER));
    assertFalse("RemoteService.isStale() return true, but the service are active!", service.isStale());
    bundle.stop(0);
    assertTrue("RemoteService.isStale() return false, but the service are unregistred!", service.isStale());
    bundle.start(0);
    assertTrue("RemoteService.isStale() return true, but the service are active!", service.isStale());
  }

  public void testIllegalStateService() throws Exception {
    RemoteService serviceOld = findService(getAllRemoteServices(TEST_SERVICE_CLASS, FILTER));
    PMPConnection pmpConnection = (PMPConnection) connectorSpi.getConnectionManager().createConnection(
        ConnectionManager.PMP_CONNECTION);
    pmpConnection.closeConnection();
    DeviceConnectorSpi connectorImpl = (DeviceConnectorSpi) connector;
    ConnectionManager connectionManager = connectorImpl.getConnectionManager();
    connectionManager.createConnection(ConnectionManager.PMP_CONNECTION);

    try {
      serviceOld.getBundle();
      fail("An IllegalStateException should be thrown for not synchronized service");
    } catch (IAgentException e) {
      assertEquals("The code error of exception should be " + IAgentErrors.ERROR_SERVICE_UNREGISTERED,
          IAgentErrors.ERROR_SERVICE_UNREGISTERED, e.getErrorCode());
    }
    try {
      serviceOld.getObjectClass();
    } catch (IAgentException e) {
      fail("An IAgentException should not be thrown for not synchronized service, when getting the object class of the service");
    }

    try {
      serviceOld.getProperties();
      fail("An IAgentException should be thrown for not synchronized service");
    } catch (IAgentException e) {
      assertEquals("The code error of exception should be " + IAgentErrors.ERROR_SERVICE_UNREGISTERED,
          IAgentErrors.ERROR_SERVICE_UNREGISTERED, e.getErrorCode());
    }
    try {
      serviceOld.getServiceId();
    } catch (IAgentException e) {
      fail("An IAgentException should not be thrown for not synchronized service");
    }
    try {
      serviceOld.getUsingBundles();
      fail("An IAgentException should be thrown for not synchronized service");
    } catch (IAgentException e) {
      assertEquals("The code error of exception should be " + IAgentErrors.ERROR_SERVICE_UNREGISTERED,
          IAgentErrors.ERROR_SERVICE_UNREGISTERED, e.getErrorCode());
    }
    try {
      assertFalse("The result from calling isStale() should be wether the service has been unregistered",
          serviceOld.isStale());
    } catch (IAgentException e) {
      fail("An IAgentException should be thrown for not synchronized service");
    }

  }

  private boolean findBundle(RemoteBundle bundle, RemoteBundle[] bundles) throws IAgentException {
    for (int j = 0; j < bundles.length; j++) {
      if (isEqualsBundles(bundle, bundles[j])) {
        return true;
      }
    }
    return false;
  }

  private boolean isEqualsBundles(RemoteBundle b1, RemoteBundle b2) throws IAgentException {
    return (b1 != null && b2 != null) ? (b1.getBundleId() == b2.getBundleId()) : false;
  }
}
