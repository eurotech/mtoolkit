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
package org.tigris.mtoolkit.osgimanagement.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.eclipse.ui.IActionFilter;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ContentChangeEvent;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ContentChangeListener;

/**
 * @since 5.0
 */
public abstract class Model implements Comparable<Object>, IActionFilter {
  protected String  name;
  protected Model   parent;

  protected boolean selected       = false;
  protected int     selectedChilds = 0;

  private Set       elementList    = Collections.synchronizedSet(new TreeSet());

  private Model     master;
  private Vector    slaves         = new Vector();

  public Model(String name) {
    this.name = name;
  }

  public Model(String name, Model master) {
    this(name);
    if (master != null) {
      this.master = master;
      if (master.slaves == null) {
        master.slaves = new Vector();
      }
      master.slaves.addElement(this);
    }
  }

  public Vector getSlaves() {
    return slaves;
  }

  public Model getMaster() {
    return master;
  }

  public void addElement(Model element) {
    if (element.getParent() != null) {
      throw new IllegalArgumentException(
          "Cannot change the parent of model object without removing it from the old one");
    }

    if (element.selectedChilds != 0) {
      fireChildSelected(element.selectedChilds);
    }

    element.setParent(this);

    filterRecursively(element);

    if (elementList.add(element)) {
      fireElementAdded(element);
    }
  }

  public void removeElement(Model element) {
    element.setParent(null);
    if (elementList.remove(element)) {
      fireChildSelected(-element.selectedChilds);
      fireElementRemoved(element);
      if (element instanceof Framework) {
        ((Framework) element).listeners.clear();
      }
    }
  }

  public Model[] getChildren() {
    return (Model[]) elementList.toArray(new Model[elementList.size()]);
  }

  public Model[] getSelectedChildrenRecursively() {
    List children = new ArrayList(selectedChilds);
    synchronized (elementList) {
      internalGetSelectedChildrenRecursively(children);
    }
    return (Model[]) children.toArray(new Model[children.size()]);
  }

  protected void internalGetSelectedChildrenRecursively(List result) {
    for (Iterator it = elementList.iterator(); it.hasNext();) {
      Model child = (Model) it.next();
      if (child.selected) {
        result.add(child);
      }
      child.internalGetSelectedChildrenRecursively(result);
    }
  }

  public int getSize() {
    return elementList.size();
  }

  protected ArrayList getListeners() {
    if (parent == null) {
      return null;
    }
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

  // When name of a element is changed at elementList node is removed and added with its new name.
  public void setName(String name) {
    Model parent = this.getParent();
    if (parent != null && parent.elementList.remove(this)) {
      this.name = name;
      parent.elementList.add(this);
    }
    this.name = name;
  }

  public String getName() {
    return name;
  }

  /**
   * Returns human-readable text for using in UI.
   *
   * @return the label
   */
  public String getLabel() {
    return getName();
  }

  public Model getParent() {
    return parent;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return name;
  }

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(Object obj) {
    return this.toString().compareTo(obj.toString());
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.IActionFilter#testAttribute(java.lang.Object, java.lang.String, java.lang.String)
   */
  public boolean testAttribute(Object target, String name, String value) {
    if (!(target instanceof Model)) {
      return false;
    }

    if (name.equalsIgnoreCase(ConstantsDistributor.NODE_NAME)) {
      if (value.equalsIgnoreCase(this.name)) {
        return true;
      }
    }
    return false;
  }

  public void removeChildren() {
    if (getSize() < 1) {
      return;
    }
    Model[] elements = getChildren();
    for (int i = 0; i < elements.length; i++) {
      removeElement(elements[i]);
      elements[i].removeChildren();
    }
    for (int i = 0; i < getSlaves().size(); i++) {
      ((Model) getSlaves().elementAt(i)).removeChildren();
    }
  }

  public boolean isVisible() {
    return selected || (selectedChilds > 0);
  }

  public boolean containSelectedChilds() {
    return selectedChilds - (selected ? 1 : 0) > 0;
  }

  protected boolean select(Model model) {
    if (getParent() != null) {
      return getParent().select(model);
    }
    return false;
  }

  public void filter() {
    boolean selected = select(this);
    int selectedDelta;
    synchronized (this) {
      if (selected == this.selected) {
        return;
      }
      this.selected = selected;
      selectedDelta = selected ? 1 : -1;
    }
    fireChildSelected(selectedDelta);
  }

  public Framework findFramework() {
    Framework fw = null;
    Model model = this;
    while (model != null && !(model instanceof Framework)) {
      model = model.getParent();
    }
    if (model != null) {
      fw = (Framework) model;
    }

    return fw;
  }

  public void updateElement() {
    fireElementChanged(this);
  }

  public int indexOf(Model child) {
    int index = 0;
    synchronized (elementList) {
      Iterator iterator = elementList.iterator();
      while (iterator.hasNext()) {
        Model node = (Model) iterator.next();
        if (node == child) {
          return index;
        }
        index++;
      }
    }
    return -1;
  }

  private void filterRecursively(Model element) {
    element.filter();
    Model[] children = element.getChildren();
    if (children != null && children.length > 0) {
      for (int i = 0; i < children.length; i++) {
        filterRecursively(children[i]);
      }
    }
  }

  private void fireChildSelected(int delta) {
    synchronized (this) {
      selectedChilds += delta;
    }
    if (getParent() != null) {
      getParent().fireChildSelected(delta);
    }
  }

  private void setParent(Model parent) {
    this.parent = parent;
  }

}
