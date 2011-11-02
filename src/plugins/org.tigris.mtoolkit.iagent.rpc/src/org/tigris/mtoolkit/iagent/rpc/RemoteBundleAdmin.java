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

public interface RemoteBundleAdmin {

	public static final int INCLUDE_BUNDLE_HEADERS = 0x01;
	public static final int INCLUDE_BUNDLE_STATES = 0x02;
	public static final int INCLUDE_REGISTERED_SERVICES = 0x04;
	public static final int INCLUDE_USED_SERVICES = 0x08;

	/**
	 * Key for bundle id. The value is of type Long.
	 */
	public static final String KEY_BUNDLE_ID = "bundle.id";

	/**
	 * Key for bundle headers. The value is of type Dictionary.
	 */
	public static final String KEY_BUNDLE_HEADERS = "bundle.headers";

	/**
	 * Key for bundle state. The value is of type Integer.
	 */
	public static final String KEY_BUNDLE_STATE = "bundle.state";

	/**
	 * Key for registered services. The value is of type Dictionary[].
	 */
	public static final String KEY_REGISTERED_SERVICES = "registered.services";

	/**
	 * Key for used services. The value is of type Dictionary[].
	 */
	public static final String KEY_USED_SERVICES = "used.services";

	long getBundleByLocation(String location);

	int getBundleState(long id);

	/**
	 * Checks if the bundle is signed.
	 * 
	 * @param id
	 *            the bundle id
	 * @return <code>true</code> if there are signer certificates.
	 */
	boolean isBundleSigned(long id);

	String getBundleLocation(long id);

	/**
	 * Returns bundle headers localized to given locale.
	 * 
	 * @param id
	 * @param locale
	 * @return null if the bundle is uninstalled, or Dictionary object with the
	 *         headers
	 */
	Dictionary getBundleHeaders(long id, String locale);

	/**
	 * Returns single bundle header localized to given locale
	 * 
	 * @param id
	 * @param headerName
	 * @param locale
	 * @return {@link Error} object containing description of the arised problem
	 *         or String object containing the value of the requested header
	 */
	Object getBundleHeader(long id, String headerName, String locale);

	/**
	 * Returns the last modified status of the bundle
	 * 
	 * @param id
	 * @return the result of the getLastModified() method call or -2 if the
	 *         bundle is uninstalled
	 */
	long getBundleLastModified(long id);

	/**
	 * Returns symbolic name of the bundle if available
	 * 
	 * @param id
	 * @return null in case the bundle has been uninstalled, empty string in
	 *         case the bundle is R3 and has no symbolic name or the symbolic
	 *         name of the bundle
	 */
	String getBundleSymbolicName(long id);

	/**
	 * Attempts to start the bundle
	 * 
	 * @param id
	 * @param flags
	 * @return Error object, describing the problem or null if the bundle has
	 *         been started successfully
	 */
	Object startBundle(long id, int flags);

	/**
	 * Attempts to start the bundle
	 * 
	 * @param id
	 * @param flags
	 * @return Error object, describing the problem or null if the bundle has
	 *         been stopped successfully
	 */
	Object stopBundle(long id, int flags);

	boolean resolveBundles(long[] ids);

	long[] listBundles();

	public Object getBundlesSnapshot(int includeOptions, Dictionary properties);

	/**
	 * Installs bundle from given input stream
	 * 
	 * @param location
	 * @param is
	 * @return Long object with the newly installed bundle id or Error object
	 *         describing the problem
	 */
	Object installBundle(String location, InputStream is);

	/**
	 * Uninstalls bundle
	 * 
	 * @param id
	 * @return null if successful or Error object describing the problem
	 */
	Object uninstallBundle(long id);

	long[] getBundles(String symbolicName, String version);

	/**
	 * Update a bundle from given InputStream
	 * 
	 * @param id
	 * @param is
	 * @return null if successful or Error object describing the problem
	 */
	Object updateBundle(long id, InputStream is);

	/**
	 * Returns the registered services by this bundle
	 * 
	 * @param id
	 * @return array of Dictionary describing the registered services. Empty
	 *         array is return in case the bundle is not active, or no services
	 *         are registered. Null result is returned if the bundle is
	 *         uninstalled
	 */
	Dictionary[] getRegisteredServices(long id);

	/**
	 * Returns the used services by this bundle
	 * 
	 * @param id
	 * @return array of Dictionary describing the used services. Empty array is
	 *         return in case the bundle is not active, or no services are in
	 *         use currently. Null result is returned if the bundle is
	 *         uninstalled
	 */
	Dictionary[] getUsingServices(long id);

	/**
	 * Returns the ids of the bundles which are fragments attached to the passed
	 * bundle
	 * 
	 * @param id
	 * @return array, containing the ids of the fragment bundles. Empty array is
	 *         returned in case the bundle hasn't any fragments or the
	 *         PackageAdmin service is unavailable. Null result is returned if
	 *         the bundle is uninstalled
	 */
	long[] getFragmentBundles(long id);

	/**
	 * Returns the ids of the bundles which are hosts to which this fragment is
	 * attached to the passed bundle
	 * 
	 * @param id
	 * @return array, containing the ids of the host bundles. Empty array is
	 *         returned in case the bundle hasn't any fragments or the
	 *         PackageAdmin service is unavailable. Null result is returned if
	 *         the bundle is uninstalled
	 */
	long[] getHostBundles(long id);

	/**
	 * Return the type of the bundle as return by PackageAdmin.getBundleType()
	 * 
	 * @param id
	 * @return the type of the bundle, -1 if the bundle is uninstalled, or -2 if
	 *         PackageAdmin is unavailable
	 */
	public int getBundleType(long id);

	public long getRemoteServiceID();

	public String[] getAgentData();

	public long[] getSystemBundlesIDs();

	public String[] getSystemBundlesNames();

	public int getBundleStartLevel(long id);

	public int getFrameworkStartLevel();

	public String getSystemProperty(String property);

	/**
	 * Returns resource with given name.
	 * 
	 * @param id
	 * @param name
	 *            the name of the resource
	 * @param properties
	 *            additional properties or null
	 * @return InputStream for getting the resource, null if there is no such
	 *         resource or Error object in case of error
	 */
	public Object getBundleResource(long id, String name, Dictionary properties);
}