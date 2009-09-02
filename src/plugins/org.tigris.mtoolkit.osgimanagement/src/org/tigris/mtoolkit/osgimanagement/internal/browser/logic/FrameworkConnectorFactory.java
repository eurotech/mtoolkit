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
package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;
import org.tigris.mtoolkit.iagent.DeviceConnectionListener;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.iagent.RemoteDP;
import org.tigris.mtoolkit.iagent.RemoteService;
import org.tigris.mtoolkit.iagent.internal.DeviceConnectorImpl;
import org.tigris.mtoolkit.iagent.spi.AbstractConnection;
import org.tigris.mtoolkit.iagent.spi.ConnectionManager;
import org.tigris.mtoolkit.osgimanagement.browser.model.Model;
import org.tigris.mtoolkit.osgimanagement.internal.ConsoleView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.UIHelper;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.BundlesCategory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Category;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.DeploymentPackage;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ObjectClass;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ServiceObject;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ServiceProperty;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ServicesCategory;
import org.tigris.mtoolkit.osgimanagement.internal.preferences.FrameworkPreferencesPage;

public class FrameworkConnectorFactory implements DeviceConnectionListener {

	/**
	 * Job listener, which deletes given file, when the associated job has
	 * finished.
	 * <p>
	 * Use this class, whenever you create temporary file, which is passed to a
	 * job and you want to remove the file, when the job has finished. The file
	 * is removed independently from the exact result of the job execution.
	 * 
	 */
	private static class DeleteWhenDoneListener extends JobChangeAdapter {
		private final File packageFile;

		private DeleteWhenDoneListener(File packageFile) {
			this.packageFile = packageFile;
		}

		public void done(IJobChangeEvent event) {
			packageFile.delete();
		}
	}

	private static FrameworkConnectorFactory factory = new FrameworkConnectorFactory();

	public static Hashtable lockObjHash = new Hashtable();

	public static boolean isAutoConnectEnabled = FrameworkPreferencesPage.autoConnectDefault;
	public static boolean isAutoStartBundlesEnabled = FrameworkPreferencesPage.autoStartAfterInstall;
	public static boolean isBundlesCategoriesShown = FrameworkPreferencesPage.showBundleCategories;

	public static Hashtable connectJobs = new Hashtable();

	public static void init() {
		try {
			DeviceConnector.addDeviceConnectionListener(factory);
		} catch (IAgentException e) {
			BrowserErrorHandler.processError(e, true);
		}
	}

	public static void deinit() {
		DeviceConnector.removeDeviceConnectionListener(factory);
	}

	public static void addBundles(FrameWork fw, boolean initServices, IProgressMonitor monitor) throws IAgentException {
		RemoteBundle rBundles[] = null;
		rBundles = fw.getConnector().getDeploymentManager().listBundles();

		if (rBundles != null) {
			if (monitor != null) {
				monitor.beginTask(Messages.retrieve_bundles_info, rBundles.length);
			}

			for (int i = 0; i < rBundles.length; i++) {
				addBundle(rBundles[i], fw);
				if (monitor != null) {
					if (monitor.isCanceled()) {
						return;
					}
					monitor.worked(1);
				}
			}
		}
		if (monitor != null) {
			monitor.done();
		}

		if (rBundles != null) {
			if (monitor != null) {
				monitor.beginTask(Messages.retrieve_services_info, rBundles.length);
			}

			for (int i = 0; i < rBundles.length; i++) {
				Bundle bundle = fw.findBundle(rBundles[i].getBundleId());
				if (bundle == null
								|| (bundle.getState() != org.osgi.framework.Bundle.ACTIVE && bundle.getState() != org.osgi.framework.Bundle.STARTING))
					continue;

				RemoteService rServices[] = rBundles[i].getRegisteredServices();
				for (int j = 0; j < rServices.length; j++) {
					fw.servicesVector.addElement(new ServiceObject(rServices[j], rBundles[i]));
				}
				rServices = rBundles[i].getServicesInUse();
				if (rServices != null) {
					for (int j = 0; j < rServices.length; j++) {
						ServiceObject.addUsedInBundle(rServices[j], rBundles[i], fw);
					}
				}

				if (monitor != null) {
					if (monitor.isCanceled()) {
						return;
					}
					monitor.worked(1);
				}
			}
		}
		if (monitor != null) {
			monitor.done();
		}
	}

	public static void addServicesNodes(FrameWork fw) throws IAgentException {
		for (int i = 0; i < fw.servicesVector.size(); i++) {
			ServiceObject servObj = (ServiceObject) fw.servicesVector.elementAt(i);
			addServiceNodes(fw, servObj);
		}
	}

	public static void addServiceNodes(FrameWork fw, ServiceObject servObj) throws IAgentException {
		Bundle bundle = fw.findBundle(servObj.getRegisteredIn().getBundleId());
		addServiceNodes(fw, servObj, bundle, true);
		bundle = fw.findBundleInDP(bundle.getID());
		if (bundle != null) {
			addServiceNodes(fw, servObj, bundle, false);
		}
	}

	public static void addServiceNodes(FrameWork fw, ServiceObject servObj, Bundle bundle, boolean first)
					throws IAgentException {
		if (bundle.getState() == org.osgi.framework.Bundle.ACTIVE
						|| bundle.getState() == org.osgi.framework.Bundle.STARTING
						|| bundle.getRemoteBundle().getState() == org.osgi.framework.Bundle.ACTIVE
						|| bundle.getRemoteBundle().getState() == org.osgi.framework.Bundle.STARTING) {
			try {
				addServiceCategoriesNodes(bundle);
				Model categories[] = bundle.getChildren();
				if (categories.length == 0)
					return;
				ServicesCategory registeredCategory = (ServicesCategory) categories[0];
				createObjectClassNodes(registeredCategory,
					servObj.getObjectClass(),
					new Long(servObj.getRemoteService().getServiceId()),
					servObj.getRemoteService());

				for (int i = 0; i < servObj.getObjectClass().length; i++) {
					ObjectClass hashService = new ObjectClass(fw,
						servObj.getObjectClass()[i] + " [Service " + servObj.getRemoteService().getServiceId() + "]",
						new Long(servObj.getRemoteService().getServiceId()),
						servObj.getRemoteService());
					BundlesCategory hashRegisteredCategory = new BundlesCategory(hashService,
						BundlesCategory.REGISTERED);
					BundlesCategory hashUsedCategory = new BundlesCategory(hashService, BundlesCategory.IN_USE);
					hashService.addElement(hashRegisteredCategory);
					hashService.addElement(hashUsedCategory);
					hashRegisteredCategory.addElement(new Bundle(bundle.getName(),
						hashRegisteredCategory,
						bundle.getRemoteBundle(),
						bundle.getState(),
						bundle.getType(),
						bundle.getCategory()));

					RemoteBundle usedInBundles[] = servObj.getUsedIn(fw);
					if (usedInBundles != null) {
						for (int k = 0; k < usedInBundles.length; k++) {
							Bundle usedInBundleNode = fw.findBundle(servObj.getUsedIn(fw)[k].getBundleId());
							if (usedInBundleNode == null) {
								throw new IllegalStateException("Bundle " + servObj.getUsedIn(fw)[k].getBundleId() + " is missing"); //$NON-NLS-1$ //$NON-NLS-2$
							}
							hashUsedCategory.addElement(new Bundle(usedInBundleNode.getName(),
								hashUsedCategory,
								usedInBundleNode.getRemoteBundle(),
								usedInBundleNode.getState(),
								usedInBundleNode.getType(),
								usedInBundleNode.getCategory()));
						}
					}

					for (int j = fw.servicesViewVector.size() - 1; j >= 0; j--) {
						Model model = (Model) fw.servicesViewVector.elementAt(j);
						if (model.getName().equals(hashService.getName())) {
							fw.servicesViewVector.removeElementAt(j);
						}
					}
					fw.servicesViewVector.addElement(hashService);

					if (fw.getViewType() == FrameWork.SERVICES_VIEW) {
						fw.addElement(hashService);
					}
				}

			} catch (IllegalArgumentException e) {
				// bundle was uninstalled
			}
		}

		RemoteBundle usedInBundles[] = servObj.getUsedIn(fw);
		if (usedInBundles != null) {
			for (int j = 0; j < usedInBundles.length; j++) {
				Bundle usedInBundle = first	? fw.findBundle(usedInBundles[j].getBundleId())
											: fw.findBundleInDP(usedInBundles[j].getBundleId());
				if (usedInBundle == null) {
					continue;
				}
				addServiceCategoriesNodes(usedInBundle);
				Model categories[] = usedInBundle.getChildren();
				ServicesCategory usedCategory = (ServicesCategory) categories[1];
				createObjectClassNodes(usedCategory,
					servObj.getObjectClass(),
					new Long(servObj.getRemoteService().getServiceId()),
					servObj.getRemoteService());
			}
		}
	}

	public static void addServiceCategoriesNodes(Bundle bundle) throws IAgentException {
		if (bundle.getType() == 0
						&& (bundle.getChildren() == null || bundle.getChildren().length == 0)
						&& (bundle.getState() == org.osgi.framework.Bundle.ACTIVE
										|| bundle.getState() == org.osgi.framework.Bundle.STARTING
										|| bundle.getRemoteBundle().getState() == org.osgi.framework.Bundle.STARTING || bundle.getRemoteBundle().getState() == org.osgi.framework.Bundle.ACTIVE)) {
			ServicesCategory registeredCategory = new ServicesCategory(bundle, ServicesCategory.REGISTERED);
			ServicesCategory usedCategory = new ServicesCategory(bundle, ServicesCategory.IN_USE);
			bundle.addElement(registeredCategory);
			bundle.addElement(usedCategory);
		}
	}

	public static void createObjectClassNodes(Model parent, String objClasses[], Long nameID, RemoteService service)
					throws IAgentException {
		for (int i = 0; i < objClasses.length; i++) {
			ObjectClass objClass = new ObjectClass(parent,
				objClasses[i] + " [Service " + service.getServiceId() + "]",
				nameID,
				service);
			parent.addElement(objClass);
			if (objClass.findFramework().isShownServicePropertiss())
				addServicePropertiesNodes(objClass);
		}
	}

	public static void addServicePropertiesNodes(ObjectClass objClass) throws IAgentException {
		RemoteService rService = objClass.getService();
		Dictionary servProperties = rService.getProperties();
		Enumeration keys = servProperties.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			Object value = servProperties.get(key);
			if (value instanceof String[]) {
				String[] values = (String[]) value;
				if (values.length == 1) {
					ServiceProperty node = new ServiceProperty(key + ": " + values[0], objClass);
					objClass.addElement(node);
				} else {
					for (int j = 0; j < values.length; j++) {
						StringBuffer buff = new StringBuffer();
						buff.append(key).append("[").append(String.valueOf(j + 1)).append("]");
						String key2 = buff.toString();
						ServiceProperty node = new ServiceProperty(key2 + ": " + values[j], objClass);
						objClass.addElement(node);
					}
				}
			} else {
				ServiceProperty node = new ServiceProperty(key + ": " + value.toString(), objClass);
				objClass.addElement(node);
			}
		}
	}

	public static synchronized void addBundle(RemoteBundle rBundle, FrameWork framework) throws IAgentException {
		try {
			if (framework.bundleHash.containsKey(new Long(rBundle.getBundleId())))
				return;

			Dictionary headers = rBundle.getHeaders(null);
			Model bundleParentModel;
			String categoryName = (String) headers.get("Bundle-Category");
			if (isBundlesCategoriesShown) {
				if (categoryName == null)
					categoryName = Messages.unknown_category_label;
				Category category = null;
				if (framework.categoryHash.containsKey(categoryName)) {
					category = (Category) framework.categoryHash.get(categoryName);
				} else {
					category = new Category(categoryName, framework.getBundlesNode());
					framework.categoryHash.put(categoryName, category);
					framework.getBundlesNode().addElement(category);
				}
				bundleParentModel = category;
			} else {
				bundleParentModel = framework.getBundlesNode();
			}

			String bundleName = getBundleName(rBundle, headers);
			Bundle bundle = new Bundle(bundleName,
				bundleParentModel,
				rBundle,
				rBundle.getState(),
				getRemoteBundleType(rBundle, headers),
				categoryName);
			if (bundle.getState() == org.osgi.framework.Bundle.ACTIVE
							|| bundle.getState() == org.osgi.framework.Bundle.STARTING) {
				addServiceCategoriesNodes(bundle);
			}
				bundleParentModel.addElement(bundle);
			framework.bundleHash.put(new Long(bundle.getID()), bundle);
			

		} catch (IllegalArgumentException e) {
			// bundle was uninstalled
		}
	}

	public static int getRemoteBundleType(RemoteBundle rBundle, Dictionary headers) throws IAgentException {
		String fragment = (String) headers.get("Fragment-Host"); //$NON-NLS-1$
		int type = 0;
		if (fragment != null && !fragment.equals("")) { //$NON-NLS-1$
			type = Bundle.BUNDLE_TYPE_FRAGMENT;
			RemoteBundle hosts[] = rBundle.getHosts();
			if (hosts != null && hosts.length == 1 && hosts[0].getBundleId() == 0) {
				type = Bundle.BUNDLE_TYPE_EXTENSION;
			}
		}
		return type;
	}

	public static void addDP(FrameWork framework, IProgressMonitor monitor) throws IAgentException {
		Model deplPackagesNode = framework.getDPNode();
		RemoteDP dps[] = null;
		dps = framework.getConnector().getDeploymentManager().listDeploymentPackages();

		Hashtable dpHash = new Hashtable();
		if (dps != null) {
			if (monitor != null) {
				monitor.beginTask(Messages.retrieve_dps_info, dps.length);
			}
			for (int i = 0; i < dps.length; i++) {
				DeploymentPackage dpNode = new DeploymentPackage(dps[i], deplPackagesNode, framework);
				dpHash.put(dps[i].getName(), dpNode);
				deplPackagesNode.addElement(dpNode);
				if (monitor != null) {
					if (monitor.isCanceled()) {
						return;
					}
					monitor.worked(1);
				}
			}

			framework.setDPHash(dpHash);
			if (monitor != null) {
				monitor.done();
			}
		}
	}

	public static void removeBundles(FrameWork framework) {
		Model[] categories = framework.getBundlesNode().getChildren();
		for (int i = 0; i < categories.length; i++) {
			framework.getBundlesNode().removeElement(categories[i]);
		}
		framework.removeElement(framework.getBundlesNode());
	}

	public static void removeDPs(FrameWork framework) {
		Model[] categories = framework.getDPNode().getChildren();
		for (int i = 0; i < categories.length; i++) {
			framework.getDPNode().removeElement(categories[i]);
		}
		framework.removeElement(framework.getDPNode());
	}

	public static void updateViewType(FrameWork fw) {
		if (fw.getViewType() == FrameWork.SERVICES_VIEW) {
			for (int i = 0; i < fw.servicesViewVector.size(); i++) {
				fw.addElement((Model) fw.servicesViewVector.elementAt(i));
			}
			fw.updateElement();
		} else {
			Model bundlesNode = fw.getBundlesNode();
			Model dpNode = fw.getDPNode();
			
			Enumeration keys = null;
			if(isBundlesCategoriesShown) {
				keys = fw.categoryHash.keys();
				while(keys.hasMoreElements()) {
					Model category = (Model)fw.categoryHash.get(keys.nextElement());
					bundlesNode.addElement(category);
					
				}
			} else {
				keys = fw.bundleHash.keys();
				while(keys.hasMoreElements()) {
					Model bundle = (Model)fw.bundleHash.get(keys.nextElement());
					bundlesNode.addElement(bundle);
					
				}
			}
			
			keys = fw.dpHash.keys();
			while (keys.hasMoreElements()) {
				dpNode.addElement((Model) fw.dpHash.get(keys.nextElement()));
			}

			fw.addElement(bundlesNode);
			fw.addElement(dpNode);
		}
	}

	public static void connectFrameWork(final FrameWork fw) {
		ConnectFrameworkJob job = new ConnectFrameworkJob(fw);
		job.schedule();
	}

	public static void stopBundle(final Bundle bundle) {
		if (bundle.getID() == 0) {
			MessageDialog dialog = new MessageDialog(FrameWorkView.getShell(),
				"Stop bundle",
				null,
				NLS.bind(Messages.stop_system_bundle, bundle.getName()),
				MessageDialog.QUESTION,
				new String[] { "Continue", "Cancel" },
				0);
			int statusCode = UIHelper.openWindow(dialog);
			if (statusCode == 1)
				return;
		}

		RemoteBundleOperation job = new StopBundleOperation(bundle);
		job.schedule();
	}

	public static void startBundle(final Bundle bundle) {
		RemoteBundleOperation job = new StartBundleOperation(bundle);
		job.schedule();
	}

	public static void updateBundle(final String bundleFileName, final Bundle bundle) {
		RemoteBundleOperation job = new UpdateBundleOperation(bundle, new File(bundleFileName));
		job.schedule();
	}

	public static void deinstallBundle(final Bundle bundle) {
		RemoteBundleOperation job = new UninstallBundleOperation(bundle);
		job.schedule();
	}

	public static void installDP(File dp, FrameWork framework) {
		InstallDeploymentOperation job = new InstallDeploymentOperation(dp, framework);
		job.schedule();
	}

	public static void installDP(InputStream stream, String name, FrameWork framework) {

		try {
			final File packageFile = saveFile(stream, name);
			InstallDeploymentOperation job = new InstallDeploymentOperation(packageFile, framework);
			job.schedule();
			job.addJobChangeListener(new DeleteWhenDoneListener(packageFile));
		} catch (IOException e) {
			StatusManager.getManager().handle(FrameworkPlugin.newStatus(IStatus.ERROR,
				"Unable to install deployment package",
				e),
				StatusManager.SHOW | StatusManager.LOG);
		}
	}

	public static void installBundle(InputStream input, String name, FrameWork framework) {
		try {
			final File bundle = saveFile(input, name);
			Job installBundleJob = new InstallBundleOperation(bundle, framework);
			installBundleJob.addJobChangeListener(new DeleteWhenDoneListener(bundle));
			installBundleJob.schedule();
		} catch (IOException e) {
			StatusManager.getManager().handle(FrameworkPlugin.newStatus(IStatus.ERROR, "Unable to install bundle", e),
				StatusManager.SHOW | StatusManager.LOG);
		}
	}

	private static File saveFile(InputStream input, String name) throws IOException {
		// TODO: Make saving stream to file done in a job
		IPath statePath = Platform.getStateLocation(FrameworkPlugin.getDefault().getBundle());
		File file = new File(statePath.toFile(), name);
		// make the directory hierarchy
		if (!file.getParentFile().exists() && !file.getParentFile().mkdirs())
			throw new IOException("Failed to create bundle state folder");
		FileOutputStream stream = new FileOutputStream(file);
		try {
			byte[] buf = new byte[8192];
			int read;
			while ((read = input.read(buf)) != -1) {
				stream.write(buf, 0, read);
			}
		} finally {
			stream.close();
		}
		return file;
	}

	public static void installBundle(final File bundle, final FrameWork framework) {
		RemoteBundleOperation job = new InstallBundleOperation(bundle, framework);
		job.schedule();
	}

	public static void deinstallDP(final DeploymentPackage dpNode) {
		UninstallDeploymentOperation job = new UninstallDeploymentOperation(dpNode);
		job.schedule();
	}

	public static RemoteBundle getRemoteBundle(String bundleLocation, FrameWork framework) throws IAgentException {

		RemoteBundle bundles[] = framework.getConnector().getDeploymentManager().listBundles();
		RemoteBundle bundle = null;

		for (int i = 0; i < bundles.length; i++) {
			if (bundles[i].getLocation().equals(bundleLocation)) {
				bundle = bundles[i];
				break;
			}
		}
		return bundle;
	}

	public static String getBundleName(RemoteBundle bundle, Dictionary headers) throws IAgentException {
		String bundleName = ""; //$NON-NLS-1$
		// bundleName = bundle.getSymbolicName();
		if (headers == null)
			headers = bundle.getHeaders(null);
		bundleName = (String) headers.get("Bundle-SymbolicName"); //$NON-NLS-1$
		if (bundleName == null || bundleName.equals("")) { //$NON-NLS-1$
			bundleName = (String) headers.get("Bundle-Name"); //$NON-NLS-1$
		}
		if (bundleName == null || bundleName.equals("")) { //$NON-NLS-1$
			bundleName = bundle.getLocation();
			if (bundleName.indexOf('/') != -1)
				bundleName = bundleName.substring(bundleName.lastIndexOf('/'));
			if (bundleName.indexOf('\\') != -1)
				bundleName = bundleName.substring(bundleName.lastIndexOf('\\'));
		}
		int delimIndex = bundleName.indexOf(';');
		if (delimIndex != -1)
			bundleName = bundleName.substring(0, delimIndex);
		return bundleName;
	}

	public void connected(final DeviceConnector connector) {
		String frameworkName = (String) connector.getProperties().get("framework-name"); //$NON-NLS-1$
		boolean autoConnected = true;
		FrameWork fw = null;
		FrameWork fws[] = FrameWorkView.getFrameworks();
		if (fws != null) {
			for (int i = 0; i < fws.length; i++) {
				if (fws[i].getName().equals(frameworkName)) {
					fw = fws[i];
					autoConnected = false;
					break;
				}
			}
		}

		// generate framework name
		if (fw == null) {
			if (!isAutoConnectEnabled)
				return;
			Hashtable frameWorkMap = new Hashtable();
			if (fws != null) {
				for (int i = 0; i < fws.length; i++) {
					frameWorkMap.put(fws[i].getName(), ""); //$NON-NLS-1$
				}
			}

			int index = 1;
			String frameWorkName = Messages.new_framework_default_name
							+ ' '
							+ connector.getProperties().get(DeviceConnector.KEY_DEVICE_IP);
			if (frameWorkMap.containsKey(frameWorkName)) {
				do {
					frameWorkName = Messages.new_framework_default_name
									+ ' '
									+ connector.getProperties().get(DeviceConnector.KEY_DEVICE_IP)
									+ "("
									+ index
									+ ")";
					index++;
				} while (frameWorkMap.containsKey(frameWorkName));
			}
			frameworkName = frameWorkName;
			connector.getProperties().put("framework-name", frameworkName); //$NON-NLS-1$
		}

		if (FrameWorkView.getTreeRoot() != null && fw == null) {
			fw = new FrameWork(frameworkName, FrameWorkView.getTreeRoot(), true);
			fw.setName(frameworkName);
			FrameWorkView.getTreeRoot().addElement(fw);
			fw.setConnector(connector);
		}

		BrowserErrorHandler.debug("FrameworkPlugin: " + connector.getProperties().get("framework-name") + " was connected with connector: " + connector); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		// already started connection job for this framework
		if (connectJobs.containsKey(frameworkName))
			return;

		if (autoConnected) {
			createPMPConnection(connector, fw, frameworkName, autoConnected);
		}
	}

	static void createPMPConnection(final DeviceConnector connector, FrameWork fw, String frameworkName,
					boolean autoConnected) {
		boolean pmp = false;
		try {
			pmp = ((DeviceConnectorImpl) connector).getConnectionManager().getActiveConnection(ConnectionManager.PMP_CONNECTION) != null;
		} catch (IAgentException e1) {
			e1.printStackTrace();
		}
		final boolean pmpConnected = pmp;

		// create and add pmp connection listener to fw
		PMPConnectionListener pmpListener = fw.getPMPConnectionListener();
		if (pmpListener == null) {
			pmpListener = new PMPConnectionListener(fw, frameworkName, connector, autoConnected);
			fw.setPMPConnectionListener(pmpListener);
		}

		final PMPConnectionListener listener = pmpListener;

		// force creating of pmp connection
		Job job = new Job(NLS.bind(Messages.connect_framework, fw.getName())) {
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("", 2); //$NON-NLS-1$
				// if pmp connection is available do not force creation but
				// directly connect
				if (pmpConnected) {
					listener.connected();
				} else {
					try {
						connector.getVMManager().isVMActive();
					} catch (IAgentException e) {
						BrowserErrorHandler.processError(e, NLS.bind(Messages.pmp_connect_error_message,
							connector.getProperties().get("framework-name")), true); //$NON-NLS-1$
						e.printStackTrace();
					}
				}
				monitor.done();
				return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
			}
		};
		connectJobs.put(frameworkName, job);
		job.schedule();
	}

	public void disconnected(DeviceConnector connector) {
		String fwName = (String) connector.getProperties().get("framework-name"); //$NON-NLS-1$
		FrameWork fw = FrameWorkView.findFramework(fwName);
		if (fw == null /* || !fw.isConnected() */)
			return;

		BrowserErrorHandler.debug("FrameworkPlugin: " + fwName + " was disconnected with connector: " + connector); //$NON-NLS-1$ //$NON-NLS-2$
		synchronized (getLockObject(connector)) {

			FrameWork fws[] = FrameWorkView.getFrameworks();
			if (fws != null) {
				for (int i = 0; i < fws.length; i++) {
					fw = fws[i];
					if (fw.getConnector() == connector) {
						fw.disconnect();
						fw.setPMPConnectionListener(null);
						fw.setConnector(null);
						if (fw.autoConnected) {
							FrameWorkView.treeRoot.removeElement(fw);
						}
						break;
					}
				}
			}
			disconnectConsole((String) connector.getProperties().get("framework-name")); //$NON-NLS-1$
		}
	}

	public static void disconnectFramework(FrameWork fw) {
		try {
			if (fw.autoConnected) {
				fw.disconnect();
			} else {
				if (fw.monitor != null) {
					fw.monitor.setCanceled(true);
				}
				// wait if connect operation is still active
				if (fw.getConnector() != null) {
					synchronized (FrameworkConnectorFactory.getLockObject(fw.getConnector())) {
					}
				}
				if (fw.getConnector() != null) {
					AbstractConnection conn = ((DeviceConnectorImpl) fw.getConnector()).getConnectionManager().getActiveConnection(ConnectionManager.PMP_CONNECTION);
					if (conn != null) {
						conn.closeConnection();
					}
				}
			}
		} catch (IAgentException e) {
			BrowserErrorHandler.processError(e, true);
			e.printStackTrace();
		}
	}

	public static void disconnectConsole(String frameworkName) {
		ConsoleView.disconnectServer(frameworkName);
	}

	public static FrameWork getFramework(String fwName) {
		FrameWork fws[] = FrameWorkView.getFrameworks();
		if (fws != null) {
			for (int i = 0; i < fws.length; i++) {
				if (fws[i].getName().equals(fwName)) {
					return fws[i];
				}
			}
		}
		return null;
	}

	public static Object getLockObject(DeviceConnector connector) {
		Object lockObj = lockObjHash.get(connector);
		if (lockObj == null) {
			lockObj = new Object();
			lockObjHash.put(connector, lockObj);
		}
		return lockObj;
	}
}