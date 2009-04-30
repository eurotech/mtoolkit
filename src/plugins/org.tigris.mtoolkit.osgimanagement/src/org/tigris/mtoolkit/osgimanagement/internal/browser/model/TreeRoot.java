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
package org.tigris.mtoolkit.osgimanagement.internal.browser.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ContentChangeListener;

public class TreeRoot extends Model {

	private ArrayList listeners;

	public TreeRoot(String name, Model parent) {
		super(name, parent);
		listeners = new ArrayList();
	}

	public HashMap getFrameWorkMap() {
		Iterator iter = elementList.iterator();
		HashMap result = new HashMap(getSize());
		while (iter.hasNext()) {
			FrameWork element = (FrameWork) iter.next();
			result.put(element.getName(), element);
		}
		return result;
	}

	protected ArrayList getListeners() {
		return listeners;
	}

	public void addListener(ContentChangeListener newListener) {
		if (!listeners.contains(newListener)) {
			listeners.add(newListener);
		}
	}

	public void removeListener(ContentChangeListener oldListener) {
		listeners.remove(oldListener);
	}
}