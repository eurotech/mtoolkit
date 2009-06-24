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
package org.tigris.mtoolkit.util;

/**
 * This class contains all the info needed for building the DeploymentPackage.
 * 
 * @author Todor Cholakov
 * 
 */
public class BuildInfo {
	String buildLocation;
	String dpFileName;
	String antFileName;

	/**
	 * Returns the name of the ant build file.
	 * 
	 * @return the full path to the ant file which will be generated
	 */
	public String getAntFileName() {
		return antFileName;
	}

	/**
	 * Sets the name of the ant build file to be generated,
	 * 
	 * @param antFileName
	 *            a String representing the full path to the ant file to be
	 *            generated.
	 */
	public void setAntFileName(String antFileName) {
		this.antFileName = antFileName;
	}

	/**
	 * Returns the default build location for the dp
	 * 
	 * @return
	 */
	public String getBuildLocation() {
		return buildLocation;
	}

	/**
	 * Sets the default build location for the dp
	 * 
	 * @param buildLocation
	 */
	public void setBuildLocation(String buildLocation) {
		this.buildLocation = buildLocation;
	}

	/**
	 * Returns the full path to the deployment package to be generated.
	 * 
	 * @return a String representing the full path to the place where the DP to
	 *         be generated. If the file exists it will be overriten.
	 */
	public String getDpFileName() {
		return dpFileName;
	}

	/**
	 * sets the path to the place where the resulting deployment package to be
	 * generated.
	 * 
	 * @param dpFileName
	 *            the full path to the deployment package
	 */
	public void setDpFileName(String dpFileName) {
		this.dpFileName = dpFileName;
	}

	/**
	 * Clears all data in this object
	 */
	public void clear() {
		buildLocation = (buildLocation != null && buildLocation.equals("")) ? "" : buildLocation;
		dpFileName = (dpFileName != null && dpFileName.equals("")) ? "" : dpFileName;
		antFileName = (antFileName != null && antFileName.equals("")) ? "" : antFileName;
	}

}
