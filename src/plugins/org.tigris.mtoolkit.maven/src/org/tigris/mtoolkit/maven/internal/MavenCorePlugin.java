/*******************************************************************************
 * Copyright (c) 2011 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.maven.internal;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.tigris.mtoolkit.maven.internal.images.ImageHolder;

/**
 * The activator class controls the plug-in life cycle
 */
public final class MavenCorePlugin extends AbstractUIPlugin {
  public static final String     PLUGIN_ID = "org.tigris.mtoolkit.maven"; //$NON-NLS-1$

  // The shared instance
  private static MavenCorePlugin plugin;

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    plugin = null;
    super.stop(context);
  }

  public static MavenCorePlugin getDefault() {
    return plugin;
  }

  /**
   * Returns an image descriptor for the image file at the given plug-in
   * relative path
   *
   * @param path
   *          the path
   * @return the image descriptor
   */
  public static ImageDescriptor getImageDescriptor(String path) {
    return imageDescriptorFromPlugin(PLUGIN_ID, path);
  }

  @Override
  protected void initializeImageRegistry(ImageRegistry reg) {
    super.initializeImageRegistry(reg);
    ImageHolder.initializeImageRegistry(reg);
  }

  public static void error(String message, Throwable t) { // NO_UCD
    log(newStatus(IStatus.ERROR, message, t));
  }

  public static void warning(String message, Throwable t) { // NO_UCD
    log(newStatus(IStatus.WARNING, message, t));
  }

  public static void log(IStatus status) {
    getDefault().getLog().log(status);
  }

  public static CoreException newException(int severity, String message, Throwable t) {
    return new CoreException(newStatus(severity, message, t));
  }

  public static CoreException newException(IStatus status) {
    return new CoreException(status);
  }

  public static IStatus newStatus(int severity, String message, Throwable t) {
    return new Status(severity, PLUGIN_ID, message, t);
  }

  public static IStatus newStatus(int severity, int code, String message, Throwable t) {
    return new Status(severity, PLUGIN_ID, code, message, t);
  }

  public static IStatus newStatus(String message, Throwable t, List<IStatus> children) { // NO_UCD
    IStatus[] statuses = children.toArray(new IStatus[children.size()]);
    return new MultiStatus(PLUGIN_ID, 0, statuses, message, t);
  }

  public static CoreException newException(int severity, int code, String message, Throwable t) { // NO_UCD
    return newException(newStatus(severity, code, message, t));
  }
}
