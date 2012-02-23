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
package org.tigris.mtoolkit.osgimanagement.dp.logic;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.graphics.Image;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.iagent.RemoteDP;
import org.tigris.mtoolkit.osgimanagement.dp.Activator;
import org.tigris.mtoolkit.osgimanagement.dp.DPActionsProvider;
import org.tigris.mtoolkit.osgimanagement.dp.images.ImageHolder;
import org.tigris.mtoolkit.osgimanagement.installation.FrameworkProcessor;
import org.tigris.mtoolkit.osgimanagement.model.Framework;

public final class DPProcessor extends FrameworkProcessor {
  public static final String MIME_DP = "application/vnd.osgi.dp";
  private static final String[] SUPPORTED_MIME_TYPES = new String[] { MIME_DP };

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.installation.FrameworkProcessor#getSupportedMimeTypes()
   */
  public String[] getSupportedMimeTypes() {
    return SUPPORTED_MIME_TYPES;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.installation.FrameworkProcessor#install(java.io.InputStream, 
   *                                                                                 org.tigris.mtoolkit.common.installation.InstallationItem, 
   *                                                                                 org.tigris.mtoolkit.osgimanagement.model.Framework, 
   *                                                                                 org.eclipse.core.runtime.IProgressMonitor)
   */
  public Object install(InputStream input, InstallationItem item, Framework framework, IProgressMonitor monitor)
      throws CoreException {
    RemoteDP result = null;
    File packageFile = null;
    try {
      packageFile = saveFile(input, item.getName());
      result = new InstallDeploymentOperation(framework).install(packageFile, monitor);
    } catch (Exception e) {
      throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Unable to install deployment package", e));
    } finally {
      if (input != null) {
        try {
          input.close();
        } catch (IOException e) {
        }
      }
      if (packageFile != null) {
        packageFile.delete();
      }
    }
    return result;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.installation.FrameworkProcessor#getName()
   */
  public String getName() {
    return "Deployment package processor";
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.installation.FrameworkProcessor#getImage()
   */
  protected Image getImage() {
    return ImageHolder.getImage(DPActionsProvider.DP_GROUP_IMAGE_PATH);
  }

}
