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

import java.util.Iterator;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.actions.ActionFactory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Category;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ObjectClass;
import org.tigris.mtoolkit.osgimanagement.model.AbstractFrameworkTreeAction;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public final class RefreshAction extends AbstractFrameworkTreeAction {
  private final TreeViewer tree;

  public RefreshAction(ISelectionProvider provider, String label, TreeViewer tree) {
    super(provider, label);
    this.tree = tree;
    setActionDefinitionId(ActionFactory.REFRESH.getCommandId());
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    Iterator iterator = getStructuredSelection().iterator();
    try {
      tree.getTree().setRedraw(false);
      while (iterator.hasNext()) {
        Model node = (Model) iterator.next();
        if (node instanceof FrameworkImpl) {
          FrameworkImpl framework = (FrameworkImpl) node;
          if (framework.isConnected() && !framework.isRefreshing()) {
            framework.refreshAction(tree.getTree());
          }
        } else if (node instanceof Category) {
          Category category = (Category) node;
          ActionsManager.refreshCategoryAction(category);
        } else if (node instanceof ObjectClass) {
          ObjectClass service = (ObjectClass) node;
          ActionsManager.refreshObjectClassAction(service);
        } else if (node instanceof Bundle) {
          Bundle bundle = (Bundle) node;
          ActionsManager.refreshBundleAction(bundle);
        }
      }
    } finally {
      tree.getTree().setRedraw(true);
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.IStateAction#updateState(org.eclipse.jface.viewers.IStructuredSelection)
   */
  @Override
  public void updateState(IStructuredSelection selection) {
    if (selection.size() == 0) {
      setEnabled(false);
      return;
    }
    boolean enabled = true;
    Iterator iterator = selection.iterator();
    boolean isAFrameworkSelected = false;
    while (iterator.hasNext()) {
      Model model = (Model) iterator.next();
      if (model instanceof FrameworkImpl) {
        FrameworkImpl framework = (FrameworkImpl) model;
        if (!framework.isConnected() || framework.isRefreshing() || framework.getConnector() == null
        /*|| FrameworkConnectorFactory.connectJobs.get(framework.getConnector()) != null*/) {
          enabled = false;
          break;
        }
        isAFrameworkSelected = true;
      } else if (isAFrameworkSelected) {
        enabled = false;
        break;
      }

    }
    this.setEnabled(enabled);
  }
}
