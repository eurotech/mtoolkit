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
package org.tigris.mtoolkit.common.gui;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * @since 5.0
 */
public class PropertiesPage extends PropertyPage {

	/**
	 * @since 6.0
	 */
	protected TreeViewer viewer;
	protected Group propertiesGroup;
	private String groupName = "Headers";
	private Action copyAction;

	public class TableContentProvider implements ITreeContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof Vector) {
				return ((Vector) parent).toArray(new Object[((Vector) parent).size()]);
			}
			return new Object[0];
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		/**
		 * @since 6.0
		 */
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof PropertyObject) {
				PropertyObject p = (PropertyObject) parentElement;
				if (p.getData() instanceof Map) {
					Vector children = getData((Map) p.getData());
					return children.toArray(new Object[children.size()]);
				} else if (p.getData() instanceof Dictionary) {
					Vector children = getData((Dictionary) p.getData());
					return children.toArray(new Object[children.size()]);
				} else if (p.getData() instanceof Object[]) {
					Vector children = getData(p.getName(), (Object[]) p.getData());
					return children.toArray(new Object[children.size()]);
				} else if (p.getData() instanceof Collection) {
					Vector children = getData(p.getName(), (Collection) p.getData());
					return children.toArray(new Object[children.size()]);
				}
			}
			return new Object[0];
		}

		/**
		 * @since 6.0
		 */
		public Object getParent(Object element) {
			return null;
		}

		/**
		 * @since 6.0
		 */
		public boolean hasChildren(Object element) {
			if (element instanceof PropertyObject) {
				PropertyObject p = (PropertyObject) element;
				Object d = p.getData();
				return d instanceof Map || d instanceof Dictionary || d instanceof Object[] || d instanceof Collection;
			}
			return false;
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

		Tree tree = new Tree(propertiesGroup, SWT.BORDER
						| SWT.MULTI
						| SWT.H_SCROLL
						| SWT.V_SCROLL
						| SWT.FULL_SELECTION);
		tree.setLayout(new GridLayout());
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);

		createActions(parent.getShell());

		MenuManager menuManager = new MenuManager();
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager mgr) {
				fillContextMenu(mgr);
			}
		});
		Menu contextMenu = menuManager.createContextMenu(tree);
		tree.setMenu(contextMenu);

		String[] columnTitles = { "Name", "Value" };
		TreeColumn treeColumn = new TreeColumn(tree, SWT.NULL);
		treeColumn.setText(columnTitles[0]);
		treeColumn.setWidth(130);
		treeColumn = new TreeColumn(tree, SWT.NULL);
		treeColumn.setText(columnTitles[1]);
		treeColumn.setWidth(315);
		viewer = new TreeViewer(tree);
		viewer.setContentProvider(new TableContentProvider());
		viewer.setLabelProvider(new TableLabelProvider());

		viewer.setColumnProperties(new String[2]);

		gd.widthHint = 460 + tree.getBorderWidth() * 2;

		parent.getShell().setText(getTitle());

		control.pack();

		return control;
	}

	protected String getGroupName() {
		return groupName;
	}

	public void setData(Dictionary data) {
		viewer.setInput(getData(data));
	}

	public void setData(Map data) {
		viewer.setInput(getData(data));
	}

	private Vector getData(Dictionary data) {
		Vector dataVector = new Vector();
		Enumeration keys = data.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			Object value = data.get(key);
			dataVector.addAll(getElements(key, value));
		}
		return dataVector;
	}

	private Vector getData(Map data) {
		Vector dataVector = new Vector();
		Iterator keys = data.keySet().iterator();
		while (keys.hasNext()) {
			Object key = keys.next();
			Object value = data.get(key);
			dataVector.addAll(getElements(key, value));
		}
		return dataVector;
	}

	private Vector getData(String key, Object[] data) {
		Vector dataVector = new Vector();
		for (int i = 0; i < data.length; i++) {
			dataVector.addAll(getElements(key + "[" + i + "]", data[i]));
		}
		return dataVector;
	}

	private Vector getData(String key, Collection data) {
		Vector dataVector = new Vector();
		int i = 0;
		for (Iterator it = data.iterator(); it.hasNext(); i++) {
			dataVector.addAll(getElements(key + "[" + i + "]", it.next()));
		}
		return dataVector;
	}

	private Vector getElements(Object key, Object value) {
		Vector elements = new Vector();
		if (value instanceof Map || value instanceof Dictionary || value instanceof Object[]
				|| value instanceof Collection) {
			PropertyObject object = new PropertyObject(key.toString(), "");
			object.setData(value);
			elements.add(object);
		} else {
			PropertyObject object = new PropertyObject(key.toString(), value.toString());
			elements.add(object);
		}

		return elements;
	}

	public void setGroupName(String tableTitle) {
		groupName = tableTitle;
	}

	private void createActions(Shell shell) {
		copyAction = new CopyAction(shell);
	}

	/**
	 * @since 6.0
	 */
	protected void fillContextMenu(IMenuManager manager) {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();

		manager.add(copyAction);
		copyAction.setEnabled(selection.size() > 0);
		copyAction.setText(selection.size() > 1 ? CopyAction.COPY_ROWS : CopyAction.COPY_ROW);
	}

	private class CopyAction extends Action {
		private static final String COPY_ROW = "Copy row";
		private static final String COPY_ROWS = "Copy rows";
		private final String NL = System.getProperty("line.separator");

		private Clipboard clipboard;

		public CopyAction(Shell shell) {
			super(COPY_ROW);
			clipboard = new Clipboard(shell.getDisplay());
		}

		public void run() {
			IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
			StringBuffer sb = new StringBuffer();

			for (Iterator it = selection.iterator(); it.hasNext();) {
				Object element = it.next();
				if (element instanceof PropertyObject) {
					PropertyObject property = (PropertyObject) element;
					if (sb.length() > 0) {
						sb.append(NL);
					}
					sb.append(property.name);
					sb.append(": ");
					sb.append(property.value);
				}
			}
			if (sb.length() == 0) {
				return;
			}
			TextTransfer textTransfer = TextTransfer.getInstance();
			clipboard.setContents(new Object[] { sb.toString() }, new Transfer[] { textTransfer });
		}
	}
}
