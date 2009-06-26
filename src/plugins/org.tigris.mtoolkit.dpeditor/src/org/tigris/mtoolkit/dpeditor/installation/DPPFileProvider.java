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
package org.tigris.mtoolkit.dpeditor.installation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.common.installation.WorkspaceFileItem;
import org.tigris.mtoolkit.common.installation.WorkspaceFileProvider;
import org.tigris.mtoolkit.dpeditor.util.DPPUtil;
import org.tigris.mtoolkit.util.DPPFile;

public class DPPFileProvider extends WorkspaceFileProvider {

	public class DPPFileItem extends WorkspaceFileItem {

		private File dpFile;
		private boolean delete = false;

		public DPPFileItem(IFile file, String mimeType) {
			super(file, mimeType);
		}

		@Override
		public void dispose() {
			System.out.println("DPPFileProvider.DPPFileItem.dispose() "+delete);
			if (delete) {
				dpFile.delete();
				try {
					file.getParent().refreshLocal(IResource.DEPTH_ONE, null);
				} catch (CoreException e) {
					// do nothing
				}
			}
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new FileInputStream(dpFile);
		}

		@Override
		public IStatus prepare(IProgressMonitor monitor) {
			try {
				DPPFile dppFile = new DPPFile(file.getLocation().toFile(), file.getProject().getLocation().toOSString());
				dpFile = new File(dppFile.getBuildInfo().getBuildLocation()+"/"+ dppFile.getBuildInfo().getDpFileName());
				delete = !dpFile.exists();
				DPPUtil.generateDeploymentPackage(dppFile, monitor, file.getProject(), DPPUtil.TYPE_QUICK_BUILD_DPP);
			} catch (Exception e) {
				e.printStackTrace();
			}
			

			return Status.OK_STATUS;
		}
		
	}

	@Override
	public InstallationItem getInstallationItem(Object resource) {
		return new DPPFileItem(getFileFromGeneric(resource), mimeType);
	}
}
