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
package org.tigris.mtoolkit.cdeditor.internal;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IActionBars2;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.SubActionBars;
import org.eclipse.ui.SubActionBars2;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;

/**
 * Contributor for Eclipse actions for Component Description Editor
 */
public class ComponentDescriptionEditorContributor extends
		MultiPageEditorActionBarContributor {

	private IEditorPart fEditor;
	private SubActionBars sourceActionBars;
	private IEditorPart multiPageEditor;

	private class GlobalAction extends Action {

		public GlobalAction(String id) {
			setId(id);
		}

		public void run() {
			if (fEditor instanceof IGlobalActionHandler)
				((IGlobalActionHandler) fEditor).handleGlobalAction(getId());
			else if (multiPageEditor instanceof IGlobalActionHandler)
				((IGlobalActionHandler) multiPageEditor).handleGlobalAction(getId());
			else
				throw new IllegalStateException("Global action handler called for editor (" + fEditor + ", " + multiPageEditor + "), which cannot handle global commands");
		}
	}

	public void setActivePage(IEditorPart activeEditor) {
		if (multiPageEditor == null)
			return;
		fEditor = activeEditor;
		boolean isSourceEditor = fEditor instanceof TextEditor;
		IActionBars rootBars = getActionBars();
		rootBars.clearGlobalActionHandlers();
		if (isSourceEditor) {
			// activate the source page contributions
			sourceActionBars.activate();
			copyGlobalActionHandlers(sourceActionBars, rootBars);
		} else {
			sourceActionBars.deactivate();
			registerMainGlobalActions(rootBars);
			((ComponentDescriptionEditor) multiPageEditor).updateUndoRedo(getGlobalAction(ActionFactory.UNDO.getId()), getGlobalAction(ActionFactory.REDO.getId()));
		}
		rootBars.updateActionBars();
	}

	public void setActiveEditor(IEditorPart part) {
		multiPageEditor = part;
		super.setActiveEditor(part);
	}

	private void copyGlobalActionHandlers(SubActionBars src, IActionBars dst) {
		Map handlers = src.getGlobalActionHandlers();
		if (handlers != null) {
			Set keys = handlers.entrySet();
			for (Iterator iter = keys.iterator(); iter.hasNext();) {
				Map.Entry entry = (Entry) iter.next();
				dst.setGlobalActionHandler((String) entry.getKey(), (IAction) entry.getValue());
			}
		}
	}

	public void init(IActionBars bars) {
		super.init(bars);
		sourceActionBars = createActionBars(bars);
		registerMainGlobalActions(bars);
	}

	private SubActionBars createActionBars(IActionBars parent) {
		if (parent instanceof IActionBars2)
			return new SubActionBars2((IActionBars2) parent);
		else
			return new SubActionBars(parent);
	}

	private void registerMainGlobalActions(IActionBars bars) {
		registerGlobalAction(bars, ActionFactory.UNDO.getId());
		registerGlobalAction(bars, ActionFactory.REDO.getId());
	}

	private IAction getGlobalAction(String id) {
		return getActionBars().getGlobalActionHandler(id);
	}

	private void registerGlobalAction(IActionBars bars, String id) {
		bars.setGlobalActionHandler(id, new GlobalAction(id));
	}

	public IActionBars getSourceActionBars() {
		return sourceActionBars;
	}

}
