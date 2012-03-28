/*******************************************************************************
 * Copyright (c) 2005, 2012 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 */
package org.tigris.mtoolkit.osgimanagement;

import java.util.Set;

/**
 * @since 3.0
 */
public interface ISystemBundlesProvider {
	static final String EXTENSION_POINT_NAME = "systemBundlesProvider";

	/**
	 * Returns the list of system bundles identifiers.
	 * 
	 * @return all tree elements
	 */
	Set<String> getSystemBundlesIDs();
}
