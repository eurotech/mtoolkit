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
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;

public class PropertyAction extends SelectionProviderAction implements IStateAction {

	private TreeViewer parentView;

	public PropertyAction(ISelectionProvider provider, String label) {
		super(provider, label);
		this.parentView = (TreeViewer) provider;
		this.setText(label + "@Alt+Enter");
	}

	// run method
	public void run() {
		FrameWork framework = (FrameWork) getStructuredSelection().getFirstElement();
		MenuFactory.frameworkPropertiesAction(framework, parentView);
		getSelectionProvider().setSelection(getSelection());
	}

	// override to react properly to selection change
	public void selectionChanged(IStructuredSelection selection) {
		updateState(selection);
	}

	public void updateState(IStructuredSelection selection) {
		if (selection.size() == 1 && getStructuredSelection().getFirstElement() instanceof FrameWork) {
			this.setEnabled(true);
		} else {
			this.setEnabled(false);
		}
	}
}
