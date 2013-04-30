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
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworksView;

public final class AddAction extends SelectionProviderAction implements IStateAction {

	public AddAction(ISelectionProvider provider, String label) {
		super(provider, label);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		ActionsManager.addFrameworkAction(FrameworksView.getTreeRoot());
		getSelectionProvider().setSelection(getSelection());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.SelectionProviderAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(true);
	}

	/* (non-Javadoc)
	 * @see org.tigris.mtoolkit.osgimanagement.IStateAction#updateState(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void updateState(IStructuredSelection selection) {
		setEnabled(true);
	}
}