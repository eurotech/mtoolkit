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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.tigris.mtoolkit.cdeditor.internal.CDEditorPlugin;

/**
 * The class BundleJavaSearchScope represents Java Search Scope for searching
 * in bundles.
 */
public class BundleJavaSearchScope implements IJavaSearchScope {

	private IJavaSearchScope enclosingScope;
	private IJavaProject project;
	
	public BundleJavaSearchScope(IJavaProject project) {
		Assert.isNotNull(project);
		this.enclosingScope = SearchEngine.createJavaSearchScope(new IJavaElement[] { project });
		this.project = project;
	}

	public boolean encloses(String resourcePath) {
		if (!enclosingScope.encloses(resourcePath))
			return false;
		if (resourcePath.endsWith(".class")) {
			return JDTModelHelper.isTypeAccessible(resourcePath, project);
		}
		CDEditorPlugin.debug("Didn't allow to pass: " + resourcePath);
		return true;
	}

	public boolean encloses(IJavaElement element) {
		if (!enclosingScope.encloses(element))
			return false;
		if (element instanceof IType)
			return JDTModelHelper.isTypeAccessible((IType) element, project);
		return true;
	}

	public IPath[] enclosingProjectsAndJars() {
		return enclosingScope.enclosingProjectsAndJars();
	}

	public boolean includesBinaries() {
		return enclosingScope.includesBinaries();
	}

	public boolean includesClasspaths() {
		return enclosingScope.includesClasspaths();
	}

	public void setIncludesBinaries(boolean includesBinaries) {
		enclosingScope.setIncludesBinaries(includesBinaries);
	}

	public void setIncludesClasspaths(boolean includesClasspaths) {
		enclosingScope.setIncludesClasspaths(includesClasspaths);
	}

}
