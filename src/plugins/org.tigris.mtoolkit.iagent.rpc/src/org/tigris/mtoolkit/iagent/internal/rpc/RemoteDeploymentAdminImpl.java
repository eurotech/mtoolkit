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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.tigris.mtoolkit.iagent.Error;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.rpc.AbstractRemoteAdmin;
import org.tigris.mtoolkit.iagent.rpc.Capabilities;
import org.tigris.mtoolkit.iagent.rpc.RemoteCapabilitiesManager;
import org.tigris.mtoolkit.iagent.rpc.RemoteDeploymentAdmin;
import org.tigris.mtoolkit.iagent.rpc.spi.DeploymentManagerDelegate;
import org.tigris.mtoolkit.iagent.util.DebugUtils;

public class RemoteDeploymentAdminImpl extends AbstractRemoteAdmin implements RemoteDeploymentAdmin {
  private static final String       EVENT_ADMIN_CLASS = "org.osgi.service.event.EventAdmin";

  private ServiceRegistration       registration;
  private EventAdminTracker         eventAdminTrack;
  private ServiceTracker            delegatesTrack;
  private BundleContext             context;
  private DeploymentAdmin           deploymentAdmin;
  private DeploymentManagerDelegate defaultDelegate;
  private DeploymentEventListener   listener;

  private class EventAdminTracker extends ServiceTracker {
    private EventAdminTracker(BundleContext context, String clazz, ServiceTrackerCustomizer customizer) {
      super(context, clazz, customizer);
    }

    public Object addingService(ServiceReference reference) {
      registerListener();
      return super.addingService(reference);
    }

    public void removedService(ServiceReference reference, Object service) {
      super.removedService(reference, service);
      if (getService() == null) {
        unregisterListener();
      }
    }
  }

  public void register(BundleContext bc, Object admin) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[register] Registering remote Deployment Admin...");
    }

    this.context = bc;
    this.deploymentAdmin = (DeploymentAdmin) admin;

    defaultDelegate = new DefaultDeploymentManagerDelegate(deploymentAdmin);

    delegatesTrack = new ServiceTracker(context, DeploymentManagerDelegate.class.getName(), null);
    delegatesTrack.open();

    eventAdminTrack = new EventAdminTracker(context, EVENT_ADMIN_CLASS, null);
    eventAdminTrack.open(true);

    registration = context.registerService(RemoteDeploymentAdmin.class.getName(), this, null);

    RemoteCapabilitiesManager capMan = Activator.getCapabilitiesManager();
    if (capMan != null) {
      capMan.setCapability(Capabilities.DEPLOYMENT_SUPPORT, new Boolean(true));
    }

    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[removedService] Remote Deployment Admin Registered.");
    }
  }

  public void unregister(BundleContext bc) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[unregister] Unregistering...");
    }

    if (eventAdminTrack != null) {
      eventAdminTrack.close();
      eventAdminTrack = null;
    }

    if (registration != null) {
      registration.unregister();
      registration = null;
    }

    RemoteCapabilitiesManager capMan = Activator.getCapabilitiesManager();
    if (capMan != null) {
      capMan.setCapability(Capabilities.DEPLOYMENT_SUPPORT, new Boolean(false));
    }

    this.context = null;
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[unregister] Unregistered.");
    }
  }

  public DeploymentAdmin getDeploymentAdmin() {
    return deploymentAdmin;
  }

  private void registerListener() {
    if (listener != null) {
      return;
    }
    listener = new DeploymentEventListener();
    listener.register(context, deploymentAdmin);
  }

  private void unregisterListener() {
    if (listener != null) {
      listener.unregister();
      listener = null;
    }
  }

  public Dictionary listDeploymentPackages() {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[listDeploymentPackages] >>>");
    }
    DeploymentAdmin admin = internalGetDeploymentAdmin();

    DeploymentPackage[] packages = admin.listDeploymentPackages();

    Dictionary result = new Hashtable();

    for (int i = 0; i < packages.length; i++) {
      DeploymentPackage dp = packages[i];
      result.put(dp.getName(), dp.getVersion().toString());
    }
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[listDeploymentPackages] Deployment packages: " + DebugUtils.convertForDebug(result));
    }
    return result;
  }

  public Object getDeploymentPackageHeader(String name, String version, String headerName) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getDeploymentPackageHeader] >> dpName: " + name + "; version: " + version
          + "; headerName: " + headerName);
    }
    DeploymentPackage dp = internalGetDeploymentPackage(name, version);
    if (dp == null) {
      Error error = new Error(Error.DEPLOYMENT_UNINSTALLED_CODE, "Deployment package " + name + " (" + version
          + ") has been uninstalled");
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getDeploymentPackageHeader] No such deployment package: " + error);
      }
      return error;
    }
    Object deploymentPackageHeader = dp.getHeader(headerName);
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getDeploymentPackageHeader] Header value: " + deploymentPackageHeader);
    }
    return deploymentPackageHeader;
  }

  public Dictionary getDeploymentPackageBundles(String name, String version) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getDeploymentPackageBundles] >>> dpName: " + name + "; version: " + version);
    }
    DeploymentPackage dp = internalGetDeploymentPackage(name, version);
    if (dp == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getDeploymentPackageBundles] No such deployment package");
      }
      return null;
    }
    BundleInfo[] bundleInfos = dp.getBundleInfos();

    Dictionary result = new Hashtable();
    for (int i = 0; i < bundleInfos.length; i++) {
      BundleInfo bundleInfo = bundleInfos[i];
      result.put(bundleInfo.getSymbolicName(), bundleInfo.getVersion().toString());
    }
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getDeploymentPackageBundles] bundles: " + DebugUtils.convertForDebug(result));
    }
    return result;
  }

  // returns bid or -1 if the bundle is missing
  public long getDeploymentPackageBundle(String dpName, String version, String symbolicName) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getDeploymentPackageBundle] >>> dpName: " + dpName + "; version: " + version
          + "; symbolicName: " + symbolicName);
    }
    DeploymentPackage dp = internalGetDeploymentPackage(dpName, version);
    if (dp == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getDeploymentPackageBundle] No such deployment package");
      }
      return -2; // indicate stale deployment package
    }
    Bundle bundle = dp.getBundle(symbolicName);
    if (bundle != null) {
      long bundleID = bundle.getBundleId();
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getDeploymentPackageBundle] bundle id: " + bundleID);
      }
      return bundleID;
    } else {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getDeploymentPackageBundle] No such bundle");
      }
      return -1; // the bundle is missing
    }
  }

  public Object uninstallDeploymentPackage(String dpName, String version, boolean force) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[uninstallDeploymentPackage] >>> dpName: " + dpName + "; version: " + version
          + "; force: " + force);
    }
    DeploymentPackage dp = internalGetDeploymentPackage(dpName, version);
    if (dp == null) {
      Error error = new Error(Error.DEPLOYMENT_UNINSTALLED_CODE, "Deployment package " + dpName + " (" + version
          + ") has been uninstalled");
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[uninstallDeploymentPackage] No such deployment package: " + error);
      }
      return error;
    }

    Object result = getDelegate().uninstallDeploymentPackage(dp, force);
    if (Boolean.TRUE.equals(result)) {
      // uninstall successful
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[uninstallDeploymentPackage] DP uninstalled");
      }
    } else if (Boolean.FALSE.equals(result)) {
      // uninstall unsuccessful
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[uninstallDeploymentPackage] DP uninstalled unsuccessful");
      }
    } else if (result instanceof Error) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[uninstallDeploymentPackage] Unable to uninstall dp: " + result);
      }
    }
    return result;
  }

  public boolean isDeploymentPackageStale(String dpName, String version) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[isDeploymentPackageStale] >>> dpName: " + dpName + "; version: " + version);
    }
    DeploymentAdmin admin = internalGetDeploymentAdmin();
    DeploymentPackage dp = admin.getDeploymentPackage(dpName);
    if (dp != null && dp.getVersion().toString().equals(version)) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[isDeploymentPackageStale] DP is not stale");
      }
      return false;
    } else {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[isDeploymentPackageStale] DP is stale");
      }
      return true;
    }
  }

  private DeploymentPackage internalGetDeploymentPackage(String name, String version) {
    DeploymentAdmin admin = internalGetDeploymentAdmin();
    DeploymentPackage dp = admin.getDeploymentPackage(name);
    if (dp != null && dp.getVersion().toString().equals(version)) {
      return dp;
    } else {
      return null;
    }
  }

  public String getDeploymentPackageVersion(String dpName) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getDeploymentPackageVersion] >>> dpName: " + dpName);
    }
    DeploymentAdmin admin = internalGetDeploymentAdmin();
    DeploymentPackage dp = admin.getDeploymentPackage(dpName);
    if (dp != null) {
      String dpVersion = dp.getVersion().toString();
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getDeploymentPackageVersion] version: " + dpVersion);
      }
      return dpVersion;
    } else {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getDeploymentPackageVersion] No deployment package with this symbolic name found");
      }
      return null;
    }
  }

  private DeploymentAdmin internalGetDeploymentAdmin() {
    DeploymentAdmin admin = deploymentAdmin;
    if (admin == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[internalGetDeploymentAdmin] DeploymentAdmin has been unregistered.");
      }
      throw new IllegalStateException("DeploymentAdmin is not available");
    }
    return admin;
  }

  public Object installDeploymentPackage(InputStream in) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[installDeploymentPackage] >>> in: " + in);
    }
    Object result = getDelegate().installDeploymentPackage(in);
    if (result instanceof Error) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[installDeploymentPackage] Unable to install DeploymentPackage: " + result);
      }
      return result;
    }
    DeploymentPackage dp = (DeploymentPackage) result;
    String name = dp.getName();
    String version = dp.getVersion().toString();
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[installDeploymentPackage] deployment package installation successful: " + name + "_"
          + version);
    }
    return new String[] {
        name, version
    };
  }

  public Object getDeploymentPackageIcon(String dpName, String version, Dictionary properties) {
    DeploymentPackage dp = internalGetDeploymentPackage(dpName, version);
    if (dp == null) {
      return new Error(Error.DEPLOYMENT_UNINSTALLED_CODE, "Deployment package has been uninstalled");
    }
    URL iconUrl;
    try {
      Method getIconMethod = DeploymentPackage.class.getMethod("getIcon", null); //$NON-NLS-1$
      iconUrl = (URL) getIconMethod.invoke(dp, null);
    } catch (Exception e) {
      return null;
    }
    if (iconUrl == null) {
      return null;
    }

    try {
      return iconUrl.openStream();
    } catch (IOException e) {
      return new Error(IAgentErrors.ERROR_DEPLOYMENT_UNKNOWN, "Failed to get icon: " + e.getMessage());
    }
  }

  private DeploymentManagerDelegate getDelegate() {
    DeploymentManagerDelegate delegate = (DeploymentManagerDelegate) delegatesTrack.getService();
    if (delegate != null) {
      return delegate;
    }
    return defaultDelegate;
  }

  public Class[] remoteInterfaces() {
    return new Class[] {
        RemoteDeploymentAdmin.class
    };
  }

  protected ServiceRegistration getServiceRegistration() {
    return registration;
  }
}
