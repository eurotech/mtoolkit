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

/**
 * Common class for remote events.
 * 
 * @author Danail Nachev
 * 
 */
public class RemoteEvent {

	private int type;

	public RemoteEvent(int type) {
		this.type = type;
	}

	/**
	 * Returns the type of the event. Specific type constants are defined in the
	 * successors of this class.
	 * 
	 * @return the type of the event
	 */
	public int getType() {
		return type;
	}
}
