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
import java.util.List;

import junit.framework.AssertionFailedError;

import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteService;
import org.tigris.mtoolkit.iagent.ServiceManager;
import org.tigris.mtoolkit.iagent.event.RemoteServiceEvent;
import org.tigris.mtoolkit.iagent.event.RemoteServiceListener;

public class ServiceManagerTestCase extends DeploymentTestCase {
  public static final String TEST_SERVICE_CLASS = "com.prosyst.test.servicemanager.packages.register.TestService";

  private ServiceManager     serviceManager     = null;
  private List               services           = null;

  protected List             events             = new ArrayList();
  protected Object           sleeper            = new Object();

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.tests.DeploymentTestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    services = new ArrayList();
    serviceManager = connector.getServiceManager();
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.tests.DeploymentTestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    for (int j = 0; j < services.size(); j++) {
      try {
        serviceManager.removeRemoteServiceListener((RemoteServiceListener) services.get(j));
      } catch (IAgentException e) {
        e.printStackTrace();
      }
    }
    if (!services.isEmpty()) {
      Thread.sleep(REMOTE_LISTENER_CHANGE_TIMEOUT);
    }
    services.clear();
    services = null;
    super.tearDown();
  }

  protected void addRemoteServiceListener(RemoteServiceListener listener) throws IAgentException, InterruptedException {
    if (listener != null) {
      serviceManager.addRemoteServiceListener(listener);
      services.add(listener);
      Thread.sleep(REMOTE_LISTENER_CHANGE_TIMEOUT);
    }
  }

  protected void removeRemoteServiceListener(RemoteServiceListener listener) throws IAgentException,
      InterruptedException {
    if (listener != null) {
      serviceManager.removeRemoteServiceListener(listener);
      services.remove(listener);
      Thread.sleep(REMOTE_LISTENER_CHANGE_TIMEOUT);
    }
  }

  protected RemoteService[] getAllRemoteServices(String clazz, String filter) throws IAgentException {
    return serviceManager.getAllRemoteServices(clazz, filter);
  }

  protected RemoteServiceEvent findEvent(String clazz, int type) throws IAgentException {
    synchronized (sleeper) {
      for (int j = 0; j < events.size(); j++) {
        RemoteServiceEvent event = (RemoteServiceEvent) events.get(j);
        if (event.getType() == type) {
          String[] clazzs = event.getService().getObjectClass();
          for (int i = 0; i < clazzs.length; i++) {
            if (clazz.equals(clazzs[i])) {
              return event;
            }
          }
        }
      }
    }
    return null;
  }

  protected RemoteService findService(RemoteService[] services) throws IAgentException {
    if (services != null) {
      for (int j = 0; j < services.length; j++) {
        String[] clazzs = services[j].getObjectClass();
        for (int i = 0; i < clazzs.length; i++) {
          if (TEST_SERVICE_CLASS.equals(clazzs[i])) {
            return services[j];
          }
        }
      }
    }
    return null;
  }

  protected void sleep(long time) {
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

  protected void sleep(long time, String expectedClass, int expectedType) throws IAgentException {
    final long now = System.currentTimeMillis();
    while (true) {
      synchronized (sleeper) {
        if (findEvent(expectedClass, expectedType) != null) {
          return;
        }
        long expiredTime = System.currentTimeMillis() - now;
        if (expiredTime >= time) {
          return;
        }
        try {
          sleeper.wait(time - expiredTime);
        } catch (InterruptedException e) {
        }
      }
    }
  }

  protected static void assertEquals(String message, Object[] expected, Object[] actual) {
    if (expected == actual) {
      return;
    }
    if (expected == null || actual == null) {
      throw new AssertionFailedError("Expected " + expected + ", but was " + actual);
    }
    assertEquals(message, expected.length, actual.length);
    for (int i = 0; i < actual.length; i++) {
      assertEquals(message, expected[i], actual[i]);
    }
  }
}
