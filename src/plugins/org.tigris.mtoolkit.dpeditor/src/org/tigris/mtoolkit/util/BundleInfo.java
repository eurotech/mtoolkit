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
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.tigris.mtoolkit.dpeditor.util.DPPUtil;

/**
 * This class contains all the info for a single bundle, needed when embedding
 * the bundle in a deployment package.
 * 
 * @author Todor Cholakov
 * 
 */
public class BundleInfo {
	String name;
	String bundlePath;
	boolean customizer = false;

	String bundleSymbolicName;
	String bundleVersion;
	boolean missing = false;
	Vector otherHeaders = new Vector();

	boolean isSymbolicNameFromJar = false;
	boolean isVersionFromJar = false;

	/**
	 * This method returns the absolute path to the bundle on the file system
	 * including the name.
	 * 
	 * @return a String representing the path to the bundle.
	 */
	public String getBundlePath() {
		return bundlePath;
	}

	/**
	 * This method sets the bundle path of the bundle referenced by this
	 * BundleInfo object. If the bundlePath parameter contains a valid path, the
	 * SymbolicName, Bundle Version and the name are automatically extracted
	 * from the jar and set.
	 * 
	 * @param bundlePath
	 *            the path that is to be set
	 */
	//what the hell? is this.. a simple setter?
	public void setBundlePath(String bundlePath) {
		this.bundlePath = bundlePath;
		
		if (bundlePath == null || bundlePath.length() == 0) {
			return;
		}
		try {
			JarFile jf = new JarFile(bundlePath);
			Manifest mf = jf.getManifest();
			jf.close();
			String value = null;
			String bunVersion = null;
			if (mf != null) {
				value = mf.getMainAttributes().getValue("Bundle-SymbolicName");
				bunVersion = "" + mf.getMainAttributes().getValue("Bundle-Version");
			}
			setBundleSymbolicName(DPPUtil.parseSymbolicName(value));
			isSymbolicNameFromJar = !(value == null || value.equals("") || value.equals("null"));
			isVersionFromJar = !(bunVersion == null || bunVersion.equals("") || bunVersion.equals("null"));
			if (bunVersion == null || bunVersion.equals("null")) {
				bunVersion = "";
			}
			setBundleVersion(bunVersion);
			
			String prefix = DPPUtilities.getPath(getName());
			if (prefix == null) {
				prefix = "bundles/";
			}
			setName(prefix + bundlePath.substring(bundlePath.lastIndexOf(File.separator) + 1));
		} catch (IOException e) {
		}
	}

	/**
	 * Checks if this bundle is a customizer.
	 * 
	 * @return true if this bundle should be customizer (resource processor) or
	 *         not
	 */
	public boolean isCustomizer() {
		return customizer;
	}

	/**
	 * Sets if this bundle should be customizer or not.
	 * 
	 * @param customizer
	 *            when true this bundle is marked as customizer.
	 */
	public void setCustomizer(boolean customizer) {
		this.customizer = customizer;
	}

	/**
	 * Returns the bundle's SymbolicName
	 * 
	 * @return the SymbolicName of this bundle. Must match that in the jar if
	 *         any.
	 */
	public String getBundleSymbolicName() {
		return bundleSymbolicName;
	}

	/**
	 * Sets the SymbolicName which to be used for the bundle
	 * 
	 * @param bundleSymbolicName
	 *            the new SymbolicName for the bundle. The symbolic name must
	 *            match the symbolic name in the bundle's manifest if any.
	 */
	public void setBundleSymbolicName(String bundleSymbolicName) {
		this.bundleSymbolicName = bundleSymbolicName;
	}

	/**
	 * Returns the version of this bundle
	 * 
	 * @return a string representing the bundle's version. It must match the
	 *         bundle version attribute in the bundle's manifest.
	 */
	public String getBundleVersion() {
		return bundleVersion;
	}

	/**
	 * Sets the bundle version of the bundle. This method dose not change the
	 * bundle's manifest itself.
	 * 
	 * @param bundleVersion
	 *            the version to be set. It muast match the one in the manifest.
	 */
	public void setBundleVersion(String bundleVersion) {
		this.bundleVersion = bundleVersion;
	}

	/**
	 * This method returns true if the bundle is missing in this package but
	 * should stay and be taken from the previously installed version. Valid
	 * only for fixed packs
	 * 
	 * @return true if the bundle should be marked as missing
	 */
	public boolean isMissing() {
		return missing;
	}

	/**
	 * Sets that the bundle should be marked as missing. Missing bundles are not
	 * contained in the fix pack but are used from the previous version of the
	 * package.
	 * 
	 * @param missing
	 *            the new missing status.
	 */
	public void setMissing(boolean missing) {
		this.missing = missing;
	}

	/**
	 * Returns the other headers associated to this bundle. Apart from the
	 * standard headers the bundle may have additional ones. These are stored in
	 * a Vector with String[2] - {keys, values}.
	 * 
	 * @return the vector containing the headers.
	 */
	public Vector getOtherHeaders() {
		return otherHeaders;
	}

	/**
	 * Sets the nonstandard headers for this bundle.
	 * 
	 * @param otherHeaders
	 *            a vector containing the additional headers and their
	 *            corresponding values. All previous headers are destroyed.
	 */
	public void setOtherHeaders(Vector otherHeaders) {
		this.otherHeaders = otherHeaders;
	}

	/**
	 * Returns the name and path of this bundle in the DP's file system. For
	 * example bundles/bundle1.jar. This name is used when creating the .dp file
	 * itself,so the system knows where to put the bundle. The default is
	 * bundles/<orginal jar name>
	 * 
	 * @return the name of the bundle.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name and path of this bundle in the DP's file system. The
	 * default is bundles/<orginal jar name>
	 * 
	 * @param name
	 *            the name of the bundle
	 */
	public void setName(String name) {
		this.name = name;
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

	/**
	 * Returns a string containing all the additional headers of this bundle and
	 * their values. The header-value pairs are separated by ';' and the header
	 * and the value are separated by '\:' from each other.
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
	 * Returns if a bundle symbolic name is set from the chosen jar or the user
	 * is set this value.
	 * 
	 * @return <code>true</code> if bundle symbolic name is set from the jar
	 *         file, otherwise <code>false</code>
	 */
	public boolean isSymbolicNameSetFromJar() {
		return isSymbolicNameFromJar;
	}

	/**
	 * Returns if a bundle version is set from the chosen jar or the user is set
	 * this value.
	 * 
	 * @return <code>true</code> if bundle version is set from the jar file,
	 *         otherwise <code>false</code>
	 */
	public boolean isVersionSetFromJar() {
		return isVersionFromJar;
	}

	public String toString() {
		String result = "";
		if (bundlePath != null) {
			result = bundlePath;
		}
		result += ";";
		if (bundleSymbolicName != null) {
			result += bundleSymbolicName;
		}
		result += ";";
		if (bundleVersion != null) {
			result += bundleVersion;
		}
		result += result + ";" + missing;
		result += ";" + customizer;
		result += ";" + otherHeadersToString();
		return result;
	}
}
