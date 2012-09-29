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

import java.io.ByteArrayOutputStream;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;
import org.tigris.mtoolkit.dpeditor.editor.base.IDPPEditorPage;
import org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage;
import org.tigris.mtoolkit.dpeditor.util.ResourceManager;
import org.tigris.mtoolkit.util.DPPUtilities;

/**
 * The standard text editor for file resources (<code>IFile</code>).
 */
public class SourceFormPage extends TextEditor implements IDPPEditorPage {

  /** Holds the title of the source page */
  public static final String PAGE_TITLE         = "DPPEditor.SourcePage.title";

  /** The editor, which source page will be this page */
  private DPPEditor          editor;
  /**
   * The <code>boolean</code> flag, that holds if the model needs to be updated
   */
  private boolean            modelNeedsUpdating = false;
  /** The parent control in which all components will be added */
  private Control            control;

  /**
   * The Document listener, which will be added to the document, which source
   * this page will be shows.
   */
  private DocumentListener   documentListener   = new DocumentListener();

  /**
   * The listener for objects which are interested in getting informed about
   * document changes. A listener is informed about document changes before they
   * are applied and after they have been applied.
   */
  class DocumentListener implements IDocumentListener {
    public void documentAboutToBeChanged(DocumentEvent e) {
    }

    public void documentChanged(DocumentEvent e) {
      if (isVisible()) {
        modelNeedsUpdating = false;
      }
    }
  }

  /*
   * Shows that this page is read only and cannot be changed from here.
   *
   * @see org.eclipse.ui.texteditor.AbstractTextEditor#isEditorInputReadOnly()
   *
   * Note: overriden to return always true because the isEditable() isn't
   * enough to guarentee the contents read-only property
   */
  @Override
  public boolean isEditorInputReadOnly() {
    return true;
  }

  /**
   * An array of Strings which contains the Action Ids and Action Definition Ids
   * for the four actions which are used by the remaining part of the Editor.
   *
   */
  private static final String[] filterActionsIds = new String[] {
      IWorkbenchActionDefinitionIds.CUT, IWorkbenchActionDefinitionIds.COPY, IWorkbenchActionDefinitionIds.PASTE,
      IWorkbenchActionDefinitionIds.SELECT_ALL, ITextEditorActionConstants.CUT, ITextEditorActionConstants.COPY,
      ITextEditorActionConstants.PASTE, ITextEditorActionConstants.SELECT_ALL,
                                                 };

  /*
   * (non-Javadoc)
   *
   * @see
   * org.eclipse.ui.texteditor.AbstractTextEditor#setAction(java.lang.String,
   * org.eclipse.jface.action.IAction)
   *
   * Note: Overriden so we can filter out the actions contained in the String
   * array filterActionsIds
   */
  @Override
  public void setAction(String actionID, IAction action) {
    for (int i = 0; i < filterActionsIds.length; i++) {
      if (actionID.equals(filterActionsIds[i])) {
        return;
      }
    }
    super.setAction(actionID, action);
  }

  /*
   * Prepares the source page to become invisible.
   *
   * @see
   * org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage#becomesInvisible(
   * org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage)
   */
  public boolean becomesInvisible(IFormPage newPage) {
    if (modelNeedsUpdating) {
      modelNeedsUpdating = false;
    }
    getSite().setSelectionProvider(getEditor());
    return true;
  }

  /*
   * Prepares this source page to become visible page.
   *
   * @see
   * org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage#becomesVisible(org
   * .tigris.mtoolkit.dpeditor.editor.forms.IFormPage)
   */
  public void becomesVisible(IFormPage oldPage) {
    modelNeedsUpdating = false;
    getSite().setSelectionProvider(getSelectionProvider());
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      DPPUtilities.debug("Internal save to source started");
      getEditor().getDPPFile().save(bos);
      IDocument document = getDocumentProvider().getDocument(getEditorInput());
      document.set(new String(bos.toByteArray()));
    } catch (Exception e) {
      e.printStackTrace();
    }
    updatePropertyDependentActions();
  }

  /*
   * Initializes the editor and source page with the given editor site and
   * input. Adds the document provider of the source page.
   *
   * @see
   * org.eclipse.ui.texteditor.AbstractTextEditor#init(org.eclipse.ui.IEditorSite
   * , org.eclipse.ui.IEditorInput)
   */
  @Override
  public void init(IEditorSite site, IEditorInput input) throws PartInitException {

    setDocumentProvider(getEditor().getDocumentProvider());
    super.init(site, input);
  }

  /*
   * Updates the source of this source page.
   *
   * @see org.tigris.mtoolkit.dpeditor.editor.base.IDPPEditorPage#update()
   */
  public void update() {
  }

  /*
   * Closes the editor, which holds this source page.
   *
   * @param save <code>boolean</code> value that signify if the changes will
   * be saved or not
   *
   * @see org.eclipse.ui.texteditor.AbstractTextEditor#close(boolean)
   */
  @Override
  public void close(boolean save) {
    editor.close(save);
  }

  /*
   * Removes added document provider and disposes all added in this page
   * elements.
   *
   * @see org.eclipse.ui.editors.text.TextEditor#dispose()
   */
  @Override
  public void dispose() {
    if (getDocumentProvider() != null) {
      IDocument document = getDocumentProvider().getDocument(getEditorInput());
      if (document != null) {
        document.removeDocumentListener(documentListener);
      }
    }
    editor = null;
    super.dispose();
  }

  /*
   * Checks whether this page can shows the context menu. Returns
   * <code>false</code>, which means that there no context menu.
   *
   * @seeorg.tigris.mtoolkit.dpeditor.editor.base.IDPPEditorPage#
   * contextMenuAboutToShow (org.eclipse.jface.action.IMenuManager)
   */
  public boolean contextMenuAboutToShow(IMenuManager manager) {
    return false;
  }

  /*
   * Returns the title of this source page.
   *
   * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage#getLabel()
   */
  public String getLabel() {
    return getTitle();
  }

  /*
   * Returns the title of this source form page.
   *
   * @see org.eclipse.ui.part.WorkbenchPart#getTitle()
   */
  @Override
  public String getTitle() {
    return ResourceManager.getString(PAGE_TITLE, "");
  }

  /*
   * Returns the composite control which is a parent of this source form page
   *
   * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage#getControl()
   */
  public Control getControl() {
    return control;
  }

  /**
   * Returns the editor, which is a parent of this source page
   *
   * @return the editor, which holds this source page
   */
  public DPPEditor getEditor() {
    return editor;
  }

  /*
   * Checks if the editor's current page is this source page.
   *
   * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage#isVisible()
   */
  public boolean isVisible() {
    return editor.getCurrentPage() == this;
  }

  /*
   * Shows if this page is source page. Returns <code>true</code>, because
   * this page is source page.
   *
   * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage#isSource()
   */
  public boolean isSource() {
    return true;
  }

  /*
   * Returns if the text in this text editor can be changed by the user. This
   * source page can not be changed by user.
   *
   * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#isEditable()
   */
  @Override
  public boolean isEditable() {
    return false;
  }

  /**
   * Creates the SWT controls of this form page. Calls
   * <code>createPartControl</code> method to create controls of this form page.
   *
   * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {
    createPartControl(parent);
  }

  /**
   * Creates the SWT controls for this form page.
   *
   * @param parent
   *          the parent control
   *
   * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#createPartControl(org.eclipse.swt.widgets.Composite)
   */
  @Override
  public void createPartControl(Composite parent) {
    super.createPartControl(parent);
    Control[] children = parent.getChildren();
    control = children[children.length - 1];
    IDocument document = getDocumentProvider().getDocument(getEditorInput());
    document.addDocumentListener(documentListener);
  }

  /**
   * Fires a property changed event.
   *
   * @param type
   *          the id of the property that changed
   *
   * @see org.eclipse.ui.texteditor.AbstractTextEditor#firePropertyChange(int)
   */
  @Override
  protected void firePropertyChange(int type) {
    if (type == PROP_DIRTY) {
      getEditor().fireSaveNeeded();
    } else {
      super.firePropertyChange(type);
    }
  }

  /**
   * Sets if this form page will be editable
   *
   * @param editable
   *          <code>boolean</code> value, that shows if this form page will be
   *          editable
   * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage#setEditable(boolean)
   */
  public void setEditable(boolean editable) {
  }

  /**
   * Performs global action in the current source editor page for the given
   * identifier.
   *
   * @see org.tigris.mtoolkit.bundles.mf.editor.base.IBundleEditorPage#performGlobalAction(java.lang.String)
   */
  public boolean performGlobalAction(String id) {
    return false;
  }
}
