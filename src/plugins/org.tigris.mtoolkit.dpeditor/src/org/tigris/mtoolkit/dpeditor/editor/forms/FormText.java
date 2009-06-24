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

import java.util.Iterator;
import java.util.Vector;

import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Text;

/**
 * This class creates the text control.
 */
public class FormText {
	/** The created text control. */
	private Text text;
	/** The value of the text field. */
	private String value;
	/**
	 * Holds the <code>boolean</code> value if the text in this text control is
	 * changed.
	 */
	private boolean dirty;
	/** Holds the list of all listeners added to this text field. */
	private Vector listeners = new Vector();
	/** Avoid the modify events if needed. */
	boolean ignoreModify = false;

	/**
	 * Constructor of the FormText. Sets the given Text field and his value.
	 * Adds the needed listeners to the given Text field.
	 * 
	 * @param text
	 *            the text field
	 */
	public FormText(Text text) {
		this.text = text;
		this.value = text.getText();
		addListeners();
	}

	/**
	 * Adds the given <code>IFormTextListener<code> into the 
	 * <code>Vector</code> with all listeners.
	 * 
	 * @param listener
	 *            the <code>IFormTextListener</code> which will be added
	 */
	public void addFormTextListener(IFormTextListener listener) {
		listeners.addElement(listener);
	}

	/**
	 * Creates and adds all needed listeners into the <code>Vector</code>, which
	 * holds all listeners, added to the Text field.
	 */
	private void addListeners() {
		text.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				keyReleaseOccured(e);
			}
		});
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				editOccured();
			}
		});
		text.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				if (dirty) {
					commit();
				}
			}
		});
	}

	/**
	 * Notifies all added <code>IFormTextListener</code>s of the text value
	 * changed event, if this is needed.
	 */
	public void commit() {
		if (dirty) {
			value = text.getText();
			// notify
			for (Iterator iter = listeners.iterator(); iter.hasNext();) {
				((IFormTextListener) iter.next()).textValueChanged(this);
			}
		}
		dirty = false;
	}

	/**
	 * Notifies all added <code>IFormTextListener</code>s to set the text dirty,
	 * only if this is needed.
	 */
	protected void editOccured(/* ModifyEvent e */) {
		if (ignoreModify)
			return;
		dirty = true;
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			((IFormTextListener) iter.next()).textDirty(this);
		}
	}

	/**
	 * Returns the text control.
	 * 
	 * @return Returns the text control
	 */
	public Text getControl() {
		return text;
	}

	/**
	 * Returns the text value which is typed in the text field.
	 * 
	 * @return the value of the text field
	 */
	public String getValue() {
		return text.getText();
	}

	/**
	 * Returns if the text field is changed or not. Returns <code>true</code> if
	 * the text in this text control is changed, <code>false</code> otherwise.
	 * 
	 * @return Returns if the text field is changed
	 */
	public boolean isDirty() {
		return dirty;
	}

	/**
	 * Commits changes in the text field if the Enter is pressed and sets the
	 * old value of the text field if the Escape is pressed.
	 * 
	 * @param e
	 *            the occurred key event
	 */
	protected void keyReleaseOccured(KeyEvent e) {
		if (e.character == '\r') {
			// commit value
			if (dirty) {
				commit();
			}
		} else if (e.character == '\u001b') { // Escape character
			text.setText(value); // restore old
			dirty = false;
		}
	}

	/**
	 * Removes the given <code>IFormTextListener</code> from the list of all
	 * listeners.
	 * 
	 * @param listener
	 *            the removed listener
	 */
	public void removeFormTextListener(IFormTextListener listener) {
		listeners.removeElement(listener);
	}

	/**
	 * Sets is this FormText changing or not.
	 * 
	 * @param newDirty
	 *            <code>true</code> if the FormText is changing, otherwise
	 *            <code>false</code>
	 */
	public void setDirty(boolean newDirty) {
		dirty = newDirty;
	}

	/**
	 * Sets provided text.
	 * 
	 * @param value
	 *            the text which will be appeared in the text field
	 */
	public void setValue(String value) {
		if (text != null && !text.getText().equals(value)) {
			text.setText(value);
		}
		this.value = value;
	}

	/**
	 * Sets provided text and sets the flag to be ignore the modification or
	 * not.
	 * 
	 * @param value
	 *            the text which will be appeared in the text field
	 * @param blockNotification
	 *            <code>boolean</code> flag that shows if the modify events are
	 *            ignored or not
	 */
	public void setValue(String value, boolean blockNotification) {
		ignoreModify = blockNotification;
		setValue(value);
		ignoreModify = false;
	}

	/**
	 * Enables the receiver if the argument is <code>true</code>, and disables
	 * it otherwise. A disabled control is typically not selectable from the
	 * user interface and draws with an inactive or "grayed" look.
	 * 
	 * @param editable
	 *            the new enabled state
	 */
	public void setEditable(boolean editable) {
		text.setEnabled(editable);
	}

	/**
	 * Causes the receiver to have the <em>keyboard focus</em>, such that all
	 * keyboard events will be delivered to it. Focus reassignment will respect
	 * applicable platform constraints.
	 * 
	 * @return <code>true</code> if the control got focus, and
	 *         <code>false</code> if it was unable to.
	 */
	public boolean setFocus() {
		return text.setFocus();
	}
}