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
package org.tigris.mtoolkit.iagent.internal.rpc;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.tigris.mtoolkit.iagent.event.EventSynchronizer;
import org.tigris.mtoolkit.iagent.internal.VMCommander;
import org.tigris.mtoolkit.iagent.internal.rpc.console.EquinoxRemoteConsole;
import org.tigris.mtoolkit.iagent.internal.rpc.console.ProSystRemoteConsole;
import org.tigris.mtoolkit.iagent.internal.rpc.console.RemoteConsoleServiceBase;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.pmp.PMPPeer;
import org.tigris.mtoolkit.iagent.pmp.PMPServer;
import org.tigris.mtoolkit.iagent.pmp.PMPServerFactory;
import org.tigris.mtoolkit.iagent.pmp.PMPService;
import org.tigris.mtoolkit.iagent.pmp.PMPServiceFactory;
import org.tigris.mtoolkit.iagent.rpc.Capabilities;
import org.tigris.mtoolkit.iagent.rpc.RemoteCapabilitiesManager;

public final class Activator implements BundleActivator, ServiceTrackerCustomizer, FrameworkListener {
  private static final String           EVENT_ADMIN_CLASS      = "org.osgi.service.event.EventAdmin";
  private static final String           DEPLOYMENT_ADMIN_CLASS = "org.osgi.service.deploymentadmin.DeploymentAdmin";

  private static final String           IAGENT_CONTROLLER_PROP = "iagent.controller";

  private static final int              IAGENT_PMP_PORT        = Integer.getInteger("iagent.pmp.port",
                                                                   PMPPeer.DEFAULT_PMP_PORT).intValue();

  private static Activator              instance;
  private static BundleContext          context;

  private PMPServer                     pmpServer;
  private VMCommander                   vmCommander;

  private RemoteBundleAdminImpl         bundleAdmin;
  private RemoteApplicationAdminImpl    applicationAdmin;
  private RemoteDeploymentAdminImpl     deploymentAdmin;
  private RemoteServiceAdminImpl        serviceAdmin;
  private RemoteConsoleServiceBase      console;
  private EventSynchronizerImpl         synchronizer;
  private RemoteCapabilitiesManagerImpl capabilitiesManager;

  private ServiceRegistration           pmpServiceReg;
  private ServiceRegistration           pmpServerReg;

  private ServiceTracker                deploymentAdminTrack;
  private ServiceTracker                eventAdminTracker;

  public void start(BundleContext context) throws Exception {
    Activator.context = context;
    instance = this;
    DebugUtils.initialize(context);

    synchronizer = new EventSynchronizerImpl(context);

    capabilitiesManager = new RemoteCapabilitiesManagerImpl();
    capabilitiesManager.register(context);

    bundleAdmin = new RemoteBundleAdminImpl();
    bundleAdmin.register(context);

    registerApplicationAdmin(context);

    deploymentAdminTrack = new ServiceTracker(context, DEPLOYMENT_ADMIN_CLASS, this);
    deploymentAdminTrack.open(true);

    eventAdminTracker = new ServiceTracker(context, EVENT_ADMIN_CLASS, this);
    eventAdminTracker.open(true);

    serviceAdmin = new RemoteServiceAdminImpl();
    serviceAdmin.register(context);

    registerConsole(context);

    pmpServiceReg = context.registerService(PMPService.class.getName(), PMPServiceFactory.getDefault(), null);
    pmpServer = PMPServerFactory.createServer(context, IAGENT_PMP_PORT, null);
    pmpServerReg = context.registerService(PMPServer.class.getName(), pmpServer, pmpServer.getProperties());
    synchronizer.setPMPServer(pmpServer);
    synchronizer.start();

    if (Boolean.valueOf(System.getProperty(IAGENT_CONTROLLER_PROP)).booleanValue()) {
      registerControllerSupport(context);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  public void stop(BundleContext context) throws Exception {
    unregisterConsole();

    pmpServerReg.unregister();
    pmpServer.close();
    pmpServiceReg.unregister();

    if (synchronizer != null) {
      synchronizer.stopDispatching();
      synchronizer.unregister(context);
    }

    if (bundleAdmin != null) {
      bundleAdmin.unregister(context);
      bundleAdmin = null;
    }

    unregisterApplicationAdmin(context);

    if (deploymentAdminTrack != null) {
      deploymentAdminTrack.close();
      deploymentAdminTrack = null;
    }

    if (eventAdminTracker != null) {
      eventAdminTracker.close();
      eventAdminTracker = null;
    }

    if (serviceAdmin != null) {
      serviceAdmin.unregister(context);
      serviceAdmin = null;
    }

    if (vmCommander != null) {
      context.removeFrameworkListener(this);
      vmCommander.close();
      vmCommander = null;
    }

    if (capabilitiesManager != null) {
      capabilitiesManager.unregister(context);
      capabilitiesManager = null;
    }

    DebugUtils.dispose();
    instance = null;
    Activator.context = null;
  }

  // TODO: Rework dependency support
  /* (non-Javadoc)
   * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
   */
  public Object addingService(ServiceReference arg0) {
    String[] classes = (String[]) arg0.getProperty("objectClass");
    for (int i = 0; i < classes.length; i++) {
      if (classes[i].equals(DEPLOYMENT_ADMIN_CLASS)) {
        Object admin = context.getService(arg0);
        registerDeploymentAdmin(admin);
        return admin;
      } else if (classes[i].equals(EVENT_ADMIN_CLASS)) {
        setCapability(Capabilities.EVENT_SUPPORT, true);
        return new Object();
      }
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, T)
   */
  public void modifiedService(ServiceReference arg0, Object arg1) {
  }

  /* (non-Javadoc)
   * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, T)
   */
  public void removedService(ServiceReference ref, Object obj) {
    String[] classes = (String[]) ref.getProperty("objectClass");
    for (int i = 0; i < classes.length; i++) {
      if (classes[i].equals(DEPLOYMENT_ADMIN_CLASS)) {
        if (unregisterDeploymentAdmin(obj)) {
          Object admin = deploymentAdminTrack.getService();
          if (admin != null) {
            registerDeploymentAdmin(admin);
          }
        }
      } else if (classes[i].equals(EVENT_ADMIN_CLASS)) {
        if (eventAdminTracker.getService() == null) {
          setCapability(Capabilities.EVENT_SUPPORT, false);
        }
      }
    }
  }

  /* (non-Javadoc)
   * @see org.osgi.framework.FrameworkListener#frameworkEvent(org.osgi.framework.FrameworkEvent)
   */
  public void frameworkEvent(FrameworkEvent event) {
    if (event.getType() == FrameworkEvent.STARTED) {
      startController(context);
    }
  }

  public static EventSynchronizer getSynchronizer() {
    return instance != null ? instance.synchronizer : null;
  }

  public static RemoteCapabilitiesManager getCapabilitiesManager() {
    return instance != null ? instance.capabilitiesManager : null;
  }

  public static BundleContext getBundleContext() {
    return context;
  }

  private void registerControllerSupport(BundleContext context) {
    Bundle sysBundle = context.getBundle(0);
    switch (sysBundle.getState()) {
    case Bundle.ACTIVE:
      startController(context);
      break;
    case Bundle.STARTING:
      context.addFrameworkListener(this);
      break;
    }
  }

  private void startController(BundleContext context) {
    boolean shutdownOnDisconnect = Boolean.getBoolean("iagent.shutdownOnDisconnect");
    vmCommander = new VMCommander(context, pmpServer, shutdownOnDisconnect);
  }

  private synchronized void registerConsole(BundleContext context) {
    // always trying mBS console first
    try {
      console = new ProSystRemoteConsole();
      console.register(context);
    } catch (Throwable t) {
      // trying Equinox console
      try {
        console = new EquinoxRemoteConsole();
        console.register(context);
      } catch (Throwable t1) {
        console = null;
      }
    }
  }

  private synchronized void unregisterConsole() {
    if (console != null) {
      console.unregister();
      console = null;
    }
  }

  private void registerApplicationAdmin(BundleContext context) {
    try {
      applicationAdmin = new RemoteApplicationAdminImpl();
      applicationAdmin.register(context);
    } catch (Throwable t) {
      applicationAdmin = null;
    }
  }

  private void unregisterApplicationAdmin(BundleContext context) {
    if (applicationAdmin != null) {
      applicationAdmin.unregister(context);
      applicationAdmin = null;
    }
  }

  private boolean registerDeploymentAdmin(Object admin) {
    if (deploymentAdmin == null) {
      try {
        deploymentAdmin = new RemoteDeploymentAdminImpl();
        deploymentAdmin.register(context, admin);
        return true;
      } catch (Throwable t) {
        deploymentAdmin = null;
        return false;
      }
    } else {
      return false;
    }
  }

  private boolean unregisterDeploymentAdmin(Object admin) {
    if (deploymentAdmin != null && deploymentAdmin.getDeploymentAdmin() == admin) {
      deploymentAdmin.unregister(context);
      deploymentAdmin = null;
      return true;
    }
    return false;
  }

  private void setCapability(String capability, boolean value) {
    if (capabilitiesManager != null) {
      capabilitiesManager.setCapability(capability, new Boolean(value));
    }
  }
}
