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

import java.io.File;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.logic.InstallDialogLogic;

public class InstallDialog extends Window implements ConstantsDistributor {

	private InstallDialogLogic logic;

	private Text textLocation;
	private Button browseButton;
	private Button okButton;
	private Button cancelButton;

	private String text;
	private FileDialog locationChooser;
	private int exitCode;
	private boolean canExit;

	public static final int INSTALL_BUNDLE_TYPE = 1;
	public static final int UPDATE_BUNDLE_TYPE = 2;
	public static final int INSTALL_DP_TYPE = 3;

	private int type = 0;

	private static final String JAR_FILTER = "*.jar"; //$NON-NLS-1$
	private static final String DP_FILTER = "*.dp"; //$NON-NLS-1$

	private static final String INSTALL_BUNDLE_TITLE = Messages.install_dialog_title;
	private static final String UPDATE_BUNDLE_TITLE = Messages.update_dialog_title;
	private static final String INSTALL_DP_TITLE = Messages.install_dp_dialog_title;

	private static final String INSTALL_BUNDLE_LABEL = Messages.install_label;
	private static final String UPDATE_BUNDLE_LABEL = Messages.update_label;
	private static final String INSTALL_DP_LABEL = Messages.install_dp_label;

	public InstallDialog(TreeViewer parentView, int type) {
		super(parentView.getControl().getShell());
		logic = new InstallDialogLogic(this);
		this.setShellStyle(SWT.RESIZE | SWT.TITLE | SWT.CLOSE | SWT.APPLICATION_MODAL);
		this.type = type;
	}

	protected Control createContents(Composite parent) {
		setBlockOnOpen(true);
		GridData grid;

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		parent.setLayout(gridLayout);

		Label labelLocation = new Label(parent, SWT.NONE);
		grid = new GridData();
		grid.horizontalSpan = 2;
		labelLocation.setLayoutData(grid);

		switch (type) {
		case INSTALL_BUNDLE_TYPE: {
			labelLocation.setText(INSTALL_BUNDLE_LABEL);
			parent.getShell().setText(INSTALL_BUNDLE_TITLE);
			break;
		}
		case UPDATE_BUNDLE_TYPE: {
			labelLocation.setText(UPDATE_BUNDLE_LABEL);
			parent.getShell().setText(UPDATE_BUNDLE_TITLE);
			break;
		}
		case INSTALL_DP_TYPE: {
			labelLocation.setText(INSTALL_DP_LABEL);
			parent.getShell().setText(INSTALL_DP_TITLE);
			break;
		}
		}
		textLocation = new Text(parent, SWT.SINGLE | SWT.BORDER);

		browseButton = new Button(parent, SWT.PUSH);
		browseButton.setText(Messages.browse_button_label);

		grid = new GridData(GridData.FILL_HORIZONTAL);
		grid.widthHint = 4 * browseButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		textLocation.setLayoutData(grid);
		textLocation.addModifyListener(logic);

		grid = new GridData();
		browseButton.setLayoutData(grid);
		browseButton.addSelectionListener(logic);

		// OK, Cancel properties holder
		Composite okcancelHolder = new Composite(parent, SWT.NONE);
		grid = new GridData(GridData.HORIZONTAL_ALIGN_END);
		grid.horizontalSpan = 2;
		okcancelHolder.setLayoutData(grid);
		GridLayout okcancelHolderGrid = new GridLayout();
		okcancelHolderGrid.numColumns = 2;
		okcancelHolderGrid.makeColumnsEqualWidth = true;
		okcancelHolder.setLayout(okcancelHolderGrid);

		okButton = new Button(okcancelHolder, SWT.PUSH);
		grid = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		okButton.setText(Messages.ok_button_label);
		okButton.setLayoutData(grid);
		okButton.addSelectionListener(logic);
		parent.getShell().setDefaultButton(okButton);

		cancelButton = new Button(okcancelHolder, SWT.PUSH);
		grid = new GridData(GridData.HORIZONTAL_ALIGN_END);
		cancelButton.setText(Messages.cancel_button_label);
		cancelButton.setLayoutData(grid);
		cancelButton.addSelectionListener(logic);

		getShell().setDefaultButton(okButton);
		updateButtons();
		return parent;
	}

	private FileDialog getLocationChooser() {
		locationChooser = new FileDialog(getShell(), SWT.OPEN | SWT.SINGLE);
		String[] filter = { (type == INSTALL_BUNDLE_TYPE || type == UPDATE_BUNDLE_TYPE) ? JAR_FILTER : DP_FILTER, "*.*" }; //$NON-NLS-1$
		String[] names = { (type == INSTALL_BUNDLE_TYPE || type == UPDATE_BUNDLE_TYPE)	? Messages.bundle_filter_label
																						: Messages.deployment_package_filter_label,
			Messages.all_files_filter_label };
		locationChooser.setFilterExtensions(filter);
		locationChooser.setFilterNames(names);
		return locationChooser;
	}

	// Return value of text area
	public String getResult() {
		return text;
	}

	// Start Location Chooser
	public void startLocationChooser() {
		FileDialog chooser = getLocationChooser();
		if (FrameworkPlugin.fileDialogLastSelection != null) {
			chooser.setFileName(null);
			chooser.setFilterPath(FrameworkPlugin.fileDialogLastSelection);
		}
		String tmp = chooser.open();
		if (tmp != null) {
			if (!tmp.equals(FrameworkPlugin.fileDialogLastSelection) || textLocation.getText().trim().equals("")) { //$NON-NLS-1$
				FrameworkPlugin.fileDialogLastSelection = tmp;
				textLocation.setText(FrameworkPlugin.fileDialogLastSelection);
				okButton.setFocus();
			}
		}
		updateButtons();
	}

	public void updateButtons() {
		if ((textLocation != null) && !textLocation.isDisposed()) {
			if ((textLocation.getText() != null) && !textLocation.getText().trim().equals("")) { //$NON-NLS-1$
				File test = new File(textLocation.getText());
				if (test.exists() && test.isFile()) {
					okButton.setEnabled(true);
				} else {
					okButton.setEnabled(false);
				}
			} else {
				okButton.setEnabled(false);
			}
		}
	}

	public void closeOK() {
		File test = new File(textLocation.getText());
		FrameworkPlugin.fileDialogLastSelection = textLocation.getText();
		if (!test.exists() || !test.isFile()) {
			canExit = false;
			return;
		}
		exitCode = 0;
		text = textLocation.getText();
		canExit = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.window.Window#open()
	 */
	public int open() {
		updateButtons();
		exitCode = 1;
		canExit = true;
		return super.open();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.window.Window#getReturnCode()
	 */
	public int getReturnCode() {
		return exitCode;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.window.Window#close()
	 */
	public boolean close() {
		if (!canExit) {
			MessageDialog.openError(getShell(), Messages.standard_error_title, Messages.file_not_exists);
			canExit = true;
			return false;
		}
		return super.close();
	}

}