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

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

public class OSGiLog implements Log {
	private LogService logService;

	public OSGiLog(BundleContext context) {
		String logServiceClass = LogService.class.getName();

		if (context == null) {
			throw new IllegalArgumentException("context is null");
		}

		ServiceReference ref = context.getServiceReference(logServiceClass);
		if (ref != null) {
			logService = (LogService) context.getService(ref);
		}

		if (logService == null) {
			throw new IllegalStateException("LogService is not available");
		}
	}

	public void log(int severity, String msg, Throwable t) {
		try {
			logService.log(getLogServiceSeverity(severity), msg, t);
		} catch (Exception ex) {
			// Logging failed
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
}
