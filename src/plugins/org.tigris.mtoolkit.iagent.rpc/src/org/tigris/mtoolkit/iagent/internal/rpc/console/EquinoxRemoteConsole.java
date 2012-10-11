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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.framework.internal.core.AbstractBundle;
import org.eclipse.osgi.framework.internal.core.Framework;
import org.eclipse.osgi.framework.internal.core.FrameworkCommandProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.tigris.mtoolkit.iagent.pmp.PMPException;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;

public final class EquinoxRemoteConsole extends RemoteConsoleServiceBase {
  private ServiceTracker           providersTrack;
  private BundleContext            context;
  private FrameworkCommandProvider frameworkProvider;
  private boolean                  fwProviderInitializationTried = false;

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.internal.rpc.console.RemoteConsoleServiceBase#register(org.osgi.framework.BundleContext)
   */
  public void register(BundleContext bundleContext) {
    // make sure we are on equinox
    Framework.class.getName();
    this.context = bundleContext;
    providersTrack = new ServiceTracker(bundleContext, CommandProvider.class.getName(), null);
    super.register(bundleContext);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.internal.rpc.console.RemoteConsoleServiceBase#unregister()
   */
  public void unregister() {
    super.unregister();
    providersTrack.close();
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.internal.rpc.console.RemoteConsoleServiceBase#registerOutput(org.tigris.mtoolkit.iagent.pmp.RemoteObject)
   */
  public void registerOutput(RemoteObject remoteObject) throws PMPException {
    super.registerOutput(remoteObject);
    printPrompt();
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteConsole#executeCommand(java.lang.String)
   */
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
}
