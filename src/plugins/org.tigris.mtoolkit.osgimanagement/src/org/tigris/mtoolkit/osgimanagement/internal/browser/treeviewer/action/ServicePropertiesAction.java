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

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteService;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.internal.IHelpContextIds;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ObjectClass;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.PropertiesDialog;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.PropertiesPage;

public class ServicePropertiesAction extends SelectionProviderAction implements IStateAction {

	/**
	 * @param provider
	 * @param text
	 */
	public ServicePropertiesAction(ISelectionProvider provider, String text) {
		super(provider, text);
		this.setText(text + "@Alt+Enter");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		Display display = Display.getCurrent();
		RemoteService service = null;
		ObjectClass object = (ObjectClass) getStructuredSelection().getFirstElement();
		service = object.getService();
		String tableHeader = null;
		try {
			tableHeader = "Service " + service.getServiceId();
		} catch (IAgentException e1) {
		}		
		if (service != null) {
			PropertiesDialog dialog = new PropertiesDialog(display.getActiveShell(), Messages.service_properties_title, tableHeader) {
				protected void attachHelp(Composite container) {
					PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.PROPERTY_SERVICE);
				}
				
			};
			try {
				dialog.create();
				PropertiesPage mainControl = (PropertiesPage) dialog.getMainControl();
				mainControl.setData(service.getProperties());
			} catch (IAgentException e) {
				BrowserErrorHandler.processError(e, true);
				e.printStackTrace();
			}
			dialog.open();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.actions.SelectionProviderAction#selectionChanged(org.eclipse
	 * .jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		updateState(selection);
	}

	public void updateState(IStructuredSelection selection) {
		if (selection.size() == 1 && getStructuredSelection().getFirstElement() instanceof ObjectClass) {
			this.setEnabled(true);
		} else {
			this.setEnabled(false);
		}
	}
}