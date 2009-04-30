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
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;

public class UpdateBundleAction extends SelectionProviderAction {

	private TreeViewer parentView;

	public UpdateBundleAction(ISelectionProvider provider, String label) {
		super(provider, label);
		this.parentView = (TreeViewer) provider;
	}

	public void run() {
		IStructuredSelection selection = getStructuredSelection();
		Bundle bundle = (Bundle) selection.getFirstElement();
		MenuFactory.updateBundleAction(bundle, parentView);
		getSelectionProvider().setSelection(selection);
	}

	// override to react properly to selection change
	public void selectionChanged(IStructuredSelection selection) {
		updateState(selection);
	}

	public void updateState(IStructuredSelection selection) {
		try {
			if (selection.size() == 1
							&& (selection.getFirstElement() instanceof Bundle)
							&&
							/*
							 * ((Bundle)selection.getFirstElement()).getType()
							 * == 0 &&
							 */
							!((((Bundle) selection.getFirstElement()).getState() & (org.osgi.framework.Bundle.UNINSTALLED | org.osgi.framework.Bundle.STOPPING)) != 0)
							&& !((Bundle) selection.getFirstElement()).getRemoteBundle().isSystemBundle()) {
				setEnabled(true);
			} else {
				setEnabled(false);
			}
		} catch (IAgentException e) {
			setEnabled(false);
		}
	}
}