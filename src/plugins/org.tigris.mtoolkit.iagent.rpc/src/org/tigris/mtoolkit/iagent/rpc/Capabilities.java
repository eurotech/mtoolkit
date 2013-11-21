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

/**
 * Contains constants that represent different device capabilities.
 */
public final class Capabilities {

	private Capabilities() {
	}

	/**
	 * Support for remote applications
	 */
	public static final String APPLICATION_SUPPORT = "remote.application.support"; //$NON-NLS-1$

	/**
	 * Support for remote deployment
	 */
	public static final String DEPLOYMENT_SUPPORT = "remote.deployment.support"; //$NON-NLS-1$

	/**
	 * Support for remote console
	 */
	public static final String CONSOLE_SUPPORT = "remote.console.support"; //$NON-NLS-1$
}
