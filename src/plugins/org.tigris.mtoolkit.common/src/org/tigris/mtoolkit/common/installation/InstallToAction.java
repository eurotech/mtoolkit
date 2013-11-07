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
package org.tigris.mtoolkit.common.installation;

import java.util.ArrayList;
import java.util.Collections;
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
import org.eclipse.ui.progress.IProgressConstants;

public final class InstallToAction extends Action {
  private final Map<String, Object>       args;
  private final List<Mapping>             items;
  private final InstallationTarget        target;
  private final InstallationItemProcessor processor;

  public InstallToAction(InstallationItemProcessor processor, Map<String, Object> args, InstallationTarget target,
      List<Mapping> items) {
    super(target.getName());
    this.processor = processor;
    this.args = args == null ? Collections.EMPTY_MAP : args;
    this.target = target;
    this.items = items;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    // Save dirty editors if possible but do not stop if not all are saved
    boolean save = PlatformUI.getWorkbench().saveAllEditors(true);
    if (!save) {
      return;
    }
    Job job = new Job("Installing to " + target.getName()) {
      /* (non-Javadoc)
       * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
       */
      @Override
      public IStatus run(IProgressMonitor monitor) {
        InstallationHistory.getDefault().promoteHistory(target, processor);
        InstallationHistory.getDefault().saveHistory();

        List<InstallationItem> instItems = new ArrayList<InstallationItem>();
        for (Mapping mapping : items) {
          InstallationItem item = selectInstallationItem(mapping.resource, mapping.providerSpecificItems);
          if (item == null) {
            return Status.CANCEL_STATUS;
          }
          instItems.add(item);
          if (monitor.isCanceled()) {
            return Status.CANCEL_STATUS;
          }
        }
        InstallationItem[] items = instItems.toArray(new InstallationItem[instItems.size()]);
        IStatus status = processor.processInstallationItems(items, args, target, monitor);
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

  private InstallationItem selectInstallationItem(final Object resource,
      final Map<InstallationItemProvider, InstallationItem> items) {
    final List<InstallationItemProvider> suitableProviders = new ArrayList<InstallationItemProvider>();
    String[] supported = processor.getSupportedMimeTypes();
    for (InstallationItemProvider provider : items.keySet()) {
      InstallationItem item = items.get(provider);
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
      return items.get(suitableProviders.get(0));
    }

    final InstallationItemProvider[] selected = new InstallationItemProvider[1];
    PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
      /* (non-Javadoc)
       * @see java.lang.Runnable#run()
       */
      public void run() {
        Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
        String resourceName = getResourceName(resource);
        ProviderSelectionDialog dialog = new ProviderSelectionDialog(shell, suitableProviders, resourceName);
        dialog.open();
        selected[0] = dialog.getSelectedProvider();
      }
    });
    if (selected[0] != null) {
      return items.get(selected[0]);
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

  public static class Mapping {
    public Object                                          resource;
    public Map<InstallationItemProvider, InstallationItem> providerSpecificItems;

    public Mapping(Object resource, Map<InstallationItemProvider, InstallationItem> providerSpecificItems) {
      this.resource = resource;
      this.providerSpecificItems = providerSpecificItems;
    }
  }

}
