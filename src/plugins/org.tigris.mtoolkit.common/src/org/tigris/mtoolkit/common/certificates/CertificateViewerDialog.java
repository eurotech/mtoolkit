/*******************************************************************************
 * Copyright (c) 2005, 2013 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.common.certificates;

import java.security.cert.X509Certificate;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.tigris.mtoolkit.common.Messages;
import org.tigris.mtoolkit.common.UtilitiesPlugin;
import org.tigris.mtoolkit.common.gui.ErrorPanel;
import org.tigris.mtoolkit.common.gui.StatusLineDialog;
import org.tigris.mtoolkit.common.gui.X509CertificateViewer;

public class CertificateViewerDialog extends StatusLineDialog {

  private Composite             certificateDetails;
  private X509CertificateViewer xcv;
  private ICertificateDescriptor certDescriptor;
  private X509Certificate        certificate;
  private Throwable              exception;

  /**
   * @param parentShell
   */
  public CertificateViewerDialog(Shell parentShell, CertificateDetails certDetails) {
    super(parentShell, Messages.dlgCertViewer_title);
    this.certDescriptor = certDetails.getCertificateDescriptor();
    this.certificate = certDetails.getX509Certificate();
    this.exception = certDetails.getError();
  }

  @Override
  protected Control createContents(Composite parent) {
    certificateDetails = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    certificateDetails.setLayout(gridLayout);
    certificateDetails.setLayoutData(new GridData(GridData.FILL_BOTH));

    Composite labelComposite = new Composite(certificateDetails, SWT.NONE);
    gridLayout = new GridLayout();
    gridLayout.numColumns = 2;
    labelComposite.setLayout(gridLayout);
    labelComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
    createKeyValue(labelComposite, Messages.dlgCertViewer_labelAlias, certDescriptor.getAlias());
    createKeyValue(labelComposite, Messages.dlgCertViewer_labelLocation, certDescriptor.getStoreLocation());
    Group viewerGroup = new Group(certificateDetails, SWT.NONE);
    viewerGroup.setText(Messages.dlgCertMan_labelCertDetails);
    viewerGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
    gridLayout = new GridLayout();
    gridLayout.horizontalSpacing = 0;
    gridLayout.marginHeight = 0;
    gridLayout.marginWidth = 0;
    gridLayout.verticalSpacing = 0;
    viewerGroup.setLayout(gridLayout);
    if (certificate != null) {
      xcv = new X509CertificateViewer(viewerGroup, SWT.NONE);
      xcv.setCertificate(certificate);
    } else {
      ErrorPanel errorPanel = new ErrorPanel(viewerGroup, ErrorPanel.NONE, SWT.NONE);
      IStatus status = UtilitiesPlugin.newStatus(IStatus.ERROR, exception.getMessage(), exception);
      errorPanel.setStatus(status);
      errorPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
    }
    Control createContents = super.createContents(certificateDetails);
    updateStatus(UtilitiesPlugin.newStatus(IStatus.OK, ""));
    return createContents;
  }

  private void createKeyValue(Composite labelComposite, String key, String value) {
    Label aliasLabel = new Label(labelComposite, SWT.NONE);
    aliasLabel.setText(key);
    aliasLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
    Label aliasValue = new Label(labelComposite, SWT.NONE);
    aliasValue.setText(value);
  }
}
