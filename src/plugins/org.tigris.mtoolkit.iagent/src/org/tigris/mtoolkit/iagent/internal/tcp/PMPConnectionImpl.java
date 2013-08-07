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
package org.tigris.mtoolkit.iagent.internal.tcp;

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;

import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.pmp.EventListener;
import org.tigris.mtoolkit.iagent.pmp.PMPException;
import org.tigris.mtoolkit.iagent.pmp.PMPService;
import org.tigris.mtoolkit.iagent.pmp.PMPServiceFactory;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;
import org.tigris.mtoolkit.iagent.spi.ConnectionManager;
import org.tigris.mtoolkit.iagent.spi.MethodSignature;
import org.tigris.mtoolkit.iagent.spi.PMPConnection;
import org.tigris.mtoolkit.iagent.transport.Transport;

public final class PMPConnectionImpl implements PMPConnection, EventListener {
  private static MethodSignature                       RELEASE_METHOD               = new MethodSignature(
                                                                                        "releaseConsole",
                                                                                        MethodSignature.NO_ARGS, true);
  private static MethodSignature                       GET_REMOTE_SERVICE_ID_METHOD = new MethodSignature(
                                                                                        "getRemoteServiceID",
                                                                                        MethodSignature.NO_ARGS, true);

  private org.tigris.mtoolkit.iagent.pmp.PMPConnection pmpConnection;
  private ConnectionManagerImpl                        connManager;

  private HashMap                                      remoteObjects                = new HashMap(5);
  private RemoteObject                                 administration;
  private RemoteObject                                 remoteParserService;
  private volatile boolean                             closed                       = false;

  public PMPConnectionImpl(Transport transport, Dictionary conProperties, ConnectionManagerImpl connManager) throws IAgentException {
    DebugUtils.debug(this,
        "[Constructor] >>> Create PMP Connection: props: " + DebugUtils.convertForDebug(conProperties) + "; manager: "
            + connManager);

    PMPService pmpService = PMPServiceFactory.getDefault();
    try {
      Integer port = (Integer) conProperties.get(DeviceConnector.PROP_PMP_PORT);
      if (port == null) {
        port = getPmpPort(connManager);
      }
      if (port != null && port.intValue() == 0) {
        throw new IAgentException("Cannot determine PMP port", IAgentErrors.ERROR_CANNOT_CONNECT);
      }
      if (port != null) {
        conProperties.put(PMPService.PROP_PMP_PORT, port);
      }
      DebugUtils.debug(this, "[Constructor] Transport: " + transport);
      pmpConnection = pmpService.connect(transport, conProperties);
    } catch (PMPException e) {
      DebugUtils.error(this, "[Constructor] Failed to create PMP connection 1", e);
      throw new IAgentException("Unable to connect to the framework", IAgentErrors.ERROR_CANNOT_CONNECT, e);
    }
    this.connManager = connManager;
    pmpConnection.addEventListener(this, new String[] {
      org.tigris.mtoolkit.iagent.pmp.PMPConnection.FRAMEWORK_DISCONNECTED
    });
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.AbstractConnection#getType()
   */
  public int getType() {
    return ConnectionManager.PMP_CONNECTION;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.AbstractConnection#closeConnection()
   */
  public void closeConnection() throws IAgentException {
    closeConnection(true);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.AbstractConnection#closeConnection(boolean)
   */
  public void closeConnection(boolean aSendEvent) throws IAgentException {
    DebugUtils.debug(this, "[closeConnection] >>>");
    synchronized (this) {
      if (closed) {
        DebugUtils.debug(this, "[closeConnection] Already closed");
        return;
      }
      closed = true;
    }

    try {
      resetRemoteReferences();

      DebugUtils.debug(this, "[closeConnection] remove event listener");
      pmpConnection.removeEventListener(this, new String[] {
        org.tigris.mtoolkit.iagent.pmp.PMPConnection.FRAMEWORK_DISCONNECTED
      });
      pmpConnection.disconnect("Integration Agent request");
    } finally {
      if (connManager != null) {
        try {
          connManager.connectionClosed(this, aSendEvent);
        } catch (Throwable e) {
          DebugUtils.error(this, "[closeConnection] Internal error in connection manager", e);
        }
      }
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.AbstractConnection#isConnected()
   */
  public boolean isConnected() {
    return !closed && pmpConnection.isConnected();
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.PMPConnection#getRemoteBundleAdmin()
   */
  public RemoteObject getRemoteBundleAdmin() throws IAgentException {
    return getRemoteAdmin(REMOTE_BUNDLE_ADMIN_NAME);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.PMPConnection#getRemoteApplicationAdmin()
   */
  public RemoteObject getRemoteApplicationAdmin() throws IAgentException {
    return getRemoteAdmin(REMOTE_APPLICATION_ADMIN_NAME);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.PMPConnection#getRemoteDeploymentAdmin()
   */
  public RemoteObject getRemoteDeploymentAdmin() throws IAgentException {
    return getRemoteAdmin(REMOTE_DEPLOYMENT_ADMIN_NAME);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.PMPConnection#getRemoteParserService()
   */
  public RemoteObject getRemoteParserService() throws IAgentException {
    DebugUtils.debug(this, "[getRemoteParserService] >>>");
    if (!isConnected()) {
      DebugUtils.info(this, "[getRemoteParserService] The connecton has been closed!");
      throw new IAgentException("The connecton has been closed!", IAgentErrors.ERROR_DISCONNECTED);
    }
    if (remoteParserService == null) {
      DebugUtils.debug(this, "[getRemoteParserService] No RemoteParserService. Creating");
      try {
        remoteParserService = pmpConnection.getReference(REMOTE_CONSOLE_NAME, null);
      } catch (PMPException e) {
        DebugUtils.info(this, "[getRemoteParserService] RemoteParserGenerator service isn't available", e);
        throw new IAgentException("Unable to retrieve reference to remote administration service "
            + REMOTE_CONSOLE_NAME, IAgentErrors.ERROR_INTERNAL_ERROR);
      }
    }
    return remoteParserService;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.PMPConnection#releaseRemoteParserService()
   */
  public void releaseRemoteParserService() throws IAgentException {
    DebugUtils.debug(this, "[releaseRemoteParserService] >>>");
    if (remoteParserService != null) {
      try {
        RELEASE_METHOD.call(remoteParserService);
        remoteParserService.dispose();
      } catch (PMPException e) {
        DebugUtils.error(this, "[releaseRemoteParserService]", e);
      }
      remoteParserService = null;
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.PMPConnection#addEventListener(org.tigris.mtoolkit.iagent.pmp.EventListener, java.lang.String[])
   */
  public void addEventListener(EventListener listener, String[] eventTypes) throws IAgentException {
    DebugUtils.debug(this,
        "[addEventListener] >>> listener: " + listener + "; eventTypes: " + DebugUtils.convertForDebug(eventTypes));
    if (!isConnected()) {
      DebugUtils.info(this, "[addEventListener] The connecton has been closed!");
      throw new IAgentException("The connecton has been closed!", IAgentErrors.ERROR_DISCONNECTED);
    }

    pmpConnection.addEventListener(listener, eventTypes);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.PMPConnection#removeEventListener(org.tigris.mtoolkit.iagent.pmp.EventListener, java.lang.String[])
   */
  public void removeEventListener(EventListener listener, String[] eventTypes) throws IAgentException {
    DebugUtils.debug(this,
        "[removeEventListener] listener: " + listener + "; eventTypes: " + DebugUtils.convertForDebug(eventTypes));
    if (!isConnected()) {
      DebugUtils.info(this, "[removeEventListener] The connecton has been closed!");
      throw new IAgentException("The connecton has been closed!", IAgentErrors.ERROR_DISCONNECTED);
    }
    pmpConnection.removeEventListener(listener, eventTypes);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.PMPConnection#getRemoteServiceAdmin()
   */
  public RemoteObject getRemoteServiceAdmin() throws IAgentException {
    return getRemoteAdmin(REMOTE_SERVICE_ADMIN_NAME);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.PMPConnection#getRemoteAdmin(java.lang.String)
   */
  public RemoteObject getRemoteAdmin(String adminClassName) throws IAgentException {
    DebugUtils.debug(this, "[getRemoteAdmin]" + adminClassName + " >>>");
    if (!isConnected()) {
      DebugUtils.info(this, "[getRemoteBundleAdmin] The connecton has been closed!");
      throw new IAgentException("The connecton has been closed!", IAgentErrors.ERROR_DISCONNECTED);
    }
    RemoteObject admin = (RemoteObject) remoteObjects.get(adminClassName);
    if (admin == null) {
      try {
        DebugUtils.debug(this, "[getRemoteAdmin] No remote admin [" + adminClassName + "]. Creating...");
        final String adminClass = adminClassName;
        admin = new PMPRemoteObjectAdapter(pmpConnection.getReference(adminClassName, null)) {
          public int verifyRemoteReference() throws IAgentException {
            if (!pmpConnection.isConnected()) {
              DebugUtils.info(this, "[verifyRemoteReference] The connection has been closed!");
              throw new IAgentException("The connecton has been closed!", IAgentErrors.ERROR_DISCONNECTED);
            }
            try {
              RemoteObject newRemoteObject = pmpConnection.getReference(adminClass, null);
              Long l = new Long(-1);
              if (GET_REMOTE_SERVICE_ID_METHOD.isDefined(newRemoteObject)) {
                l = (Long) GET_REMOTE_SERVICE_ID_METHOD.call(newRemoteObject);
              }
              long newServiceID = l.longValue();
              if (newServiceID == -1) {
                debug("[verifyRemoteReference] New reference service id is = -1. Nothing to do. Continuing.");
                return PMPRemoteObjectAdapter.CONTINUE;
              }
              debug("[verifyRemoteReference] initial: " + this.getInitialServiceID() + "; new: " + l);
              if (newServiceID != this.getInitialServiceID()) {
                this.delegate = newRemoteObject;
                this.setInitialServiceID(newServiceID);
                debug("[verifyRemoteReference] Reference to remote service was refreshed. Retry remote method call...");
                return PMPRemoteObjectAdapter.REPEAT;
              }
              newRemoteObject.dispose();
              debug("[verifyRemoteReference] Reference to remote service is looking fine. Continue");
              return PMPRemoteObjectAdapter.CONTINUE;
            } catch (PMPException e) {
              // admin = null;
              DebugUtils
                  .info(
                      this,
                      "[verifyRemoteReference] Reference to remote service cannot be got, service is not available. Fail fast.",
                      e);
              throw new IAgentException("Unable to retrieve reference to remote administration service " + adminClass,
                  IAgentErrors.ERROR_REMOTE_ADMIN_NOT_AVAILABLE, e);
            }
          }
        };
        remoteObjects.put(adminClassName, admin);
      } catch (PMPException e) {
        DebugUtils.info(this, "[getRemoteAdmin] Remote admin [" + adminClassName + "] isn't available", e);
        throw new IAgentException("Unable to retrieve reference to remote administration service [" + adminClassName
            + "]", IAgentErrors.ERROR_REMOTE_ADMIN_NOT_AVAILABLE, e);
      }
    }
    return admin;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.pmp.EventListener#event(java.lang.Object, java.lang.String)
   */
  public void event(Object ev, String evType) {
    DebugUtils.debug(this, "[event] >>> Object event: " + ev + "; eventType: " + evType);
    if (org.tigris.mtoolkit.iagent.pmp.PMPConnection.FRAMEWORK_DISCONNECTED.equals(evType)) {
      try {
        DebugUtils.debug(this, "[event] Framework disconnection event received");
        closeConnection();
      } catch (Throwable e) {
        DebugUtils.error(this, "[event] Exception while cleaning up the connection", e);
      }
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.AbstractConnection#getProperty(java.lang.String)
   */
  public Object getProperty(String propertyName) {
    return null;
  }

  private Integer getPmpPort(ConnectionManager manager) throws IAgentException {
    return (Integer) manager.queryProperty(ConnectionManager.PROP_PMP_PORT);
  }

  private void resetRemoteReferences() {
    DebugUtils.debug(this, "[resetRemoteReferences] >>>");
    if (remoteObjects != null) {
      Collection objects = remoteObjects.values();
      for (Iterator iterator = objects.iterator(); iterator.hasNext();) {
        RemoteObject remoteObject = (RemoteObject) iterator.next();
        try {
          remoteObject.dispose();
        } catch (PMPException e) {
          DebugUtils.error(this, "[resetRemoteReferences] Failure during PMP connection cleanup", e);
        }
      }
      remoteObjects.clear();
    }

    if (remoteParserService != null) {
      try {
        remoteParserService.dispose();
      } catch (PMPException e) {
        DebugUtils.error(this, "[resetRemoteReferences] Failure during PMP connection cleanup", e);
      }
      remoteParserService = null;
    }

    if (administration != null) {
      try {
        administration.dispose();
      } catch (PMPException e) {
        DebugUtils.error(this, "[resetRemoteReferences] Failure during PMP connection cleanup", e);
      }
      administration = null;
    }
  }
}
