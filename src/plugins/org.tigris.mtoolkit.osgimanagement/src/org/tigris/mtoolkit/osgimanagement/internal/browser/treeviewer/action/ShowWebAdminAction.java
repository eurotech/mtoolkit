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
package org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action;

import java.util.Dictionary;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.internal.Workbench;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.rpc.Capabilities;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.internal.editor.WebAdminInput;
import org.tigris.mtoolkit.osgimanagement.internal.editor.WebAdminEditor;
import org.tigris.mtoolkit.osgimanagement.model.Framework;
import org.tigris.mtoolkit.osgimanagement.model.Model;

import com.prosyst.tools.iagent.ext.config.ConfigManager;

public class ShowWebAdminAction extends SelectionProviderAction implements IStateAction {

	public ShowWebAdminAction(ISelectionProvider provider, String label) {
		super(provider, label);
	}

	public void run() {
		//		setEnabled(false);
		IStructuredSelection selection = (IStructuredSelection) getSelection();
		Model model = (Model) selection.getFirstElement();
		FrameworkImpl framework = (FrameworkImpl) model.findFramework();
		IEditorDescriptor editorDescr = Workbench.getInstance().getEditorRegistry().findEditor("org.tigris.mtoolkit.osgimanagement.webadmin");
		try {
			Object mgr = framework.getConnector().getManager("com.prosyst.tools.iagent.ext.config.ConfigManager");
			Dictionary result = ((com.prosyst.tools.iagent.ext.config.ConfigManager)mgr).getConfigurationProperties("mbs.http.pid");
			String suffix = (String) result.get("manager.root");
			if (suffix == null) {
				suffix = "/system/console";
			}

			IEditorPart[] editors = Workbench.getInstance().getActiveWorkbenchWindow().getActivePage().getEditors();
			IEditorPart part = null;
			String id = (String) framework.getConnector().getProperties().get(Framework.FRAMEWORK_ID);
			String url = "http://"+id+suffix;
			for (int i=0; i<editors.length; i++) {
				if (editors[i] instanceof WebAdminEditor) {
					part = editors[i];
					Workbench.getInstance().getActiveWorkbenchWindow().getActivePage().activate(part);
					((WebAdminInput)part.getEditorInput()).setURL(url);
					((WebAdminEditor)part).refresh();
					break;
				}
			}
			if (part == null) {
				part = Workbench.getInstance().getActiveWorkbenchWindow().getActivePage().openEditor(new WebAdminInput(url), "org.tigris.mtoolkit.osgimanagement.webadmin", true);
			}
		} catch (IAgentException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
		getSelectionProvider().setSelection(selection);
	}

	// override to react properly to selection change
	public void selectionChanged(IStructuredSelection selection) {
		updateState(selection);
	}

	public void updateState(IStructuredSelection selection) {
		if (selection.size() == 0) {
			setEnabled(false);
			return;
		}
		boolean enabled = false;

		Model model = (Model) selection.getFirstElement();
		FrameworkImpl framework = (FrameworkImpl) model.findFramework();
		if (isWebAdminSupported(framework.getConnector())) {
			enabled = true;
		}
		this.setEnabled(enabled);
	}
	
	public static boolean isWebAdminSupported(DeviceConnector connector) {
		if (connector == null) {
			return false;
		}
		Dictionary connectorProperties = connector.getProperties();
		Object capabilitiesSupport = connectorProperties.get(Capabilities.CAPABILITIES_SUPPORT);
		if (capabilitiesSupport == null || !Boolean.valueOf(capabilitiesSupport.toString()).booleanValue()) {
			return true;
		} else {
			Object support = connectorProperties.get(ConfigManager.WEB_ADMIN_SUPPORT);
			if (support != null && Boolean.valueOf(support.toString()).booleanValue()) {
				return true;
			}
		}
		return false;
	}

}
