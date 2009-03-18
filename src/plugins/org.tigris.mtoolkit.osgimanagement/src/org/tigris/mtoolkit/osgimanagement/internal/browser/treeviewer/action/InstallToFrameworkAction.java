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

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;

public class InstallToFrameworkAction extends Action { 

  private FrameWork fw;
  
  public InstallToFrameworkAction(FrameWork fw) {
    super(fw.getName());
    this.fw = fw;
  }
  
  public void run() {
    ISelection selection = getSelectionProvider().getSelection();
    TreeSelection sel = (TreeSelection) selection;
    final List list = sel.toList();
    Job job = new Job(Messages.install_to_framework) {
      public IStatus run(IProgressMonitor monitor) {
        monitor.beginTask(Messages.install_to_framework, list.size());
        for (int i=0; i<list.size(); i++) {
          if (list.get(i) instanceof IFile) {
            final IFile file = (IFile) list.get(i);
            String path = file.getLocation().toString();
            String ext = file.getFileExtension();
            if (ext.equals("jar")) { //$NON-NLS-1$
              FrameworkConnectorFactory.installBundle(path, fw, false);
            } else if (ext.equals("dp")) { //$NON-NLS-1$
              FrameworkConnectorFactory.installDP(path, fw);
            } else {
                FrameworkConnectorFactory.installBundle(path, fw, false);
            }
          } else if (list.get(i) instanceof IProject) {
            IProject project = (IProject) list.get(i);
            try {
              String natureIDs[] = project.getProject().getDescription().getNatureIds();
              boolean plugin = false;
              for (int j=0; j<natureIDs.length; j++) {
                if ("org.eclipse.pde.PluginNature".equals(natureIDs[j])) { //$NON-NLS-1$
                  plugin = true;
                  break;
                }
              }
              if (plugin) {
                installToFramework(project.getProject());
              }
            } catch (CoreException e) {
              BrowserErrorHandler.processError(e, true);
              e.printStackTrace();
            }

          } else if (list.get(i) instanceof IJavaProject) {
            IJavaProject project = (IJavaProject) list.get(i);
            try {
              String natureIDs[] = project.getProject().getDescription().getNatureIds();
              boolean plugin = false;
              for (int j=0; j<natureIDs.length; j++) {
                if ("org.eclipse.pde.PluginNature".equals(natureIDs[j])) { //$NON-NLS-1$
                  plugin = true;
                  break;
                }
              }
              if (plugin) {
                installToFramework(project.getProject());
              }
            } catch (CoreException e) {
              BrowserErrorHandler.processError(e, true);
              e.printStackTrace();
            }
          }
          monitor.worked(1);
        }
        monitor.done();
        if (monitor.isCanceled()) return Status.CANCEL_STATUS;
        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }
  
  private void installToFramework(IProject prj) {
    FeatureExportInfo exportInfo = new FeatureExportInfo();
    exportInfo.toDirectory = true;
    exportInfo.useJarFormat = true;
    exportInfo.exportSource = false;
    exportInfo.destinationDirectory = FrameworkPlugin.getDefault().getStateLocation().toString();
    exportInfo.zipFileName = null;
    exportInfo.qualifier = null;

    String version = "1.0.0"; //$NON-NLS-1$
    IPluginModelBase model = PluginRegistry.findModel(prj);
    BundleDescription descr = model.getBundleDescription();
    if (descr != null) {
      Object ver = descr.getVersion();
      if (ver != null) {
        version = ver.toString();
      }
    }
    String name = descr.getSymbolicName();

    PluginExporter exporter = null;
    try {
    	if (PluginExporter_35.isCompatible())
    		exporter = new PluginExporter_35();
    	else if (PluginExporter_34.isCompatible())
    		exporter = new PluginExporter_34();
    } catch (Throwable e) {
    	e.printStackTrace();
    }
    
    if (exporter == null) {
    	BrowserErrorHandler.processError(Messages.no_plugin_exporter_available, true);
    	return;
    }
    
    if (version.endsWith("qualifier")) { //$NON-NLS-1$
      exportInfo.qualifier = exporter.getQualifier();
      version = version.replaceAll("qualifier", exportInfo.qualifier); //$NON-NLS-1$
    }
    
    exportInfo.items = new Object[] {model};
    exportInfo.signingInfo = null;
    exportInfo.jnlpInfo = null;
    exportInfo.targets = null;

    final String path = exportInfo.destinationDirectory + "/plugins/"+name+"_"+version+".jar"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    exporter.exportPlugins(exportInfo);
    while (!exporter.hasFinished())
	    try {
	      Thread.sleep(100);
	    } catch (InterruptedException e1) {
	      e1.printStackTrace();
	    }
    if (exporter.getResult().isOK() && (new java.io.File(path)).exists()) {
      FrameworkConnectorFactory.installBundle(path, fw, true);
    } else {
      Throwable t = exporter.getResult().getException();
      if (t != null) {
        BrowserErrorHandler.processError(t, false);
      } else {
        BrowserErrorHandler.processWarning(Messages.bundle_installation_failure+name, false);
      }
    }
  }

  private ISelectionProvider getSelectionProvider() {
    return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getSite().getSelectionProvider();
  }
  
}
