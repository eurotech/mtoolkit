/*******************************************************************************
 * Copyright (c) 2005, 2013 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.iagent.internal.threadpool;

import org.tigris.mtoolkit.iagent.internal.rpc.Messages;
import org.tigris.mtoolkit.iagent.internal.utils.ThreadUtils;

public class ThreadPool {
  protected static final String WORKER_NAME = Messages.getString("ThreadPool_WorkerName"); //$NON-NLS-1$

  public void enqueueWork(Runnable runnable) {
    Thread runner = ThreadUtils.createThread(runnable, WORKER_NAME);
    runner.start();
  }

  public void release() {
  }

  protected ThreadPool() {
  }

  public static ThreadPool newInstance() {
    String platformInfo = System.getProperty("org.osgi.framework.vendor"); //$NON-NLS-1$
    if ("Eclipse".equalsIgnoreCase(platformInfo)) { //$NON-NLS-1$
      return new EclipseThreadPool();
    } else if ("ProSyst".equalsIgnoreCase(platformInfo)) { //$NON-NLS-1$
      return new ProSystThreadPool();
    }
    return new ThreadPool();
  }
}
