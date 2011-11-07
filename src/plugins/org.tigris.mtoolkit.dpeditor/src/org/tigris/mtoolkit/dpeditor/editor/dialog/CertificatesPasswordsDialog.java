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
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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
import org.tigris.mtoolkit.dpeditor.util.ResourceManager;
import org.tigris.mtoolkit.util.CertificateInfo;
import org.tigris.mtoolkit.util.DPPFile;
import org.tigris.mtoolkit.util.DPPUtilities;

public class CertificatesPasswordsDialog extends Dialog implements
		SelectionListener, ISelectionChangedListener {

	/** The title of the dialog */
	public static String TITLE = "DPPEditor.CertificatesPasswordsDialog.Title"; //$NON-NLS-1$

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
	private static CertificatesPasswordsDialog dialog;

	/** The Viewer of the jar table in dialog */
	private TableViewer certsTable;

	/** <code>Vector</code>, holds all Certificates and new entered passwords */
	private Vector certsVector = new Vector();

	/** The <code>DPPFile</code>, which certificates needed to be sets passwords */
	private DPPFile dppFile = null;

	/** Holds the project location */
	private String prjLocation = "";

	public int openResult;

	private CLabel errorLabel;

	/**
	 * Creates the instance of the CertificatesPasswordsDialog in the given
	 * parent shell, display position and a size of the dialog.
	 * 
	 * @param parent
	 *            a shell which will be the parent of the new instance (cannot
	 *            be null)
	 * @param displayLoc
	 *            a display location of this dialog
	 * @param size
	 *            a size of the new instance
	 */
	public CertificatesPasswordsDialog(Shell parent, Point displayLoc,
			Point size) {
		super(parent);
		this.setShellStyle(SWT.RESIZE | SWT.CLOSE | SWT.TITLE
				| SWT.APPLICATION_MODAL);
		isNewDialog = true;
		this.displayLoc = displayLoc;
		areaSize = size;
		certsVector = new Vector();
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
	public static CertificatesPasswordsDialog getInstance(Shell parent,
			Point displayLoc, Point size) {
		if (dialog == null) {
			SHELL_WIDTH = 400;
			SHELL_HEIGHT = 450;
			dialog = new CertificatesPasswordsDialog(parent, displayLoc, size);
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
			shell.setLocation(new Point(displayLoc.x
					+ (areaSize.x / 2 - SHELL_WIDTH / 2), displayLoc.y
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.Dialog#createContents(org.eclipse.swt.widgets
	 * .Composite)
	 */
	protected Control createContents(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem()
				.setHelp(parent, IHelpContextIds.CERT_PASS_DIALOG);
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

	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				ResourceManager
						.getString("DPPEditor.CertificatesSection.SkipButton"),
				false);
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
		label.setText(ResourceManager
				.getString("DPPEditor.CertificatesPasswordsDialog.Label"));
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
	public static int itemExists(TableViewer table, TableItem item,
			String newValue, int column) {
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
			if (property.equals("storepass") || property.equals("keypass")) {
				return true;
			}
			return false;
		}

		public void modify(Object object, String property, Object value) {
			TableItem item = (TableItem) object;
			CertificateInfo cert = (CertificateInfo) item.getData();
			String newValue = value.toString();

			if (property.equals("alias")) {
				return;
			} else if (property.equals("keystore")) {
				return;
			} else if (property.equals("storepass")) {
				if (newValue.equals(cert.getStorepass())
						&& (!newValue.equals(""))) {
					return;
				}
				int index = certsVector.indexOf(cert);
				cert.setStorepass(newValue);
				CertificateInfo certInfo = (CertificateInfo) certsVector
						.elementAt(index);
				certInfo.setStorepass(newValue);
			} else if (property.equals("keypass")) {
				if (newValue.equals(cert.getKeypass())
						&& (!newValue.equals(""))) {
					return;
				}
				int index = certsVector.indexOf(cert);
				cert.setKeypass(newValue);
				CertificateInfo certInfo = (CertificateInfo) certsVector
						.elementAt(index);
				certInfo.setKeypass(newValue);
			}

			certsTable.update(cert, null);

			if (!newValue.equals("") && errorLabel.isVisible()) {
				errorLabel.setVisible(false);
			}
		}

		public Object getValue(Object object, String property) {
			CertificateInfo certificate = (CertificateInfo) object;
			if (property.equals("alias")) {
				return DPPUtilities.getStringValue(certificate.getAlias());
			} else if (property.equals("keystore")) {
				String keyStore = DPPUtilities.getStringValue(certificate
						.getKeystore());
				return getRelativePath(keyStore);
			} else if (property.equals("storepass")) {
				return DPPUtilities.getStringValue(certificate.getStorepass());
			} else if (property.equals("keypass")) {
				return DPPUtilities.getStringValue(certificate.getKeypass());
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
			if (parent instanceof DPPFile) {
				Vector infos = ((DPPFile) parent).getCertificateInfos();
				if (infos == null) {
					return null;
				}
				certsVector.removeAllElements();
				for (int i = 0; i < infos.size(); i++) {
					CertificateInfo info = (CertificateInfo) infos.elementAt(i);
					String keyPass = info.getKeypass();
					String storePass = info.getStorepass();
					if ((keyPass == null || keyPass.equals(""))
							|| (storePass == null || storePass.equals(""))) {
						certsVector.addElement(info);
					}
				}
				CertificateInfo[] result = new CertificateInfo[certsVector
						.size()];
				certsVector.copyInto(result);
				return result;
			} else if (parent instanceof Vector) {
				Vector tmp = (Vector) parent;
				CertificateInfo[] result = new CertificateInfo[tmp.size()];
				tmp.copyInto(result);
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
			if (obj instanceof CertificateInfo) {
				CertificateInfo certificate = (CertificateInfo) obj;
				if (index == 0) {
					return certificate.getAlias();
				} else if (index == 1) {
					String keyStore = DPPUtilities.getStringValue(certificate
							.getKeystore());
					return getRelativePath(keyStore);
				} else if (index == 2) {
					String storePass = certificate.getStorepass();
					String returnPass = "";
					for (int i = 0; i < storePass.length(); i++) {
						returnPass += "*";
					}
					return returnPass;
				} else if (index == 3) {
					String keyPass = certificate.getKeypass();
					String returnPass = "";
					for (int i = 0; i < keyPass.length(); i++) {
						returnPass += "*";
					}
					return returnPass;
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

		errorLabel = new CLabel(tableContainer, SWT.WRAP | SWT.LEFT);
		errorLabel.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR)
				.getImage());
		errorLabel.setVisible(false);
		errorLabel.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
		errorLabel.setForeground(getShell().getDisplay().getSystemColor(
				SWT.COLOR_RED));
		errorLabel.setText(ResourceManager
				.getString("DPPEditor.CertificatesPasswordsDialog.ErrorLabel"));

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

		Table table = FormWidgetFactory.createTable(container, SWT.SINGLE
				| SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
		table.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent ev) {
				if (ev.keyCode == 27) {
					if (ev.getSource() instanceof Table) {
						Table table = (Table) ev.getSource();
						if (table.getSelectionIndex() < 0)
							return;
						/*TableItem item =*/ table.getItem(table
								.getSelectionIndex());
					}
				}
			}
		});
		table.setLayout(new GridLayout());
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		String[] columnTitles = {
				ResourceManager
						.getString("DPPEditor.CertificatesSection.ColAlias"),
				ResourceManager
						.getString("DPPEditor.CertificatesSection.ColKeystore"),
				ResourceManager
						.getString("DPPEditor.CertificatesSection.ColStorePassword"),
				ResourceManager
						.getString("DPPEditor.CertificatesSection.ColKeyPassword") };

		for (int i = 0; i < columnTitles.length; i++) {
			TableColumn tableColumn = new TableColumn(table, SWT.NULL);
			tableColumn.setText(columnTitles[i]);
		}

		TableControlListener controlListener = new TableControlListener(table);
		controlListener
				.setResizeMode(EventConstants.CERTIFICATES_SECOND_RESIZE_MODE);
		container.addControlListener(controlListener);

		certsTable = new TableViewer(table);
		certsTable.setContentProvider(new TableContentProvider());
		certsTable.setLabelProvider(new TableLabelProvider());
		certsTable.addSelectionChangedListener(this);

		CellEditor[] editors = new CellEditor[] { new TextCellEditor(table),
				new TextCellEditor(table),
				new TextCellEditor(table, SWT.PASSWORD),
				new TextCellEditor(table, SWT.PASSWORD) };
		String[] properties = { "alias", "keystore", "storepass", "keypass" };
		certsTable.setCellEditors(editors);
		certsTable.setCellModifier(new KeyModifier());
		certsTable.setColumnProperties(properties);
		if (certsTable != null) {
			certsTable.setInput(certsVector);
		}

		FormWidgetFactory.paintBordersFor(container);
	}

	/**
	 * Refreshes the viewer with information freshly obtained from the viewer's
	 * model.
	 */
	public void certsChanged() {
		if (certsTable != null) {
			certsTable.refresh();
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
			certsTable.refresh();
			return;
		}
		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof CheckedJarHolder) {
			CheckedJarHolder jarHolder = (CheckedJarHolder) changeObject;
			if (event.getChangeType() == IModelChangedEvent.ADD) {
				certsTable.add(jarHolder);
			}
			if (event.getChangeType() == IModelChangedEvent.INSERT) {
				certsTable.add(jarHolder);
				certsTable.editElement(jarHolder, 0);
			} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
				certsTable.remove(jarHolder);
			}
		} else {
			certsTable.refresh();
		}
	}

	/**
	 * Returns a <code>Vector</code> with Certificates info with the entered
	 * passwords.
	 * 
	 * @return a <code>Vector</code> with Certificates info with the entered
	 *         passwords
	 */
	public Vector getCertificateInfos() {
		return certsVector;
	}

	/**
	 * Sets the given deployment package file and given project location and
	 * sets all certificates info that's no passwords.
	 * 
	 * @param file
	 * @param location
	 */
	public void setDPPFileLocation(DPPFile file, String location) {
		this.dppFile = file;
		this.prjLocation = location;
		Vector infos = dppFile.getCertificateInfos();
		if (infos != null) {
			for (int i = 0; i < infos.size(); i++) {
				CertificateInfo info = (CertificateInfo) infos.elementAt(i);
				String keyPass = info.getKeypass();
				String storePass = info.getStorepass();
				if ((keyPass == null || keyPass.equals(""))
						|| (storePass == null || storePass.equals(""))) {
					CertificateInfo copyInfo = new CertificateInfo();
					copyInfo.setAlias(info.getAlias());
					copyInfo.setKeypass(info.getKeypass());
					copyInfo.setKeystore(info.getKeystore());
					copyInfo.setStorepass(info.getStorepass());
					copyInfo.setStoreType(info.getStoreType());
					certsVector.addElement(copyInfo);
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
		if (!hasChangedCerts()) {
			openResult = Window.CANCEL + Window.OK + 1;
		} else {
			openResult = super.open();
		}
		return openResult;
	}

	/**
	 * Returns whether there are the certificates info, which no passwords.
	 * 
	 * @return <code>true</code> if there are certificates info, in which no
	 *         passwords, otherwise returns <code>false</code>
	 */
	public boolean hasChangedCerts() {
		return (certsVector != null && certsVector.size() != 0);
	}

	/**
	 * Returns the relative of the project path of the given value.
	 * 
	 * @param value
	 *            the value, which will be relative for the project
	 * @return the relative of the project path value
	 */
	protected String getRelativePath(String value) {
		if (value.toLowerCase().startsWith(prjLocation.toLowerCase())) {
			value = "<.>" + value.substring(prjLocation.length());
		}
		return value;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {

		if (!errorLabel.isVisible()) {
			boolean hasErrors = false;
			int certLength = certsVector.size();
			for (int i = 0; i < certLength; ++i) {
				String storePass = ((CertificateInfo) certsVector.get(i))
						.getStorepass();
				if (storePass == null || "".equals(storePass)) {
					errorLabel.setVisible(true);

					hasErrors = true;
					break;
				}
			}

			if (!hasErrors) {
				super.okPressed();
			}
		}

	}
}