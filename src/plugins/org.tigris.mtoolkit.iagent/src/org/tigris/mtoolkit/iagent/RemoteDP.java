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

import java.util.Dictionary;


/**
 * Represents remote deployment package, installed in the remote OSGi
 * runtime. It can be used to query some of the deployment package metadata, its
 * state and perform uninstall (either normal or forced).
 * 
 * @author Danail Nachev
 * 
 */
public interface RemoteDP {

  /**
   * Returns the symbolic name of the deployment package
   * 
   * @return the symbolic name of the deployment package
   * @throws IAgentException
   */
  public String getName() throws IAgentException;

  /**
   * Returns the version of the deployment package
   * 
   * @return the version of the deployment package
   * @throws IAgentException
   */
  public String getVersion() throws IAgentException;

  /**
   * Returns the deployment package manifest value for the specified header.
   * Only the headers from the main section of the manifest are available. If
   * the header is not specified in the DP's manifest, null will be returned. If
   * the passed header name is null, {@link IllegalArgumentException} will be
   * thrown.
   * 
   * @return value of the given header if available or null
   * @throws IAgentException
   * @throws {@link IllegalStateException}
   *             if the deployment package has been removed
   */
  public String getHeader(String header) throws IAgentException;

  /**
   * Returns the symbolic names and versions of the bundles which belongs to the
   * deployment package.
   * 
   * @return Dictionary object where keys are the symbolic names of the bundles,
   *         and values are the versions.
   * @throws IAgentException
   * @throws {@link IllegalStateException}
   *             if the deployment package has been removed
   */
  public Dictionary getBundles() throws IAgentException;

  /**
   * Returns the bundle which has the specified symbolic name and belongs to the
   * deployment package. If the bundle isn't available in the runtime, null is
   * returned.
   * 
   * @param symbolicName
   *            the symbolic name of the bundle
   * @return RemoteBundle object
   * @throws IAgentException
   * @throws {@link IllegalArgumentException}
   *             if the deployment package has been removed
   */
  public RemoteBundle getBundle(String symbolicName) throws IAgentException;

  /**
   * Uninstalls the deployment package. Depending on the parameter passed, the
   * uninstallation can be forced
   * 
   * @param force
   *            true, if the uninstallation should be forced, false otherwise
   * @return whether the operation was successful or not. If the underlying OSGi
   *         throws DeploymentException, its code will be returned as code for
   *         the thrown IAgentException. If no DeploymentException is thrown,
   *         then the result of the method will indicate the success of the
   *         operation
   * @throws IAgentException
   * @throws {@link IllegalStateException}
   *             if the the underlying deployment package is already removed
   */
  public boolean uninstall(boolean force) throws IAgentException;

  /**
   * Returns whether the underlying deployment package is available or it has
   * been uninstalled. If this method returns true, then other methods will
   * throw {@link IllegalStateException}
   * 
   * @return true in case the deployment package is not available any more,
   *         false otherwise
   * @throws IAgentException
   */
  public boolean isStale() throws IAgentException;
  
}
