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
package org.tigris.mtoolkit.common.internal.installation;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.common.installation.InstallationItemProcessor;
import org.tigris.mtoolkit.common.installation.InstallationItemProvider;
import org.tigris.mtoolkit.common.installation.InstallationTarget;
import org.tigris.mtoolkit.common.installation.TargetSelectionDialog;
import org.tigris.mtoolkit.common.internal.Messages;

public class InstallToMenu extends CompoundContributionItem implements IWorkbenchContribution {
	private List itemProviders = new ArrayList();
	private Hashtable itemProcessors = new Hashtable();
	private Hashtable selectionDialogs = new Hashtable();
	private ISelectionService selectionService = null;

	public InstallToMenu() {
		super();
	}

	public InstallToMenu(String id) {
		super(id);
	}

	protected IContributionItem[] getContributionItems() {
		List installationItems = getInstallationItems();
		if (installationItems == null || installationItems.size() == 0) {
			return new IContributionItem[0];
		}

		List capableProcessors = getCapableProcessors(installationItems);
		if (capableProcessors == null || capableProcessors.size() == 0) {
			return new IContributionItem[0];
		}
		installationItems = removeUnsupportedFromTarget(installationItems, capableProcessors);
		if (installationItems.isEmpty())
			return new IContributionItem[0];

		MenuManager menuManager = new MenuManager(Messages.install_to_menu_label); //$NON-NLS-1$

		Iterator iterator = capableProcessors.iterator();
		boolean first = true;
		while (iterator.hasNext()) {
			if (!first) {
				menuManager.add(new Separator());
			}
			createActions((InstallationItemProcessor) iterator.next(), installationItems, menuManager);
			first = false;
		}

		menuManager.setVisible(true);
		return new IContributionItem[] { menuManager };
	}

	private List removeUnsupportedFromTarget(List installationItems, List capableProcessors) {
		Iterator iter = installationItems.iterator();
		List temp = new ArrayList();
		while (iter.hasNext()) {
			InstallationItem item = (InstallationItem) iter.next();
			String mimeType = item.getMimeType();
			boolean supported = false;
			for (int j = 0; j < capableProcessors.size(); j++) {
				InstallationItemProcessor processor = (InstallationItemProcessor) capableProcessors.get(j);
				InstallationTarget[] targets = processor.getInstallationTargets();
				for (int k = 0; k < targets.length; k++) {
					if (targets[k].isMimeTypeSupported(mimeType)) {
						supported = true;
						break;
					}
				}
				if (supported) {
					temp.add(item);
					break;
				}
			}
		}
		return temp;
	}

	private void createActions(InstallationItemProcessor processor, List items, final MenuManager menuManager) {
		InstallationTarget[] targets = InstallationHistory.getDefault().getHistory(processor);

		for (int i = 0; i < targets.length; i++) {
			Action action = new InstallToAction(processor, targets[i], items);
			action.setId("Install_to_" + processor.hashCode() + targets[i].getName().hashCode()); //$NON-NLS-1$
			action.setImageDescriptor(targets[i].getIcon());
			menuManager.add(new ActionContributionItem(action));
		}

		Action action = new InstallToSelectionDlgAction(processor,
			items,
			(Class) selectionDialogs.get(processor),
			new IShellProvider() {
				public Shell getShell() {
					return menuManager.getMenu().getShell();
				}
			});
		action.setId("Install_to_" + processor.hashCode() + "selection_dlg"); //$NON-NLS-1$
		// action.setImageDescriptor(ImageHolder.getImageDescriptor(ConstantsDistributor.INSTALL_TO_DLG_ICON));
		menuManager.add(new ActionContributionItem(action));
	}

	private void obtainInstallationProviders() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint("org.tigris.mtoolkit.common.installationItemProviders");

		obtainProviderElements(extensionPoint.getConfigurationElements(), itemProviders);
	}

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
				// TODO Log error
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
	 *            table with Class objects of current dialogs (cannot be null)
	 */
	private void obtainProcessorElements(IConfigurationElement[] elements, Hashtable processors, Hashtable dialogs) {
		for (int i = 0; i < elements.length; i++) {
			if (!elements[i].getName().equals("processor")) {
				continue;
			}
			String clazz = elements[i].getAttribute("class");
			String dlgClassName = elements[i].getAttribute("selectionDialog");
			if (clazz == null || processors.containsKey(clazz) || dlgClassName == null) {
				continue;
			}
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
					processors.put(clazz, processor);
					dialogs.put(processor, dlgClass);
				}
				dlgClass = null;
			} catch (CoreException e) {
				// TODO Log error
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private List getInstallationItems() {
		ISelection selection = selectionService.getSelection();
		if (selection == null) {
			return null;
		}
		TreeSelection sel = (TreeSelection) selection;
		List resources = sel.toList();
		List items = new ArrayList();

		obtainInstallationProviders();
		Iterator resIterator = resources.iterator();
		while (resIterator.hasNext()) {
			Object res = resIterator.next();
			Iterator providersIterator = itemProviders.iterator();
			boolean hasCapableProvider = false;
			while (providersIterator.hasNext()) {
				ProviderElement providerElement = (ProviderElement) providersIterator.next();
				try {
					providerElement.getProvider().init(providerElement.getConfigurationElement());
				} catch (CoreException e) {

					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (providerElement.getProvider().isCapable(res)) {
					items.add(providerElement.getProvider().getInstallationItem(res));
					hasCapableProvider = true;
				}
			}
			if (!hasCapableProvider) {
				// there is no provider for one resource of the selection -
				// abort install of all items of selection
				return null;
			}
		}

		return items;
	}

	private List getCapableProcessors(List installationItems) {
		obtainInstallationProcessors();
		List capableProcessors = new ArrayList();
		Iterator processorsIterator = itemProcessors.values().iterator();
		while (processorsIterator.hasNext()) {
			InstallationItemProcessor processor = (InstallationItemProcessor) processorsIterator.next();
			Iterator itemsIterator = installationItems.iterator();
			boolean isCapable = true;
			String[] supportedTypes = processor.getSupportedMimeTypes();
			while (itemsIterator.hasNext()) {
				InstallationItem item = (InstallationItem) itemsIterator.next();
				if (!hasMatch(item.getMimeType(), supportedTypes)) {
					isCapable = false;
					break;
				}
			}
			if (isCapable) {
				capableProcessors.add(processor);
			}
		}
		return capableProcessors;
	}

	private static boolean hasMatch(String type, String[] supported) {
		for (int i = 0; i < supported.length; i++) {
			if (supported[i].equals(type)) {
				return true;
			}
		}
		return false;
	}

	public void initialize(IServiceLocator serviceLocator) {
		selectionService = ((ISelectionService) serviceLocator.getService(ISelectionService.class));
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
