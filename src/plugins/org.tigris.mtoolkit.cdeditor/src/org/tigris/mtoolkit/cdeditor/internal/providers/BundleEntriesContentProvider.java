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
package org.tigris.mtoolkit.cdeditor.internal.providers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.tigris.mtoolkit.cdeditor.internal.CDEditorPlugin;


public class BundleEntriesContentProvider extends BaseWorkbenchContentProvider {

	private IPath[] excludedPaths = new IPath[0];

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput == null)
			return;
		excludedPaths = new IPath[0];
		IProject project = (IProject) newInput;
		IJavaProject javaPrj = JavaCore.create(project);
		if (javaPrj.exists()) {
			try {
				IClasspathEntry[] entries = javaPrj.getResolvedClasspath(true);
				List excludedPathsList = new ArrayList(entries.length);
				for (int i = 0; i < entries.length; i++) {
					IClasspathEntry entry = entries[i];
					if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						// add this path to the exclusion list and its output folder
						excludedPathsList.add(entry.getPath());
						IPath outputLocation = entry.getOutputLocation();
						if (outputLocation == null)
							if (!excludedPathsList.contains(javaPrj.getOutputLocation()))
								excludedPathsList.add(javaPrj.getOutputLocation());
						else
							excludedPathsList.add(outputLocation);
					} else if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
						// add this path to the exclusion list only if local to
						// the project
						if (project.getFullPath().isPrefixOf(entry.getPath()))
							excludedPathsList.add(entry.getPath());
					}
				}
				excludedPaths = (IPath[]) excludedPathsList.toArray(new IPath[excludedPathsList.size()]);
			} catch (JavaModelException e) {
				CDEditorPlugin.log(e);
				return;
			}
		}
	}

	public Object[] getChildren(Object element) {
		Object[] result = super.getChildren(element);
		if (result == null || result.length == 0)
			return result;
		int newCount = result.length;
		for (int i = 0; i < result.length; i++) {
			if (result[i] instanceof IAdaptable) {
				IResource resource = (IResource) ((IAdaptable) result[i]).getAdapter(IResource.class);
				if (resource != null) {
					boolean skipIt = false;
					if (resource.getFullPath().lastSegment().startsWith("."))
						skipIt = true;
					else
						for (int j = 0; j < excludedPaths.length; j++)
							if (excludedPaths[j].isPrefixOf(resource.getFullPath())) {
								result[i] = null;
								newCount--;
							}
					if (skipIt) {
						result[i] = null;
						newCount--;
					}
				}
			}
		}
		if (newCount == result.length)
			return result;
		// copy the array without the nulls
		Object[] newResult = new Object[newCount];
		for (int i = 0, j = 0; i < newResult.length && j < result.length; j++) {
			if (result[j] != null)
				newResult[i++] = result[j];
		}
		return newResult;
	}

}
