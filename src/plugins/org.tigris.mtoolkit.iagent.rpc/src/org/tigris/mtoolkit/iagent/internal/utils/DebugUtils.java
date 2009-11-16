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
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;

import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

public class DebugUtils {

	public static final int INFO = 0;
	public static final int ERROR = 1;
	public static final int DEBUG = 2;

	private static final String PROP_DEBUG_ENABLED = "iagent.debug"; //$NON-NLS-1$
	private static final String PROP_LOG_FILE = "iagent.log.file"; //$NON-NLS-1$
	private static final String NL = System.getProperty("line.separator"); //$NON-NLS-1$

	public static final boolean DEBUG_ENABLED = Boolean.getBoolean(PROP_DEBUG_ENABLED);

	private static Object fwLog;
	private static LogService logService;
	private static File logFile;
	private static boolean initialized = false;
	private static Object lock = new Object();


	public static final void info(Object module, String message) {
		log(module, INFO, message);
	}

	public static final void info(Object module, String message, Throwable t) {
		log(module, INFO, message, t);
	}

	public static final void debug(Object module, String message) {
		log(module, DEBUG, message);
	}

	public static final void debug(Object module, String message, Throwable t) {
		log(module, DEBUG, message, t);
	}

	public static final void error(Object module, String message) {
		log(module, ERROR, message);
	}

	public static final void error(Object module, String message, Throwable t) {
		log(module, ERROR, message, t);
	}

	/**
	 * Initializes logging support. If not called only file logging or console
	 * logging will be available.
	 * 
	 * @param context
	 *            the bundle context. If null log service won't be used for
	 *            logging.
	 */
	public static void initialize(BundleContext context) {
		if (initialized) {
			return;
		}
		initialized = true;

		// First - try file logging
		String logFileName = System.getProperty(PROP_LOG_FILE);
		if (logFileName != null) {
			logFile = new File(logFileName);
			return;
		}

		// Second - try org.eclipse.osgi.framework.log.FrameworkLog
		fwLog = getFrameworkLog(context);
		if (fwLog != null) {
			return;
		}

		// Third - try org.osgi.service.log.LogService
		if (context != null) {
			ServiceReference ref = context.getServiceReference(LogService.class.getName());
			if (ref != null) {
				logService = (LogService) context.getService(ref);
				if (logService != null) {
					return;
				}
			}
		}
	}

	/**
	 * Logs message with given severity. Equivalent to log(module, severity,
	 * message, null)
	 * 
	 * @param module
	 *            can be String, Class, null or any Object
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
	 *            can be String, Class, null or any Object
	 * @param severity
	 * @param message
	 * @param e
	 *            exception, can be null
	 */
	public static final void log(Object module, int severity, String message, Throwable e) {
		if (severity == DEBUG && !DEBUG_ENABLED) {
			return;
		}
		if (!initialized) {
			initialize(null);
		}
		String logMessage = "[IAgent][" + getIdentityString(module) + "] " + message;

		if (fwLog != null) {
			logToEclipseFw(fwLog, severity, logMessage, e);
		} else if (logService != null) {
			logService.log(getLogServiceSeverity(severity), logMessage, e);
		} else if (logFile != null) {
			logToFile(logFile, severity, logMessage, e);
		} else {
			logToConsole(severity, logMessage, e);
		}
	}

	private static int getLogServiceSeverity(int severity) {
		switch (severity) {
		case INFO:
			return LogService.LOG_INFO;
		case ERROR:
			return LogService.LOG_ERROR;
		case DEBUG:
			return LogService.LOG_DEBUG;
		default:
			return LogService.LOG_ERROR;
		}
	}

	private static int getFrameworkSeverity(int severity) {
		switch (severity) {
		case INFO:
			return FrameworkLogEntry.INFO;
		case ERROR:
			return FrameworkLogEntry.ERROR;
		case DEBUG:
			return FrameworkLogEntry.INFO;
		default:
			return FrameworkLogEntry.ERROR;
		}
	}

	private static String getSeverityString(int severity) {
		switch (severity) {
		case INFO:
			return "[I]";
		case ERROR:
			return "[E]";
		case DEBUG:
			return "[D]";
		default:
			return "[E]";
		}
	}

	private static String getDateTime() {
		return new Date().toString();
	}

	private static Object getFrameworkLog(BundleContext context) {
		String fwClass = null;
		try {
			// check we are on eclipse
			fwClass = FrameworkLog.class.getName();
		} catch (Throwable t) {
			return null;
		}

		if (context != null) {
			ServiceReference ref = context.getServiceReference(fwClass);
			if (ref != null) {
				return context.getService(ref);
			}
		}
		// XXX: Other way to get FrameworkLog without context?

		return null;
	}

	private static void logToEclipseFw(Object fwLog, int severity, String msg, Throwable t) {
		try {
			int fwSeverity = getFrameworkSeverity(severity);
			FrameworkLogEntry logEntry = new FrameworkLogEntry("", fwSeverity, 0, msg, 0, t, null);
			((FrameworkLog) fwLog).log(logEntry);
		} catch (Exception ex) {
			// Logging to the console instead
			logToConsole(severity, msg, t);
		}
	}

	private static void logToFile(File file, int severity, String msg, Throwable t) {
		synchronized (lock) {
			PrintWriter out = null;
			try {
				out = new PrintWriter(new FileWriter(file.getAbsolutePath(), true));
				out.println(getDateTime() + " " + getSeverityString(severity) + msg);
				if (t != null) {
					out.println(getStackTrace(t));
				}
				out.flush();
			} catch (Exception ex) {
				// Logging to the console instead
				logToConsole(severity, msg, t);
			} finally {
				if (out != null) {
					out.close();
				}
			}
		}
	}

	private static void logToConsole(int severity, String msg, Throwable e) {
		if (severity == DEBUG || severity == INFO) {
			return;
		}
		synchronized (lock) {
			System.out.println(getSeverityString(severity) + msg);
			if (e != null) {
				e.printStackTrace(System.out);
			}
		}
	}

	public static String convertForDebug(long[] arr) {
		if (!DEBUG_ENABLED)
			return "(debug disabled)";
		if (arr == null)
			return "null";
		if (arr.length == 0)
			return "(none)";
		StringBuffer buf = new StringBuffer();
		buf.append('[');
		for (int i = 0; i < arr.length; i++) {
			if (i > 0)
				buf.append(',');
			buf.append(arr[i]);
		}
		buf.append(']');
		return buf.toString();
	}

	public static String convertForDebug(Object[] arr) {
		if (!DEBUG_ENABLED)
			return "(debug disabled)";
		if (arr == null)
			return "[null]";
		if (arr.length == 0)
			return "[(none)]";
		StringBuffer buf = new StringBuffer();
		buf.append('[');
		for (int i = 0; i < arr.length; i++) {
			if (i > 0)
				buf.append(',');
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
		if (arr == null)
			return "[null]";
		if (arr.length == 0)
			return "[(none)]";
		StringBuffer buf = new StringBuffer();
		buf.append('[');
		for (int i = 0; i < arr.length; i++) {
			if (i > 0)
				buf.append(',');
			buf.append(arr[i]);
		}
		buf.append(']');
		return buf.toString();
	}

	public static String convertForDebug(Dictionary dict) {
		if (!DEBUG_ENABLED)
			return "(debug disabled)";
		if (dict == null)
			return "{null}";
		if (dict.size() == 0)
			return "{(none)}";
		Enumeration e = dict.keys();
		StringBuffer buf = new StringBuffer();
		buf.append('{');
		while (e.hasMoreElements()) {
			Object key = e.nextElement();
			if (buf.length() > 1)
				buf.append(',');
			buf.append(key);
			buf.append('=');
			Object value = dict.get(key);
			String valueStr;
			if (value instanceof Dictionary) {
				valueStr = convertForDebug((Dictionary) value);
			} else if (value != null && value.getClass().isArray()
					&& !value.getClass().getComponentType().isPrimitive()) {
				valueStr = convertForDebug((Object[]) value);
			} else {
				valueStr = (value != null ? value.toString() : "null");
			}
			buf.append(valueStr);
		}
		buf.append('}');
		return buf.toString();
	}

	private static String getIdentityString(Object obj) {
		if (obj == null)
			return "(null)";
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
		if (idx != -1)
			return fullClassName.substring(idx + 1, fullClassName.length());
		else
			return fullClassName;
	}

	public static String toString(Exception e) {
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
}
