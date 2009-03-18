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
package org.tigris.mtoolkit.cdeditor.internal.text;

public class StringHelper {

	public static String buildCommaSeparatedList(String[] strings) {
		if (strings == null || strings.length == 0)
			return "";
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < strings.length; i++) {
			if (strings[i] != null && strings[i].length() > 0) {
				if (buf.length() > 0)
					buf.append(", ");
				buf.append(strings[i]);
			}
		}
		return buf.toString();
	}
}
