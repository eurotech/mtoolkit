/*******************************************************************************
 * Copyright (c) 2005, 2012 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.osgimanagement.installation;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.Image;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.osgimanagement.model.Framework;

public interface FrameworkProcessorExtension {
  static final int PRIORITY_LOW = 0;
  static final int PRIORITY_NORMAL = 5;
  static final int PRIORITY_HIGH = 10;
  static final int PRIORITY_NOT_SUPPPORTED = -1;

  String getName();

  Image getImage();

  String[] getSupportedMimeTypes();

  int getPriority(InstallationItem item);

  boolean processItem(InstallationItem item, List<InstallationItem> dependencies, Map preparationProps,
      Framework framework, IProgressMonitor monitor) throws CoreException;
}
