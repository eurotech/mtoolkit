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
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

public class InteractiveHostnameVerifier implements HostnameVerifier {
  private static org.apache.commons.ssl.HostnameVerifier verifier = org.apache.commons.ssl.HostnameVerifier.DEFAULT_AND_LOCALHOST;

  private final UserInteraction                          interaction;
  private final TemporaryStore                           tempStore;

  public InteractiveHostnameVerifier(UserInteraction interaction, TemporaryStore tempStore) {
    if (interaction == null) {
      throw new NullPointerException("User interaction cannot be null");
    }
    this.interaction = interaction;
    this.tempStore = tempStore;
  }

  /* (non-Javadoc)
   * @see javax.net.ssl.HostnameVerifier#verify(java.lang.String, javax.net.ssl.SSLSession)
   */
  public boolean verify(String host, SSLSession session) {
    Certificate[] certChain = null;
    try {
      certChain = session.getPeerCertificates();
      if (certChain != null && certChain.length > 0) {
        verifier.check(host, (X509Certificate) certChain[0]);
      }
    } catch (SSLPeerUnverifiedException e) {
      return interaction.confirmConnectionTrust(UserInteraction.VERIFICATION_FAILED, e.getMessage(), host,
          (X509Certificate[]) certChain);
    } catch (SSLException e) {
      // the hostname check failed
      // check whether the mapping is saved
      if (certChain != null && tempStore.containsMapping(host, certChain[0])) {
        return true;
      }
      // ask the user what to do
      final boolean userConfirmed = interaction.confirmConnectionTrust(UserInteraction.HOSTNAME_MISMATCH,
          e.getMessage(), host, (X509Certificate[]) certChain);
      if (certChain != null && userConfirmed) {
        // the user confirmed the connection, store the exception
        tempStore.addMapping(host, certChain[0]);
      }
      return userConfirmed;
    }
    // no exception, everything is ok
    return true;
  }
}
