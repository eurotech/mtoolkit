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

import java.util.Iterator;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Model;

public class ShowBundleIDAction extends SelectionProviderAction {

	private TreeViewer tree;

	public ShowBundleIDAction(ISelectionProvider provider, String label, TreeViewer tree) {
		super(provider, label);
		this.tree = tree;
	}

	public void run() {
		boolean newState = !((Bundle) getStructuredSelection().getFirstElement()).isShowID();

		Iterator iterator = getStructuredSelection().iterator();
		while (iterator.hasNext()) {
			Bundle bundle = (Bundle) iterator.next();
			bundle.setShowID(newState);
		}
		tree.refresh();
	}

	public void selectionChanged(IStructuredSelection selection) {
		updateState(selection);
	}

	public void updateState(IStructuredSelection selection) {
		if (selection.size() == 0) {
			setEnabled(false);
			return;
		}
		boolean enabled = true;

		Iterator iterator = selection.iterator();
		while (iterator.hasNext()) {
			Model model = (Model) iterator.next();
			if (!(model instanceof Bundle)) {
				enabled = false;
				break;
			}
		}
		this.setEnabled(enabled);

		if (enabled && ((Bundle) selection.getFirstElement()).isShowID()) {
			setChecked(true);
		} else {
			setChecked(false);
		}
	}
}