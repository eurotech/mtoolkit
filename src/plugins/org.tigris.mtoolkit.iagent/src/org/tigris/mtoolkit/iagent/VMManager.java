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
import java.util.Map;

/**
 * Provides control over the virtual machine. Every method of this class will
 * throw exception right after calling it, if the {@link DeviceConnector} from,
 * which this object is retrieved is disconnected from the remote OSGi
 * framework.
 * 
 */
public interface VMManager {

	public boolean isVMActive() throws IAgentException;

	/**
	 * Redirects the framework output (stdout and stderr) to the passed
	 * OutputStream. The method shouldn't be called more than 1 time, without
	 * restoring the normal operation.
	 * 
	 * The normal operation is restored in one of the following cases:<br>
	 * <ul>
	 * <li>The connection to the remote OSGi runtime is closed, either normally
	 * or because of an error</li>
	 * <li>The method is called with a null argument</li>
	 * </ul>
	 * <br>
	 * 
	 * @param os
	 *            OutputStream object to which the framework should write or
	 *            null to restore the normal operation
	 * @throws IAgentException
	 */
	public void redirectFrameworkOutput(OutputStream os) throws IAgentException;

	/**
	 * Pass a string to be executed by the framework as if it was entered via
	 * the standard input
	 * 
	 * @param command
	 *            String to be parsed and executed
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
	 * Returns rows of the startup VM script created by the instrumentation
	 * agent.
	 * 
	 * @return array of raws to the startup VM script created by the
	 *         instrumentation agent
	 * @throws IAgentException
	 *             thrown if some error occurs during transport of the command
	 *             to the device, or during its execution
	 */
	public String[] listRawArgs() throws IAgentException;

	/**
	 * Add raw argument in the VM startup script created by the instrumentation
	 * agent. Through this method can be added only set of allowed arguments.
	 * 
	 * This method generally could be used instead of all other customized
	 * arguments set-up methods this class provides. You should be cautious
	 * using this method and other methods for argument set-up in the same time.
	 * 
	 * @throws IAgentException
	 *             thrown if some error occurs during transport of the command
	 *             to the device, or during its execution
	 */
	public void addRawArgument(String aRawArgument) throws IAgentException;

	/**
	 * Remove raw argument from the VM startup script created by the
	 * instrumentation agent.
	 * 
	 * You should be cautious using this method, because you could remove
	 * arguments which are added not only from
	 * {@link VMManager#addRawArgument(String)} but also from other customized
	 * arguments set-up methods.
	 * 
	 * @returns true in case the argument existed and was removed, false if the
	 *          argument wasn't found in the current arguments list
	 * @throws IAgentException
	 *             thrown if some error occurs during transport of the command
	 *             to the device, or during its execution
	 */
	public boolean removeRawArgument(String aRawArgument) throws IAgentException;

	/**
	 * Removes all settings done trough this class
	 * 
	 * @throws IAgentException
	 *             thrown if some error occurs during transport of the command
	 *             to the device, or during its execution
	 */
	public void resetArgs() throws IAgentException;

	/**
	 * Starts the VM on the connected target device
	 * 
	 * @throws IAgentException
	 *             thrown if some error occurs during transport of the command
	 *             to the device, or during its execution
	 */
	public void startVM() throws IAgentException;

	/**
	 * Stops the VM on the connected target device
	 * 
	 * @throws IAgentException
	 *             thrown if some error occurs during transport of the command
	 *             to the device, or during its execution
	 */
	public void stopVM() throws IAgentException;

	/**
	 * Returns whether the Instrumentation Agent device part is available on the
	 * connected device.<br>
	 * If this method returns false, the clients of the API can decice to call
	 * {@link #instrumentVM()} in order to prepare the connected device for
	 * further operations.
	 * 
	 * @param refresh
	 *            True indicates that the method should not rely on the cached
	 *            state and inquery the device directly. False indicates that
	 *            the method can return the cached state.
	 * @return whether the remote device is properly instrumented
	 *         (Instrumentation Agent device part is correctly installed and
	 *         running)
	 * @throws IAgentException
	 * @see {@link #instrumentVM()}
	 */
	public boolean isVMInstrumented(boolean refresh) throws IAgentException;

	/**
	 * Returns whether the framework can be connected via PMP connection.
	 * 
	 * @return true if pmp connection is available, false otherwise
	 * @throws IAgentException
	 */
	public boolean isVMConnectable() throws IAgentException;

	/**
	 * Instruments the remote device (installs/updates/activates the
	 * Instrumentation Agent device part)
	 * 
	 * @throws IAgentException
	 */
	public void instrumentVM() throws IAgentException;

	/**
	 * Returns value of system property argument on remote device VM specified
	 * by the method argument.
	 * 
	 * @param propertyName
	 *            - the name of the property
	 * @return - the value that is set on remote device for the property
	 * @throws IAgentException
	 */
	public String getSystemProperty(String propertyName) throws IAgentException;

	/**
	 * 
	 * @return
	 * @throws IAgentException
	 */
	public Map getPlatformProperties() throws IAgentException;

	/**
	 * Returns string array with names of all System bundles.
	 * 
	 * @return A string array with symbolic names of all system bundles.
	 * 
	 * @throws IAgentException
	 */
	public String[] getSystemBundlesNames() throws IAgentException;
}
