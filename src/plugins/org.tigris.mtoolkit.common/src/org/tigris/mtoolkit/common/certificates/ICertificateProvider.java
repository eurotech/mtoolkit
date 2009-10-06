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
package org.tigris.mtoolkit.common.certificates;

public interface ICertificateProvider {

  /**
   * Returns array with available certificates or empty array if no 
   * certificates were set.
   * @return the array with certificates
   */
  public ICertificateDescriptor[] getCertificates();

  /**
   * Returns certificate with given uid or null if no certificate was found.
   * @param uid the uid that specifies the certificate
   * @return the certificate or null
   */
  public ICertificateDescriptor getCertificate(String uid);
}
