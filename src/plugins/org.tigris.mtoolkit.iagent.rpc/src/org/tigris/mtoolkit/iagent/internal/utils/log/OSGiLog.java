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
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class OSGiLog implements Log, ServiceTrackerCustomizer {
	private BundleContext bc;
	private LogService logService;
	private ServiceTracker logTracker;

	public OSGiLog(BundleContext context) {
		String logServiceClass = LogService.class.getName();

		if (context == null) {
			throw new IllegalArgumentException("context is null");
		}

		this.bc = context;

		logTracker = new ServiceTracker(context, logServiceClass, this);
		logTracker.open();
	}

	public void log(int severity, String msg, Throwable t) {
		synchronized (this) {
			if (logService != null) {
				try {
					logService.log(getLogServiceSeverity(severity), msg, t);
					return;
				} catch (Exception ex) {
					// Logging failed
				}
			}
		}
		ConsoleLog.getDefault().log(severity, msg, t);
	}

	public Object addingService(ServiceReference reference) {
		Object service = bc.getService(reference);
		synchronized (this) {
			if (this.logService == null) {
				this.logService = (LogService) service;
			}
		}
		return service;
	}

	public void modifiedService(ServiceReference reference, Object service) {
	}

	public void removedService(ServiceReference reference, Object service) {
		synchronized (this) {
			if (this.logService == service) {
				this.logService = (LogService) logTracker.getService();
			}
		}
		bc.ungetService(reference);
	}

	public void close() {
		if (logTracker != null) {
			logTracker.close();
			logTracker = null;
		}
		bc = null;
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
