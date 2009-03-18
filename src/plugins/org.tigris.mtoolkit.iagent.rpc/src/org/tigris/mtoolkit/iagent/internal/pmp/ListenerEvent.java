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

import org.tigris.mtoolkit.iagent.pmp.EventListener;

class ListenerEvent extends PMPEvent {

	public static final byte ADD_LISTENER_OP = 1;
	public static final byte REMOVE_LISTENER_OP = 2;
	
	public byte op;
	
	public ListenerEvent(byte op, String evType, EventListener listener) {
		super(evType, listener);
		this.op = op;
	}

}
