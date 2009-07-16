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
package org.tigris.mtoolkit.cdeditor.internal.dialogs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDInterface;
import org.tigris.mtoolkit.cdeditor.internal.model.IEclipseContext;
import org.tigris.mtoolkit.cdeditor.internal.model.Validator;
import org.tigris.mtoolkit.common.gui.StatusLineDialog;


/**
 * The ServiceEditDialog allows editing the provided service for a DS 
 * component.
 */
public class ServiceEditDialog extends StatusLineDialog {

	private Text nameText;
	private Button browseBtn;
	
	private ICDInterface cdInterface;
	private IJavaProject project;

	/**
	 * Creates ServiceEditDialog.
	 * @param parent the parent shell
	 * @param title the dialog title
	 * @param serviceInterface the ICDInterface object that will be edited
	 * @param context the eclipse context
	 */
	public ServiceEditDialog(Shell parent, String title,
			ICDInterface serviceInterface, IEclipseContext context) {
		super(parent, title);
		cdInterface = serviceInterface;
		if (context != null)
			project = (IJavaProject) context.getAdapter(IJavaProject.class);
	}

	protected Control createDialogArea(Composite parent) {
		boolean jdtContextAvailable = project != null;
		
		final Composite composite = (Composite) super.createDialogArea(parent);
		if (jdtContextAvailable)
			((GridLayout) composite.getLayout()).numColumns = 3;
		else
			((GridLayout) composite.getLayout()).numColumns = 2;

		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setText("Service Name:");
		nameText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.minimumWidth = convertHorizontalDLUsToPixels(ICDDialogConstants.MINIMAL_ENTRY_WIDTH);
		nameText.setLayoutData(gridData);
		nameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String name = nameText.getText();
				IStatus newStatus = Validator.validateServiceInterface(name);
				updateStatus(newStatus);
			}
		});

		if (jdtContextAvailable) {
			browseBtn = new Button(composite, SWT.PUSH);
			browseBtn.setText("Browse...");
			browseBtn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
			browseBtn.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					String selectedClass = DialogHelper.openBrowseTypeDialog(getShell(), "Service Interface Selection", project, nameText.getText());
					if (selectedClass != null)
						nameText.setText(selectedClass);
				}
				
			});
		}
		return composite;
	}

	protected void commit() {
		cdInterface.setInterface(nameText.getText());
	}

	protected void refresh() {
		setTextField(nameText, cdInterface.getInterface());
	}

	public ICDInterface getInterface() {
		return cdInterface;
	}
}
