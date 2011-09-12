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
package org.tigris.mtoolkit.osgimanagement.dp.logic;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.statushandlers.StatusManager;
import org.tigris.mtoolkit.osgimanagement.dp.Activator;
import org.tigris.mtoolkit.osgimanagement.dp.DPActionsProvider;
import org.tigris.mtoolkit.osgimanagement.dp.images.ImageHolder;
import org.tigris.mtoolkit.osgimanagement.installation.FrameworkProcessor;
import org.tigris.mtoolkit.osgimanagement.model.Framework;


public class DPProcessor extends FrameworkProcessor {

	public static final String MIME_DP = "application/vnd.osgi.dp";

	public String[] getSupportedMimeTypes() {
		return new String[] {MIME_DP};
	}

	public void install(InputStream input, String name, Framework framework, IProgressMonitor monitor) {
		try {
			final File packageFile = saveFile(input, name);
			InstallDeploymentOperation job = new InstallDeploymentOperation(packageFile, framework);
			job.schedule();
			job.addJobChangeListener(new DeleteWhenDoneListener(packageFile));
		} catch (IOException e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
				"Unable to install deployment package",
				e),
				StatusManager.SHOW | StatusManager.LOG);
		}
	}
	
	public String getName() {
		return "Deployment package processor";
	}

	protected Image getImage() {
		return ImageHolder.getImage(DPActionsProvider.DP_GROUP_IMAGE_PATH);
	}

	
}
