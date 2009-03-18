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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Model;


public class ViewAction extends SelectionProviderAction {

  private String bundlesView = Messages.bundles_view_action_label;
  private String servicesView = Messages.services_view_action_label;
  private TreeViewer tree;

	public ViewAction(ISelectionProvider provider, String text, TreeViewer tree) {
		super(provider, text);
    this.tree = tree;
	}


  /* (non-Javadoc)
   * @see org.eclipse.jface.action.IAction#run()
   */
  public void run() {
    ISelection selection = getSelection();
    Iterator iterator = getStructuredSelection().iterator();
    int viewType = ((FrameWork)getStructuredSelection().getFirstElement()).getViewType();
    while (iterator.hasNext()) {
      FrameWork fw = (FrameWork)iterator.next();
      setViewType(fw, viewType);
    }
    getSelectionProvider().setSelection(selection);
  }
  
  private void setViewType(final FrameWork fw, int viewType) {
    tree.getTree().setRedraw(false);
    Model parent = fw.getParent();
    parent.removeElement(fw);
    final ISelection selection = tree.getSelection();
    try {
      switch (viewType) {
        case FrameWork.BUNDLES_VIEW: {
          tree.collapseToLevel(fw, TreeViewer.ALL_LEVELS);
          fw.setViewType(FrameWork.SERVICES_VIEW);
          setText(bundlesView);
          FrameworkConnectorFactory.updateViewType(fw);
          break;
        }
        case FrameWork.SERVICES_VIEW: {
          tree.collapseToLevel(fw, TreeViewer.ALL_LEVELS);
          fw.setViewType(FrameWork.BUNDLES_VIEW);
          setText(servicesView);
          FrameworkConnectorFactory.updateViewType(fw);
          break;
        }
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
    parent.addElement(fw);

    Display display = Display.getCurrent();
    if (display == null) display = Display.getDefault();
    display.asyncExec(new Runnable() {
      public void run() {
        tree.expandToLevel(fw, 1);
        tree.setSelection(selection, true);
      }
    });
    tree.getTree().setRedraw(true);
  }


	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.SelectionProviderAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
    updateState(selection);
  }
  
  public void updateState(IStructuredSelection selection) {
    if (selection.size() == 0) {
      setEnabled(false);
      return;
    }
    boolean enabled = true;
    
    Iterator iterator = selection.iterator();
    while (iterator.hasNext()) {
      Model model = (Model)iterator.next();
      if (!(model instanceof FrameWork)) {
        enabled = false;
        break;
      }
      FrameWork framework = (FrameWork) model;
      if (!framework.isConnected() || framework.isConnecting()) {
        enabled = false;
        break;
      }
    }
    this.setEnabled(enabled);
    
    if (enabled) {
      FrameWork fw = (FrameWork) selection.getFirstElement(); 
      switch (fw.getViewType()) {
        case FrameWork.BUNDLES_VIEW: {
          setText(servicesView);
          break;
        }
        case FrameWork.SERVICES_VIEW: {
          setText(bundlesView);
        }
      }
    }
 	}
}