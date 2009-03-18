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
package org.tigris.mtoolkit.osgimanagement.internal.browser.properties.logic;

import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.InstallDialog;

public class InstallDialogLogic implements SelectionListener, ConstantsDistributor, ModifyListener {

  private InstallDialog target;

  public InstallDialogLogic (InstallDialog obj) {
    this.target = obj;
  }
  
  public void widgetDefaultSelected(SelectionEvent event) {
  }

  public void widgetSelected(SelectionEvent event) {
    if (event.getSource() instanceof Button) {
      String buttonText = ((Button)event.getSource()).getText();

      // Browse
      if (buttonText.equals(Messages.browse_button_label)) {
        target.startLocationChooser();
      }
      // OK
      if (buttonText.equals(Messages.ok_button_label)) {
        target.closeOK();
        target.close();
      }
      // Cancel
      if (buttonText.equals(Messages.cancel_button_label)) {
        target.close();
      }
    }
  }

  public void modifyText(ModifyEvent e) {
    target.updateButtons();
  }
}