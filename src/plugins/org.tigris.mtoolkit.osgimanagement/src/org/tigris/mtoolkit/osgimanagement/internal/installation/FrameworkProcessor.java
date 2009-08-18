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
package org.tigris.mtoolkit.osgimanagement.internal.installation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.common.installation.InstallationItemProcessor;
import org.tigris.mtoolkit.common.installation.InstallationTarget;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConnectFrameworkJob;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;

public class FrameworkProcessor implements InstallationItemProcessor {
	private static FrameworkProcessor defaultinstance;
	private static final String MIME_JAR = "application/java-archive";
	private static final String MIME_ZIP = "application/zip";
	private static final String MIME_DP = "application/vnd.osgi.dp";

	public static FrameworkProcessor getDefault() {
		if (defaultinstance == null) {
			defaultinstance = new FrameworkProcessor();
		}

		return defaultinstance;
	}

	public InstallationTarget[] getInstallationTargets() {
		FrameWork[] fws = FrameWorkView.getFrameworks();
		if (fws == null || fws.length == 0) {
			return new InstallationTarget[0];
		}

		List targets = new ArrayList();
		for (int i = 0; i < fws.length; i++) {
			targets.add(new FrameworkTarget(fws[i]));
		}
		return (InstallationTarget[]) targets.toArray(new InstallationTarget[targets.size()]);
	}

	public String getGeneralTargetName() {
		return "OSGi Framework";
	}

	public String[] getSupportedMimeTypes() {
		return new String[] { MIME_JAR, MIME_ZIP, MIME_DP };
	}

	public IStatus processInstallationItem(InstallationItem item, InstallationTarget target, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
		IStatus preparationStatus = item.prepare(subMonitor.newChild(50));

		if (preparationStatus.getSeverity() == IStatus.ERROR || preparationStatus.getSeverity() == IStatus.CANCEL) {
			return preparationStatus;
		}

		InputStream input = null;
		FrameWork framework = ((FrameworkTarget) target).getFramework();

		// TODO: Connecting to framework should report the connection progress
		// to the current monitor
		if (!framework.isConnected()) {
			Job connectJob = new ConnectFrameworkJob(framework);
			connectJob.schedule();
			try {
				connectJob.join();
			} catch (InterruptedException e1) {
				return new Status(Status.ERROR, FrameworkPlugin.getDefault().getId(), e1.getMessage(), e1);
			}
			if (!connectJob.getResult().isOK()) {
				return connectJob.getResult();
			}
		}

		try {
			input = item.getInputStream();
			install(input, item, framework);
		} catch (IOException e) {
			return FrameworkPlugin.newStatus(IStatus.ERROR, "Remote content installation failed", e);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
				}
			}
			item.dispose();
		}

		monitor.done();
		if (monitor.isCanceled()) {
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}
	
	public void install(InputStream input, InstallationItem item, FrameWork framework) {
		if (item.getMimeType().equals(MIME_DP)) {
			// TODO: Make methods, which are called from inside jobs to do
			// the real job
			FrameworkConnectorFactory.installDP(input, item.getName(), framework);
		} else {
			FrameworkConnectorFactory.installBundle(input, item.getName(), framework);
		}
	}
}
