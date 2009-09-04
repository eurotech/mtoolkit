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
package org.tigris.mtoolkit.common.installation;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.tigris.mtoolkit.common.UtilitiesPlugin;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.common.installation.InstallationItemProvider;

/**
 * Generic implementation of the {@link InstallationItemProvider}, which
 * supports workspace files with specific extension.
 * <p>
 * The file extensions, handled by this provider and their MIME type are
 * specified as additional attributes in the configuration element:<br>
 * <ul>
 * <li><b>extension</b> - the extension of the files (without the leading dot).
 * Example: if you want to specify provider for *.jar files, you need to specify
 * <code>extension="jar"</code>.</li>
 * <li><b>type</b> - the MIME type of the files, handled by this provider.</li>
 * </ul>
 * </p>
 * 
 */
public class WorkspaceFileProvider implements InstallationItemProvider {

	protected String extension;
	protected String mimeType;
	protected String name;

	public InstallationItem getInstallationItem(Object resource) {
		return new WorkspaceFileItem(getFileFromGeneric(resource), mimeType);
	}
	
	public void init(IConfigurationElement element) throws CoreException {
		extension = element.getAttribute("extension");
		if (extension == null)
			throw new CoreException(UtilitiesPlugin.newStatus(IStatus.ERROR,
				"Installation item provider must specify 'extension' attribute",
				null));
		mimeType = element.getAttribute("type");
		if (mimeType == null)
			throw new CoreException(UtilitiesPlugin.newStatus(IStatus.ERROR,
				"Installation item provider must specify 'type' attribute",
				null));
		name = element.getAttribute("name");
		if (name == null)
			throw new CoreException(UtilitiesPlugin.newStatus(IStatus.ERROR,
				"Installation item provider must specify 'name' attribute",
				null));
		// successful
	}

	public boolean isCapable(Object resource) {
		IFile file = getFileFromGeneric(resource);
		if (file != null && extension.equals(file.getFileExtension())) {
			return true;
		}
		return false;
	}

	protected IFile getFileFromGeneric(Object resource) {
		if (resource instanceof IFile) {
			return (IFile) resource;
		} else if (resource instanceof IAdaptable) {
			return (IFile) ((IAdaptable) resource).getAdapter(IFile.class);
		}
		return null;
	}

}
