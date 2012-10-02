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
package org.tigris.mtoolkit.util;

/**
 * This class contains several constants for using the the Deployment Package
 * Editor API .
 *
 * @author Antonia Avramova
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface DPPConstants {
  /** corresponds to the "DeploymentPackage-SymbolicName" header */
  public static String dpSymbolicNameHeader = "DeploymentPackage-SymbolicName";

  /** corresponds to the "DeploymentPackage-Version" header */
  public static String dpVersionHeader      = "DeploymentPackage-Version";

  /** corresponds to the "DeploymentPackage-FixPack" header */
  public static String dpFixPackHeader      = "DeploymentPackage-FixPack";

  /** corresponds to the "DeploymentPackage-Copyright" header */
  public static String dpCopyrightHeader    = "DeploymentPackage-Copyright";

  /** corresponds to the "DeploymentPackage-ContactAddress" header */
  public static String dpAddressHeader      = "DeploymentPackage-ContactAddress";

  /** corresponds to the "DeploymentPackage-Description" header */
  public static String dpDescriptionHeader  = "DeploymentPackage-Description";

  /** corresponds to the "DeploymentPackage-DocURL" header */
  public static String dpDocURLHeader       = "DeploymentPackage-DocURL";

  /** corresponds to the "DeploymentPackage-Vendor" header */
  public static String dpVendorHeader       = "DeploymentPackage-Vendor";

  /** corresponds to the "DeploymentPackage-License" header */
  public static String dpLicenseHeader      = "DeploymentPackage-License";

  /** corresponds to the "DeploymentPackage-Icon" header */
  public static String dpIcon               = "DeploymentPackage-Icon";

  /** corresponds to the "DeploymentPackage-Customizer" header */
  public static String dpCustomizerHeader   = "DeploymentPackage-Customizer: ";

  /** corresponds to the "Name" header */
  public static String nameHeader           = "Name: ";

  /** corresponds to the "Bundle-SymbolicName" header */
  public static String bundleNameHeader     = "Bundle-SymbolicName: ";

  /** corresponds to the "Bundle-Version" header */
  public static String bundleVersionHeader  = "Bundle-Version: ";

  /** corresponds to the "DeploymentPackage-Missing" header */
  public static String dpMissingHeader      = "DeploymentPackage-Missing: ";

  /**
   * corresponds to the "DeploymentPackage-Name" header
   *
   * @since 1.2
   */
  public static String dpName               = "DeploymentPackage-Name";

  /**
   * corresponds to the "DeploymentPackage-RequiredStorage" header
   *
   * @since 1.2
   */
  public static String dpRequiredStorage    = "DeploymentPackage-RequiredStorage";

}
