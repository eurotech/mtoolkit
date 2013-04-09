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
package org.tigris.mtoolkit.osgimanagement.dp.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.progress.IProgressConstants;
import org.tigris.mtoolkit.common.installation.BaseFileItem;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.common.installation.InstallationTarget;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.Util;
import org.tigris.mtoolkit.osgimanagement.dp.DPModelProvider;
import org.tigris.mtoolkit.osgimanagement.dp.logic.DPProcessor;
import org.tigris.mtoolkit.osgimanagement.installation.FrameworkProcessor;
import org.tigris.mtoolkit.osgimanagement.installation.FrameworkTarget;
import org.tigris.mtoolkit.osgimanagement.model.Framework;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public final class InstallDPAction extends SelectionProviderAction implements IStateAction {
  private static final String DP_FILTER = "*.dp"; //$NON-NLS-1$

  private TreeViewer          parentView;

  public InstallDPAction(ISelectionProvider provider, String label) {
    super(provider, label);
    this.parentView = (TreeViewer) provider;
  }

  // run method
  @Override
  public void run() {
    Model node = (Model) getStructuredSelection().getFirstElement();
    Framework framework = node.findFramework();
    installDPAction(framework, parentView);
    getSelectionProvider().setSelection(getSelection());
  }

  private void installDPAction(final Framework framework, TreeViewer parentView) {
    final File[] files = Util.openFileSelectionDialog(parentView.getControl().getShell(),
        "Select Deployment Package To Install", DP_FILTER, "Deployment Package (*.dp)", true);
    if (files == null || files.length == 0) {
      return;
    }
    final FrameworkProcessor processor = new FrameworkProcessor();
    processor.setUseAdditionalProcessors(true);
    Job job = new Job("Installing to " + framework.getName()) {
      /* (non-Javadoc)
       * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
       */
      @Override
      public IStatus run(IProgressMonitor monitor) {
        InstallationTarget target = new FrameworkTarget(framework);
        IStatus status = Status.OK_STATUS;
        List items = new ArrayList();
        for (int i = 0; i < files.length; i++) {
          InstallationItem item = new BaseFileItem(files[i], DPProcessor.MIME_DP);
          items.add(item);
          if (monitor.isCanceled()) {
            break;
          }
        }
        status = processor.processInstallationItems(
            (InstallationItem[]) items.toArray(new InstallationItem[items.size()]), null, target, monitor);

        monitor.done();
        if (monitor.isCanceled()) {
          return Status.CANCEL_STATUS;
        }
        return status;
      }
    };
    job.setUser(true);
    job.setProperty(IProgressConstants.ICON_PROPERTY, processor.getGeneralTargetImageDescriptor());
    job.schedule();
  }

  // override to react properly to selection change
  @Override
  public void selectionChanged(IStructuredSelection selection) {
    updateState(selection);
  }

  public void updateState(IStructuredSelection selection) {
    if (selection.size() == 0) {
      setEnabled(false);
      return;
    }
    Framework fw = ((Model) selection.getFirstElement()).findFramework();
    boolean enabled = true;
    if (fw == null || !fw.isConnected()) {
      enabled = false;
    } else {
      Iterator iterator = selection.iterator();
      while (iterator.hasNext()) {
        Model model = (Model) iterator.next();
        if (model.findFramework() != fw) {
          enabled = false;
          break;
        }
        DeviceConnector connector = model.findFramework().getConnector();
        if (connector == null || DPModelProvider.supportDPDictionary.get(connector) != Boolean.TRUE) {
          enabled = false;
          break;
        }
      }
    }
    setEnabled(enabled);
  }
}
