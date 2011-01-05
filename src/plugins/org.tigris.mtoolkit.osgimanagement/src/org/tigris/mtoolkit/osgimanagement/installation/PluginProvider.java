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
package org.tigris.mtoolkit.osgimanagement.installation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Version;
import org.tigris.mtoolkit.common.IPluginExporter;
import org.tigris.mtoolkit.common.PluginExporter;
import org.tigris.mtoolkit.common.android.AndroidUtils;
import org.tigris.mtoolkit.common.certificates.CertUtils;
import org.tigris.mtoolkit.common.export.PluginExportManager;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.common.installation.InstallationItemProvider;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.InstallBundleOperation;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.RemoteBundleOperation;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;

/**
 * @since 5.0
 */
public class PluginProvider implements InstallationItemProvider {

	public class PluginItem implements InstallationItem {
		private IPluginModelBase pluginBase;
		private InstallationItemProvider provider;
		private PluginExportManager exportManager;

		/**
		 * @since 6.0
		 */
		public PluginItem(IPluginModelBase pluginBase, InstallationItemProvider provider) {
			this.pluginBase = pluginBase;
			this.provider = provider;
		}

		public String getMimeType() {
			return "application/java-archive";
		}
		
		public InputStream getInputStream() throws IOException {
			return new FileInputStream(getLocation());
		}
		
		public IStatus prepare(IProgressMonitor monitor, Map properties) {
			if (pluginBase == null) {
				return new Status(IStatus.ERROR, FrameworkPlugin.PLUGIN_ID, "Can not parse the manifest file for the plugin");
			}
			List items = new ArrayList();
			items.add(this);
			return provider.prepareItems(items, properties, monitor);

		}
		
		/**
		 * @since 6.0
		 */
		protected void setExportManager(PluginExportManager exportManager) {
			this.exportManager = exportManager;
		}
		
		/**
		 * @since 6.0
		 */
		public String getLocation() {
			if (exportManager != null) {
				return exportManager.getLocation(pluginBase);
			}
			return null;
		}
		
		public String getName() {
			return pluginBase.getBundleDescription().getName();
		}

		public void dispose() {
			File file = new File(getLocation());
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
		public IStatus checkAdditionalBundles(FrameworkImpl framework, IProgressMonitor monitor) {
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
			BundleDescription descr = pluginBase.getBundleDescription();
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
						FrameworkPlugin.error(e);
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
					RemoteBundleOperation job = new InstallBundleOperation(new File(location), framework);
					job.schedule();

				}
			}
			return Status.OK_STATUS;
		}

		public Object getAdapter(Class adapter) {
//			if (adapter.equals(IResource.class)) {
//				return project;
			if (adapter.equals(IBaseModel.class)) {
				return pluginBase;
			} else {
				return null;
			}
		}

		/**
		 * @since 6.0
		 */
		public IPluginModelBase getPlugin() {
			return pluginBase;
		}
	}

	public InstallationItem getInstallationItem(Object resource) {
		IPluginModelBase model;
		if (resource instanceof IJavaProject) {
			IProject project = ((IJavaProject) resource).getProject();
			model = PluginRegistry.findModel(project);
		} else if (resource instanceof IProject){
			IProject project = ((IProject) resource).getProject();
			model = PluginRegistry.findModel(project);
		} else {
			model = (IPluginModelBase) resource;
		}
		if (model == null)
			return null;
		return new PluginItem(model, this);
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
		} else if (resource instanceof IPluginModelBase) {
			return true;
		}
		return false;
	}

	public void init(IConfigurationElement element) throws CoreException {
	}

	/**
	 * @since 6.0
	 */
	public IStatus prepareItems(List items, Map properties, IProgressMonitor monitor) {
		try {
			IStatus result = export(items, monitor);
			if (!result.isOK())
				return result;

			//sign exported bundles
			for (int i = 0; i < items.size(); i++) {
				Object item = items.get(i);
				if (item instanceof PluginItem) {
					IPluginModelBase pluginBase = ((PluginItem) item).getPlugin();
					IStatus signStatus = signExported(pluginBase, ((PluginItem) item).getLocation(), properties, monitor);
					if (!signStatus.isOK()) {
						FrameworkPlugin.getDefault().getLog().log(signStatus);
					}
				}
			}
		} finally {
			monitor.done();
		}

		monitor.done();
		return Status.OK_STATUS;
	}
		
	private IStatus export(List items, final IProgressMonitor monitor) {//throws CoreException {
        try {
            monitor.beginTask("Exporting...", 10);
            
    		ArrayList pluginsToBeExported = new ArrayList();
    		for (int i = 0; i < items.size(); i++) {
    			Object item = items.get(i);
    			if (item instanceof PluginItem) {
    				pluginsToBeExported.add(((PluginItem) item).getPlugin());
    			}
    		}

            final IPluginExporter exporter = PluginExporter.getInstance();
            if (exporter == null) {
            	monitor.done();
				return new Status(Status.ERROR, FrameworkPlugin.getDefault().getId(), "Plugins could no be exported by "
						+ this.getClass());
            }

            //cleanOutputFolder();
            monitor.worked(2);
            
            IPath destinationPath = FrameworkPlugin.getDefault().getStateLocation();//.append("plugins");
            
            PluginExportManager exportManager = initExportManager(exporter, pluginsToBeExported );
            IStatus result = exportManager.export(destinationPath.toString(), monitor);
            if (!result.isOK())
            	return result;
            
            //set the export manager of PluginItem-s
            for (int i = 0; i < items.size(); i++) {
    			Object item = items.get(i);
    			if (item instanceof PluginItem) {
    				((PluginItem) item).setExportManager(exportManager);
    			}
    		}
        } finally {
            monitor.done();
        }
        return Status.OK_STATUS;
	}
	
	private PluginExportManager initExportManager(IPluginExporter exporter, List pluginsToBeExported) {
        PluginExportManager exportManager = PluginExportManager.create(exporter);

        // find out which bundles should be exported
		//        pluginsToBeExported = getBundlesToBeExported();
        if (pluginsToBeExported == null || pluginsToBeExported.size() == 0) {
            return null;
        }
        for (int i = 0; i < pluginsToBeExported.size(); i++) {
            IPluginModelBase iPluginModelBase = (IPluginModelBase) pluginsToBeExported.get(i);
            exportManager.addBundle(iPluginModelBase);
        }

        return exportManager;
    }
		
	private IStatus signExported(IPluginModelBase pluginBase, String exportLocation, Map properties, IProgressMonitor monitor) {
		File file = new File(exportLocation);
		if (!file.exists())
			return Status.OK_STATUS;
		try {
			if (properties != null && "Dalvik".equalsIgnoreCase((String) properties.get("jvm.name")) &&
					!AndroidUtils.isConvertedToDex(file)) {
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
			return new Status(Status.ERROR, FrameworkPlugin.getDefault().getId(), "Could not sign plugin: "
					+ pluginBase.getBundleDescription().getSymbolicName(), ioe);
		}
		return Status.OK_STATUS;
	}
		
//		public IStatus prepare(IProgressMonitor monitor, Map properties) {
//			if (pluginBase == null) {
//				return new Status(IStatus.ERROR, FrameworkPlugin.PLUGIN_ID, "Can not parse the manifest file for the plugin");
//			}
//			
//			BundleDescription descr = pluginBase.getBundleDescription();
//			String name = null;
//			String version = "1.0.0";
//			if (descr != null) {
//				Object ver = descr.getVersion();
//				if (ver != null) {
//					version = ver.toString();
//				}
//				name = descr.getSymbolicName();
//			} else {
//				name = pluginBase.toString();
//			}
//			
//			monitor.beginTask("Exporting plugin: " + name, 1);
//
//			final boolean[] saveResult = new boolean[1];
//			saveResult[0] = true;
//			Display.getDefault().syncExec(new Runnable() {
//				public void run() {
//					saveResult[0] = PlatformUI.getWorkbench().saveAllEditors(true);
//				}
//			});
//			if (!saveResult[0]) {
//				return new Status(IStatus.ERROR, FrameworkPlugin.PLUGIN_ID, "Could not prepare plugin. Saving of modified files was cancelled.");
//			}
//
//			FeatureExportInfo exportInfo = new FeatureExportInfo();
//			exportInfo.toDirectory = true;
//			exportInfo.useJarFormat = true;
//			exportInfo.exportSource = false;
//			exportInfo.destinationDirectory = FrameworkPlugin.getDefault().getStateLocation() + "";
//			exportInfo.zipFileName = null;
//			exportInfo.qualifier = null;
//
//			IPluginExporter exporter = PluginExporter.getInstance();
//			
////			PluginExportManager exportManager =   
//
//			if (exporter == null) {
//				monitor.done();
//				return new Status(Status.ERROR, FrameworkPlugin.getDefault().getId(), "Could not export plugin: "
//						+ name);
//			}
//
//			if (version.endsWith("qualifier")) {
//				exportInfo.qualifier = "qualifier";//exporter.getQualifier();
////				version = version.replaceAll("qualifier", exportInfo.qualifier);
//			}
//
//			exportInfo.items = new Object[] { pluginBase };
//			exportInfo.signingInfo = null;
//			exportInfo.jnlpInfo = null;
//			exportInfo.targets = null;
//
//			final String path = exportInfo.destinationDirectory + "/plugins/" + name + "_" + version + ".jar";
//			IStatus result = exporter.syncExportPlugins(exportInfo, monitor);
//			File file = new File(path);
//			if (result.isOK() && file.exists()) {
//				try {
//					if (properties != null && "Dalvik".equalsIgnoreCase((String) properties.get("jvm.name")) &&
//							!AndroidUtils.isConvertedToDex(file)) {
//						File convertedFile = new File(FrameworkPlugin.getDefault().getStateLocation() + "/dex/" + file.getName());
//						convertedFile.getParentFile().mkdirs();
//						AndroidUtils.convertToDex(file, convertedFile, monitor);
//						file.delete();
//						file = convertedFile;
//					}
//
//					File signedFile = new File(FrameworkPlugin.getDefault().getStateLocation() + "/signed/" + file.getName());
//					signedFile.getParentFile().mkdirs();
//					if (signedFile.exists()) {
//						signedFile.delete();
//					}
//					try {
//						CertUtils.signJar(file, signedFile, monitor, properties);
//					} catch (IOException ioe) {
//						if (CertUtils.continueWithoutSigning(ioe.getMessage())) {
//							signedFile.delete();
//						} else {
//							throw ioe;
//						}
//					}
//					if (signedFile.exists()) {
//						file.delete();
//						file = signedFile;
//					}
//				} catch (IOException ioe) {
//					monitor.done();
//					return new Status(Status.ERROR, FrameworkPlugin.getDefault().getId(), "Could not prepare plugin: "
//							+ name, ioe);
//				}
//				monitor.done();
//				return Status.OK_STATUS;
//			}
//
//			monitor.done();
//			Throwable t = exporter.getResult().getException();
//			if (t != null) {
//				return new Status(Status.ERROR, FrameworkPlugin.getDefault().getId(), t.getMessage(), t);
//			} else {
//				return new Status(Status.ERROR, FrameworkPlugin.getDefault().getId(), "Could not export plugin: "
//						+ name);
//			}
//		}
			
}
