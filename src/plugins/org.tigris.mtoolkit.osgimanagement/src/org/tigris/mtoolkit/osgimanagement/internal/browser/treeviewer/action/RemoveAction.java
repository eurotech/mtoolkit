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

public final class RemoveAction extends SelectionProviderAction implements IStateAction {
	public RemoveAction(ISelectionProvider provider, String label) {
		super(provider, label);
		this.setText(label + "@Delete");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		int count = getStructuredSelection().size();
		String msg = "Are you sure you want to remove ";
		if (count == 1) {
			msg +=  getStructuredSelection().getFirstElement() + "?";
		} else {
			msg += count + " selected frameworks?";
		}
		boolean confirm = MessageDialog.openQuestion(FrameWorkView.getShell(), "Remove Framework", msg);
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

	/* (non-Javadoc)
	 * @see org.tigris.mtoolkit.osgimanagement.IStateAction#updateState(org.eclipse.jface.viewers.IStructuredSelection)
	 */
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
	
	// override to react properly to selection change
	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.SelectionProviderAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	public void selectionChanged(IStructuredSelection selection) {
		updateState(selection);
	}
}
