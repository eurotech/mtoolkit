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
 * ICDProperties represents a properties element in one DS component. Such 
 * properties element points to external properties file.
 */
public interface ICDProperties extends ICDBaseProperty {

	public static final String TAG_PROPERTIES = "properties";
	public static final String ATTR_ENTRY = "entry";
	
	public String getEntry();
	public void setEntry(String entry);

}
