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

import java.io.File;
import java.util.Hashtable;
import java.util.Vector;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.dpeditor.IHelpContextIds;
import org.tigris.mtoolkit.dpeditor.editor.event.EventConstants;
import org.tigris.mtoolkit.dpeditor.editor.event.TableControlListener;
import org.tigris.mtoolkit.dpeditor.editor.forms.FormWidgetFactory;
import org.tigris.mtoolkit.dpeditor.editor.model.IModelChangedEvent;
import org.tigris.mtoolkit.dpeditor.util.DPPErrorHandler;
import org.tigris.mtoolkit.dpeditor.util.ResourceManager;
import org.tigris.mtoolkit.util.BundleInfo;
import org.tigris.mtoolkit.util.DPPFile;
import org.tigris.mtoolkit.util.DPPUtilities;

public class ChangeBundleJarNameDialog extends Dialog implements
		SelectionListener, ISelectionChangedListener, ICheckStateListener {

	/** Holds the label of the select all button */
	public static final String SELECT_ALL_BUTTON = "DPPEditor.SelectAll_Button";
	/** Holds the label of the deselect all button */
	public static final String DESELECT_ALL_BUTTON = "DPPEditor.DeselectAll_Button";
	/** Holds the label of the skip update button */
	public static final String SKIP_UPDATE_BUTTON = "DPPEditor.SkipUpdate_Button";

	/** The title of the dialog */
	public static String TITLE = "DPPEditor.ChangeBundleJarNameDialog.Title"; //$NON-NLS-1$
	/** The message of equals keys of the ManifestHeader */
	public static final String EQUAL_VALUES_MSG1 = "DPPEditor.ChangeBundleJarNameDialog.EqualValuesMsg1";
	/** The message to continue */
	public static final String EQUAL_VALUES_MSG2 = "DPPEditor.ChangeBundleJarNameDialog.EqualValuesMsg2";
	/** The error message that headers cannot contains spaces */
	public static final String ERROR_SPACE_VALUE = "DPPEditor.ChangeBundleJarNameDialog.ErrorSpace";

	/** The width of the dialog */
	public static int SHELL_WIDTH = 450;
	/** The height of the dialog */
	public static int SHELL_HEIGHT = 300;

	/** The shell in which this dialog will be open. */
	private Shell shell;
	/** The composite of this dialog */
	private Composite container;
	/** The old location of this dialog */
	private Point location;
	/** The first location of the dialog */
	private Point displayLoc;
	/** The size of the dialog's area */
	private Point areaSize;

	/** Shows is this is the first appearance of the dialog */
	private boolean isNewDialog;

	/** The instance of this dialog */
	private static ChangeBundleJarNameDialog dialog;

	/** The Viewer of the jar table in dialog */
	private CheckboxTableViewer jarsTable;
	/** Button, which removes the selected ManifestHeader from the table */
	private Button removeButton;
	/** Button, which select all jars in the table */
	private Button selectAllButton;
	/** Button, which deselect all jars in the table */
	private Button deselectAllButton;

	/**
	 * <code>Vector</code>, in which is the all ManifestHeaders, which will be
	 * added in bundle
	 */
	private Vector jarsVector = new Vector();

	/** The <code>DPPFile</code>, which changed bundles jar names will be shown */
	private DPPFile dppFile = null;

	public int openResult;

	/**
	 * Creates the instance of the ChangeBundleJarNameDialog in the given parent
	 * shell, display position and a size of the dialog.
	 * 
	 * @param parent
	 *            a shell which will be the parent of the new instance (cannot
	 *            be null)
	 * @param displayLoc
	 *            a display location of this dialog
	 * @param size
	 *            a size of the new instance
	 */
	public ChangeBundleJarNameDialog(Shell parent, Point displayLoc, Point size) {
		super(parent);
		this.setShellStyle(SWT.RESIZE | SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL);
		isNewDialog = true;
		this.displayLoc = displayLoc;
		areaSize = size;
		jarsVector = new Vector();
	}

	/**
	 * Checks if there are a instance of this dialog and return it or creates a
	 * new instance of this dialog by given its parent, display location and
	 * size of the dialog and return this new created dialog.
	 * 
	 * @param parent
	 *            a shell which will be the parent of the new instance (cannot
	 *            be null)
	 * @param displayLoc
	 *            a display location of dialog
	 * @param size
	 *            a size of the new instance
	 * @return an old instance of this dialog or created a new one if there are
	 *         no instance of dialog
	 */
	public static ChangeBundleJarNameDialog getInstance(Shell parent, Point displayLoc, Point size) {
		if (dialog == null) {
			SHELL_WIDTH = 400;
			SHELL_HEIGHT = 450;
			dialog = new ChangeBundleJarNameDialog(parent, displayLoc, size);
		}
		return dialog;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.
	 * Shell)
	 */
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setSize(SHELL_WIDTH, SHELL_HEIGHT);
		if (isNewDialog) {
			shell.setLocation(new Point(displayLoc.x + (areaSize.x / 2 - SHELL_WIDTH / 2), displayLoc.y + (areaSize.y / 2 - SHELL_HEIGHT / 2)));
			isNewDialog = false;
		} else {
			shell.setLocation(location);
		}
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		shell.setLayout(layout);
		this.shell = shell;
	}

	/**
	 * Opens this dialog, creating it first if it has not yet been created.
	 */
	public void showDialog() {
		super.open();
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
			if (source == selectAllButton) {
				handleSelectAll();
			} else if (source == deselectAllButton) {
				handleDeselectAll();
			}
		}
	}

	/**
	 * Select all of the changed bundles jar name in the table.
	 */
	private void handleSelectAll() {
		handleCheckedAll(true);
	}

	/**
	 * Select none of the changed bundles jar name in the table.
	 */
	private void handleDeselectAll() {
		handleCheckedAll(false);
	}

	/**
	 * Select or deselect all of the changed bundles jar name in the table
	 * depends on the given <code>boolean</code> flag.
	 * 
	 * @param selectAll
	 *            if the flag is <code>true</code> select all jars in the table,
	 *            otherwise deselect all jars in the table.
	 */
	private void handleCheckedAll(boolean selectAll) {
		TableItem[] items = jarsTable.getTable().getItems();
		if (items != null) {
			for (int i = 0; i < items.length; i++) {
				TableItem item = items[i];
				jarsTable.setChecked(item, selectAll);
				Object data = item.getData();
				if (data instanceof CheckedJarHolder) {
					CheckedJarHolder jarHolder = (CheckedJarHolder) data;
					int indexOf = jarsVector.indexOf(jarHolder);
					jarHolder.setChecked(selectAll);
					CheckedJarHolder elementAt = (CheckedJarHolder) jarsVector.elementAt(indexOf);
					elementAt.setChecked(selectAll);
				}
			}
			CheckedJarHolder[] elements = new CheckedJarHolder[jarsVector.size()];
			jarsVector.copyInto(elements);
			for (int i = 0; i < elements.length; i++) {
				CheckedJarHolder jarHolder = elements[i];
				jarsTable.setChecked(jarHolder, selectAll);
			}
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
	}

	/**
	 * Sets the receiver's text, which is the string that the window manager
	 * will typically display as the receiver's <em>title</em>, to the dialog.
	 * 
	 * @param title
	 *            the new title of the dialog
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
	public boolean close() {
		Point size = shell.getSize();
		SHELL_WIDTH = size.x;
		SHELL_HEIGHT = size.y;
		location = shell.getLocation();
		return super.close();
	}

	protected Control createContents(Composite parent) {
    PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.CHANGE_BUNDLE_DIALOG);
		// create the top level composite for the dialog
		Composite composite = new Composite(parent, 0);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		applyDialogFont(composite);
		// initialize the dialog units
		initializeDialogUnits(composite);
		// create the dialog area and button bar
		dialogArea = createDialogArea(composite);
		buttonBar = createButtonBar(composite);

		return composite;
	}

	/**
	 * Creates and returns the contents of the upper part of this dialog (above
	 * the button bar).
	 * 
	 * @param parent
	 *            the parent composite to contain the dialog area
	 * @return the dialog area control
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		// create a composite with standard margins and spacing
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		applyDialogFont(composite);
		Label label = new Label(composite, SWT.NONE);
		label.setText(ResourceManager.getString("DPPEditor.ChangeBundleJarNameDialog.Label"));
		container = createClient(composite);
		setTitle(ResourceManager.getString(TITLE, "")); //$NON-NLS-1$
		return composite;
	}

	/**
	 * Disposes the instance of this dialog.
	 */
	public static void dispose() {
		dialog = null;
	}

	/**
	 * Checks if the given value is exists as a value of the
	 * <code>TableItem</code>s of the given <code>TableViewer</code> and if
	 * exists, checks is the <code>TableItem</code> which value is the given
	 * value is the same <code>TableItem</code> as a given. In this case returns
	 * the index of the <code>TableItem</code>, otherwise returns -1.
	 * 
	 * @param table
	 *            the TableViewer, which TableItems will be checked
	 * @param item
	 *            the TableItem, which will be compare with the viewer's
	 *            TableItems
	 * @param newValue
	 *            the new value of the given TableItem
	 * @param column
	 *            the column index, which text will be compare with the new
	 *            value
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
			return false;
		}

		public void modify(Object object, String property, Object value) {
			TableItem item = (TableItem) object;
			CheckedJarHolder jarHolder = (CheckedJarHolder) item.getData();
			String newValue = value.toString();

			if (property.equals("old")) {
				if (newValue.equals(jarHolder.getOldJar()) && (!newValue.equals(""))) {
					return;
				}
				newValue = newValue.trim();
				if (newValue.indexOf(' ') != -1) {
					DPPErrorHandler.showErrorTableDialog(ResourceManager.getString(ERROR_SPACE_VALUE));
					return;
				}
				if ((!newValue.equals("")) && (itemExists(jarsTable, item, newValue, 0) != -1)) {
					DPPErrorHandler.showErrorTableDialog(ResourceManager.getString(EQUAL_VALUES_MSG1) + "\n" + ResourceManager.getString(EQUAL_VALUES_MSG2));
					jarsVector.remove(newValue);
					return;
				}
				jarHolder.setOldJar(newValue);
				removeButton.setEnabled(true);
			} else if (property.equals("new")) {
				if (newValue.equals(jarHolder.getNewJar())) {
					return;
				}
				jarHolder.setNewJar(newValue);
			}
			jarsTable.update(jarHolder, null);
		}

		public Object getValue(Object object, String property) {
			CheckedJarHolder jarHolder = (CheckedJarHolder) object;
			if (property.equals("old")) {
				return new File(jarHolder.getOldJar()).getName();
			} else if (property.equals("new")) {
				return new File(jarHolder.getNewJar()).getName();
			}
			return "";
		}
	}

	/**
	 * Content provider which mediates between the viewer's model and the viewer
	 * itself.
	 */
	class TableContentProvider extends DefaultContentProvider implements
			IStructuredContentProvider {
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
	 * A label provider which sets the CheckedJarHolder value in corresponding
	 * columns in the TableViewer.
	 */
	class TableLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (obj instanceof CheckedJarHolder) {
				CheckedJarHolder jarHolder = (CheckedJarHolder) obj;
				if (index == 0) {
					return new File(jarHolder.getOldJar()).getName();
				}
				if (index == 1) {
					return new File(jarHolder.getNewJar()).getName();
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
	 *            a composite control which will be the parent of the dialog's
	 *            contents
	 * @return the created composite
	 */
	public Composite createClient(Composite parent) {
		Composite tableContainer = FormWidgetFactory.createComposite(parent);
		tableContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		tableContainer.setLayout(layout);
		createTable(tableContainer);
		tableContainer.pack();
		return tableContainer;
	}

	/**
	 * Creates the table in this dialog
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the table's
	 *            composite
	 */
	private void createTable(Composite parent) {
		Composite container = FormWidgetFactory.createComposite(parent);
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		Table table = FormWidgetFactory.createTable(container, SWT.SINGLE | SWT.FULL_SELECTION | SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL);
		table.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent ev) {
				if (ev.keyCode == 27) {
					if (ev.getSource() instanceof Table) {
						Table table = (Table) ev.getSource();
						if (table.getSelectionIndex() < 0)
							return;
						TableItem item = table.getItem(table.getSelectionIndex());
						final CheckedJarHolder jarHolder = (CheckedJarHolder) item.getData();
						if (jarHolder.getOldJar().equals("")) {
							jarsVector.removeElement(jarHolder);
						}
					}
				}
			}
		});
		table.setLayout(new GridLayout());
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		String[] columnTitles = { "Old Jar", "New Jar" };
		for (int i = 0; i < columnTitles.length; i++) {
			TableColumn tableColumn = new TableColumn(table, SWT.NULL);
			tableColumn.setText(columnTitles[i]);
		}

		TableControlListener controlListener = new TableControlListener(table);
		controlListener.setResizeMode(EventConstants.HEADERS_RESIZE_MODE);
		container.addControlListener(controlListener);

		jarsTable = new CheckboxTableViewer(table);
		jarsTable.setContentProvider(new TableContentProvider());
		jarsTable.setLabelProvider(new TableLabelProvider());
		jarsTable.addCheckStateListener(this);

		String[] properties = { "old", "new" };
		jarsTable.setCellModifier(new KeyModifier());
		jarsTable.setColumnProperties(properties);
		if (jarsTable != null) {
			jarsTable.setInput(jarsVector);
			CheckedJarHolder[] elements = new CheckedJarHolder[jarsVector.size()];
			jarsVector.copyInto(elements);
			jarsTable.setCheckedElements(elements);
		}
		FormWidgetFactory.paintBordersFor(container);
	}

	/**
	 * Creates and returns the contents of this dialog's button bar.
	 * 
	 * @param parent
	 *            the parent composite to contain the button bar
	 * @return the button bar control
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createButtonBar(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		// create a layout with spacing and margins appropriate
		// for the font size.
		GridLayout layout = new GridLayout();
		layout.numColumns = 1; // this is incremented by createButton
		layout.makeColumnsEqualWidth = true;
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		composite.setLayout(layout);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_CENTER);
		composite.setLayoutData(data);
		composite.setFont(parent.getFont());

		createButton(composite, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(composite, IDialogConstants.CANCEL_ID, ResourceManager.getString(SKIP_UPDATE_BUTTON), false);
		selectAllButton = createButton(composite, IDialogConstants.SELECT_ALL_ID, ResourceManager.getString(SELECT_ALL_BUTTON, ""), false);
		selectAllButton.addSelectionListener(this);
		deselectAllButton = createButton(composite, IDialogConstants.DESELECT_ALL_ID, ResourceManager.getString(DESELECT_ALL_BUTTON, ""), false);
		deselectAllButton.addSelectionListener(this);
		updateEnabledButtons();
		return composite;
	}

	/**
	 * Enables or disables the select all and deselect all button, depending on
	 * the contained tables items.
	 */
	private void updateEnabledButtons() {
		Table table = jarsTable.getTable();
		boolean hasItems = table.getItemCount() > 0;
		selectAllButton.setEnabled(hasItems);
		deselectAllButton.setEnabled(hasItems);
	}

	/**
	 * Refreshes the viewer with information freshly obtained from the viewer's
	 * model.
	 */
	public void jarsChanged() {
		if (jarsTable != null) {
			jarsTable.refresh();
		}
	}

	// IModelChangedListener implementation
	/**
	 * Adds, edits and removes the ManifestHeader from the TableViewer or
	 * refreshes the TableViewer.
	 * 
	 * @param event
	 *            the event that will be processed
	 */
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			jarsTable.refresh();
			return;
		}
		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof CheckedJarHolder) {
			CheckedJarHolder jarHolder = (CheckedJarHolder) changeObject;
			if (event.getChangeType() == IModelChangedEvent.ADD) {
				jarsTable.add(jarHolder);
			}
			if (event.getChangeType() == IModelChangedEvent.INSERT) {
				jarsTable.add(jarHolder);
				jarsTable.editElement(jarHolder, 0);
			} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
				jarsTable.remove(jarHolder);
			}
		} else {
			jarsTable.refresh();
		}
	}

	/**
	 * Returns a <code>Hashtable</code> filled with all checked jar holders,
	 * which are added in the dialog.
	 * 
	 * @return a <code>Hashtable</code> with all checked jar holdes
	 */
	public Hashtable getSelectedJars() {
		Hashtable result = new Hashtable();
		for (int i = 0; i < jarsVector.size(); i++) {
			CheckedJarHolder jarHolder = (CheckedJarHolder) jarsVector.elementAt(i);
			if (jarHolder.isChecked()) {
				result.put(jarHolder.getOldJar(), jarHolder.getNewJar());
			}
		}
		return result;
	}

	public void setDPPFile(DPPFile file) {
		this.dppFile = file;
	}

	public void setDPPValues() {
		if (dppFile != null) {
			Vector infos = dppFile.getBundleInfos();
			if (infos != null) {
				for (int i = 0; i < infos.size(); i++) {
					BundleInfo info = (BundleInfo) infos.elementAt(i);
					String bundlePath = info.getBundlePath();
					File bundlePathFile = new File(bundlePath);
					File findLastJar = DPPUtilities.findLastJar(bundlePathFile);
					if (!bundlePath.equals(findLastJar.toString())) {
						CheckedJarHolder jarHolder = new CheckedJarHolder(bundlePathFile.toString(), findLastJar.toString());
						jarHolder.setChecked(true);
						jarsVector.addElement(jarHolder);
					}
				}
			}
		}
	}

	/**
	 * Opens this window, creating it first if it has not yet been created.
	 * Opens the window only if there are some jars that was changed.
	 * 
	 * @return the standard return codes: <code>OK</code> or <code>CANCEL</code>
	 * 
	 * @see org.eclipse.jface.window.Window#open()
	 */
	public int open() {
		setDPPValues();
		if (!hasChangedJars()) {
			openResult = Window.CANCEL;
		} else {
			openResult = super.open();
		}
		return openResult;
	}

	/**
	 * Returns whether there are the jar files, which not exists and there are
	 * new jar version of this jar.
	 * 
	 * @return <code>true</code> if there are changed jar filed, otherwise
	 *         returns <code>false</code>
	 */
	public boolean hasChangedJars() {
		return (jarsVector != null && jarsVector.size() != 0);
	}

	/**
	 * Notifies of a change to the checked state of an element.
	 * 
	 * @param event
	 *            event object describing the change
	 */
	public void checkStateChanged(CheckStateChangedEvent event) {
		boolean isChecked = event.getChecked();
		CheckedJarHolder jarHolder = (CheckedJarHolder) event.getElement();
		int indexOf = jarsVector.indexOf(jarHolder);
		jarHolder.setChecked(isChecked);
		CheckedJarHolder elementAt = (CheckedJarHolder) jarsVector.elementAt(indexOf);
		elementAt.setChecked(isChecked);

	}
}
