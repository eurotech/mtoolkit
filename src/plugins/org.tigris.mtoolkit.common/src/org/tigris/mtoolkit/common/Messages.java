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
package org.tigris.mtoolkit.common;

import org.eclipse.osgi.util.NLS;

/**
 *
 */
public class Messages {
  private static final String BUNDLE_NAME = "org.tigris.mtoolkit.common.messages";

  public static String        MessageDialog_NoDetails;
  public static String        PasswordDialog_lblPassword;
  public static String        plugin_exporter_not_compatible;
  public static String        CertificatesPanel_signContentGroup;
  public static String        CertificatesPanel_lblCertificates;
  public static String        CertificatesPanel_tblCertColAlias;
  public static String        CertificatesPanel_tblCertColLocation;
  public static String        CertificatesPanel_tblCertColIssuedTo;
  public static String        CertificatesPanel_tblCertColIssuedBy;
  public static String        CertificatesPanel_tblCertColExpirationDate;
  public static String        CertificatesPanel_lblDetails;
  public static String        CertificatesPanel_lblNoCertificates;
  public static String        CertificatesPanel_lblKeystoreMissing;
  public static String        CertificatesPanel_lblErrorData;
  public static String        install_to_menu_label;
  public static String        ManifestUtils_Invalid_JAR_Manifest;
  public static String        AbstractInstallationItemProcessor_Preparing_Items;
  public static String        CertificateDetails_Not_X509Certificate;
  public static String        CertUtils_NotNullAlias;
  public static String        CertUtils_NotNullLocation;

  public static String        CommonPreferencePage_WrongExtenstionClass;

  public static String        certs_ColAlias;
  public static String        certs_ColLocation;
  public static String        certs_btnAdd;
  public static String        certs_btnEdit;
  public static String        certs_btnRemove;
  public static String        cert_btnDetails;
  public static String        certs_lblJarsignerLocation;

  public static String        dlgCertMan_titleAdd;
  public static String        dlgCertMan_titleEdit;
  public static String        dlgCertMan_descr;
  public static String        dlgCertMan_message_add;
  public static String        dlgCertMan_message_edit;
  public static String        dlgCertMan_labelAlias;
  public static String        dlgCertMan_labelLocation;
  public static String        dlgCertMan_labelType;
  public static String        dlgCertMan_labelPass;
  public static String        dlgCertMan_labelKeyPass;
  public static String        dlgCertMan_verifyAliasEmpty;
  public static String        dlgCertMan_verifyLocationEmpty;
  public static String        dlgCertMan_verifyLocationNotExist;
  public static String        dlgCertMan_verifyAliasExist;
  public static String        dlgCertMan_verifyUnknownAlias;
  public static String        dlgCertMan_browseDlgCaption;
  public static String        dlgCertMan_labelCertDetails;

  public static String        dlgCertViewer_title;
  public static String        dlgCertViewer_labelAlias;
  public static String        dlgCertViewer_labelLocation;

  public static String        browseLabel;

  public static String        OSGiBundleWizard_title;
  public static String        OSGiBundleWizard_MainPage_title;
  public static String        OSGiBundleWizard_MainPage_desc;

  static {
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }
}
