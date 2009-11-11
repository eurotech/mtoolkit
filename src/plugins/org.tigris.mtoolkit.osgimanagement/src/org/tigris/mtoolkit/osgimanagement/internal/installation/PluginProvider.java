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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Version;
import org.tigris.mtoolkit.common.IPluginExporter;
import org.tigris.mtoolkit.common.PluginExporter;
import org.tigris.mtoolkit.common.android.AndroidUtils;
import org.tigris.mtoolkit.common.certificates.CertUtils;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.common.installation.InstallationItemProvider;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;

public class PluginProvider implements InstallationItemProvider {

	public class PluginItem implements InstallationItem {
		private IProject project;
		public File file;

		public PluginItem(IProject project) {
			this.project = project;
		}

		public String getMimeType() {
			return "application/java-archive";
		}

		public InputStream getInputStream() throws IOException {
			return new FileInputStream(file);
		}

		public IStatus prepare(IProgressMonitor monitor, Map properties) {
			monitor.beginTask("Exporting project: " + project.getName(), 1);

			final boolean[] saveResult = new boolean[1];
			saveResult[0] = true;
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					saveResult[0] = PlatformUI.getWorkbench().saveAllEditors(true);
				}
			});
			if (!saveResult[0]) {
				return new Status(IStatus.ERROR, FrameworkPlugin.PLUGIN_ID, "Could not prepare plugin. Saving of modified files was cancelled.");
			}

			FeatureExportInfo exportInfo = new FeatureExportInfo();
			exportInfo.toDirectory = true;
			exportInfo.useJarFormat = true;
			exportInfo.exportSource = false;
			exportInfo.destinationDirectory = FrameworkPlugin.getDefault().getStateLocation() + "";
			exportInfo.zipFileName = null;
			exportInfo.qualifier = null;

			String version = "1.0.0";
			IPluginModelBase model = PluginRegistry.findModel(project);
			if (model == null) {
				return new Status(IStatus.ERROR, FrameworkPlugin.PLUGIN_ID, "Can not parse the manifest file for project \""+project.getName()+"\"");
			}
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
				exportInfo.qualifier = "qualifier";//exporter.getQualifier();
//				version = version.replaceAll("qualifier", exportInfo.qualifier);
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
				try {
					if (properties != null && "Dalvik".equalsIgnoreCase((String) properties.get("jvm.name"))) {
						File convertedFile = new File(FrameworkPlugin.getDefault().getStateLocation() + "/dex/" + file.getName());
						convertedFile.getParentFile().mkdirs();
						AndroidUtils.convertToDex(file, convertedFile, monitor);
						file.delete();
						file = convertedFile;
					}

					File signedFile = new File(FrameworkPlugin.getDefault().getStateLocation() + "/signed/" + file.getName());
					signedFile.getParentFile().mkdirs();
					if (signedFile.exists()) {
						signedFile.delete();
					}
					try {
						CertUtils.signJar(file, signedFile, monitor, properties);
					} catch (IOException ioe) {
						if (CertUtils.continueWithoutSigning(ioe.getMessage())) {
							signedFile.delete();
						} else {
							throw ioe;
						}
					}
					if (signedFile.exists()) {
						file.delete();
						file = signedFile;
					}
				} catch (IOException ioe) {
					monitor.done();
					return new Status(Status.ERROR, FrameworkPlugin.getDefault().getId(), "Could not prepare plugin: "
							+ project.getName(), ioe);
				}
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
			if (file != null) {
				file.delete();
				file = null;
		    }
		}

		/**
		 * This method checks for required bundles for specified plugin missing
		 * on target framework. A dialog with missing bundles is shown to user
		 * to select and install necessary bundles.
		 * 
		 * @param framework
		 *            - target framework
		 * @return IStatus
		 */
		public IStatus checkAdditionalBundles(FrameWork framework, IProgressMonitor monitor) {
			// first check if framework is connected and all bundles info is
			// retrieved
			while ((!framework.isConnected() || framework.isConnecting()) && !monitor.isCanceled()) {
				try {
					Thread.currentThread().sleep(50);
				} catch (InterruptedException e) {
				}
			}

			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			
			// find missing bundle dependencies
			IPluginModelBase model = PluginRegistry.findModel(project);
			BundleDescription descr = model.getBundleDescription();
			BundleDescription[] required = descr.getResolvedRequires();
			final Vector dependencies = new Vector();
			for (int i = 0; i < required.length; i++) {
				String symbName = required[i].getSymbolicName();
				Version ver = required[i].getVersion();
				Set ids = framework.getBundlesKeys();
				Iterator iter = ids.iterator();
				boolean found = false;
				while (iter.hasNext()) {
					Bundle bundle = (Bundle) framework.findBundle(iter.next());
					try {
						if (bundle.getName().equals(symbName) && bundle.getVersion().compareTo(ver.toString()) >= 0) {
							found = true;
							break;
						}
					} catch (IAgentException e) {
						e.printStackTrace();
					}
				}
				if (!found) {
					dependencies.addElement(required[i]);
				}
			}

			// ask user which dependencies to install
			if (dependencies.size() > 0) {
				final boolean result[] = new boolean[1];
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						DependenciesSelectionDialog dependenciesDialog = new DependenciesSelectionDialog(Display
								.getDefault().getActiveShell(), dependencies);
						dependenciesDialog.open();
						if (dependenciesDialog.getReturnCode() == DependenciesSelectionDialog.OK) {
							Object[] selected = dependenciesDialog.getSelected();
							dependencies.removeAllElements();
							for (int i = 0; i < selected.length; i++) {
								dependencies.addElement(selected[i]);
							}
						} else {
							result[0] = true;
						}
					}
				});
				if (result[0]) {
					return Status.CANCEL_STATUS;
				}

				// install dependencies
				for (int i = 0; i < dependencies.size(); i++) {
					descr = (BundleDescription) dependencies.elementAt(i);
					String location = descr.getLocation();
					FrameworkConnectorFactory.installBundle(new File(location), framework);
				}
			}
			return Status.OK_STATUS;
		}

		public Object getAdapter(Class adapter) {
			if (adapter.equals(IResource.class)) {
				return project;
			} else {
				return null;
			}
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
