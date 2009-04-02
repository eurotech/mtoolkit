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

import java.util.Hashtable;
import java.util.Vector;

import org.tigris.mtoolkit.iagent.pmp.EventListener;
import org.tigris.mtoolkit.iagent.pmp.PMPConnection;
import org.tigris.mtoolkit.iagent.pmp.PMPException;

/**
 * This implementation uses the PMP Service to receive remote events.
 */
class PMPEventsManager extends Thread {

	private PMPOutputStream os;
	private PMPEvent first;
	private PMPEvent last = null;
	private boolean waiting = false;

	PMPSessionThread session;

	public PMPEventsManager(PMPSessionThread session) {
		super("PMP Events Manager Thread [" + session.sessionID + "]"); //$NON-NLS-1$
		this.session = session;
		this.os = session.os;
	}

	private boolean go = true;

	public void run() {
		while (go || first != null) {
			if (first == null) {
				synchronized (this) {
					if (first == null) {
						waiting = true;
						try {
							wait();
							if (!go && first == null) {
								session.debug("Event Manager: Thread ended.");
								return;
							}
						} catch (Exception exc) {
							continue;
						} finally {
							waiting = false;
						}
					}
				}
			}
			PMPEvent theEvent;
			synchronized (this) {
				if (first.next != null) {
					theEvent = getEvent(false);
				} else {
					theEvent = getEvent(true);
				}
			}
			deliverEvent(theEvent);
		}
		session.debug("Event Manager: Thread ended (2).");
	}

	public void stopEvents() {
		go = false;
		synchronized (this) {
			notify();
		}
	}

	private PMPEvent getEvent(boolean one) {
		PMPEvent tmp = first;
		first = first.next;
		if (one)
			last = null;
		return tmp;
	}

	private synchronized void addEvent(PMPEvent theEvent) {
		if (!go)
			return;
		if (last == null) {
			first = last = theEvent;
			if (waiting) {
				notify();
			}
		} else {
			last = last.next = theEvent;
		}
	}

	private void deliverEvent(PMPEvent event) {
		session.debug("Delivering event : " + event);
		if (event instanceof ListenerEvent) {
			if (!go)
				// don't deliver listener events to the server, the connection
				// has been closed
				return;
			ListenerEvent levent = (ListenerEvent) event;
			switch (levent.op) {
			case ListenerEvent.ADD_LISTENER_OP:
				addEventListener0(((PMPEvent) event).eventType);
				break;
			case ListenerEvent.REMOVE_LISTENER_OP:
				removeEventListener0((EventListener) ((PMPEvent) event).data, ((PMPEvent) event).eventType);
				break;
			}
		} else {
			Vector cloned = null;
			PMPEvent pmpEvent = (PMPEvent) event;
			synchronized (listeners) {
				Vector ls = (Vector) listeners.get(pmpEvent.eventType);
				if (ls == null || ls.size() == 0)
					return;
				cloned = (Vector) ls.clone();
			}
			for (int i = 0; i < cloned.size(); i++) {
				try {
					((EventListener) cloned.elementAt(i)).event(pmpEvent.data, pmpEvent.eventType);
				} catch (Exception exc) {
				}
			}
		}
	}

	private Hashtable listeners = new Hashtable(10);

	/**
	 * Registers an {@link EventListener
	 * org.tigris.mtoolkit.iagent.internal.event.EventListener}
	 * 
	 * @param el
	 *            the EventListener
	 * @exception Exception
	 */
	public void addEventListener(EventListener el, String type) {
		if (el == null)
			throw new NullPointerException("Can't add null listener");
		synchronized (listeners) {
			Vector ls = (Vector) listeners.get(type);
			if (ls == null) {
				ls = new Vector();
				ls.addElement(el);
				listeners.put(type, ls);
				if (!PMPConnection.FRAMEWORK_DISCONNECTED.equals(type))
					addEvent(new ListenerEvent(ListenerEvent.ADD_LISTENER_OP, type, el));
			} else if (!ls.contains(el))
				ls.addElement(el);
		}
	}

	private void addEventListener0(String type) {
		try {
			PMPAnswer answer = new PMPAnswer(session);
			os.begin(answer);
			os.write(PMPSessionThread.ADD_LS);
			PMPData.writeString(type, os);
			os.end(true);
			answer.get(session.is.timeout);
		} catch (Exception exc) { // PMPException, IOException
			session.error("error registering event listener", exc);
		}
		session.debug("Adding remote listener of type: " + type);
	}

	/**
	 * Unregisters a {@link EventListener
	 * org.tigris.mtoolkit.iagent.internal.event.EventListener}
	 * 
	 * @param el
	 *            the EventListener
	 */
	public void removeEventListener(EventListener el, String type) {
		if (el == null)
			throw new NullPointerException("Can't remove null listener");
		addEvent(new ListenerEvent(ListenerEvent.REMOVE_LISTENER_OP, type, el));
	}

	private void removeEventListener0(EventListener el, String evType) {
		// synchronize over listeners vector, otherwise the add/remove listener
		// operations can be executed in reverse order on the remote side,
		// resulting in no more events
		synchronized (listeners) {
			try {
				Vector ls = (Vector) listeners.get(evType);
				if (ls != null) {
					ls.removeElement(el);
					if (ls.size() == 0) {
						ls = null;
						listeners.remove(evType);
						if (PMPConnection.FRAMEWORK_DISCONNECTED.equals(evType))
							return;
						PMPAnswer answer = new PMPAnswer(session);
						os.begin(answer);
						os.write(PMPSessionThread.REMOVE_LS);
						PMPData.writeString(evType, os);
						os.end(true);
						answer.get(session.is.timeout);
						if (!answer.success) {
							throw new PMPException(answer.errMsg);
						}
					}
				}
			} catch (Exception exc) { // PMPException, IOException
				session.error("error unregitering event listener", exc);
			}
		}
	}

	/**
	 * Posts a custom event.
	 * 
	 * @param evType
	 *            the event's type
	 * @param event
	 *            the event
	 */
	public void postEvent(String evType, Object event) {
		PMPEvent e = new PMPEvent(evType, event);
		addEvent(e);
	}

	/**
	 * Registers listener in the Framework
	 * 
	 * @param eventType
	 *            The event type of the listeners ot register
	 */
	public void registerListeners(String eventType) {
		synchronized (listeners) {
			Vector ls = (Vector) listeners.get(eventType);
			if (ls != null) {
				addEvent(new ListenerEvent(ListenerEvent.ADD_LISTENER_OP, eventType, (EventListener) ls.elementAt(0)));
			}
		}
	}

	protected ClassLoader getClassLoader(String type) {
		synchronized (listeners) {
			Vector v = (Vector) listeners.get(type);
			return v == null ? null : v.elementAt(0).getClass().getClassLoader();
		}
	}
}
