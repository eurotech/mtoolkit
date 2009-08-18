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
package org.tigris.mtoolkit.osgimanagement;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

public class ToolbarIMenuCreator extends Action implements IMenuCreator {

	private Action actions[];
	private TreeViewer tree;

	public ToolbarIMenuCreator(Action actions[], TreeViewer tree) {
		this.actions = actions;
		this.tree = tree;
		setMenuCreator(this);
	}

	public void dispose() {

	}

	public int getStyle() {
		return IAction.AS_DROP_DOWN_MENU;
	}

	public void run() {
	}

	public Menu getMenu(Control parent) {
		Menu newmenu = new Menu(parent);
		newmenu.addMenuListener(new MenuListener() {

			public void menuHidden(MenuEvent e) {
			}

			public void menuShown(MenuEvent e) {
				IStructuredSelection selection = (IStructuredSelection) tree.getSelection();
				for (int i=0; i<actions.length; i++) {
					if (actions[i] instanceof IStateAction) {
						((IStateAction)actions[i]).updateState(selection);
					}
				}
			}
		});
		for (int i = 0; i < actions.length; i++) {
			ActionContributionItem item = new ActionContributionItem(actions[i]);
			item.fill(newmenu, -1);
		}
		return newmenu;
	}

	public Menu getMenu(Menu parent) {
		Menu newmenu = new Menu(parent);
		for (int i = 0; i < actions.length; i++) {
			ActionContributionItem item = new ActionContributionItem(actions[i]);
			item.fill(newmenu, -1);
		}
		return newmenu;
	}
}

