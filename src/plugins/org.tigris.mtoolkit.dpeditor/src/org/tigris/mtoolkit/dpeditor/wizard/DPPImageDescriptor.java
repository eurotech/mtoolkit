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
package org.tigris.mtoolkit.dpeditor.wizard;

/**
 * Convenience class for storing references to image descriptors
 * used by the new dpp wizard and export ant and build wizards
 */

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.tigris.mtoolkit.dpeditor.DPActivator;

public class DPPImageDescriptor {

	static final URL BASE_URL = DPActivator.getDefault().getBundle().getEntry("/");

	public static final ImageDescriptor NEW_DPP_IMAGE_WIZARD;
	public static final ImageDescriptor ANT_IMAGE_WIZARD;
	public static final ImageDescriptor BUILD_DP_IMAGE_WIZARD;

	static {
		String iconPath = "icons/";
		NEW_DPP_IMAGE_WIZARD = createImageDescriptor(iconPath + "new_dpp_b.gif");
		ANT_IMAGE_WIZARD = createImageDescriptor(iconPath + "ant_export_b.gif");
		BUILD_DP_IMAGE_WIZARD = createImageDescriptor(iconPath + "dp_export_b.gif");
	}

	/**
	 * Utility method to create an <code>ImageDescriptor</code> from a path to a
	 * file.
	 */
	private static ImageDescriptor createImageDescriptor(String path) {
		try {
			URL url = new URL(BASE_URL, path);
			return ImageDescriptor.createFromURL(url);
		} catch (MalformedURLException e) {
		}
		return ImageDescriptor.getMissingImageDescriptor();
	}
}