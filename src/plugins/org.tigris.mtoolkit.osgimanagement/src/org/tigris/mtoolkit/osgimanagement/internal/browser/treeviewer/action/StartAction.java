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
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public class StartAction extends SelectionProviderAction implements IStateAction {

	public StartAction(ISelectionProvider provider, String label) {
		super(provider, label);
	}

	public void run() {
		ISelection selection = getSelection();
		Iterator iterator = getStructuredSelection().iterator();
		while (iterator.hasNext()) {
			Bundle bundle = (Bundle) iterator.next();
			ActionsManager.startBundleAction(bundle);
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
			if (!(model instanceof Bundle)) {
				enabled = false;
				break;
			}
			Bundle bundle = (Bundle) model;
			try {
				if (bundle.getType() != 0
								|| (bundle.getState() & (org.osgi.framework.Bundle.ACTIVE
												| org.osgi.framework.Bundle.STOPPING | org.osgi.framework.Bundle.UNINSTALLED)) != 0
								|| bundle.getRemoteBundle().isSystemBundle()) {
					enabled = false;
					break;
				}
			} catch (IAgentException e) {
				enabled = false;
			}
		}
		this.setEnabled(enabled);
	}
}