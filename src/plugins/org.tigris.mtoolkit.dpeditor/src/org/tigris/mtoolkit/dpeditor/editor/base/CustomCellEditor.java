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
package org.tigris.mtoolkit.dpeditor.editor.base;

import java.io.File;
import java.text.MessageFormat;
import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.tigris.mtoolkit.dpeditor.editor.dialog.CustomHeadersDialog;
import org.tigris.mtoolkit.dpeditor.editor.forms.FormWidgetFactory;
import org.tigris.mtoolkit.dpeditor.util.DPPErrorHandler;
import org.tigris.mtoolkit.dpeditor.util.DPPUtil;
import org.tigris.mtoolkit.dpeditor.util.ResourceManager;
import org.tigris.mtoolkit.util.BundleInfo;
import org.tigris.mtoolkit.util.CertificateInfo;
import org.tigris.mtoolkit.util.DPPFile;
import org.tigris.mtoolkit.util.DPPUtilities;
import org.tigris.mtoolkit.util.ResourceInfo;

/**
 * Creates the cell editor for the table with different custom views
 */
public class CustomCellEditor extends CellEditor implements SelectionListener,
KeyListener, FocusListener, TraverseListener, MouseListener {
  public static final int SUPPORTS_CUSTOM_EDITOR = 1;
  public static final int SUPPROTS_TAGS = 2;

  /** Default component style */
  private static final int defaultStyle = SWT.SINGLE;

  /**
   * This constant indicates the number of the column, which value is the
   * bundle path
   */
  public static final int BUNDLE_PATH = 0;
  /**
   * This constant indicates the number of the column, which value is the
   * bundle customizer
   */
  public static final int BUNDLE_CUSTOMIZER = 3;
  /**
   * This constant indicates the number of the column, which value is the
   * bundle missing
   */
  public static final int BUNDLE_MISSING = 4;
  /**
   * This constant indicates the number of the column, which value is the
   * associated with bundle other headers
   */
  public static final int BUNDLE_HEADER = 5;

  /**
   * This constant indicates the number of the column, which value is the
   * resource path
   */
  public static final int RESOURCE_PATH = 0;
  /**
   * This constant indicates the number of the column, which value is the
   * resource missing
   */
  public static final int RESOURCE_MISSING = 2;
  /**
   * This constant indicates the number of the column, which value is the
   * associated with resource other headers
   */
  public static final int RESOURCE_HEADER = 4;

  /**
   * This constant indicates the number of the column, which value is the
   * certificate key store
   */
  public static final int CERT_KEYSTORE = 1;

  /** Holds the Browse button label */
  public static final String BROWSE_BUTTON = "...";

  /**
   * The type of the cell editor, which presents the text field and the browse
   * button
   */
  public static final int TEXT_BUTTON_TYPE = 0;
  /** The type of the cell editor, which presents the check box */
  public static final int CHECK_BOX_TYPE = 1;
  /**
   * The type of the cell editor, which presents the dialog that opens when
   * the browse button is checked
   */
  public static final int DIALOG_TYPE = 2;

  /** The table viewer in which table this cell editor is added */
  TableViewer viewer;
  /** The table in which this cell editor is added */
  Table table;
  /** The parent composite in which all components will be added */
  Composite editorPanel;
  /** The check box button for the corresponding type */
  Button checkBoxButton;
  /** The browse button for the type with the text field and with dialog */
  Button customButton;
  /** The text field for the corresponding cell editor type */
  Text customText;
  /**
   * The bundle's or the resource's path, depending on the type of this cell
   * editor
   */
  String customPath = "";
  /** The table editor */
  TableEditor editor;
  int i, j;
  int supports = 0;
  /** The column in the table for which this cell editor will be responsible */
  int column = 1;
  /** The composite in which all components in this cell editor will be added */
  Composite parentComposite;

  /** The ModifyListener */
  ModifyListener modifyListener;

  /** The type of this cell editor */
  int type = TEXT_BUTTON_TYPE;

  /**
   * The new instance of this cell editor for the specified viewer, table,
   * column and type. The column must be one of the constant:
   * <code>BUNDLE_PATH</code>; <code>BUNDLE_CUSTOMIZER</code>;
   * <code>BUNDLE_MISSING</code>; <code>BUNDLE_HEADER</code>;
   * <code>RESOURCE_PATH<code>;
   * <code>RESOURCE_MISSING</code>; <code>CERT_KEYSTORE</code>
   * 
   * The type of the cell editor must be : <code>TEXT_BUTTON_TYPE</code>;
   * <code>CHECK_BOX_TYPE</code>; <code>DIALOG_TYPE</code>
   * 
   * @param parent
   *            a composite control which will be the parent of the new
   *            instance
   * @param viewer
   *            the table viewer in which table this cell editor will be added
   * @param table
   *            the table which column will be associated with this cell
   *            editor
   * @param type
   *            the type of the editor
   * @param column
   *            the column, which values this cell editor will be represented
   */
  public CustomCellEditor(Composite parent, TableViewer viewer, Table table,
      int type, int column) {
    super(table, defaultStyle);
    this.viewer = viewer;
    parentComposite = parent;
    this.table = table;
    this.type = type;
    if (type == DIALOG_TYPE) {
      customText.setEditable(false);
    }
    this.column = column;
    editor = new TableEditor(table);
  }

  String startText;

  /**
   * Returns the control of this cell editor or creates new one if there are
   * no created.
   * 
   * @param i
   *            the row of the table
   * @param j
   *            the column of the table
   * @param value
   *            the value of this cell editor
   * @return the control of this cell editor
   */
  public Control getTableCellEditorComponent(int i, int j, Object value) {
    this.i = i;
    this.j = j;
    stopped = false;
    if (value instanceof BundleInfo || value instanceof ResourceInfo || value instanceof CertificateInfo) {
      if (value instanceof BundleInfo) {
        BundleInfo info = (BundleInfo) value;
        if (j == BUNDLE_PATH) { // property bundle
          customText.setText(info.getBundlePath());
          customPath = info.getBundlePath();
          startText = customText.getText();
        } else if (j == BUNDLE_CUSTOMIZER) { // property customizer
          startText = "";
          checkBoxButton.setSelection(info.isCustomizer());
        } else if (j == BUNDLE_MISSING) { // property missing
          startText = "";
          checkBoxButton.setSelection(info.isMissing());
        } else if (j == BUNDLE_HEADER) { // property custom headers
          customText.setText(info.otherHeadersToString());
          startText = customText.getText();
        }
      } else if (value instanceof ResourceInfo) {
        ResourceInfo info = (ResourceInfo) value;
        if (j == RESOURCE_PATH) { // property resource
          customText.setText(info.getResourcePath());
          customPath = info.getResourcePath();
          startText = customText.getText();
        } else if (j == RESOURCE_MISSING) { // property missing
          startText = "";
          checkBoxButton.setSelection(info.isMissing());
        } else if (j == RESOURCE_HEADER) {// property custom headers
          customText.setText(info.otherHeadersToString());
          startText = customText.getText();
        }
      } else if (value instanceof CertificateInfo) {
        CertificateInfo info = (CertificateInfo) value;
        if (j == CERT_KEYSTORE) {
          customText.setText(info.getKeystore());
          startText = customText.getText();
        }
      }
    } else if (value instanceof String) {
      Object object = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
      if (object != null) {
        if (object instanceof BundleInfo) {
          BundleInfo info = (BundleInfo) object;
          if (j == BUNDLE_PATH) { // property bundle
            customText.setText(info.getBundlePath());
            customPath = info.getBundlePath();
            startText = customText.getText();
          } else if (j == BUNDLE_HEADER) { // property custom headers
            customText.setText(info.otherHeadersToString());
            startText = customText.getText();
          }
        } else if (object instanceof ResourceInfo) {
          ResourceInfo info = (ResourceInfo) object;
          if (j == RESOURCE_PATH) { // property resource
            customText.setText(info.getResourcePath());
            customPath = info.getResourcePath();
            startText = customText.getText();
          } else if (j == RESOURCE_HEADER) {// property custom headers
            customText.setText(info.otherHeadersToString());
            startText = customText.getText();
          }
        } else if (object instanceof CertificateInfo) {
          CertificateInfo info = (CertificateInfo) object;
          if (j == CERT_KEYSTORE) { // property keystore
            customText.setText(info.getKeystore());
            startText = customText.getText();
          }
        }
      }
    } else if (value instanceof Boolean) {
      Object object = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
      if (object != null) {
        if (object instanceof BundleInfo) {
          BundleInfo info = (BundleInfo) object;
          if (j == BUNDLE_CUSTOMIZER) { // property customizer
            startText = "";
            checkBoxButton.setSelection(info.isCustomizer());
          } else if (j == BUNDLE_MISSING) { // property missing
            startText = "";
            checkBoxButton.setSelection(info.isMissing());
          }
        } else if (object instanceof ResourceInfo) {
          ResourceInfo info = (ResourceInfo) object;
          if (j == RESOURCE_MISSING) { // property missing
            startText = "";
            checkBoxButton.setSelection(info.isMissing());
          }
        }
      }
    }
    return editorPanel;
  }

  public boolean stopped;

  /**
   * Finishes up the cell editing. Used when another cell is gaining the
   * focus.
   */
  public void externalStop() {
    if (ignoreStop) {
      return;
    }
    if (!stopped) {
      stopCellEditing();
    }
    stopped = true;
  }

  /**
   * Stops the editing in this cell editor
   * 
   * @return <code>false</code> if there are no editing in this cell editor,
   *         otherwise return <code>true</code>
   */
  public boolean stopCellEditing() {
    String stopText;
    if ((supports & SUPPROTS_TAGS) != 0) {
      stopText = "";
    } else {
      stopText = customText.getText();
    }
    if (stopText.equals(startText)) {
      return false;
    }
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.swt.events.KeyListener#keyPressed(org.eclipse.swt.events.
   * KeyEvent )
   */
  public void keyPressed(KeyEvent e) {
    if (e.getSource() == table) {
      return;
    }
    if (e.keyCode == 13 || e.keyCode == SWT.KEYPAD_CR) {
      setInfoValue();
      table.update();
      viewer.refresh();
      return;
    }
    if (e.character == SWT.ESC) {
      if (e.getSource() == customText) {
        String oldValue = "";
        try {
          oldValue = setInfoValue();
        } catch (IllegalArgumentException ex) {
        } finally {
          if (oldValue == null || oldValue.equals("")) {
            customText.setText("");
          } else {
            customText.setText(oldValue);
          }
          editorPanel.forceFocus();
        }
      } else if (e.getSource() == checkBoxButton) {
        Object data = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
        if (data != null) {
          TableItem[] items = viewer.getTable().getSelection();
          TableItem modifyItem = null;
          if (items != null && items.length == 1) {
            modifyItem = items[0];
          }
          if (data instanceof BundleInfo) {
            BundleInfo info = (BundleInfo) data;
            if (column == BUNDLE_CUSTOMIZER) { // property
              // customizer
              boolean customizer = info.isCustomizer();
              boolean newCustomizer = checkBoxButton == null ? false : checkBoxButton.getSelection();
              if ((customizer && !newCustomizer) || (!customizer && newCustomizer)) {
                viewer.getCellModifier().modify(modifyItem, "customizer", "" + newCustomizer);
              }
            } else if (column == BUNDLE_MISSING) { // property
              // missing
              boolean missing = info.isMissing();
              boolean newMissing = checkBoxButton == null ? false : checkBoxButton.getSelection();
              if ((missing && !newMissing) || (!missing && newMissing)) {
                viewer.getCellModifier().modify(modifyItem, "missing", "" + newMissing);
              }
            }
          } else if (data instanceof ResourceInfo) {
            ResourceInfo info = (ResourceInfo) data;
            if (column == RESOURCE_MISSING) { // property missing
              boolean missing = info.isMissing();
              boolean newMissing = checkBoxButton == null ? false : checkBoxButton.getSelection();
              if ((missing && !newMissing) || (!missing && newMissing)) {
                viewer.getCellModifier().modify(modifyItem, "missing", "" + newMissing);
              }
            }
          }
        }
      }
      if (e.character == SWT.ESC) {
        stopped = true;
        table.setFocus();
        return;
      }
      stopCellEditing();
      table.setFocus();
    } else {
      if ((e.character == 'x' || e.character == 'X') && ((e.stateMask & SWT.CTRL) != 0)) {
        if (customText != null) {
          customText.cut();
        }
      }
    }
  }

  /**
   * Gets the selection in the viewer and depends on the selection data sets
   * the corresponding value that this cell editor holds to the data. Returns
   * the old value of the data.
   * 
   * @return the old value of the selected in the viewer data
   */
  private String setInfoValue() {
    String oldValue = "";
    if (viewer != null && viewer.getSelection() != null) {
      Object data = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
      if (data != null) {
        TableItem[] items = viewer.getTable().getSelection();
        TableItem modifyItem = null;
        if (items != null && items.length == 1) {
          modifyItem = items[0];
        }
        if (data instanceof BundleInfo) {
          BundleInfo info = (BundleInfo) data;
          if (column == BUNDLE_PATH) { // property bundle
            oldValue = info.getBundlePath();
            String str = customText == null ? "" : customText.getText();
            if (modifyItem != null) {
              if (str.endsWith(".project") && !str.equals("<.>" + File.separator + ".project")) {
                IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                IProject[] projects = root.getProjects();
                boolean isFromWorkspace = false;
                for (int j = 0; j < projects.length; j++) {
                  IProject prj = projects[j];
                  String location = prj.getLocation().toOSString();
                  if (str.startsWith(location + File.separator)) {
                    isFromWorkspace = true;
                    break;
                  }
                }
                if (!isFromWorkspace) {
                  if (oldValue != null)
                    customText.setText(oldValue);
                  DPPErrorHandler.processError(ResourceManager.format("DPPEditor.ProjectError", new String[] { str }), true);
                } else {
                  viewer.getCellModifier().modify(modifyItem, "bundle", str);
                }
              } else {
                viewer.getCellModifier().modify(modifyItem, "bundle", str);
              }
            }
          } else if (column == BUNDLE_CUSTOMIZER) { // property
            // customizer
            boolean customizer = info.isCustomizer();
            boolean newCustomizer = checkBoxButton == null ? false : checkBoxButton.getSelection();
            if ((customizer && !newCustomizer) || (!customizer && newCustomizer)) {
              viewer.getCellModifier().modify(modifyItem, "customizer", "" + newCustomizer);
            }
          } else if (column == BUNDLE_MISSING) { // property missing
            boolean missing = info.isMissing();
            boolean newMissing = checkBoxButton == null ? false : checkBoxButton.getSelection();
            if ((missing && !newMissing) || (!missing && newMissing)) {
              viewer.getCellModifier().modify(modifyItem, "missing", "" + newMissing);
            }
          } else if (column == BUNDLE_HEADER) { // property custom
            oldValue = info.otherHeadersToString();
            oldValue = oldValue == null ? "" : oldValue;
            String newValue = customText == null ? "" : customText.getText();
            if (!newValue.equals(oldValue)) {
              viewer.getCellModifier().modify(modifyItem, "custom", newValue);
            }
          }
        } else if (data instanceof ResourceInfo) {
          ResourceInfo info = (ResourceInfo) data;
          if (column == RESOURCE_PATH) { // property resource
            oldValue = info.getResourcePath();
            oldValue = oldValue == null ? "" : oldValue;
            String newValue = customText == null ? "" : customText.getText();
            if (!oldValue.equals(newValue)) {
              viewer.getCellModifier().modify(modifyItem, "resource", newValue);
            }
          } else if (column == RESOURCE_MISSING) { // property missing
            boolean missing = info.isMissing();
            boolean newMissing = checkBoxButton == null ? false : checkBoxButton.getSelection();
            if ((missing && !newMissing) || (!missing && newMissing)) {
              viewer.getCellModifier().modify(modifyItem, "missing", "" + newMissing);
            }
          } else if (column == RESOURCE_HEADER) { // property custom

            oldValue = info.otherHeadersToString();
            oldValue = oldValue == null ? "" : oldValue;
            String newValue = customText == null ? "" : customText.getText();
            if (!newValue.equals(oldValue)) {
              viewer.getCellModifier().modify(modifyItem, "custom", newValue);
            }
          }
        } else if (data instanceof CertificateInfo) {
          CertificateInfo info = (CertificateInfo) data;
          if (column == CERT_KEYSTORE) {
            oldValue = info.getKeystore();
            if(items!=null){
              for (int i = 0; i < items.length; i++) {
                TableItem tableItem = items[i];
                CertificateInfo certInfo = (CertificateInfo) tableItem.getData();
                if (certInfo.getAlias() != null && certInfo.getAlias().equals(info.getAlias())) {
                  modifyItem = tableItem;
                }
              }
            }
            if (modifyItem != null) {
              viewer.getCellModifier().modify(modifyItem, "keystore", customText == null ? "" : customText.getText());
            }
          }
        }
      }
    }
    return oldValue;
  }

  /**
   * Gets the selection in the viewer and depends on the selection data gets
   * the value of the data.
   * 
   * @return the value of the selected in the viewer data
   */
  private String getStringInfoValue() {
    String value = "";
    Object data = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
    if (data != null) {
      if (data instanceof BundleInfo) {
        BundleInfo info = (BundleInfo) data;
        if (column == BUNDLE_PATH) { // property bundle
          customPath = info.getBundlePath();
          value = customPath;
        } else if (column == BUNDLE_HEADER) { // property custom
          value = info.otherHeadersToString();
        }
      } else if (data instanceof ResourceInfo) {
        ResourceInfo info = (ResourceInfo) data;
        if (column == RESOURCE_PATH) { // property resource
          customPath = info.getResourcePath();
          value = customPath;
        } else if (column == RESOURCE_HEADER) {// property custom
          value = info.otherHeadersToString();
        }
      } else if (data instanceof CertificateInfo) {
        CertificateInfo info = (CertificateInfo) data;
        if (column == CERT_KEYSTORE) { // property keystore
          value = info.getKeystore();
        }
      }
    }
    return value;
  }

  /**
   * Gets the selection in the viewer and depends on the selection data gets
   * the value of the data only for the boolean values.
   * 
   * @return the value of the selected in the viewer data
   */
  private boolean getInfoSelection() {
    Object data = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
    if (data != null) {
      if (data instanceof BundleInfo) {
        BundleInfo info = (BundleInfo) data;
        if (column == BUNDLE_CUSTOMIZER) { // property customizer
          return info.isCustomizer();
        } else if (column == BUNDLE_MISSING) { // property missing
          return info.isMissing();
        }
      } else if (data instanceof ResourceInfo) {
        ResourceInfo info = (ResourceInfo) data;
        if (column == RESOURCE_MISSING) { // property missing
          return info.isMissing();
        }
      }
    }
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.swt.events.KeyListener#keyReleased(org.eclipse.swt.events
   * .KeyEvent )
   */
  public void keyReleased(KeyEvent e) {
  }

  /**
   * Opens file dialog chooser and sets the chosen file to the text field.
   * This method is called only when the type of this cell editor is
   * <code>TEXT_BUTTON_TYPE</code>
   */
  protected void browseAction() {
    boolean isKeyStore = column == CERT_KEYSTORE;
    boolean isResource = false;
    String filter = "*.*";
    String prjFilter = "*.project";
    if (isKeyStore) {
      filter = "*.keystore";
    } else {
      Object data = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
      if (data != null) {
        if (data instanceof BundleInfo) {
          filter = "*.jar;*.project";
        } else if (data instanceof ResourceInfo) {
          isResource = true;
        }
      }
    }
    String[] extNames = new String[] { filter };
    if (isKeyStore) {
      extNames = new String[] { filter, "*.*" };
    } else if (!isResource) {
      extNames = new String[] { filter, prjFilter };
    }
    String[] ext = new String[] { filter };
    if (isKeyStore) {
      ext = new String[] { filter, "*.*" };
    } else if (!isResource) {
      ext = new String[] { filter, prjFilter };
    }
    FileDialog dialog = new FileDialog(DPPErrorHandler.getShell(), SWT.OPEN | SWT.MULTI);
    dialog.setText(ResourceManager.getString("DPPEditor.FileChooserTitle"));
    dialog.setFilterNames(extNames);
    dialog.setFilterExtensions(ext);
    String path = DPPUtil.getFileDialogPath(customPath);
    dialog.setFilterPath(path);

    String selectedFile = dialog.open();
    String allFiles[] = dialog.getFileNames();
    int row = table.getSelectionIndex();
    if (allFiles.length > 1) {
      DPPFile dppFile = (DPPFile) viewer.getInput();
      String folder = (new File(selectedFile)).getParent() + File.separator;
      folder = dppFile.convertToRelative(folder);

      if (isKeyStore) {
        Vector infos = dppFile.getCertificateInfos();
        for (int i = 0; i < allFiles.length; i++) {
          String newPath = folder + allFiles[i];
          boolean found = false;
          for (int j = 0; j < infos.size(); j++) {
            if (newPath.equals(((CertificateInfo) infos.elementAt(j)).getKeystore())) {
              found = true;
              break;
            }
          }
          if (found)
            continue;
          CertificateInfo cInfo = null;
          if (i == 0) {
            cInfo = (CertificateInfo) infos.elementAt(row);
            customText.setText(newPath);
          } else {
            cInfo = new CertificateInfo();
            infos.addElement(cInfo);
          }
          cInfo.setKeystore(newPath);
        }
      } else if (isResource) {
        Vector infos = dppFile.getResourceInfos();
        String resPath = "resources/";
        if (infos.size() > 1) {
          String currResPath = ((ResourceInfo) infos.elementAt(row)).getName();
          if (currResPath != null && !currResPath.equals("")) {
            resPath = currResPath;
          } else {
            int prevRow = Math.max(0, row - 1);
            resPath = ((ResourceInfo) infos.elementAt(prevRow)).getName();
          }
          if (resPath.indexOf('/') != -1 || resPath.indexOf('\\') != -1) {
            resPath = resPath.substring(0, Math.max(resPath.indexOf('/'), resPath.indexOf('\\'))) + '/';
          } else {
            resPath = "";
          }
        }
        for (int i = 0; i < allFiles.length; i++) {
          String newPath = folder + allFiles[i];
          boolean found = false;
          for (int j = 0; j < infos.size(); j++) {
            if (newPath.equals(((ResourceInfo) infos.elementAt(j)).getResourcePath())) {
              found = true;
              break;
            }
          }
          if (found)
            continue;

          ResourceInfo rInfo = null;
          if (i == 0) {
            rInfo = (ResourceInfo) infos.elementAt(row);
            customText.setText(newPath);
          } else {
            rInfo = new ResourceInfo();
            infos.addElement(rInfo);
          }
          rInfo.setResourcePath(newPath);

          rInfo.setName(findResourceName(resPath + allFiles[i], infos));
        }
      } else {
        Vector infos = dppFile.getBundleInfos();
        String bundlePath = "bundles/";
        if (infos.size() > 1) {
          String currBundlePath = ((BundleInfo) infos.elementAt(row)).getName();
          if (currBundlePath != null && !currBundlePath.equals("")) {
            bundlePath = currBundlePath;
          } else {
            int prevRow = Math.max(0, row - 1);
            bundlePath = ((BundleInfo) infos.elementAt(prevRow)).getName();
          }
          if (bundlePath.indexOf('/') != -1 || bundlePath.indexOf('\\') != -1) {
            bundlePath = bundlePath.substring(0, Math.max(bundlePath.indexOf('/'), bundlePath.indexOf('\\'))) + '/';
          } else {
            bundlePath = "";
          }
        }
        for (int i = 0; i < allFiles.length; i++) {
          String newPath = folder + allFiles[i];
          boolean found = false;
          for (int j = 0; j < infos.size(); j++) {
            if (newPath.equals(((BundleInfo) infos.elementAt(j)).getBundlePath())) {
              found = true;
              break;
            }
          }
          if (found)
            continue;
          if (newPath.endsWith(".project")) {
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IProject[] projects = root.getProjects();
            boolean isFromWorkspace = false;
            for (int j = 0; j < projects.length; j++) {
              IProject prj = projects[j];
              String location = prj.getLocation().toOSString();
              location = dppFile.convertToRelative(location + File.separator);
              if (newPath.startsWith(location /* + File.separator */)) {
                isFromWorkspace = true;
                break;
              }
            }
            if (!isFromWorkspace)
              continue;
          }

          BundleInfo bInfo = null;
          if (i == 0 || ((BundleInfo) infos.elementAt(row)).getBundlePath() == null) {
            bInfo = (BundleInfo) infos.elementAt(row);
            customText.setText(newPath);
          } else {
            bInfo = new BundleInfo();
            infos.addElement(bInfo);
          }
          bInfo.setBundlePath(dppFile.convertToAbsolute(newPath));
          DPPUtil.updateBundleData(bInfo, dppFile.getProjectLocation());
          String name = "";
          if (allFiles[i].endsWith(".project")) {
            name = bInfo.getBundleSymbolicName();
            if (name.indexOf(';') != -1)
              name = name.substring(0, name.indexOf(';'));
            name = bundlePath + name + ".jar";
          } else {
            name = bundlePath + allFiles[i];
          }
          bInfo.setName(findBundleName(name, infos));
        }
      }
      if (selectedFile != null) {
        setInfoValue();
        table.update();
        viewer.refresh();
      }
      customText.setFocus();
    } else {
      if (selectedFile != null) {
        DPPUtil.fileDialogLastSelection = selectedFile;
        if (!isKeyStore) {
          if (isResource) {
          } else {
            while (selectedFile != null && !selectedFile.endsWith(".jar") && !selectedFile.endsWith(".project")) {
              DPPErrorHandler.showErrorTableDialog(ResourceManager.getString("DPPEditor.BundleExtError"));
              selectedFile = dialog.open();
              DPPUtil.fileDialogLastSelection = selectedFile;
            }
          }
        }
        if (selectedFile != null) {
          customText.setText(selectedFile);
          setInfoValue();
          table.update();
          viewer.refresh();
          customText.setFocus();
        }
      }
    }
  }

  public String findResourceName(String name, Vector infos) {
    boolean duplicate = isResourceNameAdded(infos, name);
    if (duplicate) {
      int counter = 2;
      String ext = name.substring(name.lastIndexOf('.') + 1);
      name = name.substring(0, name.length() - ext.length() - 1);
      while (duplicate) {
        String newName = name + "_" + counter + "." + ext;
        duplicate = isResourceNameAdded(infos, newName);
        if (!duplicate) {
          name = newName;
          break;
        }
        counter++;
      }
    }
    return name;
  }

  public boolean isResourceNameAdded(Vector infos, String name) {
    for (int j = 0; j < infos.size() - 1; j++) {
      if (name.equals(((ResourceInfo) infos.elementAt(j)).getName())) {
        return true;
      }
    }
    return false;
  }

  public String findBundleName(String name, Vector infos) {
    boolean duplicate = isBundleNameAdded(infos, name);
    if (duplicate) {
      int counter = 2;
      String ext = name.substring(name.lastIndexOf('.') + 1);
      name = name.substring(0, name.length() - ext.length() - 1);
      while (duplicate) {
        String newName = name + "_" + counter + "." + ext;
        duplicate = isBundleNameAdded(infos, newName);
        if (!duplicate) {
          name = newName;
          break;
        }
        counter++;
      }
    }
    return name;
  }

  public boolean isBundleNameAdded(Vector infos, String name) {
    for (int j = 0; j < infos.size() - 1; j++) {
      if (name.equals(((BundleInfo) infos.elementAt(j)).getName())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Opens the custom dialog to puts the values to this cell editor. This
   * method is called only when the type of this cell editor is
   * <code>DIALOG_TYPE</code>
   */
  protected void dialogAction() {
    Point displayLoc = editorPanel.getParent().toDisplay(editorPanel.getLocation());
    CustomHeadersDialog dialog = new CustomHeadersDialog(DPPErrorHandler.getShell(), displayLoc, editorPanel.getSize());
    Object data = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
    if (data instanceof ResourceInfo) {
      dialog.setBundleDialog(false);
      ResourceInfo info = null;
      if (data instanceof ResourceInfo) {
        info = (ResourceInfo) data;
        dialog.setHeaders(info.getOtherHeaders());
      }

      if (dialog.open() == Window.OK) {
        if (info != null) {
          Vector headers = dialog.getHeaders();

          for (int i = headers.size() - 1; i >= 0; i--) {
            String lastHeader = headers.elementAt(i).toString();
            if (lastHeader.equals("")) {
              headers.removeElementAt(i);
            }
          }
          customText.setText(DPPUtilities.convertToString(headers));
          setInfoValue();
          table.update();
          viewer.refresh();
        }
      }
    } else if (data instanceof BundleInfo) {
      dialog.setBundleDialog(true);
      BundleInfo info = null;
      if (data instanceof BundleInfo) {
        info = (BundleInfo) data;
        dialog.setHeaders(info.getOtherHeaders());
      }

      if (dialog.open() == Window.OK) {
        if (info != null) {
          Vector headers = dialog.getHeaders();

          for (int i = headers.size() - 1; i >= 0; i--) {
            String lastHeader = headers.elementAt(i).toString();
            if (lastHeader.equals("")) {
              headers.removeElementAt(i);
            }
          }
          customText.setText(DPPUtilities.convertToString(headers));
          setInfoValue();
          table.update();
          viewer.refresh();
        }
      }
    }
    customText.setFocus();
  }

  /**
   * This method is called when the custom button is pushed and to opens the
   * corresponding with the type of the cell editor dialogs to fills the
   * values.
   */
  public void actionPerformed() {
    if (type == DIALOG_TYPE) {
      dialogAction();
    } else {
      browseAction();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * 
   * 
   * 
   * @seeorg.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events.
   * FocusEvent)
   */
  public void focusLost(FocusEvent e) {
    externalStop();
    if (isActivated()) {
      markDirty();
      if (type == DIALOG_TYPE || type == TEXT_BUTTON_TYPE) {
        String newValue = customText.getText();
        TableItem[] items = viewer.getTable().getSelection();
        TableItem modifyItem = null;
        if (items != null && items.length == 1) {
          modifyItem = items[0];
        }
        if (DPPFormSection.itemExists(viewer, modifyItem, newValue) == -1) {
          fireApplyEditorValue();
        }
      } else {
        fireApplyEditorValue();
      }
      deactivate();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.swt.events.FocusListener#focusGained(org.eclipse.swt.events
   * .FocusEvent)
   */
  public void focusGained(FocusEvent e) {
    if (e.getSource() == customText) {
      if (customText != null && !customText.isDisposed() && (customText.getParent() == editorPanel)) {
        customText.forceFocus();
      }
      try {
        String txt = customText.getText();
        if (!txt.equals(customText.getText())) {
          customText.setText(getStringInfoValue());
        }
      } catch (Throwable t) {
        customText.setText(getStringInfoValue());
      }
      return;
    }
    if (e.getSource() == checkBoxButton) {
      checkBoxButton.setSelection(getInfoSelection());
      return;
    }
    if (e.getSource() == editorPanel) {
      if (customText.isDisposed()) {
        customButton.setFocus();
      }
      return;
    }
  }

  /**
   * Sets the values of the data depending on the selection in the table
   * viewer
   * 
   * @param source
   */
  public void itemStateChanged(Object source) {
    setInfoValue();
  }

  boolean ignoreStop = false;

  /**
   * destroys this object
   */
  public void destroy() {
    if (ignoreStop) {
      return;
    }
    try {
      if (checkBoxButton != null) {
        checkBoxButton.dispose();
        checkBoxButton = null;
      }
    } catch (Throwable t) {
      DPPErrorHandler.processError(t);
    }
    try {
      if (customButton != null) {
        customButton.dispose();
        customButton = null;
      }
    } catch (Throwable t) {
      DPPErrorHandler.processError(t);
    }
    try {
      if (customText != null) {
        customText.dispose();
        customText = null;
      }
    } catch (Throwable t) {
      DPPErrorHandler.processError(t);
    }
    try {
      if (editorPanel != null) {
        editorPanel.dispose();
        editorPanel = null;
      }
    } catch (Throwable t) {
      DPPErrorHandler.processError(t);
    }
  }

  /*
   * @see SelectionListener#widgetDefaultSelected(SelectionEvent)
   */
  public void widgetDefaultSelected(SelectionEvent e) {
  }

  /*
   * @see SelectionListener#widgetSelected(SelectionEvent)
   */
  public void widgetSelected(SelectionEvent e) {
    if (e.getSource() == table) {
      i = table.getSelectionIndex();
    }
  }

  /**
   * Creates the editor control with the given composite parent.
   * 
   * @param parent
   *            a composite control which will be the parent of the new cell
   *            editor
   * @return created composite control
   */
  private Control createEditorPanel(Composite parent) {
    if (editorPanel == null) {
      editorPanel = new Composite(parent, 0);
      GridLayout lt = new GridLayout();
      lt.numColumns = 2;
      lt.marginWidth = 0;
      lt.marginHeight = 0;
      lt.marginLeft = 0;
      lt.marginRight = 0;
      lt.marginTop = 0;
      lt.marginBottom = 0;
      editorPanel.setLayout(lt);
      editorPanel.addFocusListener(this);
      GridData dt = new GridData(GridData.FILL_BOTH);
      if (type == CHECK_BOX_TYPE) {
        checkBoxButton = FormWidgetFactory.createButton(editorPanel, "", SWT.CHECK);
        checkBoxButton.addSelectionListener(this);
        checkBoxButton.addFocusListener(this);
        supports = supports | SUPPROTS_TAGS;
      } else {
        customText = new Text(editorPanel, 0);
        customText.addKeyListener(this);
        customText.addFocusListener(this);
        customText.addTraverseListener(this);
        customText.addModifyListener(getModifyListener());
        customText.setLayoutData(dt);
        customText.selectAll();
        if (type == DIALOG_TYPE) {
          customText.setEditable(false);
        }
        customText.forceFocus();

        GridData cgr = new GridData();
        if (table != null) {
          cgr.heightHint = Math.max(editorPanel.getChildren()[0].computeSize(SWT.DEFAULT, SWT.DEFAULT, false).y, table.getItemHeight() + 2);
        }
        if (parent instanceof Table) {
          cgr.heightHint = Math.max(editorPanel.getChildren()[0].computeSize(SWT.DEFAULT, SWT.DEFAULT, false).y, ((Table) parent).getItemHeight() + 2);
        }
        cgr.widthHint = cgr.heightHint;
        customButton = new Button(editorPanel, 0);
        customButton.setText(BROWSE_BUTTON);
        customButton.addMouseListener(this);
        customButton.setLayoutData(cgr);
        supports = supports | SUPPORTS_CUSTOM_EDITOR;
      }
    }
    return editorPanel;
  }

  /**
   * Creates the control for this cell editor under the given parent control.
   * 
   * @param parent
   *            the parent control
   * @return the new control, or <code>null</code> if this cell editor has no
   *         control
   * 
   * @see org.eclipse.jface.viewers.CellEditor#createControl(org.eclipse.swt.widgets.Composite)
   */
  protected Control createControl(Composite parent) {
    createEditorPanel(parent);
    return editorPanel;
  }

  /**
   * Returns this cell editor's value.
   * 
   * @return the value of this cell editor
   * @see org.eclipse.jface.viewers.CellEditor#doGetValue()
   */
  protected Object doGetValue() {
    Object obj = null;
    if (type == TEXT_BUTTON_TYPE || type == DIALOG_TYPE) {
      if (!customText.isDisposed()) {
        obj = customText.getText();
      }
    } else if (type == CHECK_BOX_TYPE) {
      if (checkBoxButton == null) {
        obj = new Boolean(false);
      } else {
        obj = new Boolean(checkBoxButton.getSelection());
      }
    }
    table.update();
    viewer.refresh();
    return obj;
  }

  /**
   * Sets the focus to the cell editor's control.
   * 
   * @see org.eclipse.jface.viewers.CellEditor#doSetFocus()
   */
  protected void doSetFocus() {
    if (type == CHECK_BOX_TYPE) {
      if (checkBoxButton != null) {
        checkBoxButton.forceFocus();
      }
    } else {
      customText.forceFocus();
    }
  }

  /**
   * Sets this cell editor's value.
   * 
   * @param value
   *            the value of this cell editor
   * @see org.eclipse.jface.viewers.CellEditor#doSetValue(java.lang.Object)
   */
  protected void doSetValue(Object value) {
    if (value instanceof String) {
      customText.removeModifyListener(getModifyListener());
      if (column == BUNDLE_PATH || column == RESOURCE_PATH) {
        Object data = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
        if (data != null) {
          if (data instanceof BundleInfo) {
            BundleInfo info = (BundleInfo) data;
            if (column == BUNDLE_PATH) {
              customPath = info.getBundlePath();
              if (customPath == null || customPath.equals("")) {
                String infoName = info.getName();
                if (infoName != null && !infoName.equals("")) {
                  customPath = "bundles" + File.separator + infoName;
                }
              }
            }
          } else if (data instanceof ResourceInfo) {
            ResourceInfo info = (ResourceInfo) data;
            if (column == RESOURCE_PATH) {
              customPath = info.getResourcePath();
              if (customPath == null || customPath.equals("")) {
                String infoName = info.getName();
                if (infoName != null && !infoName.equals("")) {
                  customPath = "resources" + File.separator + infoName;
                }
              }
            }
          }
        }
      }
      customText.setText((String) value);
      customText.addModifyListener(getModifyListener());
    } else if (value instanceof Boolean) {
      Boolean bool = (Boolean) value;
      checkBoxButton.setSelection(bool.booleanValue());
    }
  }

  /**
   * This method is called from the modify listener when the edit event was
   * occurred.
   * 
   * @param e
   *            an event containing information about the modify
   */
  protected void editOccured(ModifyEvent e) {
    String value = customText.getText();
    if (value == null) {
      value = "";//$NON-NLS-1$
    }
    Object typedValue = value;
    boolean oldValidState = isValueValid();
    boolean newValidState = isCorrect(typedValue);
    if (!newValidState) {
      // try to insert the current value into the error message.
      setErrorMessage(MessageFormat.format(getErrorMessage(), new Object[] { value }));
    }
    valueChanged(oldValidState, newValidState);
  }

  /**
   * Returns the modify listener associated with this cell editor. If there
   * are no the modify listener this method will be creates.
   * 
   * @return the modify listener associated with the cell editor
   */
  private ModifyListener getModifyListener() {
    if (modifyListener == null) {
      modifyListener = new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          editOccured(e);
        }
      };
    }
    return modifyListener;
  }

  /**
   * Sent when a traverse event occurs in a control.
   * 
   * @param e
   *            an event containing information about the traverse
   * @see org.eclipse.swt.events.TraverseListener#keyTraversed(org.eclipse.swt.events.TraverseEvent)
   */
  public void keyTraversed(TraverseEvent e) {
    if (e.detail == SWT.TRAVERSE_ESCAPE || e.detail == SWT.TRAVERSE_RETURN) {
      e.doit = false;
    }
  }

  /**
   * Returns a layout data object for this cell editor.
   * 
   * @return the layout data object
   * @see org.eclipse.jface.viewers.CellEditor#getLayoutData()
   */
  public LayoutData getLayoutData() {
    LayoutData layoutData = super.getLayoutData();
    if ((customText == null) || customText.isDisposed()) {
      layoutData.minimumWidth = 60;
    } else {
      // make the customText 10 characters wide
      GC gc = new GC(customText);
      layoutData.minimumWidth = (gc.getFontMetrics().getAverageCharWidth() * 10) + 10;
      gc.dispose();
    }
    return layoutData;
  }

  public void mouseDoubleClick(MouseEvent arg0) {
  }

  public void mouseDown(MouseEvent arg0) {
    actionPerformed();
  }

  public void mouseUp(MouseEvent arg0) {
  }

}
