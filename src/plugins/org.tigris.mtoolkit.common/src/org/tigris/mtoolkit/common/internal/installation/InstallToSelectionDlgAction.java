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
package org.tigris.mtoolkit.common.internal.installation;

import java.lang.reflect.Constructor;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Shell;
import org.tigris.mtoolkit.common.installation.InstallationItemProcessor;
import org.tigris.mtoolkit.common.installation.TargetSelectionDialog;

public class InstallToSelectionDlgAction extends Action {
	private InstallationItemProcessor processor;
	private List items;
	private Class dlgClass;
	private IShellProvider shellProvider;

	public InstallToSelectionDlgAction(	InstallationItemProcessor processor,
										List items,
										Class dlgClass,
										IShellProvider shellProvider) {
		super("Select " + processor.getGeneralTargetName() + "...");
		this.processor = processor;
		this.items = items;
		this.dlgClass = dlgClass;
		this.shellProvider = shellProvider;
	}

	public void run() {
		try {
			Constructor constructor = dlgClass.getConstructor(new Class[] { Shell.class });
			TargetSelectionDialog dialog = (TargetSelectionDialog) constructor.newInstance(new Object[] { shellProvider.getShell() });
			if (dialog.open() == Dialog.OK) {
				new InstallToAction(processor, dialog.getSelectedTarget(), items).run();
			}
		} catch (Exception e) {
			// TODO log error
			e.printStackTrace();
		}
	}
}
