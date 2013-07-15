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

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.tigris.mtoolkit.osgimanagement.application.logic.StartApplicationOperation;
import org.tigris.mtoolkit.osgimanagement.application.model.Application;
import org.tigris.mtoolkit.osgimanagement.model.AbstractFrameworkTreeElementAction;

public final class StartApplicationAction extends AbstractFrameworkTreeElementAction<Application> {
  public StartApplicationAction(ISelectionProvider provider, String label) {
    super(true, Application.class, provider, label);
    updateState((IStructuredSelection) provider.getSelection());
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.model.AbstractFrameworkTreeElementAction#execute(org.tigris.mtoolkit.osgimanagement.model.Model)
   */
  @Override
  protected void execute(Application element) {
    Job job = new StartApplicationOperation(element);
    job.schedule();
  }
}
