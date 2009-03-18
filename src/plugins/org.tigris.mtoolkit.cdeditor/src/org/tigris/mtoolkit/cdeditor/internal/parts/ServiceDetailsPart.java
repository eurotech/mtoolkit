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
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.tigris.mtoolkit.cdeditor.internal.dialogs.DialogHelper;
import org.tigris.mtoolkit.cdeditor.internal.dialogs.ServiceEditDialog;
import org.tigris.mtoolkit.cdeditor.internal.model.CDModelEvent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDComponent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDInterface;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDModel;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDService;
import org.tigris.mtoolkit.cdeditor.internal.model.IEclipseContext;
import org.tigris.mtoolkit.cdeditor.internal.model.impl.CDInterface;
import org.tigris.mtoolkit.cdeditor.internal.providers.InterfacesContentProvider;
import org.tigris.mtoolkit.cdeditor.internal.providers.ServicesLabelProvider;


public class ServiceDetailsPart extends ComponentDetailsPart {

	private TableViewer interfacesViewer;
	private Text txtInterface;
	private Button checkSFactory;
	private Button btnBrowse;
	private Button btnRem;
	private Button btnAdd;
	private Button btnEdit;
	// private Button btnUp;
	// private Button btnDown;
	private Label lblInterfaces;
	private Hyperlink linkAddMore;

	private boolean singleMode = true;

	private ICDComponent component;
	private ICDModel model;

	public ServiceDetailsPart() {
		super("Provided Service", "Provided Service for the selected component", false);
	}

	protected void createSectionContents(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();

		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		parent.setLayout(layout);

		Composite serviceFactory = new Composite(parent, SWT.NONE);
		GridData gridData = new GridData();
		gridData.horizontalSpan = 3;
		serviceFactory.setLayoutData(gridData);
		FillLayout fillLayout = new FillLayout(SWT.HORIZONTAL);
		fillLayout.spacing = 0;
		fillLayout.marginHeight = 0;
		fillLayout.marginWidth = 0;
		serviceFactory.setLayout(fillLayout);
		checkSFactory = toolkit.createButton(serviceFactory, "Register provided service as service factory", SWT.CHECK);
		linkWithFormLifecycle(checkSFactory, true);
		checkSFactory.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				checkSFactory.setGrayed(false);
			}
		});

		lblInterfaces = toolkit.createLabel(parent, "Interface:", SWT.WRAP);
		lblInterfaces.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		txtInterface = toolkit.createText(parent, ""); //$NON-NLS-1$
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		txtInterface.setLayoutData(gridData);
		linkWithFormLifecycle(txtInterface, true);
		txtInterface.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				// TODO: Move this code to commit()
				updateHyperlinkAddMore();
			}
		});

		btnBrowse = toolkit.createButton(parent, "Browse...", SWT.PUSH);
		gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		gridData.exclude = true;
		btnBrowse.setLayoutData(gridData);
		btnBrowse.setVisible(false);
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IEclipseContext context = model.getProjectContext();
				if (context != null) {
					IJavaProject project = (IJavaProject) context.getAdapter(IJavaProject.class);
					if (project != null) {
						String selectedClass = DialogHelper.openBrowseTypeDialog(getManagedForm().getForm().getShell(), "Service Interface Selection", project, txtInterface.getText());
						if (selectedClass != null) {
							txtInterface.setText(selectedClass);
							commit(false);
						}
					}
				}
			}
		});

		linkAddMore = toolkit.createHyperlink(parent, "Add more interfaces...", SWT.NONE);
		gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		gridData.horizontalSpan = 3;
		gridData.exclude = true;
		linkAddMore.setLayoutData(gridData);
		linkAddMore.setVisible(false);
		linkAddMore.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				ICDInterface newInterface = handleAddService();
				if (newInterface != null) {
					addInterface(newInterface);
				}
			}
		});

		interfacesViewer = new TableViewer(toolkit.createTable(parent, SWT.NONE));
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 20;
		gridData.widthHint = 100;
		gridData.horizontalSpan = 2;
		gridData.verticalSpan = 3;
		gridData.exclude = true;
		interfacesViewer.getTable().setVisible(false);
		interfacesViewer.getTable().setLayoutData(gridData);
		interfacesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtonsState();
			}
		});
		interfacesViewer.setContentProvider(new InterfacesContentProvider());
		interfacesViewer.setLabelProvider(new ServicesLabelProvider());
		interfacesViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				if (component.getService() == null)
					return;
				int selectionIndex = interfacesViewer.getTable().getSelectionIndex();
				if (selectionIndex != -1) {
					ICDInterface oldInterface = (ICDInterface) interfacesViewer.getElementAt(selectionIndex);
					handleEditService(oldInterface);
					interfacesViewer.refresh();
				}
			}
		});
		interfacesViewer.getTable().addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				int i = interfacesViewer.getTable().getSelectionIndex();
				// this is the only way to set the dotted rectangle to the
				// element that is selected
				interfacesViewer.getTable().setSelection(i);
			}
		});
		interfacesViewer.getTable().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.DEL) {
					handleInterfaceRemove();
				}
			}
		});

		btnAdd = toolkit.createButton(parent, "Add...", SWT.PUSH);
		gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		gridData.exclude = true;
		btnAdd.setLayoutData(gridData);
		btnAdd.setEnabled(true);
		btnAdd.setVisible(false);
		btnAdd.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ICDInterface newInterface = handleAddService();
				if (newInterface != null) {
					addInterface(newInterface);
				}
			}
		});

		btnEdit = toolkit.createButton(parent, "Edit...", SWT.PUSH);
		gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		gridData.exclude = true;
		btnEdit.setLayoutData(gridData);
		btnEdit.setEnabled(false);
		btnEdit.setVisible(false);
		btnEdit.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (component.getService() == null)
					return;
				int selectionIndex = interfacesViewer.getTable().getSelectionIndex();
				if (selectionIndex != -1) {
					ICDInterface oldInterface = (ICDInterface) interfacesViewer.getElementAt(selectionIndex);
					handleEditService(oldInterface);
				}
			}
		});

		btnRem = toolkit.createButton(parent, "Remove", SWT.PUSH);
		gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		gridData.exclude = true;
		btnRem.setLayoutData(gridData);
		btnRem.setEnabled(false);
		btnRem.setVisible(false);
		btnRem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleInterfaceRemove();
			}
		});

		// btnUp = toolkit.createButton(parent, "Up", SWT.PUSH);
		// gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING |
		// GridData.HORIZONTAL_ALIGN_FILL);
		// gridData.exclude = true;
		// btnUp.setLayoutData(gridData);
		// btnUp.setEnabled(false);
		// btnUp.setVisible(false);
		// btnUp.addSelectionListener(new SelectionAdapter() {
		// public void widgetSelected(SelectionEvent e) {
		// IStructuredSelection selection = (IStructuredSelection)
		// interfacesViewer.getSelection();
		// for (Iterator it = selection.iterator(); it.hasNext();) {
		// ICDInterface cdInterface = (ICDInterface) it.next();
		// component.getService().moveUpInterface(cdInterface);
		// }
		// Object sel = selection.getFirstElement();
		// if (sel != null) {
		// interfacesViewer.reveal(sel);
		// }
		// if (interfacesViewer.getTable().getSelectionIndex() <= 0) {
		// interfacesViewer.getControl().setFocus();
		// }
		// }
		// });
		//
		// btnDown = toolkit.createButton(parent, "Down", SWT.PUSH);
		// gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING |
		// GridData.HORIZONTAL_ALIGN_FILL);
		// gridData.exclude = true;
		// btnDown.setLayoutData(gridData);
		// btnDown.setEnabled(false);
		// btnDown.setVisible(false);
		// btnDown.addSelectionListener(new SelectionAdapter() {
		// public void widgetSelected(SelectionEvent e) {
		// IStructuredSelection selection = (IStructuredSelection)
		// interfacesViewer.getSelection();
		// for (Iterator it = selection.iterator(); it.hasNext();) {
		// ICDInterface cdInterface = (ICDInterface) it.next();
		// component.getService().moveDownInterface(cdInterface);
		// }
		// Object sel = selection.getFirstElement();
		// if (sel != null) {
		// interfacesViewer.reveal(sel);
		// }
		// if (interfacesViewer.getTable().getSelectionIndex() >=
		// interfacesViewer.getTable().getItemCount() - 1) {
		// interfacesViewer.getControl().setFocus();
		// }
		// }
		// });

		model = (ICDModel) getManagedForm().getInput();
	}

	private void updateButtonsState() {
		int selected = interfacesViewer.getTable().getSelectionIndex();
		ICDInterface selectedInterface = null;
		// int totalInterfaces = 0;
		if (selected != -1 && component != null) {
			ICDInterface[] allInterfaces = (component.getService() != null ? component.getService().getInterfaces() : new ICDInterface[0]);
			selectedInterface = allInterfaces[selected];
			// totalInterfaces = allInterfaces.length;
		}
		btnEdit.setEnabled(selectedInterface != null);
		btnRem.setEnabled(selectedInterface != null);
		// btnUp.setEnabled(selected > 0);
		// btnDown.setEnabled(selected != -1 && selected < totalInterfaces - 1);
	}

	private void handleEditService(ICDInterface serviceInterface) {
		ServiceEditDialog dialog = new ServiceEditDialog(getManagedForm().getForm().getShell(), "Edit Service Interface", serviceInterface, model.getProjectContext());
		dialog.open();
	}

	private ICDInterface handleAddService() {
		ServiceEditDialog dialog = new ServiceEditDialog(getManagedForm().getForm().getShell(), "Add Service Interface", new CDInterface(), model.getProjectContext());
		int code = dialog.open();
		if (code == InputDialog.OK) {
			return dialog.getInterface();
		}
		return null;
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		if (selection.isEmpty()) {
			component = null;
		} else {
			IStructuredSelection sel = (IStructuredSelection) selection;
			Object selectedObject = sel.getFirstElement();
			if (selectedObject instanceof ICDComponent)
				component = (ICDComponent) selectedObject;
		}
		interfacesViewer.setInput(component);
		refresh();
	}

	public void commit(boolean onSave) {
		if (component != null) {
			if (isSingleMode()) { // single interface
				if (txtInterface.getText().trim().length() > 0) {
					// set single interface
					ICDInterface newInterface = new CDInterface();
					newInterface.setInterface(txtInterface.getText());
					ICDService service = component.getService();
					if (service == null) {
						component.createService(getServiceFactoryState(), newInterface);
					} else {
						ICDInterface[] interfaces = service.getInterfaces();
						if (interfaces.length == 0) {
							service.addInterface(newInterface);
						} else {
							interfaces[0].setInterface(newInterface.getInterface());
						}
						service.setServiceFactory(getServiceFactoryState());
					}
				} else {
					// remove single interface
					component.removeService();
				}
			} else { // multiple interfaces
				if (component.getService() != null) {
					component.getService().setServiceFactory(getServiceFactoryState());
				}
			}
			super.commit(onSave);
		}
	}

	private int getServiceFactoryState() {
		if (checkSFactory.getGrayed()) {
			return ICDService.SERVICE_FACTORY_UNKNOWN;
		} else if (checkSFactory.getSelection()) {
			return ICDService.SERVICE_FACTORY_YES;
		} else {
			return ICDService.SERVICE_FACTORY_NO;
		}
	}

	public void refresh() {
		beginRefresh();
		boolean needSingle = true;
		try {
			if (component != null) {
				interfacesViewer.refresh();
				ICDService service = component.getService();
				if (service != null) {
					int serviceFactory = service.getServiceFactory();
					checkSFactory.setSelection(serviceFactory == ICDService.SERVICE_FACTORY_UNKNOWN || serviceFactory == ICDService.SERVICE_FACTORY_YES);
					checkSFactory.setGrayed(serviceFactory == ICDService.SERVICE_FACTORY_UNKNOWN);
					ICDInterface[] interfaces = service.getInterfaces();
					needSingle = interfaces.length < 2;
					if (needSingle) {
						txtInterface.setText((interfaces.length > 0) ? interfaces[0].getInterface() : "");
					}
				} else {
					txtInterface.setText("");
				}
			}
		} finally {
			endRefresh();
		}
		if (isSingleMode() != needSingle) {
			setSingleMode(needSingle);
		}
		updateButtonsState();
		updateBrowseTypeButton();
		updateHyperlinkAddMore();
		updateValidationStatus();
		super.refresh();
	}

	protected void updateValidationStatus() {
		interfacesViewer.refresh();
		IStatus validationStatus = Status.OK_STATUS;
		if (component != null && component.getModel() != null)
			validationStatus = component.getModel().getAggregatedValidationStatus(component.getService())[0];
		sectionDecoration.updateStatus(validationStatus);
		super.updateValidationStatus();
	}

	public void modelModified(CDModelEvent event) {
		handleComponentChildModelChange(event, ICDService.class, component);
	}

	protected void enableButtons() {
		// TODO: Handle the case of top and bottom service
		// btnDown.setEnabled(true);
		// btnUp.setEnabled(true);
		btnRem.setEnabled(true);
		btnEdit.setEnabled(true);
	}

	protected void disableButtons() {
		// TODO: Handle the case of top and bottom service
		// btnDown.setEnabled(false);
		// btnUp.setEnabled(false);
		btnRem.setEnabled(false);
		btnEdit.setEnabled(false);
	}

	private void setSingleMode(boolean single) {
		singleMode = single;

		((GridData) lblInterfaces.getLayoutData()).horizontalSpan = single ? 1 : 3;
		lblInterfaces.setText(single ? "Interface:" : "Interfaces:");

		((GridData) txtInterface.getLayoutData()).exclude = !single;
		txtInterface.setVisible(single);

		((GridData) interfacesViewer.getTable().getLayoutData()).exclude = single;
		interfacesViewer.getTable().setVisible(!single);

		((GridData) btnAdd.getLayoutData()).exclude = single;
		btnAdd.setVisible(!single);

		((GridData) btnRem.getLayoutData()).exclude = single;
		btnRem.setVisible(!single);

		((GridData) btnEdit.getLayoutData()).exclude = single;
		btnEdit.setVisible(!single);
	}

	public boolean isSingleMode() {
		return singleMode;
	}

	private boolean canBrowse() {
		IEclipseContext context = model.getProjectContext();
		return (context != null) && (((IJavaProject) context.getAdapter(IJavaProject.class)) != null);
	}

	private void updateBrowseTypeButton() {
		if (model == null)
			return;

		if (canBrowse() && isSingleMode()) {
			btnBrowse.setVisible(true);
			((GridData) btnBrowse.getLayoutData()).exclude = false;
			((GridData) txtInterface.getLayoutData()).horizontalSpan = 1;
		} else {
			btnBrowse.setVisible(false);
			((GridData) btnBrowse.getLayoutData()).exclude = true;
			((GridData) txtInterface.getLayoutData()).horizontalSpan = 2;
		}
	}

	private void updateHyperlinkAddMore() {
		boolean showLink = isSingleMode() && component != null && txtInterface.getText().trim().length() > 0;
		((GridData) linkAddMore.getLayoutData()).exclude = !showLink;
		linkAddMore.setVisible(showLink);
	}

	private void addInterface(ICDInterface newInterface) {
		ICDService service = component.getService();
		if (service == null) {
			int serviceFactoryState = getServiceFactoryState();
			service = component.createService(serviceFactoryState, newInterface);
		} else {
			service.addInterface(newInterface);
		}
		interfacesViewer.refresh();
	}

	private void handleInterfaceRemove() {
		if (component.getService() == null)
			return;
		int[] selectionIndices = interfacesViewer.getTable().getSelectionIndices();
		if (selectionIndices.length == component.getService().getInterfaces().length) {
			component.removeService();
		} else {
			for (int i = 0; i < selectionIndices.length; i++) {
				component.getService().removeInterface(selectionIndices[i]);
			}
		}
	}
}
