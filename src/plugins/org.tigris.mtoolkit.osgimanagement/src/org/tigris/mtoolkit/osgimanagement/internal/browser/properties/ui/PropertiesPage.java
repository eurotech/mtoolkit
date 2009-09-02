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
package org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Vector;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.dialogs.PropertyPage;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;

public class PropertiesPage extends PropertyPage {

	protected TableViewer tableViewer;
	protected Group propertiesGroup;

	public class TableContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof Vector) {
				if (parent == null) {
					return null;
				}
				PropertyObject[] result = new PropertyObject[((Vector) parent).size()];
				result = (PropertyObject[]) ((Vector) parent).toArray(result);

				if (result != null) {
					return result;
				} else {
					return new Object[0];
				}
			}
			return new Object[0];
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	public class TableLabelProvider extends LabelProvider implements ITableLabelProvider {
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			String columnText = null;
			PropertyObject dataElement = (PropertyObject) element;
			switch (columnIndex) {
			case 0:
				columnText = dataElement.getName();
				break;
			case 1:
				columnText = dataElement.getValue();
				break;
			}
			return columnText;
		}
	}

	public Control createContents(Composite parent) {
		return createPage(parent);
	}

	private Control createPage(Composite parent) {
		Composite control = new Composite(parent, SWT.NONE);
		control.setLayout(new GridLayout());
		control.setLayoutData(new GridData(GridData.FILL_BOTH));

		// Connect properties group
		propertiesGroup = new Group(control, SWT.NONE);
		propertiesGroup.setText(getGroupName());
		GridData gd = new GridData(GridData.FILL_BOTH);
		propertiesGroup.setLayoutData(gd);
		propertiesGroup.setLayout(new GridLayout());

		Table table = new Table(propertiesGroup, SWT.BORDER
						| SWT.SINGLE
						| SWT.H_SCROLL
						| SWT.V_SCROLL
						| SWT.FULL_SELECTION);
		table.setLayout(new GridLayout());
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		String[] columnTitles = { Messages.name_column_label, Messages.value_column_label };
		TableColumn tableColumn = new TableColumn(table, SWT.NULL);
		tableColumn.setText(columnTitles[0]);
		tableColumn.setWidth(130);
		tableColumn = new TableColumn(table, SWT.NULL);
		tableColumn.setText(columnTitles[1]);
		tableColumn.setWidth(315);
		tableViewer = new TableViewer(table);
		tableViewer.setContentProvider(new TableContentProvider());
		tableViewer.setLabelProvider(new TableLabelProvider());

		tableViewer.setColumnProperties(new String[2]);
		DefaultTextCellEditor editors[] = new DefaultTextCellEditor[] { new DefaultTextCellEditor(tableViewer, 0),
			new DefaultTextCellEditor(tableViewer, 1) };
		tableViewer.setCellEditors(editors);
		tableViewer.setCellModifier(new DefaultCellModifier());

		gd.widthHint = 460 + table.getBorderWidth() * 2;

		parent.getShell().setText(getTitle());

		control.pack();

		return control;
	}

	protected String getGroupName() {
		return Messages.headers_label;
	}

	public void setData(Dictionary data) {
		Vector dataVector = new Vector();
		Enumeration keys = data.keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = data.get(key);
			PropertyObject object = new PropertyObject(key.toString(), value.toString());
			dataVector.addElement(object);
		}
		tableViewer.setInput(dataVector);
	}
}