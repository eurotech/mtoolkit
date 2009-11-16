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

import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class EclipseLog implements Log {
	private FrameworkLog log;

	public EclipseLog(BundleContext context) {
		// check we are on eclipse
		String fwClass = FrameworkLog.class.getName();

		if (context == null) {
			// XXX: Other way to get FrameworkLog without context?
			throw new IllegalArgumentException("context is null");
		}

		ServiceReference ref = context.getServiceReference(fwClass);
		if (ref != null) {
			log = (FrameworkLog) context.getService(ref);
		}

		if (log == null) {
			throw new IllegalStateException("FrameworkLog service is not available");
		}
	}

	public void log(int severity, String msg, Throwable t) {
		try {
			int fwSeverity = getFrameworkSeverity(severity);
			FrameworkLogEntry logEntry = new FrameworkLogEntry("", fwSeverity, 0, msg, 0, t, null);
			log.log(logEntry);
		} catch (Exception ex) {
			// Logging failed
		}
	}

	private int getFrameworkSeverity(int severity) {
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
}