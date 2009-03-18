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
package org.tigris.mtoolkit.cdeditor.internal.integration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.util.SAXParserWrapper;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.DocumentProviderRegistry;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.tigris.mtoolkit.cdeditor.internal.CDEditorPlugin;
import org.tigris.mtoolkit.cdeditor.internal.model.IEclipseContext;
import org.tigris.mtoolkit.cdeditor.internal.model.PathFilter;
import org.tigris.mtoolkit.cdeditor.internal.model.impl.ComponentDescriptionValidator;
import org.tigris.mtoolkit.cdeditor.internal.model.impl.ValidationResult;
import org.tigris.mtoolkit.cdeditor.internal.text.BundleModelProcessor;
import org.tigris.mtoolkit.cdeditor.internal.text.PDETextModelHelper;
import org.tigris.mtoolkit.cdeditor.internal.text.PlainDocumentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * The class ComponentDescriptionBuilder is used for incrementally building
 * the small delta changes of one component description. It follows the concept
 * of incremental project building.
 */
public class ComponentDescriptionBuilder extends IncrementalProjectBuilder {

	/**
	 * Controls whether the builder will always do clean build. This means that
	 * all files are revalidated when any file in the project or in the required
	 * projects changes, all component descriptions will be validated. This is
	 * not the optimal solutions, but will work for now.
	 */
	private static final boolean ALWAYS_DO_CLEAN_BUILD = true;

	private static final String SERVICE_COMPONENT_HEADER = "Service-Component";

	private static final int SERVICE_COMPONENT_VALUE_OFFSET = SERVICE_COMPONENT_HEADER.length() + 2;

	private static final IPath MANIFEST_FILE = ICoreConstants.MANIFEST_PATH;

	public static final String BUILDER_ID = "org.tigris.mtoolkit.cdeditor.componentDescriptionBuilder";

	public static final String MARKER_TYPE = "org.tigris.mtoolkit.cdeditor.problem";

	private List paths = Collections.EMPTY_LIST;

	private IEclipseContext context;

	private boolean needsCleanBuild = true;

	private IProgressMonitor progressMonitor;

	private class ServiceComponentPath {
		public int offset;
		public int length;
		public int lineNumber;
		public PathFilter path;

		public ServiceComponentPath(String path, int offset, int length,
				int lineNumber) {
			this.offset = offset;
			this.length = length;
			this.lineNumber = lineNumber;

			this.path = new PathFilter(path);
		}
	}

	private void addMarker(IFile file, String message, int lineNumber, int offset, int length, int severity) {
		try {
			IMarker marker = file.createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			marker.setAttribute(IMarker.CHAR_START, offset);
			marker.setAttribute(IMarker.CHAR_END, offset + length);
			if (lineNumber != -1)
				marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
		} catch (CoreException e) {
		}
	}

	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
		this.progressMonitor = monitor;
		try {
			if (ALWAYS_DO_CLEAN_BUILD) {
				buildProject(getProject());
			} else { // old incremental code
				switch (kind) {
				case FULL_BUILD:
				case CLEAN_BUILD:
					buildProject(getProject());
					break;
				case INCREMENTAL_BUILD:
				case AUTO_BUILD:
					incrementalBuild(getDelta(getProject()));
				}
			}
		} catch (OperationCanceledException e) {
			// ignore
		} finally {
			progressMonitor = null;
		}
		return JDTModelHelper.getRequiredProjects(getProject());
	}

	private void incrementalBuild(IResourceDelta resourceDelta) throws CoreException {
		if (resourceDelta == null || needsCleanBuild) {
			buildProject(getProject());
			return;
		}
		final boolean[] needsFullRebuild = new boolean[] { false };
		final List dirtyFiles = new ArrayList();
		resourceDelta.accept(new IResourceDeltaVisitor() {
			public boolean visit(IResourceDelta delta) throws CoreException {
				if (needsFullRebuild[0])
					return false;
				if (!getProject().equals(delta.getResource().getProject()))
					return false;
				if (delta.getProjectRelativePath().equals(MANIFEST_FILE)) {
					needsFullRebuild[0] = true;
					return false;
				}
				if (delta.getProjectRelativePath().equals(ComponentProjectContext.BUILD_PROPERTIES_FILE)) {
					needsFullRebuild[0] = true;
					return false;
				}
				IPath deltaPath = delta.getProjectRelativePath();
				for (Iterator it = paths.iterator(); it.hasNext();) {
					PathFilter componentPath = ((ServiceComponentPath) it.next()).path;
					if (componentPath.matchPath(deltaPath)) {
						dirtyFiles.add(deltaPath);
					}
				}
				return true;
			}
		});
		if (needsFullRebuild[0]) {
			buildProject(getProject());
		} else {
			IProject prj = getProject();
			if (dirtyFiles.size() > 0)
				validateManifest(prj);
			checkBuildCanceled();
			for (Iterator it = dirtyFiles.iterator(); it.hasNext();) {
				IPath filePath = (IPath) it.next();
				IFile file = prj.getFile(filePath);
				getMonitor().subTask("Validating file " + file.getProjectRelativePath());
				buildFile(file);
			}
		}
	}

	private void deleteMarkers(IFile file) {
		try {
			file.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);
		} catch (CoreException ce) {
		}
	}

	private void buildProject(IProject prj) throws CoreException {
		cleanProject(prj);

		IFile[] referencedFiles = validateManifest(prj);
		if (referencedFiles == null)
			return;
		checkBuildCanceled();
		for (int i = 0; i < referencedFiles.length; i++) {
			getMonitor().subTask("Validating " + referencedFiles[i].getProjectRelativePath());
			buildFile(referencedFiles[i]);
		}

		needsCleanBuild = false; // mark as fully build
	}

	private IFile[] validateManifest(IProject prj) throws CoreException {
		IProgressMonitor monitor = getMonitor();

		deleteMarkers(prj.getFile(ICoreConstants.MANIFEST_PATH));
		List referencedFiles = new ArrayList();

		monitor.subTask("Parsing manifest");
		paths = findComponentDescription(prj);
		checkBuildCanceled();
		monitor.subTask("Validate manifest entries");

		for (Iterator it = paths.iterator(); it.hasNext();) {
			ServiceComponentPath componentPath = (ServiceComponentPath) it.next();
			checkBuildCanceled();
			if (!componentPath.path.isValid()) {
				addMarker(getBundleManifest(prj), "Service components paths must point to files.", componentPath.lineNumber + 1, componentPath.offset, componentPath.length, IMarker.SEVERITY_ERROR);
				continue;
			}
			IFile[] files = getProjectContext().findBundleEntries(componentPath.path);
			if (files.length == 0) {
				if (componentPath.path.isWildcard()) {
					addMarker(getBundleManifest(prj), "Wildcard path '" + componentPath.path + "' cannot match any file.", componentPath.lineNumber + 1, componentPath.offset, componentPath.length, IMarker.SEVERITY_ERROR);
				} else {
					addMarker(getBundleManifest(prj), "Referenced file '" + componentPath.path + "' cannot be found.", componentPath.lineNumber + 1, componentPath.offset, componentPath.length, IMarker.SEVERITY_ERROR);
				}
			} else {
				for (int i = 0; i < files.length; i++)
					referencedFiles.add(files[i]);
			}
		}
		return (IFile[]) referencedFiles.toArray(new IFile[referencedFiles.size()]);
	}

	private void buildFile(IFile description) throws CoreException {
		if (!description.exists())
			return; // do nothing for non-existing files
		checkBuildCanceled();
		deleteMarkers(description); // clear any markers before building
		IEditorInput input = new FileEditorInput(description);
		IDocumentProvider provider = DocumentProviderRegistry.getDefault().getDocumentProvider(input);
		provider.connect(description);
		try {
			IDocument document = provider.getDocument(description);
			List result = validateDescription(document);
			applyValidationResults(result, description, document);
		} finally {
			provider.disconnect(description);
		}

		if (!getProjectContext().isBundleFilePackaged(description.getProjectRelativePath())) {
			addMarker(description, "File is referenced in the manifest, but will be excluded when the plug-in is packaged.", 1, 1, 1, IMarker.SEVERITY_WARNING);
		}
	}

	private void applyValidationResults(List result, IFile description, IDocument document) {
		for (Iterator it = result.iterator(); it.hasNext();) {
			ValidationResult valResult = (ValidationResult) it.next();
			try {
				int line;
				if (valResult.getOffset() == -1) {
					line = document.getLineOfOffset(valResult.getElement().getOffset());
				} else {
					line = document.getLineOfOffset(valResult.getOffset());
				}
				addMarker(description, valResult.getStatus().getMessage(), line + 1, valResult.getOffset(), valResult.getLength(), getSeverity(valResult.getStatus()));
			} catch (BadLocationException e) {
				CDEditorPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, CDEditorPlugin.PLUGIN_ID, "Error occurred while generating markers", e));
			}
		}
	}

	private void cleanProject(IProject prj) throws CoreException {
		prj.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_INFINITE);
		paths.clear(); // clear the component descriptions list
	}

	private int getSeverity(IStatus status) {
		switch (status.getSeverity()) {
		case IStatus.ERROR:
			return IMarker.SEVERITY_ERROR;
		case IStatus.WARNING:
			return IMarker.SEVERITY_WARNING;
		default:
			return IMarker.SEVERITY_INFO;
		}
	}

	private List validateDescription(IDocument document) {
		List result = new ArrayList();

		InputStream in = new ByteArrayInputStream(document.get().getBytes());
		try {
			SAXParserWrapper parser = new SAXParserWrapper();
			PlainDocumentHandler documentHandler = new PlainDocumentHandler(document, true);
			parser.parse(in, documentHandler);

			result.addAll(new ComponentDescriptionValidator(getProjectContext()).validateDocument(documentHandler.getRoot()));
		} catch (IOException e) {
		} catch (ParserConfigurationException e) {
		} catch (FactoryConfigurationError e) {
		} catch (SAXException e) {
			if (e instanceof SAXParseException) {
				result.add(new ValidationResult(new Status(IStatus.ERROR, CDEditorPlugin.PLUGIN_ID, "Component description is not valid XML document. " + e.getMessage()), null, 1, 1));
			}
		}
		return result;
	}

	private IFile getBundleManifest(IProject prj) {
		IPluginModelBase pluginBase = PDECore.getDefault().getModelManager().findModel(prj);
		if (pluginBase instanceof IBundlePluginModelBase) {
			IBundlePluginModelBase bundleBase = (IBundlePluginModelBase) pluginBase;
			IFile manifest = (IFile) bundleBase.getBundleModel().getUnderlyingResource();
			return manifest;
		}
		return null;
	}

	private List findComponentDescription(IProject prj) {
		try {
			return (List) PDETextModelHelper.processBundleModel(prj, new BundleModelProcessor() {

				public Object processBundleModel(IBundleModel model, IDocument document, IProgressMonitor monitor) {
					IManifestHeader header = model.getBundle().getManifestHeader(SERVICE_COMPONENT_HEADER);
					if (header == null)
						return Collections.EMPTY_LIST;

					int offset = header.getOffset() + SERVICE_COMPONENT_VALUE_OFFSET;
					try {
						String value = document.get(offset, header.getLength() - SERVICE_COMPONENT_VALUE_OFFSET);
						List files = new ArrayList();
						StringTokenizer tokenizer = new StringTokenizer(value, ",");
						while (tokenizer.hasMoreTokens()) {
							String rawPath = tokenizer.nextToken();
							String cdPath = rawPath.trim();
							int descriptionLine = -1;
							try {
								descriptionLine = document.getLineOfOffset(offset);
							} catch (BadLocationException e) {
							}
							int valueOffset = offset + rawPath.indexOf(cdPath);
							files.add(new ServiceComponentPath(cdPath, valueOffset, cdPath.length(), descriptionLine));
							offset += rawPath.length() + 1; // + 1 for ','
						}
						return files;
					} catch (BadLocationException e1) {
						CDEditorPlugin.log(e1);
						return Collections.EMPTY_LIST;
					}
				}

			}, getMonitor());
		} catch (CoreException e) {
			CDEditorPlugin.log(e);
			return Collections.EMPTY_LIST;
		}
	}

	private IEclipseContext getProjectContext() {
		if (context == null)
			context = (IEclipseContext) getProject().getAdapter(IEclipseContext.class);
		return context;
	}

	private IProgressMonitor getMonitor() {
		return progressMonitor;
	}

	private void checkBuildCanceled() {
		IProgressMonitor monitor = getMonitor();
		if (monitor != null && monitor.isCanceled())
			throw new OperationCanceledException();
	}
}
