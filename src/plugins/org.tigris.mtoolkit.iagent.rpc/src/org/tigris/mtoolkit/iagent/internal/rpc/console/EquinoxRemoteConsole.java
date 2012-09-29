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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.framework.internal.core.AbstractBundle;
import org.eclipse.osgi.framework.internal.core.Framework;
import org.eclipse.osgi.framework.internal.core.FrameworkCommandProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.tigris.mtoolkit.iagent.pmp.PMPConnection;
import org.tigris.mtoolkit.iagent.pmp.PMPException;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;
import org.tigris.mtoolkit.iagent.rpc.Remote;
import org.tigris.mtoolkit.iagent.rpc.RemoteConsole;

//TODO This class needs rework
public final class EquinoxRemoteConsole extends RemoteConsoleServiceBase implements Remote {
  private ServiceTracker           providersTrack;
  private BundleContext            context;

  private FrameworkCommandProvider frameworkProvider;
  private boolean                  fwProviderInitializationTried = false;

  private PrintStream              oldSystemOut;
  private PrintStream              oldSystemErr;
  private PrintStream              newSystemStream;
  private boolean                  replacedSystemOutputs         = false;

  public void register(BundleContext bundleContext) {
    // make sure we are on equinox
    Framework.class.getName();
    this.context = bundleContext;
    providersTrack = new ServiceTracker(bundleContext, CommandProvider.class.getName(), null);
    super.register(bundleContext);
  }

  public void unregister() {
    super.unregister();
    restoreSystemOutputs();
    providersTrack.close();
  }

  public void registerOutput(RemoteObject remoteObject) throws PMPException {
    super.registerOutput(remoteObject);
    replaceSystemOutputs();
    printPrompt();
  }

  public Class[] remoteInterfaces() {
    return new Class[] {
      RemoteConsole.class
    };
  }

  public void executeCommand(String line) {
    try {
      if (line == null || line.trim().length() == 0) {
        return;
      }
      providersTrack.open();
      Object[] providers;
      providers = getCommandProviders();
      CommandInterpreter interpreter = new EquinoxCommandInterpreter(line, providers, this);
      String nextLine = interpreter.nextArgument();
      if (nextLine != null) {
        interpreter.execute(nextLine);
      }
    } finally {
      printPrompt();
    }
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

  private synchronized void replaceSystemOutputs() {
    if (!replacedSystemOutputs) {
      if (newSystemStream == null) {
        newSystemStream = new PrintStream(new RedirectedSystemOutput());
      }
      oldSystemOut = System.out;
      oldSystemErr = System.err;
      System.setOut(newSystemStream);
      System.setErr(newSystemStream);
      replacedSystemOutputs = true;
    }
  }

  private synchronized void restoreSystemOutputs() {
    if (replacedSystemOutputs) {
      if (System.out == newSystemStream) {
        System.setOut(oldSystemOut);
      }
      if (System.err == newSystemStream) {
        System.setErr(oldSystemErr);
      }
      replacedSystemOutputs = false;
    }
  }

  private Object[] getCommandProviders() {
    Object[] providers;
    synchronized (providersTrack) {
      providers = providersTrack.getServices();
      if (!fwProviderInitializationTried) {
        fwProviderInitializationTried = true;
        boolean frameworkProviderRegistered = false;
        if (providers != null) {
          for (int i = 0; i < providers.length; i++) {
            if (providers[i] instanceof FrameworkCommandProvider) {
              frameworkProviderRegistered = true;
              break;
            }
          }
        }
        if (!frameworkProviderRegistered) {
          try {
            Framework fw = getEquinoxFramework(context);
            if (fw != null) {
              frameworkProvider = (FrameworkCommandProvider) invokeConstructor(FrameworkCommandProvider.class, fw);
              if (frameworkProvider != null) {
                try {
                  // the mistake is correct (it is
                  // "intialize", not "initialize")
                  invokeMethod(frameworkProvider, "intialize");
                } catch (Exception e) {
                  // On 3.6 method is called start()
                  invokeMethod(frameworkProvider, "start");
                }
              }
            }
          } catch (Throwable e) {
            // TODO: Add logging
            e.printStackTrace();
          }
        }
      }
    }
    return providers;
  }

  private void printPrompt() {
    print(System.getProperty("line.separator") + "osgi> ");
  }

  private Framework getEquinoxFramework(BundleContext context) {
    Bundle bundle = context.getBundle();
    if (bundle instanceof AbstractBundle) {
      Framework framework = (Framework) getFieldValue(bundle, "framework");
      return framework;
    } else {
      return null;
    }
  }

  private Object getFieldValue(Object obj, String fieldName) {
    Class c = obj.getClass();
    Field f = null;
    try {
      f = c.getDeclaredField(fieldName);
      return f.get(obj);
    } catch (NoSuchFieldException e) {
      // TODO: Add logging
      e.printStackTrace();
      return null;
    } catch (IllegalAccessException e) {
      f.setAccessible(true);
      try {
        return f.get(obj);
      } catch (IllegalAccessException e1) {
        // TODO: Add logging
        e1.printStackTrace();
        return null;
      }
    }
  }

  private Object invokeConstructor(Class clazz, Object parameter) throws Exception {
    try {
      Constructor c = clazz.getConstructor(new Class[] {
        parameter.getClass()
      });
      return c.newInstance(new Object[] {
        parameter
      });
    } catch (InvocationTargetException e) {
      if (e.getTargetException() instanceof Exception) {
        throw (Exception) e.getTargetException();
      }
      // TODO: Add logging
      e.getTargetException().printStackTrace();
      return null;
    }
  }

  private static Object invokeMethod(Object obj, String methodName) throws Exception {
    Method method = obj.getClass().getDeclaredMethod(methodName, null);
    method.setAccessible(true);
    return method.invoke(obj, null);
  }

  private class RedirectedSystemOutput extends OutputStream {
    private final byte[] singleByte = new byte[1];

    public synchronized void write(byte[] var0, int var1, int var2) throws IOException {
      oldSystemOut.write(var0, var1, var2);
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
     * @see java.io.OutputStream#flush()
     */
    public synchronized void flush() throws IOException {
      oldSystemOut.flush();
    }
  }
}
