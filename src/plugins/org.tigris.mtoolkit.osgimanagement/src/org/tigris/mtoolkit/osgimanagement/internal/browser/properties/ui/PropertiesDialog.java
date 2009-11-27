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
package org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;

public abstract class PropertiesDialog extends TrayDialog implements ConstantsDistributor/*, SelectionListener*/ {

	private PropertiesPage mainControl;
	private String title;
	private String tableTitle;
	
	public PropertiesDialog(Shell shell, String title) {
		this(shell, title, null);
	}
	
	public PropertiesDialog(Shell shell, String title, String tableTitle) {
		super(shell);
		this.setShellStyle(SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL | SWT.RESIZE);
		this.title = title;
		this.tableTitle = tableTitle;
	}

	public Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
//		setTitle(title);
//		if (tableTitle != null) {
//			setMessage(tableTitle);
//		}
		container.setLayout(new GridLayout());
		GridData mGD = new GridData(GridData.FILL_BOTH);
		mGD.heightHint = 470;
		mGD.heightHint = 240;
		container.setLayoutData(mGD);
		mainControl = createMainControl(container);
		attachHelp(container);
		return container;
	}

	public PropertiesPage getMainControl() {
		return mainControl;
	}

	protected PropertiesPage createMainControl(Composite container) {
		PropertiesPage page = new PropertiesPage();
		page.setTitle(title);
		if (tableTitle != null) {
			page.setGroupName(tableTitle);
		}
		page.createContents(container);
		return page;
	}

	protected abstract void attachHelp(Composite container);

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL,
				true);
	}
}
