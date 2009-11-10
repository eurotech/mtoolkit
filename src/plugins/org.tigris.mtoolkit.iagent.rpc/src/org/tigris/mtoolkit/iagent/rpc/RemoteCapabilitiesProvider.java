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
package org.tigris.mtoolkit.iagent.rpc;

import java.util.Map;

public interface RemoteCapabilitiesProvider {

	/**
	 * Returns Map with the current capabilities of the device.
	 * The capabilities can change in time so this method could return different
	 * results in different calls. Base capabilities keys are contained in
	 * {@link Capabilities}
	 * @return Map with device capabilities
	 * @see Capabilities
	 */
	public Map getCapabilities();
}
