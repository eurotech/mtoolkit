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
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;

public class BundlePropertiesAction extends SelectionProviderAction implements IStateAction {

	private TreeViewer parentView;

	public BundlePropertiesAction(ISelectionProvider provider, String label) {
		super(provider, label);
		this.parentView = (TreeViewer) provider;
		this.setText(label + "@Alt+Enter");
	}

	// run method
	public void run() {
		Bundle bundle = (Bundle) getStructuredSelection().getFirstElement();
		MenuFactory.bundlePropertiesAction(bundle, parentView);
		// needed to update workbench menu and toolbar status
		getSelectionProvider().setSelection(getSelection());
	}

	// override to react properly to selection change
	public void selectionChanged(IStructuredSelection selection) {
		updateState(selection);
	}

	public void updateState(IStructuredSelection selection) {
		if (selection.size() == 1
						&& getStructuredSelection().getFirstElement() instanceof Bundle
						&& (((Bundle) getStructuredSelection().getFirstElement()).getState() & (org.osgi.framework.Bundle.INSTALLED
										| org.osgi.framework.Bundle.RESOLVED
										| org.osgi.framework.Bundle.STARTING
										| org.osgi.framework.Bundle.ACTIVE | org.osgi.framework.Bundle.STOPPING)) != 0) {
			this.setEnabled(true);
		} else {
			this.setEnabled(false);
		}
	}
}