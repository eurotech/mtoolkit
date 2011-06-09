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
package org.tigris.mtoolkit.common.gui;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.common.Messages;

public class PasswordDialog extends TrayDialog {

  private Text   txtPass;
  private String title;
  private String msg;
  private String pass;

  public PasswordDialog(Shell parentShell, String title) {
    super(parentShell);
    Assert.isNotNull(title);
    this.title = title;
  }

  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(title);
  }

  protected Control createDialogArea(Composite parent) {
    PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.PASSWORD_DIALOG);
    Composite composite = (Composite) super.createDialogArea(parent);

    GridLayout layout = (GridLayout) composite.getLayout();
    layout.numColumns = 2;

    if (msg != null) {
      Label lblMsg = new Label(composite, SWT.WRAP);
      GridData gridData = new GridData();
      gridData.horizontalSpan = 2;
      gridData.widthHint = 240;
      lblMsg.setLayoutData(gridData);
      lblMsg.setText(msg);
    }

    Label lblPass = new Label(composite, SWT.CENTER);
    lblPass.setLayoutData(new GridData());
    lblPass.setText(Messages.PasswordDialog_lblPassword);

    txtPass = new Text(composite, SWT.BORDER | SWT.PASSWORD);
    GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    txtPass.setLayoutData(gridData);

    return composite;
  }

  public void setMessage(String msg) {
    this.msg = msg;
  }

  public String getMessage() {
    return msg;
  }

  protected void okPressed() {
    pass = txtPass.getText();
    super.okPressed();
  }

  public String getPassword() {
    return pass;
  }
}
