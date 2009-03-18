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
 * ICDInterface represents one interface that is provided by the Service element.
 */
public interface ICDInterface extends ICDElement {

	public static final String ATTR_INTERFACE = "interface";

	public String getInterface();

	public void setInterface(String newInterface);

}
