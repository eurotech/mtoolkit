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
package org.tigris.mtoolkit.osgimanagement;

import java.io.File;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.installation.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworksView;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.internal.preferences.FrameworkPreferencesPage;
import org.tigris.mtoolkit.osgimanagement.model.Framework;

/**
 * @since 5.0
 */
public class Util {

  public static IStatus handleIAgentException(IAgentException e) {
    Throwable cause = e.getCauseException() != null ? e.getCauseException() : e;
    return Util.newStatus(IStatus.ERROR, getErrorMessage(e), cause);
  }

  public static IStatus newStatus(String message, IStatus e) {
    return new MultiStatus(FrameworkPlugin.PLUGIN_ID, 0, new IStatus[] {
      e
    }, message, null);
  }

  public static IStatus newStatus(int severity, String message, Throwable t) {
    return new Status(severity, FrameworkPlugin.PLUGIN_ID, message, t);
  }

  private static String getErrorMessage(IAgentException e) {
    String msg = e.getMessage();
    if (msg == null) {
      msg = Messages.operation_failed;
      Throwable cause = e.getCauseException();
      if (cause != null && cause.getMessage() != null) {
        msg += " " + cause.getMessage(); //$NON-NLS-1$
      }
    }
    return msg;
  }

  /**
   * @since 6.0
   */
  public static CoreException newException(int severity, String message, Throwable t) {
    return new CoreException(newStatus(severity, message, t));
  }

  /**
   * @since 6.0
   */
  public static void showMessageDialog(IStatus severity, String message) {
    StatusManager.getManager().handle(newStatus(message, severity), StatusManager.SHOW | StatusManager.LOG);
  }

  /**
   * @since 6.0
   */
  public static Framework addFramework(DeviceConnector connector, String identifier, IProgressMonitor monitor)
      throws CoreException {
    boolean autoConnectEnabled = FrameworkPreferencesPage.isAutoConnectEnabled();
    String fwName = FrameworkConnectorFactory.generateFrameworkName(connector.getProperties(), identifier);
    FrameworkImpl fw = new FrameworkImpl(fwName, true);
    try {
      if (!connector.getVMManager().isVMActive()) {
        throw newException(IStatus.ERROR, Messages.connection_failed, null);
      }
      if (autoConnectEnabled) {
        FrameworksView.getTreeRoot().addElement(fw);
      }
      fw.connect(connector, SubMonitor.convert(monitor));
    } catch (IAgentException e) {
      if (autoConnectEnabled) {
        FrameworksView.getTreeRoot().removeElement(fw);
      }
      throw newException(IStatus.ERROR, Messages.connection_failed, e);
    }
    return fw;
  }

  public static Set<String> getSystemBundles() {
    return FrameworksView.getSystemBundles();
  }

  /**
   * @since 6.0
   */
  public static File[] openFileSelectionDialog(Shell shell, String title, String filter, String filterLabel,
      boolean multiple) {
    FileDialog dialog = new FileDialog(shell, SWT.OPEN | (multiple ? SWT.MULTI : SWT.SINGLE));
    String[] filterArr = {
        filter, "*.*"}; //$NON-NLS-1$
    String[] namesArr = {
        filterLabel, Messages.all_files_filter_label
    };
    dialog.setFilterExtensions(filterArr);
    dialog.setFilterNames(namesArr);
    if (FrameworkPlugin.fileDialogLastSelection != null) {
      dialog.setFileName(null);
      dialog.setFilterPath(FrameworkPlugin.fileDialogLastSelection);
    }
    dialog.setText(title);
    String res = dialog.open();
    if (res != null) {
      FrameworkPlugin.fileDialogLastSelection = res;
      // getFileNames returns relative names!
      String[] names = dialog.getFileNames();
      String path = dialog.getFilterPath();
      File[] files = new File[names.length];
      for (int i = 0; i < names.length; i++) {
        files[i] = new File(path, names[i]);
      }
      return files;
    }
    return null;
  }

  public static Framework findFramework(DeviceConnector connector) {
    FrameworkImpl[] fws = FrameworksView.findFramework(connector);
    if (fws != null && fws.length > 0) {
      return fws[0];
    }
    return null;
  }
}
