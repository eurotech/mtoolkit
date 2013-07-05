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
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.console.ConsoleManager;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.model.Framework;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public final class ShowFrameworkConsole extends SelectionProviderAction implements IStateAction {
  private final TreeViewer tree;

  public ShowFrameworkConsole(ISelectionProvider provider, String label, TreeViewer tree) {
    super(provider, label);
    this.tree = tree;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    IStructuredSelection selection = (IStructuredSelection) tree.getSelection();
    if (selection.isEmpty()) {
      return; // don't execute for empty selection
    }
    final Framework fw = ((Model) selection.getFirstElement()).findFramework();
    if (fw == null) {
      return; // don't execute when fw has been disconnected
    }
    ConsoleManager.showConsole(fw.getConnector(), fw.getName());
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.actions.SelectionProviderAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
   */
  @Override
  public void selectionChanged(IStructuredSelection selection) {
    updateState(selection);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.IStateAction#updateState(org.eclipse.jface.viewers.IStructuredSelection)
   */
  public void updateState(IStructuredSelection selection) {
    boolean enabled = false;
    Framework fw = null;
    for (Iterator it = selection.iterator(); it.hasNext();) {
      Framework next = ((Model) it.next()).findFramework();
      if (fw == null) {
        // we found a framework, enable console
        fw = next;
        enabled = true;
      } else if (!fw.equals(next)) {
        // we have another framework, disable console button
        enabled = false;
        break;
      }
    }
    setEnabled(enabled && fw != null && fw.isConnected());
  }
}
