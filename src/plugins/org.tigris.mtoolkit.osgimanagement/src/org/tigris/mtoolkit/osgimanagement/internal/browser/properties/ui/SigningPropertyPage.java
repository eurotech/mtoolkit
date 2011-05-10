package org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;
import org.tigris.mtoolkit.common.certificates.CertificatesPanel;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;

public class SigningPropertyPage extends PropertyPage {

	private CertificatesPanel certificatesPanel;
	private FrameworkImpl fw;

	public SigningPropertyPage() {
		super();
	}

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		fw = (FrameworkImpl) getElement();

		certificatesPanel = new CertificatesPanel(composite, 1, 1, GridData.FILL_HORIZONTAL);
		certificatesPanel.initialize(fw.getSignCertificateUids(fw.getConfig()));

		return composite;
	}

	protected void performDefaults() {

	}

	public boolean performOk() {
		fw.setSignCertificateUids(fw.getConfig(), certificatesPanel.getSignCertificateUids());
		return true;
	}

}