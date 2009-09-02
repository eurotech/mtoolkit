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

public class ShowBundleVersionAction extends SelectionProviderAction implements IStateAction {

	private TreeViewer tree;
	private List frameworks = new ArrayList();

	public ShowBundleVersionAction(ISelectionProvider provider, String label, TreeViewer tree) {
		super(provider, label);
		this.tree = tree;
	}

	public void run() {
		boolean newState = !((Bundle) ((FrameWork) frameworks.get(0)).bundleHash.values().iterator().next()).isShowVersion();

		for (int i = 0; i < frameworks.size(); i++) {
			FrameWork fw = (FrameWork) frameworks.get(i);
			((Bundle) fw.bundleHash.values().iterator().next()).setShowVersion(newState);
		}
		tree.refresh();
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

		Iterator iter = selection.iterator();
		frameworks.clear();

		while (iter.hasNext()) {
			Object element = iter.next();
			FrameWork fw = null;
			if (element instanceof FrameWork)
				fw = (FrameWork) element;
			if (element instanceof Model)
				fw = ((Model) element).findFramework();
			if (fw != null && fw.isConnected() && !frameworks.contains(fw)) {
				frameworks.add(fw);
			}
		}

		if (frameworks.isEmpty()) {
			this.setEnabled(false);
			return;
		}
		this.setEnabled(true);

		if (((FrameWork) frameworks.get(0)).bundleHash != null) {
			Bundle bundle = (Bundle) ((FrameWork) frameworks.get(0)).bundleHash.values().iterator().next();
			boolean versionShown = bundle.isShowVersion();
			if (versionShown)
				setChecked(true);
			else
				setChecked(false);
		}
	}
}