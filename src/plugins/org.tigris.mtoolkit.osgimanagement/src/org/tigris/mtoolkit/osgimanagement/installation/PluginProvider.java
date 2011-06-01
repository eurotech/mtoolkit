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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Version;
import org.tigris.mtoolkit.common.IPluginExporter;
import org.tigris.mtoolkit.common.PluginExporter;
import org.tigris.mtoolkit.common.android.AndroidUtils;
import org.tigris.mtoolkit.common.certificates.CertUtils;
import org.tigris.mtoolkit.common.export.PluginExportManager;
import org.tigris.mtoolkit.common.images.UIResources;
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
		private File preparedItem;
		

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
			String location = getLocation();
			if (location == null) {
				throw new IOException("Installation item is not prepared.");
			}
			return new FileInputStream(location);
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
		protected void setPreparedItem(File prepared) {
			this.preparedItem = prepared;
		}
		
		/**
		 * @since 6.0
		 */
		public String getLocation() {
			if (preparedItem != null) {
				return preparedItem.getAbsolutePath();
			}
			if (exportManager != null) {
				return exportManager.getLocation(pluginBase);
			}
			return null;
		}
		
		public String getName() {
			return pluginBase.getBundleDescription().getName();
		}

		public void dispose() {
			String location = getLocation();
			if (location == null) {
				return;
			}
			File file = new File(location);
			file.delete();
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
			if (descr == null) {
				String path = "";
				try {
					path = " for: " + pluginBase.getUnderlyingResource().getProject().getName();
				} catch (Throwable t) {
				}
				return new Status(IStatus.ERROR, FrameworkPlugin.getDefault().getId(),
						"Missing bundle description"+path);
			}
			BundleDescription[] required = descr == null ? new BundleDescription[0] : descr.getResolvedRequires();
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
				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
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
			if (project.isOpen()) {
			  return PDE.hasPluginNature(project);
			}
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

			List pluginItems = new ArrayList<PluginItem>();
			// post process exported bundles
			for (int i = 0; i < items.size(); i++) {
				Object item = items.get(i);
				if (item instanceof PluginItem) {
					pluginItems.add(item);
				}
			}
			
			IStatus postProcessStatus = postProcess(pluginItems, properties, monitor);
			if (!postProcessStatus.isOK()) {
				FrameworkPlugin.getDefault().getLog().log(postProcessStatus);
			}
			if (postProcessStatus.matches(IStatus.CANCEL)) {
				return postProcessStatus;
			}
		} finally {
			monitor.done();
		}

		monitor.done();
		return Status.OK_STATUS;
	}

	public String getName() {
		return "Plug-ins provider";
	}

	public ImageDescriptor getImageDescriptor() {
		return UIResources.getImageDescriptor(UIResources.PLUGIN_ICON);
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
    		
    		if (pluginsToBeExported.isEmpty())
    			// shortcut if we don't have anything for export
    			return Status.OK_STATUS;

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

        for (int i = 0; i < pluginsToBeExported.size(); i++) {
            IPluginModelBase iPluginModelBase = (IPluginModelBase) pluginsToBeExported.get(i);
            exportManager.addBundle(iPluginModelBase);
        }

        return exportManager;
    }

	private IStatus postProcess(List<PluginItem> items, Map properties, IProgressMonitor monitor) {

		File signedFile[] = new File[items.size()];
		File file[] = new File[items.size()];

		try {

			for (int i=0; i<items.size(); i++) {
				PluginItem item = items.get(i);


				String exportLocation = item.getLocation();
				if (exportLocation == null || !(file[i] = new File(exportLocation)).exists()) {
					return new Status(Status.ERROR, FrameworkPlugin.getDefault().getId(), "Plugin is not exported properly.");
				}
				if (properties != null && "Dalvik".equalsIgnoreCase((String) properties.get("jvm.name")) &&
						!AndroidUtils.isConvertedToDex(file[i])) {
					File convertedFile = new File(FrameworkPlugin.getDefault().getStateLocation() + "/dex/" + file[i].getName());
					convertedFile.getParentFile().mkdirs();
					AndroidUtils.convertToDex(file[i], convertedFile, monitor);
					file[i].delete();
					file[i] = convertedFile;
				}

				signedFile[i] = new File(FrameworkPlugin.getDefault().getStateLocation() + "/signed/" + file[i].getName());
				signedFile[i].getParentFile().mkdirs();
				if (signedFile[i].exists()) {
					signedFile[i].delete();
				}
			}

			try {
				CertUtils.signJars(file, signedFile, monitor, properties);
			} catch (IOException ioe) {
				boolean shouldContinue = CertUtils.continueWithoutSigning(ioe.getMessage());
				if (shouldContinue) {
					for (int i=0; i<signedFile.length; i++) {
						signedFile[i].delete();
					}
				} else {
					return new Status(Status.CANCEL, FrameworkPlugin.getDefault().getId(), "Could not sign plugins ", ioe);
				}
			}
			for (int i=0; i<signedFile.length; i++) {
				if (signedFile[i].exists()) {
					file[i].delete();
					file[i] = signedFile[i];
				}
				items.get(i).setPreparedItem(file[i]);
			}






		} catch (IOException ioe) {
			monitor.done();
			return new Status(Status.ERROR, FrameworkPlugin.getDefault().getId(), "Could not sign plugins ", ioe);
		}
		return Status.OK_STATUS;
	}
}
