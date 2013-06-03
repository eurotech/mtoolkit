/*******************************************************************************
 * Copyright (c) 2005, 2012 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.common;

import java.util.Iterator;

import org.eclipse.core.expressions.PropertyTester;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.common.installation.InstallationItemProcessor;
import org.tigris.mtoolkit.common.installation.InstallationItemProvider;
import org.tigris.mtoolkit.common.installation.InstallationRegistry;

public final class ResourcePropertyTester extends PropertyTester {
  /* (non-Javadoc)
   * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
   */
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    return isResourceProcessorAvailable(getInstallationItem(receiver));
  }

  private boolean isResourceProcessorAvailable(InstallationItem installationItem) {
    if (installationItem == null) {
      return false;
    }
    String curItemMimeType = installationItem.getMimeType();
    Iterator processorsIterator = InstallationRegistry.getInstance().getProcessors().iterator();
    while (processorsIterator.hasNext()) {
      InstallationItemProcessor processor = (InstallationItemProcessor) processorsIterator.next();
      String[] types = processor.getSupportedMimeTypes();
      if (types == null || types.length == 0) {
        continue;
      }
      for (int i = 0; i < types.length; i++) {
        if (types[i].equals(curItemMimeType)) {
          return true;
        }
      }
    }
    return false;
  }

  private InstallationItem getInstallationItem(Object resource) {
    Iterator providersIterator = InstallationRegistry.getInstance().getProviders().iterator();
    while (providersIterator.hasNext()) {
      InstallationItemProvider provider = (InstallationItemProvider) providersIterator.next();
      if (provider == null) {
        continue;
      }
      if (provider.isCapable(resource)) {
        InstallationItem installationItem = provider.getInstallationItem(resource);
        if (installationItem != null) {
          return installationItem;
        }
      }
    }
    return null;
  }
}
