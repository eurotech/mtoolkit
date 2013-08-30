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
import java.util.Arrays;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.tigris.mtoolkit.common.installation.BaseFileItem;

public abstract class AbstractMavenItem extends BaseFileItem {
  protected MavenInstallationItemProvider mavenProvider;

  protected AbstractMavenItem(MavenInstallationItemProvider provider) {
    super(null, "application/java-archive");
    this.mavenProvider = provider;
  }

  @Override
  public IStatus prepare(IProgressMonitor monitor, @SuppressWarnings("rawtypes") Map properties) {
    return mavenProvider.prepareItems(Arrays.asList(this), properties, monitor);
  }

  public IStatus completePrepare(IProgressMonitor monitor, Map properties) {
    return super.prepare(monitor, properties);
  }

  /**
   * Note: no validation is done, whether the artifact actually exist at the
   * specified location.
   * 
   * @param artifact
   */
  protected void setGeneratedArtifact(File artifact) {
    if (this.baseFile != null)
      throw new IllegalStateException("Cannot set generated artifact location twice for Maven installation item: "
          + getDisplayName());
    this.baseFile = artifact;
  }

  public abstract File getPomLocationAtFilesystem();

  public abstract String getDisplayName();
}
