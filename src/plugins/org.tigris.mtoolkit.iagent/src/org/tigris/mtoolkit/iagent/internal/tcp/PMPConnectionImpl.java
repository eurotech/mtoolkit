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

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.pmp.EventListener;
import org.tigris.mtoolkit.iagent.pmp.PMPException;
import org.tigris.mtoolkit.iagent.pmp.PMPService;
import org.tigris.mtoolkit.iagent.pmp.PMPServiceFactory;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;
import org.tigris.mtoolkit.iagent.spi.ConnectionManager;
import org.tigris.mtoolkit.iagent.spi.PMPConnection;
import org.tigris.mtoolkit.iagent.spi.PMPConnector;
import org.tigris.mtoolkit.iagent.spi.Utils;
import org.tigris.mtoolkit.iagent.transport.Transport;
import org.tigris.mtoolkit.iagent.util.LightServiceRegistry;

public class PMPConnectionImpl implements PMPConnection, EventListener {

	private org.tigris.mtoolkit.iagent.pmp.PMPConnection pmpConnection;
	private ConnectionManagerImpl connManager;

	private HashMap remoteObjects = new HashMap(5);
	private RemoteObject administration;
	private RemoteObject remoteParserService;

	private LightServiceRegistry pmpRegistry;
	private volatile boolean closed = false;

	public PMPConnectionImpl(Transport transport, Dictionary conProperties, ConnectionManagerImpl connManager)
			throws IAgentException {
		debug("[Constructor] >>> Create PMP Connection: props: " + DebugUtils.convertForDebug(conProperties)
				+ "; manager: " + connManager);

		PMPService pmpService = PMPServiceFactory.getDefault();
		try {
			String portString = (String) conProperties.get(DeviceConnector.PROP_PMP_PORT);
			Integer port = portString != null ? Integer.decode(portString)  : getPmpPort(connManager);
			if (port != null && port.intValue() == 0)
				throw new IAgentException("Cannot determine PMP port", IAgentErrors.ERROR_CANNOT_CONNECT);
			if (port != null)
				conProperties.put(PMPService.PROP_PMP_PORT, port);
			debug("[Constructor] Transport: " + transport);
			pmpConnection = pmpService.connect(transport, conProperties);
		} catch (PMPException e) {
			info("[Constructor] Failed to create PMP connection 1", e);
			if ("socket".equals(transport.getType().getTypeId())) {
				// if we are using old socket protocol, try to create
				// backward compatible connection
				try {
					pmpConnection = createClosedConnection(transport.getId());
				} catch (PMPException e2) {
					info("[Constructor] Failed to create PMP connection 2", e2);
					throw new IAgentException("Unable to connect to the framework", IAgentErrors.ERROR_CANNOT_CONNECT,
							e2);
				}
			}
			if (pmpConnection == null)
				throw new IAgentException("Unable to connect to the framework", IAgentErrors.ERROR_CANNOT_CONNECT, e);
		}
		this.connManager = connManager;
		pmpConnection.addEventListener(this,
				new String[] { org.tigris.mtoolkit.iagent.pmp.PMPConnection.FRAMEWORK_DISCONNECTED });
	}
	
	private Integer getPmpPort(ConnectionManager manager) throws IAgentException {
		return (Integer) manager.queryProperty(ConnectionManager.PROP_PMP_PORT);
	}

	public int getType() {
		return ConnectionManager.PMP_CONNECTION;
	}

	public void closeConnection() throws IAgentException {
		closeConnection(true);
	}

	public void closeConnection(boolean aSendEvent) throws IAgentException {
		debug("[closeConnection] >>>");
		synchronized (this) {
			if (closed) {
				debug("[closeConnection] Already closed");
				return;
			}
			closed = true;
		}

		try {
			resetRemoteReferences();

			debug("[closeConnection] remove event listener");
			pmpConnection.removeEventListener(this,
					new String[] { org.tigris.mtoolkit.iagent.pmp.PMPConnection.FRAMEWORK_DISCONNECTED });
			pmpConnection.disconnect("Integration Agent request");
		} finally {
			if (connManager != null) {
				try {
					connManager.connectionClosed(this, aSendEvent);
				} catch (Throwable e) {
					error("[closeConnection] Internal error in connection manager", e);
				}
			}
		}
	}

	private org.tigris.mtoolkit.iagent.pmp.PMPConnection createClosedConnection(String targetIP) throws PMPException {
		org.tigris.mtoolkit.iagent.pmp.PMPConnection connection = null;
		if (targetIP == null)
			throw new IllegalArgumentException(
					"Connection properties hashtable does not contain device IP value with key DeviceConnector.KEY_DEVICE_IP!");
		PMPConnector connectionMngr = (PMPConnector) getManager("org.tigris.mtoolkit.iagent.spi.PMPConnector");
		if (connectionMngr != null) {
			connection = connectionMngr.createPMPConnection(targetIP);
		}
		return connection;
	}

	private void resetRemoteReferences() {
		debug("[resetRemoteReferences] >>>");
		if (remoteObjects != null) {
			Collection objects = remoteObjects.values();
			for (Iterator iterator = objects.iterator(); iterator.hasNext();) {
				RemoteObject remoteObject = (RemoteObject) iterator.next();
				try {
					remoteObject.dispose();
				} catch (PMPException e) {
					error("[resetRemoteReferences] Failure during PMP connection cleanup", e);
				}
			}
			remoteObjects.clear();
		}

		if (remoteParserService != null) {
			try {
				remoteParserService.dispose();
			} catch (PMPException e) {
				error("[resetRemoteReferences] Failure during PMP connection cleanup", e);
			}
			remoteParserService = null;
		}

		if (administration != null) {
			try {
				administration.dispose();
			} catch (PMPException e) {
				error("[resetRemoteReferences] Failure during PMP connection cleanup", e);
			}
			administration = null;
		}
	}

	public boolean isConnected() {
		return !closed && pmpConnection.isConnected();
	}

	public RemoteObject getRemoteBundleAdmin() throws IAgentException {
		return getRemoteAdmin(REMOTE_BUNDLE_ADMIN_NAME);
	}

	public RemoteObject getRemoteApplicationAdmin() throws IAgentException {
		return getRemoteAdmin(REMOTE_APPLICATION_ADMIN_NAME);
	}

	public RemoteObject getRemoteDeploymentAdmin() throws IAgentException {
		return getRemoteAdmin(REMOTE_DEPLOYMENT_ADMIN_NAME);
	}

	public RemoteObject getRemoteParserService() throws IAgentException {
		debug("[getRemoteParserService] >>>");
		if (!isConnected()) {
			info("[getRemoteParserService] The connecton has been closed!");
			throw new IAgentException("The connecton has been closed!", IAgentErrors.ERROR_DISCONNECTED);
		}
		if (remoteParserService == null) {
			debug("[getRemoteParserService] No RemoteParserService. Creating");
			try {
				remoteParserService = pmpConnection.getReference(REMOTE_CONSOLE_NAME, null);
			} catch (PMPException e) {
				info("[getRemoteParserService] RemoteParserGenerator service isn't available", e);
				throw new IAgentException("Unable to retrieve reference to remote administration service " + REMOTE_CONSOLE_NAME,
						IAgentErrors.ERROR_INTERNAL_ERROR);
			}
		}
		return remoteParserService;
	}

	public void releaseRemoteParserService() throws IAgentException {
		debug("[releaseRemoteParserService] >>>");
		if (remoteParserService != null) {
			try {
				Utils.callRemoteMethod(remoteParserService, Utils.RELEASE_METHOD, null);
				remoteParserService.dispose();
			} catch (PMPException e) {
				error("[releaseRemoteParserService]", e);
			}
			remoteParserService = null;
		}
	}

	public void addEventListener(EventListener listener, String[] eventTypes) throws IAgentException {
		debug("[addEventListener] >>> listener: " + listener + "; eventTypes: " + DebugUtils.convertForDebug(eventTypes));
		if (!isConnected()) {
			info("[addEventListener] The connecton has been closed!");
			throw new IAgentException("The connecton has been closed!", IAgentErrors.ERROR_DISCONNECTED);
		}

		pmpConnection.addEventListener(listener, eventTypes);
	}

	public void removeEventListener(EventListener listener, String[] eventTypes) throws IAgentException {
		debug("[removeEventListener] listener: " + listener + "; eventTypes: " + DebugUtils.convertForDebug(eventTypes));
		if (!isConnected()) {
			info("[removeEventListener] The connecton has been closed!");
			throw new IAgentException("The connecton has been closed!", IAgentErrors.ERROR_DISCONNECTED);
		}
		pmpConnection.removeEventListener(listener, eventTypes);
	}

	public RemoteObject getRemoteServiceAdmin() throws IAgentException {
		return getRemoteAdmin(REMOTE_SERVICE_ADMIN_NAME);
	}

	public RemoteObject getRemoteAdmin(String adminClassName) throws IAgentException {
		debug("[getRemoteAdmin]" + adminClassName + " >>>");
		if (!isConnected()) {
			info("[getRemoteBundleAdmin] The connecton has been closed!");
			throw new IAgentException("The connecton has been closed!", IAgentErrors.ERROR_DISCONNECTED);
		}
		RemoteObject admin = (RemoteObject) remoteObjects.get(adminClassName);
		if (admin == null) {
			try {
				debug("[getRemoteAdmin] No remote admin [" + adminClassName + "]. Creating...");
				final String adminClass = adminClassName;
				admin = new PMPRemoteObjectAdapter(pmpConnection.getReference(adminClassName, null)) {
					public int verifyRemoteReference() throws IAgentException {
						if (!pmpConnection.isConnected()) {
							info("[verifyRemoteReference] The connection has been closed!");
							throw new IAgentException("The connecton has been closed!", IAgentErrors.ERROR_DISCONNECTED);
						}
						try {
							RemoteObject newRemoteObject = pmpConnection.getReference(adminClass, null);
							Long l = new Long(-1);
							if (Utils.isRemoteMethodDefined(newRemoteObject, Utils.GET_REMOTE_SERVICE_ID_METHOD)) {
								l = (Long) Utils.callRemoteMethod(newRemoteObject, Utils.GET_REMOTE_SERVICE_ID_METHOD, null);
							}
							long newServiceID = l.longValue();
							if (newServiceID == -1) {
								debug("[verifyRemoteReference] New reference service id is = -1. Nothing to do. Continuing.");
								return PMPRemoteObjectAdapter.CONTINUE;
							}
							debug("[verifyRemoteReference] initial: " + this.getInitialServiceID() + "; new: " + l);
							if (newServiceID != this.getInitialServiceID()) {
								this.delegate = newRemoteObject;
								this.setInitialServiceID(newServiceID);
								debug("[verifyRemoteReference] Reference to remote service was refreshed. Retry remote method call...");
								return PMPRemoteObjectAdapter.REPEAT;
							}
							newRemoteObject.dispose();
							debug("[verifyRemoteReference] Reference to remote service is looking fine. Continue");
							return PMPRemoteObjectAdapter.CONTINUE;
						} catch (PMPException e) {
							// admin = null;
							info("[verifyRemoteReference] Reference to remote service cannot be got, service is not available. Fail fast.",
											e);
							throw new IAgentException("Unable to retrieve reference to remote administration service " + adminClass,
									IAgentErrors.ERROR_REMOTE_ADMIN_NOT_AVAILABLE, e);
						}
					}
				};
				if (admin != null)
					remoteObjects.put(adminClassName, admin);
			} catch (PMPException e) {
				info("[getRemoteAdmin] Remote admin [" + adminClassName + "] isn't available", e);
				throw new IAgentException("Unable to retrieve reference to remote administration service ["
						+ adminClassName + "]", IAgentErrors.ERROR_REMOTE_ADMIN_NOT_AVAILABLE, e);
			}
		}
		return admin;
	}

	public void event(Object ev, String evType) {
		debug("[event] >>> Object event: " + ev + "; eventType: " + evType);
		if (org.tigris.mtoolkit.iagent.pmp.PMPConnection.FRAMEWORK_DISCONNECTED.equals(evType)) {
			try {
				debug("[event] Framework disconnection event received");
				closeConnection();
			} catch (Throwable e) {
				error("[event] Exception while cleaning up the connection", e);
			}
		}
	}

	private final void debug(String message) {
		DebugUtils.debug(this, message);
	}

	private final void info(String message) {
		DebugUtils.info(this, message);
	}

	private final void info(String message, Throwable e) {
		DebugUtils.info(this, message, e);
	}

	private final void error(String message, Throwable e) {
		DebugUtils.error(this, message, e);
	}

	private LightServiceRegistry getServiceRegistry() {
		if (pmpRegistry == null)
			pmpRegistry = new LightServiceRegistry(PMPConnectionImpl.class.getClassLoader());
		return pmpRegistry;
	}

	public Object getManager(String className) {
		LightServiceRegistry registry = getServiceRegistry();
		return registry.get(className);
	}
	
	public Object getProperty(String propertyName) {
		return null;
	}
}
