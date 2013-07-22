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
package org.tigris.mtoolkit.osgimanagement.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.tigris.mtoolkit.common.PluginUtilities;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.Util;
import org.tigris.mtoolkit.osgimanagement.installation.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.internal.preferences.FrameworkPreferencesPage;

public final class FrameworkPlugin extends AbstractUIPlugin {
  private static final boolean   DEBUG         = "true".equals(Platform.getDebugOption("org.tigris.mtoolkit.osgimanagement/debug")); //$NON-NLS-1$

  public static final String     PLUGIN_ID     = "org.tigris.mtoolkit.osgimanagement";                                              //$NON-NLS-1$

  public static final String     IAGENT_RPC_ID = "org.tigris.mtoolkit.iagent.rpc";

  private static FrameworkPlugin instance      = null;
  public static String           fileDialogLastSelection;

  public FrameworkPlugin() {
    super();
    if (instance == null) {
      instance = this;
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    FrameworkPlugin.instance = this;
    FrameworkConnectorFactory.init();
    FrameworksView.restoreModel();
    fileDialogLastSelection = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    FrameworksView.saveModel();
    FrameworkConnectorFactory.deinit();
    FrameworkPlugin.instance = null;
    super.stop(context);
  }

  // Returns default instance
  public static FrameworkPlugin getDefault() {
    return instance;
  }

  public static void error(IAgentException e) {
    log(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
  }

  public static void error(String message, Throwable t) {
    log(new Status(IStatus.ERROR, PLUGIN_ID, message, t));
  }

  public static void warning(String message, Throwable t) {
    log(new Status(IStatus.WARNING, PLUGIN_ID, message, t));
  }

  public static void info(String message, Throwable t) {
    log(new Status(IStatus.INFO, PLUGIN_ID, message, t));
  }

  public static void log(IStatus status) {
    FrameworkPlugin fwPlugin = getDefault();
    if (fwPlugin == null) {
      return;
    }
    ILog fwLog = fwPlugin.getLog();
    fwLog.log(status);
  }

  public static File getFile(String name) {
    IPath statePath = getDefault().getStateLocation();
    return new File(statePath.toFile(), name);
  }

  /** Returns an input stream to plug-in's embedded resource. */
  public static InputStream getResourceStream(Path aPath) throws IOException {
    if (instance == null) {
      return FrameworkPlugin.class.getResourceAsStream(aPath.toPortableString());
    } else {
      URL resourceURL = FileLocator.find(instance.getBundle(), aPath, null);
      if (resourceURL != null) {
        return resourceURL.openStream();
      }
    }
    return null;
  }

  public static File[] getIAgentBundles() {
    Bundle selectedIAgentRpc = Platform.getBundle(IAGENT_RPC_ID);
    if (selectedIAgentRpc == null) {
      return null;
    }
    try {
      File bundleFile = FileLocator.getBundleFile(selectedIAgentRpc);
      if (bundleFile.isFile()) {
        return new File[] {
          bundleFile
        };
      }
    } catch (IOException e) {
      error("Failed to find IAgent bundle(s)", e);
    }
    return null;
  }

  public static void processError(final String message, boolean showDialog) {
    if (showDialog) {
      Display display = PlatformUI.getWorkbench().getDisplay();
      if (!display.isDisposed()) {
        display.asyncExec(new Runnable() {
          /* (non-Javadoc)
           * @see java.lang.Runnable#run()
           */
          public void run() {
            Shell shell = PluginUtilities.getActiveWorkbenchShell();
            if (shell != null && !shell.isDisposed()) {
              MessageDialog.openError(shell, Messages.standard_error_title, message);
            }
          }
        });
      }
    }
    if (message != null) {
      FrameworkPlugin.error(message, null);
    }
  }

  // Process given exception with no reason given
  public static void processError(Throwable t, DeviceConnector connector, boolean user) {
    if (!user) {
      processError(t, connector, Messages.no_reason_message);
    }
  }

  public static void processError(Throwable t, DeviceConnector connector, String reason) {
    boolean display = true;
    Boolean autoConnected = connector == null ? new Boolean(false) : (Boolean) connector.getProperties().get(
        "framework-connection-immediate"); //$NON-NLS-1$
    if (autoConnected == null || autoConnected.booleanValue()) {
      display = false;
    }
    processError(t, reason, display);
  }

  // Process given exception with no reason given
  public static void processError(Throwable t, boolean showDialog) {
    processError(t, Messages.no_reason_message, showDialog);
  }

  // Process given exception with reason
  public static void processError(final Throwable t, String info, boolean display) {

    // Subsitute missing exception message
    final String reason[] = new String[1];
    if (t.getMessage() == null) {
      reason[0] = Messages.no_exception_message;
    } else {
      reason[0] = t.getMessage();
    }

    if (display) {
      if (t instanceof IAgentException || t instanceof IllegalStateException) {
        String infoCode = ""; //$NON-NLS-1$
        if (t instanceof IAgentException) {
          int errorCode = ((IAgentException) t).getErrorCode();
          if (errorCode != -1 && errorCode != 0) {
            infoCode = Messages.get(String.valueOf(errorCode).replace('-', '_'));
          }
          if (info == null || Messages.no_reason_message.equals(info)) {
            info = infoCode;
          }
        }
        final String trueMessage = info;
        Display disp = PlatformUI.getWorkbench().getDisplay();
        if (!disp.isDisposed()) {
          disp.asyncExec(new Runnable() {
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            public void run() {
              Shell shell = PluginUtilities.getActiveWorkbenchShell();
              if (shell != null && !shell.isDisposed()) {
                PluginUtilities.showErrorDialog(shell, Messages.standard_error_title, trueMessage, reason[0], t);
              }
            }
          });
        }
      } else {
        processError(info, display);
      }
    }
    if (t instanceof IAgentException) {
      FrameworkPlugin.log(Util.handleIAgentException((IAgentException) t));
    } else if (reason[0] != null) {
      FrameworkPlugin.error(reason[0], t);
    }
  }

  public static void processWarning(final String info, boolean display) { // NO_UCD (unused code)
    if (display) {
      Display disp = PlatformUI.getWorkbench().getDisplay();
      if (!disp.isDisposed()) {
        disp.asyncExec(new Runnable() {
          /* (non-Javadoc)
           * @see java.lang.Runnable#run()
           */
          public void run() {
            Shell shell = PluginUtilities.getActiveWorkbenchShell();
            if (shell != null && !shell.isDisposed()) {
              PluginUtilities.showWarningDialog(shell, Messages.standard_error_title, info, null);
            }
          }
        });
      }
    }
    if (info != null) {
      FrameworkPlugin.warning(info, null);
    }
  }

  public static void processWarning(final Throwable t, String info, boolean display) { // NO_UCD (unused code)
    // Subsitute missing exception message
    final String reason;
    if (t.getMessage() == null) {
      reason = Messages.no_exception_message;
    } else {
      reason = t.getMessage();
    }

    if (display) {
      if (t instanceof IAgentException) {
        int errorCode = ((IAgentException) t).getErrorCode();
        if (errorCode != -1 && errorCode != 0) {
          info = Messages.get(String.valueOf(errorCode).replace('-', '_'));
        }
        final String trueMessage = info;
        Display disp = PlatformUI.getWorkbench().getDisplay();
        if (!disp.isDisposed()) {
          disp.asyncExec(new Runnable() {
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            public void run() {
              Shell shell = PluginUtilities.getActiveWorkbenchShell();
              if (shell != null && !shell.isDisposed()) {
                PluginUtilities.showWarningDialog(shell, Messages.standard_error_title, trueMessage, reason);
              }
            }
          });
        }
      }
    }
    if (t instanceof IAgentException) {
      FrameworkPlugin.log(Util.handleIAgentException((IAgentException) t));
    } else if (reason != null) {
      FrameworkPlugin.warning(reason, t);
    }
  }

  public static void processInfo(final String text, boolean display) {
    if (display) {
      showInfoDialog(text);
    }
    if (FrameworkPreferencesPage.isLogInfoEnabled() && text != null) {
      FrameworkPlugin.info(text, null);
    }
  }

  public static void debug(String msg) {
    if (DEBUG) {
      System.out.println("[OSGiManagement][debug] " + msg); //$NON-NLS-1$
    }
  }

  public static void debug(Throwable t) {
    if (DEBUG) {
      t.printStackTrace(System.out);
    }
  }

  private static void showInfoDialog(final String text) {
    Display display = PlatformUI.getWorkbench().getDisplay();
    if (!display.isDisposed()) {
      display.asyncExec(new Runnable() {
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
          Shell shell = PluginUtilities.getActiveWorkbenchShell();
          if (shell != null && !shell.isDisposed()) {
            MessageDialog.openInformation(shell, Messages.standard_info_title, text);
          }
        }
      });
    }
  }
}
