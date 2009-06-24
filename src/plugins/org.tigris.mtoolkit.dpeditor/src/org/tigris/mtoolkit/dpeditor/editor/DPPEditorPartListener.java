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
package org.tigris.mtoolkit.dpeditor.editor;

import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;

/**
 * This class listening to part lifecycle events.
 */
public class DPPEditorPartListener implements IPartListener {

	/** The deployment package editor */
	DPPEditor editor;

	/**
	 * Constructor of the listener.
	 * 
	 * @param _editor
	 *            a deployment package editor
	 */
	public DPPEditorPartListener(DPPEditor _editor) {
		this.editor = _editor;
	}

	/*
	 * Notifies this listener that the given part has been activated.
	 * 
	 * @see
	 * org.eclipse.ui.IPartListener#partActivated(org.eclipse.ui.IWorkbenchPart)
	 */
	public void partActivated(IWorkbenchPart part) {
	}

	/*
	 * Notifies this listener that the given part has been deactivated.
	 * 
	 * @see
	 * org.eclipse.ui.IPartListener#partDeactivated(org.eclipse.ui.IWorkbenchPart
	 * )
	 */
	public void partDeactivated(IWorkbenchPart part) {
	}

	/*
	 * Notifies this listener that the given part has been brought to the top.
	 * 
	 * @see
	 * org.eclipse.ui.IPartListener#partBroughtToTop(org.eclipse.ui.IWorkbenchPart
	 * )
	 */
	public void partBroughtToTop(IWorkbenchPart part) {
	}

	/*
	 * Notifies this listener that the given part has been closed.
	 * 
	 * @see
	 * org.eclipse.ui.IPartListener#partClosed(org.eclipse.ui.IWorkbenchPart)
	 */
	public void partClosed(IWorkbenchPart part) {
		if (part instanceof DPPEditor) {
			if (editor != part) {
				return;
			}
			((DPPEditor) part).disposeAllPages();
		}
	}

	/*
	 * Notifies this listener that the given part has been opened.
	 * 
	 * @see
	 * org.eclipse.ui.IPartListener#partOpened(org.eclipse.ui.IWorkbenchPart)
	 */
	public void partOpened(IWorkbenchPart part) {
		if (part instanceof DPPEditor) {
			if (editor != part) {
				return;
			}
		}
	}
}
