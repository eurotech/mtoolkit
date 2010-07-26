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

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

/**
 * Providers of {@link InstallationItem} should implement this interface. It
 * provides base methods for getting installation items and determing whether
 * the implementation is capable of handling given resource.
 */
public interface InstallationItemProvider {
	/**
	 * Determines if this provider can use the passed resource as input.
	 * 
	 * @param resource
	 *            the resource
	 * @return true if this provider can handle the resource, false otherwise
	 */
	public boolean isCapable(Object resource);

	/**
	 * Produces installation item from given resource.
	 * <p>
	 * This method doesn't need to check the passed resource for compatibility.
	 * The Install to menu driver must have been already called
	 * {@link #isCapable(Object)} method to determine whether the provider can
	 * use the object as input.
	 * </p>
	 * 
	 * @param resource
	 *            the input for the provider
	 * @return the item to be installed or <b>null</b> if this provider is not
	 *         capable to handle the passed resource
	 */
	public InstallationItem getInstallationItem(Object resource);

	/**
	 * Prepares a group of {@link InstallationItem}s at once. The list of items
	 * is not filtered in any way, so the implementation must properly select,
	 * which items to prepare, ignoring unrecognized once.
	 * 
	 * @param items
	 * @param properties
	 * @param monitor
	 * @return
	 * @since 6.0
	 */
	public IStatus prepareItems(List/*<InstallationItem>*/items, Map properties, IProgressMonitor monitor);

	/**
	 * Initializes the provider with given extension registry configuration
	 * element.
	 * <p>
	 * This method is called by the Install to menu driver to initialize the
	 * detected item provider with additional options, specified in the
	 * extension registry.
	 * </p>
	 * <p>
	 * If the provider cannot finish its initialization, it must throw
	 * {@link CoreException} with a short message describing the problem.
	 * </p>
	 * 
	 * @param element
	 *            the configuration element of the item provider
	 */
	public void init(IConfigurationElement element) throws CoreException;
}
