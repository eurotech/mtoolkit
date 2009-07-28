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

import java.security.cert.X509Certificate;

/**
 * 
 *
 */
public interface UserInteraction {

	/**
	 * Validation result code, indicating that certificate has expired.
	 */
	int CERTIFICATE_EXPIRED = 0x00000001;

	/**
	 * Validation result code, indicating that certificate is not valid yet, i.e. it's start date is
	 * in the future.
	 */
	int CERTIFICATE_NOT_VALID_YET = 0x00000002;

	/**
	 * Validation result code, indicating that certificate chain doesn't lead to a certificate,
	 * which is included in the keystore, meaning that the certificate is issued by an untrusted
	 * authority.
	 */
	int CERTIFICATE_NOT_TRUSTED = 0x00000004;

	/**
	 * Validation result code, indicating that real name of the server and the certificate name
	 * mismatch.
	 */
	int HOSTNAME_MISMATCH = 0x00000008;

	/**
	 * Validation result code, indicating that the server trust cannot be validated, because no
	 * certificate was issued, its certificate alogirthm is unsuitable for authentication.
	 */
	int VERIFICATION_FAILED = 0x00000010;

	/**
	 * Called by {@link InteractiveTrustManager}, when a certificate cannot be trusted. The
	 * implementation of this interface should display a warning to the user regarding the exact
	 * problem with the certificate and ask him to confirm that the certificate is trusted.
	 * 
	 * @param validationResult
	 *            the validation result of the certificate chain. It's value is one of the
	 *            following: {@link #CERTIFICATE_EXPIRED}, {@link #CERTIFICATE_NOT_TRUSTED},
	 *            {@link #CERTIFICATE_NOT_VALID_YET}, {@link #HOSTNAME_MISMATCH}.
	 * @param message
	 *            a short text, giving additional information for the problem. This text can be
	 *            hidden additionally and displayed on user request for additional details. It can
	 *            be null.
	 * @param host
	 *            hostname for which a secure connection is established
	 * @param certChain
	 *            the certificate chain which trustworthness must be confirmed by the user. It can
	 *            be null, if there is more general problem with the certificates.
	 * @return true if the certificate trust is confirmed, false otherwise
	 */
	public boolean confirmConnectionTrust(int validationResult, String message, String host, X509Certificate[] certChain);

}
