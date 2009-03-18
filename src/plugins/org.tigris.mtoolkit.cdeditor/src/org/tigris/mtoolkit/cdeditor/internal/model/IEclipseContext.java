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
package org.tigris.mtoolkit.cdeditor.internal.model;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;

/**
 * Base interface for classes that provide common actions that are performed 
 * on eclipse context e.g. investigating type hierarchies and determing method 
 * belonging to specific type.
 */
public interface IEclipseContext extends IAdaptable {

	/**
	 * Finds entries that belong to the specified path.
	 * @param path the path where to search
	 * @return found entries that belong to the specified path
	 */
	public IFile[] findBundleEntries(PathFilter path);

	/**
	 * Retrieves type representation as instance of IType from provided class name 
	 * as String.
	 * @param className the name of the class
	 * @return the IType representation
	 */
	public IType findBundleClass(String className);

	/**
	 * Determines whether bundle file is packaged.
	 * @param filePath the file path
	 * @return true if bundle is packed, false otherwise
	 */
	public boolean isBundleFilePackaged(IPath filePath);

	/**
	 * Determines whether specified file exists.
	 * @param filePath the file path
	 * @return true if the file exists, false otherwise
	 */
	public boolean doesBundleFileExist(IPath filePath);

	/**
	 * Checks a type with name <code>className</code> is available in the type
	 * hierarchy of the passed <code>type</code>.
	 * 
	 * @param type
	 *            the type, whose hierarchy will be checked
	 * @param className
	 *            the name of the type searched for. Inner types must be separated with '$'
	 * @return true, if type with the given name is found in the hierarchy,
	 *         false otherwise
	 */
	public boolean doesTypeExtend(IType type, String className);

	/**
	 * Searches for method with given signature. Superclasses are not traversed.
	 * 
	 * @param type
	 * @param methodName
	 * @param parameterTypes
	 * @param considerCompatibleMethods
	 * @return
	 */
	public IMethod findMethod(IType type, String methodName, IType[] parameterTypes, boolean considerCompatibleMethods);
}
