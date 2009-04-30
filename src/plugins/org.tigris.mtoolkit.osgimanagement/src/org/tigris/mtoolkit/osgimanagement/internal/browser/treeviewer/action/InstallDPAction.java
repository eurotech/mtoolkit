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
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Model;

public class InstallDPAction extends SelectionProviderAction {

	private TreeViewer parentView;

	public InstallDPAction(ISelectionProvider provider, String label) {
		super(provider, label);
		this.parentView = (TreeViewer) provider;
	}

	// run method
	public void run() {
		Model node = (Model) getStructuredSelection().getFirstElement();
		FrameWork framework = node.findFramework();
		MenuFactory.installDPAction(framework, parentView);
		getSelectionProvider().setSelection(getSelection());
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
		FrameWork fw = ((Model) selection.getFirstElement()).findFramework();
		boolean enabled = true;
		if (fw == null || !fw.isConnected()) {
			enabled = false;
		} else {
			Iterator iterator = selection.iterator();
			while (iterator.hasNext()) {
				Model model = (Model) iterator.next();
				if (model.findFramework() != fw) {
					enabled = false;
					break;
				}
			}
		}
		setEnabled(enabled);
	}
}