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
import java.util.List;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.model.Framework;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public final class ShowServicePropertiesInTree extends SelectionProviderAction implements IStateAction {
	private final List frameworks = new ArrayList();

	public ShowServicePropertiesInTree(ISelectionProvider provider, String label) {
		super(provider, label);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		boolean newState = !((FrameworkImpl) frameworks.get(0)).isShownServicePropertiss();

		for (int i = 0; i < frameworks.size(); i++) {
			FrameworkImpl framework = (FrameworkImpl) frameworks.get(i);
			framework.setShowServicePropertiesInTree(newState);

			if (newState) {
				framework.refreshAction();
			} else {
				framework.clearServicePropertiesNodes((Model) frameworks.get(i));
			}
		}

		if (!newState) {
			((TreeViewer) getSelectionProvider()).refresh();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.SelectionProviderAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	public void selectionChanged(IStructuredSelection selection) {
		updateState(selection);
	}

	/* (non-Javadoc)
	 * @see org.tigris.mtoolkit.osgimanagement.IStateAction#updateState(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void updateState(IStructuredSelection selection) {
		TreeItem[] items = ((TreeViewer) getSelectionProvider()).getTree().getItems();
		boolean srvcPropertiesShown = (frameworks.isEmpty()) ? false : ((FrameworkImpl) frameworks.get(0))
				.isShownServicePropertiss();

		// tree structure has been updated (nodes added/removed )
		if (items.length != frameworks.size()) {
			frameworks.clear();

			for (int i = 0; i < items.length; i++) {
				Object o = items[i].getData();

				if (o instanceof FrameworkImpl) {
					frameworks.add(o);
					((FrameworkImpl) o).setShowServicePropertiesInTree(srvcPropertiesShown);
				}
			}
		}
		// all nodes of the tree have been removed
		if (frameworks.isEmpty()) {
			setEnabled(false);
			setChecked(false);
		} else {
			Framework fw = (Framework) frameworks.get(0);
			setEnabled(fw.getViewType() == Framework.BUNDLES_VIEW);
			setChecked(srvcPropertiesShown);
		}
	}
}
