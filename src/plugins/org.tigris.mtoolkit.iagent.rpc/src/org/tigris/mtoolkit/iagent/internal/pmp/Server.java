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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.osgi.framework.AllServiceListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.internal.utils.log.Log;
import org.tigris.mtoolkit.iagent.pmp.PMPServer;
import org.tigris.mtoolkit.iagent.pmp.PMPServerFactory;
import org.tigris.mtoolkit.iagent.rpc.Remote;

public class Server extends PMPPeerImpl implements Runnable, PMPServer, AllServiceListener {

	/** constant used for the pmp configuration */

	public static final String URI = "uri";

	public static final String PORT = "port";

	private static final int DEFAULT_PORT = 1450;
	
	private static final int FAILURE_RANDOM = 0;
	private static final int FAILURE_RETRY = 1;
	private static final int FAILURE_FAIL = 2;
	private static final int FAILURE_REUSE = 3;

	protected int maxStringLength;
	protected int maxArrayLength;
	private ServerSocket socket;
	protected volatile boolean run; // for what's this !?

	protected String uri;

	protected int port = DEFAULT_PORT;

	private BundleContext context;

	protected Hashtable eventTypes = new Hashtable(10);

	public Server(BundleContext context, Dictionary config) throws IOException {
		this.context = context;
		this.uri = ((String) config.get(URI));
		this.maxArrayLength = ((Integer) config.get(PMPServerFactory.MAX_ARRAY)).intValue();
		this.maxStringLength = ((Integer) config.get(PMPServerFactory.MAX_STRING)).intValue();
		Integer port = (Integer) config.get(PORT);
		if (port != null) {
			this.port = port.intValue();
		}
		init();
		context.addServiceListener(this);
	}

	protected void updateProps(Dictionary config) {
		int tempMaxA = ((Integer) config.get(PMPServerFactory.MAX_ARRAY)).intValue();
		int tempMaxS = ((Integer) config.get(PMPServerFactory.MAX_STRING)).intValue();
		if (maxArrayLength == tempMaxA && maxStringLength == tempMaxS)
			return;
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
		if ("random".equals(failureActionProp))
			return FAILURE_RANDOM;
		if ("retry".equals(failureActionProp))
			return FAILURE_RETRY;
		if ("fail".equals(failureActionProp))
			return FAILURE_FAIL;
		if ("reuse".equals(failureActionProp))
			return FAILURE_REUSE;
		return FAILURE_RANDOM;
	}
	
	protected void init() throws IOException {
		run = true;
		try {
			socket = new ServerSocket(port);
		} catch (IOException e) {
			DebugUtils.log(this, Log.ERROR, "Failed to open PMP server on " + port + ".", e);
			int failureAction = determineFailureAction();
			switch (failureAction) {
			case FAILURE_RANDOM:
				DebugUtils.log(this, Log.INFO, "Failure action set to 'random'. Retrying...");
				socket = new ServerSocket(0);
				port = socket.getLocalPort();
				DebugUtils.log(this, Log.INFO, "PMP server listening on " + port);
				break;
			case FAILURE_RETRY:
				Integer timeoutProp = Integer.getInteger("iagent.pmp.bindFailureAction.retryTimeout");
				int timeout = timeoutProp != null ? timeoutProp.intValue() : 10000;
				DebugUtils.log(this, Log.INFO, "Failure action set to 'retry'. Retrying with timeout " + timeout);
				try {
					Thread.sleep(timeout);
				} catch (InterruptedException e1) {
					throw e;
				}
				socket = new ServerSocket(port);
				break;
			case FAILURE_FAIL:
				throw e;
			case FAILURE_REUSE:
				DebugUtils.log(this, Log.INFO, "Failure action set to 'reuse'. Retrying...");
				ServerSocket serverSock = new ServerSocket();
				serverSock.setReuseAddress(true);
				serverSock.bind(new InetSocketAddress(port));
				socket = serverSock;
			}
		}
		socket.setSoTimeout(1000);
		new Thread(this, "IAgent Server Thread").start();
	}

	public void run() {
		while (run) {
			Socket client;
			try {
				client = socket.accept();
			} catch (IOException exc) {
				continue;
			}
			try {
				if (run) {
					if (System.getProperty("pmp.server.timeout") != null) {
						Integer timeout = Integer.getInteger("pmp.server.timeout", 0);
						client.setSoTimeout(timeout.intValue());
					}
					PMPSessionThread newSession = new PMPSessionThread(this,
						client,
						createSessionId(),
						client.getInetAddress().toString());
					addElement(newSession);
				}
			} catch (Exception exc) {
				error("Error Accepting Client", exc);
			}
		}
	}

	public void close() {
		synchronized (this) {
			if (!run)
				return;
			run = false;
		}
		closeConnections("PMP Server has been stopped.");
		try {
			info("Closing PMP Socket for " + uri);
		} catch (Throwable exc) {
		}
		if (socket != null) {
			try {
				socket.close();
				socket = null;
			} catch (Exception exc) {
				error("Error Closing PMP Socket", exc);
				socket = null;
			}
		}
		super.close();
	}

	public boolean isActive() {
		return run;
	}

	public void event(Object ev, String t) {
		Vector ls = (Vector) eventTypes.get(t);
		if (ls != null) {
			for (int i = 0; i < ls.size(); i++) {
				((PMPSessionThread) ls.elementAt(i)).event(ev, t);
			}
		}
	}

	protected synchronized byte addListener(String evType, PMPSessionThread listener) {
		Vector ls = (Vector) eventTypes.get(evType);
		if (ls == null) {
			ls = new Vector();
			eventTypes.put(evType, ls);
		}
		if (ls.contains(listener))
			return 1;
		ls.addElement(listener);
		return 2;
	}

	protected synchronized byte removeListener(String evType, PMPSessionThread listener) {
		Vector ls = (Vector) eventTypes.get(evType);
		return ls == null ? 1 : ls.removeElement(listener) ? 2 : (byte) 1;
	}

	protected synchronized void removeListeners(Vector evTypes, PMPSessionThread listener) {
		for (int i = 0; i < evTypes.size(); i++) {
			removeListener((String) evTypes.elementAt(i), listener);
		}
	}

	protected void debug(String msg) {
		DebugUtils.debug(this, msg);
	}

	protected void error(String msg, Throwable exc) {
		DebugUtils.error(this, msg, exc);
	}

	protected void info(String msg) {
		DebugUtils.info(this, msg);
	}

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
		if (refs == null)
			return null;
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
			debug(service + " is not instance of " + Remote.class.getName() + " or one of its remote interfaces");
			service = null;
			interfaces = null;
		}
		if (service != null)
			return new ObjectInfo(service, interfaces, sRef);
		else
			return null;
	}

	protected void ungetService(ObjectInfo info) {
		context.ungetService((ServiceReference) info.context);
	}

	protected void cleanRemoteObjects(ServiceReference sRef) {
		synchronized (connections) {
			PMPSessionThread session;
			for (Iterator it = connections.iterator(); it.hasNext();) {
				session = (PMPSessionThread) it.next();
				if (session.connected)
					session.unregisterService(sRef);
			}
		}
	}

	public void serviceChanged(ServiceEvent event) {
		if (event.getType() == ServiceEvent.UNREGISTERING)
			cleanRemoteObjects(event.getServiceReference());
	}

	public String getRole() {
		return "Server";
	}

	public Map getProperties() {
		Hashtable properties = new Hashtable();
		properties.put(PORT, new Integer(port));
		return properties;
	}
}
