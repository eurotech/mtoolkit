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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Enumeration;

public class DebugUtils {

	public static final boolean DEBUG = Boolean.getBoolean("iagent.debug");

	public static final void log(Object module, String message) {
		log(module, message, null);
	}

	public static final void log(Object module, String message, Throwable e) {
		if (!DEBUG)
			return;
		String id;
		if (!(module instanceof String)) {
			id = getIndentityString(module);
		} else {
			id = (String) module;
		}
		debug("[" + id + "]" + message, e);
	}

	/**
	 * Print specified message and throwable to system out.
	 * 
	 * @param msg
	 * @param t
	 */
	private static void debug(String msg, Throwable t) {
		if (DEBUG) {
			System.out.println("[IA|DEBUG]" + msg);
			if (t != null) {
				t.printStackTrace(System.out);
			}
		}
	}

	public static String convertForDebug(long[] arr) {
		if (!DEBUG)
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
		if (!DEBUG)
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
			} else if (element != null
							&& element.getClass().isArray()
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
		if (!DEBUG)
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
			} else if (value != null
							&& value.getClass().isArray()
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

	public static String getIndentityString(Object obj) {
		if (!DEBUG)
			return "(debug disabled)";
		if (obj == null)
			return "(null)";
		if (obj instanceof Class) {
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
					err.append(lineSeparator).append("Caused by: ").append(ex.toString());
				}
			}
		} catch (IllegalAccessException ex) {
		} catch (InvocationTargetException ex) {
		} catch (NoSuchMethodException ex) {
		}
		return err.toString();
	}	
	
	
	private static final String lineSeparator = System.getProperty("line.separator");
	
	public static String getStackTrace(Exception e) {
		StringBuffer err = new StringBuffer();
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		err.append(sw.toString());
		try {
			Method cause = e.getClass().getMethod("getCause", new Class[0]);
			if (cause != null) {
				Object ex = cause.invoke(e, new Object[0]);
				if (ex != null) {
					err.append(lineSeparator).append(getStackTrace((Exception) ex));
				}
			}
		} catch (IllegalAccessException ex) {
		} catch (InvocationTargetException ex) {
		} catch (NoSuchMethodException ex) {
		}
		return err.toString();
	}	  
}
