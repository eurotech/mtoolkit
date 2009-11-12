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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.browser.model.Model;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConnectFrameworkJob;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;

public class ConnectAction extends SelectionProviderAction implements IStateAction {

	public ConnectAction(ISelectionProvider provider, String label) {
		super(provider, label);
	}

	public void run() {
		setEnabled(false);
		ISelection selection = getSelection();
		Iterator iterator = getStructuredSelection().iterator();
		while (iterator.hasNext()) {
			FrameworkImpl framework = (FrameworkImpl) iterator.next();
			ActionsManager.connectFrameworkAction(framework);
			// needed to update workbench menu and toolbar status
		}
		getSelectionProvider().setSelection(selection);
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
			if (framework.isConnected() || framework.isConnecting() || ConnectFrameworkJob.isConnecting(framework)) {
				enabled = false;
				break;
			}
		}
		this.setEnabled(enabled);
	}
}
