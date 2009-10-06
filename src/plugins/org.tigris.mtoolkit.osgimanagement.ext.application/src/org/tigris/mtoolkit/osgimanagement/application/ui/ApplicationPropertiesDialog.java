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
package org.tigris.mtoolkit.osgimanagement.application.ui;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.PropertiesDialog;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.PropertiesPage;

public class ApplicationPropertiesDialog extends PropertiesDialog {

	public ApplicationPropertiesDialog(Shell shell) {
		super(shell, true);
	}

	protected PropertiesPage createMainControl(Composite container) {
		PropertiesPage page = new PropertiesPage();
		page.setTitle("Application Properties");
		page.createContents(container);

		return page;
	}

	protected void attachHelp(Composite container) {
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.PROPERTY_SERVICE);
	}
}
