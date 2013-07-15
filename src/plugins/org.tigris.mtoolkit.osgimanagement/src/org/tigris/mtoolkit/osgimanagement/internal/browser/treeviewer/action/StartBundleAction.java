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
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.model.AbstractFrameworkTreeElementAction;

public final class StartBundleAction extends AbstractFrameworkTreeElementAction<Bundle> {
  private static final int START_BUNDLE_STATE_MASK = org.osgi.framework.Bundle.INSTALLED
                                                       | org.osgi.framework.Bundle.RESOLVED
                                                       | org.osgi.framework.Bundle.STARTING
                                                       | org.osgi.framework.Bundle.ACTIVE;

  public StartBundleAction(ISelectionProvider provider, String label) {
    super(true, Bundle.class, provider, label);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.AbstractFrameworkTreeElementAction#isEnabledFor(org.tigris.mtoolkit.osgimanagement.model.Model)
   */
  @Override
  protected boolean isEnabledFor(Bundle element) {
    Bundle bundle = element;
    if (bundle.getType() != 0 || bundle.isSystemBundle()) {
      return false;
    }
    return (bundle.getState() & START_BUNDLE_STATE_MASK) != 0;
  }

  /* (non-Javadoc)
  * @see org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.AbstractFrameworkTreeElementAction#execute(org.tigris.mtoolkit.osgimanagement.model.Model)
  */
  @Override
  protected void execute(Bundle element) {
    ActionsManager.startBundleAction(element);
  }
}
