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

import java.io.InputStream;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.tigris.mtoolkit.iagent.DeploymentManager;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.iagent.RemoteDP;
import org.tigris.mtoolkit.iagent.spi.DeviceConnectorSpi;

public class DeploymentTestCase extends TestCase {
  protected static final long   SLEEP_INTERVAL                 = Long.getLong("iagent.tests.sleep.interval", 10000)
                                                                   .longValue();
  protected static final long   REMOTE_LISTENER_CHANGE_TIMEOUT = Long.getLong("iagent.tests.remotelistener.timeout",
                                                                   5000).longValue();

  protected static final String BUNDLE_LOCATION_PREFIX         = "dc-test:bundle:";

  protected DeviceConnector     connector;
  protected DeviceConnectorSpi  connectorSpi;
  protected DeploymentManager   commands;
  private Set                   deploymentPackages             = new HashSet();
  private Set                   bundles                        = new HashSet();

  protected void setUp() throws Exception {

    Hashtable props = new Hashtable();
    props.put(DeviceConnector.KEY_DEVICE_IP, "127.0.0.1");
    props.put("framework-connection-immediate", Boolean.FALSE);
    connector = DeviceConnector.connect("socket", "127.0.0.1", props, null);
    // use the fact that the same class implements both the connector and
    // its spi
    connectorSpi = (DeviceConnectorSpi) connector;
    commands = connector.getDeploymentManager();

    deploymentPackages.clear();
    bundles.clear();
  }

  protected void tearDown() throws Exception {
    for (Iterator it = deploymentPackages.iterator(); it.hasNext();) {
      String dpName = (String) it.next();
      RemoteDP dp = commands.getDeploymentPackage(dpName);
      if (dp != null && !dp.isStale()) {
        try {
          dp.uninstall(true);
        } catch (IAgentException e) {
          e.printStackTrace();
        }
      }
    }

    RemoteBundle[] remoteBundles = commands.listBundles();
    for (Iterator it = bundles.iterator(); it.hasNext();) {
      String bLocation = (String) it.next();
      for (int i = remoteBundles.length - 1; i >= 0; i--) {
        if (remoteBundles[i].getState() != Bundle.UNINSTALLED && bLocation.equals(remoteBundles[i].getLocation())) {
          try {
            remoteBundles[i].uninstall(null);
          } catch (Exception e) {
          }
          break;
        }
      }
    }

    connector.closeConnection();
  }

  protected RemoteDP installDeploymentPackage(String dpLocation, String dpName) throws IAgentException {
    return installDeploymentPackage(dpLocation);
  }

  protected RemoteDP installDeploymentPackage(String dpLocation) throws IAgentException {
    InputStream input = getClass().getClassLoader().getResourceAsStream(dpLocation);
    RemoteDP dp = commands.installDeploymentPackage(input);
    deploymentPackages.add(dp.getName());
    return dp;
  }

  protected RemoteBundle installBundle(String bundleResource) throws IAgentException {
    InputStream input = getClass().getClassLoader().getResourceAsStream(bundleResource);
    if (input == null) {
      throw new IllegalArgumentException(bundleResource + " cannot be found!");
    }
    String bLocation = BUNDLE_LOCATION_PREFIX + bundleResource;
    registerBundle(bLocation);
    return commands.installBundle(bLocation, input);
  }

  protected void registerBundle(String bLocation) {
    bundles.add(bLocation);
  }
}
