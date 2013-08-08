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

import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ContentChangeListener;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public final class TreeRoot extends Model {
  private boolean         showBundlesID      = false;
  private boolean         showBundlesVersion = false;
  private String          filter             = "";
  private final ArrayList listeners          = new ArrayList();

  public TreeRoot(String name) {
    super(name);
  }

  public HashMap getFrameworkMap() {
    Model[] children = getChildren();
    HashMap result = new HashMap(children.length);
    for (int i = 0; i < children.length; i++) {
      FrameworkImpl element = (FrameworkImpl) children[i];
      result.put(element.getName(), element);
    }
    return result;
  }

  public void setFilter(String filter) {
    this.filter = filter.toLowerCase();
  }

  public String getFilter() {
    return filter;
  }

  public int getSelectedChildren() {
    return selectedChilds;
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

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.model.Model#getListeners()
   */
  @Override
  protected synchronized ArrayList getListeners() {
    synchronized (listeners) {
      ArrayList result = new ArrayList(listeners);
      return result;
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.model.Model#select(org.tigris.mtoolkit.osgimanagement.model.Model)
   */
  @Override
  protected synchronized boolean select(Model model) {
    if (filter.length() == 0) {
      return true;
    }
    if (model instanceof FrameworkImpl) {
      return false;
    }
    if (model instanceof ServicesCategory || model instanceof BundlesCategory) {
      return false;
    }
    String text = model.getLabel();
    if (text.toLowerCase().indexOf(filter) != -1) {
      return true;
    } else {
      return false;
    }
  }
}
