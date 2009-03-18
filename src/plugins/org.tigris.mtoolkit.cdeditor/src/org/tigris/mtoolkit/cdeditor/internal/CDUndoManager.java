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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.IAction;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.tigris.mtoolkit.cdeditor.internal.model.CDModelEvent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDElement;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDModel;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDModifyListener;

/**
 * Adds Undo/Redo support for Component Description Model
 */
public class CDUndoManager implements ICDModifyListener {

	private List operationsStack = new ArrayList();

	private IAction undoAction;
	private IAction redoAction;

	private boolean ignoreRequests;

	private int cursor;

	private ICDModel model;

	// TODO: Add limit of the backward history

	/**
	 * Constructs Undo Manager
	 * @param model the Component Description Model for which Undo/Redo 
	 * 		operations will be provided
	 */
	public CDUndoManager(ICDModel model) {
		Assert.isNotNull(model);
		this.model = model;
		model.addModifyListener(this);
		initialize();
	}

	private void initialize() {
		operationsStack.clear();
		cursor = operationsStack.size() - 1;
		updateActions();
	}

	/**
	 * Determines whether Undo operation can be performed
	 * @return true if Undo operation can be performed, false otherwise
	 */
	public boolean isUndoable() {
		// if our cursor still points somewhere inside the stack
		return cursor >= 0;
	}

	/**
	 * Determines whether Redo operation can be performed
	 * @return true if Redo operation can be performed, false otherwise
	 */
	public boolean isRedoable() {
		// if there is at least one element above the cursor
		return cursor < operationsStack.size() - 1;
	}

	/**
	 * Performs Undo operation (if such operation can be performed)
	 */
	public void undo() {
		debug("Undo!");
		if (isUndoable()) {
			execute(getNextUndoElement(true), true);
			updateActions();
		}
	}

	/**
	 * Performs Redo operation (if such operation can be performed)
	 */
	public void redo() {
		debug("Redo!");
		if (isRedoable()) {
			execute(getNextRedoElement(true), false);
			updateActions();
		}
	}

	private void execute(CDModelEvent event, boolean undo) {
		ignoreRequests = true;
		debug("execute: " + event + "; isUndo: " + undo);
		try {
			switch (event.getType()) {
			case CDModelEvent.ADDED:
			case CDModelEvent.REMOVED:
				debug("execute: handler: " + event.getParent());
				((ICDElement) event.getParent()).executeOperation(event, undo);
				break;
			case CDModelEvent.CHANGED:
				debug("execute: handler: " + event.getChangedElement());
				((ICDElement) event.getChangedElement()).executeOperation(event, undo);
				break;
			}
		} finally {
			ignoreRequests = false;
		}
	}

	/** 
	 * Adds operation to the operations stack. For this operation later can be 
	 * performed undo/redo
	 * @param e the model event, which represents the operation
	 */
	public void addOperation(CDModelEvent e) {
		if (ignoreRequests) {
			debug("Ignored addOperation: " + e);
			return;
		}
		debug("addOperation: " + e);
		for (int i = cursor + 1; i < operationsStack.size(); i++) {
			operationsStack.remove(i);
		}
		operationsStack.add(e);
		cursor = operationsStack.size() - 1;
		updateActions();
	}

	/**
	 * Connects Undo and Redo actions to the manager
	 * @param undo the Undo action
	 * @param redo the Redo action
	 */
	public void connectActions(IAction undo, IAction redo) {
		this.undoAction = undo;
		this.redoAction = redo;
		updateActions();
	}

	private void updateActions() {
		if (undoAction != null) {
			CDModelEvent nextAction = getNextUndoElement(false);
			undoAction.setEnabled(nextAction != null);
			String actionText = getActionText(nextAction);
			undoAction.setText("Undo ".concat(actionText));
			debug("updateActions: undoable: " + undoAction.isEnabled() + "; text: " + undoAction.getText());
		}
		if (redoAction != null) {
			CDModelEvent nextAction = getNextRedoElement(false);
			redoAction.setEnabled(nextAction != null);
			String actionText = getActionText(nextAction);
			redoAction.setText("Redo ".concat(actionText));
			debug("updateActions: redoable: " + redoAction.isEnabled() + "; text: " + redoAction.getText());
		}
	}

	private CDModelEvent getNextUndoElement(boolean advance) {
		if (isUndoable()) {
			CDModelEvent nextEvent = (CDModelEvent) operationsStack.get(cursor);
			if (advance)
				cursor--;
			return nextEvent;
		}
		return null;
	}

	private CDModelEvent getNextRedoElement(boolean advance) {
		if (isRedoable()) {
			CDModelEvent nextEvent;
			if (advance)
				nextEvent = (CDModelEvent) operationsStack.get(++cursor);
			else
				nextEvent = (CDModelEvent) operationsStack.get(cursor + 1);
			return nextEvent;
		}
		return null;
	}

	private String getActionText(CDModelEvent event) {
		if (event == null)
			return "";
		switch (event.getType()) {
		case CDModelEvent.ADDED:
			return "Add Element";
		case CDModelEvent.REMOVED:
			return "Remove Element";
		case CDModelEvent.CHANGED:
			if (event.getChangedAttribute() == null)
				return "Swap Elements";
			else
				return "Attribute Change";
		default:
			return "";
		}
	}

	public void modelModified(CDModelEvent event) {
		if (event.getType() == CDModelEvent.RELOADED) {
			debug("modelModified: model reloaded. Flushing the undo history");
			initialize();
		} else if (event.getType() != CDModelEvent.REVALIDATED) {
			addOperation(event);
		}
	}

	public void dispose() {
		model.removeModifyListener(this);
	}

	private static final void debug(String message) {
		CDEditorPlugin.debug("[UndoManager] ".concat(message));
	}

	public void invalidateDetachedNodesOffsets() {
		for (Iterator it = operationsStack.iterator(); it.hasNext();) {
			CDModelEvent op = (CDModelEvent) it.next();
			// XXX: Reference to internal implementation classes
			Object element = op.getChangedElement();
			if (element instanceof ICDElement && ((ICDElement) element).getModel() == null && element instanceof IDocumentElementNode) {
				resetNodeOffset((IDocumentElementNode) element);
			}
		}
	}

	private void resetNodeOffset(IDocumentElementNode element) {
		element.setOffset(-1);
		element.setLength(-1);
		IDocumentAttributeNode[] attrs = element.getNodeAttributes();
		for (int i = 0; i < attrs.length; i++) {
			IDocumentAttributeNode attr = attrs[i];
			attr.setNameLength(-1);
			attr.setNameOffset(-1);
			attr.setValueLength(-1);
			attr.setValueOffset(-1);
		}
		if (element.getTextNode() != null) {
			element.getTextNode().setLength(-1);
			element.getTextNode().setOffset(-1);
		}
		for (Iterator it = element.getChildNodesList().iterator(); it.hasNext();) {
			IDocumentElementNode child = (IDocumentElementNode) it.next();
			resetNodeOffset(child);
		}
	}
}
