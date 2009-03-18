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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.tigris.mtoolkit.cdeditor.internal.CDEditorPlugin;

/**
 * Class JDTModelHelper provides convenient methods for type investigation.
 * Methods are static so there is no need to instantiate this class.
 */
public class JDTModelHelper {

	public static interface ITypeHierarchyVisitor {
		public boolean visit(IType type);
	}

	/**
	 * Checks whether class described by <code>resourcePath</code> is available
	 * in the context of the Java project <code>context</code>.
	 * 
	 * @param resourcePath
	 * @param context
	 * @return
	 */
	public static boolean isTypeAccessible(String resourcePath, IJavaProject context) {
		if (context == null)
			return false;
		IPath entryPath = null;
		IPath insideArchivePath = null;
		int idx = resourcePath.indexOf(IJavaSearchScope.JAR_FILE_ENTRY_SEPARATOR);
		if (idx != -1) {
			entryPath = new Path(resourcePath.substring(0, idx));
			insideArchivePath = new Path(resourcePath.substring(idx + 1, resourcePath.length()));
		} else {
			entryPath = new Path(resourcePath);
		}

		debug("isTypeAccessible(String): " + resourcePath + "\n\tcontext: " + context.getElementName() + "\n\tentryPath: " + entryPath + (insideArchivePath != null ? "\n\tinsideArchivePath: " + insideArchivePath : ""));

		try {
			IClasspathEntry[] entries = context.getResolvedClasspath(true);
			for (int i = 0; i < entries.length; i++) {
				IClasspathEntry entry = entries[i];
				if (entry.getPath().isPrefixOf(entryPath)) {
					char[] pathAsCharArray;
					if (insideArchivePath != null) {
						pathAsCharArray = insideArchivePath.toString().toCharArray();
					} else {
						if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
							pathAsCharArray = makeRelativeToPackageFragmentRoot(entryPath).toString().toCharArray();
						} else {
							pathAsCharArray = makeRelative(entry.getPath(), entryPath).toString().toCharArray();
						}
					}
					debug("isTypeAccessible(String): path inside CP entry: " + new String(pathAsCharArray));
					boolean isAccessible = doCheckTypeAccessibility(entry.getAccessRules(), pathAsCharArray);
					debug("isTypeAccessible: " + (isAccessible ? "ACCESSIBLE" : "NOT ACCESSIBLE") + " from context: " + context.getElementName());
					return isAccessible;
				}
			}
			debug("isTypeAccessible: context project doesn't have classpath entry for path: " + resourcePath);
		} catch (JavaModelException e) {
			CDEditorPlugin.log(e);
		}

		return true;
	}

	private static IPath makeRelativeToPackageFragmentRoot(IPath path) {
		IJavaProject container = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getProject(path.segment(0)));
		if (!container.exists())
			return path;
		try {
			IClasspathEntry[] entries = container.getResolvedClasspath(true);
			for (int i = 0; i < entries.length; i++) {
				if (entries[i].getPath().isPrefixOf(path))
					return makeRelative(entries[i].getPath(), path);
			}
		} catch (JavaModelException e) {
			CDEditorPlugin.log(e);
		}
		return path;
	}

	private static IPath makeRelative(IPath container, IPath child) {
		if (container.segmentCount() > child.segmentCount())
			throw new IllegalArgumentException("Cannot make " + child + " relative to " + container);
		StringBuffer relativePath = new StringBuffer();
		for (int j = container.segmentCount(); j < child.segmentCount(); j++) {
			if (relativePath.length() != 0)
				relativePath.append('/');
			relativePath.append(child.segment(j));
		}
		return new Path(relativePath.toString());
	}

	/**
	 * Checks whether given type is accessible in the context of particular
	 * project.
	 * 
	 * @param type
	 * @param project
	 * @return
	 */
	public static boolean isTypeAccessible(IType type, IJavaProject context) {
		if (context == null)
			return false;
		String resourcePath = type.getPath().toString();
		if (!resourcePath.endsWith(".java") && !resourcePath.endsWith(".class"))
			resourcePath = resourcePath + IJavaSearchScope.JAR_FILE_ENTRY_SEPARATOR + type.getFullyQualifiedName('$').replace('.', '/') + (type.isBinary() ? ".class" : ".java");
		debug("isTypeAccessible(IType): " + type.getFullyQualifiedName('$') + "\n\tcontext: " + context.getElementName() + "\n\tpath: " + resourcePath);
		return isTypeAccessible(resourcePath, context);
	}

	private static boolean doCheckTypeAccessibility(IAccessRule[] rules, char[] typePath) {
		for (int i = 0, length = rules.length; i < length; i++) {
			IAccessRule accessRule = rules[i];
			if (CharOperation.pathMatch(accessRule.getPattern().toString().toCharArray(), typePath, true, '/')) {
				switch (accessRule.getKind()) {
				case IAccessRule.K_DISCOURAGED:
				case IAccessRule.K_NON_ACCESSIBLE:
					return false;
				default:
					return true;
				}
			}
		}
		return true;
	}

	private static final void debug(String message) {
		if (CDEditorPlugin.DEBUG)
			CDEditorPlugin.debug("[JDTModelHelper] ".concat(message));
	}

	public static IProject[] getRequiredProjects(IProject prj) {
		IJavaProject project = JavaCore.create(prj);
		if (project.exists()) {
			IClasspathEntry[] entries;
			try {
				entries = project.getResolvedClasspath(true);
			} catch (JavaModelException e) {
				return null;
			}
			List dependentProjects = new ArrayList();
			for (int i = 0; i < entries.length; i++) {
				IClasspathEntry entry = entries[i];
				if (entry.getContentKind() == IPackageFragmentRoot.K_SOURCE && entry.getEntryKind() == IClasspathEntry.CPE_PROJECT)
					dependentProjects.add(prj.getWorkspace().getRoot().getProject(entry.getPath().segment(0)));
			}
			debug("Dependent projects (" + project.getElementName() + "): " + dependentProjects);
			return (IProject[]) dependentProjects.toArray(new IProject[dependentProjects.size()]);
		} else {
			return null;
		}
	}

	public static void visitTypeSuperclasses(IType type, ITypeHierarchyVisitor visitor) {
		Assert.isNotNull(type);
		Assert.isNotNull(visitor);
		IType current = type;
		try {
			boolean visitNext = visitor.visit(current);
			if (visitNext) {
				ITypeHierarchy hierarchy = type.newSupertypeHierarchy(null);
				do {
					current = hierarchy.getSuperclass(current);
					if (current != null)
						visitNext = visitor.visit(current);
				} while (current != null && visitNext);
			}
		} catch (JavaModelException e) {
			CDEditorPlugin.log(e);
		}
	}

	public static IType[] findTypes(IType context, String[] typesNames) throws JavaModelException {
		Assert.isNotNull(context);
		Assert.isNotNull(typesNames);
		IJavaProject typeProject = (IJavaProject) context.getAncestor(IJavaElement.JAVA_PROJECT);
		IType[] parameterTypes = new IType[typesNames.length];
		for (int i = 0; i < parameterTypes.length; i++) {
			parameterTypes[i] = typeProject.findType(typesNames[i].replace('$', '.'), (IProgressMonitor) null);
			if (parameterTypes[i] == null || !parameterTypes[i].exists())
				return null;
		}
		return parameterTypes;
	}

}
