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
package org.tigris.mtoolkit.osgimanagement;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.model.Framework;

public class Util {

	public static IStatus handleIAgentException(IAgentException e) {
		return Util.newStatus(IStatus.ERROR, null, e);
	}

	public static IStatus newStatus(String message, IStatus e) {
		return new MultiStatus(FrameworkPlugin.PLUGIN_ID, 0, new IStatus[] { e }, message, null);
	}

	public static IStatus newStatus(int severity, String message, Throwable t) {
		return new Status(severity, FrameworkPlugin.PLUGIN_ID, message, t);
	}
	
	public static Framework addFramework(DeviceConnector connector, String name) {
		try {
			FrameworkImpl fw = new FrameworkImpl(name, true);
			FrameWorkView.getTreeRoot().addElement(fw);
			fw.connect(connector, null);
			return fw;
		} finally {
			throw new RuntimeException("Not yet implemented");
		}
	}

}
