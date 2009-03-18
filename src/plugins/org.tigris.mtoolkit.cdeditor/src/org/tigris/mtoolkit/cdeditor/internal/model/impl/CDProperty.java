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

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.pde.internal.core.text.DocumentTextNode;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.tigris.mtoolkit.cdeditor.internal.CDEditorPlugin;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDProperty;
import org.tigris.mtoolkit.cdeditor.internal.text.DocumentChangedEvent;


public class CDProperty extends CDElement implements ICDProperty {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7746573920723080297L;

	private static final String[] EMPTY_VALUES = new String[0];

	public CDProperty() {
		setXMLTagName(TAG_PROPERTY);
	}

	public String getName() {
		return getXMLAttributeValue(ATTR_NAME);
	}

	public int getType() {
		return ModelUtil.parseEnumerateValue(getRawType(), TYPE_NAMES, TYPE_DEFAULT, TYPE_UNKNOWN);
	}

	public String getValue() {
		return getXMLAttributeValue(ATTR_VALUE);
	}

	public String[] getValues() {
		return parseMutliValue(getRawValues());
	}

	private String[] parseMutliValue(String text) {
		if (text == null || text.trim().length() == 0)
			return EMPTY_VALUES;
		StringTokenizer textTokenizer = new StringTokenizer(text, "\n"); //$NON-NLS-1$
		List lines = new LinkedList();
		while (textTokenizer.hasMoreElements()) {
			String valueLine = ((String) textTokenizer.nextElement()).trim();
			if (valueLine != null && valueLine.length() != 0) {
				lines.add(valueLine);
			}
		}
		String values[] = (String[]) lines.toArray(new String[lines.size()]);
		return values;
	}

	private String convertMultiValue(String[] values) {
		StringBuffer buf = new StringBuffer();
		if (values.length > 0) {
			buf.append(values[0]);
			for (int i = 1; i < values.length; i++) {
				buf.append('\n');
				buf.append(values[i]);
			}
		}
		return buf.toString();
	}

	public boolean isMultiValue() {
		String valueAttr = getValue();
		return valueAttr == null || valueAttr.length() == 0;
	}

	public void setName(String name) {
		if (!safeCompare(getName(), name)) {
			String oldValue = getXMLAttributeValue(ATTR_NAME);
			setXMLAttribute(ATTR_NAME, name);
			firePropertyModified();
			fireElementChanged(this, ATTR_NAME, oldValue, name);
		}
	}

	/**
	 * Fires reinsert event for this property, causing the whole property tag to
	 * be rewritten properly formatted. This approach is used, because the PDE
	 * framework for XML editing doesn't support collapsing an expanded XML tag.
	 * Adding such support will be time consuming and will be deferred.
	 */
	private void firePropertyModified() {
		fireDocumentModified(DocumentChangedEvent.INSERT, this);
	}

	public void setType(int type) {
		if (type == TYPE_UNKNOWN)
			return;
		if (getType() != type) {
			String newValue;
			if (type == TYPE_DEFAULT)
				newValue = null;
			else
				newValue = TYPE_NAMES[type - 1];
			setRawType(newValue);
		}
	}

	private String setRawType(String newValue) {
		String oldValue = getXMLAttributeValue(ATTR_TYPE);
		setXMLAttribute(ATTR_TYPE, newValue);
		firePropertyModified();
		fireElementChanged(this, ATTR_TYPE, oldValue, newValue);
		return oldValue;
	}

	public void setValue(String value) {
		if (!safeCompare(getValue(), value)) {
			if (getTextNode() != null) {
				IDocumentTextNode node = getTextNode();
				node.setText("");
			}
			String oldValue = getXMLAttributeValue(ATTR_VALUE);
			setXMLAttribute(ATTR_VALUE, value);
			firePropertyModified();
			fireElementChanged(this, ATTR_VALUE, oldValue, value);
		}
	}

	public boolean isLeafNode() {
		return true; // no child nodes expected to be contained
	}

	public void setValues(String[] values) {
		if (!compareArray(getValues(), values)) {
			String[] oldValues = getValues();
			if (!isMultiValue()) { // if single value, remove value attribute
				setXMLAttribute(ATTR_VALUE, null);
			}
			IDocumentTextNode textNode = getTextNode();
			if (textNode == null) {
				textNode = new DocumentTextNode();
				textNode.setEnclosingElement(this);
				addTextNode(textNode);
			}
			String newValue = convertMultiValue(values);
			textNode.setText(newValue);
			firePropertyModified();
			fireElementChanged(this, "values", oldValues, values);
		}
	}

	public String getRawType() {
		return getXMLAttributeValue(ATTR_TYPE);
	}

	public String getRawValues() {
		return (getTextNode() != null ? getTextNode().getText() : null);
	}

	protected void executeChangeOperation(String attribute, Object value) {
		if (ATTR_TYPE.equals(attribute)) {
			debug("executeChangeOperation: change type to " + value);
			setRawType((String) value);
		} else if (ATTR_VALUE.equals(attribute)) {
			debug("executeChangeOperation: change value to " + value);
			setValue((String) value);
		} else if (ATTR_NAME.equals(attribute)) {
			debug("executeChangeOperation: change name to " + value);
			setName((String) value);
		} else if ("values".equals(attribute)) {
			debug("executeChangeOperation: change multi-value to {" + convertMultiValue((String[]) value).replace('\n', ',') + "}");
			setValues((String[]) value);
		} else {
			super.executeChangeOperation(attribute, value);
		}
	}

	public String toString() {
		return "P[name=" + getName() + ",type=" + getRawType() + ",value=" + getValue() + ",values={" + getRawValues() + "}]";
	}

	private static final void debug(String message) {
		if (CDEditorPlugin.DEBUG)
			CDEditorPlugin.debug("[CDProperty] ".concat(message));
	}

}
