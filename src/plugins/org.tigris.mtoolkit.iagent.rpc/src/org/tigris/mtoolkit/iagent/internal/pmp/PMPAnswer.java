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

class PMPAnswer {

	public boolean connected = false; // ok - connect
	protected int objID = -1; // ok - getReference, invoke
	protected long bid = -1; // ok - getReference, invoke
	protected int methodID = -1; // ok - getMethod
	protected String returnType = null; // ok - getMethod
	protected RemoteMethodImpl[] methods = null;
	protected Object obj = null;
	protected ClassLoader loader = null;
	protected String errMsg = null;

	public boolean success = false;
	protected Connection connection;
	protected RemoteObjectImpl requestingRObj; // ok
	protected boolean expectsReturn = false; // ok

	protected boolean received = false;
	protected boolean waiting = false;

	private PMPSessionThread c;

	protected PMPAnswer(PMPSessionThread c) {
		this.c = c;
	}

	public void free() {
		connected = false;
		objID = -1;
		bid = -1;
		methodID = -1;
		returnType = null;
		methods = null;
		obj = null;
		errMsg = null;

		success = false;
		expectsReturn = false;

		received = true;
		waiting = false;
	}

	public synchronized void finish() {
		received = true;
		if (waiting) {
			notify();
		}
	}

	public void get(int timeout) throws IOException {
		long time = System.currentTimeMillis();
		synchronized (this) {
			while (!received) {
				waiting = true;
				try {
					if (timeout > 0)
						wait(timeout);
					else {
						wait();
						break;
					}
				} catch (Exception ignore) {
				}
				if (!received && (System.currentTimeMillis() - time) > timeout)
					break;
			}
		}
		if (!received) {
			c.disconnect("Connection Lost", true);
			throw new IOException("Connection Lost");
		}
	}

	public String toString() {
		return "PMPAnswer --->>> " + c + " : " + c.hashCode(); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
