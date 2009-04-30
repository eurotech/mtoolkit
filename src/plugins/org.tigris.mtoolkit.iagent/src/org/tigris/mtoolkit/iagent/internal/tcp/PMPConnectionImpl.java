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

import java.util.Dictionary;

import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.internal.Utils;
import org.tigris.mtoolkit.iagent.internal.connection.ConnectionManager;
import org.tigris.mtoolkit.iagent.internal.connection.PMPConnection;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.pmp.EventListener;
import org.tigris.mtoolkit.iagent.pmp.PMPException;
import org.tigris.mtoolkit.iagent.pmp.PMPService;
import org.tigris.mtoolkit.iagent.pmp.PMPServiceFactory;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;
import org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin;
import org.tigris.mtoolkit.iagent.rpc.RemoteConsole;
import org.tigris.mtoolkit.iagent.rpc.RemoteDeploymentAdmin;
import org.tigris.mtoolkit.iagent.rpc.RemoteServiceAdmin;

public class PMPConnectionImpl implements PMPConnection, EventListener {

	private static final String REMOTE_BUNDLE_ADMIN = RemoteBundleAdmin.class.getName();
	private static final String REMOTE_DEPLOYMENT_ADMIN = RemoteDeploymentAdmin.class.getName();
	private static final String REMOTE_CONSOLE = RemoteConsole.class.getName();
	private static final String REMOTE_SERVICE_ADMIN = RemoteServiceAdmin.class.getName();

	private org.tigris.mtoolkit.iagent.pmp.PMPConnection pmpConnection;
	private RemoteObject administration;
	private PMPRemoteObjectAdapter remoteBundleAdmin;
	private PMPRemoteObjectAdapter remoteDeploymentAdmin;
	private RemoteObject remoteParserService;
	private PMPRemoteObjectAdapter remoteServiceAdmin;

	private ConnectionManagerImpl connManager;

	private volatile boolean closed = false;

	public PMPConnectionImpl(Dictionary conProperties, ConnectionManagerImpl connManager) throws IAgentException {
		log("[Constructor] >>> Create PMP Connection: props: "
						+ DebugUtils.convertForDebug(conProperties)
						+ "; manager: "
						+ connManager);
		String targetIP = (String) conProperties.get(DeviceConnector.KEY_DEVICE_IP);
		if (targetIP == null)
			throw new IllegalArgumentException("Connection properties hashtable does not contain device IP value with key DeviceConnector.KEY_DEVICE_IP!");

		PMPService pmpService = PMPServiceFactory.getDefault();
		try {
			log("[Constructor] PMP connection spec: " + targetIP);
			pmpConnection = pmpService.connect(targetIP);
		} catch (PMPException e) {
			log("[Constructor] Failed to create PMP connection", e);
			throw new IAgentException("Unable to connect to the framework", IAgentErrors.ERROR_CANNOT_CONNECT, e);
		}
		this.connManager = connManager;
		pmpConnection.addEventListener(this,
			new String[] { org.tigris.mtoolkit.iagent.pmp.PMPConnection.FRAMEWORK_DISCONNECTED });
	}

	public int getType() {
		return ConnectionManager.PMP_CONNECTION;
	}

	public void closeConnection() throws IAgentException {
		log("[closeConnection] >>>");
		synchronized (this) {
			if (closed) {
				log("[closeConnection] Already closed");
				return;
			}
			closed = true;
		}

		try {
			resetRemoteReferences();

			log("[closeConnection] remove event listener");
			pmpConnection.removeEventListener(this,
				new String[] { org.tigris.mtoolkit.iagent.pmp.PMPConnection.FRAMEWORK_DISCONNECTED });
			pmpConnection.disconnect("Integration Agent request");
		} finally {
			if (connManager != null) {
				try {
					connManager.connectionClosed(this);
				} catch (Throwable e) {
					log("[closeConnection] Internal error in connection manager", e);
				}
			}
		}
	}

	private void resetRemoteReferences() {
		log("[resetRemoteReferences] >>>");
		Utils.clearCache();
		if (remoteBundleAdmin != null) {
			try {
				remoteBundleAdmin.dispose();
			} catch (PMPException e) {
				log("[resetRemoteReferences] Failure during PMP connection cleanup", e);
			}
			remoteBundleAdmin = null;
		}

		if (remoteDeploymentAdmin != null) {
			try {
				remoteDeploymentAdmin.dispose();
			} catch (PMPException e) {
				log("[resetRemoteReferences] Failure during PMP connection cleanup", e);
			}
			remoteDeploymentAdmin = null;
		}

		if (remoteParserService != null) {
			try {
				remoteParserService.dispose();
			} catch (PMPException e) {
				log("[resetRemoteReferences] Failure during PMP connection cleanup", e);
			}
			remoteParserService = null;
		}

		if (remoteServiceAdmin != null) {
			try {
				remoteServiceAdmin.dispose();
			} catch (PMPException e) {
				log("[resetRemoteReferences] Failure during PMP connection cleanup", e);
			}
			remoteServiceAdmin = null;
		}

		if (administration != null) {
			try {
				administration.dispose();
			} catch (PMPException e) {
				log("[resetRemoteReferences] Failure during PMP connection cleanup", e);
			}
			administration = null;
		}
	}

	public boolean isConnected() {
		return !closed && pmpConnection.isConnected();
	}

	public RemoteObject getRemoteBundleAdmin() throws IAgentException {
		log("[getRemoteBundleAdmin] >>>");
		if (!isConnected()) {
			log("[getRemoteBundleAdmin] The connecton has been closed!");
			throw new IAgentException("The connecton has been closed!", IAgentErrors.ERROR_DISCONNECTED);
		}
		if (remoteBundleAdmin == null) {
			try {
				log("[getRemoteBundleAdmin] No remote bundle admin. Creating...");
				remoteBundleAdmin = new PMPRemoteObjectAdapter(pmpConnection.getReference(REMOTE_BUNDLE_ADMIN, null)) {
					public int verifyRemoteReference() throws IAgentException {
						if (!pmpConnection.isConnected()) {
							this.log("[verifyRemoteReference] The connection has been closed!");
							throw new IAgentException("The connecton has been closed!", IAgentErrors.ERROR_DISCONNECTED);
						}
						try {
							RemoteObject newRemoteObject = pmpConnection.getReference(REMOTE_BUNDLE_ADMIN, null);
							Long l = (Long) Utils.callRemoteMethod(newRemoteObject,
								Utils.GET_REMOTE_SERVICE_ID_METHOD,
								null);
							long newServiceID = l.longValue();
							if (newServiceID == -1) {
								this.log("[verifyRemoteReference] New reference service id is = -1. Nothing to do. Continuing.");
								return PMPRemoteObjectAdapter.CONTINUE;
							}
							this.log("[verifyRemoteReference] initial: " + this.getInitialServiceID() + "; new: " + l);
							if (newServiceID != this.getInitialServiceID()) {
								this.delegate = newRemoteObject;
								this.setInitialServiceID(newServiceID);
								this.log("[verifyRemoteReference] Reference to remote service was refreshed. Retry remote method call...");
								return PMPRemoteObjectAdapter.REPEAT;
							}
							newRemoteObject.dispose();
							this.log("[verifyRemoteReference] Reference to remote service is looking fine. Continue");
							return PMPRemoteObjectAdapter.CONTINUE;
						} catch (PMPException e) {
							remoteServiceAdmin = null;
							this.log("[verifyRemoteReference] Reference to remote service cannot be got, service is not available. Fail fast.",
								e);
							throw new IAgentException("Unable to retrieve reference to remote administration service",
								IAgentErrors.ERROR_REMOTE_BUNDLE_ADMIN_NOT_ACTIVE,
								e);
						}
					}
				};
			} catch (PMPException e) {
				this.log("[getRemoteBundleAdmin] Remote bundle admin isn't available", e);
				throw new IAgentException("Unable to retrieve reference to remote administration service",
					IAgentErrors.ERROR_REMOTE_BUNDLE_ADMIN_NOT_ACTIVE,
					e);
			}
		}
		return remoteBundleAdmin;
	}

	public RemoteObject getRemoteDeploymentAdmin() throws IAgentException {
		log("[getRemoteDeploymentAdmin] >>>");
		if (!isConnected()) {
			log("[getRemoteDeploymentAdmin] The connecton has been closed!");
			throw new IAgentException("The connecton has been closed!", IAgentErrors.ERROR_DISCONNECTED);
		}
		if (remoteDeploymentAdmin == null) {
			try {
				log("[getRemoteDeploymentAdmin] No RemoteDeploymentAdmin. Creating...");
				remoteDeploymentAdmin = new PMPRemoteObjectAdapter(pmpConnection.getReference(REMOTE_DEPLOYMENT_ADMIN,
					null)) {
					public int verifyRemoteReference() throws IAgentException {
						if (!pmpConnection.isConnected()) {
							this.log("[verifyRemoteReference] The connection has been closed!");
							throw new IAgentException("The connecton has been closed!", IAgentErrors.ERROR_DISCONNECTED);
						}
						try {
							RemoteObject newRemoteObject = pmpConnection.getReference(REMOTE_DEPLOYMENT_ADMIN, null);
							Long l = (Long) Utils.callRemoteMethod(newRemoteObject,
								Utils.GET_REMOTE_SERVICE_ID_METHOD,
								null);
							long newServiceID = l.longValue();
							if (newServiceID == -1) {
								this.log("[verifyRemoteReference] New reference service id is = -1. Nothing to do. Continuing.");
								return PMPRemoteObjectAdapter.CONTINUE;
							}
							this.log("[verifyRemoteReference] initial: " + this.getInitialServiceID() + "; new: " + l);
							if (newServiceID != this.getInitialServiceID()) {
								this.delegate = newRemoteObject;
								this.setInitialServiceID(newServiceID);
								this.log("[verifyRemoteReference] Reference to remote service was refreshed. Retry remote method call...");
								return PMPRemoteObjectAdapter.REPEAT;
							}
							newRemoteObject.dispose();
							this.log("[verifyRemoteReference] Reference to remote service is looking fine. Continue");
							return PMPRemoteObjectAdapter.CONTINUE;
						} catch (PMPException e) {
							remoteDeploymentAdmin = null;
							this.log("[verifyRemoteReference] Reference to remote service cannot be got, service is not available. Fail fast",
								e);
							IAgentException agentEx = new IAgentException("RemoteDeploymentAdmin is not available",
								IAgentErrors.ERROR_DEPLOYMENT_ADMIN_NOT_ACTIVE,
								e);
							throw agentEx;
						}
					}
				};
			} catch (PMPException e) {
				this.log("[getRemoteDeploymentAdmin] Remote deployment admin isn't available", e);
				throw new IAgentException("Unable to retrieve reference to remote administration service",
					IAgentErrors.ERROR_DEPLOYMENT_ADMIN_NOT_ACTIVE,
					e);
			}
		}
		return remoteDeploymentAdmin;
	}

	public RemoteObject getRemoteParserService() throws IAgentException {
		log("[getRemoteParserService] >>>");
		if (!isConnected()) {
			log("[getRemoteParserService] The connecton has been closed!");
			throw new IAgentException("The connecton has been closed!", IAgentErrors.ERROR_DISCONNECTED);
		}
		if (remoteParserService == null) {
			log("[getRemoteParserService] No RemoteParserService. Creating");
			try {
				remoteParserService = pmpConnection.getReference(REMOTE_CONSOLE, null);
			} catch (PMPException e) {
				log("[getRemoteParserService] RemoteParserGenerator service isn't available", e);
				throw new IAgentException("Unable to retrieve reference to remote administration service",
					IAgentErrors.ERROR_INTERNAL_ERROR);
			}
		}
		return remoteParserService;
	}

	public void releaseRemoteParserService() throws IAgentException {
		log("[releaseRemoteParserService] >>>");
		if (remoteParserService != null) {
			try {
				Utils.callRemoteMethod(remoteParserService, Utils.RELEASE_METHOD, null);
				remoteParserService.dispose();
			} catch (PMPException e) {
				log("[releaseRemoteParserService]", e);
			}
			remoteParserService = null;
		}
	}

	public void addEventListener(EventListener listener, String[] eventTypes) throws IAgentException {
		log("[addEventListener] >>> listener: " + listener + "; eventTypes: " + DebugUtils.convertForDebug(eventTypes));
		if (!isConnected()) {
			log("[addEventListener] The connecton has been closed!");
			throw new IAgentException("The connecton has been closed!", IAgentErrors.ERROR_DISCONNECTED);
		}

		pmpConnection.addEventListener(listener, eventTypes);
	}

	public void removeEventListener(EventListener listener, String[] eventTypes) throws IAgentException {
		log("[removeEventListener] listener: " + listener + "; eventTypes: " + DebugUtils.convertForDebug(eventTypes));
		if (!isConnected()) {
			log("[removeEventListener] The connecton has been closed!");
			throw new IAgentException("The connecton has been closed!", IAgentErrors.ERROR_DISCONNECTED);
		}
		pmpConnection.removeEventListener(listener, eventTypes);
	}

	public RemoteObject getRemoteServiceAdmin() throws IAgentException {
		log("[getRemoteServiceAdmin] >>>");
		if (!isConnected()) {
			log("[getRemoteServiceAdmin] The connecton has been closed!");
			throw new IAgentException("The connecton has been closed!", IAgentErrors.ERROR_DISCONNECTED);
		}
		if (remoteServiceAdmin == null) {
			try {
				log("[getRemoteServiceAdmin] No RemoteServiceAdmin. Creating...");
				remoteServiceAdmin = new PMPRemoteObjectAdapter(pmpConnection.getReference(REMOTE_SERVICE_ADMIN, null)) {
					public int verifyRemoteReference() throws IAgentException {
						if (!pmpConnection.isConnected()) {
							this.log("[verifyRemoteReference] The connection has been closed!");
							throw new IAgentException("The connecton has been closed!", IAgentErrors.ERROR_DISCONNECTED);
						}
						try {
							RemoteObject newRemoteObject = pmpConnection.getReference(REMOTE_SERVICE_ADMIN, null);
							Long l = (Long) Utils.callRemoteMethod(newRemoteObject,
								Utils.GET_REMOTE_SERVICE_ID_METHOD,
								null);
							long newServiceID = l.longValue();
							if (newServiceID == -1) {
								this.log("[verifyRemoteReference] New reference service id is = -1. Nothing to do. Continuing.");
								return PMPRemoteObjectAdapter.CONTINUE;
							}
							this.log("[verifyRemoteReference] initial: " + this.getInitialServiceID() + "; new: " + l);
							if (newServiceID != this.getInitialServiceID()) {
								this.delegate = newRemoteObject;
								this.setInitialServiceID(newServiceID);
								this.log("[verifyRemoteReference] Reference to remote service was refreshed. Retry remote method call...");
								return PMPRemoteObjectAdapter.REPEAT;
							}
							newRemoteObject.dispose();
							this.log("[verifyRemoteReference] Reference to remote service is looking fine. Continue");
							return PMPRemoteObjectAdapter.CONTINUE;
						} catch (PMPException e) {
							remoteServiceAdmin = null;
							this.log("[verifyRemoteReference] Reference to remote service cannot be got, service is not available. Fail fast",
								e);
							throw new IAgentException("Unable to retrieve reference to remote administration service",
								IAgentErrors.ERROR_REMOTE_SERVICE_ADMIN_NOT_ACTIVE,
								e);
						}
					}
				};
			} catch (PMPException e) {
				log("[getRemoteServiceAdmin] RemoteServiceAdmin isnt't available", e);
				throw new IAgentException("Unable to retrieve reference to remote administration service",
					IAgentErrors.ERROR_REMOTE_SERVICE_ADMIN_NOT_ACTIVE,
					e);
			}
		}
		return remoteServiceAdmin;
	}

	public void event(Object ev, String evType) {
		log("[event] >>> Object event: " + ev + "; eventType: " + evType);
		if (org.tigris.mtoolkit.iagent.pmp.PMPConnection.FRAMEWORK_DISCONNECTED.equals(evType)) {
			try {
				log("[event] Framework disconnection event received");
				closeConnection();
			} catch (Throwable e) {
				log("[event] Exception while cleaning up the connection", e);
			}
		}
	}

	private final void log(String message) {
		log(message, null);
	}

	private final void log(String message, Throwable e) {
		DebugUtils.log(this, message, e);
	}
}
