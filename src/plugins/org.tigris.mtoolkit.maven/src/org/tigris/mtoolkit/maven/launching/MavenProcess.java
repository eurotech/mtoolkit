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
package org.tigris.mtoolkit.maven.launching;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.RefreshUtil;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.tigris.mtoolkit.maven.MavenConstants;
import org.tigris.mtoolkit.maven.internal.MavenCorePlugin;

public final class MavenProcess {
  private static final String MAVEN_LAUNCH_TYPE = "org.eclipse.m2e.Maven2LaunchConfigurationType";

  private MavenProcess() {
  }

  public static void launchDefaultBuild(File location, IProgressMonitor monitor) throws CoreException {
    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType launchConfigurationType = launchManager.getLaunchConfigurationType(MAVEN_LAUNCH_TYPE);
    if (launchConfigurationType == null) {
      throw MavenCorePlugin.newException(IStatus.ERROR, "Cannot find maven launch configuration", null);
    }
    // String goals = "clean install";
    String goals = "";
    for (String goal : MavenConstants.DEFAULT_GOALS) {
      goals += goal + " ";
    }

    String loc = location.getPath();
    loc = loc.replace('\\', '-');
    loc = loc.replace('/', '-');
    ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null, //
        "Executing " + goals + " in " + loc);
    workingCopy.setAttribute("M2_GOALS", goals);
    //Unit tests should should be bypassed
    workingCopy.setAttribute("M2_SKIP_TESTS", true);

    workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, location.getPath());
    workingCopy.setAttribute(RefreshUtil.ATTR_REFRESH_SCOPE, "${project}");
    workingCopy.setAttribute(RefreshUtil.ATTR_REFRESH_RECURSIVE, true);

    ILaunch launch = workingCopy.launch(ILaunchManager.RUN_MODE, monitor);

    IProcess[] processes = launch.getProcesses();
    if (processes == null || processes.length != 1) {
      throw MavenCorePlugin.newException(IStatus.ERROR, "Could not start maven install process!", null);
    }
    IProcess process = processes[0];
    while (!process.isTerminated()) {
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
      }
    }
    int exitValue = process.getExitValue();
    if (exitValue != 0) {
      throw MavenCorePlugin.newException(IStatus.ERROR, "Install process exit value is: " + exitValue, null);
    }
  }
}
