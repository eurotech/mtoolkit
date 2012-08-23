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

  public static String MessageDialog_NoDetails;
  public static String PasswordDialog_lblPassword;
  public static String plugin_exporter_not_compatible;
  public static String CertificatesPanel_signContentGroup;
  public static String CertificatesPanel_lblCertificates;
  public static String CertificatesPanel_tblCertColAlias;
  public static String CertificatesPanel_tblCertColLocation;
  public static String CertificatesPanel_lblNoCertificates;
  public static String install_to_menu_label;
  public static String AbstractInstallationItemProcessor_Preparing_Items;

  public static String certs_ColAlias;
  public static String certs_ColLocation;
  public static String certs_btnAdd;
  public static String certs_btnEdit;
  public static String certs_btnRemove;
  public static String certs_lblJarsignerLocation;

  public static String dlgCertMan_titleAdd;
  public static String dlgCertMan_titleEdit;
  public static String dlgCertMan_descr;
  public static String dlgCertMan_message_add;
  public static String dlgCertMan_message_edit;
  public static String dlgCertMan_labelAlias;
  public static String dlgCertMan_labelLocation;
  public static String dlgCertMan_labelType;
  public static String dlgCertMan_labelPass;
  public static String dlgCertMan_labelKeyPass;
  public static String dlgCertMan_verifyAliasEmpty;
  public static String dlgCertMan_verifyLocationEmpty;
  public static String dlgCertMan_verifyLocationNotExist;
  public static String dlgCertMan_browseDlgCaption;

  public static String browseLabel;

  static {
    reloadMessages();
  }

  public static void reloadMessages() {
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

}
