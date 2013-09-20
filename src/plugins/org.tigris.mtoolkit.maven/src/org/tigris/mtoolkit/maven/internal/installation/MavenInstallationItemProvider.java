/*******************************************************************************
 * Copyright (c) 2011 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.maven.internal.installation;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.common.installation.InstallationItemProvider;
import org.tigris.mtoolkit.maven.MavenConstants;
import org.tigris.mtoolkit.maven.MavenUtils;
import org.tigris.mtoolkit.maven.internal.MavenCorePlugin;
import org.tigris.mtoolkit.maven.internal.images.ImageHolder;

public final class MavenInstallationItemProvider implements InstallationItemProvider {
  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.installation.InstallationItemProvider#getName()
   */
  public String getName() {
    return "Maven projects provider";
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.installation.InstallationItemProvider#getImageDescriptor()
   */
  public ImageDescriptor getImageDescriptor() {
    return ImageHolder.getImageDescriptor(ImageHolder.POM_ICON);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.installation.InstallationItemProvider#init(org.eclipse.core.runtime.IConfigurationElement)
   */
  public void init(IConfigurationElement element) throws CoreException {
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.installation.InstallationItemProvider#isCapable(java.lang.Object)
   */
  public boolean isCapable(Object resource) {
    resource = adaptItem(resource);
    if (resource instanceof File) {
      File file = (File) resource;
      if (file.exists() && file.getName().equals(MavenConstants.POM_FILE)) {
        return true;
      }
    }
    if (resource instanceof IMavenProjectFacade) {
      if (!((IMavenProjectFacade) resource).getPackaging().equals(MavenConstants.POM_PACKAGING)) {
        return true;
      }
    }
    return false;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.installation.InstallationItemProvider#getInstallationItem(java.lang.Object)
   */
  public InstallationItem getInstallationItem(Object resource) {
    resource = adaptItem(resource);
    if (resource instanceof File) {
      return new MavenFileItem(this, (File) resource);
    } else if (resource instanceof IMavenProjectFacade) {
      return new MavenProjectItem(this, (IMavenProjectFacade) resource);
    } else {
      return null;
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.installation.InstallationItemProvider#prepareItems(java.util.List, java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
   */
  @SuppressWarnings("rawtypes")
  public IStatus prepareItems(List items, Map properties, IProgressMonitor monitor) {
    @SuppressWarnings("unchecked")
    List<InstallationItem> castedItems = items;
    if (monitor == null) {
      monitor = new NullProgressMonitor();
    }
    monitor.beginTask("Preparing installation items...", items.size());
    try {
      for (InstallationItem item : castedItems) {
        if (item instanceof AbstractMavenItem) {
          monitor.subTask(((AbstractMavenItem) item).getDisplayName());
          IStatus status = prepareItem((AbstractMavenItem) item, properties, monitor);
          if (status.matches(IStatus.ERROR | IStatus.CANCEL)) {
            return status;
          }
        }
        monitor.worked(1);
      }
    } finally {
      monitor.done();
    }
    return Status.OK_STATUS;
  }

  private Object adaptItem(Object resource) {
    if (resource instanceof IMavenProjectFacade) {
      return resource;
    }
    if (resource instanceof IResource) {
      Object adapted = adaptResource((IResource) resource);
      if (adapted != null) {
        return adapted;
      }
    }
    if (resource instanceof IAdaptable) {
      Object adapted = ((IAdaptable) resource).getAdapter(IResource.class);
      if (adapted != null) {
        adapted = adaptResource((IResource) adapted);
      }
      if (adapted != null) {
        return adapted;
      }
    }
    return resource;
  }

  private Object adaptResource(IResource resource) {
    if (resource.getType() == IResource.PROJECT) {
      IProject project = (IProject) resource;
      IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project);
      if (facade != null) {
        return facade;
      }
      IFile pomFile = project.getFile(new Path(MavenConstants.POM_FILE));
      resource = pomFile;
    }
    if (resource.getType() == IResource.FOLDER) {
      IFile pomFile = ((IContainer) resource).getFile(new Path(MavenConstants.POM_FILE));
      resource = pomFile;
    }
    if (resource.getType() == IResource.FILE) {
      return ((IFile) resource).getLocation().toFile();
    }
    return null;
  }

  private IStatus prepareItem(AbstractMavenItem item, Map properties, IProgressMonitor monitor) {
    File pomFile = item.getPomLocationAtFilesystem();
    if (pomFile == null || !pomFile.exists()) {
      return MavenCorePlugin.newStatus(IStatus.ERROR,
          "Cannot find " + MavenConstants.POM_FILE + " for " + item.getDisplayName(), null);
    }
    try {
      MavenUtils.launchDefaultBuild(pomFile.getParentFile(), monitor);
      File artifact = MavenUtils.locateMavenArtifact(pomFile);
      if (!artifact.exists()) {
        return MavenCorePlugin.newStatus(IStatus.ERROR, "Unable to find Maven artifact at expected location: "
            + artifact.getAbsolutePath(), null);
      }
      if (monitor.isCanceled()) {
        return Status.CANCEL_STATUS;
      }
      item.setGeneratedArtifact(artifact);
      IStatus status = item.completePrepare(monitor, properties);
      if (status.matches(IStatus.ERROR)) {
        return status;
      } else if (!status.isOK()) {
        MavenCorePlugin.log(status);
      }
      return Status.OK_STATUS;
    } catch (CoreException e) {
      return MavenCorePlugin.newStatus(IStatus.ERROR, "Unable to execute Maven build for " + item.getDisplayName(), e);
    }
  }

}
