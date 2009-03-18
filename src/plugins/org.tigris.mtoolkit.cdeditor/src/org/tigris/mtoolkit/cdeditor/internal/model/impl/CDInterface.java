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
package org.tigris.mtoolkit.cdeditor.internal.model.impl;

import org.tigris.mtoolkit.cdeditor.internal.model.ICDInterface;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDService;


public class CDInterface extends CDElement implements ICDInterface {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1509838324497497136L;

	public CDInterface() {
		setXMLTagName(ICDService.TAG_PROVIDE);
	}
	
	public String getInterface() {
		return getXMLAttributeValue(ATTR_INTERFACE);
	}
	
	public void setInterface(String interfaceName) {
		if (!safeCompare(getInterface(), interfaceName)) {
			String oldValue = getInterface();
			setXMLAttribute(ATTR_INTERFACE, interfaceName);
			fireDocumentModified(this, ATTR_INTERFACE, oldValue, interfaceName);
			fireElementChanged(this, ATTR_INTERFACE, oldValue, interfaceName);
		}
	}
	
	public boolean isLeafNode() {
		return true;
	}

	public String toString() {
		return "I[interface=" + getInterface() + "]";
	}
}
