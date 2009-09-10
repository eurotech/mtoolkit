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
package org.tigris.mtoolkit.iagent.event;

import java.util.Dictionary;

import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;

public class EventData {
	private Object conEvent = null;
	private String eventType = null;

	public EventData(Object convEvent, String eventType) {
		this.conEvent = convEvent;
		this.eventType = eventType;
	}

	public Object getConvertedEvent() {
		return conEvent;
	}

	public String getEventType() {
		return eventType;
	}

	public String toString() {
		return "EventData[event="
						+ (conEvent instanceof Dictionary	? DebugUtils.convertForDebug((Dictionary) conEvent)
															: conEvent)
						+ "; type="
						+ eventType
						+ "]";
	}

}
