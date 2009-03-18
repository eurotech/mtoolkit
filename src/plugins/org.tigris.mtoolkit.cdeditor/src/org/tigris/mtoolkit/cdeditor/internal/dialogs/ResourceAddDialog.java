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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
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
import org.tigris.mtoolkit.cdeditor.internal.model.ICDProperties;
import org.tigris.mtoolkit.cdeditor.internal.model.IEclipseContext;
import org.tigris.mtoolkit.cdeditor.internal.model.Validator;

/**
 * The ResourceAddDialog allows selecting an external properties file for the
 * <b>properties</b> element of a DS component.
 */
public class ResourceAddDialog extends StatusLineDialog {

	private Text nameText;
	private Button browseBtn;

	private ICDProperties resource;
	private IProject project;

	/**
	 * Defines the minimal count of rows to be shown in the array input control.
	 * 
	 * @see #convertHeightInCharsToPixels(int)
	 * @see #convertWidthInCharsToPixels(int)
	 */
	public static final int MINIMAL_ARRAY_ROWS = 5;

	private static final String NAME_LABEL = "Name:";
	
	/**
	 * Creates ResourceAddDialog.
	 * @param shell the parent shell
	 * @param title the dialog title
	 * @param property the ICDProperties object that will contain path to the properties file
	 * @param context the eclipse context
	 */
	public ResourceAddDialog(Shell shell, String title, ICDProperties property, IEclipseContext context) {
		super(shell, title);
		this.resource = property;
		if (context != null)
			project = (IProject) context.getAdapter(IProject.class);
	}

	protected void refresh() {
		setTextField(nameText, resource.getEntry());
	}

	protected Control createDialogArea(Composite parent) {
		boolean projectContextAvailable = project != null;
		// delegate the setup of the composite to the super implementation
		final Composite composite = (Composite) super.createDialogArea(parent);
		if (projectContextAvailable)
			// change the layout to three columns
			((GridLayout) composite.getLayout()).numColumns = 3;
		else
			// change the layout to three columns
			((GridLayout) composite.getLayout()).numColumns = 2;

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
				IStatus newStatus = Validator.validatePropertiesEntryName(name);
				updateStatus(newStatus);
			}
		});
		
		if (projectContextAvailable) {
			browseBtn = new Button(composite, SWT.PUSH);
			browseBtn.setText("Browse...");
			browseBtn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
			browseBtn.addSelectionListener(new SelectionAdapter() {
	
				public void widgetSelected(SelectionEvent e) {
					String selectedResource = DialogHelper.openBrowseResourceDialog(getShell(), "Properties Resource Selection", project);
					if (selectedResource != null)
						nameText.setText(selectedResource);
				}
				
			});
		}

		return composite;

	}

	public ICDProperties getResource() {
		return resource;
	}

	protected void commit() {
		resource.setEntry(nameText.getText());
	}
}
