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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.natures.PDE;
import org.tigris.mtoolkit.common.IPluginExporter;
import org.tigris.mtoolkit.common.PluginExporter;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.common.installation.InstallationItemProvider;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;

public class PluginProvider implements InstallationItemProvider {

	public class PluginItem implements InstallationItem {
		private IProject project;
		private File file;

		public PluginItem(IProject project) {
			this.project = project;
		}

		public String getMimeType() {
			return "application/java-archive";
		}

		public InputStream getInputStream() throws IOException {
			return new FileInputStream(file);
		}

		public IStatus prepare(IProgressMonitor monitor) {
			monitor.beginTask("Exporting project: " + project.getName(), 1);

			FeatureExportInfo exportInfo = new FeatureExportInfo();
			exportInfo.toDirectory = true;
			exportInfo.useJarFormat = true;
			exportInfo.exportSource = false;
			exportInfo.destinationDirectory = FrameworkPlugin.getDefault().getStateLocation() + "";
			exportInfo.zipFileName = null;
			exportInfo.qualifier = null;

			String version = "1.0.0";
			IPluginModelBase model = PluginRegistry.findModel(project);
			BundleDescription descr = model.getBundleDescription();
			if (descr != null) {
				Object ver = descr.getVersion();
				if (ver != null) {
					version = ver.toString();
				}
			}
			String name = descr.getSymbolicName();

			IPluginExporter exporter = PluginExporter.getInstance();

			if (exporter == null) {
				monitor.done();
				return new Status(Status.ERROR, FrameworkPlugin.getDefault().getId(), "Could not export plugin: "
								+ project.getName());
			}

			if (version.endsWith("qualifier")) {
				exportInfo.qualifier = exporter.getQualifier();
				version = version.replaceAll("qualifier", exportInfo.qualifier);
			}

			exportInfo.items = new Object[] { model };
			exportInfo.signingInfo = null;
			exportInfo.jnlpInfo = null;
			exportInfo.targets = null;

			final String path = exportInfo.destinationDirectory + "/plugins/" + name + "_" + version + ".jar";
			// TODO: Make Plugin exporter a separate API
			exporter.exportPlugins(exportInfo);
			// TODO: The plugin exporter should be joinable
			while (!exporter.hasFinished()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			file = new File(path);
			if (exporter.getResult().isOK() && file.exists()) {
				monitor.done();
				return Status.OK_STATUS;
			}

			monitor.done();
			Throwable t = exporter.getResult().getException();
			if (t != null) {
				return new Status(Status.ERROR, FrameworkPlugin.getDefault().getId(), t.getMessage(), t);
			} else {
				return new Status(Status.ERROR, FrameworkPlugin.getDefault().getId(), "Could not export plugin: "
								+ project.getName());
			}
		}

		public String getName() {
			return file.getName();
		}

		public void dispose() {
		}
	}

	public InstallationItem getInstallationItem(Object resource) {
		IProject project = null;
		if (resource instanceof IJavaProject) {
			project = ((IJavaProject) resource).getProject();
		} else {
			project = ((IProject) resource).getProject();
		}
		return new PluginItem(project);
	}

	public boolean isCapable(Object resource) {
		if (resource instanceof IProject || resource instanceof IJavaProject) {
			IProject project = null;
			if (resource instanceof IJavaProject) {
				project = ((IJavaProject) resource).getProject();
			} else {
				project = ((IProject) resource).getProject();
			}
			return PDE.hasPluginNature(project);
		}
		return false;
	}

	public void init(IConfigurationElement element) throws CoreException {
	}
}
