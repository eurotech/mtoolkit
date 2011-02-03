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
package com.prosyst.tools.maven;

 import java.io.File;

 import org.apache.maven.project.MavenProject;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IStatus;
 import org.maven.ide.eclipse.MavenPlugin;
 import org.maven.ide.eclipse.embedder.IMaven;

 import com.prosyst.tools.maven.internal.MavenCorePlugin;

 /**
  * Grouping class for various utility methods, which helps dealing with Maven
  * projects and artifacts.
  * 
  * @author Danail Nachev
  * @since 1.0
  * 
  */
 public class MavenUtils {

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
	  *            the location of the POM file itself or its containing folder
	  * @return the {@link File} pointing to the location, where the artifact
	  *         should be located after Maven build has finished
	  * @throws CoreException
	  *             in case the method cannot determine where the artifact should
	  *             be placed
	  */
	 public static File locateMavenArtifact(File pomLocation) throws CoreException {
		 if (!pomLocation.getName().equals(MavenConstants.POM_FILE))
			 pomLocation = new File(pomLocation, MavenConstants.POM_FILE);
		 if (!pomLocation.exists())
			 throw MavenCorePlugin.newException(IStatus.ERROR, "Cannot find POM file at specified location: "
				 + pomLocation.getAbsolutePath(), null);
		 MavenProject project = readPomProject(pomLocation);
		 String filename = getArtifactName(project);
		 File artifactFile = new File(project.getBuild().getDirectory(), filename);
		 return artifactFile;
	 }

	 private static String getArtifactName(MavenProject project) {
		 return project.getBuild().getFinalName() + ".jar";
	 }

	 private static MavenProject readPomProject(File pomFile) throws CoreException {
		 IMaven maven = MavenPlugin.getDefault().getMaven();
		 return maven.readProject(pomFile, null); // TODO: Add progress
	 }

 }
