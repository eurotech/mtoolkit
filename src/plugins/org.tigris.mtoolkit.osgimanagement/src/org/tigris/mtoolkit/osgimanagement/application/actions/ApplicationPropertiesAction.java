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
package org.tigris.mtoolkit.osgimanagement.application.actions;

import java.util.Map;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.common.gui.PropertiesDialog;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.application.model.Application;

public final class ApplicationPropertiesAction extends SelectionProviderAction implements IStateAction {
  private TreeViewer parentView;

  public ApplicationPropertiesAction(ISelectionProvider provider, String label) {
    super(provider, label);
    this.parentView = (TreeViewer) provider;
    setActionDefinitionId(ActionFactory.PROPERTIES.getCommandId());
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    Application application = (Application) getStructuredSelection().getFirstElement();
    try {
      Map headers = application.getRemoteApplication().getProperties();
      Shell shell = parentView.getTree().getShell();
      PropertiesDialog propertiesDialog = new PropertiesDialog(shell, "Application Properties") {
        /* (non-Javadoc)
         * @see org.tigris.mtoolkit.common.gui.PropertiesDialog#attachHelp(org.eclipse.swt.widgets.Composite)
         */
        @Override
        protected void attachHelp(Composite container) {
        }
      };

      propertiesDialog.create();
      propertiesDialog.getMainControl().setData(headers);
      propertiesDialog.open();

      // needed to update workbench menu and toolbar status
      getSelectionProvider().setSelection(getSelection());
    } catch (IAgentException e) {
      e.printStackTrace();
    }

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
    if (selection.size() == 1 && getStructuredSelection().getFirstElement() instanceof Application) {
      this.setEnabled(true);
    } else {
      this.setEnabled(false);
    }
  }
}
