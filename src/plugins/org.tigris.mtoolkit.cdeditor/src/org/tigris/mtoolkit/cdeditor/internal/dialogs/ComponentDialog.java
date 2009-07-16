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
package org.tigris.mtoolkit.cdeditor.internal.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDComponent;
import org.tigris.mtoolkit.cdeditor.internal.model.IEclipseContext;
import org.tigris.mtoolkit.cdeditor.internal.model.Validator;
import org.tigris.mtoolkit.cdeditor.internal.model.impl.CDComponent;
import org.tigris.mtoolkit.cdeditor.internal.widgets.CreateComponentGroup;
import org.tigris.mtoolkit.common.gui.StatusLineDialog;

/**
 * A ComponentDialog is dialog that allows maintaining Declarative Services 
 * Component.
 */
public class ComponentDialog extends StatusLineDialog implements
		CreateComponentGroup.InputValidator {

	private ICDComponent comp = null;
	private IEclipseContext context = null;
	private String[] comps;

	private CreateComponentGroup group;

	/**
	 * Creates Component Dialog.
	 * @param shell the parent shell
	 * @param title the dialog title
	 * @param comps array of existing components
	 * @param context the eclipse context
	 */
	public ComponentDialog(Shell shell, String title, String[] comps,
			IEclipseContext context) {
		super(shell, title);
		this.comps = comps;
		this.context = context;
	}

	protected Control createDialogArea(Composite parent) {
		// delegate the setup of the composite to the super implementation
		final Composite composite = (Composite) super.createDialogArea(parent);
		group = new CreateComponentGroup(this, context);
		group.setMinimalFieldWidth(convertHorizontalDLUsToPixels(ICDDialogConstants.MINIMAL_ENTRY_WIDTH));
		Control groupComposite = group.createContents(composite);
		groupComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		return composite;
	}

	public void validate(CreateComponentGroup validatedGroup) {
		updateStatus(Validator.validateComponent(validatedGroup.getComponentName(), validatedGroup.getComponentClass(), comps));
	}

	protected void commit() {
		// TODO: should use factory object instead
		comp = new CDComponent();
		group.commitGroup(comp);
	}

	protected void refresh() {
		validate(group);
	}

	public ICDComponent getData() {
		return comp;
	}

}
