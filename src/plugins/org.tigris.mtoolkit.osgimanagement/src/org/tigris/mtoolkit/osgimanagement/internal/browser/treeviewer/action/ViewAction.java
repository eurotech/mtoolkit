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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Model;

public class ViewAction extends SelectionProviderAction {

	private String bundlesView = Messages.bundles_view_action_label;
	private String servicesView = Messages.services_view_action_label;
	private TreeViewer tree;
	private List frameworks;
	private int counter = 0;
	private int count = 0;

	public ViewAction(ISelectionProvider provider, String text, TreeViewer tree) {
		super(provider, text);
		this.tree = tree;
		frameworks = new ArrayList();
		this.setEnabled(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		FrameWorkView.removeFilter();
		ISelection selection = getSelection();
		tree.getTree().setRedraw(false);
		List tmp = new ArrayList();
		tmp.addAll(frameworks);
		count = tmp.size();
		counter = 0;
		int viewType = ((FrameWork) frameworks.get(0)).getViewType();

		for (int i = 0; i < frameworks.size(); i++) {
			setViewType((FrameWork) frameworks.get(i), viewType);
		}
		for (int i = 0; i < tmp.size(); i++)
			expandTree((FrameWork) tmp.get(i), selection);

		getSelectionProvider().setSelection(selection);
		FrameWorkView.addFilter();
	}

	private void setViewType(final FrameWork fw, int viewType) {
		Model parent = fw.getParent();
		parent.removeElement(fw);
		try {
			switch (viewType) {
			case FrameWork.BUNDLES_VIEW: {
				tree.collapseToLevel(fw, TreeViewer.ALL_LEVELS);
				fw.setViewType(FrameWork.SERVICES_VIEW);
				setText(bundlesView);
				FrameworkConnectorFactory.updateViewType(fw);
				break;
			}
			case FrameWork.SERVICES_VIEW: {
				tree.collapseToLevel(fw, TreeViewer.ALL_LEVELS);
				fw.setViewType(FrameWork.BUNDLES_VIEW);
				setText(servicesView);
				FrameworkConnectorFactory.updateViewType(fw);
				break;
			}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
		parent.addElement(fw);
		fw.updateElement();
	}

	private void expandTree(final FrameWork fw, final ISelection selection) {
		Display display = Display.getCurrent();
		if (display == null)
			display = Display.getDefault();
		display.asyncExec(new Runnable() {
			public void run() {
				tree.expandToLevel(fw, 1);
				tree.setSelection(selection, true);
				counter++;
				if (counter == count)
					tree.getTree().setRedraw(true);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.actions.SelectionProviderAction#selectionChanged(org.eclipse
	 * .jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		updateState(selection);
	}

	public void updateState(IStructuredSelection selection) {
		if (selection.size() == 0) {
			setEnabled(false);
			return;
		}

		this.frameworks.clear();
		Iterator iter = selection.iterator();
		while (iter.hasNext()) {
			Object element = iter.next();
			FrameWork fw = null;
			if (element instanceof FrameWork)
				fw = (FrameWork) element;
			else if (element instanceof Model)
				fw = ((Model) element).findFramework();

			if (fw != null && fw.isConnected() && !frameworks.contains(fw)) {
				frameworks.add(fw);
			}

		}

		if (this.frameworks.isEmpty()) {
			this.setEnabled(false);
			return;
		}
		this.setEnabled(true);

		for (int i = this.frameworks.size() - 1; i >= 0; i--) {
			FrameWork fw = (FrameWork) this.frameworks.get(i);
			switch (fw.getViewType()) {
			case FrameWork.BUNDLES_VIEW:
				setText(servicesView);
				break;
			case FrameWork.SERVICES_VIEW:
				setText(bundlesView);
				break;
			}
		}
	}
}