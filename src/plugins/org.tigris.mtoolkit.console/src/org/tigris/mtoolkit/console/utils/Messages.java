/*******************************************************************************
 * Copyright (c) 2005, 2009 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM - initial API and implementation
 *******************************************************************************/

package org.tigris.mtoolkit.console.utils;

import java.lang.reflect.Field;

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.tigris.mtoolkit.console.utils.ConsoleMessages";//$NON-NLS-1$

	public static String redirect_console_output;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	public static String get(String fieldName) {
		try {
			Field f = Messages.class.getField(fieldName);
			if (f != null)
				return (String) f.get(null);
			else
				return null;
		} catch (IllegalAccessException e) {
			return null;
		} catch (NoSuchFieldException e) {
			return null;
		}
	}
}