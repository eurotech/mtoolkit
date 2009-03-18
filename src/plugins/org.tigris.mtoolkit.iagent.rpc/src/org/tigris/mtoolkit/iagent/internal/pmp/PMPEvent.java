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
package org.tigris.mtoolkit.iagent.internal.pmp;

public class PMPEvent {

	/**
	 * The custom event contained in this BasicEvent
	 */
	public Object data;

	/**
	 * The type of the custom event contained in this BasicEvent
	 */
	public String eventType;
	
	  
	/**
	 * Constructs a BasicEvent that represents a custom event.
	 * 
	 * @param event
	 *            the custom event
	 * @param eventType
	 *            the custom event's type
	 */
	public PMPEvent(String evType, Object event) {
		this.eventType = evType;
		this.data = event;
	}
	
	public static final String ADD_LISTENER_OPERATION = "add_listener"; 
	public static final String REMOVE_LISTENER_OPERATION = "remove_listener";
	
	/* package */ PMPEvent next;
}
