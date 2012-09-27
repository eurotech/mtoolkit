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

public class OSGiConsolePlugin extends AbstractUIPlugin {
  private static OSGiConsolePlugin instance  = null;
  public static final String       PLUGIN_ID = "org.tigris.mtoolkit.osgimanagement"; //$NON-NLS-1$

  public OSGiConsolePlugin() {
    super();
    if (instance == null) {
      instance = this;
    }
  }

  // Returns default instance
  public static OSGiConsolePlugin getDefault() {
    return instance;
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
    instance = null;
  }

  // Initialize perspectives
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
  }

  public String getId() {
    return PLUGIN_ID;
  }

  @Override
  protected void initializeImageRegistry(ImageRegistry reg) { // NO_UCD
    super.initializeImageRegistry(reg);
  }

  public static void error(String message, Throwable t) {
    getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, t));
  }

  public static void warning(String message, Throwable t) { // NO_UCD
    getDefault().getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message, t));
  }

  public static void log(IStatus status) {
    getDefault().getLog().log(status);
  }

  public static IStatus handleIAgentException(IAgentException e) {
    return newStatus(IStatus.ERROR, getErrorMessage(e), e.getCauseException());
  }

  public static IStatus newStatus(int severity, String message, Throwable t) {
    return new Status(severity, OSGiConsolePlugin.PLUGIN_ID, message, t);
  }

  private static String getErrorMessage(IAgentException e) {
    String msg = e.getMessage();
    if (msg == null) {
      msg = "Operation failed.";
      Throwable cause = e.getCauseException();
      if (cause != null && cause.getMessage() != null) {
        msg += " " + cause.getMessage(); //$NON-NLS-1$
      }
    }
    return msg;
  }
}
