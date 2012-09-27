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
package org.tigris.mtoolkit.console.utils.images;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.tigris.mtoolkit.console.utils.OSGiConsolePlugin;

public final class ImageHolder {
  public static final String  SERVER_ICON_DISCONNECTED = "servernc.gif"; //$NON-NLS-1$
  public static final String  SERVER_ICON_CONNECTED    = "server.gif";  //$NON-NLS-1$

  private static final String IMAGES_PATH              = "/images/";    //$NON-NLS-1$

  private ImageHolder() {
  }

  public static Image getImage(String key) { // NO_UCD
    ImageRegistry imgRegistry = OSGiConsolePlugin.getDefault().getImageRegistry();
    Image image = imgRegistry.get(key);
    if (image == null) {
      createImage(key, imgRegistry);
    }
    return imgRegistry.get(key);
  }

  public static ImageDescriptor getImageDescriptor(String key) {
    ImageRegistry imgRegistry = OSGiConsolePlugin.getDefault().getImageRegistry();
    ImageDescriptor descriptor = imgRegistry.getDescriptor(key);
    if (descriptor == null) {
      descriptor = createImage(key, imgRegistry);
    }
    return descriptor;
  }

  private static ImageDescriptor createImage(String key, ImageRegistry imgRegistry) {
    ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(OSGiConsolePlugin.PLUGIN_ID, key);
    if (descriptor == null) {
      descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(OSGiConsolePlugin.PLUGIN_ID, IMAGES_PATH + key);
    }
    imgRegistry.put(key, descriptor);
    return descriptor;
  }
}
