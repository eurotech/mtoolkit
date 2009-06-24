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
package org.tigris.mtoolkit.dpeditor.editor.base;

import java.util.Iterator;
import java.util.Vector;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;

/**
 * Class implements common interface to all objects that provide a selection.
 */
public class SelectionProvider implements ISelectionProvider {

	/** All added selection listeners */
	private Vector listeners = new Vector();
	/** The current selection */
	private ISelection selection;

	/**
	 * Constructor of this class.
	 */
	public SelectionProvider() {
	}

	/**
	 * Adds a listener for selection changes in this selection provider.
	 * 
	 * @param listener
	 *            a selection changed listener
	 * 
	 * @see org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.addElement(listener);
	}

	/**
	 * Returns the current selection for this provider.
	 * 
	 * @return the current selection
	 * 
	 * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
	 */
	public ISelection getSelection() {
		return selection;
	}

	/**
	 * Removes the given selection change listener from this selection provider.
	 * 
	 * @param listener
	 *            a selection changed listener
	 * 
	 * @see org.eclipse.jface.viewers.ISelectionProvider#removeSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
	 */
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.removeElement(listener);
	}

	/**
	 * Sets the current selection for this selection provider.
	 * 
	 * @param selection
	 *            the new selection
	 * 
	 * @see org.eclipse.jface.viewers.ISelectionProvider#setSelection(org.eclipse.jface.viewers.ISelection)
	 */
	public synchronized void setSelection(ISelection selection) {
		this.selection = selection;
		SelectionChangedEvent event = new SelectionChangedEvent(this, selection);
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			ISelectionChangedListener listener = (ISelectionChangedListener) iter.next();
			listener.selectionChanged(event);
		}
	}
}
