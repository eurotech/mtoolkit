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
package org.tigris.mtoolkit.console.utils;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.tigris.mtoolkit.iagent.IAgentException;

public final class OSGiConsolePlugin extends AbstractUIPlugin {
  public static final String       PLUGIN_ID = "org.tigris.mtoolkit.console"; //$NON-NLS-1$

  private static OSGiConsolePlugin instance  = null;

  public String getId() {
    return PLUGIN_ID;
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    OSGiConsolePlugin.instance = this;
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
    OSGiConsolePlugin.instance = null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#initializeImageRegistry(org.eclipse.jface.resource.ImageRegistry)
   */
  @Override
  protected void initializeImageRegistry(ImageRegistry reg) {
    super.initializeImageRegistry(reg);
  }

  // Returns default instance
  public static OSGiConsolePlugin getDefault() {
    return instance;
  }

  public static void error(String message, Throwable t) {// NO_UCD
    getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, t));
  }

  public static void warning(String message, Throwable t) { // NO_UCD
    getDefault().getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message, t));
  }

  public static void log(IStatus status) {// NO_UCD
    getDefault().getLog().log(status);
  }

  public static IStatus handleIAgentException(IAgentException e) {// NO_UCD
    return newStatus(IStatus.ERROR, getErrorMessage(e), e.getCauseException());
  }

  public static IStatus newStatus(int severity, String message, Throwable t) {// NO_UCD
    return new Status(severity, OSGiConsolePlugin.PLUGIN_ID, message, t);
  }

  private static String getErrorMessage(IAgentException e) {
    String msg = e.getMessage();
    if (msg == null) {
      msg = "Operation failed."; //$NON-NLS-1$
      Throwable cause = e.getCauseException();
      if (cause != null && cause.getMessage() != null) {
        msg += " " + cause.getMessage(); //$NON-NLS-1$
      }
    }
    return msg;
  }
}
