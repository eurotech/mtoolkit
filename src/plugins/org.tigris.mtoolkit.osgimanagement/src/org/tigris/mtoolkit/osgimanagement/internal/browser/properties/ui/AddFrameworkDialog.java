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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.common.PluginUtilities;
import org.tigris.mtoolkit.common.certificates.CertificatesPanel;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.osgimanagement.installation.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.IHelpContextIds;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public class AddFrameworkDialog extends TitleAreaDialog implements ConstantsDistributor, FrameworkPanel.ErrorMonitor {

  public Button             connectButton;

  private FrameworkPanel    fwPanel;
  private CertificatesPanel certificatesPanel;

  private FrameworkImpl     fw;

  private Composite         mainContent;

  private boolean           addFramework;

  private Model             parent;

  public AddFrameworkDialog(Model parent, FrameworkImpl element, boolean newFramework) {
    super(PluginUtilities.getActiveWorkbenchShell());
    this.addFramework = newFramework;
    this.parent = parent;
    this.setShellStyle(SWT.RESIZE | SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL);
    fw = element;
  }

  // Create page contents
  @Override
  protected Control createDialogArea(Composite parent) {
    Control main = super.createDialogArea(parent);
    setTitle("Framework details");
    setMessage("Edit framework details");

    parent.getShell().setText(addFramework ? Messages.add_framework_title : Messages.framework_properties_title);

    mainContent = new Composite((Composite) main, SWT.NONE);
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
    if (!fw.isAutoConnected() && fw.getParent() == null) {
      connectButton = createCheckboxButton(Messages.connect_button_label, mainContent);
      connectButton.setEnabled(!fw.isConnected());
    }

    init();

    PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.FW_ADD_REMOVE);

    return mainContent;
  }

  private Button createCheckboxButton(String label, Composite parent) {
    Button resultButton = new Button(parent, SWT.CHECK);
    GridData grid = new GridData(GridData.FILL_HORIZONTAL);
    resultButton.setText(label);
    resultButton.setLayoutData(grid);
    resultButton.setSelection(false);
    return resultButton;
  }

  @Override
  protected void okPressed() {
    boolean correct = fwPanel.validate();
    if (correct) {
      setFWSettings();
      if (connectButton != null && !connectButton.isDisposed() && connectButton.getSelection()) {
        FrameworkConnectorFactory.connectFrameWork(fw);
      }
      super.okPressed();
    }
  }

  private void init() {
    IMemento config = fw.getConfig();

    if (connectButton != null) {
      Boolean connect = config.getBoolean(CONNECT_TO_FRAMEWORK);
      if (connect != null) {
        connectButton.setSelection(connect.booleanValue());
      }
    }

    // Framework Panel
    fwPanel.initialize(config);

    // Signing Certificates
    certificatesPanel.initialize(fw.getSignCertificateUids());
  }

  // Called when target options are changed
  private void setFWSettings() {
    boolean connChanged = saveConfig(fw.getConfig());
    fw.setName(fw.getConfig().getString(FRAMEWORK_NAME));

    if (addFramework) {
      parent.addElement(fw);
      addFramework = false;
    } else {
      DeviceConnector connector = fw.getConnector();
      if (connector != null) {
        if (fw.isConnected() && connChanged) {
          Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
          MessageDialog.openInformation(shell, Messages.framework_ip_changed_title,
              Messages.framework_ip_changed_message);
        }
      }
      fw.updateElement();
      FrameWorkView fwView = FrameWorkView.getActiveInstance();
      if (fwView != null) {
        final TreeViewer tree = fwView.getTree();
        tree.setSelection(tree.getSelection());
      }
    }
  }

  /**
   * Save ui values to storage
   *
   * @param config
   * @return true if connection properties have changed
   */
  protected boolean saveConfig(IMemento config) {
    // Framework panel
    boolean connChanged = fwPanel.save(config);

    // Signing Certificates
    fw.setSignCertificateUids(certificatesPanel.getSignCertificateUids());

    if (connectButton != null) {
      config.putBoolean(CONNECT_TO_FRAMEWORK, connectButton.getSelection());
    }
    return connChanged;
  }

  @Override
  public void setErrorMessage(String newErrorMessage) {
    super.setErrorMessage(newErrorMessage);
    Button ok = getButton(OK);
    if (ok != null) {
      ok.setEnabled(newErrorMessage == null);
    }
  }

}
