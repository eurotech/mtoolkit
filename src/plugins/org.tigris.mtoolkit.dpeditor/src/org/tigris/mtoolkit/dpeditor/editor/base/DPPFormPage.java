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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.tigris.mtoolkit.dpeditor.editor.SourceFormPage;
import org.tigris.mtoolkit.dpeditor.editor.forms.Form;
import org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage;
import org.tigris.mtoolkit.dpeditor.editor.model.DPPFileModel;

/**
 * The page form that will be added into the editor.
 */
public abstract class DPPFormPage extends EditorPart implements IDPPEditorPage {

  /** The instance of the editor in which this page will be added */
  private DPPMultiPageEditor editor;
  /** The form of this page */
  private Form               form;
  /** The composite control which will be a parent of the form page */
  private Control            control;
  /** The current selection */
  private ISelection         selection;

  /**
   * Creates new instance of this form page and sets the editor in which this
   * page will be added and the title of the page.
   *
   * @param editor
   *          the parent editor
   * @param title
   *          the title of the page
   */
  public DPPFormPage(DPPMultiPageEditor editor, String title) {
    this.editor = editor;
    form = createForm();
    setPartName(title);
    setContentDescription(title);
  }

  /**
   * Creates the form, that will be exists in this page.
   *
   * @return the form of this page
   */
  protected abstract Form createForm();

  /**
   * Gets the form, which is placed in this page.
   *
   * @return the form of the page
   */
  public Form getForm() {
    return form;
  }

  /**
   * Returns the model of the editor or <code>null</code> if there are no
   * editor.
   *
   * @return the model of the editor
   */
  public Object getModel() {
    if (editor != null) {
      return getEditor().getModel();
    }
    return null;
  }

  /**
   * Gets the current selection.
   *
   * @return the current selection
   */
  public ISelection getSelection() {
    return selection;
  }

  /**
   * Sets the new selection as a current selection
   *
   * @param newSelection
   *          the selection
   */
  public void setSelection(ISelection newSelection) {
    selection = newSelection;
    getEditor().setSelection(selection);
  }

  /**
   * Asks this part to take focus within the workbench.
   *
   * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
   */
  @Override
  public void setFocus() {
    Form f = getForm();
    if (f != null) {
      getForm().setFocus();
    }
  }

  /**
   * Initializes the editor part with a site and input. Subclasses of
   * <code>DPPFormPage</code> must implement this method.
   *
   * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite,
   *      org.eclipse.ui.IEditorInput)
   */
  @Override
  public void init(IEditorSite site, IEditorInput input) throws PartInitException {
  }

  /**
   * Disposes all created in this page controls.
   *
   * @see org.eclipse.ui.part.WorkbenchPart#dispose()
   */
  @Override
  public void dispose() {
    if (form != null) {
      form.dispose();
      form = null;
    }
    if (control != null) {
      control.dispose();
      control = null;
    }

    if (getShell() != null) {
      getShell().dispose();
    }
    editor = null;
    super.dispose();
  }

  /**
   * Saves the contents of this form page.
   *
   * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public void doSave(IProgressMonitor monitor) {
  }

  /**
   * Saves the contents of the editor, which is parent of this page to another
   * object.
   *
   * @see org.eclipse.ui.part.EditorPart#doSaveAs()
   */
  @Override
  public void doSaveAs() {
  }

  /**
   * Returns whether the "save as" operation is supported by this editor. By
   * default this method returns <code>false</code>
   *
   * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
   */
  @Override
  public boolean isSaveAsAllowed() {
    return false;
  }

  /**
   * Returns whether the contents of the editor, which is parent of this page
   * have changed since the last save operation.
   *
   * @see org.eclipse.ui.part.EditorPart#isDirty()
   */
  @Override
  public boolean isDirty() {
    return false;
  }

  /**
   * Creates the SWT controls for this form page.
   *
   * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
   */
  @Override
  public void createPartControl(Composite parent) {
    control = form.createControl(parent);
    control.setMenu(editor.getContextMenu());
    form.initialize(getModel());
  }

  // IBundleEditorPage implementation

  /**
   * Creates the SWT controls of this form page - all sections for this page.
   *
   * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {
    createPartControl(parent);
  }

  /**
   * Prepares this page to become invisible and the given new page to become
   * visible, as sets all new values of the form.
   *
   * @param newPage
   *          the page that will become visible
   * @return <code>true</code> if there are no errors while the page was prepare
   *         to become invisible, <code>false</code> otherwise
   * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage#becomesInvisible(org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage)
   */
  public boolean becomesInvisible(IFormPage newPage) {
    if (getModel() instanceof DPPFileModel) {
      form.commitChanges(false);
    }
    getEditor().setSelection(new StructuredSelection());
    if (newPage instanceof SourceFormPage) {
      ((SourceFormPage) newPage).update();
    }
    return true;
  }

  /**
   * Invokes when this page is become visible and invokes for the given old page
   * method that prepare page to become invisible.
   *
   * @param oldPage
   *          the page, which will become invisible
   *
   * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage#becomesVisible(org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage)
   */
  public void becomesVisible(IFormPage oldPage) {
    update();
    setFocus();
  }

  /**
   * Returns the composite control which is a parent of this form page
   *
   * @return the page composite control
   *
   * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage#getControl()
   */
  public Control getControl() {
    return control;
  }

  /**
   * Returns the shell of the control of this page or <code>null</code> if the
   * control is not created or is disposed.
   *
   * @return the control's shell
   */
  public Shell getShell() {
    if ((control == null) || (control.isDisposed())) {
      return null;
    } else {
      return control.getShell();
    }
  }

  /**
   * Returns the title of this page form.
   *
   * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage#getLabel()
   */
  public String getLabel() {
    return getTitle();
  }

  /**
   * Returns the editor of this form page.
   *
   * @return the editor
   */
  public DPPMultiPageEditor getEditor() {
    return editor;
  }

  /**
   * Checks if the editor's current page is this one.
   *
   * @return <code>true</code> if this page is the current page of the editor,
   *         returns <code>false</code> otherwise
   *
   * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage#isVisible()
   */
  public boolean isVisible() {
    return getEditor().getCurrentPage() == this;
  }

  /**
   * Shows if this page is source page. By default this method returns
   * <code>false</code>. Returns <code>true</code> only if the page is source
   * page.
   *
   * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage#isSource()
   */
  public boolean isSource() {
    return false;
  }

  /**
   * Calls the <code>update</code> method of the registered in this form page
   * <code>Form</code>.
   *
   * @see org.tigris.mtoolkit.dpeditor.editor.base.IDPPEditorPage#update()
   */
  public void update() {
    form.update();
  }

  /**
   * Updates the document if the page is source page and if it is visible.
   */
  public void updateDocumentIfSource() {
    if (!isVisible()) {
      if (editor.getCurrentPage() instanceof SourceFormPage) {
        editor.updateDocument();
      }
    }
  }

  // Clipboard actions

  /**
   * Performs global action in the current editor page for the given identifier.
   *
   * @see org.tigris.mtoolkit.dpeditor.editor.base.IDPPEditorPage#performGlobalAction(java.lang.String)
   */
  public boolean performGlobalAction(String id) {
    if (form == null) {
      return false;
    }
    return form.doGlobalAction(id);
  }
}
