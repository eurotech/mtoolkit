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

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.tigris.mtoolkit.console.utils.ConsoleMessages";//$NON-NLS-1$

	public static String redirect_console_output;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
}