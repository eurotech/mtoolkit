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
 * This class contains an old and new jar file as a <code>String</code>.
 */
public class CheckedJarHolder {

	/** Holds the old jar file as <code>String</code> */
	private String oldJar;
	/** Holds the new jar file as <code>String</code> */
	private String newJar;
	/** Holds whether this jar is checked or not */
	private boolean isChecked = true;

	/**
	 * Creates a new empty CheckedJarHolder object. The oldJar and the newJar
	 * are empty strings (not null).
	 */
	public CheckedJarHolder() {
		this("", "");
	}

	/**
	 * Creates a new CheckedJarHolder object using given old and new jar file
	 * values.
	 * 
	 * @param oldJar
	 *            the key of the key-value pair
	 * @param newJar
	 *            the value of the key-value pair
	 */
	public CheckedJarHolder(String oldJar, String newJar) {
		this.oldJar = oldJar;
		this.newJar = newJar;
	}

	/**
	 * Returns an old jar as <code>String</code>.
	 * 
	 * @return an old jar as <code>String</code>
	 */
	public String getOldJar() {
		return oldJar;
	}

	/**
	 * Sets an old jar.
	 * 
	 * @param oldJar
	 *            the name of the old jar which will be set
	 */
	public void setOldJar(String oldJar) {
		if (oldJar.equals("") || (oldJar.trim().equals(":"))) {
			return;
		}
		this.oldJar = oldJar;
	}

	/**
	 * Returns the new jar as <code>String</code>.
	 * 
	 * @return the new jar as <code>String</code>
	 */
	public String getNewJar() {
		return newJar;
	}

	/**
	 * Sets a new jar for this CheckedJarHolder
	 * 
	 * @param newJar
	 *            the new jar to be set
	 */
	public void setNewJar(String newJar) {
		this.newJar = newJar;
	}

	/**
	 * Returns if the jar is checked or not.
	 * 
	 * @return <code>true</code> if the jar is checked, otherwise
	 *         <code>false</code>
	 */
	public boolean isChecked() {
		return isChecked;
	}

	/**
	 * Set jar will be checked or not.
	 * 
	 * @param checked
	 *            <code>true</code> if the jar is checked, and
	 *            <code>false</code> if the jar will not be checked
	 */
	public void setChecked(boolean checked) {
		this.isChecked = checked;
	}

	/**
	 * Returns a string representation of the CheckedJarHolder.
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return oldJar + newJar + isChecked;
	}
}
