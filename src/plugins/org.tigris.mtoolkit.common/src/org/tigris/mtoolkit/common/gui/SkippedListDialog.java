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
package org.tigris.mtoolkit.common.gui;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;
import org.tigris.mtoolkit.common.images.UIResources;

/**
 * @since 6.1
 */
public class SkippedListDialog extends ListDialog {

  private Button btnSkipDialog;
  private boolean skipDialog;

  public SkippedListDialog(Shell parentShell, List items) {
    super(parentShell);

    setContentProvider(new DefaultContentProvider());
    setLabelProvider(new DefaultLabelProvider());
    setInput(items.toArray());
    setAddCancelButton(false);
  }

  protected Control createDialogArea(Composite container) {
    Composite dialogArea = (Composite) super.createDialogArea(container);

    btnSkipDialog = new Button(dialogArea, SWT.CHECK);
    btnSkipDialog.setText("Don't show this again");
    btnSkipDialog.setLayoutData(new GridData());

    getTableViewer().setSorter(new ViewerSorter());

    return dialogArea;
  }

  protected void okPressed() {
    skipDialog = btnSkipDialog.getSelection();
    super.okPressed();
  }

  public boolean isSkipInFuture() {
    return skipDialog;
  }

  private class DefaultLabelProvider extends LabelProvider {
    public Image getImage(Object element) {
      return UIResources.getImage(UIResources.PLUGIN_ICON);
    }
  }

  private class DefaultContentProvider implements IStructuredContentProvider {
    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public Object[] getElements(Object inputElement) {
      return (Object[]) inputElement;
    }
  }

}
