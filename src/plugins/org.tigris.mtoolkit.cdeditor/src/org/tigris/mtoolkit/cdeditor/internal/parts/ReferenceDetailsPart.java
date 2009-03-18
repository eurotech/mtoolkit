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
/**
 * 
 */
package org.tigris.mtoolkit.cdeditor.internal.parts;

import java.util.Iterator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.tigris.mtoolkit.cdeditor.internal.dialogs.ReferenceAddDialog;
import org.tigris.mtoolkit.cdeditor.internal.dialogs.ReferenceEditDialog;
import org.tigris.mtoolkit.cdeditor.internal.model.CDModelEvent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDComponent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDModel;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDReference;
import org.tigris.mtoolkit.cdeditor.internal.model.impl.CDReference;
import org.tigris.mtoolkit.cdeditor.internal.model.impl.ModelUtil;
import org.tigris.mtoolkit.cdeditor.internal.providers.ReferencesContentProvider;
import org.tigris.mtoolkit.cdeditor.internal.providers.ReferencesLabelProvider;
import org.tigris.mtoolkit.cdeditor.widgets.TableToolTip;


public class ReferenceDetailsPart extends ComponentDetailsPart {

	private TableViewer referencesViewer;
	private ICDComponent component;
	private ICDModel model;

	private Button btnAdd;
	private Button btnRem;
	private Button btnProps;
	private Button btnUp;
	private Button btnDown;

	public ReferenceDetailsPart() {
		super("References", "Component References", false);
	}

	protected void createSectionContents(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		parent.setLayout(layout);

		referencesViewer = new TableViewer(toolkit.createTable(parent, SWT.NONE));

		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 100;
		gridData.widthHint = 100;
		gridData.verticalSpan = 5;
		referencesViewer.getTable().setLayoutData(gridData);
		referencesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtonsState();
			}
		});
		referencesViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				TableItem item = referencesViewer.getTable().getSelection()[0];
				if (item.getData() instanceof ICDReference) {
					ICDReference reference = (ICDReference) item.getData();
					handleReferenceProperties(reference, ReferenceEditDialog.FIELD_INTERFACE);
				}
			}

		});
		referencesViewer.setContentProvider(new ReferencesContentProvider());
		referencesViewer.setLabelProvider(new ReferencesLabelProvider());
		new ReferenceTableToolTip(referencesViewer.getTable());
		referencesViewer.getTable().addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				int i = referencesViewer.getTable().getSelectionIndex();
				// this is the only way to set the dotted rectangle to the element that is selected
				referencesViewer.getTable().setSelection(i);
			}
		});
		referencesViewer.getTable().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.DEL)
					handleReferenceRemove();
			}
		});

		btnAdd = toolkit.createButton(parent, "Add...", SWT.PUSH);
		gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		btnAdd.setLayoutData(gridData);
		btnAdd.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ICDReference ref = handleAddReference();
				if (ref != null) {
					component.addReference(ref);
				}
			}
		});

		btnProps = toolkit.createButton(parent, "Edit...", SWT.PUSH);
		gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		btnProps.setLayoutData(gridData);
		btnProps.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TableItem item = referencesViewer.getTable().getSelection()[0];
				if (item.getData() instanceof ICDReference) {
					ICDReference reference = (ICDReference) item.getData();
					handleReferenceProperties(reference, ReferenceEditDialog.FIELD_INTERFACE);
				}
			}
		});

		btnRem = toolkit.createButton(parent, "Remove", SWT.PUSH);
		gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		btnRem.setLayoutData(gridData);
		btnRem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleReferenceRemove();
			}
		});

		btnUp = toolkit.createButton(parent, "Up", SWT.PUSH);
		gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		btnUp.setLayoutData(gridData);
		btnUp.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = (IStructuredSelection) referencesViewer.getSelection();
				for (Iterator it = selection.iterator(); it.hasNext();) {
					ICDReference reference = (ICDReference) it.next();
					component.moveUpReference(reference);
				}
				Object sel = selection.getFirstElement();
				if (sel != null) {
					referencesViewer.reveal(sel);
				}
			}
		});

		btnDown = toolkit.createButton(parent, "Down", SWT.PUSH);
		gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		btnDown.setLayoutData(gridData);
		btnDown.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = (IStructuredSelection) referencesViewer.getSelection();
				for (Iterator it = selection.iterator(); it.hasNext();) {
					ICDReference reference = (ICDReference) it.next();
					component.moveDownReference(reference);
				}
				Object sel = selection.getFirstElement();
				if (sel != null) {
					referencesViewer.reveal(sel);
				}
			}
		});

		model = (ICDModel) getManagedForm().getInput();
		updateButtonsState();
	}

	private void updateButtonsState() {
		btnAdd.setEnabled(component != null);
		int selected = referencesViewer.getTable().getSelectionIndex();
		ICDReference selectedReference = null;
		int totalReferences = 0;
		if (selected != -1 && component != null) {
			ICDReference[] allReferences = component.getReferences();
			selectedReference = allReferences[selected];
			totalReferences = allReferences.length;
		}
		btnProps.setEnabled(selectedReference != null);
		btnRem.setEnabled(selectedReference != null);
		btnUp.setEnabled(selected > 0);
		btnDown.setEnabled(selected != -1 && selected < totalReferences - 1);
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		IStructuredSelection sel = (IStructuredSelection) selection;
		if (selection.isEmpty()) {
			component = null;
		} else {
			Object selectedObject = (sel.size() > 0) ? sel.getFirstElement() : null;
			if (selectedObject instanceof ICDComponent) {
				component = (ICDComponent) selectedObject;
			}
		}
		referencesViewer.setInput(component);
		refresh();
	}

	public void commit(boolean onSave) {
		super.commit(onSave);
	}

	public void refresh() {
		referencesViewer.refresh();
		updateValidationStatus();
		updateButtonsState();
		super.refresh();
	}

	protected void updateValidationStatus() {
		referencesViewer.refresh();
		IStatus validationStatus = Status.OK_STATUS;
		if (component != null && component.getModel() != null) {
			ICDReference[] references = component.getReferences();
			for (int i = 0; i < references.length; i++) {
				IStatus newValidationStatus = component.getModel().getAggregatedValidationStatus(references[i])[0];
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

	private ICDReference handleAddReference() {
		// TODO: Use factory to create model objects
		ReferenceAddDialog refDialog = new ReferenceAddDialog(getManagedForm().getForm().getShell(), "Add Reference", new CDReference(), model.getProjectContext());
		int code = refDialog.open();
		if (code == Window.OK) {
			return refDialog.getReference();
		}
		return null;
	}

	private void handleReferenceProperties(ICDReference reference, String focusDescription) {
		ReferenceEditDialog refPropsDialog = new ReferenceEditDialog(getManagedForm().getForm().getShell(), "Reference Properties", reference, focusDescription, model.getProjectContext());
		refPropsDialog.open();
	}

	public void modelModified(CDModelEvent event) {
		handleComponentChildModelChange(event, ICDReference.class, component);
	}

	private void handleReferenceRemove() {
		IStructuredSelection selection = (IStructuredSelection) referencesViewer.getSelection();
		for (Iterator it = selection.iterator(); it.hasNext();) {
			ICDReference reference = (ICDReference) it.next();
			component.removeReference(reference);
		}
	}

	private class ReferenceTableToolTip extends TableToolTip {

		public ReferenceTableToolTip(Table table) {
			super(table);
		}

		public void createToolTipArea(Composite parent, TableItem item) {
			if (!(item.getData() instanceof ICDReference)) {
				return;
			}
			StyledText text = new StyledText(parent, SWT.WRAP | SWT.READ_ONLY | SWT.MULTI);
			adapt(text);
			ICDReference ref = (ICDReference) item.getData();
			StyledString refString = getString(ref);
			text.setText(refString.getString());
			text.setStyleRanges(refString.getStyleRanges());
		}

		private StyledString getString(ICDReference ref) {
			StyledString string = new StyledString();
			string.append(ref.getInterface(), boldStyler).append("\n\t").append(ref.getName()).append("\n\n");
			appendProperty(string, "Cardinality", ModelUtil.printEnumerateValue(ref.getCardinality(), ICDReference.CARDINALITY_NAMES_SHORT));
			appendProperty(string, "Policy", ModelUtil.printEnumerateValue(ref.getPolicy(), ICDReference.POLICY_NAMES));
			appendProperty(string, "Target", ref.getTarget());
			appendProperty(string, "Bind Method", ref.getBind());
			appendProperty(string, "Unbind Method", ref.getUnbind());
			return string;
		}

		private void appendProperty(StyledString s, String propertyName, String propertyValue) {
			s.append("\t" + propertyName + ": ", StyledString.QUALIFIER_STYLER);
			if (propertyValue == null || propertyValue.length() == 0)
				// s.append("(not defined)", StyledString.QUALIFIER_STYLER);
				;
			else
				s.append(propertyValue);
			s.append("\n");
		}

		private final Styler boldStyler = new Styler() {
			public void applyStyles(TextStyle textStyle) {
				if (textStyle instanceof StyleRange)
					((StyleRange) textStyle).fontStyle = SWT.BOLD;
			}
		};
	}
}