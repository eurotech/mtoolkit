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
package org.tigris.mtoolkit.osgimanagement.internal.browser.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.tigris.mtoolkit.common.certificates.CertUtils;
import org.tigris.mtoolkit.common.certificates.ICertificateDescriptor;
import org.tigris.mtoolkit.iagent.BundleSnapshot;
import org.tigris.mtoolkit.iagent.DeploymentManager;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.iagent.RemoteService;
import org.tigris.mtoolkit.iagent.event.RemoteBundleEvent;
import org.tigris.mtoolkit.iagent.event.RemoteBundleListener;
import org.tigris.mtoolkit.iagent.event.RemoteDevicePropertyEvent;
import org.tigris.mtoolkit.iagent.event.RemoteDevicePropertyListener;
import org.tigris.mtoolkit.iagent.event.RemoteServiceEvent;
import org.tigris.mtoolkit.iagent.event.RemoteServiceListener;
import org.tigris.mtoolkit.iagent.rpc.Capabilities;
import org.tigris.mtoolkit.osgimanagement.ContentTypeModelProvider;
import org.tigris.mtoolkit.osgimanagement.Util;
import org.tigris.mtoolkit.osgimanagement.installation.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworksView;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.PMPConnectionListener;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.ActionsManager;
import org.tigris.mtoolkit.osgimanagement.internal.preferences.FrameworkPreferencesPage;
import org.tigris.mtoolkit.osgimanagement.model.Framework;
import org.tigris.mtoolkit.osgimanagement.model.FrameworkConnectionListener;
import org.tigris.mtoolkit.osgimanagement.model.Model;
import org.tigris.mtoolkit.osgimanagement.model.SimpleNode;

public final class FrameworkImpl extends Framework implements RemoteBundleListener, RemoteServiceListener,
    RemoteDevicePropertyListener, IAdaptable, ConstantsDistributor {

  private boolean               showServicePropertiesInTree = false;

  public Hashtable              bundleHash;
  public Hashtable              categoryHash;

  // stores services info during connect action
  public Vector                 servicesVector;

  // stores services view nodes
  public Vector                 servicesViewVector;

  private Model                 bundles;

  private volatile boolean      refreshing                  = false;
  private boolean               connecting                  = false;

  private PMPConnectionListener connectionListener;

  // flag indicates user has forced disconnect action
  public boolean                userDisconnect              = false;

  private boolean               supportBundles              = false;
  private boolean               supportServices             = false;
  private IMemento              configs;

  private Set<String>           systemBundlesList;
  public Map<Long, String>      frameworkSystemBundles;

  public FrameworkImpl(String name, boolean autoConnected) {
    super(name, autoConnected);
    configs = XMLMemento.createWriteRoot(MEMENTO_TYPE);
    configs.putString(FRAMEWORK_NAME, name);
  }

  public Model getBundlesNode() {
    if (bundles == null) {
      bundles = new SimpleNode(Messages.bundles_node_label);
      if (getViewType() == BUNDLES_VIEW) {
        addElement(bundles);
      }
    }
    return bundles;
  }

  public void setConfig(IMemento configs) {
    this.configs = configs;
  }

  public void dispose() {
    if (this.isConnected()) {
      this.disconnect();
    }
    FrameworksView.getTreeRoot().removeElement(this);
    bundleHash = null;
    frameworkSystemBundles = null;
    categoryHash = null;
    servicesVector = null;
    servicesViewVector = null;
    connector = null;
  }

  public boolean isConnecting() {
    return connecting;
  }

  public void setConnector(DeviceConnector connector) {
    this.connector = connector;
  }

  public void connect(final DeviceConnector connector, SubMonitor monitor) {
    this.connector = connector;
    this.connectedFlag = true;
    if (monitor == null) {
      monitor = SubMonitor.convert(new NullProgressMonitor());
    }
    if (!initModel(monitor)) {
      ActionsManager.disconnectFrameworkAction(FrameworkImpl.this);
    }
    for (int i = 0; i < listeners.size(); i++) {
      ((FrameworkConnectionListener) listeners.get(i)).connected();
    }
  }

  private void addRemoteListeners() {
    try {
      connector.addRemoteDevicePropertyListener(this);
      DeploymentManager deploymentManager = connector.getDeploymentManager();
      if (supportBundles) {
        deploymentManager.addRemoteBundleListener(this);
      }
      if (supportServices) {
        connector.getServiceManager().addRemoteServiceListener(this);
      }
    } catch (IAgentException e) {
      BrowserErrorHandler.processError(e, connector, userDisconnect);
    }
  }

  private void updateSupportedModels() {
    if (connector != null) {
      Dictionary remoteProperties;
      Object support;
      try {
        remoteProperties = connector.getRemoteProperties();
        support = remoteProperties.get(Capabilities.CAPABILITIES_SUPPORT);
      } catch (IAgentException e) {
        support = null;
        remoteProperties = new Hashtable();
      }

      if (support == null || !Boolean.valueOf(support.toString()).booleanValue()) {
        supportBundles = true;
        supportServices = true;
      } else {
        support = remoteProperties.get(Capabilities.BUNDLE_SUPPORT);
        if (support != null && Boolean.valueOf(support.toString()).booleanValue()) {
          supportBundles = true;
        }
        support = remoteProperties.get(Capabilities.SERVICE_SUPPORT);
        if (support != null && Boolean.valueOf(support.toString()).booleanValue()) {
          supportServices = true;
        }
      }
    }
  }

  private void removeRemoteListeners() {
    try {
      DeploymentManager deploymentManager = connector.getDeploymentManager();
      if (deploymentManager != null) {
        deploymentManager.removeRemoteBundleListener(this);
      }
      connector.getServiceManager().removeRemoteServiceListener(this);
    } catch (final IAgentException e) {
      if (e.getErrorCode() != IAgentErrors.ERROR_DISCONNECTED) {
        BrowserErrorHandler.processError(e, connector, userDisconnect);
      }
    }
  }

  private boolean buildModel(SubMonitor sMonitor) {
    try {
      addBundles(sMonitor);
      if (sMonitor.isCanceled()) {
        return false;
      }

      obtainModelProviders();

      addServices(sMonitor);
      if (sMonitor.isCanceled()) {
        return false;
      }

      int modelTotal = 0;
      if (modelProviders.size() > 0) {
        modelTotal = FrameworkConnectorFactory.CONNECT_PROGRESS_ADDITIONAL / modelProviders.size();
      }

      for (int i = 0; i < modelProviders.size(); i++) {
        SubMonitor monitor = sMonitor.newChild(modelTotal);
        monitor.setTaskName("Retrieve additional providers data");
        ContentTypeModelProvider manager = ((ModelProviderElement) modelProviders.get(i)).getProvider();
        /* Model node = */manager.connect(this, connector, monitor);
        if (monitor.isCanceled()) {
          return false;
        }
      }

    } catch (IAgentException e) {
      BrowserErrorHandler.processError(e, connector, userDisconnect);
      return false;
    } catch (IllegalStateException ise) {
      // connection was closed
      return false;
    }

    return true;
  }

  private boolean initModel(SubMonitor sMonitor) {
    synchronized (Framework.getLockObject(connector)) {
      try {
        connecting = true;
        userDisconnect = false;
        if (connector != null) {
          bundleHash = new Hashtable();
          categoryHash = new Hashtable();
          servicesVector = new Vector();
          servicesViewVector = new Vector();
          frameworkSystemBundles = new HashMap();
          systemBundlesList = null;

          if (sMonitor.isCanceled()) {
            return false;
          }
          updateSupportedModels();
          addRemoteListeners();
          updateElement();
          buildModel(sMonitor);
        }
        updateContextMenuStates();
      } catch (Throwable t) {
        FrameworkPlugin.error("Unexpected exception occurred while connecting to remote framework", t);
      } finally {
        connecting = false;
      }
      if (!refreshing) {
        BrowserErrorHandler.processInfo(name + " successfully " + "connected", false); //$NON-NLS-1$
      }
    }
    return true;
  }

  public void disconnect() {
    synchronized (Framework.getLockObject(connector)) {
      if (!isConnected()) {
        return;
      }
      connectedFlag = false;
      if (connector != null) {
        removeRemoteListeners();
      }

      for (int i = 0; i < modelProviders.size(); i++) {
        ((ModelProviderElement) modelProviders.get(i)).getProvider().disconnect();
      }

      clearModel();
      if (!isAutoConnected()) {
        connector = null;
      }
      updateElement();
      updateContextMenuStates();
      BrowserErrorHandler.processInfo(name + " successfully " + "disconnected", false); //$NON-NLS-1$
    }
    for (int i = 0; i < listeners.size(); i++) {
      ((FrameworkConnectionListener) listeners.get(i)).disconnected();
    }
  }

  private void clearModel() {
    modelProviders.clear();
    removeChildren();
    bundles = null;
    if (bundleHash != null) {
      bundleHash.clear();
    }
    if (categoryHash != null) {
      categoryHash.clear();
    }
    if (servicesVector != null) {
      servicesVector.removeAllElements();
    }
    if (servicesViewVector != null) {
      servicesViewVector.removeAllElements();
    }
    if (frameworkSystemBundles != null) {
      frameworkSystemBundles.clear();
    }
    systemBundlesList = null;
    ServiceObject.usedInHashFWs.remove(this);
    supportBundles = false;
    supportServices = false;
  }

  public Bundle findBundleForService(long id) throws IAgentException {
    if (bundleHash == null) {
      return null;
    }
    Object bundles[] = bundleHash.values().toArray();
    for (int i = 0; i < bundles.length; i++) {
      Model serviceCategories[] = ((Bundle) bundles[i]).getChildren();
      if (serviceCategories != null && serviceCategories.length > 0) {
        if (((ServicesCategory) serviceCategories[0]).getType() == ServicesCategory.REGISTERED_SERVICES) {
          Object services[] = serviceCategories[0].getChildren();
          if (services != null) {
            for (int j = 0; j < services.length; j++) {
              if (((ObjectClass) services[j]).getService().getServiceId() == id) {
                return (Bundle) bundles[i];
              }
            }
          }
        }
      }
    }
    return null;
  }

  public Bundle findBundle(long id) {
    if (bundleHash == null) {
      return null;
    }
    return (Bundle) bundleHash.get(new Long(id));
  }

  public Bundle findBundle(Object id) {
    if (bundleHash == null) {
      return null;
    }
    return (Bundle) bundleHash.get(id);
  }

  public Set getBundlesKeys() {
    if ((bundleHash == null) || (bundleHash.size() == 0)) {
      return Collections.EMPTY_SET;
    }
    synchronized (bundleHash) {
      Set bundleKeys = new HashSet(bundleHash.size(), 1);
      bundleKeys.addAll(bundleHash.keySet());
      return bundleKeys;
    }
  }

  // Overrides method in Model class
  @Override
  public boolean testAttribute(Object target, String name, String value) {
    if (!(target instanceof org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl)) {
      return false;
    }

    if (name.equalsIgnoreCase(FRAMEWORK_STATUS_NAME)) {
      if (value.equalsIgnoreCase(FRAMEWORK_CONNECT_VALUE)) {
        return this.isConnected();
      }
    }
    return false;
  }

  public void setViewType(int viewType) {
    if (this.viewType == viewType) {
      return;
    }
    synchronized (Framework.getLockObject(connector)) {
      this.viewType = viewType;
    }
    Model[] children = getChildren();
    for (int i = 0; i < children.length; i++) {
      removeElement(children[i]);
    }
    if (viewType == FrameworkImpl.SERVICES_VIEW) {
      for (int i = 0; i < servicesViewVector.size(); i++) {
        addElement((Model) servicesViewVector.elementAt(i));
      }
    } else {
      if (supportBundles) {
        Model bundlesNode = getBundlesNode();
        addElement(bundlesNode);
      }
    }
    updateElement();
    for (int i = 0; i < modelProviders.size(); i++) {
      ((ModelProviderElement) modelProviders.get(i)).getProvider().switchView(viewType);
    }
    updateElement();
  }

  protected boolean isShowBundlesID() {
    TreeRoot root = (TreeRoot) getParent();
    if (root != null) {
      return root.isShowBundlesID();
    }
    return false;
  }

  protected boolean isShowBundlesVersion() {
    TreeRoot root = (TreeRoot) getParent();
    if (root != null) {
      return root.isShowBundlesVersion();
    }
    return false;
  }

  public void setShowServicePropertiesInTree(boolean state) {
    showServicePropertiesInTree = state;
  }

  public void clearServicePropertiesNodes(Model parent) {
    Model children[] = parent.getChildren();
    for (int i = 0; i < children.length; i++) {
      if (children[i] instanceof ServiceProperty) {
        parent.removeElement(children[i]);
      } else {
        clearServicePropertiesNodes(children[i]);
      }
    }
  }

  public boolean isShownServicePropertiss() {
    return showServicePropertiesInTree;
  }

  // find and update nodes for bundle in "Bundles" and corresponding "slave"
  // nodes
  private void updateBundleNodes(RemoteBundle rBundle) throws IAgentException {
    long id = rBundle.getBundleId();
    Bundle node = findBundle(id);
    if (node != null) {
      node.update();
      Vector slaves = node.getSlaves();
      if (slaves != null) {
        for (int i = 0; i < slaves.size(); i++) {
          ((Bundle) slaves.elementAt(i)).update();
        }
      }
    }
  }

  private void updateBundleCategory(RemoteBundle rBundle) throws IAgentException {
    long id = rBundle.getBundleId();

    // remove bundle and empty category
    Bundle bundle = findBundle(id);
    bundleHash.remove(new Long(id));

    if (bundle != null && FrameworkPreferencesPage.isBundlesCategoriesShown()) {
      Category category = (Category) bundle.getParent();
      category.removeElement(bundle);
      if (category.getSize() == 0) {
        categoryHash.remove(category.getName());
        category.getParent().removeElement(category);
      }
    }
    removeBundleInServicesView(id);
    addBundle(rBundle);
  }

  private void removeBundle(long id) throws IAgentException {
    // remove bundle and empty category
    Bundle bundle = findBundle(id);
    if (bundle == null) {
      return;
    }
    removeBundleInServicesView(id);
    for (int i = 0; i < bundle.getSlaves().size(); i++) {
      Model parent = ((Model) bundle.getSlaves().elementAt(i)).getParent();
      if (parent != null) {
        parent.removeElement((Model) bundle.getSlaves().elementAt(i));
      }
    }
    FrameworkImpl fw = (FrameworkImpl) bundle.findFramework();
    bundleHash.remove(new Long(id));
    frameworkSystemBundles.remove(new Long(id));
    if (FrameworkPreferencesPage.isBundlesCategoriesShown()) {
      Category category = (Category) bundle.getParent();
      category.removeElement(bundle);
      if (category.getSize() == 0) {
        categoryHash.remove(category.getName());
        category.getParent().removeElement(category);
      }
    } else {
      if (fw != null) {
        fw.getBundlesNode().removeElement(bundle);
      }
    }
  }

  private void removeBundleInServicesView(long id) {
    // remove bundles from services view nodes
    for (int i = servicesViewVector.size() - 1; i >= 0; i--) {
      ObjectClass objClass = (ObjectClass) servicesViewVector.elementAt(i);
      Model children[] = objClass.getChildren();
      for (int j = 0; j < children.length; j++) {
        Model bundles[] = children[j].getChildren();
        for (int k = 0; k < bundles.length; k++) {
          if (((Bundle) bundles[k]).getID() == id) {
            children[j].removeElement(bundles[k]);
            if (j == 0) {
              servicesViewVector.removeElementAt(i);
              removeElement(objClass);
            } else {
              if (children[j].getChildren().length == 0) {
                objClass.removeElement(children[j]);
              }
            }
            break;
          }
        }
      }
    }
  }

  private String getDebugBundleChangedMsg(RemoteBundleEvent e) {
    int type = e.getType();
    String debug = "Bundle state changed " + type; //$NON-NLS-1$

    try {
      if (type == RemoteBundleEvent.UNINSTALLED) {
        debug = "Bundle " + e.getBundle().getBundleId() + " uninstalled"; //$NON-NLS-1$ //$NON-NLS-2$
      } else {
        debug = "Bundle " + getBundleName(e.getBundle(), null) + " "; //$NON-NLS-1$ //$NON-NLS-2$
        switch (type) {
        case (RemoteBundleEvent.INSTALLED): {
          debug += "INSTALLED"; //$NON-NLS-1$
          break;
        }
        case (RemoteBundleEvent.LAZY_STARTED): {
          debug += "LAZY_STARTED"; //$NON-NLS-1$
          break;
        }
        case (RemoteBundleEvent.RESOLVED): {
          debug += "RESOLVED"; //$NON-NLS-1$
          break;
        }
        case (RemoteBundleEvent.STARTED): {
          debug += "STARTED"; //$NON-NLS-1$
          break;
        }
        case (RemoteBundleEvent.STOPPED): {
          debug += "STOPPED"; //$NON-NLS-1$
          break;
        }
        case (RemoteBundleEvent.UNRESOLVED): {
          debug += "UNRESOLVED"; //$NON-NLS-1$
          break;
        }
        case (RemoteBundleEvent.UPDATED): {
          debug += "UPDATED"; //$NON-NLS-1$
          break;
        }
        case (RemoteBundleEvent.STARTING): {
          debug += "STARTING"; //$NON-NLS-1$
          break;
        }
        case (RemoteBundleEvent.STOPPING): {
          debug += "STOPPING"; //$NON-NLS-1$
          break;
        }
        }
      }
    } catch (IAgentException e1) {
    }
    return debug;
  }

  public void bundleChanged(RemoteBundleEvent e) {
    BrowserErrorHandler.debug(getDebugBundleChangedMsg(e));
    synchronized ((Framework.getLockObject(connector))) {
      if (!isConnected()) {
        return;
      }

      final int type = e.getType();
      final RemoteBundle rBundle = e.getBundle();
      long id = rBundle.getBundleId();
      try {
        if (!bundleHash.containsKey(new Long(id)) && type != RemoteBundleEvent.UNINSTALLED) {
          // add this bundle - apparently, we have missed the
          // INSTALLED
          // event
          addBundle(rBundle);
        } else if (type == RemoteBundleEvent.UPDATED) {
          Bundle bundle = findBundle(id);
          String category = rBundle.getHeader(Constants.BUNDLE_CATEGORY, ""); //$NON-NLS-1$
          if (!bundle.getParent().getName().equals(category) && FrameworkPreferencesPage.isBundlesCategoriesShown()) {
            updateBundleCategory(rBundle);
          }
          bundle = findBundle(id);
          String newName = getBundleName(rBundle, null);
          bundle.setName(newName);
          updateBundleNodes(rBundle);
        } else if (type == RemoteBundleEvent.UNINSTALLED) {
          removeBundle(rBundle.getBundleId());
        } else if (type == RemoteBundleEvent.STOPPED) {
          Bundle bundle = findBundle(rBundle.getBundleId());
          bundle.removeChildren();
          removeBundleInServicesView(rBundle.getBundleId());
          updateBundleNodes(rBundle);
        } else if (type == RemoteBundleEvent.STARTED) {
          Bundle bundle = findBundle(rBundle.getBundleId());
          RemoteService usedServ[] = bundle.getRemoteBundle().getServicesInUse();
          if (usedServ.length > 0) {
            for (int i = 0; i < usedServ.length; i++) {
              Model bundleUsedInCategory = getServiceCategoryNode(bundle, ServicesCategory.USED_SERVICES, true);
              addObjectClassNodes(bundleUsedInCategory, usedServ[i].getObjectClass(),
                  new Long(usedServ[i].getServiceId()), usedServ[i]);

              for (int j = 0; j < servicesViewVector.size(); j++) {
                ObjectClass oc = (ObjectClass) servicesViewVector.elementAt(j);
                if (oc.getNameID().longValue() == usedServ[i].getServiceId()) {
                  Model[] categories = oc.getChildren();
                  boolean added = false;
                  BundlesCategory bCategory = null;
                  if (categories.length > 1) {
                    bCategory = (BundlesCategory) oc.getChildren()[1];
                    Model bundles[] = bCategory.getChildren();
                    for (int k = 0; k < bundles.length; k++) {
                      if (((Bundle) bundles[k]).getID() == bundle.getID()) {
                        added = true;
                        break;
                      }
                    }
                  }

                  if (!added) {
                    Bundle usedInBundle = new Bundle(bundle);
                    if (FrameworkPreferencesPage.isBundlesCategoriesShown()) {
                      if (bCategory == null) {
                        bCategory = new BundlesCategory(BundlesCategory.IN_USE);
                        oc.addElement(bCategory);
                      }
                      bCategory.addElement(usedInBundle);
                    } else {
                      getBundlesNode().addElement(usedInBundle);
                    }
                  }
                }
              }

            }
          }
          updateBundleNodes(rBundle);
        } else {
          updateBundleNodes(rBundle);
        }
      } catch (IAgentException ex) {
        // ignore bundle installed exceptions, we will receive
        // uninstalled
        // event shortly
        if (ex.getErrorCode() != IAgentErrors.ERROR_BUNDLE_UNINSTALLED
            && ex.getErrorCode() != IAgentErrors.ERROR_DISCONNECTED
            && ex.getErrorCode() != IAgentErrors.ERROR_CANNOT_CONNECT) {
          BrowserErrorHandler.processError(ex, connector, userDisconnect);
        }
      }
    }
    updateContextMenuStates();
  }

  private void removeServiceNode(Model bundle, long id) {
    Model children[] = bundle.getChildren();
    for (int i = 0; i < children.length; i++) {
      Model services[] = children[i].getChildren();
      for (int j = 0; j < services.length; j++) {
        if (((ObjectClass) services[j]).getNameID().longValue() == id) {
          children[i].removeElement(services[j]);
        }
        if (children[i].getChildren().length == 0) {
          bundle.removeElement(children[i]);
        }
      }
    }
  }

  public void removeService(long id) throws IAgentException {
    try {
      Enumeration bundlesKeys = bundleHash.keys();
      while (bundlesKeys.hasMoreElements()) {
        Object key = bundlesKeys.nextElement();
        Bundle bundle = (Bundle) bundleHash.get(key);
        removeServiceNode(bundle, id);
        for (int i = 0; i < bundle.getSlaves().size(); i++) {
          removeServiceNode((Model) bundle.getSlaves().elementAt(i), id);
        }
      }

      for (int i = servicesViewVector.size() - 1; i >= 0; i--) {
        ObjectClass objClass = (ObjectClass) servicesViewVector.elementAt(i);
        if (objClass.getService().getServiceId() == id) {
          if (viewType == SERVICES_VIEW) {
            removeElement(objClass);
          }
          servicesViewVector.removeElementAt(i);
        }
      }
    } catch (Throwable t) {
      BrowserErrorHandler.processError(t, true);
    }
  }

  private String getDebugServiceChangedMsg(RemoteServiceEvent e) {
    String type = ""; //$NON-NLS-1$
    try {
      switch (e.getType()) {
      case RemoteServiceEvent.UNREGISTERED: {
        type = "UNREGISTERED"; //$NON-NLS-1$
        break;
      }
      case RemoteServiceEvent.MODIFIED: {
        type = "MODIFIED"; //$NON-NLS-1$
        break;
      }
      case RemoteServiceEvent.REGISTERED: {
        type = "REGISTERED"; //$NON-NLS-1$
        break;
      }

      }
      return "Service id = " + e.getService().getServiceId() + " " + type; //$NON-NLS-1$ //$NON-NLS-2$
    } catch (IAgentException e2) {
    }
    return "Service " + type + " " + e; //$NON-NLS-1$ //$NON-NLS-2$
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.event.RemoteServiceListener#serviceChanged(org.tigris.mtoolkit.iagent.event.RemoteServiceEvent)
   */
  public void serviceChanged(final RemoteServiceEvent e) {
    BrowserErrorHandler.debug(getDebugServiceChangedMsg(e));
    if (!isConnected()) {
      return;
    }
    synchronized (Framework.getLockObject(connector)) {
      try {
        RemoteService rService = e.getService();
        if (e.getType() == RemoteServiceEvent.UNREGISTERED) {
          removeService(rService.getServiceId());
        } else {
          RemoteBundle rBundle = rService.getBundle();
          if (rBundle != null) {
            Bundle bundle = findBundle(rBundle.getBundleId());
            if (bundle == null) {
              addBundle(rBundle);
            }
            if (e.getType() == RemoteServiceEvent.REGISTERED) {
              ServiceObject servObj = new ServiceObject(rService, rBundle);
              RemoteBundle usedInBundles[] = rService.getUsingBundles();
              for (int i = 0; i < usedInBundles.length; i++) {
                ServiceObject.addUsedInBundle(rService, usedInBundles[i], this);
              }
              addServiceNodes(servObj);
            }
          }
        }
        updateElement();
      } catch (IAgentException e1) {
        if (e1.getErrorCode() != IAgentErrors.ERROR_SERVICE_UNREGISTERED) {
          //Services might be unregistered at any time even before information
          //for REGISTERED event has been dispatched to clients because PMP events are always asynchronous
          BrowserErrorHandler.processError(e1, connector, userDisconnect);
        }
      } catch (IllegalStateException ex) {
        // ignore illegal states, they are usually due to working with
        // stale
        // data
        BrowserErrorHandler.debug(ex);
      } catch (Throwable t) {
        BrowserErrorHandler.processError(t, connector, userDisconnect);
      }
    }
  }

  private void updateContextMenuStates() {
    Display display = PlatformUI.getWorkbench().getDisplay();
    if (display.isDisposed()) {
      return;
    }
    display.asyncExec(new Runnable() {
      public void run() {
        FrameworksView view = FrameworksView.getActiveInstance();
        if (view != null) {
          view.updateContextMenuStates();
        }
      }
    });
  }

  public void refreshAction() {
    refreshAction(null);
  }

  public void refreshAction(final Composite tree) {
    if (!isConnected() || refreshing) {
      return;
    }
    Job job = new Job(Messages.refresh_framework_info) {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        synchronized (Framework.getLockObject(connector)) {
          SubMonitor sMonitor = SubMonitor.convert(monitor, FrameworkConnectorFactory.CONNECT_PROGRESS);
          sMonitor.setTaskName("Refreshing " + FrameworkImpl.this.getName());
          try {
            SubMonitor connectMonitor = sMonitor.newChild(FrameworkConnectorFactory.CONNECT_PROGRESS_CONNECTING);
            connectMonitor.setTaskName("Refreshing " + FrameworkImpl.this.getName());
            try {
              if (tree != null) {
                tree.getDisplay().asyncExec(new Runnable() {
                  public void run() {
                    tree.setRedraw(false);
                  }
                });
              }
              clearModel();
            } finally {
              if (tree != null) {
                tree.getDisplay().asyncExec(new Runnable() {
                  public void run() {
                    tree.setRedraw(true);
                  }
                });
              }
            }
            updateSupportedModels();
            connectMonitor.worked(FrameworkConnectorFactory.CONNECT_PROGRESS_CONNECTING);
            buildModel(sMonitor);
            updateContextMenuStates();
          } finally {
            refreshing = false;
            sMonitor.done();
          }
        }
        return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
      }
    };
    refreshing = true;
    job.schedule();
  }

  public void setPMPConnectionListener(PMPConnectionListener connectionListener) {
    this.connectionListener = connectionListener;
  }

  public PMPConnectionListener getPMPConnectionListener() {
    return connectionListener;
  }

  public void refreshBundleAction(final Bundle sourceBundle) {
    Job job = new Job(Messages.refresh_bundles_info) {

      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          RemoteBundle rBundlesArray[] = connector.getDeploymentManager().listBundles();
          Hashtable rBundles = new Hashtable(rBundlesArray.length); // all remote bundles
          for (int m = 0; m < rBundlesArray.length; m++) {
            Long bid = new Long(rBundlesArray[m].getBundleId());
            rBundles.put(bid, rBundlesArray[m]);
          }
          Long bid = new Long(sourceBundle.getID());
          if (rBundles.get(bid) == null) { // removed from framework -> remove
            removeBundle(bid.longValue());
          } else { // update
            RemoteService regServ[] = sourceBundle.getRemoteBundle().getRegisteredServices();
            RemoteService usedServ[] = sourceBundle.getRemoteBundle().getServicesInUse();
            removeBundle(bid.longValue());
            RemoteBundle rBundle = (RemoteBundle) rBundles.get(bid);
            addBundle(rBundle);
            updateBundleServices(findBundle(bid.longValue()), regServ, usedServ);
            addServicesInServicesView(findBundle(bid.longValue()), regServ, usedServ);
          }
        } catch (IAgentException e) {
          BrowserErrorHandler.processError(e, false);
        } catch (IllegalStateException e) {
          BrowserErrorHandler.processError(e, false);
        }
        return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
      }

    };
    job.schedule();
  }

  private void updateBundleServices(Bundle bundle, RemoteService regServ[], RemoteService usedServ[])
      throws IAgentException {
    bundle.removeChildren();

    if (regServ.length > 0) {
      Model regServCategory = getServiceCategoryNode(bundle, ServicesCategory.REGISTERED_SERVICES, true);
      for (int i = 0; i < regServ.length; i++) {
        String objClass[] = regServ[i].getObjectClass();
        for (int j = 0; j < objClass.length; j++) {
          ObjectClass oc = new ObjectClass(objClass[j] + " [" + regServ[i].getServiceId() + "]", new Long(
              regServ[i].getServiceId()), regServ[i]);
          regServCategory.addElement(oc);
          if (isShownServicePropertiss()) {
            try {
              addServicePropertiesNodes(oc);
            } catch (IAgentException e) {
              e.printStackTrace();
            }
          }

        }
      }
    }
    if (usedServ.length > 0) {
      Model usedServCategory = getServiceCategoryNode(bundle, ServicesCategory.USED_SERVICES, true);
      for (int i = 0; i < usedServ.length; i++) {
        String objClass[] = usedServ[i].getObjectClass();
        for (int j = 0; j < objClass.length; j++) {
          ObjectClass oc = new ObjectClass(objClass[j] + " [" + usedServ[i].getServiceId() + "]", new Long(
              usedServ[i].getServiceId()), usedServ[i]);
          usedServCategory.addElement(oc);
          if (isShownServicePropertiss()) {
            try {
              addServicePropertiesNodes(oc);
            } catch (IAgentException e) {
              e.printStackTrace();
            }
          }
        }
      }
    }
  }

  private void addServicesInServicesView(Bundle sourceBundle, RemoteService[] regServ, RemoteService[] usedServ)
      throws IAgentException {
    for (int i = 0; i < regServ.length; i++) {
      String objClass[] = regServ[i].getObjectClass();
      for (int j = 0; j < objClass.length; j++) {
        ObjectClass oc = new ObjectClass(objClass[j] + " [" + regServ[i].getServiceId() + "]", new Long(
            regServ[i].getServiceId()), regServ[i]);
        BundlesCategory regCategory = new BundlesCategory(BundlesCategory.REGISTERED);
        oc.addElement(regCategory);
        Bundle newBundle = new Bundle(sourceBundle);
        regCategory.addElement(newBundle);
        servicesViewVector.addElement(oc);
        RemoteBundle rBundlesArray[] = regServ[i].getUsingBundles();
        if (rBundlesArray.length > 0
            && (oc.getChildren().length == 0 || (oc.getChildren().length == 1 && ((BundlesCategory) oc.getChildren()[0])
                .getKind() == BundlesCategory.REGISTERED))) {
          BundlesCategory usedCategory = new BundlesCategory(BundlesCategory.IN_USE);
          oc.addElement(usedCategory);
        }
        for (int k = 0; k < rBundlesArray.length; k++) {
          Bundle bundle = findBundle(rBundlesArray[k].getBundleId());
          oc.getChildren()[1].addElement(new Bundle(bundle));
        }
        if (viewType == SERVICES_VIEW) {
          addElement(oc);
        }
      }
    }
    for (int l = 0; l < servicesViewVector.size(); l++) {
      ObjectClass oc = (ObjectClass) servicesViewVector.elementAt(l);
      for (int m = 0; m < usedServ.length; m++) {
        long id = usedServ[m].getServiceId();
        if (id == oc.getService().getServiceId()) {
          if (oc.getChildren().length == 0
              || (oc.getChildren().length == 1 && ((BundlesCategory) oc.getChildren()[0]).getKind() == BundlesCategory.REGISTERED)) {
            BundlesCategory usedCategory = new BundlesCategory(BundlesCategory.IN_USE);
            oc.addElement(usedCategory);
          }
          oc.getChildren()[1].addElement(new Bundle(sourceBundle));
        }
      }
    }
  }

  public int getFrameWorkStartLevel() throws IAgentException {
    return connector.getVMManager().getFrameworkStartLevel();
  }

  public boolean isRefreshing() {
    return refreshing;
  }

  /**
   * Called when connecting to framework
   *
   * @param monitor
   * @throws IAgentException
   */
  private void addBundles(SubMonitor sMonitor) throws IAgentException {
    if (!supportBundles) {
      return;
    }
    DeviceConnector connector = getConnector();
    if (connector == null) {
      return;
    }

    try {
      addBundlesBySnapshot(sMonitor);
      return;
    } catch (Exception e) {
      // getting bundles snapshot is not supported, retrieve info in
      // standard way
    }

    RemoteBundle rBundlesArray[] = null;

    rBundlesArray = connector.getDeploymentManager().listBundles();

    SubMonitor monitor = sMonitor.newChild(FrameworkConnectorFactory.CONNECT_PROGRESS_BUNDLES);
    monitor.setTaskName(Messages.retrieve_bundles_info);
    int work = (FrameworkConnectorFactory.CONNECT_PROGRESS_BUNDLES / rBundlesArray.length);
    for (int i = 0; i < rBundlesArray.length; i++) {
      try {
        addBundle(rBundlesArray[i]);
      } catch (IAgentException e) {
        if (!userDisconnect && e.getErrorCode() != IAgentErrors.ERROR_BUNDLE_UNINSTALLED) {
          BrowserErrorHandler.processError(e, getConnector(), userDisconnect);
        }
      }
      if (monitor.isCanceled()) {
        return;
      }
      monitor.worked(work);
    }
    retrieveServicesInfo(rBundlesArray, sMonitor);
  }

  private void addBundlesBySnapshot(SubMonitor sMonitor) throws IAgentException {
    if (!supportBundles) {
      return;
    }
    DeviceConnector connector = getConnector();
    if (connector == null) {
      return;
    }
    Hashtable options = new Hashtable();
    BundleSnapshot[] snapshots = connector.getDeploymentManager().getBundlesSnapshot(options);

    int totalWork = FrameworkConnectorFactory.CONNECT_PROGRESS_BUNDLES
        + FrameworkConnectorFactory.CONNECT_PROGRESS_SERVICES;
    SubMonitor monitor = sMonitor.newChild(totalWork);
    monitor.setTaskName(Messages.retrieve_bundles_info);
    int work = (totalWork / snapshots.length);
    for (int i = 0; i < snapshots.length; i++) {
      try {
        RemoteBundle rBundle = snapshots[i].getRemoteBundle();
        addBundle(rBundle, snapshots[i].getBundleHeaders(), snapshots[i].getBundleState());
        retrieveServicesInfo(rBundle, snapshots[i].getRegisteredServices(), snapshots[i].getUsedServices());
      } catch (IAgentException e) {
        if (!userDisconnect && e.getErrorCode() != IAgentErrors.ERROR_BUNDLE_UNINSTALLED) {
          BrowserErrorHandler.processError(e, getConnector(), userDisconnect);
        }
      }
      if (monitor.isCanceled()) {
        return;
      }
      monitor.worked(work);
    }
  }

  private void retrieveServicesInfo(RemoteBundle rBundlesArray[], SubMonitor sMonitor) throws IAgentException {
    if (rBundlesArray != null) {
      SubMonitor monitor = sMonitor.newChild(FrameworkConnectorFactory.CONNECT_PROGRESS_SERVICES);
      monitor.setTaskName(Messages.retrieve_services_info);

      int work = (FrameworkConnectorFactory.CONNECT_PROGRESS_SERVICES / rBundlesArray.length);

      for (int i = 0; i < rBundlesArray.length; i++) {
        retrieveServicesInfo(rBundlesArray[i], null, null);
        if (monitor.isCanceled()) {
          return;
        }
        monitor.worked(work);
      }
    }
  }

  private void retrieveServicesInfo(RemoteBundle rBundle, RemoteService[] registeredSvcs, RemoteService[] usedSvcs)
      throws IAgentException {
    Bundle bundle = findBundle(rBundle.getBundleId());
    if (bundle != null
        && (bundle.getState() != org.osgi.framework.Bundle.ACTIVE || bundle.getState() != org.osgi.framework.Bundle.STARTING)) {
      try {
        if (registeredSvcs == null) {
          registeredSvcs = rBundle.getRegisteredServices();
        }
        if (usedSvcs == null) {
          usedSvcs = rBundle.getServicesInUse();
        }
        for (int j = 0; j < registeredSvcs.length; j++) {
          servicesVector.addElement(new ServiceObject(registeredSvcs[j], rBundle));
        }
        for (int j = 0; j < usedSvcs.length; j++) {
          ServiceObject.addUsedInBundle(usedSvcs[j], rBundle, this);
        }
      } catch (IAgentException e) {
        if (e.getErrorCode() != IAgentErrors.ERROR_BUNDLE_UNINSTALLED) {
          throw e;
        }
      }
    }
  }

  public Model getServiceCategoryNode(Bundle bundle, int type, boolean add) {
    Model[] categories = bundle.getChildren();
    Model category = null;
    if (categories != null) {
      for (int i = 0; i < categories.length; i++) {
        if (((ServicesCategory) categories[i]).getType() == type) {
          category = categories[i];
          break;
        }
      }
    }
    if (category == null && add) {
      category = new ServicesCategory(type);
      bundle.addElement(category);
    }
    return category;
  }

  private void addBundle(RemoteBundle rBundle) throws IAgentException {
    addBundle(rBundle, null, 0);
  }

  private void addBundle(RemoteBundle rBundle, Dictionary headers, int state) throws IAgentException {
    try {
      if (bundleHash.containsKey(new Long(rBundle.getBundleId()))) {
        return;
      }

      if (headers == null) {
        headers = rBundle.getHeaders(null);
      }
      if (state == 0) {
        state = rBundle.getState();
      }
      String categoryName = (String) headers.get(Constants.BUNDLE_CATEGORY);
      Vector categoriesNames = new Vector();
      String bundleName = getBundleName(rBundle, headers);
      String bundleVersion = (String) headers.get(Constants.BUNDLE_VERSION);

      if (categoryName == null) {
        categoriesNames.addElement(Messages.unknown_category_label);
      } else {
        if (categoryName.indexOf(",") != -1) {
          StringTokenizer tokenizer = new StringTokenizer(categoryName, ",");
          while (tokenizer.hasMoreElements()) {
            categoriesNames.addElement(tokenizer.nextToken().trim());
          }
        } else {
          categoriesNames.addElement(categoryName);
        }
      }
      if (FrameworkPreferencesPage.isBundlesCategoriesShown()) {
        for (int i = 0; i < categoriesNames.size(); i++) {
          if (!categoryHash.containsKey(categoriesNames.elementAt(i))) {
            Category category = new Category((String) categoriesNames.elementAt(i));
            getBundlesNode().addElement(category);
            categoryHash.put(categoriesNames.elementAt(i), category);
          }
        }
        Bundle bundleMaster = new Bundle(bundleName, rBundle, state, getRemoteBundleType(rBundle, headers),
            (String) categoriesNames.elementAt(0), bundleVersion);
        Model category = (Model) categoryHash.get(categoriesNames.elementAt(0));
        category.addElement(bundleMaster);
        bundleHash.put(new Long(bundleMaster.getID()), bundleMaster);
        if (isSystemBundle(rBundle)) {
          frameworkSystemBundles.put(new Long(rBundle.getBundleId()), rBundle.getSymbolicName());
        }
        for (int i = 1; i < categoriesNames.size(); i++) {
          category = (Category) categoryHash.get(categoriesNames.elementAt(i));
          Bundle bundle = new Bundle(bundleMaster, (String) categoriesNames.elementAt(i));
          category.addElement(bundle);
        }
      } else {
        Model bundleParentModel = getBundlesNode();

        Bundle bundle = new Bundle(bundleName, rBundle, state, getRemoteBundleType(rBundle, headers),
            (String) categoriesNames.elementAt(0), bundleVersion);
        bundleParentModel.addElement(bundle);
        bundleHash.put(new Long(bundle.getID()), bundle);
        if (isSystemBundle(rBundle)) {
          frameworkSystemBundles.put(new Long(rBundle.getBundleId()), rBundle.getSymbolicName());
        }

      }
    } catch (IllegalArgumentException e) {
      // bundle was uninstalled
    }
  }

  public boolean isSystemBundle(Bundle bundle) {
    return frameworkSystemBundles.containsKey(bundle.getID());
  }

  public boolean isSystemBundle(RemoteBundle bundle) {
    if (systemBundlesList == null) {
      systemBundlesList = FrameworksView.getSystemBundles();
    }
    try {
      return systemBundlesList.contains(bundle.getSymbolicName());
    } catch (IAgentException e) {
      return false;
    }
  }

  private String getBundleName(RemoteBundle bundle, Dictionary headers) throws IAgentException {
    String bundleName = ""; //$NON-NLS-1$
    if (headers == null) {
      headers = bundle.getHeaders(null);
    }
    bundleName = (String) headers.get(Constants.BUNDLE_SYMBOLICNAME);
    if (bundleName == null || bundleName.equals("")) { //$NON-NLS-1$
      bundleName = (String) headers.get(Constants.BUNDLE_NAME);
    }
    if (bundleName == null || bundleName.equals("")) { //$NON-NLS-1$
      bundleName = bundle.getLocation();
      if (bundleName.indexOf('/') != -1) {
        bundleName = bundleName.substring(bundleName.lastIndexOf('/'));
      }
      if (bundleName.indexOf('\\') != -1) {
        bundleName = bundleName.substring(bundleName.lastIndexOf('\\'));
      }
    }
    int delimIndex = bundleName.indexOf(';');
    if (delimIndex != -1) {
      bundleName = bundleName.substring(0, delimIndex);
    }
    return bundleName;
  }

  protected int getRemoteBundleType(RemoteBundle rBundle, Dictionary headers) throws IAgentException {
    try {
      ManifestElement[] fragmentHost = ManifestElement.parseHeader(Constants.FRAGMENT_HOST,
          (String) headers.get(Constants.FRAGMENT_HOST));
      if (fragmentHost != null && fragmentHost.length > 0) {
        if (fragmentHost[0].getDirective(Constants.EXTENSION_DIRECTIVE) != null) {
          return Bundle.BUNDLE_TYPE_EXTENSION;
        } else {
          return Bundle.BUNDLE_TYPE_FRAGMENT;
        }
      }
    } catch (BundleException e) {
      // nothing to do!!!
    }
    return 0;
  }

  private void addServices(SubMonitor sMonitor) throws IAgentException {
    if (!supportServices) {
      return;
    }
    SubMonitor monitor = sMonitor.newChild(1);
    monitor.setTaskName("Deploy services info");
    for (int i = 0; i < servicesVector.size(); i++) {
      ServiceObject servObj = (ServiceObject) servicesVector.elementAt(i);
      addServiceNodes(servObj);
      if (monitor.isCanceled()) {
        return;
      }
    }
  }

  private void addServiceNodes(ServiceObject servObj) throws IAgentException {
    Bundle bundle = findBundle(servObj.getRegisteredIn().getBundleId());
    addServiceNodes(servObj, bundle/* , true */);
  }

  private void addServiceNodes(ServiceObject servObj, Bundle bundle/*
                                                                   * , boolean
                                                                   * first
                                                                   */) throws IAgentException {
    if (bundle.getState() == org.osgi.framework.Bundle.ACTIVE
        || bundle.getState() == org.osgi.framework.Bundle.STARTING
        || bundle.getRemoteBundle().getState() == org.osgi.framework.Bundle.ACTIVE
        || bundle.getRemoteBundle().getState() == org.osgi.framework.Bundle.STARTING) {
      try {
        Model registeredCategory = getServiceCategoryNode(bundle, ServicesCategory.REGISTERED_SERVICES, true);
        addObjectClassNodes(registeredCategory, servObj.getObjectClass(), new Long(servObj.getRemoteService()
            .getServiceId()), servObj.getRemoteService());

        for (int i = 0; i < servObj.getObjectClass().length; i++) {
          ObjectClass hashService = new ObjectClass(servObj.getObjectClass()[i] + " ["
              + servObj.getRemoteService().getServiceId() + "]", new Long(servObj.getRemoteService().getServiceId()),
              servObj.getRemoteService());
          BundlesCategory hashRegisteredCategory = new BundlesCategory(BundlesCategory.REGISTERED);
          hashService.addElement(hashRegisteredCategory);
          hashRegisteredCategory.addElement(new Bundle(bundle));

          RemoteBundle usedInBundles[] = servObj.getUsedIn(this);
          if (usedInBundles != null) {
            BundlesCategory hashUsedCategory = new BundlesCategory(BundlesCategory.IN_USE);
            hashService.addElement(hashUsedCategory);
            for (int k = 0; k < usedInBundles.length; k++) {
              Bundle usedInBundleNode = findBundle(usedInBundles[k].getBundleId());
              if (usedInBundleNode == null) {
                throw new IllegalStateException("Bundle " + servObj.getUsedIn(this)[k].getBundleId() + " is missing"); //$NON-NLS-1$ //$NON-NLS-2$
              }
              hashUsedCategory.addElement(new Bundle(usedInBundleNode));
            }
          }

          for (int j = servicesViewVector.size() - 1; j >= 0; j--) {
            Model model = (Model) servicesViewVector.elementAt(j);
            if (model.getName().equals(hashService.getName())) {
              servicesViewVector.removeElementAt(j);
            }
          }
          servicesViewVector.addElement(hashService);

          if (getViewType() == FrameworkImpl.SERVICES_VIEW) {
            addElement(hashService);
          }
        }

      } catch (IllegalArgumentException e) {
        // bundle was uninstalled
      }
    }

    RemoteBundle usedInBundles[] = servObj.getUsedIn(this);
    if (usedInBundles != null) {
      for (int j = 0; j < usedInBundles.length; j++) {
        Bundle usedInBundle = findBundle(usedInBundles[j].getBundleId());
        if (usedInBundle == null || usedInBundle.getMaster() != null) {
          continue;
        }
        Model usedCategory = getServiceCategoryNode(usedInBundle, ServicesCategory.USED_SERVICES, true);
        addObjectClassNodes(usedCategory, servObj.getObjectClass(),
            new Long(servObj.getRemoteService().getServiceId()), servObj.getRemoteService());
      }
    }
  }

  private void addObjectClassNodes(Model parent, String objClasses[], Long nameID, RemoteService service)
      throws IAgentException {
    for (int i = 0; i < objClasses.length; i++) {
      ObjectClass objClass = new ObjectClass(objClasses[i] + " [" + service.getServiceId() + "]", nameID, service);
      parent.addElement(objClass);
      if (isShownServicePropertiss()) {
        addServicePropertiesNodes(objClass);
      }
    }
  }

  protected void addServicePropertiesNodes(ObjectClass objClass) throws IAgentException {
    Dictionary servProperties = null;
    RemoteService rService = objClass.getService();
    try {
      servProperties = rService.getProperties();
    } catch (IAgentException e) {
      //Services can be unregistered at any time event before clients obtain any information for them
      if (e.getErrorCode() == IAgentErrors.ERROR_SERVICE_UNREGISTERED) {
        return;
      }
      throw e;
    }
    Enumeration keys = servProperties.keys();
    while (keys.hasMoreElements()) {
      String key = (String) keys.nextElement();
      Object value = servProperties.get(key);
      if (value instanceof String[]) {
        String[] values = (String[]) value;
        if (values.length == 1) {
          ServiceProperty node = new ServiceProperty(key + ": " + values[0]);
          objClass.addElement(node);
        } else {
          for (int j = 0; j < values.length; j++) {
            StringBuffer buff = new StringBuffer();
            buff.append(key).append("[").append(String.valueOf(j + 1)).append("]");
            String key2 = buff.toString();
            ServiceProperty node = new ServiceProperty(key2 + ": " + values[j]);
            objClass.addElement(node);
          }
        }
      } else if (value instanceof Collection) {
        Collection col = (Collection) value;
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        boolean first = true;
        for (Iterator it = col.iterator(); it.hasNext();) {
          if (!first) {
            sb.append(", ");
          } else {
            first = false;
          }
          sb.append(it.next());
        }
        sb.append(" }");
        ServiceProperty node = new ServiceProperty(key + ": " + sb.toString());
        objClass.addElement(node);
      } else {
        ServiceProperty node = new ServiceProperty(key + ": " + value.toString());
        objClass.addElement(node);
      }
    }
  }

  public void devicePropertiesChanged(RemoteDevicePropertyEvent e) throws IAgentException {
    if (e.getType() == RemoteDevicePropertyEvent.PROPERTY_CHANGED_TYPE) {
      boolean enabled = ((Boolean) e.getValue()).booleanValue();
      Object property = e.getProperty();
      if (Capabilities.BUNDLE_SUPPORT.equals(property)) {
        if (enabled) {
          Job addJob = new Job(Messages.retrieve_bundles_info) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
              int total = FrameworkConnectorFactory.CONNECT_PROGRESS_BUNDLES;
              SubMonitor sMonitor = SubMonitor.convert(monitor, total);
              sMonitor.setTaskName(Messages.retrieve_bundles_info);
              try {
                addBundles(sMonitor);
              } catch (IAgentException e) {
                return Util.handleIAgentException(e);
              } finally {
                sMonitor.done();
              }
              return Status.OK_STATUS;
            }
          };
          addJob.schedule();
        } else {
          connector.getDeploymentManager().removeRemoteBundleListener(this);
          bundles.removeChildren();
          removeElement(bundles);
          bundles = null;
          supportBundles = false;
        }
      }
      if (Capabilities.SERVICE_SUPPORT.equals(property)) {
        if (enabled) {
          supportServices = true;
          Job addJob = new Job(Messages.retrieve_services_info) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
              int total = FrameworkConnectorFactory.CONNECT_PROGRESS_SERVICES;
              SubMonitor sMonitor = SubMonitor.convert(monitor, total);
              sMonitor.setTaskName(Messages.retrieve_services_info);
              try {
                connector.getServiceManager().addRemoteServiceListener(FrameworkImpl.this);
                retrieveServicesInfo(connector.getDeploymentManager().listBundles(), sMonitor);
                addServices(sMonitor);
              } catch (IAgentException e) {
                return Util.handleIAgentException(e);
              } finally {
                sMonitor.done();
              }
              return Status.OK_STATUS;
            }
          };
          addJob.schedule();
        } else {
          connector.getServiceManager().removeRemoteServiceListener(this);
          // TODO remove services nodes
          supportServices = false;
        }
      }
    }
  }

  @Override
  public IMemento getConfig() {
    return configs;
  }

  /**
   * Returns map, containing information for certificates which shall be used
   * for signing the content, installed to this framework. If no signing is
   * required, then empty Map is returned.
   *
   * @return the map with certificate properties
   */
  @Override
  public Map getSigningProperties() {
    Map properties = new Hashtable();
    List certUids = getSignCertificateUids();
    Iterator signIterator = certUids.iterator();
    int certId = 0;
    while (signIterator.hasNext()) {
      ICertificateDescriptor cert = CertUtils.getCertificate((String) signIterator.next());
      if (cert != null) {
        CertUtils.pushCertificate(properties, cert, certId++);
      }
    }
    return properties;
  }

  public List getModelProviders() {
    return modelProviders;
  }

  protected void obtainModelProviders() {
    modelProviders.clear();
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint extensionPoint = registry
        .getExtensionPoint("org.tigris.mtoolkit.osgimanagement.contentTypeExtensions");

    obtainModelProviderElements(extensionPoint.getConfigurationElements(), modelProviders);
  }

  private void obtainModelProviderElements(IConfigurationElement[] elements, List providers) {
    for (int i = 0; i < elements.length; i++) {
      if (!elements[i].getName().equals("model")) {
        continue;
      }
      String clazz = elements[i].getAttribute("class");
      if (clazz == null) {
        continue;
      }

      ModelProviderElement providerElement = new ModelProviderElement(elements[i]);
      if (providers.contains(providerElement)) {
        continue;
      }

      try {
        Object provider = elements[i].createExecutableExtension("class");

        if (provider instanceof ContentTypeModelProvider) {
          providerElement.setProvider(((ContentTypeModelProvider) provider));
          providers.add(providerElement);
        }
      } catch (CoreException e) {
        FrameworkPlugin.error(e.getMessage(), e);
      }
    }
  }

  @Override
  public List getSignCertificateUids() {
    String keys[] = configs.getAttributeKeys();
    List result = new ArrayList();
    for (int i = 0; i < keys.length; i++) {
      if (keys[i].startsWith(FRAMEWORK_SIGN_CERTIFICATE_ID)) {
        String uid = configs.getString(keys[i]);
        if (uid != null && uid.trim().length() > 0) {
          result.add(uid.trim());
        }
      }
    }
    return result;
  }

  @Override
  public void setSignCertificateUids(List uids) {
    String keys[] = configs.getAttributeKeys();
    for (int i = 0; i < keys.length; i++) {
      if (keys[i].startsWith(FRAMEWORK_SIGN_CERTIFICATE_ID)) {
        configs.putString(keys[i], ""); //$NON-NLS-1$
      }
    }
    Iterator iterator = uids.iterator();
    int num = 0;
    while (iterator.hasNext()) {
      configs.putString(FRAMEWORK_SIGN_CERTIFICATE_ID + num, (String) iterator.next());
      num++;
    }
  }

  @Override
  public Model createModel(String mimeType, String id, String version) {
    Model model = null;
    if (ContentTypeModelProvider.MIME_TYPE_BUNDLE.equals(mimeType)) {
      try {
        model = getResource(id, version);
      } catch (IAgentException e) {
        e.printStackTrace();
      }
    } else {
      List providers = getModelProviders();
      for (int i = 0; i < providers.size(); i++) {
        ModelProviderElement providerElement = ((ModelProviderElement) providers.get(i));
        ContentTypeModelProvider provider = providerElement.getProvider();
        String types[] = provider.getSupportedMimeTypes();
        for (int j = 0; j < types.length; j++) {
          if (mimeType.equals(types[j])) {
            try {
              model = provider.getResource(id, version, this);
            } catch (IAgentException e) {
              e.printStackTrace();
            }
            return model;
          }
        }
      }
    }
    return model;
  }

  private Model getResource(String id, String version) throws IAgentException {
    Bundle master = findBundle(new Long(id));
    Bundle slave = new Bundle(master);

    Model children[] = master.getChildren();
    if (children != null && children.length > 0) {
      for (int i = 0; i < children.length; i++) {
        if (((ServicesCategory) children[i]).getType() == ServicesCategory.REGISTERED_SERVICES) {
          addRegisteredServices(children[i].getChildren(), slave);
        } else {
          addUsedServices(children[i].getChildren(), slave);
        }
      }
    }
    return slave;
  }

  private void addRegisteredServices(Model regServ[], Bundle slave) throws IAgentException {
    if (regServ != null) {
      Model servCategory = getServiceCategoryNode(slave, ServicesCategory.REGISTERED_SERVICES, true);
      for (int i = 0; i < regServ.length; i++) {
        ObjectClass oc = new ObjectClass(regServ[i].getName(), new Long(((ObjectClass) regServ[i]).getService()
            .getServiceId()), ((ObjectClass) regServ[i]).getService());
        servCategory.addElement(oc);
        if (isShownServicePropertiss()) {
          try {
            addServicePropertiesNodes(oc);
          } catch (IAgentException e) {
            e.printStackTrace();
          }
        }
      }
    }
  }

  private void addUsedServices(Model usedServ[], Bundle slave) throws IAgentException {
    if (usedServ != null) {
      Model servCategory = getServiceCategoryNode(slave, ServicesCategory.USED_SERVICES, true);
      for (int i = 0; i < usedServ.length; i++) {
        ObjectClass oc = new ObjectClass(usedServ[i].getName(), new Long(((ObjectClass) usedServ[i]).getService()
            .getServiceId()), ((ObjectClass) usedServ[i]).getService());
        servCategory.addElement(oc);
        if (isShownServicePropertiss()) {
          try {
            addServicePropertiesNodes(oc);
          } catch (IAgentException e) {
            e.printStackTrace();
          }
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
   */
  public Object getAdapter(Class adapter) {
    if (adapter.equals(IWorkbenchAdapter.class)) {
      return new WorkbenchAdapter() {
        @Override
        public String getLabel(Object o) {
          if (o instanceof Model) {
            return ((Model) o).getLabel();
          }
          return "";
        }
      };
    }
    return null;
  }

  /**
   * @param bundleCat
   */
  public void refreshCategoryAction(final Category category) {
    Job job = new Job(Messages.refresh_bundles_info) {

      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          RemoteBundle rBundlesArray[] = connector.getDeploymentManager().listBundles();
          Hashtable rBundles = new Hashtable(rBundlesArray.length); // all remote bundles
          Hashtable rBundlesCategory = new Hashtable(); // remote bundles in this category
          for (int m = 0; m < rBundlesArray.length; m++) {
            Long bid = new Long(rBundlesArray[m].getBundleId());
            rBundles.put(bid, rBundlesArray[m]);
            // check the category
            Dictionary headers = rBundlesArray[m].getHeaders(null);
            String categoryName = (String) headers.get(Constants.BUNDLE_CATEGORY);
            if (category.getName().equals(categoryName)
                || (category.getName().equals("unknown") && categoryName == null)) {
              rBundlesCategory.put(bid, rBundlesArray[m]);
            }
          }
          Model[] bundlesThisCat = category.getChildren();
          for (int i = 0; i < bundlesThisCat.length; i++) {
            Bundle sourceBundle = (Bundle) bundlesThisCat[i];
            Long bid = new Long(sourceBundle.getID());
            if (rBundles.get(bid) == null) { // removed from framework
              removeBundle(bid.longValue());
            } else { // remains in this category or moved to a different category plus update services
              RemoteService regServ[] = sourceBundle.getRemoteBundle().getRegisteredServices();
              RemoteService usedServ[] = sourceBundle.getRemoteBundle().getServicesInUse();
              removeBundle(bid.longValue());
              RemoteBundle rBundle = (RemoteBundle) rBundles.get(bid);
              rBundlesCategory.remove(bid);
              addBundle(rBundle);
              updateBundleServices(findBundle(bid.longValue()), regServ, usedServ);
              addServicesInServicesView(findBundle(bid.longValue()), regServ, usedServ);
            }
          }
          // add new bundle in category
          Iterator newBundles = rBundlesCategory.values().iterator();
          while (newBundles.hasNext()) {
            RemoteBundle rBundle = (RemoteBundle) newBundles.next();
            RemoteService regServ[] = rBundle.getRegisteredServices();
            RemoteService usedServ[] = rBundle.getServicesInUse();
            addBundle(rBundle);
            updateBundleServices(findBundle(rBundle.getBundleId()), regServ, usedServ);
            addServicesInServicesView(findBundle(rBundle.getBundleId()), regServ, usedServ);
          }
        } catch (IAgentException e) {
          BrowserErrorHandler.processError(e, false);
        } catch (IllegalStateException e) {
          BrowserErrorHandler.processError(e, false);
        }
        return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
      }
    };
    job.schedule();
  }

  /**
   * @param service
   */
  public void refreshObjectClassAction(final ObjectClass service) {
    Job job = new Job(Messages.refresh_bundles_info) {

      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          RemoteService rservice = service.getService();
          if (rservice.isStale()) {
            removeService(service.getService().getServiceId());
          }
        } catch (Exception e) {
          // exception in isStale
          BrowserErrorHandler.processError(e, false);
        }
        return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
      }
    };
    job.schedule();
  }
}
