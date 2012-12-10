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

import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.VMManager;
import org.tigris.mtoolkit.iagent.instrumentation.Instrument;
import org.tigris.mtoolkit.iagent.internal.tcp.DataFormater;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;
import org.tigris.mtoolkit.iagent.spi.ConnectionEvent;
import org.tigris.mtoolkit.iagent.spi.ConnectionListener;
import org.tigris.mtoolkit.iagent.spi.ConnectionManager;
import org.tigris.mtoolkit.iagent.spi.MBSAConnection;
import org.tigris.mtoolkit.iagent.spi.MBSAConnectionCallBack;
import org.tigris.mtoolkit.iagent.spi.MethodSignature;
import org.tigris.mtoolkit.iagent.spi.PMPConnection;
import org.tigris.mtoolkit.iagent.util.LightServiceRegistry;

/**
 * Implementation of VMManager
 *
 */
public final class VMManagerImpl implements VMManager, ConnectionListener {
  private static final MethodSignature REGISTER_METHOD     = new MethodSignature("registerOutput", new String[] {
                                                             RemoteObject.class.getName()
                                                           }, true);
  private static final MethodSignature EXECUTE_METHOD      = new MethodSignature("executeCommand", new String[] {
                                                             MethodSignature.STRING_TYPE
                                                           }, true);
  private static final MethodSignature GET_FW_START_LEVEL  = new MethodSignature("getFrameworkStartLevel",
                                                               MethodSignature.NO_ARGS, true);
  private static final MethodSignature GET_SYSTEM_PROPERTY = new MethodSignature("getSystemProperty", new String[] {
                                                             MethodSignature.STRING_TYPE
                                                           }, true);

  private DeviceConnectorImpl          connector;

  private OutputStream                 lastRegisteredOutput;

  private LightServiceRegistry         extensionsRegistry;

  /**
   * Creates new runtime commands with specified transport
   *
   * @param aTransport
   */
  public VMManagerImpl(DeviceConnectorImpl connector) {
    if (connector == null) {
      throw new IllegalArgumentException();
    }
    this.connector = connector;
    connector.getConnectionManager().addConnectionListener(this);
  }

  public void connectionChanged(ConnectionEvent event) {
    if (event.getType() == ConnectionEvent.CONNECTED
        && event.getConnection().getType() == ConnectionManager.PMP_CONNECTION) {
      if (lastRegisteredOutput != null) {
        // automatically redirect fw output when reconnected
        try {
          redirectFrameworkOutput(lastRegisteredOutput);
        } catch (IAgentException e) {
          DebugUtils.info(this, "[connectionChanged] Failed to redirect framework output", e);
        }
      }
    }
  }

  public boolean isVMActive() throws IAgentException {
    return isVMConnectable();
  }

  public void executeFrameworkCommand(String command) throws IAgentException {
    DebugUtils.debug(this, "[executeFrameworkCommand] >>> command: " + command);
    EXECUTE_METHOD.call(getPMPConnection().getRemoteParserService(), new Object[] {
      command
    });
  }

  public void redirectFrameworkOutput(OutputStream os) throws IAgentException {
    DebugUtils.debug(this, "[redirectFrameworkOutput] >>> os: " + os);
    PMPConnection connection = getPMPConnection();
    if (os != null) {
      RemoteObject parser = connection.getRemoteParserService();
      REGISTER_METHOD.call(parser, new Object[] {
        os
      });
      lastRegisteredOutput = os;
    } else {
      lastRegisteredOutput = null;
      connection.releaseRemoteParserService();
    }
  }

  public boolean isVMConnectable() throws IAgentException {
    if (!connector.isActive()) {
      DebugUtils.info(this, "[getPMPConnection] DeviceConnector is closed");
      throw new IAgentException("Associated DeviceConnector is closed", IAgentErrors.ERROR_DISCONNECTED);
    }
    try {
      PMPConnection connection = getPMPConnection();
      if (connection == null || !connection.isConnected()) {
        DebugUtils.debug(this, "[isVMConnectable] VM is not connectable");
        return false;
      } else {
        DebugUtils.debug(this, "[isVMConnectable] VM is connectable");
        return true;
      }
    } catch (IAgentException e) {
      DebugUtils.info(this, "[isVMConnectable] VM is not connectable", e);
      return false;
    }
  }

  public int getFrameworkStartLevel() throws IAgentException {
    DebugUtils.debug(this, "[getFrameworkStartLevel] >>> ");
    Integer fwStartLevel = (Integer) GET_FW_START_LEVEL.call(getPMPConnection().getRemoteBundleAdmin());
    return fwStartLevel.intValue();
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

  public String[] listRawArgs() throws IAgentException {
    DebugUtils.debug(this, "[listRawArgs] >>>");
    MBSAConnection connection = getMBSAConnection();
    if (!connection.isConnected()) {
      DebugUtils.info(this, "[listRawArgs] Device is disconnected!");
      throw new IAgentException("Device is disconnected!", IAgentErrors.ERROR_DISCONNECTED);
    }
    MBSAConnectionCallBack tCallBack = connection.sendData(IAgentCommands.IAGENT_CMD_LISTRAWARGS, null);
    int rspStatus = tCallBack.getRspStatus();
    if (rspStatus >= 0) {
      byte rspData[] = tCallBack.getRspData();
      if (rspData != null) {
        ByteArrayInputStream bis = null;
        try {
          bis = new ByteArrayInputStream(rspData);
          String[] args = DataFormater.readStringArray(bis);
          DebugUtils.debug(this, "[listRawArgs] Raw arguments list: " + DebugUtils.convertForDebug(args));
          return args;
        } catch (IOException e) {
          DebugUtils.info(this, "[listRawArgs] Error formatting response data!", e);
          throw new IAgentException("Error formatting response data!", IAgentErrors.ERROR_INTERNAL_ERROR, e);
        } finally {
          DataFormater.closeInputStream(bis);
        }
      } else {
        DebugUtils.debug(this, "[listRawArgs] no arguments available");
        return new String[0];
      }
    } else {
      DebugUtils.info(this, "[listRawArgs] Command failure: " + rspStatus);
      throw new IAgentException("Command failure: " + rspStatus, rspStatus);
    }
  }

  public void addRawArgument(String aRawArgument) throws IAgentException {
    DebugUtils.debug(this, "[addRawArgument] >>> aRawArgument: " + aRawArgument);
    MBSAConnection connection = getMBSAConnection();
    if (!connection.isConnected()) {
      DebugUtils.info(this, "[addRawArgument] Device is disconnected!");
      throw new IAgentException("Device is disconnected!", IAgentErrors.ERROR_DISCONNECTED);
    }
    if (aRawArgument == null) {
      throw new IllegalArgumentException("Argument could not be null!");
    }
    ByteArrayOutputStream bos = null;
    try {
      bos = new ByteArrayOutputStream(256);
      DataFormater.writeString(bos, aRawArgument);
    } catch (IOException e) {
      DebugUtils.info(this, "[addRawArgument] Error processing arguments!", e);
      throw new IAgentException("Error processing arguments!", IAgentErrors.ERROR_INTERNAL_ERROR, e);
    }
    MBSAConnectionCallBack tCallBack = connection.sendData(IAgentCommands.IAGENT_CMD_ADDRAWARGUMENT, bos.toByteArray());
    DataFormater.closeOutputStream(bos);
    int rspStatus = tCallBack.getRspStatus();
    if (rspStatus < 0) {
      DebugUtils.info(this, "[addRawArgument] Command failure: " + rspStatus);
      throw new IAgentException("Command failure: " + rspStatus, rspStatus);
    } else {
      DebugUtils.debug(this, "[addRawArgument] argument addition successful");
    }
  }

  public boolean removeRawArgument(String aRawArgument) throws IAgentException {
    DebugUtils.debug(this, "[removeRawArgument] >>> aRawArgument: " + aRawArgument);
    MBSAConnection connection = getMBSAConnection();
    if (!connection.isConnected()) {
      DebugUtils.info(this, "[removeRawArgument] Device is disconnected!");
      throw new IAgentException("Device is disconnected!", IAgentErrors.ERROR_DISCONNECTED);
    }
    if (aRawArgument == null) {
      throw new IllegalArgumentException("Argument could not be null!");
    }
    ByteArrayOutputStream bos = null;
    try {
      bos = new ByteArrayOutputStream(256);
      DataFormater.writeString(bos, aRawArgument);
    } catch (IOException e) {
      DebugUtils.info(this, "[removeRawArgument] Error processing arguments!", e);
      throw new IAgentException("Error processing arguments!", IAgentErrors.ERROR_INTERNAL_ERROR, e);
    }
    MBSAConnectionCallBack tCallBack = connection.sendData(IAgentCommands.IAGENT_CMD_REMOVERAWARGUMENT,
        bos.toByteArray());
    DataFormater.closeOutputStream(bos);
    int rspStatus = tCallBack.getRspStatus();
    if (rspStatus < 0) {
      if (rspStatus == IAgentErrors.ERR_RUNTIME_ARG_NOT_FOUND) {
        DebugUtils.debug(this, "[removeRawArgument] Runtime argument cannot be found");
        return false;
      }
      DebugUtils.info(this, "[removeRawArgument] Command failure: " + rspStatus);
      throw new IAgentException("Failed to remove VM argument: " + rspStatus, rspStatus);
    } else {
      DebugUtils.debug(this, "[removeRawArgument] argument removal successful");
    }
    return true;
  }

  public void resetArgs() throws IAgentException {
    DebugUtils.debug(this, "[resetArgs] >>>");
    MBSAConnection connection = getMBSAConnection();
    if (!connection.isConnected()) {
      DebugUtils.info(this, "[resetArgs] Device is disconnected!");
      throw new IAgentException("Device is disconnected!", IAgentErrors.ERROR_DISCONNECTED);
    }
    MBSAConnectionCallBack tCallBack = connection.sendData(IAgentCommands.IAGENT_CMD_RESETARGS, null);
    int rspStatus = tCallBack.getRspStatus();
    if (rspStatus < 0) {
      DebugUtils.info(this, "[resetArgs] Command failure: " + rspStatus);
      throw new IAgentException("Failed to reset VM arguments: " + rspStatus, rspStatus);
    }
    DebugUtils.debug(this, "[resetArgs] Arguments successfully reset");
  }

  public void startVM() throws IAgentException {
    DebugUtils.debug(this, "[startVM] >>>");
    MBSAConnection connection = getMBSAConnection();
    if (!connection.isConnected()) {
      DebugUtils.info(this, "[startVM] Device is disconnected!");
      throw new IAgentException("Device is disconnected!", IAgentErrors.ERROR_DISCONNECTED);
    }
    MBSAConnectionCallBack tCallBack = connection.sendData(IAgentCommands.IAGENT_CMD_STARTVM, null);
    int rspStatus = tCallBack.getRspStatus();
    if (rspStatus < 0) {
      DebugUtils.info(this, "[startVM] Command failure: " + rspStatus);
      throw new IAgentException("Failed to start VM: " + rspStatus, rspStatus);
    }
    DebugUtils.debug(this, "[startVM] VM successfully started");
  }

  public void stopVM() throws IAgentException {
    DebugUtils.debug(this, "[stopVM] >>>");
    MBSAConnection connection = getMBSAConnection();
    if (!connection.isConnected()) {
      DebugUtils.info(this, "[stopVM] Device is disconnected!");
      throw new IAgentException("Device is disconnected!", IAgentErrors.ERROR_DISCONNECTED);
    }
    MBSAConnectionCallBack tCallBack = connection.sendData(IAgentCommands.IAGENT_CMD_STOPVM, null);
    int rspStatus = tCallBack.getRspStatus();
    if (rspStatus < 0) {
      DebugUtils.info(this, "[stopVM] Command failure: " + rspStatus);
      throw new IAgentException("Failed to stop VM: " + rspStatus, rspStatus);
    }
    DebugUtils.debug(this, "[stopVM] VM successfully stopped");
  }

  public String getSystemProperty(String propertyName) throws IAgentException {
    return (String) GET_SYSTEM_PROPERTY.call(getPMPConnection().getRemoteBundleAdmin(), new Object[] {
      propertyName
    });
  }

  private LightServiceRegistry getExtensionsRegistry() {
    if (extensionsRegistry == null) {
      extensionsRegistry = new LightServiceRegistry(VMManagerImpl.class.getClassLoader());
    }
    return extensionsRegistry;
  }

  private MBSAConnection getMBSAConnection() throws IAgentException {
    return (MBSAConnection) connector.getConnection(ConnectionManager.MBSA_CONNECTION);
  }

  private PMPConnection getPMPConnection() throws IAgentException {
    return (PMPConnection) connector.getConnection(ConnectionManager.PMP_CONNECTION);
  }
}
