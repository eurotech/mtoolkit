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
    assertEquals("The installed bundle state should be active", Bundle.ACTIVE, bundle.getState());

    assertTrue("The result from finding services " + TEST_SERVICE_CLASS + " and with filter " + FILTER
        + " should be non-null", isServicePresent(TEST_SERVICE_CLASS, FILTER));
    assertTrue("The result from finding services " + TEST_SERVICE_CLASS + " should be non-null",
        isServicePresent(TEST_SERVICE_CLASS, null));
    assertTrue("The result from finding services with filter " + FILTER + " should be non-null",
        isServicePresent(null, FILTER));
    assertTrue("The result from finding services should be non-null", isServicePresent(null, null));

    try {
      isServicePresent(null, "wrong_filter");
      isServicePresent(TEST_SERVICE_CLASS, "wrong_filter");
      fail("Should have thrown exception, because the filter is invalid");
    } catch (IllegalArgumentException e) {
      // expected
    }

    assertFalse("The result from finding services 'unknown_class' should be null",
        isServicePresent("unknown_class", null));

    bundle.stop(0);
    assertTrue("The result from calling getState() should be different from ACTIVE", bundle.getState() != Bundle.ACTIVE);
    assertFalse("The result from finding services " + TEST_SERVICE_CLASS + " should be null",
        isServicePresent(TEST_SERVICE_CLASS, null));

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
    assertNotNull("RemoteService object associated with event should be non-null", registeredEvent.getService());

    events.clear();
    RemoteBundle bundle2 = installBundle("test_listener_service.jar");
    bundle2.start(0);
    sleep(SLEEP_INTERVAL, TEST_SERVICE_CLASS, RemoteServiceEvent.MODIFIED);
    RemoteServiceEvent modifiedEvent = findEvent(TEST_SERVICE_CLASS, RemoteServiceEvent.MODIFIED);
    assertNotNull("Service Modify event not appear!", modifiedEvent);
    assertNotNull("RemoteService object associated with Service Modify event should be non-null",
        modifiedEvent.getService());
    assertEquals("The result from calling getServiceId() from registered and modified event should be the same",
        registeredEvent.getService().getServiceId(), modifiedEvent.getService().getServiceId());
    assertEquals("The result from calling getObjectClass() from registered and modified event should be the same",
        registeredEvent.getService().getObjectClass(), modifiedEvent.getService().getObjectClass());

    events.clear();
    bundle1.stop(0);
    sleep(SLEEP_INTERVAL, TEST_SERVICE_CLASS, RemoteServiceEvent.UNREGISTERED);
    RemoteServiceEvent unregisteredEvent = findEvent(TEST_SERVICE_CLASS, RemoteServiceEvent.UNREGISTERED);
    assertNotNull("Service Unregistered event not appear!", unregisteredEvent);
    assertNotNull("RemoteService object associated with Service Unregistered event should be non-null",
        unregisteredEvent.getService());
    assertEquals("The result from calling getServiceId() from registered and unregistered event should be the same",
        registeredEvent.getService().getServiceId(), unregisteredEvent.getService().getServiceId());
    assertEquals("The result from calling getObjectClass() from registered and unregistered event should be the same",
        registeredEvent.getService().getObjectClass(), unregisteredEvent.getService().getObjectClass());

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
