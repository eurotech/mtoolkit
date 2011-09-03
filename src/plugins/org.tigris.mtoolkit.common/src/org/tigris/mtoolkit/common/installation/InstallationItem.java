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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

/**
 * InstallationItem is the item that can be installed to
 * {@link InstallationTarget}. Such items are provided by
 * {@link InstallationItemProvider} and can be used from
 * {@link InstallationItemProcessor}
 */
public interface InstallationItem extends IAdaptable {
	/**
	 * Prepares the item for installation if it is not yet prepared. Otherwise
	 * does nothing.
	 * 
	 * @param monitor
	 *            the progress monitor for displaying current progress
	 * @param properties
	 *            contains additional properties which affect item preparation
	 *            (such as signing etc.)
	 * @return status, describing the result of prepare operation
	 */
	public IStatus prepare(IProgressMonitor monitor, Map properties);

	/**
	 * Returns open {@link InputStream} which provides the installation data.
	 * The client is responsible for closing the stream when finished.
	 * 
	 * @return the output stream
	 */
  public InputStream getInputStream() throws IOException;

	/**
	 * Returns the location of the item.
	 * 
	 * @return the location
	 * @since 6.0
	 */
  public String getLocation();

	/**
	 * Returns the MIME type of this item.
	 * 
	 * @return the MIME type
	 */
	public String getMimeType();

  /**
   * Returns the name of this item.
   * 
   * @return the item name
   */
  public String getName();
	
	/**
	 * Disposes this item and used resources
	 */
	public void dispose();

  /**
   * Returns nested installation items if this item is a container.
   *
   * @return
   */
  public InstallationItem[] getChildren();
}
