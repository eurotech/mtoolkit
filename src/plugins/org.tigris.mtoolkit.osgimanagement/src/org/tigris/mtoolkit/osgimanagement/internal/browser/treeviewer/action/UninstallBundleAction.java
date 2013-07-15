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
package org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.common.PluginUtilities;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworksView;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.model.AbstractFrameworkTreeElementAction;

public final class UninstallBundleAction extends AbstractFrameworkTreeElementAction<Bundle> {
  private static final int UNINSTALL_BUNDLE_STATE_MASK = org.osgi.framework.Bundle.INSTALLED
                                                           | org.osgi.framework.Bundle.RESOLVED
                                                           | org.osgi.framework.Bundle.STARTING
                                                           | org.osgi.framework.Bundle.STOPPING
                                                           | org.osgi.framework.Bundle.ACTIVE;

  public UninstallBundleAction(ISelectionProvider provider, String label) {
    super(true, Bundle.class, provider, label);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.AbstractFrameworkTreeElementAction#execute(org.tigris.mtoolkit.osgimanagement.model.Model)
   */
  @Override
  protected void execute(Bundle element) {
    ActionsManager.deinstallBundleAction(element);
  }

  /* (non-Javadoc)
  * @see org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.AbstractFrameworkTreeElementAction#isEnabledFor(org.tigris.mtoolkit.osgimanagement.model.Model)
  */
  @Override
  protected boolean isEnabledFor(Bundle element) {
    Bundle bundle = element;
    if (bundle.isSystemBundle()) {
      return false;
    }
    return (bundle.getState() & UNINSTALL_BUNDLE_STATE_MASK) != 0;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.AbstractFrameworkTreeElementAction#run()
   */
  @Override
  public void run() {
    final int result[] = new int[1];
    PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
      /* (non-Javadoc)
       * @see java.lang.Runnable#run()
       */
      public void run() {
        int count = getStructuredSelection().size();
        String msg = "Are you sure you want to uninstall ";
        if (count == 1) {
          msg += getStructuredSelection().getFirstElement() + "?";
        } else {
          msg += count + " selected bundles?";
        }
        result[0] = PluginUtilities.showConfirmationDialog(FrameworksView.getShell(), "Confirm uninstall", msg);
      }
    });
    if (result[0] == SWT.OK) {
      super.run();
    }
  }
}
