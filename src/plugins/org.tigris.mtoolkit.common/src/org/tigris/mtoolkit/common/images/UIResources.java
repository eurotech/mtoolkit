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

	/**
	 * @since 5.0
	 */
	public static final String BLANK_ICON = UtilitiesPlugin.PLUGIN_ID + ".blank_icon";
	public static final String SMALL_ERROR_ICON = UtilitiesPlugin.PLUGIN_ID + ".small_error_icon";
	public static final String SMALL_WARNING_ICON = UtilitiesPlugin.PLUGIN_ID + ".small_warning_icon";
	public static final String SMALL_INFO_ICON = UtilitiesPlugin.PLUGIN_ID + ".small_info_icon";
	/**
	 * @since 6.0
	 */
	public static final String CERTIFICATE_ICON = UtilitiesPlugin.PLUGIN_ID + ".certificate_icon";
	/**
	 * @since 6.0
	 */
	public static final String OVR_ERROR_ICON = UtilitiesPlugin.PLUGIN_ID + ".ovr_error_icon";
	public static final String SSL_INTERACTION_WIZBAN_ICON = UtilitiesPlugin.PLUGIN_ID + "ssl_interaction_wizban_icon";
  public static final String WORKSPACE_FILE_ICON = UtilitiesPlugin.PLUGIN_ID + ".ws_file_icon";
  public static final String PLUGIN_ICON = UtilitiesPlugin.PLUGIN_ID + ".plugin_icon";

	private static final String OBJECT_PATH = "/icons/obj16/";
	private static final String OVERLAY_PATH = "/icons/ovr16/";
	private static final String WIZBAN_PATH = "/icons/wizban/";

	// TODO: Change it so the path of the image serves as a key in the image registry
	public static void initializeImageRegistry(ImageRegistry registry) {
		registry.put(BLANK_ICON, UtilitiesPlugin.getImageDescriptor(OBJECT_PATH + "blank.gif"));
		registry.put(SMALL_ERROR_ICON, UtilitiesPlugin.getImageDescriptor(OBJECT_PATH + "error_tsk.gif"));
		registry.put(SMALL_WARNING_ICON, UtilitiesPlugin.getImageDescriptor(OBJECT_PATH + "warn_tsk.gif"));
		registry.put(SMALL_INFO_ICON, UtilitiesPlugin.getImageDescriptor(OBJECT_PATH + "info_tsk.gif"));
		registry.put(CERTIFICATE_ICON, UtilitiesPlugin.getImageDescriptor(OBJECT_PATH + "signed_yes_tbl.gif"));
		registry.put(OVR_ERROR_ICON, UtilitiesPlugin.getImageDescriptor(OVERLAY_PATH + "error_co.gif"));
		registry.put(SSL_INTERACTION_WIZBAN_ICON, UtilitiesPlugin.getImageDescriptor(WIZBAN_PATH + "ssl.dialog.wiz.warning.gif"));
    registry.put(WORKSPACE_FILE_ICON, UtilitiesPlugin.getImageDescriptor(OBJECT_PATH + "ws_file.gif"));
    registry.put(PLUGIN_ICON, UtilitiesPlugin.getImageDescriptor(OBJECT_PATH + "plugin.gif"));
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
