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
package org.tigris.mtoolkit.common.security;

/**
 * This interface is intended to be implemented by clients, wanting to execute operations in the
 * context of replaced default SSLSocketFactory and HostnameVerifier for HTTPS URL connections.
 * 
 * @see InteractiveConnectionHandler
 * 
 */
public interface Operation {

	public Object execute() throws Exception;

}
