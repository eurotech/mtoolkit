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
package org.tigris.mtoolkit.common.gui;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * Certificate viewer, which mimics the way Firefox 3.5 shows the certificate
 * information. Only part of the properties of a given certificate is shown.
 *
 */
public final class X509CertificateViewer {
  private Composite       control;
  private Label           issuedToCNTxt;
  private Label           issuedToOTxt;
  private Label           issuedToOUTxt;
  private Label           issuedToSNTxt;
  private Label           issuedByCNTxt;
  private Label           issuedByOTxt;
  private Label           issuedByOUTxt;
  private Label           issuedOnTxt;
  private Label           expiresOnTxt;
  private Label           sha1FingerprintTxt;
  private Label           md5FingerprintTxt;

  private X509Certificate certificate;
  ScrolledComposite       scrolled;

  public X509CertificateViewer(Composite parent, int style) {
    scrolled = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);

    // always show the focus control
    scrolled.setShowFocusedControl(true);
    scrolled.setExpandHorizontal(true);
    scrolled.setExpandVertical(true);

    GridData scrolledData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
    scrolled.setLayoutData(scrolledData);

    control = new Composite(scrolled, SWT.NONE);
    control.setLayout(new GridLayout(2, false));
    control.setLayoutData(scrolledData);

    createSectionLabel("Issued To");
    issuedToCNTxt = createPanelEntry("Common Name (CN)");
    issuedToOTxt = createPanelEntry("Organization (O)");
    issuedToOUTxt = createPanelEntry("Organizational Unit (OU)");
    issuedToSNTxt = createPanelEntry("Serial Number");
    createSectionLabel("Issued By");
    issuedByCNTxt = createPanelEntry("Common Name (CN)");
    issuedByOTxt = createPanelEntry("Organization (O)");
    issuedByOUTxt = createPanelEntry("Organizational Unit (OU)");
    createSectionLabel("Validity");
    issuedOnTxt = createPanelEntry("Issued On");
    expiresOnTxt = createPanelEntry("Expires On");
    createSectionLabel("Fingerprints");
    sha1FingerprintTxt = createPanelEntry("SHA1 Fingerprint");
    md5FingerprintTxt = createPanelEntry("MD5 Fingerprint");
    scrolled.setContent(control);
  }

  public Control getControl() {
    return scrolled;
  }

  public void setCertificate(X509Certificate certificate) {
    this.certificate = certificate;
    refresh();
  }

  private Control createSectionLabel(String text) {
    Label sectionLabel = new Label(control, SWT.WRAP);
    sectionLabel.setText(text);
    sectionLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
    sectionLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    return sectionLabel;
  }

  private Label createPanelEntry(String label) {
    Label panelEntryLabel = new Label(control, SWT.NONE);
    panelEntryLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    panelEntryLabel.setText(label);

    Label panelEntryText = new Label(control, SWT.NONE);
    panelEntryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    return panelEntryText;
  }

  private void refresh() {
    List issuedToRDN = parseDN(certificate.getSubjectX500Principal().getName());
    issuedToCNTxt.setText(safeFindPart("CN", issuedToRDN));
    issuedToOTxt.setText(safeFindPart("O", issuedToRDN));
    issuedToOUTxt.setText(safeFindPart("OU", issuedToRDN));
    issuedToSNTxt.setText(certificate.getSerialNumber().toString());

    List issuedByRDN = parseDN(certificate.getIssuerX500Principal().getName());
    issuedByCNTxt.setText(safeFindPart("CN", issuedByRDN));
    issuedByOTxt.setText(safeFindPart("O", issuedByRDN));
    issuedByOUTxt.setText(safeFindPart("OU", issuedByRDN));

    final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
    issuedOnTxt.setText(dateFormat.format(certificate.getNotBefore()));
    expiresOnTxt.setText(dateFormat.format(certificate.getNotAfter()));

    try {
      md5FingerprintTxt.setText(formatByteArray(digest(certificate.getEncoded(), "MD5")));
    } catch (CertificateEncodingException e) {
      md5FingerprintTxt.setText(e.toString());
    }
    try {
      sha1FingerprintTxt.setText(formatByteArray(digest(certificate.getEncoded(), "SHA-1")));
    } catch (CertificateEncodingException e) {
      sha1FingerprintTxt.setText(e.toString());
    }
    control.pack();
    scrolled.setMinSize(control.getSize());
  }

  private static byte[] digest(byte[] stream, String alg) {
    try {
      MessageDigest md = MessageDigest.getInstance(alg);
      return md.digest(stream);
    } catch (NoSuchAlgorithmException e) {
      return null;
    }
  }

  private static String formatByteArray(byte[] input) {
    if (input == null || input.length == 0) {
      return "None";
    }
    StringBuffer buf = new StringBuffer(input.length * 2 /* for each byte */
        + input.length /* for the separators */);
    for (int i = 0; i < input.length; i++) {
      String c = Integer.toHexString(input[i] & 0xFF).toUpperCase();
      if (c.length() == 1) {
        buf.append('0');
      }
      buf.append(c).append(':');
    }
    buf.deleteCharAt(buf.length() - 1);
    return buf.toString();
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
    return "<Unknown>";
  }
}
