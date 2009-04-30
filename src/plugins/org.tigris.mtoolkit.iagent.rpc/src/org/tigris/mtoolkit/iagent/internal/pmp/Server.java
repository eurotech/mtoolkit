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

public class Server extends PMPPeerImpl implements Runnable, PMPServer, AllServiceListener {

	/** constant used for the pmp configuration */

	public static final String URI = "uri";

	public static final String PORT = "port";

	protected int maxStringLength;
	protected int maxArrayLength;
	private ServerSocket socket;
	protected volatile boolean run; // for what's this !?

	protected String uri;

	private BundleContext context;

	public Server(BundleContext context, Dictionary config) throws IOException {
		this.context = context;
		this.uri = ((String) config.get(URI));
		this.maxArrayLength = ((Integer) config.get(PMPServerFactory.MAX_ARRAY)).intValue();
		this.maxStringLength = ((Integer) config.get(PMPServerFactory.MAX_STRING)).intValue();
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

	protected void init() throws IOException {
		run = true;
		socket = new ServerSocket(1450);
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

	public synchronized void addEventSource(String eventType) {
		if (eventTypes.get(eventType) == null) {
			eventTypes.put(eventType, new Vector());
			synchronized (connections) {
				for (Iterator it = connections.iterator(); it.hasNext();) {
					PMPSessionThread session = (PMPSessionThread) it.next();
					if (session.connected)
						session.registerEventType(eventType);
				}
			}
		}
	}

	public void removeEventSource(String eventType) {
		eventTypes.remove(eventType);
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
		if (ls == null)
			return 0;
		if (ls.contains(listener))
			return 1;
		ls.addElement(listener);
		return 2;
	}

	protected synchronized byte removeListener(String evType, PMPSessionThread listener) {
		Vector ls = (Vector) eventTypes.get(evType);
		return ls == null ? 0 : ls.removeElement(listener) ? 2 : (byte) 1;
	}

	protected synchronized void removeListeners(Vector evTypes, PMPSessionThread listener) {
		for (int i = 0; i < evTypes.size(); i++) {
			removeListener((String) evTypes.elementAt(i), listener);
		}
	}

	protected Hashtable eventTypes = new Hashtable(10);

	protected void debug(String msg) {
		// TODO: Add logging
	}

	protected void error(String msg, Throwable exc) {
		// TODO: Add logging
	}

	protected void info(String msg) {
		// TODO: Add logging
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
			interfaces = ((Remote) service).remoteInterfaces();
			if (service instanceof Remote && PMPServiceImpl.checkInstance(interfaces, service.getClass())) {
				sRef = refs[i];
				break;
			} else {
				context.ungetService(refs[i]);
				debug(service + " is not instance of " + Remote.class.getName() + " or one of its remote interfaces");
				service = null;
				interfaces = null;
			}
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

}
