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
package org.tigris.mtoolkit.dpeditor.preferences;

import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.tigris.mtoolkit.common.preferences.IMToolkitPreferencePage;
import org.tigris.mtoolkit.dpeditor.DPActivator;
import org.tigris.mtoolkit.dpeditor.util.ResourceManager;

/**
 * Preferences page that allows of the user to choose jar signer location
 */
public class DPEditorPreferencesPage extends PreferencePage implements
		IWorkbenchPreferencePage, SelectionListener, IMToolkitPreferencePage {

	/** Check box button which automatically accept changes in bundles */
	private Button acceptCheckBoxButton;

	/** List of all added resource processors */
	private List resList;
	/** Button will be add the resource processor in the text field to the list */
	private Button addButton;
	/** Button will be remove the selected resource processor from the list */
	private Button removeButton;

	/** boolean flag which indicate if OK operation is failed */
	boolean isOkFailed = false;

	private Button editButton;

	/**
	 * Constructor of the deployment package editor preference page
	 */
	public DPEditorPreferencesPage() {
	}

	/**
	 * Creates all page controls. In this case create text field for jar signer
	 * and browse button which open file chooser.
	 * 
	 * @param parent
	 *            the parent composite
	 * @return the created control with all created elements
	 */
	protected Control createPageControl(Composite parent) {
		GridData gd;
		String property;
		GridLayout layout;

		Composite composite = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 10;
		composite.setLayout(layout);
		gd = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gd);

		acceptCheckBoxButton = new Button(composite, SWT.NONE | SWT.CHECK);
		acceptCheckBoxButton.setText(ResourceManager
				.getString("DPPEditor.Accept_Button")); //$NON-NLS-1$
		acceptCheckBoxButton.addSelectionListener(this);
		gd = new GridData();
		gd.verticalAlignment = GridData.BEGINNING;
		gd.horizontalSpan = 3;
		acceptCheckBoxButton.setLayoutData(gd);
		property = System.getProperty("dpeditor.accept");
		if (property == null || property.equals("")
				|| (!property.equals("true") && !property.equals("false"))) {
			property = "false";
		}
		boolean selected = new Boolean(property).booleanValue();
		acceptCheckBoxButton.setSelection(selected);

		Group rpGroup = new Group(composite, SWT.SHADOW_ETCHED_OUT);
		rpGroup.setText(ResourceManager
				.getString("DPPreferencesPage.resources.name"));
		gd = new GridData(GridData.FILL_BOTH);
		gd.minimumHeight = 100;
		gd.horizontalSpan = 3;
		rpGroup.setLayoutData(gd);

		layout = new GridLayout();
		layout.numColumns = 2;
		rpGroup.setLayout(layout);

		resList = new List(rpGroup, SWT.BORDER | SWT.V_SCROLL);
		gd = new GridData(GridData.FILL_BOTH);
		resList.setLayoutData(gd);
		resList.addSelectionListener(this);

		property = System.getProperty("dpeditor.resourceprcessors"); //$NON-NLS-1$
		if (property != null && !property.equals("")) {
			String[] items = getItemsFromProperty(property);
			resList.setItems(items);
		}

		Composite butComp = new Composite(rpGroup, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		butComp.setLayout(layout);

		gd = new GridData();
		gd.verticalAlignment = GridData.BEGINNING;
		butComp.setLayoutData(gd);

		addButton = new Button(butComp, SWT.NONE | SWT.PUSH);
		addButton.setText(ResourceManager.getString("DPPEditor.Add_Button1")); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL);
		addButton.setLayoutData(gd);
		addButton.addSelectionListener(this);

		editButton = new Button(butComp, SWT.NONE | SWT.PUSH);
		editButton.setText(ResourceManager.getString("DPPEditor.Edit_Button")); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL);
		editButton.setLayoutData(gd);
		editButton.addSelectionListener(this);
		editButton.setEnabled(false);

		removeButton = new Button(butComp, SWT.NONE | SWT.PUSH);
		removeButton.setText(ResourceManager
				.getString("DPPEditor.Remove_Button")); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL);
		removeButton.setLayoutData(gd);
		removeButton.addSelectionListener(this);
		removeButton.setEnabled(false);

		return composite;
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
		if (obj == addButton) {
			addAction();
		} else if (obj == editButton) {
			editAction();
		} else if (obj == removeButton) {
			removeAction();
		} else if (obj == resList) {
			int count = resList.getSelectionCount();
			removeButton.setEnabled((count != 0));
			editButton.setEnabled((count == 1));
		}
	}

	/**
	 * Checks if the typed in resource processor text field value is not empty
	 * string and if typed value does not exists in the list added in the list
	 * of all resource processors.
	 */
	private void addAction() {
		InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(), "Add Resource Processor",
				"Please enter ID for new resource processor", null, new IInputValidator() {
					public String isValid(String newText) {
						if (hasTextInList(newText)) {
							return "Entered resource processor ID is already available";
						}else if(newText.isEmpty()){ 
							return ResourceManager.getString("DPPreferencesPage.addResourceProcessorWarning");
						}else {
							return null;
						}
					}
				});
		int result = dialog.open();
		if (result == InputDialog.OK) {
			String string = dialog.getValue();
			boolean hasInList = hasTextInList(string);
			if (!hasInList) {
				resList.add(string);
			}
		}
	}

	/**
	 * Edits selected resource processor.
	 */
	private void editAction() {
		int count = resList.getSelectionCount();
		if (count == 1) {
			int index = resList.getSelectionIndex();
			String oldValue = resList.getItem(index);
			InputDialog dialog = new InputDialog(Display.getCurrent()
					.getActiveShell(), "Edit Resource Processor",
					"Please new ID for selected resource processor", oldValue,
					new IInputValidator() {
						public String isValid(String newText) {
							if (hasTextInList(newText)) {
								return "Entered resource processor ID is already available";
							} else {
								return null;
							}
						}
					});
			int result = dialog.open();
			if (result == InputDialog.OK) {
				String string = dialog.getValue();
				boolean hasInList = hasTextInList(string);
				if (!hasInList) {
					resList.remove(index);
					resList.add(string, index);
					resList.setSelection(index);
				}
			}
		}
	}

	/**
	 * Removes selected resource processor from the list of all resource
	 * processors
	 */
	private void removeAction() {
		int count = resList.getSelectionCount();
		int index = 0;
		if (count == 1) {
			index = resList.getSelectionIndex();
			int listSize = resList.getItemCount();
			resList.remove(resList.getSelectionIndex());
			if (listSize != 1 && index == listSize - 1) {
				index = resList.getItemCount() - 1;
			} else {
				if (index == 0) {
					index = 0;
				}
			}
			if (listSize != 1) {
				resList.setSelection(index);
			} else {
				removeButton.setEnabled(false);
				editButton.setEnabled(false);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.
	 * swt.widgets.Composite)
	 */
	public Control createContents(Composite parent) {
		Control result = createPageControl(parent);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	public void performDefaults() {
		super.performDefaults();
		System.setProperty("dpeditor.accept", "false");
		acceptCheckBoxButton.setSelection(false);
		resList.removeAll();
		Vector resProcs = DPActivator.readResourceProcessors();
		for (int i = 0; i < resProcs.size(); i++) {
			String text = (String) resProcs.elementAt(i);
			if (text != null && !text.equals("") && !hasTextInList(text)) {
				resList.add(text);
			}
		}
	}

	public boolean performOk() {
		boolean isOk = true;
		System.setProperty("dpeditor.accept",
				"" + acceptCheckBoxButton.getSelection());

		String loc2 = createResourceProperty();
		System.setProperty("dpeditor.resourceprcessors", loc2);

		return isOk;
	}

	/**
	 * Sets boolean flag if OK failed.
	 * 
	 * @param isFailed
	 *            the new value of the flag
	 */
	public void setOkFailed(boolean isFailed) {
		isOkFailed = isFailed;
	}

	public boolean performCancel() {
		return true;
	}

	/**
	 * Checks if given text exists in the list items.
	 * 
	 * @param text
	 *            the value will be checked
	 * @return <code>true</code> if the value exists in the list,
	 *         <code>false</code> otherwise
	 */
	private boolean hasTextInList(String text) {
		String[] items = resList.getItems();
		for (int i = 0; i < items.length; i++) {
			String string = items[i];
			if (string.equals(text)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the <code>String</code> array of the resource processor's values
	 * taken from the given property.
	 * 
	 * @param property
	 *            the array values will be taken from this value
	 * @return the array presentation of the given property value
	 */
	private String[] getItemsFromProperty(String property) {
		String[] result = new String[0];
		StringTokenizer tokenizer = new StringTokenizer(property, ";");
		Vector elements = new Vector();
		while (tokenizer.hasMoreTokens()) {
			String next = tokenizer.nextToken().trim();
			if (!next.equals("")) {
				elements.addElement(next);
			}
		}
		result = new String[elements.size()];
		elements.copyInto(result);
		return result;
	}

	/**
	 * Creates the <code>String</code> representation of the all existed in the
	 * list resource processors
	 * 
	 * @return the string representation of the all resource processors in the
	 *         list
	 */
	private String createResourceProperty() {
		String result = "";
		String[] items = resList.getItems();
		for (int i = 0; i < items.length; i++) {
			String string = items[i];
			result += (result.equals("") ? "" : ";") + string;
		}
		return result;
	}
}