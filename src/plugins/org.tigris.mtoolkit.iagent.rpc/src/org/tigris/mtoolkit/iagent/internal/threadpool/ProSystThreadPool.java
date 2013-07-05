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

import org.osgi.util.tracker.ServiceTracker;
import org.tigris.mtoolkit.iagent.internal.rpc.Activator;

import com.prosyst.util.threadpool.ThreadPoolManager;

final class ProSystThreadPool extends ThreadPool {
  private ServiceTracker tracker;

  public ProSystThreadPool() {
    tracker = new ServiceTracker(Activator.getBundleContext(), ThreadPoolManager.class.getName(), null);
    tracker.open(true);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.internal.threadpool.ThreadPoolProxy#enqueueWork(java.lang.Runnable)
   */
  public void enqueueWork(Runnable runnable) {
    ThreadPoolManager manager = (ThreadPoolManager) tracker.getService();
    if (manager != null) {
      manager.execute(runnable, WORKER_NAME);
    } else {
      super.enqueueWork(runnable);
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.internal.threadpool.ThreadPoolProxy#release()
   */
  public void release() {
    tracker.close();
  }
}
