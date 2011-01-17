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
package org.tigris.mtoolkit.console.utils;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.tigris.mtoolkit.iagent.IAgentException;

/**
 * @since 5.0
 */
public class Util {

	public static IStatus handleIAgentException(IAgentException e) {
		return Util.newStatus(IStatus.ERROR, getErrorMessage(e), e.getCauseException());
	}

	public static IStatus newStatus(String message, IStatus e) {
		return new MultiStatus(OSGiConsolePlugin.PLUGIN_ID, 0, new IStatus[] { e }, message, null);
	}

	public static IStatus newStatus(int severity, String message, Throwable t) {
		return new Status(severity, OSGiConsolePlugin.PLUGIN_ID, message, t);
	}

	private static String getErrorMessage(IAgentException e) {
		String msg = e.getMessage();
		if (msg == null) {
			msg = "Operation failed.";
			Throwable cause = e.getCauseException();
			if (cause != null && cause.getMessage() != null) {
				msg += " " + cause.getMessage(); //$NON-NLS-1$
			}
		}
		return msg;
	}
}
