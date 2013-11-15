/*******************************************************************************
 * Copyright (c) 2005, 2013 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 ****************************************************************************/
package org.tigris.mtoolkit.iagent.spi;

import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;

import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.pmp.EventListener;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;
import org.tigris.mtoolkit.iagent.util.DebugUtils;

public abstract class AbstractManager implements IAgentManager, ConnectionListener, EventListener {
  private final String         remoteAdminClass;

  protected final String       remoteEventType;

  protected DeviceConnectorSpi connectorSpi;

  protected final List         remoteEventListeners = new LinkedList();

  public AbstractManager(String remoteAdmin) {
    this(remoteAdmin, null);
  }

  public AbstractManager(String remoteAdminClass, String remoteEventType) {
    this.remoteAdminClass = remoteAdminClass;
    this.remoteEventType = remoteEventType;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.IAgentManager#init(org.tigris.mtoolkit.iagent.spi.DeviceConnectorSpi)
   */
  public void init(DeviceConnectorSpi connector) {
    if (connector == null) {
      throw new IllegalArgumentException();
    }
    this.connectorSpi = connector;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.IAgentManager#dispose()
   */
  public void dispose() {
    try {
      synchronized (remoteEventListeners) {
        remoteEventListeners.clear();
      }
      if (remoteEventType != null) {
        final PMPConnection connection = getPMPConnection();
        if (connection != null) {
          DebugUtils
              .debug(this,
                  "[removeListeners] PMP connection is available, remove event listener for synchronous application events...");
          connection.removeEventListener(this, new String[] {
            remoteEventType
          });
        }
        DebugUtils.debug(this, "[removeListeners] application listeners removed");
      }
    } catch (IAgentException e) {
      DebugUtils.error(this, "[dispose] Exception while disposing application manager", e);
    } finally {
      getConnectionManager().removeConnectionListener(this);
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.ConnectionListener#connectionChanged(org.tigris.mtoolkit.iagent.spi.ConnectionEvent)
   */
  public void connectionChanged(ConnectionEvent event) {
    if (remoteEventType == null) {
      return;
    }
    DebugUtils.debug(this, "[connectionChanged] >>> event: " + event);
    if (event.getType() == ConnectionEvent.CONNECTED) {
      if (event.getConnection().getType() == ConnectionManager.PMP_CONNECTION) {
        DebugUtils.debug(this, "[connectionChanged] New PMP connection created, restore event listeners");
        synchronized (remoteEventListeners) {
          PMPConnection connection = (PMPConnection) event.getConnection();
          if (!remoteEventListeners.isEmpty()) {
            DebugUtils.debug(this, "[connectionChanged] Restoring event listeners...");
            try {
              connection.addEventListener(this, new String[] {
                remoteEventType
              });
            } catch (IAgentException e) {
              DebugUtils.error(this, "[connectionChanged] Failed to add event listener to PMP connection", e);
            }
          }
        }
      }
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.pmp.EventListener#event(java.lang.Object, java.lang.String)
   */
  public void event(Object event, String eventType) {
    if (remoteEventType == null) {
      return;
    }
    DebugUtils.debug(this, "[event] >>> event: " + event + "; type: " + eventType);
    if (remoteEventType.equals(eventType)) {
      Dictionary eventProps = (Dictionary) event;
      if (eventProps == null) {
        return;
      }
      remoteEvent(eventProps);
    }
  }

  public RemoteObject getRemoteAdmin() throws IAgentException {
    DebugUtils.debug(this, "[getRemoteAdmin] >>>", null);
    final PMPConnection pmpConnection = getPMPConnection();
    if (pmpConnection == null || !pmpConnection.isConnected()) {
      DebugUtils.debug(this, "[getRemoteAdmin] The connecton has been closed!", null);
      throw new IAgentException("The connecton has been closed!", IAgentErrors.ERROR_DISCONNECTED);
    }
    RemoteObject remoteConfigAdmin = pmpConnection.getRemoteAdmin(remoteAdminClass);
    return remoteConfigAdmin;
  }

  protected void remoteEvent(Dictionary eventProps) {
  }

  protected void addRemoteEventListener(java.util.EventListener listener) throws IAgentException {
    getConnectionManager().addConnectionListener(this);
    synchronized (remoteEventListeners) {
      if (!remoteEventListeners.contains(listener)) {
        final PMPConnection connection = getPMPConnection();
        if (connection != null) {
          DebugUtils.debug(this, "[addRemoteEventListener] PMP connection is available, add event listener");
          connection.addEventListener(this, new String[] {
            remoteEventType
          });
        }
        remoteEventListeners.add(listener);
      } else {
        DebugUtils.debug(this, "[addRemoteEventListener] Listener already present");
      }
    }
  }

  protected void removeRemoteEventListener(java.util.EventListener listener) throws IAgentException {
    DebugUtils.debug(this, "[removeRemoteEventListener] >>> listener: " + listener);
    synchronized (remoteEventListeners) {
      if (remoteEventListeners.contains(listener)) {
        remoteEventListeners.remove(listener);
        if (remoteEventListeners.size() == 0) {
          DebugUtils.debug(this,
              "[removeRemoteEventListener] No more listeners in the list, try to remove PMP event listener");
          final PMPConnection connection = getPMPConnection();
          if (connection != null) {
            DebugUtils.debug(this, "[removeRemoteEventListener] PMP connection is available, remove event listener");
            connection.removeEventListener(this, new String[] {
              remoteEventType
            });
          }
        }
      } else {
        DebugUtils.debug(this, "[removeRemoteApplicationListener] Listener not found in the list");
      }
    }
  }

  protected java.util.EventListener[] getEventListeners() {
    synchronized (remoteEventListeners) {
      if (!remoteEventListeners.isEmpty()) {
        return (java.util.EventListener[]) remoteEventListeners
            .toArray(new java.util.EventListener[remoteEventListeners.size()]);
      }
    }
    return null;
  }

  protected PMPConnection getPMPConnection() throws IAgentException {
    return (PMPConnection) connectorSpi.getConnectionManager().getActiveConnection(ConnectionManager.PMP_CONNECTION);
  }

  protected ConnectionManager getConnectionManager() {
    return connectorSpi.getConnectionManager();
  }
}
