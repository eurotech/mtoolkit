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
package org.tigris.mtoolkit.dpeditor.osgimanagement.dp.logic;

import org.eclipse.core.runtime.IStatus;
import org.tigris.mtoolkit.dpeditor.DPActivator;
import org.tigris.mtoolkit.dpeditor.osgimanagement.dp.model.DeploymentPackage;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.model.AbstractRemoteModelOperation;

public abstract class RemoteDeploymentOperation extends AbstractRemoteModelOperation<DeploymentPackage> {
  public RemoteDeploymentOperation(String name, DeploymentPackage pack) {
    super(name, pack);
    setRule(new DPOperationSchedulingRule(pack.findFramework()));
  }

  protected DeploymentPackage getDeploymentPackage() {
    return getModel();
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.model.AbstractRemoteModelOperation#handleException(org.tigris.mtoolkit.iagent.IAgentException)
   */
  @Override
  protected IStatus handleException(IAgentException e) {
    return DPActivator.newStatus(IStatus.ERROR, e.getMessage(), e);
  }
}
