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

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.rpc.Error;
import org.tigris.mtoolkit.iagent.rpc.Remote;
import org.tigris.mtoolkit.iagent.rpc.RemoteDeploymentAdmin;

public class RemoteDeploymentAdminImpl implements Remote, RemoteDeploymentAdmin, EventHandler {
	
	  public static final String DEPLOYMENT_EVENT = "d_event";

  private static final String EVENT_ADMIN_CLASS = "org.osgi.service.event.EventAdmin";
  private static final String EVENT_TYPE_KEY = "type";
  private static final String EVENT_DEPLOYMENT_PACKAGE_KEY = "deployment.package";

  private static final int DP_INSTALLED = 1 << 0;
  private static final int DP_UNINSTALLED = 1 << 1;

  private static final String INSTALL_EVENT_TOPIC = "org/osgi/service/deployment/INSTALL";
  private static final String UNINSTALL_EVENT_TOPIC = "org/osgi/service/deployment/UNINSTALL";
  private static final String COMPLETE_EVENT_TOPIC = "org/osgi/service/deployment/COMPLETE";

  private static final String PROP_DEPLOYMENT_PACKAGE = "deploymentpackage.name";
  private static final String PROP_SUCCESSFUL = "successful";

  private ServiceRegistration registration;
  private ServiceRegistration eventHandleRegistration;
  private ServiceTracker eventAdminTrack;
  private BundleContext context;

  private String dpSymbolicName;
  private String dpVersion;

  private DeploymentAdmin deploymentAdmin;

  public void register(BundleContext bc, DeploymentAdmin admin) {
    log("[register] Registering remote Deployment Admin...");

    this.context = bc;
    this.deploymentAdmin = admin;

    registration = context.registerService(RemoteDeploymentAdmin.class.getName(), this, null);

    eventAdminTrack = new ServiceTracker(context, EVENT_ADMIN_CLASS, new ServiceTrackerCustomizer() {
      public Object addingService(ServiceReference arg0) {
        if (registerEventHandler()) {
          return context.getService(arg0);
        } else {
          log("[removedService] EvendAdmin already tracked. Ignoring...");
          return null;
        }
      }

      public void modifiedService(ServiceReference arg0, Object arg1) {
      }

      public void removedService(ServiceReference arg0, Object arg1) {
        unregisterEventHandler();
      }
    });
    eventAdminTrack.open(true);
    log("[removedService] Remote Deployment Admin Registered.");
  }

  private boolean registerEventHandler() {
    if (eventHandleRegistration == null) {
      log("[registerEventHandler] Registering Event handler for deployment package events...");
      Dictionary eventProps = new Hashtable();
      eventProps.put(EventConstants.EVENT_TOPIC, new String[] {
          INSTALL_EVENT_TOPIC,
          UNINSTALL_EVENT_TOPIC,
          COMPLETE_EVENT_TOPIC,
      });
      eventHandleRegistration = context.registerService(EventHandler.class.getName(), this, eventProps);
      log("[registerEventHandler] Event handler registered.");
      return true;
    } else {
      return false;
    }
  }

  private void unregisterEventHandler() {
    if (eventHandleRegistration != null) {
      log("[unregisterEventHandler] Unregistering event handler for deployment packages...");
      eventHandleRegistration.unregister();
      eventHandleRegistration = null;
      log("[unregisterEventHandler] Event handler unregistered.");
    } else {
    }
  }

  public void unregister(BundleContext bc) {
    log("[unregister] Unregistering...");
    if (eventHandleRegistration != null) {
      eventHandleRegistration.unregister();
      eventHandleRegistration = null;
    }

    if (registration != null) {
      registration.unregister();
      registration = null;
    }

    this.context = null;
    log("[unregister] Unregistered.");
  }

  public Dictionary listDeploymentPackages() {
    log("[listDeploymentPackages] >>>");
    DeploymentAdmin admin = internalGetDeploymentAdmin();

    DeploymentPackage[] packages = admin.listDeploymentPackages();

    Dictionary result = new Hashtable();

    for (int i = 0; i < packages.length; i++) {
      DeploymentPackage dp = packages[i];
      result.put(dp.getName(), dp.getVersion().toString());
    }
    log("[listDeploymentPackages] Deployment packages: " + DebugUtils.convertForDebug(result));
    return result;
  }

  public Object getDeploymentPackageHeader(String name, String version, String headerName) {
    log("[getDeploymentPackageHeader] >> dpName: " + name + "; version: " + version+ "; headerName: " + headerName);
    DeploymentPackage dp = internalGetDeploymentPackage(name, version);
    if (dp == null) {
      Error error = new Error(Error.DEPLOYMENT_UNINSTALLED_CODE, null);
      log("[getDeploymentPackageHeader] No such deployment package", error);
      return error;
    }
    Object deploymentPackageHeader = dp.getHeader(headerName);
    log("[getDeploymentPackageHeader] Header value: " + deploymentPackageHeader);
    return deploymentPackageHeader;
  }

  public Dictionary getDeploymentPackageBundles(String name, String version) {
    log("[getDeploymentPackageBundles] >>> dpName: " + name + "; version: " + version);
    DeploymentPackage dp = internalGetDeploymentPackage(name, version);
    if (dp == null){
      log("[getDeploymentPackageBundles] No such deployment package");
      return null;
    }
    BundleInfo[] bundleInfos = dp.getBundleInfos();

    Dictionary result = new Hashtable();
    for (int i = 0; i < bundleInfos.length; i++) {
      BundleInfo bundleInfo = bundleInfos[i];
      result.put(bundleInfo.getSymbolicName(), bundleInfo.getVersion().toString());
    }
    log("[getDeploymentPackageBundles] bundles: " + DebugUtils.convertForDebug(result));
    return result;
  }

  // returns bid or -1 if the bundle is missing
  public long getDeploymentPackageBundle(String dpName, String version, String symbolicName) {
    log("[getDeploymentPackageBundle] >>> dpName: " + dpName + "; version: " + version+ "; symbolicName: " + symbolicName);
    DeploymentPackage dp = internalGetDeploymentPackage(dpName, version);
    if (dp == null) {
      log("[getDeploymentPackageBundle] No such deployment package");
      return -2;    // indicate stale deployment package
    }
    Bundle bundle = dp.getBundle(symbolicName);
    if (bundle != null) {
      long bundleID = bundle.getBundleId();
      log("[getDeploymentPackageBundle] bundle id: " + bundleID);
      return bundleID;
    } else {
      log("[getDeploymentPackageBundle] No such bundle");
      return -1; // the bundle is missing
    }
  }

  public Object uninstallDeploymentPackage(String dpName, String version, boolean force) {
    log("[uninstallDeploymentPackage] >>> dpName: " + dpName + "; version: " + version+ "; force: " + force);
    DeploymentPackage dp = internalGetDeploymentPackage(dpName, version);
    if (dp == null) {
      Error error = new Error(Error.DEPLOYMENT_UNINSTALLED_CODE, null);
      log("[uninstallDeploymentPackage] No such deployment pacakge", error);
      return error;
    }

    if (force) {
      try {
        dp.uninstall();
        log("[uninstallDeploymentPackage] DP uninstalled");
        return new Boolean(true);
      } catch (DeploymentException e) {
        Error error = new Error(org.tigris.mtoolkit.iagent.internal.utils.ExceptionCodeHelper.fromDeploymentExceptionCode(e.getCode()), "Failed to uninstall deployment package: " + e.getMessage());
        log("[uninstallDeploymentPackage] Unable to uninstall dp", error);
        return error;
      }
    } else {
      try {
        boolean uninstallSuccess = dp.uninstallForced();
        log("[uninstallDeploymentPackage] Forced uninstall result: " + uninstallSuccess);
        return new Boolean(uninstallSuccess);   // OK
      } catch (DeploymentException e) {
        Error error = new Error(org.tigris.mtoolkit.iagent.internal.utils.ExceptionCodeHelper.fromDeploymentExceptionCode(e.getCode()), "Failed to uninstall deployment package: " + e.getMessage());
        log("[uninstallDeploymentPackage] Unable to uninstall dp", error);
        return error;
      }
    }
  }

  public boolean isDeploymentPackageStale(String dpName, String version) {
    log("[isDeploymentPackageStale] >>> dpName: " + dpName + "; version: " + version);
    DeploymentAdmin admin = internalGetDeploymentAdmin();
    DeploymentPackage dp = admin.getDeploymentPackage(dpName);
    if (dp != null && dp.getVersion().toString().equals(version)) {
      log("[isDeploymentPackageStale] DP is not stale");
      return false;
    } else {
      log("[isDeploymentPackageStale] DP is stale");
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
    log("[getDeploymentPackageVersion] >>> dpName: " + dpName);
    DeploymentAdmin admin = internalGetDeploymentAdmin();
    DeploymentPackage dp = admin.getDeploymentPackage(dpName);
    if (dp != null) {
      String dpVersion = dp.getVersion().toString();
      log("[getDeploymentPackageVersion] version: " + dpVersion);
      return dpVersion;
    } else {
      log("[getDeploymentPackageVersion] No deployment package with this symbolic name found");
      return null;
    }
  }

  private DeploymentAdmin internalGetDeploymentAdmin() {
    DeploymentAdmin admin = deploymentAdmin;
    if (admin == null) {
      log("[internalGetDeploymentAdmin] DeploymentAdmin has been unregistered.");
      throw new IllegalStateException("DeploymentAdmin is not available");
    }
    return admin;
  }

  public Object installDeploymentPackage(InputStream in) {
    log("[installDeploymentPackage] >>> in: " + in);
    DeploymentAdmin admin = internalGetDeploymentAdmin();
    try {
      DeploymentPackage dp = admin.installDeploymentPackage(in);
      String name = dp.getName();
      String version = dp.getVersion().toString();
      log("[installDeploymentPackage] deployment package installation successful: " + name + "_" + version);
      return new String[] { name, version };
    } catch (DeploymentException e) {
      Error error = new Error(org.tigris.mtoolkit.iagent.internal.utils.ExceptionCodeHelper.fromDeploymentExceptionCode(e.getCode()), "Failed to install deployment package: " + e.getMessage());
      log("[installDeploymentPackage] Unable to install DeploymentPackage: " + error, e);
      return error;
    }
  }

  public Class[] remoteInterfaces() {
    return new Class[] { RemoteDeploymentAdmin.class };
  }

  public void handleEvent(Event event) {
    String symbolicName = (String) event.getProperty(PROP_DEPLOYMENT_PACKAGE);
    DebugUtils.log(this, "[handleEvent] >>> name: " + symbolicName + "; topic: " + event.getTopic());
    if (INSTALL_EVENT_TOPIC.equals(event.getTopic())) {
      handleInstall(symbolicName);
    } else if (UNINSTALL_EVENT_TOPIC.equals(event.getTopic())) {
      handleUninstall(symbolicName);
    } else if (COMPLETE_EVENT_TOPIC.equals(event.getTopic())) {
      boolean successful = ((Boolean) event.getProperty(PROP_SUCCESSFUL)).booleanValue();
      handleComplete(symbolicName, successful);
    } // else ignore
  }

  private void handleInstall(String symbolicName) {
    DebugUtils.log(this, "[handleInstall] newSymbolicName: " + symbolicName + "; oldSymbolicName: " + dpSymbolicName);
    if (dpSymbolicName != null)
      return;
    dpSymbolicName = symbolicName;
  }

  private void handleUninstall(String symbolicName) {
    DeploymentAdmin deploymentAdmin = this.deploymentAdmin;
    if (deploymentAdmin == null) {
      DebugUtils.log(this, "[handleUninstall] DeploymentAdmin is unavailable");
      return;
    }
    DeploymentPackage dp = deploymentAdmin.getDeploymentPackage(symbolicName);
    if (dp == null) {
      DebugUtils.log(this, "[handleUninstall] No such deployment package with symbolic name: " + symbolicName);
      return;
    }
    if (dpSymbolicName != null) {
      DebugUtils.log(this, "[handleUninstall] Uninstallation already in progress for: " + dpSymbolicName + " Check for bug in deployment admin.");
      return;   // don't handle uninstall events for bundles
    }
    dpSymbolicName = symbolicName;
    dpVersion = dp.getVersion().toString();
    DebugUtils.log(this, "[handleUninstall] Uninstallation in progress for: " + dpSymbolicName + "_" + dpVersion);
  }

  private void handleComplete(String symbolicName, boolean successful) {
    if (dpSymbolicName == null) {
      DebugUtils.log(this, "[handleComplete] There is no information for started session");
      return;
    }
    if (dpSymbolicName != null && dpSymbolicName.equals(symbolicName)) {
      try {
        if (successful) {
          Dictionary event;
          EventSynchronizer synchronizer = Activator.getSynchronizer();
          if (synchronizer != null) {
            if (dpVersion != null) {  // uninstall event
              DebugUtils.log(this, "[handleComplete] Uninstall event completed for: " + dpSymbolicName + "_" + dpVersion);
              event = convertDeploymentEvent(dpSymbolicName, dpVersion, DP_UNINSTALLED);
            } else {    // install event
              DeploymentAdmin deploymentAdmin = this.deploymentAdmin;   // save the reference before someone stills it:)
              if (deploymentAdmin == null)
                return;
              DeploymentPackage dp = deploymentAdmin.getDeploymentPackage(dpSymbolicName);
              dpVersion = dp.getVersion().toString();
              DebugUtils.log(this, "[handleComplete] Install event completed for: " + dpSymbolicName + "_" + dpVersion);
              event = convertDeploymentEvent(dpSymbolicName, dpVersion, DP_INSTALLED);
            }
            synchronizer.enqueue(new EventData(event, DEPLOYMENT_EVENT));
          } else {
            log("[handleComplete] Event synchronizer was disabled.");
          }
        } else {
          log("[handleComplete] Deployment package event is not successful");
        }
      } finally {
        dpSymbolicName = null;
        dpVersion = null;
      }
    }
  }

  private Dictionary convertDeploymentEvent(String symbolicName, String version, int type) {
    Dictionary event = new Hashtable();
    event.put(EVENT_TYPE_KEY, new Integer(type));
    event.put(EVENT_DEPLOYMENT_PACKAGE_KEY, new String[] {symbolicName, version});
    return event;
  }

  private static final void log(String message) {
    log(message, (Throwable) null);
  }

  private static final void log(String message, Throwable e) {
    DebugUtils.log(RemoteDeploymentAdminImpl.class, message, e);
  }


  // XXX: Extract this method in common base class
  public long getRemoteServiceID() {
    try {
      ServiceRegistration localRegistration = registration;
      if (localRegistration == null)
        return -1;
      ServiceReference localRef = localRegistration.getReference();
      if (localRef == null)
        return -1;
      return ((Long) localRef.getProperty(Constants.SERVICE_ID)).longValue();
    } catch (IllegalStateException e) {
      // catch it in case the service is unregistered mean while
      return -1;
    }
  }

  private static final void log(String message, Error error){
    if (DebugUtils.DEBUG)
      log(message + (error != null ? " [" + error + "]": ""), (Throwable) null);
  }


}
