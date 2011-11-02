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
package org.tigris.mtoolkit.osgimanagement.dp.logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.common.PluginUtilities;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteDP;
import org.tigris.mtoolkit.osgimanagement.dp.Activator;
import org.tigris.mtoolkit.osgimanagement.dp.DPModelProvider;
import org.tigris.mtoolkit.osgimanagement.dp.model.DeploymentPackage;
import org.tigris.mtoolkit.osgimanagement.model.Framework;
import org.tigris.mtoolkit.osgimanagement.model.Model;
import org.tigris.mtoolkit.osgimanagement.model.SimpleNode;

public class InstallDeploymentOperation {

  private Framework framework;

  public InstallDeploymentOperation(Framework framework) {
    this.framework = framework;
  }

  public void startJob(final File sourceFile, IProgressMonitor monitor) {
    RemoteDeploymentOperation operation = new RemoteDeploymentOperation("Installing deployment package...", framework) {
      protected IStatus doOperation(IProgressMonitor monitor) {
        RemoteDP remoteDP = null;
        try {
          remoteDP = install(sourceFile, monitor);
          if (remoteDP == null) {
            return new Status(Status.ERROR, Activator.PLUGIN_ID, NLS.bind("Unable to install the source file \"{0}\"",
                sourceFile));
          }
        } catch (Exception e) {
          return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
        }

        return Status.OK_STATUS;
      }

      protected String getMessage(IStatus operationStatus) {
        return "Deployment package installation failed";
      }
    };
    operation.schedule();
  }

  public RemoteDP install(File sourceFile, IProgressMonitor monitor) throws IAgentException, IllegalArgumentException {
    RemoteDP dp = null;
    JarFile jar = null;
    FileInputStream fis = null;
    try {
      jar = new JarFile(sourceFile);
      Manifest manifest = jar.getManifest();
      if (manifest == null)
        throw new IllegalArgumentException(NLS.bind("Source file \"{0}\" doesn't have valid manifest", sourceFile));
      String symbolicName = manifest.getMainAttributes().getValue("DeploymentPackage-SymbolicName");
      if (symbolicName == null)
        throw new IllegalArgumentException(NLS.bind("Source file \"{0}\" doesn't have valid manifest", sourceFile));
      String version = manifest.getMainAttributes().getValue("DeploymentPackage-Version");
      if (version == null)
        throw new IllegalArgumentException(NLS.bind("Source file \"{0}\" doesn't have valid manifest", sourceFile));
      DeviceConnector connector = framework.getConnector();
      if (connector == null) {
        //        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Connection lost", null);
        //processError("Connection lost", true);
        throw new IAgentException("Connection lost", IStatus.ERROR);
      }
      RemoteDP remoteDP = connector.getDeploymentManager().getDeploymentPackage(symbolicName);
      if (remoteDP != null) {
        // deployment package already exists, if it has the same version
        // we need to remove and install again after user confirmation
        String remoteVersion = remoteDP.getVersion();
        if (remoteVersion.equals(version)) {
          if (askUserToUninstallRemotePackage(symbolicName)) {
            Model[] fwChildren = framework.getChildren();
            Model dpNode = null;
            for (int i = 0; i < fwChildren.length; i++) {
              if (fwChildren[i] instanceof SimpleNode && "Deployment Packages".equals(fwChildren[i].getName())) {
                dpNode = fwChildren[i];
                break;
              }
            }
            DeploymentPackage packageNode = DPModelProvider.findDP(dpNode, symbolicName);
            if (packageNode == null)
              throw new IllegalArgumentException(
                  "Local representation of the remote OSGi framework is stale. Refresh and try again.");
            UninstallDeploymentOperation uninstallJob = new UninstallDeploymentOperation(packageNode);
            monitor.subTask(uninstallJob.getName());
            IStatus status = uninstallJob.uninstallDeploymentPackage(false);
            if (!status.equals(Status.OK_STATUS)) {
              throw new IAgentException(status.getMessage(), status.getCode(), status.getException());
            }
            monitor.subTask(getName(sourceFile));
          }
        }
      }
      fis = new FileInputStream(sourceFile);
      dp = framework.getConnector().getDeploymentManager().installDeploymentPackage(fis);
    } catch (IOException e) {
      throw new IllegalArgumentException(NLS.bind("Failed to prepare file \"{0}\" Cause:", sourceFile.getName(),
          e.getMessage()));
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
        }
        if (jar != null) {
          try {
            jar.close();
          } catch (IOException e) {
          }
        }
      }

    }
    return dp;
  }

  private String getName(File sourceFile) {
    return sourceFile.getName();
  }

  private boolean askUserToUninstallRemotePackage(final String symbolicName) {
    Display display = PlatformUI.getWorkbench().getDisplay();
    final int[] result = new int[1];
    display.syncExec(new Runnable() {
      private MessageDialog dialog;

      public void run() {
        dialog = new MessageDialog(
            PluginUtilities.getActiveWorkbenchShell(),
            "Uninstall Existing Deployment Package",
            null,
            NLS.bind(
                "The deployment package \"{0}\" exists on the remote framework with the same version. If you want to update it, the remote version of the deployment package needs to be uninstalled first",
                symbolicName), MessageDialog.QUESTION, new String[] { "Uninstall Remote Version", "Cancel" }, 0);
        result[0] = dialog.open();
      }
    });
    return result[0] == 0;
  }

  public static void processError(final String message, boolean showDialog) {
    if (showDialog) {
      Display display = PlatformUI.getWorkbench().getDisplay();
      if (!display.isDisposed()) {
        display.asyncExec(new Runnable() {
          public void run() {
            Shell shell = getShell();
            if (shell != null) {
              if (!shell.isDisposed()) {
                MessageDialog.openError(shell, "Error", message);
              }
            }
          }
        });
      }
    }
    //dumpToLog(IStatus.ERROR, message, null);
  }

  // Get active shell
  public static Shell getShell() {
    Display display = PlatformUI.getWorkbench().getDisplay();
    if (display.isDisposed()) {
      return null;
    }
    return display.getActiveShell();
  }

}
