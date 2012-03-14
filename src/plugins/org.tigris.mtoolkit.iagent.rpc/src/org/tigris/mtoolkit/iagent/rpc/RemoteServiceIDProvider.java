/*******************************************************************************
 * Copyright (c) 2005, 2012 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.iagent.rpc;

public interface RemoteServiceIDProvider {
	long SERVICE_UNAVAILABLE = -1;

	/**
	 * Returns service id for remote management service This id is used for
	 * synchronizing local and remote management services.
	 * 
	 * @return OSGI service ID or -1 if it is unavailable
	 * 
	 */
	public long getRemoteServiceID();
}
