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
package org.tigris.mtoolkit.iagent.spi;

import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.pmp.EventListener;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;
import org.tigris.mtoolkit.iagent.rpc.RemoteApplicationAdmin;
import org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin;
import org.tigris.mtoolkit.iagent.rpc.RemoteConsole;
import org.tigris.mtoolkit.iagent.rpc.RemoteDeploymentAdmin;
import org.tigris.mtoolkit.iagent.rpc.RemoteServiceAdmin;

public interface PMPConnection extends AbstractConnection {

	public static final String REMOTE_BUNDLE_ADMIN_NAME = RemoteBundleAdmin.class.getName();
	public static final String REMOTE_APPLICATION_ADMIN_NAME = RemoteApplicationAdmin.class.getName();
	public static final String REMOTE_DEPLOYMENT_ADMIN_NAME = RemoteDeploymentAdmin.class.getName();
	public static final String REMOTE_CONSOLE_NAME = RemoteConsole.class.getName();
	public static final String REMOTE_SERVICE_ADMIN_NAME = RemoteServiceAdmin.class.getName();

	public RemoteObject getRemoteAdmin(String adminClassName) throws IAgentException;

	public RemoteObject getRemoteBundleAdmin() throws IAgentException;

	public RemoteObject getRemoteApplicationAdmin() throws IAgentException;

	public RemoteObject getRemoteDeploymentAdmin() throws IAgentException;

	public RemoteObject getRemoteParserService() throws IAgentException;

	public void releaseRemoteParserService() throws IAgentException;

	public RemoteObject getRemoteServiceAdmin() throws IAgentException;

	public void addEventListener(EventListener listener, String[] eventTypes) throws IAgentException;

	public void removeEventListener(EventListener listener, String[] eventTypes) throws IAgentException;
}
