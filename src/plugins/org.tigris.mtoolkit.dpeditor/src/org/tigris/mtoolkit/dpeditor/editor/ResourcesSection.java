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

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
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
import org.tigris.mtoolkit.dpeditor.editor.base.CustomCellEditor;
import org.tigris.mtoolkit.dpeditor.editor.base.DPPFormSection;
import org.tigris.mtoolkit.dpeditor.editor.event.EventConstants;
import org.tigris.mtoolkit.dpeditor.editor.event.TableControlListener;
import org.tigris.mtoolkit.dpeditor.editor.forms.FormWidgetFactory;
import org.tigris.mtoolkit.dpeditor.editor.model.DPPFileModel;
import org.tigris.mtoolkit.dpeditor.editor.model.IModelChangedEvent;
import org.tigris.mtoolkit.dpeditor.editor.model.ModelChangedEvent;
import org.tigris.mtoolkit.dpeditor.util.DPPErrorHandler;
import org.tigris.mtoolkit.dpeditor.util.DPPUtil;
import org.tigris.mtoolkit.dpeditor.util.ResourceManager;
import org.tigris.mtoolkit.util.DPPFile;
import org.tigris.mtoolkit.util.DPPUtilities;
import org.tigris.mtoolkit.util.Header;
import org.tigris.mtoolkit.util.ResourceInfo;

/**
 * Creates the section that will be placed in the form, which represents the
 * visualization of the all <code>ResourceInfo</code>s in the Deployment package
 * file. This section gives possibility to add, remove, edit and move the
 * created <code>ResourceInfo</code>s.
 */
public class ResourcesSection extends DPPFormSection implements
		SelectionListener, ISelectionChangedListener {

	/** Holds the title of the section */
	public static final String SECTION_TITLE = "DPPEditor.ResourcesSection.title";
	/** Holds the description of the section */
	public static final String SECTION_DESC = "DPPEditor.ResourcesSection.desc";
	/** The New button's label */
	public static final String NEW_BUTTON = "DPPEditor.New_Button";
	/** The Remove button's label */
	public static final String REMOVE_BUTTON = "DPPEditor.Remove_Button";
	/** The Up button's label */
	public static final String UP_BUTTON = "DPPEditor.Up_Button";
	/** The Down button's label */
	public static final String DOWN_BUTTON = "DPPEditor.Down_Button";

	/** Equals resource path values error message */
	public static final String EQUAL_VALUES_MSG1 = "DPPEditor.ResourcesSection.EqualValuesMsg1";
	/** Message ask to enter different resource path to continue */
	public static final String EQUAL_VALUES_MSG2 = "DPPEditor.ResourcesSection.EqualValuesMsg2";
	/** Message announced of the incorrect value of the resource path */
	public static final String WRONG_RESOURCE_PATH = "DPPEditor.ResourcesSection.WrongPath";
	/** Message announced when Resource's name already exists in Name column */
	public static final String ERROR_RESOURCE_NAME_ALREADY_EXISTS = "DPPEditor.ResourcesSection.ResourceNameAlreadyExists";
	/** Message announced when file is not specified in the Resource Path colon */
	private static final String ERROR_INVALID_FILE_NAME = "DPPEditor.ResourcesSection.InvalidFileName";

	/** Constant that indicates the add resource action */
	private static final int ADD_RESOURCE = 0;
	/** Constant that indicates the remove resource action */
	private static final int REMOVE_RESOURCE = 1;
	/** Constant that indicates the move up resource action */
	private static final int UP_RESOURCE = 2;
	/** Constant that indicates the move down resource action */
	private static final int DOWN_RESOURCE = 3;

	/** The parent composite in which all components will be added */
	private Composite container;
	/**
	 * The TableViewer, which table contains all <code>ResourceInfo</code>s
	 * object for the given Deployment package file.
	 */
	private TableViewer resourcesTable;
	/** The button, which action adds new resource into table of all resources */
	private Button newButton;
	/** The button, which action removes selected in the table resource */
	private Button removeButton;
	/** The button, which action moves up the selected in the table resource */
	private Button upButton;
	/** The button, which action moves down the selected in the table resource */
	private Button downButton;

	/**
	 * A cell editor that presents a list of items in a combo box for the
	 * missing property of the <code>ResourceInfo</code>.
	 */
	private ComboBoxCellEditor cellEditor;
	/** A combo box for the missing property of the <code>ResourceInfo</code> */
	private CCombo comboEditor;
	/**
	 * A cell editor that presents a list of items in a combo box for the
	 * resource processor property of the <code>ResourceInfo</code>.
	 */
	private ComboBoxCellEditor processorsCellEditor;
	/**
	 * A combo box for the resource processor property of the
	 * <code>ResourceInfo</code>
	 */
	private CCombo processorsCombo;
	/**
	 * The String array, that holds all values of the resource processor of the
	 * <code>ResourceInfo</code>
	 */
	private String[] items;
	/**
	 * This table holds the correlation between the combo box values and their
	 * int value, which is needed to the label provider to shows the chosen
	 * value.
	 */
	private Hashtable comboValues;
	/**
	 * Items in dpeditor.resourceprcessors which are entered through
	 * PreferencesPage.
	 */
	private String[] itemsFromProperty;
	/** Items which user enters in the combo manually. */
	private String[] itemsEnteredByUser;
	/** The <code>boolean</code> flag that shows if the table is editable or not */
	private boolean isTableEditable = true;
	/**
	 * The deployment package file model, which <code>ResourceIndo</code>s this
	 * section presents.
	 */
	private DPPFileModel model;
	/** The parents form page */
	private ResourcesFormPage page;
	/** Path to resources in DP entered by user in Resources Section */
	private String customPath = "resources/";

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
		 * this class the properties for which this method works are: resource,
		 * name, resource_processor, missing.
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
			ResourceInfo resource = (ResourceInfo) item.getData();
			String newValue = value.toString();
			boolean isSet = false;

			String filename = null;

			if (property.equals("resource")) {
				if (newValue.equals(resource.getResourcePath())
						&& (!newValue.equals(""))) {
					return;
				}
				DPPFileModel model = ((DPPFileModel) getFormPage().getModel());
				IProject project = model.getFile().getProject();
				String location = project.getLocation().toOSString();
				if (newValue.toLowerCase().startsWith(
						location.toLowerCase() + File.separator)) {
					newValue = "<.>" + newValue.substring(location.length());
				}
				int itemExists = itemExists(resourcesTable, item, newValue);
				if (itemExists != -1
						&& (!newValue.equals("") || newValue.equals(""))) {
					showErrorTableDialog(ResourceManager
							.getString(EQUAL_VALUES_MSG1)
							+ "\n"
							+ ResourceManager.getString(EQUAL_VALUES_MSG2));
					return;
				}
				if (newValue == null || newValue.equals(""))
					return;
				if ((newValue.charAt(newValue.length() - 1) == '\\')
						|| (newValue.charAt(newValue.length() - 1) == '/')) {
					DPPErrorHandler.showErrorTableDialog(ResourceManager
							.getString(ERROR_INVALID_FILE_NAME));
					return;
				}
				filename = getName(newValue);
				if (item.getText(1).equals("")) {
					customPath = getUpperPath(object);
				} else {
					String tempSTR = getPath(item.getText(1));
					customPath = (tempSTR == null) ? "" : tempSTR;
				}
				if (DPPUtil.isAlreadyInTheTable(customPath + filename, item)) {
					DPPErrorHandler.showErrorTableDialog(ResourceManager
							.getString(ERROR_RESOURCE_NAME_ALREADY_EXISTS));
					return;
				}
				resource.setResourcePath(newValue);
				resource.setName(customPath + filename);
				isSet = true;
			} else if (property.equals("name")) {
				if (newValue.equals("")) { // delete or New
					TableItem currentItem = (TableItem) object;
					String resourceFileSystemPath = currentItem.getText(0);
					if (resourceFileSystemPath.equals("")) {
						return;
					}
					filename = getName(currentItem.getText(0));
					customPath = getUpperPath(object);
				} else { // modify
					filename = getName(newValue);
					if (filename.equals("")) {
						filename = getName(item.getText(0));
					}
					String currentPath = getPath(newValue);
					customPath = (currentPath == null) ? "" : currentPath;
				}
				if (DPPUtil.isAlreadyInTheTable(customPath + filename, item)) {
					DPPErrorHandler.showErrorTableDialog(ResourceManager
							.getString(ERROR_RESOURCE_NAME_ALREADY_EXISTS));
					return;
				}
				String newName = customPath + filename;
				if (newName.equals(resource.getName()))
					return;
				resource.setName(newName);
				isSet = true;
			} else if (property.equals("missing")) {
				Integer newInteger = new Integer(newValue);
				int val = newInteger.intValue();
				if ((val != 0 && val != 1) || val == 1) {
					newValue = "false";
				} else if (val == 0) {
					newValue = "true";
				}
				if (newValue.equals("" + resource.isMissing())
						&& (!newValue.equals(""))) {
					return;
				}
				isSet = true;
				resource.setMissing(new Boolean(newValue).booleanValue());
			} else if (property.equals("resource_processor")) {
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
					newValue = processorsCombo.getText();

				}
				if (newValue.equals("")
						|| newValue.equals(resource.getResourceProcessor())) {
					return;
				}
				boolean isAlreadyInTheCombo = false;
				for (int i = 0; i < items.length; i++) {
					if (items[i].equals(newValue)) {
						isAlreadyInTheCombo = true;
						break;
					}
				}

				if (!isAlreadyInTheCombo) {
					addNewProcessor(newValue);
				}
				isSet = true;
				resource.setResourceProcessor(newValue);

			} else if (property.equals("custom")) {
				if (newValue.equals(resource.otherHeadersToString()) /*
																	 * &&
																	 * (!newValue
																	 * .equals
																	 * (""))
																	 */) {
					return;
				}
				resource.setOtherHeaders(newValue);
				isSet = true;
			}
			removeButton.setEnabled(true);
			setMoveEnable();
			setDirty(true);
			commitChanges(false);
			resourcesTable.update(resource, null);
			page.updateDocumentIfSource();
			if (isSet) {
				model.fireModelChanged(new ModelChangedEvent(
						IModelChangedEvent.EDIT, new Object[] { resource },
						null));
			}
		}

		private void addNewProcessor(String newValue) {

			if (itemsEnteredByUser != null) {
				String temp[] = new String[itemsEnteredByUser.length + 1];
				for (int i = 0; i < itemsEnteredByUser.length; i++) {
					temp[i] = itemsEnteredByUser[i];
				}
				temp[temp.length - 1] = newValue;
				itemsEnteredByUser = temp;
			} else {
				itemsEnteredByUser = new String[] { newValue };
			}
			refreshItems();
		}

		private String getUpperPath(Object object) {
			TableItem currentItem = (TableItem) object;
			Table table = resourcesTable.getTable();
			int size = table.getItems().length;
			if (size == 0)
				return "resources/";
			for (int i = 0; i < size; i++) {
				if (currentItem == table.getItem(i)) {
					if (i == 0)
						return "resources/";
					TableItem upper = table.getItem(i - 1);
					String upperPath = getPath(upper.getText(1));
					return (upperPath == null) ? "resources/" : upperPath;
				}
			}
			return "resources/";
		}

		/**
		 * Returns the value for the given property of the given element.
		 * Returns empty string if the element does not have the given property.
		 * The values of the property that are allowed are: resource, name,
		 * resource_processor, missing.
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
			ResourceInfo resource = (ResourceInfo) object;
			if (property.equals("resource")) {
				String resPath = DPPUtilities.getStringValue(resource
						.getResourcePath());
				DPPFileModel model = ((DPPFileModel) getFormPage().getModel());
				IFile ifile = model.getFile();
				IProject project = ifile.getProject();
				String location = project.getLocation().toOSString()
						+ File.separator;
				if (resPath.toLowerCase().startsWith(location.toLowerCase())) {
					resPath = "<.>" + resPath.substring(location.length());
				}
				return resPath;
			} else if (property.equals("name")) {
				return DPPUtilities.getStringValue(resource.getName());
			} else if (property.equals("missing")) {
				if (resource.isMissing()) {
					return new Integer(0);
				}
				return new Integer(1);
			} else if (property.equals("resource_processor")) {
				String resProperty = System
						.getProperty("dpeditor.resourceprcessors");
				comboValues = new Hashtable();
				if (items == null || itemsEnteredByUser == null) {
					// loading file
					items = getItemsFromProperty(resProperty);
					DPPFile dppFile = model.getDPPFile();
					Vector v = dppFile.getResourceInfos();
					int RPcount = v.size();

					itemsEnteredByUser = new String[RPcount];
					for (int i = 0; i < RPcount; i++) {
						ResourceInfo ri = (ResourceInfo) v.elementAt(i);
						String RP = ri.getResourceProcessor();
						if (RP != null) {
							itemsEnteredByUser[i] = RP;
						} else {
							itemsEnteredByUser[i] = "";
						}
					}
					refreshItems();
				} else {
					refreshItems();
				}
				String key = DPPUtilities.getStringValue(resource
						.getResourceProcessor());
				String value = (String) comboValues.get(key);
				processorsCellEditor.setItems(items);
				for (int i = 0; i < items.length; i++) {
					if (items[i].equals(key)) {
						value = "" + i;
						break;
					}
				}
				if (value == null) {
					if (key.equals("")) {
						value = "0";
					} else {
						int size = items.length;
						if (size == 0) {
							size = 1;
							items = new String[] { "" };
						}
						processorsCellEditor.setItems(items);
						value = "" + size;
					}
				}
				return new Integer(value);
			} else if (property.equals("custom")) {
				return resource.otherHeadersToString();
			}
			return "";
		}

		private void refreshItems() {
			String rpProp = System.getProperty("dpeditor.resourceprcessors");
			itemsFromProperty = getItemsFromProperty(rpProp);
			Vector itemsV = new Vector();
			for (int i = 0; i < itemsFromProperty.length; i++) {
				itemsV.addElement(itemsFromProperty[i]);
			}

			if (itemsEnteredByUser != null) {
				for (int i = 0; i < itemsEnteredByUser.length; i++) {
					if (itemsV.indexOf(itemsEnteredByUser[i]) == -1) {
						itemsV.addElement(itemsEnteredByUser[i]);
					}
				}
			}
			items = (String[]) itemsV.toArray(new String[0]);
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
				Vector infos = ((DPPFile) parent).getResourceInfos();
				if (infos == null) {
					return null;
				}
				ResourceInfo[] result = new ResourceInfo[infos.size()];
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
			if (obj instanceof ResourceInfo) {
				ResourceInfo resource = (ResourceInfo) obj;
				if (index == 0) {
					String resPath = DPPUtilities.getStringValue(resource
							.getResourcePath());
					DPPFileModel model = ((DPPFileModel) getFormPage()
							.getModel());
					IFile ifile = model.getFile();
					IProject project = ifile.getProject();
					String location = project.getLocation().toOSString();
					if (resPath.toLowerCase().startsWith(
							location.toLowerCase() + File.separator)) {
						resPath = "<.>" + resPath.substring(location.length());
					}
					return resPath;
				} else if (index == 1) {
					return DPPUtilities.getStringValue(resource.getName());
				} else if (index == 2) {
					return DPPUtilities.getStringValue(resource
							.getResourceProcessor());
				} else if (index == 3) {
					return new Boolean(resource.isMissing()).toString();
				} else if (index == 4) {
					return resource.otherHeadersToString();
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
	public ResourcesSection(ResourcesFormPage page) {
		super(page);
		this.page = page;
		setHeaderText(ResourceManager.getString(SECTION_TITLE, ""));
		setDescription(ResourceManager.getString(SECTION_DESC, ""));
	}

	/**
	 * This method is called from the <code>createControl</code> method and puts
	 * all custom components in this resources form section.
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
						final ResourceInfo resource = (ResourceInfo) item
								.getData();
						if (DPPUtilities.getStringValue(
								resource.getResourcePath()).equals("")) {
							resourceInfoChange(resource, REMOVE_RESOURCE);
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
				ResourceManager.getString("DPPEditor.ResourcesSection.ColPath"),
				ResourceManager.getString("DPPEditor.ResourcesSection.ColName"),
				ResourceManager
						.getString("DPPEditor.ResourcesSection.ColResProcessor"),
				ResourceManager
						.getString("DPPEditor.ResourcesSection.ColMissing"),
				ResourceManager
						.getString("DPPEditor.ResourcesSection.ColCustomHeaders") };
		for (int i = 0; i < columnTitles.length; i++) {
			TableColumn tableColumn = new TableColumn(table, SWT.NULL);
			tableColumn.setText(columnTitles[i]);
		}

		TableControlListener controlListener = new TableControlListener(table);
		controlListener.setResizeMode(EventConstants.RESOURCES_RESIZE_MODE);
		container.addControlListener(controlListener);

		resourcesTable = new TableViewer(table);
		resourcesTable.setContentProvider(new TableContentProvider());
		resourcesTable.setLabelProvider(new TableLabelProvider());
		resourcesTable.addSelectionChangedListener(this);

		String[] sData = { "true", "false" }; //$NON-NLS-1$ //$NON-NLS-2$
		cellEditor = new ComboBoxCellEditor(table, sData, SWT.READ_ONLY);
		String property = System.getProperty("dpeditor.resourceprcessors");
		comboValues = new Hashtable();
		items = getItemsFromProperty(property);
		processorsCellEditor = new ComboBoxCellEditor(table, items, SWT.NULL);
		CellEditor[] editors = new CellEditor[] {
				new CustomCellEditor(container, resourcesTable, table,
						CustomCellEditor.TEXT_BUTTON_TYPE,
						CustomCellEditor.RESOURCE_PATH),
				new TextCellEditor(table),
				processorsCellEditor,
				cellEditor,
				new CustomCellEditor(container, resourcesTable, table,
						CustomCellEditor.DIALOG_TYPE,
						CustomCellEditor.RESOURCE_HEADER) };
		String[] properties = { "resource", "name", "resource_processor",
				"missing", "custom" };
		resourcesTable.setCellEditors(editors);
		resourcesTable.setCellModifier(new KeyModifier());
		resourcesTable.setColumnProperties(properties);

		comboEditor = (CCombo) cellEditor.getControl();
		comboEditor.setEditable(false);
		comboEditor.setSize(5, 5);
		comboEditor.addSelectionListener(this);

		processorsCombo = (CCombo) processorsCellEditor.getControl();
		processorsCombo.setEditable(true);
		processorsCombo.setSize(5, 5);
		processorsCombo.addSelectionListener(this);

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
		upButton = FormWidgetFactory.createButton(buttonComposite,
				ResourceManager.getString(UP_BUTTON, ""), SWT.PUSH);
		downButton = FormWidgetFactory.createButton(buttonComposite,
				ResourceManager.getString(DOWN_BUTTON, ""), SWT.PUSH);

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
			resourcesTable.setInput(model.getDPPFile());
			removeButton.setEnabled(false);
			upButton.setEnabled(false);
			downButton.setEnabled(false);
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
	 * Sets the focus to the table of the resources and refresh the viewer.
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.FormSection#setFocus()
	 */
	public void setFocus() {
		resourcesTable.getTable().setFocus();
		resourcesTable.refresh();
		TableItem[] items = resourcesTable.getTable().getItems();
		for (int i = 0; i < items.length; i++) {
			ResourceInfo info = (ResourceInfo) items[i].getData();
			String path = DPPUtilities.getStringValue(info.getResourcePath());
			String name = DPPUtilities.getStringValue(info.getName());
			if (path.equals("")) {
				resourcesTable.editElement(info, 0);
				break;
			} else if (name.equals("")) {
				resourcesTable.editElement(info, 1);
				break;
			}
		}
	}

	/**
	 * Sets remove, move up and move down button enable or disable, depending on
	 * the selection in the table.
	 */
	private void updateEnabledButtons() {
		Table table = resourcesTable.getTable();
		TableItem[] selection = table.getSelection();
		boolean hasSelection = selection.length > 0;
		removeButton.setEnabled(hasSelection);
		setMoveEnable();
	}

	/**
	 * Returns the String array representation of the given property values.
	 * 
	 * @param property
	 *            the property value
	 * @return the String array, which presents the all items of the given
	 *         property
	 */
	private String[] getItemsFromProperty(String property) {
		String[] result = new String[0];
		if (property != null) {
			StringTokenizer tokenizer = new StringTokenizer(property, ";");
			Vector elements = new Vector();
			int i = 0;
			comboValues.put("", "" + i);
			while (tokenizer.hasMoreTokens()) {
				String next = tokenizer.nextToken().trim();
				if (!next.equals("")) {
					elements.addElement(next);
					i += 1;
					comboValues.put(next, "" + i);
				}
			}
			if (elements.size() != 0) {
				elements.insertElementAt("", 0);
			}
			result = new String[elements.size()];
			elements.copyInto(result);
		}
		return result;
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
		} else if (obj instanceof CCombo) {
			CCombo combo = (CCombo) obj;
			if (combo == comboEditor) {
				int index = combo.getSelectionIndex();
				String item = combo.getItem(index);
				Object object = ((IStructuredSelection) resourcesTable
						.getSelection()).getFirstElement();
				if (object != null && object instanceof ResourceInfo) {
					ResourceInfo resourceInfo = (ResourceInfo) object;
					boolean oldMissing = resourceInfo.isMissing();
					boolean newMissing = item.equals("true");
					if ((oldMissing && !newMissing)
							|| (!oldMissing && newMissing)) {
						resourceInfo.setMissing(newMissing);
						model.fireModelChanged(new ModelChangedEvent(
								IModelChangedEvent.EDIT,
								new Object[] { resourceInfo }, null));
					}
					resourcesChanged();
				}
			} else if (combo == processorsCombo) {
				int index = combo.getSelectionIndex();
				String item = combo.getItem(index);
				Object object = ((IStructuredSelection) resourcesTable
						.getSelection()).getFirstElement();
				if (object != null && object instanceof ResourceInfo) {
					ResourceInfo resourceInfo = (ResourceInfo) object;
					String oldProcessor = resourceInfo.getResourceProcessor();
					if (oldProcessor == null || !oldProcessor.equals(item)) {
						resourceInfo.setResourceProcessor(item);
						model.fireModelChanged(new ModelChangedEvent(
								IModelChangedEvent.EDIT,
								new Object[] { resourceInfo }, null));
					}
					resourcesChanged();
				}
			}
		}
	}

	/**
	 * Creates the new <code>ResourceInfo</code> object, which is presented in
	 * the table as a new table row. Adds this resource into the Deployment
	 * package file, which resources presents this table.
	 */
	private void handleNew() {
		Table table = resourcesTable.getTable();
		int size = table.getItems().length;

		if (size != 0) {
			TableItem beforeLastTableItem = table.getItem(size - 1);
			String colonNameValue = beforeLastTableItem.getText(1);
			String colonNamePath = getPath(colonNameValue);
			if (colonNamePath == null) {
				customPath = "resources/";
			} else {
				customPath = colonNamePath;
			}
		} else {
			customPath = "resources/";
		}

		ResourceInfo resource = new ResourceInfo();
		boolean found = false;

		for (int i = 0; i < size; i++) {
			TableItem currentItem = table.getItem(i);
			if (currentItem.getText(0).equalsIgnoreCase("")
					&& !currentItem.getData().equals(resource)) {
				found = true;
				break;
			}
		}

		if (!found) {
			resourceInfoChange(resource, ADD_RESOURCE);
			resourcesTable.add(resource);
			resourcesTable.editElement(resource, 0);
			setDirty(true);
			commitChanges(false);
			size++;
		}

		table.setSelection(size - 1);
		table.setFocus();
		updateEnabledButtons();
	}

	private String getPath(String str) {

		if (str == null)
			return null;
		if (str.equals(""))
			return "";
		// remove separators and white spaces in the begining
		int i = 0;
		while (str.charAt(i) == '\\' || str.charAt(i) == '/'
				|| str.charAt(i) == ' ') {

			++i;
			if (i == (str.length()))
				break;
		}
		if (i < str.length()) {
			str = str.substring(i);
		} else {
			str = "";
		}

		int indexofSlash = str.lastIndexOf("/");
		int indexofbackSlash = str.lastIndexOf("\\");
		if ((indexofSlash == -1) && (indexofbackSlash == -1)) {
			return null;
		} else {

			if (indexofbackSlash > indexofSlash) {
				return str.substring(0, indexofbackSlash + 1);
			} else {
				return str.substring(0, indexofSlash + 1);
			}

		}

	}

	private String getName(String str) {

		if (str == null)
			return null;
		if (str.equals(""))
			return "";
		int i = 0;
		while (str.charAt(i) == '\\' || str.charAt(i) == '/'
				|| str.charAt(i) == ' ') {
			++i;
			if (i == (str.length()))
				break;
		}
		if (i < str.length()) {
			str = str.substring(i);
		} else {
			str = "";
		}
		int indexofSlash = str.lastIndexOf("/");
		int indexofbackSlash = str.lastIndexOf("\\");
		if ((indexofSlash == -1) && (indexofbackSlash == -1)) {
			return str;
		} else {

			if (indexofbackSlash > indexofSlash) {
				return str.substring(indexofbackSlash + 1);
			} else {
				return str.substring(indexofSlash + 1);
			}

		}

	}

	/**
	 * Removes from the resources table the selected <code>ResourceInfo</code>
	 * object. Removes this resource from the Deployment package file, which
	 * resources presents this table.
	 */
	private void handleRemove() {
		Object object = ((IStructuredSelection) resourcesTable.getSelection())
				.getFirstElement();
		if (object != null && object instanceof ResourceInfo) {
			ResourceInfo resource = (ResourceInfo) object;
			resourceInfoChange(resource, REMOVE_RESOURCE);
			resourcesTable.remove(resource);
		}
		setDirty(true);
		commitChanges(false);
		setMoveEnable();
	}

	/**
	 * Moves up the selected resource and moves up the resource in the
	 * Deployment package file, which resources presents this table.
	 */
	private void handleUp() {
		Object object = ((IStructuredSelection) resourcesTable.getSelection())
				.getFirstElement();
		if (object != null && object instanceof ResourceInfo) {
			ResourceInfo resource = (ResourceInfo) object;
			resourceInfoChange(resource, UP_RESOURCE);
		}
		resourcesChanged();
		setDirty(true);
		commitChanges(false);
		setMoveEnable();
	}

	/**
	 * Moves down the selected resource and moves down the resource in the
	 * Deployment package file, which resources presents this table.
	 */
	private void handleDown() {
		Object object = ((IStructuredSelection) resourcesTable.getSelection())
				.getFirstElement();
		if (object != null && object instanceof ResourceInfo) {
			ResourceInfo resource = (ResourceInfo) object;
			resourceInfoChange(resource, DOWN_RESOURCE);
		}
		resourcesChanged();
		setDirty(true);
		commitChanges(false);
		setMoveEnable();
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
			if (first instanceof ResourceInfo) {
				setMoveEnable();
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
			resourcesTable.refresh();
			return;
		}
		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof ResourceInfo) {
			ResourceInfo resource = (ResourceInfo) changeObject;
			if (event.getChangeType() == IModelChangedEvent.ADD) {
				resourcesTable.add(resource);
			}
			if (event.getChangeType() == IModelChangedEvent.INSERT) {
				resourcesTable.add(resource);
				resourcesTable.editElement(resource, 0);
			} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
				resourcesTable.remove(resource);
			}
		} else {
			resourcesTable.refresh();
		}
	}

	/**
	 * Sets up and down button to be enable or disable, depending on the
	 * selection in the table.
	 */
	public void setMoveEnable() {
		Table table = resourcesTable.getTable();
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
		upButton.setEnabled(editable);
		downButton.setEnabled(editable);
	}

	/**
	 * Refreshes the resource viewer completely with information freshly
	 * obtained from the viewer's model.
	 */
	public void resourcesChanged() {
		if (resourcesTable != null) {
			resourcesTable.refresh();
		}
	}

	/**
	 * Adds, removes, moves up or down the given <code>ResourceInfo</code>,
	 * depending on the given key. Notifies all existing
	 * <code>IModelChangedListener</code>'s of a change of the model.
	 * 
	 * @param info
	 *            the <code>ResourceInfo</code>, on which will be done the
	 *            action
	 * @param key
	 *            the type of the action. This type can be one of the followed
	 *            values: ADD_RESOURCE, REMOVE_RESOURCE, UP_RESOURCE and
	 *            DOWN_RESOURCE
	 */
	private void resourceInfoChange(ResourceInfo info, int key) {
		DPPFileModel model = ((DPPFileModel) getFormPage().getModel());
		Vector infos = (model.getDPPFile()).getResourceInfos();
		switch (key) {
		case ADD_RESOURCE:
			infos.addElement(info);
			model.fireModelChanged(new ModelChangedEvent(
					IModelChangedEvent.ADD, new Object[] { info }, null));
			break;
		case REMOVE_RESOURCE:
			infos.removeElement(info);
			model.fireModelChanged(new ModelChangedEvent(
					IModelChangedEvent.REMOVE, new Object[] { info }, null));
			break;
		case UP_RESOURCE:
			DPPUtilities.moveElement(infos, info, true);
			model.fireModelChanged(new ModelChangedEvent(
					IModelChangedEvent.INSERT, new Object[] { info }, null));
			break;
		case DOWN_RESOURCE:
			DPPUtilities.moveElement(infos, info, false);
			model.fireModelChanged(new ModelChangedEvent(
					IModelChangedEvent.INSERT, new Object[] { info }, null));
			break;
		}
	}

	public void update() {
		resourcesTable.refresh();
	}
}