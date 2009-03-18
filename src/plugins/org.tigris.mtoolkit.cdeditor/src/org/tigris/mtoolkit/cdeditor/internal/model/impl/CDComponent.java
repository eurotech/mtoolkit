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
import java.util.Iterator;
import java.util.List;

import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.tigris.mtoolkit.cdeditor.internal.CDEditorPlugin;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDBaseProperty;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDComponent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDInterface;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDReference;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDService;


public class CDComponent extends CDElement implements ICDComponent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6035872900797960665L;
	private List properties = new ArrayList();
	private List references = new ArrayList();

	private boolean single = false;

	public CDComponent() {
		setXMLTagName(TAG_COMPONENT);
	}

	public int getEnabled() {
		return ModelUtil.parseEnumerateValue(getRawEnabled(), ENABLED_NAMES, ENABLED_DEFAULT, ENABLED_UNKNOWN);
	}

	public String getFactory() {
		return getXMLAttributeValue(ATTR_FACTORY);
	}

	public int getImmediate() {
		return ModelUtil.parseEnumerateValue(getRawImmediate(), IMMEDIATE_NAMES, IMMEDIATE_DEFAULT, IMMEDIATE_UNKNOWN);
	}

	public String getImplementationClass() {
		IDocumentElementNode node = findImplementationNode();
		if (node != null)
			return node.getXMLAttributeValue(ATTR_CLASS);
		return "";
	}

	private IDocumentElementNode findImplementationNode() {
		List childs = getChildNodesList();
		for (Iterator it = childs.iterator(); it.hasNext();) {
			IDocumentElementNode node = (IDocumentElementNode) it.next();
			if (node.getXMLTagName().equals(TAG_IMPLEMENTATION) || node.getXMLTagName().endsWith(":" + TAG_IMPLEMENTATION)) {
				return node;
			}
		}
		return null;
	}

	public String getName() {
		return getXMLAttributeValue(ATTR_NAME);
	}

	public ICDBaseProperty[] getProperties() {
		return (ICDBaseProperty[]) properties.toArray(new ICDBaseProperty[properties.size()]);
	}

	public ICDReference[] getReferences() {
		return (ICDReference[]) references.toArray(new ICDReference[references.size()]);
	}

	public ICDService getService() {
		return findServiceNode();
	}

	private CDService findServiceNode() {
		List childs = getChildNodesList();
		for (Iterator it = childs.iterator(); it.hasNext();) {
			IDocumentElementNode node = (IDocumentElementNode) it.next();
			if (node instanceof CDService)
				return (CDService) node;
		}
		return null;
	}

	public void setEnabled(int enabled) {
		if (enabled == ENABLED_UNKNOWN)
			return;
		if (getEnabled() != enabled) {
			String oldValue = getXMLAttributeValue(ATTR_ENABLED);
			String newValue;
			if (enabled == ENABLED_DEFAULT)
				newValue = null;
			else
				newValue = ENABLED_NAMES[enabled - 1];
			setXMLAttribute(ATTR_ENABLED, newValue);
			fireDocumentModified(this, ATTR_ENABLED, oldValue, newValue);
			fireElementChanged(this, ATTR_ENABLED, oldValue, newValue);
		}
	}

	public void setFactory(String factory) {
		if (!safeCompare(getFactory(), factory)) {
			String oldValue = getFactory();
			setXMLAttribute(ATTR_FACTORY, factory);
			fireDocumentModified(this, ATTR_FACTORY, oldValue, factory);
			fireElementChanged(this, ATTR_FACTORY, oldValue, factory);
		}
	}

	public void setImmediate(int immediate) {
		if (immediate == IMMEDIATE_UNKNOWN)
			return;
		if (getImmediate() != immediate) {
			String oldValue = getXMLAttributeValue(ATTR_IMMEDIATE);
			String newValue;
			if (immediate == IMMEDIATE_DEFAULT)
				newValue = null;
			else
				newValue = IMMEDIATE_NAMES[immediate - 1];
			setXMLAttribute(ATTR_IMMEDIATE, newValue);
			fireDocumentModified(this, ATTR_IMMEDIATE, oldValue, newValue);
			fireElementChanged(this, ATTR_IMMEDIATE, oldValue, newValue);
		}
	}

	public void setImplementationClass(String implementationClass) {
		if (!safeCompare(getImplementationClass(), implementationClass)) {
			String oldValue = getImplementationClass();
			IDocumentElementNode node = findImplementationNode();
			if (node == null) {
				node = new CDElement();
				node.setXMLTagName(TAG_IMPLEMENTATION);
				addChildNode(node);
			}
			node.setXMLAttribute(ATTR_CLASS, implementationClass);
			fireDocumentModified(node, ATTR_CLASS, oldValue, implementationClass);
			fireElementChanged(this, ATTR_CLASS, oldValue, implementationClass);
		}
	}

	public void setName(String name) {
		if (!safeCompare(getName(), name)) {
			String oldValue = getName();
			setXMLAttribute(ATTR_NAME, name);
			fireDocumentModified(this, ATTR_NAME, oldValue, name);
			fireElementChanged(this, ATTR_NAME, oldValue, name);
		}
	}

	public void addProperty(ICDBaseProperty property) {
		addChildNode((CDElement) property);
	}

	public void addReference(ICDReference reference) {
		addChildNode((CDElement) reference);
	}

	public void removeProperty(int index) {
		CDElement property = (CDElement) properties.remove(index);
		removeChildNode(property);
	}

	public void removeReference(int index) {
		CDElement reference = (CDElement) references.get(index);
		removeChildNode(reference);
	}

	public void moveUpReference(ICDReference reference) {
		int itemToMoveIndex = references.indexOf(reference);
		if (itemToMoveIndex == -1 || itemToMoveIndex == 0)
			return;
		else
			swapElements(references, itemToMoveIndex, itemToMoveIndex - 1);
	}

	public void moveDownReference(ICDReference reference) {
		int itemToMoveIndex = references.indexOf(reference);
		if (itemToMoveIndex == -1 || itemToMoveIndex == references.size() - 1)
			return;
		else
			swapElements(references, itemToMoveIndex, itemToMoveIndex + 1);
	}

	public void removeProperty(ICDBaseProperty property) {
		removeChildNode((CDElement) property);
	}

	public void removeReference(ICDReference reference) {
		removeChildNode((CDElement) reference);
	}

	public void moveDownProperty(ICDBaseProperty prop) {
		int itemToMoveIndex = properties.indexOf(prop);
		if (itemToMoveIndex == -1 || itemToMoveIndex == properties.size() - 1)
			return;
		else
			swapElements(properties, itemToMoveIndex, itemToMoveIndex + 1);
	}

	public void moveUpProperty(ICDBaseProperty prop) {
		int itemToMoveIndex = properties.indexOf(prop);
		if (itemToMoveIndex == -1 || itemToMoveIndex == 0)
			return;
		else
			swapElements(properties, itemToMoveIndex, itemToMoveIndex - 1);
	}

	protected boolean addModelObject(CDElement child) {
		if (child instanceof CDReference) {
			if (!references.contains(child)) {
				int pos = ModelUtil.getModelInsertIndex(references, CDReference.class, child);
				references.add(pos, child);
				fireElementAdded(child, this);
				return true;
			} else {
				return false;
			}
		} else if (child instanceof CDProperty || child instanceof CDProperties) {
			if (!properties.contains(child)) {
				int pos1 = ModelUtil.getModelInsertIndex(properties, CDProperty.class, child);
				int pos2 = ModelUtil.getModelInsertIndex(properties, CDProperties.class, child);
				properties.add(Math.max(pos1, pos2), child);
				fireElementAdded(child, this);
				return true;
			} else {
				return false;
			}
		} else if (child instanceof CDService) {
			fireElementAdded(child, this);
			return true;
		}
		return super.addModelObject(child);
	}

	protected boolean removeModelObject(CDElement child) {
		if (child instanceof CDReference) {
			if (references.remove(child)) {
				fireElementRemoved(child, this);
				return true;
			}
		} else if (child instanceof CDProperty || child instanceof CDProperties) {
			if (properties.remove(child)) {
				fireElementRemoved(child, this);
				return true;
			}
		} else if (child instanceof CDService) {
			fireElementRemoved(child, this);
			return true;
		}
		return false;
	}

	protected void swapModelObjects(CDElement child1, CDElement child2) {
		if (child1 instanceof CDReference && child2 instanceof CDReference) {
			int idx1 = references.indexOf(child1);
			int idx2 = references.indexOf(child2);
			if (idx1 == -1 || idx2 == -1)
				return;
			references.set(idx1, child2);
			references.set(idx2, child1);
			fireElementSwapped(this, child1, child2);
		} else if (child1 instanceof ICDBaseProperty && child2 instanceof ICDBaseProperty) {
			int idx1 = properties.indexOf(child1);
			int idx2 = properties.indexOf(child2);
			if (idx1 == -1 || idx2 == -1)
				return;
			properties.set(idx2, child1);
			properties.set(idx1, child2);
			fireElementSwapped(this, child1, child2);
		} else {
			super.swapModelObjects(child1, child2);
		}
	}

	public String getRawEnabled() {
		return getXMLAttributeValue(ATTR_ENABLED);
	}

	public String getRawImmediate() {
		return getXMLAttributeValue(ATTR_IMMEDIATE);
	}

	public ICDService createService(int serviceFactory, ICDInterface aInterface) {
		if (getService() == null) {
			CDService serviceNode = new CDService();
			serviceNode.setServiceFactory(serviceFactory);
			serviceNode.addInterface(aInterface);
			addChildNode(serviceNode);
			return serviceNode;
		}
		return getService();
	}

	public ICDService createService(int serviceFactory) {
		if (getService() == null) {
			CDService serviceNode = new CDService();
			serviceNode.setServiceFactory(serviceFactory);
			addChildNode(serviceNode);
			return serviceNode;
		}
		return getService();
	}

	public ICDService createService() {
		return createService(ICDService.SERVICE_FACTORY_DEFAULT);
	}

	public void removeService() {
		for (int i = getChildCount() - 1; i >= 0; i--) {
			IDocumentElementNode node = getChildAt(i);
			if (node instanceof CDService)
				removeChildNode(node);
		}
	}

	public void setSingle(boolean single) {
		this.single = single;
	}

	public boolean isRoot() {
		return single;
	}

	protected void executeChangeOperation(String attribute, Object value) {
		if (ATTR_CLASS.equals(attribute)) {
			CDEditorPlugin.debug("[CDComponent] executeChangeOperation: setImplementationClass(" + value + ") on " + this);
			setImplementationClass((String) value);
		} else {
			super.executeChangeOperation(attribute, value);
		}
	}

	public String toString() {
		return "C[name=" + getName() + ",impl=" + getImplementationClass() + ",factory=" + getFactory() + ",enabled=" + getRawEnabled() + ",immediate=" + getRawImmediate() + ",single=" + single + "]";
	}

}
