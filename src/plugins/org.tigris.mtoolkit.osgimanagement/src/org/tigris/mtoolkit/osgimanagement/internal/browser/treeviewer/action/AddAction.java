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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;


public class AddAction extends SelectionProviderAction {

  private TreeViewer parentView;
  
  public AddAction(ISelectionProvider provider, String label) {
    super(provider, label);
    this.parentView = (TreeViewer)provider;
  }

  // run method
  public void run() {
    MenuFactory.addFrameworkAction(FrameWorkView.getTreeRoot(), parentView);
    getSelectionProvider().setSelection(getSelection());
  }

  // override to react properly to selection change
  public void selectionChanged(IStructuredSelection selection) {
    this.setEnabled(true);
  }
}