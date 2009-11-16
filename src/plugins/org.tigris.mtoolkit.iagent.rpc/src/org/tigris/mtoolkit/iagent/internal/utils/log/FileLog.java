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

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Date;

import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;

public class FileLog implements Log {
	private File logFile;
	Object lock = new Object();

	public FileLog(File logFile) {
		if (logFile == null) {
			throw new IllegalArgumentException("logFile is null");
		}
		this.logFile = logFile;
	}

	public void log(int severity, String msg, Throwable t) {
		synchronized (lock) {
			PrintWriter out = null;
			try {
				out = new PrintWriter(new FileWriter(logFile.getAbsolutePath(), true));
				out.println(getDateTime() + " " + getSeverityString(severity) + msg);
				if (t != null) {
					out.println(DebugUtils.getStackTrace(t));
				}
				out.flush();
			} catch (Exception ex) {
				// logging failed
			} finally {
				if (out != null) {
					out.close();
				}
			}
		}
	}

	private static String getDateTime() {
		return new Date().toString();
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
