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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDReference;
import org.tigris.mtoolkit.cdeditor.internal.model.IEclipseContext;
import org.tigris.mtoolkit.cdeditor.internal.model.Validator;
import org.tigris.mtoolkit.common.gui.StatusLineDialog;


/**
 * The ReferenceEditDialog allows editing a reference for a DS Component.
 * This dialog allows specifying the Name, Interface, Cardinality, Policy, 
 * Target and Bind and Unbind methods of the reference.
 */
public class ReferenceEditDialog extends StatusLineDialog {

	public static final String FIELD_NAME = "Name";
	public static final String FIELD_INTERFACE = "Interface";
	public static final String FIELD_CARDINALITY = "Cardinality";
	public static final String FIELD_POLICY = "Policy";
	public static final String FIELD_TARGET = "Target";
	public static final String FIELD_BIND = "Bind Method";
	public static final String FIELD_UNBIND = "Unbind Method";

	private Text interfaceText;
	private Text targetText;
	private Text bindText;
	private Text unbindText;
	private Text nameText;
	private Combo cardinalityCombo;
	private Combo policyCombo;
	private Button browseBtn;

	private ICDReference reference;
	private String initialFocus = "";

	private IJavaProject project;
	private boolean referenceNameAutofill = false;

	/**
	 * Creates ReferenceEditDialog.
	 * @param shell the parent shell
	 * @param title the dialog title
	 * @param reference the reference object that will be edited
	 * @param initialFocus specifies which control has the initial focus. 
	 * 		Available values are <b>FIELD_NAME</b>, <b>FIELD_INTERFACE</b>, 
	 * 		<b>FIELD_CARDINALITY</b>, <b>FIELD_POLICY</b>, <b>FIELD_TARGET</b>,
	 * 		<b>FIELD_BIND</b> and <b>FIELD_UNBIND</b>
	 * @param context the eclipse context
	 */
	public ReferenceEditDialog(Shell shell, String title,
			ICDReference reference, String initialFocus, IEclipseContext context) {
		super(shell, title);
		this.reference = reference;
		this.initialFocus = initialFocus;
		if (context != null)
			this.project = (IJavaProject) context.getAdapter(IJavaProject.class);
	}

	protected Control createDialogArea(Composite parent) {
		final boolean jdtContextAvailable = project != null;

		final Composite composite = (Composite) super.createDialogArea(parent);
		if (jdtContextAvailable)
			((GridLayout) composite.getLayout()).numColumns = 3;
		else
			((GridLayout) composite.getLayout()).numColumns = 2;

		int defaultHorizontalSpan = jdtContextAvailable ? 2 : 1;
		GridData gridData;

		Label interfaceLabel = new Label(composite, SWT.WRAP);
		interfaceLabel.setText("Interface:*");
		interfaceText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.minimumWidth = convertHorizontalDLUsToPixels(ICDDialogConstants.MINIMAL_ENTRY_WIDTH);
		interfaceText.setLayoutData(gridData);
		interfaceText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String refInterface = interfaceText.getText();
				IStatus newStatus = Validator.validateReferenceInterface(getReference().getName(), refInterface);
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
			browseBtn.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
			browseBtn.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					String selectedClassName = DialogHelper.openBrowseTypeDialog(getShell(), "Reference Interface Selection", project, interfaceText.getText());
					if (selectedClassName != null) {
						interfaceText.setText(selectedClassName);
						suggestReferenceName();
					}
				}

			});
		}

		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setText("Name:*");
		nameText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = defaultHorizontalSpan;
		gridData.minimumWidth = convertHorizontalDLUsToPixels(ICDDialogConstants.MINIMAL_ENTRY_WIDTH);
		nameText.setLayoutData(gridData);
		nameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String name = nameText.getText();
				IStatus newStatus = Validator.validateReferenceName(name);
				estimateStatus(newStatus);
				referenceNameAutofill = false;
			}
		});

		Label cardinalityLabel = new Label(composite, SWT.WRAP);
		cardinalityLabel.setText("Cardinality:");
		cardinalityCombo = new Combo(composite, SWT.READ_ONLY | SWT.BORDER);
		cardinalityCombo.setItems(ICDReference.CARDINALITY_NAMES_SHORT); //$NON-NLS-1$
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = defaultHorizontalSpan;
		gridData.minimumWidth = convertHorizontalDLUsToPixels(ICDDialogConstants.MINIMAL_ENTRY_WIDTH);
		cardinalityCombo.setLayoutData(gridData);

		Label policyLabel = new Label(composite, SWT.WRAP);
		policyLabel.setText("Policy:");
		policyCombo = new Combo(composite, SWT.READ_ONLY | SWT.BORDER);
		policyCombo.setItems(ICDReference.POLICY_NAMES);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = defaultHorizontalSpan;
		gridData.minimumWidth = convertHorizontalDLUsToPixels(ICDDialogConstants.MINIMAL_ENTRY_WIDTH);
		policyCombo.setLayoutData(gridData);

		Label targetLabel = new Label(composite, SWT.WRAP);
		targetLabel.setText("Target:");
		targetText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = defaultHorizontalSpan;
		gridData.minimumWidth = convertHorizontalDLUsToPixels(ICDDialogConstants.MINIMAL_ENTRY_WIDTH);
		targetText.setLayoutData(gridData);
		targetText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String target = targetText.getText();
				IStatus newStatus = Validator.validateReferenceTarget(getReference().getName(), target);
				estimateStatus(newStatus);
			}
		});

		Label bindLabel = new Label(composite, SWT.WRAP);
		bindLabel.setText("Bind Method:");
		bindText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = defaultHorizontalSpan;
		gridData.minimumWidth = convertHorizontalDLUsToPixels(ICDDialogConstants.MINIMAL_ENTRY_WIDTH);
		bindText.setLayoutData(gridData);
		bindText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String bind = bindText.getText();
				IStatus newStatus = Validator.validateReferenceBindMethod(getReference().getName(), bind);
				estimateStatus(newStatus);
			}
		});

		Label unbindLabel = new Label(composite, SWT.WRAP);
		unbindLabel.setText("Unbind Method:");
		unbindText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = defaultHorizontalSpan;
		gridData.minimumWidth = convertHorizontalDLUsToPixels(ICDDialogConstants.MINIMAL_ENTRY_WIDTH);
		unbindText.setLayoutData(gridData);
		unbindText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String unbind = unbindText.getText();
				IStatus newStatus = Validator.validateReferenceUnbindMethod(getReference().getName(), unbind);
				estimateStatus(newStatus);
			}
		});

		setFocus(initialFocus);

		return composite;
	}

	private void setFocus(String focusDescription) {
		if (focusDescription == "") {
			return;
		}
		if (focusDescription.equals(FIELD_NAME)) {
			nameText.setFocus();
		}
		if (focusDescription.equals(FIELD_INTERFACE)) {
			interfaceText.setFocus();
		}
		if (focusDescription.equals(FIELD_CARDINALITY)) {
			cardinalityCombo.setFocus();
		}
		if (focusDescription.equals(FIELD_POLICY)) {
			policyCombo.setFocus();
		}
		if (focusDescription.equals(FIELD_TARGET)) {
			targetText.setFocus();
		}
		if (focusDescription.equals(FIELD_BIND)) {
			bindText.setFocus();
		}
		if (focusDescription.equals(FIELD_UNBIND)) {
			unbindText.setFocus();
		}
	}

	protected void refresh() {
		setTextField(nameText, reference.getName());
		setTextField(interfaceText, reference.getInterface());
		setTextField(targetText, reference.getTarget());
		setTextField(bindText, reference.getBind());
		setTextField(unbindText, reference.getUnbind());
		int policy = reference.getPolicy() - 1;
		if (policy == -1) {
			policyCombo.deselectAll();
		} else {
			policyCombo.select(policy);
		}
		int cardinality = reference.getCardinality() - 1;
		if (cardinality == -1) {
			cardinalityCombo.deselectAll();
		} else {
			cardinalityCombo.select(cardinality);
		}
	}

	protected void commit() {
		reference.setName(nameText.getText());
		reference.setInterface(interfaceText.getText());
		int cardinality = cardinalityCombo.getSelectionIndex() + 1;
		if (cardinality != ICDReference.CARDINALITY_UNKNOWN)
			reference.setCardinality(cardinality);
		int policy = policyCombo.getSelectionIndex() + 1;
		if (policy != ICDReference.POLICY_UNKNOWN)
			reference.setPolicy(policy);
		reference.setTarget(targetText.getText());
		reference.setBind(bindText.getText());
		reference.setUnbind(unbindText.getText());
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
		tempStatus = Validator.validateReferenceInterface(getReference().getName(), interfaceText.getText());
		if (tempStatus.getSeverity() == IStatus.ERROR) {
			return tempStatus;
		}
		tempStatus = Validator.validateReferenceTarget(getReference().getName(), targetText.getText());
		if (tempStatus.getSeverity() == IStatus.ERROR) {
			return tempStatus;
		}
		if (tempStatus.getSeverity() == IStatus.WARNING) {
			return tempStatus;
		}
		tempStatus = Validator.validateReferenceInterface(getReference().getName(), interfaceText.getText());
		if (tempStatus.getSeverity() == IStatus.WARNING) {
			return tempStatus;
		}
		return new Status(IStatus.OK, Validator.CDEDITOR_ID, "");
	}

	protected void estimateStatus(IStatus newStatus) {
		if (newStatus.getSeverity() == IStatus.ERROR) {
			updateStatus(newStatus);
		} else {
			updateStatus(getTotalStatus());
		}
	}

	private void suggestReferenceName() {
		if (nameText.getText().length() == 0 || referenceNameAutofill) {
			String selectedClass = interfaceText.getText();
			nameText.setText(DialogHelper.suggestReferenceName(selectedClass));
			referenceNameAutofill = true;
		}
	}
}
