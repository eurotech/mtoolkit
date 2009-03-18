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
package org.tigris.mtoolkit.common;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.eclipse.swt.SWT;

public class ResourceManager {
  
  public static Exception dumbException;
  private static ResourceBundle resources;
  
  static {
    try {
      String resourceBundle = "utilities"; //$NON-NLS-1$
      resources = ResourceBundle.getBundle(resourceBundle, Locale.getDefault());
    } catch (MissingResourceException ex) {
//      ex.printStackTrace();
      dumbException = ex;
    }
  }
  
  public static String getString(String key) {
    return getString(key, ""); //$NON-NLS-1$
  }
  
  public static String getString(String key, String defaultValue) {
    if (resources == null) {
      return defaultValue;
    }
    String  result;
    try {
      result = resources.getString(key);
    } catch (MissingResourceException ex) {
      result = defaultValue;
    }
    return result;
  }
  
  public static int getAccelerator(String key)throws Exception {
    String chS = getString(key + ".acc", "").trim(); //$NON-NLS-1$ //$NON-NLS-2$
    if (chS.length() == 0)return 0;
    char  c = chS.toUpperCase().charAt(0);
    String  mod = getString(key + ".mod", "").trim(); //$NON-NLS-1$ //$NON-NLS-2$
    if (mod.length() == 0)return 0;
    StringTokenizer  sTok = new StringTokenizer(mod, "+ "); //$NON-NLS-1$
    int  acc = 0;
    while (sTok.hasMoreTokens()) {
      Field  f = SWT.class.getField(sTok.nextToken());
      acc |= f.getInt(null);
    }
    acc |= c;
    return acc;
  }
  
  public static ResourceBundle getResourceBundle() {
    return resources;
  }
  
}