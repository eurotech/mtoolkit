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
package org.tigris.mtoolkit.cdeditor.internal;

import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessage;
import org.eclipse.ui.forms.MasterDetailsBlock;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.progress.UIJob;
import org.tigris.mtoolkit.cdeditor.internal.dialogs.ComponentDialog;
import org.tigris.mtoolkit.cdeditor.internal.dialogs.PropertyEditDialog;
import org.tigris.mtoolkit.cdeditor.internal.model.CDModelEvent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDComponent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDModel;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDModifyListener;
import org.tigris.mtoolkit.cdeditor.internal.model.impl.CDComponent;
import org.tigris.mtoolkit.cdeditor.internal.providers.MasterContentProvider;
import org.tigris.mtoolkit.cdeditor.internal.providers.MasterLabelProvider;

/**
 * Master-Details block for the Design page of Component Description Editor
 */
public class ComponentsBlock extends MasterDetailsBlock {
	private MainPage page;
	private TableViewer componentsViewer;

	private Button btnRem;
	private Button btnAdd;
	private Button btnUp;
	private Button btnDown;

	private ComponentDetails componentDetails;
	private SectionPart masterSectionPart;
	private IManagedForm managedForm;

	private Object messageKey = new Object();

	private UIJob hideComponentReorderProblemJob;

	// a variable indicating the current mode of operation of the form - single
	// or multi component mode
	private boolean singleMode = false;

	/**
	 * Creates Master-Details block for the Design page of Component 
	 * Description Editor.
	 * @param page the page which contains this ComponentsBlock
	 */
	public ComponentsBlock(MainPage page) {
		this.page = page;
	}

	protected void createMasterPart(IManagedForm form, Composite parent) {
		parent.setLayout(new FillLayout());

		this.managedForm = form;
		FormToolkit toolkit = managedForm.getToolkit();

		final Section section = toolkit.createSection(parent, Section.TITLE_BAR);
		section.setText("All Components");
		section.marginWidth = 10;
		section.marginHeight = 5;
		hideComponentReorderProblemJob = new UIJob(Display.getCurrent(), "") {
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (!managedForm.getForm().isDisposed()) {
					managedForm.getMessageManager().removeMessage(messageKey);
				}
				return Status.OK_STATUS;
			}
		};

		Composite client = toolkit.createComposite(section, SWT.WRAP);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 1;
		layout.marginHeight = 1;
		client.setLayout(layout);
		toolkit.paintBordersFor(client);

		final Table componentsTable = toolkit.createTable(client, SWT.NULL);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 20;
		gridData.widthHint = 100;
		gridData.verticalSpan = 4;
		componentsTable.setLayoutData(gridData);
		componentsTable.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				int i = componentsTable.getSelectionIndex();
				// this is the only way to set the dotted rectangle to the element that is selected
				componentsTable.setSelection(i);
			}
			public void focusLost(FocusEvent e) {
			}
		});

		btnAdd = toolkit.createButton(client, "Add...", SWT.PUSH);
		gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		btnAdd.setLayoutData(gridData);
		btnAdd.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ICDComponent comp = handleAddComponent(e);
				if (comp == null) {
					return;
				}
				page.getModel().addComponent(comp);
				selectComponent(comp);
			}

			private void selectComponent(ICDComponent comp) {
				ISelection selection = new StructuredSelection(comp);
				componentsViewer.setSelection(selection);
			}
		});
		section.setClient(client);
		masterSectionPart = new SectionPart(section) {
			public void refresh() {
				super.refresh();
				setupSingleMultiMode();
				if (!singleMode) {
					componentsViewer.refresh();
					if (componentsViewer.getSelection().isEmpty() && componentsViewer.getTable().getItemCount() > 0) {
						componentsViewer.setSelection(new StructuredSelection(componentsViewer.getElementAt(0)));
					}
					updateButtons();
				}
			}
		};
		managedForm.addPart(masterSectionPart);

		btnRem = toolkit.createButton(client, "Remove", SWT.PUSH);
		gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		btnRem.setLayoutData(gridData);
		btnRem.setEnabled(false);
		btnRem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});

		btnUp = toolkit.createButton(client, "Up", SWT.PUSH);
		gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		btnUp.setLayoutData(gridData);
		btnUp.setEnabled(false);
		btnUp.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = (IStructuredSelection) componentsViewer.getSelection();
				for (Iterator it = selection.iterator(); it.hasNext();) {
					ICDComponent component = (ICDComponent) it.next();
					if (!page.getModel().canMoveComponentUp(component)) {
						showComponentReorderProblem(component, true);
						continue;
					}
					page.getModel().moveUpComponent(component);
				}
				Object sel = selection.getFirstElement();
				if (sel != null) {
					componentsViewer.reveal(sel);
				}
			}
		});

		btnDown = toolkit.createButton(client, "Down", SWT.PUSH);
		gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		btnDown.setLayoutData(gridData);
		btnDown.setEnabled(false);
		btnDown.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = (IStructuredSelection) componentsViewer.getSelection();
				for (Iterator it = selection.iterator(); it.hasNext();) {
					ICDComponent component = (ICDComponent) it.next();
					if (!page.getModel().canMoveComponentDown(component)) {
						showComponentReorderProblem(component, false);
						continue;
					}
					page.getModel().moveDownComponent(component);
				}
				Object sel = selection.getFirstElement();
				if (sel != null) {
					componentsViewer.reveal(sel);
				}
			}
		});

		componentsViewer = new TableViewer(componentsTable);
		componentsViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
				// fire selection event only in case we are in multicomponent
				// mode
				if (!singleMode)
					managedForm.fireSelectionChanged(masterSectionPart, event.getSelection());
			}

		});
		componentsViewer.getTable().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.DEL) {
					handleRemove();
				}
			}
		});
		componentsViewer.setContentProvider(new MasterContentProvider());
		componentsViewer.setLabelProvider(new MasterLabelProvider());
		ICDModel model = (ICDModel) managedForm.getInput();
		componentsViewer.setInput(model);
		model.addModifyListener(new ICDModifyListener() {
			public void modelModified(CDModelEvent event) {
				if (event.getType() == CDModelEvent.REVALIDATED) {
					componentsViewer.refresh();
					return;
				}
				masterSectionPart.markStale();
			}
		});
		updateButtons();
		componentsViewer.refresh();
	}

	private void showComponentReorderProblem(ICDComponent component, boolean up) {
		managedForm.getMessageManager().addMessage(messageKey, "Component '" + component.getName() + "' doesn't have common parent with its peer " + (up ? "above." : "below."), null, IMessage.ERROR);
		hideComponentReorderProblemJob.cancel();
		hideComponentReorderProblemJob.schedule(1500L);
	}

	private Composite findSashFormChild(Composite composite) {
		Composite current = composite;
		while (current != null && !(current.getParent() instanceof SashForm)) {
			current = current.getParent();
		}
		return current;
	}

	public void createContent(IManagedForm form) {
		super.createContent(form);
		sashForm.setWeights(new int[] { 1, 2 });
	}

	protected void createToolBarActions(IManagedForm form) {
	}

	protected void registerPages(DetailsPart part) {
		componentDetails = new ComponentDetails();
		part.registerPage(CDComponent.class, componentDetails);
	}

	private void updateButtons() {
		int selected = componentsViewer.getTable().getSelectionIndex();
		ICDComponent selectedComponent = null;
		ICDModel model = (ICDModel) managedForm.getInput();
		if (selected != -1) {
			selectedComponent = model.getComponents()[selected];
		}
		btnRem.setEnabled(selectedComponent != null);
		btnUp.setEnabled(selected > 0);
		btnDown.setEnabled(selected != -1 && selected < model.getComponents().length - 1);
	}

	private ICDComponent handleAddComponent(SelectionEvent e) {
		ComponentDialog compDialog = new ComponentDialog(managedForm.getForm().getShell(), "Add Component", getComponentsNames(page.getModel().getComponents()), page.getModel().getProjectContext());
		compDialog.open();
		int code = compDialog.getReturnCode();
		if (code == PropertyEditDialog.OK) {
			return compDialog.getData();
		}
		return null;
	}

	private void handleRemove() {
		IStructuredSelection selection = (IStructuredSelection) componentsViewer.getSelection();
		for (Iterator it = selection.iterator(); it.hasNext();) {
			ICDComponent component = (ICDComponent) it.next();
			// XXX: Model internal structure leaked
			if (component instanceof IDocumentElementNode) {
				IDocumentElementNode parent = ((IDocumentElementNode) component).getParentNode();
				if (parent != null)
					parent.removeChildNode((IDocumentElementNode) component);
			} else {
				page.getModel().removeComponent(component);
			}
		}
	}

	private String[] getComponentsNames(ICDComponent[] comps) {
		String[] result = new String[comps.length];
		for (int i = 0; i < comps.length; i++) {
			result[i] = comps[i].getName();
		}
		return result;
	}

	/**
	 * Sets the view mode (displaying Single or Multiple componets). The
	 * view mode depends on whether model contains one or more components.
	 */
	public void setupSingleMultiMode() {
		ICDModel model = (ICDModel) managedForm.getInput();
		if (model != null) {
			if (model.isSingle()) {
				if (singleMode)
					return; // the page is already correctly setup
				singleMode = true;
				// force the creation of the component details page
				managedForm.fireSelectionChanged(masterSectionPart, new StructuredSelection(model.getSingleComponent()));
				if (componentDetails != null && componentDetails.getParent() != null) {
					// find the parent composite, direct child of the SashForm
					Composite sashFormChild = findSashFormChild(componentDetails.getParent());
					if (sashFormChild != null) {
						sashForm.setMaximizedControl(sashFormChild);
						return;
					}
				}
				// we were unable to restore, mark as multi component mode
				singleMode = false;
				// We should not reach here, log a warning
				CDEditorPlugin.getDefault().getLog().log(new Status(IStatus.WARNING, CDEditorPlugin.PLUGIN_ID, "The page wasn't setup correctly because details page wasn't created"));
			} else {
				if (!singleMode)
					return; // the page is already correctly setup
				singleMode = false;
				// unmaximize the component details part
				sashForm.setMaximizedControl(null);
				managedForm.fireSelectionChanged(masterSectionPart, componentsViewer.getSelection());
			}
		}
	}
}
