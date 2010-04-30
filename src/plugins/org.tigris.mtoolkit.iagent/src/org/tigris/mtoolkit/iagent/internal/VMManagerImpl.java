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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.tigris.mtoolkit.iagent.ExtVMManager;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.VMManager;
import org.tigris.mtoolkit.iagent.instrumentation.Instrument;
import org.tigris.mtoolkit.iagent.internal.tcp.DataFormater;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;
import org.tigris.mtoolkit.iagent.spi.ConnectionManager;
import org.tigris.mtoolkit.iagent.spi.MBSAConnection;
import org.tigris.mtoolkit.iagent.spi.MBSAConnectionCallBack;
import org.tigris.mtoolkit.iagent.spi.PMPConnection;
import org.tigris.mtoolkit.iagent.spi.Utils;
import org.tigris.mtoolkit.iagent.util.LightServiceRegistry;


/**
 * Implementation of VMManager
 * 
 */
public class VMManagerImpl implements VMManager {

	private DeviceConnectorImpl connector;

	private RemoteObject lastKnownRemoteConsole;

	private LightServiceRegistry extensionsRegistry;

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

	public void executeFrameworkCommand(String command) throws IAgentException {
		debug("[executeFrameworkCommand] >>> command: " + command);
		Utils.callRemoteMethod(getPMPConnection().getRemoteParserService(),
			Utils.EXECUTE_METHOD,
			new Object[] { command });
	}

	public void redirectFrameworkOutput(OutputStream os) throws IAgentException {
		debug("[redirectFrameworkOutput] >>> os: " + os);
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
			info("[getPMPConnection] DeviceConnector is closed");
			throw new IAgentException("Associated DeviceConnector is closed", IAgentErrors.ERROR_DISCONNECTED);
		}
		try {
			PMPConnection connection = getPMPConnection();
			if (connection == null || !connection.isConnected()) {
				debug("[isVMConnectable] VM is not connectable");
				return false;
			} else {
				debug("[isVMConnectable] VM is connectable");
				return true;
			}
		} catch (IAgentException e) {
			info("[isVMConnectable] VM is not connectable", e);
			return false;
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

	public int getFrameworkStartLevel() throws IAgentException {
		debug("[getFrameworkStartLevel] >>> ");
		Integer fwStartLevel = (Integer) Utils.callRemoteMethod(
				getPMPConnection().getRemoteBundleAdmin(), 
				Utils.GET_FW_START_LEVEL,
				new Object[0]);
		return fwStartLevel.intValue();
	}

	private PMPConnection getPMPConnection() throws IAgentException {
		return (PMPConnection) connector.getConnection(ConnectionManager.PMP_CONNECTION);
	}

	public void instrumentVM() throws IAgentException {
		Object[] extensions = getExtensionsRegistry().getAll(Instrument.class.getName());
		for (int i = 0; i < extensions.length; i++) {
			if (extensions[i] instanceof Instrument) {
				if (((Instrument) extensions[i]).instrumentVM(connector)) {
					// properly instrumented, stop now
					return;
				}
			}
		}
		throw new IAgentException("Failed to instrument remote framework", IAgentErrors.ERROR_INSTRUMENT_ERROR);
	}

	public boolean isVMInstrumented(boolean refresh) throws IAgentException {
		PMPConnection connection = null;
		try {
			connection = getPMPConnection();
			connection.getRemoteBundleAdmin();
			connection.getRemoteServiceAdmin();
		} catch (Exception e) {
			return false;
		}
		Object[] extensions = getExtensionsRegistry().getAll(Instrument.class.getName());
		for (int i = 0; i < extensions.length; i++) {
			if (extensions[i] instanceof Instrument) {
				if (!((Instrument) extensions[i]).isVMInstrumented(connector)) {
					return false;
				}
			}
		}
		return true;
	}

	private LightServiceRegistry getExtensionsRegistry() {
		if (extensionsRegistry == null) {
			extensionsRegistry = new LightServiceRegistry(VMManagerImpl.class.getClassLoader());
		}
		return extensionsRegistry;
	}

	public String[] listRawArgs() throws IAgentException {
		debug("[listRawArgs] >>>");
		MBSAConnection connection = getMBSAConnection();
		if ( !connection.isConnected() ){
		  info("[listRawArgs] Device is disconnected!");
		  throw new IAgentException("Device is disconnected!", IAgentErrors.ERROR_DISCONNECTED);
		}
		MBSAConnectionCallBack tCallBack = connection.sendData(IAgentCommands.IAGENT_CMD_LISTRAWARGS, null);
		int rspStatus = tCallBack.getRspStatus();
		if ( rspStatus >= 0 ) {
		  byte rspData[] = tCallBack.getRspData();
		  if ( rspData != null ) {
		    ByteArrayInputStream bis = null;
		    try {
		      bis = new ByteArrayInputStream(rspData);
		      String[] args = DataFormater.readStringArray(bis);
		      debug("[listRawArgs] Raw arguments list: " + DebugUtils.convertForDebug(args));
		      return args;
		    } catch(IOException e) {
		      info("[listRawArgs] Error formatting response data!", e);
		      throw new IAgentException("Error formatting response data!", IAgentErrors.ERROR_INTERNAL_ERROR, e);
		    } finally {
		      DataFormater.closeInputStream(bis);
		    }
		  } else {
			  debug("[listRawArgs] no arguments available");
		    return new String[0];
		  }
		} else {
		  info("[listRawArgs] Command failure: " + rspStatus);
		  throw new IAgentException("Command failure: " + rspStatus, rspStatus);
	    }
	}
	
	public void addRawArgument(String aRawArgument) throws IAgentException {
		debug("[addRawArgument] >>> aRawArgument: " + aRawArgument);
		MBSAConnection connection = getMBSAConnection();
		if ( !connection.isConnected() ){
		  info("[addRawArgument] Device is disconnected!");
		  throw new IAgentException("Device is disconnected!", IAgentErrors.ERROR_DISCONNECTED);
		}
		if ( aRawArgument == null ){
		  throw new IllegalArgumentException("Argument could not be null!");
		}
		ByteArrayOutputStream bos = null;
		try {
		  bos = new ByteArrayOutputStream(256);
		  DataFormater.writeString(bos, aRawArgument);
		} catch (IOException e) {
		  info("[addRawArgument] Error processing arguments!",e);
		  throw new IAgentException("Error processing arguments!", IAgentErrors.ERROR_INTERNAL_ERROR, e);
		}
		MBSAConnectionCallBack tCallBack = connection.sendData(IAgentCommands.IAGENT_CMD_ADDRAWARGUMENT, bos.toByteArray());
		DataFormater.closeOutputStream(bos);
		int rspStatus = tCallBack.getRspStatus();
		if ( rspStatus < 0 ){
		  info("[addRawArgument] Command failure: " + rspStatus);
		  throw new IAgentException("Command failure: " + rspStatus, rspStatus);
		} else {
		  debug("[addRawArgument] argument addition successful");
	    }
	} 
	  
	public boolean removeRawArgument(String aRawArgument) throws IAgentException {
		debug("[removeRawArgument] >>> aRawArgument: " + aRawArgument);
		MBSAConnection connection = getMBSAConnection();
		if (!connection.isConnected()) {
			info("[removeRawArgument] Device is disconnected!");
			throw new IAgentException("Device is disconnected!", IAgentErrors.ERROR_DISCONNECTED);
		}
		if (aRawArgument == null)
			throw new IllegalArgumentException("Argument could not be null!");
		ByteArrayOutputStream bos = null;
		try {
			bos = new ByteArrayOutputStream(256);
			DataFormater.writeString(bos, aRawArgument);
		} catch (IOException e) {
			info("[removeRawArgument] Error processing arguments!", e);
			throw new IAgentException("Error processing arguments!", IAgentErrors.ERROR_INTERNAL_ERROR, e);
		}
		MBSAConnectionCallBack tCallBack = connection.sendData(IAgentCommands.IAGENT_CMD_REMOVERAWARGUMENT, bos
				.toByteArray());
		DataFormater.closeOutputStream(bos);
		int rspStatus = tCallBack.getRspStatus();
		if (rspStatus < 0) {
			if (rspStatus == IAgentErrors.ERR_RUNTIME_ARG_NOT_FOUND) {
				debug("[removeRawArgument] Runtime argument cannot be found");
				return false;
			}
			info("[removeRawArgument] Command failure: " + rspStatus);
			throw new IAgentException("Failed to remove VM argument: " + rspStatus, rspStatus);
		} else {
			debug("[removeRawArgument] argument removal successful");
		}
		return true;
	}

	public void resetArgs() throws IAgentException {
		debug("[resetArgs] >>>");
		MBSAConnection connection = getMBSAConnection();
		if (!connection.isConnected()) {
			info("[resetArgs] Device is disconnected!");
			throw new IAgentException("Device is disconnected!", IAgentErrors.ERROR_DISCONNECTED);
		}
		MBSAConnectionCallBack tCallBack = connection.sendData(IAgentCommands.IAGENT_CMD_RESETARGS, null);
		int rspStatus = tCallBack.getRspStatus();
		if (rspStatus < 0) {
			info("[resetArgs] Command failure: " + rspStatus);
			throw new IAgentException("Failed to reset VM arguments: " + rspStatus, rspStatus);
		}
		debug("[resetArgs] Arguments successfully reset");
	}

	public void startVM() throws IAgentException {
		debug("[startVM] >>>");
		MBSAConnection connection = getMBSAConnection();
		if (!connection.isConnected()) {
			info("[startVM] Device is disconnected!");
			throw new IAgentException("Device is disconnected!", IAgentErrors.ERROR_DISCONNECTED);
		}
		MBSAConnectionCallBack tCallBack = connection.sendData(IAgentCommands.IAGENT_CMD_STARTVM, null);
		int rspStatus = tCallBack.getRspStatus();
		if (rspStatus < 0) {
			info("[startVM] Command failure: " + rspStatus);
			throw new IAgentException("Failed to start VM: " + rspStatus, rspStatus);
		}
		debug("[startVM] VM successfully started");
	}

	public void stopVM() throws IAgentException {
		debug("[stopVM] >>>");
		MBSAConnection connection = getMBSAConnection();
		if (!connection.isConnected()) {
			info("[stopVM] Device is disconnected!");
			throw new IAgentException("Device is disconnected!", IAgentErrors.ERROR_DISCONNECTED);
		}
		MBSAConnectionCallBack tCallBack = connection.sendData(IAgentCommands.IAGENT_CMD_STOPVM, null);
		int rspStatus = tCallBack.getRspStatus();
		if (rspStatus < 0) {
			info("[stopVM] Command failure: " + rspStatus);
			throw new IAgentException("Failed to stop VM: " + rspStatus, rspStatus);
		}
		debug("[stopVM] VM successfully stopped");
	}

	public Map getPlatformProperties() throws IAgentException {
		debug("[getPlatformProperties] >>>");
		try {
			MBSAConnection connection = getMBSAConnection();
			if (!connection.isConnected()) {
				info("[getPlatformProperties] Device is disconnected!");
				throw new IAgentException("Device is disconnected!", IAgentErrors.ERROR_DISCONNECTED);
			}
			MBSAConnectionCallBack tCallBack = connection.sendData(IAgentCommands.IAGENT_CMD_GETPLATFORMPROPERTIES, null, false);
			int rspStatus = tCallBack.getRspStatus();
			if (rspStatus >= 0) {
				byte rspData[] = tCallBack.getRspData();
				if (rspData != null) {
					ByteArrayInputStream bis = null;
					try {
						bis = new ByteArrayInputStream(rspData);
						String[] props = DataFormater.readStringArray(bis);
						debug("[getPlatformProperties] Raw properties list: " + DebugUtils.convertForDebug(props));
						return convertStringArrayToMap(props);
					} catch (IOException e) {
						info("[getPlatformProperties] Error formatting response data!", e);
						throw new IAgentException("Error formatting response data!", IAgentErrors.ERROR_INTERNAL_ERROR, e);
					} finally {
						DataFormater.closeInputStream(bis);
					}
				} else {
					debug("[listRawArgs] no properties available");
					return new HashMap();
				}
			} else {
				info("[getPlatformProperties] Command failure: " + rspStatus);
				throw new IAgentException("Failed to get platform properties: " + rspStatus, rspStatus);
			}
		} catch (IAgentException e) {
			// try ext managers
			ExtVMManager extMan = (ExtVMManager) connector.getManager(ExtVMManager.class.getName());
			if (extMan != null) {
				return extMan.getPlatformProperties();
			}
			throw e;
		}
	}

	private MBSAConnection getMBSAConnection() throws IAgentException {
		return (MBSAConnection) connector.getConnection(ConnectionManager.MBSA_CONNECTION);
	}
	
	private static Map convertStringArrayToMap(String[] arr) {
		Map result = new HashMap();
		if (arr == null || arr.length == 0)
			// empty properties
			return result;
		for (int i = 0; i < arr.length; i += 2) {
			String key = arr[i];
			String value = null;
			if (i + 1 < arr.length)
				value = arr[i + 1];
			result.put(key, value);
		}
		return result;
	}
	
	public String getSystemProperty(String propertyName) throws IAgentException {
		return (String) Utils.callRemoteMethod(getPMPConnection().getRemoteBundleAdmin(), Utils.GET_SYSTEM_PROPERTY, new Object[] { propertyName });
	}

	public String[] getSystemBundlesNames() throws IAgentException {
		debug("[getSystemBundlesNames] >>>");
		try {
			MBSAConnection connection = getMBSAConnection();
			if (!connection.isConnected()) {
				info("[getSystemBundlesNames] Device is disconnected!");
				throw new IAgentException("Device is disconnected!", IAgentErrors.ERROR_DISCONNECTED);
			}
			MBSAConnectionCallBack tCallBack = connection.sendData(IAgentCommands.IAGENT_CMD_GET_SYSTEM_BUNDLES, null, false);
			int rspStatus = tCallBack.getRspStatus();
			if (rspStatus >= 0) {
				byte rspData[] = tCallBack.getRspData();
				if (rspData != null) {
					ByteArrayInputStream bis = null;
					try {
						bis = new ByteArrayInputStream(rspData);
						String[] names = DataFormater.readStringArray(bis);
						debug("[getSystemBundlesNames] Raw bundle names list: " + DebugUtils.convertForDebug(names));
						return names;
					} catch (IOException e) {
						info("[getSystemBundlesNames] Error formatting response data!", e);
						throw new IAgentException("Error formatting response data!", IAgentErrors.ERROR_INTERNAL_ERROR, e);
					} finally {
						DataFormater.closeInputStream(bis);
					}
				} else {
					debug("[getSystemBundlesNames] no bundles available");
					return new String[0];
				}
			} else {
				debug("[getSystemBundlesNames] Command failure: " + rspStatus);
				// try fallback mechanism
				return fallbackGetSystemBundlesNames();
			}
		} catch(IAgentException e) {
			if (e.getErrorCode() == IAgentErrors.ERROR_CANNOT_CONNECT || e.getErrorCode() == IAgentErrors.ERROR_INTERNAL_ERROR) {
				return fallbackGetSystemBundlesNames();
			} else {
				throw e;
			}
		}
	}
	
	private String[] fallbackGetSystemBundlesNames() throws IAgentException {
		try {
			ExtVMManager extMan = (ExtVMManager) connector.getManager(ExtVMManager.class.getName());
			if (extMan != null) {
				return extMan.getSystemBundlesNames();
			}
		} catch (IAgentException e) {
			// no Ext managers, try another method
		}
		return ((DeploymentManagerImpl) connector.getDeploymentManager()).getSystemBundlesNames();
	}
}
