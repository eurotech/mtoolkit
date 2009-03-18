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
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
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
import org.tigris.mtoolkit.cdeditor.internal.model.ICDReference;
import org.tigris.mtoolkit.cdeditor.internal.model.IEclipseContext;
import org.tigris.mtoolkit.cdeditor.internal.model.Validator;


/**
 * The ReferenceAddDialog allows adding a reference for a DS Component.
 * This dialog allows specifying the Name and Interface for the reference.
 */
public class ReferenceAddDialog extends StatusLineDialog {

	private Text interfaceText;
	private Text nameText;
	private Button browseBtn;

	private ICDReference reference;
	private IJavaProject project;
	
	private boolean referenceNameAutoFilled = true;

	/**
	 * Creates ReferenceAddDialog.
	 * @param shell the parent shell
	 * @param title the dialog title
	 * @param reference the reference object that will be filled with data
	 * 		obtained from the dialog
	 * @param context the eclipse context
	 */
	public ReferenceAddDialog(Shell shell, String title, ICDReference reference, IEclipseContext context) {
		super(shell, title);
		this.reference = reference;
		if (context != null)
			project = (IJavaProject) context.getAdapter(IJavaProject.class);
	}

	protected Control createDialogArea(Composite parent) {
		final boolean jdtContextAvailable = project != null;
		final Composite composite = (Composite) super.createDialogArea(parent);
		
		if (jdtContextAvailable)
			((GridLayout) composite.getLayout()).numColumns = 3;
		else
			((GridLayout) composite.getLayout()).numColumns = 2;
		GridData gridData;

		Label interfaceLabel = new Label(composite, SWT.WRAP);
		interfaceLabel.setText("Interface:");
		interfaceText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.minimumWidth = convertHorizontalDLUsToPixels(ICDDialogConstants.MINIMAL_ENTRY_WIDTH);
		interfaceText.setLayoutData(gridData);
		interfaceText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String refInterface = interfaceText.getText();
				IStatus newStatus = Validator.validateReferenceInterface(nameText.getText(), refInterface);
				estimateStatus(newStatus);
			}
		});
		interfaceText.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				suggestReferenceName();
			}
		});

		if (jdtContextAvailable) {
			browseBtn = new Button(composite, SWT.PUSH);
			browseBtn.setText("Browse...");
			browseBtn.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
			browseBtn.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					String selectedClassName = DialogHelper.openBrowseTypeDialog(getShell(), "Reference Interface Selection", project);
					if (selectedClassName != null) {
						interfaceText.setText(selectedClassName);
						suggestReferenceName();
					}
				}
				
			});
		}

		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setText("Name:");
		nameText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.minimumWidth = convertHorizontalDLUsToPixels(ICDDialogConstants.MINIMAL_ENTRY_WIDTH);
		nameText.setLayoutData(gridData);
		nameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				referenceNameAutoFilled = false;
				String name = nameText.getText();
				IStatus newStatus = Validator.validateReferenceName(name);
				estimateStatus(newStatus);
			}
		});

		if (jdtContextAvailable) {
			Label emptyLabel = new Label(composite, SWT.NONE);
			emptyLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		}
		
		return composite;
	}
	
	private void suggestReferenceName() {
		if (nameText.getText().length() == 0 || referenceNameAutoFilled) {
			String selectedClassName = interfaceText.getText();
			nameText.setText(DialogHelper.suggestReferenceName(selectedClassName));
			referenceNameAutoFilled = true;
		}
	}

	protected void refresh() {
		setTextField(nameText, reference.getName());
		setTextField(interfaceText, reference.getInterface());
	}

	protected void commit() {
		reference.setName(nameText.getText());
		reference.setInterface(interfaceText.getText());
	}

	public ICDReference getReference() {
		return reference;
	}

	private IStatus getTotalStatus() {
		IStatus tempStatus;
		tempStatus = Validator.validateReferenceName(nameText.getText());
		if (tempStatus.getSeverity() == IStatus.ERROR) {
			return tempStatus;
		}
		tempStatus = Validator.validateReferenceInterface(nameText.getText(), interfaceText.getText());
		if (tempStatus.getSeverity() == IStatus.ERROR) {
			return tempStatus;
		}
		if (tempStatus.getSeverity() == IStatus.WARNING) {
			return tempStatus;
		}
		tempStatus = Validator.validateReferenceInterface(nameText.getText(), interfaceText.getText());
		if (tempStatus.getSeverity() == IStatus.WARNING) {
			return tempStatus;
		}
		return new Status(IStatus.OK, Validator.CDEDITOR_ID, "");
	}

	protected void estimateStatus(IStatus newStatus) {
		if (newStatus.getSeverity() == IStatus.ERROR) {
			updateStatus(newStatus);
		} else {
			IStatus totalStatus = getTotalStatus();
			updateStatus(totalStatus);
		}
	}
}
