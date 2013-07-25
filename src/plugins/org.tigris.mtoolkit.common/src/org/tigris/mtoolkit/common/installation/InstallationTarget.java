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

import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * This interface represents the target where one InstallationItem will be
 * installed.
 */
public interface InstallationTarget {
  /**
   * Returns unique id of the installation target. This uid can be used as a
   * persistent identifier.
   *
   * @return the unique id that represents the target
   */
  public String getUID();

  /**
   * Returns human-readable name for this target.
   *
   * @return the name
   */
  public String getName();

  /**
   * Returns the icon for this target.
   *
   * @return the icon
   */
  public ImageDescriptor getIcon();

  /**
   * Returns if the target is connected
   *
   * @return connected or not
   */
  public boolean isConnected();

  /**
   * Returns if the target can be stored persistently
   *
   * @return the target can be stored persistently
   */
  public boolean isTransient();

  /**
   * Signing certificates which will be used by installation item processor
   * 
   * @return signing certificates or empty map if none
   */
  public Map getSigningProperties();
}
