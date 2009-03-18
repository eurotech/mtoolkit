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
import java.util.Iterator;
import java.util.Map;

import org.eclipse.pde.internal.core.text.DocumentElementNode;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.tigris.mtoolkit.cdeditor.internal.model.impl.ModelUtil;


public class PlainDocumentElementNode extends DocumentElementNode {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4784931204861986023L;

	private Map fNamespaces = new HashMap();

	public void setNamespaces(Map namespaces) {
		fNamespaces.clear();
		if (namespaces != null)
			fNamespaces.putAll(namespaces);
	}

	public String getNamespace(String prefix) {
		if (prefix == null)
			prefix = "";
		String namespace = (String) fNamespaces.get(prefix);
		if (namespace != null)
			return namespace;
		if (getParentNode() instanceof PlainDocumentElementNode)
			return ((PlainDocumentElementNode) getParentNode()).getNamespace(prefix);
		return ""; // no namespace found for this prefix, return ""
	}

	public String getNamespace() {
		String elementName = getXMLTagName();
		String prefix = ModelUtil.getElementPrefix(elementName);
		return getNamespace(prefix);
	}

	public String getPrefix(String namespace) {
		if (namespace == null)
			return null;
		if (fNamespaces.containsValue(namespace)) {
			for (Iterator it = fNamespaces.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				if (namespace.equals(entry.getValue())) {
					return (String) entry.getKey();
				}
			}
		} else if (getParentNode() instanceof PlainDocumentElementNode) {
			return ((PlainDocumentElementNode) getParentNode()).getPrefix(namespace);
		}
		return null;
	}

	public IDocumentElementNode removeChildNode(IDocumentElementNode child) {
		IDocumentElementNode node = super.removeChildNode(child);
		if (node != null)
			node.setParentNode(null);
		return node;
	}

	public IDocumentElementNode removeChildNode(int index) {
		IDocumentElementNode node = super.removeChildNode(index);
		// remove the parent node, super call cannot return null
		node.setParentNode(null);	
		return node;
	}
	
	
}
