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
package org.tigris.mtoolkit.dpeditor.editor;

import org.eclipse.core.resources.IFile;
import org.tigris.mtoolkit.dpeditor.editor.base.DPPFormPage;
import org.tigris.mtoolkit.dpeditor.editor.forms.Form;

/**
 * The page form that will be added into the editor. Represents the bundles page
 * in the Deployment package editor.
 */
public class BundlesFormPage extends DPPFormPage {

	/**
	 * The selected IFile, which is the deployment package file, which
	 * components the editor will be shows.
	 */
	private IFile dppFile;

	/**
	 * Creates new instance of this form page and sets the editor in which this
	 * page will be added and the title of the page. Creates your own form and
	 * added in it form sections.
	 * 
	 * @param editor
	 *            the parent editor
	 * @param title
	 *            the title of the page
	 */
	public BundlesFormPage(DPPEditor editor, String title) {
		super(editor, title);
	}

	/**
	 * Creates the bundles form, that will be exists in this page.
	 * 
	 * @return the form of this page
	 * @see org.tigris.mtoolkit.dpeditor.editor.base.DPPFormPage#createForm()
	 */
	protected Form createForm() {
		return new BundlesForm(this);
	}

	/**
	 * Sets if the form, that this page is contained will be editable, depends
	 * on the given <code>boolean</code> value.
	 * 
	 * @param editable
	 *            <code>boolean</code> value, that shows if the form will be
	 *            editable
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage#setEditable(boolean)
	 */
	public void setEditable(boolean editable) {
		((BundlesForm) getForm()).setEditable(editable);
	}

	/**
	 * Sets the <code>IFile</code> that is the deployment package file
	 * 
	 * @param file
	 *            the <code>IFile</code> that will be shown in the editor
	 */
	public void setDPPFile(IFile file) {
		dppFile = file;
	}

	/**
	 * Returns the <code>IFile</code> that this form page holds.
	 * 
	 * @return the <code>IFile</code> that this editor is showing
	 */
	public IFile getDPPFile() {
		return dppFile;
	}
}
