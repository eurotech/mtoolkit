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
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConnectFrameworkJob;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.model.AbstractFrameworkTreeElementAction;

public final class ConnectFrameworkAction extends AbstractFrameworkTreeElementAction<FrameworkImpl> {
  public ConnectFrameworkAction(ISelectionProvider provider, String label) {
    super(true, FrameworkImpl.class, provider, label);
  }

    /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.AbstractFrameworkTreeElementAction#execute(org.tigris.mtoolkit.osgimanagement.model.Model)
   */
  @Override
  protected void execute(FrameworkImpl element) {
    ActionsManager.connectFrameworkAction(element);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.AbstractFrameworkTreeElementAction#isEnabled(org.tigris.mtoolkit.osgimanagement.model.Model)
   */
  @Override
  protected boolean isEnabledFor(FrameworkImpl framework) {
    if (framework.isConnected() || framework.isAutoConnected() || framework.isConnecting()
        || ConnectFrameworkJob.isConnecting(framework)) {
      return false;
    }
    return true;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    setEnabled(false);
    super.run();
  }
}
