/*******************************************************************************
 * Copyright (c) 2005, 2013 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action;

import java.util.Vector;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.actions.ActionFactory;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ObjectClass;
import org.tigris.mtoolkit.osgimanagement.model.AbstractFrameworkTreeElementAction;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public final class CommonPropertiesAction extends AbstractFrameworkTreeElementAction<Model> {
  private TreeViewer      parentView;
  private Vector<IAction> actions;

  public CommonPropertiesAction(ISelectionProvider provider, String text) {
    super(false, Model.class, provider, text);
    this.parentView = (TreeViewer) provider;
    setEnabled(false);
    actions = new Vector<IAction>();
    setActionDefinitionId(ActionFactory.PROPERTIES.getCommandId());
  }

  public void registerAction(IAction action) {
    actions.addElement(action);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.IStateAction#updateState(org.eclipse.jface.viewers.IStructuredSelection)
   */
  @Override
  public void updateState(IStructuredSelection selection) {
    if (selection.size() != 1) {
      setEnabled(false);
    } else {
      for (int i = 0; i < actions.size(); i++) {
        IAction action = actions.elementAt(i);
        if (action instanceof IStateAction) {
          ((IStateAction) action).updateState(selection);
        }
      }
      super.updateState(selection);
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.model.AbstractFrameworkTreeElementAction#execute(org.tigris.mtoolkit.osgimanagement.model.Model)
   */
  @Override
  protected void execute(Model element) {
    IAction action = null;
    if (element instanceof ObjectClass) {
      action = new ServicePropertiesAction(parentView, Messages.property_action_label);
    } else if (element instanceof Bundle) {
      action = new BundlePropertiesAction(parentView, Messages.property_action_label);
    } else if (element instanceof FrameworkImpl) {
      action = new FrameworkPropertiesAction(parentView, Messages.property_action_label);
    } else {
      for (int i = 0; i < actions.size(); i++) {
        IAction check = actions.elementAt(i);
        if (check.isEnabled()) {
          action = check;
          break;
        }
      }
    }
    if (action != null) {
      action.run();
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.model.AbstractFrameworkTreeElementAction#isEnabledFor(org.tigris.mtoolkit.osgimanagement.model.Model)
   */
  @Override
  protected boolean isEnabledFor(Model element) {
    if ((element instanceof ObjectClass) || (element instanceof Bundle) || (element instanceof FrameworkImpl)
        || checkActionsState(element)) {
      return true;
    } else {
      return false;
    }
  }

  private boolean checkActionsState(Model element) {
    boolean result = false;
    for (int i = 0; i < actions.size(); i++) {
      IAction action = actions.elementAt(i);
      if (action.isEnabled()) {
        result = true;
        break;
      }
    }
    return result;
  }
}
