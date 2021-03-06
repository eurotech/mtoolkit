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
package org.tigris.mtoolkit.osgimanagement.application.actions;

import java.util.Iterator;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteApplication;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.application.logic.RemoteApplicationOperation;
import org.tigris.mtoolkit.osgimanagement.application.logic.StopApplicationOperation;
import org.tigris.mtoolkit.osgimanagement.application.model.Application;
import org.tigris.mtoolkit.osgimanagement.model.Model;


public class StopApplicationAction extends SelectionProviderAction implements IStateAction {

	public StopApplicationAction(ISelectionProvider provider, String label) {
		super(provider, label);
		updateState((IStructuredSelection) provider.getSelection());
	}

	// run method
	public void run() {
		ISelection selection = getSelection();
		Iterator iterator = getStructuredSelection().iterator();
		while (iterator.hasNext()) {
			Application application = (Application) iterator.next();
			RemoteApplicationOperation job = new StopApplicationOperation(application);
			job.schedule();
		}
		getSelectionProvider().setSelection(selection);
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
			if (!(model instanceof Application)) {
				enabled = false;
				break;
			}
			Application application = (Application) model;
			try {
				if (!(RemoteApplication.STATE_RUNNING.equals(application.getRemoteApplication().getState()) ||
					  RemoteApplication.STATE_MIXED.equals(application.getRemoteApplication().getState()))) {
					enabled = false;
					break;
				}
			} catch (IAgentException e) {
				enabled = false;
			}
		}
		this.setEnabled(enabled);
	}

}