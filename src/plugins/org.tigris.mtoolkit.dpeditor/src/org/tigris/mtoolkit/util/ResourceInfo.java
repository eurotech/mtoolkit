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

import java.io.File;
import java.util.Vector;

/**
 * This class contains information on all non bundle resources thet should be
 * added to the deployment package.
 * 
 * @author todor
 * 
 */
public class ResourceInfo {
	String name;
	String resourcePath;
	String resourceProcessor;
	boolean missing;
	Vector otherHeaders = new Vector();

	/**
	 * This method retursn the full path to the resource in the filesystem.
	 * 
	 * @return a string contining the full path.
	 */
	public String getResourcePath() {
		return resourcePath;
	}

	/**
	 * sets the resource path to this resource in the file system.
	 * 
	 * @param resourcePath
	 */
	public void setResourcePath(String resourcePath) {
		this.resourcePath = resourcePath;
		if ((name == null || name.length() == 0) && resourcePath != null && resourcePath.length() != 0) {
			setName("resources" + "/" + resourcePath.substring(resourcePath.lastIndexOf(File.separator) + 1));
		}
	}

	/**
	 * return the resource processor of this resource.
	 * 
	 * @return the resource processor.
	 */
	public String getResourceProcessor() {
		return resourceProcessor;
	}

	/**
	 * sets the resource processor that should process this resource.
	 * 
	 * @param resourceProcessor
	 *            the resource processor id
	 */
	public void setResourceProcessor(String resourceProcessor) {
		this.resourceProcessor = resourceProcessor;
	}

	/**
	 * sets if this resource is missing. this header is valid only for fix
	 * packs.
	 * 
	 * @return the value of the Missing header.
	 */
	public boolean isMissing() {
		return missing;
	}

	/**
	 * sets the Missing flag for this resource. When the missing flag is set the
	 * resource appears in the manifest but is not actually packed in the
	 * deployment package
	 * 
	 * @param missing
	 */
	public void setMissing(boolean missing) {
		this.missing = missing;
	}

	/**
	 * returns tha name of the resource in the deplyment package's file system.
	 * 
	 * @return the name of the resource
	 */
	public String getName() {
		return name;
	}

	/**
	 * sets the name of the resource according to the deployment package's
	 * filesystem.
	 * 
	 * @param name
	 *            the new name.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns a string containing all the additional headers of this resource
	 * and their values. The header-value pairs are separated by ';' and the
	 * header and the value are separated by '\:' from each other.
	 * 
	 * @return the string representation of the non standard headers for this
	 *         bundle
	 */
	public String otherHeadersToString() {
		String result = "";
		if (otherHeaders != null) {
			result = DPPUtilities.convertToString(otherHeaders);
		}
		return result;
	}

	/**
	 * Returns the other headers associated to this resource. Apart from the
	 * standard headers the resource may have additional ones. These are stored
	 * in a Vector with String[2] - {keys, values}.
	 * 
	 * @return the vector containing the headers.
	 */
	public Vector getOtherHeaders() {
		return otherHeaders;
	}

	/**
	 * Sets the nonstandard headers for this resource.
	 * 
	 * @param otherHeaders
	 *            a vector containing the additional headers and their
	 *            corresponding values. All previous headers are destroyed.
	 */
	public void setOtherHeaders(Vector otherHeaders) {
		this.otherHeaders = otherHeaders;
	}

	/**
	 * Sets the additional headers of this bundle as a single string. The
	 * different headers are separated by ';' and the header and its value by
	 * '\:'. This practically means that these two symbols should not occur in
	 * neither the header nor the value.
	 * 
	 * @param str
	 *            the string representation of the bundle's additional headers
	 */
	public void setOtherHeaders(String str) {
		setOtherHeaders(DPPUtilities.convertToVector(str));
	}

}
