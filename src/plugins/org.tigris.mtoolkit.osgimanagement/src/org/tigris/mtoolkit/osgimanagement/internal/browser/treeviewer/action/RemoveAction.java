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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public class RemoveAction extends SelectionProviderAction implements IStateAction {

	public RemoveAction(ISelectionProvider provider, String label) {
		super(provider, label);
	}

	// run method
	public void run() {
		boolean confirm = MessageDialog.openQuestion(FrameWorkView.getShell(), "Remove Framework", "Remove selected framework(s)?");
		if (confirm) {
			Iterator iterator = getStructuredSelection().iterator();
			while (iterator.hasNext()) {
				FrameworkImpl framework = (FrameworkImpl) iterator.next();
				if (framework.isConnected()) {
					ActionsManager.disconnectFrameworkAction(framework);
				}
				ActionsManager.removeFrameworkAction(framework);
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
			if (!(model instanceof FrameworkImpl)) {
				enabled = false;
				break;
			}
			FrameworkImpl framework = (FrameworkImpl) model;
			if (framework.autoConnected) {
				enabled = false;
				break;
			}
		}
		this.setEnabled(enabled);
	}
}
