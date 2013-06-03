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

public final class InstallToMenu extends CompoundContributionItem implements IWorkbenchContribution {
  private ISelectionService selectionService = null;

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
    List mappings = getInstallationMappings();
    if (mappings == null || mappings.size() == 0) {
      return new IContributionItem[0];
    }
    List capableProcessors = getCapableProcessors(mappings);
    if (capableProcessors == null || capableProcessors.size() == 0) {
      return new IContributionItem[0];
    }
    MenuManager menuManager = new MenuManager(Messages.install_to_menu_label);
    Iterator iterator = capableProcessors.iterator();
    boolean first = true;
    while (iterator.hasNext()) {
      if (!first) {
        menuManager.add(new Separator());
      }
      createActions((InstallationItemProcessor) iterator.next(), mappings, menuManager);
      first = false;
    }
    menuManager.setVisible(true);
    return new IContributionItem[] {
      menuManager
    };
  }

  private Action createInstallationAction(InstallationItemProcessor processor, InstallationTarget target, List mappings) {
    return new InstallToAction(processor, null, target, mappings);
  }

  private void createActions(final InstallationItemProcessor processor, final List mappings,
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

  private List/*<Mapping>*/getInstallationMappings() {
    ISelection selection = selectionService.getSelection();
    if (selection == null) {
      return null;
    }
    if (!(selection instanceof IStructuredSelection)) {
      return null;
    }
    IStructuredSelection sel = (IStructuredSelection) selection;
    List resources = sel.toList();
    List mappings = new ArrayList();
    Iterator resIterator = resources.iterator();
    while (resIterator.hasNext()) {
      Object res = resIterator.next();
      Map resItems = InstallationRegistry.getInstance().getItems(res);
      if (resItems != null && resItems.size() > 0) {
        mappings.add(new InstallToAction.Mapping(res, resItems));
      }
    }
    return mappings;
  }

  private List getCapableProcessors(List/*<Mapping>*/installationMappings) {
    List capableProcessors = new ArrayList();
    Iterator processorsIterator = InstallationRegistry.getInstance().getProcessors().iterator();
    while (processorsIterator.hasNext()) {
      InstallationItemProcessor processor = (InstallationItemProcessor) processorsIterator.next();
      Iterator mappingsIterator = installationMappings.iterator();
      boolean isCapable = true;
      String[] supportedTypes = processor.getSupportedMimeTypes();
      while (mappingsIterator.hasNext()) {
        Map items = ((InstallToAction.Mapping) mappingsIterator.next()).providerSpecificItems;
        boolean supportedResource = false;
        for (Iterator it = items.values().iterator(); it.hasNext();) {
          InstallationItem item = (InstallationItem) it.next();
          if (hasMatch(item.getMimeType(), supportedTypes)) {
            supportedResource = true;
            break;
          }
        }
        if (!supportedResource) {
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
}
