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
package org.tigris.mtoolkit.dpeditor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.tigris.mtoolkit.dpeditor.util.DPPUtil;

public class DPActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "deploymenEditor";

	// The shared instance
	private static DPActivator plugin;

	private boolean acceptAutomaticallyChanges = false;

	/**
	 * The constructor
	 */
	public DPActivator() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		loadDPProperties();
		DPPUtil.fileDialogLastSelection = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	public void stop(BundleContext context) throws Exception {
		saveDPProperties();
		plugin = null;
		super.stop(context);
	}

	private void loadDPProperties() {

		DPActivator dpActivator = DPActivator.getDefault();
		if (dpActivator != null) {
			String jarsigner = getPreferenceStore().getString("dpeditor.jarsigner");
			if (jarsigner.equals("")) {
				setLocationJavaHome();
			} else {
				System.setProperty("dpeditor.jarsigner", jarsigner);
			}
			acceptAutomaticallyChanges = getPreferenceStore().getBoolean("dpeditor.accept");
			System.setProperty("dpeditor.accept", (new Boolean(acceptAutomaticallyChanges)).toString());
			String resourceProcessors = getPreferenceStore().getString("dpeditor.resourceprcessors");
			Vector readRP = readResourceProcessors();
			if (readRP != null && readRP.size() != 0) {
				for (int i = 0; i < readRP.size(); i++) {
					String resourceProcessor = (String) readRP.elementAt(i);
					if (resourceProcessors.indexOf(resourceProcessor) == -1) {
						resourceProcessors += (resourceProcessors.equals("") ? "" : ";") + resourceProcessor;
					}
				}
			}
			System.setProperty("dpeditor.resourceprcessors", resourceProcessors);
		}
	}

	private void saveDPProperties() {
		String signer = "dpeditor.jarsigner";
		String accept = "dpeditor.accept";
		String proc = "dpeditor.resourceprcessors";
		IPreferenceStore ips = getPreferenceStore();

		if (System.getProperty(signer) != null) {
			ips.setValue(signer, System.getProperty(signer));
		}
		if (System.getProperty(accept) != null) {
			ips.setValue(accept, System.getProperty(accept));
		} else {
			ips.setValue(accept, "false");
		}
		ips.setValue(proc, System.getProperty(proc));

	}

	/**
	 * Sets dpeditor.jarsigner property depends on the java.home property
	 */
	public static void setLocationJavaHome() {
		String property = System.getProperty("java.home"); //$NON-NLS-1$
		if (property != null && !property.equals("")) { //$NON-NLS-1$
			File javaHome = new File(property);
			String jarSignerRelativePath = "bin" + File.separator + "jarsigner.exe";
			File signerFile = new File(javaHome, jarSignerRelativePath);
			if (!signerFile.exists()) {
				File tryParentFolderFile = new File(javaHome.getParentFile(), jarSignerRelativePath);
				if (tryParentFolderFile.exists())
					signerFile = tryParentFolderFile;
			}
			if (signerFile.exists()) {
				System.setProperty("dpeditor.jarsigner", signerFile.getAbsolutePath()); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static DPActivator getDefault() {
		return plugin;
	}

	public static Vector readResourceProcessors() {
		try {
			Vector result = new Vector();
			Enumeration installRPs = getInstallRPs();
			if (installRPs != null) {
				while (installRPs.hasMoreElements()) {
					URL nextURL = (URL) installRPs.nextElement();
					InputStream inputStream = nextURL.openConnection().getInputStream();
					String content = readFile(inputStream);
					Vector v = tokenize(content, "\n");
					for (int i = 0; i < v.size(); i++) {
						String element = (String) v.elementAt(i);
						if (!result.contains(element)) {
							result.addElement(element);
						}
					}
				}
			}
			return result;
		} catch (MalformedURLException ex) {
		} catch (IOException ioex) {
		}
		return null;
	}

	private static Enumeration getInstallRPs() throws MalformedURLException {
		DPActivator dpActivator = DPActivator.getDefault();
		Enumeration installPath = dpActivator.getBundle().findEntries("/", "ResourceProcessors.txt", false);
		return installPath;
	}

	private static String readFile(InputStream reader) {
		String fileContents = new String();
		if (reader == null) {
			return fileContents;
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		int n;
		do {
			try {
				n = reader.read(buffer);
			} catch (Exception ex) {
				n = -1;
			}
			if (n != -1) {
				baos.write(buffer, 0, n);
			}
		} while (n > 0);
		fileContents = baos.toString();
		try {
			reader.close();
			baos.close();
		} catch (Exception ex) {
		}
		return fileContents;
	}

	public static Vector tokenize(String str, String delim) {
		Vector tokenizedStr = new Vector();
		if (str != null && str.trim().length() != 0) {
			StringTokenizer tokenizer = new StringTokenizer(str.trim(), delim);
			while (tokenizer.hasMoreElements()) {
				tokenizedStr.addElement(((String) tokenizer.nextElement()).trim());
			}
		}
		return tokenizedStr;
	}

	public boolean isAcceptAutomaticallyChanges() {
		String value = System.getProperty("dpeditor.accept");
		if (value == null || value.equals("") || (!value.equals("true") && !value.equals("false"))) {
			value = "true";
		}
		acceptAutomaticallyChanges = new Boolean(value).booleanValue();
		return acceptAutomaticallyChanges;
	}
}