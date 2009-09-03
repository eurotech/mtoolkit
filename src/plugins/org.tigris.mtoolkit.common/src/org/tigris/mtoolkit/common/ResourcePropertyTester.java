package org.tigris.mtoolkit.common;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.common.installation.InstallationItemProcessor;
import org.tigris.mtoolkit.common.installation.InstallationItemProvider;

public class ResourcePropertyTester extends PropertyTester {

	private IExtensionRegistry registry = null;
	private InstallationItem installationItem;

	public ResourcePropertyTester() {
		super();
		registry = Platform.getExtensionRegistry();
	}

	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		IResource resource = (IResource) receiver;
		if (isResourceProviderAvailable(resource) && isResourceProcessorAvailable(resource)) {
			return true;
		}
		return false;

	}

	private boolean isResourceProcessorAvailable(IResource resource) {

		IExtensionPoint extensionPoint = registry.getExtensionPoint("org.tigris.mtoolkit.common.installationItemProcessors");
		IConfigurationElement[] elements = extensionPoint.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			if (!elements[i].getName().equals("processor")) {
				continue;
			}
			List mimeTypes = new ArrayList();
			try {
				Object processor = elements[i].createExecutableExtension("class");
				if (processor == null)
					return false;

				String[] types = ((InstallationItemProcessor) processor).getSupportedMimeTypes();
				if (types == null)
					continue;
				for (int j = 0; j < types.length; j++) {
					if (!mimeTypes.contains(types[j]))
						mimeTypes.add(types[j]);
				}

				String curItemMimeType = installationItem.getMimeType();
				if (mimeTypes.contains(curItemMimeType))
					return true;
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	private boolean isResourceProviderAvailable(IResource resource) {
		IExtensionPoint extensionPoint = registry.getExtensionPoint("org.tigris.mtoolkit.common.installationItemProviders");
		IConfigurationElement[] elements = extensionPoint.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			if (!elements[i].getName().equals("provider")) {
				continue;
			}
			try {
				Object provider = elements[i].createExecutableExtension("class");
				((InstallationItemProvider) provider).init(elements[i]);
				if (provider != null && ((InstallationItemProvider) provider).isCapable(resource)) {
					installationItem = ((InstallationItemProvider) provider).getInstallationItem(resource);
					return true;
				}

			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

}
