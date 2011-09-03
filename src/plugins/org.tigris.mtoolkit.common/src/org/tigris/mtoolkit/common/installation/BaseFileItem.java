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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.tigris.mtoolkit.common.FileUtils;
import org.tigris.mtoolkit.common.UtilitiesPlugin;
import org.tigris.mtoolkit.common.android.AndroidUtils;
import org.tigris.mtoolkit.common.certificates.CertUtils;

public class BaseFileItem implements InstallationItem {

	/**
	 * @since 6.0
	 */
	protected InstallationItemProvider provider;
	protected File baseFile;
	protected String mimeType;
	protected File preparedFile;

	public BaseFileItem(File file, String mimeType) {
		this.baseFile = file;
		this.mimeType = mimeType;
	}

  public InputStream getInputStream() throws IOException {
		if (preparedFile != null)
			return new FileInputStream(preparedFile);
		if (baseFile != null)
			return new FileInputStream(baseFile);
		throw new IllegalStateException("Installation item is not initialized properly with the generated artifact location");
	}

	/**
	 * @since 6.0
	 */
  public String getLocation() {
		if (preparedFile != null)
			return preparedFile.getAbsolutePath();
		if (baseFile != null)
			return baseFile.getAbsolutePath();
		throw new IllegalStateException("Installation item wasn't initialized correctly, missing location to base file");
	}

	public String getMimeType() {
		return mimeType;
	}

  public String getName() {
		return baseFile.getName();
	}

  public InstallationItem[] getChildren() {
    return null;
  }

	public IStatus prepare(IProgressMonitor monitor, Map properties) {
		try {
			if (properties != null && "Dalvik".equalsIgnoreCase((String) properties.get("jvm.name")) &&
							!AndroidUtils.isConvertedToDex(baseFile)) {
				File convertedFile = new File(UtilitiesPlugin.getDefault().getStateLocation() + "/dex/" + getName());
				convertedFile.getParentFile().mkdirs();
				if (FileUtils.getFileExtension(baseFile).equals("dp")) {
					if (!AndroidUtils.isDpConvertedToDex(baseFile)) {
						AndroidUtils.convertDpToDex(baseFile, convertedFile, monitor);
						preparedFile = convertedFile;
					}
				} else if (AndroidUtils.isDexCompatible(baseFile) && !AndroidUtils.isConvertedToDex(baseFile)) {
					AndroidUtils.convertToDex(baseFile, convertedFile, monitor);
					preparedFile = convertedFile;
				}
			}

			File signedFile = new File(UtilitiesPlugin.getDefault().getStateLocation() + "/signed/" + getName());
			signedFile.getParentFile().mkdirs();
			if (signedFile.exists()) {
				signedFile.delete();
			}
			try {
				File fileToSign = preparedFile != null ? preparedFile : baseFile;
				if (FileUtils.getFileExtension(baseFile).equals("dp")) {
					CertUtils.signDp(fileToSign, signedFile, monitor, properties);
				} else {
					CertUtils.signJar(fileToSign, signedFile, monitor, properties);
				}
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
		} catch (IOException ioe) {
			return UtilitiesPlugin.newStatus(IStatus.ERROR, "Failed to prepare file for installation", ioe);
		}
		return Status.OK_STATUS;
	}

	public void dispose() {
		if (preparedFile != null) {
			preparedFile.delete();
			preparedFile = null;
		}
	}

	public Object getAdapter(Class adapter) {
		return null;
	}
}
