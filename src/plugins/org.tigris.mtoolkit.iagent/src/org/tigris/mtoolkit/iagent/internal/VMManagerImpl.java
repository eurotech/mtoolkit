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
package org.tigris.mtoolkit.iagent.internal;

import java.io.OutputStream;

import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.VMManager;
import org.tigris.mtoolkit.iagent.internal.connection.ConnectionManager;
import org.tigris.mtoolkit.iagent.internal.connection.PMPConnection;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;

/**
 * Implementation of VMManager
 * 
 */
public class VMManagerImpl implements VMManager {

	private DeviceConnectorImpl connector;

	private RemoteObject lastKnownRemoteConsole;

	/**
	 * Creates new runtime commands with specified transport
	 * 
	 * @param aTransport
	 */
	public VMManagerImpl(DeviceConnectorImpl connector) {
		if (connector == null)
			throw new IllegalArgumentException();
		this.connector = connector;
	}

	public boolean isVMActive() throws IAgentException {
		return isVMConnectable();
	}

	public PMPConnection getPMPConnection() throws IAgentException {
		log("[getPMPConnection] >>>");
		if (!connector.isActive()) {
			log("[getPMPConnection] DeviceConnector is closed");
			throw new IAgentException("Associated DeviceConnector is closed", IAgentErrors.ERROR_DISCONNECTED);
		}
		ConnectionManager connManager = connector.getConnectionManager();
		PMPConnection pmpConnection = (PMPConnection) connManager.getActiveConnection(ConnectionManager.PMP_CONNECTION);
		if (pmpConnection == null) {
			log("[getPMPConnection] No PMP active connection. Creating new one...");
			pmpConnection = (PMPConnection) connManager.createConnection(ConnectionManager.PMP_CONNECTION);
		}
		log("[getPMPConnection] connection: " + pmpConnection);
		return pmpConnection;
	}

	public void executeFrameworkCommand(String command) throws IAgentException {
		log("[executeFrameworkCommand] >>> command: " + command);
		Utils.callRemoteMethod(getPMPConnection().getRemoteParserService(),
			Utils.EXECUTE_METHOD,
			new Object[] { command });
	}

	public void redirectFrameworkOutput(OutputStream os) throws IAgentException {
		log("[redirectFrameworkOutput] >>> os: " + os);
		PMPConnection connection = getPMPConnection();
		if (os != null) {
			RemoteObject parser = connection.getRemoteParserService();
			Utils.callRemoteMethod(parser, Utils.REGISTER_METHOD, new Object[] { os });
			if (lastKnownRemoteConsole != parser) {
				lastKnownRemoteConsole = parser;
			}
		} else {
			connection.releaseRemoteParserService();
		}
	}

	public boolean isVMConnectable() throws IAgentException {
		if (!connector.isActive()) {
			log("[getPMPConnection] DeviceConnector is closed");
			throw new IAgentException("Associated DeviceConnector is closed", IAgentErrors.ERROR_DISCONNECTED);
		}
		try {
			PMPConnection connection = getPMPConnection();
			if (connection == null || !connection.isConnected()) {
				log("[isVMConnectable] VM is not connectable");
				return false;
			} else {
				log("[isVMConnectable] VM is connectable");
				return true;
			}
		} catch (IAgentException e) {
			log("[isVMConnectable] VM is not connectable", e);
			return false;
		}
	}

	private final void log(String message) {
		log(message, null);
	}

	private final void log(String message, Throwable e) {
		DebugUtils.log(this, message, e);
	}

	public int getFrameworkStartLevel() throws IAgentException {
		log("[getFrameworkStartLevel] >>> ");
		Integer fwStartLevel = (Integer) Utils.callRemoteMethod(getPMPConnection().getRemoteBundleAdmin(),
			Utils.GET_FW_START_LEVEL,
			new Object[0]);
		return fwStartLevel.intValue();
	}
}
