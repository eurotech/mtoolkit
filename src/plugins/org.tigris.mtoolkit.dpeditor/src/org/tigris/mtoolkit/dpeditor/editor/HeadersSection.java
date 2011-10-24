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
package org.tigris.mtoolkit.dpeditor.editor;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
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
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Version;
import org.tigris.mtoolkit.dpeditor.editor.base.DPPFormSection;
import org.tigris.mtoolkit.dpeditor.editor.event.EventConstants;
import org.tigris.mtoolkit.dpeditor.editor.event.TableControlListener;
import org.tigris.mtoolkit.dpeditor.editor.forms.FormWidgetFactory;
import org.tigris.mtoolkit.dpeditor.editor.model.DPPFileModel;
import org.tigris.mtoolkit.dpeditor.editor.model.IModelChangedEvent;
import org.tigris.mtoolkit.dpeditor.editor.model.ModelChangedEvent;
import org.tigris.mtoolkit.dpeditor.util.DPPErrorHandler;
import org.tigris.mtoolkit.dpeditor.util.ResourceManager;
import org.tigris.mtoolkit.util.CertificateInfo;
import org.tigris.mtoolkit.util.DPPConstants;
import org.tigris.mtoolkit.util.DPPFile;
import org.tigris.mtoolkit.util.DPPUtilities;
import org.tigris.mtoolkit.util.Header;
import org.tigris.mtoolkit.util.InconsistentDataException;
import org.tigris.mtoolkit.util.PackageHeaders;

/**
 * Creates the section that will be placed in the form, which represents the
 * visualization of the <code>PackageHeaders</code> in the Deployment package
 * file. This section gives possibility to add, remove, edit and move the
 * created <code>PackageHeaders</code>.
 */
public class HeadersSection extends DPPFormSection implements
		SelectionListener, ISelectionChangedListener {

	/** Holds the title of this section */
	public static final String SECTION_TITLE = "DPPEditor.HeadersSection.title";
	/** Holds the description of this section */
	public static final String SECTION_DESC = "DPPEditor.HeadersSection.desc";
	/** The New button's label */
	public static final String NEW_BUTTON = "DPPEditor.New_Button";
	/** The Remove button's label */
	public static final String REMOVE_BUTTON = "DPPEditor.Remove_Button";
	/** The Up button's label */
	public static final String UP_BUTTON = "DPPEditor.Up_Button";
	/** The Down button's label */
	public static final String DOWN_BUTTON = "DPPEditor.Down_Button";

	/** Message inform the user that Symbolic name and Version cannot be renamed */
	public static final String WARNING_VALUES_MSG = "DPPEditor.HeadersSection.SymNameVerWarning";
	/** Equals headers values error message */
	public static final String EQUAL_VALUES_MSG1 = "DPPEditor.HeadersSection.EqualValuesMsg1";
	/** Message ask to enter different header to continue */
	public static final String EQUAL_VALUES_MSG2 = "DPPEditor.HeadersSection.EqualValuesMsg2";
	/**
	 * Message announced of the incorrect value of the deployment package fix
	 * pack header
	 */
	public static final String WRONG_FIXPACK_HEADER = "DPPEditor.HeadersSection.WrongFixPackHeader";
	/**
	 * Message announced of the incorrect value of the deployment package
	 * version
	 */
	public static final String WRONG_VERSION = "DPPEditor.HeadersSection.WrongVersion";
	/**
	 * Message announced of the incorrect value of the deployment package
	 * symbolic name header
	 */
	public static final String WRONG_SYMBOLIC_NAME = "DPPEditor.HeadersSection.WrongSymbolicName";
	/**
	 * Message announced of the incorrect value of the deployment package
	 * symbolic name header
	 */
	public static final String WRONG_SYMBOLIC_NAME_SPACE = "DPPEditor.HeadersSection.WrongSymbolicNameSpace";
	/**
	 * Message announced of the incorrect value of the deployment package
	 * symbolic name header
	 */
	public static final String WRONG_SYMBOLIC_IDENTIFIER = "DPPEditor.HeadersSection.WrongSymbolicIdentifier";
	/** Message announced that deployment package headers cannot contains spaces */
	public static final String ERROR_SPACE_CONTAINT = "DPPEditor.HeadersSection.ErrorSpace";
	/** The error message that header key is not valid */
	public static final String ERROR_IVALID_KEY = "DPPEditor.HeadersSection.ErrorKey";

	/** Constant that indicates the add header action */
	private static final int ADD_HEADER = 0;
	/** Constant that indicates the remove header action */
	private static final int REMOVE_HEADER = 1;
	/** Constant that indicates the move up header action */
	private static final int UP_HEADER = 2;
	/** Constant that indicates the move down header action */
	private static final int DOWN_HEADER = 3;
	/**
	 * The String array, that holds all values of the headers key of the
	 * <code>PackageHeaders</code>
	 */
	private String[] data = { "", /*
								 * DPPConstants.dpSymbolicNameHeader,
								 * DPPConstants.dpVersionHeader,
								 */
	DPPConstants.dpFixPackHeader, DPPConstants.dpCopyrightHeader,
			DPPConstants.dpAddressHeader, DPPConstants.dpDescriptionHeader,
			DPPConstants.dpDocURLHeader, DPPConstants.dpVendorHeader,
			DPPConstants.dpLicenseHeader, DPPConstants.dpIcon,
			DPPConstants.dpName, DPPConstants.dpRequiredStorage };

	/** The parent composite in which all components will be added */
	private Composite container;
	/**
	 * The TableViewer, which table contains all <code>Header</code>s object of
	 * the <code>PackageHeaders</code> for the given Deployment package file.
	 */
	private TableViewer headerTable;
	/**
	 * The button, which action adds new header into table of all headers of the
	 * package headers
	 */
	private Button newButton;
	/** The button, which action removes selected in the table header */
	private Button removeButton;

	/**
	 * A cell editor that presents a list of items in a combo box for the
	 * header's key of the <code>PackageHeaders</code>.
	 */
	private ComboBoxCellEditor keyCellEditor;
	/**
	 * This table holds the correlation between the combo box values and their
	 * int value, which is needed to the label provider to shows the chosen
	 * value.
	 */
	private Hashtable comboValues = new Hashtable();
	/**
	 * A combo box for the header's key property of the
	 * <code>PackageHeaders</code>
	 */
	private CCombo keyCombo;

	/** The <code>boolean</code> flag that shows if the table is editable or not */
	private boolean isTableEditable = true;
	/**
	 * The deployment package file model, which <code>PackageHeaders</code> this
	 * section presents.
	 */
	private DPPFileModel model;
	/** The parents form page */
	private HeadersFormPage page;
	/**
	 * The <code>boolean</code> flag, that shows if this section needed of
	 * update
	 */
	private boolean updateNeeded = true;

	/**
	 * A cell modifier is used to access the data model from a cell editor.
	 */
	class KeyModifier implements ICellModifier {
		/**
		 * Checks whether the given property of the given element can be
		 * modified.
		 * 
		 * @param object
		 *            the element
		 * @param property
		 *            the property
		 * @return <code>true</code> if the property can be modified, and
		 *         <code>false</code> otherwise
		 * 
		 * @see org.eclipse.jface.viewers.ICellModifier#canModify(java.lang.Object,
		 *      java.lang.String)
		 */
		public boolean canModify(Object object, String property) {
			Header header = (Header) object;
			String key = header.getKey();
			if (property.equals("key")) {
				if (key.equals(DPPConstants.dpSymbolicNameHeader)
						|| key.equals(DPPConstants.dpVersionHeader)) {
					return false;
				}
			}
			if (property.equals("value")) {
				String value = header.getValue();
				if (key.equals("") && value.equals("")) {
					return false;
				}
			}
			return isTableEditable;
		}

		/**
		 * Modifies the value for the given property of the given element. In
		 * this class the properties for which this method works are: key and
		 * value.
		 * 
		 * @param object
		 *            the model element
		 * @param property
		 *            the property
		 * @param value
		 *            the new property value
		 * 
		 * @see org.eclipse.jface.viewers.ICellModifier#modify(java.lang.Object,
		 *      java.lang.String, java.lang.Object)
		 */
		public void modify(Object object, String property, Object value) {
			TableItem item = (TableItem) object;
			Header header = (Header) item.getData();
			String newValue = value.toString();
			boolean isSet = false;
			DPPEditor.isDialogShown = false;

			if (property.equals("key")) {
				if (comboValues.containsValue(newValue)) {
					Enumeration keys = comboValues.keys();
					while (keys.hasMoreElements()) {
						String key = (String) keys.nextElement();
						String comboValue = (String) comboValues.get(key);
						if (comboValue.equals(newValue)) {
							newValue = key;
							break;
						}
					}
				} else {
					newValue = keyCombo.getText();
				}
				String headerKey = header.getKey();
				if (headerKey.equals(newValue)) {
					return;
				}
				if (newValue.length() > 0 && newValue.trim().equals("")) {
					showErrorTableDialog(ResourceManager
							.getString(ERROR_SPACE_CONTAINT));
					DPPEditor.isDialogShown = true;
					return;
				}
				newValue = newValue.trim();
				if (newValue.indexOf(' ') != -1) {
					showErrorTableDialog(ResourceManager
							.getString(ERROR_SPACE_CONTAINT));
					DPPEditor.isDialogShown = true;
					return;
				}
				if (!DPPUtilities.isValidManifestHeader(newValue)) {
					DPPErrorHandler.showErrorTableDialog(ResourceManager
							.getString(ERROR_IVALID_KEY));
					return;
				}
				if ((!newValue.equals(""))
						&& (itemExists(headerTable, item, newValue) != -1)) {
					showErrorTableDialog(ResourceManager
							.getString(EQUAL_VALUES_MSG1)
							+ "\n"
							+ ResourceManager.getString(EQUAL_VALUES_MSG2));
					DPPEditor.isDialogShown = true;
					return;
				}
				if (headerKey.equals(DPPConstants.dpSymbolicNameHeader)
						|| headerKey.equals(DPPConstants.dpVersionHeader)
						|| newValue.equals(DPPConstants.dpSymbolicNameHeader)
						|| newValue.equals(DPPConstants.dpVersionHeader)) {
					if (!headerKey.equals(DPPConstants.dpFixPackHeader)) {
						showWarningTableDialog(ResourceManager
								.getString(WARNING_VALUES_MSG));
						DPPEditor.isDialogShown = true;
					}
					return;
				}
				if (newValue.equals(DPPConstants.dpVersionHeader)) {
					try {
						Version.parseVersion(header.getValue());
					} catch (IllegalArgumentException ex) {
						showErrorTableDialog(ResourceManager
								.getString(WRONG_VERSION));
						DPPEditor.isDialogShown = true;
						headerTable.editElement(header, 1);
						return;
					}
				} else if (newValue.equals(DPPConstants.dpFixPackHeader)) {
					String headerValue = header.getValue();
					if (!headerValue.equals("")
							&& !DPPUtilities.isValidFixPack(headerValue)) {
						if (!DPPUtilities.isValidVersion(headerValue)) {
							showErrorTableDialog(ResourceManager
									.getString(WRONG_FIXPACK_HEADER));
							DPPEditor.isDialogShown = true;
							return;
						}
					}
				}
				if (headerKey.equals(newValue)) {
					return;
				}

				header.setKey(newValue);
				DPPFile dppFile = ((DPPFileModel) getFormPage().getModel())
						.getDPPFile();
				PackageHeaders pkgHeaders = dppFile.getPackageHeaders();
				pkgHeaders.editElement(headerKey, header.getKey(),
						header.getValue());
				isSet = true;
				removeButton.setEnabled(true);
			} else if (property.equals("value")) {
				if (newValue.equals(header.getValue())) {
					return;
				}
				if (header.getKey().equals(DPPConstants.dpVersionHeader)) {
					int index = newValue.lastIndexOf('.');
					String verTxt = newValue;
					if (verTxt.indexOf(" ") != -1
							|| (index != -1 && (index == newValue.length()))) {
						showErrorTableDialog(ResourceManager
								.getString(WRONG_VERSION));
						DPPEditor.isDialogShown = true;
						return;
					}
					if (verTxt.indexOf(" ") == -1) {
						try {
							Version.parseVersion(verTxt);
							header.setValue(newValue);
						} catch (IllegalArgumentException ex) {
							showErrorTableDialog(ResourceManager
									.getString(WRONG_VERSION));
							DPPEditor.isDialogShown = true;
							return;
						}
					}
				} else if (header.getKey().equals(DPPConstants.dpFixPackHeader)) {
					if ("".equals(newValue)
							|| DPPUtilities.isValidFixPack(newValue)) {
						header.setValue(newValue);
					} else {
						if (DPPUtilities.isValidVersion(newValue)) {
							header.setValue(newValue);
						} else {
							showErrorTableDialog(ResourceManager
									.getString(WRONG_FIXPACK_HEADER));
							DPPEditor.isDialogShown = true;
							return;
						}
					}
				} else if (header.getKey().equals(
						DPPConstants.dpSymbolicNameHeader)) {
					if (!DPPUtilities.isCorrectPackage(newValue)) {
						if (newValue.startsWith(".") || newValue.endsWith(".")) {
							showErrorTableDialog(ResourceManager.format(
									WRONG_SYMBOLIC_NAME,
									new Object[] { newValue }));
						} else if (newValue.indexOf(" ") != -1) {
							showErrorTableDialog(ResourceManager.format(
									WRONG_SYMBOLIC_NAME_SPACE,
									new Object[] { newValue }));
						} else {
							showErrorTableDialog(ResourceManager.format(
									WRONG_SYMBOLIC_IDENTIFIER,
									new Object[] { newValue }));
						}
						DPPEditor.isDialogShown = true;
						return;
					}
				}
				header.setValue(newValue);
				isSet = true;
				packageHeadersChange(header, ADD_HEADER);
			}
			setDirty(true);
			commitChanges(false);
			headerTable.update(header, null);
			page.updateDocumentIfSource();
			if (isSet) {
				model.fireModelChanged(new ModelChangedEvent(
						IModelChangedEvent.EDIT, new Object[] { header }, null));
			}
			updateNeeded = true;
		}

		private boolean haveInvalidChars(String fileName) {
			if (fileName == null)
				return false;
			StringTokenizer t = new StringTokenizer(fileName, "/");
			while (t.hasMoreElements()) {
				String next = ((String) t.nextElement()).trim();
				if (!DPPUtilities.isValidPath(next)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Returns the value for the given property of the given element.
		 * Returns empty string if the element does not have the given property.
		 * The values of the property that are allowed are: key and value.
		 * 
		 * @param object
		 *            the element
		 * @param property
		 *            the property
		 * @return the property value
		 * 
		 * @see org.eclipse.jface.viewers.ICellModifier#getValue(java.lang.Object,
		 *      java.lang.String)
		 */
		public Object getValue(Object object, String property) {
			Header header = (Header) object;
			if (property.equals("key")) {
				String key = header.getKey();
				String value = (String) comboValues.get(key);
				keyCellEditor.setItems(data);
				if (value == null) {
					if (key.equals("")) {
						value = "0";
					} else {
						int size = data.length;
						String[] items = new String[size + 1];
						for (int i = 0; i < size; i++) {
							items[i] = data[i];
						}
						items[size] = key;
						keyCellEditor.setItems(items);
						value = "" + size;
					}
				}
				return new Integer(value);
			} else if (property.equals("value")) {
				return header.getValue();
			}
			return "";
		}
	}

	/**
	 * A content provider mediates between the viewer's model and the viewer
	 * itself.
	 */
	class TableContentProvider extends DefaultContentProvider implements
			IStructuredContentProvider {
		/**
		 * Returns the elements to display in the viewer when its input is set
		 * to the given element.
		 * 
		 * @param parent
		 *            the input element
		 * @return the array of elements to display in the viewer
		 * 
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object parent) {
			if (parent instanceof DPPFile) {
				DPPFile dppFile = (DPPFile) parent;
				PackageHeaders pkgHeaders = dppFile.getPackageHeaders();
				Vector pkgHeadersVector = pkgHeaders.getHeadersAsVector();
				Object[] result = new Object[pkgHeadersVector.size()];
				for (int i = 0; i < pkgHeadersVector.size(); i++) {
					Header header = (Header) pkgHeadersVector.elementAt(i);
					if (header.getKey().equals(
							DPPConstants.dpSymbolicNameHeader)) {
						String value = header.getValue();
						if (value == null || value.equals("")) {
							value = dppFile.getFile().getName();
							int index = value.lastIndexOf(".");
							if (index != -1) {
								value = value.substring(0, index);
							}
							pkgHeaders.setSymbolicName(value);
							header.setValue(value);
						}
					} else if (header.getKey().equals(
							DPPConstants.dpVersionHeader)) {
						String value = header.getValue();
						if (value == null || value.equals("")) {
							value = "1.0";
							pkgHeaders.setVersion(value);
							header.setValue(value);
						}
					}
					result[i] = header;
				}
				return result;
			}
			return new Object[0];
		}
	}

	/**
	 * A label provider sets for the value of the given column index the value
	 * of the element, that corresponding with this index.
	 */
	class TableLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		/**
		 * Returns the label text for the given column of the given element.
		 * 
		 * @param obj
		 *            the object representing the entire row
		 * @param index
		 *            the zero-based index of the column in which the label
		 *            appears
		 * @return String or <code>null</code> if there is no text for the given
		 *         object at index
		 * 
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object,
		 *      int)
		 */
		public String getColumnText(Object obj, int index) {
			if (obj instanceof Header) {
				Header header = (Header) obj;
				if (index == 0) {
					return header.getKey();
				}
				if (index == 1) {
					return header.getValue();
				}
			}
			return obj.toString();
		}

		/**
		 * Returns the label image for the given column of the given element. In
		 * this case the return image is <code>null</code>
		 * 
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object,
		 *      int)
		 */
		public Image getColumnImage(Object obj, int index) {
			return null;
		}
	}

	/**
	 * Creates the new instance of the FormSection and the form which is the
	 * parent of this section.
	 * 
	 * @param page
	 *            the parent form page
	 */
	public HeadersSection(HeadersFormPage page) {
		super(page);
		this.page = page;
		setHeaderText(ResourceManager.getString(SECTION_TITLE, ""));
		setDescription(ResourceManager.getString(SECTION_DESC, ""));
	}

	/**
	 * This method is called from the <code>createControl</code> method and puts
	 * all custom components in this package headers form section.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the created
	 *            client composite which will be holds all custom controls
	 * @return Returns the composite control which will be holds all custom
	 *         controls
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.FormSection#createClient(org.eclipse.swt.widgets.Composite)
	 */
	public Composite createClient(Composite parent) {
		container = FormWidgetFactory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		container.setLayout(layout);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, "");

		createTable(container);
		createButtons(container);

		container.pack();
		return container;
	}

	/**
	 * Creates the table viewer and the table for this table viewer. Also
	 * creates all providers and listeners that the table needed to.
	 * 
	 * @param parent
	 *            a parent composite in which all components will be added
	 */
	private void createTable(Composite parent) {
		Composite container = FormWidgetFactory.createComposite(parent);
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		Table table = FormWidgetFactory.createTable(container, SWT.SINGLE
				| SWT.FULL_SELECTION);
		table.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent ev) {
				if (ev.keyCode == 27) {
					if (ev.getSource() instanceof Table) {
						Table table = (Table) ev.getSource();
						if (table.getSelectionIndex() < 0)
							return;
						TableItem item = table.getItem(table
								.getSelectionIndex());
						final Header header = (Header) item.getData();
						if (header.getKey().equals("")) {
							packageHeadersChange(header, REMOVE_HEADER);
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
				ResourceManager.getString("DPPEditor.HeadersSection.ColKey"),
				ResourceManager.getString("DPPEditor.HeadersSection.ColValue") };
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

		comboValues.put("", "0");
		comboValues.put(DPPConstants.dpFixPackHeader, "1");
		comboValues.put(DPPConstants.dpCopyrightHeader, "2");
		comboValues.put(DPPConstants.dpAddressHeader, "3");
		comboValues.put(DPPConstants.dpDescriptionHeader, "4");
		comboValues.put(DPPConstants.dpDocURLHeader, "5");
		comboValues.put(DPPConstants.dpVendorHeader, "6");
		comboValues.put(DPPConstants.dpLicenseHeader, "7");
		comboValues.put(DPPConstants.dpIcon, "8");
		comboValues.put(DPPConstants.dpName, "9");
		comboValues.put(DPPConstants.dpRequiredStorage, "10");

		keyCellEditor = new ComboBoxCellEditor(table, data, SWT.NULL);
		CellEditor[] editors = new CellEditor[] { keyCellEditor,
				new TextCellEditor(table) };
		String[] properties = { "key", "value" };
		headerTable.setCellEditors(editors);
		headerTable.setCellModifier(new KeyModifier());
		headerTable.setColumnProperties(properties);

		keyCombo = (CCombo) keyCellEditor.getControl();
		keyCombo.setSize(5, 5);
		keyCombo.addSelectionListener(this);

		FormWidgetFactory.paintBordersFor(container);
	}

	/**
	 * Creates all navigate button for the created table.
	 * 
	 * @param parent
	 *            a parent composite in which all button components will be
	 *            added
	 */
	private void createButtons(Composite parent) {
		Composite buttonComposite = FormWidgetFactory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 4;
		buttonComposite.setLayout(layout);
		buttonComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		newButton = FormWidgetFactory.createButton(buttonComposite,
				ResourceManager.getString(NEW_BUTTON, ""), SWT.PUSH);
		removeButton = FormWidgetFactory.createButton(buttonComposite,
				ResourceManager.getString(REMOVE_BUTTON, ""), SWT.PUSH);

		newButton.addSelectionListener(this);
		GridData gd = new GridData(GridData.FILL_VERTICAL);
		gd.widthHint = removeButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		gd.verticalAlignment = GridData.BEGINNING;
		newButton.setLayoutData(gd);

		removeButton.addSelectionListener(this);
		gd = new GridData(GridData.FILL_VERTICAL);
		gd.verticalAlignment = GridData.BEGINNING;
		removeButton.setLayoutData(gd);
	}

	// Logic part
	/**
	 * Initializes the all custom created controls with the given
	 * <code>Object</code>
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.FormSection#initialize(java.lang.Object)
	 */
	public void initialize(Object input) {
		model = (DPPFileModel) input;
		if (model != null) {
			headerTable.setInput(model.getDPPFile());
			removeButton.setEnabled(false);
		}
	}

	/**
	 * Sets that this form section is changed and need to be saved.
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.FormSection#commitChanges(boolean)
	 */
	public void commitChanges(boolean onSave) {
		setDirty(false);
	}

	/**
	 * Removes added to this page model ModelChangeListener and calls dispose
	 * for the every component from the this FormSection.
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.FormSection#dispose()
	 */
	public void dispose() {
		DPPFileModel model = (DPPFileModel) getFormPage().getModel();
		if (model != null) {
			model.removeModelChangedListener(this);
		}
		super.dispose();
	}

	/**
	 * Sets the focus to the table of the package headers and refresh the
	 * viewer.
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.FormSection#setFocus()
	 */
	public void setFocus() {
		headerTable.getTable().setFocus();
		TableItem[] items = headerTable.getTable().getItems();
		for (int i = 0; i < items.length; i++) {
			Header header = (Header) items[i].getData();
			String key = DPPUtilities.getStringValue(header.getKey());
			if (key.equals("")) {
				headerTable.editElement(header, 0);
				break;
			}
		}
	}

	/**
	 * Sets remove button enable or disable, depending on the selection in the
	 * table.
	 */
	private void updateEnabledButtons() {
		Table table = headerTable.getTable();
		TableItem[] selection = table.getSelection();
		boolean hasSelection = selection.length > 0;
		removeButton.setEnabled(hasSelection);
	}

	/**
	 * Updates the values of this FormSection with the value gets from the
	 * model.
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.FormSection#update()
	 */
	public void update() {
		this.update(getFormPage().getModel());
	}

	/**
	 * Updates the values of this form section with the values from the given
	 * input object.
	 * 
	 * @param input
	 *            the object from which new values will be get
	 */
	public void update(Object input) {
		DPPFileModel model = (DPPFileModel) input;
		if (model == null) {
			return;
		}
		DPPFile dppFile = model.getDPPFile();
		boolean isPageConsistent = true;

		if (!DPPEditor.isDialogShown) {
			DPPEditor.isDialogShown = false;
			try {
				dppFile.checkConsistency();
			} catch (InconsistentDataException e) {
				DPPErrorHandler.processError(e);
				isPageConsistent = false;
				
				if (!DPPEditor.isDialogShown) {
					DPPEditor.isDialogShown = true;
					DPPEditor.showErrorDialog(e.getMessage());
				}
			}

			if (isPageConsistent) {				
				headerTable.refresh();
			}	
		}

		updateNeeded = false;
	}

	// ISelectionListener Implementation
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
		if (e.getSource() instanceof Button) {
			Button source = (Button) e.getSource();
			if (source == newButton) {
				handleNew();
			} else if (source == removeButton) {
				handleRemove();
			}
		} else if (obj instanceof CCombo) {
			CCombo combo = (CCombo) obj;
			int index = combo.getSelectionIndex();
			String item = combo.getItem(index);
			Object object = ((IStructuredSelection) headerTable.getSelection())
					.getFirstElement();
			if (object != null && object instanceof Header) {
				Header header = (Header) object;
				if (combo == keyCombo) {
					String oldKey = header.getKey();
					if (oldKey.equals(item)) {
						return;
					}
					int newIndex = 0;
					Enumeration keys = comboValues.keys();
					while (keys.hasMoreElements()) {
						String key = (String) keys.nextElement();
						if (key.equals(oldKey)) {
							String comboValue = (String) comboValues.get(key);
							newIndex = new Integer(comboValue).intValue();
							break;
						}
					}
					if (oldKey.equals(DPPConstants.dpSymbolicNameHeader)
							|| oldKey.equals(DPPConstants.dpVersionHeader)) {
						showWarningTableDialog(ResourceManager
								.getString(WARNING_VALUES_MSG));
						combo.select(newIndex);
						return;
					}
					TableItem[] selection = headerTable.getTable()
							.getSelection();
					int itemExists = itemExists(headerTable, selection[0], item);
					if (itemExists == -1) {
						String value = header.getValue();
						if (item.equals(DPPConstants.dpVersionHeader)
								&& !DPPUtilities.isValidVersion(value)) {
							showErrorTableDialog(ResourceManager
									.getString(WRONG_VERSION));
							return;
						}
						if (item.equals(DPPConstants.dpFixPackHeader)
								&& !value.equals("")
								&& !DPPUtilities.isValidFixPack(value)) {
							if (!DPPUtilities.isValidVersion(value)) {
								showErrorTableDialog(ResourceManager
										.getString(WRONG_FIXPACK_HEADER));
								return;
							}
						}
					} else {
						showErrorTableDialog(ResourceManager
								.getString(EQUAL_VALUES_MSG1)
								+ "\n"
								+ ResourceManager.getString(EQUAL_VALUES_MSG2));
						combo.removeSelectionListener(this);
						combo.select(newIndex);
						combo.addSelectionListener(this);
						return;
					}
				}
			}
		}
	}

	/**
	 * Creates the new <code>Header</code> object, which is presented in the
	 * table as a new table row. Adds this header in the
	 * <code>PackageHeaders</code> and adds it to the Deployment package file,
	 * which headers presents this table.
	 */
	private void handleNew() {
		Table table = headerTable.getTable();
		int size = table.getItems().length;
		Header header = new Header();
		boolean found = false;

		for (int i = 0; i < size; i++) {
			TableItem currentItem = table.getItem(i);
			if (currentItem.getText(0).equalsIgnoreCase("")
					&& !currentItem.getData().equals(header)) {
				found = true;
				break;
			}
		}

		if (!found) {
			packageHeadersChange(header, ADD_HEADER);
			headerTable.add(header);
			headerTable.editElement(header, 0);
			setDirty(true);
			commitChanges(false);
			size++;
		}

		table.setSelection(size - 1);
		table.setFocus();
		updateEnabledButtons();
	}

	/**
	 * Removes from the headers table the selected <code>Header</code> object.
	 * Removes this header from the <code>PackageHeaders</code>, which headers
	 * presents this table.
	 */
	private void handleRemove() {
		Object object = ((IStructuredSelection) headerTable.getSelection())
				.getFirstElement();
		if (object != null && object instanceof Header) {
			Header header = (Header) object;
			// deployment package symbolic name ant deployment package version
			// must not be removed!
			if (header.getKey().equals(DPPConstants.dpSymbolicNameHeader)
					|| header.getKey().equals(DPPConstants.dpVersionHeader)) {
				return;
			}
			packageHeadersChange(header, REMOVE_HEADER);
			headerTable.remove(header);
		}
		setDirty(true);
		commitChanges(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org
	 * .eclipse.jface.viewers.SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent e) {
		StructuredSelection selection = (StructuredSelection) e.getSelection();
		getFormPage().setSelection(selection);
		if (isTableEditable || removeButton.getEnabled()) {
			updateEnabledButtons();
		}
		if (selection != null) {
			Object first = selection.getFirstElement();
			if (first instanceof Header) {
				String key = ((Header) first).getKey().trim();
				if (key.equals(DPPConstants.dpSymbolicNameHeader.trim())
						|| key.equals(DPPConstants.dpVersionHeader.trim())) {
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
	 * Called when there is a change in the model this model listener is
	 * registered with.
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.base.DPPFormSection#modelChanged(org.tigris.mtoolkit.dpeditor.editor.model.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent event) {
	}

	/**
	 * Sets the management buttons to be enabled or disabled, depending on the
	 * given <code>boolean</code> flag.
	 * 
	 * @param editable
	 *            the new enabled state of the buttons
	 */
	public void setEditable(boolean editable) {
		isTableEditable = editable;
		newButton.setEnabled(editable);
		removeButton.setEnabled(editable);
	}

	/**
	 * Refreshes the header viewer completely with information freshly obtained
	 * from the viewer's model.
	 */
	public void headersChanged() {
		if (headerTable != null) {
			headerTable.refresh();
		}
	}

	/**
	 * Adds, removes, moves up or down the given <code>Header</code>, depending
	 * on the given key. Notifies all existing
	 * <code>IModelChangedListener</code>'s of a change of the model.
	 * 
	 * @param header
	 *            the <code>Header</code>, on which will be done the action
	 * @param key
	 *            the type of the action. This type can be one of the followed
	 *            values: ADD_HEADER, REMOVE_HEADER, UP_HEADER and DOWN_HEADER
	 */
	private void packageHeadersChange(Header header, int key) {
		DPPFileModel model = (DPPFileModel) getFormPage().getModel();
		DPPFile dppFile = model.getDPPFile();
		PackageHeaders pkgHeaders = dppFile.getPackageHeaders();
		switch (key) {
		case ADD_HEADER:
			pkgHeaders.addElement(header.getKey(), header.getValue());
			model.fireModelChanged(new ModelChangedEvent(
					IModelChangedEvent.ADD, new Object[] { header }, null));
			break;
		case REMOVE_HEADER:
			pkgHeaders.removeElement(header.getKey());
			model.fireModelChanged(new ModelChangedEvent(
					IModelChangedEvent.REMOVE, new Object[] { header }, null));
			break;
		case UP_HEADER:
			Vector vector = new Vector();
			TableItem[] items = headerTable.getTable().getItems();
			for (int i = 0; i < items.length; i++) {
				vector.addElement((Header) items[i].getData());
			}
			DPPUtilities.moveElement(vector, header, true);
			break;
		case DOWN_HEADER:
			Vector vec = new Vector();
			TableItem[] itms = headerTable.getTable().getItems();
			for (int i = 0; i < itms.length; i++) {
				vec.addElement((Header) itms[i].getData());
			}
			DPPUtilities.moveElement(vec, header, false);
			break;
		}
		dppFile.setPackageHeaders(pkgHeaders);
	}
}
