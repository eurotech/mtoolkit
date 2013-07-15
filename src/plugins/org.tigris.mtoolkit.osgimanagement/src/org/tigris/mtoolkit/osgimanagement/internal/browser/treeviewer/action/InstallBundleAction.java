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

import java.util.Iterator;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.model.AbstractFrameworkTreeAction;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public final class InstallBundleAction extends AbstractFrameworkTreeAction {
  public InstallBundleAction(ISelectionProvider provider, String label) {
    super(provider, label);
  }


  /* (non-Javadoc)
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    Model node = (Model) getStructuredSelection().getFirstElement();
    FrameworkImpl framework = (FrameworkImpl) node.findFramework();
    ActionsManager.installBundleAction(framework);
    getSelectionProvider().setSelection(getSelection());
  }


  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.model.AbstractFrameworkTreeAction#updateState(org.eclipse.jface.viewers.IStructuredSelection)
   */
  @Override
  public void updateState(IStructuredSelection selection) {
    if (selection.size() == 0) {
      setEnabled(false);
      return;
    }
    FrameworkImpl fw = (FrameworkImpl) ((Model) selection.getFirstElement()).findFramework();
    boolean enabled = true;
    if (fw == null || !fw.isConnected()) {
      enabled = false;
    } else {
      Iterator iterator = selection.iterator();
      while (iterator.hasNext()) {
        Model model = (Model) iterator.next();
        if (model.findFramework() != fw) {
          enabled = false;
          break;
        }
      }
    }
    setEnabled(enabled);
  }
}
