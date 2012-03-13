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
package org.tigris.mtoolkit.osgimanagement.dp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

  // The plug-in ID
  public static final String PLUGIN_ID = "org.tigris.mtoolkit.osgimanagement.ext.dp";

  public static final String PROPERTY_PACKAGE = "org.tigris.mtoolkit.osgimanagement.property_dp_context"; //$NON-NLS-1$

  // The shared instance
  private static Activator plugin;

  /**
   * The constructor
   */
  public Activator() {
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
   */
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
   */
  public void stop(BundleContext context) throws Exception {
    plugin = null;
    super.stop(context);
  }

  /**
   * Returns the shared instance
   *
   * @return the shared instance
   */
  public static Activator getDefault() {
    return plugin;
  }

  public static File saveFile(InputStream input, String name) throws IOException {
    IPath statePath = Platform.getStateLocation(plugin.getBundle());
    File file = new File(statePath.toFile(), name);
    if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
      throw new IOException("Failed to create bundle state folder");
    }
    FileOutputStream stream = new FileOutputStream(file);
    try {
      byte[] buf = new byte[8192];
      int read;
      while ((read = input.read(buf)) != -1) {
        stream.write(buf, 0, read);
      }
    } finally {
      stream.close();
    }
    return file;
  }
}
