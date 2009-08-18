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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.browser.model.Model;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;

public class ShowBundleIDAction extends SelectionProviderAction implements IStateAction {

	private TreeViewer tree;
	private List frameworks;

	public ShowBundleIDAction(ISelectionProvider provider, String label, TreeViewer tree) {
		super(provider, label);
		this.tree = tree;
		frameworks = new ArrayList();
	}

	public void run() {
		Bundle firstBundle = (Bundle) ((FrameWork) frameworks.get(0)).bundleHash.values().iterator().next();
		boolean newState = !firstBundle.isShowID();
		Iterator iter = frameworks.iterator();
		while (iter.hasNext()) {
			Bundle bundle = (Bundle) ((FrameWork) iter.next()).bundleHash.values().iterator().next();
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
		frameworks.clear();
		Iterator iterator = selection.iterator();
		while (iterator.hasNext()) {
			Model model = (Model) iterator.next();
			FrameWork fw = null;
			if (model instanceof FrameWork) {
				fw = (FrameWork) model;
			} else {
				fw = model.findFramework();
			}
			if (fw != null && fw.isConnected() && !frameworks.contains(fw))
				frameworks.add(fw);
		}

		if (frameworks.isEmpty()) {
			setEnabled(false);
			return;
		}
		setEnabled(true);
		Collection bundles = ((FrameWork) frameworks.get(0)).bundleHash.values();
		Bundle firstBundle = (Bundle) bundles.iterator().next();
		if (firstBundle.isShowID())
			setChecked(true);
		else
			setChecked(false);
	}
}