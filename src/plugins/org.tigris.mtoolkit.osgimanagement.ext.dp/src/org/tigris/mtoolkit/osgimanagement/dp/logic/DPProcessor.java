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
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.swt.graphics.Image;
import org.tigris.mtoolkit.common.FileUtils;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.osgimanagement.dp.Activator;
import org.tigris.mtoolkit.osgimanagement.dp.DPActionsProvider;
import org.tigris.mtoolkit.osgimanagement.dp.images.ImageHolder;
import org.tigris.mtoolkit.osgimanagement.installation.FrameworkProcessorExtension;
import org.tigris.mtoolkit.osgimanagement.model.Framework;

public final class DPProcessor implements FrameworkProcessorExtension {
  public static final String MIME_DP = "application/vnd.osgi.dp";
  public static final String[] SUPPORTED_MIME_TYPES = new String[] { MIME_DP };

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.installation.FrameworkProcessor#getName()
   */
  public String getName() {
    return "Deployment package processor";
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.installation.FrameworkProcessor#getImage()
   */
  public Image getImage() {
    return ImageHolder.getImage(DPActionsProvider.DP_GROUP_IMAGE_PATH);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.installation.FrameworkProcessor#getSupportedMimeTypes()
   */
  public String[] getSupportedMimeTypes() {
    return SUPPORTED_MIME_TYPES;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.installation.FrameworkProcessor#getPriority(org.tigris.mtoolkit.common.installation.InstallationItem)
   */
  public int getPriority(InstallationItem item) {
    String mimeType = item.getMimeType();
    if (MIME_DP.equals(mimeType)) {
      return PRIORITY_HIGH;
    }
    return PRIORITY_NOT_SUPPPORTED;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.installation.FrameworkProcessorExtension#processItems(java.util.List,
   *                                                                                              java.util.List,
   *                                                                                              java.util.Map,
   *                                                                                              boolean
   *                                                                                              org.tigris.mtoolkit.osgimanagement.model.Framework,
   *                                                                                              org.eclipse.core.runtime.IProgressMonitor)
   */
  public boolean processItems(List items, List installed, Map preparationProps, boolean autoStart, Framework framework,
      IProgressMonitor monitor) throws CoreException {
    SubMonitor processMonitor = SubMonitor.convert(monitor, items.size() + 1);
    processMonitor.setTaskName("Installing deployment packages...");
    processMonitor.worked(1);
    for (int i = 0; i < items.size(); i++) {
      processItem((InstallationItem) items.get(i), framework, processMonitor.newChild(1));
      if (processMonitor.isCanceled()) {
        break;
      }
    }
    monitor.done();
    return true;
  }

  private static void processItem(InstallationItem item, Framework framework, IProgressMonitor monitor) throws CoreException {
    File packageFile = null;
    InputStream input = null;
    try {
      input = item.getInputStream();
      packageFile = Activator.saveFile(input, item.getName());
      InstallDeploymentOperation operation = new InstallDeploymentOperation(framework);
      operation.install(packageFile, monitor);
    } catch (Exception e) {
      throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Unable to install deployment package", e));
    } finally {
      FileUtils.close(input);
      if (packageFile != null) {
        packageFile.delete();
      }
      monitor.done();
    }
  }
}
