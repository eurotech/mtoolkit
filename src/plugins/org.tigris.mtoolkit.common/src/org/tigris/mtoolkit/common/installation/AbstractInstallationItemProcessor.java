/*******************************************************************************
 * Copyright (c) 2005, 2012 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.common.installation;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public abstract class AbstractInstallationItemProcessor implements InstallationItemProcessor {
  private final List listenersList = new ArrayList();

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.installation.InstallationItemProcessor#addListener(org.tigris.mtoolkit.common.installation.InstallListener)
   */
  public synchronized void addListener(InstallListener listener) {
    synchronized (listenersList) {
      if (!listenersList.contains(listener)) {
        listenersList.add(listener);
      }
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.installation.InstallationItemProcessor#removeListener(int, java.util.EventListener)
   */
  public synchronized void removeListener(InstallListener listener) {
    synchronized (listenersList) {
      listenersList.remove(listener);
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.installation.InstallationItemProcessor#getProperties()
   */
  public Map getProperties() {
    return null;
  }

  protected IStatus fireInstallEvent(boolean before) {
    EventListener[] listeners = null;
    synchronized (listenersList) {
      listeners = new EventListener[listenersList.size()];
      listenersList.toArray(listeners);
    }
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        IStatus status = null;
        if (before) {
          status = ((InstallListener) listeners[i]).beforeInstall();
        } else {
          status = ((InstallListener) listeners[i]).afterInstall();
        }
        if (status != null && status.matches(IStatus.CANCEL) || status.matches(IStatus.ERROR)) {
          return status;
        }
      }
    }
    return Status.OK_STATUS;
  }
}
