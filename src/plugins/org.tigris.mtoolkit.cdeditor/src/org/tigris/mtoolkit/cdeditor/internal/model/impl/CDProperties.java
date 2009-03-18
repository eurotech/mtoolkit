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

import org.tigris.mtoolkit.cdeditor.internal.model.ICDProperties;

public class CDProperties extends CDElement implements ICDProperties {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 428884342761874928L;

	public CDProperties() {
		setXMLTagName(TAG_PROPERTIES);
	}
	
	public String getEntry() {
		return getXMLAttributeValue(ATTR_ENTRY);
	}

	public void setEntry(String entry) {
		if (!safeCompare(getEntry(), entry)) {
			String oldValue = getEntry();
			setXMLAttribute(ATTR_ENTRY, entry);
			fireDocumentModified(this, ATTR_ENTRY, oldValue, entry);
			fireElementChanged(this, ATTR_ENTRY, oldValue, entry);
		}
	}

	public boolean isLeafNode() {
		return true;
	}

	public String toString() {
		return "Ps[entry=" + getEntry() + "]";
	}
}
