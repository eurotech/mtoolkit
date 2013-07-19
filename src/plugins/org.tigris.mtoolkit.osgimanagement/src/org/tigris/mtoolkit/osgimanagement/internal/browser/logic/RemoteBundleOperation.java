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
package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.model.AbstractRemoteModelOperation;

public abstract class RemoteBundleOperation extends AbstractRemoteModelOperation<Bundle> {
  public RemoteBundleOperation(String taskName, Bundle bundle) {
    super(taskName, bundle);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.model.AbstractRemoteModelOperation#refreshState(org.tigris.mtoolkit.osgimanagement.model.Model)
   */
  @Override
  protected void refreshState(Bundle model) {
    model.refreshStateFromRemote();
  }

  protected Bundle getBundle() {
    return getModel();
  }
}
