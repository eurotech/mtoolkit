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

import java.io.File;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.common.installation.BaseFileItem;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.dp.DPModelProvider;
import org.tigris.mtoolkit.osgimanagement.dp.logic.DPProcessor;
import org.tigris.mtoolkit.osgimanagement.installation.FrameworkProcessor;
import org.tigris.mtoolkit.osgimanagement.installation.FrameworkTarget;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.InstallDialog;
import org.tigris.mtoolkit.osgimanagement.model.Framework;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public class InstallDPAction extends SelectionProviderAction implements IStateAction {

	private TreeViewer parentView;

	public InstallDPAction(ISelectionProvider provider, String label) {
		super(provider, label);
		this.parentView = (TreeViewer) provider;
	}

	// run method
	public void run() {
		Model node = (Model) getStructuredSelection().getFirstElement();
		Framework framework = node.findFramework();
		installDPAction(framework, parentView);
		getSelectionProvider().setSelection(getSelection());
	}

	private void installDPAction(final Framework framework, TreeViewer parentView) {
		InstallDialog installDialog = new InstallDialog(parentView, InstallDialog.INSTALL_DP_TYPE);
		installDialog.open();
		final String result = installDialog.getResult();
		if ((installDialog.getReturnCode() > 0) || (result == null) || result.trim().equals("")) { //$NON-NLS-1$
			return;
		}

		Job job = new Job("Installing to " + framework.getName()) {
			public IStatus run(IProgressMonitor monitor) {
				InstallationItem item = new BaseFileItem(new File(result), DPProcessor.MIME_DP);
				FrameworkProcessor processor = new FrameworkProcessor();
				processor.setUseAdditionalProcessors(true);
				IStatus status = processor.processInstallationItem(item, new FrameworkTarget(framework), monitor);
				monitor.done();
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				return status;
			}
		};
		job.schedule();
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
		Framework fw = ((Model) selection.getFirstElement()).findFramework();
		boolean enabled = true;
		if (fw == null || !fw.isConnected()) {
			enabled = false;
		} else {
			Iterator iterator = selection.iterator();
			while (iterator.hasNext()) {
				Model model = (Model) iterator.next();
				if (model.findFramework() != fw) {
					enabled = false;
					break;
				}
				DeviceConnector connector = model.findFramework().getConnector();
				if (connector == null || !DPModelProvider.isDpSupported(connector)) {
					enabled = false;
					break;
				}
			}
		}
		setEnabled(enabled);
	}
}