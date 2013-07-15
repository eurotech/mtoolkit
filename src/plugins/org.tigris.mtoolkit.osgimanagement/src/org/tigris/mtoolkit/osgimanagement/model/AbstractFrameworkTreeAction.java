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
package org.tigris.mtoolkit.osgimanagement.model;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.osgimanagement.IStateAction;

public abstract class AbstractFrameworkTreeAction extends SelectionProviderAction implements IStateAction {
  protected AbstractFrameworkTreeAction(ISelectionProvider provider, String text) {
    super(provider, text);
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
    setEnabled(true);
  }
}
