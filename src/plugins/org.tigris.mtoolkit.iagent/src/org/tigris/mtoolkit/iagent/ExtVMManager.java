/*******************************************************************************
 * Copyright (c) 2005, 2010 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.iagent;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Extenders which provide additional VM Management shall implement this
 * interface
 * 
 * @since 4.0
 */
public interface ExtVMManager {

	public Map getPlatformProperties() throws IAgentException;

	/**
	 * @since 4.1
	 */
	public List/* <String> */listConfigurables(Map properties) throws IAgentException;

	/**
	 * @since 4.1
	 */
	public InputStream readConfigurable(String name, Map properties) throws IAgentException;

	/**
	 * @since 4.1
	 */
	public void writeConfigurable(String name, InputStream is, Map properties) throws IAgentException;

	/**
	 * @since 4.1
	 */
	public boolean isAddonEnabled(String name, Map properties) throws IAgentException;

	/**
	 * @since 4.1
	 */
	public void setAddonEnabled(String name, boolean enabled, Map properties) throws IAgentException;

}
