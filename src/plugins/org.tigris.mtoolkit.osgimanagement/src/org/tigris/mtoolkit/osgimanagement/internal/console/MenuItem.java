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
package org.tigris.mtoolkit.osgimanagement.internal.console;

import java.util.Enumeration;
import java.util.Vector;

import org.eclipse.jface.action.Action;

public class MenuItem extends Action {

	private int style = AS_PUSH_BUTTON;
	private int accelerator = 0;

	private Vector listeners;
	private String id;

	public MenuItem(String name) {
		super(name);
		listeners = new Vector();
		id = String.valueOf(hashCode());
	}

	public MenuItem(String name, int style) {
		this(name);
		this.style = style;
	}

	public int getAccelerator() {
		return accelerator;
	}

	public void setAccelerator(int acc) {
		accelerator = acc;
	}

	public void run() {
		fireSelectionEvent();
	}

	public int getStyle() {
		return style;
	}

	public String getId() {
		return id;
	}

	public void addMenuItemListener(MenuItemListener l) {
		if (l != null)
			listeners.addElement(l);
	}

	public void removeMenuItemListener(MenuItemListener l) {
		if (l != null)
			listeners.removeElement(l);
	}

	protected void fireSelectionEvent() {
		Enumeration e = listeners.elements();
		while (e.hasMoreElements()) {
			MenuItemListener l = (MenuItemListener) e.nextElement();
			l.menuSelected(this);
		}
	}

}