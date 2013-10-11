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

public interface RemoteApplicationListener extends EventListener {
	/**
	 * Sent when remote application is changed in some way (installed, started,
	 * stopped or uninstalled).
	 *
	 * @param event
	 *            an event object containing details
	 */
	void applicationChanged(RemoteApplicationEvent event);
}
