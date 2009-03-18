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

import org.tigris.mtoolkit.cdeditor.internal.model.ICDInterface;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDService;


public class CDService extends CDElement implements ICDService {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8011611322728040591L;

	private List interfaces = new ArrayList();

	public CDService() {
		setXMLTagName(TAG_SERVICE);
	}

	public ICDInterface[] getInterfaces() {
		return (ICDInterface[]) interfaces.toArray(new ICDInterface[interfaces.size()]);
	}

	public int getServiceFactory() {
		return ModelUtil.parseEnumerateValue(getRawServiceFactory(), SERVICE_FACTORY_NAMES, SERVICE_FACTORY_DEFAULT, SERVICE_FACTORY_UNKNOWN);
	}

	public ICDInterface addInterface(String className) {
		CDInterface interfaceNode = new CDInterface();
		interfaceNode.setInterface(className);
		addInterface(interfaceNode);
		return interfaceNode;
	}

	public void addInterface(ICDInterface aInterface) {
		addChildNode((CDElement) aInterface);
	}

	public void setServiceFactory(int serviceFactory) {
		if (serviceFactory == SERVICE_FACTORY_UNKNOWN)
			return;
		if (getServiceFactory() != serviceFactory) {
			String oldValue = getRawServiceFactory();
			String newValue;
			if (serviceFactory == SERVICE_FACTORY_DEFAULT)
				newValue = "";
			else
				newValue = SERVICE_FACTORY_NAMES[serviceFactory - 1];
			setXMLAttribute(ATTR_SERVICEFACTORY, newValue);
			fireDocumentModified(this, ATTR_SERVICEFACTORY, oldValue, newValue);
			fireElementChanged(this, ATTR_SERVICEFACTORY, oldValue, newValue);
		}
	}

	public void removeInterface(int index) {
		ICDInterface selected = (ICDInterface) interfaces.get(index);
		removeChildNode((CDElement) selected);
	}

	public void removeInterface(ICDInterface aInterface) {
		removeChildNode((CDElement) aInterface);
	}

	protected boolean addModelObject(CDElement child) {
		if (child instanceof CDInterface) {
			if (!interfaces.contains(child)) {
				int pos = ModelUtil.getModelInsertIndex(interfaces, CDInterface.class, child);
				interfaces.add(pos, child);
				fireElementAdded(child, this);
				return true;
			} else {
				return false;
			}
		}
		return super.addModelObject(child);
	}

	protected boolean removeModelObject(CDElement child) {
		if (child instanceof CDInterface) {
			if (interfaces.remove(child)) {
				fireElementRemoved(child, this);
				return true;
			} else {
				return false;
			}
		}
		return super.removeModelObject(child);
	}

	protected void swapModelObjects(CDElement child1, CDElement child2) {
		if (child1 instanceof CDInterface && child2 instanceof CDInterface) {
			int idx1 = interfaces.indexOf(child1);
			int idx2 = interfaces.indexOf(child2);
			if (idx1 == -1 || idx2 == -1)
				return;
			interfaces.set(idx1, child2);
			interfaces.set(idx2, child1);
			fireElementSwapped(this, child1, child2);
		} else {
			super.swapModelObjects(child1, child2);
		}
	}

	public String getRawServiceFactory() {
		return getXMLAttributeValue(ATTR_SERVICEFACTORY);
	}

	public void moveDownInterface(ICDInterface aInterface) {
		int index = interfaces.indexOf(aInterface);
		if (index == interfaces.size() - 1) {
			return;
		}
		swapElements(interfaces, index, index + 1);
		return;
	}

	public void moveUpInterface(ICDInterface aInterface) {
		int index = interfaces.indexOf(aInterface);
		if (index == 0) {
			return;
		}
		swapElements(interfaces, index, index - 1);
		return;
	}

	public String toString() {
		return "S[servicefactory=" + getRawServiceFactory() + "]";
	}
}
