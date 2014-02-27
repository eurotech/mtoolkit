/*******************************************************************************
 * Copyright (c) 2005, 2012 ProSyst Software GmbH and others.
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.common.FileUtils;
import org.tigris.mtoolkit.common.ManifestUtils;
import org.tigris.mtoolkit.common.installation.ProgressInputStream;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworksView;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;

public final class InstallBundleOperation {

  private static final int    CANCEL  = 1;
  private static final int    INSTALL = 2;
  private static final int    UPDATE  = 3;

  private final FrameworkImpl framework;

  public InstallBundleOperation(FrameworkImpl framework) {
    this.framework = framework;
  }

  public RemoteBundle installBundle(File bundle, boolean autoUpdate, IProgressMonitor monitor) throws IAgentException {
    InputStream input = null;
    RemoteBundle rBundles[] = null;
    ZipFile zip = null;
    InputStream zis = null;
    try {
      int work = (int) bundle.length();
      monitor.beginTask(Messages.install_bundle, work);
      DeviceConnector connector = framework.getConnector();
      if (connector == null) {
        FrameworkPlugin.processError("Connection lost", true);
        return null;
      }

      zip = new ZipFile(bundle);
      ZipEntry entry = zip.getEntry(JarFile.MANIFEST_NAME);
      if (entry == null) {
        throw new IllegalArgumentException("Invalid bundle content: " + bundle.getName());
      }
      zis = zip.getInputStream(entry);
      Map headers = ManifestUtils.getManifestHeaders(zis);
      String symbolicName = ManifestUtils.getBundleSymbolicName(headers);
      String version = ManifestUtils.getBundleVersion(headers);

      // check if already installed
      boolean installAllowed = true;
      if (symbolicName != null) {
        rBundles = connector.getDeploymentManager().getBundles(symbolicName, version);
        if (rBundles != null) {
          installAllowed = false;
        } else {
          rBundles = connector.getDeploymentManager().getBundles(symbolicName, null);
        }
      }
      if (rBundles != null && framework.isSystemBundle(rBundles[0])) {
        throw new IllegalArgumentException("Bundle " + symbolicName + " is system");
      }

      // install if missing
      if (rBundles == null) {
        Set bundleIds = new HashSet();
        bundleIds.addAll(framework.getBundlesKeys());
        DateFormat df = new SimpleDateFormat("yyyyMMdd-hhmmssSSS");
        input = new ProgressInputStream(new FileInputStream(bundle), monitor);
        RemoteBundle newBundle = connector.getDeploymentManager().installBundle(
            "remote:" + bundle.getName() + "." + df.format(new Date()), input);
        // check again if already installed
        if (!bundleIds.contains(new Long(newBundle.getBundleId()))) {
          return newBundle;
        } else {
          // close the old input stream and try again
          FileUtils.close(input);
          input = null;
          rBundles = new RemoteBundle[] {
            newBundle
          };
        }
      }

      // bundle already exists, in which case, we need to update it
      int action;
      int bundleIndex;

      if (rBundles.length == 1 && autoUpdate) {
        action = UPDATE;
        bundleIndex = 0;
      } else {
        int selectedIndex[] = new int[] {
          0
        };
        action = showUpdateBundleDialog(symbolicName, version, installAllowed, selectedIndex, rBundles);
        bundleIndex = selectedIndex[0];
      }

      if (action == INSTALL) {
        monitor.beginTask(Messages.install_bundle, work);
        DateFormat df = new SimpleDateFormat("yyyyMMdd-hhmmssSSS");
        input = new ProgressInputStream(new FileInputStream(bundle), monitor);
        return connector.getDeploymentManager().installBundle(
            "remote:" + bundle.getName() + "." + df.format(new Date()), input);
      } else if (action == UPDATE) {
        monitor.beginTask(Messages.update_bundle, work);
        input = new ProgressInputStream(new FileInputStream(bundle), monitor);
        rBundles[bundleIndex].update(input);
        return rBundles[bundleIndex];
      } else {
        return null;
      }
    } catch (IOException e) {
      throw new IllegalArgumentException(NLS.bind(Messages.update_file_not_found, bundle.getName()), e);
    } finally {
      FileUtils.close(input);
      FileUtils.close(zis);
      FileUtils.close(zip);
    }
  }

  private int showUpdateBundleDialog(final String symbolicName, final String version, final boolean installAllowed,
      final int[] selectedIndex, final Object[] rBundles) {
    final int result[] = new int[] {
      InstallBundleOperation.CANCEL
    };

    PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
      public void run() {
        TitleAreaDialog updateDialog = new TitleAreaDialog(FrameworksView.getShell()) {
          private Button                       updateButton;
          private org.eclipse.swt.widgets.List list;

          @Override
          protected Control createDialogArea(Composite parent) {
            Control main = super.createDialogArea(parent);
            list = new org.eclipse.swt.widgets.List((Composite) main, SWT.BORDER);
            try {
              for (int i = 0; i < rBundles.length; i++) {
                list.add(symbolicName + " (" + ((RemoteBundle) rBundles[i]).getVersion() + ")");
              }
            } catch (IAgentException e) {
            }
            if (list.getItemCount() > 0) {
              list.setSelection(0);
            }
            list.addSelectionListener(new SelectionListener() {
              public void widgetSelected(SelectionEvent e) {
                updateButton.setEnabled(list.getSelectionIndex() != -1);
              }

              public void widgetDefaultSelected(SelectionEvent e) {
              }
            });
            list.setLayoutData(new GridData(GridData.FILL_BOTH));
            String title = "Bundle \"" + symbolicName + "\" is already installed!";
            if (installAllowed) {
              title += "\nInstall version " + version + ", or select bundle to update.";
            }
            setTitle(title);
            getShell().setText("Update bundle");
            return main;
          }

          @Override
          protected void createButtonsForButtonBar(Composite parent) {
            Button installButton = createButton(parent, IDialogConstants.CLIENT_ID + 1, "Install", false);
            updateButton = createButton(parent, IDialogConstants.CLIENT_ID + 2, "Update", false);
            updateButton.setEnabled(list.getSelectionIndex() != -1);
            createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
            if (!installAllowed) {
              installButton.setEnabled(false);
            }
          }

          @Override
          protected void buttonPressed(int buttonId) {
            selectedIndex[0] = list.getSelectionIndex();
            switch (buttonId) {
            case IDialogConstants.CLIENT_ID + 1:
              setReturnCode(InstallBundleOperation.INSTALL);
              break;
            case IDialogConstants.CLIENT_ID + 2:
              setReturnCode(InstallBundleOperation.UPDATE);
              break;
            default:
              setReturnCode(InstallBundleOperation.CANCEL);
              break;
            }
            close();
          }
        };

        result[0] = updateDialog.open();
      }
    });
    return result[0];
  }
}
