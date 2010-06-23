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
package org.tigris.mtoolkit.console.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.tigris.mtoolkit.iagent.IAgentException;

public class OSGiConsolePlugin extends AbstractUIPlugin {

	private static OSGiConsolePlugin instance = null;
	private static Hashtable storage = new Hashtable();

	public static final String PLUGIN_ID = "org.tigris.mtoolkit.osgimanagement"; //$NON-NLS-1$
	public static final String IAGENT_RPC_ID = "org.tigris.mtoolkit.iagent.rpc";

	public static String fileDialogLastSelection;

	public OSGiConsolePlugin() {
		super();
		if (instance == null)
			instance = this;
	}

	// Returns default instance
	public static OSGiConsolePlugin getDefault() {
		return instance;
	}

	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		instance = null;
	}

	// Initialize perespectives
	public void start(BundleContext context) throws Exception {
		super.start(context);
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

	public static void error(IAgentException e) {
		getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
	}

	public static void error(String message, Throwable t) {
		getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, t));
	}

	public static void warning(String message, Throwable t) {
		getDefault().getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message, t));
	}
	
	protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
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
				getDefault().getLog().log(Util.newStatus(IStatus.ERROR, "Failed to find IAgent RPC bundle", e));
			}
		}
		return null;
	}
}