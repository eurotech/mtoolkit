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
package org.tigris.mtoolkit.iagent.event;

import java.util.EventListener;

import org.tigris.mtoolkit.iagent.DeploymentManager;

/**
 * Clients interested in remote bundle events must implement this interface. The
 * listeners must be added to the listeners list via
 * {@link DeploymentManager#addRemoteBundleListener(RemoteBundleListener)}
 * method.
 *
 * @author Danail Nachev
 *
 */
public interface RemoteBundleListener extends EventListener {

	/**
	 * Sent when remote bundle is changed in some way (installed, updated,
	 * uninstalled, etc.).
	 *
	 * @param event
	 *            an event object containing details
	 */
	void bundleChanged(RemoteBundleEvent event);

}
