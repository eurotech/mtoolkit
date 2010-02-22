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
package org.tigris.mtoolkit.common;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.IProgressConstants;

public class PluginExporter_35 implements IPluginExporter {

	private IStatus result = null;

	public void exportPlugins(Object info) {
		try {
			final FeatureExportInfo fInfo = (FeatureExportInfo) info;
			
			// always allow binary cucles
			ReflectionUtils.setField(fInfo, "allowBinaryCycles", Boolean.TRUE);
			
			final Object op = ReflectionUtils.newInstance("org.eclipse.pde.internal.core.exports.PluginExportOperation", new Class[] { //$NON-NLS-1$
				FeatureExportInfo.class, String.class },
				new Object[] { fInfo, "" });

			((Job) op).setUser(true);

			((Job) op).setRule(ResourcesPlugin.getWorkspace().getRoot());

			((Job) op).setProperty(IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_PLUGIN_OBJ);

			((Job) op).addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					boolean errors = false;
					try {
						errors = ((Boolean) ReflectionUtils.invokeMethod(op, "hasAntErrors")).booleanValue(); //$NON-NLS-1$
						if (errors) {
							Display display = Display.getCurrent();
							if (display == null)
								display = Display.getDefault();
							display.syncExec(new Runnable() {
								public void run() {
									String errorMessage = NLS.bind("Errors occurred during the export operation. The ant tasks generated log files which can be found at {0}", //$NON-NLS-1$
										fInfo.destinationDirectory);
									MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(),
										"Problem during export", errorMessage); //$NON-NLS-1$
								}
							});
						}
					} catch (ReflectionUtils.InvocationException t) {
						UtilitiesPlugin.error("Failed to get export plugins status", t); //$NON-NLS-1$
					} finally {
						result = event.getResult();
						if (errors && result.isOK()) {
							result = new Status(IStatus.ERROR,
								PDEPlugin.getPluginId(),
								NLS.bind("Errors occurred during the export operation. The ant tasks generated log files which can be found at {0}",
									fInfo.destinationDirectory));
						}
					}
				}
			});

			((Job) op).schedule();
		} catch (ReflectionUtils.InvocationException e) {
			result = new Status(IStatus.ERROR, UtilitiesPlugin.PLUGIN_ID, Messages.plugin_exporter_not_compatible, e);
		}
	}

	public IStatus getResult() {
		return result;
	}

	public boolean hasFinished() {
		return getResult() != null;
	}

	public static boolean isCompatible() {
		return PluginUtilities.compareVersion("org.eclipse.pde.ui", PluginUtilities.VERSION_3_5_0); //$NON-NLS-1$
	}

	public String getQualifier() {
		try {
			return (String) ReflectionUtils.invokeStaticMethod("org.eclipse.pde.internal.build.site.QualifierReplacer", //$NON-NLS-1$
				"getDateQualifier"); //$NON-NLS-1$
		} catch (Throwable t) {
			return "qualifier"; //$NON-NLS-1$
		}
	}
}
