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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;

public class ListDialog extends SelectionDialog implements ConstantsDistributor {

	/**
	 * Constructor for ListDialog.
	 * 
	 * @param arg0
	 */
	public ListDialog(Shell shell, String[] elements, String message) {
		super(shell);
		setInitialSelections(elements);
		setMessage(message);
		this.setShellStyle(SWT.RESIZE | SWT.TITLE | SWT.CLOSE | SWT.APPLICATION_MODAL);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite main = (Composite) super.createDialogArea(parent);

		Label messageLabel = createMessageArea(main);
		GridData layoutData = new GridData();
		int borderHeight = ((GridLayout) main.getLayout()).marginHeight
						+ ((GridLayout) main.getLayout()).horizontalSpacing;
		Point hints = messageLabel.computeSize(LIST_DIALOG_WIDTH - (borderHeight * 2), SWT.DEFAULT);
		layoutData.widthHint = hints.x;
		layoutData.heightHint = hints.y;
		messageLabel.setLayoutData(layoutData);

		List displayList = new List(main, SWT.BORDER | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
		displayList.setLayoutData(new GridData(GridData.FILL_BOTH));

		java.util.List itemsList = getInitialElementSelections();
		String[] itemsToSet = new String[itemsList.size()];

		itemsList.toArray(itemsToSet);
		displayList.setItems(itemsToSet);

		return main;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell shell) {
		shell.setText(Messages.package_analyze_title);
		super.configureShell(shell);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.SelectionDialog#createMessageArea(Composite)
	 */
	protected Label createMessageArea(Composite parent) {
		Label label = new Label(parent, SWT.WRAP);
		label.setText(getMessage());
		return label;
	}
}