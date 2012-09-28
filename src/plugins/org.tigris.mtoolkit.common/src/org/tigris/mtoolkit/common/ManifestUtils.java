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
package org.tigris.mtoolkit.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public final class ManifestUtils {
  private static int          MAXLINE        = 511;
  private static final String LINE_SEPARATOR = "\n "; //$NON-NLS-1$
  private static final String LIST_SEPARATOR = ",\n "; //$NON-NLS-1$

  private ManifestUtils() {
  }

  public static Map getManifestHeaders(InputStream stream) throws IOException {
    try {
      return ManifestElement.parseBundleManifest(stream, null);
    } catch (BundleException e) {
      IOException ioe = new IOException(Messages.ManifestUtils_Invalid_JAR_Manifest + e.toString());
      ioe.initCause(e);
      throw ioe;
    }
  }

  /**
   * @since 1.2
   */
  public static String getBundleSymbolicName(Map headers) {
    try {
      ManifestElement[] element = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME,
          (String) headers.get(Constants.BUNDLE_SYMBOLICNAME));
      if (element == null) {
        return null;
      }
      return element[0].getValueComponents()[0].trim();
    } catch (BundleException e) {
      return null;
    }
  }

  /**
   * @since 1.2
   */
  public static String getBundleVersion(Map headers) {
    try {
      ManifestElement[] element = ManifestElement.parseHeader(Constants.BUNDLE_VERSION,
          (String) headers.get(Constants.BUNDLE_VERSION));
      if (element == null) {
        return null;
      }
      return element[0].getValueComponents()[0].trim();
    } catch (BundleException e) {
      return null;
    }
  }

  public static void writeManifest(OutputStream stream, Map headers) throws IOException { // NO_UCD
    Object[] keys = headers.keySet().toArray();
    for (int i = 0; i < keys.length; i++) {
      String key = (String) keys[i];
      String value = (String) headers.get(key);
      writeEntry(stream, key, value);
    }
  }

  private static void writeEntry(OutputStream out, String key, String value) throws IOException {
    if (value != null && value.length() > 0) {
      out.write(splitOnComma(key + ": " + value).getBytes()); //$NON-NLS-1$
      out.write('\n');
    }
  }

  private static String splitOnComma(String value) {
    if (value.length() < MAXLINE || value.indexOf(LINE_SEPARATOR) >= 0) {
      return value; // assume the line is already split
    }
    String[] values = ManifestElement.getArrayFromList(value);
    if (values == null || values.length == 0) {
      return value;
    }
    StringBuffer sb = new StringBuffer(value.length() + ((values.length - 1) * LIST_SEPARATOR.length()));
    for (int i = 0; i < values.length - 1; i++) {
      sb.append(values[i]).append(LIST_SEPARATOR);
    }
    sb.append(values[values.length - 1]);
    return sb.toString();
  }

}
