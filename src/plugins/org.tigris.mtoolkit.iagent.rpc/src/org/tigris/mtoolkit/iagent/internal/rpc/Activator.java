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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.tigris.mtoolkit.iagent.event.EventSynchronizer;
import org.tigris.mtoolkit.iagent.internal.rpc.console.EquinoxRemoteConsole;
import org.tigris.mtoolkit.iagent.internal.rpc.console.ProSystRemoteConsole;
import org.tigris.mtoolkit.iagent.internal.rpc.console.RemoteConsoleServiceBase;
import org.tigris.mtoolkit.iagent.pmp.PMPPeer;
import org.tigris.mtoolkit.iagent.pmp.PMPServer;
import org.tigris.mtoolkit.iagent.pmp.PMPServerFactory;
import org.tigris.mtoolkit.iagent.pmp.PMPService;
import org.tigris.mtoolkit.iagent.pmp.PMPServiceFactory;
import org.tigris.mtoolkit.iagent.rpc.RemoteCapabilitiesManager;
import org.tigris.mtoolkit.iagent.util.DebugUtils;

public final class Activator implements BundleActivator, ServiceTrackerCustomizer {
  private static final String           DEPLOYMENT_ADMIN_CLASS = "org.osgi.service.deploymentadmin.DeploymentAdmin"; //$NON-NLS-1$

  private static final int              IAGENT_PMP_PORT        = Integer.getInteger("iagent.pmp.port", //$NON-NLS-1$
                                                                   PMPPeer.DEFAULT_PMP_PORT).intValue();

  private static volatile Activator     instance;

  private BundleContext                 context;

  private PMPServer                     pmpServer;

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

  /* (non-Javadoc)
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  public void start(BundleContext context) throws Exception {
    instance = this;
    this.context = context;

    DebugUtils.initialize(this.context);

    synchronizer = new EventSynchronizerImpl(context);

    capabilitiesManager = new RemoteCapabilitiesManagerImpl();
    capabilitiesManager.register(context);

    bundleAdmin = new RemoteBundleAdminImpl();
    bundleAdmin.register(context);

    try {
      applicationAdmin = new RemoteApplicationAdminImpl();
      applicationAdmin.register(context);
    } catch (Throwable t) {
      applicationAdmin = null;
    }

    deploymentAdminTrack = new ServiceTracker(context, DEPLOYMENT_ADMIN_CLASS, this);
    deploymentAdminTrack.open(true);

    serviceAdmin = new RemoteServiceAdminImpl();
    serviceAdmin.register(context);

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

    pmpServiceReg = context.registerService(PMPService.class.getName(), PMPServiceFactory.getDefault(), null);
    pmpServer = PMPServerFactory.createServer(context, IAGENT_PMP_PORT, null);
    pmpServerReg = context.registerService(PMPServer.class.getName(), pmpServer, pmpServer.getProperties());
    synchronizer.setPMPServer(pmpServer);
    synchronizer.start();
  }

  /* (non-Javadoc)
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  public void stop(BundleContext context) throws Exception {
    if (console != null) {
      console.unregister();
      console = null;
    }

    pmpServerReg.unregister();
    pmpServer.close();
    pmpServiceReg.unregister();
    PMPServiceFactory.dispose();

    if (synchronizer != null) {
      synchronizer.unregister();
    }

    if (bundleAdmin != null) {
      bundleAdmin.unregister(context);
      bundleAdmin = null;
    }

    if (applicationAdmin != null) {
      applicationAdmin.unregister(context);
      applicationAdmin = null;
    }

    if (deploymentAdminTrack != null) {
      deploymentAdminTrack.close();
      deploymentAdminTrack = null;
    }

    if (serviceAdmin != null) {
      serviceAdmin.unregister(context);
      serviceAdmin = null;
    }

    if (capabilitiesManager != null) {
      capabilitiesManager.unregister(context);
      capabilitiesManager = null;
    }

    DebugUtils.dispose();
    instance = null;
  }

  /* (non-Javadoc)
   * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
   */
  public Object addingService(ServiceReference arg0) {
    String[] classes = (String[]) arg0.getProperty(Constants.OBJECTCLASS);
    for (int i = 0; i < classes.length; i++) {
      if (classes[i].equals(DEPLOYMENT_ADMIN_CLASS)) {
        Object admin = context.getService(arg0);
        registerDeploymentAdmin(admin);
        return admin;
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
    String[] classes = (String[]) ref.getProperty(Constants.OBJECTCLASS);
    for (int i = 0; i < classes.length; i++) {
      if (classes[i].equals(DEPLOYMENT_ADMIN_CLASS)) {
        if (unregisterDeploymentAdmin(obj)) {
          Object admin = deploymentAdminTrack.getService();
          if (admin != null) {
            registerDeploymentAdmin(admin);
          }
        }
      }
    }
  }

  public static EventSynchronizer getSynchronizer() {
    return instance != null ? instance.synchronizer : null;
  }

  public static RemoteCapabilitiesManager getCapabilitiesManager() {
    return instance != null ? instance.capabilitiesManager : null;
  }

  public static BundleContext getBundleContext() {
    return instance != null ? instance.context : null;
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
}
