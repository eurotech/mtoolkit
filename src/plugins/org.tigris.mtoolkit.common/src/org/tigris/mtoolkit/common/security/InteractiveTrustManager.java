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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.net.ssl.SSLException;
import javax.net.ssl.X509TrustManager;

public class InteractiveTrustManager implements X509TrustManager {

	private static KeyStore defaultStore;
	
	private static org.apache.commons.ssl.HostnameVerifier verifier = org.apache.commons.ssl.HostnameVerifier.DEFAULT_AND_LOCALHOST;

	private static final String CACERTS_PATH = System.getProperty("java.home") + File.separatorChar + "lib" + File.separatorChar + "security" + File.separatorChar + "cacerts"; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$
	private static final String CACERTS_TYPE = "JKS"; //$NON-NLS-1$

	private KeyStore keyStore;
	private TemporaryStore tempStore;
	private UserInteraction userInteraction;
	private String host;
	
	/**
	 * Flag indicates confirm dialog was already shown.
	 * If dialog was shown last results will be used
	 */
	private boolean confirmDialogShown = false;

	public InteractiveTrustManager(	KeyStore keyStore,
									UserInteraction userInteraction,
									TemporaryStore tempStore,
									String host) {
		if (userInteraction == null)
			throw new NullPointerException("userInteraction cannot be null");
		if (keyStore == null) {
			// try to get the default keystore
			try {
				keyStore = getDefaultKeyStore();
			} catch (GeneralSecurityException e) {
				IllegalArgumentException iae = new IllegalArgumentException("Key store wasn't specified and the default keystore failed to initialize");
				iae.initCause(e);
				throw iae;
			}
		}
		this.keyStore = keyStore;
		this.userInteraction = userInteraction;
		this.host = host;
		this.tempStore = tempStore;
	}

	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		// ignore, check only server trust
	}

	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		checkCertificate(chain);
	}
	
	private void checkCertificate(X509Certificate[] chain) throws CertificateException {
		int result = validateCertificate(chain);
		if (host != null)
			result |= validateHostname(host, chain[0]);
		if (result != 0) {
			try {
				if (!confirmDialogShown) {
					if (userInteraction.confirmConnectionTrust(result, null, host, chain)) {
						// add the certificate in the store
						tempStore.addCertificate(chain[0]);
						// add the mapping between the host and the certificate as well
						tempStore.addMapping(host, chain[0]);
						confirmDialogShown = true;
						return;
					} else {
						confirmDialogShown = true;
					}
				} else {
					return;
				}
			} catch (Exception e) {
				CertificateException ce = new CertificateException("Certificate trustworthy cannot be confirmed");
				ce.initCause(e);
				throw ce;
			}
		}
		if ((result & UserInteraction.CERTIFICATE_EXPIRED) != 0) {
			throw new CertificateExpiredException();
		} else if ((result & UserInteraction.CERTIFICATE_NOT_VALID_YET) != 0) {
			throw new CertificateNotYetValidException();	
		} else if ((result & UserInteraction.CERTIFICATE_NOT_TRUSTED) != 0) {
			throw new CertificateException("Server provided certificate, which is not trusted.");
		} else if ((result & UserInteraction.HOSTNAME_MISMATCH) != 0) {
			throw new CertificateException("Server provided certificate, which is doesn't match the hostname of the server.");
		} else if (result != 0) {
			throw new CertificateException("Certificate trust cannot be confirmed. Validation result: " + result);
		}
	}
	
	private int validateHostname(String host, X509Certificate hostCert) {
		if (tempStore.containsMapping(host, hostCert))
			// we have an explicit confirmation for this certificate 
			return 0;
		try {
			verifier.check(host, hostCert);
			// no problem with the verification
			return 0;
		} catch (SSLException e) {
			// the hostname mismatches
			return UserInteraction.HOSTNAME_MISMATCH;
		}
	}

	private int validateCertificate(X509Certificate[] certChain) throws CertificateException {
    if (certChain == null || certChain.length == 0) {
      throw new IllegalArgumentException("Certificate chain is required"); //$NON-NLS-1$
    }
    if (tempStore.containsCertificate(certChain[0])) {
			// the certificate is already confirmed by the user
			return 0;
    }

		try {
			Certificate rootCert = null;

			KeyStore store = getKeyStore();
			for (int i = 0; i < certChain.length; i++) {
        certChain[i].checkValidity();
        if (i == certChain.length - 1) {
          // this is the last certificate in the chain
          X509Certificate cert = certChain[i];
          if (cert.getSubjectDN().equals(cert.getIssuerDN())) {
            certChain[i].verify(certChain[i].getPublicKey());
            rootCert = certChain[i]; // this is a self-signed certificate
          } else {
            // try to find a parent, we have an incomplete chain
            synchronized (store) {
              for (Enumeration e = store.aliases(); e.hasMoreElements();) {
                Certificate nextCert = store.getCertificate((String) e.nextElement());
                if (nextCert instanceof X509Certificate
                    && ((X509Certificate) nextCert).getSubjectDN().equals(cert.getIssuerDN())) {
                  cert.verify(nextCert.getPublicKey());
                  rootCert = nextCert;
                  break;
								}
							}
						}
					}
        } else {
          X509Certificate nextX509Cert = certChain[i + 1];
          certChain[i].verify(nextX509Cert.getPublicKey());
				}

				synchronized (store) {
					String alias = rootCert == null ? null : store.getCertificateAlias(rootCert);
					if (alias != null)
						// we have found the root certificate in the store = trusted
						return 0;
					else if (rootCert != certChain[i]) {
						// we still haven't found a root certificate, check that the current is trusted
						alias = store.getCertificateAlias(certChain[i]);
						if (alias != null)
							return 0;
					}
				}
			}
			// we did all checks, but none of the certificates were trusted
			return UserInteraction.CERTIFICATE_NOT_TRUSTED;
		} catch (CertificateExpiredException e) {
			e.printStackTrace();
			return UserInteraction.CERTIFICATE_EXPIRED;
		} catch (CertificateNotYetValidException e) {
			e.printStackTrace();
			return UserInteraction.CERTIFICATE_NOT_VALID_YET;
		} catch (GeneralSecurityException e) {
			CertificateException ce = new CertificateException("Certificate is not valid!");
			ce.initCause(e);
			throw ce;
		}
	}

	private KeyStore getKeyStore() {
		return keyStore;
	}

	public X509Certificate[] getAcceptedIssuers() {
		// ignore; used only for clients
		return null;
	}
	
	private static KeyStore getDefaultKeyStore() throws GeneralSecurityException {
		if (defaultStore == null) {
			try {
				defaultStore = KeyStore.getInstance(CACERTS_TYPE);
				// load the keystore with no password for read-only access
				defaultStore.load(new FileInputStream(CACERTS_PATH), null);
			} catch (IOException e) {
				GeneralSecurityException gse = new GeneralSecurityException("Unable to find JRE CA keystore");
				gse.initCause(e);
				throw gse;
			}
		}
		return defaultStore;
	}

}