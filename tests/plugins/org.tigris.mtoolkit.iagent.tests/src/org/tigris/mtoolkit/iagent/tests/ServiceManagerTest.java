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

import org.osgi.framework.Bundle;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.iagent.event.RemoteServiceEvent;
import org.tigris.mtoolkit.iagent.event.RemoteServiceListener;

public final class ServiceManagerTest extends ServiceManagerTestCase implements RemoteServiceListener {
  protected static final String FILTER = "(TEST_PROPERTY=Test)";

  public void testGetRemoteServices() throws IAgentException {
    RemoteBundle bundle = installBundle("test_register_service.jar");
    bundle.start(0);
    assertEquals(Bundle.ACTIVE, bundle.getState());

    assertTrue(isServicePresent(TEST_SERVICE_CLASS, FILTER));
    assertTrue(isServicePresent(TEST_SERVICE_CLASS, null));
    assertTrue(isServicePresent(null, FILTER));
    assertTrue(isServicePresent(null, null));

    try {
      isServicePresent(null, "wrong_filter");
      isServicePresent(TEST_SERVICE_CLASS, "wrong_filter");
      fail("Should have thrown exception, because the filter is invalid");
    } catch (IllegalArgumentException e) {
      // expected
    }

    assertFalse(isServicePresent("unknown_class", null));

    bundle.stop(0);
    assertTrue(bundle.getState() != Bundle.ACTIVE);
    assertFalse(isServicePresent(TEST_SERVICE_CLASS, null));

    bundle.uninstall(null);
    bundle = null;
  }

  public void testServiceListener() throws Exception {
    events.clear();
    addRemoteServiceListener(this);
    RemoteBundle bundle1 = installBundle("test_register_service.jar");
    bundle1.start(0);
    sleep(SLEEP_INTERVAL, TEST_SERVICE_CLASS, RemoteServiceEvent.REGISTERED);
    RemoteServiceEvent registeredEvent = findEvent(TEST_SERVICE_CLASS, RemoteServiceEvent.REGISTERED);
    assertNotNull("Service Registered event not appear!", registeredEvent);
    assertNotNull(registeredEvent.getService());

    events.clear();
    RemoteBundle bundle2 = installBundle("test_listener_service.jar");
    bundle2.start(0);
    sleep(SLEEP_INTERVAL, TEST_SERVICE_CLASS, RemoteServiceEvent.MODIFIED);
    RemoteServiceEvent modifiedEvent = findEvent(TEST_SERVICE_CLASS, RemoteServiceEvent.MODIFIED);
    assertNotNull("Service Modify event not appear!", modifiedEvent);
    assertEquals(registeredEvent.getService().getServiceId(), modifiedEvent.getService().getServiceId());
    assertEquals(registeredEvent.getService().getObjectClass(), modifiedEvent.getService().getObjectClass());
    assertNotNull(modifiedEvent.getService());

    events.clear();
    bundle1.stop(0);
    sleep(SLEEP_INTERVAL, TEST_SERVICE_CLASS, RemoteServiceEvent.UNREGISTERED);
    RemoteServiceEvent unregisteredEvent = findEvent(TEST_SERVICE_CLASS, RemoteServiceEvent.UNREGISTERED);
    assertNotNull("Service Unregistered event not appear!", unregisteredEvent);
    assertNotNull(unregisteredEvent.getService());
    assertEquals(registeredEvent.getService().getServiceId(), unregisteredEvent.getService().getServiceId());
    assertEquals(registeredEvent.getService().getObjectClass(), unregisteredEvent.getService().getObjectClass());

    removeRemoteServiceListener(this);
    events.clear();
    bundle1.start(0);
    bundle1.stop(0);
    sleep(SLEEP_INTERVAL);
    assertTrue("Unexpected event(s) appear(s)!", events.size() == 0);
    bundle1.uninstall(null);
    bundle2.uninstall(null);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.event.RemoteServiceListener#serviceChanged(org.tigris.mtoolkit.iagent.event.RemoteServiceEvent)
   */
  public void serviceChanged(RemoteServiceEvent event) {
    synchronized (sleeper) {
      events.add(event);
      sleeper.notifyAll();
    }
  }

  private boolean isServicePresent(String clazz, String filter) throws IAgentException {
    return findService(getAllRemoteServices(clazz, filter)) != null;
  }
}
