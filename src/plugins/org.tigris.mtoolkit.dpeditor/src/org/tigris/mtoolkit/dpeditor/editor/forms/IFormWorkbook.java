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
package org.tigris.mtoolkit.dpeditor.editor.forms;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public interface IFormWorkbook {
	void addFormSelectionListener(IFormSelectionListener listener);

	public void addPage(IFormPage page);

	public void createControl(Composite parent);

	Control getControl();

	public IFormPage getCurrentPage();

	public IFormPage getOldPage();

	boolean isFirstPageSelected();

	void removeFormSelectionListener(IFormSelectionListener listener);

	public void removePage(IFormPage page);

	public void selectPage(final IFormPage page);

	void setFirstPageSelected(boolean selected);
}
