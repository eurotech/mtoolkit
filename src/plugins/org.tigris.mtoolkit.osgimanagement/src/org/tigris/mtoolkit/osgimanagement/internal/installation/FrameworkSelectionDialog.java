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
package org.tigris.mtoolkit.osgimanagement.internal.installation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.common.installation.InstallationTarget;
import org.tigris.mtoolkit.common.installation.TargetSelectionDialog;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ContentChangeEvent;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ContentChangeListener;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.TreeRoot;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.MenuFactory;
import org.tigris.mtoolkit.osgimanagement.internal.images.ImageHolder;

public class FrameworkSelectionDialog extends TargetSelectionDialog {
	private Button btnAdd;
	private Button btnEdit;
	private Button btnRem;
	private TableViewer frameworkViewer;
	private InstallationTarget selected = null;
	private Shell shell;

	public FrameworkSelectionDialog(Shell shell) {
		super(shell);
		this.shell = shell;
	}

	protected Control createContents(Composite parent) {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(FrameWorkView.VIEW_ID);
		} catch (PartInitException e) {
			// TODO Frameworks view cannot be shown - decide what to do
			e.printStackTrace();
		}
		Control contents = super.createContents(parent);
		getShell().setText("Install to");
		setTitle("OSGi Framework");
		setMessage("Select OSGi Framework from the list", IMessageProvider.INFORMATION);

		updateButtonsState();

		return contents;
	}

	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite((Composite) super.createDialogArea(parent), SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);

		frameworkViewer = new TableViewer(new Table(composite, SWT.SINGLE | SWT.BORDER));
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 150;
		gridData.widthHint = 340;
		gridData.verticalSpan = 3;
		frameworkViewer.getTable().setLayoutData(gridData);
		// frameworkViewer.getTable().setLinesVisible(true);
		frameworkViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtonsState();
			}
		});
		frameworkViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				close();
			}
		});
		frameworkViewer.setContentProvider(new FrameworkContentProvider());
		frameworkViewer.setLabelProvider(new FrameworkLabelProvider());
		frameworkViewer.getTable().addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				int i = frameworkViewer.getTable().getSelectionIndex();
				// setting the dotted rectangle to the element that is selected
				frameworkViewer.getTable().setSelection(i);
			}
		});
		frameworkViewer.getTable().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.DEL) {
					handleFrameworkRemove();
				}
			}
		});

		frameworkViewer.setInput(FrameWorkView.getTreeRoot());

		btnAdd = new Button(composite, SWT.PUSH);
		gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		gridData.widthHint = 72;
		btnAdd.setLayoutData(gridData);
		btnAdd.setText("Add...");
		btnAdd.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleFrameworkAdd();
			}
		});

		btnEdit = new Button(composite, SWT.PUSH);
		gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		btnEdit.setLayoutData(gridData);
		btnEdit.setText("Edit...");
		btnEdit.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleFrameworkEdit();
			}
		});

		btnRem = new Button(composite, SWT.PUSH);
		gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		btnRem.setLayoutData(gridData);
		btnRem.setText("Remove");
		btnRem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleFrameworkRemove();
			}
		});

		return composite;
	}

	public boolean close() {
		IStructuredSelection selection = (IStructuredSelection) frameworkViewer.getSelection();
		Object element = selection.getFirstElement();
		if (element instanceof FrameWork) {
			selected = new FrameworkTarget((FrameWork) element);
		}

		return super.close();
	}

	private void updateButtonsState() {
		int selected = frameworkViewer.getTable().getSelectionIndex();

		btnEdit.setEnabled(selected != -1);
		btnRem.setEnabled(selected != -1 &&
				!((FrameWork)frameworkViewer.getTable().getSelection()[0].getData()).autoConnected);

		Button btnOK = getButton(IDialogConstants.OK_ID);
		if (btnOK != null) {
			btnOK.setEnabled(selected != -1);
		}
	}

	private void handleFrameworkAdd() {
		MenuFactory.addFrameworkAction(FrameWorkView.getTreeRoot(), FrameWorkView.tree);
	}

	private void handleFrameworkRemove() {
		IStructuredSelection selection = (IStructuredSelection) frameworkViewer.getSelection();
		Object element = selection.getFirstElement();
		if (element instanceof FrameWork) {
			FrameWork framework = (FrameWork) element;
			if (!framework.autoConnected) {
				if (framework.isConnected()) {
					MenuFactory.disconnectFrameworkAction(framework);
				}
				MenuFactory.removeFrameworkAction(framework);
			}
		}
	}

	private void handleFrameworkEdit() {
		IStructuredSelection selection = (IStructuredSelection) frameworkViewer.getSelection();
		Object element = selection.getFirstElement();
		if (element instanceof FrameWork) {
			FrameWork framework = (FrameWork) element;
			MenuFactory.frameworkPropertiesAction(framework, FrameWorkView.tree);
		}
	}

	protected class FrameworkContentProvider implements IStructuredContentProvider, ContentChangeListener {
		private Viewer viewer;

		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof TreeRoot) {
				TreeRoot treeRoot = (TreeRoot) inputElement;
				Object[] elements = treeRoot.getChildren();
				List frameworks = new ArrayList();
				for (int i = 0; i < elements.length; i++) {
					if (elements[i] instanceof FrameWork) {
						frameworks.add(elements[i]);
					}
				}
				return (Object[]) frameworks.toArray(new Object[frameworks.size()]);
			}
			return null;
		}

		public void dispose() {
			Object input;
			if (viewer != null && (input = viewer.getInput()) instanceof TreeRoot) {
				((TreeRoot) input).removeListener(this);
			}
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			this.viewer = viewer;
			if (oldInput instanceof TreeRoot) {
				((TreeRoot) oldInput).removeListener(this);
			}
			if (newInput instanceof TreeRoot) {
				((TreeRoot) newInput).addListener(this);
			}
		}

		public void elementAdded(final ContentChangeEvent event) {
			if (viewer instanceof TableViewer && event.getTarget() instanceof FrameWork) {
				shell.getDisplay().asyncExec(new Runnable() {
					public void run() {
						((TableViewer) viewer).add(event.getTarget());
					}
				});
			}
		}

		public void elementChanged(final ContentChangeEvent event) {
			if (viewer instanceof TableViewer && event.getTarget() instanceof FrameWork) {
				shell.getDisplay().asyncExec(new Runnable() {
					public void run() {
						((TableViewer) viewer).update(event.getTarget(), null);
					}
				});
			}
		}

		public void elementRemoved(final ContentChangeEvent event) {
			if (viewer instanceof TableViewer && event.getTarget() instanceof FrameWork) {
				shell.getDisplay().asyncExec(new Runnable() {
					public void run() {
						((TableViewer) viewer).remove(event.getTarget());
						// TableViewer.remove() doesn't call selection change
						// listeners
						updateButtonsState();
					}
				});
			}
		}
	}

	protected class FrameworkLabelProvider extends LabelProvider {
		public Image getImage(Object element) {
			if (element instanceof FrameWork) {
				FrameWork framework = (FrameWork) element;
				if (framework.isConnected()) {
					return ImageHolder.getImage(ConstantsDistributor.SERVER_ICON_CONNECTED);
				} else {
					return ImageHolder.getImage(ConstantsDistributor.SERVER_ICON_DISCONNECTED);
				}
			}
			return null;
		}

		public String getText(Object element) {
			if (element instanceof FrameWork) {
				FrameWork framework = (FrameWork) element;
				return framework.getName();
			}
			return super.getText(element);
		}
	}

	public InstallationTarget getSelectedTarget() {
		return selected;
	}
}
