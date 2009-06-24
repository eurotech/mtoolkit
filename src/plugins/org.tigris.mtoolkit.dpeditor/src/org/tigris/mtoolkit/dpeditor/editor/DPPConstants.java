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
package org.tigris.mtoolkit.dpeditor.editor;

public interface DPPConstants {

	/** corresponds to the "Manifest-Version" header */
	public static String dpManifestVersionHeader = "Manifest-Version: ";

	/** corresponds to the "DeploymentPackage-SymbolicName" header */
	public static String dpSymbolicNameHeader = "DeploymentPackage-SymbolicName: ";

	/** corresponds to the "DeploymentPackage-Version" header */
	public static String dpVersionHeader = "DeploymentPackage-Version: ";

	/** corresponds to the "DeploymentPackage-FixPack" header */
	public static String dpFixPackHeader = "DeploymentPackage-FixPack: ";

	/** corresponds to the "DeploymentPackage-Copyright" header */
	public static String dpCopyrightHeader = "DeploymentPackage-Copyright: ";

	/** corresponds to the "DeploymentPackage-ContactAddress" header */
	public static String dpAddressHeader = "DeploymentPackage-ContactAddress: ";

	/** corresponds to the "DeploymentPackage-Description" header */
	public static String dpDescriptionHeader = "DeploymentPackage-Description: ";

	/** corresponds to the "DeploymentPackage-DocURL" header */
	public static String dpDocURLHeader = "DeploymentPackage-DocURL: ";

	/** corresponds to the "DeploymentPackage-Vendor" header */
	public static String dpVendorHeader = "DeploymentPackage-Vendor: ";

	/** corresponds to the "DeploymentPackage-License" header */
	public static String dpLicenseHeader = "DeploymentPackage-License: ";

	/** corresponds to the "Name" header */
	public static String nameHeader = "Name: ";

	/** corresponds to the "Bundle-SymbolicName" header */
	public static String bundleNameHeader = "Bundle-SymbolicName: ";

	/** corresponds to the "Bundle-Version" header */
	public static String bundleVersionHeader = "Bundle-Version: ";

	/** corresponds to the "DeploymentPackage-Missing" header */
	public static String dpMissingHeader = "DeploymentPackage-Missing: ";

	/** corresponds to the "Resource-Processor" header */
	public static String resourceProcessorHeader = "Resource-Processor: ";

	/** corresponds to the "DeploymentPackage-Customizer" header */
	public static String dpCustomizerHeader = "DeploymentPackage-Customizer: ";

	/** corresponds to the "Bundle-Vendor" header */
	public static String bundleVendorHeader = "Bundle-Vendor: ";

	/** corresponds to the "Bundle-DocURL" header */
	public static String bundleDocUrlHeader = "Bundle-DocURL: ";

	/** corresponds to the "Bundle-ContactAddress" header */
	public static String bundleAddressHeader = "Bundle-ContactAddress: ";

	/** corresponds to the "Bundle-Description" header */
	public static String bundleDescriptionHeader = "Bundle-Description: ";

	/** corresponds to the "Bundle-Activator" header */
	public static String bundleActivatorHeader = "Bundle-Activator: ";

	/** corresponds to the "Bundle-UpdateLocation" header */
	public static String bundleUpdateLocationHeader = "Bundle-UpdateLocation: ";

	/** corresponds to the "Bundle-Category" header */
	public static String bundleCategoryHeader = "Bundle-Category: ";

	/** corresponds to the "Bundle-ClassPath" header */
	public static String classPathHeader = "Bundle-ClassPath: ";

	/** corresponds to the "Bundle-ExportPackage" header */
	public static String exportPackHeader = "Export-Package: ";

	/** corresponds to the "Bundle-ImportPackage" header */
	public static String importPackHeader = "Import-Package: ";

	/** corresponds to the "Bundle-ExportService" header */
	public static String exportServicesHeader = "Export-Service: ";

	/** corresponds to the "Bundle-ImportService" header */
	public static String importServicesHeader = "Import-Service: ";

	/** corresponds to the "DynamicImport-Package" header */
	public static String dynamicImportsHeader = "DynamicImport-Package: ";

	/** corresponds to the "Bundle-RequiredExecutionEnvironment" header */
	public static String bundleExecEnvHeader = "Bundle-RequiredExecutionEnvironment: ";

	/** corresponds to the "Bundle-NativeCode" header */
	public static String nativeClausesHeader = "Bundle-NativeCode: ";
	public static String keyCreatedHeader = "Created-By: ";
	public static String keyCopyrightHeader = "Bundle-Copyright: ";
	public static String configHeader = "Config: ";
	public static String factoryConfigHeader = "FactoryConfig: ";

	/** corresponds to the "Bundle-Requirements" header */
	public static String bundleReqHeader = "Bundle-Requirements: ";

	/**
	 * parameter attribute in the "Bundle-ImportPackage" and
	 * "Bundle-ExportPackage" header
	 */
	public static final String PACKAGE_VERSION = "specification-version";

	/** parameter attribute in the "Bundle-NativeCode" header */
	public static final String PROCESSOR_VERSION = "processor";
	public static final String OS_NAME = "osname";
	public static final String OS_VERSION = "osversion";
	public static final String LANGUAGE_NAME = "language";
	public static final int NOT_RECOGNIZED_STATE = -1;
	public static final int LINE_CONTINUED_STATE = 0;

	public static final int DP_SYMBOLIC_NAME_STATE = 1;
	public static final int DP_VERSION_STATE = 2;
	public static final int DP_FIX_PACK_STATE = 3;
	public static final int DP_COPYRIGHT_STATE = 4;
	public static final int DP_ADDRESS_STATE = 5;
	public static final int DP_DESCRIPTION_STATE = 6;
	public static final int DP_DOCURL_STATE = 7;
	public static final int DP_VENDOR_STATE = 8;
	public static final int DP_LICENSE_STATE = 9;
	public static final int NAME_BUNDLE_STATE = 10;
	public static final int BUNDLE_SYMBOLIC_NAME_STATE = 11;
	public static final int BUNDLE_VERSION_STATE = 12;
	public static final int DP_MISSING_STATE = 13;
	public static final int RESOURCE_PROCESSOR_STATE = 14;
	public static final int DP_CUSTOMIZER_STATE = 15;
	public static final int CUSTOM_STATE = 16;
	public static final int RESOURCE_PATH_STATE = 17;
	public static final int RESOURCE_DP_MISSING_STATE = 18;
	public static final int RES_RESOURCE_PROCESSOR_STATE = 19;
	public static final int RESOURCE_CUSTOM_STATE = 20;

	/** constant is used to get system property for line separator */
	public static final String LINE_SEPARATOR = System.getProperty("line.separator", "\r\n");
	/** deployment package name folder (must be left-wrapped - ex: "dppfolder/") */
	public static final String DP_FOLDER = ".";
	/** extension for deployment package file */
	public static final String DP_INFO_ARCHIVE_EXTENSION = "dpp";
	/** extension for deployment package file */
	public static final String DP_INFO_ARCHIVE_FILE_EXTENSION = ".dpp";
}
