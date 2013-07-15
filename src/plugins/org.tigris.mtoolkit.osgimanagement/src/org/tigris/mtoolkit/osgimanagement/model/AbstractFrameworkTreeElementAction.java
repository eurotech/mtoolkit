/*******************************************************************************
 * Copyright (c) 2005, 2013 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.osgimanagement.model;

import java.util.Iterator;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

public abstract class AbstractFrameworkTreeElementAction<T extends Model> extends AbstractFrameworkTreeAction {
  private final boolean                allowMultiple;
  private final Class<? extends Model> elementType;

  protected AbstractFrameworkTreeElementAction(boolean allowMultiple, Class<? extends Model> elementType,
      ISelectionProvider provider, String text) {
    super(provider, text);
    this.allowMultiple = allowMultiple;
    this.elementType = elementType;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    final ISelection selection = getSelection();
    Iterator<Model> iterator = getStructuredSelection().iterator();
    while (iterator.hasNext()) {
      execute((T) iterator.next());
    }
    getSelectionProvider().setSelection(selection);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.IStateAction#updateState(org.eclipse.jface.viewers.IStructuredSelection)
   */
  @Override
  public void updateState(IStructuredSelection selection) {
    if (selection.isEmpty()) {
      setEnabled(false);
      return;
    }
    if (!allowMultiple && selection.size() != 1) {
      setEnabled(false);
      return;
    }
    setEnabled(true);
    Iterator<Object> iterator = selection.iterator();
    while (iterator.hasNext()) {
      Object element = iterator.next();
      if (!(elementType.isAssignableFrom(element.getClass()) && isEnabledFor((T) element))) {
        setEnabled(false);
        return;
      }
    }
  }

  protected boolean isEnabledFor(T element) {
    return true;
  }

  protected abstract void execute(T element);
}
