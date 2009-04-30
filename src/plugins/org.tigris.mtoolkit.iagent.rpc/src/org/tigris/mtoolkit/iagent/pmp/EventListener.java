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
package org.tigris.mtoolkit.iagent.pmp;

/**
 * Interface for receiving custom events.
 */
public interface EventListener {

	/**
	 * An event is received.
	 * 
	 * @param event
	 *            The event.
	 * @param evType
	 *            the event type.
	 */
	public void event(Object event, String evType);

}
