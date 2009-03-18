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
package org.tigris.mtoolkit.cdeditor.internal.model;


/**
 * Class CDModelEvent describes change in the Component Description Model. 
 * General types of the different changes are:
 * ADDED, REMOVED, CHANGED and RELOADED.
 */
public class CDModelEvent {

	public static final int ADDED = 1 << 0;
	public static final int REMOVED = 1 << 1;
	public static final int CHANGED = 1 << 2;
	public static final int RELOADED = 1 << 16;
	/**
	 * Event with this type is fired, when the model is revalidated without
	 * changes. This validation must be triggered by calling
	 * {@link ICDModel#validate()} method.
	 */
	public static final int REVALIDATED = 1 << 15;

	private int type;
	private Object changedElement;
	private String action;
	private Object oldValue;
	private Object newValue;
	private Object parent;

	/**
	 * General constructor for add/remove change.
	 * 
	 * @param type
	 * @param changeElement
	 * @param parent
	 */
	public CDModelEvent(int type, Object changeElement, Object parent) {
		this.type = type;
		this.changedElement = changeElement;
		this.parent = parent;
	}

	public CDModelEvent(Object changedElement, String action, Object oldValue,
			Object newValue) {
		this.type = CHANGED;
		this.changedElement = changedElement;
		this.action = action;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

	public CDModelEvent(Object parent, Object child1, Object child2) {
		this.type = CHANGED;
		this.changedElement = parent;
		this.oldValue = child1;
		this.newValue = child2;
	}

	public CDModelEvent() {
		this.type = RELOADED;
	}

	public int getType() {
		return type;
	}

	public Object getChangedElement() {
		return changedElement;
	}

	public String getChangedAttribute() {
		return action;
	}

	public Object getOldValue() {
		return oldValue;
	}

	public Object getNewValue() {
		return newValue;
	}

	public Object getParent() {
		return parent;
	}

	public String toString() {
		switch (getType()) {
		case ADDED:
			return "Event[ADDED, " + changedElement + ", " + parent + "]";
		case REMOVED:
			return "Event[REMOVED, " + changedElement + ", " + parent + "]";
		case CHANGED:
			if (action == null)
				return "Event[SWAPPED, " + changedElement + ", (" + oldValue + ", " + newValue + ")]";
			else
				return "Event[CHANGED, " + changedElement + ", " + action + ", " + oldValue + ", " + newValue + "]";
		case RELOADED:
			return "Event[RELOADED]";
		case REVALIDATED:
			return "Event[REVALIDATED]";
		default:
			return super.toString();
		}
	}

}
