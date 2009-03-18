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
package org.tigris.mtoolkit.cdeditor.internal.widgets;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.tigris.mtoolkit.cdeditor.internal.dialogs.DialogHelper;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDComponent;
import org.tigris.mtoolkit.cdeditor.internal.model.IEclipseContext;


public class CreateComponentGroup implements SelectionListener {

	private static final String NAME_LABEL = "Component name:";
	private static final String IMPLCLASS_LABEL = "Component class:";

	private InputValidator validator;
	private IJavaProject project;

	private Text nameText;
	private Text implClassText;
	private Button browseBtn;
	private Composite parent;

	private String name = "";
	private String implClass = "";

	private int minimalTextFieldWidth = SWT.DEFAULT;

	private boolean implClassAutoFilled = true;

	private ModifyListener validationListener = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			validateInput();
		}
	};

	public interface InputValidator {
		public void validate(CreateComponentGroup group);
	}

	public CreateComponentGroup(InputValidator validator) {
		this(validator, null);
	}

	public CreateComponentGroup(InputValidator validator,
			IEclipseContext context) {
		this.validator = validator;
		if (context != null)
			project = (IJavaProject) context.getAdapter(IJavaProject.class);
	}

	public Control createContents(Composite aParent) {
		this.parent = aParent;
		boolean jdtContextAvailable = project != null;
		Composite composite = new Composite(aParent, SWT.NONE);
		if (jdtContextAvailable)
			composite.setLayout(new GridLayout(3, false));
		else
			composite.setLayout(new GridLayout(2, false));

		Label nameLabel = new Label(composite, SWT.LEFT);
		nameLabel.setText(NAME_LABEL);
		nameLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		nameText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = minimalTextFieldWidth;
		nameText.setLayoutData(data);
		nameText.setText(name);
		nameText.addModifyListener(validationListener);
		nameText.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				suggestImplementationClass();
			}
		});

		if (jdtContextAvailable) {
			Label emptyLabel = new Label(composite, SWT.LEFT);
			emptyLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		}

		Label implClassLabel = new Label(composite, 0);
		implClassLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		implClassLabel.setText(IMPLCLASS_LABEL);

		implClassText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = minimalTextFieldWidth;
		implClassText.setLayoutData(data);
		implClassText.setText(implClass);
		implClassText.addModifyListener(validationListener);
		implClassText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				implClassAutoFilled = false;
			}
		});
		implClassText.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				if (implClassAutoFilled) {
					String implClassName = implClassText.getText();
					int idx = implClassName.lastIndexOf('.');
					implClassText.setSelection(idx + 1, implClassName.length());
				}
			}
		});

		if (jdtContextAvailable) {
			browseBtn = new Button(composite, SWT.PUSH);
			browseBtn.setText("Browse...");
			browseBtn.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
			browseBtn.addSelectionListener(this);
		}

		return composite;
	}

	private void validateInput() {
		if (validator != null)
			validator.validate(this);
	}

	public void setMinimalFieldWidth(int width) {
		this.minimalTextFieldWidth = width;
	}

	public void initialize(String aName, String aImplClass) {
		this.name = aName != null ? aName : nameText != null ? nameText.getText() : this.name;
		this.implClass = aImplClass != null ? aImplClass : implClassText != null ? implClassText.getText() : this.implClass;
		if (nameText != null && implClassText != null) {
			try {
				nameText.removeModifyListener(validationListener);
				implClassText.removeModifyListener(validationListener);
				nameText.setText(this.name);
				implClassText.setText(this.implClass);
			} finally {
				nameText.addModifyListener(validationListener);
				implClassText.addModifyListener(validationListener);
			}
			if (aImplClass != null)
				implClassAutoFilled = true;
		}
	}

	public String getComponentName() {
		return nameText.getText();
	}

	public String getComponentClass() {
		return implClassText.getText();
	}

	public void commitGroup(ICDComponent component) {
		component.setName(getComponentName());
		component.setImplementationClass(getComponentClass());
	}

	private void handleTypeBrowse() {
		String selectedClassName = DialogHelper.openBrowseClassDialog(parent.getShell(), "Component Implementation Class Selection", project);
		if (selectedClassName != null) {
			implClassText.setText(selectedClassName);
		}
	}

	public void widgetDefaultSelected(SelectionEvent e) {
	}

	public void widgetSelected(SelectionEvent e) {
		if (e.widget == browseBtn)
			handleTypeBrowse();
	}

	private void suggestImplementationClass() {
		if (implClassAutoFilled || implClassText.getText().length() == 0) {
			String componentName = nameText.getText();
			implClassText.setText(DialogHelper.suggestComponentImplementationClassName(componentName));
			implClassAutoFilled = true;
		}
	}
}
