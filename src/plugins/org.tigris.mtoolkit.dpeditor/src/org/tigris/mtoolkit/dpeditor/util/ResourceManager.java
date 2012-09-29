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
package org.tigris.mtoolkit.dpeditor.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.tigris.mtoolkit.dpeditor.DPActivator;

public class ResourceManager {
  private static ResourceBundle resources;

  static {
    try {
      String resourceBundle = "dppplugin";
      resources = ResourceBundle.getBundle(resourceBundle, Locale.getDefault());
    } catch (MissingResourceException ex) {
      DPActivator.error(ex.getMessage(), ex);
    }
  }

  public static String getString(String key) {
    return getString(key, "");
  }

  public static String getString(String key, String defaultValue) {
    if (resources == null) {
      return defaultValue;
    }
    String result;
    try {
      result = resources.getString(key);
    } catch (MissingResourceException ex) {
      result = defaultValue;
    }
    return result;
  }

  /**
   * Returns the formatted message for the given key in the resource bundle.
   *
   * @param key
   *          the resource name
   * @param args
   *          the message arguments
   * @return the string
   */

  public static String format(String key, Object[] args) {
    return java.text.MessageFormat.format(getString(key), args);
  }
}
