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
package org.tigris.mtoolkit.osgimanagement;

import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.model.Framework;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public interface ContentTypeModelProvider {
  /**
   * @since 5.0
   */
  public static final String MIME_TYPE_BUNDLE = "bundle";

  /**
   * @since 5.0
   */
  public void connect(Framework fw, IProgressMonitor monitor);

  /**
   * @since 5.0
   */
  public void disconnect();

  /**
   * @since 5.0
   */
  public Model switchView(int viewType);

  /**
   * @since 5.0
   */
  public Model getResource(String id, String version, Framework fw) throws IAgentException;

  /**
   * @since 5.0
   */
  public String[] getSupportedMimeTypes();

}
