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
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.eclipse.ui.progress.UIJob;
import org.tigris.mtoolkit.cdeditor.internal.integration.JDTModelHelper;
import org.tigris.mtoolkit.cdeditor.internal.model.CDModelEvent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDModel;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDModifyListener;
import org.tigris.mtoolkit.cdeditor.internal.model.IEclipseContext;
import org.tigris.mtoolkit.cdeditor.internal.model.impl.CDModel;
import org.tigris.mtoolkit.cdeditor.internal.text.CDSourceViewerConfiguration;
import org.tigris.mtoolkit.cdeditor.internal.text.ComponentDescriptionDocumentProvider;
import org.tigris.mtoolkit.cdeditor.internal.text.StyleManager;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * This class represents Component Description Editor. It consists of two pages - 
 * Design page and Source page. The Design page supports graphically editing of
 * Component description and Source page allows editing in text format.
 *
 */
public class ComponentDescriptionEditor extends FormEditor implements
		IDocumentListener, ICDModifyListener, IResourceChangeListener,
		IGlobalActionHandler {

	public static final String EDITOR_ID = "org.tigris.mtoolkit.cdeditor.ComponentDescriptionEditor";

	private ComponentDescriptionTextEditor sourcePage;
	private MainPage mainPage;
	private ICDModel model;

	private IFile underlyingFile;

	private volatile boolean synchronizedFile = true;
	private final Job refreshModelJob = new RefreshModelJob();
	private final Job revalidateModelJob = new RevalidateModelJob();

	private boolean documentChanged = false;

	private boolean ignoreModify = false;

	private boolean ignoreDocumentModify = false;

	private boolean modelValidationStateDirty = true;

	private CDUndoManager undoManager;

	public ComponentDescriptionEditor() {
		super();
	}

	protected void addPages() {
		try {
			mainPage = new MainPage(this, MainPage.PAGE_ID, "Design");
			addPage(mainPage);
		} catch (PartInitException e) {
			CDEditorPlugin.log(e);
		}
		try {
			sourcePage = new ComponentDescriptionTextEditor();
			int index = addPage(sourcePage, getEditorInput());
			setPageText(index, "Source");
			sourcePage.getDocumentProvider().getDocument(getEditorInput()).addDocumentListener(this);
		} catch (PartInitException e) {
			CDEditorPlugin.log(e);
		}
	}

	/**
	 * Saves the document.
	 */
	public void doSave(IProgressMonitor monitor) {
		commitMainPage();
		sourcePage.doSave(monitor);
	}

	private void commitMainPage() {
		if (mainPage.isDirty()) {
			beginModelModification();
			try {
				mainPage.getManagedForm().commit(true);
			} finally {
				endModelModification();
			}
		}
		ignoreDocumentModify = true;
		try {
			updateSource();
		} finally {
			ignoreDocumentModify = false;
		}
	}

	/**
	 * Saves the document as another file. Also updates this input.
	 */
	public void doSaveAs() {
		commitMainPage();
		sourcePage.doSaveAs();
		setPartName(getEditorInput().getName());
		setInput(sourcePage.getEditorInput());
		firePropertyChange(IEditorPart.PROP_INPUT);
	}

	/**
	 * Method declared on IEditorPart.
	 */
	public boolean isSaveAsAllowed() {
		return true;
	}

	public void documentAboutToBeChanged(DocumentEvent event) {
	}

	public void documentChanged(DocumentEvent event) {
		if (!ignoreDocumentModify)
			documentChanged = true;
	}

	/**
	 * Method declared on IEditorPart
	 */
	public void gotoMarker(IMarker marker) {
		setActivePage(1);
		IDE.gotoMarker(getEditor(1), marker);
	}

	/**
	 * Checks that the input is an instance of <code>IFileEditorInput</code>.
	 */
	public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
		if (!(editorInput instanceof IFileEditorInput))
			throw new PartInitException("Invalid Input: IFileEditorInput expected.");
		super.init(site, editorInput);
		// setting editor title to the name of the opened file
		setPartName(editorInput.getName());
		findUnderlyingResource(editorInput);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_CLOSE);
	}

	protected IEditorSite createSite(IEditorPart editor) {
		return new ComponentDescriptionMultiPageEditorSite(this, editor);
	}

	public void dispose() {
		super.dispose();
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		if (undoManager != null)
			undoManager.dispose();
	}

	protected void pageChange(int newPageIndex) {
		super.pageChange(newPageIndex);
		if (newPageIndex == 0) {
			if (documentChanged) {
				updateForm();
				documentChanged = false;
			}
		} else if (newPageIndex == 1) {
			// update source page before switching
			commitMainPage();
			sourcePage.refreshPresentation();
		}
	}

	private void updateSource() {
		if (model != null && model.isDirty()) {
			// XXX: Decide whether to expose the loading/saving methods on
			// ICDModel interface (perhaps yes)
			((CDModel) model).updateDocument();
			undoManager.invalidateDetachedNodesOffsets();
			model.setDirty(false);
		}
	}

	private void updateForm() {
		try {
			IEditorInput editorInput = getEditorInput();
			loadModel(sourcePage.getDocumentProvider().getDocument(editorInput), getInputStorage(editorInput));
		} catch (CoreException e) {
			CDEditorPlugin.getDefault().getLog().log(e.getStatus());
		}
	}

	private void loadModel(IDocument document, IStorage storage) throws CoreException {
		try {
			beginModelModification();
			try {
				if (model == null) {
					// XXX: convert to use factory for CDElement objects
					model = new CDModel();
					model.addModifyListener(this);
					undoManager = new CDUndoManager(model);
				}

				if (underlyingFile != null) {
					IEclipseContext context = (IEclipseContext) underlyingFile.getProject().getAdapter(IEclipseContext.class);
					model.setEclipseContext(context);
				}

				synchronizedFile = true;

				// XXX: Decide whether to expose the loading/saving methods on
				// ICDModel interface (perhaps yes)
				try {
					((CDModel) model).load(document);
					mainPage.setMessage(null, true);
				} catch (SAXException e) {
					// check for out-of-sync resource
					if (storage != null) {
						try {
							storage.getContents();
						} catch (CoreException ce) {
							mainPage.setMessage("Unable to load the document. " + ce.getMessage(), false);
							if (underlyingFile != null) {
								// don't reload the model after this point
								synchronizedFile = underlyingFile.isSynchronized(IResource.DEPTH_ZERO);
							}
							return;
						}
					}
					String message = "The document is not valid XML. " + e.getMessage();
					if (e instanceof SAXParseException) {
						SAXParseException parseEx = (SAXParseException) e;
						message += " (near line " + parseEx.getLineNumber() + ", column " + parseEx.getColumnNumber() + ")";
					}
					mainPage.setMessage(message, true);
				} catch (RuntimeException e) {
					mainPage.setMessage("Internal error occurred. Please see Error Log for more information.", true);
					throw e;
				}
			} finally {
				endModelModification();
			}
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, CDEditorPlugin.PLUGIN_ID, "Failed to parse passed XML", e));
		}
	}

	/**
	 * Rerurns the Model which represnts the Component description
	 * @return the model
	 */
	public ICDModel getModel() {
		if (model == null) {
			try {
				IEditorInput editorInput = getEditorInput();
				loadModel(sourcePage.getDocumentProvider().getDocument(editorInput), getInputStorage(editorInput));
			} catch (CoreException e) {
				CDEditorPlugin.error("Unable to load model", e);
			}
		}
		return model;
	}

	protected void firePropertyChange(int propertyId) {
		super.firePropertyChange(propertyId);
		if (propertyId == IEditorPart.PROP_INPUT) {
			IEditorInput newInput = sourcePage.getEditorInput();
			handleNewInput(newInput);
		}
	}

	private void handleNewInput(IEditorInput newInput) {
		try {
			setInput(newInput);
			setPartName(newInput.getName());
			findUnderlyingResource(newInput);
			loadModel(sourcePage.getDocumentProvider().getDocument(newInput), getInputStorage(newInput));
			sourcePage.getDocumentProvider().getDocument(newInput).addDocumentListener(this);
		} catch (CoreException e) {
			CDEditorPlugin.log(e);
		}
	}

	private void findUnderlyingResource(IEditorInput input) {
		if (input instanceof IFileEditorInput) {
			underlyingFile = ((IFileEditorInput) input).getFile();
		} else {
			underlyingFile = null;
		}
	}

	public void modelModified(CDModelEvent event) {
		modelValidationStateDirty = false;
		if (!ignoreModify)
			editorDirtyStateChanged();
	}

	private void beginModelModification() {
		ignoreModify = true;
	}

	private void endModelModification() {
		ignoreModify = false;
	}

	public boolean isDirty() {
		return super.isDirty() || (model != null ? model.isDirty() : false);
	}

	private IStorage getInputStorage(IEditorInput input) {
		if (getEditorInput() instanceof IStorageEditorInput) {
			try {
				return ((IStorageEditorInput) getEditorInput()).getStorage();
			} catch (CoreException e) {
				CDEditorPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, CDEditorPlugin.PLUGIN_ID, "Failed to retrieve IStorage object from editor's input", e));
			}
		}
		return null;
	}

	private boolean listenForChangesInJDTModel() {
		if (model != null && model.getProjectContext() != null) {
			IEclipseContext context = model.getProjectContext();
			IJavaProject project = (IJavaProject) context.getAdapter(IJavaProject.class);
			return project != null;
		}
		return false;
	}

	private boolean listenForChangesInSourceFile() {
		return underlyingFile != null && !ignoreModify && !synchronizedFile;
	}

	public void resourceChanged(IResourceChangeEvent event) {
		// handle project close by closing editors when detected
		if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
			if (underlyingFile != null && underlyingFile.getProject().equals(event.getResource()))
				close(false);
			return;
		}
		if (!listenForChangesInJDTModel() && !listenForChangesInSourceFile())
			return;
		final IProject[] revalidationContext = getContextProjects();

		final boolean[] found = listenForChangesInSourceFile() ? new boolean[] { false } : null;
		final boolean[] revalidate = listenForChangesInJDTModel() ? new boolean[] { false } : null;
		try {
			event.getDelta().accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) throws CoreException {
					if (found != null && found[0])
						return false;
					if (found != null && delta.getResource().equals(underlyingFile)) {
						if (!refreshModelJob.cancel())
							try {
								refreshModelJob.join();
							} catch (InterruptedException e) {
							}
						refreshModelJob.setRule(underlyingFile);
						refreshModelJob.schedule();
						found[0] = true;
						return false;
					}
					if (revalidate != null && revalidate[0])
						return false;
					if (revalidate != null) {
						IProject project = delta.getResource().getProject();
						if (project != null)
							for (int i = 0; i < revalidationContext.length; i++)
								if (revalidationContext[i].equals(project)) {
									revalidate[0] = true;
									return false;
								}
					}
					return true;
				}
			});
		} catch (CoreException e) {
			CDEditorPlugin.error("Unable to process resource delta", e);
		}
		if (revalidate != null && revalidate[0]) {
			modelValidationStateDirty = true;
			debug("Scheduling model revalidation...");
			if (!revalidateModelJob.cancel())
				try {
					revalidateModelJob.join();
				} catch (InterruptedException e) {
				}
			revalidateModelJob.setRule(underlyingFile);
			revalidateModelJob.schedule(500L);
		}
	}

	private static class ComponentDescriptionTextEditor extends TextEditor {

		public ComponentDescriptionTextEditor() {
			super();
			setDocumentProvider(new ComponentDescriptionDocumentProvider());
			setSourceViewerConfiguration(new CDSourceViewerConfiguration(new StyleManager()));
		}

		public void refreshPresentation() {
			// dnachev: It currently works without forcing text presentation
			// invalidation
			// getSourceViewer().invalidateTextPresentation();
		}

	}

	private static class ComponentDescriptionMultiPageEditorSite extends
			MultiPageEditorSite {

		public ComponentDescriptionMultiPageEditorSite(
				MultiPageEditorPart multiPageEditor, IEditorPart editor) {
			super(multiPageEditor, editor);
		}

		public IActionBars getActionBars() {
			return ((ComponentDescriptionEditorContributor) getMultiPageEditor().getEditorSite().getActionBarContributor()).getSourceActionBars();
		}
	}

	private class RefreshModelJob extends Job {

		private RefreshModelJob() {
			super("Refresh Component Description Editor Model");
			setSystem(true);
		}

		protected IStatus run(final IProgressMonitor monitor) {
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				public void run() {
					if (monitor.isCanceled())
						return;
					updateForm();
				}
			});
			return Status.OK_STATUS;
		}
	}

	private class RevalidateModelJob extends UIJob {
		private RevalidateModelJob() {
			super("Revalidate Component Description Editor Model");
			setSystem(true);
			setPriority(Job.DECORATE);
		}

		public IStatus runInUIThread(IProgressMonitor monitor) {
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			if (model != null && modelValidationStateDirty && !mainPage.getManagedForm().getForm().isDisposed()) {
				debug("[RevalidateModelJob] Revalidate model...");
				model.validate();
			}
			return Status.OK_STATUS;
		}
	}

	private IProject[] getContextProjects() {
		if (model != null && model.getProjectContext() != null) {
			IJavaProject project = (IJavaProject) model.getProjectContext().getAdapter(IJavaProject.class);
			if (project != null) {
				List projects = new ArrayList();
				projects.add(project.getProject());
				IProject[] dependent = JDTModelHelper.getRequiredProjects(project.getProject());
				for (int i = 0; i < dependent.length; i++)
					projects.add(dependent[i]);
				return (IProject[]) projects.toArray(new IProject[projects.size()]);
			}
		}
		return null;
	}

	public void handleGlobalAction(String id) {
		if (ActionFactory.UNDO.getId().equals(id)) {
			mainPage.getManagedForm().commit(false);
			undoManager.undo();
		} else if (ActionFactory.REDO.getId().equals(id)) {
			mainPage.getManagedForm().commit(false);
			undoManager.redo();
		} else {
			throw new IllegalArgumentException("Unknown global action requested: " + id);
		}
	}

	public void updateUndoRedo(IAction undoAction, IAction redoAction) {
		if (undoManager != null)
			undoManager.connectActions(undoAction, redoAction);
	}

	private static final void debug(String message) {
		if (CDEditorPlugin.DEBUG)
			CDEditorPlugin.debug("[CDEditor] ".concat(message));
	}
}
