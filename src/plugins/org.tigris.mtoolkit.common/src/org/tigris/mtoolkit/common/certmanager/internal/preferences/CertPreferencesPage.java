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
package org.tigris.mtoolkit.common.certmanager.internal.preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.tigris.mtoolkit.common.Messages;
import org.tigris.mtoolkit.common.UtilitiesPlugin;
import org.tigris.mtoolkit.common.certificates.CertUtils;
import org.tigris.mtoolkit.common.certificates.CertificateDetails;
import org.tigris.mtoolkit.common.certificates.CertificateViewerDialog;
import org.tigris.mtoolkit.common.certificates.ICertificateDescriptor;
import org.tigris.mtoolkit.common.certmanager.internal.dialogs.CertificateManagementDialog;
import org.tigris.mtoolkit.common.images.UIResources;

public final class CertPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {
  private static final String ATTR_JARSIGNER_LOCATION = "jarsigner.location"; //$NON-NLS-1$

  private TreeViewer          viewer;
  private Button              btnAdd;
  private Button              btnEdit;
  private Button              btnRemove;
  private Button              btnDetails;
  private Button              btnBrowse;
  private Text                txtJarsignerLocation;
  private Image               iconCertMissing;

  public CertPreferencesPage() {
    super();
    final Image iconCert = UIResources.getImage(UIResources.CERTIFICATE_ICON);
    ImageDescriptor overlay = UIResources.getImageDescriptor(UIResources.OVR_ERROR_ICON);
    iconCertMissing = new DecorationOverlayIcon(iconCert, overlay, IDecoration.BOTTOM_LEFT).createImage();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
   */
  @Override
  public Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout(2, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));

    final int treeStyle = SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.VIRTUAL;
    PatternFilter patternFilter = new PatternFilter() {
      @Override
      protected boolean isLeafMatch(Viewer viewer, Object element) {
        ILabelProvider labelProvider = (ILabelProvider) ((StructuredViewer) viewer).getLabelProvider();
        String text = null;
        if (labelProvider instanceof CellLabelProvider && element instanceof CertDescriptor) {
          Control viewerControl = viewer.getControl();
          if (viewerControl instanceof Tree) {
            int columnCount = ((Tree) viewerControl).getColumnCount();
            for (int i = 0; i < columnCount; i++) {
              text = CertPreferencesPage.this.getColumnText(element, i);
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
    FilteredTree filteredTree = new FilteredTree(composite, treeStyle, patternFilter, true);
    viewer = filteredTree.getViewer();
    Tree tree = (Tree) viewer.getControl();
    tree.setLayoutData(new GridData(GridData.FILL_BOTH));
    tree.setLinesVisible(true);
    tree.setHeaderVisible(true);
    viewer.setUseHashlookup(true);
    viewer.setLabelProvider(new LabelProvider());
    viewer.setContentProvider(new CertContentProvider());
    viewer.setInput(CertStorage.getDefault());

    final TreeColumn tc1 = new TreeColumn(tree, SWT.LEFT);
    tc1.setWidth(120);
    tc1.setText(Messages.certs_ColAlias);

    final TreeColumn tc2 = new TreeColumn(tree, SWT.LEFT);
    tc2.setWidth(200);
    tc2.setText(Messages.certs_ColLocation);

    viewer.addSelectionChangedListener(new ISelectionChangedListener() {
      /* (non-Javadoc)
       * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
       */
      public void selectionChanged(SelectionChangedEvent event) {
        updateButtonsState();
      }
    });

    viewer.getControl().addKeyListener(new KeyAdapter() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.events.KeyAdapter#keyPressed(org.eclipse.swt.events.KeyEvent)
       */
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.keyCode == SWT.DEL) {
          removeCertificate();
        }
      }
    });

    Composite btnComposite = new Composite(composite, SWT.NONE);
    btnComposite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
    layout = new GridLayout(1, true);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    btnComposite.setLayout(layout);
    btnAdd = createButton(btnComposite, Messages.certs_btnAdd);
    btnAdd.addSelectionListener(new SelectionAdapter() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent e) {
        addCertificate();
      }
    });

    btnEdit = createButton(btnComposite, Messages.certs_btnEdit);
    btnEdit.addSelectionListener(new SelectionAdapter() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent e) {
        editCertificate();
      }
    });

    btnRemove = createButton(btnComposite, Messages.certs_btnRemove);
    btnRemove.addSelectionListener(new SelectionAdapter() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent e) {
        removeCertificate();
      }
    });

    btnDetails = createButton(btnComposite, Messages.cert_btnDetails);
    btnDetails.addSelectionListener(new SelectionAdapter() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent e) {
        viewCertificate();
      }
    });

    Label label = new Label(composite, SWT.NONE);
    GridData gridData = new GridData();
    gridData.horizontalSpan = 2;
    label.setLayoutData(gridData);
    label.setText(Messages.certs_lblJarsignerLocation);

    txtJarsignerLocation = new Text(composite, SWT.BORDER);
    String location = getJarsignerLocation();
    if (location != null) {
      txtJarsignerLocation.setText(location);
    }
    txtJarsignerLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    btnBrowse = new Button(composite, SWT.PUSH);
    btnBrowse.setText(Messages.browseLabel);
    btnBrowse.addSelectionListener(new SelectionAdapter() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent e) {
        browseLocation();
      }
    });
    btnBrowse.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

    updateButtonsState();
    viewer.refresh();
    return composite;
  }

  private Button createButton(Composite parent, String label) {
    Button button = new Button(parent, SWT.PUSH);
    GridData gridData = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
    button.setLayoutData(gridData);
    button.setText(label);
    return button;
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  public void init(IWorkbench workbench) {
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
   */
  @Override
  public void performDefaults() {
    super.performDefaults();
    CertStorage.getDefault().performDefaults();
    String defLocation = CertUtils.getDefaultJarsignerLocation();
    txtJarsignerLocation.setText(defLocation);
    saveJarsignerLocation(defLocation);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.preference.PreferencePage#performOk()
   */
  @Override
  public boolean performOk() {
    CertStorage.getDefault().save();
    saveJarsignerLocation(txtJarsignerLocation.getText());
    return true;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.preference.PreferencePage#performCancel()
   */
  @Override
  public boolean performCancel() {
    return true;
  }

  private void addCertificate() {
    Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
    TreeItem[] treeItems = viewer.getTree().getItems();
    Vector allItems = new Vector();
    for (int i = 0; i < treeItems.length; i++) {
      TreeItem item = treeItems[i];
      CertDescriptor data = (CertDescriptor) item.getData();
      allItems.add(data);
    }
    CertificateManagementDialog dialog = new CertificateManagementDialog(shell, allItems, Messages.dlgCertMan_titleAdd,
        Messages.dlgCertMan_message_add);
    if (dialog.open() == Dialog.OK) {
      CertDescriptor cert = new CertDescriptor(CertStorage.getDefault().generateCertificateUid());
      cert.setAlias(dialog.alias);
      cert.setStoreLocation(dialog.storeLocation);
      cert.setStoreType(dialog.storeType);
      cert.setStorePass(dialog.storePass);
      cert.setKeyPass(dialog.keyPass);
      CertStorage.getDefault().addCertificate(cert);
    }
  }

  private void editCertificate() {
    IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
    Object el = selection.getFirstElement();
    if (!(el instanceof CertDescriptor)) {
      return;
    }
    CertDescriptor cert = (CertDescriptor) el;
    Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
    TreeItem[] treeItems = viewer.getTree().getItems();
    Vector allItems = new Vector();
    for (int i = 0; i < treeItems.length; i++) {
      TreeItem item = treeItems[i];
      CertDescriptor data = (CertDescriptor) item.getData();
      allItems.add(data);
    }
    CertificateManagementDialog dialog = new CertificateManagementDialog(shell, allItems,
        Messages.dlgCertMan_titleEdit, cert,
        Messages.dlgCertMan_message_edit);
    if (dialog.open() == Dialog.OK) {
      cert.setAlias(dialog.alias);
      cert.setStoreLocation(dialog.storeLocation);
      cert.setStoreType(dialog.storeType);
      cert.setStorePass(dialog.storePass);
      cert.setKeyPass(dialog.keyPass);
      viewer.refresh();
    }
  }

  private void removeCertificate() {
    IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
    @SuppressWarnings("rawtypes")
    Iterator it = selection.iterator();
    while (it.hasNext()) {
      ICertificateDescriptor cert = (ICertificateDescriptor) it.next();
      (CertStorage.getDefault()).removeCertificate(cert);
    }
    viewer.refresh();
  }

  private void viewCertificate() {
    IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
    Object el = selection.getFirstElement();
    if (!(el instanceof CertDescriptor)) {
      return;
    }
    CertDescriptor cert = (CertDescriptor) el;
    CertificateDetails certDetails = new CertificateDetails(cert);
    CertificateViewerDialog dialog = new CertificateViewerDialog(viewer.getControl().getShell(), certDetails);
    dialog.open();
  }

  private void browseLocation() {
    String selectedFile = null;
    String path = txtJarsignerLocation.getText();
    FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);
    dialog.setFilterPath(path);
    dialog.setText("Open");
    selectedFile = dialog.open();

    if (selectedFile != null) {
      txtJarsignerLocation.setText(selectedFile);
      btnBrowse.setFocus();
    }
  }

  private void updateButtonsState() {
    IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
    switch (selection.size()) {
    case 0:
      btnEdit.setEnabled(false);
      btnRemove.setEnabled(false);
      btnDetails.setEnabled(false);
      return;
    case 1:
      btnEdit.setEnabled(true);
      btnRemove.setEnabled(true);
      btnDetails.setEnabled(true);
      return;
    default:
      btnEdit.setEnabled(false);
      btnRemove.setEnabled(true);
      btnDetails.setEnabled(false);
      return;
    }
  }

  private String getJarsignerLocation() {
    UtilitiesPlugin plugin = UtilitiesPlugin.getDefault();
    if (plugin == null) {
      return CertUtils.getDefaultJarsignerLocation();
    }
    IPreferenceStore store = plugin.getPreferenceStore();

    String location = store.getString(ATTR_JARSIGNER_LOCATION);
    if (location.length() == 0) {
      location = CertUtils.getDefaultJarsignerLocation();
    }
    return location;
  }

  private void saveJarsignerLocation(String location) {
    UtilitiesPlugin plugin = UtilitiesPlugin.getDefault();
    if (plugin == null) {
      return;
    }
    IPreferenceStore store = plugin.getPreferenceStore();
    store.setValue(ATTR_JARSIGNER_LOCATION, location);
  }

  private Image getColumnImage(Object element, int columnIndex) {
    if (columnIndex > 0) {
      return null;
    }
    if (!(element instanceof CertDescriptor)) {
      return null;
    }
    ICertificateDescriptor cert = (CertDescriptor) element;
    File keystore = new File(cert.getStoreLocation());
    if (!keystore.exists() || !keystore.isFile()) {
      return iconCertMissing;
    }
    return UIResources.getImage(UIResources.CERTIFICATE_ICON);
  }

  private String getColumnText(Object element, int columnIndex) {
    if (!(element instanceof CertDescriptor)) {
      return null;
    }
    CertDescriptor cert = (CertDescriptor) element;
    switch (columnIndex) {
    case 0:
      return cert.getAlias();
    case 1:
      return cert.getStoreLocation();
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
      return CertPreferencesPage.this.getColumnImage(element, columnIndex);
    }

    public String getColumnText(Object element, int columnIndex) {
      return CertPreferencesPage.this.getColumnText(element, columnIndex);
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
