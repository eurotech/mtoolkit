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
package org.tigris.mtoolkit.iagent.instrumentation;

import org.tigris.mtoolkit.iagent.spi.DeviceConnectorSpi;

public interface Instrument {

	public boolean isVMInstrumented(DeviceConnectorSpi connectorSpi);

	public boolean instrumentVM(DeviceConnectorSpi connectorSpi);

}
