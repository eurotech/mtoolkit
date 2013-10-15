/*******************************************************************************
 * Copyright (c) 2005, 2012 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 ****************************************************************************/
package org.tigris.mtoolkit.iagent.internal.rpc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.tigris.mtoolkit.iagent.Error;
import org.tigris.mtoolkit.iagent.rpc.spi.BundleManagerDelegate;
import org.tigris.mtoolkit.iagent.util.DebugUtils;

public final class DefaultBundleManagerDelegate implements BundleManagerDelegate {
  private final BundleContext bc;

  public DefaultBundleManagerDelegate(BundleContext bc) {
    this.bc = bc;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.spi.BundleManagerDelegate#installBundle(java.lang.String, java.io.InputStream)
   */
  public Object installBundle(String location, InputStream in) {
    try {
      String name = getFileNameFromLocation(location);
      if (!name.toLowerCase().endsWith(".jar")) {
        name = name + ".jar";
      }
      return bc.installBundle(name, in);
    } catch (BundleException e) {
      int code = RemoteBundleAdminImpl.getBundleErrorCode(e);
      return new Error(code, "Failed to install bundle: " + DebugUtils.toString(e), DebugUtils.getStackTrace(e));
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.spi.BundleManagerDelegate#uninstallBundle(org.osgi.framework.Bundle)
   */
  public Object uninstallBundle(Bundle bundle) {
    try {
      bundle.uninstall();
    } catch (BundleException e) {
      int code = RemoteBundleAdminImpl.getBundleErrorCode(e);
      return new Error(code, "Failed to uninstall bundle: " + DebugUtils.toString(e), DebugUtils.getStackTrace(e));
    } catch (IllegalStateException e) {
      // everything is OK
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.spi.BundleManagerDelegate#updateBundle(org.osgi.framework.Bundle, java.io.InputStream)
   */
  public Object updateBundle(Bundle bundle, InputStream in) {
    try {
      bundle.update(in);
    } catch (BundleException e) {
      int code = RemoteBundleAdminImpl.getBundleErrorCode(e);
      return new Error(code, "Failed to update bundle: " + DebugUtils.toString(e), DebugUtils.getStackTrace(e));
    } catch (IllegalStateException e) {
      return new Error(Error.BUNDLE_UNINSTALLED_CODE, "Bundle " + bundle.getBundleId() + " has been uninstalled");
    } finally {
      try {
        in.close();
      } catch (IOException e) {
      }
    }
    return null;
  }

  private static String getFileNameFromLocation(String location) {
    if (location == null) {
      return null;
    }
    String name = (new File(location)).getName();
    if (name.startsWith("remote:")) {
      name = name.substring("remote:".length(), name.length());
    }
    name = name.replace(':', '_');
    name = name.replace(' ', '_');
    return name;
  }
}
