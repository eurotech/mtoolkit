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
import java.util.Dictionary;

/**
 * Represents remote bundle, installed in the remote OSGi runtime. It enables
 * client to query some of the metadata associated with the bundle, check it
 * state and perform the standard operations - install, uninstall, start, stop
 * and resolve.
 * 
 * @author Danail Nachev
 * 
 */
public interface RemoteBundle {

	/**
	 * State indicating that the underlying bundle is uninstalled
	 */
	public final static int UNINSTALLED = 1;

	/**
	 * Bundle type indicating that the underlying bundle is fragment
	 */
	public final static int BUNDLE_TYPE_FRAGMENT = 1;

	/**
	 * Returns the id of the underlying bundle
	 * 
	 * @return the id of the bundle
	 */
	public long getBundleId();

	/**
	 * Returns the symbolic name of the underlying bundle or null if the bundle
	 * doesn't follow R4 specification
	 * 
	 * @return the symbolic name of the bundle
	 * @throws IAgentException
	 */
	public String getSymbolicName() throws IAgentException;

	/**
	 * Returns the version of the bundle found in the Bundle-Version header or
	 * null if the header is missing.
	 * 
	 * @return the Bundle-Version header value
	 * @throws IAgentException
	 */
	public String getVersion() throws IAgentException;

	/**
	 * Returns Manifest headers and corresponding values. If the underlying
	 * bundle has been uninstalled its manifest may not be available. In this
	 * case, the method will return null.<br>
	 * 
	 * The header values will be localized according to the passed locale code.
	 * If it is null, then the manifest will be returned localized to the
	 * current remote locale. If the passed String is empty, then no
	 * localization will be performed and the raw values will be returned.
	 * 
	 * @param locale
	 *            the locale code into which the values will be localized
	 * @return Dictionary object containing the manifest headers and values of
	 *         the underlying bundle
	 * @throws IAgentException
	 */
	public Dictionary getHeaders(String locale) throws IAgentException;

	/**
	 * Returns localized value for single headerName. The method behaves exactly
	 * as {@link #getHeaders(String)} method. The call to this method is
	 * equivalent to: <br>
	 * <code>
	 *    getHeaders(locale).get(headerName);
	 * </code>
	 * 
	 * @param headerName
	 *            the name of the header to return
	 * @param locale
	 *            the locale code into which the value should be localized
	 * @return the localized (if requested) value of the requested header
	 * @throws IAgentException
	 */
	public String getHeader(String headerName, String locale) throws IAgentException;

	/**
	 * Returns if the underlying bundle is signed.
	 * 
	 * @return the signed state of the remote bundle
	 * @throws IAgentException
	 */
	public boolean isBundleSigned() throws IAgentException;

	/**
	 * Returns the location of the bundle
	 * 
	 * @return the location of the remote bundle
	 * @throws IAgentException
	 */
	public String getLocation() throws IAgentException;

	/**
	 * Returns the state of the underlying bundle.
	 * 
	 * @return the state of the remote bundle
	 * @throws IAgentException
	 */
	public int getState() throws IAgentException;

	/**
	 * Starts the bundle. The method will block until the bundle is in ACTIVE
	 * state or an exception is thrown indicating that the bundle cannot be
	 * started.
	 * 
	 * @param flags
	 *            options passed to the underlying framework. The supported
	 *            options are framework implementation specific. Value "0" means
	 *            that the standard start function to be called.
	 * @throws IAgentException
	 */
	public void start(int flags) throws IAgentException;

	/**
	 * Stops the bundle
	 * 
	 * @param flags
	 *            options passed to the underlying framework. The supported
	 *            options are framework implementation specific. Value "0" means
	 *            that the standard behaviour will be observed.
	 * @throws IAgentException
	 */
	public void stop(int flags) throws IAgentException;

	/**
	 * Update the bundle from the passed InputStream object. The method will
	 * always close the stream even in case of failure.
	 * 
	 * @param in
	 * @throws IAgentException
	 */
	public void update(InputStream in) throws IAgentException;

	/**
	 * Uninstalls the bundle from the runtime
	 * 
	 * @throws IAgentException
	 */
	public void uninstall() throws IAgentException;

	/**
	 * Try to resolve the bundle. The method returns true if the bundle is in
	 * resolved state. Returns false if the bundle cannot be resolved.
	 * 
	 * @return true in case of successful resolving, false when the bundle
	 *         cannot be resolved
	 * @throws IAgentException
	 */
	public boolean resolve() throws IAgentException;

	/**
	 * Returns information for all services registered by this bundle. Returns
	 * empty array if no services are registered.
	 * 
	 * @return
	 * @throws IAgentException
	 */
	public RemoteService[] getRegisteredServices() throws IAgentException;

	/**
	 * Returns information for all services, which are used by this bundle.
	 * Returns empty array if no services are known to be used. A service is
	 * considered to be used, if its usage count is greater than zero.
	 * 
	 * @return
	 * @throws IAgentException
	 */
	public RemoteService[] getServicesInUse() throws IAgentException;

	/**
	 * Returns the type of the bundle. The result is a mask of one of the
	 * following values:
	 * <ul>
	 * <li>{@link #BUNDLE_TYPE_FRAGMENT}</li>
	 * </ul>
	 * 
	 * If none of the above values is valid for this bundle, then result 0
	 * (zero) will be returned
	 * 
	 * @return
	 * @throws IAgentException
	 */
	public int getType() throws IAgentException;

	/**
	 * Returns RemoteBundle objects representing the fragments attached to this
	 * bundle or null if the bundle has no attached fragments or it isn't
	 * fragment
	 * 
	 * @return
	 * @throws IAgentException
	 */
	public RemoteBundle[] getFragments() throws IAgentException;

	/**
	 * Returns RemoteBundle object in array, representing the host to which the
	 * fragment is attached or null if the bundle is not a fragment. Currently,
	 * a fragment can be attached to only one host.
	 * 
	 * @return
	 * @throws IAgentException
	 */
	public RemoteBundle[] getHosts() throws IAgentException;

	/**
	 * Returns a timestamp of the last modification of the bundle.
	 * <p>
	 * The timestamp is a number, which can be used to track any changes to the
	 * bundle. This number is changed, whenever the bundle is updated.
	 * </p>
	 * 
	 * @return the last modified timestamp of the bundle
	 * @throws IAgentException
	 */
	public long getLastModified() throws IAgentException;

	/**
	 * Returns the start level of the remote bundle.
	 * 
	 * @return start level of the bundle
	 * @throws IAgentException
	 */
	public int getBundleStartLevel() throws IAgentException;

	/**
	 * Returns resource from the remote bundle.
	 * 
	 * @param name
	 *            the name of the resource
	 * 
	 * @return InputStream for reading the resource or null if there is no such
	 *         resource
	 * @throws IAgentException
	 * @since 4.0
	 */
	public InputStream getResource(String name) throws IAgentException;
}
