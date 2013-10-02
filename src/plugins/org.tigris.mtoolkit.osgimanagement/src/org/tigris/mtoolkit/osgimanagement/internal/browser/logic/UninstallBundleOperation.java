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
package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;

public final class UninstallBundleOperation extends RemoteBundleOperation {
  public UninstallBundleOperation(Bundle bundle) {
    super(Messages.uninstall_bundle, bundle);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.internal.browser.logic.RemoteBundleOperation#doOperation(org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  protected IStatus doOperation(IProgressMonitor monitor) throws IAgentException {
    final RemoteBundle remoteBundle = getBundle().getRemoteBundle();
    remoteBundle.uninstall(null);
    remoteBundle.refreshPackages();
    return Status.OK_STATUS;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.internal.browser.logic.RemoteBundleOperation#handleException(org.tigris.mtoolkit.iagent.IAgentException)
   */
  @Override
  protected IStatus handleException(IAgentException e) {
    // ignore that a bundle is uninstalled when trying uninstalling it
    if (e.getErrorCode() == IAgentErrors.ERROR_BUNDLE_UNINSTALLED) {
      return Status.OK_STATUS;
    }
    return super.handleException(e);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.internal.browser.logic.RemoteBundleOperation#getMessage(org.eclipse.core.runtime.IStatus)
   */
  @Override
  protected String getMessage(IStatus operationStatus) {
    return NLS.bind("Bundle {0} uninstallation did not finish cleanly", getBundle().toString());
  }
}