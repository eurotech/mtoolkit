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

import junit.framework.AssertionFailedError;

import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteDP;
import org.tigris.mtoolkit.iagent.event.RemoteDPEvent;
import org.tigris.mtoolkit.iagent.event.RemoteDPListener;

public class RemoteDPListenerTest extends DeploymentTestCase implements RemoteDPListener {
  private List   events  = new ArrayList();
  private Object sleeper = new Object();

  protected void setUp() throws Exception {
    super.setUp();
    commands.addRemoteDPListener(this);
    Thread.sleep(REMOTE_LISTENER_CHANGE_TIMEOUT);
  }

  protected void tearDown() throws Exception {
    commands.removeRemoteDPListener(this);
    Thread.sleep(REMOTE_LISTENER_CHANGE_TIMEOUT);
    super.tearDown();
  }

  public void testDPListener() throws IAgentException {
    events.clear();
    RemoteDP dp = installDeploymentPackage("test.depl.p1_1.0.0.dp");
    sleep(SLEEP_INTERVAL);
    RemoteDPEvent event = findEvent(RemoteDPEvent.INSTALLED);
    assertNotNull("The result calling find installed event should be non-null", event);
    assertEquals(dp, event.getDeploymentPackage());

    dp.uninstall(false);
    sleep(SLEEP_INTERVAL);
    event = findEvent(RemoteDPEvent.UNINSTALLED);
    assertNotNull("The result calling find uninstalled event should be non-null", event);
    assertEquals(dp, event.getDeploymentPackage());
  }

  private static void assertEquals(RemoteDP expected, RemoteDP actual) throws IAgentException {
    if (expected == actual) {
      return;
    }
    if (expected == null || actual == null) {
      throw new AssertionFailedError("Expected: " + expected + " but was: " + actual);
    }
    assertEquals("The name of the expected and actual deployment packages should be the same",
        expected.getName(), actual.getName());
    assertEquals("The version of the expected and actual deployment packages should be the same",
        expected.getVersion(), actual.getVersion());
  }

  private void sleep(long time) {
    synchronized (sleeper) {
      if (events.size() > 0) {
        return;
      }
      try {
        sleeper.wait(time);
      } catch (InterruptedException e) {
      }
    }
  }

  private RemoteDPEvent findEvent(int type) throws IAgentException {
    synchronized (sleeper) {
      for (Iterator it = events.iterator(); it.hasNext();) {
        RemoteDPEvent event = (RemoteDPEvent) it.next();
        if (event.getType() == type) {
          it.remove();
          return event;
        }
      }
    }
    return null;
  }

  public void deploymentPackageChanged(RemoteDPEvent event) {
    synchronized (sleeper) {
      events.add(event);
      sleeper.notifyAll();
    }
  }

}
