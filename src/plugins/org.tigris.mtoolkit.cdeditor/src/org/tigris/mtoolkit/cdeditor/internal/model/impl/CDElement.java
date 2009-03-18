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

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.tigris.mtoolkit.cdeditor.internal.CDEditorPlugin;
import org.tigris.mtoolkit.cdeditor.internal.model.CDModelEvent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDComponent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDElement;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDModel;
import org.tigris.mtoolkit.cdeditor.internal.text.DocumentChangedEvent;
import org.tigris.mtoolkit.cdeditor.internal.text.PlainDocumentElementNode;

/**
 * Implementations of DS elements should subclass this class in order to have 
 * support for common change operations and event distribution.
 */
public class CDElement extends PlainDocumentElementNode implements ICDElement {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5451352463609590956L;

	protected CDElement parent;

	protected boolean suppressDocumentEvents;

	protected static final String ATTR_EMPTY_VALUE = "";

	private int removeIndex = -1;

	public ICDElement getParent() {
		return parent;
	}

	public void addChildNode(IDocumentElementNode child, int position) {
		super.addChildNode(child, position);
		if (child instanceof CDElement) {
			CDElement element = (CDElement) child;
			element.setParent(this);
			addModelObject(element);
		}
		fireDocumentModified(DocumentChangedEvent.INSERT, child);
	}

	public void addChildNode(IDocumentElementNode child) {
		if (child instanceof CDElement && ((CDElement) child).getRemoveIndex() != -1) {
			addChildNode(child, ((CDElement) child).getRemoveIndex());
		} else {
			addChildNode(child, getChildCount());
		}
	}

	public IDocumentElementNode removeChildNode(IDocumentElementNode child) {
		int index = -1;
		if (child != null && child.getParentNode() != null) {
			index = child.getParentNode().indexOf(child);
		}
		child = super.removeChildNode(child);
		if (child == null)
			return null;
		if (child instanceof CDElement) {
			removeModelObject((CDElement) child);
			((CDElement) child).setParent(null);
			((CDElement) child).setRemoveIndex(index);
		}
		fireDocumentModified(DocumentChangedEvent.REMOVE, child);
		return child;
	}

	public IDocumentElementNode removeChildNode(int index) {
		return removeChildNode(getChildAt(index));
	}

	public void swap(IDocumentElementNode child1, IDocumentElementNode child2) {
		if (child1.getParentNode() != child2.getParentNode())
			throw new IllegalArgumentException("Cannot swap model objects with different parents");
		if (child1 instanceof CDElement && child2 instanceof CDElement) {
			swapModelObjects((CDElement) child1, (CDElement) child2);
		}
		super.swap(child1, child2);
		fireDocumentModified(this, "non_existent_attr", child1, child2);
	}

	protected boolean addModelObject(CDElement child) {
		if (child instanceof CDComponent) {
			if (findComponentUp() != null)
				throw new IllegalArgumentException("Cannot add component inside other component description. (" + child + ")");
			if (getModel() != null)
				return getModel().registerComponent((ICDComponent) child);
			return true;
		}
		checkSubclass(child);
		if (getModel() != null) {
			List components = ModelUtil.findComponentsDown(child, null);
			if (components.size() > 0) {
				boolean result = false;
				for (Iterator it = components.iterator(); it.hasNext();) {
					CDComponent component = (CDComponent) it.next();
					result |= getModel().registerComponent(component);
				}
				return result;
			}
		}
		return true;
	}

	private void checkSubclass(CDElement child) {
		if (!child.getClass().equals(CDElement.class))
			throw new IllegalArgumentException("This: " + this.getClass() + "; Child: " + child.getClass());
	}

	private CDComponent findComponentUp() {
		CDElement element = this;
		while (element != null && !(element instanceof CDComponent))
			element = (CDElement) element.getParent();
		if (element instanceof CDComponent)
			return (CDComponent) element;
		return null;
	}

	protected boolean removeModelObject(CDElement child) {
		if (child instanceof CDComponent) {
			if (getModel() != null)
				return getModel().unregisterComponent((ICDComponent) child);
			return true;
		}
		checkSubclass(child);
		if (getModel() != null) {
			List components = ModelUtil.findComponentsDown(child, null);
			if (components.size() > 0) {
				boolean result = false;
				for (Iterator it = components.iterator(); it.hasNext();) {
					CDComponent component = (CDComponent) it.next();
					getModel().unregisterComponent(component);
				}
				return result;
			}
		}
		return true;
	}

	protected void swapModelObjects(CDElement child1, CDElement child2) {
		if (child1 instanceof CDComponent && child2 instanceof CDComponent) {
			// find the model and delegate the swapping to it, uses internal API
			CDModel model = (CDModel) child1.getModel();
			model.swapModelObjects(child1, child2);
		}
	}

	public void setParent(CDElement element) {
		this.parent = element;
	}

	public void fireDocumentModified(DocumentChangedEvent e) {
		if (suppressDocumentEvents)
			return;
		if (getParent() != null)
			((CDElement) getParent()).fireDocumentModified(e);
	}

	public void fireModified(CDModelEvent event) {
		if (getParent() != null)
			getParent().fireModified(event);
	}

	public void fireElementAdded(Object element, Object newParent) {
		Assert.isNotNull(element);
		fireModified(new CDModelEvent(CDModelEvent.ADDED, element, newParent));
	}

	public void fireElementRemoved(Object element, Object oldParent) {
		Assert.isNotNull(element);
		fireModified(new CDModelEvent(CDModelEvent.REMOVED, element, oldParent));
	}

	public void fireElementChanged(Object element, String attribute, Object oldValue, Object newValue) {
		Assert.isNotNull(element);
		Assert.isNotNull(attribute);
		fireModified(new CDModelEvent(element, attribute, oldValue, newValue));
	}

	public void fireElementSwapped(Object parentElement, Object child1, Object child2) {
		Assert.isNotNull(parentElement);
		Assert.isNotNull(child1);
		Assert.isNotNull(child2);
		fireModified(new CDModelEvent(parentElement, child1, child2));
	}

	protected void fireDocumentModified(IDocumentElementNode node, String attribute, Object oldValue, Object newValue) {
		DocumentChangedEvent e = new DocumentChangedEvent(node, attribute, oldValue, newValue);
		fireDocumentModified(e);
	}

	protected void fireDocumentModified(IDocumentTextNode node, String oldValue, String newValue) {
		DocumentChangedEvent e = new DocumentChangedEvent(node, "text_node", oldValue, newValue);
		fireDocumentModified(e);
	}

	protected void fireDocumentModified(int type, IDocumentElementNode[] objects) {
		DocumentChangedEvent e = new DocumentChangedEvent(type, objects);
		fireDocumentModified(e);
	}

	protected void fireDocumentModified(int type, IDocumentElementNode element) {
		fireDocumentModified(type, new IDocumentElementNode[] { element });
	}

	public ICDModel getModel() {
		if (getParent() != null)
			return getParent().getModel();
		return null;
	}

	protected boolean safeCompare(String s1, String s2) {
		if (s1 == s2) // we check for both nulls
			return true;
		if (s1 == null || s2 == null)
			return false;
		return s1.equals(s2);
	}

	protected boolean compareArray(String[] vs1, String[] vs2) {
		if (vs1 == vs2) // check whether they are both null
			return true;
		// else check whether only one of them is null
		if (vs1 == null || vs2 == null)
			return false;
		if (vs1.length != vs2.length)
			return false;
		for (int i = 0; i < vs1.length; i++) {
			if (!safeCompare(vs1[i], vs2[i])) {
				return false;
			}
		}
		return true;
	}

	public String toString() {
		return "E[name=" + getXMLTagName() + "]";
	}

	public String getXMLAttributeValue(String name) {
		String attributeValue = super.getXMLAttributeValue(name);
		return attributeValue != null ? attributeValue : "";
	}

	protected void swapElements(List list, int i1, int i2) {
		CDElement child2 = (CDElement) list.get(i2);
		CDElement child1 = (CDElement) list.get(i1);
		swap(child1, child2);
	}

	public String print() {
		return write();
	}

	public void executeOperation(CDModelEvent event, boolean reverse) {
		switch (event.getType()) {
		case CDModelEvent.ADDED:
		case CDModelEvent.REMOVED:
			if (event.getParent() != this)
				throw new IllegalArgumentException("The handler specified is not the same as the handler of this request. " + event);
			if (reverse ^ event.getType() == CDModelEvent.ADDED)
				executeAddOperation((CDElement) event.getChangedElement());
			else
				executeRemoveOperation((CDElement) event.getChangedElement());
			break;
		case CDModelEvent.CHANGED:
			if (event.getChangedAttribute() == null) { // swapped elements
				// when swapping, it doesn't matter the direction of the
				// operation
				executeSwapOperation((CDElement) event.getOldValue(), (CDElement) event.getNewValue());
			} else {
				if (reverse)
					executeChangeOperation(event.getChangedAttribute(), event.getOldValue());
				else
					executeChangeOperation(event.getChangedAttribute(), event.getNewValue());
			}
			break;
		default:
			throw new IllegalArgumentException("Received request for operation with type, which cannot be handled.");
		}
	}

	protected void executeAddOperation(CDElement element) {
		CDEditorPlugin.debug("[CDElement] executeAddOperation for " + element);
		addChildNode(element);
	}

	protected void executeRemoveOperation(CDElement element) {
		CDEditorPlugin.debug("[CDElement] executeRemoveOperation for " + element);
		removeChildNode(element);
	}

	protected void executeSwapOperation(CDElement element1, CDElement element2) {
		CDEditorPlugin.debug("[CDElement] executeSwapOperation for " + element1 + " and " + element2);
		swap(element1, element2);
	}

	protected void executeChangeOperation(String attribute, Object value) {
		CDEditorPlugin.debug("[CDElement] executeChangeOperation: attribute=" + attribute + "; value='" + value + "' on " + this);
		if (!safeCompare(getXMLAttributeValue(attribute), (String) value)) {
			String oldValue = getXMLAttributeValue(attribute);
			setXMLAttribute(attribute, (String) value);
			fireDocumentModified(this, attribute, oldValue, value);
			fireElementChanged(this, attribute, oldValue, value);
		} else {
			CDEditorPlugin.debug("[CDElement] No change to execute");
		}
	}

	public boolean isLeafNode() {
		// all nodes are leaves by default
		return true;
	}

	public void setRemoveIndex(int removeIndex) {
		this.removeIndex = removeIndex;
	}

	public int getRemoveIndex() {
		return removeIndex;
	}
}
