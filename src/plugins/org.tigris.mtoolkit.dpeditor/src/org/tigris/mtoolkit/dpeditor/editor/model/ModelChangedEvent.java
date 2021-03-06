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
package org.tigris.mtoolkit.dpeditor.editor.model;

public class ModelChangedEvent implements IModelChangedEvent {

	private int type;
	private Object[] changedObjects;
	private String changedProperty;

	/**
	 * The constructor of the event.
	 * 
	 * @param event
	 *            type
	 * @param changed
	 *            objects
	 * @param changedProperty
	 *            or <samp>null</samp> if not applicable
	 */
	public ModelChangedEvent(int type, Object[] objects, String changedProperty) {
		this.type = type;
		this.changedObjects = objects;
		this.changedProperty = changedProperty;
	}

	/**
	 * @see IModelChangedEvent#getChangedObjects
	 */
	public Object[] getChangedObjects() {
		return changedObjects;
	}

	/**
	 * @see IModelChangedEvent#getChangedProperty
	 */
	public String getChangedProperty() {
		return changedProperty;
	}

	/**
	 * @see IModelChangedEvent#getChangedType
	 */
	public int getChangeType() {
		return type;
	}
}
