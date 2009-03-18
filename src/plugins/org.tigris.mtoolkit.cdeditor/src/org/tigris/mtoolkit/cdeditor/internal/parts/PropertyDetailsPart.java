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
package org.tigris.mtoolkit.cdeditor.internal.parts;

import java.util.Iterator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.tigris.mtoolkit.cdeditor.internal.dialogs.PropertyEditDialog;
import org.tigris.mtoolkit.cdeditor.internal.dialogs.ResourceAddDialog;
import org.tigris.mtoolkit.cdeditor.internal.model.CDModelEvent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDBaseProperty;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDComponent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDModel;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDProperties;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDProperty;
import org.tigris.mtoolkit.cdeditor.internal.model.impl.CDProperties;
import org.tigris.mtoolkit.cdeditor.internal.model.impl.CDProperty;
import org.tigris.mtoolkit.cdeditor.internal.providers.PropertiesContentProvider;
import org.tigris.mtoolkit.cdeditor.internal.providers.PropertiesLabelProvider;


public class PropertyDetailsPart extends ComponentDetailsPart {

	private Tree treeProperties;
	private TreeViewer propertiesViewer;
	private ICDComponent component;
	private ICDModel model;

	private Button btnAddProperty;
	private Button btnEditProperty;
	private Button btnRemoveProperty;
	private Button btnMoveUpProperty;
	private Button btnMoveDownProperty;
	private Menu propertiesAddMenu;

	private static final String COLUMN_FIELD_MAP = "column_field_map";

	public PropertyDetailsPart() {
		super("Properties", "Properties of the selected component", false);
	}

	protected void createSectionContents(Composite parent) {

		FormToolkit toolkit = getManagedForm().getToolkit();

		parent.setLayout(new GridLayout(2, false));

		treeProperties = toolkit.createTree(parent, SWT.FULL_SELECTION | SWT.MULTI);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 120;
		gridData.widthHint = 100;
		gridData.verticalSpan = 6;
		treeProperties.setLayoutData(gridData);
		TreeColumn colName = new TreeColumn(treeProperties, SWT.NONE);
		colName.setText("Name");
		colName.setWidth(120);
		colName.setData(COLUMN_FIELD_MAP, PropertyEditDialog.FIELD_NAME);
		TreeColumn colValue = new TreeColumn(treeProperties, SWT.NONE);
		colValue.setText("Value");
		colValue.setWidth(100);
		colValue.setData(COLUMN_FIELD_MAP, PropertyEditDialog.FIELD_VALUE);
		TreeColumn colType = new TreeColumn(treeProperties, SWT.NONE);
		colType.setText("Type");
		colType.setWidth(80);
		colType.setData(COLUMN_FIELD_MAP, PropertyEditDialog.FIELD_TYPE);
		treeProperties.setHeaderVisible(true);
		propertiesViewer = new TreeViewer(treeProperties);
		propertiesViewer.setContentProvider(new PropertiesContentProvider());
		propertiesViewer.setLabelProvider(new PropertiesLabelProvider());
		propertiesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtonsState();
			}
		});
		propertiesViewer.getTree().addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				TreeItem item = propertiesViewer.getTree().getSelection()[0];
				if (item.getData() instanceof String) {
					ICDProperty prop = (ICDProperty) item.getParentItem().getData();
					handleEditProperty(prop, PropertyEditDialog.FIELD_VALUE);
					return;
				}
				TreeColumn column = getTreeColumnAt(propertiesViewer.getTree(), e.x);
				String initialFocus = column != null ? (String) column.getData(COLUMN_FIELD_MAP) : "";
				IStructuredSelection selection = (IStructuredSelection) propertiesViewer.getSelection();
				ICDBaseProperty property = (ICDBaseProperty) selection.getFirstElement();
				if (property instanceof ICDProperty) {
					handleEditProperty((ICDProperty) property, initialFocus);
				} else if (property instanceof ICDProperties) {
					handleEditResource((ICDProperties) property);
				}
			}
		});
		propertiesViewer.getTree().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.DEL)
					handlePropertyRemove();
			}
		});

		final Composite addButtonsComposite = toolkit.createComposite(parent);
		toolkit.paintBordersFor(addButtonsComposite);
		gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		addButtonsComposite.setLayoutData(gridData);
		GridLayout addButtonsLayout = new GridLayout(2, false);
		addButtonsLayout.horizontalSpacing = 0;
		addButtonsLayout.verticalSpacing = 0;
		addButtonsLayout.marginHeight = 0;
		addButtonsLayout.marginWidth = 0;
		addButtonsComposite.setLayout(addButtonsLayout);

		btnAddProperty = toolkit.createButton(addButtonsComposite, "Add...", SWT.PUSH);
		btnAddProperty.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		btnAddProperty.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ICDProperty property = handleAddProperty();
				if (property == null) {
					return;
				}
				component.addProperty(property);
			}
		});

		propertiesAddMenu = new Menu(btnAddProperty);
		MenuItem propertyItem = new MenuItem(propertiesAddMenu, SWT.PUSH);
		propertyItem.setText("Property...");
		propertyItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ICDProperty property = handleAddProperty();
				if (property == null) {
					return;
				}
				component.addProperty(property);
			}
		});

		MenuItem propertiesItem = new MenuItem(propertiesAddMenu, SWT.PUSH);
		propertiesItem.setText("Resource...");
		propertiesItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ICDProperties resource = handleAddResource();
				if (resource == null) {
					return;
				}
				component.addProperty(resource);
			}
		});

		Button btnArrow = toolkit.createButton(addButtonsComposite, null, SWT.ARROW | SWT.DOWN);
		btnArrow.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		btnArrow.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Rectangle btnRect = btnAddProperty.getBounds();
				Point btnRelativePos = new Point(btnRect.x, btnRect.y + btnRect.height);
				Point displayRelativePos = Display.getCurrent().map(addButtonsComposite, null, btnRelativePos);
				propertiesAddMenu.setLocation(displayRelativePos);
				propertiesAddMenu.setVisible(true);
			}
		});

		btnEditProperty = toolkit.createButton(parent, "Edit...", SWT.PUSH);
		gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		btnEditProperty.setLayoutData(gridData);
		btnEditProperty.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TreeItem item = propertiesViewer.getTree().getSelection()[0];
				if (item.getData() instanceof String) {
					ICDProperty prop = (ICDProperty) item.getParentItem().getData();
					// focusOnMulti
					handleEditProperty(prop, PropertyEditDialog.FIELD_VALUE);
					return;
				}
				IStructuredSelection selection = (IStructuredSelection) propertiesViewer.getSelection();
				ICDBaseProperty property = (ICDBaseProperty) selection.getFirstElement();
				if (property instanceof ICDProperty) {
					handleEditProperty((ICDProperty) property, PropertyEditDialog.FIELD_VALUE);
				} else if (property instanceof ICDProperties) {
					handleEditResource((ICDProperties) property);
				}
			}
		});

		btnRemoveProperty = toolkit.createButton(parent, "Remove", SWT.PUSH);
		gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		btnRemoveProperty.setLayoutData(gridData);
		btnRemoveProperty.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handlePropertyRemove();
			}
		});

		btnMoveUpProperty = toolkit.createButton(parent, "Up", SWT.PUSH);
		gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		btnMoveUpProperty.setLayoutData(gridData);
		btnMoveUpProperty.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = (IStructuredSelection) propertiesViewer.getSelection();
				Iterator propsIterator = selection.iterator();
				while (propsIterator.hasNext()) {
					ICDBaseProperty prop = (ICDBaseProperty) propsIterator.next();
					component.moveUpProperty(prop);
				}
				Object sel = selection.getFirstElement();
				if (sel != null) {
					propertiesViewer.reveal(sel);
				}
			}
		});

		btnMoveDownProperty = toolkit.createButton(parent, "Down", SWT.PUSH);
		gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		btnMoveDownProperty.setLayoutData(gridData);
		btnMoveDownProperty.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = (IStructuredSelection) propertiesViewer.getSelection();
				Object[] selectedProps = selection.toArray();
				for (int i = selectedProps.length - 1; i >= 0; i--) {
					ICDBaseProperty prop = (ICDBaseProperty) selectedProps[i];
					component.moveDownProperty(prop);
				}
				Object sel = selection.getFirstElement();
				if (sel != null) {
					propertiesViewer.reveal(sel);
				}
			}
		});

		model = (ICDModel) getManagedForm().getInput();
		updateButtonsState();
	}

	private void updateButtonsState() {
		btnAddProperty.setEnabled(component != null);
		IStructuredSelection selection = (IStructuredSelection) propertiesViewer.getSelection();
		ICDBaseProperty firstSelectedProperty = null;
		ICDBaseProperty lastSelectedProperty = null;
		if (selection.getFirstElement() instanceof ICDBaseProperty) {
			firstSelectedProperty = (ICDBaseProperty) selection.getFirstElement();
			Iterator selIterator = selection.iterator();
			while (selIterator.hasNext()) {
				lastSelectedProperty = (ICDBaseProperty) selIterator.next();
			}
			btnEditProperty.setEnabled(selection.size() == 1);
			btnRemoveProperty.setEnabled(selection.size() > 0);
			ICDBaseProperty[] allProps = component.getProperties();
			btnMoveUpProperty.setEnabled(allProps.length > 0 && allProps[0] != firstSelectedProperty);
			btnMoveDownProperty.setEnabled(allProps.length > 0 && allProps[allProps.length - 1] != lastSelectedProperty);
		} else {
			btnEditProperty.setEnabled(selection.getFirstElement() != null);
			btnRemoveProperty.setEnabled(false);
			btnMoveUpProperty.setEnabled(false);
			btnMoveDownProperty.setEnabled(false);
		}
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		IStructuredSelection sel = (IStructuredSelection) selection;
		if (selection.isEmpty()) {
			component = null;
		} else {
			Object selectedObject = (sel.size() > 0) ? sel.getFirstElement() : null;
			if (selectedObject instanceof ICDComponent)
				component = (ICDComponent) selectedObject;
		}
		propertiesViewer.setInput(component);
		refresh();
	}

	private ICDProperty handleAddProperty() {
		// TODO: Use factory for generating model objects
		ICDProperty property = new CDProperty();
		PropertyEditDialog propsDialog = new PropertyEditDialog(getManagedForm().getForm().getShell(), "Add Property", property, true, PropertyEditDialog.FIELD_NAME);
		propsDialog.open();
		int code = propsDialog.getReturnCode();
		if (code == Window.OK) {
			return property;
		}
		return null;
	}

	private void handleEditProperty(ICDProperty prop, String initialFocus) {
		PropertyEditDialog propsDialog = new PropertyEditDialog(getManagedForm().getForm().getShell(), "Edit Property", prop, false, initialFocus);
		propsDialog.open();

	}

	private ICDProperties handleAddResource() {
		// TODO: Use factory for generating model objects
		ICDProperties resource = new CDProperties();
		ResourceAddDialog resourceDialog = new ResourceAddDialog(getManagedForm().getForm().getShell(), "Add Resource", resource, model.getProjectContext());
		resourceDialog.open();
		int code = resourceDialog.getReturnCode();
		if (code == InputDialog.OK) {
			resource.setEntry(resourceDialog.getResource().getEntry());
			return resource;
		}
		return null;
	}

	private void handleEditResource(ICDProperties properties) {
		ResourceAddDialog propsDialog = new ResourceAddDialog(getManagedForm().getForm().getShell(), "Edit Resource", properties, model.getProjectContext());
		propsDialog.open();
	}

	public void commit(boolean onSave) {
		super.commit(onSave);
	}

	public void refresh() {
		propertiesViewer.refresh();
		updateValidationStatus();
		updateButtonsState();
		super.refresh();
	}

	protected void updateValidationStatus() {
		propertiesViewer.refresh();
		IStatus validationStatus = Status.OK_STATUS;
		if (component != null && component.getModel() != null) {
			ICDBaseProperty[] props = component.getProperties();
			for (int i = 0; i < props.length; i++) {
				IStatus newValidationStatus = component.getModel().getValidationStatus(props[i])[0];
				if (validationStatus.isOK()) {
					validationStatus = newValidationStatus;
				}
				if (validationStatus.matches(IStatus.INFO)) {
					if (newValidationStatus.matches(IStatus.WARNING) || newValidationStatus.matches(IStatus.ERROR)) {
						validationStatus = newValidationStatus;
					}
				}
				if (validationStatus.matches(IStatus.WARNING)) {
					if (newValidationStatus.matches(IStatus.ERROR)) {
						validationStatus = newValidationStatus;
					}
				}
			}
		}
		sectionDecoration.updateStatus(validationStatus);
		super.updateValidationStatus();
	}

	public void modelModified(CDModelEvent event) {
		handleComponentChildModelChange(event, ICDBaseProperty.class, component);
	}

	private void handlePropertyRemove() {
		Iterator propsIterator = ((IStructuredSelection) propertiesViewer.getSelection()).iterator();
		while (propsIterator.hasNext()) {
			ICDBaseProperty prop = (ICDBaseProperty) propsIterator.next();
			component.removeProperty(prop);
		}
	}

}
