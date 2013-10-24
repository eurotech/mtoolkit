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
package org.tigris.mtoolkit.osgimanagement.application;

import java.util.Dictionary;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.tigris.mtoolkit.iagent.ApplicationManager;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteApplication;
import org.tigris.mtoolkit.iagent.event.RemoteApplicationEvent;
import org.tigris.mtoolkit.iagent.event.RemoteApplicationListener;
import org.tigris.mtoolkit.iagent.event.RemoteDevicePropertyEvent;
import org.tigris.mtoolkit.iagent.event.RemoteDevicePropertyListener;
import org.tigris.mtoolkit.iagent.rpc.Capabilities;
import org.tigris.mtoolkit.osgimanagement.ContentTypeModelProvider;
import org.tigris.mtoolkit.osgimanagement.Util;
import org.tigris.mtoolkit.osgimanagement.application.model.Application;
import org.tigris.mtoolkit.osgimanagement.application.model.ApplicationPackage;
import org.tigris.mtoolkit.osgimanagement.model.Framework;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public final class ApplicationModelProvider implements ContentTypeModelProvider, RemoteApplicationListener,
    RemoteDevicePropertyListener {
  private ApplicationPackage applicationsNode;
  private DeviceConnector    connector;
  private Framework          fw;
  private ApplicationManager manager;
  private boolean            supportApplications;

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.ContentTypeModelProvider#connect(org.tigris.mtoolkit.osgimanagement.model.Model, org.tigris.mtoolkit.iagent.DeviceConnector, org.eclipse.core.runtime.IProgressMonitor)
   */
  public void connect(Framework fw, IProgressMonitor monitor) {
    this.fw = fw;
    this.connector = fw.getConnector();
    if (connector != null && connector.isActive()) {
      supportApplications = isApplicationsSupported(connector);
      try {
        connector.addRemoteDevicePropertyListener(this);
      } catch (IAgentException e1) {
        e1.printStackTrace();
      }

      if (supportApplications) {
        initModel(monitor);
      }
    }
    monitor.done();
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.ContentTypeModelProvider#disconnect()
   */
  public void disconnect() {
    if (manager != null) {
      try {
        manager.removeRemoteApplicationListener(this);
      } catch (IAgentException e) {
        e.printStackTrace();
      }
    }
    if (fw != null) {
      if (applicationsNode != null) {
        fw.removeElement(applicationsNode);
      }
      fw = null;
    }
    if (connector != null) {
      try {
        connector.removeRemoteDevicePropertyListener(this);
      } catch (IAgentException e) {
      }
      connector = null;
    }
    applicationsNode = null;
    supportApplications = false;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.ContentTypeModelProvider#switchView(int)
   */
  public Model switchView(int viewType) {
    Model node = null;
    if (supportApplications && applicationsNode != null) {
      if (viewType == Framework.BUNDLES_VIEW) {
        fw.addElement(applicationsNode);
        node = applicationsNode;
      } else if (viewType == Framework.SERVICES_VIEW) {
        fw.removeElement(applicationsNode);
      }
    }
    return node;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.event.RemoteApplicationListener#applicationChanged(org.tigris.mtoolkit.iagent.event.RemoteApplicationEvent)
   */
  public void applicationChanged(RemoteApplicationEvent event) {
    if (applicationsNode == null) {
      return;
    }
    synchronized (fw.getLockObject()) {
      String remoteAppID = null;
      RemoteApplication remoteApp = event.getApplication();
      try {
        remoteAppID = remoteApp.getApplicationId();
      } catch (IAgentException e) {
        // TODO handle exception
        e.printStackTrace();
        return;
      }
      if (remoteAppID == null) {
        return;
      }
      if (event.getType() == RemoteApplicationEvent.INSTALLED) {
        Application application = new Application(remoteAppID, remoteApp);
        applicationsNode.addElement(application);
      } else if (event.getType() == RemoteApplicationEvent.UNINSTALLED) {
        Model applications[] = applicationsNode.getChildren();
        for (int i = 0; i < applications.length; i++) {
          Application app = (Application) applications[i];
          if (remoteAppID.equals(app.getApplicationID())) {
            applicationsNode.removeElement(app);
            break;
          }
        }
      } else if (event.getType() == RemoteApplicationEvent.STARTED || event.getType() == RemoteApplicationEvent.STOPPED) {
        updateApplicationState(remoteApp, remoteAppID);
      }
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.event.RemoteDevicePropertyListener#devicePropertiesChanged(org.tigris.mtoolkit.iagent.event.RemoteDevicePropertyEvent)
   */
  public void devicePropertiesChanged(RemoteDevicePropertyEvent e) throws IAgentException {
    if (e.getType() == RemoteDevicePropertyEvent.PROPERTY_CHANGED_TYPE) {
      boolean enabled = ((Boolean) e.getValue()).booleanValue();
      Object property = e.getProperty();
      if (Capabilities.APPLICATION_SUPPORT.equals(property)) {
        if (enabled) {
          supportApplications = true;
          initModel(new NullProgressMonitor());
        } else {
          supportApplications = false;
          manager = (ApplicationManager) connector.getManager(ApplicationManager.class.getName());
          try {
            manager.removeRemoteApplicationListener(this);
          } catch (IAgentException ex) {
            ex.printStackTrace();
          }
          if (applicationsNode != null) {
            applicationsNode.removeChildren();
            fw.removeElement(applicationsNode);
            applicationsNode = null;
          }

        }
      }
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.ContentTypeModelProvider#getResource(java.lang.String, java.lang.String, org.tigris.mtoolkit.osgimanagement.model.Framework)
   */
  public Model getResource(String id, String version, Framework fw) throws IAgentException {
    return null;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.ContentTypeModelProvider#getSupportedMimeTypes()
   */
  public String[] getSupportedMimeTypes() {
    return null;
  }

  static boolean isApplicationsSupported(DeviceConnector connector) {
    if (connector == null) {
      return true;
    }
    Framework fw = Util.findFramework(connector);
    if (fw == null) {
      return false;
    }
    Dictionary remoteProperties = fw.getRemoteDeviceProperties();
    if (remoteProperties == null) {
      return false;
    }
    Object support = remoteProperties.get(Capabilities.CAPABILITIES_SUPPORT);
    if (support == null || !Boolean.valueOf(support.toString()).booleanValue()) {
      return true;
    } else {
      support = remoteProperties.get(Capabilities.APPLICATION_SUPPORT);
      if (support != null && Boolean.parseBoolean(support.toString())) {
        return true;
      }
    }
    return false;
  }

  private void initModel(IProgressMonitor monitor) {
    applicationsNode = new ApplicationPackage("Applications");
    if (fw.getViewType() == Framework.BUNDLES_VIEW) {
      fw.addElement(applicationsNode);
    }
    try {
      manager = (ApplicationManager) connector.getManager(ApplicationManager.class.getName());
      addApplications(monitor);
      try {
        manager.addRemoteApplicationListener(this);
      } catch (IAgentException e) {
        e.printStackTrace();
      }
    } catch (IAgentException e) {
      e.printStackTrace();
    }
  }

  private void addApplications(IProgressMonitor monitor) {
    try {
      RemoteApplication[] applications = manager.listApplications();
      SubMonitor sMonitor = SubMonitor.convert(monitor, applications.length);
      for (int i = 0; i < applications.length; i++) {
        Application application = new Application(applications[i].getApplicationId(), applications[i]);
        applicationsNode.addElement(application);
        sMonitor.worked(1);
      }
    } catch (IAgentException e) {
      if (e.getErrorCode() != IAgentErrors.ERROR_REMOTE_ADMIN_NOT_AVAILABLE) {
        e.printStackTrace();
      } else {
        // no support for applications
        // do nothing
      }
    }
  }

  private void updateApplicationState(RemoteApplication remoteApp, String remoteAppID) {
    if (applicationsNode == null) {
      return;
    }
    Model applications[] = applicationsNode.getChildren();
    for (int i = 0; i < applications.length; i++) {
      try {
        Application app = (Application) applications[i];
        if (remoteAppID.equals(app.getApplicationID())) {
          app.setState(remoteApp.getState());
          break;
        }
      } catch (IAgentException e) {
        // TODO handle exception
        e.printStackTrace();
      }
    }
  }
}
