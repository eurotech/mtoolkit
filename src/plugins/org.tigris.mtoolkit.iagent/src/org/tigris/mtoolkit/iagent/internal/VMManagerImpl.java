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
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;
import org.tigris.mtoolkit.iagent.spi.ConnectionEvent;
import org.tigris.mtoolkit.iagent.spi.ConnectionListener;
import org.tigris.mtoolkit.iagent.spi.ConnectionManager;
import org.tigris.mtoolkit.iagent.spi.MethodSignature;
import org.tigris.mtoolkit.iagent.spi.PMPConnection;

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
  private static final MethodSignature SET_SYSTEM_PROPERTY = new MethodSignature("setSystemProperty", new String[] {
      MethodSignature.STRING_TYPE, MethodSignature.STRING_TYPE
                                                           }, true);

  private DeviceConnectorImpl          connector;
  private OutputStream                 lastRegisteredOutput;

  /**
   * Creates new runtime commands with specified transport
   *
   * @param aTransport
   */
  VMManagerImpl(DeviceConnectorImpl connector) {
    if (connector == null) {
      throw new IllegalArgumentException();
    }
    this.connector = connector;
    connector.getConnectionManager().addConnectionListener(this);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.ConnectionListener#connectionChanged(org.tigris.mtoolkit.iagent.spi.ConnectionEvent)
   */
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

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.VMManager#isVMActive()
   */
  public boolean isVMActive() throws IAgentException {
    return isVMConnectable();
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.VMManager#executeFrameworkCommand(java.lang.String)
   */
  public void executeFrameworkCommand(String command) throws IAgentException {
    DebugUtils.debug(this, "[executeFrameworkCommand] >>> command: " + command);
    EXECUTE_METHOD.call(getPMPConnection().getRemoteParserService(), new Object[] {
      command
    });
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.VMManager#redirectFrameworkOutput(java.io.OutputStream)
   */
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

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.VMManager#isVMConnectable()
   */
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

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.VMManager#getFrameworkStartLevel()
   */
  public int getFrameworkStartLevel() throws IAgentException {
    DebugUtils.debug(this, "[getFrameworkStartLevel] >>> ");
    Integer fwStartLevel = (Integer) GET_FW_START_LEVEL.call(getPMPConnection().getRemoteBundleAdmin());
    return fwStartLevel.intValue();
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.VMManager#getSystemProperty(java.lang.String)
   */
  public String getSystemProperty(String propertyName) throws IAgentException {
    return (String) GET_SYSTEM_PROPERTY.call(getPMPConnection().getRemoteBundleAdmin(), new Object[] {
      propertyName
    });
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.VMManager#setSystemProperty(java.lang.String, java.lang.String)
   */
  public void setSystemProperty(String propertyName, String propertyValue) throws IAgentException {
    SET_SYSTEM_PROPERTY.call(getPMPConnection().getRemoteBundleAdmin(), new Object[] {
        propertyName, propertyValue
    });
  }

  private PMPConnection getPMPConnection() throws IAgentException {
    return (PMPConnection) connector.getConnection(ConnectionManager.PMP_CONNECTION);
  }
}
