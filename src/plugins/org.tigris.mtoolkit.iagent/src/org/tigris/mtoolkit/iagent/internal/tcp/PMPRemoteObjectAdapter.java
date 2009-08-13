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

import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.internal.IAgentLog;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.pmp.PMPException;
import org.tigris.mtoolkit.iagent.pmp.RemoteMethod;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;
import org.tigris.mtoolkit.iagent.spi.Utils;

public abstract class PMPRemoteObjectAdapter implements RemoteObject {

	public static final int REPEAT = 1;
	public static final int CONTINUE = 2;

	protected RemoteObject delegate;

	protected long initialServiceId = -1;

	public PMPRemoteObjectAdapter(RemoteObject remote) {
		log("[Constructor] >>> Constructing: remote: " + remote);
		if (remote == null)
			throw new IllegalArgumentException();
		this.delegate = remote;
		try {
			Long l = (Long) Utils.callRemoteMethod(this.delegate, Utils.GET_REMOTE_SERVICE_ID_METHOD, null);
			this.initialServiceId = l.longValue();
			log("[Constructor] initialServiceId: " + l);
		} catch (IAgentException e) {
			IAgentLog.error("[PMPRemoteObjectAdapter][Constructor] Cant initialize service id.", e);
		}
	}

	public void dispose() throws PMPException {
		delegate.dispose();
	}

	public RemoteMethod getMethod(String name, String[] args) throws PMPException {
		return delegate.getMethod(name, args);
	}

	public RemoteMethod[] getMethods() throws PMPException {
		return delegate.getMethods();
	}

	public abstract int verifyRemoteReference() throws IAgentException;

	public long getInitialServiceID() {
		return this.initialServiceId;
	}

	public void setInitialServiceID(long newServiceID) {
		this.initialServiceId = newServiceID;
	}

	protected final void log(String message) {
		log(message, null);
	}

	protected final void log(String message, Throwable e) {
		DebugUtils.log(this, message, e);
	}

}
