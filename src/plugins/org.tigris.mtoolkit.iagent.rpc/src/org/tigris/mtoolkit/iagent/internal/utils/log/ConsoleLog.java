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

public class ConsoleLog implements Log {
	Object lock = new Object();

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
