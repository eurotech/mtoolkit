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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDComponent;


public class ModelUtil {

	public static int parseEnumerateValue(String value, String[] validValues, int defaultValue, int invalidValue) {
		if (value == null || value.trim().length() == 0)
			return defaultValue;
		for (int i = 0; i < validValues.length; i++) {
			String enumValue = validValues[i];
			if (enumValue.equalsIgnoreCase(value)) {
				return i + 1;
			}
		}
		return invalidValue;
	}

	public static String printEnumerateValue(int value, String[] validValues) {
		if (value == 0)
			return "(not valid)";
		return validValues[value - 1];
	}

	/**
	 * Walks down the document element hierarchy and returns all found
	 * components. These components will be added to the model, if the model is
	 * available as root document element.
	 * 
	 * @param child
	 * @param components
	 *            the list, where the found elements will be added. Null can be
	 *            passed indicating that the list should be created before
	 *            proceeding
	 * @return a list with components found
	 */
	public static List findComponentsDown(IDocumentElementNode child, List components) {
		if (components == null)
			components = new ArrayList();
		if (child instanceof ICDComponent)
			components.add(child);
		IDocumentElementNode[] children = child.getChildNodes();
		for (int i = 0; children != null && i < children.length; i++) {
			findComponentsDown(children[i], components);
		}
		return components;
	}

	public static String getElementLocalName(String name) {
		if (name.indexOf(':') > -1)
			return name.substring(name.indexOf(':') + 1, name.length());
		else
			return name;
	}

	public static String getElementPrefix(String name) {
		if (name.indexOf(':') > -1)
			return name.substring(0, name.indexOf(':'));
		else
			return "";
	}

	public static IDocumentElementNode[] findChildNode(IDocumentElementNode parent, String name) {
		IDocumentElementNode[] children = parent.getChildNodes();
		List foundNodes = new ArrayList();
		for (int i = 0; i < children.length; i++) {
			IDocumentElementNode child = children[i];
			if (name.equals(getElementLocalName(child.getXMLTagName()))) {
				foundNodes.add(child);
			}
		}
		return (IDocumentElementNode[]) foundNodes.toArray(new IDocumentElementNode[foundNodes.size()]);
	}

	public static int getModelInsertIndex(List elements, Class elementClass, IDocumentElementNode node) {
		int pos = -1;
		IDocumentElementNode prev = getPrevElementAtAllLevels(elementClass, node);
		if (prev != null) {
			pos = elements.indexOf(prev);
		}
		return pos + 1;
	}

	private static IDocumentElementNode getPrevElementAtAllLevels(Class elementClass, IDocumentElementNode node) {
		// searching node siblings
		IDocumentElementNode prev = node.getPreviousSibling();
		while (prev != null) {
			if (elementClass.isInstance(prev)) {
				return prev;
			}
			IDocumentElementNode found = getPrevElementDown(elementClass, prev);
			if (found != null) {
				return found;
			}
			prev = prev.getPreviousSibling();
		}
		// searching node parent
		IDocumentElementNode parent = node.getParentNode();
		if (parent != null) {
			if (elementClass.isInstance(parent)) {
				return parent;
			}
			IDocumentElementNode found = getPrevElementAtAllLevels(elementClass, parent);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	private static IDocumentElementNode getPrevElementDown(Class elementClass, IDocumentElementNode node) {
		IDocumentElementNode[] children = node.getChildNodes();
		for (int i = children.length - 1; i >= 0; i--) {
			if (elementClass.isInstance(children[i])) {
				return children[i];
			}
			IDocumentElementNode found = getPrevElementDown(elementClass, children[i]);
			if (found != null) {
				return found;
			}
		}
		return null;
	}
}
