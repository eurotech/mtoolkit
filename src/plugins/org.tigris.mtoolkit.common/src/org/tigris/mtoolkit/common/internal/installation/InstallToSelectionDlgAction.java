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
package org.tigris.mtoolkit.common.internal.installation;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.IShellProvider;
import org.tigris.mtoolkit.common.installation.InstallationItemProcessor;
import org.tigris.mtoolkit.common.installation.InstallationRegistry;
import org.tigris.mtoolkit.common.installation.InstallationTarget;
import org.tigris.mtoolkit.common.installation.TargetSelectionDialog;

public final class InstallToSelectionDlgAction extends Action {
  private List items;
  private InstallationItemProcessor processor;
  private IShellProvider parentShell;

  public InstallToSelectionDlgAction(InstallationItemProcessor processor, List items, IShellProvider parentShell) {
    super("Select " + processor.getGeneralTargetName() + "...");
    this.processor = processor;
    this.items = items;
    this.parentShell = parentShell;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.action.Action#run()
   */
  public void run() {
    InstallationRegistry registry = InstallationRegistry.getInstance();
    TargetSelectionDialog dialog = registry.getSelectionDialog(processor);
    InstallationTarget selectedTarget = dialog.getSelectedTarget(parentShell.getShell());
    if (selectedTarget != null) {
      Action installTo = new InstallToAction(processor, selectedTarget, items);
      installTo.run();
    }
  }
}
