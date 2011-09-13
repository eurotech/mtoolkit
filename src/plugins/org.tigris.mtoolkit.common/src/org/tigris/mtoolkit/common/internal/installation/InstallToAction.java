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
package org.tigris.mtoolkit.common.internal.installation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.common.installation.InstallationItemProcessor;
import org.tigris.mtoolkit.common.installation.InstallationItemProvider;
import org.tigris.mtoolkit.common.installation.InstallationTarget;
import org.tigris.mtoolkit.common.installation.ProviderSelectionDialog;

public class InstallToAction extends Action {
  private InstallationItemProcessor processor;
  private InstallationTarget target;
  private List items;

  public InstallToAction(InstallationItemProcessor processor, InstallationTarget target, List/*<Mapping>*/items) {
    super(target.getName());
    this.processor = processor;
    this.target = target;
    this.items = items;
  }

  public static class Mapping {
    public Object resource;
    public Map providerSpecificItems;

    public Mapping(Object resource, Map providerSpecificItems) {
      this.resource = resource;
      this.providerSpecificItems = providerSpecificItems;
    }
  }

  public void run() {
    Job job = new Job("Installing to " + target.getName()) {
      public IStatus run(IProgressMonitor monitor) {
        InstallationHistory.getDefault().promoteHistory(target, processor);
        InstallationHistory.getDefault().saveHistory();

        Iterator iterator = items.iterator();
        List instItems = new ArrayList();
        while (iterator.hasNext()) {
          Mapping mapping = (Mapping) iterator.next();
          InstallationItem item = selectInstallationItem(mapping.resource, mapping.providerSpecificItems);
          if (item == null) {
            return Status.CANCEL_STATUS;
          }

          instItems.add(item);
          if (monitor.isCanceled()) {
            return Status.CANCEL_STATUS;
          }
        }
        IStatus status = processor.processInstallationItems(
            (InstallationItem[]) instItems.toArray(new InstallationItem[instItems.size()]), target, monitor);

        monitor.done();
        if (monitor.isCanceled()) {
          return Status.CANCEL_STATUS;
        }
        return status;
      }
    };
    job.schedule();
  }

  private InstallationItem selectInstallationItem(final Object resource, final Map items) {
    final List suitableProviders = new ArrayList();
    String[] supported = processor.getSupportedMimeTypes();
    for (Iterator it = items.keySet().iterator(); it.hasNext();) {
      InstallationItemProvider provider = (InstallationItemProvider) it.next();
      InstallationItem item = (InstallationItem) items.get(provider);
      String itemMimeType = item.getMimeType();
      for (int i = 0; i < supported.length; i++) {
        if (supported[i].equals(itemMimeType)) {
          suitableProviders.add(provider);
          break;
        }
      }
    }
    if (suitableProviders.size() == 0) {
      // should not happen, unsupported items for this processor are filtered
      throw new IllegalArgumentException("Pocessor '" + processor.getGeneralTargetName()
          + "' is not capable of installing specified item");
    }
    if (suitableProviders.size() == 1) {
      return (InstallationItem) items.get(suitableProviders.get(0));
    }

    final InstallationItemProvider[] selected = new InstallationItemProvider[1];
    PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
      public void run() {
        Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
        String resourceName = getResourceName(resource);
        ProviderSelectionDialog dialog = new ProviderSelectionDialog(shell, suitableProviders, resourceName);
        dialog.open();
        selected[0] = dialog.getSelectedProvider();
      }
    });

    if (selected[0] != null) {
      return (InstallationItem) items.get(selected[0]);
    } else {
      return null;
    }
  }

  private String getResourceName(Object resource) {
    if (resource instanceof IProject) {
      return ((IProject) resource).getName();
    } else if (resource instanceof IAdaptable) {
      IProject project = (IProject) ((IAdaptable) resource).getAdapter(IProject.class);
      if (project != null) {
        return project.getName();
      }
    }
    return resource.toString();
  }
}
