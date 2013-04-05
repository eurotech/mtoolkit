/*******************************************************************************
 * Copyright (c) 2005, 2013 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.common.FileUtils;
import org.tigris.mtoolkit.common.ManifestUtils;
import org.tigris.mtoolkit.common.PluginUtilities;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.osgimanagement.Util;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;

public final class UpdatePreverifyOperation extends RemoteBundleOperation {
  private final File bundleFile;

  public UpdatePreverifyOperation(Bundle bundle, File bundleFile) {
    super(Messages.update_bundle, bundle);
    this.bundleFile = bundleFile;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.internal.browser.logic.RemoteBundleOperation#doOperation(org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  protected IStatus doOperation(IProgressMonitor monitor) throws IAgentException {
    ZipFile zip = null;
    try {
      RemoteBundle rBundle = getBundle().getRemoteBundle();
      boolean nameDiff = true;
      boolean versionsDiff = true;
      zip = new ZipFile(bundleFile);
      ZipEntry mf = zip.getEntry(JarFile.MANIFEST_NAME);
      final String symbNames[] = new String[] {
          "", rBundle.getSymbolicName()
      };
      final String versions[] = new String[] {
          "", rBundle.getVersion()
      };
      if (mf != null) {
        Map headers = ManifestUtils.getManifestHeaders(zip.getInputStream(mf));
        if (headers != null) {
          symbNames[0] = ManifestUtils.getBundleSymbolicName(headers);
          versions[0] = ManifestUtils.getBundleVersion(headers);
        }
        nameDiff = symbNames[1] != null && !symbNames[1].equals(symbNames[0]);
        versionsDiff = versions[1] != null && !versions[1].equals(versions[0]);
      }
      correctNullElements(symbNames, "unknown");
      correctNullElements(versions, "unknown");
      if (nameDiff) {
        final int confirm[] = new int[SWT.CANCEL];
        PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
          public void run() {
            String message = "The new and old versions of the bundle have different symbolic names:\n" + "Existing: "
                + symbNames[1] + " (" + versions[1] + ")\n" + "New: " + symbNames[0] + " (" + versions[0] + ")\n"
                + "Are you sure you want to do this?";
            confirm[0] = PluginUtilities.showConfirmationDialog(FrameWorkView.getShell(), "Update bundle", message);
          }
        });
        if (confirm[0] == SWT.CANCEL) {
          return Status.CANCEL_STATUS;
        }
      } else if (versionsDiff) {
        final int confirm[] = new int[SWT.CANCEL];
        PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
          /* (non-Javadoc)
           * @see java.lang.Runnable#run()
           */
          public void run() {
            String message = "The new and old bundles have different versions:\n" + "Existing: " + versions[1] + "\n"
                + "New: " + versions[0] + "\n" + "Are you sure you want to do this?";
            confirm[0] = PluginUtilities.showConfirmationDialog(FrameWorkView.getShell(), "Update bundle", message);
          }
        });
        if (confirm[0] == SWT.CANCEL) {
          return Status.CANCEL_STATUS;
        }
      }
    } catch (IOException ioe) {
      return Util.newStatus(IStatus.ERROR, "Failed to verify bundle", ioe);
    } finally {
      FileUtils.close(zip);
    }
    return Status.OK_STATUS;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.internal.browser.logic.RemoteBundleOperation#getMessage(org.eclipse.core.runtime.IStatus)
   */
  @Override
  protected String getMessage(IStatus operationStatus) {
    return NLS.bind(Messages.bundle_update_failure, operationStatus);
  }

  private static void correctNullElements(String[] arr, String def) {
    for (int i = 0; i < arr.length; i++) {
      if (arr[i] == null) {
        arr[i] = def;
      }
    }
  }
}
