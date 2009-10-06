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
package org.tigris.mtoolkit.osgimanagement.application.actions;

import java.util.Map;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.application.model.Application;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.PropertiesDialog;


public class ApplicationPropertiesAction extends SelectionProviderAction implements IStateAction {

	private TreeViewer parentView;

	public ApplicationPropertiesAction(ISelectionProvider provider, String label) {
		super(provider, label);
		this.parentView = (TreeViewer) provider;
		this.setText(label + "@Alt+Enter");
	}

	// run method
	public void run() {
		Application application = (Application)getStructuredSelection().getFirstElement();

		try {
			Map headers = application.getRemoteApplication().getProperties();
			
			Shell shell = parentView.getTree().getShell();
			PropertiesDialog propertiesDialog = new PropertiesDialog(shell, false);

			propertiesDialog.open();
			propertiesDialog.getMainControl().setData(headers);

			// needed to update workbench menu and toolbar status
			getSelectionProvider().setSelection(getSelection());
		} catch (IAgentException e) {
			e.printStackTrace();
		}

	}

	// override to react properly to selection change
	public void selectionChanged(IStructuredSelection selection) {
		updateState(selection);
	}

	public void updateState(IStructuredSelection selection) {
		if (selection.size() == 1 && getStructuredSelection().getFirstElement() instanceof Application) {
			this.setEnabled(true);
		} else {
			this.setEnabled(false);
		}
	}

}