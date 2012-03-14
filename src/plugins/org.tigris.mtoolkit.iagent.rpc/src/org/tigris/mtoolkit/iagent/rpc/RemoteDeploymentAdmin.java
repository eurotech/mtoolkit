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
package org.tigris.mtoolkit.iagent.rpc;

import java.io.InputStream;
import java.util.Dictionary;

import org.osgi.framework.BundleContext;

public interface RemoteDeploymentAdmin extends RemoteServiceIDProvider {

	void unregister(BundleContext bc);

	Dictionary listDeploymentPackages();

	Object getDeploymentPackageHeader(String name, String version, String headerName);

	Dictionary getDeploymentPackageBundles(String name, String version);

	// returns bid or -1 if the bundle is missing
	long getDeploymentPackageBundle(String dpName, String version, String symbolicName);

	Object uninstallDeploymentPackage(String dpName, String version, boolean force);

	boolean isDeploymentPackageStale(String dpName, String version);

	String getDeploymentPackageVersion(String dpName);

	Object installDeploymentPackage(InputStream in);

	/**
	 * Returns deployment package's icon.
	 * 
	 * @param dpName
	 * @param version
	 * @param properties
	 *            additional properties or null
	 * @return InputStream for getting the icon, null if there is no icon or
	 *         Error object in case of error
	 */
	public Object getDeploymentPackageIcon(String dpName, String version, Dictionary properties);

}