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
 * This class contains infor for a single certificate used for signing a
 * deployment package.
 * 
 * @author Todor Cholakov
 * 
 */
public class CertificateInfo {
	String alias;
	String keystore;
	String keypass;
	String storepass;
	String storeType;

	/**
	 * This method retreives the store type for this certificate's store.
	 * Possible values are jks,pcks11,pkcs12, but not only them
	 * 
	 * @return the store type
	 */
	public String getStoreType() {
		return storeType;
	}

	/**
	 * Sets the store type for this certificate's store. Possible values are
	 * jks,pcks11,pkcs12, but not only them
	 * 
	 * @param storeType
	 *            the new store type to be set
	 */
	public void setStoreType(String storeType) {
		this.storeType = storeType;
	}

	/**
	 * Returns the store alias under which the keys needed for signing the
	 * deployment package are held
	 * 
	 * @return the alias
	 */
	public String getAlias() {
		return alias;
	}

	/**
	 * Sets the alias whose keys should be used for signing the deployment
	 * package
	 * 
	 * @param alias
	 *            the alias to be set
	 */
	public void setAlias(String alias) {
		this.alias = alias;
	}

	/**
	 * This method retreives the keystore password needed to access the keys
	 * used for signing the deployment package.
	 * 
	 * @return the keystore password
	 */
	public String getKeypass() {
		return keypass;
	}

	/**
	 * This method seta the keystore password to be used when signing the
	 * deployment package.
	 * 
	 * @param keypass
	 *            the keystore password that is set.
	 */
	public void setKeypass(String keypass) {
		this.keypass = keypass;
	}

	/**
	 * Returns the keystore location in the filesystem
	 * 
	 * @return the full path to the keystore contininge the keys for this
	 *         certificate
	 */
	public String getKeystore() {
		return keystore;
	}

	/**
	 * Sets the path to the keystore to be used for signing
	 * 
	 * @param keystore
	 *            the full path to the key store (usually a .keystore file)
	 */
	public void setKeystore(String keystore) {
		this.keystore = keystore;
	}

	/**
	 * This method retreives the keystore password that should be used to access
	 * the keys.
	 * 
	 * @return the keystore password
	 */
	public String getStorepass() {
		return storepass;
	}

	/**
	 * This method sets the keystore password that should be used to open the
	 * keystore
	 * 
	 * @param storepass
	 *            the password for the kystore.
	 */
	public void setStorepass(String storepass) {
		this.storepass = storepass;
	}

}
