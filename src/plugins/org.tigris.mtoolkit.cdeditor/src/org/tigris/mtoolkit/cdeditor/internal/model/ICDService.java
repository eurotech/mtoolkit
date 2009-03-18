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
 * ICDService represents a service element in one DS component. One 
 * component can have only one service element, but the service element can 
 * contain many interfaces.
 */
public interface ICDService extends ICDElement {

	public static final int SERVICE_FACTORY_UNKNOWN = 0;
	public static final int SERVICE_FACTORY_YES = 1;
	public static final int SERVICE_FACTORY_NO = 2;
	public static final int SERVICE_FACTORY_DEFAULT = SERVICE_FACTORY_NO;

	public static final String[] SERVICE_FACTORY_NAMES = new String[] { "true", "false", };
	public static final String ATTR_SERVICEFACTORY = "servicefactory";
	public static final String TAG_SERVICE = "service";
	public static final String TAG_PROVIDE = "provide";

	public int getServiceFactory();

	public String getRawServiceFactory();

	public void setServiceFactory(int serviceFactory);

	public ICDInterface[] getInterfaces();

	public ICDInterface addInterface(String className);

	public void addInterface(ICDInterface aInterface);

	public void removeInterface(int index);

	public void removeInterface(ICDInterface aInterface);

	public void moveUpInterface(ICDInterface aInterface);

	public void moveDownInterface(ICDInterface aInterface);

}
