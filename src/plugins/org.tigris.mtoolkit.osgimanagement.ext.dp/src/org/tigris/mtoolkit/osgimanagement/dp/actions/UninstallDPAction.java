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
package org.tigris.mtoolkit.osgimanagement.dp.actions;

import java.util.Iterator;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.common.PluginUtilities;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.browser.model.Model;
import org.tigris.mtoolkit.osgimanagement.dp.logic.UninstallDeploymentOperation;
import org.tigris.mtoolkit.osgimanagement.dp.model.DeploymentPackage;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;

public class UninstallDPAction extends SelectionProviderAction implements IStateAction {

	public UninstallDPAction(ISelectionProvider provider, String label) {
		super(provider, label);
	}

	// run method
	public void run() {
		final int result[] = new int[1];
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				result[0] = PluginUtilities.showConfirmationDialog(FrameWorkView.getShell(), "Confirm uninstall",
				"Are you sure you want to uninstall selected resource(s)");
			}
		});
		if (result[0] != SWT.OK) {
			return;
		}

		Iterator iterator = getStructuredSelection().iterator();
		while (iterator.hasNext()) {
			DeploymentPackage dp = (DeploymentPackage) iterator.next();
			UninstallDeploymentOperation job = new UninstallDeploymentOperation(dp);
			job.schedule();
		}
		getSelectionProvider().setSelection(getSelection());
	}

	// override to react properly to selection change
	public void selectionChanged(IStructuredSelection selection) {
		updateState(selection);
	}

	public void updateState(IStructuredSelection selection) {
		if (selection.size() == 0) {
			setEnabled(false);
			return;
		}
		boolean enabled = true;
		Iterator iterator = selection.iterator();
		while (iterator.hasNext()) {
			Model model = (Model) iterator.next();
			if (!(model instanceof DeploymentPackage)) {
				enabled = false;
				break;
			}
		}
		this.setEnabled(enabled);
	}

}