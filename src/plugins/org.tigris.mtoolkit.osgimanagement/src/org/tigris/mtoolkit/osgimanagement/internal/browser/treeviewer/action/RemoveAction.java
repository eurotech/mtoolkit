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
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Model;


public class RemoveAction extends SelectionProviderAction {

  public RemoveAction(ISelectionProvider provider, String label) {
    super(provider, label);
  }


  // run method
  public void run() {
    Iterator iterator = getStructuredSelection().iterator();
    while (iterator.hasNext()) {
      FrameWork framework = (FrameWork)iterator.next();
      if (framework.isConnected()) {
        MenuFactory.disconnectFrameworkAction(framework);
      }
      MenuFactory.removeFrameworkAction(framework);
    }
  }

  // override to react properly to selection change
  public void selectionChanged(IStructuredSelection selection) {
    updateState(selection);
  }
  
  public void updateState(IStructuredSelection selection) {
    if (selection.size() == 0) {
      setEnabled(false);
      return;
    }
    boolean enabled = true;
    
    Iterator iterator = selection.iterator();
    while (iterator.hasNext()) {
      Model model = (Model)iterator.next();
      if (!(model instanceof FrameWork)) {
        enabled = false;
        break;
      }
      FrameWork framework = (FrameWork) model;
      if (framework.autoConnected) {
        enabled = false;
        break;
      }
    }
    this.setEnabled(enabled);
  }
}

