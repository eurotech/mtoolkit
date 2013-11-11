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
package org.tigris.mtoolkit.common.installation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;
import org.tigris.mtoolkit.common.Messages;
import org.tigris.mtoolkit.common.installation.InstallToAction.Mapping;

public class InstallToMenu extends CompoundContributionItem implements IWorkbenchContribution {
  private static final IContributionItem[] NO_CONTRIBUTION_ITEMS = new IContributionItem[0];

  private ISelectionService                selectionService      = null;

  public InstallToMenu() {
    super();
  }

  public InstallToMenu(String id) {
    super(id);
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.menus.IWorkbenchContribution#initialize(org.eclipse.ui.services.IServiceLocator)
   */
  public void initialize(IServiceLocator serviceLocator) {
    selectionService = ((ISelectionService) serviceLocator.getService(ISelectionService.class));
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.actions.CompoundContributionItem#getContributionItems()
   */
  @Override
  protected IContributionItem[] getContributionItems() {
    List<Mapping> mappings = getInstallationMappings();
    if (mappings == null || mappings.size() == 0) {
      return NO_CONTRIBUTION_ITEMS;
    }
    List<InstallationItemProcessor> capableProcessors = getCapableProcessors(mappings);
    if (capableProcessors == null || capableProcessors.size() == 0) {
      return NO_CONTRIBUTION_ITEMS;
    }
    MenuManager menuManager = new MenuManager(getMenuText());
    for (int i = 0; i < capableProcessors.size(); i++) {
      if (i != 0) {
        menuManager.add(new Separator());
      }
      createActions(capableProcessors.get(i), mappings, menuManager);
    }
    menuManager.setVisible(true);
    return new IContributionItem[] {
      menuManager
    };
  }

  protected String getMenuText() {
    return Messages.install_to_menu_label;
  }

  protected boolean isCapable(InstallationItemProcessor processor, List<Mapping> installationMappings) {
    String[] supportedTypes = processor.getSupportedMimeTypes();
    for (Mapping mapping : installationMappings) {
      Map<InstallationItemProvider, InstallationItem> items = mapping.providerSpecificItems;
      boolean supportedResource = false;
      for (InstallationItem item : items.values()) {
        if (hasMatch(item.getMimeType(), supportedTypes)) {
          supportedResource = true;
          break;
        }
      }
      if (!supportedResource) {
        return false;
      }
    }
    return true;
  }

  protected Action createInstallationAction(InstallationItemProcessor processor, InstallationTarget target,
      List<Mapping> mappings) {
    return new InstallToAction(processor, null, target, mappings);
  }

  protected IStructuredSelection getSelection() {
    ISelection selection = selectionService.getSelection();
    if (selection == null) {
      return null;
    }
    if (!(selection instanceof IStructuredSelection)) {
      return null;
    }
    return (IStructuredSelection) selection;
  }

  private void createActions(final InstallationItemProcessor processor, final List<Mapping> mappings,
      final MenuManager menuManager) {
    InstallationTarget[] targets = InstallationHistory.getDefault().getHistory(processor);
    for (int i = 0; i < targets.length; i++) {
      Action action = createInstallationAction(processor, targets[i], mappings);
      action.setId(getClass().getName() + processor.hashCode() + targets[i].getName().hashCode());
      action.setImageDescriptor(targets[i].getIcon());
      menuManager.add(new ActionContributionItem(action));
    }

    Action action = new Action("Select " + processor.getGeneralTargetName() + "...") { //$NON-NLS-1$ //$NON-NLS-2$
      /* (non-Javadoc)
       * @see org.eclipse.jface.action.Action#run()
       */
      @Override
      public void run() {
        InstallationRegistry registry = InstallationRegistry.getInstance();
        TargetSelectionDialog dialog = registry.getSelectionDialog(processor);
        InstallationTarget selectedTarget = dialog.getSelectedTarget(menuManager.getMenu().getShell());
        if (selectedTarget != null) {
          Action installTo = createInstallationAction(processor, selectedTarget, mappings);
          installTo.run();
        }
      }
    };
    action.setId(getClass().getName() + processor.hashCode() + "selection_dlg"); //$NON-NLS-1$
    action.setImageDescriptor(processor.getGeneralTargetImageDescriptor());
    menuManager.add(new ActionContributionItem(action));
  }

  private List<Mapping> getInstallationMappings() {
    IStructuredSelection selection = getSelection();
    if (selection == null) {
      return null;
    }
    final InstallationRegistry instance = InstallationRegistry.getInstance();
    List<Mapping> mappings = new ArrayList<Mapping>();
    Iterator<Object> resIterator = selection.iterator();
    while (resIterator.hasNext()) {
      Object res = resIterator.next();
      Map<InstallationItemProvider, InstallationItem> resItems = instance.getItems(res);
      if (resItems != null && resItems.size() > 0) {
        mappings.add(new Mapping(res, resItems));
      }
    }
    return mappings;
  }

  private List<InstallationItemProcessor> getCapableProcessors(List<Mapping> installationMappings) {
    List<InstallationItemProcessor> capableProcessors = new ArrayList<InstallationItemProcessor>();
    final InstallationRegistry instance = InstallationRegistry.getInstance();
    List<InstallationItemProcessor> processors = instance.getProcessors();
    for (InstallationItemProcessor processor : processors) {
      if (isCapable(processor, installationMappings)) {
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
}
