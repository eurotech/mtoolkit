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


public class DocumentChangedEvent {

	public static final int INSERT = 1;
	public static final int REMOVE = 2;
	public static final int CHANGE = 3;
	
	private int type;
	private Object[] changedElements;
	private String changedProperty;
	private Object oldValue;
	private Object newValue;

	public DocumentChangedEvent(int type, Object[] changedElements) {
		this.type = type;
		this.changedElements = changedElements;
	}
	
	public DocumentChangedEvent(Object changedElement, String changedProperty, Object oldValue, Object newValue) {
		this.type = CHANGE;
		this.changedProperty = changedProperty;
		this.changedElements = new Object[] { changedElement };
		this.newValue = newValue;
		this.oldValue = oldValue;
	}

	public int getChangeType() {
		return type;
	}
	
	public Object[] getChangedObjects() {
		return changedElements;
	}
	
	public String getChangedProperty() {
		return changedProperty;
	}
	
	public Object getOldValue() {
		return oldValue;
	}
	
	public Object getNewValue() {
		return newValue;
	}
	
}
