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
package org.tigris.mtoolkit.iagent.internal.utils.log;

public final class ConsoleLog implements Log {
  private static final ConsoleLog instance = new ConsoleLog();

  private final Object            lock     = new Object();

	private ConsoleLog() {
		// singleton
	}

	public static ConsoleLog getDefault() {
		return instance;
	}

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.internal.utils.log.Log#log(int, java.lang.String, java.lang.Throwable)
   */
	public void log(int severity, String msg, Throwable e) {
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

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.internal.utils.log.Log#close()
   */
	public void close() {
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
}
