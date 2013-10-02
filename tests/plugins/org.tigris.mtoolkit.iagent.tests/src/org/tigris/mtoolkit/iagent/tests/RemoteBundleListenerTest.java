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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.osgi.framework.Bundle;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.iagent.event.RemoteBundleEvent;
import org.tigris.mtoolkit.iagent.event.RemoteBundleListener;

public class RemoteBundleListenerTest extends DeploymentTestCase implements RemoteBundleListener {
  private List   events  = new ArrayList();
  private Object sleeper = new Object();

  public void testBundleListener() throws Exception {
    commands.addRemoteBundleListener(this);
    Thread.sleep(REMOTE_LISTENER_CHANGE_TIMEOUT);

    try {
      RemoteBundle bundle = installBundle("test_register_service.jar");
      bundle.resolve();

      RemoteBundleEvent[] foundEvents = findEvents(RemoteBundleEvent.INSTALLED, RemoteBundleEvent.RESOLVED);
      assertNotNull("The result calling findEvents() for all INSTALLED and RESOLVED events should be non-null",
          foundEvents);
      assertRemoteBundle(foundEvents[0].getBundle(), bundle.getBundleId());

      events.clear();
      bundle.start(0);
      foundEvents = findEvents(RemoteBundleEvent.STARTING, RemoteBundleEvent.STARTED);
      assertNotNull("The result calling findEvents() for all STARTING and STARTED events should be non-null",
          foundEvents);
      assertRemoteBundle(foundEvents[0].getBundle(), bundle.getBundleId());

      events.clear();
      bundle.stop(0);
      foundEvents = findEvents(RemoteBundleEvent.STOPPING, RemoteBundleEvent.STOPPED);
      assertNotNull("The result calling findEvents() for all STOPPING and STOPPED events should be non-null",
          foundEvents);
      assertRemoteBundle(foundEvents[0].getBundle(), bundle.getBundleId());

      events.clear();
      bundle.start(0);
      foundEvents = findEvents(RemoteBundleEvent.STARTING, RemoteBundleEvent.STARTED);
      assertNotNull("The result calling findEvents() for all STARTING and STARTED events should be non-null",
          foundEvents);
      assertRemoteBundle(foundEvents[0].getBundle(), bundle.getBundleId());

      events.clear();
      bundle.update(getClass().getClassLoader().getResourceAsStream("test_register_service.jar"));
      foundEvents = findEvents(RemoteBundleEvent.STOPPING, RemoteBundleEvent.STOPPED, RemoteBundleEvent.UNRESOLVED,
          RemoteBundleEvent.UPDATED);
      assertNotNull(
          "The result calling findEvents() for all STOPPING, STOPPED, UNRESOLVED and UPDATED events should be non-null",
          foundEvents);
      assertRemoteBundle(foundEvents[0].getBundle(), bundle.getBundleId());

      foundEvents = findEvents(RemoteBundleEvent.RESOLVED, RemoteBundleEvent.STARTING, RemoteBundleEvent.STARTED);
      assertNotNull("The result calling findEvents() for all STARTING and STARTED events should be non-null",
          foundEvents);
      assertRemoteBundle(foundEvents[0].getBundle(), bundle.getBundleId());

      events.clear();
      bundle.uninstall(null);
      foundEvents = findEvents(RemoteBundleEvent.STOPPING, RemoteBundleEvent.STOPPED, RemoteBundleEvent.UNRESOLVED,
          RemoteBundleEvent.UNINSTALLED);
      assertNotNull(
          "The result calling findEvents() for all STOPPING, STOPPED, UNRESOLVED and UNINSTALLED events should be non-null",
          foundEvents);
      assertRemoteBundle(foundEvents[0].getBundle(), bundle.getBundleId());

      assertEquals("The events size should be 0", 0, events.size());

      bundle = null;
    } finally {
      commands.removeRemoteBundleListener(this);
    }
  }

  public void testLazyStartedEvent() throws Exception {
    // Requires: R4.1 specification
    commands.addRemoteBundleListener(this);
    Thread.sleep(REMOTE_LISTENER_CHANGE_TIMEOUT);

    try {
      events.clear();
      RemoteBundle bundle = installBundle("test.bundle.b3_1.0.0.jar");
      assertNotNull("The result calling installBundle() should be non-null", bundle);
      bundle.resolve();

      assertEquals("Bundle state should be RESOLVED", Bundle.RESOLVED, bundle.getState());

      bundle.start(Bundle.START_ACTIVATION_POLICY | Bundle.START_TRANSIENT);

      assertEquals("The result from calling getState() should be STARTING", Bundle.STARTING, bundle.getState());
      try {
        Thread.sleep(SLEEP_INTERVAL);
      } catch (Exception e) {
        // TODO: handle exception
      }

      RemoteBundleEvent foundEvent = findEvent(RemoteBundleEvent.LAZY_STARTED);

      assertNotNull("The result calling findEvents() for all LAZY_STARTED events should be non-null", foundEvent);
      assertRemoteBundle(foundEvent.getBundle(), bundle.getBundleId());
      bundle.uninstall(null);
      bundle = null;
    } finally {
      commands.removeRemoteBundleListener(this);
    }
  }

  private void assertRemoteBundle(RemoteBundle bundle, long bundleId) throws IAgentException {
    assertNotNull("The bundle should be not-null", bundle);
    assertEquals("The result from calling getBundleId() should be " + bundleId, bundleId, bundle.getBundleId());
  }

  public void bundleChanged(RemoteBundleEvent event) {
    synchronized (sleeper) {
      events.add(event);
      sleeper.notifyAll();
    }
  }

  private RemoteBundleEvent findEvent(int type1) {
    RemoteBundleEvent[] events = findEvents(new int[] {
      type1
    });
    return events != null ? events[0] : null;
  }

  private RemoteBundleEvent[] findEvents(int type1, int type2) {
    return findEvents(new int[] {
        type1, type2
    });
  }

  private RemoteBundleEvent[] findEvents(int type1, int type2, int type3) {
    return findEvents(new int[] {
        type1, type2, type3
    });
  }

  private RemoteBundleEvent[] findEvents(int type1, int type2, int type3, int type4) {
    return findEvents(new int[] {
        type1, type2, type3, type4
    });
  }

  private RemoteBundleEvent[] findEvents(int[] types) {
    if (types == null) {
      return null;
    }
    synchronized (sleeper) {
      List foundEvents = new ArrayList();
      int count = 0;
      while (events.size() < types.length && count++ < 5) {
        try {
          sleeper.wait(SLEEP_INTERVAL);
        } catch (InterruptedException e) {
          // ignore
        }
      }
      for (Iterator it = events.iterator(); it.hasNext();) {
        RemoteBundleEvent event = (RemoteBundleEvent) it.next();
        for (int i = 0; i < types.length; i++) {
          if ((event).getType() == types[i]) {
            it.remove();
            foundEvents.add(event);
            break; // break the inner for
          }
        }
      }
      if (foundEvents.size() == types.length) {
        return (RemoteBundleEvent[]) foundEvents.toArray(new RemoteBundleEvent[foundEvents.size()]);
      }
    }
    return null;
  }

}
