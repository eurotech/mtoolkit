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
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.internal.preferences.FrameworkPreferencesPage;

public final class InstallBundleOperation {
  private final FrameworkImpl framework;

  public InstallBundleOperation(FrameworkImpl framework) {
    this.framework = framework;
  }

  public RemoteBundle installBundle(File bundle, IProgressMonitor monitor) throws IAgentException {
    InputStream input = null;
    RemoteBundle rBundle[] = null;
    ZipFile zip = null;
    InputStream zis = null;
    final boolean autoUpdateBundles = FrameworkPreferencesPage.isAutoUpdateBundlesOnInstallEnabled();
    try {
      int work = (int) bundle.length();
      monitor.beginTask(Messages.install_bundle, work);
      input = new ProgressInputStream(new FileInputStream(bundle), monitor);
      DeviceConnector connector = framework.getConnector();
      if (connector == null) {
        BrowserErrorHandler.processError("Connection lost", true);
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
      final boolean update[] = new boolean[] {
        false
      };
      final boolean install[] = new boolean[] {
        false
      };
      if (symbolicName != null) {
        rBundle = connector.getDeploymentManager().getBundles(symbolicName, version);
        if (rBundle != null) {
          update[0] = true;
        } else {
          rBundle = connector.getDeploymentManager().getBundles(symbolicName, null);
          if (rBundle != null) {
            if (rBundle.length == 1 && autoUpdateBundles) {
              update[0] = true;
            } else {
              install[0] = true;
            }
          }
        }
      }
      if (rBundle != null && framework.isSystemBundle(rBundle[0])) {
        throw new IllegalArgumentException("Bundle " + symbolicName + " is system");
      }

      // install if missing
      if (!update[0] && !install[0]) {
        Set bundleIds = new HashSet();
        bundleIds.addAll(framework.getBundlesKeys());
        rBundle = new RemoteBundle[1];
        DateFormat df = new SimpleDateFormat("yyyyMMdd-hhmmssSSS");
        rBundle[0] = connector.getDeploymentManager().installBundle(
            "remote:" + bundle.getName() + "." + df.format(new Date()), input);
        // check again if already installed
        if (bundleIds.contains(new Long(rBundle[0].getBundleId()))) {
          update[0] = true;
        }
      }
      // bundle already exists, in which case, we need to update it
      if (rBundle != null && (update[0] || install[0])) {
        // close the old input stream and try again
        FileUtils.close(input);
        final Object rBundles[] = rBundle;
        int bundleIndex;
        if (!install[0] && autoUpdateBundles) {
          bundleIndex = 0;
        } else {
          bundleIndex = showUpdateBundleDialog(symbolicName, version, update, install, rBundles)[0];
        }
        if (install[0]) {
          monitor.beginTask(Messages.install_bundle, work);
          DateFormat df = new SimpleDateFormat("yyyyMMdd-hhmmssSSS");
          rBundle[0] = connector.getDeploymentManager().installBundle(
              "remote:" + bundle.getName() + "." + df.format(new Date()),
              new ProgressInputStream(new FileInputStream(bundle), monitor));
        } else if (update[0]) {
          monitor.beginTask(Messages.update_bundle, work);
          input = new ProgressInputStream(new FileInputStream(bundle), monitor);
          rBundle[bundleIndex].update(input);
        } else {
          rBundle = null;
        }
      }
    } catch (IOException e) {
      throw new IllegalArgumentException(NLS.bind(Messages.update_file_not_found, bundle.getName()), e);
    } finally {
      FileUtils.close(input);
      FileUtils.close(zis);
      FileUtils.close(zip);
    }
    return (rBundle == null) ? null : rBundle[0];
  }

  private int[] showUpdateBundleDialog(final String symbolicName, final String version, final boolean[] update,
      final boolean[] install, final Object[] rBundles) {
    final int selected[] = new int[] {
      0
    };
    PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
      public void run() {
        TitleAreaDialog updateDialog = new TitleAreaDialog(FrameWorkView.getShell()) {
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
            if (install[0]) {
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
            if (!install[0]) {
              installButton.setEnabled(false);
            }
          }

          @Override
          protected void buttonPressed(int buttonId) {
            selected[0] = list.getSelectionIndex();
            setReturnCode(buttonId);
            close();
          }

        };
        int updateResult = updateDialog.open();
        if (updateResult == IDialogConstants.CLIENT_ID + 1) {
          install[0] = true;
          update[0] = false;
        } else if (updateResult == IDialogConstants.CLIENT_ID + 2) {
          update[0] = true;
          install[0] = false;
        } else {
          install[0] = false;
          update[0] = false;
        }
      }
    });
    return selected;
  }
}
