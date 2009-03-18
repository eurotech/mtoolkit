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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.eclipse.pde.internal.core.util.SAXParserWrapper;
import org.tigris.mtoolkit.cdeditor.internal.CDEditorPlugin;
import org.tigris.mtoolkit.cdeditor.internal.model.CDModelEvent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDBaseProperty;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDComponent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDElement;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDModel;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDModifyListener;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDReference;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDService;
import org.tigris.mtoolkit.cdeditor.internal.model.IEclipseContext;
import org.tigris.mtoolkit.cdeditor.internal.text.DocumentChangedEvent;
import org.tigris.mtoolkit.cdeditor.internal.text.XMLTextProcessor;
import org.xml.sax.SAXException;


public class CDModel extends CDElement implements ICDModel {

	private static final long serialVersionUID = -1968548053992561997L;

	private List components = new ArrayList();
	private volatile List listeners = null;
	private boolean dirty = false;
	private Map errors = new Hashtable();
	private IDocument document;
	private XMLTextProcessor textProcessor;
	private boolean loading = false;
	private IEclipseContext projectContext;

	// indicates that there can only be one component in the model
	private boolean single;
	private CDComponent singleComponent;

	public IEclipseContext getProjectContext() {
		return projectContext;
	}

	public void setEclipseContext(IEclipseContext projectContext) {
		this.projectContext = projectContext;
	}

	public void load(IDocument aDocument) throws SAXException {
		doLoad(aDocument);
		fireModelReloaded();
	}

	// performs load operation without firing Reload event
	private void doLoad(IDocument aDocument) throws SAXException {
		Assert.isNotNull(aDocument);
		loading = true;
		this.document = aDocument;
		textProcessor = new XMLTextProcessor(aDocument);
		try {
			InputStream in = new ByteArrayInputStream(aDocument.get().getBytes());
			try {
				SAXParserWrapper parser = new SAXParserWrapper();
				parser.parse(in, new ComponentDocumentHandler(this, true));
			} catch (IOException e) {
				// TODO: Better handle exceptions here, we should not swallow
				// them unresponsibly
			} catch (ParserConfigurationException e) {
			} catch (FactoryConfigurationError e) {
			}
			// mark the model as non-dirty, because we have loaded from scratch
			setDirty(false);
		} finally {
			loading = false;
		}
	}

	public void addComponent(ICDComponent component) {
		// XXX: What happens when we have single-component document
		configureNamespace((CDComponent) component);
		addChildNode((CDElement) component);
	}

	private void configureNamespace(CDComponent component) {
		String prefix = getPrefix(ComponentDescriptionValidator.SCR_NAMESPACE);
		if (prefix == null) {
			// no prefix available, we need to local namespace for this
			// component
			prefix = "scr";
			component.setXMLAttribute("xmlns:scr", ComponentDescriptionValidator.SCR_NAMESPACE);
		}
		component.setXMLTagName((prefix.length() > 0 ? prefix + ":" : "") + ModelUtil.getElementLocalName(component.getXMLTagName()));
	}

	public boolean registerComponent(ICDComponent component) {
		Assert.isNotNull(component);
		if (!components.contains(component)) {
			if (component instanceof CDComponent) {
				int pos = ModelUtil.getModelInsertIndex(components, CDComponent.class, (CDComponent) component);
				components.add(pos, component);
			} else {
				components.add(component);
			}
			fireElementAdded(component, component.getParent());
			return true;
		}
		return false;
	}

	public ICDComponent[] getComponents() {
		return (ICDComponent[]) components.toArray(new ICDComponent[components.size()]);
	}

	public boolean isDirty() {
		return dirty;
	}

	public void addModifyListener(ICDModifyListener listener) {
		if (listener == null)
			return;
		synchronized (this) {
			if (listeners == null)
				listeners = new LinkedList();
		}
		synchronized (listeners) {
			if (!listeners.contains(listener))
				listeners.add(listener);
		}
	}

	public void removeModifyListener(ICDModifyListener listener) {
		if (listener == null || listeners == null)
			return;
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	public void updateDocument() {
		textProcessor.flush();
		try { // reload model, so the nodes receive the new text coordinates
			doLoad(document);
		} catch (SAXException e) {
			throw new RuntimeException(e);
			// not expected
		}
	}

	public void removeComponent(int index) {
		CDElement component = (CDElement) components.get(index);
		// we are not sure who is the parent
		if (component != null && component.getParent() != null)
			((CDElement) component.getParent()).removeChildNode(component);
	}

	public boolean unregisterComponent(ICDComponent component) {
		if (components.remove(component)) {
			fireElementRemoved(component, component.getParent());
			return true;
		}
		return false;
	}

	public void fireDocumentModified(DocumentChangedEvent e) {
		if (suppressDocumentEvents || loading)
			return;
		textProcessor.handleDocumentEvent(e);
	}

	public void fireModified(CDModelEvent event) {
		if (loading)
			return;
		if ((event.getType() & (CDModelEvent.RELOADED | CDModelEvent.REVALIDATED)) == 0)
			setDirty(true);
		synchronized (this) {
			validateWithoutNotification();
			if (listeners == null || listeners.size() == 0)
				return;
			ICDModifyListener[] copiedListeners;
			synchronized (listeners) {
				copiedListeners = (ICDModifyListener[]) listeners.toArray(new ICDModifyListener[listeners.size()]);
			}
			for (int i = 0; i < copiedListeners.length; i++) {
				ICDModifyListener listener = copiedListeners[i];
				try {
					listener.modelModified(event);
				} catch (Throwable e) {
					CDEditorPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, CDEditorPlugin.PLUGIN_ID, "Failed to deliver event to listener: " + listener, e));
				}
			}
		}
	}

	private void fireModelReloaded() {
		fireModified(new CDModelEvent(CDModelEvent.RELOADED, null, null));
	}

	public ICDElement getParent() {
		return null;
	}

	public ICDModel getModel() {
		return this;
	}

	public void setDirty(boolean dirtyState) {
		dirty = dirtyState;
	}

	public void removeComponent(ICDComponent component) {
		CDElement elementParent = (CDElement) component.getParent();
		elementParent.removeChildNode((CDElement) component);
	}

	public void validate() {
		validateWithoutNotification();
		fireModified(new CDModelEvent(CDModelEvent.REVALIDATED, null, null));
	}

	private void validateWithoutNotification() {
		validateDocument(isSingle() ? (IDocumentElementNode) singleComponent : (IDocumentElementNode) this);
	}

	private List internalGetAggregatedValidationStatus(ICDElement element) {
		List aggregatedStatuses = new ArrayList();

		aggregatedStatuses.addAll(internalGetValidationStatus(element));

		if (element instanceof ICDComponent) {
			ICDComponent component = (ICDComponent) element;
			for (int i = 0; i < component.getReferences().length; i++) {
				ICDReference reference = component.getReferences()[i];
				aggregatedStatuses.addAll(internalGetAggregatedValidationStatus(reference));
			}
			for (int i = 0; i < component.getProperties().length; i++) {
				ICDBaseProperty property = component.getProperties()[i];
				aggregatedStatuses.addAll(internalGetAggregatedValidationStatus(property));
			}
			aggregatedStatuses.addAll(internalGetAggregatedValidationStatus(component.getService()));
		} else if (element instanceof ICDService) {
			ICDService service = (ICDService) element;
			for (int i = 0; i < service.getInterfaces().length; i++) {
				aggregatedStatuses.addAll(internalGetAggregatedValidationStatus(service.getInterfaces()[i]));
			}
		} else if (element instanceof ICDModel) {
			for (Iterator it = components.iterator(); it.hasNext();) {
				ICDComponent component = (ICDComponent) it.next();
				aggregatedStatuses.addAll(internalGetAggregatedValidationStatus(component));
			}
		}

		for (Iterator it = aggregatedStatuses.iterator(); it.hasNext();) {
			IStatus status = (IStatus) it.next();
			if (status.isOK())
				it.remove();
		}

		// sort the statuses, so we have any errors before the warnings,
		// no matter they are located in the hierarchy (we are asked for
		// aggregated statuses)
		Collections.sort(aggregatedStatuses, new Comparator() {
			public int compare(Object arg0, Object arg1) {
				IStatus s1 = (IStatus) arg0, s2 = (IStatus) arg1;
				if (s1.getSeverity() > s2.getSeverity()) {
					return -1;
				} else if (s1.getSeverity() < s2.getSeverity()) {
					return 1;
				} else {
					return 0;
				}
			}
		});
		return aggregatedStatuses;
	}

	public IStatus[] getAggregatedValidationStatus(ICDElement element) {
		List statuses = internalGetAggregatedValidationStatus(element);
		if (statuses.size() == 0)
			statuses.add(Status.OK_STATUS);
		return (IStatus[]) statuses.toArray(new IStatus[statuses.size()]);

	}

	private List internalGetValidationStatus(ICDElement element) {
		IStatus result = null;
		if (errors != null && element != null)
			result = (IStatus) errors.get(element);
		if (result == null)
			result = Status.OK_STATUS;
		List resultList = new ArrayList();
		resultList.add(result);
		return resultList;
	}

	public IStatus[] getValidationStatus(ICDElement element) {
		List statuses = internalGetValidationStatus(element);
		if (statuses.size() == 0)
			statuses.add(Status.OK_STATUS);
		return (IStatus[]) statuses.toArray(new IStatus[statuses.size()]);
	}

	public IDocument getDocument() {
		return document;
	}

	protected void swapModelObjects(CDElement child1, CDElement child2) {
		if (child1 instanceof CDComponent && child2 instanceof CDComponent) {
			int idx1 = components.indexOf(child1);
			int idx2 = components.indexOf(child2);
			if (idx1 == -1 || idx2 == -1)
				return;
			components.set(idx2, child1);
			components.set(idx1, child2);
			// components may have different parents
			fireElementSwapped(child1.getParent(), child1, child2);
		} else {
			super.swapModelObjects(child1, child2);
		}
	}

	private boolean doesComponentsHaveCommonParent(int idx1, int idx2) {
		if (idx1 < 0 || idx2 < 0 || idx1 > components.size() - 1 || idx2 > components.size() - 1)
			return false;
		CDComponent comp1 = (CDComponent) components.get(idx1);
		CDComponent comp2 = (CDComponent) components.get(idx2);
		return comp1.getParentNode() == comp2.getParentNode();
	}

	public boolean canMoveComponentDown(ICDComponent component) {
		int idx = components.indexOf(component);
		if ((idx < 0) || (idx >= components.size() - 1))
			return false;
		return doesComponentsHaveCommonParent(idx, idx + 1);
	}

	public boolean canMoveComponentUp(ICDComponent component) {
		int idx = components.indexOf(component);
		if (idx < 0 || idx == 0)
			return false;
		return doesComponentsHaveCommonParent(idx, idx - 1);
	}

	private void swapComponents(int idx1, int idx2) {
		CDComponent component1 = (CDComponent) components.get(idx1);
		CDComponent component2 = (CDComponent) components.get(idx2);
		if (component1.getParentNode() != component2.getParentNode())
			return;
		component1.getParentNode().swap(component1, component2);
	}

	public void moveDownComponent(ICDComponent component) {
		int itemToMoveIndex = components.indexOf(component);
		if (itemToMoveIndex == -1 || itemToMoveIndex == components.size() - 1)
			return;
		else
			swapComponents(itemToMoveIndex, itemToMoveIndex + 1);
	}

	public void moveUpComponent(ICDComponent component) {
		int itemToMoveIndex = components.indexOf(component);
		if (itemToMoveIndex == -1 || itemToMoveIndex == 0)
			return;
		else
			swapComponents(itemToMoveIndex, itemToMoveIndex - 1);
	}

	public boolean isRoot() {
		return true;
	}

	public boolean isSingle() {
		return single;
	}

	public ICDComponent getSingleComponent() {
		return singleComponent;
	}

	public void setSingle(boolean single) {
		this.single = single;
	}

	public void setSingleComponent(ICDComponent component) {
		if (component != null && component.getParent() != null)
			// this check is ensure that we don't add the same component as
			// multi-component and single component
			throw new IllegalArgumentException("Cannot already attached component as a single component");
		if (this.singleComponent != null) {
			this.singleComponent.setParent(null);
			this.singleComponent.setSingle(false);
		}
		if (component != null) {
			this.singleComponent = (CDComponent) component;
			this.singleComponent.setParent(this);
			this.singleComponent.setSingle(true);
		}
	}

	private void validateDocument(IDocumentElementNode root) {
		errors.clear();
		List validationResults = new ComponentDescriptionValidator(projectContext).validateDocument(root);
		for (Iterator it = validationResults.iterator(); it.hasNext();) {
			ValidationResult result = (ValidationResult) it.next();
			ICDElement element = findEnclosingElement(result.getElement());
			IStatus currentStatus = (IStatus) errors.get(element);
			if (currentStatus == null || currentStatus.getSeverity() < result.getStatus().getSeverity()) {
				errors.put(element, result.getStatus());
			}
		}
	}

	private ICDElement findEnclosingElement(IDocumentRange element) {
		IDocumentElementNode elementParent;
		if (element.getClass().equals(CDElement.class)) {
			// all generic nodes are CDElements now, so we need to skip over
			// them
			elementParent = ((CDElement) element).getParentNode();
		} else if (element instanceof CDElement) {
			// we have found a CDElement to which we can assign validation
			// status
			return (CDElement) element;
		} else if (element instanceof IDocumentAttributeNode) {
			elementParent = ((IDocumentAttributeNode) element).getEnclosingElement();
		} else if (element instanceof IDocumentTextNode) {
			elementParent = ((IDocumentTextNode) element).getEnclosingElement();
		} else if (element instanceof IDocumentElementNode) {
			elementParent = ((IDocumentElementNode) element).getParentNode();
		} else {
			Assert.isLegal(false, "Unrecognized element type: " + element);
			elementParent = null;
		}
		return findEnclosingElement(elementParent);
	}

	public String print() {
		return isSingle() ? singleComponent.write() : write();
	}

	public String toString() {
		return "M[single=" + single + "]";
	}

}
