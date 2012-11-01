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

import java.util.HashMap;
import java.util.Map;

/**
 * @since 6.0
 */
public final class ListItem {
  public Object  element    = null;
  public boolean checked    = false;
  protected Map  properties = new HashMap();

  /**
   * Sets the given property.
   *
   * @param key
   * @param value
   * @return the old value of the property
   */
  public Object setProperty(String key, Object value) {
    return properties.put(key, value);
  }

  /**
   * Returns the value of the property for the given key.
   *
   * @param key
   * @return the value of the property
   */
  public Object getProperty(String key) {
    return properties.get(key);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return element != null ? element.toString() : super.toString();
  }
}
