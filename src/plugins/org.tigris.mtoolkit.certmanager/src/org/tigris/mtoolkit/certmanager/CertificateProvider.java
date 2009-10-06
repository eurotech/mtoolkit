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
package org.tigris.mtoolkit.certmanager;

import org.tigris.mtoolkit.certmanager.internal.preferences.CertStorage;
import org.tigris.mtoolkit.common.certificates.ICertificateDescriptor;
import org.tigris.mtoolkit.common.certificates.ICertificateProvider;

public class CertificateProvider implements ICertificateProvider {

	public ICertificateDescriptor[] getCertificates() {
		return CertStorage.getDefault().getCertificates();
	}

	public ICertificateDescriptor getCertificate(String uid) {
		ICertificateDescriptor certificates[] = getCertificates();
		for (int i = 0; i < certificates.length; i++) {
			if (certificates[i].getUid().equals(uid)) {
				return certificates[i];
			}
		}
		return null;
	}
}
