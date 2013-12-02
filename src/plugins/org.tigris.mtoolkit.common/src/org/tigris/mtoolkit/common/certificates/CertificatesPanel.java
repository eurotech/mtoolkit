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
package org.tigris.mtoolkit.common.certificates;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.tigris.mtoolkit.common.Messages;
import org.tigris.mtoolkit.common.gui.FilteredCheckboxTree;
import org.tigris.mtoolkit.common.gui.FilteredCheckboxTree.FilterableCheckboxTreeViewer;
import org.tigris.mtoolkit.common.images.UIResources;

public final class CertificatesPanel {
  private static final String  MTOOLKIT_PAGE_ID       = "org.tigris.mtoolkit.common.certmanager.internal.preferences.CertPreferencesPage"; //$NON-NLS-1$
  /**
   * @since 5.0
   */
  public static final int      EVENT_CONTENT_MODIFIED = 1;

  private Composite            signContentGroup;
  private Label                lblCertificates;
  private FilteredCheckboxTree tree;
  private TreeViewer           viewer;
  private Tree                 treeControl;
  private Button               detailsButton;
  private Link                 link;
  private Set                  listeners              = new HashSet();
  private Image                iconCertMissing;

  public CertificatesPanel(Composite parent, int horizontalSpan, int verticalSpan) {
    this(parent, horizontalSpan, verticalSpan, GridData.FILL_BOTH);
  }

  public CertificatesPanel(Composite parent, int horizontalSpan, int verticalSpan, int style) {
    this(parent, horizontalSpan, verticalSpan, style, false);
  }

  public CertificatesPanel(Composite parent, int horizontalSpan, int verticalSpan, int style, boolean isComposite) {
    if (isComposite) {
      signContentGroup = new Composite(parent, SWT.NONE);
    } else {
      signContentGroup = new Group(parent, SWT.NONE);
      ((Group) signContentGroup).setText(Messages.CertificatesPanel_signContentGroup);
    }
    initContent(horizontalSpan, verticalSpan, style);
    final Image iconCert = UIResources.getImage(UIResources.CERTIFICATE_ICON);
    ImageDescriptor overlay = UIResources.getImageDescriptor(UIResources.OVR_ERROR_ICON);
    iconCertMissing = new DecorationOverlayIcon(iconCert, overlay, IDecoration.BOTTOM_LEFT).createImage();
  }

  private void initContent(int horizontalSpan, int verticalSpan, int style) {
    GridData gridData = new GridData(style);
    gridData.horizontalSpan = horizontalSpan;
    gridData.verticalSpan = verticalSpan;
    signContentGroup.setLayoutData(gridData);
    signContentGroup.setLayout(new GridLayout(2, false));

    lblCertificates = new Label(signContentGroup, SWT.NONE);
    lblCertificates.setText(Messages.CertificatesPanel_lblCertificates);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    lblCertificates.setLayoutData(gridData);

    // Certificates table
    final int treeStyle = SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.VIRTUAL | style;
    PatternFilter patternFilter = new PatternFilter() {
      @Override
      protected boolean isLeafMatch(Viewer viewer, Object element) {
        ILabelProvider labelProvider = (ILabelProvider) ((StructuredViewer) viewer).getLabelProvider();
        String text = null;
        if (labelProvider instanceof CellLabelProvider && element instanceof CertificateDetails) {
          Control viewerControl = viewer.getControl();
          if (viewerControl instanceof Tree) {
            int columnCount = ((Tree) viewerControl).getColumnCount();
            for (int i = 0; i < columnCount; i++) {
              text = CertificatesPanel.this.getColumnText(element, i);
              if (text == null) {
                return false;
              }
              if (wordMatches(text)) {
                return true;
              }
            }
          }
        }
        return false;
      }
    };
    patternFilter.setIncludeLeadingWildcard(true);
    tree = new FilteredCheckboxTree(signContentGroup, treeStyle, patternFilter);
    viewer = tree.getViewer();
    treeControl = (Tree) viewer.getControl();
    treeControl.setLayoutData(new GridData(GridData.FILL_BOTH));
    treeControl.setLinesVisible(true);
    treeControl.setHeaderVisible(true);
    viewer.setUseHashlookup(true);
    viewer.setLabelProvider(new LabelProvider());
    viewer.setContentProvider(new ITreeContentProvider() {
      /* (non-Javadoc)
       * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
       */
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

      /* (non-Javadoc)
       * @see org.eclipse.jface.viewers.IContentProvider#dispose()
       */
      public void dispose() {
      }

      /* (non-Javadoc)
       * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
       */
      public boolean hasChildren(Object element) {
        return false;
      }

      /* (non-Javadoc)
       * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
       */
      public Object getParent(Object element) {
        if (element instanceof CertificateDetails) {
          return ((CertificateDetails) element).getCertificateDescriptor();
        }
        return null;
      }

      /* (non-Javadoc)
       * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
       */
      public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof CertificateDetails[]) {
          return getElements(parentElement);
        }
        return new Object[0];
      }

      /* (non-Javadoc)
       * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
       */
      public Object[] getElements(Object inputElement) {
        if (inputElement instanceof CertificateDetails[]) {
          return ((CertificateDetails[]) inputElement);
        }
        return new Object[0];
      }
    });
    viewer.addSelectionChangedListener(new ISelectionChangedListener() {
      /* (non-Javadoc)
       * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
       */
      public void selectionChanged(SelectionChangedEvent event) {
        updateButtonState();
      }
    });

    final TreeColumn tc1 = new TreeColumn(treeControl, SWT.LEFT);
    tc1.setWidth(170);
    tc1.setText(Messages.CertificatesPanel_tblCertColIssuedTo);

    final TreeColumn tc2 = new TreeColumn(treeControl, SWT.LEFT);
    tc2.setWidth(160);
    tc2.setText(Messages.CertificatesPanel_tblCertColIssuedBy);

    final TreeColumn tc3 = new TreeColumn(treeControl, SWT.LEFT);
    tc3.setWidth(130);
    tc3.setText(Messages.CertificatesPanel_tblCertColExpirationDate);

    treeControl.addSelectionListener(new SelectionAdapter() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (e.detail == SWT.CHECK) {
          fireModifyEvent();
        }
      }
    });

    detailsButton = new Button(signContentGroup, SWT.PUSH);
    detailsButton.setText(Messages.CertificatesPanel_lblDetails);
    detailsButton.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, true));
    detailsButton.addSelectionListener(new SelectionAdapter() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent e) {
        showCertificateInfo();
      }
    });
    updateButtonState();
    Point computeSize = signContentGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT);
    signContentGroup.setSize(200, computeSize.y);
  }

  /**
   * Initializes the signing certificates with the given list of certificate ids
   * (of type String). This method could be called multiple times.
   *
   * @param signUids
   *          list with certificate ids or <code>null</code>
   */
  public void initialize(List signUids) {
    CertificateDetails[] certDetails = getCertificateDetails(CertUtils.getCertificates());
    viewer.setInput(certDetails);
    if (certDetails == null || certDetails.length == 0) {
      setNoCertificatesAvailable();
    } else {
      if (link != null) {
        disposeNoCertificatesAvailableState();
      }
      if (signUids != null) {
        FilterableCheckboxTreeViewer treeViewer = ((FilterableCheckboxTreeViewer) viewer);
        for (int i = 0; i < certDetails.length; i++) {
          if (signUids.contains(certDetails[i].getCertificateDescriptor().getUid())) {
            treeViewer.setChecked(certDetails[i], true);
          }
        }
      }
    }
    tree.resetFilter();
  }

  private CertificateDetails[] getCertificateDetails(ICertificateDescriptor[] certificates) {
    if (certificates == null || certificates.length == 0) {
      return null;
    }
    CertificateDetails[] certDetails = new CertificateDetails[certificates.length];
    for (int i = 0; i < certificates.length; i++) {
      ICertificateDescriptor certDescriptor = certificates[i];
      certDetails[i] = new CertificateDetails(certDescriptor);
    }
    return certDetails;
  }

  public List getSignCertificateUids() {
    List signUids = new ArrayList();
    if (viewer instanceof FilterableCheckboxTreeViewer) {
      Object[] checkedElements = ((FilterableCheckboxTreeViewer) viewer).getCheckedElements();
      if (checkedElements != null) {
        for (int i = 0; i < checkedElements.length; i++) {
          signUids.add(((CertificateDetails) checkedElements[i]).getCertificateDescriptor().getUid());
        }
      }
    }
    return signUids;
  }

  /**
   * @since 5.0
   */
  public void addEventListener(Listener listener) {
    if (listener != null) {
      listeners.add(listener);
    }
  }

  /**
   * @since 5.0
   */
  public void removeEventListener(Listener listener) {
    if (listener != null) {
      listeners.remove(listener);
    }
  }

  /**
   * @since 6.1
   */
  public void setEditable(boolean editable) {
    viewer.getControl().setEnabled(editable);
  }

  private void fireModifyEvent() {
    if (listeners.isEmpty()) {
      return;
    }
    Event event = new Event();
    event.type = EVENT_CONTENT_MODIFIED;
    for (Iterator it = listeners.iterator(); it.hasNext();) {
      Listener listener = (Listener) it.next();
      listener.handleEvent(event);
    }
  }

  private void setNoCertificatesAvailable() {
    setCertificateControlsVisible(false);
    if (link == null) {
      link = new Link(signContentGroup, SWT.NONE);
      link.setLayoutData(new GridData());
      link.setText(Messages.CertificatesPanel_lblNoCertificates);
      link.addSelectionListener(new SelectionAdapter() {
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        @Override
        public void widgetSelected(SelectionEvent e) {
          Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
          PreferencesUtil.createPreferenceDialogOn(shell, MTOOLKIT_PAGE_ID, null, null).open();
          ICertificateDescriptor certificates[] = CertUtils.getCertificates();
          if (certificates == null || certificates.length == 0) {
            return;
          }
          initialize(null);
        }
      });
    }

    signContentGroup.layout();
  }

  private void disposeNoCertificatesAvailableState() {
    link.dispose();
    link = null;
    setCertificateControlsVisible(true);
    layoutControls();
  }

  private void setCertificateControlsVisible(boolean visible) {
    lblCertificates.setVisible(visible);
    ((GridData) lblCertificates.getLayoutData()).exclude = !visible;

    detailsButton.setVisible(visible);
    ((GridData) detailsButton.getLayoutData()).exclude = !visible;
  }

  private void layoutControls() {
    signContentGroup.layout();

    Composite parent = signContentGroup;
    while (parent != null) {
      if (parent instanceof Shell) {
        Shell shell = (Shell) parent;
        Point size = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        int sizeX = Math.max(shell.getSize().x, size.x);
        int sizeY = Math.max(shell.getSize().y, size.y);
        shell.setBounds(shell.getLocation().x, shell.getLocation().y, sizeX, sizeY);
        break;
      }
      parent = parent.getParent();
    }

    if (signContentGroup.getParent() != null) {
      signContentGroup.getParent().layout();
    }
  }

  private void updateButtonState() {
    IStructuredSelection treeSelection = (IStructuredSelection) viewer.getSelection();
    detailsButton.setEnabled(treeSelection.size() == 1);
  }

  protected void showCertificateInfo() {
    Object data = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
    if (data instanceof CertificateDetails) {
      CertificateDetails certDetails = (CertificateDetails) data;
      CertificateViewerDialog dialog = new CertificateViewerDialog(viewer.getControl().getShell(), certDetails);
      dialog.open();
    }
  }

  private Image getColumnImage(Object element, int columnIndex) {
    if (columnIndex > 0) {
      return null;
    }
    if (!(element instanceof CertificateDetails)) {
      return null;
    }
    ICertificateDescriptor cert = ((CertificateDetails) element).getCertificateDescriptor();
    File keystore = new File(cert.getStoreLocation());
    if (!keystore.exists() || !keystore.isFile()) {
      return iconCertMissing;
    }
    return UIResources.getImage(UIResources.CERTIFICATE_ICON);
  }

  private String getColumnText(Object element, int columnIndex) {
    if (!(element instanceof CertificateDetails)) {
      return null;
    }
    CertificateDetails cert = (CertificateDetails) element;
    switch (columnIndex) {
    case 0:
      String issuedTo = cert.getIssuedTo();
      if (issuedTo == null) {
        Throwable error = cert.getError();
        if (error != null && error instanceof FileNotFoundException) {
          return Messages.CertificatesPanel_lblKeystoreMissing;
        }
        return Messages.CertificatesPanel_lblErrorData;
      }
      return issuedTo;
    case 1:
      return cert.getIssuedBy();
    case 2:
      return cert.getExpirationData();
    default:
      return null;
    }
  }

  private class ToolTipLabelProvider extends CellLabelProvider implements ILabelProvider {

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
     */
    public Image getImage(Object element) {
      return getColumnImage(element, 0);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
     */
    public String getText(Object element) {
      return getColumnText(element, 0);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.CellLabelProvider#update(org.eclipse.jface.viewers.ViewerCell)
     */
    @Override
    public void update(ViewerCell cell) {
    }
  }

  private class LabelProvider extends ToolTipLabelProvider {

    public Image getColumnImage(Object element, int columnIndex) {
      return CertificatesPanel.this.getColumnImage(element, columnIndex);
    }

    public String getColumnText(Object element, int columnIndex) {
      return CertificatesPanel.this.getColumnText(element, columnIndex);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.StyledCellLabelProvider#update(org.eclipse.jface.viewers.ViewerCell)
     */
    @Override
    public void update(ViewerCell cell) {
      Object element = cell.getElement();
      int columnIndex = cell.getColumnIndex();
      String text = getColumnText(element, columnIndex);
      if (columnIndex == 0) {
        int pos = text.indexOf(' ');
        if (pos > 0) {
          String name = text.substring(0, pos);
          List styles = new ArrayList();
          TextStyle style = new TextStyle();
          styles.add(new StyleRange(0, name.length(), style.foreground, style.background));
          styles.add(new StyleRange(name.length() + 1, text.length(), Display.getCurrent().getSystemColor(
              SWT.COLOR_DARK_GRAY), style.background));
          cell.setStyleRanges((StyleRange[]) styles.toArray(new StyleRange[styles.size()]));
        }
      }
      cell.setText(text);
      cell.setImage(getColumnImage(element, columnIndex));
    }
  }
}
