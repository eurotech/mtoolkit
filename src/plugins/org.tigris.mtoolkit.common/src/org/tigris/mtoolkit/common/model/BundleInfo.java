/*******************************************************************************
 * Copyright (c) 2005, 2013 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.common.model;

/**
 *
 * Common interface for all bundle models in mToolkit. It provides the basic
 * information for OSGi bundle - symbolic name and version.
 *
 * @since 6.1
 */
public interface BundleInfo {

  /**
   * Returns the symbolic name of this bundle model. It's
   * implementation-dependent if this method could return null.
   *
   * @return
   */
  public String getSymbolicName();

  /**
   * Returns the version of this bundle model. It's implementation-dependent if
   * this method could return null.
   *
   * @return
   */
  public String getVersion();

}
