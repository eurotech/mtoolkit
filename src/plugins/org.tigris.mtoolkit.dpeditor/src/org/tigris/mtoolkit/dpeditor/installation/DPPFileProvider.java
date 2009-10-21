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
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.tigris.mtoolkit.common.android.AndroidUtils;
import org.tigris.mtoolkit.common.certificates.CertUtils;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.common.installation.WorkspaceFileItem;
import org.tigris.mtoolkit.common.installation.WorkspaceFileProvider;
import org.tigris.mtoolkit.dpeditor.DPActivator;
import org.tigris.mtoolkit.dpeditor.util.DPPUtil;
import org.tigris.mtoolkit.util.DPPFile;

public class DPPFileProvider extends WorkspaceFileProvider {

	public class DPPFileItem extends WorkspaceFileItem {

		public File dpFile;
		private boolean delete = false;

		public DPPFileItem(IFile file, String mimeType) {
			super(file, mimeType);
		}

		public void dispose() {
			super.dispose();
			if (delete) {
				dpFile.delete();
				try {
					file.getParent().refreshLocal(IResource.DEPTH_ONE, null);
				} catch (CoreException e) {
					// do nothing
				}
			}
		}

		public InputStream getInputStream() throws IOException {
			return new FileInputStream(dpFile);
		}

		public IStatus prepare(IProgressMonitor monitor, Map properties) {
			try {
				DPPFile dppFile = new DPPFile(file.getLocation().toFile(), file.getProject().getLocation().toOSString());
				dpFile = new File(dppFile.getBuildInfo().getBuildLocation() + "/"
						+ dppFile.getBuildInfo().getDpFileName());
				delete = !dpFile.exists();
				DPPUtil.generateDeploymentPackage(dppFile, monitor, file.getProject(), DPPUtil.TYPE_QUICK_BUILD_DPP);

				if (properties != null && "Dalvik".equalsIgnoreCase((String) properties.get("jvm.name"))) {
					File convertedFile = new File(DPActivator.getDefault().getStateLocation() + "/dex/" + file.getName());
					convertedFile.getParentFile().mkdirs();
					AndroidUtils.convertDpToDex(dpFile, convertedFile, monitor);
					preparedFile = convertedFile;
				}

				// sign file if properties contain signing info
				File signedFile = new File(DPActivator.getDefault().getStateLocation() + "/signed/" + file.getName());
				signedFile.getParentFile().mkdirs();
				if (signedFile.exists()) {
					signedFile.delete();
				}
				try {
					CertUtils.signJar(preparedFile != null ? preparedFile : dpFile, signedFile, monitor, properties);
				} catch (IOException ioe) {
					if (CertUtils.continueWithoutSigning(ioe.getMessage())) {
						signedFile.delete();
					} else {
						throw ioe;
					}
				}

				if (signedFile.exists()) {
					if (preparedFile != null) {
						preparedFile.delete();
					}
					preparedFile = signedFile;
				}
			} catch (Exception e) {
				return new Status(Status.ERROR, DPActivator.PLUGIN_ID, "Failed to prepare file for installation", e);
			}

			return Status.OK_STATUS;
		}
	}

	public InstallationItem getInstallationItem(Object resource) {
		return new DPPFileItem(getFileFromGeneric(resource), mimeType);
	}
}
