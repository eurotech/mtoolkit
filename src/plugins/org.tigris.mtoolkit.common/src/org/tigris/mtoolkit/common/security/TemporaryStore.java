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
package org.tigris.mtoolkit.common.security;

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: Convert to support KeyStore interface
public class TemporaryStore {

	private List certificates = new ArrayList();
	private Map hostnamesMapping = new HashMap();

	public synchronized void addCertificate(Certificate cert) {
		if (!certificates.contains(cert))
			certificates.add(cert);
	}

	public synchronized boolean containsCertificate(Certificate cert) {
		return certificates.contains(cert);
	}
	
	public synchronized boolean containsMapping(String host, Certificate cert) {
		Certificate current = (Certificate) hostnamesMapping.get(host);
		return current != null && current.equals(cert);
	}
	
	public synchronized void addMapping(String host, Certificate cert) {
		hostnamesMapping.put(host, cert);
	}
}
