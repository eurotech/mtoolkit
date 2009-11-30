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

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.tigris.mtoolkit.iagent.DeviceConnectionListener;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.internal.DeviceConnectorImpl;
import org.tigris.mtoolkit.iagent.spi.ConnectionManager;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.ActionsManager;
import org.tigris.mtoolkit.osgimanagement.internal.preferences.FrameworkPreferencesPage;
import org.tigris.mtoolkit.osgimanagement.model.Framework;

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
	private static FrameworkConnectorFactory factory = new FrameworkConnectorFactory();

	public static Hashtable lockObjHash = new Hashtable();

	public static boolean isAutoConnectEnabled = FrameworkPreferencesPage.autoConnectDefault;
	public static boolean isAutoStartBundlesEnabled = FrameworkPreferencesPage.autoStartAfterInstall;
	public static boolean isBundlesCategoriesShown = FrameworkPreferencesPage.showBundleCategories;

	public static final int CONNECT_PROGRESS = 1000;
	
	public static final int CONNECT_PROGRESS_CONNECTING = (int) (CONNECT_PROGRESS * 0.1);
	public static final int CONNECT_PROGRESS_BUNDLES = (int) (CONNECT_PROGRESS * 0.3);
	public static final int CONNECT_PROGRESS_SERVICES = (int) (CONNECT_PROGRESS * 0.2);
	public static final int CONNECT_PROGRESS_ADDITIONAL = (int) (CONNECT_PROGRESS * 0.4);
	
	

//	public static Hashtable connectJobs = new Hashtable();

	public static void init() {
		DeviceConnector.addDeviceConnectionListener(factory);
	}

	public static void deinit() {
		DeviceConnector.removeDeviceConnectionListener(factory);
	}

	public static void connectFrameWork(final FrameworkImpl fw) {
		ConnectFrameworkJob job = new ConnectFrameworkJob(fw);
		job.schedule();
	}

	public void connected(final DeviceConnector connector) {
		Dictionary connProps = connector.getProperties();
		if (connProps.get("framework-connection-temporary") != null)
			// the connection is only temporary and will be closed shortly
			return;
		String frameworkName = (String) connProps.get("framework-name"); //$NON-NLS-1$
		boolean autoConnected = true;
		FrameworkImpl fw = null;
		FrameworkImpl fws[] = FrameWorkView.getFrameworks();
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
			Object ip = connProps.get(DeviceConnector.KEY_DEVICE_IP);
			String defaultFWName = Messages.new_framework_default_name+
			" ["+connProps.get(DeviceConnector.TRANSPORT_TYPE)+":"+connProps.get(DeviceConnector.TRANSPORT_ID)+"]";
			String frameWorkName = defaultFWName;
			String suffix = " ";
			if (ip != null) { 
				suffix += ip;
			}
			if (frameWorkMap.containsKey(frameWorkName)) {
				do {
					frameWorkName = defaultFWName
									+ suffix
									+ "("
									+ index
									+ ")";
					index++;
				} while (frameWorkMap.containsKey(frameWorkName));
			}
			frameworkName = frameWorkName;
			connProps.put("framework-name", frameworkName); //$NON-NLS-1$
		}

		if (FrameWorkView.getTreeRoot() != null && fw == null) {
			fw = new FrameworkImpl(frameworkName, true);
			fw.setName(frameworkName);
			FrameWorkView.getTreeRoot().addElement(fw);
		}

//		if (fw.getConnector() != connector) {
//			fw.setConnector(connector);
//		}

		BrowserErrorHandler.debug("FrameworkPlugin: " + connProps.get("framework-name") + " was connected with connector: " + connector); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		createPMPConnection(connector, fw, frameworkName, autoConnected);
	}

	static void createPMPConnection(final DeviceConnector connector, FrameworkImpl fw, String frameworkName,
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
//		connectJobs.put(connector, job);
		job.schedule();
	}

	public void disconnected(DeviceConnector connector) {
		FrameworkImpl fw = null;
		String fwName = (String) connector.getProperties().get("framework-name"); //$NON-NLS-1$
		if (fwName != null) {
			fw = FrameWorkView.findFramework(fwName);
		} else {
			fw = FrameWorkView.findFramework(connector);
		}
		
		if (fw == null /* || !fw.isConnected() */)
			return;

		BrowserErrorHandler.debug("FrameworkPlugin: " + fwName + " was disconnected with connector: " + connector); //$NON-NLS-1$ //$NON-NLS-2$
		synchronized (Framework.getLockObject(connector)) {

			FrameworkImpl fws[] = FrameWorkView.getFrameworks();
			if (fws != null) {
				for (int i = 0; i < fws.length; i++) {
					fw = fws[i];
					if (fw.getConnector() == connector) {
						fw.disconnect();
						fw.setPMPConnectionListener(null);
						if (fw.autoConnected) {
							FrameWorkView.treeRoot.removeElement(fw);
						}
						break;
					}
				}
			}
			ActionsManager.disconnectConsole(fw); //$NON-NLS-1$
		}
	}
}