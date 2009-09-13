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
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.TreeRoot;

public class ShowBundleIDAction extends SelectionProviderAction implements IStateAction {

	private TreeViewer tree;
	private TreeRoot root;

	public ShowBundleIDAction(ISelectionProvider provider, String label, TreeViewer tree, TreeRoot root) {
		super(provider, label);
		this.tree = tree;
		this.root = root;
		setChecked(root.isShowBundlesID());
	}

	public void run() {
		root.setShowBundlesID(!root.isShowBundlesID());
		setChecked(root.isShowBundlesID());
		tree.refresh();
	}

	public void selectionChanged(IStructuredSelection selection) {
		updateState(selection);
	}

	public void updateState(IStructuredSelection selection) {
		// nothing to do
	}
}