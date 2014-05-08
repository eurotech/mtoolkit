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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.osgi.framework.AllServiceListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.tigris.mtoolkit.iagent.pmp.PMPServer;
import org.tigris.mtoolkit.iagent.pmp.PMPServerFactory;
import org.tigris.mtoolkit.iagent.rpc.Remote;
import org.tigris.mtoolkit.iagent.util.DebugUtils;
import org.tigris.mtoolkit.iagent.util.ThreadUtils;

public final class Server extends PMPPeerImpl implements Runnable, PMPServer, AllServiceListener {
  public static final String URI            = "uri";

  private static final int   FAILURE_RANDOM = 0;
  private static final int   FAILURE_RETRY  = 1;
  private static final int   FAILURE_FAIL   = 2;

  private ServerSocket       socket;
  private volatile boolean   running;

  private String             uri;
  private int                port           = DEFAULT_PMP_PORT;

  private BundleContext      context;
  protected Hashtable        eventTypes     = new Hashtable(10);

  public Server(BundleContext context, Dictionary config) throws IOException {
    this.context = context;
    this.uri = ((String) config.get(URI));
    this.maxArrayLength = ((Integer) config.get(PMPServerFactory.MAX_ARRAY_PROP)).intValue();
    this.maxStringLength = ((Integer) config.get(PMPServerFactory.MAX_STRING_PROP)).intValue();
    Integer port = (Integer) config.get(PORT);
    if (port != null) {
      this.port = port.intValue();
    }
    init();
    context.addServiceListener(this);
  }

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  public void run() {
    while (running) {
      Socket client;
      try {
        client = socket.accept();
      } catch (IOException exc) {
        continue;
      }
      try {
        if (running) {
          if (System.getProperty("pmp.server.timeout") != null) {
            Integer timeout = Integer.getInteger("pmp.server.timeout", 0);
            client.setSoTimeout(timeout.intValue());
          }
          PMPSessionThread newSession = new PMPSessionThread(this, client, createSessionId(), client.getInetAddress()
              .toString());
          addElement(newSession);
        }
      } catch (Exception exc) {
        DebugUtils.error(this, "Error Accepting Client", exc);
      }
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.internal.pmp.PMPPeerImpl#close()
   */
  public void close() {
    if (!running) {
      return;
    }
    running = false;
    closeConnections("PMP Server has been stopped.");
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "Closing PMP Socket for " + uri);
    }
    if (socket != null) {
      try {
        socket.close();
        socket = null;
      } catch (Exception exc) {
        DebugUtils.error(this, "Error Closing PMP Socket", exc);
        socket = null;
      }
    }
    super.close();
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.pmp.PMPServer#event(java.lang.Object, java.lang.String)
   */
  public void event(Object ev, String t) {
    synchronized (eventTypes) {
      Vector ls = (Vector) eventTypes.get(t);
      if (ls != null) {
        for (int i = 0; i < ls.size(); i++) {
          ((PMPSessionThread) ls.elementAt(i)).event(ev, t);
        }
      }
    }
  }

  /* (non-Javadoc)
   * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
   */
  public void serviceChanged(ServiceEvent event) {
    if (event.getType() == ServiceEvent.UNREGISTERING) {
      cleanRemoteObjects(event.getServiceReference());
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.internal.pmp.PMPPeerImpl#getRole()
   */
  public String getRole() {
    return "Server";
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.pmp.PMPServer#getProperties()
   */
  public Dictionary getProperties() {
    Hashtable properties = new Hashtable(1);
    properties.put(PORT, new Integer(port));
    return properties;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.internal.pmp.PMPPeerImpl#addListener(java.lang.String, org.tigris.mtoolkit.iagent.internal.pmp.PMPSessionThread)
   */
  protected byte addListener(String evType, PMPSessionThread listener) {
    synchronized (eventTypes) {
      Vector ls = (Vector) eventTypes.get(evType);
      if (ls == null) {
        ls = new Vector();
        eventTypes.put(evType, ls);
      }
      if (ls.contains(listener)) {
        return 1;
      }
      ls.addElement(listener);
      return 2;
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.internal.pmp.PMPPeerImpl#removeListener(java.lang.String, org.tigris.mtoolkit.iagent.internal.pmp.PMPSessionThread)
   */
  protected byte removeListener(String evType, PMPSessionThread listener) {
    synchronized (eventTypes) {
      Vector ls = (Vector) eventTypes.get(evType);
      return ls == null ? 1 : ls.removeElement(listener) ? 2 : (byte) 1;
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.internal.pmp.PMPPeerImpl#removeListeners(java.util.Vector, org.tigris.mtoolkit.iagent.internal.pmp.PMPSessionThread)
   */
  protected synchronized void removeListeners(Vector evTypes, PMPSessionThread listener) {
    for (int i = 0; i < evTypes.size(); i++) {
      removeListener((String) evTypes.elementAt(i), listener);
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.internal.pmp.PMPPeerImpl#getService(java.lang.String, java.lang.String)
   */
  protected ObjectInfo getService(String clazz, String filter) {
    Object service = null;
    ServiceReference sRef = null;
    ServiceReference[] refs = null;
    Class[] interfaces = null;
    try {
      refs = context.getAllServiceReferences(clazz, filter);
    } catch (Exception exc) {
      return null;
    }
    if (refs == null) {
      return null;
    }
    for (int i = 0; i < refs.length; i++) {
      service = context.getService(refs[i]);
      if (service instanceof Remote) {
        interfaces = ((Remote) service).remoteInterfaces();
        if (PMPServiceImpl.checkInstance(interfaces, service.getClass())) {
          sRef = refs[i];
          break;
        }
      }
      context.ungetService(refs[i]);
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, service + " is not instance of " + Remote.class.getName()
            + " or one of its remote interfaces");
      }
      service = null;
      interfaces = null;
    }
    if (service != null) {
      return new ObjectInfo(service, interfaces, sRef);
    } else {
      return null;
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.internal.pmp.PMPPeerImpl#ungetService(org.tigris.mtoolkit.iagent.internal.pmp.ObjectInfo)
   */
  protected void ungetService(ObjectInfo info) {
    context.ungetService((ServiceReference) info.context);
  }

  protected void cleanRemoteObjects(ServiceReference sRef) {
    synchronized (connections) {
      for (Iterator it = connections.iterator(); it.hasNext();) {
        PMPSessionThread session = (PMPSessionThread) it.next();
        if (session.connected) {
          session.unregisterService(sRef);
        }
      }
    }
  }

  protected void updateProps(Dictionary config) {
    int tempMaxA = ((Integer) config.get(PMPServerFactory.MAX_ARRAY_PROP)).intValue();
    int tempMaxS = ((Integer) config.get(PMPServerFactory.MAX_STRING_PROP)).intValue();
    if (maxArrayLength == tempMaxA && maxStringLength == tempMaxS) {
      return;
    }
    synchronized (connections) {
      for (Iterator it = connections.iterator(); it.hasNext();) {
        PMPSessionThread session = (PMPSessionThread) it.next();
        session.maxA = maxArrayLength;
        session.maxS = maxStringLength;
      }
    }
  }

  private int determineFailureAction() {
    String failureActionProp = System.getProperty("iagent.pmp.bindFailureAction");
    if ("random".equals(failureActionProp)) {
      return FAILURE_RANDOM;
    }
    if ("retry".equals(failureActionProp)) {
      return FAILURE_RETRY;
    }
    if ("fail".equals(failureActionProp)) {
      return FAILURE_FAIL;
    }
    return FAILURE_RANDOM;
  }

  private void init() throws IOException {
    running = true;
    try {
      socket = new ServerSocket(port);
    } catch (IOException e) {
      int failureAction = determineFailureAction();
      switch (failureAction) {
      case FAILURE_RANDOM:
        if (DebugUtils.DEBUG_ENABLED) {
          DebugUtils.debug(this, "Failure action set to 'random'. Retrying...");
        }
        socket = new ServerSocket(0);
        port = socket.getLocalPort();
        break;
      case FAILURE_RETRY:
        Integer timeoutProp = Integer.getInteger("iagent.pmp.bindFailureAction.retryTimeout");
        int timeout = timeoutProp != null ? timeoutProp.intValue() : 10000;
        if (DebugUtils.DEBUG_ENABLED) {
          DebugUtils.debug(this, "Failure action set to 'retry'. Retrying with timeout " + timeout);
        }
        try {
          Thread.sleep(timeout);
        } catch (InterruptedException e1) {
          throw e;
        }
        socket = new ServerSocket(port);
        break;
      case FAILURE_FAIL:
        throw e;
      }
    }
    DebugUtils.info(this, "PMP server listening on " + port);
    ThreadUtils.createThread(this, "IAgent Server Thread").start();
  }
}
