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

import java.util.Vector;

import org.tigris.mtoolkit.iagent.pmp.PMPConnection;
import org.tigris.mtoolkit.iagent.pmp.PMPConnectionListener;


class PMPConnectionDispatcher extends Thread {

	private boolean go;
	private ConnectionEvent first;
	private ConnectionEvent last;
	private boolean waiting;
	private PMPPeerImpl pmp;

	PMPConnectionDispatcher(PMPPeerImpl pmp) {
		super("PMPConnDispatcher Thread");
		this.pmp = pmp;
		waiting = false;
		go = true;
		start();
	}

	public void run() {
		while (go || first != null) {
			if (first == null) {
				synchronized (this) {
					if (first == null) {
						waiting = true;
						try {
							wait();
							if (!go && first == null)
								return;
						} catch (Exception exc) {
							continue;
						} finally {
							waiting = false;
						}
					}
				}
			}
			ConnectionEvent theEvent;
			synchronized (this) {
				if (first.next != null) {
					theEvent = getEvent(false);
				} else {
					theEvent = getEvent(true);
				}
			}
			deliverEvent(theEvent);
		}
	}

	protected void stopEvent() {
		go = false;
		synchronized (this) {
			notify();
		}
	}

	private ConnectionEvent getEvent(boolean one) {
		ConnectionEvent tmp = first;
		first = first.next;
		if (one)
			last = null;
		return tmp;
	}

	protected synchronized void addEvent(boolean t, PMPSessionThread th) {
		ConnectionEvent theEvent = new ConnectionEvent(t, th);
		if (last == null) {
			first = last = theEvent;
			if (waiting) {
				notify();
			}
		} else {
			last = last.next = theEvent;
		}
	}

	private void deliverEvent(ConnectionEvent event) {
		Vector v = new Vector();
		synchronized (pmp.listeners) {
			v = (Vector) pmp.listeners.clone();
		}
		PMPConnection tmpCon = event.st.getConnection();
		if (event.type) {
			for (int i = 0; i < v.size(); i++) {
				try {
					((PMPConnectionListener) v.elementAt(i)).clientConnected(tmpCon);
				} catch (Exception ign) {
				}
			}
		} else {
			for (int i = 0; i < v.size(); i++) {
				try {
					((PMPConnectionListener) v.elementAt(i)).clientDisconnected(tmpCon);
				} catch (Exception ign) {
				}
			}
		}
		event = null;
	}
}

class ConnectionEvent {

	boolean type;

	PMPSessionThread st;

	ConnectionEvent next;

	ConnectionEvent(boolean type, PMPSessionThread st) {
		this.type = type;
		this.st = st;
	}

}
