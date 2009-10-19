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

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.tigris.mtoolkit.dpeditor.DPActivator;
import org.tigris.mtoolkit.dpeditor.util.ResourceManager;

/**
 * Preferences page that allows of the user to add and remove resource
 * processors that will be used in the deployment package editor for processors
 * of the resources.
 */
public class ResourceProcessorsPreferencesPage extends PreferencePage implements
		IWorkbenchPreferencePage, SelectionListener, ModifyListener {

	/** Text field for resource processor will be added in list */
	private Text resProcessorText;
	/** List of all added resource processors */
	private List resList;
	/** Button will be add the resource processor in the text field to the list */
	private Button addButton;
	/** Button will be remove the selected resource processor from the list */
	private Button removeButton;

	/** boolean flag which indicate if OK operation is failed */
	boolean isOkFailed = false;

	/**
	 * Constructor of the resource processor preferences page.
	 */
	public ResourceProcessorsPreferencesPage() {
	}

	/**
	 * Creates all page controls. In this case create text field for added
	 * resource processor, list with all added resource processors and add and
	 * remove buttons.
	 * 
	 * @param parent
	 *            the parent composite
	 * @return the created control with all created elements
	 */
	protected Control createPageControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = new Label(composite, SWT.NONE);
		label.setText(ResourceManager.getString("DPPreferencesPage.resources.resources_label")); //$NON-NLS-1$

		Composite resProcessorComp = new Composite(composite, SWT.NONE);
		GridLayout resProcessorLayout = new GridLayout();
		resProcessorLayout.marginHeight = 0;
		resProcessorLayout.marginWidth = 0;
		resProcessorLayout.numColumns = 2;
		resProcessorComp.setLayout(resProcessorLayout);
		resProcessorComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		resProcessorText = new Text(resProcessorComp, SWT.BORDER);
		resProcessorText.addModifyListener(this);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 1;
		resProcessorText.setLayoutData(gd);

		Label emptyLabel = new Label(resProcessorComp, SWT.NONE);
		emptyLabel.setText(""); //$NON-NLS-1$

		Composite listComp = new Composite(composite, SWT.NONE);
		GridLayout listLayout = new GridLayout();
		listLayout.marginHeight = 0;
		listLayout.marginWidth = 0;
		listLayout.numColumns = 2;
		listComp.setLayout(listLayout);
		listComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		resList = new List(listComp, SWT.BORDER);
		String property = System.getProperty("dpeditor.resourceprcessors"); //$NON-NLS-1$
		if (property != null && !property.equals("")) {
			String[] items = getItemsFromProperty(property);
			resList.setItems(items);
		}
		GridData listGd = new GridData(GridData.FILL_BOTH);
		listGd.grabExcessHorizontalSpace = true;
		listGd.horizontalSpan = 1;
		resList.setLayoutData(listGd);
		resList.addSelectionListener(this);

		Composite butComp = new Composite(listComp, SWT.NONE);
		GridLayout butLayout = new GridLayout();
		butLayout.marginHeight = 0;
		butLayout.marginWidth = 0;
		butLayout.numColumns = 1;
		butComp.setLayout(butLayout);
		butComp.setLayoutData(new GridData(GridData.BEGINNING));

		addButton = new Button(butComp, SWT.NONE | SWT.PUSH);
		addButton.setText(ResourceManager.getString("DPPEditor.Add_Button")); //$NON-NLS-1$
		addButton.addSelectionListener(this);

		removeButton = new Button(butComp, SWT.NONE | SWT.PUSH);
		removeButton.setText(ResourceManager.getString("DPPEditor.Remove_Button")); //$NON-NLS-1$
		removeButton.addSelectionListener(this);

		GridData butGd = new GridData();
		butGd.verticalAlignment = GridData.BEGINNING;
		butGd.widthHint = removeButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		addButton.setLayoutData(butGd);
		addButton.setEnabled(false);

		butGd = new GridData();
		butGd.verticalAlignment = GridData.BEGINNING;
		removeButton.setLayoutData(butGd);
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
		} else if (obj == removeButton) {
			removeAction();
		} else if (obj == resList) {
			int count = resList.getSelectionCount();
			removeButton.setEnabled((count != 0));
		}
	}

	/**
	 * Checks if the typed in resource processor text field value is not empty
	 * string and if typed value does not exists in the list added in the list
	 * of all resource processors.
	 */
	private void addAction() {
		String string = resProcessorText.getText();
		boolean hasInList = hasTextInList(string);
		if (!hasInList) {
			resList.add(string);
		}
		resProcessorText.setText("");
		resProcessorText.setFocus();
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
			}
		}
		setAddEnabled();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.DialogPage#getShell()
	 */
	public Shell getShell() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		Shell shell = display.getActiveShell();
		return shell;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.
	 * swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		Control result = createPageControl(parent);
		return result;
	}

	public void init(IWorkbench workbench) {
	}

	/**
	 * (non-Javadoc) Method declared on PreferencePage
	 */
	protected void performDefaults() {
		super.performDefaults();
		resProcessorText.setText("");
		resList.removeAll();
		Vector resProcs = DPActivator.readResourceProcessors();
		for (int i = 0; i < resProcs.size(); i++) {
			String text = (String) resProcs.elementAt(i);
			if (text != null && !text.equals("") && !hasTextInList(text)) {
				resList.add(text);
			}
		}
	}

	/**
	 * (non-Javadoc) Method declared on PreferencePage
	 */
	public boolean performOk() {
		boolean isOk = true;
		String text = resProcessorText.getText();
		if (text != null && !text.equals("") && !hasTextInList(text)) {
			resList.add(text);
		}
		String loc = createResourceProperty();
		System.setProperty("dpeditor.resourceprcessors", loc);
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

	/**
	 * (non-Javadoc) Method declared on PreferencePage
	 */
	public boolean performCancel() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events
	 * .ModifyEvent)
	 */
	public void modifyText(ModifyEvent e) {
		Object source = e.getSource();
		if (source instanceof Text) {
			setAddEnabled();
		}
	}

	/**
	 * Sets the add button enabled or disabled depends of the typed text in the
	 * resource processor text field
	 */
	private void setAddEnabled() {
		String text = resProcessorText.getText().trim();
		if (text == null || text.equals("")) {
			addButton.setEnabled(false);
		} else {
			boolean hasInList = hasTextInList(text);
			addButton.setEnabled(true && !hasInList);
		}
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