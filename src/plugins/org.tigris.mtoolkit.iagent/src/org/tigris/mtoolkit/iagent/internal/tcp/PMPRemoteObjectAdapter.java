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
import org.tigris.mtoolkit.iagent.spi.MethodSignature;
import org.tigris.mtoolkit.iagent.spi.Utils;

public abstract class PMPRemoteObjectAdapter implements RemoteObject {

	private static MethodSignature GET_REMOTE_SERVICE_ID_METHOD = new MethodSignature("getRemoteServiceID", Utils.NO_ARGS, true);

	public static final int REPEAT = 1;
	public static final int CONTINUE = 2;

	protected RemoteObject delegate;

	protected long initialServiceId = -1;

	public PMPRemoteObjectAdapter(RemoteObject remote) {
		debug("[Constructor] >>> Constructing: remote: " + remote);
		if (remote == null)
			throw new IllegalArgumentException();
		this.delegate = remote;
		try {
			Long l = new Long(-1);
			if (GET_REMOTE_SERVICE_ID_METHOD.isDefined(this.delegate)) {
				l = (Long) GET_REMOTE_SERVICE_ID_METHOD.call(this.delegate);
			}
			this.initialServiceId = l.longValue();
			debug("[Constructor] initialServiceId: " + l);
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
	
	public boolean validateCache(RemoteMethod method) {
		return delegate == method.getRemoteObject();
	}

	public abstract int verifyRemoteReference() throws IAgentException;

	public long getInitialServiceID() {
		return this.initialServiceId;
	}

	public void setInitialServiceID(long newServiceID) {
		this.initialServiceId = newServiceID;
	}

	protected final void debug(String message) {
		DebugUtils.debug(this, message);
	}
}
