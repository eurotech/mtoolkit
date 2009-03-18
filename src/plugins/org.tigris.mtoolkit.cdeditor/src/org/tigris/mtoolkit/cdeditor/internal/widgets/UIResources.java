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
package org.tigris.mtoolkit.cdeditor.internal.widgets;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.tigris.mtoolkit.cdeditor.internal.CDEditorPlugin;


public class UIResources {

	protected static final ImageData DEFAULT_IMAGE_DATA = new ImageData(6, 6, 1, new PaletteData(new RGB[] { new RGB(255, 0, 0) }));

	public static final String SMALL_ERROR_ICON = CDEditorPlugin.PLUGIN_ID + ".small_error_icon";
	public static final String SMALL_WARNING_ICON = CDEditorPlugin.PLUGIN_ID + ".small_warning_icon";
	public static final String SMALL_INFO_ICON = CDEditorPlugin.PLUGIN_ID + ".small_info_icon";
	public static final String BLANK_ICON = CDEditorPlugin.PLUGIN_ID + ".blank_icon";

	public static final String OVERLAY_ERROR_ICON = CDEditorPlugin.PLUGIN_ID + ".overlay_error_icon";
	public static final String OVERLAY_WARNING_ICON = CDEditorPlugin.PLUGIN_ID + ".overlay_warning_icon";
	public static final String OVERLAY_OPTIONAL_ICON = CDEditorPlugin.PLUGIN_ID + ".overlay_optional_icon";

	public static final String ARRAY_PROPERTY_ICON = CDEditorPlugin.PLUGIN_ID + ".array_property_icon";
	public static final String SINGLE_PROPERTY_ICON = CDEditorPlugin.PLUGIN_ID + ".single_property_icon";
	public static final String PROPERTIES_ICON = CDEditorPlugin.PLUGIN_ID + ".properties_icon";
	public static final String COMPONENT_ICON = CDEditorPlugin.PLUGIN_ID + ".component_icon";
	public static final String REFERENCE_ICON = CDEditorPlugin.PLUGIN_ID + ".reference_icon";
	public static final String REFERENCES_ICON = CDEditorPlugin.PLUGIN_ID + ".references_icon";

	public static final String DESCRIPTION_ICON = CDEditorPlugin.PLUGIN_ID + ".description_icon";
	public static final String NEW_DESCRIPTION_ICON = CDEditorPlugin.PLUGIN_ID + ".new_description_icon";

	public static final String NEW_DESCRIPTION_BANNER = CDEditorPlugin.PLUGIN_ID + ".new_description_wizban";

	private static final String OBJECT_PATH = "/icons/obj16/";
	private static final String OVERLAY_PATH = "/icons/ovr16/";
	private static final String WIZBAN_PATH = "/icons/wizban/";

	// TODO: Change it so the path of the image serves as a key in the image registry
	public static void initializeImageRegistry(ImageRegistry registry) {
		registry.put(SMALL_ERROR_ICON, CDEditorPlugin.getImageDescriptor(OBJECT_PATH + "error_tsk.gif"));
		registry.put(SMALL_WARNING_ICON, CDEditorPlugin.getImageDescriptor(OBJECT_PATH + "warn_tsk.gif"));
		registry.put(SMALL_INFO_ICON, CDEditorPlugin.getImageDescriptor(OBJECT_PATH + "info_tsk.gif"));
		registry.put(BLANK_ICON, CDEditorPlugin.getImageDescriptor(OBJECT_PATH + "blank.gif"));

		registry.put(OVERLAY_ERROR_ICON, CDEditorPlugin.getImageDescriptor(OVERLAY_PATH + "error_co.gif"));
		registry.put(OVERLAY_WARNING_ICON, CDEditorPlugin.getImageDescriptor(OVERLAY_PATH + "warning_co.gif"));
		registry.put(OVERLAY_OPTIONAL_ICON, CDEditorPlugin.getImageDescriptor(OVERLAY_PATH + "optional_co.gif"));

		registry.put(ARRAY_PROPERTY_ICON, CDEditorPlugin.getImageDescriptor(OBJECT_PATH + "arrprop_obj.gif"));
		registry.put(SINGLE_PROPERTY_ICON, CDEditorPlugin.getImageDescriptor(OBJECT_PATH + "prop_obj.gif"));
		registry.put(PROPERTIES_ICON, CDEditorPlugin.getImageDescriptor(OBJECT_PATH + "props_obj.gif"));
		registry.put(COMPONENT_ICON, CDEditorPlugin.getImageDescriptor(OBJECT_PATH + "component_obj.gif"));
		registry.put(REFERENCE_ICON, CDEditorPlugin.getImageDescriptor(OBJECT_PATH + "ref_obj.gif"));
		registry.put(REFERENCES_ICON, CDEditorPlugin.getImageDescriptor(OBJECT_PATH + "refs_obj.gif"));

		registry.put(DESCRIPTION_ICON, CDEditorPlugin.getImageDescriptor(OBJECT_PATH + "cd_nav.gif"));
		registry.put(NEW_DESCRIPTION_ICON, CDEditorPlugin.getImageDescriptor(OBJECT_PATH + "newcd_wiz.gif"));
		registry.put(NEW_DESCRIPTION_BANNER, CDEditorPlugin.getImageDescriptor(WIZBAN_PATH + "newcd_wiz.png"));
	}

	private static ImageRegistry getImageRegistry() {
		CDEditorPlugin plugin = CDEditorPlugin.getDefault();
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
