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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteApplication;
import org.tigris.mtoolkit.osgimanagement.ContentTypeActionsProvider;
import org.tigris.mtoolkit.osgimanagement.ToolbarIMenuCreator;
import org.tigris.mtoolkit.osgimanagement.application.actions.ApplicationPropertiesAction;
import org.tigris.mtoolkit.osgimanagement.application.actions.StartApplicationAction;
import org.tigris.mtoolkit.osgimanagement.application.actions.StopApplicationAction;
import org.tigris.mtoolkit.osgimanagement.application.images.ImageHolder;
import org.tigris.mtoolkit.osgimanagement.application.model.Application;
import org.tigris.mtoolkit.osgimanagement.application.model.ApplicationPackage;
import org.tigris.mtoolkit.osgimanagement.model.Model;


public class ApplicationActionsProvider implements ContentTypeActionsProvider {

	private static final String START_APPLICATION_IMAGE_PATH = "start_application_action.gif";
	private static final String STOP_APPLICATION_IMAGE_PATH = "stop_application_action.gif";
	private static final String APPLICATION_GROUP_IMAGE_PATH = "application_group.gif";
	private static final String APPLICATION_PROPERTIES_IMAGE_PATH = "application_properties.gif";

	public static final String APPLICATION_STARTED_ICON_PATH = "started_application_state.gif";
	public static final String APPLICATION_STOPPING_ICON_PATH = "stopping_application_state.gif";
	public static final String APPLICATION_INSTALLED_ICON_PATH = "installed_application_state.gif";
	public static final String APPLICATION_MIXED_ICON_PATH = "mixed_application_state.gif";
	
	public static final String APPLICATION_ICON_PATH = "application.gif";
	public static final String APPLICATION_PACKAGE_ICON_PATH = "application_package.gif";

	private StartApplicationAction startApplicationAction;
	private StopApplicationAction stopApplicationAction;
	private ApplicationPropertiesAction applicationPropertiesAction;
	
	private TreeViewer tree;
	private ToolbarIMenuCreator applicationTB;
	private Hashtable commonActions;
	
	
	public void init(TreeViewer tree) {
		this.tree = tree;
		startApplicationAction = new StartApplicationAction(tree, "Start Application");
		startApplicationAction.setImageDescriptor(ImageHolder.getImageDescriptor(START_APPLICATION_IMAGE_PATH));
		stopApplicationAction = new StopApplicationAction(tree, "Stop Application");
		stopApplicationAction.setImageDescriptor(ImageHolder.getImageDescriptor(STOP_APPLICATION_IMAGE_PATH));
		applicationPropertiesAction = new ApplicationPropertiesAction(tree, "Properties");
		applicationPropertiesAction.setImageDescriptor(ImageHolder.getImageDescriptor(APPLICATION_PROPERTIES_IMAGE_PATH));
		commonActions = new Hashtable();
		commonActions.put(PROPERTIES_ACTION, applicationPropertiesAction);
	}

	@Override
	public Map getCommonActions() {
		return commonActions;
	}

	public void fillToolBar(ToolBarManager tbm) {
		Action[] actions = new Action[] { startApplicationAction, stopApplicationAction, applicationPropertiesAction};
		applicationTB = new ToolbarIMenuCreator(actions, tree);
		applicationTB.setImageDescriptor(ImageHolder.getImageDescriptor(APPLICATION_GROUP_IMAGE_PATH));
		applicationTB.setToolTipText("Various application actions");
		tbm.appendToGroup(ContentTypeActionsProvider.GROUP_DEPLOYMENT, applicationTB);
	}

	public void menuAboutToShow(StructuredSelection selection, IMenuManager manager) {
		boolean homogen = true;
		if (selection.size() > 0) {
			Model element = (Model) selection.getFirstElement();
			Class clazz = element.getClass();

			Iterator iterator = selection.iterator();
			while (iterator.hasNext()) {
				Object sel = iterator.next();
				if (!clazz.equals(sel.getClass())) {
					homogen = false;
					break;
				}
			}

			if (homogen) {
				if (element instanceof Application) {
					manager.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, startApplicationAction);
					manager.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, stopApplicationAction);
					manager.appendToGroup(ContentTypeActionsProvider.GROUP_PROPERTIES, applicationPropertiesAction);
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
				} else if (RemoteApplication.STATE_STARTING.equals(state)) {
					return ImageHolder.getImage(APPLICATION_STARTED_ICON_PATH);
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

	@Override
	public void updateEnabledState(DeviceConnector connector) {
		boolean enabled = ApplicationModelProvider.isApplicationsSupported(connector);
		if (enabled) {
			applicationPropertiesAction.updateState((IStructuredSelection) tree.getSelection());
			enabled = applicationPropertiesAction.isEnabled();
		}
		applicationTB.setEnabled(enabled);
	}

}
