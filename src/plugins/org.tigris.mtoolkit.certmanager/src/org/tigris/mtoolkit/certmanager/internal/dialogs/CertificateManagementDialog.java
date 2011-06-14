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
package org.tigris.mtoolkit.certmanager.internal.dialogs;

import java.io.File;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.certmanager.internal.Messages;
import org.tigris.mtoolkit.common.certificates.ICertificateDescriptor;

public class CertificateManagementDialog extends TitleAreaDialog {

	private String title;

	private Text txtAlias;
	private Text txtLocation;
	private Text txtStorePass;
	private Text txtKeyPass;
	private Button btnBrowse;
	private Combo comboType;

	public String alias;
	public String storeLocation;
	public String storeType;
	public String storePass;
	public String keyPass;

	private static final String storeTypes[] = { "JKS", //$NON-NLS-1$
	"JCEKS", //$NON-NLS-1$
	"PKCS11", //$NON-NLS-1$
	"PKCS12", //$NON-NLS-1$
	"CMSKS", //$NON-NLS-1$
	"IBMi5OSKeyStore", //$NON-NLS-1$
	"JCERACFKS", //$NON-NLS-1$
	"JCECCAKS" //$NON-NLS-1$
	};

	public CertificateManagementDialog(Shell shell, String title) {
		super(shell);
		Assert.isNotNull(title);
		this.title = title;
	}

	public CertificateManagementDialog(Shell shell, String title,
			ICertificateDescriptor init) {
		this(shell, title);
		alias = init.getAlias();
		storeLocation = init.getStoreLocation();
		storeType = init.getStoreType();
		storePass = init.getStorePass();
		keyPass = init.getKeyPass();
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(title);
	}

	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}

	protected Control createDialogArea(Composite parent) {
	      PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.CERT_MNG_DIALOG);
		final Composite parentComposite = (Composite) super.createDialogArea(parent);
		Composite composite = new Composite(parentComposite, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		setTitle(Messages.dlgCertMan_descr);

		Label lblAlias = new Label(composite, SWT.LEFT);
		lblAlias.setText(Messages.dlgCertMan_labelAlias);
		lblAlias.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		txtAlias = new Text(composite, SWT.BORDER);
		GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gridData.widthHint = 150;
		gridData.horizontalSpan = 2;
		txtAlias.setLayoutData(gridData);
		if (alias != null) {
			txtAlias.setText(alias);
		}
		txtAlias.addModifyListener(new TextModifyListener());

		Label lblLocation = new Label(composite, SWT.LEFT);
		lblLocation.setText(Messages.dlgCertMan_labelLocation);
		lblLocation.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		txtLocation = new Text(composite, SWT.BORDER);
		txtLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (storeLocation != null) {
			txtLocation.setText(storeLocation);
		}
		txtLocation.addModifyListener(new TextModifyListener());
		btnBrowse = new Button(composite, SWT.PUSH);
		btnBrowse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnBrowse.setText(Messages.browseLabel);
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				browseLocation();
			}
		});

		Label lblType = new Label(composite, SWT.LEFT);
		lblType.setText(Messages.dlgCertMan_labelType);
		lblType.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		comboType = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gridData.horizontalSpan = 2;
		comboType.setLayoutData(gridData);
		comboType.setItems(storeTypes);
		if (storeType != null) {
			for (int i = 0; i < storeTypes.length; i++) {
				if (storeTypes[i].equalsIgnoreCase(storeType)) {
					comboType.setText(storeType);
					break;
				}
			}
		}
		if (comboType.getSelectionIndex() < 0) {
			comboType.setText(storeTypes[0]);
		}

		Label lblPass = new Label(composite, SWT.LEFT);
		lblPass.setText(Messages.dlgCertMan_labelPass);
		lblPass.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		txtStorePass = new Text(composite, SWT.BORDER | SWT.PASSWORD);
		gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gridData.horizontalSpan = 2;
		txtStorePass.setLayoutData(gridData);
		if (storePass != null) {
			txtStorePass.setText(storePass);
		}

		Label lblKeyPass = new Label(composite, SWT.LEFT);
		lblKeyPass.setText(Messages.dlgCertMan_labelKeyPass);
		lblKeyPass.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		txtKeyPass = new Text(composite, SWT.BORDER | SWT.PASSWORD);
		gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gridData.horizontalSpan = 2;
		txtKeyPass.setLayoutData(gridData);
		if (keyPass != null) {
			txtKeyPass.setText(keyPass);
		}

		return composite;
	}

	protected void okPressed() {
		if (!verifyData()) {
			return;
		}
		alias = txtAlias.getText().trim();
		storeLocation = txtLocation.getText().trim();
		storeType = comboType.getText();
		storePass = txtStorePass.getText();
		keyPass = txtKeyPass.getText();
		super.okPressed();
	}

	private boolean verifyData() {
		if (txtAlias.getText().trim().length() == 0) {
			setMessage(Messages.dlgCertMan_verifyAliasEmpty, IMessageProvider.ERROR);
			return false;
		}
		String location = txtLocation.getText().trim();
		if (location.length() == 0) {
			setMessage(Messages.dlgCertMan_verifyLocationEmpty, IMessageProvider.ERROR);
			return false;
		}
		File keystore = new File(location);
		if (!keystore.exists() || !keystore.isFile()) {
			setMessage(Messages.dlgCertMan_verifyLocationNotExist, IMessageProvider.ERROR);
			return false;
		}

		setMessage("", IMessageProvider.NONE); //$NON-NLS-1$
		return true;
	}

	private void browseLocation() {
		FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
		fd.setText(Messages.dlgCertMan_browseDlgCaption);
		String[] filterExt = { "*.keystore;*.jks", "*.*" }; //$NON-NLS-1$ //$NON-NLS-2$
		fd.setFilterExtensions(filterExt);
		String[] filterNames = { "Keystore files (*.keystore; *.jks)", "All files (*.*)" }; //$NON-NLS-1$ //$NON-NLS-2$
		fd.setFilterNames(filterNames);
		String location = fd.open();
		if (location != null) {
			txtLocation.setText(location);
		}
	}

	private class TextModifyListener implements ModifyListener {
		public void modifyText(ModifyEvent e) {
			verifyData();
		}
	}
}
