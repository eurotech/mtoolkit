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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.tigris.mtoolkit.cdeditor.internal.CDEditorPlugin;
import org.tigris.mtoolkit.cdeditor.internal.dialogs.DialogHelper;
import org.tigris.mtoolkit.cdeditor.internal.model.CDModelEvent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDComponent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDModel;
import org.tigris.mtoolkit.cdeditor.internal.model.IEclipseContext;


public class GeneralDetailsPart extends ComponentDetailsPart {

	private Text txtCompName;
	private Text txtCompImpl;
	private Text txtCompFactory;
	private Button checkCompEnabled;
	private Button checkCompImmediate;
	private Button btnBrowse;
	private Composite compositeImpLabel;
	private Hyperlink linkCompImpl;
	private Label labelCompImpl;

	private ICDComponent component;
	private ICDModel model;

	public GeneralDetailsPart() {
		super("General", "Details for the selected component", true);
	}

	protected void createSectionContents(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();

		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		parent.setLayout(layout);

		toolkit.createLabel(parent, "Component name:*", SWT.WRAP);
		txtCompName = toolkit.createText(parent, ""); //$NON-NLS-1$
		GridData layoutData = new GridData(GridData.FILL_HORIZONTAL);
		layoutData.horizontalSpan = 2;
		txtCompName.setLayoutData(layoutData);
		linkWithFormLifecycle(txtCompName, true);

		compositeImpLabel = new Composite(parent, SWT.NONE);
		compositeImpLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		StackLayout stackLayout = new StackLayout();
		compositeImpLabel.setLayout(stackLayout);
		linkCompImpl = toolkit.createHyperlink(compositeImpLabel, "Implementation class:*", SWT.NONE);
		linkCompImpl.addHyperlinkListener(new IHyperlinkListener() {
			public void linkActivated(HyperlinkEvent e) {
				IEclipseContext context = model.getProjectContext();
				if (context != null && component.getImplementationClass() != null) {
					IType implClass = context.findBundleClass(component.getImplementationClass());
					if (implClass != null) {
						try {
							JavaUI.openInEditor(implClass, true, true);
						} catch (PartInitException e1) {
							CDEditorPlugin.log(e1);
						} catch (JavaModelException e1) {
							CDEditorPlugin.log(e1);
						}
					}
				}
			}

			public void linkEntered(HyperlinkEvent e) {
			}

			public void linkExited(HyperlinkEvent e) {
			}
		});
		labelCompImpl = toolkit.createLabel(compositeImpLabel, "Implementation class:*", SWT.WRAP);
		stackLayout.topControl = labelCompImpl;

		txtCompImpl = toolkit.createText(parent, ""); //$NON-NLS-1$
		layoutData = new GridData(GridData.FILL_HORIZONTAL);
		txtCompImpl.setLayoutData(layoutData);
		linkWithFormLifecycle(txtCompImpl, true);

		btnBrowse = toolkit.createButton(parent, "Browse...", SWT.PUSH);
		btnBrowse.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		btnBrowse.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				IEclipseContext context = model.getProjectContext();
				if (context != null) {
					IJavaProject project = (IJavaProject) context.getAdapter(IJavaProject.class);
					if (project != null) {
						String selectedClass = DialogHelper.openBrowseClassDialog(getManagedForm().getForm().getShell(), "Component Implementation Class Selection", project, txtCompImpl.getText());
						if (selectedClass != null) {
							txtCompImpl.setText(selectedClass);
							// Commit the new implementation class
							commit(false);
						}
					}
				}
			}

		});

		toolkit.createLabel(parent, "Factory name:", SWT.WRAP);
		txtCompFactory = toolkit.createText(parent, ""); //$NON-NLS-1$
		layoutData = new GridData(GridData.FILL_HORIZONTAL);
		layoutData.horizontalSpan = 2;
		txtCompFactory.setLayoutData(layoutData);
		linkWithFormLifecycle(txtCompFactory, true);

		GridLayout checkLayout = new GridLayout();
		checkLayout.marginWidth = 0;

		Composite checkButtonsComp = new Composite(parent, SWT.NONE);
		checkButtonsComp.setLayout(checkLayout);
		checkButtonsComp.setLayoutData(new GridData(SWT.NONE, SWT.BEGINNING, true, true, 3, 2));
		checkCompEnabled = toolkit.createButton(checkButtonsComp, "Enable component", SWT.CHECK);
		checkCompEnabled.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		checkCompEnabled.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// XXX: add code which will set to default value if we have
				// unrecognized value
				checkCompEnabled.setGrayed(false);
			}
		});
		linkWithFormLifecycle(checkCompEnabled, true);
		checkCompImmediate = toolkit.createButton(checkButtonsComp, "Activate component immediately upon becoming satisfied", SWT.CHECK);
		checkCompImmediate.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		checkCompImmediate.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				checkCompImmediate.setGrayed(false);
			}
		});
		linkWithFormLifecycle(checkCompImmediate, true);

		model = (ICDModel) getManagedForm().getInput();

		updateBrowseTypeButton();
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		if (selection.isEmpty()) {
			component = null;
		} else {
			IStructuredSelection sel = (IStructuredSelection) selection;
			Object selectedObject = sel.getFirstElement();
			if (selectedObject instanceof ICDComponent) {
				component = (ICDComponent) selectedObject;
			}
		}
		refresh();
	}

	public void commit(boolean onSave) {
		if (component != null) {
			String str = txtCompName.getText();
			component.setName(str);

			str = txtCompImpl.getText();
			component.setImplementationClass(str);

			str = txtCompFactory.getText();
			component.setFactory(str);

			if (checkCompEnabled.getGrayed())
				component.setEnabled(ICDComponent.ENABLED_UNKNOWN);
			else if (checkCompEnabled.getSelection())
				component.setEnabled(ICDComponent.ENABLED_YES);
			else
				component.setEnabled(ICDComponent.ENABLED_NO);

			if (checkCompImmediate.getGrayed())
				component.setImmediate(ICDComponent.IMMEDIATE_UNKNOWN);
			else if (checkCompImmediate.getSelection())
				component.setImmediate(ICDComponent.IMMEDIATE_YES);
			else
				component.setImmediate(ICDComponent.IMMEDIATE_NO);
		}
		super.commit(onSave);
	}

	public void refresh() {
		if (component != null) {
			beginRefresh();
			try {
				String str = component.getName();
				txtCompName.setText((str != null) ? str : ""); //$NON-NLS-1$

				str = component.getImplementationClass();
				txtCompImpl.setText((str != null) ? str : ""); //$NON-NLS-1$
				str = component.getFactory();
				txtCompFactory.setText((str != null) ? str : ""); //$NON-NLS-1$

				int enabled = component.getEnabled();
				checkCompEnabled.setSelection(enabled == ICDComponent.ENABLED_UNKNOWN || enabled == ICDComponent.ENABLED_YES);
				checkCompEnabled.setGrayed(enabled == ICDComponent.ENABLED_UNKNOWN);

				int immediate = component.getImmediate();
				checkCompImmediate.setSelection(immediate == ICDComponent.IMMEDIATE_UNKNOWN || immediate == ICDComponent.IMMEDIATE_YES);
				checkCompImmediate.setGrayed(immediate == ICDComponent.IMMEDIATE_UNKNOWN);
			} finally {
				endRefresh();
			}
		}
		updateValidationStatus();
		super.refresh();
	}

	protected void updateValidationStatus() {
		IStatus validationStatus = Status.OK_STATUS;
		// if the component is to be removed, everything is OK
		if (component != null && component.getModel() != null)
			// XXX: Doesn't support multiple validation statuses
			validationStatus = component.getModel().getValidationStatus(component)[0];

		sectionDecoration.updateStatus(validationStatus);
		updateImplementationClassLink();
		super.updateValidationStatus();
	}

	private void updateImplementationClassLink() {
		IEclipseContext context = model.getProjectContext();
		if (context != null && component != null && component.getImplementationClass() != null) {
			IType type = context.findBundleClass(txtCompImpl.getText());
			if (type != null) {
				((StackLayout) compositeImpLabel.getLayout()).topControl = linkCompImpl;
				compositeImpLabel.layout();
				return;
			}
		}
		((StackLayout) compositeImpLabel.getLayout()).topControl = labelCompImpl;
		compositeImpLabel.layout();
	}

	public void modelModified(CDModelEvent event) {
		if (event.getType() == CDModelEvent.CHANGED && event.getChangedElement() == component) {
			markStale();
		} else if (event.getType() == CDModelEvent.RELOADED) {
			markStale();
			updateBrowseTypeButton();
		} else if (event.getType() == CDModelEvent.REVALIDATED) {
			updateValidationStatus();
		}
	}

	private void updateBrowseTypeButton() {
		if (model == null)
			return;
		IEclipseContext newContext = model.getProjectContext();
		if (newContext == null || newContext.getAdapter(IJavaProject.class) == null) {
			btnBrowse.setVisible(false);
			((GridData) btnBrowse.getLayoutData()).exclude = true;
			((GridData) txtCompImpl.getLayoutData()).horizontalSpan = 2;
		} else {
			btnBrowse.setVisible(true);
			((GridData) btnBrowse.getLayoutData()).exclude = false;
			((GridData) txtCompImpl.getLayoutData()).horizontalSpan = 1;
		}
	}
}
