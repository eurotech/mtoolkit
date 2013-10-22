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
package org.tigris.mtoolkit.common.gui;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.plugin.NewPluginProjectWizard;
import org.tigris.mtoolkit.common.Messages;

public class OSGiBundleWizard extends NewPluginProjectWizard {

  private static final String S_OSGI_PROJECT = "osgiProject"; //$NON-NLS-1$
  private static final String S_TARGET_NAME  = "targetName"; //$NON-NLS-1$

  public OSGiBundleWizard() {
    super();
    setWindowTitle(Messages.OSGiBundleWizard_title);
  }

  @Override
  public void addPages() {
    super.addPages();
    fMainPage.setTitle(Messages.OSGiBundleWizard_MainPage_title);
    fMainPage.setDescription(Messages.OSGiBundleWizard_MainPage_desc);
    IDialogSettings settings = getDialogSettings();
    settings.put(S_TARGET_NAME, PDEUIMessages.NewProjectCreationPage_standard);
    settings.put(S_OSGI_PROJECT, true);
  }
}
