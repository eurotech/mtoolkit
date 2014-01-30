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
package org.tigris.mtoolkit.iagent.internal.rpc.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.osgi.framework.console.ConsoleSession;
import org.osgi.framework.BundleContext;
import org.tigris.mtoolkit.iagent.internal.pmp.InvocationThread;
import org.tigris.mtoolkit.iagent.internal.utils.CircularBuffer;
import org.tigris.mtoolkit.iagent.pmp.PMPConnection;
import org.tigris.mtoolkit.iagent.pmp.PMPException;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;

public final class EquinoxRemoteConsole extends RemoteConsoleServiceBase {
  private BundleContext context;
  protected final Map   consoles = new HashMap();

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.internal.rpc.console.RemoteConsoleServiceBase#register(org.osgi.framework.BundleContext)
   */
  public void register(BundleContext bundleContext) {
    // make sure we are on equinox
    ConsoleSession.class.getName();
    this.context = bundleContext;
    super.register(bundleContext);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.internal.rpc.console.RemoteConsoleServiceBase#doReleaseConsole(org.tigris.mtoolkit.iagent.pmp.PMPConnection)
   */
  protected void doReleaseConsole(PMPConnection conn) {
    super.doReleaseConsole(conn);
    EquinoxConsoleSession console;
    synchronized (consoles) {
      console = (EquinoxConsoleSession) consoles.get(conn);
    }
    if (console != null) {
      console.close();
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteConsole#registerOutput(org.tigris.mtoolkit.iagent.pmp.RemoteObject)
   */
  public void registerOutput(RemoteObject remoteObject) throws PMPException {
    super.registerOutput(remoteObject);
    PMPConnection conn = InvocationThread.getContext().getConnection();
    EquinoxConsoleSession console;
    synchronized (consoles) {
      console = (EquinoxConsoleSession) consoles.get(conn);
      if (console != null) {
        console.close();
      }
      console = new EquinoxConsoleSession(new ConsoleInputStream(), newSystemOut);
      consoles.put(conn, console);
    }
    context.registerService(ConsoleSession.class.getName(), console, null);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.internal.rpc.console.RemoteConsoleServiceBase#replaceSystemOutputs()
   */
  protected synchronized void replaceSystemOutputs() {
    if (!replacedSystemOutputs) {
      if (newSystemOut == null) {
        newSystemOut = new PrintStream(new RedirectedSystemOutput(null, dispatchers));
      }
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.internal.rpc.console.RemoteConsoleServiceBase#restoreSystemOutputs()
   */
  protected synchronized void restoreSystemOutputs() {
    //Used to handle system output stream which is not redirected to parser service
    if (replacedSystemOutputs) {
      newSystemOut = null;
      replacedSystemOutputs = false;
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteConsole#executeCommand(java.lang.String)
   */
  public void executeCommand(String line) {
    try {
      if (line == null) {
        return;
      }
      line = line.trim();
      if (line.length() == 0) {
        return;
      }
      PMPConnection conn = InvocationThread.getContext().getConnection();
      EquinoxConsoleSession console;
      synchronized (consoles) {
        console = (EquinoxConsoleSession) consoles.get(conn);
      }
      if (console != null) {
        console.sendCommand(line + System.getProperty("line.separator")); //$NON-NLS-1$
      }
    } finally {
      printPrompt();
    }
  }

  private void printPrompt() {
    print(System.getProperty("line.separator") + "osgi> "); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private static class EquinoxConsoleSession extends ConsoleSession {
    private final ConsoleInputStream in;
    private final OutputStream       out;

    public EquinoxConsoleSession(ConsoleInputStream in, OutputStream out) {
      this.in = in;
      this.out = out;
    }

    /* (non-Javadoc)
     * @see org.eclipse.osgi.framework.console.ConsoleSession#doClose()
     */
    protected void doClose() {
      try {
        in.close();
      } catch (IOException e1) {
      }
    }

    /* (non-Javadoc)
     * @see org.eclipse.osgi.framework.console.ConsoleSession#getInput()
     */
    public InputStream getInput() {
      return in;
    }

    /* (non-Javadoc)
     * @see org.eclipse.osgi.framework.console.ConsoleSession#getOutput()
     */
    public OutputStream getOutput() {
      return out;
    }

    public void sendCommand(String cmd) {
      byte[] cmdBytes = cmd.getBytes();
      in.sendCommand(cmdBytes);
    }
  }

  private static class ConsoleInputStream extends InputStream {
    private final byte[]         SINGLE_BYTE = new byte[1];
    private final CircularBuffer cb          = new CircularBuffer();

    /* (non-Javadoc)
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException {
      int bytesRead = cb.read(SINGLE_BYTE);
      return (bytesRead == 1) ? SINGLE_BYTE[0] : -1;
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#read(byte[], int, int)
     */
    public int read(byte b[], int off, int len) throws IOException {
      return cb.read(b, off, len);
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#read(byte[])
     */
    public int read(byte b[]) throws IOException {
      return read(b, 0, b.length);
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#close()
     */
    public void close() throws IOException {
      cb.release();
    }

    public void sendCommand(byte[] cmdBytes) {
      cb.write(cmdBytes, 0, cmdBytes.length);
    }
  }
}
