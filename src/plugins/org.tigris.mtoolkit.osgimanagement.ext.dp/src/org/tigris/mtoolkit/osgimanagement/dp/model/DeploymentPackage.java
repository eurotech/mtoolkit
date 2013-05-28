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
package org.tigris.mtoolkit.osgimanagement.dp.model;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Enumeration;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.tigris.mtoolkit.common.FileUtils;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.iagent.RemoteDP;
import org.tigris.mtoolkit.osgimanagement.ContentTypeModelProvider;
import org.tigris.mtoolkit.osgimanagement.IconProvider;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.model.Framework;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public final class DeploymentPackage extends Model implements IconProvider {
  private static final String DP_STALE_NAME        = "stale"; //$NON-NLS-1$
  private static final String DP_STALE_VALUE_TRUE  = "true"; //$NON-NLS-1$
  private static final String DP_STALE_VALUE_FALSE = "false"; //$NON-NLS-1$

  private Framework           framework;
  private RemoteDP            dp;
  private ImageData           iconData;
  private Image               icon;

  public DeploymentPackage(RemoteDP dp, Framework fw) throws IAgentException {
    super(dp.getName());
    this.dp = dp;
    this.framework = fw;
    Dictionary bundles = dp.getBundles();
    Enumeration keys = bundles.keys();
    while (keys.hasMoreElements()) {
      try {
        String name = (String) keys.nextElement();
        RemoteBundle bundle = dp.getBundle(name);
        if (bundle == null) {
          continue;
        }
        Model bundleNode = fw.createModel(ContentTypeModelProvider.MIME_TYPE_BUNDLE,
            Long.toString(bundle.getBundleId()), bundle.getVersion());
        if (bundleNode != null) {
          addElement(bundleNode);
        }
      } catch (IAgentException e) {
        e.printStackTrace();
      }
    }
  }

  public RemoteDP getRemoteDP() {
    return dp;
  }

  /**
   * @return
   * @throws IAgentException
   */
  public boolean isStale() {
    try {
      return dp.isStale();
    } catch (IAgentException e) {
      StatusManager.getManager().handle(new Status(IStatus.ERROR, FrameworkPlugin.PLUGIN_ID, e.getMessage(), e));
    }
    return false;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.model.Model#testAttribute(java.lang.Object, java.lang.String, java.lang.String)
   */
  @Override
  public boolean testAttribute(Object target, String name, String value) {
    if (!(target instanceof DeploymentPackage)) {
      return false;
    }
    if (!framework.isConnected()) {
      return false;
    }
    if (name.equalsIgnoreCase(DP_STALE_NAME)) {
      if (value.equalsIgnoreCase(DP_STALE_VALUE_TRUE)) {
        return isStale();
      }
      if (value.equalsIgnoreCase(DP_STALE_VALUE_FALSE)) {
        return !isStale();
      }
    }
    return false;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.IconProvider#getIcon()
   */
  public Image getIcon() {
    if (icon != null) {
      return icon;
    }
    // TODO add support for big icons by scaling them
    if (iconData == null || iconData.height > 16 || iconData.width > 16) {
      return null;
    }
    icon = new Image(PlatformUI.getWorkbench().getDisplay(), iconData);
    return icon;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.IconProvider#fetchIconData()
   */
  public ImageData fetchIconData() {
    if (iconData != null) {
      return iconData;
    }
    InputStream is = null;
    try {
      is = dp.getIcon();
      if (is == null) {
        return null;
      }
      is = new BufferedInputStream(is);
      iconData = new ImageData(is);
      return iconData;
    } catch (IAgentException e) {
    } finally {
      FileUtils.close(is);
    }
    return null;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#finalize()
   */
  @Override
  protected void finalize() {
    if (icon != null) {
      icon.dispose();
      icon = null;
    }
  }
}
