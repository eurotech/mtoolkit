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
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
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
import org.tigris.mtoolkit.common.PluginUtilities;
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
import org.tigris.mtoolkit.util.BundleInfo;
import org.tigris.mtoolkit.util.DPPFile;
import org.tigris.mtoolkit.util.DPPUtilities;

/**
 * Creates the section that will be placed in the form, which represents the
 * visualization of the all <code>BundleInfo</code>s in the Deployment package
 * file. This section gives possibility to add, remove, edit and move the
 * created <code>BundleInfo</code>s.
 */
public class BundlesSection extends DPPFormSection implements SelectionListener, ISelectionChangedListener,
		ICheckStateListener {

	/** Holds the title of this section */
	public static final String SECTION_TITLE = "DPPEditor.BundlesSection.title";
	/** Holds the description of the section */
	public static final String SECTION_DESC = "DPPEditor.BundlesSection.desc";
	/** The New button's label */
	public static final String NEW_BUTTON = "DPPEditor.New_Button";
	/** The Remove button's label */
	public static final String REMOVE_BUTTON = "DPPEditor.Remove_Button";
	/** The Up button's label */
	public static final String UP_BUTTON = "DPPEditor.Up_Button";
	/** The Down button's label */
	public static final String DOWN_BUTTON = "DPPEditor.Down_Button";

	/** Equals bundle path values error message */
	public static final String EQUAL_VALUES_MSG1 = "DPPEditor.BundlesSection.EqualValuesMsg1";
	/** Message ask to enter different bundle path to continue */
	public static final String EQUAL_VALUES_MSG2 = "DPPEditor.BundlesSection.EqualValuesMsg2";
	/** Message announced of the incorrect value of the bundle version */
	public static final String WRONG_BUNDLE_VERSION = "DPPEditor.BundlesSection.WrongVersion";
	/** Message announced of the incorrect value of the bundle path */
	public static final String WRONG_BUNDLE_PATH = "DPPEditor.BundlesSection.WrongPath";
	/** Message announced for invalid bundle name */
	public static final String ERROR_INVALID_BUNDLE_NAME = "DPPEditor.BundlesSection.InvalidBundleName";
	/** Message announced when bundle name do not end with ".jar". */
	public static final String ERROR_BUNDLE_NAME_NOT_ENDS_WITH_JAR = "DPPEditor.BundlesSection.BundleNameNotEndsWithJar";
	/** Message announced when bundle name already exists in the table. */
	public static final String ERROR_BUNDLE_NAME_ALREADY_EXISTS = "DPPEditor.BundlesSection.BundleNameAlreadyExists";
	/** Constant that indicates the add bundle action */
	private static final int ADD_BUNDLE = 0;
	/** Constant that indicates the remove bundle action */
	private static final int REMOVE_BUNDLE = 1;
	/** Constant that indicates the move up bundle action */
	private static final int UP_BUNDLE = 2;
	/** Constant that indicates the move down bundle action */
	private static final int DOWN_BUNDLE = 3;

	/** The parent composite in which all components will be added */
	private Composite container;
	/**
	 * The TableViewer, which table contains all <code>BundleInfo</code>s object
	 * for the given Deployment package file.
	 */
	private TableViewer bundlesTable;
	/** The button, which action adds new bundle into table of all bundles */
	private Button newButton;
	/** The button, which action removes selected in the table bundle */
	private Button removeButton;
	/** The button, which action moves up the selected in the table bundle */
	private Button upButton;
	/** The button, which action moves down the selected in the table bundle */
	private Button downButton;

	/** The <code>boolean</code> flag that shows if the table is editable or not */
	private boolean isTableEditable = true;
	/**
	 * The deployment package file model, which <code>BundleIndo</code>s this
	 * section presents.
	 */
	private DPPFileModel model;
	/** The parents form page */
	private BundlesFormPage page;

	/**
	 * A cell editor that presents a list of items in a combo box for the
	 * customizer property of the <code>BundleInfo</code>.
	 */
	private ComboBoxCellEditor customizerCellEditor;
	/** A combo box for the customizer property of the <code>BundleInfo</code> */
	private CCombo customizerCombo;
	/**
	 * A cell editor that presents a list of items in a combo box for the
	 * missing property of the <code>BundleInfo</code>.
	 */
	private ComboBoxCellEditor missingCellEditor;
	/** A combo box for the missing property of the <code>BundleInfo</code> */
	private CCombo missingCombo;
	/** Path to bundles entered by user in Bundles Section */
	private String bundlesCustomPath = "bundles/";

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
			if (property.equals("version") || property.equals("bundle_name")) {
				BundleInfo info = null;
				if (object instanceof BundleInfo) {
					info = (BundleInfo) object;
				} else if (object instanceof TableItem) {
					TableItem item = (TableItem) object;
					info = (BundleInfo) item.getData();
				}
				String bundlePath = info.getBundlePath();
				if (bundlePath == null || bundlePath.endsWith(".project")) {
					return false;
				}
				if (property.equals("version")) {
					return !info.isVersionSetFromJar();
				} else {
					return !info.isSymbolicNameSetFromJar();
				}
			}
			return isTableEditable;
		}

		/**
		 * Modifies the value for the given property of the given element. In
		 * this class the properties for which this method works are: bundle,
		 * name, bundle_name, version, customizer, missing, custom.
		 * 
		 * @param object
		 *            the model element
		 * @param property
		 *            the property
		 * @param value
		 *            the new property value
		 * @return
		 * 
		 * @see org.eclipse.jface.viewers.ICellModifier#modify(java.lang.Object,
		 *      java.lang.String, java.lang.Object)
		 */

		private boolean modifyBundleColumn(String newValue, TableItem item) {
			if (newValue.equals("")) {
				return true;
			}

			if (!newValue.endsWith(".jar") && !newValue.endsWith(".project")) {
				DPPErrorHandler.showErrorTableDialog(ResourceManager.getString(ERROR_INVALID_BUNDLE_NAME));
				bundlesTable.getTable().setFocus();
				return false;
			}

			BundleInfo bundle = (BundleInfo) item.getData();
			String bundlePath = bundle.getBundlePath();
			String loc = ((DPPFileModel) getFormPage().getModel()).getFile().getProject().getLocation().toOSString();

			// The verifications about "<.>" are made only because of a
			// "feature" in the class TableLabelProvier
			// and should be reconsidered are they worth it at all!
			if (newValue.startsWith("<.>")) {
				newValue = loc + newValue.substring("<.>".length());
			}

			if ((itemExists(bundlesTable, item, newValue) != -1)) {
				showErrorTableDialog(ResourceManager.getString(EQUAL_VALUES_MSG1));
				bundlesTable.getTable().setFocus();
				return false;
			}

			if (!PluginUtilities.isValidPath(newValue)) {
				showErrorTableDialog(ResourceManager.getString(WRONG_BUNDLE_PATH));
				bundlesTable.getTable().setFocus();
				return false;
			}

			if (bundlePath != null && bundlePath.startsWith("<.>")) {
				bundlePath = loc + bundlePath.substring("<.>".length());
			}

			if (newValue.equals(bundlePath)) {
				return false;
			}

			IProject selProject = null;
			if (newValue.endsWith(".project")) {
				IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();

				for (int j = 0; j < projects.length; j++) {
					if (newValue.startsWith(projects[j].getLocation().toOSString() + File.separator)) {
						selProject = projects[j];
						break;
					}
				}

				if (selProject == null) {
					DPPErrorHandler.processError(
							ResourceManager.format("DPPEditor.ProjectError", new String[] { newValue }), true);
					bundlesTable.getTable().setFocus();
					return false;
				} else if (!DPPUtil.isPluginProject(selProject)) {
					DPPErrorHandler.processError(ResourceManager.getString("DPPEditor.BundlesSection.WrongProject"),
							true);
					bundlesTable.getTable().setFocus();
					return false;
				}
			}

			bundle.setBundlePath(newValue);
			// Currently the formation of the new Name is not 100% correct
			// logically, but this issue should be left future improvements,
			// according to I. Karabashev
			String tempSTR = DPPUtilities.getPath(item.getText(1));
			bundlesCustomPath = item.getText(1).equals("") ? getUpperPath(item) : (tempSTR == null) ? "" : tempSTR;
			String bundleName = bundlesCustomPath + getName(newValue);
			bundle.setName(bundleName);
			
			IPluginModelBase findModel = PluginRegistry.findModel(selProject);
			String symbName = null;

			if (findModel != null) {
				BundleDescription bundleDescr = findModel.getBundleDescription();

				if (bundleDescr == null) {
					try {
						IBundleModel bundleModel = (IBundleModel) ((IBundlePluginModelBase) findModel).getBundleModel();

						if (bundleModel != null) {
							symbName = DPPUtil.parseSymbolicName(bundleModel.getBundle()
									.getManifestHeader("Bundle-SymbolicName").getValue());
							bundle.setBundleSymbolicName(symbName);
							bundle.setBundleVersion(bundleModel.getBundle().getManifestHeader("Bundle-Version")
									.getValue());
						}
					} catch (Throwable t) {
						t.printStackTrace();
					}
				} else {
					symbName = DPPUtil.parseSymbolicName(bundleDescr.getSymbolicName());
					bundle.setBundleSymbolicName(symbName);
					bundle.setBundleVersion(bundleDescr.getVersion().toString());
				}
			}

			String msg = null;
			boolean versionAndSymbNameFlag = isExistingItemWithEqualSymbNameAndVersion(item,
					bundle.getBundleSymbolicName(), bundle.getBundleVersion());
			
			if (DPPUtil.isAlreadyInTheTable(bundleName, item)) {
				if (versionAndSymbNameFlag) {
					msg = ResourceManager
							.getString("DPPEditor.BundlesSection.VersonAndNameAndSymbolicNameAlreadyExists");
				} else {
					msg = ResourceManager.getString(ERROR_BUNDLE_NAME_ALREADY_EXISTS);
				}
			} else if (versionAndSymbNameFlag) {
				msg = ResourceManager.getString("DPPEditor.BundlesSection.VersionAndSymbolicNameAlreadyExists");
			}

			// table should be updated before the modal dialog is shown!!!!
			// DELAY!!!!
			if (msg != null) {
				DPPErrorHandler.processError(msg, true);
				bundlesTable.getTable().setFocus();
				return false;
			}

			return true;
		}
			
		private boolean isExistingItemWithEqualSymbNameAndVersion(TableItem item, String symbolicName, String version) {
			Table table = item.getParent();

			for (int i = 0; i < table.getItems().length; i++) {
				TableItem curItem = table.getItem(i);

				if (symbolicName.equals(curItem.getText(2)) && version.equals(curItem.getText(3)) && curItem != item) {
					return true;
				}
			}
			return false;
		}

		private boolean modifyNameColumn(String newValue, TableItem item) {
			BundleInfo bundle = (BundleInfo) item.getData();

			if (newValue.equals("") || newValue.equals(bundle.getName())) {
				return true;
			}

			if (!PluginUtilities.isValidPath(newValue) || newValue.indexOf(":")!=-1) {
				DPPErrorHandler.showErrorTableDialog(ResourceManager
						.getString("DPPEditor.BundlesSection.InvalidBundleName"));
				bundlesTable.getTable().setFocus();
				return false;
			}

			if (!newValue.endsWith(".jar")) {
				DPPErrorHandler.showErrorTableDialog(ResourceManager.getString(ERROR_BUNDLE_NAME_NOT_ENDS_WITH_JAR));
				bundlesTable.getTable().setFocus();
				return false;
			}
			bundle.setName(newValue);

			if (DPPUtil.isAlreadyInTheTable(newValue, item)) {
				DPPErrorHandler.showErrorTableDialog(ResourceManager.getString(ERROR_BUNDLE_NAME_ALREADY_EXISTS));
				bundlesTable.getTable().setFocus();
				return false;
			}
			return true;
		}
		
		public void modify(Object object, String property, Object value) {
			TableItem item = (TableItem) object;
			if (item == null) {
				return;
			}

			BundleInfo bundle = (BundleInfo) item.getData();
			String newValue = value.toString().trim();
			boolean isSet = false;

			if (property.equals("bundle")) {
				isSet = modifyBundleColumn(newValue, item);
			} else if (property.equals("name")) {
				isSet = modifyNameColumn(newValue, item);
			} else if (property.equals("version")) {
				if (newValue.equals(bundle.getBundleVersion()) && (!newValue.equals(""))) {
					return;
				}
				try {
					Version.parseVersion(newValue);
				} catch (IllegalArgumentException ex) {
					showErrorTableDialog(ResourceManager.getString(WRONG_BUNDLE_VERSION));
					return;
				}
				isSet = true;
				bundle.setBundleVersion(newValue);
			} else if (property.equals("customizer")) {
				Integer newInteger = new Integer(newValue);
				int val = newInteger.intValue();
				if ((val != 0 && val != 1) || val == 1) {
					newValue = "false";
				} else if (val == 0) {
					newValue = "true";
				}
				if (newValue.equals("" + bundle.isCustomizer()) && (!newValue.equals(""))) {
					return;
				}
				String lower = newValue.toLowerCase();
				if (!lower.equals("true") && !lower.equals("false")) {
					newValue = "false";
				}
				isSet = true;
				bundle.setCustomizer(new Boolean(newValue).booleanValue());
			} else if (property.equals("missing")) {
				Integer newInteger = new Integer(newValue);
				int val = newInteger.intValue();
				if ((val != 0 && val != 1) || val == 1) {
					newValue = "false";
				} else if (val == 0) {
					newValue = "true";
				}
				if (newValue.equals("" + bundle.isMissing()) && (!newValue.equals(""))) {
					return;
				}
				isSet = true;
				bundle.setMissing(new Boolean(newValue).booleanValue());
			} else if (property.equals("custom")) {
				if (newValue.equals(bundle.otherHeadersToString())) {
					return;
				}
				bundle.setOtherHeaders(newValue);
				isSet = true;
			} else if (property.equals("bundle_name")) {
				if (newValue.equals(bundle.getBundleSymbolicName()) && (!newValue.equals(""))) {
					return;
				}
				isSet = true;
				bundle.setBundleSymbolicName(DPPUtil.parseSymbolicName(newValue));
			}
			removeButton.setEnabled(true);
			setMoveEnable();
			setDirty(true);
			commitChanges(false);
			bundlesTable.update(bundle, null);
			page.updateDocumentIfSource();
			if (isSet) {
				model.fireModelChanged(new ModelChangedEvent(IModelChangedEvent.EDIT, new Object[] { bundle }, null));
			}
		}

		private String getUpperPath(Object object) {
			String path = "bundles/";
			Table table = bundlesTable.getTable();

			for (int i = 0; i < table.getItems().length; i++) {
				if (object.equals(table.getItem(i))) {
					if (i != 0) {
						String upperPath = DPPUtilities.getPath(table.getItem(i - 1).getText(1));
						if (upperPath != null) {
							path = upperPath;
						}
					}
					break;
				}
			}
			return path;
		}

		/**
		 * Returns the value for the given property of the given element.
		 * Returns empty string if the element does not have the given property.
		 * The values of the property that are allowed are: bundle, name,
		 * bundle_name, version, customizer, missing, custom.
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
			BundleInfo bundle = (BundleInfo) object;
			if (property.equals("bundle")) {
				String bundlePath = DPPUtilities.getStringValue(bundle.getBundlePath());
				DPPFileModel model = ((DPPFileModel) getFormPage().getModel());
				IFile ifile = model.getFile();
				IProject project = ifile.getProject();
				String location = project.getLocation().toOSString();
				if (bundlePath.toLowerCase().startsWith(location.toLowerCase() + File.separator)) {
					bundlePath = "<.>" + bundlePath.substring(location.length());
				}
				return bundlePath;
			} else if (property.equals("name")) {
				return DPPUtilities.getStringValue(bundle.getName());
			} else if (property.equals("version")) {
				return DPPUtilities.getStringValue(bundle.getBundleVersion());
			} else if (property.equals("bundle_name")) {
				return DPPUtilities.getStringValue(bundle.getBundleSymbolicName());
			} else if (property.equals("missing")) {
				if (bundle.isMissing()) {
					return new Integer(0);
				}
				return new Integer(1);
			} else if (property.equals("custom")) {
				return bundle.otherHeadersToString();
			} else if (property.equals("customizer")) {
				if (bundle.isCustomizer()) {
					return new Integer(0);
				}
				return new Integer(1);
			}
			return "";
		}
	}

	/**
	 * A content provider mediates between the viewer's model and the viewer
	 * itself.
	 */
	class TableContentProvider extends DefaultContentProvider implements IStructuredContentProvider {
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
				Vector bundles = ((DPPFile) parent).getBundleInfos();
				if (bundles == null) {
					return null;
				}
				BundleInfo[] result = new BundleInfo[bundles.size()];
				bundles.copyInto(result);
				return result;
			}
			return new Object[0];
		}
	}

	/**
	 * A label provider sets for the value of the given column index the value
	 * of the element, that corresponding with this index.
	 */
	class TableLabelProvider extends LabelProvider implements ITableLabelProvider {
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
			if (obj instanceof BundleInfo) {
				BundleInfo bundle = (BundleInfo) obj;
				if (index == 0) {
					String bundlePath = DPPUtilities.getStringValue(bundle.getBundlePath());
					DPPFileModel model = ((DPPFileModel) getFormPage().getModel());
					IFile ifile = model.getFile();
					IProject project = ifile.getProject();
					String location = project.getLocation().toOSString();
					if (bundlePath.startsWith(location + File.separator)) {
						bundlePath = "<.>" + bundlePath.substring(location.length());
					}
					return bundlePath;
				} else if (index == 1) {
					return DPPUtilities.getStringValue(bundle.getName());
				} else if (index == 2) {
					return DPPUtilities.getStringValue(bundle.getBundleSymbolicName());
				} else if (index == 3) {
					return DPPUtilities.getStringValue(bundle.getBundleVersion());
				} else if (index == 4) {
					return new Boolean(bundle.isCustomizer()).toString();
				} else if (index == 5) {
					return new Boolean(bundle.isMissing()).toString();
				} else if (index == 6) {
					return bundle.otherHeadersToString();
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
	public BundlesSection(BundlesFormPage page) {
		super(page);
		this.page = page;
		setHeaderText(ResourceManager.getString(SECTION_TITLE, ""));
		setDescription(ResourceManager.getString(SECTION_DESC, ""));
	}

	/**
	 * This method is called from the <code>createControl</code> method and puts
	 * all custom components in this bundles form section.
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
						final BundleInfo bundle = (BundleInfo) item.getData();
						item.setChecked(bundle.isCustomizer());
						if (DPPUtilities.getStringValue(bundle.getBundlePath()).equals("")) {
							bundleInfoChange(bundle, REMOVE_BUNDLE);
						}
					}
				}
			}
		});
		table.setLayout(new GridLayout());
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		String[] columnTitles = { ResourceManager.getString("DPPEditor.BundlesSection.ColPath"),
				ResourceManager.getString("DPPEditor.BundlesSection.ColName"),
				ResourceManager.getString("DPPEditor.BundlesSection.ColSymbolicName"),
				ResourceManager.getString("DPPEditor.BundlesSection.ColVersion"),
				ResourceManager.getString("DPPEditor.BundlesSection.ColCustomizer"),
				ResourceManager.getString("DPPEditor.BundlesSection.ColMissing"),
				ResourceManager.getString("DPPEditor.BundlesSection.ColCustomHeaders") };
		for (int i = 0; i < columnTitles.length; i++) {
			TableColumn tableColumn = new TableColumn(table, SWT.NULL);
			tableColumn.setText(columnTitles[i]);
		}

		TableControlListener controlListener = new TableControlListener(table);
		controlListener.setResizeMode(EventConstants.BUNDLES_RESIZE_MODE);
		container.addControlListener(controlListener);

		bundlesTable = new TableViewer(table);
		bundlesTable.setContentProvider(new TableContentProvider());
		bundlesTable.setLabelProvider(new TableLabelProvider());
		bundlesTable.addSelectionChangedListener(this);

		String[] data = { "true", "false" }; //$NON-NLS-1$ //$NON-NLS-2$
		customizerCellEditor = new ComboBoxCellEditor(table, data, SWT.READ_ONLY);
		missingCellEditor = new ComboBoxCellEditor(table, data, SWT.READ_ONLY);
		String[] properties = { "bundle", "name", "bundle_name", "version", "customizer", "missing", "custom" };
		bundlesTable.setColumnProperties(properties);
		CellEditor[] editors = new CellEditor[] {
				new CustomCellEditor(container, bundlesTable, table, CustomCellEditor.TEXT_BUTTON_TYPE,
						CustomCellEditor.BUNDLE_PATH), new TextCellEditor(table),
				new TextCellEditor(table),
				new TextCellEditor(table),
				customizerCellEditor,
				missingCellEditor,
				// customizer, missing,
				new CustomCellEditor(container, bundlesTable, table, CustomCellEditor.DIALOG_TYPE,
						CustomCellEditor.BUNDLE_HEADER) };
		bundlesTable.setCellEditors(editors);
		bundlesTable.setCellModifier(new KeyModifier());

		customizerCombo = (CCombo) customizerCellEditor.getControl();
		customizerCombo.setEditable(false);
		customizerCombo.setSize(5, 5);
		customizerCombo.addSelectionListener(this);

		missingCombo = (CCombo) missingCellEditor.getControl();
		missingCombo.setEditable(false);
		missingCombo.setSize(5, 5);
		missingCombo.addSelectionListener(this);

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

		newButton = FormWidgetFactory
				.createButton(buttonComposite, ResourceManager.getString(NEW_BUTTON, ""), SWT.PUSH);
		removeButton = FormWidgetFactory.createButton(buttonComposite, ResourceManager.getString(REMOVE_BUTTON, ""),
				SWT.PUSH);
		upButton = FormWidgetFactory.createButton(buttonComposite, ResourceManager.getString(UP_BUTTON, ""), SWT.PUSH);
		downButton = FormWidgetFactory.createButton(buttonComposite, ResourceManager.getString(DOWN_BUTTON, ""),
				SWT.PUSH);

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
			bundlesTable.setInput(model.getDPPFile());
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
	 * Sets the focus to the table of the bundles and refresh the viewer.
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.FormSection#setFocus()
	 */
	public void setFocus() {
		bundlesTable.getTable().setFocus();
		bundlesTable.refresh();
		TableItem[] items = bundlesTable.getTable().getItems();
		for (int i = 0; i < items.length; i++) {
			BundleInfo info = (BundleInfo) items[i].getData();
			String bundlePath = DPPUtilities.getStringValue(info.getBundlePath());
			String symName = DPPUtilities.getStringValue(info.getBundleSymbolicName());
			String name = DPPUtilities.getStringValue(info.getName());
			String version = DPPUtilities.getStringValue(info.getBundleVersion());
			if (bundlePath.equals("")) {
				bundlesTable.editElement(info, 0);
				break;
			} else if (name.equals("")) {
				bundlesTable.editElement(info, 1);
				break;
			} else if (symName.equals("")) {
				bundlesTable.editElement(info, 2);
				break;
			} else if (version.equals("")) {
				bundlesTable.editElement(info, 3);
				break;
			}
		}
	}

	/**
	 * Sets remove, move up and move down button enable or disable, depending on
	 * the selection in the table.
	 */
	private void updateEnabledButtons() {
		Table table = bundlesTable.getTable();
		TableItem[] selection = table.getSelection();
		boolean hasSelection = selection.length > 0;
		//
		removeButton.setEnabled(hasSelection);
		setMoveEnable();
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
			int index = combo.getSelectionIndex();
			String item = combo.getItem(index);
			Object object = ((IStructuredSelection) bundlesTable.getSelection()).getFirstElement();
			if (object != null && object instanceof BundleInfo) {
				BundleInfo bundleInfo = (BundleInfo) object;
				if (combo == customizerCombo) {
					boolean oldCustomizer = bundleInfo.isCustomizer();
					boolean newCustomizer = item.equals("true");
					if ((oldCustomizer && !newCustomizer) || (!oldCustomizer && newCustomizer)) {
						bundleInfo.setCustomizer(newCustomizer);
						model.fireModelChanged(new ModelChangedEvent(IModelChangedEvent.EDIT,
								new Object[] { bundleInfo }, null));
					}
				} else if (combo == missingCombo) {
					boolean oldMissing = bundleInfo.isMissing();
					boolean newMissing = item.equals("true");
					if ((oldMissing && !newMissing) || (!oldMissing && newMissing)) {
						bundleInfo.setMissing(newMissing);
						model.fireModelChanged(new ModelChangedEvent(IModelChangedEvent.EDIT,
								new Object[] { bundleInfo }, null));
					}
				}
				bundlesChanged();
			}
		}
	}

	/**
	 * Creates the new <code>BundleInfo</code> object, which is presented in the
	 * table as a new table row. Adds this bundle into the Deployment package
	 * file, which bundles presents this table.
	 */
	private void handleNew() {
		Table table = bundlesTable.getTable();
		int size = table.getItemCount();

		if (size != 0) {
			TableItem beforeLastTableItem = table.getItem(size - 1);
			String colonNameValue = beforeLastTableItem.getText(1);
			String colonNamePath = DPPUtilities.getPath(colonNameValue);

			if (colonNamePath == null) {
				bundlesCustomPath = "bundles/";
			} else {
				bundlesCustomPath = colonNamePath;
			}
		} else {
			bundlesCustomPath = "bundles/";
		}

		BundleInfo bundle = new BundleInfo();
		boolean found = false;

		for (int i = 0; i < size; i++) {
			TableItem currentItem = table.getItem(i);
			if (currentItem.getText(0).equalsIgnoreCase("") && !currentItem.getData().equals(bundle)) {
				found = true;
				break;
			}
		}

		if (!found) {
			bundleInfoChange(bundle, ADD_BUNDLE);
			bundlesTable.add(bundle);
			bundlesTable.editElement(bundle, 0);
			setDirty(true);
			commitChanges(false);
			size++;
		}

		table.setSelection(size - 1);
		updateEnabledButtons();
	}

	private String getName(String str) {

		if (str == null)
			return null;
		if (str.equals(""))
			return "";
		if (str.endsWith(".project")) {
			if (Math.max(str.lastIndexOf('\\'), str.lastIndexOf('/')) != Math.max(str.indexOf('\\'), str.indexOf('/'))) {
				str = str.substring(0, Math.max(str.lastIndexOf('\\'), str.lastIndexOf('/')));
				str = str.substring(Math.max(str.lastIndexOf('\\'), str.lastIndexOf('/')) + 1);
				str = str + ".jar";
			}
		}
		int i = 0;
		while (str.charAt(i) == '\\' || str.charAt(i) == '/' || str.charAt(i) == ' ') {
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
	 * Removes from the bundles table the selected <code>BundleInfo</code>
	 * object. Removes this bundle from the Deployment package file, which
	 * bundles presents this table.
	 */
	private void handleRemove() {
		Object object = ((IStructuredSelection) bundlesTable.getSelection()).getFirstElement();
		if (object != null && object instanceof BundleInfo) {
			BundleInfo bundle = (BundleInfo) object;
			bundleInfoChange(bundle, REMOVE_BUNDLE);
		}
		bundlesChanged();
		setDirty(true);
		commitChanges(false);
	}

	/**
	 * Moves up the selected bundle and moves up the bundle in the Deployment
	 * package file, which bundles presents this table.
	 */
	private void handleUp() {
		Object object = ((IStructuredSelection) bundlesTable.getSelection()).getFirstElement();
		if (object != null && object instanceof BundleInfo) {
			BundleInfo bundle = (BundleInfo) object;
			bundleInfoChange(bundle, UP_BUNDLE);
		}
		bundlesChanged();
		setDirty(true);
		commitChanges(false);
		setMoveEnable();
	}

	/**
	 * Moves down the selected bundle and moves down the bundle in the
	 * Deployment package file, which bundles presents this table.
	 */
	private void handleDown() {
		Object object = ((IStructuredSelection) bundlesTable.getSelection()).getFirstElement();
		if (object != null && object instanceof BundleInfo) {
			BundleInfo bundle = (BundleInfo) object;
			bundleInfoChange(bundle, DOWN_BUNDLE);
		}
		bundlesChanged();
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
			if (first instanceof BundleInfo) {
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
			bundlesTable.refresh();
			return;
		}
		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof BundleInfo) {
			BundleInfo bundle = (BundleInfo) changeObject;
			if (event.getChangeType() == IModelChangedEvent.ADD) {
				bundlesTable.add(bundle);
			}
			if (event.getChangeType() == IModelChangedEvent.INSERT) {
				bundlesTable.add(bundle);
				bundlesTable.editElement(bundle, 0);
			} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
				bundlesTable.remove(bundle);
			}
		} else {
			bundlesTable.refresh();
		}
	}

	/**
	 * Sets up and down button to be enable or disable, depending on the
	 * selection in the table.
	 */
	public void setMoveEnable() {
		Table table = bundlesTable.getTable();
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
	 * Notifies of a change to the checked state of an element.
	 * 
	 * @see org.eclipse.jface.viewers.ICheckStateListener#checkStateChanged(org.eclipse.jface.viewers.CheckStateChangedEvent)
	 */
	public void checkStateChanged(CheckStateChangedEvent event) {
	}

	/**
	 * Refreshes the bundle viewer completely with information freshly obtained
	 * from the viewer's model.
	 */
	public void bundlesChanged() {
		if (bundlesTable != null) {
			bundlesTable.refresh();
		}
	}

	/**
	 * Adds, removes, moves up or down the given <code>BundleInfo</code>,
	 * depending on the given key. Notifies all existing
	 * <code>IModelChangedListener</code>'s of a change of the model.
	 * 
	 * @param bundle
	 *            the <code>BundleInfo</code>, on which will be done the action
	 * @param key
	 *            the type of the action. This type can be one of the followed
	 *            values: ADD_BUNDLE, REMOVE_BUNDLE, UP_BUNDLE and DOWN_BUNDLE
	 */
	private void bundleInfoChange(BundleInfo bundle, int key) {
		DPPFile dppFile = ((DPPFileModel) getFormPage().getModel()).getDPPFile();
		Vector infos = dppFile.getBundleInfos();
		switch (key) {
		case ADD_BUNDLE:
			infos.addElement(bundle);
			model.fireModelChanged(new ModelChangedEvent(IModelChangedEvent.ADD, new Object[] { bundle }, null));
			break;
		case REMOVE_BUNDLE:
			infos.removeElement(bundle);
			model.fireModelChanged(new ModelChangedEvent(IModelChangedEvent.REMOVE, new Object[] { bundle }, null));
			break;
		case UP_BUNDLE:
			DPPUtilities.moveElement(infos, bundle, true);
			model.fireModelChanged(new ModelChangedEvent(IModelChangedEvent.INSERT, new Object[] { bundle }, null));
			break;
		case DOWN_BUNDLE:
			DPPUtilities.moveElement(infos, bundle, false);
			model.fireModelChanged(new ModelChangedEvent(IModelChangedEvent.INSERT, new Object[] { bundle }, null));
			break;
		}
	}

	public void update() {
		bundlesTable.refresh();
	}
}
