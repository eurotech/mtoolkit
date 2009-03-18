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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.core.text.DocumentAttributeNode;
import org.eclipse.pde.internal.core.text.DocumentHandler;
import org.eclipse.pde.internal.core.text.DocumentTextNode;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class PlainDocumentHandler extends DocumentHandler {

	private static class Namespace {
		public String prefix;
		public String namespaceURI;
		int level;

		public String toString() {
			return "Namespace[" + prefix + "=" + namespaceURI + "][" + level + "]";
		}
	}

	private IDocument document;

	private PlainDocumentElementNode root = new PlainDocumentElementNode();

	private List namespaces = new ArrayList();

	private int currentLevel = 0;

	public PlainDocumentHandler(IDocument document, boolean reconciling) {
		super(reconciling);
		this.document = document;
	}

	public PlainDocumentHandler(boolean reconciling) {
		super(reconciling);
	}

	protected IDocument getDocument() {
		return document;
	}

	protected IDocumentElementNode doGetDocumentNode(String name, IDocumentElementNode parent) {
		IDocumentElementNode node = null;
		if (parent == null) {
			node = root;
		} else {
			node = findExistingChildNode(name, parent);
		}
		if (node == null) {
			node = new PlainDocumentElementNode();
		}
		return node;
	}

	public IDocumentElementNode getRoot() {
		return root;
	}

	public void startDocument() throws SAXException {
		super.startDocument();
		namespaces.clear();
		currentLevel = 0;
	}

	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
		for (int i = 0; i < attributes.getLength(); i++) {
			String attrQname = attributes.getQName(i);
			if (attrQname.startsWith("xmlns:")) {
				String prefix = attrQname.substring("xmlns:".length(), attrQname.length());
				String namespace = attributes.getValue(i);
				pushNamespace(prefix, namespace, currentLevel);
			} else if ("xmlns".equals(attrQname)) {
				String namespace = attributes.getValue(i);
				pushNamespace("", namespace, currentLevel);
			}
		}
		super.startElement(uri, localName, name, attributes);
		currentLevel++;
	}

	public void endElement(String uri, String localName, String name) throws SAXException {
		currentLevel--;
		super.endElement(uri, localName, name);
		removeNamespaces(currentLevel);
	}

	protected void pushNamespace(String prefix, String namespace, int level) {
		Namespace ns = new Namespace();
		ns.prefix = prefix;
		ns.namespaceURI = namespace;
		ns.level = level;
		namespaces.add(ns);
	}
	
	protected Map getNamespaces(int level) {
		Map namespacesMap = new HashMap();
		for (int i = namespaces.size() - 1; i >= 0; i--) {
			Namespace ns = (Namespace) namespaces.get(i);
			if (ns.level == level)
				namespacesMap.put(ns.prefix, ns.namespaceURI);
		}
		return namespacesMap;
	}

	protected void removeNamespaces(int level) {
		for (int i = namespaces.size() - 1; i >= 0; i--) {
			Namespace ns = (Namespace) namespaces.get(i);
			if (ns.level < level)
				break;
			namespaces.remove(i);
		}
	}

	protected String getNamespace(String prefix) {
		for (int i = namespaces.size() - 1; i >= 0; i--) {
			Namespace ns = (Namespace) namespaces.get(i);
			if (ns.prefix.equals(prefix)) {
				return ns.namespaceURI;
			}
		}
		return "";
	}

	protected IDocumentAttributeNode getDocumentAttribute(String name, String value, IDocumentElementNode parent) {
		IDocumentAttributeNode attr = parent.getDocumentAttribute(name);
		try {
			if (attr == null) {
				attr = new DocumentAttributeNode();
				attr.setAttributeName(name);
				attr.setAttributeValue(value);
				attr.setEnclosingElement(parent);
			} else {
				if (!name.equals(attr.getAttributeName()))
					attr.setAttributeName(name);
				if (!value.equals(attr.getAttributeValue()))
					attr.setAttributeValue(value);
			}
		} catch (CoreException e) {
		}
		return attr;
	}

	protected IDocumentTextNode getDocumentTextNode(String content, IDocumentElementNode parent) {
		IDocumentTextNode textNode = parent.getTextNode();
		if (textNode == null) {
			if (content.trim().length() > 0) {
				textNode = new DocumentTextNode();
				textNode.setEnclosingElement(parent);
				parent.addTextNode(textNode);
				textNode.setText(content.trim());
			}
		} else {
			String newContent = textNode.getText() + content;
			textNode.setText(newContent);
		}
		return textNode;
	}

	protected IDocumentElementNode getDocumentNode(String name, IDocumentElementNode parent) {

		IDocumentElementNode node = doGetDocumentNode(name, parent);

		node.setXMLTagName(name);
		node.setOffset(-1);
		node.setLength(-1);

		IDocumentAttributeNode[] attrs = node.getNodeAttributes();
		for (int i = 0; i < attrs.length; i++) {
			attrs[i].setNameOffset(-1);
			attrs[i].setNameLength(-1);
			attrs[i].setValueOffset(-1);
			attrs[i].setValueLength(-1);
		}

		for (int i = 0; i < node.getChildNodes().length; i++) {
			IDocumentElementNode child = node.getChildAt(i);
			child.setOffset(-1);
			child.setLength(-1);
		}
		
		if (node instanceof PlainDocumentElementNode)
			((PlainDocumentElementNode) node).setNamespaces(getNamespaces(currentLevel));

		// clear text nodes if the user is typing on the source page
		// they will be recreated in the characters() method
		if (isReconciling()) {
			node.removeTextNode();
			node.setIsErrorNode(false);
		}

		return node;
	}

	protected IDocumentElementNode findExistingChildNode(String name, IDocumentElementNode parent) {
		IDocumentElementNode node = null;
		IDocumentElementNode[] children = parent.getChildNodes();
		for (int i = 0; i < children.length; i++) {
			if (children[i].getOffset() < 0) {
				if (name.equals(children[i].getXMLTagName())) {
					node = children[i];
				}
				break;
			}
		}
		return node;
	}

}
