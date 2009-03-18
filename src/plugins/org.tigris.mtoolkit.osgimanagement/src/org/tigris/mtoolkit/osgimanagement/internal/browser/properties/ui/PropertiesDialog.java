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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.osgimanagement.internal.IHelpContextIds;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;


public class PropertiesDialog extends Window implements ConstantsDistributor,
		SelectionListener {

	private PropertiesPage mainControl;
	private Button closeBtn;
	private FontMetrics fontMetrics;
	private boolean bundlePropsDialog = true;

	public PropertiesDialog(Shell shell, boolean bundleProps) {
		super(shell);
		this.setShellStyle(SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL
				| SWT.RESIZE);
		this.bundlePropsDialog = bundleProps;
	}

	public Control createContents(Composite parent) {
		GC gc = new GC(parent);
		gc.setFont(parent.getFont());
		fontMetrics = gc.getFontMetrics();
		gc.dispose();

		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		GridData mGD = new GridData(GridData.FILL_BOTH);
		mGD.heightHint = 470;
		mGD.heightHint = 240;
		container.setLayoutData(mGD);

		mainControl = createMainControl(container);

		// Bottom buttons group
		Composite buttonsArea = new Composite(container, SWT.NONE);
		buttonsArea.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		GridLayout buttonsGrid = new GridLayout();
		buttonsArea.setLayout(buttonsGrid);
		closeBtn = new Button(buttonsArea, SWT.PUSH);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_END);
		data.widthHint = Dialog.convertHorizontalDLUsToPixels(fontMetrics,
				IDialogConstants.BUTTON_WIDTH);
		closeBtn.setLayoutData(data);

		closeBtn.setText(Messages.close_button_label);
		closeBtn.addSelectionListener(this);

		getShell().setDefaultButton(closeBtn);
		container.pack();

		attachHelp(container);

		return container;
	}

	public PropertiesPage getMainControl() {
		return mainControl;
	}

	protected PropertiesPage createMainControl(Composite container) {
		PropertiesPage page = new PropertiesPage();
		if (bundlePropsDialog) {
			page.setTitle(Messages.bundle_properties_title);
		} else {
			page.setTitle(Messages.dp_properties_title);
		}
		page.createContents(container);

		return page;
	}

	protected void attachHelp(Composite container) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(
				container,
				bundlePropsDialog ? IHelpContextIds.PROPERTY_BUNDLE
						: IHelpContextIds.PROPERTY_PACKAGE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt
	 * .events.SelectionEvent)
	 */
	public void widgetSelected(SelectionEvent event) {
		if (event.getSource().equals(closeBtn)) {
			close();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse
	 * .swt.events.SelectionEvent)
	 */
	public void widgetDefaultSelected(SelectionEvent event) {
	}

}
