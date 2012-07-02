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
package org.tigris.mtoolkit.osgimanagement.installation;

import org.eclipse.jface.resource.ImageDescriptor;
import org.tigris.mtoolkit.common.installation.InstallationTarget;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.images.ImageHolder;
import org.tigris.mtoolkit.osgimanagement.model.Framework;

/**
 * @since 5.0
 */
public final class FrameworkTarget implements InstallationTarget {
  private final Framework fw;

  public FrameworkTarget(Framework fw) {
    this.fw = fw;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.installation.InstallationTarget#getIcon()
   */
  public ImageDescriptor getIcon() {
    if (fw.isConnected()) {
      return ImageHolder.getImageDescriptor(ConstantsDistributor.SERVER_ICON_CONNECTED);
    }
    return ImageHolder.getImageDescriptor(ConstantsDistributor.SERVER_ICON_DISCONNECTED);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.installation.InstallationTarget#getName()
   */
  public String getName() {
    return fw.getName();
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.installation.InstallationTarget#getUID()
   */
  public String getUID() {
    return "framework_" + fw.getName().hashCode();
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.installation.InstallationTarget#isConnected()
   */
  public boolean isConnected() {
    return fw.isConnected();
  }


  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.installation.InstallationTarget#isTransient()
   */
  public boolean isTransient() {
    return fw.isAutoConnected();
  }

  public Framework getFramework() {
    return fw;
  }
}
