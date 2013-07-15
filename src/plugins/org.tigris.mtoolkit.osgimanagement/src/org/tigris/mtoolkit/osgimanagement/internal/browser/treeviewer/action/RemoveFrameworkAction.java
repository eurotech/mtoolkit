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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.actions.ActionFactory;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworksView;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.model.AbstractFrameworkTreeElementAction;

public final class RemoveFrameworkAction extends AbstractFrameworkTreeElementAction<FrameworkImpl> {
  public RemoveFrameworkAction(ISelectionProvider provider, String label) {
    super(true, FrameworkImpl.class, provider, label);
    setActionDefinitionId(ActionFactory.DELETE.getCommandId());
  }


    /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.model.AbstractFrameworkTreeElementAction#execute(org.tigris.mtoolkit.osgimanagement.model.Model)
   */
  @Override
  protected void execute(FrameworkImpl framework) {
    if (framework.isConnected()) {
      ActionsManager.disconnectFrameworkAction(framework);
    }
    ActionsManager.removeFrameworkAction(framework);
  }


    /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.model.AbstractFrameworkTreeElementAction#isEnabledFor(org.tigris.mtoolkit.osgimanagement.model.Model)
   */
  @Override
  protected boolean isEnabledFor(FrameworkImpl framework) {
    return !framework.isAutoConnected();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    int count = getStructuredSelection().size();
    String msg = "Are you sure you want to remove ";
    if (count == 1) {
      msg += getStructuredSelection().getFirstElement() + "?";
    } else {
      msg += count + " selected frameworks?";
    }
    if (MessageDialog.openQuestion(FrameworksView.getShell(), "Remove Framework", msg)) {
      super.run();
    }
  }
}
