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
 * ICDComponent represents a Declarative services component.
 */
public interface ICDComponent extends ICDElement {

	public static final int ENABLED_UNKNOWN = 0;
	public static final int ENABLED_YES = 1;
	public static final int ENABLED_NO = 2;
	public static final int ENABLED_DEFAULT = ENABLED_YES;

	public static final String[] ENABLED_NAMES = new String[] { "true", "false", };

	public static final int IMMEDIATE_UNKNOWN = 0;
	public static final int IMMEDIATE_YES = 1;
	public static final int IMMEDIATE_NO = 2;
	public static final int IMMEDIATE_DEFAULT = IMMEDIATE_NO;

	public static final String[] IMMEDIATE_NAMES = new String[] { "true", "false" };

	public static final String ATTR_IMMEDIATE = "immediate";
	public static final String ATTR_NAME = "name";
	public static final String TAG_IMPLEMENTATION = "implementation";
	public static final String ATTR_CLASS = "class";
	public static final String ATTR_FACTORY = "factory";
	public static final String ATTR_ENABLED = "enabled";
	public static final String TAG_COMPONENT = "component";

	public int getEnabled();

	public String getRawEnabled();

	public void setEnabled(int enabled);

	public String getName();

	public void setName(String name);

	public String getFactory();

	public void setFactory(String factory);

	public int getImmediate();

	public String getRawImmediate();

	public void setImmediate(int immediate);

	public String getImplementationClass();

	public void setImplementationClass(String implementationClass);

	public ICDService getService();

	public ICDService createService();

	public ICDService createService(int serviceFactory);
	
	public ICDService createService(int serviceFactory, ICDInterface aInterface);

	public void removeService();

	public ICDReference[] getReferences();

	public void addReference(ICDReference reference);

	public void removeReference(int index);

	public void removeReference(ICDReference reference);

	public void moveUpReference(ICDReference reference);

	public void moveDownReference(ICDReference reference);

	public ICDBaseProperty[] getProperties();

	public void addProperty(ICDBaseProperty property);

	public void removeProperty(int index);

	public void removeProperty(ICDBaseProperty property);

	public void moveUpProperty(ICDBaseProperty prop);

	public void moveDownProperty(ICDBaseProperty prop);

}
