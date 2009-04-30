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
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Model;

public class RefreshAction extends SelectionProviderAction {

	private String label1;
	private String label2;

	public RefreshAction(ISelectionProvider provider, String label1, String label2) {
		super(provider, label1);
		this.label1 = label1;
		this.label2 = label2;
	}

	// run method
	public void run() {
		Iterator iterator = getStructuredSelection().iterator();
		while (iterator.hasNext()) {
			Model node = (Model) iterator.next();
			if (node instanceof FrameWork) {
				FrameWork framework = (FrameWork) node;
				MenuFactory.refreshFrameworkAction(framework);
			} else if (node instanceof Bundle) {
				Bundle bundle = (Bundle) node;
				MenuFactory.refreshBundleAction(bundle);
			}
		}
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
		boolean enabled = true;

		Iterator iterator = selection.iterator();
		while (iterator.hasNext()) {
			Model model = (Model) iterator.next();
			if (!(model instanceof FrameWork || model instanceof Bundle)) {
				enabled = false;
				break;
			}
			if (model instanceof FrameWork) {
				FrameWork framework = (FrameWork) model;
				if (!framework.isConnected()) {
					enabled = false;
					break;
				}
			}
		}
		if (enabled) {
			if (selection.getFirstElement() instanceof FrameWork) {
				setText(label1);
			} else {
				setText(label2);
			}
		}
		this.setEnabled(enabled);
	}
}
