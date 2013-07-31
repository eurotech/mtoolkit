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
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.tigris.mtoolkit.iagent.internal.pmp.InvocationThread;
import org.tigris.mtoolkit.iagent.internal.rpc.Activator;
import org.tigris.mtoolkit.iagent.internal.utils.CircularBuffer;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.internal.utils.ThreadUtils;
import org.tigris.mtoolkit.iagent.pmp.EventListener;
import org.tigris.mtoolkit.iagent.pmp.PMPConnection;
import org.tigris.mtoolkit.iagent.pmp.PMPException;
import org.tigris.mtoolkit.iagent.pmp.RemoteMethod;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;
import org.tigris.mtoolkit.iagent.rpc.Capabilities;
import org.tigris.mtoolkit.iagent.rpc.Remote;
import org.tigris.mtoolkit.iagent.rpc.RemoteCapabilitiesManager;
import org.tigris.mtoolkit.iagent.rpc.RemoteConsole;

public abstract class RemoteConsoleServiceBase implements Remote, RemoteConsole, EventListener {
  private static final Integer OFFSET                = new Integer(0);

  private ServiceRegistration  registration;

  private final Map            dispatchers           = new HashMap();

  private PrintStream          oldSystemOut;
  private PrintStream          newSystemOut;
  private PrintStream          oldSystemErr;
  private PrintStream          newSystemErr;
  private boolean              replacedSystemOutputs = false;

  public void register(BundleContext context) {
    registration = context.registerService(RemoteConsole.class.getName(), this, null);
    RemoteCapabilitiesManager capMan = Activator.getCapabilitiesManager();
    if (capMan != null) {
      capMan.setCapability(Capabilities.CONSOLE_SUPPORT, new Boolean(true));
    }
  }

  public void unregister() {
    registration.unregister();
    restoreSystemOutputs();
    RemoteCapabilitiesManager capMan = Activator.getCapabilitiesManager();
    if (capMan != null) {
      capMan.setCapability(Capabilities.CONSOLE_SUPPORT, new Boolean(false));
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.Remote#remoteInterfaces()
   */
  public Class[] remoteInterfaces() {
    return new Class[] {
      RemoteConsole.class
    };
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.pmp.EventListener#event(java.lang.Object, java.lang.String)
   */
  public void event(Object event, String evType) {
    if (PMPConnection.FRAMEWORK_DISCONNECTED.equals(evType)) {
      doReleaseConsole((PMPConnection) event);
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteConsole#registerOutput(org.tigris.mtoolkit.iagent.pmp.RemoteObject)
   */
  public void registerOutput(RemoteObject remoteObject) throws PMPException {
    PMPConnection conn = InvocationThread.getContext().getConnection();
    WriteDispatcher dispatcher = createDispatcher(conn, new CircularBuffer(), remoteObject);
    dispatcher.start();
    conn.addEventListener(this, new String[] {
      PMPConnection.FRAMEWORK_DISCONNECTED
    });
    synchronized (dispatchers) {
      WriteDispatcher oldDispatcher = (WriteDispatcher) dispatchers.put(conn, dispatcher);
      if (oldDispatcher != null) {
        oldDispatcher.finish();
      }
      replaceSystemOutputs();
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteConsole#releaseConsole()
   */
  public final void releaseConsole() {
    doReleaseConsole(InvocationThread.getContext().getConnection());
  }

  protected void doReleaseConsole(PMPConnection conn) {
    synchronized (dispatchers) {
      WriteDispatcher dispatcher = (WriteDispatcher) dispatchers.remove(conn);
      if (dispatcher != null) {
        dispatcher.finish();
      }
      if (dispatchers.size() == 0) {
        restoreSystemOutputs();
      }
    }
  }

  protected void print(String msg) {
    // TODO: Handle different encodings
    byte[] msgBytes = msg.getBytes();
    print(msgBytes, 0, msgBytes.length);
  }

  protected void print(byte[] buf, int off, int len) {
    PMPConnection conn = InvocationThread.getContext().getConnection();
    WriteDispatcher dispatcher;
    synchronized (dispatchers) {
      dispatcher = (WriteDispatcher) dispatchers.get(conn);
      if (dispatcher == null) {
        return;
      }
    }
    dispatcher.buffer.write(buf, off, len);
    synchronized (dispatcher) {
      dispatcher.notifyAll();
    }
  }

  protected WriteDispatcher createDispatcher(PMPConnection conn, CircularBuffer buffer, RemoteObject remoteObject)
      throws PMPException {
    return new WriteDispatcher(conn, buffer, remoteObject);
  }

  protected WriteDispatcher getDispatcher(PMPConnection conn) {
    synchronized (dispatchers) {
      return (WriteDispatcher) dispatchers.get(conn);
    }
  }

  protected List/* <WriteDispatcher> */getDispatchers() {
    synchronized (dispatchers) {
      return new ArrayList(dispatchers.values());
    }
  }

  private synchronized void replaceSystemOutputs() {
    //Used to handle system output stream which is not redirected to parser service
    if (!replacedSystemOutputs) {
      if (newSystemOut == null) {
        newSystemOut = new PrintStream(new RedirectedSystemOutput(System.out, dispatchers));
      }
      if (System.out != newSystemOut) {
        oldSystemOut = System.out;
        System.setOut(newSystemOut);
      }
      if (newSystemErr == null) {
        newSystemErr = new PrintStream(new RedirectedSystemOutput(System.err, dispatchers));
      }
      if (System.err != newSystemErr) {
        oldSystemErr = System.err;
        System.setErr(newSystemErr);
      }
      replacedSystemOutputs = true;
    }
  }

  private synchronized void restoreSystemOutputs() {
    //Used to handle system output stream which is not redirected to parser service
    if (replacedSystemOutputs) {
      if (System.out == newSystemOut) {
        System.setOut(oldSystemOut);
      }
      if (System.err == newSystemErr) {
        System.setErr(oldSystemErr);
      }
      replacedSystemOutputs = false;
    }
  }

  protected class WriteDispatcher implements Runnable {
    public PMPConnection     conn;
    public CircularBuffer    buffer;
    public RemoteMethod      method;
    public RemoteObject      object;
    private Thread           dispatcherThread;

    private volatile boolean running = true;

    public WriteDispatcher(PMPConnection conn, CircularBuffer buffer, RemoteObject object) throws PMPException {
      dispatcherThread = ThreadUtils.createThread(this, "Remote Console Dispatcher");
      this.conn = conn;
      this.buffer = buffer;
      this.object = object;
      method = object.getMethod("write", new String[] {
          byte[].class.getName(), Integer.TYPE.getName(), Integer.TYPE.getName()
      });
    }

    public void start() {
      dispatcherThread.start();
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
      while (true) {
        synchronized (this) {
          while (buffer.available() <= 0 && running && conn.isConnected()) {
            try {
              wait();
            } catch (InterruptedException e) {
            }
          }
        }
        if (!running || !conn.isConnected()) {
          try {
            object.dispose();
          } catch (PMPException e) {
            DebugUtils.error(this, e.getMessage(), e);
          }
          return;
        }
        byte[] buf = new byte[1024];
        while (buffer.available() > 0) {
          int read = buffer.read(buf);
          try {
            method.invoke(new Object[] {
                buf, OFFSET, new Integer(read)
            }, true);
          } catch (PMPException e) {
            DebugUtils.error(this, e.getMessage(), e);
          }
        }
      }
    }

    public synchronized void finish() {
      running = false;
      notifyAll();
    }
  }

  private static final class RedirectedSystemOutput extends OutputStream {
    private final byte[]       singleByte = new byte[1];
    private final Map          dispatchers;
    private final OutputStream base;

    public RedirectedSystemOutput(OutputStream base, Map dispatchers) {
      this.base = base;
      this.dispatchers = dispatchers;
    }

    /* (non-Javadoc)
     * @see java.io.OutputStream#write(byte[])
     */
    public synchronized void write(byte[] var0) throws IOException {
      write(var0, 0, var0.length);
    }

    /* (non-Javadoc)
     * @see java.io.OutputStream#write(int)
     */
    public synchronized void write(int arg0) throws IOException {
      singleByte[0] = (byte) (arg0 & 0xFF);
      write(singleByte, 0, 1);
    }

    /* (non-Javadoc)
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    public synchronized void write(byte[] var0, int var1, int var2) throws IOException {
      if (base != null) {
        base.write(var0, var1, var2);
      }
      synchronized (dispatchers) {
        for (Iterator it = dispatchers.values().iterator(); it.hasNext();) {
          WriteDispatcher dispatcher = (WriteDispatcher) it.next();
          dispatcher.buffer.write(var0, var1, var2);
          synchronized (dispatcher) {
            dispatcher.notifyAll();
          }
        }
      }
    }

    /* (non-Javadoc)
     * @see java.io.OutputStream#flush()
     */
    public synchronized void flush() throws IOException {
      if (base != null) {
        base.flush();
      }
    }
  }
}
