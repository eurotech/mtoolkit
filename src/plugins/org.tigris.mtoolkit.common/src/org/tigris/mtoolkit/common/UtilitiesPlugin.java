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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.tigris.mtoolkit.common.images.UIResources;

public class UtilitiesPlugin extends AbstractUIPlugin {

  public static final String     PLUGIN_ID = "org.tigris.mtoolkit.common"; //$NON-NLS-1$

  private static UtilitiesPlugin inst;

  private BundleContext          bundleContext;

  /**
   * Creates the Utilities plugin and caches its default instance
   * 
   * @param descriptor
   *            the plugin descriptor which the receiver is made from
   */
  public UtilitiesPlugin() {
    super();
    if (inst == null)
      inst = this;
  }

  public void start(BundleContext context) throws Exception {
    super.start(context);
    bundleContext = context;
  }

  public void stop(BundleContext context) throws Exception {
    super.stop(context);
  }

  public BundleContext getBundleContext() {
    return bundleContext;
  }

  public String getId() {
    return PLUGIN_ID;
  }

  /**
   * Gets the plugin singleton.
   * 
   * @return the default UtilitiesPlugin instance
   */
  public static UtilitiesPlugin getDefault() {
    return inst;
  }

  public IWorkbenchPage getActivePage() {
    IWorkbenchWindow window = getWorkbench().getActiveWorkbenchWindow();
    if (window == null)
      return null;
    return getWorkbench().getActiveWorkbenchWindow().getActivePage();
  }

  public static IStatus newStatus(int severity, String message, Throwable t) {
    return new Status(severity, PLUGIN_ID, message, t);
  }

  public static void error(String message, Throwable t) {
    getDefault().getLog().log(newStatus(IStatus.ERROR, message, t));
  }

  /**
   * Returns an image descriptor for the image file at the given plug-in
   * relative path
   * 
   * @param path
   *            the path
   * @return the image descriptor
   */
  public static ImageDescriptor getImageDescriptor(String path) {
    return imageDescriptorFromPlugin(PLUGIN_ID, path);
  }

  protected void initializeImageRegistry(ImageRegistry reg) {
    super.initializeImageRegistry(reg);
    UIResources.initializeImageRegistry(reg);
  }

}