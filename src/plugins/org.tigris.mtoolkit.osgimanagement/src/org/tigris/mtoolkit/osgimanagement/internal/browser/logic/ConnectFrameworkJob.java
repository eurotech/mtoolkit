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
package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.tigris.mtoolkit.common.FileUtils;
import org.tigris.mtoolkit.common.lm.LM;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.Util;
import org.tigris.mtoolkit.osgimanagement.installation.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworksView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.FrameworkPanel;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.FrameworkPanel.DeviceTypeProviderElement;
import org.tigris.mtoolkit.osgimanagement.model.Framework;

public final class ConnectFrameworkJob extends Job {
  private static final List connectingFrameworks = new ArrayList();

  private Framework         fw;

  public ConnectFrameworkJob(Framework framework) {
    super(NLS.bind(Messages.connect_framework, framework.getName()));
    this.fw = framework;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.
   * IProgressMonitor)
   */
  @Override
  public IStatus run(final IProgressMonitor monitor) {
    try {
      LM.verify(new NullProgressMonitor());
    } catch (CoreException e) {
      return e.getStatus();
    }

    monitor.beginTask(NLS.bind(Messages.connect_framework, fw.getName()), 1);

    synchronized (connectingFrameworks) {
      // there is already job for this fw, so wait that job
      // otherwise, start connecting
      if (connectingFrameworks.contains(fw)) {
        do {
          try {
            connectingFrameworks.wait();
          } catch (InterruptedException e) {
          }
          if (monitor.isCanceled()) {
            monitor.done();
            return Status.CANCEL_STATUS;
          }
        } while (connectingFrameworks.contains(fw));
        monitor.done();
        if (fw.isConnected()) {
          return Status.OK_STATUS;
        } else {
          return Util.newStatus(IStatus.ERROR, "Could not connect to framework " + fw.getName(), null);
        }
      }
      connectingFrameworks.add(fw);
    }

    DeviceConnector connector = fw.getConnector();
    try {
      if (connector != null && connector.isActive()) {
        FrameworkConnectorFactory.createPMPConnection(connector, (FrameworkImpl) fw, fw.getName(),
            ((FrameworkImpl) fw).isAutoConnected());
      } else {
        IMemento config = ((FrameworkImpl) fw).getConfig();
        String id = null;
        String transportType = null;
        Dictionary aConnProps = null;

        String providerID = config.getString(ConstantsDistributor.TRANSPORT_PROVIDER_ID);
        List providers = FrameworkPanel.obtainDeviceTypeProviders(null);
        for (int i = 0; i < providers.size(); i++) {
          DeviceTypeProviderElement provider = (DeviceTypeProviderElement) providers.get(i);
          if (providerID.equals(provider.getTypeId())) {
            try {
              transportType = provider.getProvider().getTransportType();
              aConnProps = provider.getProvider().load(config);
              id = (String) aConnProps.get(Framework.FRAMEWORK_ID);
            } catch (CoreException e) {
              FrameworkPlugin.log(e.getStatus());
            }
            break;
          }
        }

        if (transportType != null && id != null) {
          if (aConnProps == null) {
            aConnProps = new Hashtable();
          }
          IStatus rStatus = null;
          try {
            DeviceConnector conn = DeviceConnector.connect(transportType, id, aConnProps, null);
            FrameworkConnectorFactory.connectFramework(conn, (FrameworkImpl) fw);
          } catch (IAgentException e) {
            if (monitor.isCanceled()) {
              return Status.CANCEL_STATUS;
            }
            if (e.getErrorCode() == IAgentErrors.ERROR_CANNOT_CONNECT) {
              handleConnectionFailure(e);
              monitor.setCanceled(true);
            } else {
              rStatus = Util.handleIAgentException(e);
            }
          } catch (IllegalStateException e) {
            rStatus = Util.handleIAgentException(new IAgentException(e.getMessage(), IAgentErrors.ERROR_CANNOT_CONNECT,
                e));
          }
          if (rStatus != null) {
            monitor.done();
            return rStatus;
          }
        } else {
          errorProviderNotFound();
        }
      }
    } finally {
      // remove the framework in any case
      synchronized (connectingFrameworks) {
        connectingFrameworks.remove(fw);
        connectingFrameworks.notifyAll();
      }
    }
    monitor.done();
    if (monitor.isCanceled()) {
      return Status.CANCEL_STATUS;
    }
    return Status.OK_STATUS;
  }

  public static boolean isConnecting(FrameworkImpl fw) {
    synchronized (connectingFrameworks) {
      return connectingFrameworks.contains(fw);
    }
  }

  private static void errorProviderNotFound() {
    Display display = PlatformUI.getWorkbench().getDisplay();
    display.syncExec(new Runnable() {
      /*
       * (non-Javadoc)
       *
       * @see java.lang.Runnable#run()
       */
      public void run() {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        MessageDialog.openError(shell, "Error", "Could not connect to framework. The selected "
            + "connection type provider is no more available. Please select another connection type.");
      }
    });
  }

  private void handleConnectionFailure(final IAgentException e) {
    final Display display = PlatformUI.getWorkbench().getDisplay();
    display.asyncExec(new Runnable() {
      /*
       * (non-Javadoc)
       *
       * @see java.lang.Runnable#run()
       */
      public void run() {
        File[] iagentFile = FrameworkPlugin.getIAgentBundles();
        String[] buttons = null;
        if (iagentFile == null) {
          buttons = new String[] {
              Messages.close_button_label
          };
        } else {
          buttons = new String[] {
            Messages.close_button_label, Messages.get_iagent_button_label
          };
        }
        String message = Messages.connection_failed;
        if (e != null) {// add cause for connection failed
          message += "\nCause: " + e.getMessage();
          Throwable cause = e.getCauseException();
          if (cause != null) {
            message += " (" + cause.getLocalizedMessage() + ")";
          }
          FrameworkPlugin.error(e);
        }
        message += "\n\n" + Messages.rcp_bundle_missing_message;
        if (iagentFile != null) {
          message += Messages.get_missing_bundle_message;
        }
        MessageDialog dialog = new MessageDialog(FrameworksView.getShell(), Messages.rcp_bundle_missing_title, null,
            message, MessageDialog.INFORMATION, buttons, 0);
        dialog.setBlockOnOpen(true);
        dialog.open();
        if (dialog.getReturnCode() == 1) {
          // get IAgent button has been selected
          FileInputStream rpcStream = null;
          try {
            if (iagentFile == null) {
              return;
            }

            DirectoryDialog dirDialog = new DirectoryDialog(display.getActiveShell(), SWT.SAVE);
            dirDialog.setText(Messages.save_as_dialog_title);
            String path = dirDialog.open();
            if (path == null) {
              return;
            }
            path += File.separator;
            rpcStream = new FileInputStream(iagentFile[0]);
            saveFile(path + "iagent.rpc.jar", rpcStream);
          } catch (IOException ex) {
            StatusManager.getManager().handle(
                Util.newStatus(IStatus.ERROR, "An error occurred while saving IAgent bundle(s)", ex));
          } finally {
            FileUtils.close(rpcStream);
          }
        }
      }

      private void saveFile(String filePath, InputStream input) {
        OutputStream output = null;
        try {
          File file = new File(filePath);
          if (file.exists()) {
            boolean replaceFile = MessageDialog.openQuestion(null, Messages.confirm_replace_title,
                NLS.bind(Messages.error_file_already_exist, file.toString()));
            if (replaceFile) {
              int bytesRead = 0;
              byte[] buffer = new byte[1024];
              output = new FileOutputStream(file);
              while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
              }
            }
          }
        } catch (IOException ex) {
          StatusManager.getManager().handle(
              Util.newStatus(IStatus.ERROR, "An error occurred while saving IAgent bundle " + filePath, ex));
        } finally {
          FileUtils.close(output);
        }
      }
    });

  }
}
