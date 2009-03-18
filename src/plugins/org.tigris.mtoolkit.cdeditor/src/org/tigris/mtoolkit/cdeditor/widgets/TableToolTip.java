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
package org.tigris.mtoolkit.cdeditor.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

public abstract class TableToolTip {

	private class ToolTipListener implements Listener {

		public void handleEvent(Event event) {
			switch (event.type) {
			case SWT.Dispose:
			case SWT.KeyDown:
			case SWT.MouseMove:
				if (tooltipShell == null)
					break;
				tooltipShell.dispose();
				tooltipShell = null;
				break;
			case SWT.MouseHover:
				Point coords = new Point(event.x, event.y);
				TableItem item = table.getItem(coords);
				if (item != null) {
					int columns = table.getColumnCount();

					for (int i = 0; i < columns || i == 0; i++) {
						if (item.getBounds(i).contains(coords)) {
							if (tooltipShell != null && !tooltipShell.isDisposed())
								tooltipShell.dispose();
							tooltipShell = new Shell(table.getShell(), SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
							tooltipShell.setBackground(table.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
							tooltipShell.setForeground(table.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
							FillLayout layout = new FillLayout();
							layout.marginWidth = 10;
							tooltipShell.setLayout(layout);
							
							createToolTipArea(tooltipShell, item);
							
							hookRecursively(tooltipShell);
							
							tooltipShell.setData("_TABLEITEM", item);
							Point size = tooltipShell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
							Rectangle rect = item.getBounds(i);
							Point pt = table.toDisplay(rect.x, rect.y);
							tooltipShell.setBounds(pt.x, pt.y, size.x, size.y);
							tooltipShell.setVisible(true);
							break;
						}
					}
				}
				break;
			case SWT.MouseDown:
			case SWT.MouseExit:
				if (tooltipShell == null || tooltipShell.isDisposed())
					return;
				if (event.type == SWT.MouseDown) {
					// Assuming table is single select, set the selection as if
					// the mouse down event went through to the table
					Event e = new Event();
					e.item = (TableItem) tooltipShell.getData("_TABLEITEM");
					table.setSelection(new TableItem[] { (TableItem) e.item });
					table.notifyListeners(SWT.Selection, e);
					table.setFocus();
				} else {
					Rectangle rect = tooltipShell.getBounds();
					rect.x += 5;
					rect.y += 5;
					rect.width -= 10;
					rect.height -= 10;

					if (rect.contains(((Control)event.widget).toDisplay(event.x, event.y)))
						return;
				}
				tooltipShell.dispose();
				break;
			}
		}

	}
	
	private Table table;
	private Shell tooltipShell;
	private ToolTipListener tooltipListener;

	public TableToolTip(Table table) {
		this.table = table;
		tooltipListener = new ToolTipListener();
		table.addListener(SWT.Dispose, tooltipListener);
		table.addListener(SWT.KeyDown, tooltipListener);
		table.addListener(SWT.MouseMove, tooltipListener);
		table.addListener(SWT.MouseHover, tooltipListener);
	}

	public abstract void createToolTipArea(Composite parent, TableItem item);
	
	protected void adapt(Control control) {
		control.setBackground(control.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		control.setForeground(control.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
	}
	
	private void hookRecursively(Control control) {
		control.addListener(SWT.MouseExit, tooltipListener);
		control.addListener(SWT.MouseDown, tooltipListener);
		if (control instanceof Composite) {
			Control[] childs = ((Composite)control).getChildren();
			for (int i = 0; i < childs.length; i++) {
				hookRecursively(childs[i]);
			}
		}
	}
}
