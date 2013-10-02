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

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.iagent.RemoteService;

public class RemoteBundleTest extends DeploymentTestCase {

  RemoteBundle bundle;


  protected void setUp() throws Exception {
    super.setUp();
    bundle = installBundle("test.bundle.b1_1.0.0.jar");
  }

  protected void tearDown() throws Exception {
    if (bundle != null && bundle.getState() != Bundle.UNINSTALLED) {
      try {
        bundle.uninstall(null);
      } catch (Exception e) {
      }
    }
    super.tearDown();
  }

  public void testGetSymbolicName() throws IAgentException {
    assertNotNull("The bundle should be non-null", bundle);
    assertEquals("Installed bundle must be with the given name", "test.bundle.b1", bundle.getSymbolicName());
  }

  public void testGetVersion() throws IAgentException {
    assertNotNull("The bundle should be non-null", bundle);
    assertEquals("Installed bundle must be with the given version", "1.0.0", bundle.getVersion());
  }

  public void testGetHeaders() throws IAgentException {
    Dictionary dict = bundle.getHeaders(null);
    String value = (String) dict.get("Localized-Header");
    assertEquals("Installed bundle must be with default localization", "Default localization", value);

    dict = bundle.getHeaders("");
    value = (String) dict.get("Localized-Header");
    assertEquals("The value of the header 'Localized-Header' should be '%localized'", "%localized", value);

    dict = bundle.getHeaders("de");
    value = (String) dict.get("Localized-Header");
    assertEquals("The value of the header 'Localized-Header' should be 'German localization'", "German localization",
        value);
  }

  public void testGetHeader() throws IAgentException {
    String value = bundle.getHeader("Localized-Header", null);
    assertEquals("Installed bundle must be with default localization", "Default localization", value);

    value = bundle.getHeader("Localized-Header", "");
    assertEquals("The value of the header 'Localized-Header' should be '%localized'", "%localized", value);

    value = bundle.getHeader("Localized-Header", "de");
    assertEquals("The value of the header 'Localized-Header' should be 'German localization'", "German localization",
        value);
  }

  public void testGetLocation() throws IAgentException {
    RemoteBundle[] bundles = commands.getBundles(bundle.getSymbolicName(), bundle.getVersion());
    assertNotNull("The result from calling getBundles() should be non-null", bundles);
    assertEquals("Get bundles length must be 1", 1, bundles.length);
    assertEquals("The bundle location should be the same", bundle.getLocation(), bundles[0].getLocation());
  }

  public void testStartStop() throws IAgentException {
    assertTrue("Bundle state should not be active", bundle.getState() != Bundle.ACTIVE);

    bundle.start(0);

    assertEquals("Bundle state must be active", Bundle.ACTIVE, bundle.getState());

    bundle.stop(0);

    assertEquals("Bundle state must be resolved", Bundle.RESOLVED, bundle.getState());
  }

  public void testUpdate() throws IAgentException {
    InputStream input = getClass().getClassLoader().getResourceAsStream("test.bundle.b1_1.0.1.jar");

    bundle.update(input);

    assertEquals("Bundle version must be 1.0.1", "1.0.1", bundle.getVersion());

    try {
      bundle.update(null);
      fail("IllegalArgumentException must be thrown when null InputStream is passed");
    } catch (IllegalArgumentException e) {
      // expected result
    }
  }

  public void testUninstall() throws IAgentException {
    assertTrue("Bundle state should not be uninstalled", bundle.getState() != Bundle.UNINSTALLED);

    bundle.uninstall(null);

    assertEquals("Bundle state should be uninstalled", Bundle.UNINSTALLED, bundle.getState());
  }

  public void testResolve() throws IAgentException {
    assertTrue("The bundle mast be in resolved state", bundle.resolve());
    assertEquals("Bundle state shoulc be resolved", Bundle.RESOLVED, bundle.getState());

    RemoteBundle unresolvedBundle = installBundle("test.bundle.b2_1.0.0.jar");

    assertFalse("The bundle cannot be resolved", unresolvedBundle.resolve());
    assertEquals("The bundle state should be installed", Bundle.INSTALLED, unresolvedBundle.getState());
  }

  public void testGetRegisteredService() throws IAgentException {
    assertNotNull("The result from calling getRegisteredServices() should be non-null", bundle.getRegisteredServices());
    assertEquals("The registered services length should be 0", 0, bundle.getRegisteredServices().length);

    RemoteBundle serviceBundle = installBundle("test_register_service.jar");
    assertNotNull("The result from calling installBundle() should be non-null", serviceBundle);
    serviceBundle.start(0);
    assertEquals("The state of the bundle should be ACTIVE", Bundle.ACTIVE, serviceBundle.getState());
    RemoteService[] services = serviceBundle.getRegisteredServices();

    assertRemoteService(services, serviceBundle.getBundleId());

    uninstallBundleSilently(serviceBundle);

    try {
      serviceBundle.getRegisteredServices();
      fail("Should throw IllegalStateException");
    } catch (IAgentException e) {
    }
  }

  private void assertRemoteService(RemoteService[] services, long bundleId) throws IAgentException {
    assertNotNull("The remote services should not be null", services);
    assertEquals("The remote services lenght should be 1", 1, services.length);
    assertNotNull("The first remote service from the array should not be null", services[0]);

    assertRemoteService(services[0], bundleId);
  }

  private void assertRemoteService(RemoteService service, long bundleId) throws IAgentException {
    assertNotNull("The remote service should not be null", service);

    RemoteBundle registeringBundle = service.getBundle();
    assertNotNull("The remote bundle should not be null", registeringBundle);
    assertEquals("The result from calling getBundleId() should be ", bundleId, registeringBundle.getBundleId());

    String[] objectClass = service.getObjectClass();
    assertNotNull("The array containing the interfaces under which the service is registered should not be null",
        objectClass);
    assertEquals("The length of the array should be 1", 1, objectClass.length);
    assertNotNull("The first interfece which the service is registered should not be null", objectClass[0]);
    assertEquals("The interface which the service is registered should be ", ServiceManagerTestCase.TEST_SERVICE_CLASS,
        objectClass[0]);
  }

  public void testGetServicesInUse() throws IAgentException {
    assertNotNull("The services, which are used by this bundle should not be null", bundle.getServicesInUse());
    assertEquals("The result calling getServicesInUse() should be 0", 0, bundle.getServicesInUse().length);

    RemoteBundle serviceBundle = installBundle("test_register_service.jar");
    assertNotNull("The install bundle should not be null", serviceBundle);

    RemoteBundle listenerBundle = installBundle("test_listener_service.jar");
    assertNotNull("The install listener bundle should not be null", listenerBundle);

    serviceBundle.start(0);
    listenerBundle.start(0);

    assertEquals("The result from calling getState() should be ACTIVE state", Bundle.ACTIVE, serviceBundle.getState());
    assertEquals("The result from calling getState() of listener bundle should be ACTIVE state", Bundle.ACTIVE,
        listenerBundle.getState());

    RemoteService[] services = listenerBundle.getServicesInUse();
    assertRemoteService(services, serviceBundle.getBundleId());

    uninstallBundleSilently(listenerBundle);

    try {
      listenerBundle.getServicesInUse();
      fail("Should throw IllegalStateException");
    } catch (IAgentException e) {
    }
  }

  public void testIllegalStateUpdate() throws IAgentException {
    uninstallBundleSilently(bundle);

    try {
      bundle.update(new InputStream() {
        public int read() throws IOException {
          return -1;
        }
      });
      fail("Should throw IllegalStateException");
    } catch (IAgentException e) {
    }
  }

  private void uninstallBundleSilently(RemoteBundle bundle) throws IAgentException {
    bundle.uninstall(null);
    assertTrue("The bundle state should be uninstalled", bundle.getState() == RemoteBundle.UNINSTALLED);
  }

  public void testIllegalStateUninstall() throws IAgentException {
    uninstallBundleSilently(bundle);
    try {
      bundle.uninstall(null);
      fail("Should throw IllegalStateException");
    } catch (IAgentException e) {
    }
  }

  public void testIllegalStateStop() throws IAgentException {
    uninstallBundleSilently(bundle);
    try {
      bundle.stop(0);
      fail("Should throw IllegalStateException");
    } catch (IAgentException e) {
    }
  }

  public void testIllegalStateStart() throws IAgentException {
    uninstallBundleSilently(bundle);
    try {
      bundle.start(0);
      fail("Should throw IllegalStateException");
    } catch (IAgentException e) {
    }
  }

  public void testIllegalStateResolve() throws IAgentException {
    uninstallBundleSilently(bundle);
    try {
      bundle.resolve();
      fail("Should throw IllegalStateException");
    } catch (IAgentException e) {
    }
  }

  public void testIllegalStateGetVersion() throws IAgentException {
    uninstallBundleSilently(bundle);
    try {
      bundle.getVersion();
      fail("Should throw IllegalStateException");
    } catch (IAgentException e) {
    }
  }

  public void testIllegalStateGetSymbolicName() throws IAgentException {
    uninstallBundleSilently(bundle);
    try {
      bundle.getSymbolicName();
      fail("Should throw IllegalStateException");
    } catch (IAgentException e) {
    }
  }

  public void testIllegalStateGetHeader() throws IAgentException {
    uninstallBundleSilently(bundle);
    try {
      bundle.getHeader("Localized-Header", "");
      fail("Should throw IllegalStateException");
    } catch (IAgentException e) {
    }
  }

  public void testIllegalStateGetHeaders() throws IAgentException {
    uninstallBundleSilently(bundle);
    try {
      bundle.getHeaders("");
      fail("Should throw IllegalStateException");
    } catch (IAgentException e) {
    }
  }
}
