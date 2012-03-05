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
package org.tigris.mtoolkit.common;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;

/**
 * @since 5.0
 */
public class CMultiStatus extends MultiStatus {

	public CMultiStatus(String pluginId, int code, Throwable exception) {
		super(pluginId, code, "", exception);
	}
	
	public void add(IStatus status) {
		if (status.getSeverity() > getSeverity()) {
			setMessage(status.getMessage());
		}
		super.add(status);
	}

}
