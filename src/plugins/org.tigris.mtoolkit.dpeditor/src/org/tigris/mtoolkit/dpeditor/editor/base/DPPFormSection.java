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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.tigris.mtoolkit.dpeditor.editor.forms.FormSection;
import org.tigris.mtoolkit.dpeditor.editor.model.IModelChangedEvent;
import org.tigris.mtoolkit.dpeditor.editor.model.IModelChangedListener;
import org.tigris.mtoolkit.dpeditor.util.DPPErrorHandler;
import org.tigris.mtoolkit.dpeditor.util.ResourceManager;

/**
 * FormSection is a custom control that renders a header, description and custom
 * section and will be added in to the Form. This is the abstract class, that
 * all form section of the deployment package editor will be extend.
 */
public abstract class DPPFormSection extends FormSection implements
		IModelChangedListener {

	/** Holds the error message */
	public static String ERROR_MSG = ResourceManager.getString("DPPEditor.Error", "");
	/** Holds the warning message */
	public static String WARNING_MSG = ResourceManager.getString("DPPEditor.Warning", "");
	/**
	 * Ask the user to enter different values in the table, because there are
	 * two equals values in the table
	 */
	public static final String EQUAL_VALUES_MSG3 = "DPPEditor.FormSection.EqualValuesMsg3";
	/** Announce that in the table two of the values are equals */
	public static final String EQUAL_VALUES_MSG5 = "DPPEditor.FormSection.EqualValuesMsg5";
	/** Message for empty value in the table */
	public static final String EQUAL_VALUES_MSG6 = "DPPEditor.FormSection.EqualValuesMsg6";
	/** The page in which this FormSection will be placed */
	private DPPFormPage formPage;

	/**
	 * Creates the new instance of the FormSection and the form which is the
	 * parent of this section.
	 * 
	 * @param formPage
	 *            the parent form page
	 */
	public DPPFormSection(DPPFormPage formPage) {
		this.formPage = formPage;
	}

	/**
	 * Gets the parent form of this section
	 * 
	 * @return the parent form page
	 */
	public DPPFormPage getFormPage() {
		return formPage;
	}

	/**
	 * Called when there is a change in the model this listener is registered
	 * with.
	 * 
	 * @param e
	 *            a change event that describes the kind of the model change
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.model.IModelChangedListener#modelChanged(org.tigris.mtoolkit.dpeditor.editor.model.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent e) {
	}

	/**
	 * Checks if the given value is exists as a value of the
	 * <code>TableItem</code>s of the given <code>TableViewer</code> and if
	 * exists, checks is the <code>TableItem</code> which value is the given
	 * value is the same <code>TableItem</code> as a given. The column from
	 * which will be get the value is the first one. In this case returns the
	 * index of the <code>TableItem</code>, otherwise returns -1.
	 * 
	 * @param table
	 *            the TableViewer, which TableItems will be checked
	 * @param item
	 *            the TableItem, which will be compare with the viewer's
	 *            TableItems
	 * @param newValue
	 *            the new value of the given TableItem
	 * @return the index of the TableItem in the given TableViewer or -1 if this
	 *         TableItem is not exists in the TableViewer
	 */
	public static int itemExists(TableViewer table, TableItem item, String newValue) {
		return itemExists(table, item, newValue, 0);
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
	 * Checks if the given value is exists as a value of the
	 * <code>TableItem</code>s of the given <code>TableViewer</code> and if
	 * exists, checks is the <code>TableItem</code> which value is the given
	 * value is the same <code>TableItem</code> as a given, as checks every
	 * value in the <code>TableItem</code> of the table with the every value in
	 * the given <code>TableItem</code>. In this case returns the index of the
	 * <code>TableItem</code>, otherwise returns -1.
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
	public static int rowExists(TableViewer table, TableItem item, String newValue, int column) {
		int n = -1;
		Table parentTable = table.getTable();
		for (int i = 0; i < parentTable.getItemCount(); i++) {
			TableItem currentItem = parentTable.getItem(i);
			if (currentItem.getText(column).equalsIgnoreCase(newValue)) {
				if (!currentItem.equals(item)) {
					int columns = parentTable.getColumnCount();
					for (int j = 0; j < columns; j++) {
						if (j != column) {
							if (currentItem.getText(j).equals(item.getText(j))) {
								return i;
							}
						}
					}
				}
			}
		}
		return n;
	}

	/**
	 * Checks if the given value, which is the file path, is exists as a value
	 * of the <code>TableItem</code>s of the given <code>TableViewer</code> and
	 * if exists, checks is the <code>TableItem</code> which value is the given
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
	public static int fileExists(TableViewer table, TableItem item, String newValue, int column) {
		int n = -1;
		Table parentTable = table.getTable();
		String res = System.getProperty("os.name");
		boolean ignoreCase = !(res.toLowerCase().indexOf("windows") < 0);
		for (int i = 0; i < parentTable.getItemCount(); i++) {
			TableItem currentItem = parentTable.getItem(i);
			if (ignoreCase) {
				if (currentItem.getText(column).equalsIgnoreCase(newValue)) {
					if (!currentItem.equals(item)) {
						return i;
					}
				}
			} else {
				if (currentItem.getText(column).equals(newValue)) {
					if (!currentItem.equals(item)) {
						return i;
					}
				}
			}
		}
		return n;
	}

	/**
	 * Opens the <code>MessageDialog</code>'s standard warning dialog with the
	 * given message.
	 * 
	 * @param message
	 *            the message
	 */
	public static void showWarningTableDialog(final String message) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				MessageDialog.openWarning(DPPErrorHandler.getAnyShell(), WARNING_MSG, message);
			}
		});
	}

	/**
	 * Opens the <code>MessageDialog</code>'s standard error dialog with the
	 * given message.
	 * 
	 * @param message
	 *            the message
	 */
	public static void showErrorTableDialog(final String message) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				MessageDialog.openError(DPPErrorHandler.getAnyShell(), ERROR_MSG, message);
			}
		});
	}

	/**
	 * Opens the <code>MessageDialog</code>'s standard error dialog with the
	 * given message. The thread which calls this method is suspended until the
	 * runnable completes.
	 * 
	 * @param message
	 *            the message
	 */
	public static void syncShowErrorTableDialog(final String message) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				MessageDialog.openError(DPPErrorHandler.getAnyShell(), ERROR_MSG, message);
			}
		});
	}
}
