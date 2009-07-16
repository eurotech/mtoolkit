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
package org.tigris.mtoolkit.common.images;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

import org.tigris.mtoolkit.common.UtilitiesPlugin;

public class UIResources {

	protected static final ImageData DEFAULT_IMAGE_DATA = new ImageData(6, 6, 1, new PaletteData(new RGB[] { new RGB(255, 0, 0) }));

	public static final String SMALL_ERROR_ICON = UtilitiesPlugin.PLUGIN_ID + ".small_error_icon";
	public static final String SMALL_WARNING_ICON = UtilitiesPlugin.PLUGIN_ID + ".small_warning_icon";
	public static final String SMALL_INFO_ICON = UtilitiesPlugin.PLUGIN_ID + ".small_info_icon";

	private static final String OBJECT_PATH = "/icons/obj16/";

	// TODO: Change it so the path of the image serves as a key in the image registry
	public static void initializeImageRegistry(ImageRegistry registry) {
		registry.put(SMALL_ERROR_ICON, UtilitiesPlugin.getImageDescriptor(OBJECT_PATH + "error_tsk.gif"));
		registry.put(SMALL_WARNING_ICON, UtilitiesPlugin.getImageDescriptor(OBJECT_PATH + "warn_tsk.gif"));
		registry.put(SMALL_INFO_ICON, UtilitiesPlugin.getImageDescriptor(OBJECT_PATH + "info_tsk.gif"));
	}

	private static ImageRegistry getImageRegistry() {
		UtilitiesPlugin plugin = UtilitiesPlugin.getDefault();
		if (plugin != null) {
			ImageRegistry registry = plugin.getImageRegistry();
			if (registry != null)
				return registry;
		}
		throw new IllegalStateException("Plugin's image registry not available. Most probably it failed to startup properly");
	}

	public static Image getImage(String id) {
		return getImageRegistry().get(id);
	}

	public static ImageDescriptor getImageDescriptor(String id) {
		return getImageRegistry().getDescriptor(id);
	}

}
