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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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

public class InstallDialog extends TrayDialog implements ConstantsDistributor {

	private Text textLocation;
	private Button browseButton;
	private Button okButton;

	private String text;
	private FileDialog locationChooser;
	private int exitCode;
	private boolean canExit;

	public static final int INSTALL_BUNDLE_TYPE = 1;
	public static final int UPDATE_BUNDLE_TYPE = 2;
	public static final int INSTALL_DP_TYPE = 3;
	public static final int UNKNOWN_TYPE = -1;

	private int type = 0;

	private String label = null;
	private String title = null;
	private String filter = null;
	private String filterLabel = null;

	private static final String JAR_FILTER = "*.jar"; //$NON-NLS-1$
	private static final String DP_FILTER = "*.dp"; //$NON-NLS-1$

	private static final String INSTALL_BUNDLE_TITLE = Messages.install_dialog_title;
	private static final String UPDATE_BUNDLE_TITLE = Messages.update_dialog_title;
	private static final String INSTALL_DP_TITLE = Messages.install_dp_dialog_title;

	private static final String INSTALL_BUNDLE_LABEL = Messages.install_label;
	private static final String UPDATE_BUNDLE_LABEL = Messages.update_label;
	private static final String INSTALL_DP_LABEL = Messages.install_dp_label;

	public InstallDialog(TreeViewer parentView, String title, String label, String filter, String filterLabel) {
		this(parentView, UNKNOWN_TYPE);
		this.title = title;
		this.label = label;
		this.filter = filter;
		this.filterLabel = filterLabel;
		init();
	}

	public InstallDialog(TreeViewer parentView, int type) {
		super(parentView.getControl().getShell());
		this.type = type;
		init();
	}
	
	private void init() {
		this.setShellStyle(SWT.RESIZE | SWT.TITLE | SWT.CLOSE | SWT.APPLICATION_MODAL);
		
		switch (type) {
		case INSTALL_BUNDLE_TYPE: {
			label = INSTALL_BUNDLE_LABEL;
			title = INSTALL_BUNDLE_TITLE;
			filter = JAR_FILTER;
			filterLabel = Messages.bundle_filter_label;
			break;
		}
		case UPDATE_BUNDLE_TYPE: {
			label = UPDATE_BUNDLE_LABEL;
			title = UPDATE_BUNDLE_TITLE;
			filter = JAR_FILTER;
			filterLabel = Messages.bundle_filter_label;
			break;
		}
		case INSTALL_DP_TYPE: {
			label = INSTALL_DP_LABEL;
			title = INSTALL_DP_TITLE;
			filter = DP_FILTER;
			filterLabel = Messages.deployment_package_filter_label;
			break;
		}
		}
	}

	protected Control createDialogArea(Composite comp) {
		Composite parent = (Composite) super.createDialogArea(comp);
		
		setBlockOnOpen(true);
		GridData grid;

		GridLayout gridLayout = new GridLayout();
		// create button method increases columns number
		gridLayout.numColumns = 1;
		parent.setLayout(gridLayout);

		Label labelLocation = new Label(parent, SWT.NONE);
		grid = new GridData();
		grid.horizontalSpan = 2;
		labelLocation.setLayoutData(grid);

		labelLocation.setText(label);
		parent.getShell().setText(title);
		textLocation = new Text(parent, SWT.SINGLE | SWT.BORDER);

		browseButton = createButton(parent, IDialogConstants.CLIENT_ID, Messages.browse_button_label, false);

		grid = new GridData(GridData.FILL_HORIZONTAL);
		grid.widthHint = 4 * browseButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		textLocation.setLayoutData(grid);
		textLocation.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateButtons();
			}
		});

		grid = new GridData();
		return parent;
	}

	private FileDialog getLocationChooser() {
		locationChooser = new FileDialog(getShell(), SWT.OPEN | SWT.SINGLE);
		String[] filterArr = { filter, "*.*" }; //$NON-NLS-1$
		String[] namesArr = { filterLabel, Messages.all_files_filter_label };
		locationChooser.setFilterExtensions(filterArr);
		locationChooser.setFilterNames(namesArr);
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
		chooser.setText(Messages.install_file_dialog_title);
		String tmp = chooser.open();
		if (tmp != null) {
			FrameworkPlugin.fileDialogLastSelection = tmp;
			textLocation.setText(FrameworkPlugin.fileDialogLastSelection);
			okButton.setFocus();
		}
		updateButtons();
	}
	
	protected void createButtonsForButtonBar(Composite parent) {
		okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
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
	
	protected void okPressed() {
		closeOK();
		super.okPressed();
	}
	
	protected void buttonPressed(int buttonId) {
		super.buttonPressed(buttonId);
		if (buttonId == IDialogConstants.CLIENT_ID) {
			startLocationChooser();
		}
	}

}