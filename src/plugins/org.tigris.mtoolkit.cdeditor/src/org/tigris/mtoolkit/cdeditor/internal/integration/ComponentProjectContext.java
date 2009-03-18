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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.tigris.mtoolkit.cdeditor.internal.CDEditorPlugin;
import org.tigris.mtoolkit.cdeditor.internal.model.IEclipseContext;
import org.tigris.mtoolkit.cdeditor.internal.model.PathFilter;

/**
 * This implementation of IEclipseContext is intended to provide context of a 
 * project that contains Component Descriptions.
 * @see IEclipseContext
 */
public class ComponentProjectContext implements IEclipseContext {

	private IProject project;
	public static final IPath BUILD_PROPERTIES_FILE = new Path("build.properties");

	public ComponentProjectContext(IProject prj) {
		Assert.isNotNull(prj);
		this.project = prj;
	}

	public IFile[] findBundleEntries(PathFilter path) {
		if (!path.isValid())
			// invalid filtering paths doesn't match anything
			return new IFile[0];
		IPath parentPath = path.getPath().removeLastSegments(1);
		IContainer parent = parentPath.isEmpty() ? (IContainer) project : (IContainer) project.getFolder(parentPath);
		if (!parent.exists())
			return new IFile[0];
		List foundFiles = new ArrayList();
		try {
			IResource[] members = parent.members();
			for (int i = 0; i < members.length; i++) {
				if (members[i].getType() == IResource.FILE) {
					if (path.matchPath(members[i].getProjectRelativePath()))
						foundFiles.add(members[i]);
				}
			}
		} catch (CoreException e) {
			CDEditorPlugin.log(e);
			return new IFile[0];
		}
		return (IFile[]) foundFiles.toArray(new IFile[foundFiles.size()]);
	}

	public static IBuildModel getBuildModel(IPluginModelBase pluginBase) {
		if (pluginBase.getBuildModel() != null)
			return pluginBase.getBuildModel();
		IFile file = pluginBase.getUnderlyingResource().getProject().getFile(ComponentProjectContext.BUILD_PROPERTIES_FILE);
		if (!file.exists())
			return null;
		try {
			IBuildModel model = new WorkspaceBuildModel(file);
			model.load();
			return model;
		} catch (CoreException e) {
			CDEditorPlugin.log(e);
			return null;
		}
	}

	public boolean isBundleFilePackaged(IPath filePath) {
		IPluginModelBase modelBase = PDECore.getDefault().getModelManager().findModel(project);
		if (modelBase == null)
			return true;
		IBuildModel buildModel = ComponentProjectContext.getBuildModel(modelBase);
		if (buildModel == null)
			// no build model, skip the check
			return true;
		IBuild build = buildModel.getBuild();
		IBuildEntry binIncludesEntry = build.getEntry(IBuildEntry.BIN_INCLUDES);
		if (binIncludesEntry == null)
			return true;
		String[] tokens = binIncludesEntry.getTokens();
		for (int i = 0; i < tokens.length; i++) {
			IPath tokenPath = new Path(tokens[i]);
			if (tokenPath.isPrefixOf(filePath)) {
				return true;
			}
		}
		return false;
	}

	public boolean doesBundleFileExist(IPath filePath) {
		IFile file = project.getFile(filePath);
		return file.exists();
	}

	public IType findBundleClass(String className) {
		if (className == null)
			return null;
		debug("findBundleClass: ".concat(className));
		IJavaProject javaPrj = JavaCore.create(project);
		if (javaPrj.exists()) {
			try {
				IType type = javaPrj.findType(className.replace('$', '.'), (IProgressMonitor) null);
				if (type != null && type.exists() && JDTModelHelper.isTypeAccessible(type, javaPrj)) {
					debug("findBundleClass: found and accessible: " + type.getFullyQualifiedName('$'));
					return type;
				} else {
					debug("findBundleClass: not found or not accessible: " + (type != null ? type.getFullyQualifiedName('$') : "not found"));
					return null;
				}
			} catch (JavaModelException e) {
				CDEditorPlugin.log(e);
			}
		}
		debug("findBundleClass: JDT is missing structure information regarding this project.");
		return null;
	}

	public boolean doesTypeExtend(IType type, String className) {
		Assert.isNotNull(type);
		if (type.getFullyQualifiedName('$').equals(className))
			return true;
		try {
			ITypeHierarchy typeHierarchy = type.newSupertypeHierarchy(null);
			IType[] types = typeHierarchy.getAllSupertypes(type);
			for (int i = 0; i < types.length; i++) {
				if (types[i].getFullyQualifiedName('$').equals(className))
					return true;
			}
		} catch (JavaModelException e) {
		}
		return false;
	}

	public IMethod findMethod(IType methodContainer, String methodName, IType[] parameterExactTypes, boolean considerCompatibleMethods) {
		// case 1: filter methods only by name
		if (parameterExactTypes == null) {
			IMethod[] methods;
			try {
				methods = methodContainer.getMethods();
				for (int i = 0; i < methods.length; i++) {
					if (methods[i].getElementName().equals(methodName))
						return methods[i];
				}
			} catch (JavaModelException e) {
				CDEditorPlugin.log(e);
			}
			return null;
		}
		// case 2: there exists method with the same parameter types as
		// requested
		String[] parameterTypeSignatures = new String[parameterExactTypes.length];
		for (int i = 0; i < parameterExactTypes.length; i++)
			parameterTypeSignatures[i] = Signature.createTypeSignature(parameterExactTypes[i].getElementName(), false);
		IMethod candidateMethod = methodContainer.getMethod(methodName, parameterTypeSignatures);
		IMethod[] foundMethods = methodContainer.findMethods(candidateMethod);
		if (foundMethods != null && foundMethods.length > 0) {
			for (int i = 0; i < foundMethods.length; i++)
				if (isMethodParametersCompatible(foundMethods[i], parameterExactTypes, null))
					return foundMethods[i];
		} else if (considerCompatibleMethods) {
			// case 3: search for method which have compatible parameters
			// parameters which are supertypes of the requested ones
			try {
				IMethod[] allMethods = methodContainer.getMethods();
				ITypeHierarchy[] hierarchies = null;
				for (int i = 0; i < allMethods.length; i++) {
					if (!allMethods[i].getElementName().equals(methodName))
						continue;
					if (hierarchies == null) {
						hierarchies = new ITypeHierarchy[parameterExactTypes.length];
						for (int j = 0; j < parameterExactTypes.length; j++)
							hierarchies[j] = parameterExactTypes[j].newSupertypeHierarchy(null);
					}
					if (isMethodParametersCompatible(allMethods[i], parameterExactTypes, hierarchies))
						return allMethods[i];
				}
			} catch (JavaModelException e) {
				CDEditorPlugin.log(e);
			}
		}
		return null;
	}

	private boolean isMethodParametersCompatible(IMethod method, IType[] parameterTypes, ITypeHierarchy[] hierarchies) {
		try {
			IType context = (IType) method.getAncestor(IJavaElement.TYPE);
			String[] parameterSignatures = method.getParameterTypes();
			if (parameterTypes.length != parameterSignatures.length)
				return false;
			for (int j = 0; j < parameterTypes.length; j++) {
				String[][] resolvedTypes = context.resolveType(Signature.toString(Signature.getTypeErasure(parameterSignatures[j])));
				if (resolvedTypes == null || resolvedTypes.length != 1)
					// proceed to the next method if the type cannot be
					// resolved or it is ambiguous
					return false;
				if (resolvedTypes[0][0].equals("java.lang") && resolvedTypes[0][1].equals("Object"))
					// check whether the method accepts all parameters
					return true;
				if (!isResolvedTypeNameEquals(parameterTypes[j], resolvedTypes[0])) {
					if (hierarchies != null) {
						IType[] parameterSuperTypes = hierarchies[j].getAllSupertypes(parameterTypes[j]);
						boolean foundCompatible = false;
						for (int i = 0; i < parameterSuperTypes.length; i++) {
							if (isResolvedTypeNameEquals(parameterSuperTypes[i], resolvedTypes[0])) {
								foundCompatible = true;
								break;
							}
						}
						if (!foundCompatible)
							return false;
					}
				}
			}
			// if all parameters are validated, this is our method
			return true;
		} catch (JavaModelException e) {
			CDEditorPlugin.log(e);
			return false;
		}
	}

	private boolean isResolvedTypeNameEquals(IType type, String[] resolvedTypeName) {
		return resolvedTypeName[0].equals(type.getPackageFragment().getElementName()) && resolvedTypeName[1].equals(type.getTypeQualifiedName('.'));
	}

	public IProject getProject() {
		return project;
	}

	public Object getAdapter(Class adapter) {
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	private final void debug(String message) {
		if (CDEditorPlugin.DEBUG)
			CDEditorPlugin.debug("[EclipseContext(" + project.getName() + ")] " + message);
	}
}
