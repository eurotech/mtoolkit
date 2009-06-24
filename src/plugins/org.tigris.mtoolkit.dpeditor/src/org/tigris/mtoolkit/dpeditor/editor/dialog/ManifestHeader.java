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
package org.tigris.mtoolkit.dpeditor.editor.dialog;

/**
 * This class contains a key-value pair used to describe a single custom header
 * which is appeared in the Manifest of the bundle.
 */
public class ManifestHeader {

	/** Holds the key of the key-value pair */
	private String key;
	/** Holds the value of the key-value pair */
	private String value;

	/**
	 * Creates a new empty ManifestHeader object. The key and the value are
	 * empty strings (not null).
	 */
	public ManifestHeader() {
		this("", "");
	}

	/**
	 * Creates a new ManifestHeader object using given key and value.
	 * 
	 * @param key
	 *            the key of the key-value pair
	 * @param value
	 *            the value of the key-value pair
	 */
	public ManifestHeader(String key, String value) {
		this.key = key;
		this.value = value;
	}

	/**
	 * Returns a ManifestHeader name or key
	 * 
	 * @return a ManifestHeader name or key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Sets a ManifestHeader name or key.
	 * 
	 * @param key
	 *            the name or key which will be set
	 */
	public void setKey(String key) {
		if (key.equals("") || (key.trim().equals(":"))) {
			return;
		}
		this.key = key;
	}

	/**
	 * Returns the value associated to this ManifestHeader
	 * 
	 * @return the value of ManifestHeader
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Sets a new value for this ManifestHeader
	 * 
	 * @param value
	 *            the value to be set
	 */
	public void setValue(String value, boolean fireEvent) {
		this.value = value;
	}

	/**
	 * Returns a string representation of the ManifestHeader. Returns
	 * concatenated string between key and value.
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return key + value;
	}
}
