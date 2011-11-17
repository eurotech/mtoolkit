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
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.console.ConsoleManager;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ContentChangeEvent;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ContentChangeListener;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.TreeRoot;
import org.tigris.mtoolkit.osgimanagement.model.Framework;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public final class ViewContentProvider implements ITreeContentProvider, ContentChangeListener, ConstantsDistributor {
	private TreeViewer viewer;

	// Returns the child elements of the given parent element.
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.
	 * Object)
	 */
	public Object[] getChildren(Object parentElement) {
		return ((Model) parentElement).getChildren();
	}

	// Returns the parent for the given element, or null indicating that the
	// parent can't be computed.
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object
	 * )
	 */
	public Object getParent(Object element) {
		return ((Model) element).getParent();
	}

	// Returns whether the given element has children.
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.
	 * Object)
	 */
	public boolean hasChildren(Object element) {
		return ((Model) element).getSize() > 0;
	}

	// Returns the elements to display in the viewer when its input is set to
	// the given element.
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.
	 * Object)
	 */
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	// Disposes of this content provider.
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
	}

	// Notifies this content provider that the given viewer's input has been
	// switched to a different element.
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface
	 * .viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = (TreeViewer) viewer;

		if (oldInput != null) {
			((TreeRoot) oldInput).removeListener(this);
		}

		if (newInput != null) {
			((TreeRoot) newInput).addListener(this);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.tigris.mtoolkit.osgimanagement.internal.browser.logic.
	 * ContentChangeListener
	 * #elementAdded(org.tigris.mtoolkit.osgimanagement.internal
	 * .browser.logic.ContentChangeEvent)
	 */
	public void elementAdded(final ContentChangeEvent event) {
		if (!canUpdate()) {
			return;
		}
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (display.isDisposed()) {
			return;
		}
		display.asyncExec(new Runnable() {
			public void run() {
				if (canUpdate()) {
					if (event.getTarget().getParent() != null) {
						viewer.add(event.getTarget().getParent(), event.getTarget());
					}
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.tigris.mtoolkit.osgimanagement.internal.browser.logic.
	 * ContentChangeListener
	 * #elementChanged(org.tigris.mtoolkit.osgimanagement.internal
	 * .browser.logic.ContentChangeEvent)
	 */
	public void elementChanged(final ContentChangeEvent event) {
		if (!canUpdate()) {
			return;
		}
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (display.isDisposed()) {
			return;
		}
		display.asyncExec(new Runnable() {
			public void run() {
				if (canUpdate()) {
					if (event.getTarget() instanceof Framework) {
						Framework fw = (Framework) event.getTarget();
						ConsoleManager.setName(fw.getConnector(), fw.getName());
						viewer.refresh();
					} else {
						viewer.refresh(event.getTarget());
					}
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.tigris.mtoolkit.osgimanagement.internal.browser.logic.
	 * ContentChangeListener
	 * #elementRemoved(org.tigris.mtoolkit.osgimanagement.internal
	 * .browser.logic.ContentChangeEvent)
	 */
	public void elementRemoved(ContentChangeEvent event) {
		if (!canUpdate()) {
			return;
		}
		final Model target = event.getTarget();
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (display.isDisposed()) {
			return;
		}
		display.asyncExec(new Runnable() {
			public void run() {
				if (canUpdate()) {
					viewer.remove(target);
				}
			}
		});

	}

	private boolean canUpdate() {
		return viewer != null && !viewer.getControl().isDisposed();
	}
}