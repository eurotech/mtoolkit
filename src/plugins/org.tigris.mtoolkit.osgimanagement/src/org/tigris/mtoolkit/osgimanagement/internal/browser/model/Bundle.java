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
package org.tigris.mtoolkit.osgimanagement.internal.browser.model;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleException;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.osgimanagement.IconProvider;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.images.ImageHolder;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public class Bundle extends Model implements IconProvider, ConstantsDistributor {
  public static final String  OVR_ACTIVE_ICON       = "ovr_active.gif";                 //$NON-NLS-1$
  public static final String  OVR_RESOLVED_ICON     = "ovr_resolved.gif";               //$NON-NLS-1$
  public static final String  OVR_SIGNED_ICON       = "ovr_signed2.gif";                //$NON-NLS-1$
  public static final int     ICON_WIDTH            = 16;

  // bundle types
  public static final int     BUNDLE_TYPE_FRAGMENT  = RemoteBundle.BUNDLE_TYPE_FRAGMENT;
  public static final int     BUNDLE_TYPE_EXTENSION = BUNDLE_TYPE_FRAGMENT + 1;

  private static final String BUNDLE_ICON_HEADER    = "Bundle-Icon";

  private final RemoteBundle  rBundle;

  private long                id;
  private boolean             needsUpdate;
  private int                 state;
  private String              version;
  private boolean             isSigned;
  // 0 for regular bundles
  private int                 type                  = -1;
  private String              category;
  private ImageData           iconData;
  private Image               icon;
  private Image               oldIcon;

  public Bundle(String name, RemoteBundle rBundle, int state, int type, String category, String version) throws IAgentException {
    super(name);
    Assert.isNotNull(rBundle);
    this.rBundle = rBundle;
    this.id = rBundle.getBundleId();
    isSigned = rBundle.isBundleSigned();
    // state is not get from rBundle to avoid unnecessary remote method
    // calls
    this.state = state;
    this.type = type;
    this.category = category;
    this.version = version;
    needsUpdate = true;
  }

  public Bundle(Bundle master) {
    super(master.getName(), master);
    rBundle = master.rBundle;
    id = master.getID();
    state = master.getState();
    type = master.getType();
    category = master.getCategory();
    needsUpdate = true;
    isSigned = master.isSigned;
    version = master.version;
  }

  public Bundle(Bundle master, String category) {
    this(master);
    this.category = category;
  }

  public RemoteBundle getRemoteBundle() {
    return rBundle;
  }

  public long getID() {
    return id;
  }

  // Overrides method in Model class
  @Override
  public boolean testAttribute(Object target, String name, String value) {
    if (!(target instanceof org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle)) {
      return false;
    }
    FrameworkImpl framework = (FrameworkImpl) findFramework();
    if (framework == null) {
      return false;
    }
    if (!framework.isConnected()) {
      return false;
    }

    if (name.equalsIgnoreCase(BUNDLE_STATE_NAME)) {
      if (value.equalsIgnoreCase(BUNDLE_UNINSTALLED_VALUE)) {
        return state == org.osgi.framework.Bundle.UNINSTALLED;
      }
      if (value.equalsIgnoreCase(BUNDLE_INSTALLED_VALUE)) {
        return state == org.osgi.framework.Bundle.INSTALLED;
      }
      if (value.equalsIgnoreCase(BUNDLE_RESOLVED_VALUE)) {
        return state == org.osgi.framework.Bundle.RESOLVED;
      }
      if (value.equalsIgnoreCase(BUNDLE_STARTING_VALUE)) {

        return state == org.osgi.framework.Bundle.STARTING;
      }
      if (value.equalsIgnoreCase(BUNDLE_STOPPING_VALUE)) {
        return state == org.osgi.framework.Bundle.STOPPING;
      }
      if (value.equalsIgnoreCase(BUNDLE_ACTIVE_VALUE)) {
        return state == org.osgi.framework.Bundle.ACTIVE;
      }
    }

    return false;
  }

  public boolean isShowID() {
    return findFramework() != null ? ((FrameworkImpl) findFramework()).isShowBundlesID() : false;
  }

  public boolean isShowVersion() {
    return findFramework() != null ? ((FrameworkImpl) findFramework()).isShowBundlesVersion() : false;
  }

  // this method will always ask the remote side, so it needs to throw
  // exception
  public void update() throws IAgentException {
    /* Framework framework = */findFramework();
    // if (framework != null && framework.getConnector() != null) {
    try {
      refreshStateFromRemote();
      RemoteBundle rBundle = getRemoteBundle();
      version = rBundle.getVersion();
      iconData = null;
      if (icon != null) {
        oldIcon = icon;
        icon = null;
      }
      isSigned = rBundle.isBundleSigned();
    } finally {
      // always update the viewers
      updateElement();
    }
    // }
  }

  public boolean isNeedUpdate() {
    return needsUpdate;
  }

  public int getState() {
    if (getMaster() != null) {
      return ((Bundle) getMaster()).getState();
    }
    return state;
  }

  public void setState(int i) {
    state = i;
  }

  public void refreshStateFromRemote() {
    try {
      setState(getRemoteBundle().getState());
    } catch (IAgentException e) {
      // ignore
      // TODO: Add logging of this exception in debug mode
    }
  }

  public String getVersion() throws IAgentException {
    if (version == null) {
      try {
        version = rBundle.getVersion();
      } catch (IAgentException e) {
        if (e.getErrorCode() != IAgentErrors.ERROR_BUNDLE_UNINSTALLED) {
          // ignore uninstalled bundles
          throw e;
        }
      }
    }
    version = version == null ? Messages.missing_version : version;
    return version;
  }

  public int getType() {
    return type;
  }

  public boolean isSystemBundle() {
    FrameworkImpl framework = (FrameworkImpl) findFramework();
    if (framework == null || !framework.isConnected()) {
      return false;
    }
    return framework.isSystemBundle(this);
  }

  public void refreshTypeFromRemote() throws IAgentException {
    FrameworkImpl fw = (FrameworkImpl) findFramework();
    if (fw != null) {
      type = fw.getRemoteBundleType(rBundle, rBundle.getHeaders(null));
    }
  }

  public String getCategory() {
    return category;
  }

  public String getString() {
    StringBuffer buff = new StringBuffer();
    buff.append("ID: ").append(getID()).append(" Bundle name: ").append(getName()); //$NON-NLS-1$ //$NON-NLS-2$
    return buff.toString();
  }

  @Override
  public String toString() {
    try {
      return name + " " + getVersion();
    } catch (IAgentException e) {
    }
    return name;
  }

  @Override
  public String getLabel() {
    String label = getName();
    if (isShowID()) {
      label += " [" + String.valueOf(getID()) + "]";
    }
    if (isShowVersion()) {
      try {
        label += " [" + String.valueOf(getVersion()) + "]";
      } catch (IAgentException e) {
        BrowserErrorHandler.processError(e, NLS.bind(Messages.cant_get_bundle_version, String.valueOf(getID())), false);
      }
    }
    return label;
  }

  public Image getIcon() {
    if (icon != null) {
      return icon;
    }
    if (iconData == null) {
      return null;
    }

    ImageDescriptor overlay;
    switch (state) {
    case org.osgi.framework.Bundle.RESOLVED:
      overlay = ImageHolder.getImageDescriptor(OVR_RESOLVED_ICON);
      break;
    case org.osgi.framework.Bundle.ACTIVE:
      overlay = ImageHolder.getImageDescriptor(OVR_ACTIVE_ICON);
      break;
    default:
      return null;
    }
    if (iconData == null) {
      // it could be in a just started icon update process, so later will
      // be updated
      return null;
    }
    Image baseIcon = new Image(PlatformUI.getWorkbench().getDisplay(), iconData);
    icon = new DecorationOverlayIcon(baseIcon, overlay, IDecoration.TOP_RIGHT).createImage();
    if (oldIcon != null) {
      oldIcon.dispose();
      oldIcon = null;
    }
    baseIcon.dispose();
    return icon;
  }

  public ImageData fetchIconData() {
    if (iconData != null) {
      return iconData;
    }
    InputStream is = null;
    try {
      String iconPath = getIconPath();
      if (iconPath == null) {
        return null;
      }
      is = rBundle.getResource(iconPath);
      if (is == null) {
        return null;
      }
      is = new BufferedInputStream(is);
      iconData = new ImageData(is);
      if (iconData.width != ICON_WIDTH) {
        iconData = iconData.scaledTo(ICON_WIDTH, ICON_WIDTH);
      }
      return iconData;
    } catch (IAgentException e) {
    } catch (BundleException e) {
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
        }
      }
    }
    return null;
  }

  private String getIconPath() throws IAgentException, BundleException {
    String iconHeader = rBundle.getHeader(BUNDLE_ICON_HEADER, null);
    ManifestElement[] elements = null;
    if (iconHeader != null) {
      elements = ManifestElement.parseHeader(BUNDLE_ICON_HEADER, iconHeader);
    }
    if (elements == null) {
      return null;
    }
    String noSize = null;
    String smallerSize = null;
    int smaller = 0;
    String biggerSize = null;
    int bigger = Integer.MAX_VALUE;
    for (int i = 0; i < elements.length; i++) {
      ManifestElement manifestEl = elements[i];
      String value = manifestEl.getValue();
      String sizeAttr = manifestEl.getAttribute("size");
      if (sizeAttr == null) {
        if (noSize == null) {
          noSize = value;
        }
        continue;
      }
      int size;
      try {
        size = Integer.parseInt(sizeAttr);
      } catch (NumberFormatException e) {
        continue;
      }
      if (size == ICON_WIDTH) {
        return value;
      } else if (size < ICON_WIDTH) {
        if (smaller < size) {
          smaller = size;
          smallerSize = value;
        }
      } else if (size > ICON_WIDTH) {
        if (bigger > size) {
          bigger = size;
          biggerSize = value;
        }
      }
    }
    if (biggerSize != null) {
      return biggerSize;
    } else if (smallerSize != null) {
      return smallerSize;
    }
    return noSize;
  }

  public boolean isSigned() {
    return isSigned;
  }

  @Override
  public void finalize() {
    if (icon != null) {
      icon.dispose();
      icon = null;
    }
    if (oldIcon != null) {
      oldIcon.dispose();
      oldIcon = null;
    }
  }
}
