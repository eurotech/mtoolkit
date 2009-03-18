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

import org.tigris.mtoolkit.cdeditor.internal.model.ICDReference;

public class CDReference extends CDElement implements ICDReference {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1166560289002596231L;

	public CDReference() {
		setXMLTagName(TAG_REFERENCE);
	}
	
	public String getBind() {
		return getXMLAttributeValue(ATTR_BIND);
	}

	public int getCardinality() {
		return ModelUtil.parseEnumerateValue(getRawCardinality(), CARDINALITY_NAMES_SHORT, CARDINALITY_DEFAULT, CARDINALITY_UNKNOWN);
	}
	
	public int getPolicy() {
		return ModelUtil.parseEnumerateValue(getRawPolicy(), POLICY_NAMES, POLICY_DEFAULT, POLICY_UNKNOWN);
	}

	public String getInterface() {
		return getXMLAttributeValue(ATTR_INTERFACE);
	}

	public String getName() {
		return getXMLAttributeValue(ATTR_NAME);
	}
	
	public String getTarget() {
		return getXMLAttributeValue(ATTR_TARGET);
	}

	public String getUnbind() {
		return getXMLAttributeValue(ATTR_UNBIND);
	}

	public void setBind(String bind) {
		if (!safeCompare(getBind(), bind)) {
			String oldValue = getBind();
			setXMLAttribute(ATTR_BIND, bind);
			fireDocumentModified(this, ATTR_BIND, oldValue, bind);
			fireElementChanged(this, ATTR_BIND, oldValue, bind);
		}
	}

	public void setCardinality(int cardinality) {
		if (cardinality == ICDReference.CARDINALITY_UNKNOWN)
			return;	// don't set uknown cardinality, it means don't change the current
		if (getCardinality() != cardinality) {
			String oldValue = getXMLAttributeValue(ATTR_CARDINALITY);
			String newValue;
			if (cardinality == ICDReference.CARDINALITY_DEFAULT)
				newValue = null;
			else
				newValue = ICDReference.CARDINALITY_NAMES_SHORT[cardinality - 1];
			setXMLAttribute(ATTR_CARDINALITY, newValue);
			fireDocumentModified(this, ATTR_CARDINALITY, oldValue, newValue);
			fireElementChanged(this, ATTR_CARDINALITY, oldValue, newValue);
		}
	}

	public void setInterface(String value) {
		if (!safeCompare(getInterface(), value)) {
			String oldValue = getInterface();
			setXMLAttribute(ATTR_INTERFACE, value);
			fireDocumentModified(this, ATTR_INTERFACE, oldValue, value);
			fireElementChanged(this, ATTR_INTERFACE, oldValue, value);
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

	public void setPolicy(int policy) {
		if (policy == ICDReference.POLICY_UNKNOWN)
			// don't set unknown value, it means don't change the current value
			return;
		if (getPolicy() != policy) {
			String oldValue = getXMLAttributeValue(ATTR_POLICY);
			String newValue;
			if (policy == ICDReference.POLICY_DEFAULT)
				newValue = null;
			else
				newValue = ICDReference.POLICY_NAMES[policy - 1];
			setXMLAttribute(ATTR_POLICY, newValue);
			fireDocumentModified(this, ATTR_POLICY, oldValue, newValue);
			fireElementChanged(this, ATTR_POLICY, oldValue, newValue);
		}
	}

	public void setTarget(String target) {
		if (!safeCompare(getTarget(), target)) {
			String oldValue = getTarget();
			setXMLAttribute(ATTR_TARGET, target);
			fireDocumentModified(this, ATTR_TARGET, oldValue, target);
			fireElementChanged(this, ATTR_TARGET, oldValue, target);
		}
	}

	public void setUnbind(String unbind) {
		if (!safeCompare(getUnbind(), unbind)) {
			String oldValue = getUnbind();
			setXMLAttribute(ATTR_UNBIND, unbind);
			fireDocumentModified(this, ATTR_UNBIND, oldValue, unbind);
			fireElementChanged(this, ATTR_UNBIND, oldValue, unbind);
		}
	}

	public String getRawCardinality() {
		return getXMLAttributeValue(ATTR_CARDINALITY);
	}

	public String getRawPolicy() {
		return getXMLAttributeValue(ATTR_POLICY);
	}
	
	public boolean isLeafNode() {
		return true;	// no child nodes are expected to be added
	}

	public String toString() {
		return "R[interface=" + getInterface() + ",name=" + getName() + ",cardinality=" + getRawCardinality() + ",policy=" + getRawPolicy() + ",target=" + getTarget() + ",bind=" + getBind() + ",unbind=" + getUnbind() + "]";
	}

	

}
