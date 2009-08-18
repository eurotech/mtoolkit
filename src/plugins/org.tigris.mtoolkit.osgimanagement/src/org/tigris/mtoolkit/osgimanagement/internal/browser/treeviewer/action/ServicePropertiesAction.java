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
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteService;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ObjectClass;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.ServicePropertiesDialog;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.ServicePropertiesPage;

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
		if (service != null) {
			ServicePropertiesDialog dialog = new ServicePropertiesDialog(display.getActiveShell());
			dialog.open();
			try {
				ServicePropertiesPage mainControl = (ServicePropertiesPage) dialog.getMainControl();
				mainControl.setServiceName("Service " + service.getServiceId()); //$NON-NLS-1$
				mainControl.setData(service);
			} catch (IAgentException e) {
				BrowserErrorHandler.processError(e, true);
				e.printStackTrace();
			}
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