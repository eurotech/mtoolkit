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
package org.tigris.mtoolkit.common.installation;

public final class InstallationConstants {
  //Installation item processor properties/arguments
  public static final String AUTO_START_ITEMS    = "auto_start_items";          //$NON-NLS-1$
  public static final String AUTO_UPDATE_ITEMS   = "auto_update_items";         //$NON-NLS-1$

  // Signing contents
  public static final String CERT_ALIAS          = "certificate-alias";         //$NON-NLS-1$
  public static final String CERT_STORE_LOCATION = "certificate-store-location"; //$NON-NLS-1$
  public static final String CERT_STORE_TYPE     = "certificate-store-type";    //$NON-NLS-1$
  public static final String CERT_STORE_PASS     = "certificate-store-pass";    //$NON-NLS-1$
  public static final String CERT_KEY_PASS       = "certificate-key-pass";      //$NON-NLS-1$

  private InstallationConstants() {
  }
}
