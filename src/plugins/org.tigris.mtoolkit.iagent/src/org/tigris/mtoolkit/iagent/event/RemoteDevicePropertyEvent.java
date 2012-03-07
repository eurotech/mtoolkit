/*******************************************************************************
 * Copyright (c) 2005, 2012 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 ****************************************************************************/
package org.tigris.mtoolkit.iagent.event;

/**
 * Event object containing details about remote device properties changes.
 */
public class RemoteDevicePropertyEvent extends RemoteEvent {

	/**
	 * @since 3.0
	 */
	public static final int PROPERTY_CHANGED_TYPE = 0;

	private String property;
	private Object value;

	/**
	 * @since 3.0
	 */
	public RemoteDevicePropertyEvent(String property, Object value) {
		this(PROPERTY_CHANGED_TYPE, property, value);
	}

	/**
	 * @since 3.0
	 */
	public RemoteDevicePropertyEvent(int type, String property, Object value) {
		super(type);
		this.property = property;
		this.value = value;
	}

	public String toString() {
		return "Remote property event[type=" + convertType(getType()) + " property: " + property + " value: " + value + "]";
	}

	private String convertType(int type) {
		switch (type) {
		case PROPERTY_CHANGED_TYPE:
			return "PROPERTY_CHANGED";
		default:
			return "UNKNOWN(" + type + ")";
		}
	}

	/**
	 * @since 3.0
	 */
	public Object getProperty() {
		return property;
	}

	/**
	 * @since 3.0
	 */
	public Object getValue() {
		return value;
	}
}
