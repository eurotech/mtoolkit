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

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.tigris.mtoolkit.cdeditor.internal.CDEditorPlugin;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDProperty;
import org.tigris.mtoolkit.cdeditor.internal.model.Validator;


/**
 * The PropertyEditDialog allows editing a property of a DS Component.
 * The property has Name, Type and Value.
 */
public class PropertyEditDialog extends StatusLineDialog {

	public static final String FIELD_NAME = "Name";
	public static final String FIELD_TYPE = "Type";
	public static final String FIELD_VALUE = "Value";

	private Text nameText;
	private Text valueTextArray;
	private Text valueTextSingle;

	private Button radioSingle;
	private Button radioArray;
	private Combo typeCombo;

	private ICDProperty property;

	private boolean forceSingleValue;
	private String initialFocus = "";

	/**
	 * Defines the minimal count of rows to be shown in the array input control.
	 * 
	 * @see #convertHeightInCharsToPixels(int)
	 * @see #convertWidthInCharsToPixels(int)
	 */
	public static final int MINIMAL_ARRAY_ROWS = 5;

	private static final String NAME_LABEL = "Name*:";
	private static final String TYPE_LABEL = "Type:";
	private static final String SINGLE = "Single Value*";
	private static final String ARRAY = "Multi Value*";

	/**
	 * Creates PropertyEditDialog.
	 * @param shell the parent shell
	 * @param title the dialog title
	 * @param property the edited property
	 * @param forceSingleValue specifies whether property has one single value
	 * 		or can have multiple values (array of values)
	 * @param initialFocus specifies which control has the initial focus. Available values are
	 * <b>FIELD_NAME</b>, <b>FIELD_TYPE</b> and <b>FIELD_VALUE</b>
	 */
	public PropertyEditDialog(Shell shell, String title, ICDProperty property,
			boolean forceSingleValue, String initialFocus) {
		super(shell, title);
		this.property = property;
		this.forceSingleValue = forceSingleValue;
		this.initialFocus = initialFocus;
	}

	protected void refresh() {
		setTextField(nameText, property.getName());
		typeCombo.select(property.getType() - 1);
		if (property.isMultiValue() && !forceSingleValue) {
			radioArray.setSelection(true);
			radioSingle.setSelection(false);
			valueTextArray.setEnabled(true);
			valueTextSingle.setEnabled(false);
			String bufferedValues = convertMultiValued(property.getValues());
			setTextField(valueTextArray, bufferedValues);
		} else {
			radioSingle.setSelection(true);
			radioArray.setSelection(false);
			valueTextArray.setEnabled(false);
			valueTextSingle.setEnabled(true);
			setTextField(valueTextSingle, property.getValue());
		}
		forceSingleValue = false;
	}

	private String convertMultiValued(String[] values) {
		String bufferedValues = ""; //$NON-NLS-1$
		for (int i = 0; i < values.length; i++) {
			bufferedValues += values[i] + System.getProperty("line.separator"); //$NON-NLS-1$
		}
		return bufferedValues;
	}

	protected Control createDialogArea(Composite parent) {
		// delegate the setup of the composite to the super implementation
		final Composite composite = (Composite) super.createDialogArea(parent);
		((GridLayout) composite.getLayout()).numColumns = 2; // change the
		// layout to two
		// columns

		Label nameLabel = new Label(composite, SWT.LEFT);
		nameLabel.setText(NAME_LABEL);
		nameLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		nameText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = convertHorizontalDLUsToPixels(ICDDialogConstants.MINIMAL_ENTRY_WIDTH);
		nameText.setLayoutData(data);
		nameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String name = nameText.getText();
				IStatus newStatus = Validator.validatePropertyName(name);
				estimateStatus(newStatus);

			}
		});

		Label typeLabel = new Label(composite, 0);
		typeLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		typeLabel.setText(TYPE_LABEL);

		typeCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		typeCombo.setItems(ICDProperty.TYPE_NAMES);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = convertHorizontalDLUsToPixels(ICDDialogConstants.MINIMAL_ENTRY_WIDTH);
		typeCombo.setLayoutData(data);
		typeCombo.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				if (radioSingle.getSelection()) {
					String singleText = valueTextSingle.getText();
					IStatus newStatus = Validator.validateSinglePropertyValue(nameText.getText(), singleText, typeCombo.getSelectionIndex() + 1);
					estimateStatus(newStatus);
				}
				if (radioArray.getSelection()) {
					String multiText = valueTextArray.getText();
					IStatus newStatus = Validator.validateMultiPropertyFlatValue(nameText.getText(), multiText, typeCombo.getSelectionIndex() + 1);
					estimateStatus(newStatus);
				}
			}

		});

		radioSingle = new Button(composite, SWT.RADIO);
		radioSingle.setText(SINGLE);
		radioSingle.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		radioSingle.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				valueTextArray.setEnabled(false);
				valueTextSingle.setEnabled(true);
				String singleText = valueTextSingle.getText();
				IStatus newStatus = Validator.validateSinglePropertyValue(nameText.getText(), singleText, typeCombo.getSelectionIndex() + 1);
				estimateStatus(newStatus);
			}
		});
		valueTextSingle = new Text(composite, SWT.SINGLE | SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = convertHorizontalDLUsToPixels(ICDDialogConstants.MINIMAL_ENTRY_WIDTH);
		valueTextSingle.setLayoutData(data);
		valueTextSingle.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String singleText = valueTextSingle.getText();
				IStatus newStatus = Validator.validateSinglePropertyValue(nameText.getText(), singleText, typeCombo.getSelectionIndex() + 1);
				estimateStatus(newStatus);
			}
		});

		radioArray = new Button(composite, SWT.RADIO);
		radioArray.setText(ARRAY);
		radioArray.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		radioArray.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				valueTextArray.setEnabled(true);
				valueTextSingle.setEnabled(false);
				String multiText = valueTextArray.getText();
				IStatus newStatus = Validator.validateMultiPropertyFlatValue(nameText.getText(), multiText, typeCombo.getSelectionIndex() + 1);
				estimateStatus(newStatus);
			}
		});

		valueTextArray = new Text(composite, SWT.MULTI | SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = convertHeightInCharsToPixels(MINIMAL_ARRAY_ROWS);
		data.widthHint = convertHorizontalDLUsToPixels(ICDDialogConstants.MINIMAL_ENTRY_WIDTH);
		valueTextArray.setLayoutData(data);
		valueTextArray.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String multiText = valueTextArray.getText();
				IStatus newStatus = Validator.validateMultiPropertyFlatValue(nameText.getText(), multiText, typeCombo.getSelectionIndex() + 1);
				estimateStatus(newStatus);
			}
		});

		return composite;
	}

	protected Control createContents(Composite parent) {
		Control result = super.createContents(parent);

		setFocus(initialFocus);

		return result;
	}

	private void setFocus(String focusDescription) {
		if (FIELD_NAME.equals(focusDescription))
			nameText.setFocus();
		else if (FIELD_TYPE.equals(focusDescription))
			typeCombo.setFocus();
		else if (FIELD_VALUE.equals(focusDescription)) {
			if (radioSingle.getSelection()) {
				valueTextSingle.setFocus();
			} else {
				valueTextArray.setFocus();
			}
		}
	}

	public ICDProperty getProperty() {
		return property;
	}

	protected void commit() {
		property.setName(nameText.getText());
		property.setType(typeCombo.getSelectionIndex() + 1);
		// so that type becomes one of ICDProperty constant types
		boolean isMulti = radioArray.getSelection();
		if (!isMulti) {
			property.setValue(valueTextSingle.getText());
		} else {
			String[] values = parseArrayValue(valueTextArray.getText());
			property.setValues(values);
		}
	}

	private String[] parseArrayValue(String text) {
		StringTokenizer textTokenizer = new StringTokenizer(text, System.getProperty("line.separator")); //$NON-NLS-1$
		List lines = new LinkedList();
		while (textTokenizer.hasMoreElements()) {
			String valueLine = (String) textTokenizer.nextElement();
			if (valueLine != null) {
				lines.add(valueLine);
			}
		}
		String values[] = (String[]) lines.toArray(new String[lines.size()]);
		return values;
	}

	protected void estimateStatus(IStatus newStatus) {
		if (newStatus.getSeverity() == IStatus.ERROR) {
			updateStatus(newStatus);
		} else {
			IStatus totalStatus = getTotalStatus();
			updateStatus(totalStatus);
		}
	}

	private IStatus getTotalStatus() {
		IStatus result;
		result = Validator.validatePropertyName(nameText.getText());
		IStatus tempStatus = Validator.validatePropertyType(nameText.getText(), typeCombo.getSelectionIndex() + 1);
		if (tempStatus.getSeverity() > result.getSeverity())
			result = tempStatus;
		if (valueTextSingle.isEnabled()) {
			tempStatus = Validator.validateSinglePropertyValue(nameText.getText(), valueTextSingle.getText(), typeCombo.getSelectionIndex() + 1);
			if (tempStatus.getSeverity() > result.getSeverity())
				result = tempStatus;
		} else if (valueTextArray.isEnabled()) {
			tempStatus = Validator.validateMultiPropertyFlatValue(nameText.getText(), valueTextArray.getText(), typeCombo.getSelectionIndex() + 1);
			if (tempStatus.getSeverity() > result.getSeverity())
				result = tempStatus;
		} else {
			return new Status(IStatus.ERROR, CDEditorPlugin.PLUGIN_ID, "Single or multi value property type must be selected");
		}
		return result;
	}

}
