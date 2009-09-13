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
package org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.logic;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.tigris.mtoolkit.osgimanagement.browser.model.Model;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ContentChangeEvent;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ContentChangeListener;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.TreeRoot;

public class ViewContentProvider implements ITreeContentProvider, ContentChangeListener, ConstantsDistributor {

	private TreeViewer viewer;
	private boolean canUpdate;
	private Display display;

	public ViewContentProvider() {
		canUpdate = true;
	}

	// Returns the child elements of the given parent element.
	public Object[] getChildren(Object parentElement) {
		return ((Model) parentElement).getChildren();
	}

	// Returns the parent for the given element, or null indicating that the
	// parent can't be computed.
	public Object getParent(Object element) {
		return ((Model) element).getParent();
	}

	// Returns whether the given element has children.
	public boolean hasChildren(Object element) {
		return ((Model) element).getSize() > 0;
	}

	// Returns the elements to display in the viewer when its input is set to
	// the given element.
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	// Disposes of this content provider.
	public void dispose() {
		canUpdate = false;
	}

	// Notifies this content provider that the given viewer's input has been
	// switched to a different element.
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = (TreeViewer) viewer;

		if (oldInput != null) {
			((TreeRoot) oldInput).removeListener(this);
		}

		if (newInput != null) {
			((TreeRoot) newInput).addListener(this);
		}
	}

	public void elementAdded(final ContentChangeEvent event) {
		if (canUpdate) {
			display = Display.getCurrent();
			if (display == null)
				display = Display.getDefault();
			display.asyncExec(new Runnable() {
				public void run() {
					viewer.add(event.getTarget().getParent(), event.getTarget());
				}
			});
		}
	}

	public void elementChanged(final ContentChangeEvent event) {
		if (canUpdate) {
			display = Display.getCurrent();
			if (display == null)
				display = Display.getDefault();
			display.syncExec(new Runnable() {
				public void run() {
					if (event.getTarget().getParent() != null) {
						viewer.refresh(event.getTarget().getParent());
					} else {
						viewer.refresh(event.getTarget());
					}
				}
			});
		}
	}

	public void elementRemoved(ContentChangeEvent event) {
		if (canUpdate) {
			final Model target = event.getTarget();
			display = Display.getCurrent();
			if (display == null)
				display = Display.getDefault();
			display.syncExec(new Runnable() {
				public void run() {
					viewer.remove(target);
				}
			});
		}
	}
}