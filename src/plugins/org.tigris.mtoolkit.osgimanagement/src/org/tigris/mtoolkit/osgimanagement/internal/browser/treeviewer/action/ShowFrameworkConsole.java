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

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.browser.model.Model;
import org.tigris.mtoolkit.osgimanagement.internal.ConsoleView;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;

public class ShowFrameworkConsole extends SelectionProviderAction implements IStateAction {

	private TreeViewer tree;

	public ShowFrameworkConsole(ISelectionProvider provider, String label, TreeViewer tree) {
		super(provider, label);
		this.tree = tree;
	}

	public void run() {
		IStructuredSelection selection = (IStructuredSelection) tree.getSelection();
		Model model = (Model) selection.getFirstElement();
		final FrameWork fw = model.findFramework();

		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("org.tigris.mtoolkit.osgimanagement.internal.consoleview"); //$NON-NLS-1$
			Display display = Display.getCurrent();
			if (display == null)
				display = Display.getDefault();
			display.asyncExec(new Runnable() {
				public void run() {
					ConsoleView.setActiveServer(fw.getName());
				}
			});
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}

	// override to react properly to selection change
	public void selectionChanged(IStructuredSelection selection) {
		updateState(selection);
	}

	public void updateState(IStructuredSelection selection) {
		boolean enabled = false;
		FrameWork fw = null;
		for (Iterator it = selection.iterator(); it.hasNext();) {
			Model element = (Model) it.next();
			FrameWork next = element instanceof FrameWork ? (FrameWork) element : element.findFramework();
			if (fw == null) {
				// we found a framework, enable console
				fw = next;
				enabled = true;
			} else if (!fw.equals(next)) {
				// we have another framework, disable console button
				enabled = false;
				break;
			}
		}
		this.setEnabled(enabled);
	}
}