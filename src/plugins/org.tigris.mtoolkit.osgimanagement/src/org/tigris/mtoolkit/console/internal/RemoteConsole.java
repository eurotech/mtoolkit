/*******************************************************************************
 * Copyright (c) 2009 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.console.internal;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleHyperlink;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.part.IPageBookViewPage;
import org.tigris.mtoolkit.iagent.DeviceConnectionEvent;
import org.tigris.mtoolkit.iagent.DeviceConnectionListener;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.VMManager;
import org.tigris.mtoolkit.osgimanagement.Util;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.images.ImageHolder;

@SuppressWarnings("deprecation")
public final class RemoteConsole extends IOConsole implements IConsole, DeviceConnectionListener {
  public static final String             P_DISCONNECTED = "org.tigris.mtoolkit.console.internal.console.disconnected"; //$NON-NLS-1$

  private final Date                     timestamp      = new Date();
  private final IProcess                 process;

  private volatile DeviceConnector       connector;
  private volatile ConsoleReader         reader;
  private volatile IOConsoleOutputStream output;
  private String                         name;

  public RemoteConsole(DeviceConnector dc, String name, String consoleType, IProcess iProcess, Object fwId) {
    super("", consoleType, ImageHolder.getImageDescriptor(ImageHolder.SERVER_ICON_CONNECTED), true); //$NON-NLS-1$
    this.name = name;
    this.connector = dc;
    this.process = iProcess;
    DeviceConnector.addDeviceConnectionListener(this);
    setAttribute("mtoolkit.console.connector", connector); //$NON-NLS-1$
    if (fwId != null) {
      setAttribute("mtoolkit.console.frameworkid", fwId); //$NON-NLS-1$
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.console.AbstractConsole#getImageDescriptor()
   */
  @Override
  public ImageDescriptor getImageDescriptor() {
    if (isDisconnected()) {
      return ImageHolder.getImageDescriptor(ImageHolder.SERVER_ICON_DISCONNECTED);
    }
    return ImageHolder.getImageDescriptor(ImageHolder.SERVER_ICON_CONNECTED);
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.console.IOConsole#createPage(org.eclipse.ui.console.IConsoleView)
   */
  @Override
  public IPageBookViewPage createPage(IConsoleView view) {
    IPageBookViewPage createPage = super.createPage(view);
    if (reader != null) {
      return createPage;
    }
    Job job = new Job(Messages.redirect_console_output) {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        synchronized (RemoteConsole.this) {
          if (reader == null && connector != null && connector.isActive()) {
            reader = redirectInput(RemoteConsole.this, connector);
            output = newOutputStream();
            redirectOutput(output, connector);
          }
        }
        return Status.OK_STATUS;
      }
    };
    job.schedule();
    setName(computeName());
    return createPage;
  }

  public IProcess getProcess() {
    return process;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.console.IConsole#getStream(java.lang.String)
   */
  public IOConsoleOutputStream getStream(String streamIdentifier) {
    return output;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.console.IConsole#addLink(org.eclipse.debug.ui.console.IConsoleHyperlink, int, int)
   */
  public void addLink(IConsoleHyperlink link, int offset, int length) {
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.console.IConsole#addLink(org.eclipse.ui.console.IHyperlink, int, int)
   */
  public void addLink(IHyperlink link, int offset, int length) {
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.console.IConsole#connect(org.eclipse.debug.core.model.IStreamsProxy)
   */
  public void connect(IStreamsProxy streamsProxy) {
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.console.IConsole#connect(org.eclipse.debug.core.model.IStreamMonitor, java.lang.String)
   */
  public void connect(IStreamMonitor streamMonitor, String streamIdentifer) {
  }

  public IRegion getRegion(IConsoleHyperlink link) {
    return super.getRegion(link);
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.console.IOConsole#dispose()
   */
  @Override
  protected void dispose() {
    Job disconnectJob = new Job(Messages.RemoteConsole_Disconnecting_Console) {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        disconnect();
        if (monitor.isCanceled()) {
          return Status.CANCEL_STATUS;
        }
        return Status.OK_STATUS;
      }
    };
    disconnectJob.setSystem(true);
    disconnectJob.schedule();
    super.dispose();
  }

  public boolean isDisconnected() {
    final DeviceConnector connector = this.connector;
    return connector == null || !connector.isActive();
  }

  public void setConsoleName(String name) {
    this.name = name;
    setName(computeName());
  }

  public void disconnect() {
    DeviceConnector.removeDeviceConnectionListener(this);

    Display display = PlatformUI.getWorkbench().getDisplay();
    if (!display.isDisposed()) {
      display.asyncExec(new Runnable() {
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
          setName(computeName());
        }
      });
    }

    synchronized (RemoteConsole.this) {
      if (reader != null) {
        reader.dispose();
      }
      if (output != null) {
        if (connector != null && connector.isActive()) {
          try {
            VMManager vmManager = connector.getVMManager();
            if (vmManager.isVMActive()) {
              vmManager.redirectFrameworkOutput(null);
            }
          } catch (IAgentException e) {
            FrameworkPlugin.error(Messages.RemoteConsole_FW_Out_Reset_Failed, e);
          }
        }
        try {
          output.close();
        } catch (IOException e) {
        }
      }
      connector = null;
    }

    firePropertyChange(this, P_DISCONNECTED, Boolean.FALSE, Boolean.TRUE);

    // Do not call dispose here because console remains in the view
    // (disconnected).
    // Some IOConsole operations run in jobs and if the the console is
    // disposed,
    // they may not be scheduled. E.g. making console read only may not be
    // executed
    // when the console is disposed. The console will be disposed when it is
    // removed.
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.DeviceConnectionListener#deviceConnectionEvent(org.tigris.mtoolkit.iagent.DeviceConnectionEvent)
   */
  public void deviceConnectionEvent(DeviceConnectionEvent event) {
    if (event.getType() == DeviceConnectionEvent.DISCONNECTED) {
      if (event.getConnector().equals(this.connector)) {
        disconnect();
      }
    }
  }

  public boolean equalsName(String name) {
    String computeName = computeName(name);
    String fwName = computeName();
    return computeName.equals(fwName);
  }

  private String computeName() {
    return computeName(name);
  }

  private String computeName(String fwName) {
    StringTokenizer tokenizer = new StringTokenizer(fwName, "()");
    boolean hasDate = false;
    while (tokenizer.hasMoreTokens()) {
      String nextToken = tokenizer.nextToken();
      try {
        Date.parse(nextToken);
        hasDate = true;
      } catch (Exception e) {
      }
    }
    String timeStamp = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(timestamp);
    return (isDisconnected() ? Messages.RemoteConsole_Disconnected : "") + fwName //$NON-NLS-1$
        + (hasDate ? Messages.RemoteConsole_Remote_Framework_Name : NLS.bind(
            Messages.RemoteConsole_Remote_Console_Name, timeStamp));
  }

  private static ConsoleReader redirectInput(RemoteConsole console, DeviceConnector connector) {
    try {
      return new ConsoleReader(console, connector.getVMManager());
    } catch (IAgentException e) {
      FrameworkPlugin.error(Messages.RemoteConsole_Redirection_Failed, e);
    }
    return null;
  }

  private static void redirectOutput(IOConsoleOutputStream output, DeviceConnector connector) {
    try {
      connector.getVMManager().redirectFrameworkOutput(output);
    } catch (IAgentException e) {
      try {
        IStatus status = Util.handleIAgentException(e);
        output.write(NLS.bind(Messages.RemoteConsole_FW_Redirection_Failed, status.getMessage()));
      } catch (IOException e1) {
        FrameworkPlugin.error(Messages.RemoteConsole_Console_Write_Failed, e1);
      }
      FrameworkPlugin.log(Util.handleIAgentException(e));
    }
  }
}
