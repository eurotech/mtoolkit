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
package org.tigris.mtoolkit.cdeditor.internal.integration;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;
import org.tigris.mtoolkit.cdeditor.internal.dialogs.DialogHelper;

/**
 * Class ProjectPropertyPage provides property page for the Component 
 * Description Editor. It can be displayed for example when properties of a 
 * given project are shown.
 */
public class ProjectPropertyPage extends PropertyPage {

	private Button enableValidationBtn;
	private Button setupAllBtn;

	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 20;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		enableValidationBtn = new Button(composite, SWT.CHECK);
		enableValidationBtn.setText("Enable component description validation for this project");
		enableValidationBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		enableValidationBtn.setSelection(EclipseHelper.isDescriptionValidationEnabled(getProject()));
		
		setupAllBtn = new Button(composite, SWT.PUSH);
		setupAllBtn.setText("Setup All Projects...");
		setupAllBtn.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		setupAllBtn.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				handleSetupAll();
			}
			
		});
		return composite;
	}
	
	private void handleSetupAll() {
		DialogHelper.openSetupDescriptionValidationDialog(getShell());
		performDefaults();
	}

	protected void performDefaults() {
		super.performDefaults();
		enableValidationBtn.setSelection(EclipseHelper.isDescriptionValidationEnabled(getProject()));
	}
	
	public boolean performOk() {
		boolean enable = enableValidationBtn.getSelection();
		IProject project = getProject();
		if (project == null)
			return false;
		EclipseHelper.enableDescriptionValidation(project, enable);
		return true;
	}

	private IProject getProject() {
		IAdaptable element = getElement();
		IProject project;
		if (element instanceof IProject) {
			project = (IProject) element;
		} else {
			project = (IProject) element.getAdapter(IProject.class);
		}
		return project;
	}
}