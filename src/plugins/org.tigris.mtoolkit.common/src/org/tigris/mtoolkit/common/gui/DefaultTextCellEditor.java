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
package org.tigris.mtoolkit.common.gui;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;

public class DefaultTextCellEditor extends TextCellEditor {

	private int fColumn;

	public DefaultTextCellEditor(TableViewer tableViewer, int column) {
		super(tableViewer.getTable());
		fColumn = column;
		text.setEditable(false);
	}

	protected void doSetValue(Object value) {
		Assert.isTrue(text != null && (value instanceof PropertyObject));
		String newText = fColumn == 0 ? ((PropertyObject) value).name : ((PropertyObject) value).value;
		super.doSetValue(newText);
	}

}
