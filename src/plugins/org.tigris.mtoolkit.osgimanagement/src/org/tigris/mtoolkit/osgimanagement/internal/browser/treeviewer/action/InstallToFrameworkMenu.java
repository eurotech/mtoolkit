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
package org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;
import org.tigris.mtoolkit.osgimanagement.internal.images.ImageHolder;


public class InstallToFrameworkMenu  extends CompoundContributionItem {

  public InstallToFrameworkMenu() {
    super();
  }

  public InstallToFrameworkMenu(String id) {
    super(id);
  }

  protected IContributionItem[] getContributionItems() {
    MenuManager menuManager = new MenuManager(Messages.install_to_menu_label); //$NON-NLS-1$
    FrameWork fws[] = FrameWorkView.getFrameworks();
    int count = 0;
    if (fws != null) {
      for (int i=0; i<fws.length; i++) {
        if (fws[i].isConnected()) {
          Action action = new InstallToFrameworkAction(fws[i]);
          action.setId("Install_to_"+fws[i].getName().hashCode()); //$NON-NLS-1$
          action.setImageDescriptor(ImageHolder.getImageDescriptor(ConstantsDistributor.SERVER_ICON_CONNECTED));
          menuManager.add(new ActionContributionItem(action));
          count++;
        }
      }
    }
    if (count == 0) {
      Action action = new Action(Messages.no_frameworks_connected_label) { //$NON-NLS-1$
      };
      action.setEnabled(false);
      menuManager.add(new ActionContributionItem(action));
    }

    menuManager.setVisible(true);
    return new IContributionItem[] { menuManager };
  }

}
