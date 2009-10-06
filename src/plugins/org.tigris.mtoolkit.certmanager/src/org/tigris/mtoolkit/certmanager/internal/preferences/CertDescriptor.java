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
package org.tigris.mtoolkit.certmanager.internal.preferences;

import org.eclipse.core.runtime.Assert;
import org.tigris.mtoolkit.common.certificates.ICertificateDescriptor;

public class CertDescriptor implements ICertificateDescriptor {
	private String alias;
	private String storeLocation;
	private String storeType;
	private String storePass;
	private String uid;
	private ICertificateModifyListener listener;

	public CertDescriptor(String uid) {
		Assert.isNotNull(uid);
		this.uid = uid;
	}

	public String getAlias() {
		return alias;
	}

	public String getStoreLocation() {
		return storeLocation;
	}

	public String getStoreType() {
		return storeType;
	}

	public String getStorePass() {
		return storePass;
	}

	public String getUid() {
		return uid;
	}

	public void setAlias(String alias) {
		this.alias = alias;
		fireModified();
	}

	public void setStoreLocation(String storeLocation) {
		this.storeLocation = storeLocation;
		fireModified();
	}

	public void setStoreType(String storeType) {
		this.storeType = storeType;
		fireModified();
	}

	public void setStorePass(String storePass) {
		this.storePass = storePass;
		fireModified();
	}

	/**
	 * Adds modify listener. Only one listener at a time is supported.
	 * 
	 * @param listener
	 *            the listener to be added.
	 */
	public void addModifyListener(ICertificateModifyListener l) {
		this.listener = l;
	}

	/**
	 * Removes the specified modify listener.
	 * 
	 * @param listener
	 *            the listener to be removed. Does nothing if passed listener is
	 *            not recognized.
	 */
	public void removeModifyListener(ICertificateModifyListener l) {
		if (this.listener == l) {
			this.listener = null;
		}
	}

	private void fireModified() {
		if (listener != null) {
			listener.certificateChanged(this);
		}
	}
}
