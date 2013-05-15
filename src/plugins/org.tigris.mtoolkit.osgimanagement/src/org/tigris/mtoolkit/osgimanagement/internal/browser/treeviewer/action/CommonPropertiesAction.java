package org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action;

import java.util.Vector;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ObjectClass;

public final class CommonPropertiesAction extends SelectionProviderAction implements IStateAction {
  private TreeViewer              parentView;
  private SelectionProviderAction action;
  private Vector                  actions;

  public CommonPropertiesAction(ISelectionProvider provider, String text) {
    super(provider, text);
    this.parentView = (TreeViewer) provider;
    this.setEnabled(false);
    actions = new Vector();
    setActionDefinitionId(ActionFactory.PROPERTIES.getCommandId());
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    IStructuredSelection selection = getStructuredSelection();
    Object element = selection.getFirstElement();
    action = null;
    if (element instanceof ObjectClass) {
      action = new ServicePropertiesAction(parentView, Messages.property_action_label);
    } else if (element instanceof Bundle) {
      action = new BundlePropertiesAction(parentView, Messages.property_action_label);
    } else if (element instanceof FrameworkImpl) {
      action = new PropertyAction(parentView, Messages.property_action_label);
    } else {
      for (int i = 0; i < actions.size(); i++) {
        SelectionProviderAction check = (SelectionProviderAction) actions.elementAt(i);
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
   * @see org.eclipse.ui.actions.SelectionProviderAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
   */
  @Override
  public void selectionChanged(IStructuredSelection selection) {
    updateState(selection);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.IStateAction#updateState(org.eclipse.jface.viewers.IStructuredSelection)
   */
  public void updateState(IStructuredSelection selection) {
    if (selection.size() != 1) {
      this.setEnabled(false);
    } else {
      Object element = selection.getFirstElement();
      if ((element instanceof ObjectClass) || (element instanceof Bundle) || (element instanceof FrameworkImpl)
          || checkActionsState(selection)) {
        this.setEnabled(true);
      } else {
        this.setEnabled(false);
      }
    }

  }

  private boolean checkActionsState(IStructuredSelection selection) {
    boolean result = false;
    for (int i = 0; i < actions.size(); i++) {
      IAction action = (IAction) actions.elementAt(i);
      ((IStateAction) action).updateState(selection);
      if (action.isEnabled()) {
        result = true;
        break;
      }
    }
    return result;
  }

  public void registerAction(IAction action) {
    actions.addElement(action);
  }
}
