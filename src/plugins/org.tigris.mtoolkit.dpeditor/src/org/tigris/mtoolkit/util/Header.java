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
 * this class contains a key-value pair used to describe a single header.
 * 
 * @author tony
 * 
 */
public class Header {

	private String key;
	private String value;

	/**
	 * creates a new empty Header object. The key and the value are empty
	 * strings (not null)
	 * 
	 */
	public Header() {
		this("", "");
	}

	/**
	 * creates a new Header object using the given key and value
	 * 
	 * @param key
	 *            the key
	 * @param value
	 *            the value
	 */
	public Header(String key, String value) {
		this.key = key;
		this.value = value;
	}

	/**
	 * returns the header name or key
	 * 
	 * @return the header name
	 */
	public String getKey() {
		return key;
	}

	/**
	 * sets a new header name
	 * 
	 * @param key
	 *            the name to be set
	 */
	public void setKey(String key) {
		if (key.equals("") || (key.trim().equals(":"))) {
			return;
		}
		this.key = key;
	}

	/**
	 * return the value associated to this header
	 * 
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * sets a new value for this header
	 * 
	 * @param value
	 *            the value to be set.
	 */
	public void setValue(String value) {
		this.value = value;
	}

	public String toString() {
		return "" + key + value;
	}
}
