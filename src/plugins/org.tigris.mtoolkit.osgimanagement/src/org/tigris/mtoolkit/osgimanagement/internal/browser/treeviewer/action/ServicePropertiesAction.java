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

import java.util.Dictionary;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.common.gui.PropertiesDialog;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteService;
import org.tigris.mtoolkit.osgimanagement.Util;
import org.tigris.mtoolkit.osgimanagement.internal.IHelpContextIds;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ObjectClass;
import org.tigris.mtoolkit.osgimanagement.model.AbstractFrameworkTreeElementAction;

public final class ServicePropertiesAction extends AbstractFrameworkTreeElementAction<ObjectClass> {
  public ServicePropertiesAction(ISelectionProvider provider, String text) {
    super(false, ObjectClass.class, provider, text);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.AbstractFrameworkTreeElementAction#execute(org.tigris.mtoolkit.osgimanagement.model.Model)
   */
  @Override
  protected void execute(ObjectClass object) {
    final RemoteService service = object.getService();
    if (service == null) {
      return;
    }
    final Dictionary[] properties = new Dictionary[1];
    Job job = new Job("Retrieving service properties...") {
      /* (non-Javadoc)
       * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
       */
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          properties[0] = service.getProperties();
        } catch (IAgentException e) {
          return Util.newStatus(IStatus.ERROR, "Failed to get service properties", e);
        }
        return Status.OK_STATUS;
      }
    };
    job.addJobChangeListener(new JobChangeAdapter() {
      /* (non-Javadoc)
       * @see org.eclipse.core.runtime.jobs.JobChangeAdapter#done(org.eclipse.core.runtime.jobs.IJobChangeEvent)
       */
      @Override
      public void done(IJobChangeEvent event) {
        IStatus result = event.getResult();
        if (!result.isOK()) {
          if (result.getException() != null) {
            BrowserErrorHandler.processError(result.getException(), "Failed to get service properties", true);
          }
          return;
        }
        if (properties[0] == null) {
          BrowserErrorHandler.processError("Empty service properties returned.", true);
          return;
        }
        final Display display = PlatformUI.getWorkbench().getDisplay();
        if (display.isDisposed()) {
          return;
        }
        display.asyncExec(new Runnable() {
          /* (non-Javadoc)
           * @see java.lang.Runnable#run()
           */
          public void run() {
            String tableHeader = null;
            try {
              tableHeader = "Service " + service.getServiceId();
            } catch (IAgentException e1) {
            }
            PropertiesDialog dialog = new PropertiesDialog(display.getActiveShell(), Messages.service_properties_title,
                tableHeader) {
              /* (non-Javadoc)
               * @see org.tigris.mtoolkit.common.gui.PropertiesDialog#attachHelp(org.eclipse.swt.widgets.Composite)
               */
              @Override
              protected void attachHelp(Composite container) {
                PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.PROPERTY_SERVICE);
              }
            };
            dialog.create();
            dialog.getMainControl().setData(properties[0]);
            dialog.open();
          }
        });
      }
    });
    job.schedule();
  }
}
