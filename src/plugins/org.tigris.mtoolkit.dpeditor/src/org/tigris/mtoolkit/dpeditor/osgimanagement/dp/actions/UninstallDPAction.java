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
package org.tigris.mtoolkit.dpeditor.osgimanagement.dp.actions;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.common.PluginUtilities;
import org.tigris.mtoolkit.dpeditor.osgimanagement.dp.logic.UninstallDeploymentOperation;
import org.tigris.mtoolkit.dpeditor.osgimanagement.dp.model.DeploymentPackage;
import org.tigris.mtoolkit.osgimanagement.model.AbstractFrameworkTreeElementAction;

public final class UninstallDPAction extends AbstractFrameworkTreeElementAction<DeploymentPackage> {
  public UninstallDPAction(ISelectionProvider provider, String label) {
    super(true, DeploymentPackage.class, provider, label);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.model.AbstractFrameworkTreeElementAction#execute(org.tigris.mtoolkit.osgimanagement.model.Model)
   */
  @Override
  protected void execute(DeploymentPackage element) {
    Job job = new UninstallDeploymentOperation(element);
    job.schedule();
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.model.AbstractFrameworkTreeElementAction#run()
   */
  @Override
  public void run() {
    final int result[] = new int[1];
    Display display = PlatformUI.getWorkbench().getDisplay();
    if (display.isDisposed()) {
      return;
    }
    display.syncExec(new Runnable() {
      /* (non-Javadoc)
       * @see java.lang.Runnable#run()
       */
      public void run() {
        int count = getStructuredSelection().size();
        String msg = "Are you sure you want to uninstall ";
        if (count == 1) {
          msg += getStructuredSelection().getFirstElement() + "?";
        } else {
          msg += count + " selected resources?";
        }
        result[0] = PluginUtilities.showConfirmationDialog(PluginUtilities.getActiveWorkbenchShell(),
            "Confirm uninstall", msg);
      }
    });
    if (result[0] == SWT.OK) {
      super.run();
    }
  }
}
