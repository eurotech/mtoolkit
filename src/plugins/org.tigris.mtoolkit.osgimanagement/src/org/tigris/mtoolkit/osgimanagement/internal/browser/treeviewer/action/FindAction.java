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
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.SearchPane;


public class FindAction extends SelectionProviderAction {

  private SearchPane searchPanel;
  
  public FindAction(ISelectionProvider provider, SearchPane searchPanel, String label) {
    super(provider, label);
    this.searchPanel = searchPanel;
  }

  // run method
  public void run() {
    searchPanel.show();
  }

  // override to react properly to selection change
  public void selectionChanged(IStructuredSelection selection) {
  }
}