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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.actions.SelectionProviderAction;

public class FindAction extends SelectionProviderAction {

	private Text filterText;

	public FindAction(ISelectionProvider provider, Text filterText, String label) {
		super(provider, label);
		this.filterText = filterText;
		this.setText(label + "@Ctrl+F");
	}

	// run method
	public void run() {
		filterText.forceFocus();
		filterText.selectAll();
	}

	// override to react properly to selection change
	public void selectionChanged(IStructuredSelection selection) {
	}
}