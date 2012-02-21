package org.tigris.mtoolkit.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IResource;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.common.installation.InstallationItemProcessor;
import org.tigris.mtoolkit.common.installation.InstallationItemProvider;
import org.tigris.mtoolkit.common.installation.InstallationRegistry;

public class ResourcePropertyTester extends PropertyTester {
  protected InstallationItem installationItem;

  /* (non-Javadoc)
   * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
   */
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    IResource resource = (IResource) receiver;
    if (isResourceProviderAvailable(resource) && isResourceProcessorAvailable(resource)) {
      return true;
    }
    return false;
  }

  private boolean isResourceProcessorAvailable(IResource resource) {
    Iterator processorsIterator = InstallationRegistry.getInstance().getProcessors().iterator();
    while (processorsIterator.hasNext()) {
      InstallationItemProcessor processor = (InstallationItemProcessor) processorsIterator.next();
      String[] types = processor.getSupportedMimeTypes();
      if (types == null) {
        continue;
      }
      List mimeTypes = new ArrayList();
      for (int j = 0; j < types.length; j++) {
        if (!mimeTypes.contains(types[j])) {
          mimeTypes.add(types[j]);
        }
      }
      String curItemMimeType = installationItem.getMimeType();
      if (mimeTypes.contains(curItemMimeType)) {
        return true;
      }
    }
    return false;
  }

  private boolean isResourceProviderAvailable(IResource resource) {
    Iterator providersIterator = InstallationRegistry.getInstance().getProviders().iterator();
    while (providersIterator.hasNext()) {
      InstallationItemProvider provider = (InstallationItemProvider) providersIterator.next();
      if (provider == null) {
        continue;
      }
      if (provider.isCapable(resource)) {
        installationItem = ((InstallationItemProvider) provider).getInstallationItem(resource);
        if (installationItem != null) {
          return true;
        }
      }
    }
    return false;
  }
}
