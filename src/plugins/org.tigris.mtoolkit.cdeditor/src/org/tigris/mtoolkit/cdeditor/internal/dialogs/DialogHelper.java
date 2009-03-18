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
package org.tigris.mtoolkit.cdeditor.internal.dialogs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.dialogs.FilteredTypesSelectionDialog;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.dialogs.TypeSelectionExtension;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.tigris.mtoolkit.cdeditor.internal.CDEditorPlugin;
import org.tigris.mtoolkit.cdeditor.internal.integration.BundleJavaSearchScope;
import org.tigris.mtoolkit.cdeditor.internal.integration.EclipseHelper;
import org.tigris.mtoolkit.cdeditor.internal.providers.BundleEntriesContentProvider;

/**
 * This class provides convenient methods for opening different browse dialogs
 */
public class DialogHelper {

	public static String openBrowseTypeDialog(Shell parent, String title, IJavaProject context) {
		return openBrowseTypeDialog(parent, title, context, "");
	}

	public static String openBrowseTypeDialog(Shell parent, String title, IJavaProject context, String filter) {
		return internalOpenBrowseTypeDialog(parent, title, context, IJavaElementSearchConstants.CONSIDER_CLASSES_AND_INTERFACES, filter);
	}

	public static String openBrowseInterfaceDialog(Shell parent, String title, IJavaProject context) {
		return openBrowseInterfaceDialog(parent, title, context, "");
	}

	public static String openBrowseInterfaceDialog(Shell parent, String title, IJavaProject context, String filter) {
		return internalOpenBrowseTypeDialog(parent, title, context, IJavaElementSearchConstants.CONSIDER_INTERFACES, filter);
	}

	public static String openBrowseClassDialog(Shell parent, String title, IJavaProject context) {
		return openBrowseClassDialog(parent, title, context, "");
	}

	public static String openBrowseClassDialog(Shell parent, String title, IJavaProject context, String filter) {
		return internalOpenBrowseTypeDialog(parent, title, context, IJavaElementSearchConstants.CONSIDER_CLASSES, filter);
	}

	private static String internalOpenBrowseTypeDialog(Shell parent, String title, IJavaProject context, int style, String filter) {
		// SelectionDialog dialog = JavaUI.createTypeDialog(parent.getShell(),
		// null, new BundleJavaSearchScope(context), considerStyle, false);
		int elementKinds = 0;
		if (style == IJavaElementSearchConstants.CONSIDER_ALL_TYPES) {
			elementKinds = IJavaSearchConstants.TYPE;
		} else if (style == IJavaElementSearchConstants.CONSIDER_INTERFACES) {
			elementKinds = IJavaSearchConstants.INTERFACE;
		} else if (style == IJavaElementSearchConstants.CONSIDER_CLASSES) {
			elementKinds = IJavaSearchConstants.CLASS;
		} else if (style == IJavaElementSearchConstants.CONSIDER_ANNOTATION_TYPES) {
			elementKinds = IJavaSearchConstants.ANNOTATION_TYPE;
		} else if (style == IJavaElementSearchConstants.CONSIDER_ENUMS) {
			elementKinds = IJavaSearchConstants.ENUM;
		} else if (style == IJavaElementSearchConstants.CONSIDER_CLASSES_AND_INTERFACES) {
			elementKinds = IJavaSearchConstants.CLASS_AND_INTERFACE;
		} else if (style == IJavaElementSearchConstants.CONSIDER_CLASSES_AND_ENUMS) {
			elementKinds = IJavaSearchConstants.CLASS_AND_ENUM;
		} else {
			throw new IllegalArgumentException("Invalid style constant."); //$NON-NLS-1$
		}
		CenteredTypesSelectionDialog dialog = new DialogHelper.CenteredTypesSelectionDialog(parent.getShell(), false, null, new BundleJavaSearchScope(context), elementKinds, null);
		dialog.setMessage(JavaUIMessages.JavaUI_defaultDialogMessage);
		dialog.setInitialPattern((filter == null) ? "" : filter);
		dialog.setTitle(title);

		dialog.open();
		if (dialog.getResult() != null) {
			IType type = (IType) dialog.getResult()[0];
			return type.getFullyQualifiedName('$');
		}
		return null;
	}

	public static String openBrowseResourceDialog(Shell parent, String title, IProject context) {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(parent, new WorkbenchLabelProvider(), new BundleEntriesContentProvider());
		dialog.setAllowMultiple(false);
		dialog.setInput(context);
		dialog.setEmptyListMessage("No entries available");
		dialog.setTitle(title);
		dialog.setValidator(new ISelectionStatusValidator() {

			public IStatus validate(Object[] selection) {
				if (selection != null && selection.length > 0 && !(selection[0] instanceof IFile))
					return CDEditorPlugin.newStatus(IStatus.ERROR, "");
				return CDEditorPlugin.newStatus(IStatus.OK, "");
			}

		});
		dialog.open();
		if (dialog.getResult() != null) {
			IResource result = (IResource) dialog.getResult()[0];
			return result.getProjectRelativePath().toString();
		}
		return null;
	}

	public static void openSetupDescriptionValidationDialog(Shell parent) {
		IProject[] inputProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		ListSelectionDialog dialog = new ListSelectionDialog(parent, inputProjects, new ArrayContentProvider(), new WorkbenchLabelProvider(), "Select projects to enable component description validation:");
		dialog.setTitle("Component Description Validation");

		List initialValidated = getValidatedProjects(inputProjects);
		dialog.setInitialElementSelections(initialValidated);

		if (dialog.open() == Window.OK) {
			Object[] projects = dialog.getResult();
			for (int i = 0; i < projects.length; i++) {
				if (initialValidated.contains(projects[i]))
					initialValidated.remove(projects[i]);
				EclipseHelper.enableDescriptionValidation((IProject) projects[i], true);
			}
			for (Iterator it = initialValidated.iterator(); it.hasNext();) {
				IProject validatedProject = (IProject) it.next();
				EclipseHelper.enableDescriptionValidation(validatedProject, false);
			}
		}
	}

	private static List getValidatedProjects(IProject[] projects) {
		List result = new ArrayList(projects.length);
		for (int i = 0; i < projects.length; i++) {
			if (EclipseHelper.isDescriptionValidationEnabled(projects[i]))
				result.add(projects[i]);
		}
		return result;
	}

	public static String suggestReferenceName(String interfaceName) {
		int idx = interfaceName.lastIndexOf('$');
		if (idx == -1)
			idx = interfaceName.lastIndexOf('.');
		if (idx == -1)
			return interfaceName.toLowerCase();
		else
			return interfaceName.substring(idx + 1).toLowerCase();
	}

	public static String suggestComponentImplementationClassName(String componentName) {
		if (componentName.trim().length() == 0)
			return "";
		return componentName + ".Component";
	}

	public static class CenteredTypesSelectionDialog extends
			FilteredTypesSelectionDialog {
		private static final String DIALOG_WIDTH = "DIALOG_WIDTH"; //$NON-NLS-1$
		private static final String DIALOG_HEIGHT = "DIALOG_HEIGHT"; //$NON-NLS-1$
		private int width = 600;
		private int height = 500;

		public CenteredTypesSelectionDialog(Shell shell, boolean multi,
				IRunnableContext context, IJavaSearchScope scope,
				int elementKinds, TypeSelectionExtension extension) {
			super(shell, multi, context, scope, elementKinds, extension);
		}

		protected IDialogSettings getDialogBoundsSettings() {
			String sectionName = "Main";
			Shell parent = getParentShell();
			if (parent != null) {
				String title = parent.getText();
				if (title.indexOf("Eclipse") < 0) {
					sectionName = title.replace(' ', '_');
				}
			}
			sectionName += "-" + getShell().getText().replace(' ', '_') + "_Dialog_Bounds";

			IDialogSettings settings = getDialogSettings();
			IDialogSettings section = settings.getSection(sectionName);
			if (section == null) {
				section = settings.addNewSection(sectionName);
				section.put(DIALOG_HEIGHT, height);
				section.put(DIALOG_WIDTH, width);
			}
			return section;
		}

		public int getWidth() {
			return width;
		}

		public void setWidth(int width) {
			this.width = width;
		}

		public int getHeight() {
			return height;
		}

		public void setHeight(int height) {
			this.height = height;
		}
	}

	/*
	 * public static class BundleResourceSelectionDialog extends
	 * FilteredResourcesSelectionDialog {
	 * 
	 * public BundleResourceSelectionDialog(Shell shell, boolean multi, IProject
	 * project) { super(shell, multi, project, IResource.FILE);
	 * addListFilter(new BundleEntriesViewerFilter(project)); } }
	 * 
	 * private static class BundleEntriesViewerFilter extends ViewerFilter {
	 * 
	 * private IPath[] excludedPaths = new IPath[0];
	 * 
	 * public BundleEntriesViewerFilter(IProject project) { excludedPaths = new
	 * IPath[0]; IJavaProject javaPrj = JavaCore.create(project); if
	 * (javaPrj.exists()) { try { IClasspathEntry[] entries =
	 * javaPrj.getResolvedClasspath(true); List excludedPathsList = new
	 * ArrayList(entries.length); for (int i = 0; i < entries.length; i++) {
	 * IClasspathEntry entry = entries[i]; if (entry.getEntryKind() ==
	 * IClasspathEntry.CPE_SOURCE) { // add this path to the exclusion list and
	 * its output folder excludedPathsList.add(entry.getPath()); IPath
	 * outputLocation = entry.getOutputLocation(); if (outputLocation == null)
	 * if (!excludedPathsList.contains(javaPrj.getOutputLocation()))
	 * excludedPathsList.add(javaPrj.getOutputLocation()); else
	 * excludedPathsList.add(outputLocation); } else if (entry.getEntryKind() ==
	 * IClasspathEntry.CPE_LIBRARY) { // add this path to the exclusion list
	 * only if local to // the project if
	 * (project.getFullPath().isPrefixOf(entry.getPath()))
	 * excludedPathsList.add(entry.getPath()); } } excludedPaths = (IPath[])
	 * excludedPathsList.toArray(new IPath[excludedPathsList.size()]); } catch
	 * (JavaModelException e) { CDEditorPlugin.log(e); return; } } }
	 * 
	 * public boolean select(Viewer viewer, Object parentElement, Object
	 * element) { if (element instanceof IResource) { IResource resource =
	 * (IResource) element; if
	 * (resource.getFullPath().lastSegment().startsWith(".")) return false; for
	 * (int j = 0; j < excludedPaths.length; j++) if
	 * (excludedPaths[j].isPrefixOf(resource.getFullPath())) return false;
	 * return true; } return false; }
	 * 
	 * }
	 */
}
