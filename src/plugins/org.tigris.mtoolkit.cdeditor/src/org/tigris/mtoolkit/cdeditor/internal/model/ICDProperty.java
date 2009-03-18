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
package org.tigris.mtoolkit.cdeditor.internal.model;


/**
 * ICDProperty represents a property element in one DS component. Such 
 * element is single property, unlike properties element.
 */
public interface ICDProperty extends ICDBaseProperty {
	
	public static final int TYPE_UNKNOWN   = 0;
	public static final int TYPE_STRING    = 1;
	public static final int TYPE_LONG      = 2;
	public static final int TYPE_DOUBLE    = 3;
	public static final int TYPE_FLOAT     = 4;
	public static final int TYPE_INTEGER   = 5;
	public static final int TYPE_BYTE      = 6;
	public static final int TYPE_CHAR      = 7;
	public static final int TYPE_BOOLEAN   = 8;
	public static final int TYPE_SHORT     = 9;
	public static final int TYPE_DEFAULT   = TYPE_STRING;
	
	public static final String[] TYPE_NAMES = {
		"String",
		"Long",
		"Double",
		"Float",
		"Integer",
		"Byte",
		"Character",
		"Boolean",
		"Short",
	};
	
	public static final String TAG_PROPERTY = "property";
	public static final String ATTR_TYPE = "type";
	public static final String ATTR_NAME = "name";
	public static final String ATTR_VALUE = "value";

	public String getName();
	public void setName(String name);

	public boolean isMultiValue();

	public String getValue();
	public void setValue(String value);

	public String[] getValues();
	public String getRawValues();
	public void setValues(String[] values);

	public int getType();
	public String getRawType();
	public void setType(int type);
}
