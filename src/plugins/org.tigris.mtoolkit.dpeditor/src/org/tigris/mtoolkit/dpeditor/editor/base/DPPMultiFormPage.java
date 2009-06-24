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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.tigris.mtoolkit.dpeditor.editor.forms.Form;
import org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage;

/**
 * The page form with a multiple pages, which will be added into the editor.
 */
public abstract class DPPMultiFormPage extends DPPFormPage {

	/** All pages that are added into this form page */
	private Vector innerPages;
	/** The composite control which will be a parent of the multiple form page */
	private Control control;
	/**
	 * The table which holds all pages of the form page and the corresponding
	 * page id
	 */
	private Hashtable table = new Hashtable();

	/**
	 * Creates the multiple form page on given editor and title.
	 * 
	 * @param editor
	 *            the editor which is corresponding of this multiple form page
	 * @param title
	 *            the title of the editor
	 */
	public DPPMultiFormPage(DPPMultiPageEditor editor, String title) {
		super(editor, title);
		setPartName(title);
		setContentDescription(title);
		innerPages = new Vector();
		createPages();
	}

	/**
	 * Creates all inner pages, which is a part of this multiple form page
	 */
	public abstract void createPages();

	/**
	 * Gets the inner page, which is corresponding with the given identifier of
	 * the searching page.
	 * 
	 * @param pageId
	 *            the page's identifier
	 * @return the page, which identifier is the given
	 */
	public IFormPage getPage(String pageId) {
		return (IFormPage) table.get(pageId);
	}

	/**
	 * Adds to this multiple form page the given page with corresponding page
	 * id.
	 * 
	 * @param id
	 *            the identifier of the added page
	 * @param page
	 *            the added page
	 */
	public void addPage(String id, IFormPage page) {
		table.put(id, page);
		innerPages.addElement(page);
	}

	/**
	 * Removes the given page from this multiple form page.
	 * 
	 * @param page
	 *            the removed page
	 */
	public void removePage(IFormPage page) {
		innerPages.removeElement(page);
	}

	/**
	 * Gets all pages, which are put in the multiple form page.
	 * 
	 * @return all pages of the multiple form page
	 */
	public Iterator getPages() {
		return innerPages.iterator();
	}

	/**
	 * Creates the SWT controls for this workbench part.
	 * 
	 * @param parent
	 *            the parent control
	 * @see org.tigris.mtoolkit.dpeditor.editor.base.DPPFormPage#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
	}

	/**
	 * Disposes of this multiple form page, as disposes all pages, that this
	 * form page contains.
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.base.DPPFormPage#dispose()
	 */
	public void dispose() {
		for (int i = 0; i < innerPages.size(); i++) {
			DPPFormPage page = (DPPFormPage) innerPages.elementAt(i);
			page.dispose();
			page = null;
		}
		super.dispose();
	}

	/**
	 * Invokes when this page is become visible and invokes for the given old
	 * page method that prepare page to become invisible.
	 * 
	 * @param oldPage
	 *            the page, which will become invisible
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.base.DPPFormPage#becomesVisible(org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage)
	 */
	public void becomesVisible(IFormPage oldPage) {
	}

	/**
	 * Creates the SWT controls for all pages that this page contains.
	 * 
	 * @param parent
	 *            the parent control
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.base.DPPFormPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		createPartControl(parent);
	}

	/**
	 * Returns the composite control which is a parent of this multiple form
	 * page
	 * 
	 * @return the page composite control
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.base.DPPFormPage#getControl()
	 */
	public Control getControl() {
		return control;
	}

	/**
	 * Creates the form of this page. Every page must be overwrite this method
	 * and to create your own form.
	 * 
	 * @return the created form
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.base.DPPFormPage#createForm()
	 */
	protected Form createForm() {
		return null;
	}

	/**
	 * Asks this part to take focus within the workbench. The first added page
	 * takes focus.
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.base.DPPFormPage#setFocus()
	 */
	public void setFocus() {
		((DPPFormPage) innerPages.elementAt(0)).getForm().setFocus();
	}

	/**
	 * Prepares this page to become invisible and the given new page to become
	 * visible.
	 * 
	 * @param newPage
	 *            the page that will become visible
	 * @return <code>true</code> if there are no errors while the page was
	 *         prepare to become invisible, <code>false</code> otherwise
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.base.DPPFormPage#becomesInvisible(org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage)
	 */
	public boolean becomesInvisible(IFormPage newPage) {
		boolean result = true;
		for (int i = 0; i < innerPages.size(); i++) {
			result &= ((DPPFormPage) innerPages.elementAt(i)).becomesInvisible(newPage);
		}
		return result;
	}

	/**
	 * Notifies the all listeners of the each inner page of this multiple form
	 * page of the new values of the components and sets that this form is
	 * changed and need to be saved.
	 * 
	 * @param onSave
	 *            <code>boolean</code> parameter to sets if the changes will be
	 *            saved or not
	 * @throws CanceledOperationException
	 *             while the operation was canceled
	 */
	public void commitFormPages(boolean onSave) throws CanceledOperationException {

		for (int i = 0; i < innerPages.size(); i++) {
			((DPPFormPage) innerPages.elementAt(i)).getForm().commitChanges(onSave);
		}

	}

	/**
	 * Updates each one of the inner pages of this multiple form page.
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.base.DPPFormPage#update()
	 */
	public void update() {
		for (int i = 0; i < innerPages.size(); i++) {
			((DPPFormPage) innerPages.elementAt(i)).update();
		}
	}
}
