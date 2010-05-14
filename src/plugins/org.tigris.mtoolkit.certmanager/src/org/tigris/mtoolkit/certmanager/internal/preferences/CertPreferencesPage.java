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
package org.tigris.mtoolkit.certmanager.internal.preferences;

import java.util.Iterator;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.certmanager.internal.CertManagerPlugin;
import org.tigris.mtoolkit.certmanager.internal.Messages;
import org.tigris.mtoolkit.certmanager.internal.dialogs.CertificateManagementDialog;
import org.tigris.mtoolkit.common.certificates.CertUtils;
import org.tigris.mtoolkit.common.certificates.ICertificateDescriptor;
import org.tigris.mtoolkit.common.preferences.IMToolkitPreferencePage;

public class CertPreferencesPage extends PreferencePage implements
		IWorkbenchPreferencePage, IMToolkitPreferencePage {
	private TableViewer certificatesViewer;
	private Button btnAdd;
	private Button btnEdit;
	private Button btnRemove;
	private Button btnBrowse;
	private Text txtJarsignerLocation;

	private static final String ATTR_JARSIGNER_LOCATION = "jarsigner.location"; //$NON-NLS-1$

	public Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		Table table = new Table(composite, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.verticalSpan = 3;
		gridData.heightHint = 100;
		table.setLayoutData(gridData);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		TableColumn column = new TableColumn(table, SWT.LEFT);
		column.setText(Messages.certs_ColAlias);
		column.setWidth(120);

		column = new TableColumn(table, SWT.LEFT);
		column.setText(Messages.certs_ColLocation);
		column.setWidth(200);

		certificatesViewer = new TableViewer(table);
		certificatesViewer.setContentProvider(new CertContentProvider());
		certificatesViewer.setLabelProvider(new CertLabelProvider());
		certificatesViewer.setInput(CertStorage.getDefault());
		certificatesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtonsState();
			}
		});

		btnAdd = createButton(composite, Messages.certs_btnAdd);
		btnAdd.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				addCertificate();
			}
		});

		btnEdit = createButton(composite, Messages.certs_btnEdit);
		btnEdit.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				editCertificate();
			}
		});

		btnRemove = createButton(composite, Messages.certs_btnRemove);
		btnRemove.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				removeCertificate();
			}
		});

		Label label = new Label(composite, SWT.NONE);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		label.setLayoutData(gridData);
		label.setText(Messages.certs_lblJarsignerLocation);

		txtJarsignerLocation = new Text(composite, SWT.BORDER);
		String location = getJarsignerLocation();
		if (location != null) {
			txtJarsignerLocation.setText(location);
		}
		txtJarsignerLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		btnBrowse = new Button(composite, SWT.PUSH);
		btnBrowse.setText(Messages.browseLabel);
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				browseLocation();
			}
		});
		btnBrowse.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

		updateButtonsState();

		return composite;
	}

	private Button createButton(Composite parent, String label) {
		Button button = new Button(parent, SWT.PUSH);
		GridData gridData = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
		button.setLayoutData(gridData);
		button.setText(label);
		return button;
	}

	public void init(IWorkbench workbench) {
	}

	public void performDefaults() {
		super.performDefaults();

		CertStorage.getDefault().performDefaults();

		String defLocation = CertUtils.getDefaultJarsignerLocation();
		txtJarsignerLocation.setText(defLocation);
		saveJarsignerLocation(defLocation);
	}

	public boolean performOk() {
		CertStorage.getDefault().save();
		saveJarsignerLocation(txtJarsignerLocation.getText());
		return true;
	}

	public boolean performCancel() {
		return true;
	}

	private void addCertificate() {
		Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
		CertificateManagementDialog dialog = new CertificateManagementDialog(shell, Messages.dlgCertMan_titleAdd);
		if (dialog.open() == Dialog.OK) {
			CertDescriptor cert = new CertDescriptor(CertStorage.getDefault().generateCertificateUid());
			cert.setAlias(dialog.alias);
			cert.setStoreLocation(dialog.storeLocation);
			cert.setStoreType(dialog.storeType);
			cert.setStorePass(dialog.storePass);
			CertStorage.getDefault().addCertificate(cert);
		}
	}

	private void editCertificate() {
		IStructuredSelection selection = (IStructuredSelection) certificatesViewer.getSelection();
		Object el = selection.getFirstElement();
		if (!(el instanceof CertDescriptor)) {
			return;
		}
		CertDescriptor cert = (CertDescriptor) el;
		Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
		CertificateManagementDialog dialog = new CertificateManagementDialog(shell, Messages.dlgCertMan_titleEdit, cert);
		if (dialog.open() == Dialog.OK) {
			cert.setAlias(dialog.alias);
			cert.setStoreLocation(dialog.storeLocation);
			cert.setStoreType(dialog.storeType);
			cert.setStorePass(dialog.storePass);
		}
	}

	private void removeCertificate() {
		IStructuredSelection selection = (IStructuredSelection) certificatesViewer.getSelection();
		Iterator it = selection.iterator();
		while (it.hasNext()) {
			ICertificateDescriptor cert = (ICertificateDescriptor) it.next();
			(CertStorage.getDefault()).removeCertificate(cert);
		}
	}

	private void browseLocation() {
		String selectedFile = null;
		String path = txtJarsignerLocation.getText();
		FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);
		dialog.setFilterPath(path);
		dialog.setText("Open");
		selectedFile = dialog.open();

		if (selectedFile != null) {
			txtJarsignerLocation.setText(selectedFile);
			btnBrowse.setFocus();
		}
	}

	private void updateButtonsState() {
		IStructuredSelection selection = (IStructuredSelection) certificatesViewer.getSelection();
		switch (selection.size()) {
		case 0:
			btnEdit.setEnabled(false);
			btnRemove.setEnabled(false);
			return;
		case 1:
			btnEdit.setEnabled(true);
			btnRemove.setEnabled(true);
			return;
		default:
			btnEdit.setEnabled(false);
			btnRemove.setEnabled(true);
			return;
		}
	}

	private String getJarsignerLocation() {
		CertManagerPlugin plugin = CertManagerPlugin.getDefault();
		if (plugin == null) {
			return CertUtils.getDefaultJarsignerLocation();
		}
		IPreferenceStore store = plugin.getPreferenceStore();

		String location = store.getString(ATTR_JARSIGNER_LOCATION);
		if (location.length() == 0) {
			location = CertUtils.getDefaultJarsignerLocation();
		}
		return location;
	}

	private void saveJarsignerLocation(String location) {
		CertManagerPlugin plugin = CertManagerPlugin.getDefault();
		if (plugin == null) {
			return;
		}
		IPreferenceStore store = plugin.getPreferenceStore();

		store.setValue(ATTR_JARSIGNER_LOCATION, location);
	}
}
