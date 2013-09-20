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
package org.tigris.mtoolkit.maven;

import java.io.File;

import org.apache.maven.project.MavenProject;
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
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.tigris.mtoolkit.maven.internal.MavenCorePlugin;

/**
 * Grouping class for various utility methods, which helps dealing with Maven
 * projects and artifacts.
 *
 * @author Danail Nachev
 * @since 1.0
 *
 */
public class MavenUtils {
  private static final String MAVEN_LAUNCH_TYPE = "org.eclipse.m2e.Maven2LaunchConfigurationType";

  private MavenUtils() {
  }

  /**
   * Locates the artifact, produced when Maven build is executed for given
   * location, by parsing and resolving the POM contents.
   * <p>
   * Location can be either the path to the POM file itself or the containing
   * folder. The method's result is the location, where the artifact should be
   * available after Maven build has finished. No check is done, whether the
   * location is valid. It is up to the caller to check that the file is
   * actually there.
   * </p>
   * <p>
   * <em>Note: the method for determining the output location of the Maven
   * build does not guarantee, that it will be successful, because everyone
   * can customize their Maven build. This method relies on that this is not
   * recommended and it is very rarely done.</em>
   * </p>
   *
   * @param pomLocation
   *          the location of the POM file itself or its containing folder
   * @return the {@link File} pointing to the location, where the artifact
   *         should be located after Maven build has finished
   * @throws CoreException
   *           in case the method cannot determine where the artifact should be
   *           placed
   */
  public static File locateMavenArtifact(File pomLocation) throws CoreException {
    if (!pomLocation.getName().equals(MavenConstants.POM_FILE)) {
      pomLocation = new File(pomLocation, MavenConstants.POM_FILE);
    }
    if (!pomLocation.exists()) {
      throw MavenCorePlugin.newException(IStatus.ERROR,
          "Cannot find POM file at specified location: " + pomLocation.getAbsolutePath(), null);
    }
    MavenProject project = readPomProject(pomLocation);
    String filename = getArtifactName(project);
    File artifactFile = new File(project.getBuild().getDirectory(), filename);
    return artifactFile;
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

  private static String getArtifactName(MavenProject project) {
    return project.getBuild().getFinalName() + ".jar";
  }

  private static MavenProject readPomProject(File pomFile) throws CoreException {
    IMaven maven = MavenPlugin.getMaven();
    return maven.readProject(pomFile, null); // TODO: Add progress
  }
}
