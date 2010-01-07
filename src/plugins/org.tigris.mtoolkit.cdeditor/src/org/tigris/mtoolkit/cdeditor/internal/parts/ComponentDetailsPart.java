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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.tigris.mtoolkit.cdeditor.internal.ReflowingExpansionHandler;
import org.tigris.mtoolkit.cdeditor.internal.model.CDModelEvent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDElement;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDModifyListener;
import org.tigris.mtoolkit.common.gui.StatusSectionDecoration;


public abstract class ComponentDetailsPart extends AbstractFormPart implements
		IDetailsPage, ICDModifyListener {

	protected StatusSectionDecoration sectionDecoration;
	protected Section section;
	protected Control statusLine;

	private String text;
	private String description;
	private boolean expanded;

	public ComponentDetailsPart(String text, String description,
			boolean expanded) {
		this.description = description;
		this.text = text;
		this.expanded = expanded;
	}

	private boolean ignoreModify = false;
	private ModifyListener textModifyListener = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			if (!ignoreModify)
				markDirty();
		}
	};
	private SelectionListener selectionModifyListener = new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			// markDirty();
			commit(false);
		}
	};

	private FocusListener commitFocusListener = new FocusAdapter() {
		public void focusLost(FocusEvent e) {
			commit(false);
		}
	};

	protected void linkWithFormLifecycle(Text textWidget, boolean commitOnLostFocus) {
		textWidget.addModifyListener(textModifyListener);
		if (commitOnLostFocus)
			textWidget.addFocusListener(commitFocusListener);
	}

	protected void linkWithFormLifecycle(Button button, boolean commitOnLostFocus) {
		button.addSelectionListener(selectionModifyListener);
		if (commitOnLostFocus)
			button.addFocusListener(commitFocusListener);
	}

	protected void linkWithFormLifecycle(CCombo combo, boolean commitOnLostFocus) {
		combo.addSelectionListener(selectionModifyListener);
		if (commitOnLostFocus)
			combo.addFocusListener(commitFocusListener);
	}

	protected void beginRefresh() {
		ignoreModify = true;
	}

	protected void endRefresh() {
		ignoreModify = false;
	}

	public void createContents(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();

		int style = Section.TITLE_BAR | Section.TWISTIE | Section.LEFT_TEXT_CLIENT_ALIGNMENT;
		if (expanded)
			style |= Section.EXPANDED;
		section = toolkit.createSection(parent, style);
		section.setText(text);
		section.setDescription(description);
		section.marginWidth = 10;
		section.marginHeight = 0;
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.BEGINNING);
		section.setLayoutData(gridData);
		hookSectionListeners(section);

		Composite client = toolkit.createComposite(section, SWT.WRAP);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 3;
		layout.marginHeight = 1;
		client.setLayout(layout);
		toolkit.paintBordersFor(client);

		Composite contents = toolkit.createComposite(client, SWT.NONE);
		contents.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		toolkit.paintBordersFor(contents);

		createSectionContents(contents);

		sectionDecoration = new StatusSectionDecoration(section);
		statusLine = sectionDecoration.createStatusLine(client);
		statusLine.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		section.setClient(client);
	}

	abstract protected void createSectionContents(Composite parent);

	public static void hookSectionListeners(Section section) {
		section.addExpansionListener(ReflowingExpansionHandler.INSTANCE);
	}

	public static TreeColumn getTreeColumnAt(Tree tree, int x) {
		TreeColumn[] cols = tree.getColumns();
		int cummulativeWidth = 0;
		for (int i = 0; i < cols.length; i++) {
			TreeColumn col = cols[i];
			cummulativeWidth += col.getWidth();
			if (cummulativeWidth > x)
				return col;
		}
		return null;
	}

	/**
	 * Checks model event, whether it describes changes for given child class
	 * and parent.
	 * 
	 * @param event
	 */
	protected boolean handleComponentChildModelChange(CDModelEvent event, Class childClass, ICDElement parent) {
		int type = event.getType();
		switch (type) {
		case CDModelEvent.ADDED:
		case CDModelEvent.REMOVED:
		case CDModelEvent.CHANGED:
			ICDElement element;
			ICDElement nextParent;
			if (type == CDModelEvent.CHANGED && event.getChangedAttribute() == null) { // swapped
																						// case
				element = (ICDElement) event.getOldValue();
				nextParent = (ICDElement) event.getChangedElement();
			} else {
				element = (ICDElement) event.getChangedElement();
				nextParent = (ICDElement) event.getParent();
			}
			if (nextParent == null)
				nextParent = element.getParent();
			while (nextParent != null && nextParent != parent) {
				element = nextParent;
				nextParent = nextParent.getParent();
			}
			if (nextParent != null && childClass.isInstance(element)) {
				markStale();
				return true;
			} else if (nextParent != null || event.getChangedElement() == parent) {
				// update validation in case there is any change in the current
				// component
				updateValidationStatus();
				return true;
			}
			break;
		case CDModelEvent.RELOADED:
			markStale();
			return true;
		case CDModelEvent.REVALIDATED:
			updateValidationStatus();
			return true;
		}
		return false;
	}

	protected void updateValidationStatus() {
		((GridData) statusLine.getLayoutData()).exclude = sectionDecoration.getStatus().isOK();
	}

	public abstract void modelModified(CDModelEvent event);
}
