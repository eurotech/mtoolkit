/*******************************************************************************
 * Copyright (c) 2005, 2012 ProSyst Software GmbH and others.
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.osgimanagement.internal.IHelpContextIds;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.FrameworkPanel.ErrorMonitor;

public final class FrameworkPropertyPage extends PropertyPage implements ErrorMonitor {
  private FrameworkPanel fwPanel;

  /* (non-Javadoc)
   * @see org.eclipse.jface.preference.PreferencePage#performOk()
   */
  @Override
  public boolean performOk() {
    boolean correct = fwPanel.validate();
    if (correct) {
      final FrameworkImpl fw = (FrameworkImpl) getElement();
      String oldLabel = fw.getLabel();

      boolean connChanged = fwPanel.save(fw.getConfig());
      fw.setName(fw.getConfig().getString(FrameworkImpl.FRAMEWORK_NAME));

      updateTitle(oldLabel, fw.getLabel());

      DeviceConnector connector = fw.getConnector();
      if (connector != null) {
        if (fw.isConnected() && connChanged) {
          MessageDialog.openInformation(getShell(), Messages.framework_ip_changed_title,
              Messages.framework_ip_changed_message);
        }
      }
      fw.updateElement();
    }

    return correct;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createContents(Composite parent) {
    PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.PROPERTY_FRAMEWORK);

    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));

    final FrameworkImpl fw = (FrameworkImpl) getElement();
    fwPanel = new FrameworkPanel(composite, fw, fw.getParent(), GridData.FILL_HORIZONTAL);
    fwPanel.setErrorMonitor(this);
    fwPanel.initialize(fw.getConfig());

    return composite;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
   */
  @Override
  protected void performDefaults() {
    final FrameworkImpl fw = (FrameworkImpl) getElement();
    fwPanel.initialize(fw.getConfig());
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.preference.PreferencePage#setErrorMessage(java.lang.String)
   */
  @Override
  public void setErrorMessage(String error) {
    super.setErrorMessage(error);
    setValid(error == null);
  }

  private void updateTitle(String oldName, String newName) {
    // there is no other way to update PropertyDialog's title
    String title = getShell().getText();
    title = title.replace(oldName, newName);
    getShell().setText(title);
  }
}
