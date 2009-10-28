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
package org.tigris.mtoolkit.osgimanagement.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.FrameworkConnectorFactory;

public class FrameworkPlugin extends AbstractUIPlugin {

	private static FrameworkPlugin instance = null;
	private static Hashtable storage = new Hashtable();

	public static final String PLUGIN_ID = "org.tigris.mtoolkit.osgimanagement"; //$NON-NLS-1$
	public static final String IAGENT_RPC_ID = "org.tigris.mtoolkit.iagent.rpc";

	public static String fileDialogLastSelection;

	public FrameworkPlugin() {
		super();
		if (instance == null)
			instance = this;
	}

	// Returns default instance
	public static FrameworkPlugin getDefault() {
		return instance;
	}

	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		FrameworkConnectorFactory.deinit();
		instance = null;
	}

	// Initialize perespectives
	public void start(BundleContext context) throws Exception {
		super.start(context);
		// TODO: These settings should be kept in separate class, which also
		// needs to listen for changes in these settings
		FrameworkConnectorFactory.isAutoConnectEnabled = getPreferenceStore().getBoolean(ConstantsDistributor.MEMENTO_AUTOCONNECT);
		FrameworkConnectorFactory.isAutoStartBundlesEnabled = getPreferenceStore().getBoolean(ConstantsDistributor.MEMENTO_AUTOSTART_AFTER_INSTALL);
		BrowserErrorHandler.isInfoLogEnabled = getPreferenceStore().getBoolean(ConstantsDistributor.MEMENTO_INFO_LOG);
		FrameworkConnectorFactory.isBundlesCategoriesShown = getPreferenceStore().getBoolean(ConstantsDistributor.MEMENTO_SHOW_BUNDLE_CATEGORY);

		FrameworkConnectorFactory.init();
		FrameWorkView.restoreModel();
		fileDialogLastSelection = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
	}

	public String getId() {
		return PLUGIN_ID;
	}

	public static void putInStorage(Object key, Object value) {
		if (value == null) {
			storage.remove(key);
		} else {
			storage.put(key, value);
		}
	}

	public static Object getFromStorage(Object key) {
		return storage.get(key);
	}

	public static void error(String message, Throwable t) {
		getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, t));
	}

	protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
	}

	public static IStatus newStatus(int severity, String message, Throwable t) {
		return new Status(severity, PLUGIN_ID, message, t);
	}

	public static IStatus newStatus(String message, IStatus e) {
		return new MultiStatus(PLUGIN_ID, 0, new IStatus[] { e }, message, null);
	}

	public static IStatus handleIAgentException(IAgentException e) {
		String message = Messages.get(String.valueOf(e.getErrorCode()).replace('-', '_'));
		if (message == null) message = e.getMessage();
		return newStatus(IStatus.ERROR, message, e);
	}
	
	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}
	
	public static InputStream getIAgentBundleAsStream() {
		Bundle[] bundles = getDefault().getBundle().getBundleContext().getBundles();
		Bundle selectedIAgent = null;
		String selectedVersion = null;
		for (int i = 0; i < bundles.length; i++) {
			Bundle bundle = bundles[i];
			if (IAGENT_RPC_ID.equals(bundle.getSymbolicName())) {
				String version = (String) bundle.getHeaders("").get(Constants.BUNDLE_VERSION);
				if (version == null) {
					if (selectedVersion == null) {
						// if the iagent don't have a version
						// use the bundle with highest ID
						selectedIAgent = bundle;
						selectedVersion = version;
					}
				} else {
					if (selectedVersion == null || version.compareTo(selectedVersion) >= 0) {
						// if we have a version
						// we want the bundle with highest version and highest
						// ID
						selectedIAgent = bundle;
						selectedVersion = version;
					}
				}
			}
		}
		if (selectedIAgent != null) {
			try {
				File bundleFile = FileLocator.getBundleFile(selectedIAgent);
				if (bundleFile.isFile()) {
					return new FileInputStream(bundleFile);
				}
			} catch (IOException e) {
				getDefault().getLog().log(newStatus(IStatus.ERROR, "Failed to find IAgent RPC bundle", e));
			}
		}
		return null;
	}
}