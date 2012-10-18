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
package org.tigris.mtoolkit.iagent.internal.pmp;

import java.util.Dictionary;

import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.pmp.PMPConnection;
import org.tigris.mtoolkit.iagent.pmp.PMPException;
import org.tigris.mtoolkit.iagent.pmp.PMPService;
import org.tigris.mtoolkit.iagent.transport.Transport;

public class PMPServiceImpl extends PMPPeerImpl implements PMPService {
  private static final int     DEFAULT_PMP_PORT = 1450;

  protected boolean            running          = false;

  protected static ClassLoader loader;

  public PMPServiceImpl() {
    numSessions = 0;
    loader = getClass().getClassLoader();
    running = true;
  }

  public void destroy() {
    synchronized (this) {
      if (!running) {
        return;
      }
      running = false;
    }
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "Disconnecting Clients ...");
    }
    closeConnections("PMP Service has been stopped.");
    if (connDispatcher != null) {
      connDispatcher.stopEvent();
    }
    super.close();
  }

  public String getName() {
    return "PMPService"; //$NON-NLS-1$
  }

  public PMPConnection connect(Transport transport, Dictionary properties) throws PMPException {
    if (!running) {
      throw new PMPException("Stopping pmpservice");
    }
    try {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "Creating new connection for " + transport);
      }
      Object pmpPort = properties.get(PROP_PMP_PORT);
      int port = -1;
      if (pmpPort != null && pmpPort instanceof Integer) {
        port = ((Integer) pmpPort).intValue();
      }
      if (port < 0) {
        port = DEFAULT_PMP_PORT;
      }

      PMPSessionThread st = null;
      st = new PMPSessionThread(this, transport.createConnection(port), createSessionId());

      Connection con = st.getConnection();
      con.connect();
      addElement(st);
      return con;
    } catch (Exception exc) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "Error creating connection for " + transport, exc);
      }
      if (exc instanceof PMPException) {
        throw (PMPException) exc;
      }
      throw new PMPException(exc.getMessage(), exc);
    }
  }

  protected void removeElement(PMPSessionThread ss) {
    super.removeElement(ss);
    if ((connDispatcher != null) && ss.connected) {
      connDispatcher.addEvent(false, ss);
    }
  }

  protected static boolean checkInstance(Class[] interfaces, Class serviceClass) {
    if (interfaces == null) {
      return false;
    }
    boolean toRet = false;
    for (int i = 0; i < interfaces.length; i++) {
      if (interfaces[i] == null) {
        continue;
      }
      toRet = true;
      if (!interfaces[i].isAssignableFrom(serviceClass)) {
        return false;
      }
    }
    return toRet;
  }

  public String getRole() {
    return "Client";
  }

}
