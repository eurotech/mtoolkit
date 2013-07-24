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

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.common.PluginUtilities;
import org.tigris.mtoolkit.common.certificates.CertificatesPanel;
import org.tigris.mtoolkit.osgimanagement.installation.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.internal.IHelpContextIds;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public final class AddFrameworkDialog extends TitleAreaDialog implements FrameworkPanel.ErrorMonitor {
  private static final String CONNECT_TO_FRAMEWORK = "framework_connect_key";

  private FrameworkPanel      fwPanel;
  private CertificatesPanel   certificatesPanel;
  private Button              connectButton;

  private final FrameworkImpl fw;
  private final Model         parent;

  public AddFrameworkDialog(Model parent, String frameworkName) {
    super(PluginUtilities.getActiveWorkbenchShell());
    this.parent = parent;
    this.fw = new FrameworkImpl(frameworkName, false);
    this.setShellStyle(SWT.RESIZE | SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.TitleAreaDialog#setErrorMessage(java.lang.String)
   */
  @Override
  public void setErrorMessage(String newErrorMessage) {
    super.setErrorMessage(newErrorMessage);
    Button ok = getButton(OK);
    if (ok != null) {
      ok.setEnabled(newErrorMessage == null);
    }
  }

  // Create page contents
  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    Control main = super.createDialogArea(parent);
    setTitle("Framework details");
    setMessage("Edit framework details");

    parent.getShell().setText(Messages.add_framework_title);

    Composite mainContent = new Composite((Composite) main, SWT.NONE);
    mainContent.setLayout(new GridLayout());
    GridData mainGD = new GridData(GridData.FILL_BOTH);
    mainGD.minimumWidth = 300;
    mainContent.setLayoutData(mainGD);

    // Framework Panel
    fwPanel = new FrameworkPanel(mainContent, fw, this.parent);
    fwPanel.setErrorMonitor(this);

    // Signing Certificates
    certificatesPanel = new CertificatesPanel(mainContent, 1, 1);

    // Autoconnect checkbox
    connectButton = createCheckboxButton(Messages.connect_button_label, mainContent);
    connectButton.setEnabled(!fw.isConnected());

    init();

    PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.FW_ADD_REMOVE);
    return mainContent;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.Dialog#okPressed()
   */
  @Override
  protected void okPressed() {
    if (fwPanel.validate()) {
      setFWSettings();
      if (!connectButton.isDisposed() && connectButton.getSelection()) {
        FrameworkConnectorFactory.connectFramework(fw);
      }
      super.okPressed();
    }
  }

  private Button createCheckboxButton(String label, Composite parent) {
    Button resultButton = new Button(parent, SWT.CHECK);
    GridData grid = new GridData(GridData.FILL_HORIZONTAL);
    resultButton.setText(label);
    resultButton.setLayoutData(grid);
    resultButton.setSelection(false);
    return resultButton;
  }

  private void init() {
    final IMemento config = fw.getConfig();

    Boolean connect = config.getBoolean(CONNECT_TO_FRAMEWORK);
    if (connect != null) {
      connectButton.setSelection(connect.booleanValue());
    }

    // Framework Panel
    fwPanel.initialize(config);

    // Signing Certificates
    certificatesPanel.initialize(fw.getSignCertificateUids());
  }

  // Called when target options are changed
  private void setFWSettings() {
    saveConfig(fw.getConfig());
    fw.setName(fw.getConfig().getString(ConstantsDistributor.FRAMEWORK_NAME));
    parent.addElement(fw);
  }

  /**
   * Save ui values to storage
   *
   * @param config
   * @return true if connection properties have changed
   */
  private void saveConfig(IMemento config) {
    // Framework panel
    fwPanel.save(config);
    // Signing Certificates
    fw.setSignCertificateUids(certificatesPanel.getSignCertificateUids());
    config.putBoolean(CONNECT_TO_FRAMEWORK, connectButton.getSelection());
  }
}
