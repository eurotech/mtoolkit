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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.VMManager;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;

public final class ConsoleReader implements Runnable {
  private volatile boolean     running = true;
  private IOConsoleInputStream input;
  private BufferedReader       reader;
  private VMManager            manager;
  private Thread               thread;

  public ConsoleReader(IOConsole console, VMManager manager) {
    this.manager = manager;
    this.input = console.getInputStream();
    try {
      this.reader = new BufferedReader(new InputStreamReader(this.input, console.getEncoding()));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException(Messages.ConsoleReader_Unsupported_Encoding);
    }
    thread = new Thread(this, Messages.ConsoleReader_Remote_Console + console.getName());
    thread.setDaemon(true);
    thread.start();
  }

  public void dispose() {
    running = false;
    thread.interrupt();
    try {
      input.close();
    } catch (IOException e) {
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  public void run() {
    while (running) {
      try {
        String line;
        while ((line = reader.readLine()) != null) {
          manager.executeFrameworkCommand(line);
        }
      } catch (IOException e1) {
        if (running) {
          try {
            // just check for IO exception
            input.available();
          } catch (IOException e2) {
            running = false;
            try {
              // just to be sure
              input.close();
            } catch (IOException e3) {
            }
          }
          if (running) {
            FrameworkPlugin.error(Messages.ConsoleReader_Error_Reading_User_Input, e1);
          }
        }
      } catch (IAgentException e) {
        if (running) {
          FrameworkPlugin.error(Messages.ConsoleReader_Command_Execution_Failed, e);
        }
      }
    }
  }

}
