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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.tigris.mtoolkit.iagent.internal.rpc.Messages;
import org.tigris.mtoolkit.iagent.internal.threadpool.ThreadPool;
import org.tigris.mtoolkit.iagent.pmp.PMPConnectionListener;
import org.tigris.mtoolkit.iagent.pmp.PMPPeer;

public class PMPPeerImpl implements PMPPeer {
  protected List                    connections     = new ArrayList();
  protected int                     numSessions;

  protected int                     maxStringLength = -1;
  protected int                     maxArrayLength  = -1;

  protected ThreadPool              pool            = ThreadPool.newInstance();
  protected Vector                  listeners       = new Vector(2, 5);
  protected PMPConnectionDispatcher connDispatcher;

  protected void addElement(PMPSessionThread ss) {
    synchronized (connections) {
      if (!connections.contains(ss)) {
        connections.add(ss);
      }
    }
  }

  protected void removeElement(PMPSessionThread ss) {
    synchronized (connections) {
      connections.remove(ss);
    }
  }

  protected synchronized String createSessionId() {
    return String.valueOf(numSessions++);
  }

  protected void closeConnections(String reason) {
    synchronized (connections) {
      PMPSessionThread[] copied = (PMPSessionThread[]) connections.toArray(new PMPSessionThread[connections.size()]);
      for (int i = 0; i < copied.length; i++) {
        PMPSessionThread session = copied[i];
        session.disconnect(reason, true);
      }
    }
  }

  public void close() {
    if (pool != null) {
      pool.release();
      pool = null;
    }
  }

  protected ObjectInfo getService(String clazz, String filter) {
    throw new UnsupportedOperationException();
  }

  protected void ungetService(ObjectInfo info) {
    throw new UnsupportedOperationException();
  }

  protected byte removeListener(String evType, PMPSessionThread listener) {
    throw new UnsupportedOperationException();
  }

  protected void removeListeners(Vector evTypes, PMPSessionThread listener) {
    throw new UnsupportedOperationException();
  }

  protected byte addListener(String evType, PMPSessionThread listener) {
    throw new UnsupportedOperationException();
  }

  public String getRole() {
    return Messages.getString("PMPPeerImpl.Name"); //$NON-NLS-1$
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.pmp.PMPPeer#addConnectionListener(org.tigris.mtoolkit.iagent.pmp.PMPConnectionListener)
   */
  public void addConnectionListener(PMPConnectionListener listener) {
    if (connDispatcher == null) {
      connDispatcher = new PMPConnectionDispatcher(this);
    }
    synchronized (listeners) {
      if (!listeners.contains(listener)) {
        listeners.addElement(listener);
      }
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.pmp.PMPPeer#removeConnectionListener(org.tigris.mtoolkit.iagent.pmp.PMPConnectionListener)
   */
  public void removeConnectionListener(PMPConnectionListener listener) {
    synchronized (listeners) {
      listeners.removeElement(listener);
    }
  }

  /**
   * Fire Event When Client connected.
   *
   * @param ss
   *          SessionThread for connection
   */
  protected void fireConnectionEvent(boolean created, PMPSessionThread ss) {
    if (connDispatcher != null) {
      connDispatcher.addEvent(created, ss);
    }
  }
}
