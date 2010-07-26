package org.tigris.mtoolkit.common.installation;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * @since 6.0
 */
public class InstallationRegistry {
	private static InstallationRegistry registry = null;
	private List itemProviders = new ArrayList();
	private List itemProcessors = new ArrayList();
	private Map selectionDialogs = new HashMap();

	public static InstallationRegistry getInstance() {
		if (registry == null) {
			registry = new InstallationRegistry();
		}
		return registry;
	}

	public InstallationTarget findTarget(Object target) {
		InstallationTarget result = null;

		Iterator processorsIterator = getProcessors().iterator();
		while (processorsIterator.hasNext()) {
			InstallationItemProcessor element = (InstallationItemProcessor) processorsIterator.next();
			result = element.getInstallationTarget(target);
			if (result != null) {
				return result;
			}
		}
		return result;
	}

	public List/*<InstallationItem>*/getItems(Object source) {
		List items = new ArrayList();
		Iterator providersIterator = getProviders().iterator();
		boolean hasCapableProvider = false;
		while (providersIterator.hasNext()) {
			ProviderElement providerElement = (ProviderElement) providersIterator.next();
			try {
				providerElement.getProvider().init(providerElement.getConfigurationElement());
			} catch (CoreException e) {
				e.printStackTrace();
			}
			if (providerElement.getProvider().isCapable(source)) {
				items.add(providerElement.getProvider().getInstallationItem(source));
				hasCapableProvider = true;
			}
		}
		if (!hasCapableProvider) {
			return null;
		}
		return items;
	}

	public List/*<InstallationItemProviders>*/getProviders() {
		if (itemProviders.isEmpty()) {
			obtainInstallationProviders();
		}
		return itemProviders;
	}

	public List/*<InstallationItemProcessors>*/getProcessors() {
		if (itemProcessors.isEmpty()) {
			obtainInstallationProcessors();
		}
		return itemProcessors;
	}
	
	public Class getSelectionDialog(InstallationItemProcessor itemProcessor) {
		return (Class) selectionDialogs.get(itemProcessor);
	}

	private void obtainInstallationProviders() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint("org.tigris.mtoolkit.common.installationItemProviders");

		obtainProviderElements(extensionPoint.getConfigurationElements(), itemProviders);
	}

	// getSelectionDialog

	private void obtainInstallationProcessors() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint("org.tigris.mtoolkit.common.installationItemProcessors");

		obtainProcessorElements(extensionPoint.getConfigurationElements(), itemProcessors, selectionDialogs);
	}

	/**
	 * Creates instances of item providers for given configuration elements and
	 * adds them to a passed hash table with existing item providers (if a
	 * provider already exists in the table, it is not re-created). For keys are
	 * used the class names of item providers as String objects.
	 * 
	 * @param elements
	 *            array of configuration elements
	 * @param providers
	 *            table with current providers (cannot be null)
	 */
	private void obtainProviderElements(IConfigurationElement[] elements, List providers) {
		for (int i = 0; i < elements.length; i++) {
			if (!elements[i].getName().equals("provider")) {
				continue;
			}
			String clazz = elements[i].getAttribute("class");
			if (clazz == null) {
				continue;
			}

			ProviderElement providerElement = new ProviderElement(elements[i]);
			if (providers.contains(providerElement))
				continue;

			try {
				Object provider = elements[i].createExecutableExtension("class");

				if (provider instanceof InstallationItemProvider) {
					providerElement.setProvider(((InstallationItemProvider) provider));
					providers.add(providerElement);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Creates instances of item processors for given configuration elements and
	 * adds them to a passed hash table with existing item processors (if a
	 * processor already exists in the table, it is not re-created). Class
	 * objects of selection dialogs for each processor are also obtained. Keys
	 * for processors table are class names of item processors as String
	 * objects. Keys for dialogs table are the instances of item processors.
	 * 
	 * @param elements
	 *            array of configuration elements
	 * @param processors
	 *            table with current processors (cannot be null)
	 * @param dialogs
	 * @param dialogs
	 *            table with Class objects of current dialogs (cannot be null)
	 */
	private void obtainProcessorElements(IConfigurationElement[] elements, List processors, Map dialogs) {
		for (int i = 0; i < elements.length; i++) {
			if (!elements[i].getName().equals("processor")) {
				continue;
			}
			//			String clazz = elements[i].getAttribute("class");
			String dlgClassName = elements[i].getAttribute("selectionDialog");
			try {
				Object processor = elements[i].createExecutableExtension("class");

				Class dlgClass = null;
				String classPackageName = dlgClassName.substring(0, dlgClassName.lastIndexOf('.'));
				BundleContext bc = ResourcesPlugin.getPlugin().getBundle().getBundleContext();
				Bundle[] bundles = bc.getBundles();

				for (int index = 0; index < bundles.length; index++) {
					Dictionary dictionary = bundles[index].getHeaders();

					String exportedPackages = (String) dictionary.get("Export-Package");
					if (exportedPackages != null) {
						if (exportedPackages.indexOf(classPackageName) != -1) {
							dlgClass = bundles[index].loadClass(dlgClassName);
						}
					}
				}
				if (dlgClass == null)
					continue;

				if (processor instanceof InstallationItemProcessor
								&& TargetSelectionDialog.class.isAssignableFrom((Class) dlgClass)) {
					processors.add(processor);
					try {
						dialogs.put(processor, dlgClass);
					} catch (AbstractMethodError e) {
						// TODO: handle exception
					}
				}
				dlgClass = null;
			} catch (CoreException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	public class ProviderElement {
		private String extension;
		private String clazz;
		private InstallationItemProvider provider;
		private IConfigurationElement confElement;

		public ProviderElement(IConfigurationElement configurationElement) {
			confElement = configurationElement;
			extension = configurationElement.getAttribute("extension");
			clazz = configurationElement.getAttribute("class");
		}

		public void setProvider(InstallationItemProvider provider) {
			this.provider = provider;
		}

		public IConfigurationElement getConfigurationElement() {
			return confElement;
		}

		public InstallationItemProvider getProvider() {
			return provider;
		}

		public boolean equals(ProviderElement otherElement) {
			if (this.clazz.equals(otherElement.clazz) && this.extension.equals(otherElement.extension))
				return true;
			return false;
		}
	}
}
