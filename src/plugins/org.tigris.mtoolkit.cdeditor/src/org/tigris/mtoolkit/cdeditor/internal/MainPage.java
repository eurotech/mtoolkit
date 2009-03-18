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
package org.tigris.mtoolkit.cdeditor.internal;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDModel;
import org.tigris.mtoolkit.cdeditor.internal.widgets.UIResources;

/**
 * The Main page of the Component Description Editor
 */
public class MainPage extends FormPage {
	
	public static final String PAGE_ID = "org.tigris.mtoolkit.cdeditor.MainPage"; //$NON-NLS-1$
	
	private ComponentsBlock componentsBlock;

	private Composite pageControl;

	private StackLayout stackLayout;

	private Composite formComposite;

	private Composite statusComposite;

	private Label statusText;
	
	private String statusMessage;
	
	private boolean canLeavePage = true;

	public MainPage(FormEditor editor, String id, String title) {
		super(editor, id, title);
	}

	protected void createFormContent(IManagedForm managedForm) {
		managedForm.setInput(getModel());
		managedForm.getForm().setText("Design");
		managedForm.getToolkit().decorateFormHeading(managedForm.getForm().getForm());
		managedForm.getForm().setImage(UIResources.getImage(UIResources.DESCRIPTION_ICON));
		componentsBlock = new ComponentsBlock(this);
		componentsBlock.createContent(managedForm);
	}

	public ICDModel getModel() {
		return ((ComponentDescriptionEditor) getEditor()).getModel();
	}
	
	public void setMessage(String message, boolean allowPageLeaving) {
		statusMessage = message;
		canLeavePage = allowPageLeaving;
		updateStatus();
	}
	
	private void updateStatus() {
		if (statusText != null) {
			if (statusMessage == null || statusMessage.length() == 0) {
				stackLayout.topControl = formComposite;
				statusText.setText("");
				pageControl.layout();
			} else {
				statusText.setText(statusMessage);
				stackLayout.topControl = statusComposite;
				pageControl.layout();
			}
		}
	}
	
	public void createPartControl(Composite parent) {
		pageControl = new Composite(parent, SWT.NONE);
		stackLayout = new StackLayout();
		pageControl.setLayout(stackLayout);

		formComposite = new Composite(pageControl, SWT.NONE);
		formComposite.setLayout(new FillLayout());
		super.createPartControl(formComposite);
		stackLayout.topControl = formComposite;
		
		FormToolkit formToolkit = getManagedForm().getToolkit();

		statusComposite = new Composite(pageControl, SWT.NONE);
		formToolkit.adapt(statusComposite);
		statusComposite.setLayout(new GridLayout());
		
		statusText = new Label(statusComposite, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.verticalIndent = 50;
		statusText.setLayoutData(gd);
		formToolkit.adapt(statusText, false, false);
		
		Label separator= new Label(statusComposite, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.LINE_SOLID);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		formToolkit.adapt(separator, false, false);
		
		// setup the page (single/multi component editor, etc.)
		componentsBlock.setupSingleMultiMode();
		
		updateStatus();
	}

	public Control getPartControl() {
		return pageControl;
	}
	
	public boolean canLeaveThePage() {
		return canLeavePage && super.canLeaveThePage();
	}
	
	
}
