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

public interface IFormPage {

	public boolean becomesInvisible(IFormPage newPage);

	public void becomesVisible(IFormPage previousPage);

	public void createControl(Composite parent);

	public Control getControl();

	public void setEditable(boolean state);

	public String getLabel();

	public String getTitle();

	public boolean isSource();

	public boolean isVisible();

}
