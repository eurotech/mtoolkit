/*******************************************************************************
 * Copyright (c) 2011 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.common.installation;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;

/**
 * @since 6.1
 */
public class ProviderSelectionDialog extends ListDialog {

  public ProviderSelectionDialog(Shell parent, List/*<InstallationItemProvider>*/providers, String resName) {
    super(parent);
    setTitle("Select Provider");
    String msg = "Select installation item provider";
    if (resName != null) {
      msg += " for \"" + resName + "\"";
    }
    setMessage(msg);
    setLabelProvider(new ProviderLabelProvider());
    setContentProvider(new ArrayContentProvider());
    setInput(providers);
    setInitialSelections(new Object[] { providers.get(0) });
  }

  /**
   * Returns the selected provider or null if the dialog was canceled.
   * 
   * @return
   */
  public InstallationItemProvider getSelectedProvider() {
    Object[] res = getResult();
    if (res != null && res.length > 0) {
      return (InstallationItemProvider) res[0];
    }
    return null;
  }

  private class ProviderLabelProvider extends LabelProvider {
    private Map images = new Hashtable();

    public String getText(Object element) {
      if (element instanceof InstallationItemProvider) {
        return ((InstallationItemProvider) element).getName();
      }
      return element.toString();
    }

    public Image getImage(Object element) {
      if (element instanceof InstallationItemProvider) {
        ImageDescriptor descriptor = ((InstallationItemProvider) element).getImageDescriptor();
        if (descriptor == null) {
          return null;
        }
        return getImage(descriptor);
      }
      return null;
    }

    private Image getImage(ImageDescriptor descriptor) {
      Image image = (Image) images.get(descriptor);
      if (image == null) {
        image = descriptor.createImage();
        images.put(descriptor, image);
      }
      return image;
    }

    public void dispose() {
      for (Iterator it = images.values().iterator(); it.hasNext();) {
        ((Image) it.next()).dispose();
      }
    }
  }
}
