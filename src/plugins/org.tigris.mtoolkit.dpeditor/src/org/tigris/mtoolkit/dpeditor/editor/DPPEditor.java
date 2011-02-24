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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.tigris.mtoolkit.common.PluginUtilities;
import org.tigris.mtoolkit.dpeditor.DPActivator;
import org.tigris.mtoolkit.dpeditor.editor.base.CustomWorkbook;
import org.tigris.mtoolkit.dpeditor.editor.base.DPPFormPage;
import org.tigris.mtoolkit.dpeditor.editor.base.DPPMultiPageEditor;
import org.tigris.mtoolkit.dpeditor.editor.base.IDPPEditorPage;
import org.tigris.mtoolkit.dpeditor.editor.dialog.ChangeBundleJarNameDialog;
import org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage;
import org.tigris.mtoolkit.dpeditor.editor.model.DPPFileModel;
import org.tigris.mtoolkit.dpeditor.util.DPPErrorHandler;
import org.tigris.mtoolkit.dpeditor.util.DPPUtil;
import org.tigris.mtoolkit.dpeditor.util.ResourceManager;
import org.tigris.mtoolkit.util.BundleInfo;
import org.tigris.mtoolkit.util.DPPFile;
import org.tigris.mtoolkit.util.DPPUtilities;
import org.tigris.mtoolkit.util.InconsistentDataException;

/**
 * This class creates all pages of the editor and holds all opened deployment
 * package editors.
 */
public class DPPEditor extends DPPMultiPageEditor implements
		IElementChangedListener, IResourceChangeListener {

	/** Avoid element change event when the element was renamed */
	public static boolean isRenamed = false;
	/** The packages header page identifier */
	public static final String HEADERS_PAGE = "HeadersPage";
	/** The bundles page identifier */
	public static final String BUNDLES_PAGE = "BundlesPage";
	/** The resources page identifier */
	public static final String RESOURCES_PAGE = "ResourcesPage";
	/** The certificates page identifier */
	public static final String CERTIFICATES_PAGE = "CertificatesPage";
	/** The build page identifier */
	public static final String BUILD_PAGE = "BuildPage";
	/** The source page identifier */
	public static final String SOURCE_PAGE = "SourcePage";
	/** The manifest source page identifier */
	public static final String MF_SOURCE_PAGE = "ManifestSourcePage";

	/** The packages header page title */
	public static final String KEY_HEADERS = "DPPEditor.HeadersPage.title";
	/** The bundles page title */
	public static final String KEY_BUNDLES = "DPPEditor.BundlesPage.title";
	/** The resources page title */
	public static final String KEY_RESOURCES = "DPPEditor.ResourcesPage.title";
	/** The certificates page title */
	public static final String KEY_CERTIFICATES = "DPPEditor.CertificatesPage.title";
	/** The build page title */
	public static final String KEY_BUILD = "DPPEditor.BuildPage.title";

	/** Ask to save file and ignore warning */
	public static final String IGNORE_WARNING = "DPPEditor.IgnoreWarning";

	/** File modified dialog title */
	public static final String FILE_MODIFIED = "DPPEditor.FileModifiedTitle";
	/** Ask message to revert to the saved file */
	public static final String FILE_MODIFIED_MESSAGE = "DPPEditor.FileModifiedMessage2";
	/** Holds the error title */
	public static final String ERROR_MSG = "DPPEditor.Error";

	/** The headers form page */
	private HeadersFormPage headersPage;
	/** The bundles form page */
	private BundlesFormPage bundlesPage;
	/** The resources form page */
	private ResourcesFormPage resourcesPage;
	/** The certificates form page */
	private CertificatesFormPage certificatesPage;
	/** The build form page */
	private BuildFormPage buildPage;
	/** The manifest source form page */
	private ManifestSourceFormPage mfPage;

	/** Checks the modified values if not active */
	private boolean activated = false;
	/** Shows if the model was created from archive */
	private boolean fromArchive = false;

	/** Holds value representing the time the file was last modified */
	private long lastModified;
	/**
	 * Shows if the formSelected method to check the consistency of the form,
	 * will be selected
	 */
	private boolean formSelection = false;

	public static boolean isDialogShown = false;

	/** The <code>IFile</code> for which this editor was responsible to */
	private IFile dppFile;

	/**
	 * A <code>Hashtable</code> keeping for every deployment package file a
	 * vector of opened deployment package editors
	 */
	public static Hashtable activeEditors = new Hashtable();

	/**
	 * Creates the all editor pages and work's control.
	 */
	public DPPEditor() {
		super();
		JavaCore.addElementChangedListener(this);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
	}

	/*
	 * Initializes the editor part with a site and input.
	 * 
	 * @see
	 * org.tigris.mtoolkit.dpeditor.editor.base.DPPMultiPageEditor#init(org.
	 * eclipse .ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	public void init(final IEditorSite site, final IEditorInput input) throws PartInitException {
		super.init(site, input);
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				try {
					init0(site, input);
				} catch (PartInitException e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void init0(IEditorSite site, IEditorInput input) throws PartInitException {
		dppFile = getFile();
		if (dppFile != null) {
			model = getModel(dppFile);
			if (model == null) {
				model = new DPPFileModel(dppFile);
			}
			DPPFile file = ((DPPFileModel) model).getDPPFile();
			Control shellControl = site.getShell();
			if (!DPActivator.getDefault().isAcceptAutomaticallyChanges()) {
				ChangeBundleJarNameDialog dialog = new ChangeBundleJarNameDialog(DPPErrorHandler.getShell(), shellControl.getLocation(), shellControl.getSize());
				dialog.setDPPFile(file);
				if (dialog.open() == Window.OK) {
					Hashtable selectedJars = dialog.getSelectedJars();
					Enumeration keys = selectedJars.keys();
					boolean needToSave = false;
					while (keys.hasMoreElements()) {
						String key = (String) keys.nextElement();
						String value = (String) selectedJars.get(key);
						Vector infos = file.getBundleInfos();
						for (int i = 0; i < infos.size(); i++) {
							BundleInfo info = (BundleInfo) infos.elementAt(i);
							String bundlePath = info.getBundlePath();
							if (bundlePath.toLowerCase().equals(key.toLowerCase())) {
								DPPUtil.updateBundlePath(info, value);
								if (!needToSave) {
									needToSave = true;
								}
							}
						}
					}
					if (needToSave) {
						try {
							((DPPFileModel) model).setDirty(true);
						} catch (Exception e) {
							DPPErrorHandler.processError(e, false);
						}
					}
				}
			} else {
				Vector infos = file.getBundleInfos();
				if (infos != null) {
					boolean needToSave = false;
					for (int i = 0; i < infos.size(); i++) {
						BundleInfo info = (BundleInfo) infos.elementAt(i);
						String bundlePath = info.getBundlePath();
						File bundlePathFile = new File(bundlePath);
						File findLastJar = DPPUtilities.findLastJar(bundlePathFile);
						if (!bundlePath.toLowerCase().equals(findLastJar.toString().toLowerCase())) {
							DPPUtil.updateBundlePath(info, findLastJar.toString());
							if (!needToSave) {
								needToSave = true;
							}
						}
					}
					if (needToSave) {
						try {
							file.save();
							lastModified = getLastModified(((DPPFileModel) model).getFile());
						} catch (Exception e) {
							DPPErrorHandler.processError(e, false);
						}
					}
				}
			}
			DPPUtil.updateBundlesData(file);
		}
		if (headersPage != null) {
			headersPage.setDPPFile(dppFile);
		}
		if (buildPage != null) {
			buildPage.setDPPFile(dppFile);
		}
	}

	/*
	 * Creates all pages of this editor.
	 * 
	 * @see
	 * org.tigris.mtoolkit.dpeditor.editor.base.DPPMultiPageEditor#createPages()
	 */
	protected void createPages() {
		firstPageId = HEADERS_PAGE;
		formWorkbook.setFirstPageSelected(false);

		headersPage = new HeadersFormPage(this, ResourceManager.getString(KEY_HEADERS, ""));
		addPage(HEADERS_PAGE, headersPage);

		bundlesPage = new BundlesFormPage(this, ResourceManager.getString(KEY_BUNDLES, ""));
		addPage(BUNDLES_PAGE, bundlesPage);

		resourcesPage = new ResourcesFormPage(this, ResourceManager.getString(KEY_RESOURCES, ""));
		addPage(RESOURCES_PAGE, resourcesPage);

		certificatesPage = new CertificatesFormPage(this, ResourceManager.getString(KEY_CERTIFICATES, ""));
		addPage(CERTIFICATES_PAGE, certificatesPage);

		buildPage = new BuildFormPage(this, ResourceManager.getString(KEY_BUILD, ""));
		addPage(BUILD_PAGE, buildPage);

		mfPage = new ManifestSourceFormPage(this);
		addPage(MF_SOURCE_PAGE, mfPage);
	}

	/*
	 * Returns the identifier of the source page of this editor.
	 * 
	 * @see
	 * org.tigris.mtoolkit.dpeditor.editor.base.DPPMultiPageEditor#getSourcePageId
	 * ()
	 */
	protected String getSourcePageId() {
		return HEADERS_PAGE;
	}

	/**
	 * Returns the default page of this editor. In this case this page is the
	 * <code>HeadersFormPage</code>.
	 * 
	 * @return the default page of editor
	 */
	public IDPPEditorPage getHomePage() {
		return getPage(HEADERS_PAGE);
	}

	/**
	 * Returns the workbench window of the page of the site for this workbench
	 * part.
	 * 
	 * @return the workbench window
	 */
	public IWorkbenchWindow getWindow() {
		return getSite().getPage().getWorkbenchWindow();
	}

	/**
	 * Gets the deployment package file model for the given file, or returns
	 * <code>null</code> if there are no active editors.
	 * 
	 * @param file
	 *            the file, which model searching for
	 * @return the deployment package file model for the given file,
	 *         <code>null</code> if no active editors
	 */
	public static DPPFileModel getModel(IFile file) {
		Vector editors = (Vector) activeEditors.get(file);
		if ((editors == null) || (editors.size() == 0)) {
			return null;
		} else {
			return (DPPFileModel) ((DPPEditor) editors.elementAt(0)).getModel();
		}
	}

	/**
	 * Checks if <code>activeEditors</code> hashtable contains an editor, with
	 * the same deployment package file and opened in the same workbench window,
	 * as the given editor. If such editor doesn't exist in the hashtable, the
	 * given one is added.
	 * 
	 * @param window
	 *            the workbench window, where new editor is opened
	 * @param editor
	 *            the deployment package editor that has to be added to
	 *            <code>activeEditors</code> <code>Hashtable</code>
	 */
	public static void addDPPEditor(/* IWorkbenchWindow window, */DPPEditor editor) {
		IFile file = editor.getFile();
		if (file == null) {
			return;
		}
		Vector v = (Vector) activeEditors.get(file);
		if (v != null) {
			for (int i = 0; i < v.size(); i++) {
				DPPEditor nextEditor = (DPPEditor) v.elementAt(i);
				if (nextEditor.getWindow().equals(editor.getWindow())) {
					return;
				}
			}
		} else {
			v = new Vector();
		}
		v.addElement(editor);
		activeEditors.put(file, v);
	}

	/**
	 * Checks if <code>activeEditors</code> hashtable contains an editor, with
	 * the same deployment package file and in the same workbench window, as the
	 * given editor. If such editor exists in the <code>Hashtable</code>, remove
	 * it.
	 * 
	 * @param window
	 *            the workbench window, where new editor was opened
	 * @param editor
	 *            the deployment package editor that has to be removed from
	 *            <code>activeEditors</code> <code>Hashtable</code>
	 */
	public static void removeDPPEditor(
	/* IWorkbenchWindow window, */DPPEditor editor) {
		IFile file = editor.getFile();
		if (file == null)
			return;

		Vector v = (Vector) activeEditors.get(file);
		if (v != null) {
			for (int i = v.size() - 1; i >= 0; i--) {
				DPPEditor nextEditor = (DPPEditor) v.elementAt(i);
				if (nextEditor.getWindow().equals(editor.getWindow())) {
					v.removeElement(nextEditor);
					if (v.size() == 0) {
						activeEditors.remove(file);
					} else {
						activeEditors.put(file, v);
					}
					return;
				}
			}
		}
	}

	/**
	 * Returns all deployment package editors, opened in the given workbench
	 * window.
	 * 
	 * @param window
	 *            the workbench window
	 * 
	 * @return <code>Vector</code> of deployment package editors in the given
	 *         window
	 */
	public static Vector getDPPEditors(IWorkbenchWindow window) {
		Vector result = new Vector();
		if (window == null) {
			return null;
		} else {
			Enumeration editors = activeEditors.elements();
			while (editors.hasMoreElements()) {
				Vector next = (Vector) editors.nextElement();
				for (int i = 0; i < next.size(); i++) {
					DPPEditor nextEditor = ((DPPEditor) next.elementAt(0));
					if (window.equals(nextEditor.getWindow())) {
						result.addElement(nextEditor);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Returns the deployment package editor for the given file, and in the
	 * given window
	 * 
	 * @param window
	 *            the window
	 * @param file
	 *            the file
	 * @return the deployment package editor, if it exists, <code>null</code>
	 *         otherwise
	 */
	public static DPPEditor getDPPEditor(IWorkbenchWindow window, IFile file) {
		Vector v = (Vector) activeEditors.get(file);
		if (v != null) {
			for (int i = 0; i < v.size(); i++) {
				DPPEditor nextEditor = (DPPEditor) v.elementAt(i);
				if (nextEditor.getWindow().equals(window)) {
					return nextEditor;
				}
			}
		}
		return null;
	}

	/**
	 * Returns all deployment package editors currently opened
	 * 
	 * @return <code>Vector</code> of all deployment package editors
	 */
	public static Vector getAllDPPEditors() {
		Vector result = new Vector();
		Enumeration en = activeEditors.elements();
		while (en.hasMoreElements()) {
			Vector nextEditors = (Vector) en.nextElement();
			for (int i = 0; i < nextEditors.size(); i++) {
				result.addElement(nextEditors.elementAt(i));
			}
		}
		return result;
	}

	/**
	 * Returns for the given file the active deployment package editor, in which
	 * this file is opened.
	 * 
	 * @param file
	 *            the file, which will be opened in active editor
	 * @return the active deployment package editor for the given file
	 */
	public static DPPEditor getActiveDPPEditor(IFile file) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			return getDPPEditor(window, file);
		}
		return null;
	}

	/**
	 * Returns all deployment package editors, opened for the given file in the
	 * given workbench window.
	 * 
	 * @param file
	 *            the file, which will be opened in all returned editors
	 * @return <code>Vector</code> of deployment package editors in the given
	 *         window
	 */
	public static Vector getDPPEditors(IFile file) {
		if (file == null) {
			return null;
		} else {
			return (Vector) activeEditors.get(file);
		}
	}

	/**
	 * Delegates the creation of the <code>DPPFileModel</code> for the given
	 * input to either createDPPModel, or createModelFromStorage, depending on
	 * the type of the input object.
	 * 
	 * @param input
	 *            input object
	 * @return the resource model for this input
	 */
	protected Object createModel(Object input) {
		if (input instanceof IFile) {
			model = (DPPFileModel) createDPPModel((IFile) input);
			addDPPEditor(this);
			return model;
		} else if (input instanceof IStorageEditorInput) {
			fromArchive = true;
			model = (DPPFileModel) createModelFromStorage((IStorageEditorInput) input);
			return model;
		}
		return null;
	}

	/*
	 * Creates the SWT controls for this workbench part.
	 * 
	 * @see
	 * org.tigris.mtoolkit.dpeditor.editor.base.DPPMultiPageEditor#createPartControl
	 * (org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		if (fromArchive) {
			((CustomWorkbook) formWorkbook).disablePages();
		}
	}

	/**
	 * Creates the Deployment package file model for the given storage.
	 * 
	 * @param input
	 *            the input for which will be created the deployment package
	 *            file model
	 * @return the created deployment package file model.
	 */
	private DPPFileModel createModelFromStorage(IStorageEditorInput input) {
		try {
			InputStream stream = input.getStorage().getContents();
			DPPFileModel model = new DPPFileModel();
			model.getDPPFile().restoreFromFile();
			/*
			 * try { model.load(stream); } catch (CoreException e) {
			 * DPPErrorHandler.processError(e, false); return null; } catch
			 * (IncorrectDPPException ex) { DPPErrorHandler.processError(ex,
			 * false); }
			 */

			try {
				stream.close();
			} catch (IOException e) {
				DPPErrorHandler.processError(e, false);
			}
			return model;
		} catch (CoreException e) {
			DPPErrorHandler.processError(e, false);
			return null;
		}
	}

	/**
	 * Creates deployment package file model for the given <code>IFile</code>.
	 * 
	 * @param file
	 *            the file for which will be created the deployment package file
	 *            model
	 * @return the created deployment package file model.
	 */
	private DPPFileModel createDPPModel(IFile file) {
		InputStream stream = null;
		try {
			file.refreshLocal(IResource.DEPTH_INFINITE, null);
			stream = file.getContents();
			lastModified = getLastModified(file);
		} catch (CoreException e) {
			DPPErrorHandler.processError(e, false);
			return null;
		}

		model = getModel(file);
		if (model == null) {
			model = new DPPFileModel(file);
		}

		DPPFile dppFile = ((DPPFileModel) model).getDPPFile();
		dppFile.restoreFromFile();

		try {
			stream.close();
		} catch (IOException e) {
			DPPErrorHandler.processError(e, false);
		}
		return (DPPFileModel) model;
	}

	/*
	 * Initializes the editor model with a given input.
	 * 
	 * @see
	 * org.tigris.mtoolkit.dpeditor.editor.base.DPPMultiPageEditor#initializeModels
	 * (java.lang.Object)
	 */
	protected void initializeModels(Object model) {
		super.initializeModels(model);
	}

	/*
	 * Updates the model of this editor.
	 * 
	 * @see
	 * org.tigris.mtoolkit.dpeditor.editor.base.DPPMultiPageEditor#updateModel()
	 */
	protected boolean updateModel() {
		IDocument document = getDocumentProvider().getDocument(getEditorInput());
		boolean cleanModel = true;
		if (document == null)
			return cleanModel;
		String text = document.get();

		try {
			InputStream stream = new ByteArrayInputStream(text.getBytes("UTF8"));
			((DPPFileModel) model).getDPPFile().restoreFromFile();
			getCurrentPage().update();
			try {
				stream.close();
			} catch (IOException e) {
				DPPErrorHandler.processError(e, false);
			}
		} catch (UnsupportedEncodingException e) {
			DPPErrorHandler.processError(e, false);
		}
		return cleanModel;
	}

	/*
	 * Checks if the given object is the instance of the model that editor works
	 * with and if this model was changed.
	 * 
	 * @see
	 * org.tigris.mtoolkit.dpeditor.editor.base.DPPMultiPageEditor#isModelDirty
	 * (java.lang.Object)
	 */
	protected boolean isModelDirty(Object model) {
		return model != null && model instanceof DPPFileModel && ((DPPFileModel) model).isDirty();
	}

	/*
	 * Checks correctness of the entered data in the editor.
	 * 
	 * @see
	 * org.tigris.mtoolkit.dpeditor.editor.base.DPPMultiPageEditor#checkCorrectness
	 * ()
	 */
	protected void checkCorrectness() throws InconsistentDataException {
		try {
			getDPPFile().checkConsistency();
		} catch (Exception e) {
			DPPErrorHandler.processError(e);
			DPPUtil.showErrorDialog(DPPErrorHandler.getAnyShell(), ResourceManager.getString(ERROR_MSG), ResourceManager.getString("DPPEditor.TabError"), e.getMessage(), e);
		}
	}

	/**
	 * Calls the <code>update</code> method of the <code>HeadersPage</code>.
	 */
	public void updateHeadersPage() {
		headersPage.update();
	}

	/**
	 * Returns the time that the given file was last modified.
	 * 
	 * @param file
	 *            the file that last modified will be returned
	 * @return A <code>long</code> value representing the time the file was last
	 *         modified
	 */
	private long getLastModified(IFile file) {
		try {
			file.refreshLocal(IResource.DEPTH_ZERO, null);
			return file.getLocation().toFile().lastModified();
		} catch (CoreException ex) {
			DPPErrorHandler.processError(ex, false);
			return lastModified;
		}
	}

	/*
	 * Sent when a shell becomes the active window and revert the file if
	 * needed.
	 * 
	 * @see
	 * org.eclipse.swt.events.ShellListener#shellActivated(org.eclipse.swt.events
	 * .ShellEvent)
	 */
	public void shellActivated(ShellEvent e) {
		if (e.getSource().equals(DPPErrorHandler.getShell())) {
			checkModified();
		}
	}

	/**
	 * Checks if the time that the file was last modified is equal of the
	 * current save last modified value. If there are no equals values the
	 * question dialog ask to revert to the saved file appear.
	 */
	private void checkModified() {
		if (!activated) {
			DPPFileModel model = (DPPFileModel) getModel();
			IFile file = (IFile) getEditorInput().getAdapter(IFile.class);
			if (file != null) {
				if (file.exists()) {
					long lm = getLastModified(file);
					if (lastModified == lm) {
						return;
					} else if (lm == model.getModelModified()) {
						lastModified = lm;
						activated = false;
						return;
					}
					activated = true;
					boolean res = MessageDialog.openQuestion(DPPErrorHandler.getShell(), ResourceManager.getString(FILE_MODIFIED, ""), ResourceManager.format(FILE_MODIFIED_MESSAGE, new Object[] { file.getName() }));
					if (res) {
						updateModel();
						model.setDirty(false);
						activated = false;
					} else {
						model.setDirty(true);
						updateDocument();
						activated = false;
					}
					lastModified = lm;
				} else {
					activated = false;
				}
			}
		}
	}

	/**
	 * Disposes all created in this editor pages.
	 */
	public void disposeAllPages() {
		if (bundlesPage == null) {
			return;
		}
		bundlesPage.dispose();
		resourcesPage.dispose();
		certificatesPage.dispose();
		headersPage.dispose();
		buildPage.dispose();
		mfPage.dispose();
		bundlesPage = null;
		resourcesPage = null;
		certificatesPage = null;
		headersPage = null;
		buildPage = null;
		mfPage = null;
	}

	/**
	 * Gets the <code>IFile</code> from the deployment package file model.
	 * 
	 * @return the file or <code>null</code> if there are no deployment package
	 *         file model
	 */
	public IFile getFile() {
		DPPFileModel model = (DPPFileModel) getModel();
		if (model == null) {
			return null;
		} else {
			return model.getFile();
		}
	}

	/**
	 * Gets from the deployment package file model the deployment package file.
	 * 
	 * @return the deployment package file or <code>null</code> if there are no
	 *         deployment package file model
	 */
	public DPPFile getDPPFile() {
		DPPFileModel model = (DPPFileModel) getModel();
		if (model == null) {
			return null;
		} else {
			return model.getDPPFile();
		}
	}

	/*
	 * Closes this editor and depends on the given save flag saves the changes
	 * made in the editor.
	 * 
	 * @see
	 * org.tigris.mtoolkit.dpeditor.editor.base.DPPMultiPageEditor#close(boolean
	 * )
	 */
	public void close(final boolean save) {
		super.close(save);
		removeDPPEditor(this);
	}

	/*
	 * Disposes all created in this editor elements.
	 * 
	 * @see
	 * org.tigris.mtoolkit.dpeditor.editor.base.DPPMultiPageEditor#dispose()
	 */
	public void dispose() {
		JavaCore.removeElementChangedListener(this);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		removeDPPEditor(this);
		super.dispose();
	}

	/*
	 * Notifies that one or more attributes of one or more Java elements have
	 * changed. The details of the change are described by the given event.
	 * 
	 * @see
	 * org.eclipse.jdt.core.IElementChangedListener#elementChanged(org.eclipse
	 * .jdt.core.ElementChangedEvent)
	 */
	public void elementChanged(ElementChangedEvent event) {
		IJavaElementDelta delta = (IJavaElementDelta) event.getSource();
		if (delta.getKind() == IJavaElementDelta.CHANGED) {
			if (isRenamed) {
				isRenamed = false;
				return;
			}
			IPath resourceTo = null;
			IPath resourceFrom = null;
			IJavaElement elementFrom = null;
			IJavaElement elementTo = null;
			IJavaElementDelta[] affected = delta.getAffectedChildren();
			boolean isFolder = false;
			if (affected.length > 0) {
				for (int i = 0; i < affected.length; i++) {
					IJavaElementDelta element = affected[i];
					IResourceDelta[] resources = element.getResourceDeltas();
					if (affected.length == 1) {
						boolean dive = true; // continue traversing down the
						// delta tree
						if (resources != null && resources.length > 0) {
							for (int j = 0; j < resources.length; j++) {
								IResource r = resources[j].getResource();
								if (r.equals(dppFile)) {
									dive = false;
									break; // the resources contains the
									// manifest file, stop
									// diving
								}
							}
						}
						if (dive) {
							affected = element.getAffectedChildren();
							i = -1;
							continue;
						}
					}
					if (resources != null) {
						IPath[] paths = getResources(resources);
						isFolder = (paths != null);
						if (isFolder) {
							resourceFrom = paths[0];
							resourceTo = paths[1];
							break;
						} else {
							isFolder = true;
							affected = element.getRemovedChildren();
							if ((affected != null) && (affected.length > 0)) {
								if (affected[0].getMovedFromElement() != null) {
									resourceTo = affected[0].getMovedToElement().getPath();
								}
							}
							affected = element.getAddedChildren();
							if ((affected != null) && (affected.length > 0)) {
								if (affected[0].getMovedFromElement() != null) {
									resourceFrom = affected[0].getMovedFromElement().getPath();
								}
							}
							break;
						}
					}
					if (element.getFlags() == IJavaElementDelta.F_MOVED_FROM) {
						elementFrom = element.getMovedFromElement();
					} else if (element.getFlags() == IJavaElementDelta.F_MOVED_TO) {
						elementTo = element.getMovedToElement();
					}
				}
			} else {
				IResourceDelta[] resources = delta.getResourceDeltas();
				if (resources != null) {
					IPath[] paths = getResources(resources);
					isFolder = (paths != null);
					if (isFolder) {
						resourceFrom = paths[0];
						resourceTo = paths[1];
						if ((resourceFrom != null) && (resourceTo != null)) {
							IPath pp = dppFile.getFullPath();
							String mp = dppFile.getProject().getLocation().toOSString();
							File file = new File(mp + File.separatorChar + resourceTo.removeFirstSegments(1).toOSString());
							boolean isFile = false;
							if (!(isFile = file.isFile())) {
								pp = pp.removeLastSegments(1);
							}
							boolean hasToRelocate = true;
							for (int i = 0; i < resourceFrom.segmentCount(); i++) {
								if (!resourceFrom.segment(i).equals(pp.segment(i))) {
									hasToRelocate = false;
									break;
								}
							}
							if (!hasToRelocate)
								return;
							IFolder iFolder = null;
							String fileName = dppFile.getName();
							if (resourceFrom.segmentCount() == 1) {
								IProject prj = ResourcesPlugin.getWorkspace().getRoot().getProject(resourceFrom.lastSegment());
								IPath path = pp.removeFirstSegments(1);
								iFolder = prj.getFolder(path);
							} else {
								pp = pp.removeFirstSegments(resourceTo.segmentCount());
								IPath newPath = resourceTo.removeFirstSegments(1);
								if (isFile) {
									fileName = newPath.lastSegment();
									newPath = newPath.removeLastSegments(1).append(pp.removeLastSegments(1));
								} else {
									newPath = newPath.append(pp);
								}
								IProject prj = dppFile.getProject();
								iFolder = prj.getFolder(newPath);
							}
							IFile newDPPFile = iFolder.getFile(fileName);
						}
					}
				}
				return;
			}
			if (isFolder) {
				if ((resourceTo != null) && (resourceFrom != null)) {
					IPath pp = dppFile.getFullPath();
					boolean isFile = false;
					String mp = dppFile.getProject().getLocation().toOSString();
					File file = new File(mp + File.separatorChar + resourceTo.removeFirstSegments(1).toOSString());
					if (!(isFile = file.isFile())) {
						pp = pp.removeLastSegments(1);
					}
					boolean hasToRelocate = true;
					for (int i = 0; i < resourceFrom.segmentCount(); i++) {
						if (!resourceFrom.segment(i).equals(pp.segment(i))) {
							hasToRelocate = false;
							break;
						}
					}
					if (!hasToRelocate)
						return;
					pp = pp.removeFirstSegments(resourceFrom.segmentCount());
					IProject prj = dppFile.getProject();
					IPath iPath = resourceTo.removeFirstSegments(1);
					String fileName = dppFile.getName();
					if (isFile) {
						fileName = iPath.lastSegment();
						iPath = iPath.removeLastSegments(1).append(pp.removeLastSegments(1));
					} else {
						iPath = iPath.append(pp);
					}
					IFile newDPPFile;
					if (iPath.isEmpty()) {
						newDPPFile = prj.getFile(fileName);
					} else {
						IFolder iFolder = prj.getFolder(iPath);
						newDPPFile = iFolder.getFile(fileName);
					}
				}
			} else {
				if ((dppFile != null) && (elementTo != null) && (elementFrom != null)) {
					if (!dppFile.getProject().getName().equals(elementFrom.getElementName()))
						return;
					String newPrj = elementTo.getElementName();
					IPath path = dppFile.getFullPath().removeLastSegments(1).removeFirstSegments(1);
					IProject prj = ResourcesPlugin.getWorkspace().getRoot().getProject(newPrj);
					IFolder iFolder = prj.getFolder(path);
					IFile newDPPFile = iFolder.getFile(dppFile.getName());
				}
			}
		}
	}

	/**
	 * Gets for the given resource delta the paths. The paths array is the two
	 * elements array. The first element contains the path from which the
	 * resource will be moved. The second element is the path to which the
	 * resource will be moved.
	 * 
	 * @param resources
	 *            the resource delta for which will be gets the path array
	 * @return the path array for the given resource delta array
	 */
	private IPath[] getResources(IResourceDelta[] resources) {
		if (resources == null)
			return null;
		IPath resourceFrom = null;
		IPath resourceTo = null;
		int br = 0;
		if (resources.length == 2) {
			for (int i = 0; i < resources.length; i++) {
				IResourceDelta resource = resources[i];
				if (resource.getKind() == IResourceDelta.ADDED) {
					resourceFrom = resource.getMovedFromPath();
					br++;
				} else if (resource.getKind() == IResourceDelta.REMOVED) {
					resourceTo = resource.getMovedToPath();
					br++;
				}
			}
			if (br < 1)
				return null;
			IPath[] paths = new IPath[2];
			paths[0] = resourceFrom;
			paths[1] = resourceTo;
			return paths;
		} else if (resources.length == 1) {
			return getResources(resources[0].getAffectedChildren());
		}
		return null;
	}

	/**
	 * Sets all editors, in which the given file is open to become dirty or not,
	 * depending of the given flag.
	 * 
	 * @param dppFile
	 *            the file, which editors will become dirty
	 * @param flag
	 *            the <code>boolean</code> flag, that shows if editor will
	 *            become dirty or not
	 */
	public static void setEditorDirty(IFile dppFile, boolean flag) {
		Vector dppEditors = DPPEditor.getDPPEditors(dppFile);
		if (dppEditors != null) {
			for (int i = 0; i < dppEditors.size(); i++) {
				DPPEditor nextEditor = (DPPEditor) dppEditors.elementAt(i);
				DPPFileModel base = (DPPFileModel) nextEditor.getModel();
				base.setDirty(flag);
			}
		}
	}

	/**
	 * Opens a standard error dialog with the given message.
	 * 
	 * @param message
	 *            the message
	 */
	public static void showErrorDialog(final String message) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (!display.isDisposed()) {
			display.asyncExec(new Runnable() {
				public void run() {
					MessageDialog.openError(DPPErrorHandler.getAnyShell(), ResourceManager.getString(ERROR_MSG, ""), message);
				}
			});
		}
	}

	/*
	 * Sets the given editor page to be selected in the editor.
	 * 
	 * @see
	 * org.tigris.mtoolkit.dpeditor.editor.base.DPPMultiPageEditor#formSelected
	 * (org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage)
	 */
	public void formSelected(IFormPage page) {
		if (page instanceof DPPFormPage || (page instanceof SourceFormPage || page instanceof ManifestSourceFormPage)) {
			IDPPEditorPage oldPage = getOldPage();
			if (!formSelection) {
				try {
					if (getDPPFile() == null) {
						showPage(oldPage);
						if (!DPPEditor.isDialogShown) {
							showErrorDialog(ResourceManager.getString("DPPEditor.EmptyFileMessage"));
						}
						return;
					}
					getDPPFile().checkConsistency();
				} catch (Exception e) {
					formSelection = true;
					if (oldPage != null) {
						showPage(oldPage);
						if (!DPPEditor.isDialogShown) {
							DPPEditor.isDialogShown = true;
							DPPErrorHandler.showErrorTableDialog(e.getMessage());
						}
					} else {
						showPage((IDPPEditorPage) page);
					}
					DPPEditor.isDialogShown = false;
					return;
				}
			}
			if (page instanceof DPPFormPage) {
				DPPFormPage formPage = (DPPFormPage) page;
				formSelection = false;
				if (formPage.getSelection() != null) {
					setSelection(formPage.getSelection());
				}
			}
		}
	}

	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		try {
			delta.accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) {
					try {
						IResource res = delta.getResource();
						if (delta.getKind() == IResourceDelta.ADDED && delta.getFlags() == IResourceDelta.MOVED_FROM) {
							IPath movedFrom = delta.getMovedFromPath();
							if (!(delta.getResource() instanceof IFile) || !((IFile) delta.getResource()).getName().endsWith(".dpp") || delta.getMovedFromPath() == null) {
								return true;
							}

							IFile iFile = (IFile) delta.getResource();
							Vector allEditors = getAllDPPEditors();
							for (int i = 0; i < allEditors.size(); i++) {
								DPPEditor editor = (DPPEditor) allEditors.elementAt(i);
								String editorFileName = editor.getFile().getLocation().toOSString();
								String loc = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
								if (editorFileName.startsWith(loc)) {
									editorFileName = editorFileName.substring(loc.length());
								}
								String checkFileName = movedFrom.toOSString();

								if (editorFileName.equals(checkFileName)) {
									IEditorInput oldEditorInput = editor.getEditorInput();

									editor.getDocumentProvider().disconnect(editor.getEditorInput());
									IAnnotationModel amodel1 = editor.getDocumentProvider().getAnnotationModel(oldEditorInput);
									if (amodel1 != null) {
										amodel1.disconnect(editor.getDocumentProvider().getDocument(oldEditorInput));
									}
									removeDPPEditor(editor);

									((DPPFileModel) editor.getModel()).setFile((IFile) iFile);
									FileEditorInput newEditorInput = new FileEditorInput(iFile);
									editor.setInput(newEditorInput);
									editor.setTitleToolTip(res.getFullPath().toOSString());
									editor.setPartName(iFile.getName());
									addDPPEditor(editor);
									try {
										editor.getDocumentProvider().connect(newEditorInput);
										IAnnotationModel amodel = editor.getDocumentProvider().getAnnotationModel(newEditorInput);
										if (amodel != null) {
											amodel.connect(editor.getDocumentProvider().getDocument(newEditorInput));
										}
									} catch (CoreException e) {
										DPPErrorHandler.processError(e, false);
									}
									break;
								}
							}
						}
					} catch (Throwable t) {
						t.printStackTrace();
					}
					return true;
				}
			});
		} catch (CoreException e) {
			DPPErrorHandler.processError(e, false);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
