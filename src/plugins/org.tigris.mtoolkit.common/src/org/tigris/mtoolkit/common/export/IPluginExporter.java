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
package org.tigris.mtoolkit.common.export;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

interface IPluginExporter {
  boolean hasFinished();

  IStatus getResult();

  String getQualifier();

  /**
   * @since 5.0
   */
  void asyncExportPlugins(Object info);

  /**
   * @since 5.0
   */
  IStatus syncExportPlugins(Object info, IProgressMonitor monitor);

  /**
   * @since 5.0
   */
  IStatus join(long timeout) throws InterruptedException;
}
