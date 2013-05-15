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
package org.tigris.mtoolkit.osgimanagement.dp.actions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.statushandlers.StatusManager;
import org.tigris.mtoolkit.common.gui.PropertiesDialog;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteDP;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.dp.Activator;
import org.tigris.mtoolkit.osgimanagement.dp.model.DeploymentPackage;

public final class DPPropertiesAction extends SelectionProviderAction implements IStateAction {
  private TreeViewer parentView;

  public DPPropertiesAction(ISelectionProvider provider, String label) {
    super(provider, label);
    this.parentView = (TreeViewer) provider;
    setActionDefinitionId(ActionFactory.PROPERTIES.getCommandId());
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    DeploymentPackage dp = (DeploymentPackage) getStructuredSelection().getFirstElement();
    dpPropertiesAction(dp, parentView);
    // needed to update workbench menu and toolbar status
    getSelectionProvider().setSelection(getSelection());
  }

  private void dpPropertiesAction(DeploymentPackage dp, TreeViewer parentView) {
    try {
      RemoteDP rdp = dp.getRemoteDP();
      Shell shell = parentView.getTree().getShell();
      PropertiesDialog propertiesDialog = new PropertiesDialog(shell, "Deployment Package Headers") {
        /* (non-Javadoc)
         * @see org.tigris.mtoolkit.common.gui.PropertiesDialog#attachHelp(org.eclipse.swt.widgets.Composite)
         */
        @Override
        protected void attachHelp(Composite container) {
          PlatformUI.getWorkbench().getHelpSystem().setHelp(container, Activator.PROPERTY_PACKAGE);
        }
      };
      Dictionary headers = new Hashtable();

      String header = "ManifestFile";
      String value = rdp.getHeader(header);
      if (value != null) {
        BufferedReader br = new BufferedReader(new StringReader(value));
        String line = null;
        try {
          while ((line = br.readLine()) != null) {
            headers.put(line.substring(0, line.indexOf(':')), line.substring(line.indexOf(':') + 1));
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else {
        headers.put("DeploymentPackage-SymbolicName", rdp.getHeader("DeploymentPackage-SymbolicName")); //$NON-NLS-1$ //$NON-NLS-2$
        headers.put("DeploymentPackage-Version", rdp.getHeader("DeploymentPackage-Version")); //$NON-NLS-1$ //$NON-NLS-2$

        header = "DeploymentPackage-FixPack"; //$NON-NLS-1$
        value = rdp.getHeader(header);
        if (value != null) {
          headers.put(header, value);
        }

        header = "DeploymentPackage-Copyright";value = rdp.getHeader(header); //$NON-NLS-1$
        if (value != null) {
          headers.put(header, value);
        }

        header = "DeploymentPackage-ContactAddress";value = rdp.getHeader(header); //$NON-NLS-1$
        if (value != null) {
          headers.put(header, value);
        }

        header = "DeploymentPackage-Description";value = rdp.getHeader(header); //$NON-NLS-1$
        if (value != null) {
          headers.put(header, value);
        }

        header = "DeploymentPackage-DocURL";value = rdp.getHeader(header); //$NON-NLS-1$
        if (value != null) {
          headers.put(header, value);
        }

        header = "DeploymentPackage-Vendor";value = rdp.getHeader(header); //$NON-NLS-1$
        if (value != null) {
          headers.put(header, value);
        }

        header = "DeploymentPackage-License";value = rdp.getHeader(header); //$NON-NLS-1$
        if (value != null) {
          headers.put(header, value);
        }

        header = "DeploymentPackage-Icon";value = rdp.getHeader(header); //$NON-NLS-1$
        if (value != null) {
          headers.put(header, value);
        }
      }

      propertiesDialog.create();
      propertiesDialog.getMainControl().setData(headers);
      propertiesDialog.open();

    } catch (IAgentException e) {
      StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
      return;
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
    if (selection.size() == 1 && getStructuredSelection().getFirstElement() instanceof DeploymentPackage) {
      this.setEnabled(true);
    } else {
      this.setEnabled(false);
    }
  }
}
