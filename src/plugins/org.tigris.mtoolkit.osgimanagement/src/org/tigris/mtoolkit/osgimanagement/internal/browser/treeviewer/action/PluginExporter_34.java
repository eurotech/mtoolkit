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
package org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.exports.FeatureExportOperation;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.ui.progress.IProgressConstants;
import org.osgi.framework.Version;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;

public class PluginExporter_34 implements PluginExporter {

	private volatile IStatus result;

	public void exportPlugins(Object info) {
		try {
			Object obj = ReflectionUtils.newInstance("org.eclipse.pde.internal.ui.build.PluginExportJob", new Class[] { FeatureExportInfo.class }, new Object[] { info }); //$NON-NLS-1$

			((Job) obj).setUser(true);
			((Job) obj).setProperty(IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_PLUGIN_OBJ);
			((Job) obj).addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					result = event.getResult();
				}
			});

			((Job) obj).schedule();
		} catch (ReflectionUtils.InvocationException e) {
			result = new Status(IStatus.ERROR,
				FrameworkPlugin.PLUGIN_ID,
				"Plugin exporter is not compatible with current version of Eclipse", e); //$NON-NLS-1$
		}
	}

	public IStatus getResult() {
		return result;
	}

	public boolean hasFinished() {
		return result != null;
	}

	public static boolean isCompatible() {
		Version pdeCoreVersion = new Version((String) Platform.getBundle("org.eclipse.pde.ui").getHeaders().get("Bundle-Version")); //$NON-NLS-1$ //$NON-NLS-2$
		Version compatibleRange = new Version("3.5.0"); //$NON-NLS-1$
		return compatibleRange.compareTo(pdeCoreVersion) > 0;
	}

	public String getQualifier() {
		try {
			return (String) ReflectionUtils.invokeStaticMethod(FeatureExportOperation.class.getName(), "getDate"); //$NON-NLS-1$
		} catch (ReflectionUtils.InvocationException e) {
			return "qualifier"; //$NON-NLS-1$
		}
	}
}
