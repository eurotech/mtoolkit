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
package org.tigris.mtoolkit.dpeditor.editor.dialog;

import java.util.Vector;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.dpeditor.IHelpContextIds;
import org.tigris.mtoolkit.dpeditor.editor.event.EventConstants;
import org.tigris.mtoolkit.dpeditor.editor.event.TableControlListener;
import org.tigris.mtoolkit.dpeditor.editor.forms.FormWidgetFactory;
import org.tigris.mtoolkit.dpeditor.util.DPPErrorHandler;
import org.tigris.mtoolkit.dpeditor.util.ResourceManager;
import org.tigris.mtoolkit.util.DPPConstants;
import org.tigris.mtoolkit.util.DPPUtilities;
import org.tigris.mtoolkit.util.Header;

/**
 * This class creates modal dialog to add, remove or edit the key-value pair in
 * the Manifest Header of the selected bundle.
 */
public class CustomHeadersDialog extends Dialog implements SelectionListener, ISelectionChangedListener {

  /** Holds the label of the new button */
  public static final String NEW_BUTTON                 = "DPPEditor.New_Button";
  /** Holds the label of the remove button */
  public static final String REMOVE_BUTTON              = "DPPEditor.Remove_Button";
  /** Holds the label of the move up button */
  public static final String UP_BUTTON                  = "DPPEditor.Up_Button";
  /** Holds the label of the move down button */
  public static final String DOWN_BUTTON                = "DPPEditor.Down_Button";

  /** The title of the dialog */
  public static String       TITLE_BUNDLE               = "DPPEditor.BundleHeadersDialog.Title";          //$NON-NLS-1$
  /** The title of the dialog */
  public static String       TITLE_RESOURCE             = "DPPEditor.ResourceHeadersDialog.Title";        //$NON-NLS-1$
  /** The message of equals keys of the ManifestHeader */
  public static final String EQUAL_VALUES_MSG1          = "DPPEditor.BundleHeadersDialog.EqualValuesMsg1";
  /** The message to continue */
  public static final String EQUAL_VALUES_MSG2          = "DPPEditor.BundleHeadersDialog.EqualValuesMsg2";
  /** The error message that headers cannot contains spaces */
  public static final String ERROR_SPACE_VALUE_BUNDLE   = "DPPEditor.BundleHeadersDialog.ErrorSpace";
  /** The error message that headers cannot contains spaces */
  public static final String ERROR_SPACE_VALUE_RESOURCE = "DPPEditor.ResourceHeadersDialog.ErrorSpace";
  /** The error message that header key is not valid */
  public static final String ERROR_IVALID_KEY           = "DPPEditor.BundleHeadersDialog.ErrorKey";

  /** The width of the dialog */
  public static int          SHELL_WIDTH                = 330;
  /** The height of the dialog */
  public static int          SHELL_HEIGHT               = 300;

  /** The shell in which this dialog will be open. */
  private Shell              shell;
  /** The composite of this dialog */
  private Composite          container;
  /** The old location of this dialog */
  private Point              location;
  /** The first location of the dialog */
  private Point              displayLoc;
  /** The size of the dialog's area */
  private Point              areaSize;

  /** Shows is this is the first appearance of the dialog */
  private boolean            isNewDialog;

  /** The Viewer of the header table in dialog */
  private TableViewer        headerTable;
  /** Button, which creates new ManifestHeader and put it into the table */
  private Button             newButton;
  /** Button, which removes the selected ManifestHeader from the table */
  private Button             removeButton;
  /** Button, which moves up the selected ManifestHeader in table */
  private Button             upButton;
  /** Button, which moves down the selected ManifestHeader in table */
  private Button             downButton;

  /** <code>boolean</code> flag that indicates if the table is editable or not */
  private boolean            isTableEditable            = true;

  /**
   * <code>Vector</code>, in which is the all ManifestHeaders, which will be
   * added in bundle
   */
  private Vector             headerVector               = new Vector();
  /** Shows is this a bundle dialog, or a resource dialog */
  private boolean            isBundleDialog             = false;

  public boolean isBundleDialog() {
    return isBundleDialog;
  }

  public void setBundleDialog(boolean isBundleDialog) {
    this.isBundleDialog = isBundleDialog;
  }

  /**
   * Creates the instance of the BundleCustomHeadesDialog in the given parent
   * shell, display position and a size of the dialog.
   *
   * @param parent
   *          a shell which will be the parent of the new instance (cannot be
   *          null)
   * @param displayLoc
   *          a display location of this dialog
   * @param size
   *          a size of the new instance
   */
  public CustomHeadersDialog(Shell parent, Point displayLoc, Point size) {
    super(parent);
    this.setShellStyle(SWT.RESIZE | SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL);
    isNewDialog = true;
    this.displayLoc = displayLoc;
    areaSize = size;
    headerVector = new Vector();
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.
   * Shell)
   */
  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setSize(SHELL_WIDTH, SHELL_HEIGHT);
    if (isNewDialog) {
      shell.setLocation(new Point(displayLoc.x + (areaSize.x / 2 - SHELL_WIDTH / 2), displayLoc.y
          + (areaSize.y / 2 - SHELL_HEIGHT / 2)));
      isNewDialog = false;
    } else {
      shell.setLocation(location);
    }
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    shell.setLayout(layout);
    this.shell = shell;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse
   * .swt.events.SelectionEvent)
   */
  public void widgetDefaultSelected(SelectionEvent e) {
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt
   * .events.SelectionEvent)
   */
  public void widgetSelected(SelectionEvent e) {
    Object obj = e.getSource();
    if (obj instanceof Button) {
      Button source = (Button) obj;
      if (source == newButton) {
        handleNew();
      } else if (source == removeButton) {
        handleRemove();
      } else if (source == upButton) {
        handleUp();
      } else if (source == downButton) {
        handleDown();
      }
    }
  }

  /**
   * Creates new empty Manifest Header and if it not exists in manifest headers
   * in the bundle, adds the created Manifest Header
   */
  private void handleNew() {

    ManifestHeader header = new ManifestHeader();
    boolean hasHeader = false;
    for (int i = 0; i < headerVector.size(); i++) {
      ManifestHeader tmpHeader = (ManifestHeader) headerVector.elementAt(i);
      if (tmpHeader.getKey().equals(header.getKey())) {
        hasHeader = true;
        break;
      }
    }
    if (!hasHeader) {
      headerTable.add(header);
      headerTable.editElement(header, 0);
      headerVector.addElement(header);
      removeButton.setEnabled(false);
      upButton.setEnabled(false);
      downButton.setEnabled(false);
    }
  }

  /**
   * Removes from Manifest Headers table selected ManifestHeader.
   */
  private void handleRemove() {
    Object object = ((IStructuredSelection) headerTable.getSelection()).getFirstElement();
    if (object != null && object instanceof ManifestHeader) {
      ManifestHeader header = (ManifestHeader) object;
      headerTable.remove(header);
      headerVector.removeElement(header);
    }
    headersChanged();
  }

  /**
   * Moves the selected ManifestHeader one position up.
   */
  private void handleUp() {
    Object object = ((IStructuredSelection) headerTable.getSelection()).getFirstElement();
    if (object != null && object instanceof ManifestHeader) {
      ManifestHeader header = (ManifestHeader) object;
      moveHeader(header, true);
    }
    headersChanged();
    setMoveEnable();
    setHeadersInTable();
  }

  /**
   * Moves the selected ManifestHeader one position down.
   */
  private void handleDown() {
    Object object = ((IStructuredSelection) headerTable.getSelection()).getFirstElement();
    if (object != null && object instanceof ManifestHeader) {
      ManifestHeader header = (ManifestHeader) object;
      moveHeader(header, false);
    }
    headersChanged();
    setMoveEnable();
  }

  /**
   * Moves given ManifestHeader one position up or down depends of the given
   * <code>boolean</code> flag.
   *
   * @param header
   *          the ManifestHeader which will be moved
   * @param up
   *          <code>true</code> if ManifestHeader will be moved one position up,
   *          <code>false</code> will be moved one position down
   */
  public void moveHeader(ManifestHeader header, boolean up) {
    int index = headerVector.indexOf(header);
    int newIndex = index;
    if (up) {
      newIndex -= 1;
      if (newIndex < 0) {
        newIndex = index;
      }
    } else {
      newIndex += 1;
      if (newIndex > headerVector.size()) {
        newIndex = index;
      }
    }
    if (newIndex < index) {
      headerVector.insertElementAt(header, newIndex);
      headerVector.removeElementAt(index + 1);
    } else {
      ManifestHeader oldHeader = (ManifestHeader) headerVector.elementAt(newIndex);
      headerVector.insertElementAt(oldHeader, index);
      headerVector.removeElementAt(newIndex + 1);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org
   * .eclipse.jface.viewers.SelectionChangedEvent)
   */
  public void selectionChanged(SelectionChangedEvent event) {
    StructuredSelection selection = (StructuredSelection) event.getSelection();
    if (isTableEditable || removeButton.getEnabled()) {
      updateEnabledButtons();
    }
    if (selection != null) {
      Object first = selection.getFirstElement();
      if (first instanceof ManifestHeader) {
        setMoveEnable();
        String key = ((ManifestHeader) first).getKey().trim();
        if (key.equals(DPPConstants.nameHeader.trim()) || key.equals(DPPConstants.bundleNameHeader.trim())
            || key.equals(DPPConstants.bundleVersionHeader.trim()) || key.equals(DPPConstants.dpMissingHeader.trim())
            || key.equals(DPPConstants.dpCustomizerHeader.trim())) {
          removeButton.setEnabled(false);
        } else {
          if (isTableEditable) {
            removeButton.setEnabled(true);
          }
        }
      }
    }
  }

  /**
   * Sets the receiver's text, which is the string that the window manager will
   * typically display as the receiver's <em>title</em>, to the dialog.
   *
   * @param title
   *          the new title of the dialog
   */
  public void setTitle(String title) {
    if (title != null) {
      container.getShell().setText(title);
    }
  }

  /**
   * Returns the content area of the dialog.
   *
   * @return the content area of the dialog
   */
  public Composite getContainer() {
    return container;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.jface.dialogs.Dialog#close()
   */
  @Override
  public boolean close() {
    Point size = shell.getSize();
    SHELL_WIDTH = size.x;
    SHELL_HEIGHT = size.y;
    location = shell.getLocation();
    return super.close();
  }

  /**
   * Creates and returns the contents of the upper part of this dialog (above
   * the button bar).
   *
   * @param parent
   *          the parent composite to contain the dialog area
   * @return the dialog area control
   *
   * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.CUSTOM_HEADER_DIALOG);
    // create a composite with standard margins and spacing
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
    layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
    layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
    layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));
    applyDialogFont(composite);
    container = createClient(composite);
    if (isBundleDialog()) {
      setTitle(ResourceManager.getString(TITLE_BUNDLE, "")); //$NON-NLS-1$
    } else {
      setTitle(ResourceManager.getString(TITLE_RESOURCE, "")); //$NON-NLS-1$
    }
    return composite;
  }

  /**
   * Checks if the given value is exists as a value of the
   * <code>TableItem</code>s of the given <code>TableViewer</code> and if
   * exists, checks is the <code>TableItem</code> which value is the given value
   * is the same <code>TableItem</code> as a given. In this case returns the
   * index of the <code>TableItem</code>, otherwise returns -1.
   *
   * @param table
   *          the TableViewer, which TableItems will be checked
   * @param item
   *          the TableItem, which will be compare with the viewer's TableItems
   * @param newValue
   *          the new value of the given TableItem
   * @param column
   *          the column index, which text will be compare with the new value
   * @return the index of the TableItem in the given TableViewer or -1 if this
   *         TableItem is not exists in the TableViewer
   */
  public static int itemExists(TableViewer table, TableItem item, String newValue, int column) {
    int n = -1;
    Table parentTable = table.getTable();
    for (int i = 0; i < parentTable.getItemCount(); i++) {
      TableItem currentItem = parentTable.getItem(i);
      if (currentItem.getText(column).equalsIgnoreCase(newValue)) {
        if (!currentItem.equals(item)) {
          return i;
        }
      }
    }
    return n;
  }

  /**
   * Cell modifier to access the data model from a cell editor in an abstract
   * way
   */
  class KeyModifier implements ICellModifier {
    public boolean canModify(Object object, String property) {
      ManifestHeader header = (ManifestHeader) object;
      String key = header.getKey();
      if (key.equals(DPPConstants.nameHeader) || key.equals(DPPConstants.bundleNameHeader)
          || key.equals(DPPConstants.bundleVersionHeader) || key.equals(DPPConstants.dpMissingHeader)
          || key.equals(DPPConstants.dpCustomizerHeader)) {
        return false; // this headers will be modified from the
        // BundlesTable
      }
      return isTableEditable;
    }

    public void modify(Object object, String property, Object value) {
      TableItem item = (TableItem) object;
      ManifestHeader header = (ManifestHeader) item.getData();
      String newValue = value.toString();

      if (property.equals("key")) {
        if (newValue.equals(header.getKey()) && (!newValue.equals(""))) {
          return;
        }

        newValue = newValue.trim();
        if (newValue.indexOf(' ') != -1) {
          if (isBundleDialog()) {
            DPPErrorHandler.showErrorTableDialog(ResourceManager.getString(ERROR_SPACE_VALUE_BUNDLE));
          } else {
            DPPErrorHandler.showErrorTableDialog(ResourceManager.getString(ERROR_SPACE_VALUE_RESOURCE));
          }
          return;
        }
        if (!DPPUtilities.isValidManifestHeader(newValue)) {
          DPPErrorHandler.showErrorTableDialog(ResourceManager.getString(ERROR_IVALID_KEY));
          return;
        }
        if ((!newValue.equals("")) && (itemExists(headerTable, item, newValue, 0) != -1)) {
          DPPErrorHandler.showErrorTableDialog(ResourceManager.getString(EQUAL_VALUES_MSG1) + "\n"
              + ResourceManager.getString(EQUAL_VALUES_MSG2));
          headerVector.remove(newValue);
          return;
        }

        header.setKey(newValue);
        removeButton.setEnabled(true);
      } else if (property.equals("value")) {
        if (newValue.equals(header.getValue())) {
          return;
        }
        header.setValue(newValue, true);
      }
      headerTable.update(header, null);
    }

    public Object getValue(Object object, String property) {
      ManifestHeader header = (ManifestHeader) object;
      if (property.equals("key")) {
        return header.getKey();
      } else if (property.equals("value")) {
        return header.getValue();
      }
      return "";
    }
  }

  /**
   * Content provider which mediates between the viewer's model and the viewer
   * itself.
   */
  class TableContentProvider extends DefaultContentProvider implements IStructuredContentProvider {
    public Object[] getElements(Object parent) {
      if (parent instanceof Vector) {
        Vector vec = (Vector) parent;
        Object[] result = new Object[vec.size()];
        vec.copyInto(result);
        return result;
      }
      return new Object[0];
    }
  }

  /**
   * A label provider which sets the ManifestHeaders value in corresponding
   * columns in the TableViewer.
   */
  class TableLabelProvider extends LabelProvider implements ITableLabelProvider {
    public String getColumnText(Object obj, int index) {
      if (obj instanceof ManifestHeader) {
        ManifestHeader header = (ManifestHeader) obj;
        if (index == 0) {
          return header.getKey();
        }
        if (index == 1) {
          return header.getValue();
        }
      }
      return obj.toString();
    }

    public Image getColumnImage(Object obj, int index) {
      return null;
    }
  }

  /**
   * Creates the custom area of this dialog.
   *
   * @param parent
   *          a composite control which will be the parent of the dialog's
   *          contents
   * @return the created composite
   */
  public Composite createClient(Composite parent) {
    Composite tableContainer = FormWidgetFactory.createComposite(parent);
    tableContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    tableContainer.setLayout(layout);

    createTable(tableContainer);
    createButtons(tableContainer);

    tableContainer.pack();
    return tableContainer;
  }

  /**
   * Creates the table in this dialog
   *
   * @param parent
   *          a composite control which will be the parent of the table's
   *          composite
   */
  private void createTable(Composite parent) {
    Composite container = FormWidgetFactory.createComposite(parent);
    container.setLayout(new GridLayout());
    container.setLayoutData(new GridData(GridData.FILL_BOTH));

    Table table = FormWidgetFactory.createTable(container, SWT.SINGLE | SWT.FULL_SELECTION | SWT.H_SCROLL
        | SWT.V_SCROLL);
    table.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent ev) {
        if (ev.keyCode == 27) {
          if (ev.getSource() instanceof Table) {
            Table table = (Table) ev.getSource();
            if (table.getSelectionIndex() < 0) {
              return;
            }
            TableItem item = table.getItem(table.getSelectionIndex());
            final ManifestHeader header = (ManifestHeader) item.getData();
            if (header.getKey().equals("")) {
              headerVector.removeElement(header);
            }
          }
        }
      }
    });
    table.setLayout(new GridLayout());
    table.setLayoutData(new GridData(GridData.FILL_BOTH));
    table.setHeaderVisible(true);
    table.setLinesVisible(true);

    String[] columnTitles = {
        "Key", "Value"
    };
    for (int i = 0; i < columnTitles.length; i++) {
      TableColumn tableColumn = new TableColumn(table, SWT.NULL);
      tableColumn.setText(columnTitles[i]);
    }

    TableControlListener controlListener = new TableControlListener(table);
    controlListener.setResizeMode(EventConstants.HEADERS_RESIZE_MODE);
    container.addControlListener(controlListener);

    headerTable = new TableViewer(table);
    headerTable.setContentProvider(new TableContentProvider());
    headerTable.setLabelProvider(new TableLabelProvider());
    headerTable.addSelectionChangedListener(this);

    CellEditor[] editors = new CellEditor[] {
        new TextCellEditor(table), new TextCellEditor(table)
    };
    String[] properties = {
        "key", "value"
    };
    headerTable.setCellEditors(editors);
    headerTable.setCellModifier(new KeyModifier());
    headerTable.setColumnProperties(properties);
    setHeadersInTable();

    FormWidgetFactory.paintBordersFor(container);
  }

  /**
   * Creates the table's navigate buttons.
   *
   * @param parent
   *          a composite control which will be the parent of the buttons
   */
  private void createButtons(Composite parent) {
    Composite buttonComposite = FormWidgetFactory.createComposite(parent);
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    buttonComposite.setLayout(layout);
    buttonComposite.setLayoutData(new GridData());

    newButton = FormWidgetFactory.createButton(buttonComposite, ResourceManager.getString(NEW_BUTTON, ""), SWT.PUSH);
    removeButton = FormWidgetFactory.createButton(buttonComposite, ResourceManager.getString(REMOVE_BUTTON, ""),
        SWT.PUSH);
    upButton = FormWidgetFactory.createButton(buttonComposite, ResourceManager.getString(UP_BUTTON, ""), SWT.PUSH);
    downButton = FormWidgetFactory.createButton(buttonComposite, ResourceManager.getString(DOWN_BUTTON, ""), SWT.PUSH);

    newButton.addSelectionListener(this);
    GridData gd = new GridData(GridData.FILL_VERTICAL);
    gd.widthHint = removeButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
    gd.verticalAlignment = GridData.BEGINNING;
    newButton.setLayoutData(gd);

    removeButton.addSelectionListener(this);
    gd = new GridData(GridData.FILL_VERTICAL);
    gd.verticalAlignment = GridData.BEGINNING;
    removeButton.setLayoutData(gd);

    upButton.addSelectionListener(this);
    gd = new GridData(GridData.FILL_VERTICAL);
    gd.widthHint = removeButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
    gd.verticalAlignment = GridData.BEGINNING;
    upButton.setLayoutData(gd);

    downButton.addSelectionListener(this);
    gd = new GridData(GridData.FILL_VERTICAL);
    gd.widthHint = removeButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
    gd.verticalAlignment = GridData.BEGINNING;
    downButton.setLayoutData(gd);

    updateEnabledButtons();
  }

  /**
   * Enables or disables the remove, up and down button, depending on the
   * selection in the table.
   */
  private void updateEnabledButtons() {
    Table table = headerTable.getTable();
    TableItem[] selection = table.getSelection();
    boolean hasSelection = selection.length > 0;
    removeButton.setEnabled(hasSelection);
    setMoveEnable();
  }

  /**
   * Sets the buttons for moving up and down enable or disable depending on the
   * selection in the table.
   */
  public void setMoveEnable() {
    Table table = headerTable.getTable();
    int selectionIndex = table.getSelectionIndex();
    if (selectionIndex == -1) {
      upButton.setEnabled(false);
      downButton.setEnabled(false);
    } else {
      upButton.setEnabled((selectionIndex != 0));
      downButton.setEnabled((selectionIndex != table.getItemCount() - 1));
    }
  }

  /**
   * Refreshes the viewer with information freshly obtained from the viewer's
   * model.
   */
  public void headersChanged() {
    if (headerTable != null) {
      headerTable.refresh();
    }
  }

  /**
   * Sets to the TableViewer the all ManifestHeaders from the given
   * <code>Dictionary</code>
   *
   * @param headers
   *          the all ManifestHeaders which will be appeared in the dialog
   */
  public void setHeaders(Vector headers) {
    Vector hVec = new Vector();
    for (int i = 0; i < headers.size(); i++) {
      Header header = (Header) headers.elementAt(i);
      ManifestHeader mfHeader = new ManifestHeader(header.getKey(), header.getValue());
      if (!hVec.contains(mfHeader)) {
        hVec.addElement(mfHeader);
      }
    }
    headerVector = hVec;
    setHeadersInTable();
  }

  /**
   * Sets all ManifestHeaders in the table.
   */
  private void setHeadersInTable() {
    if (headerTable != null) {
      headerTable.setInput(headerVector);
    }
  }

  /**
   * Returns a <code>Vector</code> filled with all Manifest Headers, which are
   * added in the dialog.
   *
   * @return a <code>Vector</code> with all Manifest headers
   */
  public Vector getHeaders() {
    Vector result = new Vector();
    for (int i = 0; i < headerVector.size(); i++) {
      ManifestHeader mfHeader = (ManifestHeader) headerVector.elementAt(i);
      result.addElement(new Header(mfHeader.getKey(), mfHeader.getValue()));
    }
    return result;
  }
}
