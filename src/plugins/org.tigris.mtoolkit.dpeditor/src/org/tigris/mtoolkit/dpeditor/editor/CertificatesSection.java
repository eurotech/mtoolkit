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
import java.util.Vector;

import org.eclipse.core.resources.IFile;
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
import org.tigris.mtoolkit.dpeditor.editor.base.CustomCellEditor;
import org.tigris.mtoolkit.dpeditor.editor.base.DPPFormSection;
import org.tigris.mtoolkit.dpeditor.editor.event.EventConstants;
import org.tigris.mtoolkit.dpeditor.editor.event.TableControlListener;
import org.tigris.mtoolkit.dpeditor.editor.forms.FormWidgetFactory;
import org.tigris.mtoolkit.dpeditor.editor.model.DPPFileModel;
import org.tigris.mtoolkit.dpeditor.editor.model.IModelChangedEvent;
import org.tigris.mtoolkit.dpeditor.editor.model.ModelChangedEvent;
import org.tigris.mtoolkit.dpeditor.util.ResourceManager;
import org.tigris.mtoolkit.util.CertificateInfo;
import org.tigris.mtoolkit.util.DPPFile;
import org.tigris.mtoolkit.util.DPPUtilities;

/**
 * Creates the section that will be placed in the form, which represents the
 * visualization of the all <code>CertificateInfo</code>s in the Deployment
 * package file. This section gives possibility to add, remove and edit the
 * created <code>CertificateInfo</code>s.
 */
public class CertificatesSection extends DPPFormSection implements
		SelectionListener, ISelectionChangedListener {

	/** Holds the title of the section */
	public static final String SECTION_TITLE = "DPPEditor.CertificatesSection.title";
	/** Holds the description of the section */
	public static final String SECTION_DESC = "DPPEditor.CertificatesSection.desc";
	/** The New button's label */
	public static final String NEW_BUTTON = "DPPEditor.New_Button";
	/** The Remove button's label */
	public static final String REMOVE_BUTTON = "DPPEditor.Remove_Button";
	/** The Up button's label */
	public static final String UP_BUTTON = "DPPEditor.Up_Button";
	/** The Down button's label */
	public static final String DOWN_BUTTON = "DPPEditor.Down_Button";

	/** Equals certificate alias values error message */
	public static final String EQUAL_VALUES_MSG1 = "DPPEditor.CertificatesSection.EqualValuesMsg1";
	/** Message ask to enter different certificate alias to continue */
	public static final String EQUAL_VALUES_MSG2 = "DPPEditor.CertificatesSection.EqualValuesMsg2";
	/** Message announced the incorrect value of the certificate's alias */
	public static final String WRONG_CERT_ALIAS = "DPPEditor.CertificatesSection.WrongAlias";

	/** Constant that indicates the add new certificate action */
	private static final int ADD_CERTIFICATE = 0;
	/** Constant that indicates the remove certificate action */
	private static final int REMOVE_CERTIFICATE = 1;
	/** Constant that indicates the move up certificate action */
	private static final int UP_CERTIFICATE = 2;
	/** Constant that indicates the move down certificate action */
	private static final int DOWN_CERTIFICATE = 3;

	/**
	 * The String array, that holds all values of the store types of the
	 * <code>SertificateInfo</code>
	 */
	private String[] sData = { "", "jks", "pkcs11", "pkcs12" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	/** The parent composite in which all components will be added */
	private Composite container;

	/**
	 * The TableViewer, which table contains all <code>CertificateInfo</code>s
	 * object for the given Deployment package file.
	 */
	private TableViewer certsTable;
	/**
	 * The button, which action adds new certificate into table of all
	 * certificates
	 */
	private Button newButton;
	/** The button, which action removes selected in the table certificate */
	private Button removeButton;
	/**
	 * The button, which is used for specifying whether the bundles should be
	 * signed
	 */
	private Button signBundlesButton;

	/**
	 * A cell editor that presents a list of items in a combo box for the store
	 * type property of the <code>CertificateInfo</code>.
	 */
	private ComboBoxCellEditor cellEditor;
	/**
	 * A combo box for the store type property of the
	 * <code>CertificateInfo</code>
	 */
	private CCombo comboEditor;
	/**
	 * This table holds the correlation between the combo box values and their
	 * int value, which is needed to the label provider to shows the chosen
	 * value.
	 */
	private Hashtable comboValues = new Hashtable();

	/** The <code>boolean</code> flag that shows if the table is editable or not */
	private boolean isTableEditable = true;

	private boolean ignoreSelectionEvents = false;
	/**
	 * The deployment package file model, which <code>CertificateIndo</code>s
	 * this section presents.
	 */
	private DPPFileModel model;
	/** The parents form page */
	private CertificatesFormPage page;

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
			return isTableEditable;
		}

		/**
		 * Modifies the value for the given property of the given element. In
		 * this class the properties for which this method works are: alias,
		 * keystore, storepass, keypass, storetype.
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
			if (item == null)
				return;
			CertificateInfo cert = (CertificateInfo) item.getData();
			String newValue = value.toString();
			boolean isSet = false;

			if (property.equals("alias")) {
				if (newValue.equals(cert.getAlias()) && (!newValue.equals(""))) {
					return;
				}
				if ((!newValue.equals("")) && (itemExists(certsTable, item, newValue) != -1)) {
					showErrorTableDialog(ResourceManager.getString(EQUAL_VALUES_MSG1) + "\n" + ResourceManager.getString(EQUAL_VALUES_MSG2));
					certificateInfoChange(cert, REMOVE_CERTIFICATE);
					return;
				}
				isSet = true;
				cert.setAlias(newValue);
			} else if (property.equals("keystore")) {
				if (newValue.equals(cert.getKeystore()) && (!newValue.equals(""))) {
					return;
				}
				isSet = true;
				cert.setKeystore(newValue);
			} else if (property.equals("storepass")) {
				isSet = true;
				cert.setStorepass(newValue);
			} else if (property.equals("keypass")) {
				isSet = true;
				cert.setKeypass(newValue);
			} else if (property.equals("storetype")) {
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
					newValue = comboEditor.getText();
				}
				if (newValue.equals(cert.getStoreType()))
					return;
				isSet = true;
				cert.setStoreType(newValue);
			}
			removeButton.setEnabled(true);
			setDirty(true);
			commitChanges(false);
			certsTable.update(cert, null);
			page.updateDocumentIfSource();
			if (isSet) {
				model.fireModelChanged(new ModelChangedEvent(IModelChangedEvent.EDIT, new Object[] { cert }, null));
			}
		}

		/**
		 * Returns the value for the given property of the given element.
		 * Returns empty string if the element does not have the given property.
		 * The values of the property that are allowed are: alias, keystore,
		 * storepass, keypass, storetype.
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
			CertificateInfo certificate = (CertificateInfo) object;
			if (property.equals("alias")) {
				return DPPUtilities.getStringValue(certificate.getAlias());
			} else if (property.equals("keystore")) {
				String keyStore = DPPUtilities.getStringValue(certificate.getKeystore());
				return getRelativePath(keyStore);
			} else if (property.equals("storepass")) {
				return DPPUtilities.getStringValue(certificate.getStorepass());
			} else if (property.equals("keypass")) {
				return DPPUtilities.getStringValue(certificate.getKeypass());
			} else if (property.equals("storetype")) {
				String type = DPPUtilities.getStringValue(certificate.getStoreType());
				String value = (String) comboValues.get(type);
				cellEditor.setItems(sData);
				if (value == null) {
					if (type.equals("")) {
						value = "0";
					} else {
						int size = sData.length;
						String[] items = new String[size + 1];
						for (int i = 0; i < size; i++) {
							items[i] = sData[i];
						}
						items[size] = type;
						cellEditor.setItems(items);
						value = "" + size;
					}
				}
				return new Integer(value);
			}
			return "";
		}
	}

	/**
	 * A content provider mediates between the viewer's model and the viewer
	 * itself.
	 */
	class TableContentProvider implements
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
				Vector infos = ((DPPFile) parent).getCertificateInfos();
				if (infos == null) {
					return null;
				}
				CertificateInfo[] result = new CertificateInfo[infos.size()];
				infos.copyInto(result);
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
			if (obj instanceof CertificateInfo) {
				CertificateInfo certificate = (CertificateInfo) obj;
				if (index == 0) {
					return certificate.getAlias();
				} else if (index == 1) {
					String keyStore = DPPUtilities.getStringValue(certificate.getKeystore());
					return getRelativePath(keyStore);
				} else if (index == 2) {
					String storePass = certificate.getStorepass();
					String returnPass = "";
					if (storePass != null) {
						for (int i = 0; i < storePass.length(); i++) {
							returnPass += "*";
						}
					}
					return returnPass;
				} else if (index == 3) {
					String keyPass = certificate.getKeypass();
					String returnPass = "";
					if (keyPass != null) {
						for (int i = 0; i < keyPass.length(); i++) {
							returnPass += "*";
						}
					}
					return returnPass;
				} else if (index == 4) {
					return certificate.getStoreType();
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
	public CertificatesSection(CertificatesFormPage page) {
		super(page);
		this.page = page;
		setHeaderText(ResourceManager.getString(SECTION_TITLE, ""));
		setDescription(ResourceManager.getString(SECTION_DESC, ""));
	}

	/**
	 * This method is called from the <code>createControl</code> method and puts
	 * all custom components in this certificates form section.
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

		Table table = FormWidgetFactory.createTable(container, SWT.SINGLE | SWT.FULL_SELECTION);
		table.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent ev) {
				if (ev.keyCode == 27) {
					if (ev.getSource() instanceof Table) {
						Table table = (Table) ev.getSource();
						if (table.getSelectionIndex() < 0)
							return;
						TableItem item = table.getItem(table.getSelectionIndex());
						final CertificateInfo certificate = (CertificateInfo) item.getData();
						if (certificate != null && certificate.getAlias() != null && certificate.getAlias().equals("")) {
							certificateInfoChange(certificate, REMOVE_CERTIFICATE);
						}
					}
				}
			}
		});
		table.setLayout(new GridLayout());
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		String[] columnTitles = { ResourceManager.getString("DPPEditor.CertificatesSection.ColAlias"), ResourceManager.getString("DPPEditor.CertificatesSection.ColKeystore"), ResourceManager.getString("DPPEditor.CertificatesSection.ColStorePassword"), ResourceManager.getString("DPPEditor.CertificatesSection.ColKeyPassword"), ResourceManager.getString("DPPEditor.CertificatesSection.ColStoreType") };
		for (int i = 0; i < columnTitles.length; i++) {
			TableColumn tableColumn = new TableColumn(table, SWT.NULL);
			tableColumn.setText(columnTitles[i]);
		}

		TableControlListener controlListener = new TableControlListener(table);
		controlListener.setResizeMode(EventConstants.CERTIFICATES_RESIZE_MODE);
		container.addControlListener(controlListener);

		certsTable = new TableViewer(table);
		certsTable.setContentProvider(new TableContentProvider());
		certsTable.setLabelProvider(new TableLabelProvider());
		certsTable.addSelectionChangedListener(this);

		cellEditor = new ComboBoxCellEditor(table, sData);
		CellEditor[] editors = new CellEditor[] { new TextCellEditor(table), new CustomCellEditor(container, certsTable, table, CustomCellEditor.TEXT_BUTTON_TYPE, CustomCellEditor.CERT_KEYSTORE), new TextCellEditor(table, SWT.PASSWORD), new TextCellEditor(table, SWT.PASSWORD), cellEditor };
		comboValues.put("", "0");
		comboValues.put("jks", "1");
		comboValues.put("pkcs11", "2");
		comboValues.put("pkcs12", "3");
		String[] properties = { "alias", "keystore", "storepass", "keypass", "storetype" };
		certsTable.setCellEditors(editors);
		certsTable.setCellModifier(new KeyModifier());
		certsTable.setColumnProperties(properties);

		comboEditor = (CCombo) cellEditor.getControl();
		comboEditor.setSize(5, 5);
		comboEditor.addSelectionListener(this);

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
		layout.numColumns = 3;
		buttonComposite.setLayout(layout);
		buttonComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		newButton = FormWidgetFactory.createButton(buttonComposite, ResourceManager.getString(NEW_BUTTON, ""), SWT.PUSH);
		removeButton = FormWidgetFactory.createButton(buttonComposite, ResourceManager.getString(REMOVE_BUTTON, ""), SWT.PUSH);
		signBundlesButton = FormWidgetFactory.createButton(buttonComposite, "Also sign bundles included in the package", SWT.CHECK);

		newButton.addSelectionListener(this);
		GridData gd = new GridData(GridData.FILL_VERTICAL);
		gd.widthHint = removeButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		gd.verticalAlignment = GridData.BEGINNING;
		newButton.setLayoutData(gd);

		removeButton.addSelectionListener(this);
		gd = new GridData(GridData.FILL_VERTICAL);
		gd.verticalAlignment = GridData.BEGINNING;
		removeButton.setLayoutData(gd);

		signBundlesButton.addSelectionListener(this);
		gd = new GridData(SWT.RIGHT, SWT.CENTER, true, false);
		signBundlesButton.setLayoutData(gd);
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
			certsTable.setInput(model.getDPPFile());
			removeButton.setEnabled(false);
			ignoreSelectionEvents = true;
			try {
				signBundlesButton.setSelection(model.getDPPFile().getSignBundles());
			} finally {
				ignoreSelectionEvents = false;
			}
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
	 * Sets the focus to the table of the certificates and refresh the viewer.
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.FormSection#setFocus()
	 */
	public void setFocus() {
		certsTable.getTable().setFocus();
		certsTable.refresh();
		TableItem[] items = certsTable.getTable().getItems();
		for (int i = 0; i < items.length; i++) {
			CertificateInfo info = (CertificateInfo) items[i].getData();
			String alias = DPPUtilities.getStringValue(info.getAlias());
			if (alias.equals("")) {
				certsTable.editElement(info, 0);
				break;
			}
		}
	}

	/**
	 * Sets remove button enable or disable, depending on the selection in the
	 * table.
	 */
	private void updateEnabledButtons() {
		Table table = certsTable.getTable();
		TableItem[] selection = table.getSelection();
		boolean hasSelection = selection.length > 0;
		removeButton.setEnabled(hasSelection);
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
		if (ignoreSelectionEvents)
			return;
		Object obj = e.getSource();
		if (obj instanceof Button) {
			Button source = (Button) obj;
			if (source == newButton) {
				handleNew();
			} else if (source == removeButton) {
				handleRemove();
			} else if (source == signBundlesButton) {
				handleSignBundleChange();
			}
		} else if (obj instanceof CCombo) {
			CCombo combo = (CCombo) obj;
			int index = combo.getSelectionIndex();
			String item = combo.getItem(index);
			Object object = ((IStructuredSelection) certsTable.getSelection()).getFirstElement();
			if (object != null && object instanceof CertificateInfo) {
				CertificateInfo certInfo = (CertificateInfo) object;
				if (combo == comboEditor) {
					String oldType = certInfo.getStoreType();
					oldType = oldType == null ? "" : oldType;
					if (!oldType.equals(item)) {
						model.fireModelChanged(new ModelChangedEvent(IModelChangedEvent.EDIT, new Object[] { certInfo }, null));
					}
				}
			}
		}
	}

	/**
	 * Creates the new <code>CertificateInfo</code> object, which is presented
	 * in the table as a new table row. Adds this certificate into the
	 * Deployment package file, which certificates presents this table.
	 */
	private void handleNew() {
		CertificateInfo cert = new CertificateInfo();
		certificateInfoChange(cert, ADD_CERTIFICATE);
		certsTable.add(cert);
		certsTable.editElement(cert, 0);
		setDirty(true);
		commitChanges(false);
		removeButton.setEnabled(false);
	}

	/**
	 * Removes from the certificates table the selected
	 * <code>CertificateInfo</code> object. Removes this certificate from the
	 * Deployment package file, which certificates presents this table.
	 */
	private void handleRemove() {
		Object object = ((IStructuredSelection) certsTable.getSelection()).getFirstElement();
		if (object != null && object instanceof CertificateInfo) {
			CertificateInfo cert = (CertificateInfo) object;
			certificateInfoChange(cert, REMOVE_CERTIFICATE);
		}
		certificatesChanged();
		setDirty(true);
		commitChanges(false);
	}

	private void handleSignBundleChange() {
		DPPFile file = ((DPPFileModel) getFormPage().getModel()).getDPPFile();
		file.setSignBundles(signBundlesButton.getSelection());
		model.fireModelObjectChanged(file, "signBundles");
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
			if (first instanceof CertificateInfo) {
				if (isTableEditable) {
					removeButton.setEnabled(true);
				}
			}
		}
	}

	// IModelChangedListener implementation
	/**
	 * Called when there is a change in the model this model listener is
	 * registered with.
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.base.DPPFormSection#modelChanged(org.tigris.mtoolkit.dpeditor.editor.model.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			certsTable.refresh();
			return;
		}
		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof CertificateInfo) {
			CertificateInfo cert = (CertificateInfo) changeObject;
			if (event.getChangeType() == IModelChangedEvent.ADD) {
				certsTable.add(cert);
			}
			if (event.getChangeType() == IModelChangedEvent.INSERT) {
				certsTable.add(cert);
				certsTable.editElement(cert, 0);
			} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
				certsTable.remove(cert);
			}
		} else {
			certsTable.refresh();
		}
	}

	/**
	 * Sets the new and remove buttons to be enabled or disabled, depending on
	 * the given <code>boolean</code> flag.
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
	 * Refreshes the certificate viewer completely with information freshly
	 * obtained from the viewer's model.
	 */
	public void certificatesChanged() {
		if (certsTable != null) {
			certsTable.refresh();
		}
	}

	/**
	 * Adds, removes, moves up or down the given <code>CertificateInfo</code>,
	 * depending on the given key. Notifies all existing
	 * <code>IModelChangedListener</code>'s of a change of the model.
	 * 
	 * @param info
	 *            the <code>CertificateInfo</code>, at which will be execute the
	 *            action
	 * @param key
	 *            the type of the action. This type can be one of the followed
	 *            values: ADD_CERTIFICATE, REMOVE_CERTIFICATE, UP_CERTIFICATE
	 *            and DOWN_CERTIFICATE
	 */
	private void certificateInfoChange(CertificateInfo info, int key) {
		DPPFile dppFile = ((DPPFileModel) getFormPage().getModel()).getDPPFile();
		Vector infos = dppFile.getCertificateInfos();
		switch (key) {
		case ADD_CERTIFICATE:
			infos.addElement(info);
			model.fireModelChanged(new ModelChangedEvent(IModelChangedEvent.ADD, new Object[] { info }, null));
			break;
		case REMOVE_CERTIFICATE:
			infos.removeElement(info);
			model.fireModelChanged(new ModelChangedEvent(IModelChangedEvent.REMOVE, new Object[] { info }, null));
			break;
		case UP_CERTIFICATE:
			DPPUtilities.moveElement(infos, info, true);
			model.fireModelChanged(new ModelChangedEvent(IModelChangedEvent.INSERT, new Object[] { info }, null));
			break;
		case DOWN_CERTIFICATE:
			DPPUtilities.moveElement(infos, info, false);
			model.fireModelChanged(new ModelChangedEvent(IModelChangedEvent.INSERT, new Object[] { info }, null));
			break;
		}
	}

	/**
	 * Returns the relative of the project path of the given value.
	 * 
	 * @param value
	 *            the value, which will be relative for the project
	 * @return the relative of the project path value
	 */
	protected String getRelativePath(String value) {
		DPPFileModel model = ((DPPFileModel) getFormPage().getModel());
		IFile ifile = model.getFile();
		if (ifile.getProject().getLocation() != null) {
			String location = ifile.getProject().getLocation().toOSString();
			if (value.toLowerCase().startsWith(location.toLowerCase())) {
				value = "<.>" + value.substring(location.length());
			}
		}
		return value;
	}

	public void update() {
		certsTable.refresh();
	}
}