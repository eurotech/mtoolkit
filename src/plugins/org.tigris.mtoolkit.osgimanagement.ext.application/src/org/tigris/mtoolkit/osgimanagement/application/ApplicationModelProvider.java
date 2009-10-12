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

import org.eclipse.swt.graphics.Image;
import org.tigris.mtoolkit.iagent.ApplicationManager;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteApplication;
import org.tigris.mtoolkit.iagent.event.RemoteApplicationEvent;
import org.tigris.mtoolkit.iagent.event.RemoteApplicationListener;
import org.tigris.mtoolkit.osgimanagement.ContentTypeModelProvider;
import org.tigris.mtoolkit.osgimanagement.application.images.ImageHolder;
import org.tigris.mtoolkit.osgimanagement.application.model.Application;
import org.tigris.mtoolkit.osgimanagement.application.model.ApplicationPackage;
import org.tigris.mtoolkit.osgimanagement.browser.model.Model;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;


public class ApplicationModelProvider implements ContentTypeModelProvider, RemoteApplicationListener {

	private ApplicationPackage applicationsNode;
	private DeviceConnector connector;
	private Model parent;
	private ApplicationManager manager;

	public static final String APPLICATION_STARTED_ICON_PATH = "started_application_state.gif";
	public static final String APPLICATION_STOPPING_ICON_PATH = "stopping_application_state.gif";
	public static final String APPLICATION_INSTALLED_ICON_PATH = "installed_application_state.gif";
	public static final String APPLICATION_MIXED_ICON_PATH = "mixed_application_state.gif";
	
	public static final String APPLICATION_ICON_PATH = "application.gif";
	public static final String APPLICATION_PACKAGE_ICON_PATH = "application_package.gif";

	public Model connect(Model parent, DeviceConnector connector) {
		applicationsNode = new ApplicationPackage("Application");
		this.connector = connector;
		this.parent = parent;
		if (parent.findFramework().getViewType() == FrameWork.BUNDLES_VIEW) { 
			parent.addElement(applicationsNode);
		}
		try {
			manager = (ApplicationManager) connector.getManager(ApplicationManager.class.getName());
			addApplications();
			try {
				manager.addRemoteApplicationListener(this);
			} catch (IAgentException e) {
				e.printStackTrace();
			}
		} catch (IAgentException e) {
			e.printStackTrace();
		}

		return applicationsNode;
	}

	private void addApplications() {
		try {
			RemoteApplication[] applications = manager.listApplications();
			for (int i=0; i<applications.length; i++) {
				Application application = new Application(applications[i].getApplicationId(), applications[i]);
				applicationsNode.addElement(application);
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

	public void disconnect() {
		if (manager != null) {
			try {
				manager.removeRemoteApplicationListener(this);
			} catch (IAgentException e) {
				e.printStackTrace();
			}
		}
		if (parent != null) {
			parent.removeElement(applicationsNode);
			parent = null;
		}
		connector = null;
		applicationsNode = null;
	}

	public Model switchView(int viewType) {
		Model node = null;
		if (viewType == FrameWork.BUNDLES_VIEW) {
			parent.addElement(applicationsNode);
			node = applicationsNode;
		} else if (viewType == FrameWork.SERVICES_VIEW) {
			parent.removeElement(applicationsNode);
		}
		return node;
	}

	public void applicationChanged(RemoteApplicationEvent event) {
		if (event.getType() == RemoteApplicationEvent.INSTALLED) {
			RemoteApplication rApplication = event.getApplication();
			Application application;
			try {
				application = new Application(rApplication.getApplicationId(), rApplication);
				applicationsNode.addElement(application);
			} catch (IAgentException e) {
				e.printStackTrace();
			}

		} else if (event.getType() == RemoteApplicationEvent.UNINSTALLED) {
			Model applications[] = applicationsNode.getChildren();
			for (int i=0; i<applications.length; i++) {
				try {
					if (((Application)applications[i]).getRemoteApplication().getApplicationId().equals(event.getApplication().getApplicationId())) {
						applicationsNode.removeElement(applications[i]);
						break;
					}
				} catch (IAgentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public Image getImage(Model node) {
		if (node instanceof Application) {
			try {
				String state = ((Application)node).getRemoteApplication().getState();
				if (RemoteApplication.STATE_RUNNING.equals(state)) {
					return ImageHolder.getImage(APPLICATION_STARTED_ICON_PATH);
				} else if (RemoteApplication.STATE_INSTALLED.equals(state)) {
					return ImageHolder.getImage(APPLICATION_INSTALLED_ICON_PATH);
				} else if (RemoteApplication.STATE_MIXED.equals(state)) {
					return ImageHolder.getImage(APPLICATION_MIXED_ICON_PATH);
				} else if (RemoteApplication.STATE_STOPPING.equals(state)) {
					return ImageHolder.getImage(APPLICATION_STOPPING_ICON_PATH);
				}
			} catch (IAgentException e) {
				e.printStackTrace();
				return ImageHolder.getImage(APPLICATION_INSTALLED_ICON_PATH);
			}
		}
		if (node instanceof ApplicationPackage) {
			return ImageHolder.getImage(APPLICATION_PACKAGE_ICON_PATH);
		}
		return null;
	}

}
