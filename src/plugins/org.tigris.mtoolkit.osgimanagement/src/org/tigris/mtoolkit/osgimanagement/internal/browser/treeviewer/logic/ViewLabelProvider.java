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
package org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.osgimanagement.ContentTypeActionsProvider;
import org.tigris.mtoolkit.osgimanagement.IconFetcher;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworksView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworksView.ActionsProviderElement;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.BundlesCategory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Category;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ObjectClass;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ServiceProperty;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ServicesCategory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.TreeRoot;
import org.tigris.mtoolkit.osgimanagement.internal.images.ImageHolder;
import org.tigris.mtoolkit.osgimanagement.model.Framework;
import org.tigris.mtoolkit.osgimanagement.model.Model;
import org.tigris.mtoolkit.osgimanagement.model.SimpleNode;

public final class ViewLabelProvider extends StyledCellLabelProvider {
  public static final String     ROOT_ICON                       = "homefolder.gif";                //$NON-NLS-1$
  public static final String     BUNDLE_ICON_EXTENSION_INSTALLED = "bundle_extension_installed.gif"; //$NON-NLS-1$
  public static final String     OBJECT_CLASS_ICON               = "objectClass.gif";               //$NON-NLS-1$
  public static final String     SERVICES_CATEGORY_ICON          = "services.gif";                  //$NON-NLS-1$
  public static final String     CATEGORY_ICON                   = "category.gif";                  //$NON-NLS-1$
  public static final String     BUNDLE_ICON_FRAGMENT_INSTALLED  = "bundle_fragment_installed.gif"; //$NON-NLS-1$
  public static final String     BUNDLE_ICON_EXTENSION           = "bundle_extension.gif";          //$NON-NLS-1$
  public static final String     BUNDLE_ICON_FRAGMENT            = "bundle_fragment.gif";           //$NON-NLS-1$
  public static final String     BUNDLE_ICON_STOPPING            = "bundle_stopping.gif";           //$NON-NLS-1$
  public static final String     BUNDLE_ICON_STARTING            = "bundle_starting.gif";           //$NON-NLS-1$
  public static final String     BUNDLE_ICON_INSTALLED           = "bundle_installed.gif";          //$NON-NLS-1$
  public static final String     BUNDLE_ICON_UNINSTALLED         = "bundle_uninstalled.gif";        //$NON-NLS-1$
  public static final String     BUNDLE_ICON_UNKNOWN             = "bundle_unk.gif";                //$NON-NLS-1$
  public static final String     BUNDLE_ICON_RESOLVED            = "bundle_resolved.gif";           //$NON-NLS-1$
  public static final String     BUNDLE_ICON_ACTIVE              = "bundle_active.gif";             //$NON-NLS-1$
  public static final String     BUNDLE_NODE_ICON                = "bundles_package.gif";           //$NON-NLS-1$

  private HashMap<Object, Image> customImages                    = new HashMap<Object, Image>();

  // Override to return proper image for every element
  public Image getImage(Object element) {
    if (element instanceof FrameworkImpl) {
      FrameworkImpl framework = (FrameworkImpl) element;
      if (framework.isConnected()) {
        return ImageHolder.getImage(ConstantsDistributor.SERVER_ICON_CONNECTED);
      } else {
        return ImageHolder.getImage(ConstantsDistributor.SERVER_ICON_DISCONNECTED);
      }
    }
    if (element instanceof Category) {
      return ImageHolder.getImage(ViewLabelProvider.CATEGORY_ICON);
    }
    if (element instanceof Bundle) {
      Bundle bundle = (Bundle) element;
      Image icon = getBundleImage(bundle);
      if (bundle.isSigned()) {
        if (customImages.containsKey(bundle)) {
          Image image = customImages.get(bundle);
          if (image != null) {
            image.dispose();
          }
        }
        icon = new DecorationOverlayIcon(icon, ImageHolder.getImageDescriptor(Bundle.OVR_SIGNED_ICON),
            IDecoration.BOTTOM_LEFT).createImage();
        if (icon != null) {
          customImages.put(bundle, icon);
        }
      }
      return icon;
    }
    if ((element instanceof ServicesCategory) || (element instanceof BundlesCategory)) {
      return ImageHolder.getImage(ViewLabelProvider.SERVICES_CATEGORY_ICON);
    }
    if (element instanceof ObjectClass) {
      return ImageHolder.getImage(ViewLabelProvider.OBJECT_CLASS_ICON);
    }
    if (element instanceof TreeRoot) {
      return ImageHolder.getImage(ViewLabelProvider.ROOT_ICON);
    }
    if (element instanceof SimpleNode) {
      if (((SimpleNode) element).getName().equals(Messages.bundles_node_label)) {
        return ImageHolder.getImage(ViewLabelProvider.BUNDLE_NODE_ICON);
      }
    }
    if (element instanceof ServiceProperty) {
      return null;
    }
    if (element instanceof Model) {
      FrameworkImpl fw = (FrameworkImpl) ((Model) element).findFramework();
      if (fw != null) {
        List actionProviders = FrameworksView.getActionsProviders();
        for (int i = 0; i < actionProviders.size(); i++) {
          ContentTypeActionsProvider manager = ((ActionsProviderElement) actionProviders.get(i)).getProvider();
          Image image = manager.getImage((Model) element);
          if (image != null) {
            return image;
          }
        }
      }
    }
    return null;
  }

  // Override to dispose Images properly
  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.StyledCellLabelProvider#dispose()
   */
  @Override
  public void dispose() {
    Collection<Image> images = customImages.values();
    for (Image image : images) {
      image.dispose();
    }
    images.clear();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.StyledCellLabelProvider#update(org.eclipse.jface.viewers.ViewerCell)
   */
  @Override
  public void update(ViewerCell cell) {
    Object element = cell.getElement();
    String text = (element instanceof Model) ? ((Model) element).getLabel() : element.toString();
    List styles = new ArrayList();

    if (element instanceof ServiceProperty) {
      TextStyle style = new TextStyle();
      style.foreground = PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLUE);
      styles.add(new StyleRange(0, text.indexOf(":"), style.foreground, style.background));
      style.foreground = PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK);
      styles.add(new StyleRange(text.indexOf(":") + 1, text.length(), style.foreground, style.background));
    }
    cell.setText(text);
    cell.setStyleRanges((StyleRange[]) styles.toArray(new StyleRange[styles.size()]));
    cell.setImage(getImage(element));
    super.update(cell);
  }

  private Image getBundleImage(Bundle bundle) {
    int state = bundle.getState();

    Image icon = getBundleDefinedIcon(bundle);
    if (icon != null) {
      return icon;
    }

    if (state == org.osgi.framework.Bundle.INSTALLED) {
      if (bundle.getType() != 0) {
        if (bundle.getType() == Bundle.BUNDLE_TYPE_FRAGMENT) {
          return ImageHolder.getImage(ViewLabelProvider.BUNDLE_ICON_FRAGMENT_INSTALLED);
        } else {
          return ImageHolder.getImage(ViewLabelProvider.BUNDLE_ICON_EXTENSION_INSTALLED);
        }
      }
      return ImageHolder.getImage(ViewLabelProvider.BUNDLE_ICON_INSTALLED);
    } else if (bundle.getType() != 0) {
      if (bundle.getType() == Bundle.BUNDLE_TYPE_FRAGMENT) {
        return ImageHolder.getImage(ViewLabelProvider.BUNDLE_ICON_FRAGMENT);
      } else {
        return ImageHolder.getImage(ViewLabelProvider.BUNDLE_ICON_EXTENSION);
      }
    } else {
      if (state == 0) {
        return ImageHolder.getImage(ViewLabelProvider.BUNDLE_ICON_UNKNOWN);
      }

      switch (state) {
      case org.osgi.framework.Bundle.UNINSTALLED: {
        return ImageHolder.getImage(ViewLabelProvider.BUNDLE_ICON_UNINSTALLED);
      }
      case org.osgi.framework.Bundle.RESOLVED: {
        return ImageHolder.getImage(ViewLabelProvider.BUNDLE_ICON_RESOLVED);
      }
      case org.osgi.framework.Bundle.STARTING: {
        return ImageHolder.getImage(ViewLabelProvider.BUNDLE_ICON_STARTING);
      }
      case org.osgi.framework.Bundle.STOPPING: {
        return ImageHolder.getImage(ViewLabelProvider.BUNDLE_ICON_STOPPING);
      }
      case org.osgi.framework.Bundle.ACTIVE: {
        return ImageHolder.getImage(ViewLabelProvider.BUNDLE_ICON_ACTIVE);
      }
      default: {
        return ImageHolder.getImage(ViewLabelProvider.BUNDLE_ICON_RESOLVED);
      }
      }
    }

  }

  private Image getBundleDefinedIcon(Bundle bundle) {
    Image icon = bundle.getIcon();
    if (icon != null) {
      return icon;
    }
    String name = null;
    Framework fw = bundle.findFramework();
    if (fw != null) {
      name = fw.getName();
    }
    IconFetcher.getInstance(name).enqueue(bundle);
    return null;
  }
}
