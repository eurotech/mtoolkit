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
package org.tigris.mtoolkit.osgimanagement.browser.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.ui.IActionFilter;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ContentChangeEvent;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ContentChangeListener;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;

public abstract class Model implements Comparable, IActionFilter, ConstantsDistributor {

	protected String name;
	protected Model parent;
	protected Set elementList;

	public Model(String name, Model parent) {
		this.name = name;
		this.parent = parent;
		elementList = new TreeSet();
	}

	public void addElement(Model element) {
		if (!element.getParent().equals(this)) {
			element.setParent(this);
		}

		if (element instanceof Bundle) {
			if (elementList.add((Bundle) element)) {
				fireElementAdded(element);
			}
		} else if (elementList.add(element))
			fireElementAdded(element);
	}

	private void setParent(Model parent) {
		this.parent = parent;
	}

	public void removeElement(Model element) {
		if (element instanceof Bundle) {
			if (elementList.remove((Bundle) element)) {
				fireElementRemoved((Bundle) element);
			}
		} else if (elementList.remove(element)) {
			fireElementRemoved(element);
		}
	}

	public Model[] getChildren() {
		if (elementList == null) {
			return new Model[0];
		}
		Model[] resultArray = new Model[elementList.size()];
		elementList.toArray(resultArray);
		return resultArray;
	}

	public int getSize() {
		if (elementList == null) {
			return 0;
		}
		return elementList.size();
	}

	protected ArrayList getListeners() {
		return parent.getListeners();
	}

	protected void fireElementAdded(Model target) {
		ArrayList listeners = this.getListeners();
		if (listeners == null) {
			return;
		}

		Iterator iter = listeners.iterator();
		while (iter.hasNext()) {
			ContentChangeListener listener = (ContentChangeListener) iter.next();
			ContentChangeEvent event = new ContentChangeEvent(target);
			listener.elementAdded(event);
		}
	}

	protected void fireElementChanged(Model target) {
		ArrayList listeners = this.getListeners();
		if (listeners == null) {
			return;
		}

		Iterator iter = listeners.iterator();
		while (iter.hasNext()) {
			ContentChangeListener listener = (ContentChangeListener) iter.next();
			ContentChangeEvent event = new ContentChangeEvent(target);
			listener.elementChanged(event);
		}
	}

	protected void fireElementRemoved(Model target) {
		ArrayList listeners = this.getListeners();
		if (listeners == null) {
			return;
		}

		Iterator iter = listeners.iterator();
		while (iter.hasNext()) {
			ContentChangeListener listener = (ContentChangeListener) iter.next();
			ContentChangeEvent event = new ContentChangeEvent(target);
			listener.elementRemoved(event);
		}
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Model getParent() {
		return parent;
	}

	public String toString() {
		return name;
	}

	public int compareTo(Object obj) {
		return this.toString().compareTo(obj.toString());
	}

	public void removeChildren() {
		if (getSize() < 1)
			return;
		Model[] elements = getChildren();
		for (int i = 0; i < elements.length; i++) {
			removeElement(elements[i]);
		}
	}

	public boolean testAttribute(Object target, String name, String value) {
		if (!(target instanceof Model)) {
			return false;
		}

		if (name.equalsIgnoreCase(NODE_NAME)) {
			if (value.equalsIgnoreCase(this.name)) {
				return true;
			}
		}
		return false;
	}

	public FrameWork findFramework() {
		FrameWork fw = null;
		Model model = this;
		while (model != null && !(model instanceof FrameWork)) {
			model = model.getParent();
		}
		if (model != null) {
			fw = (FrameWork) model;
		}

		return fw;
	}

	public void updateElement() {
		fireElementChanged(this);
	}

	public int indexOf(Model child) {
		Iterator iterator = elementList.iterator();
		int index = 0;
		while (iterator.hasNext()) {
			Model node = (Model) iterator.next();
			if (node == child)
				return index;
			index++;
		}
		return -1;
	}

}