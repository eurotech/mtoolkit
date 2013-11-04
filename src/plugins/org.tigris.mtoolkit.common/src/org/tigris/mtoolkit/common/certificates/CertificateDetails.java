/*******************************************************************************
 * Copyright (c) 2005, 2013 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.common.certificates;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.tigris.mtoolkit.common.Messages;

public class CertificateDetails {
  private static final String    UNKNOWN         = "<Unknown>";
  private ICertificateDescriptor certDescriptor;

  private X509Certificate        certificate = null;
  private Throwable              exception;
  private String          issuedTo;
  private String          issuedBy;
  private String          expDate;

  public CertificateDetails(ICertificateDescriptor descr) {
    if (descr == null) {
      throw new NullPointerException("descr");
    }
    this.certDescriptor = descr;
    refresh();
  }

  /**
   * Returns the certificate descriptor.
   *
   * @return the certificate descriptor, never null.
   */
  public ICertificateDescriptor getCertificateDescriptor() {
    return certDescriptor;
  }

  public String getIssuedTo() {
    return issuedTo;
  }

  public String getIssuedBy() {
    return issuedBy;
  }

  public String getExpirationData() {
    return expDate;
  }

  public Throwable getError() {
    return exception;
  }

  public X509Certificate getX509Certificate() {
    return certificate;
  }

  private void refresh() {
    try {
      exception = null;
      Certificate readCertificate = CertUtils.readCertificate(certDescriptor.getStoreLocation(),
          certDescriptor.getAlias());
      if (readCertificate instanceof X509Certificate) {
        certificate = (X509Certificate) readCertificate;
        if (certificate != null) {
          List issuedToRDN = parseDN(certificate.getSubjectX500Principal().getName());
          issuedTo = safeFindPart("CN", issuedToRDN);

          List issuedByRDN = parseDN(certificate.getIssuerX500Principal().getName());
          issuedBy = safeFindPart("CN", issuedByRDN);

          DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
          expDate = dateFormat.format(certificate.getNotAfter());
        }
      } else {
        throw new Exception(Messages.CertificateDetails_Not_X509Certificate);
      }
    } catch (Exception e) {
      exception = e;
      certificate = null;
    }
  }

  /*
   * Taken initially from org.eclipse.osgi.internal.signedcontent.DNChainMatching
   */
  private static List parseDN(String dn) throws IllegalArgumentException {
    List rdnArray = new ArrayList(7);
    int dnLen = dn.length();
    char c = '\0';
    List nameValues = new ArrayList(1);
    int startIndex = 0;
    int endIndex;
    while (startIndex < dnLen) {
      for (endIndex = startIndex; endIndex < dnLen; endIndex++) {
        c = dn.charAt(endIndex);
        if (c == ',' || c == '+') {
          break;
        }
        if (c == '\\') {
          endIndex++; // skip the escaped char
        }
      }
      if (endIndex > dnLen) {
        throw new IllegalArgumentException();
      }
      if (nameValues != null) {
        nameValues.add(dn.substring(startIndex, endIndex));
      }
      if (c != '+') {
        rdnArray.add(nameValues);
        if (endIndex != dnLen) {
          nameValues = new ArrayList(1);
        } else {
          nameValues = null;
        }
      }
      startIndex = endIndex + 1;
    }
    if (nameValues != null) {
      throw new IllegalArgumentException();
    }
    return rdnArray;
  }

  private static String findPart(String partName, List rdnArray) {
    String searchStr = partName + '=';

    for (Iterator i = rdnArray.iterator(); i.hasNext();) {
      List nameList = (List) i.next();
      String part = (String) nameList.get(0);

      if (part.startsWith(searchStr)) {
        return part.toString().substring(searchStr.length());
      }
    }
    return null;
  }

  private static String safeFindPart(String partName, List rdnArray) {
    String part = findPart(partName, rdnArray);
    if (part != null && part.length() > 0) {
      return part;
    }
    return UNKNOWN;
  }
}
