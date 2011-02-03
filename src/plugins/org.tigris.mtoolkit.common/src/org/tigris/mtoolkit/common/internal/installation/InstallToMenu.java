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
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;
import org.tigris.mtoolkit.common.Messages;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.common.installation.InstallationItemProcessor;
import org.tigris.mtoolkit.common.installation.InstallationRegistry;
import org.tigris.mtoolkit.common.installation.InstallationTarget;

public class InstallToMenu extends CompoundContributionItem implements IWorkbenchContribution {
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
				String[] procMimeTypes = processor.getSupportedMimeTypes();
				for (int k = 0; k < procMimeTypes.length; k++) {
					if (procMimeTypes[k].equals(mimeType)) {
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
			InstallationRegistry.getInstance().getSelectionDialog(processor),
			new IShellProvider() {
				public Shell getShell() {
					return menuManager.getMenu().getShell();
				}
			});
		action.setId("Install_to_" + processor.hashCode() + "selection_dlg"); //$NON-NLS-1$
		action.setImageDescriptor(processor.getGeneralTargetImageDescriptor());
		menuManager.add(new ActionContributionItem(action));
	}

	private List getInstallationItems() {
		ISelection selection = selectionService.getSelection();
		if (selection == null) {
			return null;
		}
		if (!(selection instanceof IStructuredSelection))
			return null;
		IStructuredSelection sel = (IStructuredSelection) selection;
		List resources = sel.toList();
		
		List items = new ArrayList();
		Iterator resIterator = resources.iterator();
		while (resIterator.hasNext()) {
			Object res = resIterator.next();
			List resItems = InstallationRegistry.getInstance().getItems(res);
			if (resItems != null && resItems.size() > 0 ) {
				items.addAll(resItems);
				}
			}

		return items;
	}

	private List getCapableProcessors(List installationItems) {
		List capableProcessors = new ArrayList();

		Iterator processorsIterator = InstallationRegistry.getInstance().getProcessors().iterator();
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
}
