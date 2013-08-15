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
import java.util.Hashtable;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.common.PluginUtilities;
import org.tigris.mtoolkit.common.gui.PropertiesDialog;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.osgimanagement.Util;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.IHelpContextIds;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.model.AbstractFrameworkTreeElementAction;

public final class BundlePropertiesAction extends AbstractFrameworkTreeElementAction<Bundle> {
  private static final int BUNDLE_PROPERTIES_STATE_MASK = org.osgi.framework.Bundle.INSTALLED
                                                            | org.osgi.framework.Bundle.RESOLVED
                                                            | org.osgi.framework.Bundle.STARTING
                                                            | org.osgi.framework.Bundle.ACTIVE
                                                            | org.osgi.framework.Bundle.STOPPING;

  public BundlePropertiesAction(ISelectionProvider provider, String label) {
    super(false, Bundle.class, provider, label);
  }

  /* (non-Javadoc)
  * @see org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.AbstractFrameworkTreeElementAction#execute(org.tigris.mtoolkit.osgimanagement.model.Model)
  */
  @Override
  protected void execute(final Bundle bundle) {
    final Dictionary[] headers = new Dictionary[2];
    Job job = new Job("Retrieving bundle properties...") {
      /* (non-Javadoc)
       * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
       */
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          RemoteBundle rBundle = bundle.getRemoteBundle();
          headers[0] = rBundle.getHeaders(null);
          if (bundle.isSigned()) {
            headers[1] = rBundle.getSignerCertificates();
          }
        } catch (IAgentException e) {
          return Util.newStatus(IStatus.ERROR, "Failed to get bundle headers", e);
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
            FrameworkPlugin.processError(result.getException(), "Failed to get bundle headers", true);
          }
          return;
        }
        if (headers[0] == null) {
          FrameworkPlugin.processError("The bundle has been uninstalled.", true);
          return;
        }
        Display display = PlatformUI.getWorkbench().getDisplay();
        if (display.isDisposed()) {
          return;
        }
        display.asyncExec(new Runnable() {
          /* (non-Javadoc)
           * @see java.lang.Runnable#run()
           */
          public void run() {
            Shell shell = PluginUtilities.getActiveWorkbenchShell();
            PropertiesDialog propertiesDialog;
            if (headers[1] == null) {
              propertiesDialog = new PropertiesDialog(shell, Messages.bundle_properties_title) {
                /* (non-Javadoc)
                 * @see org.tigris.mtoolkit.common.gui.PropertiesDialog#attachHelp(org.eclipse.swt.widgets.Composite)
                 */
                @Override
                protected void attachHelp(Composite container) {
                  PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.PROPERTY_BUNDLE);
                }
              };
              propertiesDialog.create();
              propertiesDialog.getMainControl().setData(headers[0]);
            } else {
              propertiesDialog = new PropertiesDialog(shell, Messages.bundle_properties_title) {
                @Override
                protected void attachHelp(Composite container) {
                  PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.PROPERTY_BUNDLE);
                }

                @Override
                public Control createDialogArea(Composite parent) {
                  parent.setLayout(new GridLayout());
                  GridData mGD = new GridData(GridData.FILL_BOTH);
                  mGD.heightHint = 500;
                  mGD.widthHint = 800;
                  parent.setLayoutData(mGD);
                  BundlePropertiesPage page = new BundlePropertiesPage();
                  Dictionary props = new Hashtable(2);
                  props.put(BundlePropertiesPage.HEADERS_KEY, headers[0]);
                  props.put(BundlePropertiesPage.CERTIFICATES_KEY, headers[1]);
                  setMainControl(parent, page, props);
                  attachHelp(parent);
                  return parent;
                }
              };
              propertiesDialog.create();
            }
            propertiesDialog.open();
          }
        });
      }
    });
    job.schedule();
  }

  /* (non-Javadoc)
  * @see org.tigris.mtoolkit.osgimanagement.model.AbstractFrameworkTreeElementAction#isEnabledFor(org.tigris.mtoolkit.osgimanagement.model.Model)
  */
  @Override
  protected boolean isEnabledFor(Bundle bundle) {
    return (bundle.getState() & BUNDLE_PROPERTIES_STATE_MASK) != 0;
  }
}
