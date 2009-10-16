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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.tigris.mtoolkit.common.UtilitiesPlugin;

public class WorkspaceFileItem extends BaseFileItem {

  protected IFile file;

  public WorkspaceFileItem(IFile file, String mimeType) {
    super(file.getLocation().toFile(), mimeType);
    this.file = file;
  }

  public InputStream getInputStream() throws IOException {
    try {
      if (preparedFile != null) {
        return new FileInputStream(preparedFile);
      }
      return file.getContents();
    } catch (CoreException e) {
      UtilitiesPlugin.error(NLS.bind("Failed to retrieve contents of file: {0}", file.getFullPath()), e);
      return null;
    }
  }

  public IFile getFile() {
    return file;
  }

  public IStatus prepare(IProgressMonitor monitor, Map properties) {
    try {
      file.refreshLocal(IFile.DEPTH_ZERO, monitor);
      baseFile = file.getLocation().toFile();
      return super.prepare(monitor, properties);
    } catch (CoreException e) {
      return UtilitiesPlugin.newStatus(IStatus.ERROR, "Failed to prepare file for installation", e);
    }
  }

  public Object getAdapter(Class adapter) {
    if (adapter.equals(IResource.class)) {
      return file;
    } else {
      return super.getAdapter(adapter);
    }
  }
}
