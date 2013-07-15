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

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteService;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ObjectClass;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ServicesCategory;
import org.tigris.mtoolkit.osgimanagement.model.AbstractFrameworkTreeElementAction;
import org.tigris.mtoolkit.osgimanagement.model.Framework;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public final class GotoServiceAction extends AbstractFrameworkTreeElementAction<ObjectClass> {
  public GotoServiceAction(ISelectionProvider provider, String text) {
    super(false, ObjectClass.class, provider, text);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.AbstractFrameworkTreeElementAction#execute(org.tigris.mtoolkit.osgimanagement.model.Model)
   */
  @Override
  protected void execute(final ObjectClass objectClass) {
    try {
      RemoteService service = objectClass.getService();
      String searchName = objectClass.getName();
      if (((ServicesCategory) objectClass.getParent()).getType() == ServicesCategory.USED_SERVICES) {
        Model[] services = null;
        FrameworkImpl fw = ((FrameworkImpl) objectClass.findFramework());
        if (fw.getViewType() == Framework.BUNDLES_VIEW) {
          Bundle bundle = fw.findBundleForService(service.getServiceId());
          if (bundle == null) {
            return;
          }
          ServicesCategory category = (ServicesCategory) bundle.getChildren()[0];
          services = category.getChildren();
        } else {
          services = fw.getChildren();
        }
        for (int i = 0; i < services.length; i++) {
          if (services[i].getName().equals(searchName)) {
            StructuredSelection selection = new StructuredSelection(services[i]);
            getSelectionProvider().setSelection(new StructuredSelection(services[i]));
            if (!selection.equals(getSelectionProvider().getSelection())) {
              Object expanded[] = ((TreeViewer) getSelectionProvider()).getExpandedElements();
              try {
                ((TreeViewer) getSelectionProvider()).getTree().setRedraw(false);
                ((TreeViewer) getSelectionProvider()).expandAll();
                ((TreeViewer) getSelectionProvider()).setExpandedElements(expanded);
              } finally {
                ((TreeViewer) getSelectionProvider()).getTree().setRedraw(true);
              }
              getSelectionProvider().setSelection(selection);
            }
            return;
          }
        }
      }
    } catch (IAgentException e) {
      FrameworkPlugin.error(e);
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.AbstractFrameworkTreeElementAction#isEnabled(org.tigris.mtoolkit.osgimanagement.model.Model)
   */
  @Override
  protected boolean isEnabledFor(ObjectClass element) {
    ObjectClass oClass = element;
    if ((oClass.getParent() instanceof FrameworkImpl)) {
      return true;
    }
    ServicesCategory category = (ServicesCategory) oClass.getParent();
    if (category == null || category.getType() == ServicesCategory.REGISTERED_SERVICES) {
      return false;
    }
    return true;
  }
}
