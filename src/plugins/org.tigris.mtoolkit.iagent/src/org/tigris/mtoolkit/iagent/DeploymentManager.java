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
package org.tigris.mtoolkit.iagent;

import java.io.InputStream;

import org.tigris.mtoolkit.iagent.event.RemoteBundleEvent;
import org.tigris.mtoolkit.iagent.event.RemoteBundleListener;
import org.tigris.mtoolkit.iagent.event.RemoteDPEvent;
import org.tigris.mtoolkit.iagent.event.RemoteDPListener;

/**
 * Responsible for managing deployment units inside the OSGi Runtime. It
 * supports bundles and deployment packages. <br>
 * 
 * There two classes which represents single deployment unit:
 * {@link RemoteBundle} (in case of bundles) and {@link RemoteDP} in case of
 * deployment packages.<br>
 * 
 * @author Danail Nachev
 * 
 */
public interface DeploymentManager {

	/**
	 * Lists all currently installed deployment packages in the runtime.
	 * 
	 * @return array of RemoteDP objects. In case, when no DPs are installed,
	 *         empty array will be returned.
	 * @throws IAgentException
	 */
	public RemoteDP[] listDeploymentPackages() throws IAgentException;

	/**
	 * Returns the currently installed DP with given symbolic name. Returns null
	 * in case there is no installed DP with the given symbolic name.
	 * 
	 * @param symbolicName
	 *            the name of the DP to return. If the passed name is null,
	 *            {@link IllegalArgumentException} will be thrown
	 * @return RemoteDP instance representing the installed DP if any. Returns
	 *         null if there is no DP with the given name
	 * @throws IAgentException
	 * @throws {@link IllegalArgumentException} if the passed symbolicName is
	 *         null
	 */
	public RemoteDP getDeploymentPackage(String symbolicName) throws IAgentException;

	/**
	 * Installs the deployment package from the specified InputStream object. If
	 * the install is successful, a RemoteDP object is returned which can be
	 * used to manage and introspect the installed DP.<br>
	 * 
	 * If the installation fails, {@link IAgentException} will be thrown. Its
	 * code will be equal to the corresponding DeploymentException code.
	 * 
	 * @param is
	 *            InputStream with the deployment package's contents
	 * @return RemoteDP object representing the newly installed/updated
	 *         deployment package
	 * @throws IAgentException
	 */
	public RemoteDP installDeploymentPackage(InputStream is) throws IAgentException;

	/**
	 * Returns all installed bundles currently available in the runtime. The
	 * returned array contains RemoteBundle objects, which can be used to query
	 * various information and to manage the underlying bundle.
	 * 
	 * @return array with RemoteBundle objects. The result is never null
	 * @throws IAgentException
	 */
	public RemoteBundle[] listBundles() throws IAgentException;

	/**
	 * Returns bundles with given symbolic name and version. If the version is
	 * null, then all bundles with this symbolic name are returned. If no bundle
	 * exists with this symbolic name, null is returned.
	 * 
	 * @param symbolicName
	 *            the symbolic name of the bundle to return. If null is passed,
	 *            {@link IllegalArgumentException} is thrown.
	 * @param version
	 *            the version of the bundle. In case of null, all bundles with
	 *            given symbolic name are returned
	 * @return array of RemoteBundle objects. When there are no bundles to
	 *         return, null array is returned.
	 * @throws IAgentException
	 * @throws {@link IllegalArgumentException}
	 */
	public RemoteBundle[] getBundles(String symbolicName, String version) throws IAgentException;

	/**
	 * Installs the bundle from the passed {@link InputStream} object and
	 * returns RemoteBundle object representing the newly installed bundle. In
	 * case of failure, IAgentException is thrown.
	 * 
	 * @param location
	 *            the location to be assigned to the newly installed bundle
	 * @param is
	 *            InputStream object containing the bundle
	 * @return RemoteBundle object representing the newly installed bundle
	 * @throws IAgentException
	 */
	public RemoteBundle installBundle(String location, InputStream is) throws IAgentException;

	/**
	 * Add a listener which will be notified whenever bundle event is generated
	 * on the remote site. Adding the same listener twice doesn't have any
	 * effect
	 * 
	 * @param listener
	 *            the listener which will be notified for remote bundle events
	 * @throws IAgentException
	 * @see {@link RemoteBundleEvent}
	 */
	public void addRemoteBundleListener(RemoteBundleListener listener) throws IAgentException;

	/**
	 * Removes a listener from the listener list. This means that the listener
	 * won't be notified for remote bundle events anymore.
	 * 
	 * @param listener
	 *            the listener to be removed
	 * @throws IAgentException
	 *             if the remote OSGi framework is already disconnected
	 */
	public void removeRemoteBundleListener(RemoteBundleListener listener) throws IAgentException;

	/**
	 * Add a listener to be notified when deployment package event is generated
	 * on the remote site. Adding the same listener twice doesn't have any
	 * effect.
	 * 
	 * @param listener
	 *            the listener to be notified for deployment package events
	 * @throws IAgentException
	 * @see {@link RemoteDPEvent}
	 */
	public abstract void addRemoteDPListener(RemoteDPListener listener) throws IAgentException;

	/**
	 * Removes a listener from the deployment package events listener list. The
	 * listener won't be notified for events anymore.
	 * 
	 * @param listener
	 *            the listener to be removed
	 * @throws IAgentException
	 *             if the remote OSGi framework is already disconnected
	 */
	public void removeRemoteDPListener(RemoteDPListener listener) throws IAgentException;

	/**
	 * Returns string array with names of all System bundles.
	 * 
	 * @return A string array with symbolic names of all system bundles.
	 * 
	 * @throws IAgentException
	 */
	public String[] getSystemBundlesNames() throws IAgentException;

}
