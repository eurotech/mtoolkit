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
package org.tigris.mtoolkit.dpeditor.editor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.tigris.mtoolkit.dpeditor.editor.forms.ScrollableForm;
import org.tigris.mtoolkit.dpeditor.util.ResourceManager;

/**
 * The Form that presents the resources page in the Deployment package editor.
 */
public class ResourcesForm extends ScrollableForm {
	public static final String TITLE = "DPPEditor.ResourcesForm.title";

	/** The Form page in which this form will be added */
	private ResourcesFormPage page;
	/** The FormSection which will be added to this form */
	private ResourcesSection resourcesSection;

	/**
	 * Creates the new Form for the given form page.
	 * 
	 * @param page
	 *            the Form page in which this form will be added
	 */
	public ResourcesForm(ResourcesFormPage page) {
		this.page = page;
		setVerticalFit(true);
		setTitle(ResourceManager.getString(TITLE, ""));
	}

	/**
	 * This method is called from the <code>createControl</code> method and puts
	 * the form section in this form.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the created
	 *            client form
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.Form#createFormClient(org.eclipse.swt.widgets.Composite)
	 */
	protected void createFormClient(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.makeColumnsEqualWidth = true;
		layout.marginWidth = 10;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 10;
		parent.setLayout(layout);

		resourcesSection = new ResourcesSection(page);
		Control control = resourcesSection.createControl(parent);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = control.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		control.setLayoutData(gd);

		registerSection(resourcesSection);
	}

	/**
	 * Sets if this form will be editable or not.
	 * 
	 * @param editable
	 *            <code>boolean</code> value, that shows if this form will be
	 *            editable
	 */
	public void setEditable(boolean editable) {
		editable = false;
		resourcesSection.setEditable(editable);
	}
}
