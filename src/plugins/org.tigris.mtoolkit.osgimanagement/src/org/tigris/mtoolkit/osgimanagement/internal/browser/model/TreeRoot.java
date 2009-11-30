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
import org.tigris.mtoolkit.osgimanagement.model.Model;

public class TreeRoot extends Model {

	private ArrayList listeners;
	private boolean showBundlesID = false;
	private boolean showBundlesVersion = false;
	private String filter = "";

	public TreeRoot(String name) {
		super(name);
		listeners = new ArrayList();
	}

	public HashMap getFrameWorkMap() {
		Iterator iter = elementList.iterator();
		HashMap result = new HashMap(getSize());
		while (iter.hasNext()) {
			FrameworkImpl element = (FrameworkImpl) iter.next();
			result.put(element.getName(), element);
		}
		return result;
	}
	
	public void setFilter(String filter) {
		if ("".equals(filter))
			this.filter = "";
		else
			this.filter = filter.toLowerCase();
	}
	
	public String getFilter() {
		return filter;
	}
	
	public int getSelectedChildren() {
		return selectedChilds;
	}

	protected ArrayList getListeners() {
		synchronized (listeners) {
			ArrayList result = new ArrayList(listeners);
			return result;
		}
	}
	
	public boolean isShowBundlesID() {
		return showBundlesID;
	}

	public void setShowBundlesID(boolean b) {
		showBundlesID = b;
	}

	public boolean isShowBundlesVersion() {
		return showBundlesVersion;
	}

	public void setShowBundlesVersion(boolean b) {
		showBundlesVersion = b;
	}

	public void addListener(ContentChangeListener newListener) {
		synchronized (listeners) {
			if (!listeners.contains(newListener)) {
				listeners.add(newListener);
			}
		}
	}

	public void removeListener(ContentChangeListener oldListener) {
		synchronized (listeners) {
			listeners.remove(oldListener);
		}
	}

	protected boolean select(Model model) {
		if ("" == filter) {
			return true;
		}
		if (model instanceof FrameworkImpl) {
			return false;
		}
		if (model instanceof ServicesCategory || model instanceof BundlesCategory) {
			return false;
		}
		String text = model.toString();
		if (text.indexOf(filter) != -1 || text.toLowerCase().indexOf(filter) != -1) {
			return true;
		} else {
			return false;
		}
	}
	
	
}