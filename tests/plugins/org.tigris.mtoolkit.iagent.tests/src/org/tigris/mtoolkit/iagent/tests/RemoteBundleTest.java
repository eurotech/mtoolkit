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

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    bundle = installBundle("test.bundle.b1_1.0.0.jar");
  }

  @Override
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
    assertNotNull(bundle);
    assertEquals("test.bundle.b1", bundle.getSymbolicName());
  }

  public void testGetVersion() throws IAgentException {
    assertNotNull(bundle);
    assertEquals("1.0.0", bundle.getVersion());
  }

  public void testGetHeaders() throws IAgentException {
    Dictionary dict = bundle.getHeaders(null);
    String value = (String) dict.get("Localized-Header");
    assertEquals("Default localization", value);

    dict = bundle.getHeaders("");
    value = (String) dict.get("Localized-Header");
    assertEquals("%localized", value);

    dict = bundle.getHeaders("de");
    value = (String) dict.get("Localized-Header");
    assertEquals("German localization", value);
  }

  public void testGetHeader() throws IAgentException {
    String value = bundle.getHeader("Localized-Header", null);
    assertEquals("Default localization", value);

    value = bundle.getHeader("Localized-Header", "");
    assertEquals("%localized", value);

    value = bundle.getHeader("Localized-Header", "de");
    assertEquals("German localization", value);
  }

  public void testGetLocation() throws IAgentException {
    RemoteBundle[] bundles = commands.getBundles(bundle.getSymbolicName(), bundle.getVersion());
    assertNotNull(bundles);
    assertEquals(1, bundles.length);
    assertEquals(bundle.getLocation(), bundles[0].getLocation());
  }

  public void testStartStop() throws IAgentException {
    assertTrue(bundle.getState() != Bundle.ACTIVE);

    bundle.start(0);

    assertEquals(Bundle.ACTIVE, bundle.getState());

    bundle.stop(0);

    assertEquals(Bundle.RESOLVED, bundle.getState());
  }

  public void testUpdate() throws IAgentException {
    InputStream input = getClass().getClassLoader().getResourceAsStream("test.bundle.b1_1.0.1.jar");

    bundle.update(input);

    assertEquals("1.0.1", bundle.getVersion());

    try {
      bundle.update(null);
      fail("IllegalArgumentException must be thrown when null InputStream is passed");
    } catch (IllegalArgumentException e) {
      // expected result
    }
  }

  public void testUninstall() throws IAgentException {
    assertTrue(bundle.getState() != Bundle.UNINSTALLED);

    bundle.uninstall(null);

    assertEquals(Bundle.UNINSTALLED, bundle.getState());
  }

  public void testResolve() throws IAgentException {
    assertTrue(bundle.resolve());
    assertEquals(Bundle.RESOLVED, bundle.getState());

    RemoteBundle unresolvedBundle = installBundle("test.bundle.b2_1.0.0.jar");

    assertFalse(unresolvedBundle.resolve());
    assertEquals(Bundle.INSTALLED, unresolvedBundle.getState());
  }

  public void testGetRegisteredService() throws IAgentException {
    assertNotNull(bundle.getRegisteredServices());
    assertEquals(0, bundle.getRegisteredServices().length);

    RemoteBundle serviceBundle = installBundle("test_register_service.jar");
    assertNotNull(serviceBundle);
    serviceBundle.start(0);
    assertEquals(Bundle.ACTIVE, serviceBundle.getState());
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
    assertNotNull(services);
    assertEquals(1, services.length);
    assertNotNull(services[0]);

    assertRemoteService(services[0], bundleId);
  }

  private void assertRemoteService(RemoteService service, long bundleId) throws IAgentException {
    assertNotNull(service);

    RemoteBundle registeringBundle = service.getBundle();
    assertNotNull(registeringBundle);
    assertEquals(bundleId, registeringBundle.getBundleId());

    String[] objectClass = service.getObjectClass();
    assertNotNull(objectClass);
    assertEquals(1, objectClass.length);
    assertNotNull(objectClass[0]);
    assertEquals(ServiceManagerTestCase.TEST_SERVICE_CLASS, objectClass[0]);
  }

  public void testGetServicesInUse() throws IAgentException {
    assertNotNull(bundle.getServicesInUse());
    assertEquals(0, bundle.getServicesInUse().length);

    RemoteBundle serviceBundle = installBundle("test_register_service.jar");
    assertNotNull(serviceBundle);

    RemoteBundle listenerBundle = installBundle("test_listener_service.jar");
    assertNotNull(listenerBundle);

    serviceBundle.start(0);
    listenerBundle.start(0);

    assertEquals(Bundle.ACTIVE, serviceBundle.getState());
    assertEquals(Bundle.ACTIVE, listenerBundle.getState());

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
        @Override
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
    assertTrue(bundle.getState() == RemoteBundle.UNINSTALLED);
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
