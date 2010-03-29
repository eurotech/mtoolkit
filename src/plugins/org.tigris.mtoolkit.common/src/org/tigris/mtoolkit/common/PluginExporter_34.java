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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.ui.progress.IProgressConstants;
import org.tigris.mtoolkit.common.ReflectionUtils.InvocationException;

/**
 * 
 * @author danail
 * @noextend This class is not intended to be subclassed by clients.
 */
public class PluginExporter_34 extends BasePluginExporter implements IPluginExporter {

	/**
	 * @since 5.0
	 */
	public void asyncExportPlugins(Object info) {
		try {
			Object obj = createExportOperation(info);
			((Job) obj).addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					setResult(event.getResult());
				}
			});

			((Job) obj).schedule();
		} catch (ReflectionUtils.InvocationException e) {
			setResult(new Status(IStatus.ERROR, UtilitiesPlugin.PLUGIN_ID, Messages.plugin_exporter_not_compatible, e)); //$NON-NLS-1$
		}
	}

	private Object createExportOperation(Object info) throws InvocationException {
		Object obj = ReflectionUtils.newInstance("org.eclipse.pde.internal.ui.build.PluginExportJob", new Class[] { FeatureExportInfo.class }, new Object[] { info }); //$NON-NLS-1$

		((Job) obj).setUser(true);
		((Job) obj).setProperty(IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_PLUGIN_OBJ);
		return obj;
	}

	public static boolean isCompatible() {
		return !PluginUtilities.compareVersion("org.eclipse.pde.ui", PluginUtilities.VERSION_3_5_0); //$NON-NLS-1$
	}

	public String getQualifier() {
		try {
			return (String) ReflectionUtils.invokeStaticMethod("org.eclipse.pde.internal.core.exports.FeatureExportOperation", "getDate"); //$NON-NLS-1$
		} catch (ReflectionUtils.InvocationException e) {
			return "qualifier"; //$NON-NLS-1$
		}
	}

	/**
	 * @since 5.0
	 */
	public IStatus syncExportPlugins(Object info, IProgressMonitor monitor) {
		try {
			Object op = createExportOperation(info);
			IStatus result;
			try {
				Job.getJobManager().beginRule(((Job) op).getRule(), monitor);
				result = (IStatus) ReflectionUtils.invokeProtectedMethod(op, "run", new Class[] { IProgressMonitor.class }, new Object[] { monitor });
			} catch (ReflectionUtils.InvocationException e) {
				result = UtilitiesPlugin.newStatus(IStatus.ERROR, Messages.plugin_exporter_not_compatible, e);
			} catch (ThreadDeath e) {
				throw e;
			} catch (Throwable t) {
				result = UtilitiesPlugin.newStatus(IStatus.ERROR, "An internal error ocurred during: " + ((Job)op).getName(), t);
			} finally {
				Job.getJobManager().endRule(((Job) op).getRule());
			}
			return result;
		} catch (ReflectionUtils.InvocationException e) {
			return new Status(IStatus.ERROR, UtilitiesPlugin.PLUGIN_ID, Messages.plugin_exporter_not_compatible, e); //$NON-NLS-1$
		}
	}
}
