/*******************************************************************************
 * Copyright (c) 2005, 2010 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.osgimanagement;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

public interface IconProvider {

	/**
	 * Returns icon if available. This method should return null immediately if
	 * icon data is not available and should not attempt to fetch it.
	 * 
	 * @return
	 */
	public Image getIcon();

	/**
	 * Fetches icon data. This method could be long-running. Returns icon data
	 * or null if not available.
	 * 
	 * @return
	 */
	public ImageData fetchIconData();

}
