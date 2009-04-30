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

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.tigris.mtoolkit.iagent.pmp.PMPConnection;
import org.tigris.mtoolkit.iagent.pmp.PMPContext;
import org.tigris.mtoolkit.iagent.rpc.Remote;

public class InvocationThread implements Runnable, PMPContext {

	private static ThreadLocal invocationContext = new ThreadLocal();

	private PMPSessionThread session;
	private Method m;
	private boolean serflag;
	private Object objContext;
	private Object[] args;
	private short msgID;
	private Object obj;

	public InvocationThread(PMPSessionThread session,
							Method m,
							Object obj,
							boolean serflag,
							Object context,
							Object[] args,
							short msgID) {
		this.session = session;
		this.m = m;
		this.obj = obj;
		this.serflag = serflag;
		this.objContext = context;
		this.args = args;
		this.msgID = msgID;
		session.pool.enqueueWork(this);
	}

	public void run() {
		invocationContext.set(this);
		Object result;
		try {
			result = m.invoke(obj, args);
		} catch (Exception exc) {
			String errMsg = new String();
			if (exc instanceof InvocationTargetException) {
				errMsg = ((InvocationTargetException) exc).getTargetException().toString();
				session.error("Target Exception: ", ((InvocationTargetException) exc).getTargetException());
			} else {
				errMsg = exc.toString();
				session.error("Invocation Error", exc);
			}
			session.writeInvocationError(errMsg, msgID);
			return;
		} finally {
			invocationContext.set(null);
		}
		String returnType = m.getReturnType().getName();
		if (returnType.equals(PMPData.TYPES1[8]) || returnType.equals(PMPData.TYPES2[8])) {
			try {
				session.os.begin(msgID);
				session.os.write(PMPSessionThread.SERIALIZED);
				session.os.write(1);
				session.os.end(false);
			} catch (Exception exc) {
				session.os.unlock();
				if (session.running)
					session.writeInvocationError(exc.toString(), msgID);
				session.error(PMPSessionThread.ERRMSG2, exc);
			}
		} else if (serflag || result == null) {
			try {
				session.os.begin(msgID);
				session.os.write(PMPSessionThread.SERIALIZED);
				session.os.write(1);
				PMPData.writeObject(result, session.os, true);
				session.os.end(false);
			} catch (Exception exc) {
				session.os.unlock();
				if (session.running)
					session.writeInvocationError(exc.toString(), msgID);
				session.error(PMPSessionThread.ERRMSG2, exc);
			}
		} else {
			Class[] interfaces = null;
			if (result instanceof InputStream && !(result instanceof Remote)) {
				result = new RemoteInputStream((InputStream) result);
				interfaces = ((Remote) result).remoteInterfaces();
			} else if (!(result instanceof Remote)
							|| !PMPServiceImpl.checkInstance(interfaces = ((Remote) result).remoteInterfaces(),
								result.getClass())) {
				String errMsg = "Method result "
								+ result
								+ " Is not instance of "
								+ Remote.class.getName()
								+ " or one of its remote interfaces";
				session.debug(errMsg);
				session.writeInvocationError(errMsg, msgID);
				return;
			}
			int objID = session.addRemoteObject(result, interfaces, objContext);
			try {
				session.os.begin(msgID);
				session.os.write(PMPSessionThread.REFERENCE);
				PMPData.writeInt(objID, session.os);
				session.os.end(false);
			} catch (Exception exc) {
				session.os.unlock();
				session.writeInvocationError(exc.getMessage(), msgID);
				session.error(PMPSessionThread.ERRMSG2, exc);
			}
		}
	}

	public PMPConnection getConnection() {
		return session.getConnection();
	}

	public void postEvent(Object event, String eventType) {
		session.postEvent(event, eventType);
	}

	public String getSessionID() {
		return session.sessionID;
	}

	public static PMPContext getContext() {
		return (PMPContext) invocationContext.get();
	}
}
