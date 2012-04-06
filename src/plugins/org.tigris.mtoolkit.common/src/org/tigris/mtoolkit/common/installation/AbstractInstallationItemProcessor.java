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
import java.util.Arrays;
import java.util.EventListener;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.tigris.mtoolkit.common.Messages;
import org.tigris.mtoolkit.common.UtilitiesPlugin;

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

  public static IStatus prepareItems(InstallationItem[] items, Map properties, IProgressMonitor monitor, boolean bulk) {
    if (items.length == 0) {
      return Status.OK_STATUS;
    }
    SubMonitor sub = SubMonitor.convert(monitor, Messages.AbstractInstallationItemProcessor_Preparing_Items, 100);
    if (bulk) {
      List itemsList = Arrays.asList(items);
      InstallationRegistry registry = InstallationRegistry.getInstance();
      List providers = registry.getProviders();
      if (!providers.isEmpty()) {
        int work = 100 / providers.size();
        for (int i = 0; i < providers.size(); i++) {
          InstallationItemProvider provider = (InstallationItemProvider) providers.get(i);
          IStatus status = provider.prepareItems(itemsList, properties, sub.newChild(work));
          if (status == null) {
            continue;
          }
          if (status.matches(IStatus.CANCEL) || status.matches(IStatus.ERROR)) {
            return status;
          } else if (!status.isOK()) {
            UtilitiesPlugin.log(status);
          }
        }
      }
    } else {
      int work = 100 / items.length;
      for (int i = 0; i < items.length; i++) {
        IStatus status = items[i].prepare(sub.newChild(work), properties);
        if (status == null) {
          continue;
        }
        if (status.matches(IStatus.CANCEL) || status.matches(IStatus.ERROR)) {
          return status;
        } else if (!status.isOK()) {
          UtilitiesPlugin.log(status);
        }
      }
    }
    sub.done();
    if (monitor.isCanceled()) {
      return Status.CANCEL_STATUS;
    }
    return Status.OK_STATUS;
  }

  protected IStatus fireInstallEvent(boolean before) {
    EventListener[] listeners;
    synchronized (listenersList) {
      listeners = new EventListener[listenersList.size()];
      listenersList.toArray(listeners);
    }
    for (int i = 0; i < listeners.length; i++) {
      IStatus status = null;
      if (before) {
        status = ((InstallListener) listeners[i]).beforeInstall();
      } else {
        status = ((InstallListener) listeners[i]).afterInstall();
      }
      if (status != null && (status.matches(IStatus.CANCEL) || status.matches(IStatus.ERROR))) {
        return status;
      }
    }
    return Status.OK_STATUS;
  }
}
