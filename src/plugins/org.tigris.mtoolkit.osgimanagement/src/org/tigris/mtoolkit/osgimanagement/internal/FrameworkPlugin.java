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
package org.tigris.mtoolkit.osgimanagement.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.Util;
import org.tigris.mtoolkit.osgimanagement.installation.FrameworkConnectorFactory;

public final class FrameworkPlugin extends AbstractUIPlugin {
  private static FrameworkPlugin instance      = null;

  public static final String     PLUGIN_ID     = "org.tigris.mtoolkit.osgimanagement"; //$NON-NLS-1$

  public static final String     IAGENT_RPC_ID = "org.tigris.mtoolkit.iagent.rpc";

  public static String           fileDialogLastSelection;

  public FrameworkPlugin() {
    super();
    if (instance == null) {
      instance = this;
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    FrameworkPlugin.instance = this;
    FrameworkConnectorFactory.init();
    FrameWorkView.restoreModel();
    fileDialogLastSelection = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    FrameWorkView.saveModel();
    FrameworkConnectorFactory.deinit();
    FrameworkPlugin.instance = null;
    super.stop(context);
  }

  // Returns default instance
  public static FrameworkPlugin getDefault() {
    return instance;
  }

  public static void error(IAgentException e) {
    log(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
  }

  public static void error(String message, Throwable t) {
    log(new Status(IStatus.ERROR, PLUGIN_ID, message, t));
  }

  public static void warning(String message, Throwable t) {
    log(new Status(IStatus.WARNING, PLUGIN_ID, message, t));
  }

  public static void log(IStatus status) {
    FrameworkPlugin fwPlugin = getDefault();
    if (fwPlugin == null) {
      return;
    }
    ILog fwLog = fwPlugin.getLog();
    fwLog.log(status);
  }

  public static File getFile(String name) {
    IPath statePath = getDefault().getStateLocation();
    return new File(statePath.toFile(), name);
  }

  /** Returns an input stream to plug-in's embedded resource. */
  public static InputStream getResourceStream(Path aPath) throws IOException {
    if (instance == null) {
      return FrameworkPlugin.class.getResourceAsStream(aPath.toPortableString());
    } else {
      URL resourceURL = FileLocator.find(instance.getBundle(), aPath, null);
      if (resourceURL != null) {
        return resourceURL.openStream();
      }
    }
    return null;
  }

  public static File[] getIAgentBundles() {
    Bundle[] bundles = getDefault().getBundle().getBundleContext().getBundles();
    Bundle selectedIAgentRpc = null;
    for (int i = 0; i < bundles.length; i++) {
      Bundle bundle = bundles[i];
      if (IAGENT_RPC_ID.equals(bundle.getSymbolicName())) {
        selectedIAgentRpc = bundle;
      }
    }
    if (selectedIAgentRpc != null) {
      try {
        File bundleFile = FileLocator.getBundleFile(selectedIAgentRpc);
        if (bundleFile.isFile()) {
          return new File[] {
            bundleFile
          };
        }
      } catch (IOException e) {
        getDefault().getLog().log(Util.newStatus(IStatus.ERROR, "Failed to find IAgent bundle(s)", e));
      }
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#initializeImageRegistry(org.eclipse.jface.resource.ImageRegistry)
   */
  @Override
  protected void initializeImageRegistry(ImageRegistry reg) {
    super.initializeImageRegistry(reg);
  }
}
