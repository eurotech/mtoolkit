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
package org.tigris.mtoolkit.iagent.internal.utils;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.framework.BundleContext;
import org.tigris.mtoolkit.iagent.internal.utils.log.ConsoleLog;
import org.tigris.mtoolkit.iagent.internal.utils.log.FileLog;
import org.tigris.mtoolkit.iagent.internal.utils.log.Log;
import org.tigris.mtoolkit.iagent.internal.utils.log.OSGiLog;

public final class DebugUtils {
  private static final String PROP_DEBUG_ENABLED = "iagent.debug";                        //$NON-NLS-1$
  private static final String PROP_LOG_FILE      = "iagent.log.file";                     //$NON-NLS-1$
  private static final String NL                 = System.getProperty("line.separator");  //$NON-NLS-1$
  public static final boolean DEBUG_ENABLED      = Boolean.getBoolean(PROP_DEBUG_ENABLED);

  private static Log          log;
  private static boolean      initialized        = false;

  public static final void info(Object module, String message) {
    log(module, Log.INFO, message);
  }

  public static final void info(Object module, String message, Throwable t) {
    log(module, Log.INFO, message, t);
  }

  public static final void debug(Object module, String message) {
    log(module, Log.DEBUG, message);
  }

  public static final void debug(Object module, String message, Throwable t) {
    log(module, Log.DEBUG, message, t);
  }

  public static final void error(Object module, String message) {
    log(module, Log.ERROR, message);
  }

  public static final void error(Object module, String message, Throwable t) {
    log(module, Log.ERROR, message, t);
  }

  /**
   * Initializes logging support.
   *
   * @param context
   *          the bundle context. If null log service won't be used for logging.
   */
  public static void initialize(BundleContext context) {
    if (initialized) {
      return;
    }
    initialized = true;

    // First - try file logging
    try {
      String logFileName = System.getProperty(PROP_LOG_FILE);
      if (logFileName != null) {
        log = new FileLog(new File(logFileName));
        return;
      }
    } catch (Throwable t) { // Continue with other loggers
    }

    // Third - try org.osgi.service.log.LogService
    try {
      log = new OSGiLog(context);
      return;
    } catch (Throwable t) { // Continue with other loggers
    }

    // Fourth - logging to console
    log = ConsoleLog.getDefault();
  }

  /**
   * Closes any opened logging utilities.
   */
  public static void dispose() {
    initialized = false;
    if (log != null) {
      log.close();
    }
  }

  /**
   * Logs message with given severity. Equivalent to log(module, severity,
   * message, null)
   *
   * @param module
   *          can be String, Class, null or any Object
   * @param severity
   * @param message
   */
  public static final void log(Object module, int severity, String message) {
    log(module, severity, message, null);
  }

  /**
   * Logs message with given severity.
   *
   * @param module
   *          can be String, Class, null or any Object
   * @param severity
   * @param message
   * @param t
   *          exception, can be null
   */
  public static final void log(Object module, int severity, String message, Throwable t) {
    if (severity == Log.DEBUG && !DEBUG_ENABLED) {
      return;
    }
    if (!initialized) {
      return;
    }
    String logMessage = "[IAgent][" + getIdentityString(module) + "] " + message;

    if (log != null) {
      log.log(severity, logMessage, t);
    }
  }

  public static String convertForDebug(long[] arr) {
    if (!DEBUG_ENABLED) {
      return "(debug disabled)";
    }
    if (arr == null) {
      return "null";
    }
    if (arr.length == 0) {
      return "(none)";
    }
    StringBuffer buf = new StringBuffer();
    buf.append('[');
    for (int i = 0; i < arr.length; i++) {
      if (i > 0) {
        buf.append(',');
      }
      buf.append(arr[i]);
    }
    buf.append(']');
    return buf.toString();
  }

  public static String convertForDebug(Object[] arr) {
    if (!DEBUG_ENABLED) {
      return "(debug disabled)";
    }
    if (arr == null) {
      return "[null]";
    }
    if (arr.length == 0) {
      return "[(none)]";
    }
    StringBuffer buf = new StringBuffer();
    buf.append('[');
    for (int i = 0; i < arr.length; i++) {
      if (i > 0) {
        buf.append(',');
      }
      Object element = arr[i];
      String elementStr;
      if (element instanceof Dictionary) {
        elementStr = convertForDebug((Dictionary) element);
      } else if (element != null && element.getClass().isArray()
          && !element.getClass().getComponentType().isPrimitive()) {
        elementStr = convertForDebug((Object[]) element);
      } else {
        elementStr = (element != null ? element.toString() : "null");
      }
      buf.append(elementStr);
    }
    buf.append(']');
    return buf.toString();
  }

  public static String flattenStringArray(String[] arr) {
    if (arr == null) {
      return "[null]";
    }
    if (arr.length == 0) {
      return "[(none)]";
    }
    StringBuffer buf = new StringBuffer();
    buf.append('[');
    for (int i = 0; i < arr.length; i++) {
      if (i > 0) {
        buf.append(',');
      }
      buf.append(arr[i]);
    }
    buf.append(']');
    return buf.toString();
  }

  public static String convertForDebug(Dictionary dict) {
    if (!DEBUG_ENABLED) {
      return "(debug disabled)";
    }
    if (dict == null) {
      return "{null}";
    }
    if (dict.size() == 0) {
      return "{(none)}";
    }
    Enumeration e = dict.keys();
    StringBuffer buf = new StringBuffer();
    buf.append('{');
    while (e.hasMoreElements()) {
      Object key = e.nextElement();
      if (buf.length() > 1) {
        buf.append(',');
      }
      buf.append(key);
      buf.append('=');
      Object value = dict.get(key);
      String valueStr;
      if (value instanceof Dictionary) {
        valueStr = convertForDebug((Dictionary) value);
      } else if (value != null && value.getClass().isArray() && !value.getClass().getComponentType().isPrimitive()) {
        valueStr = convertForDebug((Object[]) value);
      } else {
        valueStr = (value != null ? value.toString() : "null");
      }
      buf.append(valueStr);
    }
    buf.append('}');
    return buf.toString();
  }

  public static String toString(Throwable e) {
    StringBuffer err = new StringBuffer();
    err.append(e.toString());
    try {
      Method cause = e.getClass().getMethod("getCause", new Class[0]);
      if (cause != null) {
        Object ex = cause.invoke(e, new Object[0]);
        if (ex != null) {
          err.append(NL).append("Caused by: ").append(ex.toString());
        }
      }
    } catch (IllegalAccessException ex) {
    } catch (InvocationTargetException ex) {
    } catch (NoSuchMethodException ex) {
    }
    return err.toString();
  }

  public static String getStackTrace(Throwable e) {
    StringBuffer err = new StringBuffer();
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    err.append(sw.toString());
    try {
      Method cause = e.getClass().getMethod("getCause", new Class[0]);
      if (cause != null) {
        Object ex = cause.invoke(e, new Object[0]);
        if (ex instanceof Throwable) {
          err.append(NL).append(getStackTrace((Throwable) ex));
        }
      }
    } catch (IllegalAccessException ex) {
    } catch (InvocationTargetException ex) {
    } catch (NoSuchMethodException ex) {
    }
    return err.toString();
  }

  private static String getIdentityString(Object obj) {
    if (obj == null) {
      return "(null)";
    }
    if (obj instanceof String) {
      return (String) obj;
    } else if (obj instanceof Class) {
      return getClassName(((Class) obj).getName());
    } else {
      Class cl = obj.getClass();
      return getClassName(cl.getName()) + '@' + Integer.toHexString(System.identityHashCode(obj));
    }
  }

  private static String getClassName(String fullClassName) {
    int idx = fullClassName.lastIndexOf('.');
    if (idx != -1) {
      return fullClassName.substring(idx + 1, fullClassName.length());
    } else {
      return fullClassName;
    }
  }
}
