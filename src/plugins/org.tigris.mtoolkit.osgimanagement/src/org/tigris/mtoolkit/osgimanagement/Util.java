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

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.model.Framework;

/**
 * @since 5.0
 */
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
	
	public static Framework addFramework(DeviceConnector connector) {
		Hashtable frameWorkMap = new Hashtable();
		FrameworkImpl fws[] = FrameWorkView.getFrameworks();
		if (fws != null) {
			for (int i = 0; i < fws.length; i++) {
				frameWorkMap.put(fws[i].getName(), ""); //$NON-NLS-1$
			}
		}

		int index = 1;
		Dictionary connProps = connector.getProperties();
		Object ip = connProps.get(DeviceConnector.KEY_DEVICE_IP);
		String defaultFWName = Messages.new_framework_default_name+
		" ("+connProps.get(DeviceConnector.TRANSPORT_TYPE)+"="+connProps.get(DeviceConnector.TRANSPORT_ID)+")";
		String frameWorkName = defaultFWName;
		String suffix = " ";
		if (ip != null) { 
			suffix += ip;
		}
		if (frameWorkMap.containsKey(frameWorkName)) {
			do {
				frameWorkName = defaultFWName
								+ suffix
								+ "("
								+ index
								+ ")";
				index++;
			} while (frameWorkMap.containsKey(frameWorkName));
		}
		return addFramework(connector, frameWorkName);
	}
	
	public static Framework addFramework(DeviceConnector connector, String name) {
		FrameworkImpl fw = new FrameworkImpl(name, true);
		FrameWorkView.getTreeRoot().addElement(fw);
		fw.connect(connector, SubMonitor.convert(new NullProgressMonitor()));
		return fw;
	}
	
	

}
