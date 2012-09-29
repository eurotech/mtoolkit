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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.tigris.mtoolkit.iagent.internal.pmp.InvocationThread;
import org.tigris.mtoolkit.iagent.internal.rpc.Activator;
import org.tigris.mtoolkit.iagent.internal.utils.CircularBuffer;
import org.tigris.mtoolkit.iagent.internal.utils.ThreadUtils;
import org.tigris.mtoolkit.iagent.pmp.EventListener;
import org.tigris.mtoolkit.iagent.pmp.PMPConnection;
import org.tigris.mtoolkit.iagent.pmp.PMPException;
import org.tigris.mtoolkit.iagent.pmp.RemoteMethod;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;
import org.tigris.mtoolkit.iagent.rpc.Capabilities;
import org.tigris.mtoolkit.iagent.rpc.RemoteCapabilitiesManager;
import org.tigris.mtoolkit.iagent.rpc.RemoteConsole;

public abstract class RemoteConsoleServiceBase implements RemoteConsole, EventListener {
  private ServiceRegistration registration;
  protected final Map         dispatchers = new HashMap();

  public void register(BundleContext context) {
    registration = context.registerService(RemoteConsole.class.getName(), this, null);

    RemoteCapabilitiesManager capMan = Activator.getCapabilitiesManager();
    if (capMan != null) {
      capMan.setCapability(Capabilities.CONSOLE_SUPPORT, new Boolean(true));
    }
  }

  public void unregister() {
    registration.unregister();
    RemoteCapabilitiesManager capMan = Activator.getCapabilitiesManager();
    if (capMan != null) {
      capMan.setCapability(Capabilities.CONSOLE_SUPPORT, new Boolean(false));
    }
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
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteConsole#releaseConsole()
   */
  public synchronized final void releaseConsole() {
    PMPConnection conn = InvocationThread.getContext().getConnection();
    doReleaseConsole(conn);
  }

  protected WriteDispatcher createDispatcher(PMPConnection conn, CircularBuffer buffer, RemoteObject remoteObject)
      throws PMPException {
    return new WriteDispatcher(conn, buffer, remoteObject);
  }

  protected void doReleaseConsole(PMPConnection conn) {
    synchronized (dispatchers) {
      WriteDispatcher dispatcher = (WriteDispatcher) dispatchers.remove(conn);
      if (dispatcher != null) {
        dispatcher.finish();
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
            // TODO: Log exception
          }
          return;
        }
        byte[] buf = new byte[1024];
        while (buffer.available() > 0) {
          int read = buffer.read(buf);
          try {
            method.invoke(new Object[] {
                buf, new Integer(0), new Integer(read)
            }, true);
          } catch (PMPException e) {
            // TODO: Log exception
          }
        }
      }
    }

    public synchronized void finish() {
      running = false;
      notifyAll();
    }
  }
}
