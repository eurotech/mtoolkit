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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.browser.model.Model;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;

public class ViewAction extends Action implements IStateAction, ISelectionChangedListener {

	private TreeViewer tree;
	private int viewType;
	private ISelectionProvider provider;

	public ViewAction(ISelectionProvider provider, String text, TreeViewer tree, int viewType) {
		super(text, AS_RADIO_BUTTON);
		this.tree = tree;
		this.viewType = viewType;
		setEnabled(false);
		setChecked(false);
		this.provider = provider;
		provider.addSelectionChangedListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		if (!isChecked())
			return;
		IStructuredSelection selection = getSelection();
		if (selection.isEmpty()) {
			return;
		}
		tree.getTree().setRedraw(false);
		try {
			Set frameworks = new HashSet();
			for (Iterator it = selection.iterator(); it.hasNext();) {
				Object next = it.next();
				if (next instanceof Model) {
					frameworks.add(((Model) next).findFramework());
				}
			}
			for (Iterator it = frameworks.iterator(); it.hasNext();) {
				FrameWork fw = (FrameWork) it.next();
				setViewType(fw, viewType);
			}
			for (Iterator it = frameworks.iterator(); it.hasNext();) {
				FrameWork fw = (FrameWork) it.next();
				tree.expandToLevel(fw, 1);
			}
		} finally {
			tree.getTree().setRedraw(true);
		}
		provider.setSelection(selection);
	}

	private IStructuredSelection getSelection() {
		ISelection selection = provider.getSelection();
		if (selection instanceof IStructuredSelection)
			return (IStructuredSelection) selection;
		else
			return new StructuredSelection();
	}
	
	private void setViewType(final FrameWork fw, int newViewType) {
		if (fw.getViewType() == newViewType)
			return;
 		if (fw.getViewType() == FrameWork.BUNDLES_VIEW) {
 			fw.getBundlesNode().removeChildren();
 			fw.getDPNode().removeChildren();
 		}
 		fw.removeChildren();
		try {
			tree.collapseToLevel(fw, TreeViewer.ALL_LEVELS);
			fw.setViewType(newViewType);
			FrameworkConnectorFactory.updateViewType(fw);
		} catch (Throwable t) {
			FrameworkPlugin.error("Exception while switching framework view type", t);
		}
		fw.updateElement();
	}

	public void updateState(IStructuredSelection selection) {
		if (selection.size() == 0) {
			setEnabled(false);
			setChecked(false);
			return;
		}

		Set frameworks = new HashSet();
		for (Iterator it = selection.iterator(); it.hasNext();) {
			Object next = (Object) it.next();
			if (next instanceof Model) {
				FrameWork fw = ((Model) next).findFramework();
				if (fw != null && fw.isConnected())
					frameworks.add(fw);
			}
		}
		if (frameworks.isEmpty()) {
			setEnabled(false);
			setChecked(false);
			return;
		}
		
		Iterator it = frameworks.iterator();
		FrameWork fw = (FrameWork) it.next();
		boolean checked = fw.getViewType() == viewType;
		for (;it.hasNext();) {
			fw = (FrameWork) it.next();
			boolean nextChecked = fw.getViewType() == viewType;
			if (nextChecked != checked) {
				checked = true;
				break;
			}
		}
		setEnabled(true);
		setChecked(checked);
	}

	public void selectionChanged(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		if (selection instanceof IStructuredSelection) {
			updateState((IStructuredSelection) selection);
		} else {
			setEnabled(false);
			setChecked(false);
		}
	}
}