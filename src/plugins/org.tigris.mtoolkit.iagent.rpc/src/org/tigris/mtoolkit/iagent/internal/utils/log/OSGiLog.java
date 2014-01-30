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
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.tigris.mtoolkit.iagent.internal.rpc.Messages;

public final class OSGiLog implements Log {
  private final ServiceTracker logTracker;

  public OSGiLog(BundleContext context) {
    final String logServiceClass = LogService.class.getName();

    if (context == null) {
      throw new NullPointerException(Messages.getString("OSGiLog_NullCtxErr")); //$NON-NLS-1$
    }

    logTracker = new ServiceTracker(context, logServiceClass, null);
    logTracker.open();
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.internal.utils.log.Log#log(int, java.lang.String, java.lang.Throwable)
   */
  public void log(int severity, String msg, Throwable t) {
    final LogService logService = (LogService) logTracker.getService();
    if (logService != null) {
      try {
        logService.log(getLogServiceSeverity(severity), msg, t);
      } catch (Exception e) {
        /* log service instance is invalid */
        ConsoleLog.getDefault().log(severity, msg, t);
      }
    } else {
      ConsoleLog.getDefault().log(severity, msg, t);
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.internal.utils.log.Log#close()
   */
  public void close() {
    logTracker.close();
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
