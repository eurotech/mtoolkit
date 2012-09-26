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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.common.gui.PropertiesDialog;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.Util;
import org.tigris.mtoolkit.osgimanagement.internal.IHelpContextIds;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;

public class BundlePropertiesAction extends SelectionProviderAction implements IStateAction {

  private TreeViewer parentView;

  public BundlePropertiesAction(ISelectionProvider provider, String label) {
    super(provider, label);
    this.parentView = (TreeViewer) provider;
    this.setText(label + "@Alt+Enter");
  }

  // run method
  @Override
  public void run() {
    final Bundle bundle = (Bundle) getStructuredSelection().getFirstElement();
    final Dictionary[] headers = new Dictionary[1];
    Job job = new Job("Retrieving bundle properties...") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          RemoteBundle rBundle = bundle.getRemoteBundle();
          headers[0] = rBundle.getHeaders(null);
        } catch (IAgentException e) {
          return Util.newStatus(IStatus.ERROR, "Failed to get bundle headers", e);
        }
        return Status.OK_STATUS;
      }
    };
    job.addJobChangeListener(new JobChangeAdapter() {
      @Override
      public void done(IJobChangeEvent event) {
        IStatus result = event.getResult();
        if (!result.isOK()) {
          if (result.getException() != null) {
            BrowserErrorHandler.processError(result.getException(), "Failed to get bundle headers", true);
          }
          return;
        }
        if (headers[0] == null) {
          BrowserErrorHandler.processError("The bundle has been uninstalled.", true);
          return;
        }
        Display display = PlatformUI.getWorkbench().getDisplay();
        if (display.isDisposed()) {
          return;
        }
        display.asyncExec(new Runnable() {
          public void run() {
            if (parentView.getTree().isDisposed()) {
              return;
            }
            Shell shell = parentView.getTree().getShell();
            PropertiesDialog propertiesDialog = new PropertiesDialog(shell, Messages.bundle_properties_title) {
              @Override
              protected void attachHelp(Composite container) {
                PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.PROPERTY_BUNDLE);
              }
            };
            propertiesDialog.create();
            propertiesDialog.getMainControl().setData(headers[0]);
            propertiesDialog.open();
          }
        });
      }
    });
    job.schedule();
    // needed to update workbench menu and toolbar status
    getSelectionProvider().setSelection(getSelection());
  }

  // override to react properly to selection change
  @Override
  public void selectionChanged(IStructuredSelection selection) {
    updateState(selection);
  }

  public void updateState(IStructuredSelection selection) {
    if (selection.size() == 1
        && getStructuredSelection().getFirstElement() instanceof Bundle
        && (((Bundle) getStructuredSelection().getFirstElement()).getState() & (org.osgi.framework.Bundle.INSTALLED
            | org.osgi.framework.Bundle.RESOLVED | org.osgi.framework.Bundle.STARTING
            | org.osgi.framework.Bundle.ACTIVE | org.osgi.framework.Bundle.STOPPING)) != 0) {
      this.setEnabled(true);
    } else {
      this.setEnabled(false);
    }
  }
}
