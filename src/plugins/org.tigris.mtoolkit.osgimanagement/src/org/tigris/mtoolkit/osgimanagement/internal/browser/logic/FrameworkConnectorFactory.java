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
package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IMemento;
import org.tigris.mtoolkit.iagent.DeviceConnectionListener;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.iagent.RemoteDP;
import org.tigris.mtoolkit.iagent.RemoteService;
import org.tigris.mtoolkit.iagent.internal.DeviceConnectorImpl;
import org.tigris.mtoolkit.iagent.internal.connection.AbstractConnection;
import org.tigris.mtoolkit.iagent.internal.connection.ConnectionManager;
import org.tigris.mtoolkit.osgimanagement.internal.ConsoleView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.BundlesCategory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Category;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.DeploymentPackage;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Model;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ObjectClass;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ServiceObject;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ServicesCategory;
import org.tigris.mtoolkit.osgimanagement.internal.preferences.FrameworkPreferencesPage;


public class FrameworkConnectorFactory implements ConstantsDistributor, DeviceConnectionListener {
	
  private static FrameworkConnectorFactory factory;
  public static Display display = null;
  
  public static Hashtable lockObjHash = new Hashtable();
  
  public static boolean isAutoConnectEnabled = FrameworkPreferencesPage.autoConnectDefault;
  public static boolean isAutoStartBundlesEnabled = FrameworkPreferencesPage.autoStartAfterInstall;
  public static Hashtable connectJobs = new Hashtable();
  
  // Constructor
  // TODO: Postpone the SWT Display retrieval
  private FrameworkConnectorFactory() {
    display = Display.getDefault();
  }
  
  static {
    factory = new FrameworkConnectorFactory();
  }
  
  public static void init() {
    try {
      DeviceConnector.addDeviceConnectionListener(factory);
    } catch (IAgentException e) {
      BrowserErrorHandler.processError(e, true);
    }
  }
  
  public static void deinit() {
    DeviceConnector.removeDeviceConnectionListener(factory);
  }


  public static void addBundles(FrameWork fw, boolean initServices, IProgressMonitor monitor) throws IAgentException {
    RemoteBundle rBundles[] = null;
    rBundles = fw.getConnector().getDeploymentManager().listBundles();
    
    if (rBundles != null) {
      if (monitor != null) {
        monitor.beginTask(Messages.retrieve_bundles_info, rBundles.length);
      }

      fw.getBundlesNode();
			for (int i=0; i < rBundles.length; i++) {
			  addBundle(rBundles[i], fw);
			  if (monitor != null) {
	        if (monitor.isCanceled()) {
	          return;
	        }
			    monitor.worked(1);
			  }
			}
    }
    if (monitor != null) {
      monitor.done();
    }
    
    
    if (rBundles != null) {
      if (monitor != null) {
        monitor.beginTask(Messages.retrieve_services_info, rBundles.length);
      }

      for (int i=0; i < rBundles.length; i++) {
        Bundle bundle = fw.findBundle(rBundles[i].getBundleId());
        if (bundle == null || (bundle.getState() != org.osgi.framework.Bundle.ACTIVE && 
            bundle.getState() != org.osgi.framework.Bundle.STARTING))
        continue;
        
        RemoteService rServices[] = rBundles[i].getRegisteredServices();
        for (int j=0; j<rServices.length; j++) {
          fw.servicesVector.addElement(new ServiceObject(rServices[j], rBundles[i]));
        }
        rServices = rBundles[i].getServicesInUse();
        if (rServices != null) {
          for (int j=0; j<rServices.length; j++) {
            ServiceObject.addUsedInBundle(rServices[j], rBundles[i], fw);
          }
        }
        
        if (monitor != null) {
          if (monitor.isCanceled()) {
            return;
          }
          monitor.worked(1);
        }
      }
    }
    if (monitor != null) {
      monitor.done();
    }
  }

  public static void addServicesNodes(FrameWork fw) throws IAgentException {
    for (int i=0; i<fw.servicesVector.size(); i++) {
      ServiceObject servObj = (ServiceObject) fw.servicesVector.elementAt(i);
      addServiceNodes(fw, servObj);
    }
  }
  
  public static void addServiceNodes(FrameWork fw, ServiceObject servObj) throws IAgentException {
    Bundle bundle = fw.findBundle(servObj.getRegisteredIn().getBundleId());
    addServiceNodes(fw, servObj, bundle, true);
    bundle = fw.findBundleInDP(bundle.getID());
    if (bundle != null) {
      addServiceNodes(fw, servObj, bundle, false);
    }
  }
  
  public static void addServiceNodes(FrameWork fw, ServiceObject servObj, Bundle bundle, boolean first) throws IAgentException {
    if (bundle.getState() == org.osgi.framework.Bundle.ACTIVE || 
        bundle.getState() == org.osgi.framework.Bundle.STARTING ||
        bundle.getRemoteBundle().getState() == org.osgi.framework.Bundle.ACTIVE ||
        bundle.getRemoteBundle().getState() == org.osgi.framework.Bundle.STARTING) {
      try {
        addServiceCategoriesNodes(bundle);
        Model categories[] = bundle.getChildren();
        if (categories.length == 0) return;
        ServicesCategory registeredCategory = (ServicesCategory) categories[0];
        createObjectClassNodes(registeredCategory, servObj.getObjectClass(), new Long(servObj.getRemoteService().getServiceId()), servObj.getRemoteService());

        for (int i=0; i<servObj.getObjectClass().length; i++) {
          ObjectClass hashService = new ObjectClass(fw, servObj.getObjectClass()[i]+" [Service "+ servObj.getRemoteService().getServiceId() +"]", new Long(servObj.getRemoteService().getServiceId()), servObj.getRemoteService());
          BundlesCategory hashRegisteredCategory = new BundlesCategory(hashService, BundlesCategory.REGISTERED);
          BundlesCategory hashUsedCategory = new BundlesCategory(hashService, BundlesCategory.IN_USE);
          hashService.addElement(hashRegisteredCategory);
          hashService.addElement(hashUsedCategory);
          hashRegisteredCategory.addElement(new Bundle(bundle.getName(), hashRegisteredCategory, bundle.getRemoteBundle(), bundle.getState(), bundle.getType(), bundle.getCategory()));

          RemoteBundle usedInBundles[] = servObj.getUsedIn(fw);
          if (usedInBundles != null) {
            for (int k=0; k<usedInBundles.length; k++) {
              Bundle usedInBundleNode = fw.findBundle(servObj.getUsedIn(fw)[k].getBundleId());
              if (usedInBundleNode == null) {
                throw new IllegalStateException("Bundle "+servObj.getUsedIn(fw)[k].getBundleId()+" is missing"); //$NON-NLS-1$ //$NON-NLS-2$
              }
              hashUsedCategory.addElement(new Bundle(usedInBundleNode.getName(), hashUsedCategory, usedInBundleNode.getRemoteBundle(), usedInBundleNode.getState(), usedInBundleNode.getType(), usedInBundleNode.getCategory()));
            }
          }

          for (int j=fw.servicesViewVector.size()-1; j>=0; j--) {
            Model model = (Model) fw.servicesViewVector.elementAt(j);
            if (model.getName().equals(hashService.getName())) {
              fw.servicesViewVector.removeElementAt(j);
            }
          }
          fw.servicesViewVector.addElement(hashService);

          if (fw.getViewType() == FrameWork.SERVICES_VIEW) {
            Model children[] = fw.getChildren();
            for (int j=0; j<children.length; j++) {
              if (children[j].getName().equals(hashService.getName())) {
                fw.removeElement(children[j]);
                break;
              }
            }
            fw.addElement(hashService);
          }
        }

      } catch (IllegalArgumentException e) {
        // bundle was uninstalled
      }
    }


    RemoteBundle usedInBundles[] = servObj.getUsedIn(fw);
    if (usedInBundles != null) {
      for (int j=0; j<usedInBundles.length; j++) {
        Bundle usedInBundle = first ? fw.findBundle(usedInBundles[j].getBundleId()) : fw.findBundleInDP(usedInBundles[j].getBundleId());
        if (usedInBundle == null) {
          continue;
        }
        addServiceCategoriesNodes(usedInBundle);
        Model categories[] = usedInBundle.getChildren();
        ServicesCategory usedCategory = (ServicesCategory) categories[1];

        createObjectClassNodes(usedCategory, servObj.getObjectClass(), new Long(servObj.getRemoteService().getServiceId()), servObj.getRemoteService());
      }
    }
  }
  
  public static void addServiceCategoriesNodes(Bundle bundle) throws IAgentException {
    if (bundle.getType() == 0 && (bundle.getChildren() == null || bundle.getChildren().length == 0) &&    
          (bundle.getState() == org.osgi.framework.Bundle.ACTIVE || 
           bundle.getState() == org.osgi.framework.Bundle.STARTING ||
           bundle.getRemoteBundle().getState() == org.osgi.framework.Bundle.STARTING ||
           bundle.getRemoteBundle().getState() == org.osgi.framework.Bundle.ACTIVE)) {
      ServicesCategory registeredCategory = new ServicesCategory(bundle, ServicesCategory.REGISTERED);
      ServicesCategory usedCategory = new ServicesCategory(bundle, ServicesCategory.IN_USE);
      bundle.addElement(registeredCategory);
      bundle.addElement(usedCategory);
    }
  }
  
  public static void createObjectClassNodes(Model parent, String objClasses[], Long nameID, RemoteService service) throws IAgentException {
    for (int i=0; i<objClasses.length; i++) {
      ObjectClass objClass = new ObjectClass(parent, objClasses[i]+" [Service "+ service.getServiceId() + "]", nameID, service);
      parent.addElement(objClass);
    }
  }

  public static synchronized void addBundle(RemoteBundle rBundle, FrameWork framework) throws IAgentException {
    try {
      if (framework.bundleHash.containsKey(new Long(rBundle.getBundleId()))) return;
      Dictionary headers = rBundle.getHeaders(null);
      String categoryName = (String) headers.get("Bundle-Category"); //$NON-NLS-1$
      if (categoryName == null) categoryName = Messages.unknown_category_label;
      String bundleName = getBundleName(rBundle, headers);
      Category category = null;
      if (framework.categoryHash.containsKey(categoryName)) {
        category = (Category)framework.categoryHash.get(categoryName);
      } else {
        category = new Category(categoryName, framework.getBundlesNode());
        framework.categoryHash.put(categoryName, category);
        framework.getBundlesNode().addElement(category);
      }
      Bundle bundle = new Bundle(bundleName, category, rBundle, rBundle.getState(), getRemoteBundleType(rBundle, headers), categoryName);
      if (bundle.getState() == org.osgi.framework.Bundle.ACTIVE || 
          bundle.getState() == org.osgi.framework.Bundle.STARTING) {
        addServiceCategoriesNodes(bundle);
      }
      category.addElement(bundle);
      framework.bundleHash.put(new Long(bundle.getID()), bundle);
    } catch (IllegalArgumentException e) {
      // bundle was uninstalled
    }
  }
  
  public static int getRemoteBundleType(RemoteBundle rBundle, Dictionary headers) throws IAgentException {
    String fragment = (String) headers.get("Fragment-Host"); //$NON-NLS-1$
    int type = 0;
    if (fragment != null && !fragment.equals("")) { //$NON-NLS-1$
      type = Bundle.BUNDLE_TYPE_FRAGMENT;
      RemoteBundle hosts[] = rBundle.getHosts(); 
      if (hosts != null && hosts.length == 1 && hosts[0].getBundleId() == 0) {
        type = Bundle.BUNDLE_TYPE_EXTENSION;
      }
    }
    return type;
  }

  
  public static void addDP(FrameWork framework, IProgressMonitor monitor) throws IAgentException {
    Model deplPackagesNode = framework.getDPNode();
    RemoteDP dps[] = null;
    dps = framework.getConnector().getDeploymentManager().listDeploymentPackages();

    Hashtable dpHash = new Hashtable();
    if (dps != null) {
      if (monitor != null) {
        monitor.beginTask(Messages.retrieve_dps_info, dps.length);
      }
      for (int i=0; i < dps.length; i++) {
        DeploymentPackage dpNode = new DeploymentPackage(dps[i], framework.getDPNode(), framework);
        deplPackagesNode.addElement(dpNode);
        dpHash.put(dps[i].getName(), dpNode);
        if (monitor != null) {
          if (monitor.isCanceled()) {
            return;
          }
          monitor.worked(1);
        }
      }
      framework.setDPHash(dpHash);
      if (monitor != null) {
        monitor.done();
      }
    }
  }
  
  public static void removeBundles(FrameWork framework) {
    Model[] categories = framework.getBundlesNode().getChildren();
    for (int i=0; i < categories.length; i++) {
      framework.getBundlesNode().removeElement(categories[i]);
    }
    framework.removeElement(framework.getBundlesNode());
  }

  public static void removeDPs(FrameWork framework) {
    Model[] categories = framework.getDPNode().getChildren();
    for (int i=0; i < categories.length; i++) {
      framework.getDPNode().removeElement(categories[i]);
    }
    framework.removeElement(framework.getDPNode());
  }

  public static void updateViewType(FrameWork fw) {
    fw.removeChildren();
    if (fw.getViewType() == FrameWork.SERVICES_VIEW) {
      for (int i=0; i<fw.servicesViewVector.size(); i++) {
        fw.addElement((Model)fw.servicesViewVector.elementAt(i));
      }
      fw.updateViewers();
    } else {
      Model bundlesNode = fw.getBundlesNode();
      Model dpNode = fw.getDPNode();
      
      Enumeration keys = fw.categoryHash.keys();
      while (keys.hasMoreElements()) {
        bundlesNode.addElement((Model) fw.categoryHash.get(keys.nextElement()));
      }

      keys = fw.dpHash.keys();
      while (keys.hasMoreElements()) {
        dpNode.addElement((Model) fw.dpHash.get(keys.nextElement()));
      }
    }
  }
  
  public static Vector tmpConnectingFWs = new Vector();
  public static void connectFrameWork(final FrameWork fw) {
    if (tmpConnectingFWs.contains(fw)) return;
    tmpConnectingFWs.addElement(fw);
    // thread used to avoid gui blocking if DeviceConnector.openClientConnection blocks for long time
    Job job = new Job(NLS.bind(Messages.connect_framework, fw.getName())) {
      protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask(NLS.bind(Messages.connect_framework, fw.getName()), 2);
        monitor.worked(1);
        DeviceConnector connector = null;
        boolean failed = false;
        if (fw.getConnector() != null && fw.getConnector().isActive()) {
          connector = fw.getConnector();
          createPMPConnection(connector, fw, fw.getName(), fw.autoConnected);
        } else {
          try {
            IMemento config = fw.getConfig();
            Dictionary aConnProps = new Hashtable();
            aConnProps.put(DeviceConnector.KEY_DEVICE_IP, config.getString(FRAMEWORK_IP_ID));
            aConnProps.put("framework-name", fw.getName()); //$NON-NLS-1$
            aConnProps.put("framework-connection-immediate", new Boolean(false)); //$NON-NLS-1$
            connector = DeviceConnector.openClientConnection(DeviceConnector.TYPE_TCP, aConnProps);
            if (!monitor.isCanceled()) {
              fw.setConnector(connector);
              createPMPConnection(connector, fw, fw.getName(), false);
            }
          } catch (final IAgentException e) {
            BrowserErrorHandler.processError(e, fw.getConnector(), NLS.bind(Messages.connect_error_message, fw.getName()));
            failed = true;
          } catch (final IllegalStateException e) {
            BrowserErrorHandler.processError(e, fw.getConnector(), NLS.bind(Messages.connect_error_message, fw.getName()));
            failed = true;
          }
        }
        tmpConnectingFWs.removeElement(fw);
        monitor.done();
        boolean canceled = monitor.isCanceled();
        if (canceled) {
          failed = true;
          if (fw.autoConnected) {
            fw.disconnect();
          } else {
            DeviceConnector con = connector;
            if (con == null) con = fw.getConnector();
            if (con != null) {
              try {
                con.closeConnection();
              } catch (IAgentException e) {
                BrowserErrorHandler.processError(e, fw);
              }
            }
          }
        }
        if (failed) {
          display.asyncExec(new Runnable() {
            public void run() {
              FrameWorkView.updateContextMenuStates();
            }
          });
        }
        return canceled ? Status.CANCEL_STATUS : Status.OK_STATUS;
      }
    };
    job.schedule();
  }
  
  public static void stopBundle(final Bundle bundle) {
    new Thread() {
      public void run() {
        Job job = new Job(Messages.stop_bundle) {
          protected IStatus run(IProgressMonitor monitor) {
            monitor.beginTask(Messages.stop_bundle, 2);
            monitor.worked(1);
            FrameworkConnectorFactory.stopBundle0(bundle);
            bundle.updateViewers();
            monitor.done();
            return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
          }
        };
        job.schedule();
      }
    }.start();
  }
  
  private static void stopBundle0(Bundle bundle) {
    try {
      final RemoteBundle rBundle = bundle.getRemoteBundle();
      try {
        rBundle.stop(0);
      } catch (IAgentException e) {
        try {
          bundle.setState(rBundle.getState());
        } catch (IAgentException e1) {
          e1.printStackTrace();
        }
        BrowserErrorHandler.processError(e, true);
      } catch (IllegalStateException e) {
        try {
          bundle.setState(rBundle.getState());
        } catch (IAgentException e1) {
          e1.printStackTrace();
        }
        BrowserErrorHandler.processError(e, true);
      }
    } catch (Exception e) {
      BrowserErrorHandler.processError(e, true);
    }    
  }
  
  public static void startBundle(final Bundle bundle) {
    new Thread() {
      public void run() {
        Job job = new Job(Messages.start_bundle) {
          protected IStatus run(IProgressMonitor monitor) {
            monitor.beginTask(Messages.start_bundle, 2);
            monitor.worked(1);
            FrameworkConnectorFactory.startBundle0(bundle);
            bundle.updateViewers();
            monitor.done();
            return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
          }
        };
        job.schedule();
      }
    }.start();
  }
  
  private static void startBundle0(Bundle bundle) {
    try {
      final RemoteBundle rBundle = bundle.getRemoteBundle();
      try {
//        bundle.setState(org.osgi.framework.Bundle.STARTING);
        rBundle.start(0);
      } catch (IAgentException e) {
        try {
          bundle.setState(rBundle.getState());
        } catch (IAgentException e1) {
          e1.printStackTrace();
        }
        e.printStackTrace();
        BrowserErrorHandler.processError(e, true);
      } catch (IllegalStateException e) {
        try {
          bundle.setState(rBundle.getState());
        } catch (IAgentException e1) {
          e1.printStackTrace();
        }
        e.printStackTrace();
        BrowserErrorHandler.processError(e, true);
      }
    } catch (Exception e) {
      e.printStackTrace();
      BrowserErrorHandler.processError(e, true);
    }
  }
  
  public static void updateBundle(final String bundleFileName, final Bundle bundle) {
    new Thread() {
      public void run() {
        Job job = new Job(Messages.update_bundle) {
          protected IStatus run(IProgressMonitor monitor) {
//            monitor.beginTask(ResourceManager.getString("update_bundle"), 2);
//            monitor.worked(1);
            FrameworkConnectorFactory.updateBundle0(bundleFileName, bundle, monitor);
//            monitor.done();
            return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
          }
        };
        job.schedule();
      }
    }.start();
  }

  private static void updateBundle0(String sourceFileName, Bundle bundle, IProgressMonitor monitor) {
    InputStream pis = null;
    try {
      File file = new File(sourceFileName);
      RemoteBundle rBundle = bundle.getRemoteBundle();
      monitor.beginTask(Messages.update_bundle, (int)file.length());
      pis = new ProgressInputStream(new FileInputStream(file), monitor);
      try {
        rBundle.update(pis);
      } catch (IAgentException e) {
        pis.close();
        throw e;
      }
      pis.close();
    } catch (IAgentException e) {
      e.printStackTrace();
      BrowserErrorHandler.processError(e, true);
    } catch (IOException io) {
      io.printStackTrace();
      BrowserErrorHandler.processError(io, true);
    } catch (IllegalStateException e) {
      BrowserErrorHandler.processError(e, true);
    } finally {
      if (pis != null) {
        try {
          pis.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
  
  private static boolean updateInstalledDP = false;
  public static void installDP(final String deplPackageName, final FrameWork framework) {
    new Thread() {
      public void run() {
        Job job = new Job(Messages.install_dp) {
          protected IStatus run(IProgressMonitor monitor) {
            InputStream pis = null;
            try {
              File file = new File(deplPackageName);
              JarInputStream jis = new JarInputStream(new FileInputStream(file));
              Manifest mf = jis.getManifest();
              if (mf == null) {
                BrowserErrorHandler.processError(Messages.missing_manifest, true);
                jis.close();
                return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
              }
              String dpName = mf.getMainAttributes().getValue("DeploymentPackage-SymbolicName"); //$NON-NLS-1$
              String dpVer = mf.getMainAttributes().getValue("DeploymentPackage-Version"); //$NON-NLS-1$
              jis.close();
              RemoteDP prev = framework.getConnector().getDeploymentManager().getDeploymentPackage(dpName);
              if (prev != null && prev.getVersion().equals(dpVer)) {
                final Object syncObj = new Object();
                display.asyncExec(new Runnable() {
                  public void run() {
                    Display display = Display.getCurrent();
                    if (display == null) display = Display.getDefault();
                    updateInstalledDP = MessageDialog.openConfirm(
                        display.getActiveShell(),
                        Messages.install_confirm_title,
                        Messages.duplicate_dp_confirm_message);
                    synchronized (syncObj) {
                      syncObj.notify();
                    }
                  }
                });
                synchronized (syncObj) {
                  try {
                    syncObj.wait();
                  } catch (InterruptedException e) {
                    e.printStackTrace();
                  }
                }
                if (updateInstalledDP) {
                  monitor.beginTask(Messages.uninstall_dp, 2);
                  monitor.worked(1);
                  deinstallDP0(framework.findDP(dpName));
                  monitor.done();
                } else {
                  return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
                }
              }
              
              monitor.beginTask(Messages.install_dp, (int)file.length());
              pis = new ProgressInputStream(new FileInputStream(file), monitor);
              RemoteDP dp = framework.getConnector().getDeploymentManager().installDeploymentPackage(pis);
              try { BrowserErrorHandler.processInfo(NLS.bind(Messages.dp_installed_message, dp.getName()), false); } catch (IAgentException ie) {}
              pis.close();
            } catch (IAgentException e) {
              if (pis != null) {
                try {
                  pis.close();
                } catch (IOException e1) {
                  e1.printStackTrace();
                }
              }
              e.printStackTrace();
              BrowserErrorHandler.processError(e, true);
            } catch (IOException e) {
              if (pis != null) {
                try {
                  pis.close();
                } catch (IOException e1) {
                  e1.printStackTrace();
                }
              }
              e.printStackTrace();
              BrowserErrorHandler.processError(e, true);
            } catch (IllegalStateException e) {
              if (pis != null) {
                try {
                  pis.close();
                } catch (IOException e1) {
                  e1.printStackTrace();
                }
              }
              e.printStackTrace();
              BrowserErrorHandler.processError(e, true);
            }
            return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
          }
        };
        job.schedule();
      }
    }.start();
  }
  
  public static void installBundle(final String bundleFileName, final FrameWork framework, final boolean isTempFile) {
    new Thread() {
      public void run() {
        Job job = new Job(Messages.install_bundle) {
          protected IStatus run(IProgressMonitor monitor) {
            try {
              FrameworkConnectorFactory.installBundle0(bundleFileName, framework, monitor, isTempFile);
            } catch (IAgentException e) {
              e.printStackTrace();
              BrowserErrorHandler.processError(e, true);
            } catch (IllegalStateException e) {
              e.printStackTrace();
              BrowserErrorHandler.processError(e, true);
            }
            return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
          }
        };
        job.schedule();
      }
    }.start();
  }

  private static boolean updateInstalledBundle = false;
  private static void installBundle0(String bundleFileName, FrameWork framework, IProgressMonitor monitor, boolean isTempFile) throws IAgentException {
    File file = new File(bundleFileName);

    InputStream pis = null;
    RemoteBundle rBundle = null;
    // store bundle ids before operation
    Vector ids = framework.getBundlesKeys();

    try {
      monitor.beginTask(Messages.install_bundle, (int)file.length());
      pis = new ProgressInputStream(new FileInputStream(file), monitor);
      // try to install bundle
      rBundle = framework.getConnector().getDeploymentManager().installBundle(file.getName(), pis);
      pis.close();
    } catch (IAgentException e) {
      if (pis != null)
        try {
          pis.close();
        } catch (IOException e1) {
          e1.printStackTrace();
        }
        throw e;
    } catch (IOException e1) {
      try {
        pis.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // if bundle already exists - ask user to update the bundle 
    if (ids.contains(new Long(rBundle.getBundleId()))) {

      final Object syncObj = new Object();
      display.asyncExec(new Runnable() {
        public void run() {
          Display display = Display.getCurrent();
          if (display == null) display = Display.getDefault();
          updateInstalledBundle = MessageDialog.openConfirm(
              display.getActiveShell(),
              Messages.install_confirm_title,
              Messages.duplicate_bundle_confirm_message);
          synchronized (syncObj) {
            syncObj.notify();
          }
        }
      });
      synchronized (syncObj) {
        try {
          syncObj.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      if (updateInstalledBundle) {
        monitor.beginTask(Messages.update_bundle, (int)file.length());
        try {
          pis = new ProgressInputStream(new FileInputStream(file), monitor);
          rBundle.update(pis);
          BrowserErrorHandler.processInfo(NLS.bind(Messages.bundle_updated_message, rBundle.getSymbolicName()), false);
          pis.close();
        } catch (IAgentException e) {
          try {
            pis.close();
          } catch (IOException e1) {
            e1.printStackTrace();
          }
          throw e;
        } catch (IllegalStateException e) {
          try {
            pis.close();
          } catch (IOException e1) {
            e1.printStackTrace();
          }
          throw e;
        } catch (IOException e1) {
          try {
            pis.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        } finally {
          if (isTempFile) {
            file.delete();
          }
        }
      }
    } else {
      BrowserErrorHandler.processInfo(NLS.bind(Messages.bundle_installed_message, getBundleName(rBundle, rBundle.getHeaders(null))), false);
      
      if (isTempFile) {
        file.delete();
      }
    }
    if (isAutoStartBundlesEnabled && rBundle != null) {
      rBundle.start(0);
    }
  }

  public static void deinstallDP(final DeploymentPackage dpNode) {
    new Thread() {
      public void run() {
        Job job = new Job(Messages.uninstall_dp) {
          protected IStatus run(IProgressMonitor monitor) {
            monitor.beginTask(Messages.uninstall_dp, 2);
            monitor.worked(1);
            FrameworkConnectorFactory.deinstallDP0(dpNode);
            monitor.done();
            return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
          }
        };
        job.schedule();
      }
    }.start();
  }
  
  private static void deinstallDP0(DeploymentPackage dpNode) {
    final RemoteDP dp = dpNode.getRemoteDP();
    try {
      try {
        dp.uninstall(false);
        try { BrowserErrorHandler.processInfo(NLS.bind(Messages.dp_deinstalled_message, dp.getName()), false); } catch (IAgentException ie) {}
      } catch (IAgentException e) {
        e.printStackTrace();
        BrowserErrorHandler.processError(e, true);
        MessageBox deinstallDPDialog = new MessageBox(BrowserErrorHandler.getShell(), SWT.ICON_ERROR | SWT.YES | SWT.NO);
        deinstallDPDialog.setText(Messages.uninstall_failed);
        deinstallDPDialog.setMessage(Messages.uninstall_forced);
        int result = deinstallDPDialog.open();
        if (result == SWT.YES) {
          try {
            dp.uninstall(true);
            try { BrowserErrorHandler.processInfo(NLS.bind(Messages.dp_deinstalled_message, dp.getName()), false); } catch (IAgentException ie) {}
          } catch (IAgentException e1) {
            e1.printStackTrace();
            BrowserErrorHandler.processError(e1, true);
          }
        }
      }
    } catch (IllegalStateException e) {
      e.printStackTrace();
      BrowserErrorHandler.processError(e, true);
    }
  }
  
  public static void deinstallBundle(final Bundle bundle) {
    new Thread() {
      public void run() {
        Job job = new Job(Messages.uninstall_bundle) {
          protected IStatus run(IProgressMonitor monitor) {
            monitor.beginTask(Messages.uninstall_bundle, 2);
            monitor.worked(1);
            FrameworkConnectorFactory.deinstallBundle0(bundle);
            monitor.done();
            return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
          }
        };
        job.schedule();
      }
    }.start();
  }
  
  private static void deinstallBundle0(Bundle bundle) {
    try {
      RemoteBundle rBundle = bundle.getRemoteBundle();
      rBundle.uninstall();
      BrowserErrorHandler.processInfo(NLS.bind(Messages.bundle_deinstalled_message, bundle.getName()), false);
    } catch (IAgentException e) {
      e.printStackTrace();
      BrowserErrorHandler.processError(e, true);
    } catch (IllegalStateException e) {
      e.printStackTrace();
      BrowserErrorHandler.processError(e, true);
    }
  }

  public static RemoteBundle getRemoteBundle(String bundleLocation,
                                      FrameWork framework) throws IAgentException {
    
    RemoteBundle bundles[] = framework.getConnector().getDeploymentManager().listBundles();
    RemoteBundle bundle = null;
    
    for (int i=0; i<bundles.length; i++) {
      if (bundles[i].getLocation().equals(bundleLocation)) {
        bundle = bundles[i];
        break;
      }
    }
    return bundle;
  }
  
  public static String getBundleName(RemoteBundle bundle, Dictionary headers) throws IAgentException {
    String bundleName = ""; //$NON-NLS-1$
//    bundleName = bundle.getSymbolicName();
    if (headers == null) headers = bundle.getHeaders(null);
    bundleName = (String) headers.get("Bundle-SymbolicName"); //$NON-NLS-1$
    if (bundleName == null || bundleName.equals("")) { //$NON-NLS-1$
      bundleName = (String) headers.get("Bundle-Name"); //$NON-NLS-1$
    }
    if (bundleName == null || bundleName.equals("")) { //$NON-NLS-1$
      bundleName = bundle.getLocation();
      if (bundleName.indexOf('/') != -1)
        bundleName = bundleName.substring(bundleName.lastIndexOf('/'));
      if (bundleName.indexOf('\\') != -1)
        bundleName = bundleName.substring(bundleName.lastIndexOf('\\'));
    }
    int delimIndex = bundleName.indexOf(';'); 
    if (delimIndex != -1)
      bundleName = bundleName.substring(0, delimIndex);
    return bundleName;
  }

  public void connected(final DeviceConnector connector) {
    String frameworkName = (String) connector.getProperties().get("framework-name"); //$NON-NLS-1$
    boolean autoConnected = true;
    FrameWork fw = null;
    FrameWork fws[] = FrameWorkView.getFrameworks();
    if (fws != null) {
      for (int i=0; i<fws.length; i++) {
        if (fws[i].getName().equals(frameworkName)) {
          fw = fws[i];
          autoConnected = false;
          break;
        }
      }
    }
    
    // generate framework name
    if (fw == null) {
      if (!isAutoConnectEnabled) return;
//      HashMap frameWorkMap = FrameWorkView.getTreeRoot().getFrameWorkMap();
      Hashtable frameWorkMap = new Hashtable();
      if (fws != null) {
        for (int i=0; i<fws.length; i++) {
          frameWorkMap.put(fws[i].getName(), ""); //$NON-NLS-1$
        }
      }
      
      int index = 1;
      String frameWorkName = Messages.new_framework_default_name + ' ' + connector.getProperties().get(DeviceConnector.KEY_DEVICE_IP);
      if (frameWorkMap.containsKey(frameWorkName)) {
        do {
          frameWorkName = Messages.new_framework_default_name + ' ' + connector.getProperties().get(DeviceConnector.KEY_DEVICE_IP)+ "("+ index+")";
          index++;
        } while (frameWorkMap.containsKey(frameWorkName));
      }
      frameworkName = frameWorkName;
      connector.getProperties().put("framework-name", frameworkName); //$NON-NLS-1$
    }

    if (FrameWorkView.getTreeRoot() != null && fw == null) {
      fw = new FrameWork(frameworkName, FrameWorkView.getTreeRoot(), true);
      fw.setName(frameworkName);
      FrameWorkView.getTreeRoot().addElement(fw);
      fw.setConnector(connector);
    }

    BrowserErrorHandler.debug("FrameworkPlugin: "+connector.getProperties().get("framework-name")+" was connected with connector: "+connector); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    // already started connection job for this framework
    if (connectJobs.containsKey(frameworkName)) return;

    if (autoConnected) {
      createPMPConnection(connector, fw, frameworkName, autoConnected);
    }
  }
  
  private static void createPMPConnection(final DeviceConnector connector, FrameWork fw, String frameworkName, boolean autoConnected) {
    boolean pmp = false;
    try {
      pmp = ((DeviceConnectorImpl) connector).getConnectionManager().getActiveConnection(ConnectionManager.PMP_CONNECTION) != null;
    } catch (IAgentException e1) {
      e1.printStackTrace();
    }
    final boolean pmpConnected = pmp;
    
    // create and add pmp connection listener to fw
    PMPConnectionListener pmpListener = fw.getPMPConnectionListener();
    if (pmpListener == null) {
      pmpListener = new PMPConnectionListener(fw, frameworkName, connector, autoConnected);
      fw.setPMPConnectionListener(pmpListener);
    }
    
    final PMPConnectionListener listener = pmpListener;
    
    // force creating of pmp connection
    Job job = new Job(NLS.bind(Messages.connect_framework, fw.getName())) {
      protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask("", 2); //$NON-NLS-1$
        // if pmp connection is available do not force creation but directly connect
        if (pmpConnected) {
          listener.connected();
        } else {
          try {
            connector.getVMManager().isVMActive();
          } catch (IAgentException e) {
            BrowserErrorHandler.processError(e, NLS.bind(Messages.pmp_connect_error_message, connector.getProperties().get("framework-name")), true); //$NON-NLS-1$
            e.printStackTrace();
          }
        }
        monitor.done();
        return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
      }
    };
    connectJobs.put(frameworkName, job);
    job.schedule();
  }

  public void disconnected(DeviceConnector connector) {
    String fwName = (String) connector.getProperties().get("framework-name"); //$NON-NLS-1$
    FrameWork fw = FrameWorkView.findFramework(fwName);
    if (fw == null /*|| !fw.isConnected()*/) return;

    BrowserErrorHandler.debug("FrameworkPlugin: "+fwName +" was disconnected with connector: "+connector); //$NON-NLS-1$ //$NON-NLS-2$
//    BrowserErrorHandler.processInfo(connector.getProperties().get("framework-name") +" successfully disconnected", false);
    synchronized (getLockObject(connector)) {

      FrameWork fws[] = FrameWorkView.getFrameworks();
      if (fws != null) {
        for (int i=0; i<fws.length; i++) {
          fw = fws[i];
          if (fw.getConnector() == connector) {
            fw.disconnect();
            fw.setPMPConnectionListener(null);
            fw.setConnector(null);
            if (fw.autoConnected) {
              FrameWorkView.treeRoot.removeElement(fw);
            }
            break;
          }
        }
      }
      disconnectConsole((String)connector.getProperties().get("framework-name")); //$NON-NLS-1$
    }
  }
  
  public static void disconnectFramework(FrameWork fw) {
    try {
      if (fw.autoConnected) {
        fw.disconnect();
      } else {
        if (fw.monitor != null) {
          fw.monitor.setCanceled(true);
        }
        // wait if connect operation is still active
        synchronized (FrameworkConnectorFactory.getLockObject(fw.getConnector())) {
        }
        if (fw.getConnector() != null) {
          AbstractConnection conn = ((DeviceConnectorImpl)fw.getConnector()).getConnectionManager().getActiveConnection(ConnectionManager.PMP_CONNECTION);
          if (conn != null) {
            conn.closeConnection();
          }
        }
      }
    } catch (IAgentException e) {
      BrowserErrorHandler.processError(e, true);
      e.printStackTrace();
    }
  }
  
  public static void disconnectConsole(String frameworkName) {
      ConsoleView.disconnectServer(frameworkName);
  }

  public static FrameWork getFramework(String fwName) {
    FrameWork fws[] = FrameWorkView.getFrameworks();
    if (fws != null) {
      for (int i=0; i<fws.length; i++) {
        if (fws[i].getName().equals(fwName)) {
          return fws[i];
        }
      }
    }
    return null;
  }
  
  public static Object getLockObject(DeviceConnector connector) {
    Object lockObj = lockObjHash.get(connector);
    if (lockObj == null) {
      lockObj = new Object();
      lockObjHash.put(connector, lockObj);
    }
    return lockObj;
  }
}