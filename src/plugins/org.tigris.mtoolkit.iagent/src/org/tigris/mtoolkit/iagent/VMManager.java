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
package org.tigris.mtoolkit.iagent;

import java.io.OutputStream;

/**
 * Provides control over the virtual machine. Every method of this class will
 * throw exception right after calling it, if the {@link DeviceConnector} from,
 * which this object is retrieved is disconnected from the remote OSGi
 * framework.
 *
 */
public interface VMManager {
  /**
   * Redirects the framework output (stdout and stderr) to the passed
   * OutputStream. The method shouldn't be called more than 1 time, without
   * restoring the normal operation.
   *
   * The normal operation is restored in one of the following cases:<br>
   * <ul>
   * <li>The connection to the remote OSGi runtime is closed, either normally or
   * because of an error</li>
   * <li>The method is called with a null argument</li>
   * </ul>
   * <br>
   *
   * @param os
   *          OutputStream object to which the framework should write or null to
   *          restore the normal operation
   * @throws IAgentException
   */
  public void redirectFrameworkOutput(OutputStream os) throws IAgentException;

  /**
   * Pass a string to be executed by the framework as if it was entered via the
   * standard input
   *
   * @param command
   *          String to be parsed and executed
   * @throws IAgentException
   */
  public void executeFrameworkCommand(String command) throws IAgentException;

  /**
   * Returns the start level of the remote OSGi framework
   *
   * @return the start level of the remote framework
   * @throws IAgentException
   */
  public int getFrameworkStartLevel() throws IAgentException;
  /**
   * Returns whether the framework can be connected via PMP connection.
   *
   * @return true if pmp connection is available, false otherwise
   * @throws IAgentException
   */
  public boolean isVMConnectable() throws IAgentException;

  /**
   * Returns whether the framework can be connected via PMP connection.
   *
   * @return true if pmp connection is available, false otherwise
   * @throws IAgentException
   */
  public boolean isVMActive() throws IAgentException;

  /**
   * Returns value of system property argument on remote device VM specified by
   * the method argument.
   *
   * @param propertyName
   *          - the name of the property
   * @return - the value that is set on remote device for the property
   * @throws IAgentException
   */
  public String getSystemProperty(String propertyName) throws IAgentException;

  /**
   * Sets a system property on the remote device VM specified by the method
   * argument.
   *
   * @param propertyName
   *          - the name of the property
   * @param propertyValue
   *          - the new value of the property
   * @throws IAgentException
   */
  public void setSystemProperty(String propertyName, String propertyValue) throws IAgentException;
}
