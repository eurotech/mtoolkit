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
package org.tigris.mtoolkit.cdeditor.internal.text;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.text.AbstractEditingModel;
import org.eclipse.pde.internal.core.text.IModelTextChangeListener;
import org.eclipse.pde.internal.core.text.build.BuildModel;
import org.eclipse.pde.internal.core.text.build.PropertiesTextChangeListener;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.BundleTextChangeListener;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.tigris.mtoolkit.cdeditor.internal.CDEditorPlugin;


// XXX: Most part of this class is taken directly from the PDE code repository. The corresponding class is org.eclipse.pde.internal.ui.util.PDEModelUtility
public class PDETextModelHelper {

	private static IBundleModel prepareBundleEditingModel(IFile file) throws CoreException {
		IDocument document = getDocument(file);
		BundleModel model = new BundleModel(document, true);
		model.load();
		model.setUnderlyingResource(file);
		model.addModelChangedListener(new BundleTextChangeListener(document, false));
		return model;
	}

	private static IBuildModel prepareBuildEditingModel(IFile file) throws CoreException {
		IDocument document = getDocument(file);
		BuildModel model = new BuildModel(document, true);
		model.load();
		model.setUnderlyingResource(file);
		model.addModelChangedListener(new PropertiesTextChangeListener(document, false));
		return model;
	}

	public static Object processBuildModel(IProject prj, BuildModelProcessor processor, IProgressMonitor monitor) throws CoreException {
		IPluginModelBase pluginBase = PDECore.getDefault().getModelManager().findModel(prj);
		if (pluginBase instanceof IBundlePluginModelBase) {
			IFile buildProperties = (IFile) ((IBundlePluginModelBase) pluginBase).getBuildModel().getUnderlyingResource();
			return processBuildModel(buildProperties, processor, monitor);
		} else {
			CDEditorPlugin.newCoreException(IStatus.ERROR, "Selected project '" + prj + "' is not valid plug-in project.", null);
			// fool the compiler
			return null;
		}
	}

	private static void connectFile(IFile file, IProgressMonitor monitor) throws CoreException {
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		manager.connect(file.getFullPath(), LocationKind.IFILE, monitor);
	}

	private static void disconnectFile(IFile file, IProgressMonitor monitor) {
		try {
			FileBuffers.getTextFileBufferManager().disconnect(file.getFullPath(), LocationKind.IFILE, monitor);
		} catch (CoreException e) {
			CDEditorPlugin.log(e);
		}
	}

	private static void applyModelChanges(IModel model, IProgressMonitor monitor) throws CoreException {
		if (model instanceof AbstractEditingModel) {
			IFile file = (IFile) model.getUnderlyingResource();
			if (file == null) {
				CDEditorPlugin.newCoreException(IStatus.ERROR, "The model wasn't properly built. Its underlying resource is missing.", null);
				return;
			}
			ITextFileBuffer buffer = FileBuffers.getTextFileBufferManager().getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
			IDocument document = buffer.getDocument();
			IModelTextChangeListener listener = ((AbstractEditingModel) model).getLastTextChangeListener();
			TextEdit[] edits = listener.getTextOperations();
			if (edits.length > 0) {
				MultiTextEdit multiEdit = new MultiTextEdit();
				multiEdit.addChildren(edits);
				try {
					multiEdit.apply(document);
				} catch (MalformedTreeException e) {
					CDEditorPlugin.newCoreException(IStatus.ERROR, "Failed to apply changes to document", e);
				} catch (BadLocationException e) {
					CDEditorPlugin.newCoreException(IStatus.ERROR, "Failed to apply changes to document", e);
				}
				buffer.commit(monitor, true);
			}
		}
	}

	private static IDocument getDocument(IFile file) {
		ITextFileBuffer buffer = FileBuffers.getTextFileBufferManager().getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
		if (buffer != null)
			return buffer.getDocument();
		return null;
	}

	public static Object processBuildModel(IFile buildProperties, BuildModelProcessor processor, IProgressMonitor monitor) throws CoreException {
		checkFile(buildProperties);
		connectFile(buildProperties, monitor);
		try {
			IBuildModel editingModel = prepareBuildEditingModel(buildProperties);

			Object result = processor.processBuildModel(editingModel, getDocument(buildProperties), monitor);

			applyModelChanges(editingModel, monitor);

			return result;
		} finally {
			disconnectFile(buildProperties, monitor);
		}
	}

	private static void checkFile(IFile file) throws CoreException {
		if (!file.exists())
			CDEditorPlugin.newCoreException(IStatus.ERROR, "Selected file '" + file + "' doesn't exists.", null);
	}

	public static Object processBundleModel(IProject prj, BundleModelProcessor processor, IProgressMonitor monitor) throws CoreException {
		IPluginModelBase pluginBase = PDECore.getDefault().getModelManager().findModel(prj);
		if (pluginBase instanceof IBundlePluginModelBase) {
			IFile manifest = (IFile) ((IBundlePluginModelBase) pluginBase).getBundleModel().getUnderlyingResource();
			return processBundleModel(manifest, processor, monitor);
		} else {
			CDEditorPlugin.newCoreException(IStatus.ERROR, "Selected project '" + prj + "' is not valid plug-in project.", null);
			// fool the compiler
			return null;
		}
	}

	public static Object processBundleModel(IFile manifest, BundleModelProcessor processor, IProgressMonitor monitor) throws CoreException {
		checkFile(manifest);
		connectFile(manifest, monitor);
		try {
			IBundleModel editingModel = (IBundleModel) prepareBundleEditingModel(manifest);

			Object result = processor.processBundleModel(editingModel, getDocument(manifest), monitor);

			applyModelChanges(editingModel, monitor);
			return result;
		} finally {
			disconnectFile(manifest, monitor);
		}
	}

}
