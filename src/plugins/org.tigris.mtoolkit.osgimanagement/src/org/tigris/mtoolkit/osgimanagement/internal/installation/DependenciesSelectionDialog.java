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

import java.util.Vector;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;

public class DependenciesSelectionDialog extends TitleAreaDialog {

	private Shell shell;
	private CheckboxTableViewer bundlesViewer;
	private Vector bundles;
	private Object[] selected;
	private Button selectAllButton;
	private Button deselectAllButton;


	public DependenciesSelectionDialog(Shell shell, Vector bundles) {
		super(shell);
		this.shell = shell;
		this.bundles = bundles;
	}
	
	protected Control createContents(Composite parent) {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(FrameWorkView.VIEW_ID);
		} catch (PartInitException e) {
			// TODO Frameworks view cannot be shown - decide what to do
			e.printStackTrace();
		}
		Control contents = super.createContents(parent);
		getShell().setText("Install additional bundles");
		setTitle("Dependencies");
		setMessage("Select bundle dependencies from the list to install", IMessageProvider.INFORMATION);
		return contents;
	}
	
	
	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite((Composite) super.createDialogArea(parent), SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);
		
		bundlesViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 150;
		gridData.widthHint = 340;
		gridData.verticalSpan = 3;
		bundlesViewer.getTable().setLayoutData(gridData);
		Composite buttonsComposite = new Composite(composite, SWT.NONE);
		buttonsComposite.setLayout(new GridLayout(3, false));
		selectAllButton = SWTFactory.createPushButton(buttonsComposite, "Select all", null);
		deselectAllButton = SWTFactory.createPushButton(buttonsComposite, "Deselect all", null);
		
		selectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				bundlesViewer.setAllChecked(true);
			}
		});
		deselectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				bundlesViewer.setAllChecked(false);
			}
		});

		
		for (int i=0; i<bundles.size(); i++) {
			bundlesViewer.add(bundles.elementAt(i));
		}
		bundlesViewer.setAllChecked(true);
		
		return composite;

	}
	
	public boolean close() {
		selected = bundlesViewer.getCheckedElements();
		return super.close();
	}

	public Object[] getSelected() {
		return selected;
	}
}
