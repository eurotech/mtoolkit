/*******************************************************************************
 * Copyright (c) 2005, 2012 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.common.gui;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckable;

/**
 * Event object describing a change to the checked states of a viewer elements.
 * This event object can be used to incorporate check sate changes of multiple
 * elements.
 *
 * @see CheckStateChangedEvent
 * @since 6.0
 */
public final class CheckStateChangedEvent2 extends CheckStateChangedEvent {

  private static final long serialVersionUID = 5932685219565428075L;

  private Object[] elements;
  private boolean[] states;

  public CheckStateChangedEvent2(ICheckable source, Object[] elements, boolean state) {
    super(source, elements[0], state);
    this.elements = elements;
    this.states = new boolean[elements.length];
    for (int i = 0; i < elements.length; i++) {
      this.states[i] = state;
    }
  }

  public Object[] getElements() {
    if (elements == null) {
      return new Object[] { getElement() };
    }
    return elements;
  }

  public boolean getChecked(int index) { // NO_UCD
    if (states == null) {
      if (index != 0) {
        throw new IndexOutOfBoundsException();
      }
      return getChecked();
    }
    return states[index];
  }
}
