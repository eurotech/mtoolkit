package org.tigris.mtoolkit.common.installation;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
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
import org.tigris.mtoolkit.common.UtilitiesPlugin;

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

  /**
   * Returns map containing capable providers (as keys) and installation items
   * (as values) for the given source element.
   * 
   * @param source
   * @return map with provider-item pairs or empty map
   */
  public Map/*<InstallationItemProvider, InstallationItem>*/getItems(Object source) {
    Map items = new Hashtable();
    for (Iterator it = getProviders().iterator(); it.hasNext();) {
      InstallationItemProvider provider = (InstallationItemProvider) it.next();
      if (provider.isCapable(source)) {
        InstallationItem item = provider.getInstallationItem(source);
        if (item != null) {
          items.put(provider, item);
        }
      }
    }
    return items;
  }

  /**
   * Returns the first found installation item for given source with specified
   * mimeType.
   * 
   * @param source
   * @param mimeType
   * @return installation item or null if no item is found
   */
  public InstallationItem getItem(Object source, String mimeType) {
    Map items = getItems(source);
    for (Iterator it = items.values().iterator(); it.hasNext();) {
      InstallationItem item = (InstallationItem) it.next();
      if (item.getMimeType().equals(mimeType)) {
        return item;
      }
    }
    return null;
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
      if (clazz == null)
        continue;

      try {
        Object provider = elements[i].createExecutableExtension("class");

        if (provider instanceof InstallationItemProvider) {
          ((InstallationItemProvider) provider).init(elements[i]);
          providers.add(provider);
        }
      } catch (CoreException e) {
        UtilitiesPlugin.error("Failed to initialize installation provider from "
            + elements[i].getNamespaceIdentifier()
            + " namespace",
      e);
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
      //      String clazz = elements[i].getAttribute("class");
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
 && TargetSelectionDialog.class.isAssignableFrom(dlgClass)) {
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
}
