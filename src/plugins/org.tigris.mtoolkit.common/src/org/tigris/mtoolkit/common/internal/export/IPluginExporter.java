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
package org.tigris.mtoolkit.common.internal.export;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

public interface IPluginExporter {
  boolean hasFinished(); // NO_UCD

  IStatus getResult(); // NO_UCD

  String getQualifier(); // NO_UCD
  /**
   * @since 5.0
   */
  void asyncExportPlugins(Object info); // NO_UCD

  /**
   * @since 5.0
   */
  IStatus syncExportPlugins(Object info, IProgressMonitor monitor);
}
