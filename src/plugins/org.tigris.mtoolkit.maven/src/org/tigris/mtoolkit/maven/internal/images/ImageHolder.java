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
package org.tigris.mtoolkit.maven.internal.images;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.tigris.mtoolkit.maven.internal.MavenCorePlugin;


public class ImageHolder {

  private static final String OBJECT_PATH = "/icons/obj16/";

  public static final String POM_ICON = OBJECT_PATH + "pom.gif";

  public static void initializeImageRegistry(ImageRegistry registry) {
    registry.put(POM_ICON, MavenCorePlugin.getImageDescriptor(POM_ICON));
  }

  private static ImageRegistry getImageRegistry() {
    MavenCorePlugin plugin = MavenCorePlugin.getDefault();
    if (plugin != null) {
      ImageRegistry registry = plugin.getImageRegistry();
      if (registry != null)
        return registry;
    }
    throw new IllegalStateException("Plugin's image registry is not available.");
  }

  public static Image getImage(String id) {
    return getImageRegistry().get(id);
  }

  public static ImageDescriptor getImageDescriptor(String id) {
    return getImageRegistry().getDescriptor(id);
  }

}
