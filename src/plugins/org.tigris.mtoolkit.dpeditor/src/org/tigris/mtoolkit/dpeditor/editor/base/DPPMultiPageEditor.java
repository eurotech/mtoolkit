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

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.editors.text.StorageDocumentProvider;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.tigris.mtoolkit.dpeditor.editor.DPPEditor;
import org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage;
import org.tigris.mtoolkit.dpeditor.editor.forms.IFormSelectionListener;
import org.tigris.mtoolkit.dpeditor.editor.forms.IFormWorkbook;
import org.tigris.mtoolkit.dpeditor.editor.model.DPPFileModel;
import org.tigris.mtoolkit.dpeditor.editor.model.IModelChangedEvent;
import org.tigris.mtoolkit.dpeditor.editor.model.IModelChangedListener;
import org.tigris.mtoolkit.dpeditor.util.DPPErrorHandler;
import org.tigris.mtoolkit.dpeditor.util.ResourceManager;
import org.tigris.mtoolkit.util.InconsistentDataException;

/**
 * The Editor class extends the abstract base implementation of all workbench
 * editors.
 */
public abstract class DPPMultiPageEditor extends EditorPart implements ShellListener, ISelectionProvider,
    IFormSelectionListener, IModelChangedListener {

  /** Holds warning label */
  public static String        WARNING                 = ResourceManager.getString("DPPEditor.Warning", "");
  /** Holds ignore warning message */
  public static String        IGNORE_WARNING_QUESTION = ResourceManager.getString("DPPEditor.CloseEditorWarning", "");
  /** Holds the read only error message */
  private static final String RO_ERROR                = ResourceManager.getString("DPPEditor.Read_Only_Error", "");

  /** The form work's control */
  protected IFormWorkbook     formWorkbook;
  /** Holds the identifier of the first page in the editor */
  protected String            firstPageId;
  /** Holds the model of the object which will be opened in this editor */
  protected Object            model;                                                                                  // Must be DPPFile
  /** The file representation of the object will be opened in this editor */
  protected IFile             file;

  /**
   * The table which holds the page of the editor and the corresponding page id
   */
  private Hashtable           table                   = new Hashtable();
  /** Holds the pages of the editor */
  private Vector              pages;
  /** The context menu of the editor */
  private Menu                contextMenu;
  /** The selection provider */
  private SelectionProvider   selectionProvider       = new SelectionProvider();
  /** The document provider */
  private IDocumentProvider   documentProvider;
  /**
   * The <code>Clipboard</code>, which provides a mechanism for transferring
   * data from one application to another
   */
  private Clipboard           clipboard;
  /** The composite control which will be the parent of the editor */
  private Composite           parent;
  /** The active shell where shell listener is added */
  private Shell               theShell;

  /**
   * Returns the identifier of the source page of this editor.
   *
   * @return source's page identifier
   */
  protected abstract String getSourcePageId();

  /**
   * Creates all pages of this editor.
   */
  protected abstract void createPages();

  /**
   * Checks correctness of the entered data in the editor.
   *
   * @throws InconsistentDataException
   *           when the data is not correct
   */
  protected abstract void checkCorrectness() throws InconsistentDataException;

  /**
   * Creates the model from the given input Object.
   *
   * @param input
   *          the Object, which model will be created
   * @return the created model
   */
  protected abstract Object createModel(Object input);

  /**
   * Updates the model of this editor.
   *
   * @return <code>true</code> if the model is successfully updated,
   *         <code>false</code> otherwise
   */
  protected abstract boolean updateModel();

  /**
   * Checks if the given object is the instance of the model that editor works
   * with and if this model is changing
   *
   * @param model
   *          the model which will be checked
   * @return <code>true</code> if the model is changed, otherwise
   *         <code>false</code>
   */
  protected abstract boolean isModelDirty(Object model);

  /**
   * Creates the all editor pages and work's control.
   */
  public DPPMultiPageEditor() {
    formWorkbook = new CustomWorkbook();
    formWorkbook.setFirstPageSelected(false);
    pages = new Vector();
    createPages();
  }

  /**
   * Adds to the editor the given editor page with its id.
   *
   * @param id
   *          the page's identifier
   * @param page
   *          the editor page
   */
  public void addPage(String id, IDPPEditorPage page) {
    table.put(id, page);
    pages.addElement(page);
  }

  /**
   * Removes the given editor page from the editor.
   *
   * @param page
   *          the editor page will be removed
   */
  public void removePage(IDPPEditorPage page) { // NO_UCD
    formWorkbook.removePage(page);
    pages.removeElement(page);
  }

  /**
   * Returns the editor page, which corresponding with the given identifier.
   *
   * @param pageId
   *          the identifier of the page
   * @return the page, corresponding with this id
   */
  public IDPPEditorPage getPage(String pageId) {
    return (IDPPEditorPage) table.get(pageId);
  }

  /**
   * Returns the current editor page.
   *
   * @return current page
   */
  public IDPPEditorPage getCurrentPage() {
    if (formWorkbook == null) {
      return null;
    }
    return (IDPPEditorPage) formWorkbook.getCurrentPage();
  }

  /**
   * Gets all pages, which this editor has.
   *
   * @return the <code>Iterator</code> of all editor's pages
   */
  public Iterator getPages() {
    return pages.iterator();
  }

  /**
   * Returns the context menu of this editor.
   *
   * @return the context menu of the editor
   */
  public Menu getContextMenu() {
    return contextMenu;
  }

  /**
   * Returns the object, which is opened in this editor
   *
   * @return the model, which is opened in the editor
   */
  public Object getModel() {
    return model;
  }

  /**
   * Creates the SWT controls for this workbench part.
   *
   * @param parent
   *          the parent control
   *
   * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
   */
  @Override
  public void createPartControl(Composite parent) {
    this.parent = parent;
    formWorkbook.createControl(parent);
    formWorkbook.addFormSelectionListener(this);

    MenuManager manager = new MenuManager();
    manager.setRemoveAllWhenShown(true);
    contextMenu = manager.createContextMenu(formWorkbook.getControl());

    for (Iterator iter = pages.iterator(); iter.hasNext();) {
      IFormPage page = (IFormPage) iter.next();
      formWorkbook.addPage(page);
    }

    firstPageId = getSourcePageId();
    if (firstPageId != null) {
      showPage(firstPageId);
    }

    theShell = parent.getShell();
    theShell.addShellListener(this);
  }

  /**
   * Initializes the editor part with a site and input. Examines the editor
   * input object type to determine if it is understood. If not, the implementor
   * must throw a <code>PartInitException</code>.
   *
   * @param site
   *          the editor site
   * @param input
   *          the editor input
   * @exception PartInitException
   *              if this editor was not initialized successfully
   *
   * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite,
   *      org.eclipse.ui.IEditorInput)
   */
  @Override
  public void init(IEditorSite site, IEditorInput input) throws PartInitException {
    setSite(site);
    setInput(input);
    Object inputObject = null;
    if (input instanceof FileEditorInput) {
      inputObject = input.getAdapter(IFile.class);
    } else if (input instanceof IStorageEditorInput) {
      inputObject = input;
      setPartName(input.getName());
      setContentDescription(input.getName());
    }

    site.setSelectionProvider(this);

    if (inputObject instanceof IFile) {
      IFile iFile = (IFile) inputObject;
      IPath location = iFile.getLocation();
      File ff = null;
      if (location != null) {
        ff = new File(location.toOSString());
      }
      if (ff == null || !ff.exists()) {
        pages.removeAllElements();
        table.clear();
        close(false);
        return;
      } else {
        file = iFile;
      }
      setPartName(iFile.getName());
    }
    initializeModels(inputObject);
    for (Iterator iter = pages.iterator(); iter.hasNext();) {
      IEditorPart part = (IEditorPart) iter.next();
      part.init(site, input);
    }
  }

  /**
   * Initializes the editor model with a given input. Creates the editor model
   * if this is needed. Examines the editor input object type to determine if it
   * is understood.
   *
   * @param input
   *          the editor input
   */
  protected void initializeModels(Object input) {
    documentProvider = createDocumentProvider(input);
    if (documentProvider == null) {
      return;
    }
    // create document provider
    model = createModel(input);
    if (model instanceof DPPFileModel) {
      ((DPPFileModel) model).addModelChangedListener(this);
    }

    try {
      IEditorInput editorInput = getEditorInput();
      documentProvider.connect(editorInput);
      IAnnotationModel amodel = documentProvider.getAnnotationModel(editorInput);
      if (amodel != null) {
        amodel.connect(documentProvider.getDocument(editorInput));
      }
    } catch (CoreException e) {
      DPPErrorHandler.processError(e, false);
    }
  }

  /**
   * Gets the document provider.
   *
   * @return the document provider of this editor
   */
  public IDocumentProvider getDocumentProvider() {
    return documentProvider;
  }

  /**
   * Creates the document provider for the given editor input.
   *
   * @param input
   *          the editor input
   * @return the created document provider
   */
  protected IDocumentProvider createDocumentProvider(Object input) {
    IDocumentProvider documentProvider = null;
    if (input instanceof IFile) {
      documentProvider = new FileDocumentProvider() {
        @Override
        public IDocument createDocument(Object element) throws CoreException {
          IDocument document = super.createDocument(element);
          return document;
        }
      };
    } else if (input instanceof IStorageEditorInput) {
      documentProvider = new StorageDocumentProvider() {
        @Override
        public IDocument createDocument(Object element) throws CoreException {
          IDocument document = super.createDocument(element);
          return document;
        }
      };
    }
    return documentProvider;
  }

  /**
   * The implementation of the <code>IWorkbenchPart</code> method disposes the
   * title image and all created in this editor elements.
   *
   * @see org.eclipse.ui.part.WorkbenchPart#dispose()
   */
  @Override
  public void dispose() {
    setSelection(new StructuredSelection());
    for (int i = 0; i < pages.size(); i++) {
      IWorkbenchPart part = (IWorkbenchPart) pages.elementAt(i);
      part.dispose();
      part = null;
    }
    IEditorInput input = getEditorInput();
    if (documentProvider != null) {
      IAnnotationModel amodel = documentProvider.getAnnotationModel(input);
      if (amodel != null) {
        amodel.disconnect(documentProvider.getDocument(input));
      }
      documentProvider.disconnect(input);
    }
    formWorkbook.removeFormSelectionListener(this);
    documentProvider = null;
    formWorkbook = null;
    if (!theShell.isDisposed()) {
      theShell.removeShellListener(this);
    }
    selectionProvider = null;
    Vector dppEditors = DPPEditor.getDPPEditors(file);
    if ((dppEditors == null) || (dppEditors.size() == 0)) {
      model = null;
    }
    if (clipboard != null) {
      clipboard.dispose();
      clipboard = null;
    }
  }

  /**
   * Returns the clip board.
   *
   * @return the clip board
   */
  public Clipboard getClipboard() {
    return clipboard;
  }

  /**
   * Updates the document, which is corresponding with the editor's document
   * provider.
   */
  public void updateDocument(boolean throwEx) throws Exception {
    // if model is dirty, flush its content into
    // the document so that the source editor will
    // pick up the changes.
    DPPFileModel modelBase = (DPPFileModel) model;
    if (modelBase.isDirty() == false && !getCurrentPage().isSource()) {
      return;
    }
    try {
      // need to update the document
      IDocument document = documentProvider.getDocument(getEditorInput());
      StringWriter swriter = new StringWriter();
      PrintWriter writer = new PrintWriter(swriter);
      modelBase.save(writer);
      swriter.close();
      document.set(swriter.toString());
      modelBase.setDirty(false);
    } catch (Exception e) {
      DPPErrorHandler.processError(e, true);
      if (throwEx) {
        throw e;
      }
    }
  }

  /**
   * Updates the document, which is corresponding with the editor's document
   * provider.
   */
  public void updateDocument() {
    try {
      updateDocument(false);
    } catch (Exception e) {
      DPPErrorHandler.processError(e);
    }
  }

  /**
   * Saves the contents of this editor to another object.
   *
   * @see org.eclipse.ui.part.EditorPart#doSaveAs()
   */
  @Override
  public void doSaveAs() {
    getCurrentPage().doSaveAs();
  }

  /**
   * Returns whether the "Save As" operation is supported by this editor. By
   * default the "Save As" is not supported.
   *
   * @return <code>true</code> if "Save As" is supported, and <code>false</code>
   *         if not supported
   * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
   */
  @Override
  public boolean isSaveAsAllowed() {
    return false;
  }

  /**
   * Saves the contents of this editor. This method is long-running; progress
   * and cancellation are provided by the given progress monitor.
   *
   * @param monitor
   *          the progress monitor
   * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public void doSave(IProgressMonitor monitor) {
    final IEditorInput input = getEditorInput();
    try {
      commitFormPages(true);
    } catch (CanceledOperationException ex) {
      monitor.setCanceled(true);
      return;
    }

    try {
      updateDocument(true);
    } catch (Exception e) {
      DPPErrorHandler.processError(e);
      monitor.setCanceled(true);
      DPPErrorHandler.showErrorTableDialog(e.getMessage());
      return;
    }
    try {
      checkCorrectness();
    } catch (InconsistentDataException e) {
      if (monitor != null) {
        monitor.setCanceled(true);
      }
      return;
    }

    WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
      @Override
      public void execute(IProgressMonitor monitor) throws CoreException {
        documentProvider.saveDocument(monitor, input, documentProvider.getDocument(input), true);
        ((DPPFileModel) model).updateLastModified(((DPPFileModel) model).getFile());
      }
    };
    try {
      documentProvider.aboutToChange(input);
      op.run(null);
      documentProvider.changed(input);
      fireSaveNeeded();
    } catch (InterruptedException x) {
      DPPErrorHandler.processError(x, false);
      monitor.setCanceled(true);
      return;
    } catch (InvocationTargetException x) {
      if (x.getTargetException().getMessage().endsWith("is read-only.")) {
        Shell shell = parent.getShell();
        if ((shell != null) && !shell.isDisposed()) {
          String message = ResourceManager.format(RO_ERROR, new String[] {
            file.getName()
          });
          MessageDialog.openError(shell, ResourceManager.getString(DPPFormSection.ERROR_MSG, ""), message);
        }
      } else {
        DPPErrorHandler.processError(x, false);
      }
      monitor.setCanceled(true);
      return;
    }
    op = null;

    if (getCurrentPage().isSource()) {
      getCurrentPage().becomesVisible(getCurrentPage());
    }
    fireSaveNeeded();
  }

  /**
   * Fires a property changed event when the save is needed. Calls
   * <code>firePropertyChange</code> method with the property id for
   * <code>isDirty</code>.
   */
  public void fireSaveNeeded() {
    firePropertyChange(PROP_DIRTY);
  }

  /**
   * Closes this editor and depends on the given save flag saves the changes
   * made in the editor.
   *
   * @param save
   *          <code>boolean</code> value that signify if the editor changes will
   *          be saved or not
   */
  public void close(final boolean save) {
    Display display = getSite().getShell().getDisplay();

    display.asyncExec(new Runnable() {
      public void run() {
        getSite().getPage().closeEditor(DPPMultiPageEditor.this, save);
      }
    });
  }

  /**
   * Calls the notify method for all listeners of the each editor page for the
   * new values of the components and sets that this editor is changed and need
   * to be saved.
   *
   * @param onSave
   *          <code>boolean</code> parameter to sets if the changes will be
   *          saved or not
   * @throws CanceledOperationException
   *           shows if the operation is canceled.
   */
  public void commitFormPages(boolean onSave) throws CanceledOperationException {
    for (Iterator iter = getPages(); iter.hasNext();) {
      try {
        IDPPEditorPage page = (IDPPEditorPage) iter.next();
        if (page instanceof DPPFormPage) {
          DPPFormPage formPage = (DPPFormPage) page;
          if (formPage instanceof DPPMultiFormPage) {
            DPPMultiFormPage multiPage = (DPPMultiFormPage) formPage;
            multiPage.commitFormPages(onSave);
          } else {
            formPage.getForm().commitChanges(onSave);
          }
        }
      } catch (CanceledOperationException coe) {
        if (coe.getMessage().trim().length() <= 0) {
          throw new CanceledOperationException(coe.getMessage());
        }
        boolean b = MessageDialog.openConfirm(DPPErrorHandler.getShell(), WARNING, coe.getMessage() + "\n"
            + IGNORE_WARNING_QUESTION);
        if (!b) {
          throw new CanceledOperationException(coe.getMessage());
        }
      }
    }
  }

  /**
   * Returns whether the contents of this editor have changed since the last
   * save operation.
   *
   * @return <code>true</code> if the contents have been modified and need
   *         saving, and <code>false</code> if they have not changed since the
   *         last save
   *
   * @see org.eclipse.ui.part.EditorPart#isDirty()
   */
  @Override
  public boolean isDirty() {
    if (isModelDirty(model)) {
      return true;
    }
    return false;
  }

  /**
   * Asks this part to take focus within the workbench.
   *
   * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
   */
  @Override
  public void setFocus() {
    if (getCurrentPage() != null) {
      getCurrentPage().setFocus();
    }
  }

  /**
   * Shows the editor page, which corresponding with the given page identifier.
   *
   * @param id
   *          the page identifier
   * @return the searched editor page
   */
  public IDPPEditorPage showPage(String id) {
    return showPage(getPage(id));
  }

  /**
   * Selects the given editor page and makes the page current visible page.
   *
   * @param page
   *          the editor page which will become selected page
   * @return the selected page
   */
  public IDPPEditorPage showPage(final IDPPEditorPage page) {
    formWorkbook.selectPage(page);
    return page;
  }

  /**
   * Returns the old selected editor page.
   *
   * @return old current editor page
   */
  public IDPPEditorPage getOldPage() {
    if (formWorkbook == null) {
      return null;
    }
    return (IDPPEditorPage) formWorkbook.getOldPage();
  }

  // ISelectionProvider implementation

  /**
   * Sets the current selection for the selection provider.
   *
   * @param selection
   *          the new selection
   *
   * @see org.eclipse.jface.viewers.ISelectionProvider#setSelection(org.eclipse.jface.viewers.ISelection)
   */
  public void setSelection(ISelection selection) {
    if (selectionProvider != null) {
      selectionProvider.setSelection(selection);
    }
  }

  /**
   * Adds a listener for selection changes in the editor's selection provider.
   *
   * @param listener
   *          a selection changed listener
   *
   * @see org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
   */
  public void addSelectionChangedListener(ISelectionChangedListener listener) {
    if (selectionProvider != null) {
      selectionProvider.addSelectionChangedListener(listener);
    }
  }

  /**
   * Removes the given selection change listener from the editor's selection
   * provider.
   *
   * @param listener
   *          a selection changed listener
   *
   * @see org.eclipse.jface.viewers.ISelectionProvider#removeSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
   */
  public void removeSelectionChangedListener(ISelectionChangedListener listener) {
    if (selectionProvider != null) {
      selectionProvider.removeSelectionChangedListener(listener);
    }
  }

  /**
   * Returns the current selection for the editor's selection provider.
   *
   * @return the current selection
   *
   * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
   */
  public ISelection getSelection() {
    if (selectionProvider == null) {
      return null;
    }
    return selectionProvider.getSelection();
  }

  /**
   * Sent when a shell stops being the active window.
   *
   * @param e
   *          an event containing information about the deactivation
   * @see org.eclipse.swt.events.ShellListener#shellDeactivated(org.eclipse.swt.events.ShellEvent)
   */
  public void shellDeactivated(ShellEvent e) {
  }

  /**
   * Sent when a shell is closed.
   *
   * @param e
   *          an event containing information about the close
   *
   * @see org.eclipse.swt.events.ShellListener#shellClosed(org.eclipse.swt.events.ShellEvent)
   */
  public void shellClosed(ShellEvent e) {
  }

  /**
   * Sent when a shell is minimized.
   *
   * @param e
   *          an event containing information about the minimization
   *
   * @see org.eclipse.swt.events.ShellListener#shellIconified(org.eclipse.swt.events.ShellEvent)
   */
  public void shellIconified(ShellEvent e) {
  }

  /**
   * Sent when a shell is un-minimized.
   *
   * @param e
   *          an event containing information about the un-minimization
   *
   * @see org.eclipse.swt.events.ShellListener#shellDeiconified(org.eclipse.swt.events.ShellEvent)
   */
  public void shellDeiconified(ShellEvent e) {
  }

  // IFormSelectionListener implementation
  /**
   * Sets the given editor page to be selected
   *
   * @param page
   *          the page which will be selected
   *
   * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormSelectionListener#formSelected(org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage)
   */
  public void formSelected(IFormPage page) {
    if (page instanceof DPPFormPage) {
      DPPFormPage formPage = (DPPFormPage) page;
      if (formPage.getSelection() != null) {
        setSelection(formPage.getSelection());
      }
    }
  }

  // IModelChangedListener implementation
  /**
   * Called when there is a change in the model this listener is registered
   * with.
   *
   * @param e
   *          a change event that describes the kind of the model change
   *
   * @see org.tigris.mtoolkit.dpeditor.editor.model.IModelChangedListener#modelChanged(org.tigris.mtoolkit.dpeditor.editor.model.IModelChangedEvent)
   */
  public void modelChanged(IModelChangedEvent e) {
    if (e.getChangeType() != IModelChangedEvent.WORLD_CHANGED) {
      fireSaveNeeded();
    }
  }
}
