/*******************************************************************************
 * Copyright (c) 2005, 2013 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.osgimanagement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;

public final class SystemBundlesProvider {
  private static final String                       SYSTEM_BUNDLES_EXT_POINT_ID = "org.tigris.mtoolkit.osgimanagement.systemBundlesProvider";

  // System bundles providers
  private static final List<ISystemBundlesProvider> systemBundlesProviders      = new ArrayList<ISystemBundlesProvider>();

  static {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint extensionPoint = registry.getExtensionPoint(SYSTEM_BUNDLES_EXT_POINT_ID);
    IConfigurationElement[] elements = extensionPoint.getConfigurationElements();
    if (elements != null) {
      for (int i = 0; i < elements.length; i++) {
        try {
          systemBundlesProviders.add((ISystemBundlesProvider) elements[i].createExecutableExtension("class"));
        } catch (CoreException e) {
          FrameworkPlugin.error("Exception while intializing system bundles provider elements", e);
        }
      }
    }
  }

  private SystemBundlesProvider() {
  }

  public static Set<String> getSystemBundlesIDs() {
    Set<String> systemBundles = new HashSet<String>();
    for (ISystemBundlesProvider provider : systemBundlesProviders) {
      Set<String> bundles = provider.getSystemBundlesIDs();
      if (bundles == null) {
        continue;
      }
      systemBundles.addAll(bundles);
    }
    return systemBundles;
  }

}
