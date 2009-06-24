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
package org.tigris.mtoolkit.dpeditor.editor.base;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IEditorPart;
import org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage;

public interface IDPPEditorPage extends IEditorPart, IFormPage {
	boolean contextMenuAboutToShow(IMenuManager manager);

	IAction getAction(String id);

	boolean performGlobalAction(String id);

	void update();
}
