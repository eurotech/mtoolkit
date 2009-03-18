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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.tigris.mtoolkit.cdeditor.internal.CDEditorPlugin;


public class StyleManager {

	public static final String TYPE_XML_TAG_NAME = "xml_tag_name";
	public static final String TYPE_XML_TAG_ATTR_NAME = "xml_tag_attr_name";
	public static final String TYPE_XML_TAG_ATTR_VALUE = "xml_tag_attr_value";
	public static final String TYPE_XML_PROC_INSTR = "xml_proc_instr";
	public static final String TYPE_XML_COMMENT = "xml_comment";
	public static final String TYPE_DEFAULT = "default_type";

	private static RGB COLOR_XML_TAG_NAME = new RGB(0, 0, 192);
	private static RGB COLOR_XML_TAG_ATTR_NAME = new RGB(0, 0, 0);
	private static RGB COLOR_XML_TAG_ATTR_VALUE = new RGB(0, 192, 0);
	private static RGB COLOR_XML_PROC_INSTR = new RGB(128, 128, 128);
	private static RGB COLOR_XML_COMMENT = new RGB(96, 96, 96);
	private static RGB COLOR_DEFAULT = new RGB(0, 0, 0);

	private int STYLE_XML_TAG_NAME = SWT.NORMAL;
	private int STYLE_XML_TAG_ATTR_NAME = SWT.NORMAL;
	private int STYLE_XML_TAG_ATTR_VALUE = SWT.NORMAL;
	private int STYLE_XML_PROC_INSTR = SWT.NORMAL;
	private int STYLE_XML_COMMENT = SWT.ITALIC;
	private int STYLE_DEFAULT = SWT.NORMAL;

	private Map attributes = new HashMap();

	private boolean initialized;

	private synchronized void initialize() {
		if (initialized)
			return;
		createStyle(TYPE_XML_COMMENT, STYLE_XML_COMMENT);
		createStyle(TYPE_XML_PROC_INSTR, STYLE_XML_PROC_INSTR);
		createStyle(TYPE_XML_TAG_ATTR_NAME, STYLE_XML_TAG_ATTR_NAME);
		createStyle(TYPE_XML_TAG_ATTR_VALUE, STYLE_XML_TAG_ATTR_VALUE);
		createStyle(TYPE_XML_TAG_NAME, STYLE_XML_TAG_NAME);
		createStyle(TYPE_DEFAULT, STYLE_DEFAULT);
		
		initialized = true;
	}
	
	private void createStyle(String type, int style) {
		attributes.put(type, new TextAttribute(getColor(type), null, style));
	}

	public TextAttribute getStyle(String type) {
		initialize();
		TextAttribute style = (TextAttribute) attributes.get(type);
		return style != null ? style : (TextAttribute) attributes.get(TYPE_DEFAULT);
	}
	
	private static ColorRegistry getColorRegistry() {
		CDEditorPlugin plugin = CDEditorPlugin.getDefault();
		if (plugin != null) {
			ColorRegistry registry = plugin.getColorRegistry();
			if (registry != null)
				return registry;
		}
		throw new IllegalStateException("Plugin's color registry not available. Most probably, plugin failed to startup properly.");
	}
	
	private static Color getColor(String type) {
		return getColorRegistry().get(type);
	}
	
	public static void initializeColorRegistry(ColorRegistry registry) { 
		registry.put(TYPE_XML_COMMENT, COLOR_XML_COMMENT);
		registry.put(TYPE_XML_PROC_INSTR, COLOR_XML_PROC_INSTR);
		registry.put(TYPE_XML_TAG_ATTR_NAME, COLOR_XML_TAG_ATTR_NAME);
		registry.put(TYPE_XML_TAG_ATTR_VALUE, COLOR_XML_TAG_ATTR_VALUE);
		registry.put(TYPE_XML_TAG_NAME, COLOR_XML_TAG_NAME);
		registry.put(TYPE_DEFAULT, COLOR_DEFAULT);
	}
}
