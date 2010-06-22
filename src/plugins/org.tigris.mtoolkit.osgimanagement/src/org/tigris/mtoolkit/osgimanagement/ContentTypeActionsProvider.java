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
package org.tigris.mtoolkit.osgimanagement;

import java.util.Map;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public interface ContentTypeActionsProvider {
	
	// common groups for toolbar and context menu 
	public static final String GROUP_UNSIGNED = "unsigned";
	public static final String GROUP_ACTIONS = "actions";
	
	// context menu groups
	public static final String GROUP_INSTALL = "install";
	public static final String GROUP_OPTIONS = "options";
	public static final String GROUP_DEFAULT = "default";
	public static final String GROUP_PROPERTIES = "properties";

	// toolbar groups
	public static final String GROUP_CONNECT = "connect";
	public static final String GROUP_DEPLOYMENT = "deployment";
	public static final String GROUP_FRAMEWORK = "framework";

	/**
	 * @since 6.0
	 */
	public static final String PROPERTIES_ACTION = "PROPERTIES";
	
	
	public void init(TreeViewer tree);
	
	public void menuAboutToShow(StructuredSelection selection, IMenuManager manager);
	
	public void fillToolBar(ToolBarManager tbm);
	
	/**
	 * @since 6.0
	 */
	public Map getCommonActions();

	/**
	 * @since 5.0
	 */
	public Image getImage(Model node);

	/**
	 * @since 6.0
	 */
	public void updateEnabledState(DeviceConnector connector);
}
